package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.loading.ElwhaLoadingIndicator;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
 * The Elwha Showcase leaf surface for the loading indicator (story #519): a {@link
 * ComponentWorkbench} staging one live {@link ElwhaLoadingIndicator} with Mode / Contained / Size
 * controls and a value {@link ElwhaSlider} sharing the indicator's model (so a determinate run
 * morphs {@code Circle → SoftBurst}), plus a "Simulate load" button that flips to determinate and
 * sweeps the model 0→100. The gallery rows the standard and contained spinners, a determinate
 * progression, and a size row.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class LoadingShowcasePanels {

  /** The staged mode. */
  enum LoadingMode {
    /** The looping shape-morph spinner. */
    INDETERMINATE,
    /** The progress-driven Circle→SoftBurst morph. */
    DETERMINATE
  }

  private LoadingShowcasePanels() {}

  /** Builds the interactive Workbench (live indicator + shared-model slider + code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaSelectField<LoadingMode> modeBox = ElwhaSelectField.outlined("Mode");
    modeBox.setOptions(List.of(LoadingMode.values()));
    modeBox.setSelectedValue(LoadingMode.INDETERMINATE);
    final ElwhaSwitch containedSwitch = new ElwhaSwitch();
    containedSwitch.setLabel("Contained");
    final ElwhaSelectField<Integer> sizeBox = ElwhaSelectField.outlined("Indicator size px");
    sizeBox.setOptions(List.of(24, 38, 56, 80));
    sizeBox.setSelectedValue(56);

    final DefaultBoundedRangeModel model = new DefaultBoundedRangeModel(40, 0, 0, 100);
    final ElwhaSlider valueSlider = new ElwhaSlider(model);
    valueSlider.setLabel("Value (determinate)");
    final ElwhaButton simulate = ElwhaButton.filledTonalButton("Simulate load");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Loading");
    controls.addControl("", modeBox);
    controls.addControl("", containedSwitch);
    controls.addControl("", sizeBox);
    controls.addSection("Value (shared model)");
    controls.addControl("", valueSlider);
    controls.addControl("", simulate);

    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setOpaque(false);
    stage.setPreferredSize(new Dimension(520, 220));
    stage.setName("loadingShowcaseStage");
    workbench.setStage(stage);

    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator(model);
    stage.add(indicator, new GridBagConstraints());

    final Runnable apply =
        () -> {
          final boolean contained = containedSwitch.isSelected();
          indicator.setIndeterminate(
              orDefault(modeBox.getSelectedValue(), LoadingMode.INDETERMINATE)
                  == LoadingMode.INDETERMINATE);
          indicator.setContained(contained);
          indicator.setIndicatorColorRole(
              contained ? ColorRole.ON_PRIMARY_CONTAINER : ColorRole.PRIMARY);
          indicator.setContainerColorRole(ColorRole.PRIMARY_CONTAINER);
          final int size = orDefault(sizeBox.getSelectedValue(), 56);
          indicator.setIndicatorSize(size);
          indicator.setContainerSize(Math.round(size * 48f / 38f));
          stage.revalidate();
          stage.repaint();
          workbench.setCode(renderCode(indicator.isIndeterminate(), contained, size));
        };

    modeBox.addSelectionChangeListener(v -> apply.run());
    containedSwitch.addActionListener(e -> apply.run());
    sizeBox.addSelectionChangeListener(v -> apply.run());

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
          modeBox.setSelectedValue(LoadingMode.DETERMINATE);
          apply.run();
          model.setValue(0);
          loader.restart();
        });

    apply.run();
    return workbench;
  }

  /** Builds the state gallery — standard, contained, determinate progression, and a size row. */
  static JComponent buildGallery() {
    final JPanel stack = new JPanel();
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
    stack.setOpaque(false);
    stack.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    final JPanel configs = new JPanel(new FlowLayout(FlowLayout.LEADING, 28, 4));
    configs.setOpaque(false);
    configs.add(cell("standard", standardIndeterminate(56)));
    configs.add(cell("contained", containedIndeterminate(56)));
    stack.add(galleryRow("Indeterminate — standard & contained", configs));

    final JPanel det = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 4));
    det.setOpaque(false);
    for (final int v : new int[] {0, 25, 50, 75, 100}) {
      det.add(cell(v + "%", determinate(v, 56)));
    }
    det.add(cell("contained 60%", containedDeterminate(60, 56)));
    stack.add(galleryRow("Determinate — Circle → SoftBurst across progress", det));

    final JPanel sizes = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 4));
    sizes.setOpaque(false);
    for (final int s : new int[] {24, 38, 56, 80}) {
      sizes.add(cell(s + "px", standardIndeterminate(s)));
    }
    stack.add(galleryRow("Sizes", sizes));

    stack.add(Box.createVerticalGlue());
    return stack;
  }

  private static ElwhaLoadingIndicator standardIndeterminate(final int size) {
    final ElwhaLoadingIndicator i = new ElwhaLoadingIndicator();
    i.setIndicatorSize(size);
    return i;
  }

  private static ElwhaLoadingIndicator containedIndeterminate(final int size) {
    final ElwhaLoadingIndicator i = ElwhaLoadingIndicator.contained();
    i.setIndicatorSize(size);
    i.setContainerSize(Math.round(size * 48f / 38f));
    return i;
  }

  private static ElwhaLoadingIndicator determinate(final int value, final int size) {
    final ElwhaLoadingIndicator i = ElwhaLoadingIndicator.determinate();
    i.setIndicatorSize(size);
    i.setValue(value);
    return i;
  }

  private static ElwhaLoadingIndicator containedDeterminate(final int value, final int size) {
    final ElwhaLoadingIndicator i = ElwhaLoadingIndicator.containedDeterminate();
    i.setIndicatorSize(size);
    i.setContainerSize(Math.round(size * 48f / 38f));
    i.setValue(value);
    return i;
  }

  private static JComponent cell(final String caption, final JComponent content) {
    final JPanel cell = new JPanel(new java.awt.BorderLayout(0, 6));
    cell.setOpaque(false);
    final JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    center.setOpaque(false);
    center.add(content);
    cell.add(center, java.awt.BorderLayout.CENTER);
    cell.add(new JLabel(caption, SwingConstants.CENTER), java.awt.BorderLayout.SOUTH);
    return cell;
  }

  private static JComponent galleryRow(final String caption, final JComponent content) {
    final JPanel row = new JPanel(new java.awt.BorderLayout(0, 8));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
    row.add(new JLabel(caption), java.awt.BorderLayout.NORTH);
    final JPanel host = new JPanel(new java.awt.BorderLayout());
    host.setOpaque(false);
    host.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 48));
    host.add(content, java.awt.BorderLayout.CENTER);
    row.add(host, java.awt.BorderLayout.CENTER);
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    return row;
  }

  private static String renderCode(
      final boolean indeterminate, final boolean contained, final int size) {
    final StringBuilder code = new StringBuilder();
    if (contained && indeterminate) {
      code.append("ElwhaLoadingIndicator i = ElwhaLoadingIndicator.contained();\n");
    } else if (contained) {
      code.append("ElwhaLoadingIndicator i = ElwhaLoadingIndicator.containedDeterminate();\n");
    } else if (indeterminate) {
      code.append("ElwhaLoadingIndicator i = new ElwhaLoadingIndicator();\n");
    } else {
      code.append("ElwhaLoadingIndicator i = ElwhaLoadingIndicator.determinate();\n");
    }
    code.append("i.setIndicatorSize(").append(size).append(");\n");
    if (contained) {
      code.append("i.setContainerSize(").append(Math.round(size * 48f / 38f)).append(");\n");
    }
    if (!indeterminate) {
      code.append("i.setValue(progress); // 0..100, morphs Circle → SoftBurst\n");
    }
    code.append("i.getAccessibleContext().setAccessibleName(\"Loading…\");\n");
    return code.toString();
  }

  private static <T> T orDefault(final T value, final T fallback) {
    return value != null ? value : fallback;
  }
}
