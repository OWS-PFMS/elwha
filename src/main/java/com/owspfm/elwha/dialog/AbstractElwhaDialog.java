package com.owspfm.elwha.dialog;

import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

/**
 * Package-private base for Elwha's in-window <em>modal</em> dialog primitives — {@link ElwhaDialog}
 * (the M3 Basic Dialog) and {@link ElwhaFullScreenDialog} (the M3 Full-screen Dialog). A thin
 * modal-flavored specialization of {@link AbstractElwhaOverlay}: it pins the overlay to the
 * modal-scrim + centered strategy ({@link #lightDismiss()} {@code false}, {@link #overlayLayer()}
 * {@link JLayeredPane#MODAL_LAYER}, a focus trap rather than light-dismiss) and adds the {@link
 * DismissCause} reporting the two dialogs share.
 *
 * <p><strong>Extraction provenance.</strong> The mount/focus/motion/teardown machinery once owned
 * here was lifted into {@link AbstractElwhaOverlay} in epic #298 S1 so the M3 Menu (#298) and the
 * pending side-sheet (#308) reuse it. This class now contributes only the modal posture and the
 * dismiss-cause plumbing; the lifecycle lives in the base. Subclasses are unaffected — the abstract
 * anatomy hooks and the {@code dismiss(...)} / {@code isDismissibleByEsc()} surface are unchanged.
 *
 * <p><strong>Modality mechanism.</strong> The dialog is an overlay installed on {@code
 * SwingUtilities.getRootPane(parent).getLayeredPane()} at {@link JLayeredPane#MODAL_LAYER} rather
 * than a separate {@link java.awt.Window}: a subclass-supplied backdrop beneath the surface (a scrim
 * for the Basic Dialog; nothing for the frame-filling Full-screen Dialog) plus the surface on top.
 * Input is blocked by the backdrop's event-consumer and/or the surface physically covering the
 * content, backed by the base's keyboard focus trap. Reuses the {@code ElwhaFabAnchor} (#205)
 * layered-pane glue.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.3.0
 */
abstract class AbstractElwhaDialog extends AbstractElwhaOverlay {

  private final boolean dismissibleByEsc;
  private final Consumer<DismissCause> onClose;

  private DismissCause exitCause;

  /**
   * @param dismissibleByEsc whether Escape dismisses the dialog
   * @param onClose the close hook fired after teardown, or {@code null} for none
   */
  protected AbstractElwhaDialog(
      final boolean dismissibleByEsc, final Consumer<DismissCause> onClose) {
    this.dismissibleByEsc = dismissibleByEsc;
    this.onClose = onClose;
  }

  // ------------------------------------------------ modal anatomy hooks

  /**
   * Creates the full-bounds backdrop painted beneath the surface, or {@code null} for none (the
   * Full-screen Dialog fills the frame and needs no scrim).
   *
   * @return the backdrop component, or {@code null}
   */
  @Override
  protected abstract JComponent createBackdrop();

  /** Installs Esc / Enter key bindings on the surface's input/action maps. */
  @Override
  protected abstract void installKeyBindings();

  // ----------------------------------------------------------- lifecycle

  /**
   * Closes the dialog programmatically (cancel-equivalent), reporting {@link
   * DismissCause#PROGRAMMATIC}. A no-op if not currently shown.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public final void dismiss() {
    dismiss(DismissCause.PROGRAMMATIC);
  }

  /**
   * Begins closing with the given cause. Re-entry guarded by the base so an action listener that
   * also calls {@code dismiss(...)} — or a backdrop click mid-exit — can't double-fire.
   *
   * @param cause the dismiss cause to report through {@code onClose}
   */
  final void dismiss(final DismissCause cause) {
    if (isClosing() || !isShowing()) {
      return;
    }
    exitCause = cause;
    beginClose();
  }

  // The base teardown fires this once the overlay is fully detached and focus restored; relay the
  // recorded cause to the consumer's onClose hook.
  @Override
  protected final void onClosed() {
    if (onClose != null) {
      onClose.accept(exitCause);
    }
  }

  /**
   * @return whether the Escape key dismisses this dialog (set at build time)
   */
  protected final boolean isDismissibleByEsc() {
    return dismissibleByEsc;
  }

  /**
   * The preferred initial-focus target; {@code null} falls through to the first focusable descendant
   * of the surface, then to the surface itself.
   *
   * @return the preferred focus target, or {@code null}
   */
  @Override
  protected Component initialFocusTarget() {
    return null;
  }
}
