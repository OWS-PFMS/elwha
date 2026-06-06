package com.owspfm.elwha.menu;

/**
 * How an {@link ElwhaMenu} treats item activation (M3 menu "Selection", design §9). Mirrors M3's
 * exact nouns per the terminology lock (design §P) and is name-compatible with the cross-cutting
 * selection surface (#252).
 *
 * <p>The selected visual — a {@code TERTIARY_CONTAINER} (Standard) / bold {@code TERTIARY}
 * (Vibrant) fill plus a ✓ checkmark, a 3:1 + non-color cue per accessibility (§X) — is shared by
 * both persistent modes; only the activation behavior differs.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum SelectionMode {

  /**
   * Action menu (the default): activating an item fires its action and closes the menu, with no
   * persistent selection. This is the Phase-1 behavior — every item is a one-shot command.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  NONE,

  /**
   * Single-select: at most one item is selected at a time. Activating an item selects it,
   * auto-deselects the previously selected item, and closes the menu (a {@link
   * MenuDismissCause#SELECTION} close that restores focus to the trigger). The consumer may set an
   * initial selection with {@link ElwhaMenuItem#setSelected(boolean)} before building. Radio-like:
   * the selected item exposes {@code AccessibleState.SELECTED}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  SINGLE,

  /**
   * Multi-select: any number of items may be selected. Activating an item toggles its selection and
   * the menu <em>stays open</em> ({@code keepOpen}) until dismissed by the normal light-dismiss /
   * Escape paths. Checkbox-like: each selected item exposes {@code AccessibleState.CHECKED} in
   * addition to {@code SELECTED}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  MULTI
}
