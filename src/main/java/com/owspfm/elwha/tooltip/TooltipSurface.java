package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JPanel;

/**
 * The painted tooltip surface, both variants ({@code elwha-tooltip-design.md} §4/§5).
 *
 * <p><strong>Plain</strong>: a flat {@link ColorRole#INVERSE_SURFACE} round-rect at {@link
 * ShapeScale#XS} carrying a {@link TypeRole#BODY_SMALL} {@link ColorRole#INVERSE_ON_SURFACE} label,
 * word-wrapped by hand at the 200&nbsp;px max width (no HTML views — the #305 doctrine). The
 * wrapped block centers horizontally as a unit; lines inside stay leading-aligned (Compose parity),
 * which collapses to true centering for the single-line common case.
 *
 * <p><strong>Rich</strong>: a {@link ColorRole#SURFACE_CONTAINER} card at {@link ShapeScale#MD}
 * under an elevation-2 {@link ShadowPainter} halo (reserved via {@link #halo()}), with an optional
 * {@link TypeRole#TITLE_SMALL} subhead and {@link TypeRole#BODY_MEDIUM} supporting text — both
 * {@link ColorRole#ON_SURFACE_VARIANT}, wrapped at the 320&nbsp;px max width on the M3 baseline
 * rhythm (28 top→subhead, 24 subhead→text, 16 text→bottom) — and a start-aligned row of {@link
 * ElwhaButton#textButton} action children (row min-height 36, bottom pad 8).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
class TooltipSurface extends JPanel {

  /** Plain horizontal content padding (M3 {@code PlainTooltipContentPadding}). */
  static final int PLAIN_H_PAD_PX = 8;

  /** Plain vertical content padding (M3 {@code PlainTooltipContentPadding}). */
  static final int PLAIN_V_PAD_PX = 4;

  /** Minimum tooltip width (M3 {@code TooltipMinWidth}). */
  static final int MIN_WIDTH_PX = 40;

  /** Minimum tooltip height (M3 {@code TooltipMinHeight}). */
  static final int MIN_HEIGHT_PX = 24;

  /** Plain max width — the label wraps beyond this (M3 {@code plainTooltipMaxWidth}). */
  static final int PLAIN_MAX_WIDTH_PX = 200;

  /** Rich horizontal content padding (M3 {@code RichTooltipHorizontalPadding}). */
  static final int RICH_H_PAD_PX = 16;

  /** Rich max width (M3 {@code richTooltipMaxWidth}). */
  static final int RICH_MAX_WIDTH_PX = 320;

  /** Container top → subhead first baseline (M3 {@code HeightToSubheadFirstLine}). */
  static final int RICH_SUBHEAD_BASELINE_PX = 28;

  /** Subhead baseline → supporting first baseline (M3 {@code HeightFromSubheadToTextFirstLine}). */
  static final int RICH_SUBHEAD_TO_TEXT_PX = 24;

  /** Supporting last baseline → container bottom, no actions (M3 {@code TextBottomPadding}). */
  static final int RICH_TEXT_BOTTOM_PAD_PX = 16;

  /** Action row minimum height (M3 {@code ActionLabelMinHeight}). */
  static final int RICH_ACTION_ROW_MIN_PX = 36;

  /** Action row → container bottom (M3 {@code ActionLabelBottomPadding}). */
  static final int RICH_ACTION_BOTTOM_PAD_PX = 8;

  /** Gap between action buttons. */
  static final int RICH_ACTION_GAP_PX = 8;

  /** Rich container elevation (M3 {@code ContainerElevation} Level2). */
  static final int RICH_ELEVATION = 2;

  private final TooltipVariant variant;
  private final List<ElwhaButton> actionButtons = new ArrayList<>();
  private String text;
  private String subhead;
  private String supportingText;
  private FloatSupplier alphaSupplier = () -> 1f;

  TooltipSurface(final String text) {
    this(TooltipVariant.PLAIN, text, null, null);
  }

  TooltipSurface(
      final TooltipVariant variant,
      final String text,
      final String subhead,
      final String supportingText) {
    setOpaque(false);
    setFocusable(false);
    setLayout(null);
    this.variant = variant;
    this.text = text;
    this.subhead = subhead;
    this.supportingText = supportingText;
  }

  void setText(final String text) {
    this.text = text;
    revalidate();
    repaint();
  }

  String getText() {
    return text;
  }

  void setSubhead(final String subhead) {
    this.subhead = subhead;
    revalidate();
    repaint();
  }

  void setSupportingText(final String supportingText) {
    this.supportingText = supportingText;
    revalidate();
    repaint();
  }

  void addActionButton(final ElwhaButton button) {
    actionButtons.add(button);
    add(button);
  }

  void setAlphaSupplier(final FloatSupplier alphaSupplier) {
    this.alphaSupplier = alphaSupplier;
  }

  /** The fade-alpha feed — the overlay host's eased {@code motionProgress}. */
  interface FloatSupplier {
    float get();
  }

  // The whole surface — container, text, and live action-button children — fades as one unit
  // (design §6 / §12-2: children must not pop in at full alpha mid-entrance). SrcOver at 1.0 is
  // the steady-state no-op.
  @Override
  public void paint(final Graphics g) {
    final float alpha = Math.max(0f, Math.min(1f, alphaSupplier.get()));
    if (alpha >= 1f) {
      super.paint(g);
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
      super.paint(g2);
    } finally {
      g2.dispose();
    }
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext =
          new AccessibleJPanel() {
            @Override
            public AccessibleRole getAccessibleRole() {
              return AccessibleRole.TOOL_TIP;
            }
          };
    }
    return accessibleContext;
  }

  List<ElwhaButton> actionButtons() {
    return actionButtons;
  }

  /**
   * The shadow reserve around the body — elevation-2 insets for the rich card, zero for the flat
   * plain bubble. The placement engine offsets by this so the <em>body</em> aligns to the anchor.
   *
   * @return the halo insets
   */
  Insets halo() {
    return variant == TooltipVariant.RICH
        ? ShadowPainter.shadowInsets(RICH_ELEVATION)
        : new Insets(0, 0, 0, 0);
  }

  @Override
  public Dimension getPreferredSize() {
    if (variant == TooltipVariant.PLAIN) {
      return plainPreferredSize();
    }
    final RichMetrics m = richMetrics();
    final Insets halo = halo();
    return new Dimension(
        m.bodyWidth + halo.left + halo.right, m.bodyHeight + halo.top + halo.bottom);
  }

  @Override
  public void doLayout() {
    if (variant == TooltipVariant.PLAIN || actionButtons.isEmpty()) {
      return;
    }
    final RichMetrics m = richMetrics();
    final Insets halo = halo();
    final boolean rtl = !getComponentOrientation().isLeftToRight();
    int x = rtl ? halo.left + m.bodyWidth - RICH_H_PAD_PX : halo.left + RICH_H_PAD_PX;
    for (final ElwhaButton button : actionButtons) {
      final Dimension pref = button.getPreferredSize();
      final int bx = rtl ? x - pref.width : x;
      final int by = halo.top + m.actionRowY + (m.actionRowHeight - pref.height) / 2;
      button.setBounds(bx, by, pref.width, pref.height);
      x = rtl ? bx - RICH_ACTION_GAP_PX : bx + pref.width + RICH_ACTION_GAP_PX;
    }
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      if (variant == TooltipVariant.PLAIN) {
        paintPlain(g2);
      } else {
        paintRich(g2);
      }
    } finally {
      g2.dispose();
    }
  }

  // ------------------------------------------------------------------ plain

  private Dimension plainPreferredSize() {
    final FontMetrics fm = getFontMetrics(TypeRole.BODY_SMALL.resolve());
    final List<String> lines = wrap(text, fm, PLAIN_MAX_WIDTH_PX - 2 * PLAIN_H_PAD_PX);
    int textWidth = 0;
    for (final String line : lines) {
      textWidth = Math.max(textWidth, fm.stringWidth(line));
    }
    final int width =
        Math.max(MIN_WIDTH_PX, Math.min(textWidth + 2 * PLAIN_H_PAD_PX, PLAIN_MAX_WIDTH_PX));
    final int height = Math.max(MIN_HEIGHT_PX, lines.size() * fm.getHeight() + 2 * PLAIN_V_PAD_PX);
    return new Dimension(width, height);
  }

  private void paintPlain(final Graphics2D g2) {
    final int arc = ShapeScale.XS.px() * 2;
    g2.setColor(ColorRole.INVERSE_SURFACE.resolve());
    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));

    g2.setFont(TypeRole.BODY_SMALL.resolve());
    g2.setColor(ColorRole.INVERSE_ON_SURFACE.resolve());
    final FontMetrics fm = g2.getFontMetrics();
    final List<String> lines = wrap(text, fm, PLAIN_MAX_WIDTH_PX - 2 * PLAIN_H_PAD_PX);
    int blockWidth = 0;
    for (final String line : lines) {
      blockWidth = Math.max(blockWidth, fm.stringWidth(line));
    }
    final int blockX = Math.max(PLAIN_H_PAD_PX, (getWidth() - blockWidth) / 2);
    final int blockHeight = lines.size() * fm.getHeight();
    int y = Math.max(PLAIN_V_PAD_PX, (getHeight() - blockHeight) / 2) + fm.getAscent();
    final boolean rtl = !getComponentOrientation().isLeftToRight();
    for (final String line : lines) {
      final int x = rtl ? blockX + blockWidth - fm.stringWidth(line) : blockX;
      g2.drawString(line, x, y);
      y += fm.getHeight();
    }
  }

  // ------------------------------------------------------------------ rich

  // The shared geometry of the rich card: both getPreferredSize and doLayout/paint derive from
  // this one computation so the painted text and the laid-out buttons can never disagree.
  private RichMetrics richMetrics() {
    final FontMetrics subheadFm = getFontMetrics(TypeRole.TITLE_SMALL.resolve());
    final FontMetrics bodyFm = getFontMetrics(TypeRole.BODY_MEDIUM.resolve());
    final int wrapWidth = RICH_MAX_WIDTH_PX - 2 * RICH_H_PAD_PX;

    final List<String> subheadLines =
        subhead == null || subhead.isEmpty() ? List.of() : wrap(subhead, subheadFm, wrapWidth);
    final List<String> bodyLines = wrap(supportingText, bodyFm, wrapWidth);

    int contentWidth = 0;
    for (final String line : subheadLines) {
      contentWidth = Math.max(contentWidth, subheadFm.stringWidth(line));
    }
    for (final String line : bodyLines) {
      contentWidth = Math.max(contentWidth, bodyFm.stringWidth(line));
    }
    int rowWidth = 0;
    for (final ElwhaButton button : actionButtons) {
      rowWidth += button.getPreferredSize().width;
    }
    if (!actionButtons.isEmpty()) {
      rowWidth += (actionButtons.size() - 1) * RICH_ACTION_GAP_PX;
    }
    contentWidth = Math.min(Math.max(contentWidth, rowWidth), wrapWidth);
    final int bodyWidth = Math.max(MIN_WIDTH_PX, contentWidth + 2 * RICH_H_PAD_PX);

    final int subheadFirstBaseline = RICH_SUBHEAD_BASELINE_PX;
    final int supportingFirstBaseline;
    if (subheadLines.isEmpty()) {
      supportingFirstBaseline = RICH_SUBHEAD_TO_TEXT_PX;
    } else {
      final int lastSubheadBaseline =
          subheadFirstBaseline + (subheadLines.size() - 1) * subheadFm.getHeight();
      supportingFirstBaseline = lastSubheadBaseline + RICH_SUBHEAD_TO_TEXT_PX;
    }
    final int lastBodyBaseline =
        supportingFirstBaseline + (bodyLines.size() - 1) * bodyFm.getHeight();

    final int actionRowY;
    final int actionRowHeight;
    final int bodyHeight;
    if (actionButtons.isEmpty()) {
      actionRowY = -1;
      actionRowHeight = 0;
      bodyHeight = lastBodyBaseline + RICH_TEXT_BOTTOM_PAD_PX;
    } else {
      actionRowY = lastBodyBaseline + bodyFm.getDescent() + 4;
      int rowHeight = RICH_ACTION_ROW_MIN_PX;
      for (final ElwhaButton button : actionButtons) {
        rowHeight = Math.max(rowHeight, button.getPreferredSize().height);
      }
      actionRowHeight = rowHeight;
      bodyHeight = actionRowY + actionRowHeight + RICH_ACTION_BOTTOM_PAD_PX;
    }
    return new RichMetrics(
        subheadLines,
        bodyLines,
        bodyWidth,
        bodyHeight,
        subheadFirstBaseline,
        supportingFirstBaseline,
        actionRowY,
        actionRowHeight);
  }

  private void paintRich(final Graphics2D g2) {
    final RichMetrics m = richMetrics();
    final Insets halo = halo();
    final int bx = halo.left;
    final int by = halo.top;
    final int bw = getWidth() - halo.left - halo.right;
    final int bh = getHeight() - halo.top - halo.bottom;
    final int arc = ShapeScale.MD.px() * 2;

    final Graphics2D sg = (Graphics2D) g2.create();
    sg.translate(bx, by);
    ShadowPainter.paint(sg, bw, bh, arc, RICH_ELEVATION);
    sg.dispose();
    g2.setColor(ColorRole.SURFACE_CONTAINER.resolve());
    g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, arc, arc));

    final boolean rtl = !getComponentOrientation().isLeftToRight();
    g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());

    g2.setFont(TypeRole.TITLE_SMALL.resolve());
    FontMetrics fm = g2.getFontMetrics();
    int baseline = by + m.subheadFirstBaseline;
    for (final String line : m.subheadLines) {
      g2.drawString(line, textX(line, fm, bx, bw, rtl), baseline);
      baseline += fm.getHeight();
    }

    g2.setFont(TypeRole.BODY_MEDIUM.resolve());
    fm = g2.getFontMetrics();
    baseline = by + m.supportingFirstBaseline;
    for (final String line : m.bodyLines) {
      g2.drawString(line, textX(line, fm, bx, bw, rtl), baseline);
      baseline += fm.getHeight();
    }
  }

  private static int textX(
      final String line,
      final FontMetrics fm,
      final int bodyX,
      final int bodyWidth,
      final boolean rtl) {
    return rtl ? bodyX + bodyWidth - RICH_H_PAD_PX - fm.stringWidth(line) : bodyX + RICH_H_PAD_PX;
  }

  private record RichMetrics(
      List<String> subheadLines,
      List<String> bodyLines,
      int bodyWidth,
      int bodyHeight,
      int subheadFirstBaseline,
      int supportingFirstBaseline,
      int actionRowY,
      int actionRowHeight) {}

  /**
   * Greedy word wrap against {@code maxWidth}: explicit {@code \n} forces a break, runs of
   * whitespace collapse, and a single word wider than the wrap width hard-breaks by character so
   * pathological input can never blow past the M3 max width.
   *
   * @param text the label text
   * @param fm the metrics of the font the label paints with
   * @param maxWidth the wrap width in pixels
   * @return the wrapped lines, never empty
   */
  static List<String> wrap(final String text, final FontMetrics fm, final int maxWidth) {
    final List<String> lines = new ArrayList<>();
    for (final String paragraph : text.split("\n", -1)) {
      StringBuilder line = new StringBuilder();
      for (final String word : paragraph.trim().split("\\s+")) {
        if (word.isEmpty()) {
          continue;
        }
        final String candidate = line.isEmpty() ? word : line + " " + word;
        if (fm.stringWidth(candidate) <= maxWidth) {
          line = new StringBuilder(candidate);
          continue;
        }
        if (!line.isEmpty()) {
          lines.add(line.toString());
        }
        String rest = word;
        while (fm.stringWidth(rest) > maxWidth && rest.length() > 1) {
          int cut = rest.length() - 1;
          while (cut > 1 && fm.stringWidth(rest.substring(0, cut)) > maxWidth) {
            cut--;
          }
          lines.add(rest.substring(0, cut));
          rest = rest.substring(cut);
        }
        line = new StringBuilder(rest);
      }
      lines.add(line.toString());
    }
    return lines;
  }
}
