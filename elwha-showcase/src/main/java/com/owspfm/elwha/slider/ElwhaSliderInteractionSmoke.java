package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.event.MouseEvent;

/**
 * Headless interaction guard for S2 (story #343). Synthesises a press &rarr; drag &rarr; release on
 * an offscreen {@link ElwhaSlider} and asserts: click-to-jump moves the value to the press point,
 * dragging tracks the pointer, {@link ElwhaSlider#getValueIsAdjusting()} is true mid-gesture and
 * false after release, and the value clamps at the track ends. No display required.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderInteractionSmoke {

  private ElwhaSliderInteractionSmoke() {}

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

    final int w = 240;
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 0);
    slider.setSize(w, ElwhaSlider.HANDLE_HEIGHT_PX);
    slider.addNotify();

    final int[] lastValue = {-1};
    final boolean[] sawAdjusting = {false};
    slider.addChangeListener(
        e -> {
          lastValue[0] = slider.getValue();
          if (slider.getValueIsAdjusting()) {
            sawAdjusting[0] = true;
          }
        });

    final int midX = w / 2;
    final int y = slider.getHeight() / 2;

    dispatch(slider, MouseEvent.MOUSE_PRESSED, midX, y);
    check("click-to-jump lands near the press point", Math.abs(slider.getValue() - 50) <= 2);
    check("getValueIsAdjusting() true on press", slider.getValueIsAdjusting());

    dispatch(slider, MouseEvent.MOUSE_DRAGGED, (int) (w * 0.75), y);
    check("drag tracks the pointer (≈75)", Math.abs(slider.getValue() - 75) <= 2);

    dispatch(slider, MouseEvent.MOUSE_DRAGGED, w + 50, y);
    check("drag past the right end clamps at max", slider.getValue() == 100);

    dispatch(slider, MouseEvent.MOUSE_DRAGGED, -50, y);
    check("drag past the left end clamps at min", slider.getValue() == 0);

    dispatch(slider, MouseEvent.MOUSE_RELEASED, -50, y);
    check("getValueIsAdjusting() false after release", !slider.getValueIsAdjusting());
    check("a live update fired with adjusting=true mid-drag", sawAdjusting[0]);
    check("change listener observed the final value", lastValue[0] == 0);

    slider.removeNotify();
    System.out.println("ElwhaSliderInteractionSmoke: OK (press/drag/release + adjusting + clamp)");
  }

  private static void dispatch(final ElwhaSlider slider, final int id, final int x, final int y) {
    final int button = MouseEvent.BUTTON1;
    final int modifiers = (id == MouseEvent.MOUSE_RELEASED) ? 0 : MouseEvent.BUTTON1_DOWN_MASK;
    // Explicit-abs-coords ctor: the (x,y)-only ctor calls getLocationOnScreen, which NPEs on an
    // unrealized (peerless) component in headless mode.
    final MouseEvent e =
        new MouseEvent(slider, id, System.nanoTime(), modifiers, x, y, x, y, 1, false, button);
    for (var l : slider.getMouseListeners()) {
      switch (id) {
        case MouseEvent.MOUSE_PRESSED -> l.mousePressed(e);
        case MouseEvent.MOUSE_RELEASED -> l.mouseReleased(e);
        default -> {
          // handled by motion listeners below
        }
      }
    }
    if (id == MouseEvent.MOUSE_DRAGGED) {
      for (var l : slider.getMouseMotionListeners()) {
        l.mouseDragged(e);
      }
    }
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
