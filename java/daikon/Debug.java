package daikon;

import daikon.*;
import daikon.inv.*;
import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import utilMDE.*;

/**
 * Debug class used with the log4j logger to create standardized output.
 * It can be setup to track combinations of classes, program points,
 * and variables.  The most common class to track is an invariant, but
 * any class can be used.
 *
 * This allows detailed information about a particular class/ppt/variable
 * combination to be printed without getting lost in a mass of other
 * information (which is a particular problem in Daikon due to the volume
 * of data considered).
 *
 * Note that each of the three items (class, ppt, variable) must match
 * in order for a print to occur.
 **/

public class Debug {

  /** Debug Logger */
  public static final Logger debugTrack = Logger.getLogger ("daikon.Debug");

  /**
   * List of classes for logging. Each name listed is compared to the
   * fully qualified class name.  If it matches (shows up anywhere in
   * the class name) it will be included in debug prints.  This is
   * not a regular expression match
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[] debugTrackClass
    = {
      // "PptSliceEquality",
      // "PptTopLevel",
      // "PptSlice",
      // "DynamicConstants",
      // "Equality",
      // "LowerBound",
      // "UpperBound",
      // "LinearBinary",
      // "SeqIndexComparison",
      // "SeqIndexNonEqual",
      // "IntEqual",
      // "SeqSeqIntEqual",
      // "NonZero",
      // "FunctionBinary",
      // "OneOfSequence",
      // "IntLessEqual",
      // "IntGreaterEqual",
      // "IntLessThan",
       "IntGreaterThan",
      // "IntNonEqual",
      // "Member",
      // "FunctionBinary"
      // "EltNonZero",
      // "SubSet",
      // "SuperSet",
      // "EltOneOf",
      // "Bound",
      // "SeqSeqIntLessThan",
      // "SeqSeqIntGreaterThan",
      // "OneOf"
      // "StringComparison",
      // "StringLessThan",
      // "StringGreaterThan",
   };

  /**
   * Restrict function binary prints to the specified method.  Implementation
   * is in the FunctionBinary specific log functions.  If null, there is no
   * restriction (all function binary methods are printed).  See Functions.java
   * for a list of function names
   */
  public static String function_binary_method =
    "java.lang.Math.max(";

  /**
   * List of Ppts for logging. Each name listed is compared to
   * the full program point name. If it matches (shows up anywhere in
   * the ppt name) it will be included in the debug prints.  This is
   * not a regular expression match
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[] debugTrackPpt
    = {
      // "DataStructures.StackAr.makeEmpty()V:::ENTER",
      // "GLOBAL",
      "std.new_job(int;)int:::EXIT",
    };

  /**
   * List of variable names for logging. Each name listed is compared
   * to each variable in turn.  If each matches exactly it will be
   * included in track debug prints.  This is not a regular expression
   * match.  Note that the number of variables must match the slice
   * exactly.
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[][] debugTrackVars
    = {
      // { "this.terms.wrapped[orig(e)+1..]", "this.terms.wrapped[orig(c)..]" }
      // {"::performance_report", "ds"},
      {"::next_pid", "orig(::next_pid)"},
    };

  // cached standard parts of the debug print so that multiple calls from
  // the same context don't have to repeat these each time

  /** true if the cached variables are printable **/
  public boolean cache_match = true;

  /** cached class */
  public Class cache_class;

  /** cached ppt */
  public Ppt cache_ppt;

  /** cached variables */
  public VarInfo cache_vis[];

  /**
   * Sets the cache for class, ppt, and vis so that future calls to log
   * don't have to set them.
   **/

  public Debug (Class c, Ppt ppt, VarInfo[] vis) {
    set (c, ppt, vis);
  }

  /**
   * Returns a Debug object if the specified class, ppt, and vis match
   * what is being tracked.  Otherwise, return NULL.  Preferred over calling
   * the constructor directly, since it doesn't create the object if it
   * doesn't have to
   */
  public static Debug newDebug (Class c, Ppt ppt, VarInfo[] vis) {
    if (logOn() && class_match (c) && ppt_match (ppt) && var_match (vis))
      return new Debug (c, ppt, vis);
    else
      return null;
  }


