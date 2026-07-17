package ui;

import javax.microedition.lcdui.Font;

final class ShellTextWrap {
  private ShellTextWrap() {
  }

  static int wrap(String text, Font font, int maxWidth, int[] starts, int[] lengths) {
    int len;
    int lineStart;
    int lineWidth;
    int lastBreak;
    int count = 0;

    if (starts == null || lengths == null || starts.length == 0 || lengths.length == 0) {
      return 0;
    }
    if (text == null || text.length() == 0) {
      starts[0] = 0;
      lengths[0] = 0;
      return 1;
    }

    len = text.length();
    lineStart = 0;
    lineWidth = 0;
    lastBreak = -1;

    for (int i = 0; i < len; ++i) {
      char ch = text.charAt(i);
      int width;

      if (ch == '\n') {
        count = pushLine(lineStart, trimEnd(text, lineStart, i), starts, lengths, count);
        if (count >= starts.length) {
          return count;
        }
        lineStart = i + 1;
        lineWidth = 0;
        lastBreak = -1;
        continue;
      }

      width = font.charWidth(ch);
      if (ch == ' ') {
        lastBreak = i;
      }

      if (lineWidth + width > maxWidth && i > lineStart) {
        int breakAt = i;
        int nextStart = i;

        if (lastBreak >= lineStart) {
          breakAt = lastBreak;
          nextStart = lastBreak + 1;
          while (nextStart < len && text.charAt(nextStart) == ' ') {
            nextStart += 1;
          }
        }

        count = pushLine(lineStart, trimEnd(text, lineStart, breakAt), starts, lengths, count);
        if (count >= starts.length) {
          return count;
        }
        lineStart = nextStart;
        i = nextStart - 1;
        lineWidth = 0;
        lastBreak = -1;
        continue;
      }

      if (lineWidth + width > maxWidth && i == lineStart) {
        count = pushLine(i, i + 1, starts, lengths, count);
        if (count >= starts.length) {
          return count;
        }
        lineStart = i + 1;
        lineWidth = 0;
        lastBreak = -1;
        continue;
      }

      lineWidth += width;
    }

    count = pushLine(lineStart, trimEnd(text, lineStart, len), starts, lengths, count);
    return count > 0 ? count : 1;
  }

  static int widestLine(String text, Font font, int[] starts, int[] lengths, int count) {
    int widest = 0;

    for (int i = 0; i < count; ++i) {
      int width = lengths[i] > 0 ? font.substringWidth(text, starts[i], lengths[i]) : 0;
      if (width > widest) {
        widest = width;
      }
    }
    return widest;
  }

  private static int pushLine(int start, int end, int[] starts, int[] lengths, int count) {
    if (count >= starts.length || count >= lengths.length) {
      return count;
    }
    starts[count] = start;
    lengths[count] = end >= start ? end - start : 0;
    return count + 1;
  }

  private static int trimEnd(String text, int start, int end) {
    int out = end;

    while (out > start && text.charAt(out - 1) == ' ') {
      out -= 1;
    }
    return out;
  }
}
