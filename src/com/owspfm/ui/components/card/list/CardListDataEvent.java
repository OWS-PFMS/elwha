package com.owspfm.ui.components.card.list;

import java.util.EventObject;

/**
 * Fine-grained change event fired by a {@link CardListModel}.
 *
 * <p>Modeled on {@link javax.swing.event.ListDataEvent} but extends the type taxonomy with an
 * explicit {@link Type#MOVED} event so the list can animate a reorder without losing item identity
 * (a remove-then-add sequence visually destroys and re-creates the moved item, which breaks
 * selection survival, focus, and slide animations).
 *
 * <p>Index semantics:
 *
 * <ul>
 *   <li>{@link Type#ADDED} — items are now present at indices {@code [index0, index1]} (inclusive)
 *   <li>{@link Type#REMOVED} — items were at indices {@code [index0, index1]} prior to removal
 *   <li>{@link Type#CHANGED} — items at indices {@code [index0, index1]} were replaced in place
 *   <li>{@link Type#MOVED} — exactly one item moved from {@code index0} to {@code index1}
 * </ul>
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public class CardListDataEvent extends EventObject {

  /** The kind of structural change a {@link CardListDataEvent} represents. */
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

  private final Type myType;
  private final int myIndex0;
  private final int myIndex1;

  /**
   * Constructs a new event.
   *
   * @param theSource the originating model
   * @param theType the kind of change
   * @param theIndex0 the first affected index (or source index for {@link Type#MOVED})
   * @param theIndex1 the last affected index (or destination index for {@link Type#MOVED})
   */
  public CardListDataEvent(
      final Object theSource, final Type theType, final int theIndex0, final int theIndex1) {
    super(theSource);
    myType = theType;
    myIndex0 = theIndex0;
    myIndex1 = theIndex1;
  }

  /**
   * Returns the change type.
   *
   * @return the change type
   */
  public Type getType() {
    return myType;
  }

  /**
   * Returns the lower bound index, or the source index for moves.
   *
   * @return index0
   */
  public int getIndex0() {
    return myIndex0;
  }

  /**
   * Returns the upper bound index, or the destination index for moves.
   *
   * @return index1
   */
  public int getIndex1() {
    return myIndex1;
  }
}
