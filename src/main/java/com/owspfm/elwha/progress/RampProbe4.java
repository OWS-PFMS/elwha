package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Throwaway diagnostic 4 — windowed centroid profile of wavy@97. */
public final class RampProbe4 {
  private RampProbe4() {}

  /**
   * Dumps the profile.
   *
   * @param args unused
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    final ElwhaCircularProgressIndicator high = ElwhaCircularProgressIndicator.wavy();
    high.setValue(97);
    final int d = high.getDiameter();
    high.setSize(d, d);
    final BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    high.paint(g);
    g.dispose();
    final Color primary = ColorRole.PRIMARY.resolve();
    final float c = d / 2f;
    float min = Float.MAX_VALUE;
    float max = -1f;
    for (int a = 135; a <= 405; a += 2) {
      final int idx = ((a % 360) + 360) % 360;
      final double rad = Math.toRadians(idx);
      float outer = 0f;
      float inner = 0f;
      for (float r = c; r >= 2f; r -= 0.5f) {
        final int x = Math.round(c + (float) (r * Math.cos(rad)));
        final int y = Math.round(c - (float) (r * Math.sin(rad)));
        if (x < 0 || y < 0 || x >= d || y >= d) continue;
        final Color px = new Color(img.getRGB(x, y), true);
        final boolean hit =
            px.getAlpha() >= 200
                && Math.abs(px.getRed() - primary.getRed())
                        + Math.abs(px.getGreen() - primary.getGreen())
                        + Math.abs(px.getBlue() - primary.getBlue())
                    < 60;
        if (hit) {
          if (outer == 0f) outer = r;
          inner = r;
        } else if (outer > 0f) {
          break;
        }
      }
      final float mid = outer == 0f ? 0f : (outer + inner) / 2f;
      if (mid > 0f) {
        if (mid < min) min = mid;
        if (mid > max) max = mid;
        if (mid <= 19.5f || mid >= 21.25f) System.out.println("outlier " + idx + " mid=" + mid + " o=" + outer + " i=" + inner);
      }
    }
    System.out.println("min=" + min + " max=" + max + " spread=" + (max - min));
  }
}
