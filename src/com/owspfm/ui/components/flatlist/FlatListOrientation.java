package com.owspfm.ui.components.flatlist;

/**
 * Layout orientation shared across the {@code FlatList} component family ({@code FlatCardList},
 * {@code FlatPillList}, and future siblings).
 *
 * <p>Hoisted out of the per-family inner enums in story #237 so that consumers can write code
 * against the abstraction:
 *
 * <pre>{@code
 * void fitOrientation(FlatList<?> list, boolean wide) {
 *   list.setOrientation(wide ? FlatListOrientation.HORIZONTAL : FlatListOrientation.VERTICAL);
 * }
 * }</pre>
 *
 * <p>Not every implementation supports every orientation at every release — for instance, {@code
 * FlatCardList} ships VERTICAL and GRID only (HORIZONTAL and WRAP land in #242), while {@code
 * FlatPillList} adds the wider set in #238. Implementations may throw {@link
 * UnsupportedOperationException} or fall back to a supported orientation; check the
 * implementation's own documentation.
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
public enum FlatListOrientation {

  /** Single-column vertical stack. */
  VERTICAL,

  /** Single-row horizontal flow with clip / scroll overflow. */
  HORIZONTAL,

  /** Multi-row wrapping flow ({@link java.awt.FlowLayout}-derivative). */
  WRAP,

  /** N-column grid with configurable column count. */
  GRID
}
