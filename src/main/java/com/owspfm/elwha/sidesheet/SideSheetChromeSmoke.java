package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

/**
 * S1 headless guard for {@link ElwhaSideSheet} static chrome (#459): API defaults and slot
 * semantics, the new {@code arrowBack} glyph, and pixel-asserted type-derived chrome — standard
 * SURFACE fill + edge divider vs modal SURFACE_CONTAINER_LOW fill + content-facing corner rounding
 * — in light and dark modes, both docked edges.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetChromeSmoke {

  private static final int W = 256;
  private static final int H = 400;

  private SideSheetChromeSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkApiDefaults();
    checkSlots();
    checkIcon();
    checkChromePixels();
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    checkChromePixels();

    System.out.println(
        "SideSheetChromeSmoke: OK (API defaults, slots, arrowBack glyph, light+dark"
            + " standard/modal chrome pixels, both edges)");
  }

  private static void checkApiDefaults() {
    final ElwhaSideSheet sheet = new ElwhaSideSheet("Title");
    check("convenience ctor is STANDARD", sheet.getSheetType() == SheetType.STANDARD);
    check("default edge TRAILING", sheet.getSheetEdge() == SheetEdge.TRAILING);
    check("default width 256", sheet.getSheetWidth() == ElwhaSideSheet.SHEET_WIDTH_PX);
    check("headline getter", "Title".equals(sheet.getHeadline()));
    check("close affordance default on", sheet.isCloseAffordanceVisible());
    check("back affordance default off", !sheet.isBackAffordanceVisible());
    check("edge divider default on", sheet.isEdgeDividerVisible());
    check("footer divider default on", sheet.isFooterDividerVisible());
    check("no content by default", sheet.getContent() == null);
    check("no actions by default", sheet.getActions().isEmpty());

    check("modal factory type", ElwhaSideSheet.modalSheet("M").getSheetType() == SheetType.MODAL);
    check(
        "standard factory type",
        ElwhaSideSheet.standardSheet("S").getSheetType() == SheetType.STANDARD);

    check(
        "accessible name tracks headline",
        "Title".equals(sheet.getAccessibleContext().getAccessibleName()));
    sheet.setHeadline("Renamed");
    check(
        "accessible name follows setHeadline",
        "Renamed".equals(sheet.getAccessibleContext().getAccessibleName()));

    final Insets standardReserve = sheet.getShadowInsets();
    check(
        "standard sheet has no shadow reserve",
        standardReserve.top == 0
            && standardReserve.left == 0
            && standardReserve.bottom == 0
            && standardReserve.right == 0);
    sheet.setSheetType(SheetType.MODAL);
    final Insets modalReserve = sheet.getShadowInsets();
    check(
        "modal sheet reserves shadow halo",
        modalReserve.top > 0 || modalReserve.left > 0 || modalReserve.bottom > 0);
    check(
        "preferred width = sheet width + reserve",
        sheet.getPreferredSize().width
            == ElwhaSideSheet.SHEET_WIDTH_PX + modalReserve.left + modalReserve.right);

    sheet.setSheetWidth(-10);
    check("negative width clamps to 0", sheet.getSheetWidth() == 0);
    sheet.setSheetWidth(320);
    check("width setter", sheet.getSheetWidth() == 320);
  }

  private static void checkSlots() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Slots");
    final javax.swing.JLabel content = new javax.swing.JLabel("content");
    sheet.setContent(content);
    check("content getter", sheet.getContent() == content);
    final javax.swing.JLabel replacement = new javax.swing.JLabel("replacement");
    sheet.setContent(replacement);
    check("content replaced", sheet.getContent() == replacement);
    check("old content detached", content.getParent() == null);
    sheet.setContent(null);
    check("content cleared", sheet.getContent() == null && replacement.getParent() == null);

    final ElwhaButton save = ElwhaButton.filledButton("Save");
    final ElwhaButton cancel = ElwhaButton.outlinedButton("Cancel");
    sheet.setActions(save, cancel);
    check("actions getter", sheet.getActions().size() == 2);
    check("actions order leading-first", sheet.getActions().get(0) == save);
    sheet.setActions();
    check("empty actions clears footer", sheet.getActions().isEmpty());

    final boolean[] ran = {false};
    sheet.setOnBack(() -> ran[0] = true);
    sheet.setBackAffordanceVisible(true);
    sheet.onBackActivated();
    check("back affordance runs onBack", ran[0]);
  }

  private static void checkIcon() {
    check("arrowBack glyph loads", MaterialIcons.arrowBack() != null);
    check("arrowBack sized overload", MaterialIcons.arrowBack(20).getIconWidth() == 20);
  }

  private static void checkChromePixels() {
    final BufferedImage standard = render(configured(SheetType.STANDARD, SheetEdge.TRAILING, true));
    final Color surface = ColorRole.SURFACE.resolve();
    check("standard fill is SURFACE", rgbEquals(standard, W / 2, H / 2, surface));
    check(
        "standard edge divider on the content-facing (left) edge",
        rgbEquals(standard, 0, H / 2, ColorRole.OUTLINE_VARIANT.resolve()));
    check(
        "standard square corner filled",
        rgbEquals(standard, 0, 0, ColorRole.OUTLINE_VARIANT.resolve()));

    final ElwhaSideSheet modalSheet = configured(SheetType.MODAL, SheetEdge.TRAILING, true);
    final Insets reserve = modalSheet.getShadowInsets();
    final int mw = W + reserve.left + reserve.right;
    final int mh = H + reserve.top + reserve.bottom;
    final BufferedImage modal = render(modalSheet, mw, mh);
    final Color low = ColorRole.SURFACE_CONTAINER_LOW.resolve();
    check("modal fill is SURFACE_CONTAINER_LOW", rgbEquals(modal, mw / 2, mh / 2, low));
    check(
        "modal content-facing top corner rounded (transparent at the corner)",
        alpha(modal, reserve.left, reserve.top) < 255);
    check(
        "modal window-edge top corner square (filled)",
        rgbEquals(modal, mw - reserve.right - 1, reserve.top, low));
    check(
        "modal content-facing bottom corner rounded",
        alpha(modal, reserve.left, mh - reserve.bottom - 1) < 255);

    final ElwhaSideSheet leading = configured(SheetType.MODAL, SheetEdge.LEADING, true);
    final BufferedImage leadingImg = render(leading, mw, mh);
    check(
        "leading-edge sheet rounds the trailing-side corners instead",
        alpha(leadingImg, mw - reserve.right - 1, reserve.top) < 255
            && rgbEquals(leadingImg, reserve.left, reserve.top, low));

    final ElwhaSideSheet rtl = configured(SheetType.STANDARD, SheetEdge.TRAILING, true);
    rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    final BufferedImage rtlImg = render(rtl);
    check(
        "RTL TRAILING standard sheet docks left: divider on its right edge",
        rgbEquals(rtlImg, W - 1, H / 2, ColorRole.OUTLINE_VARIANT.resolve()));

    final ElwhaSideSheet noDivider = configured(SheetType.STANDARD, SheetEdge.TRAILING, false);
    final BufferedImage noDividerImg = render(noDivider);
    check(
        "edge divider toggles off", rgbEquals(noDividerImg, 0, H / 2, ColorRole.SURFACE.resolve()));
  }

  private static ElwhaSideSheet configured(
      final SheetType type, final SheetEdge edge, final boolean edgeDivider) {
    final ElwhaSideSheet sheet =
        type == SheetType.MODAL
            ? ElwhaSideSheet.modalSheet("Chrome")
            : ElwhaSideSheet.standardSheet("Chrome");
    sheet.setSheetEdge(edge);
    sheet.setEdgeDividerVisible(edgeDivider);
    return sheet;
  }

  private static BufferedImage render(final ElwhaSideSheet sheet) {
    return render(sheet, W, H);
  }

  private static BufferedImage render(final ElwhaSideSheet sheet, final int w, final int h) {
    sheet.setSize(w, h);
    sheet.doLayout();
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      sheet.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean rgbEquals(
      final BufferedImage img, final int x, final int y, final Color expected) {
    final int rgb = img.getRGB(x, y);
    return (rgb >>> 24) == 255 && (rgb & 0xFFFFFF) == (expected.getRGB() & 0xFFFFFF);
  }

  private static int alpha(final BufferedImage img, final int x, final int y) {
    return img.getRGB(x, y) >>> 24;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
  }
}
