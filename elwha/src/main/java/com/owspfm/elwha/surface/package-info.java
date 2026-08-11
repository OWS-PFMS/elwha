/**
 * Token-native rounded surface primitive ({@link com.owspfm.elwha.surface.ElwhaSurface}) — the
 * round-rect {@code JPanel} the rest of the component set composes for its background paint. Lives
 * here so {@code ElwhaCard} and any other surface-bearing component can delegate the round-rect
 * geometry instead of duplicating it.
 *
 * <p>Design: {@code docs/research/elwha-surface-design.md}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
package com.owspfm.elwha.surface;
