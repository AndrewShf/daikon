// ***** This file is automatically generated from SequencesIntersectionFactory.java.jpp

package daikon.derive.binary;

import daikon.*;
import daikon.derive.Derivation;
import daikon.inv.binary.twoScalar.*; // for IntComparison
import daikon.inv.unary.scalar.*; // for LowerBound

import utilMDE.*;
import org.apache.log4j.Category;
import java.util.*;

// This controls derivations which use the scalar as an index into the
// sequence, such as getting the element at that index or a subsequence up
// to that index.

public final class SequenceScalarIntersectionFactory  extends BinaryDerivationFactory {

  /** Debug tracer **/
  private static final Category debug =
    Category.getInstance ("daikon.derive.binary.SequenceScalarIntersectionFactory" );

  public BinaryDerivation[] instantiate(VarInfo seq1, VarInfo seq2) {

    if (! SequenceScalarIntersection.dkconfig_enabled) {
      return null;
    }

    if ((seq1.rep_type != ProglangType.INT_ARRAY )
        || (seq2.rep_type != ProglangType.INT_ARRAY )) {
      return null;
    }

    // Intersect only sets with the same declared element type
    if (!seq1.type.base().equals(seq2.type.base()))
      return null;

    // Assert.assertTrue(seq1.isCanonical()); // [INCR]
    // Assert.assertTrue(seq2.isCanonical()); // [INCR]

    // For now, do nothing if the sequences are derived.
    //    if ((seq1.derived != null)||(seq2.derived != null))
    //  return null;

    // We allow double derivations of predicate slices because it may be interesting
    if ((seq1.derived != null)) {
      Derivation derivation = seq1.derived;
      if (!(derivation instanceof SequencesPredicate)) return null;
    }

    if ((seq2.derived != null)) {
      Derivation derivation = seq2.derived;
      if (!(derivation instanceof SequencesPredicate)) return null;
    }

    if (debug.isDebugEnabled()) {
      debug.debug ("Instantiatiating " + seq1.name + " and " + seq2.name);
    }

    return new BinaryDerivation[] {
      new SequenceScalarIntersection (seq1, seq2) };
  }
}
