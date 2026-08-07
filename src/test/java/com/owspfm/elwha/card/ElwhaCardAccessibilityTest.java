package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import javax.accessibility.AccessibleRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the card's accessible shape (spec §16) — the chassis role that follows the
 * actionability gate, the selected-state announcement, and the §16.3 traversal rule that a card
 * being inert does not make the affordances inside it unreachable.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardAccessibilityTest {

  @Test
  void anInertCardIsAPanel() {
    assertThat(ElwhaCard.filledCard().getAccessibleContext().getAccessibleRole())
        .as("§16.1 — a card that does nothing on click is a grouping, not a control")
        .isEqualTo(AccessibleRole.PANEL);
  }

  @Test
  void anActionableCardIsAPushButton() {
    assertThat(
            ElwhaCard.filledCard().setActionable(true).getAccessibleContext().getAccessibleRole())
        .as("§16.1 — the whole card is the click target, so it announces as one button")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void theRoleFollowsTheGateInBothDirections() {
    final ElwhaCard card = ElwhaCard.filledCard().setActionable(true);

    card.setActionable(false);

    assertThat(card.getAccessibleContext().getAccessibleRole())
        .as("the same context re-resolves rather than freezing the first answer")
        .isEqualTo(AccessibleRole.PANEL);
  }

  @Test
  void aSelectedCardSaysSoInItsName() {
    final ElwhaCard card = ElwhaCard.filledCard().setSelectable(true);
    card.getAccessibleContext().setAccessibleName("Recent activity");

    card.setSelected(true);

    assertThat(card.getAccessibleContext().getAccessibleName())
        .as("§13 — selection is announced, not left as a purely visual badge")
        .isEqualTo("Recent activity (selected)");
  }

  @Test
  void deselectingDropsTheSuffixAgain() {
    final ElwhaCard card = ElwhaCard.filledCard().setSelectable(true);
    card.getAccessibleContext().setAccessibleName("Recent activity");
    card.setSelected(true);

    card.setSelected(false);

    assertThat(card.getAccessibleContext().getAccessibleName())
        .as("the suffix tracks the state rather than sticking after the first selection")
        .isEqualTo("Recent activity");
  }

  @Test
  void anUnnamedSelectedCardInventsNoName() {
    final ElwhaCard card = ElwhaCard.filledCard().setSelectable(true);

    card.setSelected(true);

    assertThat(card.getAccessibleContext().getAccessibleName())
        .as("a card with nothing to announce stays silent rather than announcing '(selected)'")
        .isNull();
  }

  @Test
  void theCardHandsOutOneAccessibleContext() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThat(card.getAccessibleContext())
        .as("a cached context is what lets assistive technology hold a stable reference")
        .isSameAs(card.getAccessibleContext());
  }

  @Test
  void anInertCardStillLetsItsAffordancesBeReached() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final ElwhaIconButton overflow = new ElwhaIconButton(MaterialIcons.moreVert());
    card.add(new ElwhaCardHeader().setTitle("Title").addTrailing(overflow));

    assertThat(card.isFocusable())
        .as("§16.3 — the inert chassis itself is not a tab stop")
        .isFalse();
    assertThat(overflow.isFocusable())
        .as("but the controls inside it keep theirs, so the header stays operable")
        .isTrue();
  }

  @Test
  void theTextAtomsAnnounceTheirContent() {
    final ElwhaCardTitle title = new ElwhaCardTitle("Recent activity");
    final ElwhaCardSupportingText body = new ElwhaCardSupportingText("12 cycles found");

    assertThat(title.getAccessibleContext().getAccessibleRole())
        .as("§16.2 — the title reads as a label")
        .isEqualTo(AccessibleRole.LABEL);
    assertThat(title.getAccessibleContext().getAccessibleName())
        .as("carrying its own text, markup wrapper and all")
        .contains("Recent activity");
    assertThat(body.getAccessibleContext().getAccessibleName())
        .as("and supporting text announces its copy too")
        .contains("12 cycles found");
  }
}
