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
 * round-bbox icons. The {@code favorite} glyph in particular has a slightly low optical bias in its
 * 24×24 viewBox (the heart artwork's bbox center sits a fraction of a pixel below geometric center)
 * — visible as a faint asymmetry inside an {@link com.owspfm.elwha.iconbutton.ElwhaIconButton}.
 * Accepted as an upstream-asset characteristic; not worth a per-icon nudge API on the consuming
 * components.
 *
 * @author Charles Bryan
 * @version v0.3.0
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

  /** Trash / delete glyph (outline). */
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

  /** Trash / delete glyph (filled / solid) — pairs with {@link #delete()} as the "active" state. */
  public static FlatSVGIcon deleteFilled() {
    return deleteFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #deleteFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon deleteFilled(final int size) {
    return load("delete_fill", size);
  }

  /** Pencil / edit glyph (outline). */
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

  /** Pencil / edit glyph (filled / solid) — pairs with {@link #edit()} as the "active" state. */
  public static FlatSVGIcon editFilled() {
    return editFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #editFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon editFilled(final int size) {
    return load("edit_fill", size);
  }

  /** Info "i" glyph (outline). */
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

  /** Info "i" glyph (filled / solid) — pairs with {@link #info()} as the "active" state. */
  public static FlatSVGIcon infoFilled() {
    return infoFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #infoFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon infoFilled(final int size) {
    return load("info_fill", size);
  }

  /** Help "?" glyph (outline). */
  public static FlatSVGIcon help() {
    return help(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #help()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon help(final int size) {
    return load("help", size);
  }

  /** Help "?" glyph (filled / solid) — pairs with {@link #help()} as the "active" state. */
  public static FlatSVGIcon helpFilled() {
    return helpFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #helpFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon helpFilled(final int size) {
    return load("help_fill", size);
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

  /** Minus / remove glyph (horizontal bar). */
  public static FlatSVGIcon remove() {
    return remove(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #remove()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon remove(final int size) {
    return load("remove", size);
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

  /** Eye / visibility glyph (outline). */
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

  /**
   * Eye / visibility glyph (filled / solid) — pairs with {@link #visibility()} as the "active"
   * state.
   */
  public static FlatSVGIcon visibilityFilled() {
    return visibilityFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #visibilityFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static FlatSVGIcon visibilityFilled(final int size) {
    return load("visibility_fill", size);
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

  /** Downward chevron — disclosure / dropdown / expand indicator. */
  public static FlatSVGIcon expandMore() {
    return expandMore(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #expandMore()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon expandMore(final int size) {
    return load("expand_more", size);
  }

  /** Upward chevron — collapse indicator (pairs with {@link #expandMore()}). */
  public static FlatSVGIcon expandLess() {
    return expandLess(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #expandLess()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon expandLess(final int size) {
    return load("expand_less", size);
  }

  /**
   * Vertical 3-dot overflow glyph — M3 standard for "more actions" affordance in headers, app bars,
   * list items, and card trailing-actions slots.
   */
  public static FlatSVGIcon moreVert() {
    return moreVert(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #moreVert()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon moreVert(final int size) {
    return load("more_vert", size);
  }

  /** Rotate 90° counter-clockwise glyph. */
  public static FlatSVGIcon rotate90DegreesCcw() {
    return rotate90DegreesCcw(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #rotate90DegreesCcw()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon rotate90DegreesCcw(final int size) {
    return load("rotate_90_degrees_ccw", size);
  }

  /** Rotate 90° clockwise glyph — pairs with {@link #rotate90DegreesCcw()}. */
  public static FlatSVGIcon rotate90DegreesCw() {
    return rotate90DegreesCw(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #rotate90DegreesCw()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon rotate90DegreesCw(final int size) {
    return load("rotate_90_degrees_cw", size);
  }

  /** Rotate-left glyph (curved arrow). */
  public static FlatSVGIcon rotateLeft() {
    return rotateLeft(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #rotateLeft()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon rotateLeft(final int size) {
    return load("rotate_left", size);
  }

  /** Rotate-right glyph (curved arrow) — pairs with {@link #rotateLeft()}. */
  public static FlatSVGIcon rotateRight() {
    return rotateRight(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #rotateRight()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon rotateRight(final int size) {
    return load("rotate_right", size);
  }

  /** Auto-renew glyph (two-arrow loop) — refresh / sync affordance. */
  public static FlatSVGIcon autorenew() {
    return autorenew(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #autorenew()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon autorenew(final int size) {
    return load("autorenew", size);
  }

  /**
   * Cached glyph (single-arrow loop) — alternate refresh styling, pairs with {@link #autorenew()}.
   */
  public static FlatSVGIcon cached() {
    return cached(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #cached()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon cached(final int size) {
    return load("cached", size);
  }

  /** Start / play glyph. */
  public static FlatSVGIcon start() {
    return start(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #start()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon start(final int size) {
    return load("start", size);
  }

  /** Keyboard-tab glyph — tab-advance affordance, pairs with {@link #start()}. */
  public static FlatSVGIcon keyboardTab() {
    return keyboardTab(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #keyboardTab()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.2.0
   * @since v0.2.0
   */
  public static FlatSVGIcon keyboardTab(final int size) {
    return load("keyboard_tab", size);
  }

  /** Light-mode glyph (outline) — a sun; the "light theme" affordance. */
  public static FlatSVGIcon lightMode() {
    return lightMode(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #lightMode()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon lightMode(final int size) {
    return load("light_mode", size);
  }

  /** Light-mode glyph (filled / solid) — pairs with {@link #lightMode()} as the "active" state. */
  public static FlatSVGIcon lightModeFilled() {
    return lightModeFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #lightModeFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon lightModeFilled(final int size) {
    return load("light_mode_fill", size);
  }

  /** Dark-mode glyph (outline) — a crescent moon; the "dark theme" affordance. */
  public static FlatSVGIcon darkMode() {
    return darkMode(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #darkMode()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon darkMode(final int size) {
    return load("dark_mode", size);
  }

  /** Dark-mode glyph (filled / solid) — pairs with {@link #darkMode()} as the "active" state. */
  public static FlatSVGIcon darkModeFilled() {
    return darkModeFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #darkModeFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon darkModeFilled(final int size) {
    return load("dark_mode_fill", size);
  }

  /**
   * Brightness-auto glyph (outline) — an "A" inside a sun; the "follow the system theme"
   * affordance.
   */
  public static FlatSVGIcon brightnessAuto() {
    return brightnessAuto(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #brightnessAuto()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon brightnessAuto(final int size) {
    return load("brightness_auto", size);
  }

  /**
   * Brightness-auto glyph (filled / solid) — pairs with {@link #brightnessAuto()} as the "active"
   * state.
   */
  public static FlatSVGIcon brightnessAutoFilled() {
    return brightnessAutoFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #brightnessAutoFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon brightnessAutoFilled(final int size) {
    return load("brightness_auto_fill", size);
  }

  /** Artist-palette glyph (outline) — a colour / theme affordance. */
  public static FlatSVGIcon palette() {
    return palette(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #palette()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon palette(final int size) {
    return load("palette", size);
  }

  /**
   * Artist-palette glyph (filled / solid) — pairs with {@link #palette()} as the "active" state.
   */
  public static FlatSVGIcon paletteFilled() {
    return paletteFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #paletteFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon paletteFilled(final int size) {
    return load("palette_fill", size);
  }

  /** Eyedropper / colorize glyph (outline) — a colour-pick affordance. */
  public static FlatSVGIcon colorize() {
    return colorize(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #colorize()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon colorize(final int size) {
    return load("colorize", size);
  }

  /** Eyedropper / colorize glyph (filled / solid) — pairs with {@link #colorize()} as "active". */
  public static FlatSVGIcon colorizeFilled() {
    return colorizeFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #colorizeFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon colorizeFilled(final int size) {
    return load("colorize_fill", size);
  }

  /** Widgets / four-square cluster glyph (outline) — the M3 "components / building blocks" mark. */
  public static FlatSVGIcon widgets() {
    return widgets(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #widgets()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon widgets(final int size) {
    return load("widgets", size);
  }

  /** Widgets glyph (filled / solid) — pairs with {@link #widgets()} as the "active" state. */
  public static FlatSVGIcon widgetsFilled() {
    return widgetsFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #widgetsFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon widgetsFilled(final int size) {
    return load("widgets_fill", size);
  }

  /** Layers / stacked-surfaces glyph (outline) — the M3 "layered surface" mark. */
  public static FlatSVGIcon layers() {
    return layers(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #layers()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon layers(final int size) {
    return load("layers", size);
  }

  /** Layers glyph (filled / solid) — pairs with {@link #layers()} as the "active" state. */
  public static FlatSVGIcon layersFilled() {
    return layersFilled(DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #layersFilled()}.
   *
   * @param size pixel size for the returned icon
   * @return the icon at the requested size
   * @version v0.3.0
   * @since v0.3.0
   */
  public static FlatSVGIcon layersFilled(final int size) {
    return load("layers_fill", size);
  }

  private static FlatSVGIcon load(final String name, final int size) {
    final FlatSVGIcon icon = new FlatSVGIcon(BASE + name + ".svg", size, size);
    icon.setColorFilter(THEME_FILTER);
    return icon;
  }

  /**
   * Returns the outline / fill pair for a Material Symbol that has both axes bundled — the
   * canonical M3 toggle-icon-swap pattern. The {@code filled} variant is resolved by appending
   * {@code _fill} to the base name (so {@code pair("push_pin")} loads {@code push_pin.svg} and
   * {@code push_pin_fill.svg}). Both icons render at {@link #DEFAULT_SIZE}.
   *
   * <p>Currently bundled outline/fill pairs: {@code push_pin}, {@code anchor}, {@code favorite},
   * {@code star}, {@code info}, {@code help}, {@code delete}, {@code edit}, {@code visibility},
   * {@code light_mode}, {@code dark_mode}, {@code brightness_auto}, {@code palette}, {@code
   * colorize}, {@code widgets}, {@code layers}. Calling {@code pair} on a name that lacks a bundled
   * {@code _fill} variant throws when the missing SVG is first painted, not on construction — see
   * {@link FlatSVGIcon}'s lazy-load semantics.
   *
   * <p><strong>Why a helper, not an auto-detect inside {@link
   * com.owspfm.elwha.iconbutton.ElwhaIconButton}.</strong> The button stays icon-library-agnostic
   * (no import of {@code com.owspfm.elwha.icons.*}); the outline/fill convention lives here, in the
   * icon library that actually knows about Material Symbol naming. Consumers using non-Material
   * icons supply their own pair via the existing {@code setIcons(resting, selected)} setter.
   *
   * @param name the bare Material Symbol name (no path, no extension, no {@code _fill} suffix)
   * @return the resting + filled pair
   * @version v0.1.0
   * @since v0.1.0
   */
  public static IconPair pair(final String name) {
    return pair(name, DEFAULT_SIZE);
  }

  /**
   * Sized variant of {@link #pair(String)}.
   *
   * @param name the bare Material Symbol name
   * @param size pixel size for both icons in the returned pair
   * @return the resting + filled pair at the requested size
   * @version v0.1.0
   * @since v0.1.0
   */
  public static IconPair pair(final String name, final int size) {
    return new IconPair(load(name, size), load(name + "_fill", size));
  }

  /**
   * The outline / fill icon pair returned by {@link #pair(String)}. Use with {@link
   * com.owspfm.elwha.iconbutton.ElwhaIconButton#setIcons(javax.swing.Icon, javax.swing.Icon)
   * setIcons}:
   *
   * <pre>{@code
   * MaterialIcons.IconPair p = MaterialIcons.pair("push_pin");
   * button.setIcons(p.resting(), p.filled());
   * }</pre>
   *
   * @param resting the resting (outline) icon — rendered when the button is unselected
   * @param filled the filled icon — rendered when the button is selected
   * @author Charles Bryan
   * @version v0.1.0
   * @since v0.1.0
   */
  public record IconPair(FlatSVGIcon resting, FlatSVGIcon filled) {}

  /**
   * Wraps a Material Symbol name as a strongly-typed handle that resolves both fill-axis variants
   * on demand. Created via {@link #symbol(String)}.
   *
   * <p>The selection-state semantics ({@link #unselected()} vs {@link #selected()}) are framed for
   * consumers that toggle a glyph's fill axis based on a discrete selected state — primarily {@link
   * com.owspfm.elwha.navrail.ElwhaNavRailDestination ElwhaNavRailDestination}'s active-indicator
   * pill, but also any future component that wants the same fill-1-when-active pattern without
   * threading two {@link FlatSVGIcon} instances by hand.
   *
   * <p><strong>Graceful fallback.</strong> If the bundle doesn't ship a {@code <name>_fill.svg}
   * (e.g., linework-only glyphs like chevrons, check, +, −, dots — Material Symbols itself doesn't
   * publish fill-1 variants for these), {@link #selected()} returns the unfilled glyph. The
   * consumer sees no visual fill swap, which is the correct result for a glyph with no semantic
   * fill axis. {@link #hasSelectedVariant()} reports the runtime resolution if a consumer wants to
   * decide whether the fill swap will be visible.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public static final class Symbol {

    private final String name;

    Symbol(final String name) {
      this.name = name;
    }

    /** Bare glyph name (no path, no extension, no {@code _fill} suffix). */
    public String name() {
      return name;
    }

    /** Unselected glyph (fill-0) at {@link #DEFAULT_SIZE}. */
    public FlatSVGIcon unselected() {
      return unselected(DEFAULT_SIZE);
    }

    /**
     * Sized variant of {@link #unselected()}.
     *
     * @param size pixel size for the returned icon
     * @return the unfilled glyph at the requested size
     * @version v0.3.0
     * @since v0.3.0
     */
    public FlatSVGIcon unselected(final int size) {
      return load(name, size);
    }

    /**
     * Selected glyph (fill-1) at {@link #DEFAULT_SIZE}; falls back to the unfilled glyph if the
     * bundle doesn't ship a fill variant for this symbol — see class-level note on graceful
     * fallback.
     */
    public FlatSVGIcon selected() {
      return selected(DEFAULT_SIZE);
    }

    /**
     * Sized variant of {@link #selected()}.
     *
     * @param size pixel size for the returned icon
     * @return the fill-1 glyph if bundled; otherwise the unfilled glyph
     * @version v0.3.0
     * @since v0.3.0
     */
    public FlatSVGIcon selected(final int size) {
      return hasSelectedVariant() ? load(name + "_fill", size) : load(name, size);
    }

    /**
     * Reports whether the bundle ships a fill-1 SVG for this symbol — i.e., whether {@link
     * #selected()} will visibly differ from {@link #unselected()}.
     *
     * @return {@code true} if a {@code <name>_fill.svg} resource exists; {@code false} otherwise
     * @version v0.3.0
     * @since v0.3.0
     */
    public boolean hasSelectedVariant() {
      return MaterialIcons.class.getClassLoader().getResource(BASE + name + "_fill.svg") != null;
    }
  }

  /**
   * Factory for a {@link Symbol} handle wrapping the named glyph. The base SVG ({@code <name>.svg})
   * must be bundled; the fill variant ({@code <name>_fill.svg}) is optional and resolved at paint
   * time — see {@link Symbol} for fallback semantics.
   *
   * @param name the bare Material Symbol name (no path, no extension, no {@code _fill} suffix)
   * @return a symbol handle backed by the bundle
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Symbol symbol(final String name) {
    return new Symbol(name);
  }
}
