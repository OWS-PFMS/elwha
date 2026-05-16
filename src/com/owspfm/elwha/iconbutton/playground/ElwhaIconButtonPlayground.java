package com.owspfm.elwha.iconbutton.playground;

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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
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
 * @version v0.1.0
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
