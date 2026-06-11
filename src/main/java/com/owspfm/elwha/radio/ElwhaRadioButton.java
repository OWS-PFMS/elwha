package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
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
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The Elwha Material 3 <strong>radio button</strong> — a token-themed single-selection control
 * painting M3's 20&nbsp;dp ring-and-dot icon: a {@value #RING_WIDTH_PX}&nbsp;dp ring (outer
 * &Oslash;{@value #ICON_SIZE_PX}) that fills with a &Oslash;{@value #DOT_SIZE_PX} inner dot while
 * selected.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-radiobutton-design.md} §2).</strong> {@code ElwhaRadioButton} is one dedicated {@link
 * JComponent} that paints every M3 part itself, holding the selection as a plain boolean. It is
 * <em>not</em> a styled {@code JRadioButton} and <em>not</em> a {@code ButtonUI} delegate: the
 * button UI's text/icon layout fights the icon-only anatomy, and {@code ButtonGroup} has no
 * vocabulary for the M3 group keyboard contract (selection-follows-focus arrows, roving tab stop)
 * that {@link ElwhaRadioGroup} carries.
 *
 * <p><strong>Color (zero new tokens — research §Tokens).</strong> Unselected: ring {@link
 * ColorRole#ON_SURFACE_VARIANT}, shifting to {@link ColorRole#ON_SURFACE} under hover, focus, or
 * press. Selected: ring and dot {@link ColorRole#PRIMARY} in every interactive state. Disabled:
 * ring and dot {@link ColorRole#ON_SURFACE} at the M3 content opacity (0.38) — symmetric across
 * both sides, unlike the switch. All roles resolve at paint time so runtime theme + light/dark
 * switching re-skins the radio live.
 *
 * <p><strong>Interaction (research §B / §S).</strong> A click or Space <em>selects</em> — a user
 * gesture never deselects a radio ("clicking on a radio input always selects it"); deselection
 * happens programmatically or through an {@link ElwhaRadioGroup} sibling. Hover paints {@link
 * StateLayer#HOVER} (0.08) and focus {@link StateLayer#FOCUS} (0.10) on the {@value
 * #STATE_LAYER_SIZE_PX}&nbsp;dp circle in the state's tint ({@link ColorRole#ON_SURFACE}
 * unselected, {@link ColorRole#PRIMARY} selected). A press paints {@link StateLayer#PRESSED} plus a
 * {@link RipplePainter} ripple bounded to the same circle — in the <strong>swapped</strong> tint
 * (research §C′): pressing an unselected radio shows {@link ColorRole#PRIMARY} (the press
 * anticipates selection), pressing a selected one shows {@link ColorRole#ON_SURFACE}. User-gesture
 * selection commits fire the registered {@link ActionListener}s <em>and</em> {@link
 * ChangeListener}s; programmatic {@code setSelected} fires only the latter.
 *
 * <p><strong>Motion (design §6 / research §Mo — M3 verbatim).</strong> Selecting grows the dot
 * {@code 0→1} over {@value #DOT_GROW_MS}&nbsp;ms on {@link Easing#EMPHASIZED_DECELERATE}
 * (material-web's {@code inner-circle-grow}) while the dot's alpha and the ring color arrive in
 * {@value #COLOR_FADE_MS}&nbsp;ms linear; deselecting <em>fades</em> the dot at its current scale —
 * M3 specs no shrink — and crossfades the ring back, both over {@value #COLOR_FADE_MS}&nbsp;ms. A
 * mid-flight direction change continues from the current value (no jump). Programmatic {@code
 * setSelected} animates only while the radio is displayable and enabled — otherwise it snaps — and
 * {@link MorphAnimator#isReducedMotion() reduced motion} snaps everything globally.
 *
 * <p><strong>Geometry (M3 token-locked — research §T/§G).</strong> The ring is painted as a filled
 * ring ({@link Area} subtraction — material-web mask-builds it the same way), never a stroke, so
 * the disabled translucent fills cannot double-blend and no half-pixel seam appears. The preferred
 * size is the state-layer-inclusive box — the {@value #ICON_SIZE_PX}&nbsp;dp icon plus the {@value
 * #STATE_LAYER_SIZE_PX}&nbsp;dp interaction halo's {@value #HALO_OVERHANG_PX}&nbsp;dp overhang per
 * side; the chrome centers itself in larger bounds. The M3 48&nbsp;dp touch target is guidance for
 * touch contexts, not painted geometry — the component's whole bounds are interactive.
 *
 * <p><strong>Labelling.</strong> A radio button never labels itself — M3 requires an adjacent label
 * and the accessible name must always be set (research §A); pair with a {@link javax.swing.JLabel}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaRadioButton extends JComponent {

  // --- M3 radio-button geometry (dp == px at 100% scale; research §T/§G) ---

  /** Icon size — the ring's outer diameter. */
  static final int ICON_SIZE_PX = 20;

  /** Ring width — material-web's r=10 circle masked by an r=8 hole. */
  static final int RING_WIDTH_PX = 2;

  /** Inner dot diameter while selected. */
  static final int DOT_SIZE_PX = 10;

  /** Diameter of the hover/focus/press state layer concentric with the icon. */
  static final int STATE_LAYER_SIZE_PX = 40;

  /** The state layer's overhang past the icon, per side. */
  static final int HALO_OVERHANG_PX = (STATE_LAYER_SIZE_PX - ICON_SIZE_PX) / 2;

  /** Dot-grow duration on select — {@code motion.duration.medium2}, material-web verbatim. */
  static final int DOT_GROW_MS = MorphAnimator.MEDIUM2_MS;

  /** Deselect dot fade + ring color crossfade duration — material-web's 50ms linear transitions. */
  static final int COLOR_FADE_MS = 50;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;

  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private final RetargetTween dotScale;
  private final RetargetTween dotAlpha;
  private final RetargetTween ringBlend;

  private boolean selected;
  private boolean hovered;
  private boolean pressed;

  private ElwhaRadioGroup group;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private ColorRole rippleTintRole = ColorRole.PRIMARY;
  private Timer rippleTimer;

  /**
   * Creates an unselected radio button.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioButton() {
    this(false);
  }

  /**
   * Creates a radio button in the given selection state.
   *
   * @param selected the initial selection state
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioButton(final boolean selected) {
    this.selected = selected;
    final float rest = selected ? 1f : 0f;
    this.dotScale = new RetargetTween(DOT_GROW_MS, rest);
    this.dotAlpha = new RetargetTween(COLOR_FADE_MS, rest);
    this.ringBlend = new RetargetTween(COLOR_FADE_MS, rest);
    setOpaque(false);
    setFocusable(true);
    initInteraction();
    initKeyboard();
  }

  // ------------------------------------------------------------------ selection

  /**
   * Returns whether the radio button is selected.
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
   * actually changes; never fires the user-gesture {@link ActionListener}s (material-web parity:
   * {@code change} fires on user interaction only). Both directions are legal programmatically —
   * only <em>user gestures</em> are select-only (research §B).
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
    syncMotion(animateAllowed());
    if (group != null) {
      group.memberSelectionChanged(this, selected);
    }
    fireStateChanged();
    repaint();
  }

  /**
   * Returns the {@link ElwhaRadioGroup} this radio belongs to, or {@code null} when ungrouped.
   * Membership is managed through {@link ElwhaRadioGroup#add} / {@link ElwhaRadioGroup#remove}.
   *
   * @return the owning group, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioGroup getGroup() {
    return group;
  }

  /** Back-reference maintained by {@link ElwhaRadioGroup#add} / {@link ElwhaRadioGroup#remove}. */
  void setGroup(final ElwhaRadioGroup group) {
    this.group = group;
  }

  /**
   * Retargets the dot + ring tweens at the current state; snaps when animation is not allowed
   * (research §Mo): select grows the dot over {@value #DOT_GROW_MS}ms emphasized-decelerate while
   * its alpha and the ring color arrive in {@value #COLOR_FADE_MS}ms; deselect fades the dot at its
   * current scale — M3 has no shrink — and crossfades the ring back.
   */
  private void syncMotion(final boolean animate) {
    if (selected) {
      if (dotAlpha.value() <= 0f) {
        dotScale.seed(0f);
      }
      dotScale.retarget(1f, DOT_GROW_MS, Easing.EMPHASIZED_DECELERATE, animate);
      dotAlpha.retarget(1f, COLOR_FADE_MS, Easing.LINEAR, animate);
    } else {
      dotScale.seed(dotScale.value());
      dotAlpha.retarget(0f, COLOR_FADE_MS, Easing.LINEAR, animate);
    }
    ringBlend.retarget(selected ? 1f : 0f, COLOR_FADE_MS, Easing.LINEAR, animate);
  }

  /** Whether state changes may tween — never while undisplayable (first paint) or disabled. */
  private boolean animateAllowed() {
    return isDisplayable() && isEnabled();
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
    for (final ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(changeEvent);
    }
  }

  /**
   * Adds an {@link ActionListener} notified when the <em>user</em> selects this radio — a click, a
   * Space release, or (once grouped) an arrow-key arrival. The event's action command is always
   * {@code "selected"}: no user gesture deselects a radio, so no {@code "deselected"} command
   * exists. Programmatic {@link #setSelected(boolean)} writes never fire it.
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

  /** Notifies registered {@link ActionListener}s of a user-gesture selection. */
  private void fireActionPerformed() {
    final ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selected");
    for (final ActionListener listener : listenerList.getListeners(ActionListener.class)) {
      listener.actionPerformed(event);
    }
  }

  /**
   * Commits a user-gesture selection: a no-op when already selected (research §B — re-affirming
   * fires nothing), otherwise the state change first, then the action event.
   */
  void commitUserSelect() {
    if (selected) {
      return;
    }
    setSelected(true);
    fireActionPerformed();
  }

  // -------------------------------------------------------------- gallery hooks

  /**
   * Forces the hover treatment on or off — for static gallery rendering only (the slider/switch
   * parallel); real hover tracking is automatic.
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
   * Forces the pressed treatment on or off — for static gallery rendering only (the slider/switch
   * parallel); real press tracking is automatic.
   *
   * @param pressed whether to paint the pressed state layer (in the swapped tint)
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
    return new Dimension(STATE_LAYER_SIZE_PX, STATE_LAYER_SIZE_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ------------------------------------------------------------------ geometry

  /** The icon center's x — the component center. */
  int iconCenterX() {
    return getWidth() / 2;
  }

  /** The icon center's y — the component center. */
  int iconCenterY() {
    return getHeight() / 2;
  }

  /** The icon circle, inset uniformly — {@code 0} is the ring's outer edge. */
  private Ellipse2D.Float iconCircle(final float inset) {
    final float d = ICON_SIZE_PX - 2f * inset;
    return new Ellipse2D.Float(iconCenterX() - d / 2f, iconCenterY() - d / 2f, d, d);
  }

  /** The ring — outer circle minus the ring-width-inset circle. */
  private Area ring() {
    final Area ring = new Area(iconCircle(0));
    ring.subtract(new Area(iconCircle(RING_WIDTH_PX)));
    return ring;
  }

  /** The inner dot at the given diameter, concentric with the icon. */
  private Ellipse2D.Float dotCircle(final float diameter) {
    return new Ellipse2D.Float(
        iconCenterX() - diameter / 2f, iconCenterY() - diameter / 2f, diameter, diameter);
  }

  /** The {@value #STATE_LAYER_SIZE_PX} dp interaction circle concentric with the icon. */
  private Ellipse2D.Float stateLayerCircle() {
    final float r = STATE_LAYER_SIZE_PX / 2f;
    return new Ellipse2D.Float(
        iconCenterX() - r, iconCenterY() - r, STATE_LAYER_SIZE_PX, STATE_LAYER_SIZE_PX);
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintStateLayer(g2);
      paintRipple(g2);
      paintIcon(g2);
    } finally {
      g2.dispose();
    }
  }

  /**
   * Paints the static hover/focus/pressed layers on the circle. Hover and focus use the state's
   * tint ({@code ON_SURFACE} unselected / {@code PRIMARY} selected); pressed uses the
   * <strong>swap</strong> (research §C′). The layers stack independently, the slider/switch rule.
   */
  private void paintStateLayer(final Graphics2D g2) {
    if (!isEnabled()) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      final Ellipse2D.Float halo = stateLayerCircle();
      final Color restTint = (selected ? ColorRole.PRIMARY : ColorRole.ON_SURFACE).resolve();
      if (isFocusOwner()) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.FOCUS.opacity()));
        s.setColor(restTint);
        s.fill(halo);
      }
      if (hovered) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.HOVER.opacity()));
        s.setColor(restTint);
        s.fill(halo);
      }
      // Pressed feedback is primarily the ripple; the static layer keeps the pressed state
      // legible in still renders (gallery cells, reduced motion, between ripple frames).
      if (pressed) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.PRESSED.opacity()));
        s.setColor(pressedTint());
        s.fill(halo);
      }
    } finally {
      s.dispose();
    }
  }

  /**
   * The pressed/ripple tint — the M3 swap: {@code PRIMARY} unselected, {@code ON_SURFACE} selected.
   */
  private Color pressedTint() {
    return (selected ? ColorRole.ON_SURFACE : ColorRole.PRIMARY).resolve();
  }

  /**
   * Paints the press ripple, bounded to the state-layer circle (material-web bounds {@code
   * md-ripple} the same way). The tint role is captured at press time so the ripple of the gesture
   * that selects stays {@code PRIMARY} even as the state flips beneath it.
   */
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
          rippleTintRole.resolve());
    } finally {
      r.dispose();
    }
  }

  /**
   * Paints the ring, and the dot at its tweened scale and alpha — mid-grow the dot is a smaller
   * {@code PRIMARY} circle; mid-fade it holds its scale and loses alpha (research §Mo).
   */
  private void paintIcon(final Graphics2D g2) {
    g2.setColor(ringColor());
    g2.fill(ring());
    final float scale = clampF(dotScale.value());
    final float alpha = clampF(dotAlpha.value());
    if (scale <= 0f || alpha <= 0f) {
      return;
    }
    final Graphics2D d = (Graphics2D) g2.create();
    try {
      if (alpha < 1f) {
        d.setComposite(AlphaComposite.SrcOver.derive(alpha));
      }
      d.setColor(dotColor());
      d.fill(dotCircle(DOT_SIZE_PX * scale));
    } finally {
      d.dispose();
    }
  }

  // --------------------------------------------------------------------- color

  /**
   * Whether any interactive treatment is active — drives the unselected ring shift (research §T).
   */
  private boolean interactionActive() {
    return hovered || pressed || isFocusOwner();
  }

  /**
   * The ring fill — per-state roles crossfaded over {@value #COLOR_FADE_MS}ms (research §Mo),
   * disabled at the 0.38 content opacity (research §T).
   */
  private Color ringColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    final Color unselectedRing =
        (interactionActive() ? ColorRole.ON_SURFACE : ColorRole.ON_SURFACE_VARIANT).resolve();
    return mixColor(unselectedRing, ColorRole.PRIMARY.resolve(), clampF(ringBlend.value()));
  }

  /** The dot fill — {@link ColorRole#PRIMARY}, disabled at the 0.38 content opacity. */
  private Color dotColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return ColorRole.PRIMARY.resolve();
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
            startRipple(e.getPoint());
            repaint();
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed) {
              return;
            }
            pressed = false;
            if (isEnabled() && contains(e.getPoint())) {
              commitUserSelect();
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
  }

  private void initKeyboard() {
    getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "elwhaRadio.press");
    getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "elwhaRadio.release");
    getActionMap()
        .put(
            "elwhaRadio.press",
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                if (!ElwhaRadioButton.this.isEnabled() || pressed) {
                  return;
                }
                pressed = true;
                startRipple(new Point(iconCenterX(), iconCenterY()));
                repaint();
              }
            });
    getActionMap()
        .put(
            "elwhaRadio.release",
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                if (!ElwhaRadioButton.this.isEnabled() || !pressed) {
                  return;
                }
                pressed = false;
                commitUserSelect();
                repaint();
              }
            });
  }

  private void startRipple(final Point origin) {
    rippleOrigin = origin;
    rippleProgress = 0f;
    rippleTintRole = selected ? ColorRole.ON_SURFACE : ColorRole.PRIMARY;
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
    dotScale.finish();
    dotAlpha.finish();
    ringBlend.finish();
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

  /** Opaque RGB lerp between two colors. */
  private static Color mixColor(final Color a, final Color b, final float t) {
    final float f = clampF(t);
    return new Color(
        Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
        Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
        Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
  }

  /**
   * A retargeting tween over one float quantity — the CSS-transition model the research §Mo
   * durations describe. Each {@link #retarget} captures the current value as the new starting point
   * and re-runs the backing {@link MorphAnimator} {@code 0→1} through the given {@link Easing}, so
   * a mid-flight direction change (deselect during the dot grow) continues from wherever the value
   * currently is instead of jumping. Re-implemented privately from {@code ElwhaSwitch} — the #401
   * branch is unmerged; extraction to {@code theme/} is the recorded design §14-2 follow-up.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  private final class RetargetTween {

    private final MorphAnimator animator;
    private float from;
    private float to;
    private Easing easing = Easing.LINEAR;

    RetargetTween(final int durationMs, final float initial) {
      this.animator = new MorphAnimator(ElwhaRadioButton.this, durationMs);
      this.from = initial;
      this.to = initial;
      this.animator.snapTo(1f);
    }

    float value() {
      return from + (to - from) * easing.ease(animator.progress());
    }

    void retarget(
        final float target, final int durationMs, final Easing easing, final boolean animate) {
      if (this.to == target) {
        if (!animate) {
          this.from = target;
          animator.snapTo(1f);
        }
        return;
      }
      this.from = value();
      this.to = target;
      this.easing = easing;
      animator.setDurationMs(durationMs);
      if (animate) {
        animator.snapTo(0f);
        animator.start();
      } else {
        this.from = target;
        animator.snapTo(1f);
      }
    }

    /** Re-seats the tween at the given value without animating. */
    void seed(final float value) {
      this.from = value;
      this.to = value;
      animator.snapTo(1f);
    }

    /** Stops the timer and lands on the target — {@code removeNotify} cleanup. */
    void finish() {
      animator.immediateFinish();
    }
  }
}
