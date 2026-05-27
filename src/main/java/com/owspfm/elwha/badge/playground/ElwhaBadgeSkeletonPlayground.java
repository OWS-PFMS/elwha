package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
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
 * Story #210 (S1) smoketest — {@link ElwhaBadge} skeleton at both variants. Pure visual: container
 * paints at the M3 token values (Small 6 × 6 dp, Large 16 × 16 dp default) in the default {@link
 * com.owspfm.elwha.theme.ColorRole#ERROR} container color, light + dark mode toggleable.
 *
 * <p>For S1 the Large badge container is rendered at a fixed 16 × 16 dp (no label, no dynamic
 * width) — those land in S2 (#211). Anchor placement against a host icon lands in S3 (#212).
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.badge.playground.ElwhaBadgeSkeletonPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaBadgeSkeletonPlayground {

  private final JFrame frame = new JFrame("ElwhaBadge — S1 skeleton (#210)");

  private ElwhaBadgeSkeletonPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaBadgeSkeletonPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildVariantMatrix(), BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.setSize(560, 280);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildVariantMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 32, 8, 32);
    gc.anchor = GridBagConstraints.CENTER;

    gc.gridx = 0;
    gc.gridy = 0;
    grid.add(new JLabel("SMALL (6 × 6 dp)"), gc);
    gc.gridy = 1;
    grid.add(ElwhaBadge.small(), gc);

    gc.gridx = 1;
    gc.gridy = 0;
    grid.add(new JLabel("LARGE (16 × 16 dp, S2 adds label)"), gc);
    gc.gridy = 1;
    grid.add(ElwhaBadge.large("1"), gc);

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
