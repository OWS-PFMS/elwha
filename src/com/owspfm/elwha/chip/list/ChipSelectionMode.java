package com.owspfm.elwha.chip.list;

/**
 * Selection semantics for {@link ElwhaChipList}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ChipSelectionMode {

  /** Selection is disabled; chips never enter the selected state via list-driven interaction. */
  NONE,

  /**
   * Zero or one chip selected — click an unselected chip to select it; click the already- selected
   * chip to deselect it. Filter-chip / toggle-style semantics.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SINGLE,

  /**
   * Exactly one chip always selected — click-to-deselect is suppressed; clicking the already-
   * selected chip is a no-op. Tab-strip / segmented-control / radio-group semantics. The list
   * auto-selects the first visible item whenever the mode is entered and no item is currently
   * selected, and re-selects the first remaining item whenever a model change leaves the selection
   * empty.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SINGLE_MANDATORY,

  /** Any number of chips may be selected; supports Shift-click range and Cmd/Ctrl-click toggle. */
  MULTIPLE
}
