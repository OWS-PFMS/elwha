package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.ComponentOrientation;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.accessibility.AccessibleRole;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Guard for #451 — the a11y and motion contract: the surface carries {@code
 * AccessibleRole.TOOL_TIP} with the content as its accessible name (plain text / rich
 * subhead+supporting concatenation); {@code attach} writes the anchor's accessible description,
 * content setters keep it in sync, and {@code detach} clears it only when still ours; and —
 * windowed — the 150/75 fade is observable (mid-entrance renders below full alpha, reduced motion
 * snaps to full immediately, the exit completes fast) and RTL placement mirrors end to end on a
 * real pane.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipA11ySmoke {

  private static int checks;
  private static int failures;

  private TooltipA11ySmoke() {}

  /**
   * Runs the guard; exits non-zero on any failure.
   *
   * @param args unused
   * @throws Exception on EDT plumbing failures
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    roleAndNameChecks();
    descriptionChecks();
    if (!GraphicsEnvironment.isHeadless()) {
      motionChecks();
      rtlChecks();
    } else {
      System.out.println("(headless — windowed motion/RTL checks skipped)");
    }

    System.out.println(
        failures == 0 ? "PASS — " + checks + " checks" : "FAIL — " + failures + "/" + checks);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void roleAndNameChecks() {
    final TooltipSurface plain = new TooltipSurface("a label");
    check(
        plain.getAccessibleContext().getAccessibleRole() == AccessibleRole.TOOL_TIP,
        "surface role is TOOL_TIP");

    final TooltipSurface rich = new TooltipSurface(TooltipVariant.RICH, null, "s", "b");
    check(
        rich.getAccessibleContext().getAccessibleRole() == AccessibleRole.TOOL_TIP,
        "rich surface role is TOOL_TIP");
  }

  private static void descriptionChecks() {
    final JButton anchor = new JButton("anchor");
    final ElwhaTooltip tip = ElwhaTooltip.plain("describe me").attach(anchor);
    check(
        "describe me".equals(anchor.getAccessibleContext().getAccessibleDescription()),
        "attach writes the anchor accessible description");
    tip.setText("updated");
    check(
        "updated".equals(anchor.getAccessibleContext().getAccessibleDescription()),
        "setText keeps the description in sync");
    tip.detach();
    check(
        anchor.getAccessibleContext().getAccessibleDescription() == null,
        "detach clears the description when still ours");

    final ElwhaTooltip again = ElwhaTooltip.plain("mine").attach(anchor);
    anchor.getAccessibleContext().setAccessibleDescription("consumer overwrote this");
    again.detach();
    check(
        "consumer overwrote this".equals(anchor.getAccessibleContext().getAccessibleDescription()),
        "detach leaves a foreign description alone");

    final JButton richAnchor = new JButton("rich anchor");
    ElwhaTooltip.rich().subhead("Head").supportingText("Body").build().attach(richAnchor);
    check(
        "Head. Body".equals(richAnchor.getAccessibleContext().getAccessibleDescription()),
        "rich description concatenates subhead and supporting text");
  }

  private static void motionChecks() throws Exception {
    final AtomicReference<JFrame> frameRef = new AtomicReference<>();
    final AtomicReference<JButton> anchorRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("TooltipA11ySmoke");
          final JPanel panel = new JPanel(new java.awt.GridBagLayout());
          final JButton anchorButton = new JButton("anchor");
          panel.add(anchorButton);
          frame.add(panel);
          frame.setSize(480, 360);
          final Rectangle screen = frame.getGraphicsConfiguration().getBounds();
          frame.setLocation(screen.x + screen.width - 500, screen.y + screen.height - 400);
          frame.setVisible(true);
          frameRef.set(frame);
          anchorRef.set(anchorButton);
        });
    Thread.sleep(250);

    // Animated entrance: a render taken immediately after show() sits below full alpha.
    MorphAnimator.setReducedMotion(false);
    final ElwhaTooltip tip = ElwhaTooltip.plain("fading");
    final AtomicBoolean midFade = new AtomicBoolean();
    SwingUtilities.invokeAndWait(
        () -> {
          tip.show(anchorRef.get());
          midFade.set(surfaceAlpha(frameRef.get()) < 255);
        });
    check(midFade.get(), "mid-entrance render is below full alpha (fade-in live)");
    check(waitFor(() -> surfaceAlpha(frameRef.get()) == 255, 1500), "entrance reaches full alpha");
    SwingUtilities.invokeAndWait(tip::dismiss);
    check(waitFor(() -> !tip.isTooltipShowing(), 600), "75ms exit completes fast");

    // Reduced motion snaps.
    MorphAnimator.setReducedMotion(true);
    final ElwhaTooltip snapped = ElwhaTooltip.plain("snapped");
    final AtomicBoolean fullAtOnce = new AtomicBoolean();
    SwingUtilities.invokeAndWait(
        () -> {
          snapped.show(anchorRef.get());
          fullAtOnce.set(surfaceAlpha(frameRef.get()) == 255);
        });
    check(fullAtOnce.get(), "reduced motion snaps the entrance to full alpha");
    SwingUtilities.invokeAndWait(snapped::dismiss);
    SwingUtilities.invokeAndWait(() -> frameRef.get().dispose());
    MorphAnimator.setReducedMotion(false);
  }

  // Renders the mounted surface and samples a fill pixel's alpha (0..255), or -1 when absent.
  private static int surfaceAlpha(final JFrame frame) {
    for (final java.awt.Component c :
        frame
            .getRootPane()
            .getLayeredPane()
            .getComponentsInLayer(javax.swing.JLayeredPane.POPUP_LAYER)) {
      if (c instanceof TooltipSurface s && s.getWidth() > 0) {
        final java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(
                s.getWidth(), s.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        final java.awt.Graphics2D g2 = img.createGraphics();
        s.paint(g2);
        g2.dispose();
        return img.getRGB(4, s.getHeight() / 2) >>> 24;
      }
    }
    return -1;
  }

  private static void rtlChecks() throws Exception {
    MorphAnimator.setReducedMotion(true);
    final AtomicReference<JFrame> frameRef = new AtomicReference<>();
    final AtomicReference<JButton> anchorRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("TooltipA11ySmoke RTL");
          final JPanel panel = new JPanel(new java.awt.GridBagLayout());
          final JButton anchorButton = new JButton("anchor");
          panel.add(anchorButton);
          frame.add(panel);
          frame.setSize(480, 360);
          frame.getContentPane().applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
          final Rectangle screen = frame.getGraphicsConfiguration().getBounds();
          frame.setLocation(screen.x + screen.width - 500, screen.y + screen.height - 400);
          frame.setVisible(true);
          frameRef.set(frame);
          anchorRef.set(anchorButton);
        });
    Thread.sleep(250);

    final ElwhaTooltip tip = ElwhaTooltip.plain("rtl start");
    tip.setAlignment(TooltipAlignment.START);
    SwingUtilities.invokeAndWait(() -> tip.show(anchorRef.get()));
    final AtomicBoolean mirrored = new AtomicBoolean();
    SwingUtilities.invokeAndWait(
        () -> {
          final javax.swing.JLayeredPane pane = frameRef.get().getRootPane().getLayeredPane();
          for (final java.awt.Component c :
              pane.getComponentsInLayer(javax.swing.JLayeredPane.POPUP_LAYER)) {
            if (c instanceof TooltipSurface s) {
              final Rectangle anchorInPane =
                  SwingUtilities.convertRectangle(
                      anchorRef.get().getParent(), anchorRef.get().getBounds(), pane);
              // RTL START = leading edges flush = RIGHT edges align.
              mirrored.set(s.getX() + s.getWidth() == anchorInPane.x + anchorInPane.width);
            }
          }
          tip.dismiss();
          frameRef.get().dispose();
        });
    check(mirrored.get(), "RTL START aligns trailing (right) edges end to end");
    MorphAnimator.setReducedMotion(false);
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
