package com.owspfm.elwha.navrail.playground;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #226 smoketest — {@link ElwhaNavRailDestination} Collapsed skeleton. Renders three side-by-
 * side destinations against a neutral background so the pill-shaped state layer is visible on hover
 * and focus, and so the press ripple animates inside the pill. No selected state yet (story #227),
 * no badge (story #228).
 *
 * <p>What to verify:
 *
 * <ul>
 *   <li>Icon + label layout matches the design doc §7.1 — icon centered in a 32×56 pill region,
 *       label centered 4 dp below.
 *   <li>Hover over an icon → pill-shaped tint appears behind it. Hover lateral to the icon (still
 *       inside the 96 dp row) → no pill tint, but the hit target still reads. (Click test below
 *       confirms the row-wide hit target.)
 *   <li>Tab to focus a destination → focus ring traces the pill outline.
 *   <li>Click anywhere in the 96 dp row → action fires (label appears in the bottom status line),
 *       ripple animates from the click point clipped to the pill.
 *   <li>Mode toggle re-tints icon + label without breaking the state-layer paint.
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.navrail.playground.ElwhaNavRailDestinationSkeletonPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavRailDestinationSkeletonPlayground {

  private final JFrame frame = new JFrame("ElwhaNavRailDestination — S2 skeleton (#226)");
  private final JLabel status = new JLabel(" ");

  private ElwhaNavRailDestinationSkeletonPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaNavRailDestinationSkeletonPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildRow(), BorderLayout.CENTER);
    frame.add(buildStatus(), BorderLayout.SOUTH);
    frame.setSize(420, 220);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildRow() {
    final JPanel row = new JPanel(new GridLayout(1, 3, 0, 0));
    row.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    row.add(destination("widgets", "Home"));
    row.add(destination("favorite", "Liked"));
    row.add(destination("visibility", "Watched"));
    return row;
  }

  private ElwhaNavRailDestination destination(final String symbolName, final String label) {
    final ElwhaNavRailDestination d =
        ElwhaNavRailDestination.of(MaterialIcons.symbol(symbolName), label);
    d.addActionListener(e -> status.setText("clicked: " + label));
    return d;
  }

  private JPanel buildStatus() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    p.add(new JLabel("Last activation:"));
    p.add(status);
    return p;
  }

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    bar.add(new JLabel("Mode:"));
    final ButtonGroup group = new ButtonGroup();
    for (final Mode mode : Mode.values()) {
      final JToggleButton b = new JToggleButton(mode.name());
      b.setSelected(mode == Mode.SYSTEM);
      b.addActionListener(e -> applyMode(mode));
      group.add(b);
      bar.add(b);
    }
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
