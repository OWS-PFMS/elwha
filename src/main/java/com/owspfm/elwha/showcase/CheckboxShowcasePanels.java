package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaCheckbox} (story #414; truncation visibility
 * #443): a {@link ComponentWorkbench} stage with a live tri-state selector, label text, a
 * constrain-width pair (the stage honors preferred size, which grows with the label — the
 * constraint is what makes the label's ellipsis truncation observable), error, and enabled controls
 * plus the generated-code view, and a state gallery matrix rendering enabled / hover / focused /
 * pressed / disabled across the plain, labeled, error, indeterminate, and truncating-label
 * configurations. The boolean workbench controls are themselves {@code ElwhaCheckbox}es and the
 * label control is an {@link ElwhaTextField} — the component dogfoods its own leaf.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class CheckboxShowcasePanels {

  private CheckboxShowcasePanels() {}

  /** Builds the interactive Workbench (live checkbox stage + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<CheckState> stateBox = new JComboBox<>(CheckState.values());
    final ElwhaTextField labelCtl = ElwhaTextField.outlined("");
    labelCtl.setText("Remember this device");
    final ElwhaCheckbox errorCtl = new ElwhaCheckbox("Error shown");
    final ElwhaCheckbox enabledCtl = new ElwhaCheckbox("Enabled");
    enabledCtl.setChecked(true);
    // The stage honors getPreferredSize(), which grows with the label — so the label's ellipsis
    // truncation is unobservable without an external constraint. This pair makes it observable.
    final ElwhaCheckbox constrainCtl = new ElwhaCheckbox("Constrain width");
    final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(200, 96, 600, 20));

    final ElwhaCheckbox subject = new ElwhaCheckbox();

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Checkbox");
    controls.addControl("State", stateBox);
    controls.addControl("Label", labelCtl);
    controls.addControl("", constrainCtl);
    controls.addControl("Width", widthSpinner);
    controls.addSection("State");
    controls.addControl("", errorCtl);
    controls.addControl("", enabledCtl);

    final Runnable apply =
        () -> {
          final CheckState state = (CheckState) stateBox.getSelectedItem();
          final String label = labelCtl.getText();
          final boolean constrained = constrainCtl.isChecked();
          final int width = (Integer) widthSpinner.getValue();
          widthSpinner.setEnabled(constrained);
          subject.setCheckState(state);
          subject.setLabel(label);
          subject.setErrorShown(errorCtl.isChecked());
          subject.setEnabled(enabledCtl.isChecked());
          subject.setPreferredSize(constrained ? new Dimension(width, 48) : null);
          workbench.setStage(stage(subject));
          workbench.setCode(
              renderCode(
                  state,
                  label,
                  errorCtl.isChecked(),
                  enabledCtl.isChecked(),
                  constrained ? width : -1));
        };

    // The live subject feeds the state selector back so a stage click keeps the rail honest.
    subject.addActionListener(e -> stateBox.setSelectedItem(subject.getCheckState()));

    stateBox.addActionListener(e -> apply.run());
    onChange(labelCtl, apply);
    constrainCtl.addActionListener(e -> apply.run());
    widthSpinner.addChangeListener(e -> apply.run());
    errorCtl.addActionListener(e -> apply.run());
    enabledCtl.addActionListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  /** Builds the state × configuration gallery matrix. */
  static JComponent buildGallery() {
    final String[] columns = {"Enabled", "Hover", "Focused (Tab to)", "Pressed", "Disabled"};
    final String[] rows = {
      "Unchecked",
      "Checked",
      "Indeterminate",
      "Labeled",
      "Error unchecked",
      "Error checked",
      "Long label (truncates)"
    };

    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 8, 6, 8);
    gbc.anchor = GridBagConstraints.LINE_START;

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
        matrix.add(galleryCell(r, c), gbc);
      }
    }
    return matrix;
  }

  private static JComponent galleryCell(final int row, final int col) {
    final ElwhaCheckbox box = galleryCheckbox(row);
    switch (col) {
      case 1 -> box.setHovered(true);
      case 3 -> box.setPressed(true);
      case 4 -> box.setEnabled(false);
      default -> {
        // Enabled idle / focused (focus is live — Tab into the cell).
      }
    }
    return box;
  }

  /** A fresh gallery checkbox for the row's configuration. */
  private static ElwhaCheckbox galleryCheckbox(final int row) {
    final ElwhaCheckbox box =
        switch (row) {
          case 3 -> new ElwhaCheckbox("Labeled");
          case 6 -> new ElwhaCheckbox("A label too long for its constrained width");
          default -> new ElwhaCheckbox();
        };
    switch (row) {
      case 1, 3 -> box.setChecked(true);
      case 2 -> box.setIndeterminate(true);
      case 4 -> box.setErrorShown(true);
      case 5 -> {
        box.setChecked(true);
        box.setErrorShown(true);
      }
      case 6 -> {
        box.setChecked(true);
        // Pinned narrower than the label's natural width so the ellipsis renders statically.
        box.setPreferredSize(new Dimension(170, 48));
      }
      default -> {
        // Unchecked.
      }
    }
    return box;
  }

  private static JComponent stage(final ElwhaCheckbox subject) {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalGlue());
    final JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
    row.setOpaque(false);
    row.add(subject);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, subject.getPreferredSize().height));
    row.setAlignmentX(0.5f);
    panel.add(row);
    panel.add(Box.createVerticalGlue());
    return panel;
  }

  private static JLabel header(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private static String renderCode(
      final CheckState state,
      final String label,
      final boolean error,
      final boolean enabled,
      final int constrainedWidth) {
    final boolean labeled = label != null && !label.isBlank();
    final StringBuilder code = new StringBuilder(220);
    if (labeled) {
      code.append("ElwhaCheckbox checkbox = new ElwhaCheckbox(\"").append(label).append("\");\n");
    } else {
      code.append("ElwhaCheckbox checkbox = new ElwhaCheckbox();\n");
      code.append("checkbox.setAccessibleLabel(\"…\");\n");
    }
    switch (state) {
      case CHECKED -> code.append("checkbox.setChecked(true);\n");
      case INDETERMINATE -> code.append("checkbox.setIndeterminate(true);\n");
      case UNCHECKED -> {
        // Default state — no call needed.
      }
    }
    if (error) {
      code.append("checkbox.setErrorShown(true);\n");
    }
    if (!enabled) {
      code.append("checkbox.setEnabled(false);\n");
    }
    if (constrainedWidth > 0) {
      code.append("// Any layout that constrains the width truncates the label the same way.\n");
      code.append("checkbox.setPreferredSize(new Dimension(")
          .append(constrainedWidth)
          .append(", 48));\n");
    }
    code.append("checkbox.addActionListener(e -> apply(checkbox.isChecked()));");
    return code.toString();
  }

  private static void onChange(final ElwhaTextField control, final Runnable callback) {
    control
        .getEditor()
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                callback.run();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                callback.run();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                callback.run();
              }
            });
  }
}
