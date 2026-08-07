/**
 * The Elwha list family — {@link com.owspfm.elwha.list.ElwhaItemList}, its model and selection
 * layers, and the {@link com.owspfm.elwha.list.ElwhaList} contract every list component honors.
 *
 * <p>{@code ElwhaItemList} is the one concrete list: a generic, model-driven container that renders
 * any domain item type through an {@link com.owspfm.elwha.list.ElwhaItemAdapter}, with selection,
 * drag-reorder, pin and anchor partitions, four orientations, right-to-left mirroring, and
 * accessibility built in. It arrived in epic #67 story #69 as the single successor to the parallel
 * card-list and chip-list families, whose surfaces it supersedes.
 *
 * <p>Components hosted inside a list may implement {@link com.owspfm.elwha.list.ElwhaListItemView}
 * to receive selection state and drag chrome; {@code ElwhaCard} and {@code ElwhaChip} both do.
 * Components that do not implement it still render and lay out normally.
 *
 * <p>{@link com.owspfm.elwha.list.ElwhaList} predates the unification — extracted in epic #230
 * story #237 — and stays as the narrow cross-cutting contract (orientation, item gap, padding,
 * empty and loading state, filter, sort) that any future list sibling is expected to expose.
 *
 * <p>This package is free of OWS-specific imports and depends only on standard Swing and the Elwha
 * theme foundation.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
package com.owspfm.elwha.list;
