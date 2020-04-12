package test;

public class TestSuite {
  public static void register_all_tests() {
    TestRunner.register(new AES256IGEContext.SubBytesTest());
    TestRunner.register(new AES256IGEContext.MixColumnsTest());
    TestRunner.register(new AES256IGEContext.ShiftRowsTest());
    TestRunner.register(new AES256IGEContext.KeyScheduleTest());

    TestRunner.register(new BigIntegerContext.HexTest());
    TestRunner.register(new BigIntegerContext.EqualTest());
    TestRunner.register(new BigIntegerContext.UnsignedGreaterThanTest());
    TestRunner.register(new BigIntegerContext.UnsignedLessThanTest());
    TestRunner.register(new BigIntegerContext.UnsignedGreaterThanEqualTest());
    TestRunner.register(new BigIntegerContext.UnsignedLessThanEqualTest());
    TestRunner.register(new BigIntegerContext.NotTest());
    TestRunner.register(new BigIntegerContext.AddTest());
    TestRunner.register(new BigIntegerContext.NegateTest());
    TestRunner.register(new BigIntegerContext.SubtractTest());
    TestRunner.register(new BigIntegerContext.UnsignedDivideModuloTest());
    TestRunner.register(new BigIntegerContext.UnsignedLeftShiftTest());
    TestRunner.register(new BigIntegerContext.UnsignedRightShiftTest());
    TestRunner.register(new BigIntegerContext.UnsignedLongMultiplyTest());
    TestRunner.register(new BigIntegerContext.AndTest());
    TestRunner.register(new BigIntegerContext.OrTest());
    TestRunner.register(new BigIntegerContext.ToLongTest());

    TestRunner.register(new IntegerPlusContext.PowerTest());

    TestRunner.register(new LongPlusContext.PowerTest());

    TestRunner.register(new PrimeDecomposerContext.FiniteRingTest());
    TestRunner.register(new PrimeDecomposerContext.EuclidianGreatestCommonDenominatorTest());
    //TestRunner.register(new PrimeDecomposerContext.DecomposeTest()); //Warning: SLOW, ~4 mins
  }
}
