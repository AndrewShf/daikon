package daikon.test;

import daikon.LogHelper;

import junit.framework.*;
import junit.textui.*;
import utilMDE.*;

/**
 * This class runs all the Daikon unit tests.  These tests are small, fast
 * to run, and certainly not comprehensive.  Daikon also has a much more
 * substantial set of regression tests; in the CVS repository, they can be
 * found at invariants/tests/.
 **/
public class MasterUnitTester extends TestCase {

  public static void main(String[] args) {
    TestRunner runner = new TestRunner();
    TestResult result = runner.doRun(suite(), false);
    if (! result.wasSuccessful()) {
      throw new daikon.Daikon.TerminationMessage("Unsuccessful test!");
    }
  }

  public MasterUnitTester(String name) {
    super(name);
  }

  public static Test suite() {
    LogHelper.setupLogs (LogHelper.INFO);

    TestSuite result = new TestSuite();

    // To determine what should be in this list:
    //   find . -name '*Test*.java' | perl -pe 's/^\./      daikon.test/; s:/:.:g; s/.java/.class,/;' | grep -v MasterUnitTester | sort

    Class[] classes = new Class[] {
      daikon.test.config.ConfigurationTest.class,
      daikon.test.diff.ConsequentCVFPairComparatorTester.class,
      daikon.test.diff.ConsequentCVFSortComparatorTester.class,
      daikon.test.diff.DetailedStatisticsVisitorTester.class,
      daikon.test.diff.DiffTester.class,
      daikon.test.diff.InvMapTester.class,
      daikon.test.diff.MinusVisitorTester.class,
      daikon.test.diff.PrintDifferingInvariantsVisitorTester.class,
      daikon.test.diff.UnionVisitorTester.class,
      daikon.test.diff.XorVisitorTester.class,
      daikon.test.InvariantFormatTester.class,
      daikon.test.SampleTester.class,
      daikon.test.inv.InvariantTester.class,
      daikon.test.inv.unary.scalar.OneOfScalarTester.class,
      daikon.test.inv.unary.sequence.OneOfSequenceTester.class,
      daikon.test.LinearTernaryCoreTest.class,
      daikon.test.ModBitTrackerTest.class,
      daikon.test.ProglangTypeTest.class,
      daikon.test.VarComparabilityTest.class,
      daikon.test.VarInfoNameTest.class,
      daikon.test.inv.InvariantAddAndCheckTester.class,
      daikon.test.TestQuant.class
    };

    for (int i=0; i<classes.length; i++) {
      result.addTest(new TestSuite(classes[i]));
    }

    // This is possibly not right; the JIT needs to be disabled in order
    // for these tests to succeed.
    result.addTest(new TestSuite(TestUtilMDE.class));

    return result;
  }

}
