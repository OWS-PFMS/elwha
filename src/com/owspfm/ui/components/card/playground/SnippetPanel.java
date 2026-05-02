package com.owspfm.ui.components.card.playground;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Read-only Java code snippet that reproduces the current focus-card configuration. Updated by
 * {@link LiveConfigPanel} via {@link #update(LiveConfigPanel.Snapshot)}.
 *
 * <p>The "Copy" button copies the snippet text to the system clipboard.
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public final class SnippetPanel extends JPanel {

  private final JTextArea myArea;

  /** Builds the snippet view with an empty initial body. */
  public SnippetPanel() {
    super(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    JLabel heading = new JLabel("Java snippet");
    heading.putClientProperty("FlatLaf.styleClass", "h4");

    myArea = new JTextArea();
    myArea.setEditable(false);
    myArea.putClientProperty("FlatLaf.styleClass", "monospaced");
    myArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    JButton copy = new JButton("Copy");
    copy.addActionListener(
        e ->
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(myArea.getText()), null));

    JPanel header = new JPanel(new BorderLayout());
    header.add(heading, BorderLayout.WEST);
    header.add(copy, BorderLayout.EAST);
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

    add(header, BorderLayout.NORTH);
    add(new JScrollPane(myArea), BorderLayout.CENTER);
  }

  /**
   * Recomputes and displays the snippet for the supplied snapshot.
   *
   * @param theSnapshot the latest config snapshot from {@link LiveConfigPanel}
   */
  public void update(final LiveConfigPanel.Snapshot theSnapshot) {
    myArea.setText(render(theSnapshot));
    myArea.setCaretPosition(0);
  }

  private static String render(final LiveConfigPanel.Snapshot s) {
    StringBuilder sb = new StringBuilder(512);
    sb.append("FlatCard card = new FlatCard()\n");
    sb.append("    .setVariant(CardVariant.").append(s.variant().name()).append(")\n");
    sb.append("    .setInteractionMode(CardInteractionMode.").append(s.mode().name()).append(")\n");
    sb.append("    .setElevation(").append(s.elevation()).append(")\n");
    sb.append("    .setCornerRadius(").append(s.cornerRadius()).append(")\n");
    sb.append("    .setPadding(").append(s.padding()).append(")\n");
    sb.append("    .setBorderWidth(").append(s.borderWidth()).append(")\n");
    if (s.showHeader()) {
      sb.append("    .setHeader(\"Focus card\", \"Live-edited example\")\n");
    }
    if (s.showMedia()) {
      sb.append("    .setMedia(buildMediaComponent())\n");
    }
    if (s.showFooter()) {
      sb.append("    .setFooter(new JButton(\"Primary\"), new JButton(\"Cancel\"))\n");
    }
    if (s.collapsible()) {
      sb.append("    .setCollapsible(true)\n");
    }
    if (s.collapsed()) {
      sb.append("    .setCollapsed(true)\n");
    }
    sb.append(";\n");
    if (s.disabled()) {
      sb.append("card.setEnabled(false);\n");
    }
    return sb.toString();
  }
}
