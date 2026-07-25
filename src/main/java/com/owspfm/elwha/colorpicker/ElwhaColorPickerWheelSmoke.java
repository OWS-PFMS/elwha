package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.icons.MaterialIcons;
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
 * V2 S1 headless guard for the WHEEL pane (#497): the four-mode default set and `colors` icon
 * assets, polar commits through the picker, the hue-preservation invariant across the desaturated
 * center and black value floor, hue wrap-around, adjusting semantics, the real press/drag/release
 * gesture path on the disc (the V1 shadowing lesson — handlers must track live state, not
 * construction state), and a light+dark paint pass asserting the disc's white center and the
 * value-0 black overlay reach pixels.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerWheelSmoke {

  private ElwhaColorPickerWheelSmoke() {}

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

    checkModeSet();
    checkPane();
    checkGesture();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      checkPaint(mode);
    }

    System.out.println(
        "ElwhaColorPickerWheelSmoke: OK (mode set, hue invariant, gesture path, light+dark"
            + " paint)");
  }

  private static void checkModeSet() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    check("default offers four modes", picker.getModes().size() == 4);
    check("WHEEL sits between SPECTRUM and SLIDERS", picker.getModes().get(2) == PickerMode.WHEEL);
    picker.setMode(PickerMode.WHEEL);
    check("WHEEL activates", picker.getMode() == PickerMode.WHEEL);
    check("wheel pane present", picker.paneFor(PickerMode.WHEEL) instanceof WheelPane);
    check("colors factory loads", MaterialIcons.colors() != null);
    check("colors fill variant bundled", MaterialIcons.symbol("colors").hasSelectedVariant());
  }

  private static void checkPane() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF0000));
    picker.setModes(PickerMode.WHEEL);
    final WheelPane pane = (WheelPane) picker.paneFor(PickerMode.WHEEL);
    check(
        "red adopts h=0 s=1 v=1",
        pane.hueDegrees() == 0f && pane.saturation() == 1f && pane.value() == 1f);

    pane.hueSatTo(120f, 1f, false);
    check("polar commit produces pure green", new Color(0x00FF00).equals(picker.getColor()));

    pane.hueSatTo(120f, 0f, false);
    check("desaturated center reaches white", Color.WHITE.equals(picker.getColor()));
    check("hue survives the center", pane.hueDegrees() == 120f);

    pane.hueSatTo(120f, 1f, false);
    check("hue resumes after the center", new Color(0x00FF00).equals(picker.getColor()));

    pane.valueTo(0, false);
    check("value floor reaches black", Color.BLACK.equals(picker.getColor()));
    check("hue and saturation survive black", pane.hueDegrees() == 120f && pane.saturation() == 1f);
    pane.valueTo(100, false);
    check("value resumes to green", new Color(0x00FF00).equals(picker.getColor()));

    picker.setColor(new Color(0x808080));
    check("external grey keeps the hue", pane.hueDegrees() == 120f);
    check("external grey adopts s=0", pane.saturation() == 0f);

    pane.hueSatTo(365f, 1f, false);
    check("hue wraps past 360", pane.hueDegrees() == 5f);
    pane.hueSatTo(-10f, 1f, false);
    check("hue wraps below 0", pane.hueDegrees() == 350f);

    pane.hueSatTo(200f, 0.5f, true);
    check("drag reports adjusting", picker.isAdjusting());
    pane.hueSatTo(200f, 0.5f, false);
    check("release settles", !picker.isAdjusting());

    picker.setColor(new Color(0x0000FF));
    check("external blue resyncs hue 240", Math.round(pane.hueDegrees()) == 240);
  }

  private static void checkGesture() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF0000));
    picker.setModes(PickerMode.WHEEL);
    layoutTree(picker, new Dimension(360, 460));
    final WheelPane pane = (WheelPane) picker.paneFor(PickerMode.WHEEL);
    final Component disc = pane.getComponent(0);
    final int cx = disc.getWidth() / 2;
    final int cy = WheelPane.DISC_DIAMETER / 2;

    dispatchMouse(disc, java.awt.event.MouseEvent.MOUSE_PRESSED, cx + 30, cy);
    check("press reports adjusting", picker.isAdjusting());
    dispatchMouse(disc, java.awt.event.MouseEvent.MOUSE_DRAGGED, cx, cy - 50);
    check("drag lands near 90° hue", Math.abs(pane.hueDegrees() - 90f) < 1.5f);
    final float draggedHue = pane.hueDegrees();
    final float draggedSat = pane.saturation();
    check("drag saturation tracks the radius", Math.abs(draggedSat - 50f / 73f) < 0.02f);
    dispatchMouse(disc, java.awt.event.MouseEvent.MOUSE_RELEASED, cx, cy - 50);
    check(
        "release settles at the dragged point, not the construction point",
        !picker.isAdjusting()
            && pane.hueDegrees() == draggedHue
            && pane.saturation() == draggedSat);

    dispatchMouse(disc, java.awt.event.MouseEvent.MOUSE_PRESSED, cx + 500, cy);
    check("outside press clamps saturation to 1", pane.saturation() == 1f);
    dispatchMouse(disc, java.awt.event.MouseEvent.MOUSE_RELEASED, cx + 500, cy);
  }

  private static void dispatchMouse(
      final Component target, final int id, final int x, final int y) {
    target.dispatchEvent(
        new java.awt.event.MouseEvent(
            target, id, 0L, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1));
  }

  private static void checkPaint(final Mode mode) {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF0000));
    picker.setModes(PickerMode.WHEEL);
    layoutTree(picker, new Dimension(360, 460));
    final WheelPane pane = (WheelPane) picker.paneFor(PickerMode.WHEEL);
    final Component disc = pane.getComponent(0);
    final java.awt.Point discAt = javax.swing.SwingUtilities.convertPoint(disc, 0, 0, picker);
    final int cx = discAt.x + disc.getWidth() / 2;
    final int cy = discAt.y + WheelPane.DISC_DIAMETER / 2;

    final BufferedImage image = paint(picker);
    check(
        "disc center paints white at v=1 (" + mode + ")",
        nearly(new Color(image.getRGB(cx, cy)), Color.WHITE));

    pane.valueTo(0, false);
    final BufferedImage dark = paint(picker);
    check(
        "value floor paints the disc black (" + mode + ")",
        nearly(new Color(dark.getRGB(cx, cy)), Color.BLACK));
  }

  private static BufferedImage paint(final ElwhaColorPicker picker) {
    final BufferedImage image =
        new BufferedImage(picker.getWidth(), picker.getHeight(), BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, image.getWidth(), image.getHeight());
    picker.paint(g2);
    g2.dispose();
    return image;
  }

  private static boolean nearly(final Color actual, final Color expected) {
    return Math.abs(actual.getRed() - expected.getRed()) <= 3
        && Math.abs(actual.getGreen() - expected.getGreen()) <= 3
        && Math.abs(actual.getBlue() - expected.getBlue()) <= 3;
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
