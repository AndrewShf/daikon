package daikon.inv.unary.sequence;

import daikon.*;

/**
 * Class defined so that the different types of EltwiseIntComparison (and
 * seperately EltwiseFloatComparison), at the current moment those are ==,
 * !=, <, <=, >, >= can have a common superclass which is needed for
 * actions like the isExclusiveFormula method. It is also used to force the
 * definition of the hasSeenNonTrivialSample function for each type of
 * EltwiseIntComparison.
 */

public abstract class EltwiseIntComparison
  extends SingleScalarSequence
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030109L;

  public static boolean dkconfig_enabled = true;

  abstract public boolean hasSeenNonTrivialSample();

  protected EltwiseIntComparison(PptSlice ppt) {
    super(ppt);
  }
}
