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
 * Phase-1 / S2 playground (story #403) — {@link ElwhaSwitch} interaction: click anywhere to toggle,
 * drag the handle across the track (release commits to the nearest half), Tab to a switch and
 * toggle with Space (press &rarr; grow, release &rarr; commit), and watch the hover/focus/pressed
 * state layers + press ripple. The readout separates {@code ActionListener} fires (user gestures
 * only) from {@code ChangeListener} fires — the dogfooded {@link ElwhaButton} flips the first
 * switch programmatically and must bump only the change count.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchInteractionDemo {

  private final JFrame frame = new JFrame("ElwhaSwitch — Phase 1 / S2 interaction");

  private ElwhaSwitchInteractionDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSwitchInteractionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSwitch wifi = new ElwhaSwitch(true);
    final ElwhaSwitch bluetooth = new ElwhaSwitch();
    final ElwhaSwitch hotspot = new ElwhaSwitch();
    final ElwhaSwitch airplane = new ElwhaSwitch(true);
    airplane.setEnabled(false);

    final JLabel actionReadout = new JLabel("action events: 0 (last: —)");
    final JLabel changeReadout = new JLabel("change events: 0");
    final int[] actions = {0};
    final int[] changes = {0};
    for (final ElwhaSwitch s : new ElwhaSwitch[] {wifi, bluetooth, hotspot, airplane}) {
      s.addActionListener(
          e ->
              actionReadout.setText(
                  "action events: " + ++actions[0] + " (last: " + e.getActionCommand() + ")"));
      s.addChangeListener(e -> changeReadout.setText("change events: " + ++changes[0]));
    }

    final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 12));
    rows.setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
    rows.add(row("Wi-Fi (click, drag, or Space)", wifi));
    rows.add(row("Bluetooth", bluetooth));
    rows.add(row("Hotspot — try dragging the handle partway", hotspot));
    rows.add(row("Airplane mode (disabled, selected)", airplane));

    final ElwhaButton programmatic = ElwhaButton.filledTonalButton("Flip Wi-Fi programmatically");
    programmatic.addActionListener(e -> wifi.setSelected(!wifi.isSelected()));

    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    top.add(programmatic);
    top.add(actionReadout);
    top.add(changeReadout);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rows, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(640, 340));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel row(final String text, final ElwhaSwitch elwhaSwitch) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 0));
    row.add(elwhaSwitch);
    row.add(new JLabel(text));
    return row;
  }
}
