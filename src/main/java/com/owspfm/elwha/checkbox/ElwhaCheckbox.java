package com.owspfm.elwha.checkbox;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

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
 * Every state change fires a {@link #PROPERTY_CHECK_STATE} property-change event.
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

  private CheckState checkState = CheckState.UNCHECKED;

  /**
   * Creates an unchecked, enabled checkbox.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaCheckbox() {
    setOpaque(false);
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
   * toggles).
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
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      final int cx = TOUCH_TARGET / 2;
      final int cy = getHeight() / 2;
      paintContainer(g2, cx, cy);
    } finally {
      g2.dispose();
    }
  }

  /** Paints the container (outline or fill) and the mark, centered at {@code (cx, cy)}. */
  private void paintContainer(final Graphics2D g2, final int cx, final int cy) {
    final float bx = cx - CONTAINER_SIZE / 2f;
    final float by = cy - CONTAINER_SIZE / 2f;
    if (checkState == CheckState.UNCHECKED) {
      g2.setColor(outlineColor());
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
      return;
    }
    g2.setColor(containerFillColor());
    g2.fill(
        new RoundRectangle2D.Float(
            bx, by, CONTAINER_SIZE, CONTAINER_SIZE, CONTAINER_ARC, CONTAINER_ARC));
    g2.setColor(markColor());
    g2.setStroke(new BasicStroke(MARK_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    if (checkState == CheckState.CHECKED) {
      g2.draw(checkmarkPath(bx, by, 1f));
    } else {
      g2.draw(dashPath(bx, by, 1f));
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

  /** Unchecked outline color per the research §B table (disabled = {@code ON_SURFACE} @ 0.38). */
  private Color outlineColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
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

  private static Color withAlpha(final Color base, final float alpha) {
    return new Color(
        base.getRed(), base.getGreen(), base.getBlue(), Math.round(Math.min(1f, alpha) * 255f));
  }
}
