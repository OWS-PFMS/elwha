package com.owspfm.ui.components.theme;

/**
 * The {@link Theme}s FlatComp ships out of the box.
 *
 * <p>v1 ships exactly one — {@link #baseline()}, the Material 3 baseline scheme — whose job is to
 * validate the token pipeline end-to-end. Branded palettes (e.g. OWS's own colors) are
 * consumer-side artifacts: a consumer drops an M3-builder export into its own resources and loads
 * it with {@link PaletteLoader}. The token <em>vocabulary</em> is FlatComp's; the palette
 * <em>values</em> are the consumer's. See {@code flatcomp-design-direction.md} §13.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class MaterialPalettes {

  private static final String BASELINE_RESOURCE =
      "/com/owspfm/ui/components/theme/palettes/baseline.json";

  private static volatile Theme baseline;

  private MaterialPalettes() {}

  /**
   * Returns the Material 3 baseline theme — the default light/dark scheme an M3 theme builder
   * produces before a custom source color is chosen.
   *
   * <p>Loaded once from the bundled JSON resource and cached. This is the theme to install when
   * validating the pipeline or when a consumer has not yet supplied its own palette.
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
