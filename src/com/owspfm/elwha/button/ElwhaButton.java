package com.owspfm.elwha.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.SurfacePainter;
import com.owspfm.elwha.theme.TypeRole;
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
 * A token-native M3 Expressive text-button primitive: a rounded-rect text button with five emphasis
 * variants ({@link ButtonVariant#ELEVATED} / {@link ButtonVariant#FILLED} / {@link
 * ButtonVariant#FILLED_TONAL} / {@link ButtonVariant#OUTLINED} / {@link ButtonVariant#TEXT}), a
 * {@link ButtonInteractionMode#CLICKABLE} / {@link ButtonInteractionMode#SELECTABLE} interaction
 * axis, two shape options ({@link ButtonShape#ROUND} / {@link ButtonShape#SQUARE}), and the hybrid
 * selection color model from {@code docs/research/elwha-button-design.md} §7.
 *
 * <p><strong>Story 3 hardcodes Small dimensions</strong> (40 dp container height, 16 dp horizontal
 * padding, 8 dp icon→label gap, 20 dp icon size, 12 dp square corner). Story 4 (#117) generalizes
 * the {@code ButtonSize} axis.
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
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public class ElwhaButton extends JComponent {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  /** Story 3 hardcodes Small height per design doc Appendix A. */
  private static final int SMALL_HEIGHT_PX = 40;

  /** Story 3 hardcodes Small horizontal padding per design doc Appendix A. */
  private static final int SMALL_HORIZ_PADDING_PX = 16;

  /** Story 3 hardcodes Small icon→label gap per design doc Appendix A. */
  private static final int SMALL_ICON_GAP_PX = 8;

  /** Story 3 hardcodes Small icon size per design doc Appendix A. */
  private static final int SMALL_ICON_PX = 20;

  /** Story 3 hardcodes Small square corner arcWidth per design doc Appendix B. */
  private static final int SMALL_SQUARE_ARC = 12;

  /** A11y target inflation minimum per design doc §9 (WCAG 2.5.5). */
  private static final int A11Y_TARGET_MIN_PX = 48;

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
  private ColorRole surfaceRoleOverride;
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
    return shape == ButtonShape.ROUND ? Integer.MAX_VALUE : SMALL_SQUARE_ARC;
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
      return SMALL_HORIZ_PADDING_PX
          + SMALL_ICON_PX
          + SMALL_ICON_GAP_PX
          + labelW
          + SMALL_HORIZ_PADDING_PX;
    }
    return SMALL_HORIZ_PADDING_PX + labelW + SMALL_HORIZ_PADDING_PX;
  }

  private int labelWidthPx() {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    final FontMetrics fm = getFontMetrics(TypeRole.LABEL_LARGE.resolve());
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
            if (!containsBodyPoint(e.getPoint())) {
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
            if (containsBodyPoint(e.getPoint())) {
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
            // Center-of-body ripple for keyboard activation.
            final Insets s = shadowReserve();
            final int bodyW = Math.max(1, getWidth() - s.left - s.right);
            final int bodyH = Math.max(1, getHeight() - s.top - s.bottom);
            startRipple(new Point(bodyW / 2, bodyH / 2));
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

  private boolean containsBodyPoint(final Point componentPoint) {
    final Insets s = shadowReserve();
    final int bodyW = getWidth() - s.left - s.right;
    final int bodyH = getHeight() - s.top - s.bottom;
    final int x = componentPoint.x - s.left;
    final int y = componentPoint.y - s.top;
    return x >= 0 && y >= 0 && x < bodyW && y < bodyH;
  }

  private Point toBodyPoint(final Point componentPoint) {
    final Insets s = shadowReserve();
    return new Point(componentPoint.x - s.left, componentPoint.y - s.top);
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
    if (!containsBodyPoint(local)) {
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
    return containsBodyPoint(local);
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
    final Insets s = shadowReserve();
    final int bodyW = Math.max(1, getWidth() - s.left - s.right);
    final int bodyH = Math.max(1, getHeight() - s.top - s.bottom);
    final int arc = cornerRadiusPx();
    final boolean focused = isFocusOwner() && isEnabled();

    final ColorRole surfaceRole = effectiveSurfaceRole();
    final StateLayer overlay = activeOverlay();
    final ColorRole borderRole = effectiveBorderRole(focused);
    final float borderStroke = effectiveBorderWidth(focused);

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.translate(s.left, s.top);

      if (variant == ButtonVariant.ELEVATED && isEnabled()) {
        ShadowPainter.paint(g2, bodyW, bodyH, arc, elevationLevel());
      }

      if (!isEnabled()) {
        final Graphics2D dim = (Graphics2D) g2.create();
        try {
          dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
          SurfacePainter.paint(dim, bodyW, bodyH, arc, surfaceRole, null, borderRole, borderStroke);
        } finally {
          dim.dispose();
        }
        paintContent(g2, bodyW, bodyH, StateLayer.disabledContentOpacity());
        return;
      }

      SurfacePainter.paint(g2, bodyW, bodyH, arc, surfaceRole, overlay, borderRole, borderStroke);

      if (rippleProgress < 1f && rippleOrigin != null) {
        RipplePainter.paint(
            g2, bodyW, bodyH, rippleOrigin, rippleProgress, arc, resolveForegroundColor());
      }

      paintContent(g2, bodyW, bodyH, 1f);
    } finally {
      g2.dispose();
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
      g2.setFont(TypeRole.LABEL_LARGE.resolve());

      final FontMetrics fm = g2.getFontMetrics();
      final int labelW = (text == null || text.isEmpty()) ? 0 : fm.stringWidth(text);
      final int contentW =
          (icon != null ? SMALL_ICON_PX + (labelW > 0 ? SMALL_ICON_GAP_PX : 0) : 0) + labelW;
      int x = (bodyW - contentW) / 2;
      final int y = bodyH / 2;

      if (icon != null) {
        final int iconY = y - SMALL_ICON_PX / 2;
        icon.paintIcon(this, g2, x, iconY);
        x += SMALL_ICON_PX + (labelW > 0 ? SMALL_ICON_GAP_PX : 0);
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
    return new Dimension(bodyWidthPx() + s.left + s.right, SMALL_HEIGHT_PX + s.top + s.bottom);
  }

  @Override
  public Dimension getMinimumSize() {
    final Dimension pref = getPreferredSize();
    // §9 a11y target inflation — Small is 40 dp, below the 48 dp minimum. Layout managers that
    // honor minimumSize will give the button the inflated cross-axis target; the visible body
    // remains at 40.
    return new Dimension(
        Math.max(pref.width, A11Y_TARGET_MIN_PX), Math.max(pref.height, A11Y_TARGET_MIN_PX));
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
  }
}
