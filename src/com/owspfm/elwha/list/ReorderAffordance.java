package com.owspfm.elwha.list;

/**
 * Visual idiom used to signal that an {@link ElwhaItemList} supports drag-to-reorder. Decoupled
 * from {@link ElwhaItemList#setReorderable(boolean)} (which controls <em>whether</em> drag is
 * accepted) — this enum controls <em>how the list hints at it</em>. Both legacy implementations are
 * preserved: Card used {@link #CURSOR_SWAP}; Chip used {@link #HOVER_ICON}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ReorderAffordance {

  /**
   * Card's idiom — the whole item shows a grab cursor on hover and a grabbing cursor while a drag
   * is active, via the bundled Capitaine cursor PNGs. No persistent visual hint when the item is
   * not hovered. Default.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  CURSOR_SWAP,

  /**
   * Chip's idiom — a leading-slot drag-handle icon appears on hover and stays while a drag is
   * active. The cursor stays default. Better for dense rows where a cursor swap would be noisy, and
   * pairs naturally with pin / anchor affordances since the leading slot is shared.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HOVER_ICON,

  /** Both — cursor swap AND the leading-slot icon appears on hover. Maximum discoverability. */
  BOTH,

  /**
   * Neither — drag is still enabled by {@link ElwhaItemList#setReorderable(boolean)}, but the list
   * adds no visual hint. Use when the consumer's items carry their own affordance.
   */
  NONE
}
