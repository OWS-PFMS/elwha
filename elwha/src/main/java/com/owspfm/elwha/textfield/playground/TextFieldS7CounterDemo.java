package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.textfield.ElwhaTextField.SupportingTextVisibility;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S7 demo (#363): the character counter + supporting-text visibility mode. The left panel is a live
 * field — step the {@code maxLength} with the +/- buttons to show a {@code used/total} counter,
 * type past the limit to watch it turn the error color (the input is <i>not</i> truncated), and
 * flip the visibility picker between <i>Always</i> and <i>On focus</i> to reveal/hide the
 * supporting row's advisory content on blur (error text and an over-limit counter always show). The
 * right panel is a static reference grid: under-limit / over-limit and Always / On-focus, both
 * variants.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS7CounterDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS7CounterDemo {

  private TextFieldS7CounterDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());

          final JFrame frame = new JFrame("ElwhaTextField — S7 counter / supporting visibility");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          frame.setLayout(new BorderLayout(24, 0));
          ((JPanel) frame.getContentPane())
              .setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

          frame.add(buildInteractive(), BorderLayout.WEST);
          frame.add(buildReferenceGrid(), BorderLayout.CENTER);

          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }

  private static JComponent buildInteractive() {
    final ElwhaTextField field = ElwhaTextField.outlined("Bio");
    field.setPlaceholder("Tell us about yourself");
    field.setSupportingText("A short blurb for your profile");
    field.setMaxLength(20);
    field.setPreferredSize(new Dimension(300, field.getPreferredSize().height));

    final JPanel fieldHost = new JPanel(new BorderLayout());
    fieldHost.setOpaque(false);
    fieldHost.add(field, BorderLayout.NORTH);

    final ElwhaButtonGroup variantGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS);
    variantGroup.add(new ElwhaButton("Filled"));
    variantGroup.add(new ElwhaButton("Outlined"));
    variantGroup.setSelectedIndex(1);
    variantGroup.addSelectionListener(
        g -> {
          field.setVariant(g.getSelectedIndex() == 0 ? Variant.FILLED : Variant.OUTLINED);
          fieldHost.revalidate();
        });

    final ElwhaButtonGroup visibilityGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS);
    visibilityGroup.add(new ElwhaButton("Always"));
    visibilityGroup.add(new ElwhaButton("On focus"));
    visibilityGroup.setSelectedIndex(0);
    visibilityGroup.addSelectionListener(
        g ->
            field.setSupportingTextVisibility(
                g.getSelectedIndex() == 0
                    ? SupportingTextVisibility.ALWAYS
                    : SupportingTextVisibility.ON_FOCUS));

    final JLabel maxValue = new JLabel("maxLength: " + field.getMaxLength());
    final ElwhaButton maxDown = ElwhaButton.outlinedButton("−");
    final ElwhaButton maxUp = ElwhaButton.outlinedButton("+");
    maxDown.addActionListener(
        e -> {
          field.setMaxLength(Math.max(-1, field.getMaxLength() - 5));
          maxValue.setText("maxLength: " + field.getMaxLength());
        });
    maxUp.addActionListener(
        e -> {
          field.setMaxLength(Math.max(0, field.getMaxLength()) + 5);
          maxValue.setText("maxLength: " + field.getMaxLength());
        });
    final JPanel maxRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    maxRow.setOpaque(false);
    maxRow.add(maxDown);
    maxRow.add(maxUp);
    maxRow.add(maxValue);

    final JPanel controls = new JPanel(new GridBagLayout());
    controls.setOpaque(false);
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 0, 6, 8);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx = 0;
    gbc.gridy = 0;
    controls.add(new JLabel("Variant"), gbc);
    gbc.gridx = 1;
    controls.add(variantGroup, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    controls.add(new JLabel("Max length"), gbc);
    gbc.gridx = 1;
    controls.add(maxRow, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    controls.add(new JLabel("Supporting text"), gbc);
    gbc.gridx = 1;
    controls.add(visibilityGroup, gbc);

    final JPanel panel = new JPanel(new BorderLayout(0, 20));
    panel.setOpaque(false);
    panel.add(controls, BorderLayout.NORTH);
    panel.add(fieldHost, BorderLayout.CENTER);
    return panel;
  }

  private static JComponent buildReferenceGrid() {
    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 16));
    grid.setOpaque(false);
    for (final Variant v : Variant.values()) {
      grid.add(
          refRow(v, "under limit (12/20)", "Twelve chars", 20, SupportingTextVisibility.ALWAYS));
      grid.add(
          refRow(
              v,
              "over limit (24/20)",
              "This text is over the cap",
              20,
              SupportingTextVisibility.ALWAYS));
      grid.add(refRow(v, "On focus — blank on blur", "", 20, SupportingTextVisibility.ON_FOCUS));
    }
    return grid;
  }

  private static JComponent refRow(
      final Variant variant,
      final String caption,
      final String text,
      final int maxLength,
      final SupportingTextVisibility visibility) {
    final ElwhaTextField field = new ElwhaTextField(variant, captionLabel(variant));
    field.setSupportingText("Supporting text");
    field.setMaxLength(maxLength);
    field.setSupportingTextVisibility(visibility);
    field.setText(text);
    field.setPreferredSize(new Dimension(280, field.getPreferredSize().height));

    final JPanel row = new JPanel(new BorderLayout(0, 2));
    row.setOpaque(false);
    final JLabel head = new JLabel(variant + " — " + caption);
    head.putClientProperty("FlatLaf.styleClass", "small");
    row.add(head, BorderLayout.NORTH);
    final JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    host.add(field, BorderLayout.WEST);
    row.add(host, BorderLayout.CENTER);
    return row;
  }

  private static String captionLabel(final Variant variant) {
    return variant == Variant.FILLED ? "Filled field" : "Outlined field";
  }
}
