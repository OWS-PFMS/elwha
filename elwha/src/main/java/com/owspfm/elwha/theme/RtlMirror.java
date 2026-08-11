package com.owspfm.elwha.theme;

import java.awt.Container;
import java.awt.Insets;

/**
 * Mirrors a child's x position for a right-to-left {@link java.awt.ComponentOrientation}.
 *
 * <p>Every Elwha layout that lays children out along the x axis writes the same shape: compute
 * positions as if the container were left-to-right, then mirror on the way out. That keeps the
 * ordering logic in one direction — a segment still "advances from the leading edge" — and confines
 * the direction question to a single expression. Four components had independently written that
 * expression; this is it, once.
 *
 * <p>The mapping is <strong>its own inverse</strong>, which is what lets one method serve both
 * directions: layout code mirrors positions on the way out, and hit-testing or drag code mirrors
 * pointer positions on the way in.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it, exactly as {@link
 * MorphAnimator} and {@link RipplePainter} are.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class RtlMirror {

  private RtlMirror() {
    // utility
  }

  /**
   * Mirrors {@code x} within {@code [0, span]} — the identity when {@code leftToRight}.
   *
   * <p>Use this where the coordinate space already <em>is</em> the content box: a {@code doLayout}
   * placing children inside a component whose own insets are zero or already subtracted.
   *
   * @param leftToRight the container's resolved orientation; {@code true} returns {@code x}
   *     unchanged
   * @param span the width of the box to mirror within
   * @param x the left edge, measured as though the container were left-to-right
   * @param width the child's width; pass {@code 0} to mirror a bare point rather than a box
   * @return the mirrored left edge
   * @version v0.5.0
   * @since v0.5.0
   */
  public static int mirrorX(
      final boolean leftToRight, final int span, final int x, final int width) {
    return leftToRight ? x : span - x - width;
  }

  /**
   * Mirrors {@code x} within {@code parent}'s content box — its width less its {@linkplain
   * Container#getInsets() insets} — leaving coordinates in the parent's own space.
   *
   * <p>Use this where positions are measured against the parent rather than against the content
   * box, so the left inset has to survive the mirror.
   *
   * @param leftToRight the container's resolved orientation; {@code true} returns {@code x}
   *     unchanged
   * @param parent the container whose content box bounds the mirror
   * @param x the left edge in {@code parent}'s space, measured as though it were left-to-right
   * @param width the child's width; pass {@code 0} to mirror a bare point rather than a box
   * @return the mirrored left edge, in {@code parent}'s space
   * @version v0.5.0
   * @since v0.5.0
   */
  public static int mirrorX(
      final boolean leftToRight, final Container parent, final int x, final int width) {
    if (leftToRight) {
      return x;
    }
    final Insets insets = parent.getInsets();
    return insets.left + (parent.getWidth() - insets.right) - x - width;
  }
}
