package com.owspfm.elwha.showcase;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * The canonical layout for a multi-instance container's interactive workbench: a live
 * <strong>container</strong>, a <strong>controls</strong> column, and a scrolling <strong>event
 * log</strong>.
 *
 * <p>List and group surfaces ({@code ElwhaChipList}, {@code ElwhaCardList}, the Button / Icon
 * Button groups) cannot be expressed by a single live instance, so they mount here instead of on
 * {@link ComponentWorkbench}. The event log makes selection / reorder / group-change callbacks
 * visible — append to it with {@link #logEvent(String)}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ContainerWorkbench extends JPanel {

  private static final int CONTROLS_WIDTH = 480;
  private static final int LOG_HEIGHT = 150;

  private final JPanel containerHost;
  private final WorkbenchControls controls;
  private final JTextArea log;

  /**
   * Builds an empty container workbench — call {@link #setContainer}, {@link #controls}, and {@link
   * #logEvent} to fill it.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public ContainerWorkbench() {
    super(new BorderLayout());

    containerHost = new JPanel(new BorderLayout());
    containerHost.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    controls = new WorkbenchControls();
    final JScrollPane controlsScroll =
        new JScrollPane(
            controls,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    controlsScroll.setPreferredSize(new Dimension(CONTROLS_WIDTH, 0));
    controlsScroll.getVerticalScrollBar().setUnitIncrement(16);
    controlsScroll.setBorder(
        BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Component.borderColor")));

    log = new JTextArea();
    log.setEditable(false);
    log.putClientProperty("FlatLaf.styleClass", "monospaced");
    log.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final JLabel logHeading = new JLabel("Event log");
    logHeading.putClientProperty("FlatLaf.styleClass", "h4");
    final JPanel logPanel = new JPanel(new BorderLayout());
    logPanel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    logPanel.setPreferredSize(new Dimension(0, LOG_HEIGHT));
    logHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    logPanel.add(logHeading, BorderLayout.NORTH);
    logPanel.add(new JScrollPane(log), BorderLayout.CENTER);

    final JSplitPane split =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, containerHost, controlsScroll);
    split.setResizeWeight(1.0);
    split.setBorder(null);

    add(split, BorderLayout.CENTER);
    add(logPanel, BorderLayout.SOUTH);
  }

  /**
   * Mounts the live container. Replaces any previous container component.
   *
   * @param container the container being demonstrated
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setContainer(final JComponent container) {
    containerHost.removeAll();
    containerHost.add(container, BorderLayout.CENTER);
    containerHost.revalidate();
    containerHost.repaint();
  }

  /**
   * Returns the controls column to populate with this container's option controls.
   *
   * @return the controls column
   * @version v0.3.0
   * @since v0.3.0
   */
  public WorkbenchControls controls() {
    return controls;
  }

  /**
   * Appends one line to the event log and scrolls it into view.
   *
   * @param message the event description
   * @version v0.3.0
   * @since v0.3.0
   */
  public void logEvent(final String message) {
    log.append(message + "\n");
    log.setCaretPosition(log.getDocument().getLength());
  }
}
