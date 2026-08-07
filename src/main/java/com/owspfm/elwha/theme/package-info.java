/**
 * The design-token foundation every Elwha component paints from (epic #30) — install a theme once,
 * and every component resolves its colors, shapes, spacing, and type at paint time, so a runtime
 * theme or light/dark switch re-skins the whole UI live with no per-component listeners.
 *
 * <p><strong>Install (the one required call):</strong>
 *
 * <pre>{@code
 * ElwhaTheme.install(
 *     ElwhaTheme.config()
 *         .theme(MaterialPalettes.baseline())
 *         .mode(Mode.SYSTEM)
 *         .build());
 * }</pre>
 *
 * <p><strong>The facade enums</strong> are the vocabulary components and consumers share: {@link
 * com.owspfm.elwha.theme.ColorRole} (the M3 color system, resolved per mode), {@link
 * com.owspfm.elwha.theme.ShapeScale} (corner-radius steps), {@link
 * com.owspfm.elwha.theme.SpaceScale} (spacing steps), {@link com.owspfm.elwha.theme.TypeRole} (the
 * full M3 15-role type scale over the bundled Inter), and {@link com.owspfm.elwha.theme.StateLayer}
 * (hover/focus/pressed/dragged opacities).
 *
 * <p><strong>Palettes:</strong> {@link com.owspfm.elwha.theme.MaterialPalettes} bundles the M3
 * baseline scheme plus two directory-discovered demo tiers; {@link
 * com.owspfm.elwha.theme.PaletteLoader} loads consumer-supplied Elwha-format palette JSON. {@link
 * com.owspfm.elwha.theme.FlatLafKeyMapping} bridges the curated FlatLaf-native UIManager keys to
 * roles so stock Swing chrome follows the theme too.
 *
 * <p><strong>Shared paint/motion machinery</strong> components compose rather than reimplement:
 * {@link com.owspfm.elwha.theme.RipplePainter}, {@link com.owspfm.elwha.theme.ShapeMorphPainter},
 * {@link com.owspfm.elwha.theme.ContentMorphPainter}, {@link com.owspfm.elwha.theme.ShadowPainter}
 * with the {@link com.owspfm.elwha.theme.ShadowBearing} reserve contract, {@link
 * com.owspfm.elwha.theme.MorphAnimator} (the shared motion clock, including the global {@link
 * com.owspfm.elwha.theme.MorphAnimator#setReducedMotion(boolean) reduced-motion} switch), and
 * {@link com.owspfm.elwha.theme.Easing}.
 *
 * <p>Token taxonomy and binding rules: {@code docs/research/elwha-token-taxonomy.md}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha.theme;
