package daikon.simplify;

import java.util.Vector;
import java.util.Stack;
import java.util.Random;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;
import daikon.simplify.SimpUtil;
import utilMDE.*;

/**
 * A stack of Lemmas that shadows the stack of assumptions that
 * Simplify keeps. Keeping this stack is necessary if we're to be able
 * to restart Simplify from where we left off after it hangs, but it's
 * also a convenient place to hang routines that any Simplify client
 * can use.
 **/

public class LemmaStack {
  /**
   * Boolean. Controls Daikon's response when inconsistent invariants
   * are discovered while running Simplify. If false, Daikon will give
   * up on using Simplify for that program point. If true, Daikon will
   * try to find a small subset of the invariants that cause the
   * contradiction and avoid them, to allow processing to
   * continue. For more information, see the section on
   * troubleshooting contradictory invariants in the Daikon manual.
   **/
  public static boolean dkconfig_remove_contradictions = true;

  /**
   * Boolean. Controls Daikon's response when inconsistent invariants
   * are discovered while running Simplify. If true, Daikon will print
   * an error message to the standard error stream listing the
   * contradictory invariants. This is mainly intended for debugging
   * Daikon itself, but can sometimes be helpful in tracing down other
   * problems. For more information, see the section on
   * troubleshooting contradictory invariants in the Daikon manual.
   **/
  public static boolean dkconfig_print_contradictions = false;

  /**
   * Boolean. If true, ask Simplify to check a simple proposition
   * after each assumption is pushed, providing an opportunity to wait
   * for output from Simplify and potentially receive error messages
   * about the assumption. When false, long sequences of assumptions
   * may be pushed in a row, so that by the time an error message
   * arrives, it's not clear which input caused the error. Of course,
   * Daikon's input to Simplify isn't supposed to cause errors, so
   * this option should only be needed for debugging.
   **/
  public static boolean dkconfig_synchronous_errors = false;

  private Stack lemmas;
  private SessionManager session;

  /** Tell Simplify to assume a lemma, which should already be on our
   * stack. */
  private void assume(Lemma lemma) throws TimeoutException {
    session.request(new CmdAssume(lemma.formula));
  }

  /** Assume a list of lemmas */
  private void assumeAll(Vector/*<Lemma>*/ invs) throws TimeoutException {
    for (int i = 0; i < invs.size(); i++) {
      assume((Lemma)invs.elementAt(i));
    }
  }

  /** Pop a lemma off Simplify's stack */
  private void unAssume() {
    try {
      session.request(CmdUndoAssume.single);
    } catch (TimeoutException e) {
      Assert.assertTrue(false, "Unexpected timeout on (BG_POP)");
    }
  }

  /** Pop a bunch of lemmas off Simplify's stack. Since it's a stack,
   * it only works to unassume the things you most recently assumed,
   * but we aren't smart enough to check that. */
  private void unAssumeAll(Vector/*<Lemma>*/ invs) {
    for (int i = 0; i < invs.size(); i++) {
      unAssume();
    }
  }

  /** Try to start Simplify */
  private void startProver() throws SimplifyError {
    session = SessionManager.attemptProverStartup();
    if (session == null) {
      throw new SimplifyError("Couldn't start Simplify");
    }
  }

  /** Try to restart Simplify back where we left off, after killing it. */
  private void restartProver() throws SimplifyError {
    startProver();
    try {
      assumeAll(lemmas);
    } catch (TimeoutException e) {
      throw new SimplifyError("Simplify restart timed out");
    }
  }

  public LemmaStack() throws SimplifyError {
    startProver();
    lemmas = new Stack();
    if (daikon.inv.Invariant.dkconfig_simplify_define_predicates)
      pushLemmas(Lemma.lemmasVector());
  }

  /** Pop a lemma from our and Simplify's stacks */
  public void popLemma() {
    unAssume();
    lemmas.pop();
  }

  /** Push an assumption onto our and Simplify's stacks. */
  public boolean pushLemma(Lemma lem) throws SimplifyError {
    SimpUtil.assert_well_formed(lem.formula);
    try {
      assume(lem);
      lemmas.push(lem);
      if (dkconfig_synchronous_errors) {
        // The following debugging code causes us to flush all our input
        // to Simplify after each lemma, and is useful to figure out
        // which lemma an error message refers to.
        try {
          checkString("(AND)");
        } catch (SimplifyError err) {
          System.err.println("Error after pushing " + lem.summarize() + " " +
                             lem.formula);
          throw err;
        }
      }

      return true;
    } catch (TimeoutException e) {
      restartProver();
      return false;
    }
  }

