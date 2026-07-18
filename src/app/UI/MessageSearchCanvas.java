package ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import reader.MessageListModel;
import search.MessageSearchStateModel;

/** Low-allocation, keypad-first rendering for one bounded search page. */
final class MessageSearchCanvas extends Canvas {
  private static final String TIME_GLYPHS = "0123456789:Z";

  private final AppShellModel model;
  private final AppShellScreen host;
  private final int[] wrapStarts = new int[4];
  private final int[] wrapLengths = new int[4];
  private final char[] timeChars = new char[MessageTime.LABEL_LENGTH];
  private ShellMetrics metrics = ShellMetrics.forCanvas(ShellMetrics.TARGET_PHONE_WIDTH);
  private int scrollY = 0;

  MessageSearchCanvas(AppShellModel model, AppShellScreen host) {
    this.model = model;
    this.host = host;
    setFullScreenMode(true);
  }

  void refresh() {
    ensureSelectionVisible();
    repaint();
  }

  protected void sizeChanged(int width, int height) {
    metrics = ShellMetrics.forCanvas(width);
    ensureSelectionVisible();
  }

  protected void keyPressed(int keyCode) {
    int gameAction = safeGameAction(keyCode);
    int selected = model.messageSearch.results.selectedIndex;

    if (gameAction == Canvas.UP) {
      if (selected > 0 && model.selectMessageSearchIndex(selected - 1)) {
        ensureSelectionVisible();
        repaint();
      }
      return;
    }
    if (gameAction == Canvas.DOWN) {
      if (selected + 1 < model.messageSearch.results.count &&
          model.selectMessageSearchIndex(selected + 1)) {
        ensureSelectionVisible();
        repaint();
      }
      return;
    }
    if ((gameAction == Canvas.FIRE || keyCode == KEY_NUM5) &&
        model.messageSearch.results.count > 0) {
      host.showMessageSearchDetail();
    }
  }

  protected void paint(Graphics g) {
    MessageSearchStateModel search = model.messageSearch;
    int width = getWidth();
    int height = getHeight();
    int headerHeight = metrics.titleFont.getHeight() + metrics.bodyFont.getHeight() + 14;
    int footerHeight = metrics.metaFont.getHeight() + 8;
    int viewportTop = headerHeight + 4;
    int viewportHeight = height - viewportTop - footerHeight - 4;
    int rowHeight = rowHeight();
    int x = metrics.contentX;
    int selected = search.results.selectedIndex;
    String headerText = search.query.length() > 0 ? search.query : "Search messages";
    int summaryWidth = metrics.metaFont.stringWidth(search.summary);
    int headerTextWidth = metrics.contentWidth - summaryWidth - 8;
    int headerLineCount;

    normalizeScroll(viewportHeight);
    ShellTheme.fillVerticalGradient(
      g, 0, 0, width, height, ShellTheme.BG_TOP, ShellTheme.BG_BOTTOM, 10
    );
    ShellTheme.fillBevelBox(
      g,
      metrics.frameX,
      0,
      metrics.frameWidth,
      headerHeight,
      ShellTheme.PANEL_BOTTOM,
      ShellTheme.PANEL_HILITE,
      ShellTheme.PANEL_SHADOW
    );
    g.setFont(metrics.titleFont);
    g.setColor(ShellTheme.TEXT_PRIMARY);
    g.drawString(search.peerTitle, x, 6, Graphics.TOP | Graphics.LEFT);
    g.setFont(metrics.bodyFont);
    g.setColor(ShellTheme.TEXT_MUTED);
    headerLineCount = ShellTextWrap.wrap(
      headerText,
      metrics.bodyFont,
      headerTextWidth > 16 ? headerTextWidth : 16,
      wrapStarts,
      wrapLengths
    );
    if (headerLineCount > 0 && wrapLengths[0] > 0) {
      g.drawSubstring(
        headerText,
        wrapStarts[0],
        wrapLengths[0],
        x,
        8 + metrics.titleFont.getHeight(),
        Graphics.TOP | Graphics.LEFT
      );
    }
    g.setFont(metrics.metaFont);
    g.drawString(
      search.summary,
      x + metrics.contentWidth,
      9 + metrics.titleFont.getHeight(),
      Graphics.TOP | Graphics.RIGHT
    );

    if (search.loading) {
      drawEmptyStateCard(
        g,
        height,
        "Searching Telegram...",
        "Waiting for one bounded server result page. Back keeps the current chat."
      );
      drawFooter(g, height, footerHeight, "Back  Edit");
      return;
    }
    if (search.results.count == 0) {
      drawEmptyStateCard(
        g,
        height,
        "SEARCH FAIL".equals(search.summary) ? "Search failed." : "No matches.",
        search.detail
      );
      drawFooter(g, height, footerHeight, "Back  Edit  Refresh");
      return;
    }

    for (int i = 0; i < search.results.count; ++i) {
      int rowY = viewportTop + (i * rowHeight) - scrollY;

      if (rowY + rowHeight < viewportTop || rowY > viewportTop + viewportHeight) {
        continue;
      }
      drawResultRow(g, search.results.items[i], rowY, i == selected, rowHeight);
    }

    drawFooter(
      g,
      height,
      footerHeight,
      Integer.toString(selected + 1) + "/" +
        Integer.toString(search.results.count) + " of " +
        Integer.toString(search.totalCount) + "  5 Open"
    );
  }

