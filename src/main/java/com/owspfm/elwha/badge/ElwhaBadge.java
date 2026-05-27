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
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRelation;
import javax.accessibility.AccessibleRelationSet;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * The M3 Expressive Badge primitive — a single class covering both the {@link Variant#SMALL} (6 dp
 * dot, no content) and {@link Variant#LARGE} (16 dp pill with 1–4 character label) variants in the
 * default {@code Error} / {@code On error} color mapping. Spec lives in {@code
 * docs/research/elwha-badge-design.md}.
 *
 * <p><strong>Phase 1 scope (#210–#214).</strong> Class skeleton + per-variant factories with
 * content validation (null → NPE, empty → IAE, &gt;4 chars → silent truncate per design doc §3),
 * Small + Large container rendering, the color override surface with default {@link
 * ColorRole#ERROR} / {@link ColorRole#ON_ERROR} mapping enforced by construction, Large label paint
 * at {@link TypeRole#LABEL_SMALL} with 4 dp interior padding (§4.2), content-driven Large width via
 * {@link #setContent(String)}, the push-model accessibility surface ({@link
 * #getAccessibilityText()} / {@link #withAccessibilityText(String)} with hardcoded English defaults
 * per §10.3, custom {@link AccessibleContext} reporting {@link AccessibleRole#LABEL} with a {@link
 * AccessibleRelation#LABEL_FOR} relation set by the anchor). Placement geometry, RTL mirroring, and
 * host-name splicing live in the companion {@link ElwhaBadgeAnchor}.
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

  /**
   * Property name fired by {@link #setContent(String)} on change — listened by {@link
   * ElwhaBadgeAnchor} to recompute Large badge bounds when content width changes.
   *
   * @since v0.3.0
   */
  public static final String PROPERTY_CONTENT = "content";

  /**
   * Property name fired when {@link #getAccessibilityText()} effectively changes (either via {@link
   * #withAccessibilityText(String)} or because {@link #setContent(String)} altered the
   * default-derived value). {@link ElwhaBadgeAnchor} listens to keep {@code host.accessibleName} in
   * sync.
   *
   * @since v0.3.0
   */
  public static final String PROPERTY_ACCESSIBILITY_TEXT = "accessibilityText";

  /** Maximum label length (characters, including any trailing {@code +}). Design doc §3. */
  private static final int MAX_CONTENT_LEN = 4;

  /**
   * M3 sentinel for pure-numeric counts &gt; 999. Returned by {@link #coerceContent(String)} and
   * used by {@link #large(int)} on overflow.
   */
  private static final String OVERFLOW_SENTINEL = "999+";

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
  private String accessibilityTextOverride;
  private JComponent labelForTarget;

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
   * Creates a {@link Variant#LARGE Large} badge with the given label content. Content is coerced to
   * fit the 4-character display cap per design doc §3:
   *
   * <ul>
   *   <li>Pure-numeric content &gt; 999 collapses to the M3 {@code "999+"} overflow sentinel — so
   *       {@code large("1234")} and {@code large("999999")} both render as {@code "999+"}.
   *   <li>Non-numeric content longer than 3 characters keeps its first 3 characters and gains a
   *       {@code "+"} suffix — {@code large("Message")} renders as {@code "Mes+"}, {@code
   *       large("BETA")} as {@code "BET+"}.
   *   <li>Shorter content (≤ 3 characters, or numeric ≤ 999) passes through verbatim.
   * </ul>
   *
   * <p>For the common case of displaying a count, prefer {@link #large(int)}.
   *
   * @param content the label content
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
    return new ElwhaBadge(Variant.LARGE, coerceContent(content));
  }

  /**
   * Creates a {@link Variant#LARGE Large} badge displaying a non-negative count. Counts ≤ 999
   * render verbatim ({@code "0"} through {@code "999"}); counts &gt; 999 collapse to the M3 {@code
   * "999+"} overflow sentinel per the four-character cap (design doc §3).
   *
   * @param count the non-negative count to display
   * @return a new Large badge with the count or overflow sentinel as content
   * @throws IllegalArgumentException if {@code count} is negative
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaBadge large(final int count) {
    if (count < 0) {
      throw new IllegalArgumentException("Large badge count must be non-negative");
    }
    return large(Integer.toString(count));
  }

  /**
   * Coerces user-supplied content to the M3 4-character display cap, signaling overflow with a
   * trailing {@code "+"} rather than silently chopping characters. Design doc §3.
   *
   * <ul>
   *   <li>Pure-numeric input &gt; 999 collapses to {@code "999+"}.
   *   <li>Anything else longer than 3 characters keeps its first 3 characters and gains a {@code
   *       "+"} suffix — {@code "Message"} → {@code "Mes+"}, {@code "BETA"} → {@code "BET+"}.
   *   <li>Inputs already short enough (≤ 3 characters, or numeric ≤ 999) pass through verbatim.
   * </ul>
   */
  private static String coerceContent(final String content) {
    if (isAllDigits(content) && exceedsNumericCap(content)) {
      return OVERFLOW_SENTINEL;
    }
    if (content.length() > MAX_CONTENT_LEN - 1) {
      return content.substring(0, MAX_CONTENT_LEN - 1) + "+";
    }
    return content;
  }

  private static boolean isAllDigits(final String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return !s.isEmpty();
  }

  /**
   * Returns true for all-digit strings strictly greater than 999. String-level comparison is safe
   * here because both operands are decimal-digit-only with the same lexical-vs-numeric ordering.
   */
  private static boolean exceedsNumericCap(final String digits) {
    return digits.length() > 3 || (digits.length() == 3 && digits.compareTo("999") > 0);
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
   * {@link #large(String)} factory: {@code null} rejected, empty rejected, pure-numeric &gt; 999
   * collapses to {@code "999+"}, non-numeric longer than 3 characters keeps its first 3 plus a
   * {@code "+"} suffix ({@code "Message"} → {@code "Mes+"}). Triggers a {@code revalidate()} and
   * {@code repaint()} so the container resizes to the new content width.
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
    final String previous = this.content;
    final String previousAccText = getAccessibilityText();
    this.content = coerceContent(content);
    firePropertyChange(PROPERTY_CONTENT, previous, this.content);
    if (accessibilityTextOverride == null) {
      firePropertyChange(PROPERTY_ACCESSIBILITY_TEXT, previousAccText, getAccessibilityText());
    }
    revalidate();
    repaint();
    return this;
  }

  /**
   * Updates the content of a {@link Variant#LARGE Large} badge from a non-negative count. Counts ≤
   * 999 render verbatim; counts &gt; 999 collapse to {@code "999+"} per design doc §3.
   *
   * @param count the non-negative count to display
   * @return this badge for fluent chaining
   * @throws IllegalStateException if this is a {@link Variant#SMALL Small} badge
   * @throws IllegalArgumentException if {@code count} is negative
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaBadge setContent(final int count) {
    if (count < 0) {
      throw new IllegalArgumentException("Large badge count must be non-negative");
    }
    return setContent(Integer.toString(count));
  }

  /**
   * Returns the accessibility announcement for this badge. Default values per design doc §10.3:
   * Small returns {@code "New notification"}; Large returns {@code "{content} new notifications"}.
   * Overridden by {@link #withAccessibilityText(String)} when the consumer wants a custom string.
   *
   * <p>{@link ElwhaBadgeAnchor} splices the returned value into the host's accessible name on
   * attach and on change, so AT users discover the badge via the host destination per the M3
   * accessibility use-case requirement.
   *
   * @return the announcement string AT will hear after the host's accessible name
   * @version v0.3.0
   * @since v0.3.0
   */
  public String getAccessibilityText() {
    if (accessibilityTextOverride != null) {
      return accessibilityTextOverride;
    }
    if (variant == Variant.SMALL) {
      return "New notification";
    }
    return content + " new notifications";
  }

  /**
   * Overrides the default accessibility announcement string. Pass {@code null} to clear the
   * override and revert to the default-derived value (Small: {@code "New notification"}; Large:
   * {@code "{content} new notifications"}).
   *
   * @param text the override string, or {@code null} to clear
   * @return this badge for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaBadge withAccessibilityText(final String text) {
    final String previous = getAccessibilityText();
    this.accessibilityTextOverride = text;
    firePropertyChange(PROPERTY_ACCESSIBILITY_TEXT, previous, getAccessibilityText());
    return this;
  }

  /**
   * Package-private hook called by {@link ElwhaBadgeAnchor} to set or clear the host this badge
   * labels — exposed through the badge's {@link AccessibleContext} as an {@link
   * AccessibleRelation#LABEL_FOR} relation. Not part of the public API; consumers use the anchor
   * primitive's attach/detach surface instead.
   *
   * @param host the host this badge is labelling, or {@code null} to clear the relation
   */
  void anchorSetLabelFor(final JComponent host) {
    this.labelForTarget = host;
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

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaBadge();
    }
    return accessibleContext;
  }

  private final class AccessibleElwhaBadge extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.LABEL;
    }

    @Override
    public String getAccessibleName() {
      final String explicit = accessibleName;
      return explicit != null ? explicit : getAccessibilityText();
    }

    @Override
    public AccessibleRelationSet getAccessibleRelationSet() {
      final AccessibleRelationSet set = super.getAccessibleRelationSet();
      if (labelForTarget != null) {
        set.add(new AccessibleRelation(AccessibleRelation.LABEL_FOR, labelForTarget));
      }
      return set;
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