  /** Push a vector of assumptions onto our and Simplify's stacks. */
  public void pushLemmas(Vector lemmas) throws SimplifyError {
    for (int i = 0; i < lemmas.size(); i++) {
      Lemma l = (Lemma)lemmas.elementAt(i);
      pushLemma(l);
    }
  }

  /** Ask Simplify whether a string is a valid statement, given our
   * assumptions. Returns 'T' if Simplify says yes, 'F' if Simplify
   * says no, or '?' if we have to kill Simplify because it won't
   * answer. */
  private char checkString(String str) throws SimplifyError {
    SimpUtil.assert_well_formed(str);
    CmdCheck cc = new CmdCheck(str);
    try {
      session.request(cc);
    } catch (TimeoutException e) {
      restartProver();
      return '?';
    }
    if (cc.unknown)
      return '?';
    return cc.valid ? 'T' : 'F';
  }

  /** Ask Simplify whether a lemma is valid, given our
   * assumptions. Returns 'T' if Simplify says yes, 'F' if Simplify
   * says no, or '?' if we have to kill Simplify because it won't
   * answer. */
  public char checkLemma(Lemma lemma) throws SimplifyError {
    return checkString(lemma.formula);
  }

  /** Ask Simplify whether the assumptions we've pushed so far are
   * contradictory. Returns 'T' if Simplify says yes, 'F' if Simplify
   * says no, or '?' if we have to kill Simplify because it won't
   * answer. */
  public char checkForContradiction() throws SimplifyError {
    return checkString("(OR)"); // s/b always false
  }

  /** Return true if all the invariants in invs[i] in invs[] not
   * between min and max (inclusive) for which excluded[i] is false,
   * together imply the formula conseq. */
  private boolean allExceptImply(Lemma[] invs, boolean[] excluded,
                                 int min, int max, String conseq)
    throws TimeoutException
  {
    int assumed = 0;
    for (int i = 0; i < invs.length; i++) {
      if (!excluded[i] && (i < min || i > max)) {
        assume(invs[i]);
        assumed++;
      }
    }
    boolean valid = checkString(conseq) != 'F';
    for (int i = 0; i < assumed; i++) {
      unAssume();
    }
    return valid;
  }

  /** Return true if all the elements of bools between min and max
   * (inclusive) are true. */
  private static boolean allTrue(boolean[] bools, int min, int max) {
    for (int i = min; i <= max; i++) {
      if (!bools[i])
        return false;
    }
    return true;
  }

  /** Find a subset of invs[] that imply consequence, such that no
   * subset of that set does. Note that we may not return the smallest
   * such set. The set is currently returned in the same order as the
   * invariants appeared in invs[] */
  private Vector minimizeAssumptions(Lemma[] invs, String consequence)
    throws TimeoutException
  {
    boolean[] excluded = new boolean[invs.length];

    for (int size = invs.length / 2; size > 1; size /= 2) {
      for (int start = 0; start < invs.length; start += size) {
        int end = Math.min(start + size - 1, invs.length - 1);
        if (!allTrue(excluded, start, end) &&
            allExceptImply(invs, excluded, start, end, consequence)) {
          for (int i = start; i <= end; i++)
            excluded[i] = true;
        }
      }
    }

    boolean reduced;
    do {
      reduced = false;
      for (int i = 0; i < invs.length; i++) {
        if (!excluded[i]) {
          if (allExceptImply(invs, excluded, i, i, consequence)) {
            excluded[i] = true;
            reduced = true;
          }
        }
      }
    } while (reduced);
    Vector/*<Lemma>*/ new_invs = new Vector/*<Lemma>*/();
    for (int i = 0; i < invs.length; i++) {
      if (!excluded[i])
        new_invs.add(invs[i]);
    }
    return new_invs;
  }

  private static void shuffle(Object[] ary, Random rand) {
    for (int i = 0; i < ary.length - 1; i++) {
      int j = i + rand.nextInt(ary.length - i);
      Object temp = ary[i];
      ary[i] = ary[j];
      ary[j] = temp;
    }
  }

  /** Return a minimal set of assumptions from the stack that imply a
   * given string. */
  private Vector minimizeReasons(String str) throws SimplifyError {
    Assert.assertTrue(checkString(str) == 'T');
    unAssumeAll(lemmas);
    Vector result;
    try {
      Lemma[] lemmaAry = (Lemma[])lemmas.toArray(new Lemma[0]);
      // shuffle(lemmaAry, new Random());
      result = minimizeAssumptions(lemmaAry, str);
      assumeAll(lemmas);
    } catch (TimeoutException e) {
      System.err.println("Minimzation timed out");
      restartProver();
      return lemmas;
    }
    return result;
  }

