package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.textfield.ElwhaTextField.InputMode;
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
 * S6 demo (#353): the multi-line / text-area editor. The left panel is a live field whose {@link
 * InputMode} and {@link Variant} flip via dogfooded {@link ElwhaButtonGroup} pickers — switch to
 * <i>multi-line</i> and type past one line to watch the field auto-grow and shift the layout, or to
 * <i>text-area</i> to get a fixed three-row box that scrolls internally; the row count steps with
 * the +/- buttons. The right panel is a static reference grid of both variants across all three
 * modes.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS6MultilineDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS6MultilineDemo {

  private TextFieldS6MultilineDemo() {}

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

          final JFrame frame = new JFrame("ElwhaTextField — S6 multi-line / text-area");
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
    final ElwhaTextField field = ElwhaTextField.outlined("Notes");
    field.setPlaceholder("Type a few lines…");
    field.setSupportingText("Switch the mode, then type past one line");
    field.setPreferredSize(new Dimension(280, field.getPreferredSize().height));

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

    final ElwhaButtonGroup modeGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS);
    modeGroup.add(new ElwhaButton("Single"));
    modeGroup.add(new ElwhaButton("Multi-line"));
    modeGroup.add(new ElwhaButton("Text area"));
    modeGroup.setSelectedIndex(0);
    modeGroup.addSelectionListener(
        g -> {
          field.setInputMode(InputMode.values()[g.getSelectedIndex()]);
          fieldHost.revalidate();
        });

    final JLabel rowsValue = new JLabel("rows: " + field.getRows());
    final ElwhaButton rowsDown = ElwhaButton.outlinedButton("−");
    final ElwhaButton rowsUp = ElwhaButton.outlinedButton("+");
    rowsDown.addActionListener(
        e -> {
          field.setRows(field.getRows() - 1);
          rowsValue.setText("rows: " + field.getRows());
          fieldHost.revalidate();
        });
    rowsUp.addActionListener(
        e -> {
          field.setRows(field.getRows() + 1);
          rowsValue.setText("rows: " + field.getRows());
          fieldHost.revalidate();
        });
    final JPanel rowsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    rowsRow.setOpaque(false);
    rowsRow.add(rowsDown);
    rowsRow.add(rowsUp);
    rowsRow.add(rowsValue);

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
    controls.add(new JLabel("Input mode"), gbc);
    gbc.gridx = 1;
    controls.add(modeGroup, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    controls.add(new JLabel("Text-area rows"), gbc);
    gbc.gridx = 1;
    controls.add(rowsRow, gbc);

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
      grid.add(refRow(v, InputMode.SINGLE_LINE, "single-line", "Ada Lovelace"));
      grid.add(
          refRow(
              v, InputMode.MULTI_LINE, "multi-line (auto-grow)", "Line one\nLine two\nLine three"));
      grid.add(
          refRow(
              v,
              InputMode.TEXT_AREA,
              "text-area (fixed, scrolls)",
              "A longer entry\nthat overflows\nthe fixed rows\nand scrolls"));
    }
    return grid;
  }

  private static JComponent refRow(
      final Variant variant, final InputMode mode, final String caption, final String text) {
    final ElwhaTextField field = new ElwhaTextField(variant, captionLabel(variant));
    field.setInputMode(mode);
    if (mode == InputMode.TEXT_AREA) {
      field.setRows(3);
    }
    field.setText(text);
    field.setPreferredSize(new Dimension(260, field.getPreferredSize().height));

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
