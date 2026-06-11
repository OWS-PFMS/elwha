package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ElwhaLayers;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.function.BooleanSupplier;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * Smoke for {@link ElwhaSideSheet} edge anchoring + RTL (#465). Headless section: the four-way
 * resolved-dock truth table (edge × orientation) and the RTL header mirroring (back affordance
 * leading, close trailing, flipped under RTL). With a display it additionally opens the modal
 * presentation in all four combos and asserts the surface docks against the resolved window side.
 * Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetEdgeRtlSmoke {

  private static final int TIMEOUT_MS = 4000;

  private SideSheetEdgeRtlSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @throws Exception on EDT plumbing failures
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkDockTruthTable();
    checkHeaderMirroring();

    if (GraphicsEnvironment.isHeadless()) {
      System.out.println(
          "SideSheetEdgeRtlSmoke: OK (dock truth table, header mirroring; live docking checks"
              + " skipped — no display)");
      return;
    }
    checkLiveModalDocking();
    System.out.println(
        "SideSheetEdgeRtlSmoke: OK (dock truth table, header mirroring, live 4-combo modal"
            + " docking)");
  }

  private static void checkDockTruthTable() {
    check("TRAILING + LTR docks right", docked(SheetEdge.TRAILING, false));
    check("LEADING + LTR docks left", !docked(SheetEdge.LEADING, false));
    check("TRAILING + RTL docks left", !docked(SheetEdge.TRAILING, true));
    check("LEADING + RTL docks right", docked(SheetEdge.LEADING, true));
  }

  private static boolean docked(final SheetEdge edge, final boolean rtl) {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("t");
    sheet.setSheetEdge(edge);
    sheet.applyComponentOrientation(
        rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
    return sheet.isDockedRight();
  }

  private static void checkHeaderMirroring() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Mirror");
    sheet.setBackAffordanceVisible(true);
    sheet.setSize(256, 400);
    layoutTree(sheet);
    final Component back = findByName(sheet, "Back");
    final Component close = findByName(sheet, "Close");
    check("LTR: back is leading (left of close)", screenX(back, sheet) < screenX(close, sheet));

    sheet.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    layoutTree(sheet);
    check("RTL: back mirrors to the right of close", screenX(back, sheet) > screenX(close, sheet));
  }

  // Lays out the unrealized tree manually — Container.validate() is a no-op without a peer.
  private static void layoutTree(final Container root) {
    root.doLayout();
    for (final Component child : root.getComponents()) {
      if (child instanceof Container nested) {
        layoutTree(nested);
      }
    }
  }

  private static int screenX(final Component c, final Component root) {
    int x = 0;
    for (Component walk = c; walk != null && walk != root; walk = walk.getParent()) {
      x += walk.getX();
    }
    return x;
  }

  private static Component findByName(final Container root, final String accessibleName) {
    for (final Component child : root.getComponents()) {
      if (accessibleName.equals(child.getAccessibleContext().getAccessibleName())) {
        return child;
      }
      if (child instanceof Container nested) {
        final Component found = findByName(nested, accessibleName);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void checkLiveModalDocking() throws Exception {
    final JFrame frame = new JFrame("SideSheetEdgeRtlSmoke");
    try {
      onEdt(
          () -> {
            frame.getContentPane().add(new javax.swing.JButton("main"));
            frame.setSize(900, 600);
            frame.setLocation(40, 40);
            frame.setVisible(true);
          });
      checkDocking(frame, SheetEdge.TRAILING, false, true);
      checkDocking(frame, SheetEdge.LEADING, false, false);
      checkDocking(frame, SheetEdge.TRAILING, true, false);
      checkDocking(frame, SheetEdge.LEADING, true, true);
    } finally {
      onEdt(frame::dispose);
    }
  }

  private static void checkDocking(
      final JFrame frame, final SheetEdge edge, final boolean rtl, final boolean expectRight)
      throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet(edge + (rtl ? " RTL" : " LTR"));
    sheet.setSheetEdge(edge);
    onEdt(
        () -> {
          frame
              .getContentPane()
              .applyComponentOrientation(
                  rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
          sheet.showModal(frame.getContentPane());
        });
    final JLayeredPane lp = frame.getRootPane().getLayeredPane();
    final Component surface = surfaceAtOverlayBand(lp);
    check(edge + (rtl ? "+RTL" : "+LTR") + ": surface mounted", surface != null);
    final int mid = surface.getX() + surface.getWidth() / 2;
    final boolean onRight = mid > lp.getWidth() / 2;
    check(
        edge + (rtl ? "+RTL" : "+LTR") + " docks " + (expectRight ? "right" : "left"),
        onRight == expectRight);
    onEdt(sheet::dismiss);
    waitFor("teardown settles", () -> !sheet.isModalShowing());
  }

  private static Component surfaceAtOverlayBand(final JLayeredPane lp) {
    for (final Component c : lp.getComponents()) {
      if (lp.getLayer(c) == ElwhaLayers.OVERLAY_LAYER
          && c instanceof javax.swing.JComponent jc
          && jc.getActionMap().get("elwha-sidesheet-esc") != null) {
        return c;
      }
    }
    return null;
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
