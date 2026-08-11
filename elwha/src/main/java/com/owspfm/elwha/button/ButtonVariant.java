package com.owspfm.elwha.button;

import com.owspfm.elwha.theme.ColorRole;

/**
 * Treatment-only surface variants for {@link ElwhaButton} — the five M3 Expressive emphasis levels
 * mapped onto Elwha's token vocabulary.
 *
 * <p>The variant declares <em>treatment</em> (elevated / filled / filled-tonal / outlined / text)
 * and carries the {@link ColorRole}s the button resolves its <strong>default</strong> surface,
 * border, and foreground from. Color and treatment are orthogonal: the surface role is
 * independently overridable on each button via {@link ElwhaButton#setSurfaceRole(ColorRole)}, and
 * the foreground re-pairs against the new effective surface's {@code on}-pair — except for {@link
 * #TEXT}, whose foreground is rigidly {@link ColorRole#PRIMARY} regardless of override.
 *
 * <p>The hybrid selection color model lives in {@link ElwhaButton}'s paint pipeline, not on the
 * variant: {@link #FILLED} and {@link #OUTLINED} swap surface roles between unselected and selected
 * states; {@link #ELEVATED} and {@link #FILLED_TONAL} composite a uniform 12% {@link
 * com.owspfm.elwha.theme.StateLayer#SELECTED} overlay; {@link #TEXT} rejects the SELECTABLE
 * interaction mode at runtime. See {@code docs/research/elwha-button-design.md} §7.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ButtonVariant {

  /**
   * Low-emphasis filled treatment over a low-elevation tint — {@link
   * ColorRole#SURFACE_CONTAINER_LOW} surface, no border, {@link ColorRole#PRIMARY} foreground, dp1
   * shadow at rest. The only variant with a real shadow stack. M3's "lifted but not loud"
   * affordance — reads as colored-icon-and-label on a quiet surface.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  ELEVATED(ColorRole.SURFACE_CONTAINER_LOW, null, ColorRole.PRIMARY),

  /**
   * Highest-emphasis filled treatment — {@link ColorRole#PRIMARY} surface, no border, {@link
   * ColorRole#ON_PRIMARY} foreground. The default variant. Primary affordance in its region.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  FILLED(ColorRole.PRIMARY, null, ColorRole.ON_PRIMARY),

  /**
   * Moderate-emphasis filled treatment — {@link ColorRole#SECONDARY_CONTAINER} surface, no border,
   * {@link ColorRole#ON_SECONDARY_CONTAINER} foreground. M3's canonical CTA pairing with Outlined
   * cards (V3 card spec §3.3). "Active but not primary."
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  FILLED_TONAL(ColorRole.SECONDARY_CONTAINER, null, ColorRole.ON_SECONDARY_CONTAINER),

  /**
   * Medium-emphasis treatment — transparent surface, {@link ColorRole#OUTLINE_VARIANT} border,
   * {@link ColorRole#ON_SURFACE_VARIANT} foreground. Border-only chrome; the container fill is
   * invisible at rest. State-layer overlays paint over a {@link ColorRole#SURFACE}-tinted base.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  OUTLINED(null, ColorRole.OUTLINE_VARIANT, ColorRole.ON_SURFACE_VARIANT),

  /**
   * Lowest-emphasis treatment — transparent surface, no border, {@link ColorRole#PRIMARY}
   * foreground (direct tint, not on-pair). M3 prohibits toggle on the TEXT variant; {@link
   * ElwhaButton#setInteractionMode} throws {@link IllegalStateException} on a TEXT button.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  TEXT(null, null, ColorRole.PRIMARY);

  private final ColorRole surfaceRole;
  private final ColorRole borderRole;
  private final ColorRole foregroundRole;

  ButtonVariant(
      final ColorRole surfaceRole, final ColorRole borderRole, final ColorRole foregroundRole) {
    this.surfaceRole = surfaceRole;
    this.borderRole = borderRole;
    this.foregroundRole = foregroundRole;
  }

  /**
   * Returns the default surface role for this variant, or {@code null} for variants with no resting
   * fill ({@link #OUTLINED} and {@link #TEXT}).
   *
   * @return the default surface role, or {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole surfaceRole() {
    return surfaceRole;
  }

  /**
   * Returns the default border role for this variant, or {@code null} for variants with no resting
   * border ({@link #ELEVATED}, {@link #FILLED}, {@link #FILLED_TONAL}, {@link #TEXT}).
   *
   * @return the default border role, or {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole borderRole() {
    return borderRole;
  }

  /**
   * Returns the default foreground role for this variant — the role used for the label color and
   * icon tint when no surface override is set. Overrides re-pair against the override's {@code
   * on}-role (except for {@link #TEXT}, whose {@link ColorRole#PRIMARY} foreground is rigid).
   *
   * @return the default foreground role (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole foregroundRole() {
    return foregroundRole;
  }
}
