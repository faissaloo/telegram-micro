package test;

import mtproto.EncryptedRequest;
import bouncycastle.BigInteger;
import support.Integer256;
import support.Integer128;
import support.RandomPlus;

public class EncryptedRequestContext {
  public static class RandomStub extends RandomPlus {
    public RandomStub() {
      super();
    }
    
    protected int next(int bits) {    
      return 1;
    }
  }
  
  public static class GeneratePaddedEncryptedDataTest extends Test {
    public String label() {
      return "Data is padded correctly";
    }
    
    public void test() {
      RandomStub random_stub = new RandomStub();
      byte[] result = EncryptedRequest.pad_unencrypted_data(new byte[] {(byte)0x69}, random_stub);
      expect(result, new byte[] {
        (byte)0x69, (byte)0x01, (byte)0x01, (byte)0x01,
        (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
        (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
        (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01
      });
    }
  }
}
