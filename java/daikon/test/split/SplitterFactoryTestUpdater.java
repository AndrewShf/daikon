package daikon.test.split;

import daikon.split.*;
import java.util.*;
import java.io.*;
import utilMDE.*;
import daikon.*;

/**
 * This class's main method can be used to update both the
 * target files of SplitterFactoryTest and the code of the
 * SplitterFactoryTest it self.
 *
 * To use this program to update SplitterFactoryTest
 * and the target files, run "rm *.java.goal" while in the
 * target directory ($inv/java/daikon/test/split/target).
 * Then simply run the main method with out any arguments
 * in the $INV/java directory. After running the
 * main method one should re-compile the SplitterFactoryTest.
 *
 * To add additional tests to this test program, place the .spinfo
 * and decls files into the target directory then add a call to
 * generateSplitters with the new files.  generateSplitters is
 * overloaded; therefore if there are only one .spinfo file and
 * only decls file then only the names of those two files need to be
 * used as arguments to generateSplitters.  However, if there are
 * multiple .spinfo files or multiple decls files, the file names
 * should be placed into Lists then passed to generateSplitters.
 * See generateSplitters for more information.
 */
public class SplitterFactoryTestUpdater {
  public static java.lang.Runtime commander = java.lang.Runtime.getRuntime();
  private static String lineSep = System.getProperty("line.separator");
  private static String targetDir = "daikon/test/split/targets/";
  private static String splitDir = "daikon/test/split/";

  private static List /*List of Strings*/ spinfoFileLists = new ArrayList();
  private static List /*List of Strings*/ declsFileLists = new ArrayList();
  private static List /*String*/ classNames = new ArrayList();

  private static File tempDir = null;

  private SplitterFactoryTestUpdater() {} //blocks public constructor

  /**
   * If one has changed the test cases used below, for best results run
   * "rm *.java.goal" while in the targets directory before running this
   * method. Creates new splitter java files, move the new files into
   * target directory, rewrites the code of SplitterFactoryTest
   * to use the new files.  One should recompile SplitterFactoryTest
   * after running this method.
   * @param args are ignored.
   */
  public static void  main(String[] args) {
    generateSplitters("StreetNumberSet.spinfo", "StreetNumberSet.decls");
    generateSplitters("Fib.spinfo", "Fib.decls");
    generateSplitters("QueueAr.spinfo", "QueueAr.decls");
    generateSplitters("muldiv.spinfo", "BigFloat.decls");
    moveFiles();
    writeTestClass();
    deleteDir(tempDir); // file's delete requires a dir be empty
  }

  /**
   * This is a short-cut method if only one spinfo file and only
   * one decls files is to be used.  See generateSplitters(List, List).
   */
  private static void generateSplitters(String spinfoFile, String declsFile) {
    List spinfo = new ArrayList();
    spinfo.add(spinfoFile);
    List decls = new ArrayList();
    decls.add(declsFile);
    generateSplitters(spinfo, decls);
  }

