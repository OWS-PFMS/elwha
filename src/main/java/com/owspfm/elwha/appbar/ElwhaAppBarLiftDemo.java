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

/**
 * S3 visual smoke for {@link ElwhaAppBar} lift-on-scroll (#457): a small bar heading a scrollable
 * page — scroll down and the container fades SURFACE → SURFACE_CONTAINER (~200&nbsp;ms), scroll
 * back to the top and it fades home. Toggles for lift-on-scroll and light/dark; also exercises the
 * {@code ScrollSourceBinding} extraction end-to-end (the FAB scroll playground re-smokes the
 * refactored {@code ElwhaFabAnchor} side).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarLiftDemo {

  private ElwhaAppBarLiftDemo() {}

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
    SwingUtilities.invokeLater(ElwhaAppBarLiftDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — S3 lift on scroll (#457)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSubtitle("Scroll the page — the bar lifts");
    bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    bar.addAction(MaterialIcons.favorite(), "Favorite", null);
    bar.addAction(MaterialIcons.moreVert(), "More options", null);

    final JPanel page = new JPanel();
    page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
    page.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    for (int i = 1; i <= 60; i++) {
      page.add(new JLabel("Page content row " + i));
      page.add(Box.createVerticalStrut(14));
    }
    final JScrollPane scroller = new JScrollPane(page);
    scroller.setBorder(BorderFactory.createEmptyBorder());
    scroller.getVerticalScrollBar().setUnitIncrement(16);

    bar.setScrollSource(scroller);

    final ElwhaButton lift = ElwhaButton.filledTonalButton("Toggle lift-on-scroll");
    lift.addActionListener(e -> bar.setLiftOnScroll(!bar.isLiftOnScroll()));

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
    controls.add(lift);
    controls.add(mode);

    frame.setLayout(new BorderLayout());
    frame.add(bar, BorderLayout.NORTH);
    frame.add(scroller, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(720, 540);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
