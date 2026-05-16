package com.owspfm.elwha.list;

/**
 * Where on a list item the user can initiate a drag-to-reorder gesture. Replaces {@code
 * com.owspfm.elwha.card.list.ReorderHandle}; {@code WHOLE_CARD} is renamed {@link #WHOLE_ITEM} (the
 * unified class is type-agnostic — items aren't always cards).
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ReorderHandle {

  /** Drag from anywhere on the item. Default; equivalent to the legacy WHOLE_CARD value. */
  WHOLE_ITEM,

  /** Drag only from the item's leading icon slot. */
  LEADING_ICON,

  /** Drag only from a dedicated trailing-edge grip. */
  TRAILING_HANDLE
}
