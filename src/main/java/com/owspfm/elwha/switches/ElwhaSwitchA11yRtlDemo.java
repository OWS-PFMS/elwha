package com.owspfm.elwha.switches;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase-1 / S5 playground (story #406) — {@link ElwhaSwitch} accessibility, labelling, and RTL. The
 * first row names its switch via {@code JLabel.setLabelFor}, the second via {@code
 * setLabel(String)}; the readout echoes the focused/toggled switch's accessible name, role, and
 * CHECKED state exactly as assistive tech would read them. The dogfooded {@link ElwhaButton} flips
 * the whole panel between LTR and RTL — under RTL the selected handle rests at the <em>left</em>
 * end and drags mirror (try dragging; the commit halves flip too).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchA11yRtlDemo {

  private final JFrame frame = new JFrame("ElwhaSwitch — Phase 1 / S5 a11y + RTL");

  private ElwhaSwitchA11yRtlDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaSwitchA11yRtlDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSwitch labelled = new ElwhaSwitch(true);
    final JLabel wifiLabel = new JLabel("Wi-Fi (labelFor association)");
    wifiLabel.setLabelFor(labelled);

    final ElwhaSwitch named = new ElwhaSwitch();
    named.setLabel("Bluetooth");

    final ElwhaSwitch icons = new ElwhaSwitch(true);
    icons.setIconsVisible(true);
    icons.setLabel("Location");

    final JLabel readout = new JLabel("toggle a switch to read its accessible state");
    for (final ElwhaSwitch s : new ElwhaSwitch[] {labelled, named, icons}) {
      s.addChangeListener(
          e -> {
            final AccessibleContext ax = s.getAccessibleContext();
            final boolean checked = ax.getAccessibleStateSet().contains(AccessibleState.CHECKED);
            readout.setText(
                "a11y: \""
                    + ax.getAccessibleName()
                    + "\" · "
                    + ax.getAccessibleRole().toDisplayString()
                    + " · "
                    + (checked ? "CHECKED" : "not checked"));
          });
    }

    final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 12));
    rows.setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
    rows.add(row(wifiLabel, labelled));
    rows.add(row(new JLabel("Bluetooth (setLabel name)"), named));
    rows.add(row(new JLabel("Location (icons, RTL keeps glyphs upright)"), icons));

    final ElwhaButton rtl = ElwhaButton.outlinedButton("Flip to RTL");
    rtl.addActionListener(
        e -> {
          final boolean toRtl = rows.getComponentOrientation().isLeftToRight();
          rows.applyComponentOrientation(
              toRtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
          rtl.setText(toRtl ? "Flip to LTR" : "Flip to RTL");
          rows.revalidate();
          rows.repaint();
        });

    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    top.add(rtl);
    top.add(readout);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rows, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(720, 320));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel row(final JLabel label, final ElwhaSwitch elwhaSwitch) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 0));
    row.add(elwhaSwitch);
    row.add(label);
    return row;
  }
}
