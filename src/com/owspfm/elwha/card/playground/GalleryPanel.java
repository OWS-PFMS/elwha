package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.CollapseRule;
import com.owspfm.elwha.card.DividerStyle;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardActions;
import com.owspfm.elwha.card.ElwhaCardChevron;
import com.owspfm.elwha.card.ElwhaCardDivider;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.ElwhaCardThumbnail;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

/**
 * Read-only static gallery of V3 {@link ElwhaCard} configurations. Each section pairs a heading
 * with a row of cards demonstrating one axis of the API: variants, actionability, collapse,
 * disabled state, and real-world patterns (two-tier conversation, OWS loop).
 *
 * <p>This is the "what does it look like" half of the playground; the {@link LiveConfigPanel}
 * handles the "what does it look like under <em>my</em> settings" half.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class GalleryPanel extends JPanel {

  /** Builds the gallery scroller. */
  public GalleryPanel() {
    super(new BorderLayout());
    final ViewportWidthPanel content = new ViewportWidthPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    content.add(
        section(
            "Variants",
            row(
                variantCard(CardVariant.ELEVATED, "Elevated"),
                variantCard(CardVariant.FILLED, "Filled"),
                variantCard(CardVariant.OUTLINED, "Outlined"))));

    content.add(
        section(
            "Actionability",
            row(
                actionabilityCard("Static", false, false),
                actionabilityCard("Actionable", true, false),
                actionabilityCard("Selectable", false, true),
                actionabilityCard("Both", true, true))));

    content.add(
        section(
            "Collapsible — expanded ↔ collapsed",
            row(collapsibleCard(false), collapsibleCard(true))));

    content.add(section("Full slot configuration", row(fullSlotCard())));
    content.add(section("Minimal config (defaults only)", row(minimalCard())));

    content.add(
        section(
            "Card ↔ Button pairings (M3 spec §3.3)",
            row(
                pairingCard(CardVariant.ELEVATED),
                pairingCard(CardVariant.FILLED),
                pairingCard(CardVariant.OUTLINED))));

    content.add(
        section("Two-tier conversation card (Gmail pattern, spec §4.3)", row(twoTierCard())));
    content.add(section("OWS Loop card (real-world pattern)", row(OwsLoopExample.build())));

    content.add(
        section(
            "Disabled states",
            row(
                disabledVariantCard(CardVariant.ELEVATED),
                disabledVariantCard(CardVariant.FILLED),
                disabledVariantCard(CardVariant.OUTLINED))));

    final JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    add(scroll, BorderLayout.CENTER);
  }

  /**
   * Gallery content panel that locks its width to the enclosing {@link JScrollPane} viewport.
   * Without this the {@code GridLayout} rows expand to the cards' natural preferred width — and an
   * {@link ElwhaCard} cooperates with whatever width its parent assigns (spec §3.4), so
   * unconstrained it reports a very wide preferred size. Tracking the viewport width forces the
   * rows to reflow the cards into the visible area; only vertical scrolling remains.
   */
  private static final class ViewportWidthPanel extends JPanel implements Scrollable {
    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(final Rectangle r, final int orient, final int dir) {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(final Rectangle r, final int orient, final int dir) {
      return Math.max(16, r.height - 32);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }

  private static JComponent section(final String title, final JComponent body) {
    final JPanel sec = new JPanel(new BorderLayout());
    sec.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
    final JLabel heading = new JLabel(title);
    heading.putClientProperty("FlatLaf.styleClass", "h3");
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    sec.add(heading, BorderLayout.NORTH);
    sec.add(body, BorderLayout.CENTER);
    sec.setAlignmentX(Component.LEFT_ALIGNMENT);
    return sec;
  }

  private static JComponent row(final JComponent... cards) {
    final JPanel row = new JPanel(new GridLayout(1, cards.length, 12, 12));
    row.setOpaque(false);
    for (final JComponent c : cards) {
      row.add(c);
    }
    return row;
  }

  // ---------------------------------------------------------- card builders

  private static ElwhaCard variantCard(final CardVariant v, final String name) {
    final ElwhaCard card = new ElwhaCard().setVariant(v);
    card.add(new ElwhaCardHeader().setTitle(name).setSubtitle("Variant: " + v.name()));
    card.add(
        new ElwhaCardSupportingText("Shows the " + name.toLowerCase() + " surface treatment."));
    return card;
  }

  private static ElwhaCard actionabilityCard(
      final String label, final boolean actionable, final boolean selectable) {
    final ElwhaCard card =
        ElwhaCard.elevatedCard().setActionable(actionable).setSelectable(selectable);
    card.add(new ElwhaCardHeader().setTitle(label));
    card.add(
        new ElwhaCardSupportingText(
            "actionable=" + actionable + ", selectable=" + selectable + "."));
    if (actionable) {
      card.addActionListener(
          e -> {
            /* demo */
          });
    }
    return card;
  }

  private static ElwhaCard collapsibleCard(final boolean startCollapsed) {
    final ElwhaCard card = ElwhaCard.elevatedCard().setCollapsible(true);
    final ElwhaCardHeader header =
        new ElwhaCardHeader()
            .setTitle("Advanced options")
            .setSubtitle(startCollapsed ? "Collapsed by default" : "Expanded by default");
    header.addTrailing(new ElwhaCardChevron(card));
    card.add(header);
    card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);
    card.add(new ElwhaCardSupportingText("Detail line one."));
    card.add(new ElwhaCardSupportingText("Detail line two."));
    card.add(new ElwhaCardSupportingText("Detail line three."));
    card.setCollapsed(startCollapsed);
    return card;
  }

  private static ElwhaCard fullSlotCard() {
    final ElwhaCard card = ElwhaCard.elevatedCard();
    card.add(
        ElwhaCardMedia.image(stripeImage(320, 180, new Color(0x42A5F5))).setPreferredHeight(120));
    card.add(Box.createVerticalStrut(8));
    card.add(
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardThumbnail(stripeImage(56, 56, new Color(0xFF7043))))
            .setTitle("Project alpha")
            .setSubtitle("Updated 2 minutes ago")
            .addTrailing(ElwhaIconButton.standardIconButton(MaterialIcons.moreVert())));
    card.add(
        new ElwhaCardSupportingText(
            "Every layer 2-3 primitive in one card: media + header with leading thumbnail +"
                + " supporting text + actions."));
    card.add(Box.createVerticalStrut(8));
    card.add(new ElwhaCardDivider(DividerStyle.FULL));
    card.add(Box.createVerticalStrut(8));
    card.add(
        new ElwhaCardActions()
            .addLeading(ElwhaButton.textButton("Share"))
            .addTrailing(ElwhaButton.outlinedButton("Cancel"))
            .addTrailing(ElwhaButton.filledButton("Save")));
    return card;
  }

  /**
   * A card whose action row demonstrates the M3 spec §3.3 Card ↔ Button pairing: each card variant
   * pairs with a specific secondary + primary action-button variant.
   */
  private static ElwhaCard pairingCard(final CardVariant v) {
    final String name = v.name().charAt(0) + v.name().substring(1).toLowerCase();
    final String pairing =
        switch (v) {
          case ELEVATED -> "Outlined + Filled";
          case FILLED -> "Text + Outlined";
          case OUTLINED -> "Text + Filled-tonal";
        };
    final ElwhaCard card = new ElwhaCard().setVariant(v);
    card.add(
        new ElwhaCardHeader().setTitle(name + " card").setSubtitle("CTA pairing — " + pairing));
    card.add(
        new ElwhaCardSupportingText(
            "Per M3 spec §3.3 the "
                + name.toLowerCase()
                + " card variant pairs with the "
                + pairing.toLowerCase()
                + " action-button pair."));
    card.add(new ElwhaCardDivider());
    card.add(
        new ElwhaCardActions().addTrailing(pairingSecondary(v)).addTrailing(pairingPrimary(v)));
    return card;
  }

  private static ElwhaButton pairingSecondary(final CardVariant v) {
    return switch (v) {
      case ELEVATED -> ElwhaButton.outlinedButton("Cancel");
      case FILLED, OUTLINED -> ElwhaButton.textButton("Cancel");
    };
  }

  private static ElwhaButton pairingPrimary(final CardVariant v) {
    return switch (v) {
      case ELEVATED -> ElwhaButton.filledButton("Confirm");
      case FILLED -> ElwhaButton.outlinedButton("Confirm");
      case OUTLINED -> ElwhaButton.filledTonalButton("Confirm");
    };
  }

  private static ElwhaCard minimalCard() {
    final ElwhaCard card = new ElwhaCard();
    card.add(new ElwhaCardSupportingText("A card with only supporting text and the defaults."));
    return card;
  }

  private static ElwhaCard twoTierCard() {
    final ElwhaCard card = ElwhaCard.outlinedCard();
    card.add(
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardThumbnail(stripeImage(72, 72, new Color(0xE91E63))))
            .setTitle("REI")
            .setSubtitle("Your order has shipped")
            .addTrailing(ElwhaIconButton.standardIconButton(MaterialIcons.moreVert())));
    card.add(Box.createVerticalStrut(8));
    card.add(
        new ElwhaCardSupportingText(
            "<html>Hi Charles, your order #4831 is on its way and should arrive Friday.<br><br>"
                + "Includes: 1× CAMP Speed Comp helmet, 2× Petzl Tikkina headlamp, "
                + "1× Black Diamond Z-Pole.</html>"));
    return card;
  }

  private static ElwhaCard disabledVariantCard(final CardVariant v) {
    final ElwhaCard card = new ElwhaCard().setVariant(v);
    card.add(new ElwhaCardHeader().setTitle("Disabled " + v.name().toLowerCase()));
    card.add(new ElwhaCardSupportingText("Container at 0.38 opacity over the variant fill."));
    card.setEnabled(false);
    return card;
  }

  // ------------------------------------------------------------- utilities

  /** Generates a striped {@link Image} for thumbnails / media — avoids bundling assets. */
  static Image stripeImage(final int w, final int h, final Color base) {
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = img.createGraphics();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setPaint(new GradientPaint(0, 0, base, w, h, base.brighter()));
      g2.fillRect(0, 0, w, h);
      g2.setColor(new Color(255, 255, 255, 60));
      for (int x = -h; x < w; x += 24) {
        g2.fillPolygon(new int[] {x, x + 16, x + 16 + h, x + h}, new int[] {0, 0, h, h}, 4);
      }
    } finally {
      g2.dispose();
    }
    return img;
  }
}
