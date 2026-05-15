/**
 * Shared abstractions for the {@code ElwhaList} component family — the umbrella over {@link
 * com.owspfm.elwha.card.list.ElwhaCardList}, {@link
 * com.owspfm.elwha.chip.list.ElwhaChipList}, and any future model-driven flat-list
 * primitives.
 *
 * <p>Extracted in epic #230 story #237. The package defines just the cross-cutting
 * <em>contract</em> that every family member is expected to expose (orientation, item gap, padding,
 * empty/loading state, filter, sort). Family-specific concerns — selection-model types,
 * adapter-types, the per-item rendered component class — stay on the concrete classes since their
 * signatures vary across families.
 *
 * <p>This package is free of OWS-specific imports and depends only on standard Swing.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
package com.owspfm.elwha.list;
