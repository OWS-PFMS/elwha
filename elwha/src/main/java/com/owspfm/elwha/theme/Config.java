package com.owspfm.elwha.theme;

import java.util.Objects;

/**
 * The complete, immutable input to {@link ElwhaTheme#install} — a {@link Theme}, a {@link Mode},
 * and a {@link Typography}.
 *
 * <p>Built through {@link #builder()} (or, equivalently, {@link ElwhaTheme#config()}). {@code
 * theme} is required; {@code mode} defaults to {@link Mode#SYSTEM} and {@code typography} to {@link
 * Typography#defaults()}. The {@code with*} methods produce cheap derivations for runtime switching
 * — {@code install(current().withMode(Mode.DARK))} is the whole dark-mode toggle.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public final class Config {

  private final Theme theme;
  private final Mode mode;
  private final Typography typography;
  // #176 Phase 5 — global reduced-motion override. {@code null} = "let the OS reduced-motion
  // signal decide" (the {@link MorphAnimator} class-load detection in
  // ShapeMorphPainter / MorphAnimator picks up macOS / Windows / Linux preferences); a
  // {@code Boolean} value forces on or off regardless of OS state. Snapshot tooling pins this
  // to {@code true} for determinism (design doc §10).
  private final Boolean reducedMotion;

  private Config(Theme theme, Mode mode, Typography typography, Boolean reducedMotion) {
    this.theme = theme;
    this.mode = mode;
    this.typography = typography;
    this.reducedMotion = reducedMotion;
  }

  /**
   * Returns the theme this config installs.
   *
   * @return the theme
   * @version v0.1.0
   * @since v0.1.0
   */
  public Theme theme() {
    return theme;
  }

  /**
   * Returns the mode this config installs under.
   *
   * @return the mode
   * @version v0.1.0
   * @since v0.1.0
   */
  public Mode mode() {
    return mode;
  }

  /**
   * Returns the typography this config installs.
   *
   * @return the typography
   * @version v0.1.0
   * @since v0.1.0
   */
  public Typography typography() {
    return typography;
  }

  /**
   * Returns the explicit reduced-motion override, or {@code null} when the config defers to the OS
   * reduced-motion signal. {@code true} forces every {@link MorphAnimator} into the snap-to-target
   * behavior — see {@link MorphAnimator#setReducedMotion(boolean)}.
   *
   * @return the explicit override, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public Boolean reducedMotion() {
    return reducedMotion;
  }

  /**
   * Returns a copy of this config with a different theme.
   *
   * @param newTheme the theme for the derived config
   * @return a new config identical to this one but with {@code newTheme}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Config withTheme(Theme newTheme) {
    return new Config(Objects.requireNonNull(newTheme, "theme"), mode, typography, reducedMotion);
  }

  /**
   * Returns a copy of this config with a different mode.
   *
   * @param newMode the mode for the derived config
   * @return a new config identical to this one but with {@code newMode}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Config withMode(Mode newMode) {
    return new Config(theme, Objects.requireNonNull(newMode, "mode"), typography, reducedMotion);
  }

  /**
   * Returns a copy of this config with different typography.
   *
   * @param newTypography the typography for the derived config
   * @return a new config identical to this one but with {@code newTypography}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Config withTypography(Typography newTypography) {
    return new Config(
        theme, mode, Objects.requireNonNull(newTypography, "typography"), reducedMotion);
  }

  /**
   * Returns a copy of this config with a different reduced-motion override. Pass {@code null} to
   * defer to the OS reduced-motion signal; pass {@code true} / {@code false} to force on / off.
   *
   * @param newReducedMotion the override, or {@code null}
   * @return a new config identical to this one but with {@code newReducedMotion}
   * @version v0.3.0
   * @since v0.3.0
   */
  public Config withReducedMotion(Boolean newReducedMotion) {
    return new Config(theme, mode, typography, newReducedMotion);
  }

  /**
   * Value equality — two configs are equal when all four settings are.
   *
   * <p>The most useful of the four value types to be able to compare, because {@link
   * ElwhaTheme#current()} hands one back and the documented switching idiom derives from it ({@code
   * install(current().withMode(DARK))}). Without equality a consumer could not ask "is this the
   * config already installed?" to skip a redundant {@link ElwhaTheme#install(Config)} — and
   * installing is not cheap: it re-runs the base look-and-feel setup, writes the whole key table,
   * and calls {@code updateComponentTreeUI} on every live window.
   *
   * @param obj the object to compare against
   * @return whether {@code obj} is a config with the same theme, mode, typography and
   *     reduced-motion override
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Config other)) {
      return false;
    }
    return theme.equals(other.theme)
        && mode == other.mode
        && typography.equals(other.typography)
        && Objects.equals(reducedMotion, other.reducedMotion);
  }

  /**
   * @return a hash consistent with {@link #equals(Object)}
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public int hashCode() {
    return Objects.hash(theme, mode, typography, reducedMotion);
  }

  /**
   * @return the four settings, with {@code reducedMotion=null} shown as the "defer to the OS"
   *     default it means
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public String toString() {
    return "Config[theme="
        + theme
        + ", mode="
        + mode
        + ", typography="
        + typography
        + ", reducedMotion="
        + reducedMotion
        + "]";
  }

  /**
   * Creates a new {@link Builder}.
   *
   * @return a fresh config builder
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable accumulator for a {@link Config}.
   *
   * @author Charles Bryan
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final class Builder {

    private Theme theme;
    private Mode mode = Mode.SYSTEM;
    private Typography typography;
    private Boolean reducedMotion;

    private Builder() {}

    /**
     * Sets the theme to install. Required.
     *
     * @param value the theme
     * @return this builder, for chaining
     * @version v0.1.0
     * @since v0.1.0
     */
    public Builder theme(Theme value) {
      this.theme = Objects.requireNonNull(value, "theme");
      return this;
    }

    /**
     * Sets the mode to install under. Defaults to {@link Mode#SYSTEM} if not set.
     *
     * @param value the mode
     * @return this builder, for chaining
     * @version v0.1.0
     * @since v0.1.0
     */
    public Builder mode(Mode value) {
      this.mode = Objects.requireNonNull(value, "mode");
      return this;
    }

    /**
     * Sets the typography to install. Defaults to {@link Typography#defaults()} if not set.
     *
     * @param value the typography
     * @return this builder, for chaining
     * @version v0.1.0
     * @since v0.1.0
     */
    public Builder typography(Typography value) {
      this.typography = Objects.requireNonNull(value, "typography");
      return this;
    }

    /**
     * Sets the reduced-motion override. {@code null} (the default) defers to the OS signal; {@code
     * true} forces every Elwha morph animation to snap to its destination ({@link
     * MorphAnimator#setReducedMotion}); {@code false} forces animations on regardless of OS state.
     * Snapshot tooling and visual-regression tests pin this to {@code true} for determinism —
     * design doc §10.
     *
     * @param value the override, or {@code null} to defer to the OS
     * @return this builder, for chaining
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder reducedMotion(Boolean value) {
      this.reducedMotion = value;
      return this;
    }

    /**
     * Builds the immutable {@link Config}.
     *
     * @return the completed config
     * @throws IllegalStateException if no theme was set
     * @version v0.1.0
     * @since v0.1.0
     */
    public Config build() {
      if (theme == null) {
        throw new IllegalStateException("Config requires a theme — call theme(...) before build()");
      }
      Typography resolvedTypography = typography != null ? typography : Typography.defaults();
      return new Config(theme, mode, resolvedTypography, reducedMotion);
    }
  }
}
