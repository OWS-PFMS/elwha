package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S2 interaction demo (story #418) — three <em>ungrouped</em> {@link ElwhaRadioButton}s (grouping
 * arrives in S4 #420) with an event log proving the interaction contract: click/Space select and
 * fire {@code ActionListener} once; re-clicking the selected radio fires nothing; programmatic
 * deselect fires {@code ChangeListener} only. Hover/focus/press show their state layers live —
 * watch the press tint swap: {@code PRIMARY} on an unselected radio, {@code ON_SURFACE} on a
 * selected one.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonInteractionDemo {

  private final JFrame frame = new JFrame("ElwhaRadioButton — S2 interaction");
  private final DefaultListModel<String> log = new DefaultListModel<>();
  private boolean dark;

  private ElwhaRadioButtonInteractionDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaRadioButtonInteractionDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaRadioButton[] radios = {
      new ElwhaRadioButton("alpha"),
      new ElwhaRadioButton("beta", true),
      new ElwhaRadioButton("gamma")
    };
    final String[] names = {"alpha", "beta", "gamma"};
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
    for (int i = 0; i < radios.length; i++) {
      final String name = names[i];
      final ElwhaRadioButton radio = radios[i];
      radio.addActionListener(e -> append(name + " action: " + e.getActionCommand()));
      radio.addChangeListener(
          e -> append(name + " change → " + (radio.isSelected() ? "selected" : "deselected")));
      row.add(radio);
    }

    final ElwhaRadioButton disabledRadio = new ElwhaRadioButton("disabled");
    disabledRadio.setEnabled(false);
    disabledRadio.addActionListener(e -> append("disabled action — MUST NEVER APPEAR"));
    row.add(disabledRadio);

    final JButton deselectAll = new JButton("setSelected(false) all (programmatic)");
    deselectAll.addActionListener(
        e -> {
          for (final ElwhaRadioButton radio : radios) {
            radio.setSelected(false);
          }
        });
    final JButton modeToggle = new JButton("Toggle light / dark");
    modeToggle.addActionListener(
        e -> {
          dark = !dark;
          ElwhaTheme.install(ElwhaTheme.current().withMode(dark ? Mode.DARK : Mode.LIGHT));
        });
    final JButton clear = new JButton("Clear log");
    clear.addActionListener(e -> log.clear());
    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    controls.add(deselectAll);
    controls.add(modeToggle);
    controls.add(clear);

    final JScrollPane logPane = new JScrollPane(new JList<>(log));
    logPane.setBorder(BorderFactory.createTitledBorder("Event log"));

    frame.add(row, BorderLayout.NORTH);
    frame.add(logPane, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setMinimumSize(new java.awt.Dimension(640, 400));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void append(final String line) {
    log.addElement(line);
  }
}
