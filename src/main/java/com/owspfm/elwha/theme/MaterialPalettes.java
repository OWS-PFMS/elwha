package com.owspfm.elwha.theme;

/**
 * The {@link Theme}s Elwha ships out of the box.
 *
 * <p>v1 ships exactly one — {@link #baseline()}, the Material 3 baseline scheme — whose job is to
 * validate the token pipeline end-to-end. Branded palettes (e.g. OWS's own colors) are
 * consumer-side artifacts: a consumer drops an M3-builder export into its own resources and loads
 * it with {@link PaletteLoader}. The token <em>vocabulary</em> is Elwha's; the palette
 * <em>values</em> are the consumer's. See {@code elwha-design-direction.md} §13.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class MaterialPalettes {

  private static final String BASELINE_RESOURCE = "/com/owspfm/elwha/theme/palettes/baseline.json";

  private static volatile Theme baseline;

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
}
