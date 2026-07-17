package test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import mtproto.CombinatorIds;
import mtproto.Deserialize;
import mtproto.EncryptedResponse;
import mtproto.ProtocolLimits;
import mtproto.RPCResponse;
import mtproto.TCPRequest;
import mtproto.TCPResponse;
import mtproto.auth.PasswordCheck;
import mtproto.handle.HandleRecieveServerDHGenOk;
import mtproto.recieve.RecieveMsgContainer;
import mtproto.recieve.RecieveServerDHGenOk;
import mtproto.recieve.help.RecieveConfig;
import mtproto.recieve.service.RecieveFutureSalts;
import mtproto.recieve.upload.RecieveUploadFile;
import support.BoundedByteArrayOutputStream;
import support.Encode;
import support.Integer128;
import support.Integer256;
import support.Queue;

public class ProtocolBoundaryContext {
  private static void putInt(byte[] out, int offset, int value) {
    byte[] encoded = Encode.int_encode(value);
    System.arraycopy(encoded, 0, out, offset, encoded.length);
  }

  public static class TcpFrameRejectsOversizedLengthTest extends Test {
    public String label() {
      return "TCP framing rejects an oversized length before body allocation";
    }

    public void test() throws TestFailureException {
      boolean rejected = false;
      try {
        TCPResponse.from_stream(new ByteArrayInputStream(
          Encode.int_encode(ProtocolLimits.MAX_WIRE_FRAME_BYTES + 1)
        ));
      } catch (IOException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class TcpFrameRequiresCompleteBodyTest extends Test {
    public String label() {
      return "TCP framing rejects truncated bodies";
    }

    public void test() throws TestFailureException {
      byte[] frame = new byte[] {8, 0, 0, 0, 1, 2, 3, 4};
      boolean rejected = false;
      try {
        TCPResponse.from_stream(new ByteArrayInputStream(frame));
      } catch (IOException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class TcpRequestRejectsOversizedBodyTest extends Test {
    public String label() {
      return "TCP requests reject oversized bodies before framing";
    }

    public void test() throws TestFailureException {
      boolean rejected = false;
      try {
        new TCPRequest(new byte[ProtocolLimits.MAX_WIRE_FRAME_BYTES + 1]);
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class QueueCapacityIsEnforcedTest extends Test {
    public String label() {
      return "Bounded queues reject accidental overfill";
    }

    public void test() throws TestFailureException {
      Queue queue = new Queue(1);
      boolean rejected = false;
      queue.enqueue(this);
      try {
        queue.enqueue(this);
      } catch (IllegalStateException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class PbkdfOutputBudgetIsEnforcedTest extends Test {
    public String label() {
      return "PBKDF2 rejects oversized output before allocation";
    }

    public void test() throws TestFailureException {
      boolean rejected = false;
      try {
        PasswordCheck.pbkdf2_hmac_sha512(
          new byte[32],
          new byte[16],
          1,
          PasswordCheck.MAX_PBKDF2_OUTPUT_BYTES + 1
        );
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class ExtendedTlBytesUseCorrectPaddingTest extends Test {
    public String label() {
      return "Extended TL bytes include their four-byte header in padding";
    }

    public void test() throws TestFailureException {
      byte[] encoded = new byte[260];
      encoded[0] = (byte)254;
      encoded[1] = (byte)254;
      for (int i = 0; i < 254; ++i) {
        encoded[4 + i] = (byte)i;
      }
      expect(Deserialize.bytes_length_deserialize(encoded, 0), 260);
      expect(Deserialize.bytes_deserialize(encoded, 0).length, 254);
    }
  }

  public static class TlBytesRejectBudgetBeforeAllocationTest extends Test {
    public String label() {
      return "TL bytes reject over-budget declared lengths before allocation";
    }

    public void test() throws TestFailureException {
      int length = ProtocolLimits.MAX_TL_BYTES + 1;
      byte[] encoded = new byte[] {
        (byte)254,
        (byte)length,
        (byte)(length >>> 8),
        (byte)(length >>> 16)
      };
      boolean rejected = false;
      try {
        Deserialize.bytes_deserialize(encoded, 0);
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class MessageContainerRejectsOversizedCountTest extends Test {
    public String label() {
      return "Message containers reject over-budget counts before allocation";
    }

    public void test() throws TestFailureException {
      byte[] data = Encode.int_encode(ProtocolLimits.MAX_CONTAINER_MESSAGES + 1);
      boolean rejected = false;
      try {
        RecieveMsgContainer.from_encrypted_message(
          new EncryptedResponse(0, 0, 1, 0, CombinatorIds.msg_container, data)
        );
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class BoundedInflationRejectsOverflowTest extends Test {
    public String label() {
      return "Bounded decompression output refuses to grow beyond its budget";
    }

    public void test() throws TestFailureException {
      BoundedByteArrayOutputStream out = new BoundedByteArrayOutputStream(4, 8);
      boolean rejected = false;
      try {
        out.write(new byte[9], 0, 9);
      } catch (IOException expected) {
        rejected = true;
      }
      expect(rejected, true);
      expect(out.size(), 0);
    }
  }

  public static class ConfigRejectsOversizedDcVectorTest extends Test {
    public String label() {
      return "Config parsing rejects over-budget DC vectors before allocation";
    }

    public void test() throws TestFailureException {
      byte[] body = new byte[28];
      putInt(body, 12, RecieveConfig.bool_false);
      putInt(body, 20, CombinatorIds.vector);
      putInt(body, 24, ProtocolLimits.MAX_DC_OPTIONS + 1);
      boolean rejected = false;
      try {
        RecieveConfig.from_rpc_response(new RPCResponse(CombinatorIds.config, body));
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class FutureSaltsRejectOversizedVectorTest extends Test {
    public String label() {
      return "Future-salt parsing rejects over-budget vectors before allocation";
    }

    public void test() throws TestFailureException {
      byte[] body = new byte[20];
      putInt(body, 12, CombinatorIds.vector);
      putInt(body, 16, ProtocolLimits.MAX_FUTURE_SALTS + 1);
      boolean rejected = false;
      try {
        RecieveFutureSalts.from_rpc_response(
          new RPCResponse(CombinatorIds.future_salts, body)
        );
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class UploadChunkRejectsOversizedBytesTest extends Test {
    public String label() {
      return "Upload chunks reject server data beyond the media budget";
    }

    public void test() throws TestFailureException {
      int length = ProtocolLimits.MAX_MEDIA_CHUNK_BYTES + 1;
      byte[] body = new byte[12 + length + 3];
      putInt(body, 0, 1);
      putInt(body, 4, 1);
      body[8] = (byte)254;
      body[9] = (byte)length;
      body[10] = (byte)(length >>> 8);
      body[11] = (byte)(length >>> 16);
      boolean rejected = false;
      try {
        RecieveUploadFile.from_rpc_response(new RPCResponse(CombinatorIds.upload_file, body));
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      expect(rejected, true);
    }
  }

  public static class DhCompletionPreservesAndChecksNonceHashTest extends Test {
    public String label() {
      return "DH completion preserves and verifies the server nonce hash";
    }

    public void test() throws TestFailureException {
      Integer128 nonce = new Integer128(1, 2);
      Integer128 serverNonce = new Integer128(3, 4);
      Integer256 newNonce = new Integer256(1, 2, 3, 4, 5, 6, 7, 8);
      byte[] authHash = new byte[20];
      for (int i = 0; i < authHash.length; ++i) {
        authHash[i] = (byte)(i + 9);
      }
      Integer128 expected = HandleRecieveServerDHGenOk.computeNewNonceHash(newNonce, authHash);
      RecieveServerDHGenOk parsed = new RecieveServerDHGenOk(nonce, serverNonce, expected);

      expect(parsed.new_nonce_hash1.equals(expected), true);
      expect(HandleRecieveServerDHGenOk.validNewNonceHash(newNonce, authHash, parsed.new_nonce_hash1), true);
      expected.representation_0 ^= 1;
      expect(HandleRecieveServerDHGenOk.validNewNonceHash(newNonce, authHash, expected), false);
    }
  }
}
