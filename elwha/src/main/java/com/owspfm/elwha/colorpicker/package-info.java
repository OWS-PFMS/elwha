/**
 * The Elwha color picker — {@link com.owspfm.elwha.colorpicker.ElwhaColorPicker} (inline composite
 * with a closed set of {@link com.owspfm.elwha.colorpicker.PickerMode modes}), its modal
 * counterpart, and the anchored {@link com.owspfm.elwha.colorpicker.ElwhaColorPickerPopover}. M3
 * defines no color picker; the design synthesizes the picker grammar shared by M3's date and time
 * pickers.
 *
 * <p><strong>Eyedropper note:</strong> the opt-in screen sampler captures via {@code
 * java.awt.Robot}; on macOS the Screen Recording permission is required for captures to include
 * other applications' windows, and a denial is not detectable in code — see {@link
 * com.owspfm.elwha.colorpicker.ElwhaColorPicker#setEyedropperEnabled}.
 *
 * <p>Design: {@code docs/research/elwha-color-picker-design.md}; V2 design: {@code
 * docs/research/elwha-color-picker-v2-design.md}. Epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/481">#481</a>; V2 epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/482">#482</a>.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha.colorpicker;
