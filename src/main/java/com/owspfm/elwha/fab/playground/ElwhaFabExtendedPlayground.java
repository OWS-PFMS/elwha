package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #190 (S4) smoketest — Extended FAB factories + label rendering at every {@link
 * ElwhaFab.Size}. Renders a grid that pairs the two {@code extended(...)} factories ({@link
 * ElwhaFab#extended(String)} for text-only and {@link ElwhaFab#extended(javax.swing.Icon, String)}
 * for icon + label) against the three sizes so the validator can confirm per-size leading /
 * trailing / icon-label-gap insets and the size-specific label typography (Inter Medium for Small,
 * Inter Regular for Medium and Large) all resolve. A live row below repeats the icon+text variants
 * as real-interaction artifacts so hover / focus / press behave identically to the Standard form.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabExtendedPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabExtendedPlayground {

  private final JFrame frame = new JFrame("ElwhaFab — S4 Extended (#190)");

  private ElwhaFabExtendedPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaFabExtendedPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    final JPanel body = new JPanel(new BorderLayout());
    body.add(new JScrollPane(buildStaticMatrix()), BorderLayout.CENTER);
    body.add(buildLivePanel(), BorderLayout.SOUTH);
    frame.add(body, BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.setSize(1040, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildStaticMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 16, 8, 16);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();

    gc.gridy = 0;
    gc.gridx = 0;
    grid.add(boldLabel("Form \\ Size"), gc);
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      grid.add(boldLabel(sizes[col].name() + " · " + sizes[col].containerPx() + " dp"), gc);
    }

    gc.gridy = 1;
    gc.gridx = 0;
    gc.anchor = GridBagConstraints.WEST;
    grid.add(new JLabel("extended(text)"), gc);
    gc.anchor = GridBagConstraints.CENTER;
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      final ElwhaFab fab = ElwhaFab.extended("Compose").setFabSize(sizes[col]);
      fab.setToolTipText("Compose — " + sizes[col].name() + " · text-only");
      grid.add(fab, gc);
    }

    gc.gridy = 2;
    gc.gridx = 0;
    gc.anchor = GridBagConstraints.WEST;
    grid.add(new JLabel("extended(icon, text)"), gc);
    gc.anchor = GridBagConstraints.CENTER;
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      final ElwhaFab fab =
          ElwhaFab.extended(MaterialIcons.add(sizes[col].iconPx()), "Compose")
              .setFabSize(sizes[col]);
      fab.setToolTipText("Compose — " + sizes[col].name() + " · icon + text");
      grid.add(fab, gc);
    }

    gc.gridy = 3;
    gc.gridx = 0;
    gc.anchor = GridBagConstraints.WEST;
    grid.add(new JLabel("Disabled"), gc);
    gc.anchor = GridBagConstraints.CENTER;
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      final ElwhaFab fab =
          ElwhaFab.extended(MaterialIcons.add(sizes[col].iconPx()), "Compose")
              .setFabSize(sizes[col]);
      fab.setEnabled(false);
      grid.add(fab, gc);
    }

    return grid;
  }

  private JPanel buildLivePanel() {
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 16));
    panel.setBorder(BorderFactory.createTitledBorder("Live — Tab to focus, Space/Enter to fire"));
    for (ElwhaFab.Size size : ElwhaFab.Size.values()) {
      final ElwhaFab fab =
          ElwhaFab.extended(MaterialIcons.add(size.iconPx()), "Compose").setFabSize(size);
      fab.addActionListener(
          e ->
              System.out.println(
                  "FAB activated: "
                      + size.name()
                      + " text=\""
                      + fab.getText()
                      + "\" modifiers="
                      + e.getModifiers()));
      panel.add(fab);
    }
    return panel;
  }

  private JLabel boldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
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
