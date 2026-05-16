package com.owspfm.elwha.iconbutton.playground;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.icons.MaterialIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
   * (idle / hover / pressed / selected / focused / disabled) as a column. The buttons in the
   * "hover" / "pressed" / "selected" / "focused" columns aren't programmatically driven into those
   * states — instead each column's leftmost button starts in the natural state, and the viewer
   * interacts with the live buttons to validate transitions. The {@code disabled} column buttons
   * are actually disabled.
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
      "Idle", "Hover (live)", "Pressed (live)", "Selected", "Focused (Tab to)", "Disabled"
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
   * Builds the toggle-examples panel: one row per variant, each row exercising the {@code
   * setIcons(resting, selected)} pair pattern (and one state-layer-only row that drops the
   * selected-icon swap so the overlay carries the signal alone).
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
            "Pin (icon swap pair)",
            () -> MaterialIcons.pushPin(),
            () -> MaterialIcons.pushPinFilled()));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Anchor (icon swap pair)",
            () -> MaterialIcons.anchor(),
            () -> MaterialIcons.anchorFilled()));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Favorite (state-layer only — same glyph in both states)",
            () -> MaterialIcons.favorite(),
            null));
    column.add(Box.createVerticalStrut(12));

    column.add(
        buildToggleRow(
            "Star (state-layer only — same glyph in both states)",
            () -> MaterialIcons.star(),
            null));
    column.add(Box.createVerticalGlue());
    return column;
  }

  // ----- private helpers -----

  private static JComponent buildGalleryCell(
      final IconButtonVariant variant, final int columnIndex) {
    final ElwhaIconButton button =
        new ElwhaIconButton(MaterialIcons.favorite()).setVariant(variant);
    button.setToolTipText(variant.name());

    switch (columnIndex) {
      case 0 -> {
        /* idle — nothing */
      }
      case 1, 2 -> button.setToolTipText(variant.name() + " — hover/press the button");
      case 3 -> {
        button.setInteractionMode(IconButtonInteractionMode.SELECTABLE);
        button.setIcons(MaterialIcons.favorite(), MaterialIcons.favorite());
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
      final String label, final Supplier<Icon> resting, final Supplier<Icon> selected) {
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
      if (selected != null) {
        button.setIcons(resting.get(), selected.get());
      } else {
        button.setIcons(resting.get(), resting.get());
      }
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
   * Convenience that bundles the two panels under one {@link JTabbedPane}, used by both the
   * standalone playground and the {@code ThemePlayground} tab.
   *
   * @return a tabbed pane with {@code Variant gallery} and {@code Toggle examples} sub-tabs
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JTabbedPane buildCombinedTabbedPane() {
    final JTabbedPane inner = new JTabbedPane();
    inner.addTab("Variant gallery", buildVariantGalleryPanel());
    inner.addTab("Toggle examples", buildToggleExamplesPanel());
    return inner;
  }
}
