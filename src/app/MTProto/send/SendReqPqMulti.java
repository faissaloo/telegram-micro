package mtproto.send;
import java.util.Random;
import java.io.IOException;
import java.lang.Math;
import java.lang.System;

import support.ByteArrayPlus;
import support.Integer128;
import support.RandomPlus;

import mtproto.CombinatorIds;
import mtproto.UnencryptedRequest;

import java.lang.Integer;
//https://stackoverflow.com/questions/30661644/how-to-implement-authorization-using-a-telegram-api/32809138#32809138
public class SendReqPqMulti {
  ByteArrayPlus message_data;

  public SendReqPqMulti(Integer128 nonce) {
    message_data = new ByteArrayPlus();
    message_data.append_int(CombinatorIds.req_pq_multi);
    message_data.append_Integer128(nonce); //nonce
  }

  public void send() {
    (new UnencryptedRequest(message_data.toByteArray())).send();
  }
}
