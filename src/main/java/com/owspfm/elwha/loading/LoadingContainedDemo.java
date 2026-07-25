package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Interactive demo for the S3 contained configuration (story #516): a standard spinner beside a
 * contained one (active shape on a {@code primaryContainer} circle) at several sizes. Not a test;
 * run by hand.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingContainedDemo {

  private LoadingContainedDemo() {}

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
          final JFrame frame = new JFrame("ElwhaLoadingIndicator — S3 contained");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          final JPanel root = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 40));
          root.setBorder(new EmptyBorder(24, 24, 24, 24));

          final ElwhaLoadingIndicator standard = new ElwhaLoadingIndicator();
          standard.setIndicatorSize(48);
          root.add(standard);

          final ElwhaLoadingIndicator contained = ElwhaLoadingIndicator.contained();
          contained.setIndicatorSize(48);
          contained.setContainerSize(60);
          root.add(contained);

          final ElwhaLoadingIndicator big = ElwhaLoadingIndicator.contained();
          big.setIndicatorSize(76);
          big.setContainerSize(96);
          root.add(big);

          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
