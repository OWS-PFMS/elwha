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
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Elwha Showcase leaf for {@link ElwhaSelectField} — a {@link ComponentWorkbench} stage with
 * live controls (variant, label, supporting / placeholder text, option count, error, read-only,
 * enabled, the Phase-2 editable / free-text policy toggles, plus the Phase-3 multi-select toggle)
 * and a live "selected value(s)" readout, and a Gallery of the variant&#215;state matrix, a long
 * scrolling option list, a pre-selected value, the editable filter-as-you-type combo, and the
 * multi-select summary display.
 *
 * <p>The controls dogfood Elwha components: the variant picker is an {@link ElwhaButtonGroup}, the
 * boolean toggles are {@code SELECTABLE} {@link ElwhaButton}s, the option-count stepper is a pair
 * of {@link ElwhaIconButton}s, and every text input is itself an {@link ElwhaTextField}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class SelectFieldShowcasePanels {

  private static final List<String> POOL =
      List.of("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune");

  private SelectFieldShowcasePanels() {}

  static JComponent buildComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Variants × states", buildStateMatrix()),
                gallerySection("Long option list (scrolls)", buildLongListGallery()),
                gallerySection("Pre-selected value", buildPreselectedGallery()),
                gallerySection("Filtering (editable combo)", buildFilteringGallery()),
                gallerySection("Multi-select (summary display)", buildMultiSelectGallery()))));
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

    final ElwhaTextField labelCtl = textControl("Planet");
    final ElwhaTextField supportingCtl = textControl("");
    final ElwhaTextField placeholderCtl = textControl("");
    final ElwhaTextField errorTextCtl = textControl("Pick an option");

    final int[] count = {4};
    final JLabel countValue = new JLabel(String.valueOf(count[0]));
    final ElwhaIconButton countDown =
        new ElwhaIconButton(MaterialIcons.remove())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(IconButtonSize.S);
    final ElwhaIconButton countUp =
        new ElwhaIconButton(MaterialIcons.add())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(IconButtonSize.S);
    final JPanel countStepper = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    countStepper.setOpaque(false);
    countStepper.add(countDown);
    countStepper.add(countValue);
    countStepper.add(countUp);

    final ElwhaButton errorToggle = toggle("Error");
    final ElwhaButton readOnlyToggle = toggle("Read-only");
    final ElwhaButton enabledToggle = toggle("Enabled");
    enabledToggle.setSelected(true);
    final ElwhaButton editableToggle = toggle("Editable");
    final ElwhaButton freeTextToggle = toggle("Free text");
    final ElwhaButton multiToggle = toggle("Multi-select");

    final JLabel readout = new JLabel("Selected value: —");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Content");
    controls.addControl("Variant", variantGroup);
    controls.addControl("Label", labelCtl);
    controls.addControl("Supporting text", supportingCtl);
    controls.addControl("Placeholder", placeholderCtl);
    controls.addControl("Option count", countStepper);
    controls.addSection("State");
    controls.addControl("", errorToggle);
    controls.addControl("Error text", errorTextCtl);
    controls.addControl("", readOnlyToggle);
    controls.addControl("", enabledToggle);
    controls.addSection("Editable combo");
    controls.addControl("", editableToggle);
    controls.addControl("", freeTextToggle);
    controls.addSection("Multi-select");
    controls.addControl("", multiToggle);
    controls.addSection("Selection");
    controls.addControl("", readout);

    final ElwhaSelectField<?>[] live = {null};

    final Runnable apply =
        () -> {
          final ElwhaTextField.Variant variant =
              variantGroup.getSelectedIndex() == 0
                  ? ElwhaTextField.Variant.FILLED
                  : ElwhaTextField.Variant.OUTLINED;
          final List<String> options = POOL.subList(0, count[0]);
          @SuppressWarnings("unchecked")
          final List<String> carried =
              live[0] == null
                  ? List.of()
                  : ((ElwhaSelectField<String>) live[0]).getSelectedValues();

          final ElwhaSelectField<String> select =
              new ElwhaSelectField<>(variant, labelCtl.getText());
          select.setOptions(options);
          select.setSupportingText(supportingCtl.getText());
          select.setPlaceholder(placeholderCtl.getText());
          if (errorToggle.isSelected()) {
            select.setError(true);
            select.setErrorText(errorTextCtl.getText());
          }
          select.setReadOnly(readOnlyToggle.isSelected());
          select.setEnabled(enabledToggle.isSelected());
          select.setEditable(editableToggle.isSelected());
          select.setFreeTextAllowed(freeTextToggle.isSelected());
          select.setMultiSelect(multiToggle.isSelected());
          select.setSelectedValues(carried);
          select.addSelectionChangeListener(
              value ->
                  readout.setText(
                      value != null
                          ? "Selected value: " + value
                          : select.getText().isEmpty()
                              ? "Selected value: —"
                              : "Free text: \"" + select.getText() + "\""));
          select.addMultiSelectionChangeListener(
              values ->
                  readout.setText(
                      values.isEmpty()
                          ? "Selected values: —"
                          : "Selected values: " + String.join(", ", values)));
          readout.setText(
              select.isMultiSelect()
                  ? (select.getSelectedValues().isEmpty()
                      ? "Selected values: —"
                      : "Selected values: " + String.join(", ", select.getSelectedValues()))
                  : "Selected value: "
                      + (select.getSelectedValue() == null ? "—" : select.getSelectedValue()));

          live[0] = select;
          workbench.setStage(topAligned(select));
          workbench.setCode(
              renderCode(
                  variant,
                  labelCtl.getText(),
                  options.size(),
                  supportingCtl.getText(),
                  placeholderCtl.getText(),
                  errorToggle.isSelected(),
                  errorTextCtl.getText(),
                  readOnlyToggle.isSelected(),
                  enabledToggle.isSelected(),
                  editableToggle.isSelected(),
                  freeTextToggle.isSelected(),
                  multiToggle.isSelected()));
        };

    variantGroup.addSelectionListener(group -> apply.run());
    countDown.addActionListener(
        event -> {
          count[0] = Math.max(1, count[0] - 1);
          countValue.setText(String.valueOf(count[0]));
          apply.run();
        });
    countUp.addActionListener(
        event -> {
          count[0] = Math.min(POOL.size(), count[0] + 1);
          countValue.setText(String.valueOf(count[0]));
          apply.run();
        });
    for (final ElwhaTextField ctl :
        new ElwhaTextField[] {labelCtl, supportingCtl, placeholderCtl, errorTextCtl}) {
      onChange(ctl, apply);
    }
    for (final ElwhaButton tgl : new ElwhaButton[] {errorToggle, readOnlyToggle, enabledToggle}) {
      tgl.addActionListener(event -> apply.run());
    }
    // Editable and multi-select are mutually exclusive on the component (each setter forces the
    // other off) — mirror that in the toggle chips so the controls never lie about the live state.
    editableToggle.addActionListener(
        event -> {
          if (editableToggle.isSelected()) {
            multiToggle.setSelected(false);
          }
          apply.run();
        });
    freeTextToggle.addActionListener(
        event -> {
          if (freeTextToggle.isSelected()) {
            editableToggle.setSelected(true);
            multiToggle.setSelected(false);
          }
          apply.run();
        });
    multiToggle.addActionListener(
        event -> {
          if (multiToggle.isSelected()) {
            editableToggle.setSelected(false);
            freeTextToggle.setSelected(false);
          }
          apply.run();
        });
    apply.run();
    return workbench;
  }

  private static String renderCode(
      final ElwhaTextField.Variant variant,
      final String label,
      final int optionCount,
      final String supporting,
      final String placeholder,
      final boolean error,
      final String errorText,
      final boolean readOnly,
      final boolean enabled,
      final boolean editable,
      final boolean freeText,
      final boolean multi) {
    final StringBuilder code = new StringBuilder(420);
    code.append("ElwhaSelectField<String> select = ElwhaSelectField.")
        .append(variant == ElwhaTextField.Variant.FILLED ? "filled" : "outlined")
        .append("(\"")
        .append(label)
        .append("\");\n");
    code.append("select.setOptions(List.of(");
    for (int i = 0; i < optionCount; i++) {
      code.append(i == 0 ? "" : ", ").append('"').append(POOL.get(i)).append('"');
    }
    code.append("));\n");
    if (!supporting.isEmpty()) {
      code.append("select.setSupportingText(\"").append(supporting).append("\");\n");
    }
    if (!placeholder.isEmpty()) {
      code.append("select.setPlaceholder(\"").append(placeholder).append("\");\n");
    }
    if (error) {
      code.append("select.setError(true);\n");
      code.append("select.setErrorText(\"").append(errorText).append("\");\n");
    }
    if (readOnly) {
      code.append("select.setReadOnly(true);\n");
    }
    if (!enabled) {
      code.append("select.setEnabled(false);\n");
    }
    if (editable) {
      code.append("select.setEditable(true);\n");
    }
    if (freeText) {
      code.append("select.setFreeTextAllowed(true);\n");
    }
    if (multi) {
      code.append("select.setMultiSelect(true);\n");
      code.append("select.addMultiSelectionChangeListener(values -> /* … */);");
    } else {
      code.append("select.addSelectionChangeListener(value -> /* … */);");
    }
    return code.toString();
  }

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
    row = matrixRow(grid, gbc, row, "Selected", false, false, false, true);
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
      final boolean selected) {
    gbc.gridy = row;
    gbc.gridx = 0;
    grid.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    grid.add(matrixField(ElwhaTextField.Variant.FILLED, error, disabled, readOnly, selected), gbc);
    gbc.gridx = 2;
    grid.add(
        matrixField(ElwhaTextField.Variant.OUTLINED, error, disabled, readOnly, selected), gbc);
    return row + 1;
  }

  private static ElwhaSelectField<String> matrixField(
      final ElwhaTextField.Variant variant,
      final boolean error,
      final boolean disabled,
      final boolean readOnly,
      final boolean selected) {
    final ElwhaSelectField<String> select = new ElwhaSelectField<>(variant, "Planet");
    select.setOptions(POOL.subList(0, 4));
    if (selected) {
      select.setSelectedValue("Earth");
    }
    if (error) {
      select.setError(true);
      select.setErrorText("Error text");
    }
    select.setReadOnly(readOnly);
    select.setEnabled(!disabled);
    return select;
  }

  private static JComponent buildLongListGallery() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = 0;

    final ElwhaSelectField<String> longList = ElwhaSelectField.outlined("Country");
    longList.setOptions(
        List.of(
            "Argentina",
            "Brazil",
            "Canada",
            "Denmark",
            "Egypt",
            "France",
            "Germany",
            "Hungary",
            "Iceland",
            "Japan",
            "Kenya",
            "Latvia",
            "Mexico",
            "Norway",
            "Oman",
            "Peru",
            "Qatar",
            "Romania",
            "Spain",
            "Thailand"));
    longList.setSupportingText("The menu scrolls when the list is long");

    galleryRow(grid, gbc, "Twenty options (scrolling menu)", longList);
    return grid;
  }

  private static JComponent buildPreselectedGallery() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = 0;

    final ElwhaSelectField<String> filled = ElwhaSelectField.filled("Planet");
    filled.setOptions(POOL.subList(0, 5));
    filled.setSelectedValue("Mars");

    final ElwhaSelectField<String> outlined = ElwhaSelectField.outlined("Planet");
    outlined.setOptions(POOL.subList(0, 5));
    outlined.setSelectedValue("Jupiter");

    galleryRow(grid, gbc, "Filled, pre-selected (Mars)", filled);
    galleryRow(grid, gbc, "Outlined, pre-selected (Jupiter)", outlined);
    return grid;
  }

  private static JComponent buildFilteringGallery() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = 0;

    final ElwhaSelectField<String> filtering = ElwhaSelectField.outlined("Fruit");
    filtering.setOptions(
        List.of(
            "Apple",
            "Apricot",
            "Banana",
            "Blueberry",
            "Cherry",
            "Cranberry",
            "Elderberry",
            "Mango"));
    filtering.setEditable(true);
    filtering.setSupportingText("Type to filter — try \"berry\"");

    final ElwhaSelectField<String> freeText = ElwhaSelectField.outlined("Tag");
    freeText.setOptions(List.of("bug", "feature", "docs", "ci"));
    freeText.setEditable(true);
    freeText.setFreeTextAllowed(true);
    freeText.setSupportingText("Free text allowed — commit anything with Enter");

    galleryRow(grid, gbc, "Editable combo (constrained)", filtering);
    galleryRow(grid, gbc, "Editable combo, free text allowed", freeText);
    return grid;
  }

  private static JComponent buildMultiSelectGallery() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(12, 20, 20, 20));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = 0;

    final ElwhaSelectField<String> joined = ElwhaSelectField.outlined("Toppings");
    joined.setOptions(
        List.of("Mushroom", "Pepperoni", "Onion", "Olive", "Basil", "Pineapple", "Spinach"));
    joined.setMultiSelect(true);
    joined.setSelectedValues(List.of("Pepperoni", "Basil"));
    joined.setSupportingText("Toggling keeps the menu open");

    final ElwhaSelectField<String> overflow = ElwhaSelectField.filled("Weekdays");
    overflow.setOptions(
        List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
    overflow.setMultiSelect(true);
    overflow.setSelectedValues(List.of("Monday", "Tuesday", "Thursday", "Friday", "Saturday"));
    overflow.setSupportingText("Past the summary limit — the count form");

    galleryRow(grid, gbc, "Pre-checked values (joined summary)", joined);
    galleryRow(grid, gbc, "Wide selection (count form)", overflow);
    return grid;
  }

  private static void galleryRow(
      final JPanel grid,
      final GridBagConstraints gbc,
      final String caption,
      final ElwhaSelectField<String> select) {
    gbc.gridx = 0;
    grid.add(new JLabel(caption), gbc);
    gbc.gridx = 1;
    grid.add(select, gbc);
    gbc.gridy++;
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

  /**
   * Wraps the select at its preferred height so the Workbench stage doesn't stretch it vertically.
   */
  private static JComponent topAligned(final JComponent component) {
    final JPanel holder = new JPanel(new BorderLayout());
    holder.setOpaque(false);
    holder.add(component, BorderLayout.NORTH);
    return holder;
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
