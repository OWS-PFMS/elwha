package com.owspfm.elwha.list;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.UIManager;

/**
 * Lazily-built drag cursors for {@link ElwhaItemList}'s {@link ReorderAffordance#CURSOR_SWAP}
 * affordance.
 *
 * <p>AWT ships only {@link Cursor#HAND_CURSOR} (pointing finger) and {@link Cursor#MOVE_CURSOR}
 * (four-direction arrow); modern desktops distinguish "grab" — an open hand meaning this surface is
 * draggable — from "grabbing", a closed fist meaning a drag is in flight. This helper fills the gap
 * with a three-tier loading strategy:
 *
 * <ol>
 *   <li>The cursor PNGs bundled under {@code cursors/}, in a light or dark variant chosen from the
 *       active theme's panel-background luminance.
 *   <li>If no PNG can be loaded, a Java2D-painted silhouette, so the pointer is at least
 *       recognisable as a hand.
 *   <li>If even custom-cursor creation fails — some platforms reject particular sizes — {@link
 *       Cursor#MOVE_CURSOR}.
 * </ol>
 *
 * <p>Hotspots are authored against a 32&nbsp;px design grid and scaled to whatever size the toolkit
 * reports as best. A runtime theme switch invalidates the cache, so the next call rebuilds in the
 * new variant.
 *
 * <p>Relocated here from the V1 card list in story #69 (spec §7), which is what removes the last
 * code dependency the pre-#70 list families carried. The asset content itself is owned by issue
 * #531.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ReorderCursors {

  private static final int DESIGN_SIZE = 32;
  private static final int FALLBACK_SIZE = 16;

  private static Cursor grabCached;
  private static Cursor grabbingCached;
  private static Boolean cachedDark;

  // Counts cache drops so a caller can tell whether a rebuild actually happened. EDT-confined,
  // like everything else here.
  private static long generation;

  // The activation token the last drop was charged to, held weakly so remembering it cannot pin a
  // Window. See invalidateOnce.
  private static WeakReference<Object> lastActivation;

  private ReorderCursors() {}

  /**
   * Returns the open-hand "grab" cursor, which signals that a surface is draggable.
   *
   * @return the grab cursor; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  static Cursor grab() {
    if (GraphicsEnvironment.isHeadless()) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
    refreshIfThemeChanged();
    if (grabCached == null) {
      grabCached = loadCursor("grab", isDarkTheme(), new Point(15, 8), "ElwhaItemList.grab", true);
    }
    return grabCached;
  }

  /**
   * Returns the closed-fist "grabbing" cursor, which signals a drag in flight.
   *
   * @return the grabbing cursor; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  static Cursor grabbing() {
    if (GraphicsEnvironment.isHeadless()) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
    refreshIfThemeChanged();
    if (grabbingCached == null) {
      grabbingCached =
          loadCursor("grabbing", isDarkTheme(), new Point(13, 13), "ElwhaItemList.grabbing", false);
    }
    return grabbingCached;
  }

  /**
   * Drops the cached cursors so the next {@link #grab()} or {@link #grabbing()} builds fresh ones.
   *
   * <p>macOS drops the OS-side association for {@code createCustomCursor} cursors across a Spaces
   * or Mission Control transition. The cached {@link Cursor} objects survive the transition but
   * stop rendering, so re-applying the same instances changes nothing — they have to be rebuilt
   * (#556).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  static void invalidate() {
    grabCached = null;
    grabbingCached = null;
    generation++;
  }

  /**
   * Invalidates at most once for a given {@code activation} — the shared event object AWT hands to
   * every listener of one window-activation.
   *
   * <p>The cache is global while the listener that drives it is per-list, so a window holding N
   * cursor-swap lists used to run N invalidate-and-rebuild cycles per Alt-Tab, each list throwing
   * away the cursor the previous one had just decoded. Charging the drop to the activation itself
   * collapses that to one rebuild; the remaining lists read the cache the first one filled.
   *
   * <p>A {@code null} activation means "no event to charge this to" and always invalidates, which
   * is what a direct programmatic refresh wants.
   *
   * @param activation the activation event, used only for identity, or {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  static void invalidateOnce(final Object activation) {
    if (activation != null && lastActivation != null && lastActivation.get() == activation) {
      return;
    }
    lastActivation = activation == null ? null : new WeakReference<>(activation);
    invalidate();
  }

  /**
   * How many times the cache has been dropped. Package-private so the suite can assert that N lists
   * sharing a window rebuild once, not N times.
   *
   * @return a counter that advances on every cache drop
   * @version v0.5.0
   * @since v0.5.0
   */
  static long generation() {
    return generation;
  }

  private static void refreshIfThemeChanged() {
    final boolean dark = isDarkTheme();
    if (cachedDark == null || cachedDark != dark) {
      cachedDark = dark;
      invalidate();
    }
  }

  private static boolean isDarkTheme() {
    final Color panel = UIManager.getColor("Panel.background");
    if (panel == null) {
      return false;
    }
    return (panel.getRed() + panel.getGreen() + panel.getBlue()) / 3 < 128;
  }

  private static Cursor loadCursor(
      final String baseName,
      final boolean dark,
      final Point hotspot,
      final String name,
      final boolean openHand) {
    final Dimension best = bestCursorSize();
    final BufferedImage image = pickBestImage(baseName, dark, best.width, openHand);
    if (image == null) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
    final Point scaledHotspot =
        new Point(
            hotspot.x * image.getWidth() / DESIGN_SIZE,
            hotspot.y * image.getHeight() / DESIGN_SIZE);
    try {
      return Toolkit.getDefaultToolkit().createCustomCursor(image, scaledHotspot, name);
    } catch (final Exception | Error ignore) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
  }

  private static Dimension bestCursorSize() {
    try {
      final Dimension best =
          Toolkit.getDefaultToolkit().getBestCursorSize(DESIGN_SIZE, DESIGN_SIZE);
      if (best.width > 0 && best.height > 0) {
        return best;
      }
    } catch (final Exception | Error ignore) {
      // Toolkits without a display, and some remote ones, refuse to answer at all.
    }
    return new Dimension(DESIGN_SIZE, DESIGN_SIZE);
  }

  private static BufferedImage pickBestImage(
      final String baseName, final boolean dark, final int requestedSize, final boolean openHand) {
    final String themeKey = dark ? "dark" : "light";
    final int[] sizes = requestedSize >= 24 ? new int[] {32, 16} : new int[] {16, 32};
    for (final int size : sizes) {
      final BufferedImage image = loadPng(baseName + "-" + themeKey + "-" + size + ".png");
      if (image != null) {
        return scaleIfNeeded(image, requestedSize);
      }
    }
    return paintFallback(openHand, requestedSize);
  }

  private static BufferedImage loadPng(final String fileName) {
    final URL url = ReorderCursors.class.getResource("cursors/" + fileName);
    if (url == null) {
      return null;
    }
    try (InputStream in = url.openStream()) {
      return ImageIO.read(in);
    } catch (final IOException ignore) {
      return null;
    }
  }

  private static BufferedImage scaleIfNeeded(final BufferedImage source, final int targetSize) {
    if (source.getWidth() == targetSize && source.getHeight() == targetSize) {
      return source;
    }
    final BufferedImage out =
        new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = out.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.drawImage(source, 0, 0, targetSize, targetSize, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  // ----------------------------------------------------------- fallback paint

  private static BufferedImage paintFallback(final boolean openHand, final int size) {
    final int actualSize = size > 0 ? size : FALLBACK_SIZE;
    return openHand ? paintOpenHand(actualSize) : paintClosedFist(actualSize);
  }

  private static BufferedImage paintOpenHand(final int size) {
    final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final double scale = size / (double) DESIGN_SIZE;

      final Area hand = new Area();
      hand.add(new Area(rr(8, 16, 16, 14, 5, scale)));
      hand.add(new Area(rr(8.5, 6, 3, 12, 1.5, scale)));
      hand.add(new Area(rr(12, 4, 3, 14, 1.5, scale)));
      hand.add(new Area(rr(15.5, 4, 3, 14, 1.5, scale)));
      hand.add(new Area(rr(19, 6, 3, 12, 1.5, scale)));
      hand.add(new Area(rr(3.5, 18, 7, 5, 2.5, scale)));

      strokeSilhouette(g, hand, scale);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static BufferedImage paintClosedFist(final int size) {
    final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final double scale = size / (double) DESIGN_SIZE;

      final Area fist = new Area();
      fist.add(new Area(rr(7, 13, 18, 15, 6, scale)));
      fist.add(new Area(rr(4, 16, 6, 7, 3, scale)));
      fist.add(new Area(rr(8, 10.5, 4, 4, 2, scale)));
      fist.add(new Area(rr(13, 10, 4, 4, 2, scale)));
      fist.add(new Area(rr(18, 10.5, 4, 4, 2, scale)));

      strokeSilhouette(g, fist, scale);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static void strokeSilhouette(final Graphics2D g, final Area shape, final double scale) {
    g.setColor(Color.WHITE);
    g.fill(shape);
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke((float) Math.max(1.0, 1.2 * scale)));
    g.draw(shape);
  }

  private static RoundRectangle2D.Double rr(
      final double x,
      final double y,
      final double width,
      final double height,
      final double arc,
      final double scale) {
    return new RoundRectangle2D.Double(
        x * scale, y * scale, width * scale, height * scale, arc * scale, arc * scale);
  }
}
