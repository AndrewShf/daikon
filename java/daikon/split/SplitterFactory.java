package daikon.split;

import utilMDE.*;
import org.apache.oro.text.regex.*;
import daikon.*;
import daikon.split.misc.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.log4j.Category;

/**
 * This class creates Splitters from a .spinfo file. The public method is
 * read_spinfofile( spinfofilename ) which returns a vector containing program
 * point names and their corresponding arrays of Splitters
 **/

//todo: add logging and debugging
//      log error messages from compilation of splitters
public class SplitterFactory {

  public static final Category debug = Category.getInstance("daikon.split.SplitterFactory");
  // These are not Global.regexp_matcher and Global.regexp_compiler
  // because you can't reference fields of Global in your static
  // initializer if you have fields that are set via dkconfig.
  private static Perl5Matcher re_matcher = new Perl5Matcher();
  private static Perl5Compiler re_compiler = new Perl5Compiler();
  private static String tempdir;
  private static boolean javac = false; //indicates whether javac is being used as the  compiler.

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  Specifies whether or not the temporary Splitter files
   * should be deleted on exit.
   **/
  public static boolean dkconfig_delete_splitters_on_exit = true;

  /**
   * Reads the Splitter info.
   * @param <filename>.spinfo
   * @return Vector with the data: (Pptname, Splitter[], Pptname, Splitter[] ...)
   **/
  public static SplitterObject[][] read_spinfofile(File infofile, PptMap all_ppts)
    throws IOException, FileNotFoundException
  {
    tempdir = getTempdirName();
    if (FileCompiler.dkconfig_compiler.equals("javac"))
      javac = true;

    LineNumberReader reader = UtilMDE.LineNumberFileReader(infofile.toString());
    Vector splitterObjectArrays = new Vector(); //Vector of SplitterObject[]
    Vector replace = new Vector(); // [String] but paired
    Vector returnSplitters = new Vector();

    try {
      String line = reader.readLine();
      for ( ; line != null; line = reader.readLine()) {
        // skip blank lines and comments
        if (re_matcher.matches(line, blank_line) || line.startsWith("#")) {
          continue;
        } else if (line.startsWith("REPLACE")) {
          replace = read_replace_statements(replace, reader);
        } else if (line.startsWith("PPT_NAME")) {
          StringTokenizer tokenizer = new StringTokenizer(line);
          tokenizer.nextToken(); // throw away the first token "PPT_NAME"
          String pptName = tokenizer.nextToken().trim();
          SplitterObject[] spobjects = read_ppt_conditions(reader, pptName);
          if (spobjects != null)
            splitterObjectArrays.addElement(spobjects);
        } else {
          System.err.println("Incorrect format in .spinfo " + infofile
                             + " at line number " + reader.getLineNumber());
        }
      }
    } catch (IOException ioe ) {
      System.err.println(ioe + " \n at line number " + reader.getLineNumber()
                         + " of .spinfo file \n");
    }
    SplitterObject[][] sArrays = (SplitterObject[][])splitterObjectArrays.toArray(new SplitterObject[0][0]);
    write_compile_load(sArrays, replace, all_ppts);

    return sArrays;
  }

  /**
   * Reads the statements in the REPLACE section of the Splitter info
   * file.
   *
   * @return the same Vector[String] passed as the replace argument,
   * containing replace statements.
   **/
  static Vector read_replace_statements(Vector replace, // [String]
                                        LineNumberReader reader)
    throws IOException, FileNotFoundException
  {
    String line = reader.readLine();
    while ((line != null) && !re_matcher.matches(line, blank_line)) {
      // skip comments
      if (! line.startsWith("#")) {
        replace.addElement(line.trim());
      }
      line = reader.readLine();
    }
    return replace;
  }

  /**
   * Reads the splitting conditions associated with a program point.
   * @return an array of SplitterObjects representing the conditions
   * at a program point, or null if there is no condition under this
   * program point.
   **/
  static SplitterObject[] read_ppt_conditions(LineNumberReader reader, String pptname)
    throws IOException, FileNotFoundException
  {
    Vector splitterObjects = new Vector(); // [String == splitting condition]
    String line = reader.readLine();
    while ((line != null) && !re_matcher.matches(line, blank_line)) {
      // skip comments
      if (!line.startsWith("#")) {
        String condition = (line.trim());
        splitterObjects.addElement(new SplitterObject(pptname, condition, tempdir));
        if (re_matcher.contains(condition, null_pattern)) {
          splitterObjects.addElement(new SplitterObject(pptname, perform_null_substitution(condition), tempdir));
        }
      }
      line = reader.readLine();
    }
    if ( splitterObjects.size() > 0)
      return (SplitterObject[]) splitterObjects.toArray(new SplitterObject[0]);
    else
      return null;
  }

