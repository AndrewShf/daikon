package daikon.derive.binary;

import daikon.*;
import daikon.derive.*;

import utilMDE.*;

/**
 * Derivations of the form A[0..i] or A[i..<end>], derived from A and
 * i.
 **/
public abstract class SequenceSubsequence
  extends BinaryDerivation
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020801L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.

  // base1 is the sequence
  // base2 is the scalar
  public VarInfo seqvar() { return base1; }
  public VarInfo sclvar() { return base2; }

  // Indicates whether the subscript is an index of valid data or a limit
  // (one element beyond the data of interest).  The first (or last)
  // element of the derived variable is seqvar()+index_shift.
  public final int index_shift;

  // True for deriving from the start of the sequence to the scalar: B[0..I]
  // False for deriving from the scalar to the end of the sequence: B[I..]
  public final boolean from_start;

  /**
   * @param from_start true means the range goes 0..n; false means the
   * range goes n..end.  (n might be fudged through off_by_one)
   * @param off_by_one true means we should exclude the scalar from
   * the range; false means we should include it
   **/
  public SequenceSubsequence (VarInfo vi1, VarInfo vi2, boolean from_start, boolean off_by_one) {
    super(vi1, vi2);
    this.from_start = from_start;
    if (off_by_one)
      index_shift = from_start ? -1 : +1;
    else
      index_shift = 0;
  }


  protected VarInfo makeVarInfo() {
    VarInfo seqvar = seqvar();
    VarInfo sclvar = sclvar();

    VarInfoName viname;
    if (from_start) {
      if (index_shift == 0) {
        // q[0..c]
        viname = seqvar.name.applySlice(null, sclvar.name);
      } else if (index_shift == -1) {
        // q[0..c-1]
        viname = seqvar.name.applySlice(null, sclvar.name.applyDecrement());
      } else {
        throw new UnsupportedOperationException("Unsupported shift: " + index_shift);
      }
    } else {
      if (index_shift == 0) {
        // q[c..]
        viname = seqvar.name.applySlice(sclvar.name, null);
      } else if (index_shift == 1) {
        // q[c+1..]
        viname = seqvar.name.applySlice(sclvar.name.applyIncrement(), null);
      } else {
        throw new UnsupportedOperationException("Unsupported shift: " + index_shift);
      }
    }

    return new VarInfo(viname, seqvar.type, seqvar.file_rep_type,
                       seqvar.comparability, seqvar.aux);
  }

}
