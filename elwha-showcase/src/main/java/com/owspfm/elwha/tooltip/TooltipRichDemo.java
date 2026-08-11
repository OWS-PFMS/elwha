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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S3 visual smoke for the rich {@link ElwhaTooltip} (#449): hover the icon anchors to see the
 * surface-container card under its elevation-2 shadow — subhead + supporting baseline rhythm
 * (28/24/16), the 320&nbsp;px wrap, the no-subhead form, and one- and two-action rows of real
 * {@code PRIMARY} text buttons. Hover from the anchor onto the card to see it stay (default-rich
 * hoverable contents); click inside the card to dismiss it; click an action to see
 * dismiss-then-fire. The {@code END} default alignment hangs the card off the anchor's trailing
 * corner. Light/dark toggle for token checks.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipRichDemo {

  private TooltipRichDemo() {}

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
    SwingUtilities.invokeLater(TooltipRichDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTooltip — S3 rich tooltip (#449)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaIconButton fullAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.info());
    ElwhaTooltip.rich()
        .subhead("Rich tooltip")
        .supportingText(
            "Rich tooltips bring attention to a particular element or feature that warrants the"
                + " user's focus. They support multiple lines and wrap at 320 px.")
        .build()
        .attach(fullAnchor);

    final ElwhaIconButton bareAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.help());
    ElwhaTooltip.rich()
        .supportingText("No subhead here — the supporting text starts on its own baseline.")
        .build()
        .attach(bareAnchor);

    final ElwhaIconButton oneAction = ElwhaIconButton.standardIconButton(MaterialIcons.edit());
    ElwhaTooltip.rich()
        .subhead("Grant value is calculated using the closing stock price")
        .supportingText(
            "The closing price is from the day before the grant date. The value is then divided"
                + " into quarterly increments.")
        .action(
            "Learn more",
            e ->
                JOptionPane.showMessageDialog(
                    frame, "Action fired — and the tooltip dismissed first."))
        .build()
        .attach(oneAction);

    final ElwhaIconButton twoActions = ElwhaIconButton.standardIconButton(MaterialIcons.delete());
    ElwhaTooltip.rich()
        .subhead("Delete forever?")
        .supportingText("Two start-aligned text buttons, 8 px apart, on the 36 px action row.")
        .action("Learn more", e -> System.out.println("learn more"))
        .action("Dismiss", e -> System.out.println("dismissed"))
        .build()
        .attach(twoActions);

    final JPanel anchors = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 120));
    anchors.setBorder(BorderFactory.createEmptyBorder(160, 24, 24, 24));
    anchors.add(fullAnchor);
    anchors.add(bareAnchor);
    anchors.add(oneAction);
    anchors.add(twoActions);

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
    frame.setSize(880, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
