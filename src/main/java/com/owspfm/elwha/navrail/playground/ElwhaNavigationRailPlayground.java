package com.owspfm.elwha.navrail.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #237 — the playground for {@link ElwhaNavigationRail} (Collapsed only, Phase 2).
 *
 * <p>Layout: the rail docks on the leading edge; a content area on the right shows which
 * destination is selected and an event log of selection changes; a controls bar at the bottom
 * exercises every knob in the rail's Phase 2 surface (surface fill, divider, elevation, menu button
 * presence, FAB presence, trailing-actions count, primary-destination count, badge slot on a single
 * destination).
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.navrail.playground.ElwhaNavigationRailPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ElwhaNavigationRailPlayground {

  private final JFrame frame = new JFrame("ElwhaNavigationRail — playground (#237)");
  private final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
  private final JLabel selectedLabel = new JLabel(" ", SwingConstants.CENTER);
  private final JTextArea log = new JTextArea(8, 40);

  private ElwhaNavigationRailPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaNavigationRailPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Interactive", buildInteractiveTab());
    tabs.addTab("Gallery", buildGalleryTab());
    frame.add(tabs, BorderLayout.CENTER);

    rail.getAccessibleContext().setAccessibleName("Primary navigation");
    applyDestinationCount(5);
    rail.addSelectionListener(
        (prev, cur) -> {
          final String prevLabel = prev == null ? "(none)" : prev.getLabel();
          final String curLabel = cur == null ? "(none)" : cur.getLabel();
          log.append("Selection: " + prevLabel + " → " + curLabel + "\n");
          log.setCaretPosition(log.getDocument().getLength());
          selectedLabel.setText("Selected: " + curLabel);
        });

    selectedLabel.setText(
        "Selected: " + (rail.getSelected() == null ? "(none)" : rail.getSelected().getLabel()));

    frame.setSize(1000, 720);
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
    scroll.setBorder(BorderFactory.createTitledBorder("Selection log"));
    panel.add(scroll, BorderLayout.CENTER);
    return panel;
  }

  private JPanel buildControlsPanel() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));

    final JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 4));
    final ElwhaCheckbox surface = new ElwhaCheckbox("Surface filled");
    surface.addActionListener(e -> rail.setSurfaceFilled(surface.isChecked()));
    row1.add(surface);

    final ElwhaCheckbox divider = new ElwhaCheckbox("Divider");
    divider.addActionListener(e -> rail.setDivider(divider.isChecked()));
    row1.add(divider);

    final ElwhaCheckbox elevation = new ElwhaCheckbox("Elevation 1");
    elevation.addActionListener(e -> rail.setElevation(elevation.isChecked() ? 1 : 0));
    row1.add(elevation);

    final ElwhaCheckbox menu = new ElwhaCheckbox("Menu button");
    menu.addActionListener(
        e ->
            rail.setMenuButton(
                menu.isChecked() ? new ElwhaIconButton(MaterialIcons.menu()) : null));
    row1.add(menu);

    final ElwhaCheckbox fab = new ElwhaCheckbox("FAB");
    fab.addActionListener(
        e -> rail.setFab(fab.isChecked() ? ElwhaFab.standard(MaterialIcons.edit()) : null));
    row1.add(fab);

    final ElwhaCheckbox trailing = new ElwhaCheckbox("Trailing actions");
    trailing.addActionListener(
        e -> {
          if (trailing.isChecked()) {
            final List<ElwhaIconButton> actions = new ArrayList<>();
            actions.add(new ElwhaIconButton(MaterialIcons.help()));
            actions.add(new ElwhaIconButton(MaterialIcons.info()));
            rail.setTrailingActions(actions);
          } else {
            rail.setTrailingActions(null);
          }
        });
    row1.add(trailing);

    panel.add(row1);

    final JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 4));
    final ElwhaSelectField<Integer> count = ElwhaSelectField.outlined("Destinations");
    count.setOptions(List.of(3, 5, 7));
    count.setSelectedValue(5);
    count.addSelectionChangeListener(this::applyDestinationCount);
    row2.add(count);

    row2.add(Box.createHorizontalStrut(16));
    final ElwhaSelectField<String> badge = ElwhaSelectField.outlined("Badge on \"Liked\"");
    badge.setOptions(List.of("None", "Small dot", "Large 3"));
    badge.setSelectedValue("Small dot");
    badge.addSelectionChangeListener(
        picked -> {
          final List<ElwhaNavRailDestination> dests = rail.getPrimary();
          if (dests.size() < 2) {
            return;
          }
          final ElwhaNavRailDestination liked = dests.get(1);
          switch (picked) {
            case "Small dot" -> liked.setBadge(ElwhaBadge.small());
            case "Large 3" -> liked.setBadge(ElwhaBadge.large(3));
            default -> liked.setBadge(null);
          }
        });
    row2.add(badge);

    panel.add(row2);
    return panel;
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

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    bar.add(new JLabel("Mode:"));
    final Mode[] modes = Mode.values();
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setColorStyle(ButtonGroupColorStyle.OUTLINED);
    for (final Mode mode : modes) {
      group.add(mode.name());
    }
    group.setSelectedIndex(Mode.SYSTEM.ordinal());
    group.addSelectionListener(g -> applyMode(modes[g.getSelectedIndex()]));
    bar.add(group);
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
