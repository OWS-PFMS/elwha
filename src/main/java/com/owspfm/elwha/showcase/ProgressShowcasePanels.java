package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.progress.AbstractElwhaProgressIndicator;
import com.owspfm.elwha.progress.ElwhaCircularProgressIndicator;
import com.owspfm.elwha.progress.ElwhaLinearProgressIndicator;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.slider.ElwhaSlider;
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
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * The Elwha Showcase leaf surface for the progress indicators (story #475): a {@link
 * ComponentWorkbench} staging one live indicator over a session-long shared {@link
 * javax.swing.BoundedRangeModel} — the value {@link ElwhaSlider} drives the same model the
 * indicator reads, dogfooding the shared-model design — with Variant / Thickness / Indeterminate /
 * Wavy / RTL controls (variant swaps the instance; everything else applies live so the 500ms
 * amplitude crossfade is visible) and a "Simulate load" run that sweeps the model 0→100. The
 * gallery stacks the linear configurations and a circular row covering flat / wavy / thick /
 * indeterminate forms.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class ProgressShowcasePanels {

  /** The staged variant. */
  enum ProgressVariant {
    /** The horizontal bar. */
    LINEAR,
    /** The ring. */
    CIRCULAR
  }

  private ProgressShowcasePanels() {}

  /** Builds the interactive Workbench (live indicator + shared-model slider + code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaSelectField<ProgressVariant> variantBox = ElwhaSelectField.outlined("Variant");
    variantBox.setOptions(List.of(ProgressVariant.values()));
    variantBox.setSelectedValue(ProgressVariant.LINEAR);
    final ElwhaSelectField<Integer> thicknessBox = ElwhaSelectField.outlined("Thickness px");
    thicknessBox.setOptions(List.of(4, 6, 8));
    thicknessBox.setSelectedValue(4);
    final ElwhaCheckbox indeterminateBox = new ElwhaCheckbox("Indeterminate");
    final ElwhaCheckbox wavyBox = new ElwhaCheckbox("Wavy");
    final ElwhaCheckbox rtlBox = new ElwhaCheckbox("Right-to-left (linear)");

    final DefaultBoundedRangeModel model = new DefaultBoundedRangeModel(60, 0, 0, 100);
    final ElwhaSlider valueSlider = new ElwhaSlider(model);
    valueSlider.setLabel("Value");
    final ElwhaButton simulate = ElwhaButton.filledTonalButton("Simulate load");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Progress");
    controls.addControl("", variantBox);
    controls.addControl("", thicknessBox);
    controls.addSection("Mode & shape");
    controls.addControl("", indeterminateBox);
    controls.addControl("", wavyBox);
    controls.addControl("", rtlBox);
    controls.addSection("Value (shared model)");
    controls.addControl("", valueSlider);
    controls.addControl("", simulate);

    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setOpaque(false);
    stage.setPreferredSize(new Dimension(520, 200));
    stage.setName("progressShowcaseStage");
    workbench.setStage(stage);

    final AbstractElwhaProgressIndicator[] staged = new AbstractElwhaProgressIndicator[1];
    final Runnable apply =
        () -> {
          final ProgressVariant variant =
              orDefault(variantBox.getSelectedValue(), ProgressVariant.LINEAR);
          final boolean linear = variant == ProgressVariant.LINEAR;
          final boolean variantChanged =
              staged[0] == null || linear != (staged[0] instanceof ElwhaLinearProgressIndicator);
          if (variantChanged) {
            staged[0] =
                linear
                    ? new ElwhaLinearProgressIndicator(model)
                    : new ElwhaCircularProgressIndicator(model);
            stage.removeAll();
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.insets = new Insets(0, 40, 0, 40);
            constraints.fill = linear ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            stage.add(staged[0], constraints);
          }
          staged[0].setTrackThickness(orDefault(thicknessBox.getSelectedValue(), 4));
          staged[0].setIndeterminate(indeterminateBox.isChecked());
          staged[0].setWavy(wavyBox.isChecked());
          staged[0].setComponentOrientation(
              rtlBox.isChecked()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          stage.revalidate();
          stage.repaint();
          workbench.setCode(
              renderCode(
                  linear,
                  orDefault(thicknessBox.getSelectedValue(), 4),
                  indeterminateBox.isChecked(),
                  wavyBox.isChecked(),
                  rtlBox.isChecked()));
        };

    variantBox.addSelectionChangeListener(v -> apply.run());
    thicknessBox.addSelectionChangeListener(v -> apply.run());
    indeterminateBox.addActionListener(e -> apply.run());
    wavyBox.addActionListener(e -> apply.run());
    rtlBox.addActionListener(e -> apply.run());

    final Timer loader = new Timer(40, null);
    loader.addActionListener(
        e -> {
          if (model.getValue() >= model.getMaximum()) {
            loader.stop();
          } else {
            model.setValue(model.getValue() + 1);
          }
        });
    simulate.addActionListener(
        e -> {
          model.setValue(0);
          loader.restart();
        });

    apply.run();
    return workbench;
  }

  /** Builds the state gallery — linear stack + the circular configuration row. */
  static JComponent buildGallery() {
    final JPanel stack = new JPanel();
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
    stack.setOpaque(false);
    stack.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    stack.add(galleryRow("Linear determinate @ 60", linear(60, 4, false, false)));
    stack.add(galleryRow("Linear wavy @ 60", linear(60, 4, true, false)));
    stack.add(galleryRow("Linear indeterminate", linear(60, 4, false, true)));
    stack.add(galleryRow("Linear wavy indeterminate", linear(60, 4, true, true)));
    stack.add(galleryRow("Linear thick 8px wavy @ 60", linear(60, 8, true, false)));
    stack.add(galleryRow("Linear RTL @ 60 (mirrored fill + stop)", rtlLinear()));
    stack.add(galleryRow("Linear without the stop dot @ 60", noStopLinear()));

    final JPanel circulars = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 4));
    circulars.setOpaque(false);
    circulars.add(circularCell("25%", circular(25, 4, false, false)));
    circulars.add(circularCell("75%", circular(75, 4, false, false)));
    circulars.add(circularCell("wavy 60%", circular(60, 4, true, false)));
    circulars.add(circularCell("thick 8px", circular(60, 8, false, false)));
    circulars.add(circularCell("indeterminate", circular(60, 4, false, true)));
    circulars.add(circularCell("wavy indet.", circular(60, 4, true, true)));
    stack.add(galleryRow("Circular — determinate, wavy, thick, indeterminate", circulars));

    stack.add(Box.createVerticalGlue());
    return stack;
  }

  private static ElwhaLinearProgressIndicator linear(
      final int value, final int thickness, final boolean wavy, final boolean indeterminate) {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(0, 100, value);
    bar.setTrackThickness(thickness);
    bar.setWavy(wavy);
    bar.setIndeterminate(indeterminate);
    return bar;
  }

  private static ElwhaLinearProgressIndicator rtlLinear() {
    final ElwhaLinearProgressIndicator bar = linear(60, 4, false, false);
    bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    return bar;
  }

  private static ElwhaLinearProgressIndicator noStopLinear() {
    final ElwhaLinearProgressIndicator bar = linear(60, 4, false, false);
    bar.setTrackStopIndicatorSize(0);
    return bar;
  }

  private static ElwhaCircularProgressIndicator circular(
      final int value, final int thickness, final boolean wavy, final boolean indeterminate) {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator(0, 100, value);
    ring.setTrackThickness(thickness);
    ring.setWavy(wavy);
    ring.setIndeterminate(indeterminate);
    return ring;
  }

  private static JComponent circularCell(final String caption, final JComponent content) {
    final JPanel cell = new JPanel(new BorderLayout(0, 6));
    cell.setOpaque(false);
    final JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    center.setOpaque(false);
    center.add(content);
    cell.add(center, BorderLayout.CENTER);
    cell.add(new JLabel(caption, SwingConstants.CENTER), BorderLayout.SOUTH);
    return cell;
  }

  private static JComponent galleryRow(final String caption, final JComponent content) {
    final JPanel row = new JPanel(new BorderLayout(0, 8));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
    final JLabel label = new JLabel(caption);
    row.add(label, BorderLayout.NORTH);
    final JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    host.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 48));
    host.add(content, BorderLayout.CENTER);
    row.add(host, BorderLayout.CENTER);
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    return row;
  }

  private static String renderCode(
      final boolean linear,
      final int thickness,
      final boolean indeterminate,
      final boolean wavy,
      final boolean rtl) {
    final String type = linear ? "ElwhaLinearProgressIndicator" : "ElwhaCircularProgressIndicator";
    final StringBuilder code = new StringBuilder();
    code.append("// the slider and the indicator share one model\n")
        .append("BoundedRangeModel model = new DefaultBoundedRangeModel(60, 0, 0, 100);\n")
        .append(type)
        .append(" indicator = new ")
        .append(type)
        .append("(model);\n");
    if (thickness != 4) {
      code.append("indicator.setTrackThickness(").append(thickness).append(");\n");
    }
    if (indeterminate) {
      code.append("indicator.setIndeterminate(true);\n");
    }
    if (wavy) {
      code.append("indicator.setWavy(true);\n");
    }
    if (rtl && linear) {
      code.append("indicator.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);\n");
    }
    code.append("indicator.getAccessibleContext().setAccessibleName(\"Download progress\");\n");
    return code.toString();
  }

  private static <T> T orDefault(final T value, final T fallback) {
    return value != null ? value : fallback;
  }
}
