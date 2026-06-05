package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.Action;

/**
 * Headless keyboard / accessibility / RTL guard for S3 (story #344). Drives the {@link ElwhaSlider}
 * action map directly (no display) to assert: arrows step by the unit increment, Space promotes
 * arrows to the block increment, Home/End jump to min/max, the horizontal arrows mirror under a
 * right-to-left orientation, and the accessible surface reports {@link AccessibleRole#SLIDER} +
 * {@link AccessibleValue} (current / min / max) with the label as the accessible name.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderKeyboardSmoke {

  private ElwhaSliderKeyboardSmoke() {}

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
    slider.setUnitIncrement(1);
    slider.setBlockIncrement(10);

    fire(slider, "elwhaSlider.right");
    check("right arrow +unit", slider.getValue() == 51);
    fire(slider, "elwhaSlider.left");
    fire(slider, "elwhaSlider.left");
    check("left arrow -unit", slider.getValue() == 49);

    fire(slider, "elwhaSlider.increase");
    check("up arrow +unit", slider.getValue() == 50);
    fire(slider, "elwhaSlider.decrease");
    check("down arrow -unit", slider.getValue() == 49);

    fire(slider, "elwhaSlider.spaceDown");
    fire(slider, "elwhaSlider.right");
    check("Space+right = +block", slider.getValue() == 59);
    fire(slider, "elwhaSlider.spaceUp");
    fire(slider, "elwhaSlider.right");
    check("after Space released, right = +unit again", slider.getValue() == 60);

    fire(slider, "elwhaSlider.blockUp");
    check("PageUp = +block", slider.getValue() == 70);

    fire(slider, "elwhaSlider.min");
    check("Home = min", slider.getValue() == 0);
    fire(slider, "elwhaSlider.max");
    check("End = max", slider.getValue() == 100);

    // RTL: the left arrow should now increase, the right arrow decrease.
    slider.setValue(50);
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    fire(slider, "elwhaSlider.left");
    check("RTL: left arrow increases", slider.getValue() == 51);
    fire(slider, "elwhaSlider.right");
    fire(slider, "elwhaSlider.right");
    check("RTL: right arrow decreases", slider.getValue() == 49);

    // RTL hit-testing mirrors: at min the handle sits on the right; at max on the left.
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    slider.setSize(240, ElwhaSlider.HANDLE_HEIGHT_PX);
    slider.setValue(slider.getMinimum());
    final int minHandle = slider.handleCenterX();
    slider.setValue(slider.getMaximum());
    final int maxHandle = slider.handleCenterX();
    check("RTL: min handle is right of max handle", minHandle > maxHandle);

    final AccessibleContext ac = slider.getAccessibleContext();
    check("accessible role is SLIDER", ac.getAccessibleRole() == AccessibleRole.SLIDER);
    final AccessibleValue av = ac.getAccessibleValue();
    check("AccessibleValue present", av != null);
    check("AccessibleValue current == value", av.getCurrentAccessibleValue().intValue() == 100);
    check("AccessibleValue min == 0", av.getMinimumAccessibleValue().intValue() == 0);
    check("AccessibleValue max == 100", av.getMaximumAccessibleValue().intValue() == 100);
    av.setCurrentAccessibleValue(42);
    check("AccessibleValue set writes through", slider.getValue() == 42);

    slider.setLabel("Brightness");
    check("accessible name == label", "Brightness".equals(ac.getAccessibleName()));

    System.out.println("ElwhaSliderKeyboardSmoke: OK (keymap + RTL mirror + slider a11y)");
  }

  private static void fire(final ElwhaSlider slider, final String key) {
    final Action a = slider.getActionMap().get(key);
    if (a == null) {
      System.err.println("FAIL: no action bound for " + key);
      System.exit(1);
    }
    a.actionPerformed(new ActionEvent(slider, ActionEvent.ACTION_PERFORMED, key));
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
