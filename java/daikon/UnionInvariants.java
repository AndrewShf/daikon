package daikon;

import java.util.*;
import java.io.*;
import gnu.getopt.*;
import utilMDE.UtilMDE;

/**
 * UnionInvariants is a command-line tool that will read in one (or
 * more) .inv files (possibly gzipped) and write their union into a
 * new .inv file (possibly gzipped).  Run with -h flag to view the
 * command line syntax.
 *
 * <p> Currently, UnionInvariants works at program point granularity,
 * so two inv files cannot have printable invariants at the same
 * program point.
 *
 * <p> You can optionally use Simplify after combination in case you
 * believe invariant context from other types will suppress some
 * invariants.  (This tool is also a nice way to run Simplify on a
 * single inv file.)
 **/
public final class UnionInvariants {
  private UnionInvariants() { throw new Error("do not instantiate"); }

  private static final String lineSep = Global.lineSep;

  // Non-empty program points in the input files must be distinct.
  private static String usage =
    UtilMDE.join(new String[] {
      "Usage: java daikon.UnionInvariants [OPTION]... FILE.inv[.gz] [FILE.inv[.gz] ...]",
      "  -h, --" + Daikon.help_SWITCH,
      "      Display this usage message",
      "  --" + Daikon.suppress_redundant_SWITCH,
      "      Suppress display of logically redundant invariants.",
    }, lineSep);

  public static void main(String[] args)
    throws Exception
  {
    File inv_file = null;

    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.suppress_redundant_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
    };
    Getopt g = new Getopt("daikon.UnionInvariants", args, "ho:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          System.exit(1);
        } else if (Daikon.suppress_redundant_SWITCH.equals(option_name)) {
          Daikon.suppress_redundant_invariants_with_simplify = true;
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
        if (inv_file != null)
          throw new Error("multiple serialization output files supplied on command line");

        String inv_filename = g.getOptarg();
        System.out.println("Inv filename = " + inv_filename);
        inv_file = new File(inv_filename);

        if (! UtilMDE.canCreateAndWrite(inv_file)) {
          throw new Error("Cannot write to file " + inv_file);
        }
        break;
        //
      case '?':
        break; // getopt() already printed an error
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    // The index of the first non-option argument
    int fileIndex = g.getOptind();
    if ((inv_file == null) || (args.length - fileIndex == 0)) {
        System.out.println(usage);
        System.exit(1);
    }

    PptMap result = new PptMap();
    for (int i = fileIndex; i < args.length; i++) {
      String filename = args[i];
      System.out.println("Reading " + filename + "...");
      PptMap ppt_map =
        FileIO.read_serialized_pptmap(new File(filename),
                                      true // use saved config
                                      );
      union(result, ppt_map);
    }

    // TODO: We should check consistency things, such as entry_ppt not
    // pointing outside of the PptMap.  (What else?)

    // Mark redundant invariants (may have more given additional
    // surrounding program points)

    if (Daikon.suppress_redundant_invariants_with_simplify) {
      System.out.print("Invoking Simplify to identify redundant invariants...");
      System.out.flush();
      long start = System.currentTimeMillis();
      for (Iterator i = result.pptIterator() ; i.hasNext() ; ) {
        PptTopLevel ppt = (PptTopLevel) i.next();
        ppt.mark_implied_via_simplify(result);
      }
      long end = System.currentTimeMillis();
      double elapsed = (end - start) / 1000.0;
      System.out.println((new java.text.DecimalFormat("#.#")).format(elapsed) + "s");
    }

    // Write serialized output
    System.out.println("Writing " + inv_file + "...");
    FileIO.write_serialized_pptmap(result, inv_file);

    System.out.println("Exiting");
  }

  /**
   * Union multiple PptMaps into one.
   **/
  public static void union(PptMap collector,  // mutated
                           PptMap source      // unmodified (but aliased into)
                           ) {
    for (Iterator i = source.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();

      if ((ppt.numViews() == 0) && (ppt.joiner_view.invs.size() == 0))
        continue;

      if (collector.get(ppt.ppt_name) != null) {
        throw new RuntimeException("Cannot merge two non-empty ppts named " + ppt.name());
      }

      System.out.println("Adding ppt " + ppt.name());
      collector.add(ppt);
    }
  }

}
