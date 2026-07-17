package test;

import session.ResumeState;
import session.KnownDcHost;
import session.SessionSnapshot;
import session.SessionSnapshotCodec;
import session.SessionStore;
import support.Encode;

public class SessionContext {
  private static SessionSnapshot buildSnapshot() {
    SessionSnapshot snapshot = new SessionSnapshot();
    byte[] authKey = new byte[SessionSnapshot.AUTH_KEY_BYTES];

    for (int i = 0; i < authKey.length; ++i) {
      authKey[i] = (byte)(i + 1);
    }

    snapshot.setAuthKey(authKey);
    snapshot.authorized = true;
    snapshot.dcId = 3;
    snapshot.authKeyId = 0x1122334455667788L;
    snapshot.serverSalt = 0x0102030405060708L;
    snapshot.sessionId = 0x1020304050607080L;
    snapshot.seqNo = 7;
    snapshot.timeOffsetSeconds = 17;
    snapshot.lastMessageId = 0x1122334455667784L;
    return snapshot;
  }

  private static byte[] encodeLegacySnapshot(SessionSnapshot snapshot) {
    byte[] out = new byte[SessionSnapshotCodec.LEGACY_ENCODED_SIZE];
    int flags = SessionSnapshotCodec.FLAG_HAS_AUTH_STATE | SessionSnapshotCodec.FLAG_AUTHORIZED;
    int offset = 0;

    System.arraycopy(Encode.int_encode(SessionSnapshotCodec.MAGIC), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(SessionSnapshot.LEGACY_VERSION), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(flags), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(snapshot.dcId), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.long_encode(snapshot.authKeyId), 0, out, offset, 8);
    offset += 8;
    System.arraycopy(Encode.long_encode(snapshot.serverSalt), 0, out, offset, 8);
    offset += 8;
    System.arraycopy(Encode.long_encode(snapshot.sessionId), 0, out, offset, 8);
    offset += 8;
    System.arraycopy(Encode.int_encode(snapshot.seqNo), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(snapshot.authKey, 0, out, offset, SessionSnapshot.AUTH_KEY_BYTES);
    return out;
  }

  public static class SnapshotRoundTripTest extends Test {
    public String label() {
      return "Session snapshots round-trip through the fixed codec";
    }

    public void test() throws TestFailureException {
      SessionSnapshot source = buildSnapshot();
      SessionSnapshot decoded = new SessionSnapshot();
      byte[] encoded = SessionSnapshotCodec.encode(source);

      expect(encoded.length, SessionSnapshotCodec.ENCODED_SIZE);
      expect(SessionSnapshotCodec.decode(decoded, encoded), true);
      expect(decoded.hasUsableAuth(), true);
      expect(decoded.authorized, true);
      expect(decoded.dcId, 3);
      expect(decoded.authKeyId, 0x1122334455667788L);
      expect(decoded.serverSalt, 0x0102030405060708L);
      expect(decoded.sessionId, 0x1020304050607080L);
      expect(decoded.seqNo, 7);
      expect(decoded.timeOffsetSeconds, 17);
      expect(decoded.lastMessageId, 0x1122334455667784L);
      expect(decoded.authKey, source.authKey);
    }
  }

  public static class LegacySnapshotCompatibilityTest extends Test {
    public String label() {
      return "Legacy session snapshots still decode with zeroed transport clock";
    }

    public void test() throws TestFailureException {
      SessionSnapshot decoded = new SessionSnapshot();
      byte[] legacy = encodeLegacySnapshot(buildSnapshot());

      expect(SessionSnapshotCodec.decode(decoded, legacy), true);
      expect(decoded.version, SessionSnapshot.LEGACY_VERSION);
      expect(decoded.timeOffsetSeconds, 0);
      expect(decoded.lastMessageId, 0L);
      expect(decoded.hasUsableAuth(), true);
    }
  }

  public static class RejectsTruncatedSnapshotTest extends Test {
    public String label() {
      return "Session snapshots reject truncated payloads";
    }

    public void test() throws TestFailureException {
      SessionSnapshot snapshot = buildSnapshot();
      byte[] encoded = SessionSnapshotCodec.encode(snapshot);
      byte[] truncated = new byte[encoded.length - 1];

      System.arraycopy(encoded, 0, truncated, 0, truncated.length);
      expect(SessionSnapshotCodec.decode(new SessionSnapshot(), truncated), false);
    }
  }

  public static class ShortAuthKeyNormalizesToFixedWidthTest extends Test {
    public String label() {
      return "Session snapshots normalize shorter auth keys to fixed width";
    }

    public void test() throws TestFailureException {
      SessionSnapshot snapshot = new SessionSnapshot();
      byte[] shortKey = new byte[251];
      byte[] expected = new byte[SessionSnapshot.AUTH_KEY_BYTES];

      for (int i = 0; i < shortKey.length; ++i) {
        shortKey[i] = (byte)(0x80 + i);
      }

      snapshot.setAuthKey(shortKey);
      snapshot.authKeyId = 9;

      System.arraycopy(shortKey, 0, expected, expected.length - shortKey.length, shortKey.length);
      expect(snapshot.hasUsableAuth(), true);
      expect(snapshot.authKey, expected);
    }
  }

  public static class ResumeStateLoadsFromSnapshotTest extends Test {
    public String label() {
      return "Resume state reflects saved auth and transport baselines";
    }

    public void test() throws TestFailureException {
      SessionSnapshot snapshot = buildSnapshot();
      ResumeState state = new ResumeState();

      expect(state.load(snapshot), true);
      expect(state.hasAuth, true);
      expect(state.hasTransportState, true);
      expect(state.authorized, true);
      expect(state.dcId, 3);
      expect(state.authKeyId, 0x1122334455667788L);
      expect(state.sessionId, 0x1020304050607080L);
      expect(state.timeOffsetSeconds, 17);
      expect(state.phase, ResumeState.PHASE_READY);
      expect(state.phaseName(), "READY");
    }
  }

  public static class DurableRestoreClearsTransportReplayStateTest extends Test {
    public String label() {
      return "Durable session snapshots can drop stale transport replay state";
    }

    public void test() throws TestFailureException {
      SessionSnapshot snapshot = buildSnapshot();

      expect(snapshot.hasTransportReplayState(), true);
      snapshot.clearTransportReplayState();
      expect(snapshot.sessionId, 0L);
      expect(snapshot.seqNo, 0);
      expect(snapshot.lastMessageId, 0L);
      expect(snapshot.serverSalt, 0x0102030405060708L);
      expect(snapshot.timeOffsetSeconds, 17);
      expect(snapshot.shouldResumeSession(), true);
      expect(snapshot.hasTransportReplayState(), false);
    }
  }

  public static class ResumeStateLoadsAuthOnlySnapshotTest extends Test {
    public String label() {
      return "Resume state keeps auth-ready restores separate from transport replay";
    }

    public void test() throws TestFailureException {
      SessionSnapshot snapshot = buildSnapshot();
      ResumeState state = new ResumeState();

      snapshot.clearTransportReplayState();
      expect(state.load(snapshot), true);
      expect(state.hasAuth, true);
      expect(state.hasTransportState, false);
      expect(state.authorized, true);
      expect(state.phase, ResumeState.PHASE_RECONNECT);
      expect(state.phaseName(), "RECONNECT");
      expect(state.timeOffsetSeconds, 17);
    }
  }

  public static class UnauthorizedSnapshotsAreNotResumeEligibleTest extends Test {
    public String label() {
      return "Unauthorized snapshots do not qualify for session resume";
    }

    public void test() throws TestFailureException {
      SessionSnapshot snapshot = buildSnapshot();

      snapshot.authorized = false;
      expect(snapshot.hasUsableAuth(), true);
      expect(snapshot.shouldResumeSession(), false);
    }
  }

  public static class ResumeStateTracksFailuresTest extends Test {
    public String label() {
      return "Resume state records reconnect and failure transitions";
    }

    public void test() throws TestFailureException {
      ResumeState state = new ResumeState();

      state.noteDisconnect(401);
      expect(state.phase, ResumeState.PHASE_RECONNECT);
      expect(state.reconnectAttempts, 1);
      expect(state.lastErrorCode, 401);

      state.noteFailure(500);
      expect(state.phase, ResumeState.PHASE_FAILED);
      expect(state.reconnectAttempts, 2);
      expect(state.lastErrorCode, 500);

      state.noteSuccess();
      expect(state.phase, ResumeState.PHASE_READY);
      expect(state.reconnectAttempts, 0);
      expect(state.lastErrorCode, 0);
    }
  }

  public static class KnownDcHostMapsProductionDcIdsTest extends Test {
    public String label() {
      return "Known production DC ids map to the expected hosts";
    }

    public void test() throws TestFailureException {
      expect(KnownDcHost.production(1), "149.154.175.53");
      expect(KnownDcHost.production(2), "149.154.167.50");
      expect(KnownDcHost.production(3), "149.154.175.100");
      expect(KnownDcHost.production(4), "149.154.167.91");
      expect(KnownDcHost.production(5), "91.108.56.130");
      expect(KnownDcHost.production(0) == null, true);
      expect(KnownDcHost.production(9) == null, true);
    }
  }

  public static class SessionStoreRoundTripTest extends SkippedTest {
    public String label() {
      return "Session stores round-trip snapshots through RMS";
    }

    public void test() throws TestFailureException {
      SessionStore store = new SessionStore("TelegramMicroSessionTest");
      SessionSnapshot source = buildSnapshot();
      SessionSnapshot loaded = new SessionSnapshot();

      expect(store.clear(), true);
      expect(store.save(source), true);
      expect(store.load(loaded), true);
      expect(loaded.authorized, true);
      expect(loaded.dcId, 3);
      expect(loaded.authKeyId, source.authKeyId);
      expect(loaded.serverSalt, source.serverSalt);
      expect(loaded.seqNo, source.seqNo);
      expect(loaded.authKey, source.authKey);
      expect(store.clear(), true);
    }
  }
}
