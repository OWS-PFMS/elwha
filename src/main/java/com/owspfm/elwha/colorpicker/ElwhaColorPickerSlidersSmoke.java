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

/**
 * S4 headless guard for the SLIDERS pane (#486): ColorHex parsing grammar (6-digit, shorthand,
 * '#'-optional, alpha forms gated), RGB and HSV channel commits through the picker, the HSV rows'
 * hue preservation, hex commit/error/revert behavior, model toggling, slider resync on external
 * commits, and a light+dark paint pass asserting each RGB track's full-channel end reaches pixels.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSlidersSmoke {

  private ElwhaColorPickerSlidersSmoke() {}

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

    checkParse();
    checkChannels();
    checkHexField();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(
          ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      checkPaint(mode);
    }

    System.out.println(
        "ElwhaColorPickerSlidersSmoke: OK (parse, channels, hex field, light+dark paint)");
  }

  private static void checkParse() {
    check("six-digit", new Color(0xFF0000).equals(ColorHex.parse("#FF0000", false)));
    check("hash optional", new Color(0xFF0000).equals(ColorHex.parse("ff0000", false)));
    check("shorthand expands", new Color(0xFF0000).equals(ColorHex.parse("#f00", false)));
    check("garbage rejected", ColorHex.parse("nope", false) == null);
    check("empty rejected", ColorHex.parse("  ", false) == null);
    check("null rejected", ColorHex.parse(null, false) == null);
    check("alpha rejected when gated", ColorHex.parse("#11223344", false) == null);
    check(
        "alpha parsed when allowed",
        new Color(0x11, 0x22, 0x33, 0x44).equals(ColorHex.parse("#11223344", true)));
    check(
        "shorthand alpha when allowed",
        new Color(0xFF, 0x00, 0x00, 0xFF).equals(ColorHex.parse("#f00f", true)));
  }

  private static void checkChannels() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x336699));
    picker.setModes(PickerMode.SLIDERS);
    final SlidersPane pane = (SlidersPane) picker.paneFor(PickerMode.SLIDERS);

    check(
        "rgb rows read the color",
        pane.rgbValue(0) == 0x33 && pane.rgbValue(1) == 0x66 && pane.rgbValue(2) == 0x99);

    pane.rgbFromUser(0, 255, false);
    check("red channel commit", new Color(0xFF6699).equals(picker.getColor()));

    pane.setChannelModel(SlidersPane.ChannelModel.HSV);
    check("model toggles", pane.getChannelModel() == SlidersPane.ChannelModel.HSV);

    picker.setColor(new Color(0x00FF00));
    check("hsv rows resync", pane.hsvValue(0) == 120 && pane.hsvValue(1) == 100);

    pane.hsvFromUser(2, 0, false);
    check("value channel reaches black", Color.BLACK.equals(picker.getColor()));
    pane.hsvFromUser(2, 100, false);
    check("hue preserved through black", new Color(0x00FF00).equals(picker.getColor()));

    pane.hsvFromUser(1, 50, true);
    check("drag reports adjusting", picker.isAdjusting());
    pane.hsvFromUser(1, 50, false);
    check("release settles", !picker.isAdjusting());
  }

  private static void checkHexField() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x336699));
    picker.setModes(PickerMode.SLIDERS);
    final SlidersPane pane = (SlidersPane) picker.paneFor(PickerMode.SLIDERS);

    check("hex field mirrors the color", "#336699".equals(pane.hexText()));

    pane.commitHexText("0f0");
    check("hex commit applies", new Color(0x00FF00).equals(picker.getColor()));
    check("valid hex clears error", !pane.isHexError());
    check("hex text normalized", "#00FF00".equals(pane.hexText()));

    pane.commitHexText("garbage");
    check("invalid hex raises error", pane.isHexError());
    check("invalid hex never commits", new Color(0x00FF00).equals(picker.getColor()));

    pane.revertHex();
    check("revert restores text", "#00FF00".equals(pane.hexText()));
    check("revert clears error", !pane.isHexError());

    picker.setColor(new Color(0xD32F2F));
    check("external commit refreshes hex", "#D32F2F".equals(pane.hexText()));
  }

  private static void checkPaint(final Mode mode) {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.BLACK);
    picker.setModes(PickerMode.SLIDERS);
    layoutTree(picker, new Dimension(360, 480));
    final BufferedImage image = new BufferedImage(360, 480, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, 360, 480);
    picker.paint(g2);
    g2.dispose();
    check("red track end painted (" + mode + ")", contains(image, new Color(0xFF0000)));
    check("green track end painted (" + mode + ")", contains(image, new Color(0x00FF00)));
    check("blue track end painted (" + mode + ")", contains(image, new Color(0x0000FF)));
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
