package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.colorpicker.ElwhaColorPicker;
import com.owspfm.elwha.colorpicker.ElwhaColorPickerDialog;
import com.owspfm.elwha.colorpicker.PickerMode;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaColorPicker} + {@link ElwhaColorPickerDialog}
 * (story #490): a {@link ComponentWorkbench} stage hosting one persistent live picker with
 * mode-subset, preset-color, alpha, enabled, and supporting-text knobs plus a dialog launcher and a
 * generated construction snippet — every knob an Elwha control (the #424 dogfood direction); and a
 * gallery of single-mode pickers, the alpha-enabled spectrum, a populated recent row, and the
 * disabled treatment.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ColorPickerShowcasePanels {

  private static final Map<String, Color> PRESETS = presets();

  private ColorPickerShowcasePanels() {}

  private static Map<String, Color> presets() {
    final Map<String, Color> map = new LinkedHashMap<>();
    map.put("Deep Orange 400", new Color(0xFF7043));
    map.put("Blue 400", new Color(0x42A5F5));
    map.put("Green 600", new Color(0x43A047));
    map.put("Purple 500", new Color(0x9C27B0));
    map.put("Amber 300", new Color(0xFFD54F));
    map.put("White", Color.WHITE);
    map.put("Black", Color.BLACK);
    return map;
  }

  /** Builds the interactive Workbench (live picker stage + all-Elwha control rail + code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaColorPicker picker = new ElwhaColorPicker(PRESETS.get("Deep Orange 400"));

    final ElwhaCheckbox swatchesBox = new ElwhaCheckbox("Swatches");
    final ElwhaCheckbox spectrumBox = new ElwhaCheckbox("Spectrum");
    final ElwhaCheckbox slidersBox = new ElwhaCheckbox("Sliders");
    swatchesBox.setChecked(true);
    spectrumBox.setChecked(true);
    slidersBox.setChecked(true);

    final ElwhaSelectField<String> presetField = new ElwhaSelectField<>("Preset");
    presetField.setOptions(List.copyOf(PRESETS.keySet()));
    presetField.setSelectedValue("Deep Orange 400");

    final ElwhaCheckbox alphaBox = new ElwhaCheckbox("Alpha enabled");
    final ElwhaSwitch enabledSwitch = new ElwhaSwitch(true);
    enabledSwitch.setLabel("Enabled");

    final ElwhaTextField supportingField = ElwhaTextField.outlined("Supporting text");
    supportingField.setText("Select color");

    final ElwhaButton dialogButton = ElwhaButton.filledTonalButton("Open dialog");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Modes");
    controls.addControl("", swatchesBox);
    controls.addControl("", spectrumBox);
    controls.addControl("", slidersBox);
    controls.addSection("Color");
    controls.addControl("", presetField);
    controls.addControl("", alphaBox);
    controls.addSection("Context");
    controls.addControl("", enabledSwitch);
    controls.addControl("", supportingField);
    controls.addSection("Dialog");
    controls.addControl("", dialogButton);

    final JLabel readout = new JLabel("getColor() → #FF7043FF (adjusting)", SwingConstants.CENTER);
    final Dimension readoutPref = readout.getPreferredSize();
    readout.setPreferredSize(readoutPref);
    readout.setMaximumSize(readoutPref);
    readout.setText("getColor() → #FF7043");
    picker.addChangeListener(
        e ->
            readout.setText(
                "getColor() → " + hex(picker) + (picker.isAdjusting() ? " (adjusting)" : "")));

    workbench.setStage(stage(picker, readout));

    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    dialog.setTitle("Pick a color");
    dialog.onConfirm(picker::setColor);
    dialogButton.addActionListener(
        e -> {
          dialog.setInitialColor(picker.getColor());
          dialog.setAlphaEnabled(picker.isAlphaEnabled());
          dialog.show(workbench);
        });

    final Runnable apply =
        () -> {
          final List<PickerMode> modes = new ArrayList<>();
          if (swatchesBox.isChecked()) {
            modes.add(PickerMode.SWATCHES);
          }
          if (spectrumBox.isChecked()) {
            modes.add(PickerMode.SPECTRUM);
          }
          if (slidersBox.isChecked()) {
            modes.add(PickerMode.SLIDERS);
          }
          if (modes.isEmpty()) {
            swatchesBox.setChecked(true);
            modes.add(PickerMode.SWATCHES);
          }
          if (!picker.getModes().equals(modes)) {
            picker.setModes(modes.toArray(new PickerMode[0]));
          }
          if (picker.isAlphaEnabled() != alphaBox.isChecked()) {
            picker.setAlphaEnabled(alphaBox.isChecked());
          }
          picker.setEnabled(enabledSwitch.isSelected());
          final String supporting = supportingField.getText().trim();
          picker.setSupportingText(supporting.isEmpty() ? null : supporting);
          workbench.setCode(
              renderCode(modes, alphaBox.isChecked(), enabledSwitch.isSelected(), supporting));
          picker.revalidate();
          picker.repaint();
        };

    swatchesBox.addActionListener(e -> apply.run());
    spectrumBox.addActionListener(e -> apply.run());
    slidersBox.addActionListener(e -> apply.run());
    alphaBox.addActionListener(e -> apply.run());
    enabledSwitch.addActionListener(e -> apply.run());
    presetField.addSelectionChangeListener(
        name -> {
          final Color preset = PRESETS.get(name);
          if (preset != null) {
            picker.setColor(preset);
          }
        });
    if (supportingField.getEditor() instanceof JTextField textField) {
      textField.addActionListener(e -> apply.run());
    }
    apply.run();
    return workbench;
  }

  /** Builds the gallery — single-mode pickers, alpha spectrum, recent row, disabled. */
  static JComponent buildGallery() {
    final JPanel stack = new JPanel();
    stack.setOpaque(false);
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

    final JPanel singles = row();
    singles.add(titled("Swatches only", singleMode(PickerMode.SWATCHES, new Color(0xF44336))));
    singles.add(titled("Spectrum only", singleMode(PickerMode.SPECTRUM, new Color(0x2E7D32))));
    singles.add(titled("Sliders only", singleMode(PickerMode.SLIDERS, new Color(0x336699))));
    stack.add(singles);

    final JPanel second = row();
    final ElwhaColorPicker alphaPicker = new ElwhaColorPicker(new Color(0xF4, 0x43, 0x36, 0x80));
    alphaPicker.setAlphaEnabled(true);
    alphaPicker.setColor(new Color(0xF4, 0x43, 0x36, 0x80));
    alphaPicker.setModes(PickerMode.SPECTRUM);
    alphaPicker.setSupportingText("Alpha enabled");
    second.add(titled("Alpha", alphaPicker));

    final ElwhaColorPicker recentPicker = new ElwhaColorPicker();
    recentPicker.setModes(PickerMode.SWATCHES);
    recentPicker.setSupportingText("Recent row populated");
    for (final Color color : PRESETS.values()) {
      recentPicker.setColor(color);
    }
    second.add(titled("Recent colors", recentPicker));

    final ElwhaColorPicker disabledPicker = new ElwhaColorPicker(new Color(0x7E57C2));
    disabledPicker.setSupportingText("Disabled");
    disabledPicker.setEnabled(false);
    second.add(titled("Disabled", disabledPicker));
    stack.add(second);

    return stack;
  }

  private static ElwhaColorPicker singleMode(final PickerMode mode, final Color color) {
    final ElwhaColorPicker picker = new ElwhaColorPicker(color);
    picker.setModes(mode);
    picker.setSupportingText(null);
    return picker;
  }

  private static JPanel row() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 12));
    row.setOpaque(false);
    row.setAlignmentX(0f);
    return row;
  }

  private static JComponent titled(final String title, final JComponent component) {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    final JLabel label = new JLabel(title);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    label.setAlignmentX(0f);
    component.setAlignmentX(0f);
    panel.add(label);
    panel.add(Box.createVerticalStrut(6));
    panel.add(component);
    return panel;
  }

  private static JComponent stage(final ElwhaColorPicker picker, final JLabel readout) {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    picker.setAlignmentX(0.5f);
    readout.setAlignmentX(0.5f);
    panel.add(Box.createVerticalGlue());
    panel.add(picker);
    panel.add(Box.createVerticalStrut(12));
    panel.add(readout);
    panel.add(Box.createVerticalGlue());
    return panel;
  }

  private static String hex(final ElwhaColorPicker picker) {
    final Color color = picker.getColor();
    final String rgb = String.format("#%06X", color.getRGB() & 0xFFFFFF);
    return picker.isAlphaEnabled() ? rgb + String.format("%02X", color.getAlpha()) : rgb;
  }

  private static String renderCode(
      final List<PickerMode> modes,
      final boolean alpha,
      final boolean enabled,
      final String supporting) {
    final StringBuilder code = new StringBuilder(280);
    code.append("ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF7043));\n");
    if (modes.size() < 3) {
      code.append("picker.setModes(");
      for (int i = 0; i < modes.size(); i++) {
        if (i > 0) {
          code.append(", ");
        }
        code.append("PickerMode.").append(modes.get(i).name());
      }
      code.append(");\n");
    }
    if (alpha) {
      code.append("picker.setAlphaEnabled(true);\n");
    }
    if (!"Select color".equals(supporting)) {
      code.append("picker.setSupportingText(")
          .append(supporting.isEmpty() ? "null" : "\"" + supporting + "\"")
          .append(");\n");
    }
    if (!enabled) {
      code.append("picker.setEnabled(false);\n");
    }
    code.append("picker.addChangeListener(e -> apply(picker.getColor()));\n");
    code.append("// Modal: ElwhaColorPickerDialog.show(parent, \"Pick a color\", current, c -> …)");
    return code.toString();
  }
}
