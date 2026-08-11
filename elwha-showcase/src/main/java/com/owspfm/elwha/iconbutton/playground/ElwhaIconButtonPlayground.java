package com.owspfm.elwha.iconbutton.playground;

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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Standalone icon-button-focused playground.
 *
 * <p>A two-tab frame composing the shared {@link IconButtonPlaygroundPanels} builders — the variant
 * gallery and toggle examples — under a light / dark / system mode toggle that re-installs the
 * Elwha theme at runtime. The same two panels are also surfaced inside {@code ThemePlayground}'s
 * {@code Icon Button} tab so the validation surface stays in lockstep across both entry points.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.iconbutton.playground.ElwhaIconButtonPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public final class ElwhaIconButtonPlayground {

  private final JFrame frame = new JFrame("ElwhaIconButton playground");

  private ElwhaIconButtonPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaIconButtonPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab(
        "Variant gallery", new JScrollPane(IconButtonPlaygroundPanels.buildVariantGalleryPanel()));
    tabs.addTab(
        "Toggle examples", new JScrollPane(IconButtonPlaygroundPanels.buildToggleExamplesPanel()));
    tabs.addTab("Sizes", new JScrollPane(IconButtonPlaygroundPanels.buildSizesPanel()));
    tabs.addTab("Live", IconButtonPlaygroundPanels.buildLivePanel());
    frame.add(tabs, BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);

    frame.setSize(900, 640);
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
