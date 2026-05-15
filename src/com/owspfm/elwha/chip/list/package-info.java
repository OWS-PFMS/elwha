/**
 * Reusable, model-driven list-of-chips primitive built on {@link
 * com.owspfm.elwha.chip.ElwhaChip}.
 *
 * <p>Follows the standard adapter / observable-model pattern (see {@link
 * com.owspfm.elwha.chip.list.ChipListModel} and {@link
 * com.owspfm.elwha.chip.list.ChipAdapter}) and adds modern affordances on top: selection,
 * drag-to-reorder, four orientations (VERTICAL, HORIZONTAL, WRAP, GRID), filter, sort,
 * empty/loading states, keyboard navigation, and accessibility.
 *
 * <p>Like the parent {@code chip} package, this package has no dependencies on application code:
 * only FlatLaf and standard Swing.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
package com.owspfm.elwha.chip.list;
