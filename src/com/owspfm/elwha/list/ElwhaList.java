package com.owspfm.elwha.list;

import java.awt.Insets;
import java.util.Comparator;
import java.util.function.Predicate;
import javax.swing.JComponent;

/**
 * The shared contract implemented by every {@code ElwhaList} component family member — {@code
 * ElwhaCardList}, {@code ElwhaChipList}, and any future siblings (e.g. {@code FlatTagList}).
 *
 * <p>Extracted in story #237 so consumers can write orientation-agnostic and family-agnostic code:
 *
 * <pre>{@code
 * void configureForCompactMode(ElwhaList<?> list) {
 *   list.setOrientation(ElwhaListOrientation.HORIZONTAL);
 *   list.setItemGap(4);
 *   list.setListPadding(new Insets(2, 4, 2, 4));
 * }
 * }</pre>
 *
 * <p>This interface deliberately stays narrow — it covers only the cross-cutting concerns that
 * every family member is expected to expose:
 *
 * <ul>
 *   <li>Orientation, item gap, list padding
 *   <li>Empty / loading-state placeholders
 *   <li>Filter and sort predicates over the visible items
 * </ul>
 *
 * <p>Selection, drag-to-reorder, and per-family model types stay on the concrete classes since they
 * have family-specific signatures ({@code ElwhaChip} vs {@code ElwhaCard}, {@code ChipSelectionMode}
 * vs {@code CardSelectionMode}, etc.). Those cross the abstraction barrier only via the
 * per-implementation API.
 *
 * <p><strong>Fluent return types</strong>: every mutator returns {@code ElwhaList<T>} so concrete
 * implementations can return their own type via covariant override and keep their existing fluent
 * builder API intact.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public interface ElwhaList<T> {

  /**
   * Returns the active orientation.
   *
   * @return the orientation (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaListOrientation getOrientation();

  /**
   * Sets the layout orientation. Implementations not yet supporting a given orientation may either
   * throw {@link UnsupportedOperationException} or fall back to a supported value with a one-shot
   * warning — see the concrete class documentation.
   *
   * @param orientation the new orientation
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setOrientation(ElwhaListOrientation orientation);

  /**
   * Returns the gap between rendered items.
   *
   * @return the gap in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  int getItemGap();

  /**
   * Sets the gap between rendered items.
   *
   * @param gap pixels, clamped to {@code >= 0}
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setItemGap(int gap);

  /**
   * Sets the padding around the rendered list.
   *
   * @param insets the insets; null treated as zero
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setListPadding(Insets insets);

  /**
   * Sets the column count for {@link ElwhaListOrientation#GRID}. No effect on other orientations.
   *
   * @param columns column count, clamped to {@code >= 1}
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setColumns(int columns);

  /**
   * Returns the column count for grid mode.
   *
   * @return the column count
   * @version v0.1.0
   * @since v0.1.0
   */
  int getColumns();

  /**
   * Replaces the empty-state placeholder. Pass {@code null} to restore the built-in default.
   *
   * @param component the placeholder
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setEmptyState(JComponent component);

  /**
   * Sets the loading flag. While true, the list renders the loading component instead of items.
   *
   * @param loading whether to show the loading state
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setLoading(boolean loading);

  /**
   * Replaces the loading-state component. Pass {@code null} to restore the built-in default.
   *
   * @param component the loading component
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setLoadingComponent(JComponent component);

  /**
   * Sets a filter predicate that hides items rejected by it. Pass null to clear.
   *
   * @param filter the predicate; null clears filtering
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setFilter(Predicate<T> filter);

  /**
   * Sets a sort comparator that orders rendered items. Pass null to clear.
   *
   * @param comparator the comparator; null clears sorting
   * @return this list (for fluent chaining)
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaList<T> setSortOrder(Comparator<T> comparator);
}
