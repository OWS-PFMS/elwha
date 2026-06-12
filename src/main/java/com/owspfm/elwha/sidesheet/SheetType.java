package com.owspfm.elwha.sidesheet;

/**
 * The two M3 side sheet types — the spec's <em>standard</em> / <em>modal</em> axis. The type
 * derives the sheet's chrome (container color, corner shape, edge divider) and matches its
 * presentation path: a {@link #STANDARD} sheet is embedded in the consumer's layout and coexists
 * with the main UI; a {@link #MODAL} sheet is presented over a scrim via {@link
 * ElwhaSideSheet#showModal(java.awt.Component)}. Token capture: {@code
 * docs/research/elwha-side-sheet-research.md} §B; decisions: {@code elwha-side-sheet-design.md}
 * §3/§6.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum SheetType {

  /**
   * The standard (docked) side sheet — coexists with the main UI on the same plane. {@code SURFACE}
   * container, square corners, an optional 1px {@code OUTLINE_VARIANT} divider on the
   * content-facing edge.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  STANDARD,

  /**
   * The modal side sheet — blocks the UI behind a scrim. {@code SURFACE_CONTAINER_LOW} container,
   * {@code ShapeScale.LG} (16px) rounding on the two content-facing corners (square on the
   * window-edge corners), no edge divider, no drop shadow (the spec renders show the sheet flat
   * over its scrim).
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  MODAL
}
