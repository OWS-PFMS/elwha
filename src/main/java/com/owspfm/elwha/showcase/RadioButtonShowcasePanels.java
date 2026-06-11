package com.owspfm.elwha.showcase;

import com.owspfm.elwha.radio.ElwhaRadioButton;
import com.owspfm.elwha.radio.ElwhaRadioGroup;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaRadioButton} + {@link ElwhaRadioGroup} (story
 * #423): a {@link ComponentWorkbench} stage hosting one persistent live 3-member group (selection
 * survives control changes — the Menu-epic lesson) with selected-member, per-member enabled, RTL,
 * and reduced-motion controls plus a generated construction snippet; and a state gallery — the
 * Unselected / Selected rows against Enabled / Hover / Focused / Pressed / Disabled columns (the
 * pressed column shows the M3 <strong>press swap</strong> from both sides, the disabled column both
 * 0.38 treatments) over a live grouped strip exercising arrows + roving focus in place.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class RadioButtonShowcasePanels {

  private static final String[] MEMBER_NAMES = {"First option", "Second option", "Third option"};

  private RadioButtonShowcasePanels() {}

  /** Builds the interactive Workbench (live radio-group stage + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton[] radios = new ElwhaRadioButton[MEMBER_NAMES.length];
    final JLabel[] rowLabels = new JLabel[MEMBER_NAMES.length];
    for (int i = 0; i < MEMBER_NAMES.length; i++) {
      radios[i] = new ElwhaRadioButton();
      radios[i].setLabel(MEMBER_NAMES[i]);
      rowLabels[i] = new JLabel(MEMBER_NAMES[i]);
      rowLabels[i].setLabelFor(radios[i]);
      group.add(radios[i]);
    }
    group.setSelected(radios[0]);

    final String[] selectionOptions = {"First option", "Second option", "Third option", "(none)"};
    final JComboBox<String> selectedBox = new JComboBox<>(selectionOptions);
    final JCheckBox[] enabledBoxes = new JCheckBox[MEMBER_NAMES.length];
    for (int i = 0; i < MEMBER_NAMES.length; i++) {
      enabledBoxes[i] = new JCheckBox(MEMBER_NAMES[i] + " enabled", true);
    }
    final JCheckBox rtlBox = new JCheckBox("Right-to-left");
    final JCheckBox reducedBox = new JCheckBox("Reduced motion (global)");
    reducedBox.setSelected(MorphAnimator.isReducedMotion());

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Radio group");
    controls.addControl("Selected", selectedBox);
    controls.addSection("Members");
    for (final JCheckBox box : enabledBoxes) {
      controls.addControl("", box);
    }
    controls.addSection("Context");
    controls.addControl("", rtlBox);
    controls.addControl("", reducedBox);

    final JLabel readout = new JLabel(" ", SwingConstants.CENTER);
    group.addChangeListener(
        e -> {
          final ElwhaRadioButton current = group.getSelected();
          readout.setText(
              "group.getSelected() → " + (current == null ? "null" : current.getLabel()));
          // Reflect user-driven changes (click / arrows on the stage) back into the control.
          int index = selectionOptions.length - 1;
          for (int i = 0; i < radios.length; i++) {
            if (current == radios[i]) {
              index = i;
            }
          }
          if (selectedBox.getSelectedIndex() != index) {
            selectedBox.setSelectedIndex(index);
          }
        });

    final Runnable apply =
        () -> {
          final int selection = selectedBox.getSelectedIndex();
          if (selection >= 0 && selection < radios.length) {
            if (group.getSelected() != radios[selection]) {
              group.setSelected(radios[selection]);
            }
          } else if (group.getSelected() != null) {
            group.clearSelection();
          }
          final ComponentOrientation orientation =
              rtlBox.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          for (int i = 0; i < radios.length; i++) {
            radios[i].setEnabled(enabledBoxes[i].isSelected());
            radios[i].setComponentOrientation(orientation);
          }
          MorphAnimator.setReducedMotion(reducedBox.isSelected());
          workbench.setStage(stage(radios, rowLabels, readout, orientation));
          workbench.setCode(
              renderCode(
                  selection,
                  new boolean[] {
                    enabledBoxes[0].isSelected(),
                    enabledBoxes[1].isSelected(),
                    enabledBoxes[2].isSelected()
                  },
                  rtlBox.isSelected()));
        };

    selectedBox.addActionListener(e -> apply.run());
    for (final JCheckBox box : enabledBoxes) {
      box.addActionListener(e -> apply.run());
    }
    rtlBox.addActionListener(e -> apply.run());
    reducedBox.addActionListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  /** Builds the state gallery matrix plus the live grouped strip. */
  static JComponent buildGallery() {
    final JPanel stack = new JPanel();
    stack.setOpaque(false);
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
    stack.add(matrix());
    stack.add(Box.createVerticalStrut(12));
    stack.add(groupedStrip());
    return stack;
  }

  private static JComponent matrix() {
    final String[] columns = {
      "Enabled", "Hover", "Focused (Tab to)", "Pressed (the swap)", "Disabled"
    };
    final String[] rows = {"Unselected", "Selected"};

    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setOpaque(false);
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 10, 6, 10);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridx = 0;
    gbc.gridy = 0;
    matrix.add(header("Config \\ State"), gbc);
    for (int c = 0; c < columns.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(header(columns[c]), gbc);
    }
    for (int r = 0; r < rows.length; r++) {
      gbc.gridx = 0;
      gbc.gridy = r + 1;
      matrix.add(header(rows[r]), gbc);
      for (int c = 0; c < columns.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(galleryCell(r == 1, c), gbc);
      }
    }
    return matrix;
  }

  private static JComponent galleryCell(final boolean selected, final int col) {
    final ElwhaRadioButton radio = new ElwhaRadioButton(selected);
    radio.setLabel("Gallery radio");
    switch (col) {
      case 1 -> radio.setHovered(true);
      case 3 -> radio.setPressed(true);
      case 4 -> radio.setEnabled(false);
      default -> {
        // Enabled idle / focused (focus is live — Tab into the cell).
      }
    }
    final JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    holder.setOpaque(false);
    holder.setPreferredSize(new Dimension(64, 44));
    holder.add(radio);
    return holder;
  }

  /** A live 3-member group — click, Tab (one stop), and arrow through it in place. */
  private static JComponent groupedStrip() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 4));
    row.setOpaque(false);
    final String[] options = {"Small", "Medium", "Large"};
    for (int i = 0; i < options.length; i++) {
      final ElwhaRadioButton radio = new ElwhaRadioButton(i == 0);
      radio.setLabel(options[i]);
      group.add(radio);
      final JLabel label = new JLabel(options[i]);
      label.setLabelFor(radio);
      row.add(radio);
      row.add(label);
    }

    final JPanel pane = new JPanel(new BorderLayout());
    pane.setOpaque(false);
    pane.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
    pane.add(
        header("Grouped (live) — Tab enters as one stop; arrows move + select, wrap, honor RTL"),
        BorderLayout.NORTH);
    pane.add(row, BorderLayout.CENTER);
    return pane;
  }

  private static JComponent stage(
      final ElwhaRadioButton[] radios,
      final JLabel[] rowLabels,
      final JLabel readout,
      final ComponentOrientation orientation) {
    final JPanel rows = new JPanel();
    rows.setOpaque(false);
    rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
    for (int i = 0; i < radios.length; i++) {
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 2));
      row.setOpaque(false);
      row.setComponentOrientation(orientation);
      row.add(radios[i]);
      row.add(rowLabels[i]);
      row.setAlignmentX(0.5f);
      final Dimension pref = row.getPreferredSize();
      row.setMaximumSize(new Dimension(220, pref.height));
      rows.add(row);
    }

    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalGlue());
    panel.add(rows);
    panel.add(Box.createVerticalStrut(12));
    readout.setAlignmentX(0.5f);
    panel.add(readout);
    panel.add(Box.createVerticalGlue());
    return panel;
  }

  private static JLabel header(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private static String renderCode(
      final int selection, final boolean[] enabled, final boolean rtl) {
    final StringBuilder code = new StringBuilder(320);
    code.append("ElwhaRadioGroup group = new ElwhaRadioGroup();\n");
    final String[] vars = {"first", "second", "third"};
    for (int i = 0; i < vars.length; i++) {
      code.append("ElwhaRadioButton ")
          .append(vars[i])
          .append(" = new ElwhaRadioButton();\n")
          .append(vars[i])
          .append(".setLabel(\"")
          .append(MEMBER_NAMES[i])
          .append("\");\n");
      if (!enabled[i]) {
        code.append(vars[i]).append(".setEnabled(false);\n");
      }
      if (rtl) {
        code.append(vars[i])
            .append(".setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);\n");
      }
      code.append("group.add(").append(vars[i]).append(");\n");
    }
    if (selection >= 0 && selection < vars.length) {
      code.append("group.setSelected(").append(vars[selection]).append(");\n");
    }
    code.append("group.addChangeListener(e -> apply(group.getSelected()));");
    return code.toString();
  }
}
