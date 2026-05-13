/**
 * Reusable, model-driven list-of-cards primitive built on {@link
 * com.owspfm.ui.components.card.FlatCard}.
 *
 * <p>The package follows the standard adapter / observable-model pattern (see {@link
 * com.owspfm.ui.components.card.list.CardListModel} and {@link
 * com.owspfm.ui.components.card.list.CardAdapter}) and adds modern affordances on top — selection,
 * drag-to-reorder, filter, sort, empty/loading states, fade animations, keyboard navigation, and
 * accessibility — so callers stop hand-rolling these for every list-of-cards surface.
 *
 * <p>Like the parent {@code card} package, this package has no dependencies on application code:
 * only FlatLaf and standard Swing. The two directories together can be lifted into their own
 * library by moving them to a fresh Maven module.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
package com.owspfm.ui.components.card.list;
