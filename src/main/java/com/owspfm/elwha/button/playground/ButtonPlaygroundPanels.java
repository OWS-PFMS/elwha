package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonGroup;
import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

/**
 * Reusable panel builders for the {@link ElwhaButton} playground surfaces. Lets the standalone
 * {@link ElwhaButtonPlayground} and the {@code ThemePlayground}'s {@code Button} tab share one
 * canonical implementation of the variant gallery, sizes, toggle-examples, and live-control panels
 * so both entry points stay in lockstep.
 *
 * <p>Mirrors {@code ChipPlaygroundPanels} / {@code IconButtonPlaygroundPanels} / {@code
 * SurfacePlaygroundPanels} — same factored-builder pattern, same reasons (the repo has no JUnit
 * infra; components are validated visually and both playground entry points must not drift).
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ButtonPlaygroundPanels {

  private static final int CELL_GAP = 12;

  private ButtonPlaygroundPanels() {}

  // ----------------------------------------------------- variant gallery

  /**
   * Builds the variant gallery panel: every {@link ButtonVariant} as a row × six visual states as
   * columns (Enabled / Disabled / Hovered / Focused / Pressed / Selected). Hovered and Pressed
   * cells pre-render the state-layer overlay via {@link ElwhaButton#setHovered} / {@link
   * ElwhaButton#setPressed} so the M3 rendering is visible without live interaction; the Focused
   * column requires Tab-to since focus is bound to real focus ownership. The Selected column shows
   * the toggle-selected rendering for the four toggleable variants and a dash for {@link
   * ButtonVariant#TEXT} (which rejects {@code SELECTABLE}). {@link ButtonVariant#FILLED} — the
   * default variant — is flagged in its row label.
   *
   * @return the variant gallery panel
   * @version v0.2.0
   * @since v0.2.0
   */
  public static JPanel buildVariantGalleryPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] columns = {
      "Enabled", "Disabled", "Hovered", "Focused (Tab to)", "Pressed", "Selected"
    };

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Variant \\ State"), gbc);
    for (int c = 0; c < columns.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(columns[c]), gbc);
    }

    int row = 1;
    for (ButtonVariant variant : ButtonVariant.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      final boolean isDefault = variant == ButtonVariant.FILLED;
      matrix.add(rowLabel(variant.name() + (isDefault ? "  (default)" : "")), gbc);
      for (int c = 0; c < columns.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildGalleryCell(variant, c), gbc);
      }
    }
    return matrix;
  }

  private static JComponent buildGalleryCell(final ButtonVariant variant, final int columnIndex) {
    if (columnIndex == 5 && variant == ButtonVariant.TEXT) {
      // TEXT rejects SELECTABLE — no Selected rendering exists.
      return headerLabel("—");
    }
    final ElwhaButton button = new ElwhaButton("Common button").setVariant(variant);
    switch (columnIndex) {
      case 0 -> {
        /* enabled — nothing */
      }
      case 1 -> button.setEnabled(false);
      case 2 -> button.setHovered(true);
      case 3 -> button.setToolTipText("Tab to this button to see the focus border");
      case 4 -> button.setPressed(true);
      case 5 -> {
        button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
        button.setSelected(true);
      }
      default -> {
        /* nothing */
      }
    }
    return button;
  }

  // -------------------------------------------------------------- sizes

  /**
   * Builds the sizes panel: a {@link ButtonSize} × {@link ButtonShape} matrix at the {@link
   * ButtonVariant#FILLED} variant (5 sizes × 2 shapes), plus a with-icon row and a per-size spec
   * caption. The label scales per size via {@link ButtonSize#typeRole()}; XS / S visibly inflate
   * their component bounds to the 48 dp WCAG touch target with the body centered inside.
   *
   * @return the sizes panel
   * @version v0.2.0
   * @since v0.2.0
   */
  public static JPanel buildSizesPanel() {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    column.add(
        captionLabel(
            "The 5 M3 Expressive sizes at FILLED, in both shapes. Label type scales per size "
                + "(labelLarge → titleMedium → headlineSmall → headlineLarge). XS / S inflate "
                + "their component bounds to the 48 dp WCAG touch target — the visible chrome "
                + "stays 32 / 40, centered."));
    column.add(Box.createVerticalStrut(12));

    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setOpaque(false);
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Size \\ Shape"), gbc);
    gbc.gridx = 1;
    matrix.add(headerLabel("Round"), gbc);
    gbc.gridx = 2;
    matrix.add(headerLabel("Square"), gbc);
    gbc.gridx = 3;
    matrix.add(headerLabel("With icon"), gbc);
    gbc.gridx = 4;
    matrix.add(headerLabel("Spec"), gbc);

    int row = 1;
    for (ButtonSize size : ButtonSize.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      matrix.add(rowLabel(size.name()), gbc);
      gbc.gridx = 1;
      matrix.add(
          new ElwhaButton("Common button").setButtonSize(size).setShape(ButtonShape.ROUND), gbc);
      gbc.gridx = 2;
      matrix.add(
          new ElwhaButton("Common button").setButtonSize(size).setShape(ButtonShape.SQUARE), gbc);
      gbc.gridx = 3;
      matrix.add(
          new ElwhaButton("Common button", MaterialIcons.delete(size.iconSizePx()))
              .setButtonSize(size),
          gbc);
      gbc.gridx = 4;
      matrix.add(specLabel(size), gbc);
    }

    matrix.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(matrix);
    column.add(Box.createVerticalGlue());
    return column;
  }

  // ----------------------------------------------------- toggle examples

  /**
   * Builds the toggle-examples panel: one row per {@code SELECTABLE}-capable variant ({@link
   * ButtonVariant#ELEVATED} / {@link ButtonVariant#FILLED} / {@link ButtonVariant#FILLED_TONAL} /
   * {@link ButtonVariant#OUTLINED}), each row a three-button {@link ButtonGroup} demonstrating
   * mutex selection. The FILLED and FILLED_TONAL rows are mandatory groups (always one selected);
   * ELEVATED and OUTLINED are non-mandatory (the active button can be cleared by re-click). Each
   * row has a live status label fed from the group's {@code PROPERTY_SELECTED_BUTTON} listener.
   *
   * @return the toggle-examples panel
   * @version v0.2.0
   * @since v0.2.0
   */
  public static JPanel buildTogglesPanel() {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    column.add(
        captionLabel(
            "Each row is a 3-button ButtonGroup — mutex selection. FILLED + FILLED_TONAL rows "
                + "are mandatory (always one selected); ELEVATED + OUTLINED are non-mandatory "
                + "(re-click the active button to clear). Status label updates from the group's "
                + "PROPERTY_SELECTED_BUTTON listener."));
    column.add(Box.createVerticalStrut(16));

    column.add(buildToggleRow(ButtonVariant.ELEVATED, false, "Day", "Week", "Month"));
    column.add(Box.createVerticalStrut(16));
    column.add(buildToggleRow(ButtonVariant.FILLED, true, "List", "Grid", "Compact"));
    column.add(Box.createVerticalStrut(16));
    column.add(buildToggleRow(ButtonVariant.FILLED_TONAL, true, "Low", "Medium", "High"));
    column.add(Box.createVerticalStrut(16));
    column.add(buildToggleRow(ButtonVariant.OUTLINED, false, "All", "Active", "Archived"));
    column.add(Box.createVerticalGlue());
    return column;
  }

  private static JPanel buildToggleRow(
      final ButtonVariant variant, final boolean mandatory, final String... labels) {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
    row.setOpaque(false);
    row.setAlignmentX(Component.LEFT_ALIGNMENT);

    final String mode = mandatory ? "mandatory" : "non-mandatory";
    final JLabel header = rowLabel(variant.name() + " — " + mode);
    header.setAlignmentX(Component.LEFT_ALIGNMENT);
    row.add(header);
    row.add(Box.createVerticalStrut(6));

    final JPanel buttons = new JPanel();
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
    buttons.setOpaque(false);
    buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

    final ButtonGroup group = new ButtonGroup().setMandatory(mandatory);
    ElwhaButton firstButton = null;
    for (final String label : labels) {
      final ElwhaButton button =
          new ElwhaButton(label)
              .setVariant(variant)
              .setInteractionMode(ButtonInteractionMode.SELECTABLE);
      group.add(button);
      if (firstButton == null) {
        firstButton = button;
      }
      buttons.add(button);
      buttons.add(Box.createHorizontalStrut(CELL_GAP));
    }

    final JLabel status = rowLabel("selected: (none)");
    group.addSelectionChangeListener(
        evt -> {
          final ElwhaButton nv = (ElwhaButton) evt.getNewValue();
          status.setText("selected: " + (nv == null ? "(none)" : nv.getText()));
        });
    // Mandatory groups must start with a selection — seed the first button.
    if (mandatory && firstButton != null) {
      group.setSelected(firstButton);
    }

    buttons.add(status);
    row.add(buttons);
    return row;
  }

  // --------------------------------------------------------------- live

  /**
   * Builds the live-control panel: a single {@link ElwhaButton} driven by combo boxes, a spinner,
   * and checkboxes covering every axis the class exposes (variant, interaction mode, size, shape,
   * surface-role override, border width, leading icon, selected, enabled). A live config-snippet
   * text area shows the equivalent Java construction code, refreshed on every change.
   *
   * @return the live-control panel
   * @version v0.2.0
   * @since v0.2.0
   */
  public static JPanel buildLivePanel() {
    final LiveState state = new LiveState();

    final JPanel controls = new JPanel(new GridBagLayout());
    controls.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 4, 4, 12);
    gbc.anchor = GridBagConstraints.WEST;
    int row = 0;

    final JComboBox<ButtonVariant> variantBox = new JComboBox<>(ButtonVariant.values());
    variantBox.setSelectedItem(ButtonVariant.FILLED);
    addControlRow(controls, gbc, row++, "Variant", variantBox);

    final JComboBox<ButtonInteractionMode> modeBox =
        new JComboBox<>(ButtonInteractionMode.values());
    addControlRow(controls, gbc, row++, "Interaction mode", modeBox);

    final JComboBox<ButtonSize> sizeBox = new JComboBox<>(ButtonSize.values());
    sizeBox.setSelectedItem(ButtonSize.S);
    addControlRow(controls, gbc, row++, "Size", sizeBox);

    final JComboBox<ButtonShape> shapeBox = new JComboBox<>(ButtonShape.values());
    addControlRow(controls, gbc, row++, "Shape", shapeBox);

    final JComboBox<SurfaceRoleChoice> surfaceBox = new JComboBox<>(SurfaceRoleChoice.values());
    addControlRow(controls, gbc, row++, "Surface role override", surfaceBox);

    final JSpinner borderWidth = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
    addControlRow(controls, gbc, row++, "Border width (px)", borderWidth);

    final JCheckBox iconBox = new JCheckBox("Leading icon");
    addControlRow(controls, gbc, row++, "", iconBox);

    final JCheckBox selectedBox = new JCheckBox("Selected");
    addControlRow(controls, gbc, row++, "", selectedBox);

    final JCheckBox enabledBox = new JCheckBox("Enabled", true);
    addControlRow(controls, gbc, row++, "", enabledBox);

    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));

    final JTextArea snippet = new JTextArea(7, 44);
    snippet.setEditable(false);
    snippet.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    snippet.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final Runnable rebuild =
        () -> {
          state.variant = (ButtonVariant) variantBox.getSelectedItem();
          state.mode = (ButtonInteractionMode) modeBox.getSelectedItem();
          state.size = (ButtonSize) sizeBox.getSelectedItem();
          state.shape = (ButtonShape) shapeBox.getSelectedItem();
          state.surface = (SurfaceRoleChoice) surfaceBox.getSelectedItem();
          state.borderWidth = (Integer) borderWidth.getValue();
          state.icon = iconBox.isSelected();
          state.selected = selectedBox.isSelected();
          state.enabled = enabledBox.isSelected();
          // SELECTABLE + TEXT is illegal — guard the combo pairing so the demo never throws.
          if (state.mode == ButtonInteractionMode.SELECTABLE
              && state.variant == ButtonVariant.TEXT) {
            state.mode = ButtonInteractionMode.CLICKABLE;
            modeBox.setSelectedItem(ButtonInteractionMode.CLICKABLE);
          }
          rebuildStage(stage, state);
          snippet.setText(renderSnippet(state));
        };

    variantBox.addActionListener(e -> rebuild.run());
    modeBox.addActionListener(e -> rebuild.run());
    sizeBox.addActionListener(e -> rebuild.run());
    shapeBox.addActionListener(e -> rebuild.run());
    surfaceBox.addActionListener(e -> rebuild.run());
    borderWidth.addChangeListener(e -> rebuild.run());
    iconBox.addActionListener(e -> rebuild.run());
    selectedBox.addActionListener(e -> rebuild.run());
    enabledBox.addActionListener(e -> rebuild.run());
    rebuild.run();

    final JPanel wrap = new JPanel();
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
    controls.setAlignmentX(Component.LEFT_ALIGNMENT);
    stage.setAlignmentX(Component.LEFT_ALIGNMENT);
    final JScrollPane snippetScroll = new JScrollPane(snippet);
    snippetScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
    snippetScroll.setBorder(BorderFactory.createTitledBorder("Equivalent Java"));
    wrap.add(controls);
    wrap.add(stage);
    wrap.add(snippetScroll);
    return wrap;
  }

  private static void rebuildStage(final JPanel stage, final LiveState state) {
    stage.removeAll();
    final ElwhaButton button =
        new ElwhaButton("Common button")
            .setVariant(state.variant)
            .setButtonSize(state.size)
            .setShape(state.shape)
            .setBorderWidth(state.borderWidth);
    if (state.mode == ButtonInteractionMode.SELECTABLE) {
      button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
      button.setSelected(state.selected);
    }
    if (state.surface.role != null) {
      button.setSurfaceRole(state.surface.role);
    }
    if (state.icon) {
      button.setIcon(MaterialIcons.delete(state.size.iconSizePx()));
    }
    button.setEnabled(state.enabled);
    stage.add(button);
    stage.revalidate();
    stage.repaint();
  }

  private static String renderSnippet(final LiveState state) {
    final StringBuilder sb = new StringBuilder();
    if (state.icon) {
      sb.append("new ElwhaButton(\"Common button\",\n")
          .append("    MaterialIcons.delete(")
          .append(state.size.iconSizePx())
          .append("))\n");
    } else {
      sb.append("new ElwhaButton(\"Common button\")\n");
    }
    sb.append("    .setVariant(ButtonVariant.").append(state.variant).append(")\n");
    sb.append("    .setButtonSize(ButtonSize.").append(state.size).append(")\n");
    sb.append("    .setShape(ButtonShape.").append(state.shape).append(")");
    if (state.borderWidth != 1) {
      sb.append("\n    .setBorderWidth(").append(state.borderWidth).append(")");
    }
    if (state.surface.role != null) {
      sb.append("\n    .setSurfaceRole(ColorRole.").append(state.surface.role).append(")");
    }
    if (state.mode == ButtonInteractionMode.SELECTABLE) {
      sb.append("\n    .setInteractionMode(ButtonInteractionMode.SELECTABLE)");
      sb.append("\n    .setSelected(").append(state.selected).append(")");
    }
    sb.append(";");
    if (!state.enabled) {
      sb.append("\n// button.setEnabled(false);");
    }
    return sb.toString();
  }

  /** Mutable bag of the live panel's current control values. */
  private static final class LiveState {
    ButtonVariant variant = ButtonVariant.FILLED;
    ButtonInteractionMode mode = ButtonInteractionMode.CLICKABLE;
    ButtonSize size = ButtonSize.S;
    ButtonShape shape = ButtonShape.ROUND;
    SurfaceRoleChoice surface = SurfaceRoleChoice.VARIANT_DEFAULT;
    int borderWidth = 1;
    boolean icon;
    boolean selected;
    boolean enabled = true;
  }

  /** Wraps a nullable {@link ColorRole} as a combo-box entry — {@code VARIANT_DEFAULT} → null. */
  private enum SurfaceRoleChoice {
    VARIANT_DEFAULT(null),
    PRIMARY(ColorRole.PRIMARY),
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),
    SURFACE_CONTAINER_HIGHEST(ColorRole.SURFACE_CONTAINER_HIGHEST),
    ERROR_CONTAINER(ColorRole.ERROR_CONTAINER);

    final ColorRole role;

    SurfaceRoleChoice(final ColorRole role) {
      this.role = role;
    }
  }

  // -------------------------------------------------------- combined tab

  /**
   * Bundles all four panels under one {@link JTabbedPane}, used by both the standalone {@link
   * ElwhaButtonPlayground} and the {@code ThemePlayground} {@code Button} tab.
   *
   * @return a tabbed pane with {@code Variant gallery}, {@code Sizes}, {@code Toggle examples}, and
   *     {@code Live} sub-tabs
   * @version v0.2.0
   * @since v0.2.0
   */
  public static JTabbedPane buildCombinedTabbedPane() {
    final JTabbedPane inner = new JTabbedPane();
    inner.addTab("Variant gallery", new JScrollPane(buildVariantGalleryPanel()));
    inner.addTab("Sizes", new JScrollPane(buildSizesPanel()));
    inner.addTab("Toggle examples", new JScrollPane(buildTogglesPanel()));
    inner.addTab("Live", new JScrollPane(buildLivePanel()));
    return inner;
  }

  // ----------------------------------------------------------- helpers

  private static void addControlRow(
      final JPanel panel,
      final GridBagConstraints gbc,
      final int row,
      final String label,
      final JComponent control) {
    gbc.gridy = row;
    gbc.gridx = 0;
    panel.add(new JLabel(label), gbc);
    gbc.gridx = 1;
    panel.add(control, gbc);
  }

  private static JLabel specLabel(final ButtonSize size) {
    final JLabel label =
        new JLabel(
            "<html>h="
                + size.containerHeightPx()
                + " &nbsp;sq="
                + size.squareCornerPx()
                + "<br>icon="
                + size.iconSizePx()
                + " &nbsp;target="
                + size.minimumTargetPx()
                + "</html>");
    label.putClientProperty("FlatLaf.styleClass", "small");
    final Color disabled = UIManager.getColor("Label.disabledForeground");
    if (disabled != null) {
      label.setForeground(disabled);
    }
    return label;
  }

  private static JLabel headerLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.putClientProperty("FlatLaf.styleClass", "small");
    final Color disabled = UIManager.getColor("Label.disabledForeground");
    if (disabled != null) {
      label.setForeground(disabled);
    }
    return label;
  }

  private static JLabel rowLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.putClientProperty("FlatLaf.styleClass", "small");
    return label;
  }

  private static JLabel captionLabel(final String text) {
    final JLabel label = new JLabel("<html><div style='width: 720px'>" + text + "</div></html>");
    label.putClientProperty("FlatLaf.styleClass", "small");
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }
}
