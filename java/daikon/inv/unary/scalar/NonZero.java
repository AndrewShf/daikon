package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.binary.sequenceScalar.*;
import daikon.derive.unary.*;

import java.util.*;

import utilMDE.*;

// This also serves as NonNull.

public class NonZero
  extends SingleScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff NonZero invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  private static boolean debugNonZero = false;

  /** Smallest value seen so far. **/
  long min = Long.MAX_VALUE;
  /** Largest value seen so far. **/
  long max = Long.MIN_VALUE;
  /** Maximum value ever used for max-min in probability calculation. **/
  long range_max = 50;

  // If nonzero, use this as the range instead of the actual range.
  // This lets one use a specified probability of nonzeroness (say, 1/10
  // for pointers).
  long override_range = 0;
  boolean pointer_type = false;

  private NonZero(PptSlice ppt) {
    super(ppt);
  }

  public static NonZero instantiate(PptSlice ppt) {
    if (!dkconfig_enabled)
      return null;

    if (debugNonZero || ppt.debugged) {
      System.out.println("NonZero.instantiate(" + ppt.name + ")");
    }

    NonZero result = new NonZero(ppt);


    if (ppt.var_infos[0].file_rep_type == ProglangType.HASHCODE) {
      result.pointer_type = true;
      result.override_range = 3;
      if (!result.var().aux.getFlag(VarInfoAux.HAS_NULL)) {
        // If it's not a number and null doesn't have special meaning...
        return null;
      }
    }
    return result;
  }

  public String repr() {
    return "NonZero" + varNames() + ": "
      + !falsified + ",min=" + min + ",max=" + max;
  }

  private String zero() { return pointer_type ? "null" : "0"; }

  public String format_using(OutputFormat format) {
    String name = var().name.name_using(format);

    if ((format == OutputFormat.DAIKON)
        || (format == OutputFormat.ESCJAVA)
        || (format == OutputFormat.JML))
    {
      return name + " != " + zero();
    }

    if (format == OutputFormat.SIMPLIFY) {
      return "(NEQ " + name + " " + zero() + ")";
    }

    if (format == OutputFormat.IOA) {
      return name + " ~= "
        + (pointer_type ? "nil" : "0");
    }

    return format_unimplemented(format);
  }

  public String format_java() {
    return var().name.java_name() + " != " + zero();
  }

  public void add_modified(long v, int count) {
    if (v == 0) {
      if (debugNonZero || ppt.debugged) {
        System.out.println("NonZero.destroy(" + ppt.name + ")");
      }
      flowThis();
      destroy();
      return;
    }
    // The min and max tests will simultaneously succeed exactly once (for
    // the first value).
    if (v < min) min = v;
    if (v > max) max = v;
  }

  protected double computeProbability() {
    Assert.assertTrue(! falsified);
    if ((override_range == 0) && ((min > 0) || (max < 0))) {
      // Maybe just use 0 as the min or max instead, and see what happens:
      // see whether the "nonzero" invariant holds anyway.  (Perhaps only
      // makes sense to do if the {Lower,Upper}Bound invariant doesn't imply
      // the non-zeroness.)  In that case, do still check for no values yet
      // received.
      return Invariant.PROBABILITY_UNJUSTIFIED;
    } else {
      long range;
      if (override_range != 0) {
        range = override_range;
      } else {
        long modulus = 1;
        {
          Modulus mi = Modulus.find(ppt);
          if (mi != null) {
            modulus = mi.modulus;
          }
        }
        // Perhaps I ought to check that it's possible (given the modulus
        // constraints) for the value to be zero; otherwise, the modulus
        // constraint implies non-zero.
        range = (max - min + 1) / modulus;
      }
      if ((range_max != 0) && (range > range_max)) {
        range = range_max;
      }

      double probability_one_elt_nonzero = 1 - 1.0/range;
      // This could underflow; so consider doing
      //   double log_probability = self.samples*math.log(probability);
      // then calling Math.exp (if the value is in the range that wouldn't
      // cause underflow).
      return Math.pow(probability_one_elt_nonzero, ppt.num_mod_non_missing_samples());
    }
  }

  public boolean isObviousImplied() {
    VarInfo var = var();

    // In Java, "this" can never be non-null, so "this != null" is vacuous.
    if (var.name.name() == "this") { // interned
      return true;
    }
    if (var.name instanceof VarInfoName.Prestate &&
        ((VarInfoName.Prestate) var.name).term.name() == "this") { // interned
      return true;
    }

    // System.out.println("isObviousImplied: " + format());

    // For every EltNonZero at this program point, see if this variable is
    // an obvious member of that sequence.
    PptTopLevel parent = ppt.parent;
    for (Iterator itor = parent.invariants_iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if ((inv instanceof EltNonZero) && inv.enoughSamples()) {
        VarInfo v1 = var();
        VarInfo v2 = inv.ppt.var_infos[0];
        // System.out.println("NonZero.isObviousImplied: calling Member.isObviousMember(" + v1.name + ", " + v2.name + ")");
        // Don't use isEqualToObviousMember:  that is too subtle
        // and eliminates desirable invariants such as "return != null".
        if (Member.isObviousMember(v1, v2)) {
          // System.out.println("NonZero.isObviousImplied: Member.isObviousMember(" + v1.name + ", " + v2.name + ") = true");
          return true;
        }
      }
    }

    if ((var.derived != null)
        && (var.derived instanceof SequenceInitial)) {
      SequenceInitial si = (SequenceInitial) var.derived;
      if (si.index == 0) {

        // For each sequence variable, if var is an obvious member, and
        // the sequence has the same invariant, then this one is obvious.
        PptTopLevel pptt = ppt.parent;
        for (int i=0; i<pptt.var_infos.length; i++) {
          VarInfo vi = pptt.var_infos[i];
          if (Member.isObviousMember(var, vi)) {
            PptSlice1 other_slice = pptt.findSlice(vi);
            if (other_slice != null) {
              SeqIndexNonEqual sine = SeqIndexNonEqual.find(other_slice);
              if ((sine != null) && sine.enoughSamples()) {
                // System.out.println("NonZero.isObviousImplied true due to: " + sine.format()");
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }


  public boolean isSameFormula(Invariant other)
  {
    Assert.assertTrue(other instanceof NonZero);
    return true;
  }

  public boolean isExclusiveFormula(Invariant other)
  {
    if (other instanceof OneOfScalar) {
      OneOfScalar oos = (OneOfScalar) other;
      if ((oos.num_elts() == 1) && (((Long)oos.elt()).longValue() == 0)) {
        return true;
      }
    }
    return false;
  }

}
