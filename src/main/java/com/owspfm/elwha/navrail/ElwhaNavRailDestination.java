package com.owspfm.elwha.navrail;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.badge.IconBearing;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
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
 * <p><strong>Phase 1 / Story #226 skeleton.</strong> This first revision lays out the Collapsed
 * form only — icon over label, full-row hit target, pill-shaped state layer and ripple aligned with
 * the 32×56 active-indicator region. No selected state and no badge slot at this stage; those land
 * in stories #227 and #228. The Expanded form, the Collapsed↔Expanded morph, and the parent
 * container come later in the epic.
 *
 * <p><strong>Geometry (M3 token-locked, see {@code elwha-navigation-rail-design.md} §4).</strong>
 * Component is {@value #COLLAPSED_WIDTH_PX} dp wide × {@value #COLLAPSED_CONTENT_HEIGHT_PX} dp
 * tall. The 32×56 indicator pill is centered horizontally at the top; the 24-dp icon glyph centers
 * inside it; the label sits 4 dp below, centered horizontally. The destination's hit target is the
 * full component bounds regardless of pill shape — the pill only governs paint.
 *
 * <p><strong>Paint contract.</strong>
 *
 * <ul>
 *   <li>Surface is transparent — the rail container paints the rail's own surface behind a row of
 *       destinations.
 *   <li>State-layer overlay paints at the {@link StateLayer#HOVER hover}, {@link StateLayer#FOCUS
 *       focus}, and {@link StateLayer#PRESSED pressed} opacities — clipped to the pill shape so the
 *       affordance reads as "the icon got hit", not "the whole row got hit". Selected adds a {@link
 *       ColorRole#SECONDARY_CONTAINER}-filled pill in story #227.
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
 * {@code ElwhaNavigationRail} once that container lands in Phase 2 — they are not part of the
 * destination's public surface today.
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
  static final int INDICATOR_WIDTH_PX = 56;
  static final int INDICATOR_HEIGHT_PX = 32;
  static final int ICON_SIZE_PX = 24;
  static final int ICON_LABEL_GAP_PX = 4;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;
  private static final float FOCUS_RING_STROKE = 2f;

  private final Icon iconUnselected;
  private final Icon iconSelected;
  private final String label;

  private boolean selected;
  private boolean hovered;
  private boolean pressed;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;
  private Timer hoverPollTimer;

  private final List<ActionListener> actionListeners = new ArrayList<>();

  private final FlatSVGIcon.ColorFilter iconFilter = new FlatSVGIcon.ColorFilter(c -> iconColor());

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
    getAccessibleContext().setAccessibleName(label);
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
   * <p>Paint switches instantaneously — no animation in Phase 1. The grow-from-center
   * active-indicator animation is a Phase 5 polish item.
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
    firePropertyChange(PROPERTY_SELECTED, previous, selected);
    repaint();
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

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(COLLAPSED_WIDTH_PX, COLLAPSED_CONTENT_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public Rectangle getIconBounds() {
    final Rectangle pill = pillRect();
    final int iconX = pill.x + (pill.width - ICON_SIZE_PX) / 2;
    final int iconY = pill.y + (pill.height - ICON_SIZE_PX) / 2;
    return new Rectangle(iconX, iconY, ICON_SIZE_PX, ICON_SIZE_PX);
  }

  Rectangle pillRect() {
    final int x = (getWidth() - INDICATOR_WIDTH_PX) / 2;
    return new Rectangle(x, 0, INDICATOR_WIDTH_PX, INDICATOR_HEIGHT_PX);
  }

  // -------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final Rectangle pill = pillRect();
      paintActiveIndicator(g2, pill);
      paintStateLayer(g2, pill);
      paintRippleLayer(g2, pill);
      paintIcon(g2, pill);
      paintLabel(g2, pill);
      paintFocusRing(g2, pill);
    } finally {
      g2.dispose();
    }
  }

  private void paintActiveIndicator(final Graphics2D g2, final Rectangle pill) {
    if (!selected) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      s.setColor(ColorRole.SECONDARY_CONTAINER.resolve());
      s.fill(pillShape(pill));
    } finally {
      s.dispose();
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

  private void paintStateLayer(final Graphics2D g2, final Rectangle pill) {
    final StateLayer overlay = activeOverlay();
    if (overlay == null) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      s.setComposite(AlphaComposite.SrcOver.derive(overlay.opacity()));
      s.setColor(stateLayerColor());
      s.fill(pillShape(pill));
    } finally {
      s.dispose();
    }
  }

  private void paintRippleLayer(final Graphics2D g2, final Rectangle pill) {
    if (rippleOrigin == null || rippleProgress >= 1f) {
      return;
    }
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(pill.x, pill.y);
      final Point localOrigin = new Point(rippleOrigin.x - pill.x, rippleOrigin.y - pill.y);
      RipplePainter.paint(
          r, pill.width, pill.height, localOrigin, rippleProgress, pill.height, stateLayerColor());
    } finally {
      r.dispose();
    }
  }

  private void paintIcon(final Graphics2D g2, final Rectangle pill) {
    final Icon icon = selected ? iconSelected : iconUnselected;
    if (icon == null) {
      return;
    }
    final int x = pill.x + (pill.width - icon.getIconWidth()) / 2;
    final int y = pill.y + (pill.height - icon.getIconHeight()) / 2;
    icon.paintIcon(this, g2, x, y);
  }

  private void paintLabel(final Graphics2D g2, final Rectangle pill) {
    if (label.isEmpty()) {
      return;
    }
    g2.setFont(getFont());
    g2.setColor(labelColor());
    final FontMetrics fm = g2.getFontMetrics();
    final int labelWidth = fm.stringWidth(label);
    final int x = (getWidth() - labelWidth) / 2;
    final int y = pill.y + pill.height + ICON_LABEL_GAP_PX + fm.getAscent();
    g2.drawString(label, x, y);
  }

  private void paintFocusRing(final Graphics2D g2, final Rectangle pill) {
    if (!isFocusOwner() || !isEnabled()) {
      return;
    }
    final Graphics2D f = (Graphics2D) g2.create();
    try {
      f.setStroke(new BasicStroke(FOCUS_RING_STROKE));
      f.setColor(ColorRole.PRIMARY.resolve());
      final float inset = FOCUS_RING_STROKE / 2f;
      final float arc = pill.height;
      f.draw(
          new RoundRectangle2D.Float(
              pill.x + inset,
              pill.y + inset,
              pill.width - 2f * inset,
              pill.height - 2f * inset,
              arc,
              arc));
    } finally {
      f.dispose();
    }
  }

  private RoundRectangle2D.Float pillShape(final Rectangle pill) {
    return new RoundRectangle2D.Float(
        pill.x, pill.y, pill.width, pill.height, pill.height, pill.height);
  }

  private Color iconColor() {
    return (selected ? ColorRole.ON_SECONDARY_CONTAINER : ColorRole.ON_SURFACE_VARIANT).resolve();
  }

  private Color labelColor() {
    return (selected ? ColorRole.SECONDARY : ColorRole.ON_SURFACE_VARIANT).resolve();
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
            if (isRequestFocusEnabled()) {
              requestFocusInWindow();
            }
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
  public void removeNotify() {
    stopHoverPolling();
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    super.removeNotify();
  }
}
