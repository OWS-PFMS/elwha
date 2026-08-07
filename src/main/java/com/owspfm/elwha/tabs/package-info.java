/**
 * The M3 tab bar (epic #425) — {@link com.owspfm.elwha.tabs.ElwhaTabs} hosting a row of {@link
 * com.owspfm.elwha.tabs.ElwhaTab}s with the sliding active indicator, in both {@link
 * com.owspfm.elwha.tabs.TabsVariant} treatments (primary / secondary) and both {@link
 * com.owspfm.elwha.tabs.TabMode} layouts (fixed / scrollable). Tabs carry optional leading icons
 * and attach {@link com.owspfm.elwha.badge.ElwhaBadge badges} through {@link
 * com.owspfm.elwha.badge.ElwhaBadgeAnchor} (including the label-trailing "Label 999+" placement).
 * The indicator paints in a {@code paintChildren} overlay so the slide never fights the tab
 * children; motion runs on the shared {@link com.owspfm.elwha.theme.MorphAnimator} clock and snaps
 * under reduced motion.
 *
 * <p>Design + token bindings: {@code docs/research/elwha-tabs-design.md}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha.tabs;
