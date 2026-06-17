/**
 * The Elwha color picker — {@link com.owspfm.elwha.colorpicker.ElwhaColorPicker} (inline composite
 * with a closed set of {@link com.owspfm.elwha.colorpicker.PickerMode modes}), its modal
 * counterpart, and the anchored {@link com.owspfm.elwha.colorpicker.ElwhaColorPickerPopover}. M3
 * defines no color picker; the design synthesizes the picker grammar shared by M3's date and time
 * pickers (docs/research/elwha-color-picker-design.md, epic #481; V2
 * docs/research/elwha-color-picker-v2-design.md, epic #482).
 *
 * <p><strong>Eyedropper note:</strong> the opt-in screen sampler captures via {@code
 * java.awt.Robot}; on macOS the Screen Recording permission is required for captures to include
 * other applications' windows, and a denial is not detectable in code — see {@link
 * com.owspfm.elwha.colorpicker.ElwhaColorPicker#setEyedropperEnabled}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha.colorpicker;