  /**
   * Generates the splitter java files in the tempDir.
   * @param spinfos the spinfo files that should be used in generating
   *  the splitter java files.
   * @param decls the decls files that should be used in generating the
   *  splitter java files.
   */
  private static void generateSplitters(List /*String*/ spinfos,
                                        List /*String*/ decls) {
    Set /*File*/ declsFileSet = new HashSet();
    List /*String*/ spinfoFiles = new ArrayList();
    for (int i = 0; i < spinfos.size(); i++) {
      String spinfoFile = (String) spinfos.get(i);
      spinfoFile = targetDir + spinfoFile;
      spinfoFiles.add(spinfoFile);
    }
    spinfoFileLists.add(spinfoFiles);
    for (int i = 0; i < decls.size(); i++) {
      String declsFile = (String) decls.get(i);
      declsFile = targetDir + declsFile;
      declsFileSet.add(new File(declsFile));
    }
    declsFileLists.add(new ArrayList(declsFileSet));
    try {
      PptMap allPpts = FileIO.read_declaration_files(declsFileSet);
      Dataflow.init_partial_order(allPpts);
      Dataflow.create_combined_exits(allPpts);
      Daikon.dkconfig_suppressSplitterErrors = true;
      for (int i = 0; i < spinfoFiles.size(); i++) {
        //create the java files
        String spinfoFile = (String) spinfoFiles.get(i);
        SplitterObject[][] splitters =
          SplitterFactory.read_spinfofile( new File(spinfoFile), allPpts);
      }
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Moves the generated splitter files from the tempDir to the target Dir.
   */
  private static void moveFiles() {
    File tempDir = new File(SplitterFactory.getTempDir());
    SplitterFactoryTestUpdater.tempDir = tempDir;
    String[] fileNames = tempDir.list();
    for (int i = 0; i < fileNames.length; i++) {
      if (fileNames[i].endsWith(".java")) {
        StringBuffer command = new StringBuffer();
        String fileName  = fileNames[i];
        moveFile(tempDir.getPath() + "/" + fileName,
                 targetDir + fileName + ".goal");
        String className =
          fileName.substring(fileName.lastIndexOf('/') + 1 ,
                             fileName.length()-".java".length());
        classNames.add(className);
      }
    }
  }

  private static void moveFile(String from, String to) {
      (new File(from)).renameTo(new File(to));
  }

  /**
   * Writes the new code for "SplitterFactoryTest.java".
   */
  private static void writeTestClass() {
    String code = getTestClassText();
    try {
      BufferedWriter writer = UtilMDE.BufferedFileWriter(splitDir + "SplitterFactoryTest.java");
      writer.write(code);
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a String of the new text for the SplitterFactoryTest class.
   */
  private static String getTestClassText() {
    StringBuffer code = new StringBuffer();

    code.append("// ***** This file is automatically generated by SplitterFactoryTestUpdater.java" + lineSep);
    code.append(lineSep);
    code.append("package daikon.test.split;" + lineSep);
    code.append(lineSep);
    code.append("import junit.framework.*; " + lineSep);
    code.append("import daikon.split.*;"   + lineSep);
    code.append("import daikon.*; " + lineSep);
    code.append("import java.util.*; " + lineSep);
    code.append("import java.io.*;" + lineSep);
    code.append("import utilMDE.*;" + lineSep);
    code.append("import gnu.getopt.*;" + lineSep);
    code.append(lineSep);
    code.append("/**" + lineSep);
    code.append(" * THIS CLASS WAS GENERATED BY SplitterFactoryTestUpdater." + lineSep);
    code.append(" * Therefore, it is a bad idea to directly edit this class's" +lineSep);
    code.append(" * code for all but temporary reasons.  Any permanent changes " + lineSep);
    code.append(" * should be made through SplitterFactoryUpdater." + lineSep);
    code.append(" * " + lineSep);
    code.append(" * This class contains regression tests for the SplitterFactory class." + lineSep);
    code.append(" * The tests directly test the java files produced by the" + lineSep);
    code.append(" * read_spinfofile method by comparing them against goal files." + lineSep);
    code.append(" * Note that it is normal for some classes not to compile during this test." + lineSep);
    code.append(" * " + lineSep);
    code.append(" * These tests assume that the goal files are contained in the directory: " + lineSep);
    code.append(" * \"daikon/test/split/targets\"" + lineSep);
    code.append(" * These tests ignore extra white spaces." + lineSep);
    code.append(" */" + lineSep);
    code.append(" public class SplitterFactoryTest extends TestCase { " + lineSep);
    code.append("  // Because the SplitterFactory sequentially numbers the " + lineSep);
    code.append("  // java files it produces, changing the order that the setUpTests " + lineSep);
    code.append("  // commands are run will cause the tests to fail." + lineSep);
    code.append("  //" + lineSep);
    code.append("  // javaFiles are the names of the files created by SplitterFactory" + lineSep);
    code.append("  // targetFiles are the names of the files that the created files" + lineSep);
    code.append("  // are to by checked for equality." + lineSep);
    code.append(lineSep);
    code.append("  private static String targetDir = \"daikon/test/split/targets/\";" + lineSep);
    code.append(lineSep);
    code.append("  private static String tempDir = null;" + lineSep);
    code.append(lineSep);
    code.append("  private static boolean saveFiles = false;" + lineSep);
    code.append(lineSep);
    code.append("    private static final String lineSep = System.getProperty(\"line.separator\");" + lineSep);
    code.append(lineSep);
    code.append("    private static String usage =" + lineSep);
    code.append("      UtilMDE.join(new String[] {" + lineSep);
    code.append("        \"Usage:  java daikon.tools.CreateSpinfo FILE.java ...\"," + lineSep);
    code.append("        \"  -s save  Do not delete the splitter java files from the temp directory\"," + lineSep);
    code.append("        \"  -h       Display this usage message\"," + lineSep);
    code.append("      }," + lineSep);
    code.append("      lineSep);" + lineSep);
    code.append(lineSep);
    code.append("  public static void main(String[] args) {" + lineSep);
    code.append("      Getopt g = " + lineSep);
    code.append("        new Getopt(\"daikon.test.split.SplitterFactoryTest\", args, \"hs\");" + lineSep);
    code.append("      int c;" + lineSep);
    code.append("      while ((c = g.getopt()) != -1) {" + lineSep);
    code.append("        switch(c) {" + lineSep);
    code.append("        case 's':" + lineSep);
    code.append("          saveFiles = true;" + lineSep);
    code.append("          break;" + lineSep);
    code.append("        case 'h':" + lineSep);
    code.append("          System.out.println(usage);" + lineSep);
    code.append("          System.exit(1);" + lineSep);
    code.append("          break;" + lineSep);
    code.append("        case '?':" + lineSep);
    code.append("          break;" + lineSep);
    code.append("        default:" + lineSep);
    code.append("          System.out.println(\"getopt() returned \" + c);" + lineSep);
    code.append("          break;" + lineSep);
    code.append("        }" + lineSep);
    code.append("      }" + lineSep);
    code.append("    junit.textui.TestRunner.run(suite());" + lineSep);
    code.append("  }" + lineSep);
    code.append(lineSep);

    appendSetUpTest(code);

    code.append(lineSep);
    code.append("  public SplitterFactoryTest(String name) {" + lineSep);
    code.append("    super(name);" + lineSep);
    code.append("  }" + lineSep);
    code.append(lineSep);
    code.append("  /**" + lineSep);
    code.append("   * Sets up the test by generating the needed splitter java files. " + lineSep);
    code.append("   */" + lineSep);
    code.append("  private static void createSplitterFiles(List /*String*/ spinfos, List /*String*/ decls) {" + lineSep);
    code.append("    List declsFiles = new ArrayList();" + lineSep);
    code.append("    for (int i = 0; i < decls.size(); i++) {" + lineSep);
    code.append("      declsFiles.add(new File((String) decls.get(i)));" + lineSep);
    code.append("    }" + lineSep);
    code.append("    try {" + lineSep);
    code.append("      PptMap allPpts = FileIO.read_declaration_files(declsFiles);" + lineSep);
    code.append("      Dataflow.init_partial_order(allPpts);" + lineSep);
    code.append("      Dataflow.create_combined_exits(allPpts);" + lineSep);
    code.append("      if (saveFiles) {" + lineSep);
    code.append("        SplitterFactory.dkconfig_delete_splitters_on_exit = false;" + lineSep);
    code.append("      }" + lineSep);
    code.append("     Daikon.dkconfig_suppressSplitterErrors = true;" + lineSep);
    code.append("      for (int i = 0; i < spinfos.size(); i++) {" + lineSep);
    code.append("        //create the java files" + lineSep);
    code.append("        String spinfoFile = (String) spinfos.get(i);" + lineSep);
    code.append("        SplitterObject[][] splitters =" + lineSep);
    code.append("          SplitterFactory.read_spinfofile(new File(spinfoFile), allPpts);" + lineSep);
    code.append("        tempDir = SplitterFactory.getTempDir();" + lineSep);
    code.append("      }" + lineSep);
    code.append("    } catch(IOException e) {" + lineSep);
    code.append("        throw new RuntimeException(e);" + lineSep);
    code.append("    }" + lineSep);
    code.append("   }" + lineSep);
    code.append(lineSep);
    code.append("  /**" + lineSep);
    code.append("   * Returns true iff files are the same. (ignoring extra white space)" + lineSep);
    code.append("   */ " + lineSep);
    code.append("  private static boolean equalFiles(String file1, String file2) { " + lineSep);
    code.append("    try {" + lineSep);
    code.append("      LineNumberReader reader1 = UtilMDE.LineNumberFileReader(file1);" + lineSep);
    code.append("      LineNumberReader reader2 = UtilMDE.LineNumberFileReader(file2);" + lineSep);
    code.append("      String line1 = reader1.readLine().trim();" + lineSep);
    code.append("      String line2 = reader2.readLine().trim();" + lineSep);
    code.append("      while(line1 != null && line2 != null) { " + lineSep);
    code.append("        if (! (line1.trim().equals(line2.trim()))) {" + lineSep);
    code.append("          return false;" + lineSep);
    code.append("        }" + lineSep);
    code.append("        line1 = reader1.readLine();" + lineSep);
    code.append("        line2 = reader2.readLine();" + lineSep);
    code.append("      }" + lineSep);
    code.append("      if (line1 == null && line2 == null) {" + lineSep);
    code.append("        return true;" + lineSep);
    code.append("      }" + lineSep);
    code.append("        return false;" + lineSep);
    code.append("      } catch (IOException e) { " + lineSep);
    code.append("        throw new RuntimeException(e); " + lineSep);
    code.append("      }" + lineSep);
    code.append("  }" + lineSep);
    code.append(lineSep);
    code.append("  /** " + lineSep);
    code.append("   * Deletes the directory at dirName and all its files; there may be no" + lineSep);
    code.append("   * directory is dirName." + lineSep);
    code.append("   */" + lineSep);
    code.append("  private static void deleteDir(String dirName) {" + lineSep);
    code.append("    File dir = new File(dirName);" + lineSep);
    code.append("    File[] files = dir.listFiles();" + lineSep);
    code.append("    for (int i = 0; i < files.length; i++) {" + lineSep);
    code.append("      files[i].delete();" + lineSep);
    code.append("    }" + lineSep);
    code.append("    dir.delete();" + lineSep);
    code.append("  }" + lineSep);
    code.append(lineSep);

    appendTests(code);

    code.append("  public static Test suite() {" + lineSep);
    code.append("    setUpTests();" + lineSep);
    code.append("    TestSuite suite = new TestSuite();" + lineSep);

    appendSuite(code);

    code.append("    return suite;" + lineSep);
    code.append( "  } " + lineSep);
    code.append(lineSep);
    code.append( "}" + lineSep);

    return code.toString();
  }

  /**
   * Appends the code to write the static block of code to code.
   * This code is used by the SplitterFactoryTest to set up the
   * needed files to run the tests on.
   */
  public static void appendSetUpTest(StringBuffer code) {
    code.append("  private static void setUpTests() {" + lineSep);
    code.append("    List /*String*/ spinfoFiles;" + lineSep);
    code.append("    List /*String*/ declsFiles;" + lineSep);
    for (int i = 0; i < spinfoFileLists.size(); i++) {
      List spinfoFiles = (List) spinfoFileLists.get(i);
      code.append("    spinfoFiles = new ArrayList();" + lineSep);
      for (int j = 0; j < spinfoFiles.size(); j++) {
        code.append("    spinfoFiles.add(\"");
        code.append(spinfoFiles.get(j));
        code.append("\");" + lineSep);
      }
      List declsFiles = (List) declsFileLists.get(i);
      code.append("    declsFiles = new ArrayList();" + lineSep);
      for (int j = 0; j < declsFiles.size(); j++) {
        code.append("    declsFiles.add(\"");
        code.append(declsFiles.get(j));
        code.append("\");" + lineSep);
      }
      code.append("    createSplitterFiles(spinfoFiles, declsFiles);" + lineSep);
    }
    code.append("  }" + lineSep);
  }

  /**
   * Appends the code that executes the test in SplitterFactoryTest
   * to code.
   */
  private static void appendTests(StringBuffer code) {
    for (int i = 0; i < classNames.size(); i++) {
      String className = (String) classNames.get(i);
      code.append("  public static void ");
      code.append("test" + className);
      code.append("() {");
      code.append(lineSep);
      code.append("    assertTrue(equalFiles(" + lineSep);
      code.append("      tempDir +\"");
      code.append(className);
      code.append(".java\", ");
      code.append(lineSep);
      code.append("      \"");
      code.append(targetDir);
      code.append(className);
      code.append(".java.goal\"));");
      code.append(lineSep);
      code.append("  }" + lineSep);
      code.append(lineSep);
    }
  }

  /**
   * Appends the code that generates the test suite in SplitterFactoryTest
   * to code.
   */
  private static void appendSuite(StringBuffer code) {
    for (int i = 0; i < classNames.size(); i++) {
      String className = (String) classNames.get(i);
      code.append("    suite.addTest(new SplitterFactoryTest(\"");
      code.append("test" + className);
      code.append("\"));");
      code.append(lineSep);
    }
  }

  /**
   *@requires there are no directory in the directory given by dirName
   *@modifies the directory are dirName and all its files
   *@effects deletes the directory at dirName and all its files
   */
  private static void deleteDir(File dir) {
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
    dir.delete();
  }

}
