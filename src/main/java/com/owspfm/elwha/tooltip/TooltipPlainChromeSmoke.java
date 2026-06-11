package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Headless guard for #447 — plain-tooltip wrap math, M3 minimum/maximum sizing, the pure placement
 * engine (above default, flip, clamp, alignment, RTL mirror, halo offset), token-correct paint in
 * both modes, and — when a display is available — the passive-focus contract end to end on a real
 * layered pane (show steals nothing, dismiss restores nothing, double-show is a no-op).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipPlainChromeSmoke {

  private static int checks;
  private static int failures;

  private TooltipPlainChromeSmoke() {}

  /**
   * Runs the guard; exits non-zero on any failure.
   *
   * @param args unused
   * @throws Exception on EDT plumbing failures
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    MorphAnimator.setReducedMotion(true);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    wrapChecks();
    sizeChecks();
    placementChecks();
    paintChecks();
    if (!GraphicsEnvironment.isHeadless()) {
      windowedChecks();
    } else {
      System.out.println("(headless — windowed passive-focus checks skipped)");
    }

    System.out.println(
        failures == 0 ? "PASS — " + checks + " checks" : "FAIL — " + failures + "/" + checks);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void wrapChecks() {
    final TooltipSurface probe = new TooltipSurface("x");
    final FontMetrics fm =
        probe.getFontMetrics(com.owspfm.elwha.theme.TypeRole.BODY_SMALL.resolve());
    final int wrapWidth = TooltipSurface.PLAIN_MAX_WIDTH_PX - 2 * TooltipSurface.PLAIN_H_PAD_PX;

    final List<String> one = TooltipSurface.wrap("Save", fm, wrapWidth);
    check(one.size() == 1 && one.get(0).equals("Save"), "short label stays one line");

    final List<String> many =
        TooltipSurface.wrap(
            "Plain tooltips wrap their label by hand at the two hundred pixel max width",
            fm,
            wrapWidth);
    boolean allFit = many.size() > 1;
    for (final String line : many) {
      allFit &= fm.stringWidth(line) <= wrapWidth;
    }
    check(allFit, "long label wraps and every line fits the wrap width");

    final List<String> hard = TooltipSurface.wrap("w".repeat(300), fm, wrapWidth);
    boolean hardFit = hard.size() > 1;
    for (final String line : hard) {
      hardFit &= fm.stringWidth(line) <= wrapWidth;
    }
    check(hardFit, "an over-long single word hard-breaks within the wrap width");

    check(
        TooltipSurface.wrap("a\nb", fm, wrapWidth).size() == 2, "explicit newline forces a break");
  }

  private static void sizeChecks() {
    final Dimension tiny = new TooltipSurface("OK").getPreferredSize();
    check(
        tiny.width >= TooltipSurface.MIN_WIDTH_PX && tiny.height >= TooltipSurface.MIN_HEIGHT_PX,
        "tiny label honors the 40x24 minimums (was " + tiny.width + "x" + tiny.height + ")");

    final Dimension wide =
        new TooltipSurface("a long plain tooltip label that must wrap rather than widen forever")
            .getPreferredSize();
    check(
        wide.width <= TooltipSurface.PLAIN_MAX_WIDTH_PX,
        "long label caps at the 200px max width (was " + wide.width + ")");
    final Dimension shortOne = new TooltipSurface("Save to favorites").getPreferredSize();
    check(
        shortOne.height >= TooltipSurface.MIN_HEIGHT_PX,
        "single-line height at least the 24px minimum");
  }

  private static void placementChecks() {
    final Rectangle anchor = new Rectangle(300, 300, 80, 40);
    final Dimension pref = new Dimension(100, 24);
    final Insets none = new Insets(0, 0, 0, 0);

    final Rectangle above =
        ElwhaTooltip.place(
            anchor, pref, none, 800, 600, TooltipPlacement.ABOVE, TooltipAlignment.CENTER, false);
    check(
        above.y + above.height + ElwhaTooltip.ANCHOR_GAP_PX == anchor.y,
        "ABOVE: body bottom sits 4px off the anchor top");
    check(
        above.x == anchor.x + (anchor.width - pref.width) / 2,
        "CENTER: body centers on the anchor");

    final Rectangle flipped =
        ElwhaTooltip.place(
            new Rectangle(300, 10, 80, 40),
            pref,
            none,
            800,
            600,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);
    check(
        flipped.y == 10 + 40 + ElwhaTooltip.ANCHOR_GAP_PX,
        "ABOVE flips BELOW when the top would clip");

    final Rectangle clamped =
        ElwhaTooltip.place(
            new Rectangle(0, 300, 40, 40),
            pref,
            none,
            800,
            600,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);
    check(clamped.x == ElwhaTooltip.EDGE_MARGIN_PX, "left-edge anchor clamps to the 8px margin");

    final Rectangle startLtr =
        ElwhaTooltip.place(
            anchor, pref, none, 800, 600, TooltipPlacement.ABOVE, TooltipAlignment.START, false);
    final Rectangle startRtl =
        ElwhaTooltip.place(
            anchor, pref, none, 800, 600, TooltipPlacement.ABOVE, TooltipAlignment.START, true);
    check(startLtr.x == anchor.x, "START aligns leading edges (LTR)");
    check(startRtl.x == anchor.x + anchor.width - pref.width, "START mirrors under RTL");

    final Rectangle endLtr =
        ElwhaTooltip.place(
            anchor, pref, none, 800, 600, TooltipPlacement.ABOVE, TooltipAlignment.END, false);
    check(endLtr.x == anchor.x + anchor.width - pref.width, "END aligns trailing edges (LTR)");

    final Insets halo = new Insets(10, 12, 14, 16);
    final Rectangle haloed =
        ElwhaTooltip.place(
            anchor,
            new Dimension(pref.width + 28, pref.height + 24),
            halo,
            800,
            600,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);
    check(
        haloed.x + halo.left == above.x && haloed.y + halo.top == above.y,
        "halo insets offset the surface so the body lands where the flat body did");
  }

  private static void paintChecks() {
    final BufferedImage lightImg = renderPlain("Save to favorites");
    final int fillLight = lightImg.getRGB(4, lightImg.getHeight() / 2);
    check(
        fillLight == ColorRole.INVERSE_SURFACE.resolve().getRGB(),
        "light-mode fill is INVERSE_SURFACE");
    check(hasNonFillPixel(lightImg), "light-mode label pixels painted over the fill");

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    final BufferedImage darkImg = renderPlain("Save to favorites");
    final int fillDark = darkImg.getRGB(4, darkImg.getHeight() / 2);
    check(
        fillDark == ColorRole.INVERSE_SURFACE.resolve().getRGB(),
        "dark-mode fill is INVERSE_SURFACE");
    check(fillDark != fillLight, "fill actually changes between modes");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
  }

  private static BufferedImage renderPlain(final String text) {
    final TooltipSurface surface = new TooltipSurface(text);
    final Dimension pref = surface.getPreferredSize();
    surface.setSize(pref);
    final BufferedImage img =
        new BufferedImage(pref.width, pref.height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = img.createGraphics();
    surface.paint(g2);
    g2.dispose();
    return img;
  }

  private static boolean hasNonFillPixel(final BufferedImage img) {
    final int fill = ColorRole.INVERSE_SURFACE.resolve().getRGB();
    for (int y = 4; y < img.getHeight() - 4; y++) {
      for (int x = 8; x < img.getWidth() - 8; x++) {
        if (img.getRGB(x, y) != fill) {
          return true;
        }
      }
    }
    return false;
  }

  private static void windowedChecks() throws Exception {
    final AtomicReference<JFrame> frameRef = new AtomicReference<>();
    final AtomicReference<JTextField> fieldRef = new AtomicReference<>();
    final AtomicReference<javax.swing.JButton> anchorRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("TooltipPlainChromeSmoke");
          final JTextField field = new JTextField("focus home", 16);
          final javax.swing.JButton anchorButton = new javax.swing.JButton("anchor");
          // GridBagLayout centers the row vertically so the ABOVE preference has room and the
          // placement assertion is deterministic (a top-of-frame anchor legitimately flips).
          final javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
          final javax.swing.JPanel row = new javax.swing.JPanel();
          row.add(field);
          row.add(anchorButton);
          panel.add(row);
          frame.add(panel);
          frame.setSize(500, 300);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
          field.requestFocusInWindow();
          frameRef.set(frame);
          fieldRef.set(field);
          anchorRef.set(anchorButton);
        });
    Thread.sleep(200);

    final ElwhaTooltip tip = ElwhaTooltip.plain("Passive focus");
    final AtomicReference<Component> ownerBefore = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          ownerBefore.set(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
          tip.show(anchorRef.get());
        });
    Thread.sleep(100);

    SwingUtilities.invokeAndWait(
        () -> {
          final Component ownerAfter =
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          check(ownerAfter == ownerBefore.get(), "show() steals no focus");
          check(tip.isTooltipShowing(), "tooltip reports showing");

          final JLayeredPane pane = frameRef.get().getRootPane().getLayeredPane();
          Component mounted = null;
          for (final Component c : pane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
            if (c instanceof TooltipSurface) {
              mounted = c;
            }
          }
          check(mounted != null, "surface mounts on POPUP_LAYER");
          if (mounted != null) {
            final Rectangle anchorInPane =
                SwingUtilities.convertRectangle(
                    anchorRef.get().getParent(), anchorRef.get().getBounds(), pane);
            check(
                mounted.getY() + mounted.getHeight() + ElwhaTooltip.ANCHOR_GAP_PX == anchorInPane.y,
                "surface placed 4px above the anchor in pane coordinates");
          }

          tip.show(anchorRef.get());
          check(
              pane.getComponentsInLayer(JLayeredPane.POPUP_LAYER).length == 1,
              "double-show is a no-op (one surface mounted)");

          tip.setText("A re-measured, much longer passive-focus tooltip label");
          check(tip.isTooltipShowing(), "setText while showing keeps it shown");

          tip.dismiss();
        });
    Thread.sleep(100);

    SwingUtilities.invokeAndWait(
        () -> {
          check(!tip.isTooltipShowing(), "dismiss tears down");
          final JLayeredPane pane = frameRef.get().getRootPane().getLayeredPane();
          check(
              pane.getComponentsInLayer(JLayeredPane.POPUP_LAYER).length == 0,
              "surface removed from the pane");
          final Component ownerEnd =
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          check(ownerEnd == ownerBefore.get(), "dismiss restores nothing — focus untouched");
          frameRef.get().dispose();
        });
  }

  private static void check(final boolean ok, final String label) {
    checks++;
    if (!ok) {
      failures++;
      System.out.println("FAIL: " + label);
    } else {
      System.out.println("  ok: " + label);
    }
  }
}