  /**
   * Write the Splitter classes, compile and load the Splitter objects
   * for each condition.  The Vector ppts_and_conds contains the
   * pptnames and their associated splitting conditions.
   *
   * @param splitterObjectArrays an Array containing Arrays of
   * SplitterObjects. Each slice of the 2-Dimensional array
   * corresponds to the SplitterObjects at a program point. The Java
   * source for each splitter condition is  yet to be written
   *
   * @return another Array containing Arrays of SplitterObjects. The
   * source code for the splitterObjects have been written, compiled and loaded
   **/
  static void write_compile_load(SplitterObject[][] splitterObjectArrays,
                                 Vector replace,        // [String]
                                 PptMap all_ppts)
  {
    ArrayList processes = new ArrayList(); // the processes

    for (int i = 0; i < splitterObjectArrays.length; i++) {
      //write the Splitter classes
      try {
        write_function_splitters(splitterObjectArrays[i], replace, all_ppts);

      } catch(IOException ioe) {
        System.err.println("SplitterFactory: " + ioe.toString()
                           + "\n while writing Splitter source");
      }

      ArrayList compile_list = new ArrayList();
      for (int j = 0; j < splitterObjectArrays[i].length; j++) {
        compile_list.add(splitterObjectArrays[i][j].getFullSourcePath());
      }

      //compile all the splitters under this program point
      processes.add(FileCompiler.compile_source(compile_list));
    }

    StringBuffer errorString = new StringBuffer(); //stores the error messages
    // wait for all the compilation processes to terminate
    // if compiling with javac, then the size of this vector is 0.
    for (int i = 0; i < processes.size(); i++) {
      TimedProcess tp = (TimedProcess) processes.get(i);
      errorString.append("\n");
      errorString.append(tp.getErrorMessage());
      if (!tp.finished()) {
        tp.waitFor();
      }
    }

    //javac tends to stop without completing the compilation if there
    //is an error in one of the files. Remove all the erring files
    //and recompile only the good ones
    if (javac) {
      recompile_without_errors (splitterObjectArrays, errorString.toString());
    }

    // Load the Splitters
    SplitterLoader loader = new SplitterLoader();
    for (int i = 0; i < splitterObjectArrays.length; i++) {
      for (int j = 0; j < splitterObjectArrays[i].length; j++) {
        splitterObjectArrays[i][j].load(loader);
      }
    }

    if (splitterObjectArrays.length > 0) {
      System.out.println("Splitters for this run created in " + tempdir + ".... Will " +
                         ((dkconfig_delete_splitters_on_exit == true) ? "": "Not ") + "be deleted on exit");
    }
  }

  /**
   * examine the errorString to identify the Splitters which cannot
   * compile, then recompile all the other files. This function is
   * necessary when compiling with javac because javac does not
   * compile all the files supplied to it if some of them contain
   * errors. So some "good" files end up not being compiled
   */
  private static void recompile_without_errors (SplitterObject[][] spArrays,
                                                String errorString) {
    //search the error string and extract the files with errors.
    if (errorString != null) {
      HashSet errors = new HashSet();
      PatternMatcherInput input = new PatternMatcherInput(errorString);
      while (re_matcher.contains(input, splitter_classname_pattern)) {
        MatchResult result = re_matcher.getMatch();
        errors.add(result.group(1));
      }

      List retry = new ArrayList();
      //collect all the splitters which were not compiled.
      for (int i = 0; i < spArrays.length; i++) {
        for (int j = 0; j < spArrays[i].length; j++) {
          if (!spArrays[i][j].compiled()) {
            if (!errors.contains(spArrays[i][j].getClassName())) {
              retry.add(spArrays[i][j].getFullSourcePath());
            }
          }
        }
      }

      TimedProcess tp = FileCompiler.compile_source(retry);

      try {
        Thread.sleep(3000);
      } catch (InterruptedException ie) {
        ie.printStackTrace();
      }

      //We don't want to wait for the old process for too long. We
      //wait for a short time, kill the process and recompile the set
      //of files, removing the leading file
      if (tp != null && !tp.finished()) {
        tp.waitFor();
      }
    }
  }

  //this pattern is used to search for the classnames of Java source
  //files in a string.
  static Pattern splitter_classname_pattern;
  static {
    try {
      splitter_classname_pattern
        = re_compiler.compile("\\s*([^" + UtilMDE.quote(File.separator) + "]*)\\.java");
    } catch (MalformedPatternException me) {
      System.err.println("Error while compiling javafile_pattern in SplitterFactory");
      me.printStackTrace();
    }
  }

  private static int guid = 0; // To give classes unique names

