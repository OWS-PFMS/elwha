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
 * Story #189 (S3) smoketest — Standard FAB state model. The static panel pre-renders the four M3
 * states (Enabled / Hovered / Focused / Pressed) for one canonical FAB so a visual reviewer can
 * compare the resting / hover-elevation-bump / focus-ring / press-state-layer treatments side by
 * side without driving the cursor and keyboard. The live panel exercises the real interaction path
 * — mouse hover, Tab focus, mouse and Space/Enter press (with the {@link
 * com.owspfm.elwha.theme.RipplePainter} ripple) — and prints click events so an a11y screen reader
 * can be validated against the same artifact.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabStatesPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabStatesPlayground {

  private final JFrame frame = new JFrame("ElwhaFab — S3 states (#189)");

  private ElwhaFabStatesPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaFabStatesPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    final JPanel body = new JPanel(new BorderLayout());
    body.add(new JScrollPane(buildStaticStateMatrix()), BorderLayout.CENTER);
    body.add(buildLivePanel(), BorderLayout.SOUTH);
    frame.add(body, BorderLayout.CENTER);
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.setSize(900, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildStaticStateMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 24, 8, 24);
    gc.anchor = GridBagConstraints.CENTER;

    final String[] stateLabels = {"Enabled", "Hovered", "Pressed", "Disabled"};
    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();

    gc.gridy = 0;
    gc.gridx = 0;
    grid.add(boldLabel("Size \\ State"), gc);
    for (int col = 0; col < stateLabels.length; col++) {
      gc.gridx = col + 1;
      grid.add(boldLabel(stateLabels[col]), gc);
    }

    for (int row = 0; row < sizes.length; row++) {
      gc.gridy = row + 1;
      gc.gridx = 0;
      gc.anchor = GridBagConstraints.WEST;
      grid.add(new JLabel(sizes[row].name()), gc);
      gc.anchor = GridBagConstraints.CENTER;

      gc.gridx = 1;
      grid.add(makeFab(sizes[row], false, false, true), gc);
      gc.gridx = 2;
      grid.add(makeFab(sizes[row], true, false, true), gc);
      gc.gridx = 3;
      grid.add(makeFab(sizes[row], false, true, true), gc);
      gc.gridx = 4;
      grid.add(makeFab(sizes[row], false, false, false), gc);
    }
    return grid;
  }

  private ElwhaFab makeFab(
      final ElwhaFab.Size size,
      final boolean hovered,
      final boolean pressed,
      final boolean enabled) {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add(size.iconPx())).setFabSize(size);
    fab.setEnabled(enabled);
    fab.setHovered(hovered);
    fab.setPressed(pressed);
    fab.setToolTipText(size.name() + " — static preview");
    return fab;
  }

  private JPanel buildLivePanel() {
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 16));
    panel.setBorder(BorderFactory.createTitledBorder("Live — Tab to focus, Space/Enter to fire"));
    for (ElwhaFab.Size size : ElwhaFab.Size.values()) {
      final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add(size.iconPx())).setFabSize(size);
      fab.setToolTipText("Create (" + size.name() + ")");
      fab.addActionListener(
          e ->
              System.out.println(
                  "FAB activated: " + size.name() + " modifiers=" + e.getModifiers()));
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
