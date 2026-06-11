package com.owspfm.elwha.switches;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The Elwha Material 3 <strong>switch</strong> — a token-themed binary selection control painting
 * M3 chrome: a {@value #TRACK_WIDTH_PX}&times;{@value #TRACK_HEIGHT_PX} corner-full track (with a
 * {@value #TRACK_OUTLINE_PX} dp {@link ColorRole#OUTLINE} border while unselected), and a circular
 * handle riding the track that morphs {@value #HANDLE_UNSELECTED_PX}&rarr;{@value
 * #HANDLE_SELECTED_PX} dp across the selection states.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-switch-design.md} §2).</strong> {@code ElwhaSwitch} is one dedicated {@link JComponent}
 * that paints every M3 part itself, holding the selection as a plain boolean. It is <em>not</em> a
 * styled {@code JToggleButton} and <em>not</em> a {@code ButtonUI} delegate: nothing of a button's
 * text/icon layout survives the track-and-riding-handle anatomy, the handle drag gesture has no
 * {@code ButtonModel} vocabulary, and switches are never {@code ButtonGroup}-grouped — so the model
 * would buy nothing. The package is {@code switches} because {@code switch} is a Java reserved
 * word.
 *
 * <p><strong>Color (zero new tokens — research §Tokens).</strong> Selected: track {@link
 * ColorRole#PRIMARY}, handle {@link ColorRole#ON_PRIMARY}. Unselected: track {@link
 * ColorRole#SURFACE_CONTAINER_HIGHEST} with the {@link ColorRole#OUTLINE} border, handle {@link
 * ColorRole#OUTLINE}. Disabled: track at the M3 container opacity (0.12) over {@link
 * ColorRole#ON_SURFACE} (selected) / {@link ColorRole#SURFACE_CONTAINER_HIGHEST} (unselected, with
 * an {@link ColorRole#ON_SURFACE} ring), handle {@link ColorRole#ON_SURFACE} at the content opacity
 * (0.38) unselected but <strong>opaque {@link ColorRole#SURFACE}</strong> selected — the spec's
 * deliberate asymmetry (research §T ⚠). All roles resolve at paint time so runtime theme +
 * light/dark switching re-skins the switch live.
 *
 * <p><strong>Geometry (M3 token-locked — research §T).</strong> The preferred size is the
 * state-layer-inclusive box — the track block plus the {@value #STATE_LAYER_SIZE_PX} dp interaction
 * halo's {@value #HALO_OVERHANG_PX} dp overhang per side; the chrome centers itself in larger
 * bounds. The M3 48&nbsp;dp touch target is guidance for touch contexts, not painted geometry — the
 * component's whole bounds are interactive.
 *
 * <p><strong>Interaction (research §B / §S).</strong> Click anywhere to toggle, drag the handle
 * past the track midpoint to commit a state, or toggle from the keyboard with Space (press shows
 * the pressed treatment, release commits — Swing button semantics; Enter is deliberately unbound).
 * Hover paints {@link StateLayer#HOVER} (0.08) and focus {@link StateLayer#FOCUS} (0.10) on the
 * {@value #STATE_LAYER_SIZE_PX} dp circle riding the handle; a press grows the handle to {@value
 * #HANDLE_PRESSED_PX} dp, paints {@link StateLayer#PRESSED}, and shows a {@link RipplePainter}
 * ripple bounded to the same circle. User-gesture commits fire the registered {@link
 * ActionListener}s <em>and</em> {@link ChangeListener}s; programmatic {@code setSelected} fires
 * only the latter. Under a right-to-left {@link java.awt.ComponentOrientation} the switch mirrors:
 * selected rests the handle at the <em>left</em> end, and the travel, drag scrubbing, and commit
 * halves all flip with it.
 *
 * <p><strong>Motion (design §6 / research §Mo).</strong> A toggle slides the handle over {@value
 * #SLIDE_MS}&nbsp;ms on material-web's overshoot bezier {@code (0.175, 0.885, 0.32, 1.275)} — the
 * handle deliberately swings a couple of px past its rest point and settles (it stays inside the
 * track). The handle diameter tweens through its own retargeting morph ({@value
 * #SIZE_MORPH_MS}&nbsp;ms standard easing for selection changes, {@value #PRESS_MORPH_MS}&nbsp;ms
 * linear for press grow/release), and the track/handle colors crossfade along the slide progress —
 * an accepted deviation from M3's 67&nbsp;ms color snap so a scrubbed drag previews color
 * continuously (design §6). A drag owns the handle position while in flight; release hands the
 * position back to the slide tween with no jump. Programmatic {@code setSelected} animates only
 * while the switch is displayable and enabled — otherwise it snaps — and {@link
 * MorphAnimator#isReducedMotion() reduced motion} snaps everything globally.
 *
 * <p><strong>Icons (research §A / §Mo).</strong> {@link #setIconsVisible(boolean)} shows a glyph on
 * the handle in <em>both</em> states (M3's {@code icons} configuration); {@link
 * #setShowOnlySelectedIcon(boolean)} shows one on the <em>selected</em> handle only — and wins when
 * both are set. The defaults are {@link MaterialIcons#check(int) check} / {@link
 * MaterialIcons#close(int) close} at {@value #ICON_SIZE_PX} px, replaceable per side via {@link
 * #setSelectedIcon(Icon)} / {@link #setUnselectedIcon(Icon)} ({@code null} restores the default). A
 * handle whose icon is enabled rides at {@value #HANDLE_WITH_ICON_PX} px; icons crossfade along the
 * slide, and in selected-icon-only mode the glyph rotates &minus;45&deg; into view (material-web's
 * flourish). Glyphs are recolored at paint time by compositing — no shared {@code FlatSVGIcon}
 * color-filter mutation — so any {@link Icon} works.
 *
 * <p><strong>Labelling &amp; accessibility (research §A / §X).</strong> A switch never labels
 * itself — M3 requires an adjacent label naming what it toggles, and the accessible name must
 * <em>always</em> be set: call {@link #setLabel(String)} or associate a {@link javax.swing.JLabel}
 * via {@link javax.swing.JLabel#setLabelFor}. Swing has no SWITCH role, so the accessible context
 * is the {@code JToggleButton} shape: {@link AccessibleRole#TOGGLE_BUTTON} with {@link
 * AccessibleState#CHECKED} while selected, one "click" {@link AccessibleAction} (a user-gesture
 * toggle), and an {@link AccessibleValue} of 0/1.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaSwitch extends JComponent {

  // --- M3 switch geometry (dp == px at 100% scale; research §T — the md-comp-switch values) ---

  /** Track width. */
  static final int TRACK_WIDTH_PX = 52;

  /** Track height — also the capsule end diameter. */
  static final int TRACK_HEIGHT_PX = 32;

  /** Unselected track outline width, painted inside the track bounds. */
  static final int TRACK_OUTLINE_PX = 2;

  /** Handle diameter while unselected (no icon). */
  static final int HANDLE_UNSELECTED_PX = 16;

  /** Handle diameter while selected. */
  static final int HANDLE_SELECTED_PX = 24;

  /** Handle diameter while pressed or mid-drag — on either side of the toggle. */
  static final int HANDLE_PRESSED_PX = 28;

  /** Handle diameter whenever its icon is shown ({@code with-icon-handle}). */
  static final int HANDLE_WITH_ICON_PX = 24;

  /** On-handle icon size. */
  static final int ICON_SIZE_PX = 16;

  /** Diameter of the hover/focus/press state layer riding the handle. */
  static final int STATE_LAYER_SIZE_PX = 40;

  /** The state layer's overhang past the track box, per side. */
  static final int HALO_OVERHANG_PX = (STATE_LAYER_SIZE_PX - TRACK_HEIGHT_PX) / 2;

  /** Pointer travel from the press point before a press becomes a drag (design §12). */
  static final int DRAG_THRESHOLD_PX = 4;

  /** Handle slide duration — {@code motion.duration.medium2} (research §Mo). */
  static final int SLIDE_MS = MorphAnimator.MEDIUM2_MS;

  /** Handle size-morph duration for selection-driven diameter changes (research §Mo). */
  static final int SIZE_MORPH_MS = 250;

  /** Handle size-morph duration for press grow / release shrink (research §Mo). */
  static final int PRESS_MORPH_MS = 100;

  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;

  // material-web's handle-slide margin curve — y2 > 1 gives the deliberate settle-past overshoot.
  private static final Easing OVERSHOOT = Easing.cubicBezier(0.175f, 0.885f, 0.32f, 1.275f);

  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private final RetargetTween slideTween;
  private final RetargetTween sizeTween;

  private boolean selected;
  private boolean hovered;
  private boolean pressed;
  private boolean dragging;
  private int pressX;
  private float dragFraction;

  private String label;

  private boolean iconsVisible;
  private boolean showOnlySelectedIcon;
  private Icon selectedIcon = MaterialIcons.check(ICON_SIZE_PX);
  private Icon unselectedIcon = MaterialIcons.close(ICON_SIZE_PX);

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;

  /**
   * Creates an unselected switch.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSwitch() {
    this(false);
  }

  /**
   * Creates a switch in the given selection state.
   *
   * @param selected the initial selection state
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSwitch(final boolean selected) {
    this.selected = selected;
    this.slideTween = new RetargetTween(SLIDE_MS, selected ? 1f : 0f);
    this.sizeTween =
        new RetargetTween(SIZE_MORPH_MS, selected ? HANDLE_SELECTED_PX : HANDLE_UNSELECTED_PX);
    setOpaque(false);
    setFocusable(true);
    initInteraction();
    initKeyboard();
    addPropertyChangeListener("componentOrientation", e -> repaint());
  }

  // ------------------------------------------------------------------ selection

  /**
   * Returns whether the switch is selected (on).
   *
   * @return {@code true} if selected
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Sets the selection state. Fires the registered {@link ChangeListener}s only when the state
   * actually changes. Programmatic writes never fire the user-gesture {@code ActionListener}s (the
   * interaction story's surface) — mirroring material-web, whose {@code change} event fires on user
   * interaction only.
   *
   * @param selected the new selection state
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSelected(final boolean selected) {
    if (this.selected == selected) {
      return;
    }
    this.selected = selected;
    syncMotion(animateAllowed());
    fireStateChanged();
    if (accessibleContext != null) {
      accessibleContext.firePropertyChange(
          AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
          selected ? null : AccessibleState.CHECKED,
          selected ? AccessibleState.CHECKED : null);
    }
    repaint();
  }

  /**
   * Adds a {@link ChangeListener} notified on every selection-state change, user-driven or
   * programmatic.
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

  /** Notifies registered {@link ChangeListener}s of a selection-state change. */
  private void fireStateChanged() {
    final ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
    for (final ChangeListener listener : listeners) {
      listener.stateChanged(changeEvent);
    }
  }

  /**
   * Adds an {@link ActionListener} notified when the <em>user</em> toggles the switch — a click, a
   * Space release, or a drag commit that changed the state. Programmatic {@link
   * #setSelected(boolean)} writes never fire it (material-web parity: {@code change} fires on user
   * interaction only). The event's action command is {@code "selected"} or {@code "deselected"},
   * reflecting the state just committed.
   *
   * @param listener the listener to add
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addActionListener(final ActionListener listener) {
    listenerList.add(ActionListener.class, listener);
  }

  /**
   * Removes a previously added {@link ActionListener}.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeActionListener(final ActionListener listener) {
    listenerList.remove(ActionListener.class, listener);
  }

  /** Notifies registered {@link ActionListener}s of a user-gesture toggle. */
  private void fireActionPerformed() {
    final ActionEvent event =
        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected ? "selected" : "deselected");
    for (final ActionListener listener : listenerList.getListeners(ActionListener.class)) {
      listener.actionPerformed(event);
    }
  }

  /** Commits a user-gesture toggle: state change first, then the action event. */
  private void commitUserToggle(final boolean next) {
    final boolean changed = next != selected;
    setSelected(next);
    if (changed) {
      fireActionPerformed();
    }
  }

  /**
   * Returns the switch's accessible label, or {@code null} if none was set via {@link
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
   * Sets the switch's accessible name — the adjacent UI label a screen reader reads before the role
   * and state (research §A: a switch always needs one). Alternatively, associate a {@link
   * javax.swing.JLabel} via {@link javax.swing.JLabel#setLabelFor} and the name is derived from it
   * automatically; an explicit value here takes precedence.
   *
   * @param label the accessible label, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setLabel(final String label) {
    this.label = label;
  }

  // -------------------------------------------------------------- gallery hooks

  /**
   * Forces the hover treatment on or off — for static gallery rendering only (a parallel to the
   * slider's hook); real hover tracking is automatic.
   *
   * @param hovered whether to paint the hover state layer
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setHovered(final boolean hovered) {
    this.hovered = hovered;
    repaint();
  }

  /**
   * Forces the pressed treatment on or off — for static gallery rendering only (a parallel to the
   * slider's hook); real press tracking is automatic.
   *
   * @param pressed whether to paint the pressed treatment (grown handle + pressed layer)
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setPressed(final boolean pressed) {
    this.pressed = pressed;
    syncMotion(false);
    repaint();
  }

  // --------------------------------------------------------------------- icons

  /**
   * Returns whether icons are shown on the handle in both states (M3's {@code icons}
   * configuration).
   *
   * @return {@code true} if both handle icons are shown
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isIconsVisible() {
    return iconsVisible;
  }

  /**
   * Shows or hides the handle icons in <em>both</em> states — {@linkplain #getSelectedIcon() the
   * selected glyph} on the selected handle and {@linkplain #getUnselectedIcon() the unselected
   * glyph} on the unselected one. A handle whose icon shows rides at {@value #HANDLE_WITH_ICON_PX}
   * px. Defaults to {@code false}; {@link #setShowOnlySelectedIcon(boolean)} wins when both are
   * set.
   *
   * @param visible {@code true} to show icons in both states
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setIconsVisible(final boolean visible) {
    if (this.iconsVisible == visible) {
      return;
    }
    this.iconsVisible = visible;
    syncMotion(animateAllowed());
    repaint();
  }

  /**
   * Returns whether only the selected handle shows its icon.
   *
   * @return {@code true} if the icon shows on the selected handle only
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isShowOnlySelectedIcon() {
    return showOnlySelectedIcon;
  }

  /**
   * Restricts the handle icon to the <em>selected</em> state (M3's {@code show-only-selected-icon}
   * configuration) — the unselected handle returns to {@value #HANDLE_UNSELECTED_PX} px with no
   * glyph, and the selected glyph rotates &minus;45&deg; into view on selection. Wins over {@link
   * #setIconsVisible(boolean)} when both are set. Defaults to {@code false}.
   *
   * @param showOnlySelected {@code true} to show the icon on the selected handle only
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setShowOnlySelectedIcon(final boolean showOnlySelected) {
    if (this.showOnlySelectedIcon == showOnlySelected) {
      return;
    }
    this.showOnlySelectedIcon = showOnlySelected;
    syncMotion(animateAllowed());
    repaint();
  }

  /**
   * Returns the selected-handle glyph — the custom icon if one was set, otherwise the {@link
   * MaterialIcons#check(int) check} default.
   *
   * @return the selected-handle icon; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public Icon getSelectedIcon() {
    return selectedIcon;
  }

  /**
   * Replaces the selected-handle glyph. The icon is recolored to the state's role at paint time by
   * compositing, so any {@link Icon} works; glyphs at {@value #ICON_SIZE_PX} px match the M3
   * geometry.
   *
   * @param icon the new selected-handle icon, or {@code null} to restore the check default
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSelectedIcon(final Icon icon) {
    this.selectedIcon = icon != null ? icon : MaterialIcons.check(ICON_SIZE_PX);
    repaint();
  }

  /**
   * Returns the unselected-handle glyph — the custom icon if one was set, otherwise the {@link
   * MaterialIcons#close(int) close} default.
   *
   * @return the unselected-handle icon; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public Icon getUnselectedIcon() {
    return unselectedIcon;
  }

  /**
   * Replaces the unselected-handle glyph. The icon is recolored to the state's role at paint time
   * by compositing, so any {@link Icon} works; glyphs at {@value #ICON_SIZE_PX} px match the M3
   * geometry.
   *
   * @param icon the new unselected-handle icon, or {@code null} to restore the close default
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setUnselectedIcon(final Icon icon) {
    this.unselectedIcon = icon != null ? icon : MaterialIcons.close(ICON_SIZE_PX);
    repaint();
  }

  /** Whether the selected handle's icon is enabled (either icons mode). */
  private boolean selectedIconEnabled() {
    return iconsVisible || showOnlySelectedIcon;
  }

  /** Whether the unselected handle's icon is enabled (both-icons mode only). */
  private boolean unselectedIconEnabled() {
    return iconsVisible && !showOnlySelectedIcon;
  }

  // -------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(
        TRACK_WIDTH_PX + 2 * HALO_OVERHANG_PX, TRACK_HEIGHT_PX + 2 * HALO_OVERHANG_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ------------------------------------------------------------------ geometry

  /** The x of the track block's left edge — centered in the component bounds. */
  private int trackX() {
    return (getWidth() - TRACK_WIDTH_PX) / 2;
  }

  /** The y of the track block's top edge — centered in the component bounds. */
  private int trackY() {
    return (getHeight() - TRACK_HEIGHT_PX) / 2;
  }

  /** The handle center's x at the unselected (start) end of its travel. */
  private int travelStartX() {
    return trackX() + TRACK_HEIGHT_PX / 2;
  }

  /** The handle center's x at the selected (end) end of its travel. */
  private int travelEndX() {
    return trackX() + TRACK_WIDTH_PX - TRACK_HEIGHT_PX / 2;
  }

  /** Whether the travel is mirrored — a right-to-left {@code ComponentOrientation}. */
  private boolean mirror() {
    return !getComponentOrientation().isLeftToRight();
  }

  /**
   * The handle center's x — the slide tween's position (deliberately unclamped: the overshoot
   * swings past the rest point), or the scrub position mid-drag. Value-space fractions ({@code 0} =
   * unselected, {@code 1} = selected) flip to pixel space under a right-to-left orientation.
   */
  int handleCenterX() {
    final float fraction = dragging ? dragFraction : slideTween.value();
    final float pixelFraction = mirror() ? 1f - fraction : fraction;
    return Math.round(travelStartX() + pixelFraction * (travelEndX() - travelStartX()));
  }

  /** The handle center's y — the track's vertical center. */
  int handleCenterY() {
    return trackY() + TRACK_HEIGHT_PX / 2;
  }

  /** Maps a pointer x to a value-space fraction {@code [0, 1]} — orientation-aware. */
  private float fractionForX(final int x) {
    final int start = travelStartX();
    final int end = travelEndX();
    if (end <= start) {
      return 0f;
    }
    final float pixelFraction = clampF((x - start) / (float) (end - start));
    return mirror() ? 1f - pixelFraction : pixelFraction;
  }

  /** The handle diameter — the size tween's current value. */
  private float handleDiameter() {
    return sizeTween.value();
  }

  /** The color-crossfade progress {@code [0, 1]} — the slide position, overshoot clamped off. */
  private float colorProgress() {
    return clampF(dragging ? dragFraction : slideTween.value());
  }

  /**
   * Retargets the slide + size tweens at the current state; snaps when animation is not allowed.
   */
  private void syncMotion(final boolean animate) {
    slideTween.retarget(selected ? 1f : 0f, SLIDE_MS, OVERSHOOT, animate);
    final boolean press = pressed || dragging;
    final float diameter;
    if (press) {
      diameter = HANDLE_PRESSED_PX;
    } else if (selected) {
      diameter = selectedIconEnabled() ? HANDLE_WITH_ICON_PX : HANDLE_SELECTED_PX;
    } else {
      diameter = unselectedIconEnabled() ? HANDLE_WITH_ICON_PX : HANDLE_UNSELECTED_PX;
    }
    final boolean pressTransition = press || sizeTween.target() == HANDLE_PRESSED_PX;
    sizeTween.retarget(
        diameter,
        pressTransition ? PRESS_MORPH_MS : SIZE_MORPH_MS,
        pressTransition ? Easing.LINEAR : Easing.STANDARD,
        animate);
  }

  /** Whether state changes may tween — never while undisplayable (first paint) or disabled. */
  private boolean animateAllowed() {
    return isDisplayable() && isEnabled();
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintTrack(g2);
      paintStateLayer(g2);
      paintRipple(g2);
      paintHandle(g2);
      paintIcons(g2);
    } finally {
      g2.dispose();
    }
  }

  /** The {@value #STATE_LAYER_SIZE_PX} dp interaction circle concentric with the handle. */
  private Ellipse2D.Float stateLayerCircle() {
    final float r = STATE_LAYER_SIZE_PX / 2f;
    return new Ellipse2D.Float(
        handleCenterX() - r, handleCenterY() - r, STATE_LAYER_SIZE_PX, STATE_LAYER_SIZE_PX);
  }

  /** The state-layer tint — crossfaded with the slide (research §T roles at the rest points). */
  private Color stateLayerTint() {
    return mixColor(ColorRole.ON_SURFACE.resolve(), ColorRole.PRIMARY.resolve(), colorProgress());
  }

  /** Paints the static hover/focus/pressed layers on the circle riding the handle. */
  private void paintStateLayer(final Graphics2D g2) {
    if (!isEnabled()) {
      return;
    }
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      final Ellipse2D.Float halo = stateLayerCircle();
      final Color tint = stateLayerTint();
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
      if (pressed || dragging) {
        s.setComposite(AlphaComposite.SrcOver.derive(StateLayer.PRESSED.opacity()));
        s.setColor(tint);
        s.fill(halo);
      }
    } finally {
      s.dispose();
    }
  }

  /** Paints the press ripple, bounded to the state-layer circle (material-web's md-ripple). */
  private void paintRipple(final Graphics2D g2) {
    if (rippleOrigin == null || rippleProgress >= 1f || !isEnabled()) {
      return;
    }
    final Ellipse2D.Float halo = stateLayerCircle();
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(halo.x, halo.y);
      final Point local = new Point(rippleOrigin.x - (int) halo.x, rippleOrigin.y - (int) halo.y);
      RipplePainter.paint(
          r,
          STATE_LAYER_SIZE_PX,
          STATE_LAYER_SIZE_PX,
          local,
          rippleProgress,
          STATE_LAYER_SIZE_PX,
          stateLayerTint());
    } finally {
      r.dispose();
    }
  }

  /**
   * Paints the track capsule. The unselected outline is a filled ring ({@link Area} subtraction,
   * not a stroke) so the ring and the interior tile exactly — no half-pixel seam and, in the
   * disabled treatment, no double-blended overlap between the two translucent fills (design §12).
   * Mid-slide, the selected {@link ColorRole#PRIMARY} fill paints over the unselected chrome at the
   * crossfade progress — the outline fades out as the fill fades in (design §6).
   */
  private void paintTrack(final Graphics2D g2) {
    if (!isEnabled()) {
      final float opacity = StateLayer.disabledContainerOpacity();
      if (selected) {
        g2.setColor(withAlpha(ColorRole.ON_SURFACE.resolve(), opacity));
        g2.fill(trackCapsule(0));
      } else {
        g2.setColor(withAlpha(ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(), opacity));
        g2.fill(trackCapsule(TRACK_OUTLINE_PX));
        g2.setColor(withAlpha(ColorRole.ON_SURFACE.resolve(), opacity));
        g2.fill(trackRing());
      }
      return;
    }
    final float p = colorProgress();
    if (p < 1f) {
      g2.setColor(ColorRole.SURFACE_CONTAINER_HIGHEST.resolve());
      g2.fill(trackCapsule(TRACK_OUTLINE_PX));
      g2.setColor(ColorRole.OUTLINE.resolve());
      g2.fill(trackRing());
    }
    if (p > 0f) {
      g2.setColor(withAlpha(ColorRole.PRIMARY.resolve(), p));
      g2.fill(trackCapsule(0));
    }
  }

  /** The track capsule, inset uniformly — {@code 0} is the full track outline. */
  private RoundRectangle2D.Float trackCapsule(final int inset) {
    final float arc = TRACK_HEIGHT_PX - 2f * inset;
    return new RoundRectangle2D.Float(
        trackX() + inset, trackY() + inset, TRACK_WIDTH_PX - 2f * inset, arc, arc, arc);
  }

  /** The unselected outline ring — outer capsule minus the outline-inset capsule. */
  private Area trackRing() {
    final Area ring = new Area(trackCapsule(0));
    ring.subtract(new Area(trackCapsule(TRACK_OUTLINE_PX)));
    return ring;
  }

  /** Paints the circular handle at its resting position for the current state. */
  private void paintHandle(final Graphics2D g2) {
    final float diameter = handleDiameter();
    final float x = handleCenterX() - diameter / 2f;
    final float y = handleCenterY() - diameter / 2f;
    g2.setColor(handleColor());
    g2.fill(new Ellipse2D.Float(x, y, diameter, diameter));
  }

  /**
   * Paints the enabled handle glyphs, crossfaded along the slide: the selected icon's alpha rises
   * with the progress (rotating &minus;45&deg;&rarr;0 in selected-icon-only mode — research §Mo),
   * the unselected icon's falls. Disabled states paint their single icon at the 0.38 content
   * opacity (research §T).
   */
  private void paintIcons(final Graphics2D g2) {
    if (!selectedIconEnabled() && !unselectedIconEnabled()) {
      return;
    }
    final float p = colorProgress();
    final int cx = handleCenterX();
    final int cy = handleCenterY();
    final float disabledFactor = isEnabled() ? 1f : StateLayer.disabledContentOpacity();
    if (selectedIconEnabled() && p > 0f) {
      final Color color =
          isEnabled() ? ColorRole.ON_PRIMARY_CONTAINER.resolve() : ColorRole.ON_SURFACE.resolve();
      final double rotation = showOnlySelectedIcon ? Math.toRadians(-45.0 * (1.0 - p)) : 0.0;
      paintGlyph(g2, selectedIcon, cx, cy, color, p * disabledFactor, rotation);
    }
    if (unselectedIconEnabled() && p < 1f) {
      final Color color = ColorRole.SURFACE_CONTAINER_HIGHEST.resolve();
      paintGlyph(g2, unselectedIcon, cx, cy, color, (1f - p) * disabledFactor, 0.0);
    }
  }

  /**
   * Paints one glyph centered at {@code (cx, cy)}, recolored by {@code SrcIn} compositing into an
   * offscreen buffer — works for any {@link Icon} and never mutates a shared {@code FlatSVGIcon}
   * color filter (the #197 lesson).
   */
  private void paintGlyph(
      final Graphics2D g2,
      final Icon icon,
      final int cx,
      final int cy,
      final Color color,
      final float alpha,
      final double rotation) {
    if (icon == null || alpha <= 0f) {
      return;
    }
    final int w = icon.getIconWidth();
    final int h = icon.getIconHeight();
    if (w <= 0 || h <= 0) {
      return;
    }
    final BufferedImage buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D ig = buffer.createGraphics();
    try {
      ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      icon.paintIcon(this, ig, 0, 0);
      ig.setComposite(AlphaComposite.SrcIn);
      ig.setColor(color);
      ig.fillRect(0, 0, w, h);
    } finally {
      ig.dispose();
    }
    final Graphics2D c = (Graphics2D) g2.create();
    try {
      c.setComposite(AlphaComposite.SrcOver.derive(clampF(alpha)));
      if (rotation != 0.0) {
        c.rotate(rotation, cx, cy);
      }
      c.drawImage(buffer, cx - w / 2, cy - h / 2, null);
    } finally {
      c.dispose();
    }
  }

  // --------------------------------------------------------------------- color

  /**
   * The handle fill, honoring the per-state roles and the disabled treatment — including the spec's
   * asymmetry: the disabled <em>selected</em> handle is opaque {@link ColorRole#SURFACE} (a solid
   * "hole" punched in the 12% track), while the disabled unselected handle is {@link
   * ColorRole#ON_SURFACE} at the 0.38 content opacity (research §T ⚠). Hover/focus/press shift the
   * enabled handle to {@link ColorRole#PRIMARY_CONTAINER} (selected) / {@link
   * ColorRole#ON_SURFACE_VARIANT} (unselected).
   */
  private Color handleColor() {
    if (!isEnabled()) {
      return selected
          ? ColorRole.SURFACE.resolve()
          : withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    final boolean active = hovered || pressed || dragging || isFocusOwner();
    final Color unselectedRole =
        (active ? ColorRole.ON_SURFACE_VARIANT : ColorRole.OUTLINE).resolve();
    final Color selectedRole =
        (active ? ColorRole.PRIMARY_CONTAINER : ColorRole.ON_PRIMARY).resolve();
    return mixColor(unselectedRole, selectedRole, colorProgress());
  }

  // ------------------------------------------------------------- accessibility

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaSwitch();
    }
    return accessibleContext;
  }

  /**
   * Accessible context for the switch — the {@code JToggleButton} shape, Swing's closest mapping
   * for the web {@code switch} role (research §X): {@link AccessibleRole#TOGGLE_BUTTON}, {@link
   * AccessibleState#CHECKED} while selected, one "click" {@link AccessibleAction} performing the
   * user-gesture toggle (assistive tech acts as the user, so it fires {@code ActionListener}s), and
   * an {@link AccessibleValue} of 0/1 (programmatic, so it fires {@code ChangeListener}s only). The
   * accessible name comes from {@link ElwhaSwitch#setLabel(String)}, falling back to an associated
   * {@code labelFor} {@link javax.swing.JLabel}.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  protected class AccessibleElwhaSwitch extends AccessibleJComponent
      implements AccessibleAction, AccessibleValue {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.TOGGLE_BUTTON;
    }

    @Override
    public String getAccessibleName() {
      if (label != null && !label.isEmpty()) {
        return label;
      }
      return super.getAccessibleName();
    }

    @Override
    public AccessibleStateSet getAccessibleStateSet() {
      final AccessibleStateSet states = super.getAccessibleStateSet();
      if (selected) {
        states.add(AccessibleState.CHECKED);
      }
      return states;
    }

    @Override
    public AccessibleAction getAccessibleAction() {
      return this;
    }

    @Override
    public AccessibleValue getAccessibleValue() {
      return this;
    }

    @Override
    public int getAccessibleActionCount() {
      return 1;
    }

    @Override
    public String getAccessibleActionDescription(final int i) {
      return i == 0 ? "click" : null;
    }

    @Override
    public boolean doAccessibleAction(final int i) {
      if (i != 0 || !ElwhaSwitch.this.isEnabled()) {
        return false;
      }
      commitUserToggle(!selected);
      return true;
    }

    @Override
    public Number getCurrentAccessibleValue() {
      return selected ? 1 : 0;
    }

    @Override
    public boolean setCurrentAccessibleValue(final Number n) {
      if (n == null) {
        return false;
      }
      setSelected(n.intValue() != 0);
      return true;
    }

    @Override
    public Number getMinimumAccessibleValue() {
      return 0;
    }

    @Override
    public Number getMaximumAccessibleValue() {
      return 1;
    }
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
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            requestFocusInWindow();
            pressed = true;
            pressX = e.getX();
            dragFraction = selected ? 1f : 0f;
            startRipple(new Point(handleCenterX(), handleCenterY()));
            syncMotion(animateAllowed());
            repaint();
          }

          @Override
          public void mouseDragged(final MouseEvent e) {
            if (!isEnabled() || !pressed) {
              return;
            }
            if (!dragging && Math.abs(e.getX() - pressX) >= DRAG_THRESHOLD_PX) {
              dragging = true;
            }
            if (dragging) {
              dragFraction = fractionForX(e.getX());
              repaint();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed) {
              return;
            }
            final boolean wasDragging = dragging;
            final float fraction = dragFraction;
            if (wasDragging) {
              slideTween.seed(fraction);
            }
            pressed = false;
            dragging = false;
            if (!isEnabled()) {
              syncMotion(false);
              repaint();
              return;
            }
            if (wasDragging) {
              commitUserToggle(fraction >= 0.5f);
            } else if (contains(e.getPoint())) {
              commitUserToggle(!selected);
            }
            syncMotion(animateAllowed());
            repaint();
          }
        };
    addMouseListener(ma);
    addMouseMotionListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            if (dragging) {
              slideTween.seed(dragFraction);
            }
            pressed = false;
            dragging = false;
            syncMotion(animateAllowed());
            repaint();
          }
        });
  }

  private void initKeyboard() {
    getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "elwhaSwitch.press");
    getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "elwhaSwitch.release");
    getActionMap()
        .put(
            "elwhaSwitch.press",
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                if (!ElwhaSwitch.this.isEnabled() || pressed) {
                  return;
                }
                pressed = true;
                startRipple(new Point(handleCenterX(), handleCenterY()));
                syncMotion(animateAllowed());
                repaint();
              }
            });
    getActionMap()
        .put(
            "elwhaSwitch.release",
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                if (!ElwhaSwitch.this.isEnabled() || !pressed) {
                  return;
                }
                pressed = false;
                commitUserToggle(!selected);
                syncMotion(animateAllowed());
                repaint();
              }
            });
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

  @Override
  public void removeNotify() {
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    slideTween.finish();
    sizeTween.finish();
    super.removeNotify();
  }

  // -------------------------------------------------------------------- helpers

  private static Color withAlpha(final Color base, final float opacity) {
    final int a = Math.round(clampF(opacity) * 255f);
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
  }

  private static float clampF(final float v) {
    return Math.max(0f, Math.min(1f, v));
  }

  /** Opaque RGB lerp between two colors. */
  private static Color mixColor(final Color a, final Color b, final float t) {
    final float f = clampF(t);
    return new Color(
        Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
        Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
        Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
  }

  /**
   * A retargeting tween over one float quantity — the CSS-transition model the research §Mo
   * durations describe. Each {@link #retarget} captures the current value as the new starting point
   * and re-runs the backing {@link MorphAnimator} {@code 0→1} through the given {@link Easing}, so
   * a mid-flight direction change (deselect during the select slide, press during a release shrink)
   * continues from wherever the value currently is instead of jumping.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  private final class RetargetTween {

    private final MorphAnimator animator;
    private float from;
    private float to;
    private Easing easing = Easing.LINEAR;

    RetargetTween(final int durationMs, final float initial) {
      this.animator = new MorphAnimator(ElwhaSwitch.this, durationMs);
      this.from = initial;
      this.to = initial;
      this.animator.snapTo(1f);
    }

    float value() {
      return from + (to - from) * easing.ease(animator.progress());
    }

    float target() {
      return to;
    }

    void retarget(
        final float target, final int durationMs, final Easing easing, final boolean animate) {
      if (this.to == target) {
        if (!animate) {
          this.from = target;
          animator.snapTo(1f);
        }
        return;
      }
      this.from = value();
      this.to = target;
      this.easing = easing;
      animator.setDurationMs(durationMs);
      if (animate) {
        animator.snapTo(0f);
        animator.start();
      } else {
        this.from = target;
        animator.snapTo(1f);
      }
    }

    /** Re-seats the tween at an externally-driven value (the drag handoff) without animating. */
    void seed(final float value) {
      this.from = value;
      this.to = value;
      animator.snapTo(1f);
    }

    /** Stops the timer and lands on the target — {@code removeNotify} cleanup. */
    void finish() {
      animator.immediateFinish();
    }
  }
}
