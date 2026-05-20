package com.owspfm.elwha.theme;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The {@link Theme}s Elwha ships out of the box.
 *
 * <p>{@link #baseline()} is the Material 3 baseline scheme — the default an M3 theme builder
 * produces before a custom source color is chosen, and the theme to install when validating the
 * pipeline. {@link #bundled()} returns every palette bundled under the {@code theme/palettes/}
 * resource directory: the baseline plus the ROYGBIV demo set that The Elwha Showcase's palette
 * picker offers.
 *
 * <p>The bundled set is <em>directory-derived</em>, never a hardcoded list — {@link #bundled()}
 * enumerates the {@code palettes/} resource directory, so dropping a new Elwha-format palette JSON
 * in there is enough for it to appear, with no code change. Consumers remain free to ship their own
 * palettes loaded directly through {@link PaletteLoader}: the token <em>vocabulary</em> is Elwha's,
 * the palette <em>values</em> are the consumer's (see {@code elwha-design-direction.md} §13).
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.1.0
 */
public final class MaterialPalettes {

  private static final String PALETTES_DIR = "/com/owspfm/elwha/theme/palettes";
  private static final String BASELINE_RESOURCE = PALETTES_DIR + "/baseline.json";

  private static volatile Theme baseline;
  private static volatile List<Theme> bundled;

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
   * Returns every {@link Theme} bundled under the {@code theme/palettes/} resource directory,
   * sorted by {@linkplain Theme#name() name}.
   *
   * <p>The set is discovered by enumerating the resource directory — both when running from
   * exploded classes and from inside the packaged jar — so it always reflects exactly the palette
   * JSONs present, with no hardcoded theme list. Loaded once and cached.
   *
   * @return the bundled themes, sorted by name; never empty in a correctly packaged build
   * @throws IllegalArgumentException if a bundled palette resource is malformed
   * @version v0.3.0
   * @since v0.3.0
   */
  public static List<Theme> bundled() {
    List<Theme> cached = bundled;
    if (cached != null) {
      return cached;
    }
    synchronized (MaterialPalettes.class) {
      if (bundled == null) {
        List<Theme> themes = new ArrayList<>();
        for (String fileName : discoverPaletteResources()) {
          themes.add(PaletteLoader.loadTheme(PALETTES_DIR + "/" + fileName));
        }
        themes.sort(Comparator.comparing(Theme::name));
        bundled = List.copyOf(themes);
      }
      return bundled;
    }
  }

  // Enumerates the *.json file names directly under the palettes resource directory. Handles both
  // an exploded classpath (file: URL) and the packaged jar (jar: URL) so the directory-derived
  // contract holds in every run mode.
  private static List<String> discoverPaletteResources() {
    URL directory = MaterialPalettes.class.getResource(PALETTES_DIR);
    if (directory == null) {
      return List.of();
    }
    try {
      final String protocol = directory.getProtocol();
      if ("file".equals(protocol)) {
        String[] names = new File(directory.toURI()).list((unused, name) -> name.endsWith(".json"));
        return names == null ? List.of() : List.of(names);
      }
      if ("jar".equals(protocol)) {
        return jarPaletteEntries((JarURLConnection) directory.openConnection());
      }
    } catch (URISyntaxException | IOException failure) {
      throw new IllegalStateException(
          "Failed to enumerate bundled palettes under " + PALETTES_DIR, failure);
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
