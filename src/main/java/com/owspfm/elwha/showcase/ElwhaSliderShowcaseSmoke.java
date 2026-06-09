package com.owspfm.elwha.showcase;

import com.owspfm.elwha.card.v1.playground.ElwhaCardListShowcase;
import com.owspfm.elwha.card.v1.playground.LiveConfigPanel;
import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;

/**
 * Headless guard for the Showcase Slider leaf (stories #346 / #351 / #371). Builds the {@link
 * SliderShowcasePanels} Workbench + Gallery leaf surfaces and constructs the two dogfooded {@code
 * card/v1/playground} panels, asserting they build without a display, that the gallery now spans
 * the standard + centered + range + size + inset-icon + vertical 14×5 state matrix (with the
 * original nine config rows still {@code XS} and the new rows scaling up / carrying an inset icon /
 * transposing to vertical), and that the migrated controls are now {@link ElwhaSlider}s (zero raw
 * {@code JSlider} left in those panels).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderShowcaseSmoke {

  private ElwhaSliderShowcaseSmoke() {}

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

    final JComponent workbench = SliderShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    check("workbench contains a live ElwhaSlider", countSliders(workbench) >= 1);

    final JComponent gallery = SliderShowcasePanels.buildGallery();
    check("gallery builds", gallery != null);
    // 14 configs (3 standard + 3 centered + 3 range + M + XL + inset + 2 vertical) × 5 states = 70.
    check("gallery is the 14×5 state matrix", countSliders(gallery) == 70);
    // 3 horizontal centered config rows + 1 vertical centered row × 5 states = 20 CENTERED sliders.
    check("gallery includes the centered variant rows", countCentered(gallery) == 20);
    // The 3 range config rows × 5 states = 15 RANGE sliders (no vertical range row — doc-warned).
    check(
        "gallery includes the range variant rows",
        countVariant(gallery, ElwhaSlider.Variant.RANGE) == 15);
    // 9 original rows + 2 vertical rows stay XS (visually unchanged); the size/inset rows scale up.
    check(
        "the XS rows total 11 (9 original + 2 vertical)",
        countSize(gallery, ElwhaSlider.Size.XS) == 55);
    // Size rows: one M row + one XL row, 5 states each.
    check("gallery includes a Size M row", countSize(gallery, ElwhaSlider.Size.M) == 5);
    check("gallery includes a Size XL row", countSize(gallery, ElwhaSlider.Size.XL) == 5);
    // Inset-icon row: a standard L slider carrying an inset icon, 5 states.
    check("gallery includes an inset-icon row", countInsetIcon(gallery) == 5);
    // Two vertical rows (standard + centered) × 5 states = 10 VERTICAL sliders.
    check(
        "gallery includes the vertical orientation rows",
        countOrientation(gallery, ElwhaSlider.Orientation.VERTICAL) == 10);

    final LiveConfigPanel liveConfig = new LiveConfigPanel();
    check("LiveConfigPanel dogfood builds", liveConfig != null);
    check("LiveConfigPanel uses 4 ElwhaSliders", countSliders(liveConfig) == 4);

    // ElwhaCardListShowcase pulls the system clipboard at construction, so it cannot be built in a
    // headless JVM; verify its migration reflectively over the declared field types instead.
    int elwhaSliderFields = 0;
    int rawJSliderFields = 0;
    for (final var field : ElwhaCardListShowcase.class.getDeclaredFields()) {
      if (field.getType() == ElwhaSlider.class) {
        elwhaSliderFields++;
      }
      if (field.getType() == javax.swing.JSlider.class) {
        rawJSliderFields++;
      }
    }
    check("ElwhaCardListShowcase migrated 4 sliders to ElwhaSlider", elwhaSliderFields == 4);
    check("ElwhaCardListShowcase has no raw JSlider field", rawJSliderFields == 0);

    System.out.println("ElwhaSliderShowcaseSmoke: OK (leaf panels + dogfood migration)");
  }

  private static int countSliders(final Component root) {
    int count = root instanceof ElwhaSlider ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countSliders(child);
      }
    }
    return count;
  }

  private static int countCentered(final Component root) {
    return countVariant(root, ElwhaSlider.Variant.CENTERED);
  }

  private static int countVariant(final Component root, final ElwhaSlider.Variant variant) {
    int count = (root instanceof ElwhaSlider s && s.getVariant() == variant) ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countVariant(child, variant);
      }
    }
    return count;
  }

  private static int countSize(final Component root, final ElwhaSlider.Size size) {
    int count = (root instanceof ElwhaSlider s && s.getSizeVariant() == size) ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countSize(child, size);
      }
    }
    return count;
  }

  private static int countOrientation(
      final Component root, final ElwhaSlider.Orientation orientation) {
    int count = (root instanceof ElwhaSlider s && s.getOrientation() == orientation) ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countOrientation(child, orientation);
      }
    }
    return count;
  }

  private static int countInsetIcon(final Component root) {
    int count = (root instanceof ElwhaSlider s && s.getInsetIcon() != null) ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countInsetIcon(child);
      }
    }
    return count;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
