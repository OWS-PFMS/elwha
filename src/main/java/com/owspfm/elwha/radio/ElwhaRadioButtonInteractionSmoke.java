package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.KeyStroke;

/**
 * Headless guard for the S2 {@link ElwhaRadioButton} interaction surface (story #418). Drives
 * synthetic mouse events through {@code dispatchEvent} and the Space bindings through the action
 * map, asserting the select-only gesture contract ({@code ActionListener} on commit only, no
 * deselect, no re-fire, disabled inert) — then pixel-asserts the hover/focus/pressed state layers
 * including the M3 <strong>press swap</strong> ({@code PRIMARY} layer on an unselected press,
 * {@code ON_SURFACE} on a selected press) and the unselected ring's hover shift.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonInteractionSmoke {

  private static final int SIZE = ElwhaRadioButton.TOUCH_TARGET;
  private static final int CX = SIZE / 2;

  private ElwhaRadioButtonInteractionSmoke() {}

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

    checkClickContract();
    checkSpaceContract();
    checkDisabledInert();
    checkStateLayers();
    checkFocusVisibleGating();
    checkLabelClickTarget();

    System.out.println("ElwhaRadioButtonInteractionSmoke: OK (gesture contract + press swap)");
  }

  /**
   * Focus layer paints only for keyboard-caused focus — the checkbox/button-family gating. Focus
   * events synthesized through {@code dispatchEvent} are retargeted (and swallowed) by the
   * KeyboardFocusManager headlessly, so the registered listeners are driven directly.
   */
  private static void checkFocusVisibleGating() {
    final Color surface = ColorRole.SURFACE.resolve();
    final Color onSurface = ColorRole.ON_SURFACE.resolve();

    final ElwhaRadioButton radio = sized(new ElwhaRadioButton());
    focus(
        radio,
        java.awt.event.FocusEvent.FOCUS_GAINED,
        java.awt.event.FocusEvent.Cause.TRAVERSAL_FORWARD);
    check(
        "keyboard focus paints the FOCUS layer",
        near(
            layerSample(render(radio, surface)),
            mix(surface, onSurface, StateLayer.FOCUS.opacity())));
    focus(
        radio,
        java.awt.event.FocusEvent.FOCUS_LOST,
        java.awt.event.FocusEvent.Cause.TRAVERSAL_FORWARD);
    focus(
        radio, java.awt.event.FocusEvent.FOCUS_GAINED, java.awt.event.FocusEvent.Cause.MOUSE_EVENT);
    check(
        "pointer-caused focus paints no layer", near(layerSample(render(radio, surface)), surface));

    final ElwhaRadioButton stacked = sized(new ElwhaRadioButton());
    stacked.setHovered(true);
    stacked.setPressed(true);
    check(
        "one layer at a time: pressed wins over hover (no stacking)",
        near(
            layerSample(render(stacked, surface)),
            mix(surface, ColorRole.PRIMARY.resolve(), StateLayer.PRESSED.opacity())));
  }

  /** The attached label extends the click target — clicking the text selects. */
  private static void checkLabelClickTarget() {
    final ElwhaRadioButton radio = new ElwhaRadioButton("Click my label");
    final java.awt.Dimension pref = radio.getPreferredSize();
    radio.setSize(pref);
    final int labelX = ElwhaRadioButton.TOUCH_TARGET + 10;
    final int cy = pref.height / 2;
    radio.dispatchEvent(
        new MouseEvent(
            radio, MouseEvent.MOUSE_PRESSED, 0, 0, labelX, cy, 1, false, MouseEvent.BUTTON1));
    radio.dispatchEvent(
        new MouseEvent(
            radio, MouseEvent.MOUSE_RELEASED, 0, 0, labelX, cy, 1, false, MouseEvent.BUTTON1));
    check("clicking the label selects the radio", radio.isSelected());
  }

  private static void checkClickContract() {
    final ElwhaRadioButton radio = sized(new ElwhaRadioButton());
    final int[] actions = {0};
    final int[] changes = {0};
    radio.addActionListener(e -> actions[0] += "selected".equals(e.getActionCommand()) ? 1 : 100);
    radio.addChangeListener(e -> changes[0]++);

    click(radio);
    check("click selects", radio.isSelected());
    check("click fires ActionListener once, command 'selected'", actions[0] == 1);
    check("click fires ChangeListener once", changes[0] == 1);

    click(radio);
    check("re-click keeps selected", radio.isSelected());
    check("re-click fires no ActionListener", actions[0] == 1);
    check("re-click fires no ChangeListener", changes[0] == 1);

    radio.setSelected(false);
    check("programmatic deselect works", !radio.isSelected());
    check("programmatic deselect fires no ActionListener", actions[0] == 1);
    check("programmatic deselect fires ChangeListener", changes[0] == 2);

    press(radio);
    release(radio, new java.awt.Point(SIZE * 3, SIZE * 3));
    check("release outside bounds does not select", !radio.isSelected());
  }

  private static void checkSpaceContract() {
    final ElwhaRadioButton radio = sized(new ElwhaRadioButton());
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    check(
        "Space press binding installed",
        radio
                .getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false))
            != null);
    check(
        "Space release binding installed",
        radio
                .getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true))
            != null);

    spacePress(radio);
    check("Space press does not select yet", !radio.isSelected());
    spaceRelease(radio);
    check("Space release selects", radio.isSelected());
    check("Space fires ActionListener once", actions[0] == 1);

    spacePress(radio);
    spaceRelease(radio);
    check("Space on selected radio fires nothing", actions[0] == 1);
  }

  private static void checkDisabledInert() {
    final ElwhaRadioButton radio = sized(new ElwhaRadioButton());
    radio.setEnabled(false);
    final int[] events = {0};
    radio.addActionListener(e -> events[0]++);
    radio.addChangeListener(e -> events[0]++);

    click(radio);
    spacePress(radio);
    spaceRelease(radio);
    check("disabled radio ignores click and Space", !radio.isSelected() && events[0] == 0);
  }

  private static void checkStateLayers() {
    final Color surface = ColorRole.SURFACE.resolve();
    final Color onSurface = ColorRole.ON_SURFACE.resolve();
    final Color primary = ColorRole.PRIMARY.resolve();

    final ElwhaRadioButton hoverUnselected = sized(new ElwhaRadioButton());
    hoverUnselected.setHovered(true);
    final BufferedImage hoverU = render(hoverUnselected, surface);
    check(
        "unselected hover layer is ON_SURFACE @ HOVER",
        near(layerSample(hoverU), mix(surface, onSurface, StateLayer.HOVER.opacity())));
    check("unselected hovered ring shifts to ON_SURFACE", near(hoverU.getRGB(17, 17), onSurface));

    final ElwhaRadioButton hoverSelected = sized(new ElwhaRadioButton(true));
    hoverSelected.setHovered(true);
    final BufferedImage hoverS = render(hoverSelected, surface);
    check(
        "selected hover layer is PRIMARY @ HOVER",
        near(layerSample(hoverS), mix(surface, primary, StateLayer.HOVER.opacity())));

    final ElwhaRadioButton pressUnselected = sized(new ElwhaRadioButton());
    pressUnselected.setPressed(true);
    final BufferedImage pressU = render(pressUnselected, surface);
    check(
        "PRESS SWAP: unselected pressed layer is PRIMARY @ PRESSED",
        near(layerSample(pressU), mix(surface, primary, StateLayer.PRESSED.opacity())));

    final ElwhaRadioButton pressSelected = sized(new ElwhaRadioButton(true));
    pressSelected.setPressed(true);
    final BufferedImage pressS = render(pressSelected, surface);
    check(
        "PRESS SWAP: selected pressed layer is ON_SURFACE @ PRESSED",
        near(layerSample(pressS), mix(surface, onSurface, StateLayer.PRESSED.opacity())));

    final ElwhaRadioButton disabledRadio = sized(new ElwhaRadioButton());
    disabledRadio.setEnabled(false);
    disabledRadio.setHovered(true);
    disabledRadio.setPressed(true);
    final BufferedImage disabledImg = render(disabledRadio, surface);
    check("disabled paints no state layer", near(disabledImg.getRGB(CX, 8), surface));
  }

  // ------------------------------------------------------------------ plumbing

  private static void focus(
      final ElwhaRadioButton radio, final int id, final java.awt.event.FocusEvent.Cause cause) {
    final java.awt.event.FocusEvent event =
        new java.awt.event.FocusEvent(radio, id, false, null, cause);
    for (final java.awt.event.FocusListener listener : radio.getFocusListeners()) {
      if (id == java.awt.event.FocusEvent.FOCUS_GAINED) {
        listener.focusGained(event);
      } else {
        listener.focusLost(event);
      }
    }
  }

  private static ElwhaRadioButton sized(final ElwhaRadioButton radio) {
    radio.setSize(SIZE, SIZE);
    return radio;
  }

  private static void click(final ElwhaRadioButton radio) {
    press(radio);
    release(radio, new java.awt.Point(CX, CX));
  }

  private static void press(final ElwhaRadioButton radio) {
    radio.dispatchEvent(
        new MouseEvent(
            radio, MouseEvent.MOUSE_PRESSED, 0, 0, CX, CX, 1, false, MouseEvent.BUTTON1));
  }

  private static void release(final ElwhaRadioButton radio, final java.awt.Point at) {
    radio.dispatchEvent(
        new MouseEvent(
            radio, MouseEvent.MOUSE_RELEASED, 0, 0, at.x, at.y, 1, false, MouseEvent.BUTTON1));
  }

  private static void spacePress(final ElwhaRadioButton radio) {
    radio.getActionMap().get("elwhaRadio.press").actionPerformed(null);
  }

  private static void spaceRelease(final ElwhaRadioButton radio) {
    radio.getActionMap().get("elwhaRadio.release").actionPerformed(null);
  }

  private static BufferedImage render(final ElwhaRadioButton radio, final Color ground) {
    final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(ground);
      g.fillRect(0, 0, SIZE, SIZE);
      radio.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  /** Samples the state layer inside the 40 circle but clear of the icon (pixel distance ~15.5). */
  private static int layerSample(final BufferedImage img) {
    return img.getRGB(CX, 8);
  }

  private static boolean near(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    return c.getAlpha() == 255
        && Math.abs(c.getRed() - target.getRed()) <= 8
        && Math.abs(c.getGreen() - target.getGreen()) <= 8
        && Math.abs(c.getBlue() - target.getBlue()) <= 8;
  }

  private static Color mix(final Color base, final Color over, final float alpha) {
    return new Color(
        Math.round(base.getRed() + (over.getRed() - base.getRed()) * alpha),
        Math.round(base.getGreen() + (over.getGreen() - base.getGreen()) * alpha),
        Math.round(base.getBlue() + (over.getBlue() - base.getBlue()) * alpha));
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
