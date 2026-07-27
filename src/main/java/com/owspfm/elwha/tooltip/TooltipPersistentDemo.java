package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S4 visual smoke for the persistent rich {@link ElwhaTooltip} (#450): the info icon toggles its
 * tooltip on click (hover does nothing — try it); once up, it survives arbitrary hovering and
 * clicks <em>inside</em> the card, and dismisses on a press anywhere outside, Esc, wheel, the
 * action click, or a second anchor click. Tab to the icon and press Space/Enter for the keyboard
 * toggle. The default-rich anchor beside it contrasts the hover flavor — and showing either evicts
 * the other (one at a time).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipPersistentDemo {

  private TooltipPersistentDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(TooltipPersistentDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTooltip — S4 persistent rich (#450)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaIconButton persistentAnchor =
        ElwhaIconButton.standardIconButton(MaterialIcons.info());
    ElwhaTooltip.rich()
        .subhead("Persistent rich tooltip")
        .supportingText(
            "Click the info icon to toggle me. I ignore hover entirely, survive clicks inside"
                + " this card, and dismiss on an outside press, Esc, wheel, the action, or a"
                + " second click on the icon.")
        .action("Got it", e -> System.out.println("got it"))
        .persistent(true)
        .build()
        .attach(persistentAnchor);

    final ElwhaIconButton hoverAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.help());
    ElwhaTooltip.rich()
        .subhead("Default rich tooltip")
        .supportingText(
            "I'm the hover flavor for contrast — and showing me evicts the persistent one.")
        .build()
        .attach(hoverAnchor);

    final JPanel anchors = new JPanel(new FlowLayout(FlowLayout.CENTER, 64, 0));
    anchors.setBorder(BorderFactory.createEmptyBorder(200, 24, 24, 24));
    anchors.add(labeled("Persistent (click me)", persistentAnchor));
    anchors.add(labeled("Default (hover me)", hoverAnchor));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode nextMode =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(nextMode).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(mode);

    frame.setLayout(new BorderLayout());
    frame.add(anchors, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(820, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel labeled(final String caption, final ElwhaIconButton anchor) {
    final JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setOpaque(false);
    panel.add(anchor, BorderLayout.CENTER);
    final JLabel label = new JLabel(caption, JLabel.CENTER);
    panel.add(label, BorderLayout.SOUTH);
    return panel;
  }
}
