package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #192 (S6) smoketest — bidirectional Standard ↔ Extended {@link ElwhaFab#morphTo(
 * ElwhaFab.Form) morphTo()} morph at all three {@link ElwhaFab.Size sizes}. One FAB per size, each
 * paired with a Toggle button that flips it to the opposite form so the validator can observe the
 * three transitions (container width, icon translation, label opacity) running in parallel on the
 * shared 300 ms eased progress per design doc §9.1. A "Toggle all" control fires every morph
 * simultaneously to confirm the animator does not interfere across instances.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabMorphPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ElwhaFabMorphPlayground {

  private final JFrame frame = new JFrame("ElwhaFab — S6 Standard ↔ Extended morph (#192)");
  private final ElwhaFab[] fabs = new ElwhaFab[ElwhaFab.Size.values().length];

  private ElwhaFabMorphPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaFabMorphPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildControlBar(), BorderLayout.NORTH);
    frame.add(buildMatrix(), BorderLayout.CENTER);
    frame.setSize(960, 640);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(12, 24, 12, 24);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();

    gc.gridy = 0;
    gc.gridx = 0;
    grid.add(boldLabel("Size"), gc);
    gc.gridx = 1;
    grid.add(boldLabel("FAB"), gc);
    gc.gridx = 2;
    grid.add(boldLabel("Toggle"), gc);

    for (int row = 0; row < sizes.length; row++) {
      final int index = row;
      final ElwhaFab.Size size = sizes[row];
      // Constructed via extended(Icon, String) so both endpoints are reachable. Starts in Standard
      // form via the immediate morphTo call — the FAB renders at the Standard width on first paint
      // because the morph snaps without animation when the target equals the construction form's
      // opposite at t = 0 ... actually we just snap manually below to avoid an initial animation.
      final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(size.iconPx()), "Compose");
      fab.setFabSize(size);
      fabs[index] = fab;

      gc.gridy = row + 1;
      gc.gridx = 0;
      gc.anchor = GridBagConstraints.WEST;
      grid.add(new JLabel(size.name() + " · " + size.containerPx() + " dp"), gc);
      gc.anchor = GridBagConstraints.CENTER;

      gc.gridx = 1;
      grid.add(fab, gc);

      gc.gridx = 2;
      final ElwhaButton toggle = ElwhaButton.filledTonalButton(currentLabel(fab));
      toggle.addActionListener(
          e -> {
            final ElwhaFab.Form target =
                fab.getForm() == ElwhaFab.Form.EXTENDED
                    ? ElwhaFab.Form.STANDARD
                    : ElwhaFab.Form.EXTENDED;
            fab.morphTo(target);
            toggle.setText(currentLabel(fab));
          });
      grid.add(toggle, gc);
    }
    return grid;
  }

  private String currentLabel(final ElwhaFab fab) {
    return fab.getForm() == ElwhaFab.Form.EXTENDED ? "→ STANDARD" : "→ EXTENDED";
  }

  private JLabel boldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  private JPanel buildControlBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    final ElwhaButton toggleAll = ElwhaButton.filledTonalButton("Toggle all");
    toggleAll.addActionListener(
        e -> {
          for (ElwhaFab fab : fabs) {
            if (fab == null) {
              continue;
            }
            fab.morphTo(
                fab.getForm() == ElwhaFab.Form.EXTENDED
                    ? ElwhaFab.Form.STANDARD
                    : ElwhaFab.Form.EXTENDED);
          }
        });
    bar.add(toggleAll);

    bar.add(new JLabel("    Mode:"));
    final Mode[] modes = {Mode.LIGHT, Mode.DARK, Mode.SYSTEM};
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL);
    for (final Mode mode : modes) {
      group.add(new ElwhaButton(mode.name()));
    }
    final Mode current = ElwhaTheme.current().mode();
    for (int i = 0; i < modes.length; i++) {
      if (modes[i] == current) {
        group.setSelectedIndex(i);
      }
    }
    // Seeded before the listener is attached so the initial selection does not re-install the
    // theme on startup.
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
