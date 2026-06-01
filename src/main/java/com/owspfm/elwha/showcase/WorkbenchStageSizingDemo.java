package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.buttongroup.ButtonGroupVariant;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JComponent;

/**
 * Headless verification harness for the workbench stage-sizing fix ([#179]). Not an interactive
 * playground — it lays the workbench out at a sweep of host widths and exits non-zero on any
 * failure, so it doubles as a smoke gate.
 *
 * <p>The reported bug — the stage surface "hugs" the live component at narrow window widths and
 * "pops" to a comfortable size when widened — was diagnosed <em>not</em> to be the issue's
 * hypothesised stale-preferred-size: the live component's preferred size is identical at every
 * width. The real cause is that the surface's preferred size carries the {@code STAGE_FIT_MARGIN}
 * breathing room but its minimum (inherited from the GridBag-centered live component, whose minimum
 * equals its marginless preferred) does not, so a cramped {@code GridBagLayout} shrinks the surface
 * toward — and even below — that minimum. The fix puts the stage in a scroll pane whose {@code
 * Scrollable} view fills the viewport when there is room and holds its preferred size (scrolling)
 * when cramped, so the surface never shrinks.
 *
 * <p>Asserts: (1) the surface preferred size reserves margin beyond the live component, and (2) the
 * surface's laid-out size stays at its preferred size across a wide→narrow host sweep — the widths
 * at which the pre-fix surface collapsed.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class WorkbenchStageSizingDemo {

  private WorkbenchStageSizingDemo() {}

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final ElwhaButtonGroup group =
        new ElwhaButtonGroup()
            .setVariant(ButtonGroupVariant.CONNECTED)
            .setButtonSize(ButtonSize.XL)
            .add("One", "Two", "Three");

    final ComponentWorkbench wb = new ComponentWorkbench();
    wb.setStage(group);
    final ElwhaSurface surface = findSurface(wb);

    final Dimension surfacePref = surface.getPreferredSize();
    final Dimension groupPref = group.getPreferredSize();
    check(
        "surface preferred reserves breathing room beyond the live component ("
            + surfacePref.width
            + " > "
            + groupPref.width
            + ")",
        surfacePref.width > groupPref.width && surfacePref.height > groupPref.height);

    // Sweep from comfortably wide down to narrow — the band where the pre-fix surface collapsed
    // (786 → 658 → 593 → … → 433). The surface must hold its preferred size throughout.
    for (final int hostWidth : new int[] {1400, 1000, 800, 600, 480}) {
      layoutAt(wb, hostWidth, 900);
      final Dimension actual = surface.getSize();
      check(
          "host=" + hostWidth + ": surface holds its preferred size (got " + actual.width + " px)",
          actual.equals(surfacePref));
    }

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: all workbench stage-sizing checks passed.");
  }

  private static void layoutAt(final JComponent root, final int w, final int h) {
    root.setSize(w, h);
    layoutTree(root);
  }

  private static void layoutTree(final Component c) {
    if (c instanceof Container container) {
      container.doLayout();
      for (final Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static ElwhaSurface findSurface(final Container root) {
    for (final Component c : root.getComponents()) {
      if (c instanceof ElwhaSurface s) {
        return s;
      }
      if (c instanceof Container child) {
        final ElwhaSurface found = findSurface(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
