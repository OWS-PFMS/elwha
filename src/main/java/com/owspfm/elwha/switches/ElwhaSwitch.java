package com.owspfm.elwha.switches;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
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
 * <p><strong>Labelling.</strong> A switch never labels itself — M3 requires an adjacent label
 * naming what it toggles, and the accessible name must always be set: call {@code setLabel(String)}
 * or associate a {@link javax.swing.JLabel} via {@link javax.swing.JLabel#setLabelFor} (the a11y
 * story wires both).
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

  /** Diameter of the hover/focus/press state layer riding the handle. */
  static final int STATE_LAYER_SIZE_PX = 40;

  /** The state layer's overhang past the track box, per side. */
  static final int HALO_OVERHANG_PX = (STATE_LAYER_SIZE_PX - TRACK_HEIGHT_PX) / 2;

  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private boolean selected;

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
    setOpaque(false);
    setFocusable(true);
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
    fireStateChanged();
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

  /** The handle center's resting x for the current selection state. */
  int handleCenterX() {
    final int inset = TRACK_HEIGHT_PX / 2;
    return selected ? trackX() + TRACK_WIDTH_PX - inset : trackX() + inset;
  }

  /** The handle center's y — the track's vertical center. */
  int handleCenterY() {
    return trackY() + TRACK_HEIGHT_PX / 2;
  }

  /** The handle diameter for the current state. */
  private float handleDiameter() {
    return selected ? HANDLE_SELECTED_PX : HANDLE_UNSELECTED_PX;
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintTrack(g2);
      paintHandle(g2);
    } finally {
      g2.dispose();
    }
  }

  /**
   * Paints the track capsule. The unselected outline is a filled ring ({@link Area} subtraction,
   * not a stroke) so the ring and the interior tile exactly — no half-pixel seam and, in the
   * disabled treatment, no double-blended overlap between the two translucent fills (design §12).
   */
  private void paintTrack(final Graphics2D g2) {
    final RoundRectangle2D.Float outer =
        new RoundRectangle2D.Float(
            trackX(), trackY(), TRACK_WIDTH_PX, TRACK_HEIGHT_PX, TRACK_HEIGHT_PX, TRACK_HEIGHT_PX);
    if (selected) {
      g2.setColor(selectedTrackColor());
      g2.fill(outer);
      return;
    }
    final int inset = TRACK_OUTLINE_PX;
    final RoundRectangle2D.Float inner =
        new RoundRectangle2D.Float(
            trackX() + inset,
            trackY() + inset,
            TRACK_WIDTH_PX - 2 * inset,
            TRACK_HEIGHT_PX - 2 * inset,
            TRACK_HEIGHT_PX - 2 * inset,
            TRACK_HEIGHT_PX - 2 * inset);
    final Area ring = new Area(outer);
    ring.subtract(new Area(inner));
    g2.setColor(unselectedTrackFillColor());
    g2.fill(inner);
    g2.setColor(unselectedTrackOutlineColor());
    g2.fill(ring);
  }

  /** Paints the circular handle at its resting position for the current state. */
  private void paintHandle(final Graphics2D g2) {
    final float diameter = handleDiameter();
    final float x = handleCenterX() - diameter / 2f;
    final float y = handleCenterY() - diameter / 2f;
    g2.setColor(handleColor());
    g2.fill(new Ellipse2D.Float(x, y, diameter, diameter));
  }

  // --------------------------------------------------------------------- color

  /** The selected track fill, honoring the disabled treatment (research §T). */
  private Color selectedTrackColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContainerOpacity());
    }
    return ColorRole.PRIMARY.resolve();
  }

  /** The unselected track interior fill, honoring the disabled treatment (research §T). */
  private Color unselectedTrackFillColor() {
    final Color base = ColorRole.SURFACE_CONTAINER_HIGHEST.resolve();
    if (!isEnabled()) {
      return withAlpha(base, StateLayer.disabledContainerOpacity());
    }
    return base;
  }

  /** The unselected track outline ring, honoring the disabled treatment (research §T). */
  private Color unselectedTrackOutlineColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContainerOpacity());
    }
    return ColorRole.OUTLINE.resolve();
  }

  /**
   * The handle fill, honoring the disabled treatment — including the spec's asymmetry: the disabled
   * <em>selected</em> handle is opaque {@link ColorRole#SURFACE} (a solid "hole" punched in the 12%
   * track), while the disabled unselected handle is {@link ColorRole#ON_SURFACE} at the 0.38
   * content opacity (research §T ⚠).
   */
  private Color handleColor() {
    if (!isEnabled()) {
      return selected
          ? ColorRole.SURFACE.resolve()
          : withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return selected ? ColorRole.ON_PRIMARY.resolve() : ColorRole.OUTLINE.resolve();
  }

  // -------------------------------------------------------------------- helpers

  private static Color withAlpha(final Color base, final float opacity) {
    final int a = Math.round(clampF(opacity) * 255f);
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
  }

  private static float clampF(final float v) {
    return Math.max(0f, Math.min(1f, v));
  }
}
