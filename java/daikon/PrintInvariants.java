package daikon;

import java.util.*;
import java.io.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import utilMDE.*;
import daikon.derive.*;
import daikon.derive.binary.*;
import daikon.inv.*;
import daikon.inv.Invariant.OutputFormat;
import daikon.inv.filter.*;
import daikon.suppress.*;
import daikon.config.Configuration;
import daikon.repair.Repair;

public final class PrintInvariants {

  private PrintInvariants() { throw new Error("do not instantiate"); }

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.

  /**
   * Print invariant classname with invariants in output of format()
   * method.  Note that this only works with the Invariant.format() method;
   * it doesn't have any effect on calls to format_using().  format() is
   * used in debugging statements, and format_using() is used in typical
   * output.
   **/
  public static boolean dkconfig_print_inv_class = false;

  /** print all invariants without any filtering **/
  public static boolean dkconfig_print_all = false;

  /**
   * Main debug tracer for PrintInvariants (for things unrelated to printing).
   **/
  public static final Logger debug = Logger.getLogger("daikon.PrintInvariants");

  /** Debug tracer for printing. **/
  public static final Logger debugRepr
    = Logger.getLogger("daikon.PrintInvariants.repr");

  /** Debug tracer for printing. **/
  public static final Logger debugPrint = Logger.getLogger("daikon.print");

  /** Debug tracer for printing modified variables in ESC/JML/DBC output. **/
  public static final Logger debugPrintModified
    = Logger.getLogger("daikon.print.modified");

  /** Debug tracer for printing equality. **/
  public static final Logger debugPrintEquality
    = Logger.getLogger("daikon.print.equality");

  /** Debug tracer for filtering. **/
  public static final Logger debugFiltering
    = Logger.getLogger("daikon.filtering");

  /** Debug tracer for variable bound information. **/
  public static final Logger debugBound  = Logger.getLogger ("daikon.bound");

  private static final String lineSep = Global.lineSep;

  /** Whether we are doing output for testing.  Used only for IOA output. **/
  public static boolean test_output = false;

  /**
   * Switch for whether to print discarded Invariants or not, default is false.
   * Activated by --disc_reason switch.
   **/
  public static boolean print_discarded_invariants = false;

  /**
   * If true, then each invariant is printed using the current
   * OutputFormat, but it's wrapped inside xml tags, along with other
   * information about the invariant.  For example, if this switch is
   * true and if the output format is JAVA, and the invariant prints
   * as "x == null", the results of print_invariant would look
   * something like:
   *
   * <INVINFO>
   * <INV> x == null </INV>
   * <SAMPLES> 100 </SAMPLES>
   * <DAIKON> x == null </DAIKON>
   * <DAIKONCLASS> daikon.inv.unary.scalar.NonZero </DAIKONCLASS>
   * <METHOD> foo() </METHOD>
   * </INVINFO>
   *
   * The above output is actually all in one line, although in this
   * comment it's broken up into multiple lines for clarity.
   *
   * Note the extra information printed with the invariant: the number
   * of samples from which the invariant was derived, the daikon
   * representation (i.e. the Daikon output format), the Java class
   * that the invariant corresponds to, and the method that the
   * invariant belongs to ("null" for object invariants).
   */
  public static boolean wrap_xml = false;

  // Fields that will be used if the --disc_reason switch is used
  private static String discClass = null;
  private static String discVars = null;
  private static String discPpt = null;

  // Avoid problems if daikon.Runtime is loaded at analysis (rather than
  // test-run) time.  This might have to change when JTrace is used.
  static { daikon.Runtime.no_dtrace = true; }

  private static String usage =
    UtilMDE.join(new String[] {
      "Usage: java daikon.PrintInvariants [OPTION]... FILE",
      "  -h, --" + Daikon.help_SWITCH,
      "      Display this usage message",
      "  --" + Daikon.suppress_redundant_SWITCH,
      "      Suppress display of logically redundant invariants.",
      "  --" + Daikon.esc_output_SWITCH,
      "      Write output in ESC format.",
      "  --" + Daikon.simplify_output_SWITCH,
      "      Write output in Simplify format.",
      "  --" + Daikon.ioa_output_SWITCH,
      "      Write output in IOA format.",
      "  --" + Daikon.java_output_SWITCH,
      "      Write output as java expressions.",
      "  --" + Daikon.jml_output_SWITCH,
      "      Write output in JML format.",
      "  --" + Daikon.dbc_output_SWITCH,
      "      Write output as Design-By-Contract format.",
      "  --" + Daikon.output_num_samples_SWITCH,
      "      Output numbers of values and samples for invariants and " +
      "program points; for debugging.",
      "  --" + Daikon.config_option_SWITCH + " config_var=val",
      "      Sets the specified configuration variable.  ",
      "  --" + Daikon.debugAll_SWITCH,
      "      Turns on all debug flags (voluminous output)",
      "  --" + Daikon.debug_SWITCH + " logger",
      "      Turns on the specified debug logger",
      "  --" + Daikon.track_SWITCH + " class<var1,var2,var3>@ppt",
      "      Print debug info on the specified invariant class, vars, and ppt",
    }, lineSep);

