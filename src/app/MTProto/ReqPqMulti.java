package mtproto;
import java.util.Random;
import support.ByteArrayPlus;
import support.Integer128;
import support.RandomPlus;
import java.io.IOException;
import java.lang.Math;
import java.lang.System;

import java.lang.Integer;
//https://stackoverflow.com/questions/30661644/how-to-implement-authorization-using-a-telegram-api/32809138#32809138
public class ReqPqMulti {
  final static RandomPlus random_number_generator = new RandomPlus(); //Not sure if this is enough, might need cryptographically secure RNG going forward
  ByteArrayPlus message_data;

  public ReqPqMulti() {
    // req_pq_multi#be7e8ef1 nonce:int128 = ResPQ;
    // We'll have to stick to a shorter nonce then pad out the upper range cus we aren't in a position to be handling 128-bit integers on the regular
    message_data = new ByteArrayPlus();
    message_data.append_int(0xbe7e8ef1); //combinator_id
    message_data.append_Integer128(random_number_generator.nextInteger128()); //nonce
  }

  public void send() {
    (new UnencryptedRequest(message_id(), message_data.toByteArray())).send();
  }

  private long message_id() {
    //https://core.telegram.org/mtproto/description#message-identifier-msg-id
    return (System.currentTimeMillis()/1000L)*4294967296L;
  }
}
