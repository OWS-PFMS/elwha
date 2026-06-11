package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The Elwha Material 3 <strong>radio button</strong> — a token-themed single-selection control
 * painting M3's 20&nbsp;dp ring-and-dot icon: a {@value #RING_WIDTH_PX}&nbsp;dp ring (outer
 * &Oslash;{@value #ICON_SIZE_PX}) that fills with a &Oslash;{@value #DOT_SIZE_PX} inner dot while
 * selected.
 *
 * <p><strong>Architecture (load-bearing, locked by the S1 spike — design doc {@code
 * elwha-radiobutton-design.md} §2).</strong> {@code ElwhaRadioButton} is one dedicated {@link
 * JComponent} that paints every M3 part itself, holding the selection as a plain boolean. It is
 * <em>not</em> a styled {@code JRadioButton} and <em>not</em> a {@code ButtonUI} delegate: the
 * button UI's text/icon layout fights the icon-only anatomy, and {@code ButtonGroup} has no
 * vocabulary for the M3 group keyboard contract (selection-follows-focus arrows, roving tab stop)
 * that {@link ElwhaRadioGroup} carries.
 *
 * <p><strong>Color (zero new tokens — research §Tokens).</strong> Unselected: ring {@link
 * ColorRole#ON_SURFACE_VARIANT}. Selected: ring and dot {@link ColorRole#PRIMARY}. Disabled: ring
 * and dot {@link ColorRole#ON_SURFACE} at the M3 content opacity (0.38) — symmetric across both
 * sides, unlike the switch. All roles resolve at paint time so runtime theme + light/dark switching
 * re-skins the radio live.
 *
 * <p><strong>Geometry (M3 token-locked — research §T/§G).</strong> The ring is painted as a filled
 * ring ({@link Area} subtraction — material-web mask-builds it the same way), never a stroke, so
 * the disabled translucent fills cannot double-blend and no half-pixel seam appears. The preferred
 * size is the state-layer-inclusive box — the {@value #ICON_SIZE_PX}&nbsp;dp icon plus the {@value
 * #STATE_LAYER_SIZE_PX}&nbsp;dp interaction halo's {@value #HALO_OVERHANG_PX}&nbsp;dp overhang per
 * side; the chrome centers itself in larger bounds. The M3 48&nbsp;dp touch target is guidance for
 * touch contexts, not painted geometry.
 *
 * <p><strong>Labelling.</strong> A radio button never labels itself — M3 requires an adjacent label
 * and the accessible name must always be set (research §A); pair with a {@link javax.swing.JLabel}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaRadioButton extends JComponent {

  // --- M3 radio-button geometry (dp == px at 100% scale; research §T/§G) ---

  /** Icon size — the ring's outer diameter. */
  static final int ICON_SIZE_PX = 20;

  /** Ring width — material-web's r=10 circle masked by an r=8 hole. */
  static final int RING_WIDTH_PX = 2;

  /** Inner dot diameter while selected. */
  static final int DOT_SIZE_PX = 10;

  /** Diameter of the hover/focus/press state layer concentric with the icon. */
  static final int STATE_LAYER_SIZE_PX = 40;

  /** The state layer's overhang past the icon, per side. */
  static final int HALO_OVERHANG_PX = (STATE_LAYER_SIZE_PX - ICON_SIZE_PX) / 2;

  private final EventListenerList listenerList = new EventListenerList();
  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private boolean selected;

  /**
   * Creates an unselected radio button.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioButton() {
    this(false);
  }

  /**
   * Creates a radio button in the given selection state.
   *
   * @param selected the initial selection state
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaRadioButton(final boolean selected) {
    this.selected = selected;
    setOpaque(false);
    setFocusable(true);
  }

  // ------------------------------------------------------------------ selection

  /**
   * Returns whether the radio button is selected.
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
   * actually changes. Both directions are legal programmatically — only <em>user gestures</em> are
   * select-only (a user can never deselect a radio; research §B).
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
    for (final ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(changeEvent);
    }
  }

  // -------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(STATE_LAYER_SIZE_PX, STATE_LAYER_SIZE_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ------------------------------------------------------------------ geometry

  /** The icon center's x — the component center. */
  int iconCenterX() {
    return getWidth() / 2;
  }

  /** The icon center's y — the component center. */
  int iconCenterY() {
    return getHeight() / 2;
  }

  /** The icon circle, inset uniformly — {@code 0} is the ring's outer edge. */
  private Ellipse2D.Float iconCircle(final float inset) {
    final float d = ICON_SIZE_PX - 2f * inset;
    return new Ellipse2D.Float(iconCenterX() - d / 2f, iconCenterY() - d / 2f, d, d);
  }

  /** The ring — outer circle minus the ring-width-inset circle. */
  private Area ring() {
    final Area ring = new Area(iconCircle(0));
    ring.subtract(new Area(iconCircle(RING_WIDTH_PX)));
    return ring;
  }

  /** The inner dot at the given diameter, concentric with the icon. */
  private Ellipse2D.Float dotCircle(final float diameter) {
    return new Ellipse2D.Float(
        iconCenterX() - diameter / 2f, iconCenterY() - diameter / 2f, diameter, diameter);
  }

  // --------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintIcon(g2);
    } finally {
      g2.dispose();
    }
  }

  /** Paints the ring, and the dot while selected. */
  private void paintIcon(final Graphics2D g2) {
    g2.setColor(ringColor());
    g2.fill(ring());
    if (selected) {
      g2.setColor(dotColor());
      g2.fill(dotCircle(DOT_SIZE_PX));
    }
  }

  // --------------------------------------------------------------------- color

  /** The ring fill — per-state roles, disabled at the 0.38 content opacity (research §T). */
  private Color ringColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return (selected ? ColorRole.PRIMARY : ColorRole.ON_SURFACE_VARIANT).resolve();
  }

  /** The dot fill — {@link ColorRole#PRIMARY}, disabled at the 0.38 content opacity. */
  private Color dotColor() {
    if (!isEnabled()) {
      return withAlpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return ColorRole.PRIMARY.resolve();
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
