package com.owspfm.elwha.iconbutton;

import com.owspfm.elwha.theme.ColorRole;

/**
 * Treatment-only surface variants for {@link ElwhaIconButton} — the four canonical Material 3 icon
 * button emphasis levels mapped onto Elwha's token vocabulary.
 *
 * <p>The variant declares <em>treatment</em> (filled / filled-tonal / outlined / standard) and
 * carries the {@link ColorRole}s the icon button resolves its default surface and border from.
 * <strong>Color and treatment are orthogonal:</strong> the variant pins the treatment, and the
 * surface role is independently overridable on each button via {@code setSurfaceRole(ColorRole)}.
 * The foreground is never stored on the variant — it is always derived as the {@code on}-pair of
 * the effective surface role (or {@link ColorRole#ON_SURFACE_VARIANT} for transparent variants).
 *
 * <p>The {@code FILLED} / {@code FILLED_TONAL} surface defaults are the M3 toggle-<em>on</em>
 * colors — Elwha picks the high-emphasis end of M3's per-variant toggle scheme as the resting
 * default and signals selection via the uniform {@link com.owspfm.elwha.theme.StateLayer#SELECTED}
 * overlay. See {@code docs/research/elwha-icon-button-design.md} §10 for the full divergence
 * rationale.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum IconButtonVariant {

  /**
   * Highest-emphasis filled treatment — {@link ColorRole#PRIMARY} surface, no border. The primary
   * affordance in its region. Foreground resolves to {@link ColorRole#ON_PRIMARY}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FILLED(ColorRole.PRIMARY, null),

  /**
   * Moderate-emphasis filled treatment — {@link ColorRole#SECONDARY_CONTAINER} surface, no border.
   * "Active control, not the headline" — the common OWS case. Foreground resolves to {@link
   * ColorRole#ON_SECONDARY_CONTAINER}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FILLED_TONAL(ColorRole.SECONDARY_CONTAINER, null),

  /**
   * Medium-emphasis treatment — transparent surface, {@link ColorRole#OUTLINE} border. Foreground
   * resolves to {@link ColorRole#ON_SURFACE_VARIANT}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINED(null, ColorRole.OUTLINE),

  /**
   * Lowest-emphasis treatment — transparent surface, no border. The borderless toggle pattern (the
   * OWS-tool playground's hand-rolled {@code pushPin ↔ pushPinFilled} pin button is a STANDARD
   * variant under the hood). Foreground resolves to {@link ColorRole#ON_SURFACE_VARIANT} at rest;
   * when the interaction mode is {@code SELECTABLE} and the button is selected, the foreground
   * tints to {@link ColorRole#PRIMARY} — the one per-state foreground swap (icon-button-design §3),
   * there because STANDARD has neither a surface fill nor a border to carry the selection signal on
   * its own.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  STANDARD(null, null);

  private final ColorRole surfaceRole;
  private final ColorRole borderRole;

  IconButtonVariant(ColorRole surfaceRole, ColorRole borderRole) {
    this.surfaceRole = surfaceRole;
    this.borderRole = borderRole;
  }

  /**
   * Returns the default surface role for this variant, or {@code null} for variants with no resting
   * fill ({@link #OUTLINED} and {@link #STANDARD}).
   *
   * @return the default surface role, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole surfaceRole() {
    return surfaceRole;
  }

  /**
   * Returns the default border role for this variant, or {@code null} for variants with no resting
   * border ({@link #FILLED}, {@link #FILLED_TONAL}, {@link #STANDARD}).
   *
   * @return the default border role, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole borderRole() {
    return borderRole;
  }
}
