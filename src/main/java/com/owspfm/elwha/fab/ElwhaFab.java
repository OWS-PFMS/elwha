package com.owspfm.elwha.fab;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ContentMorphPainter;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.SurfacePainter;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The M3 Expressive Floating Action Button primitive — a single class covering both the Standard
 * (icon-only) and Extended (icon + label) forms across three sizes ({@link Size#SMALL} / {@link
 * Size#MEDIUM} / {@link Size#LARGE}) and six color styles ({@link Color}). Spec lives in {@code
 * docs/research/elwha-fab-design.md}; tracks M3 Expressive post-May-2025 (drops baseline Small,
 * Surface, baseline Extended, and Lowered FABs).
 *
 * <p><strong>Phase 1 + 2 + 3 scope.</strong> Phase 1 (#187–#189) shipped the Standard form: {@link
 * #standard(Icon)} factory + container rendering across all three sizes and six color styles + the
 * full state model (hover state layer + level-4 elevation bump, focus state layer + focus ring,
 * press state layer + ripple). Phase 2 (#190–#191) layered in the Extended form: {@link
 * #extended(String)} and {@link #extended(Icon, String)} factories, per-size label typography
 * (Inter Medium / Regular per design doc §4.2), dynamic content-driven width, and RTL mirroring of
 * the icon-leading / label-trailing order. Phase 3 (#192–#193) adds the bidirectional Standard ↔
 * Extended morph via {@link #morphTo(Form)} — one {@link MorphAnimator} at {@link
 * MorphAnimator#MEDIUM2_MS 300 ms} drives three parallel transitions per design doc §9.1: container
 * width (the visible "shape morph" — Standard's square body grows / shrinks into Extended's wider
 * round-rect through the eased progress), icon translation (centered ↔ leading inset), and label
 * opacity fade. Bidirectional morph requires an instance constructed via {@link #extended(Icon,
 * String)} so both forms' content rules can be satisfied; the other factories are single-form by
 * construction.
 *
 * <p><strong>Posture.</strong> Extends {@link JComponent} with a hand-rolled {@link
 * AccessibleJComponent} override, matching {@link com.owspfm.elwha.button.ElwhaButton} and {@link
 * com.owspfm.elwha.iconbutton.ElwhaIconButton} — the same Tab focus + Space/Enter activation +
 * {@link AccessibleRole#PUSH_BUTTON} surface the design doc §10.1 contract calls for, delivered
 * through the codebase's existing JComponent + custom-interaction pattern rather than {@code
 * AbstractButton}.
 *
 * <p><strong>Paint pipeline.</strong> {@link ShadowPainter} (resting elevation 3, hover bumps to 4)
 * → {@link SurfacePainter} (round-rect fill + state-layer overlay + focus-ring border) → {@link
 * RipplePainter} (press ripple) → icon glyph. Per design doc §6.4 the icon, label, and state-layer
 * overlay all share the active style's on-container color.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFab extends JComponent {

  /**
   * Three sizes that scale across both Standard and Extended forms per M3 Expressive (May 2025).
   * Each tier pins container side length, icon size, and corner radius — the values M3 ships in its
   * token panels. Design doc §4.
   *
   * <p><strong>M3 naming.</strong> {@link #SMALL} is what M3 calls "Regular FAB" on the Standard
   * side and "Small Extended FAB" on the Extended side (same 56 dp container, different label).
   * Elwha unifies under {@code SMALL} for cross-form symmetry — design doc §4 / §8.3.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public enum Size {

    /**
     * Small — 56 dp container, 24 dp icon, 16 dp corner radius. The default size. Matches M3's
     * "Regular FAB" (Standard) / "Small Extended FAB" (Extended). Extended-form padding 16 / 8 / 16
     * dp; label typography {@link TypeRole#TITLE_MEDIUM} (Inter Medium / 16 pt).
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    SMALL(56, 24, 16, 16, 8, 16, TypeRole.TITLE_MEDIUM),

    /**
     * Medium — 80 dp container, 28 dp icon, 20 dp corner radius. Matches M3's "Medium FAB" /
     * "Medium Extended FAB". Extended-form padding 26 / 12 / 26 dp; label typography {@link
     * TypeRole#TITLE_LARGE} (Inter Regular / 22 pt).
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    MEDIUM(80, 28, 20, 26, 12, 26, TypeRole.TITLE_LARGE),

    /**
     * Large — 96 dp container, 36 dp icon, 28 dp corner radius. Matches M3's "Large FAB" / "Large
     * Extended FAB". Extended-form padding 28 / 16 / 28 dp; label typography {@link
     * TypeRole#HEADLINE_SMALL} (Inter Regular / 24 pt).
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    LARGE(96, 36, 28, 28, 16, 28, TypeRole.HEADLINE_SMALL);

    private final int containerPx;
    private final int iconPx;
    private final int cornerRadiusPx;
    private final int extendedLeadingPx;
    private final int extendedIconGapPx;
    private final int extendedTrailingPx;
    private final TypeRole labelTypeRole;

    Size(
        final int containerPx,
        final int iconPx,
        final int cornerRadiusPx,
        final int extendedLeadingPx,
        final int extendedIconGapPx,
        final int extendedTrailingPx,
        final TypeRole labelTypeRole) {
      this.containerPx = containerPx;
      this.iconPx = iconPx;
      this.cornerRadiusPx = cornerRadiusPx;
      this.extendedLeadingPx = extendedLeadingPx;
      this.extendedIconGapPx = extendedIconGapPx;
      this.extendedTrailingPx = extendedTrailingPx;
      this.labelTypeRole = labelTypeRole;
    }

    /**
     * Returns the container side length in pixels for this size (the Standard form's full width and
     * height; the Extended form's height only — width is dynamic).
     *
     * @return the container side length
     * @version v0.3.0
     * @since v0.3.0
     */
    public int containerPx() {
      return containerPx;
    }

    /**
     * Returns the rendered icon size in pixels for this size.
     *
     * @return the icon size
     * @version v0.3.0
     * @since v0.3.0
     */
    public int iconPx() {
      return iconPx;
    }

    /**
     * Returns the corner radius in pixels for this size.
     *
     * @return the corner radius
     * @version v0.3.0
     * @since v0.3.0
     */
    public int cornerRadiusPx() {
      return cornerRadiusPx;
    }

    /**
     * Returns the leading inset in pixels for the Extended form — the gap between the container's
     * leading edge and the icon (or label, when the icon is absent). Per design doc §4.2: SMALL →
     * 16 dp, MEDIUM → 26 dp, LARGE → 28 dp.
     *
     * @return the leading inset
     * @version v0.3.0
     * @since v0.3.0
     */
    public int extendedLeadingPx() {
      return extendedLeadingPx;
    }

    /**
     * Returns the icon-to-label gap in pixels for the Extended form — applied only when an icon is
     * present. Per design doc §4.2: SMALL → 8 dp, MEDIUM → 12 dp, LARGE → 16 dp.
     *
     * @return the icon-label gap
     * @version v0.3.0
     * @since v0.3.0
     */
    public int extendedIconGapPx() {
      return extendedIconGapPx;
    }

    /**
     * Returns the trailing inset in pixels for the Extended form — the gap between the label's
     * trailing edge and the container's trailing edge. Per design doc §4.2: SMALL → 16 dp, MEDIUM →
     * 26 dp, LARGE → 28 dp.
     *
     * @return the trailing inset
     * @version v0.3.0
     * @since v0.3.0
     */
    public int extendedTrailingPx() {
      return extendedTrailingPx;
    }

    /**
     * Returns the type role that drives the Extended-form label font. Maps M3's per-size FAB label
     * typography onto Elwha's bundled Inter via {@link
     * com.owspfm.elwha.theme.Typography#defaults()} — SMALL → {@link TypeRole#TITLE_MEDIUM} (Inter
     * Medium / 16 pt), MEDIUM → {@link TypeRole#TITLE_LARGE} (Inter Regular / 22 pt), LARGE →
     * {@link TypeRole#HEADLINE_SMALL} (Inter Regular / 24 pt). Design doc §4.2 captures the Roboto
     * → Inter substitution rule.
     *
     * @return the label type role
     * @version v0.3.0
     * @since v0.3.0
     */
    public TypeRole labelTypeRole() {
      return labelTypeRole;
    }
  }

  /**
   * The six M3 color styles — three tonal ({@link #PRIMARY} / {@link #SECONDARY} / {@link
   * #TERTIARY}) plus three container ({@link #PRIMARY_CONTAINER} / {@link #SECONDARY_CONTAINER} /
   * {@link #TERTIARY_CONTAINER}). Each style binds a {@link ColorRole} container; its paired
   * on-container role (driving icon tint, label tint, and the state-layer overlay color per design
   * doc §6.4) is resolved via {@link ColorRole#on()}.
   *
   * <p>{@link #PRIMARY_CONTAINER} is the default per the M3 Color styles legend (both Standard and
   * Extended pages). Design doc §5.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public enum Color {

    /**
     * Primary container — the default color style. Resolves {@code primaryContainer} surface and
     * {@code onPrimaryContainer} foreground.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),

    /**
     * Secondary container. Resolves {@code secondaryContainer} surface and {@code
     * onSecondaryContainer} foreground.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),

    /**
     * Tertiary container. Resolves {@code tertiaryContainer} surface and {@code
     * onTertiaryContainer} foreground.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),

    /**
     * Primary tonal. Resolves {@code primary} surface and {@code onPrimary} foreground.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    PRIMARY(ColorRole.PRIMARY),

    /**
     * Secondary tonal. Resolves {@code secondary} surface and {@code onSecondary} foreground.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    SECONDARY(ColorRole.SECONDARY),

    /**
     * Tertiary tonal. Resolves {@code tertiary} surface and {@code onTertiary} foreground.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    TERTIARY(ColorRole.TERTIARY);

    private final ColorRole containerRole;

    Color(final ColorRole containerRole) {
      this.containerRole = containerRole;
    }

    /**
     * Returns this style's container {@link ColorRole} — the role that fills the FAB surface.
     *
     * @return the container role (never {@code null})
     * @version v0.3.0
     * @since v0.3.0
     */
    public ColorRole containerRole() {
      return containerRole;
    }

    /**
     * Returns this style's on-container {@link ColorRole} — the role that tints the icon, the
     * label, and the state-layer overlay color per design doc §6.4. Equivalent to {@code
     * containerRole().on().orElse(ColorRole.ON_SURFACE)}.
     *
     * @return the on-container role (never {@code null})
     * @version v0.3.0
     * @since v0.3.0
     */
    public ColorRole onContainerRole() {
      return containerRole.on().orElse(ColorRole.ON_SURFACE);
    }
  }

  // Resting elevation for the FAB body. Hover bumps to level 4 — design doc §6.1 notes that level 4
  // is the only per-state elevation token M3 publishes for FAB; everything else (resting / focused
  // / pressed) shares this component-default value.
  private static final int RESTING_ELEVATION = 3;
  private static final int HOVER_ELEVATION = 4;
  private static final float FOCUS_BORDER_WIDTH = 2f;
  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;
  // Brief keyboard-activation press-state flash. Mouse press holds pressed=true for the full
  // mouse-down duration; keyboard activation is instantaneous so this paints the M3 10% PRESSED
  // state-layer overlay for ~150 ms to give screen-reader / keyboard users the same visual
  // confirmation a mouse user gets. Pairs with the ripple lifecycle (~400 ms total).
  private static final int KEYBOARD_PRESS_FLASH_MS = 150;

  // M3 placement-diagram annotation: Extended FAB width is "Dynamic, min 80". The floor binds only
  // for the smallest size with a very short label — Medium/Large Extended naturally exceed 80 dp
  // from leading + iconPx + gap + trailing alone. Design doc §4.3.
  private static final int EXTENDED_MIN_WIDTH_PX = 80;

  // The component reserves shadow space for HOVER_ELEVATION so the hover bump never clips against
  // the component bounds. At rest the smaller resting shadow paints inside the larger reserve;
  // this is fine — the unused outer pixels are transparent.
  private static final Insets SHADOW_RESERVE = ShadowPainter.shadowInsets(HOVER_ELEVATION);

  /**
   * The two FAB forms — pinned by the construction factory and the destination value of {@link
   * #morphTo(Form)}. {@link #STANDARD} is icon-only / text-forbidden, {@link #EXTENDED} is
   * text-required / icon-optional, per the M3 content rules in design doc §3.
   *
   * <p>A FAB constructed via {@link ElwhaFab#standard(Icon)} resists morphing to {@link #EXTENDED}
   * (no label text to draw); one constructed via {@link ElwhaFab#extended(String)} resists morphing
   * to {@link #STANDARD} (no icon to draw). Only {@link ElwhaFab#extended(Icon, String)} produces
   * an instance that can move freely in either direction — Nav Rail (#159) is the consumer pattern.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public enum Form {

    /**
     * Standard (icon-only) FAB — square container, icon centered. Construction factory: {@link
     * ElwhaFab#standard(Icon)}.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    STANDARD,

    /**
     * Extended (icon + label, or label-only) FAB — content-hugging round-rect, icon leading and
     * label trailing in LTR (mirrored in RTL). Construction factories: {@link
     * ElwhaFab#extended(String)} and {@link ElwhaFab#extended(Icon, String)}.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    EXTENDED
  }

  // Construction-time form — pinned by the factory and immutable. Tracks which content (icon, text)
  // is required for either morph endpoint to be reachable. currentForm is the live destination,
  // possibly different from form after morphTo(...) has run.
  private final Form form;
  private Form currentForm;
  private Size size = Size.SMALL;
  private Color color = Color.PRIMARY_CONTAINER;
  private final Icon icon;
  private final String text;

  private boolean hovered;
  private boolean pressed;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;

  private Timer hoverPollTimer;

  private final FlatSVGIcon.ColorFilter iconFilter =
      new FlatSVGIcon.ColorFilter(c -> color.onContainerRole().resolve());

  private final List<ActionListener> actionListeners = new ArrayList<>();

  // The form-morph progress source. 0 paints Standard, 1 paints Extended; every value in between is
  // an interpolated frame of the §9.1 morph (container width, icon X, label alpha). The same
  // animator drives both directions — start() animates toward Extended, reverse() toward Standard.
  // Initialized in the constructor by snapping to the construction-time form so a non-morphing FAB
  // paints identically to its pre-Phase-3 behavior.
  private final MorphAnimator formMorph = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);

  private ElwhaFab(final Form form, final Icon icon, final String text) {
    this.form = form;
    this.currentForm = form;
    this.icon = icon;
    this.text = text;
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
    formMorph.snapTo(form == Form.EXTENDED ? 1f : 0f);
    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setFocusable(true);
    initInteraction();
  }

  /**
   * Creates a Standard FAB — icon-only, no label. Per the M3 content rule (design doc §3) the icon
   * is required; passing {@code null} throws.
   *
   * @param icon the icon to render (required)
   * @return a configured Standard FAB at the default {@link Size#SMALL} size and {@link
   *     Color#PRIMARY_CONTAINER} color
   * @throws NullPointerException if {@code icon} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaFab standard(final Icon icon) {
    if (icon == null) {
      throw new NullPointerException("icon");
    }
    return new ElwhaFab(Form.STANDARD, icon, null);
  }

  /**
   * Creates an Extended FAB — text-only, no icon. Per the M3 content rule (design doc §3) the label
   * text is required; passing {@code null} throws. There is intentionally no {@code extended(Icon)}
   * factory: an icon-only Extended FAB is a Standard FAB, which has its own factory.
   *
   * @param text the label text to render (required)
   * @return a configured Extended FAB at the default {@link Size#SMALL} size and {@link
   *     Color#PRIMARY_CONTAINER} color
   * @throws NullPointerException if {@code text} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaFab extended(final String text) {
    if (text == null) {
      throw new NullPointerException("text");
    }
    return new ElwhaFab(Form.EXTENDED, null, text);
  }

  /**
   * Creates an Extended FAB — icon (leading) plus text (trailing). Per the M3 content rule (design
   * doc §3) the label text is required and the icon is optional; the icon-bearing factory exists as
   * a convenience for the common case. Passing {@code null} text throws; {@code null} icon would
   * collapse to the text-only Extended FAB and is rejected here — use {@link #extended(String)}.
   *
   * @param icon the leading icon (required by this overload; use {@link #extended(String)} for the
   *     no-icon case)
   * @param text the label text (required)
   * @return a configured Extended FAB at the default {@link Size#SMALL} size and {@link
   *     Color#PRIMARY_CONTAINER} color
   * @throws NullPointerException if {@code icon} or {@code text} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaFab extended(final Icon icon, final String text) {
    if (icon == null) {
      throw new NullPointerException("icon");
    }
    if (text == null) {
      throw new NullPointerException("text");
    }
    return new ElwhaFab(Form.EXTENDED, icon, text);
  }

  /**
   * Sets the size tier and triggers a relayout + repaint. Named {@code setFabSize} rather than
   * {@code setSize} to avoid colliding with {@link java.awt.Component#setSize(java.awt.Dimension)}
   * — matches the {@code setButtonSize} precedent on {@link com.owspfm.elwha.button.ElwhaButton}
   * and {@link com.owspfm.elwha.iconbutton.ElwhaIconButton}.
   *
   * @param size the new size; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab setFabSize(final Size size) {
    if (size == null || size == this.size) {
      return this;
    }
    this.size = size;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active size tier.
   *
   * @return the active size (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public Size getFabSize() {
    return size;
  }

  /**
   * Sets the color style and triggers a repaint. The icon's color filter re-resolves automatically
   * on the next paint.
   *
   * @param color the new color style; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab setColor(final Color color) {
    if (color == null || color == this.color) {
      return this;
    }
    this.color = color;
    repaint();
    return this;
  }

  /**
   * Returns the active color style.
   *
   * @return the active color (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public Color getColor() {
    return color;
  }

  /**
   * Returns the installed icon. Standard FAB always has an icon; Extended FAB created via {@link
   * #extended(String)} has none. Extended FAB created via {@link #extended(Icon, String)} has one.
   *
   * @return the icon, or {@code null} when none is installed
   * @version v0.3.0
   * @since v0.3.0
   */
  public Icon getIcon() {
    return icon;
  }

  /**
   * Returns the installed label text. Standard FAB always returns {@code null}; both Extended
   * factories always return a non-null, non-empty string.
   *
   * @return the label text, or {@code null} for the Standard form
   * @version v0.3.0
   * @since v0.3.0
   */
  public String getText() {
    return text;
  }

  // ------------------------------------------------------------- morph

  /**
   * Animates this FAB to the given {@link Form} per design doc §9 — one 300 ms {@link
   * MorphAnimator} drives container width, icon translation, and label opacity in parallel, all as
   * functions of a single eased progress {@code [0, 1]}. Calling with the current form is a no-op;
   * a fresh call while a morph is in flight retargets the existing animator (forward becomes
   * reverse or vice versa) so the FAB never falls out of sync with its drive signal.
   *
   * <p><strong>Content prerequisites.</strong> {@link Form#EXTENDED} requires non-{@code null}
   * label text — an instance built via {@link #standard(Icon)} cannot morph to {@code EXTENDED}.
   * {@link Form#STANDARD} requires a non-{@code null} icon — an instance built via {@link
   * #extended(String)} cannot morph to {@code STANDARD}. Only {@link #extended(Icon, String)}
   * carries the content for both endpoints; that is the Nav Rail (#159) consumer pattern.
   *
   * @param target the destination form
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code target} is {@code null}
   * @throws IllegalStateException if the target form's content rules cannot be satisfied
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab morphTo(final Form target) {
    if (target == null) {
      throw new NullPointerException("target");
    }
    if (target == Form.EXTENDED && text == null) {
      throw new IllegalStateException(
          "morphTo(EXTENDED) requires a non-null label — construct via extended(Icon, String)");
    }
    if (target == Form.STANDARD && icon == null) {
      throw new IllegalStateException(
          "morphTo(STANDARD) requires a non-null icon — construct via extended(Icon, String)");
    }
    if (target == currentForm) {
      return this;
    }
    currentForm = target;
    if (target == Form.EXTENDED) {
      formMorph.start();
    } else {
      formMorph.reverse();
    }
    revalidate();
    return this;
  }

  /**
   * Returns the live form — the destination of the most recent {@link #morphTo(Form)} call, or the
   * construction-time form if {@code morphTo} has never been called. Stays {@link Form#EXTENDED} or
   * {@link Form#STANDARD} for the entire animation; querying mid-morph does not report an
   * intermediate state.
   *
   * @return the live form (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public Form getForm() {
    return currentForm;
  }

  /**
   * Returns whether the form-morph animator is currently running — {@code true} from the {@link
   * #morphTo(Form)} call that initiates a direction change until the animator's progress settles on
   * its target value. Useful for consumers ({@code NavRail}) that want to defer follow-on layout
   * work until the morph completes.
   *
   * @return {@code true} if a morph is in flight
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isMorphing() {
    return formMorph.isRunning();
  }

  // ------------------------------------------------------------- listeners

  /**
   * Registers an action listener fired on every activation (click or Space/Enter).
   *
   * @param listener the listener; {@code null} is ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addActionListener(final ActionListener listener) {
    if (listener != null) {
      actionListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered action listener.
   *
   * @param listener the listener to remove
   * @version v0.3.0
   * @since v0.3.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  /**
   * Forces the hover visual state on or off. Primarily for visual-validation tools (the playground
   * pre-renders the hover column without requiring a live cursor) and snapshot tests — under normal
   * use the mouse listeners drive this automatically.
   *
   * @param hovered whether to render the hover overlay
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab setHovered(final boolean hovered) {
    if (this.hovered == hovered) {
      return this;
    }
    this.hovered = hovered;
    repaint();
    return this;
  }

  /**
   * Forces the pressed visual state on or off. Primarily for visual-validation tools — pressed is
   * normally transient (cleared on mouse-up), so previewing it statically requires this hook.
   *
   * @param pressed whether to render the pressed overlay
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab setPressed(final boolean pressed) {
    if (this.pressed == pressed) {
      return this;
    }
    this.pressed = pressed;
    repaint();
    return this;
  }

  // ------------------------------------------------------------ interaction

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
            if (isCursorStillInsideBody(e)) {
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
            // Honor JComponent's setRequestFocusEnabled — clicks grab focus by default, but
            // chrome-slot contexts (e.g. ElwhaNavigationRail) suppress click-focus so the rail's
            // tablist-style focus ring is a keyboard-only affordance. Tab navigation still works
            // regardless, since it's gated by isFocusable() not isRequestFocusEnabled().
            if (isRequestFocusEnabled()) {
              requestFocusInWindow();
            }
            startRipple(toBodyPoint(e.getPoint()));
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
            pressed = true;
            repaint();
            startRipple(new Point(bodyWidthPx() / 2, bodyHeightPx() / 2));
            activate(0);
            // One-shot press-flash release. Repeated activations within the flash window each
            // schedule a new release Timer; redundant fires after pressed is already false are
            // harmless (state is idempotent).
            final Timer release =
                new Timer(
                    KEYBOARD_PRESS_FLASH_MS,
                    ev -> {
                      pressed = false;
                      repaint();
                    });
            release.setRepeats(false);
            release.start();
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhafab.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhafab.activate");
    am.put("elwhafab.activate", activate);
  }

  private void activate(final int modifiers) {
    final ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(evt);
    }
  }

  // Tests whether a component-local point lies inside the FAB body (the painted round-rect, inset
  // from the component bounds by the shadow reserve). Points in the reserve are NOT click targets —
  // the visible surface is what counts.
  private boolean containsPoint(final Point componentPoint) {
    final int bodyW = bodyWidthPx();
    final int bodyH = bodyHeightPx();
    final int x = componentPoint.x - SHADOW_RESERVE.left;
    final int y = componentPoint.y - SHADOW_RESERVE.top;
    return x >= 0 && y >= 0 && x < bodyW && y < bodyH;
  }

  // Converts a component-local click point to body-local coordinates clamped inside the visible
  // body — used as the ripple origin so a click near the body edge still seeds the ripple inside
  // the visible chrome.
  private Point toBodyPoint(final Point componentPoint) {
    final int bodyW = bodyWidthPx();
    final int bodyH = bodyHeightPx();
    final int x = componentPoint.x - SHADOW_RESERVE.left;
    final int y = componentPoint.y - SHADOW_RESERVE.top;
    return new Point(Math.max(0, Math.min(bodyW - 1, x)), Math.max(0, Math.min(bodyH - 1, y)));
  }

  // The painted body width. Standard is square (containerPx); Extended is content-hugging — leading
  // inset + (icon + icon-label gap when present) + label width + trailing inset — clamped up to the
  // M3 minimum (80 dp). No max-width clamp and no truncation per design doc §4.3. Mid-morph the
  // width interpolates between the two endpoints through Easing.EMPHASIZED so the body's apparent
  // shape glides from square (Standard) to rounded-rect (Extended) and back.
  private int bodyWidthPx() {
    final int standardW = size.containerPx();
    final int extendedW = extendedBodyWidthPx();
    final float eased = Easing.EMPHASIZED.ease(formMorph.progress());
    return ContentMorphPainter.containerWidth(standardW, extendedW, eased);
  }

  private int extendedBodyWidthPx() {
    final int leading = size.extendedLeadingPx();
    final int trailing = size.extendedTrailingPx();
    final int labelW = labelWidthPx();
    final int contentW =
        (icon != null)
            ? leading + size.iconPx() + size.extendedIconGapPx() + labelW + trailing
            : leading + labelW + trailing;
    return Math.max(EXTENDED_MIN_WIDTH_PX, contentW);
  }

  // The painted body height. Identical for both forms at a given size — Extended re-uses the
  // Standard container height per design doc §4 (the May-2025 alignment rule).
  private int bodyHeightPx() {
    return size.containerPx();
  }

  private int labelWidthPx() {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    final FontMetrics fm = getFontMetrics(size.labelTypeRole().resolve());
    return fm.stringWidth(text);
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

  // Swing's mouseExited fires unreliably on macOS for slow cursor exits; the timer queries the
  // live cursor while hovered is true and clears hover when the cursor has actually left the body.
  // Same workaround ElwhaIconButton uses.
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
    final Point screenPt = info.getLocation();
    final Point local = new Point(screenPt);
    SwingUtilities.convertPointFromScreen(local, this);
    if (!containsPoint(local)) {
      hovered = false;
      pressed = false;
      stopHoverPolling();
      repaint();
    }
  }

  private boolean isCursorStillInsideBody(final MouseEvent event) {
    if (!isShowing()) {
      return false;
    }
    final java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    final Point screenPt;
    if (info != null) {
      screenPt = info.getLocation();
    } else {
      screenPt = new Point(event.getXOnScreen(), event.getYOnScreen());
    }
    final Point local = new Point(screenPt);
    SwingUtilities.convertPointFromScreen(local, this);
    return containsPoint(local);
  }

  // ------------------------------------------------------------ ripple

  private void startRipple(final Point bodyPoint) {
    rippleOrigin = bodyPoint;
    rippleProgress = 0f;
    if (rippleTimer != null && rippleTimer.isRunning()) {
      rippleTimer.stop();
    }
    final long startNanos = System.nanoTime();
    // Capture the Timer reference locally so the lambda stops its own Timer even if a re-press
    // already replaced the field — a stale tick from the previous Timer would otherwise read
    // rippleTimer-after-reassignment and call stop() on the new Timer, freezing it mid-animation.
    final Timer self = new Timer(RIPPLE_TICK_MS, null);
    self.addActionListener(
        e -> {
          rippleProgress =
              Math.min(1f, (System.nanoTime() - startNanos) / (RIPPLE_TOTAL_MS * 1_000_000f));
          repaint();
          if (rippleProgress >= 1f) {
            self.stop();
          }
        });
    self.setRepeats(true);
    rippleTimer = self;
    self.start();
    repaint();
  }

  @Override
  public void removeNotify() {
    stopHoverPolling();
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    // Snap the form morph to its destination on detach so a re-add starts cleanly. snapTo also
    // stops the underlying Timer — equivalent to {@link MorphAnimator#immediateFinish()} but
    // explicit about the parking value we want.
    formMorph.snapTo(currentForm == Form.EXTENDED ? 1f : 0f);
    super.removeNotify();
  }

  // ----------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    // Mid-morph the body width changes every frame; revalidate so the parent layout can grow /
    // shrink the FAB's reserved footprint in lockstep with the painted body. Outside of an active
    // morph this is a no-op — preferred size is stable.
    if (formMorph.isRunning()) {
      revalidate();
    }

    final int bodyW = bodyWidthPx();
    final int bodyH = bodyHeightPx();
    final int arc = size.cornerRadiusPx();
    final boolean focused = isFocusOwner() && isEnabled();
    final int elevation = (hovered && isEnabled()) ? HOVER_ELEVATION : RESTING_ELEVATION;

    final ColorRole surfaceRole = color.containerRole();
    final StateLayer overlay = activeOverlay();
    final ColorRole borderRole = focused ? ColorRole.PRIMARY : null;
    final float borderWidth = focused ? FOCUS_BORDER_WIDTH : 0f;
    // §9.1 step 1 — container shape change. In Phase 3 today, Standard and Extended share the same
    // per-size corner radius — the visible "shape morph" is the width morph that bodyWidthPx()
    // already drives, plus the SurfacePainter clamp Math.min(arc, min(w, h)) keeping the rounded
    // ends well-formed at every interpolated width. Routing through the int-arc SurfacePainter /
    // ShadowPainter paths matches the convention every other shadowed Elwha primitive uses (a
    // value that's a RoundRectangle2D arcWidth — corner diameter, not radius — so body and shadow
    // silhouette agree on the same corner). A future asymmetric-radii variant routes through
    // {@link com.owspfm.elwha.theme.ShapeMorphPainter#interpolate} + the per-corner SurfacePainter
    // overload — the per-corner painter uses the same numeric value as a real radius, so callers
    // mixing it with ShadowPainter must reconcile the convention.

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.translate(SHADOW_RESERVE.left, SHADOW_RESERVE.top);

      if (isEnabled()) {
        ShadowPainter.paint(g2, bodyW, bodyH, arc, elevation);
        SurfacePainter.paint(g2, bodyW, bodyH, arc, surfaceRole, overlay, borderRole, borderWidth);
        paintRippleLayer(g2, bodyW, bodyH, arc);
        paintContent(g2, bodyW, bodyH, 1f);
        return;
      }

      // M3 disabled — container fill at 12% alpha, content at 38% alpha. No shadow. Matches the
      // chip / icon-button disabled handling.
      final Graphics2D dim = (Graphics2D) g2.create();
      try {
        dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
        SurfacePainter.paint(dim, bodyW, bodyH, arc, surfaceRole, null, null, 0f);
      } finally {
        dim.dispose();
      }
      paintContent(g2, bodyW, bodyH, StateLayer.disabledContentOpacity());
    } finally {
      g2.dispose();
    }
  }

  private void paintRippleLayer(
      final Graphics2D g, final int bodyW, final int bodyH, final int arc) {
    if (rippleProgress >= 1f || rippleOrigin == null) {
      return;
    }
    RipplePainter.paint(
        g, bodyW, bodyH, rippleOrigin, rippleProgress, arc, color.onContainerRole().resolve());
  }

  // Unified Standard ↔ Extended paint. Three transitions run on the same eased progress per design
  // doc §9.1: container width (already baked into bodyW from bodyWidthPx), icon X (centered ↔
  // leading-inset), and label alpha (0 ↔ 1). RTL mirrors the icon-leading / label-trailing order
  // through bodyW so the design doc §11 contract holds for every mid-morph frame too.
  private void paintContent(
      final Graphics2D g, final int bodyW, final int bodyH, final float contentAlpha) {
    final float eased = Easing.EMPHASIZED.ease(formMorph.progress());
    final boolean ltr = getComponentOrientation().isLeftToRight();

    final Font font = size.labelTypeRole().resolve();
    g.setFont(font);
    // Source the FontMetrics from the component, not Graphics2D — labelWidthPx() reads it through
    // the same getFontMetrics(font) path, so layout and paint agree exactly. Graphics2D's
    // FontMetrics inherits the paint pipeline's FontRenderContext (subpixel-AA hints + fractional
    // metrics on HiDPI macOS) and would disagree with the component's by 1–3 px, clipping the
    // trailing glyph into the trailing inset.
    final FontMetrics fm = getFontMetrics(font);
    final int labelW = (text == null || text.isEmpty()) ? 0 : fm.stringWidth(text);

    final int iconW = (icon != null) ? icon.getIconWidth() : 0;
    final int iconH = (icon != null) ? icon.getIconHeight() : 0;
    final int leading = size.extendedLeadingPx();
    final int gap = size.extendedIconGapPx();

    // Extended-form icon X (LTR-positioned, mirrored via bodyW for RTL). Slack-centers the content
    // block when the 80 dp floor binds, matching the Phase 2 layout exactly.
    final int extendedContentW =
        leading
            + iconW
            + ((iconW > 0 && labelW > 0) ? gap : 0)
            + labelW
            + size.extendedTrailingPx();
    final int slack = Math.max(0, bodyW - extendedContentW);
    final int leadOffset = leading + slack / 2;
    final int extendedIconX = ltr ? leadOffset : bodyW - leadOffset - iconW;

    // Standard-form icon X — centered in whatever current bodyW is, so the icon stays optically
    // centered even mid-morph (it slides outward as the body grows).
    final int standardIconX = (bodyW - iconW) / 2;

    // Eased horizontal lerp between the two anchors. iy is constant (vertical center) — design doc
    // §4.1 and §4.2 both put the icon on the vertical midline.
    final int iconX = ContentMorphPainter.iconX(standardIconX, extendedIconX, eased);
    final int iconY = (bodyH - iconH) / 2;

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      if (contentAlpha < 1f) {
        g2.setComposite(AlphaComposite.SrcOver.derive(contentAlpha));
      }
      if (icon != null) {
        icon.paintIcon(this, g2, iconX, iconY);
      }
      // Label alpha is shifted into the *second half* of the eased progress — the M3
      // container-emphasized fade pattern. Going Standard → Extended (forward), the body
      // expands through the first half with no visible label, then the label fades in during
      // the second half once the body has room. Going Extended → Standard (reverse), the same
      // curve runs backwards: the label fades out during the first half of the reverse, then
      // the body shrinks through the second half with no visible label — which prevents the
      // glyph from being seen outside the body as it contracts.
      final float labelAlpha = ContentMorphPainter.labelAlpha(eased);
      if (labelW > 0 && labelAlpha > 0f) {
        final int labelX;
        if (iconW > 0) {
          labelX = ltr ? leadOffset + iconW + gap : bodyW - leadOffset - iconW - gap - labelW;
        } else {
          labelX = ltr ? leadOffset : bodyW - leadOffset - labelW;
        }
        final Graphics2D gl = (Graphics2D) g2.create();
        try {
          // Defensive clip — even with the shifted alpha curve, a slow morph or a future
          // wider-label edge case shouldn't be able to paint glyph pixels past the shrinking
          // body's trailing edge. The clip rect is the painted body, not the component bounds
          // (which include the shadow reserve).
          gl.clipRect(0, 0, bodyW, bodyH);
          // Compose the label's morph-alpha on top of the inherited content alpha (which is < 1
          // only on the disabled paint branch). DST_IN-style compositing isn't needed — both
          // factors are independent SRC_OVER alpha multipliers.
          gl.setComposite(AlphaComposite.SrcOver.derive(contentAlpha * labelAlpha));
          gl.setColor(color.onContainerRole().resolve());
          gl.setFont(font);
          final int baseline = bodyH / 2 + (fm.getAscent() - fm.getDescent()) / 2;
          gl.drawString(text, labelX, baseline);
        } finally {
          gl.dispose();
        }
      }
    } finally {
      g2.dispose();
    }
  }

  // Press wins over hover (M3 states are mutually exclusive in priority); focus has its own ring
  // rather than a state-layer overlay so it can coexist with hover/press visually per design doc
  // §6.3. Disabled returns null — the disabled treatment compositing pass handles it.
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
    return null;
  }

  // ----------------------------------------------------------- LAF / sizing / a11y

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    // Extended FAB preferred width depends on FontMetrics of the per-size label TypeRole; a theme
    // reinstall that changes Typography would otherwise leave the parent layout holding a stale
    // preferred size until something else triggered relayout (e.g., the window resize).
    revalidate();
    repaint();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    if (!enabled) {
      hovered = false;
      pressed = false;
      stopHoverPolling();
      // A ripple in flight when the FAB is disabled mid-press is invisible (paintRippleLayer is
      // unreachable on the disabled paint branch) but the Timer would otherwise keep ticking and
      // scheduling repaints for the rest of the 400 ms window — wasted EDT work.
      if (rippleTimer != null) {
        rippleTimer.stop();
      }
      rippleProgress = 1f;
    }
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(
        bodyWidthPx() + SHADOW_RESERVE.left + SHADOW_RESERVE.right,
        bodyHeightPx() + SHADOW_RESERVE.top + SHADOW_RESERVE.bottom);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaFab();
    }
    return accessibleContext;
  }

  /**
   * Accessible role: {@link AccessibleRole#PUSH_BUTTON}. Name resolution priority is: declared
   * accessible name → Extended-form label text → tooltip → component name → the literal {@code
   * "Floating action button"}. The Extended-form label sits above tooltip on purpose — design doc
   * §10.4 says "Extended FAB: label text is automatically the accessible name." Standard FAB is
   * icon-only, so consumers SHOULD still set a tooltip (which doubles as the M3 hover-tooltip per
   * design doc §10.4 and as the accessible-name fallback) or call {@code
   * getAccessibleContext().setAccessibleName(...)} on every Standard FAB or screen-reader users
   * will hear nothing meaningful.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  protected class AccessibleElwhaFab extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.PUSH_BUTTON;
    }

    @Override
    public String getAccessibleName() {
      final String declared = super.getAccessibleName();
      if (declared != null && !declared.isEmpty()) {
        return declared;
      }
      if (form == Form.EXTENDED && text != null && !text.isEmpty()) {
        return text;
      }
      final String tip = getToolTipText();
      if (tip != null && !tip.isEmpty()) {
        return tip;
      }
      final String name = getName();
      if (name != null && !name.isEmpty()) {
        return name;
      }
      return "Floating action button";
    }
  }
}
