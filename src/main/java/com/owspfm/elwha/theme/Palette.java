package com.owspfm.elwha.theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One complete role-to-value color map for a single mode — every one of the 49 {@link ColorRole}s
 * mapped to a concrete {@link Color}.
 *
 * <p>A {@code Palette} is color only. Shape and spacing are fixed enums, not palette data — see
 * {@code elwha-theme-install-api.md} §1.4. The 12 mode-invariant <em>fixed</em> roles are stored
 * here just like any other role (with identical values in the light and dark palettes), keeping the
 * type self-contained.
 *
 * <p><strong>Completeness is validated at construction.</strong> {@link Builder#build()} throws if
 * any role is missing — a partial palette would produce {@code null} resolves and silent paint
 * bugs. Palettes come from the M3 builder export, which is always complete, so fail-fast costs
 * nothing and catches transcription errors early.
 *
 * <p>Instances are immutable.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public final class Palette {

  private final Map<ColorRole, Color> colors;

  private Palette(Map<ColorRole, Color> colors) {
    this.colors = colors;
  }

  /**
   * Returns the color this palette assigns to {@code role}.
   *
   * @param role the color role to look up
   * @return the assigned color, never {@code null} (completeness is guaranteed at construction)
   * @version v0.1.0
   * @since v0.1.0
   */
  public Color get(ColorRole role) {
    return colors.get(role);
  }

  /**
   * Value equality — two palettes are equal when they assign the same color to every role.
   *
   * <p>A palette is a complete, immutable map of 49 roles to colors and nothing else, so identity
   * equality made two loads of one JSON file unequal. That is the defect behind {@link
   * MaterialPalettes}'s resource-keyed cache ((#671)), which fixed the bundled case by ensuring
   * there is only ever one instance; a consumer-loaded palette had no such guarantee and could
   * never be compared against a bundled one. Ruled for the 1.0 freeze in (#698).
   *
   * @param obj the object to compare against
   * @return whether {@code obj} is a palette with the same role-to-color mapping
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Palette other)) {
      return false;
    }
    return colors.equals(other.colors);
  }

  /**
   * @return a hash consistent with {@link #equals(Object)}
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public int hashCode() {
    return colors.hashCode();
  }

  /**
   * A short fingerprint rather than all 49 roles — enough to tell two palettes apart in a debugger
   * or an assertion message without filling the line.
   *
   * @return the palette's primary and surface colors in hex
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public String toString() {
    return "Palette[primary="
        + hex(get(ColorRole.PRIMARY))
        + ", surface="
        + hex(get(ColorRole.SURFACE))
        + "]";
  }

  private static String hex(Color color) {
    return String.format("#%06X", color.getRGB() & 0xFFFFFF);
  }

  /**
   * Creates a new, empty {@link Builder}.
   *
   * @return a fresh palette builder
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable accumulator for a {@link Palette}. Not thread-safe; intended for single-threaded
   * assembly followed by a single {@link #build()}.
   *
   * @author Charles Bryan
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final class Builder {

    private final EnumMap<ColorRole, Color> colors = new EnumMap<>(ColorRole.class);

    private Builder() {}

    /**
     * Assigns a color to a role, replacing any previous assignment.
     *
     * @param role the color role to assign
     * @param color the color to assign to it
     * @return this builder, for chaining
     * @version v0.1.0
     * @since v0.1.0
     */
    public Builder set(ColorRole role, Color color) {
      colors.put(Objects.requireNonNull(role, "role"), Objects.requireNonNull(color, "color"));
      return this;
    }

    /**
     * Builds the immutable {@link Palette}, validating that all 49 color roles are present.
     *
     * @return the completed palette
     * @throws IllegalStateException if any color role has not been assigned
     * @version v0.1.0
     * @since v0.1.0
     */
    public Palette build() {
      List<ColorRole> missing = new ArrayList<>();
      for (ColorRole role : ColorRole.values()) {
        if (!colors.containsKey(role)) {
          missing.add(role);
        }
      }
      if (!missing.isEmpty()) {
        throw new IllegalStateException(
            "Palette is incomplete — missing " + missing.size() + " role(s): " + missing);
      }
      return new Palette(new EnumMap<>(colors));
    }
  }
}
