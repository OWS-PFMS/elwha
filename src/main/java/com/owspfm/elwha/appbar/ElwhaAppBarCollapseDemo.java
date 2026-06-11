package com.owspfm.elwha.appbar;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * S4 visual smoke for {@link ElwhaAppBar} flexible collapse motion (#460): a flexible bar heading a
 * scrollable page collapses to the 64&nbsp;px strip as content scrolls (expanded headline fades out
 * and slides up; the collapsed title fades in over the last 30%), re-expanding symmetrically when
 * scrolled back — scrubbing the scrollbar scrubs the bar. Variant swap, subtitle toggle, a live
 * fraction readout, and light/dark.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarCollapseDemo {

  private ElwhaAppBarCollapseDemo() {}

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
    SwingUtilities.invokeLater(ElwhaAppBarCollapseDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — S4 flexible collapse (#460)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final JPanel page = new JPanel();
    page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
    page.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    for (int i = 1; i <= 80; i++) {
      page.add(new JLabel("Page content row " + i));
      page.add(Box.createVerticalStrut(14));
    }
    final JScrollPane scroller = new JScrollPane(page);
    scroller.setBorder(BorderFactory.createEmptyBorder());
    scroller.getVerticalScrollBar().setUnitIncrement(16);

    final ElwhaAppBar[] barRef = new ElwhaAppBar[1];
    final JPanel north = new JPanel(new BorderLayout());
    north.setOpaque(false);

    final Runnable[] mount = new Runnable[1];
    final AppBarVariant[] variantRef = {AppBarVariant.MEDIUM_FLEXIBLE};
    final String[] subtitleRef = {null};
    mount[0] =
        () -> {
          final ElwhaAppBar bar = new ElwhaAppBar(variantRef[0]);
          bar.setTitle("Headline");
          bar.setSubtitle(subtitleRef[0]);
          bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
          bar.addAction(MaterialIcons.favorite(), "Favorite", null);
          bar.addAction(MaterialIcons.moreVert(), "More options", null);
          bar.setScrollSource(scroller);
          north.removeAll();
          north.add(bar, BorderLayout.CENTER);
          north.revalidate();
          north.repaint();
          barRef[0] = bar;
        };
    mount[0].run();

    final JLabel readout = new JLabel("fraction 0.00");
    final Timer poll =
        new Timer(
            100,
            e ->
                readout.setText(
                    String.format(
                        "fraction %.2f  ·  height %d  ·  lifted %s",
                        barRef[0].getCollapsedFraction(),
                        barRef[0].getHeight(),
                        barRef[0].isLifted())));
    poll.start();

    final ElwhaButton variant = ElwhaButton.filledTonalButton("Swap medium / large");
    variant.addActionListener(
        e -> {
          variantRef[0] =
              variantRef[0] == AppBarVariant.MEDIUM_FLEXIBLE
                  ? AppBarVariant.LARGE_FLEXIBLE
                  : AppBarVariant.MEDIUM_FLEXIBLE;
          mount[0].run();
        });

    final ElwhaButton subtitle = ElwhaButton.filledTonalButton("Toggle subtitle");
    subtitle.addActionListener(
        e -> {
          subtitleRef[0] = subtitleRef[0] == null ? "Subtitle" : null;
          barRef[0].setSubtitle(subtitleRef[0]);
        });

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode next =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(next).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(variant);
    controls.add(subtitle);
    controls.add(mode);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(north, BorderLayout.NORTH);
    frame.add(scroller, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(760, 620);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