  /**
   * @param splitterObjects Must have at least one SplitterObject in it.
   * Writes the Java source code for the Splitters for this program point.
   **/
  static void write_function_splitters (SplitterObject[] splitterObjects,
                                        Vector replace,
                                        PptMap all_ppts)
    throws IOException
  {
    String ppt_name;
    SplitterObject curSplitterObject;

    ppt_name = splitterObjects[0].getPptName();
    PptTopLevel ppt = find_corresponding_ppt(ppt_name, all_ppts);
    if (ppt == null) {
      //try with the OBJECT program point
      ppt = find_corresponding_ppt("OBJECT", all_ppts);
    }
    if (ppt == null) {
      //We just get a random program point (the first) from the pptmap.
      //Hopefully we can find the variables that we need at this ppt
      Iterator pptIter = all_ppts.iterator();
      if (pptIter.hasNext()) {
        ppt = (PptTopLevel)pptIter.next();
      }
    }
    if (ppt == null) {
      for (int i = 0; i < splitterObjects.length; i++) {
        splitterObjects[i].setError( "No corresponding program point found for " + ppt_name);
        return;
      }
    }

    Vector[] params_and_types = get_params_and_types(ppt);
    Vector parameters = params_and_types[0];
    Vector types = params_and_types[1];

    if (parameters.size() > 0 && types.size() > 0) {
      String[] all_params = (String[])parameters.toArray(new String[0]);
      String[] all_types = (String[])types.toArray(new String[0]);
      int num_params = all_params.length;

      // The Splitter variable names corresponding to the variables
      // at the program point.
      String[] param_names = new String[num_params];
      for (int i = 0; i < num_params; i++) {
        //declared variable names in the Splitter class cannot have characters
        //like ".", "(" etc. Change, for example, "node.parent" to "node_parent"
        //and orig(x) to orig_x
        String temp = all_params[i];
        temp = temp.replace('.','_');
        temp = temp.replace('[','_');
        temp = temp.replace(']','_');
        if (temp.equals("return")) temp = "return_Daikon";
        if (temp.equals("this")) temp = "this_Daikon";
        if (temp.indexOf("orig") >= 0) temp = replace_orig(temp);
        param_names[i] = temp;
      }

      // Get the function names and argument names of functions to be replaced.
      // For example, if the function "max(int a, int b)" is to be replaced by
      // the expression "a > b ? a : b", then the return value of this
      // function call is a vector containing a String representing the function
      // name "max" as the first element, an array of Strings representing the
      // argument names [ a b ] as the second element, and the replacement
      // expression "a > b ? a : b" as the third element
      Vector replace_data = get_fnames_and_args(replace);


      // Class names cannot only have legal identifier characters in
      // them. For example if the ppt_name is Foo.bar, the name of the
      // Splitter start with Foo_bar
      String splittername;
      {
        char[] cleaned = ppt_name.toCharArray();
        for (int i=0; i < cleaned.length; i++) {
          char c = cleaned[i];
          if (! Character.isJavaIdentifierStart(c)) {
            cleaned[i] = '_';
          }
        }
        splittername = new String(cleaned);
      }

      // write a Splitter class for each condition:
      for (int numsplitters = 0; numsplitters < splitterObjects.length; numsplitters++) {
        curSplitterObject = splitterObjects[numsplitters];

        // the String 'condition' is returned in this Splitter's public method
        // 'condition()' the String 'test_string' is used to do the splitting in
        // the 'test()' method
        String condition = curSplitterObject.condition();
        String test_string = condition;

        String splitter_fname = splittername + "_" + (guid++);
        int lastIndex = splitter_fname.indexOf(File.separator);
        curSplitterObject.setClassName(splitter_fname.substring(lastIndex + 1));
        String class_name = ppt_name.substring(0, ppt_name.indexOf('.')+1);
        curSplitterObject.setGUID(guid);

        // Each Splitter will use a different set of parameters depending on the
        // parameters used in its condition
        String[] params = (String[])all_params.clone();
        // substitute the occurence of "== null" or "!= null" with "== 0" in the
        // test string

        // if the condition has a function call in it which needs to be replaced
        // with the function body: eg. isEmpty -> myArray.length == 0,
        // do the replacement now

        test_string = replace_condition(test_string, replace_data);

        // ensure that the declared variable names in the Splitter match the
        // variable names in the test string. For example: if the condition tests
        // for "this.mylength == 0", the variable corresponding to "this.mylength"
        // in the Splitter is declared as "this_mylength". Therefore change the
        // condition to "this_mylength == 0"

        test_string = find_applicable_variables(params, param_names, test_string, class_name);

        //replace all occurences of "orig(varname)" with "orig_varname" in the condition.

        test_string = replace_orig(test_string);

        // look for all variables which are used as array accessors and change
        // their type to "int_index". This is necessary because daikon represents
        // all numeric types as long. However array accessors must be cast to ints
        // so they can be used to access arrays. Therefore for these variables,
        // we need to call a different method (VarInfo.getIndexValue()) to
        // obtain their values from the VarInfo.

        identify_array_index_variables_as_int(condition, params, all_types);

        StringBuffer file_string = new StringBuffer();
        file_string.append("import daikon.*; \n");
        file_string.append("import daikon.split.*; \n");

        file_string.append("public final class " + splitter_fname + " extends Splitter { \n\n");

        // print the condition() method
        file_string.append("  public String condition () { return\"" +
                           UtilMDE.quote(condition) + "\" ;} \n\n");

        // print the parameters
        for (int i = 0; i < num_params; i++) {
          // the param has been replaced with null if it doesn't appear in the
          // test string. Therefore skip it since the splitter doesn't need any
          // information about it
          String par = param_names[i].trim();
          String typ = all_types[i].trim();
          print_parameter_declarations(file_string, par, typ);
        }

        // print the constructors
        file_string.append("\n  public " + splitter_fname + "() { } \n");

        file_string.append("  public " + splitter_fname + "(Ppt ppt) {  \n");
        for (int i = 0; i < num_params; i++) {
          String param =  params[i];
          if (param == null) continue;
          String param_name = param_names[i];
          String typ = all_types[i];
          if (typ.equals("char[]")) {
            //char[] is not an array
            file_string.append("    " + param_name + "_varinfo = ppt.findVar(VarInfoName.parse(\"" + param + "[]\")) ; \n");
          } else if (typ.endsWith("[]")) {
            // attach "_array" to an array variable name anytime to distinguish
            // it from its hashCode, which has the same name. (eg change
            // "<arrayname>.length" to "<arrayname>_array.length" and
            // <arrayname>[i] to <arrayname>_array[i] so that the array , and not
            // the hashCode of the array is used in the test). The arrayname is
            // also changed to <arrayname>_array in the Splitter.
            file_string.append("    " + param_name + "_array_varinfo = ppt.findVar(VarInfoName.parse(\"" + param + "[]\")) ; \n");
            test_string = distinguish_arraynames_from_hashCodes_in_teststring(param, test_string);
          } else {
            file_string.append("    " + param_name + "_varinfo = ppt.findVar(VarInfoName.parse(\"" + param + "\")) ; \n");
          }
        }
        // file_string.append("    instantiated = true;\n");
        file_string.append("  }\n\n");

        // print the instantiate method.
        file_string.append("  public Splitter instantiate(Ppt ppt) { \n    return new ");
        file_string.append(splitter_fname + "(ppt);\n  } \n\n");

        // print the valid() method
        file_string.append("  public boolean valid() { \n    return(");
        for (int i = 0; i < param_names.length; i++) {
          if (params[i] == null) continue;
          if (all_types[i].endsWith("[]") && !all_types[i].equals("char[]")) {
            file_string.append( "(" + param_names[i] + "_array_varinfo != null) && ");
          } else {
            file_string.append("(" + param_names[i] + "_varinfo != null) && ");
          }

        }
        file_string.append(" true );\n  }\n\n");


        // print the test() method
        test_string = print_test_method(file_string, param_names, params, all_types, test_string);
        curSplitterObject.setTestString(test_string);

        // print the repr() method
        file_string.append("  public String repr() { \n    return \"" + splitter_fname + ": \"");
        for (int i = 0; i < num_params; i++) {
          if (params[i] == null) continue;
          String par = param_names[i].trim();
          if (all_types[i].endsWith("[]") && !all_types[i].equals("char[]")) {
            file_string.append(" + \"" + par + "_varinfo=\" + " + par + "_array_varinfo.repr() + \" \"\n");
          } else {
            file_string.append(" + \"" + par + "_varinfo=\" + " + par + "_varinfo.repr() + \" \"\n");
          }
        }
        file_string.append("        ;\n  }\n\n");

        // finish off the class
        file_string.append("}\n");

        // write to the file. Assuming that the tempdir has already been set
        try {
          BufferedWriter writer = UtilMDE.BufferedFileWriter(tempdir+splitter_fname+".java");
          if (dkconfig_delete_splitters_on_exit) {
            (new File (tempdir+splitter_fname+".java")).deleteOnExit();
            (new File (tempdir+splitter_fname+".class")).deleteOnExit();
          }
          writer.write(file_string.toString());
          writer.flush();
        } catch (IOException ioe) {
          debugPrint("Error while writing Splitter file " + tempdir+splitter_fname+".java \n");
          debugPrint(ioe.toString());
        }
      }
    }
  }

