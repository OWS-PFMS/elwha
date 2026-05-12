package com.owspfm.ui.components.pill.list;

/**
 * Selection semantics for {@link FlatPillList}.
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
public enum PillSelectionMode {

  /** Selection is disabled; pills never enter the selected state via list-driven interaction. */
  NONE,

  /**
   * Zero or one pill selected — click an unselected pill to select it; click the already- selected
   * pill to deselect it. Filter-chip / toggle-style semantics.
   */
  SINGLE,

  /**
   * Exactly one pill always selected — click-to-deselect is suppressed; clicking the already-
   * selected pill is a no-op. Tab-strip / segmented-control / radio-group semantics. The list
   * auto-selects the first visible item whenever the mode is entered and no item is currently
   * selected, and re-selects the first remaining item whenever a model change leaves the selection
   * empty.
   */
  SINGLE_MANDATORY,

  /** Any number of pills may be selected; supports Shift-click range and Cmd/Ctrl-click toggle. */
  MULTIPLE
}
