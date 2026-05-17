package com.owspfm.elwha.card.list;

/**
 * The four selection modes supported by {@link ElwhaCardList}'s V3 selection model. Mirrors the
 * {@code ChipSelectionMode} pattern from the chip side; the {@link #SINGLE_MANDATORY} mode
 * (always-one selected) is shared between the two families.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum CardSelectionMode {
  /** No selection — cards are static, clicks do not toggle. */
  NONE,

  /** Single selection — clicking a selected card deselects it. Zero or one card selected. */
  SINGLE,

  /**
   * Single selection enforced — clicking the active card is a no-op; selection cannot be cleared
   * once made. Use for filter-strip-style lists that always need an active item.
   */
  SINGLE_MANDATORY,

  /** Multiple selection — clicks toggle individual cards independently. */
  MULTIPLE
}
