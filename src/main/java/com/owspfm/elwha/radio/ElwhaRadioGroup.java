package com.owspfm.elwha.radio;

import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
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
 * <p><strong>Keyboard (design §9 — material-web's controller contract, verbatim).</strong> Arrows
 * navigate in membership order — Up/Left to the previous member, Down/Right to the next — wrapping
 * at both ends and skipping disabled members; horizontal arrows flip under the focused member's
 * right-to-left {@link java.awt.ComponentOrientation}. <em>Selection follows focus</em>: arriving
 * at a member focuses <em>and selects</em> it as a user gesture (its {@code ActionListener}s fire).
 * The group is a single <strong>roving tab stop</strong>: with a member selected, only it is
 * focusable; while a member is focused, only it is focusable; with none selected or focused, every
 * enabled member is focusable (the three material-web rules). Bindings and focus management are
 * installed by {@link #add} and removed by {@link #remove} — an ungrouped radio has no arrow
 * behavior and plain focusability.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaRadioGroup {

  private static final String ACTION_UP = "elwhaRadioGroup.up";
  private static final String ACTION_DOWN = "elwhaRadioGroup.down";
  private static final String ACTION_LEFT = "elwhaRadioGroup.left";
  private static final String ACTION_RIGHT = "elwhaRadioGroup.right";
  private static final String[] ACTION_KEYS = {ACTION_UP, ACTION_DOWN, ACTION_LEFT, ACTION_RIGHT};
  private static final int[][] KEY_CODES = {
    {KeyEvent.VK_UP, KeyEvent.VK_KP_UP},
    {KeyEvent.VK_DOWN, KeyEvent.VK_KP_DOWN},
    {KeyEvent.VK_LEFT, KeyEvent.VK_KP_LEFT},
    {KeyEvent.VK_RIGHT, KeyEvent.VK_KP_RIGHT}
  };

  private final List<ElwhaRadioButton> members = new ArrayList<>();
  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private final FocusListener rovingFocusListener =
      new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
          if (navigationTarget == e.getComponent()) {
            navigationTarget = null;
          }
          updateRovingFocus();
        }

        @Override
        public void focusLost(final FocusEvent e) {
          updateRovingFocus();
        }
      };
  private final PropertyChangeListener enabledListener = e -> updateRovingFocus();

  private ElwhaRadioButton selected;
  private ElwhaRadioButton navigationTarget;
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
    installNavigation(radio);
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
    updateRovingFocus();
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
    uninstallNavigation(radio);
    if (navigationTarget == radio) {
      navigationTarget = null;
    }
    if (selected == radio) {
      selected = null;
      fireGroupChanged();
    }
    updateRovingFocus();
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
      updateRovingFocus();
    } else if (selected == radio) {
      selected = null;
      fireGroupChanged();
      updateRovingFocus();
    }
  }

  // ------------------------------------------------------------ keyboard contract

  /** Installs the arrow bindings + roving-focus listeners on a joining member (design §9). */
  private void installNavigation(final ElwhaRadioButton radio) {
    for (int i = 0; i < ACTION_KEYS.length; i++) {
      for (final int keyCode : KEY_CODES[i]) {
        radio
            .getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(keyCode, 0), ACTION_KEYS[i]);
      }
    }
    radio.getActionMap().put(ACTION_UP, new NavigateAction(radio, -1, false));
    radio.getActionMap().put(ACTION_DOWN, new NavigateAction(radio, +1, false));
    radio.getActionMap().put(ACTION_LEFT, new NavigateAction(radio, -1, true));
    radio.getActionMap().put(ACTION_RIGHT, new NavigateAction(radio, +1, true));
    radio.addFocusListener(rovingFocusListener);
    radio.addPropertyChangeListener("enabled", enabledListener);
  }

  /** Removes everything {@link #installNavigation} added and restores plain focusability. */
  private void uninstallNavigation(final ElwhaRadioButton radio) {
    for (int i = 0; i < ACTION_KEYS.length; i++) {
      for (final int keyCode : KEY_CODES[i]) {
        radio.getInputMap(JComponent.WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(keyCode, 0));
      }
      radio.getActionMap().remove(ACTION_KEYS[i]);
    }
    radio.removeFocusListener(rovingFocusListener);
    radio.removePropertyChangeListener("enabled", enabledListener);
    radio.setFocusable(true);
  }

  /**
   * Moves the selection one step from {@code from} in membership order — wrapping, skipping
   * disabled members ("if we return to the host index, there is nothing to select") — then focuses
   * and <em>selects</em> the arrival as a user gesture (selection follows focus). The target is
   * held as a transient roving-stop override so the focus request survives the flag flips while
   * focus is in flight (design §14-3); a request that cannot succeed (headless, not in a window)
   * clears the override immediately.
   */
  void navigate(final ElwhaRadioButton from, final int step) {
    final int origin = members.indexOf(from);
    if (origin < 0) {
      return;
    }
    final int size = members.size();
    ElwhaRadioButton target = null;
    for (int n = 1; n < size; n++) {
      final ElwhaRadioButton candidate = members.get(((origin + step * n) % size + size) % size);
      if (candidate.isEnabled()) {
        target = candidate;
        break;
      }
      if (candidate == from) {
        break;
      }
    }
    if (target == null || target == from) {
      return;
    }
    navigationTarget = target;
    target.setFocusable(true);
    final boolean focusLikely = target.requestFocusFromGroupNavigation();
    target.commitUserSelect();
    if (!focusLikely) {
      navigationTarget = null;
    }
    updateRovingFocus();
  }

  /**
   * Applies the three material-web roving-tab-stop rules: a transient navigation target (focus in
   * flight) wins; else the focused member; else the enabled selected member; else every enabled
   * member is a stop. The current focus owner never loses focusability mid-hold — AWT would
   * auto-transfer focus — it settles on the next focus event instead.
   */
  private void updateRovingFocus() {
    ElwhaRadioButton stop = navigationTarget;
    if (stop == null) {
      for (final ElwhaRadioButton member : members) {
        if (member.isFocusOwner()) {
          stop = member;
          break;
        }
      }
    }
    if (stop == null && selected != null && selected.isEnabled()) {
      stop = selected;
    }
    for (final ElwhaRadioButton member : members) {
      if (stop == null) {
        member.setFocusable(true);
      } else {
        member.setFocusable(member == stop || member.isFocusOwner());
      }
    }
  }

  /**
   * One arrow-key step. Vertical arrows are direction-fixed; horizontal arrows flip under the
   * acting member's right-to-left {@link java.awt.ComponentOrientation} (research §B′).
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  private final class NavigateAction extends AbstractAction {

    private final ElwhaRadioButton radio;
    private final int step;
    private final boolean horizontal;

    NavigateAction(final ElwhaRadioButton radio, final int step, final boolean horizontal) {
      this.radio = radio;
      this.step = step;
      this.horizontal = horizontal;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (radio.getGroup() != ElwhaRadioGroup.this || !radio.isEnabled()) {
        return;
      }
      final boolean mirror = horizontal && !radio.getComponentOrientation().isLeftToRight();
      navigate(radio, mirror ? -step : step);
    }
  }
}
