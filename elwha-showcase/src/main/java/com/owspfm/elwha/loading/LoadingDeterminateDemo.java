package com.owspfm.elwha.loading;

import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Interactive demo for the S4 determinate mode (story #517): a standard and a contained determinate
 * indicator sharing one model with an {@link ElwhaSlider}, so dragging the slider morphs both
 * indicators {@code Circle → SoftBurst} and sweeps their −180° rotation. Not a test; run by hand.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingDeterminateDemo {

  private LoadingDeterminateDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(
        () -> {
          final JFrame frame = new JFrame("ElwhaLoadingIndicator — S4 determinate");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

          final BoundedRangeModel model = new DefaultBoundedRangeModel(35, 0, 0, 100);

          final JPanel indicators = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 24));
          final ElwhaLoadingIndicator standard = ElwhaLoadingIndicator.determinate(model);
          standard.setIndicatorSize(72);
          indicators.add(standard);
          final ElwhaLoadingIndicator contained = ElwhaLoadingIndicator.determinate(model);
          contained.setContained(true);
          contained.setIndicatorColorRole(ColorRole.ON_PRIMARY_CONTAINER);
          contained.setContainerColorRole(ColorRole.PRIMARY_CONTAINER);
          contained.setIndicatorSize(72);
          contained.setContainerSize(92);
          indicators.add(contained);

          final ElwhaSlider slider = new ElwhaSlider(model);

          final JPanel root = new JPanel(new BorderLayout(0, 16));
          root.setBorder(new EmptyBorder(24, 24, 24, 24));
          root.add(indicators, BorderLayout.CENTER);
          root.add(slider, BorderLayout.SOUTH);

          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
