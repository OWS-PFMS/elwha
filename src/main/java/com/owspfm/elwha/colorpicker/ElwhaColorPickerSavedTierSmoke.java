package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;

/**
 * V2 S3 headless guard for the SAVED tier (#499): the favorites model contract (copy/dedupe/cap 30,
 * null rejection, silent no-ops, listener fires only on actual mutation), survival across the alpha
 * rebuild, the grid's gesture-path commit of the exact saved color, the Delete-key removal binding,
 * and an empty-state paint pass.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSavedTierSmoke {

  private ElwhaColorPickerSavedTierSmoke() {}

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
    checkGrid();

    System.out.println(
        "ElwhaColorPickerSavedTierSmoke: OK (model contract, listener discipline, gesture commit,"
            + " delete binding, empty paint)");
  }

  private static void checkModel() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    check(
        "default offers all three sources",
        List.of(SwatchSource.MATERIAL, SwatchSource.THEME, SwatchSource.SAVED)
            .equals(picker.getSwatchSources()));
    check("favorites start empty", picker.getFavorites().isEmpty());

    final AtomicInteger fires = new AtomicInteger();
    picker.addFavoritesListener(e -> fires.incrementAndGet());

    picker.addFavorite(new Color(0x111111));
    check("add fires once", fires.get() == 1 && picker.getFavorites().size() == 1);
    picker.addFavorite(new Color(0x111111));
    check("duplicate add is a silent no-op", fires.get() == 1 && picker.getFavorites().size() == 1);
    picker.removeFavorite(new Color(0x222222));
    check("absent remove is a silent no-op", fires.get() == 1);
    picker.removeFavorite(new Color(0x111111));
    check("remove fires once", fires.get() == 2 && picker.getFavorites().isEmpty());

    final List<Color> oversized = new ArrayList<>();
    for (int i = 0; i < 35; i++) {
      oversized.add(new Color(i + 1));
    }
    oversized.add(new Color(1));
    picker.setFavorites(oversized);
    check(
        "setFavorites dedupes and caps at 30",
        fires.get() == 3 && picker.getFavorites().size() == 30);

    picker.setFavorites(List.of(new Color(0x111111)));
    picker.addFavorite(new Color(0x333333));
    final int before = fires.get();
    picker.setAlphaEnabled(true);
    check(
        "favorites survive the alpha rebuild",
        picker.getFavorites().size() == 2 && fires.get() == before);

    expectThrow("null list rejected", () -> picker.setFavorites(null));
    expectThrow(
        "null entry rejected",
        () -> picker.setFavorites(java.util.Arrays.asList(new Color(1), null)));
    expectThrow("null add rejected", () -> picker.addFavorite(null));
  }

  private static void checkGrid() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF7043));
    picker.setModes(PickerMode.SWATCHES);
    picker.setFavorites(List.of(new Color(0x6750A4), new Color(0x2E7D32)));
    picker.setSwatchSource(SwatchSource.SAVED);
    layoutTree(picker, new Dimension(360, 460));

    final Component grid = findByAccessibleName(picker, "Saved colors");
    check("favorites grid present", grid != null);
    final int pitch = grid.getWidth() / 10;
    pressCell(grid, pitch / 2, 17);
    check("cell 0 commits the exact saved color", new Color(0x6750A4).equals(picker.getColor()));

    final javax.swing.Action delete = ((JComponent) grid).getActionMap().get("pressed DELETE");
    check("delete binding installed", delete != null);
    delete.actionPerformed(new java.awt.event.ActionEvent(grid, 0, "delete"));
    check(
        "delete removes the focused favorite",
        List.of(new Color(0x2E7D32)).equals(picker.getFavorites()));

    picker.setFavorites(List.of());
    final BufferedImage image =
        new BufferedImage(picker.getWidth(), picker.getHeight(), BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    picker.paint(g2);
    g2.dispose();
    check("empty-state paints without error", true);
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
