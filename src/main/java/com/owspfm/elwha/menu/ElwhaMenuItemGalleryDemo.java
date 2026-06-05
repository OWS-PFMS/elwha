package com.owspfm.elwha.menu;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual gallery demo for epic #298 S2 — a static matrix of {@link ElwhaMenuItem} primitives
 * exercising every slot and state (leading icon, supporting text, trailing text/icon, badge,
 * swappable slot, selected fill + checkmark, focused ring, disabled dim) on a {@code
 * SURFACE_CONTAINER_LOW} panel. The items are not yet assembled into an {@code ElwhaMenu} (S3) — this
 * isolates the primitive. Throwaway per-story smoke artifact.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenuItemGalleryDemo {

  private ElwhaMenuItemGalleryDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(ElwhaMenuItemGalleryDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenuItem S2 — slot × state gallery");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final JPanel root = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 24));
    root.setBackground(ColorRole.SURFACE.resolve());

    final JPanel surface = new JPanel();
    surface.setLayout(new BoxLayout(surface, BoxLayout.Y_AXIS));
    surface.setBackground(ColorRole.SURFACE_CONTAINER_LOW.resolve());
    surface.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    surface.add(item(ElwhaMenuItem.of("Plain action")));
    surface.add(item(ElwhaMenuItem.of(MaterialIcons.edit(20), "Edit")));

    final ElwhaMenuItem copy = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
    copy.setTrailingText("⌘⌫");
    surface.add(item(copy));

    final ElwhaMenuItem two = ElwhaMenuItem.of(MaterialIcons.favorite(20), "Apparel");
    two.setSupportingText("Latest trends & styles");
    two.setBadge(ElwhaBadge.large("New"));
    surface.add(item(two));

    final ElwhaMenuItem submenu = ElwhaMenuItem.of(MaterialIcons.star(20), "More options");
    submenu.setTrailingIcon(MaterialIcons.expandMore(20));
    surface.add(item(submenu));

    final ElwhaMenuItem selected = ElwhaMenuItem.of(MaterialIcons.home(20), "Selected item");
    selected.setSelected(true);
    surface.add(item(selected));

    final ElwhaMenuItem focused = ElwhaMenuItem.of(MaterialIcons.info(20), "Focused item");
    focused.setFocused(true);
    surface.add(item(focused));

    final ElwhaMenuItem disabled = ElwhaMenuItem.of(MaterialIcons.help(20), "Disabled item");
    disabled.setEnabled(false);
    surface.add(item(disabled));

    final ElwhaMenuItem slot = ElwhaMenuItem.of("Swatch slot");
    final JPanel swatch = new JPanel();
    swatch.setPreferredSize(new Dimension(120, 20));
    swatch.setBackground(ColorRole.TERTIARY.resolve());
    slot.setSlot(swatch);
    surface.add(item(slot));

    root.add(surface);
    frame.setContentPane(root);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // Stretch each item to the widest preferred width so the column aligns, capped to a menu-ish width.
  private static JComponent item(final ElwhaMenuItem mi) {
    mi.setAlignmentX(Component.LEFT_ALIGNMENT);
    final Dimension pref = mi.getPreferredSize();
    mi.setMaximumSize(new Dimension(Math.max(280, pref.width), pref.height));
    mi.setPreferredSize(new Dimension(Math.max(280, pref.width), pref.height));
    return mi;
  }
}