  public static void main(String[] args)
    throws FileNotFoundException, StreamCorruptedException,
           OptionalDataException, IOException, ClassNotFoundException {
    daikon.LogHelper.setupLogs(daikon.LogHelper.INFO);

    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.suppress_redundant_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.esc_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.simplify_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.ioa_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.test_ioa_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.java_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.jml_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.dbc_output_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.output_num_samples_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.config_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.ppt_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.track_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };
    Getopt g = new Getopt("daikon.PrintInvariants", args, "h", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          System.exit(1);
        } else if (Daikon.disc_reason_SWITCH.equals(option_name)) {
          try { PrintInvariants.discReasonSetup(g.getOptarg()); }
          catch (IllegalArgumentException e) {
            System.out.print(e.getMessage());
            System.exit(1);
          }
        } else if (Daikon.suppress_redundant_SWITCH.equals(option_name)) {
          Daikon.suppress_redundant_invariants_with_simplify = true;
        } else if (Daikon.esc_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.ESCJAVA;
        } else if (Daikon.simplify_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.SIMPLIFY;
        } else if (Daikon.repair_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.REPAIR;
        } else if (Daikon.java_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.JAVA;
        } else if (Daikon.ioa_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.IOA;
        } else if (Daikon.test_ioa_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.IOATEST;
          test_output = true;
        } else if (Daikon.jml_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.JML;
        } else if (Daikon.dbc_output_SWITCH.equals(option_name)) {
          Daikon.output_style = OutputFormat.DBCJAVA;
        } else if (Daikon.output_num_samples_SWITCH.equals(option_name)) {
          Daikon.output_num_samples = true;
        } else if (Daikon.config_SWITCH.equals(option_name)) {
          String config_file = g.getOptarg();
          try {
            InputStream stream = new FileInputStream(config_file);
            Configuration.getInstance().apply(stream);
          } catch (IOException e) {
            throw new RuntimeException("Could not open config file "
                                        + config_file);
          }
          break;
        } else if (Daikon.config_option_SWITCH.equals(option_name)) {
          String item = g.getOptarg();
          daikon.config.Configuration.getInstance().apply(item);
          break;
        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;
        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
        } else if (Daikon.track_SWITCH.equals (option_name)) {
          LogHelper.setLevel("daikon.Debug", LogHelper.FINE);
          String error = Debug.add_track (g.getOptarg());
          if (error != null) {
            System.out.println ("Error parsing track argument '"
                                + g.getOptarg() + "' - " + error);
            System.exit(1);
          }
        } else {
          throw new RuntimeException("Unknown long option received: " +
                                     option_name);
        }
        break;
      case 'h':
        System.out.println(usage);
        System.exit(1);
        break;
      case '?':
        break; // getopt() already printed an error
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    // The index of the first non-option argument -- the name of the file
    int fileIndex = g.getOptind();
    if (args.length - fileIndex != 1) {
        System.out.println(usage);
        System.exit(1);
    }

    // Read in the invariants
    String filename = args[fileIndex];
    PptMap ppts = FileIO.read_serialized_pptmap(new File(filename),
                                               true // use saved config
                                               );
    // Setup the list of proto invariants and initialize NIS suppressions
    Daikon.setup_proto_invs();
    Daikon.setup_NISuppression();

    // Make sure ppts' rep invariants hold
    ppts.repCheck();

    if ((Daikon.output_style == OutputFormat.ESCJAVA ||
         Daikon.output_style == OutputFormat.JML) &&
        !Daikon.dkconfig_noInvariantGuarding)
      Daikon.guardInvariants(ppts);

