package com.owspfm.elwha.tabs;

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
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * One tab of an M3 tab bar — the dedicated tab primitive hosted by {@link ElwhaTabs}. Paints its
 * own content (label; icon forms arrive with the epic's S4 story), state layers, and press ripple
 * in the M3 {@code title-small} type role with the variant's active/inactive content colors; the
 * bar paints everything that spans tabs (container fill, divider, the animated active indicator).
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
 * <p><strong>Geometry (M3 token-locked, design §5):</strong> {@value #H_PADDING_PX}&nbsp;px
 * horizontal padding, {@value #INLINE_CONTENT_HEIGHT_PX}&nbsp;px inline content height, label in
 * {@link TypeRole#TITLE_SMALL}, single line, no wrap. Colors resolve at paint time (the binding
 * rule) — see {@code docs/research/elwha-tabs-design.md} §4.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTab extends JComponent {

  static final int H_PADDING_PX = 16;
  static final int INLINE_CONTENT_HEIGHT_PX = 48;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;

  private final String label;
  private final List<ActionListener> actionListeners = new ArrayList<>();

  private TabsVariant variant = TabsVariant.PRIMARY;
  private boolean active;
  private boolean hovered;
  private boolean pressed;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;
  private Timer hoverPollTimer;

  private ElwhaTab(final String label) {
    this.label = Objects.requireNonNull(label, "label");
    setOpaque(false);
    initInteraction();
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
    return new ElwhaTab(label);
  }

  /**
   * The tab's label text.
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

  void fireAction(final int modifiers) {
    final ActionEvent event =
        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(event);
    }
  }

  // ------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    final FontMetrics fm = getFontMetrics(labelFont());
    return new Dimension(
        H_PADDING_PX + fm.stringWidth(label) + H_PADDING_PX, INLINE_CONTENT_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ----------------------------------------------------------------- geometry

  // The horizontal span of the content cluster (the label for now), in tab coordinates — the
  // PRIMARY variant's content-hugging indicator width (material-web spans the `.content` box).
  Rectangle contentSpan() {
    final FontMetrics fm = getFontMetrics(labelFont());
    final int w = fm.stringWidth(label);
    return new Rectangle((getWidth() - w) / 2, 0, w, getHeight());
  }

  // -------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      paintStateLayer(g2);
      paintRippleLayer(g2);
      paintLabel(g2);
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

  private void paintLabel(final Graphics2D g2) {
    if (label.isEmpty()) {
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
      final int x = (getWidth() - fm.stringWidth(label)) / 2;
      final int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
      l.drawString(label, x, y);
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
