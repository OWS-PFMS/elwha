package com.owspfm.elwha.appbar;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S2 visual smoke for {@link ElwhaAppBar} flexible variants' static expanded chrome (#456): the
 * 112/136/120/152 expanded heights, the expanded headline block (HEADLINE_MEDIUM / DISPLAY_SMALL
 * titles, 16&nbsp;px margins, bottom anchoring), subtitle stacking, the centered option in the
 * expanded block, live subtitle add/remove height flips, and a light/dark toggle.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarFlexibleDemo {

  private ElwhaAppBarFlexibleDemo() {}

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
    SwingUtilities.invokeLater(ElwhaAppBarFlexibleDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — S2 flexible variants static chrome (#456)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaAppBar medium = ElwhaAppBar.mediumFlexible();
    medium.setTitle("Headline");
    medium.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    medium.addAction(MaterialIcons.favorite(), "Favorite", null);
    medium.addAction(MaterialIcons.moreVert(), "More options", null);

    final ElwhaAppBar mediumSub = ElwhaAppBar.mediumFlexible();
    mediumSub.setTitle("Headline");
    mediumSub.setSubtitle("Subtitle");
    mediumSub.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    mediumSub.addAction(MaterialIcons.moreVert(), "More options", null);

    final ElwhaAppBar large = ElwhaAppBar.largeFlexible();
    large.setTitle("Headline");
    large.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    large.addAction(MaterialIcons.favorite(), "Favorite", null);

    final ElwhaAppBar largeSub = ElwhaAppBar.largeFlexible();
    largeSub.setTitle("Headline");
    largeSub.setSubtitle("Subtitle");
    largeSub.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    largeSub.addAction(MaterialIcons.moreVert(), "More options", null);

    final ElwhaAppBar centered = ElwhaAppBar.largeFlexible();
    centered.setTitle("Centered headline");
    centered.setSubtitle("Centered subtitle");
    centered.setTitleCentered(true);
    centered.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    centered.addAction(MaterialIcons.favorite(), "Favorite", null);

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 14));
    bars.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    bars.add(titled("Medium flexible — 112", medium));
    bars.add(titled("Medium flexible + subtitle — 136", mediumSub));
    bars.add(titled("Large flexible — 120", large));
    bars.add(titled("Large flexible + subtitle — 152", largeSub));
    bars.add(titled("Large flexible — centered", centered));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode next =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(next).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final ElwhaButton subtitles = ElwhaButton.filledTonalButton("Toggle subtitles (height flip)");
    subtitles.addActionListener(
        e -> {
          for (ElwhaAppBar bar : List.of(medium, large)) {
            bar.setSubtitle(bar.getSubtitle() == null ? "Live subtitle" : null);
          }
          frame.revalidate();
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(mode);
    controls.add(subtitles);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(820, 860);
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
