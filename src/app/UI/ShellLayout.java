package ui;

/** Pure phone-frame geometry which does not initialize LCDUI. */
public class ShellLayout {
  public static final int TARGET_PHONE_WIDTH = 240;

  public final int screenWidth;
  public final int frameX;
  public final int frameWidth;
  public final int contentX;
  public final int contentWidth;
  public final int edgePadding;
  public final int innerPadding;

  protected ShellLayout(int width) {
    int resolvedWidth = width > 0 ? width : TARGET_PHONE_WIDTH;
    int frameWidth = resolvedWidth > TARGET_PHONE_WIDTH ? TARGET_PHONE_WIDTH : resolvedWidth;
    int frameX = (resolvedWidth - frameWidth) / 2;
    int edgePadding = frameWidth <= 176 ? 4 : 6;
    int innerPadding = frameWidth <= 176 ? 5 : 6;

    this.screenWidth = resolvedWidth;
    this.frameX = frameX;
    this.frameWidth = frameWidth;
    this.edgePadding = edgePadding;
    this.innerPadding = innerPadding;
    this.contentX = frameX + edgePadding;
    this.contentWidth = frameWidth - (edgePadding * 2);
  }

  public static ShellLayout forCanvas(int width) {
    return new ShellLayout(width);
  }
}
