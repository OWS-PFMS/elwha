package com.owspfm.elwha.badge;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.SurfacePainter;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * The M3 Expressive Badge primitive — a single class covering both the {@link Variant#SMALL} (6 dp
 * dot, no content) and {@link Variant#LARGE} (16 dp pill with 1–4 character label) variants in the
 * default {@code Error} / {@code On error} color mapping. Spec lives in {@code
 * docs/research/elwha-badge-design.md}.
 *
 * <p><strong>S1 + S2 scope (#210, #211).</strong> S1 shipped the class skeleton, per-variant static
 * factories with content validation (null → NPE, empty → IAE, &gt;4 chars → silent truncate per
 * design doc §3), Small + Large container rendering, and the color override surface with default
 * {@link ColorRole#ERROR} / {@link ColorRole#ON_ERROR} mapping locked in by construction. S2 adds
 * the Large label paint at the {@link TypeRole#LABEL_SMALL} typography role, the 4 dp interior
 * padding per design doc §4.2, content-driven Large width (single-digit square at 16 × 16 dp
 * expanding to fit the 4-character cap, per §4.1), and the {@link #setContent(String)} setter
 * (truncating for Large, {@link IllegalStateException} for Small). Placement via anchor primitive
 * lands in S3 (#212); RTL in S4 (#213); accessibility wiring in S5 (#214).
 *
 * <p><strong>Posture.</strong> Extends {@link JComponent} directly — badges are decorations, not
 * actions; the host destination owns the click / focus / Space / Enter surface. {@link
 * #setFocusable(boolean) Focusable is forced off} in the constructor and not exposed for override
 * (design doc §8.3 / §10.5).
 *
 * <p><strong>Paint pipeline.</strong> {@link SurfacePainter} (full-round round-rect fill in the
 * resolved container color) → label glyph (Large only, resolved {@link TypeRole#LABEL_SMALL} font
 * at paint time).
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaBadge extends JComponent {

  /**
   * Two variants per M3 Expressive — {@link #SMALL} (presence indicator, no content) and {@link
   * #LARGE} (count or short label, content required).
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public enum Variant {

    /**
     * Small — 6 dp solid filled dot, no content; conveys boolean presence ("has unread"). M3
     * default a11y announcement: "New notification" (wired in S5).
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    SMALL,

    /**
     * Large — 16 dp tall rounded-pill container with a 1–4 character label, communicating a
     * quantifiable count or short status. Width grows with content up to a 16 × 34 dp cap (S2).
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    LARGE
  }

  /** Maximum label length (characters, including any trailing {@code +}). Design doc §3. */
  private static final int MAX_CONTENT_LEN = 4;

  /** Small badge box dimension in dp — square, fully round. */
  private static final int SMALL_SIZE_DP = 6;

  /**
   * Large badge container height in dp. Width is content-driven via {@link #getPreferredSize()}.
   */
  private static final int LARGE_HEIGHT_DP = 16;

  /**
   * Large badge interior padding on each side (between container edge and label text), in dp. Per
   * design doc §4.2. With a 16 dp tall container this leaves 8 dp of vertical text height for the
   * {@link TypeRole#LABEL_SMALL} glyph.
   */
  private static final int LARGE_PADDING_DP = 4;

  /**
   * SurfacePainter's {@code arc} parameter is the {@link java.awt.geom.RoundRectangle2D arcWidth}
   * (diameter), not the real radius — see the corner-radius convention noted in the FAB design doc
   * and the #199 hotfix. For a 6 dp square box, an arc of 6 yields a full circle (real radius = 3
   * dp per spec).
   */
  private static final int SMALL_ARC = SMALL_SIZE_DP;

  /**
   * Same diameter-not-radius convention as {@link #SMALL_ARC}. A 16 dp arc on a 16 dp-tall pill
   * yields fully round end caps (real radius = 8 dp per spec).
   */
  private static final int LARGE_ARC = LARGE_HEIGHT_DP;

  private final Variant variant;
  private String content;
  private ColorRole containerColor = ColorRole.ERROR;
  private ColorRole labelColor;

  private ElwhaBadge(final Variant variant, final String content) {
    this.variant = variant;
    this.content = content;
    this.labelColor = variant == Variant.LARGE ? ColorRole.ON_ERROR : null;
    setFocusable(false);
    setOpaque(false);
  }

  /**
   * Creates a {@link Variant#SMALL Small} badge — a 6 dp solid filled dot with no content.
   *
   * @return a new Small badge in the default {@link ColorRole#ERROR} container color
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaBadge small() {
    return new ElwhaBadge(Variant.SMALL, null);
  }

  /**
   * Creates a {@link Variant#LARGE Large} badge with the given label content. Content longer than
   * {@value #MAX_CONTENT_LEN} characters is silently truncated to the first {@value
   * #MAX_CONTENT_LEN} characters per design doc §3 — the cap is a layout invariant.
   *
   * @param content the label content; 1 to {@value #MAX_CONTENT_LEN} characters
   * @return a new Large badge in the default {@link ColorRole#ERROR} / {@link ColorRole#ON_ERROR}
   *     color mapping
   * @throws NullPointerException if {@code content} is {@code null}
   * @throws IllegalArgumentException if {@code content} is empty (use {@link #small()} for the
   *     no-content case)
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaBadge large(final String content) {
    if (content == null) {
      throw new NullPointerException("Large badge content must not be null");
    }
    if (content.isEmpty()) {
      throw new IllegalArgumentException(
          "Large badge content must not be empty — use ElwhaBadge.small() for a no-content badge");
    }
    final String truncated =
        content.length() > MAX_CONTENT_LEN ? content.substring(0, MAX_CONTENT_LEN) : content;
    return new ElwhaBadge(Variant.LARGE, truncated);
  }

  /**
   * Returns this badge's variant.
   *
   * @return the variant set at construction
   * @version v0.3.0
   * @since v0.3.0
   */
  public Variant getVariant() {
    return variant;
  }

  /**
   * Returns this badge's label content, or {@code null} for a {@link Variant#SMALL Small} badge.
   * The returned value is the post-truncation value the badge actually stores — never longer than
   * {@value #MAX_CONTENT_LEN} characters.
   *
   * @return the stored content, or {@code null} for Small
   * @version v0.3.0
   * @since v0.3.0
   */
  public String getContent() {
    return content;
  }

  /**
   * Updates the label content of a {@link Variant#LARGE Large} badge. Same content rules as the
   * {@link #large(String)} factory: {@code null} rejected, empty rejected, &gt; {@value
   * #MAX_CONTENT_LEN} chars silently truncated. Triggers a {@code revalidate()} and {@code
   * repaint()} so the container resizes to the new content width.
   *
   * @param content the new label content
   * @return this badge for fluent chaining
   * @throws IllegalStateException if this is a {@link Variant#SMALL Small} badge — Small has no
   *     label sub-part
   * @throws NullPointerException if {@code content} is {@code null}
   * @throws IllegalArgumentException if {@code content} is empty
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaBadge setContent(final String content) {
    if (variant == Variant.SMALL) {
      throw new IllegalStateException("Small badge has no content — cannot setContent");
    }
    if (content == null) {
      throw new NullPointerException("Large badge content must not be null");
    }
    if (content.isEmpty()) {
      throw new IllegalArgumentException("Large badge content must not be empty");
    }
    this.content =
        content.length() > MAX_CONTENT_LEN ? content.substring(0, MAX_CONTENT_LEN) : content;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the container color role.
   *
   * @return the current container color role
   * @version v0.3.0
   * @since v0.3.0
   */
  public ColorRole getContainerColor() {
    return containerColor;
  }

  /**
   * Returns the label color role, or {@code null} for a {@link Variant#SMALL Small} badge (which
   * has no label sub-part).
   *
   * @return the current label color role, or {@code null} for Small
   * @version v0.3.0
   * @since v0.3.0
   */
  public ColorRole getLabelColor() {
    return labelColor;
  }

  /**
   * Overrides the default {@link ColorRole#ERROR} container color. Consumers must ensure ≥ 3:1
   * contrast against the label color per the design doc §6 / M3 accessibility rule.
   *
   * @param role the new container color role
   * @return this badge for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaBadge withContainerColor(final ColorRole role) {
    if (role == null) {
      throw new NullPointerException("containerColor");
    }
    this.containerColor = role;
    repaint();
    return this;
  }

  /**
   * Overrides the default {@link ColorRole#ON_ERROR} label color. Only valid for {@link
   * Variant#LARGE Large} badges; {@link Variant#SMALL Small} has no label sub-part.
   *
   * @param role the new label color role
   * @return this badge for fluent chaining
   * @throws IllegalStateException if this is a Small badge
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaBadge withLabelColor(final ColorRole role) {
    if (variant == Variant.SMALL) {
      throw new IllegalStateException("Small badge has no label sub-part — cannot set label color");
    }
    if (role == null) {
      throw new NullPointerException("labelColor");
    }
    this.labelColor = role;
    repaint();
    return this;
  }

  @Override
  public Dimension getPreferredSize() {
    if (variant == Variant.SMALL) {
      return new Dimension(SMALL_SIZE_DP, SMALL_SIZE_DP);
    }
    final Font font = TypeRole.LABEL_SMALL.resolve();
    final FontMetrics fm = getFontMetrics(font);
    final int textWidth = fm.stringWidth(content);
    final int width = Math.max(LARGE_HEIGHT_DP, textWidth + LARGE_PADDING_DP * 2);
    return new Dimension(width, LARGE_HEIGHT_DP);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    final int w = getWidth();
    final int h = getHeight();
    final int arc = variant == Variant.SMALL ? SMALL_ARC : LARGE_ARC;
    SurfacePainter.paint((Graphics2D) g, w, h, arc, containerColor, null, null, 0f);
    if (variant == Variant.LARGE) {
      paintLabel((Graphics2D) g, w, h);
    }
  }

  private void paintLabel(final Graphics2D g, final int w, final int h) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      final Font font = TypeRole.LABEL_SMALL.resolve();
      g2.setFont(font);
      g2.setColor(labelColor.resolve());
      final FontMetrics fm = g2.getFontMetrics();
      final int textWidth = fm.stringWidth(content);
      final int x = (w - textWidth) / 2;
      final int y = (h - fm.getHeight()) / 2 + fm.getAscent();
      g2.drawString(content, x, y);
    } finally {
      g2.dispose();
    }
  }
}
