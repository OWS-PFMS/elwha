package com.owspfm.elwha.slider;

import com.formdev.flatlaf.extras.FlatSVGIcon;
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
import java.util.logging.Logger;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

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
 * <p><strong>Variants (design doc §1 / §11).</strong> {@link Variant#STANDARD} fills the active
 * track from the leading edge to the handle; {@link Variant#CENTERED} fills from a fixed
 * {@linkplain #getOrigin() origin} (range midpoint, or zero when {@code 0} is in range) outward to
 * the handle in either direction — for a positive/negative range with the default in the middle.
 * {@link Variant#RANGE} adds a second handle and fills the active track <em>between</em> the two,
 * selecting a {@code [lower, upper]} sub-span (build one with {@link #range(int, int, int, int)}).
 * The {@linkplain Size size} axis ({@link #setSizeVariant(Size)}) scales the chrome across the M3
 * {@code XS}&ndash;{@code XL} preset table; the orientation axis is a later V1 phase. The geometry
 * constants below are the {@code XS} (default) preset (M3's only off-Android code preset; §M /
 * §Cfg).
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

  /**
   * How the active track fills relative to the handle.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public enum Variant {
    /** The active track fills from the leading edge to the handle (the default). */
    STANDARD,
    /**
     * The active track fills from a fixed {@linkplain ElwhaSlider#getOrigin() origin} outward to
     * the handle — leftward when the value is below the origin, rightward when above — leaving
     * inactive track on both outer sides. For a positive/negative range with the default in the
     * middle.
     */
    CENTERED,
    /**
     * Two handles select a {@code [lower, upper]} sub-span: the active track fills <em>between</em>
     * the two handles, with inactive track on both outer sides. Backed by the {@linkplain
     * ElwhaSlider#getLowerValue() lower} / {@linkplain ElwhaSlider#getUpperValue() upper} values
     * over the same {@code [min, max]}; build one with {@link ElwhaSlider#range(int, int, int,
     * int)}.
     */
    RANGE
  }

  /**
   * The axis along which the slider's track runs and the handle travels (research §A / §GD4).
   *
   * <p>{@link #HORIZONTAL} is the default and matches Phases 1&ndash;4: the active fill grows from
   * the leading ({@linkplain java.awt.ComponentOrientation#isLeftToRight() orientation-aware}) end
   * toward the handle. {@link #VERTICAL} is the M3 Expressive transposition: a tall track whose
   * active fill grows <strong>bottom-up</strong> (from the minimum/bottom end toward the handle),
   * with a horizontal pill handle. Vertical is <em>not</em> right-to-left mirrored — it always
   * fills bottom-up regardless of {@code ComponentOrientation}.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public enum Orientation {
    /** The track runs left&harr;right; the active fill honors right-to-left mirroring. */
    HORIZONTAL,
    /** The track runs top&harr;bottom; the active fill grows bottom-up (never RTL-mirrored). */
    VERTICAL
  }

  /**
   * Which of a {@link Variant#RANGE} slider's two handles a per-handle operation targets.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public enum Handle {
    /** The lower-value (leading) handle. */
    LOWER,
    /** The upper-value (trailing) handle. */
    UPPER
  }

  /**
   * The slider's size preset — the M3 track-thickness scale {@code XS} &le; {@code S} &le; {@code
   * M} &le; {@code L} &le; {@code XL} (research §M / §GD5). Each size scales the three geometry
   * values the M3 measurements table varies — {@linkplain #trackHeight() track height}, {@linkplain
   * #handleHeight() handle height}, and the {@linkplain #outerCorner() outer corner radius} — plus
   * the {@linkplain #insetIconSize() inset-icon size}; the handle width, handle&harr;track gap,
   * stop dot, inner corner, and value bubble are constant across sizes (M3 §M). {@code XS} is the
   * default and M3's only off-Android code preset; larger sizes give a bigger touch target and more
   * visual emphasis ({@code XL} for hero moments).
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public enum Size {
    /** Extra-small — track 16, handle 44, corner 8; the default. No inset icon. */
    XS(16, 44, 8, 0),
    /** Small — track 24, handle 44, corner 8. No inset icon. */
    S(24, 44, 8, 0),
    /** Medium — track 40, handle 52, corner 12; inset icon 24 (the &ge;40&nbsp;dp floor). */
    M(40, 52, 12, 24),
    /** Large — track 56, handle 68, corner 16; inset icon 24. */
    L(56, 68, 16, 24),
    /** Extra-large — track 96, handle 108, corner 28; inset icon 32. For hero moments. */
    XL(96, 108, 28, 32);

    private final int trackHeight;
    private final int handleHeight;
    private final int outerCorner;
    private final int insetIconSize;

    Size(final int trackHeight, final int handleHeight, final int outerCorner, final int icon) {
      this.trackHeight = trackHeight;
      this.handleHeight = handleHeight;
      this.outerCorner = outerCorner;
      this.insetIconSize = icon;
    }

    /**
     * Whether this size admits an inset icon (M/L/XL — a track &ge;&nbsp;40&nbsp;dp; research
     * §GD4).
     */
    boolean allowsInsetIcon() {
      return insetIconSize > 0;
    }
  }

  // --- XS geometry preset; the XS row of the {@link Size} table (dp == px at 100% scale; §M / §T)
  // ---

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

  private static final Logger LOG = Logger.getLogger(ElwhaSlider.class.getName());

  private static final int VALUE_BUBBLE_NUB_HEIGHT_PX = 8;
  private static final int HANDLE_MORPH_MS = MorphAnimator.SHORT3_MS;
  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final float VALUE_BUBBLE_MIN_SCALE = 0.85f;
  private static final float VALUE_BUBBLE_LABEL_PT = 14f;

  private final BoundedRangeModel model;
  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);
  private final ChangeListener modelListener =
      e -> {
        repaint();
        fireStateChanged();
      };

  private boolean hovered;
  private boolean pressed;
  private boolean valueIndicatorEnabled;
  private boolean spaceDown;
  private boolean endStopsVisible = true;
  private Variant variant = Variant.STANDARD;
  private Size sizeVariant = Size.XS;
  private Orientation orientation = Orientation.HORIZONTAL;
  private boolean verticalRangeWarned;
  private Integer originOverride;

  private Icon insetIcon;
  private FlatSVGIcon insetIconThemed;
  private Color insetIconTint;
  private boolean insetIconNoOpWarned;

  private int lowerValue;
  private int upperValue;
  Handle activeHandle = Handle.LOWER;
  Handle focusedHandle = Handle.LOWER;
  private Handle hoveredHandle;
  private final RangeHandle lowerFocus = new RangeHandle(Handle.LOWER);
  private final RangeHandle upperFocus = new RangeHandle(Handle.UPPER);

  private int unitIncrement = 1;
  private int stopStep;
  private Integer blockIncrement;
  private String label;
  private java.util.function.IntFunction<String> valueFormatter;

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
    this.lowerValue = model.getMinimum();
    this.upperValue = model.getMaximum();
    setOpaque(false);
    setFocusable(true);
    initInteraction();
    initKeyboard();
  }

  /**
   * Creates a {@link Variant#RANGE} slider over {@code [min, max]} with the two handles at {@code
   * lower} / {@code upper}. The values are clamped into {@code [min, max]} and to each other
   * ({@code lower <= upper}).
   *
   * @param min the range lower bound
   * @param max the range upper bound
   * @param lower the initial lower-handle value
   * @param upper the initial upper-handle value
   * @return a new range slider
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaSlider range(final int min, final int max, final int lower, final int upper) {
    final ElwhaSlider slider = new ElwhaSlider(min, max, clamp(lower, min, max));
    slider.lowerValue = clamp(lower, min, max);
    slider.upperValue = clamp(upper, min, max);
    slider.setVariant(Variant.RANGE);
    return slider;
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
    final int snapped = isStopsEnabled() ? snap(value) : value;
    model.setValue(clamp(snapped, model.getMinimum(), model.getMaximum()));
  }

  /** Rounds a value to the nearest stop (measured from the minimum), clamped into range. */
  private int snap(final int value) {
    if (stopStep <= 0) {
      return value;
    }
    final int min = model.getMinimum();
    final int steps = Math.round((value - min) / (float) stopStep);
    return clamp(min + steps * stopStep, min, model.getMaximum());
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

  // ------------------------------------------------------------ range value API

  /**
   * Returns the {@link Variant#RANGE} lower-handle value, clamped into {@code [min, max]} and never
   * above the {@linkplain #getUpperValue() upper} value. Meaningful only for the range variant.
   *
   * @return the lower-handle value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getLowerValue() {
    return clamp(lowerValue, getMinimum(), getMaximum());
  }

  /**
   * Returns the {@link Variant#RANGE} upper-handle value, clamped into {@code [min, max]} and never
   * below the {@linkplain #getLowerValue() lower} value. Meaningful only for the range variant.
   *
   * @return the upper-handle value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getUpperValue() {
    return clamp(upperValue, getLowerValue(), getMaximum());
  }

  /**
   * Sets the {@link Variant#RANGE} lower-handle value. Snapped to the nearest stop in stops mode,
   * then clamped into {@code [min, upper]} so the lower handle never crosses the upper one. Fires a
   * {@link ChangeListener} when the value changes.
   *
   * @param value the new lower-handle value
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setLowerValue(final int value) {
    final int snapped = isStopsEnabled() ? snap(value) : value;
    final int next = clamp(snapped, getMinimum(), getUpperValue());
    if (next == lowerValue) {
      return;
    }
    lowerValue = next;
    fireStateChanged();
    repaint();
  }

  /**
   * Sets the {@link Variant#RANGE} upper-handle value. Snapped to the nearest stop in stops mode,
   * then clamped into {@code [lower, max]} so the upper handle never crosses the lower one. Fires a
   * {@link ChangeListener} when the value changes.
   *
   * @param value the new upper-handle value
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setUpperValue(final int value) {
    final int snapped = isStopsEnabled() ? snap(value) : value;
    final int next = clamp(snapped, getLowerValue(), getMaximum());
    if (next == upperValue) {
      return;
    }
    upperValue = next;
    fireStateChanged();
    repaint();
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
   * Fires for single-variant value changes (via the backing model) and for {@link Variant#RANGE}
   * lower/upper changes alike.
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

  /** Notifies registered {@link ChangeListener}s; shared across single and range value changes. */
  private void fireStateChanged() {
    final ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
    for (final ChangeListener listener : listeners) {
      listener.stateChanged(changeEvent);
    }
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

  /**
   * Returns the stops step — the snap increment between adjacent stop indicators, or {@code 0} when
   * the slider is continuous.
   *
   * @return the stops step, or {@code 0} if continuous
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getStops() {
    return stopStep;
  }

  /**
   * Returns whether the slider is in <strong>stops</strong> mode (M3's renamed "discrete"): the
   * handle snaps to the nearest stop and stop-indicator dots are painted.
   *
   * @return {@code true} if stops are enabled
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isStopsEnabled() {
    return stopStep > 0;
  }

  /**
   * Enables <strong>stops</strong> mode with the given step (M3's renamed "discrete"
   * configuration). The handle snaps to the nearest multiple of {@code step} (measured from the
   * minimum), arrows advance by one stop, and a stop-indicator dot is painted at every stop. Pass
   * {@code 0} (or a negative value) to return to continuous mode.
   *
   * <p><strong>Don't over-crowd</strong> (research §GD3): too many stops are visually noisy and
   * hard to land on. Prefer a step that yields a readable handful of stops across the range; for a
   * fine scale, leave the slider continuous and rely on the value indicator instead.
   *
   * @param step the snap increment; {@code <= 0} disables stops
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setStops(final int step) {
    final int next = Math.max(0, step);
    if (this.stopStep == next) {
      return;
    }
    this.stopStep = next;
    if (next > 0) {
      setValue(snap(model.getValue()));
    }
    repaint();
  }

  /**
   * Returns whether end-stop indicators are painted.
   *
   * @return {@code true} if end stops are shown
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isEndStopsVisible() {
    return endStopsVisible;
  }

  /**
   * Sets whether an end-stop indicator is painted at the trailing (inactive) end of the track.
   * Defaults to {@code true}: the end stop guarantees the inactive track end meets the M3 &ge;3:1
   * contrast requirement against the background (research §X #48 / §GD3). Suppress it only when the
   * track already meets 3:1 on its own.
   *
   * @param visible {@code true} to paint the end stop
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setEndStopsVisible(final boolean visible) {
    if (this.endStopsVisible == visible) {
      return;
    }
    this.endStopsVisible = visible;
    repaint();
  }

  /**
   * Returns the fill variant.
   *
   * @return the current {@link Variant}
   * @version v0.4.0
   * @since v0.4.0
   */
  public Variant getVariant() {
    return variant;
  }

  /**
   * Sets the fill variant. {@link Variant#STANDARD} (the default) fills from the leading edge;
   * {@link Variant#CENTERED} fills from the {@linkplain #getOrigin() origin} outward to the handle;
   * {@link Variant#RANGE} fills between two handles (use {@link #range(int, int, int, int)} or set
   * the {@linkplain #setLowerValue(int) lower} / {@linkplain #setUpperValue(int) upper} values).
   * Switching variants does not change the value; all interaction, keyboard, value-bubble, stops
   * and disabled behavior is shared across variants.
   *
   * @param variant the variant; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setVariant(final Variant variant) {
    if (variant == null) {
      throw new IllegalArgumentException("variant");
    }
    if (this.variant == variant) {
      return;
    }
    this.variant = variant;
    applyRangeFocusModel();
    maybeWarnVerticalRange();
    repaint();
  }

  /**
   * In {@link Variant#RANGE} the two pill handles are the keyboard tab stops and the {@link
   * AccessibleValue} children: attach the click-through focus proxies and make the slider itself a
   * non-tab-stop with traversal keys re-enabled on the children. Any other variant detaches them
   * and restores the slider as the single focus target.
   */
  private void applyRangeFocusModel() {
    final boolean range = variant == Variant.RANGE;
    final boolean attached = lowerFocus.getParent() == this;
    if (range && !attached) {
      add(lowerFocus);
      add(upperFocus);
    } else if (!range && attached) {
      remove(lowerFocus);
      remove(upperFocus);
    }
    setFocusable(!range);
    revalidate();
  }

  /**
   * Returns the slider's {@linkplain Size size} preset.
   *
   * <p><strong>Named {@code getSizeVariant} (not {@code getSize}) deliberately:</strong> {@link
   * java.awt.Component} already defines {@code getSize()} / {@code setSize(int, int)} returning the
   * pixel {@link Dimension}, so the M3 size axis is exposed under a distinct name rather than
   * shadowing the {@code Component} contract.
   *
   * @return the current size preset (default {@link Size#XS})
   * @version v0.4.0
   * @since v0.4.0
   */
  public Size getSizeVariant() {
    return sizeVariant;
  }

  /**
   * Sets the slider's {@linkplain Size size} preset — the M3 track-thickness scale that grows the
   * track, handle, and outer corner together (research §M / §GD5). Defaults to {@link Size#XS}, the
   * canonical M3 code preset; the Phase&nbsp;1&ndash;3 behavior is exactly the {@code XS} size.
   * Switching size does not change the value; all variants, interaction, keyboard, stops, value
   * bubble, and disabled behavior are shared across sizes. The optional {@linkplain
   * #setInsetIcon(javax.swing.Icon) inset icon} requires {@link Size#M} or larger.
   *
   * @param size the size preset; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSizeVariant(final Size size) {
    if (size == null) {
      throw new IllegalArgumentException("size");
    }
    if (this.sizeVariant == size) {
      return;
    }
    this.sizeVariant = size;
    rebuildInsetIcon();
    revalidate();
    repaint();
  }

  /**
   * Returns the slider's {@linkplain Orientation orientation} — the axis the track runs along.
   *
   * @return the current orientation (default {@link Orientation#HORIZONTAL})
   * @version v0.4.0
   * @since v0.4.0
   */
  public Orientation getOrientation() {
    return orientation;
  }

  /**
   * Sets the slider's {@linkplain Orientation orientation}. {@link Orientation#VERTICAL} transposes
   * the whole component to a tall track whose active fill grows <strong>bottom-up</strong> (minimum
   * at the bottom, maximum at the top) with a horizontal pill handle; the {@linkplain
   * #getPreferredSize() preferred size} swaps its long and short axes accordingly. All variants,
   * sizes, stops, value indicator, keyboard, and accessibility carry over unchanged; vertical is
   * never right-to-left mirrored (it always fills bottom-up).
   *
   * <p><strong>Vertical {@link Variant#RANGE} is discouraged</strong> (M3 cognitive-load guidance —
   * research §G): a two-handle range is harder to scan vertically. The combination is
   * <em>allowed</em> (no hard block, matching Elwha's no-nanny API doctrine) but logs a one-time
   * advisory; prefer a horizontal range slider.
   *
   * @param orientation the orientation; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setOrientation(final Orientation orientation) {
    if (orientation == null) {
      throw new IllegalArgumentException("orientation");
    }
    if (this.orientation == orientation) {
      return;
    }
    this.orientation = orientation;
    maybeWarnVerticalRange();
    revalidate();
    repaint();
  }

  /** Logs one advisory when a range slider is put into the discouraged vertical orientation. */
  private void maybeWarnVerticalRange() {
    if (orientation == Orientation.VERTICAL && variant == Variant.RANGE && !verticalRangeWarned) {
      verticalRangeWarned = true;
      LOG.info(
          "ElwhaSlider: vertical RANGE is discouraged (M3 cognitive-load guidance) — a two-handle"
              + " range is harder to scan vertically. The combination is allowed but a horizontal"
              + " range slider is recommended.");
    }
  }

  /**
   * Returns the optional inset icon painted inside the active track at the leading/origin end, or
   * {@code null} if none is set. Renders only for the {@link Variant#STANDARD} variant at sizes
   * {@link Size#M} and larger.
   *
   * @return the inset icon, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public Icon getInsetIcon() {
    return insetIcon;
  }

  /**
   * Sets the optional <strong>inset icon</strong> (M3 §An part&nbsp;6) painted inside the track at
   * the leading/origin end — a glyph denoting what the slider controls (volume, brightness). Source
   * it via {@link com.owspfm.elwha.icons.MaterialIcons} so it is auto-tinted for contrast and sized
   * to the preset's icon dp (24 at {@link Size#M}/{@link Size#L}, 32 at {@link Size#XL}); a {@link
   * FlatSVGIcon} is recolored to {@link ColorRole#ON_PRIMARY} on the active track and {@link
   * ColorRole#ON_SECONDARY_CONTAINER} once it swaps to the inactive track, while a non-{@code
   * FlatSVGIcon} is painted verbatim.
   *
   * <p><strong>Standard variant, sizes M/L/XL only</strong> (research §GD4 — a track below
   * 40&nbsp;dp is too thin to inset a legible glyph). Setting an icon on {@link Size#XS}/{@link
   * Size#S}, {@link Variant#CENTERED}, or {@link Variant#RANGE} is a documented no-op (it logs one
   * advisory message and paints nothing). The icon sits at the leading end of the active fill and
   * <strong>repositions into the inactive track</strong> when the value drops too low for the
   * active fill to contain it (the M3 swap-at-zero affordance); the leading end and reposition
   * direction both mirror under a right-to-left {@link java.awt.ComponentOrientation}.
   *
   * @param icon the inset icon, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setInsetIcon(final Icon icon) {
    this.insetIcon = icon;
    this.insetIconNoOpWarned = false;
    rebuildInsetIcon();
    repaint();
  }

  /**
   * Caches a slot-sized, filter-bound copy of a {@link FlatSVGIcon} inset icon so paint never
   * mutates the caller's shared glyph (the #197 shared-icon-filter hazard) nor re-derives per
   * frame. Re-run whenever the icon or {@linkplain Size size} changes. Non-{@code FlatSVGIcon}
   * icons are painted as-is, so there is nothing to cache.
   */
  private void rebuildInsetIcon() {
    if (insetIcon instanceof FlatSVGIcon svg && sizeVariant.allowsInsetIcon()) {
      final int px = sizeVariant.insetIconSize;
      insetIconThemed =
          (svg.getIconWidth() == px && svg.getIconHeight() == px)
              ? new FlatSVGIcon(svg)
              : svg.derive(px, px);
      insetIconTint = null;
    } else {
      insetIconThemed = null;
      insetIconTint = null;
    }
  }

  /**
   * Returns the fill origin — the value the {@link Variant#CENTERED} active track grows out of.
   * When no origin has been {@linkplain #setOrigin(int) set} the default is {@code 0} if {@code 0}
   * lies in {@code [min, max]}, otherwise the range midpoint {@code (min + max) / 2}; the value is
   * clamped into the current range. The origin has no visual effect on the {@link Variant#STANDARD}
   * variant, which always fills from the leading edge.
   *
   * @return the resolved fill origin, clamped into {@code [min, max]}
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getOrigin() {
    if (originOverride != null) {
      return clamp(originOverride, getMinimum(), getMaximum());
    }
    return defaultOrigin();
  }

  /**
   * Sets the fill origin for the {@link Variant#CENTERED} variant — the value its active track
   * grows out of. Pass a value inside {@code [min, max]}; it is clamped on read. Has no visual
   * effect on {@link Variant#STANDARD}.
   *
   * @param origin the fill origin value
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setOrigin(final int origin) {
    this.originOverride = origin;
    repaint();
  }

  /** The default origin when none is set: zero if in range, else the range midpoint. */
  private int defaultOrigin() {
    final int min = getMinimum();
    final int max = getMaximum();
    if (min <= 0 && 0 <= max) {
      return 0;
    }
    return min + (max - min) / 2;
  }

  /**
   * Sets the formatter that renders the value-indicator bubble text. Defaults to the raw integer.
   * Use it to add a unit or map the value to a label (e.g. {@code v -> v + "%"}).
   *
   * @param formatter the value-to-text formatter, or {@code null} to reset to the raw integer
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setValueFormatter(final java.util.function.IntFunction<String> formatter) {
    this.valueFormatter = formatter;
    repaint();
  }

  /**
   * Forces the hover state on or off for static rendering (gallery / documentation previews), as on
   * {@code ElwhaButton} / {@code ElwhaFab}. Live mouse hover overrides this. Hover does not narrow
   * the handle (M3 §TS-summary: hover keeps the resting width).
   *
   * @param hovered {@code true} to paint the hover state layer
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
   * Forces the pressed state on or off for static rendering (gallery / documentation previews), as
   * on {@code ElwhaButton} / {@code ElwhaFab}. Pressed narrows the handle and reveals the value
   * bubble; live interaction also plays the ripple.
   *
   * @param pressed {@code true} to paint the pressed state
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setPressed(final boolean pressed) {
    if (this.pressed == pressed) {
      return;
    }
    this.pressed = pressed;
    updateInteractionAnimator();
    repaint();
  }

  // -------------------------------------------------------------------- sizing

  private int bubbleReserveHeight() {
    return valueIndicatorEnabled ? VALUE_BUBBLE_HEIGHT_PX + VALUE_BUBBLE_GAP_PX : 0;
  }

  private int contentHeight() {
    return bubbleReserveHeight() + handleHeight();
  }

  @Override
  public Dimension getPreferredSize() {
    return sized(DEFAULT_TRACK_LENGTH_PX, contentHeight());
  }

  @Override
  public Dimension getMinimumSize() {
    return sized(handleHeight(), contentHeight());
  }

  @Override
  public Dimension getMaximumSize() {
    return sized(Integer.MAX_VALUE, contentHeight());
  }

  /**
   * Packs a (long-axis, short-axis) extent pair into a {@link Dimension}, swapping width/height for
   * the {@linkplain Orientation#VERTICAL vertical} orientation so the long track axis is the
   * height.
   */
  private Dimension sized(final int longAxis, final int shortAxis) {
    return vertical() ? new Dimension(shortAxis, longAxis) : new Dimension(longAxis, shortAxis);
  }

  // ------------------------------------------------------------------ geometry

  /** Whether the slider is laid out and painted on the vertical axis. */
  private boolean vertical() {
    return orientation == Orientation.VERTICAL;
  }

  /** The pixel length of the long (track-running) axis — width when horizontal, height vertical. */
  private int longExtent() {
    return vertical() ? getHeight() : getWidth();
  }

  /** The pixel length of the short (cross) axis — height when horizontal, width when vertical. */
  private int shortExtent() {
    return vertical() ? getWidth() : getHeight();
  }

  /**
   * Whether the active fill is mirrored relative to value-space. Only a horizontal right-to-left
   * component mirrors; vertical always fills bottom-up (research §385/§386), so it never mirrors.
   */
  private boolean mirror() {
    return !vertical() && !getComponentOrientation().isLeftToRight();
  }

  /** The current size preset's track thickness (both segments), per the M3 §M scale. */
  private int trackHeight() {
    return sizeVariant.trackHeight;
  }

  /** The current size preset's resting handle height, per the M3 §M scale. */
  private int handleHeight() {
    return sizeVariant.handleHeight;
  }

  /** The current size preset's outer (far-end) track corner radius, per the M3 §M scale. */
  private int outerCorner() {
    return sizeVariant.outerCorner;
  }

  /** The half-width used for handle travel — the resting pill half-width, stable across morphs. */
  private static int travelInset() {
    return HANDLE_WIDTH_PX / 2;
  }

  /** The x of the leftmost handle-center position (value == minimum). */
  private int trackStartX() {
    return travelInset();
  }

  /** The long-axis position of the maximum-value handle center (right when horizontal). */
  private int trackEndX() {
    return longExtent() - travelInset();
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

  /** The pixel-space fraction, mirrored under a right-to-left (horizontal-only) orientation. */
  private float pixelFraction() {
    return mirror() ? 1f - valueFraction() : valueFraction();
  }

  /** The handle's center along the long axis for the current value; honors RTL / bottom-up fill. */
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
    if (mirror()) {
      fraction = 1f - fraction;
    }
    final int range = model.getMaximum() - model.getMinimum();
    return model.getMinimum() + Math.round(fraction * range);
  }

  /**
   * The cross-axis offset of the handle band's near edge — below the reserved bubble band. In the
   * rotated vertical paint frame this is a logical-y, mapping to a device-x (the bubble reserve
   * sits to the handle's leading side); horizontally it is the literal y below the bubble band.
   */
  int handleTopY() {
    final int top = (shortExtent() - contentHeight()) / 2;
    return Math.max(0, top) + bubbleReserveHeight();
  }

  /** The y of the track bar's top — centered on the handle band. */
  private int trackTopY() {
    return handleTopY() + (handleHeight() - trackHeight()) / 2;
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
      maybeWarnInsetIconNoOp();
      // Chrome (track / stops / handle / state layer / ripple) paints in a logical long-axis frame:
      // for VERTICAL the graphics is rotated -90° so the existing horizontal paint code draws a
      // tall, bottom-up slider verbatim. The upright overlays (inset glyph + value bubble) paint
      // afterward in native device space so their glyph / text stay upright (research §A render).
      final Graphics2D chrome = chromeGraphics(g2);
      try {
        if (variant == Variant.RANGE) {
          paintRange(chrome);
        } else {
          final int cx = handleCenterX();
          paintTrack(chrome, cx);
          if (!vertical()) {
            paintInsetIcon(chrome, cx);
          }
          paintStops(chrome, cx);
          paintStateLayer(chrome, cx);
          paintRipple(chrome, cx);
          paintHandle(chrome, cx);
          if (!vertical()) {
            paintValueBubble(chrome, cx);
          }
        }
      } finally {
        if (chrome != g2) {
          chrome.dispose();
        }
      }
      if (vertical() && variant != Variant.RANGE) {
        final int cx = handleCenterX();
        paintInsetIconVertical(g2, cx);
        paintValueBubbleVertical(g2, cx, valueText());
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * The graphics used to paint chrome: a copy rotated into the logical long-axis frame for {@link
   * Orientation#VERTICAL} (translate to the bottom-left, rotate &minus;90° so logical&nbsp;+x runs
   * up and logical&nbsp;+y runs right), or {@code g2} itself when horizontal. Callers dispose the
   * returned graphics only when it differs from {@code g2}.
   */
  private Graphics2D chromeGraphics(final Graphics2D g2) {
    if (!vertical()) {
      return g2;
    }
    final Graphics2D rot = (Graphics2D) g2.create();
    rot.translate(0, getHeight());
    rot.rotate(-Math.PI / 2);
    return rot;
  }

  private void paintTrack(final Graphics2D g2, final int cx) {
    if (variant == Variant.CENTERED) {
      paintCenteredTrack(g2, cx);
      return;
    }
    final int trackTop = trackTopY();
    final int half = HANDLE_WIDTH_PX / 2;
    final int leftWidth = cx - half - HANDLE_TRACK_GAP_PX;
    final int rightStart = cx + half + HANDLE_TRACK_GAP_PX;
    final int rightEnd = longExtent();
    // The active segment grows from the origin end: the leading (low-value) geometric segment is
    // active unless a horizontal RTL orientation mirrors it (only the color swaps; the geometry is
    // identical). Outer (far) corners full-round, handle-facing inner corners squared.
    final boolean leftIsActive = !mirror();

    final int trackH = trackHeight();
    if (leftWidth > 0) {
      g2.setColor(trackColor(leftIsActive));
      g2.fill(trackSegment(0, trackTop, leftWidth, trackH, outerCorner(), TRACK_INNER_CORNER_PX));
    }
    if (rightStart < rightEnd) {
      g2.setColor(trackColor(!leftIsActive));
      g2.fill(
          trackSegment(
              rightStart,
              trackTop,
              rightEnd - rightStart,
              trackH,
              TRACK_INNER_CORNER_PX,
              outerCorner()));
    }
  }

  /**
   * Paints the centered-variant track: the two geometric segments on either side of the handle gap
   * are each split in color at the {@linkplain #getOrigin() origin} pixel, so the active (PRIMARY)
   * fill spans only from the origin to the handle and inactive track occupies both outer sides. All
   * positions are pixel-space and already RTL-mirrored ({@link #handleCenterX()} / {@link
   * #xForValue(int)}), so no orientation special-casing is needed here.
   */
  private void paintCenteredTrack(final Graphics2D g2, final int cx) {
    final int trackTop = trackTopY();
    final int half = HANDLE_WIDTH_PX / 2;
    final int leftEnd = cx - half - HANDLE_TRACK_GAP_PX;
    final int rightStart = cx + half + HANDLE_TRACK_GAP_PX;
    final int width = longExtent();
    final int originX = clamp(xForValue(getOrigin()), 0, width);

    if (leftEnd > 0) {
      paintCenteredSegment(
          g2, trackTop, 0, leftEnd, originX, outerCorner(), TRACK_INNER_CORNER_PX, true);
    }
    if (rightStart < width) {
      paintCenteredSegment(
          g2, trackTop, rightStart, width, originX, TRACK_INNER_CORNER_PX, outerCorner(), false);
    }
  }

  /**
   * Paints one geometric track segment {@code [x0, x1]} for the centered variant, split at {@code
   * originX} into an active piece (the side toward the handle) and an inactive piece (the far side
   * of the origin). {@code leftOfHandle} marks which side of the handle gap this segment sits on,
   * which is the side whose origin-facing piece is active. The origin junction edges are squared so
   * the two colors butt seamlessly; the outer ends keep their passed radii.
   */
  private void paintCenteredSegment(
      final Graphics2D g2,
      final int trackTop,
      final int x0,
      final int x1,
      final int originX,
      final int outerLeftCorner,
      final int outerRightCorner,
      final boolean leftOfHandle) {
    if (x1 <= x0) {
      return;
    }
    final int split = clamp(originX, x0, x1);
    final int trackH = trackHeight();
    if (split > x0) {
      final int rightCorner = (split < x1) ? 0 : outerRightCorner;
      g2.setColor(trackColor(!leftOfHandle));
      g2.fill(trackSegment(x0, trackTop, split - x0, trackH, outerLeftCorner, rightCorner));
    }
    if (split < x1) {
      final int leftCorner = (split > x0) ? 0 : outerLeftCorner;
      g2.setColor(trackColor(leftOfHandle));
      g2.fill(trackSegment(split, trackTop, x1 - split, trackH, leftCorner, outerRightCorner));
    }
  }

  /** The lower handle's center x for the current lower value (RTL-aware). */
  int lowerHandleCenterX() {
    return xForValue(getLowerValue());
  }

  /** The upper handle's center x for the current upper value (RTL-aware). */
  int upperHandleCenterX() {
    return xForValue(getUpperValue());
  }

  /** The center x of the currently {@linkplain #activeHandle active} range handle. */
  private int activeHandleCenterX() {
    return activeHandle == Handle.UPPER ? upperHandleCenterX() : lowerHandleCenterX();
  }

  /**
   * Picks the range handle nearest the given x (value-space distance, orientation-agnostic). On a
   * tie — including a collapsed span — the choice keeps the gesture monotonic: a position at or
   * below the lower value grabs the lower handle, otherwise the upper.
   */
  Handle pickHandle(final int x) {
    final int clickValue = valueForX(x);
    final int dl = Math.abs(clickValue - getLowerValue());
    final int du = Math.abs(clickValue - getUpperValue());
    if (dl < du) {
      return Handle.LOWER;
    }
    if (du < dl) {
      return Handle.UPPER;
    }
    return clickValue <= getLowerValue() ? Handle.LOWER : Handle.UPPER;
  }

  /**
   * Sets the {@linkplain #activeHandle active} handle's value (snap + no-cross clamp via setters).
   */
  private void setActiveHandleValue(final int value) {
    if (activeHandle == Handle.UPPER) {
      setUpperValue(value);
    } else {
      setLowerValue(value);
    }
  }

  /** The painted width of a range handle — morphed only for the active handle (focus/press). */
  private float rangeHandleWidth(final Handle handle) {
    return handle == activeHandle ? currentHandleWidth() : HANDLE_WIDTH_PX;
  }

  /** The current value of the given range handle. */
  private int handleValue(final Handle handle) {
    return handle == Handle.UPPER ? getUpperValue() : getLowerValue();
  }

  /** Sets the given range handle's value (snap + no-cross clamp via the public setters). */
  private void setHandleValue(final Handle handle, final int value) {
    if (handle == Handle.UPPER) {
      setUpperValue(value);
    } else {
      setLowerValue(value);
    }
  }

  /** The lower bound a handle may take under the no-cross clamp (Home target). */
  private int handleMin(final Handle handle) {
    return handle == Handle.UPPER ? getLowerValue() : getMinimum();
  }

  /** The upper bound a handle may take under the no-cross clamp (End target). */
  private int handleMax(final Handle handle) {
    return handle == Handle.UPPER ? getMaximum() : getUpperValue();
  }

  /** The click-through focus proxy for the given handle. */
  private RangeHandle childFor(final Handle handle) {
    return handle == Handle.UPPER ? upperFocus : lowerFocus;
  }

  /** Whether either range handle currently owns the keyboard focus. */
  private boolean isRangeFocused() {
    return lowerFocus.isFocusOwner() || upperFocus.isFocusOwner();
  }

  /** Whether the given range handle's focus proxy owns the keyboard focus. */
  private boolean childHasFocus(final Handle handle) {
    return childFor(handle).isFocusOwner();
  }

  @Override
  public void doLayout() {
    super.doLayout();
    if (variant == Variant.RANGE) {
      positionHandleFocus(lowerFocus, lowerHandleCenterX());
      positionHandleFocus(upperFocus, upperHandleCenterX());
    }
  }

  /**
   * Sizes/positions a focus proxy over its handle halo (focus-ring + screen-reader bounds). {@code
   * cx} is the handle's long-axis center; for the vertical orientation it is mapped to device space
   * (device-y = {@code height − cx}) with the halo/handle extents transposed.
   */
  private void positionHandleFocus(final RangeHandle child, final int cx) {
    final int x;
    final int y;
    final int w;
    final int h;
    if (vertical()) {
      x = handleTopY();
      y = getHeight() - cx - HANDLE_HALO_WIDTH_PX / 2;
      w = handleHeight();
      h = HANDLE_HALO_WIDTH_PX;
    } else {
      x = cx - HANDLE_HALO_WIDTH_PX / 2;
      y = handleTopY();
      w = HANDLE_HALO_WIDTH_PX;
      h = handleHeight();
    }
    if (child.getX() != x || child.getY() != y || child.getWidth() != w || child.getHeight() != h) {
      child.setBounds(x, y, w, h);
    }
  }

  /**
   * Paints the {@link Variant#RANGE} variant: the active ({@link ColorRole#PRIMARY}) track fills
   * <em>between</em> the two handles, inactive ({@link ColorRole#SECONDARY_CONTAINER}) track flanks
   * both outer sides, stop / end-stop dots paint over the inactive ends, and both pill handles are
   * drawn. All x positions come from {@link #xForValue(int)} and are already RTL-mirrored, so the
   * lower handle may sit to the <em>right</em> of the upper under a right-to-left orientation; this
   * method works in pixel space (left/right = min/max of the two handle centers) so no orientation
   * special-casing is needed.
   */
  private void paintRange(final Graphics2D g2) {
    final int loX = lowerHandleCenterX();
    final int hiX = upperHandleCenterX();
    positionHandleFocus(lowerFocus, loX);
    positionHandleFocus(upperFocus, hiX);
    final int leftX = Math.min(loX, hiX);
    final int rightX = Math.max(loX, hiX);
    paintRangeTrack(g2, leftX, rightX);
    paintRangeStops(g2, loX, hiX);
    paintRangeStateLayer(g2, Handle.LOWER, loX);
    paintRangeStateLayer(g2, Handle.UPPER, hiX);
    paintRipple(g2, activeHandleCenterX());
    paintHandleAt(g2, loX, rangeHandleWidth(Handle.LOWER));
    paintHandleAt(g2, hiX, rangeHandleWidth(Handle.UPPER));
    // One value bubble at a time (M3): only the active (focused / dragged) handle shows one.
    if (valueIndicatorEnabled) {
      paintValueBubbleAt(g2, activeHandleCenterX(), rangeValueText(activeHandle));
    }
  }

  /**
   * Paints the hover / focus / press state-layer halo for one range handle, gated so only the
   * relevant handle lights up: hover follows the {@linkplain #hoveredHandle pointer}, focus and
   * press follow the {@linkplain #activeHandle active} handle.
   */
  private void paintRangeStateLayer(final Graphics2D g2, final Handle handle, final int cx) {
    if (!isEnabled()) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      final RoundRectangle2D.Float halo = handleHalo(cx);
      final Color tint = ColorRole.PRIMARY.resolve();
      if (childHasFocus(handle)) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.FOCUS.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
      if (hovered && handle == hoveredHandle) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.HOVER.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
      if (pressed && handle == activeHandle) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.PRESSED.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
    } finally {
      s.dispose();
    }
  }

  /**
   * Paints the range track: inactive outer-left {@code [0, leftX-gap]}, active middle between the
   * handle gaps, inactive outer-right {@code [rightX+gap, width]}. {@code leftX} / {@code rightX}
   * are the pixel-space handle centers (already ordered low&rarr;high).
   */
  private void paintRangeTrack(final Graphics2D g2, final int leftX, final int rightX) {
    final int trackTop = trackTopY();
    final int half = HANDLE_WIDTH_PX / 2;
    final int width = longExtent();
    final int leftEnd = leftX - half - HANDLE_TRACK_GAP_PX;
    final int midStart = leftX + half + HANDLE_TRACK_GAP_PX;
    final int midEnd = rightX - half - HANDLE_TRACK_GAP_PX;
    final int rightStart = rightX + half + HANDLE_TRACK_GAP_PX;

    final int trackH = trackHeight();
    if (leftEnd > 0) {
      g2.setColor(trackColor(false));
      g2.fill(trackSegment(0, trackTop, leftEnd, trackH, outerCorner(), TRACK_INNER_CORNER_PX));
    }
    if (midEnd > midStart) {
      g2.setColor(trackColor(true));
      g2.fill(
          trackSegment(
              midStart,
              trackTop,
              midEnd - midStart,
              trackH,
              TRACK_INNER_CORNER_PX,
              TRACK_INNER_CORNER_PX));
    }
    if (rightStart < width) {
      g2.setColor(trackColor(false));
      g2.fill(
          trackSegment(
              rightStart,
              trackTop,
              width - rightStart,
              trackH,
              TRACK_INNER_CORNER_PX,
              outerCorner()));
    }
  }

  /**
   * Paints stop / end-stop dots for the range variant — interior dots colored active only between
   * the two handles ({@link #stopOnActive(int)}), both-end contrast end stops in continuous mode
   * (mirroring the centered both-end rule), skipping any dot under either handle.
   */
  private void paintRangeStops(final Graphics2D g2, final int loX, final int hiX) {
    final int min = model.getMinimum();
    final int max = model.getMaximum();
    if (isStopsEnabled()) {
      for (int v = min; v <= max; v += stopStep) {
        paintRangeStopDot(g2, loX, hiX, xForValue(v), stopOnActive(v));
      }
      if (endStopsVisible && (max - min) % stopStep != 0) {
        paintRangeStopDot(g2, loX, hiX, xForValue(max), stopOnActive(max));
      }
    } else if (endStopsVisible) {
      paintRangeStopDot(g2, loX, hiX, xForValue(min), stopOnActive(min));
      paintRangeStopDot(g2, loX, hiX, xForValue(max), stopOnActive(max));
    }
  }

  private void paintRangeStopDot(
      final Graphics2D g2, final int loX, final int hiX, final int x, final boolean onActiveTrack) {
    final int skip = HANDLE_TRACK_GAP_PX + HANDLE_WIDTH_PX / 2 + STOP_INDICATOR_SIZE_PX;
    if (Math.abs(x - loX) < skip || Math.abs(x - hiX) < skip) {
      return;
    }
    drawStopDot(g2, x, onActiveTrack);
  }

  /** The handle-center x that the given value maps to (RTL-aware), without moving the model. */
  int xForValue(final int value) {
    final int range = model.getMaximum() - model.getMinimum();
    final float frac = range <= 0 ? 0f : (value - model.getMinimum()) / (float) range;
    final float pixelFrac = mirror() ? 1f - frac : frac;
    return Math.round(trackStartX() + pixelFrac * (trackEndX() - trackStartX()));
  }

  private void paintStops(final Graphics2D g2, final int cx) {
    if (isStopsEnabled()) {
      final int min = model.getMinimum();
      final int max = model.getMaximum();
      for (int v = min; v <= max; v += stopStep) {
        paintStopDot(g2, cx, xForValue(v), stopOnActive(v));
      }
      if (endStopsVisible && (max - min) % stopStep != 0) {
        paintStopDot(g2, cx, xForValue(max), stopOnActive(max));
      }
    } else if (endStopsVisible) {
      paintContinuousEndStops(g2, cx);
    }
  }

  /** Whether a stop at value {@code v} sits on the active fill — variant-aware. */
  private boolean stopOnActive(final int v) {
    if (variant == Variant.RANGE) {
      return getLowerValue() <= v && v <= getUpperValue();
    }
    if (variant == Variant.CENTERED) {
      final int origin = getOrigin();
      final int lo = Math.min(origin, model.getValue());
      final int hi = Math.max(origin, model.getValue());
      return lo <= v && v <= hi;
    }
    return v <= model.getValue();
  }

  /**
   * Continuous-mode end stops guaranteeing the inactive track meets &ge;3:1 contrast (research §X
   * #48). Standard shows a single stop at the trailing end; centered shows one at <em>both</em>
   * outer ends, since inactive track flanks the origin on both sides (research §V2).
   */
  private void paintContinuousEndStops(final Graphics2D g2, final int cx) {
    final int min = model.getMinimum();
    final int max = model.getMaximum();
    if (variant == Variant.CENTERED) {
      paintStopDot(g2, cx, xForValue(min), stopOnActive(min));
      paintStopDot(g2, cx, xForValue(max), stopOnActive(max));
    } else if (model.getValue() < max) {
      paintStopDot(g2, cx, xForValue(max), false);
    }
  }

  private void paintStopDot(
      final Graphics2D g2, final int cx, final int x, final boolean onActiveTrack) {
    final int skip = HANDLE_TRACK_GAP_PX + HANDLE_WIDTH_PX / 2 + STOP_INDICATOR_SIZE_PX;
    if (Math.abs(x - cx) < skip) {
      return;
    }
    drawStopDot(g2, x, onActiveTrack);
  }

  /** Draws a single stop-indicator dot centered on the track at the given x. */
  private void drawStopDot(final Graphics2D g2, final int x, final boolean onActiveTrack) {
    final int cy = trackTopY() + trackHeight() / 2;
    final float r = STOP_INDICATOR_SIZE_PX / 2f;
    g2.setColor(stopColor(onActiveTrack));
    g2.fill(
        new java.awt.geom.Ellipse2D.Float(
            x - r, cy - r, STOP_INDICATOR_SIZE_PX, STOP_INDICATOR_SIZE_PX));
  }

  private Color stopColor(final boolean onActiveTrack) {
    if (!isEnabled()) {
      return onActiveTrack
          ? ColorRole.SURFACE.resolve()
          : withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return (onActiveTrack ? ColorRole.ON_PRIMARY : ColorRole.ON_SECONDARY_CONTAINER).resolve();
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
      // Pressed feedback is primarily the ripple; a faint static layer keeps the pressed state
      // legible in still renders (gallery cells, reduced motion, between ripple frames).
      if (pressed) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.PRESSED.opacity()));
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
    paintHandleAt(g2, cx, currentHandleWidth());
  }

  /** Paints a single pill handle centered at {@code cx} with the given width. */
  private void paintHandleAt(final Graphics2D g2, final int cx, final float width) {
    final int handleTop = handleTopY();
    final float x = cx - width / 2f;
    g2.setColor(handleColor());
    g2.fill(new RoundRectangle2D.Float(x, handleTop, width, handleHeight(), width, width));
  }

  /** Whether the inset icon should render: a non-null icon on a standard M/L/XL slider. */
  private boolean insetIconApplies() {
    return insetIcon != null && variant == Variant.STANDARD && sizeVariant.allowsInsetIcon();
  }

  /** Logs one advisory when an inset icon was set but the current variant/size won't render it. */
  private void maybeWarnInsetIconNoOp() {
    if (insetIcon != null && !insetIconApplies() && !insetIconNoOpWarned) {
      insetIconNoOpWarned = true;
      LOG.info(
          "ElwhaSlider inset icon ignored: it renders only on the STANDARD variant at sizes M/L/XL"
              + " (current variant="
              + variant
              + ", size="
              + sizeVariant
              + ").");
    }
  }

  /**
   * Paints the inset icon at the leading/origin end of the active fill, swapping it into the
   * inactive track on the handle's trailing side when the value is too low for the active fill to
   * contain it (M3 swap-at-zero, §GD4). All x math is pixel-space and RTL-mirrored via {@link
   * #mirror()}. The vertical orientation uses {@link #paintInsetIconVertical(Graphics2D, int)}.
   */
  private void paintInsetIcon(final Graphics2D g2, final int cx) {
    if (!insetIconApplies()) {
      return;
    }
    final int iconSize = sizeVariant.insetIconSize;
    final int pad = iconSize / 2;
    final int width = longExtent();
    final int half = HANDLE_WIDTH_PX / 2;
    final int iconY = trackTopY() + trackHeight() / 2 - iconSize / 2;
    final boolean ltr = !mirror();

    final int leadingSlotX = ltr ? pad : width - pad - iconSize;
    final int activeFillLength =
        ltr ? cx - half - HANDLE_TRACK_GAP_PX : width - (cx + half + HANDLE_TRACK_GAP_PX);

    final boolean onActive = activeFillLength >= pad + iconSize;
    final int iconX;
    if (onActive) {
      iconX = leadingSlotX;
    } else {
      final int swapped =
          ltr
              ? cx + half + HANDLE_TRACK_GAP_PX + pad
              : cx - half - HANDLE_TRACK_GAP_PX - pad - iconSize;
      iconX = clamp(swapped, pad, Math.max(pad, width - pad - iconSize));
    }
    paintInsetGlyph(g2, iconX, iconY, onActive);
  }

  /**
   * Paints the inset glyph upright at the <strong>top (max) end</strong> of a vertical track — the
   * M3 bulb-at-top pattern (research §123 / §A). Unlike the horizontal leading-end icon, the
   * vertical icon stays pinned at the top: its tint follows whichever segment currently covers it,
   * swapping {@link ColorRole#ON_SECONDARY_CONTAINER} &rarr; {@link ColorRole#ON_PRIMARY} once the
   * bottom-up active fill rises far enough to reach it. Painted in device space (upright).
   */
  private void paintInsetIconVertical(final Graphics2D g2, final int cx) {
    if (!insetIconApplies()) {
      return;
    }
    final int iconSize = sizeVariant.insetIconSize;
    final int pad = iconSize / 2;
    final int half = HANDLE_WIDTH_PX / 2;
    // Device-x: centered on the track's cross axis (logical-y maps to device-x under the rotation).
    final int iconX = trackTopY() + trackHeight() / 2 - iconSize / 2;
    // Device-y: pinned near the top (max) end, the icon's near edge a pad below the top.
    final int iconY = pad;
    // The active fill rises from the bottom to the handle; it covers the top icon slot once its top
    // (logical-x cx-half-gap) passes the icon's lower edge (longExtent - pad - iconSize).
    final int activeFillTop = cx - half - HANDLE_TRACK_GAP_PX;
    final boolean onActive = activeFillTop >= longExtent() - pad - iconSize;
    paintInsetGlyph(g2, iconX, iconY, onActive);
  }

  /** Paints the (possibly themed) inset glyph; a {@link FlatSVGIcon} is recolored for its track. */
  private void paintInsetGlyph(
      final Graphics2D g2, final int iconX, final int iconY, final boolean onActive) {
    if (insetIconThemed != null) {
      final Color tint = insetIconTintColor(onActive);
      if (!tint.equals(insetIconTint)) {
        insetIconTint = tint;
        insetIconThemed.setColorFilter(new FlatSVGIcon.ColorFilter(c -> tint));
      }
      insetIconThemed.paintIcon(this, g2, iconX, iconY);
      return;
    }
    final Graphics2D ig = (Graphics2D) g2.create();
    try {
      if (!isEnabled()) {
        ig.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      }
      insetIcon.paintIcon(this, ig, iconX, iconY);
    } finally {
      ig.dispose();
    }
  }

  /** The inset-icon tint: ON_PRIMARY on the active track, ON_SECONDARY_CONTAINER once swapped. */
  private Color insetIconTintColor(final boolean onActive) {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return (onActive ? ColorRole.ON_PRIMARY : ColorRole.ON_SECONDARY_CONTAINER).resolve();
  }

  private void paintValueBubble(final Graphics2D g2, final int cx) {
    paintValueBubbleAt(g2, cx, valueText());
  }

  private void paintValueBubbleAt(final Graphics2D g2, final int cx, final String text) {
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
            Math.max(VALUE_BUBBLE_WIDTH_PX / 2, longExtent() - VALUE_BUBBLE_WIDTH_PX / 2));
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
      final int bodyHeight = VALUE_BUBBLE_HEIGHT_PX - VALUE_BUBBLE_NUB_HEIGHT_PX;
      final int bodyTop = nubTipY - VALUE_BUBBLE_HEIGHT_PX;
      final int tx = Math.round(bubbleCenterX - fm.stringWidth(text) / 2f);
      final int ty = bodyTop + (bodyHeight - fm.getHeight()) / 2 + fm.getAscent();
      b.drawString(text, tx, ty);
    } finally {
      b.dispose();
    }
  }

  /**
   * Paints the value bubble for a vertical slider, upright, on the handle's <strong>leading
   * side</strong> (left of the track) with the nub pointing right at the handle — the transposition
   * of the horizontal above-the-handle bubble. Drawn in device space so the text stays upright;
   * {@code longPos} is the handle's logical long-axis center (device-y = {@code height − longPos}).
   */
  private void paintValueBubbleVertical(final Graphics2D g2, final int longPos, final String text) {
    if (!valueIndicatorEnabled) {
      return;
    }
    final float appear = interactionAnimator.progress();
    if (appear <= 0f) {
      return;
    }
    final int half = VALUE_BUBBLE_WIDTH_PX / 2;
    final float cy = clamp(getHeight() - longPos, half, Math.max(half, getHeight() - half));
    final float nubTipX = handleTopY() - VALUE_BUBBLE_GAP_PX;
    final float scale = lerp(VALUE_BUBBLE_MIN_SCALE, 1f, appear);

    final Graphics2D b = (Graphics2D) g2.create();
    try {
      b.setComposite(AlphaComposite.SrcOver.derive(clampF(appear)));
      // Scale about the nub tip so the bubble grows out of the handle.
      b.translate(nubTipX, cy);
      b.scale(scale, scale);
      b.translate(-nubTipX, -cy);

      b.setColor(ColorRole.INVERSE_SURFACE.resolve());
      b.fill(verticalValueBubbleShape(nubTipX, cy));

      b.setColor(ColorRole.INVERSE_ON_SURFACE.resolve());
      b.setFont(getFont().deriveFont(Font.PLAIN, VALUE_BUBBLE_LABEL_PT));
      final FontMetrics fm = b.getFontMetrics();
      final float bodyWidth = VALUE_BUBBLE_HEIGHT_PX - VALUE_BUBBLE_NUB_HEIGHT_PX;
      final float bodyRight = nubTipX - VALUE_BUBBLE_NUB_HEIGHT_PX;
      final float bodyLeft = bodyRight - bodyWidth;
      final int tx = Math.round(bodyLeft + (bodyWidth - fm.stringWidth(text)) / 2f);
      final int ty = Math.round(cy - fm.getHeight() / 2f + fm.getAscent());
      b.drawString(text, tx, ty);
    } finally {
      b.dispose();
    }
  }

  /**
   * The rounded body plus rightward nub for the vertical value bubble; the nub tip sits at {@code
   * (nubTipX, cy)} pointing right at the handle, the body extending leftward.
   */
  private static Path2D.Float verticalValueBubbleShape(final float nubTipX, final float cy) {
    final float bodyW = VALUE_BUBBLE_HEIGHT_PX - VALUE_BUBBLE_NUB_HEIGHT_PX;
    final float bodyH = VALUE_BUBBLE_WIDTH_PX;
    final float right = nubTipX - VALUE_BUBBLE_NUB_HEIGHT_PX;
    final float left = right - bodyW;
    final float top = cy - bodyH / 2f;
    final float arc = Math.min(bodyW, bodyH) / 2f;
    final float nubHalf = 6f;

    final Path2D.Float p = new Path2D.Float();
    p.append(new RoundRectangle2D.Float(left, top, bodyW, bodyH, arc, arc), false);
    final Path2D.Float nub = new Path2D.Float();
    nub.moveTo(right, cy - nubHalf);
    nub.lineTo(right, cy + nubHalf);
    nub.lineTo(nubTipX, cy);
    nub.closePath();
    p.append(nub, false);
    return p;
  }

  /** The text shown in the value bubble — the {@code valueFormatter} output, or the raw value. */
  String valueText() {
    if (valueFormatter != null) {
      final String text = valueFormatter.apply(model.getValue());
      if (text != null) {
        return text;
      }
    }
    return Integer.toString(model.getValue());
  }

  /**
   * The value-bubble text for a range handle — the {@code valueFormatter} output, or the raw value.
   */
  String rangeValueText(final Handle handle) {
    final int value = handleValue(handle);
    if (valueFormatter != null) {
      final String text = valueFormatter.apply(value);
      if (text != null) {
        return text;
      }
    }
    return Integer.toString(value);
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
        handleHeight(),
        HANDLE_HALO_WIDTH_PX,
        HANDLE_HALO_WIDTH_PX);
  }

  /** A horizontal track segment with independent left/right corner radii (top == bottom). */
  static Path2D.Float trackSegment(
      final int x,
      final int y,
      final int width,
      final int height,
      final int leftRadius,
      final int rightRadius) {
    final float w = width;
    final float h = height;
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
            hoveredHandle = null;
            repaint();
          }

          @Override
          public void mouseMoved(final MouseEvent e) {
            if (!isEnabled() || variant != Variant.RANGE) {
              return;
            }
            final Handle next = pickHandle(longCoord(e));
            if (next != hoveredHandle) {
              hoveredHandle = next;
              repaint();
            }
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
            final int pos = longCoord(e);
            if (variant == Variant.RANGE) {
              activeHandle = pickHandle(pos);
              childFor(activeHandle).requestFocusInWindow();
              setActiveHandleValue(valueForX(pos));
              startRipple(new Point(activeHandleCenterX(), handleCenterY()));
            } else {
              setValue(valueForX(pos));
              startRipple(new Point(handleCenterX(), handleCenterY()));
            }
            updateInteractionAnimator();
            repaint();
          }

          @Override
          public void mouseDragged(final MouseEvent e) {
            if (!isEnabled() || !pressed) {
              return;
            }
            if (variant == Variant.RANGE) {
              setActiveHandleValue(valueForX(longCoord(e)));
            } else {
              setValue(valueForX(longCoord(e)));
            }
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
    return handleTopY() + handleHeight() / 2;
  }

  /**
   * Maps a mouse event to the long-axis position {@link #valueForX(int)} / {@link #pickHandle(int)}
   * expect: the literal {@code x} when horizontal, or {@code height − y} when vertical (so the
   * bottom of the component is the minimum end and the top is the maximum).
   */
  private int longCoord(final MouseEvent e) {
    return vertical() ? getHeight() - e.getY() : e.getX();
  }

  /** Drives the handle-narrow + value-bubble appearance toward active (focus or press) or rest. */
  private void updateInteractionAnimator() {
    final boolean focused = variant == Variant.RANGE ? isRangeFocused() : isFocusOwner();
    if (pressed || focused) {
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

  /**
   * Enables or disables the slider, propagating the state to the {@link Variant#RANGE} focus-proxy
   * children — a disabled component is skipped by focus traversal, so without the propagation a
   * disabled range slider's two handle proxies stayed Tab-reachable and the keyboard could move its
   * handles (#432). The single variant needs nothing extra: the slider itself is the focus stop.
   *
   * @param enabled whether the slider responds to input
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    lowerFocus.setEnabled(enabled);
    upperFocus.setEnabled(enabled);
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
    // WHEN_FOCUSED drives the single-variant slider (the slider is the focus owner); the ancestor
    // map drives the range variant, where a per-handle focus child owns focus and key events bubble
    // up to these same shared actions, which then target the focused handle.
    installKeys(getInputMap(WHEN_FOCUSED));
    installKeys(getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    final ActionMap am = getActionMap();

    // Up/Down always increase/decrease the value — the primary axis for a vertical slider. Left/
    // Right also adjust: they mirror under a horizontal RTL orientation but never for vertical
    // (which fills bottom-up regardless). Space held promotes arrows to the block increment
    // (research
    // §X #49 "Space & Arrows"); Page keys are the always-block equivalent.
    am.put("elwhaSlider.left", step(() -> mirror() ? stepAmount() : -stepAmount()));
    am.put("elwhaSlider.right", step(() -> mirror() ? -stepAmount() : stepAmount()));
    am.put("elwhaSlider.increase", step(this::stepAmount));
    am.put("elwhaSlider.decrease", step(() -> -stepAmount()));
    am.put("elwhaSlider.blockUp", step(this::getBlockIncrement));
    am.put("elwhaSlider.blockDown", step(() -> -getBlockIncrement()));
    am.put("elwhaSlider.min", jumpTo(true));
    am.put("elwhaSlider.max", jumpTo(false));
    am.put("elwhaSlider.spaceDown", spaceState(true));
    am.put("elwhaSlider.spaceUp", spaceState(false));
  }

  private static void installKeys(final InputMap im) {
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
  }

  /** The current arrow step — block while Space is held, otherwise the unit increment. */
  private int stepAmount() {
    return spaceDown ? getBlockIncrement() : effectiveUnitIncrement();
  }

  /**
   * The unit increment used by a single arrow; stops mode (story #345) overrides this to one stop.
   */
  int effectiveUnitIncrement() {
    return isStopsEnabled() ? stopStep : unitIncrement;
  }

  private AbstractAction step(final java.util.function.IntSupplier delta) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        // ElwhaSlider.this-qualified: an unqualified isEnabled() binds to AbstractAction's own
        // always-true flag, letting keyboard actions mutate a disabled slider (#432).
        if (!ElwhaSlider.this.isEnabled()) {
          return;
        }
        if (variant == Variant.RANGE) {
          setHandleValue(focusedHandle, handleValue(focusedHandle) + delta.getAsInt());
        } else {
          setValue(ElwhaSlider.this.getValue() + delta.getAsInt());
        }
      }
    };
  }

  private AbstractAction jumpTo(final boolean toMin) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (!ElwhaSlider.this.isEnabled()) {
          return;
        }
        if (variant == Variant.RANGE) {
          setHandleValue(
              focusedHandle, toMin ? handleMin(focusedHandle) : handleMax(focusedHandle));
        } else {
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
    public int getAccessibleChildrenCount() {
      return variant == Variant.RANGE ? 2 : 0;
    }

    @Override
    public Accessible getAccessibleChild(final int i) {
      if (variant == Variant.RANGE) {
        if (i == 0) {
          return lowerFocus;
        }
        if (i == 1) {
          return upperFocus;
        }
      }
      return null;
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

  /**
   * A zero-paint, click-through focusable proxy for one {@link Variant#RANGE} handle. It is the
   * keyboard tab stop and the {@link AccessibleValue} accessible child for its handle, while {@link
   * ElwhaSlider} paints the handle, focus ring, and value bubble. {@link #contains(int, int)}
   * returns {@code false} so the proxy never intercepts mouse events — the slider's own listener
   * drives press / drag and focuses the proxy programmatically.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  private final class RangeHandle extends JComponent implements Accessible {

    private final Handle handle;

    RangeHandle(final Handle handle) {
      this.handle = handle;
      setOpaque(false);
      setFocusable(true);
      addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
              focusedHandle = handle;
              activeHandle = handle;
              updateInteractionAnimator();
              ElwhaSlider.this.repaint();
            }

            @Override
            public void focusLost(final FocusEvent e) {
              updateInteractionAnimator();
              ElwhaSlider.this.repaint();
            }
          });
    }

    @Override
    public boolean contains(final int x, final int y) {
      return false;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleRangeHandle();
      }
      return accessibleContext;
    }

    /** Accessible node for one range handle: a slider with its own no-cross-clamped bounds. */
    final class AccessibleRangeHandle extends AccessibleJComponent implements AccessibleValue {

      @Override
      public AccessibleRole getAccessibleRole() {
        return AccessibleRole.SLIDER;
      }

      @Override
      public String getAccessibleName() {
        final String position = handle == Handle.LOWER ? "Lower" : "Upper";
        return (label != null && !label.isEmpty()) ? label + " " + position : position;
      }

      @Override
      public AccessibleValue getAccessibleValue() {
        return this;
      }

      @Override
      public Number getCurrentAccessibleValue() {
        return handleValue(handle);
      }

      @Override
      public boolean setCurrentAccessibleValue(final Number n) {
        if (n == null) {
          return false;
        }
        setHandleValue(handle, n.intValue());
        return true;
      }

      @Override
      public Number getMinimumAccessibleValue() {
        return handleMin(handle);
      }

      @Override
      public Number getMaximumAccessibleValue() {
        return handleMax(handle);
      }
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
