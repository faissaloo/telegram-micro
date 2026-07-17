package ui;

import crypto.EntropyPool;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** First-install key-timing collector used when the platform has no CSPRNG. */
public final class EntropyCanvas extends Canvas {
  private static final int EVENT_PRESS = 1;
  private static final int EVENT_RELEASE = 2;

  private final EntropyScreenHost host;
  private final EntropyPool pool;
  private boolean delivered;
  private String failure = "";

  public EntropyCanvas(EntropyScreenHost host) {
    this.host = host;
    byte[] initial = new byte[32];
    putLong(initial, 0, System.currentTimeMillis());
    putLong(initial, 8, Runtime.getRuntime().freeMemory());
    putLong(initial, 16, Runtime.getRuntime().totalMemory());
    putLong(initial, 24, hashCode());
    pool = new EntropyPool(initial);
    clear(initial);
    setFullScreenMode(true);
  }

  public void showFailure(String detail) {
    failure = detail == null ? "Could not save entropy safely." : detail;
    repaint();
  }

  protected void keyPressed(int keyCode) {
    collect(keyCode, EVENT_PRESS);
  }

  protected void keyReleased(int keyCode) {
    collect(keyCode, EVENT_RELEASE);
  }

  protected void paint(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    int margin = 14;
    int barWidth = width - (margin * 2);
    int progressWidth = (barWidth * pool.eventCount()) / EntropyPool.REQUIRED_EVENTS;
    Font title = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    Font body = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

    g.setColor(0x142536);
    g.fillRect(0, 0, width, height);
    g.setColor(0xffffff);
    g.setFont(title);
    g.drawString("Secure setup", margin, 18, Graphics.TOP | Graphics.LEFT);
    g.setFont(body);
    g.setColor(0xd7e6f3);
    g.drawString("Tap different keys naturally.", margin, 60, Graphics.TOP | Graphics.LEFT);
    g.drawString("Press and release both count.", margin, 82, Graphics.TOP | Graphics.LEFT);

    g.setColor(0x52677a);
    g.fillRect(margin, 124, barWidth, 18);
    g.setColor(failure.length() == 0 ? 0x4ba3e3 : 0xd45b5b);
    g.fillRect(margin, 124, progressWidth, 18);
    g.setColor(0xffffff);
    g.drawString(
      Integer.toString(pool.remainingEvents()) + " timing samples remaining",
      margin,
      154,
      Graphics.TOP | Graphics.LEFT
    );

    if (failure.length() > 0) {
      g.setColor(0xffb3b3);
      g.drawString(failure, margin, 190, Graphics.TOP | Graphics.LEFT);
    } else {
      g.setColor(0xaec5d8);
      g.drawString("Network access starts only after", margin, 190, Graphics.TOP | Graphics.LEFT);
      g.drawString("this one-time setup is saved.", margin, 212, Graphics.TOP | Graphics.LEFT);
    }
  }

  private void collect(int keyCode, int eventKind) {
    if (delivered || failure.length() > 0) {
      return;
    }
    if (pool.recordEvent(
        System.currentTimeMillis(),
        keyCode,
        eventKind,
        Runtime.getRuntime().freeMemory())) {
      repaint();
    }
    if (pool.ready()) {
      delivered = true;
      host.onEntropyCollected(pool.extractSeed());
    }
  }

  private static void putLong(byte[] out, int offset, long value) {
    for (int i = 7; i >= 0; --i) {
      out[offset + i] = (byte)(value & 0xff);
      value >>>= 8;
    }
  }

  private static void clear(byte[] value) {
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }
}
