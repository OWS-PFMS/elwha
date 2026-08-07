package com.owspfm.elwha.navrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.FocusTraversalPolicy;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the rail's selection model and accessibility shape — design doc §6 (the
 * container is the single source of truth, with exactly one mandatory selection) and §10 (the
 * {@code PAGE_TAB_LIST} / {@code PAGE_TAB} pairing and the traversal cycle that enters the rail on
 * the selected destination rather than on its chrome).
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaNavigationRailSelectionTest {

  private static ElwhaNavigationRail rail(final int count) {
    return RailFixture.laidOut(ElwhaNavigationRail.collapsed(), count);
  }

  // ---------------------------------------------------------- the invariant

  @Test
  void firstDestinationIsSelectedOnPopulation() {
    final ElwhaNavigationRail rail = rail(4);

    assertThat(rail.getSelected())
        .as("§6 — the selection is mandatory, so populating the rail establishes one")
        .isSameAs(rail.getPrimary().get(0));
  }

  @Test
  void exactlyOneDestinationIsSelectedAtATime() {
    final ElwhaNavigationRail rail = rail(4);

    rail.setSelected(rail.getPrimary().get(2));

    final List<ElwhaNavRailDestination> selected = new ArrayList<>();
    for (final ElwhaNavRailDestination destination : rail.getPrimary()) {
      if (destination.isSelected()) {
        selected.add(destination);
      }
    }
    assertThat(selected).hasSize(1);
    assertThat(selected.get(0)).isSameAs(rail.getPrimary().get(2));
  }

  @Test
  void anEmptyRailHasNoSelection() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();

    rail.setPrimary(List.of());

    assertThat(rail.getSelected())
        .as("§6 — nothing to select is the one legal way to have no selection")
        .isNull();
  }

  @Test
  void rePopulatingKeepsASurvivingSelection() {
    final ElwhaNavigationRail rail = rail(4);
    final ElwhaNavRailDestination keep = rail.getPrimary().get(2);
    rail.setSelected(keep);
    final List<ElwhaNavRailDestination> next = new ArrayList<>(RailFixture.destinations(2));
    next.add(keep);

    rail.setPrimary(next);

    assertThat(rail.getSelected())
        .as("§6 — a destination that is still a member keeps the selection across a refresh")
        .isSameAs(keep);
  }

  @Test
  void rePopulatingWithoutTheSelectedDestinationFallsBackToTheFirst() {
    final ElwhaNavigationRail rail = rail(4);
    rail.setSelected(rail.getPrimary().get(3));
    final List<ElwhaNavRailDestination> replacement = RailFixture.destinations(2);

    rail.setPrimary(replacement);

    assertThat(rail.getSelected())
        .as("§6 — the invariant survives: something must be selected")
        .isSameAs(replacement.get(0));
  }

  @Test
  void aNonMemberIsRejected() {
    final ElwhaNavigationRail rail = rail(3);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> rail.setSelected(RailFixture.destination("Stranger")));
  }

  @Test
  void aSectionDestinationIsSelectable() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();
    rail.setPrimary(RailFixture.destinations(3));
    final List<ElwhaNavRailDestination> secondary = RailFixture.destinations(2);
    rail.addSection("Tools", secondary);

    rail.setSelected(secondary.get(0));

    assertThat(rail.getSelected())
        .as("§6 — the selection model is the union of primary and section destinations")
        .isSameAs(secondary.get(0));
    assertThat(rail.getPrimary().get(0).isSelected())
        .as("and selecting across the union still deselects the previous one")
        .isFalse();
  }

  // -------------------------------------------------------------- events

  @Test
  void aSelectionChangeNotifiesListeners() {
    final ElwhaNavigationRail rail = rail(4);
    final AtomicInteger fired = new AtomicInteger();
    rail.addSelectionListener((previous, current) -> fired.incrementAndGet());

    rail.setSelected(rail.getPrimary().get(1));

    assertThat(fired.get()).isOne();
  }

  @Test
  void aSelectionChangeFiresTheSelectedProperty() {
    final ElwhaNavigationRail rail = rail(4);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    rail.addPropertyChangeListener(ElwhaNavigationRail.PROPERTY_SELECTED, events::add);

    rail.setSelected(rail.getPrimary().get(1));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getNewValue()).isSameAs(rail.getPrimary().get(1));
  }

  @Test
  void reSelectingTheSameDestinationIsANoOp() {
    final ElwhaNavigationRail rail = rail(4);
    rail.setSelected(rail.getPrimary().get(1));
    final AtomicInteger fired = new AtomicInteger();
    rail.addSelectionListener((previous, current) -> fired.incrementAndGet());

    rail.setSelected(rail.getPrimary().get(1));

    assertThat(fired.get()).as("nothing changed, so nothing is announced").isZero();
  }

  @Test
  void aRemovedListenerStopsHearingChanges() {
    final ElwhaNavigationRail rail = rail(4);
    final AtomicInteger fired = new AtomicInteger();
    final ElwhaNavigationRail.NavRailSelectionListener listener =
        (previous, current) -> fired.incrementAndGet();
    rail.addSelectionListener(listener);
    rail.setSelected(rail.getPrimary().get(1));

    rail.removeSelectionListener(listener);
    rail.setSelected(rail.getPrimary().get(2));

    assertThat(fired.get()).isOne();
  }

  @Test
  void clickingADestinationSelectsIt() {
    final ElwhaNavigationRail rail = rail(4);
    final ElwhaNavRailDestination target = rail.getPrimary().get(2);

    Input.click(target, target.getWidth() / 2, target.getHeight() / 2);

    assertThat(rail.getSelected())
        .as("§6 — a click routes through the container, which owns the selection")
        .isSameAs(target);
  }

  @Test
  void aDestinationsOwnListenersStillFireOnClick() {
    final ElwhaNavigationRail rail = rail(4);
    final ElwhaNavRailDestination target = rail.getPrimary().get(2);
    final AtomicInteger fired = new AtomicInteger();
    target.addActionListener(e -> fired.incrementAndGet());

    Input.click(target, target.getWidth() / 2, target.getHeight() / 2);

    assertThat(fired.get())
        .as("§6 — the container's routing does not swallow the consumer's own listener")
        .isOne();
  }

  @Test
  void aDestinationsSelectedStateIsAnnounced() {
    final ElwhaNavigationRail rail = rail(4);
    final ElwhaNavRailDestination target = rail.getPrimary().get(2);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    target.addPropertyChangeListener(ElwhaNavRailDestination.PROPERTY_SELECTED, events::add);

    rail.setSelected(target);

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getNewValue()).isEqualTo(true);
  }

  // ------------------------------------------------------- accessibility

  @Test
  void railIsATabList() {
    assertThat(rail(4).getAccessibleContext().getAccessibleRole())
        .as("§10 — the ARIA tablist mapping, matching the tab bar's")
        .isEqualTo(AccessibleRole.PAGE_TAB_LIST);
  }

  @Test
  void eachDestinationIsAPageTab() {
    final ElwhaNavigationRail rail = rail(4);

    for (final ElwhaNavRailDestination destination : rail.getPrimary()) {
      assertThat(destination.getAccessibleContext().getAccessibleRole())
          .as("§10 — the tablist/tab pairing AT relies on")
          .isEqualTo(AccessibleRole.PAGE_TAB);
    }
  }

  @Test
  void onlyTheSelectedDestinationReportsSelected() {
    final ElwhaNavigationRail rail = rail(4);
    rail.setSelected(rail.getPrimary().get(2));

    for (int i = 0; i < rail.getPrimary().size(); i++) {
      assertThat(
              rail.getPrimary()
                  .get(i)
                  .getAccessibleContext()
                  .getAccessibleStateSet()
                  .contains(AccessibleState.SELECTED))
          .as("§10 — the selected state tracks the container's selection, destination %d", i)
          .isEqualTo(i == 2);
    }
  }

  @Test
  void aDestinationIsNamedByItsLabel() {
    final ElwhaNavigationRail rail = rail(3);

    assertThat(rail.getPrimary().get(1).getAccessibleContext().getAccessibleName())
        .isEqualTo("Item 1");
  }

  // ----------------------------------------------------- traversal policy

  @Test
  void railProvidesItsOwnTraversalPolicy() {
    assertThat(rail(4).isFocusTraversalPolicyProvider())
        .as("§10.2 — the rail curates its own tab cycle rather than exposing every child")
        .isTrue();
  }

  @Test
  void tabEntersTheRailOnTheSelectedDestination() {
    final ElwhaNavigationRail rail = rail(4);
    rail.setSelected(rail.getPrimary().get(2));

    assertThat(rail.getFocusTraversalPolicy().getDefaultComponent(rail))
        .as("#256 — focus enters at the selected destination, not on the rail's chrome")
        .isSameAs(rail.getPrimary().get(2));
  }

  @Test
  void tabWalkingStillStartsAtTheMenuButton() {
    final ElwhaNavigationRail rail = rail(4);
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);

    assertThat(rail.getFocusTraversalPolicy().getFirstComponent(rail))
        .as("§10.2 — the walk order still opens on the menu; only the default entry changed")
        .isSameAs(menu);
  }

  @Test
  void anEmptyRailFallsBackToItsFirstComponentForTheDefault() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);
    rail.setPrimary(List.of());

    assertThat(rail.getFocusTraversalPolicy().getDefaultComponent(rail))
        .as("#256 — with no destination to enter on, the walk order's first stop takes over")
        .isSameAs(menu);
  }

  @Test
  void tabOrderExposesExactlyOneDestination() {
    final ElwhaNavigationRail rail = rail(5);
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);
    final ElwhaIconButton help = new ElwhaIconButton(MaterialIcons.symbol("help").unselected());
    rail.setTrailingActions(List.of(help));
    rail.setSelected(rail.getPrimary().get(3));
    final FocusTraversalPolicy policy = rail.getFocusTraversalPolicy();

    final List<java.awt.Component> walked = new ArrayList<>();
    java.awt.Component stop = policy.getFirstComponent(rail);
    while (stop != null && walked.size() < 20) {
      walked.add(stop);
      stop = policy.getComponentAfter(rail, stop);
    }

    assertThat(walked)
        .as(
            "§10.2 — Tab walks menu, then the destination band's single stop, then trailing "
                + "actions, then leaves the rail")
        .containsExactly(menu, rail.getPrimary().get(3), help);
    assertThat(walked.stream().filter(c -> c instanceof ElwhaNavRailDestination).count())
        .as(
            "§10.2 — the active-descendant pattern: one destination in the cycle, arrows move "
                + "within the band")
        .isOne();
  }

  @Test
  void trailingActionsJoinTheTabOrder() {
    final ElwhaNavigationRail rail = rail(3);
    final ElwhaIconButton action = new ElwhaIconButton(MaterialIcons.symbol("help").unselected());
    rail.setTrailingActions(List.of(action));

    assertThat(rail.getTrailingActions()).containsExactly(action);
    assertThat(rail.getFocusTraversalPolicy().getLastComponent(rail))
        .as("§10.2 — trailing actions are the last stops before focus leaves the rail")
        .isSameAs(action);
  }

  @Test
  void trailingActionsAreNotPartOfTheSelectionModel() {
    final ElwhaNavigationRail rail = rail(3);
    final ElwhaIconButton action = new ElwhaIconButton(MaterialIcons.symbol("help").unselected());
    rail.setTrailingActions(List.of(action));

    assertThat(rail.getSelected())
        .as("§3 — trailing actions are utility buttons, not destinations")
        .isSameAs(rail.getPrimary().get(0));
  }
}
