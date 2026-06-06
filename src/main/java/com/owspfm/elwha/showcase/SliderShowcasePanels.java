package com.owspfm.elwha.showcase;

import com.owspfm.elwha.slider.ElwhaSlider;
import java.awt.Dimension;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaSlider} (stories #346 / #351): a {@link
 * ComponentWorkbench} stage with a live <strong>Variant</strong> selector (Standard / Centered,
 * with an origin control when Centered) plus the stops on/off + step, value indicator on/off, end
 * stops, and disabled controls (orientation / size selectors remain stubbed-disabled placeholders
 * for later V1 phases). The state gallery matrix renders enabled / hover / focused / pressed /
 * disabled across the continuous, stops, and value-bubble configurations for <em>both</em> the
 * standard and centered variants.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class SliderShowcasePanels {

  private static final int STAGE_TRACK_PX = 280;

  private SliderShowcasePanels() {}

  /** Builds the interactive Workbench (live slider stage + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<ElwhaSlider.Variant> variantBox =
        new JComboBox<>(
            new ElwhaSlider.Variant[] {ElwhaSlider.Variant.STANDARD, ElwhaSlider.Variant.CENTERED});
    final JComboBox<String> orientationBox =
        stubbedCombo("Horizontal", "Vertical orientation lands in a later V1 phase.");
    final JComboBox<String> sizeBox = stubbedCombo("XS", "Sizes S–XL land in a later V1 phase.");

    final JSpinner originSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
    final JCheckBox stopsBox = new JCheckBox("Stops");
    final JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
    final JCheckBox valueIndicatorBox = new JCheckBox("Value indicator", true);
    final JCheckBox endStopsBox = new JCheckBox("End stops", true);
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final ElwhaSlider slider = new ElwhaSlider(0, 100, 70);
    slider.setLabel("Workbench slider");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Slider");
    controls.addControl("Variant", variantBox);
    controls.addControl("Origin", originSpinner);
    controls.addControl("Orientation", orientationBox);
    controls.addControl("Size", sizeBox);
    controls.addSection("Configuration");
    controls.addControl("", stopsBox);
    controls.addControl("Step", stepSpinner);
    controls.addControl("", valueIndicatorBox);
    controls.addControl("", endStopsBox);
    controls.addSection("State");
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          final ElwhaSlider.Variant variant = (ElwhaSlider.Variant) variantBox.getSelectedItem();
          final boolean centered = variant == ElwhaSlider.Variant.CENTERED;
          final int origin = (Integer) originSpinner.getValue();
          final boolean stops = stopsBox.isSelected();
          final int step = (Integer) stepSpinner.getValue();
          originSpinner.setEnabled(centered);
          stepSpinner.setEnabled(stops);
          slider.setVariant(variant);
          if (centered) {
            slider.setOrigin(origin);
          }
          slider.setStops(stops ? step : 0);
          slider.setValueIndicatorEnabled(valueIndicatorBox.isSelected());
          slider.setEndStopsVisible(endStopsBox.isSelected());
          slider.setEnabled(enabledBox.isSelected());
          workbench.setStage(stage(slider));
          workbench.setCode(
              renderCode(
                  variant,
                  origin,
                  stops,
                  step,
                  valueIndicatorBox.isSelected(),
                  endStopsBox.isSelected(),
                  enabledBox.isSelected()));
        };

    variantBox.addActionListener(e -> apply.run());
    originSpinner.addChangeListener(e -> apply.run());
    stopsBox.addActionListener(e -> apply.run());
    stepSpinner.addChangeListener(e -> apply.run());
    valueIndicatorBox.addActionListener(e -> apply.run());
    endStopsBox.addActionListener(e -> apply.run());
    enabledBox.addActionListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  /** Builds the state × configuration gallery matrix. */
  static JComponent buildGallery() {
    final String[] columns = {"Enabled", "Hover", "Focused (Tab to)", "Pressed", "Disabled"};
    final String[] rows = {
      "Continuous", "Stops (10)", "Value bubble", "Centered", "Centered stops", "Centered bubble"
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
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0;
      matrix.add(header(rows[r]), gbc);
      for (int c = 0; c < columns.length; c++) {
        gbc.gridx = c + 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        matrix.add(galleryCell(r, c), gbc);
      }
    }
    return matrix;
  }

  private static JComponent galleryCell(final int row, final int col) {
    final boolean centered = row >= 3;
    final ElwhaSlider s = centered ? new ElwhaSlider(-50, 50, 25) : new ElwhaSlider(0, 100, 55);
    s.setLabel("Gallery slider");
    if (centered) {
      s.setVariant(ElwhaSlider.Variant.CENTERED);
    }
    switch (row) {
      case 1, 4 -> s.setStops(centered ? 25 : 10);
      case 2, 5 -> s.setValueIndicatorEnabled(true);
      default -> {
        // Continuous, no value bubble (rows 0 / 3).
      }
    }
    switch (col) {
      case 1 -> s.setHovered(true);
      case 3 -> s.setPressed(true);
      case 4 -> s.setEnabled(false);
      default -> {
        // Enabled idle / focused (focus is live — Tab into the cell).
      }
    }
    final JPanel holder = new JPanel(new java.awt.BorderLayout());
    holder.setOpaque(false);
    holder.add(s, java.awt.BorderLayout.CENTER);
    holder.setPreferredSize(new Dimension(150, s.getPreferredSize().height));
    return holder;
  }

  private static JComponent stage(final ElwhaSlider slider) {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalGlue());
    final JPanel row = new JPanel(new java.awt.BorderLayout());
    row.setOpaque(false);
    row.setMaximumSize(new Dimension(STAGE_TRACK_PX, slider.getPreferredSize().height));
    row.setPreferredSize(new Dimension(STAGE_TRACK_PX, slider.getPreferredSize().height));
    row.add(slider, java.awt.BorderLayout.CENTER);
    row.setAlignmentX(0.5f);
    panel.add(row);
    panel.add(Box.createVerticalGlue());
    return panel;
  }

  private static JComboBox<String> stubbedCombo(final String only, final String tip) {
    final JComboBox<String> box = new JComboBox<>(new String[] {only});
    box.setSelectedIndex(0);
    box.setEnabled(false);
    box.setToolTipText(tip);
    return box;
  }

  private static JLabel header(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private static String renderCode(
      final ElwhaSlider.Variant variant,
      final int origin,
      final boolean stops,
      final int step,
      final boolean valueIndicator,
      final boolean endStops,
      final boolean enabled) {
    final StringBuilder code = new StringBuilder(220);
    code.append("ElwhaSlider slider = new ElwhaSlider(0, 100, 70);\n");
    if (variant == ElwhaSlider.Variant.CENTERED) {
      code.append("slider.setVariant(ElwhaSlider.Variant.CENTERED);\n");
      code.append("slider.setOrigin(").append(origin).append(");\n");
    }
    if (stops) {
      code.append("slider.setStops(").append(step).append(");\n");
    }
    if (valueIndicator) {
      code.append("slider.setValueIndicatorEnabled(true);\n");
    }
    if (!endStops) {
      code.append("slider.setEndStopsVisible(false);\n");
    }
    if (!enabled) {
      code.append("slider.setEnabled(false);\n");
    }
    code.append("slider.setLabel(\"Brightness\");\n");
    code.append("slider.addChangeListener(e -> apply(slider.getValue()));");
    return code.toString();
  }
}
