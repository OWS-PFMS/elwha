package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S5 keyboard demo (story #421) — the focus-order proof: a button, a vertical 4-member group (one
 * member disabled), a horizontal RTL-toggleable group, and a closing button. Tab crosses each group
 * as <em>one</em> stop (the roving rules), arrows move <em>and select</em> with wrap + disabled
 * skip, and the RTL checkbox flips the horizontal group's Left/Right sense live.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioGroupKeyboardDemo {

  private final JFrame frame = new JFrame("ElwhaRadioGroup — S5 keyboard / roving tab stop");
  private final JLabel readout = new JLabel(" ", SwingConstants.CENTER);

  private ElwhaRadioGroupKeyboardDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaRadioGroupKeyboardDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout(0, 8));

    final JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));

    final ElwhaRadioGroup vertical = new ElwhaRadioGroup();
    final JPanel verticalPane = new JPanel(new GridLayout(4, 1, 0, 4));
    verticalPane.setBorder(BorderFactory.createTitledBorder("Vertical (third disabled)"));
    final String[] verticalNames = {"first", "second", "third (disabled)", "fourth"};
    for (int i = 0; i < verticalNames.length; i++) {
      final ElwhaRadioButton radio = new ElwhaRadioButton(i == 0);
      if (i == 2) {
        radio.setEnabled(false);
      }
      vertical.add(radio);
      wireReadout(vertical, radio, "vertical/" + verticalNames[i]);
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
      row.add(radio);
      row.add(new JLabel(verticalNames[i]));
      verticalPane.add(row);
    }
    center.add(verticalPane);

    final ElwhaRadioGroup horizontal = new ElwhaRadioGroup();
    final JPanel horizontalRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 16));
    final JPanel horizontalPane = new JPanel(new BorderLayout());
    horizontalPane.setBorder(BorderFactory.createTitledBorder("Horizontal (RTL-toggleable)"));
    final String[] horizontalNames = {"small", "medium", "large"};
    final ElwhaRadioButton[] horizontalRadios = new ElwhaRadioButton[horizontalNames.length];
    for (int i = 0; i < horizontalNames.length; i++) {
      horizontalRadios[i] = new ElwhaRadioButton(i == 0);
      horizontal.add(horizontalRadios[i]);
      wireReadout(horizontal, horizontalRadios[i], "horizontal/" + horizontalNames[i]);
      horizontalRow.add(horizontalRadios[i]);
      horizontalRow.add(new JLabel(horizontalNames[i]));
    }
    final JCheckBox rtl = new JCheckBox("Right-to-left orientation");
    rtl.addActionListener(
        e -> {
          final ComponentOrientation orientation =
              rtl.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          for (final ElwhaRadioButton radio : horizontalRadios) {
            radio.setComponentOrientation(orientation);
          }
          horizontalRow.setComponentOrientation(orientation);
          horizontalRow.revalidate();
        });
    horizontalPane.add(horizontalRow, BorderLayout.CENTER);
    horizontalPane.add(rtl, BorderLayout.SOUTH);
    center.add(horizontalPane);

    frame.add(new JButton("Before the groups (Tab from here)"), BorderLayout.NORTH);
    frame.add(center, BorderLayout.CENTER);
    final JPanel south = new JPanel(new BorderLayout());
    south.add(readout, BorderLayout.NORTH);
    south.add(new JButton("After the groups"), BorderLayout.SOUTH);
    frame.add(south, BorderLayout.SOUTH);
    frame.setMinimumSize(new java.awt.Dimension(720, 360));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void wireReadout(
      final ElwhaRadioGroup group, final ElwhaRadioButton radio, final String name) {
    radio.addActionListener(e -> readout.setText("user selected: " + name));
  }
}
