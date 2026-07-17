package test;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.Serializer;
import mtproto.recieve.help.RecieveConfig;

public class RecieveConfigContext {
  public static class LegacyConfigParseTest extends Test {
    public String label() {
      return "Can parse legacy Config payloads";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(0)
        .append_int(1234)
        .append_int(5678)
        .append_int(RecieveConfig.bool_false)
        .append_int(2)
        .append_int(CombinatorIds.vector)
        .append_int(2)
        .append_int(RecieveConfig.dc_option)
        .append_int(0)
        .append_int(2)
        .append_string("149.154.167.40")
        .append_int(443)
        .append_int(RecieveConfig.dc_option)
        .append_int(1 << 10)
        .append_int(3)
        .append_string("149.154.175.100")
        .append_int(80)
        .append_byte_string(new byte[] {(byte)0x11, (byte)0x22, (byte)0x33})
        .end();

      RecieveConfig config = RecieveConfig.from_rpc_response(new RPCResponse(CombinatorIds.config, body));

      expect(config.flags, 0);
      expect(config.date, 1234);
      expect(config.expires, 5678);
      expect(config.test_mode, false);
      expect(config.this_dc, 2);
      expect(config.dc_options.length, 2);
      expect(config.dc_options[0].ip_address, "149.154.167.40");
      expect(config.dc_options[0].port, 443);
      expect(config.dc_options[0].has_secret(), false);
      expect(config.dc_options[1].id, 3);
      expect(config.dc_options[1].has_secret(), true);
      expect(config.dc_options[1].secret, new byte[] {(byte)0x11, (byte)0x22, (byte)0x33});
      expect(config.find_this_dc_option().id, 2);
    }
  }

  public static class ModernConfigTypeAcceptedTest extends Test {
    public String label() {
      return "Can accept modern Config constructor ids";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(7)
        .append_int(99)
        .append_int(100)
        .append_int(RecieveConfig.bool_true)
        .append_int(4)
        .append_int(CombinatorIds.vector)
        .append_int(1)
        .append_int(RecieveConfig.dc_option)
        .append_int(0)
        .append_int(4)
        .append_string("2001:db8::4")
        .append_int(443)
        .end();

      RecieveConfig config = RecieveConfig.from_rpc_response(new RPCResponse(CombinatorIds.config_modern, body));

      expect(config.flags, 7);
      expect(config.test_mode, true);
      expect(config.this_dc, 4);
      expect(config.find_this_dc_option().ip_address, "2001:db8::4");
    }
  }

  public static class PreferredDcOptionPrefersUsableIpv4Test extends Test {
    public String label() {
      return "Preferred DC lookup favors non-secret IPv4 endpoints";
    }

    public void test() throws TestFailureException {
      RecieveConfig config = new RecieveConfig(
        0,
        1,
        2,
        false,
        4,
        new RecieveConfig.DCOption[] {
          new RecieveConfig.DCOption(1 << 10, 4, "149.154.167.91", 443, new byte[] {(byte)0x01}),
          new RecieveConfig.DCOption(0, 4, "2001:db8::4", 443, new byte[0]),
          new RecieveConfig.DCOption(0, 4, "149.154.167.91", 443, new byte[0])
        }
      );

      expect(config.find_preferred_dc_option(4).ip_address, "149.154.167.91");
      expect(config.find_preferred_dc_option(4).has_secret(), false);
    }
  }
}
