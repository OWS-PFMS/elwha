package com.owspfm.elwha.tabs;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.switches.ElwhaSwitch;
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
 * S4 visual smoke for {@link ElwhaTab} icons and badges (#429): primary stacked (64&nbsp;px bar),
 * the inline-icon toggle, secondary inline icons (48&nbsp;px stays), icon-only tabs, the fill-swap
 * on activation, a count badge riding an icon corner, a small-dot badge trailing a label-only tab,
 * and a mixed bar showing inline-height tabs bottom-aligning in a 64&nbsp;px bar.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsIconsBadgesDemo {

  private ElwhaTabsIconsBadgesDemo() {}

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
    SwingUtilities.invokeLater(ElwhaTabsIconsBadgesDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTabs — S4 icons + badges (#429)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaTabs stacked = ElwhaTabs.primary();
    stacked.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Home"));
    stacked.addTab(ElwhaTab.of(MaterialIcons.symbol("favorite"), "Favorites"));
    stacked.addTab(ElwhaTab.of(MaterialIcons.symbol("info"), "About"));

    final ElwhaTabs inline = ElwhaTabs.primary();
    for (String[] def :
        new String[][] {{"home", "Home"}, {"favorite", "Favorites"}, {"info", "About"}}) {
      final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol(def[0]), def[1]);
      tab.setInlineIcon(true);
      inline.addTab(tab);
    }

    final ElwhaTabs secondary = ElwhaTabs.secondary();
    secondary.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Travel"));
    secondary.addTab(ElwhaTab.of(MaterialIcons.symbol("favorite"), "Hotel"));
    secondary.addTab(ElwhaTab.of(MaterialIcons.symbol("info"), "Activities"));

    final ElwhaTabs iconsOnly = ElwhaTabs.primary();
    iconsOnly.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("home"), "Home"));
    iconsOnly.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("favorite"), "Favorites"));
    iconsOnly.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("info"), "About"));

    final ElwhaTabs badged = ElwhaTabs.primary();
    final ElwhaTab inbox = ElwhaTab.of(MaterialIcons.symbol("home"), "Inbox");
    inbox.setBadge(ElwhaBadge.large(88));
    badged.addTab(inbox);
    final ElwhaTab updates = ElwhaTab.of("Updates");
    updates.setBadge(ElwhaBadge.small());
    badged.addTab(updates);
    badged.addTab("Archive");

    final ElwhaTabs mixed = ElwhaTabs.primary();
    mixed.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Stacked 64"));
    mixed.addTab("Label-only bottom-aligns");
    mixed.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("favorite"), "Icon-only"));

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 14));
    bars.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    bars.add(titled("Primary stacked — 64px bar, gap 2, fill-swap on activation", stacked));
    bars.add(titled("Primary inline-icon — 48px bar, gap 8", inline));
    bars.add(titled("Secondary — icons always inline, 48px", secondary));
    bars.add(titled("Icon-only — accessible labels set", iconsOnly));
    bars.add(titled("Badged — count on icon corner, dot trailing the label", badged));
    bars.add(titled("Mixed heights — inline tabs bottom-align in the 64px bar", mixed));

    final ElwhaSwitch inlineToggle = new ElwhaSwitch();
    inlineToggle.setSelected(true);
    inlineToggle.setLabel("Second bar inline icons");
    inlineToggle.addActionListener(
        e -> {
          for (int i = 0; i < inline.getTabCount(); i++) {
            inline.getTabAt(i).setInlineIcon(inlineToggle.isSelected());
          }
          inline.revalidate();
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(new JLabel("Inline icons (bar 2):"));
    controls.add(inlineToggle);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(780, 720);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel titled(final String title, final ElwhaTabs bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setOpaque(false);
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}
