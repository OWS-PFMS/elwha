package com.owspfm.elwha.tabs;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * S6 visual smoke for {@link ElwhaTabs} keyboard navigation, a11y, and RTL (#431). Tab from the
 * leading text field: focus enters the bar on the <em>active</em> tab (the roving stop) with the
 * keyboard-only focus ring (mouse clicks never ring); ←/→ wrap around, Home/End jump, Space/Enter
 * activate, and the focused tab auto-scrolls into view on the scrollable bar. Toggle auto-activate
 * to make focus moves select. Toggle RTL: tab order, arrow direction, inline icon side, indicator,
 * and scrolling all mirror. Tab again leaves the bar to the trailing field.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsKeyboardA11yDemo {

  private ElwhaTabsKeyboardA11yDemo() {}

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
    SwingUtilities.invokeLater(ElwhaTabsKeyboardA11yDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTabs — S6 keyboard + a11y + RTL (#431)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaTabs fixed = ElwhaTabs.primary();
    fixed.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Home"));
    final ElwhaTab inlined = ElwhaTab.of(MaterialIcons.symbol("favorite"), "Favorites");
    inlined.setInlineIcon(true);
    fixed.addTab(inlined);
    fixed.addTab("Notes");
    fixed.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("info"), "About"));
    fixed.setActiveTabIndex(2);

    final ElwhaTabs scrollable = ElwhaTabs.secondary();
    scrollable.setTabMode(TabMode.SCROLLABLE);
    for (int i = 1; i <= 12; i++) {
      scrollable.addTab("Topic " + i);
    }

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 14));
    bars.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    bars.add(new JTextField("Tab from here — focus enters on the ACTIVE tab"));
    bars.add(titled("Primary fixed — arrows wrap; Space/Enter activate", fixed));
    bars.add(titled("Secondary scrollable — focused tab scrolls into view", scrollable));
    bars.add(new JTextField("…and Tab leaves the bar to here"));

    final ElwhaSwitch auto = new ElwhaSwitch();
    auto.setLabel("Auto-activate on focus");
    auto.addActionListener(
        e -> {
          fixed.setAutoActivate(auto.isSelected());
          scrollable.setAutoActivate(auto.isSelected());
        });

    final ElwhaSwitch rtl = new ElwhaSwitch();
    rtl.setLabel("Right-to-left");
    rtl.addActionListener(
        e -> {
          final ComponentOrientation o =
              rtl.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          frame.applyComponentOrientation(o);
          frame.revalidate();
          frame.repaint();
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(new JLabel("Auto-activate:"));
    controls.add(auto);
    controls.add(new JLabel("RTL:"));
    controls.add(rtl);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(720, 460);
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
