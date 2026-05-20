package com.owspfm.elwha.showcase;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

/**
 * The canonical three-region layout for a single-instance component's interactive workbench: a
 * <strong>stage</strong> holding one live component, a <strong>controls</strong> column exposing
 * every option, and a <strong>code view</strong> showing the equivalent Java.
 *
 * <p>Every component in The Elwha Showcase mounts its Workbench tab on this scaffold, so the
 * arrangement is uniform: a horizontally split stage-and-controls over a full-width {@link
 * CodeView}. A workbench builder calls {@link #setStage(JComponent)} once, populates {@link
 * #controls()}, and pushes equivalent-Java text through {@link #setCode(String)} whenever a control
 * changes.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ComponentWorkbench extends JPanel {

  private static final int CONTROLS_WIDTH = 320;
  private static final int CODE_HEIGHT = 200;

  private final JPanel stageHost;
  private final WorkbenchControls controls;
  private final CodeView codeView;

  /**
   * Builds an empty workbench — call {@link #setStage}, {@link #controls}, and {@link #setCode} to
   * fill it.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public ComponentWorkbench() {
    super(new BorderLayout());

    stageHost = new JPanel(new GridBagLayout());
    stageHost.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

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

    codeView = new CodeView();
    codeView.setPreferredSize(new Dimension(0, CODE_HEIGHT));
    // Compound — keep CodeView's own content padding, add the top divider on the outside.
    codeView.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            codeView.getBorder()));

    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stageHost, controlsScroll);
    split.setResizeWeight(1.0);
    split.setBorder(null);

    add(split, BorderLayout.CENTER);
    add(codeView, BorderLayout.SOUTH);
  }

  /**
   * Mounts the live component in the stage, centred with generous padding. Replaces any previous
   * stage component.
   *
   * @param liveComponent the component being demonstrated
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setStage(final JComponent liveComponent) {
    stageHost.removeAll();
    stageHost.add(liveComponent);
    stageHost.revalidate();
    stageHost.repaint();
  }

  /**
   * Returns the controls column to populate with this component's option controls.
   *
   * @return the controls column
   * @version v0.3.0
   * @since v0.3.0
   */
  public WorkbenchControls controls() {
    return controls;
  }

  /**
   * Updates the equivalent-Java code view.
   *
   * @param code the code text to show
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setCode(final String code) {
    codeView.setCode(code);
  }
}
