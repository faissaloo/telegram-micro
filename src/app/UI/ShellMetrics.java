package ui;

import javax.microedition.lcdui.Font;

public final class ShellMetrics {
  public static final int TARGET_PHONE_WIDTH = 240;

  public final int screenWidth;
  public final int frameX;
  public final int frameWidth;
  public final int contentX;
  public final int contentWidth;
  public final int edgePadding;
  public final int innerPadding;
  public final Font titleFont;
  public final Font bodyFont;
  public final Font metaFont;
  public final Font badgeFont;

  private ShellMetrics(int screenWidth, int frameX, int frameWidth, int edgePadding, int innerPadding,
    Font titleFont, Font bodyFont, Font metaFont, Font badgeFont) {
    this.screenWidth = screenWidth;
    this.frameX = frameX;
    this.frameWidth = frameWidth;
    this.edgePadding = edgePadding;
    this.innerPadding = innerPadding;
    this.contentX = frameX + edgePadding;
    this.contentWidth = frameWidth - (edgePadding * 2);
    this.titleFont = titleFont;
    this.bodyFont = bodyFont;
    this.metaFont = metaFont;
    this.badgeFont = badgeFont;
  }

  public static ShellMetrics forCanvas(int width) {
    int resolvedWidth = width > 0 ? width : TARGET_PHONE_WIDTH;
    int frameWidth = resolvedWidth > TARGET_PHONE_WIDTH ? TARGET_PHONE_WIDTH : resolvedWidth;
    int frameX = (resolvedWidth - frameWidth) / 2;
    int edgePadding = frameWidth <= 176 ? 4 : 6;
    int innerPadding = frameWidth <= 176 ? 5 : 6;
    Font titleFont = Font.getFont(
      Font.FACE_PROPORTIONAL,
      Font.STYLE_BOLD,
      frameWidth <= 176 ? Font.SIZE_MEDIUM : Font.SIZE_LARGE
    );
    Font bodyFont = Font.getFont(
      Font.FACE_PROPORTIONAL,
      Font.STYLE_PLAIN,
      frameWidth <= 176 ? Font.SIZE_SMALL : Font.SIZE_MEDIUM
    );
    Font metaFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    Font badgeFont = Font.getFont(
      Font.FACE_PROPORTIONAL,
      Font.STYLE_BOLD,
      frameWidth <= 176 ? Font.SIZE_SMALL : Font.SIZE_MEDIUM
    );

    return new ShellMetrics(
      resolvedWidth,
      frameX,
      frameWidth,
      edgePadding,
      innerPadding,
      titleFont,
      bodyFont,
      metaFont,
      badgeFont
    );
  }
}
