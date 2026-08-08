package com.owspfm.elwha.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A font family resolved against all 15 {@link TypeRole}s — the typography half of a {@link
 * Config}.
 *
 * <p>{@link #defaults()} returns the bundled-Inter typography: Elwha ships Inter with real Regular
 * (400) and Medium (500) faces, so the M3 400/500 weight distinction renders correctly (see {@code
 * elwha-token-taxonomy.md} §2.2). {@link #ofFamily(String)} builds typography from any installed
 * family name, falling back to {@link TextAttribute#WEIGHT_MEDIUM} — then {@link Font#BOLD} — for
 * the medium-weight roles when the family supplies no Medium face.
 *
 * <p>Instances are immutable.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public final class Typography {

  private static final String FONT_RESOURCE_DIR = "/com/owspfm/elwha/theme/fonts/";
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
   * Returns the name of the font family this typography is built on — the Regular face's family.
   *
   * <p>A typography can span two AWT families: a static 500-weight TTF names itself apart from its
   * Regular sibling, so {@link #defaults()} reports {@code "Inter"} here while its five 500-weight
   * roles resolve {@code "Inter Medium"}. That is the family to hand back to {@link
   * #ofFamily(String)}, which looks the Medium face up again — the round trip keeps both faces.
   *
   * @return the family name
   * @version v0.5.0
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

  // Register the bundled faces, then build through the same path ofFamily uses. Deriving from the
  // loaded Font objects instead would resolve the identical faces but name the regular ones by
  // their full face name ("Inter Regular") where ofFamily names them by family ("Inter") — and
  // Font.equals compares the name, so the round trip ofFamily(defaults().familyName()) came back
  // rendering-identical but not equal. That was invisible until Typography gained value equality
  // (#698); routing both paths through one construction is what makes the documented round trip
  // lossless by the contract and not merely by what it paints.
  private static Typography buildDefault() {
    Font regular = loadBundledFont(INTER_REGULAR_RESOURCE);
    Font medium = loadBundledFont(INTER_MEDIUM_RESOURCE);
    if (regular == null || medium == null) {
      return ofFamily(Font.SANS_SERIF);
    }
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    environment.registerFont(regular);
    environment.registerFont(medium);
    return ofFamily(regular.getFamily());
  }

  /**
   * Builds typography from an installed font family name.
   *
   * <p>Regular-weight roles use the family directly. Medium-weight roles use the family's Medium
   * face if the platform has one installed under {@code "<family> Medium"} — which is how a static
   * 500-weight TTF names itself, the bundled Inter Medium included. Failing that they fall back to
   * {@link TextAttribute#WEIGHT_MEDIUM}, which the platform honors only if the family supplies the
   * glyphs.
   *
   * <p>Looking the Medium face up is what makes {@code ofFamily(existing.familyName())} lossless:
   * {@link #familyName()} reports the Regular face's family, so without it a round trip through
   * this method would quietly downgrade every 500-weight role to synthesis. Since #698 the round
   * trip is lossless by {@link #equals(Object)} and not merely by what it paints — {@link
   * #defaults()} builds through this method rather than deriving from the bundled {@code Font}
   * objects, so both paths name their faces the same way.
   *
   * @param familyName the installed font family to build on
   * @return typography over that family
   * @version v0.5.0
   * @since v0.1.0
   */
  public static Typography ofFamily(String familyName) {
    Objects.requireNonNull(familyName, "familyName");
    Font mediumFace = installedFace(familyName + " Medium");
    EnumMap<TypeRole, Font> fonts = new EnumMap<>(TypeRole.class);
    for (TypeRole role : TypeRole.values()) {
      fonts.put(role, faceForRole(familyName, mediumFace, role));
    }
    return new Typography(familyName, fonts);
  }

  private static Font faceForRole(String familyName, Font mediumFace, TypeRole role) {
    if (role.medium() && mediumFace != null) {
      return mediumFace.deriveFont((float) role.pt());
    }
    Font base = new Font(familyName, Font.PLAIN, role.pt());
    if (!role.medium()) {
      return base;
    }
    Map<TextAttribute, Object> attributes = new HashMap<>();
    attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_MEDIUM);
    return base.deriveFont(attributes);
  }

  // A Font asked for a family the platform does not have resolves silently to Dialog rather than
  // failing, so the family it reports back is the only reliable existence check.
  private static Font installedFace(String candidate) {
    Font face = new Font(candidate, Font.PLAIN, 1);
    return face.getFamily().equalsIgnoreCase(candidate) ? face : null;
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

  /**
   * Value equality — two typographies are equal when they name the same family and resolve every
   * role to the same font.
   *
   * <p>{@link #ofFamily(String)} is documented as a lossless round trip through {@link
   * #familyName()}, and without this it was lossless in rendering but produced an object that did
   * not compare equal to its source — which would have shown up as a spurious difference the moment
   * {@link Config} gained equality. Ruled alongside {@link Palette} and {@link Theme} in (#698).
   *
   * @param obj the object to compare against
   * @return whether {@code obj} is a typography over the same family with the same faces
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Typography other)) {
      return false;
    }
    return familyName.equals(other.familyName) && fonts.equals(other.fonts);
  }

  /**
   * @return a hash consistent with {@link #equals(Object)}
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public int hashCode() {
    return 31 * familyName.hashCode() + fonts.hashCode();
  }

  /**
   * @return the family this typography is built over
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public String toString() {
    return "Typography[" + familyName + "]";
  }
}
