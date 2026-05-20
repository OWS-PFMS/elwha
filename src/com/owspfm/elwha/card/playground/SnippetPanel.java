package com.owspfm.elwha.card.playground;

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
 * Read-only Java snippet that reproduces the current focus-card configuration as V3 API calls.
 * Updated by {@link LiveConfigPanel} via {@link #update(LiveConfigPanel.Snapshot)}.
 *
 * <p>The "Copy" button copies the snippet text to the system clipboard.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class SnippetPanel extends JPanel {

  private final JTextArea area;

  /** Builds the snippet view with an empty initial body. */
  public SnippetPanel() {
    super(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    final JLabel heading = new JLabel("Java snippet");
    heading.putClientProperty("FlatLaf.styleClass", "h4");

    area = new JTextArea();
    area.setEditable(false);
    area.putClientProperty("FlatLaf.styleClass", "monospaced");
    area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final ElwhaButton copy = ElwhaButton.outlinedButton("Copy");
    copy.addActionListener(
        e ->
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(area.getText()), null));

    final JPanel header = new JPanel(new BorderLayout());
    header.add(heading, BorderLayout.WEST);
    header.add(copy, BorderLayout.EAST);
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

    add(header, BorderLayout.NORTH);
    add(new JScrollPane(area), BorderLayout.CENTER);
  }

  /**
   * Recomputes and displays the snippet for the supplied snapshot.
   *
   * @param snapshot the latest config snapshot from {@link LiveConfigPanel}
   * @version v0.2.0
   * @since v0.2.0
   */
  public void update(final LiveConfigPanel.Snapshot snapshot) {
    area.setText(render(snapshot));
    area.setCaretPosition(0);
  }

  private static String render(final LiveConfigPanel.Snapshot s) {
    final StringBuilder sb = new StringBuilder(384);
    final String factory =
        switch (s.variant()) {
          case ELEVATED -> "ElwhaCard.elevatedCard()";
          case FILLED -> "ElwhaCard.filledCard()";
          case OUTLINED -> "ElwhaCard.outlinedCard()";
        };
    sb.append("ElwhaCard card = ").append(factory);
    if (s.overflow() != com.owspfm.elwha.card.ExpansionOverflow.GROW) {
      sb.append("\n    .setExpansionOverflow(ExpansionOverflow.")
          .append(s.overflow().name())
          .append(")");
    }
    if (s.actionable()) {
      sb.append("\n    .setActionable(true)");
    }
    if (s.selectable()) {
      sb.append("\n    .setSelectable(true)");
    }
    if (s.collapsible()) {
      sb.append("\n    .setCollapsible(true)");
    }
    if (s.collapsed()) {
      sb.append("\n    .setCollapsed(true)");
    }
    sb.append(";\n");
    sb.append(
        "card.add(new ElwhaCardHeader().setTitle(\"Focus card\").setSubtitle(\"Live-edited"
            + " example\"));\n");
    if (s.collapsible()) {
      sb.append("card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);\n");
    }
    sb.append(
        "card.add(new ElwhaCardSupportingText(\"Adjust the controls on the right; the card mutates"
            + " in real time.\"));\n");
    if (s.disabled()) {
      sb.append("card.setEnabled(false);\n");
    }
    return sb.toString();
  }
}
