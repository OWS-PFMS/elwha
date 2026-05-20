package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Standalone {@link com.owspfm.elwha.button.ElwhaButton}-focused playground.
 *
 * <p>A four-tab frame composing the shared {@link ButtonPlaygroundPanels} builders — variant
 * gallery, sizes, toggle examples, and the live-control panel — under a light / dark / system mode
 * toggle that re-installs the Elwha theme at runtime. The same panels surface inside {@code
 * ThemePlayground}'s {@code Button} tab so the validation surface stays in lockstep across both
 * entry points.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn compile exec:java -Dexec.mainClass=com.owspfm.elwha.button.playground.ElwhaButtonPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaButtonPlayground {

  private final JFrame frame = new JFrame("ElwhaButton playground");

  private ElwhaButtonPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaButtonPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JTabbedPane tabs = ButtonPlaygroundPanels.buildCombinedTabbedPane();
    frame.add(tabs, BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);

    frame.setSize(1100, 760);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    bar.add(new JLabel("Mode:"));
    final ButtonGroup group = new ButtonGroup();
    for (Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK, Mode.SYSTEM}) {
      final JToggleButton button = new JToggleButton(mode.name());
      button.addActionListener(e -> applyMode(mode));
      if (ElwhaTheme.current().mode() == mode) {
        button.setSelected(true);
      }
      group.add(button);
      bar.add(button);
    }
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
