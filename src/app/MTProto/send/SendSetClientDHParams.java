package mtproto.send;
import bouncycastle.BigInteger;
import support.ByteArrayPlus;
import support.Integer128;
import crypto.SHA1;
import crypto.AES256IGE;
import crypto.SecureRandomPlus;
import mtproto.CombinatorIds;
import mtproto.Serializer;
import support.ServerConnection;

public class SendSetClientDHParams extends SendUnencrypted {
    private BigInteger getGroupGeneratorPowerBFromServer(BigInteger base, int exponent, BigInteger modulus, String authHelpUrl) {
        System.out.println(base.toString());
        ServerConnection srvCon = new ServerConnection(authHelpUrl);
        BigInteger result = srvCon.getModPow(base.toString(), exponent, modulus.toString());
        return result;
    }
    
  public SendSetClientDHParams(SecureRandomPlus random_number_generator, Integer128 nonce, Integer128 server_nonce, long retry_id, int group_generator, BigInteger diffie_hellman_prime, BigInteger b, byte[] tmp_aes_key, byte[] tmp_aes_iv, String authHelpUrl) {
    BigInteger modpowremote = getGroupGeneratorPowerBFromServer(b, group_generator, diffie_hellman_prime, authHelpUrl);
    if (modpowremote.intValue() == -1) {
        System.out.println("Could not calculate mod pow on server. Will calculate on device, takes a while");
    } else { System.out.println("Received modpow result " + modpowremote); }
    BigInteger group_generator_power_b = modpowremote.intValue() != -1 ? modpowremote : BigInteger.valueOf(group_generator).modPow(b, diffie_hellman_prime);
    System.out.println("Base: " + b.toString());
    System.out.println("Exponent: " + group_generator);
    System.out.println("Modulus: " + diffie_hellman_prime.toString());
    System.out.println("Modpow result: " + group_generator_power_b.toString());
    byte[] inner_data = inner_data(nonce, server_nonce, retry_id, group_generator_power_b);
    byte[] inner_data_hash = (new SHA1()).digest(inner_data);
    
    byte[] data_with_hash = data_with_hash(inner_data_hash, inner_data, random_number_generator);
    byte[] encrypted_data = AES256IGE.encrypt(tmp_aes_key, tmp_aes_iv, data_with_hash);
    
    message_data
      .append_int(CombinatorIds.set_client_DH_params)
      .append_Integer128(nonce)
      .append_Integer128(server_nonce)
      .append_byte_string(encrypted_data);
  }
  
  public static byte[] inner_data(Integer128 nonce, Integer128 server_nonce, long retry_id, BigInteger group_generator_power_b) {
    return (new Serializer())
      .append_int(CombinatorIds.client_DH_inner_data)
      .append_Integer128(nonce)
      .append_Integer128(server_nonce)
      .append_long(retry_id)
      .append_BigInteger(group_generator_power_b)
      .end();
  }
  
  public static byte[] data_with_hash(byte[] inner_data_hash, byte[] inner_data, SecureRandomPlus random_number_generator) {
    return (new Serializer())
      .append_raw_bytes(inner_data_hash)
      .append_raw_bytes(inner_data)
      .pad_to_alignment(16, random_number_generator)
      .end();
  }
}

