package ui;

import javax.microedition.lcdui.Graphics;

final class ShellTheme {
  static final int BG_TOP = 0x18314f;
  static final int BG_BOTTOM = 0x0a1627;
  static final int PANEL_TOP = 0x315c8f;
  static final int PANEL_BOTTOM = 0x173252;
  static final int PANEL_SHADOW = 0x07111d;
  static final int PANEL_HILITE = 0x5b93d1;
  static final int TEXT_PRIMARY = 0xf3f7fb;
  static final int TEXT_MUTED = 0xb4c7dd;
  static final int TEXT_SOFT = 0x7f94ac;
  static final int INCOMING_FILL = 0xe2edf9;
  static final int INCOMING_TEXT = 0x11253b;
  static final int OUTGOING_FILL = 0x83d08f;
  static final int OUTGOING_TEXT = 0x10301a;
  static final int SELECT_FILL = 0xf7d67b;
  static final int SELECT_TEXT = 0x352300;
  static final int BADGE_FILL = 0xf4c055;
  static final int BADGE_TEXT = 0x352300;
  static final int CARD_FILL = 0xdfeafb;
  static final int CARD_FILL_SELECTED = 0xf8d783;
  static final int CARD_TEXT = 0x10233a;
  static final int CARD_PREVIEW = 0x37506b;

  private ShellTheme() {
  }

  static void fillVerticalGradient(Graphics g, int x, int y, int width, int height, int top, int bottom, int steps) {
    int bands = steps > 1 ? steps : 1;
    int bandHeight = height / bands;
    int remainder = height % bands;

    for (int i = 0; i < bands; ++i) {
      int lineHeight = bandHeight + (i < remainder ? 1 : 0);
      int lineY = y + ((height * i) / bands);
      g.setColor(mix(top, bottom, i, bands - 1));
      g.fillRect(x, lineY, width, lineHeight);
    }
  }

  static void fillBevelBox(Graphics g, int x, int y, int width, int height, int fill, int hilite, int shadow) {
    g.setColor(fill);
    g.fillRect(x, y, width, height);
    g.setColor(hilite);
    g.drawLine(x, y, x + width - 1, y);
    g.drawLine(x, y, x, y + height - 1);
    g.setColor(shadow);
    g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
    g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
  }

  static void drawBadge(Graphics g, int x, int y, int width, int height, int fill, int textColor, String text) {
    fillBevelBox(g, x, y, width, height, fill, mix(fill, 0xffffff, 1, 4), mix(fill, 0, 1, 3));
    g.setColor(textColor);
    g.drawString(text, x + (width / 2), y + (height / 2), Graphics.HCENTER | Graphics.VCENTER);
  }

  private static int mix(int a, int b, int step, int maxStep) {
    int ratio = maxStep <= 0 ? 0 : (step * 255) / maxStep;
    int ar = (a >> 16) & 0xff;
    int ag = (a >> 8) & 0xff;
    int ab = a & 0xff;
    int br = (b >> 16) & 0xff;
    int bg = (b >> 8) & 0xff;
    int bb = b & 0xff;
    int rr = ((ar * (255 - ratio)) + (br * ratio)) / 255;
    int rg = ((ag * (255 - ratio)) + (bg * ratio)) / 255;
    int rb = ((ab * (255 - ratio)) + (bb * ratio)) / 255;

    return (rr << 16) | (rg << 8) | rb;
  }
}
