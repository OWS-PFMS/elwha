package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.event.MouseEvent;

/**
 * Headless interaction guard for Phase-3 / S2 (story #360). Synthesises presses / drags / clicks on
 * an offscreen {@link ElwhaSlider.Variant#RANGE} slider and asserts: a press grabs the nearest
 * handle, dragging moves only that handle, the no-cross clamp holds (a dragged handle stops at the
 * other's value), click-to-jump moves the nearest handle, stops snap the moving handle, {@link
 * ElwhaSlider#getValueIsAdjusting()} brackets the gesture, and hit-testing mirrors under RTL. No
 * display required.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderRangeInteractionSmoke {

  private static final int W = 240;

  private ElwhaSliderRangeInteractionSmoke() {}

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

    // --- nearest-handle pick ---
    final ElwhaSlider pick = range(0, 100, 30, 70);
    check("pick nearest = LOWER near the lower handle",
        pick.pickHandle(pick.xForValue(28)) == ElwhaSlider.Handle.LOWER);
    check("pick nearest = UPPER near the upper handle",
        pick.pickHandle(pick.xForValue(72)) == ElwhaSlider.Handle.UPPER);
    check("midpoint ties toward LOWER below, UPPER above",
        pick.pickHandle(pick.xForValue(40)) == ElwhaSlider.Handle.LOWER
            && pick.pickHandle(pick.xForValue(60)) == ElwhaSlider.Handle.UPPER);

    // --- drag moves only the grabbed handle ---
    final ElwhaSlider dragLower = range(0, 100, 30, 70);
    press(dragLower, dragLower.xForValue(30));
    drag(dragLower, dragLower.xForValue(50));
    check("drag lower → lower≈50", Math.abs(dragLower.getLowerValue() - 50) <= 2);
    check("drag lower leaves upper at 70", dragLower.getUpperValue() == 70);
    release(dragLower, dragLower.xForValue(50));

    final ElwhaSlider dragUpper = range(0, 100, 30, 70);
    press(dragUpper, dragUpper.xForValue(70));
    drag(dragUpper, dragUpper.xForValue(85));
    check("drag upper → upper≈85", Math.abs(dragUpper.getUpperValue() - 85) <= 2);
    check("drag upper leaves lower at 30", dragUpper.getLowerValue() == 30);
    release(dragUpper, dragUpper.xForValue(85));

    // --- no-cross clamp ---
    final ElwhaSlider crossLow = range(0, 100, 30, 70);
    press(crossLow, crossLow.xForValue(30));
    drag(crossLow, crossLow.xForValue(95));
    check("lower can't pass upper (clamps at 70)", crossLow.getLowerValue() == 70);
    release(crossLow, crossLow.xForValue(95));

    final ElwhaSlider crossHigh = range(0, 100, 30, 70);
    press(crossHigh, crossHigh.xForValue(70));
    drag(crossHigh, crossHigh.xForValue(5));
    check("upper can't pass lower (clamps at 30)", crossHigh.getUpperValue() == 30);
    release(crossHigh, crossHigh.xForValue(5));

    // --- click-to-jump nearest handle ---
    final ElwhaSlider jump = range(0, 100, 20, 80);
    press(jump, jump.xForValue(35));
    release(jump, jump.xForValue(35));
    check("click near lower jumps lower (≈35)", Math.abs(jump.getLowerValue() - 35) <= 2);
    check("click-to-jump leaves upper at 80", jump.getUpperValue() == 80);

    // --- stops snap the moving handle ---
    final ElwhaSlider snap = range(0, 100, 20, 80);
    snap.setStops(10);
    press(snap, snap.xForValue(20));
    drag(snap, snap.xForValue(34));
    check("stops snap the moving handle (→30)", snap.getLowerValue() == 30);
    release(snap, snap.xForValue(34));

    // --- adjusting brackets the gesture + listener fires ---
    final ElwhaSlider events = range(0, 100, 30, 70);
    final boolean[] sawAdjusting = {false};
    final int[] changes = {0};
    events.addChangeListener(
        e -> {
          changes[0]++;
          if (events.getValueIsAdjusting()) {
            sawAdjusting[0] = true;
          }
        });
    press(events, events.xForValue(30));
    check("getValueIsAdjusting() true on press", events.getValueIsAdjusting());
    drag(events, events.xForValue(45));
    release(events, events.xForValue(45));
    check("getValueIsAdjusting() false after release", !events.getValueIsAdjusting());
    check("a change fired with adjusting=true mid-drag", sawAdjusting[0]);
    check("change listener observed updates", changes[0] > 0);

    // --- RTL hit-testing mirrors ---
    final ElwhaSlider rtl = range(0, 100, 30, 70);
    rtl.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    rtl.setSize(W, ElwhaSlider.HANDLE_HEIGHT_PX);
    check("RTL: lower handle still picked at its (mirrored) x",
        rtl.pickHandle(rtl.xForValue(30)) == ElwhaSlider.Handle.LOWER);
    check("RTL: upper handle still picked at its (mirrored) x",
        rtl.pickHandle(rtl.xForValue(70)) == ElwhaSlider.Handle.UPPER);

    System.out.println(
        "ElwhaSliderRangeInteractionSmoke: OK (pick + drag + no-cross + jump + stops + adjusting +"
            + " RTL)");
  }

  private static ElwhaSlider range(final int min, final int max, final int lower, final int upper) {
    final ElwhaSlider s = ElwhaSlider.range(min, max, lower, upper);
    s.setSize(W, ElwhaSlider.HANDLE_HEIGHT_PX);
    s.addNotify();
    return s;
  }

  private static void press(final ElwhaSlider s, final int x) {
    dispatch(s, MouseEvent.MOUSE_PRESSED, x);
  }

  private static void drag(final ElwhaSlider s, final int x) {
    dispatch(s, MouseEvent.MOUSE_DRAGGED, x);
  }

  private static void release(final ElwhaSlider s, final int x) {
    dispatch(s, MouseEvent.MOUSE_RELEASED, x);
  }

  private static void dispatch(final ElwhaSlider slider, final int id, final int x) {
    final int y = slider.getHeight() / 2;
    final int modifiers = (id == MouseEvent.MOUSE_RELEASED) ? 0 : MouseEvent.BUTTON1_DOWN_MASK;
    final MouseEvent e =
        new MouseEvent(
            slider, id, System.nanoTime(), modifiers, x, y, x, y, 1, false, MouseEvent.BUTTON1);
    if (id == MouseEvent.MOUSE_DRAGGED) {
      for (var l : slider.getMouseMotionListeners()) {
        l.mouseDragged(e);
      }
      return;
    }
    for (var l : slider.getMouseListeners()) {
      if (id == MouseEvent.MOUSE_PRESSED) {
        l.mousePressed(e);
      } else {
        l.mouseReleased(e);
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
