package com.owspfm.ui.components.card.playground;

import com.owspfm.ui.components.card.CardInteractionMode;
import com.owspfm.ui.components.card.CardVariant;
import com.owspfm.ui.components.card.FlatCard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Read-only gallery of {@link FlatCard} configurations. Each section pairs a heading with a row of
 * cards demonstrating one axis of the API (variants, interaction modes, collapsed state, etc.).
 *
 * <p>This is the "what does it look like" half of the playground; the {@link LiveConfigPanel}
 * handles "what does it look like under <em>my</em> settings".
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public final class GalleryPanel extends JPanel {

  /** Builds the gallery scroller. */
  public GalleryPanel() {
    super(new BorderLayout());
    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    content.add(
        section(
            "Variants",
            row(
                variantCard("Elevated", CardVariant.ELEVATED),
                variantCard("Outlined", CardVariant.OUTLINED),
                variantCard("Filled", CardVariant.FILLED))));

    content.add(
        section(
            "Interaction modes",
            row(
                modeCard("Static", CardInteractionMode.STATIC),
                modeCard("Hoverable", CardInteractionMode.HOVERABLE),
                modeCard("Clickable", CardInteractionMode.CLICKABLE),
                modeCard("Selectable", CardInteractionMode.SELECTABLE))));

    content.add(
        section(
            "Collapsible (expanded ↔ collapsed)",
            row(collapsibleCard(false), collapsibleCard(true))));

    content.add(section("Full slot configuration", row(fullSlotCard())));

    content.add(section("Minimal config (defaults only)", row(minimalCard())));

    content.add(section("OWS cycle card (real-world composition)", row(CycleCardExample.build())));

    content.add(
        section(
            "Disabled states",
            row(
                disabledCard(CardInteractionMode.HOVERABLE, "Hoverable"),
                disabledCard(CardInteractionMode.CLICKABLE, "Clickable"),
                disabledCard(CardInteractionMode.SELECTABLE, "Selectable"))));

    JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    add(scroll, BorderLayout.CENTER);
  }

  private static JComponent section(final String title, final JComponent body) {
    JPanel sec = new JPanel(new BorderLayout());
    sec.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
    JLabel heading = new JLabel(title);
    heading.putClientProperty("FlatLaf.styleClass", "h3");
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    sec.add(heading, BorderLayout.NORTH);
    sec.add(body, BorderLayout.CENTER);
    sec.setAlignmentX(Component.LEFT_ALIGNMENT);
    return sec;
  }

  private static JComponent row(final JComponent... cards) {
    JPanel row = new JPanel(new GridLayout(1, cards.length, 12, 12));
    for (JComponent c : cards) {
      row.add(c);
    }
    return row;
  }

  private static FlatCard variantCard(final String label, final CardVariant v) {
    return new FlatCard()
        .setVariant(v)
        .setHeader(label, "Variant: " + v.name())
        .setBody(
            new JLabel("This card shows the " + v.name().toLowerCase() + " surface treatment."))
        .setFooter(new JButton("Action"));
  }

  private static FlatCard modeCard(final String label, final CardInteractionMode m) {
    FlatCard c =
        new FlatCard()
            .setHeader(label, "Mode: " + m.name())
            .setBody(new JLabel("Hover, click, or focus to see feedback."))
            .setInteractionMode(m);
    if (m == CardInteractionMode.SELECTABLE) {
      c.addActionListener(
          e -> {
            // toggling shown via card.isSelected(); no-op listener for demonstration
          });
    }
    return c;
  }

  private static FlatCard collapsibleCard(final boolean startCollapsed) {
    JPanel body = new JPanel();
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    body.setOpaque(false);
    body.add(new JLabel("Detail line one."));
    body.add(new JLabel("Detail line two."));
    body.add(new JLabel("Detail line three."));
    return new FlatCard()
        .setHeader(
            "Advanced options", startCollapsed ? "Collapsed by default" : "Expanded by default")
        .setBody(body)
        .setCollapsible(true)
        .setCollapsed(startCollapsed)
        .setCollapsedSummary(new JLabel("3 options hidden — click to expand"))
        .setAnimateCollapse(true);
  }

  private static FlatCard fullSlotCard() {
    return new FlatCard()
        .setHeader("Project alpha", "Updated 2 minutes ago")
        .setLeadingIcon(new DotIcon(new Color(72, 130, 180)))
        .setTrailingActions(toolbarButton("⋯"))
        .setMedia(new MediaPlaceholder())
        .setBody(
            new JLabel(
                "<html>The card uses every slot: leading icon, title,"
                    + " subtitle, trailing actions, media, body, and footer.</html>"))
        .setFooter(new JButton("Open"), new JButton("Dismiss"));
  }

  private static FlatCard minimalCard() {
    return new FlatCard().setBody(new JLabel("A card with only a body and the defaults."));
  }

  private static FlatCard disabledCard(final CardInteractionMode m, final String label) {
    FlatCard c =
        new FlatCard()
            .setHeader(label, "Disabled — " + m.name())
            .setBody(new JLabel("Interaction is suppressed."))
            .setInteractionMode(m);
    c.setEnabled(false);
    return c;
  }

  private static JButton toolbarButton(final String text) {
    JButton b = new JButton(text);
    b.putClientProperty("JButton.buttonType", "toolBarButton");
    return b;
  }

  /** Solid colored circle used as a header leading icon stand-in. */
  private static final class DotIcon implements javax.swing.Icon {
    private final Color color;

    DotIcon(final Color c) {
      this.color = c;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(color);
      g2.fill(new Ellipse2D.Float(x, y + 2, 12, 12));
      g2.dispose();
    }

    @Override
    public int getIconWidth() {
      return 14;
    }

    @Override
    public int getIconHeight() {
      return 16;
    }
  }

  /** Stand-in for an image asset; paints a soft gradient plate. */
  private static final class MediaPlaceholder extends JComponent {
    MediaPlaceholder() {
      setPreferredSize(new Dimension(160, 80));
    }

    @Override
    protected void paintComponent(final Graphics g) {
      java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color a = new Color(98, 142, 207);
      Color b = new Color(202, 142, 207);
      g2.setPaint(new java.awt.GradientPaint(0, 0, a, getWidth(), getHeight(), b));
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
      g2.setColor(new Color(255, 255, 255, 180));
      g2.drawString("media", 12, 22);
      g2.dispose();
    }
  }
}
