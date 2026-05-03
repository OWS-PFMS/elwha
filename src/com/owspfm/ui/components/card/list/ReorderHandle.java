package com.owspfm.ui.components.card.list;

/**
 * Where a {@link FlatCardList} watches for the start of a reorder drag.
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public enum ReorderHandle {

  /** The whole card surface is draggable. The simplest setup but conflicts with click selection. */
  WHOLE_CARD,

  /**
   * Drags begin only when the mouse press lands on the card's leading-icon area. Pairs well with
   * cards that already use the leading icon as a visual handle.
   */
  LEADING_ICON,

  /**
   * Drags begin only when the mouse press lands on a small grip glyph appended to the trailing
   * actions row. The list installs the glyph automatically when reorder is enabled with this
   * handle.
   */
  TRAILING_HANDLE
}
