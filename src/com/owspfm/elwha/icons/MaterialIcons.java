package com.owspfm.elwha.icons;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import javax.swing.UIManager;

/**
 * Lookups for the bundled Material Symbols icon set (Rounded style, weight 400, fill 0, optical
 * size 20px). SVGs are loaded via FlatLaf's {@link FlatSVGIcon} so they HiDPI-render and
 * auto-theme: black strokes in the source SVG become light foreground on dark themes via the
 * built-in color filter.
 *
 * <p>Each lookup returns a fresh instance so per-instance derivations (color filter, resize) don't
 * leak across call sites. Every icon has two overloads: a no-arg form returning the icon at {@link
 * #DEFAULT_SIZE}, and a sized form taking an explicit pixel size, so consumers don't have to chain
 * {@code .derive(size, size)} themselves.
 *
 * <p>Adding new icons: drop the SVG under {@code resources/com/owspfm/icons/material/} (download
 * from <a href="https://fonts.google.com/icons">fonts.google.com/icons</a> with the same style axes
 * — Rounded, 400, fill 0, 20px — to keep visual consistency) and add a lookup here. Material
 * Symbols are licensed Apache-2.0; see the project LICENSE-NOTICES file for attribution.
 *
 * <p><strong>Per-glyph optical centering varies.</strong> Material Symbols are designed for
 * text-baseline alignment in font usage, not optical centering when consumed as standalone
 * round-bbox icons. The {@code favorite} glyph in particular has a slightly low optical bias in
 * its 24×24 viewBox (the heart artwork's bbox center sits a fraction of a pixel below geometric
 * center) — visible as a faint asymmetry inside an {@link
 * com.owspfm.elwha.iconbutton.ElwhaIconButton}. Accepted as an upstream-asset characteristic; not
 * worth a per-icon nudge API on the consuming components.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class MaterialIcons {

  /**
   * Default render size in pixels — M3's standard for icon buttons and toolbar icons. Material
   * Symbols are designed at the 20-dp optical-size axis, so 24px keeps them at their
   * design-intended visual weight. Chip-context callers (ElwhaChipList, ElwhaChipPlayground) pin to
   * a smaller explicit size today; the ElwhaCard V2 / ElwhaChip V2 refresh will revisit chip icon
   * sizing on its own terms.
   */
  public static final int DEFAULT_SIZE = 24;

  private static final String BASE = "com/owspfm/icons/material/";

  /**
   * Theme-aware color filter: every painted color in the SVG is remapped to the current {@code
   * Label.foreground}. Material Symbols are monochrome, so a blanket remap is safe and avoids the
   * per-color "map black to foreground" enumeration. The function runs at paint time, so a runtime
   * LAF switch (light ↔ dark) re-themes the icons on the next repaint with no re-allocation.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final FlatSVGIcon.ColorFilter THEME_FILTER =
      new FlatSVGIcon.ColorFilter(
          color -> {
            final Color fg = UIManager.getColor("Label.foreground");
            return fg != null ? fg : color;
          });

  private MaterialIcons() {
    // utility
  }

  /** Pushpin glyph (outline) — used for pinned-item affordance. */
  public static FlatSVGIcon pushPin() {
    return pushPin(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #pushPin()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon pushPin(final int size) {
    return load("push_pin", size);
  }

  /** Pushpin glyph (filled / solid) — pairs with {@link #pushPin()} as the "active" state. */
  public static FlatSVGIcon pushPinFilled() {
    return pushPinFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #pushPinFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon pushPinFilled(final int size) {
    return load("push_pin_fill", size);
  }

  /** Trash / delete glyph. */
  public static FlatSVGIcon delete() {
    return delete(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #delete()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon delete(final int size) {
    return load("delete", size);
  }

  /** Pencil / edit glyph. */
  public static FlatSVGIcon edit() {
    return edit(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #edit()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon edit(final int size) {
    return load("edit", size);
  }

  /** Info "i" glyph. */
  public static FlatSVGIcon info() {
    return info(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #info()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon info(final int size) {
    return load("info", size);
  }

  /** Heart / favorite glyph (outline). */
  public static FlatSVGIcon favorite() {
    return favorite(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #favorite()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon favorite(final int size) {
    return load("favorite", size);
  }

  /**
   * Heart / favorite glyph (filled / solid) — pairs with {@link #favorite()} as the "active" state.
   */
  public static FlatSVGIcon favoriteFilled() {
    return favoriteFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #favoriteFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon favoriteFilled(final int size) {
    return load("favorite_fill", size);
  }

  /** Five-point star glyph (outline). */
  public static FlatSVGIcon star() {
    return star(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #star()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon star(final int size) {
    return load("star", size);
  }

  /** Five-point star glyph (filled / solid) — pairs with {@link #star()} as the "active" state. */
  public static FlatSVGIcon starFilled() {
    return starFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #starFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon starFilled(final int size) {
    return load("star_fill", size);
  }

  /** Plus / add glyph. */
  public static FlatSVGIcon add() {
    return add(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #add()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon add(final int size) {
    return load("add", size);
  }

  /** Checkmark glyph. */
  public static FlatSVGIcon check() {
    return check(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #check()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon check(final int size) {
    return load("check", size);
  }

  /** Eye / visibility glyph. */
  public static FlatSVGIcon visibility() {
    return visibility(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #visibility()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon visibility(final int size) {
    return load("visibility", size);
  }

  /** Anchor glyph (outline) — used for "fixed/locked to position" affordances. */
  public static FlatSVGIcon anchor() {
    return anchor(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #anchor()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon anchor(final int size) {
    return load("anchor", size);
  }

  /** Anchor glyph (filled / solid) — pairs with {@link #anchor()} as the "active" state. */
  public static FlatSVGIcon anchorFilled() {
    return anchorFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #anchorFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon anchorFilled(final int size) {
    return load("anchor_fill", size);
  }

  /** Grid-view (cards/tiles) glyph. */
  public static FlatSVGIcon gridView() {
    return gridView(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #gridView()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon gridView(final int size) {
    return load("grid_view", size);
  }

  /** Generic table glyph. */
  public static FlatSVGIcon table() {
    return table(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #table()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon table(final int size) {
    return load("table", size);
  }

  /** Fine background-grid glyph — pairs well as a "show grid overlay" toggle. */
  public static FlatSVGIcon backgroundGridSmall() {
    return backgroundGridSmall(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #backgroundGridSmall()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon backgroundGridSmall(final int size) {
    return load("background_grid_small", size);
  }

  /** Select-all glyph. */
  public static FlatSVGIcon selectAll() {
    return selectAll(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #selectAll()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon selectAll(final int size) {
    return load("select_all", size);
  }

  /** Deselect glyph (counterpart to select-all). */
  public static FlatSVGIcon deselect() {
    return deselect(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #deselect()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon deselect(final int size) {
    return load("deselect", size);
  }

  /**
   * Generic lookup for cases where a name is computed at runtime. Throws if the resource is
   * missing.
   *
   * @param name the bare icon name (no path, no extension), e.g. {@code "push_pin"}
   * @return a fresh icon sized to {@link #DEFAULT_SIZE}
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon get(final String name) {
    return get(name, DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #get(String)}.
   *
   * @param name the bare icon name (no path, no extension), e.g. {@code "push_pin"}
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon get(final String name, final int size) {
    return load(name, size);
  }

  private static FlatSVGIcon load(final String name, final int size) {
    final FlatSVGIcon icon = new FlatSVGIcon(BASE + name + ".svg", size, size);
    icon.setColorFilter(THEME_FILTER);
    return icon;
  }
}
