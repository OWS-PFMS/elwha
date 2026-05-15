package com.owspfm.elwha.chip;

import com.owspfm.elwha.theme.ColorRole;

/**
 * Treatment-only surface variants for {@link ElwhaChip}.
 *
 * <p>The variant declares <em>treatment</em> — filled / outlined / ghost — and carries the {@link
 * ColorRole}s the chip resolves its surface and border from. <strong>Color and treatment are
 * orthogonal:</strong> the variant pins the treatment, and the surface role is independently
 * overridable on each chip via {@code setSurfaceRole(ColorRole)}. The foreground is never stored on
 * the variant — it is always derived as the {@code on}-pair of the effective surface role.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ChipVariant {

  /**
   * Filled surface — the M3 default for a chip cluster that needs to read as a distinct group.
   * Surface defaults to {@link ColorRole#PRIMARY_CONTAINER}, border to {@link
   * ColorRole#OUTLINE_VARIANT}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FILLED(ColorRole.PRIMARY_CONTAINER, ColorRole.OUTLINE_VARIANT),

  /**
   * Hairline border with a {@link ColorRole#SURFACE} fill — the M3 resting outlined chip. Best for
   * dense rows where multiple filled chips would crowd the visual field.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINED(ColorRole.SURFACE, ColorRole.OUTLINE),

  /**
   * No resting fill, no resting border. The chip renders as text-with-padding until hovered,
   * pressed, focused, or selected — useful for tab strips where the unselected entries should
   * disappear into the surface. State-layer overlays still composite over a transparent base, with
   * the foreground paired against {@link ColorRole#SURFACE}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  GHOST(null, null);

  private final ColorRole surfaceRole;
  private final ColorRole borderRole;

  ChipVariant(ColorRole surfaceRole, ColorRole borderRole) {
    this.surfaceRole = surfaceRole;
    this.borderRole = borderRole;
  }

  /**
   * Returns the default surface role for this variant, or {@code null} for variants with no resting
   * fill (currently only {@link #GHOST}).
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
   * border (currently only {@link #GHOST}).
   *
   * @return the default border role, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole borderRole() {
    return borderRole;
  }
}
