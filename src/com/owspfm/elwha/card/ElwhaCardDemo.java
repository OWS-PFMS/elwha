package com.owspfm.elwha.card;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Minimal smoke-test demo for {@link ElwhaCard}: one card per variant, one per interaction mode,
 * plus a collapsible example. Useful as a quick visual check during development.
 *
 * <p>For the polished, interactive playground (with live-editing controls and a theme switcher),
 * see {@link com.owspfm.elwha.card.playground.ElwhaCardPlayground}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class ElwhaCardDemo {

  private ElwhaCardDemo() {
    // entry point only
  }

  /**
   * Launches the demo frame.
   *
   * @param args command-line arguments (unused)
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          FlatLightLaf.setup();
          JFrame frame = new JFrame("ElwhaCard demo");
          frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
          frame.setContentPane(buildContent());
          frame.setSize(900, 720);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static JPanel buildContent() {
    JPanel grid = new JPanel(new GridLayout(0, 3, 16, 16));
    grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    grid.add(simpleCard(CardVariant.ELEVATED, "Elevated"));
    grid.add(simpleCard(CardVariant.OUTLINED, "Outlined"));
    grid.add(simpleCard(CardVariant.FILLED, "Filled"));

    grid.add(modeCard(CardInteractionMode.STATIC, "Static"));
    grid.add(modeCard(CardInteractionMode.HOVERABLE, "Hoverable"));
    grid.add(modeCard(CardInteractionMode.CLICKABLE, "Clickable"));

    grid.add(modeCard(CardInteractionMode.SELECTABLE, "Selectable"));
    grid.add(collapsibleCard(false));
    grid.add(collapsibleCard(true));

    JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(new JScrollPane(grid), BorderLayout.CENTER);
    wrap.setPreferredSize(new Dimension(900, 720));
    return wrap;
  }

  private static ElwhaCard simpleCard(final CardVariant v, final String label) {
    return new ElwhaCard()
        .setVariant(v)
        .setHeader(label, v.name())
        .setBody(new JLabel("Body content for the " + label.toLowerCase() + " card."))
        .setFooter(new JButton("OK"));
  }

  private static ElwhaCard modeCard(final CardInteractionMode m, final String label) {
    return new ElwhaCard()
        .setHeader(label, m.name())
        .setBody(new JLabel("Hover, click, or focus."))
        .setInteractionMode(m);
  }

  private static ElwhaCard collapsibleCard(final boolean startCollapsed) {
    return new ElwhaCard()
        .setHeader("Collapsible", startCollapsed ? "Collapsed" : "Expanded")
        .setBody(new JLabel("Click the chevron in the header to toggle."))
        .setCollapsible(true)
        .setCollapsed(startCollapsed)
        .setCollapsedSummary(new JLabel("Hidden — toggle to expand"));
  }
}
