package com.owspfm.elwha.card.list;

/**
 * Where a {@link ElwhaCardList} watches for the start of a reorder drag.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ReorderHandle {

  /** The whole card surface is draggable. The simplest setup but conflicts with click selection. */
  WHOLE_CARD,

  /**
   * Drags begin only when the mouse press lands on the card's leading-icon area. Pairs well with
   * cards that already use the leading icon as a visual handle.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LEADING_ICON,

  /**
   * Drags begin only when the mouse press lands on a small grip glyph appended to the trailing
   * actions row. The list installs the glyph automatically when reorder is enabled with this
   * handle.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TRAILING_HANDLE
}
