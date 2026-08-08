package com.owspfm.elwha.list;

/**
 * Layout orientation shared across the {@code ElwhaList} component family ({@link ElwhaItemList}
 * and future siblings).
 *
 * <p>Hoisted out of the per-family inner enums so that consumers can write code against the
 * abstraction:
 *
 * <pre>{@code
 * void fitOrientation(ElwhaList<?> list, boolean wide) {
 *   list.setOrientation(wide ? ElwhaListOrientation.HORIZONTAL : ElwhaListOrientation.VERTICAL);
 * }
 * }</pre>
 *
 * <p>{@code ElwhaItemList} supports all four. A future sibling need not — implementations may throw
 * {@link UnsupportedOperationException} or fall back to a supported orientation; check the
 * implementation's own documentation.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public enum ElwhaListOrientation {

  /** Single-column vertical stack. */
  VERTICAL,

  /** Single-row horizontal flow with clip / scroll overflow. */
  HORIZONTAL,

  /** Multi-row wrapping flow ({@link java.awt.FlowLayout}-derivative). */
  WRAP,

  /** N-column grid with configurable column count. */
  GRID
}
