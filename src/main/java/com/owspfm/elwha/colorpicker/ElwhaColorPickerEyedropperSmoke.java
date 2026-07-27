package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleContext;

/**
 * V2 S4 headless guard for the eyedropper (#500): the opt-in gate (default off), the headless
 * degradation rule ({@code ScreenSampler.isSupported()} is {@code false} here, so the header
 * affordance stays hidden even when enabled), and the sampler's pure seams — capture sampling with
 * bounds clamping and the loupe's quadrant-flip placement math.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerEyedropperSmoke {

  private ElwhaColorPickerEyedropperSmoke() {}

  /**
   * Runs the guard.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkGate();
    checkSampling();
    checkLoupePlacement();

    System.out.println(
        "ElwhaColorPickerEyedropperSmoke: OK (opt-in gate, headless degradation, sampling clamp,"
            + " loupe quadrants)");
  }

  private static void checkGate() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    check("eyedropper is opt-in (default off)", !picker.isEyedropperEnabled());

    picker.setEyedropperEnabled(true);
    check("setter reflects", picker.isEyedropperEnabled());
    check("headless environment reports unsupported", !ScreenSampler.isSupported());
    layoutTree(picker, new Dimension(360, 460));
    final Component button = findByAccessibleName(picker, "Pick color from screen");
    check("affordance exists in the header", button != null);
    check("affordance stays hidden when unsupported", !button.isVisible());

    picker.setEyedropperEnabled(false);
    check("setter toggles back", !picker.isEyedropperEnabled());
  }

  private static void checkSampling() {
    final BufferedImage capture = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
    capture.setRGB(5, 5, 0xFF112233);
    capture.setRGB(0, 0, 0xFFAABBCC);
    capture.setRGB(19, 9, 0xFF445566);

    check(
        "samples the exact pixel",
        new Color(0x112233).equals(ScreenSampler.colorAt(capture, 5, 5)));
    check(
        "clamps negative coordinates",
        new Color(0xAABBCC).equals(ScreenSampler.colorAt(capture, -3, -7)));
    check(
        "clamps past the far edge",
        new Color(0x445566).equals(ScreenSampler.colorAt(capture, 500, 500)));
  }

  private static void checkLoupePlacement() {
    final Dimension screen = new Dimension(1000, 800);
    final int side = ScreenSampler.LOUPE_GRID * ScreenSampler.LOUPE_SCALE;

    final Rectangle nearOrigin = ScreenSampler.loupePlacement(screen, new Point(100, 100));
    check(
        "default quadrant sits below-trailing of the pointer",
        nearOrigin.x > 100 && nearOrigin.y > 100);
    check("loupe box is grid × scale", nearOrigin.width == side);

    final Rectangle nearRight = ScreenSampler.loupePlacement(screen, new Point(990, 100));
    check("flips leading near the trailing edge", nearRight.x + side <= 990);

    final Rectangle nearBottom = ScreenSampler.loupePlacement(screen, new Point(100, 790));
    check("flips above near the bottom edge", nearBottom.y + nearBottom.height <= 790);

    final Rectangle corner = ScreenSampler.loupePlacement(screen, new Point(995, 795));
    check(
        "corner flips both axes and stays on-screen",
        corner.x >= 0
            && corner.y >= 0
            && corner.x + corner.width <= screen.width
            && corner.y + corner.height <= screen.height);
  }

  private static Component findByAccessibleName(final Component root, final String name) {
    final AccessibleContext context = root.getAccessibleContext();
    if (context != null && name.equals(context.getAccessibleName())) {
      return root;
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        final Component found = findByAccessibleName(child, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void layoutTree(final Component component, final Dimension size) {
    component.setSize(size);
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutTree(child, child.getSize());
      }
    }
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
