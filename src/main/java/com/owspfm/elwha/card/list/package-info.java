/**
 * V3 ElwhaCardList — the list family consuming V3 {@link com.owspfm.elwha.card.ElwhaCard}. Owns the
 * M3-compliant selection model ({@link com.owspfm.elwha.card.list.CardSelectionModel}) and the
 * single-pointer alternatives required for accessible drag-reorder per the M3 doctrine cited in
 * {@code docs/research/elwha-card-v3-spec.md} §16.4: keyboard reorder (Cmd+↑ / Cmd+↓ / Delete) and
 * a right-click context menu (Move up / Move down / Delete).
 *
 * <p>V1 ElwhaCardList lives at {@link com.owspfm.elwha.card.v1.list} through 0.2.0 for OWS
 * incremental migration; deleted in 1.0.0.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
package com.owspfm.elwha.card.list;
