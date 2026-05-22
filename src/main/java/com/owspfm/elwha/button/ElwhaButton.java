package com.owspfm.elwha.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.ShadowPainter;
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

  private static final int DEFAULT_BORDER_WIDTH = 1;
  private static final float FOCUSED_BORDER_WIDTH = 2f;
  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;
  private static final int HOVER_POLL_INTERVAL_MS = 100;
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

  // State ------------------------------------------------------------------
  private boolean hovered;
  private boolean pressed;
  private boolean selected;

  // Ripple state -----------------------------------------------------------
  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;

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
   * Sets the leading icon. Pass {@code null} to clear.
   *
   * @param icon the leading icon, or {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaButton setIcon(final Icon icon) {
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
    this.icon = icon;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the leading icon, or {@code null}.
   *
   * @return the leading icon
   * @version v0.2.0
   * @since v0.2.0
   */
  public Icon getIcon() {
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
    this.cornerRadii = radii;
    revalidate();
    repaint();
    return this;
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
    if (this.pressed == pressed) {
      return this;
    }
    this.pressed = pressed;
    repaint();
    return this;
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
            pressed = false;
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
            pressed = true;
            requestFocusInWindow();
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
    super.removeNotify();
  }

  // ----------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final int bodyW = Math.max(1, effectiveBodyWidth());
    final int bodyH = Math.max(1, buttonSize.containerHeightPx());
    final Point bodyOrigin = bodyOrigin();
    final int arc = cornerRadiusPx();
    final boolean focused = isFocusOwner() && isEnabled();

    final ColorRole surfaceRole = effectiveSurfaceRole();
    final StateLayer overlay = activeOverlay();
    final ColorRole borderRole = effectiveBorderRole(focused);
    final float borderStroke = effectiveBorderWidth(focused);

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.translate(bodyOrigin.x, bodyOrigin.y);

      if (variant == ButtonVariant.ELEVATED && isEnabled()) {
        ShadowPainter.paint(g2, bodyW, bodyH, arc, elevationLevel());
      }

      if (!isEnabled()) {
        final Graphics2D dim = (Graphics2D) g2.create();
        try {
          dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
          paintSurface(dim, bodyW, bodyH, arc, surfaceRole, null, borderRole, borderStroke);
        } finally {
          dim.dispose();
        }
        paintContent(g2, bodyW, bodyH, StateLayer.disabledContentOpacity());
        return;
      }

      paintSurface(g2, bodyW, bodyH, arc, surfaceRole, overlay, borderRole, borderStroke);
      paintRippleLayer(g2, bodyW, bodyH, arc);
      paintContent(g2, bodyW, bodyH, 1f);
    } finally {
      g2.dispose();
    }
  }

  // Routes the surface paint through the per-corner SurfacePainter overload in connected-segment
  // mode, or the uniform int-arc overload otherwise. The int-arc path is byte-identical to the
  // pre-connected-mode rendering, so an ordinary button is unaffected.
  private void paintSurface(
      final Graphics2D g,
      final int w,
      final int h,
      final int arc,
      final ColorRole surfaceRole,
      final StateLayer overlay,
      final ColorRole borderRole,
      final float borderStroke) {
    if (cornerRadii != null) {
      SurfacePainter.paint(g, w, h, cornerRadii, surfaceRole, overlay, borderRole, borderStroke);
    } else {
      SurfacePainter.paint(g, w, h, arc, surfaceRole, overlay, borderRole, borderStroke);
    }
  }

  private void paintRippleLayer(final Graphics2D g, final int w, final int h, final int arc) {
    if (rippleProgress >= 1f || rippleOrigin == null) {
      return;
    }
    if (cornerRadii != null) {
      RipplePainter.paint(
          g, w, h, rippleOrigin, rippleProgress, cornerRadii, resolveForegroundColor());
    } else {
      RipplePainter.paint(g, w, h, rippleOrigin, rippleProgress, arc, resolveForegroundColor());
    }
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
      final int contentW = (icon != null ? iconSlot + (labelW > 0 ? iconGap : 0) : 0) + labelW;
      int x = (bodyW - contentW) / 2;
      final int y = bodyH / 2;

      if (icon != null) {
        final int iconY = y - iconSlot / 2;
        icon.paintIcon(this, g2, x, iconY);
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
