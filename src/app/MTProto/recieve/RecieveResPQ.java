package mtproto.recieve;

import support.Integer128;
import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;

import mtproto.CombinatorIds;
import mtproto.UnencryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.Deserialize;

public class RecieveResPQ {
  public static RecieveResPQ from_unencrypted_message(UnencryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data();
    int message_type = message.type();

    if (message_type == CombinatorIds.resPQ) {
      Integer128 nonce = Decode.Little.Integer128_decode(data, skip);
      skip += 16;
      Integer128 server_nonce = Decode.Little.Integer128_decode(data, skip);
      skip += 16;
      long pq = Decode.Big.long_decode(Deserialize.bytes_deserialize(data, skip), 0); //this probably deserializes some stuff wrong on rare occasions

      System.out.println("BYTES LENGTH");
      System.out.println(Deserialize.bytes_length_deserialize(data, skip));
      skip += Deserialize.bytes_length_deserialize(data, skip); //this breaks without including the offset in the calculation
      
      long[] server_public_key_fingerprints = Deserialize.vector_long_deserialize(data, skip);
      return new RecieveResPQ(nonce, server_nonce, pq, server_public_key_fingerprints);
    } else {
      throw new TypeMismatchException("Expected a %(resPQ)");
    }
  }

  public Integer128 nonce;
  public Integer128 server_nonce;
  public long pq;
  public long[] server_public_key_fingerprints;

  //pq is not in fact a Utf8String, it's that bytes are encoded the same
  //pq is actually a big endian number...
  //resPQ#05162463 nonce:int128 server_nonce:int128 pq:string server_public_key_fingerprints:Vector long = resPQ;
  public RecieveResPQ(Integer128 nonce, Integer128 server_nonce, long pq, long[] server_public_key_fingerprints) {
    this.nonce = nonce;
    this.server_nonce = server_nonce;
    this.pq = pq;
    this.server_public_key_fingerprints = server_public_key_fingerprints;
  }
}
