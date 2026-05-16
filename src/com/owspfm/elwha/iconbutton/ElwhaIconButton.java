package com.owspfm.elwha.iconbutton;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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
 * A token-native M3 icon-button primitive: a square, icon-only button with four emphasis variants
 * ({@link IconButtonVariant#FILLED} / {@link IconButtonVariant#FILLED_TONAL} / {@link
 * IconButtonVariant#OUTLINED} / {@link IconButtonVariant#STANDARD}), a {@link
 * IconButtonInteractionMode#CLICKABLE} / {@link IconButtonInteractionMode#SELECTABLE} interaction
 * axis, and a declarative {@code setIcons(resting, selected)} toggle pattern.
 *
 * <p><strong>Treatment + role, orthogonal.</strong> The variant declares the treatment and carries
 * default surface/border {@link ColorRole}s; the surface role is overridable per instance via
 * {@link #setSurfaceRole(ColorRole)}. The foreground is never set — it is always derived as the
 * {@code on}-pair of the effective surface role, falling back to {@link
 * ColorRole#ON_SURFACE_VARIANT} for the transparent variants. (The one per-state foreground swap: a
 * selected {@code STANDARD} button tints to {@link ColorRole#PRIMARY} since STANDARD has nothing
 * else to carry the selection signal.)
 *
 * <p><strong>Uniform selection model.</strong> Selection composites a 12% {@link
 * StateLayer#SELECTED} overlay across all variants, with the border swapping to {@link
 * ColorRole#PRIMARY} for non-STANDARD variants. This is the deliberate Elwha-uniform divergence
 * from M3's per-variant surface-color swap — see {@code docs/research/elwha-icon-button-design.md}
 * §10.
 *
 * <p><strong>Paint.</strong> The round-rect surface + optional border delegates to the shared
 * {@link SurfacePainter}; the icon paints centered on top, tinted to the resolved foreground via
 * the same {@link FlatSVGIcon.ColorFilter} mechanism the chip uses.
 *
 * <p><strong>Defaults.</strong> {@link IconButtonVariant#STANDARD} variant, {@link
 * IconButtonInteractionMode#CLICKABLE} mode, {@link ShapeScale#FULL} shape, {@link
 * IconButtonSize#M} (40 px container, 24 px icon), border width 1 (only visible when a border role
 * is set).
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaIconButton pin = new ElwhaIconButton(MaterialIcons.pushPin())
 *     .setIcons(MaterialIcons.pushPin(), MaterialIcons.pushPinFilled())
 *     .setInteractionMode(IconButtonInteractionMode.SELECTABLE);
 * pin.addActionListener(evt -> System.out.println("pinned: " + pin.isSelected()));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaIconButton extends JComponent {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  private static final IconButtonSize DEFAULT_SIZE = IconButtonSize.M;
  private static final int DEFAULT_BORDER_WIDTH = 1;
  private static final float FOCUSED_BORDER_WIDTH = 2f;
  private static final int HOVER_POLL_INTERVAL_MS = 100;

  // Configuration ----------------------------------------------------------
  private IconButtonVariant variant = IconButtonVariant.STANDARD;
  private IconButtonInteractionMode interactionMode = IconButtonInteractionMode.CLICKABLE;
  private ColorRole surfaceRoleOverride;
  private ShapeScale shape = ShapeScale.FULL;
  private IconButtonSize buttonSize = DEFAULT_SIZE;
  private int containerSize = DEFAULT_SIZE.containerPx();
  private int iconSize = DEFAULT_SIZE.iconPx();
  private int borderWidth = DEFAULT_BORDER_WIDTH;

  // State ------------------------------------------------------------------
  private boolean hovered;
  private boolean pressed;
  private boolean selected;
  private Icon restingIcon;
  private Icon selectedIcon;

  /**
   * Backup poll timer for hover-clear. Swing's {@code mouseExited} fires unreliably on macOS for
   * slow cursor exits; the timer queries the live cursor position while {@link #hovered} is true
   * and clears hover when the cursor has actually left the bounds. Same workaround {@code
   * ElwhaChip} uses.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private Timer hoverPollTimer;

  private final FlatSVGIcon.ColorFilter iconFilter =
      new FlatSVGIcon.ColorFilter(c -> resolveForegroundColor());

  private final List<ActionListener> actionListeners = new ArrayList<>();

  // ----------------------------------------------------------------- ctors

  /**
   * Creates an icon button with no icon installed. Call {@link #setIcon(Icon)} or {@link
   * #setIcons(Icon, Icon)} before adding to a UI.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton() {
    this(null);
  }

  /**
   * Creates an icon button with the given resting icon.
   *
   * @param icon the resting icon (may be {@code null} for deferred install)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton(final Icon icon) {
    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setFocusable(true);
    setIcon(icon);
    initInteraction();
  }

  // ---------------------------------------------------------- factory presets

  /**
   * Creates an M3 filled icon-button preset — {@link IconButtonVariant#FILLED}. Returns a fully
   * further-configurable button; every preset choice is overridable through the normal setters.
   *
   * @param icon the resting icon (may be {@code null} for deferred install)
   * @return a configured filled icon-button
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaIconButton filledIconButton(final Icon icon) {
    return new ElwhaIconButton(icon).setVariant(IconButtonVariant.FILLED);
  }

  /**
   * Creates an M3 filled-tonal icon-button preset — {@link IconButtonVariant#FILLED_TONAL}.
   *
   * @param icon the resting icon (may be {@code null} for deferred install)
   * @return a configured filled-tonal icon-button
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaIconButton filledTonalIconButton(final Icon icon) {
    return new ElwhaIconButton(icon).setVariant(IconButtonVariant.FILLED_TONAL);
  }

  /**
   * Creates an M3 outlined icon-button preset — {@link IconButtonVariant#OUTLINED}.
   *
   * @param icon the resting icon (may be {@code null} for deferred install)
   * @return a configured outlined icon-button
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaIconButton outlinedIconButton(final Icon icon) {
    return new ElwhaIconButton(icon).setVariant(IconButtonVariant.OUTLINED);
  }

  /**
   * Creates an M3 standard icon-button preset — {@link IconButtonVariant#STANDARD}, the
   * transparent/glyph-only treatment.
   *
   * @param icon the resting icon (may be {@code null} for deferred install)
   * @return a configured standard icon-button
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaIconButton standardIconButton(final Icon icon) {
    return new ElwhaIconButton(icon).setVariant(IconButtonVariant.STANDARD);
  }

  // -------------------------------------------------------------- variant

  /**
   * Sets the surface variant and triggers a repaint.
   *
   * @param variant the new variant; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setVariant(final IconButtonVariant variant) {
    if (variant == null || variant == this.variant) {
      return this;
    }
    this.variant = variant;
    repaint();
    return this;
  }

  /**
   * Returns the active variant.
   *
   * @return the active variant (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconButtonVariant getVariant() {
    return variant;
  }

  // ---------------------------------------------------------- interaction

  /**
   * Sets the interaction mode and triggers a repaint.
   *
   * @param mode the new mode; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setInteractionMode(final IconButtonInteractionMode mode) {
    if (mode == null || mode == this.interactionMode) {
      return this;
    }
    this.interactionMode = mode;
    repaint();
    return this;
  }

  /**
   * Returns the active interaction mode.
   *
   * @return the active interaction mode (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconButtonInteractionMode getInteractionMode() {
    return interactionMode;
  }

  // ---------------------------------------------------------------- icons

  /**
   * Sets the resting icon. The selected-state icon is cleared — call {@link #setIcons(Icon, Icon)}
   * to install both.
   *
   * @param icon the resting icon; {@code null} clears it
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setIcon(final Icon icon) {
    return setIcons(icon, null);
  }

  /**
   * Installs the resting / selected icon pair. The selected icon is rendered when {@link
   * #isSelected()} is true; if {@code selected} is {@code null}, the resting icon is rendered in
   * both states (and the selected indicator falls through to the state-layer overlay and — for
   * {@link IconButtonVariant#STANDARD} — the primary foreground tint).
   *
   * <p><strong>Press preview.</strong> The selected icon also flashes during a live press
   * regardless of interaction mode. For {@link IconButtonInteractionMode#CLICKABLE} buttons this is
   * pure tactile feedback (the fill momentarily punches in and reverts on release); for {@link
   * IconButtonInteractionMode#SELECTABLE} buttons it doubles as a toggle preview (the user sees the
   * toggled-on look on press, and on release the icon either stays filled if the toggle flipped on,
   * or returns to outline if the toggle flipped off — no flicker either way). Pass {@code null} for
   * {@code selected} to opt out (the press still composites a state-layer overlay regardless).
   *
   * @param resting the icon rendered when not selected and not being pressed
   * @param selected the icon rendered when selected or pressed, or {@code null} to reuse {@code
   *     resting}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setIcons(final Icon resting, final Icon selected) {
    applyIconColorFilter(resting);
    applyIconColorFilter(selected);
    this.restingIcon = resting;
    this.selectedIcon = selected;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the resting icon, or {@code null} if none is installed.
   *
   * @return the resting icon, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Icon getIcon() {
    return restingIcon;
  }

  /**
   * Returns the selected-state icon, or {@code null} if only the resting icon was installed.
   *
   * @return the selected icon, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Icon getSelectedIcon() {
    return selectedIcon;
  }

  private Icon currentIcon() {
    // Show the selected icon when the button is selected OR being pressed — the press case gives a
    // tactile press-preview for CLICKABLE buttons (the fill flashes during the click) and a
    // smooth-transition preview for SELECTABLE buttons (the fill appears on press and stays after
    // release if the toggle flipped on, or reverts to outline if the toggle flipped off).
    if (selectedIcon != null && (selected || pressed)) {
      return selectedIcon;
    }
    return restingIcon;
  }

  private void applyIconColorFilter(final Icon icon) {
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
  }

  // ----------------------------------------------------------- token setters

  /**
   * Overrides the variant's default surface role. Pass {@code null} to fall back to the variant's
   * default; the foreground re-pairs against the new effective surface's {@code on}-role.
   *
   * @param role the surface role, or {@code null} to clear the override
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setSurfaceRole(final ColorRole role) {
    this.surfaceRoleOverride = role;
    repaint();
    return this;
  }

  /**
   * Returns the effective surface role — the per-instance override if set, otherwise the variant's
   * default surface role ({@code null} for transparent variants without an override).
   *
   * @return the effective surface role, or {@code null} for transparent variants
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole getSurfaceRole() {
    return surfaceRoleOverride != null ? surfaceRoleOverride : variant.surfaceRole();
  }

  /**
   * Sets the corner-radius shape step and triggers a repaint. {@link ShapeScale#FULL} produces the
   * capsule look M3 spec defaults to; other steps produce squarer treatments.
   *
   * @param shape the shape step; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setShape(final ShapeScale shape) {
    if (shape == null) {
      return this;
    }
    this.shape = shape;
    repaint();
    return this;
  }

  /**
   * Returns the active shape step.
   *
   * @return the active shape step (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public ShapeScale getShape() {
    return shape;
  }

  /**
   * Sets the resting border-stroke width in pixels. Has no visible effect while the effective
   * border role is {@code null}. Focus bumps the effective stroke width to {@code 2}; the value set
   * here is only the resting width.
   *
   * @param px the stroke width, clamped to {@code >= 0}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setBorderWidth(final int px) {
    this.borderWidth = Math.max(0, px);
    repaint();
    return this;
  }

  /**
   * Returns the resting border-stroke width in pixels.
   *
   * @return the resting stroke width
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getBorderWidth() {
    return borderWidth;
  }

  // -------------------------------------------------------------- sizing

  /**
   * Sets both container and icon dimensions in one call from the {@link IconButtonSize} preset —
   * the primary sizing API, covering all five M3 size tiers.
   *
   * <p>The shape is not touched — selecting a size leaves the corner radius alone, so {@code
   * setButtonSize(L)} on a default button stays {@link ShapeScale#FULL} (the capsule); pair with
   * {@link #setShape} for a square treatment at any size.
   *
   * @param size the size preset; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setButtonSize(final IconButtonSize size) {
    if (size == null) {
      return this;
    }
    this.buttonSize = size;
    this.containerSize = size.containerPx();
    this.iconSize = size.iconPx();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active {@link IconButtonSize} preset. Note that the returned value reflects the
   * last preset selected via {@link #setButtonSize}; calling the lower-level {@link
   * #setContainerSize(int)} / {@link #setIconSize(int)} does not change it (those setters drive the
   * raw {@code containerSize} / {@code iconSize} fields directly, last-write-wins, but leave the
   * preset enum unchanged for {@link #getButtonSize} bookkeeping).
   *
   * @return the active size preset (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconButtonSize getButtonSize() {
    return buttonSize;
  }

  /**
   * Sets the container side length in pixels (the icon button is square). Lower-level escape hatch
   * — prefer {@link #setButtonSize(IconButtonSize)} for the M3-spec sizes. Both setters are
   * last-write-wins; a {@code setButtonSize} call after a {@code setContainerSize} call overrides
   * this value.
   *
   * @param px the container side length, clamped to {@code >= 1}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setContainerSize(final int px) {
    this.containerSize = Math.max(1, px);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the container side length in pixels.
   *
   * @return the container side length
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getContainerSize() {
    return containerSize;
  }

  /**
   * Sets the rendered icon size in pixels. Lower-level escape hatch — prefer {@link
   * #setButtonSize(IconButtonSize)} for the M3-spec sizes. The icon size is only a layout hint; the
   * actual rendered icon is whatever {@link Icon} was installed via {@link #setIcon(Icon)} / {@link
   * #setIcons(Icon, Icon)} (the {@link FlatSVGIcon}s {@link com.owspfm.elwha.icons.MaterialIcons}
   * returns already carry their own size).
   *
   * @param px the icon size, clamped to {@code >= 1}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setIconSize(final int px) {
    this.iconSize = Math.max(1, px);
    repaint();
    return this;
  }

  /**
   * Returns the icon size in pixels.
   *
   * @return the icon size
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getIconSize() {
    return iconSize;
  }

  // -------------------------------------------------------------- selected

  /**
   * Sets the selected state and fires a {@link #PROPERTY_SELECTED} property change.
   *
   * @param selected the new selected state
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setSelected(final boolean selected) {
    if (selected == this.selected) {
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
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Forces the hover visual state on or off. Primarily for visual-validation tools (the playground
   * pre-renders the hover column without requiring a live cursor) and snapshot tests — under normal
   * use the mouse listeners drive this automatically. A genuine {@code mouseExited} will clear a
   * forced-on state on its next firing.
   *
   * @param hovered whether to render the hover overlay
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setHovered(final boolean hovered) {
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
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaIconButton setPressed(final boolean pressed) {
    if (this.pressed == pressed) {
      return this;
    }
    this.pressed = pressed;
    repaint();
    return this;
  }

  // ------------------------------------------------------------- listeners

  /**
   * Registers an action listener that fires on click (CLICKABLE) or toggle (SELECTABLE).
   *
   * @param listener the listener; {@code null} is ignored
   * @version v0.1.0
   * @since v0.1.0
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
   * @version v0.1.0
   * @since v0.1.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  /**
   * Convenience: scopes a {@link PropertyChangeListener} to {@link #PROPERTY_SELECTED}.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addSelectionChangeListener(final PropertyChangeListener listener) {
    addPropertyChangeListener(PROPERTY_SELECTED, listener);
  }

  // ------------------------------------------------------------ foreground

  /**
   * Resolves the effective icon foreground per icon-button-design §3: {@link ColorRole#PRIMARY}
   * when {@code STANDARD + selected}; the {@code on}-pair of the effective surface role when the
   * surface role has one; {@link ColorRole#ON_SURFACE_VARIANT} otherwise.
   *
   * @return the resolved foreground color (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  protected Color resolveForegroundColor() {
    if (selected
        && variant == IconButtonVariant.STANDARD
        && interactionMode == IconButtonInteractionMode.SELECTABLE) {
      return ColorRole.PRIMARY.resolve();
    }
    final ColorRole effective = getSurfaceRole();
    if (effective != null) {
      return effective.on().orElse(ColorRole.ON_SURFACE_VARIANT).resolve();
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
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
            if (isCursorStillInsideButton(e)) {
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
            // toolbar contexts typically suppress click-focus (the toolbar action shouldn't pull
            // focus away from the document / list / editor being acted on). Tab navigation still
            // works regardless, since it's gated by isFocusable() not isRequestFocusEnabled().
            if (isRequestFocusEnabled()) {
              requestFocusInWindow();
            }
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
            activate(0);
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhaiconbutton.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhaiconbutton.activate");
    am.put("elwhaiconbutton.activate", activate);
  }

  private void activate(final int modifiers) {
    if (interactionMode == IconButtonInteractionMode.SELECTABLE) {
      setSelected(!selected);
    }
    final ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(evt);
    }
  }

  private boolean containsPoint(final Point point) {
    return point.x >= 0 && point.y >= 0 && point.x < getWidth() && point.y < getHeight();
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
    if (!containsPoint(local)) {
      hovered = false;
      pressed = false;
      stopHoverPolling();
      repaint();
    }
  }

  private boolean isCursorStillInsideButton(final MouseEvent event) {
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

  @Override
  public void removeNotify() {
    stopHoverPolling();
    super.removeNotify();
  }

  // --------------------------------------------------------------- painting

  @Override
  protected void paintComponent(final Graphics g) {
    final int w = getWidth();
    final int h = getHeight();
    final int arc = shape.px();
    final boolean focused = isFocusOwner() && isEnabled();

    final ColorRole surfaceRole = getSurfaceRole();
    final StateLayer overlay = activeOverlay();
    final ColorRole borderRole = effectiveBorderRole(focused);
    final float borderStroke = focused ? Math.max(borderWidth, FOCUSED_BORDER_WIDTH) : borderWidth;

    if (!isEnabled()) {
      // M3 disabled is a compositing pass (container at 12%, content at 38%) over the resolved
      // surface — not a tinted overlay. Matches the chip's disabled handling.
      final Graphics2D dim = (Graphics2D) g.create();
      try {
        dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
        SurfacePainter.paint(dim, w, h, arc, surfaceRole, null, borderRole, borderStroke);
      } finally {
        dim.dispose();
      }
      paintIcon(g, StateLayer.disabledContentOpacity());
      return;
    }

    SurfacePainter.paint((Graphics2D) g, w, h, arc, surfaceRole, overlay, borderRole, borderStroke);
    paintIcon(g, 1f);
  }

  private void paintIcon(final Graphics g, final float contentAlpha) {
    final Icon icon = currentIcon();
    if (icon == null) {
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      if (contentAlpha < 1f) {
        g2.setComposite(AlphaComposite.SrcOver.derive(contentAlpha));
      }
      final int x = (getWidth() - icon.getIconWidth()) / 2;
      final int y = (getHeight() - icon.getIconHeight()) / 2;
      icon.paintIcon(this, g2, x, y);
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
    if (selected && interactionMode == IconButtonInteractionMode.SELECTABLE) {
      return StateLayer.SELECTED;
    }
    return null;
  }

  /**
   * Returns the border role for the current state. Focus always swaps to {@link ColorRole#PRIMARY};
   * for non-STANDARD variants, selection also swaps to PRIMARY so the user reads the toggled state
   * without relying on the state-layer overlay alone. STANDARD is the M3 text-equivalent emphasis
   * level and intentionally suppresses the border in all states (the foreground primary tint in
   * {@link #resolveForegroundColor()} carries the selection signal instead, per icon-button-design
   * §3).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private ColorRole effectiveBorderRole(final boolean focused) {
    if (focused) {
      return ColorRole.PRIMARY;
    }
    if (selected
        && interactionMode == IconButtonInteractionMode.SELECTABLE
        && variant != IconButtonVariant.STANDARD) {
      return ColorRole.PRIMARY;
    }
    return variant.borderRole();
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
    return new Dimension(containerSize, containerSize);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaIconButton();
    }
    return accessibleContext;
  }

  /**
   * Accessible role: {@link AccessibleRole#PUSH_BUTTON} for {@code CLICKABLE}; {@link
   * AccessibleRole#TOGGLE_BUTTON} for {@code SELECTABLE}. Name resolution falls through tooltip →
   * component name → the literal {@code "Icon button"} — consumers SHOULD set one or the other on
   * every icon-only button or screen-reader users will hear nothing meaningful.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  protected class AccessibleElwhaIconButton extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return interactionMode == IconButtonInteractionMode.SELECTABLE
          ? AccessibleRole.TOGGLE_BUTTON
          : AccessibleRole.PUSH_BUTTON;
    }

    @Override
    public String getAccessibleName() {
      final String declared = super.getAccessibleName();
      if (declared != null && !declared.isEmpty()) {
        return declared;
      }
      final String tip = getToolTipText();
      if (tip != null && !tip.isEmpty()) {
        return tip;
      }
      final String name = getName();
      if (name != null && !name.isEmpty()) {
        return name;
      }
      return "Icon button";
    }
  }
}
