package ui;

/** Formats Telegram epoch seconds without allocating paint-time objects. */
public final class MessageTime {
  public static final int LABEL_LENGTH = 6;
  private static final int SECONDS_PER_DAY = 24 * 60 * 60;

  private MessageTime() {
  }

  public static int formatUtc(int dateSeconds, char[] out) {
    int secondsOfDay;
    int hour;
    int minute;

    if (dateSeconds <= 0 || out == null || out.length < LABEL_LENGTH) {
      return 0;
    }

    secondsOfDay = dateSeconds % SECONDS_PER_DAY;
    hour = secondsOfDay / (60 * 60);
    minute = (secondsOfDay / 60) % 60;
    out[0] = (char)('0' + (hour / 10));
    out[1] = (char)('0' + (hour % 10));
    out[2] = ':';
    out[3] = (char)('0' + (minute / 10));
    out[4] = (char)('0' + (minute % 10));
    out[5] = 'Z';
    return LABEL_LENGTH;
  }
}
