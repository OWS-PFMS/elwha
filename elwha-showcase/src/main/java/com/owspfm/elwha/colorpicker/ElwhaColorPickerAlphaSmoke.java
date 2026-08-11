package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S6 headless guard for the alpha channel (#488): the opt-in gate (off strips, on preserves,
 * turning off strips the current color with a change commit), the 8-digit hex readout and headline
 * step-down, alpha edits flowing through the spectrum and sliders panes, hex alpha grammar + error
 * text, swatch picks preserving alpha, dialog pass-through, and a paint pass asserting the alpha
 * track's opaque end reaches pixels.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerAlphaSmoke {

  private ElwhaColorPickerAlphaSmoke() {}

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

    checkGate();
    checkPanes();
    checkDialog();
    checkPaint();

    System.out.println("ElwhaColorPickerAlphaSmoke: OK (gate, panes, hex grammar, paint)");
  }

  private static void checkGate() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    check("alpha off by default", !picker.isAlphaEnabled());
    picker.setColor(new Color(10, 20, 30, 77));
    check("off strips alpha", picker.getColor().getAlpha() == 255);

    picker.setAlphaEnabled(true);
    picker.setColor(new Color(10, 20, 30, 77));
    check("on preserves alpha", picker.getColor().getAlpha() == 77);
    check("readout grows to 8 digits", "#0A141E4D".equals(picker.formatCurrentHex()));
    check("headline steps down", picker.headlineTypeRole() == TypeRole.HEADLINE_MEDIUM);

    final int[] fires = {0};
    picker.addChangeListener(e -> fires[0]++);
    picker.setAlphaEnabled(false);
    check(
        "turning off strips with a change commit",
        picker.getColor().getAlpha() == 255 && fires[0] == 1);
    check("readout shrinks back", "#0A141E".equals(picker.formatCurrentHex()));
    check("headline restores", picker.headlineTypeRole() == TypeRole.HEADLINE_LARGE);
  }

  private static void checkPanes() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xF4, 0x43, 0x36, 0x80));
    picker.setAlphaEnabled(true);
    picker.setColor(new Color(0xF4, 0x43, 0x36, 0x80));

    final SpectrumPane spectrum = (SpectrumPane) picker.paneFor(PickerMode.SPECTRUM);
    spectrum.pointTo(1f, 1f, false);
    check("spectrum point preserves alpha", picker.getColor().getAlpha() == 0x80);
    spectrum.alphaTo(40, false);
    check("spectrum alpha slider commits", picker.getColor().getAlpha() == 40);

    final SlidersPane sliders = (SlidersPane) picker.paneFor(PickerMode.SLIDERS);
    sliders.rgbFromUser(0, 10, false);
    check("rgb edit preserves alpha", picker.getColor().getAlpha() == 40);
    sliders.alphaFromUser(200, false);
    check("sliders alpha row commits", picker.getColor().getAlpha() == 200);
    check("alpha row reads back", sliders.alphaValue() == 200);

    sliders.commitHexText("#11223344");
    check("hex alpha commits", new Color(0x11, 0x22, 0x33, 0x44).equals(picker.getColor()));
    check("hex text mirrors 8 digits", "#11223344".equals(sliders.hexText()));
    sliders.commitHexText("#123456789");
    check("bad alpha hex raises error", sliders.isHexError());

    sliders.revertHex();
    final SwatchesPane swatches = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    swatches.selectHue(0);
    check(
        "swatch pick preserves alpha",
        picker.getColor().getAlpha() == 0x44
            && (picker.getColor().getRGB() & 0xFFFFFF) == 0xF44336);
  }

  private static void checkDialog() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    dialog.setAlphaEnabled(true);
    check("dialog passes alpha through", dialog.picker().isAlphaEnabled());
    dialog.setInitialColor(new Color(1, 2, 3, 4));
    dialog.stage();
    check("dialog stages translucent colors", dialog.picker().getColor().getAlpha() == 4);
  }

  private static void checkPaint() {
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      final ElwhaColorPicker picker = new ElwhaColorPicker();
      picker.setAlphaEnabled(true);
      picker.setColor(new Color(0xD3, 0x2F, 0x2F, 0x60));
      picker.setMode(PickerMode.SLIDERS);
      layoutTree(picker, new Dimension(360, 520));
      final BufferedImage image = new BufferedImage(360, 520, BufferedImage.TYPE_INT_RGB);
      final Graphics2D g2 = image.createGraphics();
      g2.setColor(Color.GRAY);
      g2.fillRect(0, 0, 360, 520);
      picker.paint(g2);
      g2.dispose();
      check("alpha track opaque end painted (" + mode + ")", contains(image, new Color(0xD32F2F)));
    }
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
