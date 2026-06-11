package com.owspfm.elwha.tabs;

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
 * S5 visual smoke for {@link TabMode#SCROLLABLE} (#430): a 12-tab primary strip overflowing the
 * window — wheel over the bar to scroll (an in-flight scroll-to tween cancels), click off-screen
 * tabs via "activate first/last" to watch the 300&nbsp;ms scroll-to-tab with the 48&nbsp;px margin
 * compose with the indicator slide, a tab capped at 264&nbsp;px with an ellipsized label and a
 * 72&nbsp;px-floored tiny tab, add/remove buttons proving the active tab stays visible across child
 * mutations, and a secondary scrollable strip.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsScrollableDemo {

  private ElwhaTabsScrollableDemo() {}

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
    SwingUtilities.invokeLater(ElwhaTabsScrollableDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTabs — S5 scrollable (#430)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaTabs primary = ElwhaTabs.primary();
    primary.setTabMode(TabMode.SCROLLABLE);
    primary.addTab("Hi");
    primary.addTab(
        "An exceptionally long tab label that hits the 264px cap and ellipsizes politely");
    for (int i = 1; i <= 8; i++) {
      primary.addTab("Section " + i);
    }
    primary.addTab(ElwhaTab.of(MaterialIcons.symbol("favorite"), "Pinned"));
    primary.addTab("Last");

    final ElwhaTabs secondary = ElwhaTabs.secondary();
    secondary.setTabMode(TabMode.SCROLLABLE);
    for (int i = 1; i <= 14; i++) {
      secondary.addTab("Topic " + i);
    }

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 18));
    bars.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    bars.add(titled("Primary scrollable — wheel to scroll; 264px cap + 72px floor", primary));
    bars.add(titled("Secondary scrollable", secondary));

    final ElwhaButton first = ElwhaButton.filledTonalButton("Activate first");
    first.addActionListener(e -> primary.setActiveTabIndex(0));
    final ElwhaButton last = ElwhaButton.filledTonalButton("Activate last");
    last.addActionListener(e -> primary.setActiveTabIndex(primary.getTabCount() - 1));

    final ElwhaButton add = ElwhaButton.outlinedButton("Add tab at end");
    add.addActionListener(e -> primary.addTab("Added " + (primary.getTabCount() + 1)));
    final ElwhaButton removeFirst = ElwhaButton.outlinedButton("Remove first tab");
    removeFirst.addActionListener(
        e -> {
          if (primary.getTabCount() > 1) {
            primary.removeTab(primary.getTabAt(0));
          }
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(first);
    controls.add(last);
    controls.add(add);
    controls.add(removeFirst);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(640, 320);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel titled(final String title, final ElwhaTabs bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 6));
    panel.setOpaque(false);
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}
