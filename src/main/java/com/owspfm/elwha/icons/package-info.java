/**
 * The bundled Material Symbols — {@link com.owspfm.elwha.icons.MaterialIcons} wraps {@code
 * FlatSVGIcon} over the 76 bundled SVGs in the house cut (Rounded / weight 400 / fill 0, 20-dp
 * optical-size axis), rendered at 24&nbsp;px by default with sized overloads, and auto-themed
 * through a shared {@code Label.foreground} color filter so every glyph follows the installed
 * {@link com.owspfm.elwha.theme.ElwhaTheme theme} without per-icon wiring. Filled ({@code fill 1})
 * variants exist for selected/active states where the M3 spec calls for them and gracefully fall
 * back to the outline glyph when the bundle ships none.
 *
 * <p>The bundle carries 49 glyphs, addressable by name through {@link
 * com.owspfm.elwha.icons.MaterialIcons#get(String)} (most also have a named factory). The 27
 * shipped in both cuts — outline and {@code fill 1}: {@code anchor}, {@code background_grid_small},
 * {@code brightness_auto}, {@code colorize}, {@code colors}, {@code dark_mode}, {@code delete},
 * {@code edit}, {@code expand_less}, {@code expand_more}, {@code favorite}, {@code gradient},
 * {@code grid_view}, {@code help}, {@code home}, {@code info}, {@code layers}, {@code light_mode},
 * {@code palette}, {@code push_pin}, {@code remove}, {@code rotate_90_degrees_ccw}, {@code
 * rotate_90_degrees_cw}, {@code settings}, {@code star}, {@code visibility}, {@code widgets}.
 *
 * <p>The 22 shipped outline-only: {@code add}, {@code arrow_back}, {@code arrow_drop_down}, {@code
 * arrow_drop_up}, {@code autorenew}, {@code cached}, {@code check}, {@code chevron_right}, {@code
 * close}, {@code deselect}, {@code drag_indicator}, {@code error}, {@code keyboard_tab}, {@code
 * menu}, {@code menu_open}, {@code more_vert}, {@code rotate_left}, {@code rotate_right}, {@code
 * select_all}, {@code start}, {@code table}, {@code tune}.
 *
 * <p>These inventories are pinned by {@code MaterialIconsInventoryDocTest}, which derives the truth
 * from the resource directory and fails the build when this doc drifts — adding a glyph means
 * updating this list in the same change.
 *
 * <p>For the bundled set, use this helper, never raw {@code FlatSVGIcon} — the house icon style and
 * the theming filter are the point. Client code bringing its <em>own</em> SVGs keeps them in its
 * own classpath namespace and wraps them with {@link
 * com.owspfm.elwha.icons.MaterialIcons#themed(com.formdev.flatlaf.extras.FlatSVGIcon)
 * MaterialIcons.themed} to get the same theme treatment — the recipe is in the {@code
 * MaterialIcons} class doc. Attribution for the Apache-2.0 Google assets lives in {@code NOTICE}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha.icons;
