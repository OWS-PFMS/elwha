package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import java.awt.event.HierarchyEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

/**
 * The shared base of the Elwha Material 3 <strong>progress indicator</strong> family — value
 * model, mode, color roles, geometry knobs, and the animation clock that {@link
 * ElwhaLinearProgressIndicator} and {@link ElwhaCircularProgressIndicator} paint from.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-progress-indicator-design.md} §2).</strong> Progress indicators are dedicated {@link
 * JComponent}s backed by a {@link BoundedRangeModel} ({@link DefaultBoundedRangeModel}) — the
 * {@code ElwhaSlider} precedent — <em>not</em> {@code JProgressBar} subclasses and <em>not</em>
 * {@code ProgressBarUI} delegates: the updated-M3 anatomy (track-active gap, stop indicator,
 * wavy active path) has no counterpart in {@code BasicProgressBarUI}'s box-fill layout, and the
 * shared model means a consumer can drive a slider and a progress readout off one instance.
 *
 * <p><strong>Modes.</strong> Determinate (the default) fills toward {@link #getProgressFraction()}
 * of the run; {@linkplain #setIndeterminate(boolean) indeterminate} loops the current-M3
 * choreography (design §6). <strong>Shapes.</strong> {@linkplain #setWavy(boolean) Wavy} turns the
 * active indicator into a traveling sine wave (Expressive); the track always stays flat.
 *
 * <p><strong>Animation clock.</strong> One {@link Timer} ticks at ~60fps <em>only while
 * something animates</em> — indeterminate mode, a moving wave, or an amplitude transition — and
 * only while the component is showing (hierarchy-gated; stopped on {@link #removeNotify()}).
 * Determinate flat indicators never run the clock.
 *
 * <p>Progress indicators are <strong>non-interactive</strong>: no hover/focus/pressed states, not
 * focusable. Name the activity for assistive tech via {@code
 * getAccessibleContext().setAccessibleName("Download progress")}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public abstract class AbstractElwhaProgressIndicator extends JComponent implements Accessible {

  /** The M3 default track / active-indicator thickness, px. */
  public static final int TRACK_THICKNESS_DEFAULT_PX = 4;

  /** The M3 {@code TrackActiveSpace} — the gap between the active indicator head and the track. */
  public static final int TRACK_ACTIVE_SPACE_DEFAULT_PX = 4;

  /** Inner-end corner radius where active indicator and track face each other across the gap. */
  protected static final float INNER_CORNER_RADIUS_PX = 2f;

  /** Amplitude transitions (flat↔wavy, determinate ramp) run {@code DurationLong2} = 500ms. */
  protected static final int AMPLITUDE_TRANSITION_MS = 500;

  /** Determinate wave amplitude ramps to zero at or below this progress fraction. */
  protected static final float AMPLITUDE_RAMP_MIN_FRACTION = 0.10f;

  /** Determinate wave amplitude ramps to zero at or above this progress fraction. */
  protected static final float AMPLITUDE_RAMP_MAX_FRACTION = 0.95f;

  private static final int CLOCK_FRAME_MS = 16;
  private static final float WAVE_SPEED_AUTO = -1f;

  private final BoundedRangeModel model;
  private final int specWavelengthDeterminate;
  private final int specWavelengthIndeterminate;
  private final float specWaveAmplitude;

  private final Timer clock;

  private boolean indeterminate;
  private boolean wavy;
  private ColorRole indicatorColorRole = ColorRole.PRIMARY;
  private ColorRole trackColorRole = ColorRole.SECONDARY_CONTAINER;
  private int trackThickness = TRACK_THICKNESS_DEFAULT_PX;
  private int indicatorTrackGapSize = TRACK_ACTIVE_SPACE_DEFAULT_PX;

  private float waveAmplitude;
  private int wavelengthDeterminate;
  private int wavelengthIndeterminate;
  private float waveSpeed = WAVE_SPEED_AUTO;

  private long cycleAnchorNanos;
  private long lastTickNanos;
  private float wavePhasePx;
  private float amplitudeFraction;
  private float amplitudeAnimFrom;
  private float amplitudeAnimTarget;
  private long amplitudeAnimStartNanos = -1L;

  /**
   * Base wiring for a concrete indicator.
   *
   * @param model the value model (never {@code null})
   * @param specWavelengthDeterminate the variant's M3 determinate wavelength, px
   * @param specWavelengthIndeterminate the variant's M3 indeterminate wavelength, px
   * @param specWaveAmplitude the variant's M3 wave amplitude, px
   * @version v0.4.0
   * @since v0.4.0
   */
  protected AbstractElwhaProgressIndicator(
      final BoundedRangeModel model,
      final int specWavelengthDeterminate,
      final int specWavelengthIndeterminate,
      final float specWaveAmplitude) {
    this.model = model;
    this.specWavelengthDeterminate = specWavelengthDeterminate;
    this.specWavelengthIndeterminate = specWavelengthIndeterminate;
    this.specWaveAmplitude = specWaveAmplitude;
    this.wavelengthDeterminate = specWavelengthDeterminate;
    this.wavelengthIndeterminate = specWavelengthIndeterminate;
    this.waveAmplitude = specWaveAmplitude;
    this.clock = new Timer(CLOCK_FRAME_MS, e -> tick());
    this.clock.setRepeats(true);
    setOpaque(false);
    setFocusable(false);
    model.addChangeListener(e -> onModelChange());
    addHierarchyListener(
        e -> {
          if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            updateAnimationDemand();
          }
        });
  }

  /**
   * The value model backing this indicator.
   *
   * @return the model
   * @version v0.4.0
   * @since v0.4.0
   */
  public BoundedRangeModel getModel() {
    return model;
  }

  /**
   * The model's current value.
   *
   * @return the value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getValue() {
    return model.getValue();
  }

  /**
   * Sets the model's value (the model clamps into its range).
   *
   * @param value the new value
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setValue(final int value) {
    model.setValue(value);
  }

  /**
   * The model's minimum.
   *
   * @return the minimum
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getMinimum() {
    return model.getMinimum();
  }

  /**
   * Sets the model's minimum.
   *
   * @param minimum the new minimum
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setMinimum(final int minimum) {
    model.setMinimum(minimum);
  }

  /**
   * The model's maximum.
   *
   * @return the maximum
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getMaximum() {
    return model.getMaximum();
  }

  /**
   * Sets the model's maximum.
   *
   * @param maximum the new maximum
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setMaximum(final int maximum) {
    model.setMaximum(maximum);
  }

  /**
   * Registers a listener on the value model.
   *
   * @param listener the listener
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addChangeListener(final ChangeListener listener) {
    model.addChangeListener(listener);
  }

  /**
   * Removes a model listener.
   *
   * @param listener the listener
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    model.removeChangeListener(listener);
  }

  /**
   * Whether the indicator loops the indeterminate choreography instead of filling toward the
   * model value.
   *
   * @return {@code true} when indeterminate
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isIndeterminate() {
    return indeterminate;
  }

  /**
   * Switches between determinate and indeterminate modes. The model is untouched — flipping back
   * resumes at the current value.
   *
   * @param indeterminate {@code true} for indeterminate
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setIndeterminate(final boolean indeterminate) {
    if (this.indeterminate == indeterminate) {
      return;
    }
    this.indeterminate = indeterminate;
    cycleAnchorNanos = 0L;
    updateAnimationDemand();
    repaint();
  }

  /**
   * The model value as a clamped {@code [0, 1]} fraction of the filled range. Honors the model's
   * extent ({@code value} can only reach {@code max - extent}); a degenerate range reads as zero.
   *
   * @return the progress fraction
   * @version v0.4.0
   * @since v0.4.0
   */
  public float getProgressFraction() {
    final int span = model.getMaximum() - model.getMinimum() - model.getExtent();
    if (span <= 0) {
      return 0f;
    }
    final float fraction = (model.getValue() - model.getMinimum()) / (float) span;
    return Math.max(0f, Math.min(1f, fraction));
  }

  /**
   * The color role of the active indicator (M3: {@code primary}).
   *
   * @return the active-indicator role
   * @version v0.4.0
   * @since v0.4.0
   */
  public ColorRole getIndicatorColorRole() {
    return indicatorColorRole;
  }

  /**
   * Re-roles the active indicator.
   *
   * @param role the new role (never {@code null})
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setIndicatorColorRole(final ColorRole role) {
    this.indicatorColorRole = role;
    repaint();
  }

  /**
   * The color role of the track (M3: {@code secondaryContainer}).
   *
   * @return the track role
   * @version v0.4.0
   * @since v0.4.0
   */
  public ColorRole getTrackColorRole() {
    return trackColorRole;
  }

  /**
   * Re-roles the track.
   *
   * @param role the new role (never {@code null})
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTrackColorRole(final ColorRole role) {
    this.trackColorRole = role;
    repaint();
  }

  /**
   * The track / active-indicator thickness, px (M3 default 4; the spec redlines 8 as the "thick"
   * reference).
   *
   * @return the thickness
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getTrackThickness() {
    return trackThickness;
  }

  /**
   * Sets the track / active-indicator thickness. Preferred size follows (linear height, circular
   * diameter), so this revalidates.
   *
   * @param thickness the thickness, px (clamped to ≥ 1)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTrackThickness(final int thickness) {
    this.trackThickness = Math.max(1, thickness);
    revalidate();
    repaint();
  }

  /**
   * The gap between the active indicator's head and the track (M3 {@code TrackActiveSpace}, 4px).
   *
   * @return the gap, px
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getIndicatorTrackGapSize() {
    return indicatorTrackGapSize;
  }

  /**
   * Sets the active-indicator↔track gap.
   *
   * @param gapSize the gap, px (clamped to ≥ 0)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setIndicatorTrackGapSize(final int gapSize) {
    this.indicatorTrackGapSize = Math.max(0, gapSize);
    repaint();
  }

  /**
   * Whether the active indicator paints as the Expressive traveling sine wave.
   *
   * @return {@code true} when wavy
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isWavy() {
    return wavy;
  }

  /**
   * Switches the active indicator between flat and wavy. The amplitude transition animates (500ms
   * — design §6); preferred size grows to reserve the wave band, so this revalidates.
   *
   * @param wavy {@code true} for the wavy shape
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setWavy(final boolean wavy) {
    if (this.wavy == wavy) {
      return;
    }
    this.wavy = wavy;
    updateAnimationDemand();
    revalidate();
    repaint();
  }

  /**
   * The wave amplitude, px — the M3 spec value for the variant (linear 3, circular 1.6) unless
   * overridden.
   *
   * @return the amplitude, px
   * @version v0.4.0
   * @since v0.4.0
   */
  public float getWaveAmplitude() {
    return waveAmplitude;
  }

  /**
   * Overrides the wave amplitude.
   *
   * @param amplitude the amplitude, px (clamped to ≥ 0)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setWaveAmplitude(final float amplitude) {
    this.waveAmplitude = Math.max(0f, amplitude);
    revalidate();
    repaint();
  }

  /**
   * The determinate-mode wavelength, px.
   *
   * @return the wavelength
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getWavelengthDeterminate() {
    return wavelengthDeterminate;
  }

  /**
   * Sets the determinate-mode wavelength.
   *
   * @param wavelength the wavelength, px (clamped to ≥ 1)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setWavelengthDeterminate(final int wavelength) {
    this.wavelengthDeterminate = Math.max(1, wavelength);
    repaint();
  }

  /**
   * The indeterminate-mode wavelength, px.
   *
   * @return the wavelength
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getWavelengthIndeterminate() {
    return wavelengthIndeterminate;
  }

  /**
   * Sets the indeterminate-mode wavelength.
   *
   * @param wavelength the wavelength, px (clamped to ≥ 1)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setWavelengthIndeterminate(final int wavelength) {
    this.wavelengthIndeterminate = Math.max(1, wavelength);
    repaint();
  }

  /**
   * Convenience — sets both mode wavelengths at once (the MDC {@code wavelength} attribute; the
   * per-mode setters mirror {@code wavelengthDeterminate} / {@code wavelengthIndeterminate}).
   *
   * @param wavelength the wavelength, px (clamped to ≥ 1)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setWavelength(final int wavelength) {
    setWavelengthDeterminate(wavelength);
    setWavelengthIndeterminate(wavelength);
  }

  /**
   * The effective wave travel speed, px/s. Defaults to <em>one wavelength per second</em> for the
   * current mode until {@linkplain #setWaveSpeed(float) overridden}.
   *
   * @return the speed, px/s
   * @version v0.4.0
   * @since v0.4.0
   */
  public float getWaveSpeed() {
    if (waveSpeed >= 0f) {
      return waveSpeed;
    }
    return currentWavelength();
  }

  /**
   * Overrides the wave travel speed. Zero freezes the wave in place; a negative value restores the
   * one-wavelength-per-second auto default.
   *
   * @param pxPerSecond the speed, px/s
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setWaveSpeed(final float pxPerSecond) {
    this.waveSpeed = pxPerSecond < 0f ? WAVE_SPEED_AUTO : pxPerSecond;
    updateAnimationDemand();
  }

  /**
   * The wavelength for the current mode.
   *
   * @return the wavelength, px
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final int currentWavelength() {
    return indeterminate ? wavelengthIndeterminate : wavelengthDeterminate;
  }

  /**
   * The current traveling-wave phase offset, px (advanced by the clock at {@link #getWaveSpeed()}).
   *
   * @return the phase offset
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final float wavePhasePx() {
    return wavePhasePx;
  }

  /**
   * The animated wave-amplitude fraction in {@code [0, 1]} — ramps with determinate progress
   * (zero outside {@code (0.10, 0.95)}) and crossfades flat↔wavy (design §6).
   *
   * @return the amplitude fraction
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final float amplitudeFraction() {
    return amplitudeFraction;
  }

  /**
   * Position inside a looping indeterminate timeline.
   *
   * @param cycleMs the cycle length, ms
   * @return the cycle position in {@code [0, 1)}
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final float indeterminateCycleT(final int cycleMs) {
    if (cycleAnchorNanos == 0L) {
      cycleAnchorNanos = System.nanoTime();
    }
    final long elapsedMs = (System.nanoTime() - cycleAnchorNanos) / 1_000_000L;
    return (elapsedMs % cycleMs) / (float) cycleMs;
  }

  /**
   * Milliseconds elapsed since the indeterminate timeline was (re)anchored — for choreography that
   * spans multiple cycles.
   *
   * @return elapsed ms
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final long indeterminateElapsedMs() {
    if (cycleAnchorNanos == 0L) {
      cycleAnchorNanos = System.nanoTime();
    }
    return (System.nanoTime() - cycleAnchorNanos) / 1_000_000L;
  }

  /**
   * Whether the active indicator currently needs per-frame repaints — indeterminate mode, a
   * traveling wave, or an in-flight amplitude transition.
   *
   * @return {@code true} when animating
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final boolean isAnimating() {
    return clock.isRunning();
  }

  /** Recomputes whether the clock should run and starts/stops it. */
  final void updateAnimationDemand() {
    final boolean waveMoving = wavy && getWaveSpeed() > 0f && amplitudeTargetNow() > 0f;
    final boolean transition =
        amplitudeAnimStartNanos >= 0L || Math.abs(amplitudeFraction - amplitudeTargetNow()) > 0.001f;
    final boolean needed = isShowing() && (indeterminate || waveMoving || transition);
    if (needed) {
      if (!clock.isRunning()) {
        lastTickNanos = System.nanoTime();
        startAmplitudeAnimationIfNeeded();
        clock.start();
      }
    } else {
      if (clock.isRunning()) {
        clock.stop();
        cycleAnchorNanos = 0L;
      }
      amplitudeFraction = amplitudeTargetNow();
      amplitudeAnimStartNanos = -1L;
    }
  }

  private float amplitudeTargetNow() {
    if (!wavy) {
      return 0f;
    }
    if (indeterminate) {
      return 1f;
    }
    final float f = getProgressFraction();
    return (f <= AMPLITUDE_RAMP_MIN_FRACTION || f >= AMPLITUDE_RAMP_MAX_FRACTION) ? 0f : 1f;
  }

  private void startAmplitudeAnimationIfNeeded() {
    final float target = amplitudeTargetNow();
    if (Math.abs(target - amplitudeFraction) > 0.001f
        && (amplitudeAnimStartNanos < 0L || Math.abs(target - amplitudeAnimTarget) > 0.001f)) {
      amplitudeAnimFrom = amplitudeFraction;
      amplitudeAnimTarget = target;
      amplitudeAnimStartNanos = System.nanoTime();
    }
  }

  private void tick() {
    final long now = System.nanoTime();
    final float dtSeconds = (now - lastTickNanos) / 1_000_000_000f;
    lastTickNanos = now;

    final float speed = getWaveSpeed();
    if (wavy && speed > 0f) {
      final int wavelength = Math.max(1, currentWavelength());
      wavePhasePx = (wavePhasePx + speed * dtSeconds) % wavelength;
    }

    startAmplitudeAnimationIfNeeded();
    if (amplitudeAnimStartNanos >= 0L) {
      final float t =
          Math.min(1f, (now - amplitudeAnimStartNanos) / 1_000_000f / AMPLITUDE_TRANSITION_MS);
      final Easing easing =
          amplitudeAnimTarget > amplitudeAnimFrom ? Easing.STANDARD : Easing.EMPHASIZED_ACCELERATE;
      amplitudeFraction =
          amplitudeAnimFrom + (amplitudeAnimTarget - amplitudeAnimFrom) * easing.ease(t);
      if (t >= 1f) {
        amplitudeFraction = amplitudeAnimTarget;
        amplitudeAnimStartNanos = -1L;
      }
    }

    repaint();
    if (!indeterminate) {
      updateAnimationDemand();
    }
  }

  private void onModelChange() {
    if (clock.isRunning()) {
      startAmplitudeAnimationIfNeeded();
    } else {
      updateAnimationDemand();
      if (!clock.isRunning()) {
        amplitudeFraction = amplitudeTargetNow();
      }
    }
    if (accessibleContext != null && !indeterminate) {
      accessibleContext.firePropertyChange(
          AccessibleContext.ACCESSIBLE_VALUE_PROPERTY, null, model.getValue());
    }
    repaint();
  }

  /**
   * The accessible context — role {@link AccessibleRole#PROGRESS_BAR} with an {@link
   * AccessibleValue} backed by the model; the value reads {@code null} while indeterminate (an
   * indeterminate indicator has no meaningful progress to report). Name the activity via {@code
   * getAccessibleContext().setAccessibleName("Download progress")}.
   *
   * @return the accessible context
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaProgressIndicator();
    }
    return accessibleContext;
  }

  /**
   * The {@link AccessibleContext} implementation for progress indicators — mirrors {@code
   * JProgressBar.AccessibleJProgressBar} (role, model-backed value, extent-aware maximum) except
   * that the current value is withheld while {@linkplain #isIndeterminate() indeterminate}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  protected class AccessibleElwhaProgressIndicator extends AccessibleJComponent
      implements AccessibleValue {

    /**
     * Creates the context.
     *
     * @version v0.4.0
     * @since v0.4.0
     */
    protected AccessibleElwhaProgressIndicator() {}

    /**
     * The role — {@link AccessibleRole#PROGRESS_BAR}.
     *
     * @return the role
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.PROGRESS_BAR;
    }

    /**
     * Adds {@link AccessibleState#BUSY} while indeterminate.
     *
     * @return the state set
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public AccessibleStateSet getAccessibleStateSet() {
      final AccessibleStateSet states = super.getAccessibleStateSet();
      if (isIndeterminate()) {
        states.add(AccessibleState.BUSY);
      }
      return states;
    }

    /**
     * This context is its own value model.
     *
     * @return {@code this}
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public AccessibleValue getAccessibleValue() {
      return this;
    }

    /**
     * The model value — {@code null} while indeterminate.
     *
     * @return the current value, or {@code null}
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public Number getCurrentAccessibleValue() {
      return isIndeterminate() ? null : getModel().getValue();
    }

    /**
     * Sets the model value.
     *
     * @param n the new value
     * @return {@code true} when applied; {@code false} for {@code null}
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public boolean setCurrentAccessibleValue(final Number n) {
      if (n == null) {
        return false;
      }
      setValue(n.intValue());
      return true;
    }

    /**
     * The model minimum.
     *
     * @return the minimum
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public Number getMinimumAccessibleValue() {
      return getModel().getMinimum();
    }

    /**
     * The model maximum less the extent (the largest reachable value).
     *
     * @return the maximum
     * @version v0.4.0
     * @since v0.4.0
     */
    @Override
    public Number getMaximumAccessibleValue() {
      return getModel().getMaximum() - getModel().getExtent();
    }
  }

  /**
   * Stops the animation clock with the component — no leaked timers when an animating indicator
   * leaves the hierarchy.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public void removeNotify() {
    super.removeNotify();
    clock.stop();
  }

  /**
   * The variant's M3 spec amplitude, px (linear 3, circular 1.6).
   *
   * @return the spec amplitude
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final float specWaveAmplitude() {
    return specWaveAmplitude;
  }

  /**
   * The variant's M3 spec determinate wavelength, px.
   *
   * @return the spec wavelength
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final int specWavelengthDeterminate() {
    return specWavelengthDeterminate;
  }

  /**
   * The variant's M3 spec indeterminate wavelength, px.
   *
   * @return the spec wavelength
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final int specWavelengthIndeterminate() {
    return specWavelengthIndeterminate;
  }
}
