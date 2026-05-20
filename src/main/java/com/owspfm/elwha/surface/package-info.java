/**
 * Token-native rounded surface primitive ({@link com.owspfm.elwha.surface.ElwhaSurface}) — the
 * round-rect {@code JPanel} the rest of the component set composes for its background paint. Lives
 * here so {@code ElwhaCard} V2 and any other surface-bearing component can delegate the round-rect
 * geometry instead of duplicating it.
 *
 * <p>Design and decisions: {@code docs/research/elwha-surface-design.md}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
package com.owspfm.elwha.surface;
