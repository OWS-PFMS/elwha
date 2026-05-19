package com.owspfm.elwha.card.fixes;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardActions;
import com.owspfm.elwha.card.ElwhaCardChevron;
import com.owspfm.elwha.card.ElwhaCardDivider;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardLeadingIcon;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.card.ElwhaCardSubtitle;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.ElwhaCardTitle;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Standalone demo verifying the six card-layer fixes filed against epic #80:
 *
 * <ul>
 *   <li>#105 — §3.4 width-constraint contract (chassis honors parent width, atoms reflow, inter-
 *       element gaps, media cover-fit).
 *   <li>#106 — media corner-clip aligned to chassis via shared body shape.
 *   <li>#107 — M3 paint compliance (focused-outlined replaces resting outline; disabled-outlined
 *       uses OUTLINE at 0.12; disabled container role swap per variant).
 *   <li>#108 — token defaults verification (ShapeScale.MD=12, SpaceScale.LG=16, list gap=8).
 *   <li>#109 — ElwhaCardMedia decorative / alt-text accessibility API.
 *   <li>#110 — collapse-tween shadow perf (root-caused by #122's size-decoupled shadow cache).
 * </ul>
 *
 * <p>Deliberately written from scratch for the card-fixes verification — not derived from the V3
 * playground or the v0.2 gallery. Each tab focuses on one story so a reviewer can confirm the
 * specific fix without wading through unrelated chrome.
 *
 * <p>Run: {@code mvn compile exec:java -Dexec.mainClass= com.owspfm.elwha.card.fixes.CardFixesDemo}
 * on JDK 21.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class CardFixesDemo {

  private CardFixesDemo() {}

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(CardFixesDemo::launch);
  }

  private static void launch() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    final JFrame frame = new JFrame("ElwhaCard V3 — card-layer fixes demo (#105–#110)");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("#105 width + reflow", new WidthReflowTab());
    tabs.addTab("#106 media corner", new MediaCornerTab());
    tabs.addTab("#107 paint compliance", new PaintComplianceTab());
    tabs.addTab("#108 token defaults", new TokenDefaultsTab());
    tabs.addTab("#109 decorative API", new DecorativeMediaTab());
    tabs.addTab("#110 animation perf", new AnimationPerfTab());
    frame.setContentPane(tabs);
    frame.setSize(1180, 820);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // ============================================================== shared helpers

  /**
   * Wraps a tab body in a scroll pane with a description header. The body is nested inside a {@link
   * ViewportTrackingPanel} so the scroll pane locks content WIDTH to the viewport — without this,
   * long text or wide content drives row preferred-width past the viewport and horizontal scroll
   * appears (cards then stretch to natural preferred width instead of fitting the visible frame).
   * Vertical content can still scroll; only width is tracked.
   */
  private static JComponent tabFrame(final String description, final JComponent body) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    final JLabel desc = new JLabel("<html>" + description + "</html>");
    desc.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
    panel.add(desc, BorderLayout.NORTH);
    final ViewportTrackingPanel viewportWrap = new ViewportTrackingPanel(new BorderLayout());
    viewportWrap.add(body, BorderLayout.CENTER);
    panel.add(new JScrollPane(viewportWrap), BorderLayout.CENTER);
    return panel;
  }

  /**
   * A {@link JPanel} that tells the enclosing {@link JScrollPane} its content WIDTH should track
   * the viewport (so cards laid out by the body's {@code GridLayout} / {@code BoxLayout} get
   * cell-width = viewport / cols, not their natural preferred width). Height-tracking stays off so
   * vertical scroll appears when the body genuinely runs taller than the viewport.
   */
  private static final class ViewportTrackingPanel extends JPanel
      implements javax.swing.Scrollable {
    ViewportTrackingPanel(final java.awt.LayoutManager layout) {
      super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(
        final java.awt.Rectangle visibleRect, final int orientation, final int direction) {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(
        final java.awt.Rectangle visibleRect, final int orientation, final int direction) {
      return Math.max(16, visibleRect.height - 32);
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

  /** Builds a fully-loaded card showcasing every primitive — used by several tabs. */
  private static ElwhaCard demoCard(final String title, final String supporting) {
    final ElwhaCard card = ElwhaCard.elevatedCard();
    final ElwhaCardHeader header = new ElwhaCardHeader();
    header.setLeading(new ElwhaCardLeadingIcon(MaterialIcons.info()));
    header.setTitle(new ElwhaCardTitle(title));
    header.setSubtitle(new ElwhaCardSubtitle("subtitle line — secondary metadata"));
    card.add(header);
    card.add(new ElwhaCardSupportingText(supporting));
    card.add(new ElwhaCardDivider());
    final ElwhaCardActions actions = new ElwhaCardActions();
    actions.addTrailing(new JButton("Cancel"));
    actions.addTrailing(new JButton("OK"));
    card.add(actions);
    return card;
  }

  // ============================================================== #105 tab

  /**
   * Story #105 — width-constraint contract. A split pane with a draggable divider; the left side
   * holds cards that should honor whatever width the divider allocates without paint overflow.
   * Below: side-by-side narrow + wide cards demonstrating text reflow at narrow widths.
   */
  private static final class WidthReflowTab extends JPanel {
    WidthReflowTab() {
      super(new BorderLayout());
      final JComponent body = buildBody();
      add(
          tabFrame(
              "Drag the split-pane divider <b>narrower</b> — the cards on the left must not paint "
                  + "past the divider into the gray right pane. Text wraps; inter-element gaps "
                  + "stay 8 dp; chassis padding stays 16 dp; the chassis never refuses to "
                  + "compress.",
              body),
          BorderLayout.CENTER);
    }

    private JComponent buildBody() {
      final JPanel left = new JPanel();
      left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
      left.setBackground(new Color(0xF6, 0xF2, 0xFB));
      left.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
      final ElwhaCard a =
          demoCard(
              "Width-constraint contract",
              "This supporting text must reflow at any width the parent allocates. At narrow widths"
                  + " you should see HTML wrap engage and the chassis honor the cell width without"
                  + " overflow. Atoms now override getMaximumSize so BoxLayout(Y_AXIS) stretches"
                  + " them to chassis-content width.");
      a.setAlignmentX(Component.LEFT_ALIGNMENT);
      left.add(a);
      left.add(Box.createVerticalStrut(12));
      final ElwhaCard b =
          demoCard(
              "Second card — same rules",
              "A second card under the first so vertical stacking behavior is visible. The 8 dp gap"
                  + " between cards lives on ElwhaCardList; here it's a Box strut for the demo but"
                  + " the card's <i>internal</i> rhythm — header→supporting→divider→actions — is"
                  + " the SpaceScale.SM rhythm story #105 added.");
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      left.add(b);

      final JPanel right = new JPanel();
      right.setBackground(new Color(0xE0, 0xE0, 0xE0));
      right.add(new JLabel("Right pane — cards must not paint here."));

      // JSplitPane refuses to drag the divider past either pane's getMinimumSize, and the JPanel
      // default minimum = preferred. The cards' natural-single-line widths (per #20 fix) drive
      // left.preferred to ~1500 px, which would silently block the divider from compressing the
      // cards pane below 1500 — defeating the demo's whole point (showing chassis width-
      // honoring under live drag). Pin both pane minimums to 0 so the divider drags freely.
      left.setMinimumSize(new Dimension(0, 0));
      right.setMinimumSize(new Dimension(0, 0));

      final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
      split.setDividerLocation(420);
      split.setOneTouchExpandable(true);
      split.setContinuousLayout(true);

      final JPanel reflowRow = new JPanel(new GridLayout(1, 2, 16, 0));
      reflowRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
      reflowRow.add(reflowSample("Narrow (220 px)", 220));
      reflowRow.add(reflowSample("Wide (480 px)", 480));

      // BorderLayout instead of BoxLayout(Y_AXIS): the split pane goes in CENTER and gets all
      // remaining vertical space (its natural preferred height = sum of stacked card heights),
      // the reflow row at SOUTH takes its preferred height. The previous BoxLayout setup
      // required forcing split.setPreferredSize(0, 380) — which clipped the cards (they sum to
      // ~512 px tall, exceeding 380) AND interacted badly with the divider drag (the fixed
      // preferred bounds kept the divider from repositioning past certain points). Removing the
      // fixed preferred and using BorderLayout fixes both #15 (vertical clipping) and #16
      // (drag handle non-responsive).
      final JPanel container = new JPanel(new BorderLayout());
      container.add(split, BorderLayout.CENTER);
      container.add(reflowRow, BorderLayout.SOUTH);
      return container;
    }

    private JComponent reflowSample(final String label, final int width) {
      final JPanel wrap = new JPanel(new BorderLayout());
      wrap.setBorder(BorderFactory.createTitledBorder(label));
      final JPanel boxed = new JPanel(new BorderLayout());
      boxed.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
      final ElwhaCard card = demoCard("Reflow check", explainerText());
      card.setMinimumSize(new Dimension(0, 0));
      final JPanel sizer = new JPanel(new BorderLayout());
      sizer.setPreferredSize(new Dimension(width, 320));
      sizer.add(card, BorderLayout.NORTH);
      boxed.add(sizer, BorderLayout.WEST);
      wrap.add(boxed);
      return wrap;
    }

    private String explainerText() {
      return "Wide-card layout uses one or two text lines for the title and a multi-line "
          + "supporting block. At narrow widths the HTML view wraps to whatever width the cell "
          + "gives. Both samples render identical content; only the cell width differs.";
    }
  }

  // ============================================================== #106 tab

  /**
   * Story #106 — media corner-clip aligned with chassis. Cards rendered at large corner radii with
   * a high-contrast checkerboard media painter so any pixel leakage outside the chassis curve is
   * immediately visible. All three variants tested.
   */
  private static final class MediaCornerTab extends JPanel {
    MediaCornerTab() {
      super(new BorderLayout());
      add(
          tabFrame(
              "Each card paints a high-contrast checkerboard <b>media</b> region at the top. The "
                  + "media is sized to fill the chassis width; the chassis owns the top-corner "
                  + "rounding via SurfacePainter.bodyShape. <b>Zoom in</b> on the top-left and "
                  + "top-right corners — there must be no media pixels visible outside the chassis "
                  + "curve. All three variants use shape <code>ShapeScale.LG</code> (16 dp) here "
                  + "to make corner-shape mismatches obvious.",
              buildBody()),
          BorderLayout.CENTER);
    }

    private JComponent buildBody() {
      final JPanel row = new JPanel(new GridLayout(1, 3, 16, 16));
      row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
      row.add(cornerCard(withLargeCorner(ElwhaCard.elevatedCard()), "ELEVATED"));
      row.add(cornerCard(withLargeCorner(ElwhaCard.filledCard()), "FILLED"));
      row.add(cornerCard(withLargeCorner(ElwhaCard.outlinedCard()), "OUTLINED"));
      return row;
    }

    private ElwhaCard withLargeCorner(final ElwhaCard card) {
      card.setShape(ShapeScale.LG);
      return card;
    }

    private JComponent cornerCard(final ElwhaCard card, final String label) {
      final ElwhaCardMedia media =
          ElwhaCardMedia.painter((g, w, h) -> paintCheckerboard(g, w, h, 20));
      media.setPreferredHeight(160);
      card.add(media);
      final ElwhaCardSupportingText body =
          new ElwhaCardSupportingText(
              "<b>"
                  + label
                  + "</b> — checkerboard above. Top corners must clip to the chassis "
                  + "curve cleanly; no crescents of media bleeding past the curve.");
      card.add(body);
      final JPanel pad = new JPanel(new BorderLayout());
      pad.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      pad.add(card);
      return pad;
    }
  }

  // ============================================================== #107 tab

  /**
   * Story #107 — paint compliance grid. 3 × 3 grid of (variant × state). Resting / Focused
   * (actionable + focus owner) / Disabled. The Outlined row at focus should show a single 2 dp
   * ON_SURFACE ring (not the resting outline plus a ring); the Outlined disabled cell uses OUTLINE
   * at 0.12; Elevated and Filled disabled cells visibly swap container role.
   */
  private static final class PaintComplianceTab extends JPanel {
    PaintComplianceTab() {
      super(new BorderLayout());
      add(
          tabFrame(
              "Three variants × three states. <b>Click any card</b> in the Focused column to give "
                  + "it focus and inspect the focus ring (Elevated/Filled = SECONDARY, Outlined = "
                  + "ON_SURFACE, replacing the resting outline rather than double-stacking inside "
                  + "it). Disabled column: Elevated swaps container to SURFACE, Filled to "
                  + "SURFACE_VARIANT, Outlined keeps SURFACE but its border swaps to OUTLINE at "
                  + "12% opacity (PL-10).",
              buildBody()),
          BorderLayout.CENTER);
    }

    private JComponent buildBody() {
      final JPanel grid = new JPanel(new GridLayout(4, 4, 12, 12));
      grid.add(new JLabel("", SwingConstants.CENTER));
      grid.add(stateHeader("Resting"));
      grid.add(stateHeader("Focused (click to focus)"));
      grid.add(stateHeader("Disabled"));
      addRow(grid, "ELEVATED", ElwhaCard::elevatedCard);
      addRow(grid, "FILLED", ElwhaCard::filledCard);
      addRow(grid, "OUTLINED", ElwhaCard::outlinedCard);
      return grid;
    }

    private JLabel stateHeader(final String text) {
      final JLabel label = new JLabel(text, SwingConstants.CENTER);
      final Font f = label.getFont();
      label.setFont(f.deriveFont(Font.BOLD, f.getSize2D()));
      return label;
    }

    private void addRow(
        final JPanel grid,
        final String name,
        final java.util.function.Supplier<ElwhaCard> factory) {
      grid.add(stateHeader(name));
      grid.add(cellCard(factory.get(), "Resting"));
      final ElwhaCard focusCell = factory.get();
      focusCell.setActionable(true);
      grid.add(cellCard(focusCell, "Click → focused"));
      final ElwhaCard disabled = factory.get();
      disabled.setEnabled(false);
      grid.add(cellCard(disabled, "Disabled"));
    }

    private JComponent cellCard(final ElwhaCard card, final String label) {
      card.add(new ElwhaCardHeader().setTitle(label).setSubtitle("Inspect chrome"));
      card.add(new ElwhaCardSupportingText("Body content fades to 0.38 when disabled."));
      final JPanel wrap = new JPanel(new BorderLayout());
      wrap.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      wrap.add(card);
      return wrap;
    }
  }

  // ============================================================== #108 tab

  /**
   * Story #108 — token defaults verification. Reads the live token values and asserts each matches
   * the M3 canonical measurement.
   */
  private static final class TokenDefaultsTab extends JPanel {
    TokenDefaultsTab() {
      super(new BorderLayout());
      final JPanel body = new JPanel();
      body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
      body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      body.add(verificationRow("ShapeScale.MD.px()", ShapeScale.MD.px(), 12));
      body.add(verificationRow("SpaceScale.LG.px()", SpaceScale.LG.px(), 16));
      body.add(verificationRow("SpaceScale.SM.px() (default list itemGap)", SpaceScale.SM.px(), 8));
      add(
          tabFrame(
              "Three M3 canonical measurements per m3-card-spec-organized.md §4.1. All three are "
                  + "already at spec on main — this tab is the public verification.",
              body),
          BorderLayout.CENTER);
    }

    private JComponent verificationRow(final String name, final int actual, final int expected) {
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
      row.setAlignmentX(Component.LEFT_ALIGNMENT);
      row.add(new JLabel(name + ":"));
      final JLabel value = new JLabel(actual + " px");
      value.setFont(value.getFont().deriveFont(Font.BOLD));
      row.add(value);
      row.add(new JLabel("expected " + expected));
      final JLabel verdict = new JLabel(actual == expected ? "PASS" : "FAIL");
      verdict.setForeground(
          actual == expected ? new Color(0x1B, 0x5E, 0x20) : new Color(0xB7, 0x1C, 0x1C));
      verdict.setFont(verdict.getFont().deriveFont(Font.BOLD));
      row.add(verdict);
      return row;
    }
  }

  // ============================================================== #109 tab

  /**
   * Story #109 — decorative / informative media accessibility API. Toggle the checkbox and edit the
   * alt-text; the live AccessibleContext readout below shows what assistive tech sees.
   */
  private static final class DecorativeMediaTab extends JPanel {
    private final JLabel atReadout = new JLabel();
    private final ElwhaCardMedia media;

    DecorativeMediaTab() {
      super(new BorderLayout());
      media = ElwhaCardMedia.painter((g, w, h) -> paintGradientStrip(g, w, h));
      media.setPreferredHeight(200);
      media.setAltText("A purple-to-blue gradient strip (default informative media).");

      final ElwhaCard card = ElwhaCard.elevatedCard();
      card.add(media);
      card.add(
          new ElwhaCardSupportingText(
              "The media above is a gradient strip. Toggle decorative below to see the "
                  + "AccessibleRole flip from ICON to LABEL and the alt-text drop out of the AT "
                  + "tree."));

      final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
      final JCheckBox decorative = new JCheckBox("setDecorative(true)");
      final JTextField alt = new JTextField(media.getAltText(), 36);
      decorative.addActionListener(
          e -> {
            media.setDecorative(decorative.isSelected());
            updateReadout();
          });
      alt.addActionListener(
          e -> {
            media.setAltText(alt.getText());
            updateReadout();
          });
      controls.add(decorative);
      controls.add(new JLabel("alt-text (Enter to apply):"));
      controls.add(alt);

      atReadout.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      updateReadout();

      final JPanel stack = new JPanel();
      stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
      final JPanel cardWrap = new JPanel(new BorderLayout());
      cardWrap.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      cardWrap.add(card);
      stack.add(cardWrap);
      stack.add(controls);
      stack.add(atReadout);
      add(
          tabFrame(
              "Story #109 adds ElwhaCardMedia.setDecorative(boolean) + setAltText(String). "
                  + "Informative media reports AccessibleRole.ICON with the alt-text as accessible "
                  + "description. Decorative media reports AccessibleRole.LABEL with null name / "
                  + "description so assistive technology skips the node.",
              stack),
          BorderLayout.CENTER);
    }

    private void updateReadout() {
      final AccessibleContext ctx = media.getAccessibleContext();
      atReadout.setText(
          String.format(
              "<html><b>AccessibleRole:</b> %s &nbsp;&nbsp; "
                  + "<b>AccessibleDescription:</b> %s &nbsp;&nbsp; "
                  + "<b>isDecorative():</b> %s</html>",
              ctx.getAccessibleRole(),
              ctx.getAccessibleDescription() == null
                  ? "(null — AT skips)"
                  : ctx.getAccessibleDescription(),
              media.isDecorative()));
    }
  }

  // ============================================================== #110 tab

  /**
   * Story #110 — collapse animation perf. Three side-by-side cards with collapse chevrons; clicking
   * any chevron triggers the 250 ms tween. The label below averages frame intervals across the
   * tween so the smoothness gain from the size-decoupled shadow cache (#122) is measurable.
   */
  private static final class AnimationPerfTab extends JPanel {
    private final JLabel frameStats =
        new JLabel("Click any chevron to collapse / expand a card. Frame stats render below.");

    AnimationPerfTab() {
      super(new BorderLayout());
      final JPanel row = new JPanel(new GridLayout(1, 3, 12, 12));
      row.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      row.add(buildCollapsibleCard("Card A"));
      row.add(buildCollapsibleCard("Card B"));
      row.add(buildCollapsibleCard("Card C"));

      frameStats.setBorder(BorderFactory.createEmptyBorder(12, 8, 0, 8));

      final JPanel stack = new JPanel(new BorderLayout());
      stack.add(row, BorderLayout.CENTER);
      stack.add(frameStats, BorderLayout.SOUTH);

      add(
          tabFrame(
              "Three collapsible cards arranged in a row. Click any chevron to trigger the 250 ms"
                  + " collapse / expand tween. Before #122, the per-instance shadow cache was keyed"
                  + " on body size, so the per-frame height change invalidated it and the two-pass"
                  + " ConvolveOp blur re-fired every frame (and cascaded to the sibling cards in"
                  + " the same GridLayout row when their heights changed too). Now ShadowPainter's"
                  + " cache is keyed on (arc, elevation) only — the height tween no longer"
                  + " invalidates it, so each frame costs just a 9-slice draw. Animation should"
                  + " feel smooth, frame stats stay tight.",
              stack),
          BorderLayout.CENTER);
    }

    private JComponent buildCollapsibleCard(final String name) {
      final ElwhaCard card = ElwhaCard.elevatedCard();
      card.setCollapsible(true);
      card.setAnimateCollapse(true);
      final ElwhaCardHeader header = new ElwhaCardHeader();
      header.setTitle(name);
      header.setSubtitle("Click chevron →");
      header.addTrailing(new ElwhaCardChevron(card));
      card.add(header);
      // Header auto-anchored ALWAYS_VISIBLE by the chevron's addNotify (#23). Consumers can
      // override here with setCollapseConstraint if they want different behavior.
      final ElwhaCardSupportingText body =
          new ElwhaCardSupportingText(
              "Collapsible body content — multiple lines so the tween has noticeable amplitude. "
                  + "The shadow used to recompute on every frame; now ShadowPainter's "
                  + "(arc, elevation)-keyed cache survives the height tween untouched.");
      card.add(body);
      card.add(new ElwhaCardDivider());
      final ElwhaCardActions actions = new ElwhaCardActions();
      actions.addTrailing(new JButton("Action"));
      card.add(actions);
      attachFrameProbe(card);
      return card;
    }

    private void attachFrameProbe(final ElwhaCard card) {
      card.addPropertyChangeListener(
          ElwhaCard.PROPERTY_COLLAPSED,
          e -> SwingUtilities.invokeLater(() -> measureAnimation(card)));
    }

    private void measureAnimation(final ElwhaCard card) {
      final long[] last = {System.nanoTime()};
      final long[] sumNs = {0};
      final int[] frames = {0};
      final long[] maxFrameNs = {0};
      final Timer probe =
          new Timer(
              16,
              e -> {
                final long now = System.nanoTime();
                final long dt = now - last[0];
                last[0] = now;
                sumNs[0] += dt;
                maxFrameNs[0] = Math.max(maxFrameNs[0], dt);
                frames[0]++;
              });
      probe.setRepeats(true);
      probe.start();
      new Timer(
              350,
              e -> {
                probe.stop();
                final double avgMs = frames[0] == 0 ? 0 : sumNs[0] / 1_000_000.0 / frames[0];
                final double maxMs = maxFrameNs[0] / 1_000_000.0;
                frameStats.setText(
                    String.format(
                        "<html>Last tween: <b>%d frames</b> sampled · avg <b>%.1f ms</b>/frame · "
                            + "max <b>%.1f ms</b>/frame &nbsp;<i>(target: ~16.7 ms; before #110 "
                            + "saw 30–50 ms with cascading sibling recomputes)</i></html>",
                        frames[0], avgMs, maxMs));
                ((Timer) e.getSource()).stop();
              })
          .start();
    }
  }

  // ============================================================== painters

  private static void paintGradientStrip(final Graphics2D g2, final int w, final int h) {
    g2.setPaint(
        new java.awt.GradientPaint(
            0,
            0,
            ColorRole.PRIMARY_CONTAINER.resolve(),
            w,
            h,
            ColorRole.SECONDARY_CONTAINER.resolve()));
    g2.fillRect(0, 0, w, h);
    g2.setColor(ColorRole.ON_PRIMARY_CONTAINER.resolve());
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24f));
    final String text = "MEDIA";
    final java.awt.FontMetrics fm = g2.getFontMetrics();
    final int tx = (w - fm.stringWidth(text)) / 2;
    final int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
    g2.drawString(text, tx, ty);
  }

  /**
   * Renders a high-contrast checkerboard so any corner-clip leakage stands out. {@code cell} is the
   * cell size in pixels; the alternating colors maximize edge contrast at the chassis curve.
   */
  private static void paintCheckerboard(
      final Graphics2D g2, final int w, final int h, final int cell) {
    final Color a = new Color(0x1A, 0x73, 0xE8);
    final Color b = new Color(0xFF, 0xC1, 0x07);
    for (int y = 0; y < h; y += cell) {
      for (int x = 0; x < w; x += cell) {
        g2.setColor((((x / cell) + (y / cell)) & 1) == 0 ? a : b);
        g2.fillRect(x, y, cell, cell);
      }
    }
  }

  /**
   * Convenience: produce a {@link BufferedImage} from a painter so the {@code image(...)} factory
   * branch is also exercised. Not currently used but kept as a reference for future demos.
   */
  @SuppressWarnings("unused")
  private static BufferedImage rasterize(
      final int w, final int h, final java.util.function.BiConsumer<Graphics2D, Dimension> paint) {
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = img.createGraphics();
    try {
      paint.accept(g2, new Dimension(w, h));
    } finally {
      g2.dispose();
    }
    return img;
  }
}
