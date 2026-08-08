package com.owspfm.elwha.testkit;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.RepaintManager;

/**
 * Counts what a component schedules from <em>inside</em> its own paint pass — the technique banked
 * from the #305 repaint-storm investigation, wrapped so several suites can reuse it.
 *
 * <p>A paint is supposed to be a read-only pass over already-settled state. Anything a component
 * schedules while painting — a {@code revalidate()} that re-enters the invalidation queue, a {@code
 * repaint()} raised by mutating a child's bounds — buys an extra layout/paint cycle on top of
 * whatever already drove this one, and at 60 fps that compounds. Installing a counting {@link
 * RepaintManager} around the paint turns "does this paint have side effects" into a number.
 *
 * <p>The counters increment before delegating to the real manager, so they record the <em>call</em>
 * even where the manager itself would discard it (an unrealized component tree has no peer, so the
 * default manager drops the request — but the component still made it, which is the contract under
 * test).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class PaintPass {

  private final int revalidations;
  private final int repaints;

  private PaintPass(final int revalidations, final int repaints) {
    this.revalidations = revalidations;
    this.repaints = repaints;
  }

  /**
   * Paints {@code component} offscreen at the given size and reports what it scheduled while
   * painting. The component is sized and laid out first, so nothing the setup itself schedules is
   * counted.
   *
   * @param component the component to paint
   * @param width raster width in px
   * @param height raster height in px
   * @return the counts recorded during the paint
   * @version v0.5.0
   * @since v0.5.0
   */
  public static PaintPass capture(final JComponent component, final int width, final int height) {
    component.setSize(width, height);
    component.doLayout();
    final RepaintManager previous = RepaintManager.currentManager(component);
    final Counting counting = new Counting();
    RepaintManager.setCurrentManager(counting);
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      counting.armed = true;
      component.paint(g);
    } finally {
      counting.armed = false;
      g.dispose();
      RepaintManager.setCurrentManager(previous);
    }
    return new PaintPass(counting.revalidations, counting.repaints);
  }

  /**
   * Paints {@code component} offscreen at its preferred size.
   *
   * @param component the component to paint
   * @return the counts recorded during the paint
   * @version v0.5.0
   * @since v0.5.0
   */
  public static PaintPass capture(final JComponent component) {
    return capture(
        component, component.getPreferredSize().width, component.getPreferredSize().height);
  }

  /**
   * Runs the same counters around an arbitrary action instead of a paint — the other half of the
   * contract, for pinning where a scheduled relayout is supposed to come from once it has been
   * moved out of the paint pass.
   *
   * @param component any component in the tree whose repaint manager should be instrumented
   * @param action the action to run under the counters
   * @return the counts recorded while the action ran
   * @version v0.5.0
   * @since v0.5.0
   */
  public static PaintPass captureDuring(final JComponent component, final Runnable action) {
    final RepaintManager previous = RepaintManager.currentManager(component);
    final Counting counting = new Counting();
    RepaintManager.setCurrentManager(counting);
    try {
      counting.armed = true;
      action.run();
    } finally {
      counting.armed = false;
      RepaintManager.setCurrentManager(previous);
    }
    return new PaintPass(counting.revalidations, counting.repaints);
  }

  /**
   * How many times something called {@code revalidate()} during the paint.
   *
   * @return the invalidation count; zero for a side-effect-free paint
   * @version v0.5.0
   * @since v0.5.0
   */
  public int revalidations() {
    return revalidations;
  }

  /**
   * How many dirty regions were scheduled during the paint.
   *
   * @return the repaint-request count; zero for a side-effect-free paint
   * @version v0.5.0
   * @since v0.5.0
   */
  public int repaints() {
    return repaints;
  }

  private static final class Counting extends RepaintManager {
    private volatile boolean armed;
    private int revalidations;
    private int repaints;

    @Override
    public synchronized void addInvalidComponent(final JComponent component) {
      if (armed) {
        revalidations++;
      }
      super.addInvalidComponent(component);
    }

    @Override
    public void addDirtyRegion(
        final JComponent component, final int x, final int y, final int w, final int h) {
      if (armed) {
        repaints++;
      }
      super.addDirtyRegion(component, x, y, w, h);
    }
  }
}
