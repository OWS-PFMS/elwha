package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.list.DefaultElwhaListModel;
import com.owspfm.elwha.list.ElwhaItemList;
import com.owspfm.elwha.list.ElwhaListItemView;
import com.owspfm.elwha.list.SelectionMode;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaCard} as an {@link ElwhaListItemView} — the capability the unified
 * list needs from a hosted component, exercised both through the interface reference and through a
 * real {@link ElwhaItemList} rendering cards. The card side of the same contract {@code
 * ElwhaChipListItemViewTest} pins for chips.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardListItemViewTest {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 320;

  private static ElwhaItemList<String> cardListOf(final String... items) {
    final ElwhaItemList<String> list =
        new ElwhaItemList<>(
            new DefaultElwhaListModel<>(List.of(items)),
            (item, index) -> {
              final ElwhaCard card = ElwhaCard.filledCard();
              card.add(new ElwhaCardSupportingText(item));
              return card;
            });
    list.setSize(WIDTH, HEIGHT);
    list.doLayout();
    return list;
  }

  // ------------------------------------------------------ interface shape

  @Test
  void everyCapabilityMutatorReturnsTheCardItself() {
    final ElwhaListItemView view = ElwhaCard.filledCard();

    assertThat(view.setSelected(true))
        .as("the covariant override keeps the card's own builder chain usable")
        .isInstanceOf(ElwhaCard.class);
    assertThat(view.setListInteractive(true)).as("as does the handover").isSameAs(view);
    assertThat(view.cancelPendingClick()).as("and the click cancel").isSameAs(view);
    assertThat(view.setDragged(true)).as("and the drag chrome").isSameAs(view);
  }

  @Test
  void theListReadsAndWritesSelectionThroughTheInterface() {
    final ElwhaListItemView view = ElwhaCard.filledCard();
    view.setListInteractive(true);

    view.setSelected(true);

    assertThat(view.isSelected())
        .as("what the list wrote is what the view reports painting")
        .isTrue();
  }

  @Test
  void draggedChromeIsRealOnACardRatherThanTheInterfaceDefault() {
    final ElwhaCard card = ElwhaCard.filledCard();

    card.setDragged(true);

    assertThat(card.isDragged())
        .as(
            "a card has elevation to lift, so it implements the optional hook rather than"
                + " no-opping")
        .isTrue();
    assertThat(card.currentElevationForPaint())
        .as("and the lift is visible in what it paints")
        .isGreaterThan(ElwhaCard.defaultElevationFor(CardVariant.FILLED));
  }

  // -------------------------------------------------------- hosted in a list

  @Test
  void aListClaimsInteractionOnEveryRenderedCard() {
    final ElwhaItemList<String> list = cardListOf("a", "b");

    list.setSelectionMode(SelectionMode.SINGLE);

    assertThat(((ElwhaCard) list.getComponentFor("a")).isActionable())
        .as("the list needs a click surface on each item, so it forces one on")
        .isTrue();
  }

  @Test
  void aListDoesNotClaimCardsWhileSelectionIsOff() {
    final ElwhaItemList<String> list = cardListOf("a", "b");

    assertThat(((ElwhaCard) list.getComponentFor("a")).isActionable())
        .as("a display-only list leaves the cards exactly as the adapter built them")
        .isFalse();
  }

  @Test
  void clickingAHostedCardSelectsItThroughTheList() {
    final ElwhaItemList<String> list = cardListOf("a", "b");
    list.setSelectionMode(SelectionMode.SINGLE);

    Input.click(list.getComponentFor("b"), 8, 8);

    assertThat(list.getSelectionModel().getSelected())
        .as("the list owns the gesture and records the selection")
        .containsExactly("b");
    assertThat(((ElwhaCard) list.getComponentFor("b")).isSelected())
        .as("and pushes the result back onto the card's chrome")
        .isTrue();
  }

  @Test
  void aHostedCardDoesNotSelfToggleAheadOfTheList() {
    final ElwhaItemList<String> list = cardListOf("a", "b");
    list.setSelectionMode(SelectionMode.SINGLE);
    final ElwhaCard card = (ElwhaCard) list.getComponentFor("a");
    card.setSelectable(true);

    Input.click(card, 8, 8);
    Input.click(card, 8, 8);

    assertThat(card.isSelected())
        .as(
            "one click selects and the next deselects — a self-toggle would cancel the list's"
                + " write")
        .isFalse();
    assertThat(list.getSelectionModel().getSelected())
        .as("and the model agrees with the chrome")
        .isEmpty();
  }

  @Test
  void aProgrammaticSelectionReachesEveryHostedCard() {
    final ElwhaItemList<String> list = cardListOf("a", "b", "c");
    list.setSelectionMode(SelectionMode.MULTIPLE);

    list.getSelectionModel().setSelected(List.of("a", "c"));

    assertThat(((ElwhaCard) list.getComponentFor("a")).isSelected())
        .as("a model-side write syncs the card chrome, not only the model")
        .isTrue();
    assertThat(((ElwhaCard) list.getComponentFor("b")).isSelected())
        .as("and leaves the unselected cards alone")
        .isFalse();
    assertThat(((ElwhaCard) list.getComponentFor("c")).isSelected())
        .as("across every affected view")
        .isTrue();
  }

  @Test
  void selectionSurvivesAFilterThatHidesACard() {
    final ElwhaItemList<String> list = cardListOf("a", "b", "c");
    list.setSelectionMode(SelectionMode.MULTIPLE);
    list.getSelectionModel().setSelected(List.of("b"));

    list.setFilter(item -> !"b".equals(item));
    list.setFilter(null);

    assertThat(((ElwhaCard) list.getComponentFor("b")).isSelected())
        .as("the rebuilt card comes back painting the selection the model still holds")
        .isTrue();
  }
}
