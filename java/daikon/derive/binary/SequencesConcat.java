package daikon.derive.binary;

import daikon.*;
import daikon.derive.*;

import utilMDE.*;

import org.apache.log4j.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

/**
 * Represents the concatenation of two base variables.  This derived
 * variable works for both sequences of numbers and strings.
 **/

public final class SequencesConcat
  extends BinaryDerivation
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /**
   * Debug tracer
   **/
  public static final Category debug = Category.getInstance("daikon.derive.binary.SequencesConcat");

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SequencesConcat derived variables should be created.
   **/
  public static boolean dkconfig_enabled = false;

  public VarInfo var1() { return base1; }
  public VarInfo var2() { return base2; }


  /**
   * Create a new SequenceScarlarConcat that represents the concatenation
   * of two base variables.
   * @param vi1 base variable 1
   * @param vi2 base variable 2
   **/
  public SequencesConcat (VarInfo vi1, VarInfo vi2) {
    super(vi1, vi2);
  }

  public ValueAndModified computeValueAndModifiedImpl(ValueTuple full_vt) {
    Object val1 = var1().getValue(full_vt);
    Object val2 = var2().getValue(full_vt);

    int mod = ValueTuple.UNMODIFIED;
    int mod1 = base1.getModified(full_vt);
    int mod2 = base2.getModified(full_vt);

    if (mod1 == ValueTuple.MODIFIED) mod = ValueTuple.MODIFIED;
    if (mod1 == ValueTuple.MISSING_NONSENSICAL) mod = ValueTuple.MISSING_NONSENSICAL;
    if (mod2 == ValueTuple.MODIFIED) mod = ValueTuple.MODIFIED;
    if (mod2 == ValueTuple.MISSING_NONSENSICAL) mod = ValueTuple.MISSING_NONSENSICAL;

    if (val1 == null && val2 == null) {
      return new ValueAndModified (null, mod);
    }
    if (var1().rep_type == ProglangType.INT_ARRAY) {
      // val1 instanceof long[] || val2 instanceof long[]
      long[] result = ArraysMDE.concat (val1 == null ? null : (long[]) val1,
                                        val2 == null ? null : (long[]) val2);
      return new ValueAndModified(Intern.intern(result), mod);
    } else if (var1().rep_type == ProglangType.DOUBLE_ARRAY) {
       double[] result = ArraysMDE.concat(val1 == null ? null : (double[]) val1,
                                        val2 == null ? null : (double[]) val2);
       return new ValueAndModified(Intern.intern(result), mod);

    } else if (var1().rep_type == ProglangType.STRING_ARRAY) {
      // val1 instanceof String[] || val2 instanceof String[]
      String[] result = ArraysMDE.concat (val1 == null ? null : (String[]) val1,
                                          val2 == null ? null : (String[]) val2);
      return new ValueAndModified(Intern.intern(result), mod);
    } else {
      throw new Error ("Attempted to concatenate unknown arrays");
    }

  }

  protected VarInfo makeVarInfo() {
    VarInfo var1 = var1();
    return new VarInfo(VarInfoName.applyFunctionOfN("concat",
                                                    new VarInfoName[] {var1.name, var2().name}),
                       var1.type,
                       var1.file_rep_type,
                       var1.comparability,
                       var1.aux);
  }

  public String toString() {
    return "[SequencesConcat of " + var1().name + " " + var2().name + "]";

  }

  public  boolean isSameFormula(Derivation other) {
    return (other instanceof SequencesConcat);
  }

}
