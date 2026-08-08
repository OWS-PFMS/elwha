package com.owspfm.elwha.theme;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * The uniform accessor for a primitive's <strong>visible body</strong> — the rect it actually
 * paints, inside whatever bounds a layout granted it.
 *
 * <p>Elwha primitives routinely paint smaller than their bounds. A fill layout ({@code GridLayout},
 * {@code BorderLayout.CENTER}, {@code GridBagLayout} with fill, a stretching {@code BoxLayout})
 * hands a component arbitrary extra space and the visible pill, track or box floats centered in a
 * larger invisible rect; {@link ShadowBearing} halos additionally live <em>inside</em> the bounds;
 * and a size that inflates for the WCAG 48&nbsp;dp target does the same thing even at preferred
 * size. Anything that positions against {@code getBounds()} therefore addresses an edge nobody can
 * see — a tooltip's 4&nbsp;dp gap measured to the cell edge rather than the pill, a trailing badge
 * stranded in dead space, a menu opening off the bottom of an invisible rect (#493). Hit testing
 * has the same shape from the input side: a button whose bounds stretch accepts clicks far outside
 * its painted pill (#505).
 *
 * <p>Only the component knows where its body is, which is why this has to be a contract rather than
 * a calculation callers can do. It is a sibling of {@link ShadowBearing} rather than an extension
 * of it: most primitives that center a body carry no halo at all, and making them answer {@code
 * getShadowInsets()} with zeros to get a body rect would be backwards. Both follow the {@code
 * IconBearing} shape — one narrow accessor, implemented by every primitive that conforms, queried
 * by the placement helper.
 *
 * <p><strong>Consumers should call {@link #bodyBoundsOf(Component)}, not this method
 * directly.</strong> That resolver degrades correctly for the components that do not implement this
 * interface, so a placement helper handles the whole catalog — and any consumer component — with
 * one call.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public interface BodyBearing {

  /**
   * Returns the visible painted body in this component's own coordinate space.
   *
   * <p>The rect a consumer should anchor to, hit-test against, or measure a spec gap from. It
   * excludes any shadow halo, any centering slack a stretching layout introduced, and any padding
   * added purely to reach a minimum touch target. For a component painting at exactly its preferred
   * size with no halo, this is {@code (0, 0, getWidth(), getHeight())}.
   *
   * <p>Implementations must return a fresh {@link Rectangle} each call — the body rect is derived
   * from internal geometry and callers must not be able to mutate it.
   *
   * @return the painted body rect in component coordinates (never {@code null})
   * @version v0.5.0
   * @since v0.5.0
   */
  Rectangle getBodyBounds();

  /**
   * Resolves any component's visible body rect, in that component's own coordinates — the call
   * every placement helper and hit test should make.
   *
   * <p>Three tiers, most-informed first. A {@link BodyBearing} answers for itself. A {@link
   * ShadowBearing} that is not one has its halo backed out of its bounds, which recovers the body
   * for a primitive whose only inset is the shadow reserve — this is the correction {@code
   * ElwhaTooltip} previously applied by hand, now shared. Anything else, including an arbitrary
   * consumer component, paints its whole bounds as far as anyone can tell, so its bounds are its
   * body.
   *
   * @param component the component to measure; {@code null} yields an empty rect
   * @return the body rect in {@code component}'s coordinate space (never {@code null})
   * @version v0.5.0
   * @since v0.5.0
   */
  static Rectangle bodyBoundsOf(final Component component) {
    if (component == null) {
      return new Rectangle();
    }
    if (component instanceof BodyBearing bearing) {
      return bearing.getBodyBounds();
    }
    if (component instanceof ShadowBearing bearing) {
      final Insets halo = bearing.getShadowInsets();
      return new Rectangle(
          halo.left,
          halo.top,
          Math.max(0, component.getWidth() - halo.left - halo.right),
          Math.max(0, component.getHeight() - halo.top - halo.bottom));
    }
    return new Rectangle(0, 0, component.getWidth(), component.getHeight());
  }
}
