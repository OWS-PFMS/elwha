/**
 * The Material 3 Basic Dialog primitive ({@link com.owspfm.elwha.dialog.ElwhaDialog}) — a
 * token-themed modal surface shown as an in-window overlay on the host frame's {@link
 * javax.swing.JLayeredPane}, with typed anatomy slots (icon → headline → supporting text → content
 * → actions) and a typed action-row API that takes {@code ElwhaButton}. Formalizes the hand-rolled
 * About-dialog chrome from #252; exists because {@code ElwhaButton extends JComponent} closes
 * {@code JOptionPane} to Elwha actions.
 *
 * <p>Design and decisions: {@code docs/research/elwha-dialog-design.md}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
package com.owspfm.elwha.dialog;