  /**
   * Sets up the cache for c, ppt, and whatever variable (if any) from
   * vis that is on the debugTrackVar list.  Essentially this creates
   * a debug object that will print if any of the variables in vis are
   * being tracked (and c and ppt match)
   */
  public Debug (Class c, Ppt ppt, List vis) {

    VarInfo v = visTracked (vis);
    if (v != null)
      set (c, ppt, new VarInfo[] {v});
    else if (vis.size() > 0)
      set (c ,ppt, new VarInfo[] {(VarInfo) vis.get(0)});
    else
      set (c, ppt, null);
  }

  /**
   * Looks for each of the variables in vis in the DebugTrackVar list.  If
   * any match, returns that variable.  Null is returned if there are no
   * matches.
   */
  public VarInfo visTracked (List vis) {

    for (int i = 0; i < vis.size(); i++) {
      VarInfo v = (VarInfo) vis.get(i);
      Set evars = null;
      if (v.equalitySet != null)
        evars = v.equalitySet.getVars();
      if (evars != null) {
        for (Iterator iter = evars.iterator(); iter.hasNext(); ) {
          VarInfo ev = (VarInfo) iter.next();
          for (int k = 0; k < debugTrackVars.length; k++) {
            if (ev.equals (debugTrackVars[k][0]))
              return (v);
          }
        }
      }
    }

    return null;
  }

  private static String ourvars[] = new String[3];

  private static final VarInfo[] vis1 = new VarInfo[1];
  private static final VarInfo[] vis2 = new VarInfo[2];
  private static final VarInfo[] vis3 = new VarInfo[3];

  public static VarInfo[] vis (VarInfo v1) {
    vis1[0] = v1;
    return (vis1);
  }

  public static VarInfo[] vis (VarInfo v1, VarInfo v2) {
    vis2[0] = v1;
    vis2[1] = v2;
    return (vis2);
  }

  public static VarInfo[] vis (VarInfo v1, VarInfo v2, VarInfo v3) {
    vis3[0] = v1;
    vis3[1] = v2;
    vis3[2] = v3;
    return (vis3);
  }

  /**
   * Sets the cache for class, ppt, and vis so that future calls to log
   * don't have to set them.
   **/

  void set (Class c, Ppt ppt, VarInfo[] vis) {
    cache_class = c;
    cache_ppt = ppt;
    cache_vis = vis;
    if (c == null)
      System.out.println ("Class = null");
    if (ppt == null)
      System.out.println ("ppt = null");
    if (vis == null)
      System.out.println ("vis = null");
    else {
      for (int i = 0; i < vis.length; i++)
        if (vis[i] == null)
          System.out.println ("vis[" + i + "] == null");
    }
    cache_match = class_match (c) && ppt_match (ppt) && var_match (vis);
  }


  /**
   * Determines whether or not traceback information is printed for each
   * call to log.
   *
   * @see #log(Logger, Class, Ppt, String)
   */
  public static boolean dkconfig_showTraceback = false;

  /**
   * Determines whether or not detailed info (such as from add_modified)
   * is printed
   *
   * @see #log(Logger, Class, Ppt, String)
   * @see #logDetail()
   */
  public static boolean dkconfig_logDetail = false;

  /**
   * Returns whether or not detailed logging is on.  Note that this check
   * is not performed inside the logging calls themselves, it must be
   * performed by the caller.
   *
   * @see #log(Logger, Class, Ppt, String)
   * @see #logOn()
   */

  public static boolean logDetail () {
    return (dkconfig_logDetail && debugTrack.isLoggable(Level.FINE));
  }

  /**
   * Returns whether or not logging is on.
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static boolean logOn() {
    return debugTrack.isLoggable(Level.FINE);
  }

  /**
   * Logs the cached class, cached ppt, cached variables and the
   * specified msg via the log4j logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}
   */

  public void log (Logger debug, String msg) {
    if (cache_match)
      log (debug, cache_class, cache_ppt, cache_vis, msg);
  }

