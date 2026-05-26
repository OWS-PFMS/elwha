package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #188 (S2) smoketest — Standard FAB across all six {@link ElwhaFab.Color} styles at every
 * {@link ElwhaFab.Size}. Renders a 6 × 3 matrix (color × size) so the validator can scan a single
 * panel and confirm each {@code (container, on-container)} role pair resolves correctly. The light
 * / dark / system mode toggle exercises the M3 dark-scheme resolution path — the same role names
 * pick up the dark palette values without any per-mode code in the FAB.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabColorPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabColorPlayground {

  private final JFrame frame = new JFrame("ElwhaFab — S2 colors (#188)");

  private ElwhaFabColorPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaFabColorPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(new JScrollPane(buildColorSizeMatrix()), BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.setSize(960, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildColorSizeMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 16, 8, 16);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();
    final ElwhaFab.Color[] colors = ElwhaFab.Color.values();

    gc.gridy = 0;
    gc.gridx = 0;
    grid.add(boldLabel("Color \\ Size"), gc);
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      grid.add(boldLabel(sizes[col].name() + " · " + sizes[col].containerPx() + " dp"), gc);
    }

    for (int row = 0; row < colors.length; row++) {
      gc.gridy = row + 1;
      gc.gridx = 0;
      gc.anchor = GridBagConstraints.WEST;
      grid.add(new JLabel(colors[row].name()), gc);
      gc.anchor = GridBagConstraints.CENTER;
      for (int col = 0; col < sizes.length; col++) {
        gc.gridx = col + 1;
        final ElwhaFab fab =
            ElwhaFab.standard(MaterialIcons.add(sizes[col].iconPx()))
                .setFabSize(sizes[col])
                .setColor(colors[row]);
        fab.setToolTipText(colors[row].name() + " · " + sizes[col].name());
        grid.add(fab, gc);
      }
    }
    return grid;
  }

  private JLabel boldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    bar.add(new JLabel("Mode:"));
    final ButtonGroup group = new ButtonGroup();
    for (Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK, Mode.SYSTEM}) {
      final JToggleButton button = new JToggleButton(mode.name());
      button.addActionListener(e -> applyMode(mode));
      if (ElwhaTheme.current().mode() == mode) {
        button.setSelected(true);
      }
      group.add(button);
      bar.add(button);
    }
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
