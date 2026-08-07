package com.owspfm.elwha.appbar;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import javax.accessibility.AccessibleRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the app bar's accessibility surface — design doc §9: the bar is structural
 * chrome, so it reports a plain {@link AccessibleRole#PANEL} named after its own title, and adds no
 * interactive semantics of its own. Every control in it is an {@code ElwhaIconButton} carrying its
 * own role and action.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaAppBarAccessibilityTest {

  @Test
  void barIsAStructuralPanel() {
    assertThat(ElwhaAppBar.small().getAccessibleContext().getAccessibleRole())
        .as("§9 — the bar groups chrome; it is not itself a control")
        .isEqualTo(AccessibleRole.PANEL);
  }

  @Test
  void barCarriesNoKeyboardBindingsOfItsOwn() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    assertThat(bar.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).allKeys())
        .as("§9 — the bar adds no interactive semantics; its buttons carry the keyboard surface")
        .isNullOrEmpty();
    assertThat(bar.getActionMap().allKeys()).isNullOrEmpty();
  }

  @Test
  void barTakesItsNameFromItsTitle() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    bar.setTitle("Inbox");

    assertThat(bar.getAccessibleContext().getAccessibleName())
        .as("§9 — the title is what identifies the screen, so it names the region")
        .isEqualTo("Inbox");
  }

  @Test
  void aSubtitleIsAppendedToTheName() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");

    bar.setSubtitle("12 unread");

    assertThat(bar.getAccessibleContext().getAccessibleName())
        .as("§9 — both lines are announced, separated as one phrase")
        .isEqualTo("Inbox, 12 unread");
  }

  @Test
  void aSubtitleAloneNamesTheBarByItself() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    bar.setSubtitle("12 unread");

    assertThat(bar.getAccessibleContext().getAccessibleName())
        .as("§9 — no leading comma when there is no title in front of it")
        .isEqualTo("12 unread");
  }

  @Test
  void nameFollowsALaterTitleChange() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");

    bar.setTitle("Archive");

    assertThat(bar.getAccessibleContext().getAccessibleName())
        .as("§9 — the name is computed live, so a screen change is announced correctly")
        .isEqualTo("Archive");
  }

  @Test
  void clearingTheSubtitleShortensTheName() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSubtitle("12 unread");

    bar.setSubtitle(null);

    assertThat(bar.getAccessibleContext().getAccessibleName()).isEqualTo("Inbox");
  }

  @Test
  void anUntitledBarHasNoNameToReport() {
    assertThat(ElwhaAppBar.small().getAccessibleContext().getAccessibleName())
        .as("§9 — an empty computed name is reported as absent, not as an empty string")
        .isNull();
  }

  @Test
  void aConsumerSuppliedNameWinsOverTheTitle() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");

    bar.getAccessibleContext().setAccessibleName("Mail navigation");

    assertThat(bar.getAccessibleContext().getAccessibleName())
        .as("§9 — the derived name is a fallback, not a ceiling")
        .isEqualTo("Mail navigation");
  }

  @Test
  void slottedButtonsKeepTheirOwnAccessibleIdentity() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    final ElwhaIconButton nav =
        bar.setNavigationIcon(
            MaterialIcons.symbol("menu").unselected(), "Open navigation", e -> {});
    final ElwhaIconButton action =
        bar.addAction(MaterialIcons.symbol("close").unselected(), "Dismiss", e -> {});

    assertThat(nav.getAccessibleContext().getAccessibleName())
        .as("§9 — the bar adds no a11y of its own to the controls it hosts")
        .isEqualTo("Open navigation");
    assertThat(action.getAccessibleContext().getAccessibleName()).isEqualTo("Dismiss");
    assertThat(bar.getAccessibleContext().getAccessibleName())
        .as("and its own name is unaffected by what it hosts")
        .isEqualTo("Inbox");
  }

  @Test
  void contextIsStableAcrossCalls() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    assertThat(bar.getAccessibleContext())
        .as("AT holds onto the context, so it must not be rebuilt per call")
        .isSameAs(bar.getAccessibleContext());
  }
}