  /**
   * Find a program point in daikon whose name matches "ppt_name".
   * ppt_name is usually of the form "MethodName.functionName"
   **/
  static PptTopLevel find_corresponding_ppt(String ppt_name, PptMap all_ppts) {
    // look for corresponding EXIT ppt. This is because the exit ppt usually has
    // more relevant variables in scope (eg. return, hashcodes) than the enter.
    Vector corresponding_ppts = new Vector();
    int index = -1;
    if ((index = ppt_name.indexOf("OBJECT")) == -1) {
      ppt_name = qm(ppt_name) + ".*EXIT";
    } else {
      if (ppt_name.length() > 6)
        ppt_name = ppt_name.substring(0, index-1) + ":::OBJECT";
    }
    try {
      Pattern ppt_pattern = re_compiler.compile(ppt_name);
      Iterator ppt_itor = all_ppts.iterator();

      while (ppt_itor.hasNext()) {
        String name = ((PptTopLevel)ppt_itor.next()).name;
        if (re_matcher.contains( name, ppt_pattern)) {
          //return more than one? do more than one match??
          return all_ppts.get(name);
        }
      }
    } catch (MalformedPatternException e) {
      debugPrint(e.toString() + " while matching " + ppt_name);
    }
    return null;
  }


  static Pattern find_orig_pattern;
  static Perl5Substitution orig_subst;
  static {
    try {
      //this regex pattern is used to search for "orig" variable names.
      //it replaces orig(varname) with orig_varname
      find_orig_pattern = re_compiler.compile("\\orig\\s*\\(\\s*(\\S*?)\\s*\\)");
      orig_subst = new Perl5Substitution("orig_$1", Perl5Substitution.INTERPOLATE_ALL);
    } catch (MalformedPatternException me) {
      System.err.println("Error while compiling regular expresssion find_orig_pattern in SplitterFactory");
    }
  }

