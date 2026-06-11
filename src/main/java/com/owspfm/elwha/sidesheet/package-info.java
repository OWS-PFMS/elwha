/**
 * The M3 <strong>side sheet</strong> family (epic #308): {@link
 * com.owspfm.elwha.sidesheet.ElwhaSideSheet} — one surface component with two presentation paths —
 * plus its {@link com.owspfm.elwha.sidesheet.SheetType type} / {@link
 * com.owspfm.elwha.sidesheet.SheetEdge edge} axes. A {@code STANDARD} sheet embeds in the
 * consumer's layout and coexists with the main UI (content reflows — the M3 coplanar squash falls
 * out of Swing embedding); a {@code MODAL} sheet presents over a scrim on the host frame's layered
 * pane at {@code ElwhaLayers.OVERLAY_LAYER}, below dialogs and menus. Spec capture: {@code
 * docs/research/elwha-side-sheet-research.md}; decisions: {@code elwha-side-sheet-design.md}.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
package com.owspfm.elwha.sidesheet;
