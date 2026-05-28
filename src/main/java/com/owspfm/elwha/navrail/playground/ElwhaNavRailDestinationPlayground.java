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
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #229 — the per-component consolidated playground for {@link ElwhaNavRailDestination}.
 * Replaces ad-hoc per-story playgrounds with a single Gallery + interactive view: every Phase 1
 * surface (both factories, selected ↔ unselected, badge variants, hover / focus / press) reachable
 * from one window.
 *
 * <p>Layout:
 *
 * <ul>
 *   <li><strong>Gallery</strong> tab — static reference matrices from {@link
 *       NavRailDestinationPlaygroundPanels} (Variants, Factory axis). These are exactly what the
 *       Showcase mounts in the Component's Gallery tab.
 *   <li><strong>Interactive</strong> tab — five destinations wired as a single-mandatory tab strip
 *       (mirroring story #227's smoketest) plus a per-destination badge picker (small / large
 *       numeric / large overflow / none) so the badge slot from story #228 can be exercised against
 *       the live selection state.
 * </ul>
 *
 * <p>Mode toggle re-installs the theme so every cell re-resolves through the {@link Mode}.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.navrail.playground.ElwhaNavRailDestinationPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavRailDestinationPlayground {

  private final JFrame frame = new JFrame("ElwhaNavRailDestination — playground (#229)");

  private ElwhaNavRailDestinationPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaNavRailDestinationPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildTabs(), BorderLayout.CENTER);
    frame.setSize(880, 520);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JTabbedPane buildTabs() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Gallery", buildGallery());
    tabs.addTab("Interactive", buildInteractive());
    return tabs;
  }

  private JPanel buildGallery() {
    final JPanel column = new JPanel();
    column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
    column.add(section("Variants", NavRailDestinationPlaygroundPanels.buildVariantsPanel()));
    column.add(section("Factory axis", NavRailDestinationPlaygroundPanels.buildFactoryAxisPanel()));
    return column;
  }

  private JPanel section(final String title, final JPanel body) {
    final JPanel section = new JPanel(new BorderLayout());
    final JLabel heading = new JLabel(title);
    heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD));
    heading.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
    section.add(heading, BorderLayout.NORTH);
    section.add(body, BorderLayout.CENTER);
    return section;
  }

  private JPanel buildInteractive() {
    final java.util.List<ElwhaNavRailDestination> destinations = new java.util.ArrayList<>();
    final JPanel row = new JPanel(new GridLayout(1, 5, 0, 0));
    row.setBorder(BorderFactory.createEmptyBorder(28, 24, 16, 24));
    addInteractive(row, destinations, "widgets", "Home", null);
    addInteractive(row, destinations, "favorite", "Liked", ElwhaBadge.small());
    addInteractive(row, destinations, "visibility", "Watched", ElwhaBadge.large(3));
    addInteractive(row, destinations, "layers", "Stacks", null);
    addInteractive(row, destinations, "star", "Starred", ElwhaBadge.large("999+"));
    destinations.get(0).setSelected(true);

    final JPanel container = new JPanel(new BorderLayout());
    container.add(row, BorderLayout.CENTER);
    container.add(buildToggleBar(destinations), BorderLayout.SOUTH);
    return container;
  }

  private void addInteractive(
      final JPanel row,
      final java.util.List<ElwhaNavRailDestination> destinations,
      final String symbolName,
      final String label,
      final ElwhaBadge badge) {
    final ElwhaNavRailDestination d =
        ElwhaNavRailDestination.of(MaterialIcons.symbol(symbolName), label);
    if (badge != null) {
      d.setBadge(badge);
    }
    d.addActionListener(
        e -> {
          for (final ElwhaNavRailDestination other : destinations) {
            other.setSelected(other == d);
          }
        });
    destinations.add(d);
    row.add(d);
  }

  private JPanel buildToggleBar(final java.util.List<ElwhaNavRailDestination> destinations) {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
    final JToggleButton clearBadges = new JToggleButton("Clear all badges");
    clearBadges.addActionListener(
        e -> {
          if (clearBadges.isSelected()) {
            for (final ElwhaNavRailDestination d : destinations) {
              d.setBadge(null);
            }
          } else {
            // Restore the original badge config from initial wiring.
            destinations.get(1).setBadge(ElwhaBadge.small());
            destinations.get(2).setBadge(ElwhaBadge.large(3));
            destinations.get(4).setBadge(ElwhaBadge.large("999+"));
          }
        });
    bar.add(clearBadges);
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
