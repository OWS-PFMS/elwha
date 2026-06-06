package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Visual demo for epic #298 S5 — the {@link ColorStyle#VIBRANT} color style beside {@link
 * ColorStyle#STANDARD}. Each column shows a live trigger (click to open the real overlay) and a
 * static {@code renderPreview()} snapshot so the surface tint and the selected-item treatment are
 * visible without opening. The "Favorite" item is pre-selected to surface the selection role —
 * {@code TERTIARY_CONTAINER} under Standard vs the bold {@code TERTIARY} jump under Vibrant — each
 * with its ✓ checkmark. Dogfoods {@link ElwhaButton} triggers per the operator's house rule.
 *
 * <p>Pass {@code dark} as the first arg for dark mode — both styles use the same roles, so dark is
 * free via {@link ElwhaTheme}.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenuVibrantDemo {

  private ElwhaMenuVibrantDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(ElwhaMenuVibrantDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu S5 — Vibrant color style");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 520);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new GridLayout(1, 2, 24, 0));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    content.add(column("Standard (surface)", ColorStyle.STANDARD));
    content.add(column("Vibrant (tertiary-tinted)", ColorStyle.VIBRANT));

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private static JComponent column(final String title, final ColorStyle style) {
    final JPanel col = new JPanel();
    col.setOpaque(false);
    col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

    final JLabel heading = new JLabel(title, SwingConstants.CENTER);
    heading.setForeground(ColorRole.ON_SURFACE.resolve());
    heading.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    col.add(heading);
    col.add(javax.swing.Box.createVerticalStrut(12));

    final ElwhaButton trigger = ElwhaButton.outlinedButton("Open live menu");
    trigger.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    trigger.addActionListener(e -> sampleMenu(style).open(trigger));
    col.add(trigger);
    col.add(javax.swing.Box.createVerticalStrut(20));

    final JComponent preview = sampleMenu(style).renderPreview();
    preview.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    col.add(preview);
    col.add(javax.swing.Box.createVerticalGlue());
    return col;
  }

  private static ElwhaMenu sampleMenu(final ColorStyle style) {
    final ElwhaMenu.Builder b =
        ElwhaMenu.builder().separator(Separator.GAP).layout(Layout.GROUPED).colorStyle(style);
    b.addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename"));
    final ElwhaMenuItem duplicate = ElwhaMenuItem.of(MaterialIcons.add(20), "Duplicate");
    duplicate.setTrailingText("⌘D");
    b.addItem(duplicate);
    final ElwhaMenuItem favorite = ElwhaMenuItem.of(MaterialIcons.star(20), "Favorite");
    favorite.setSelected(true);
    b.addItem(favorite);
    b.addGroup();
    b.addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"));
    final ElwhaMenuItem settings = ElwhaMenuItem.of(MaterialIcons.palette(20), "Settings");
    settings.setTrailingText("⌘,");
    b.addItem(settings);
    return b.build();
  }
}
