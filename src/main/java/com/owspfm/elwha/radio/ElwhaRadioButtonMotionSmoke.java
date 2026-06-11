package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for the S3 {@link ElwhaRadioButton} motion (story #419). True tweening needs a
 * displayable component (animation is gated on {@code isDisplayable()}, impossible headless — the
 * motion demo covers the live read), so this guard pins what headless can: the snap path renders
 * final states instantly in both directions and through select/deselect/select sequences, the
 * reduced-motion flag leaves the same end states, the {@code EMPHASIZED_DECELERATE} curve is the
 * monotonic decelerate M3 specs (fast start — &gt;60% done at t=0.25), and the motion constants
 * hold their research-§Mo values (300/50ms).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonMotionSmoke {

  private static final int SIZE = ElwhaRadioButton.STATE_LAYER_SIZE_PX;
  private static final int CX = SIZE / 2;

  private ElwhaRadioButtonMotionSmoke() {}

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

    check("DOT_GROW_MS is motion.duration.medium2 (300)", ElwhaRadioButton.DOT_GROW_MS == 300);
    check("COLOR_FADE_MS is material-web's 50", ElwhaRadioButton.COLOR_FADE_MS == 50);
    checkEasingShape();
    checkSnapRenders(false);
    checkSnapRenders(true);

    System.out.println("ElwhaRadioButtonMotionSmoke: OK (snap paths + curve + constants)");
  }

  /** EMPHASIZED_DECELERATE: monotonic, endpoint-exact, decelerating (fast early progress). */
  private static void checkEasingShape() {
    final Easing curve = Easing.EMPHASIZED_DECELERATE;
    check("curve starts at 0", Math.abs(curve.ease(0f)) < 0.001f);
    check("curve ends at 1", Math.abs(curve.ease(1f) - 1f) < 0.001f);
    boolean monotonic = true;
    float prev = 0f;
    for (int i = 1; i <= 20; i++) {
      final float v = curve.ease(i / 20f);
      monotonic &= v >= prev - 0.0001f;
      prev = v;
    }
    check("curve is monotonic", monotonic);
    check("curve decelerates (>60% done at t=0.25)", curve.ease(0.25f) > 0.6f);
  }

  private static void checkSnapRenders(final boolean reducedMotion) {
    final boolean before = MorphAnimator.isReducedMotion();
    MorphAnimator.setReducedMotion(reducedMotion);
    try {
      final String tag = reducedMotion ? " [reduced motion]" : " [undisplayable snap]";
      final Color surface = ColorRole.SURFACE.resolve();
      final Color primary = ColorRole.PRIMARY.resolve();
      final Color restRing = ColorRole.ON_SURFACE_VARIANT.resolve();

      final ElwhaRadioButton radio = new ElwhaRadioButton();
      radio.setSize(SIZE, SIZE);

      radio.setSelected(true);
      BufferedImage img = render(radio, surface);
      check("select snaps to full PRIMARY dot" + tag, near(img.getRGB(CX, CX), primary));
      check("select snaps ring to PRIMARY" + tag, near(img.getRGB(13, 13), primary));

      radio.setSelected(false);
      img = render(radio, surface);
      check("deselect snaps the dot away" + tag, near(img.getRGB(CX, CX), surface));
      check("deselect snaps ring back" + tag, near(img.getRGB(13, 13), restRing));

      radio.setSelected(true);
      radio.setSelected(false);
      radio.setSelected(true);
      img = render(radio, surface);
      check("rapid flip sequence lands selected" + tag, near(img.getRGB(CX, CX), primary));
    } finally {
      MorphAnimator.setReducedMotion(before);
    }
  }

  private static BufferedImage render(final ElwhaRadioButton radio, final Color ground) {
    final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(ground);
      g.fillRect(0, 0, SIZE, SIZE);
      radio.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean near(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    return c.getAlpha() == 255
        && Math.abs(c.getRed() - target.getRed()) <= 8
        && Math.abs(c.getGreen() - target.getGreen()) <= 8
        && Math.abs(c.getBlue() - target.getBlue()) <= 8;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
