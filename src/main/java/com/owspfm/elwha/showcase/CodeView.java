package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A read-only, monospaced code view with a heading and a "Copy" button — the shared equivalent-Java
 * surface every Component Workbench shows beneath its live stage.
 *
 * <p>Generalised from the Card playground's {@code SnippetPanel}: where that took a Card-specific
 * snapshot, this takes a plain {@code String} via {@link #setCode(String)}, so any component's
 * workbench can drive it. The "Copy" button puts the current text on the system clipboard.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class CodeView extends JPanel {

  private final JTextArea area;

  /**
   * Builds a code view headed "Equivalent Java".
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public CodeView() {
    this("Equivalent Java");
  }

  /**
   * Builds a code view with a custom heading.
   *
   * @param title the heading text shown above the code area
   * @version v0.3.0
   * @since v0.3.0
   */
  public CodeView(final String title) {
    super(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

    final JLabel heading = new JLabel(title);
    heading.putClientProperty("FlatLaf.styleClass", "h4");

    area = new JTextArea();
    area.setEditable(false);
    area.putClientProperty("FlatLaf.styleClass", "monospaced");
    area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final ElwhaButton copy = ElwhaButton.outlinedButton("Copy");
    copy.addActionListener(
        event ->
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(area.getText()), null));

    final JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    header.add(heading, BorderLayout.WEST);
    header.add(copy, BorderLayout.EAST);

    add(header, BorderLayout.NORTH);
    add(new JScrollPane(area), BorderLayout.CENTER);
  }

  /**
   * Replaces the displayed code and scrolls the view back to the top.
   *
   * @param code the code text to show
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setCode(final String code) {
    area.setText(code);
    area.setCaretPosition(0);
  }
}
