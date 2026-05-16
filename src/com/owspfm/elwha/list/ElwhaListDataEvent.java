package com.owspfm.elwha.list;

import java.util.EventObject;

/**
 * Fine-grained change event fired by an {@link ElwhaListModel}.
 *
 * <p>Replaces {@code CardListDataEvent} and {@code ChipListDataEvent}. Modeled on {@link
 * javax.swing.event.ListDataEvent} with an explicit {@link Type#MOVED} variant so a reorder
 * preserves item identity (a remove-then-add sequence destroys visual continuity).
 *
 * <p>Index semantics:
 *
 * <ul>
 *   <li>{@link Type#ADDED} — items are now at indices {@code [index0, index1]} (inclusive)
 *   <li>{@link Type#REMOVED} — items were at indices {@code [index0, index1]} before removal
 *   <li>{@link Type#CHANGED} — items at indices {@code [index0, index1]} were replaced in place
 *   <li>{@link Type#MOVED} — one item moved from {@code index0} to {@code index1}
 * </ul>
 *
 * @param <T> the item type (carried for symmetry with the generic listener)
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaListDataEvent<T> extends EventObject {

  /** The kind of structural change an {@link ElwhaListDataEvent} represents. */
  public enum Type {
    /** Contiguous items inserted at {@code [index0, index1]}. */
    ADDED,
    /** Contiguous items removed from {@code [index0, index1]}. */
    REMOVED,
    /** Contiguous items at {@code [index0, index1]} replaced in place. */
    CHANGED,
    /** Exactly one item moved from {@code index0} to {@code index1}. */
    MOVED
  }

  private final Type type;
  private final int index0;
  private final int index1;

  /**
   * Constructs a new event.
   *
   * @param source the originating model
   * @param type the kind of change
   * @param index0 the first affected index (or source index for {@link Type#MOVED})
   * @param index1 the last affected index (or destination index for {@link Type#MOVED})
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaListDataEvent(
      final Object source, final Type type, final int index0, final int index1) {
    super(source);
    this.type = type;
    this.index0 = index0;
    this.index1 = index1;
  }

  /**
   * Returns the change type.
   *
   * @return the change type
   * @version v0.1.0
   * @since v0.1.0
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the lower bound index (or source index for {@link Type#MOVED}).
   *
   * @return index0
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getIndex0() {
    return index0;
  }

  /**
   * Returns the upper bound index (or destination index for {@link Type#MOVED}).
   *
   * @return index1
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getIndex1() {
    return index1;
  }
}
