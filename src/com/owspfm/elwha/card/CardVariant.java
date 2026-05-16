package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;

/**
 * Surface-style variants for {@link ElwhaCard}, mirroring the Material 3 Card spec.
 *
 * <p>The variant controls the surface fill role, default elevation, dragged elevation, and default
 * border treatment. It does not affect layout, slot composition, or interaction behavior — those
 * axes are independent.
 *
 * <p>Variant table (locked in <a
 * href="../../../../../../docs/research/elwha-card-v2-spec.md">elwha-card-v2-spec.md</a>):
 *
 * <table>
 *   <caption>Card variant defaults</caption>
 *   <tr><th>Variant</th><th>Surface role</th><th>Elevation</th><th>Dragged elevation</th><th>Border</th></tr>
 *   <tr><td>{@link #ELEVATED} (default)</td><td>{@link ColorRole#SURFACE_CONTAINER_LOW}</td><td>1 dp</td><td>2 dp</td><td>none</td></tr>
 *   <tr><td>{@link #FILLED}</td><td>{@link ColorRole#SURFACE_CONTAINER_HIGHEST}</td><td>0 dp</td><td>8 dp</td><td>none</td></tr>
 *   <tr><td>{@link #OUTLINED}</td><td>{@link ColorRole#SURFACE}</td><td>0 dp</td><td>8 dp</td><td>{@link ColorRole#OUTLINE_VARIANT}, 1 px</td></tr>
 * </table>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum CardVariant {

  /**
   * Filled background with a soft drop-shadow at the default 1 dp elevation. The M3 spec default;
   * use for surfaces that should visually float above the page (e.g., dialog content, hero cards).
   * Dragged elevation: 2 dp.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ELEVATED(ColorRole.SURFACE_CONTAINER_LOW, /* elevation */ 1, /* dragged */ 2, /* border */ null),

  /**
   * Tinted background (high-emphasis container) with no border and no resting shadow. Best for
   * grouping related content without competing with surrounding emphasis. Dragged elevation: 8 dp.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FILLED(
      ColorRole.SURFACE_CONTAINER_HIGHEST, /* elevation */ 0, /* dragged */ 8, /* border */ null),

  /**
   * Plain surface with a hairline border and no shadow. Best for dense layouts where shadows would
   * create visual noise. Dragged elevation: 8 dp.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINED(ColorRole.SURFACE, /* elevation */ 0, /* dragged */ 8, ColorRole.OUTLINE_VARIANT);

  private final ColorRole surfaceRole;
  private final int restingElevation;
  private final int draggedElevation;
  private final ColorRole borderRole;

  CardVariant(
      final ColorRole surfaceRole,
      final int restingElevation,
      final int draggedElevation,
      final ColorRole borderRole) {
    this.surfaceRole = surfaceRole;
    this.restingElevation = restingElevation;
    this.draggedElevation = draggedElevation;
    this.borderRole = borderRole;
  }

  /**
   * Returns the default surface fill role for this variant.
   *
   * @return the surface role
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole surfaceRole() {
    return surfaceRole;
  }

  /**
   * Returns the default resting elevation in dp for this variant.
   *
   * @return the resting elevation
   * @version v0.1.0
   * @since v0.1.0
   */
  public int restingElevation() {
    return restingElevation;
  }

  /**
   * Returns the dragged-state elevation in dp for this variant, per M3.
   *
   * @return the dragged elevation
   * @version v0.1.0
   * @since v0.1.0
   */
  public int draggedElevation() {
    return draggedElevation;
  }

  /**
   * Returns the default border role for this variant, or {@code null} if the variant has no border.
   *
   * @return the border role, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole borderRole() {
    return borderRole;
  }
}
