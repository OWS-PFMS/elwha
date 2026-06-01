package com.owspfm.elwha.navrail.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Library-internal Gallery panels for {@link ElwhaNavRailDestination} — static visual matrices the
 * Showcase mounts in the Nav Rail Destination component's Gallery tab. Mirrors the {@code
 * BadgePlaygroundPanels} / {@code FabPlaygroundPanels} shape so the Showcase Gallery reads
 * identically across components. Story #229.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only because the Showcase
 * lives in a sibling package; consumers must not depend on this type.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.3.0
 */
public final class NavRailDestinationPlaygroundPanels {

  private static final int CELL_GAP = 24;

  private NavRailDestinationPlaygroundPanels() {}

  /**
   * Variants panel — four reference destinations at the locked Collapsed dimensions:
   *
   * <ol>
   *   <li>Unselected, no badge — baseline paint
   *   <li>Selected, no badge — active-indicator pill + fill-1 icon + Secondary label
   *   <li>Unselected, small-dot badge
   *   <li>Selected, large-numeric badge
   * </ol>
   *
   * @return the variants matrix
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildVariantsPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] labels = {
      "Unselected", "Selected", "With small badge", "Selected · large badge"
    };
    for (int c = 0; c < labels.length; c++) {
      gbc.gridx = c;
      gbc.gridy = 0;
      matrix.add(headerLabel(labels[c]), gbc);
    }

    gbc.gridy = 1;
    gbc.gridx = 0;
    matrix.add(destination("widgets", "Home", false, null), gbc);
    gbc.gridx = 1;
    matrix.add(destination("favorite", "Liked", true, null), gbc);
    gbc.gridx = 2;
    matrix.add(destination("visibility", "Watched", false, ElwhaBadge.small()), gbc);
    gbc.gridx = 3;
    matrix.add(destination("layers", "Stacks", true, ElwhaBadge.large(3)), gbc);

    return matrix;
  }

  /**
   * Factory-axis panel — same paint contract, two construction paths. Shows the {@link
   * ElwhaNavRailDestination#of(MaterialIcons.Symbol, String) Symbol factory} (primary) and the
   * {@link ElwhaNavRailDestination#of(javax.swing.Icon, javax.swing.Icon, String) two-Icon escape
   * hatch} side by side using the same glyph.
   *
   * @return the factory-axis matrix
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildFactoryAxisPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridx = 0;
    gbc.gridy = 0;
    matrix.add(headerLabel("of(Symbol, label)"), gbc);
    gbc.gridx = 1;
    matrix.add(headerLabel("of(Icon, Icon, label)"), gbc);

    gbc.gridy = 1;
    gbc.gridx = 0;
    matrix.add(destination("star", "Starred", true, null), gbc);
    gbc.gridx = 1;
    final MaterialIcons.Symbol sym = MaterialIcons.symbol("star");
    final ElwhaNavRailDestination escapeHatch =
        ElwhaNavRailDestination.of(sym.unselected(), sym.selected(), "Starred");
    escapeHatch.setSelected(true);
    matrix.add(escapeHatch, gbc);

    return matrix;
  }

  /**
   * Badge-anchor panel — the same badged destination in both rail states, proving the M3 "Favorites
   * 84" placement: <strong>Collapsed</strong> pins the badge to the icon corner ({@link
   * com.owspfm.elwha.badge.ElwhaBadgeAnchor.AnchorMode#ICON_CORNER ICON_CORNER});
   * <strong>Expanded</strong> moves it to the row's trailing edge ({@link
   * com.owspfm.elwha.badge.ElwhaBadgeAnchor.AnchorMode#TRAILING_EDGE TRAILING_EDGE}). Each
   * destination is hosted in a real {@link ElwhaNavigationRail} so its variant — and therefore the
   * anchor mode — is driven through the production path, not a test hook. Story #300.
   *
   * @return the badge-anchor matrix
   * @version v0.4.0
   * @since v0.4.0
   */
  public static JPanel buildBadgeAnchorPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(CELL_GAP, CELL_GAP, CELL_GAP, CELL_GAP);
    gbc.anchor = GridBagConstraints.NORTH;

    gbc.gridx = 0;
    gbc.gridy = 0;
    matrix.add(headerLabel("Collapsed · icon-corner"), gbc);
    gbc.gridx = 1;
    matrix.add(headerLabel("Expanded · trailing-edge"), gbc);

    gbc.gridy = 1;
    gbc.gridx = 0;
    matrix.add(badgedRail(ElwhaNavigationRail.collapsed()), gbc);
    gbc.gridx = 1;
    matrix.add(badgedRail(ElwhaNavigationRail.expanded()), gbc);

    return matrix;
  }

  // A minimal rail hosting a single selected, badged destination. The rail drives the destination's
  // variant, so the Large badge anchors at the icon corner (Collapsed) or the row trailing edge
  // (Expanded) — the two states shown side by side in buildBadgeAnchorPanel.
  private static ElwhaNavigationRail badgedRail(final ElwhaNavigationRail rail) {
    final ElwhaNavRailDestination dest =
        ElwhaNavRailDestination.of(MaterialIcons.symbol("favorite"), "Favorites");
    dest.setSelected(true);
    rail.setPrimary(List.of(dest));
    dest.setBadge(ElwhaBadge.large(84));
    return rail;
  }

  private static ElwhaNavRailDestination destination(
      final String symbolName, final String label, final boolean selected, final ElwhaBadge badge) {
    final ElwhaNavRailDestination d =
        ElwhaNavRailDestination.of(MaterialIcons.symbol(symbolName), label);
    d.setSelected(selected);
    if (badge != null) {
      d.setBadge(badge);
    }
    return d;
  }

  private static JLabel headerLabel(final String text) {
    final JLabel l = new JLabel(text);
    l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
    return l;
  }
}
