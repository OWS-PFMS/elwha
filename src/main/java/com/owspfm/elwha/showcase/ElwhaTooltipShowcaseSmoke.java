package com.owspfm.elwha.showcase;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.tooltip.TooltipVariant;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * Headless guard for #452 — the Tooltip Showcase leaf: the tabbed leaf surface constructs with
 * Workbench + Gallery tabs, the gallery's {@code renderPreview()} tiles lay out and paint into a
 * {@link BufferedImage} without throwing (light and dark), and the workbench's variant control
 * applies (flipping PLAIN → RICH rebuilds without throwing).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTooltipShowcaseSmoke {

  private static int checks;
  private static int failures;

  private ElwhaTooltipShowcaseSmoke() {}

  /**
   * Runs the guard; exits non-zero on any failure.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    MorphAnimator.setReducedMotion(true);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final JComponent component = TooltipShowcasePanels.buildComponent();
    check(component instanceof JTabbedPane, "leaf surface is a tabbed pane");
    check(((JTabbedPane) component).getTabCount() == 2, "Workbench + Gallery tabs present");

    check(renderGallery(), "gallery renders to an image (light)");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    check(renderGallery(), "gallery renders to an image (dark)");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final JComponent workbench = TooltipShowcasePanels.buildWorkbench();
    check(workbench != null, "workbench constructs");
    final JComboBox<?> variantBox = findVariantBox(workbench);
    check(variantBox != null, "variant control present");
    boolean applied = true;
    try {
      if (variantBox != null) {
        variantBox.setSelectedItem(TooltipVariant.RICH);
      }
    } catch (final RuntimeException e) {
      applied = false;
    }
    check(applied, "flipping the variant to RICH applies without throwing");

    System.out.println(
        failures == 0 ? "PASS — " + checks + " checks" : "FAIL — " + failures + "/" + checks);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static boolean renderGallery() {
    try {
      final JComponent gallery = TooltipShowcasePanels.buildGallery();
      final Dimension pref = gallery.getPreferredSize();
      gallery.setSize(Math.max(pref.width, 480), Math.max(pref.height, 480));
      gallery.doLayout();
      layoutTree(gallery);
      final BufferedImage img =
          new BufferedImage(gallery.getWidth(), gallery.getHeight(), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g2 = img.createGraphics();
      gallery.paint(g2);
      g2.dispose();
      return true;
    } catch (final RuntimeException e) {
      e.printStackTrace();
      return false;
    }
  }

  private static void layoutTree(final Container root) {
    root.doLayout();
    for (final Component child : root.getComponents()) {
      if (child instanceof Container container) {
        layoutTree(container);
      }
    }
  }

  private static JComboBox<?> findVariantBox(final Container root) {
    for (final Component child : root.getComponents()) {
      if (child instanceof JComboBox<?> box
          && box.getItemCount() > 0
          && box.getItemAt(0) instanceof TooltipVariant) {
        return box;
      }
      if (child instanceof Container container) {
        final JComboBox<?> nested = findVariantBox(container);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  private static void check(final boolean ok, final String label) {
    checks++;
    if (!ok) {
      failures++;
      System.out.println("FAIL: " + label);
    } else {
      System.out.println("  ok: " + label);
    }
  }
}
