package com.owspfm.elwha.surface.playground;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import javax.swing.JLabel;

/**
 * Headless smoke gate for the rounded-corner child-clip gating + buffer reuse ([#272]). Not an
 * interactive playground — it paints surfaces offscreen and asserts, via the private {@code
 * clipBufferCache} field, that the expensive offscreen buffer runs <em>only</em> when a surface
 * actually clips its children, and that it is reused (not reallocated) across repaints. Exits
 * non-zero on any mismatch so it doubles as a CI gate.
 *
 * <p>Three cases: (1) an inset-only {@link ElwhaSurface} never allocates the buffer across repeated
 * paints; (2) a surface with the public {@link ElwhaSurface#setClipChildrenToCorners(boolean)} flag
 * on does allocate it; (3) an {@link ElwhaCard} hosting an edge-bleed {@link ElwhaCardMedia} auto-
 * enables the clip, reuses one buffer across two paints, and still cuts the media to the chassis
 * corner (the #157 regression guard).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SurfaceClipGatingSmoke {

  private SurfaceClipGatingSmoke() {}

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    insetSurfaceNeverBuffers();
    optInFlagBuffers();
    mediaCardBuffersReusesAndCuts();

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println(
        "PASS: clip buffer runs only when clipping, is reused, and still cuts media.");
  }

  /** An inset-only surface (default flag off) must never allocate the offscreen clip buffer. */
  private static void insetSurfaceNeverBuffers() {
    final ElwhaSurface surface = new ElwhaSurface().setSurfaceRole(ColorRole.SURFACE_CONTAINER);
    surface.add(new JLabel("inset"));
    check("default flag is off", !surface.getClipChildrenToCorners());
    paintOffscreen(surface, 200, 120, Color.GREEN);
    paintOffscreen(surface, 200, 120, Color.GREEN);
    check("inset-only surface allocates no clip buffer", cachedBuffer(surface) == null);
  }

  /** Flipping the public opt-in flag on makes even a plain surface run the buffer. */
  private static void optInFlagBuffers() {
    final ElwhaSurface surface = new ElwhaSurface().setSurfaceRole(ColorRole.SURFACE_CONTAINER);
    surface.setClipChildrenToCorners(true);
    surface.add(opaqueCover(Color.RED));
    paintOffscreen(surface, 200, 120, Color.GREEN);
    check("opt-in flag runs the clip buffer", cachedBuffer(surface) != null);
  }

  /** A card with edge-bleed media auto-clips, reuses one buffer, and cuts the corner. */
  private static void mediaCardBuffersReusesAndCuts() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(ElwhaCardMedia.painter((g, w, h) -> fill(g, w, h, Color.RED)).setPreferredHeight(120));

    final BufferedImage first = paintOffscreen(card, 220, 140, Color.GREEN);
    final BufferedImage bufferAfterFirst = cachedBuffer(card);
    check("edge-bleed media auto-enables the clip buffer", bufferAfterFirst != null);

    paintOffscreen(card, 220, 140, Color.GREEN);
    check(
        "clip buffer is reused across same-size repaints", cachedBuffer(card) == bufferAfterFirst);

    // Media fills the body; near the rounded corner the media must be cut (shows the sentinel
    // backdrop, not red), while the body interior shows the opaque media (#157 still holds).
    check("corner pixel cut (media not painted into the round corner)", !isRed(first.getRGB(2, 2)));
    check(
        "interior pixel shows the opaque media",
        isRed(first.getRGB(first.getWidth() / 2, first.getHeight() / 2)));
  }

  private static Component opaqueCover(final Color color) {
    final JLabel cover = new JLabel();
    cover.setOpaque(true);
    cover.setBackground(color);
    return cover;
  }

  private static void fill(final Graphics2D g, final int w, final int h, final Color color) {
    g.setColor(color);
    g.fillRect(0, 0, w, h);
  }

  private static boolean isRed(final int argb) {
    final Color c = new Color(argb, true);
    return c.getAlpha() > 200 && c.getRed() > 180 && c.getGreen() < 80 && c.getBlue() < 80;
  }

  private static BufferedImage paintOffscreen(
      final Component c, final int w, final int h, final Color backdrop) {
    c.setSize(w, h);
    layoutTree(c);
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(backdrop);
      g.fillRect(0, 0, w, h);
      c.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static void layoutTree(final Component c) {
    c.doLayout();
    if (c instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static BufferedImage cachedBuffer(final ElwhaSurface surface) {
    try {
      final Field field = ElwhaSurface.class.getDeclaredField("clipBufferCache");
      field.setAccessible(true);
      final Object ref = field.get(surface);
      if (ref == null) {
        return null;
      }
      @SuppressWarnings("unchecked")
      final SoftReference<BufferedImage> typed = (SoftReference<BufferedImage>) ref;
      return typed.get();
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("cannot read clipBufferCache", e);
    }
  }

  private static void check(final String label, final boolean ok) {
    if (!ok) {
      System.out.println("  FAIL " + label);
      failures++;
    } else {
      System.out.println("  ok   " + label);
    }
  }
}
