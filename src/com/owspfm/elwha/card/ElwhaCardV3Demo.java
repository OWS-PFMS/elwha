package com.owspfm.elwha.card;

import com.owspfm.elwha.card.list.CardSelectionMode;
import com.owspfm.elwha.card.list.DefaultCardListModel;
import com.owspfm.elwha.card.list.ElwhaCardList;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

/**
 * Quick visual smoke-test for V3 {@link ElwhaCard} and its companion primitives. Not a full
 * playground (that's #92 in Phase 3) — just one launchable window with as many representative card
 * examples as fit, so the operator can eyeball the chrome + atoms + primitives + interactions
 * end-to-end while #100 is in review.
 *
 * <pre>{@code
 * mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.ElwhaCardV3Demo"
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardV3Demo {

  private ElwhaCardV3Demo() {
    // entry point only
  }

  /**
   * Launches the demo window.
   *
   * @param args command-line args (ignored)
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
          buildFrame().setVisible(true);
        });
  }

  private static JFrame buildFrame() {
    final JFrame frame = new JFrame("ElwhaCard V3 — demo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    final ScrollableViewportPanel root = new ScrollableViewportPanel();
    root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
    root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    root.setBackground(ColorRole.SURFACE.resolve());
    root.add(section("Variants", variantsRow()));
    root.add(section("Header anatomy", anatomyRow()));
    root.add(section("Media + Actions + Divider", mediaRow()));
    root.add(section("Collapse — chevron and expand link", collapseRow()));
    root.add(section("Interactivity — actionable / selectable / both", interactivityRow()));
    root.add(section("Disabled state", disabledRow()));
    root.add(section("Two-tier conversation card (Gmail pattern, spec §4.3)", twoTierRow()));
    root.add(section("ExpansionOverflow.SCROLL (320 dp cap)", scrollOverflowRow()));
    root.add(
        section("ElwhaCardList<T> — selection + keyboard reorder + right-click menu", listRow()));
    final JScrollPane scroll =
        new JScrollPane(
            root,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setBorder(null);
    frame.setContentPane(scroll);
    frame.setSize(820, 900);
    frame.setLocationRelativeTo(null);
    return frame;
  }

  // -------------------------------------------------------------- sections

  private static JPanel section(final String title, final JPanel content) {
    final JPanel panel = new JPanel(new BorderLayout(0, 12));
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 24, 0));
    final JLabel header = new JLabel(title);
    header.setFont(TypeRole.TITLE_MEDIUM.resolve());
    header.setForeground(ColorRole.ON_SURFACE.resolve());
    panel.add(header, BorderLayout.NORTH);
    panel.add(content, BorderLayout.CENTER);
    return panel;
  }

  private static JPanel grid(final int cols) {
    final JPanel p = new JPanel(new GridLayout(0, cols, 16, 16));
    p.setOpaque(false);
    return p;
  }

  // ---------------------------------------------------------- demo content

  private static JPanel variantsRow() {
    final JPanel g = grid(3);
    g.add(simpleCard(CardVariant.ELEVATED, "Elevated", "The default. Soft shadow over the page."));
    g.add(simpleCard(CardVariant.FILLED, "Filled", "Flat tinted surface above the page."));
    g.add(simpleCard(CardVariant.OUTLINED, "Outlined", "Bordered surface flush with the page."));
    return g;
  }

  private static JPanel anatomyRow() {
    final JPanel g = grid(2);

    // Title only
    final ElwhaCard a = ElwhaCard.elevatedCard();
    a.add(new ElwhaCardHeader().setTitle("Title only"));
    g.add(a);

    // Title + subtitle
    final ElwhaCard b = ElwhaCard.elevatedCard();
    b.add(new ElwhaCardHeader().setTitle("Project alpha").setSubtitle("Updated 5 minutes ago"));
    g.add(b);

    // Leading icon
    final ElwhaCard c = ElwhaCard.elevatedCard();
    c.add(
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardLeadingIcon(MaterialIcons.pushPin()))
            .setTitle("Pinned item")
            .setSubtitle("Stays at the top"));
    g.add(c);

    // Leading thumbnail
    final ElwhaCard d = ElwhaCard.elevatedCard();
    d.add(
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardThumbnail(stripeImage(80, 80, new Color(0xFF7043))))
            .setTitle("Daniel Maas")
            .setSubtitle("daniel@example.com"));
    g.add(d);

    // Trailing affordances (icon button + overflow)
    final ElwhaCard e = ElwhaCard.elevatedCard();
    e.add(
        new ElwhaCardHeader()
            .setTitle("With trailing actions")
            .setSubtitle("Icon button + overflow")
            .addTrailing(ElwhaIconButton.standardIconButton(MaterialIcons.pushPin()))
            .addTrailing(ElwhaIconButton.standardIconButton(MaterialIcons.moreVert())));
    g.add(e);

    // Full slot configuration
    final ElwhaCard f = ElwhaCard.elevatedCard();
    f.add(
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardLeadingIcon(MaterialIcons.pushPin()))
            .setTitle("Full slots")
            .setSubtitle("Leading icon + trailing affordances + supporting text")
            .addTrailing(ElwhaIconButton.standardIconButton(MaterialIcons.moreVert())));
    f.add(Box.createVerticalStrut(8));
    f.add(new ElwhaCardSupportingText("Body text uses ON_SURFACE_VARIANT for de-emphasis."));
    g.add(f);

    return g;
  }

  private static JPanel mediaRow() {
    final JPanel g = grid(2);

    // Image media at top — cap the preferred height so the 16:9 ratio doesn't balloon at
    // wide cell widths.
    final ElwhaCard a = ElwhaCard.elevatedCard();
    a.add(ElwhaCardMedia.image(stripeImage(320, 180, new Color(0x42A5F5))).setPreferredHeight(140));
    a.add(Box.createVerticalStrut(8));
    a.add(new ElwhaCardHeader().setTitle("Image media").setSubtitle("Auto-clipped top corners"));
    a.add(new ElwhaCardSupportingText("16:9 aspect ratio by default."));
    g.add(a);

    // Painter-based media
    final ElwhaCard b = ElwhaCard.elevatedCard();
    b.add(
        ElwhaCardMedia.painter(
                (g2, w, h) -> {
                  g2.setPaint(
                      new GradientPaint(0, 0, new Color(0x9C27B0), w, h, new Color(0xFFEB3B)));
                  g2.fillRect(0, 0, w, h);
                })
            .setPreferredHeight(140));
    b.add(Box.createVerticalStrut(8));
    b.add(new ElwhaCardHeader().setTitle("Painter media").setSubtitle("Procedural gradient"));
    g.add(b);

    // Actions row + divider
    final ElwhaCard c = ElwhaCard.elevatedCard();
    c.add(new ElwhaCardHeader().setTitle("With actions").setSubtitle("Leading + trailing"));
    c.add(new ElwhaCardSupportingText("Divider below separates body from actions."));
    c.add(Box.createVerticalStrut(8));
    // INSET per M3 §1.7 — body → actions is a within-body separator, not a collapse-pair.
    c.add(new ElwhaCardDivider(DividerStyle.INSET));
    c.add(Box.createVerticalStrut(8));
    c.add(
        new ElwhaCardActions()
            .addLeading(new JButton("Share"))
            .addTrailing(new JButton("Cancel"))
            .addTrailing(new JButton("Save")));
    g.add(c);

    return g;
  }

  private static JPanel collapseRow() {
    final JPanel g = grid(2);

    // Chevron in header trailing
    final ElwhaCard a = ElwhaCard.elevatedCard().setCollapsible(true);
    final ElwhaCardHeader headerA =
        new ElwhaCardHeader().setTitle("With chevron").setSubtitle("Click chevron to toggle");
    headerA.addTrailing(new ElwhaCardChevron(a));
    a.add(headerA);
    a.setCollapseConstraint(headerA, CollapseRule.ALWAYS_VISIBLE);
    a.add(Box.createVerticalStrut(8));
    final ElwhaCardSupportingText bodyA =
        new ElwhaCardSupportingText(
            "Body hides when collapsed. Header stays visible because it's ALWAYS_VISIBLE.");
    a.add(bodyA);
    g.add(a);

    // ExpandLink at body bottom
    final ElwhaCard b = ElwhaCard.elevatedCard().setCollapsible(true);
    final ElwhaCardHeader headerB =
        new ElwhaCardHeader().setTitle("With expand link").setSubtitle("Text affordance");
    b.add(headerB);
    b.setCollapseConstraint(headerB, CollapseRule.ALWAYS_VISIBLE);
    b.add(Box.createVerticalStrut(8));
    b.add(new ElwhaCardSupportingText("Details hidden when collapsed."));
    b.add(Box.createVerticalStrut(8));
    // FULL per M3 §1.7 — pairs with the ExpandLink below to mark the body/hidden boundary.
    b.add(new ElwhaCardDivider(DividerStyle.FULL));
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(b, "Show details", "Hide details");
    b.add(link);
    b.setCollapseConstraint(link, CollapseRule.ALWAYS_VISIBLE);
    g.add(b);

    return g;
  }

  private static JPanel interactivityRow() {
    final JPanel g = grid(3);

    // Actionable only — click ripples + focus ring
    final ElwhaCard a = ElwhaCard.elevatedCard().setActionable(true);
    a.add(new ElwhaCardHeader().setTitle("Actionable").setSubtitle("Click for ripple"));
    a.add(new ElwhaCardSupportingText("Tab to focus → see the SECONDARY-colored ring."));
    a.addActionListener(e -> System.out.println("Actionable card clicked"));
    g.add(a);

    // Selectable only — click toggles selection badge
    final ElwhaCard b = ElwhaCard.elevatedCard().setSelectable(true);
    b.add(new ElwhaCardHeader().setTitle("Selectable").setSubtitle("Click to toggle the badge"));
    b.add(new ElwhaCardSupportingText("Top-right circle appears when selected."));
    // Need to make it interactable too so the click handler can toggle selection. Otherwise the
    // chassis isn't a click target and the user has to know to call setSelected programmatically.
    b.setActionable(true);
    g.add(b);

    // Actionable + Selectable + Outlined for focus-ring contrast
    final ElwhaCard c = ElwhaCard.outlinedCard().setActionable(true).setSelectable(true);
    c.add(new ElwhaCardHeader().setTitle("Both").setSubtitle("Action fires AND selection toggles"));
    c.add(
        new ElwhaCardSupportingText(
            "Outlined variant — outline swaps to ON_SURFACE when focused."));
    g.add(c);

    return g;
  }

  private static JPanel disabledRow() {
    final JPanel g = grid(3);
    for (final CardVariant variant : CardVariant.values()) {
      final ElwhaCard card = new ElwhaCard().setVariant(variant);
      card.add(new ElwhaCardHeader().setTitle("Disabled " + variant.name().toLowerCase()));
      card.add(new ElwhaCardSupportingText("Container at 0.38 opacity over the variant fill."));
      card.setEnabled(false);
      g.add(card);
    }
    return g;
  }

  private static JPanel twoTierRow() {
    final JPanel g = new JPanel(new BorderLayout());
    g.setOpaque(false);
    final ElwhaCard card = ElwhaCard.outlinedCard();
    card.add(
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardThumbnail(stripeImage(80, 80, new Color(0xE91E63))))
            .setTitle("REI")
            .setSubtitle("Your order has shipped")
            .addTrailing(ElwhaIconButton.standardIconButton(MaterialIcons.moreVert())));
    card.add(Box.createVerticalStrut(8));
    card.add(
        new ElwhaCardSupportingText(
            "<html>Hi Charles, your order #4831 is on its way and should arrive Friday.<br><br>"
                + "Includes: 1× CAMP Speed Comp helmet, 2× Petzl Tikkina headlamp, "
                + "1× Black Diamond Z-Pole.</html>"));
    g.add(card, BorderLayout.CENTER);
    return g;
  }

  private static JPanel scrollOverflowRow() {
    final JPanel g = new JPanel(new BorderLayout());
    g.setOpaque(false);
    final ElwhaCard card = ElwhaCard.elevatedCard();
    card.setExpansionOverflow(ExpansionOverflow.SCROLL);
    card.add(
        new ElwhaCardHeader()
            .setTitle("Tall content, SCROLL overflow")
            .setSubtitle("Capped at 320 dp"));
    for (int i = 1; i <= 20; i++) {
      card.add(new ElwhaCardSupportingText("Line " + i + " — body exceeds the 320 dp cap."));
    }
    g.add(card, BorderLayout.CENTER);
    g.setPreferredSize(new Dimension(0, 340));
    return g;
  }

  private static JPanel listRow() {
    final JPanel g = new JPanel(new BorderLayout());
    g.setOpaque(false);
    final DefaultCardListModel<String> model =
        new DefaultCardListModel<>(
            List.of(
                "Trip plan — Olympic Hot Springs",
                "Cycle: switchbacks at mile 4",
                "Cycle: river crossing at mile 7",
                "Cycle: alpine meadow at mile 9",
                "Trip notes — pack for rain"));
    final ElwhaCardList<String> list = new ElwhaCardList<>(model);
    list.setOrientation(ElwhaListOrientation.VERTICAL);
    list.getSelectionModel().setSelectionMode(CardSelectionMode.MULTIPLE);
    list.setCellRenderer(
        item -> {
          final ElwhaCard card = ElwhaCard.outlinedCard().setActionable(true).setSelectable(true);
          card.add(new ElwhaCardHeader().setTitle(item));
          card.add(
              new ElwhaCardSupportingText(
                  "Cmd+↑ / Cmd+↓ to reorder · right-click for menu · Delete to remove"));
          return card;
        });
    g.add(list, BorderLayout.CENTER);
    g.setPreferredSize(new Dimension(0, 460));
    return g;
  }

  // ------------------------------------------------------------- utilities

  private static ElwhaCard simpleCard(
      final CardVariant variant, final String title, final String body) {
    final ElwhaCard card = new ElwhaCard().setVariant(variant);
    card.add(new ElwhaCardHeader().setTitle(title));
    card.add(new ElwhaCardSupportingText(body));
    return card;
  }

  /**
   * Root panel that reports {@code tracksViewportWidth = true} so the scroll-pane sizes it to the
   * viewport's actual width every layout pass — without this, BoxLayout(Y_AXIS) cascades children's
   * preferred widths upward and the cards balloon past the frame edge.
   */
  private static final class ScrollableViewportPanel extends JPanel implements Scrollable {
    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(
        final Rectangle visibleRect, final int orientation, final int direction) {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(
        final Rectangle visibleRect, final int orientation, final int direction) {
      return 100;
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

  /** Generates a simple striped {@link Image} for thumbnails / media — avoids bundling assets. */
  private static Image stripeImage(final int w, final int h, final Color base) {
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = img.createGraphics();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(base);
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
