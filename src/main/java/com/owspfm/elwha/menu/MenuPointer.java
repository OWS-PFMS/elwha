package com.owspfm.elwha.menu;

import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;

/**
 * The submenu chain's one read of the OS pointer, guarded so the surrounding logic runs headless.
 *
 * <p>{@link MouseInfo#getPointerInfo()} throws {@link java.awt.HeadlessException} rather than
 * answering {@code null} when there is no display, so the {@code info != null} check the call sites
 * carried never got the chance to fire. That made every code path reaching the chain-active
 * recomputation — which is most of the submenu machinery — structurally untestable outside the
 * {@code gui} tier, and pushed coverage there that had nothing to do with a real pointer (#709).
 *
 * <p>A {@code null} answer is already the "the platform will not tell me" case the callers handle:
 * the chain-active pass falls through to its focus/keyboard signal, and the hover-away dwell treats
 * an unknown pointer as not-in-chain. So headless behaves like a screen-locked or pointer-capture
 * moment rather than like a special case.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class MenuPointer {

  private MenuPointer() {}

  /**
   * The pointer's current screen location, or {@code null} when the platform will not answer —
   * headless, or a moment where the OS declines (a locked screen, another window holding a pointer
   * grab).
   *
   * @return the pointer location in screen coordinates, or {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  static Point screenLocation() {
    if (GraphicsEnvironment.isHeadless()) {
      return null;
    }
    final PointerInfo info = MouseInfo.getPointerInfo();
    return info != null ? info.getLocation() : null;
  }
}
