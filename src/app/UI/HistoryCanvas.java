package ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import reader.DialogListModel;
import reader.MessageListModel;

final class HistoryCanvas extends Canvas {
  // The pinned hosted backend leaves charsWidth/drawChars unfinished.
  private static final String TIME_GLYPHS = "0123456789:Z";
  private final AppShellModel model;
  private final int[] messageTop;
  private final int[] messageHeight;
  private final int[] messageWidth;
  private final int[] wrapStarts = new int[24];
  private final int[] wrapLengths = new int[24];
  private final int[] mediaWrapStarts = new int[16];
  private final int[] mediaWrapLengths = new int[16];
  private final char[] timeChars = new char[MessageTime.LABEL_LENGTH];
  private ShellMetrics metrics = ShellMetrics.forCanvas(ShellMetrics.TARGET_PHONE_WIDTH);
  private int scrollY = 0;
  private int contentHeight = 0;
  private int layoutWidth = -1;

  HistoryCanvas(AppShellModel model) {
    this.model = model;
    this.messageTop = new int[model.reader.history.capacity()];
    this.messageHeight = new int[model.reader.history.capacity()];
    this.messageWidth = new int[model.reader.history.capacity()];
    setFullScreenMode(true);
  }

  void refresh() {
    invalidateLayout();
    ensureSelectedVisible();
    repaint();
  }

  protected void keyPressed(int keyCode) {
    int gameAction = safeGameAction(keyCode);

    if (gameAction == Canvas.UP) {
      if (model.focusPreviousMessage()) {
        ensureSelectedVisible();
        repaint();
      }
      return;
    }
    if (gameAction == Canvas.DOWN) {
      if (model.focusNextMessage()) {
        ensureSelectedVisible();
        repaint();
      }
      return;
    }
    if (gameAction == Canvas.LEFT) {
      pageBy(-viewportHeight());
      repaint();
      return;
    }
    if (gameAction == Canvas.RIGHT) {
      pageBy(viewportHeight());
      repaint();
    }
  }

  protected void sizeChanged(int width, int height) {
    metrics = ShellMetrics.forCanvas(width);
    invalidateLayout();
  }

  protected void paint(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    Font headerFont = metrics.titleFont;
    Font bodyFont = metrics.bodyFont;
    Font metaFont = metrics.metaFont;
    int headerHeight = headerFont.getHeight() + bodyFont.getHeight() + 16;
    int footerHeight = metaFont.getHeight() + 8;
    int viewportTop = headerHeight + 4;
    int viewportHeight = height - viewportTop - footerHeight - 4;

    ensureLayout();
    normalizeScroll(viewportHeight);

    ShellTheme.fillVerticalGradient(g, 0, 0, width, height, ShellTheme.BG_TOP, ShellTheme.BG_BOTTOM, 12);
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
    g.setFont(headerFont);
    g.setColor(ShellTheme.TEXT_PRIMARY);
    g.drawString(model.selectedDialogTitle(), metrics.contentX, 6, Graphics.TOP | Graphics.LEFT);
    g.setFont(bodyFont);
    g.setColor(ShellTheme.TEXT_MUTED);
    g.drawString(model.readerSummary, metrics.contentX, 8 + headerFont.getHeight(), Graphics.TOP | Graphics.LEFT);

    if (model.reader.loadingHistory) {
      drawEmptyStateCard(g, width, height, "Loading history...", "Fetching the recent message window for the selected dialog.");
      drawFooter(g, height, footerHeight, "Refresh  Compose  Reply  Media");
      return;
    }
    if (!model.reader.historyLoaded) {
      drawEmptyStateCard(g, width, height, "No history loaded yet.", "Open a dialog and use Refresh to fetch a recent message window.");
      drawFooter(g, height, footerHeight, "Dialogs  Status  Refresh");
      return;
    }
    if (model.reader.history.count == 0) {
      drawEmptyStateCard(g, width, height, "No recent messages.", "This cached history window is empty. Try Refresh for another slice.");
      drawFooter(g, height, footerHeight, "Dialogs  Compose  Refresh");
      return;
    }

    for (int i = 0; i < model.reader.history.count; ++i) {
      int bubbleY = viewportTop + messageTop[i] - scrollY;

      if (bubbleY + messageHeight[i] < viewportTop || bubbleY > viewportTop + viewportHeight) {
        continue;
      }
      drawMessageBubble(g, i, bubbleY);
    }

    drawFooter(g, height, footerHeight,
      Integer.toString(model.reader.history.selectedIndex + 1) +
      "/" + Integer.toString(model.reader.history.count) + "  2/8 Select  4/6 Page  Media");
  }

