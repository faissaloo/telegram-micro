package ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import reader.DialogListModel;

final class DialogsCanvas extends Canvas {
  private final AppShellModel model;
  private final AppShellActions actions;
  private final int[] wrapStarts = new int[4];
  private final int[] wrapLengths = new int[4];
  private ShellMetrics metrics = ShellMetrics.forCanvas(ShellMetrics.TARGET_PHONE_WIDTH);
  private int scrollY = 0;

  DialogsCanvas(AppShellModel model, AppShellActions actions) {
    this.model = model;
    this.actions = actions;
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

    if (gameAction == Canvas.UP) {
      if (model.focusPreviousDialog()) {
        ensureSelectionVisible();
        repaint();
      }
      return;
    }
    if (gameAction == Canvas.DOWN) {
      if (model.focusNextDialog()) {
        ensureSelectionVisible();
        repaint();
      }
      return;
    }
    if ((gameAction == Canvas.FIRE || keyCode == KEY_NUM5) &&
        actions != null &&
        model.reader.dialogs.count > 0) {
      actions.openDialog(model.reader.dialogs.selectedIndex);
    }
  }

  protected void paint(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    Font titleFont = metrics.titleFont;
    Font bodyFont = metrics.bodyFont;
    Font badgeFont = metrics.badgeFont;
    Font footerFont = metrics.metaFont;
    int headerHeight = titleFont.getHeight() + bodyFont.getHeight() + 14;
    int footerHeight = footerFont.getHeight() + 8;
    int viewportTop = headerHeight + 4;
    int viewportHeight = height - viewportTop - footerHeight - 4;
    int rowHeight = rowHeight();
    int selectedIndex = model.reader.dialogs.selectedIndex;
    int x = metrics.contentX;
    int cardWidth = metrics.contentWidth;

    ShellTheme.fillVerticalGradient(g, 0, 0, width, height, ShellTheme.BG_TOP, ShellTheme.BG_BOTTOM, 10);
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
    g.setColor(ShellTheme.TEXT_PRIMARY);
    g.setFont(titleFont);
    g.drawString("Dialogs", x, 6, Graphics.TOP | Graphics.LEFT);
    g.setFont(bodyFont);
    g.setColor(ShellTheme.TEXT_MUTED);
    g.drawString(model.readerSummary, x, 8 + titleFont.getHeight(), Graphics.TOP | Graphics.LEFT);

    if (model.reader.loadingDialogs) {
      g.setColor(ShellTheme.TEXT_PRIMARY);
      drawEmptyStateCard(g, width, height, "Loading dialogs...", "Waiting for Telegram to return the current inbox window.");
      drawFooter(g, height, footerHeight, "Waiting for Telegram");
      return;
    }
    if (model.reader.dialogs.count == 0) {
      drawEmptyStateCard(g, width, height, "No dialogs yet.", "Use Refresh after authorization to fetch the current dialog list.");
      drawFooter(g, height, footerHeight, "Refresh  Status  Exit");
      return;
    }

    for (int i = 0; i < model.reader.dialogs.count; ++i) {
      DialogListModel.Item item = model.reader.dialogs.items[i];
      int rowY = viewportTop + (i * rowHeight) - scrollY;
      int previewLines;
      int badgeWidth;

      if (rowY + rowHeight < viewportTop || rowY > viewportTop + viewportHeight) {
        continue;
      }

      ShellTheme.fillBevelBox(
        g,
        x,
        rowY,
        cardWidth,
        rowHeight - 4,
        i == selectedIndex ? ShellTheme.CARD_FILL_SELECTED : ShellTheme.CARD_FILL,
        0xffffff,
        0x6f879d
      );

      g.setFont(titleFont);
      g.setColor(i == selectedIndex ? ShellTheme.SELECT_TEXT : ShellTheme.CARD_TEXT);
      g.drawString(item.title, x + metrics.innerPadding, rowY + 5, Graphics.TOP | Graphics.LEFT);

      g.setFont(bodyFont);
      previewLines = ShellTextWrap.wrap(
        item.preview,
        bodyFont,
        cardWidth - (metrics.innerPadding * 2) - 28,
        wrapStarts,
        wrapLengths
      );
      g.setColor(ShellTheme.CARD_PREVIEW);
      for (int line = 0; line < previewLines && line < 2; ++line) {
        if (wrapLengths[line] > 0) {
          g.drawSubstring(
            item.preview,
            wrapStarts[line],
            wrapLengths[line],
            x + metrics.innerPadding,
            rowY + 7 + titleFont.getHeight() + (line * bodyFont.getHeight()),
            Graphics.TOP | Graphics.LEFT
          );
        }
      }

      if (item.unreadCount > 0) {
        String badge = Integer.toString(item.unreadCount);
        g.setFont(badgeFont);
        badgeWidth = badgeFont.stringWidth(badge) + 12;
        ShellTheme.drawBadge(
          g,
          x + cardWidth - badgeWidth - metrics.innerPadding,
          rowY + 6,
          badgeWidth,
          badgeFont.getHeight() + 4,
          ShellTheme.BADGE_FILL,
          ShellTheme.BADGE_TEXT,
          badge
        );
      }

      if (item.hasMedia) {
        g.setFont(bodyFont);
        g.setColor(ShellTheme.TEXT_SOFT);
        g.drawString(
          "media",
          x + cardWidth - bodyFont.stringWidth("media") - metrics.innerPadding,
          rowY + rowHeight - bodyFont.getHeight() - 8,
          Graphics.TOP | Graphics.LEFT
        );
      }
    }

    drawFooter(g, height, footerHeight, Integer.toString(selectedIndex + 1) + "/" +
      Integer.toString(model.reader.dialogs.count) + "  5 Open");
  }

  private int rowHeight() {
    return metrics.titleFont.getHeight() + (metrics.bodyFont.getHeight() * 2) + 18;
  }

  private void drawEmptyStateCard(Graphics g, int width, int height, String title, String detail) {
    Font titleFont = metrics.titleFont;
    Font bodyFont = metrics.bodyFont;
    int cardWidth = metrics.contentWidth;
    int cardHeight = titleFont.getHeight() + (bodyFont.getHeight() * 3) + 16;
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
    g.setFont(titleFont);
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
          cardY + 12 + titleFont.getHeight() + (line * bodyFont.getHeight()),
          Graphics.TOP | Graphics.LEFT
        );
      }
    }
  }

  private void ensureSelectionVisible() {
    int viewportHeight = getHeight() - (metrics.titleFont.getHeight() + metrics.bodyFont.getHeight() + 14) -
      (metrics.metaFont.getHeight() + 8) - 8;
    int rowHeight = rowHeight();
    int selectedTop = model.reader.dialogs.selectedIndex * rowHeight;
    int selectedBottom = selectedTop + rowHeight;
    int maxScroll = (model.reader.dialogs.count * rowHeight) - viewportHeight;

    if (selectedTop < scrollY) {
      scrollY = selectedTop;
    } else if (selectedBottom > scrollY + viewportHeight) {
      scrollY = selectedBottom - viewportHeight;
    }
    if (scrollY < 0) {
      scrollY = 0;
    }
    if (maxScroll > 0 && scrollY > maxScroll) {
      scrollY = maxScroll;
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
    g.drawString(text, metrics.contentX, height - footerHeight + 3, Graphics.TOP | Graphics.LEFT);
  }

  private int safeGameAction(int keyCode) {
    try {
      return getGameAction(keyCode);
    } catch (IllegalArgumentException e) {
      return 0;
    }
  }
}
