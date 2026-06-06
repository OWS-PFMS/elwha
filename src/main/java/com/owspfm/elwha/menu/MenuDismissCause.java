package com.owspfm.elwha.menu;

/**
 * Why an {@link ElwhaMenu} closed — reported to the menu's {@code onClose} hook so a consumer can
 * distinguish "the user picked an item" from "the user dismissed the menu without choosing."
 * Mirrors the dialog family's {@code DismissCause}.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum MenuDismissCause {

  /**
   * A menu item was activated. In {@code SelectionMode.NONE}/{@code SINGLE} the menu closes on
   * select; in {@code MULTI} it stays open, so this cause is reported only on the final dismiss.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  SELECTION,

  /**
   * The Escape key closed the menu.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  ESCAPE,

  /**
   * A mouse press outside the menu surface closed it (light dismiss).
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  OUTSIDE_PRESS,

  /**
   * Keyboard focus left the menu (a Tab-away or focus moving into other content) and closed it.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  FOCUS_LOST,

  /**
   * The menu was closed programmatically via {@code dismiss()}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  PROGRAMMATIC,

  /**
   * The menu was superseded — another menu opened and replaced it (at most one menu is open at a
   * time). Focus is <em>not</em> restored to this menu's trigger on a supersede; the new menu owns
   * it.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  SUPERSEDED
}