    // Debug print the hierarchy is a more readable manner
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("Printing PPT Hierarchy");
      for (Iterator i = ppts.pptIterator(); i.hasNext(); ) {
        PptTopLevel my_ppt = (PptTopLevel) i.next();
        if (my_ppt.parents.size() == 0)
          my_ppt.debug_print_tree (debug, 0, null);
      }
    }


    print_invariants(ppts);
  }

  // To avoid the leading "UtilMDE." on all calls.
  private static String nplural(int n, String noun) {
    return UtilMDE.nplural(n, noun);
  }

  /**
   * Prints out all the discardCodes and discardStrings of the Invariants
   * that will not be printed if the --disc_reason switch is used.
   **/
  public static void print_reasons(PptMap ppts) {
    if (!print_discarded_invariants || Daikon.no_text_output) {
      return;
    }

    System.out.println();
    System.out.println("DISCARDED INVARIANTS:");
    // DiscReasonMap.debug(discPpt);

    // Makes things faster if a ppt is specified
    if (discPpt != null) {
      PptTopLevel ppt = ppts.get(discPpt);
      if (ppt==null) {
        System.out.println("No such ppt found: "+discPpt);
      }
      else {
        String toPrint = "";
        toPrint += print_reasons_from_ppt(ppt,ppts);

        StringTokenizer st = new StringTokenizer(toPrint, lineSep);
        if (st.countTokens() > 2)
          System.out.print(toPrint);
        else {
          String matching = "";
          if (discVars!=null || discClass!=null)
            matching = " matching ";
          System.out.println("No" + matching + "discarded Invariants found in "
                             +ppt.name());
        }
      }
      return;
    }

    // Uses the custom comparator to get the Ppt objects in sorted order
    Comparator comparator = new Ppt.NameComparator();
    TreeSet ppts_sorted = new TreeSet(comparator);
    ppts_sorted.addAll(ppts.asCollection());

    // Iterate over the PptTopLevels in ppts
    for (Iterator itor = ppts_sorted.iterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      StringBuffer toPrint = new StringBuffer();
      toPrint.append(print_reasons_from_ppt(ppt,ppts));

      // A little hack so that PptTopLevels without discarded Invariants of
      // interest don't get their names printed
      StringTokenizer st = new StringTokenizer(toPrint.toString(), lineSep);
      if (st.countTokens() > 2) {
        System.out.print(toPrint.toString());
      }
    }
  }

  /**
   * Add discard reasons for invariants that are filtered out
   */
  private static void add_filter_reasons(PptTopLevel ppt, PptMap ppts) {
    Iterator fullInvItor = ppt.invariants_iterator();
    InvariantFilters fi = new InvariantFilters();
    fi.setPptMap(ppts);
    while (fullInvItor.hasNext()) {
      Invariant nextInv = (Invariant) fullInvItor.next();
      InvariantFilter varFilter = fi.shouldKeepVarFilters(nextInv);
      if (varFilter != null) {
        DiscReasonMap.put(nextInv, DiscardCode.findCode(varFilter),
                          varFilter.getDescription());
      } else {
        InvariantFilter propFilter = fi.shouldKeepPropFilters(nextInv);
        if (propFilter != null) {
          DiscardInfo di;
          if (propFilter instanceof ObviousFilter) {
            di = nextInv.isObvious();
            if (nextInv.logOn())
              nextInv.log ("DiscardInfo's stuff: " + di.className() + lineSep
                           + di.format());
          } else if (propFilter instanceof UnjustifiedFilter) {
            di = new DiscardInfo(nextInv, DiscardCode.bad_confidence,
                                 "Had confidence: " + nextInv.getConfidence());
          } else {
            di = new DiscardInfo(nextInv, DiscardCode.findCode(propFilter),
                                 propFilter.getDescription());
          }
          DiscReasonMap.put(nextInv, di);
        }
      }
    }
  }


  private static String print_reasons_from_ppt(PptTopLevel ppt, PptMap ppts) {
    // Add all the reasons that would come from filtering to the DiscReasonMap
    add_filter_reasons(ppt, ppts);

    String toPrint = "";
    Iterator fullInvItor = ppt.invariants_iterator();
    String dashes = "--------------------------------------------"
                  + "-------------------------------" + lineSep;

    if (!(ppt instanceof PptConditional)) {
      toPrint += "==============================================="
              + "============================" + lineSep;
      toPrint += (ppt.name() + lineSep);
    }

    Iterator matchesIter = DiscReasonMap.returnMatches_from_ppt
                (new InvariantInfo(ppt.name(), discVars, discClass)).iterator();

    StringBuffer sb = new StringBuffer();
    while (matchesIter.hasNext()) {
      DiscardInfo nextInfo = (DiscardInfo) matchesIter.next();
      sb.append(dashes + nextInfo.format() + lineSep);
    }

    // In case the user is interested in conditional ppt's
    if (Daikon.dkconfig_output_conditionals
          && Daikon.output_style == OutputFormat.DAIKON) {
      for (Iterator i = ppt.cond_iterator(); i.hasNext() ; ) {
        PptConditional pcond = (PptConditional) i.next();
        sb.append(print_reasons_from_ppt(pcond,ppts));
      }
    }
    return (toPrint + sb.toString());
  }

  /**
   * Method used to setup fields if the --disc_reason switch is used
   * if (arg==null) then show all discarded Invariants, otherwise just
   * show the ones specified in arg, where arg =
   * <class-name><<var1>,<var2>,...>@<ppt.name> e.g.:
   * OneOf<x>@foo():::ENTER would only show OneOf Invariants that
   * involve x at the program point foo:::ENTER (any of the 3 params
   * can be ommitted, e.g. OneOf@foo:::ENTER)
   * @throws IllegalArgumentException if arg is not of the proper syntax
   */
  public static void discReasonSetup(String arg) {
    print_discarded_invariants = true;
    usage = "Usage: <class-name><<var1>,<var2>,,,,>@<ppt.name()>" + lineSep +
            "or use --disc_reason \"all\" to show all discarded Invariants" + lineSep +
            "e.g.: OneOf<x>@foo():::ENTER" + lineSep;

    // Will print all discarded Invariants in this case
    if (arg==null || arg.length()==0 || arg.equals("all"))
      return;

    // User wishes to specify a classname for the discarded Invariants of
    // interest
    char firstChar = arg.charAt(0);
    // This temp is used later as a way of "falling through" the cases
    String temp = arg;
    if (firstChar!='@' && firstChar!='<') {
      StringTokenizer splitArg = new StringTokenizer(arg,"@<");
      discClass = splitArg.nextToken();
      if ((arg.indexOf('<') != -1) && (arg.indexOf('@') != -1) && (arg.indexOf('@') < (arg.indexOf('<'))))
        temp = arg.substring(arg.indexOf('@')); // in case the pptname has a < in it
      else if (arg.indexOf('<') != -1)
        temp = arg.substring(arg.indexOf('<'));
      else if (arg.indexOf('@') != -1)
        temp = arg.substring(arg.indexOf('@'));
      else
        return;
    }
    firstChar = temp.charAt(0);

    // User wants to specify the variable names of interest
    if (firstChar=='<') {
      if (temp.length() < 2)
        throw new IllegalArgumentException("Missing '>'" + lineSep +usage);
      if (temp.indexOf('>',1) == -1)
        throw new IllegalArgumentException("Missing '>'" + lineSep +usage);
      StringTokenizer parenTokens = new StringTokenizer(temp,"<>");
      if ((temp.indexOf('@')==-1 && parenTokens.countTokens() > 0)
          || (temp.indexOf('@')>-1 && parenTokens.countTokens() > 2))
        throw new IllegalArgumentException("Too many brackets" + lineSep +usage);
      StringTokenizer vars = new StringTokenizer(parenTokens.nextToken(),",");
      if (vars.hasMoreTokens()) {
        discVars = vars.nextToken();
        while (vars.hasMoreTokens())
          discVars += "," + vars.nextToken();
        // Get rid of *all* spaces since we know varnames can't have them
        discVars = discVars.replaceAll(" ", "");
      }
      if (temp.endsWith(">"))
        return;
      else {
        if (temp.charAt(temp.indexOf('>')+1) != '@')
          throw new IllegalArgumentException("Must have '@' after '>'" + lineSep +usage);
        else
          temp = temp.substring(temp.indexOf('>')+1);
      }
    }

    // If it made it this far, the first char of temp has to be '@'
    Assert.assertTrue(temp.charAt(0) == '@');
    if (temp.length()==1)
      throw new IllegalArgumentException("Must provide ppt name after '@'" + lineSep +usage);
    discPpt = temp.substring(1);
  }

  // The following code is a little odd because it is trying to match the
  // output format of V2.  In V2, combined exit points are printed after
  // the original exit points (rather than before as they are following
  // the PptMap sort order).
  //
  // Also, V2 only prints out a single ppt when there is only one
  // exit point.  This seems correct.  Probably a better solution to
  // this would be to not create the combined exit point at all when there
  // is only a single exit.  Its done here instead so as not to futz with
  // the partial order stuff.
  //
  // All of this can (and should be) improved when V2 is dropped.

  public static void print_invariants(PptMap all_ppts) {

    PrintWriter pw = new PrintWriter(System.out, true);
    PptTopLevel combined_exit = null;
    boolean enable_exit_swap = true; // !Daikon.dkconfig_df_bottom_up;

    if (Daikon.no_text_output)
      return;

    // Retrieve Ppt objects in sorted order.  Put them in an array list
    // so that it is easier to look behind and ahead.
    PptTopLevel[] ppts = new PptTopLevel [all_ppts.size()];
    int ii = 0;
    for (Iterator itor = all_ppts.pptIterator() ; itor.hasNext() ; )
      ppts[ii++] = (PptTopLevel) itor.next();

    for (int i = 0 ; i < ppts.length; i++) {
      PptTopLevel ppt = ppts[i];

      if (debug.isLoggable(Level.FINE))
        debug.fine ("Looking at point " + ppt.name());

      // If this point is not an exit point, print out any retained combined
      // exit point
      if (enable_exit_swap && !ppt.ppt_name.isExitPoint()) {
        if (combined_exit != null)
          print_invariants_maybe(combined_exit, pw, all_ppts);
        combined_exit = null;
      }

      // Just cache the combined exit point for now, print it after the
      // EXITnn points.
      if (enable_exit_swap && ppt.ppt_name.isCombinedExitPoint()) {
        combined_exit = ppt;
        continue;
      }

      // If there is only one exit point, just show the combined one (since
      // the EXITnn point will be empty)  This is accomplished by skipping this
      // point if it is an EXITnn point and the previous point was a combined
      // exit point and the next one is not an EXITnn point.  But don't skip
      // any conditional ppts attached to the skipped ppt.
      if (enable_exit_swap && (i > 0) && ppt.ppt_name.isExitPoint()) {
        if (ppts[i-1].ppt_name.isCombinedExitPoint()) {
          if (((i + 1) >= ppts.length) || !ppts[i+1].ppt_name.isExitPoint()) {
//             if (Daikon.dkconfig_output_conditionals
//                 && Daikon.output_style == OutputFormat.DAIKON) {
//               for (Iterator j = ppt.cond_iterator(); j.hasNext() ; ) {
//                 PptConditional pcond = (PptConditional) j.next();
//                 print_invariants_maybe(pcond, pw, all_ppts);
//               }
//             }
            continue;
          }
        }
      }

      print_invariants_maybe(ppt, pw, all_ppts);
    }

    // print a last remaining combined exit point (if any)
    if (enable_exit_swap && combined_exit != null)
      print_invariants_maybe(combined_exit, pw, all_ppts);

    pw.flush();
  }

  /**
   * Print invariants for a single program point and its conditionals.
   * Does no output if no samples or no views.
   **/
  public static void print_invariants_maybe(PptTopLevel ppt,
                                            PrintWriter out,
                                            PptMap all_ppts) {
    debugPrint.fine  ("Considering printing ppt " + ppt.name());

    // Be silent if we never saw any samples.
    // (Maybe this test isn't even necessary, but will be subsumed by others,
    // as all the invariants will be unjustified.)
    if (ppt.num_samples() == 0) {
      if (debugPrint.isLoggable(Level.FINE)) {
        debugPrint.fine ("[No samples for " + ppt.name() + "]");
      }
      if (Daikon.output_num_samples) {
        out.println("[No samples for " + ppt.name() + "]");
      }
      return;
    }
    if ((ppt.numViews() == 0) && (ppt.joiner_view.invs.size() == 0)) {
      if (debugPrint.isLoggable(Level.FINE)) {
        debugPrint.fine ("[No views for " + ppt.name() + "]");
      }
      if (! (ppt instanceof PptConditional)) {
        // Presumably all the views that were originally there were deleted
        // because no invariants remained in any of them.
        if (Daikon.output_num_samples) {
          out.println("[No views for " + ppt.name() + "]");
        }
        return;
      }
    }

    // out.println("This = " + this + ", Name = " + name + " = " + ppt_name);

    if (Daikon.output_style != OutputFormat.IOA) {
      out.println("==========================================="
                  + "================================");
    } else {
      out.println();
      out.println("% Invariants generated by Daikon for");
    }
    print_invariants(ppt, out, all_ppts);

    if (Daikon.dkconfig_output_conditionals
        && Daikon.output_style == OutputFormat.DAIKON) {
      for (Iterator j = ppt.cond_iterator(); j.hasNext() ; ) {
        PptConditional pcond = (PptConditional) j.next();
        print_invariants_maybe(pcond, out, all_ppts);
      }
    }
  }


  /**
   * If Daikon.output_num_samples is enabled, prints the number of samples
   * for the specified ppt.  Also prints all of the variables for the ppt
   * if Daikon.output_num_samples is enabled or the format is ESCIJAVA,
   * JML, or DBCJAVA
   */
  public static void print_sample_data(PptTopLevel ppt, PrintWriter out) {

    if (Daikon.output_num_samples) {
      out.println(ppt.name() + "  " + nplural(ppt.num_samples(), "sample"));
    } else {
      if (Daikon.output_style == OutputFormat.IOA) {
        out.print("% ");  // IOA comment style
      }
      out.println(ppt.name());
    }
    if (Daikon.output_num_samples
        || (Daikon.output_style == OutputFormat.ESCJAVA)
        || (Daikon.output_style == OutputFormat.JML)
        || (Daikon.output_style == OutputFormat.DBCJAVA )) {
      out.print("    Variables:");
      for (int i=0; i<ppt.var_infos.length; i++) {
        out.print(" " + ppt.var_infos[i].name.name());
      }
      out.println();
    }
  }

  /**
   * prints all variables that were modified if the format is ESCJAVA or
   * DBCJAVA
   */
  public static void print_modified_vars(PptTopLevel ppt, PrintWriter out) {
    if (debugPrintModified.isLoggable(Level.FINE)) {
      debugPrintModified.fine ("Doing print_modified_vars for: " + ppt.name());
    }

    Vector modified_vars = new Vector();
    Vector modified_primitive_args = new Vector();
    Vector unmodified_vars = new Vector();
    Vector unmodified_orig_vars = new Vector();

    for (int i=0; i<ppt.var_infos.length; i++) {
      VarInfo vi = ppt.var_infos[i];
      if (debugPrintModified.isLoggable(Level.FINE)) {
        debugPrintModified.fine ("  Testing var: " + vi.name.name());
      }
      // This test is purely an optimization.
      if (! vi.isPrestate()) {
        debugPrintModified.fine ("  not prestate");
        VarInfo vi_orig = ppt.findVar(vi.name.applyPrestate());
        if (vi_orig != null) {
          debugPrintModified.fine ("  has orig var");
          // Assert.assertTrue(vi_orig.postState.name == vi.name, "vi_orig="+vi_orig.name+", vi_orig.postState="+vi_orig.postState+((vi_orig.postState!=null)?"="+vi_orig.postState.name:"")+", vi="+vi+"="+vi.name);
          // Assert.assertTrue(vi_orig.postState == vi, "vi_orig="+vi_orig.name+", vi_orig.postState="+vi_orig.postState+((vi_orig.postState!=null)?"="+vi_orig.postState.name:"")+", vi="+vi+"="+vi.name);
          boolean is_unmodified = false;
          if (! is_unmodified) {
            java.lang.reflect.Field f = vi.name.resolveField(ppt);
            // System.out.println("Field for " + vi.name.name() + ": " + f);
            if ((f != null)
                && java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
              // System.out.println("Final: " + vi.name.name());
              is_unmodified = true;
              debugPrintModified.fine ("  modified from reflection");
            }
          }
          // System.out.println(vi.name.name() + (is_unmodified ? " unmodified" : " modified"));
          if (is_unmodified) {
            debugPrintModified.fine ("  concluded unmodified");
            unmodified_vars.add(vi);
            unmodified_orig_vars.add(vi_orig);
          } else {
            // out.println("Modified: " + vi.name + " (=" + vi.equal_to.name + "), " + vi_orig.name + " (=" + vi_orig.equal_to.name + ")");
            PptSlice1 view = ppt.findSlice(vi);
            // out.println("View " + view + " num_values=" + ((view!=null)?view.num_values():0));
            // The test "((view != null) && (view.num_values() > 0))" is
            // fallacious becuase the view might have been removed (is now
            // null) because all invariants at it were false.
            if ((view == null) ||
                view.containsOnlyGuardingPredicates()) {
              // Further modified because a view might have otherwise been
              // destroyed if it were not for the guarding invariants put
              // into the PptSlice. This if view != null, and it only contains
              // guarding predicates, it would have been null had invariant
              // guarding been off, thus the variable belongs in modified_vars.

              // Using only the isPrimitive test is wrong.  We should suppress
              // for only parameters, not all primitive values.  That's why we
              // look for the period in the name.
              // (Shouldn't this use the VarInfoName, and/or the isParam
              // information in VarInfoAux, rather than examining the text
              // of the name?  -MDE 12/2/2002)
              is_unmodified = false;
            } else {
              // If a slice is present, though, we can try to make some judgements
              // is_unmodified = (view.num_mod_samples() == 0);
            }
            if (!is_unmodified) {
              if (vi.type.isPrimitive() && (vi.name.name().indexOf(".") == -1)) {
                modified_primitive_args.add(vi);
                debugPrintModified.fine ("  concluded modified prim");
              } else {
                modified_vars.add(vi);
                debugPrintModified.fine ("  concluded modified ");
              }
            }
          }
        }
      }
    }
    if (Daikon.output_num_samples
        || (Daikon.output_style == OutputFormat.ESCJAVA)
        || (Daikon.output_style == OutputFormat.DBCJAVA)) {
      if (modified_vars.size() > 0) {
        out.print("      Modified variables:");
        for (int i=0; i<modified_vars.size(); i++) {
          VarInfo vi = (VarInfo)modified_vars.elementAt(i);
          out.print(" " + vi.name.name());
        }
        out.println();
      }
      if (modified_primitive_args.size() > 0) {
        out.print("      Modified primitive arguments:");
        for (int i=0; i<modified_primitive_args.size(); i++) {
          VarInfo vi = (VarInfo)modified_primitive_args.elementAt(i);
          out.print(" " + vi.name.name());
        }
        out.println();
      }
      if (unmodified_vars.size() > 0) {
        out.print("      Unmodified variables:");
        for (int i=0; i<unmodified_vars.size(); i++)
          out.print(" " + ((VarInfo)unmodified_vars.elementAt(i)).name.name());
        out.println();
      }
    }
    // It would be nice to collect the list of indices that are modified,
    // and create a \forall to specify that the rest aren't.
    if (Daikon.output_style == OutputFormat.ESCJAVA
        || Daikon.output_style == OutputFormat.JML
        ) {
      Vector mods = new Vector();
      for (int i=0; i<modified_vars.size(); i++) {
        VarInfo vi = (VarInfo)modified_vars.elementAt(i);
        // System.out.println("modified var: " + vi.name.name());
        while (vi != null) {
          Derivation derived = vi.derived;
          VarInfoName vin = vi.name;
          if (vin instanceof VarInfoName.TypeOf) {
            // "VAR.class"
            vi = null;
          } else if (vin instanceof VarInfoName.SizeOf) {
            // "size(VAR)"
            vi = null;
          } else if ((vin instanceof VarInfoName.Field)
                     && ((VarInfoName.Field)vin).term.name().endsWith("]")) {
            // "VAR[..].field" => VAR[..];
            // vi = ppt.findVar(((VarInfoName.Field)vin).term.name());
            vi = ppt.findVar(((VarInfoName.Field)vin).term);
            if (vi == null) {
              System.out.println("Failed findVar(" + ((VarInfoName.Field)vin).term.name() + ") from " + vin.name() + " at " + ppt.name());
            }
            Assert.assertTrue(vi != null);
          } else if (derived instanceof SequenceScalarSubscript) {
            vi = ((SequenceScalarSubscript)vi.derived).seqvar();
          } else if (derived instanceof SequenceFloatSubscript) {
            vi = ((SequenceFloatSubscript)vi.derived).seqvar();
          } else if (derived instanceof SequenceStringSubscript) {
            vi = ((SequenceStringSubscript)vi.derived).seqvar();
          } else if (derived instanceof SequenceScalarSubsequence) {
            vi = ((SequenceScalarSubsequence)vi.derived).seqvar();
          } else if (derived instanceof SequenceFloatSubsequence) {
            vi = ((SequenceFloatSubsequence)vi.derived).seqvar();
          } else if (derived instanceof SequenceStringSubsequence) {
            vi = ((SequenceStringSubsequence)vi.derived).seqvar();
            Assert.assertTrue(vi != null);
          } else {
            break;
          }
        }
        // Change this.myvector[*] to this.myvector (or would it be
        // best to just remove it?)
        if ((vi != null) && (vi.name instanceof VarInfoName.Elements)) {
          VarInfoName.Elements elems = (VarInfoName.Elements) vi.name;
          VarInfo base = ppt.findVar(elems.term);
          // Assert.assertTrue(base != null);
          if (base != null) {
            if (! base.type.isArray()) {
              vi = base;
            }
          }
        }
        // System.out.println("really modified var: " + ((vi == null) ? "null" : vi.name.name()));
        if ((vi != null) && (! mods.contains(vi))) {
          mods.add(vi);
        }
      }
      if (mods.size() > 0) {
        if (Daikon.output_style == OutputFormat.ESCJAVA)
          out.print("modifies ");
        else
          out.print("assignable ");
        int inserted = 0;
        for (int i=0; i<mods.size(); i++) {
          VarInfo vi = (VarInfo)mods.elementAt(i);
          String name = vi.name.name();
          if (!name.equals("this")) {
            if (inserted>0) {
              out.print(", ");
            }
            if (name.endsWith("[]")) {
              name = name.substring(0, name.length()-1) + "*]";
            }
            out.print(name);
          inserted++;
          }
        }
        out.println();
      }
    }

  }

  /** Count statistics (via Global) on variables (canonical, missing, etc.) **/
  public static void count_global_stats(PptTopLevel ppt) {
    for (int i=0; i<ppt.var_infos.length; i++) {
      if (ppt.var_infos[i].isDerived()) {
        Global.derived_variables++;
      }
    }
  }

  // This is just a temporary thing to provide more info about the
  // reason invariants are rejected.
  private static String reason = "";

  /** Prints the specified invariant to out **/
  public static void print_invariant(Invariant inv, PrintWriter out,
                                     int invCounter, PptTopLevel ppt) {
    int inv_num_samps = inv.ppt.num_samples();
    String num_values_samples = "\t\t(" +
      nplural(inv_num_samps, "sample") + ")";

    String inv_rep;
    // All this should turn into simply a call to format_using.
    if (Daikon.output_style == OutputFormat.DAIKON) {
      inv_rep = inv.format_using(Daikon.output_style);
    } else if (Daikon.output_style == OutputFormat.ESCJAVA) {
      if (inv.isValidEscExpression()) {
        inv_rep = inv.format_using(Daikon.output_style);
      } else {
        if (inv instanceof Equality) {
          inv_rep = "warning: method 'equality'.format(OutputFormat:ESC/Java) needs to be implemented: " + inv.format();
        } else {
          inv_rep = "warning: method " + inv.getClass().getName() + ".format(OutputFormat:ESC/Java) needs to be implemented: " + inv.format();
        }
      }
    } else if (Daikon.output_style == OutputFormat.SIMPLIFY) {
      inv_rep = inv.format_using(Daikon.output_style);
    } else if (Daikon.output_style == OutputFormat.IOA) {

      String invName = get_ioa_invname (invCounter, ppt);
      if (debugPrint.isLoggable(Level.FINE)) {
        debugPrint.fine ("Printing normal for " + invName + " with inv " +
                          inv.getClass().getName());
      }

      inv_rep = "invariant " + invName + " of " + ppt.ppt_name.getFullClassName() + ": ";

      inv_rep += get_ioa_precondition (invCounter, ppt);
      // We look for indexed variables and add fake quantifiers to
      // the left.  Should we be doing this with visitors and the
      // quantification engine?  Maybe, but then again, Daikon
      // doesn't really know what it means to sample.
      String rawOutput = inv.format_using(Daikon.output_style);
      int startPos = rawOutput.indexOf("anIndex");
      if (startPos != -1) {
        int endPos = rawOutput.indexOf ("]", startPos);
        String qvar = rawOutput.substring (startPos, endPos);
        rawOutput = "\\A " + qvar + " (" + rawOutput + ")";
      }
      inv_rep += rawOutput;
      if (PptTopLevel.debug.isLoggable(Level.FINE)) {
        PptTopLevel.debug.fine (inv.repr());
      }
    } else if (Daikon.output_style == OutputFormat.JAVA
               || Daikon.output_style == OutputFormat.JML
               || Daikon.output_style == OutputFormat.DBCJAVA) {

      inv_rep = inv.format_using(Daikon.output_style);

      // TODO: Remove once we revise OutputFormat
      if (Daikon.output_style == OutputFormat.JAVA) {
        inv_rep = inv.format_using (OutputFormat.JAVA);
        // if there is a $pre string in the format, then it contains
        // the orig variable and should not be printed.
        if (inv_rep.indexOf ("$pre") != -1) {
          return;
        }
      }

    } else if (Daikon.output_style==OutputFormat.REPAIR) {
	inv_rep = inv.format_using(Daikon.output_style);
        if (inv_rep.indexOf ("$noprinttest") != -1) {
	    return;
        }
    } else {
      throw new IllegalStateException("Unknown output mode");
    }
    if (Daikon.output_num_samples) {
      inv_rep += num_values_samples;
    }

    if (debugRepr.isLoggable(Level.FINE)) {
      debugRepr.fine ("Printing: [" + inv.repr_prob() + "]");
    } else if (debugPrint.isLoggable(Level.FINE)) {
      debugPrint.fine ("Printing: [" + inv.repr_prob() + "]");
    }

    if(wrap_xml) {
      out.print("<INVINFO>");
      out.print("<" + inv.ppt.parent.ppt_name.getPoint() + ">");
      out.print("<INV> ");
      out.print(inv_rep);
      out.print(" </INV> ");
      out.print(" <SAMPLES> " + Integer.toString(inv.ppt.num_samples()) + " </SAMPLES> ");
      out.print(" <DAIKON> " + inv.format_using(OutputFormat.DAIKON) + " </DAIKON> ");
      out.print(" <DAIKONCLASS> " + inv.getClass().toString() + " </DAIKONCLASS> ");
      out.print(" <METHOD> " + inv.ppt.parent.ppt_name.getSignature() + " </METHOD> ");
      out.println("</INVINFO>");
    } else if (Daikon.output_style == OutputFormat.REPAIR) {
	String quantifiers=Repair.getRepair().getQuantifiers();
	Repair.getRepair().reset();
	out.println("["+quantifiers+"],"+inv_rep);
    } else {
      out.println(inv_rep);
    }
    if (debug.isLoggable(Level.FINE)) {
      debug.fine (inv.repr());
    }

  }

  /**
   * Takes a list of Invariants and returns a list of Invariants that
   * is sorted according to PptTopLevel.icfp.
   */
  public static List sort_invariant_list(List invs) {
    Invariant[] invs_array = (Invariant[]) invs.toArray(new Invariant[invs.size()]);
    Arrays.sort(invs_array, PptTopLevel.icfp);

    Vector result = new Vector(invs_array.length);

    for (int i = 0; i < invs_array.length; i++) {
      result.add(invs_array[i]);
    }
    return result;
  }

  /**
   * Print invariants for a single program point, once we know that
   * this ppt is worth printing.
   **/
  public static void print_invariants(PptTopLevel ppt, PrintWriter out,
                                      PptMap ppt_map) {

    // make names easier to read before printing
    ppt.simplify_variable_names();

    print_sample_data(ppt, out);
    print_modified_vars(ppt, out);

    // Dump some debugging info, if enabled
    if (debugPrint.isLoggable(Level.FINE)) {
      debugPrint.fine ("Variables for ppt "  + ppt.name());
      for (int i=0; i<ppt.var_infos.length; i++) {
        VarInfo vi = ppt.var_infos[i];
        PptTopLevel ppt_tl = (PptTopLevel) vi.ppt;
        PptSlice slice1 = ppt_tl.findSlice(vi);
        debugPrint.fine ("      " + vi.name.name());
      }
      debugPrint.fine ("Equality set: ");
      debugPrint.fine ((ppt.equality_view == null) ? "null"
                       : ppt.equality_view.toString());
    }
    if (debugFiltering.isLoggable(Level.FINE)) {
      debugFiltering.fine ("----------------------------------------"
        + "--------------------------------------------------------" + lineSep);
      debugFiltering.fine (ppt.name() + lineSep + lineSep);
    }

    // Count statistics (via Global) on variables (canonical, missing, etc.)
    count_global_stats(ppt);

    int invCounter = 0; // Count printed invariants for this program point

    // I could instead sort the PptSlice objects, then sort the invariants
    // in each PptSlice.  That would be more efficient, but this is
    // probably not a bottleneck anyway.
    List invs_vector = new LinkedList(ppt.getInvariants());

    if (PptSplitter.debug.isLoggable (Level.FINE)) {
      PptSplitter.debug.fine ("Joiner View for ppt " + ppt.name);
      for (Iterator ii = ppt.joiner_view.invs.iterator(); ii.hasNext(); ) {
        Invariant inv = (Invariant) ii.next();
        PptSplitter.debug.fine ("-- " + inv.format());
      }
    }

    if (debugBound.isLoggable (Level.FINE))
      ppt.debug_unary_info (debugBound);

    Invariant[] invs_array = (Invariant[]) invs_vector.toArray(
      new Invariant[invs_vector.size()]);
    Arrays.sort(invs_array, PptTopLevel.icfp);

    Global.non_falsified_invariants += invs_array.length;

    List accepted_invariants = new Vector();

    for (int i = 0; i < invs_array.length; i++) {
      Invariant inv = invs_array[i];

      if (inv.logOn())
        inv.log ("Considering Printing");
      Assert.assertTrue (!(inv instanceof Equality));
      for (int j = 0; j < inv.ppt.var_infos.length; j++)
        Assert.assertTrue (!inv.ppt.var_infos[j].missingOutOfBounds(),
                           "var '" + inv.ppt.var_infos[j].name.name()
                            + "' out of bounds in " + inv.format());
      InvariantFilters fi = new InvariantFilters();
      fi.setPptMap(ppt_map);

      boolean fi_accepted = true;
      InvariantFilter filter_result = null;
      if (!dkconfig_print_all) {
        filter_result = fi.shouldKeep (inv);
        fi_accepted = (filter_result == null);
      }

      if ((inv instanceof Implication)
          && PptSplitter.debug.isLoggable(Level.FINE))
        PptSplitter.debug.fine ("filter result = " + filter_result
                                + " for inv " + inv);

      if (inv.logOn())
        inv.log ("Filtering, accepted = " + fi_accepted);

      // Never print the guarding predicates themselves, they should only
      // print as part of GuardingImplications
      if (fi_accepted && !inv.isGuardingPredicate) {
        invCounter++;
        Global.reported_invariants++;
        accepted_invariants.add(inv);
      } else {
        if (inv.logOn() || debugPrint.isLoggable(Level.FINE)) {
          inv.log (debugPrint, "fi_accepted = " + fi_accepted +
                    " inv.isGuardingPredicate = " + inv.isGuardingPredicate
                    + " not printing " + inv.repr());
        }
      }
    }

    accepted_invariants
      = InvariantFilters.addEqualityInvariants(accepted_invariants);

    if (debugFiltering.isLoggable(Level.FINE)) {
      Iterator inv_iter = accepted_invariants.iterator();
      while (inv_iter.hasNext()) {
        Invariant current_inv = (Invariant)inv_iter.next();
        if (current_inv instanceof Equality) {
          debugFiltering.fine ("Found Equality that says "
                                + current_inv.format() + lineSep);
        }
      }
    }

    if (debugFiltering.isLoggable(Level.FINE)) {
      for (int i=0; i<ppt.var_infos.length; i++) {
        VarInfo vi = ppt.var_infos[i];
      }
    }
    finally_print_the_invariants(accepted_invariants, out, ppt);
    if (false && ppt.constants != null)
      ppt.constants.print_missing (out);
  }

  /**
   * Does the actual printing of the invariants.
   **/
  private static void finally_print_the_invariants(List invariants,
                                                   PrintWriter out,
                                                   PptTopLevel ppt) {
    int index = 0;
    Iterator inv_iter = invariants.iterator();
    while (inv_iter.hasNext()) {
      index++;
      Invariant inv = (Invariant)inv_iter.next();

      print_invariant(inv, out, index, ppt);

    }
    if (Daikon.output_style == OutputFormat.REPAIR) {
	if (Repair.getRepair().getRules(ppt)!=null) {
	    out.println("-----------------------------------------------------------------------------");
	    out.print(Repair.getRepair().getRules(ppt));
	}
	if (Repair.getRepair().getSetRelation(ppt)!=null) {
	    out.println("-----------------------------------------------------------------------------");
	    out.print(Repair.getRepair().getSetRelation(ppt));
	}
	if (Repair.getRepair().getGlobals(ppt)!=null) {
	    out.println("-----------------------------------------------------------------------------");
	    out.print(Repair.getRepair().getGlobals(ppt));
	}
    }
  }

  /**
   * Get name of invariant for IOA output, since IOA invariants have
   * to be given unique names.  The name can be derived from a count
   * of the invariants and the program point name.  We simply change
   * the ppt name's characters to be valid IOA syntax.
   **/
  public static String get_ioa_invname (int numbering, PptTopLevel ppt) {
    String replaced = "";
    if (PrintInvariants.test_output) {
      if (ppt.ppt_name.getSignature() != null) {
        replaced = ppt.ppt_name.getSignature().replace
                                                ('(', '_').replace(')', '_');
      }
      return "Inv" + replaced;
    } else {
      if (ppt.ppt_name.getSignature() != null) {
        replaced = ppt.ppt_name.getSignature().replace('(', '_').replace(')', '_');
      }
      return "Inv" + replaced + numbering;
    }
  }

  public static String get_ioa_precondition (int numbering, PptTopLevel ppt) {
    if (ppt.ppt_name.isClassStaticSynthetic()) return "";
    if (ppt.ppt_name.isObjectInstanceSynthetic()) return "";
    return "enabled(" + ppt.ppt_name.getSignature() + ") => ";
  }

  /**
   * Prints all invariants for ternary slices (organized by slice) and
   * all of the unary and binary invariants over the same variables.
   * The purpose of this is to look for possible ni-suppressions.  Its
   * not intended as a normal output mechanism
   */
  public static void print_all_ternary_invs (PptMap all_ppts) {

    // loop through each ppt
    for (Iterator itor = all_ppts.pptIterator(); itor.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();

      // if (ppt.num_samples() == 0)
      //  continue;

      // First figure out how many ternary invariants/slices there are
      int lt_cnt = 0;
      int slice_cnt = 0;
      int inv_cnt = 0;
      int total_slice_cnt = 0;
      int total_inv_cnt = 0;
      for (Iterator si = ppt.views_iterator(); si.hasNext(); ) {
        PptSlice slice = (PptSlice) si.next();
        total_slice_cnt++;
        total_inv_cnt += slice.invs.size();
        if (slice.arity() != 3)
          continue;
        slice_cnt++;
        inv_cnt += slice.invs.size();
        for (Iterator ii = slice.invs.iterator(); ii.hasNext(); ) {
          Invariant inv = (Invariant) ii.next();
          if (inv.getClass().getName().indexOf ("Ternary") > 0) {
            lt_cnt++;
          }
        }
      }

      Fmt.pf ("");
      Fmt.pf ("%s - %s samples, %s slices, %s invariants (%s linearternary)",
              ppt.name(),"" + ppt.num_samples(), "" + slice_cnt, "" + inv_cnt,
              "" + lt_cnt);
      Fmt.pf ("    total slice count = " + total_slice_cnt +
              ", total_inv_cnt = " + total_inv_cnt);

      // Loop through each ternary slice
      for (Iterator si = ppt.views_iterator(); si.hasNext(); ) {
        PptSlice slice = (PptSlice) si.next();
        if (slice.arity() != 3)
          continue;
        VarInfo[] vis = slice.var_infos;

        String var_str = "";
        for (int i = 0; i < vis.length; i++) {
          var_str += vis[i].name.name() + " ";
          if (ppt.is_constant (vis[i]))
            var_str += "["
                 + Debug.toString(ppt.constants.constant_value(vis[i]))+ "] ";
        }
        Fmt.pf ("  Slice %s - %s invariants", var_str, "" + slice.invs.size());

        // Loop through each invariant (skipping ternary ones)
        for (Iterator ii = slice.invs.iterator(); ii.hasNext(); ) {
          Invariant inv = (Invariant) ii.next();
          if (inv.getClass().getName().indexOf ("Ternary") > 0) {
            continue;
          }

          // Check to see if the invariant should be suppressed
          String suppress = "";
          NISuppressionSet ss = inv.get_ni_suppressions();
          if ((ss != null) && ss.suppressed (slice))
            suppress = "ERROR: Should be suppressed by " + ss;

          // Print the invariant
          Fmt.pf ("    %s [%s] %s", inv.format(),
                  UtilMDE.unqualified_name(inv.getClass()), suppress);

          // Print all unary and binary invariants over the same variables
          for (int i = 0; i < vis.length; i++) {
            Fmt.pf ("      %s is %s", vis[i].name.name(),vis[i].file_rep_type);
            print_all_invs (ppt, vis[i], "      ");
          }
          print_all_invs (ppt, vis[0], vis[1], "      ");
          print_all_invs (ppt, vis[1], vis[2], "      ");
          print_all_invs (ppt, vis[0], vis[2], "      ");
        }
      }
    }
  }

  /**
   * Prints all of the unary invariants over the specified variable
   */
  public static void print_all_invs (PptTopLevel ppt, VarInfo vi,
                                     String indent) {
    String name = Fmt.spf ("%s [%s]", vi.name.name(), vi.file_rep_type);
    if (ppt.is_missing (vi))
      Fmt.pf ("%s%s missing", indent, name);
    else if (ppt.is_constant (vi))
      Fmt.pf ("%s%s = %s", indent, name,
              Debug.toString(ppt.constants.constant_value(vi)));
    else {
      PptSlice slice = ppt.findSlice (vi);
      if (slice != null)
        print_all_invs (slice, indent);

      if (slice == null)
        Fmt.pf ("%s%s has %s values", indent, name, "" + ppt.num_values (vi));
    }
  }

  /** Prints all of the binary invariants over the specified variables **/
  public static void print_all_invs (PptTopLevel ppt, VarInfo v1, VarInfo v2,
                                     String indent) {
    // Get any invariants in the local slice
    PptSlice slice = ppt.findSlice (v1, v2);
    print_all_invs (slice, indent);

  }

  /** Prints all of the invariants in the specified slice **/
  public static void print_all_invs (PptSlice slice, String indent) {

    if (slice == null)
      return;

    for (Iterator ii = slice.invs.iterator(); ii.hasNext(); ) {
      Invariant inv = (Invariant) ii.next();
      Fmt.pf ("%s%s [%s]", indent, inv.format(),
              UtilMDE.unqualified_name(inv.getClass()));
    }

  }

  /**
   * Prints how many invariants are filtered by each filter
   */
  public static void print_filter_stats (Logger log, PptTopLevel ppt,
                                         PptMap ppt_map) {

    boolean print_invs = false;

    List invs_vector = new LinkedList(ppt.getInvariants());
    Invariant[] invs_array = (Invariant[]) invs_vector.toArray(
      new Invariant[invs_vector.size()]);

    Map filter_map = new LinkedHashMap();

    if (print_invs)
      debug.fine (ppt.name());

    for (int i = 0; i < invs_array.length; i++) {
      Invariant inv = invs_array[i];

      InvariantFilters fi = new InvariantFilters();
      fi.setPptMap(ppt_map);
      InvariantFilter filter = fi.shouldKeep(inv);
      Class filter_class = null;
      if (filter != null)
        filter_class = filter.getClass();
      Map inv_map = (Map) filter_map.get (filter_class);
      if (inv_map == null) {
        inv_map = new LinkedHashMap();
        filter_map.put (filter_class, inv_map);
      }
      Integer cnt = (Integer) inv_map.get (inv.getClass());
      if (cnt == null)
        cnt = new Integer(1);
      else
        cnt = new Integer (cnt.intValue() + 1);
      inv_map.put (inv.getClass(), cnt);

      if (print_invs)
        log.fine (" : " + filter_class + " : " + inv.format());
    }

    log.fine (ppt.name() + ": " + invs_array.length);

    for (Iterator i = filter_map.keySet().iterator(); i.hasNext(); ) {
      Class filter_class = (Class) i.next();
      Map inv_map = (Map) filter_map.get (filter_class);
      int total = 0;
      for (Iterator j = inv_map.keySet().iterator(); j.hasNext(); ) {
        Integer cnt = (Integer) inv_map.get (j.next());
        total += cnt.intValue();
      }
      if (filter_class == null)
        log.fine (" : Accepted Invariants : " + total);
      else
        log.fine (" : " + filter_class.getName() + ": " + total);
      for (Iterator j = inv_map.keySet().iterator(); j.hasNext(); ) {
        Class inv_class = (Class) j.next();
        Integer cnt = (Integer) inv_map.get (inv_class);
        log.fine (" : : " + inv_class.getName() + ": " + cnt.intValue());
      }
    }
  }
}
