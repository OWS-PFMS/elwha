package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;

/**
 * Headless guard for Phase-3 / S3 (story #361). Asserts the {@link ElwhaSlider.Variant#RANGE}
 * keyboard / accessibility surface: the two-foci tab-stop structure, per-handle arrow / Home / End
 * keys (including the no-cross clamp and RTL mirroring), the two {@link AccessibleValue} children
 * reporting correct names + current / min / max bounds, and the single value bubble tracking the
 * active handle. No display required (actions are invoked directly; focus is driven via the
 * package-private {@code focusedHandle} and the mouse listener's active-handle pick).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderRangeKeyboardA11ySmoke {

  private static final int W = 240;

  private ElwhaSliderRangeKeyboardA11ySmoke() {}

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

    // --- two-foci structure: handles are the tab stops, the slider itself is not ---
    final ElwhaSlider single = new ElwhaSlider(0, 100, 0);
    check("single variant: slider is focusable", single.isFocusable());
    check("single variant: no accessible children",
        single.getAccessibleContext().getAccessibleChildrenCount() == 0);

    final ElwhaSlider rangeSlider = ElwhaSlider.range(0, 100, 30, 70);
    check("range variant: slider itself is not a tab stop", !rangeSlider.isFocusable());
    check("range variant: two focusable children (the handles)", focusableChildCount(rangeSlider) == 2);

    // --- accessibility: two AccessibleValue children, correct names + no-cross bounds ---
    rangeSlider.setLabel("Price");
    final AccessibleContext ctx = rangeSlider.getAccessibleContext();
    check("two accessible children", ctx.getAccessibleChildrenCount() == 2);

    final AccessibleContext lower = ctx.getAccessibleChild(0).getAccessibleContext();
    final AccessibleContext upper = ctx.getAccessibleChild(1).getAccessibleContext();
    check("lower child role SLIDER", lower.getAccessibleRole() == AccessibleRole.SLIDER);
    check("lower child name distinguishes lower", lower.getAccessibleName().equals("Price Lower"));
    check("upper child name distinguishes upper", upper.getAccessibleName().equals("Price Upper"));

    final AccessibleValue lv = lower.getAccessibleValue();
    final AccessibleValue uv = upper.getAccessibleValue();
    check("lower current = 30", lv.getCurrentAccessibleValue().intValue() == 30);
    check("lower min = slider min (0)", lv.getMinimumAccessibleValue().intValue() == 0);
    check("lower max = upper value (70, no-cross)", lv.getMaximumAccessibleValue().intValue() == 70);
    check("upper current = 70", uv.getCurrentAccessibleValue().intValue() == 70);
    check("upper min = lower value (30, no-cross)", uv.getMinimumAccessibleValue().intValue() == 30);
    check("upper max = slider max (100)", uv.getMaximumAccessibleValue().intValue() == 100);

    // --- per-handle arrow keys move the focused handle only ---
    final ElwhaSlider keys = ElwhaSlider.range(0, 100, 30, 70);
    keys.focusedHandle = ElwhaSlider.Handle.LOWER;
    invoke(keys, "elwhaSlider.increase");
    check("arrow moves the focused (lower) handle", keys.getLowerValue() == 31);
    check("arrow leaves the other (upper) handle", keys.getUpperValue() == 70);
    invoke(keys, "elwhaSlider.decrease");
    check("arrow back to 30", keys.getLowerValue() == 30);

    // --- Home/End target the focused handle's no-cross bound ---
    invoke(keys, "elwhaSlider.max"); // End on lower → upper value
    check("End on lower → upper value (no-cross)", keys.getLowerValue() == 70);
    final ElwhaSlider keys2 = ElwhaSlider.range(0, 100, 30, 70);
    keys2.focusedHandle = ElwhaSlider.Handle.UPPER;
    invoke(keys2, "elwhaSlider.min"); // Home on upper → lower value
    check("Home on upper → lower value (no-cross)", keys2.getUpperValue() == 30);
    invoke(keys2, "elwhaSlider.max"); // End on upper → slider max
    check("End on upper → slider max", keys2.getUpperValue() == 100);

    // --- RTL mirrors the left/right arrows ---
    final ElwhaSlider rtl = ElwhaSlider.range(0, 100, 30, 70);
    rtl.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    rtl.focusedHandle = ElwhaSlider.Handle.LOWER;
    invoke(rtl, "elwhaSlider.left");
    check("RTL: Left increases the value (mirrored)", rtl.getLowerValue() == 31);

    // --- one bubble: the active handle follows the press / focus ---
    final ElwhaSlider bubble = ElwhaSlider.range(0, 100, 30, 70);
    bubble.setValueIndicatorEnabled(true);
    bubble.setSize(W, ElwhaSlider.HANDLE_HEIGHT_PX);
    bubble.addNotify();
    pressRelease(bubble, bubble.xForValue(70));
    check("press near upper makes UPPER the active (bubble) handle",
        bubble.activeHandle == ElwhaSlider.Handle.UPPER);
    pressRelease(bubble, bubble.xForValue(30));
    check("press near lower moves the single bubble to LOWER",
        bubble.activeHandle == ElwhaSlider.Handle.LOWER);

    System.out.println(
        "ElwhaSliderRangeKeyboardA11ySmoke: OK (two foci + per-handle keys + a11y children + one"
            + " bubble)");
  }

  private static int focusableChildCount(final ElwhaSlider slider) {
    int count = 0;
    for (final java.awt.Component c : slider.getComponents()) {
      if (c.isFocusable()) {
        count++;
      }
    }
    return count;
  }

  private static void invoke(final ElwhaSlider slider, final String action) {
    slider
        .getActionMap()
        .get(action)
        .actionPerformed(new ActionEvent(slider, ActionEvent.ACTION_PERFORMED, action));
  }

  private static void pressRelease(final ElwhaSlider slider, final int x) {
    final int y = slider.getHeight() / 2;
    final MouseEvent press =
        new MouseEvent(
            slider,
            MouseEvent.MOUSE_PRESSED,
            System.nanoTime(),
            MouseEvent.BUTTON1_DOWN_MASK,
            x,
            y,
            x,
            y,
            1,
            false,
            MouseEvent.BUTTON1);
    final MouseEvent release =
        new MouseEvent(
            slider, MouseEvent.MOUSE_RELEASED, System.nanoTime(), 0, x, y, x, y, 1, false,
            MouseEvent.BUTTON1);
    for (final var l : slider.getMouseListeners()) {
      l.mousePressed(press);
      l.mouseReleased(release);
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
