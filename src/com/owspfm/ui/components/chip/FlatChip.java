package com.owspfm.ui.components.chip;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.awt.geom.RoundRectangle2D;
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
import javax.swing.UIManager;

/**
 * A compact FlatLaf-aware chip primitive: a single-row leading-icon + text + optional trailing
 * icon-button surface, themable via FlatLaf {@link UIManager} keys.
 *
 * <p><strong>Three-layer styling</strong>: every visual property (background, border color, corner
 * arc, padding, etc.) is resolved through three layers, last-wins:
 *
 * <ol>
 *   <li>Variant defaults — selected by {@link #setVariant(ChipVariant)}. Each variant pre-fills a
 *       coherent set of property values from FlatLaf {@link UIManager} keys.
 *   <li>{@link UIManager} overrides — {@code FlatChip.background}, {@code FlatChip.borderColor},
 *       {@code FlatChip.arc}, {@code FlatChip.padding}, {@code FlatChip.hoverBackground}, {@code
 *       FlatChip.pressedBackground}, {@code FlatChip.selectedBackground}, {@code
 *       FlatChip.selectedBorderColor}, {@code FlatChip.focusColor}, {@code
 *       FlatChip.disabledBackground}. These let a FlatLaf properties file theme every chip in the
 *       app without per-instance intervention.
 *   <li>Per-instance overrides — call setters (e.g. {@link #setCornerRadius(Integer)}, {@link
 *       #setSurfaceColor(Color)}) or put a {@code "FlatChip.style"} client property with a
 *       key=value string (FlatLaf-style) for ad-hoc tweaks.
 * </ol>
 *
 * <p><strong>Quick start</strong>:
 *
 * <pre>{@code
 * FlatChip chip = new FlatChip("Demand")
 *     .setVariant(ChipVariant.FILLED)
 *     .setLeadingIcon(icon)
 *     .setInteractionMode(ChipInteractionMode.SELECTABLE);
 * chip.addActionListener(evt -> System.out.println("toggled: " + chip.isSelected()));
 * }</pre>
 *
 * <p>This class has no dependencies on application code; the {@code com.owspfm.ui.components.chip}
 * directory can be lifted into its own library.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class FlatChip extends JPanel {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  /** Client-property key for the per-instance style escape hatch (FlatLaf-style key=value). */
  public static final String STYLE_PROPERTY = "FlatChip.style";

  // ----- UIManager key names (publicly documented contract) ----
  /** UIManager key for the default surface fill. */
  public static final String K_BACKGROUND = "FlatChip.background";

  /** UIManager key for the default border stroke color. */
  public static final String K_BORDER_COLOR = "FlatChip.borderColor";

  /** UIManager key for the corner arc (Integer, pixels). */
  public static final String K_ARC = "FlatChip.arc";

  /**
   * UIManager key for the padding ({@link Insets}). Unlike color and arc keys (which are read at
   * paint time and pick up runtime changes automatically), padding is baked into each chip's Swing
   * {@link javax.swing.border.Border} at construction. To propagate a live {@code
   * UIManager.put(K_PADDING, ...)} call to existing chips, run {@link
   * javax.swing.SwingUtilities#updateComponentTreeUI(java.awt.Component)} on the host container —
   * this is the standard Swing pattern for runtime UIManager tweaks and matches what FlatLaf's own
   * theme-switching does.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final String K_PADDING = "FlatChip.padding";

  /** UIManager key for the hover-state background tint. */
  public static final String K_HOVER_BACKGROUND = "FlatChip.hoverBackground";

  /** UIManager key for the pressed-state background tint. */
  public static final String K_PRESSED_BACKGROUND = "FlatChip.pressedBackground";

  /** UIManager key for the selected-state background tint. */
  public static final String K_SELECTED_BACKGROUND = "FlatChip.selectedBackground";

  /** UIManager key for the selected-state border color. */
  public static final String K_SELECTED_BORDER_COLOR = "FlatChip.selectedBorderColor";

  /** UIManager key for the focus-ring color. */
  public static final String K_FOCUS_COLOR = "FlatChip.focusColor";

  /** UIManager key for the disabled-state background. */
  public static final String K_DISABLED_BACKGROUND = "FlatChip.disabledBackground";

  /** UIManager key for the warm-accent fill (used by {@link ChipVariant#WARM_ACCENT}). */
  public static final String K_WARM_ACCENT = "FlatChip.warmAccent";

  /**
   * UIManager key for the text + icon foreground. When unset (the default), the chip auto-computes
   * a softened light or dark foreground from the surface luminance — guaranteeing readability even
   * when callers override the background to an unusual color. Set this (or use the per-instance
   * {@link #setForegroundColor(Color)} override) only when you want to pin a specific color
   * (branding, accessibility, etc.).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final String K_FOREGROUND = "FlatChip.foreground";

  private static final int DEFAULT_ARC = 999; // capsule by default
  private static final int DEFAULT_PADX = 10;
  private static final int DEFAULT_PADY = 4;
  private static final int DEFAULT_INNER_GAP = 6;
  private static final int DEFAULT_BORDER_WIDTH = 1;

  /**
   * Minimum hit-target side for the leading and trailing inline buttons. Keeps a 14px glyph
   * clickable at a reasonable 22×22 area without inflating the visible glyph itself.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static final int BUTTON_MIN_HIT_TARGET = 22;

  // Configuration ----------------------------------------------------------
  private ChipVariant variant = ChipVariant.FILLED;
  private ChipInteractionMode interactionMode = ChipInteractionMode.STATIC;
  private Integer cornerRadius;
  private Insets padding;
  private Color borderColor;
  private Color surfaceColorOverride;
  private Color foregroundOverride;
  private int borderWidth = DEFAULT_BORDER_WIDTH;

  // Softened black/white poles for auto-contrast foreground. Pure #000 / #FFF tends to read
  // harsh on saturated backgrounds; these slightly desaturated tones look more polished while
  // still meeting WCAG-style contrast against any reasonable surface.
  private static final Color AUTO_FG_DARK = new Color(30, 30, 30);
  private static final Color AUTO_FG_LIGHT = new Color(240, 240, 240);

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
  // refresh. Bound to icons as they're set via setLeadingIcon / setTrailingAction /
  // setLeadingAffordance — overriding whatever filter the icon arrived with (typically
  // MaterialIcons' Label.foreground filter).
  private final FlatSVGIcon.ColorFilter iconFilter =
      new FlatSVGIcon.ColorFilter(c -> resolveForegroundColor());

  // Listeners --------------------------------------------------------------
  private final List<ActionListener> actionListeners = new ArrayList<>();
  private Consumer<MouseEvent> contextMenuCallback;

  // ----------------------------------------------------------------- ctor

  /** Creates a chip with empty text and default FILLED variant. */
  public FlatChip() {
    this("");
  }

  /**
   * Creates a chip with the given text.
   *
   * @param text the chip label
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip(final String text) {
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

    // Leading cluster packs leading-button + leading-icon-label + text tight-left via FlowLayout.
    // The cluster itself is placed in BorderLayout.WEST so it stays anchored to the chip's leading
    // edge — and crucially, the trailing button goes to BorderLayout.EAST so it floats to the
    // chip's trailing edge when the chip is stretched (grid cells, horizontal-stretch parents).
    // At preferred size the layout matches the historical FlowLayout result because FlowLayout's
    // own horizontal insets supply the same gap between cluster and trailing button.
    leadingCluster = new JPanel(new FlowLayout(FlowLayout.LEADING, DEFAULT_INNER_GAP, 0));
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
  public FlatChip setText(final String text) {
    textLabel.setText(text == null ? "" : text);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the live text label for font / color / FlatLaf style-class customization.
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
  public FlatChip setVariant(final ChipVariant variant) {
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
  public FlatChip setInteractionMode(final ChipInteractionMode mode) {
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
  public FlatChip setLeadingIcon(final Icon icon) {
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
   * the chip-resolved color (auto-contrast, per-instance override, or UIManager) rather than
   * whatever filter the caller supplied — typically {@link
   * com.owspfm.ui.components.icons.MaterialIcons}'s app-wide {@code Label.foreground} filter, which
   * is the right default outside a chip but wrong inside one with a custom surface.
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
   * Installs (or clears) a clickable leading-slot affordance with two visual states. Used by host
   * containers like {@code FlatChipList} to render pin / anchor / etc. buttons that the user can
   * click directly.
   *
   * <ul>
   *   <li>{@code idleIcon} renders when {@code active == false}. If {@code hoverRevealIdle ==
   *       true}, the idle icon is only painted while the chip body is hovered — the slot still
   *       reserves its hit-area so the chip width doesn't jump.
   *   <li>{@code activeIcon} (or {@code idleIcon} if active is null) renders persistently when
   *       {@code active == true} — for the pinned / anchored state.
   *   <li>Pass {@code null} for both icons to clear the affordance entirely. The leading-icon slot
   *       reverts to its {@link #setLeadingIcon(Icon)} value.
   * </ul>
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
  public FlatChip setLeadingAffordance(
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
      // Restore the user-supplied leading icon (if any).
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

  /**
   * Refreshes the leading-button's visible icon based on the active state and hover-reveal flag.
   * Called whenever chip hover state changes, or when the affordance configuration changes.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
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
  public FlatChip setTrailingAction(final Action action) {
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
   * Installs a callback invoked when the user requests a context menu (right-click, or platform
   * menu key) on the chip body. The callback receives the originating event so it can position a
   * {@link JPopupMenu} relative to the click point. Pass null to clear.
   *
   * @param callback the callback, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setContextMenuCallback(final Consumer<MouseEvent> callback) {
    contextMenuCallback = callback;
    return this;
  }

  /**
   * Convenience: attaches a {@link JPopupMenu} that pops up at the click point on right-click /
   * VK_CONTEXT_MENU / Shift+F10. Equivalent to {@link #setContextMenuCallback(Consumer)} with a
   * default-positioning callback. Pass null to clear.
   *
   * @param popup the popup to attach, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip attachContextMenu(final JPopupMenu popup) {
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
   * Convenience: attaches a {@code Supplier<JPopupMenu>} so callers can build the menu lazily
   * (e.g., based on selection state at click-time).
   *
   * @param supplier the supplier; null clears
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip attachContextMenu(final java.util.function.Supplier<JPopupMenu> supplier) {
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
   * button bound to the supplied icon, tooltip, and runnable. Equivalent to building an inline
   * {@link Action}.
   *
   * @param icon the icon shown in the trailing slot (null hides the trailing button)
   * @param tooltip optional tooltip text (null suppresses)
   * @param onClick callback invoked when the button is clicked
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setTrailingIcon(final Icon icon, final String tooltip, final Runnable onClick) {
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
  public FlatChip setSelected(final boolean selected) {
    if (selected == this.selected) {
      return this;
    }
    final boolean old = selected;
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

  // ----------------------------------------------------------- style API

  /**
   * Overrides the corner radius. Pass null to fall back to {@code FlatChip.arc} (or capsule
   * default).
   *
   * @param radius the radius, or null for theme default
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setCornerRadius(final Integer radius) {
    cornerRadius = radius;
    repaint();
    return this;
  }

  /**
   * Returns the effective corner radius — overriding setter if non-null, otherwise the {@code
   * FlatChip.arc} UIManager value, otherwise the capsule default ({@value #DEFAULT_ARC}).
   *
   * @return effective arc in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getEffectiveCornerRadius() {
    if (cornerRadius != null) {
      return cornerRadius;
    }
    final Object v = UIManager.get(K_ARC);
    if (v instanceof Number n) {
      return Math.max(0, n.intValue());
    }
    return DEFAULT_ARC;
  }

  /**
   * Replaces the content padding (the inset between the rounded surface and the leading
   * icon/text/trailing button row).
   *
   * @param insets the padding; null restores theme default
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setPadding(final Insets insets) {
    padding = insets == null ? null : (Insets) insets.clone();
    rebuildBorder();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Convenience overload that applies a uniform horizontal and vertical padding.
   *
   * @param horizontal left/right padding
   * @param vertical top/bottom padding
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setPadding(final int horizontal, final int vertical) {
    return setPadding(new Insets(vertical, horizontal, vertical, horizontal));
  }

  /**
   * Returns the active padding.
   *
   * @return the active padding (defensive copy)
   * @version v0.1.0
   * @since v0.1.0
   */
  public Insets getPadding() {
    return (Insets) effectivePadding().clone();
  }

  /**
   * Overrides the border color. Pass null to fall back to {@code FlatChip.borderColor} or theme.
   *
   * @param color the border color, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setBorderColor(final Color color) {
    borderColor = color;
    repaint();
    return this;
  }

  /**
   * Sets the border thickness in pixels.
   *
   * @param width the width, clamped to {@code >= 0}
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setBorderWidth(final int width) {
    borderWidth = Math.max(0, width);
    repaint();
    return this;
  }

  /**
   * Overrides the variant-derived surface color. Pass null to restore variant default.
   *
   * @param color the surface color, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setSurfaceColor(final Color color) {
    surfaceColorOverride = color;
    repaint();
    return this;
  }

  /**
   * Per-instance foreground override. Wins over the {@link #K_FOREGROUND} UIManager key and over
   * the auto-contrast default. Pass null to restore the resolution chain.
   *
   * @param color the foreground color, or null
   * @return this chip
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatChip setForegroundColor(final Color color) {
    foregroundOverride = color;
    repaint();
    return this;
  }

  /**
   * Resolves the effective text + icon foreground color via a three-layer chain.
   *
   * <ol>
   *   <li>Per-instance override from {@link #setForegroundColor(Color)} (non-null wins).
   *   <li>UIManager key {@link #K_FOREGROUND} (non-null wins).
   *   <li>Auto-contrast — picks the softened light or dark pole based on the perceived luminance of
   *       the effective surface color (BT.601 luma). When the surface is null or transparent (GHOST
   *       variant) the chain falls through to {@code Label.foreground}.
   * </ol>
   *
   * @return the resolved foreground color (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  protected Color resolveForegroundColor() {
    if (foregroundOverride != null) {
      return foregroundOverride;
    }
    final Color managerOverride = UIManager.getColor(K_FOREGROUND);
    if (managerOverride != null) {
      return managerOverride;
    }
    final Color surface = effectiveSurfaceForContrast();
    if (surface == null) {
      final Color labelFg = UIManager.getColor("Label.foreground");
      return labelFg != null ? labelFg : Color.BLACK;
    }
    return isLight(surface) ? AUTO_FG_DARK : AUTO_FG_LIGHT;
  }

  /**
   * The surface color used to drive auto-contrast. Computed from the variant + per-instance
   * override + selection blend — ignoring hover/press so a passing cursor doesn't flicker the
   * foreground color. GHOST returns null (transparent surface; contrast caller falls through to
   * theme foreground).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private Color effectiveSurfaceForContrast() {
    final Color base = resolveSurfaceColor();
    if (base == null) {
      return null;
    }
    if (selected) {
      // Match what paintBackground actually renders for the selected state so auto-contrast
      // picks foreground against the rendered color, not against an intermediate value.
      return composeSelectedBackground(base);
    }
    return base;
  }

  /**
   * Perceived-brightness threshold. {@code true} means the color is light enough that dark
   * foreground reads better; {@code false} means a light foreground is needed. Uses ITU-R BT.601
   * luma — cheap, well-established, gives good results across saturated and muted colors alike.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static boolean isLight(final Color color) {
    final double luma = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
    return luma >= 128;
  }

  // ------------------------------------------------------------ listeners

  /**
   * Installs a mouse listener on both the chip body and the inner content row, so callers (such as
   * a hosting {@code FlatChipList} wiring drag-to-reorder) receive events regardless of which inner
   * subcomponent the user actually pressed on. Without this, presses on the text label or
   * leading-icon area would be trapped by the content row's internal listener and never reach the
   * caller.
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
    // The leading cluster wraps leading-button + leading-icon + text. Presses on the text label
    // would otherwise route through the cluster (no listener) and miss the content row's listener,
    // depending on Swing's lightweight-dispatch behavior. Installing here is belt-and-braces.
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
   * Convenience: scopes a {@link PropertyChangeListener} to {@link #PROPERTY_SELECTED}. Equivalent
   * to {@link #addPropertyChangeListener(String, PropertyChangeListener)}.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addSelectionChangeListener(final PropertyChangeListener listener) {
    addPropertyChangeListener(PROPERTY_SELECTED, listener);
  }

  /**
   * Reports whether a context-menu callback (or attached popup) is installed. Useful for hosting
   * containers that want to layer additional context-menu items without clobbering a caller's
   * existing menu.
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
  public FlatChip cancelPendingClick() {
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
            // Fast-path hover-clear when mouseExited fires reliably and the live cursor confirms
            // we're truly outside the chip. The hover-poll timer started in mouseEntered is the
            // backup for the cases this path misses (macOS dispatch quirks, boundary-precision
            // for slow exits).
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
            // Press/release from the inline button children own their own click semantics —
            // treating them as a chip press would create a phantom pressed-tint on the whole
            // chip on every pin/trash click.
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
    // Children (cluster + buttons) get the same hover-tracking listener so an exit that fires on
    // a child (cursor moves from a button straight off the chip) reaches our hover-clear logic
    // — without this, the button's mouseExited went to its own listener only and the parent
    // chip kept hovered=true. The isCursorStillInsideChip guard in mouseExited handles the
    // benign "moved to a sibling child" case for free.
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
                    FlatChip.this,
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
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "flatchip.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "flatchip.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "flatchip.contextMenu");
    // Shift+F10 is the standard Swing/Windows accelerator for "show context menu".
    im.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F10, java.awt.event.InputEvent.SHIFT_DOWN_MASK),
        "flatchip.contextMenu");
    am.put("flatchip.activate", activate);
    am.put("flatchip.contextMenu", contextMenu);
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

  /**
   * Returns true when the event source is one of the inline chip buttons (leading affordance or
   * trailing action). The chip-level press/release handlers skip these so a click on the pin or
   * trash glyph doesn't paint a phantom pressed-tint on the entire chip — the buttons own their own
   * click semantics.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private boolean isFromInlineButton(final MouseEvent event) {
    final Object src = event.getSource();
    return src == leadingButton || src == trailingButton;
  }

  /**
   * Starts (or no-ops if already running) the hover-poll timer that backs up Swing's unreliable
   * {@code mouseExited} dispatch on macOS. The timer queries {@link java.awt.MouseInfo} for the
   * live cursor position every {@link #HOVER_POLL_INTERVAL_MS}ms and clears hover state when it
   * confirms the cursor is outside the chip bounds.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
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
      // Chip is no longer realized — clear and stop. Defensive; removeNotify also stops the
      // timer when the chip leaves the hierarchy.
      hovered = false;
      pressed = false;
      refreshLeadingAffordanceIcon();
      stopHoverPolling();
      return;
    }
    final java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    if (info == null) {
      // Headless or sandboxed environment — can't poll. Leave hover state untouched; the
      // mouseExited fast-path is the only signal we have.
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
    // Stop the hover-poll timer when the chip leaves its parent hierarchy — otherwise the
    // timer's strong reference to the lambda (which captures `this`) keeps the chip alive past
    // its useful life.
    stopHoverPolling();
    super.removeNotify();
  }

  /**
   * Returns true when the live cursor is currently inside this chip's bounds. Fast-path guard for
   * {@code mouseExited} so a "cursor moved into a child component" event doesn't get treated as a
   * real chip exit. The hover-poll timer is the authoritative backup for slow exits and platforms
   * where {@code mouseExited} fires unreliably.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private boolean isCursorStillInsideChip(final MouseEvent event) {
    if (!isShowing()) {
      return false;
    }
    final java.awt.PointerInfo info = java.awt.MouseInfo.getPointerInfo();
    final Point screenPt;
    if (info != null) {
      screenPt = info.getLocation();
    } else {
      // Sandboxed environments (some security managers, headless screens) may return null —
      // fall back to event-time coords. Imperfect but better than treating as "outside."
      screenPt = new Point(event.getXOnScreen(), event.getYOnScreen());
    }
    final Point chipPt = new Point(screenPt);
    javax.swing.SwingUtilities.convertPointFromScreen(chipPt, this);
    return containsPoint(chipPt);
  }

  // ---------------------------------------------------------------- border

  private void rebuildBorder() {
    final Insets p = effectivePadding();
    setBorder(BorderFactory.createEmptyBorder(p.top, p.left, p.bottom, p.right));
  }

  private Insets effectivePadding() {
    if (padding != null) {
      return padding;
    }
    final Object v = UIManager.get(K_PADDING);
    if (v instanceof Insets in) {
      return in;
    }
    return new Insets(DEFAULT_PADY, DEFAULT_PADX, DEFAULT_PADY, DEFAULT_PADX);
  }

  // --------------------------------------------------------------- painting

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      final int w = getWidth();
      final int h = getHeight();
      final int arc = clampArc(getEffectiveCornerRadius(), w, h);
      paintBackground(g2, w, h, arc);
      paintBorder(g2, w, h, arc);
    } finally {
      g2.dispose();
    }
  }

  private int clampArc(final int arc, final int w, final int h) {
    final int max = Math.min(w, h);
    return Math.min(arc, max);
  }

  /**
   * Builds the canonical chip outline. Centered on the half-pixel grid so a 1px AA stroke renders
   * crisp without straddling integer columns; both fill and stroke use this same shape so the
   * surface edge and border edge stay co-located across all variants and states.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private RoundRectangle2D.Float chipShape(final int w, final int h, final int arc) {
    return new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc);
  }

  private void paintBackground(final Graphics2D g2, final int w, final int h, final int arc) {
    Color bg = resolveSurfaceColor();
    final boolean interactive = interactionMode != ChipInteractionMode.STATIC && isEnabled();
    if (selected) {
      bg = composeSelectedBackground(bg);
    }
    if (pressed && interactive) {
      bg = composePressedBackground(bg);
    } else if (hovered && interactive) {
      bg = composeHoverBackground(bg);
    }
    if (!isEnabled()) {
      final Color disabled = resolveDisabledBackground();
      if (disabled != null) {
        bg = disabled;
      }
    }
    if (bg == null) {
      return;
    }
    g2.setColor(bg);
    g2.fill(chipShape(w, h, arc));
  }

  /**
   * Paints the chip's single state-driven outline. Focus is folded in here — no separate ring at a
   * different radius. State precedence: focused → accent at thicker stroke; selected → accent at
   * baseline width; otherwise variant border. GHOST suppresses the border in its idle state for the
   * "no chrome" look but reveals it once hovered, pressed, selected, or focused.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void paintBorder(final Graphics2D g2, final int w, final int h, final int arc) {
    if (borderWidth <= 0) {
      return;
    }
    final boolean interactive = interactionMode != ChipInteractionMode.STATIC && isEnabled();
    final boolean focused = isFocusOwner() && interactive;
    final boolean idleGhost =
        variant == ChipVariant.GHOST && !selected && !hovered && !pressed && !focused;
    if (idleGhost) {
      return;
    }
    Color border;
    float strokeWidth = borderWidth;
    if (focused) {
      border = resolveFocusColor();
      strokeWidth = Math.max(strokeWidth, 1.6f);
    } else if (selected) {
      border = resolveSelectedBorderColor();
    } else {
      border = resolveBorderColor();
    }
    if (border == null) {
      return;
    }
    g2.setColor(border);
    g2.setStroke(new BasicStroke(strokeWidth));
    g2.draw(chipShape(w, h, arc));
  }

  /**
   * Per-variant strength of the selected-state fill blend. FILLED/GHOST go all-in for a bold accent
   * surface; OUTLINED stays mostly transparent so the variant's "outline" character isn't
   * overwritten into a generic blue blob; WARM_ACCENT splits the difference.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private float selectedFillStrength() {
    return switch (variant) {
      case FILLED, GHOST -> 1.0f;
      case OUTLINED -> 0.22f;
      case WARM_ACCENT -> 0.6f;
    };
  }

  private Color resolveFocusColor() {
    Color ring = UIManager.getColor(K_FOCUS_COLOR);
    if (ring == null) {
      ring = UIManager.getColor("Component.focusColor");
    }
    if (ring == null) {
      ring = accentFallback();
    }
    return ring;
  }

  // ---------------------------------------------------------- color resolve

  private Color resolveSurfaceColor() {
    if (surfaceColorOverride != null) {
      return surfaceColorOverride;
    }
    // Direct-override model: when a UIManager key is explicitly set, it IS the rendered color —
    // no variant-blended character is added on top. This makes the LAF tweak picker WYSIWYG.
    // Variant blend kicks in only when the caller hasn't expressed an opinion.
    final Color managerOverride = UIManager.getColor(K_BACKGROUND);
    if (managerOverride != null) {
      return managerOverride;
    }
    final Color base = panelBackground();
    return switch (variant) {
      case FILLED -> blend(base, foregroundForBlend(), 0.10f);
      case OUTLINED -> base;
      case GHOST -> null; // transparent until hovered/pressed/selected
      case WARM_ACCENT -> {
        final Color warm = UIManager.getColor(K_WARM_ACCENT);
        yield warm != null ? warm : new Color(248, 226, 165);
      }
    };
  }

  private Color resolveBorderColor() {
    if (borderColor != null) {
      return borderColor;
    }
    final Color managerOverride = UIManager.getColor(K_BORDER_COLOR);
    if (managerOverride != null) {
      return managerOverride;
    }
    return switch (variant) {
      case FILLED, WARM_ACCENT -> blend(panelBackground(), foregroundForBlend(), 0.20f);
      case OUTLINED, GHOST -> blend(panelBackground(), foregroundForBlend(), 0.30f);
    };
  }

  /**
   * Composes the final hovered surface color. Direct-override model: if {@link #K_HOVER_BACKGROUND}
   * is explicitly set, that color is the result (no blend with {@code base}). If unset, the
   * theme-default hover tint is blended into {@code base} at 45% to produce a subtle "the surface
   * leaned toward hover" effect.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private Color composeHoverBackground(final Color base) {
    final Color explicit = UIManager.getColor(K_HOVER_BACKGROUND);
    if (explicit != null) {
      return explicit;
    }
    final Color tint = blend(panelBackground(), foregroundForBlend(), 0.18f);
    return blend(base, tint, 0.45f);
  }

  /** Pressed counterpart of {@link #composeHoverBackground(Color)}; explicit wins direct. */
  private Color composePressedBackground(final Color base) {
    final Color explicit = UIManager.getColor(K_PRESSED_BACKGROUND);
    if (explicit != null) {
      return explicit;
    }
    final Color tint = blend(panelBackground(), foregroundForBlend(), 0.28f);
    return blend(base, tint, 0.55f);
  }

  /**
   * Selected counterpart of {@link #composeHoverBackground(Color)}. When unset, the theme-default
   * accent tint is blended at {@link #selectedFillStrength()} so OUTLINED stays mostly transparent
   * and FILLED/GHOST go all-in.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private Color composeSelectedBackground(final Color base) {
    final Color explicit = UIManager.getColor(K_SELECTED_BACKGROUND);
    if (explicit != null) {
      return explicit;
    }
    final Color tint = blend(panelBackground(), accentFallback(), 0.45f);
    return blend(base, tint, selectedFillStrength());
  }

  private Color resolveSelectedBorderColor() {
    final Color v = UIManager.getColor(K_SELECTED_BORDER_COLOR);
    if (v != null) {
      return v;
    }
    return accentFallback();
  }

  private Color resolveDisabledBackground() {
    return UIManager.getColor(K_DISABLED_BACKGROUND);
  }

  private static Color panelBackground() {
    final Color c = UIManager.getColor("Panel.background");
    return c == null ? new Color(245, 245, 245) : c;
  }

  private static Color foregroundForBlend() {
    final Color c = UIManager.getColor("Label.foreground");
    return c == null ? Color.DARK_GRAY : c;
  }

  private static Color accentFallback() {
    Color c = UIManager.getColor("Component.accentColor");
    if (c == null) {
      c = UIManager.getColor("Component.focusColor");
    }
    return c == null ? new Color(72, 130, 180) : c;
  }

  private static Color blend(final Color a, final Color b, final float t) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    final float c = Math.max(0f, Math.min(1f, t));
    final int r = (int) (a.getRed() * (1 - c) + b.getRed() * c);
    final int g = (int) (a.getGreen() * (1 - c) + b.getGreen() * c);
    final int bb = (int) (a.getBlue() * (1 - c) + b.getBlue() * c);
    return new Color(r, g, bb);
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
      accessibleContext = new AccessibleFlatChip();
    }
    return accessibleContext;
  }

  /**
   * Accessible role: PUSH_BUTTON for clickable / static / hoverable; TOGGLE_BUTTON for selectable.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  protected class AccessibleFlatChip extends AccessibleJPanel {
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
   * FlatChip#resolveForegroundColor()} resolves to at paint time. This sidesteps the usual chain of
   * "manually call setForeground on every state transition that might affect color" — instead the
   * label always paints in the chip's current resolved foreground (auto-contrast against the
   * surface, per-instance override, or UIManager key) without any explicit refresh.
   *
   * <p>Inner (non-static) so it can read the outer chip's resolver. The outer reference is stable
   * for the label's lifetime — labels never migrate between chips.
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
      // FlatChip's resolveForegroundColor reads default-initialized state (variant, selection,
      // surface override) plus UIManager, so it's safe to invoke during JLabel's superclass
      // construction too — no NPE on partially-initialized FlatChip fields.
      return resolveForegroundColor();
    }
  }

  // ----------------------------------------------------------- leading btn

  /**
   * Leading-slot clickable affordance — used by host containers (like {@code FlatChipList}) to
   * render pin/anchor toggle buttons that the user can click directly on the chip. Independent of
   * the trailing button: it has its own hover/press tint, its own hit-area floor of {@code
   * MIN_HIT_TARGET} pixels (so a 14px glyph still gets a 22×22 click target), and it consumes its
   * own mouse events so the click is never interpreted as a chip drag or selection.
   *
   * <p>The icon is set via {@link #setRenderedIcon} rather than {@code setIcon} so the host can
   * swap glyphs (outline/filled, hover-revealed/hidden) without re-binding a fresh Action each time
   * and losing its tooltip.
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
              // Consume so the host chip's drag-start handler can detect that the press
              // originated on the button and skip its drag-threshold tracking entirely.
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

    /**
     * Swaps the rendered icon without going through {@code setAction}, preserving the tooltip and
     * click binding. A null icon leaves the slot empty but the component still reserves its hit
     * area via {@link #getPreferredSize()}.
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    void setRenderedIcon(final Icon icon) {
      setIcon(icon);
      repaint();
    }

    @Override
    protected void paintComponent(final Graphics g) {
      if ((hovered || pressed) && isEnabled() && getIcon() != null) {
        // Tint only when there's actually a glyph to highlight — a hover-revealed empty slot
        // shouldn't paint a phantom circle on mouseover.
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final int alpha = pressed ? 60 : 32;
          g2.setComposite(AlphaComposite.SrcOver.derive(1f));
          final Color base = UIManager.getColor("Label.foreground");
          final Color c =
              base != null
                  ? new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha)
                  : new Color(0, 0, 0, alpha);
          g2.setColor(c);
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
      // BorderLayout (used by FlatChip's content row) doesn't skip invisible components when
      // computing preferred sizes, so report zero when invisible to avoid reserving the leading
      // slot when no affordance is active.
      if (!isVisible()) {
        return new Dimension(0, 0);
      }
      final Dimension d = super.getPreferredSize();
      // Always reserve the hit-area floor even when no icon is rendered, so a hover-revealed
      // affordance doesn't make the chip width jump on hover-enter / hover-exit.
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
      // No border: the hover/press circle is painted on the full component bounds, so any
      // asymmetric padding here would offset the glyph relative to the circle.
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
      // Background hover/press tint for affordance, painted under the icon/text.
      if ((hovered || pressed) && isEnabled()) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          final int alpha = pressed ? 60 : 32;
          g2.setComposite(AlphaComposite.SrcOver.derive(1f));
          final Color base = UIManager.getColor("Label.foreground");
          final Color c =
              base != null
                  ? new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha)
                  : new Color(0, 0, 0, alpha);
          g2.setColor(c);
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
      // BorderLayout-anchored trailing slot — report zero when invisible so a chip without a
      // trailing action doesn't reserve 22px of empty space at its trailing edge.
      if (!isVisible()) {
        return new Dimension(0, 0);
      }
      final Dimension d = super.getPreferredSize();
      // Square + at-least {@code BUTTON_MIN_HIT_TARGET} so a small (14px) icon still has a
      // reasonable click area, matching the leading-button slot's footprint.
      final int side = Math.max(BUTTON_MIN_HIT_TARGET, Math.max(d.width, d.height));
      return new Dimension(side, side);
    }
  }
}
