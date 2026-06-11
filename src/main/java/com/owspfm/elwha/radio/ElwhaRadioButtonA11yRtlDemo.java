package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRelation;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S6 a11y demo (story #422) — labeled groups two ways ({@code setLabel} and {@code
 * JLabel.setLabelFor}) plus an RTL pane, with a live accessible-tree readout: focus any radio and
 * the bar shows its accessible name, role, CHECKED state, value, and MEMBER_OF target count exactly
 * as assistive tech would query them.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonA11yRtlDemo {

  private final JFrame frame = new JFrame("ElwhaRadioButton — S6 a11y / labels / RTL");
  private final JLabel readout = new JLabel("focus a radio…", SwingConstants.CENTER);

  private ElwhaRadioButtonA11yRtlDemo() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaRadioButtonA11yRtlDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout(0, 8));

    final JPanel panes = new JPanel(new GridLayout(1, 2, 12, 0));

    final ElwhaRadioGroup named = new ElwhaRadioGroup();
    final JPanel namedPane = new JPanel(new GridLayout(3, 1, 0, 4));
    namedPane.setBorder(BorderFactory.createTitledBorder("Built-in label (clickable)"));
    for (final String option : new String[] {"Comfortable", "Cozy", "Compact"}) {
      final ElwhaRadioButton radio = new ElwhaRadioButton(option);
      named.add(radio);
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
      row.add(radio);
      namedPane.add(row);
    }
    named.setSelected(named.getMembers().get(0));
    panes.add(namedPane);

    final ElwhaRadioGroup labeled = new ElwhaRadioGroup();
    final JPanel labeledPane = new JPanel(new GridLayout(3, 1, 0, 4));
    labeledPane.setBorder(
        BorderFactory.createTitledBorder("JLabel.setLabelFor naming + RTL orientation"));
    for (final String option : new String[] {"يمين", "وسط", "يسار"}) {
      final ElwhaRadioButton radio = new ElwhaRadioButton();
      radio.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
      labeled.add(radio);
      final JLabel rowLabel = new JLabel(option);
      rowLabel.setLabelFor(radio);
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
      row.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
      row.add(radio);
      row.add(rowLabel);
      labeledPane.add(row);
    }
    labeled.setSelected(labeled.getMembers().get(0));
    panes.add(labeledPane);

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener(
            "focusOwner",
            e -> {
              if (e.getNewValue() instanceof ElwhaRadioButton radio) {
                describe(radio);
              }
            });

    frame.add(panes, BorderLayout.CENTER);
    frame.add(readout, BorderLayout.SOUTH);
    frame.setMinimumSize(new java.awt.Dimension(720, 300));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void describe(final ElwhaRadioButton radio) {
    final AccessibleContext ac = radio.getAccessibleContext();
    final AccessibleRelation memberOf =
        ac.getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF);
    readout.setText(
        String.format(
            "name=\"%s\"  role=%s  checked=%s  value=%s  memberOf=%d radios",
            ac.getAccessibleName(),
            ac.getAccessibleRole(),
            ac.getAccessibleStateSet().contains(javax.accessibility.AccessibleState.CHECKED),
            ac.getAccessibleValue().getCurrentAccessibleValue(),
            memberOf == null ? 0 : memberOf.getTarget().length));
  }
}
