/**
 * The Elwha list family — {@link com.owspfm.elwha.list.ElwhaItemList}, its model and selection
 * layers, and the {@link com.owspfm.elwha.list.ElwhaList} contract every list component honors.
 *
 * <p>{@code ElwhaItemList} is the one concrete list: a generic, model-driven container that renders
 * any domain item type through an {@link com.owspfm.elwha.list.ElwhaItemAdapter}, with selection,
 * drag-reorder, pin and anchor partitions, four orientations, right-to-left mirroring, and
 * accessibility built in. It is the single successor to the parallel card-list and chip-list
 * families, whose surfaces it supersedes.
 *
 * <p>Components hosted inside a list may implement {@link com.owspfm.elwha.list.ElwhaListItemView}
 * to receive selection state and drag chrome; {@code ElwhaCard} and {@code ElwhaChip} both do.
 * Components that do not implement it still render and lay out normally.
 *
 * <p>{@link com.owspfm.elwha.list.ElwhaList} predates the unification and stays as the narrow
 * cross-cutting contract (orientation, item gap, padding, empty and loading state, filter, sort)
 * that any future list sibling is expected to expose.
 *
 * <p>{@link com.owspfm.elwha.list.ElwhaCursors} is the package's one export that has nothing to do
 * with lists: the open-hand and closed-fist drag pointers AWT does not ship, published so a
 * consumer's own draggable surfaces read the way an Elwha list's do. It lives here because this is
 * where the artwork and its loader already lived.
 *
 * <p>This package is free of OWS-specific imports and depends only on standard Swing and the Elwha
 * theme foundation.
 *
 * @author Charles Bryan
 * @version v1.1.2
 * @since v0.1.0
 */
package com.owspfm.elwha.list;
