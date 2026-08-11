/**
 * The Elwha Material 3 Expressive slider. {@link com.owspfm.elwha.slider.ElwhaSlider} is one
 * dedicated {@link javax.swing.JComponent} backed by a {@link javax.swing.BoundedRangeModel},
 * painting the full M3 chrome (split active/inactive track with the rounded-end gap, tall pill
 * handle, stop indicators, value-indicator bubble) itself — <em>not</em> a {@code JSlider} subclass
 * or {@code SliderUI} delegate (design §2).
 *
 * <p>Design: {@code docs/research/elwha-slider-design.md}; research: {@code
 * elwha-slider-research.md}. Epic <a href="https://github.com/OWS-PFMS/elwha/issues/340">#340</a>.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.4.0
 */
package com.owspfm.elwha.slider;
