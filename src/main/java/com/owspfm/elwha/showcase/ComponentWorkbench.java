package com.owspfm.elwha.showcase;

import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.chip.list.ChipSelectionMode;
import com.owspfm.elwha.chip.list.DefaultChipListModel;
import com.owspfm.elwha.chip.list.ElwhaChipList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.surface.ElwhaSurface;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

/**
 * The canonical three-region layout for a single-instance component's interactive workbench: a
 * <strong>stage</strong> holding one live component on a configurable surface, a
 * <strong>controls</strong> column exposing every option, and a <strong>code view</strong> showing
 * the equivalent Java.
 *
 * <p>Every component in The Elwha Showcase mounts its Workbench tab on this scaffold, so the
 * arrangement is uniform: a horizontally split stage-and-controls over a full-width {@link
 * CodeView}.
 *
 * <p><strong>The surface stage.</strong> The live component never floats on the bare panel — it
 * sits centered on an {@link ElwhaSurface} so role / elevation / contrast read honestly. That
 * surface is itself configurable: the controls column carries a {@code Component | Surface} chip
 * switcher (an {@link ElwhaChipList} in {@link ChipSelectionMode#SINGLE_MANDATORY} mode) that flips
 * between the component's own controls and the surface's controls, with the code view tracking the
 * active segment. The surface can be sized or hidden entirely — hidden, the component falls back to
 * the bare stage background.
 *
 * <p>A workbench builder calls {@link #setStage(JComponent)} once, populates {@link #controls()}
 * with the component's option controls, and pushes equivalent-Java text through {@link
 * #setCode(String)} whenever a control changes. The surface segment is supplied by the scaffold —
 * builders never wire it.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ComponentWorkbench extends JPanel {

  private static final int CONTROLS_WIDTH = 480;
  private static final int CODE_HEIGHT = 200;
  // Breathing room kept between the live component and the stage surface's rounded edge.
  private static final int STAGE_FIT_MARGIN = 48;

  private final JPanel stageHost;
  private final ElwhaSurface stageSurface;
  private final SurfaceControlPanel surfacePanel;
  private final WorkbenchControls componentControls;
  private final JPanel controlsCards;
  private final CodeView codeView;

  private JComponent liveComponent;
  private String componentCode = "";
  private String surfaceCode = "";
  private boolean surfaceSegmentActive;

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

    componentControls = new WorkbenchControls();
    final WorkbenchControls surfaceControls = new WorkbenchControls();
    surfacePanel = new SurfaceControlPanel(surfaceControls, true);
    surfacePanel.addChangeListener(this::onSurfaceChanged);
    stageSurface = surfacePanel.surface();
    stageSurface.setLayout(new GridBagLayout());

    controlsCards = new JPanel(new CardLayout());
    controlsCards.add(componentControls, "Component");
    controlsCards.add(surfaceControls, "Surface");

    final JScrollPane controlsScroll =
        new JScrollPane(
            controlsCards,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    controlsScroll.getVerticalScrollBar().setUnitIncrement(16);
    controlsScroll.setBorder(null);

    final JPanel controlsRegion = new JPanel(new BorderLayout());
    controlsRegion.setPreferredSize(new Dimension(CONTROLS_WIDTH, 0));
    controlsRegion.setBorder(
        BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Component.borderColor")));
    controlsRegion.add(buildSwitcher(), BorderLayout.NORTH);
    controlsRegion.add(controlsScroll, BorderLayout.CENTER);

    codeView = new CodeView();
    codeView.setPreferredSize(new Dimension(0, CODE_HEIGHT));
    // Compound — keep CodeView's own content padding, add the top divider on the outside.
    codeView.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            codeView.getBorder()));

    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stageHost, controlsRegion);
    split.setResizeWeight(1.0);
    split.setBorder(null);

    add(split, BorderLayout.CENTER);
    add(codeView, BorderLayout.SOUTH);

    surfaceCode = surfacePanel.code();
    mountStage();
  }

  /**
   * Mounts the live component in the stage, centred on the configurable surface. Replaces any
   * previous stage component.
   *
   * @param liveComponent the component being demonstrated
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setStage(final JComponent liveComponent) {
    this.liveComponent = liveComponent;
    mountStage();
  }

  /**
   * Returns the controls column for this component's option controls — the {@code Component}
   * segment of the switcher. The {@code Surface} segment is owned by the scaffold.
   *
   * @return the component controls column
   * @version v0.3.0
   * @since v0.3.0
   */
  public WorkbenchControls controls() {
    return componentControls;
  }

  /**
   * Updates the component's equivalent-Java code. Shown immediately unless the {@code Surface}
   * segment is the active switcher tab.
   *
   * @param code the code text to show
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setCode(final String code) {
    componentCode = code;
    if (!surfaceSegmentActive) {
      codeView.setCode(code);
    }
  }

  // The Component | Surface switcher — an ElwhaChipList tab strip, dogfooding the library's own
  // SINGLE_MANDATORY (segmented-control) selection semantics.
  private JComponent buildSwitcher() {
    final DefaultChipListModel<String> model =
        new DefaultChipListModel<>(List.of("Component", "Surface"));
    final ElwhaChipList<String> switcher =
        new ElwhaChipList<>(model, (item, index) -> new ElwhaChip(item))
            .setOrientation(ElwhaListOrientation.HORIZONTAL)
            .setSelectionMode(ChipSelectionMode.SINGLE_MANDATORY);
    switcher
        .getSelectionModel()
        .addSelectionListener(
            event ->
                showSegment(
                    !event.getSelected().isEmpty()
                        && "Surface".equals(event.getSelected().get(0))));

    final JPanel bar = new JPanel(new BorderLayout());
    bar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    bar.add(switcher, BorderLayout.CENTER);
    return bar;
  }

  private void showSegment(final boolean surface) {
    surfaceSegmentActive = surface;
    ((CardLayout) controlsCards.getLayout()).show(controlsCards, surface ? "Surface" : "Component");
    codeView.setCode(surface ? surfaceCode : componentCode);
  }

  private void onSurfaceChanged() {
    surfaceCode = surfacePanel.code();
    mountStage();
    if (surfaceSegmentActive) {
      codeView.setCode(surfaceCode);
    }
  }

  // Re-parents the live component: onto the surface when the surface is shown, directly onto the
  // bare stage host when it is hidden.
  private void mountStage() {
    stageHost.removeAll();
    stageSurface.removeAll();
    if (surfacePanel.isSurfaceVisible()) {
      if (liveComponent != null) {
        stageSurface.add(liveComponent);
      }
      sizeStageSurface();
      stageHost.add(stageSurface);
    } else if (liveComponent != null) {
      stageHost.add(liveComponent);
    }
    stageHost.revalidate();
    stageHost.repaint();
  }

  // The chosen Size is a floor: the stage surface grows past it to fit a component larger than it,
  // so a big component (a full Card) is never clipped by the surface's rounded body.
  private void sizeStageSurface() {
    final Dimension chosen = surfacePanel.chosenStageSize();
    int width = chosen.width;
    int height = chosen.height;
    if (liveComponent != null) {
      final Dimension need = liveComponent.getPreferredSize();
      final Insets insets = stageSurface.getInsets();
      width = Math.max(width, need.width + insets.left + insets.right + STAGE_FIT_MARGIN);
      height = Math.max(height, need.height + insets.top + insets.bottom + STAGE_FIT_MARGIN);
    }
    stageSurface.setPreferredSize(new Dimension(width, height));
  }
}
