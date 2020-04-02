package mtproto;

import support.BigInteger;
import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;

public class RecieveResPQ {
  public static RecieveResPQ from_unencrypted_message(UnencryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data();
    int message_type = Decode.Little.int_decode(data, skip);
    skip += 4;

    if (message_type == 0x05162463) {
      BigInteger nonce = Decode.Little.biginteger_decode(data, skip);
      skip += 16;
      BigInteger server_nonce = Decode.Little.biginteger_decode(data, skip);
      skip += 16;
      long pq = Decode.Big.long_decode(Deserialize.bytes_deserialize(data, skip), 0);

      skip += Deserialize.bytes_length_deserialize(data, skip);
      long[] server_public_key_fingerprints = Deserialize.vector_long_deserialize(data, skip);
      return new RecieveResPQ(nonce, server_nonce, pq, server_public_key_fingerprints);
    } else {
      throw new TypeMismatchException("Expected a %(resPQ)");
    }
  }

  BigInteger nonce;
  BigInteger server_nonce;
  long pq;
  long[] server_public_key_fingerprints;

  //pq is not in fact a Utf8String, it's that bytes are encoded the same
  //pq is actually a big endian number...
  //resPQ#05162463 nonce:int128 server_nonce:int128 pq:string server_public_key_fingerprints:Vector long = resPQ;
  public RecieveResPQ(BigInteger nonce, BigInteger server_nonce, long pq, long[] server_public_key_fingerprints) {
    this.nonce = nonce;
    this.server_nonce = server_nonce;
    this.pq = pq;
    this.server_public_key_fingerprints = server_public_key_fingerprints;
  }

  public BigInteger nonce() {
    return nonce;
  }

  public BigInteger server_nonce() {
    return server_nonce;
  }

  public long pq() {
    return pq;
  }

  public long[] server_public_key_fingerprints() {
    return server_public_key_fingerprints;
  }
}
