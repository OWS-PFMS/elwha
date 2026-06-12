package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * S7 headless guard for keyboard + a11y + RTL (#489): the picker's COLOR_CHOOSER role with
 * supporting-text naming and live hex description, ColorTrackSlider's SLIDER role + AccessibleValue
 * round-trip + channel names, the three swatch strips exposing LIST roles with catalog cell names,
 * the SV box's name/value description, RTL value mapping on the track, and the #432 contract — a
 * disabled slider's keyboard action leaves the value untouched.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerA11ySmoke {

  private ElwhaColorPickerA11ySmoke() {}

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

    checkPickerContext();
    checkSliderAccessibility();
    checkStripNames();
    checkRtlAndDisabled();

    System.out.println(
        "ElwhaColorPickerA11ySmoke: OK (roles, names, AccessibleValue, RTL, disabled-inert)");
  }

  private static void checkPickerContext() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x7E57C2));
    final AccessibleContext context = picker.getAccessibleContext();
    check("role is COLOR_CHOOSER", context.getAccessibleRole() == AccessibleRole.COLOR_CHOOSER);
    check("named by default supporting text", "Select color".equals(context.getAccessibleName()));
    picker.setSupportingText("Pick an accent");
    check("name follows supporting text", "Pick an accent".equals(context.getAccessibleName()));
    check("description carries the hex", context.getAccessibleDescription().contains("#7E57C2"));
    picker.setColor(new Color(0x00FF00));
    check("description tracks commits", context.getAccessibleDescription().contains("#00FF00"));
  }

  private static void checkSliderAccessibility() {
    final ColorTrackSlider slider = new ColorTrackSlider(0, 255, 10);
    slider.setAccessibleChannelName("Red");
    final AccessibleContext context = slider.getAccessibleContext();
    check("slider role", context.getAccessibleRole() == AccessibleRole.SLIDER);
    check("channel name", "Red".equals(context.getAccessibleName()));
    check(
        "current value", context.getAccessibleValue().getCurrentAccessibleValue().intValue() == 10);
    check(
        "min and max",
        context.getAccessibleValue().getMinimumAccessibleValue().intValue() == 0
            && context.getAccessibleValue().getMaximumAccessibleValue().intValue() == 255);

    final AtomicInteger fires = new AtomicInteger();
    slider.setListener((value, adjusting) -> fires.incrementAndGet());
    check("set via AccessibleValue", context.getAccessibleValue().setCurrentAccessibleValue(40));
    check("AccessibleValue set lands", slider.value() == 40 && fires.get() == 1);
  }

  private static void checkStripNames() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xF44336));
    picker.setAlphaEnabled(false);
    final SwatchesPane swatches = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    final List<String> listNames = new ArrayList<>();
    collectNamesByRole(swatches, AccessibleRole.LIST, listNames);
    check("four strips expose LIST", listNames.size() == 4);
    check("hue grid named", listNames.contains("Hue swatches"));
    check("theme grid named", listNames.contains("Theme colors"));
    check("shade strip names its hue", listNames.contains("Shades of Red"));
    check("recent row named", listNames.contains("Recent colors"));

    final SpectrumPane spectrum = (SpectrumPane) picker.paneFor(PickerMode.SPECTRUM);
    final List<String> panelNames = new ArrayList<>();
    collectNamesByRole(spectrum, AccessibleRole.PANEL, panelNames);
    check("sv box named", panelNames.contains("Saturation and value"));

    final List<String> sliderNames = new ArrayList<>();
    collectNamesByRole(
        (Container) picker.paneFor(PickerMode.SLIDERS), AccessibleRole.SLIDER, sliderNames);
    check(
        "channel sliders named",
        sliderNames.containsAll(List.of("Red", "Green", "Blue", "Hue", "Saturation", "Value")));
  }

  private static void checkRtlAndDisabled() {
    final ColorTrackSlider slider = new ColorTrackSlider(0, 100, 50);
    slider.setSize(120, ColorTrackSlider.COMPONENT_HEIGHT);
    check("LTR left edge maps to min", slider.valueAt(5) == 0);
    check("LTR right edge maps to max", slider.valueAt(115) == 100);
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    check("RTL left edge maps to max", slider.valueAt(5) == 100);
    check("RTL right edge maps to min", slider.valueAt(115) == 0);

    slider.setEnabled(false);
    final KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    final Action action = slider.getActionMap().get(right.toString());
    check("key action exists", action != null);
    final int before = slider.value();
    action.actionPerformed(
        new java.awt.event.ActionEvent(slider, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
    check("disabled slider is keyboard-inert", slider.value() == before);

    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setEnabled(false);
    final SwatchesPane swatches = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    check("disable reaches strip children", !firstFocusableChild(swatches).isEnabled());
  }

  private static Component firstFocusableChild(final Container container) {
    for (final Component child : container.getComponents()) {
      if (child.isFocusable() && child instanceof JComponent) {
        return child;
      }
      if (child instanceof Container nested) {
        final Component found = firstFocusableChild(nested);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void collectNamesByRole(
      final Container root, final AccessibleRole role, final List<String> names) {
    for (final Component child : root.getComponents()) {
      if (child instanceof JComponent component) {
        final AccessibleContext context = component.getAccessibleContext();
        if (context != null && context.getAccessibleRole() == role) {
          names.add(context.getAccessibleName());
        }
      }
      if (child instanceof Container nested) {
        collectNamesByRole(nested, role, names);
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
