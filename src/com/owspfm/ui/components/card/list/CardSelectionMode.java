package com.owspfm.ui.components.card.list;

/**
 * Selection semantics for {@link FlatCardList}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum CardSelectionMode {

  /** Selection is disabled; cards never enter the selected state. */
  NONE,

  /** At most one card may be selected at a time; selecting another deselects the previous. */
  SINGLE,

  /** Any number of cards may be selected; supports Shift-click range and Cmd/Ctrl-click toggle. */
  MULTIPLE
}
