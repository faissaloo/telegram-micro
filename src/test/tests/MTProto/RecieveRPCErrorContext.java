package test;

import mtproto.recieve.RecieveRPCError;
import support.Utf8String;

public class RecieveRPCErrorContext {
  public static class ParsePhoneMigrateDcTest extends Test {
    public String label() {
      return "Can parse PHONE_MIGRATE target DC ids";
    }

    public void test() throws TestFailureException {
      expect(RecieveRPCError.migrate_dc_id("PHONE_MIGRATE_4"), 4);
      expect((new RecieveRPCError(303, new Utf8String(new byte[] {
        'P', 'H', 'O', 'N', 'E', '_', 'M', 'I', 'G', 'R', 'A', 'T', 'E', '_', '2'
      }))).migrate_dc_id(), 2);
    }
  }

  public static class IgnoreNonMigrateRpcMessagesTest extends Test {
    public String label() {
      return "Ignores RPC errors that are not DC migrations";
    }

    public void test() throws TestFailureException {
      expect(RecieveRPCError.migrate_dc_id("PHONE_CODE_INVALID"), 0);
      expect(RecieveRPCError.migrate_dc_id("PHONE_MIGRATE_X"), 0);
      expect(RecieveRPCError.migrate_dc_id(null), 0);
    }
  }
}
