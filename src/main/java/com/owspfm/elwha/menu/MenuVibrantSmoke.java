package com.owspfm.elwha.menu;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Headless smoke for epic #298 S5 — the {@link ColorStyle#VIBRANT} color style. Renders each color
 * style's {@code renderPreview()} surface (one item pre-selected) into an off-screen image and
 * counts exact role-color pixels, proving the research §K role matrix holds in actual paint output:
 *
 * <ul>
 *   <li><strong>Standard</strong> paints the container in {@code SURFACE_CONTAINER_LOW} and never
 *       uses the bold {@code TERTIARY} fill;
 *   <li><strong>Vibrant</strong> tints the container {@code TERTIARY_CONTAINER}, paints the
 *       selected item in the bold {@code TERTIARY} fill, and never falls back to {@code
 *       SURFACE_CONTAINER_LOW};
 *   <li>Vibrant renders identically by role in dark mode (free via {@link ElwhaTheme}).
 * </ul>
 *
 * Runs fully headless (off-screen image paint — no display). Exits non-zero on any failed
 * assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuVibrantSmoke {

  private MenuVibrantSmoke() {}

  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    MorphAnimator.setReducedMotion(true);

    int checks = 0;
    int failures = 0;

    // --- enum surface (pure) ---
    failures += check(ColorStyle.values().length == 2, "ColorStyle has 2 values");
    checks++;
    boolean hasVibrant = false;
    for (final ColorStyle s : ColorStyle.values()) {
      hasVibrant |= s == ColorStyle.VIBRANT;
    }
    failures += check(hasVibrant, "ColorStyle.VIBRANT exists");
    checks++;

    // --- light mode role matrix ---
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final Color surfaceLow = ColorRole.SURFACE_CONTAINER_LOW.resolve();
    final Color tertiaryContainer = ColorRole.TERTIARY_CONTAINER.resolve();
    final Color tertiary = ColorRole.TERTIARY.resolve();

    final BufferedImage standard = render(ColorStyle.STANDARD);
    failures +=
        check(count(standard, surfaceLow) >= 500, "Standard container = SURFACE_CONTAINER_LOW");
    checks++;
    failures +=
        check(
            count(standard, tertiaryContainer) >= 100,
            "Standard selected fill = TERTIARY_CONTAINER");
    checks++;
    failures += check(count(standard, tertiary) == 0, "Standard never uses bold TERTIARY");
    checks++;

    final BufferedImage vibrant = render(ColorStyle.VIBRANT);
    failures +=
        check(count(vibrant, tertiaryContainer) >= 500, "Vibrant container = TERTIARY_CONTAINER");
    checks++;
    failures += check(count(vibrant, tertiary) >= 100, "Vibrant selected fill = bold TERTIARY");
    checks++;
    failures += check(count(vibrant, surfaceLow) == 0, "Vibrant never uses SURFACE_CONTAINER_LOW");
    checks++;

    // --- dark mode: same roles, container still resolves & fills ---
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    final Color darkTertiaryContainer = ColorRole.TERTIARY_CONTAINER.resolve();
    final BufferedImage vibrantDark = render(ColorStyle.VIBRANT);
    failures +=
        check(
            count(vibrantDark, darkTertiaryContainer) >= 500,
            "Vibrant container fills dark TERTIARY_CONTAINER");
    checks++;
    failures +=
        check(
            !darkTertiaryContainer.equals(tertiaryContainer),
            "dark TERTIARY_CONTAINER differs from light");
    checks++;

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static BufferedImage render(final ColorStyle style) {
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .colorStyle(style)
            .addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename"))
            .addItem(selected(ElwhaMenuItem.of(MaterialIcons.star(20), "Favorite")))
            .addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"))
            .build();
    final JComponent preview = menu.renderPreview();
    final Dimension d = preview.getPreferredSize();
    preview.setSize(d);
    layoutTree(preview);
    final BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      preview.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static ElwhaMenuItem selected(final ElwhaMenuItem item) {
    item.setSelected(true);
    return item;
  }

  private static void layoutTree(final Component c) {
    if (c instanceof Container parent) {
      parent.doLayout();
      for (final Component child : parent.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static int count(final BufferedImage img, final Color role) {
    final int target = role.getRGB() | 0xFF000000;
    int n = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if ((img.getRGB(x, y) | 0xFF000000) == target && (img.getRGB(x, y) >>> 24) == 0xFF) {
          n++;
        }
      }
    }
    return n;
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