  static String replace_orig(String orig_string) {
    String result;
    result = Util.substitute(re_matcher, find_orig_pattern, orig_subst, orig_string, Util.SUBSTITUTE_ALL);
    return result;
  }

  /**
   * extract the in-scope variables and their corresponding types from the
   * program point
   **/
  static Vector[] get_params_and_types(PptTopLevel ppt) {

    Vector parameters = new Vector();
    Vector types = new Vector();

    VarInfo[] var_infos = ppt.var_infos;
    for (int i = 0; i < var_infos.length; i++) {
      //we don't want hashcodes. We just want the variable values
      if (var_infos[i].file_rep_type == ProglangType.HASHCODE) {
        parameters.addElement(var_infos[i].name.name().trim());
        types.addElement("int");
        continue;
      }
      String temp = var_infos[i].name.name().trim();
      if (temp.endsWith(".class"))
        continue;
      if (temp.endsWith("[]")) {
        //strip off the brackets and search for the variable name in the test string.
        temp = temp.substring(0, temp.length() - 2);
      }
      parameters.addElement(temp);
      //do rep_type changes here.
      if (var_infos[i].type.format().trim().equals("char[]")) {
        //if char[], we need to treat as a String in Daikon
        types.addElement ("char[]");
      } else if (var_infos[i].type.format().trim().equals("boolean")) {
        types.addElement("boolean");
      } else {
        types.addElement(var_infos[i].rep_type.format().trim());
      }
    }
    Vector[] return_vector = {parameters, types};
    return return_vector;
  }

  // this regex pattern is used to search for the variable names of arguments
  // inside function calls.
  static Pattern arg_pattern;
  static {
    try {
      arg_pattern = re_compiler.compile("(\\S+)\\s*\\((.*)\\)");
    } catch (MalformedPatternException me) {
      System.err.println("Error while compiling regular expresssion arg_pattern in SplitterFactory");
    }
  }



  /**
   * Get the function names and arguments of the functions to be replaced.
   * For example the function "Max(int a, int b)" designated to be replaced
   * by "a > b ? a : b" with will be parsed into a Vector of Strings
   * containing as first element the regular expression
   * "Max\s*\(\s*(.*),\*(.*)\)" which matches any function call of Max with
   * two arguments. The second element of the return value is a Vector
   * of the arguments (a, b). The third element is the expression
   * "a > b ? a : b"
   *
   * @return heterogenous vector, where 0th element (mod 3) is type
   * String, 1st element (mod 3) is type String[], and 2nd element
   * (mod 3) is type String.
   **/
  static Vector get_fnames_and_args(Vector replace // [String] but paired
                                    )
  {
    Vector replace_data = new Vector();
    try {
      for (int i = 0; i < replace.size(); i+=2) {
        String replace_function = (String)replace.elementAt(i); // eg Max(int a, int b)
        PatternMatcherInput replace_function_pattern = new PatternMatcherInput(replace_function);
        if (re_matcher.contains(replace_function_pattern, arg_pattern)) {
          MatchResult result = re_matcher.getMatch();
          String function_name = result.group(1);  // Max
          String arguments = result.group(2); // int a, int b
          // the arguments are in the form "type1 name1, type name2, type3 name3, ..."
          StringTokenizer split_args = new StringTokenizer(arguments.trim(), ",");
          Vector tempargs = new Vector();
          while (split_args.hasMoreTokens()) {
            String arg = split_args.nextToken().trim();
            // each argument is now of the form "type name" after splitting using ","
            StringTokenizer extract_argname = new StringTokenizer(arg);
            extract_argname.nextToken(); // throw away the type of the argument
            tempargs.addElement(extract_argname.nextToken().trim()); // the argument name
          }
          replace_data.addElement(function_name);
          replace_data.addElement((String[])tempargs.toArray(new String[0]));
          replace_data.addElement(replace.elementAt(i+1));
        }
      }
    } catch (ClassCastException e) {
      System.out.println(e.toString());
    }
    // create the regular expression which will be used to search for each
    // occurrence of the function call
    for (int i = 0; i < replace_data.size(); i+=3) {
      String fname = (String) replace_data.elementAt(i);
      int num_args = ((String[])replace_data.elementAt(i+1)).length;
      fname = fname + "\\s*\\(\\s*";
      for (int j = 0; j < num_args; j++) {
        fname = fname + "\\s*(\\S*)";
        if (j+1 < num_args) {
          fname = fname+"\\s*,";
        }
      }
      fname = fname + "\\s*\\)";
      replace_data.setElementAt(fname, i);
    }

    return replace_data;
  }

