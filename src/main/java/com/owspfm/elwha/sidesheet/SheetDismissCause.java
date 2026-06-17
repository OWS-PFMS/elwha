package com.owspfm.elwha.sidesheet;

/**
 * Why a modal {@link ElwhaSideSheet} closed — reported through {@link
 * ElwhaSideSheet#setOnClose(java.util.function.Consumer)} after teardown. The sheet's own cause
 * vocabulary, distinct from the dialog's {@code DismissCause} ({@code CONFIRM}/{@code CANCEL} are
 * dialog-action semantics a sheet doesn't have — footer actions on a sheet are consumer-owned and
 * do not auto-dismiss; design doc §5/§9). Mirrors the menu's own-enum precedent.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public enum SheetDismissCause {

  /**
   * The header's close icon button was activated.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  CLOSE_AFFORDANCE,

  /**
   * The header's back icon button was activated with no {@link ElwhaSideSheet#setOnBack(Runnable)}
   * handler installed (its default behavior dismisses the sheet).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  BACK_AFFORDANCE,

  /**
   * The scrim was clicked while {@link ElwhaSideSheet#isDismissibleByScrim()} was {@code true}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  SCRIM,

  /**
   * The Escape key was pressed while {@link ElwhaSideSheet#isDismissibleByEsc()} was {@code true}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  ESC,

  /**
   * {@link ElwhaSideSheet#dismiss()} was called.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  PROGRAMMATIC
}
