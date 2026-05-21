package com.owspfm.elwha.theme;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The {@link Theme}s Elwha ships out of the box, organised into two directory-derived tiers.
 *
 * <p>{@link #baseline()} is the Material 3 baseline scheme — the default an M3 theme builder
 * produces before a custom source color is chosen, and the theme to install when validating the
 * pipeline.
 *
 * <p>The bundled demo set is split into two tiers, each its own resource subdirectory:
 *
 * <ul>
 *   <li>{@link #primary()} — the curated set under {@code theme/palettes/primary/}: the baseline
 *       plus the ROYGBIV demo palettes.
 *   <li>{@link #secondary()} — the broader exploration set under {@code theme/palettes/secondary/}.
 * </ul>
 *
 * <p>Each tier is <em>directory-derived</em>, never a hardcoded list — the accessors enumerate the
 * subdirectory, so dropping a new Elwha-format palette JSON in there is enough for it to appear,
 * with no code change. Within a tier the themes are returned in <strong>spectral order</strong>:
 * sorted by the hue of each theme's {@code primary} color (red → violet), with neutral-family
 * palettes collected at the end. Consumers remain free to ship their own palettes loaded directly
 * through {@link PaletteLoader}: the token <em>vocabulary</em> is Elwha's, the palette
 * <em>values</em> are the consumer's (see {@code elwha-design-direction.md} §13).
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.1.0
 */
public final class MaterialPalettes {

  private static final String PALETTES_DIR = "/com/owspfm/elwha/theme/palettes";
  private static final String PRIMARY_DIR = PALETTES_DIR + "/primary";
  private static final String SECONDARY_DIR = PALETTES_DIR + "/secondary";
  private static final String BASELINE_RESOURCE = PRIMARY_DIR + "/baseline.json";

  // Themes whose name carries a neutral-family keyword sort after the chromatic spread. M3 Theme
  // Builder saturates every theme's primary role regardless of seed, so the palette content itself
  // carries no neutral signal — the theme name is the only available cue.
  private static final List<String> NEUTRAL_KEYWORDS = List.of("grey", "gray", "brown");

  // Spectral order: chromatic themes first, sorted by primary-role hue (red → violet); neutral-
  // family themes last; ties broken by name for a stable order.
  private static final Comparator<Theme> SPECTRAL =
      Comparator.comparingInt(MaterialPalettes::neutralRank)
          .thenComparingDouble(MaterialPalettes::primaryHue)
          .thenComparing(Theme::name);

  private static volatile Theme baseline;
  private static volatile List<Theme> primary;
  private static volatile List<Theme> secondary;

  private MaterialPalettes() {}

  /**
   * Returns the Material 3 baseline theme — the default light/dark scheme an M3 theme builder
   * produces before a custom source color is chosen.
   *
   * <p>Loaded once from the bundled JSON resource and cached. This is the theme to install when
   * validating the pipeline or when a consumer has not yet supplied its own palette.
   *
   * <p><strong>Known quirk:</strong> the baseline ships a single-seed M3 export, which under M3's
   * default Tonal Spot algorithm produces near-identical {@code primaryContainer} and {@code
   * secondaryContainer} values in <em>light</em> mode (high-tone end of the tonal palette, where
   * the secondary hue rotation compresses toward white). The same role pair is comfortably distinct
   * in <em>dark</em> mode (low-tone end). This is M3-correct algorithm output, not a transcription
   * artifact. Consumers needing visibly distinct containers across both modes should ship their own
   * palette built with a more expressive scheme variant (Expressive / Vibrant / Fidelity) or with
   * multi-seed core colors.
   *
   * @return the baseline theme
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Theme baseline() {
    Theme cached = baseline;
    if (cached != null) {
      return cached;
    }
    synchronized (MaterialPalettes.class) {
      if (baseline == null) {
        baseline = PaletteLoader.loadTheme(BASELINE_RESOURCE);
      }
      return baseline;
    }
  }

  /**
   * Returns the curated <strong>primary</strong> tier — the baseline plus the ROYGBIV demo palettes
   * — discovered from {@code theme/palettes/primary/} and returned in spectral order.
   *
   * <p>The set is directory-derived (both from exploded classes and from inside the packaged jar),
   * so it always reflects exactly the palette JSONs present. Loaded once and cached.
   *
   * @return the primary-tier themes in spectral order; never empty in a correctly packaged build
   * @throws IllegalArgumentException if a bundled palette resource is malformed
   * @version v0.3.0
   * @since v0.3.0
   */
  public static List<Theme> primary() {
    List<Theme> cached = primary;
    if (cached != null) {
      return cached;
    }
    synchronized (MaterialPalettes.class) {
      if (primary == null) {
        primary = loadTier(PRIMARY_DIR);
      }
      return primary;
    }
  }

  /**
   * Returns the broader <strong>secondary</strong> exploration tier — discovered from {@code
   * theme/palettes/secondary/} and returned in spectral order.
   *
   * <p>Additive to {@link #primary()}: the two tiers are disjoint — the secondary tier carries only
   * colors the primary tier does not — and are never browsed simultaneously. Directory-derived and
   * cached like the primary tier.
   *
   * @return the secondary-tier themes in spectral order; never empty in a correctly packaged build
   * @throws IllegalArgumentException if a bundled palette resource is malformed
   * @version v0.3.0
   * @since v0.3.0
   */
  public static List<Theme> secondary() {
    List<Theme> cached = secondary;
    if (cached != null) {
      return cached;
    }
    synchronized (MaterialPalettes.class) {
      if (secondary == null) {
        secondary = loadTier(SECONDARY_DIR);
      }
      return secondary;
    }
  }

  private static List<Theme> loadTier(final String dir) {
    final List<Theme> themes = new ArrayList<>();
    for (final String fileName : discoverPaletteResources(dir)) {
      themes.add(PaletteLoader.loadTheme(dir + "/" + fileName));
    }
    themes.sort(SPECTRAL);
    return List.copyOf(themes);
  }

  private static int neutralRank(final Theme theme) {
    final String name = theme.name().toLowerCase(Locale.ROOT);
    for (final String keyword : NEUTRAL_KEYWORDS) {
      if (name.contains(keyword)) {
        return 1;
      }
    }
    return 0;
  }

  private static float primaryHue(final Theme theme) {
    final Color primaryColor = theme.light().get(ColorRole.PRIMARY);
    return Color.RGBtoHSB(
        primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), null)[0];
  }

  // Enumerates the *.json file names directly under a palettes resource subdirectory. Handles both
  // an exploded classpath (file: URL) and the packaged jar (jar: URL) so the directory-derived
  // contract holds in every run mode.
  private static List<String> discoverPaletteResources(final String dir) {
    final URL directory = MaterialPalettes.class.getResource(dir);
    if (directory == null) {
      return List.of();
    }
    try {
      final String protocol = directory.getProtocol();
      if ("file".equals(protocol)) {
        final String[] names =
            new File(directory.toURI()).list((unused, name) -> name.endsWith(".json"));
        return names == null ? List.of() : List.of(names);
      }
      if ("jar".equals(protocol)) {
        return jarPaletteEntries((JarURLConnection) directory.openConnection());
      }
    } catch (URISyntaxException | IOException failure) {
      throw new IllegalStateException("Failed to enumerate bundled palettes under " + dir, failure);
    }
    return List.of();
  }

  private static List<String> jarPaletteEntries(final JarURLConnection connection)
      throws IOException {
    final String prefix = connection.getEntryName() + "/";
    final List<String> names = new ArrayList<>();
    // The JarFile is the shared, JVM-cached classpath archive — iterate it but do not close it.
    final JarFile jar = connection.getJarFile();
    final Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      final String entry = entries.nextElement().getName();
      if (entry.startsWith(prefix) && entry.endsWith(".json")) {
        final String simple = entry.substring(prefix.length());
        if (!simple.isEmpty() && simple.indexOf('/') < 0) {
          names.add(simple);
        }
      }
    }
    return names;
  }
}
