/**
 * Reusable, model-driven list-of-cards primitive built on {@link
 * com.owspfm.elwha.card.v1.ElwhaCard}.
 *
 * <p>The package follows the standard adapter / observable-model pattern (see {@link
 * com.owspfm.elwha.card.v1.list.CardListModel} and {@link
 * com.owspfm.elwha.card.v1.list.CardAdapter}) and adds modern affordances on top — selection,
 * drag-to-reorder, filter, sort, empty/loading states, fade animations, keyboard navigation, and
 * accessibility — so callers stop hand-rolling these for every list-of-cards surface.
 *
 * <p>Like the parent {@code card} package, this package has no dependencies on application code:
 * only FlatLaf and standard Swing. The two directories together can be lifted into their own
 * library by moving them to a fresh Maven module.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
package com.owspfm.elwha.card.v1.list;
