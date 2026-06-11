package com.owspfm.elwha.slider;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Headless interaction guard for the Phase-5 / S2 vertical slider (story #386). Asserts that (a)
 * vertical drag + click-to-jump move the value <strong>monotonically with Y</strong> (top = max,
 * bottom = min) with {@link ElwhaSlider#getValueIsAdjusting()} honored, (b) Up / Down keys increase
 * / decrease the value, and (c) the vertical inset icon paints at the top (max) end and its tint
 * swaps from {@link ColorRole#ON_SECONDARY_CONTAINER} (low value, inactive top) to {@link
 * ColorRole#ON_PRIMARY} (high value, active top). Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderVerticalInteractionSmoke {

  private static final int LONG = 300;

  private ElwhaSliderVerticalInteractionSmoke() {}

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

    // (a) vertical drag / click-to-jump: bottom = min, top = max.
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 0);
    slider.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    final int w = slider.getPreferredSize().width;
    slider.setSize(w, LONG);
    slider.addNotify();

    final boolean[] sawAdjusting = {false};
    slider.addChangeListener(
        e -> {
          if (slider.getValueIsAdjusting()) {
            sawAdjusting[0] = true;
          }
        });

    final int x = w / 2;
    press(slider, x, LONG - 4);
    check("press near the bottom lands near min", slider.getValue() <= 3);
    check("getValueIsAdjusting() true on press", slider.getValueIsAdjusting());

    drag(slider, x, LONG / 2);
    final int mid = slider.getValue();
    check("drag to the vertical midpoint lands near 50", Math.abs(mid - 50) <= 3);

    drag(slider, x, 4);
    final int high = slider.getValue();
    check("drag toward the top increases the value (monotonic with Y)", high > mid);
    check("drag near the top lands near max", high >= 97);

    drag(slider, x, LONG + 50);
    check("drag past the bottom clamps at min", slider.getValue() == 0);

    release(slider, x, LONG + 50);
    check("getValueIsAdjusting() false after release", !slider.getValueIsAdjusting());
    check("a live update fired with adjusting=true mid-drag", sawAdjusting[0]);

    // (b) keyboard Up / Down drive the value on the vertical axis.
    slider.setValue(50);
    fireKey(slider, KeyEvent.VK_UP);
    check("Up increases the value", slider.getValue() == 51);
    fireKey(slider, KeyEvent.VK_DOWN);
    fireKey(slider, KeyEvent.VK_DOWN);
    check("Down decreases the value", slider.getValue() == 49);
    slider.removeNotify();

    // (c) vertical inset icon at the top end, with the coverage-tint swap.
    final int[] lowSlot = insetSlot(95 - 90); // low value: inactive top.
    final int[] highSlot = insetSlot(95); // high value: active top.
    check("inset glyph is painted at the top of a low-value vertical slider", lowSlot[0] > 80);
    check("inset glyph is painted at the top of a high-value vertical slider", highSlot[0] > 80);
    check(
        "inset glyph tint swaps lighter when the active fill reaches the top (ON_PRIMARY)",
        highSlot[1] - lowSlot[1] > 60);

    System.out.println(
        "ElwhaSliderVerticalInteractionSmoke: OK (Y-drag + keyboard + inset top swap)");
  }

  /** Paints a vertical M slider with the inset icon at {@code value}; returns {ink, avgInkLum}. */
  private static int[] insetSlot(final int value) {
    final ElwhaSlider s = new ElwhaSlider(0, 100, value);
    s.setSizeVariant(ElwhaSlider.Size.M);
    s.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    s.setInsetIcon(MaterialIcons.brightnessAuto());
    final int w = s.getPreferredSize().width;
    s.setSize(w, LONG);
    final BufferedImage img = new BufferedImage(w, LONG, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      s.paint(g);
    } finally {
      g.dispose();
    }
    final Color primary = ColorRole.PRIMARY.resolve();
    final Color inactive = ColorRole.SECONDARY_CONTAINER.resolve();
    int ink = 0;
    long lum = 0;
    for (int y = 12; y < 36; y++) {
      for (int px = Math.max(0, w / 2 - 12); px < Math.min(w, w / 2 + 12); px++) {
        final Color c = new Color(img.getRGB(px, y), true);
        if (c.getAlpha() < 40 || near(c, primary) || near(c, inactive)) {
          continue;
        }
        ink++;
        lum += (c.getRed() + c.getGreen() + c.getBlue()) / 3;
      }
    }
    return new int[] {ink, ink == 0 ? 0 : (int) (lum / ink)};
  }

  private static void press(final ElwhaSlider s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_PRESSED, x, y);
  }

  private static void drag(final ElwhaSlider s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_DRAGGED, x, y);
  }

  private static void release(final ElwhaSlider s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_RELEASED, x, y);
  }

  private static void dispatch(final ElwhaSlider slider, final int id, final int x, final int y) {
    final int modifiers = (id == MouseEvent.MOUSE_RELEASED) ? 0 : MouseEvent.BUTTON1_DOWN_MASK;
    final MouseEvent e =
        new MouseEvent(
            slider, id, System.nanoTime(), modifiers, x, y, x, y, 1, false, MouseEvent.BUTTON1);
    for (var l : slider.getMouseListeners()) {
      switch (id) {
        case MouseEvent.MOUSE_PRESSED -> l.mousePressed(e);
        case MouseEvent.MOUSE_RELEASED -> l.mouseReleased(e);
        default -> {
          // motion below
        }
      }
    }
    if (id == MouseEvent.MOUSE_DRAGGED) {
      for (var l : slider.getMouseMotionListeners()) {
        l.mouseDragged(e);
      }
    }
  }

  /** Fires the keyboard action bound to {@code keyCode} via the slider's input/action maps. */
  private static void fireKey(final ElwhaSlider slider, final int keyCode) {
    final KeyStroke ks = KeyStroke.getKeyStroke(keyCode, 0);
    final Object name = slider.getInputMap(JComponent.WHEN_FOCUSED).get(ks);
    final AbstractAction action = (AbstractAction) slider.getActionMap().get(name);
    action.actionPerformed(
        new ActionEvent(slider, ActionEvent.ACTION_PERFORMED, String.valueOf(name)));
  }

  private static boolean near(final Color c, final Color t) {
    return Math.abs(c.getRed() - t.getRed()) <= 10
        && Math.abs(c.getGreen() - t.getGreen()) <= 10
        && Math.abs(c.getBlue() - t.getBlue()) <= 10;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
