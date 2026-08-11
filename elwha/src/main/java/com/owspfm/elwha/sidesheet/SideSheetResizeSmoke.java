package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Cursor;

/**
 * S4 headless guard for drag-to-resize (#511): the opt-in default, the width bounds and their
 * clamping, the min/max setters' clamping of the current width, and the resize strip's geometry —
 * an 8px hot zone with a horizontal-resize cursor on the content-facing edge, mirrored across
 * edges.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetResizeSmoke {

  private SideSheetResizeSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkDefaultsAndClamp();
    checkStripGeometry();

    System.out.println(
        "SideSheetResizeSmoke: OK (opt-in default, width bounds + clamping, min/max setter clamp,"
            + " strip geometry + resize cursor both edges)");
  }

  private static void checkDefaultsAndClamp() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Resize");
    check("resizable off by default", !sheet.isResizable());
    check("default min width 200", sheet.getMinSheetWidth() == 200);
    check("default max width 600", sheet.getMaxSheetWidth() == 600);
    sheet.setResizable(true);
    check("resizable setter", sheet.isResizable());

    sheet.resizeWidthTo(333);
    check("resize within bounds applies", sheet.getSheetWidth() == 333);
    sheet.resizeWidthTo(50);
    check("resize below min clamps to min", sheet.getSheetWidth() == 200);
    sheet.resizeWidthTo(10_000);
    check("resize above max clamps to max", sheet.getSheetWidth() == 600);

    // Raising the min above the current width grows the sheet.
    sheet.resizeWidthTo(300);
    sheet.setMinSheetWidth(400);
    check("raising min grows the sheet to the new minimum", sheet.getSheetWidth() == 400);
    // Lowering the max below the min pins the max at the min (never inverts).
    sheet.setMaxSheetWidth(300);
    check("max never drops below min", sheet.getMaxSheetWidth() == 400);
    // A subsequent over-max resize clamps to the (now equal) bound.
    sheet.resizeWidthTo(9999);
    check("resize clamps to the pinned max", sheet.getSheetWidth() == 400);
  }

  private static void checkStripGeometry() {
    final ElwhaSideSheet trailing = ElwhaSideSheet.standardSheet("Trailing");
    trailing.setResizable(true);
    trailing.setSize(256, 400);
    trailing.doLayout();
    final Component strip = trailing.getComponent(0);
    check("resize strip is the topmost child", strip.isVisible() && strip.getWidth() == 8);
    check("trailing (dock-right) strip on the left content-facing edge", strip.getX() == 0);
    check(
        "trailing strip shows a horizontal-resize cursor",
        strip.getCursor().getType() == Cursor.W_RESIZE_CURSOR);

    final ElwhaSideSheet leading = ElwhaSideSheet.standardSheet("Leading");
    leading.setResizable(true);
    leading.setSheetEdge(SheetEdge.LEADING);
    leading.setSize(256, 400);
    leading.doLayout();
    final Component leadStrip = leading.getComponent(0);
    check(
        "leading (dock-left) strip on the right content-facing edge", leadStrip.getX() == 256 - 8);
    check(
        "leading strip shows the mirrored resize cursor",
        leadStrip.getCursor().getType() == Cursor.E_RESIZE_CURSOR);

    final ElwhaSideSheet off = ElwhaSideSheet.standardSheet("Off");
    off.setSize(256, 400);
    off.doLayout();
    check("strip hidden when not resizable", !off.getComponent(0).isVisible());
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
  }
}
