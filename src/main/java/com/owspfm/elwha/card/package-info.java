/**
 * ElwhaCard V3 — chrome + composition primitives. The chrome ({@link
 * com.owspfm.elwha.card.ElwhaCard}) extends {@link com.owspfm.elwha.surface.ElwhaSurface} and adds
 * the card-specific behavior axes (variant, elevation, actionability, selection, collapse,
 * orientation). It owns no typed content slots; content is supplied by composing companion
 * primitives from this package and adding them via {@code card.add(...)}.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} for the full implementation contract and
 * {@code docs/research/elwha-card-v3-sketch.md} for the architectural narrative.
 *
 * <p>V1 (the pre-Elwha-theme card extracted from OWS) lives at {@link com.owspfm.elwha.card.v1}
 * through 0.2.0 to support OWS incremental migration; it is deleted in 1.0.0.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
package com.owspfm.elwha.card;
