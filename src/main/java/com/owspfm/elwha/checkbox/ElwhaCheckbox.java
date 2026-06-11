package com.owspfm.elwha.checkbox;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.FocusVisible;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.BasicStroke;
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * M3 checkbox — a dedicated {@link JComponent} primitive painting the Material 3 checkbox anatomy:
 * an 18px rounded-square container (2px corner radius, 2px outline when unchecked, {@link
 * ColorRole#PRIMARY} fill when checked or indeterminate), a hand-stroked checkmark / indeterminate
 * dash, a 40px circular state-layer field, and a 48px minimum touch target. Spec capture and design
 * locks: {@code docs/research/elwha-checkbox-research.md} / {@code elwha-checkbox-design.md} (epic
 * #410).
 *
 * <p>The check state is tri-state ({@link CheckState}) with boolean conveniences mirroring the M3
 * nouns: {@link #setChecked(boolean) checked} and {@link #setIndeterminate(boolean) indeterminate}.
 * Every state change fires a {@link #PROPERTY_CHECK_STATE} property-change event; user-driven
 * toggles (click anywhere in the target, Space when focused) additionally fire the registered
 * {@link #addActionListener(ActionListener) action listeners}. The user toggle cycle is {@code
 * UNCHECKED → CHECKED → UNCHECKED}; an indeterminate checkbox exits to {@code CHECKED}.
 *
 * <p>Interaction follows the M3 token table exactly, including the cross-color pressed state
 * layers: an unchecked press previews {@code PRIMARY}, a checked press previews {@code ON_SURFACE}.
 * The press ripple is the shared {@link RipplePainter} clipped to the 40px circle; the mark draws
 * in / retracts via a {@link MorphAnimator}-driven stroke reveal (reduced-motion snaps).
 *
 * <p>All theme tokens resolve at paint time — the component auto-themes across palette / mode
 * swaps with zero new theme tokens; geometry is fixed component constants per design §5.
 *
 * <p><strong>Parent–child tri-state wiring</strong> (a parent checkbox summarizing children) is
 * deliberately app logic, as in every first-party M3 implementation: listen to the children, call
 * {@link #setIndeterminate(boolean)} on the parent when the children disagree, and {@link
 * #setChecked(boolean)} when they agree.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaCheckbox extends JComponent {

  /**
   * The tri-state check value of an {@link ElwhaCheckbox} — M3's {@code unchecked} / {@code
   * checked} / {@code indeterminate}. Indeterminate is entered only programmatically; user
   * interaction exits it to {@link #CHECKED} (design §8).
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public enum CheckState {
    /** Not checked — outlined container, no mark. */
    UNCHECKED,
    /** Checked — filled container with the checkmark. */
    CHECKED,
    /** Partially checked — filled container with the horizontal dash. */
    INDETERMINATE
  }

  /** Property-change key fired whenever the {@link CheckState} changes. */
  public static final String PROPERTY_CHECK_STATE = "checkState";

  /** Container edge length in px — M3 token {@code container-size} (18dp). */
  private static final int CONTAINER_SIZE = 18;

  /**
   * Container corner arc in px — M3 token {@code container-shape} is a 2dp corner radius; stored as
   * the round-rect arc diameter per the lib's arc-width convention.
   */
  private static final int CONTAINER_ARC = 4;

  /** Outline stroke width in px — M3 token {@code outline-width} (2dp, unchecked only). */
  private static final float OUTLINE_WIDTH = 2f;

  /** State-layer circle diameter in px — M3 token {@code state-layer-size} (40dp). */
  static final int STATE_LAYER_SIZE = 40;

  /** Minimum touch-target edge in px — M3 / WCAG 48dp, matching the button family's inflation. */
  static final int TOUCH_TARGET = 48;

  /** Mark stroke width in px (checkmark and indeterminate dash). */
  private static final float MARK_STROKE = 2f;

  // Mark geometry in 18x18 container coordinates (design §5): the checkmark polyline's three
  // points and the dash's half-length around the container center.
  private static final float[] CHECK_X = {4.4f, 7.4f, 13.6f};
  private static final float[] CHECK_Y = {9.3f, 12.3f, 6.1f};
  private static final float DASH_HALF_LENGTH = 4.5f;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;

  private CheckState checkState = CheckState.UNCHECKED;

  private final List<ActionListener> actionListeners = new ArrayList<>();

  // Fill cross-fade (outline ↔ filled container) and mark stroke-reveal run on separate slots so
  // an INDETERMINATE → CHECKED swap can redraw the mark while the fill holds steady at 1.
  private final MorphAnimator fillAnimator = new MorphAnimator(this, MorphAnimator.SHORT3_MS);
  private final MorphAnimator markAnimator = new MorphAnimator(this, MorphAnimator.SHORT3_MS);

  // The glyph the mark animator is revealing/retracting — stays at the previous selected state
  // while unchecking so the retract draws the mark the user last saw.
  private CheckState markGlyph = CheckState.CHECKED;

  private boolean hovered;
  private boolean pressed;
  private boolean focusVisible;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Color rippleTint;
  private Timer rippleTimer;

  /**
   * Creates an unchecked, enabled checkbox.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaCheckbox() {
    setOpaque(false);
    setFocusable(true);
    initInteraction();
    initKeyboard();
  }

  /**
   * Returns the current tri-state check value.
   *
   * @return the check state; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public CheckState getCheckState() {
    return checkState;
  }

  /**
   * Sets the tri-state check value, firing {@link #PROPERTY_CHECK_STATE} when it changes. This is
   * the programmatic path — no {@code ActionListener} fires (those are reserved for user-driven
   * toggles). Animates the fill / mark transition when showing; snaps when not realized or under
   * reduced motion.
   *
   * @param state the new check state
   * @throws NullPointerException if {@code state} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setCheckState(final CheckState state) {
    if (state == null) {
      throw new NullPointerException("state");
    }
    if (this.checkState == state) {
      return;
    }
    final CheckState old = this.checkState;
    this.checkState = state;
    animateTransition(state);
    firePropertyChange(PROPERTY_CHECK_STATE, old, state);
    repaint();
  }

  /**
   * Returns whether the checkbox is checked. Indeterminate reports {@code false} — it is the
   * "neither" answer, matching the M3 implementations.
   *
   * @return {@code true} only in {@link CheckState#CHECKED}
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isChecked() {
    return checkState == CheckState.CHECKED;
  }

  /**
   * Convenience for {@link #setCheckState(CheckState)} with {@link CheckState#CHECKED} / {@link
   * CheckState#UNCHECKED}.
   *
   * @param checked whether the checkbox is checked
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setChecked(final boolean checked) {
    setCheckState(checked ? CheckState.CHECKED : CheckState.UNCHECKED);
  }

  /**
   * Returns whether the checkbox is indeterminate.
   *
   * @return {@code true} only in {@link CheckState#INDETERMINATE}
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isIndeterminate() {
    return checkState == CheckState.INDETERMINATE;
  }

  /**
   * Convenience for {@link #setCheckState(CheckState)} with {@link CheckState#INDETERMINATE};
   * clearing indeterminate lands on {@link CheckState#UNCHECKED}.
   *
   * @param indeterminate whether the checkbox is indeterminate
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setIndeterminate(final boolean indeterminate) {
    if (indeterminate) {
      setCheckState(CheckState.INDETERMINATE);
    } else if (checkState == CheckState.INDETERMINATE) {
      setCheckState(CheckState.UNCHECKED);
    }
  }

  /**
   * Registers a listener fired on every <em>user-driven</em> toggle — pointer click or Space.
   * Programmatic {@link #setCheckState(CheckState)} calls do not fire it; observe {@link
   * #PROPERTY_CHECK_STATE} for those.
   *
   * @param listener the listener to add; {@code null} is ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addActionListener(final ActionListener listener) {
    if (listener != null) {
      actionListeners.add(listener);
    }
  }

  /**
   * Removes a previously-registered action listener.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      hovered = false;
      pressed = false;
      rippleProgress = 1f;
      setCursor(Cursor.getDefaultCursor());
    }
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    return new Dimension(TOUCH_TARGET, TOUCH_TARGET);
  }

  @Override
  public Dimension getMinimumSize() {
    if (isMinimumSizeSet()) {
      return super.getMinimumSize();
    }
    return getPreferredSize();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    syncAnimatorsToState();
  }

  @Override
  public void removeNotify() {
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    fillAnimator.stop();
    markAnimator.stop();
    super.removeNotify();
  }

  // ---------------------------------------------------------------- motion

  private void animateTransition(final CheckState next) {
    if (!isShowing()) {
      syncAnimatorsToState();
      return;
    }
    if (next == CheckState.UNCHECKED) {
      fillAnimator.reverse();
      markAnimator.reverse();
      return;
    }
    markGlyph = next;
    fillAnimator.start();
    markAnimator.snapTo(0f);
    markAnimator.start();
  }

  private void syncAnimatorsToState() {
    final float resting = checkState == CheckState.UNCHECKED ? 0f : 1f;
    if (checkState != CheckState.UNCHECKED) {
      markGlyph = checkState;
    }
    fillAnimator.snapTo(resting);
    markAnimator.snapTo(resting);
  }

  private void startRipple() {
    rippleOrigin = new Point(STATE_LAYER_SIZE / 2, STATE_LAYER_SIZE / 2);
    rippleTint = pressedLayerTint();
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
            focusVisible = false;
            pressed = true;
            startRipple();
            repaint();
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed) {
              return;
            }
            pressed = false;
            if (isEnabled() && contains(e.getPoint())) {
              userToggle();
            }
            repaint();
          }
        };
    addMouseListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            focusVisible = FocusVisible.isKeyboardCause(e.getCause());
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            focusVisible = false;
            pressed = false;
            repaint();
          }
        });
  }

  private void initKeyboard() {
    final InputMap im = getInputMap(WHEN_FOCUSED);
    final ActionMap am = getActionMap();
    final Action activate =
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (!isEnabled()) {
              return;
            }
            startRipple();
            userToggle();
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhacheckbox.toggle");
    am.put("elwhacheckbox.toggle", activate);
  }

  private void userToggle() {
    setCheckState(
        checkState == CheckState.CHECKED ? CheckState.UNCHECKED : CheckState.CHECKED);
    final ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle");
    for (final ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(evt);
    }
  }

  // ---------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      final int cx = TOUCH_TARGET / 2;
      final int cy = getHeight() / 2;
      paintStateLayer(g2, cx, cy);
      paintRipple(g2, cx, cy);
      paintContainer(g2, cx, cy);
    } finally {
      g2.dispose();
    }
  }

  private void paintStateLayer(final Graphics2D g2, final int cx, final int cy) {
    final StateLayer layer = activeLayer();
    if (layer == null) {
      return;
    }
    final Color tint = layer == StateLayer.PRESSED ? pressedLayerTint() : hoverFocusLayerTint();
    g2.setColor(withAlpha(tint, layer.opacity()));
    g2.fill(
        new Ellipse2D.Float(
            cx - STATE_LAYER_SIZE / 2f, cy - STATE_LAYER_SIZE / 2f,
            STATE_LAYER_SIZE, STATE_LAYER_SIZE));
  }

  private StateLayer activeLayer() {
    if (!isEnabled()) {
      return null;
    }
    if (pressed) {
      return StateLayer.PRESSED;
    }
    if (hovered) {
      return StateLayer.HOVER;
    }
    if (focusVisible) {
      return StateLayer.FOCUS;
    }
    return null;
  }

  private void paintRipple(final Graphics2D g2, final int cx, final int cy) {
    if (rippleOrigin == null || rippleProgress >= 1f || !isEnabled()) {
      return;
    }
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(cx - STATE_LAYER_SIZE / 2, cy - STATE_LAYER_SIZE / 2);
      RipplePainter.paint(
          r,
          STATE_LAYER_SIZE,
          STATE_LAYER_SIZE,
          rippleOrigin,
          rippleProgress,
          STATE_LAYER_SIZE,
          rippleTint);
    } finally {
      r.dispose();
    }
  }

  /**
   * Paints the container cross-fade and the mark, centered at {@code (cx, cy)}: the outline fades
   * out as the fill fades in, and the mark reveals to the mark animator's eased progress.
   */
  private void paintContainer(final Graphics2D g2, final int cx, final int cy) {
    final float bx = cx - CONTAINER_SIZE / 2f;
    final float by = cy - CONTAINER_SIZE / 2f;
    final float fill = Easing.STANDARD.ease(fillAnimator.progress());
    if (fill < 1f) {
      g2.setColor(withAlpha(outlineColor(), 1f - fill));
      g2.setStroke(new BasicStroke(OUTLINE_WIDTH));
      final float inset = OUTLINE_WIDTH / 2f;
      g2.draw(
          new RoundRectangle2D.Float(
              bx + inset,
              by + inset,
              CONTAINER_SIZE - OUTLINE_WIDTH,
              CONTAINER_SIZE - OUTLINE_WIDTH,
              CONTAINER_ARC - OUTLINE_WIDTH / 2f,
              CONTAINER_ARC - OUTLINE_WIDTH / 2f));
    }
    if (fill <= 0f) {
      return;
    }
    g2.setColor(withAlpha(containerFillColor(), fill));
    g2.fill(
        new RoundRectangle2D.Float(
            bx, by, CONTAINER_SIZE, CONTAINER_SIZE, CONTAINER_ARC, CONTAINER_ARC));
    final float reveal =
        (markAnimator.target() >= 1f ? Easing.STANDARD_DECELERATE : Easing.STANDARD_ACCELERATE)
            .ease(markAnimator.progress());
    if (reveal <= 0f) {
      return;
    }
    g2.setColor(markColor());
    g2.setStroke(new BasicStroke(MARK_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    if (markGlyph == CheckState.INDETERMINATE) {
      g2.draw(dashPath(bx, by, reveal));
    } else {
      g2.draw(checkmarkPath(bx, by, reveal));
    }
  }

  /**
   * The checkmark polyline revealed to {@code progress} of its arc length — the M3 "draw" gesture.
   * Progress {@code 1} is the full mark.
   */
  private static Path2D checkmarkPath(final float bx, final float by, final float progress) {
    final float seg1 = (float) Math.hypot(CHECK_X[1] - CHECK_X[0], CHECK_Y[1] - CHECK_Y[0]);
    final float seg2 = (float) Math.hypot(CHECK_X[2] - CHECK_X[1], CHECK_Y[2] - CHECK_Y[1]);
    final float drawn = Math.max(0f, Math.min(1f, progress)) * (seg1 + seg2);
    final Path2D path = new Path2D.Float();
    path.moveTo(bx + CHECK_X[0], by + CHECK_Y[0]);
    if (drawn <= seg1) {
      final float t = drawn / seg1;
      path.lineTo(
          bx + CHECK_X[0] + (CHECK_X[1] - CHECK_X[0]) * t,
          by + CHECK_Y[0] + (CHECK_Y[1] - CHECK_Y[0]) * t);
    } else {
      path.lineTo(bx + CHECK_X[1], by + CHECK_Y[1]);
      final float t = (drawn - seg1) / seg2;
      path.lineTo(
          bx + CHECK_X[1] + (CHECK_X[2] - CHECK_X[1]) * t,
          by + CHECK_Y[1] + (CHECK_Y[2] - CHECK_Y[1]) * t);
    }
    return path;
  }

  /** The indeterminate dash grown from the container center to {@code progress} of its length. */
  private static Path2D dashPath(final float bx, final float by, final float progress) {
    final float half = DASH_HALF_LENGTH * Math.max(0f, Math.min(1f, progress));
    final float midX = bx + CONTAINER_SIZE / 2f;
    final float midY = by + CONTAINER_SIZE / 2f;
    final Path2D path = new Path2D.Float();
    path.moveTo(midX - half, midY);
    path.lineTo(midX + half, midY);
    return path;
  }

  // ---------------------------------------------------------------- colors

  /**
   * Unchecked outline color per the research §B table — {@code ON_SURFACE_VARIANT} at rest,
   * brightening to {@code ON_SURFACE} under hover / focus / press, {@code ON_SURFACE} @ 0.38
   * disabled.
   */
  private Color outlineColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    if (hovered || pressed || focusVisible) {
      return ColorRole.ON_SURFACE.resolve();
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
  }

  /** Selected-family container fill (checked and indeterminate share the selected colors). */
  private Color containerFillColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return ColorRole.PRIMARY.resolve();
  }

  /** Mark color — disabled punches {@code SURFACE} through the 38% fill per the token table. */
  private Color markColor() {
    if (!isEnabled()) {
      return ColorRole.SURFACE.resolve();
    }
    return ColorRole.ON_PRIMARY.resolve();
  }

  /** Hover / focus layers tint with the <em>current</em> state's color. */
  private Color hoverFocusLayerTint() {
    return checkState == CheckState.UNCHECKED
        ? ColorRole.ON_SURFACE.resolve()
        : ColorRole.PRIMARY.resolve();
  }

  /**
   * The pressed layer (and ripple) tints with the <em>destination</em> state's color — the M3
   * cross-color rule: unchecked-pressed previews {@code PRIMARY}, selected-pressed previews {@code
   * ON_SURFACE}.
   */
  private Color pressedLayerTint() {
    return checkState == CheckState.UNCHECKED
        ? ColorRole.PRIMARY.resolve()
        : ColorRole.ON_SURFACE.resolve();
  }

  private static Color withAlpha(final Color base, final float alpha) {
    final float clamped = Math.max(0f, Math.min(1f, alpha));
    return new Color(
        base.getRed(), base.getGreen(), base.getBlue(), Math.round(clamped * 255f));
  }
}
