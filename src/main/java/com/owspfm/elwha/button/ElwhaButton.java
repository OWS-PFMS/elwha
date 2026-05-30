package com.owspfm.elwha.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeMorphPainter;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
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
 * A token-native M3 Expressive text-button primitive: a rounded-rect text button with five emphasis
 * variants ({@link ButtonVariant#ELEVATED} / {@link ButtonVariant#FILLED} / {@link
 * ButtonVariant#FILLED_TONAL} / {@link ButtonVariant#OUTLINED} / {@link ButtonVariant#TEXT}), a
 * {@link ButtonInteractionMode#CLICKABLE} / {@link ButtonInteractionMode#SELECTABLE} interaction
 * axis, two shape options ({@link ButtonShape#ROUND} / {@link ButtonShape#SQUARE}), and the hybrid
 * selection color model from {@code docs/research/elwha-button-design.md} §7.
 *
 * <p><strong>Size axis.</strong> The {@link ButtonSize} enum covers the five M3 Expressive tiers
 * (XS / S / M / L / XL) with per-tier container height, padding, icon size, square-corner radius,
 * and label type role from design doc Appendix A. {@link #setButtonSize(ButtonSize)} switches
 * tiers; XS / S have their component cross-axis inflated to the 48 dp WCAG touch target so click
 * dispatch covers the full target, while the visible chrome stays at the spec-mandated 32 / 40
 * centered inside the inflated bounds.
 *
 * <p><strong>Paint pipeline.</strong> {@link ShadowPainter} ({@link ButtonVariant#ELEVATED} +
 * enabled only) → {@link SurfacePainter} (round-rect fill + state-layer overlay + border) → {@link
 * RipplePainter} (during the 400 ms post-press window) → leading icon + label.
 *
 * <p><strong>Hybrid selection model.</strong> In {@link ButtonInteractionMode#SELECTABLE} mode,
 * each variant signals its selected state differently — {@link ButtonVariant#FILLED} and {@link
 * ButtonVariant#OUTLINED} <em>swap</em> their surface roles ({@code surfaceContainer ↔ primary} and
 * {@code transparent ↔ inverseSurface} respectively), while {@link ButtonVariant#ELEVATED} and
 * {@link ButtonVariant#FILLED_TONAL} composite a uniform 12% {@link StateLayer#SELECTED} overlay
 * over the resting surface and swap the border to {@link ColorRole#PRIMARY} at 2 px. The {@link
 * ButtonVariant#TEXT} variant rejects {@code SELECTABLE} at runtime with {@link
 * IllegalStateException} — the M3 spec prohibits toggle on text buttons. See {@code
 * elwha-button-design.md} §7 for the full rationale.
 *
 * <p><strong>Defaults.</strong> {@link ButtonVariant#FILLED} variant, {@link
 * ButtonInteractionMode#CLICKABLE} mode, {@link ButtonShape#ROUND} shape, border width {@code 1}
 * (only visible on {@link ButtonVariant#OUTLINED} or when selected on {@link
 * ButtonVariant#ELEVATED} / {@link ButtonVariant#FILLED_TONAL}; focus bumps to 2).
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaButton save = new ElwhaButton("Save");                       // FILLED + CLICKABLE
 * save.addActionListener(evt -> System.out.println("save clicked"));
 *
 * ElwhaButton pin = new ElwhaButton("Pinned")
 *     .setVariant(ButtonVariant.FILLED_TONAL)
 *     .setInteractionMode(ButtonInteractionMode.SELECTABLE);
 * pin.addSelectionChangeListener(evt -> System.out.println("pinned: " + pin.isSelected()));
 * }</pre>
 *
 * <p><strong>Connected-segment mode.</strong> {@link #setCornerRadii(CornerRadii)} installs a
 * per-corner radius override and switches the button into connected-segment rendering: the body is
 * painted to the full component width rather than hugging its content, so a connected {@code
 * ElwhaButtonGroup} can size segments to fill and butt them edge-to-edge. Clearing the override
 * ({@code setCornerRadii(null)}) returns the button to ordinary content-hugging rendering.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.2.0
 */
public class ElwhaButton extends JComponent {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  /**
   * Property name fired when the {@code pressed} flag flips. Used by {@link
   * com.owspfm.elwha.buttongroup.ElwhaButtonGroup} to drive the §6 standard-group width-ripple (a
   * pressed segment narrows, its ±1 / ±2 neighbors borrow a diminishing fraction of the press-width
   * delta).
   */
  public static final String PROPERTY_PRESSED = "pressed";

  private static final int DEFAULT_BORDER_WIDTH = 1;
  private static final float FOCUSED_BORDER_WIDTH = 2f;
  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;
  // #176 Phase 2 — press morph deltas. PRESS_RADIUS_DELTA_PX shrinks each corner during a full
  // press (eased press progress = 1); the body reads as "less round" without changing shape.
  // PRESS_WIDTH_DELTA_RATIO shrinks the painted body width (paint-layer only; layout untouched),
  // floored at PRESS_WIDTH_DELTA_FLOOR_PX so the smallest XS buttons still show the ripple per
  // design doc §15.4.
  private static final int PRESS_RADIUS_DELTA_PX = 4;
  private static final float PRESS_WIDTH_DELTA_RATIO = 0.06f;
  private static final int PRESS_WIDTH_DELTA_FLOOR_PX = 1;
  private static final String TEXT_TOGGLE_ERROR =
      "M3 prohibits toggle on the TEXT variant — choose FILLED_TONAL for a low-emphasis toggle, or"
          + " FILLED for high emphasis.";

  // Configuration ----------------------------------------------------------
  private ButtonVariant variant = ButtonVariant.FILLED;
  private ButtonInteractionMode interactionMode = ButtonInteractionMode.CLICKABLE;
  private ButtonShape shape = ButtonShape.ROUND;
  private ButtonSize buttonSize = ButtonSize.S;
  private ColorRole surfaceRoleOverride;
  private CornerRadii cornerRadii;
  private int borderWidth = DEFAULT_BORDER_WIDTH;
  private String text = "";
  private Icon icon;
  private Icon selectedIcon;

  // State ------------------------------------------------------------------
  private boolean hovered;
  private boolean pressed;
  private boolean selected;

  // Ripple state -----------------------------------------------------------
  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;
  private boolean rippleEnabled = true;

  // Morph state ------------------------------------------------------------
  // Press shape + width morph (#176 Phase 2) — both animate together on press / release. Width
  // morph is a paint-layer offset; layout is not re-run. The widthBorrowFactor is driven by a
  // hosting standard ElwhaButtonGroup in Phase 3 (per design doc §6); standalone buttons leave it
  // at 0.
  private final MorphAnimator pressMorph = new MorphAnimator(this, MorphAnimator.SHORT3_MS);
  private final MorphAnimator selectMorph = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);
  // Group-driven width ripple (#176 Phase 3, design doc §6). The pressed segment of a standard
  // ElwhaButtonGroup gets a +1.0 borrow factor; ±1 neighbors get +0.3; ±2 get +0.1; further get
  // 0. The factor multiplies the natural press-width delta, so during the morph the segment's
  // painted body width = natural - (easedWidth * borrowFactor * pressWidthDelta).
  private final MorphAnimator widthMorph = new MorphAnimator(this, MorphAnimator.SHORT3_MS);
  private float widthBorrowFactor;

  // #176 Phase 4 — animated connected-segment pill-pop. setCornerRadii(...) becomes the morph
  // target rather than the painted value: the previously-set radii are saved in
  // cornerRadiiFrom and cornerRadiiMorph animates from old → new through EASE_IN_OUT at the
  // §3-pinned MEDIUM2 (300 ms). The connected ElwhaButtonGroup pumps every segment's radii on
  // selection change; idempotent calls (value unchanged) skip the morph. Going to / from
  // {@code null} (i.e. switching variant) snaps without animation since one endpoint would be
  // undefined.
  private final MorphAnimator cornerRadiiMorph = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);
  private CornerRadii cornerRadiiFrom;

  // Backup poll timer for hover-clear — same workaround ElwhaChip / ElwhaIconButton use.
  private Timer hoverPollTimer;

  private final FlatSVGIcon.ColorFilter iconFilter =
      new FlatSVGIcon.ColorFilter(c -> resolveForegroundColor());

  private final List<ActionListener> actionListeners = new ArrayList<>();

  // ----------------------------------------------------------------- ctors

  /**
   * Creates an empty button with no label or icon. Call {@link #setText(String)} before adding to a
   * UI.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton() {
    this(null, null);
  }

  /**
   * Creates a button with the given label.
   *
   * @param label the button label; may be {@code null} or empty
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton(final String label) {
    this(label, null);
  }

  /**
   * Creates a button with a label and a leading icon.
   *
   * @param label the button label; may be {@code null} or empty
   * @param icon the leading icon; may be {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton(final String label, final Icon icon) {
    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setFocusable(true);
    this.text = label != null ? label : "";
    setIcon(icon);
    initInteraction();
  }

  // ---------------------------------------------------------- factory presets

  /**
   * Creates an M3 elevated button preset — {@link ButtonVariant#ELEVATED}. Every preset choice
   * remains overridable through the normal setters.
   *
   * @param label the button label
   * @return a configured elevated button
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaButton elevatedButton(final String label) {
    return new ElwhaButton(label).setVariant(ButtonVariant.ELEVATED);
  }

  /**
   * Creates an M3 filled button preset — {@link ButtonVariant#FILLED}, the default variant.
   *
   * @param label the button label
   * @return a configured filled button
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaButton filledButton(final String label) {
    return new ElwhaButton(label).setVariant(ButtonVariant.FILLED);
  }

  /**
   * Creates an M3 filled-tonal button preset — {@link ButtonVariant#FILLED_TONAL}. M3's canonical
   * CTA pairing with Outlined cards.
   *
   * @param label the button label
   * @return a configured filled-tonal button
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaButton filledTonalButton(final String label) {
    return new ElwhaButton(label).setVariant(ButtonVariant.FILLED_TONAL);
  }

  /**
   * Creates an M3 outlined button preset — {@link ButtonVariant#OUTLINED}.
   *
   * @param label the button label
   * @return a configured outlined button
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaButton outlinedButton(final String label) {
    return new ElwhaButton(label).setVariant(ButtonVariant.OUTLINED);
  }

  /**
   * Creates an M3 text button preset — {@link ButtonVariant#TEXT}. Lowest emphasis; transparent
   * surface and no border. Cannot be made selectable.
   *
   * @param label the button label
   * @return a configured text button
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaButton textButton(final String label) {
    return new ElwhaButton(label).setVariant(ButtonVariant.TEXT);
  }

  // -------------------------------------------------------------- variant

  /**
   * Sets the surface variant and triggers a repaint. Throws if the target variant is {@link
   * ButtonVariant#TEXT} while the interaction mode is already {@link
   * ButtonInteractionMode#SELECTABLE} — see {@code elwha-button-design.md} §7 for the symmetric
   * guard rule.
   *
   * @param variant the new variant; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @throws IllegalStateException if {@code variant == TEXT} and {@code interactionMode ==
   *     SELECTABLE}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setVariant(final ButtonVariant variant) {
    if (variant == null || variant == this.variant) {
      return this;
    }
    if (variant == ButtonVariant.TEXT && interactionMode == ButtonInteractionMode.SELECTABLE) {
      throw new IllegalStateException(TEXT_TOGGLE_ERROR);
    }
    this.variant = variant;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active variant.
   *
   * @return the active variant (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonVariant getVariant() {
    return variant;
  }

  // ---------------------------------------------------------- interaction

  /**
   * Sets the interaction mode and triggers a repaint. Throws if the target mode is {@link
   * ButtonInteractionMode#SELECTABLE} while the variant is {@link ButtonVariant#TEXT} — the
   * symmetric guard for {@link #setVariant}.
   *
   * @param mode the new mode; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @throws IllegalStateException if {@code mode == SELECTABLE} and {@code variant == TEXT}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setInteractionMode(final ButtonInteractionMode mode) {
    if (mode == null || mode == this.interactionMode) {
      return this;
    }
    if (mode == ButtonInteractionMode.SELECTABLE && variant == ButtonVariant.TEXT) {
      throw new IllegalStateException(TEXT_TOGGLE_ERROR);
    }
    this.interactionMode = mode;
    if (mode == ButtonInteractionMode.CLICKABLE) {
      // Switching out of SELECTABLE clears any latched selection — there is no toggle state in
      // CLICKABLE mode, and leaving the flag latched would produce a phantom border/overlay if the
      // mode were later switched back.
      this.selected = false;
    }
    repaint();
    return this;
  }

  /**
   * Returns the active interaction mode.
   *
   * @return the active interaction mode (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonInteractionMode getInteractionMode() {
    return interactionMode;
  }

  // ------------------------------------------------------------- label/icon

  /**
   * Sets the button label.
   *
   * @param label the new label; {@code null} clears it
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setText(final String label) {
    this.text = label != null ? label : "";
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the button label, or the empty string if none is set.
   *
   * @return the label
   * @version v0.2.0
   * @since v0.2.0
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the leading icon. The selected-state icon is cleared — call {@link #setIcons(Icon, Icon)}
   * to install a resting / selected pair.
   *
   * @param icon the leading icon, or {@code null} to clear
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setIcon(final Icon icon) {
    return setIcons(icon, null);
  }

  /**
   * Installs the resting / selected leading-icon pair — the M3 toggle-icon swap, mirroring {@link
   * com.owspfm.elwha.iconbutton.ElwhaIconButton#setIcons(Icon, Icon)}. The selected icon is
   * rendered when {@link #isSelected()} is true; if {@code selected} is {@code null}, the resting
   * icon is rendered in both states.
   *
   * <p><strong>Press preview.</strong> The selected icon also flashes during a live press,
   * regardless of interaction mode — tactile feedback for a {@link ButtonInteractionMode#CLICKABLE}
   * button, a toggle preview for a {@link ButtonInteractionMode#SELECTABLE} one. Pass {@code null}
   * for {@code selected} to opt out (the resting icon then renders in every state).
   *
   * @param resting the icon rendered when not selected and not being pressed; {@code null} clears
   *     the leading icon entirely
   * @param selected the icon rendered when selected or pressed, or {@code null} to reuse {@code
   *     resting}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButton setIcons(final Icon resting, final Icon selected) {
    applyIconFilter(resting);
    applyIconFilter(selected);
    this.icon = resting;
    this.selectedIcon = selected;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the resting leading icon, or {@code null}.
   *
   * @return the resting leading icon
   * @version v0.2.0
   * @since v0.2.0
   */
  public Icon getIcon() {
    return icon;
  }

  /**
   * Returns the selected-state leading icon, or {@code null} if only a resting icon was installed.
   *
   * @return the selected leading icon, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public Icon getSelectedIcon() {
    return selectedIcon;
  }

  private void applyIconFilter(final Icon candidate) {
    if (candidate instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
  }

  // The leading icon for the current state — the selected icon when the button is selected or
  // being pressed (and a selected icon was installed), the resting icon otherwise.
  private Icon currentIcon() {
    if (selectedIcon != null && (selected || pressed)) {
      return selectedIcon;
    }
    return icon;
  }

  // ----------------------------------------------------------- token setters

  /**
   * Overrides the variant's default surface role. Pass {@code null} to fall back to the variant's
   * default; the foreground re-pairs against the new effective surface's {@code on}-role (except
   * for {@link ButtonVariant#TEXT}, whose {@link ColorRole#PRIMARY} foreground is rigid). The
   * override is ignored on {@link ButtonVariant#TEXT}.
   *
   * @param role the surface role, or {@code null} to clear the override
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setSurfaceRole(final ColorRole role) {
    this.surfaceRoleOverride = role;
    repaint();
    return this;
  }

  /**
   * Returns the effective surface role for the current variant + selection + override combination.
   * Reflects the §7 selection swaps for {@link ButtonVariant#FILLED} and {@link
   * ButtonVariant#OUTLINED} when {@link #isSelected()}; returns {@code null} for transparent rest
   * states.
   *
   * @return the effective surface role, or {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ColorRole getSurfaceRole() {
    return effectiveSurfaceRole();
  }

  /**
   * Sets the shape and triggers a repaint.
   *
   * @param shape the new shape; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setShape(final ButtonShape shape) {
    if (shape == null || shape == this.shape) {
      return this;
    }
    this.shape = shape;
    repaint();
    return this;
  }

  /**
   * Returns the active shape.
   *
   * @return the active shape (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonShape getShape() {
    return shape;
  }

  /**
   * Installs a per-corner radius override and switches the button into connected-segment rendering,
   * or clears it. This is the {@code ElwhaButtonGroup} connected-variant hook (design doc §16): a
   * butted segment renders its outward corners at the group shape and its inner corners nearly
   * square, which a single uniform {@link ButtonShape} cannot express.
   *
   * <p>While an override is installed the button is in <em>connected-segment mode</em> — it paints
   * its body to the full component width instead of hugging its content, and the override radii
   * replace the {@link #setShape(ButtonShape) shape}-derived corner radius for the surface, border,
   * and ripple clip. {@link #getPreferredSize()} still reports the content-hugging width, so the
   * owning group remains free to size segments larger via layout. Pass {@code null} to clear the
   * override and return to ordinary rendering.
   *
   * @param radii the four corner radii, or {@code null} to clear the override
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButton setCornerRadii(final CornerRadii radii) {
    if (Objects.equals(this.cornerRadii, radii)) {
      return this;
    }
    // #176 Phase 4 — when both the old value and the new value are non-null, animate from one to
    // the other via cornerRadiiMorph (300 ms EASE_IN_OUT, the connected pill-pop choreography in
    // design doc §7). Mid-animation, capture the currently-displayed (interpolated) value as the
    // new `from` so the next animation starts from where the eye is, not from the previous
    // target — avoids the visible snap when a connected group pumps state changes faster than
    // 300 ms apart. Going to or from {@code null} (variant switching) snaps because one endpoint
    // would be undefined.
    final CornerRadii newFrom = captureCornerRadiiFromValue();
    this.cornerRadiiFrom = newFrom;
    this.cornerRadii = radii;
    if (this.cornerRadiiFrom != null && this.cornerRadii != null) {
      cornerRadiiMorph.snapTo(0f);
      cornerRadiiMorph.start();
    } else {
      cornerRadiiMorph.snapTo(1f);
    }
    revalidate();
    repaint();
    return this;
  }

  // Returns the currently-displayed radii — either the previous static target, or the in-flight
  // interpolated value if a cornerRadii morph is mid-flight. Used as the `from` endpoint for a
  // newly-started morph so a rapid state-change sequence reads as a continuous motion rather
  // than a series of snaps.
  private CornerRadii captureCornerRadiiFromValue() {
    if (this.cornerRadii == null || this.cornerRadiiFrom == null) {
      return this.cornerRadii;
    }
    if (!cornerRadiiMorph.isRunning()) {
      return this.cornerRadii;
    }
    return ShapeMorphPainter.interpolate(
        this.cornerRadiiFrom, this.cornerRadii, cornerRadiiMorph.progress(), Easing.EASE_IN_OUT);
  }

  /**
   * Returns the per-corner radius override, or {@code null} when the button uses its {@link
   * #getShape() shape}-derived uniform corner radius.
   *
   * @return the per-corner radius override, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public CornerRadii getCornerRadii() {
    return cornerRadii;
  }

  /**
   * Sets the M3 size tier and triggers a relayout + repaint. The shape is not touched — picking a
   * size leaves the corner treatment alone (a {@link ButtonShape#ROUND} button stays a capsule at
   * every size).
   *
   * @param size the new size; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setButtonSize(final ButtonSize size) {
    if (size == null || size == this.buttonSize) {
      return this;
    }
    this.buttonSize = size;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active size tier.
   *
   * @return the active size (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public ButtonSize getButtonSize() {
    return buttonSize;
  }

  /**
   * Sets the resting border-stroke width in pixels. Focus bumps the effective stroke width to
   * {@code 2}; the value set here is only the resting width.
   *
   * @param px the stroke width, clamped to {@code >= 0}
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setBorderWidth(final int px) {
    this.borderWidth = Math.max(0, px);
    repaint();
    return this;
  }

  /**
   * Returns the resting border-stroke width in pixels.
   *
   * @return the resting stroke width
   * @version v0.2.0
   * @since v0.2.0
   */
  public int getBorderWidth() {
    return borderWidth;
  }

  // ------------------------------------------------------------ selected

  /**
   * Sets the selected state and fires a {@link #PROPERTY_SELECTED} property change. No-op if the
   * interaction mode is {@link ButtonInteractionMode#CLICKABLE} — push-buttons have no persistent
   * selected state.
   *
   * @param selected the new selected state
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setSelected(final boolean selected) {
    if (interactionMode != ButtonInteractionMode.SELECTABLE || selected == this.selected) {
      return this;
    }
    final boolean old = this.selected;
    this.selected = selected;
    // #176 Phase 2 — animate the round ↔ square shape flip. A disabled button is visually frozen
    // (§15.7); snap the morph to the new state without scheduling ticks.
    if (!isEnabled()) {
      selectMorph.snapTo(selected ? 1f : 0f);
    } else if (selected) {
      selectMorph.start();
    } else {
      selectMorph.reverse();
    }
    repaint();
    firePropertyChange(PROPERTY_SELECTED, old, selected);
    return this;
  }

  /**
   * Returns the current selected state.
   *
   * @return the current selected state
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Forces the hover visual state on or off. Primarily for visual-validation tools (the playground
   * pre-renders the hover column without requiring a live cursor) and snapshot tests.
   *
   * @param hovered whether to render the hover overlay
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setHovered(final boolean hovered) {
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
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setPressed(final boolean pressed) {
    setPressedInternal(pressed);
    return this;
  }

  /**
   * Enables or disables the press / touch ripple — the expanding {@link
   * com.owspfm.elwha.theme.RipplePainter} circle seeded at the click point on press or keyboard
   * activation. Disable it for hosts whose own motion is the press feedback: a dialog that
   * dismisses on an action click would otherwise freeze the in-flight ripple onto its exit-fade
   * snapshot (epic #288). Disabling mid-ripple clears any in-flight ripple immediately.
   *
   * <p>This gates <em>only</em> the press ripple. The pressed state-layer darken and the
   * connected-group width-ripple ({@link com.owspfm.elwha.buttongroup.ElwhaButtonGroup}, #176) are
   * unaffected. Default {@code true}.
   *
   * @param rippleEnabled whether press / touch ripples animate
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButton setRippleEnabled(final boolean rippleEnabled) {
    this.rippleEnabled = rippleEnabled;
    if (!rippleEnabled && rippleTimer != null && rippleTimer.isRunning()) {
      rippleTimer.stop();
      rippleProgress = 1f;
      repaint();
    }
    return this;
  }

  /**
   * Reports whether the press / touch ripple is enabled.
   *
   * @return {@code true} if press ripples animate (the default), {@code false} if suppressed
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isRippleEnabled() {
    return this.rippleEnabled;
  }

  // Internal mutator — every `this.pressed = ...` assignment routes through here so the
  // PROPERTY_PRESSED firing stays consistent (the group listens for it to drive the §6
  // width-ripple). No-op if the value isn't actually changing.
  private void setPressedInternal(final boolean newPressed) {
    if (this.pressed == newPressed) {
      return;
    }
    final boolean old = this.pressed;
    this.pressed = newPressed;
    repaint();
    firePropertyChange(PROPERTY_PRESSED, old, newPressed);
  }

  // ------------------------------------------------------------ listeners

  /**
   * Registers an action listener that fires on click (CLICKABLE) or toggle (SELECTABLE).
   *
   * @param listener the listener; {@code null} is ignored
   * @version v0.2.0
   * @since v0.2.0
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
   * @version v0.2.0
   * @since v0.2.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  /**
   * Programmatically activates the button, firing its action listeners (and, in {@code SELECTABLE}
   * mode, toggling selection) exactly as a click or keyboard activation would — the {@link
   * javax.swing.JButton#doClick()} analogue {@code JComponent} doesn't provide. No ripple or press
   * animation runs; this is the plumbing path for host components that drive a button by key
   * binding, such as an {@code ElwhaDialog} mapping Enter to its confirming action.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void doClick() {
    if (isEnabled()) {
      activate(0);
    }
  }

  /**
   * Convenience: scopes a {@link PropertyChangeListener} to {@link #PROPERTY_SELECTED}.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  public void addSelectionChangeListener(final PropertyChangeListener listener) {
    addPropertyChangeListener(PROPERTY_SELECTED, listener);
  }

  // ----------------------------------------------------------- role resolution

  /**
   * Resolves the effective surface role for the current variant + selection + override combination.
   * Implements the §7 hybrid selection model: {@link ButtonVariant#FILLED} swaps {@code
   * surfaceContainer ↔ primary} on selected; {@link ButtonVariant#OUTLINED} swaps {@code
   * transparent ↔ inverseSurface}. Override always wins.
   */
  private ColorRole effectiveSurfaceRole() {
    if (variant == ButtonVariant.TEXT) {
      return null;
    }
    if (surfaceRoleOverride != null) {
      return surfaceRoleOverride;
    }
    if (interactionMode == ButtonInteractionMode.SELECTABLE) {
      if (variant == ButtonVariant.FILLED) {
        return selected ? ColorRole.PRIMARY : ColorRole.SURFACE_CONTAINER;
      }
      if (variant == ButtonVariant.OUTLINED) {
        return selected ? ColorRole.INVERSE_SURFACE : null;
      }
    }
    return variant.surfaceRole();
  }

  /**
   * Resolves the effective border role + width for the current state. Focus always wins (PRIMARY at
   * 2 px). Selected ELEVATED/FILLED_TONAL also swap to PRIMARY at 2 px. Selected OUTLINED drops the
   * border (the inverse fill carries the signal). Selected FILLED has no border in either toggle
   * state.
   */
  private ColorRole effectiveBorderRole(final boolean focused) {
    if (variant == ButtonVariant.TEXT) {
      return null;
    }
    if (focused) {
      return ColorRole.PRIMARY;
    }
    if (interactionMode == ButtonInteractionMode.SELECTABLE && selected) {
      if (variant == ButtonVariant.ELEVATED || variant == ButtonVariant.FILLED_TONAL) {
        return ColorRole.PRIMARY;
      }
      if (variant == ButtonVariant.OUTLINED) {
        return null;
      }
    }
    return variant.borderRole();
  }

  private float effectiveBorderWidth(final boolean focused) {
    if (focused) {
      return Math.max(borderWidth, FOCUSED_BORDER_WIDTH);
    }
    if (interactionMode == ButtonInteractionMode.SELECTABLE
        && selected
        && (variant == ButtonVariant.ELEVATED || variant == ButtonVariant.FILLED_TONAL)) {
      return FOCUSED_BORDER_WIDTH;
    }
    return borderWidth;
  }

  /**
   * Resolves the effective foreground color for the label and icon tint. Implements §3 plus the §7
   * hybrid swaps: TEXT is always PRIMARY; ELEVATED with no override uses PRIMARY (not the surface's
   * on-pair). Selectable FILLED/OUTLINED swap foreground roles to match their swapped surfaces.
   *
   * @return the resolved foreground color (never {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  protected Color resolveForegroundColor() {
    if (variant == ButtonVariant.TEXT) {
      return ColorRole.PRIMARY.resolve();
    }
    if (interactionMode == ButtonInteractionMode.SELECTABLE) {
      if (variant == ButtonVariant.FILLED) {
        return (selected ? ColorRole.ON_PRIMARY : ColorRole.ON_SURFACE_VARIANT).resolve();
      }
      if (variant == ButtonVariant.OUTLINED && selected) {
        return ColorRole.INVERSE_ON_SURFACE.resolve();
      }
    }
    if (surfaceRoleOverride != null) {
      return surfaceRoleOverride.on().orElse(ColorRole.ON_SURFACE_VARIANT).resolve();
    }
    return variant.foregroundRole().resolve();
  }

  // ----------------------------------------------------------- geometry

  private int cornerRadiusPx() {
    return shape == ButtonShape.ROUND ? Integer.MAX_VALUE : buttonSize.squareCornerPx();
  }

  private int elevationLevel() {
    return variant == ButtonVariant.ELEVATED && isEnabled() ? 1 : 0;
  }

  private Insets shadowReserve() {
    final int e = elevationLevel();
    return e > 0 ? ShadowPainter.shadowInsets(e) : new Insets(0, 0, 0, 0);
  }

  private int bodyWidthPx() {
    final int labelW = labelWidthPx();
    if (icon != null) {
      return buttonSize.paddingWithIconLeadingPx()
          + buttonSize.iconSizePx()
          + buttonSize.paddingWithIconGapPx()
          + labelW
          + buttonSize.paddingWithIconTrailingPx();
    }
    return buttonSize.paddingNoIconPx() + labelW + buttonSize.paddingNoIconPx();
  }

  // The width the body is painted at: the content-hugging width normally, or the full component
  // width (minus shadow reserve) in connected-segment mode — so a connected ElwhaButtonGroup can
  // stretch a segment to fill its share of the row and have the surface fill it.
  private int effectiveBodyWidth() {
    if (cornerRadii != null) {
      final Insets s = shadowReserve();
      return Math.max(1, getWidth() - s.left - s.right);
    }
    return bodyWidthPx();
  }

  private int labelWidthPx() {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    final FontMetrics fm = getFontMetrics(buttonSize.typeRole().resolve());
    return fm.stringWidth(text);
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
            if (pressed && firesPressMorph()) {
              pressMorph.reverse();
            }
            setPressedInternal(false);
            stopHoverPolling();
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            if (!containsClickPoint(e.getPoint())) {
              return;
            }
            setPressedInternal(true);
            requestFocusInWindow();
            startRipple(toBodyPoint(e.getPoint()));
            if (firesPressMorph()) {
              pressMorph.start();
            }
            repaint();
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed || !isEnabled()) {
              setPressedInternal(false);
              if (firesPressMorph()) {
                pressMorph.reverse();
              }
              repaint();
              return;
            }
            setPressedInternal(false);
            if (firesPressMorph()) {
              pressMorph.reverse();
            }
            if (containsClickPoint(e.getPoint())) {
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
            if (pressed && firesPressMorph()) {
              pressMorph.reverse();
            }
            setPressedInternal(false);
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
            // Center-of-body ripple for keyboard activation — body-local coords, not
            // component-local, so the ripple seeds inside the visible chrome even when the
            // component is inflated for a11y target.
            startRipple(new Point(effectiveBodyWidth() / 2, buttonSize.containerHeightPx() / 2));
            activate(0);
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhabutton.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhabutton.activate");
    am.put("elwhabutton.activate", activate);
  }

  private void activate(final int modifiers) {
    if (interactionMode == ButtonInteractionMode.SELECTABLE) {
      setSelected(!selected);
    }
    final ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(evt);
    }
  }

  /**
   * Returns the body origin in component-local coordinates — the top-left of the visible round-rect
   * body. The body is centered inside the {@code component - shadowReserve} rect on each axis, so
   * when the a11y target inflation grows the cross axis beyond the body's natural height the body
   * sits visually centered with the inflated padding split above and below.
   */
  private Point bodyOrigin() {
    final Insets s = shadowReserve();
    final int insetW = getWidth() - s.left - s.right;
    final int insetH = getHeight() - s.top - s.bottom;
    final int bodyW = effectiveBodyWidth();
    final int bodyH = buttonSize.containerHeightPx();
    return new Point(
        s.left + Math.max(0, (insetW - bodyW) / 2), s.top + Math.max(0, (insetH - bodyH) / 2));
  }

  /**
   * Tests whether a component-local point lies inside the click hit area. The hit area is the full
   * component bounds (excluding the shadow reserve) — including the a11y target inflation padding
   * around the visible body, so clicks in that padding still register and dispatch a press / ripple
   * on the body. WCAG 2.5.5 — 48 dp minimum touch target on XS / S.
   */
  private boolean containsClickPoint(final Point componentPoint) {
    final Insets s = shadowReserve();
    final int hitW = getWidth() - s.left - s.right;
    final int hitH = getHeight() - s.top - s.bottom;
    final int x = componentPoint.x - s.left;
    final int y = componentPoint.y - s.top;
    return x >= 0 && y >= 0 && x < hitW && y < hitH;
  }

  /**
   * Converts a component-local click point to body-local coordinates clamped inside the visible
   * body — used as the ripple origin so a click on the a11y inflation padding still seeds the
   * ripple inside the visible chrome.
   */
  private Point toBodyPoint(final Point componentPoint) {
    final Point origin = bodyOrigin();
    final int bodyW = effectiveBodyWidth();
    final int bodyH = buttonSize.containerHeightPx();
    final int x = componentPoint.x - origin.x;
    final int y = componentPoint.y - origin.y;
    return new Point(Math.max(0, Math.min(bodyW - 1, x)), Math.max(0, Math.min(bodyH - 1, y)));
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
    if (!containsClickPoint(local)) {
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
    return containsClickPoint(local);
  }

  // ----------------------------------------------------------- ripple

  private void startRipple(final Point bodyOrigin) {
    if (!rippleEnabled) {
      return;
    }
    rippleOrigin = bodyOrigin;
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
    stopHoverPolling();
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    pressMorph.stop();
    selectMorph.stop();
    widthMorph.stop();
    cornerRadiiMorph.stop();
    super.removeNotify();
  }

  // #176 Phase 2 — snap selectMorph to the current selected state on first addNotify so a button
  // constructed in the selected state (e.g. new ElwhaButton(...).setSelected(true) before display)
  // paints at the selected shape on its first frame rather than animating into it.
  @Override
  public void addNotify() {
    super.addNotify();
    selectMorph.snapTo(selected ? 1f : 0f);
    pressMorph.snapTo(0f);
    widthMorph.snapTo(0f);
    widthBorrowFactor = 0f;
    // #176 Phase 4 — first-display paints the current cornerRadii target directly; subsequent
    // setCornerRadii(...) calls (e.g. selection moves in a connected group) will animate via
    // captureCornerRadiiFromValue. snapTo(1) is the "morph is done" state so morphedRadii uses
    // the target verbatim.
    cornerRadiiMorph.snapTo(1f);
    cornerRadiiFrom = null;
  }

  /**
   * Starts a group-driven width-ripple borrow. Called by a hosting standard {@link
   * com.owspfm.elwha.buttongroup.ElwhaButtonGroup} on every segment when one of its segments is
   * pressed — the pressed segment gets {@code factor = 1.0} (full pinch), ±1 neighbors get {@code
   * 0.3} (30 % of the natural press-width delta), ±2 neighbors get {@code 0.1}, and further-out
   * segments get {@code 0.0} (no ripple). Design doc §6.
   *
   * <p>The factor multiplies the natural press-width delta of <em>this</em> button (so a smaller
   * neighbor borrows proportionally less). The morph itself is the standard {@link
   * MorphAnimator#SHORT3_MS} (150 ms) press timing.
   *
   * <p>Calling with {@code factor = 0.0} is equivalent to {@link #releaseWidthBorrow()} —
   * convenient when a group computes a borrow vector with some zero entries.
   *
   * @param factor the borrow factor in {@code [-1, 1]}; clamped if outside
   * @version v0.3.0
   * @since v0.3.0
   */
  public void startWidthBorrow(final float factor) {
    final float clamped = Math.max(-1f, Math.min(1f, factor));
    if (clamped == 0f) {
      releaseWidthBorrow();
      return;
    }
    widthBorrowFactor = clamped;
    widthMorph.start();
  }

  /**
   * Releases a group-driven width-ripple borrow — the morph reverses to {@code 0}, the painted
   * width returns to the natural width. Idempotent. Called by the hosting group on every segment
   * when the pressed segment releases.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void releaseWidthBorrow() {
    widthMorph.reverse();
  }

  // ----------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final int naturalBodyW = Math.max(1, effectiveBodyWidth());
    final int bodyH = Math.max(1, buttonSize.containerHeightPx());
    final Point bodyOrigin = bodyOrigin();
    final int arc = cornerRadiusPx();
    final boolean focused = isFocusOwner() && isEnabled();

    // #176 Phase 2 — press width morph. Paint-layer only; layout (and getPreferredSize) report
    // the natural width. The body is re-centered inside the natural footprint so the morph reads
    // as the button "pulling in" on press rather than shifting.
    //
    // #176 Phase 3 — additive group-driven width borrow. A standard ElwhaButtonGroup calls
    // startWidthBorrow / releaseWidthBorrow on every segment when one of its segments is
    // pressed; the two morphs combine linearly so an outright-pressed CLICKABLE in a group
    // shrinks by (press + borrow), a SELECTABLE-in-a-group shrinks by borrow only (the press
    // morph is suppressed for SELECTABLE per §5), and a solo button shrinks by press only.
    final float easedPress = easedPressProgress();
    final float easedWidth = widthMorph.progress();
    final int pressWidthDeltaPx = pressWidthDeltaPx(naturalBodyW);
    final int totalShrink =
        Math.round((easedPress + easedWidth * widthBorrowFactor) * pressWidthDeltaPx);
    final int paintedBodyW = Math.max(1, naturalBodyW - totalShrink);
    final int bodyXShift = (naturalBodyW - paintedBodyW) / 2;

    final ColorRole surfaceRole = effectiveSurfaceRole();
    final StateLayer overlay = activeOverlay();
    final ColorRole borderRole = effectiveBorderRole(focused);
    final float borderStroke = effectiveBorderWidth(focused);
    final CornerRadii morphedRadii = morphedRadii(paintedBodyW, bodyH, arc, easedPress);

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.translate(bodyOrigin.x + bodyXShift, bodyOrigin.y);

      if (variant == ButtonVariant.ELEVATED && isEnabled()) {
        // The shadow follows the press width / radius too — its shape tracks the surface body.
        // CornerRadii stores real radii; ShadowPainter wants a RoundRectangle2D arcWidth (corner
        // diameter), so double it — otherwise the shadow's corners are half the body's and the
        // squarer halo bulges past the pill ends (#218, cf. #199).
        ShadowPainter.paint(
            g2, paintedBodyW, bodyH, morphedRadii.largestPx() * 2, elevationLevel());
      }

      if (!isEnabled()) {
        final Graphics2D dim = (Graphics2D) g2.create();
        try {
          dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
          paintSurface(
              dim, paintedBodyW, bodyH, morphedRadii, surfaceRole, null, borderRole, borderStroke);
        } finally {
          dim.dispose();
        }
        paintContent(g2, paintedBodyW, bodyH, StateLayer.disabledContentOpacity());
        return;
      }

      paintSurface(
          g2, paintedBodyW, bodyH, morphedRadii, surfaceRole, overlay, borderRole, borderStroke);
      paintRippleLayer(g2, paintedBodyW, bodyH, morphedRadii);
      paintContent(g2, paintedBodyW, bodyH, 1f);
    } finally {
      g2.dispose();
    }
  }

  // Routes the surface paint through the per-corner SurfacePainter overload. The morph helper
  // gives us per-corner radii in every path now (the int-arc uniform path is gone here — the
  // CornerRadii overload's per-corner clamp produces a byte-identical render for a uniform
  // radius, so an ordinary button is unaffected).
  private void paintSurface(
      final Graphics2D g,
      final int w,
      final int h,
      final CornerRadii radii,
      final ColorRole surfaceRole,
      final StateLayer overlay,
      final ColorRole borderRole,
      final float borderStroke) {
    SurfacePainter.paint(g, w, h, radii, surfaceRole, overlay, borderRole, borderStroke);
  }

  private void paintRippleLayer(
      final Graphics2D g, final int w, final int h, final CornerRadii radii) {
    if (rippleProgress >= 1f || rippleOrigin == null) {
      return;
    }
    RipplePainter.paint(g, w, h, rippleOrigin, rippleProgress, radii, resolveForegroundColor());
  }

  // #176 Phase 2 — SELECTABLE buttons own their shape signal through the select-flip; firing the
  // press shape + width morph on top of an in-flight select-flip stacks too much motion on a
  // quick click. The press color overlay (StateLayer.PRESSED) still fires — that's tracked by
  // the `pressed` flag, not pressMorph. Design doc §5.
  private boolean firesPressMorph() {
    return interactionMode != ButtonInteractionMode.SELECTABLE;
  }

  // #176 Phase 2 — pick per-direction easing for press (M3 emphasized.decelerate going in,
  // emphasized.accelerate going out per design doc §3), and apply it to the press animator's
  // current progress. Returns 0 when no morph is in flight and the press isn't held.
  private float easedPressProgress() {
    final float progress = pressMorph.progress();
    if (progress <= 0f) {
      return 0f;
    }
    final Easing easing =
        pressMorph.target() >= 0.5f ? Easing.EMPHASIZED_DECELERATE : Easing.EMPHASIZED_ACCELERATE;
    return easing.ease(progress);
  }

  private int pressWidthDeltaPx(final int naturalBodyW) {
    return Math.max(PRESS_WIDTH_DELTA_FLOOR_PX, Math.round(naturalBodyW * PRESS_WIDTH_DELTA_RATIO));
  }

  // Composes the select shape flip and the press shape morph per design doc §5 — select first
  // (interpolate between unselected and selected radii through EASE_IN_OUT for a symmetric
  // toggle), then press (shrink each corner of the selected geometry by the press delta).
  //
  // Connected-segment mode (cornerRadii override set by ElwhaButtonGroup) bypasses the select
  // flip — the group owns the per-segment shape and supplies it directly; the animated
  // connected pill-pop is Phase 4. The press shrink still applies for press feedback.
  private CornerRadii morphedRadii(
      final int w, final int h, final int uniformArc, final float easedPress) {
    final CornerRadii base;
    if (cornerRadii != null) {
      // #176 Phase 4 — connected-segment animated pill-pop. Interpolate from the previous radii
      // to the current target through EASE_IN_OUT (symmetric so a slide-left and slide-right
      // read identically), driven by cornerRadiiMorph. Once the morph completes (progress = 1)
      // the interpolation collapses to the target. Static / first-set values bypass the
      // interpolation entirely (cornerRadiiFrom == null) so non-connected use of setCornerRadii
      // is unaffected.
      if (cornerRadiiFrom != null && cornerRadiiMorph.progress() < 1f) {
        base =
            ShapeMorphPainter.interpolate(
                cornerRadiiFrom, cornerRadii, cornerRadiiMorph.progress(), Easing.EASE_IN_OUT);
      } else {
        base = cornerRadii;
      }
    } else {
      final CornerRadii resting = uniformRadiiFor(shape, h, uniformArc);
      final CornerRadii selectedTarget = uniformRadiiFor(invertShape(shape), h, uniformArc);
      base =
          ShapeMorphPainter.interpolate(
              resting, selectedTarget, selectMorph.progress(), Easing.EASE_IN_OUT);
    }
    final int pressShrink = Math.round(easedPress * PRESS_RADIUS_DELTA_PX);
    if (pressShrink <= 0) {
      return base;
    }
    // #176 Phase 2 — only shrink corners that aren't already at "pill territory" (>= bodyH/2).
    // For a pill corner the radius is already at its maximum; subtracting 4 px would visibly
    // change the shape category from "pill" to "rounded-rect," which reads as shaving off the
    // pill rather than shrinking — most obvious on small ROUND buttons where the pill arc is
    // already small. For a pill, the width pinch alone is the press signal. Mixed-radii bodies
    // (connected segments mid-flip, etc.) shrink only the non-pill corners. Design doc §5.
    final int pillThreshold = h / 2;
    return CornerRadii.of(
        shrinkOrKeepPill(base.topLeftPx(), pressShrink, pillThreshold),
        shrinkOrKeepPill(base.topRightPx(), pressShrink, pillThreshold),
        shrinkOrKeepPill(base.bottomRightPx(), pressShrink, pillThreshold),
        shrinkOrKeepPill(base.bottomLeftPx(), pressShrink, pillThreshold));
  }

  private static int shrinkOrKeepPill(final int radius, final int shrink, final int pillThreshold) {
    return radius >= pillThreshold ? radius : Math.max(0, radius - shrink);
  }

  private CornerRadii uniformRadiiFor(
      final ButtonShape shapeKind, final int bodyH, final int uniformArc) {
    if (shapeKind == ButtonShape.ROUND) {
      // SurfacePainter clamps each corner to half the shorter body axis, so the body-height
      // / 2 value paints as a pill regardless of width.
      return CornerRadii.uniform(Math.max(0, bodyH / 2));
    }
    // The SQUARE arc the caller already resolved via cornerRadiusPx() for the resting shape;
    // for the inverted shape we recompute from the button's size token.
    return CornerRadii.uniform(shapeKind == shape ? uniformArc : buttonSize.squareCornerPx());
  }

  private static ButtonShape invertShape(final ButtonShape s) {
    return s == ButtonShape.ROUND ? ButtonShape.SQUARE : ButtonShape.ROUND;
  }

  private void paintContent(
      final Graphics2D g, final int bodyW, final int bodyH, final float alpha) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      if (alpha < 1f) {
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
      }
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setFont(buttonSize.typeRole().resolve());

      final FontMetrics fm = g2.getFontMetrics();
      final int labelW = (text == null || text.isEmpty()) ? 0 : fm.stringWidth(text);
      final int iconSlot = buttonSize.iconSizePx();
      final int iconGap = buttonSize.paddingWithIconGapPx();
      final Icon paintedIcon = currentIcon();
      final int contentW =
          (paintedIcon != null ? iconSlot + (labelW > 0 ? iconGap : 0) : 0) + labelW;
      int x = (bodyW - contentW) / 2;
      final int y = bodyH / 2;

      if (paintedIcon != null) {
        final int iconY = y - iconSlot / 2;
        paintedIcon.paintIcon(this, g2, x, iconY);
        x += iconSlot + (labelW > 0 ? iconGap : 0);
      }
      if (labelW > 0) {
        final int baseline = y + (fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(resolveForegroundColor());
        g2.drawString(text, x, baseline);
      }
    } finally {
      g2.dispose();
    }
  }

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
    if (selected
        && interactionMode == ButtonInteractionMode.SELECTABLE
        && (variant == ButtonVariant.ELEVATED || variant == ButtonVariant.FILLED_TONAL)) {
      return StateLayer.SELECTED;
    }
    return null;
  }

  // ----------------------------------------------------------- LAF / a11y

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    repaint();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    final Insets s = shadowReserve();
    final int rawW = bodyWidthPx() + s.left + s.right;
    final int rawH = buttonSize.containerHeightPx() + s.top + s.bottom;
    // §9 a11y target inflation — XS (32 dp) and S (40 dp) sit below the 48 dp WCAG touch target.
    // The component grows on the cross axis (height) to the target so the click hit area covers
    // the target; the visible body is centered inside via the bodyOriginY offset in
    // paintComponent. Width is not inflated — text buttons are always wide enough that the
    // cross-axis target carries the a11y guarantee.
    final int targetMin = buttonSize.minimumTargetPx();
    return new Dimension(rawW, Math.max(rawH, targetMin));
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaButton();
    }
    return accessibleContext;
  }

  /**
   * Accessible role: {@link AccessibleRole#PUSH_BUTTON} for {@code CLICKABLE}; {@link
   * AccessibleRole#TOGGLE_BUTTON} for {@code SELECTABLE}. Name resolution falls through label →
   * tooltip → component name → the literal {@code "Button"}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  protected class AccessibleElwhaButton extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return interactionMode == ButtonInteractionMode.SELECTABLE
          ? AccessibleRole.TOGGLE_BUTTON
          : AccessibleRole.PUSH_BUTTON;
    }

    @Override
    public String getAccessibleName() {
      if (text != null && !text.isEmpty()) {
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
      return "Button";
    }

    @Override
    public AccessibleStateSet getAccessibleStateSet() {
      final AccessibleStateSet states = super.getAccessibleStateSet();
      if (interactionMode == ButtonInteractionMode.SELECTABLE && selected) {
        states.add(AccessibleState.SELECTED);
      }
      return states;
    }
  }
}
