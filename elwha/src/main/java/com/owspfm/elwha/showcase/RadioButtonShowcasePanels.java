package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.radio.ElwhaRadioButton;
import com.owspfm.elwha.radio.ElwhaRadioGroup;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
 * 0.38 treatments) over a live grouped strip exercising arrows + roving focus in place. The control
 * rail dogfoods {@link ElwhaSelectField} and {@link ElwhaCheckbox}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.4.0
 */
final class RadioButtonShowcasePanels {

  private static final String[] MEMBER_NAMES = {"First option", "Second option", "Third option"};

  /** The one option that names no member — the picker's "clear the selection" entry. */
  private static final String NONE = "(none)";

  private RadioButtonShowcasePanels() {}

  /** Builds the interactive Workbench (live radio-group stage + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton[] radios = new ElwhaRadioButton[MEMBER_NAMES.length];
    for (int i = 0; i < MEMBER_NAMES.length; i++) {
      radios[i] = new ElwhaRadioButton(MEMBER_NAMES[i]);
      group.add(radios[i]);
    }
    group.setSelected(radios[0]);

    final List<String> selectionOptions =
        List.of("First option", "Second option", "Third option", NONE);
    final ElwhaSelectField<String> selectedBox = ElwhaSelectField.outlined("Selected");
    selectedBox.setOptions(selectionOptions);
    selectedBox.setSelectedValue(selectionOptions.get(0));
    final ElwhaCheckbox[] enabledBoxes = new ElwhaCheckbox[MEMBER_NAMES.length];
    for (int i = 0; i < MEMBER_NAMES.length; i++) {
      enabledBoxes[i] = new ElwhaCheckbox(MEMBER_NAMES[i] + " enabled");
      enabledBoxes[i].setChecked(true);
    }
    final ElwhaCheckbox rtlBox = new ElwhaCheckbox("Right-to-left");
    final ElwhaCheckbox reducedBox = new ElwhaCheckbox("Reduced motion (global)");
    reducedBox.setChecked(MorphAnimator.isReducedMotion());

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Radio group");
    controls.addControl("", selectedBox);
    controls.addSection("Members");
    for (final ElwhaCheckbox box : enabledBoxes) {
      controls.addControl("", box);
    }
    controls.addSection("Context");
    controls.addControl("", rtlBox);
    controls.addControl("", reducedBox);

    // Pin the readout to its widest footprint so selection changes never reflow the stage.
    final JLabel readout = new JLabel("group.getSelected() → Second option", SwingConstants.CENTER);
    final Dimension readoutPref = readout.getPreferredSize();
    readout.setPreferredSize(readoutPref);
    readout.setMaximumSize(readoutPref);
    readout.setText("group.getSelected() → First option");
    group.addSelectionChangeListener(
        e -> {
          final ElwhaRadioButton current = group.getSelected();
          readout.setText(
              "group.getSelected() → " + (current == null ? "null" : current.getLabel()));
          // Reflect user-driven changes (click / arrows on the stage) back into the control. The
          // member's own label IS the option value, so the round trip needs no position
          // (conventions §13).
          selectedBox.setSelectedValue(current == null ? NONE : current.getLabel());
        });

    // The stage is built ONCE; apply mutates state in place. Rebuilding per event re-parents the
    // radios, which finishes their tweens mid-flight (removeNotify) and breathes the surface.
    final JPanel[] rowPanels = new JPanel[radios.length];
    workbench.setStage(stage(radios, rowPanels, readout));

    final Runnable apply =
        () -> {
          final ElwhaRadioButton chosen = memberLabelled(radios, selectedBox.getSelectedValue());
          if (chosen != null) {
            if (group.getSelected() != chosen) {
              group.setSelected(chosen);
            }
          } else if (group.getSelected() != null) {
            group.clearSelection();
          }
          final ComponentOrientation orientation =
              rtlBox.isChecked()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          for (int i = 0; i < radios.length; i++) {
            radios[i].setEnabled(enabledBoxes[i].isChecked());
            radios[i].setComponentOrientation(orientation);
            rowPanels[i].setComponentOrientation(orientation);
            rowPanels[i].revalidate();
          }
          MorphAnimator.setReducedMotion(reducedBox.isChecked());
          workbench.setCode(
              renderCode(
                  chosen,
                  new boolean[] {
                    enabledBoxes[0].isChecked(),
                    enabledBoxes[1].isChecked(),
                    enabledBoxes[2].isChecked()
                  },
                  rtlBox.isChecked()));
        };

    selectedBox.addSelectionChangeListener(v -> apply.run());
    for (final ElwhaCheckbox box : enabledBoxes) {
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
    radio.setAccessibleLabel("Gallery radio");
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
      final ElwhaRadioButton radio = new ElwhaRadioButton(options[i], i == 0);
      group.add(radio);
      row.add(radio);
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

  /** Builds the persistent stage once; {@code rowPanels} receives the rows for RTL flips. */
  private static JComponent stage(
      final ElwhaRadioButton[] radios, final JPanel[] rowPanels, final JLabel readout) {
    final JPanel rows = new JPanel();
    rows.setOpaque(false);
    rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
    for (int i = 0; i < radios.length; i++) {
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 2));
      row.setOpaque(false);
      row.add(radios[i]);
      row.setAlignmentX(0.5f);
      final Dimension pref = row.getPreferredSize();
      row.setMaximumSize(new Dimension(220, pref.height));
      rowPanels[i] = row;
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
      final ElwhaRadioButton selected, final boolean[] enabled, final boolean rtl) {
    final StringBuilder code = new StringBuilder(320);
    code.append("ElwhaRadioGroup group = new ElwhaRadioGroup();\n");
    final String[] vars = {"first", "second", "third"};
    for (int i = 0; i < vars.length; i++) {
      code.append("ElwhaRadioButton ")
          .append(vars[i])
          .append(" = new ElwhaRadioButton(\"")
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
    // The one place a position is still the right answer: it names the local variable the
    // snippet declares, which is a property of the generated text and not of the group.
    for (int i = 0; selected != null && i < vars.length; i++) {
      if (MEMBER_NAMES[i].equals(selected.getLabel())) {
        code.append("group.setSelected(").append(vars[i]).append(");\n");
      }
    }
    code.append("group.addSelectionChangeListener(e -> apply(group.getSelected()));");
    return code.toString();
  }

  /** The member carrying {@code label}, or {@code null} for the {@link #NONE} entry. */
  private static ElwhaRadioButton memberLabelled(
      final ElwhaRadioButton[] radios, final String label) {
    for (final ElwhaRadioButton radio : radios) {
      if (radio.getLabel().equals(label)) {
        return radio;
      }
    }
    return null;
  }
}
