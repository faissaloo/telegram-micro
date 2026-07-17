package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.recieve.RecieveServerDHGenOk;
import mtproto.send.service.SendPing;
import mtproto.CombinatorIds;
import mtproto.UnencryptedResponse;
import mtproto.Response;

import support.Integer256;
import support.Integer128;
import support.Encode;
import support.Decode;
import support.ArrayPlus;
import support.ByteArrayPlus;
import crypto.SHA1;

public class HandleRecieveServerDHGenOk extends MTProtoCallback {
  public HandleRecieveServerDHGenOk(MTProtoConnection connection) {
    super(CombinatorIds.dh_gen_ok, connection);
  }
  
  public void execute(Response response) {
    UnencryptedResponse unencrypted_response = (UnencryptedResponse)response;
    RecieveServerDHGenOk dh_gen_ok = RecieveServerDHGenOk.from_unencrypted_message(unencrypted_response);
    if (connection.nonce == null || !connection.nonce.equals(dh_gen_ok.nonce) ||
        connection.server_nonce == null || !connection.server_nonce.equals(dh_gen_ok.server_nonce)) {
      throw new SecurityException("dh_gen_ok nonce mismatch.");
    }
    if (!validNewNonceHash(
        connection.new_nonce,
        connection.auth_key_full_hash,
        dh_gen_ok.new_nonce_hash1)) {
      throw new SecurityException("dh_gen_ok new nonce hash mismatch.");
    }
    connection.server_salt = generate_server_salt(connection.new_nonce, dh_gen_ok.server_nonce);
    //https://github.com/badoualy/kotlogram/blob/master/mtproto/src/main/kotlin/com/github/badoualy/telegram/mtproto/auth/AuthKeyCreation.kt#L193
    do {
      connection.session_id = connection.random_number_generator.nextLong();
    } while (connection.session_id == 0);
    System.out.println("DH GEN OK, sending ping...");
    //We're just piggybacking off this callback for now 
    (new SendPing(25565)).send(connection);
  }
  
  public static long generate_server_salt(Integer256 new_nonce, Integer128 server_nonce) {
    return Decode.Little.long_decode(ArrayPlus.subarray(Encode.Integer256_encode(new_nonce), 8), 0) ^
      Decode.Little.long_decode(ArrayPlus.subarray(Encode.Integer128_encode(server_nonce), 8), 0);
  }

  public static boolean validNewNonceHash(
    Integer256 newNonce,
    byte[] authKeyFullHash,
    Integer128 actual
  ) {
    if (newNonce == null || actual == null ||
        authKeyFullHash == null || authKeyFullHash.length < 8) {
      return false;
    }

    return computeNewNonceHash(newNonce, authKeyFullHash).equals(actual);
  }

  public static Integer128 computeNewNonceHash(Integer256 newNonce, byte[] authKeyFullHash) {
    if (newNonce == null || authKeyFullHash == null || authKeyFullHash.length < 8) {
      throw new IllegalArgumentException("Nonce and auth key hash are required.");
    }
    byte[] material = (new ByteArrayPlus())
      .append_raw_bytes(Encode.Integer256_encode(newNonce))
      .append_byte((byte)1)
      .append_raw_bytes_up_to(authKeyFullHash, 8)
      .toByteArray();
    byte[] digest = (new SHA1()).digest(material);
    return Decode.Little.Integer128_decode(digest, 4);
  }
}
