package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.function.BooleanSupplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S2 headless guard for the standard {@link ElwhaSideSheet} presentation (#463): open/close target
 * semantics, the animated preferred-width collapse 256→0→256 (driven by the live {@code
 * MorphAnimator} timer), the stays-in-hierarchy contract, sibling reflow under a validated
 * BorderLayout host, and the mid-flight body pinning that keeps children at their open-width
 * layout.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetStandardSmoke {

  private static final int TIMEOUT_MS = 3000;

  private SideSheetStandardSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @throws Exception on EDT plumbing failures
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkOpenCloseContract();
    checkReflowInBorderLayoutHost();
    checkMidFlightBodyPinning();

    System.out.println(
        "SideSheetStandardSmoke: OK (open/close targets, animated width collapse + reopen,"
            + " hierarchy retention, sibling reflow, mid-flight body pinning)");
  }

  private static void checkOpenCloseContract() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    check("constructed open", sheet.isOpen());
    check("open preferred width is the sheet width", prefWidth(sheet) == 256);

    onEdt(() -> sheet.setOpen(false));
    check("isOpen flips immediately", !sheet.isOpen());
    waitFor("width collapses to 0", () -> prefWidth(sheet) == 0);

    onEdt(sheet::open);
    check("reopen flips the target", sheet.isOpen());
    waitFor("width animates back to 256", () -> prefWidth(sheet) == 256);

    onEdt(() -> sheet.setOpen(true));
    check("redundant setOpen is a no-op", sheet.isOpen() && prefWidth(sheet) == 256);

    onEdt(sheet::close);
    waitFor("close() collapses", () -> prefWidth(sheet) == 0);
    onEdt(() -> sheet.onCloseActivated());
    check("close affordance on a closed sheet stays closed", !sheet.isOpen());
  }

  private static void checkReflowInBorderLayoutHost() throws Exception {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Sheet");
    final JLabel center = new JLabel("main");
    final JPanel host = new JPanel(new BorderLayout());
    host.add(center, BorderLayout.CENTER);
    host.add(sheet, BorderLayout.LINE_END);
    host.setSize(800, 400);
    onEdt(host::doLayout);

    check("open sheet occupies its width", sheet.getWidth() == 256);
    check("center yields to the sheet", center.getWidth() == 800 - 256);

    onEdt(() -> sheet.setOpen(false));
    waitFor(
        "center reclaims the full host width after close",
        () -> {
          onEdtQuiet(host::doLayout);
          return center.getWidth() == 800 && sheet.getWidth() == 0;
        });
    check("closed sheet stays in the hierarchy", sheet.getParent() == host);

    onEdt(() -> sheet.setOpen(true));
    waitFor(
        "center yields again after reopen",
        () -> {
          onEdtQuiet(host::doLayout);
          return center.getWidth() == 800 - 256 && sheet.getWidth() == 256;
        });
  }

  private static void checkMidFlightBodyPinning() throws Exception {
    final ElwhaSideSheet trailing = ElwhaSideSheet.standardSheet("Trailing");
    onEdt(() -> trailing.setOpen(false));
    waitFor("collapse settles", () -> prefWidth(trailing) == 0);
    onEdt(() -> trailing.open());
    // Mid-flight (progress < 1) the body must stay pinned at the full sheet width so children
    // keep their open-width layout; for a right-docked sheet the visible part is the leading edge.
    onEdt(
        () -> {
          trailing.setSize(120, 300);
          trailing.doLayout();
        });
    final Component body = trailing.getComponents()[0];
    if (prefWidth(trailing) < 256) {
      check("mid-flight body pinned to full width", body.getWidth() == 256);
      check("right-docked body pinned at the leading edge", body.getX() == 0);
    }
    waitFor("reopen settles", () -> prefWidth(trailing) == 256);

    final ElwhaSideSheet leading = ElwhaSideSheet.standardSheet("Leading");
    leading.setSheetEdge(SheetEdge.LEADING);
    onEdt(() -> leading.setOpen(false));
    waitFor("leading collapse settles", () -> prefWidth(leading) == 0);
    onEdt(leading::open);
    onEdt(
        () -> {
          leading.setSize(120, 300);
          leading.doLayout();
        });
    final Component leadingBody = leading.getComponents()[0];
    if (prefWidth(leading) < 256) {
      check(
          "left-docked body pinned at the trailing edge (clip on the window side)",
          leadingBody.getX() == 120 - 256);
    }
    waitFor("leading reopen settles", () -> prefWidth(leading) == 256);
  }

  private static int prefWidth(final ElwhaSideSheet sheet) {
    return sheet.getPreferredSize().width;
  }

  private static void onEdt(final Runnable body) throws Exception {
    SwingUtilities.invokeAndWait(body);
  }

  private static void onEdtQuiet(final Runnable body) {
    try {
      SwingUtilities.invokeAndWait(body);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
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
