  // XXX There is no SubSequenceString(?)

// ***** This file is automatically generated from Member.java.jpp

package daikon.inv.binary.sequenceString;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.twoScalar.*;
import daikon.inv.binary.twoSequence.*;
import daikon.inv.binary.twoScalar.IntLessThan;
import daikon.inv.binary.twoScalar.IntGreaterThan;
import daikon.inv.binary.twoScalar.IntLessEqual;
import daikon.inv.binary.twoScalar.IntGreaterEqual;
import daikon.derive.*;
import daikon.derive.unary.*;
import daikon.derive.binary.*;
import daikon.derive.ternary.*;
import daikon.suppress.*;
import java.util.*;
import utilMDE.*;
import org.apache.log4j.Logger;

public final class Member
  extends SequenceString
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public static final Logger debug =
    Logger.getLogger ("daikon.inv.binary.Member");

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff Member invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  protected Member(PptSlice ppt, boolean seq_first) {
    super(ppt, seq_first);
    Assert.assertTrue(sclvar().rep_type == ProglangType.STRING);
    Assert.assertTrue(seqvar().rep_type == ProglangType.STRING_ARRAY);
  }

  // This constructor enables testing with InvariantFormatTester.
  public static Member instantiate(PptSlice ppt) {
    return instantiate(ppt,true);
  }

  public static Member instantiate(PptSlice ppt, boolean seq_first) {
    if (!dkconfig_enabled) return null;

    VarInfo seqvar = ppt.var_infos[seq_first ? 0 : 1];
    VarInfo sclvar = ppt.var_infos[seq_first ? 1 : 0];

    if (debug.isDebugEnabled()) {
      debug.debug ("Member instantiated: "
                   + sclvar.name + " in " + seqvar.name);
    }
    return new Member(ppt, seq_first);
  }

  public boolean isObviousStatically(VarInfo[] vis) {
    VarInfo seqvar = vis[seq_first ? 0 : 1];
    VarInfo sclvar = vis[seq_first ? 1 : 0];
    if (isObviousMember(sclvar, seqvar)) {
      return true;
    }
    return super.isObviousStatically (vis);
  }

  /* [INCR]  
  // Like isObviousMember, but also checks everything equal to the given
  // variables.
  public static boolean isEqualToObviousMember(VarInfo sclvar, VarInfo seqvar) {
    Assert.assertTrue(sclvar.isCanonical());
    Assert.assertTrue(seqvar.isCanonical());
    Vector scl_equalto = sclvar.equalTo();
    scl_equalto.add(0, sclvar);
    Vector seq_equalto = seqvar.equalTo();
    seq_equalto.add(0, seqvar);

    for (int sclidx=0; sclidx<scl_equalto.size(); sclidx++) {
      for (int seqidx=0; seqidx<seq_equalto.size(); seqidx++) {
        VarInfo this_sclvar = (VarInfo) scl_equalto.elementAt(sclidx);
        VarInfo this_seqvar = (VarInfo) seq_equalto.elementAt(seqidx);
        if (isObviousMember(this_sclvar, this_seqvar))
          return true;
      }
    }
    return isObviousMember (sclvar, seqvar);
  }
  */ // [INCR]

  /**
   * Check whether sclvar is a member of seqvar can be determined
   * statically.
   **/
  public static boolean isObviousMember(VarInfo sclvar, VarInfo seqvar) {

    VarInfo sclvar_seq = sclvar.isDerivedSequenceMember();
    // System.out.println("Member.isObviousMember(" + sclvar.name + ", " + seqvar.name + "):");
    // System.out.println("  sclvar.derived=" + sclvar.derived
    //                    + ", sclvar_seq=" + ((sclvar_seq == null) ? "null" : sclvar_seq.name));

    if (sclvar_seq == null) {
      // The scalar is not obviously (lexically) a member of any array.
      return false;
    }
    // isObviousImplied: a[i] in a; max(a) in a
    if (sclvar_seq == seqvar) {
      // The scalar is a member of the same array.
      return true;
    }
    // The scalar is a member of a different array than the sequence.
    // But maybe the relationship is still obvious, so keep checking.

    // isObviousImplied: when b==a[0..i]:  b[j] in a; max(b) in a
    // If the scalar is a member of a subsequence of the sequence, then
    // the scalar is a member of the full sequence.
    // This is satisfied, for instance, when determining that
    // max(B[0..I]) is an obvious member of B.
    VarInfo sclseqsuper = sclvar_seq.isDerivedSubSequenceOf();
    if (sclseqsuper == seqvar)
      return true;

    // We know the scalar was derived from some array, but not from the
    // sequence variable.  If also not from what the sequence variable was
    // derived from, we don't know anything about membership.
    // Check:
    //  * whether comparing B[I] to B[0..J]
    //  * whether comparing min(B[0..I]) to B[0..J]
    VarInfo seqvar_super = seqvar.isDerivedSubSequenceOf();
    if ((seqvar_super != sclvar_seq)
        && (seqvar_super != sclseqsuper)) {
      // System.out.println("Member.isObviousMember(" + sclvar.name + ", " + seqvar.name + "):"
      //                    + " isDerivedSubSequenceOf() != " + sclvar_seq.name);
      return false;
    }

    // If the scalar is a positional element of the sequence from which
    // the sequence at hand was derived, then any relationship will be
    // (mostly) obvious by comparing the length of the sequence to the
    // index.  By contrast, if the scalar is max(...) or min(...), all bets
    // are off.

    if (seqvar.derived instanceof SequenceStringSubsequence ||
        seqvar.derived instanceof SequenceStringArbitrarySubsequence) {
      VarInfo seq_left_index = null, seq_right_index = null;
      // I'm careful not to access foo_shift unless foo_var has been set
      // to a non-null value, but Java is too stupid to recognize that.
      int seq_left_shift = 0xdead, seq_right_shift = 0xbeef;
      if (seqvar.derived instanceof SequenceStringSubsequence) {
        // the sequence is B[0..J-1] or similar.  Get information about it.
        SequenceStringSubsequence seqsss
          = (SequenceStringSubsequence)seqvar.derived;
        if (seqsss.from_start) {
          seq_right_index = seqsss.sclvar();
          seq_right_shift = seqsss.index_shift;
        } else {
          seq_left_index = seqsss.sclvar();
          seq_left_shift = seqsss.index_shift;
        }
      } else if (seqvar.derived instanceof SequenceStringArbitrarySubsequence) {
        // the sequence is B[I+1..J] or similar
        SequenceStringArbitrarySubsequence ssass = (SequenceStringArbitrarySubsequence)seqvar.derived;
        seq_left_index = ssass.startvar();
        seq_left_shift = (ssass.left_closed ? 0 : 1);
        seq_right_index = ssass.endvar();
        seq_right_shift = (ssass.right_closed ? 0 : -1);
      } else {
        Assert.assertTrue(false);
      }

      if (sclvar.derived instanceof SequenceStringSubscript) {
        // B[I] in B[0..J]

        SequenceStringSubscript sclsss = (SequenceStringSubscript) sclvar.derived;
        VarInfo scl_index = sclsss.sclvar(); // "I" in "B[I]"
        int scl_shift = sclsss.index_shift;
        // System.out.println("scl_shift = " + scl_shift + ", seq_shift = " + seq_shift);

        boolean left_included, right_included;
        if (seq_left_index == null)
          left_included = true;
        else
          left_included = VarInfo.compare_vars(scl_index, scl_shift,
                                               seq_left_index, seq_left_shift,
                                               false /* >= */);
        if (seq_right_index == null)
          right_included = true;
        else
          right_included
            = VarInfo.compare_vars(scl_index, scl_shift,
                                   seq_right_index, seq_right_shift,
                                   true /* <= */);
//         System.out.println("Is " + sclvar.name.name() + " contained in "
//                            + seqvar.name.name()
//                            + "? left: " + left_included + ", right: "
//                            + right_included);
        if (left_included && right_included)
          return true;
      } else if (sclvar.derived instanceof SequenceInitial) {
        // System.out.println("sclvar derived from SequenceInitial: " + sclvar.name);

        // isObviousImplied: B[0] in B[0..J]; also B[-1] in B[J..]
        SequenceInitial sclse = (SequenceInitial) sclvar.derived;
        int scl_index = sclse.index;
        if (((scl_index == 0) && seq_left_index == null)
            || ((scl_index == -1) && seq_right_index == null))
          // It might not be true, because the array could be empty;
          // but if the array isn't empty, then it's obvious.
          return true;
      } else if ((sclvar.derived instanceof SequenceMin)
                 || (sclvar.derived instanceof SequenceMax)) {
        return SubSequence.isObviousSubSequence(sclvar_seq, seqvar);
      }
    }

    /// I need to test this code!
    // Now do tests over variable name, to avoid invariants like:
    //   header.next in header.~ll~next~
    //   header.next.element in header.~ll~next~.element
    //   header.next in header.next.~ll~next~
    //   return.current in return.current.~ll~next~
    String sclname = sclvar.name.name(); // mistere adds: this code
    String seqname = seqvar.name.name(); // looks pretty sketchy (XXX)
    int llpos = seqname.indexOf("~ll~");
    if (llpos != -1) {
      int tildepos = seqname.indexOf("~", llpos+5);
      if (tildepos != -1) {
        int midsize = tildepos-llpos-4;
        int lastsize = seqname.length()-tildepos-1;
        if (seqname.regionMatches(0, sclname, 0, llpos)
            && (((tildepos == seqname.length() - 1)
                 && (llpos == sclname.length()))
                || (seqname.regionMatches(llpos+4, sclname, llpos, midsize)
                    && seqname.regionMatches(tildepos+1, sclname, tildepos-4, lastsize))))
          // isObviousImplied: to do
          return true;
      }
    }

    // int lastdot = sclvar.lastIndexOf(".");
    // if (lastdot != -1) {
    //   if (sclname.substring(0, lastdot).equals(seqname.substring(0, lastdot))
    //       && seqname.substring(lastdot).equals("~ll~" + sclname.substring(lastdot) + "~")) {
    //     return true;
    //   }
    // }

    return false;
  }

  public String repr() {
    return "Member" + varNames() + ": "
      + "falsified=" + falsified;
  }

  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON) {
      return format_daikon();
    } else if (format == OutputFormat.JAVA) {
      return format_java();
    } else if (format == OutputFormat.IOA) {
      return format_ioa();
    } else if (format == OutputFormat.SIMPLIFY) {
      return format_simplify();
    } else if (format == OutputFormat.ESCJAVA) {
      return format_esc();
    } else if (format == OutputFormat.JML) {
      return format_jml();
    } else {
      return format_unimplemented(format);
    }
  }

  public String format_daikon() {
    return sclvar().name.name() + " in " + seqvar().name.name();
  }

  public String format_java() {
    return "( (daikon.inv.FormatJavaHelper.memberOf("
      + sclvar().name.name()
      + " , " + seqvar().name.name() + " ) == true ) ";
  }

  public String format_ioa() {
    return sclvar().name.ioa_name() + " \\in " + seqvar().name.ioa_name();
  }

  // XXX this "#ifdef SCALAR" test seems unlikely to have its original
  // meaning, since it's currently always true -smcc

  public String format_esc() {
    // "exists x in a..b : P(x)" gets written as "!(forall x in a..b : !P(x))"
    String[] form =
      VarInfoName.QuantHelper.format_esc(new VarInfoName[]
        { seqvar().name, sclvar().name });
    return "!" + form[0] + "(" + form[1] + " != " + form[2] + ")" + form[3];
  }

  public String format_jml() {
    // Uses jml exists option
    String[] form =
      VarInfoName.QuantHelper.format_jml(new VarInfoName[]
        { seqvar().name, sclvar().name },false,false);
    return form[0] + form[1] + " == " + form[2] + form[3];
  }

  public String format_simplify() {
    // "exists x in a..b : P(x)" gets written as "!(forall x in a..b : !P(x))"
    String[] form =
      VarInfoName.QuantHelper.format_simplify(new VarInfoName[]
        { seqvar().name, sclvar().name });
    return "(NOT " + form[0] + "(NEQ " + form[1] + " " + form[2] + ")" + form[3] + ")";
  }

  public void add_modified(String[] a, String i, int count) {
    if (ArraysMDE.indexOf(a, i) == -1) {
      if (debug.isDebugEnabled()) {
        debug.debug ("Member destroyed:  " + format() + " because " + i +
                     " not in " + ArraysMDE.toString(a));
      }
      destroyAndFlow();
      return;
    }
  }

  protected double computeProbability() {
    if (falsified)
      return Invariant.PROBABILITY_NEVER;
    else
      return Invariant.PROBABILITY_JUSTIFIED;
  }

  public boolean isSameFormula(Invariant other)
  {
    Assert.assertTrue(other instanceof Member);
    return true;
  }

  private static SuppressionFactory[] suppressionFactories = null;

  public SuppressionFactory[] getSuppressionFactories() {
    if (suppressionFactories == null) {
      SuppressionFactory[] supers = super.getSuppressionFactories();
      suppressionFactories = new SuppressionFactory[supers.length + 2];
      System.arraycopy (supers, 0, suppressionFactories, 0, supers.length);
      suppressionFactories[supers.length] = MemberSuppressionFactory1.getInstance();
      suppressionFactories[supers.length + 1] = MemberSuppressionFactory2.getInstance();
    }
    return suppressionFactories;
  }

  /**
   * Suppression in the form of A subset B => A[i] member B.  Note
   * that A[i] could also be max(A), etc.
   **/
  public static class MemberSuppressionFactory1 extends SuppressionFactory {

    public static final Logger debug =
      Logger.getLogger("daikon.suppress.factories.MemberSuppressionFactory");

    private static final MemberSuppressionFactory1 theInstance =
      new MemberSuppressionFactory1();

    private MemberSuppressionFactory1() {
      template = new SuppressionTemplate();
      template.invTypes = new Class[1];
      template.varInfos = new VarInfo[][] {new VarInfo[2]};
    }

    public static SuppressionFactory getInstance() {
      return theInstance;
    }

    private Object readResolve() {
      return theInstance;
    }

    private transient SuppressionTemplate template;

    public SuppressionLink generateSuppressionLink (Invariant arg) {
      Assert.assertTrue (arg instanceof Member);
      Member inv = (Member) arg;
      VarInfo sclSequence = inv.sclvar().isDerivedSequenceMember();
      if (sclSequence == null) return null;
      VarInfo seqvar = inv.seqvar();
      if (sclSequence.isDerivedSubSequenceOf() == seqvar) {
        return null;
        // This should never get instantiated
      }

      template.resetResults();
      template.varInfos[0][0] = sclSequence;
      template.varInfos[0][1] =  seqvar;
      // Shouldn't happen, because isObvious should
      // have handled it, but in case it does, no big deal, just say no suppression
      if (sclSequence == seqvar) return null;
      {
        template.invTypes[0] = PairwiseIntComparison.class;
        SuppressionLink sl = byTemplate (template, inv);
        if (sl != null) {
          String comparator = ((PairwiseIntComparison) template.results[0]).getComparator();
          if (comparator.indexOf ("==") != -1 ||
              comparator.indexOf ("?") != -1) {
            return sl;
          }
        }
      }

      {
        // Try to see if SubSet invariant is there
        template.resetResults();
        template.invTypes[0] = SubSet.class;
        SuppressionLink sl = byTemplate (template, inv);
        if (sl != null) {
          // First transformed var in first invariant
          VarInfo transSclSequence = template.transforms[0][0];
          // Second transformed var in first invariant
          VarInfo transSeqvar = template.transforms[0][1];
          SubSet subSet = (SubSet) template.results[0];
          if ((subSet.var1_in_var2 && subSet.var1() == transSclSequence) ||
              (subSet.var2_in_var1 && subSet.var2() == transSclSequence)) {
            if (debug.isDebugEnabled()) {
              debug.debug ("Suppressed by subset: " + subSet.repr());
              debug.debug ("  sclSeq " + transSclSequence.name.name());
              debug.debug ("  seqVar " + transSeqvar.name.name());
            }
            return sl;
          }
        }
      }

      {
        // Failed on finding the right SubSet invariant.  Now try SubSequence
        template.resetResults();
        template.invTypes[0] = SubSequence.class;
        SuppressionLink sl = byTemplate (template, inv);
        if (sl != null) {
          // First transformed var in first invariant
          VarInfo transSclSequence = template.transforms[0][0];
          // Second transformed var in first invariant
          VarInfo transSeqvar = template.transforms[0][1];
          SubSequence subSeq = (SubSequence) template.results[0];
          if ((subSeq.var1_in_var2 && subSeq.var1() == transSclSequence) ||
              (subSeq.var2_in_var1 && subSeq.var2() == transSclSequence)) {
            return sl;
          }
        }
      }
      return null;
    }
  }

  /**
   * Suppression in the form of <pre>  0<=i<=j  ==>  b[i] in b[0..j] </pre>
   **/
  public static class MemberSuppressionFactory2 extends SuppressionFactory {

    public static final Logger debug =
      Logger.getLogger("daikon.suppress.factories.MemberSuppressionFactory2");

    private static final MemberSuppressionFactory2 theInstance =
      new MemberSuppressionFactory2();

    public static SuppressionFactory getInstance() {
      return theInstance;
    }

    private Object readResolve() {
      return theInstance;
    }

    /**
     * Check if leftIndex < rightIndex.
     **/
    public SuppressionLink generateSuppressionLink (Invariant arg) {
      Assert.assertTrue (arg instanceof Member);
      Member inv = (Member) arg;
      VarInfo sclvar = inv.sclvar();
      VarInfo sclSequence = sclvar.isDerivedSequenceMember();
      if (debug.isDebugEnabled()) {
        debug.debug ("Trying for: " + inv.repr());
      }

      if (sclSequence == null) {
        debug.debug ("  Sclvar is not from a sequence");
        return null;
      }
      SequenceStringSubscript sssc = (SequenceStringSubscript) inv.sclvar().derived;
      VarInfo leftIndex = sssc.sclvar();
      VarInfo seqvar = inv.seqvar();
      VarInfo origSeqvar = seqvar.isDerivedSubSequenceOf();
      if (origSeqvar == null) {
        debug.debug ("  Seqvar is not a subsequence derived var");
        return null;
      }
      if (sclSequence != origSeqvar) {
        debug.debug ("  Not from the same sequences");
        return null;
      }
      SequenceStringSubsequence ssss = (SequenceStringSubsequence) seqvar.derived;
      VarInfo rightIndex = ssss.sclvar();
      if (debug.isDebugEnabled()) {
        debug.debug ("  Attempting to find <= template for: ");
        debug.debug ("  " + leftIndex.name.name());
        debug.debug ("  " + rightIndex.name.name());
        debug.debug ("  In inv: " + inv.repr());
      }
      if (leftIndex == rightIndex) {
        return null;
      }

      // Here's the math, explained:

      // We want A[i] in A[0..j+shift]
      // i <= j+shift
      // That's like saying:
      // i <= j + interval
      // interval = - shift

      // We want A[i] in A[j+shift..]
      // j+shift <= i
      // That's like saying:
      // j <= i + interval
      // interval = shift

      int interval = 0;
      if (ssss.from_start) {
        interval = sssc.index_shift - ssss.index_shift ;
        return findLessEqual (leftIndex, rightIndex, inv, interval);
      } else {
        interval = ssss.index_shift + sssc.index_shift;
        return findLessEqual (rightIndex, leftIndex, inv, interval);
      }
    }
  }

}
