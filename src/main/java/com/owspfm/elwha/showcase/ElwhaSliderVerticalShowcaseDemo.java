package com.owspfm.elwha.showcase;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase-5 / S3 smoke launcher (story #387) — opens the Showcase <strong>Slider</strong> leaf
 * standalone (Workbench + state Gallery) so the un-stubbed <strong>Orientation</strong> selector
 * and the vertical gallery rows can be smoked without the full {@link ElwhaShowcase} chrome. In the
 * Workbench, switch Orientation to <em>Vertical</em>: the stage transposes to a bottom-up tall
 * track; choosing the Range variant while vertical surfaces the in-place doc-warn.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderVerticalShowcaseDemo {

  private final JFrame frame = new JFrame("ElwhaSlider — Phase 5 / S3 Showcase leaf");

  private ElwhaSliderVerticalShowcaseDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(() -> new ElwhaSliderVerticalShowcaseDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", SliderShowcasePanels.buildWorkbench());
    tabs.addTab("Gallery", new JScrollPane(SliderShowcasePanels.buildGallery()));

    frame.add(tabs, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(960, 700));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
