package com.owspfm.elwha.theme;

import com.formdev.flatlaf.json.Json;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Loads a {@link Theme} from a Elwha-normalized palette JSON resource.
 *
 * <p>The schema is thin and maps 1:1 onto the 49 {@link ColorRole} keys — a top-level {@code name},
 * an optional {@code description}, and {@code light} / {@code dark} objects each holding every
 * role's {@code camelCase} key mapped to a {@code "#rrggbb"} string. Elwha owns this contract; a
 * documented, automatable conversion from the M3 theme builder's native export produces it. See
 * Appendix B of {@code elwha-theme-install-api.md}.
 *
 * <p>JSON parsing reuses FlatLaf's bundled parser ({@code com.formdev.flatlaf.json.Json}) — no new
 * dependency. {@link Palette.Builder#build()} validates completeness, so a resource missing any
 * role fails fast with a clear message rather than producing silent {@code null} resolves.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class PaletteLoader {

  private PaletteLoader() {}

  /**
   * Loads a {@link Theme} from a classpath resource.
   *
   * @param resourcePath the absolute classpath resource path of the palette JSON, e.g. {@code
   *     "/com/owspfm/elwha/theme/palettes/baseline.json"}
   * @return the loaded theme
   * @throws IllegalArgumentException if the resource cannot be found, is malformed, or is missing
   *     any color role
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Theme loadTheme(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    InputStream stream = PaletteLoader.class.getResourceAsStream(resourcePath);
    if (stream == null) {
      throw new IllegalArgumentException(
          "Palette resource not found on classpath: " + resourcePath);
    }
    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      return parseTheme(Json.parse(reader), resourcePath);
    } catch (IOException ioFailure) {
      throw new UncheckedIOException("Failed to read palette resource: " + resourcePath, ioFailure);
    } catch (RuntimeException parseFailure) {
      throw new IllegalArgumentException(
          "Malformed palette resource: " + resourcePath + " — " + parseFailure.getMessage(),
          parseFailure);
    }
  }

  private static Theme parseTheme(Object root, String resourcePath) {
    Map<?, ?> rootObject = asObject(root, "<root>");
    Object nameValue = rootObject.get("name");
    String name = nameValue instanceof String ? (String) nameValue : resourcePath;
    Palette light = parsePalette(asObject(rootObject.get("light"), "light"));
    Palette dark = parsePalette(asObject(rootObject.get("dark"), "dark"));
    return new Theme(name, light, dark);
  }

  private static Palette parsePalette(Map<?, ?> roleObject) {
    Palette.Builder builder = Palette.builder();
    for (ColorRole role : ColorRole.values()) {
      Object value = roleObject.get(role.key());
      if (!(value instanceof String)) {
        // Leave the role unset — Palette.Builder.build() reports every missing role at once.
        continue;
      }
      builder.set(role, decodeHex((String) value, role));
    }
    return builder.build();
  }

  private static Color decodeHex(String hex, ColorRole role) {
    try {
      return Color.decode(hex.trim());
    } catch (NumberFormatException badHex) {
      throw new IllegalArgumentException(
          "Role '" + role.key() + "' has an invalid color value: '" + hex + "'", badHex);
    }
  }

  private static Map<?, ?> asObject(Object value, String label) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException(
          "Expected a JSON object for '" + label + "' but found " + describe(value));
    }
    return (Map<?, ?>) value;
  }

  private static String describe(Object value) {
    return value == null ? "nothing" : value.getClass().getSimpleName();
  }
}
