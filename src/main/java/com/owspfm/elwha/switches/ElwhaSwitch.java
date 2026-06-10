package com.owspfm.elwha.switches;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The Elwha Material 3 <strong>switch</strong> — a token-themed binary selection control painting
 * M3 chrome: a {@value #TRACK_WIDTH_PX}&times;{@value #TRACK_HEIGHT_PX} corner-full track (with a
 * {@value #TRACK_OUTLINE_PX} dp {@link ColorRole#OUTLINE} border while unselected), and a circular
 * handle riding the track that morphs {@value #HANDLE_UNSELECTED_PX}&rarr;{@value
 * #HANDLE_SELECTED_PX} dp across the selection states.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-switch-design.md} §2).</strong> {@code ElwhaSwitch} is one dedicated {@link JComponent}
 * that paints every M3 part itself, holding the selection as a plain boolean. It is <em>not</em> a
 * styled {@code JToggleButton} and <em>not</em> a {@code ButtonUI} delegate: nothing of a button's
 * text/icon layout survives the track-and-riding-handle anatomy, the handle drag gesture has no
 * {@code ButtonModel} vocabulary, and switches are never {@code ButtonGroup}-grouped — so the model
 * would buy nothing. The package is {@code switches} because {@code switch} is a Java reserved
 * word.
 *
 * <p><strong>Color (zero new tokens — research §Tokens).</strong> Selected: track {@link
 * ColorRole#PRIMARY}, handle {@link ColorRole#ON_PRIMARY}. Unselected: track {@link
 * ColorRole#SURFACE_CONTAINER_HIGHEST} with the {@link ColorRole#OUTLINE} border, handle {@link
 * ColorRole#OUTLINE}. Disabled: track at the M3 container opacity (0.12) over {@link
 * ColorRole#ON_SURFACE} (selected) / {@link ColorRole#SURFACE_CONTAINER_HIGHEST} (unselected, with
 * an {@link ColorRole#ON_SURFACE} ring), handle {@link ColorRole#ON_SURFACE} at the content opacity
 * (0.38) unselected but <strong>opaque {@link ColorRole#SURFACE}</strong> selected — the spec's
 * deliberate asymmetry (research §T ⚠). All roles resolve at paint time so runtime theme +
 * light/dark switching re-skins the switch live.
 *
 * <p><strong>Geometry (M3 token-locked — research §T).</strong> The preferred size is the
 * state-layer-inclusive box — the track block plus the {@value #STATE_LAYER_SIZE_PX} dp interaction
 * halo's {@value #HALO_OVERHANG_PX} dp overhang per side; the chrome centers itself in larger
 * bounds. The M3 48&nbsp;dp touch target is guidance for touch contexts, not painted geometry — the
 * component's whole bounds are interactive.
 *
 * <p><strong>Interaction (research §B / §S).</strong> Click anywhere to toggle, drag the handle
 * past the track midpoint to commit a state, or toggle from the keyboard with Space (press shows
 * the pressed treatment, release commits — Swing button semantics; Enter is deliberately unbound).
 * Hover paints {@link StateLayer#HOVER} (0.08) and focus {@link StateLayer#FOCUS} (0.10) on the
 * {@value #STATE_LAYER_SIZE_PX} dp circle riding the handle; a press grows the handle to {@value
 * #HANDLE_PRESSED_PX} dp, paints {@link StateLayer#PRESSED}, and shows a {@link RipplePainter}
 * ripple bounded to the same circle. User-gesture commits fire the registered {@link
 * ActionListener}s <em>and</em> {@link ChangeListener}s; programmatic {@code setSelected} fires
 * only the latter.
 *
 * <p><strong>Labelling.</strong> A switch never labels itself — M3 requires an adjacent label
 * naming what it toggles, and the accessible name must always be set: call {@code setLabel(String)}
 * or associate a {@link javax.swing.JLabel} via {@link javax.swing.JLabel#setLabelFor} (the a11y
 * story wires both).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaSwitch extends JComponent {

  // --- M3 switch geometry (dp == px at 100% scale; research §T — the md-comp-switch values) ---

  /** Track width. */
  static final int TRACK_WIDTH_PX = 52;

  /** Track height — also the capsule end diameter. */
  static final int TRACK_HEIGHT_PX = 32;

  /** Unselected track outline width, painted inside the track bounds. */
  static final int TRACK_OUTLINE_PX = 2;

  /** Handle diameter while unselected (no icon). */
  static final int HANDLE_UNSELECTED_PX = 16;

  /** Handle diameter while selected. */
  static final int HANDLE_SELECTED_PX = 24;

  /** Handle diameter while pressed or mid-drag — on either side of the toggle. */
  static final int HANDLE_PRESSED_PX = 28;

  /** Diameter of the hover/focus/press state layer riding the handle. */
  static final int STATE_LAYER_SIZE_PX = 40;

  /** The state layer's overhang past the track box, per side. */
  static final int HALO_OVERHANG_PX = (STATE_LAYER_SIZE_PX - TRACK_HEIGHT_PX) / 2;

  /** Pointer travel from the press point before a press becomes a drag (design §12). */
  static final int DRAG_THRESHOLD_PX = 4;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;

  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private boolean selected;
  private boolean hovered;
  private boolean pressed;
  private boolean dragging;
  private int pressX;
  private float dragFraction;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;

  /**
   * Creates an unselected switch.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSwitch() {
    this(false);
  }

  /**
   * Creates a switch in the given selection state.
   *
   * @param selected the initial selection state
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSwitch(final boolean selected) {
    this.selected = selected;
    setOpaque(false);
    setFocusable(true);
    initInteraction();
    initKeyboard();
  }

  // ------------------------------------------------------------------ selection

  /**
   * Returns whether the switch is selected (on).
   *
   * @return {@code true} if selected
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Sets the selection state. Fires the registered {@link ChangeListener}s only when the state
   * actually changes. Programmatic writes never fire the user-gesture {@code ActionListener}s (the
   * interaction story's surface) — mirroring material-web, whose {@code change} event fires on user
   * interaction only.
   *
   * @param selected the new selection state
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSelected(final boolean selected) {
    if (this.selected == selected) {
      return;
    }
    this.selected = selected;
    fireStateChanged();
    repaint();
  }

  /**
   * Adds a {@link ChangeListener} notified on every selection-state change, user-driven or
   * programmatic.
   *
   * @param listener the listener to add
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addChangeListener(final ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  /**
   * Removes a previously added {@link ChangeListener}.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  /** Notifies registered {@link ChangeListener}s of a selection-state change. */
  private void fireStateChanged() {
    final ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
    for (final ChangeListener listener : listeners) {
      listener.stateChanged(changeEvent);
    }
  }

  /**
   * Adds an {@link ActionListener} notified when the <em>user</em> toggles the switch — a click, a
   * Space release, or a drag commit that changed the state. Programmatic {@link
   * #setSelected(boolean)} writes never fire it (material-web parity: {@code change} fires on user
   * interaction only). The event's action command is {@code "selected"} or {@code "deselected"},
   * reflecting the state just committed.
   *
   * @param listener the listener to add
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addActionListener(final ActionListener listener) {
    listenerList.add(ActionListener.class, listener);
  }

  /**
   * Removes a previously added {@link ActionListener}.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeActionListener(final ActionListener listener) {
    listenerList.remove(ActionListener.class, listener);
  }

  /** Notifies registered {@link ActionListener}s of a user-gesture toggle. */
  private void fireActionPerformed() {
    final ActionEvent event =
        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected ? "selected" : "deselected");
    for (final ActionListener listener : listenerList.getListeners(ActionListener.class)) {
      listener.actionPerformed(event);
    }
  }

  /** Commits a user-gesture toggle: state change first, then the action event. */
  private void commitUserToggle(final boolean next) {
    final boolean changed = next != selected;
    setSelected(next);
    if (changed) {
      fireActionPerformed();
    }
  }

  // -------------------------------------------------------------- gallery hooks

  /**
   * Forces the hover treatment on or off — for static gallery rendering only (a parallel to the
   * slider's hook); real hover tracking is automatic.
   *
   * @param hovered whether to paint the hover state layer
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setHovered(final boolean hovered) {
    this.hovered = hovered;
    repaint();
  }

  /**
   * Forces the pressed treatment on or off — for static gallery rendering only (a parallel to the
   * slider's hook); real press tracking is automatic.
   *
   * @param pressed whether to paint the pressed treatment (grown handle + pressed layer)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setPressed(final boolean pressed) {
    this.pressed = pressed;
    repaint();
  }

  // -------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(
        TRACK_WIDTH_PX + 2 * HALO_OVERHANG_PX, TRACK_HEIGHT_PX + 2 * HALO_OVERHANG_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ------------------------------------------------------------------ geometry

  /** The x of the track block's left edge — centered in the component bounds. */
  private int trackX() {
    return (getWidth() - TRACK_WIDTH_PX) / 2;
  }

  /** The y of the track block's top edge — centered in the component bounds. */
  private int trackY() {
    return (getHeight() - TRACK_HEIGHT_PX) / 2;
  }

  /** The handle center's x at the unselected (start) end of its travel. */
  private int travelStartX() {
    return trackX() + TRACK_HEIGHT_PX / 2;
  }

  /** The handle center's x at the selected (end) end of its travel. */
  private int travelEndX() {
    return trackX() + TRACK_WIDTH_PX - TRACK_HEIGHT_PX / 2;
  }

  /** The handle center's x — the resting end for the state, or the scrub position mid-drag. */
  int handleCenterX() {
    final float fraction = dragging ? dragFraction : (selected ? 1f : 0f);
    return Math.round(travelStartX() + fraction * (travelEndX() - travelStartX()));
  }

  /** The handle center's y — the track's vertical center. */
  int handleCenterY() {
    return trackY() + TRACK_HEIGHT_PX / 2;
  }

  /** Maps a pointer x to a travel fraction {@code [0, 1]} along the handle run. */
  private float fractionForX(final int x) {
    final int start = travelStartX();
    final int end = travelEndX();
    if (end <= start) {
      return 0f;
    }
    return clampF((x - start) / (float) (end - start));
  }

  /** The handle diameter for the current state — pressed and drag grow to the press size. */
  private float handleDiameter() {
    if (pressed || dragging) {
      return HANDLE_PRESSED_PX;
    }
    return selected ? HANDLE_SELECTED_PX : HANDLE_UNSELECTED_PX;
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintTrack(g2);
      paintStateLayer(g2);
      paintRipple(g2);
      paintHandle(g2);
    } finally {
      g2.dispose();
    }
  }

  /** The {@value #STATE_LAYER_SIZE_PX} dp interaction circle concentric with the handle. */
  private Ellipse2D.Float stateLayerCircle() {
    final float r = STATE_LAYER_SIZE_PX / 2f;
    return new Ellipse2D.Float(
        handleCenterX() - r, handleCenterY() - r, STATE_LAYER_SIZE_PX, STATE_LAYER_SIZE_PX);
  }

  /** The state-layer tint role for the current selection state (research §T). */
  private Color stateLayerTint() {
    return (selected ? ColorRole.PRIMARY : ColorRole.ON_SURFACE).resolve();
  }

  /** Paints the static hover/focus/pressed layers on the circle riding the handle. */
  private void paintStateLayer(final Graphics2D g2) {
    if (!isEnabled()) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      final Ellipse2D.Float halo = stateLayerCircle();
      final Color tint = stateLayerTint();
      if (isFocusOwner()) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.FOCUS.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
      if (hovered) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.HOVER.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
      // Pressed feedback is primarily the ripple; a faint static layer keeps the pressed state
      // legible in still renders (gallery cells, reduced motion, between ripple frames).
      if (pressed || dragging) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.PRESSED.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
    } finally {
      s.dispose();
    }
  }

  /** Paints the press ripple, bounded to the state-layer circle (material-web's md-ripple). */
  private void paintRipple(final Graphics2D g2) {
    if (rippleOrigin == null || rippleProgress >= 1f || !isEnabled()) {
      return;
    }
    final Ellipse2D.Float halo = stateLayerCircle();
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(halo.x, halo.y);
      final Point local = new Point(rippleOrigin.x - (int) halo.x, rippleOrigin.y - (int) halo.y);
      RipplePainter.paint(
          r,
          STATE_LAYER_SIZE_PX,
          STATE_LAYER_SIZE_PX,
          local,
          rippleProgress,
          STATE_LAYER_SIZE_PX,
          stateLayerTint());
    } finally {
      r.dispose();
    }
  }

  /**
   * Paints the track capsule. The unselected outline is a filled ring ({@link Area} subtraction,
   * not a stroke) so the ring and the interior tile exactly — no half-pixel seam and, in the
   * disabled treatment, no double-blended overlap between the two translucent fills (design §12).
   */
  private void paintTrack(final Graphics2D g2) {
    final RoundRectangle2D.Float outer =
        new RoundRectangle2D.Float(
            trackX(), trackY(), TRACK_WIDTH_PX, TRACK_HEIGHT_PX, TRACK_HEIGHT_PX, TRACK_HEIGHT_PX);
    if (selected) {
      g2.setColor(selectedTrackColor());
      g2.fill(outer);
      return;
    }
    final int inset = TRACK_OUTLINE_PX;
    final RoundRectangle2D.Float inner =
        new RoundRectangle2D.Float(
            trackX() + inset,
            trackY() + inset,
            TRACK_WIDTH_PX - 2 * inset,
            TRACK_HEIGHT_PX - 2 * inset,
            TRACK_HEIGHT_PX - 2 * inset,
            TRACK_HEIGHT_PX - 2 * inset);
    final Area ring = new Area(outer);
    ring.subtract(new Area(inner));
    g2.setColor(unselectedTrackFillColor());
    g2.fill(inner);
    g2.setColor(unselectedTrackOutlineColor());
    g2.fill(ring);
  }

  /** Paints the circular handle at its resting position for the current state. */
  private void paintHandle(final Graphics2D g2) {
    final float diameter = handleDiameter();
    final float x = handleCenterX() - diameter / 2f;
    final float y = handleCenterY() - diameter / 2f;
    g2.setColor(handleColor());
    g2.fill(new Ellipse2D.Float(x, y, diameter, diameter));
  }

  // --------------------------------------------------------------------- color

  /** The selected track fill, honoring the disabled treatment (research §T). */
  private Color selectedTrackColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContainerOpacity());
    }
    return ColorRole.PRIMARY.resolve();
  }

  /** The unselected track interior fill, honoring the disabled treatment (research §T). */
  private Color unselectedTrackFillColor() {
    final Color base = ColorRole.SURFACE_CONTAINER_HIGHEST.resolve();
    if (!isEnabled()) {
      return withAlpha(base, StateLayer.disabledContainerOpacity());
    }
    return base;
  }

  /** The unselected track outline ring, honoring the disabled treatment (research §T). */
  private Color unselectedTrackOutlineColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContainerOpacity());
    }
    return ColorRole.OUTLINE.resolve();
  }

  /**
   * The handle fill, honoring the per-state roles and the disabled treatment — including the spec's
   * asymmetry: the disabled <em>selected</em> handle is opaque {@link ColorRole#SURFACE} (a solid
   * "hole" punched in the 12% track), while the disabled unselected handle is {@link
   * ColorRole#ON_SURFACE} at the 0.38 content opacity (research §T ⚠). Hover/focus/press shift the
   * enabled handle to {@link ColorRole#PRIMARY_CONTAINER} (selected) / {@link
   * ColorRole#ON_SURFACE_VARIANT} (unselected).
   */
  private Color handleColor() {
    if (!isEnabled()) {
      return selected
          ? ColorRole.SURFACE.resolve()
          : withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    final boolean active = hovered || pressed || dragging || isFocusOwner();
    if (selected) {
      return (active ? ColorRole.PRIMARY_CONTAINER : ColorRole.ON_PRIMARY).resolve();
    }
    return (active ? ColorRole.ON_SURFACE_VARIANT : ColorRole.OUTLINE).resolve();
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
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repaint();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            hovered = false;
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            requestFocusInWindow();
            pressed = true;
            pressX = e.getX();
            dragFraction = selected ? 1f : 0f;
            startRipple(new Point(handleCenterX(), handleCenterY()));
            repaint();
          }

          @Override
          public void mouseDragged(final MouseEvent e) {
            if (!isEnabled() || !pressed) {
              return;
            }
            if (!dragging && Math.abs(e.getX() - pressX) >= DRAG_THRESHOLD_PX) {
              dragging = true;
            }
            if (dragging) {
              dragFraction = fractionForX(e.getX());
              repaint();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed) {
              return;
            }
            final boolean wasDragging = dragging;
            final float fraction = dragFraction;
            pressed = false;
            dragging = false;
            if (!isEnabled()) {
              repaint();
              return;
            }
            if (wasDragging) {
              commitUserToggle(fraction >= 0.5f);
            } else if (contains(e.getPoint())) {
              commitUserToggle(!selected);
            }
            repaint();
          }
        };
    addMouseListener(ma);
    addMouseMotionListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            pressed = false;
            dragging = false;
            repaint();
          }
        });
  }

  private void initKeyboard() {
    getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "elwhaSwitch.press");
    getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "elwhaSwitch.release");
    getActionMap()
        .put(
            "elwhaSwitch.press",
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                if (!ElwhaSwitch.this.isEnabled() || pressed) {
                  return;
                }
                pressed = true;
                startRipple(new Point(handleCenterX(), handleCenterY()));
                repaint();
              }
            });
    getActionMap()
        .put(
            "elwhaSwitch.release",
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                if (!ElwhaSwitch.this.isEnabled() || !pressed) {
                  return;
                }
                pressed = false;
                commitUserToggle(!selected);
                repaint();
              }
            });
  }

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
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    super.removeNotify();
  }

  // -------------------------------------------------------------------- helpers

  private static Color withAlpha(final Color base, final float opacity) {
    final int a = Math.round(clampF(opacity) * 255f);
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
  }

  private static float clampF(final float v) {
    return Math.max(0f, Math.min(1f, v));
  }
}
