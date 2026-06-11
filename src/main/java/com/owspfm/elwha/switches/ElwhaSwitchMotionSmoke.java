package com.owspfm.elwha.switches;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Headless S3 guard (story #404) — asserts the design §6 motion contract where it is decidable
 * without a display: a non-displayable {@code setSelected} snaps (no half-slid first paint), a
 * mid-drag scrub owns the handle position and previews the color crossfade (track = 50/50 {@code
 * scH}/{@code PRIMARY} blend, outline ring half-faded), the drag release lands the handle at its
 * rest point, reduced motion snaps globally, the gallery {@code setPressed} hook applies the 28px
 * diameter immediately, and the overshoot bezier's excursion stays inside the track (design §12-2).
 * The tween <em>continuity</em> (overshoot settle, no-jump handoff) is timer-driven and verified
 * visually in {@code ElwhaSwitchMotionDemo}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchMotionSmoke {

  private static final int W = 60;
  private static final int H = 40;
  private static final int CY = 20;

  private ElwhaSwitchMotionSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkNonDisplayableSnap();
    checkDragScrubAndCrossfade();
    checkReducedMotionSnap();
    checkGalleryPressSnap();
    checkOvershootContainment();

    System.out.println(
        "ElwhaSwitchMotionSmoke: OK (snap paths + drag scrub/crossfade + reduced motion + gallery"
            + " press + overshoot containment)");
  }

  private static void checkNonDisplayableSnap() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    s.setSelected(true);
    check("non-displayable setSelected lands the handle immediately", s.handleCenterX() == 40);
    final BufferedImage img = render(s);
    check(
        "first paint after the snap is the full selected chrome",
        near(img, 40, CY, ColorRole.ON_PRIMARY.resolve())
            && near(img, 10, CY, ColorRole.PRIMARY.resolve()));
  }

  private static void checkDragScrubAndCrossfade() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    press(s, 20, CY);
    drag(s, 30, CY);
    check("mid-drag the handle rides the pointer", s.handleCenterX() == 30);

    final BufferedImage img = render(s);
    final Color halfTrack =
        mix(ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(), ColorRole.PRIMARY.resolve(), 0.5f);
    final Color halfRing = mix(ColorRole.OUTLINE.resolve(), ColorRole.PRIMARY.resolve(), 0.5f);
    check(
        "mid-drag the outline ring is half-faded toward PRIMARY (outside the halo)",
        near(img, 5, CY, halfRing));
    check("mid-drag scrub previews the 50/50 color crossfade", nearIgnoringLayers(img, halfTrack));

    release(s, 30, CY);
    check("the half-way release commits selected", s.isSelected());
    check("after release the handle lands at the selected rest point", s.handleCenterX() == 40);
  }

  private static void checkReducedMotionSnap() {
    MorphAnimator.setReducedMotion(true);
    try {
      final ElwhaSwitch s = sized(new ElwhaSwitch(true));
      s.setSelected(false);
      check("reduced motion snaps the slide", s.handleCenterX() == 20);
    } finally {
      MorphAnimator.setReducedMotion(false);
    }
  }

  private static void checkGalleryPressSnap() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final BufferedImage idle = render(s);
    check(
        "idle unselected: x=9 is track interior",
        near(idle, 9, CY, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));
    s.setPressed(true);
    final BufferedImage grown = render(s);
    check(
        "gallery setPressed applies the 28px handle immediately",
        near(grown, 9, CY, ColorRole.ON_SURFACE_VARIANT.resolve()));
  }

  private static void checkOvershootContainment() {
    final Easing overshoot = Easing.cubicBezier(0.175f, 0.885f, 0.32f, 1.275f);
    float max = 0f;
    for (int i = 0; i <= 1000; i++) {
      max = Math.max(max, overshoot.ease(i / 1000f));
    }
    check("the overshoot bezier does overshoot (the point of the curve)", max > 1.0f);
    // Excursion past the rest point in px over the 20px travel; the selected handle (r=12) has
    // 8px between its rest edge (x=48) and the track edge (x=56) — design §12-2.
    final float excursionPx = (max - 1f) * 20f;
    check("the overshoot excursion stays inside the track", excursionPx < 8f);
  }

  private static ElwhaSwitch sized(final ElwhaSwitch s) {
    s.setSize(W, H);
    return s;
  }

  private static void press(final ElwhaSwitch s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_PRESSED, x, y);
  }

  private static void drag(final ElwhaSwitch s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_DRAGGED, x, y);
  }

  private static void release(final ElwhaSwitch s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_RELEASED, x, y);
  }

  private static void dispatch(final ElwhaSwitch s, final int id, final int x, final int y) {
    final int button = MouseEvent.BUTTON1;
    final int modifiers = (id == MouseEvent.MOUSE_RELEASED) ? 0 : MouseEvent.BUTTON1_DOWN_MASK;
    // Explicit-abs-coords ctor: the (x,y)-only ctor calls getLocationOnScreen, which NPEs on an
    // unrealized (peerless) component in headless mode.
    final MouseEvent e =
        new MouseEvent(s, id, System.nanoTime(), modifiers, x, y, x, y, 1, false, button);
    for (final var l : s.getMouseListeners()) {
      switch (id) {
        case MouseEvent.MOUSE_PRESSED -> l.mousePressed(e);
        case MouseEvent.MOUSE_RELEASED -> l.mouseReleased(e);
        default -> {
          // handled by motion listeners below
        }
      }
    }
    if (id == MouseEvent.MOUSE_DRAGGED) {
      for (final var l : s.getMouseMotionListeners()) {
        l.mouseDragged(e);
      }
    }
  }

  /**
   * The mid-drag track probe sits inside the pressed state layer, so the expected color is the
   * crossfaded track with up to one 10% layer over it — accept the blend with a widened tolerance
   * instead of replicating the exact layer stack.
   */
  private static boolean nearIgnoringLayers(final BufferedImage img, final Color want) {
    final Color got = new Color(img.getRGB(47, CY), true);
    return Math.abs(got.getRed() - want.getRed()) <= 30
        && Math.abs(got.getGreen() - want.getGreen()) <= 30
        && Math.abs(got.getBlue() - want.getBlue()) <= 30;
  }

  private static BufferedImage render(final ElwhaSwitch s) {
    final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(ColorRole.SURFACE.resolve());
      g.fillRect(0, 0, W, H);
      s.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean near(final BufferedImage img, final int x, final int y, final Color want) {
    final Color got = new Color(img.getRGB(x, y), true);
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
  }

  /** Source-over blend of {@code over} at {@code alpha} onto an opaque {@code base}. */
  private static Color mix(final Color base, final Color over, final float alpha) {
    return new Color(
        Math.round(base.getRed() + (over.getRed() - base.getRed()) * alpha),
        Math.round(base.getGreen() + (over.getGreen() - base.getGreen()) * alpha),
        Math.round(base.getBlue() + (over.getBlue() - base.getBlue()) * alpha));
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
