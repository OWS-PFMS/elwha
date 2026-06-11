package com.owspfm.elwha.radio;

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
 * S4 group demo (story #420) — two independent {@link ElwhaRadioGroup}s ("Sort by" and "View")
 * proving mutual exclusion, group isolation, and the group {@code ChangeListener}: each pane's
 * readout reflects {@code getSelected()} live, including the empty selection after "Clear".
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioGroupDemo {

  private final JFrame frame = new JFrame("ElwhaRadioGroup — S4 mutual exclusion");
  private boolean dark;

  private ElwhaRadioGroupDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaRadioGroupDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new GridLayout(1, 2, 12, 0));
    frame.add(groupPane("Sort by", "Name", "Date modified", "Size"));
    frame.add(groupPane("View", "List", "Grid"));
    frame.setMinimumSize(new java.awt.Dimension(640, 320));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel groupPane(final String title, final String... options) {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();

    final JPanel rows = new JPanel(new GridLayout(options.length, 1, 0, 4));
    for (final String option : options) {
      final ElwhaRadioButton radio = new ElwhaRadioButton(option);
      group.add(radio);
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
      row.add(radio);
      rows.add(row);
    }

    final JLabel readout = new JLabel("selected: (none)", SwingConstants.CENTER);
    group.addChangeListener(
        e -> {
          final ElwhaRadioButton current = group.getSelected();
          readout.setText("selected: " + (current == null ? "(none)" : current.getLabel()));
        });
    group.setSelected(group.getMembers().get(0));

    final JButton clear = new JButton("Clear");
    clear.addActionListener(e -> group.clearSelection());
    final JButton selectLast = new JButton("Select last (programmatic)");
    selectLast.addActionListener(
        e -> group.setSelected(group.getMembers().get(group.getMembers().size() - 1)));
    final JButton mode = new JButton("Light / dark");
    mode.addActionListener(
        e -> {
          dark = !dark;
          ElwhaTheme.install(ElwhaTheme.current().withMode(dark ? Mode.DARK : Mode.LIGHT));
        });
    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
    controls.add(clear);
    controls.add(selectLast);
    controls.add(mode);

    final JPanel pane = new JPanel(new BorderLayout(0, 8));
    pane.setBorder(BorderFactory.createTitledBorder(title));
    pane.add(rows, BorderLayout.CENTER);
    final JPanel south = new JPanel(new BorderLayout());
    south.add(readout, BorderLayout.NORTH);
    south.add(controls, BorderLayout.SOUTH);
    pane.add(south, BorderLayout.SOUTH);
    return pane;
  }
}
