// ***** This file is automatically generated from SeqComparison.java.jpp

package daikon.inv.binary.twoSequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.twoScalar.*;

import utilMDE.*;

import java.util.*;

/**
 * Compares two sequences.  If order does matter, then sequences are
 * compared lexically.  We assume that if repeats don't matter, then
 * the given data contains only one instance of an element.  If order
 * doesn't matter, then we do a double subset comparison to test
 * equality.  If the two Aux fields of the VarInfos are not identical,
 * then we don't compare at all.
 **/
public class SeqComparisonFloat
  extends TwoSequenceFloat
  implements Comparison
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SeqComparisonFloat invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  static Comparator comparator = new ArraysMDE. DoubleArrayComparatorLexical() ;

  public final boolean only_check_eq;

  boolean can_be_eq = false;
  boolean can_be_lt = false;
  boolean can_be_gt = false;

  boolean orderMatters;

  int num_sc_samples = 0;
  private FloatValueTracker values_cache = new FloatValueTracker(8);

  protected SeqComparisonFloat (PptSlice ppt, boolean only_eq, boolean order) {
    super(ppt);
    only_check_eq = only_eq;
    orderMatters = order;
  }

  public static SeqComparisonFloat instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;

    VarInfo var1 = ppt.var_infos[0];
    VarInfo var2 = ppt.var_infos[1];

    if (var1.aux != var2.aux) return null;
    // Equality does not make sense if the auxiliary info for the two
    // arrays are different.  Or does it??

    // System.out.println("Ppt: " + ppt.name);
    // System.out.println("vars[0]: " + var1.type.format());
    // System.out.println("vars[1]: " + var2.type.format());

    if ((SubSequenceFloat.isObviousDerived(var1, var2))
        || (SubSequenceFloat.isObviousDerived(var2, var1))) {
      Global.implied_noninstantiated_invariants++;
      return null;
    }

    ProglangType type1 = var1.type;
    ProglangType type2 = var2.type;
    // This intentonally checks dimensions(), not pseudoDimensions.
    boolean only_eq = (! ((type1.dimensions() == 1)
                          && type1. baseIsFloat()
                          && (type2.dimensions() == 1)
                          && type2. baseIsFloat()));
    // System.out.println("only_eq: " + only_eq);
    if (var1.aux.getFlag(VarInfoAux.HAS_ORDER)
        && var2.aux.getFlag(VarInfoAux.HAS_ORDER)) {
      return new SeqComparisonFloat(ppt, only_eq, true);
    } else {
      return new SeqComparisonFloat(ppt, true, false);
    }
  }

  protected Object clone() {
    SeqComparisonFloat result = (SeqComparisonFloat) super.clone();
    result.values_cache = (FloatValueTracker) values_cache.clone();
    return result;
  }

  protected Invariant resurrect_done_swapped() {
    boolean tmp = can_be_lt;
    can_be_gt = can_be_lt;
    can_be_gt = tmp;
    return this;
  }

  public String repr() {
    return "SeqComparisonFloat" + varNames() + ": "
      + "can_be_eq=" + can_be_eq
      + ",can_be_lt=" + can_be_lt
      + ",can_be_gt=" + can_be_gt
      + ",only_check_eq=" + only_check_eq
      + ",orderMatters=" + orderMatters
      + ",enoughSamples=" + enoughSamples()
      ;
  }

  public String format_using(OutputFormat format) {
    // System.out.println("Calling SeqComparisonFloat.format for: " + repr());
    String comparator = FloatComparisonCore.format_comparator
      (format, can_be_lt, can_be_eq, can_be_gt);

    if ((format == OutputFormat.DAIKON)
        || (format == OutputFormat.JAVA))
    {
      String name1 = var1().name.name_using(format);
      String name2 = var2().name.name_using(format);
      String lexically = (var1().aux.getFlag(VarInfoAux.HAS_ORDER)
                          ? " (lexically)"
                          : "");
      return name1 + " " + comparator + " " + name2 + lexically;
    }

    if (format == OutputFormat.IOA) {
      if (var1().isIOASet() || var2().isIOASet()) {
        return "Not valid for Sets: " + format();
      }
      String name1 = var1().name.name_using(format);
      String name2 = var2().name.name_using(format);
      return name1 + " " + comparator + " " + name2 + " ***";
    }

    if (format == OutputFormat.JML) { // Must complete
      String quantResult[] = VarInfoName.QuantHelper.format_jml(new VarInfoName[] {var1().name,var2().name},true);
      return quantResult[0] + quantResult[1] + comparator + quantResult[2] + quantResult[3];
    }
    return format_unimplemented(format);
  }

  public void add_modified(double [] v1, double [] v2, int count) {
    /// This does not do the right thing; I really want to avoid comparisons
    /// if one is missing, but not if one is zero-length.
    // // Don't make comparisons with empty arrays.
    // if ((v1.length == 0) || (v2.length == 0)) {
    //   return;
    // }
    num_sc_samples += count;

    int comparison = 0;
        if (orderMatters) {
      // Standard element wise comparison
       comparison = comparator.compare(v1, v2);
    } else {
      // Do a double subset comparison
      comparison = ArraysMDE.isSubset (v1, v2) && ArraysMDE.isSubset (v2, v1) ? 0 : -1;
    }

    // System.out.println("SeqComparisonFloat" + varNames() + ": "
    //                    + "compare(" + ArraysMDE.toString(v1)
    //                    + ", " + ArraysMDE.toString(v2) + ") = " + comparison);

    boolean new_can_be_eq = can_be_eq;
    boolean new_can_be_lt = can_be_lt;
    boolean new_can_be_gt = can_be_gt;
    boolean changed = false;
    if (comparison == 0) {
      new_can_be_eq = true;
      changed = true;
    } else if (comparison < 0) {
      new_can_be_lt = true;
      changed = true;
    } else {
      new_can_be_gt = true;
      changed = true;
    }

    if (! changed) {
      values_cache.add(v1, v2);
      return;
    }

    if ((new_can_be_lt && new_can_be_gt)
        || (only_check_eq && (new_can_be_lt || new_can_be_gt))) {
      destroyAndFlow();
      return;
    }

    // changed but didn't die
    cloneAndFlow();
    can_be_eq = new_can_be_eq;
    can_be_lt = new_can_be_lt;
    can_be_gt = new_can_be_gt;

    values_cache.add(v1, v2);
  }

  protected double computeProbability() {
    if (falsified) {
      return Invariant.PROBABILITY_NEVER;
    } else if (can_be_lt || can_be_gt) {
      // System.out.println("prob = " + Math.pow(.5, ppt.num_values()) + " for " + format());
      return Math.pow(.5, values_cache.num_values());
    } else if (num_sc_samples == 0) {
      return Invariant.PROBABILITY_UNJUSTIFIED;
    } else {
      return Invariant.PROBABILITY_JUSTIFIED;
    }
  }

  // For Comparison interface
  public double eq_probability() {
    if (can_be_eq && (!can_be_lt) && (!can_be_gt))
      return computeProbability();
    else
      return Invariant.PROBABILITY_NEVER;
  }

  public boolean isSameFormula(Invariant o)
  {
    SeqComparisonFloat other = (SeqComparisonFloat) o;
    return
      (can_be_eq == other.can_be_eq) &&
      (can_be_lt == other.can_be_lt) &&
      (can_be_gt == other.can_be_gt);
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof SeqComparisonFloat) {
      SeqComparisonFloat other = (SeqComparisonFloat) o;
      return (! ((can_be_eq && other.can_be_eq)
                 || (can_be_lt && other.can_be_lt)
                 || (can_be_gt && other.can_be_gt)));
    }
    return false;
  }

  // Copied from IntComparison.
  public boolean isObviousImplied() {
    PairwiseFloatComparison pic = PairwiseFloatComparison.find(ppt);
    if ((pic != null)
        && (pic.core.can_be_eq == can_be_eq)
        && (pic.core.can_be_lt == can_be_lt)
        && (pic.core.can_be_gt == can_be_gt)) {
      return true;
    }

    return false;
  }

}
