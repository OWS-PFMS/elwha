package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S5 visual smoke for {@link ElwhaTooltip} motion, RTL, and a11y (#451): hover the anchors to see
 * the 150&nbsp;ms standard-curve fade-in (the rich card's action button fades with the card — no
 * full-alpha pop) and the snappier 75&nbsp;ms fade-out; the reduced-motion checkbox snaps both
 * ways. The RTL checkbox flips the whole frame's orientation — START/END alignments mirror, the
 * rich END default hangs off the other corner, and surface text right-aligns. Anchor accessible
 * descriptions are wired on attach (verify with an accessibility inspector).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipMotionRtlDemo {

  private TooltipMotionRtlDemo() {}

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
    SwingUtilities.invokeLater(TooltipMotionRtlDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTooltip — S5 motion, RTL & a11y (#451)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaIconButton plainAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.info());
    final ElwhaTooltip plainTip =
        ElwhaTooltip.plain("Fades in over 150 ms, out over 75 ms").attach(plainAnchor);

    final ElwhaIconButton startAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.edit());
    final ElwhaTooltip startTip = ElwhaTooltip.plain("START aligned").attach(startAnchor);
    startTip.setAlignment(TooltipAlignment.START);

    final ElwhaIconButton endAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.delete());
    final ElwhaTooltip endTip = ElwhaTooltip.plain("END aligned").attach(endAnchor);
    endTip.setAlignment(TooltipAlignment.END);

    final ElwhaIconButton richAnchor = ElwhaIconButton.standardIconButton(MaterialIcons.help());
    ElwhaTooltip.rich()
        .subhead("Rich fade")
        .supportingText(
            "The action button below fades in with the card — watch for any full-alpha pop.")
        .action("Learn more", e -> System.out.println("learn more"))
        .build()
        .attach(richAnchor);

    final JPanel anchors = new JPanel(new FlowLayout(FlowLayout.CENTER, 56, 0));
    anchors.setBorder(BorderFactory.createEmptyBorder(200, 24, 24, 24));
    anchors.add(plainAnchor);
    anchors.add(startAnchor);
    anchors.add(endAnchor);
    anchors.add(richAnchor);

    final ElwhaCheckbox reducedMotion = new ElwhaCheckbox("Reduced motion");
    reducedMotion.addActionListener(e -> MorphAnimator.setReducedMotion(reducedMotion.isChecked()));

    final ElwhaCheckbox rtl = new ElwhaCheckbox("Right-to-left");
    rtl.addActionListener(
        e -> {
          frame
              .getContentPane()
              .applyComponentOrientation(
                  rtl.isChecked()
                      ? ComponentOrientation.RIGHT_TO_LEFT
                      : ComponentOrientation.LEFT_TO_RIGHT);
          frame.revalidate();
          frame.repaint();
        });

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
    controls.add(reducedMotion);
    controls.add(rtl);
    controls.add(mode);

    frame.setLayout(new BorderLayout());
    frame.add(anchors, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(880, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
