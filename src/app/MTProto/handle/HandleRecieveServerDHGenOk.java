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

public class HandleRecieveServerDHGenOk extends MTProtoCallback {
  public HandleRecieveServerDHGenOk(MTProtoConnection connection) {
    super(CombinatorIds.dh_gen_ok, connection);
  }
  
  public void execute(Response response) {
    UnencryptedResponse unencrypted_response = (UnencryptedResponse)response;
    RecieveServerDHGenOk dh_gen_ok = RecieveServerDHGenOk.from_unencrypted_message(unencrypted_response);
    connection.server_salt = generate_server_salt(connection.new_nonce, dh_gen_ok.server_nonce);
    //https://github.com/badoualy/kotlogram/blob/master/mtproto/src/main/kotlin/com/github/badoualy/telegram/mtproto/auth/AuthKeyCreation.kt#L193
    connection.session_id = connection.random_number_generator.nextLong();
    System.out.println("DH GEN OK, sending ping...");
    //We're just piggybacking off this callback for now 
    (new SendPing(25565)).send(connection);
  }
  
  public static long generate_server_salt(Integer256 new_nonce, Integer128 server_nonce) {
    return Decode.Little.long_decode(ArrayPlus.subarray(Encode.Integer256_encode(new_nonce), 8), 0) ^
      Decode.Little.long_decode(ArrayPlus.subarray(Encode.Integer128_encode(server_nonce), 8), 0);
  }
}
