package test;

import ui.MessageTime;

public class MessageTimeContext {
  public static class FormatsUtcWithoutDateObjectsTest extends Test {
    public String label() {
      return "Message times format into a reusable fixed UTC buffer";
    }

    public void test() throws TestFailureException {
      char[] buffer = new char[MessageTime.LABEL_LENGTH];
      int length;

      length = MessageTime.formatUtc(1700000000, buffer);
      expect(length, MessageTime.LABEL_LENGTH);
      expect(new String(buffer, 0, length), "22:13Z");

      length = MessageTime.formatUtc(86400, buffer);
      expect(new String(buffer, 0, length), "00:00Z");
      expect(MessageTime.formatUtc(0, buffer), 0);
      expect(MessageTime.formatUtc(-1, buffer), 0);
      expect(MessageTime.formatUtc(1700000000, new char[5]), 0);
    }
  }
}
