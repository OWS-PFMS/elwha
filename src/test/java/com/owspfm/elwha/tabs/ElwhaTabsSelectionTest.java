package com.owspfm.elwha.tabs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of tab selection — design doc §2/§6: exactly one active tab at a time, the
 * add/remove rules that keep that invariant, the change-notification contract, and the material-web
 * parity rule that a gesture on the already-active tab is a no-op.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTabsSelectionTest {

  private static ElwhaTabs barOf(final String... labels) {
    final ElwhaTabs bar = new ElwhaTabs();
    for (final String label : labels) {
      bar.addTab(label);
    }
    bar.setSize(400, bar.getPreferredSize().height);
    bar.doLayout();
    return bar;
  }

  // --------------------------------------------------------- the invariant

  @Test
  void firstTabAddedBecomesActive() {
    final ElwhaTabs bar = new ElwhaTabs();

    final ElwhaTab first = bar.addTab("One");
    bar.addTab("Two");

    assertThat(bar.getActiveTab())
        .as("§2 — a bar with children always has exactly one active tab")
        .isSameAs(first);
    assertThat(bar.getActiveTabIndex()).isZero();
  }

  @Test
  void exactlyOneTabIsActiveAtATime() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");

    bar.setActiveTabIndex(2);

    final List<ElwhaTab> active = new ArrayList<>();
    for (int i = 0; i < bar.getTabCount(); i++) {
      if (bar.getTabAt(i).isActive()) {
        active.add(bar.getTabAt(i));
      }
    }
    assertThat(active).as("§2 — activating one tab deactivates the rest").hasSize(1);
    assertThat(active.get(0)).isSameAs(bar.getTabAt(2));
  }

  @Test
  void anEmptyBarHasNoActiveTab() {
    final ElwhaTabs bar = new ElwhaTabs();

    assertThat(bar.getActiveTab()).isNull();
    assertThat(bar.getActiveTabIndex())
        .as("§2 — the no-children case is the only one without a selection")
        .isNegative();
  }

  @Test
  void setActiveTabSelectsByIdentity() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");

    bar.setActiveTab(bar.getTabAt(1));

    assertThat(bar.getActiveTabIndex()).isEqualTo(1);
  }

  @Test
  void anOutOfRangeIndexLeavesTheSelectionUntouched() {
    final ElwhaTabs bar = barOf("One", "Two");
    bar.setActiveTabIndex(1);

    bar.setActiveTabIndex(5);
    bar.setActiveTabIndex(-1);

    assertThat(bar.getActiveTabIndex())
        .as(
            "§2 — the selection is mandatory, so an unusable index is ignored rather than "
                + "leaving the bar with nothing active")
        .isEqualTo(1);
  }

  @Test
  void getTabAtRejectsAnOutOfRangeIndex() {
    final ElwhaTabs bar = barOf("One", "Two");

    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> bar.getTabAt(9));
  }

  // ------------------------------------------------------------ add / remove

  @Test
  void addingATabStampsTheBarsVariantOntoIt() {
    final ElwhaTabs bar = ElwhaTabs.secondary();

    final ElwhaTab tab = bar.addTab("One");

    assertThat(tab.getVariant())
        .as("M3: use the same type of tab in a bar — the bar owns the variant")
        .isEqualTo(TabsVariant.SECONDARY);
  }

  @Test
  void factoriesPinTheVariant() {
    assertThat(ElwhaTabs.primary().getVariant()).isEqualTo(TabsVariant.PRIMARY);
    assertThat(ElwhaTabs.secondary().getVariant()).isEqualTo(TabsVariant.SECONDARY);
    assertThat(new ElwhaTabs().getVariant())
        .as("§3 — primary is the default")
        .isEqualTo(TabsVariant.PRIMARY);
  }

  @Test
  void addTabRejectsNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new ElwhaTabs().addTab((ElwhaTab) null));
  }

  @Test
  void removingTheActiveTabActivatesTheFirstSurvivor() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    bar.setActiveTabIndex(1);

    bar.removeTab(bar.getTabAt(1));

    assertThat(bar.getTabCount()).isEqualTo(2);
    assertThat(bar.getActiveTabIndex())
        .as("§2 — the invariant survives a removal: something must be active")
        .isZero();
  }

  @Test
  void removingATabBeforeTheActiveOneKeepsTheSameTabActive() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    bar.setActiveTabIndex(2);
    final ElwhaTab stillActive = bar.getTabAt(2);

    bar.removeTab(bar.getTabAt(0));

    assertThat(bar.getActiveTab())
        .as("the selection is a tab, not an index — renumbering must not move it")
        .isSameAs(stillActive);
  }

  @Test
  void removingTheLastTabLeavesNoSelection() {
    final ElwhaTabs bar = barOf("Only");

    bar.removeTab(bar.getTabAt(0));

    assertThat(bar.getTabCount()).isZero();
    assertThat(bar.getActiveTab()).isNull();
  }

  @Test
  void removingAnUnknownTabIsIgnored() {
    final ElwhaTabs bar = barOf("One", "Two");

    bar.removeTab(ElwhaTab.of("Stranger"));

    assertThat(bar.getTabCount()).as("a defensive no-op, not an exception").isEqualTo(2);
  }

  // ----------------------------------------------------- selection events

  @Test
  void activatingADifferentTabFiresTheActiveTabProperty() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final List<PropertyChangeEvent> events = new ArrayList<>();
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, events::add);

    bar.setActiveTabIndex(2);

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getSource())
        .as("the bar is the source; the selection belongs to it")
        .isSameAs(bar);
  }

  @Test
  void reActivatingTheSameTabFiresNothing() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    bar.setActiveTabIndex(1);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, events::add);

    bar.setActiveTabIndex(1);

    assertThat(events).as("nothing changed, so nothing is announced").isEmpty();
  }

  @Test
  void firstTabsImplicitActivationIsSilent() {
    final ElwhaTabs bar = new ElwhaTabs();
    final List<PropertyChangeEvent> events = new ArrayList<>();
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, events::add);

    bar.addTab("One");

    assertThat(events)
        .as("§2 — populating a bar is construction, not a user selection change")
        .isEmpty();
  }

  @Test
  void aRemovedListenerStopsHearingChanges() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final AtomicInteger count = new AtomicInteger();
    final PropertyChangeListener listener = e -> count.incrementAndGet();
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, listener);
    bar.setActiveTabIndex(1);

    bar.removePropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, listener);
    bar.setActiveTabIndex(2);

    assertThat(count.get()).isOne();
  }

  // ------------------------------------------------------- user activation

  @Test
  void clickingATabActivatesItAndFiresItsAction() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final ElwhaTab target = bar.getTabAt(2);
    final AtomicInteger fired = new AtomicInteger();
    target.addActionListener(e -> fired.incrementAndGet());

    Input.click(target, target.getWidth() / 2, target.getHeight() / 2);

    assertThat(bar.getActiveTab()).as("a click is the primary activation gesture").isSameAs(target);
    assertThat(fired.get()).as("and it notifies the tab's own listeners").isOne();
  }

  @Test
  void clickingTheAlreadyActiveTabIsANoOp() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final ElwhaTab active = bar.getTabAt(0);
    final AtomicInteger fired = new AtomicInteger();
    active.addActionListener(e -> fired.incrementAndGet());

    Input.click(active, active.getWidth() / 2, active.getHeight() / 2);

    assertThat(fired.get())
        .as("material-web parity — re-clicking the current tab is not a selection event")
        .isZero();
  }

  @Test
  void spaceActivatesTheFocusedTab() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final ElwhaTab target = bar.getTabAt(1);

    Input.pressBoundKey(target, "pressed SPACE", "elwhaTab.activate");

    assertThat(bar.getActiveTab())
        .as("§8 — Space is the keyboard equivalent of a click on the focused tab")
        .isSameAs(target);
  }

  @Test
  void enterActivatesTheFocusedTab() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final ElwhaTab target = bar.getTabAt(2);

    Input.pressBoundKey(target, "pressed ENTER", "elwhaTab.activate");

    assertThat(bar.getActiveTab()).isSameAs(target);
  }

  // ------------------------------------------------------------- enablement

  @Test
  void disablingTheBarDisablesEveryTab() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");

    bar.setEnabled(false);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).isEnabled())
          .as("the bar owns enablement; a tab cannot be live in a dead bar")
          .isFalse();
    }
  }

  @Test
  void aTabAddedToADisabledBarArrivesDisabled() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setEnabled(false);

    final ElwhaTab late = bar.addTab("Late");

    assertThat(late.isEnabled())
        .as("the bar's state applies to arrivals too, not just tabs present at the time")
        .isFalse();
  }

  @Test
  void aDisabledBarIgnoresClicks() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    bar.setEnabled(false);
    final ElwhaTab target = bar.getTabAt(2);

    Input.click(target, target.getWidth() / 2, target.getHeight() / 2);

    assertThat(bar.getActiveTabIndex())
        .as("#432 — the guard reads the bar's own enabled state, not the tab's")
        .isZero();
  }

  @Test
  void reEnablingTheBarReEnablesItsTabs() {
    final ElwhaTabs bar = barOf("One", "Two");
    bar.setEnabled(false);

    bar.setEnabled(true);

    assertThat(bar.getTabAt(1).isEnabled()).isTrue();
  }

  // ---------------------------------------------------------- teardown state

  /** A ground no bundled palette carries, so "the tab painted nothing here" is unambiguous. */
  private static final Color GROUND = Color.MAGENTA;

  @Test
  void aTabRemovedWhileHoveredComesBackUnhovered() {
    // Labels are empty so no glyph pixels can land on the probe.
    final ElwhaTabs bar = barOf("", "");
    final ElwhaTab inactive = bar.getTabAt(1);
    inactive.setHovered(true);
    Pixels.assertPixelNear(
        Pixels.render(inactive, 80, 48, GROUND),
        4,
        4,
        Pixels.mix(GROUND, ColorRole.ON_SURFACE.resolve(), StateLayer.HOVER.opacity()),
        "the hover layer paints while the pointer is over the tab");

    inactive.removeNotify();

    Pixels.assertPixelNear(
        Pixels.render(inactive, 80, 48, GROUND),
        4,
        4,
        GROUND,
        "#625 — the hover poll is the only self-correction, so teardown has to clear the flag "
            + "or a re-added tab paints its hover layer for good");
  }

  @Test
  void aTabDisabledWhileHoveredComesBackUnhovered() {
    final ElwhaTabs bar = barOf("", "");
    final ElwhaTab inactive = bar.getTabAt(1);
    inactive.setHovered(true);

    inactive.setEnabled(false);
    inactive.setEnabled(true);

    Pixels.assertPixelNear(
        Pixels.render(inactive, 80, 48, GROUND),
        4,
        4,
        GROUND,
        "#683 — the hover poll is the only self-correction and a disabled tab receives no mouse"
            + " events, so re-enabling without moving the pointer must not replay the hover layer");
  }

  /**
   * #686 — #683 completed the ripple on the way through {@code setEnabled}, which covers the
   * component's own disable path and nothing else. A tab that starts a ripple while already
   * disabled never passes through that path, and until the guard moved into the shared {@code
   * RippleAnimation} the paint layer was willing to draw it.
   */
  @Test
  void aDisabledTabPaintsNoRippleEvenWhenOneIsStarted() {
    final ElwhaTabs bar = barOf("", "");
    final ElwhaTab inactive = bar.getTabAt(1);
    inactive.setEnabled(false);

    inactive.startRipple(new java.awt.Point(40, 24));

    Pixels.assertPixelNear(
        Pixels.render(inactive, 80, 48, GROUND),
        40,
        24,
        GROUND,
        "the ripple paint path now consults isEnabled(), so no state combination can put"
            + " interactive chrome on a tab that refuses interaction");
  }

  @Test
  void aTabDisabledWhilePressedComesBackUnpressed() {
    final ElwhaTabs bar = barOf("", "");
    final ElwhaTab inactive = bar.getTabAt(1);
    inactive.setPressed(true);

    inactive.setEnabled(false);
    inactive.setEnabled(true);

    Pixels.assertPixelNear(
        Pixels.render(inactive, 80, 48, GROUND),
        4,
        4,
        GROUND,
        "#683 — a press latched at the moment of the disable is never released either");
  }

  @Test
  void aTabRemovedWhilePressedComesBackUnpressed() {
    final ElwhaTabs bar = barOf("", "");
    final ElwhaTab inactive = bar.getTabAt(1);
    inactive.setPressed(true);

    inactive.removeNotify();

    Pixels.assertPixelNear(
        Pixels.render(inactive, 80, 48, GROUND),
        4,
        4,
        GROUND,
        "#625 — the pressed flag is stranded by the same stopped poll");
  }

  // -------------------------------------------------------- property changes

  @Test
  void everyActivationCarriesBothTabs() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final ElwhaTab first = bar.getTabAt(0);
    final ElwhaTab third = bar.getTabAt(2);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, events::add);

    bar.setActiveTabIndex(2);

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getOldValue())
        .as("the tab the selection left is carried, not just its index")
        .isSameAs(first);
    assertThat(events.get(0).getNewValue()).as("along with the arriving tab").isSameAs(third);
  }

  @Test
  void anActiveTabListenerIsNotWokenByOtherProperties() {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    final int[] changes = {0};
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, e -> changes[0]++);

    bar.setEnabled(false);

    assertThat(changes[0])
        .as("subscription is key-scoped — the enabled property must not wake it")
        .isZero();
  }

  @Test
  void removingTheActiveTabAnnouncesTheReplacementAtTheSameIndex() {
    final ElwhaTabs bar = barOf("One", "Two");
    final ElwhaTab second = bar.getTabAt(1);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    bar.addPropertyChangeListener(ElwhaTabs.PROPERTY_ACTIVE_TAB, events::add);

    bar.removeTab(bar.getTabAt(0));

    assertThat(bar.getActiveTab()).as("the survivor takes index 0").isSameAs(second);
    assertThat(events)
        .as("the event carries tab identity, so a same-index replacement is still a change")
        .hasSize(1);
    assertThat(events.get(0).getNewValue()).isSameAs(second);
  }
}
