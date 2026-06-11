package com.owspfm.elwha.tabs;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.badge.IconBearing;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * One tab of an M3 tab bar — the dedicated tab primitive hosted by {@link ElwhaTabs}. Paints its
 * own content (icon and/or label), state layers, and press ripple in the M3 {@code title-small}
 * type role with the variant's active/inactive content colors; the bar paints everything that
 * spans tabs (container fill, divider, the animated active indicator).
 *
 * <p><strong>Content forms (design §3, research §A).</strong> Label-only · icon+label · icon-only:
 *
 * <ul>
 *   <li><strong>Primary stacked</strong> (the primary-variant default with icon+label): 24&nbsp;px
 *       icon over the label, {@value #STACKED_GAP_PX}&nbsp;px gap, {@value
 *       #STACKED_CONTENT_HEIGHT_PX}&nbsp;px content height. {@link #setInlineIcon(boolean)}
 *       switches a primary tab to the inline row.
 *   <li><strong>Inline</strong> (secondary tabs always; primary with {@code inlineIcon}): icon
 *       beside label, {@value #INLINE_GAP_PX}&nbsp;px gap, {@value
 *       #INLINE_CONTENT_HEIGHT_PX}&nbsp;px content height.
 *   <li><strong>Icon-only:</strong> the icon centered; pass an accessible label — icon-only tabs
 *       always need one (research §A; surfaces as the accessible name with S6).
 * </ul>
 *
 * <p>The {@link MaterialIcons.Symbol} factories follow the house fill-swap: the fill-0 glyph
 * paints inactive, the fill-1 form paints active. Raw-{@link Icon} factories paint the one icon in
 * both states; {@link FlatSVGIcon}s are auto-tinted to the content color.
 *
 * <p><strong>Badges (anatomy item 2).</strong> {@link #setBadge(ElwhaBadge)} anchors via {@link
 * ElwhaBadgeAnchor} — {@code ICON_CORNER} on the icon when one exists, {@code TRAILING_EDGE} for
 * label-only tabs; this tab implements {@link IconBearing} for the anchor's geometry feed.
 *
 * <p><strong>Activation flows through the bar.</strong> {@link #isActive()} is read-only here;
 * {@link ElwhaTabs#setActiveTabIndex(int)} (or a user gesture on the tab) is the way a tab becomes
 * active — M3's noun is <em>active</em> (material-web deprecates {@code selected} for tabs). A tab
 * is meaningless outside a bar: the bar stamps the {@link TabsVariant} and enabled state on add.
 *
 * <p><strong>State layers (design §4, research §T).</strong> Hover / focus / pressed paint over
 * the full tab rect at the {@link StateLayer} opacities. The tint is the active content color
 * family ({@code PRIMARY} for primary tabs, {@code ON_SURFACE} for secondary) when active, {@code
 * ON_SURFACE} when inactive — except the primary variant's inactive <em>pressed</em> layer, which
 * is {@code PRIMARY} (the press flashes the destination color; verbatim token sheet). Hovering,
 * pressing, or focusing an inactive tab also lifts its content color to {@code ON_SURFACE}. The
 * press ripple seeds at the press point, clipped to the tab rect.
 *
 * <p><strong>Listener semantics.</strong> {@link #addActionListener(ActionListener)} fires on
 * <em>user</em> activation of this tab only (click; keyboard arrives with S6) — never on
 * programmatic activation, and never when the tab was already active (clicking the active tab is a
 * no-op). Any-change observation lives on the bar's {@link
 * ElwhaTabs#addChangeListener(javax.swing.event.ChangeListener)}.
 *
 * <p>Colors resolve at paint time (the binding rule) — see {@code
 * docs/research/elwha-tabs-design.md} §4–§5.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTab extends JComponent implements IconBearing {

  static final int H_PADDING_PX = 16;
  static final int INLINE_CONTENT_HEIGHT_PX = 48;
  static final int STACKED_CONTENT_HEIGHT_PX = 64;
  static final int ICON_SIZE_PX = 24;
  static final int INLINE_GAP_PX = 8;
  static final int STACKED_GAP_PX = 2;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;

  private final String label;
  private final Icon iconInactive;
  private final Icon iconActive;
  private final String accessibleLabel;
  private final List<ActionListener> actionListeners = new ArrayList<>();

  private final FlatSVGIcon.ColorFilter iconFilter =
      new FlatSVGIcon.ColorFilter(c -> contentColor());

  private TabsVariant variant = TabsVariant.PRIMARY;
  private boolean active;
  private boolean hovered;
  private boolean pressed;
  private boolean inlineIcon;

  private ElwhaBadge badge;
  private ElwhaBadgeAnchor.Attachment badgeAttachment;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;
  private Timer hoverPollTimer;

  private ElwhaTab(
      final String label,
      final Icon iconInactive,
      final Icon iconActive,
      final String accessibleLabel) {
    this.label = Objects.requireNonNull(label, "label");
    this.iconInactive = iconInactive;
    this.iconActive = iconActive;
    this.accessibleLabel = accessibleLabel;
    applyIconColorFilter(iconInactive);
    applyIconColorFilter(iconActive);
    setOpaque(false);
    initInteraction();
  }

  private void applyIconColorFilter(final Icon icon) {
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
  }

  /**
   * Constructs a label-only tab.
   *
   * @param label the tab label; required
   * @return a new tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTab of(final String label) {
    return new ElwhaTab(label, null, null, null);
  }

  /**
   * Constructs an icon+label tab from a {@link MaterialIcons.Symbol} — the primary path. The
   * symbol's fill-0 glyph paints inactive; its fill-1 (or the unfilled fallback) paints active.
   *
   * @param icon the Material symbol handle; required
   * @param label the tab label; required
   * @return a new tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTab of(final MaterialIcons.Symbol icon, final String label) {
    Objects.requireNonNull(icon, "icon");
    return new ElwhaTab(label, icon.unselected(ICON_SIZE_PX), icon.selected(ICON_SIZE_PX), null);
  }

  /**
   * Constructs an icon+label tab from an arbitrary {@link Icon} — the escape hatch for custom
   * (non-Material) glyphs. The one icon paints in both states; {@link FlatSVGIcon}s are
   * auto-tinted to the content color.
   *
   * @param icon the icon; required
   * @param label the tab label; required
   * @return a new tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTab of(final Icon icon, final String label) {
    Objects.requireNonNull(icon, "icon");
    return new ElwhaTab(label, icon, icon, null);
  }

  /**
   * Constructs an icon-only tab. Icon-only tabs always need an accessible label (research §A) —
   * it becomes the tab's accessible name.
   *
   * @param icon the Material symbol handle; required
   * @param accessibleLabel the screen-reader name; required, never empty
   * @return a new tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTab iconOnly(final MaterialIcons.Symbol icon, final String accessibleLabel) {
    Objects.requireNonNull(icon, "icon");
    requireText(accessibleLabel);
    return new ElwhaTab(
        "", icon.unselected(ICON_SIZE_PX), icon.selected(ICON_SIZE_PX), accessibleLabel);
  }

  /**
   * Constructs an icon-only tab from an arbitrary {@link Icon}.
   *
   * @param icon the icon; required
   * @param accessibleLabel the screen-reader name; required, never empty
   * @return a new tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTab iconOnly(final Icon icon, final String accessibleLabel) {
    Objects.requireNonNull(icon, "icon");
    requireText(accessibleLabel);
    return new ElwhaTab("", icon, icon, accessibleLabel);
  }

  private static void requireText(final String accessibleLabel) {
    Objects.requireNonNull(accessibleLabel, "accessibleLabel");
    if (accessibleLabel.isBlank()) {
      throw new IllegalArgumentException("icon-only tabs need a non-blank accessibleLabel");
    }
  }

  /**
   * The tab's label text — empty for icon-only tabs.
   *
   * @return the label, never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getLabel() {
    return label;
  }

  /**
   * Reports whether this tab is the bar's active tab. The bar is the single source of truth — this
   * getter reflects the most-recent activation push from the parent {@link ElwhaTabs}.
   *
   * @return {@code true} if this tab paints in its active form
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Switches a primary tab with icon+label between the stacked default and the inline row (the
   * material-web {@code inline-icon} attribute). No effect on secondary tabs (always inline),
   * icon-only, or label-only tabs.
   *
   * @param inlineIcon {@code true} to lay the icon inline beside the label
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setInlineIcon(final boolean inlineIcon) {
    if (this.inlineIcon == inlineIcon) {
      return;
    }
    this.inlineIcon = inlineIcon;
    revalidate();
    repaint();
  }

  /**
   * Whether a primary icon+label tab lays out inline rather than stacked.
   *
   * @return the inline-icon flag
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isInlineIcon() {
    return inlineIcon;
  }

  /**
   * Anchors a badge to this tab (M3 tab anatomy item 2). Passing {@code null} detaches any current
   * badge; setting a new badge cleanly detaches the prior one first. Placement: {@link
   * ElwhaBadgeAnchor.AnchorMode#ICON_CORNER} riding the icon's upper-trailing corner when this tab
   * has an icon; {@link ElwhaBadgeAnchor.AnchorMode#TRAILING_EDGE} after the label for label-only
   * tabs. Badge accessibility content splices into this tab's accessible name via the anchor's
   * push-model.
   *
   * @param badge the badge to anchor, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
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
      final ElwhaBadgeAnchor.AnchorMode mode =
          hasIcon()
              ? ElwhaBadgeAnchor.AnchorMode.ICON_CORNER
              : ElwhaBadgeAnchor.AnchorMode.TRAILING_EDGE;
      badgeAttachment = ElwhaBadgeAnchor.attach(this, badge, mode);
    }
  }

  /**
   * Returns the currently attached badge, or {@code null} if none.
   *
   * @return the attached badge, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaBadge getBadge() {
    return badge;
  }

  /**
   * Adds an action listener fired when the <em>user</em> activates this tab (click; keyboard with
   * S6). Programmatic activation and clicks on the already-active tab never fire.
   *
   * @param listener the listener to add; null is ignored
   * @version v0.4.0
   * @since v0.4.0
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
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  /**
   * Gallery/static-render hook: forces the hover treatment on or off without real pointer input.
   * Real hover tracking sets the same state.
   *
   * @param hovered whether the hover treatment paints
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setHovered(final boolean hovered) {
    if (this.hovered == hovered) {
      return;
    }
    this.hovered = hovered;
    repaint();
  }

  /**
   * Gallery/static-render hook: forces the pressed treatment on or off without real pointer input
   * (no ripple — the static layer only). Real presses set the same state and seed the ripple.
   *
   * @param pressed whether the pressed treatment paints
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setPressed(final boolean pressed) {
    if (this.pressed == pressed) {
      return;
    }
    this.pressed = pressed;
    repaint();
  }

  // Push-only from the parent bar; consumers activate through ElwhaTabs.
  void setActive(final boolean active) {
    if (this.active == active) {
      return;
    }
    this.active = active;
    repaint();
  }

  void setVariant(final TabsVariant variant) {
    if (this.variant == variant) {
      return;
    }
    this.variant = variant;
    revalidate();
    repaint();
  }

  TabsVariant getVariant() {
    return variant;
  }

  String getAccessibleLabel() {
    return accessibleLabel;
  }

  void fireAction(final int modifiers) {
    final ActionEvent event =
        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(event);
    }
  }

  private boolean hasIcon() {
    return iconInactive != null;
  }

  private boolean hasLabel() {
    return !label.isEmpty();
  }

  // The primary-variant default with icon+label is the stacked column; secondary tabs are always
  // inline (research §Open-1).
  boolean isStacked() {
    return variant == TabsVariant.PRIMARY && hasIcon() && hasLabel() && !inlineIcon;
  }

  // ------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    final FontMetrics fm = getFontMetrics(labelFont());
    final int labelWidth = hasLabel() ? fm.stringWidth(label) : 0;
    if (isStacked()) {
      return new Dimension(
          2 * H_PADDING_PX + Math.max(ICON_SIZE_PX, labelWidth), STACKED_CONTENT_HEIGHT_PX);
    }
    int content = 0;
    if (hasIcon()) {
      content += ICON_SIZE_PX;
    }
    if (hasIcon() && hasLabel()) {
      content += INLINE_GAP_PX;
    }
    content += labelWidth;
    return new Dimension(2 * H_PADDING_PX + content, INLINE_CONTENT_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ----------------------------------------------------------------- geometry

  private record ContentGeometry(Rectangle icon, Rectangle text, Rectangle span) {}

  private ContentGeometry contentGeometry() {
    final FontMetrics fm = getFontMetrics(labelFont());
    final int labelWidth = hasLabel() ? fm.stringWidth(label) : 0;
    final int width = getWidth();
    final int height = getHeight();

    if (isStacked()) {
      final int clusterWidth = Math.max(hasIcon() ? ICON_SIZE_PX : 0, labelWidth);
      final int blockHeight = ICON_SIZE_PX + STACKED_GAP_PX + fm.getHeight();
      final int blockY = (height - blockHeight) / 2;
      final Rectangle icon =
          new Rectangle((width - ICON_SIZE_PX) / 2, blockY, ICON_SIZE_PX, ICON_SIZE_PX);
      final Rectangle text =
          new Rectangle(
              (width - labelWidth) / 2,
              blockY + ICON_SIZE_PX + STACKED_GAP_PX,
              labelWidth,
              fm.getHeight());
      return new ContentGeometry(
          icon, text, new Rectangle((width - clusterWidth) / 2, 0, clusterWidth, height));
    }

    int clusterWidth = labelWidth;
    if (hasIcon()) {
      clusterWidth += ICON_SIZE_PX + (hasLabel() ? INLINE_GAP_PX : 0);
    }
    final int clusterX = (width - clusterWidth) / 2;
    Rectangle icon = null;
    int textX = clusterX;
    if (hasIcon()) {
      icon = new Rectangle(clusterX, (height - ICON_SIZE_PX) / 2, ICON_SIZE_PX, ICON_SIZE_PX);
      textX = clusterX + ICON_SIZE_PX + (hasLabel() ? INLINE_GAP_PX : 0);
    }
    final Rectangle text =
        hasLabel()
            ? new Rectangle(textX, (height - fm.getHeight()) / 2, labelWidth, fm.getHeight())
            : null;
    return new ContentGeometry(icon, text, new Rectangle(clusterX, 0, clusterWidth, height));
  }

  // The horizontal span of the content cluster, in tab coordinates — the PRIMARY variant's
  // content-hugging indicator width (material-web spans the `.content` box).
  Rectangle contentSpan() {
    return contentGeometry().span;
  }

  /**
   * The icon glyph's bounds in this tab's coordinate space — the {@link IconBearing} feed for
   * {@link ElwhaBadgeAnchor}. Falls back to the label cluster for label-only tabs (whose badges
   * anchor trailing-edge and do not read this).
   *
   * @return the icon (or content-cluster) bounds
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public Rectangle getIconBounds() {
    final ContentGeometry geom = contentGeometry();
    if (geom.icon != null) {
      return geom.icon;
    }
    if (geom.text != null) {
      return geom.text;
    }
    return new Rectangle(0, 0, getWidth(), getHeight());
  }

  // -------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      final ContentGeometry geom = contentGeometry();
      paintStateLayer(g2);
      paintRippleLayer(g2);
      paintIcon(g2, geom);
      paintLabel(g2, geom);
    } finally {
      g2.dispose();
    }
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

  private void paintStateLayer(final Graphics2D g2) {
    final StateLayer overlay = activeOverlay();
    if (overlay == null) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      s.setComposite(AlphaComposite.SrcOver.derive(overlay.opacity()));
      s.setColor(stateLayerTint(overlay));
      s.fillRect(0, 0, getWidth(), getHeight());
    } finally {
      s.dispose();
    }
  }

  private void paintRippleLayer(final Graphics2D g2) {
    if (rippleOrigin == null || rippleProgress >= 1f) {
      return;
    }
    RipplePainter.paint(
        g2,
        getWidth(),
        getHeight(),
        rippleOrigin,
        rippleProgress,
        0,
        stateLayerTint(StateLayer.PRESSED));
  }

  private void paintIcon(final Graphics2D g2, final ContentGeometry geom) {
    if (!hasIcon() || geom.icon == null) {
      return;
    }
    final Icon icon = active ? iconActive : iconInactive;
    final Graphics2D i = (Graphics2D) g2.create();
    try {
      if (!isEnabled()) {
        i.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      }
      icon.paintIcon(this, i, geom.icon.x, geom.icon.y);
    } finally {
      i.dispose();
    }
  }

  private void paintLabel(final Graphics2D g2, final ContentGeometry geom) {
    if (!hasLabel() || geom.text == null) {
      return;
    }
    final Graphics2D l = (Graphics2D) g2.create();
    try {
      if (!isEnabled()) {
        l.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      }
      l.setFont(labelFont());
      final FontMetrics fm = l.getFontMetrics();
      l.setColor(contentColor());
      l.drawString(label, geom.text.x, geom.text.y + fm.getAscent());
    } finally {
      l.dispose();
    }
  }

  private Font labelFont() {
    return TypeRole.TITLE_SMALL.resolve();
  }

  // Active content: PRIMARY in primary tabs, ON_SURFACE in secondary tabs. Inactive content:
  // ON_SURFACE_VARIANT, lifted to ON_SURFACE under hover / focus / press (research §T).
  private Color contentColor() {
    if (active) {
      return activeContentRole().resolve();
    }
    if (isEnabled() && (hovered || pressed || isFocusOwner())) {
      return ColorRole.ON_SURFACE.resolve();
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
  }

  private ColorRole activeContentRole() {
    return variant == TabsVariant.PRIMARY ? ColorRole.PRIMARY : ColorRole.ON_SURFACE;
  }

  // Layer tint: the active content family when active; ON_SURFACE when inactive — except the
  // primary variant's inactive pressed layer, which the token sheet makes PRIMARY (research §T).
  private Color stateLayerTint(final StateLayer overlay) {
    if (active) {
      return activeContentRole().resolve();
    }
    if (overlay == StateLayer.PRESSED && variant == TabsVariant.PRIMARY) {
      return ColorRole.PRIMARY.resolve();
    }
    return ColorRole.ON_SURFACE.resolve();
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
            if (containsPoint(e.getPoint()) && getParent() instanceof ElwhaTabs bar) {
              bar.userActivate(ElwhaTab.this, e.getModifiersEx());
            }
            repaint();
          }
        };
    addMouseListener(ma);
  }

  void startRipple(final Point origin) {
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

  private boolean containsPoint(final Point p) {
    return p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight();
  }

  private boolean isCursorStillInside(final MouseEvent event) {
    if (!isShowing()) {
      return false;
    }
    final PointerInfo info = MouseInfo.getPointerInfo();
    final Point screenPt =
        info != null ? info.getLocation() : new Point(event.getXOnScreen(), event.getYOnScreen());
    final Point local = new Point(screenPt);
    SwingUtilities.convertPointFromScreen(local, this);
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
    final PointerInfo info = MouseInfo.getPointerInfo();
    if (info == null) {
      return;
    }
    final Point local = new Point(info.getLocation());
    SwingUtilities.convertPointFromScreen(local, this);
    if (!containsPoint(local)) {
      hovered = false;
      pressed = false;
      stopHoverPolling();
      repaint();
    }
  }

  @Override
  public void removeNotify() {
    stopHoverPolling();
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    super.removeNotify();
  }
}
