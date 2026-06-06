package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Elwha Showcase leaf for {@link ElwhaTextField} — a {@link ComponentWorkbench} stage with live
 * controls over every knob (variant, label, placeholder, prefix/suffix, icons, supporting/error
 * text, required, read-only) and a Gallery of the variant&#215;state matrix plus the slot
 * configurations.
 *
 * <p>The controls dogfood Elwha components: the variant picker is an {@link ElwhaButtonGroup}, the
 * boolean toggles are {@code SELECTABLE} {@link ElwhaButton}s, and every text input is itself an
 * {@link ElwhaTextField}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class TextFieldShowcasePanels {

  private TextFieldShowcasePanels() {}

  static JComponent buildComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Variants × states", buildStateMatrix()),
                gallerySection("Slots & affixes", buildSlotsGallery()),
                gallerySection("Multi-line & text area", buildMultilineGallery()))));
    return tabs;
  }

  private static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaButtonGroup variantGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS);
    variantGroup.add(new ElwhaButton("Filled"));
    variantGroup.add(new ElwhaButton("Outlined"));
    variantGroup.setSelectedIndex(0);

    final ElwhaTextField labelCtl = textControl("Label");
    final ElwhaTextField placeholderCtl = textControl("");
    final ElwhaTextField prefillCtl = textControl("");
    final ElwhaTextField prefixCtl = textControl("");
    final ElwhaTextField suffixCtl = textControl("");
    final ElwhaTextField supportingCtl = textControl("");
    final ElwhaTextField errorTextCtl = textControl("Something isn't right");

    final ElwhaButton leadingToggle = toggle("Leading icon");
    final ElwhaButton trailingToggle = toggle("Trailing clear button");
    final ElwhaButton requiredToggle = toggle("Required");
    final ElwhaButton errorToggle = toggle("Error");
    final ElwhaButton readOnlyToggle = toggle("Read-only");
    final ElwhaButton enabledToggle = toggle("Enabled");
    enabledToggle.setSelected(true);

    final ElwhaButtonGroup modeGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS);
    modeGroup.add(new ElwhaButton("Single"));
    modeGroup.add(new ElwhaButton("Multi-line"));
    modeGroup.add(new ElwhaButton("Text area"));
    modeGroup.setSelectedIndex(0);

    final int[] rows = {3};
    final JLabel rowsValue = new JLabel(String.valueOf(rows[0]));
    final ElwhaIconButton rowsDown =
        new ElwhaIconButton(MaterialIcons.remove())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(IconButtonSize.S);
    final ElwhaIconButton rowsUp =
        new ElwhaIconButton(MaterialIcons.add())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(IconButtonSize.S);
    final JPanel rowsStepper = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    rowsStepper.setOpaque(false);
    rowsStepper.add(rowsDown);
    rowsStepper.add(rowsValue);
    rowsStepper.add(rowsUp);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Content");
    controls.addControl("Variant", variantGroup);
    controls.addControl("Label", labelCtl);
    controls.addControl("Placeholder", placeholderCtl);
    controls.addControl("Prefill text", prefillCtl);
    controls.addControl("Prefix", prefixCtl);
    controls.addControl("Suffix", suffixCtl);
    controls.addControl("Supporting text", supportingCtl);
    controls.addSection("Editor");
    controls.addControl("Input mode", modeGroup);
    controls.addControl("Text-area rows", rowsStepper);
    controls.addSection("Slots");
    controls.addControl("", leadingToggle);
    controls.addControl("", trailingToggle);
    controls.addControl("", requiredToggle);
    controls.addSection("State");
    controls.addControl("", errorToggle);
    controls.addControl("Error text", errorTextCtl);
    controls.addControl("", readOnlyToggle);
    controls.addControl("", enabledToggle);

    final Runnable apply =
        () -> {
          final ElwhaTextField.Variant variant =
              variantGroup.getSelectedIndex() == 0
                  ? ElwhaTextField.Variant.FILLED
                  : ElwhaTextField.Variant.OUTLINED;
          final ElwhaTextField field = new ElwhaTextField(variant, labelCtl.getText());
          final ElwhaTextField.InputMode mode =
              ElwhaTextField.InputMode.values()[modeGroup.getSelectedIndex()];
          field.setInputMode(mode);
          field.setRows(rows[0]);
          field.setPlaceholder(placeholderCtl.getText());
          field.setPrefixText(prefixCtl.getText());
          field.setSuffixText(suffixCtl.getText());
          field.setSupportingText(supportingCtl.getText());
          if (!prefillCtl.getText().isEmpty()) {
            field.setText(prefillCtl.getText());
          }
          if (leadingToggle.isSelected()) {
            field.setLeadingIcon(MaterialIcons.info());
          }
          if (trailingToggle.isSelected()) {
            final ElwhaIconButton clear =
                new ElwhaIconButton(MaterialIcons.close())
                    .setVariant(IconButtonVariant.STANDARD)
                    .setButtonSize(IconButtonSize.M);
            clear.addActionListener(event -> field.setText(""));
            field.setTrailingIconButton(clear);
          }
          field.setRequired(requiredToggle.isSelected());
          field.setError(errorToggle.isSelected());
          if (errorToggle.isSelected()) {
            field.setErrorText(errorTextCtl.getText());
          }
          field.setReadOnly(readOnlyToggle.isSelected());
          field.setEnabled(enabledToggle.isSelected());
          workbench.setStage(field);
          workbench.setCode(
              renderCode(
                  variant,
                  mode,
                  rows[0],
                  labelCtl.getText(),
                  placeholderCtl.getText(),
                  prefixCtl.getText(),
                  suffixCtl.getText(),
                  supportingCtl.getText(),
                  leadingToggle.isSelected(),
                  trailingToggle.isSelected(),
                  requiredToggle.isSelected(),
                  errorToggle.isSelected(),
                  errorTextCtl.getText(),
                  readOnlyToggle.isSelected(),
                  enabledToggle.isSelected()));
        };

    variantGroup.addSelectionListener(group -> apply.run());
    modeGroup.addSelectionListener(group -> apply.run());
    rowsDown.addActionListener(
        event -> {
          rows[0] = Math.max(1, rows[0] - 1);
          rowsValue.setText(String.valueOf(rows[0]));
          apply.run();
        });
    rowsUp.addActionListener(
        event -> {
          rows[0] = rows[0] + 1;
          rowsValue.setText(String.valueOf(rows[0]));
          apply.run();
        });
    for (final ElwhaTextField ctl :
        new ElwhaTextField[] {
          labelCtl, placeholderCtl, prefillCtl, prefixCtl, suffixCtl, supportingCtl, errorTextCtl
        }) {
      onChange(ctl, apply);
    }
    for (final ElwhaButton tgl :
        new ElwhaButton[] {
          leadingToggle, trailingToggle, requiredToggle, errorToggle, readOnlyToggle, enabledToggle
        }) {
      tgl.addActionListener(event -> apply.run());
    }
    apply.run();
    return workbench;
  }

  private static String renderCode(
      final ElwhaTextField.Variant variant,
      final ElwhaTextField.InputMode mode,
      final int rows,
      final String label,
      final String placeholder,
      final String prefix,
      final String suffix,
      final String supporting,
      final boolean leading,
      final boolean trailing,
      final boolean required,
      final boolean error,
      final String errorText,
      final boolean readOnly,
      final boolean enabled) {
    final StringBuilder code = new StringBuilder(360);
    code.append("ElwhaTextField field = ElwhaTextField.")
        .append(variant == ElwhaTextField.Variant.FILLED ? "filled" : "outlined")
        .append("(\"")
        .append(label)
        .append("\");");
    if (mode != ElwhaTextField.InputMode.SINGLE_LINE) {
      code.append("\nfield.setInputMode(ElwhaTextField.InputMode.")
          .append(mode.name())
          .append(");");
    }
    if (mode == ElwhaTextField.InputMode.TEXT_AREA) {
      code.append("\nfield.setRows(").append(rows).append(");");
    }
    if (!placeholder.isEmpty()) {
      code.append("\nfield.setPlaceholder(\"").append(placeholder).append("\");");
    }
    if (!prefix.isEmpty()) {
      code.append("\nfield.setPrefixText(\"").append(prefix).append("\");");
    }
    if (!suffix.isEmpty()) {
      code.append("\nfield.setSuffixText(\"").append(suffix).append("\");");
    }
    if (!supporting.isEmpty()) {
      code.append("\nfield.setSupportingText(\"").append(supporting).append("\");");
    }
    if (leading) {
      code.append("\nfield.setLeadingIcon(MaterialIcons.info());");
    }
    if (trailing) {
      code.append("\nfield.setTrailingIconButton(new ElwhaIconButton(MaterialIcons.close()));");
    }
    if (required) {
      code.append("\nfield.setRequired(true);");
    }
    if (error) {
      code.append("\nfield.setError(true);");
      if (!errorText.isEmpty()) {
        code.append("\nfield.setErrorText(\"").append(errorText).append("\");");
      }
    }
    if (readOnly) {
      code.append("\nfield.setReadOnly(true);");
    }
    if (!enabled) {
      code.append("\nfield.setEnabled(false);");
    }
    return code.toString();
  }

  // ---- Gallery --------------------------------------------------------------

  private static JComponent buildStateMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.WEST;

    gbc.gridy = 0;
    gbc.gridx = 1;
    grid.add(bold(new JLabel("Filled")), gbc);
    gbc.gridx = 2;
    grid.add(bold(new JLabel("Outlined")), gbc);

    int row = 1;
    row = matrixRow(grid, gbc, row, "Enabled", false, false, false, false);
    row = matrixRow(grid, gbc, row, "Populated", false, false, false, true);
    row = matrixRow(grid, gbc, row, "Error", true, false, false, true);
    row = matrixRow(grid, gbc, row, "Disabled", false, true, false, true);
    matrixRow(grid, gbc, row, "Read-only", false, false, true, true);
    return grid;
  }

  private static int matrixRow(
      final JPanel grid,
      final GridBagConstraints gbc,
      final int row,
      final String caption,
      final boolean error,
      final boolean disabled,
      final boolean readOnly,
      final boolean populated) {
    gbc.gridy = row;
    gbc.gridx = 0;
    grid.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    grid.add(matrixField(ElwhaTextField.Variant.FILLED, error, disabled, readOnly, populated), gbc);
    gbc.gridx = 2;
    grid.add(
        matrixField(ElwhaTextField.Variant.OUTLINED, error, disabled, readOnly, populated), gbc);
    return row + 1;
  }

  private static ElwhaTextField matrixField(
      final ElwhaTextField.Variant variant,
      final boolean error,
      final boolean disabled,
      final boolean readOnly,
      final boolean populated) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Label");
    if (populated) {
      field.setText("Value");
    }
    if (error) {
      field.setError(true);
      field.setErrorText("Error text");
    }
    field.setReadOnly(readOnly);
    field.setEnabled(!disabled);
    return field;
  }

  private static JComponent buildSlotsGallery() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = 0;

    final ElwhaTextField leading = ElwhaTextField.outlined("Leading icon");
    leading.setLeadingIcon(MaterialIcons.info());

    final ElwhaTextField trailing = ElwhaTextField.filled("Trailing clear");
    trailing.setText("Clearable");
    final ElwhaIconButton clear =
        new ElwhaIconButton(MaterialIcons.close())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(IconButtonSize.M);
    clear.addActionListener(event -> trailing.setText(""));
    trailing.setTrailingIconButton(clear);

    final ElwhaTextField affixes = ElwhaTextField.filled("Amount");
    affixes.setText("1.43");
    affixes.setPrefixText("$");
    affixes.setSuffixText("USD");

    final ElwhaTextField supporting = ElwhaTextField.outlined("With supporting");
    supporting.setSupportingText("Helper text below the field");

    final ElwhaTextField required = ElwhaTextField.filled("Required");
    required.setRequired(true);

    slotsRow(grid, gbc, "Leading icon", leading);
    slotsRow(grid, gbc, "Trailing clear button", trailing);
    slotsRow(grid, gbc, "Prefix + suffix", affixes);
    slotsRow(grid, gbc, "Supporting text", supporting);
    slotsRow(grid, gbc, "Required asterisk", required);
    return grid;
  }

  private static void slotsRow(
      final JPanel grid,
      final GridBagConstraints gbc,
      final String caption,
      final ElwhaTextField field) {
    gbc.gridx = 0;
    grid.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    grid.add(field, gbc);
    gbc.gridy++;
  }

  private static JComponent buildMultilineGallery() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.gridy = 0;

    final ElwhaTextField autoGrow = ElwhaTextField.outlined("Notes (auto-grow)");
    autoGrow.setInputMode(ElwhaTextField.InputMode.MULTI_LINE);
    autoGrow.setText(
        "Multi-line grows with content,\nshifting the layout down\nas lines are added.");
    autoGrow.setSupportingText("Grows as you type");

    final ElwhaTextField textArea = ElwhaTextField.filled("Bio (text area)");
    textArea.setInputMode(ElwhaTextField.InputMode.TEXT_AREA);
    textArea.setRows(3);
    textArea.setText(
        "A fixed three-row text area that\n"
            + "scrolls its content internally\n"
            + "rather than growing the field\n"
            + "when the text overflows.");

    slotsRow(grid, gbc, "Multi-line (auto-grow)", autoGrow);
    slotsRow(grid, gbc, "Text area (fixed rows)", textArea);
    return grid;
  }

  // ---- Helpers --------------------------------------------------------------

  private static ElwhaTextField textControl(final String initial) {
    final ElwhaTextField field = ElwhaTextField.outlined("");
    field.setText(initial);
    return field;
  }

  private static ElwhaButton toggle(final String label) {
    return ElwhaButton.outlinedButton(label)
        .setInteractionMode(ButtonInteractionMode.SELECTABLE)
        .setButtonSize(ButtonSize.XS);
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

  private static JLabel bold(final JLabel label) {
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  private static JComponent stack(final JComponent... parts) {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    for (final JComponent part : parts) {
      part.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      column.add(part);
    }
    column.add(Box.createVerticalStrut(8));
    return column;
  }

  private static JComponent gallerySection(final String title, final JComponent body) {
    final JPanel section = new JPanel(new BorderLayout());
    section.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    final JLabel heading = new JLabel(title);
    heading.setFont(heading.getFont().deriveFont(Font.BOLD));
    heading.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
    section.add(heading, BorderLayout.NORTH);
    section.add(body, BorderLayout.CENTER);
    return section;
  }

  private static JScrollPane scroll(final Component view) {
    final JScrollPane pane = new JScrollPane(view);
    pane.setBorder(BorderFactory.createEmptyBorder());
    pane.getVerticalScrollBar().setUnitIncrement(16);
    return pane;
  }
}
