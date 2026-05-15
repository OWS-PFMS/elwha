package com.owspfm.elwha.surface;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * A token-native rounded surface primitive — a {@link JPanel} subclass that paints a role-filled,
 * round-rect, optionally outlined background through the shared {@link SurfacePainter} helper.
 *
 * <p><strong>What it is.</strong> {@code ElwhaSurface} is Elwha's equivalent of Material's
 * <em>Paper</em>: the bare round-rect container that the rest of the component set composes for its
 * background paint. It carries no interaction model — no hover / pressed / selected state layers,
 * no focus indicator, no padding API. Consumers needing an interactive surface should compose an
 * {@code ElwhaSurface} inside {@link com.owspfm.elwha.chip.ElwhaChip}, the upcoming {@code
 * ElwhaIconButton}, or {@code ElwhaCard} V2.
 *
 * <p><strong>Typed API only.</strong> The four setters — {@link #setSurfaceRole(ColorRole)}, {@link
 * #setShape(ShapeScale)}, {@link #setBorderRole(ColorRole)}, {@link #setBorderWidth(int)} — accept
 * tokens or pixel counts only. There is no {@code setSurfaceColor(Color)} / {@code
 * setCornerRadius(int)} / raw-typed escape hatch; the lesson from {@code ElwhaCard} V1 is that
 * every escape hatch becomes a long-term migration cost. Pre-1.0, the typed API is the only API.
 *
 * <p><strong>Defaults.</strong> {@link ColorRole#SURFACE} fill, {@link ShapeScale#MD} (12 px)
 * corner radius, no border. These match the locked {@code ElwhaCard} default so Card V2 composing
 * a Surface inherits the right look out of the box.
 *
 * <p><strong>Binding rule.</strong> The painter resolves every token at paint time, so a runtime
 * theme/mode switch re-skins the Surface on the next paint without any per-instance listener — see
 * {@code docs/research/elwha-token-taxonomy.md}.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaSurface card = new ElwhaSurface()
 *     .setSurfaceRole(ColorRole.SURFACE_CONTAINER)
 *     .setShape(ShapeScale.LG)
 *     .setBorderRole(ColorRole.OUTLINE_VARIANT);
 * card.add(new JLabel("Hello"));
 * }</pre>
 *
 * <p>Design decisions and Card V2 composition relationship: {@code
 * docs/research/elwha-surface-design.md}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaSurface extends JPanel {

  private ColorRole surfaceRole = ColorRole.SURFACE;
  private ShapeScale shape = ShapeScale.MD;
  private ColorRole borderRole;
  private int borderWidth = 1;

  /**
   * Creates a Surface with the default look — {@link ColorRole#SURFACE} fill, {@link ShapeScale#MD}
   * shape, no border. Use the fluent setters to override any of the four axes.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface() {
    setOpaque(false);
  }

  /**
   * Sets the surface fill role and triggers a repaint. Must not be {@code null} — Surface always
   * paints a fill; a transparent variant is intentionally not modeled here (compose the Surface
   * inside a parent that paints the desired background instead).
   *
   * @param role the fill role
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code role} is {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setSurfaceRole(final ColorRole role) {
    this.surfaceRole = Objects.requireNonNull(role, "surfaceRole");
    repaint();
    return this;
  }

  /**
   * Returns the current surface fill role.
   *
   * @return the surface role, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole getSurfaceRole() {
    return surfaceRole;
  }

  /**
   * Sets the corner-radius step and triggers a repaint. Must not be {@code null}.
   *
   * @param shape the shape step
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code shape} is {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setShape(final ShapeScale shape) {
    this.shape = Objects.requireNonNull(shape, "shape");
    repaint();
    return this;
  }

  /**
   * Returns the current corner-radius step.
   *
   * @return the shape step, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ShapeScale getShape() {
    return shape;
  }

  /**
   * Sets the border stroke role and triggers a repaint. Pass {@code null} to remove the border
   * entirely — the corollary is that an outlined Surface is just {@code
   * setBorderRole(ColorRole.OUTLINE)}.
   *
   * @param role the border role, or {@code null} to suppress the border
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setBorderRole(final ColorRole role) {
    this.borderRole = role;
    repaint();
    return this;
  }

  /**
   * Returns the current border stroke role, or {@code null} if no border is painted.
   *
   * @return the border role, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ColorRole getBorderRole() {
    return borderRole;
  }

  /**
   * Sets the border stroke width in pixels and triggers a repaint. Has no visible effect while
   * {@link #getBorderRole()} is {@code null}. A value of {@code 0} or less suppresses the border
   * just as {@code setBorderRole(null)} does.
   *
   * @param px the stroke width in pixels
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSurface setBorderWidth(final int px) {
    this.borderWidth = px;
    repaint();
    return this;
  }

  /**
   * Returns the current border stroke width in pixels.
   *
   * @return the stroke width
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getBorderWidth() {
    return borderWidth;
  }

  /**
   * Paints the surface — delegates the round-rect fill + optional border stroke to {@link
   * SurfacePainter} with no state-layer overlay. Child components are painted by the standard
   * {@code JComponent.paintChildren} pass after this returns.
   *
   * @param g the graphics context
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    SurfacePainter.paint(
        (Graphics2D) g,
        getWidth(),
        getHeight(),
        shape.px(),
        surfaceRole,
        null,
        borderRole,
        borderWidth);
  }
}
