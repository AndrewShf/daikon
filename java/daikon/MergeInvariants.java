package daikon;

import java.util.*;
import java.io.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import utilMDE.*;

public final class MergeInvariants {
  private MergeInvariants() { throw new Error("do not instantiate"); }

  private static final String lineSep = Global.lineSep;

  public static final Logger debug = Logger.getLogger("daikon.MergeInvariants");

  public static final Logger debugProgress
                        = Logger.getLogger("daikon.MergeInvariants.progress");

  public static File output_inv_file;

  private static Stopwatch stopwatch = new Stopwatch();

  private static String usage =
    UtilMDE.join(new String[] {
      "Usage: java daikon.PrintInvariants [OPTION]... FILE",
      "  -h, --" + Daikon.help_SWITCH,
      "      Display this usage message",
      "  --" + Daikon.config_option_SWITCH,
      "      Specify a configuration option ",
      "  --" + Daikon.debug_SWITCH,
      "      Specify a logger to enable",
      "  --" + Daikon.track_SWITCH,
      "      Specify a class, varinfos, and ppt to debug track.  Format"
             + "is class<var1,var2,var3>@ppt",
      "   -o ",
      "      Specify an output inv file.  If not specified, the results "
             + "are printed"},
      lineSep);

  public static void main(String[] args)
    throws FileNotFoundException, StreamCorruptedException,
           OptionalDataException, IOException, ClassNotFoundException {
    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT,
                  null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.track_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };

    Getopt g = new Getopt("daikon.MergeInvariants", args, "ho:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {

      // long option
      case 0:
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          System.exit(1);

        } else if (Daikon.config_option_SWITCH.equals(option_name)) {
          String item = g.getOptarg();
          daikon.config.Configuration.getInstance().apply(item);
          break;

        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;

        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
        } else if (Daikon.track_SWITCH.equals(option_name)) {
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

      case 'o':
        if (output_inv_file != null)
          throw new Error("multiple serialization output files "
                         + "supplied on command line");

        String output_inv_filename = g.getOptarg();
        output_inv_file = new File(output_inv_filename);

        if (! UtilMDE.canCreateAndWrite(output_inv_file)) {
          throw new Error("Cannot write to file " + output_inv_file);
        }
        break;

      case '?':
        break; // getopt() already printed an error

      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    daikon.LogHelper.setupLogs(Global.debugAll ? LogHelper.FINE
                               : LogHelper.INFO);

    List inv_files = new ArrayList();
    File decl_file = null;
    Set splitter_files = new TreeSet();

    // Get each file specified
    for (int i = g.getOptind(); i < args.length; i++) {
      File file = new File (args[i]);
      if (! file.exists()) {
        throw new Error("File " + file + " not found.");
      }
      if (file.toString().indexOf (".inv") != -1)
        inv_files.add (file);
      else if (file.toString().indexOf (".decls") != -1) {
        if (decl_file != null)
          throw new Error ("Only one decl file may be specified");
        decl_file = file;
      } else if (file.toString().indexOf(".spinfo") != -1) {
        splitter_files.add(file);
      } else {
        throw new Error ("unexpected file: " + file);
      }
    }

    // Make sure at least two files were specified
    if (inv_files.size() < 2)
      throw new Error ("Must specify at least two inv files");

    // Read in each of the specified maps
    List pptmaps = new ArrayList();
    for (int i = 0; i < inv_files.size(); i++) {
      File file = (File) inv_files.get(i);
      debugProgress.fine ("Processing " + file);
      PptMap ppts = FileIO.read_serialized_pptmap (file, false);
      ppts.repCheck();
      pptmaps.add (ppts);
    }

    // Merged ppt map (result of merging each specified inv file)
    PptMap merge_ppts = null;

    // if no decls file was specified
    if (decl_file == null) {
      if (splitter_files.size() > 0)
        throw new Error(".spinfo files may only be specified along "
                        + "with a .decls file");

      // Read in the first map again to serve as a template
      File file = (File) inv_files.get(0);
      debugProgress.fine ("Reading " + file + " as merge template");
      merge_ppts = FileIO.read_serialized_pptmap (file, true);

      // Remove all of the slices, equality sets, to start
      debugProgress.fine ("Cleaning ppt map in preparation for merge");
      for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
        PptTopLevel ppt = (PptTopLevel) i.next();
        ppt.clean_for_merge();
      }

    } else {

      // Build the result ppmap from the specific decls file
      debugProgress.fine ("Building result ppt map from decls file");
      List  decl_files = new ArrayList();
      decl_files.add (decl_file);
      merge_ppts = FileIO.read_declaration_files(decl_files);
      Daikon.create_splitters(merge_ppts, splitter_files);
      Daikon.init_ppts (merge_ppts);
      merge_ppts.trimToSize();
      PptRelation.init_hierarchy (merge_ppts);
    }

