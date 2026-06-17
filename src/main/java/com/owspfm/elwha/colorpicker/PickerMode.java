package com.owspfm.elwha.colorpicker;

/**
 * The closed set of ways an {@link ElwhaColorPicker} lets people pick or define a color (design doc
 * {@code elwha-color-picker-design.md} TL;DR-2). Mirrors the fixed-mode precedent of macOS's color
 * panel, WinUI's {@code ColorPicker}, and Chrome's color popup: client code may subset or reorder
 * the modes via {@link ElwhaColorPicker#setModes}, but never extend them — there is no pluggable
 * chooser-panel API by design.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public enum PickerMode {

  /** Quick pick — the Material swatch catalog: hue grid, shade strip, and recent colors. */
  SWATCHES("Swatches", "palette"),

  /** Freeform pick — the saturation/value square with a hue slider. */
  SPECTRUM("Spectrum", "gradient"),

  /**
   * Freeform pick — the hue/saturation wheel with a value slider (V2, design doc {@code
   * elwha-color-picker-v2-design.md} §2).
   *
   * @since v0.5.0
   */
  WHEEL("Wheel", "colors"),

  /** Precise definition — RGB/HSV channel sliders and a hex field. */
  SLIDERS("Sliders", "tune");

  private final String label;
  private final String iconName;

  PickerMode(final String label, final String iconName) {
    this.label = label;
    this.iconName = iconName;
  }

  String label() {
    return label;
  }

  String iconName() {
    return iconName;
  }
}
