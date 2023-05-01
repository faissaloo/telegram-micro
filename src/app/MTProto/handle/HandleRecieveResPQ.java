package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.recieve.RecieveResPQ;
import mtproto.send.SendReqDhParams;
import mtproto.CombinatorIds;
import mtproto.TelegramPublicKeys;
import mtproto.PrimeDecomposer;
import mtproto.Response;
import mtproto.UnencryptedResponse;

import crypto.RSAPublicKey;

public class HandleRecieveResPQ extends MTProtoCallback {
  private String authHelpUrl = "";
  public HandleRecieveResPQ(MTProtoConnection connection, String authHelpUrl) {
    super(CombinatorIds.resPQ, connection);
    this.authHelpUrl = authHelpUrl;
  }
  
  public void execute(Response response) {
    UnencryptedResponse unencrypted_response = (UnencryptedResponse)response;
    System.out.println("RECIEVING RES PQ");
    RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(unencrypted_response);
    PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq, authHelpUrl);
    TelegramPublicKeys public_keys = new TelegramPublicKeys();
    RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
    if (public_key == null) {
      throw new SecurityException("Could not find public key for any of the fingerprints recieved");
    }
    connection.new_nonce = connection.random_number_generator.nextInteger256();
    
    SendReqDhParams diffie_hellman_params_request = new SendReqDhParams(
      connection.random_number_generator,
      connection.nonce,
      pq_data.server_nonce,
      pq_data.pq,
      decomposed_pq.lesser_prime,
      decomposed_pq.greater_prime,
      public_key,
      connection.new_nonce
    );
    diffie_hellman_params_request.send(connection);
    System.out.println("SENDING DH PARAMS");
  }
}
