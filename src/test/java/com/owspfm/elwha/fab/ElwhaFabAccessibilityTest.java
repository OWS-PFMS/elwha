package com.owspfm.elwha.fab;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A accessibility-shape coverage for {@link ElwhaFab} — the §10.1 role and the §10.4 name
 * chain, in which the Extended form's own label outranks a tooltip because M3 makes that label the
 * accessible name automatically.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaFabAccessibilityTest {

  @Test
  void bothFormsExposeThePushButtonRole() {
    assertThat(ElwhaFab.standard(MaterialIcons.add()).getAccessibleContext().getAccessibleRole())
        .as("§10.1 — a Standard FAB is a push button")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
    assertThat(ElwhaFab.extended("Compose").getAccessibleContext().getAccessibleRole())
        .as("and so is an Extended one")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void anExtendedFabsLabelIsAutomaticallyItsAccessibleName() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");

    assertThat(fab.getAccessibleContext().getAccessibleName())
        .as("§10.4 — the visible label needs no extra wiring to be announced")
        .isEqualTo("Compose");
  }

  @Test
  void extendedLabelOutranksATooltip() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    fab.setToolTipText("Write a new message");

    assertThat(fab.getAccessibleContext().getAccessibleName())
        .as("§10.4 puts the label above the tooltip, unlike the icon-button chain")
        .isEqualTo("Compose");
  }

  @Test
  void aDeclaredNameOutranksEverything() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    fab.getAccessibleContext().setAccessibleName("Compose a new message");

    assertThat(fab.getAccessibleContext().getAccessibleName())
        .as("an explicit name is always the consumer's final say")
        .isEqualTo("Compose a new message");
  }

  @Test
  void aStandardFabFallsThroughTooltipThenComponentNameThenALastResort() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());
    assertThat(fab.getAccessibleContext().getAccessibleName())
        .as("an unlabeled icon-only FAB still announces something")
        .isEqualTo("Floating action button");

    fab.setName("composeFab");
    assertThat(fab.getAccessibleContext().getAccessibleName())
        .as("the component name is picked up when nothing better exists")
        .isEqualTo("composeFab");

    fab.setToolTipText("Compose");
    assertThat(fab.getAccessibleContext().getAccessibleName())
        .as("§10.4 — the tooltip doubles as the M3 hover tooltip and the name fallback")
        .isEqualTo("Compose");
  }

  @Test
  void enabledStateTracksTheComponent() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());
    assertThat(fab.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.ENABLED))
        .as("an enabled FAB reports ENABLED")
        .isTrue();

    fab.setEnabled(false);

    assertThat(fab.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.ENABLED))
        .as("a disabled FAB drops ENABLED so assistive tech can skip it")
        .isFalse();
  }
}
