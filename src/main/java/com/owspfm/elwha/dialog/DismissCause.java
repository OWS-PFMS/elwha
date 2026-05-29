package com.owspfm.elwha.dialog;

import java.util.function.Consumer;

/**
 * Why an Elwha dialog closed — reported to a dialog's {@code onClose(}{@link Consumer}{@code
 * <DismissCause>)} hook. Shared across the Elwha dialog primitives ({@link ElwhaDialog}, the M3
 * Basic Dialog, and {@link ElwhaFullScreenDialog}, the M3 Full-screen Dialog) so a consumer's close
 * handler reads the same vocabulary regardless of dialog type.
 *
 * <p>Promoted to a top-level type from a nested {@code ElwhaDialog.DismissCause} in epic #271 S1
 * when the overlay-host lifecycle was extracted into {@link AbstractElwhaDialog} — a pre-1.0
 * breaking change with no compatibility shim.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.3.0
 * @since v0.3.0
 */
public enum DismissCause {
  /** A confirming action fired. */
  CONFIRM,
  /** A cancelling action fired, Esc was pressed, or the full-screen close affordance was used. */
  CANCEL,
  /** An alternate action fired. */
  ALTERNATE,
  /**
   * The scrim was clicked while scrim-dismiss was enabled (Basic Dialog only — no scrim exists on a
   * Full-screen Dialog).
   */
  SCRIM,
  /** The Escape key was pressed while Esc-dismiss was enabled. */
  ESC,
  /** A dialog's {@code dismiss()} was called programmatically. */
  PROGRAMMATIC
}
