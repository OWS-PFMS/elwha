package com.owspfm.ui.components.card.list;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.UIManager;

/**
 * Lazily-built custom cursors for {@link FlatCardList}'s drag interaction.
 *
 * <p>AWT only ships {@link Cursor#HAND_CURSOR} (pointer finger) and {@link Cursor#MOVE_CURSOR}
 * (4-direction arrow); modern desktop UIs distinguish "grab" (open hand: this surface is draggable)
 * from "grabbing" (closed fist: you are dragging right now). This helper fills the gap with a
 * three-tier loading strategy:
 *
 * <ol>
 *   <li>Capitaine cursor PNGs bundled under {@code cursors/} (LGPL-3.0; see LICENSE in that
 *       directory). Light or dark variant chosen automatically based on the active FlatLaf theme's
 *       panel background luminance.
 *   <li>If the PNGs can't be loaded for any reason, fall back to a Java2D-painted silhouette so the
 *       cursor is at least recognisable as a hand.
 *   <li>If even cursor creation fails (some platforms reject custom-cursor sizes), fall back to
 *       {@link Cursor#MOVE_CURSOR}.
 * </ol>
 *
 * <p>Theme switches at runtime invalidate the cache; the next call rebuilds with the new variant.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class Cursors {

  private static final int DESIGN_SIZE = 32;
  private static final int FALLBACK_SIZE = 16;

  private static Cursor grabCached;
  private static Cursor grabbingCached;
  private static Boolean cachedDark;

  private Cursors() {
    // utility class
  }

  /**
   * Returns the open-hand "grab" cursor (signals "this surface is draggable").
   *
   * @return the grab cursor
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Cursor grab() {
    refreshIfThemeChanged();
    if (grabCached == null) {
      grabCached = loadCursor("grab", isDarkTheme(), new Point(15, 8), "FlatCardList.grab", true);
    }
    return grabCached;
  }

  /**
   * Returns the closed-fist "grabbing" cursor (signals "you are currently dragging").
   *
   * @return the grabbing cursor
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Cursor grabbing() {
    refreshIfThemeChanged();
    if (grabbingCached == null) {
      grabbingCached =
          loadCursor("grabbing", isDarkTheme(), new Point(13, 13), "FlatCardList.grabbing", false);
    }
    return grabbingCached;
  }

  private static void refreshIfThemeChanged() {
    final boolean dark = isDarkTheme();
    if (cachedDark == null || cachedDark != dark) {
      cachedDark = dark;
      grabCached = null;
      grabbingCached = null;
    }
  }

  private static boolean isDarkTheme() {
    final Color panel = UIManager.getColor("Panel.background");
    if (panel == null) {
      return false;
    }
    return (panel.getRed() + panel.getGreen() + panel.getBlue()) / 3 < 128;
  }

  /**
   * Tries to load a PNG cursor at the platform's preferred size, then falls back to the bundled
   * smaller PNG, then to Java2D-painted shapes if no PNG is available.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static Cursor loadCursor(
      final String theBaseName,
      final boolean theDark,
      final Point theHotspot,
      final String theName,
      final boolean theOpenHand) {
    final Dimension best = bestCursorSize();
    final int requestedSize = best.width;
    final BufferedImage image = pickBestImage(theBaseName, theDark, requestedSize, theOpenHand);
    if (image == null) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
    final Point scaledHotspot =
        new Point(
            theHotspot.x * image.getWidth() / DESIGN_SIZE,
            theHotspot.y * image.getHeight() / DESIGN_SIZE);
    try {
      return Toolkit.getDefaultToolkit().createCustomCursor(image, scaledHotspot, theName);
    } catch (Exception | Error ignore) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
  }

  private static Dimension bestCursorSize() {
    final Dimension best = Toolkit.getDefaultToolkit().getBestCursorSize(DESIGN_SIZE, DESIGN_SIZE);
    if (best.width > 0 && best.height > 0) {
      return best;
    }
    return new Dimension(DESIGN_SIZE, DESIGN_SIZE);
  }

  private static BufferedImage pickBestImage(
      final String theBaseName,
      final boolean theDark,
      final int theRequestedSize,
      final boolean theOpenHand) {
    final String themeKey = theDark ? "dark" : "light";
    final int[] sizes = theRequestedSize >= 24 ? new int[] {32, 16} : new int[] {16, 32};
    for (int s : sizes) {
      final BufferedImage img = loadPng(theBaseName + "-" + themeKey + "-" + s + ".png");
      if (img != null) {
        return scaleIfNeeded(img, theRequestedSize);
      }
    }
    return paintFallback(theOpenHand, theRequestedSize);
  }

  private static BufferedImage loadPng(final String theFileName) {
    final URL url = Cursors.class.getResource("cursors/" + theFileName);
    if (url == null) {
      return null;
    }
    try (InputStream in = url.openStream()) {
      return ImageIO.read(in);
    } catch (IOException ignore) {
      return null;
    }
  }

  private static BufferedImage scaleIfNeeded(final BufferedImage theSrc, final int theTargetSize) {
    if (theSrc.getWidth() == theTargetSize && theSrc.getHeight() == theTargetSize) {
      return theSrc;
    }
    final BufferedImage out =
        new BufferedImage(theTargetSize, theTargetSize, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = out.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.drawImage(theSrc, 0, 0, theTargetSize, theTargetSize, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  // --------------------------------------------------------- fallback paint

  private static BufferedImage paintFallback(final boolean theOpenHand, final int theSize) {
    final int size = theSize > 0 ? theSize : FALLBACK_SIZE;
    return theOpenHand ? paintOpenHand(size) : paintClosedFist(size);
  }

  private static BufferedImage paintOpenHand(final int theSize) {
    final BufferedImage img = new BufferedImage(theSize, theSize, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final double s = theSize / 32.0;

      final Area hand = new Area();
      hand.add(new Area(rr(8, 16, 16, 14, 5, s)));
      hand.add(new Area(rr(8.5, 6, 3, 12, 1.5, s)));
      hand.add(new Area(rr(12, 4, 3, 14, 1.5, s)));
      hand.add(new Area(rr(15.5, 4, 3, 14, 1.5, s)));
      hand.add(new Area(rr(19, 6, 3, 12, 1.5, s)));
      hand.add(new Area(rr(3.5, 18, 7, 5, 2.5, s)));

      g.setColor(Color.WHITE);
      g.fill(hand);
      g.setColor(Color.BLACK);
      g.setStroke(new BasicStroke((float) Math.max(1.0, 1.2 * s)));
      g.draw(hand);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static BufferedImage paintClosedFist(final int theSize) {
    final BufferedImage img = new BufferedImage(theSize, theSize, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final double s = theSize / 32.0;

      final Area fist = new Area();
      fist.add(new Area(rr(7, 13, 18, 15, 6, s)));
      fist.add(new Area(rr(4, 16, 6, 7, 3, s)));
      fist.add(new Area(rr(8, 10.5, 4, 4, 2, s)));
      fist.add(new Area(rr(13, 10, 4, 4, 2, s)));
      fist.add(new Area(rr(18, 10.5, 4, 4, 2, s)));

      g.setColor(Color.WHITE);
      g.fill(fist);
      g.setColor(Color.BLACK);
      g.setStroke(new BasicStroke((float) Math.max(1.0, 1.2 * s)));
      g.draw(fist);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static RoundRectangle2D.Double rr(
      final double theX,
      final double theY,
      final double theW,
      final double theH,
      final double theArc,
      final double theScale) {
    return new RoundRectangle2D.Double(
        theX * theScale,
        theY * theScale,
        theW * theScale,
        theH * theScale,
        theArc * theScale,
        theArc * theScale);
  }

  /** Convenience for the playground reference panel: returns the raw PNG image. */
  static Image previewImage(final String theBaseName, final boolean theDark) {
    return loadPng(theBaseName + "-" + (theDark ? "dark" : "light") + "-32.png");
  }
}
