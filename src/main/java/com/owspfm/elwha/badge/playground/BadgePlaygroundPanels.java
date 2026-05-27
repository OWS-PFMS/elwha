package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Library-internal Gallery panels for {@link ElwhaBadge} — static visual matrices the Showcase
 * mounts in the Badge component's Gallery tab. Mirrors the {@code FabPlaygroundPanels} shape so the
 * Showcase Gallery sections read identically across components. Story #216 (S7).
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only because the Showcase
 * lives in a sibling package; consumers must not depend on this type.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class BadgePlaygroundPanels {

  private static final int CELL_GAP = 16;

  private BadgePlaygroundPanels() {}

  /**
   * Variants panel — three cells demonstrating Small, Large at single-digit, and Large at the
   * {@code "999+"} max-char overflow, each anchored to an {@link ElwhaIconButton} host.
   *
   * @return the variants matrix
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildVariantsPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] cellLabels = {"Small", "Large · 1 digit", "Large · max chars"};
    for (int c = 0; c < cellLabels.length; c++) {
      gbc.gridx = c;
      gbc.gridy = 0;
      matrix.add(headerLabel(cellLabels[c]), gbc);
    }

    gbc.gridy = 1;
    gbc.gridx = 0;
    matrix.add(galleryCell(MaterialIcons.favoriteFilled(), ElwhaBadge.small()), gbc);
    gbc.gridx = 1;
    matrix.add(galleryCell(MaterialIcons.starFilled(), ElwhaBadge.large("3")), gbc);
    gbc.gridx = 2;
    matrix.add(galleryCell(MaterialIcons.infoFilled(), ElwhaBadge.large("999+")), gbc);

    return matrix;
  }

  /**
   * Content-range panel — Large badges across the spec'd numeric range plus a non-numeric label,
   * exercising the M3 {@code "999+"} collapse and the non-numeric {@code "+"} overflow suffix
   * (e.g., {@code "BETA"} → {@code "BET+"}).
   *
   * @return the content matrix
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildContentRangePanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] inputs = {"1", "9", "99", "999", "1000", "BETA"};
    for (int c = 0; c < inputs.length; c++) {
      gbc.gridx = c;
      gbc.gridy = 0;
      matrix.add(headerLabel("input \"" + inputs[c] + "\""), gbc);
    }

    for (int c = 0; c < inputs.length; c++) {
      gbc.gridx = c;
      gbc.gridy = 1;
      matrix.add(galleryCell(MaterialIcons.favoriteFilled(), ElwhaBadge.large(inputs[c])), gbc);

      gbc.gridy = 2;
      final ElwhaBadge sample = ElwhaBadge.large(inputs[c]);
      matrix.add(rowLabel("stored: \"" + sample.getContent() + "\""), gbc);
    }

    return matrix;
  }

  /**
   * Orientation panel — the same Large badge anchored to LTR and RTL hosts side-by-side, so the
   * mirror is visible at a glance.
   *
   * @return the orientation matrix
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildOrientationPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridx = 0;
    gbc.gridy = 0;
    matrix.add(headerLabel("LTR"), gbc);
    gbc.gridx = 1;
    matrix.add(headerLabel("RTL"), gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    matrix.add(
        galleryCell(
            MaterialIcons.favoriteFilled(),
            ElwhaBadge.large("999+"),
            ComponentOrientation.LEFT_TO_RIGHT),
        gbc);
    gbc.gridx = 1;
    matrix.add(
        galleryCell(
            MaterialIcons.favoriteFilled(),
            ElwhaBadge.large("999+"),
            ComponentOrientation.RIGHT_TO_LEFT),
        gbc);

    return matrix;
  }

  private static JPanel galleryCell(final Icon icon, final ElwhaBadge badge) {
    return galleryCell(icon, badge, ComponentOrientation.LEFT_TO_RIGHT);
  }

  private static JPanel galleryCell(
      final Icon icon, final ElwhaBadge badge, final ComponentOrientation orientation) {
    final JPanel cell = new JPanel(new GridBagLayout());
    cell.setComponentOrientation(orientation);
    cell.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    final ElwhaIconButton host = new ElwhaIconButton(icon).setButtonSize(IconButtonSize.M);
    host.setComponentOrientation(orientation);
    cell.add(host, new GridBagConstraints());
    ElwhaBadgeAnchor.attach(host, badge);
    return cell;
  }

  private static JLabel headerLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.putClientProperty("FlatLaf.styleClass", "small");
    final Color disabled = UIManager.getColor("Label.disabledForeground");
    if (disabled != null) {
      label.setForeground(disabled);
    }
    return label;
  }

  private static JLabel rowLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.putClientProperty("FlatLaf.styleClass", "small");
    return label;
  }
}
