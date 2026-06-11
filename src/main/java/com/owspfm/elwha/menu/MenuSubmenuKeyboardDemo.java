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
 * Visual demo for epic #322 S4 — keyboard chain navigation, hover-intent, and nested accessibility.
 * Open the menu and drive the submenu chain entirely by keyboard:
 *
 * <ul>
 *   <li><strong>Up / Down / Home / End / letters</strong> rove within the current level only;
 *   <li><strong>Right / Enter / Space</strong> open the focused submenu and move focus into it;
 *   <li><strong>Left</strong> closes the current submenu back to its opener;
 *   <li><strong>Esc</strong> closes the current level (the menu at the root).
 * </ul>
 *
 * Hover works too: dwell over a submenu row to open it, and the submenu stays open while the
 * pointer is over it or the opener (no flicker across the boundary). A screen reader announces each
 * level as a pop-up menu and the trigger as expandable/expanded.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuKeyboardDemo {

  private MenuSubmenuKeyboardDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(MenuSubmenuKeyboardDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu V2 S4 — keyboard chain + a11y");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 460);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new BorderLayout(0, 16));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    final JLabel status =
        new JLabel(
            "Open the menu, then: Right opens a submenu, Left backs out, Up/Down rove a level.");
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
    final ElwhaMenuItem email = ElwhaMenuItem.of(MaterialIcons.add(20), "Email");
    email.addActionListener(e -> status.setText("Activated: Share → Email"));
    final ElwhaMenuItem link = ElwhaMenuItem.of(MaterialIcons.anchor(20), "Copy link");
    link.addActionListener(e -> status.setText("Activated: Share → Copy link"));
    final ElwhaMenuItem embed = ElwhaMenuItem.of(MaterialIcons.layers(20), "Embed");
    embed.addActionListener(e -> status.setText("Activated: Share → Embed"));
    final ElwhaMenu shareMenu =
        ElwhaMenu.builder().addItem(email).addItem(link).addItem(embed).build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of(MaterialIcons.start(20), "Share", shareMenu);

    final ElwhaMenuItem rename = ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename");
    rename.addActionListener(e -> status.setText("Activated: Rename"));
    final ElwhaMenuItem move = ElwhaMenuItem.of(MaterialIcons.start(20), "Move");
    move.addActionListener(e -> status.setText("Activated: Move"));
    final ElwhaMenuItem deleteItem = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
    deleteItem.addActionListener(e -> status.setText("Activated: Delete"));

    return ElwhaMenu.builder()
        .addItem(rename)
        .addItem(move)
        .addItem(share)
        .addItem(deleteItem)
        .onClose(cause -> status.setText("Closed: " + cause))
        .build();
  }
}
