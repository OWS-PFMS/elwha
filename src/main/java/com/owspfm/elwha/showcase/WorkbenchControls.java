package com.owspfm.elwha.showcase;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The controls column of a Workbench — a vertically stacked set of labelled control rows,
 * optionally split into titled sections.
 *
 * <p>Shared so every component's workbench presents its options the same way: a section header,
 * then {@code label : control} rows in a two-column grid. Build a control (combo box, spinner,
 * checkbox, …), then drop it in with {@link #addControl(String, JComponent)}; group related
 * controls under {@link #addSection(String)}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class WorkbenchControls extends JPanel {

  private final GridBagConstraints constraints = new GridBagConstraints();
  private int nextRow;

  /**
   * Builds an empty controls column.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public WorkbenchControls() {
    super(new GridBagLayout());
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    constraints.anchor = GridBagConstraints.WEST;
  }

  /**
   * Adds a section header. Rows added after it belong to the section until the next header.
   *
   * @param title the section title
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addSection(final String title) {
    final JLabel header = new JLabel(title);
    header.setFont(header.getFont().deriveFont(Font.BOLD));
    constraints.gridx = 0;
    constraints.gridy = nextRow++;
    constraints.gridwidth = 2;
    constraints.insets = new Insets(nextRow == 1 ? 0 : 18, 0, 8, 8);
    add(header, constraints);
    constraints.gridwidth = 1;
  }

  /**
   * Adds a labelled control row — the label in the left column, the control in the right.
   *
   * @param label the row label
   * @param control the control component
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addControl(final String label, final JComponent control) {
    constraints.gridy = nextRow++;
    constraints.insets = new Insets(4, 0, 4, 12);
    constraints.gridx = 0;
    add(new JLabel(label), constraints);
    constraints.gridx = 1;
    constraints.insets = new Insets(4, 0, 4, 0);
    add(control, constraints);
  }
}