  private void drawResultRow(
    Graphics g,
    MessageListModel.Item result,
    int rowY,
    boolean selected,
    int rowHeight
  ) {
    Font senderFont = metrics.badgeFont;
    Font bodyFont = metrics.bodyFont;
    Font metaFont = metrics.metaFont;
    String preview = result.text != null && result.text.length() > 0
      ? result.text
      : (result.mediaInfo != null && result.mediaInfo.length() > 0
        ? result.mediaInfo
        : result.mediaLabel);
    int timeLength;
    int timeWidth = 0;
    int senderWidth;
    int senderLineCount;
    int previewWidth = metrics.contentWidth - (metrics.innerPadding * 2);
    int lineCount;

    ShellTheme.fillBevelBox(
      g,
      metrics.contentX,
      rowY,
      metrics.contentWidth,
      rowHeight - 4,
      selected ? ShellTheme.CARD_FILL_SELECTED : ShellTheme.CARD_FILL,
      0xffffff,
      0x6f879d
    );
    timeLength = MessageTime.formatUtc(result.dateSeconds, timeChars);
    if (timeLength > 0) {
      timeWidth = charsWidth(metaFont, timeChars, timeLength);
    }
    senderWidth = metrics.contentWidth - (metrics.innerPadding * 2) -
      (timeWidth > 0 ? timeWidth + 8 : 0);
    g.setFont(senderFont);
    g.setColor(selected ? ShellTheme.SELECT_TEXT : ShellTheme.CARD_TEXT);
    senderLineCount = ShellTextWrap.wrap(
      result.sender,
      senderFont,
      senderWidth > 16 ? senderWidth : 16,
      wrapStarts,
      wrapLengths
    );
    if (senderLineCount > 0 && wrapLengths[0] > 0) {
      g.drawSubstring(
        result.sender,
        wrapStarts[0],
        wrapLengths[0],
        metrics.contentX + metrics.innerPadding,
        rowY + 5,
        Graphics.TOP | Graphics.LEFT
      );
    }
    if (timeLength > 0) {
      drawTime(
        g,
        metaFont,
        timeLength,
        metrics.contentX + metrics.contentWidth - metrics.innerPadding - timeWidth,
        rowY + 6,
        selected ? ShellTheme.SELECT_TEXT : ShellTheme.TEXT_SOFT
      );
    }

    if (result.hasMedia && result.mediaLabel != null && result.mediaLabel.length() > 0) {
      previewWidth -= bodyFont.stringWidth(result.mediaLabel) + 12;
      g.setFont(metaFont);
      g.setColor(selected ? ShellTheme.SELECT_TEXT : ShellTheme.TEXT_SOFT);
      g.drawString(
        result.mediaLabel,
        metrics.contentX + metrics.contentWidth - metrics.innerPadding,
        rowY + 7 + senderFont.getHeight(),
        Graphics.TOP | Graphics.RIGHT
      );
    }

    g.setFont(bodyFont);
    g.setColor(selected ? ShellTheme.SELECT_TEXT : ShellTheme.CARD_PREVIEW);
    lineCount = ShellTextWrap.wrap(
      preview,
      bodyFont,
      previewWidth,
      wrapStarts,
      wrapLengths
    );
    if (lineCount > 0 && wrapLengths[0] > 0) {
      g.drawSubstring(
        preview,
        wrapStarts[0],
        wrapLengths[0],
        metrics.contentX + metrics.innerPadding,
        rowY + 7 + senderFont.getHeight(),
        Graphics.TOP | Graphics.LEFT
      );
    }
  }

