package com.owspfm.elwha.iconbutton;

/**
 * The five Material 3 icon-button size presets, encoded as {@code (containerPx, iconPx)} pairs.
 *
 * <p>Each constant pins the container side length and the rendered icon size to the M3-spec values
 * for that size tier. Selecting a size via {@link ElwhaIconButton#setButtonSize(IconButtonSize)}
 * sets both dimensions in one call — the primary API. The lower-level {@link
 * ElwhaIconButton#setContainerSize(int)} / {@link ElwhaIconButton#setIconSize(int)} setters stay
 * public for off-spec custom sizes; both setters last-write-wins.
 *
 * <p><strong>Size and shape are independent.</strong> Selecting a size does <em>not</em> change the
 * corner radius — the {@link com.owspfm.elwha.theme.ShapeScale} default ({@link
 * com.owspfm.elwha.theme.ShapeScale#FULL}, the M3 capsule) is preserved across sizes. Square
 * treatments at any size are reached by also calling {@link
 * ElwhaIconButton#setShape(com.owspfm.elwha.theme.ShapeScale)}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum IconButtonSize {

  /**
   * Extra-small — 24 px container, 16 px icon. For tight rows, table cells, and any context where
   * even the {@link #S} size is too prominent.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XS(24, 16),

  /**
   * Small — 32 px container, 20 px icon. The M3 toolbar-standard size; the most common Elwha choice
   * for a {@link javax.swing.JToolBar}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  S(32, 20),

  /**
   * Medium — 40 px container, 24 px icon. The {@link ElwhaIconButton} default and the M3 spec
   * default. Standalone affordances; first choice when no other constraint applies.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  M(40, 24),

  /**
   * Large — 48 px container, 28 px icon. Primary actions and prominent affordances in spacious
   * layouts.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  L(48, 28),

  /**
   * Extra-large — 56 px container, 32 px icon. Hero affordances; sized close to an M3 FAB without
   * actually being one.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  XL(56, 32);

  private final int containerPx;
  private final int iconPx;

  IconButtonSize(int containerPx, int iconPx) {
    this.containerPx = containerPx;
    this.iconPx = iconPx;
  }

  /**
   * Returns the container side length in pixels for this size.
   *
   * @return the container side length
   * @version v0.1.0
   * @since v0.1.0
   */
  public int containerPx() {
    return containerPx;
  }

  /**
   * Returns the rendered icon size in pixels for this size.
   *
   * @return the icon size
   * @version v0.1.0
   * @since v0.1.0
   */
  public int iconPx() {
    return iconPx;
  }
}
