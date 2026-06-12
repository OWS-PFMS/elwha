package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Guard for #449 — the rich tooltip contract: builder validation (supporting text required,
 * variant-guarded setters both ways), the M3 box math (320 cap, halo reserve in the preferred size,
 * baseline rhythm growing the card per part, action row sizing), token-correct paint
 * (SURFACE_CONTAINER fill, shadow alpha in the halo band, ON_SURFACE_VARIANT text presence,
 * light/dark), the END default alignment, and — windowed — default-rich dismissal behaviors (press
 * inside dismisses; action click dismisses then fires).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipRichSmoke {

  private static int checks;
  private static int failures;

  private TooltipRichSmoke() {}

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

    contractChecks();
    geometryChecks();
    paintChecks();
    if (!GraphicsEnvironment.isHeadless()) {
      behaviorChecks();
    } else {
      System.out.println("(headless — windowed behavior checks skipped)");
    }

    System.out.println(
        failures == 0 ? "PASS — " + checks + " checks" : "FAIL — " + failures + "/" + checks);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void contractChecks() {
    boolean threw = false;
    try {
      ElwhaTooltip.rich().subhead("only a subhead").build();
    } catch (final IllegalStateException e) {
      threw = true;
    }
    check(threw, "build() without supportingText throws");

    final ElwhaTooltip rich = ElwhaTooltip.rich().supportingText("body").build();
    check(rich.getVariant() == TooltipVariant.RICH, "builder yields RICH");
    check(rich.getAlignment() == TooltipAlignment.END, "rich defaults to END alignment");
    check(ElwhaTooltip.plain("p").getAlignment() == TooltipAlignment.CENTER, "plain stays CENTER");

    boolean plainGuard = false;
    try {
      rich.setText("nope");
    } catch (final IllegalStateException e) {
      plainGuard = true;
    }
    check(plainGuard, "setText on RICH throws");
    boolean richGuard = false;
    try {
      ElwhaTooltip.plain("p").setSupportingText("nope");
    } catch (final IllegalStateException e) {
      richGuard = true;
    }
    check(richGuard, "setSupportingText on PLAIN throws");

    rich.setSubhead("now with subhead");
    rich.setSupportingText("replaced");
    check(
        "now with subhead".equals(rich.getSubhead()) && "replaced".equals(rich.getSupportingText()),
        "rich setters round-trip");
  }

  private static void geometryChecks() {
    final TooltipSurface bare = new TooltipSurface(TooltipVariant.RICH, null, null, "short body");
    final Insets halo = bare.halo();
    check(halo.top > 0 && halo.bottom > 0, "rich halo reserves shadow insets");
    check(
        new TooltipSurface(TooltipVariant.PLAIN, "p", null, null).halo().top == 0,
        "plain halo is zero");

    final Dimension barePref = bare.getPreferredSize();
    final TooltipSurface withSubhead =
        new TooltipSurface(TooltipVariant.RICH, null, "A subhead", "short body");
    final Dimension subheadPref = withSubhead.getPreferredSize();
    check(subheadPref.height > barePref.height, "subhead grows the card");

    final TooltipSurface wide =
        new TooltipSurface(
            TooltipVariant.RICH,
            null,
            null,
            "supporting text long enough that it must wrap several times before it would ever be"
                + " allowed to push the rich container past the three hundred twenty pixel cap"
                + " that the M3 token sheet specifies for this variant");
    final Dimension widePref = wide.getPreferredSize();
    check(
        widePref.width - halo.left - halo.right <= TooltipSurface.RICH_MAX_WIDTH_PX,
        "body width caps at 320 (was " + (widePref.width - halo.left - halo.right) + ")");
    check(widePref.height > barePref.height, "wrapped supporting text grows the card");

    final TooltipSurface withAction =
        new TooltipSurface(TooltipVariant.RICH, null, null, "short body");
    withAction.addActionButton(com.owspfm.elwha.button.ElwhaButton.textButton("Learn more"));
    final Dimension actionPref = withAction.getPreferredSize();
    check(
        actionPref.height
            >= barePref.height
                + TooltipSurface.RICH_ACTION_ROW_MIN_PX
                + TooltipSurface.RICH_ACTION_BOTTOM_PAD_PX
                - TooltipSurface.RICH_TEXT_BOTTOM_PAD_PX,
        "action row grows the card by at least the 36+8 row block");
  }

  private static void paintChecks() {
    final BufferedImage light = renderRich();
    final TooltipSurface probe = new TooltipSurface(TooltipVariant.RICH, null, "s", "b");
    final Insets halo = probe.halo();
    final int fill = light.getRGB(halo.left + 8, halo.top + 16);
    check(
        fill == ColorRole.SURFACE_CONTAINER.resolve().getRGB(),
        "light-mode fill is SURFACE_CONTAINER");
    final int below = light.getRGB(light.getWidth() / 2, light.getHeight() - halo.bottom / 2);
    check((below >>> 24) > 0, "shadow alpha present in the halo band below the card");
    check(
        hasColorPixel(light, ColorRole.ON_SURFACE_VARIANT.resolve().getRGB(), halo),
        "ON_SURFACE_VARIANT text pixels painted");

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    final BufferedImage dark = renderRich();
    final int darkFill = dark.getRGB(halo.left + 8, halo.top + 16);
    check(
        darkFill == ColorRole.SURFACE_CONTAINER.resolve().getRGB(),
        "dark-mode fill is SURFACE_CONTAINER");
    check(darkFill != fill, "fill changes between modes");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
  }

  private static BufferedImage renderRich() {
    final TooltipSurface surface =
        new TooltipSurface(
            TooltipVariant.RICH, null, "Rich tooltip", "Supporting text for the paint check");
    final Dimension pref = surface.getPreferredSize();
    surface.setSize(pref);
    final BufferedImage img =
        new BufferedImage(pref.width, pref.height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = img.createGraphics();
    surface.paint(g2);
    g2.dispose();
    return img;
  }

  private static boolean hasColorPixel(final BufferedImage img, final int rgb, final Insets halo) {
    for (int y = halo.top; y < img.getHeight() - halo.bottom; y++) {
      for (int x = halo.left; x < img.getWidth() - halo.right; x++) {
        if (img.getRGB(x, y) == rgb) {
          return true;
        }
      }
    }
    return false;
  }

  private static void behaviorChecks() throws Exception {
    final AtomicReference<JFrame> frameRef = new AtomicReference<>();
    final AtomicReference<JButton> anchorRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("TooltipRichSmoke");
          final JPanel panel = new JPanel(new java.awt.GridBagLayout());
          final JButton anchorButton = new JButton("anchor");
          panel.add(anchorButton);
          frame.add(panel);
          frame.setSize(560, 420);
          final java.awt.Rectangle screen = frame.getGraphicsConfiguration().getBounds();
          frame.setLocation(screen.x + screen.width - 580, screen.y + screen.height - 460);
          frame.setVisible(true);
          frameRef.set(frame);
          anchorRef.set(anchorButton);
        });
    Thread.sleep(250);

    // Press inside the contents dismisses default rich.
    final ElwhaTooltip rich =
        ElwhaTooltip.rich().subhead("Subhead").supportingText("Body text").build();
    SwingUtilities.invokeAndWait(() -> rich.show(anchorRef.get()));
    pumpEdt();
    check(rich.isTooltipShowing(), "rich shows programmatically");
    final AtomicReference<TooltipSurface> surfaceRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final javax.swing.JLayeredPane pane = frameRef.get().getRootPane().getLayeredPane();
          for (final java.awt.Component c :
              pane.getComponentsInLayer(javax.swing.JLayeredPane.POPUP_LAYER)) {
            if (c instanceof TooltipSurface s) {
              surfaceRef.set(s);
            }
          }
        });
    check(surfaceRef.get() != null, "rich surface mounted");
    SwingUtilities.invokeAndWait(
        () ->
            surfaceRef
                .get()
                .dispatchEvent(
                    new MouseEvent(
                        surfaceRef.get(),
                        MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(),
                        0,
                        10,
                        10,
                        1,
                        false,
                        MouseEvent.BUTTON1)));
    check(waitFor(() -> !rich.isTooltipShowing(), 1000), "press inside the contents dismisses");

    // Action click dismisses then fires.
    final AtomicBoolean fired = new AtomicBoolean();
    final AtomicBoolean shownWhenFired = new AtomicBoolean(true);
    final AtomicReference<ElwhaTooltip> actionTipRef = new AtomicReference<>();
    final ElwhaTooltip actionTip =
        ElwhaTooltip.rich()
            .supportingText("with an action")
            .action(
                "Learn more",
                e -> {
                  fired.set(true);
                  shownWhenFired.set(actionTipRef.get().isTooltipShowing());
                })
            .build();
    actionTipRef.set(actionTip);
    SwingUtilities.invokeAndWait(() -> actionTip.show(anchorRef.get()));
    pumpEdt();
    check(actionTip.isTooltipShowing(), "action-bearing rich shows");
    final AtomicReference<TooltipSurface> actionSurfaceRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final javax.swing.JLayeredPane pane = frameRef.get().getRootPane().getLayeredPane();
          for (final java.awt.Component c :
              pane.getComponentsInLayer(javax.swing.JLayeredPane.POPUP_LAYER)) {
            if (c instanceof TooltipSurface s) {
              actionSurfaceRef.set(s);
            }
          }
        });
    check(
        actionSurfaceRef.get() != null && actionSurfaceRef.get().actionButtons().size() == 1,
        "one real text button on the action row");
    SwingUtilities.invokeAndWait(() -> actionSurfaceRef.get().actionButtons().get(0).doClick());
    check(waitFor(fired::get, 1000), "action listener fired");
    check(
        waitFor(() -> !actionTip.isTooltipShowing(), 1000) && !shownWhenFired.get(),
        "tooltip dismissed before the listener ran");

    SwingUtilities.invokeAndWait(() -> frameRef.get().dispose());
  }

  private static void pumpEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
  }

  private static boolean waitFor(final BooleanSupplier condition, final long timeoutMs)
      throws Exception {
    final long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      final AtomicBoolean state = new AtomicBoolean();
      SwingUtilities.invokeAndWait(() -> state.set(condition.getAsBoolean()));
      if (state.get()) {
        return true;
      }
      Thread.sleep(20);
    }
    return false;
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