  private void drawTime(
    Graphics g,
    Font font,
    int length,
    int x,
    int y,
    int color
  ) {
    g.setFont(font);
    g.setColor(color);
    for (int i = 0; i < length; ++i) {
      char glyph = timeChars[i];
      int glyphIndex = glyph >= '0' && glyph <= '9'
        ? glyph - '0'
        : (glyph == ':' ? 10 : 11);

      g.drawSubstring(TIME_GLYPHS, glyphIndex, 1, x, y, Graphics.TOP | Graphics.LEFT);
      x += font.charWidth(glyph);
    }
  }

  private static int charsWidth(Font font, char[] chars, int length) {
    int width = 0;

    for (int i = 0; i < length; ++i) {
      width += font.charWidth(chars[i]);
    }
    return width;
  }

  private int rowHeight() {
    return metrics.badgeFont.getHeight() + metrics.bodyFont.getHeight() + 14;
  }

  private void ensureSelectionVisible() {
    int viewportHeight = viewportHeight();
    int rowHeight = rowHeight();
    int selected = model.messageSearch.results.selectedIndex;
    int selectedTop;
    int selectedBottom;

    if (model.messageSearch.results.count == 0 || selected < 0) {
      scrollY = 0;
      return;
    }
    selectedTop = selected * rowHeight;
    selectedBottom = selectedTop + rowHeight;
    if (selectedTop < scrollY) {
      scrollY = selectedTop;
    } else if (selectedBottom > scrollY + viewportHeight) {
      scrollY = selectedBottom - viewportHeight;
    }
    normalizeScroll(viewportHeight);
  }

  private void normalizeScroll(int viewportHeight) {
    int maxScroll = (model.messageSearch.results.count * rowHeight()) - viewportHeight;

    if (scrollY < 0 || maxScroll <= 0) {
      scrollY = 0;
    } else if (scrollY > maxScroll) {
      scrollY = maxScroll;
    }
  }

  private int viewportHeight() {
    return getHeight() -
      (metrics.titleFont.getHeight() + metrics.bodyFont.getHeight() + 14) -
      (metrics.metaFont.getHeight() + 8) - 8;
  }

  private void drawEmptyStateCard(Graphics g, int height, String title, String detail) {
    int cardHeight = metrics.titleFont.getHeight() + (metrics.bodyFont.getHeight() * 3) + 16;
    int cardY = (height / 2) - (cardHeight / 2) - 12;
    int lineCount;

    ShellTheme.fillBevelBox(
      g,
      metrics.contentX,
      cardY,
      metrics.contentWidth,
      cardHeight,
      ShellTheme.PANEL_BOTTOM,
      ShellTheme.PANEL_HILITE,
      ShellTheme.PANEL_SHADOW
    );
    g.setFont(metrics.titleFont);
    g.setColor(ShellTheme.TEXT_PRIMARY);
    g.drawString(
      title,
      metrics.contentX + 10,
      cardY + 8,
      Graphics.TOP | Graphics.LEFT
    );
    g.setFont(metrics.bodyFont);
    g.setColor(ShellTheme.TEXT_MUTED);
    lineCount = ShellTextWrap.wrap(
      detail,
      metrics.bodyFont,
      metrics.contentWidth - 20,
      wrapStarts,
      wrapLengths
    );
    for (int line = 0; line < lineCount && line < 3; ++line) {
      if (wrapLengths[line] > 0) {
        g.drawSubstring(
          detail,
          wrapStarts[line],
          wrapLengths[line],
          metrics.contentX + 10,
          cardY + 12 + metrics.titleFont.getHeight() +
            (line * metrics.bodyFont.getHeight()),
          Graphics.TOP | Graphics.LEFT
        );
      }
    }
  }

  private void drawFooter(Graphics g, int height, int footerHeight, String text) {
    ShellTheme.fillBevelBox(
      g,
      metrics.frameX,
      height - footerHeight,
      metrics.frameWidth,
      footerHeight,
      ShellTheme.PANEL_BOTTOM,
      ShellTheme.PANEL_HILITE,
      ShellTheme.PANEL_SHADOW
    );
    g.setFont(metrics.metaFont);
    g.setColor(ShellTheme.TEXT_MUTED);
    g.drawString(
      text,
      metrics.contentX,
      height - footerHeight + 3,
      Graphics.TOP | Graphics.LEFT
    );
  }

  private int safeGameAction(int keyCode) {
    try {
      return getGameAction(keyCode);
    } catch (IllegalArgumentException e) {
      return 0;
    }
  }
}
