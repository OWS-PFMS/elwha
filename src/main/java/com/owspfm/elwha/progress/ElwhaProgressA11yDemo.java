package com.owspfm.elwha.progress;

import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleValue;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Throwaway S6 playground (story #474) — proves the progress-indicator accessibility contract
 * live: the {@code PROGRESS_BAR} role, the model-backed {@code AccessibleValue} readout (and its
 * disappearance while indeterminate — the BUSY state stands in), accessible naming, and RTL
 * mirroring across flat/wavy linear bars. The readout label re-reads the accessible context on
 * every change, so what you see is literally what assistive tech is told.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaProgressA11yDemo {

  private final JFrame frame = new JFrame("ElwhaProgressIndicator — S6 a11y + RTL");
  private final JLabel readout = new JLabel();
  private ElwhaLinearProgressIndicator linear;
  private ElwhaLinearProgressIndicator wavyLinear;
  private ElwhaCircularProgressIndicator circular;

  private ElwhaProgressA11yDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaProgressA11yDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    linear = new ElwhaLinearProgressIndicator(0, 100, 60);
    linear.getAccessibleContext().setAccessibleName("Download progress");
    wavyLinear = ElwhaLinearProgressIndicator.wavy();
    wavyLinear.setValue(60);
    wavyLinear.getAccessibleContext().setAccessibleName("Sync progress");
    circular = new ElwhaCircularProgressIndicator(0, 100, 60);
    circular.getAccessibleContext().setAccessibleName("Upload progress");

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 18));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
    grid.add(labeled("linear “Download progress”", linear));
    grid.add(labeled("wavy “Sync progress”", wavyLinear));
    final JPanel circularRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
    circularRow.add(circular);
    grid.add(labeled("circular “Upload progress”", circularRow));
    grid.add(readout);

    final ElwhaSwitch indeterminate = new ElwhaSwitch();
    indeterminate.setLabel("Indeterminate");
    indeterminate.addActionListener(
        e -> {
          final boolean on = indeterminate.isSelected();
          linear.setIndeterminate(on);
          wavyLinear.setIndeterminate(on);
          circular.setIndeterminate(on);
          updateReadout();
        });
    final ElwhaSwitch rtl = new ElwhaSwitch();
    rtl.setLabel("RTL");
    rtl.addActionListener(
        e -> {
          final ComponentOrientation orientation =
              rtl.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          linear.setComponentOrientation(orientation);
          wavyLinear.setComponentOrientation(orientation);
          frame.repaint();
        });
    linear.addChangeListener(e -> updateReadout());

    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    top.add(indeterminate);
    top.add(rtl);

    frame.add(top, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    updateReadout();
    frame.setMinimumSize(new Dimension(640, 420));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void updateReadout() {
    final AccessibleContext context = linear.getAccessibleContext();
    final AccessibleValue value = context.getAccessibleValue();
    readout.setText(
        "<html><b>assistive-tech view:</b> role="
            + context.getAccessibleRole()
            + " · name=“"
            + context.getAccessibleName()
            + "” · value="
            + value.getCurrentAccessibleValue()
            + " · range=["
            + value.getMinimumAccessibleValue()
            + ", "
            + value.getMaximumAccessibleValue()
            + "] · states="
            + context.getAccessibleStateSet()
            + "</html>");
  }

  private static JPanel labeled(final String text, final java.awt.Component content) {
    final JPanel row = new JPanel(new BorderLayout(12, 0));
    final JLabel label = new JLabel(text);
    label.setPreferredSize(new Dimension(230, 20));
    row.add(label, BorderLayout.WEST);
    row.add(content, BorderLayout.CENTER);
    return row;
  }
}
