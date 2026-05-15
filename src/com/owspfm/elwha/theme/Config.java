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
 * @version v0.1.0
 * @since v0.1.0
 */
public final class Config {

  private final Theme theme;
  private final Mode mode;
  private final Typography typography;

  private Config(Theme theme, Mode mode, Typography typography) {
    this.theme = theme;
    this.mode = mode;
    this.typography = typography;
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
   * Returns a copy of this config with a different theme.
   *
   * @param newTheme the theme for the derived config
   * @return a new config identical to this one but with {@code newTheme}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Config withTheme(Theme newTheme) {
    return new Config(Objects.requireNonNull(newTheme, "theme"), mode, typography);
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
    return new Config(theme, Objects.requireNonNull(newMode, "mode"), typography);
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
    return new Config(theme, mode, Objects.requireNonNull(newTypography, "typography"));
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
      return new Config(theme, mode, resolvedTypography);
    }
  }
}
