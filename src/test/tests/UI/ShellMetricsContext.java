package test;

import ui.ShellMetrics;

public class ShellMetricsContext {
  public static class WideHostedCanvasUsesPhoneViewportTest extends Test {
    public String label() {
      return "Hosted canvas widths clamp to a 240px phone viewport";
    }

    public void test() throws TestFailureException {
      ShellMetrics metrics = ShellMetrics.forCanvas(480);

      expect(metrics.frameWidth, 240);
      expect(metrics.frameX, 120);
      expect(metrics.contentWidth, 228);
      expect(metrics.contentX, 126);
    }
  }

  public static class NarrowCanvasKeepsNativeWidthTest extends Test {
    public String label() {
      return "Native phone-sized canvases keep their own width and compact padding";
    }

    public void test() throws TestFailureException {
      ShellMetrics metrics = ShellMetrics.forCanvas(176);

      expect(metrics.frameWidth, 176);
      expect(metrics.frameX, 0);
      expect(metrics.edgePadding, 4);
      expect(metrics.contentWidth, 168);
      expect(metrics.titleFont.getHeight() >= metrics.bodyFont.getHeight(), true);
    }
  }
}
