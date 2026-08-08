package com.owspfm.elwha.appbar;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual smoke for height-driven collapse (#525) — what a flexible bar does when the host gives it
 * less height than it asked for.
 *
 * <p>The <strong>left column</strong> is the reported bug's exact configuration (large flexible,
 * subtitle, nav icon, one action — preferred height 152) allocated 152, 120, 96, 72 and 64&nbsp;px
 * in turn. Before the fix, 96 painted the hamburger glyph straight through the "H" of the headline
 * and 72 clipped the headline off the top of the component. Each row should now read as a properly
 * collapsed bar at that height: icons untouched in their strip, the expanded headline fading out,
 * the collapsed strip title fading in.
 *
 * <p>The <strong>right column</strong> is the claim that makes the fix defensible rather than
 * merely nicer: each row is the same bar <em>scrolled</em> to that height instead of squeezed to
 * it. Left and right should be indistinguishable — an under-allocated bar is not a special
 * rendering mode, it is the ordinary collapse read off the height it was handed.
 *
 * <p>The bottom strip is a {@code GridLayout} of all three variants, which is where the bug was
 * found: {@code GridLayout} ignores minimum and preferred sizes and divides its cells evenly, so
 * the two flexible bars are under-allocated by construction. Drag the window shorter and they
 * should collapse rather than overlap.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaAppBarAllocationDemo {

  private static final int[] HEIGHTS = {152, 120, 96, 72, 64};

  private ElwhaAppBarAllocationDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(ElwhaAppBarAllocationDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — height-driven collapse (#525)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // BoxLayout, not GridLayout: these rows have to keep the exact heights they were given, and
    // GridLayout would even them out again — which is the very behaviour under test below.
    final JPanel squeezed = column("Under-allocated to N px");
    final JPanel scrolled = column("Scroll-collapsed to N px — should match");
    for (final int height : HEIGHTS) {
      squeezed.add(pinned(titled(height + " px allocated", clampedTo(specimen(), height))));
      final ElwhaAppBar bar = specimen();
      bar.setCollapsedFraction(fractionFor(height));
      scrolled.add(pinned(titled(height + " px by collapse", clampedTo(bar, height))));
    }

    final JPanel comparison = new JPanel(new GridLayout(1, 2, 16, 0));
    comparison.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    comparison.add(squeezed);
    comparison.add(scrolled);

    // GridLayout divides its cells evenly and consults neither minimum nor preferred size, which
    // is exactly how the S1/S2/S5 specimen demos surfaced this in the first place.
    final JPanel evenCells = new JPanel(new GridLayout(1, 3, 12, 0));
    evenCells.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
    for (final AppBarVariant variant : AppBarVariant.values()) {
      final ElwhaAppBar bar = new ElwhaAppBar(variant);
      bar.setTitle("Headline");
      bar.setSubtitle("Subtitle");
      bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
      bar.addAction(MaterialIcons.moreVert(), "More options", null);
      evenCells.add(bar);
    }
    evenCells.setPreferredSize(new Dimension(0, 160));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode next =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(next).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(mode);
    controls.add(
        new JLabel("Drag the window shorter — the GridLayout row below collapses, never overlaps"));

    frame.setLayout(new BorderLayout());
    frame.add(comparison, BorderLayout.CENTER);
    frame.add(evenCells, BorderLayout.SOUTH);
    frame.add(controls, BorderLayout.NORTH);
    frame.setSize(1000, 900);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static ElwhaAppBar specimen() {
    final ElwhaAppBar bar = ElwhaAppBar.largeFlexible();
    bar.setTitle("Headline");
    bar.setSubtitle("Subtitle");
    bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    bar.addAction(MaterialIcons.moreVert(), "More options", null);
    return bar;
  }

  private static float fractionFor(final int height) {
    final int expanded = AppBarVariant.LARGE_FLEXIBLE.expandedHeightPx(true);
    return (expanded - height) / (float) (expanded - ElwhaAppBar.STRIP_HEIGHT_PX);
  }

  // BorderLayout.CENTER hands the bar the whole of a panel whose own height is pinned, which is
  // how a host under-allocates it without the bar ever being asked what it wanted.
  private static JPanel clampedTo(final ElwhaAppBar bar, final int height) {
    final JPanel clamp = new JPanel(new BorderLayout());
    clamp.setOpaque(false);
    clamp.add(bar, BorderLayout.CENTER);
    clamp.setPreferredSize(new Dimension(0, height));
    return clamp;
  }

  private static JPanel titled(final String label, final JPanel content) {
    final JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setOpaque(false);
    panel.add(new JLabel(label), BorderLayout.NORTH);
    panel.add(content, BorderLayout.CENTER);
    return panel;
  }

  private static JPanel column(final String heading) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(false);
    final JLabel label = new JLabel(heading);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(label);
    return panel;
  }

  private static JPanel pinned(final JPanel row) {
    row.setAlignmentX(Component.LEFT_ALIGNMENT);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 12));
    row.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
    return row;
  }
}
