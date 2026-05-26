package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #187 (S1) smoketest — Standard FAB skeleton at all three sizes. Pure visual: container +
 * icon paints at the M3 token values (56 / 80 / 96 dp container, 24 / 28 / 36 dp icon, 16 / 20 / 28
 * dp corner radius) in the default {@link ElwhaFab.Color#PRIMARY_CONTAINER} color, light + dark
 * mode toggleable.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabSkeletonPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabSkeletonPlayground {

  private final JFrame frame = new JFrame("ElwhaFab — S1 skeleton (#187)");

  private ElwhaFabSkeletonPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaFabSkeletonPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildSizeMatrix(), BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.setSize(720, 360);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildSizeMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 24, 8, 24);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col;
      gc.gridy = 0;
      grid.add(new JLabel(sizes[col].name() + " (" + sizes[col].containerPx() + " dp)"), gc);

      gc.gridy = 1;
      final ElwhaFab fab =
          ElwhaFab.standard(MaterialIcons.add(sizes[col].iconPx())).setFabSize(sizes[col]);
      fab.setToolTipText("Create (" + sizes[col].name() + ")");
      grid.add(fab, gc);
    }
    return grid;
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
