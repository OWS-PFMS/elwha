package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.accessibility.AccessibleContext;

/**
 * V2 S2 headless guard for the SwatchSource toggle + THEME tier (#498): the setSwatchSources
 * closed-set contract (subset/reorder/validation, single source hides the toggle), source switching
 * without color mutation, source survival across the alpha rebuild, the THEME grid's gesture-path
 * commit of live-resolved roles as plain (non-UIResource) colors, live re-resolution across a mode
 * flip, alpha preservation on role picks, and the height budget.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerThemeTierSmoke {

  private ElwhaColorPickerThemeTierSmoke() {}

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

    checkContract();
    checkThemeGrid();

    System.out.println(
        "ElwhaColorPickerThemeTierSmoke: OK (source contract, gesture commits, live re-resolve,"
            + " budget)");
  }

  private static void checkContract() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x123456));
    check(
        "default offers Material then Theme",
        java.util.List.of(SwatchSource.MATERIAL, SwatchSource.THEME)
            .equals(picker.getSwatchSources()));
    check("default active source", picker.getSwatchSource() == SwatchSource.MATERIAL);

    picker.setSwatchSource(SwatchSource.THEME);
    check("setSwatchSource activates", picker.getSwatchSource() == SwatchSource.THEME);
    check("source switch never mutates color", new Color(0x123456).equals(picker.getColor()));

    picker.setAlphaEnabled(true);
    check("source survives the alpha rebuild", picker.getSwatchSource() == SwatchSource.THEME);
    picker.setAlphaEnabled(false);

    picker.setSwatchSources(SwatchSource.THEME);
    check("single-source subset applies", picker.getSwatchSource() == SwatchSource.THEME);
    final Component swatchesPane = picker.paneFor(PickerMode.SWATCHES);
    check(
        "single source hides the toggle",
        findByAccessibleName(swatchesPane, "Theme colors") != null
            && findDescendant(swatchesPane, com.owspfm.elwha.buttongroup.ElwhaButtonGroup.class)
                == null);

    picker.setSwatchSources(SwatchSource.THEME, SwatchSource.MATERIAL);
    check("reorder applies, first becomes active", picker.getSwatchSource() == SwatchSource.THEME);

    expectThrow("empty sources rejected", picker::setSwatchSources);
    expectThrow(
        "duplicate sources rejected",
        () -> picker.setSwatchSources(SwatchSource.THEME, SwatchSource.THEME));
    expectThrow(
        "null source rejected",
        () -> picker.setSwatchSources(SwatchSource.MATERIAL, (SwatchSource) null));
    picker.setSwatchSources(SwatchSource.MATERIAL);
    expectThrow("unoffered source rejected", () -> picker.setSwatchSource(SwatchSource.THEME));
  }

  private static void checkThemeGrid() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x123456));
    picker.setModes(PickerMode.SWATCHES);
    picker.setSwatchSource(SwatchSource.THEME);
    layoutTree(picker, new Dimension(360, 460));

    final Component grid = findByAccessibleName(picker, "Theme colors");
    check("theme grid present", grid != null);
    final int pitch = grid.getWidth() / 10;
    pressCell(grid, pitch / 2, 17);
    final Color expected = ColorRole.values()[0].resolve();
    check(
        "cell 0 commits the resolved PRIMARY",
        (picker.getColor().getRGB() & 0xFFFFFF) == (expected.getRGB() & 0xFFFFFF));
    check(
        "committed color is a plain Color, not a UIResource",
        picker.getColor().getClass() == Color.class);

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    final Color darkPrimary = ColorRole.values()[0].resolve();
    check("mode flip re-resolves PRIMARY", darkPrimary.getRGB() != expected.getRGB());
    pressCell(grid, pitch / 2, 17);
    check(
        "cell 0 commits the re-resolved PRIMARY after the flip",
        (picker.getColor().getRGB() & 0xFFFFFF) == (darkPrimary.getRGB() & 0xFFFFFF));

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    final ElwhaColorPicker alphaPicker = new ElwhaColorPicker();
    alphaPicker.setModes(PickerMode.SWATCHES);
    alphaPicker.setAlphaEnabled(true);
    alphaPicker.setColor(new Color(10, 20, 30, 128));
    alphaPicker.setSwatchSource(SwatchSource.THEME);
    layoutTree(alphaPicker, new Dimension(360, 460));
    final Component alphaGrid = findByAccessibleName(alphaPicker, "Theme colors");
    pressCell(alphaGrid, alphaGrid.getWidth() / 20, 17);
    check("role pick preserves current alpha", alphaPicker.getColor().getAlpha() == 128);

    Component swatches = grid;
    while (swatches != null && !(swatches instanceof ColorPickerPane)) {
      swatches = swatches.getParent();
    }
    check(
        "swatches pane stays inside the height budget",
        swatches != null && swatches.getPreferredSize().height <= 230);
  }

  private static void pressCell(final Component grid, final int x, final int y) {
    grid.dispatchEvent(
        new java.awt.event.MouseEvent(
            grid,
            java.awt.event.MouseEvent.MOUSE_PRESSED,
            0L,
            0,
            x,
            y,
            1,
            false,
            java.awt.event.MouseEvent.BUTTON1));
    grid.dispatchEvent(
        new java.awt.event.MouseEvent(
            grid,
            java.awt.event.MouseEvent.MOUSE_RELEASED,
            0L,
            0,
            x,
            y,
            1,
            false,
            java.awt.event.MouseEvent.BUTTON1));
  }

  private static Component findByAccessibleName(final Component root, final String name) {
    final AccessibleContext context = root.getAccessibleContext();
    if (context != null && name.equals(context.getAccessibleName())) {
      return root;
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        final Component found = findByAccessibleName(child, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static <T> T findDescendant(final Component root, final Class<T> type) {
    if (type.isInstance(root)) {
      return type.cast(root);
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        final T found = findDescendant(child, type);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void expectThrow(final String message, final Runnable body) {
    try {
      body.run();
      check(message, false);
    } catch (final IllegalArgumentException expected) {
      // The contract under test.
    }
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
