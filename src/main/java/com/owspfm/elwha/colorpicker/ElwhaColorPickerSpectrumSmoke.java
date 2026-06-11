package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * S3 headless guard for the SPECTRUM pane (#485): ColorTrackSlider clamping and
 * user-vs-programmatic notification rules, hue/point commits through the picker, the
 * hue-preservation invariant across greys and external grey commits, adjusting semantics, hue
 * slider resync, and a light+dark paint pass asserting the SV square's pure-hue corner and black
 * floor reach pixels.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSpectrumSmoke {

  private ElwhaColorPickerSpectrumSmoke() {}

  /**
   * Runs the guard.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkSlider();
    checkPane();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      checkPaint(mode);
    }

    System.out.println(
        "ElwhaColorPickerSpectrumSmoke: OK (slider, hue invariant, adjusting, light+dark paint)");
  }

  private static void checkSlider() {
    final ColorTrackSlider slider = new ColorTrackSlider(0, 360, 400);
    check("constructor clamps", slider.value() == 360);

    final AtomicInteger fires = new AtomicInteger();
    final int[] last = new int[2];
    slider.setListener(
        (value, adjusting) -> {
          fires.incrementAndGet();
          last[0] = value;
          last[1] = adjusting ? 1 : 0;
        });

    slider.userSet(-5, false);
    check("userSet clamps low and notifies", slider.value() == 0 && fires.get() == 1);
    check("settled notification", last[1] == 0);

    slider.userSet(20, true);
    check("adjusting notification", fires.get() == 2 && last[0] == 20 && last[1] == 1);
    slider.userSet(20, true);
    check("unchanged adjusting value stays quiet", fires.get() == 2);
    slider.userSet(20, false);
    check("release always settles", fires.get() == 3 && last[1] == 0);

    slider.setValue(100);
    check("programmatic setValue never notifies", fires.get() == 3 && slider.value() == 100);
  }

  private static void checkPane() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF0000));
    picker.setModes(PickerMode.SPECTRUM);
    final SpectrumPane pane = (SpectrumPane) picker.paneFor(PickerMode.SPECTRUM);
    check(
        "red adopts h=0 s=1 v=1",
        pane.hueDegrees() == 0f && pane.saturation() == 1f && pane.value() == 1f);

    pane.hueTo(120, false);
    check("hue commit produces pure green", new Color(0x00FF00).equals(picker.getColor()));

    pane.pointTo(0f, 1f, false);
    check("desaturating reaches white", Color.WHITE.equals(picker.getColor()));
    check("hue survives the grey axis", pane.hueDegrees() == 120f);

    pane.pointTo(1f, 1f, false);
    check("hue resumes after grey", new Color(0x00FF00).equals(picker.getColor()));

    picker.setColor(new Color(0x808080));
    check("external grey keeps the hue", pane.hueDegrees() == 120f);
    check("external grey adopts s=0", pane.saturation() == 0f);

    pane.pointTo(1f, pane.value(), false);
    final float[] hsb = new float[3];
    final Color resumed = picker.getColor();
    Color.RGBtoHSB(resumed.getRed(), resumed.getGreen(), resumed.getBlue(), hsb);
    check("resaturating resumes hue 120", Math.round(hsb[0] * 360f) == 120);

    pane.pointTo(0.5f, 0.5f, true);
    check("drag reports adjusting", picker.isAdjusting());
    pane.pointTo(0.5f, 0.5f, false);
    check("release settles", !picker.isAdjusting());

    picker.setColor(new Color(0x0000FF));
    check("hue slider resyncs to external blue", Math.round(pane.hueDegrees()) == 240);
  }

  private static void checkPaint(final Mode mode) {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF0000));
    picker.setModes(PickerMode.SPECTRUM);
    layoutTree(picker, new Dimension(360, 460));
    final BufferedImage image = new BufferedImage(360, 460, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, 360, 460);
    picker.paint(g2);
    g2.dispose();
    check("pure-hue corner painted (" + mode + ")", contains(image, new Color(0xFF0000)));
    check("black value floor painted (" + mode + ")", contains(image, Color.BLACK));
  }

  private static boolean contains(final BufferedImage image, final Color target) {
    final int rgb = target.getRGB();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (image.getRGB(x, y) == rgb) {
          return true;
        }
      }
    }
    return false;
  }

  private static void layoutTree(final Component component, final Dimension size) {
    component.setSize(size);
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutTree(child, child.getSize());
      }
    }
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