  /**
   * Logs a description of the class, ppt, ppt variables and the
   * specified msg via the log4j logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}
   */

  public static void log (Logger debug, Class inv_class, Ppt ppt, String msg) {
    log (debug, inv_class, ppt, ppt.var_infos, msg);
  }

  /**
   * Logs a description of the class, ppt, variables and the specified
   * msg via the log4j logger.  The class, ppt, and variables are
   * checked against those described in {@link #debugTrackClass},
   * {@link #debugTrackPpt}, and {@link #debugTrackVars}.  Only
   * those that match are printed.  Variables will match if they are
   * in the same equality set.  The information is written as: <p>
   *
   * <code> class: ppt : var1 : var2 : var3 : msg </code> <p>
   *
   * Note that if {@link #debugTrack} is not enabled then
   * nothing is printed.  It is somewhat faster to check {@link #logOn()}
   * directly rather than relying on the check here. <p>
   *
   * Other versions of this method (noted below) work without the Logger
   * parameter and take class, ppt, and vis from the cached values
   *
   * @param debug       A second Logger to query if debug tracking is turned
   *                    off or does not match.  If this logger is
   *                    enabled, the same information will be written
   *                    to it.  Note that the information is never
   *                    written to both loggers.
   * @param inv_class   The class.  Can be obtained in a static context
   *                    by ClassName.class
   * @param ppt         Program point
   * @param vis         Variables at the program point.  These are sometimes
   *                    different from the ones in the ppt itself.
   * @param msg         String message to log
   *
   * @see #logOn()
   * @see #logDetail()
   * @see #log(Class, Ppt, VarInfo[], String)
   * @see #log(Class, Ppt, String)
   * @see #log(Logger, String)
   * @see #log(String)
   */

  public static void log (Logger debug, Class inv_class, Ppt ppt,
                          VarInfo[] vis, String msg) {

    // Try to log via the logger first
    if (log (inv_class, ppt, vis, msg))
      return;

    // If debug isn't turned on, there is nothing to do
    if (!debug.isLoggable(Level.FINE))
      return;

    // Get the non-qualified class name
    String class_str = "null";
    if (inv_class != null)
      class_str = UtilMDE.replaceString (inv_class.getName(),
                                inv_class.getPackage().getName() + ".", "");

    // Get a string with all of the variable names.  Each is separated by ': '
    // 3 variable slots are always setup for consistency
    String vars = "";
    for (int i = 0; i < vis.length; i++) {
      VarInfo v = vis[i];
      vars += v.name.name() + ": ";
    }
    for (int i = vis.length; i < 3; i++)
      vars += ": ";

    // Figure out the sample count if possible
    String samp_str = "";
    if (ppt instanceof PptSlice) {
      PptSlice pslice = (PptSlice) ppt;
      samp_str = " s" + pslice.num_samples();
    }

    // Figure out the line number if possible
    LineNumberReader lnr = FileIO.data_trace_reader;
    String line = (lnr == null) ? "?" : String.valueOf(lnr.getLineNumber());
    line = " line=" + line;

    debug.fine (class_str + ": " + ppt.name()
                 + samp_str + line + ": " + vars + msg);
    if (dkconfig_showTraceback) {
      Throwable stack = new Throwable("debug traceback");
      stack.fillInStackTrace();
      stack.printStackTrace();
    }
  }

 /**
  * Logs a description of the cached class, ppt, and variables and the
  * specified msg via the log4j logger as described in {@link
  * #log(Logger, Class, Ppt, VarInfo[], String)}
  *
  * @return whether or not it logged anything
  */

  public boolean log (String msg) {
    if (!logOn())
      return (false);
    return (log (cache_class, cache_ppt, cache_vis, msg));
  }

  /**
   * Logs a description of the class, ppt, ppt variables and the
   * specified msg via the log4j logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}
   *
   * @return whether or not it logged anything
   */
  public static boolean log (Class inv_class, Ppt ppt, String msg) {

    return (log (inv_class, ppt, ppt.var_infos, msg));
  }

