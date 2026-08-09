package com.owspfm.elwha.list;

import java.util.function.Predicate;

/**
 * Movement semantics for {@link ElwhaItemList}. Mutually exclusive — the list is in exactly one
 * mode at a time. The mode drives drag-enable, the reorder cursor / handle affordance, the
 * auto-injected context-menu sections, and which partition (if any) the render order honors.
 *
 * <p>Lifted verbatim from the chip family and hoisted to the top level (spec §8). For back-compat,
 * {@link ElwhaItemList#setReorderable(boolean)} maps to {@link #STATIC} (false) or {@link #MOVABLE}
 * (true), {@link ElwhaItemList#setPinPredicate(Predicate)} implicitly flips to {@link #PINNED}, and
 * {@link ElwhaItemList#setAnchorPredicate(Predicate)} to {@link #ANCHORED}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public enum MovementMode {

  /**
   * Items are display-only. No drag, no reorder menu items, no grab cursor or drag handle; the pin
   * and anchor APIs are inert. Default for a fresh list.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  STATIC,

  /**
   * Items can be freely reordered by drag. No partition.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  MOVABLE,

  /**
   * As {@link #MOVABLE}, plus a pinned partition: items the pin predicate reports as pinned render
   * before unpinned items regardless of comparator, and drags clamp to their own partition.
   * Pin/Unpin is auto-injected into the context menu when the predicate, the action, and no
   * caller-installed menu are all present. Mutually exclusive with {@link #ANCHORED}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  PINNED,

  /**
   * As {@link #MOVABLE}, plus a single-item anchor: the item the anchor predicate reports as
   * anchored is locked at the leading slot and cannot be dragged; other items reorder freely but
   * cannot drop onto slot 0. Set as anchor / Remove anchor is auto-injected into the context menu
   * under the same three conditions. Mutually exclusive with {@link #PINNED}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  ANCHORED
}
