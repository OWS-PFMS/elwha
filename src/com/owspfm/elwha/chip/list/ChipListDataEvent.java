package com.owspfm.elwha.chip.list;

import java.util.EventObject;

/**
 * Fine-grained change event fired by a {@link ChipListModel}.
 *
 * <p>Mirrors {@link com.owspfm.elwha.card.list.CardListDataEvent} but lives in the chip
 * namespace so the two component families stay independent. Both eventually share a common base via
 * the {@code elwha.list} package extracted in story #237 — see that package's {@code
 * package-info} for the cross-family contract.
 *
 * <p>Index semantics mirror the parent contract:
 *
 * <ul>
 *   <li>{@link Type#ADDED} — items are now present at indices {@code [index0, index1]} (inclusive)
 *   <li>{@link Type#REMOVED} — items were at indices {@code [index0, index1]} prior to removal
 *   <li>{@link Type#CHANGED} — items at indices {@code [index0, index1]} were replaced in place
 *   <li>{@link Type#MOVED} — exactly one item moved from {@code index0} to {@code index1}
 * </ul>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ChipListDataEvent extends EventObject {

  /** The kind of structural change a {@link ChipListDataEvent} represents. */
  public enum Type {
    /** One or more contiguous items were inserted at {@code [index0, index1]}. */
    ADDED,
    /** One or more contiguous items were removed from {@code [index0, index1]}. */
    REMOVED,
    /** One or more contiguous items at {@code [index0, index1]} were replaced in place. */
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
  public ChipListDataEvent(
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
   * Returns the lower bound index, or the source index for moves.
   *
   * @return index0
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getIndex0() {
    return index0;
  }

  /**
   * Returns the upper bound index, or the destination index for moves.
   *
   * @return index1
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getIndex1() {
    return index1;
  }
}
