/**
 * Elwha — a Swing component library implementing Material 3 Expressive as a design system for
 * desktop Java, built on FlatLaf. A design-token foundation plus the component catalog and the
 * containers, overlays, and anchors around it; runtime dependencies are Swing + FlatLaf only.
 *
 * <p><strong>Install the theme first.</strong> Every component resolves its color, shape, spacing,
 * and type tokens at paint time, so {@link com.owspfm.elwha.theme.ElwhaTheme#install
 * ElwhaTheme.install} runs once before any UI is built — and a later theme or light/dark switch
 * re-skins the running UI live. The token vocabulary, the bundled palettes, and the install API
 * live in {@link com.owspfm.elwha.theme}.
 *
 * <p>The catalog groups into the same families the M3 spec uses:
 *
 * <ul>
 *   <li><strong>Foundation</strong> — {@link com.owspfm.elwha.theme} (tokens, palettes, install,
 *       shared paint/motion helpers), {@link com.owspfm.elwha.surface} (the token-resolved painted
 *       panel most components extend), {@link com.owspfm.elwha.icons} (the bundled Material
 *       Symbols, auto-themed)
 *   <li><strong>Actions</strong> — {@link com.owspfm.elwha.button}, {@link
 *       com.owspfm.elwha.iconbutton}, {@link com.owspfm.elwha.buttongroup}, {@link
 *       com.owspfm.elwha.fab} (standard + extended, with the scroll-aware anchor)
 *   <li><strong>Selection controls</strong> — {@link com.owspfm.elwha.checkbox}, {@link
 *       com.owspfm.elwha.radio}, {@link com.owspfm.elwha.switches}, {@link com.owspfm.elwha.chip}
 *   <li><strong>Fields</strong> — {@link com.owspfm.elwha.textfield}, {@link
 *       com.owspfm.elwha.selectfield}, {@link com.owspfm.elwha.slider}, {@link
 *       com.owspfm.elwha.colorpicker}
 *   <li><strong>Containers &amp; surfaces</strong> — {@link com.owspfm.elwha.card} (the V3
 *       chrome-plus-composition card), {@link com.owspfm.elwha.list} (the unified item list),
 *       {@link com.owspfm.elwha.sidesheet}
 *   <li><strong>Overlays</strong> — {@link com.owspfm.elwha.overlay} (the shared layered-pane
 *       host), {@link com.owspfm.elwha.dialog}, {@link com.owspfm.elwha.menu}, {@link
 *       com.owspfm.elwha.tooltip}
 *   <li><strong>Navigation</strong> — {@link com.owspfm.elwha.appbar}, {@link
 *       com.owspfm.elwha.navrail}, {@link com.owspfm.elwha.tabs}
 *   <li><strong>Feedback</strong> — {@link com.owspfm.elwha.badge}, {@link
 *       com.owspfm.elwha.progress}, {@link com.owspfm.elwha.loading}
 * </ul>
 *
 * <p>The Elwha Showcase ({@link com.owspfm.elwha.showcase}) is the visual storefront — one leaf per
 * component — and most component packages also carry {@code *Demo} / {@code *Smoke} mains and a
 * {@code playground} subpackage; those are development artifacts, not part of the supported API.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha;
