package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.showcase.ElwhaShowcase.LeafEntry;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The Showcase must never trip the library's own accessibility advisories — the storefront is the
 * first thing a consumer runs, and a library warning about its own demo is the #769 failure mode.
 * Sweeps every mounted catalog surface plus the Showcase's own rail for the components that carry
 * the first-paint missing-accessible-name advisory, and for the rail's 3–7 primary-destination
 * recommendation.
 *
 * <p>The checks read the same public name resolution the advisories consult, so a violation here is
 * exactly a warning a Showcase visitor would see in their console.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseAccessibleNameGuardTest {

  private interface SurfaceVisitor {
    void visit(String surfaceLabel, Component component);
  }

  private static void walkMountedSurfaces(final SurfaceVisitor visitor) {
    for (final LeafEntry leaf : ShowcaseFixture.leaves()) {
      for (final Component component : ShowcaseFixture.descendants(leaf.surface)) {
        visitor.visit(leaf.label, component);
      }
    }
    for (final Component component : ShowcaseFixture.descendants(ShowcaseFixture.rail())) {
      visitor.visit("Showcase rail", component);
    }
  }

  private static String describe(final String surfaceLabel, final JComponent component) {
    return surfaceLabel
        + " → "
        + component.getClass().getSimpleName()
        + " (name="
        + component.getName()
        + ", tooltip="
        + component.getToolTipText()
        + ")";
  }

  @Test
  void everyMountedIconOnlyInteractiveResolvesAMeaningfulName() {
    final List<String> unnamed = new ArrayList<>();
    walkMountedSurfaces(
        (label, component) -> {
          if (component instanceof ElwhaIconButton button
              && "Icon button".equals(button.getAccessibleContext().getAccessibleName())) {
            unnamed.add(describe(label, button));
          }
          if (component instanceof ElwhaFab fab
              && "Floating action button".equals(fab.getAccessibleContext().getAccessibleName())) {
            unnamed.add(describe(label, fab));
          }
          if (component instanceof ElwhaSwitch toggle) {
            final String name = toggle.getAccessibleContext().getAccessibleName();
            if (name == null || name.isEmpty()) {
              unnamed.add(describe(label, toggle));
            }
          }
        });

    assertThat(unnamed)
        .as("every icon-only interactive the Showcase mounts must resolve a meaningful name")
        .isEmpty();
  }

  @Test
  void everyMountedRailIsNamedAndKeepsItsPrimariesInTheRecommendedRange() {
    final List<String> violations = new ArrayList<>();
    walkMountedSurfaces(
        (label, component) -> {
          if (!(component instanceof ElwhaNavigationRail rail)) {
            return;
          }
          final String name = rail.getAccessibleContext().getAccessibleName();
          if (name == null || name.isEmpty()) {
            violations.add("unnamed rail: " + describe(label, rail));
          }
          final int primaries = rail.getPrimary().size();
          if (primaries != 0 && (primaries < 3 || primaries > 7)) {
            violations.add(
                label + " → rail \"" + name + "\" carries " + primaries + " primary destinations");
          }
        });

    assertThat(violations)
        .as("no mounted rail may trip the rail's own name or 3–7 destination advisories (#769)")
        .isEmpty();
  }
}
