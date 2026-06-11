package com.owspfm.elwha.checkbox;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Dedicated keyboard + accessibility demo for {@link ElwhaCheckbox} (story #413 follow-up from the
 * smoke loop) — exercises Tab/Shift-Tab traversal with the keyboard-only focus-visible layer, Space
 * toggling, and the accessibility contract <em>live</em>: a focus-tracking inspector renders the
 * focused checkbox's accessible role / name / states / description on every change, and an event
 * log proves {@code ACCESSIBLE_STATE_PROPERTY} fires on checked / indeterminate transitions (what a
 * screen reader hears). The RTL toggle (an {@link ElwhaButton}) flips the
 * box-trailing/label-leading mirroring.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class CheckboxKeyboardA11yDemo {

  private final JFrame frame = new JFrame("ElwhaCheckbox — keyboard + a11y + RTL");
  private final JPanel column = new JPanel(new GridLayout(0, 1, 0, 4));
  private final JLabel inspector = new JLabel(" ");
  private final JTextArea log = new JTextArea(6, 48);
  private boolean rightToLeft;
  private int eventCount;

  private CheckboxKeyboardA11yDemo() {}

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
    SwingUtilities.invokeLater(() -> new CheckboxKeyboardA11yDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    column.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));

    column.add(tracked(new ElwhaCheckbox("Email notifications")));
    final ElwhaCheckbox checked = new ElwhaCheckbox("Remember this device");
    checked.setChecked(true);
    column.add(tracked(checked));
    final ElwhaCheckbox parent = new ElwhaCheckbox("Select all (starts indeterminate)");
    parent.setIndeterminate(true);
    column.add(tracked(parent));
    final ElwhaCheckbox bare = new ElwhaCheckbox();
    bare.setAccessibleLabel("Bare checkbox (accessible label only)");
    final JPanel bareRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
    bareRow.add(tracked(bare));
    bareRow.add(new JLabel("← label-less; name comes from setAccessibleLabel"));
    column.add(bareRow);
    final ElwhaCheckbox error = new ElwhaCheckbox("Error state (described as \"Error\")");
    error.setErrorShown(true);
    column.add(tracked(error));
    final ElwhaCheckbox disabled = new ElwhaCheckbox("Disabled — skipped by Tab");
    disabled.setEnabled(false);
    column.add(tracked(disabled));

    final ElwhaButton rtlToggle = ElwhaButton.outlinedButton("Orientation: LTR");
    rtlToggle.addActionListener(
        e -> {
          rightToLeft = !rightToLeft;
          column.applyComponentOrientation(
              rightToLeft
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          column.revalidate();
          column.repaint();
          rtlToggle.setText("Orientation: " + (rightToLeft ? "RTL" : "LTR"));
        });
    final JLabel hint =
        new JLabel("Tab/Shift-Tab to traverse (focus layer is keyboard-only) · Space toggles");
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.add(rtlToggle);
    top.add(hint);

    inspector.setBorder(BorderFactory.createTitledBorder("Focused checkbox — AccessibleContext"));
    log.setEditable(false);
    final JScrollPane logScroll = new JScrollPane(log);
    logScroll.setBorder(BorderFactory.createTitledBorder("ACCESSIBLE_STATE_PROPERTY events"));
    final JPanel south = new JPanel(new BorderLayout());
    south.add(inspector, BorderLayout.NORTH);
    south.add(logScroll, BorderLayout.CENTER);
    south.setPreferredSize(new Dimension(100, 200));

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener(
            "focusOwner",
            e -> {
              if (e.getNewValue() instanceof ElwhaCheckbox box) {
                renderInspector(box);
              }
            });

    frame.add(top, BorderLayout.NORTH);
    frame.add(column, BorderLayout.CENTER);
    frame.add(south, BorderLayout.SOUTH);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /** Wires the accessible-event log + inspector refresh onto a demo checkbox. */
  private ElwhaCheckbox tracked(final ElwhaCheckbox box) {
    final AccessibleContext ctx = box.getAccessibleContext();
    ctx.addPropertyChangeListener(
        e -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(e.getPropertyName())) {
            eventCount++;
            log.append(
                eventCount
                    + ". ["
                    + nameOf(ctx)
                    + "] state "
                    + (e.getOldValue() == null ? "+" + e.getNewValue() : "-" + e.getOldValue())
                    + "\n");
            log.setCaretPosition(log.getDocument().getLength());
          }
        });
    box.addPropertyChangeListener(
        ElwhaCheckbox.PROPERTY_CHECK_STATE,
        e -> {
          if (box.isFocusOwner()) {
            renderInspector(box);
          }
        });
    return box;
  }

  private void renderInspector(final ElwhaCheckbox box) {
    final AccessibleContext ctx = box.getAccessibleContext();
    final String desc = ctx.getAccessibleDescription();
    inspector.setText(
        "<html>role=<b>"
            + ctx.getAccessibleRole()
            + "</b> · name=<b>"
            + nameOf(ctx)
            + "</b> · states=<b>"
            + ctx.getAccessibleStateSet()
            + "</b>"
            + (desc == null ? "" : " · description=<b>" + desc + "</b>")
            + "</html>");
  }

  private static String nameOf(final AccessibleContext ctx) {
    final String name = ctx.getAccessibleName();
    return name == null ? "(unnamed)" : name;
  }
}
