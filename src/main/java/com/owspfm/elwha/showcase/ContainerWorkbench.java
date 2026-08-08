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
 * <strong>container</strong>, a <strong>controls</strong> column, and a bottom band carrying the
 * <strong>code view</strong> beside a scrolling <strong>event log</strong>.
 *
 * <p>List and group surfaces ({@code ElwhaItemList}, the Button / Icon Button groups) cannot be
 * expressed by a single live instance, so they mount here instead of on {@link ComponentWorkbench}.
 * The event log makes selection / reorder / group-change callbacks visible — append to it with
 * {@link #logEvent(String)}.
 *
 * <p><strong>The code view.</strong> A container workbench shows equivalent Java for the same
 * reason a component one does, and it earns it harder: a list is a model, a renderer, and a
 * container, which is precisely the construction a reader cannot guess from the live surface. Push
 * it through {@link #setCode(String)} on every control change, exactly as a {@code
 * ComponentWorkbench} builder does. The code view sits on the leading edge of the bottom band — the
 * position it occupies in the other scaffold — with the event log, which is this scaffold's own
 * addition, beside it (#582).
 *
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ContainerWorkbench extends JPanel {

  private static final int CONTROLS_WIDTH = 480;
  // Matches ComponentWorkbench's CODE_HEIGHT: the two scaffolds' bottom bands are the same depth,
  // so switching between a component leaf and a container leaf does not shift the stage.
  private static final int BOTTOM_HEIGHT = 200;

  private final JPanel containerHost;
  private final WorkbenchControls controls;
  private final CodeView codeView;
  private final JTextArea log;

  /**
   * Builds an empty container workbench — call {@link #setContainer}, {@link #controls}, and {@link
   * #logEvent} to fill it.
   *
   * @version v0.5.0
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
    // Inner panes, tagged so the Showcase's floating-FAB scroll-shrink skips them the way it skips
    // a ComponentWorkbench's (#310): scrolling the controls column or the event log is not paging
    // content, and a container leaf would otherwise shrink the FAB on a knob scroll.
    controlsScroll.putClientProperty(ComponentWorkbench.FAB_SCROLL_IGNORE, Boolean.TRUE);

    log = new JTextArea();
    log.setEditable(false);
    log.putClientProperty("FlatLaf.styleClass", "monospaced");
    log.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final JLabel logHeading = new JLabel("Event log");
    logHeading.putClientProperty("FlatLaf.styleClass", "h4");
    final JPanel logPanel = new JPanel(new BorderLayout());
    logPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
    logHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    logPanel.add(logHeading, BorderLayout.NORTH);
    final JScrollPane logScroll = new JScrollPane(log);
    logScroll.putClientProperty(ComponentWorkbench.FAB_SCROLL_IGNORE, Boolean.TRUE);
    logPanel.add(logScroll, BorderLayout.CENTER);

    codeView = new CodeView();

    // The bottom band: equivalent Java on the leading edge, the event log beside it, split so a
    // reader can give either side the room the moment needs. The divider is the band's only
    // internal chrome; the band as a whole carries the rule that separates it from the stage.
    final JSplitPane bottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codeView, logPanel);
    bottom.setResizeWeight(0.5);
    bottom.setBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));
    bottom.setPreferredSize(new Dimension(0, BOTTOM_HEIGHT));

    final JSplitPane split =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, containerHost, controlsScroll);
    split.setResizeWeight(1.0);
    split.setBorder(null);

    add(split, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);
  }

  /**
   * Updates the container's equivalent-Java code, shown in the bottom band's code view.
   *
   * @param code the code text to show
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setCode(final String code) {
    codeView.setCode(code);
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

  // Test seams — the code view and the event log, which the Showcase suite asserts against without
  // walking the hierarchy for whichever JTextArea happens to come first (#544).
  CodeView codeView() {
    return codeView;
  }

  JTextArea log() {
    return log;
  }
}
