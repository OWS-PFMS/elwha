package com.owspfm.elwha.menu;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Epic #298 S8 dogfood sanity-check — proves the unblock target [#238](
 * https://github.com/OWS-PFMS/elwha/issues/238): an {@link ElwhaNavigationRail} trailing-action
 * overflow button consumes {@link ElwhaMenu#open(java.awt.Component)} for its actions menu. The
 * rail's trailing ⋮ {@link ElwhaIconButton} (SELECTABLE, so it shows pressed-while-open) opens an
 * anchored {@code ElwhaMenu} — exactly the generic vertical popover #238 needs.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class NavRailOverflowMenuDemo {

  private NavRailOverflowMenuDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(NavRailOverflowMenuDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu S8 — #238 Nav-Rail overflow dogfood");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 520);
    frame.setLocationRelativeTo(null);

    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.getAccessibleContext().setAccessibleName("Primary navigation");
    rail.setMenuButton(new ElwhaIconButton(MaterialIcons.menu()));
    rail.setPrimary(
        List.of(
            ElwhaNavRailDestination.of(MaterialIcons.symbol("home"), "Home"),
            ElwhaNavRailDestination.of(MaterialIcons.symbol("star"), "Starred"),
            ElwhaNavRailDestination.of(MaterialIcons.symbol("layers"), "Files")));

    final JLabel status =
        new JLabel("Click the ⋮ overflow action at the rail's foot.", SwingConstants.CENTER);
    status.setForeground(ColorRole.ON_SURFACE.resolve());

    // The #238 overflow trigger: a trailing-action icon button that opens an ElwhaMenu.
    final ElwhaIconButton overflow =
        new ElwhaIconButton(MaterialIcons.moreVert())
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    overflow.addActionListener(e -> overflowMenu(status).open(overflow));
    rail.setTrailingActions(List.of(overflow));

    final JPanel content = new JPanel(new BorderLayout());
    content.setBackground(ColorRole.SURFACE.resolve());
    content.add(rail, BorderLayout.WEST);
    final JPanel body = new JPanel(new BorderLayout());
    body.setOpaque(false);
    body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    body.add(status, BorderLayout.NORTH);
    content.add(body, BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private static ElwhaMenu overflowMenu(final JLabel status) {
    final ElwhaMenuItem settings = ElwhaMenuItem.of(MaterialIcons.palette(20), "Settings");
    settings.addActionListener(e -> status.setText("Overflow: Settings"));
    final ElwhaMenuItem help = ElwhaMenuItem.of(MaterialIcons.help(20), "Help");
    help.addActionListener(e -> status.setText("Overflow: Help"));
    final ElwhaMenuItem about = ElwhaMenuItem.of(MaterialIcons.info(20), "About");
    about.addActionListener(e -> status.setText("Overflow: About"));
    return ElwhaMenu.builder()
        .addItem(settings)
        .addItem(help)
        .addItem(about)
        .onClose(cause -> status.setText("Overflow menu closed: " + cause))
        .build();
  }
}
