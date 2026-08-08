package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #193 (S7) verification — every from → to morph combination across all three {@link
 * ElwhaFab.Size} tiers. The matrix carries one FAB per (size, starting form) cell so the validator
 * can observe each direction independently: row 1 (Standard-start) FABs morph forward to Extended;
 * row 2 (Extended-start) FABs morph reverse to Standard. A "Toggle all" control fires every morph
 * simultaneously so cross-instance interference is observable in one glance.
 *
 * <p>An RTL toggle re-orients every cell, confirming the design doc §11 icon-trailing /
 * label-leading layout mirrors correctly both as a static endpoint and through every mid-morph
 * frame (the icon translation and label fade are anchored to the morph-mirrored layout, not a
 * frozen LTR layout).
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabMorphMatrixPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ElwhaFabMorphMatrixPlayground {

  private final JFrame frame = new JFrame("ElwhaFab — S7 morph matrix (#193)");
  private final List<ElwhaFab> fabs = new ArrayList<>();
  private final List<ElwhaButton> rowToggles = new ArrayList<>();
  private JPanel matrixHost;
  private ComponentOrientation orientation = ComponentOrientation.LEFT_TO_RIGHT;

  private ElwhaFabMorphMatrixPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaFabMorphMatrixPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildControlBar(), BorderLayout.NORTH);
    matrixHost = new JPanel(new BorderLayout());
    matrixHost.add(buildMatrix(), BorderLayout.CENTER);
    frame.add(matrixHost, BorderLayout.CENTER);
    frame.setSize(1100, 640);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildMatrix() {
    fabs.clear();
    rowToggles.clear();

    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
    grid.applyComponentOrientation(orientation);
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(14, 24, 14, 24);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();
    final ElwhaFab.Form[] startingForms = {ElwhaFab.Form.STANDARD, ElwhaFab.Form.EXTENDED};

    gc.gridy = 0;
    gc.gridx = 0;
    grid.add(boldLabel("Start → Target"), gc);
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      grid.add(boldLabel(sizes[col].name() + " · " + sizes[col].containerPx() + " dp"), gc);
    }

    for (int row = 0; row < startingForms.length; row++) {
      final ElwhaFab.Form start = startingForms[row];
      final ElwhaFab.Form target =
          (start == ElwhaFab.Form.STANDARD) ? ElwhaFab.Form.EXTENDED : ElwhaFab.Form.STANDARD;
      final int gridRow = row + 1;

      gc.gridy = gridRow;
      gc.gridx = 0;
      gc.anchor = GridBagConstraints.WEST;
      grid.add(new JLabel(start.name() + " → " + target.name()), gc);
      gc.anchor = GridBagConstraints.CENTER;

      for (int col = 0; col < sizes.length; col++) {
        final ElwhaFab.Size size = sizes[col];
        final ElwhaFab fab =
            ElwhaFab.extended(MaterialIcons.add(size.iconPx()), "Compose").setFabSize(size);
        fab.setComponentOrientation(orientation);
        if (start == ElwhaFab.Form.STANDARD) {
          // Reverse synchronously without an animation — the form is already EXTENDED from the
          // construction factory; snap to Standard so the first paint reads as the row-1 start.
          fab.morphTo(ElwhaFab.Form.STANDARD);
        }
        fab.setToolTipText("Start: " + start.name() + " · target: " + target.name());
        fabs.add(fab);

        gc.gridx = col + 1;
        grid.add(fab, gc);
      }

      gc.gridy = gridRow;
      gc.gridx = sizes.length + 1;
      final ElwhaButton rowToggle = ElwhaButton.filledTonalButton("Toggle row");
      rowToggle.addActionListener(
          e -> {
            // Capture the row's fabs by gridRow — fabs are appended in column order, row by row.
            final int rowStart = (gridRow - 1) * sizes.length;
            for (int i = rowStart; i < rowStart + sizes.length; i++) {
              final ElwhaFab fab = fabs.get(i);
              fab.morphTo(
                  fab.getForm() == ElwhaFab.Form.EXTENDED
                      ? ElwhaFab.Form.STANDARD
                      : ElwhaFab.Form.EXTENDED);
            }
          });
      rowToggles.add(rowToggle);
      grid.add(rowToggle, gc);
    }

    return grid;
  }

  private JLabel boldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  private JPanel buildControlBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));

    final ElwhaButton toggleAll = ElwhaButton.filledTonalButton("Toggle all");
    toggleAll.addActionListener(
        e -> {
          for (ElwhaFab fab : fabs) {
            fab.morphTo(
                fab.getForm() == ElwhaFab.Form.EXTENDED
                    ? ElwhaFab.Form.STANDARD
                    : ElwhaFab.Form.EXTENDED);
          }
        });
    bar.add(toggleAll);

    bar.add(new JLabel("    Orientation:"));
    final ElwhaButtonGroup orientGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL)
            .add(new ElwhaButton("LTR"))
            .add(new ElwhaButton("RTL"));
    orientGroup.setSelectedIndex(0);
    orientGroup.addSelectionListener(
        g ->
            applyOrientation(
                g.getSelectedIndex() == 1
                    ? ComponentOrientation.RIGHT_TO_LEFT
                    : ComponentOrientation.LEFT_TO_RIGHT));
    bar.add(orientGroup);

    bar.add(new JLabel("    Mode:"));
    final Mode[] modes = {Mode.LIGHT, Mode.DARK, Mode.SYSTEM};
    final ElwhaButtonGroup modeGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL);
    for (final Mode mode : modes) {
      modeGroup.add(new ElwhaButton(mode.name()));
    }
    final Mode current = ElwhaTheme.current().mode();
    for (int i = 0; i < modes.length; i++) {
      if (modes[i] == current) {
        modeGroup.setSelectedIndex(i);
      }
    }
    // Seeded before the listener is attached so the initial selection does not re-install the
    // theme on startup.
    modeGroup.addSelectionListener(g -> applyMode(modes[g.getSelectedIndex()]));
    bar.add(modeGroup);
    return bar;
  }

  private void applyOrientation(final ComponentOrientation next) {
    this.orientation = next;
    matrixHost.removeAll();
    matrixHost.add(buildMatrix(), BorderLayout.CENTER);
    matrixHost.revalidate();
    matrixHost.repaint();
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
