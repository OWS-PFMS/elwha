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
 * Showcase Gallery sections read identically across components. Story #216 (S7). Also hosts the
 * reusable {@linkplain #buildBadgeEditor(BadgeSlot) live badge editor} consumed by
 * composed-component Workbenches via the sub-component selector pattern (story #306).
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only because the Showcase
 * lives in a sibling package; consumers must not depend on this type.
 *
 * @author Charles Bryan
 * @version v0.4.0
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

  /**
   * Trailing-edge panel — the M3 "Favorites 84" composition ([#219]): a Large badge anchored at the
   * trailing edge of an icon + label row (not on the icon's corner), shown in LTR and RTL so the
   * mirror is visible. The row is wider than its content so the badge right-aligns in clear
   * trailing space, the way a list-row count reads.
   *
   * @return the trailing-edge matrix
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildTrailingEdgePanel() {
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

    gbc.gridy = 1;
    gbc.gridx = 0;
    matrix.add(trailingEdgeCell(ComponentOrientation.LEFT_TO_RIGHT), gbc);
    gbc.gridx = 1;
    matrix.add(trailingEdgeCell(ComponentOrientation.RIGHT_TO_LEFT), gbc);

    return matrix;
  }

  private static JPanel trailingEdgeCell(final ComponentOrientation orientation) {
    final JPanel cell = new JPanel(new GridBagLayout());
    cell.setComponentOrientation(orientation);
    cell.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final JLabel content =
        new JLabel("Favorites", MaterialIcons.favoriteFilled(), javax.swing.SwingConstants.LEADING);
    content.setIconTextGap(8);
    content.setComponentOrientation(orientation);

    final JPanel row = new JPanel(new java.awt.BorderLayout());
    row.setOpaque(false);
    row.setComponentOrientation(orientation);
    row.add(content, java.awt.BorderLayout.LINE_START);
    row.setPreferredSize(new java.awt.Dimension(200, 40));

    cell.add(row, new GridBagConstraints());
    ElwhaBadgeAnchor.attachTrailingEdge(row, ElwhaBadge.large("84"));
    return cell;
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

  // ---------------------------------------------------------------- badge editor

  /**
   * Read/write handle on a host's single badge slot — the seam a {@linkplain
   * #buildBadgeEditor(BadgeSlot) badge editor} drives. A host (e.g. an {@code
   * ElwhaNavRailDestination} or the Badge Workbench itself) exposes its current badge and accepts a
   * replacement; the editor never assumes how the host anchors or stores it. {@code null} means "no
   * badge attached".
   *
   * <p>The replacement seam (rather than in-place mutation) is required because {@link
   * ElwhaBadge}'s variant is fixed at construction — switching Small&nbsp;↔&nbsp;Large means a
   * fresh badge, which the host must re-anchor. Content / color / accessibility edits are applied
   * in place on the current badge.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public interface BadgeSlot {
    /**
     * @return the badge currently in the slot, or {@code null} if none
     */
    ElwhaBadge get();

    /**
     * Installs {@code badge} as the slot's badge ({@code null} detaches any current badge). The
     * host is responsible for anchoring / detaching.
     *
     * @param badge the replacement badge, or {@code null} to clear
     */
    void set(ElwhaBadge badge);
  }

  /**
   * Builds a reusable, live badge editor panel bound to a host's {@link BadgeSlot} — the
   * sub-component editor a composed component's Workbench swaps in to tune its embedded badge
   * (story #306). Every control applies immediately: changing the variant rebuilds and re-installs
   * the badge through the slot; content / color / accessibility edits mutate the slot's current
   * badge in place. The panel exposes the badge's own axes (variant, content, container + label
   * color, accessibility override) and deliberately omits anchor mode and RTL — those are owned by
   * the host (e.g. the Nav Rail drives the anchor by Collapsed&nbsp;/&nbsp;Expanded variant per
   * #300), per the pattern's "host suppresses axes it owns" rule.
   *
   * @param slot the host's badge slot to drive
   * @return a control panel suitable for mounting in a {@code WorkbenchControls} surface
   * @version v0.4.0
   * @since v0.4.0
   */
  public static JPanel buildBadgeEditor(final BadgeSlot slot) {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

    final javax.swing.JComboBox<String> variantBox =
        new javax.swing.JComboBox<>(new String[] {"None", "Small (dot)", "Large"});
    final javax.swing.JTextField contentField = new javax.swing.JTextField("3", 8);
    final javax.swing.JComboBox<com.owspfm.elwha.theme.ColorRole> containerColorBox =
        new javax.swing.JComboBox<>(com.owspfm.elwha.theme.ColorRole.values());
    final javax.swing.JComboBox<com.owspfm.elwha.theme.ColorRole> labelColorBox =
        new javax.swing.JComboBox<>(com.owspfm.elwha.theme.ColorRole.values());
    final javax.swing.JTextField a11yField = new javax.swing.JTextField("", 12);

    // Seed the controls from whatever badge the slot already holds.
    final ElwhaBadge existing = slot.get();
    if (existing == null) {
      variantBox.setSelectedIndex(0);
    } else if (existing.getVariant() == ElwhaBadge.Variant.SMALL) {
      variantBox.setSelectedIndex(1);
    } else {
      variantBox.setSelectedIndex(2);
    }
    containerColorBox.setSelectedItem(com.owspfm.elwha.theme.ColorRole.ERROR);
    labelColorBox.setSelectedItem(com.owspfm.elwha.theme.ColorRole.ON_ERROR);

    // Rebuilds the badge for the selected variant + current control values and installs it through
    // the slot. Variant change must construct a fresh badge (variant is construction-fixed); the
    // content/color/a11y values are re-applied onto the new instance so a variant flip preserves
    // the
    // user's other edits.
    final Runnable rebuild =
        () -> {
          final int v = variantBox.getSelectedIndex();
          final boolean large = v == 2;
          contentField.setEnabled(large);
          labelColorBox.setEnabled(large);
          if (v == 0) {
            slot.set(null);
            return;
          }
          final ElwhaBadge badge;
          if (large) {
            final String content = contentField.getText().isBlank() ? "0" : contentField.getText();
            badge = ElwhaBadge.large(content);
            badge.withLabelColor(
                (com.owspfm.elwha.theme.ColorRole) labelColorBox.getSelectedItem());
          } else {
            badge = ElwhaBadge.small();
          }
          badge.withContainerColor(
              (com.owspfm.elwha.theme.ColorRole) containerColorBox.getSelectedItem());
          if (!a11yField.getText().isBlank()) {
            badge.withAccessibilityText(a11yField.getText());
          }
          slot.set(badge);
        };

    // In-place edit on the current badge when the variant is unchanged — avoids a needless
    // re-anchor
    // on every keystroke. Falls back to a rebuild if the slot is empty / mismatched.
    final Runnable applyContent =
        () -> {
          final ElwhaBadge badge = slot.get();
          if (badge != null && badge.getVariant() == ElwhaBadge.Variant.LARGE) {
            if (!contentField.getText().isBlank()) {
              badge.setContent(contentField.getText());
            }
          } else {
            rebuild.run();
          }
        };
    final Runnable applyColors =
        () -> {
          final ElwhaBadge badge = slot.get();
          if (badge == null) {
            return;
          }
          badge.withContainerColor(
              (com.owspfm.elwha.theme.ColorRole) containerColorBox.getSelectedItem());
          if (badge.getVariant() == ElwhaBadge.Variant.LARGE) {
            badge.withLabelColor(
                (com.owspfm.elwha.theme.ColorRole) labelColorBox.getSelectedItem());
          }
        };
    final Runnable applyA11y =
        () -> {
          final ElwhaBadge badge = slot.get();
          if (badge != null) {
            badge.withAccessibilityText(a11yField.getText().isBlank() ? null : a11yField.getText());
          }
        };

    variantBox.addActionListener(e -> rebuild.run());
    contentField.getDocument().addDocumentListener(onTextChange(applyContent));
    containerColorBox.addActionListener(e -> applyColors.run());
    labelColorBox.addActionListener(e -> applyColors.run());
    a11yField.getDocument().addDocumentListener(onTextChange(applyA11y));

    // Initial enabled-state sync (don't rebuild — that would stomp the slot's existing badge).
    final boolean largeNow = variantBox.getSelectedIndex() == 2;
    contentField.setEnabled(largeNow);
    labelColorBox.setEnabled(largeNow);

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 0, 3, 8);
    gbc.anchor = GridBagConstraints.WEST;
    int rowY = 0;
    rowY = editorRow(panel, gbc, rowY, "Variant", variantBox);
    rowY = editorRow(panel, gbc, rowY, "Content", contentField);
    rowY = editorRow(panel, gbc, rowY, "Container", containerColorBox);
    rowY = editorRow(panel, gbc, rowY, "Label", labelColorBox);
    rowY = editorRow(panel, gbc, rowY, "A11y override", a11yField);
    return panel;
  }

  private static int editorRow(
      final JPanel panel,
      final GridBagConstraints gbc,
      final int rowY,
      final String label,
      final javax.swing.JComponent field) {
    gbc.gridy = rowY;
    gbc.gridx = 0;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(rowLabel(label), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(field, gbc);
    return rowY + 1;
  }

  private static javax.swing.event.DocumentListener onTextChange(final Runnable action) {
    return new javax.swing.event.DocumentListener() {
      @Override
      public void insertUpdate(final javax.swing.event.DocumentEvent e) {
        action.run();
      }

      @Override
      public void removeUpdate(final javax.swing.event.DocumentEvent e) {
        action.run();
      }

      @Override
      public void changedUpdate(final javax.swing.event.DocumentEvent e) {
        action.run();
      }
    };
  }
}
