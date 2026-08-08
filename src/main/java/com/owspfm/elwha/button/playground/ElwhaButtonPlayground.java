package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
 * @version v0.5.0
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
    final Mode[] modes = {Mode.LIGHT, Mode.DARK, Mode.SYSTEM};
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setColorStyle(ButtonGroupColorStyle.OUTLINED);
    for (Mode mode : modes) {
      group.add(new ElwhaButton(mode.name()));
    }
    for (int i = 0; i < modes.length; i++) {
      if (ElwhaTheme.current().mode() == modes[i]) {
        group.setSelectedIndex(i);
      }
    }
    group.addSelectionListener(g -> applyMode(modes[g.getSelectedIndex()]));
    bar.add(group);
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
