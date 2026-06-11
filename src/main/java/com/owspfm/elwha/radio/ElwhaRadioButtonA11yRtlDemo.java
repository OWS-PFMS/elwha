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
 * S6 a11y demo (story #422) — three panes with a live accessible-tree readout (focus any radio and
 * the bar shows its accessible name, role, CHECKED state, value, and MEMBER_OF target count exactly
 * as assistive tech would query them):
 *
 * <ol>
 *   <li><strong>Built-in label, LTR</strong> — {@code setLabel} text is part of the radio: it
 *       extends the click target (click the words) and names the radio.
 *   <li><strong>Built-in label, RTL</strong> — the same clickable label under a right-to-left
 *       {@code ComponentOrientation}: the icon block pins to the right edge, the text paints to its
 *       left, and the whole row stays clickable.
 *   <li><strong>{@code JLabel.setLabelFor} fallback</strong> — an external label provides the
 *       accessible <em>name only</em>; it is deliberately <em>not</em> part of the radio's click
 *       target (Swing's {@code labelFor} has no click forwarding — the same contract as {@code
 *       ElwhaCheckbox}).
 * </ol>
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

    final JPanel panes = new JPanel(new GridLayout(1, 3, 12, 0));
    panes.add(
        builtInPane(
            "Built-in label (clickable)",
            ComponentOrientation.LEFT_TO_RIGHT,
            "Comfortable",
            "Cozy",
            "Compact"));
    panes.add(
        builtInPane(
            "Built-in label — RTL (clickable)",
            ComponentOrientation.RIGHT_TO_LEFT,
            "First",
            "Middle",
            "Last"));
    panes.add(labelForPane());

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
    frame.setMinimumSize(new java.awt.Dimension(980, 300));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /** A group of radios carrying built-in (clickable) labels under the given orientation. */
  private static JPanel builtInPane(
      final String title, final ComponentOrientation orientation, final String... options) {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final JPanel pane = new JPanel(new GridLayout(options.length, 1, 0, 4));
    pane.setBorder(BorderFactory.createTitledBorder(title));
    for (final String option : options) {
      final ElwhaRadioButton radio = new ElwhaRadioButton(option);
      radio.setComponentOrientation(orientation);
      group.add(radio);
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
      row.setComponentOrientation(orientation);
      row.add(radio);
      pane.add(row);
    }
    group.setSelected(group.getMembers().get(0));
    return pane;
  }

  /** The external-label fallback: names the radio for assistive tech, forwards no clicks. */
  private JPanel labelForPane() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final JPanel pane = new JPanel(new GridLayout(3, 1, 0, 4));
    pane.setBorder(
        BorderFactory.createTitledBorder("JLabel.setLabelFor — name only (NOT clickable)"));
    for (final String option : new String[] {"Alpha", "Beta", "Gamma"}) {
      final ElwhaRadioButton radio = new ElwhaRadioButton();
      group.add(radio);
      final JLabel rowLabel = new JLabel(option);
      rowLabel.setLabelFor(radio);
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
      row.add(radio);
      row.add(rowLabel);
      pane.add(row);
    }
    group.setSelected(group.getMembers().get(0));
    return pane;
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
