package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ButtonInteractionMode;
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
 * Visual demo for epic #298 S4 — keyboard navigation, roving focus, and light dismiss. Open the menu
 * from the trigger, then drive it with the keyboard:
 *
 * <ul>
 *   <li><strong>Up / Down</strong> move the focus ring (wrapping); <strong>Home / End</strong> jump
 *       to first / last.
 *   <li><strong>Letters</strong> type-ahead to the next matching item.
 *   <li><strong>Enter / Space</strong> activate the focused item (and close).
 *   <li><strong>Esc</strong>, an outside click, or Tab-away dismisses; focus returns to the trigger.
 * </ul>
 *
 * The {@code SELECTABLE} trigger shows its pressed-while-open state and is restored after. A status
 * line reports each activation and the dismiss cause.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenuKeyboardDemo {

  private ElwhaMenuKeyboardDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(ElwhaMenuKeyboardDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu S4 — keyboard + focus");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(560, 360);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new BorderLayout(0, 16));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    final JLabel status = new JLabel("Open the menu, then use ↑ ↓ Home End, letters, Enter, Esc.");
    status.setForeground(ColorRole.ON_SURFACE.resolve());
    content.add(status, BorderLayout.NORTH);

    final ElwhaButton trigger =
        ElwhaButton.outlinedButton("Actions")
            .setInteractionMode(ButtonInteractionMode.SELECTABLE);
    trigger.addActionListener(e -> buildMenu(status).open(trigger));

    final JPanel center = new JPanel();
    center.setOpaque(false);
    center.add(trigger);
    content.add(center, BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  private static ElwhaMenu buildMenu(final JLabel status) {
    final ElwhaMenuItem rename = ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename");
    rename.addActionListener(e -> status.setText("Activated: Rename"));
    final ElwhaMenuItem duplicate = ElwhaMenuItem.of(MaterialIcons.add(20), "Duplicate");
    duplicate.addActionListener(e -> status.setText("Activated: Duplicate"));
    final ElwhaMenuItem archive = ElwhaMenuItem.of(MaterialIcons.layers(20), "Archive");
    archive.addActionListener(e -> status.setText("Activated: Archive"));
    final ElwhaMenuItem deleteDisabled = ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete");
    deleteDisabled.setEnabled(false);
    final ElwhaMenuItem settings = ElwhaMenuItem.of(MaterialIcons.palette(20), "Settings");
    settings.setTrailingText("⌘,");
    settings.addActionListener(e -> status.setText("Activated: Settings"));

    return ElwhaMenu.builder()
        .addItem(rename)
        .addItem(duplicate)
        .addItem(archive)
        .addGroup()
        .addItem(deleteDisabled)
        .addItem(settings)
        .separator(Separator.DIVIDER)
        .onClose(cause -> status.setText("Closed: " + cause))
        .build();
  }
}
