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
}
