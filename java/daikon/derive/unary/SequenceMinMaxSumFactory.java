package daikon.derive.unary;

import java.util.ArrayList;
import daikon.Daikon;
import daikon.VarInfo;
import daikon.ProglangType;
import daikon.inv.OutputFormat;

public final class SequenceMinMaxSumFactory extends UnaryDerivationFactory {

  public UnaryDerivation[] instantiate(VarInfo vi) {
    // System.out.println("SequenceMinMaxSumFactory.instantiate(" + vi.name + ")");

    if (vi.rep_type != ProglangType.INT_ARRAY)
      return null;
    if (! vi.type.isArray())
      return null;
    if (! vi.type.elementIsIntegral() && ! vi.type.elementIsFloat())
      return null;
    if (vi.type.base() == "char") // interned
      return null;
    // Should be reversed at some point; for now, will improve runtime.
    if (Daikon.output_format != OutputFormat.DAIKON)
      return null;

    ArrayList result = new ArrayList(3);
    if (SequenceMin.dkconfig_enabled) { result.add(new SequenceMin(vi)); }
    if (SequenceMax.dkconfig_enabled) { result.add(new SequenceMax(vi)); }
    if (SequenceSum.dkconfig_enabled) { result.add(new SequenceSum(vi)); }

    if (result.size() == 0) {
      return null;
    }

    return (UnaryDerivation[]) result.toArray(new UnaryDerivation[result.size()]);
  }

}
