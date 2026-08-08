package com.owspfm.elwha.theme;

import java.util.Objects;

/**
 * A named light/dark {@link Palette} pair — the thing a user "picks."
 *
 * <p>A {@code Theme} carries no FlatLaf base-LAF class: the mode-to-LAF mapping ({@link Mode#LIGHT}
 * to {@code FlatLightLaf}, {@link Mode#DARK} to {@code FlatDarkLaf}) is owned by {@link
 * ElwhaTheme}, because the base-LAF choice is a property of the <em>mode</em>, not the theme. See
 * {@code elwha-theme-install-api.md} §1.2.
 *
 * <p>Instances are immutable.
 *
 * @author Charles Bryan
 * @version v0.5.0
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

  /**
   * Value equality — two themes are equal when they carry the same name and the same pair of
   * palettes.
   *
   * <p>Its absence is what (#671) actually hit: {@code baseline.json} loaded twice gave two
   * equal-but-distinct themes, so {@code primary().contains(baseline())} was false and a palette
   * picker could not mark the installed theme as selected. That was fixed by caching one instance
   * per resource path, which {@link MaterialPalettes} still does — but the cache only ever covered
   * the bundled entry points, leaving a consumer who built a theme through {@link PaletteLoader} or
   * the constructor unable to compare it against anything. Ruled for the 1.0 freeze in (#698).
   *
   * @param obj the object to compare against
   * @return whether {@code obj} is a theme with the same name and palettes
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Theme other)) {
      return false;
    }
    return name.equals(other.name) && light.equals(other.light) && dark.equals(other.dark);
  }

  /**
   * @return a hash consistent with {@link #equals(Object)}
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + light.hashCode();
    result = 31 * result + dark.hashCode();
    return result;
  }

  /**
   * @return the theme's name, which is what identifies it to a reader
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public String toString() {
    return "Theme[" + name + "]";
  }
}
