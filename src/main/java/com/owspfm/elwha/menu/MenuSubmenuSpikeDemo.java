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
 * Visual spike for epic #322 S1 — the parent–child overlay <strong>chain</strong>. Opens a root
 * menu containing an {@link ElwhaSubMenuItem} whose nested {@link ElwhaMenu} itself contains a
 * second-level submenu, proving the host mechanics the rest of V2 builds on:
 *
 * <ul>
 *   <li><strong>Right-arrow / click</strong> on a submenu trigger opens its nested menu while the
 *       parent <em>stays open</em>; focus moves into the submenu.
 *   <li><strong>Left-arrow / Esc</strong> closes one level back to its opener (focus restored to
 *       the opener item); the parent remains.
 *   <li>A press <strong>outside every level</strong> closes the whole chain; clicking a
 *       <em>different</em> item in the parent collapses only the submenu.
 *   <li>Selecting a leaf action item closes the entire chain (focus back to the trigger).
 * </ul>
 *
 * Side placement, the {@code ›} caret, hover-intent timers, and the active-state morph are later
 * stories; this spike anchors each submenu with the V1 placement and minimal item.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuSpikeDemo {

  private MenuSubmenuSpikeDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(MenuSubmenuSpikeDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu V2 S1 — submenu chain spike");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(620, 420);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new BorderLayout(0, 16));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    final JLabel status =
        new JLabel(
            "Open the menu, then drive the chain: Right opens a submenu, Left/Esc backs out.");
    status.setForeground(ColorRole.ON_SURFACE.resolve());
    content.add(status, BorderLayout.NORTH);

    final ElwhaButton trigger = ElwhaButton.outlinedButton("Actions");
    trigger.addActionListener(e -> buildRootMenu(status).open(trigger));

    final JPanel center = new JPanel();
    center.setOpaque(false);
    center.add(trigger);
    content.add(center, BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private static ElwhaMenu buildRootMenu(final JLabel status) {
    // Second-level submenu: Share › Email/Link/Embed.
    final ElwhaMenuItem email = ElwhaMenuItem.of(MaterialIcons.add(20), "Email");
    email.addActionListener(e -> status.setText("Activated: Share → Email"));
    final ElwhaMenuItem link = ElwhaMenuItem.of(MaterialIcons.anchor(20), "Copy link");
    link.addActionListener(e -> status.setText("Activated: Share → Copy link"));
    final ElwhaMenuItem embed = ElwhaMenuItem.of(MaterialIcons.layers(20), "Embed");
    embed.addActionListener(e -> status.setText("Activated: Share → Embed"));
    final ElwhaMenu shareMenu =
        ElwhaMenu.builder().addItem(email).addItem(link).addItem(embed).build();

    // First-level submenu trigger nests the share menu plus a sibling leaf.
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of(MaterialIcons.start(20), "Share", shareMenu);
    final ElwhaMenuItem more = ElwhaMenuItem.of(MaterialIcons.help(20), "More options");
    more.addActionListener(e -> status.setText("Activated: More options"));
    final ElwhaMenu moreMenu = ElwhaMenu.builder().addItem(share).addItem(more).build();
    final ElwhaSubMenuItem moreSub =
        ElwhaSubMenuItem.of(MaterialIcons.expandMore(20), "Advanced", moreMenu);

    final ElwhaMenuItem rename = ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename");
    rename.addActionListener(e -> status.setText("Activated: Rename"));
    final ElwhaMenuItem deleteItem = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
    deleteItem.addActionListener(e -> status.setText("Activated: Delete"));

    return ElwhaMenu.builder()
        .addItem(rename)
        .addItem(moreSub)
        .addItem(deleteItem)
        .onClose(cause -> status.setText("Chain closed: " + cause))
        .build();
  }
}
