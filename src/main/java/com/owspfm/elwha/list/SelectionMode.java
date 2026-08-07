package com.owspfm.elwha.list;

/**
 * Selection semantics for {@link ElwhaItemList}.
 *
 * <p>Hoisted to the top level of the unified list family in story #69 — the shipped card and chip
 * lists each carried their own nested copy. The constants and their semantics are the chip
 * family's, which were the strict superset of the two (spec §4).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public enum SelectionMode {

  /**
   * Selection is disabled. Items never enter the selected state through list-driven interaction;
   * clicks still reach the hosted component and fire whatever action it carries.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  NONE,

  /**
   * Zero or one item selected, toggleable — clicking an unselected item selects it, clicking the
   * already-selected item deselects it. Filter-chip semantics.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  SINGLE,

  /**
   * Exactly one item always selected — clicking the already-selected item is a no-op, so the
   * selection is never empty. Tab-strip / segmented-control / radio-group semantics. The list
   * auto-seeds the first visible item whenever the mode is entered with an empty selection, and
   * re-seeds after any model, filter, or sort change that would otherwise leave it empty.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  SINGLE_MANDATORY,

  /**
   * Any number of items selected. A plain click collapses the selection to the clicked item;
   * Cmd/Ctrl-click toggles one item; Shift-click extends a range over visible order; Cmd/Ctrl-A
   * selects every visible item.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  MULTIPLE
}
