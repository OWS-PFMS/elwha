package com.owspfm.ui.components.theme;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;

/**
 * The 12 semantic type roles of the FlatComp token vocabulary, derived from Material 3's type
 * scale.
 *
 * <p>M3's full scale is 15 (5 sizes across display / headline / title / body / label); FlatComp v1
 * drops the {@code display} tier, which a desktop tool rarely needs. Each role resolves at paint
 * time from {@link UIManager} under the key {@code FlatComp.type.<key>} to a concrete {@link Font},
 * which {@link FlatCompTheme#install} writes from the installed {@link Typography}.
 *
 * <p><strong>Binding rule.</strong> Components MUST call {@link #resolve()} at paint time (or
 * re-resolve on {@code updateUI()}) — never cache the returned {@link Font} across paints.
 *
 * <p><strong>Baseline fallback.</strong> When no theme has been installed, {@link #resolve()}
 * degrades to a sans-serif font at this role's point size, applying {@link
 * TextAttribute#WEIGHT_MEDIUM} as a best-effort approximation for the medium-weight roles. The
 * faithful weight rendering is {@link Typography}'s job — see {@code flatcomp-token-taxonomy.md}
 * §2.2.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum TypeRole {

  /**
   * Headline, large (32pt, regular) — consumer page headers.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HEADLINE_LARGE("headlineLarge", 32, false),
  /**
   * Headline, medium (28pt, regular) — consumer page headers.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HEADLINE_MEDIUM("headlineMedium", 28, false),
  /**
   * Headline, small (24pt, regular) — consumer page headers.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HEADLINE_SMALL("headlineSmall", 24, false),
  /**
   * Title, large (22pt, regular) — the large {@code FlatCard} header.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TITLE_LARGE("titleLarge", 22, false),
  /**
   * Title, medium (16pt, medium) — the default {@code FlatCard} header.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TITLE_MEDIUM("titleMedium", 16, true),
  /**
   * Title, small (14pt, medium) — the {@code FlatCard} sub-header.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  TITLE_SMALL("titleSmall", 14, true),
  /**
   * Body, large (16pt, regular) — {@code FlatCard} body text.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  BODY_LARGE("bodyLarge", 16, false),
  /**
   * Body, medium (14pt, regular) — compact {@code FlatCard} body; the default text role.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  BODY_MEDIUM("bodyMedium", 14, false),
  /**
   * Body, small (12pt, regular) — {@code FlatCard} summary and metadata text.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  BODY_SMALL("bodySmall", 12, false),
  /**
   * Label, large (14pt, medium) — the default {@code FlatChip} label.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LABEL_LARGE("labelLarge", 14, true),
  /**
   * Label, medium (12pt, medium) — the compact {@code FlatChip} label.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LABEL_MEDIUM("labelMedium", 12, true),
  /**
   * Label, small (11pt, medium) — badge and caption text.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LABEL_SMALL("labelSmall", 11, true);

  /** The {@code UIManager} key namespace all type roles resolve under. */
  private static final String KEY_PREFIX = "FlatComp.type.";

  private final String key;
  private final int pt;
  private final boolean medium;

  TypeRole(String key, int pt, boolean medium) {
    this.key = key;
    this.pt = pt;
    this.medium = medium;
  }

  /**
   * Returns the {@code camelCase} token key for this role, e.g. {@code "bodyMedium"}.
   *
   * @return the token key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String key() {
    return key;
  }

  /**
   * Returns the fully-qualified {@link UIManager} key this role resolves under, e.g. {@code
   * "FlatComp.type.bodyMedium"}.
   *
   * @return the fully-qualified {@code UIManager} key
   * @version v0.1.0
   * @since v0.1.0
   */
  public String uiKey() {
    return KEY_PREFIX + key;
  }

  /**
   * Returns this role's point size.
   *
   * @return the point size
   * @version v0.1.0
   * @since v0.1.0
   */
  public int pt() {
    return pt;
  }

  /**
   * Returns whether this role is a medium-weight (M3 500) role rather than a regular-weight (400)
   * one.
   *
   * @return {@code true} if this is a medium-weight role
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean medium() {
    return medium;
  }

  /**
   * Resolves this role to a concrete {@link Font} from {@link UIManager} at the moment of the call.
   *
   * <p>Per the binding rule, callers must invoke this at paint time and must not cache the result
   * across paints. If no theme has been installed (the {@code UIManager} key is absent), this
   * degrades to a best-effort sans-serif font rather than returning {@code null}.
   *
   * @return the resolved font, never {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Font resolve() {
    Font resolved = UIManager.getFont(uiKey());
    return resolved != null ? resolved : fallbackFont();
  }

  private Font fallbackFont() {
    Font base = new Font(Font.SANS_SERIF, Font.PLAIN, pt);
    if (!medium) {
      return base;
    }
    Map<TextAttribute, Object> attributes = new HashMap<>();
    attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_MEDIUM);
    return base.deriveFont(attributes);
  }
}
