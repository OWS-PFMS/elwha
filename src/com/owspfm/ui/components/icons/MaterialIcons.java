package com.owspfm.ui.components.icons;

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
 * leak across call sites.
 *
 * <p>Adding new icons: drop the SVG under {@code resources/com/owspfm/icons/material/} (download
 * from <a href="https://fonts.google.com/icons">fonts.google.com/icons</a> with the same style axes
 * — Rounded, 400, fill 0, 20px — to keep visual consistency) and add a lookup here. Material
 * Symbols are licensed Apache-2.0; see the project LICENSE-NOTICES file for attribution.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class MaterialIcons {

  /** Default render size in pixels — matches the leading-icon footprint on FlatPill. */
  public static final int DEFAULT_SIZE = 14;

  private static final String BASE = "com/owspfm/icons/material/";

  /**
   * Theme-aware color filter: every painted color in the SVG is remapped to the current {@code
   * Label.foreground}. Material Symbols are monochrome, so a blanket remap is safe and avoids the
   * per-color "map black to foreground" enumeration. The function runs at paint time, so a runtime
   * LAF switch (light ↔ dark) re-themes the icons on the next repaint with no re-allocation.
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
    return load("push_pin");
  }

  /** Pushpin glyph (filled / solid) — pairs with {@link #pushPin()} as the "active" state. */
  public static FlatSVGIcon pushPinFilled() {
    return load("push_pin_fill");
  }

  /** Trash / delete glyph. */
  public static FlatSVGIcon delete() {
    return load("delete");
  }

  /** Pencil / edit glyph. */
  public static FlatSVGIcon edit() {
    return load("edit");
  }

  /** Info "i" glyph. */
  public static FlatSVGIcon info() {
    return load("info");
  }

  /** Heart / favorite glyph. */
  public static FlatSVGIcon favorite() {
    return load("favorite");
  }

  /** Five-point star glyph. */
  public static FlatSVGIcon star() {
    return load("star");
  }

  /** Plus / add glyph. */
  public static FlatSVGIcon add() {
    return load("add");
  }

  /** Checkmark glyph. */
  public static FlatSVGIcon check() {
    return load("check");
  }

  /** Eye / visibility glyph. */
  public static FlatSVGIcon visibility() {
    return load("visibility");
  }

  /** Anchor glyph (outline) — used for "fixed/locked to position" affordances. */
  public static FlatSVGIcon anchor() {
    return load("anchor");
  }

  /** Anchor glyph (filled / solid) — pairs with {@link #anchor()} as the "active" state. */
  public static FlatSVGIcon anchorFilled() {
    return load("anchor_fill");
  }

  /** Grid-view (cards/tiles) glyph. */
  public static FlatSVGIcon gridView() {
    return load("grid_view");
  }

  /** Generic table glyph. */
  public static FlatSVGIcon table() {
    return load("table");
  }

  /** Fine background-grid glyph — pairs well as a "show grid overlay" toggle. */
  public static FlatSVGIcon backgroundGridSmall() {
    return load("background_grid_small");
  }

  /** Select-all glyph. */
  public static FlatSVGIcon selectAll() {
    return load("select_all");
  }

  /** Deselect glyph (counterpart to select-all). */
  public static FlatSVGIcon deselect() {
    return load("deselect");
  }

  /**
   * Generic lookup for cases where a name is computed at runtime. Throws if the resource is
   * missing.
   *
   * @param theName the bare icon name (no path, no extension), e.g. {@code "push_pin"}
   * @return a fresh icon sized to {@link #DEFAULT_SIZE}
    * @version v0.1.0
    * @since v0.1.0
   */
  public static FlatSVGIcon get(final String theName) {
    return load(theName);
  }

  private static FlatSVGIcon load(final String theName) {
    final FlatSVGIcon icon = new FlatSVGIcon(BASE + theName + ".svg", DEFAULT_SIZE, DEFAULT_SIZE);
    icon.setColorFilter(THEME_FILTER);
    return icon;
  }
}
