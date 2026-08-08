package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaCard}'s behavior axes — the §12 actionability quadrad as an atomic
 * gate, the §13 selection model and its property event, and the §14 collapse model (per-child
 * rules, the {@code ALWAYS_VISIBLE} default for disclosure affordances, and the reduced-motion
 * snap).
 *
 * <p>Keyboard coverage asserts the {@code WHEN_FOCUSED} binding and then invokes the mapped action;
 * real focus ownership belongs to the gui tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardInteractionTest {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 160;

  /** A child with a pinned preferred size, so collapse height arithmetic below is exact. */
  private static final class Block extends JComponent {
    private final Dimension preferred;

    Block(final int height) {
      this.preferred = new Dimension(100, height);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(preferred);
    }
  }

  private static ElwhaCard sized(final ElwhaCard card) {
    card.setSize(WIDTH, HEIGHT);
    card.doLayout();
    return card;
  }

  // ------------------------------------------------------ actionability

  @Test
  void actionabilityFlipsTheWholeQuadradAtOnce() {
    final ElwhaCard card = ElwhaCard.filledCard();

    card.setActionable(true);

    assertThat(card.isFocusable()).as("§12 — an actionable card takes a tab stop").isTrue();
    assertThat(card.getCursor().getType())
        .as("and shows the hand pointer")
        .isEqualTo(Cursor.HAND_CURSOR);
    assertThat(card.getAccessibleContext().getAccessibleRole())
        .as("and reads as a button to assistive technology")
        .isEqualTo(javax.accessibility.AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void clearingActionabilityRestoresTheInertCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setActionable(true);

    card.setActionable(false);

    assertThat(card.isFocusable()).as("the tab stop goes away with the gate").isFalse();
    assertThat(card.getCursor().getType())
        .as("and so does the hand pointer")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
    assertThat(card.getAccessibleContext().getAccessibleRole())
        .as("an inert card is a panel again")
        .isEqualTo(javax.accessibility.AccessibleRole.PANEL);
  }

  @Test
  void togglingActionabilityFiresItsProperty() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final List<PropertyChangeEvent> heard = new ArrayList<>();
    card.addPropertyChangeListener(ElwhaCard.PROPERTY_ACTIONABLE, heard::add);

    card.setActionable(true);
    card.setActionable(true);

    assertThat(heard).as("a redundant write fires nothing").hasSize(1);
    assertThat(heard.get(0).getNewValue()).as("the event carries the new state").isEqualTo(true);
  }

  @Test
  void aDisabledActionableCardDropsTheHandPointer() {
    final ElwhaCard card = ElwhaCard.filledCard().setActionable(true);

    card.setEnabled(false);

    assertThat(card.getCursor().getType())
        .as("a disabled card must not advertise a click it will not honor")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @Test
  void anActionableCardFiresOnClick() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setActionable(true));
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);

    Input.click(card, WIDTH / 2, HEIGHT / 2);

    assertThat(heard).as("a press-release pair inside the card is a click").hasSize(1);
    assertThat(heard.get(0).getSource()).as("the event names the card").isSameAs(card);
  }

  @Test
  void anInertCardIgnoresTheSameClick() {
    final ElwhaCard card = sized(ElwhaCard.filledCard());
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);

    Input.click(card, WIDTH / 2, HEIGHT / 2);

    assertThat(heard).as("the gate is closed, so nothing fires").isEmpty();
  }

  @Test
  void aDisabledCardIgnoresTheClickToo() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setActionable(true));
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);
    card.setEnabled(false);

    Input.click(card, WIDTH / 2, HEIGHT / 2);

    assertThat(heard).as("disabling gates the action as firmly as clearing actionable").isEmpty();
  }

  @Test
  void spaceAndEnterBothActivateTheFocusedCard() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setActionable(true));
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);

    Input.pressBoundKey(card, "pressed SPACE", "elwhaCardActivate");
    Input.pressBoundKey(card, "pressed ENTER", "elwhaCardActivate");

    assertThat(heard).as("§12 — keyboard activation matches the click surface").hasSize(2);
  }

  @Test
  void keyboardActivationRespectsTheGateToo() {
    final ElwhaCard card = sized(ElwhaCard.filledCard());
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);

    Input.pressBoundKey(card, "pressed SPACE", "elwhaCardActivate");

    assertThat(heard)
        .as("the binding stays installed but the action checks the gate before firing")
        .isEmpty();
  }

  @Test
  void aRemovedActionListenerStopsHearing() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setActionable(true));
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);
    card.addActionListener(null);

    card.removeActionListener(heard::add);
    Input.click(card, WIDTH / 2, HEIGHT / 2);

    assertThat(heard)
        .as("a null listener is ignored, and removing an equal-but-different lambda is a no-op")
        .hasSize(1);
  }

  @Test
  void aCanceledClickDoesNotFireOnRelease() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setActionable(true));
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);

    Input.press(card, WIDTH / 2, HEIGHT / 2);
    card.cancelPendingClick();
    Input.release(card, WIDTH / 2, HEIGHT / 2);

    assertThat(heard)
        .as("a press that turned into a drag must not also fire the card's action")
        .isEmpty();
  }

  @Test
  void cancelOnlyCoversTheOneGestureItInterrupted() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setActionable(true));
    final List<ActionEvent> heard = new ArrayList<>();
    card.addActionListener(heard::add);
    Input.press(card, WIDTH / 2, HEIGHT / 2);
    card.cancelPendingClick();
    Input.release(card, WIDTH / 2, HEIGHT / 2);

    Input.click(card, WIDTH / 2, HEIGHT / 2);

    assertThat(heard).as("the next click is honored normally").hasSize(1);
  }

  // ---------------------------------------------------------- selection

  @Test
  void selectionIsGatedOnSelectable() {
    final ElwhaCard card = ElwhaCard.filledCard();

    card.setSelected(true);

    assertThat(card.isSelected())
        .as("a card that was never made selectable refuses to enter the state")
        .isFalse();
  }

  @Test
  void aSelectableCardTracksTheStateAndFiresTheProperty() {
    final ElwhaCard card = ElwhaCard.filledCard().setSelectable(true);
    final List<PropertyChangeEvent> heard = new ArrayList<>();
    card.addSelectionChangeListener(heard::add);

    card.setSelected(true);
    card.setSelected(true);

    assertThat(card.isSelected()).as("the state round-trips").isTrue();
    assertThat(heard).as("and a redundant write is silent").hasSize(1);
    assertThat(heard.get(0).getPropertyName())
        .as("the event names the selected property")
        .isEqualTo(ElwhaCard.PROPERTY_SELECTED);
  }

  @Test
  void aRemovedSelectionListenerStopsHearing() {
    final ElwhaCard card = ElwhaCard.filledCard().setSelectable(true);
    final List<PropertyChangeEvent> heard = new ArrayList<>();
    final java.beans.PropertyChangeListener listener = heard::add;
    card.addSelectionChangeListener(listener);

    card.removeSelectionChangeListener(listener);
    card.setSelected(true);

    assertThat(heard).as("removal actually detaches the listener").isEmpty();
  }

  @Test
  void selectionAlsoReachesPlainPropertyChangeListeners() {
    final ElwhaCard card = ElwhaCard.filledCard().setSelectable(true);
    final List<PropertyChangeEvent> heard = new ArrayList<>();
    card.addPropertyChangeListener(ElwhaCard.PROPERTY_SELECTED, heard::add);

    card.setSelected(true);

    assertThat(heard)
        .as("the standard bean channel carries the change as well as the typed one")
        .hasSize(1);
  }

  @Test
  void selectionAndActionabilityAreOrthogonal() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setSelectable(true));

    Input.click(card, WIDTH / 2, HEIGHT / 2);

    assertThat(card.isSelected())
        .as("a selectable but inert card has no click surface to toggle from")
        .isFalse();
  }

  @Test
  void aSelectableActionableCardTogglesOnClick() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setSelectable(true).setActionable(true));

    Input.click(card, WIDTH / 2, HEIGHT / 2);
    assertThat(card.isSelected()).as("the first click selects").isTrue();

    Input.click(card, WIDTH / 2, HEIGHT / 2);
    assertThat(card.isSelected()).as("and the second deselects").isFalse();
  }

  // ------------------------------------------------- list-driven handover

  @Test
  void handingOverToAListForcesTheClickSurfaceOn() {
    final ElwhaCard card = ElwhaCard.filledCard();

    card.setListInteractive(true);

    assertThat(card.isActionable())
        .as("a hosted card must respond to the list's clicks whatever it was configured as")
        .isTrue();
  }

  @Test
  void aListInteractiveCardStopsSelfTogglingButAcceptsTheWriteBack() {
    final ElwhaCard card = sized(ElwhaCard.filledCard().setSelectable(true));
    card.setListInteractive(true);

    Input.click(card, WIDTH / 2, HEIGHT / 2);
    assertThat(card.isSelected())
        .as("the list owns selection — self-toggling here would race the write-back")
        .isFalse();

    card.setSelected(true);
    assertThat(card.isSelected()).as("the list's own write is what moves the chrome").isTrue();
  }

  @Test
  void listWriteBackIsAllowedPastTheSelectableGate() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.setListInteractive(true);

    card.setSelected(true);

    assertThat(card.isSelected())
        .as("a hosted card need not be independently selectable for the list to select it")
        .isTrue();
  }

  @Test
  void takingControlBackRestoresThePreviousActionability() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.setListInteractive(true);

    card.setListInteractive(false);

    assertThat(card.isActionable())
        .as("the card gets back exactly the configuration the list borrowed")
        .isFalse();
  }

  @Test
  void anAlreadyActionableCardKeepsThatWhenReleased() {
    final ElwhaCard card = ElwhaCard.filledCard().setActionable(true);
    card.setListInteractive(true);

    card.setListInteractive(false);

    assertThat(card.isActionable()).as("restoring means restoring, not clearing").isTrue();
  }

  @Test
  void aRepeatedHandOverRemembersTheOriginalConfiguration() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.setListInteractive(true);
    card.setListInteractive(true);

    card.setListInteractive(false);

    assertThat(card.isActionable())
        .as("the second claim must not record the already-claimed state as the original")
        .isFalse();
  }

  @Test
  void releasingACardThatWasNeverClaimedChangesNothing() {
    final ElwhaCard card = ElwhaCard.filledCard().setActionable(true);

    card.setListInteractive(false);

    assertThat(card.isActionable())
        .as("an unmatched release is a no-op rather than a reset to false")
        .isTrue();
  }

  @Test
  void handoverReturnsTheCardForFluentUse() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThat(card.setListInteractive(true))
        .as("the covariant override keeps the card's own builder chain usable")
        .isSameAs(card);
    assertThat(card.setSelected(true)).as("as does the selection write").isSameAs(card);
    assertThat(card.cancelPendingClick()).as("and the click cancel").isSameAs(card);
  }

  // ----------------------------------------------------------- collapse

  @Test
  void collapseIsGatedOnCollapsible() {
    final ElwhaCard card = ElwhaCard.filledCard();

    card.setCollapsed(true);

    assertThat(card.isCollapsed())
        .as("a card that was never made collapsible refuses to collapse")
        .isFalse();
  }

  @Test
  void collapsingHidesTheCollapsibleChildren() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final Block body = new Block(40);
    card.add(body);

    card.setCollapsed(true);

    assertThat(body.isVisible())
        .as("§14.2 — a child with no explicit rule collapses away")
        .isFalse();
  }

  @Test
  void expandingBringsThemBack() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final Block body = new Block(40);
    card.add(body);
    card.setCollapsed(true);

    card.setCollapsed(false);

    assertThat(body.isVisible()).as("expanding restores every collapsible child").isTrue();
  }

  @Test
  void anAlwaysVisibleChildSurvivesTheCollapse() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final Block header = new Block(30);
    final Block body = new Block(40);
    card.add(header);
    card.add(body);
    card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);

    card.setCollapsed(true);

    assertThat(header.isVisible()).as("the anchored child stays on screen").isTrue();
    assertThat(body.isVisible()).as("while the rest collapses away").isFalse();
  }

  @Test
  void defaultRuleIsCollapsible() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final Block body = new Block(30);
    card.add(body);

    assertThat(card.getCollapseConstraint(body))
        .as("§14.2 — an unconstrained child collapses with the body")
        .isEqualTo(CollapseRule.COLLAPSIBLE);
  }

  @Test
  void anUnknownChildStillReportsTheDefaultRule() {
    assertThat(ElwhaCard.filledCard().getCollapseConstraint(new Block(10)))
        .as("querying a component that was never added answers rather than throwing")
        .isEqualTo(CollapseRule.COLLAPSIBLE);
  }

  @Test
  void aRuleCanBeReversedAfterTheFact() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final Block header = new Block(30);
    card.add(header);
    card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);

    card.setCollapseConstraint(header, CollapseRule.COLLAPSIBLE);
    card.setCollapsed(true);

    assertThat(header.isVisible())
        .as("a consumer can override an auto-anchored rule back to collapsing")
        .isFalse();
  }

  @Test
  void collapseConstraintsRejectNull() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThatThrownBy(() -> card.setCollapseConstraint(null, CollapseRule.COLLAPSIBLE))
        .as("a null child is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> card.setCollapseConstraint(new Block(1), null))
        .as("a null rule is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void collapsingFiresTheExpansionProperty() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final List<PropertyChangeEvent> heard = new ArrayList<>();
    card.addExpansionChangeListener(heard::add);

    card.setCollapsed(true);
    card.setCollapsed(true);

    assertThat(heard).as("a redundant collapse is silent").hasSize(1);
    assertThat(heard.get(0).getPropertyName())
        .as("the event names the collapsed property")
        .isEqualTo(ElwhaCard.PROPERTY_COLLAPSED);
  }

  @Test
  void aRemovedExpansionListenerStopsHearing() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final List<PropertyChangeEvent> heard = new ArrayList<>();
    final java.beans.PropertyChangeListener listener = heard::add;
    card.addExpansionChangeListener(listener);

    card.removeExpansionChangeListener(listener);
    card.setCollapsed(true);

    assertThat(heard).as("removal actually detaches the listener").isEmpty();
  }

  @Test
  void collapsingShrinksThePreferredHeightToTheSurvivingChildren() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true).setAnimateCollapse(false);
    final Block header = new Block(30);
    final Block body = new Block(60);
    card.add(header);
    card.add(body);
    card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);
    final int expanded = card.getPreferredSize().height;

    card.setCollapsed(true);

    assertThat(card.getPreferredSize().height)
        .as("the card measures only what is still showing")
        .isLessThan(expanded);
  }

  @Test
  void collapseSnapsRatherThanTweeningWhenAnimationIsOff() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true).setAnimateCollapse(false);
    final Block header = new Block(30);
    final Block body = new Block(60);
    card.add(header);
    card.add(body);
    card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);

    card.setCollapsed(true);

    final int reserve = card.getInsets().top + card.getInsets().bottom;
    assertThat(card.getPreferredSize().height)
        .as("with the tween off the very first measurement is already the settled height")
        .isEqualTo(30 + 2 * com.owspfm.elwha.theme.SpaceScale.LG.px() + reserve);
  }

  @Test
  void animationFlagRoundTrips() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThat(card.setAnimateCollapse(true).isAnimateCollapse())
        .as("the tween can be asked for")
        .isTrue();
    assertThat(card.setAnimateCollapse(false).isAnimateCollapse())
        .as("and turned back off")
        .isFalse();
  }

  @Test
  void aHeadlessCardDoesNotTweenByDefault() {
    assertThat(ElwhaCard.filledCard().isAnimateCollapse())
        .as("with no display there is nothing to animate for, so the tween starts off")
        .isEqualTo(!java.awt.GraphicsEnvironment.isHeadless());
  }

  // ------------------------------------------------- expansion overflow

  @Test
  void overflowStrategyRoundTripsAndRejectsNull() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThat(card.setExpansionOverflow(ExpansionOverflow.SCROLL).getExpansionOverflow())
        .as("the internal-scroll strategy is selectable")
        .isEqualTo(ExpansionOverflow.SCROLL);
    assertThat(card.setExpansionOverflow(ExpansionOverflow.GROW).getExpansionOverflow())
        .as("and the growing default comes back")
        .isEqualTo(ExpansionOverflow.GROW);
    assertThatThrownBy(() -> card.setExpansionOverflow(null))
        .as("a null strategy is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void switchingToScrollKeepsAddWorkingAndCapsTheHeight() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(new Block(200));

    card.setExpansionOverflow(ExpansionOverflow.SCROLL);
    card.add(new Block(600));

    assertThat(card.getPreferredSize().height)
        .as("§14.4 — a scrolling card caps instead of pushing its siblings around")
        .isLessThanOrEqualTo(320);
  }

  @Test
  void removeRoutesIntoTheScrollBodyJustAsAddDoes() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final Block body = new Block(40);
    card.setExpansionOverflow(ExpansionOverflow.SCROLL);
    card.add(body);

    card.remove(body);

    assertThat(body.getParent())
        .as("#677 — remove ignored the routing addImpl performs, so it silently did nothing")
        .isNull();
  }

  @Test
  void removeByIndexAndRemoveAllAddressTheScrollBodysContent() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.setExpansionOverflow(ExpansionOverflow.SCROLL);
    final Block first = new Block(40);
    final Block second = new Block(40);
    card.add(first);
    card.add(second);

    card.remove(0);
    assertThat(first.getParent()).as("the index is a content index, as it is for add").isNull();
    assertThat(second.getParent()).isNotNull();

    card.removeAll();
    assertThat(second.getParent()).as("and removeAll clears the content").isNull();
    assertThat(card.getPreferredSize())
        .as("the scroll chrome is the card's own furniture and survives")
        .isNotNull();
    card.add(new Block(600));
    assertThat(card.getPreferredSize().height)
        .as("a card that still scrolls after removeAll never lost its wrapper")
        .isLessThanOrEqualTo(320);
  }

  @Test
  void switchingBackToGrowReturnsTheChildrenToTheChassis() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final Block body = new Block(40);
    card.add(body);
    card.setExpansionOverflow(ExpansionOverflow.SCROLL);

    card.setExpansionOverflow(ExpansionOverflow.GROW);

    assertThat(body.getParent())
        .as("unwrapping puts the content back under the card itself, not the discarded viewport")
        .isSameAs(card);
  }

  @Test
  void collapseRulesStillApplyInsideAScrollWrapper() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    card.setExpansionOverflow(ExpansionOverflow.SCROLL);
    final Block body = new Block(40);
    card.add(body);

    card.setCollapsed(true);

    assertThat(body.isVisible())
        .as("the collapse walks the real content host, not just the card's direct children")
        .isFalse();
  }
}
