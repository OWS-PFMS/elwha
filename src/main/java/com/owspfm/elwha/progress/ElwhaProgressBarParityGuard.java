package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleValue;
import javax.swing.JProgressBar;

/**
 * Headless S6 parity guard (story #474) — pins the progress indicators' accessibility contract
 * against {@link JProgressBar}'s: same {@code PROGRESS_BAR} role, same model-backed current/min/max
 * values (extent-aware maximum), same writable {@code AccessibleValue}, and not focusable.
 * Documents the one intentional divergence: while indeterminate, Elwha withholds the current value
 * ({@code null}, plus the BUSY state) where {@code JProgressBar} keeps reporting a stale number —
 * the M3/Compose semantics. Also re-verifies RTL mirroring for the wavy and indeterminate linear
 * paints that the S1 chrome smoke doesn't cover. Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaProgressBarParityGuard {

  private static final int W = 240;

  private ElwhaProgressBarParityGuard() {}

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

    final JProgressBar reference = new JProgressBar(0, 100);
    reference.setValue(60);

    for (final AbstractElwhaProgressIndicator indicator :
        new AbstractElwhaProgressIndicator[] {
          new ElwhaLinearProgressIndicator(0, 100, 60),
          new ElwhaCircularProgressIndicator(0, 100, 60)
        }) {
      final String kind = indicator.getClass().getSimpleName();
      check(
          kind + ": role matches JProgressBar",
          indicator.getAccessibleContext().getAccessibleRole() == AccessibleRole.PROGRESS_BAR
              && reference.getAccessibleContext().getAccessibleRole()
                  == AccessibleRole.PROGRESS_BAR);
      final AccessibleValue value = indicator.getAccessibleContext().getAccessibleValue();
      final AccessibleValue referenceValue = reference.getAccessibleContext().getAccessibleValue();
      check(
          kind + ": current value matches",
          value.getCurrentAccessibleValue().intValue()
              == referenceValue.getCurrentAccessibleValue().intValue());
      check(
          kind + ": minimum matches",
          value.getMinimumAccessibleValue().intValue()
              == referenceValue.getMinimumAccessibleValue().intValue());
      check(
          kind + ": maximum matches (extent-aware)",
          value.getMaximumAccessibleValue().intValue()
              == referenceValue.getMaximumAccessibleValue().intValue());
      check(kind + ": AccessibleValue writes back", value.setCurrentAccessibleValue(25));
      check(kind + ": write landed in the model", indicator.getValue() == 25);
      check(kind + ": rejects a null write", !value.setCurrentAccessibleValue(null));
      check(
          kind + ": not focusable (explicit opt-out; JProgressBar keeps the default flag)",
          !indicator.isFocusable());

      indicator.setIndeterminate(true);
      check(
          kind + ": indeterminate withholds the value (M3 semantics; JProgressBar diverges)",
          value.getCurrentAccessibleValue() == null);
      check(
          kind + ": indeterminate reports BUSY",
          indicator.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.BUSY));
      indicator.setIndeterminate(false);
      check(
          kind + ": value returns after the round-trip",
          value.getCurrentAccessibleValue().intValue() == 25);
    }

    final Color primary = ColorRole.PRIMARY.resolve();
    final ElwhaLinearProgressIndicator rtlWavy = ElwhaLinearProgressIndicator.wavy();
    rtlWavy.setValue(60);
    rtlWavy.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    final BufferedImage wavyImg = paint(rtlWavy, 10);
    check("RTL wavy: fill on the visual right", columnHas(wavyImg, W - 12, primary));
    check("RTL wavy: stop dot at the visual left", columnHas(wavyImg, 2, primary));

    final ElwhaLinearProgressIndicator rtlIndet = ElwhaLinearProgressIndicator.indeterminate();
    rtlIndet.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    boolean lineAppears = false;
    for (int i = 0; i < 10 && !lineAppears; i++) {
      final BufferedImage frame = paint(rtlIndet, 4);
      for (int x = 0; x < W && !lineAppears; x += 2) {
        lineAppears = nearColor(frame.getRGB(x, 2), primary);
      }
      sleep(80);
    }
    check("RTL indeterminate: lines render under RTL", lineAppears);

    System.out.println("ElwhaProgressBarParityGuard: OK (role/value parity + RTL coverage)");
  }

  private static BufferedImage paint(final ElwhaLinearProgressIndicator bar, final int height) {
    bar.setSize(W, height);
    final BufferedImage img = new BufferedImage(W, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean columnHas(final BufferedImage img, final int x, final Color target) {
    for (int y = 0; y < img.getHeight(); y++) {
      if (nearColor(img.getRGB(x, y), target)) {
        return true;
      }
    }
    return false;
  }

  private static void sleep(final long ms) {
    try {
      Thread.sleep(ms);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 200) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed())
            + Math.abs(c.getGreen() - target.getGreen())
            + Math.abs(c.getBlue() - target.getBlue())
        < 60;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
