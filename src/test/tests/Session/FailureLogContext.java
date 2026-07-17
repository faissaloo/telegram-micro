package test;

import session.FailureLog;
import session.FailureLogCodec;
import session.FailureLogStore;

public class FailureLogContext {
  private static FailureLog buildFailureLog() {
    FailureLog log = new FailureLog();

    log.push(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -303, 1);
    log.push(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_ERROR, -401, 214);
    return log;
  }

  public static class PushTracksLatestEntryTest extends Test {
    public String label() {
      return "Failure logs keep the latest bounded entry metadata";
    }

    public void test() throws TestFailureException {
      FailureLog log = buildFailureLog();
      FailureLog.Entry latest = log.latest();

      expect(log.count, 2);
      expect(latest != null, true);
      expect(latest.category, FailureLog.CATEGORY_READER);
      expect(latest.severity, FailureLog.SEVERITY_ERROR);
      expect(latest.code, -401);
      expect(latest.detail, 214);
    }
  }

  public static class RingRetainsNewestEntriesTest extends Test {
    public String label() {
      return "Failure logs retain the newest entries when the ring wraps";
    }

    public void test() throws TestFailureException {
      FailureLog log = new FailureLog();

      for (int i = 0; i < 12; ++i) {
        log.push(FailureLog.CATEGORY_NETWORK, FailureLog.SEVERITY_WARN, -100 - i, i);
      }

      expect(log.count, FailureLog.MAX_ENTRIES);
      expect(log.latest().code, -111);
      expect(log.newestAt(FailureLog.MAX_ENTRIES - 1).code, -104);
    }
  }

  public static class CodecRoundTripTest extends Test {
    public String label() {
      return "Failure logs round-trip through the fixed codec";
    }

    public void test() throws TestFailureException {
      FailureLog source = buildFailureLog();
      FailureLog decoded = new FailureLog();
      byte[] encoded = FailureLogCodec.encode(source);

      expect(encoded.length, FailureLogCodec.ENCODED_SIZE);
      expect(FailureLogCodec.decode(decoded, encoded), true);
      expect(decoded.count, 2);
      expect(decoded.latest().category, FailureLog.CATEGORY_READER);
      expect(decoded.latest().severity, FailureLog.SEVERITY_ERROR);
      expect(decoded.latest().code, -401);
      expect(decoded.latest().detail, 214);
    }
  }

  public static class RejectsInvalidCodecPayloadTest extends Test {
    public String label() {
      return "Failure logs reject invalid codec payloads";
    }

    public void test() throws TestFailureException {
      byte[] invalid = new byte[FailureLogCodec.ENCODED_SIZE];

      expect(FailureLogCodec.decode(new FailureLog(), invalid), false);
    }
  }

  public static class CategoryAndSeverityNamesTest extends Test {
    public String label() {
      return "Failure logs expose compact category and severity labels";
    }

    public void test() throws TestFailureException {
      expect(FailureLog.categoryName(FailureLog.CATEGORY_BOOTSTRAP), "BOOT");
      expect(FailureLog.categoryName(99), "NONE");
      expect(FailureLog.severityName(FailureLog.SEVERITY_WARN), "WARN");
      expect(FailureLog.severityName(99), "UNKNOWN");
    }
  }

  public static class FailureLogStoreRoundTripTest extends Test {
    public String label() {
      return "Failure logs round-trip through RMS";
    }

    public void test() throws TestFailureException {
      FailureLogStore store = new FailureLogStore("TelegramMicroFailureLogTest");
      FailureLog source = buildFailureLog();
      FailureLog loaded = new FailureLog();

      expect(store.clear(), true);
      expect(store.save(source), true);
      expect(store.load(loaded), true);
      expect(loaded.count, 2);
      expect(loaded.latest().code, -401);
      expect(store.clear(), true);
    }
  }
}
