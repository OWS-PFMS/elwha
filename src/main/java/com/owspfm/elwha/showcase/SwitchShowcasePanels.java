package com.owspfm.elwha.showcase;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.switches.ElwhaSwitch;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaSwitch} (story #407): a {@link
 * ComponentWorkbench} stage with a live switch (click, drag, or Space it — the Selected control
 * tracks user toggles back), the icons configurations (both / selected-only / a custom
 * favorite-pair), an accessible-label input, and Enabled + RTL state controls. The state gallery
 * matrix renders the four icon configurations across unselected / selected / hover / focused /
 * pressed / both disabled cells — the two disabled columns exist because the spec's disabled
 * treatment is asymmetric (opaque {@code SURFACE} handle when selected, 38% {@code ON_SURFACE} when
 * not).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class SwitchShowcasePanels {

  private SwitchShowcasePanels() {}

  /** Builds the interactive Workbench (live switch stage + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JCheckBox selectedBox = new JCheckBox("Selected", true);
    final JCheckBox iconsBox = new JCheckBox("Icons (both states)");
    final JCheckBox onlySelectedIconBox = new JCheckBox("Show only selected icon");
    final JCheckBox customIconsBox = new JCheckBox("Custom icons (favorite pair)");
    final JTextField labelField = new JTextField("Wi-Fi");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);
    final JCheckBox rtlBox = new JCheckBox("Right-to-left");

    final ElwhaSwitch elwhaSwitch = new ElwhaSwitch(true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Switch");
    controls.addControl("", selectedBox);
    controls.addControl("Label", labelField);
    controls.addSection("Icons");
    controls.addControl("", iconsBox);
    controls.addControl("", onlySelectedIconBox);
    controls.addControl("", customIconsBox);
    controls.addSection("State");
    controls.addControl("", enabledBox);
    controls.addControl("", rtlBox);

    final Runnable apply =
        () -> {
          final boolean icons = iconsBox.isSelected();
          final boolean onlySelected = onlySelectedIconBox.isSelected();
          final boolean custom = customIconsBox.isSelected();
          customIconsBox.setEnabled(icons || onlySelected);
          elwhaSwitch.setSelected(selectedBox.isSelected());
          elwhaSwitch.setIconsVisible(icons);
          elwhaSwitch.setShowOnlySelectedIcon(onlySelected);
          elwhaSwitch.setSelectedIcon(custom ? MaterialIcons.favoriteFilled(16) : null);
          elwhaSwitch.setUnselectedIcon(custom ? MaterialIcons.favorite(16) : null);
          elwhaSwitch.setLabel(labelField.getText());
          elwhaSwitch.setEnabled(enabledBox.isSelected());
          elwhaSwitch.setComponentOrientation(
              rtlBox.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          workbench.setCode(
              renderCode(
                  selectedBox.isSelected(),
                  icons,
                  onlySelected,
                  custom,
                  labelField.getText(),
                  enabledBox.isSelected(),
                  rtlBox.isSelected()));
        };

    // The stage is set once — re-staging on every apply would re-parent the live switch and drop
    // its focus mid-gesture; apply only writes properties + code.
    workbench.setStage(stage(elwhaSwitch));

    // A user toggle on the stage switch (click / drag / Space) tracks back into the Selected
    // control and refreshes the code panel; the re-entrant apply writes the same selected value,
    // which fires no second change event, so the loop terminates.
    elwhaSwitch.addChangeListener(
        e -> {
          selectedBox.setSelected(elwhaSwitch.isSelected());
          apply.run();
        });

    selectedBox.addActionListener(e -> apply.run());
    iconsBox.addActionListener(e -> apply.run());
    onlySelectedIconBox.addActionListener(e -> apply.run());
    customIconsBox.addActionListener(e -> apply.run());
    enabledBox.addActionListener(e -> apply.run());
    rtlBox.addActionListener(e -> apply.run());
    labelField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                apply.run();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                apply.run();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                apply.run();
              }
            });
    apply.run();
    return workbench;
  }

  /** Builds the state × configuration gallery matrix. */
  static JComponent buildGallery() {
    final String[] columns = {
      "Unselected",
      "Selected",
      "Hover",
      "Focused (Tab to)",
      "Pressed",
      "Disabled unselected",
      "Disabled selected"
    };
    final String[] rows = {"Basic", "Icons", "Selected icon only", "Custom icons"};

    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 10, 6, 10);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    matrix.add(header("Config \\ State"), gbc);
    for (int c = 0; c < columns.length; c++) {
      gbc.gridx = c + 1;
      gbc.anchor = GridBagConstraints.CENTER;
      matrix.add(header(columns[c]), gbc);
    }

    for (int r = 0; r < rows.length; r++) {
      gbc.gridx = 0;
      gbc.gridy = r + 1;
      gbc.anchor = GridBagConstraints.LINE_START;
      matrix.add(header(rows[r]), gbc);
      for (int c = 0; c < columns.length; c++) {
        gbc.gridx = c + 1;
        gbc.anchor = GridBagConstraints.CENTER;
        matrix.add(galleryCell(r, c), gbc);
      }
    }
    return matrix;
  }

  private static JComponent galleryCell(final int row, final int col) {
    final ElwhaSwitch s = galleryRow(row);
    s.setLabel("Gallery switch");
    switch (col) {
      case 1 -> s.setSelected(true);
      case 2 -> {
        s.setSelected(true);
        s.setHovered(true);
      }
      case 3 -> s.setSelected(true);
      case 4 -> {
        s.setSelected(true);
        s.setPressed(true);
      }
      case 5 -> s.setEnabled(false);
      case 6 -> {
        s.setSelected(true);
        s.setEnabled(false);
      }
      default -> {
        // Unselected idle. Column 3 (focused) is live — Tab into the cell.
      }
    }
    return s;
  }

  /** A fresh gallery switch for the row's icon configuration. */
  private static ElwhaSwitch galleryRow(final int row) {
    final ElwhaSwitch s = new ElwhaSwitch();
    switch (row) {
      case 1 -> s.setIconsVisible(true);
      case 2 -> s.setShowOnlySelectedIcon(true);
      case 3 -> {
        s.setIconsVisible(true);
        s.setSelectedIcon(MaterialIcons.favoriteFilled(16));
        s.setUnselectedIcon(MaterialIcons.favorite(16));
      }
      default -> {
        // Basic — no icons.
      }
    }
    return s;
  }

  private static JComponent stage(final ElwhaSwitch elwhaSwitch) {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    panel.add(elwhaSwitch);
    return panel;
  }

  private static JLabel header(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private static String renderCode(
      final boolean selected,
      final boolean icons,
      final boolean onlySelected,
      final boolean custom,
      final String label,
      final boolean enabled,
      final boolean rtl) {
    final StringBuilder code = new StringBuilder(256);
    code.append("ElwhaSwitch wifi = new ElwhaSwitch(").append(selected).append(");\n");
    if (icons) {
      code.append("wifi.setIconsVisible(true);\n");
    }
    if (onlySelected) {
      code.append("wifi.setShowOnlySelectedIcon(true);\n");
    }
    if (custom) {
      code.append("wifi.setSelectedIcon(MaterialIcons.favoriteFilled(16));\n");
      code.append("wifi.setUnselectedIcon(MaterialIcons.favorite(16));\n");
    }
    code.append("wifi.setLabel(\"").append(label).append("\");\n");
    if (!enabled) {
      code.append("wifi.setEnabled(false);\n");
    }
    if (rtl) {
      code.append("wifi.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);\n");
    }
    code.append("wifi.addActionListener(e -> apply(wifi.isSelected()));");
    return code.toString();
  }
}
