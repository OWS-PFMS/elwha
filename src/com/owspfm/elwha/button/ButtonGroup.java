package com.owspfm.elwha.button;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Enforces mutually-exclusive selection across a set of {@link ElwhaButton}s — the segmented-radio
 * pattern Swing's {@link javax.swing.ButtonGroup} provides for {@link javax.swing.AbstractButton}s.
 * Lives here because {@link ElwhaButton} extends {@link javax.swing.JComponent} directly rather
 * than {@code AbstractButton}, so Swing's {@code ButtonGroup} can't accept it.
 *
 * <p><strong>Mandatory mode.</strong> A non-mandatory group ({@link #ButtonGroup()}) lets the user
 * deselect the active button by clicking it again, leaving the group with no selection — matches
 * Swing's {@code ButtonGroup} default. A mandatory group ({@link #setMandatory(boolean)
 * setMandatory(true)}) refuses the deselect — clicking the currently-selected mandatory button is a
 * no-op (the group re-asserts the selection inside its own listener). Calling {@link
 * #setSelected(ElwhaButton) setSelected(null)} on a mandatory group throws {@link
 * IllegalStateException}.
 *
 * <p><strong>Member requirement.</strong> Only {@link ButtonInteractionMode#SELECTABLE} buttons may
 * be added; {@link #add(ElwhaButton)} throws {@link IllegalArgumentException} otherwise.
 *
 * <p><strong>Listener contract.</strong> {@link
 * #addSelectionChangeListener(PropertyChangeListener)} subscribes a listener to {@link
 * #PROPERTY_SELECTED_BUTTON} events; the event's {@code oldValue} and {@code newValue} are {@link
 * ElwhaButton} references (or {@code null} for "no selection"). Events fire only when the group's
 * selected button transitions — internal flips during a click that swaps between two members
 * produce exactly one event reporting the new selection.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ButtonGroup viewMode = new ButtonGroup()
 *     .setMandatory(true)
 *     .add(listButton)
 *     .add(gridButton)
 *     .add(compactButton);
 * listButton.setSelected(true);  // seed initial selection — required for mandatory mode
 * viewMode.addSelectionChangeListener(
 *     evt -> System.out.println("now: " + ((ElwhaButton) evt.getNewValue()).getText()));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ButtonGroup {

  /**
   * Property name fired when the group's selected button changes. The event's {@code oldValue} and
   * {@code newValue} are {@link ElwhaButton} references (or {@code null}).
   */
  public static final String PROPERTY_SELECTED_BUTTON = "selectedButton";

  private final List<ElwhaButton> buttons = new ArrayList<>();
  private final PropertyChangeListener memberListener;
  private final PropertyChangeSupport pcs;

  private boolean mandatory;
  private boolean adjusting;
  private ElwhaButton currentSelected;

  /**
   * Creates a non-mandatory group — clicking the selected button deselects it.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonGroup() {
    this.memberListener = this::onMemberSelectionChanged;
    this.pcs = new PropertyChangeSupport(this);
  }

  /**
   * Adds a button to the group. The button must be in {@link ButtonInteractionMode#SELECTABLE}
   * mode; clickable buttons cannot participate in mutex selection. If the button is already
   * selected at add-time, any other already-selected members are deselected to enforce the
   * one-selected invariant.
   *
   * @param button the button to add
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code button} is {@code null}
   * @throws IllegalArgumentException if {@code button} is not in {@code SELECTABLE} mode
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonGroup add(final ElwhaButton button) {
    if (button == null) {
      throw new NullPointerException("button");
    }
    if (button.getInteractionMode() != ButtonInteractionMode.SELECTABLE) {
      throw new IllegalArgumentException(
          "ButtonGroup requires SELECTABLE buttons; got " + button.getInteractionMode());
    }
    if (buttons.contains(button)) {
      return this;
    }
    buttons.add(button);
    button.addSelectionChangeListener(memberListener);
    if (button.isSelected()) {
      adjusting = true;
      try {
        for (ElwhaButton other : buttons) {
          if (other != button && other.isSelected()) {
            other.setSelected(false);
          }
        }
      } finally {
        adjusting = false;
      }
      updateCurrentSelected(button);
    }
    return this;
  }

  /**
   * Removes a button from the group. The button resumes independent toggle behavior; its selected
   * state at the time of removal is preserved.
   *
   * @param button the button to remove
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonGroup remove(final ElwhaButton button) {
    if (button != null && buttons.remove(button)) {
      button.removePropertyChangeListener(ElwhaButton.PROPERTY_SELECTED, memberListener);
      if (currentSelected == button) {
        updateCurrentSelected(findSelected());
      }
    }
    return this;
  }

  /**
   * Returns the currently-selected button, or {@code null} if no member is selected.
   *
   * @return the selected button, or {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton getSelected() {
    return findSelected();
  }

  /**
   * Programmatically sets the selected button. Passing {@code null} clears the selection (only
   * valid when {@link #isMandatory()} is {@code false}); passing a button not in this group throws.
   *
   * @param button the button to select, or {@code null} to clear
   * @return {@code this} for fluent chaining
   * @throws IllegalArgumentException if {@code button} is not in this group
   * @throws IllegalStateException if {@code button} is {@code null} and the group is mandatory
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonGroup setSelected(final ElwhaButton button) {
    if (button == null) {
      if (mandatory) {
        throw new IllegalStateException(
            "Cannot clear selection on a mandatory ButtonGroup — exactly one button must remain"
                + " selected.");
      }
      adjusting = true;
      try {
        for (ElwhaButton b : buttons) {
          if (b.isSelected()) {
            b.setSelected(false);
          }
        }
      } finally {
        adjusting = false;
      }
      updateCurrentSelected(null);
      return this;
    }
    if (!buttons.contains(button)) {
      throw new IllegalArgumentException("button is not a member of this ButtonGroup");
    }
    adjusting = true;
    try {
      for (ElwhaButton b : buttons) {
        if (b == button) {
          if (!b.isSelected()) {
            b.setSelected(true);
          }
        } else if (b.isSelected()) {
          b.setSelected(false);
        }
      }
    } finally {
      adjusting = false;
    }
    updateCurrentSelected(button);
    return this;
  }

  /**
   * Returns whether the group is in mandatory mode.
   *
   * @return {@code true} if mandatory
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Switches between non-mandatory and mandatory modes. Toggling mandatory on while the group has
   * no selection is permitted — the next click on a member becomes the locked-in selection.
   *
   * @param mandatory {@code true} to forbid deselect; {@code false} to allow it
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonGroup setMandatory(final boolean mandatory) {
    this.mandatory = mandatory;
    return this;
  }

  /**
   * Returns the number of buttons in the group.
   *
   * @return the group size
   * @version v0.2.0
   * @since v0.2.0
   */
  public int size() {
    return buttons.size();
  }

  /**
   * Subscribes a listener to {@link #PROPERTY_SELECTED_BUTTON} events. The event's {@code oldValue}
   * and {@code newValue} are {@link ElwhaButton} references (or {@code null} for "no selection").
   *
   * @param listener the listener; {@code null} is ignored
   * @version v0.2.0
   * @since v0.2.0
   */
  public void addSelectionChangeListener(final PropertyChangeListener listener) {
    if (listener != null) {
      pcs.addPropertyChangeListener(PROPERTY_SELECTED_BUTTON, listener);
    }
  }

  /**
   * Removes a previously registered selection listener.
   *
   * @param listener the listener to remove
   * @version v0.2.0
   * @since v0.2.0
   */
  public void removeSelectionChangeListener(final PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(PROPERTY_SELECTED_BUTTON, listener);
  }

  // ----------------------------------------------------------- internals

  private void onMemberSelectionChanged(final PropertyChangeEvent evt) {
    if (adjusting) {
      return;
    }
    final ElwhaButton source = (ElwhaButton) evt.getSource();
    final boolean nowSelected = Boolean.TRUE.equals(evt.getNewValue());
    adjusting = true;
    try {
      if (nowSelected) {
        for (ElwhaButton other : buttons) {
          if (other != source && other.isSelected()) {
            other.setSelected(false);
          }
        }
        updateCurrentSelected(source);
      } else if (mandatory && findSelected() == null) {
        // Mandatory group: refuse deselect by re-asserting the source button's selection. The
        // brief false→true transition is internal; the no-op observer view is enforced by the
        // adjusting flag suppressing the intermediate event.
        source.setSelected(true);
      } else {
        updateCurrentSelected(findSelected());
      }
    } finally {
      adjusting = false;
    }
  }

  private ElwhaButton findSelected() {
    for (ElwhaButton b : buttons) {
      if (b.isSelected()) {
        return b;
      }
    }
    return null;
  }

  private void updateCurrentSelected(final ElwhaButton newSelected) {
    if (newSelected == currentSelected) {
      return;
    }
    final ElwhaButton old = currentSelected;
    currentSelected = newSelected;
    pcs.firePropertyChange(PROPERTY_SELECTED_BUTTON, old, newSelected);
  }
}
