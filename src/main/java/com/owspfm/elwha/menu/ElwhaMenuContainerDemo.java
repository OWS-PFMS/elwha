package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual demo for epic #298 S3 — the {@link ElwhaMenu} container across its Phase-1 configurations:
 * Standard (flat), Grouped + Gap (rounded cards), Grouped + Divider, a tall scrolling menu (gaps
 * forced to dividers), and an edge-anchored trigger that flips the menu above. Dogfoods {@link
 * ElwhaButton} triggers per the operator's house rule.
 *
 * <p>Pass {@code dark} as the first arg for dark mode.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenuContainerDemo {

  private ElwhaMenuContainerDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(ElwhaMenuContainerDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu S3 — container");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 480);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new GridBagLayout());
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.gridx = 0;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.anchor = GridBagConstraints.LINE_START;

    content.add(label("Click a trigger to open the live menu:"), gc);

    content.add(trigger("Standard (flat)", ElwhaMenuContainerDemo::standardMenu), gc);
    content.add(trigger("Grouped + Gap", ElwhaMenuContainerDemo::gapMenu), gc);
    content.add(trigger("Grouped + Divider", ElwhaMenuContainerDemo::dividerMenu), gc);
    content.add(trigger("Tall (scrolls → divider)", ElwhaMenuContainerDemo::tallMenu), gc);

    final ElwhaButton flip = ElwhaButton.filledTonalButton("Near bottom — flips up ↑");
    flip.addActionListener(e -> standardMenu().open(flip));
    gc.weighty = 1;
    gc.anchor = GridBagConstraints.PAGE_END;
    content.add(flip, gc);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private interface MenuFactory {
    ElwhaMenu create();
  }

  private static ElwhaButton trigger(final String text, final MenuFactory factory) {
    final ElwhaButton button = ElwhaButton.outlinedButton(text);
    button.addActionListener(e -> factory.create().open(button));
    return button;
  }

  private static ElwhaMenu standardMenu() {
    return ElwhaMenu.builder()
        .addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Edit"))
        .addItem(shortcut(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"), "⌘⌫"))
        .addItem(ElwhaMenuItem.of(MaterialIcons.star(20), "Favorite"))
        .build();
  }

  private static ElwhaMenu gapMenu() {
    return ElwhaMenu.builder()
        .separator(Separator.GAP)
        .addItem(ElwhaMenuItem.of(MaterialIcons.home(20), "Home"))
        .addItem(ElwhaMenuItem.of(MaterialIcons.star(20), "Starred"))
        .addGroup()
        .addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename"))
        .addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"))
        .build();
  }

  private static ElwhaMenu dividerMenu() {
    return ElwhaMenu.builder()
        .separator(Separator.DIVIDER)
        .addItem(ElwhaMenuItem.of(MaterialIcons.home(20), "Home"))
        .addItem(ElwhaMenuItem.of(MaterialIcons.star(20), "Starred"))
        .addGroup()
        .addItem(ElwhaMenuItem.of(MaterialIcons.info(20), "Properties"))
        .addItem(ElwhaMenuItem.of(MaterialIcons.help(20), "Help"))
        .build();
  }

  private static ElwhaMenu tallMenu() {
    final ElwhaMenu.Builder b = ElwhaMenu.builder().separator(Separator.GAP);
    for (int i = 1; i <= 18; i++) {
      b.addItem(ElwhaMenuItem.of("Option " + i));
      if (i % 6 == 0) {
        b.addGroup();
      }
    }
    return b.build();
  }

  private static ElwhaMenuItem shortcut(final ElwhaMenuItem item, final String keys) {
    item.setTrailingText(keys);
    return item;
  }

  private static JLabel label(final String text) {
    final JLabel l = new JLabel(text);
    l.setForeground(ColorRole.ON_SURFACE.resolve());
    return l;
  }
}
