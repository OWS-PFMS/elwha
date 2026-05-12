/**
 * Shared abstractions for the {@code FlatList} component family — the umbrella over {@link
 * com.owspfm.ui.components.card.list.FlatCardList}, {@link
 * com.owspfm.ui.components.pill.list.FlatPillList}, and any future model-driven flat-list
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
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
package com.owspfm.ui.components.flatlist;
