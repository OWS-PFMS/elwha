package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.icons.MaterialIcons;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Reusable panel builders for the {@link ElwhaFab} playground surfaces. The Elwha Showcase's FAB
 * tab composes these so the showcase entry and the per-story standalone smoketest playgrounds (each
 * {@code ElwhaFab*Playground} under this package) stay in lockstep on the variant / size matrices —
 * same factored-builder pattern as {@code IconButtonPlaygroundPanels} / {@code
 * ButtonPlaygroundPanels} / {@code ChipPlaygroundPanels}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class FabPlaygroundPanels {

  private static final int CELL_GAP = 14;

  private FabPlaygroundPanels() {}

  /**
   * Builds the variant gallery panel: every {@link ElwhaFab.Color} as a row × every visual state
   * (idle / hover / pressed / focused / disabled) as a column, all rendered at {@link
   * ElwhaFab.Size#SMALL} in the Standard (icon-only) form. Hover and pressed cells use {@link
   * ElwhaFab#setHovered}/{@link ElwhaFab#setPressed} to pre-render the state-layer overlay so the
   * M3 spec rendering is visible side-by-side without requiring live interaction. The focused
   * column still requires Tab-to since focus is bound to actual focus ownership; the disabled
   * column FABs are actually disabled.
   *
   * <p>Matches the {@code IconButtonPlaygroundPanels.buildVariantGalleryPanel} shape — same column
   * vocabulary, same pre-render hooks — so the two galleries read identically in the Showcase.
   *
   * @return the variant gallery panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildVariantGalleryPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] columnLabels = {"Idle", "Hover", "Pressed", "Focused (Tab to)", "Disabled"};

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Color \\ State"), gbc);
    for (int c = 0; c < columnLabels.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(columnLabels[c]), gbc);
    }

    int row = 1;
    for (ElwhaFab.Color color : ElwhaFab.Color.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      matrix.add(rowLabel(color.name()), gbc);

      for (int c = 0; c < columnLabels.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildStandardGalleryCell(color, c), gbc);
      }
    }
    return matrix;
  }

  /**
   * Builds the sizes panel: every {@link ElwhaFab.Size} as a row × every form ({@code Standard
   * (icon)} / {@code Extended (text)} / {@code Extended (icon + text)}) as a column, all at the
   * default {@link ElwhaFab.Color#PRIMARY_CONTAINER}. Lets the validator confirm container height
   * (56 / 80 / 96 dp), icon-size scaling, and the Extended-form per-size leading / icon-gap /
   * trailing insets all match the M3 token-panel values at a glance.
   *
   * @return the sizes panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildSizesPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] columnLabels = {"Standard (icon)", "Extended (text)", "Extended (icon + text)"};

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Size \\ Form"), gbc);
    for (int c = 0; c < columnLabels.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(columnLabels[c]), gbc);
    }

    int row = 1;
    for (ElwhaFab.Size size : ElwhaFab.Size.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      matrix.add(rowLabel(size.name() + " · " + size.containerPx() + " dp"), gbc);

      gbc.gridx = 1;
      matrix.add(ElwhaFab.standard(MaterialIcons.add(size.iconPx())).setFabSize(size), gbc);

      gbc.gridx = 2;
      matrix.add(ElwhaFab.extended("Compose").setFabSize(size), gbc);

      gbc.gridx = 3;
      matrix.add(
          ElwhaFab.extended(MaterialIcons.add(size.iconPx()), "Compose").setFabSize(size), gbc);
    }
    return matrix;
  }

  private static JComponent buildStandardGalleryCell(
      final ElwhaFab.Color color, final int columnIndex) {
    final Icon icon = MaterialIcons.add(ElwhaFab.Size.SMALL.iconPx());
    final ElwhaFab fab = ElwhaFab.standard(icon).setColor(color);
    fab.setToolTipText(color.name());
    switch (columnIndex) {
      case 0 -> {
        /* idle — nothing */
      }
      case 1 -> fab.setHovered(true);
      case 2 -> fab.setPressed(true);
      case 3 -> fab.setToolTipText(color.name() + " — focus this with Tab");
      case 4 -> fab.setEnabled(false);
      default -> {
        /* nothing */
      }
    }
    return fab;
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
}
