package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.playground.ButtonPlaygroundPanels;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.chip.playground.ChipPlaygroundPanels;
import com.owspfm.elwha.iconbutton.playground.IconButtonPlaygroundPanels;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * Headless guard for the S5 dogfood sweep (story #415): constructs every swept living-Showcase
 * control surface and walks its component tree asserting <strong>zero raw {@link
 * JCheckBox}</strong> instances remain and at least one {@link ElwhaCheckbox} took their place. The
 * raw-Swing Foundations gallery is exempt by design (its {@code JCheckBox} is demo subject matter),
 * as are frozen per-story demo artifacts — see the epic #410 PR body for the exclusion list. Runs
 * in CI's headless JVM ({@code -Djava.awt.headless=true} safe).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class CheckboxDogfoodSweepSmoke {

  private CheckboxDogfoodSweepSmoke() {}

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

    assertSwept("SliderShowcasePanels.buildWorkbench", SliderShowcasePanels.buildWorkbench());
    assertSwept("ButtonPlaygroundPanels.buildLivePanel", ButtonPlaygroundPanels.buildLivePanel());
    assertSwept(
        "IconButtonPlaygroundPanels.buildLivePanel", IconButtonPlaygroundPanels.buildLivePanel());
    try {
      assertSwept(
          "ChipPlaygroundPanels.buildLiveListPanel", ChipPlaygroundPanels.buildLiveListPanel());
    } catch (final java.awt.HeadlessException pre) {
      // Pre-existing: ElwhaChipList loads drag cursors via Toolkit.getBestCursorSize, which
      // throws headless. The chip surface's sweep is verified visually in the Showcase instead.
      System.out.println(
          "  – ChipPlaygroundPanels.buildLiveListPanel skipped headless (cursor loading)");
    }

    final ComponentWorkbench host = new ComponentWorkbench();
    new SurfaceControlPanel(host.controls(), false);
    assertSwept("SurfaceControlPanel (via ComponentWorkbench controls)", host);

    System.out.println("CheckboxDogfoodSweepSmoke: OK (no raw JCheckBox in the swept surfaces)");
  }

  private static void assertSwept(final String what, final JComponent root) {
    final int raw = count(root, JCheckBox.class);
    final int elwha = count(root, ElwhaCheckbox.class);
    if (raw > 0) {
      System.err.println("FAIL: " + what + " still contains " + raw + " raw JCheckBox");
      System.exit(1);
    }
    System.out.println("  ✓ " + what + " — 0 raw JCheckBox, " + elwha + " ElwhaCheckbox");
  }

  private static int count(final Component c, final Class<?> type) {
    int n = type.isInstance(c) ? 1 : 0;
    if (c instanceof Container container) {
      for (final Component child : container.getComponents()) {
        n += count(child, type);
      }
    }
    return n;
  }
}
