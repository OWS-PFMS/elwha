package com.owspfm.elwha.tabs;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the tab bar's mouse-wheel hand-off (#624). This one needs a real display for a
 * structural reason: {@code Component.dispatchEventImpl} retargets a wheel event to an ancestor
 * only when the component has a peer, so on a headless hierarchy nothing forwards and the assertion
 * could not tell a swallowed notch from a forwarded one.
 *
 * <p>Wheel events are dispatched synthetically rather than through {@code Robot} — the peer is the
 * ingredient that was missing, not the pointer, and a synthetic notch keeps the scroll arithmetic
 * exact.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaTabsWheelGuiTest {

  private JFrame frame;
  private ElwhaTabs bar;
  private JPanel filler;
  private JScrollPane scroll;

  @BeforeEach
  void showAPageThatScrolls() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          bar = ElwhaTabs.primary();
          bar.addTab(ElwhaTab.of("One"));
          bar.addTab(ElwhaTab.of("Two"));
          final JPanel page = new JPanel();
          page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
          page.add(bar);
          filler = new JPanel();
          filler.setPreferredSize(new Dimension(400, 4000));
          page.add(filler);
          scroll = new JScrollPane(page);
          frame = new JFrame("ElwhaTabsWheelGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setContentPane(scroll);
          frame.setSize(500, 320);
          frame.setVisible(true);
        });
    onEdt(() -> bar.isDisplayable());
  }

  @AfterEach
  void disposeTheFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  /** Sends one wheel notch to {@code target} and reports where the page viewport ended up. */
  private int wheelOver(final Component target) throws Exception {
    final int[] y = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          scroll.getViewport().setViewPosition(new Point(0, 0));
          target.dispatchEvent(
              new MouseWheelEvent(
                  target,
                  MouseWheelEvent.MOUSE_WHEEL,
                  System.currentTimeMillis(),
                  0,
                  5,
                  5,
                  0,
                  false,
                  MouseWheelEvent.WHEEL_UNIT_SCROLL,
                  3,
                  3));
          y[0] = scroll.getViewport().getViewPosition().y;
        });
    return y[0];
  }

  @Test
  void aFixedBarDoesNotSwallowThePagesWheelScrolling() throws Exception {
    SwingUtilities.invokeAndWait(() -> bar.setTabMode(TabMode.FIXED));

    final int overTheBar = wheelOver(bar);

    assertThat(overTheBar)
        .as(
            "#624 — registering any MouseWheelListener stops the ancestor retarget, so a FIXED bar"
                + " froze the page whenever the pointer was over it")
        .isEqualTo(wheelOver(filler))
        .isGreaterThan(0);
  }

  @Test
  void aScrollableBarWithNowhereLeftToGoPassesTheNotchUp() throws Exception {
    // Two short tabs in a 500px window: the strip already fits, so there is no scrolling to do.
    SwingUtilities.invokeAndWait(() -> bar.setTabMode(TabMode.SCROLLABLE));

    assertThat(wheelOver(bar))
        .as("a strip at its scroll limit did not act on the notch, so the page must get it")
        .isEqualTo(wheelOver(filler));
  }

  @Test
  void aScrollableBarThatCanScrollKeepsTheNotchForItself() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          bar.setTabMode(TabMode.SCROLLABLE);
          for (int i = 0; i < 20; i++) {
            bar.addTab(ElwhaTab.of("Destination " + i));
          }
          // Cap the strip narrower than its content so it genuinely overflows; left to the page's
          // BoxLayout it would simply be handed its full preferred width and never scroll.
          bar.setMaximumSize(new Dimension(300, bar.getPreferredSize().height));
          frame.validate();
        });
    assertThat(onEdt(() -> bar.getWidth() < bar.getPreferredSize().width))
        .as("arrangement: the strip is narrower than its tabs")
        .isTrue();

    final int pageY = wheelOver(bar);

    assertThat(onEdt(() -> bar.getScrollOffset() > 0))
        .as("the strip overflows, so the notch scrolls the strip")
        .isTrue();
    assertThat(pageY).as("and the page must not scroll along with it").isZero();
  }
}