  /**
   * Replace the function call with its replacement expression.  In
   * doing this, do a replacement of the arguments too.
   *
   * @param replace_data heterogenous vector, where 0th element (mod
   * 3) is type String, 1st element (mod 3) is type String[], and 2nd
   * element (mod 3) is type String.
   **/
  static String replace_condition(String condition,
                                  Vector replace_data // see javadoc for type
                                  )
  {
    Pattern replace_expr_pattern;
    for (int i = 0; i < replace_data.size(); i+=3) {
      try {
        // search for the expression
        String temp = (String)replace_data.elementAt(i);
        replace_expr_pattern = re_compiler.compile( temp );
        PatternMatcherInput input = new PatternMatcherInput(condition);
        while (re_matcher.contains(input, replace_expr_pattern)) {
          MatchResult result = re_matcher.getMatch();
          Perl5Substitution temp_subst = new Perl5Substitution("#", Perl5Substitution.INTERPOLATE_ALL);
          condition = Util.substitute(re_matcher, replace_expr_pattern, temp_subst, condition, 1);
          String[] arguments = (String[])replace_data.elementAt(i+1);
          String replacement = (String) replace_data.elementAt(i+2);
          // replace the arguments and replace them
          for (int j = 1; j < result.groups(); j++) {
            Perl5Substitution arg_subst = new Perl5Substitution(result.group(j));
            Pattern argument_pattern = re_compiler.compile(arguments[j-1]);
            replacement = Util.substitute(re_matcher, argument_pattern, arg_subst, replacement, Util.SUBSTITUTE_ALL);
          }
          // substitute back into the condition
          replacement = "(" + replacement + ")";
          Perl5Substitution replace_subst =
            new Perl5Substitution(replacement, Perl5Substitution.INTERPOLATE_ALL);
          condition = Util.substitute(re_matcher, re_compiler.compile("#"), replace_subst, condition, 1);
        }
      } catch (MalformedPatternException e) {
        debugPrint( e.toString() +" while performing replacement on condition " + condition);
      }
    }
    return condition;
  }

  /**
   * fix the variable names (of arrays) in the condition to match the declared
   * variable names in the Splitter. Anytime you see a <varname>.length or
   * <varname>[] being used in a condition, add a "_array" to the varname.
   **/
  static String distinguish_arraynames_from_hashCodes_in_teststring(String varname, String test_string) {

    String dot_length; // The regular expression used to search for "<arrayname>.length"
    String bracket;  // the regular expression used to search for "<arrayname>[]"

    if (varname.startsWith("this.")) {
        bracket = "(" + varname + "|" + varname.substring(5) ;
        dot_length = "(" + varname + "|" + varname.substring(5);
    } else {
        bracket = "(" + varname ;
        dot_length = "(" + varname ;
    }

    bracket = bracket + ")(\\s*\\[[^\\]]*)";
    dot_length = dot_length  + ")\\.length";

    // try performing the substitution on every condition
    Pattern brack_pattern, length_pattern;
    Perl5Substitution brack_subst, length_subst;
    try {
      brack_pattern = re_compiler.compile(bracket);
      brack_subst = new Perl5Substitution("$1_array$2", Perl5Substitution.INTERPOLATE_ALL);

      if (re_matcher.contains(test_string, brack_pattern)) {
        test_string =
          Util.substitute(re_matcher, brack_pattern, brack_subst, test_string, Util.SUBSTITUTE_ALL);
      }

      length_pattern = re_compiler.compile(dot_length);
      length_subst = new Perl5Substitution("$1_array.length", Perl5Substitution.INTERPOLATE_ALL);

      if (re_matcher.contains(test_string, length_pattern)) {
        test_string =
          Util.substitute(re_matcher, length_pattern, length_subst, test_string, Util.SUBSTITUTE_ALL);
      }
    } catch (MalformedPatternException e) {
      debugPrint(e.toString() + "\n while performing substitution on teststring " + test_string + "\n");
    }
    return test_string;
  }

  // Short name for purposes of abbreviation.
  private static String qm(String exp) {
    return re_compiler.quotemeta(exp);
  }

  private static String delimit(String exp) {
    return delimit_end(delimit_beginning(exp));
  }

  private static String delimit_end(String exp) {
    if ((exp == null) || exp.equals(""))
      return exp;
    char lastchar = exp.charAt(exp.length()-1);
    if (Character.isLetterOrDigit(lastchar)
        || lastchar == '_') {
      exp = exp + "\\b";
    }
    return exp;
  }

  private static String delimit_beginning(String exp) {
    if ((exp == null) || exp.equals(""))
      return exp;
    char firstchar = exp.charAt(0);
    if (Character.isLetterOrDigit(firstchar)
        || firstchar == '_') {
      exp = "\\b" + exp;
    }
    return exp;
  }

