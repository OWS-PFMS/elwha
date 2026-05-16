package com.owspfm.elwha.list;

/**
 * Selection semantics for an {@link ElwhaItemList}. Unified superset of the prior {@code
 * CardSelectionMode} (NONE / SINGLE / MULTIPLE) and {@code ChipSelectionMode} (NONE / SINGLE /
 * SINGLE_MANDATORY / MULTIPLE); the chip side's superset wins per epic #67. {@code MULTIPLE} is
 * renamed {@link #MULTI} to align with the locked vocabulary.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum SelectionMode {

  /** Selection is disabled; items never enter the selected state via list-driven interaction. */
  NONE,

  /**
   * Zero or one item selected — click an unselected item to select it; click the already-selected
   * item to deselect it. Filter-chip / toggle-style semantics.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SINGLE,

  /**
   * Exactly one item always selected — click-to-deselect is suppressed; clicking the already-
   * selected item is a no-op. Tab-strip / segmented-control / radio-group semantics. The list
   * auto-selects the first visible item whenever the mode is entered and no item is currently
   * selected; re-selects the first remaining item whenever a model change leaves the selection
   * empty. Inherited from Chip's superset.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SINGLE_MANDATORY,

  /**
   * Any number of items may be selected; Shift-click extends a range and Cmd/Ctrl-click toggles an
   * individual item's membership.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  MULTI
}
