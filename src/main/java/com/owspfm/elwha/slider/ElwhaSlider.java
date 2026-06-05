package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

/**
 * The Elwha Material 3 <strong>Expressive slider</strong> — a token-themed range input painting M3
 * chrome: a split active/inactive track with the rounded-end gap, a tall pill handle that narrows
 * when active, hover/focus state layers, a press ripple, and an optional value-indicator bubble.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-slider-design.md} §2).</strong> {@code ElwhaSlider} is one dedicated {@link JComponent}
 * that paints every M3 part itself, backed by a {@link BoundedRangeModel} ({@link
 * DefaultBoundedRangeModel}) for the value / min / max math. It is <em>not</em> a {@code JSlider}
 * subclass and <em>not</em> a {@code SliderUI} delegate: {@code BasicSliderUI}'s track/thumb layout
 * fights the M3 split-track-with-gap geometry, and the eventual range variant (two handles) is not
 * a {@code JSlider} concept — so a unified custom component keeps single and range on one paint /
 * interaction codebase.
 *
 * <p><strong>Phase-1 surface (this story arc).</strong> The {@code STANDARD} variant, horizontal,
 * {@code XS} size, continuous (later: {@code stops}), with an optional value indicator. Variant /
 * size / orientation axes are later V1 phases — the geometry constants below are the XS preset
 * (M3's only off-Android code preset; research §M / §Cfg).
 *
 * <p><strong>Interaction & motion (research §S / §TS / §B).</strong> Drag the handle or click the
 * track to jump; the value updates live and a {@link ChangeListener} fires on every change with
 * {@link #getValueIsAdjusting()} true mid-drag. Hover paints {@link StateLayer#HOVER} (0.08), focus
 * {@link StateLayer#FOCUS} (0.10); a press shows a {@link RipplePainter} ripple. The handle
 * <strong>narrows {@value #HANDLE_WIDTH_PX}&rarr;{@value #NARROW_HANDLE_WIDTH_PX} dp on
 * focus/press</strong> (height constant) and the value bubble fades/scales in — both via a short
 * tween that snaps under {@link MorphAnimator#isReducedMotion() reduced motion}.
 *
 * <p><strong>Geometry (XS, M3 token-locked — research §M / §T).</strong> Track {@value
 * #TRACK_HEIGHT_PX} dp tall; handle {@value #HANDLE_HEIGHT_PX}&times;{@value #HANDLE_WIDTH_PX} dp
 * flat pill (no shadow — M3 deprecates handle elevation); {@value #HANDLE_TRACK_GAP_PX} dp gap
 * between the handle and each track segment; value bubble {@value
 * #VALUE_BUBBLE_WIDTH_PX}&times;{@value #VALUE_BUBBLE_HEIGHT_PX} dp, {@value #VALUE_BUBBLE_GAP_PX}
 * dp above the handle.
 *
 * <p><strong>Color (zero new tokens — research §Color).</strong> Active track + handle = {@link
 * ColorRole#PRIMARY}; inactive track = {@link ColorRole#SECONDARY_CONTAINER}; value bubble = {@link
 * ColorRole#INVERSE_SURFACE} + {@link ColorRole#INVERSE_ON_SURFACE}; disabled = {@link
 * ColorRole#ON_SURFACE} at the M3 content (0.38) / container (0.12) opacities. Resolved at paint
 * time so runtime theme + light/dark switching re-skins the slider live.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaSlider extends JComponent {

  // --- XS geometry preset (dp == px at 100% scale; research §M / §T) ---

  /** Track thickness (both active and inactive segments). */
  static final int TRACK_HEIGHT_PX = 16;

  /** Resting handle height — constant across all interaction states. */
  static final int HANDLE_HEIGHT_PX = 44;

  /** Resting handle width — the tall pill. */
  static final int HANDLE_WIDTH_PX = 4;

  /** Active (focus/press) handle width — the pill narrows to this. */
  static final int NARROW_HANDLE_WIDTH_PX = 2;

  /** Gap between the handle and each adjacent track segment ({@code thumbTrackGapSize}). */
  static final int HANDLE_TRACK_GAP_PX = 6;

  /** Stop-indicator dot diameter (painted in the stops story). */
  static final int STOP_INDICATOR_SIZE_PX = 4;

  /** Track outer (far-end) corner radius — full round on the 16&nbsp;dp XS track. */
  static final int TRACK_OUTER_CORNER_PX = 8;

  /** Track inner (gap-side) corner radius — the squared-off end facing the handle. */
  static final int TRACK_INNER_CORNER_PX = 2;

  /** Value-indicator bubble width. */
  static final int VALUE_BUBBLE_WIDTH_PX = 48;

  /** Value-indicator bubble height (rounded body plus the downward nub). */
  static final int VALUE_BUBBLE_HEIGHT_PX = 44;

  /** Gap between the value bubble's nub tip and the handle top. */
  static final int VALUE_BUBBLE_GAP_PX = 12;

  /** The width of the handle's hover/focus/press interaction halo (state layer + ripple bounds). */
  static final int HANDLE_HALO_WIDTH_PX = 20;

  /** Default preferred track length when the layout gives the slider its preferred size. */
  static final int DEFAULT_TRACK_LENGTH_PX = 240;

  private static final int VALUE_BUBBLE_NUB_HEIGHT_PX = 8;
  private static final int HANDLE_MORPH_MS = MorphAnimator.SHORT3_MS;
  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final float VALUE_BUBBLE_MIN_SCALE = 0.85f;
  private static final float VALUE_BUBBLE_LABEL_PT = 14f;

  private final BoundedRangeModel model;
  private final ChangeListener modelListener = e -> repaint();

  private boolean hovered;
  private boolean pressed;
  private boolean valueIndicatorEnabled;
  private boolean spaceDown;

  private int unitIncrement = 1;
  private Integer blockIncrement;
  private String label;

  private final MorphAnimator interactionAnimator = new MorphAnimator(this, HANDLE_MORPH_MS);

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;

  /**
   * Creates a slider over {@code [0, 100]} with an initial value of {@code 0}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSlider() {
    this(0, 100, 0);
  }

  /**
   * Creates a slider over {@code [min, max]} with the given initial value.
   *
   * @param min the range lower bound
   * @param max the range upper bound
   * @param value the initial value, clamped into {@code [min, max]}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSlider(final int min, final int max, final int value) {
    this(new DefaultBoundedRangeModel(clamp(value, min, max), 0, min, max));
  }

  /**
   * Creates a slider backed by a caller-supplied {@link BoundedRangeModel}. The model is the single
   * source of truth for value / min / max; the slider paints from it and writes back to it.
   *
   * @param model the value model; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSlider(final BoundedRangeModel model) {
    if (model == null) {
      throw new IllegalArgumentException("model");
    }
    this.model = model;
    this.model.addChangeListener(modelListener);
    setOpaque(false);
    setFocusable(true);
    initInteraction();
    initKeyboard();
  }

  // ------------------------------------------------------------------ value API

  /**
   * Returns the slider's current value.
   *
   * @return the current value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getValue() {
    return model.getValue();
  }

  /**
   * Sets the slider's value, clamped into the current {@code [min, max]} range.
   *
   * @param value the new value
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setValue(final int value) {
    model.setValue(clamp(value, model.getMinimum(), model.getMaximum()));
  }

  /**
   * Returns the range lower bound.
   *
   * @return the minimum value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getMinimum() {
    return model.getMinimum();
  }

  /**
   * Sets the range lower bound.
   *
   * @param minimum the new minimum
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setMinimum(final int minimum) {
    model.setMinimum(minimum);
  }

  /**
   * Returns the range upper bound.
   *
   * @return the maximum value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getMaximum() {
    return model.getMaximum();
  }

  /**
   * Sets the range upper bound.
   *
   * @param maximum the new maximum
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setMaximum(final int maximum) {
    model.setMaximum(maximum);
  }

  /**
   * Returns the backing value model.
   *
   * @return the model
   * @version v0.4.0
   * @since v0.4.0
   */
  public BoundedRangeModel getModel() {
    return model;
  }

  /**
   * Reports whether the value is mid-adjustment — {@code true} from the start of a drag (or
   * click-to-jump press) until release. Mirrors {@code JSlider.getValueIsAdjusting()}: a {@link
   * ChangeListener} can use it to distinguish live drag updates from the committed value.
   *
   * @return {@code true} while the handle is being dragged
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean getValueIsAdjusting() {
    return model.getValueIsAdjusting();
  }

  /**
   * Adds a {@link ChangeListener} notified on every value change, including live changes mid-drag.
   * Delegates to the backing model.
   *
   * @param listener the listener to add
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addChangeListener(final ChangeListener listener) {
    model.addChangeListener(listener);
  }

  /**
   * Removes a previously added {@link ChangeListener}.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    model.removeChangeListener(listener);
  }

  // -------------------------------------------------------------- configuration

  /**
   * Returns whether the value-indicator bubble is shown on focus/press.
   *
   * @return {@code true} if the value indicator is enabled
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isValueIndicatorEnabled() {
    return valueIndicatorEnabled;
  }

  /**
   * Enables or disables the value-indicator bubble (M3 {@code labeled}). When enabled, a bubble
   * showing the current value fades in above the handle while the slider is focused or pressed.
   * Enabling it reserves vertical space above the track so the bubble never clips. Defaults to
   * {@code false}, matching M3 (the value indicator is an opt-in configuration).
   *
   * @param enabled {@code true} to show the value bubble on focus/press
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setValueIndicatorEnabled(final boolean enabled) {
    if (this.valueIndicatorEnabled == enabled) {
      return;
    }
    this.valueIndicatorEnabled = enabled;
    revalidate();
    repaint();
  }

  /**
   * Returns the value change applied by a single arrow keypress.
   *
   * @return the unit increment
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getUnitIncrement() {
    return unitIncrement;
  }

  /**
   * Sets the value change applied by a single arrow keypress (default {@code 1}). In stops mode the
   * arrows step by one stop regardless (story #345).
   *
   * @param increment the unit increment; clamped to {@code >= 1}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setUnitIncrement(final int increment) {
    this.unitIncrement = Math.max(1, increment);
  }

  /**
   * Returns the value change applied by Space+Arrow / Page keys — the block increment. Defaults to
   * one tenth of the range (at least {@code 1}) when never set explicitly.
   *
   * @return the block increment
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getBlockIncrement() {
    if (blockIncrement != null) {
      return blockIncrement;
    }
    return Math.max(1, (model.getMaximum() - model.getMinimum()) / 10);
  }

  /**
   * Sets the value change applied by Space+Arrow / Page keys.
   *
   * @param increment the block increment; clamped to {@code >= 1}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setBlockIncrement(final int increment) {
    this.blockIncrement = Math.max(1, increment);
  }

  /**
   * Returns the slider's accessible label, or {@code null} if none was set via {@link
   * #setLabel(String)}.
   *
   * @return the accessible label text, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the slider's accessible name — the adjacent UI label a screen reader reads before the role
   * and value (research §X #50). Alternatively, associate a {@link javax.swing.JLabel} via {@link
   * javax.swing.JLabel#setLabelFor} and the name is derived from it automatically; an explicit
   * value here takes precedence.
   *
   * @param label the accessible label, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setLabel(final String label) {
    this.label = label;
  }

  // -------------------------------------------------------------------- sizing

  private int bubbleReserveHeight() {
    return valueIndicatorEnabled ? VALUE_BUBBLE_HEIGHT_PX + VALUE_BUBBLE_GAP_PX : 0;
  }

  private int contentHeight() {
    return bubbleReserveHeight() + HANDLE_HEIGHT_PX;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(DEFAULT_TRACK_LENGTH_PX, contentHeight());
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(HANDLE_HEIGHT_PX, contentHeight());
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, contentHeight());
  }

  // ------------------------------------------------------------------ geometry

  /** The half-width used for handle travel — the resting pill half-width, stable across morphs. */
  private static int travelInset() {
    return HANDLE_WIDTH_PX / 2;
  }

  /** The x of the leftmost handle-center position (value == minimum). */
  private int trackStartX() {
    return travelInset();
  }

  /** The x of the rightmost handle-center position (value == maximum). */
  private int trackEndX() {
    return getWidth() - travelInset();
  }

  /**
   * The fraction {@code [0, 1]} the current value sits along the range (value-space, not pixels).
   */
  private float valueFraction() {
    final int range = model.getMaximum() - model.getMinimum();
    if (range <= 0) {
      return 0f;
    }
    return (model.getValue() - model.getMinimum()) / (float) range;
  }

  /** The pixel-space fraction, mirrored under a right-to-left component orientation. */
  private float pixelFraction() {
    return getComponentOrientation().isLeftToRight() ? valueFraction() : 1f - valueFraction();
  }

  /** The handle's center x for the current value; fill direction honors RTL. */
  int handleCenterX() {
    final int start = trackStartX();
    final int end = trackEndX();
    return Math.round(start + pixelFraction() * (end - start));
  }

  /**
   * Inverse of {@link #handleCenterX()} — the value at a given x, clamped into range; RTL-aware.
   */
  int valueForX(final int x) {
    final int start = trackStartX();
    final int end = trackEndX();
    if (end <= start) {
      return model.getMinimum();
    }
    float fraction = clampF((x - start) / (float) (end - start));
    if (!getComponentOrientation().isLeftToRight()) {
      fraction = 1f - fraction;
    }
    final int range = model.getMaximum() - model.getMinimum();
    return model.getMinimum() + Math.round(fraction * range);
  }

  /** The y of the handle band's top — the tall pill, below the reserved bubble band. */
  int handleTopY() {
    final int top = (getHeight() - contentHeight()) / 2;
    return Math.max(0, top) + bubbleReserveHeight();
  }

  /** The y of the track bar's top — centered on the handle band. */
  private int trackTopY() {
    return handleTopY() + (HANDLE_HEIGHT_PX - TRACK_HEIGHT_PX) / 2;
  }

  /** The current handle width, narrowed by the focus/press morph. */
  private float currentHandleWidth() {
    return lerp(HANDLE_WIDTH_PX, NARROW_HANDLE_WIDTH_PX, interactionAnimator.progress());
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int cx = handleCenterX();
      paintTrack(g2, cx);
      paintStateLayer(g2, cx);
      paintRipple(g2, cx);
      paintHandle(g2, cx);
      paintValueBubble(g2, cx);
    } finally {
      g2.dispose();
    }
  }

  private void paintTrack(final Graphics2D g2, final int cx) {
    final int trackTop = trackTopY();
    final int half = HANDLE_WIDTH_PX / 2;
    final int leftWidth = cx - half - HANDLE_TRACK_GAP_PX;
    final int rightStart = cx + half + HANDLE_TRACK_GAP_PX;
    final int rightEnd = getWidth();
    // The active segment grows from the origin end: LTR origin is the left edge, RTL the right —
    // so the left geometric segment is active in LTR, inactive in RTL (only the color swaps; the
    // geometry is identical). Outer (far) corners full-round, handle-facing inner corners squared.
    final boolean leftIsActive = getComponentOrientation().isLeftToRight();

    if (leftWidth > 0) {
      g2.setColor(trackColor(leftIsActive));
      g2.fill(trackSegment(0, trackTop, leftWidth, TRACK_OUTER_CORNER_PX, TRACK_INNER_CORNER_PX));
    }
    if (rightStart < rightEnd) {
      g2.setColor(trackColor(!leftIsActive));
      g2.fill(
          trackSegment(
              rightStart,
              trackTop,
              rightEnd - rightStart,
              TRACK_INNER_CORNER_PX,
              TRACK_OUTER_CORNER_PX));
    }
  }

  private void paintStateLayer(final Graphics2D g2, final int cx) {
    if (!isEnabled()) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      final RoundRectangle2D.Float halo = handleHalo(cx);
      final Color tint = ColorRole.PRIMARY.resolve();
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
    } finally {
      s.dispose();
    }
  }

  private void paintRipple(final Graphics2D g2, final int cx) {
    if (rippleOrigin == null || rippleProgress >= 1f || !isEnabled()) {
      return;
    }
    final RoundRectangle2D.Float halo = handleHalo(cx);
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(halo.x, halo.y);
      final Point local = new Point(rippleOrigin.x - (int) halo.x, rippleOrigin.y - (int) halo.y);
      RipplePainter.paint(
          r,
          (int) halo.width,
          (int) halo.height,
          local,
          rippleProgress,
          (int) (halo.width / 2f),
          ColorRole.PRIMARY.resolve());
    } finally {
      r.dispose();
    }
  }

  private void paintHandle(final Graphics2D g2, final int cx) {
    final int handleTop = handleTopY();
    final float width = currentHandleWidth();
    final float x = cx - width / 2f;
    g2.setColor(handleColor());
    g2.fill(new RoundRectangle2D.Float(x, handleTop, width, HANDLE_HEIGHT_PX, width, width));
  }

  private void paintValueBubble(final Graphics2D g2, final int cx) {
    if (!valueIndicatorEnabled) {
      return;
    }
    final float appear = interactionAnimator.progress();
    if (appear <= 0f) {
      return;
    }
    final float bubbleCenterX =
        clamp(
            cx,
            VALUE_BUBBLE_WIDTH_PX / 2,
            Math.max(VALUE_BUBBLE_WIDTH_PX / 2, getWidth() - VALUE_BUBBLE_WIDTH_PX / 2));
    final int nubTipY = handleTopY() - VALUE_BUBBLE_GAP_PX;
    final float scale = lerp(VALUE_BUBBLE_MIN_SCALE, 1f, appear);

    final Graphics2D b = (Graphics2D) g2.create();
    try {
      b.setComposite(AlphaComposite.SrcOver.derive(clampF(appear)));
      // Scale about the nub tip so the bubble grows out of the handle.
      b.translate(bubbleCenterX, nubTipY);
      b.scale(scale, scale);
      b.translate(-bubbleCenterX, -nubTipY);

      b.setColor(ColorRole.INVERSE_SURFACE.resolve());
      final Path2D.Float shape = valueBubbleShape(bubbleCenterX, nubTipY);
      b.fill(shape);

      b.setColor(ColorRole.INVERSE_ON_SURFACE.resolve());
      b.setFont(getFont().deriveFont(Font.PLAIN, VALUE_BUBBLE_LABEL_PT));
      final FontMetrics fm = b.getFontMetrics();
      final String text = valueText();
      final int bodyHeight = VALUE_BUBBLE_HEIGHT_PX - VALUE_BUBBLE_NUB_HEIGHT_PX;
      final int bodyTop = nubTipY - VALUE_BUBBLE_HEIGHT_PX;
      final int tx = Math.round(bubbleCenterX - fm.stringWidth(text) / 2f);
      final int ty = bodyTop + (bodyHeight - fm.getHeight()) / 2 + fm.getAscent();
      b.drawString(text, tx, ty);
    } finally {
      b.dispose();
    }
  }

  /** The text shown in the value bubble. Default is the raw value; a formatter hook lands in S4. */
  String valueText() {
    return Integer.toString(model.getValue());
  }

  /**
   * The rounded-body-plus-downward-nub bubble outline; nub tip sits at {@code (centerX, nubTipY)}.
   */
  private static Path2D.Float valueBubbleShape(final float centerX, final int nubTipY) {
    final float w = VALUE_BUBBLE_WIDTH_PX;
    final float bodyH = VALUE_BUBBLE_HEIGHT_PX - VALUE_BUBBLE_NUB_HEIGHT_PX;
    final float left = centerX - w / 2f;
    final float bodyBottom = nubTipY - VALUE_BUBBLE_NUB_HEIGHT_PX;
    final float top = bodyBottom - bodyH;
    final float arc = Math.min(w, bodyH) / 2f;
    final float nubHalf = 6f;

    final Path2D.Float p = new Path2D.Float();
    p.append(new RoundRectangle2D.Float(left, top, w, bodyH, arc, arc), false);
    final Path2D.Float nub = new Path2D.Float();
    nub.moveTo(centerX - nubHalf, bodyBottom);
    nub.lineTo(centerX + nubHalf, bodyBottom);
    nub.lineTo(centerX, nubTipY);
    nub.closePath();
    p.append(nub, false);
    return p;
  }

  private RoundRectangle2D.Float handleHalo(final int cx) {
    final int handleTop = handleTopY();
    final float x = cx - HANDLE_HALO_WIDTH_PX / 2f;
    return new RoundRectangle2D.Float(
        x,
        handleTop,
        HANDLE_HALO_WIDTH_PX,
        HANDLE_HEIGHT_PX,
        HANDLE_HALO_WIDTH_PX,
        HANDLE_HALO_WIDTH_PX);
  }

  /** A horizontal track segment with independent left/right corner radii (top == bottom). */
  static Path2D.Float trackSegment(
      final int x, final int y, final int width, final int leftRadius, final int rightRadius) {
    final float w = width;
    final float h = TRACK_HEIGHT_PX;
    final float lr = Math.min(leftRadius, Math.min(w / 2f, h / 2f));
    final float rr = Math.min(rightRadius, Math.min(w / 2f, h / 2f));
    final Path2D.Float p = new Path2D.Float();
    p.moveTo(x + lr, y);
    p.lineTo(x + w - rr, y);
    p.quadTo(x + w, y, x + w, y + rr);
    p.lineTo(x + w, y + h - rr);
    p.quadTo(x + w, y + h, x + w - rr, y + h);
    p.lineTo(x + lr, y + h);
    p.quadTo(x, y + h, x, y + h - lr);
    p.lineTo(x, y + lr);
    p.quadTo(x, y, x + lr, y);
    p.closePath();
    return p;
  }

  /** The active or inactive track color, honoring the disabled treatment. */
  private Color trackColor(final boolean active) {
    if (!isEnabled()) {
      final float opacity =
          active ? StateLayer.disabledContentOpacity() : StateLayer.disabledContainerOpacity();
      return withAlpha(ColorRole.ON_SURFACE.resolve(), opacity);
    }
    return (active ? ColorRole.PRIMARY : ColorRole.SECONDARY_CONTAINER).resolve();
  }

  /** The handle color, honoring the disabled treatment. */
  private Color handleColor() {
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
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            model.setValueIsAdjusting(true);
            setValue(valueForX(e.getX()));
            startRipple(new Point(handleCenterX(), handleCenterY()));
            updateInteractionAnimator();
            repaint();
          }

          @Override
          public void mouseDragged(final MouseEvent e) {
            if (!isEnabled() || !pressed) {
              return;
            }
            setValue(valueForX(e.getX()));
            repaint();
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed) {
              return;
            }
            pressed = false;
            model.setValueIsAdjusting(false);
            setCursor(
                hovered
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            updateInteractionAnimator();
            repaint();
          }
        };
    addMouseListener(ma);
    addMouseMotionListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            updateInteractionAnimator();
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            pressed = false;
            updateInteractionAnimator();
            repaint();
          }
        });
  }

  private int handleCenterY() {
    return handleTopY() + HANDLE_HEIGHT_PX / 2;
  }

  /** Drives the handle-narrow + value-bubble appearance toward active (focus or press) or rest. */
  private void updateInteractionAnimator() {
    if (pressed || isFocusOwner()) {
      interactionAnimator.start();
    } else {
      interactionAnimator.reverse();
    }
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
    interactionAnimator.stop();
    super.removeNotify();
  }

  // ------------------------------------------------------------------ keyboard

  private void initKeyboard() {
    final InputMap im = getInputMap(WHEN_FOCUSED);
    final ActionMap am = getActionMap();

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "elwhaSlider.left");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "elwhaSlider.right");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "elwhaSlider.decrease");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "elwhaSlider.increase");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "elwhaSlider.blockDown");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "elwhaSlider.blockUp");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "elwhaSlider.min");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "elwhaSlider.max");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "elwhaSlider.spaceDown");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "elwhaSlider.spaceUp");

    // Horizontal arrows mirror under RTL; vertical arrows do not. Space held promotes arrows to the
    // block increment (research §X #49 "Space & Arrows"); Page keys are the always-block
    // equivalent.
    am.put("elwhaSlider.left", step(() -> ltr() ? -stepAmount() : stepAmount()));
    am.put("elwhaSlider.right", step(() -> ltr() ? stepAmount() : -stepAmount()));
    am.put("elwhaSlider.increase", step(this::stepAmount));
    am.put("elwhaSlider.decrease", step(() -> -stepAmount()));
    am.put("elwhaSlider.blockUp", step(this::getBlockIncrement));
    am.put("elwhaSlider.blockDown", step(() -> -getBlockIncrement()));
    am.put("elwhaSlider.min", jumpTo(model.getMinimum(), true));
    am.put("elwhaSlider.max", jumpTo(model.getMaximum(), false));
    am.put("elwhaSlider.spaceDown", spaceState(true));
    am.put("elwhaSlider.spaceUp", spaceState(false));
  }

  private boolean ltr() {
    return getComponentOrientation().isLeftToRight();
  }

  /** The current arrow step — block while Space is held, otherwise the unit increment. */
  private int stepAmount() {
    return spaceDown ? getBlockIncrement() : effectiveUnitIncrement();
  }

  /**
   * The unit increment used by a single arrow; stops mode (story #345) overrides this to one stop.
   */
  int effectiveUnitIncrement() {
    return unitIncrement;
  }

  private AbstractAction step(final java.util.function.IntSupplier delta) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (isEnabled()) {
          setValue(ElwhaSlider.this.getValue() + delta.getAsInt());
        }
      }
    };
  }

  private AbstractAction jumpTo(final int target, final boolean toMin) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (isEnabled()) {
          setValue(toMin ? model.getMinimum() : model.getMaximum());
        }
      }
    };
  }

  private AbstractAction spaceState(final boolean down) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        spaceDown = down;
      }
    };
  }

  // ------------------------------------------------------------- accessibility

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaSlider();
    }
    return accessibleContext;
  }

  /**
   * Accessible context for the slider — reports {@link AccessibleRole#SLIDER}, exposes {@link
   * AccessibleValue} (current / min / max), and uses the {@link ElwhaSlider#setLabel(String) label}
   * (falling back to an associated {@code labelFor} {@link javax.swing.JLabel}) as the accessible
   * name so a screen reader announces label &rarr; role &rarr; value (research §X #50).
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  protected class AccessibleElwhaSlider extends AccessibleJComponent implements AccessibleValue {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.SLIDER;
    }

    @Override
    public String getAccessibleName() {
      if (label != null && !label.isEmpty()) {
        return label;
      }
      return super.getAccessibleName();
    }

    @Override
    public AccessibleValue getAccessibleValue() {
      return this;
    }

    @Override
    public Number getCurrentAccessibleValue() {
      return model.getValue();
    }

    @Override
    public boolean setCurrentAccessibleValue(final Number n) {
      if (n == null) {
        return false;
      }
      setValue(n.intValue());
      return true;
    }

    @Override
    public Number getMinimumAccessibleValue() {
      return model.getMinimum();
    }

    @Override
    public Number getMaximumAccessibleValue() {
      return model.getMaximum();
    }
  }

  // -------------------------------------------------------------------- helpers

  private static Color withAlpha(final Color base, final float opacity) {
    final int a = Math.round(clampF(opacity) * 255f);
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
  }

  private static float lerp(final float a, final float b, final float t) {
    return a + (b - a) * t;
  }

  private static float clampF(final float v) {
    return Math.max(0f, Math.min(1f, v));
  }

  private static int clamp(final int value, final int min, final int max) {
    return Math.max(min, Math.min(max, value));
  }
}