  /**
   * Logs a description of the class, ppt, variables and the specified
   * msg via the log4j logger as described in {@link #log(Logger,
   * Class, Ppt, String)}.  Accepts vis because sometimes the
   * variables are different from those in the ppt.
   *
   * @return whether or not it logged anything
   */
  public static boolean log (Class inv_class, Ppt ppt, VarInfo vis[],
                             String msg) {

    if (!debugTrack.isLoggable(Level.FINE))
      return (false);

    // Make sure the class matches
    if (!class_match (inv_class))
      return (false);

    // Make sure the Ppt matches
    if (!ppt_match (ppt))
      return (false);

    // Make sure the variables match
    if (!var_match (vis))
      return (false);

    // Get the non-qualified class name
    String class_str = "null";
    if (inv_class != null)
      class_str = UtilMDE.replaceString (inv_class.getName(),
                                inv_class.getPackage().getName() + ".", "");

    // Get a string with all of the variable names.  Each is separated by ': '
    // 3 variable slots are always setup for consistency
    String vars = "";
    for (int i = 0; i < vis.length; i++) {
      VarInfo v = vis[i];
      vars += v.name.name();
      if (ourvars[i] != null)
        vars += " {" + ourvars[i] + "}";
      vars += ": ";
    }
    for (int i = vis.length; i < 3; i++)
      vars += ": ";

    // Figure out the sample count if possible
    String samp_str = "";
    if (ppt instanceof PptSlice) {
      PptSlice pslice = (PptSlice) ppt;
      samp_str = " s" + pslice.num_samples();
    }

    // Figure out the line number if possible
    LineNumberReader lnr = FileIO.data_trace_reader;
    String line = (lnr == null) ? "?" : String.valueOf(lnr.getLineNumber());
    line = " line=" + line;

    debugTrack.fine (class_str + ": " + ppt.name()
                     + samp_str + line + ": " + vars + msg);
    if (dkconfig_showTraceback) {
      Throwable stack = new Throwable("debug traceback");
      stack.fillInStackTrace();
      stack.printStackTrace();
    }

    return (true);
  }

  /**
   * Returns whether or not the specified class matches the classes being
   * tracked
   */
  public static boolean class_match (Class inv_class) {

    if ((debugTrackClass.length > 0) && (inv_class != null)) {
      return (strContainsElem (inv_class.getName(), debugTrackClass));
    }
    return (true);
  }

  /**
   * Returns whether onot the specified ppt matches the ppts being tracked
   */
  public static boolean ppt_match (Ppt ppt) {

    if (debugTrackPpt.length > 0) {
      return (strContainsElem (ppt.name(), debugTrackPpt));
    }
    return (true);
  }

  /**
   * Returns whether or not the specified vars match the ones being tracked.
   * Also, sets Debug.ourvars with the names of the variables matched if they
   * are not the leader of their equality sets
   */

  public static boolean var_match (VarInfo vis[]) {

    if (debugTrackVars.length == 0)
      return (true);
    if (vis == null)
      return (false);

    boolean match = false;

    // Loop through each set of specified debug variables.
    outer: for (int i = 0; i < debugTrackVars.length; i++) {
      String[] cv = debugTrackVars[i];
      if (cv.length != vis.length)
        continue;
      for (int j = 0; j < ourvars.length; j++)
        ourvars[j] = null;

      // Flags to insure that we don't match a variable more than once
      boolean[] used = {false, false, false};

      // Loop through each variable in this set of debug variables
      for (int j = 0; j < cv.length; j++) {
        boolean this_match = false;
        Set evars = null;

        // Loop through each variable at this point
        eachvis: for (int k = 0; k < vis.length; k++) {

          // Get the matching equality set
          evars = null;
          if (vis[k].equalitySet != null)
            evars = vis[k].equalitySet.getVars();

          // If there is an equality set
          if ((evars != null) && vis[k].isCanonical()) {

            // Loop through each variable in the equality set
            for (Iterator iter = evars.iterator(); iter.hasNext(); ) {
              VarInfo v = (VarInfo) iter.next();
              if (!used[k] &&
                  (cv[j].equals ("*") || cv[j].equals (v.name.name()))) {
                used[k] = true;
                this_match = true;
                if (!cv[j].equals (vis[j].name.name())) {
                  ourvars[j] = v.name.name();
                  if (j != k)
                    ourvars[j] += " (" + j +"/" + k + ")";
                  if (v.isCanonical())
                    ourvars[j] += " (Leader)";
                }
                break eachvis;
              }
            }
          } else { // sometimes, no equality set
            if (cv[j].equals ("*") || cv[j].equals (vis[k].name.name()))
              this_match = true;
          }
        }
        if (!this_match)
          continue outer;
      }
      match = true;
      break outer;
    }

    return (match);
  }


