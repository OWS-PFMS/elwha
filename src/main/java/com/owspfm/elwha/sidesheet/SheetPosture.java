package com.owspfm.elwha.sidesheet;

/**
 * Whether a side sheet sits flush against the window edge or floats inset from it — the M3
 * <em>docked</em> / <em>detached</em> axis, orthogonal to {@link SheetType}. {@link #DOCKED} is the
 * default and the only V1 posture; {@link #DETACHED} is the V2 floating posture. The posture drives
 * the sheet's silhouette (outer margin, corner rounding, edge-divider presence) — not its container
 * color or its presentation path. Token capture: {@code docs/research/elwha-side-sheet-research.md}
 * §A/§B; decisions: {@code elwha-side-sheet-v2-design.md} §0/§2.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public enum SheetPosture {

  /**
   * The docked posture — the sheet sits flush against the resolved window edge. A standard sheet
   * has square corners and a content-facing edge divider; a modal sheet rounds only its
   * content-facing corners. The M3 default and the whole of V1.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  DOCKED,

  /**
   * The detached posture — the sheet floats inset by a 16dp ({@code SpaceScale.LG}) margin on all
   * four sides, with {@code ShapeScale.LG} (16px) rounding on all four corners and no edge divider
   * (the rounded float reads as a panel without one). Applies to both {@link SheetType#STANDARD}
   * and {@link SheetType#MODAL}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  DETACHED
}
