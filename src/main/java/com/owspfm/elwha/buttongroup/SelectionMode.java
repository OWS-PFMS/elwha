package com.owspfm.elwha.buttongroup;

/**
 * The three M3 selection modes an {@link ElwhaButtonGroup} can enforce across its segments.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public enum SelectionMode {

  /**
   * At most one segment selected — clicking the selected segment deselects it, leaving the group
   * with no selection. The M3 single-select toggle.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  SINGLE,

  /**
   * Any number of segments selected independently — each segment toggles on its own. The M3
   * multi-select pattern.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  MULTI,

  /**
   * Exactly one segment selected at all times — clicking the selected segment is a no-op (the
   * deselect is refused). The M3 selection-required mode; equivalent to the chip-list {@code
   * SINGLE_MANDATORY} and a mandatory {@link com.owspfm.elwha.button.ButtonGroup}.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  REQUIRED
}
