package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.swing.Action;

/**
 * Headless guard for S4 (story #345). Asserts stops snapping (set + arrow), the value formatter,
 * the stop-indicator dot colors (active = {@link ColorRole#ON_PRIMARY}, inactive = {@link
 * ColorRole#ON_SECONDARY_CONTAINER}) sampled from an offscreen render, and the continuous-mode
 * contrast end stop. No display required.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderStopsSmoke {

  private ElwhaSliderStopsSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setStops(10);

    slider.setValue(37);
    check("setValue snaps up to the nearest stop (37 → 40)", slider.getValue() == 40);
    slider.setValue(34);
    check("setValue snaps down to the nearest stop (34 → 30)", slider.getValue() == 30);

    fire(slider, "elwhaSlider.right");
    check("arrow steps by one stop (30 → 40)", slider.getValue() == 40);

    slider.setValueFormatter(v -> v + "%");
    check("value formatter applied", "40%".equals(slider.valueText()));

    // Stop-dot colors sampled from a render at value 50.
    slider.setValue(50);
    final int w = 240;
    final int h = ElwhaSlider.HANDLE_HEIGHT_PX;
    slider.setSize(w, h);
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      slider.paint(g);
    } finally {
      g.dispose();
    }
    final int cy = h / 2;
    final boolean activeDot =
        nearColor(img.getRGB(slider.xForValue(20), cy), ColorRole.ON_PRIMARY.resolve());
    final boolean inactiveDot =
        nearColor(img.getRGB(slider.xForValue(80), cy), ColorRole.ON_SECONDARY_CONTAINER.resolve());
    check("active-side stop dot is ON_PRIMARY", activeDot);
    check("inactive-side stop dot is ON_SECONDARY_CONTAINER", inactiveDot);

    // Continuous mode still paints a single end stop at the trailing inactive end.
    final ElwhaSlider cont = new ElwhaSlider(0, 100, 30);
    cont.setSize(w, h);
    final BufferedImage img2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = img2.createGraphics();
    try {
      cont.paint(g2);
    } finally {
      g2.dispose();
    }
    final boolean endStop =
        nearColor(img2.getRGB(cont.xForValue(100), cy), ColorRole.ON_SECONDARY_CONTAINER.resolve());
    check("continuous-mode end stop present", endStop);

    cont.setEndStopsVisible(false);
    final BufferedImage img3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g3 = img3.createGraphics();
    try {
      cont.paint(g3);
    } finally {
      g3.dispose();
    }
    final boolean stillEndStop =
        nearColor(img3.getRGB(cont.xForValue(100), cy), ColorRole.ON_SECONDARY_CONTAINER.resolve());
    check("end stop suppressed when disabled", !stillEndStop);

    System.out.println("ElwhaSliderStopsSmoke: OK (snap + formatter + stop-dot colors + end stop)");
  }

  private static void fire(final ElwhaSlider slider, final String key) {
    final Action a = slider.getActionMap().get(key);
    a.actionPerformed(new ActionEvent(slider, ActionEvent.ACTION_PERFORMED, key));
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 160) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed()) <= 8
        && Math.abs(c.getGreen() - target.getGreen()) <= 8
        && Math.abs(c.getBlue() - target.getBlue()) <= 8;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
