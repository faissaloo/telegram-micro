package mtproto.recieve.service;

import mtproto.CombinatorIds;
import mtproto.Deserialize;
import mtproto.RPCResponse;
import mtproto.TypeMismatchException;
import mtproto.ProtocolLimits;

import support.Decode;

public class RecieveFutureSalts {
  public static class FutureSalt {
    public int valid_since;
    public int valid_until;
    public long salt;

    public FutureSalt(int valid_since, int valid_until, long salt) {
      this.valid_since = valid_since;
      this.valid_until = valid_until;
      this.salt = salt;
    }
  }

  public static RecieveFutureSalts from_rpc_response(RPCResponse response) throws TypeMismatchException {
    int skip = 0;
    byte[] data = response.body;

    if (response.type != CombinatorIds.future_salts) {
      throw new TypeMismatchException("Expected a %(FutureSalts)");
    }
    require(data, skip, 20);

    long req_msg_id = Decode.Little.long_decode(data, skip);
    skip += 8;
    int now = Decode.Little.int_decode(data, skip);
    skip += 4;

    int vector_type = Decode.Little.int_decode(data, skip);
    skip += 4;
    if (vector_type != CombinatorIds.vector) {
      throw new TypeMismatchException("Expected a %(Vector FutureSalt)");
    }

    int salt_count = Decode.Little.int_decode(data, skip);
    skip += 4;
    ProtocolLimits.requireLength(
      salt_count,
      ProtocolLimits.MAX_FUTURE_SALTS,
      "Future salt vector"
    );
    if (salt_count > (data.length - skip) / 20) {
      throw new IllegalArgumentException("Future salt count exceeds its payload.");
    }
    FutureSalt[] salts = new FutureSalt[salt_count];
    for (int i = 0; i < salt_count; ++i) {
      require(data, skip, 20);
      int salt_type = Decode.Little.int_decode(data, skip);
      skip += 4;
      if (salt_type != CombinatorIds.future_salt) {
        throw new TypeMismatchException("Expected a %(future_salt)");
      }

      salts[i] = new FutureSalt(
        Decode.Little.int_decode(data, skip),
        Decode.Little.int_decode(data, skip + 4),
        Decode.Little.long_decode(data, skip + 8)
      );
      skip += 16;
    }

    return new RecieveFutureSalts(req_msg_id, now, salts);
  }

  private static void require(byte[] data, int offset, int amount) {
    if (data == null || offset < 0 || amount < 0 || offset > data.length - amount) {
      throw new IllegalArgumentException("Future salts payload is truncated.");
    }
  }

  public long req_msg_id;
  public int now;
  public FutureSalt[] salts;

  public RecieveFutureSalts(long req_msg_id, int now, FutureSalt[] salts) {
    this.req_msg_id = req_msg_id;
    this.now = now;
    this.salts = salts;
  }
}
