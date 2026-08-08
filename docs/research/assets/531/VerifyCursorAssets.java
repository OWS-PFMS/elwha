import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Checks the #531 candidate cursor PNGs the way ReorderCursors actually consumes them.
 *
 * <p>The renderer is Python, so this is the one step that proves the output is usable
 * from Java: ImageIO decodes it, the alpha channel survived, the hotspot lands inside
 * the image after ReorderCursors' integer scaling, and — when a display is present —
 * Toolkit accepts it as a custom cursor at the size the toolkit asks for.
 *
 * <pre>
 *   java docs/research/assets/531/VerifyCursorAssets.java docs/research/assets/531
 * </pre>
 *
 * <p>Not part of the build: it lives under {@code docs/}, outside the source roots
 * Spotless and Checkstyle scan, and nothing ships from this branch.
 */
public final class VerifyCursorAssets {

  private static final int DESIGN_SIZE = 32;

  /** Hotspot every candidate uses, in design-grid units. */
  private static final Point HOTSPOT = new Point(16, 14);

  private static final String[] STATES = {"grab", "grabbing"};
  private static final String[] THEMES = {"light", "dark"};
  private static final int[] SIZES = {32, 16};

  private VerifyCursorAssets() {}

  public static void main(final String[] args) throws IOException {
    final File root = new File(args.length > 0 ? args[0] : ".", "candidates");
    final File[] candidates = root.listFiles(File::isDirectory);
    if (candidates == null || candidates.length == 0) {
      System.out.println("no candidate directories under " + root);
      System.exit(1);
    }
    java.util.Arrays.sort(candidates);

    final List<String> failures = new ArrayList<>();
    int checked = 0;
    final boolean headless = GraphicsEnvironment.isHeadless();
    System.out.println(headless ? "headless: skipping createCustomCursor" : "display available");

    for (final File candidate : candidates) {
      for (final String state : STATES) {
        for (final String theme : THEMES) {
          for (final int size : SIZES) {
            final String name = candidate.getName() + "/" + state + "-" + theme + "-" + size;
            final File file = new File(candidate, state + "-" + theme + "-" + size + ".png");
            final BufferedImage image = ImageIO.read(file);
            checked++;
            if (image == null) {
              failures.add(name + ": ImageIO returned null");
              continue;
            }
            if (image.getWidth() != size || image.getHeight() != size) {
              failures.add(name + ": " + image.getWidth() + "x" + image.getHeight()
                  + ", expected " + size + " square");
              continue;
            }
            if (!image.getColorModel().hasAlpha()) {
              failures.add(name + ": no alpha channel — the cursor would paint a box");
              continue;
            }

            // Exactly the arithmetic loadCursor performs before handing off to Toolkit.
            final Point scaled = new Point(
                HOTSPOT.x * image.getWidth() / DESIGN_SIZE,
                HOTSPOT.y * image.getHeight() / DESIGN_SIZE);
            if (scaled.x < 0 || scaled.x >= size || scaled.y < 0 || scaled.y >= size) {
              failures.add(name + ": scaled hotspot " + scaled.x + "," + scaled.y
                  + " falls outside a " + size + "px image");
              continue;
            }

            int opaque = 0;
            for (int y = 0; y < size; y++) {
              for (int x = 0; x < size; x++) {
                if ((image.getRGB(x, y) >>> 24) == 0xFF) {
                  opaque++;
                }
              }
            }
            if (opaque == 0) {
              failures.add(name + ": nothing fully opaque — artwork is missing or all halo");
              continue;
            }

            if (!headless) {
              try {
                final Cursor cursor =
                    Toolkit.getDefaultToolkit().createCustomCursor(image, scaled, name);
                if (cursor == null) {
                  failures.add(name + ": createCustomCursor returned null");
                }
              } catch (final Exception | Error problem) {
                failures.add(name + ": createCustomCursor threw " + problem);
              }
            }
            System.out.printf(
                "  ok  %-42s hotspot (%d,%d)  %4d opaque px%n",
                name, scaled.x, scaled.y, opaque);
          }
        }
      }
    }

    if (!headless) {
      final Dimension best = Toolkit.getDefaultToolkit().getBestCursorSize(DESIGN_SIZE, DESIGN_SIZE);
      System.out.println("\ngetBestCursorSize(32,32) on this platform: "
          + best.width + "x" + best.height
          + "  -> pickBestImage prefers the "
          + (best.width >= 24 ? "32px" : "16px") + " asset");
    }

    System.out.println();
    if (failures.isEmpty()) {
      System.out.println("all " + checked + " assets load and are usable as cursors");
    } else {
      System.out.println(failures.size() + " of " + checked + " failed:");
      failures.forEach(failure -> System.out.println("  " + failure));
      System.exit(1);
    }
  }
}
