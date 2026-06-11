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
 * Visual demo for epic #322 S2 — the full {@link ElwhaSubMenuItem} primitive: the trailing {@code
 * ›} caret, hover-intent open/close (400 ms), click and Right-arrow open, and side placement with
 * the trailing→leading flip. Two triggers are pinned to opposite window edges so the submenu opens
 * to the right from the left trigger and <em>flips left</em> from the right trigger; a second-level
 * submenu shows the chain. Hover a submenu row to dwell-open it, or click / press Right.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuPlacementDemo {

  private MenuSubmenuPlacementDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(MenuSubmenuPlacementDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu V2 S2 — caret + side placement");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 460);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new BorderLayout(0, 16));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    final JLabel status =
        new JLabel("Hover a submenu row (400 ms) or click / Right to open; watch the side-flip.");
    status.setForeground(ColorRole.ON_SURFACE.resolve());
    content.add(status, BorderLayout.NORTH);

    final ElwhaButton left = ElwhaButton.outlinedButton("Left trigger");
    left.addActionListener(e -> buildMenu(status).open(left));
    final ElwhaButton right = ElwhaButton.filledTonalButton("Right trigger");
    right.addActionListener(e -> buildMenu(status).open(right));

    final JPanel row = new JPanel(new BorderLayout());
    row.setOpaque(false);
    final JPanel leftWrap = new JPanel();
    leftWrap.setOpaque(false);
    leftWrap.add(left);
    final JPanel rightWrap = new JPanel();
    rightWrap.setOpaque(false);
    rightWrap.add(right);
    row.add(leftWrap, BorderLayout.WEST);
    row.add(rightWrap, BorderLayout.EAST);
    content.add(row, BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private static ElwhaMenu buildMenu(final JLabel status) {
    final ElwhaMenuItem email = ElwhaMenuItem.of(MaterialIcons.add(20), "Email");
    email.addActionListener(e -> status.setText("Activated: Share → Email"));
    final ElwhaMenuItem link = ElwhaMenuItem.of(MaterialIcons.anchor(20), "Copy link");
    link.addActionListener(e -> status.setText("Activated: Share → Copy link"));
    final ElwhaMenu shareMenu = ElwhaMenu.builder().addItem(email).addItem(link).build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of(MaterialIcons.start(20), "Share", shareMenu);

    final ElwhaMenuItem rename = ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename");
    rename.addActionListener(e -> status.setText("Activated: Rename"));
    final ElwhaMenuItem duplicate = ElwhaMenuItem.of(MaterialIcons.layers(20), "Duplicate");
    duplicate.addActionListener(e -> status.setText("Activated: Duplicate"));
    final ElwhaMenuItem deleteItem = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
    deleteItem.addActionListener(e -> status.setText("Activated: Delete"));

    return ElwhaMenu.builder()
        .addItem(rename)
        .addItem(duplicate)
        .addItem(share)
        .addItem(deleteItem)
        .onClose(cause -> status.setText("Closed: " + cause))
        .build();
  }
}
