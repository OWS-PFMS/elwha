package com.owspfm.elwha.list;

/**
 * How {@link ElwhaItemList} signals that an item can be dragged to reorder.
 *
 * <p>Introduced in story #69 (spec §7). The two shipped list families offered only the cursor swap;
 * the hover-revealed drag handle is new with the unified list.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public enum ReorderAffordance {

  /**
   * The pointer changes to an open-hand "grab" cursor over a draggable item and to a closed-fist
   * "grabbing" cursor for the duration of the drag, restoring to the hover cursor on release. The
   * default, and the behavior both shipped families had.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  CURSOR_SWAP,

  /**
   * A drag-handle glyph fades in over the item's leading edge while the pointer is over it, and
   * stays visible for the duration of a drag. The cursor is left alone.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  HOVER_ICON,

  /**
   * Both {@link #CURSOR_SWAP} and {@link #HOVER_ICON}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  BOTH,

  /**
   * No affordance — items still drag, but nothing advertises it. For lists whose reorder is
   * discovered through the context menu or the keyboard.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  NONE
}
