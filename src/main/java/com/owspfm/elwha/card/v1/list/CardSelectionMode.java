package com.owspfm.elwha.card.v1.list;

/**
 * Selection semantics for {@link ElwhaCardList}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum CardSelectionMode {

  /** Selection is disabled; cards never enter the selected state. */
  NONE,

  /** At most one card may be selected at a time; selecting another deselects the previous. */
  SINGLE,

  /** Any number of cards may be selected; supports Shift-click range and Cmd/Ctrl-click toggle. */
  MULTIPLE
}
