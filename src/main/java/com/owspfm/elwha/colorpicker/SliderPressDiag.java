package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Smoke-iterate diagnostic for the "sliders jump and stick to the far left" finding (#486):
 * synthesizes MOUSE_PRESSED/DRAGGED/RELEASED straight onto the SLIDERS pane's channel sliders and
 * the spectrum pane's hue slider after a real layout pass, logging bounds, event x, valueAt
 * mapping, slider value, and picker color at every step — separating the (smoke-proven) model path
 * from the live event path.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SliderPressDiag {

  private SliderPressDiag() {}

  /**
   * Runs the diagnostic.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x336699));
    picker.setMode(PickerMode.SLIDERS);
    layoutTree(picker, new Dimension(328, 480));

    final SlidersPane pane = (SlidersPane) picker.paneFor(PickerMode.SLIDERS);
    final List<ColorTrackSlider> sliders = new ArrayList<>();
    collect(pane, ColorTrackSlider.class, sliders);
    System.out.println("sliders found in pane: " + sliders.size());
    for (final ColorTrackSlider slider : sliders) {
      System.out.println(
          "  bounds="
              + slider.getBounds()
              + " value="
              + slider.value()
              + " showing-parent="
              + slider.getParent().getClass().getSimpleName());
    }

    final ColorTrackSlider red = sliders.get(0);
    System.out.println("--- press mid-track on R (width=" + red.getWidth() + ") ---");
    final int midX = red.getWidth() / 2;
    System.out.println("valueAt(" + midX + ") = " + red.valueAt(midX));
    press(red, midX);
    System.out.println(
        "after press: R.value="
            + red.value()
            + "  picker="
            + ColorHex.format(picker.getColor(), false));
    drag(red, midX + 20);
    System.out.println(
        "after drag(+20): R.value="
            + red.value()
            + "  picker="
            + ColorHex.format(picker.getColor(), false));
    release(red, midX + 20);
    System.out.println(
        "after release: R.value="
            + red.value()
            + "  picker="
            + ColorHex.format(picker.getColor(), false)
            + "  adjusting="
            + picker.isAdjusting());

    System.out.println("--- second gesture on R after refresh ---");
    press(red, midX - 30);
    release(red, midX - 30);
    System.out.println(
        "after gesture2: R.value="
            + red.value()
            + "  picker="
            + ColorHex.format(picker.getColor(), false));

    System.out.println("--- hue slider in SPECTRUM for contrast ---");
    picker.setMode(PickerMode.SPECTRUM);
    final SpectrumPane spectrum = (SpectrumPane) picker.paneFor(PickerMode.SPECTRUM);
    final List<ColorTrackSlider> hueSliders = new ArrayList<>();
    collect(spectrum, ColorTrackSlider.class, hueSliders);
    final ColorTrackSlider hue = hueSliders.get(0);
    System.out.println("hue bounds=" + hue.getBounds() + " value=" + hue.value());
    press(hue, hue.getWidth() / 2);
    release(hue, hue.getWidth() / 2);
    System.out.println(
        "after hue press: value="
            + hue.value()
            + "  picker="
            + ColorHex.format(picker.getColor(), false));

    System.out.println("SliderPressDiag: done");
  }

  private static void press(final Component target, final int x) {
    target.dispatchEvent(
        new MouseEvent(
            target, MouseEvent.MOUSE_PRESSED, 0L, 0, x, 14, 1, false, MouseEvent.BUTTON1));
  }

  private static void drag(final Component target, final int x) {
    target.dispatchEvent(
        new MouseEvent(
            target, MouseEvent.MOUSE_DRAGGED, 0L, 0, x, 14, 1, false, MouseEvent.BUTTON1));
  }

  private static void release(final Component target, final int x) {
    target.dispatchEvent(
        new MouseEvent(
            target, MouseEvent.MOUSE_RELEASED, 0L, 0, x, 14, 1, false, MouseEvent.BUTTON1));
  }

  private static void layoutTree(final Component component, final Dimension size) {
    component.setSize(size);
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        if (child.getWidth() == 0 || child.getHeight() == 0) {
          child.setSize(child.getPreferredSize());
        }
        layoutTree(child, child.getSize());
      }
    }
  }

  private static <T> void collect(final Container root, final Class<T> type, final List<T> out) {
    for (final Component child : root.getComponents()) {
      if (type.isInstance(child)) {
        out.add(type.cast(child));
      }
      if (child instanceof Container nested) {
        collect(nested, type, out);
      }
    }
  }
}
