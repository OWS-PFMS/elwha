package com.owspfm.elwha.checkbox;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S2 demo (story #412) — {@link ElwhaCheckbox} live interaction: hover / focus / pressed state
 * layers with the M3 cross-color pressed rule, the centered circular ripple, Space toggling, the
 * mark draw-in / retract motion, the indeterminate-exits-to-checked cycle, and an action-listener
 * log proving user toggles fire while programmatic sets don't.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class CheckboxInteractionDemo {

  private final JFrame frame = new JFrame("ElwhaCheckbox — S2 interaction & motion");
  private final JTextArea log = new JTextArea(6, 40);
  private Mode mode = Mode.LIGHT;
  private int eventCount;

  private CheckboxInteractionDemo() {}

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
    SwingUtilities.invokeLater(() -> new CheckboxInteractionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 8));
    rows.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
    rows.add(row("Click / Tab+Space me", checkbox(CheckState.UNCHECKED, true, "unchecked")));
    rows.add(row("Starts checked", checkbox(CheckState.CHECKED, true, "checked")));
    rows.add(
        row(
            "Starts indeterminate — one click lands on checked",
            checkbox(CheckState.INDETERMINATE, true, "indeterminate")));
    rows.add(row("Disabled — inert", checkbox(CheckState.CHECKED, false, "disabled")));

    final ElwhaCheckbox programmaticTarget = checkbox(CheckState.UNCHECKED, true, "programmatic");
    final ElwhaButton programmatic = ElwhaButton.outlinedButton("Programmatic cycle →");
    programmatic.addActionListener(
        e ->
            programmaticTarget.setCheckState(
                switch (programmaticTarget.getCheckState()) {
                  case UNCHECKED -> CheckState.CHECKED;
                  case CHECKED -> CheckState.INDETERMINATE;
                  case INDETERMINATE -> CheckState.UNCHECKED;
                }));
    final JPanel programmaticRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
    programmaticRow.add(programmatic);
    programmaticRow.add(programmaticTarget);
    programmaticRow.add(new JLabel("(property log only — no action event)"));
    rows.add(programmaticRow);

    final ElwhaButton modeToggle = ElwhaButton.outlinedButton("Toggle dark mode");
    modeToggle.addActionListener(e -> toggleMode());
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(modeToggle);

    log.setEditable(false);
    final JScrollPane logScroll = new JScrollPane(log);
    logScroll.setBorder(BorderFactory.createTitledBorder("Listener log"));
    logScroll.setPreferredSize(new Dimension(100, 140));

    frame.add(top, BorderLayout.NORTH);
    frame.add(rows, BorderLayout.CENTER);
    frame.add(logScroll, BorderLayout.SOUTH);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private ElwhaCheckbox checkbox(final CheckState state, final boolean enabled, final String tag) {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(state);
    box.setEnabled(enabled);
    box.addActionListener(e -> append("[" + tag + "] ACTION → " + box.getCheckState()));
    box.addPropertyChangeListener(
        ElwhaCheckbox.PROPERTY_CHECK_STATE,
        e -> append("[" + tag + "] property " + e.getOldValue() + " → " + e.getNewValue()));
    return box;
  }

  private static JPanel row(final String text, final ElwhaCheckbox box) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
    row.add(box);
    row.add(new JLabel(text));
    return row;
  }

  private void append(final String line) {
    eventCount++;
    log.append(eventCount + ". " + line + "\n");
    log.setCaretPosition(log.getDocument().getLength());
  }

  private void toggleMode() {
    mode = (mode == Mode.LIGHT) ? Mode.DARK : Mode.LIGHT;
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    frame.repaint();
  }
}
