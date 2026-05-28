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
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #227 smoketest — {@link ElwhaNavRailDestination} Collapsed selected state. Five
 * destinations in a row act as a single-mandatory tab strip — clicking one selects it and clears
 * the others. The grow-from-center indicator animation is a Phase 5 polish item; here the swap is
 * instantaneous (the story's locked contract).
 *
 * <p>What to verify:
 *
 * <ul>
 *   <li>Click any destination → its 32×56 active-indicator pill paints in {@code
 *       SecondaryContainer}; icon swaps to the fill-1 glyph if bundled, tinted {@code
 *       OnSecondaryContainer}; label tints to {@code Secondary}.
 *   <li>Click another destination → previously selected destination reverts to unselected ({@code
 *       OnSurfaceVariant} icon + label, no indicator), new one takes the selection. Swap is snap,
 *       not animated.
 *   <li>Hover/focus/press visuals continue to read correctly over a selected destination — the
 *       state-layer tint switches to {@code OnSecondaryContainer} so the overlay still reads
 *       against the {@code SecondaryContainer} pill.
 *   <li>Mode toggle re-resolves every color through the theme — selected destination remains
 *       legible in dark mode.
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.navrail.playground.ElwhaNavRailDestinationSelectedPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavRailDestinationSelectedPlayground {

  private final JFrame frame = new JFrame("ElwhaNavRailDestination — S3 selected (#227)");
  private final List<ElwhaNavRailDestination> destinations = new ArrayList<>();
  private final JLabel status = new JLabel(" ");

  private ElwhaNavRailDestinationSelectedPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaNavRailDestinationSelectedPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildRow(), BorderLayout.CENTER);
    frame.add(buildStatus(), BorderLayout.SOUTH);
    frame.setSize(640, 220);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildRow() {
    final JPanel row = new JPanel(new GridLayout(1, 5, 0, 0));
    row.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    row.add(destination("widgets", "Home"));
    row.add(destination("favorite", "Liked"));
    row.add(destination("visibility", "Watched"));
    row.add(destination("layers", "Stacks"));
    row.add(destination("star", "Starred"));
    if (!destinations.isEmpty()) {
      select(destinations.get(0));
    }
    return row;
  }

  private ElwhaNavRailDestination destination(final String symbolName, final String label) {
    final ElwhaNavRailDestination d =
        ElwhaNavRailDestination.of(MaterialIcons.symbol(symbolName), label);
    d.addActionListener(e -> select(d));
    destinations.add(d);
    return d;
  }

  private void select(final ElwhaNavRailDestination picked) {
    for (final ElwhaNavRailDestination d : destinations) {
      d.setSelected(d == picked);
    }
    status.setText("selected: " + picked.getLabel());
  }

  private JPanel buildStatus() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    p.add(new JLabel("Tab-strip selection:"));
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
