package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

/**
 * The Elwha Material 3 <strong>Expressive slider</strong> — a token-themed range input painting M3
 * chrome: a split active/inactive track with the rounded-end gap, a tall pill handle, and (in later
 * stories) stop indicators and a value-indicator bubble.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-slider-design.md} §2).</strong> {@code ElwhaSlider} is one dedicated {@link JComponent}
 * that paints every M3 part itself, backed by a {@link BoundedRangeModel} ({@link
 * DefaultBoundedRangeModel}) for the value / min / max math. It is <em>not</em> a {@code JSlider}
 * subclass and <em>not</em> a {@code SliderUI} delegate: {@code BasicSliderUI}'s track/thumb layout
 * fights the M3 split-track-with-gap geometry, and the eventual range variant (two handles) is not
 * a {@code JSlider} concept — so a unified custom component keeps single and range on one paint /
 * interaction codebase. The keyboard map and {@link javax.accessibility.AccessibleValue} surface
 * are finite and fully specified (research §B / §X), so hand-wiring them once costs little.
 *
 * <p><strong>Phase-1 surface (this story arc).</strong> The {@code STANDARD} variant, horizontal,
 * {@code XS} size, continuous (later: {@code stops}), with an optional value indicator. Variant /
 * size / orientation axes are later V1 phases — the geometry constants below are the XS preset
 * (M3's only off-Android code preset; research §M / §Cfg).
 *
 * <p><strong>Geometry (XS, M3 token-locked — research §M / §T).</strong> Track {@value
 * #TRACK_HEIGHT_PX} dp tall; handle {@value #HANDLE_HEIGHT_PX}&times;{@value #HANDLE_WIDTH_PX} dp
 * flat pill (no shadow — M3 deprecates handle elevation); {@value #HANDLE_TRACK_GAP_PX} dp gap
 * between the handle and each track segment; track outer corner full / inner (gap-side) corner
 * ~{@value #TRACK_INNER_CORNER_PX} dp. Active and inactive track heights are always equal.
 *
 * <p><strong>Color (zero new tokens — research §Color).</strong> Active track + handle = {@link
 * ColorRole#PRIMARY}; inactive track = {@link ColorRole#SECONDARY_CONTAINER}; disabled = {@link
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

  /** Resting handle width — the tall pill; narrows to 2&nbsp;dp on focus/press in a later story. */
  static final int HANDLE_WIDTH_PX = 4;

  /** Gap between the handle and each adjacent track segment ({@code thumbTrackGapSize}). */
  static final int HANDLE_TRACK_GAP_PX = 6;

  /** Stop-indicator dot diameter (painted in the stops story). */
  static final int STOP_INDICATOR_SIZE_PX = 4;

  /** Track outer (far-end) corner radius — full round on the 16&nbsp;dp XS track. */
  static final int TRACK_OUTER_CORNER_PX = 8;

  /** Track inner (gap-side) corner radius — the squared-off end facing the handle. */
  static final int TRACK_INNER_CORNER_PX = 2;

  /** Default preferred track length when the layout gives the slider its preferred size. */
  static final int DEFAULT_TRACK_LENGTH_PX = 240;

  private final BoundedRangeModel model;
  private final ChangeListener modelListener = e -> repaint();

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
   * Adds a {@link ChangeListener} notified on every value change (including live changes mid-drag,
   * once interaction lands). Delegates to the backing model.
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

  // -------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(DEFAULT_TRACK_LENGTH_PX, HANDLE_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(HANDLE_HEIGHT_PX, HANDLE_HEIGHT_PX);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, HANDLE_HEIGHT_PX);
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

  /** The fraction {@code [0, 1]} the current value sits along the range. */
  private float valueFraction() {
    final int range = model.getMaximum() - model.getMinimum();
    if (range <= 0) {
      return 0f;
    }
    return (model.getValue() - model.getMinimum()) / (float) range;
  }

  /** The handle's center x for the current value, honoring left-to-right fill. */
  int handleCenterX() {
    final int start = trackStartX();
    final int end = trackEndX();
    return Math.round(start + valueFraction() * (end - start));
  }

  /** The y of the handle band's top — the tall pill is vertically centered in the component. */
  int handleTopY() {
    return (getHeight() - HANDLE_HEIGHT_PX) / 2;
  }

  /** The y of the track bar's top — centered on the handle band. */
  private int trackTopY() {
    return (getHeight() - TRACK_HEIGHT_PX) / 2;
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int cx = handleCenterX();
      paintTrack(g2, cx);
      paintHandle(g2, cx);
    } finally {
      g2.dispose();
    }
  }

  private void paintTrack(final Graphics2D g2, final int cx) {
    final int trackTop = trackTopY();
    final int half = HANDLE_WIDTH_PX / 2;
    final int activeRight = cx - half - HANDLE_TRACK_GAP_PX;
    final int inactiveLeft = cx + half + HANDLE_TRACK_GAP_PX;
    final int rightEnd = getWidth();

    // Active segment: leading edge → handle. Outer (leading) corner full, inner corner squared.
    if (activeRight > 0) {
      g2.setColor(trackColor(true));
      g2.fill(trackSegment(0, trackTop, activeRight, TRACK_OUTER_CORNER_PX, TRACK_INNER_CORNER_PX));
    }
    // Inactive segment: handle → trailing edge. Inner corner squared, outer (trailing) corner full.
    if (inactiveLeft < rightEnd) {
      g2.setColor(trackColor(false));
      g2.fill(
          trackSegment(
              inactiveLeft,
              trackTop,
              rightEnd - inactiveLeft,
              TRACK_INNER_CORNER_PX,
              TRACK_OUTER_CORNER_PX));
    }
  }

  private void paintHandle(final Graphics2D g2, final int cx) {
    final int handleTop = handleTopY();
    final int width = HANDLE_WIDTH_PX;
    final float x = cx - width / 2f;
    g2.setColor(handleColor());
    g2.fill(new RoundRectangle2D.Float(x, handleTop, width, HANDLE_HEIGHT_PX, width, width));
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

  private static Color withAlpha(final Color base, final float opacity) {
    final int a = Math.round(Math.max(0f, Math.min(1f, opacity)) * 255f);
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
  }

  private static int clamp(final int value, final int min, final int max) {
    return Math.max(min, Math.min(max, value));
  }
}
