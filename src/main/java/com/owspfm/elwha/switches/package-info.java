/**
 * The Elwha Material 3 switch — epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/401">#401</a>. {@link
 * com.owspfm.elwha.switches.ElwhaSwitch} is one dedicated {@link javax.swing.JComponent} painting
 * the full M3 chrome (corner-full track with the unselected outline, the state-morphing handle,
 * optional on-handle icons) itself — <em>not</em> a styled {@code JToggleButton} or {@code
 * ButtonUI} delegate (design §2). The package is named {@code switches} because {@code switch} is a
 * Java reserved word (the same dodge MDC-Android's {@code materialswitch} package makes). Spec
 * lives in {@code docs/research/elwha-switch-design.md} + {@code elwha-switch-research.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
package com.owspfm.elwha.switches;
