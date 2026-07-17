package ui;

import javax.microedition.lcdui.Font;

public final class ShellMetrics extends ShellLayout {
  public final Font titleFont;
  public final Font bodyFont;
  public final Font metaFont;
  public final Font badgeFont;

  private ShellMetrics(int width) {
    super(width);
    this.titleFont = Font.getFont(
      Font.FACE_PROPORTIONAL,
      Font.STYLE_BOLD,
      this.frameWidth <= 176 ? Font.SIZE_MEDIUM : Font.SIZE_LARGE
    );
    this.bodyFont = Font.getFont(
      Font.FACE_PROPORTIONAL,
      Font.STYLE_PLAIN,
      this.frameWidth <= 176 ? Font.SIZE_SMALL : Font.SIZE_MEDIUM
    );
    this.metaFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    this.badgeFont = Font.getFont(
      Font.FACE_PROPORTIONAL,
      Font.STYLE_BOLD,
      this.frameWidth <= 176 ? Font.SIZE_SMALL : Font.SIZE_MEDIUM
    );
  }

  public static ShellMetrics forCanvas(int width) {
    return new ShellMetrics(width);
  }
}