    // Create a hierarchy between the merge exitNN points and the
    // corresponding points in each of the specified maps.  This
    // should only be created at the exitNN points (ie, the leaves)
    // so that the normal processing will create the invariants at
    // upper points.
    debugProgress.fine ("Building hierarchy between leaves of the maps");
    for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();
      if (!ppt.ppt_name.isExitPoint())
        continue;
      if (ppt.ppt_name.isCombinedExitPoint())
        continue;
      // Remove any relations down to conditionals, since we want to
      // build the ppt from the matching points in the specified maps
      for (Iterator j = ppt.children.iterator(); j.hasNext(); ) {
        PptRelation rel = (PptRelation) j.next();
        if (rel.getRelationType() == PptRelation.PPT_PPTCOND)
          j.remove();
      }
      for (int j = 0; j < pptmaps.size(); j++ ) {
        PptMap pmap = (PptMap) pptmaps.get (j);
        PptTopLevel child = pmap.get (ppt.ppt_name);
        if ((decl_file == null) && (child == null))
          throw new Error ("Can't find " + ppt.ppt_name + " in "
                           + inv_files.get(j));
        if (child == null)
          continue;
        if (child.num_samples() == 0)
          continue;
        if (child.equality_view == null)
          System.out.println ("equality_view == null in child ppt: "
                              + child.name() + " (" + inv_files.get(j) + ")");
        else if (child.equality_view.invs == null)
          System.out.println ("equality_view.invs == null in child ppt: "
                              + child.name() + " (" + inv_files.get(j) + ")"
                              + " samples = " + child.num_samples());
        PptRelation rel = PptRelation.newMergeChildRel (ppt, child);
        setup_conditional_merge (rel, ppt, child);
      }
    }

    // Debug print the hierarchy is a more readable manner
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("PPT Hierarchy");
      for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
        PptTopLevel ppt = (PptTopLevel) i.next();
        if (ppt.parents.size() == 0)
          ppt.debug_print_tree (debug, 0, null);
      }
    }

    // Merge the invariants
    debugProgress.fine ("Merging invariants");
    Daikon.createUpperPpts (merge_ppts);

    // Equality post processing
    debugProgress.fine ("Equality Post Processing");
    for (Iterator itor = merge_ppts.pptIterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      ppt.postProcessEquality();
    }

    // Implications
    stopwatch.reset();
    System.out.println("Creating implications ");
    debugProgress.fine ("Adding Implications ... ");
    for (Iterator itor = merge_ppts.pptIterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      if (ppt.num_samples() > 0)
        ppt.addImplications();
    }
    debugProgress.fine ("Time spent in implications: " + stopwatch.format());


    // Remove the PptRelation links so that when the file is written
    // out it only includes the new information
    for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();
      if (!ppt.ppt_name.isExitPoint())
        continue;
      if (ppt.ppt_name.isCombinedExitPoint())
        continue;
      ppt.children.clear();
      for (Iterator conds = ppt.cond_iterator(); conds.hasNext(); ) {
        PptConditional cond = (PptConditional) conds.next();
        cond.children.clear();
      }
    }

    // Write serialized output - must be done before guarding invariants
    debugProgress.fine ("Writing Output");
    if (output_inv_file != null) {
      try {
        FileIO.write_serialized_pptmap(merge_ppts, output_inv_file);
      } catch (IOException e) {
        throw new RuntimeException("Error while writing .inv file "
                                + "'" + output_inv_file + "': " + e.toString());
      }
    } else {
      // Print the invariants
      PrintInvariants.print_invariants (merge_ppts);
    }

  }

  /**
   * Ses up the specified relation beteween each of the conditionals
   * in ppt and the matching conditionals in child.  Each must have
   * the same number of splitters setup in the same order.  The splitter
   * match can't be checked because splitters can't be read back in.
   */
  private static void setup_conditional_merge (PptRelation rel,
                                        PptTopLevel ppt, PptTopLevel child) {

    if (ppt.has_splitters() != child.has_splitters()) {
      System.err.println("Merge ppt " + ppt.name +
                         (ppt.has_splitters() ? " has " : "doesn't have ") +
                         "splitters, but child ppt " + child.name +
                         (child.has_splitters() ? " does" : " doesn't"));
      Assert.assertTrue(false);
    }
    if (!ppt.has_splitters())
      return;

    if (ppt.splitters.size() != child.splitters.size()) {
      System.err.println("Merge ppt " + ppt.name + " has " +
                         ((ppt.splitters.size() > child.splitters.size()) ?
                          "more" : "fewer") + " splitters (" +
                         ppt.splitters.size() + ") than child ppt " +
                         child.name + " (" + child.splitters.size() + ")");
      Assert.assertTrue(false);
    }
    for (int ii = 0; ii < ppt.splitters.size(); ii++) {
      PptSplitter ppt_split = (PptSplitter) ppt.splitters.get(ii);
      PptSplitter child_split = (PptSplitter) child.splitters.get(ii);
      ppt_split.add_relation (rel, child_split);
    }
  }

}
