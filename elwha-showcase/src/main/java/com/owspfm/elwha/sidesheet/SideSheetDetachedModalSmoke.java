package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ElwhaLayers;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * S2 guard for the detached modal {@link ElwhaSideSheet} presentation (#509). Headless section: the
 * footprint width (sheet width + 2·margin) the modal host docks. With a display it mounts the real
 * overlay and asserts the slide band is the footprint, flush to the resolved edge (trailing →
 * right, leading → left), that the sheet's own margin floats the body, and that a live posture
 * change re-docks the shown surface at the new footprint.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetDetachedModalSmoke {

  private static final int M = ElwhaSideSheet.DETACHED_MARGIN_PX;
  private static final int BODY = ElwhaSideSheet.SHEET_WIDTH_PX;

  private SideSheetDetachedModalSmoke() {}

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

    final ElwhaSideSheet headless = ElwhaSideSheet.modalSheet("Footprint");
    headless.setSheetPosture(SheetPosture.DETACHED);
    check(
        "detached modal footprint = width + 2*margin",
        headless.modalFootprintWidth() == BODY + 2 * M);

    if (GraphicsEnvironment.isHeadless()) {
      System.out.println(
          "SideSheetDetachedModalSmoke: OK (footprint contract; live placement skipped — no"
              + " display)");
      return;
    }
    checkLivePlacement();
    System.out.println(
        "SideSheetDetachedModalSmoke: OK (footprint contract; live detached band width + flush"
            + " placement both edges, body float, live posture re-dock)");
  }

  private static void checkLivePlacement() throws Exception {
    final JFrame frame = new JFrame("SideSheetDetachedModalSmoke");
    try {
      onEdt(
          () -> {
            frame.setSize(900, 600);
            frame.setLocation(40, 40);
            frame.setVisible(true);
          });
      final JLayeredPane lp = frame.getRootPane().getLayeredPane();

      // Trailing detached: footprint-wide band flush to the right edge.
      final ElwhaSideSheet trailing = ElwhaSideSheet.modalSheet("Trailing");
      trailing.setSheetPosture(SheetPosture.DETACHED);
      onEdt(() -> trailing.showModal(frame.getContentPane()));
      final Rectangle tb = surfaceBounds(lp);
      check("detached trailing band width = footprint", tb.width == BODY + 2 * M);
      check("detached trailing band flush to the right edge", tb.x + tb.width == lp.getWidth());
      check("detached sheet's own margin floats the body", trailing.getInsets().left == M);

      // Live posture change re-docks at the new footprint.
      onEdt(() -> trailing.setSheetPosture(SheetPosture.DOCKED));
      check("posture->DOCKED re-docks at the plain sheet width", surfaceBounds(lp).width == BODY);
      onEdt(trailing::dismiss);
      Thread.sleep(120);

      // Leading detached: footprint-wide band flush to the left edge.
      final ElwhaSideSheet leading = ElwhaSideSheet.modalSheet("Leading");
      leading.setSheetPosture(SheetPosture.DETACHED);
      leading.setSheetEdge(SheetEdge.LEADING);
      onEdt(() -> leading.showModal(frame.getContentPane()));
      final Rectangle lb = surfaceBounds(lp);
      check("detached leading band width = footprint", lb.width == BODY + 2 * M);
      check("detached leading band flush to the left edge", lb.x == 0);
      onEdt(leading::dismiss);
    } finally {
      onEdt(frame::dispose);
    }
  }

  private static Rectangle surfaceBounds(final JLayeredPane lp) {
    for (final Component c : lp.getComponents()) {
      if (lp.getLayer(c) == ElwhaLayers.OVERLAY_LAYER
          && c instanceof JComponent jc
          && jc.getActionMap().get("elwha-sidesheet-esc") != null) {
        return c.getBounds();
      }
    }
    return new Rectangle(-1, -1, -1, -1);
  }

  private static void onEdt(final Runnable body) throws Exception {
    SwingUtilities.invokeAndWait(body);
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
  }
}
