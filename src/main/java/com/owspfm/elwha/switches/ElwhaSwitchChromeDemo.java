package com.owspfm.elwha.switches;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase-1 / S1 playground (story #402) — the {@link ElwhaSwitch} architecture-spike chrome
 * skeleton: the four static state cells (selected/unselected &times; enabled/disabled) painted from
 * tokens, with no interaction yet. The buttons dogfood {@link ElwhaButton}: one flips the top pair
 * programmatically (exercising {@code setSelected} + {@code ChangeListener}), one toggles
 * light/dark to prove the paint-time token resolve re-skins live.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchChromeDemo {

  private final JFrame frame = new JFrame("ElwhaSwitch — Phase 1 / S1 chrome skeleton");
  private boolean dark;

  private ElwhaSwitchChromeDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(() -> new ElwhaSwitchChromeDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSwitch off = new ElwhaSwitch();
    final ElwhaSwitch on = new ElwhaSwitch(true);
    final ElwhaSwitch disabledOff = new ElwhaSwitch();
    disabledOff.setEnabled(false);
    final ElwhaSwitch disabledOn = new ElwhaSwitch(true);
    disabledOn.setEnabled(false);

    final JLabel changeReadout = new JLabel("changes: 0");
    final int[] changes = {0};
    off.addChangeListener(e -> changeReadout.setText("changes: " + ++changes[0]));
    on.addChangeListener(e -> changeReadout.setText("changes: " + ++changes[0]));

    final JPanel grid = new JPanel(new GridLayout(0, 2, 24, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
    grid.add(labeled("unselected", off));
    grid.add(labeled("selected", on));
    grid.add(labeled("disabled unselected", disabledOff));
    grid.add(labeled("disabled selected (opaque SURFACE handle)", disabledOn));

    final ElwhaButton flip = ElwhaButton.filledTonalButton("Flip top pair");
    flip.addActionListener(
        e -> {
          off.setSelected(!off.isSelected());
          on.setSelected(!on.isSelected());
        });
    final ElwhaButton mode = ElwhaButton.outlinedButton("Dark mode");
    mode.addActionListener(
        e -> {
          dark = !dark;
          ElwhaTheme.install(ElwhaTheme.current().withMode(dark ? Mode.DARK : Mode.LIGHT));
          mode.setText(dark ? "Light mode" : "Dark mode");
        });

    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    top.add(flip);
    top.add(mode);
    top.add(changeReadout);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(640, 320));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel labeled(final String text, final ElwhaSwitch elwhaSwitch) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 0));
    row.add(elwhaSwitch);
    row.add(new JLabel(text));
    return row;
  }
}
