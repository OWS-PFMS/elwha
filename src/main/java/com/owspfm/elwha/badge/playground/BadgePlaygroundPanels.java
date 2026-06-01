package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ButtonGroupVariant;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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

  /** Default Large-badge content the editor seeds and its reset action restores. */
  public static final String BADGE_DEFAULT_CONTENT = "3";

  /**
   * Builds the canonical reusable live badge editor bound to a host's {@link BadgeSlot} — the
   * single badge-axis control surface shared by the Badge Workbench and by any composed-component
   * Workbench that embeds a badge (story #306). Every control applies immediately; there is no
   * Apply button.
   *
   * <p>Exposes the badge's own axes, grouped into sections matching the standalone Badge Workbench:
   *
   * <ul>
   *   <li><strong>Badge</strong> — a two-segment {@link ElwhaButtonGroup} (Small / Large) plus a
   *       content field with a lib-native {@code − / + / reset} stepper row (enabled only for
   *       numeric Large content; decrement gates at 0, increment caps at 1000 → M3 {@code "999+"}).
   *   <li><strong>Color</strong> — container + label {@link ColorRole} pickers with the M3
   *       Error/On&nbsp;error guidance note (label disabled for Small, which has no label
   *       sub-part).
   *   <li><strong>Accessibility</strong> — an override field with an outlined Reset button.
   * </ul>
   *
   * <p>Deliberately omits anchor mode and RTL: those are owned by the host (e.g. the Nav Rail
   * drives the anchor by Collapsed&nbsp;/&nbsp;Expanded variant per #300), per the pattern's "host
   * suppresses axes it owns" rule. A host that wants those (the Badge Workbench) adds them around
   * the editor. Variant changes rebuild the badge (variant is construction-fixed) and re-install it
   * through the slot, preserving the other edits; content / color / a11y edits mutate the slot
   * badge in place.
   *
   * @param slot the host's badge slot to drive
   * @return a sectioned control panel suitable for mounting in a {@code WorkbenchControls} surface
   * @version v0.4.0
   * @since v0.4.0
   */
  public static JPanel buildBadgeEditor(final BadgeSlot slot) {
    return buildBadgeEditor(slot, () -> {});
  }

  /**
   * {@link #buildBadgeEditor(BadgeSlot)} with an {@code onChange} hook fired after every applied
   * edit — for hosts (e.g. the Badge Workbench) that mirror the badge into a code snippet or other
   * derived view.
   *
   * @param slot the host's badge slot to drive
   * @param onChange invoked on the EDT after each control change has been applied to the slot
   * @return a sectioned control panel
   * @version v0.4.0
   * @since v0.4.0
   */
  public static JPanel buildBadgeEditor(final BadgeSlot slot, final Runnable onChange) {
    // Variant: a two-segment ElwhaButtonGroup (mandatory single-select), not a combo — there's no
    // clean icon for "small dot" vs "large pill", so text segments read clearer. Matches the
    // standalone Badge Workbench's control exactly (the point of the extraction).
    final ElwhaButtonGroup variantGroup =
        new ElwhaButtonGroup(ButtonGroupVariant.CONNECTED)
            .setColorStyle(ButtonGroupColorStyle.FILLED)
            .setSelectionMode(SelectionMode.REQUIRED)
            .add("Small", "Large");
    final JTextField contentField = new JTextField(BADGE_DEFAULT_CONTENT, 8);

    // Lib-native stepper buttons (ElwhaIconButton XS, remove / add / cached). setFocusable(false)
    // keeps focus on the content field so each click doesn't shift focus to the next button.
    final int stepperIconPx = IconButtonSize.XS.iconPx();
    final ElwhaIconButton decrementButton =
        new ElwhaIconButton(MaterialIcons.remove(stepperIconPx)).setButtonSize(IconButtonSize.XS);
    final ElwhaIconButton incrementButton =
        new ElwhaIconButton(MaterialIcons.add(stepperIconPx)).setButtonSize(IconButtonSize.XS);
    final ElwhaIconButton contentResetButton =
        new ElwhaIconButton(MaterialIcons.cached(stepperIconPx)).setButtonSize(IconButtonSize.XS);
    decrementButton.setToolTipText("Decrement count (only enabled when content is a numeric > 0)");
    incrementButton.setToolTipText("Increment count (only enabled when content is numeric)");
    contentResetButton.setToolTipText(
        "Reset content to default (\"" + BADGE_DEFAULT_CONTENT + "\")");
    decrementButton.setFocusable(false);
    incrementButton.setFocusable(false);
    contentResetButton.setFocusable(false);
    final JPanel stepperRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    stepperRow.setOpaque(false);
    stepperRow.add(decrementButton);
    stepperRow.add(incrementButton);
    stepperRow.add(contentResetButton);

    final JComboBox<ColorRole> containerColorBox = new JComboBox<>(ColorRole.values());
    containerColorBox.setSelectedItem(ColorRole.ERROR);
    final JComboBox<ColorRole> labelColorBox = new JComboBox<>(ColorRole.values());
    labelColorBox.setSelectedItem(ColorRole.ON_ERROR);
    final JLabel colorGuidance =
        new JLabel(
            "<html><i>M3 strongly prefers Error / On&nbsp;error.</i></html>",
            MaterialIcons.info(14),
            SwingConstants.LEADING);
    colorGuidance.setToolTipText(
        "<html>M3 doesn't strictly forbid other color roles, but every Badge example in the spec"
            + " uses Error / On&nbsp;error for visibility against navigation surfaces.<br>"
            + "Pick a different pair only when the consumer has a clear contrast story and"
            + " accessible defaults — see design doc §6.</html>");

    final JTextField a11yOverrideField = new JTextField("", 16);
    final ElwhaButton clearOverrideButton = ElwhaButton.outlinedButton("Reset");

    // Seed the variant from whatever badge the slot already holds (default Large).
    final ElwhaBadge existing = slot.get();
    variantGroup.setSelectedIndex(
        existing != null && existing.getVariant() == ElwhaBadge.Variant.SMALL ? 0 : 1);

    // Single apply: read the controls, build the badge for the current variant, install it through
    // the slot. Variant is construction-fixed, so every apply constructs a fresh badge and re-sets
    // it — the host's BadgeSlot.set re-anchors. Content/color/a11y values are re-applied onto the
    // new instance so any edit preserves the others.
    final Runnable apply =
        () -> {
          final boolean isLarge = variantGroup.getSelectedIndex() == 1;
          contentField.setEnabled(isLarge);
          labelColorBox.setEnabled(isLarge);
          final String contentText = contentField.getText() == null ? "" : contentField.getText();
          final boolean numericContent = isAllAsciiDigits(contentText);
          final int parsedCount = numericContent ? Integer.parseInt(contentText) : -1;
          decrementButton.setEnabled(isLarge && numericContent && parsedCount > 0);
          incrementButton.setEnabled(isLarge && numericContent);
          contentResetButton.setEnabled(isLarge);

          final ElwhaBadge badge;
          if (isLarge) {
            badge = ElwhaBadge.large(contentText.isEmpty() ? BADGE_DEFAULT_CONTENT : contentText);
            badge.withLabelColor((ColorRole) labelColorBox.getSelectedItem());
          } else {
            badge = ElwhaBadge.small();
          }
          badge.withContainerColor((ColorRole) containerColorBox.getSelectedItem());
          final String overrideText =
              a11yOverrideField.getText() == null ? "" : a11yOverrideField.getText();
          if (!overrideText.isEmpty()) {
            badge.withAccessibilityText(overrideText);
          }
          slot.set(badge);
          onChange.run();
        };

    clearOverrideButton.addActionListener(
        event -> {
          a11yOverrideField.setText("");
          apply.run();
        });
    decrementButton.addActionListener(
        event -> {
          final String text = contentField.getText();
          if (isAllAsciiDigits(text)) {
            contentField.setText(Integer.toString(Math.max(0, Integer.parseInt(text) - 1)));
          }
        });
    incrementButton.addActionListener(
        event -> {
          final String text = contentField.getText();
          if (isAllAsciiDigits(text)) {
            // Cap at 1000 so increment-from-999 lands on the M3 "999+" overflow.
            contentField.setText(Integer.toString(Math.min(1000, Integer.parseInt(text) + 1)));
          }
        });
    contentResetButton.addActionListener(event -> contentField.setText(BADGE_DEFAULT_CONTENT));
    variantGroup.addSelectionListener(group -> apply.run());
    containerColorBox.addActionListener(event -> apply.run());
    labelColorBox.addActionListener(event -> apply.run());
    contentField.getDocument().addDocumentListener(onTextChange(apply));
    a11yOverrideField.getDocument().addDocumentListener(onTextChange(apply));

    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(editorSection("Badge"));
    panel.add(editorRow("Variant", variantGroup));
    panel.add(editorRow("Content", contentField));
    panel.add(editorRow("", stepperRow));
    panel.add(editorSection("Color"));
    panel.add(editorRow("", colorGuidance));
    panel.add(editorRow("Container", containerColorBox));
    panel.add(editorRow("Label", labelColorBox));
    panel.add(editorSection("Accessibility"));
    panel.add(editorRow("Override", a11yOverrideField));
    panel.add(editorRow("", clearOverrideButton));

    apply.run();
    return panel;
  }

  private static JComponent editorSection(final String title) {
    final JLabel label = new JLabel(title);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
    return label;
  }

  private static JComponent editorRow(final String label, final JComponent field) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    row.setOpaque(false);
    row.setAlignmentX(Component.LEFT_ALIGNMENT);
    if (!label.isEmpty()) {
      row.add(rowLabel(label));
    }
    row.add(field);
    return row;
  }

  private static boolean isAllAsciiDigits(final String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) < '0' || s.charAt(i) > '9') {
        return false;
      }
    }
    return true;
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
