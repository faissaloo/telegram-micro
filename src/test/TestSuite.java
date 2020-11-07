package test;

public class TestSuite {
  public static void register_all_tests() {
    TestRunner.register(new RecieveServerDHParamsOkContext.DeserializeTest());
    TestRunner.register(new MTProtoConnectionContext.GenerateAuthKeyTest());
    TestRunner.register(new SendReqDhParamsContext.PQInnerDataTest());
    
    TestRunner.register(new ArrayPlusContext.RemoveLeadingZeroesTest());
    TestRunner.register(new ArrayPlusContext.XorTest());
    TestRunner.register(new ArrayPlusContext.SubarrayTest());

    TestRunner.register(new SerializerContext.AppendByteStringTest());

    TestRunner.register(new RSAPublicKeyContext.RSAPublicKeyFingerprintTest());
    TestRunner.register(new RSAContext.RSAEncryptionPrimitiveTest());

    TestRunner.register(new SHA256Context.DigestTest());
    TestRunner.register(new SHA1Context.DigestTest());
    TestRunner.register(new SHA1Context.ShortenedDigestTest());

    TestRunner.register(new AES256IGEContext.SubBytesTest());
    TestRunner.register(new AES256IGEContext.MixColumnsTest());
    TestRunner.register(new AES256IGEContext.ShiftRowsTest());
    TestRunner.register(new AES256IGEContext.KeyScheduleTest());
    TestRunner.register(new AES256IGEContext.GaloisField2PowerTest());
    TestRunner.register(new AES256IGEContext.RotateLeftTest());
    TestRunner.register(new AES256IGEContext.AddRoundKeyTest());
    TestRunner.register(new AES256IGEContext.EncryptBlockTest());
    TestRunner.register(new AES256IGEContext.DecryptBlockTest());
    TestRunner.register(new AES256IGEContext.UnshiftRowsTest());
    TestRunner.register(new AES256IGEContext.UnmixColumnsTest());
    TestRunner.register(new AES256IGEContext.UnsubstituteBytesTest());
    TestRunner.register(new AES256IGEContext.IGEEncryptTest());
    TestRunner.register(new AES256IGEContext.IGEDecryptTest());

    TestRunner.register(new Integer128Context.HexTest());
    TestRunner.register(new Integer128Context.EqualTest());
    TestRunner.register(new Integer128Context.UnsignedGreaterThanTest());
    TestRunner.register(new Integer128Context.UnsignedLessThanTest());
    TestRunner.register(new Integer128Context.UnsignedGreaterThanEqualTest());
    TestRunner.register(new Integer128Context.UnsignedLessThanEqualTest());
    TestRunner.register(new Integer128Context.NotTest());
    TestRunner.register(new Integer128Context.AddTest());
    TestRunner.register(new Integer128Context.NegateTest());
    TestRunner.register(new Integer128Context.SubtractTest());
    TestRunner.register(new Integer128Context.UnsignedDivideModuloTest());
    TestRunner.register(new Integer128Context.UnsignedLeftShiftTest());
    TestRunner.register(new Integer128Context.UnsignedRightShiftTest());
    TestRunner.register(new Integer128Context.UnsignedLongMultiplyTest());
    TestRunner.register(new Integer128Context.AndTest());
    TestRunner.register(new Integer128Context.OrTest());
    TestRunner.register(new Integer128Context.ToLongTest());

    TestRunner.register(new IntegerPlusContext.RotateRightTest());

    TestRunner.register(new PrimeDecomposerContext.FiniteRingTest());
    TestRunner.register(new PrimeDecomposerContext.EuclidianGreatestCommonDenominatorTest());
    TestRunner.register(new PrimeDecomposerContext.DecomposeTest()); //Warning: SLOW 20 sec test
  }
}
