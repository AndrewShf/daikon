package daikon.derive.binary;

import daikon.*;
import daikon.inv.twoString.*;

import utilMDE.*;

// This controls derivations which use the scalar as an index into the
// sequence, such as getting the element at that index or a subsequence up
// to that index.

public final class SequenceStringSubscriptFactory extends BinaryDerivationFactory {

  // When calling/creating the derivations, arrange that:
  //   var_info1 is the sequence
  //   var_info2 is the scalar

  public BinaryDerivation[] instantiate(VarInfo vi1, VarInfo vi2) {
    // This isn't the very most efficient way to do this, but at least it's
    // comprehensible.
    VarInfo seqvar;
    VarInfo sclvar;

    if ((vi1.rep_type == ProglangType.STRING_ARRAY)
        && (vi2.rep_type == ProglangType.STRING)) {
      seqvar = vi1;
      sclvar = vi2;
    } else if ((vi2.rep_type == ProglangType.STRING_ARRAY)
               && (vi1.rep_type == ProglangType.STRING)) {
      seqvar = vi2;
      sclvar = vi1;
    } else {
      return null;
    }

    if (! sclvar.isIndex())
      return null;
    // Could also do a Lackwit/Ajax comparability test here.


    // For now, do nothing if the sequence is itself derived.
    if (seqvar.derived != null)
      return null;
    // For now, do nothing if the scalar is itself derived.
    if (sclvar.derived != null)
      return null;

    Assert.assert(sclvar.isCanonical());

    VarInfo seqsize = seqvar.sequenceSize().canonicalRep();
    // System.out.println("BinaryDerivation.instantiate: sclvar=" + sclvar.name
    //                    + ", sclvar_rep=" + sclvar.canonicalRep().name
    //                    + ", seqsize=" + seqsize.name
    //                    + ", seqsize_rep=" + seqsize.canonicalRep().name);
    // Since both are canonical, this is equivalent to
    // "if (sclvar.canonicalRep() == seqsize.canonicalRep()) ...
    if (sclvar == seqsize) {
      Global.tautological_suppressed_derived_variables += 4;
      return null;
    }

    // ***** This eliminates the derivation if it can *ever* be
    // nonsensical/missing.  Is that what I want?

    // Find an StringComparison relationship over the scalar and the sequence
    // size, if possible.
    Assert.assert(sclvar.ppt == seqsize.ppt);
    PptSlice compar_slice = sclvar.ppt.getView(sclvar, seqsize);
    if (compar_slice != null) {
      StringComparison compar = StringComparison.find(compar_slice);
      if (compar != null) {
        if ((sclvar.varinfo_index < seqsize.varinfo_index)
            ? compar.core.can_be_gt // sclvar can be less than seqsize
            : compar.core.can_be_lt // seqsize can be less than sclvar
            ) {
          Global.nonsensical_suppressed_derived_variables += 4;
          return null;
        }
      }
    }

    // End of applicability tests; now actually create the invariants

    return new BinaryDerivation[] {
      new SequenceStringSubscript(seqvar, sclvar, false),
      new SequenceStringSubscript(seqvar, sclvar, true),
      new SequenceStringSubsequence(seqvar, sclvar, false),
      new SequenceStringSubsequence(seqvar, sclvar, true),
    };
  }

}
