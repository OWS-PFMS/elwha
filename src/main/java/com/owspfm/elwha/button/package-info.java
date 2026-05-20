/**
 * The Elwha button family — {@link com.owspfm.elwha.button.ElwhaButton}, a token-native M3
 * Expressive text-button primitive with five emphasis variants ({@link
 * com.owspfm.elwha.button.ButtonVariant#ELEVATED} / {@link
 * com.owspfm.elwha.button.ButtonVariant#FILLED} / {@link
 * com.owspfm.elwha.button.ButtonVariant#FILLED_TONAL} / {@link
 * com.owspfm.elwha.button.ButtonVariant#OUTLINED} / {@link
 * com.owspfm.elwha.button.ButtonVariant#TEXT}), a {@link
 * com.owspfm.elwha.button.ButtonInteractionMode#CLICKABLE} / {@link
 * com.owspfm.elwha.button.ButtonInteractionMode#SELECTABLE} interaction axis, and {@link
 * com.owspfm.elwha.button.ButtonShape#ROUND} / {@link com.owspfm.elwha.button.ButtonShape#SQUARE}
 * shape options.
 *
 * <p>The fifth token-native component (after {@code ElwhaChip} #31, {@code ElwhaSurface} #43,
 * {@code ElwhaIconButton} #45, and the V3 {@code ElwhaCard} #80), filling out the action-row
 * vocabulary alongside {@code ElwhaIconButton}. V3 Card spec §3.3 pairs Outlined cards with
 * Filled-Tonal buttons, which had no path through the lib before this primitive landed.
 *
 * <p>Decisions and the deliberate M3 divergences: {@code docs/research/elwha-button-design.md}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
package com.owspfm.elwha.button;
