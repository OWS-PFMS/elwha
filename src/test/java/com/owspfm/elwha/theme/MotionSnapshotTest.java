package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/**
 * Tier A coverage of the entrance-motion snapshot cache (#713) — the three copies of this that
 * {@code ElwhaDialog}, {@code ElwhaFullScreenDialog} and the side sheet's slide surface used to
 * carry, keyed on {@code (deviceWidth, deviceHeight)} and nothing else.
 *
 * <p>The gap those copies had is the second test below. Because the hosts declare themselves
 * painting origins for the length of the tween, every descendant repaint is routed back through the
 * host's {@code paint} — which found a same-sized raster and re-blitted the stale one, so content
 * that changed during the ~18-frame entrance rendered frozen. Two mechanisms each correct alone.
 *
 * <p>Tested through the helper rather than through a live dialog because the whole contract is
 * here: <em>when does the render callback run</em>. Counting invocations of that callback states
 * the caching and the invalidation directly, where a pixel comparison on a real overlay would have
 * to infer both from a raster.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/713">#713</a>
 */
class MotionSnapshotTest {

  private static final int WIDTH = 40;
  private static final int HEIGHT = 30;

  /** A render callback that counts its calls and paints whatever colour it was last given. */
  private static final class CountingRender implements MotionSnapshot.Render {
    private int calls;
    private Color fill = Color.RED;

    @Override
    public void paint(final Graphics g) {
      calls++;
      g.setColor(fill);
      g.fillRect(0, 0, WIDTH, HEIGHT);
    }
  }

  /** A frame graphics at the given device scale, standing in for the host's own {@code paint} g. */
  private static Graphics2D frameAt(final double scale) {
    final BufferedImage target =
        new BufferedImage(
            (int) (WIDTH * scale), (int) (HEIGHT * scale), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = target.createGraphics();
    g.scale(scale, scale);
    return g;
  }

  @Test
  void oneRenderServesEveryFrameOfATween() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();
    final Graphics2D frame = frameAt(1.0);

    final BufferedImage first = snapshot.rasterFor(frame, WIDTH, HEIGHT, render);
    for (int tick = 0; tick < 17; tick++) {
      assertThat(snapshot.rasterFor(frame, WIDTH, HEIGHT, render))
          .as("every later frame of the tween blits the same raster")
          .isSameAs(first);
    }

    assertThat(render.calls).as("a full ~18-frame tween costs one render").isEqualTo(1);
  }

  /**
   * The defect. A descendant repainting mid-tween reaches the host only as a {@code
   * isPaintingOrigin()} query followed by a whole-surface {@code paint}, and without this the paint
   * re-served the raster rendered before the descendant changed.
   */
  @Test
  void aDescendantRepaintDuringTheTweenInvalidatesTheRaster() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();
    final Graphics2D frame = frameAt(1.0);
    snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    // Swing asks the host this on its way to redirecting a descendant's repaint upward.
    assertThat(snapshot.paintingOrigin(true))
        .as("mid-tween the host still answers that it is a painting origin")
        .isTrue();
    render.fill = Color.BLUE;
    final BufferedImage refreshed = snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    assertThat(render.calls)
        .as("the redirected repaint re-renders rather than re-blitting")
        .isEqualTo(2);
    assertThat(new Color(refreshed.getRGB(WIDTH / 2, HEIGHT / 2)))
        .as("and the frame carries what the descendant changed to")
        .isEqualTo(Color.BLUE);
  }

  /**
   * The other half of the same call: at the steady state the host is not a painting origin, so
   * being asked must not invalidate anything. Without this the guard could be "always stale", which
   * would satisfy the invalidation test while destroying the cache.
   */
  @Test
  void beingAskedAtTheSteadyStateInvalidatesNothing() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();
    final Graphics2D frame = frameAt(1.0);
    snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    assertThat(snapshot.paintingOrigin(false))
        .as("past full progress the host transforms nothing and is not an origin")
        .isFalse();
    snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    assertThat(render.calls).as("so the cached raster still serves").isEqualTo(1);
  }

  @Test
  void aResizeDuringTheTweenReRendersAtTheNewSize() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();

    snapshot.rasterFor(frameAt(1.0), WIDTH, HEIGHT, render);
    final BufferedImage wider = snapshot.rasterFor(frameAt(1.0), WIDTH + 20, HEIGHT, render);

    assertThat(render.calls).as("a size change cannot reuse the raster").isEqualTo(2);
    assertThat(wider.getWidth()).isEqualTo(WIDTH + 20);
  }

  @Test
  void aHiDpiFrameRasterizesAtDeviceResolution() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();

    final BufferedImage raster = snapshot.rasterFor(frameAt(2.0), WIDTH, HEIGHT, render);

    assertThat(raster.getWidth())
        .as("a 2x frame rasterizes at 2x, not at logical size")
        .isEqualTo(WIDTH * 2);
    assertThat(raster.getHeight()).isEqualTo(HEIGHT * 2);
  }

  @Test
  void aScaleChangeReRendersEvenAtTheSameLogicalSize() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();

    snapshot.rasterFor(frameAt(1.0), WIDTH, HEIGHT, render);
    snapshot.rasterFor(frameAt(2.0), WIDTH, HEIGHT, render);

    assertThat(render.calls)
        .as("dragging the window to a HiDPI screen keeps the logical size but changes the raster")
        .isEqualTo(2);
  }

  @Test
  void releasingDropsTheRasterSoTheNextOpenRendersAfresh() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();
    final Graphics2D frame = frameAt(1.0);
    snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    snapshot.release();
    render.fill = Color.GREEN;
    final BufferedImage reopened = snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    assertThat(render.calls)
        .as("the steady state drops the buffer rather than holding it")
        .isEqualTo(2);
    assertThat(new Color(reopened.getRGB(1, 1)))
        .as("so the next open picks up whatever the theme and content have become")
        .isEqualTo(Color.GREEN);
  }

  /**
   * Release must also clear the stale flag. A tween that ends immediately after a descendant
   * repaint would otherwise leave the mark set, costing the first frame of the <em>next</em> open a
   * redundant render.
   */
  @Test
  void releasingClearsAPendingInvalidation() {
    final MotionSnapshot snapshot = new MotionSnapshot();
    final CountingRender render = new CountingRender();
    final Graphics2D frame = frameAt(1.0);

    snapshot.paintingOrigin(true);
    snapshot.release();
    snapshot.rasterFor(frame, WIDTH, HEIGHT, render);
    snapshot.rasterFor(frame, WIDTH, HEIGHT, render);

    assertThat(render.calls).as("the reopened tween starts with a clean cache").isEqualTo(1);
  }
}
