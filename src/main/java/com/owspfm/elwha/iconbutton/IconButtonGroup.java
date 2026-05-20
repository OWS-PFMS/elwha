package com.owspfm.elwha.iconbutton;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enforces mutually-exclusive selection across a set of {@link ElwhaIconButton}s — the toolbar
 * "radio" pattern Swing's {@link javax.swing.ButtonGroup} provides for {@link
 * javax.swing.AbstractButton}s. Lives here because {@link ElwhaIconButton} extends {@link
 * javax.swing.JComponent} directly rather than {@code AbstractButton}, so {@code ButtonGroup} can't
 * accept it.
 *
 * <p><strong>Mode.</strong> A non-mandatory group ({@link #IconButtonGroup()}) lets the user
 * deselect the active button by clicking it again, leaving the group with no selection — matches
 * Swing's {@code ButtonGroup} default. A mandatory group ({@link #IconButtonGroup(boolean)} with
 * {@code true}) refuses the deselect, mirroring the chip side's {@code SINGLE_MANDATORY} mode
 * (always exactly one selected once a selection exists).
 *
 * <p><strong>Lifecycle.</strong> The group only enforces single-selection while it's listening;
 * removing a button via {@link #remove(ElwhaIconButton)} detaches the listener so the button
 * resumes independent toggle behavior. The group does not hold strong references to buttons that
 * have been removed.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * IconButtonGroup viewMode = new IconButtonGroup(true)  // mandatory
 *     .add(gridButton)
 *     .add(tableButton)
 *     .add(tilesButton);
 * gridButton.setSelected(true);  // initial selection — required for mandatory groups
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class IconButtonGroup {

  private final boolean mandatory;
  private final List<ElwhaIconButton> buttons = new ArrayList<>();
  private final PropertyChangeListener listener;
  private boolean adjusting;

  /** Creates a non-mandatory group — the user may deselect the active button. */
  public IconButtonGroup() {
    this(false);
  }

  /**
   * Creates a group with the specified mode.
   *
   * @param mandatory {@code true} to forbid deselect (one button always selected once a selection
   *     exists); {@code false} to allow deselect (Swing's {@code ButtonGroup} default behavior)
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconButtonGroup(final boolean mandatory) {
    this.mandatory = mandatory;
    this.listener = this::onSelectionChanged;
  }

  /**
   * Adds a button to the group. The button's interaction mode must be {@link
   * IconButtonInteractionMode#SELECTABLE} — clickable buttons can't participate in a
   * mutually-exclusive selection. Returns {@code this} for fluent chaining.
   *
   * @param button the button to add
   * @return {@code this}
   * @throws NullPointerException if {@code button} is {@code null}
   * @throws IllegalArgumentException if {@code button} is not in {@code SELECTABLE} mode
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconButtonGroup add(final ElwhaIconButton button) {
    if (button == null) {
      throw new NullPointerException("button");
    }
    if (button.getInteractionMode() != IconButtonInteractionMode.SELECTABLE) {
      throw new IllegalArgumentException(
          "IconButtonGroup requires SELECTABLE buttons; got " + button.getInteractionMode());
    }
    if (!buttons.contains(button)) {
      buttons.add(button);
      button.addSelectionChangeListener(listener);
    }
    return this;
  }

  /**
   * Removes a button from the group. The button resumes independent toggle behavior.
   *
   * @param button the button to remove
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconButtonGroup remove(final ElwhaIconButton button) {
    if (buttons.remove(button) && button != null) {
      button.removePropertyChangeListener(ElwhaIconButton.PROPERTY_SELECTED, listener);
    }
    return this;
  }

  /**
   * Returns the currently-selected button, or {@link Optional#empty()} if none is selected.
   *
   * @return the selected button, or empty
   * @version v0.1.0
   * @since v0.1.0
   */
  public Optional<ElwhaIconButton> getSelected() {
    return buttons.stream().filter(ElwhaIconButton::isSelected).findFirst();
  }

  /**
   * Returns the number of buttons in the group.
   *
   * @return the group size
   * @version v0.1.0
   * @since v0.1.0
   */
  public int size() {
    return buttons.size();
  }

  /**
   * Returns whether this group is mandatory — i.e., refuses deselects when only one button is
   * selected.
   *
   * @return {@code true} if mandatory
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isMandatory() {
    return mandatory;
  }

  private void onSelectionChanged(final java.beans.PropertyChangeEvent evt) {
    if (adjusting) {
      return;
    }
    final ElwhaIconButton source = (ElwhaIconButton) evt.getSource();
    final boolean nowSelected = Boolean.TRUE.equals(evt.getNewValue());
    adjusting = true;
    try {
      if (nowSelected) {
        for (ElwhaIconButton other : buttons) {
          if (other != source && other.isSelected()) {
            other.setSelected(false);
          }
        }
      } else if (mandatory && getSelected().isEmpty()) {
        // User tried to clear the only selection in a mandatory group — re-assert it.
        source.setSelected(true);
      }
    } finally {
      adjusting = false;
    }
  }
}
