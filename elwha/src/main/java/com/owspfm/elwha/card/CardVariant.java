package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.ColorRole;

/**
 * The three M3 card treatments, each anchored to the AndroidX Compose Material3 token sources
 * ({@code ElevatedCardTokens.kt} / {@code FilledCardTokens.kt} / {@code OutlinedCardTokens.kt}).
 * Carries the variant's container role + outline role; resting elevation defaults live on {@link
 * ElwhaCard#defaultElevationFor(CardVariant)}.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §8 for the full variant table.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum CardVariant {

  /**
   * Default. Soft-shadow lifted surface over the page. Container = {@link
   * ColorRole#SURFACE_CONTAINER_LOW}; no border.
   */
  ELEVATED(ColorRole.SURFACE_CONTAINER_LOW, null),

  /**
   * Flat tinted surface above the page. Container = {@link ColorRole#SURFACE_CONTAINER_HIGHEST}; no
   * border.
   */
  FILLED(ColorRole.SURFACE_CONTAINER_HIGHEST, null),

  /**
   * Bordered surface flush with the page. Container = {@link ColorRole#SURFACE}; outline = {@link
   * ColorRole#OUTLINE_VARIANT} at 1dp.
   */
  OUTLINED(ColorRole.SURFACE, ColorRole.OUTLINE_VARIANT);

  private final ColorRole containerRole;
  private final ColorRole borderRole;

  CardVariant(final ColorRole containerRole, final ColorRole borderRole) {
    this.containerRole = containerRole;
    this.borderRole = borderRole;
  }

  /**
   * The container fill role for this variant in the resting state.
   *
   * @return the role used to resolve the chassis fill color
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole containerRole() {
    return containerRole;
  }

  /**
   * The border role for this variant ({@code null} for {@link #ELEVATED} and {@link #FILLED}, which
   * don't paint a border).
   *
   * @return the border role, or {@code null} for borderless variants
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole borderRole() {
    return borderRole;
  }
}
