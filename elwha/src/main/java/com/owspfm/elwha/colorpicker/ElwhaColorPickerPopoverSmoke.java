package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * V2 S5 headless guard for the popover (#501): the pure placement function (below/leading default,
 * flip-above on clip, horizontal shift, RTL trailing alignment, shadow-reserve body math — the
 * surface bounds include the shadow, the visual alignment must not), API delegation onto the
 * embedded picker (initial color, modes, sources, alpha, eyedropper, live change listeners,
 * suppressed supporting text), and the once-per-show dismiss-callback latch.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerPopoverSmoke {

  private ElwhaColorPickerPopoverSmoke() {}

  /**
   * Runs the guard.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkPlacement();
    checkDelegation();
    checkDismissLatch();

    System.out.println(
        "ElwhaColorPickerPopoverSmoke: OK (placement quadrants + shadow math, delegation,"
            + " dismiss latch)");
  }

  private static void checkPlacement() {
    final Dimension pane = new Dimension(1000, 800);
    final Dimension surfaceSize = new Dimension(360, 480);
    final Insets shadow = new Insets(8, 12, 16, 12);
    final int gap = 4;

    final Rectangle below =
        PopoverHost.place(pane, new Rectangle(100, 50, 80, 40), surfaceSize, shadow, gap, true);
    check("body opens below the anchor", below.y + shadow.top == 50 + 40 + gap);
    check("leading edges align (LTR)", below.x + shadow.left == 100);
    check("surface keeps its shadow-inclusive size", below.width == 360 && below.height == 480);

    final Rectangle flipped =
        PopoverHost.place(pane, new Rectangle(100, 700, 80, 40), surfaceSize, shadow, gap, true);
    final int bodyHeight = 480 - shadow.top - shadow.bottom;
    check("flips above when below clips", flipped.y + shadow.top == 700 - gap - bodyHeight);

    final Rectangle shifted =
        PopoverHost.place(pane, new Rectangle(950, 50, 40, 40), surfaceSize, shadow, gap, true);
    final int bodyWidth = 360 - shadow.left - shadow.right;
    check(
        "shifts horizontally to stay inside the pane",
        shifted.x + shadow.left + bodyWidth <= pane.width && shifted.x + shadow.left >= 0);

    final Rectangle rtl =
        PopoverHost.place(pane, new Rectangle(600, 50, 80, 40), surfaceSize, shadow, gap, false);
    check("RTL aligns trailing edges", rtl.x + shadow.left + bodyWidth == 600 + 80);

    final Rectangle cramped =
        PopoverHost.place(
            new Dimension(400, 300),
            new Rectangle(10, 250, 40, 40),
            surfaceSize,
            shadow,
            gap,
            true);
    check("no room either way still clamps on-pane", cramped.y + shadow.top >= 0);
  }

  private static void checkDelegation() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    check("supporting text suppressed", popover.picker().getSupportingText() == null);
    check("not showing before show()", !popover.isShowing());

    popover.setInitialColor(new Color(0x2E7D32));
    check("initial color delegates", new Color(0x2E7D32).equals(popover.getColor()));

    popover.setModes(PickerMode.WHEEL, PickerMode.SLIDERS);
    check(
        "modes delegate", List.of(PickerMode.WHEEL, PickerMode.SLIDERS).equals(popover.getModes()));

    popover.setSwatchSources(SwatchSource.THEME);
    check(
        "sources delegate",
        List.of(SwatchSource.THEME).equals(popover.picker().getSwatchSources()));

    popover.setAlphaEnabled(true);
    check("alpha delegates", popover.picker().isAlphaEnabled());
    popover.setEyedropperEnabled(true);
    check("eyedropper delegates", popover.picker().isEyedropperEnabled());

    final AtomicInteger commits = new AtomicInteger();
    popover.addChangeListener(e -> commits.incrementAndGet());
    popover.picker().setColor(new Color(0x123456));
    check("live commits reach popover listeners", commits.get() == 1);
  }

  private static void checkDismissLatch() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    final AtomicInteger dismissals = new AtomicInteger();
    popover.onDismiss(dismissals::incrementAndGet);

    popover.handleClosed();
    check("no dismiss callback before any show", dismissals.get() == 0);

    popover.close();
    check("close() while hidden is a no-op", dismissals.get() == 0 && !popover.isShowing());
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
