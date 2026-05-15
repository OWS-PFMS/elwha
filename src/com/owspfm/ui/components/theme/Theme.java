package com.owspfm.ui.components.theme;

import java.util.Objects;

/**
 * A named light/dark {@link Palette} pair — the thing a user "picks."
 *
 * <p>A {@code Theme} carries no FlatLaf base-LAF class: the mode-to-LAF mapping ({@link Mode#LIGHT}
 * to {@code FlatLightLaf}, {@link Mode#DARK} to {@code FlatDarkLaf}) is owned by {@link
 * FlatCompTheme}, because the base-LAF choice is a property of the <em>mode</em>, not the theme.
 * See {@code flatcomp-theme-install-api.md} §1.2.
 *
 * <p>Instances are immutable.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class Theme {

  private final String name;
  private final Palette light;
  private final Palette dark;

  /**
   * Creates a theme from a name and its two palettes.
   *
   * @param name the theme's display name
   * @param light the palette used in {@link Mode#LIGHT}
   * @param dark the palette used in {@link Mode#DARK}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Theme(String name, Palette light, Palette dark) {
    this.name = Objects.requireNonNull(name, "name");
    this.light = Objects.requireNonNull(light, "light");
    this.dark = Objects.requireNonNull(dark, "dark");
  }

  /**
   * Returns this theme's display name.
   *
   * @return the theme name
   * @version v0.1.0
   * @since v0.1.0
   */
  public String name() {
    return name;
  }

  /**
   * Returns the palette used in {@link Mode#LIGHT}.
   *
   * @return the light palette
   * @version v0.1.0
   * @since v0.1.0
   */
  public Palette light() {
    return light;
  }

  /**
   * Returns the palette used in {@link Mode#DARK}.
   *
   * @return the dark palette
   * @version v0.1.0
   * @since v0.1.0
   */
  public Palette dark() {
    return dark;
  }

  /**
   * Returns the palette for a given mode, resolving {@link Mode#SYSTEM} first.
   *
   * @param mode the mode to select a palette for
   * @return the dark palette if {@code mode} resolves to {@link Mode#DARK}, otherwise the light
   *     palette
   * @version v0.1.0
   * @since v0.1.0
   */
  public Palette paletteFor(Mode mode) {
    return mode.resolved() == Mode.DARK ? dark : light;
  }
}
