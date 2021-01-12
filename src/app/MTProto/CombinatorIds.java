package mtproto;

public class CombinatorIds {
  public static int unknown = 0; //pseudo-combinator
  
  public static int p_q_inner_data = 0x83c95aec;
  public static int req_DH_params = 0xd712e4be;
  public static int resPQ = 0x05162463;
  public static int req_pq_multi = 0xbe7e8ef1;
  public static int ping = 0x7abe77ec;
  public static int pong = 0x347773c5;
  public static int server_DH_params_ok = 0xd0e8075c;
  public static int server_DH_inner_data = 0xb5890dba;
  public static int set_client_DH_params = 0xf5045f1f;
  public static int client_DH_inner_data = 0x6643b654;
  
  public static int dh_gen_ok = 0x3bcbf734;
  public static int dh_gen_retry = 0x46dc1fb9;
  public static int dh_gen_fail = 0xa69dae02;
  
  public static int msg_container = 0x73f1f8dc;
  public static int vector = 0x1cb5c415;
  public static int new_session_created = 0x9ec20908;
  public static int bad_msg_notification = 0xa7eff811;
  
  public static int rpc_result = 0xf35c6d01;
  public static int rpc_error = 0x2144ca19;
  
  public static int msgs_ack = 0x62d6b459;
}
