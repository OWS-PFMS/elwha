package com.owspfm.elwha.colorpicker;

/**
 * The closed set of swatch catalogs the SWATCHES mode can offer (V2, design doc {@code
 * elwha-color-picker-v2-design.md} §3). Client code may subset or reorder the sources via {@link
 * ElwhaColorPicker#setSwatchSources}, but never extend them — the same closed-set contract as
 * {@link PickerMode}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public enum SwatchSource {

  /** The Material catalog — hue grid, shade strip, and recent colors (the V1 swatch stack). */
  MATERIAL("Material"),

  /**
   * The live theme's color roles — every {@code ColorRole}, resolved against the installed palette
   * and mode at paint time, for theme-consistent picks.
   */
  THEME("Theme"),

  /**
   * The user's saved swatches — the picker's favorites model (the NSColorPanel favorites-bar
   * analog). Persistence is client-owned; see {@link ElwhaColorPicker#setFavorites}.
   */
  SAVED("Saved");

  private final String label;

  SwatchSource(final String label) {
    this.label = label;
  }

  String label() {
    return label;
  }
}
