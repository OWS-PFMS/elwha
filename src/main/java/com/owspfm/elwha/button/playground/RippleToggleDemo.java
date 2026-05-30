package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #289 (S1, epic #288) smoketest — the {@link ElwhaButton#setRippleEnabled(boolean)} / {@link
 * ElwhaIconButton#setRippleEnabled(boolean)} press-ripple toggle.
 *
 * <p>Validate, for both the text button and the icon button:
 *
 * <ul>
 *   <li><b>Ripple ON</b> (left column, the default): press → an expanding ripple seeds at the click
 *       point and fades; keyboard activation (Space/Enter while focused) seeds a centered ripple.
 *   <li><b>Ripple OFF</b> (right column): press → <em>no</em> ripple, but the pressed state-layer
 *       darken still appears while held (the toggle gates only the ripple, not the press feedback).
 *   <li><b>Live toggle</b> (bottom): flip the checkbox to disable the ripple on the wired button.
 *       Hold the button to start a ripple, then untick the box mid-stroke — the in-flight ripple
 *       clears immediately rather than freezing.
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.button.playground.RippleToggleDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class RippleToggleDemo {

  private final JFrame frame = new JFrame("Press-ripple toggle — S1 (#289)");

  private RippleToggleDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new RippleToggleDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout(0, 24));

    frame.add(header(), BorderLayout.NORTH);
    frame.add(comparison(), BorderLayout.CENTER);
    frame.add(liveToggle(), BorderLayout.SOUTH);

    frame.setSize(560, 460);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JLabel header() {
    final JLabel label =
        new JLabel(
            "<html><div style='text-align:center'>Ripple ON keeps the default press ripple.<br>"
                + "Ripple OFF suppresses it — the pressed darken still shows.</div></html>",
            SwingConstants.CENTER);
    label.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
    return label;
  }

  private JPanel comparison() {
    final JPanel grid = new JPanel(new GridLayout(2, 2, 24, 24));
    grid.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

    grid.add(cell("Ripple ON", ElwhaButton.filledButton("Press me")));
    grid.add(cell("Ripple OFF", ElwhaButton.filledButton("Press me").setRippleEnabled(false)));
    grid.add(cell("Ripple ON", ElwhaIconButton.filledIconButton(MaterialIcons.favorite())));
    grid.add(
        cell(
            "Ripple OFF",
            ElwhaIconButton.filledIconButton(MaterialIcons.favorite()).setRippleEnabled(false)));
    return grid;
  }

  private JPanel cell(final String caption, final java.awt.Component button) {
    final JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.add(new JLabel(caption, SwingConstants.CENTER), BorderLayout.NORTH);
    final JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
    center.add(button);
    panel.add(center, BorderLayout.CENTER);
    return panel;
  }

  private JPanel liveToggle() {
    final ElwhaButton wired = ElwhaButton.filledTonalButton("Hold, then untick");
    final JCheckBox box = new JCheckBox("Ripple enabled", true);
    box.addActionListener(e -> wired.setRippleEnabled(box.isSelected()));

    final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 16));
    row.add(wired);
    row.add(box);
    return row;
  }
}
