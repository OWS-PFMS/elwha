package com.owspfm.elwha.list;

/**
 * The capability a hosted component implements to participate fully in {@link ElwhaItemList}'s
 * selection and drag polish.
 *
 * <p>The list renders arbitrary {@link javax.swing.JComponent}s, but selection visuals and drag
 * chrome need the component's cooperation — the list cannot paint a selected state onto a component
 * that has no concept of one. This interface formalizes a contract {@code ElwhaCard} and {@code
 * ElwhaChip} already satisfied in substance before it existed; both implement it with near-zero new
 * code.
 *
 * <p>A hosted component that does <strong>not</strong> implement it still works. It simply gets no
 * selection visuals and no drag chrome, and click-versus-drag disambiguation falls back to the
 * list's own movement threshold rather than the component's own pending-click bookkeeping.
 *
 * <p><strong>The list owns selection.</strong> The list handles the press, consults its {@link
 * SelectionMode}, mutates the selection model, and then pushes the result onto every rendered view
 * through {@link #setSelected(boolean)}. Views must not self-toggle in response to a click while
 * list-interactive — that inverted wiring is what produced the card family's stale-visual and
 * selection-lost-on-filter defects, and {@link #setListInteractive(boolean)} exists to switch it
 * off.
 *
 * <p><strong>Fluent return types</strong>: every mutator returns {@code ElwhaListItemView} so
 * implementations can return their own type by covariant override and keep their existing fluent
 * builder API intact — the same arrangement {@link ElwhaList} uses.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public interface ElwhaListItemView {

  /**
   * Pushes the list's selection state onto this view's chrome. Called on every rendered view after
   * any selection change, including the ones the view itself triggered.
   *
   * @param selected whether the list considers this view's item selected
   * @return this view, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  ElwhaListItemView setSelected(boolean selected);

  /**
   * Returns the selection state currently painted by this view's chrome.
   *
   * @return whether the view is painting itself as selected
   * @version v0.5.0
   * @since v0.5.0
   */
  boolean isSelected();

  /**
   * Discards the click this view has pending, because a drag has just won the gesture. Without it,
   * the release that ends a drag also fires the view's action or selection toggle.
   *
   * @return this view, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  ElwhaListItemView cancelPendingClick();

  /**
   * Hands interaction ownership to the list, or takes it back.
   *
   * <p>While list-interactive, the view responds to clicks but does not change its own selected
   * state — the list writes that through {@link #setSelected(boolean)}. Passing {@code false}
   * restores whatever interaction configuration the view carried beforehand.
   *
   * @param interactive true to put the view under list-driven interaction, false to release it
   * @return this view, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  ElwhaListItemView setListInteractive(boolean interactive);

  /**
   * Applies or clears drag chrome for the duration of a drag — an elevation lift and a dragged
   * state layer on views that paint them.
   *
   * <p>Defaults to doing nothing, since a view with no elevation has nothing to lift.
   *
   * @param dragged whether this view is the one being dragged right now
   * @return this view, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  default ElwhaListItemView setDragged(boolean dragged) {
    return this;
  }
}
