package com.owspfm.ui.components.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A font family resolved against all 12 {@link TypeRole}s — the typography half of a {@link
 * Config}.
 *
 * <p>{@link #defaults()} returns the bundled-Inter typography: FlatComp ships Inter with real
 * Regular (400) and Medium (500) faces, so the M3 400/500 weight distinction renders correctly (see
 * {@code flatcomp-token-taxonomy.md} §2.2). {@link #ofFamily(String)} builds typography from any
 * installed family name, falling back to {@link TextAttribute#WEIGHT_MEDIUM} — then {@link
 * Font#BOLD} — for the medium-weight roles when the family supplies no Medium face.
 *
 * <p>Instances are immutable.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class Typography {

  private static final String FONT_RESOURCE_DIR = "/com/owspfm/ui/components/theme/fonts/";
  private static final String INTER_REGULAR_RESOURCE = FONT_RESOURCE_DIR + "Inter-Regular.ttf";
  private static final String INTER_MEDIUM_RESOURCE = FONT_RESOURCE_DIR + "Inter-Medium.ttf";

  private static volatile Typography defaultInstance;

  private final String familyName;
  private final Map<TypeRole, Font> fonts;

  private Typography(String familyName, Map<TypeRole, Font> fonts) {
    this.familyName = familyName;
    this.fonts = fonts;
  }

  /**
   * Returns the resolved font for a type role.
   *
   * @param role the type role to look up
   * @return the font for that role, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Font get(TypeRole role) {
    return fonts.get(role);
  }

  /**
   * Returns the name of the font family this typography is built on.
   *
   * @return the family name
   * @version v0.1.0
   * @since v0.1.0
   */
  public String familyName() {
    return familyName;
  }

  /**
   * Returns the bundled-Inter default typography.
   *
   * <p>The Inter Regular and Medium faces are registered with the {@link GraphicsEnvironment} on
   * first call and the result is cached. If the bundled font resources cannot be loaded, this
   * degrades to {@link #ofFamily(String)} over a logical sans-serif family rather than failing.
   *
   * @return the default typography
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Typography defaults() {
    Typography cached = defaultInstance;
    if (cached != null) {
      return cached;
    }
    synchronized (Typography.class) {
      if (defaultInstance == null) {
        defaultInstance = buildDefault();
      }
      return defaultInstance;
    }
  }

  private static Typography buildDefault() {
    Font regular = loadBundledFont(INTER_REGULAR_RESOURCE);
    Font medium = loadBundledFont(INTER_MEDIUM_RESOURCE);
    if (regular == null || medium == null) {
      return ofFamily(Font.SANS_SERIF);
    }
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    environment.registerFont(regular);
    environment.registerFont(medium);
    EnumMap<TypeRole, Font> fonts = new EnumMap<>(TypeRole.class);
    for (TypeRole role : TypeRole.values()) {
      Font face = role.medium() ? medium : regular;
      fonts.put(role, face.deriveFont((float) role.pt()));
    }
    return new Typography(regular.getFamily(), fonts);
  }

  /**
   * Builds typography from an installed font family name.
   *
   * <p>Regular-weight roles use the family directly. Medium-weight roles use the family's Medium
   * face if it has one; otherwise they fall back to {@link TextAttribute#WEIGHT_MEDIUM}, which the
   * platform honors only if the family supplies the glyphs, and finally to {@link Font#BOLD}.
   *
   * @param familyName the installed font family to build on
   * @return typography over that family
   * @version v0.1.0
   * @since v0.1.0
   */
  public static Typography ofFamily(String familyName) {
    Objects.requireNonNull(familyName, "familyName");
    EnumMap<TypeRole, Font> fonts = new EnumMap<>(TypeRole.class);
    for (TypeRole role : TypeRole.values()) {
      Font base = new Font(familyName, Font.PLAIN, role.pt());
      if (role.medium()) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_MEDIUM);
        base = base.deriveFont(attributes);
      }
      fonts.put(role, base);
    }
    return new Typography(familyName, fonts);
  }

  private static Font loadBundledFont(String resource) {
    try (InputStream stream = Typography.class.getResourceAsStream(resource)) {
      if (stream == null) {
        return null;
      }
      return Font.createFont(Font.TRUETYPE_FONT, stream);
    } catch (Exception loadFailed) {
      return null;
    }
  }
}
