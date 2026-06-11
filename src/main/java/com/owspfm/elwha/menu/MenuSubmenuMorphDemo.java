package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual demo for epic #322 S3 — the active-state corner shape-morph. Open the menu and step into
 * the nested submenus (hover, click, or Right): watch the <strong>focused</strong> level round its
 * corners up (to {@code LG}) while the level you stepped out of <strong>squares off</strong> (to
 * {@code SM}); stepping back (Left / Esc) animates the swap in reverse. A three-level chain
 * (Advanced › Share › … ) shows several levels morphing at once. Reduced motion snaps to the end
 * shape.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuMorphDemo {

  private MenuSubmenuMorphDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(MenuSubmenuMorphDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu V2 S3 — active-state shape morph");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 460);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new BorderLayout(0, 16));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    final JLabel status =
        new JLabel(
            "Step into a submenu — the focused level rounds up, the one behind it squares off.");
    status.setForeground(ColorRole.ON_SURFACE.resolve());
    content.add(status, BorderLayout.NORTH);

    final ElwhaButton trigger = ElwhaButton.outlinedButton("Actions");
    trigger.addActionListener(e -> buildMenu(status).open(trigger));

    final JPanel center = new JPanel();
    center.setOpaque(false);
    center.add(trigger);
    content.add(center, BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private static ElwhaMenu buildMenu(final JLabel status) {
    final ElwhaMenuItem leafA = ElwhaMenuItem.of(MaterialIcons.add(20), "Email");
    leafA.addActionListener(e -> status.setText("Activated: Advanced → Share → Email"));
    final ElwhaMenuItem leafB = ElwhaMenuItem.of(MaterialIcons.anchor(20), "Copy link");
    leafB.addActionListener(e -> status.setText("Activated: Advanced → Share → Copy link"));
    final ElwhaMenu shareMenu = ElwhaMenu.builder().addItem(leafA).addItem(leafB).build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of(MaterialIcons.start(20), "Share", shareMenu);

    final ElwhaMenuItem exportItem = ElwhaMenuItem.of(MaterialIcons.layers(20), "Export");
    exportItem.addActionListener(e -> status.setText("Activated: Advanced → Export"));
    final ElwhaMenu advancedMenu = ElwhaMenu.builder().addItem(share).addItem(exportItem).build();
    final ElwhaSubMenuItem advanced =
        ElwhaSubMenuItem.of(MaterialIcons.expandMore(20), "Advanced", advancedMenu);

    final ElwhaMenuItem rename = ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename");
    rename.addActionListener(e -> status.setText("Activated: Rename"));
    final ElwhaMenuItem deleteItem = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
    deleteItem.addActionListener(e -> status.setText("Activated: Delete"));

    return ElwhaMenu.builder()
        .addItem(rename)
        .addItem(advanced)
        .addItem(deleteItem)
        .onClose(cause -> status.setText("Closed: " + cause))
        .build();
  }
}
