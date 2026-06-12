package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * S1 headless guard for the {@link ElwhaColorPicker} shell (#483): selection-model contract
 * (never-null color, alpha stripping, change-fire rules, adjusting semantics), the closed {@code
 * setModes} validation, single-mode tab-bar hiding, supporting-text collapse, enabled cascade, hex
 * formatting, and a light+dark paint pass with a swatch pixel assertion.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerShellSmoke {

  private ElwhaColorPickerShellSmoke() {}

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

    checkModel();
    checkModes();
    checkHex();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      checkPaint(mode);
    }

    System.out.println("ElwhaColorPickerShellSmoke: OK (model, modes, hex, light+dark paint)");
  }

  private static void checkModel() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    check("default color is white", Color.WHITE.equals(picker.getColor()));
    check("default not adjusting", !picker.isAdjusting());

    boolean threw = false;
    try {
      picker.setColor(null);
    } catch (final IllegalArgumentException e) {
      threw = true;
    }
    check("setColor(null) throws", threw);

    final AtomicInteger fires = new AtomicInteger();
    picker.addChangeListener(e -> fires.incrementAndGet());

    picker.setColor(new Color(10, 20, 30, 77));
    check("alpha stripped on commit", new Color(10, 20, 30).equals(picker.getColor()));
    check("one fire per change", fires.get() == 1);

    picker.setColor(new Color(10, 20, 30));
    check("same color does not re-fire", fires.get() == 1);

    picker.commitFromPane(null, new Color(40, 50, 60), true);
    check("adjusting commit fires", fires.get() == 2);
    check("adjusting reported", picker.isAdjusting());
    picker.commitFromPane(null, new Color(40, 50, 60), false);
    check("settling commit fires even without color change", fires.get() == 3);
    check("adjusting cleared", !picker.isAdjusting());

    final int before = fires.get();
    picker.setMode(PickerMode.SLIDERS);
    check("mode switch never fires color change", fires.get() == before);
    check("mode switch preserves color", new Color(40, 50, 60).equals(picker.getColor()));
  }

  private static void checkModes() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    check(
        "default mode order",
        List.of(PickerMode.SWATCHES, PickerMode.SPECTRUM, PickerMode.SLIDERS)
            .equals(picker.getModes()));
    check("default active mode", picker.getMode() == PickerMode.SWATCHES);

    picker.setMode(PickerMode.SPECTRUM);
    check("setMode activates", picker.getMode() == PickerMode.SPECTRUM);

    final ElwhaTabs tabs = findDescendant(picker, ElwhaTabs.class);
    check("tab bar present", tabs != null);
    check("tab bar visible with three modes", tabs.isVisible());

    picker.setModes(PickerMode.SLIDERS);
    check("subset applied", List.of(PickerMode.SLIDERS).equals(picker.getModes()));
    check("single mode active", picker.getMode() == PickerMode.SLIDERS);
    check("single mode hides tab bar", !findDescendant(picker, ElwhaTabs.class).isVisible());

    picker.setModes(PickerMode.SPECTRUM, PickerMode.SWATCHES);
    check(
        "reorder respected",
        List.of(PickerMode.SPECTRUM, PickerMode.SWATCHES).equals(picker.getModes()));
    check("first mode becomes active", picker.getMode() == PickerMode.SPECTRUM);

    check("empty modes throw", throwsIae(picker::setModes));
    check(
        "duplicate modes throw",
        throwsIae(() -> picker.setModes(PickerMode.SWATCHES, PickerMode.SWATCHES)));
    check("null mode throws", throwsIae(() -> picker.setModes(PickerMode.SWATCHES, null)));
    check("unoffered setMode throws", throwsIae(() -> picker.setMode(PickerMode.SLIDERS)));

    picker.setSupportingText(null);
    check("supporting text hidden", picker.getSupportingText() == null);

    picker.setEnabled(false);
    check("disable cascades to tabs", !findDescendant(picker, ElwhaTabs.class).isEnabled());
  }

  private static void checkHex() {
    check("rgb hex", "#0A141E".equals(ColorHex.format(new Color(10, 20, 30), false)));
    check("alpha hex", "#0A141E4D".equals(ColorHex.format(new Color(10, 20, 30, 77), true)));
    check(
        "headline readout",
        "#FF7043".equals(new ElwhaColorPicker(new Color(0xFF7043)).formatCurrentHex()));
  }

  private static void checkPaint(final Mode mode) {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xD32F2F));
    picker.setSupportingText(null);
    layoutTree(picker, new Dimension(360, 420));
    final BufferedImage image = new BufferedImage(360, 420, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, 360, 420);
    picker.paint(g2);
    g2.dispose();
    final Color probe = new Color(image.getRGB(36, 36));
    check("swatch pixel painted (" + mode + ")", new Color(0xD32F2F).equals(probe));
  }

  private static void layoutTree(final Component component, final Dimension size) {
    component.setSize(size);
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutChild(child);
      }
    }
  }

  private static void layoutChild(final Component component) {
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutChild(child);
      }
    }
  }

  private static boolean throwsIae(final Runnable action) {
    try {
      action.run();
      return false;
    } catch (final IllegalArgumentException e) {
      return true;
    }
  }

  private static <T> T findDescendant(final Container root, final Class<T> type) {
    for (final Component child : root.getComponents()) {
      if (type.isInstance(child)) {
        return type.cast(child);
      }
      if (child instanceof Container container) {
        final T found = findDescendant(container, type);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
