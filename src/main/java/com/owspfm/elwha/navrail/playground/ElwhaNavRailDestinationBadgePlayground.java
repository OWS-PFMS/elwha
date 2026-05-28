package com.owspfm.elwha.navrail.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
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
 * Story #228 smoketest — {@link ElwhaNavRailDestination} badge slot. Four destinations cover the
 * Collapsed badge content range:
 *
 * <ol>
 *   <li>No badge (control / reference layout)
 *   <li>Small dot ({@link ElwhaBadge#small()})
 *   <li>Large numeric ({@link ElwhaBadge#large(int)})
 *   <li>Large capped overflow ({@link ElwhaBadge#large(String)} with "999+")
 * </ol>
 *
 * <p>What to verify:
 *
 * <ul>
 *   <li>Each badge is placed upper-right of the icon glyph, tracking icon bounds (resize the window
 *       — the badge stays anchored).
 *   <li>Small badge clears the 32×56 indicator pill outline without overlap; large badge sits a few
 *       pixels further from the icon to clear its own rounded container.
 *   <li>Toggle the row's "selected" radio → indicator paints behind the badged icon without z-order
 *       issues (badge stays above the pill).
 *   <li>Detach button clears the badge cleanly (cuts the listener wiring); re-attach restores it.
 *   <li>Mode toggle re-resolves badge colors.
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.navrail.playground.ElwhaNavRailDestinationBadgePlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavRailDestinationBadgePlayground {

  private final JFrame frame = new JFrame("ElwhaNavRailDestination — S4 badge (#228)");
  private ElwhaNavRailDestination destNone;
  private ElwhaNavRailDestination destDot;
  private ElwhaNavRailDestination destNumeric;
  private ElwhaNavRailDestination destOverflow;

  private ElwhaNavRailDestinationBadgePlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaNavRailDestinationBadgePlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildRow(), BorderLayout.CENTER);
    frame.add(buildToggleBar(), BorderLayout.SOUTH);
    frame.setSize(560, 260);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildRow() {
    final JPanel row = new JPanel(new GridLayout(1, 4, 0, 0));
    row.setBorder(BorderFactory.createEmptyBorder(28, 20, 12, 20));
    destNone = ElwhaNavRailDestination.of(MaterialIcons.symbol("widgets"), "Home");
    destDot = ElwhaNavRailDestination.of(MaterialIcons.symbol("favorite"), "Liked");
    destNumeric = ElwhaNavRailDestination.of(MaterialIcons.symbol("visibility"), "Watched");
    destOverflow = ElwhaNavRailDestination.of(MaterialIcons.symbol("layers"), "Stacks");

    destDot.setBadge(ElwhaBadge.small());
    destNumeric.setBadge(ElwhaBadge.large(3));
    destOverflow.setBadge(ElwhaBadge.large("999+"));

    row.add(destNone);
    row.add(destDot);
    row.add(destNumeric);
    row.add(destOverflow);
    return row;
  }

  private JPanel buildToggleBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    final JToggleButton selectedToggle = new JToggleButton("Toggle selected");
    selectedToggle.addActionListener(
        e -> {
          final boolean on = selectedToggle.isSelected();
          destNone.setSelected(on);
          destDot.setSelected(on);
          destNumeric.setSelected(on);
          destOverflow.setSelected(on);
        });
    final JToggleButton detachToggle = new JToggleButton("Detach all badges");
    detachToggle.addActionListener(
        e -> {
          final boolean detach = detachToggle.isSelected();
          if (detach) {
            destDot.setBadge(null);
            destNumeric.setBadge(null);
            destOverflow.setBadge(null);
          } else {
            destDot.setBadge(ElwhaBadge.small());
            destNumeric.setBadge(ElwhaBadge.large(3));
            destOverflow.setBadge(ElwhaBadge.large("999+"));
          }
        });
    bar.add(selectedToggle);
    bar.add(detachToggle);
    return bar;
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
