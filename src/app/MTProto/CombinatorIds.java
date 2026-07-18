package mtproto;

public class CombinatorIds {
  //base layer and service messages
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
  public static int bad_server_salt = 0xedab447b;
  public static int future_salt = 0x0949d9dc;
  public static int future_salts = 0xae500895;
  public static int get_future_salts = 0xb921bd04;
  
  public static int rpc_result = 0xf35c6d01;
  //https://docs.pyrogram.org/api/errors
  public static int rpc_error = 0x2144ca19;
  public static int bool_true = 0x997275b5;
  public static int bool_false = 0xbc799737;
  
  public static int msgs_ack = 0x62d6b459;
  public static int updates = 0x74ae4240;
  public static int updates_combined = 0x725b04c3;
  public static int updates_too_long = 0xe317af7e;
  public static int updates_get_state = 0xedd4882a;
  public static int updates_get_difference = 0x19c2f763;
  public static int updates_get_channel_difference = 0x03173d78;
  public static int updates_state = 0xa56c2a3e;
  public static int updates_difference = 0x00f49ca0;
  public static int updates_difference_slice = 0xa8fb1981;
  public static int updates_difference_empty = 0x5d75a138;
  public static int updates_difference_too_long = 0x4afe8f6d;
  public static int updates_channel_difference = 0x2064674e;
  public static int updates_channel_difference_empty = 0x3e11affb;
  public static int updates_channel_difference_too_long = 0xa4bcc6fe;
  public static int update_short = 0x78d4dec1;
  public static int update_new_message = 0x1f2b0afd;
  public static int update_message_id = 0x4e90bfd6;
  public static int update_delete_messages = 0xa20db0e5;
  public static int update_read_history_inbox = 0x9e84bc99;
  public static int update_read_history_outbox = 0x2f2f21bf;
  public static int update_new_channel_message = 0x62ba04d9;
  public static int update_read_channel_inbox = 0x922e6e10;
  public static int update_edit_message = 0xe40370a3;
  public static int update_edit_channel_message = 0x1b3f4df7;
  public static int update_read_channel_outbox = 0xb75f99a9;
  public static int update_delete_channel_messages = 0xc32d5b12;
  public static int update_encryption = 0xb4a2e88d;
  public static int update_short_message = 0x313bc7f8;
  public static int update_short_chat_message = 0x4d6deea5;
  public static int update_short_sent_message = 0x9015e101;
  public static int input_peer_empty = 0x7f3b18ea;
  public static int input_peer_self = 0x7da07ec9;
  public static int input_peer_chat = 0x35a95cb9;
  public static int input_peer_user = 0xf21158c6;
  public static int input_peer_channel = 0x27bcbbfc;
  public static int input_user_self = 0xf7c1b13f;
  public static int input_user = 0xf21158c6;
  public static int input_encrypted_chat = 0xf141b5e1;
  public static int input_reply_to_message = 0x869fbe10;
  public static int input_channel = 0xf35aec28;
  public static int input_document_file_location = 0xbad07584;
  public static int input_photo_file_location = 0x40181ffe;
  public static int channel_messages_filter_empty = 0x94d42ee7;
  
  //user
  public static int invoke_with_layer = 0xda9b0d0d;
  public static int send_code = 0xa677244f;
  public static int sign_in = 0x8d52a951;
  public static int auth_sent_code = 0x5e002502;
  public static int auth_sent_code_success = 0x2390fe44;
  public static int auth_authorization = 0x2ea2c0d4;
  public static int auth_authorization_sign_up_required = 0x44747e9a;
  public static int auth_check_password = 0xd18b4d16;
  public static int input_check_password_srp = 0xd27ff082;
  public static int code_settings = 0xad253d78;
  public static int code_settings_legacy = 0x8a6469c2;
  public static int init_connection = 0xc1cd5ea9;
  public static int account_get_password = 0x548a30f5;
  public static int account_password = 0x957b50fb;
  public static int password_kdf_algo_modpow = 0x3a912d4a;
  
  //help
  public static int help_get_config = 0xc4f9186b;
  public static int config = 0x330b4067;
  public static int config_modern = 0xcc1a241e;
  public static int messages_send_message = 0xfe05dc9a;
  public static int messages_forward_messages = 0x13704a7c;
  public static int messages_delete_messages = 0xe58e95d2;
  public static int messages_read_history = 0xe306d3a;
  public static int messages_get_dialogs = 0xa0f4cb4f;
  public static int messages_get_history = 0x4423e6c5;
  public static int messages_get_full_chat = 0xaeb00b34;
  public static int messages_request_encryption = 0xf64daf43;
  public static int messages_accept_encryption = 0x3dbc0415;
  public static int messages_discard_encryption = 0xf393aea0;
  public static int messages_send_encrypted_service = 0x32d439a4;
  public static int upload_get_file = 0xbe5335be;
  public static int upload_file = 0x096a18d5;
  public static int upload_file_cdn_redirect = 0xf18cda44;
  public static int messages_affected_messages = 0x84d19185;
  public static int messages_dialogs = 0x15ba6c40;
  public static int messages_dialogs_slice = 0x71e094f3;
  public static int messages_dialogs_not_modified = 0xf0e3e596;
  public static int messages_chat_full = 0xe5d7d19c;
  public static int messages_messages = 0x1d73e7ea;
  public static int messages_messages_legacy = 0x8c718e87;
  public static int messages_messages_slice = 0x5f206716;
  public static int messages_messages_slice_legacy = 0x762b263d;
  public static int messages_messages_not_modified = 0x74535f21;
  public static int messages_channel_messages = 0xc776ba4e;
  public static int contacts_search = 0x11f812d8;
  public static int contacts_found = 0xb3134d9d;
  public static int users_get_full_user = 0xb60f5918;
  public static int users_user_full = 0x3b6d152e;
  public static int channels_get_full_channel = 0x08736a09;
  public static int channels_join_channel = 0x24b524c5;
  public static int channels_leave_channel = 0xf836aa95;
  public static int channels_delete_messages = 0x84c1fd4e;
  public static int channels_read_history = 0xcc104937;
  public static int encrypted_chat_waiting = 0x66b25953;
  public static int encrypted_chat_requested = 0x48f1d94c;
  public static int encrypted_chat = 0x61f0d4c7;
  public static int encrypted_chat_discarded = 0x1e1c7c45;
  
  //i'm gonna need to implement gzip aren't i... https://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art053
  //https://zlib.net/feldspar.html
  public static int gzip_packed = 0x3072cfa1;
}