  private void drawMessageBubble(Graphics g, int index, int y) {
    MessageListModel.Item item = model.reader.history.items[index];
    boolean selected = index == model.reader.history.selectedIndex;
    Font senderFont = metrics.titleFont;
    Font bodyFont = metrics.bodyFont;
    Font metaFont = metrics.metaFont;
    int bubblePadding = metrics.innerPadding;
    int maxBubbleWidth = metrics.contentWidth - 20;
    int lineCount = ShellTextWrap.wrap(item.text, bodyFont, maxBubbleWidth - (bubblePadding * 2), wrapStarts, wrapLengths);
    String mediaSummary = item.mediaSummary();
    String messageStatus = messageStatus(item);
    int mediaLineCount = item.hasMedia && mediaSummary.length() > 0
      ? ShellTextWrap.wrap(mediaSummary, metaFont, maxBubbleWidth - (bubblePadding * 2), mediaWrapStarts, mediaWrapLengths)
      : 0;
    int bubbleWidth = messageWidth[index];
    int bubbleX = item.outgoing
      ? metrics.contentX + metrics.contentWidth - bubbleWidth
      : metrics.contentX;
    int textX = bubbleX + bubblePadding;
    int textY = y + bubblePadding;
    int fill = selected
      ? ShellTheme.SELECT_FILL
      : (item.outgoing ? ShellTheme.OUTGOING_FILL : ShellTheme.INCOMING_FILL);
    int textColor = selected
      ? ShellTheme.SELECT_TEXT
      : (item.outgoing ? ShellTheme.OUTGOING_TEXT : ShellTheme.INCOMING_TEXT);
    int timeLength;

    ShellTheme.fillBevelBox(
      g,
      bubbleX,
      y,
      bubbleWidth,
      messageHeight[index] - 4,
      fill,
      0xffffff,
      0x6f879d
    );

    g.setFont(senderFont);
    g.setColor(textColor);
    g.drawString(item.sender, textX, textY, Graphics.TOP | Graphics.LEFT);
    textY += senderFont.getHeight() + 2;

    g.setFont(bodyFont);
    for (int line = 0; line < lineCount; ++line) {
      if (wrapLengths[line] > 0) {
        g.drawSubstring(item.text, wrapStarts[line], wrapLengths[line], textX, textY, Graphics.TOP | Graphics.LEFT);
      }
      textY += bodyFont.getHeight();
    }

    g.setFont(metaFont);
    g.setColor(selected ? ShellTheme.SELECT_TEXT : ShellTheme.TEXT_SOFT);
    for (int line = 0; line < mediaLineCount; ++line) {
      if (mediaWrapLengths[line] > 0) {
        g.drawSubstring(mediaSummary, mediaWrapStarts[line], mediaWrapLengths[line], textX, textY + 2, Graphics.TOP | Graphics.LEFT);
      }
      textY += metaFont.getHeight();
    }
    g.setColor(item.pending ? 0x845900 : (selected ? ShellTheme.SELECT_TEXT : ShellTheme.TEXT_SOFT));
    if (messageStatus.length() > 0) {
      g.drawString(messageStatus, textX, textY + 2, Graphics.TOP | Graphics.LEFT);
    }
    timeLength = MessageTime.formatUtc(item.dateSeconds, timeChars);
    if (timeLength > 0) {
      int timeWidth = charsWidth(metaFont, timeChars, timeLength);
      int timeX = bubbleX + bubbleWidth - bubblePadding - timeWidth;

      for (int i = 0; i < timeLength; ++i) {
        char glyph = timeChars[i];
        int glyphIndex = glyph >= '0' && glyph <= '9'
          ? glyph - '0'
          : (glyph == ':' ? 10 : 11);

        g.drawSubstring(TIME_GLYPHS, glyphIndex, 1, timeX, textY + 2,
          Graphics.TOP | Graphics.LEFT);
        timeX += metaFont.charWidth(glyph);
      }
    }
  }

  private static int charsWidth(Font font, char[] chars, int length) {
    int width = 0;

    for (int i = 0; i < length; ++i) {
      width += font.charWidth(chars[i]);
    }
    return width;
  }

  private String messageStatus(MessageListModel.Item item) {
    DialogListModel.Item dialog;

    if (item.pending) {
      return "pending";
    }
    if (!item.outgoing || item.messageId <= 0) {
      return "";
    }

    dialog = model.reader.selectedDialog();
    if (dialog != null &&
        dialog.peerKind == model.reader.historyPeerKind &&
        dialog.peerId == model.reader.historyPeerId &&
        dialog.readOutboxMaxId >= item.messageId) {
      return "read";
    }
    return "sent";
  }

