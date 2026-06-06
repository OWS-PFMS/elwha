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
 * Headless guard for the Showcase Slider leaf (stories #346 / #351). Builds the {@link
 * SliderShowcasePanels} Workbench + Gallery leaf surfaces and constructs the two dogfooded {@code
 * card/v1/playground} panels, asserting they build without a display, that the gallery now spans
 * the standard + centered 6×5 state matrix (with live {@code CENTERED} sliders), and that the
 * migrated controls are now {@link ElwhaSlider}s (zero raw {@code JSlider} left in those panels).
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
    // 6 configs (3 standard + 3 centered) × 5 states = 30 gallery sliders.
    check("gallery is the 6×5 state matrix", countSliders(gallery) == 30);
    // The 3 centered config rows × 5 states = 15 CENTERED sliders.
    check("gallery includes the centered variant rows", countCentered(gallery) == 15);

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
    int count =
        (root instanceof ElwhaSlider s && s.getVariant() == ElwhaSlider.Variant.CENTERED) ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countCentered(child);
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
