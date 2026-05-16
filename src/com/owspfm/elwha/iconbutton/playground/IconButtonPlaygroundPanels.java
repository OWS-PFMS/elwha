package com.owspfm.elwha.iconbutton.playground;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonGroup;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Reusable panel builders for the {@link ElwhaIconButton} playground surfaces. Lets the standalone
 * {@code ElwhaIconButtonPlayground} and the {@code ThemePlayground}'s {@code Icon Button} tab share
 * one canonical implementation of the variant gallery and toggle-examples panels so both entry
 * points stay in lockstep.
 *
 * <p>Mirrors {@code SurfacePlaygroundPanels} / {@code ChipPlaygroundPanels} — same factored-builder
 * pattern, same reasons.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class IconButtonPlaygroundPanels {

  private static final int CELL_GAP = 12;

  private IconButtonPlaygroundPanels() {}

  /**
   * Builds the variant gallery panel: every {@link IconButtonVariant} as a row × every visual state
   * (idle / hover / pressed / selected / focused / disabled) as a column. Hover and pressed cells
   * use {@link ElwhaIconButton#setHovered}/{@link ElwhaIconButton#setPressed} to pre-render the
   * state-layer overlay so the M3 spec rendering is visible side-by-side without requiring live
   * interaction (pressed in particular is normally too transient to inspect). The {@code focused}
   * column still requires Tab-to since focus is bound to actual focus ownership; the {@code
   * disabled} column buttons are actually disabled.
   *
   * <p>The matrix re-skins end-to-end on a theme/mode switch — the binding-rule contract for the
   * whole component set.
   *
   * @return the variant gallery panel
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JPanel buildVariantGalleryPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] columnLabels = {
      "Idle", "Hover", "Pressed", "Selected", "Focused (Tab to)", "Disabled"
    };

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Variant \\ State"), gbc);
    for (int c = 0; c < columnLabels.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(columnLabels[c]), gbc);
    }

    int row = 1;
    for (IconButtonVariant variant : IconButtonVariant.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      matrix.add(rowLabel(variant.name()), gbc);

      for (int c = 0; c < columnLabels.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildGalleryCell(variant, c), gbc);
      }
    }

    return matrix;
  }

  /**
   * Builds the toggle-examples panel: one row per icon-swap pair (pin / anchor / favorite / star),
   * each row spanning all 4 variants, plus a square-shape row at the bottom that re-uses the pin
   * pair with {@link ShapeScale#MD} so the playground covers both the capsule (M3 default) and the
   * square treatments.
   *
   * @return the toggle-examples panel
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JPanel buildToggleExamplesPanel() {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    column.add(
        captionLabel(
            "Each row: SELECTABLE icon buttons across all 4 variants. "
                + "Click to toggle. Tab to focus."));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Pin (push_pin ↔ push_pin_fill)",
            () -> MaterialIcons.pushPin(),
            () -> MaterialIcons.pushPinFilled(),
            ShapeScale.FULL));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Anchor (anchor ↔ anchor_fill)",
            () -> MaterialIcons.anchor(),
            () -> MaterialIcons.anchorFilled(),
            ShapeScale.FULL));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Favorite (favorite ↔ favorite_fill)",
            () -> MaterialIcons.favorite(),
            () -> MaterialIcons.favoriteFilled(),
            ShapeScale.FULL));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Star (star ↔ star_fill)",
            () -> MaterialIcons.star(),
            () -> MaterialIcons.starFilled(),
            ShapeScale.FULL));
    column.add(Box.createVerticalStrut(20));

    column.add(captionLabel("Same pin pair, square shape (ShapeScale.MD = 12 px corner radius)."));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Pin, square (setShape(MD))",
            () -> MaterialIcons.pushPin(),
            () -> MaterialIcons.pushPinFilled(),
            ShapeScale.MD));
    column.add(Box.createVerticalGlue());
    return column;
  }

  /**
   * Builds the sizes panel: a {@link IconButtonSize} × {@link IconButtonVariant} matrix (5 sizes ×
   * 4 variants = 20 buttons) at the {@link ShapeScale#FULL} capsule shape, plus a small {@link
   * JToolBar} mockup at {@link IconButtonSize#S} so the toolbar-usage case is visible end-to-end
   * (focus, hover, press all behave the same as in a free-standing layout).
   *
   * @return the sizes panel
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JPanel buildSizesPanel() {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    column.add(
        captionLabel(
            "JToolBar mockup at IconButtonSize.M (40 dp). "
                + "Left cluster: independent SELECTABLE toggles (pin / anchor / visibility / edit "
                + "/ delete, each its own state). Right cluster: mandatory radio group "
                + "(favorite / star / info / help — exactly one always selected) via "
                + "IconButtonGroup."));
    column.add(Box.createVerticalStrut(8));
    final JToolBar toolBar = buildToolbarMockup();
    toolBar.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(toolBar);
    column.add(Box.createVerticalStrut(24));

    column.add(captionLabel("The 5 M3 sizes × 4 variants — favorite glyph, capsule shape."));
    column.add(Box.createVerticalStrut(12));
    final JPanel sizeMatrix = buildSizeMatrix();
    sizeMatrix.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(sizeMatrix);
    column.add(Box.createVerticalGlue());
    return column;
  }

  /**
   * Builds the live panel: a single {@link ElwhaIconButton} driven by combo boxes, a spinner, and a
   * checkbox covering every axis the class exposes — variant, interaction mode, size, shape,
   * surface-role override, border width, icon pair, and selected state. Mirrors the surface and
   * chip playgrounds' live tabs; lets the viewer feel every property change live without recompile.
   *
   * @return the live-control panel
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JPanel buildLivePanel() {
    final ElwhaIconButton target =
        new ElwhaIconButton(MaterialIcons.pushPin())
            .setIcons(MaterialIcons.pushPin(), MaterialIcons.pushPinFilled());

    final JPanel controls = new JPanel(new GridBagLayout());
    controls.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 4, 4, 12);
    gbc.anchor = GridBagConstraints.WEST;

    int row = 0;

    final JComboBox<IconButtonVariant> variantBox = new JComboBox<>(IconButtonVariant.values());
    variantBox.setSelectedItem(target.getVariant());
    variantBox.addActionListener(
        e -> target.setVariant((IconButtonVariant) variantBox.getSelectedItem()));
    addControlRow(controls, gbc, row++, "Variant", variantBox);

    final JComboBox<IconButtonInteractionMode> modeBox =
        new JComboBox<>(IconButtonInteractionMode.values());
    modeBox.setSelectedItem(target.getInteractionMode());
    modeBox.addActionListener(
        e -> target.setInteractionMode((IconButtonInteractionMode) modeBox.getSelectedItem()));
    addControlRow(controls, gbc, row++, "Interaction mode", modeBox);

    final JComboBox<IconButtonSize> sizeBox = new JComboBox<>(IconButtonSize.values());
    sizeBox.setSelectedItem(target.getButtonSize());
    sizeBox.addActionListener(
        e -> {
          final IconButtonSize chosen = (IconButtonSize) sizeBox.getSelectedItem();
          target.setButtonSize(chosen);
          // Reload current icon pair at the new pixel size so the icon stays sharp.
          final IconPairChoice currentPair =
              (IconPairChoice) controls.getClientProperty("currentPair");
          if (currentPair != null) {
            target.setIcons(
                currentPair.pair(chosen.iconPx()).resting(),
                currentPair.pair(chosen.iconPx()).filled());
          }
        });
    addControlRow(controls, gbc, row++, "Size", sizeBox);

    final JComboBox<ShapeScale> shapeBox = new JComboBox<>(ShapeScale.values());
    shapeBox.setSelectedItem(target.getShape());
    shapeBox.addActionListener(e -> target.setShape((ShapeScale) shapeBox.getSelectedItem()));
    addControlRow(controls, gbc, row++, "Shape", shapeBox);

    final JComboBox<SurfaceRoleChoice> surfaceBox = new JComboBox<>(SurfaceRoleChoice.values());
    surfaceBox.setSelectedItem(SurfaceRoleChoice.VARIANT_DEFAULT);
    surfaceBox.addActionListener(
        e -> target.setSurfaceRole(((SurfaceRoleChoice) surfaceBox.getSelectedItem()).role));
    addControlRow(controls, gbc, row++, "Surface role override", surfaceBox);

    final JSpinner borderWidth =
        new JSpinner(new SpinnerNumberModel(target.getBorderWidth(), 0, 4, 1));
    borderWidth.addChangeListener(e -> target.setBorderWidth((Integer) borderWidth.getValue()));
    addControlRow(controls, gbc, row++, "Border width (px)", borderWidth);

    final JComboBox<IconPairChoice> iconBox = new JComboBox<>(IconPairChoice.values());
    iconBox.setSelectedItem(IconPairChoice.PIN);
    controls.putClientProperty("currentPair", IconPairChoice.PIN);
    iconBox.addActionListener(
        e -> {
          final IconPairChoice chosen = (IconPairChoice) iconBox.getSelectedItem();
          controls.putClientProperty("currentPair", chosen);
          final int iconPx = target.getButtonSize().iconPx();
          target.setIcons(chosen.pair(iconPx).resting(), chosen.pair(iconPx).filled());
        });
    addControlRow(controls, gbc, row++, "Icon pair", iconBox);

    final JCheckBox selectedBox = new JCheckBox("Selected");
    selectedBox.addActionListener(e -> target.setSelected(selectedBox.isSelected()));
    target.addSelectionChangeListener(
        evt -> selectedBox.setSelected(Boolean.TRUE.equals(evt.getNewValue())));
    addControlRow(controls, gbc, row++, "", selectedBox);

    final JCheckBox enabledBox = new JCheckBox("Enabled", true);
    enabledBox.addActionListener(e -> target.setEnabled(enabledBox.isSelected()));
    addControlRow(controls, gbc, row++, "", enabledBox);

    final JPanel stage = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    stage.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
    stage.setPreferredSize(new Dimension(220, 100));
    stage.add(target);

    final JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(controls, BorderLayout.NORTH);
    wrap.add(stage, BorderLayout.CENTER);
    return wrap;
  }

  // ----- private helpers -----

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

  /** Wraps a nullable {@link ColorRole} as a combo-box entry — {@code VARIANT_DEFAULT} → null. */
  private enum SurfaceRoleChoice {
    VARIANT_DEFAULT(null),
    PRIMARY(ColorRole.PRIMARY),
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),
    SURFACE(ColorRole.SURFACE),
    SURFACE_CONTAINER_HIGHEST(ColorRole.SURFACE_CONTAINER_HIGHEST),
    ERROR_CONTAINER(ColorRole.ERROR_CONTAINER);

    final ColorRole role;

    SurfaceRoleChoice(final ColorRole role) {
      this.role = role;
    }
  }

  /** Bundled outline/fill icon pairs, used by the live-control icon picker. */
  private enum IconPairChoice {
    PIN("push_pin"),
    ANCHOR("anchor"),
    FAVORITE("favorite"),
    STAR("star"),
    INFO("info"),
    HELP("help"),
    DELETE("delete"),
    EDIT("edit"),
    VISIBILITY("visibility");

    final String baseName;

    IconPairChoice(final String baseName) {
      this.baseName = baseName;
    }

    MaterialIcons.IconPair pair(final int size) {
      return MaterialIcons.pair(baseName, size);
    }
  }

  private static JPanel buildSizeMatrix() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setOpaque(false);

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Variant \\ Size"), gbc);
    final IconButtonSize[] sizes = IconButtonSize.values();
    for (int c = 0; c < sizes.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(sizes[c].name() + " (" + sizes[c].containerPx() + ")"), gbc);
    }

    int row = 1;
    for (IconButtonVariant variant : IconButtonVariant.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      matrix.add(rowLabel(variant.name()), gbc);
      for (int c = 0; c < sizes.length; c++) {
        gbc.gridx = c + 1;
        final ElwhaIconButton button =
            new ElwhaIconButton(MaterialIcons.favorite(sizes[c].iconPx()))
                .setVariant(variant)
                .setButtonSize(sizes[c]);
        button.setToolTipText(variant.name() + " · " + sizes[c].name());
        matrix.add(button, gbc);
      }
    }
    return matrix;
  }

  private static JToolBar buildToolbarMockup() {
    final JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    toolBar.setRollover(true);
    final IconButtonSize size = IconButtonSize.M;
    final int iconPx = size.iconPx();

    // Independent toggles — each unrelated affordance tracks its own state.
    toolBar.add(
        makeToggleButton(size, "Pin (independent toggle)", MaterialIcons.pair("push_pin", iconPx)));
    toolBar.add(
        makeToggleButton(
            size, "Anchor (independent toggle)", MaterialIcons.pair("anchor", iconPx)));
    toolBar.add(
        makeToggleButton(
            size, "Visibility (independent toggle)", MaterialIcons.pair("visibility", iconPx)));
    toolBar.add(
        makeToggleButton(size, "Edit (independent toggle)", MaterialIcons.pair("edit", iconPx)));
    toolBar.add(
        makeToggleButton(
            size, "Delete (independent toggle)", MaterialIcons.pair("delete", iconPx)));
    toolBar.addSeparator();

    // Mandatory radio group via IconButtonGroup — exactly one selected at all times.
    final ElwhaIconButton favorite =
        makeToggleButton(size, "Favorite (radio group)", MaterialIcons.pair("favorite", iconPx));
    final ElwhaIconButton star =
        makeToggleButton(size, "Star (radio group)", MaterialIcons.pair("star", iconPx));
    final ElwhaIconButton info =
        makeToggleButton(size, "Info (radio group)", MaterialIcons.pair("info", iconPx));
    final ElwhaIconButton help =
        makeToggleButton(size, "Help (radio group)", MaterialIcons.pair("help", iconPx));
    favorite.setSelected(true); // initial selection — required for a mandatory group
    new IconButtonGroup(true).add(favorite).add(star).add(info).add(help);
    toolBar.add(favorite);
    toolBar.add(star);
    toolBar.add(info);
    toolBar.add(help);
    return toolBar;
  }

  private static ElwhaIconButton makeToggleButton(
      final IconButtonSize size, final String tooltip, final MaterialIcons.IconPair pair) {
    final ElwhaIconButton button =
        new ElwhaIconButton(pair.resting())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(size)
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE)
            .setIcons(pair.resting(), pair.filled());
    button.setToolTipText(tooltip);
    // Toolbar convention: clicks don't grab focus (the toolbar action shouldn't pull focus away
    // from the document / list / editor being acted on). Tab still focuses the button normally.
    button.setRequestFocusEnabled(false);
    return button;
  }

  private static JComponent buildGalleryCell(
      final IconButtonVariant variant, final int columnIndex) {
    final ElwhaIconButton button =
        new ElwhaIconButton(MaterialIcons.favorite()).setVariant(variant);
    button.setToolTipText(variant.name());

    switch (columnIndex) {
      case 0 -> {
        /* idle — nothing */
      }
      case 1 -> button.setHovered(true);
      case 2 -> button.setPressed(true);
      case 3 -> {
        button.setInteractionMode(IconButtonInteractionMode.SELECTABLE);
        button.setIcons(MaterialIcons.favorite(), MaterialIcons.favoriteFilled());
        button.setSelected(true);
      }
      case 4 -> button.setToolTipText(variant.name() + " — focus this with Tab");
      case 5 -> button.setEnabled(false);
      default -> {
        /* nothing */
      }
    }
    return button;
  }

  private static JPanel buildToggleRow(
      final String label,
      final Supplier<Icon> resting,
      final Supplier<Icon> selected,
      final ShapeScale shape) {
    final JPanel row = new JPanel(new BorderLayout(16, 0));
    row.setOpaque(false);
    row.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JLabel header = new JLabel(label);
    header.putClientProperty("FlatLaf.styleClass", "small");
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, CELL_GAP, 0));
    buttons.setOpaque(false);

    for (IconButtonVariant variant : IconButtonVariant.values()) {
      final ElwhaIconButton button = new ElwhaIconButton(resting.get()).setVariant(variant);
      button.setInteractionMode(IconButtonInteractionMode.SELECTABLE);
      button.setShape(shape);
      button.setIcons(resting.get(), selected.get());
      button.setToolTipText(variant.name());
      buttons.add(labeledButton(button, variant.name()));
    }

    final JPanel stack = new JPanel(new BorderLayout());
    stack.setOpaque(false);
    stack.add(header, BorderLayout.NORTH);
    stack.add(buttons, BorderLayout.CENTER);
    row.add(stack, BorderLayout.CENTER);
    return row;
  }

  private static JComponent labeledButton(final ElwhaIconButton button, final String label) {
    final JPanel cell = new JPanel();
    cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
    cell.setOpaque(false);
    button.setAlignmentX(Component.CENTER_ALIGNMENT);
    cell.add(button);
    final JLabel name = new JLabel(label);
    name.putClientProperty("FlatLaf.styleClass", "mini");
    name.setHorizontalAlignment(SwingConstants.CENTER);
    name.setAlignmentX(Component.CENTER_ALIGNMENT);
    cell.add(Box.createVerticalStrut(4));
    cell.add(name);
    return cell;
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
    final JLabel label = new JLabel(text);
    label.putClientProperty("FlatLaf.styleClass", "small");
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  /**
   * Convenience that bundles all three panels under one {@link JTabbedPane}, used by both the
   * standalone playground and the {@code ThemePlayground} tab.
   *
   * @return a tabbed pane with {@code Variant gallery}, {@code Toggle examples}, and {@code Sizes}
   *     sub-tabs
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JTabbedPane buildCombinedTabbedPane() {
    final JTabbedPane inner = new JTabbedPane();
    inner.addTab("Variant gallery", buildVariantGalleryPanel());
    inner.addTab("Toggle examples", buildToggleExamplesPanel());
    inner.addTab("Sizes", buildSizesPanel());
    inner.addTab("Live", buildLivePanel());
    return inner;
  }
}
