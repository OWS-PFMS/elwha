package com.owspfm.elwha.radio;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The Elwha Material 3 <strong>radio group</strong> — a <em>non-visual</em> mutual-exclusion
 * controller over {@link ElwhaRadioButton}s (design doc {@code elwha-radiobutton-design.md} §8).
 * Layout stays free-form: members can live in any container or geometry; the group only coordinates
 * state. It is the Elwha-native answer to Android's {@code RadioGroup} and material-web's
 * per-{@code name} selection controller — {@code javax.swing.ButtonGroup} is structurally
 * unavailable (it requires {@code AbstractButton}).
 *
 * <p><strong>Exclusion.</strong> Any member reaching {@code selected == true} — a user gesture or a
 * programmatic {@link ElwhaRadioButton#setSelected setSelected(true)} — deselects the previously
 * selected member; the group never holds two selections. Adding an already-selected radio to a
 * group that has a selection deselects the <em>incoming</em> radio (first-selected-wins, the {@code
 * ButtonGroup} precedent); adding one to a selection-less group adopts it as the selection. A radio
 * belongs to at most one group — adding it to a second group removes it from the first. Removing
 * the selected member clears the group's selection (the removed radio keeps its own state).
 *
 * <p><strong>Events.</strong> {@link #addChangeListener} fires once per group-selection change,
 * including to and from empty. It fires <em>before</em> the changing members' own {@code
 * ChangeListener}s complete their cascade, and the group's state is consistent ({@link
 * #getSelected()} already answers the new member) inside every listener. Members' own listeners
 * still fire per-member — the deselected sibling tells its subscribers itself.
 *
 * <p><strong>Navigation order.</strong> Membership order is the arrow-key navigation order the
 * group keyboard contract uses (design §9).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaRadioGroup {

  private final List<ElwhaRadioButton> members = new ArrayList<>();
  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private ElwhaRadioButton selected;
  private boolean cascading;

  /**
   * Creates an empty radio group.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioGroup() {}

  /**
   * Adds a radio to this group, at the end of the navigation order. Adding a member of another
   * group moves it (it leaves the old group first); re-adding an existing member is a no-op. An
   * already-selected radio joining a group that has a selection is deselected
   * (first-selected-wins); joining a selection-less group, it becomes the selection.
   *
   * @param radio the radio to add
   * @throws IllegalArgumentException if {@code radio} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void add(final ElwhaRadioButton radio) {
    if (radio == null) {
      throw new IllegalArgumentException("radio must not be null");
    }
    if (members.contains(radio)) {
      return;
    }
    final ElwhaRadioGroup current = radio.getGroup();
    if (current != null) {
      current.remove(radio);
    }
    members.add(radio);
    radio.setGroup(this);
    if (radio.isSelected()) {
      if (selected == null) {
        selected = radio;
        fireGroupChanged();
      } else {
        cascading = true;
        try {
          radio.setSelected(false);
        } finally {
          cascading = false;
        }
      }
    }
  }

  /**
   * Removes a radio from this group. Removing the selected member clears the group's selection; the
   * removed radio keeps its own selected state. Removing a non-member is a no-op.
   *
   * @param radio the radio to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void remove(final ElwhaRadioButton radio) {
    if (!members.remove(radio)) {
      return;
    }
    radio.setGroup(null);
    if (selected == radio) {
      selected = null;
      fireGroupChanged();
    }
  }

  /**
   * Returns the group's members in navigation order.
   *
   * @return an unmodifiable snapshot of the members
   * @version v0.4.0
   * @since v0.4.0
   */
  public List<ElwhaRadioButton> getMembers() {
    return List.copyOf(members);
  }

  /**
   * Returns the selected member, or {@code null} when no member is selected.
   *
   * @return the selected member, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioButton getSelected() {
    return selected;
  }

  /**
   * Selects the given member programmatically — the previously selected member deselects, both
   * members' {@code ChangeListener}s fire, the group listener fires once; no {@code
   * ActionListener}s fire (not a user gesture).
   *
   * @param radio the member to select
   * @throws IllegalArgumentException if {@code radio} is {@code null} or not a member — use {@link
   *     #clearSelection()} to empty the selection
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSelected(final ElwhaRadioButton radio) {
    if (radio == null || !members.contains(radio)) {
      throw new IllegalArgumentException("radio must be a member of this group");
    }
    radio.setSelected(true);
  }

  /**
   * Clears the selection — the selected member (if any) deselects and the group listener fires.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void clearSelection() {
    if (selected != null) {
      selected.setSelected(false);
    }
  }

  /**
   * Adds a {@link ChangeListener} notified once per group-selection change — selecting a member,
   * moving the selection between members, or clearing it.
   *
   * @param listener the listener to add
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addChangeListener(final ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  /**
   * Removes a previously added {@link ChangeListener}.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  /** Notifies registered {@link ChangeListener}s of a group-selection change. */
  private void fireGroupChanged() {
    for (final ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(changeEvent);
    }
  }

  /**
   * Member-state callback from {@link ElwhaRadioButton#setSelected}: a member turning on adopts the
   * selection and cascades the previous holder off (reentrant cascade guarded); the selected member
   * turning off empties the selection.
   */
  void memberSelectionChanged(final ElwhaRadioButton radio, final boolean nowSelected) {
    if (cascading) {
      return;
    }
    if (nowSelected) {
      final ElwhaRadioButton previous = selected;
      if (previous == radio) {
        return;
      }
      selected = radio;
      if (previous != null) {
        cascading = true;
        try {
          previous.setSelected(false);
        } finally {
          cascading = false;
        }
      }
      fireGroupChanged();
    } else if (selected == radio) {
      selected = null;
      fireGroupChanged();
    }
  }
}
