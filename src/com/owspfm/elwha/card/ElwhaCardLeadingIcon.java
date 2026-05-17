package com.owspfm.elwha.card;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * The card-leading-icon atom — a Layer 2 typed icon label for the leading slot of a card header.
 * Defaults to size 24 dp (M3 canonical card-leading icon size) and color {@link ColorRole#PRIMARY}.
 * When the icon is a {@link FlatSVGIcon} from {@link com.owspfm.elwha.icons.MaterialIcons}, the
 * theme color filter resolves the color role automatically.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §4.4.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardLeadingIcon extends JLabel {

  /** Canonical M3 card-leading icon size in dp. */
  public static final int DEFAULT_ICON_SIZE_DP = 24;

  private ColorRole colorRole = ColorRole.PRIMARY;

  /** Creates an empty leading-icon slot. */
  public ElwhaCardLeadingIcon() {
    this(null);
  }

  /**
   * Creates a leading-icon slot wrapping the given icon. SVG icons are auto-tinted to the active
   * color role via a {@link FlatSVGIcon.ColorFilter}; non-SVG icons render as-is.
   *
   * @param icon the icon (may be {@code null} for a deferred install)
   */
  public ElwhaCardLeadingIcon(final Icon icon) {
    super(icon);
    applyColorFilter();
  }

  /**
   * Sets the foreground color role. Re-tints SVG icons via their color filter on next paint.
   *
   * @param role the color role (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardLeadingIcon setColorRole(final ColorRole role) {
    this.colorRole = Objects.requireNonNull(role, "colorRole");
    applyColorFilter();
    repaint();
    return this;
  }

  /**
   * @return the active color role
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole getColorRole() {
    return colorRole;
  }

  @Override
  public void setIcon(final Icon icon) {
    super.setIcon(icon);
    applyColorFilter();
  }

  @Override
  public Color getForeground() {
    return colorRole != null ? colorRole.resolve() : super.getForeground();
  }

  private void applyColorFilter() {
    final Icon icon = getIcon();
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(
          new FlatSVGIcon.ColorFilter(orig -> colorRole != null ? colorRole.resolve() : orig));
    }
  }
}
