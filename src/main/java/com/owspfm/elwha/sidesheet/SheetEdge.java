package com.owspfm.elwha.sidesheet;

/**
 * Which side of the window a side sheet anchors to, in logical (orientation-aware) terms — MDC's
 * {@code layout_gravity} start/end axis. {@link #TRAILING} is the M3 default. The edge is resolved
 * against the sheet's {@link java.awt.ComponentOrientation} at paint/layout time: {@code TRAILING}
 * is the right side in LTR and the left side in RTL. The resolved edge drives the modal corner
 * asymmetry (the content-facing corners round), the standard sheet's edge-divider side, the modal
 * slide direction, and the modal host's docking side ({@code elwha-side-sheet-design.md} §11).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public enum SheetEdge {

  /**
   * The leading window edge — left in LTR, right in RTL.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  LEADING,

  /**
   * The trailing window edge — right in LTR, left in RTL. The M3 default (MDC {@code
   * layout_gravity=end}).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  TRAILING
}
