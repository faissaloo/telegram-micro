package test;

import crypto.EntropyPool;
import crypto.SecureRandomPlus;
import session.EntropySeedCodec;

public class EntropyContext {
  private static byte[] seed(int offset) {
    byte[] value = new byte[SecureRandomPlus.SEED_BYTES];
    for (int i = 0; i < value.length; ++i) {
      value[i] = (byte)(i + offset);
    }
    return value;
  }

  public static class SecureGeneratorIsDeterministicForTestsTest extends Test {
    public String label() {
      return "Secure generator is deterministic only when explicitly given the same seed";
    }

    public void test() throws TestFailureException {
      SecureRandomPlus first = new SecureRandomPlus(seed(1));
      SecureRandomPlus second = new SecureRandomPlus(seed(1));
      SecureRandomPlus different = new SecureRandomPlus(seed(2));
      byte[] firstOutput = first.nextBytes(96);
      byte[] secondOutput = second.nextBytes(96);
      byte[] differentOutput = different.nextBytes(96);
      boolean differs = false;

      expect(firstOutput, secondOutput);
      for (int i = 0; i < firstOutput.length; ++i) {
        if (firstOutput[i] != differentOutput[i]) {
          differs = true;
          break;
        }
      }
      expect(differs, true);
    }
  }

  public static class SecureGeneratorRejectsMissingSeedTest extends Test {
    public String label() {
      return "Secure generator refuses missing or undersized entropy";
    }

    public void test() throws TestFailureException {
      boolean nullRejected = false;
      boolean shortRejected = false;
      try {
        new SecureRandomPlus(null);
      } catch (IllegalArgumentException expected) {
        nullRejected = true;
      }
      try {
        new SecureRandomPlus(new byte[31]);
      } catch (IllegalArgumentException expected) {
        shortRejected = true;
      }
      expect(nullRejected, true);
      expect(shortRejected, true);
    }
  }

  public static class PoolRequiresTimingThresholdTest extends Test {
    public String label() {
      return "Entropy pool cannot be consumed before the timing threshold";
    }

    public void test() throws TestFailureException {
      EntropyPool pool = new EntropyPool(seed(4));
      boolean rejected = false;

      expect(pool.recordEvent(1000, 1, 1, 5000), true);
      expect(pool.recordEvent(1000, 2, 2, 5000), false);
      try {
        pool.extractSeed();
      } catch (IllegalStateException expected) {
        rejected = true;
      }
      expect(rejected, true);
      expect(pool.eventCount(), 1);
    }
  }

  public static class PoolMixesBoundedSamplesTest extends Test {
    public String label() {
      return "Entropy pool mixes exactly the required bounded sample count";
    }

    public void test() throws TestFailureException {
      EntropyPool first = new EntropyPool(seed(7));
      EntropyPool second = new EntropyPool(seed(7));

      for (int i = 0; i < EntropyPool.REQUIRED_EVENTS; ++i) {
        long timestamp = 10000L + (i * 17L) + ((i * i) % 13);
        expect(first.recordEvent(timestamp, i % 12, (i & 1) + 1, 64000L - i), true);
        expect(second.recordEvent(timestamp, i % 12, (i & 1) + 1, 64000L - i), true);
      }
      expect(first.ready(), true);
      expect(first.recordEvent(999999, 3, 1, 1), false);
      expect(first.extractSeed(), second.extractSeed());
    }
  }

  public static class SeedRecordDetectsCorruptionTest extends Test {
    public String label() {
      return "Persisted entropy seed records reject corruption";
    }

    public void test() throws TestFailureException {
      byte[] source = seed(9);
      byte[] decoded = new byte[EntropySeedCodec.SEED_BYTES];
      byte[] encoded = EntropySeedCodec.encode(source);

      expect(encoded.length, EntropySeedCodec.ENCODED_SIZE);
      expect(EntropySeedCodec.decode(decoded, encoded), true);
      expect(decoded, source);
      encoded[11] ^= 0x40;
      expect(EntropySeedCodec.decode(decoded, encoded), false);
    }
  }
}
