package com.owspfm.elwha.tabs;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S1 visual smoke for {@link ElwhaTabs} static chrome (#426): both variants label-only in FIXED
 * layout — container fill, divider, at-rest indicator (content-hugging rounded primary vs
 * full-width square secondary), active/inactive label colors, programmatic activation, and a
 * light/dark toggle to eyeball token-correctness in both modes.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsChromeDemo {

  private ElwhaTabsChromeDemo() {}

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
    SwingUtilities.invokeLater(ElwhaTabsChromeDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTabs — S1 static chrome (#426)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaTabs primary = ElwhaTabs.primary();
    primary.addTab("Video");
    primary.addTab("Photos");
    primary.addTab("Audio");
    primary.addTab("Subtitles & captions");

    final ElwhaTabs secondary = ElwhaTabs.secondary();
    secondary.addTab("Birds");
    secondary.addTab("Cats");
    secondary.addTab("Dogs");
    secondary.addTab("Reptiles & amphibians");

    final ElwhaTabs three = ElwhaTabs.primary();
    three.addTab("Flights");
    three.addTab("Hotels");
    three.addTab("Activities");
    three.setActiveTabIndex(1);

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 24));
    bars.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    bars.add(titled("Primary — 4 tabs, active 0", primary));
    bars.add(titled("Secondary — 4 tabs, active 0", secondary));
    bars.add(titled("Primary — 3 tabs, active 1 (programmatic)", three));

    final ElwhaButton next = ElwhaButton.filledTonalButton("Activate next (top bar)");
    next.addActionListener(
        e -> primary.setActiveTabIndex((primary.getActiveTabIndex() + 1) % primary.getTabCount()));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode nextMode =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(nextMode).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(next);
    controls.add(mode);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(720, 420);
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
