package com.owspfm.elwha.appbar;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S1 visual smoke for {@link ElwhaAppBar} small-bar static chrome (#455): slot layout (nav button /
 * title / actions at the 4-48-0-4 token geometry), bar-painted title + subtitle, the centered
 * option, the no-nav 16&nbsp;px title inset, long-title ellipsis, and a light/dark toggle to
 * eyeball token-correctness in both modes.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarChromeDemo {

  private ElwhaAppBarChromeDemo() {}

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
    SwingUtilities.invokeLater(ElwhaAppBarChromeDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — S1 small bar static chrome (#455)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaAppBar full = ElwhaAppBar.small();
    full.setTitle("Inbox");
    full.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    full.addAction(MaterialIcons.favorite(), "Favorite", null);
    full.addAction(MaterialIcons.moreVert(), "More options", null);

    final ElwhaAppBar subtitled = ElwhaAppBar.small();
    subtitled.setTitle("Saved searches");
    subtitled.setSubtitle("Synced 5 minutes ago");
    subtitled.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    subtitled.addAction(MaterialIcons.edit(), "Edit", null);

    final ElwhaAppBar noNav = ElwhaAppBar.small();
    noNav.setTitle("No navigation button — 16px title inset");
    noNav.addAction(MaterialIcons.moreVert(), "More options", null);

    final ElwhaAppBar centered = ElwhaAppBar.small();
    centered.setTitle("Centered");
    centered.setSubtitle("with subtitle");
    centered.setTitleCentered(true);
    centered.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    centered.addAction(MaterialIcons.favorite(), "Favorite", null);

    final ElwhaAppBar overflow = ElwhaAppBar.small();
    overflow.setTitle("A very long headline that cannot possibly fit and must ellipsize cleanly");
    overflow.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    overflow.addAction(MaterialIcons.favorite(), "Favorite", null);
    overflow.addAction(MaterialIcons.edit(), "Edit", null);
    overflow.addAction(MaterialIcons.moreVert(), "More options", null);

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 18));
    bars.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    bars.add(titled("Small — nav + title + 2 actions", full));
    bars.add(titled("Small — subtitle", subtitled));
    bars.add(titled("Small — no nav button", noNav));
    bars.add(titled("Small — centered title + subtitle", centered));
    bars.add(titled("Small — long-title ellipsis, 3 actions", overflow));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode next =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(next).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final ElwhaButton center = ElwhaButton.filledTonalButton("Toggle centered (top bar)");
    center.addActionListener(e -> full.setTitleCentered(!full.isTitleCentered()));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(mode);
    controls.add(center);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(760, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel titled(final String label, final ElwhaAppBar bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setOpaque(false);
    panel.add(new JLabel(label), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}
