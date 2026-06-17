package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

/**
 * The Elwha Material 3 Expressive <strong>loading indicator</strong> — a filled rounded-polygon
 * that continuously rotates while morphing through a sequence of M3 shape-library forms. For short
 * (&lt; ~5s) <em>indeterminate</em> waits — pull-to-refresh, inline-button loading — which M3 says
 * "should replace most uses of the indeterminate circular progress indicator." A separate component
 * from the {@linkplain com.owspfm.elwha.progress progress indicators}, not a variant of them.
 *
 * <p><strong>Shape engine.</strong> The package carries its own radius-profile shape-morph engine
 * ({@link RoundedPolygonShape} + {@link ShapeMorph}) — every M3 loading-indicator shape is
 * star-convex, so a shape is a function {@code r(θ)} and a morph is a per-angle lerp (design {@code
 * elwha-loading-indicator-design.md} §2). The continuous spin is a phase offset on the sampling
 * angle.
 *
 * <p><strong>Indeterminate choreography</strong> (design §6, faithful to the Compose constants):
 * the shape steps through the 7-shape loop every {@value #STEP_MS} ms (an {@code EMPHASIZED} settle
 * over the first {@value #MORPH_MS} ms, then a dwell), riding a constant {@value
 * #GLOBAL_ROTATION_MS} ms linear full-spin, with a +90° rotational kick committed per completed
 * morph. Derived from a wall-clock timeline, so it is reproducible for headless rendering.
 *
 * <p><strong>Animation clock.</strong> One {@link Timer} schedules ~60 fps repaints <em>only while
 * showing and indeterminate</em> (hierarchy-gated; stopped on {@link #removeNotify()}); {@linkplain
 * MorphAnimator#isReducedMotion() reduced motion} freezes the spinner on a static shape. The
 * indicator is non-interactive — not focusable, no hover/press.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaLoadingIndicator extends JComponent {

  /** The M3 {@code ActiveSize} — the active indicator's box, px. */
  public static final int INDICATOR_SIZE_DEFAULT_PX = 38;

  /** The M3 {@code ContainerSize} — the contained variant's container circle, px. */
  public static final int CONTAINER_SIZE_DEFAULT_PX = 48;

  /** The morph-step period — one shape advance per this interval, ms (M3 {@code MorphInterval}). */
  static final int STEP_MS = 650;

  /**
   * The eased morph settle within a step, ms (the Compose spring, deterministically interpreted).
   */
  static final int MORPH_MS = 450;

  /** The linear full-spin period, ms (M3 {@code GlobalRotationDuration}). */
  static final int GLOBAL_ROTATION_MS = 4666;

  /** The rotational kick committed per completed morph, degrees (M3 {@code QuarterRotation}). */
  static final double QUARTER_TURN_DEG = 90.0;

  private static final int CLOCK_FRAME_MS = 16;

  private int indicatorSize = INDICATOR_SIZE_DEFAULT_PX;
  private int containerSize = CONTAINER_SIZE_DEFAULT_PX;
  private ColorRole indicatorColorRole = ColorRole.PRIMARY;
  private ColorRole containerColorRole = ColorRole.PRIMARY_CONTAINER;
  private boolean indeterminate = true;
  private boolean contained;

  private final BoundedRangeModel model;
  private final Timer clock;
  private long cycleAnchorNanos;

  /**
   * A standard, indeterminate loading indicator over the default {@code [0, 100]} model.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaLoadingIndicator() {
    this(new DefaultBoundedRangeModel(0, 0, 0, 100));
  }

  /**
   * A loading indicator over a caller-supplied progress model (used in determinate mode; ignored
   * while indeterminate). Defaults to indeterminate mode.
   *
   * @param model the value model (never {@code null})
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaLoadingIndicator(final BoundedRangeModel model) {
    if (model == null) {
      throw new NullPointerException("model");
    }
    this.model = model;
    setOpaque(false);
    setFocusable(false);
    this.clock = new Timer(CLOCK_FRAME_MS, e -> repaint());
    this.clock.setRepeats(true);
    model.addChangeListener(e -> repaint());
    addHierarchyListener(
        e -> {
          if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            updateAnimationDemand();
          }
        });
  }

  /**
   * Factory — a contained loading indicator: the active shape ({@link
   * ColorRole#ON_PRIMARY_CONTAINER}) on a filled {@link ColorRole#PRIMARY_CONTAINER} container
   * circle (the M3 contained color pairing).
   *
   * @return the indicator
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaLoadingIndicator contained() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    indicator.contained = true;
    indicator.indicatorColorRole = ColorRole.ON_PRIMARY_CONTAINER;
    indicator.containerColorRole = ColorRole.PRIMARY_CONTAINER;
    return indicator;
  }

  /**
   * Factory — a standard determinate loading indicator: the shape morphs {@code Circle → SoftBurst}
   * as the progress fraction advances (M3 {@code DeterminateIndicatorPolygons}), with no continuous
   * spin.
   *
   * @return the indicator
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaLoadingIndicator determinate() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    indicator.setIndeterminate(false);
    return indicator;
  }

  /**
   * Factory — a determinate loading indicator over a caller-supplied model (the slider/progress
   * sharing precedent).
   *
   * @param model the value model (never {@code null})
   * @return the indicator
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaLoadingIndicator determinate(final BoundedRangeModel model) {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator(model);
    indicator.setIndeterminate(false);
    return indicator;
  }

  /**
   * Factory — a contained determinate loading indicator.
   *
   * @return the indicator
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaLoadingIndicator containedDeterminate() {
    final ElwhaLoadingIndicator indicator = contained();
    indicator.setIndeterminate(false);
    return indicator;
  }

  /**
   * Whether the indicator loops indeterminately (the default) rather than tracking a progress
   * value.
   *
   * @return {@code true} when indeterminate
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isIndeterminate() {
    return indeterminate;
  }

  /**
   * Sets indeterminate (looping) versus determinate (progress-driven) mode.
   *
   * @param value {@code true} for indeterminate
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setIndeterminate(final boolean value) {
    if (this.indeterminate == value) {
      return;
    }
    this.indeterminate = value;
    updateAnimationDemand();
    repaint();
  }

  /**
   * Whether the active shape sits on a filled container circle (the M3 <em>contained</em> variant).
   *
   * @return {@code true} when contained
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isContained() {
    return contained;
  }

  /**
   * Sets whether the active shape sits on a filled container circle. This toggles only whether the
   * container paints; it does <em>not</em> change the color roles. For the M3 contained color
   * pairing ({@link ColorRole#ON_PRIMARY_CONTAINER} on {@link ColorRole#PRIMARY_CONTAINER}) use the
   * {@link #contained()} factory.
   *
   * @param value {@code true} to paint the container circle
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setContained(final boolean value) {
    if (this.contained == value) {
      return;
    }
    this.contained = value;
    revalidate();
    repaint();
  }

  /**
   * The container circle's color role (contained variant).
   *
   * @return the container color role
   * @version v0.5.0
   * @since v0.5.0
   */
  public ColorRole getContainerColorRole() {
    return containerColorRole;
  }

  /**
   * Sets the container circle's color role.
   *
   * @param role the color role (never {@code null})
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setContainerColorRole(final ColorRole role) {
    if (role == null) {
      throw new NullPointerException("role");
    }
    this.containerColorRole = role;
    repaint();
  }

  /**
   * The active indicator's box size, px.
   *
   * @return the indicator size
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getIndicatorSize() {
    return indicatorSize;
  }

  /**
   * Sets the active indicator's box size.
   *
   * @param size the size, px (clamped to ≥ 8)
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setIndicatorSize(final int size) {
    this.indicatorSize = Math.max(8, size);
    revalidate();
    repaint();
  }

  /**
   * The container circle's box size, px (contained variant).
   *
   * @return the container size
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getContainerSize() {
    return containerSize;
  }

  /**
   * Sets the container circle's box size. The active shape holds the M3 active-to-container ratio
   * within it.
   *
   * @param size the size, px (clamped to ≥ 8)
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setContainerSize(final int size) {
    this.containerSize = Math.max(8, size);
    revalidate();
    repaint();
  }

  /**
   * The active indicator's color role.
   *
   * @return the indicator color role
   * @version v0.5.0
   * @since v0.5.0
   */
  public ColorRole getIndicatorColorRole() {
    return indicatorColorRole;
  }

  /**
   * Sets the active indicator's color role.
   *
   * @param role the color role (never {@code null})
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setIndicatorColorRole(final ColorRole role) {
    if (role == null) {
      throw new NullPointerException("role");
    }
    this.indicatorColorRole = role;
    repaint();
  }

  /**
   * The progress value model (drives determinate mode; ignored while indeterminate).
   *
   * @return the model
   * @version v0.5.0
   * @since v0.5.0
   */
  public BoundedRangeModel getModel() {
    return model;
  }

  /**
   * The model's current value.
   *
   * @return the value
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getValue() {
    return model.getValue();
  }

  /**
   * Sets the model's value (the model clamps into its range). Meaningful in determinate mode.
   *
   * @param value the new value
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setValue(final int value) {
    model.setValue(value);
  }

  /**
   * The model's minimum.
   *
   * @return the minimum
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getMinimum() {
    return model.getMinimum();
  }

  /**
   * Sets the model's minimum.
   *
   * @param minimum the new minimum
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setMinimum(final int minimum) {
    model.setMinimum(minimum);
  }

  /**
   * The model's maximum.
   *
   * @return the maximum
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getMaximum() {
    return model.getMaximum();
  }

  /**
   * Sets the model's maximum.
   *
   * @param maximum the new maximum
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setMaximum(final int maximum) {
    model.setMaximum(maximum);
  }

  /**
   * The progress fraction in {@code [0, 1]} (extent-aware) — the determinate morph and rotation are
   * driven by this.
   *
   * @return the clamped progress fraction
   * @version v0.5.0
   * @since v0.5.0
   */
  public float getProgressFraction() {
    final int span = model.getMaximum() - model.getMinimum() - model.getExtent();
    if (span <= 0) {
      return 0f;
    }
    final float f = (model.getValue() - model.getMinimum()) / (float) span;
    return Math.max(0f, Math.min(1f, f));
  }

  /**
   * Registers a listener notified when the value model changes.
   *
   * @param listener the listener
   * @version v0.5.0
   * @since v0.5.0
   */
  public void addChangeListener(final ChangeListener listener) {
    model.addChangeListener(listener);
  }

  /**
   * Removes a value-model change listener.
   *
   * @param listener the listener
   * @version v0.5.0
   * @since v0.5.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    model.removeChangeListener(listener);
  }

  /**
   * Whether the animation clock is currently scheduled.
   *
   * @return {@code true} while animating
   * @version v0.5.0
   * @since v0.5.0
   */
  protected final boolean isAnimating() {
    return clock.isRunning();
  }

  // ---- choreography (pure, deterministic from an elapsed-ms timeline) ----

  /**
   * The indeterminate morph profile at {@code elapsedMs} — the 7-shape loop, an {@code EMPHASIZED}
   * settle per step.
   *
   * @param elapsedMs the wall-clock ms since the timeline anchor
   * @return the interpolated radius profile
   * @version v0.5.0
   * @since v0.5.0
   */
  static float[] indeterminateProfile(final long elapsedMs) {
    final int n = LoadingShapes.INDETERMINATE.length;
    final long step = elapsedMs / STEP_MS;
    final int i = (int) (step % n);
    final int j = (i + 1) % n;
    return ShapeMorph.lerp(
        LoadingShapes.INDETERMINATE[i].radii(),
        LoadingShapes.INDETERMINATE[j].radii(),
        morphLocalT(elapsedMs));
  }

  /**
   * The indeterminate rotation at {@code elapsedMs} — a linear full-spin plus the per-step +90°
   * kicks plus the in-flight morph's 0→90° contribution, all clockwise.
   *
   * @param elapsedMs the wall-clock ms since the timeline anchor
   * @return the rotation, radians
   * @version v0.5.0
   * @since v0.5.0
   */
  static double indeterminateRotationRad(final long elapsedMs) {
    final long step = elapsedMs / STEP_MS;
    final double globalDeg = (elapsedMs / (double) GLOBAL_ROTATION_MS) * 360.0;
    final double deg =
        globalDeg + step * QUARTER_TURN_DEG + morphLocalT(elapsedMs) * QUARTER_TURN_DEG;
    return Math.toRadians(deg);
  }

  private static float morphLocalT(final long elapsedMs) {
    final float raw = Math.min(1f, (elapsedMs % STEP_MS) / (float) MORPH_MS);
    return Easing.EMPHASIZED.ease(raw);
  }

  /**
   * The determinate morph profile at progress {@code fraction} — {@code Circle → SoftBurst}.
   *
   * @param fraction the progress fraction in {@code [0, 1]}
   * @return the interpolated radius profile
   * @version v0.5.0
   * @since v0.5.0
   */
  static float[] determinateProfile(final float fraction) {
    return ShapeMorph.lerp(
        LoadingShapes.DETERMINATE[0].radii(), LoadingShapes.DETERMINATE[1].radii(), fraction);
  }

  /**
   * The determinate rotation at progress {@code fraction} — a single progress-mapped −180° sweep
   * (M3), no continuous spin.
   *
   * @param fraction the progress fraction in {@code [0, 1]}
   * @return the rotation, radians
   * @version v0.5.0
   * @since v0.5.0
   */
  static double determinateRotationRad(final float fraction) {
    return Math.toRadians(-fraction * 180.0);
  }

  /** The radius profile to paint this frame. */
  private float[] currentProfile() {
    if (!indeterminate) {
      return determinateProfile(getProgressFraction());
    }
    if (MorphAnimator.isReducedMotion()) {
      return LoadingShapes.INDETERMINATE[0].radii();
    }
    return indeterminateProfile(elapsedMs());
  }

  /** The rotation to apply this frame, radians. */
  private double currentRotationRad() {
    if (!indeterminate) {
      return determinateRotationRad(getProgressFraction());
    }
    if (MorphAnimator.isReducedMotion()) {
      return 0.0;
    }
    return indeterminateRotationRad(elapsedMs());
  }

  /** Wall-clock ms since the indeterminate timeline anchor (lazily set). */
  private long elapsedMs() {
    if (cycleAnchorNanos == 0L) {
      cycleAnchorNanos = System.nanoTime();
    }
    return (System.nanoTime() - cycleAnchorNanos) / 1_000_000L;
  }

  /** Starts/stops the repaint clock based on visibility, mode, and reduced motion. */
  private void updateAnimationDemand() {
    final boolean needed = isShowing() && indeterminate && !MorphAnimator.isReducedMotion();
    if (needed) {
      if (!clock.isRunning()) {
        if (cycleAnchorNanos == 0L) {
          cycleAnchorNanos = System.nanoTime();
        }
        clock.start();
      }
    } else if (clock.isRunning()) {
      clock.stop();
    }
  }

  /**
   * Preferred size — the indicator box plus insets, square.
   *
   * @return the preferred size
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final Insets in = getInsets();
    final int box = contained ? containerSize : indicatorSize;
    return new Dimension(box + in.left + in.right, box + in.top + in.bottom);
  }

  /**
   * Minimum size — same as preferred.
   *
   * @return the minimum size
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getMinimumSize() {
    return isMinimumSizeSet() ? super.getMinimumSize() : getPreferredSize();
  }

  /**
   * Maximum size — same as preferred (a fixed-size widget).
   *
   * @return the maximum size
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getMaximumSize() {
    return isMaximumSizeSet() ? super.getMaximumSize() : getPreferredSize();
  }

  /**
   * Paints the active indicator (design §2, §5–§6).
   *
   * @param g the graphics
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      final Insets in = getInsets();
      final float availW = getWidth() - in.left - in.right;
      final float availH = getHeight() - in.top - in.bottom;
      if (availW < 6f || availH < 6f) {
        return;
      }
      final float avail = Math.min(availW, availH);
      final float cx = in.left + availW / 2f;
      final float cy = in.top + availH / 2f;

      float activeBox = Math.min(avail, indicatorSize);
      if (contained) {
        final float containerDiameter = Math.min(avail, containerSize);
        g2.setColor(containerColorRole.resolve());
        g2.fill(
            new Ellipse2D.Float(
                cx - containerDiameter / 2f,
                cy - containerDiameter / 2f,
                containerDiameter,
                containerDiameter));
        // Hold the M3 38/48 active-to-container ratio even if the container was shrunk to fit.
        activeBox = containerDiameter * (indicatorSize / (float) containerSize);
      }

      final float scale = activeBox / 2f - 1f;
      if (scale <= 1f) {
        return;
      }
      final Path2D.Float path =
          ShapeMorph.toPath(currentProfile(), cx, cy, scale, currentRotationRad());
      g2.setColor(indicatorColorRole.resolve());
      g2.fill(path);
    } finally {
      g2.dispose();
    }
  }

  /**
   * Stops the clock with the component — no leaked timers when an animating indicator leaves the
   * hierarchy.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public void removeNotify() {
    super.removeNotify();
    clock.stop();
  }
}
