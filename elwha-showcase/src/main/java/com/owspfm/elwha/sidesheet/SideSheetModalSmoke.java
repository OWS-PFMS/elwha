package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ElwhaLayers;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * Smoke for the modal {@link ElwhaSideSheet} presentation (#464). Headless section: modal config
 * defaults, the not-shown no-op contract, and the unrealized-parent failure mode. With a display it
 * additionally mounts the real overlay on a frame and asserts: the z-band ({@code
 * ElwhaLayers.OVERLAY_LAYER} = 190, scrim + surface), the type force on {@code showModal}, every
 * programmatic dismiss cause (Esc binding, scrim press, close/back affordances, {@code dismiss()}),
 * the Esc/scrim dismissibility toggles, single-fire {@code onClose}, and clean detach after
 * teardown. Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetModalSmoke {

  private static final int TIMEOUT_MS = 4000;

  private SideSheetModalSmoke() {}

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

    checkHeadlessContract();

    if (GraphicsEnvironment.isHeadless()) {
      System.out.println(
          "SideSheetModalSmoke: OK (headless contract; live overlay checks skipped — no display)");
      return;
    }
    checkLiveOverlay();
    System.out.println(
        "SideSheetModalSmoke: OK (headless contract; live z-band/scrim/type-force, all five"
            + " dismiss causes, dismissibility toggles, single-fire onClose, clean detach)");
  }

  private static void checkHeadlessContract() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Modal");
    check("esc dismissible by default", sheet.isDismissibleByEsc());
    check("scrim dismissible by default", sheet.isDismissibleByScrim());
    check("not modally shown initially", !sheet.isModalShowing());
    sheet.dismiss();
    check("dismiss when not shown is a no-op", !sheet.isModalShowing());

    boolean threw = false;
    try {
      sheet.showModal(new JButton("unrealized"));
    } catch (final IllegalStateException expected) {
      threw = true;
    }
    check("showModal on an unrealized parent throws IllegalStateException", threw);
    check("failed show leaves the sheet unshown", !sheet.isModalShowing());
  }

  private static void checkLiveOverlay() throws Exception {
    final JFrame frame = new JFrame("SideSheetModalSmoke");
    final AtomicReference<SheetDismissCause> lastCause = new AtomicReference<>();
    final int[] closeFires = {0};
    try {
      final JButton mainButton = new JButton("main");
      frame.add(mainButton);
      onEdt(
          () -> {
            frame.setSize(900, 600);
            frame.setLocation(40, 40);
            frame.setVisible(true);
          });

      final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Live modal");
      final JButton inContent = new JButton("content focus target");
      sheet.setContent(inContent);
      sheet.setOnClose(
          cause -> {
            lastCause.set(cause);
            closeFires[0]++;
          });

      // --- show: type force, z-band, scrim, attach
      onEdt(() -> sheet.showModal(frame.getContentPane()));
      check("showModal forces MODAL type", sheet.getSheetType() == SheetType.MODAL);
      check("modal showing", sheet.isModalShowing());
      final JLayeredPane lp = frame.getRootPane().getLayeredPane();
      check("scrim + surface mounted at OVERLAY_LAYER (190)", overlayBandCount(lp) == 2);

      // --- live width: a shown sheet re-docks at the new width without a re-show
      onEdt(() -> sheet.setSheetWidth(320));
      check("live setSheetWidth re-docks the shown surface", surfaceWidth(lp) == 320);
      onEdt(() -> sheet.setSheetWidth(256));
      check("live width change back re-docks again", surfaceWidth(lp) == 256);

      // --- Esc honored live: disabled → no-op; enabled → ESC cause
      onEdt(() -> sheet.setDismissibleByEsc(false));
      onEdt(() -> fireEscBinding(lp));
      Thread.sleep(150);
      check("Esc ignored while not dismissible", sheet.isModalShowing());
      onEdt(() -> sheet.setDismissibleByEsc(true));
      onEdt(() -> fireEscBinding(lp));
      // Poll on the onClose-recorded cause, not isModalShowing(): teardown clears the live state
      // (so isModalShowing flips false) a few statements BEFORE onClosed() relays the cause —
      // polling the earlier signal races the cause assertion (flaked once on a loaded machine).
      // The cause landing is the authoritative end of a dismissal.
      waitFor(
          "Esc dismisses with ESC cause",
          () -> lastCause.get() == SheetDismissCause.ESC && closeFires[0] == 1);
      check("layered pane clean after teardown", overlayBandCount(lp) == 0);

      // --- scrim press: blocked toggle, then SCRIM cause
      onEdt(() -> sheet.showModal(frame.getContentPane()));
      onEdt(() -> sheet.setDismissibleByScrim(false));
      onEdt(() -> pressScrim(lp));
      Thread.sleep(150);
      check("scrim press ignored while not dismissible", sheet.isModalShowing());
      onEdt(() -> sheet.setDismissibleByScrim(true));
      onEdt(() -> pressScrim(lp));
      waitFor(
          "scrim press dismisses with SCRIM cause",
          () -> lastCause.get() == SheetDismissCause.SCRIM && closeFires[0] == 2);

      // --- close affordance
      onEdt(() -> sheet.showModal(frame.getContentPane()));
      onEdt(sheet::onCloseActivated);
      waitFor(
          "close affordance dismisses with CLOSE_AFFORDANCE cause",
          () -> lastCause.get() == SheetDismissCause.CLOSE_AFFORDANCE);

      // --- back affordance: onBack overrides, default dismisses
      final boolean[] backRan = {false};
      sheet.setOnBack(() -> backRan[0] = true);
      onEdt(() -> sheet.showModal(frame.getContentPane()));
      onEdt(sheet::onBackActivated);
      Thread.sleep(150);
      check("onBack handler overrides the dismiss default", backRan[0] && sheet.isModalShowing());
      sheet.setOnBack(null);
      onEdt(sheet::onBackActivated);
      waitFor(
          "back affordance dismisses with BACK_AFFORDANCE cause",
          () -> lastCause.get() == SheetDismissCause.BACK_AFFORDANCE);

      // --- programmatic + re-show no-op
      onEdt(() -> sheet.showModal(frame.getContentPane()));
      onEdt(() -> sheet.showModal(frame.getContentPane()));
      check("re-show while shown is a no-op", overlayBandCount(lp) == 2);
      onEdt(sheet::dismiss);
      waitFor(
          "dismiss() tears down with PROGRAMMATIC cause",
          () -> lastCause.get() == SheetDismissCause.PROGRAMMATIC);
      check("layered pane clean at the end", overlayBandCount(lp) == 0);
    } finally {
      onEdt(frame::dispose);
    }
  }

  private static int surfaceWidth(final JLayeredPane lp) {
    for (final Component c : lp.getComponents()) {
      if (lp.getLayer(c) == ElwhaLayers.OVERLAY_LAYER
          && c instanceof javax.swing.JComponent jc
          && jc.getActionMap().get("elwha-sidesheet-esc") != null) {
        return c.getWidth();
      }
    }
    return -1;
  }

  private static int overlayBandCount(final JLayeredPane lp) {
    int count = 0;
    for (final Component c : lp.getComponents()) {
      if (lp.getLayer(c) == ElwhaLayers.OVERLAY_LAYER) {
        count++;
      }
    }
    return count;
  }

  // Fires the surface's Esc binding directly — the binding is registered on the slide surface at
  // OVERLAY_LAYER; dispatching a real key event would race the window focus state on CI desktops.
  private static void fireEscBinding(final JLayeredPane lp) {
    for (final Component c : lp.getComponents()) {
      if (lp.getLayer(c) == ElwhaLayers.OVERLAY_LAYER && c instanceof javax.swing.JComponent jc) {
        final javax.swing.Action esc = jc.getActionMap().get("elwha-sidesheet-esc");
        if (esc != null) {
          esc.actionPerformed(new ActionEvent(jc, ActionEvent.ACTION_PERFORMED, "esc"));
          return;
        }
      }
    }
  }

  private static void pressScrim(final JLayeredPane lp) {
    for (final Component c : lp.getComponents()) {
      if (lp.getLayer(c) == ElwhaLayers.OVERLAY_LAYER
          && c instanceof javax.swing.JComponent jc
          && jc.getActionMap().get("elwha-sidesheet-esc") == null) {
        for (final java.awt.event.MouseListener l : jc.getMouseListeners()) {
          l.mousePressed(
              new java.awt.event.MouseEvent(
                  jc, java.awt.event.MouseEvent.MOUSE_PRESSED, 0, 0, 5, 5, 1, false));
        }
        return;
      }
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
