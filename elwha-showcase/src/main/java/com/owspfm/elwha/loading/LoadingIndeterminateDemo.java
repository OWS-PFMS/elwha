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
 * Interactive demo for the S2 indeterminate choreography (story #515): three live spinners at
 * different sizes morphing through the 7-shape loop while rotating. Watch the shape step every
 * ~650ms and the +90° kick per morph. Not a test; run by hand.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingIndeterminateDemo {

  private LoadingIndeterminateDemo() {}

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
          final JFrame frame = new JFrame("ElwhaLoadingIndicator — S2 indeterminate");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          final JPanel root = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 48));
          root.setBorder(new EmptyBorder(24, 24, 24, 24));
          for (final int size : new int[] {24, 38, 64, 96}) {
            final ElwhaLoadingIndicator ind = new ElwhaLoadingIndicator();
            ind.setIndicatorSize(size);
            root.add(ind);
          }
          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
