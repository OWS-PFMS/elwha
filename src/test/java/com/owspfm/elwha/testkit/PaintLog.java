package com.owspfm.elwha.testkit;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JComponent;

/**
 * A recording {@link Graphics2D} that captures what a component <em>asked</em> to paint — every
 * {@code drawString} with its position, font and color, and every {@code fill}/{@code draw} of a
 * shape with its bounds and color — while delegating the actual rasterization to a real graphics
 * context.
 *
 * <p>This is the sanctioned oracle for text-bearing contracts. The determinism rules forbid probing
 * pixels inside a glyph's box, and counting "ink" in a band (the idiom the {@code *Smoke} mains
 * used) is the same probe wearing a hat: it reads rasterized glyph coverage, so it is sensitive to
 * hinting and anti-aliasing and therefore to the host's font stack. A recorded {@code drawString}
 * carries the exact string, the exact baseline, and the exact resolved color, so assertions like
 * "the counter paints in the error role once over the limit" or "the floated label drops to
 * BODY_SMALL" become contract statements with no rasterization in the loop at all.
 *
 * <p>Positions and shape bounds are recorded in the <em>root component's</em> coordinate space: the
 * current {@link AffineTransform} is applied before recording, so a child painted through {@code
 * paintChildren}'s translated graphics — or a rotated painting frame such as a vertical slider's —
 * reports where its content actually lands, not where it was drawn in some local frame.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class PaintLog extends Graphics2D {

  /**
   * One recorded {@code drawString} call.
   *
   * @param string the painted string
   * @param x baseline-origin x in root-component coordinates
   * @param y baseline y in root-component coordinates
   * @param font the font in effect
   * @param color the paint in effect, or {@code null} if it was not a plain color
   * @version v0.5.0
   * @since v0.5.0
   */
  public record Text(String string, double x, double y, Font font, Color color) {}

  /**
   * One recorded shape {@code fill} or {@code draw} call.
   *
   * @param shape the painted shape in root-component coordinates
   * @param color the paint in effect, or {@code null} if it was not a plain color
   * @param stroked {@code true} for {@code draw} (outline), {@code false} for {@code fill}
   * @version v0.5.0
   * @since v0.5.0
   */
  public record Painted(Shape shape, Color color, boolean stroked) {

    /**
     * Returns the painted shape's bounding box.
     *
     * @return the bounds in root-component coordinates
     * @version v0.5.0
     * @since v0.5.0
     */
    public Rectangle2D bounds() {
      return shape.getBounds2D();
    }

    /**
     * Returns how many disjoint sub-paths the shape contains — one per {@code SEG_MOVETO}. A
     * container stroke that has been notched (an M3 outlined field's floated label, say) reports
     * more than one; an unbroken outline reports exactly one.
     *
     * @return the sub-path count
     * @version v0.5.0
     * @since v0.5.0
     */
    public int subPaths() {
      int count = 0;
      final java.awt.geom.PathIterator it = shape.getPathIterator(null);
      final double[] coords = new double[6];
      while (!it.isDone()) {
        if (it.currentSegment(coords) == java.awt.geom.PathIterator.SEG_MOVETO) {
          count++;
        }
        it.next();
      }
      return count;
    }
  }

  private final Graphics2D delegate;
  private final List<Text> texts;
  private final List<Painted> shapes;

  private PaintLog(final Graphics2D delegate, final List<Text> texts, final List<Painted> shapes) {
    this.delegate = delegate;
    this.texts = texts;
    this.shapes = shapes;
  }

  /**
   * Sizes, lays out, and paints an unrealized component through a recording context, returning the
   * log. Mirrors {@link Pixels#render} so a test can probe pixels and painted text off the same
   * geometry.
   *
   * @param component the component to paint (never realized; no peer required)
   * @param width raster width in px
   * @param height raster height in px
   * @return the recorded paint calls
   * @version v0.5.0
   * @since v0.5.0
   */
  public static PaintLog capture(final JComponent component, final int width, final int height) {
    component.setSize(width, height);
    component.doLayout();
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    final PaintLog log = new PaintLog(g, new ArrayList<>(), new ArrayList<>());
    try {
      component.paint(log);
    } finally {
      g.dispose();
    }
    return log;
  }

  /**
   * Sizes, lays out, and paints an unrealized component at its preferred size.
   *
   * @param component the component to paint
   * @return the recorded paint calls
   * @version v0.5.0
   * @since v0.5.0
   */
  public static PaintLog capture(final JComponent component) {
    return capture(
        component, component.getPreferredSize().width, component.getPreferredSize().height);
  }

  /**
   * Returns every recorded {@code drawString}, in paint order.
   *
   * @return the recorded text calls
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<Text> texts() {
    return List.copyOf(texts);
  }

  /**
   * Returns every recorded shape {@code fill} / {@code draw}, in paint order.
   *
   * @return the recorded shape calls
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<Painted> shapes() {
    return List.copyOf(shapes);
  }

  /**
   * Returns the first recorded string equal to {@code string}, if it was painted at all.
   *
   * @param string the exact string to look for
   * @return the matching record, or empty if the string was never painted
   * @version v0.5.0
   * @since v0.5.0
   */
  public Optional<Text> text(final String string) {
    return texts.stream().filter(t -> t.string().equals(string)).findFirst();
  }

  /**
   * Returns whether {@code string} was painted.
   *
   * @param string the exact string to look for
   * @return {@code true} if some {@code drawString} painted exactly that string
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean painted(final String string) {
    return text(string).isPresent();
  }

  private Color currentColor() {
    return delegate.getPaint() instanceof Color color ? color : null;
  }

  private void record(final String string, final double x, final double y) {
    final Point2D at = delegate.getTransform().transform(new Point2D.Double(x, y), null);
    texts.add(new Text(string, at.getX(), at.getY(), delegate.getFont(), currentColor()));
  }

  private void record(final Shape shape, final boolean stroked) {
    shapes.add(
        new Painted(
            delegate.getTransform().createTransformedShape(shape), currentColor(), stroked));
  }

  // ---- recording overrides --------------------------------------------------

  @Override
  public void drawString(final String str, final int x, final int y) {
    record(str, x, y);
    delegate.drawString(str, x, y);
  }

  @Override
  public void drawString(final String str, final float x, final float y) {
    record(str, x, y);
    delegate.drawString(str, x, y);
  }

  @Override
  public void fill(final Shape s) {
    record(s, false);
    delegate.fill(s);
  }

  @Override
  public void draw(final Shape s) {
    record(s, true);
    delegate.draw(s);
  }

  @Override
  public void fillRect(final int x, final int y, final int width, final int height) {
    record(new Rectangle(x, y, width, height), false);
    delegate.fillRect(x, y, width, height);
  }

  @Override
  public Graphics create() {
    return new PaintLog((Graphics2D) delegate.create(), texts, shapes);
  }

  // ---- pure delegation ------------------------------------------------------

  @Override
  public void drawString(final AttributedCharacterIterator iterator, final int x, final int y) {
    delegate.drawString(iterator, x, y);
  }

  @Override
  public void drawString(final AttributedCharacterIterator iterator, final float x, final float y) {
    delegate.drawString(iterator, x, y);
  }

  @Override
  public void drawGlyphVector(final GlyphVector g, final float x, final float y) {
    delegate.drawGlyphVector(g, x, y);
  }

  @Override
  public boolean drawImage(final Image img, final AffineTransform xform, final ImageObserver obs) {
    return delegate.drawImage(img, xform, obs);
  }

  @Override
  public void drawImage(
      final BufferedImage img, final BufferedImageOp op, final int x, final int y) {
    delegate.drawImage(img, op, x, y);
  }

  @Override
  public void drawRenderedImage(final RenderedImage img, final AffineTransform xform) {
    delegate.drawRenderedImage(img, xform);
  }

  @Override
  public void drawRenderableImage(final RenderableImage img, final AffineTransform xform) {
    delegate.drawRenderableImage(img, xform);
  }

  @Override
  public boolean hit(final Rectangle rect, final Shape s, final boolean onStroke) {
    return delegate.hit(rect, s, onStroke);
  }

  @Override
  public GraphicsConfiguration getDeviceConfiguration() {
    return delegate.getDeviceConfiguration();
  }

  @Override
  public void setComposite(final Composite comp) {
    delegate.setComposite(comp);
  }

  @Override
  public void setPaint(final Paint paint) {
    delegate.setPaint(paint);
  }

  @Override
  public void setStroke(final Stroke s) {
    delegate.setStroke(s);
  }

  @Override
  public void setRenderingHint(final RenderingHints.Key hintKey, final Object hintValue) {
    delegate.setRenderingHint(hintKey, hintValue);
  }

  @Override
  public Object getRenderingHint(final RenderingHints.Key hintKey) {
    return delegate.getRenderingHint(hintKey);
  }

  @Override
  public void setRenderingHints(final Map<?, ?> hints) {
    delegate.setRenderingHints(hints);
  }

  @Override
  public void addRenderingHints(final Map<?, ?> hints) {
    delegate.addRenderingHints(hints);
  }

  @Override
  public RenderingHints getRenderingHints() {
    return delegate.getRenderingHints();
  }

  @Override
  public void translate(final int x, final int y) {
    delegate.translate(x, y);
  }

  @Override
  public void translate(final double tx, final double ty) {
    delegate.translate(tx, ty);
  }

  @Override
  public void rotate(final double theta) {
    delegate.rotate(theta);
  }

  @Override
  public void rotate(final double theta, final double x, final double y) {
    delegate.rotate(theta, x, y);
  }

  @Override
  public void scale(final double sx, final double sy) {
    delegate.scale(sx, sy);
  }

  @Override
  public void shear(final double shx, final double shy) {
    delegate.shear(shx, shy);
  }

  @Override
  public void transform(final AffineTransform tx) {
    delegate.transform(tx);
  }

  @Override
  public void setTransform(final AffineTransform tx) {
    delegate.setTransform(tx);
  }

  @Override
  public AffineTransform getTransform() {
    return delegate.getTransform();
  }

  @Override
  public Paint getPaint() {
    return delegate.getPaint();
  }

  @Override
  public Composite getComposite() {
    return delegate.getComposite();
  }

  @Override
  public void setBackground(final Color color) {
    delegate.setBackground(color);
  }

  @Override
  public Color getBackground() {
    return delegate.getBackground();
  }

  @Override
  public Stroke getStroke() {
    return delegate.getStroke();
  }

  @Override
  public void clip(final Shape s) {
    delegate.clip(s);
  }

  @Override
  public FontRenderContext getFontRenderContext() {
    return delegate.getFontRenderContext();
  }

  @Override
  public Color getColor() {
    return delegate.getColor();
  }

  @Override
  public void setColor(final Color c) {
    delegate.setColor(c);
  }

  @Override
  public void setPaintMode() {
    delegate.setPaintMode();
  }

  @Override
  public void setXORMode(final Color c1) {
    delegate.setXORMode(c1);
  }

  @Override
  public Font getFont() {
    return delegate.getFont();
  }

  @Override
  public void setFont(final Font font) {
    delegate.setFont(font);
  }

  @Override
  public FontMetrics getFontMetrics(final Font f) {
    return delegate.getFontMetrics(f);
  }

  @Override
  public Rectangle getClipBounds() {
    return delegate.getClipBounds();
  }

  @Override
  public void clipRect(final int x, final int y, final int width, final int height) {
    delegate.clipRect(x, y, width, height);
  }

  @Override
  public void setClip(final int x, final int y, final int width, final int height) {
    delegate.setClip(x, y, width, height);
  }

  @Override
  public Shape getClip() {
    return delegate.getClip();
  }

  @Override
  public void setClip(final Shape clip) {
    delegate.setClip(clip);
  }

  @Override
  public void copyArea(
      final int x, final int y, final int width, final int height, final int dx, final int dy) {
    delegate.copyArea(x, y, width, height, dx, dy);
  }

  @Override
  public void drawLine(final int x1, final int y1, final int x2, final int y2) {
    delegate.drawLine(x1, y1, x2, y2);
  }

  @Override
  public void clearRect(final int x, final int y, final int width, final int height) {
    delegate.clearRect(x, y, width, height);
  }

  @Override
  public void drawRoundRect(
      final int x,
      final int y,
      final int width,
      final int height,
      final int arcWidth,
      final int arcHeight) {
    delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  @Override
  public void fillRoundRect(
      final int x,
      final int y,
      final int width,
      final int height,
      final int arcWidth,
      final int arcHeight) {
    delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  @Override
  public void drawOval(final int x, final int y, final int width, final int height) {
    delegate.drawOval(x, y, width, height);
  }

  @Override
  public void fillOval(final int x, final int y, final int width, final int height) {
    delegate.fillOval(x, y, width, height);
  }

  @Override
  public void drawArc(
      final int x,
      final int y,
      final int width,
      final int height,
      final int startAngle,
      final int arcAngle) {
    delegate.drawArc(x, y, width, height, startAngle, arcAngle);
  }

  @Override
  public void fillArc(
      final int x,
      final int y,
      final int width,
      final int height,
      final int startAngle,
      final int arcAngle) {
    delegate.fillArc(x, y, width, height, startAngle, arcAngle);
  }

  @Override
  public void drawPolyline(final int[] xpoints, final int[] ypoints, final int npoints) {
    delegate.drawPolyline(xpoints, ypoints, npoints);
  }

  @Override
  public void drawPolygon(final int[] xpoints, final int[] ypoints, final int npoints) {
    delegate.drawPolygon(xpoints, ypoints, npoints);
  }

  @Override
  public void fillPolygon(final int[] xpoints, final int[] ypoints, final int npoints) {
    delegate.fillPolygon(xpoints, ypoints, npoints);
  }

  @Override
  public boolean drawImage(
      final Image img, final int x, final int y, final ImageObserver observer) {
    return delegate.drawImage(img, x, y, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int x,
      final int y,
      final int width,
      final int height,
      final ImageObserver observer) {
    return delegate.drawImage(img, x, y, width, height, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int x,
      final int y,
      final Color bgcolor,
      final ImageObserver observer) {
    return delegate.drawImage(img, x, y, bgcolor, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int x,
      final int y,
      final int width,
      final int height,
      final Color bgcolor,
      final ImageObserver observer) {
    return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int dx1,
      final int dy1,
      final int dx2,
      final int dy2,
      final int sx1,
      final int sy1,
      final int sx2,
      final int sy2,
      final ImageObserver observer) {
    return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
  }

  @Override
  public boolean drawImage(
      final Image img,
      final int dx1,
      final int dy1,
      final int dx2,
      final int dy2,
      final int sx1,
      final int sy1,
      final int sx2,
      final int sy2,
      final Color bgcolor,
      final ImageObserver observer) {
    return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }
}
