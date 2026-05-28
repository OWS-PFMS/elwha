package com.owspfm.elwha.navrail.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Shared Gallery panels for the {@link ElwhaNavigationRail} (Phase 2). Surfaced by both the
 * standalone playground ({@link ElwhaNavigationRailPlayground}) and the Showcase's Navigation Rail
 * Gallery tab so both views stay in sync.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class NavigationRailPlaygroundPanels {

  private NavigationRailPlaygroundPanels() {}

  /**
   * Builds the Variants gallery panel — four side-by-side rails demonstrating the chrome knobs:
   * minimal (destinations only), with menu + FAB, with trailing actions, fully populated (menu +
   * FAB + trailing).
   *
   * @return the panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildVariantsPanel() {
    final JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    grid.add(cell("Minimal", railMinimal()));
    grid.add(cell("Menu + FAB", railWithMenuAndFab()));
    grid.add(cell("Trailing actions", railWithTrailing()));
    grid.add(cell("Fully populated", railFull()));
    return grid;
  }

  /**
   * Builds the Surface knobs panel — three side-by-side rails demonstrating the surface knobs:
   * transparent (default), filled, filled + divider + elevation.
   *
   * @return the panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildSurfacePanel() {
    final JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    grid.add(cell("Transparent (default)", railSurface(false, false, 0)));
    grid.add(cell("Filled", railSurface(true, false, 0)));
    grid.add(cell("Filled + divider + elev.", railSurface(true, true, 1)));
    return grid;
  }

  /**
   * Builds the Expanded-variant gallery panel — three side-by-side rails demonstrating the Phase 3
   * Expanded form: primary destinations only, primary + one section, primary + two sections.
   *
   * @return the panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildExpandedPanel() {
    final JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    grid.add(cell("Expanded — primary only", expandedPrimaryOnly()));
    grid.add(cell("Expanded — one section", expandedOneSection()));
    grid.add(cell("Expanded — two sections", expandedTwoSections()));
    return grid;
  }

  private static JPanel expandedPrimaryOnly() {
    return hostRail(buildExpandedBase(0));
  }

  private static JPanel expandedOneSection() {
    return hostRail(buildExpandedBase(1));
  }

  private static JPanel expandedTwoSections() {
    return hostRail(buildExpandedBase(2));
  }

  private static ElwhaNavigationRail buildExpandedBase(final int sectionCount) {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();
    rail.getAccessibleContext().setAccessibleName("Sample navigation");
    rail.setSurfaceFilled(true);
    rail.setDivider(true);
    rail.setExpandedWidth(240);
    rail.setMenuButton(new ElwhaIconButton(MaterialIcons.menuOpen()));
    rail.setFab(ElwhaFab.extended(MaterialIcons.edit(), "Compose"));
    final List<ElwhaNavRailDestination> dests = new ArrayList<>();
    dests.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("widgets"), "Home"));
    final ElwhaNavRailDestination liked =
        ElwhaNavRailDestination.of(MaterialIcons.symbol("favorite"), "Liked");
    liked.setBadge(ElwhaBadge.small());
    dests.add(liked);
    dests.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("visibility"), "Watched"));
    rail.setPrimary(dests);
    if (sectionCount >= 1) {
      final List<ElwhaNavRailDestination> tools = new ArrayList<>();
      tools.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("dark_mode"), "Theme"));
      tools.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("help"), "Help"));
      rail.addSection("Tools", tools);
    }
    if (sectionCount >= 2) {
      final List<ElwhaNavRailDestination> other = new ArrayList<>();
      other.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("info"), "About"));
      other.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("star"), "Sponsor"));
      rail.addSection("Other", other);
    }
    return rail;
  }

  private static JPanel cell(final String title, final JPanel railHost) {
    final JPanel cell = new JPanel(new BorderLayout(0, 8));
    cell.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    final JLabel label = new JLabel(title);
    cell.add(label, BorderLayout.NORTH);
    cell.add(railHost, BorderLayout.CENTER);
    return cell;
  }

  private static JPanel railMinimal() {
    return hostRail(buildBaseRail(false, false, false, false));
  }

  private static JPanel railWithMenuAndFab() {
    return hostRail(buildBaseRail(true, true, false, false));
  }

  private static JPanel railWithTrailing() {
    return hostRail(buildBaseRail(false, false, true, false));
  }

  private static JPanel railFull() {
    return hostRail(buildBaseRail(true, true, true, false));
  }

  private static JPanel railSurface(final boolean filled, final boolean divider, final int elev) {
    final ElwhaNavigationRail rail = buildBaseRail(true, false, false, false);
    rail.setSurfaceFilled(filled);
    rail.setDivider(divider);
    rail.setElevation(elev);
    return hostRail(rail);
  }

  private static ElwhaNavigationRail buildBaseRail(
      final boolean menu, final boolean fab, final boolean trailing, final boolean badge) {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.getAccessibleContext().setAccessibleName("Sample navigation");
    if (menu) {
      rail.setMenuButton(new ElwhaIconButton(MaterialIcons.menu()));
    }
    if (fab) {
      rail.setFab(ElwhaFab.standard(MaterialIcons.edit()));
    }
    final List<ElwhaNavRailDestination> dests = new ArrayList<>();
    dests.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("widgets"), "Home"));
    final ElwhaNavRailDestination liked =
        ElwhaNavRailDestination.of(MaterialIcons.symbol("favorite"), "Liked");
    if (badge) {
      liked.setBadge(ElwhaBadge.small());
    }
    dests.add(liked);
    dests.add(ElwhaNavRailDestination.of(MaterialIcons.symbol("visibility"), "Watched"));
    rail.setPrimary(dests);
    if (trailing) {
      final List<ElwhaIconButton> actions = new ArrayList<>();
      actions.add(new ElwhaIconButton(MaterialIcons.help()));
      actions.add(new ElwhaIconButton(MaterialIcons.info()));
      rail.setTrailingActions(actions);
    }
    return rail;
  }

  // Wraps a rail in a host panel that locks its size to the rail's preferred size so the
  // FlowLayout-arranged Gallery cells render at consistent widths. The rail's preferred height
  // adapts to its chrome/destinations/trailing slot population.
  private static JPanel hostRail(final ElwhaNavigationRail rail) {
    final JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    final Dimension pref = rail.getPreferredSize();
    host.setPreferredSize(pref);
    host.add(rail, BorderLayout.CENTER);
    return host;
  }
}
