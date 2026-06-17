package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * S3 guard for drag-to-dismiss (#510). Headless section: the opt-in default, and the standard
 * narrow-scrub — a partial drag narrows the open width while leaving the sheet open, a release
 * under the threshold settles back to full width, and a release past it closes (driven
 * deterministically under reduced motion). With a display it shows a modal, scrubs its slide, and
 * asserts a release under the threshold keeps it up while a release past it tears down with {@link
 * SheetDismissCause#DRAG}.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetDragDismissSmoke {

  private static final int TIMEOUT_MS = 4000;
  private static final int BODY = ElwhaSideSheet.SHEET_WIDTH_PX;

  private SideSheetDragDismissSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @throws Exception on EDT plumbing failures
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) throws Exception {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkDefaultsAndStandardScrub();

    if (GraphicsEnvironment.isHeadless()) {
      System.out.println(
          "SideSheetDragDismissSmoke: OK (opt-in default, standard narrow-scrub; live modal DRAG"
              + " skipped — no display)");
      return;
    }
    checkLiveModalDrag();
    System.out.println(
        "SideSheetDragDismissSmoke: OK (opt-in default, standard narrow-scrub settle/dismiss, live"
            + " modal scrub + DRAG-cause dismiss + under-threshold settle)");
  }

  private static void checkDefaultsAndStandardScrub() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Drag");
    check("drag-to-dismiss off by default", !sheet.isDragToDismissEnabled());
    sheet.setDragToDismissEnabled(true);
    check("drag-to-dismiss setter", sheet.isDragToDismissEnabled());

    MorphAnimator.setReducedMotion(true);
    try {
      // A partial drag narrows the open width 1:1 but the sheet stays open.
      sheet.applyDragFraction(0.30f);
      final int narrowed = sheet.getPreferredSize().width;
      check("partial drag narrows the open width", narrowed > 0 && narrowed < BODY);
      check("partial drag leaves the sheet open", sheet.isOpen());

      // Release under the threshold settles back to full width.
      sheet.releaseDrag(0.30f);
      check("under-threshold release settles back open", sheet.isOpen());
      check("settled width is the full sheet width", sheet.getPreferredSize().width == BODY);

      // Release past the threshold closes.
      sheet.applyDragFraction(0.70f);
      sheet.releaseDrag(0.70f);
      check("past-threshold release closes the standard sheet", !sheet.isOpen());
      check("closed width is 0", sheet.getPreferredSize().width == 0);
    } finally {
      MorphAnimator.setReducedMotion(false);
    }
  }

  private static void checkLiveModalDrag() throws Exception {
    final JFrame frame = new JFrame("SideSheetDragDismissSmoke");
    final AtomicReference<SheetDismissCause> lastCause = new AtomicReference<>();
    try {
      onEdt(
          () -> {
            frame.setSize(900, 600);
            frame.setLocation(40, 40);
            frame.setVisible(true);
          });

      final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Drag");
      modal.setDragToDismissEnabled(true);
      modal.setOnClose(lastCause::set);

      // Scrub partway, release under the threshold → stays up.
      onEdt(() -> modal.showModal(frame.getContentPane()));
      check("modal shown", modal.isModalShowing());
      onEdt(() -> modal.applyDragFraction(0.30f));
      onEdt(() -> modal.releaseDrag(0.30f));
      Thread.sleep(450);
      check("under-threshold drag leaves the modal up", modal.isModalShowing());
      check("no dismiss cause fired on settle", lastCause.get() == null);

      // Scrub past the threshold, release → DRAG-cause teardown.
      onEdt(() -> modal.applyDragFraction(0.80f));
      onEdt(() -> modal.releaseDrag(0.80f));
      waitFor(
          "past-threshold drag dismisses with DRAG cause",
          () -> lastCause.get() == SheetDismissCause.DRAG && !modal.isModalShowing());
    } finally {
      onEdt(frame::dispose);
    }
  }

  private static void onEdt(final Runnable body) throws Exception {
    SwingUtilities.invokeAndWait(body);
  }

  private static void waitFor(final String what, final BooleanSupplier condition)
      throws InterruptedException {
    final long deadline = System.currentTimeMillis() + TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(20);
    }
    check(what + " (timed out)", condition.getAsBoolean());
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
  }
}
