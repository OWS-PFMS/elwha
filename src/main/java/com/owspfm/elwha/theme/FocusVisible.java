package com.owspfm.elwha.theme;

import java.awt.event.FocusEvent;

/**
 * Classifies a {@link FocusEvent.Cause} as keyboard-driven ("focus-visible") or not, so a component
 * can paint its focus indicator only when focus arrives via keyboard navigation — the modern M3 /
 * CSS {@code :focus-visible} behavior. A mouse click ({@link FocusEvent.Cause#MOUSE_EVENT}) or a
 * window re-activation ({@link FocusEvent.Cause#ACTIVATION}) acquires focus without showing the
 * ring; only directional {@code TRAVERSAL_*} (Tab) traversal does.
 *
 * <p>Stateless helper shared by the button-family primitives ({@link
 * com.owspfm.elwha.button.ElwhaButton}, {@link com.owspfm.elwha.iconbutton.ElwhaIconButton}, {@link
 * com.owspfm.elwha.fab.ElwhaFab}); each keeps its own focus-visible flag (set in {@code
 * focusGained}, cleared on a pointer press and on {@code focusLost}) and consults this only for the
 * keyboard test.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class FocusVisible {

  private FocusVisible() {}

  /**
   * Returns whether a focus gain with the given cause should show a keyboard focus indicator.
   *
   * @param cause the focus-gain cause from {@link FocusEvent#getCause()}
   * @return {@code true} only for the directional Tab-traversal causes; {@code false} for mouse,
   *     window activation, programmatic, and unknown causes
   * @version v0.3.0
   * @since v0.3.0
   */
  public static boolean isKeyboardCause(final FocusEvent.Cause cause) {
    return switch (cause) {
      case TRAVERSAL, TRAVERSAL_UP, TRAVERSAL_DOWN, TRAVERSAL_FORWARD, TRAVERSAL_BACKWARD -> true;
      default -> false;
    };
  }
}
