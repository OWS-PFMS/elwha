package com.owspfm.elwha.showcase;

import com.owspfm.elwha.icons.MaterialIcons;
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
 * The Elwha Showcase leaf surface for {@link ElwhaSlider} (stories #346 / #351 / #362 / #371): a
 * {@link ComponentWorkbench} stage with a live <strong>Variant</strong> selector (Standard /
 * Centered / Range — an origin control when Centered, lower + upper controls when Range), a live
 * <strong>Size</strong> selector (XS&ndash;XL), and an inset-icon toggle (enabled only for the
 * standard variant at M/L/XL, matching the component's own rule), plus the stops on/off + step,
 * value indicator on/off, end stops, and disabled controls, plus a live
 * <strong>Orientation</strong> selector (Horizontal / Vertical — vertical range stays selectable
 * but is doc-warned in place per §10). The state gallery matrix renders enabled / hover / focused /
 * pressed / disabled across the continuous, stops, and value-bubble configurations for the
 * standard, centered, and range variants, plus size (M, XL), inset-icon, and vertical (standard +
 * centered) rows.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class SliderShowcasePanels {

  private static final int STAGE_TRACK_PX = 280;
  private static final int STAGE_VERTICAL_LENGTH_PX = 300;

  private SliderShowcasePanels() {}

  /** Builds the interactive Workbench (live slider stage + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<ElwhaSlider.Variant> variantBox =
        new JComboBox<>(
            new ElwhaSlider.Variant[] {
              ElwhaSlider.Variant.STANDARD, ElwhaSlider.Variant.CENTERED, ElwhaSlider.Variant.RANGE
            });
    final JComboBox<ElwhaSlider.Orientation> orientationBox =
        new JComboBox<>(ElwhaSlider.Orientation.values());
    final JComboBox<ElwhaSlider.Size> sizeBox = new JComboBox<>(ElwhaSlider.Size.values());
    final JLabel verticalRangeWarn =
        new JLabel("⚠ Vertical range is discouraged (M3) — prefer horizontal.");
    verticalRangeWarn.setVisible(false);

    final JSpinner originSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
    final JSpinner lowerSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 100, 5));
    final JSpinner upperSpinner = new JSpinner(new SpinnerNumberModel(70, 0, 100, 5));
    final JCheckBox stopsBox = new JCheckBox("Stops");
    final JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
    final JCheckBox valueIndicatorBox = new JCheckBox("Value indicator", true);
    final JCheckBox endStopsBox = new JCheckBox("End stops", true);
    final JCheckBox insetIconBox = new JCheckBox("Inset icon");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final ElwhaSlider slider = new ElwhaSlider(0, 100, 70);
    slider.setLabel("Workbench slider");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Slider");
    controls.addControl("Variant", variantBox);
    controls.addControl("Origin", originSpinner);
    controls.addControl("Lower", lowerSpinner);
    controls.addControl("Upper", upperSpinner);
    controls.addControl("Orientation", orientationBox);
    controls.addControl("", verticalRangeWarn);
    controls.addControl("Size", sizeBox);
    controls.addSection("Configuration");
    controls.addControl("", stopsBox);
    controls.addControl("Step", stepSpinner);
    controls.addControl("", valueIndicatorBox);
    controls.addControl("", endStopsBox);
    controls.addControl("", insetIconBox);
    controls.addSection("State");
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          final ElwhaSlider.Variant variant = (ElwhaSlider.Variant) variantBox.getSelectedItem();
          final boolean centered = variant == ElwhaSlider.Variant.CENTERED;
          final boolean range = variant == ElwhaSlider.Variant.RANGE;
          final int origin = (Integer) originSpinner.getValue();
          final int lower = (Integer) lowerSpinner.getValue();
          final int upper = (Integer) upperSpinner.getValue();
          final boolean stops = stopsBox.isSelected();
          final int step = (Integer) stepSpinner.getValue();
          final ElwhaSlider.Size size = (ElwhaSlider.Size) sizeBox.getSelectedItem();
          final ElwhaSlider.Orientation orientation =
              (ElwhaSlider.Orientation) orientationBox.getSelectedItem();
          final boolean vertical = orientation == ElwhaSlider.Orientation.VERTICAL;
          // Inset icon: standard variant + M/L/XL only (matches the component's own rule).
          final boolean insetEligible =
              !centered && !range && size.ordinal() >= ElwhaSlider.Size.M.ordinal();
          originSpinner.setEnabled(centered);
          lowerSpinner.setEnabled(range);
          upperSpinner.setEnabled(range);
          stepSpinner.setEnabled(stops);
          insetIconBox.setEnabled(insetEligible);
          // Vertical range stays selectable (no-nanny doctrine, §10) but is doc-warned in place.
          verticalRangeWarn.setVisible(vertical && range);
          final boolean inset = insetEligible && insetIconBox.isSelected();
          slider.setVariant(variant);
          slider.setOrientation(orientation);
          slider.setSizeVariant(size);
          if (centered) {
            slider.setOrigin(origin);
          }
          if (range) {
            slider.setLowerValue(lower);
            slider.setUpperValue(upper);
          }
          slider.setStops(stops ? step : 0);
          slider.setValueIndicatorEnabled(valueIndicatorBox.isSelected());
          slider.setEndStopsVisible(endStopsBox.isSelected());
          slider.setInsetIcon(inset ? MaterialIcons.brightnessAuto() : null);
          slider.setEnabled(enabledBox.isSelected());
          workbench.setStage(stage(slider));
          workbench.setCode(
              renderCode(
                  variant,
                  origin,
                  lower,
                  upper,
                  stops,
                  step,
                  valueIndicatorBox.isSelected(),
                  endStopsBox.isSelected(),
                  size,
                  orientation,
                  inset,
                  enabledBox.isSelected()));
        };

    variantBox.addActionListener(e -> apply.run());
    orientationBox.addActionListener(e -> apply.run());
    sizeBox.addActionListener(e -> apply.run());
    insetIconBox.addActionListener(e -> apply.run());
    originSpinner.addChangeListener(e -> apply.run());
    lowerSpinner.addChangeListener(e -> apply.run());
    upperSpinner.addChangeListener(e -> apply.run());
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
      "Continuous",
      "Stops (10)",
      "Value bubble",
      "Centered",
      "Centered stops",
      "Centered bubble",
      "Range",
      "Range stops",
      "Range bubble",
      "Size M",
      "Size XL",
      "Inset icon (L)",
      "Vertical standard",
      "Vertical centered"
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
    final ElwhaSlider s = gallerySlider(row);
    s.setLabel("Gallery slider");
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
    final boolean vertical = s.getOrientation() == ElwhaSlider.Orientation.VERTICAL;
    final int cellWidth = vertical ? Math.max(120, s.getPreferredSize().width) : 150;
    holder.setPreferredSize(new Dimension(cellWidth, s.getPreferredSize().height));
    return holder;
  }

  /**
   * A fresh gallery slider for the given row. Rows 0–8 group in threes: 0–2 standard, 3–5 centered,
   * 6–8 range, each as continuous / stops / value-bubble. Rows 9–11 exercise the size scale (M, XL)
   * and the inset icon (a standard L slider); rows 12–13 are the vertical standard + centered
   * transpositions (bottom-up fill). Rows 0–8 stay {@code XS} so they are visually unchanged from
   * before the size phase.
   */
  private static ElwhaSlider gallerySlider(final int row) {
    if (row >= 9) {
      return switch (row) {
        case 9 -> sized(ElwhaSlider.Size.M);
        case 10 -> sized(ElwhaSlider.Size.XL);
        case 11 -> {
          final ElwhaSlider s = sized(ElwhaSlider.Size.L);
          s.setInsetIcon(MaterialIcons.brightnessAuto());
          yield s;
        }
        case 12 -> verticalGallery(galleryVariant(0));
        default -> verticalGallery(galleryVariant(1));
      };
    }
    final int kind = row / 3;
    final int config = row % 3;
    final ElwhaSlider s = galleryVariant(kind);
    switch (config) {
      case 1 -> s.setStops(kind == 1 ? 25 : 10);
      case 2 -> s.setValueIndicatorEnabled(true);
      default -> {
        // Continuous, no value bubble.
      }
    }
    return s;
  }

  /**
   * Transposes a gallery slider to the vertical orientation, capped to a compact gallery height.
   */
  private static ElwhaSlider verticalGallery(final ElwhaSlider slider) {
    slider.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    slider.setPreferredSize(new Dimension(slider.getPreferredSize().width, 130));
    return slider;
  }

  /** A fresh standard slider at the given size for the size/inset gallery rows. */
  private static ElwhaSlider sized(final ElwhaSlider.Size size) {
    final ElwhaSlider s = new ElwhaSlider(0, 100, 60);
    s.setSizeVariant(size);
    return s;
  }

  /** A fresh gallery slider for the variant group (0 standard, 1 centered, 2 range). */
  private static ElwhaSlider galleryVariant(final int kind) {
    return switch (kind) {
      case 1 -> {
        final ElwhaSlider s = new ElwhaSlider(-50, 50, 25);
        s.setVariant(ElwhaSlider.Variant.CENTERED);
        yield s;
      }
      case 2 -> ElwhaSlider.range(0, 100, 30, 70);
      default -> new ElwhaSlider(0, 100, 55);
    };
  }

  private static JComponent stage(final ElwhaSlider slider) {
    final boolean vertical = slider.getOrientation() == ElwhaSlider.Orientation.VERTICAL;
    final int longAxis = vertical ? STAGE_VERTICAL_LENGTH_PX : STAGE_TRACK_PX;
    final int shortAxis = vertical ? slider.getPreferredSize().width : STAGE_TRACK_PX;
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalGlue());
    final JPanel row = new JPanel(new java.awt.BorderLayout());
    row.setOpaque(false);
    final Dimension cell =
        vertical
            ? new Dimension(shortAxis, longAxis)
            : new Dimension(STAGE_TRACK_PX, slider.getPreferredSize().height);
    row.setMaximumSize(cell);
    row.setPreferredSize(cell);
    row.add(slider, java.awt.BorderLayout.CENTER);
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
      final ElwhaSlider.Variant variant,
      final int origin,
      final int lower,
      final int upper,
      final boolean stops,
      final int step,
      final boolean valueIndicator,
      final boolean endStops,
      final ElwhaSlider.Size size,
      final ElwhaSlider.Orientation orientation,
      final boolean insetIcon,
      final boolean enabled) {
    final boolean range = variant == ElwhaSlider.Variant.RANGE;
    final boolean vertical = orientation == ElwhaSlider.Orientation.VERTICAL;
    final StringBuilder code = new StringBuilder(320);
    if (vertical && range) {
      code.append("// Vertical range is discouraged (M3 cognitive-load) — prefer horizontal.\n");
    }
    if (range) {
      code.append("ElwhaSlider slider = ElwhaSlider.range(0, 100, ")
          .append(lower)
          .append(", ")
          .append(upper)
          .append(");\n");
    } else {
      code.append("ElwhaSlider slider = new ElwhaSlider(0, 100, 70);\n");
      if (variant == ElwhaSlider.Variant.CENTERED) {
        code.append("slider.setVariant(ElwhaSlider.Variant.CENTERED);\n");
        code.append("slider.setOrigin(").append(origin).append(");\n");
      }
    }
    if (size != ElwhaSlider.Size.XS) {
      code.append("slider.setSizeVariant(ElwhaSlider.Size.").append(size).append(");\n");
    }
    if (vertical) {
      code.append("slider.setOrientation(ElwhaSlider.Orientation.VERTICAL);\n");
    }
    if (insetIcon) {
      code.append("slider.setInsetIcon(MaterialIcons.brightnessAuto());\n");
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
    if (range) {
      code.append(
          "slider.addChangeListener(e -> apply(slider.getLowerValue(), slider.getUpperValue()));");
    } else {
      code.append("slider.addChangeListener(e -> apply(slider.getValue()));");
    }
    return code.toString();
  }
}
