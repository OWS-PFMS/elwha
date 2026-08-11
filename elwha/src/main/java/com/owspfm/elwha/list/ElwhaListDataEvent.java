package com.owspfm.elwha.list;

import java.util.EventObject;

/**
 * Fine-grained change event fired by an {@link ElwhaListModel}.
 *
 * <p>The unified successor to the card and chip families' parallel event classes (spec §5). It adds
 * the {@link Type#STRUCTURE} constant the older pair lacked, and carries the item type so listeners
 * are typed end to end.
 *
 * <p>Index semantics:
 *
 * <ul>
 *   <li>{@link Type#ADDED} — items are now present at indices {@code [index0, index1]} inclusive
 *   <li>{@link Type#REMOVED} — items were at indices {@code [index0, index1]} prior to removal
 *   <li>{@link Type#CHANGED} — items at indices {@code [index0, index1]} were replaced in place
 *   <li>{@link Type#MOVED} — exactly one item moved from {@code index0} to {@code index1}
 *   <li>{@link Type#STRUCTURE} — the contents changed wholesale; both indices are {@code -1}
 * </ul>
 *
 * @param <T> the item type
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaListDataEvent<T> extends EventObject {

  /**
   * The kind of structural change an {@link ElwhaListDataEvent} represents.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public enum Type {

    /**
     * One or more contiguous items were inserted at {@code [index0, index1]}.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    ADDED,

    /**
     * One or more contiguous items were removed from {@code [index0, index1]}.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    REMOVED,

    /**
     * One or more contiguous items at {@code [index0, index1]} were replaced in place.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    CHANGED,

    /**
     * Exactly one item moved from {@code index0} to {@code index1}.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    MOVED,

    /**
     * The contents changed wholesale — {@code setItems} or {@code clear}. Both indices are {@code
     * -1}; listeners rebuild rather than patch.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    STRUCTURE
  }

  private final transient Type type;
  private final int index0;
  private final int index1;

  /**
   * Constructs a new event.
   *
   * @param source the originating model
   * @param type the kind of change
   * @param index0 the first affected index, the source index for {@link Type#MOVED}, or {@code -1}
   *     for {@link Type#STRUCTURE}
   * @param index1 the last affected index, the destination index for {@link Type#MOVED}, or {@code
   *     -1} for {@link Type#STRUCTURE}
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaListDataEvent(
      final Object source, final Type type, final int index0, final int index1) {
    super(source);
    this.type = type;
    this.index0 = index0;
    this.index1 = index1;
  }

  /**
   * Returns the kind of change this event describes.
   *
   * @return the change type
   * @version v0.5.0
   * @since v0.5.0
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the lower-bound index, the source index for moves, or {@code -1} for structural
   * changes.
   *
   * @return the first affected index
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getIndex0() {
    return index0;
  }

  /**
   * Returns the upper-bound index, the destination index for moves, or {@code -1} for structural
   * changes.
   *
   * @return the last affected index
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getIndex1() {
    return index1;
  }
}
