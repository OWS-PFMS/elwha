/**
 * The Elwha text-field family — {@link com.owspfm.elwha.textfield.ElwhaTextField}, a token-native
 * M3 text-field primitive with two chrome variants ({@link
 * com.owspfm.elwha.textfield.ElwhaTextField.Variant#FILLED} / {@link
 * com.owspfm.elwha.textfield.ElwhaTextField.Variant#OUTLINED}), a floating label, supporting/error
 * text, and icon / prefix-suffix slots.
 *
 * <p>Architecturally a <i>decorator</i>: {@code ElwhaTextField extends JComponent} owns the chrome
 * paint, the floating label, the typed slots, and the token mapping, while an embedded {@code
 * JTextField} owns the editing surface (caret, selection, IME, copy/paste, accessibility) that
 * Swing provides for free. Decisions: {@code docs/research/elwha-textfield-design.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
package com.owspfm.elwha.textfield;