  /** Return a set of contradictory assumptions from the stack (as a
   * vector of Lemmas) which are minimal in the sense that no proper
   * subset of them are contradictory as far as Simplify can tell. */
  public Vector minimizeContradiction() throws SimplifyError {
    return minimizeReasons("(OR)");
  }

  /** Return a set of assumptions from the stack (as a vector of
   * Lemmas) that imply the given Lemma and which are minimal in the
   * sense that no proper subset of them imply it as far as Simplify
   * can tell. */
  public Vector minimizeProof(Lemma lem) throws SimplifyError {
    return minimizeReasons(lem.formula);
  }

  /** Remove some lemmas from the stack, such that our set of
   * assumptions is no longer contradictory. This is not a very
   * principled thing to do, but it's better than just giving up. The
   * approach is relatively slow, trying not too remove too many
   * lemmas. */
  public void removeContradiction() throws SimplifyError {
    do {
      Vector problems = minimizeContradiction();
      if (problems.size() == 0) {
        throw new SimplifyError("Minimization failed");
      }
      Lemma bad = (Lemma)problems.elementAt(problems.size() - 1);
      removeLemma(bad);
      System.err.print("x");
    } while (checkForContradiction() == 'T');
  }

  /** Search for the given lemma in the stack, and then remove it from
   * both our stack and Simplify's. This is rather inefficient. */
  public void removeLemma(Lemma bad) throws SimplifyError {
    unAssumeAll(lemmas);
    int spliceOut = -1;
    for (int i = 0; i < lemmas.size(); i++) {
      Lemma lem = (Lemma)lemmas.elementAt(i);
      if (lem == bad) {
        spliceOut = i;
      } else {
        try {
          assume(lem);
        } catch (TimeoutException e) {
          throw new SimplifyError("Timeout in contradiction removal");
        }
      }
    }
    Assert.assertTrue(spliceOut != -1);
    lemmas.removeElementAt(spliceOut);
  }

  /** Blow away everything on our stack and Simplify's. */
  public void clear() {
    unAssumeAll(lemmas);
    lemmas.removeAllElements();
  }

  /** Return a reference to the current position on the lemma
   * stack. If, after pushing some stuff, you want to get back here,
   * pass the mark to popToMark(). This will only work if you use
   * these routines in a stack-disciplined way, of course. In
   * particular, beware that removeContradiction() invalidates marks,
   * since it can remove a lemma from anywhere on the stack. */
  public int markLevel() {
    return lemmas.size();
  }

  /** Pop off lemmas from the stack until its level matches mark. */
  public void popToMark(int mark) {
    while (lemmas.size() > mark)
      popLemma();
  }

  /** Convenience method to print a vector of lemmas, in both their
   * human-readable and Simplify forms. */
  public static void printLemmas(java.io.PrintStream out, Vector v) {
    for (int i = 0; i < v.size(); i++) {
      Lemma l = (Lemma)v.elementAt(i);
      out.println(l.summarize());
      out.println("    " + l.formula);
    }
  }

  /** Dump the state of the stack to a file, for debugging manually in
   * Simplify */
  public void dumpLemmas(java.io.PrintStream out) {
    for (int i = 0; i < lemmas.size(); i++) {
      Lemma l = (Lemma)lemmas.elementAt(i);
      out.println("(BG_PUSH " + l.formula + ")");
    }
  }

  private static SortedSet ints_seen = new TreeSet();

  /** Keep track that we've seen this number in formulas, for the sake
   * of assumeOrdering */
  public static void noticeInt(long i) {
    ints_seen.add(new Long(i));
  }

  public static void clearInts() {
    ints_seen = new TreeSet();
  }

  /** For all the integers we've seen, tell Simplify about the
   * ordering between them */
  public void pushOrdering() throws SimplifyError {
    Iterator longs_it = ints_seen.iterator();
    long last_long = Long.MIN_VALUE;
    while (longs_it.hasNext()) {
      long l = ((Long)longs_it.next()).longValue();
      if (l == Long.MIN_VALUE)
        continue;
      Assert.assertTrue(l != last_long);
      String formula = "(< " + SimpUtil.formatInteger(last_long) + " " +
        SimpUtil.formatInteger(l) + ")";
      Lemma lem = new Lemma(last_long + " < " + l, formula);
      pushLemma(lem);
      if (l > -32000 && l < 32000) {
        String eq_formula = "(EQ " + l + " " + SimpUtil.formatInteger(l) + ")";
        Lemma eq_lem = new Lemma(l + " == " + l, eq_formula);
        pushLemma(eq_lem);
      }
      last_long = l;
    }
  }
}
