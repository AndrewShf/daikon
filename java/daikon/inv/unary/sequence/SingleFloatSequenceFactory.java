// ***** This file is automatically generated from SingleSequenceFactory.java.jpp

package daikon.inv.unary.sequence;

import daikon.*;

import utilMDE.*;

import java.util.*;

public final class SingleFloatSequenceFactory {

  // Adds the appropriate new Invariant objects to the specified Invariants
  // collection.
  public static Vector instantiate(PptSlice ppt) {

    VarInfo var = ppt.var_infos[0];
    Assert.assertTrue(var.rep_type == ProglangType. DOUBLE_ARRAY);
    Assert.assertTrue(var.type.pseudoDimensions() > 0);

    Vector result = new Vector();
    { // previously (pass == 1)
      result.add(OneOfFloatSequence.instantiate(ppt));
      result.add(EltOneOfFloat.instantiate(ppt));
    }
    { // previously (pass == 2)
      // EltOneOfFloat eoo = EltOneOfFloat.find(ppt);
      // if (!((eoo != null) && (eoo.num_elts() == 1)))
      {
        result.add(EltNonZeroFloat.instantiate(ppt));
        result.add(NoDuplicatesFloat.instantiate(ppt));
        result.add(CommonFloatSequence.instantiate(ppt));
        if (var.type. elementIsFloat()) {
          result.add(EltwiseFloatComparison.instantiate(ppt));
          result.add(EltLowerBoundFloat.instantiate(ppt));
          result.add(EltUpperBoundFloat.instantiate(ppt));
          result.add(SeqIndexComparisonFloat.instantiate(ppt));
          result.add(SeqIndexNonEqualFloat.instantiate(ppt));
        } else {
          result.add(EltwiseFloatComparison.instantiate(ppt));
        }
      }
    }
    return result;
  }

  private SingleFloatSequenceFactory () {
  }

}