  /**
   * Find the variables at the program point which apply to this splitter and
   * substitute their correct form in the test string. For example, change
   * this.<varname> in the test_string to this_<varname>
   **/
  static String find_applicable_variables(String[] params, String[] param_names,
                                          String test_string, String class_name )
  {

    String orig_test_string = test_string;
    for (int i = 0; i < params.length; i++) {

        Pattern param_pattern;
        // some instrumented variables start with "this." whereas the "this" is
        // not always used in the test string. Eg. instrumented variable is
        // "this.myArray", but the condition test is "myArray.length == 0". In
        // such a situation, search the test_string for this.myArray or myArray
        // and change the test string to this_myArray.length == 0.
        try {
          if (params[i].charAt(0) == '*') {
            param_names[i] = "star_" + params[i].substring(1);
            param_pattern = re_compiler.compile(delimit(qm(params[i])));
          } else if (params[i].startsWith("orig(*")) {
            param_names[i] = "orig_star_" + params[i].substring(6, params[i].length() - 1) + "_";
            param_pattern = re_compiler.compile(delimit_end("orig\\(\\*" + qm(params[i].substring(6))));
          } else if (params[i].startsWith("this.")) {
            String params_minus_this = params[i].substring(5);
            // for example for the variable 'this.myArray', we will be searching
            // the condition for the regex "myArray|this.myArray" and replacing
            // it with this_myArray as declared in the Splitter.
            param_pattern = re_compiler.compile("(" + delimit(qm(params[i])) + "|" + delimit(qm(params_minus_this)) + ")");
          } else if (!class_name.equals("") && params[i].startsWith(class_name)) {
            param_pattern = re_compiler.compile(qm(params[i]) + "|" + qm(params[i].substring(class_name.length())));
          } else if (params[i].startsWith("orig")) {
            //we've already substituted for example orig(this.Array) with "orig(this_theArray)",
            //so search for "orig(this_theArray)" in the test_string
            String temp = param_names[i].replace('.','_');
            String search_string = "orig\\s*\\(\\s*" + temp.substring(5) + "\\s*\\)";
            if (temp.length() > 10) {
              search_string = search_string + "|" + "orig\\s*\\(\\s*" + qm(temp.substring(10)) + "\\s*\\)";
            }
            param_pattern = re_compiler.compile(search_string);
          } else if (params[i].charAt(0) == '$') {
            //remove unwanted characters from the param name. These confuse the regexp.
            //(find a better solution for arbitrary characters at arbitrary locations)
            param_pattern = re_compiler.compile(delimit(qm(params[i].substring(1))));
          } else {
            param_pattern = re_compiler.compile(delimit(qm(params[i])));
          }

          Perl5Substitution param_subst = new Perl5Substitution(param_names[i], Perl5Substitution.INTERPOLATE_ALL);
          PatternMatcherInput input = new PatternMatcherInput(orig_test_string);
          // remove any parameters which are not used in the condition
          if (re_matcher.contains(input, param_pattern)) {
            test_string = Util.substitute(re_matcher, param_pattern, param_subst, test_string, Util.SUBSTITUTE_ALL);
            //while (re_matcher.contains(input, param_pattern)) {
            //test_string = Util.substitute(re_matcher, param_pattern, param_subst, test_string, Util.SUBSTITUTE_ALL);
            //}
          } else {
            params[i] = null;
          }
        } catch (MalformedPatternException e) {
          debugPrint(e.toString());
        }
      }
      {
        // Change "this_Daikon.this_" to "this_"
        int this_loc = test_string.indexOf("this_Daikon.this_");
        while (this_loc != -1) {
          test_string = (test_string.substring(0, this_loc)
                         + test_string.substring(this_loc + 12));
          this_loc = test_string.indexOf("this_Daikon.this_");
        }
      }
      return test_string;
  }

  static Pattern find_index_pattern;
  static {
    try {
      // this regex pattern is used to search for variables which might act as array indices.
      // ie. variable names inside square brackets
      find_index_pattern = re_compiler.compile("\\[\\s*([^\\])]*)\\s*\\]");
    } catch (MalformedPatternException me) {
      System.err.println("Error while compiling regular expresssion find_index_pattern in SplitterFactory");
    }
  }

  static Pattern blank_line;
  static {
    try {
      blank_line  = re_compiler.compile("^\\s*$");
    } catch (MalformedPatternException me) {
      System.out.println("Error while compiling regular expression " + me.toString());
    }
  }

    /**
     * Find all variables which are used to index into arrays and change their
     * type to "int" (Most numeric variables have type long in Daikon, apart from
     * array indices which are represented as int). To find array index variables,
     * Find all occurences of arrayname[varname] in the test_string and
     * mark the variable(s) in the square brackets as array index variables.
     */
    static void identify_array_index_variables_as_int(String condition, String[] params, String[] types) {
        Vector arrayIndexVariables = new Vector();
        try {

            PatternMatcherInput input = new PatternMatcherInput(condition);
            while (re_matcher.contains(input, find_index_pattern)) {
                MatchResult result = re_matcher.getMatch();
                String possible_varname = result.group(1);
                // if we have say myarray[ i + j ], we have to identify both i and j
                // as array index variables and treat them as integers.
                Pattern operator_pattern = re_compiler.compile("[+=!><-]"); // split on this pattern
                Vector tempIndices = new Vector();
                Util.split(tempIndices, re_matcher, operator_pattern, possible_varname);
                for (int i = 0; i < tempIndices.size(); i++) {
                  arrayIndexVariables.addElement(((String)tempIndices.elementAt(i)).trim());
                }
            }
        } catch (MalformedPatternException e) {
          debugPrint(e.toString());
        }

        for (int i = 0; i < arrayIndexVariables.size(); i++) {
          for (int j = 0; j < params.length; j++) {
            String variable = (String) arrayIndexVariables.elementAt(i);
            if (params[j] == null) continue;
            if (variable.equals(params[j])) {
              types[j] = "int_index";
            } else if (params[j].startsWith("this.") && (params[j].substring(5)).equals(variable)) {
              types[j] = "int_index";
            }
          }
        }
    }

