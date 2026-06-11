package com.owspfm.elwha.tabs;

import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * S3 visual smoke for the {@link ElwhaTabs} indicator slide (#428): unequal-width tabs make the
 * 250&nbsp;ms emphasized x+width interpolation visible on both variants (the primary
 * content-hugging indicator stretches between label widths; the secondary full-width indicator
 * stretches between tab widths). Controls: auto-cycle, 5× slow motion, reduced motion (snaps),
 * and click anywhere mid-slide to watch the retarget pick up from the in-flight rect.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsMotionDemo {

  private ElwhaTabsMotionDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(ElwhaTabsMotionDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTabs — S3 indicator slide (#428)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaTabs primary = ElwhaTabs.primary();
    final ElwhaTabs secondary = ElwhaTabs.secondary();
    for (String label : new String[] {"Hi", "A much longer tab label", "Mid", "Tiny"}) {
      primary.addTab(label);
      secondary.addTab(label);
    }

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 18));
    bars.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    bars.add(titled("Primary — content-hugging indicator stretches between label widths",
        primary));
    bars.add(titled("Secondary — full-width indicator stretches between tab widths", secondary));

    final Timer cycler =
        new Timer(
            1200,
            e -> {
              final int next = (primary.getActiveTabIndex() + 1) % primary.getTabCount();
              primary.setActiveTabIndex(next);
              secondary.setActiveTabIndex(next);
            });

    final ElwhaSwitch auto = new ElwhaSwitch();
    auto.setLabel("Auto-cycle");
    auto.addActionListener(
        e -> {
          if (auto.isSelected()) {
            cycler.start();
          } else {
            cycler.stop();
          }
        });

    final ElwhaSwitch slow = new ElwhaSwitch();
    slow.setLabel("5x slow motion");
    slow.addActionListener(e -> MorphAnimator.setDurationMultiplier(slow.isSelected() ? 5f : 1f));

    final ElwhaSwitch reduced = new ElwhaSwitch();
    reduced.setLabel("Reduced motion");
    reduced.setSelected(MorphAnimator.isReducedMotion());
    reduced.addActionListener(e -> MorphAnimator.setReducedMotion(reduced.isSelected()));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(new JLabel("Auto-cycle:"));
    controls.add(auto);
    controls.add(new JLabel("5x slow:"));
    controls.add(slow);
    controls.add(new JLabel("Reduced motion:"));
    controls.add(reduced);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(760, 320);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel titled(final String title, final ElwhaTabs bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 6));
    panel.setOpaque(false);
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}
