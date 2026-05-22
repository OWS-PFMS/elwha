/**
 * The Elwha button-group family — {@link com.owspfm.elwha.buttongroup.ElwhaButtonGroup}, the M3
 * Expressive <em>Button group</em>: an invisible visual + layout + selection container that
 * composes {@link com.owspfm.elwha.button.ElwhaButton} and {@link
 * com.owspfm.elwha.iconbutton.ElwhaIconButton} segments.
 *
 * <p>Two variants ({@link com.owspfm.elwha.buttongroup.ButtonGroupVariant}): {@code STANDARD} — a
 * gapped toolbar cluster — and {@code CONNECTED} — butted segments sharing edges, the selection
 * control that replaces M3's deprecated Segmented Button. Both carry the three M3 selection modes
 * ({@link com.owspfm.elwha.buttongroup.SelectionMode}: {@code SINGLE} / {@code MULTI} / {@code
 * REQUIRED}) and the static selected-shape inversion.
 *
 * <p>Not to be confused with {@link com.owspfm.elwha.button.ButtonGroup} / {@link
 * com.owspfm.elwha.iconbutton.IconButtonGroup} — those are pure selection-mutex helpers (the {@link
 * javax.swing.ButtonGroup} analog) with no layout or paint. {@code ElwhaButtonGroup} is the visual
 * container.
 *
 * <p>Design and the M3 capture: {@code docs/research/elwha-button-group-design.md}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
package com.owspfm.elwha.buttongroup;