  static Pattern null_pattern;
  static {
    try {
      null_pattern = re_compiler.compile("(!|=)=\\s*null");
    } catch (MalformedPatternException e) {
      debugPrint("Error compiling regex (!|=)=\\s*null");
    }
  }

  // Replace "== null" by "== 0".
  /**
   * Daikon represents the value of null as zero (for some classes of objects).
   * Replace all occurences of equality tests against null with tests against 0
   * in the condition.
   **/
  static String perform_null_substitution(String test_string) {
    Perl5Substitution null_subst = new Perl5Substitution(" $1= 0", Perl5Substitution.INTERPOLATE_ALL);
    if (re_matcher.contains(test_string, null_pattern)) {
      test_string =
        Util.substitute(re_matcher, null_pattern, null_subst, test_string, Util.SUBSTITUTE_ALL);
    }
    return test_string;
  }


  /**
   * get the name of the temporary directory. This is where the Splitters are created.
   **/
  static String getTempdirName() {

    try {
      String fs = File.separator;
      String path = System.getProperty("java.io.tmpdir") + fs + System.getProperty("user.name") + fs;
      File pathFile =  new File(path);
      pathFile.mkdirs();
      File tmpfile = File.createTempFile("daikon_", "_", pathFile);
      File splitdir = new File(tmpfile.getPath() + "Split");
      tmpfile.delete();
      splitdir.mkdirs();
      if (dkconfig_delete_splitters_on_exit) {
        splitdir.deleteOnExit();
      }
      if (splitdir.exists() && splitdir.isDirectory()) {
        tempdir = splitdir.getPath() + File.separator;
      } else {
        tempdir = "";
      }
    } catch (IOException e) {
      debugPrint(e.toString());
    }
    return tempdir;
  }

  /**
   * Print out a message if the debugPptSplit variable is set to "true"
   **/
  static void debugPrint(String s) {
    //System.out.println(s);
    Global.debugSplit.debug (s);
  }

  /**
   * Declare the VarInfo for the parameter <parameter> of type <type> in the
   * Java source of the Splitter. For example, for a variable named "myint"
   * of type "int", it would print "VarInfo myint_varinfo" and for an array
   * "myarr", it would print "VarInfo myarr_array_varinfo"
   **/
  static void print_parameter_declarations(StringBuffer splitter_source, String parameter, String type) {
    if (type.equals("int")) {
      splitter_source.append("  VarInfo " + parameter + "_varinfo; \n");
    } else if (type.equals("int_index")) {
      splitter_source.append("  VarInfo " + parameter + "_varinfo; \n");
    } else if (type.equals("char[]")) {
      splitter_source.append("  VarInfo " + parameter + "_varinfo; \n");
    } else if (type.endsWith("[]")) {
      splitter_source.append("  VarInfo " + parameter + "_array_varinfo; \n");
    } else if (type.equals("java.lang.String") || type.equals("String")) {
      splitter_source.append("  VarInfo " + parameter + "_varinfo; \n");
    } else if (type.equals("boolean")) {
      splitter_source.append("  VarInfo " + parameter + "_varinfo; \n");
    } else {
      debugPrint("Can't deal with this type " + type + " declared in Splitter file");
    }
  }

  /**
   * Print the test() method of the splitter
   **/
  static String print_test_method(StringBuffer splitter_source, String[] param_names,
                                        String[] params, String[] all_types, String test_string ) {

    splitter_source.append("  public boolean test(ValueTuple vt) { \n");
    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) continue;
      String type = all_types[i];
      String parameter = param_names[i];
      if (type.equals("int_index")) {
        splitter_source.append("   int " + parameter + " = "
                               + parameter + "_varinfo.getIndexValue(vt); \n");
      } else if (type.equals("int")) {
        splitter_source.append("    long " + parameter + " = "
                               + parameter + "_varinfo.getIntValue(vt); \n");
      } else if (type.equals("boolean")) {
        //we get the boolean as an int
        splitter_source.append("    boolean " + parameter + " = (" + parameter
                               + "_varinfo.getIntValue(vt) > 0 ? true : false ); \n");
      } else if (type.equals("int[]")) {
        splitter_source.append("  long[] " + parameter + "_array = " + parameter
                               + "_array_varinfo.getIntArrayValue(vt); \n");
      } else if (type.equals("String") || type.equals("java.lang.String") || type.equals("char[]")) {
        splitter_source.append("    String " + parameter + " = "
                               + parameter + "_varinfo.getStringValue(vt); \n");
      } else if (type.equals("String[]") || type.equals("java.lang.String[]")) {
        splitter_source.append("    String[] " + parameter + "_array = "
                               + parameter + "_array_varinfo.getStringArrayValue(vt); \n");
      } else {
        debugPrint("Can't deal with this type " + type + " declared in Splitter File");
      }
    }

    splitter_source.append("    return( " + test_string + " ); \n  }\n");
    return test_string;
  }
}
