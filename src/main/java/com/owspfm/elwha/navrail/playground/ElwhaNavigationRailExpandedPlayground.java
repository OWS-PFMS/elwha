package com.owspfm.elwha.navrail.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #244 — Phase 3 playground for {@link ElwhaNavigationRail}, focused on the new Expanded
 * variant + Collapsed↔Expanded morph. Per the operator's fresh-demo-per-story doctrine this is a
 * brand-new artifact, not an extension of {@link ElwhaNavigationRailPlayground} (which remains the
 * Phase 2 Collapsed-only smoketest).
 *
 * <p>Layout: rail docks on the leading edge; the content panel right of it shows the selected
 * destination + a selection log; the controls bar at the bottom drives the Phase 3 surface —
 * Variant radio (Collapsed / Expanded triggers {@link ElwhaNavigationRail#morphTo(Variant)}), an
 * Expanded-width slider (220–360 dp), a Sections toggle (adds two sample sections in Expanded),
 * plus the Phase 2 knobs (surface fill, divider, elevation, menu button, FAB, trailing actions,
 * destination count, badge slot).
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.navrail.playground.ElwhaNavigationRailExpandedPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavigationRailExpandedPlayground {

  private final JFrame frame = new JFrame("ElwhaNavigationRail — Phase 3 playground (#244)");
  private final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
  private final JLabel selectedLabel = new JLabel(" ", SwingConstants.CENTER);
  private final JTextArea log = new JTextArea(8, 40);

  private ElwhaNavigationRailExpandedPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaNavigationRailExpandedPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Interactive", buildInteractiveTab());
    final JScrollPane galleryScroll = new JScrollPane(buildGalleryTab());
    galleryScroll.getVerticalScrollBar().setUnitIncrement(16);
    galleryScroll.setBorder(BorderFactory.createEmptyBorder());
    tabs.addTab("Gallery", galleryScroll);
    frame.add(tabs, BorderLayout.CENTER);

    rail.getAccessibleContext().setAccessibleName("Primary navigation");
    rail.setSurfaceFilled(true);
    rail.setDivider(true);
    rail.setMenuButton(new ElwhaIconButton(MaterialIcons.menu()));
    rail.setFab(ElwhaFab.extended(MaterialIcons.edit(), "Compose"));
    applyDestinationCount(5);

    rail.addSelectionListener(
        (prev, cur) -> {
          final String prevLabel = prev == null ? "(none)" : prev.getLabel();
          final String curLabel = cur == null ? "(none)" : cur.getLabel();
          log.append("Selection: " + prevLabel + " → " + curLabel + "\n");
          log.setCaretPosition(log.getDocument().getLength());
          selectedLabel.setText("Selected: " + curLabel);
        });
    rail.addPropertyChangeListener(
        ElwhaNavigationRail.PROPERTY_SELECTED,
        e -> {
          // no-op — selection listener handles the log
        });

    selectedLabel.setText(
        "Selected: " + (rail.getSelected() == null ? "(none)" : rail.getSelected().getLabel()));

    frame.setSize(1100, 760);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildInteractiveTab() {
    final JPanel root = new JPanel(new BorderLayout());
    final JPanel center = new JPanel(new BorderLayout());
    center.add(rail, BorderLayout.WEST);
    center.add(buildContent(), BorderLayout.CENTER);
    root.add(center, BorderLayout.CENTER);
    root.add(buildControlsPanel(), BorderLayout.SOUTH);
    return root;
  }

  private JPanel buildGalleryTab() {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.add(galleryHeading("Variants"));
    column.add(NavigationRailPlaygroundPanels.buildVariantsPanel());
    column.add(galleryHeading("Expanded variant"));
    column.add(NavigationRailPlaygroundPanels.buildExpandedPanel());
    column.add(galleryHeading("Surface knobs"));
    column.add(NavigationRailPlaygroundPanels.buildSurfacePanel());
    return column;
  }

  private JLabel galleryHeading(final String text) {
    final JLabel heading = new JLabel(text);
    heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 14f));
    heading.setBorder(BorderFactory.createEmptyBorder(16, 20, 4, 20));
    heading.setAlignmentX(0f);
    return heading;
  }

  private JPanel buildContent() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    selectedLabel.setFont(selectedLabel.getFont().deriveFont(java.awt.Font.BOLD, 20f));
    panel.add(selectedLabel, BorderLayout.NORTH);

    log.setEditable(false);
    log.setLineWrap(true);
    log.setWrapStyleWord(true);
    log.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    final JScrollPane scroll = new JScrollPane(log);
    scroll.setBorder(BorderFactory.createTitledBorder("Selection + variant log"));
    panel.add(scroll, BorderLayout.CENTER);
    return panel;
  }

  private JPanel buildControlsPanel() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
    panel.add(buildVariantRow());
    panel.add(buildSurfaceRow());
    panel.add(buildContentRow());
    return panel;
  }

  private JPanel buildVariantRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 4));
    row.add(new JLabel("Variant:"));

    final JRadioButton collapsedBtn = new JRadioButton("Collapsed", true);
    final JRadioButton expandedBtn = new JRadioButton("Expanded");
    final ButtonGroup grp = new ButtonGroup();
    grp.add(collapsedBtn);
    grp.add(expandedBtn);
    collapsedBtn.addActionListener(
        e -> {
          rail.morphTo(ElwhaNavigationRail.Variant.COLLAPSED);
          log.append("Variant → COLLAPSED (morph)\n");
        });
    expandedBtn.addActionListener(
        e -> {
          rail.morphTo(ElwhaNavigationRail.Variant.EXPANDED);
          log.append("Variant → EXPANDED (morph)\n");
        });
    rail.addPropertyChangeListener(
        ElwhaNavigationRail.PROPERTY_VARIANT,
        e -> {
          final ElwhaNavigationRail.Variant v = (ElwhaNavigationRail.Variant) e.getNewValue();
          if (v == ElwhaNavigationRail.Variant.EXPANDED) {
            expandedBtn.setSelected(true);
          } else {
            collapsedBtn.setSelected(true);
          }
        });
    row.add(collapsedBtn);
    row.add(expandedBtn);

    row.add(Box.createHorizontalStrut(16));
    row.add(new JLabel("Expanded width:"));
    final JSlider widthSlider = new JSlider(220, 360, 256);
    widthSlider.setPreferredSize(
        new java.awt.Dimension(180, widthSlider.getPreferredSize().height));
    widthSlider.setMajorTickSpacing(70);
    widthSlider.setPaintTicks(true);
    final JLabel widthLabel = new JLabel("256 px");
    widthSlider.addChangeListener(
        e -> {
          final int v = widthSlider.getValue();
          rail.setExpandedWidth(v);
          widthLabel.setText(v + " px");
        });
    row.add(widthSlider);
    row.add(widthLabel);

    row.add(Box.createHorizontalStrut(16));
    final JCheckBox sectionsCb = new JCheckBox("Sections");
    sectionsCb.addActionListener(
        e -> {
          if (sectionsCb.isSelected()) {
            installSampleSections();
            log.append("Sections: ADDED\n");
          } else {
            rail.clearSections();
            log.append("Sections: CLEARED\n");
          }
        });
    row.add(sectionsCb);

    return row;
  }

  private JPanel buildSurfaceRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 4));
    final JCheckBox surface = new JCheckBox("Surface filled", true);
    surface.addActionListener(e -> rail.setSurfaceFilled(surface.isSelected()));
    row.add(surface);

    final JCheckBox divider = new JCheckBox("Divider", true);
    divider.addActionListener(e -> rail.setDivider(divider.isSelected()));
    row.add(divider);

    final JCheckBox elevation = new JCheckBox("Elevation 1");
    elevation.addActionListener(e -> rail.setElevation(elevation.isSelected() ? 1 : 0));
    row.add(elevation);

    final JCheckBox menu = new JCheckBox("Menu button", true);
    menu.addActionListener(
        e ->
            rail.setMenuButton(
                menu.isSelected() ? new ElwhaIconButton(MaterialIcons.menu()) : null));
    row.add(menu);

    final JCheckBox fab = new JCheckBox("FAB", true);
    fab.addActionListener(
        e ->
            rail.setFab(
                fab.isSelected() ? ElwhaFab.extended(MaterialIcons.edit(), "Compose") : null));
    row.add(fab);

    final JCheckBox trailing = new JCheckBox("Trailing actions");
    trailing.addActionListener(
        e -> {
          if (trailing.isSelected()) {
            final List<ElwhaIconButton> actions = new ArrayList<>();
            actions.add(new ElwhaIconButton(MaterialIcons.help()));
            actions.add(new ElwhaIconButton(MaterialIcons.info()));
            rail.setTrailingActions(actions);
          } else {
            rail.setTrailingActions(null);
          }
        });
    row.add(trailing);
    return row;
  }

  private JPanel buildContentRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 4));
    row.add(new JLabel("Destinations:"));
    final JComboBox<Integer> count = new JComboBox<>(new Integer[] {3, 5, 7});
    count.setSelectedItem(5);
    count.addActionListener(e -> applyDestinationCount((Integer) count.getSelectedItem()));
    row.add(count);

    row.add(Box.createHorizontalStrut(16));
    row.add(new JLabel("Badge on \"Liked\":"));
    final JComboBox<String> badge = new JComboBox<>(new String[] {"None", "Small dot", "Large 3"});
    badge.setSelectedIndex(1);
    badge.addActionListener(
        e -> {
          final List<ElwhaNavRailDestination> dests = rail.getPrimary();
          if (dests.size() < 2) {
            return;
          }
          final ElwhaNavRailDestination liked = dests.get(1);
          switch ((String) badge.getSelectedItem()) {
            case "Small dot" -> liked.setBadge(ElwhaBadge.small());
            case "Large 3" -> liked.setBadge(ElwhaBadge.large(3));
            default -> liked.setBadge(null);
          }
        });
    row.add(badge);
    return row;
  }

  private void applyDestinationCount(final int n) {
    final String[][] table = {
      {"widgets", "Home"},
      {"favorite", "Liked"},
      {"visibility", "Watched"},
      {"layers", "Stacks"},
      {"star", "Starred"},
      {"info", "About"},
      {"help", "Help"},
    };
    final List<ElwhaNavRailDestination> dests = new ArrayList<>();
    for (int i = 0; i < n && i < table.length; i++) {
      final ElwhaNavRailDestination d =
          ElwhaNavRailDestination.of(MaterialIcons.symbol(table[i][0]), table[i][1]);
      dests.add(d);
    }
    if (dests.size() >= 2) {
      dests.get(1).setBadge(ElwhaBadge.small());
    }
    rail.setPrimary(dests);
  }

  private void installSampleSections() {
    rail.clearSections();
    final List<ElwhaNavRailDestination> tools = new ArrayList<>();
    tools.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("dark_mode"), "Theme"));
    tools.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("help"), "Help"));
    rail.addSection("Tools", tools);

    final List<ElwhaNavRailDestination> other = new ArrayList<>();
    other.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("info"), "About"));
    other.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("star"), "Sponsor"));
    rail.addSection("Other", other);
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
