package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S1 chrome demo (story #417) — the four static {@link ElwhaRadioButton} cells (unselected/selected
 * &times; enabled/disabled) on a {@link ColorRole#SURFACE} ground with a light/dark toggle, plus a
 * {@code FlowLayout} row proving the 40&times;40 halo-inclusive preferred size lays out without
 * clipping.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonChromeDemo {

  private final JFrame frame = new JFrame("ElwhaRadioButton — S1 static chrome");
  private final JLabel readout = new JLabel(" ", SwingConstants.CENTER);
  private boolean dark;

  private ElwhaRadioButtonChromeDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaRadioButtonChromeDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel grid = new JPanel(new GridLayout(2, 4, 12, 4));
    grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
    grid.add(cell(new ElwhaRadioButton()));
    grid.add(cell(new ElwhaRadioButton(true)));
    grid.add(cell(disabled(new ElwhaRadioButton())));
    grid.add(cell(disabled(new ElwhaRadioButton(true))));
    grid.add(caption("Unselected"));
    grid.add(caption("Selected"));
    grid.add(caption("Disabled"));
    grid.add(caption("Disabled selected"));

    final JPanel flowRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    final ElwhaRadioButton flowA = new ElwhaRadioButton(true);
    final ElwhaRadioButton flowB = new ElwhaRadioButton();
    final ElwhaRadioButton flowC = new ElwhaRadioButton();
    flowA.addChangeListener(e -> updateReadout(flowA, flowB, flowC));
    flowB.addChangeListener(e -> updateReadout(flowA, flowB, flowC));
    flowC.addChangeListener(e -> updateReadout(flowA, flowB, flowC));
    flowRow.add(flowA);
    flowRow.add(flowB);
    flowRow.add(flowC);
    updateReadout(flowA, flowB, flowC);

    final JButton modeToggle = new JButton("Toggle light / dark");
    modeToggle.addActionListener(e -> switchMode());
    final JButton selectToggle = new JButton("setSelected sweep");
    selectToggle.addActionListener(
        e -> {
          flowA.setSelected(!flowA.isSelected());
          flowB.setSelected(!flowB.isSelected());
          flowC.setSelected(!flowC.isSelected());
        });
    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    controls.add(modeToggle);
    controls.add(selectToggle);

    final JPanel south = new JPanel(new BorderLayout());
    south.add(flowRow, BorderLayout.NORTH);
    south.add(readout, BorderLayout.CENTER);
    south.add(controls, BorderLayout.SOUTH);

    frame.add(grid, BorderLayout.CENTER);
    frame.add(south, BorderLayout.SOUTH);
    frame.setMinimumSize(new java.awt.Dimension(560, 320));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void updateReadout(final ElwhaRadioButton... radios) {
    final StringBuilder sb = new StringBuilder("selected: ");
    for (final ElwhaRadioButton radio : radios) {
      sb.append(radio.isSelected() ? "1" : "0");
    }
    readout.setText(sb.toString());
  }

  private void switchMode() {
    dark = !dark;
    ElwhaTheme.install(ElwhaTheme.current().withMode(dark ? Mode.DARK : Mode.LIGHT));
  }

  private static ElwhaRadioButton disabled(final ElwhaRadioButton radio) {
    radio.setEnabled(false);
    return radio;
  }

  private static JPanel cell(final ElwhaRadioButton radio) {
    final JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    cell.add(radio);
    return cell;
  }

  private static JLabel caption(final String text) {
    return new JLabel(text, SwingConstants.CENTER);
  }
}
