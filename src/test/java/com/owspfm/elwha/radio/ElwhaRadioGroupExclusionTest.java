package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the group's mutual-exclusion controller — membership bookkeeping, the
 * first-selected-wins join rule, and the once-per-change group event whose listeners must already
 * see the settled state. Oracle: {@code docs/research/elwha-radiobutton-design.md} §8 as restated
 * in the class Javadoc.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioGroupExclusionTest {

  // -------------------------------------------------------------- membership

  @Test
  void anEmptyGroupHasNoMembersAndNoSelection() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();

    assertThat(group.getMembers()).as("a new group is empty").isEmpty();
    assertThat(group.getSelected()).as("and holds no selection").isNull();
  }

  @Test
  void addingSetsTheBackReferenceAndTheNavigationOrder() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();

    group.add(first);
    group.add(second);

    assertThat(group.getMembers())
        .as("membership order is navigation order — the order they were added")
        .containsExactly(first, second);
    assertThat(first.getGroup()).as("and each member points back at the group").isSameAs(group);
  }

  @Test
  void addingTheSameRadioTwiceIsANoOp() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton();

    group.add(radio);
    group.add(radio);

    assertThat(group.getMembers())
        .as("a member is in the group once, not twice")
        .containsExactly(radio);
  }

  @Test
  void addingNullIsRejected() {
    assertThatThrownBy(() -> new ElwhaRadioGroup().add(null))
        .as("there is nothing sensible to do with a null member")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void memberListIsAnUnmodifiableSnapshot() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(new ElwhaRadioButton());
    final List<ElwhaRadioButton> members = group.getMembers();

    assertThatThrownBy(() -> members.add(new ElwhaRadioButton()))
        .as("callers cannot reach behind the group's back to edit membership")
        .isInstanceOf(UnsupportedOperationException.class);

    group.add(new ElwhaRadioButton());
    assertThat(members).as("and the snapshot they hold does not move under them").hasSize(1);
  }

  @Test
  void removingClearsTheBackReference() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    group.add(radio);

    group.remove(radio);

    assertThat(group.getMembers()).as("the member is gone").isEmpty();
    assertThat(radio.getGroup()).as("and no longer points at the group").isNull();
  }

  @Test
  void removingANonMemberIsANoOp() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton member = new ElwhaRadioButton();
    group.add(member);
    final int[] fires = {0};
    group.addSelectionChangeListener(e -> fires[0]++);

    group.remove(new ElwhaRadioButton());

    assertThat(group.getMembers()).as("the real member is untouched").containsExactly(member);
    assertThat(fires[0]).as("and nothing is announced").isZero();
  }

  @Test
  void addingToASecondGroupMovesTheRadio() {
    final ElwhaRadioGroup first = new ElwhaRadioGroup();
    final ElwhaRadioGroup second = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    first.add(radio);

    second.add(radio);

    assertThat(first.getMembers()).as("a radio belongs to at most one group").isEmpty();
    assertThat(second.getMembers()).as("so joining a second one moves it").containsExactly(radio);
    assertThat(radio.getGroup()).as("and the back reference follows").isSameAs(second);
  }

  // ------------------------------------------------------------- join rules

  @Test
  void aSelectedRadioJoiningAnEmptyGroupBecomesItsSelection() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    final int[] fires = {0};
    group.addSelectionChangeListener(e -> fires[0]++);

    group.add(radio);

    assertThat(group.getSelected())
        .as("a selection-less group adopts an already-selected joiner")
        .isSameAs(radio);
    assertThat(radio.isSelected()).as("which keeps its own state").isTrue();
    assertThat(fires[0]).as("and the adoption is announced").isEqualTo(1);
  }

  @Test
  void aSelectedRadioJoiningAGroupThatHasOneIsDeselected() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton incumbent = new ElwhaRadioButton(true);
    group.add(incumbent);
    final ElwhaRadioButton joiner = new ElwhaRadioButton(true);

    group.add(joiner);

    assertThat(group.getSelected())
        .as("first-selected-wins, the ButtonGroup precedent — the incumbent keeps the selection")
        .isSameAs(incumbent);
    assertThat(joiner.isSelected()).as("and the joiner is the one that gives way").isFalse();
  }

  @Test
  void aSelectedJoinerBeingDeselectedDoesNotEmptyTheGroup() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton incumbent = new ElwhaRadioButton(true);
    group.add(incumbent);
    final int[] fires = {0};
    group.addSelectionChangeListener(e -> fires[0]++);

    group.add(new ElwhaRadioButton(true));

    assertThat(group.getSelected())
        .as("the cascade that deselects the joiner must not report back as a group change")
        .isSameAs(incumbent);
    assertThat(fires[0]).as("so nothing is announced").isZero();
  }

  @Test
  void anUnselectedJoinerLeavesTheSelectionAlone() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton incumbent = new ElwhaRadioButton(true);
    group.add(incumbent);

    group.add(new ElwhaRadioButton());

    assertThat(group.getSelected()).as("an ordinary join changes nothing").isSameAs(incumbent);
  }

  // -------------------------------------------------------------- exclusion

  @Test
  void selectingAMemberDeselectsThePreviousOne() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setSelected(true);

    second.setSelected(true);

    assertThat(second.isSelected()).as("the new member is selected").isTrue();
    assertThat(first.isSelected()).as("and the group never holds two selections").isFalse();
    assertThat(group.getSelected()).as("the group answers the new one").isSameAs(second);
  }

  @Test
  void aUserGestureRoutesTheExclusionToo() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton(true);
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);

    second.doClick();

    assertThat(first.isSelected())
        .as("a click cascades exactly as a programmatic write does")
        .isFalse();
    assertThat(group.getSelected()).as("and the group follows").isSameAs(second);
  }

  @Test
  void deselectingTheSelectedMemberEmptiesTheGroup() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    group.add(radio);

    radio.setSelected(false);

    assertThat(group.getSelected()).as("a group may hold no selection at all").isNull();
  }

  @Test
  void groupSetSelectedMovesTheSelectionWithoutAUserGesture() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton(true);
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final int[] actions = {0};
    second.addActionListener(e -> actions[0]++);

    group.setSelected(second);

    assertThat(group.getSelected()).as("the selection moves").isSameAs(second);
    assertThat(first.isSelected()).as("and the previous holder gives way").isFalse();
    assertThat(actions[0]).as("but this is not a user gesture, so no action event").isZero();
  }

  @Test
  void groupSetSelectedRejectsNonMembers() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(new ElwhaRadioButton());

    assertThatThrownBy(() -> group.setSelected(new ElwhaRadioButton()))
        .as("a group can only select something it owns")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> group.setSelected(null))
        .as("and null is clearSelection's job, not this one's")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void clearSelectionEmptiesTheGroup() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    group.add(radio);

    group.clearSelection();

    assertThat(group.getSelected()).as("the group holds nothing").isNull();
    assertThat(radio.isSelected()).as("and the member is really deselected").isFalse();
  }

  @Test
  void clearSelectionOnAnEmptyGroupFiresNothing() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(new ElwhaRadioButton());
    final int[] fires = {0};
    group.addSelectionChangeListener(e -> fires[0]++);

    group.clearSelection();

    assertThat(fires[0]).as("there was nothing to clear, so nothing changed").isZero();
  }

  @Test
  void removingTheSelectedMemberClearsTheGroupButNotTheRadio() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    group.add(radio);

    group.remove(radio);

    assertThat(group.getSelected()).as("the group's selection goes with the member").isNull();
    assertThat(radio.isSelected())
        .as("but the radio keeps its own state — it is no longer the group's business")
        .isTrue();
  }

  // ------------------------------------------------------------------ events

  @Test
  void movingTheSelectionFiresTheGroupListenerExactlyOnce() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton(true);
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final int[] fires = {0};
    group.addSelectionChangeListener(e -> fires[0]++);

    second.setSelected(true);

    assertThat(fires[0])
        .as("one selection move is one group event, not one per member that changed")
        .isEqualTo(1);
  }

  @Test
  void bothMembersOwnListenersStillFireOnAMove() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton(true);
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final int[] firstChanges = {0};
    final int[] secondChanges = {0};
    first.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> firstChanges[0]++);
    second.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> secondChanges[0]++);

    second.setSelected(true);

    assertThat(secondChanges[0]).as("the arriving member tells its own subscribers").isEqualTo(1);
    assertThat(firstChanges[0]).as("and so does the departing one").isEqualTo(1);
  }

  @Test
  void groupStateIsSettledInsideEveryListener() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton(true);
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final List<ElwhaRadioButton> seenByGroup = new ArrayList<>();
    final List<ElwhaRadioButton> seenByDeparting = new ArrayList<>();
    group.addSelectionChangeListener(e -> seenByGroup.add(group.getSelected()));
    first.addPropertyChangeListener(
        ElwhaRadioButton.PROPERTY_SELECTED, e -> seenByDeparting.add(group.getSelected()));

    second.setSelected(true);

    assertThat(seenByGroup)
        .as("the group listener already sees the new selection when it runs")
        .containsExactly(second);
    assertThat(seenByDeparting)
        .as("and so does the deselected member's own listener, mid-cascade")
        .containsExactly(second);
  }

  @Test
  void groupEventNamesTheGroupAsItsSource() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    group.add(radio);
    final List<Object> sources = new ArrayList<>();
    group.addSelectionChangeListener(e -> sources.add(e.getSource()));

    radio.setSelected(true);

    assertThat(sources)
        .as("a listener watching several groups can tell which one moved")
        .containsExactly(group);
  }

  @Test
  void removedGroupListenersStopHearingChanges() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final int[] fires = {0};
    final PropertyChangeListener listener = e -> fires[0]++;
    group.addSelectionChangeListener(listener);
    first.setSelected(true);

    group.removeSelectionChangeListener(listener);
    second.setSelected(true);

    assertThat(fires[0]).as("a removed listener hears nothing further").isEqualTo(1);
  }

  @Test
  void movingASelectedRadioBetweenGroupsClearsTheOldGroup() {
    final ElwhaRadioGroup first = new ElwhaRadioGroup();
    final ElwhaRadioGroup second = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    first.add(radio);

    second.add(radio);

    assertThat(first.getSelected()).as("the old group loses the selection it held").isNull();
    assertThat(second.getSelected()).as("and the new one adopts it").isSameAs(radio);
  }

  // -------------------------------------------------------- property changes

  @Test
  void everyGroupSelectionChangeCarriesBothMembers() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    group.addSelectionChangeListener(events::add);

    first.setSelected(true);
    second.setSelected(true);
    group.clearSelection();

    assertThat(events).as("adopt, move, clear — three group changes").hasSize(3);
    assertThat(events.get(0).getOldValue()).as("an empty group starts from null").isNull();
    assertThat(events.get(0).getNewValue()).isSameAs(first);
    assertThat(events.get(1).getOldValue())
        .as("a move carries the member the selection left")
        .isSameAs(first);
    assertThat(events.get(1).getNewValue()).isSameAs(second);
    assertThat(events.get(2).getOldValue()).isSameAs(second);
    assertThat(events.get(2).getNewValue()).as("clearing lands on null").isNull();
  }

  @Test
  void groupEventCarriesTheKeyItWasSubscribedUnder() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    group.add(radio);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    group.addSelectionChangeListener(events::add);

    radio.setSelected(true);

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getPropertyName())
        .as("the group's only observable state is named, so subscription can be key-scoped")
        .isEqualTo(ElwhaRadioGroup.PROPERTY_SELECTED);
  }
}
