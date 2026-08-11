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
 * Interactive demo for the S5 sizing + accessibility surface (story #518): standard and contained
 * spinners across a range of sizes, plus a console dump of each indicator's accessible role, value,
 * and busy state. Not a test; run by hand.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingSizingDemo {

  private LoadingSizingDemo() {}

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
          final JFrame frame = new JFrame("ElwhaLoadingIndicator — S5 sizing + a11y");
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          final JPanel root = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 28));
          root.setBorder(new EmptyBorder(24, 24, 24, 24));
          for (final int size : new int[] {16, 24, 38, 56, 80}) {
            final ElwhaLoadingIndicator std = new ElwhaLoadingIndicator();
            std.setIndicatorSize(size);
            std.getAccessibleContext().setAccessibleName("Loading, " + size + "px");
            root.add(std);
          }
          for (final int size : new int[] {38, 56, 80}) {
            final ElwhaLoadingIndicator c = ElwhaLoadingIndicator.contained();
            c.setIndicatorSize(size);
            c.setContainerSize(Math.round(size * 48f / 38f));
            root.add(c);
          }
          final ElwhaLoadingIndicator probe = new ElwhaLoadingIndicator();
          System.out.println("role=" + probe.getAccessibleContext().getAccessibleRole());
          System.out.println(
              "indeterminate value="
                  + probe.getAccessibleContext().getAccessibleValue().getCurrentAccessibleValue());

          frame.setContentPane(root);
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}