  private void ensureLayout() {
    Font senderFont = metrics.titleFont;
    Font bodyFont = metrics.bodyFont;
    Font metaFont = metrics.metaFont;
    int maxBubbleWidth = metrics.contentWidth - 20;
    int y = 0;

    if (layoutWidth == metrics.contentWidth) {
      return;
    }

    for (int i = 0; i < model.reader.history.count && i < messageTop.length; ++i) {
      MessageListModel.Item item = model.reader.history.items[i];
      String messageStatus = messageStatus(item);
      int lineCount = ShellTextWrap.wrap(item.text, bodyFont, maxBubbleWidth - 12, wrapStarts, wrapLengths);
      String mediaSummary = item.mediaSummary();
      int mediaLineCount = item.hasMedia && mediaSummary.length() > 0
        ? ShellTextWrap.wrap(mediaSummary, metaFont, maxBubbleWidth - 12, mediaWrapStarts, mediaWrapLengths)
        : 0;
      int textWidth = ShellTextWrap.widestLine(item.text, bodyFont, wrapStarts, wrapLengths, lineCount);
      int senderWidth = senderFont.stringWidth(item.sender);
      int mediaWidth = mediaLineCount > 0
        ? ShellTextWrap.widestLine(mediaSummary, metaFont, mediaWrapStarts, mediaWrapLengths, mediaLineCount)
        : 0;
      int messageStatusWidth = messageStatus.length() > 0 ? metaFont.stringWidth(messageStatus) : 0;
      int timeWidth = item.dateSeconds > 0 ? metaFont.stringWidth("00:00Z") : 0;
      int statusWidth = messageStatusWidth;
      int metaWidth;

      if (messageStatusWidth > 0 && timeWidth > 0) {
        statusWidth += 8 + timeWidth;
      } else if (timeWidth > statusWidth) {
        statusWidth = timeWidth;
      }
      metaWidth = mediaWidth > statusWidth ? mediaWidth : statusWidth;
      int widest = textWidth;

      if (senderWidth > widest) {
        widest = senderWidth;
      }
      if (metaWidth > widest) {
        widest = metaWidth;
      }

      messageTop[i] = y;
      messageWidth[i] = widest + 16;
      if (messageWidth[i] < ((metrics.contentWidth * 3) / 5)) {
        messageWidth[i] = (metrics.contentWidth * 3) / 5;
      }
      if (messageWidth[i] > maxBubbleWidth) {
        messageWidth[i] = maxBubbleWidth;
      }
      messageHeight[i] = 10 + senderFont.getHeight() +
        (lineCount * bodyFont.getHeight()) +
        (mediaLineCount * metaFont.getHeight()) +
        ((messageStatus.length() > 0 || item.dateSeconds > 0) ? metaFont.getHeight() : 0) + 8;
      y += messageHeight[i] + 6;
    }
    contentHeight = y;
    layoutWidth = metrics.contentWidth;
  }

  private void ensureSelectedVisible() {
    int viewportHeight = viewportHeight();
    int index = model.reader.history.selectedIndex;

    ensureLayout();
    if (index < 0 || index >= model.reader.history.count) {
      return;
    }

    if (messageTop[index] < scrollY) {
      scrollY = messageTop[index];
    } else if (messageTop[index] + messageHeight[index] > scrollY + viewportHeight) {
      scrollY = messageTop[index] + messageHeight[index] - viewportHeight;
    }
    normalizeScroll(viewportHeight);
  }

  private void normalizeScroll(int viewportHeight) {
    int maxScroll = contentHeight - viewportHeight;

    if (scrollY < 0) {
      scrollY = 0;
    }
    if (maxScroll > 0 && scrollY > maxScroll) {
      scrollY = maxScroll;
    }
    if (maxScroll <= 0) {
      scrollY = 0;
    }
  }

  private void pageBy(int amount) {
    scrollY += amount;
    normalizeScroll(viewportHeight());
  }

  private int viewportHeight() {
    return getHeight() - (metrics.titleFont.getHeight() + metrics.bodyFont.getHeight() + 16) -
      (metrics.metaFont.getHeight() + 8) - 8;
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
    g.drawString(text, metrics.contentX, height - footerHeight + 3, Graphics.TOP | Graphics.LEFT);
  }

  private void drawEmptyStateCard(Graphics g, int width, int height, String title, String detail) {
    Font headerFont = metrics.titleFont;
    Font bodyFont = metrics.bodyFont;
    int cardWidth = metrics.contentWidth;
    int cardHeight = headerFont.getHeight() + (bodyFont.getHeight() * 3) + 16;
    int cardX = metrics.contentX;
    int cardY = (height / 2) - (cardHeight / 2) - 12;
    int lineCount;

    ShellTheme.fillBevelBox(
      g,
      cardX,
      cardY,
      cardWidth,
      cardHeight,
      ShellTheme.PANEL_BOTTOM,
      ShellTheme.PANEL_HILITE,
      ShellTheme.PANEL_SHADOW
    );
    g.setFont(headerFont);
    g.setColor(ShellTheme.TEXT_PRIMARY);
    g.drawString(title, cardX + 10, cardY + 8, Graphics.TOP | Graphics.LEFT);
    g.setFont(bodyFont);
    g.setColor(ShellTheme.TEXT_MUTED);
    lineCount = ShellTextWrap.wrap(detail, bodyFont, cardWidth - 20, wrapStarts, wrapLengths);
    for (int line = 0; line < lineCount && line < 3; ++line) {
      if (wrapLengths[line] > 0) {
        g.drawSubstring(
          detail,
          wrapStarts[line],
          wrapLengths[line],
          cardX + 10,
          cardY + 12 + headerFont.getHeight() + (line * bodyFont.getHeight()),
          Graphics.TOP | Graphics.LEFT
        );
      }
    }
  }

  private void invalidateLayout() {
    layoutWidth = -1;
  }

  private int safeGameAction(int keyCode) {
    try {
      return getGameAction(keyCode);
    } catch (IllegalArgumentException e) {
      return 0;
    }
  }
}
