package com.owspfm.ui.components.theme;

import java.awt.Color;
import java.util.Optional;
import javax.swing.UIManager;

/**
 * The 49 semantic color roles of the FlatComp token vocabulary — the full standard Material 3 color
 * scheme.
 *
 * <p>A role is a <em>name</em>, not a literal color. Components reference roles; the installed
 * {@link Palette} supplies the values. Each role resolves at paint time from {@link UIManager}
 * under the key {@code FlatComp.color.<camelCaseKey>}, which {@link FlatCompTheme#install} writes.
 *
 * <p><strong>Binding rule.</strong> Components MUST call {@link #resolve()} at paint time (or
 * re-resolve on {@code updateUI()}) — never cache the returned {@link Color} in a field across
 * paints. Runtime theme switching works by re-writing the {@code FlatComp.*} keys; a cached color
 * goes stale and the component silently fails to re-skin. See {@code flatcomp-token-taxonomy.md}.
 *
 * <p><strong>Baseline fallback.</strong> When no theme has been installed, {@link #resolve()}
 * degrades gracefully to a compiled-in M3 baseline value rather than returning {@code null}. The
 * full baseline light/dark scheme ships as a JSON resource loaded by {@code
 * MaterialPalettes.baseline()}; the single value carried here is only the last-ditch fallback.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ColorRole {

  // --- Accent groups — primary / secondary / tertiary (12) ---

  /**
   * Primary accent — the highest-emphasis brand color.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  PRIMARY("primary", "ON_PRIMARY", 0x6750A4),
  /**
   * Foreground for content placed directly on {@link #PRIMARY}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_PRIMARY("onPrimary", null, 0xFFFFFF),
  /**
   * Lower-emphasis primary fill — the {@code FILLED} chip/card surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  PRIMARY_CONTAINER("primaryContainer", "ON_PRIMARY_CONTAINER", 0xE9DDFF),
  /**
   * Foreground for content placed on {@link #PRIMARY_CONTAINER}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_PRIMARY_CONTAINER("onPrimaryContainer", null, 0x22005D),
  /**
   * Secondary accent — supporting emphasis alongside {@link #PRIMARY}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SECONDARY("secondary", "ON_SECONDARY", 0x684FA4),
  /**
   * Foreground for content placed directly on {@link #SECONDARY}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_SECONDARY("onSecondary", null, 0xFFFFFF),
  /**
   * Lower-emphasis secondary fill — the warm-accent chip/card surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SECONDARY_CONTAINER("secondaryContainer", "ON_SECONDARY_CONTAINER", 0xE9DDFF),
  /**
   * Foreground for content placed on {@link #SECONDARY_CONTAINER}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_SECONDARY_CONTAINER("onSecondaryContainer", null, 0x23005C),
  /**
   * Tertiary accent — a contrasting third accent for balance.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TERTIARY("tertiary", "ON_TERTIARY", 0x984063),
  /**
   * Foreground for content placed directly on {@link #TERTIARY}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_TERTIARY("onTertiary", null, 0xFFFFFF),
  /**
   * Lower-emphasis tertiary fill.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TERTIARY_CONTAINER("tertiaryContainer", "ON_TERTIARY_CONTAINER", 0xFFD9E3),
  /**
   * Foreground for content placed on {@link #TERTIARY_CONTAINER}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_TERTIARY_CONTAINER("onTertiaryContainer", null, 0x3E001F),

  // --- Error group (4) ---

  /**
   * Error accent — destructive or invalid state.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ERROR("error", "ON_ERROR", 0xBA1A1A),
  /**
   * Foreground for content placed directly on {@link #ERROR}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_ERROR("onError", null, 0xFFFFFF),
  /**
   * Lower-emphasis error fill.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ERROR_CONTAINER("errorContainer", "ON_ERROR_CONTAINER", 0xFFDAD6),
  /**
   * Foreground for content placed on {@link #ERROR_CONTAINER}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_ERROR_CONTAINER("onErrorContainer", null, 0x410002),

  // --- Surface family (11) ---

  /**
   * The default component surface — the workhorse background role.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE("surface", "ON_SURFACE", 0xFFFBFF),
  /**
   * Foreground for content placed on {@link #SURFACE}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_SURFACE("onSurface", null, 0x1C1B1E),
  /**
   * A muted surface variant — subtle differentiation without an accent.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_VARIANT("surfaceVariant", "ON_SURFACE_VARIANT", 0xE7E0EC),
  /**
   * Foreground for content placed on {@link #SURFACE_VARIANT}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_SURFACE_VARIANT("onSurfaceVariant", null, 0x49454E),
  /**
   * The dimmest surface tone in the elevation ladder.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_DIM("surfaceDim", null, 0xDDD8DD),
  /**
   * The brightest surface tone in the elevation ladder.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_BRIGHT("surfaceBright", null, 0xFDF8FD),
  /**
   * Lowest container tone — elevation ladder (system deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_CONTAINER_LOWEST("surfaceContainerLowest", null, 0xFFFFFF),
  /**
   * Low container tone — elevation ladder (system deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_CONTAINER_LOW("surfaceContainerLow", null, 0xF7F2F7),
  /**
   * Default container tone — elevation ladder (system deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_CONTAINER("surfaceContainer", null, 0xF2ECF1),
  /**
   * High container tone — elevation ladder (system deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_CONTAINER_HIGH("surfaceContainerHigh", null, 0xECE7EB),
  /**
   * Highest container tone — elevation ladder (system deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_CONTAINER_HIGHEST("surfaceContainerHighest", null, 0xE6E1E6),

  // --- Outline (2) ---

  /**
   * Standard outline — the {@code OUTLINED} variant stroke.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINE("outline", null, 0x7A757F),
  /**
   * Subtle outline — dividers and low-emphasis borders.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINE_VARIANT("outlineVariant", null, 0xCAC4CF),

  // --- Inverse (3) ---

  /**
   * Inverse surface — high-contrast surfaces such as tooltips and snackbars.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  INVERSE_SURFACE("inverseSurface", "INVERSE_ON_SURFACE", 0x313033),
  /**
   * Foreground for content placed on {@link #INVERSE_SURFACE}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  INVERSE_ON_SURFACE("inverseOnSurface", null, 0xF4EFF4),
  /**
   * Primary accent re-toned for use on an {@link #INVERSE_SURFACE}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  INVERSE_PRIMARY("inversePrimary", null, 0xCFBCFF),

  // --- Utility (3) ---

  /**
   * Shadow color — elevation system (deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SHADOW("shadow", null, 0x000000),
  /**
   * Scrim color — the dim behind modal surfaces.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SCRIM("scrim", null, 0x000000),
  /**
   * Surface tint — the tonal-lift overlay color (elevation system deferred to v2).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SURFACE_TINT("surfaceTint", null, 0x6750A4),

  // --- Background (2) ---

  /**
   * Page background. Components prefer {@link #SURFACE}; retained for M3 completeness.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  BACKGROUND("background", "ON_BACKGROUND", 0xFFFBFF),
  /**
   * Foreground for content placed on {@link #BACKGROUND}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_BACKGROUND("onBackground", null, 0x1C1B1E),

  // --- Fixed accents — mode-invariant (12) ---

  /**
   * Primary fixed accent — holds the same value in light and dark.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  PRIMARY_FIXED("primaryFixed", "ON_PRIMARY_FIXED", 0xE9DDFF),
  /**
   * Dimmer primary fixed accent — mode-invariant.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  PRIMARY_FIXED_DIM("primaryFixedDim", "ON_PRIMARY_FIXED", 0xCFBCFF),
  /**
   * High-emphasis foreground for content on a primary fixed surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_PRIMARY_FIXED("onPrimaryFixed", null, 0x22005D),
  /**
   * Lower-emphasis foreground for content on a primary fixed surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_PRIMARY_FIXED_VARIANT("onPrimaryFixedVariant", null, 0x4F378A),
  /**
   * Secondary fixed accent — mode-invariant.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SECONDARY_FIXED("secondaryFixed", "ON_SECONDARY_FIXED", 0xE9DDFF),
  /**
   * Dimmer secondary fixed accent — mode-invariant.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SECONDARY_FIXED_DIM("secondaryFixedDim", "ON_SECONDARY_FIXED", 0xD0BCFF),
  /**
   * High-emphasis foreground for content on a secondary fixed surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_SECONDARY_FIXED("onSecondaryFixed", null, 0x23005C),
  /**
   * Lower-emphasis foreground for content on a secondary fixed surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_SECONDARY_FIXED_VARIANT("onSecondaryFixedVariant", null, 0x4F378A),
  /**
   * Tertiary fixed accent — mode-invariant.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TERTIARY_FIXED("tertiaryFixed", "ON_TERTIARY_FIXED", 0xFFD9E3),
  /**
   * Dimmer tertiary fixed accent — mode-invariant.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TERTIARY_FIXED_DIM("tertiaryFixedDim", "ON_TERTIARY_FIXED", 0xFFB0CA),
  /**
   * High-emphasis foreground for content on a tertiary fixed surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_TERTIARY_FIXED("onTertiaryFixed", null, 0x3E001F),
  /**
   * Lower-emphasis foreground for content on a tertiary fixed surface.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ON_TERTIARY_FIXED_VARIANT("onTertiaryFixedVariant", null, 0x7A294B);

  /** The {@code UIManager} key namespace all color roles resolve under. */
  private static final String KEY_PREFIX = "FlatComp.color.";

  private final String key;
  private final String onName;
  private final Color baseline;

  ColorRole(String key, String onName, int baselineRgb) {
    this.key = key;
    this.onName = onName;
    this.baseline = new Color(baselineRgb);
  }

  /**
   * Returns the {@code camelCase} token key for this role — the trailing segment of its {@link
   * UIManager} key, e.g. {@code "onPrimaryContainer"}.
   *
   * @return the camelCase token key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String key() {
    return key;
  }

  /**
   * Returns the fully-qualified {@link UIManager} key this role resolves under, e.g. {@code
   * "FlatComp.color.onPrimaryContainer"}.
   *
   * @return the fully-qualified {@code UIManager} key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String uiKey() {
    return KEY_PREFIX + key;
  }

  /**
   * Resolves this role to a concrete {@link Color} from {@link UIManager} at the moment of the
   * call.
   *
   * <p>Per the binding rule, callers must invoke this at paint time and must not cache the result
   * across paints. If no theme has been installed (the {@code UIManager} key is absent), this
   * degrades to the compiled-in M3 baseline value rather than returning {@code null}.
   *
   * @return the resolved color, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Color resolve() {
    Color resolved = UIManager.getColor(uiKey());
    return resolved != null ? resolved : baseline;
  }

  /**
   * Returns this role's paired {@code on-} foreground role, if it has one.
   *
   * <p>Container and surface roles pair with the foreground role meant to sit on top of them —
   * {@code PRIMARY_CONTAINER} pairs with {@code ON_PRIMARY_CONTAINER}, {@code INVERSE_SURFACE} with
   * {@code INVERSE_ON_SURFACE}, and so on. Roles that are themselves foregrounds, or that have no
   * defined pairing (outlines, utility colors, the surface-container ladder), return {@link
   * Optional#empty()}.
   *
   * @return the paired foreground role, or empty if this role has none
   * @version v0.1.0
   * @since v0.1.0
   */
  public Optional<ColorRole> on() {
    return onName == null ? Optional.empty() : Optional.of(ColorRole.valueOf(onName));
  }

  /**
   * Returns the compiled-in M3 baseline value for this role — the same value {@link #resolve()}
   * falls back to when no theme is installed.
   *
   * @return the baseline color, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Color baseline() {
    return baseline;
  }
}
