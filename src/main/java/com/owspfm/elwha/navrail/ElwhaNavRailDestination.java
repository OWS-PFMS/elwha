package com.owspfm.elwha.navrail;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.badge.IconBearing;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ContentMorphPainter;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * One slot of an M3 Expressive Navigation Rail — the "rail button". A {@link JComponent}
 * implementing {@link IconBearing} that paints a single icon (with an optional label) and exposes a
 * pill-shaped active-indicator region for state-layer / ripple / future selected paint.
 *
 * <p><strong>Phase 3 — both variants and the Collapsed↔Expanded morph.</strong> Collapsed renders
 * icon-over-label with a 32×56 icon pill (Phase 1 layout). Expanded renders icon-beside-label with
 * a 56-tall Hug-width row pill (icon left, label inline). The variant is pushed in by the parent
 * {@link ElwhaNavigationRail} via the package-private {@code setHostVariant(Variant)}; the morph
 * progress is pushed in via {@code setMorphProgress(float, Variant, Variant)} once per {@link
 * com.owspfm.elwha.theme.MorphAnimator} tick. The destination is not animator-aware itself — the
 * container owns the clock and broadcasts progress to every destination in lock-step (design doc
 * §9.2).
 *
 * <p><strong>Geometry (M3 token-locked, see {@code elwha-navigation-rail-design.md} §4).</strong>
 *
 * <ul>
 *   <li><strong>Collapsed:</strong> {@value #COLLAPSED_WIDTH_PX} dp wide × {@value
 *       #COLLAPSED_CONTENT_HEIGHT_PX} dp tall. The 32×56 indicator pill is centered horizontally at
 *       the top; the 24-dp icon glyph centers inside it; the label sits {@value
 *       #ICON_LABEL_GAP_COLLAPSED} dp below, centered horizontally.
 *   <li><strong>Expanded:</strong> Hug-width × {@value #EXPANDED_CONTENT_HEIGHT_PX} dp tall, full
 *       row pill (corner-radius = height/2). Inside the pill: {@value #LEADING_PAD_EXPANDED} dp
 *       leading pad + 24 dp icon + {@value #ICON_LABEL_GAP_EXPANDED} dp gap + label + {@value
 *       #TRAILING_PAD_EXPANDED} dp trailing pad. The destination's hit target is its full bounds
 *       regardless of pill shape.
 *   <li><strong>Mid-morph (rail-driven):</strong> indicator width lerps {@code 56 → hugWidth},
 *       indicator height lerps {@code 32 → 56}, indicator left-anchor slides {@code centeredAtTop →
 *       leftAtRow}. Label paint anchor switches discretely at progress 0.5 (stacked below for
 *       {@code [0, 0.5)}, inline beside for {@code [0.5, 1.0]}); label alpha cross-fades via {@link
 *       ContentMorphPainter#labelAlpha(float)} symmetric about 0.5.
 * </ul>
 *
 * <p><strong>Paint contract.</strong>
 *
 * <ul>
 *   <li>Surface is transparent — the rail container paints the rail's own surface behind a row of
 *       destinations.
 *   <li>State-layer overlay paints at the {@link StateLayer#HOVER hover}, {@link StateLayer#FOCUS
 *       focus}, and {@link StateLayer#PRESSED pressed} opacities — clipped to the pill shape so the
 *       affordance reads as "the icon got hit" in Collapsed and "the whole row got hit" in
 *       Expanded.
 *   <li>Ripple is seeded at the click point and clipped to the pill region — matches state-layer
 *       scope, same pattern as Button / Chip ripple clipping.
 *   <li>Focus ring: thin {@link ColorRole#PRIMARY} stroke around the pill (M3 keyboard-focus
 *       affordance), only when the destination owns focus.
 * </ul>
 *
 * <p><strong>API shape</strong> (full sketch in {@code elwha-navigation-rail-design.md} §8):
 *
 * <pre>{@code
 * ElwhaNavRailDestination home =
 *     ElwhaNavRailDestination.of(MaterialIcons.symbol("widgets"), "Home");
 * home.addActionListener(e -> openHomeTab());
 * }</pre>
 *
 * <p>The variant (Collapsed / Expanded) and the selected boolean are pushed down by the parent
 * {@code ElwhaNavigationRail} — they are not part of the destination's public surface.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavRailDestination extends JComponent implements IconBearing {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  static final int COLLAPSED_WIDTH_PX = 96;
  static final int COLLAPSED_CONTENT_HEIGHT_PX = 56;
  static final int EXPANDED_CONTENT_HEIGHT_PX = 56;
  static final int INDICATOR_WIDTH_COLLAPSED_PX = 56;
  static final int INDICATOR_HEIGHT_COLLAPSED_PX = 32;
  static final int INDICATOR_HEIGHT_EXPANDED_PX = 56;
  static final int ICON_SIZE_PX = 24;
  static final int ICON_LABEL_GAP_COLLAPSED = 4;
  static final int ICON_LABEL_GAP_EXPANDED = 8;
  static final int LEADING_PAD_EXPANDED = 16;
  static final int TRAILING_PAD_EXPANDED = 16;
  static final float LABEL_ANCHOR_SWITCH_PROGRESS = 0.5f;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;
  private static final float FOCUS_RING_STROKE = 2f;
  private static final float ICON_SWAP_PROGRESS = 0.5f;

  private final Icon iconUnselected;
  private final Icon iconSelected;
  private final String label;

  private boolean selected;
  private boolean hovered;
  private boolean pressed;

  private ElwhaBadge badge;
  private ElwhaBadgeAnchor.Attachment badgeAttachment;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;
  private Timer hoverPollTimer;

  private final List<ActionListener> actionListeners = new ArrayList<>();

  private final FlatSVGIcon.ColorFilter iconFilter = new FlatSVGIcon.ColorFilter(c -> iconColor());

  private ElwhaNavigationRail.Variant hostVariant = ElwhaNavigationRail.Variant.COLLAPSED;
  private boolean morphing;
  private ElwhaNavigationRail.Variant morphFrom = ElwhaNavigationRail.Variant.COLLAPSED;
  private ElwhaNavigationRail.Variant morphTo = ElwhaNavigationRail.Variant.COLLAPSED;
  private float morphProgress;

  private final MorphAnimator selectionAnimator = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);

  private ElwhaNavRailDestination(
      final Icon iconUnselected, final Icon iconSelected, final String label) {
    this.iconUnselected = Objects.requireNonNull(iconUnselected, "iconUnselected");
    this.iconSelected = Objects.requireNonNull(iconSelected, "iconSelected");
    this.label = Objects.requireNonNull(label, "label");
    applyIconColorFilter(iconUnselected);
    applyIconColorFilter(iconSelected);
    setOpaque(false);
    setFocusable(true);
    initInteraction();
  }

  private void applyIconColorFilter(final Icon icon) {
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
  }

  /**
   * Constructs a destination from a {@link MaterialIcons.Symbol} — the primary path. The symbol's
   * fill-0 glyph becomes the unselected icon; its fill-1 (or the unfilled fallback when no
   * fill-axis variant is bundled) becomes the selected icon.
   *
   * @param icon the Material symbol handle
   * @param label the destination label; required, never empty
   * @return a new destination
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaNavRailDestination of(final MaterialIcons.Symbol icon, final String label) {
    Objects.requireNonNull(icon, "icon");
    return new ElwhaNavRailDestination(
        icon.unselected(ICON_SIZE_PX), icon.selected(ICON_SIZE_PX), label);
  }

  /**
   * Constructs a destination from two arbitrary {@link Icon} instances — the escape hatch for
   * consumers with custom (non-Material) glyphs that still want the unselected / selected swap.
   *
   * @param unselected icon painted when this destination is not selected
   * @param selected icon painted when this destination is selected
   * @param label the destination label; required, never empty
   * @return a new destination
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaNavRailDestination of(
      final Icon unselected, final Icon selected, final String label) {
    return new ElwhaNavRailDestination(unselected, selected, label);
  }

  /** The destination's label text. */
  public String getLabel() {
    return label;
  }

  /** The icon rendered in the unselected state. */
  public Icon getIconUnselected() {
    return iconUnselected;
  }

  /** The icon rendered in the selected state. */
  public Icon getIconSelected() {
    return iconSelected;
  }

  /**
   * Reports whether this destination is currently the rail's selected destination. The container is
   * the single source of truth — this getter reflects the most-recent {@link #setSelected(boolean)}
   * push from the parent rail.
   *
   * @return {@code true} if this destination paints in its selected form
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Updates this destination's selected state. Push-only from the parent {@code
   * ElwhaNavigationRail} container — clicking the destination does <em>not</em> auto-flip this
   * field; the container reacts to the action event and decides which destination is selected.
   *
   * <p>The paint transition is animated via the M3 <strong>grow-from-center</strong> motion: the
   * active-indicator pill expands outward from the icon's geometric center to its full Collapsed
   * 32×56 (or Expanded full-row) size over {@link MorphAnimator#MEDIUM2_MS}. Going unselected
   * reverses the animation. Under {@link MorphAnimator#isReducedMotion() reduced motion} the paint
   * snaps to the end state with no in-between frames.
   *
   * @param selected new selected state
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setSelected(final boolean selected) {
    if (this.selected == selected) {
      return;
    }
    final boolean previous = this.selected;
    this.selected = selected;
    if (selected) {
      selectionAnimator.start();
    } else {
      selectionAnimator.reverse();
    }
    firePropertyChange(PROPERTY_SELECTED, previous, selected);
    repaint();
  }

  /**
   * Anchors a badge to this destination's icon. Passing {@code null} detaches any current badge.
   * Setting a new non-null badge while one is already attached cleanly detaches the prior one
   * first.
   *
   * <p>The badge is positioned by {@link ElwhaBadgeAnchor} using {@link #getIconBounds()} —
   * upper-right of the icon glyph by default. Accessibility content from the badge splices into
   * this destination's accessible name via the anchor's push-model (see {@code ElwhaBadgeAnchor}).
   *
   * <p>In Expanded variant the badge tracks the icon's new position (left-anchored, vertically
   * centered) automatically because the anchor reads {@link #getIconBounds()} every frame.
   *
   * @param badge the badge to anchor, or {@code null} to clear
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setBadge(final ElwhaBadge badge) {
    if (this.badge == badge) {
      return;
    }
    if (badgeAttachment != null) {
      ElwhaBadgeAnchor.detach(badgeAttachment);
      badgeAttachment = null;
    }
    this.badge = badge;
    if (badge != null) {
      badgeAttachment = ElwhaBadgeAnchor.attach(this, badge);
    }
  }

  /**
   * Returns the currently attached badge, or {@code null} if none.
   *
   * @return the attached badge, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaBadge getBadge() {
    return badge;
  }

  /**
   * Adds an action listener. Fires on click and on keyboard activation (Space / Enter).
   *
   * @param listener the listener to add; null is ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addActionListener(final ActionListener listener) {
    if (listener != null) {
      actionListeners.add(listener);
    }
  }

  /**
   * Removes a previously added action listener.
   *
   * @param listener the listener to remove
   * @version v0.3.0
   * @since v0.3.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  // -------------------------------------------------------------- variant / morph

  /**
   * Pushes the parent rail's variant into this destination — drives the static layout (icon-over-
   * label vs icon-beside-label) and indicator paint. Called by {@link ElwhaNavigationRail} whenever
   * {@code setVariant(Variant)} runs. Not for consumer use.
   *
   * @param v the new variant
   */
  void setHostVariant(final ElwhaNavigationRail.Variant v) {
    if (this.hostVariant == v && !this.morphing) {
      return;
    }
    this.hostVariant = v;
    this.morphing = false;
    this.morphProgress = 0f;
    revalidate();
    repaint();
  }

  /** The host variant currently driving this destination's layout. Package-private for tests. */
  ElwhaNavigationRail.Variant getHostVariant() {
    return hostVariant;
  }

  /**
   * Drives one frame of the Collapsed↔Expanded morph. The rail container owns the {@link
   * com.owspfm.elwha.theme.MorphAnimator} and broadcasts {@code progress} to every destination per
   * tick. {@code progress} runs {@code [0, 1]} regardless of direction — the destination reads the
   * {@code from}/{@code to} parameters to interpret which endpoint is which.
   *
   * <p>When {@code progress} reaches {@code 1.0} the destination snaps its host variant to {@code
   * to} and clears the morphing flag — subsequent paints render the static end-state.
   *
   * @param progress the eased animation phase in {@code [0, 1]}
   * @param from the starting variant
   * @param to the destination variant
   */
  void setMorphProgress(
      final float progress,
      final ElwhaNavigationRail.Variant from,
      final ElwhaNavigationRail.Variant to) {
    final float clamped = Math.max(0f, Math.min(1f, progress));
    this.morphFrom = from;
    this.morphTo = to;
    this.morphProgress = clamped;
    if (clamped >= 1f) {
      this.hostVariant = to;
      this.morphing = false;
    } else if (clamped <= 0f) {
      this.hostVariant = from;
      this.morphing = false;
    } else {
      this.morphing = true;
    }
    revalidate();
    repaint();
  }

  /**
   * Pushes the row content width the rail allocates to this destination in Expanded. Drives the
   * Hug-width indicator pill so the indicator spans the full row content area — only relevant when
   * the rail's selected destination is this one. The rail calls this once at layout time per
   * destination in Expanded; it has no effect on Collapsed paint.
   *
   * @param widthPx the row content width in pixels
   */
  void setRowContentWidth(final int widthPx) {
    if (this.rowContentWidthPx == widthPx) {
      return;
    }
    this.rowContentWidthPx = widthPx;
    repaint();
  }

  private int rowContentWidthPx;

  // ------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    if (currentLayoutIsExpanded()) {
      return new Dimension(expandedHugWidth(), EXPANDED_CONTENT_HEIGHT_PX);
    }
    return new Dimension(COLLAPSED_WIDTH_PX, COLLAPSED_CONTENT_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    if (currentLayoutIsExpanded()) {
      return new Dimension(Integer.MAX_VALUE, EXPANDED_CONTENT_HEIGHT_PX);
    }
    return getPreferredSize();
  }

  private boolean currentLayoutIsExpanded() {
    if (morphing) {
      return morphProgress >= LABEL_ANCHOR_SWITCH_PROGRESS;
    }
    return hostVariant == ElwhaNavigationRail.Variant.EXPANDED;
  }

  private int expandedHugWidth() {
    final FontMetrics fm = getFontMetrics(getFont());
    final int labelWidth = label.isEmpty() ? 0 : fm.stringWidth(label);
    return LEADING_PAD_EXPANDED
        + ICON_SIZE_PX
        + (label.isEmpty() ? 0 : ICON_LABEL_GAP_EXPANDED + labelWidth)
        + TRAILING_PAD_EXPANDED;
  }

  // ------------------------------------------------------------------ geometry

  @Override
  public Rectangle getIconBounds() {
    final IndicatorGeometry geom = currentIndicatorGeometry();
    return new Rectangle(geom.iconX, geom.iconY, ICON_SIZE_PX, ICON_SIZE_PX);
  }

  /** Returns the pill bounds; preserved for back-compat with internal callers. */
  Rectangle pillRect() {
    final IndicatorGeometry geom = currentIndicatorGeometry();
    return new Rectangle(geom.pillX, geom.pillY, geom.pillWidth, geom.pillHeight);
  }

  private record IndicatorGeometry(
      int pillX, int pillY, int pillWidth, int pillHeight, int iconX, int iconY) {}

  private IndicatorGeometry geometryForCollapsed() {
    final int pillX = (getWidth() - INDICATOR_WIDTH_COLLAPSED_PX) / 2;
    final int pillY = 0;
    final int pillW = INDICATOR_WIDTH_COLLAPSED_PX;
    final int pillH = INDICATOR_HEIGHT_COLLAPSED_PX;
    final int iconX = pillX + (pillW - ICON_SIZE_PX) / 2;
    final int iconY = pillY + (pillH - ICON_SIZE_PX) / 2;
    return new IndicatorGeometry(pillX, pillY, pillW, pillH, iconX, iconY);
  }

  private IndicatorGeometry geometryForExpanded() {
    final int rowWidth =
        rowContentWidthPx > 0 ? rowContentWidthPx : Math.max(getWidth(), expandedHugWidth());
    final int pillX = 0;
    final int pillY = 0;
    final int pillW = rowWidth;
    final int pillH = INDICATOR_HEIGHT_EXPANDED_PX;
    final int iconX = pillX + LEADING_PAD_EXPANDED;
    final int iconY = pillY + (pillH - ICON_SIZE_PX) / 2;
    return new IndicatorGeometry(pillX, pillY, pillW, pillH, iconX, iconY);
  }

  private IndicatorGeometry currentIndicatorGeometry() {
    if (!morphing) {
      return hostVariant == ElwhaNavigationRail.Variant.EXPANDED
          ? geometryForExpanded()
          : geometryForCollapsed();
    }
    final IndicatorGeometry from =
        morphFrom == ElwhaNavigationRail.Variant.EXPANDED
            ? geometryForExpanded()
            : geometryForCollapsed();
    final IndicatorGeometry to =
        morphTo == ElwhaNavigationRail.Variant.EXPANDED
            ? geometryForExpanded()
            : geometryForCollapsed();
    final float t = morphProgress;
    final int pillX = lerpInt(from.pillX, to.pillX, t);
    final int pillY = lerpInt(from.pillY, to.pillY, t);
    final int pillW = lerpInt(from.pillWidth, to.pillWidth, t);
    final int pillH = lerpInt(from.pillHeight, to.pillHeight, t);
    final int iconX = lerpInt(from.iconX, to.iconX, t);
    final int iconY = lerpInt(from.iconY, to.iconY, t);
    return new IndicatorGeometry(pillX, pillY, pillW, pillH, iconX, iconY);
  }

  private static int lerpInt(final int a, final int b, final float t) {
    return Math.round(a + (b - a) * t);
  }

  // -------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final IndicatorGeometry geom = currentIndicatorGeometry();
      paintActiveIndicator(g2, geom);
      paintStateLayer(g2, geom);
      paintRippleLayer(g2, geom);
      paintIcon(g2, geom);
      paintLabel(g2, geom);
      paintFocusRing(g2, geom);
    } finally {
      g2.dispose();
    }
  }

  private void paintActiveIndicator(final Graphics2D g2, final IndicatorGeometry geom) {
    final float t = selectionAnimator.progress();
    if (t <= 0f) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      s.setColor(ColorRole.SECONDARY_CONTAINER.resolve());
      s.fill(growFromCenter(geom, t));
    } finally {
      s.dispose();
    }
  }

  private RoundRectangle2D.Float growFromCenter(final IndicatorGeometry geom, final float t) {
    final float clamped = Math.max(0f, Math.min(1f, t));
    final float cx = geom.iconX + ICON_SIZE_PX / 2f;
    final float cy = geom.iconY + ICON_SIZE_PX / 2f;
    // Lerp each edge independently from the icon-center seed point to the static end geometry.
    // In Collapsed the icon center IS the pill center (symmetric grow); in Expanded the pill is
    // anchored at the row leading edge with the icon offset toward the leading side, so the
    // pill grows asymmetrically — emerging from the icon and expanding outward to fill the row.
    final float left = lerp(cx, geom.pillX, clamped);
    final float right = lerp(cx, geom.pillX + geom.pillWidth, clamped);
    final float top = lerp(cy, geom.pillY, clamped);
    final float bottom = lerp(cy, geom.pillY + geom.pillHeight, clamped);
    final float w = right - left;
    final float h = bottom - top;
    final float arc = h;
    return new RoundRectangle2D.Float(left, top, w, h, arc, arc);
  }

  private static float lerp(final float a, final float b, final float t) {
    return a + (b - a) * t;
  }

  private StateLayer activeOverlay() {
    if (!isEnabled()) {
      return null;
    }
    if (pressed) {
      return StateLayer.PRESSED;
    }
    if (hovered) {
      return StateLayer.HOVER;
    }
    if (isFocusOwner()) {
      return StateLayer.FOCUS;
    }
    return null;
  }

  private void paintStateLayer(final Graphics2D g2, final IndicatorGeometry geom) {
    final StateLayer overlay = activeOverlay();
    if (overlay == null) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      s.setComposite(AlphaComposite.SrcOver.derive(overlay.opacity()));
      s.setColor(stateLayerColor());
      s.fill(pillShape(geom));
    } finally {
      s.dispose();
    }
  }

  private void paintRippleLayer(final Graphics2D g2, final IndicatorGeometry geom) {
    if (rippleOrigin == null || rippleProgress >= 1f) {
      return;
    }
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(geom.pillX, geom.pillY);
      final Point localOrigin = new Point(rippleOrigin.x - geom.pillX, rippleOrigin.y - geom.pillY);
      RipplePainter.paint(
          r,
          geom.pillWidth,
          geom.pillHeight,
          localOrigin,
          rippleProgress,
          geom.pillHeight,
          stateLayerColor());
    } finally {
      r.dispose();
    }
  }

  private void paintIcon(final Graphics2D g2, final IndicatorGeometry geom) {
    final Icon icon = paintSelectedForm() ? iconSelected : iconUnselected;
    if (icon == null) {
      return;
    }
    icon.paintIcon(this, g2, geom.iconX, geom.iconY);
  }

  private boolean paintSelectedForm() {
    return selectionAnimator.progress() >= ICON_SWAP_PROGRESS;
  }

  private void paintLabel(final Graphics2D g2, final IndicatorGeometry geom) {
    if (label.isEmpty()) {
      return;
    }
    g2.setFont(getFont());
    final FontMetrics fm = g2.getFontMetrics();
    final int labelWidth = fm.stringWidth(label);

    if (morphing) {
      final float stackedAlpha = stackedLabelAlpha(morphProgress);
      final float inlineAlpha = inlineLabelAlpha(morphProgress);
      paintStackedLabel(g2, fm, labelWidth, stackedAlpha);
      paintInlineLabel(g2, fm, labelWidth, geom, inlineAlpha);
      return;
    }

    if (hostVariant == ElwhaNavigationRail.Variant.EXPANDED) {
      paintInlineLabel(g2, fm, labelWidth, geom, 1f);
    } else {
      paintStackedLabel(g2, fm, labelWidth, 1f);
    }
  }

  private void paintStackedLabel(
      final Graphics2D g2, final FontMetrics fm, final int labelWidth, final float alpha) {
    if (alpha <= 0f) {
      return;
    }
    final Graphics2D l = (Graphics2D) g2.create();
    try {
      l.setComposite(AlphaComposite.SrcOver.derive(alpha));
      l.setColor(labelColor());
      final int x = (getWidth() - labelWidth) / 2;
      final int y = INDICATOR_HEIGHT_COLLAPSED_PX + ICON_LABEL_GAP_COLLAPSED + fm.getAscent();
      l.drawString(label, x, y);
    } finally {
      l.dispose();
    }
  }

  private void paintInlineLabel(
      final Graphics2D g2,
      final FontMetrics fm,
      final int labelWidth,
      final IndicatorGeometry geom,
      final float alpha) {
    if (alpha <= 0f) {
      return;
    }
    final Graphics2D l = (Graphics2D) g2.create();
    try {
      l.setComposite(AlphaComposite.SrcOver.derive(alpha));
      l.setColor(labelColor());
      final int x = LEADING_PAD_EXPANDED + ICON_SIZE_PX + ICON_LABEL_GAP_EXPANDED;
      final int rowHeight = geom.pillHeight > 0 ? geom.pillHeight : EXPANDED_CONTENT_HEIGHT_PX;
      final int y = (rowHeight - fm.getHeight()) / 2 + fm.getAscent();
      l.drawString(label, x, y);
    } finally {
      l.dispose();
    }
  }

  private static float stackedLabelAlpha(final float progress) {
    return ContentMorphPainter.labelAlpha(1f - progress, LABEL_ANCHOR_SWITCH_PROGRESS);
  }

  private static float inlineLabelAlpha(final float progress) {
    return ContentMorphPainter.labelAlpha(progress, LABEL_ANCHOR_SWITCH_PROGRESS);
  }

  private void paintFocusRing(final Graphics2D g2, final IndicatorGeometry geom) {
    if (!isFocusOwner() || !isEnabled()) {
      return;
    }
    final Graphics2D f = (Graphics2D) g2.create();
    try {
      f.setStroke(new BasicStroke(FOCUS_RING_STROKE));
      f.setColor(ColorRole.PRIMARY.resolve());
      final float inset = FOCUS_RING_STROKE / 2f;
      final float arc = geom.pillHeight;
      f.draw(
          new RoundRectangle2D.Float(
              geom.pillX + inset,
              geom.pillY + inset,
              geom.pillWidth - 2f * inset,
              geom.pillHeight - 2f * inset,
              arc,
              arc));
    } finally {
      f.dispose();
    }
  }

  private RoundRectangle2D.Float pillShape(final IndicatorGeometry geom) {
    return new RoundRectangle2D.Float(
        geom.pillX, geom.pillY, geom.pillWidth, geom.pillHeight, geom.pillHeight, geom.pillHeight);
  }

  private Color iconColor() {
    return (paintSelectedForm() ? ColorRole.ON_SECONDARY_CONTAINER : ColorRole.ON_SURFACE_VARIANT)
        .resolve();
  }

  private Color labelColor() {
    return (paintSelectedForm() ? ColorRole.SECONDARY : ColorRole.ON_SURFACE_VARIANT).resolve();
  }

  private Color stateLayerColor() {
    return iconColor();
  }

  // ---------------------------------------------------------------- interaction

  private void initInteraction() {
    final MouseAdapter ma =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            if (!isEnabled()) {
              return;
            }
            hovered = true;
            repaint();
            ensureHoverPolling();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            if (isCursorStillInside(e)) {
              return;
            }
            hovered = false;
            pressed = false;
            stopHoverPolling();
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            pressed = true;
            // Mouse activation deliberately does NOT request focus — the focus ring is a
            // keyboard-only affordance for M3 tablist-style components. Keyboard nav still picks
            // the right destination on Tab-entry via the rail's FocusTraversalPolicy (focused →
            // selected → first).
            startRipple(e.getPoint());
            repaint();
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed || !isEnabled()) {
              pressed = false;
              repaint();
              return;
            }
            pressed = false;
            if (containsPoint(e.getPoint())) {
              activate(e.getModifiersEx());
            }
            repaint();
          }
        };
    addMouseListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            pressed = false;
            repaint();
          }
        });

    final InputMap im = getInputMap(WHEN_FOCUSED);
    final ActionMap am = getActionMap();
    final Action activate =
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (!isEnabled()) {
              return;
            }
            startRipple(pillCenter());
            activate(0);
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhaNavRailDestination.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhaNavRailDestination.activate");
    am.put("elwhaNavRailDestination.activate", activate);
  }

  private Point pillCenter() {
    final Rectangle p = pillRect();
    return new Point(p.x + p.width / 2, p.y + p.height / 2);
  }

  private void activate(final int modifiers) {
    final ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(evt);
    }
  }

  private boolean containsPoint(final Point p) {
    return p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight();
  }

  private boolean isCursorStillInside(final MouseEvent event) {
    if (!isShowing()) {
      return false;
    }
    final java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    final Point screenPt =
        info != null ? info.getLocation() : new Point(event.getXOnScreen(), event.getYOnScreen());
    final Point local = new Point(screenPt);
    javax.swing.SwingUtilities.convertPointFromScreen(local, this);
    return containsPoint(local);
  }

  private void ensureHoverPolling() {
    if (hoverPollTimer != null && hoverPollTimer.isRunning()) {
      return;
    }
    hoverPollTimer = new Timer(HOVER_POLL_INTERVAL_MS, e -> pollHoverState());
    hoverPollTimer.setRepeats(true);
    hoverPollTimer.start();
  }

  private void stopHoverPolling() {
    if (hoverPollTimer != null) {
      hoverPollTimer.stop();
    }
  }

  private void pollHoverState() {
    if (!hovered) {
      stopHoverPolling();
      return;
    }
    if (!isShowing()) {
      hovered = false;
      pressed = false;
      stopHoverPolling();
      return;
    }
    final java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    if (info == null) {
      return;
    }
    final Point local = new Point(info.getLocation());
    javax.swing.SwingUtilities.convertPointFromScreen(local, this);
    if (!containsPoint(local)) {
      hovered = false;
      pressed = false;
      stopHoverPolling();
      repaint();
    }
  }

  // --------------------------------------------------------------------- ripple

  private void startRipple(final Point origin) {
    rippleOrigin = origin;
    rippleProgress = 0f;
    if (rippleTimer != null && rippleTimer.isRunning()) {
      rippleTimer.stop();
    }
    final long startNanos = System.nanoTime();
    rippleTimer =
        new Timer(
            RIPPLE_TICK_MS,
            e -> {
              rippleProgress =
                  Math.min(1f, (System.nanoTime() - startNanos) / (RIPPLE_TOTAL_MS * 1_000_000f));
              repaint();
              if (rippleProgress >= 1f) {
                rippleTimer.stop();
              }
            });
    rippleTimer.setRepeats(true);
    rippleTimer.start();
    repaint();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    selectionAnimator.snapTo(selected ? 1f : 0f);
  }

  @Override
  public void removeNotify() {
    stopHoverPolling();
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    selectionAnimator.stop();
    super.removeNotify();
  }

  // ----------------------------------------------------------- accessibility

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaNavRailDestination();
    }
    return accessibleContext;
  }

  /**
   * The destination's accessible context. Reports {@link AccessibleRole#PAGE_TAB} (the standard
   * pairing with a {@code PAGE_TAB_LIST}-roled container — matches ARIA {@code tablist} / {@code
   * tab}), the destination's label as the accessible name, and {@link AccessibleState#SELECTED}
   * when {@link ElwhaNavRailDestination#isSelected()} is true. Badge content fragments are spliced
   * in by {@link com.owspfm.elwha.badge.ElwhaBadgeAnchor} via its push-model accessibility wiring —
   * see story #228.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  protected class AccessibleElwhaNavRailDestination extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.PAGE_TAB;
    }

    @Override
    public String getAccessibleName() {
      final String override = super.getAccessibleName();
      if (override != null && !override.isEmpty()) {
        return override;
      }
      return label;
    }

    @Override
    public AccessibleStateSet getAccessibleStateSet() {
      final AccessibleStateSet states = super.getAccessibleStateSet();
      if (selected) {
        states.add(AccessibleState.SELECTED);
      }
      return states;
    }
  }
}