  /**
   * Looks for an element in arr that is a substring of str.
   */
  private static boolean strContainsElem (String str, String[] arr) {

    for (int i = 0; i < arr.length; i++) {
      if (str.indexOf (arr[i]) >= 0)
        return (true);
    }
    return (false);
  }

  /**
   * Looks through entire ppt tree and checks for any items we are interested
   * in.  If found, prints them out.
   */
  public static void check (PptMap all_ppts, String msg) {

    boolean found = false;

    for (Iterator i = all_ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();
      for (Iterator j = ppt.views_iterator(); j.hasNext(); ) {
        PptSlice slice = (PptSlice) j.next();
        for (int k = 0; k < slice.invs.size(); k++ ) {
          Invariant inv = (Invariant) slice.invs.get(k);
          if (inv.log (msg + ": found (" + k + ") " + inv.format() +
                       " in slice " + slice))
            found = true;
        }
      }
    }
    if (!found)
      debugTrack.fine ("Found no points at '" + msg + "'");
  }

  /**
   * Returns a string containing the integer variables and their
   * values
   */
  public static String int_vars (PptTopLevel ppt, ValueTuple vt) {

    String out = "";

    for (int i = 0; i < ppt.var_infos.length; i++) {
      VarInfo v = ppt.var_infos[i];
      if (!v.isCanonical())
        continue;
      if (v.file_rep_type != ProglangType.INT)
        continue;
      out += v.name.name() + "=" + toString (v.getValue(vt))
        + " [" + vt.getModified(v) + "]: ";
    }
    return out;
  }

  /**
   * Returns a string containing the variable values for any variables
   * that are currently being tracked in ppt.  The string is of the
   * form 'v1 = val1: v2 = val2, etc.
   */
  public static String related_vars (PptTopLevel ppt, ValueTuple vt) {

    String out = "";

    for (int i = 0; i < ppt.var_infos.length; i++) {
      VarInfo v = ppt.var_infos[i];
      for (int j = 0; j < debugTrackVars.length; j++) {
        String[] cv = debugTrackVars[j];
        for (int k = 0; k < cv.length; k++) {
          if (cv[k].equals (v.name.name())) {
            Object val = v.getValue (vt);
            int mod = vt.getModified (v);
            out += v.name.name() + "=";
            out += toString (val);
            if ((mod == ValueTuple.MISSING_FLOW)
              || (mod == ValueTuple.MISSING_NONSENSICAL))
              out += " (missing)";
            if (v.missingOutOfBounds())
              out += " (out of bounds)";
            if (!v.isCanonical())
              out += " (leader=" + v.canonicalRep().name.name() + ")";
            // out += " mod=" + mod;
            out += ": ";
          }
        }
      }
    }

    return (out);
  }

  public static String toString (Object val) {
    if (val == null)
      return ("none");
    if (val instanceof String)
      return "\"" + val + "\"";
    if (val instanceof long[])
      return ArraysMDE.toString ((long[])val);
    else if (val instanceof String[])
      return ArraysMDE.toString ((String[])val);
    else if (val instanceof double[])
      return ArraysMDE.toString ((double[])val);
    else
      return (val.toString());
  }

  public static String toString (VarInfo[] vis) {

    String vars = "";
    for (int j = 0; j < vis.length; j++)
      vars += vis[j].name.name() + " ";
    return (vars);
  }

}
