package com.owspfm.elwha.chip;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.function.Consumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A token-native chip primitive: a single-row leading-icon + text + optional trailing icon-button
 * surface, styled entirely from the Elwha design tokens.
 *
 * <p><strong>Treatment + role, orthogonal.</strong> The {@link ChipVariant} declares the
 * <em>treatment</em> (filled / outlined / ghost) and carries the {@link ColorRole}s it resolves to.
 * The surface role is independently overridable per instance via {@link #setSurfaceRole(ColorRole)}
 * — for example, {@code FILLED + setSurfaceRole(SECONDARY_CONTAINER)} gives the secondary-container
 * look without bundling a color into the variant.
 *
 * <p><strong>Foreground is derived, never set.</strong> Text and icons paint in the {@code on}-pair
 * of the effective surface role (per the {@link ColorRole#on()} facade), so foreground stays
 * correct by construction across every variant, palette, and runtime theme switch. There is no
 * per-instance foreground setter.
 *
 * <p><strong>State layers are uniform.</strong> Hover / pressed / selected feedback is composited
 * via the {@link StateLayer} model at uniform opacities — 8% hover, 10% pressed, 12% selected —
 * tinted by the surface's {@code on}-role.
 *
 * <p><strong>M3 chip-type sugar.</strong> The four canonical Material 3 chip patterns ship as
 * factory presets over the orthogonal axes — see {@link #assistChip(String)}, {@link
 * #filterChip(String)}, {@link #inputChip(String, Runnable)}, {@link #suggestionChip(String)} — not
 * as a rigid {@code ChipType} enum.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaChip chip = ElwhaChip.filterChip("Demand")
 *     .setLeadingIcon(icon);
 * chip.addActionListener(evt -> System.out.println("toggled: " + chip.isSelected()));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaChip extends JPanel {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  private static final int DEFAULT_INNER_GAP = 6;
  private static final int DEFAULT_BORDER_WIDTH = 1;
  private static final float FOCUSED_BORDER_WIDTH = 2f;

  /**
   * Minimum hit-target side for the leading and trailing inline buttons. Keeps a 14px glyph
   * clickable at a reasonable 22×22 area without inflating the visible glyph itself.
   */
  private static final int BUTTON_MIN_HIT_TARGET = 22;

  // Configuration ----------------------------------------------------------
  private ChipVariant variant = ChipVariant.FILLED;
  private ChipInteractionMode interactionMode = ChipInteractionMode.STATIC;
  private ColorRole surfaceRoleOverride;
  private ShapeScale shape = ShapeScale.SM;
  private SpaceScale paddingVertical = SpaceScale.XS;
  private SpaceScale paddingHorizontal = SpaceScale.MD;
  private int borderWidth = DEFAULT_BORDER_WIDTH;

  // State ------------------------------------------------------------------
  private boolean hovered;
  private boolean pressed;
  private boolean selected;

  /**
   * Backup poll timer for hover-clear: Swing's {@code mouseExited} fires unreliably on macOS, and
   * even when it fires, boundary-precision can leave the live cursor "just inside" the chip for
   * slow exits. The timer queries the current cursor position every {@link
   * #HOVER_POLL_INTERVAL_MS}ms while {@link #hovered} is true and clears hover when it confirms the
   * cursor is outside the chip. Started in {@code mouseEntered}, stops itself once hover clears or
   * the chip leaves the hierarchy.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private Timer hoverPollTimer;

  private static final int HOVER_POLL_INTERVAL_MS = 100;

  // Subcomponents ----------------------------------------------------------
  private final LeadingButton leadingButton;
  private final JLabel leadingIconLabel;
  private final JLabel textLabel;
  private final TrailingIconButton trailingButton;
  private final JPanel leadingCluster;
  private final JPanel contentRow;

  // Leading-affordance state -----------------------------------------------
  private Icon leadingAffordanceIdleIcon;
  private Icon leadingAffordanceActiveIcon;
  private boolean leadingAffordanceActiveState;
  private boolean leadingAffordanceHoverRevealIdle;

  // Icon recoloring --------------------------------------------------------
  // Chip-local color filter for FlatSVGIcons in the leading / trailing slots. The filter's
  // function is invoked at paint time, so a foreground-resolution change (selection, theme
  // switch, surface override, etc.) re-tints the icons on the next repaint with no manual
  // refresh.
  private final FlatSVGIcon.ColorFilter iconFilter =
      new FlatSVGIcon.ColorFilter(c -> resolveForegroundColor());

  // Listeners --------------------------------------------------------------
  private final List<ActionListener> actionListeners = new ArrayList<>();
  private Consumer<MouseEvent> contextMenuCallback;

  // ----------------------------------------------------------------- ctor

  /** Creates a chip with empty text and the default {@link ChipVariant#FILLED} treatment. */
  public ElwhaChip() {
    this("");
  }

  /**
   * Creates a chip with the given text.
   *
   * @param text the chip label
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip(final String text) {
    super(new BorderLayout());
    setOpaque(false);

    leadingButton = new LeadingButton();
    leadingButton.setVisible(false);

    leadingIconLabel = new JLabel();
    leadingIconLabel.setVisible(false);

    textLabel = new ChipTextLabel(text == null ? "" : text);
    textLabel.setHorizontalAlignment(SwingConstants.LEADING);

    trailingButton = new TrailingIconButton();
    trailingButton.setVisible(false);

    leadingCluster =
        new JPanel(new BaselineCenteringFlowLayout(DEFAULT_INNER_GAP, BUTTON_MIN_HIT_TARGET));
    leadingCluster.setOpaque(false);
    leadingCluster.add(leadingButton);
    leadingCluster.add(leadingIconLabel);
    leadingCluster.add(textLabel);

    contentRow = new JPanel(new BorderLayout(0, 0));
    contentRow.setOpaque(false);
    contentRow.add(leadingCluster, BorderLayout.WEST);
    contentRow.add(trailingButton, BorderLayout.EAST);
    add(contentRow, BorderLayout.CENTER);

    rebuildBorder();
    initInteraction();
  }

  // ---------------------------------------------------------- factory presets

  /**
   * Creates an M3 assist-chip preset — {@link ChipInteractionMode#CLICKABLE} + {@link
   * ChipVariant#OUTLINED}. Returns a fully further-configurable chip; every preset choice is
   * overridable through the normal setters.
   *
   * @param text the chip label
   * @return a configured assist-chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaChip assistChip(final String text) {
    return new ElwhaChip(text)
        .setVariant(ChipVariant.OUTLINED)
        .setInteractionMode(ChipInteractionMode.CLICKABLE);
  }

  /**
   * Creates an M3 filter-chip preset — {@link ChipInteractionMode#SELECTABLE} + {@link
   * ChipVariant#OUTLINED}.
   *
   * @param text the chip label
   * @return a configured filter-chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaChip filterChip(final String text) {
    return new ElwhaChip(text)
        .setVariant(ChipVariant.OUTLINED)
        .setInteractionMode(ChipInteractionMode.SELECTABLE);
  }

  /**
   * Creates an M3 input-chip preset — {@link ChipInteractionMode#CLICKABLE} + {@link
   * ChipVariant#OUTLINED} with a trailing remove (×) affordance wired to {@code onRemove}.
   *
   * @param text the chip label
   * @param onRemove invoked when the user clicks the trailing remove button; may be {@code null} to
   *     suppress the click but still render the affordance
   * @return a configured input-chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaChip inputChip(final String text, final Runnable onRemove) {
    ElwhaChip chip =
        new ElwhaChip(text)
            .setVariant(ChipVariant.OUTLINED)
            .setInteractionMode(ChipInteractionMode.CLICKABLE);
    chip.setTrailingIcon(new RemoveGlyphIcon(), "Remove", onRemove);
    return chip;
  }

  /**
   * Creates an M3 suggestion-chip preset — {@link ChipInteractionMode#CLICKABLE} + {@link
   * ChipVariant#OUTLINED}.
   *
   * @param text the chip label
   * @return a configured suggestion-chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaChip suggestionChip(final String text) {
    return new ElwhaChip(text)
        .setVariant(ChipVariant.OUTLINED)
        .setInteractionMode(ChipInteractionMode.CLICKABLE);
  }

  // ----------------------------------------------------------------- text

  /**
   * Returns the current chip text.
   *
   * @return the current text (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public String getText() {
    return textLabel.getText() == null ? "" : textLabel.getText();
  }

  /**
   * Sets the chip text.
   *
   * @param text label text; null is treated as the empty string
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setText(final String text) {
    textLabel.setText(text == null ? "" : text);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the live text label for font / FlatLaf style-class customization. The foreground color
   * is owned by the chip's token-resolved foreground — overriding it via {@code setForeground} on
   * the returned label re-introduces unpaired surface/foreground and is not supported.
   *
   * @return the text label (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public JLabel getTextLabel() {
    return textLabel;
  }

  // -------------------------------------------------------------- variant

  /**
   * Sets the surface variant.
   *
   * @param variant the new variant; ignored if null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setVariant(final ChipVariant variant) {
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
   * @return the active variant (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ChipVariant getVariant() {
    return variant;
  }

  // ---------------------------------------------------------- interaction

  /**
   * Sets the interaction mode.
   *
   * @param mode the new mode; ignored if null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setInteractionMode(final ChipInteractionMode mode) {
    if (mode == null || mode == interactionMode) {
      return this;
    }
    interactionMode = mode;
    setFocusable(mode == ChipInteractionMode.CLICKABLE || mode == ChipInteractionMode.SELECTABLE);
    setCursor(
        mode == ChipInteractionMode.STATIC
            ? Cursor.getDefaultCursor()
            : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    repaint();
    return this;
  }

  /**
   * Returns the active interaction mode.
   *
   * @return the active interaction mode (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ChipInteractionMode getInteractionMode() {
    return interactionMode;
  }

  // ----------------------------------------------------------- slot setters

  /**
   * Replaces the leading icon (display-only — see {@link #setLeadingAffordance} for a clickable
   * alternative).
   *
   * @param icon the icon; null clears
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setLeadingIcon(final Icon icon) {
    applyIconColorFilter(icon);
    leadingIconLabel.setIcon(icon);
    // Don't overwrite the leading-button's claim on the slot: when an affordance is active, the
    // button owns the leading position and the icon-label stays hidden until the affordance is
    // cleared.
    if (!leadingButton.isVisible()) {
      leadingIconLabel.setVisible(icon != null);
    }
    revalidate();
    repaint();
    return this;
  }

  /**
   * Re-binds an SVG icon's color filter to this chip's foreground resolver, so the icon paints in
   * the chip's token-resolved foreground rather than whatever filter the caller supplied —
   * typically {@link com.owspfm.elwha.icons.MaterialIcons}'s app-wide {@code Label.foreground}
   * filter, which is the right default outside a chip but wrong inside one with a custom surface.
   *
   * <p>No-op for non-SVG icons (raster bitmaps and the like — those don't have a color filter
   * concept and stay whatever color they were authored).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void applyIconColorFilter(final Icon icon) {
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(iconFilter);
    }
  }

  /**
   * Installs (or clears) a clickable leading-slot affordance with two visual states.
   *
   * @param idleIcon icon when the affordance is in the idle / off state
   * @param activeIcon icon when the affordance is active / on (falls back to idle when null)
   * @param active whether the affordance is currently active
   * @param hoverRevealIdle when true and not active, hide the idle icon until chip hover
   * @param tooltip tooltip text; null suppresses
   * @param onClick click handler; null disables clicks but the slot still reserves its area
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setLeadingAffordance(
      final Icon idleIcon,
      final Icon activeIcon,
      final boolean active,
      final boolean hoverRevealIdle,
      final String tooltip,
      final Runnable onClick) {
    leadingAffordanceIdleIcon = idleIcon;
    leadingAffordanceActiveIcon = activeIcon;
    leadingAffordanceActiveState = active;
    leadingAffordanceHoverRevealIdle = hoverRevealIdle;
    if (idleIcon == null && activeIcon == null) {
      leadingButton.setVisible(false);
      leadingButton.setOnClick(null);
      leadingButton.setToolTipText(null);
      leadingIconLabel.setVisible(leadingIconLabel.getIcon() != null);
      revalidate();
      repaint();
      return this;
    }
    applyIconColorFilter(idleIcon);
    applyIconColorFilter(activeIcon);
    leadingIconLabel.setVisible(false);
    leadingButton.setVisible(true);
    leadingButton.setOnClick(onClick);
    leadingButton.setToolTipText(tooltip);
    refreshLeadingAffordanceIcon();
    revalidate();
    repaint();
    return this;
  }

  private void refreshLeadingAffordanceIcon() {
    if (!leadingButton.isVisible()) {
      return;
    }
    final Icon next;
    if (leadingAffordanceActiveState) {
      next =
          leadingAffordanceActiveIcon != null
              ? leadingAffordanceActiveIcon
              : leadingAffordanceIdleIcon;
    } else if (leadingAffordanceHoverRevealIdle) {
      next = hovered ? leadingAffordanceIdleIcon : null;
    } else {
      next = leadingAffordanceIdleIcon;
    }
    leadingButton.setRenderedIcon(next);
  }

  /**
   * Installs an {@link Action}-bound trailing icon button. The button has its own hover and press
   * states; its click does <em>not</em> bubble up to the chip's own action listeners.
   *
   * @param action the action backing the trailing button; null clears
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setTrailingAction(final Action action) {
    if (action != null && action.getValue(Action.SMALL_ICON) instanceof Icon icon) {
      applyIconColorFilter(icon);
    }
    trailingButton.setAction(action);
    trailingButton.setVisible(action != null);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the trailing icon-button if installed. Useful for callers that want to bind keystrokes
   * or test-drive the button programmatically.
   *
   * @return the trailing button (always present, may be invisible)
   * @version v0.1.0
   * @since v0.1.0
   */
  public JComponent getTrailingButton() {
    return trailingButton;
  }

  // ---------------------------------------------------------- context menu

  /**
   * Installs a callback invoked when the user requests a context menu on the chip body.
   *
   * @param callback the callback, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setContextMenuCallback(final Consumer<MouseEvent> callback) {
    contextMenuCallback = callback;
    return this;
  }

  /**
   * Convenience: attaches a {@link JPopupMenu} that pops up at the click point on right-click /
   * VK_CONTEXT_MENU / Shift+F10. Pass null to clear.
   *
   * @param popup the popup to attach, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip attachContextMenu(final JPopupMenu popup) {
    if (popup == null) {
      contextMenuCallback = null;
      return this;
    }
    contextMenuCallback =
        evt -> {
          final Component src = evt.getComponent();
          if (src == null || !src.isShowing()) {
            popup.show(this, getWidth() / 2, getHeight() / 2);
          } else {
            popup.show(src, evt.getX(), evt.getY());
          }
        };
    return this;
  }

  /**
   * Convenience: attaches a {@code Supplier<JPopupMenu>} so callers can build the menu lazily.
   *
   * @param supplier the supplier; null clears
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip attachContextMenu(final java.util.function.Supplier<JPopupMenu> supplier) {
    if (supplier == null) {
      contextMenuCallback = null;
      return this;
    }
    contextMenuCallback =
        evt -> {
          final JPopupMenu p = supplier.get();
          if (p == null) {
            return;
          }
          final Component src = evt.getComponent();
          if (src == null || !src.isShowing()) {
            p.show(this, getWidth() / 2, getHeight() / 2);
          } else {
            p.show(src, evt.getX(), evt.getY());
          }
        };
    return this;
  }

  /**
   * Convenience overload of {@link #setTrailingAction(Action)}: installs a click-handling icon
   * button bound to the supplied icon, tooltip, and runnable.
   *
   * @param icon the icon shown in the trailing slot (null hides the trailing button)
   * @param tooltip optional tooltip text (null suppresses)
   * @param onClick callback invoked when the button is clicked
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setTrailingIcon(final Icon icon, final String tooltip, final Runnable onClick) {
    if (icon == null) {
      return setTrailingAction(null);
    }
    final AbstractAction action =
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (onClick != null) {
              onClick.run();
            }
          }
        };
    action.putValue(Action.SMALL_ICON, icon);
    if (tooltip != null) {
      action.putValue(Action.SHORT_DESCRIPTION, tooltip);
    }
    return setTrailingAction(action);
  }

  // --------------------------------------------------------------- selected

  /**
   * Sets the selected state. Fires a {@link #PROPERTY_SELECTED} property change.
   *
   * @param selected the new selection state
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setSelected(final boolean selected) {
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
   * Returns the current selection state.
   *
   * @return current selection state
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isSelected() {
    return selected;
  }

  // ----------------------------------------------------------- token setters

  /**
   * Overrides the variant's default surface role. Pass null to fall back to the variant's surface
   * role. The foreground re-pairs automatically against the effective surface role's {@code
   * on}-pair.
   *
   * @param role the surface role, or null to clear the override
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setSurfaceRole(final ColorRole role) {
    surfaceRoleOverride = role;
    repaint();
    return this;
  }

  /**
   * Returns the effective surface role for this chip — the per-instance override if set, otherwise
   * the variant's default surface role (which is {@code null} for {@link ChipVariant#GHOST}).
   *
   * @return the effective surface role, or {@code null} for transparent variants without an
   *     override
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole getSurfaceRole() {
    return surfaceRoleOverride != null ? surfaceRoleOverride : variant.surfaceRole();
  }

  /**
   * Sets the corner-radius shape step. Pass null to fall back to the default ({@link
   * ShapeScale#SM}, per the chip taxonomy decision); {@link ShapeScale#FULL} gives the legacy
   * capsule look.
   *
   * @param shape the shape step, or null for default
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setShape(final ShapeScale shape) {
    this.shape = shape != null ? shape : ShapeScale.SM;
    repaint();
    return this;
  }

  /**
   * Returns the active shape step.
   *
   * @return the active shape step (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ShapeScale getShape() {
    return shape;
  }

  /**
   * Sets the chip's symmetric padding from the spacing scale — {@code horizontal} on the left and
   * right, {@code vertical} on the top and bottom.
   *
   * @param horizontal the left/right padding step (null ignored)
   * @param vertical the top/bottom padding step (null ignored)
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setPadding(final SpaceScale horizontal, final SpaceScale vertical) {
    if (horizontal != null) {
      paddingHorizontal = horizontal;
    }
    if (vertical != null) {
      paddingVertical = vertical;
    }
    rebuildBorder();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active padding as resolved {@link Insets}. Defensive copy.
   *
   * @return the active padding
   * @version v0.1.0
   * @since v0.1.0
   */
  public Insets getPadding() {
    return SpaceScale.insets(paddingVertical, paddingHorizontal);
  }

  /**
   * Sets the border thickness in pixels.
   *
   * @param width the width, clamped to {@code >= 0}
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip setBorderWidth(final int width) {
    borderWidth = Math.max(0, width);
    repaint();
    return this;
  }

  /**
   * Returns the resting border-stroke width in pixels. Focus bumps the effective stroke width to
   * {@code 2}; the value returned here is only the resting width.
   *
   * @return the resting stroke width
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getBorderWidth() {
    return borderWidth;
  }

  /**
   * Resolves the effective text + icon foreground color — the {@code on}-pair of the effective
   * surface role (or {@link ColorRole#ON_SURFACE} when the surface role is null / unpaired). Always
   * correct by construction; there is no per-instance foreground override.
   *
   * @return the resolved foreground color (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  protected Color resolveForegroundColor() {
    ColorRole basis = getSurfaceRole();
    if (basis == null) {
      basis = ColorRole.SURFACE;
    }
    return basis.on().orElse(ColorRole.ON_SURFACE).resolve();
  }

  // ------------------------------------------------------------ listeners

  /**
   * Installs a mouse listener on both the chip body and the inner content row.
   *
   * @param listener the listener to install
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addChipMouseListener(final java.awt.event.MouseListener listener) {
    if (listener == null) {
      return;
    }
    addMouseListener(listener);
    contentRow.addMouseListener(listener);
    leadingCluster.addMouseListener(listener);
  }

  /**
   * Mirror of {@link #addChipMouseListener(java.awt.event.MouseListener)} for {@link
   * java.awt.event.MouseMotionListener}.
   *
   * @param listener the listener to install
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addChipMouseMotionListener(final java.awt.event.MouseMotionListener listener) {
    if (listener == null) {
      return;
    }
    addMouseMotionListener(listener);
    contentRow.addMouseMotionListener(listener);
    leadingCluster.addMouseMotionListener(listener);
  }

  /**
   * Registers an action listener that fires on click (clickable) or toggle (selectable).
   *
   * @param listener the listener; null is ignored
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

  /**
   * Reports whether a context-menu callback (or attached popup) is installed.
   *
   * @return true if any context-menu callback is installed
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean hasContextMenu() {
    return contextMenuCallback != null;
  }

  /**
   * Cancels any in-flight press that's been seen by mousePressed but not yet completed by
   * mouseReleased. Hosting containers that convert presses into drags should call this when the
   * drag activates so the pending click doesn't fire on release.
   *
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaChip cancelPendingClick() {
    pressed = false;
    repaint();
    return this;
  }

  // ----------------------------------------------------------- interaction

  private void initInteraction() {
    final MouseAdapter ma =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            if (interactionMode != ChipInteractionMode.STATIC && isEnabled()) {
              hovered = true;
              refreshLeadingAffordanceIcon();
              repaint();
              ensureHoverPolling();
            }
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            if (isCursorStillInsideChip(e)) {
              return;
            }
            hovered = false;
            pressed = false;
            refreshLeadingAffordanceIcon();
            stopHoverPolling();
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (e.isPopupTrigger()) {
              maybeShowContextMenu(e);
              return;
            }
            if (isFromInlineButton(e)) {
              return;
            }
            if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            if (interactionMode == ChipInteractionMode.CLICKABLE
                || interactionMode == ChipInteractionMode.SELECTABLE) {
              pressed = true;
              requestFocusInWindow();
              repaint();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (e.isPopupTrigger()) {
              maybeShowContextMenu(e);
              return;
            }
            if (isFromInlineButton(e)) {
              return;
            }
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
    contentRow.addMouseListener(ma);
    leadingCluster.addMouseListener(ma);
    leadingButton.addMouseListener(ma);
    trailingButton.addMouseListener(ma);

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
    final Action contextMenu =
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (contextMenuCallback == null || !isEnabled()) {
              return;
            }
            final Point p = new Point(getWidth() / 2, getHeight() / 2);
            final MouseEvent synthetic =
                new MouseEvent(
                    ElwhaChip.this,
                    MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(),
                    0,
                    p.x,
                    p.y,
                    1,
                    true,
                    MouseEvent.BUTTON3);
            contextMenuCallback.accept(synthetic);
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhachip.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhachip.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "elwhachip.contextMenu");
    im.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F10, java.awt.event.InputEvent.SHIFT_DOWN_MASK),
        "elwhachip.contextMenu");
    am.put("elwhachip.activate", activate);
    am.put("elwhachip.contextMenu", contextMenu);
  }

  private void activate(final int modifiers) {
    if (interactionMode == ChipInteractionMode.SELECTABLE) {
      setSelected(!selected);
    }
    if (interactionMode == ChipInteractionMode.CLICKABLE
        || interactionMode == ChipInteractionMode.SELECTABLE) {
      final ActionEvent evt =
          new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
      for (ActionListener l : new ArrayList<>(actionListeners)) {
        l.actionPerformed(evt);
      }
    }
  }

  private void maybeShowContextMenu(final MouseEvent event) {
    if (contextMenuCallback != null) {
      contextMenuCallback.accept(event);
    }
  }

  private boolean containsPoint(final Point point) {
    return point.x >= 0 && point.y >= 0 && point.x < getWidth() && point.y < getHeight();
  }

  private boolean isFromInlineButton(final MouseEvent event) {
    final Object src = event.getSource();
    return src == leadingButton || src == trailingButton;
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
      refreshLeadingAffordanceIcon();
      stopHoverPolling();
      return;
    }
    final java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    if (info == null) {
      return;
    }
    final Point screenPt = info.getLocation();
    final Point chipPt = new Point(screenPt);
    SwingUtilities.convertPointFromScreen(chipPt, this);
    if (!containsPoint(chipPt)) {
      hovered = false;
      pressed = false;
      refreshLeadingAffordanceIcon();
      stopHoverPolling();
      repaint();
    }
  }

  @Override
  public void removeNotify() {
    stopHoverPolling();
    super.removeNotify();
  }

  private boolean isCursorStillInsideChip(final MouseEvent event) {
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
    final Point chipPt = new Point(screenPt);
    javax.swing.SwingUtilities.convertPointFromScreen(chipPt, this);
    return containsPoint(chipPt);
  }

  // ---------------------------------------------------------------- border

  private void rebuildBorder() {
    // JPanel's constructor calls updateUI() (which calls this) before our instance-field
    // initializers run, so the padding fields can be null on the very first invocation. Fall
    // back to the same defaults the field declarations use until the explicit values land.
    final SpaceScale v = paddingVertical != null ? paddingVertical : SpaceScale.XS;
    final SpaceScale h = paddingHorizontal != null ? paddingHorizontal : SpaceScale.MD;
    final Insets p = SpaceScale.insets(v, h);
    setBorder(BorderFactory.createEmptyBorder(p.top, p.left, p.bottom, p.right));
  }

  // --------------------------------------------------------------- painting

  @Override
  protected void paintComponent(final Graphics g) {
    final int w = getWidth();
    final int h = getHeight();
    final int arc = shape.px();
    final boolean interactive = interactionMode != ChipInteractionMode.STATIC && isEnabled();
    final boolean focused = isFocusOwner() && interactive;

    final ColorRole surfaceRole = getSurfaceRole();
    final StateLayer overlay = activeOverlay(interactive);
    final ColorRole borderRole = effectiveBorderRole(focused);
    final float borderStroke = focused ? Math.max(borderWidth, FOCUSED_BORDER_WIDTH) : borderWidth;

    if (!isEnabled()) {
      // M3 disabled is a compositing pass on top of the resolved surface, not a tinted overlay.
      final Graphics2D dim = (Graphics2D) g.create();
      try {
        dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
        SurfacePainter.paint(dim, w, h, arc, surfaceRole, null, borderRole, borderStroke);
      } finally {
        dim.dispose();
      }
      return;
    }

    SurfacePainter.paint((Graphics2D) g, w, h, arc, surfaceRole, overlay, borderRole, borderStroke);
  }

  private StateLayer activeOverlay(final boolean interactive) {
    if (pressed && interactive) {
      return StateLayer.PRESSED;
    }
    if (hovered && interactive) {
      return StateLayer.HOVER;
    }
    if (selected && variant != ChipVariant.GHOST) {
      return StateLayer.SELECTED;
    }
    return null;
  }

  /**
   * Returns the border role for the current state. Selected and focused both swap to {@link
   * ColorRole#PRIMARY} so the chip reads as "the picked one" without relying on the fill alone —
   * particularly relevant for OUTLINED chips under the uniform 12% selected overlay (rebuild doc §9
   * Q2). GHOST suppresses the border at rest but reveals it on hover / press / focus, and ignores
   * the selected state entirely (rebuild doc §9 Q2 amendment, issue #50): GHOST is M3's text-button
   * equivalent and the spec doesn't render a selected state on that emphasis level.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private ColorRole effectiveBorderRole(final boolean focused) {
    if (focused) {
      return ColorRole.PRIMARY;
    }
    if (selected && variant != ChipVariant.GHOST) {
      return ColorRole.PRIMARY;
    }
    final boolean idleGhost = variant == ChipVariant.GHOST && !hovered && !pressed;
    if (idleGhost) {
      return null;
    }
    return variant.borderRole();
  }

  // ----------------------------------------------------------- LAF / a11y

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    rebuildBorder();
    repaint();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(
        enabled && interactionMode != ChipInteractionMode.STATIC
            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
    trailingButton.setEnabled(enabled);
    repaint();
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(getPreferredSize().width, getPreferredSize().height);
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaChip();
    }
    return accessibleContext;
  }

  /**
   * Accessible role: PUSH_BUTTON for clickable / static / hoverable; TOGGLE_BUTTON for selectable.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  protected class AccessibleElwhaChip extends AccessibleJPanel {
    @Override
    public AccessibleRole getAccessibleRole() {
      return interactionMode == ChipInteractionMode.SELECTABLE
          ? AccessibleRole.TOGGLE_BUTTON
          : AccessibleRole.PUSH_BUTTON;
    }

    @Override
    public String getAccessibleName() {
      String n = super.getAccessibleName();
      if (n != null && !n.isEmpty()) {
        return n;
      }
      return textLabel.getText();
    }
  }

  // ----------------------------------------------------------- text label

  /**
   * JLabel subclass whose {@code getForeground()} is dynamic: it returns whatever {@link
   * ElwhaChip#resolveForegroundColor()} resolves to at paint time. This sidesteps the usual chain
   * of "manually call setForeground on every state transition that might affect color" — instead
   * the label always paints in the chip's current resolved foreground.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class ChipTextLabel extends JLabel {

    ChipTextLabel(final String text) {
      super(text);
    }

    @Override
    public Color getForeground() {
      return resolveForegroundColor();
    }
  }

  // --------------------------------------------------------- remove glyph

  /**
   * Minimal Material-shaped × glyph used by {@link #inputChip(String, Runnable)} so the input-chip
   * preset doesn't require a MaterialIcons dependency at the public-API level. Renders crisp at the
   * chip's foreground color via the surrounding chip's icon filter — at 14px optical size to match
   * the in-chip glyph convention.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final class RemoveGlyphIcon implements Icon {

    private static final int SIZE = 14;

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.getForeground());
        g2.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, 0));
        final int inset = 3;
        g2.drawLine(x + inset, y + inset, x + SIZE - inset, y + SIZE - inset);
        g2.drawLine(x + SIZE - inset, y + inset, x + inset, y + SIZE - inset);
      } finally {
        g2.dispose();
      }
    }

    @Override
    public int getIconWidth() {
      return SIZE;
    }

    @Override
    public int getIconHeight() {
      return SIZE;
    }
  }

  // ---------------------------------------------------------- leading layout

  /**
   * {@link FlowLayout} variant for the chip's leading cluster. Differs from stock {@code
   * FlowLayout} in two ways: floors the row height at a configurable minimum, and vertically
   * centers the row inside the container when the container is taller than the row.
   *
   * <p>Both behaviors exist to keep the text baseline stable across affordance states. Stock {@code
   * FlowLayout} skips invisible children when computing row height, so a chip with no visible
   * leading affordance reports a shorter preferred height than one with an anchor / pin glyph
   * showing — and when the chip's {@code BorderLayout} content row stretches the cluster vertically
   * to match the trailing button's hit-target height, the resulting slack lands below the row
   * instead of being split above and below. Net effect: the text label drifts ~2px up relative to
   * the trailing icon's centerline. Flooring the row height and re-centering after the parent's
   * layout pass fixes both halves.
   *
   * @author Charles Bryan
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final class BaselineCenteringFlowLayout extends FlowLayout {

    private static final long serialVersionUID = 1L;

    private final int minRowHeight;

    BaselineCenteringFlowLayout(final int hgap, final int minRowHeight) {
      super(FlowLayout.LEADING, hgap, 0);
      this.minRowHeight = minRowHeight;
    }

    @Override
    public Dimension preferredLayoutSize(final Container target) {
      final Dimension d = super.preferredLayoutSize(target);
      d.height = Math.max(d.height, minRowHeight);
      return d;
    }

    @Override
    public Dimension minimumLayoutSize(final Container target) {
      final Dimension d = super.minimumLayoutSize(target);
      d.height = Math.max(d.height, minRowHeight);
      return d;
    }

    @Override
    public void layoutContainer(final Container target) {
      super.layoutContainer(target);
      int rowHeight = 0;
      int rowTop = Integer.MAX_VALUE;
      final int count = target.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Component c = target.getComponent(i);
        if (!c.isVisible()) {
          continue;
        }
        rowHeight = Math.max(rowHeight, c.getHeight());
        rowTop = Math.min(rowTop, c.getY());
      }
      if (rowHeight == 0) {
        return;
      }
      final Insets in = target.getInsets();
      final int slack = target.getHeight() - in.top - in.bottom - rowHeight;
      if (slack <= 0) {
        return;
      }
      final int desiredTop = in.top + slack / 2;
      final int dy = desiredTop - rowTop;
      if (dy == 0) {
        return;
      }
      for (int i = 0; i < count; i++) {
        final Component c = target.getComponent(i);
        if (c.isVisible()) {
          c.setLocation(c.getX(), c.getY() + dy);
        }
      }
    }
  }

  // ----------------------------------------------------------- leading btn

  /**
   * Leading-slot clickable affordance — used by host containers to render pin/anchor toggle
   * buttons. Independent of the trailing button: it has its own hover/press tint, its own hit-area
   * floor of {@code BUTTON_MIN_HIT_TARGET} pixels, and it consumes its own mouse events.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final class LeadingButton extends JLabel {

    private Runnable onClick;
    private boolean hovered;
    private boolean pressed;

    LeadingButton() {
      super();
      setOpaque(false);
      setHorizontalAlignment(SwingConstants.CENTER);
      setVerticalAlignment(SwingConstants.CENTER);
      setBorder(BorderFactory.createEmptyBorder());
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
              hovered = true;
              repaint();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
              hovered = false;
              pressed = false;
              repaint();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
              if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
                return;
              }
              pressed = true;
              repaint();
              e.consume();
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
              if (!pressed || !isEnabled()) {
                pressed = false;
                repaint();
                return;
              }
              pressed = false;
              repaint();
              if (onClick != null && containsLocal(e.getPoint())) {
                onClick.run();
              }
              e.consume();
            }

            private boolean containsLocal(final Point p) {
              return p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight();
            }
          });
    }

    void setOnClick(final Runnable runnable) {
      onClick = runnable;
    }

    void setRenderedIcon(final Icon icon) {
      setIcon(icon);
      repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
      if ((hovered || pressed) && isEnabled() && getIcon() != null) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final StateLayer overlay = pressed ? StateLayer.PRESSED : StateLayer.HOVER;
          final Color tint =
              overlay.over(ColorRole.SURFACE.resolve(), ColorRole.ON_SURFACE.resolve());
          g2.setColor(tint);
          final int arc = Math.min(getWidth(), getHeight());
          g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
          g2.dispose();
        }
      }
      super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
      if (!isVisible()) {
        return new Dimension(0, 0);
      }
      final Dimension d = super.getPreferredSize();
      final int side = Math.max(BUTTON_MIN_HIT_TARGET, Math.max(d.width, d.height));
      return new Dimension(side, side);
    }
  }

  // ---------------------------------------------------------- trailing btn

  /**
   * A minimal {@link JLabel}-based button used for the trailing slot. Renders the {@link Action}'s
   * {@code SMALL_ICON} (or its {@code NAME} as fallback text) and forwards click to the action
   * without bubbling to the host chip.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final class TrailingIconButton extends JLabel {

    private Action action;
    private boolean hovered;
    private boolean pressed;

    TrailingIconButton() {
      super();
      setOpaque(false);
      setHorizontalAlignment(SwingConstants.CENTER);
      setVerticalAlignment(SwingConstants.CENTER);
      setBorder(BorderFactory.createEmptyBorder());
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
              hovered = true;
              repaint();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
              hovered = false;
              pressed = false;
              repaint();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
              if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
                return;
              }
              pressed = true;
              repaint();
              e.consume();
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
              if (!pressed || !isEnabled()) {
                pressed = false;
                repaint();
                return;
              }
              pressed = false;
              repaint();
              if (action != null && action.isEnabled() && containsLocal(e.getPoint())) {
                action.actionPerformed(
                    new ActionEvent(
                        TrailingIconButton.this,
                        ActionEvent.ACTION_PERFORMED,
                        "trailing",
                        e.getModifiersEx()));
              }
              e.consume();
            }

            private boolean containsLocal(final Point p) {
              return p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight();
            }
          });
    }

    void setAction(final Action action) {
      this.action = action;
      if (action == null) {
        setIcon(null);
        setText("");
        setToolTipText(null);
        return;
      }
      final Object icon = action.getValue(Action.SMALL_ICON);
      if (icon instanceof Icon i) {
        setIcon(i);
        setText("");
      } else {
        setIcon(null);
        final Object name = action.getValue(Action.NAME);
        setText(name == null ? "" : name.toString());
      }
      final Object desc = action.getValue(Action.SHORT_DESCRIPTION);
      setToolTipText(desc == null ? null : desc.toString());
    }

    @Override
    protected void paintComponent(final Graphics g) {
      if ((hovered || pressed) && isEnabled()) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final StateLayer overlay = pressed ? StateLayer.PRESSED : StateLayer.HOVER;
          final Color tint =
              overlay.over(ColorRole.SURFACE.resolve(), ColorRole.ON_SURFACE.resolve());
          g2.setColor(tint);
          final int arc = Math.min(getWidth(), getHeight());
          g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
          g2.dispose();
        }
      }
      super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
      if (!isVisible()) {
        return new Dimension(0, 0);
      }
      final Dimension d = super.getPreferredSize();
      final int side = Math.max(BUTTON_MIN_HIT_TARGET, Math.max(d.width, d.height));
      return new Dimension(side, side);
    }
  }
}
