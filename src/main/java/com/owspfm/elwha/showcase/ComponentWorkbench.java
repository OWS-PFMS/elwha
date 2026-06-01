package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.surface.ElwhaSurface;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
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
 * surface is itself configurable: the controls column carries a {@code Component | Surface}
 * switcher (an {@link ElwhaButtonGroup} connected group in {@link SelectionMode#REQUIRED} mode)
 * that flips between the component's own controls and the surface's controls, with the code view
 * tracking the active segment. The surface can be sized or hidden entirely — hidden, the component
 * falls back to the bare stage background.
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
  // Breathing room (combined; ~half per side) kept between the live component and the stage
  // surface's rounded edge. Sized so the floor is comfortable for the library's tallest live
  // components — a connected ElwhaButtonGroup at XL is 136 px tall, and 128 px combined breathing
  // room (~64 each side) keeps the surface visibly larger than the buttons rather than hugging
  // them flush at chosen MEDIUM (220 px).
  private static final int STAGE_FIT_MARGIN = 128;

  // Client-property marker on the stage's scroll pane. The Showcase's floating-FAB scroll-shrink
  // dogfood (#274) targets the first scroll pane it finds in the active page; the stage scroll
  // (added for #179) would shadow the page/controls scroll it actually means to track, so the
  // FAB search skips any pane carrying this flag.
  static final String FAB_SCROLL_IGNORE = "Showcase.fabScrollIgnore";

  private final StageHost stageHost;
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

    stageHost = new StageHost();
    stageHost.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

    componentControls = new WorkbenchControls();
    final WorkbenchControls surfaceControls = new WorkbenchControls();
    surfacePanel = new SurfaceControlPanel(surfaceControls, true);
    surfacePanel.addChangeListener(this::onSurfaceChanged);
    stageSurface = surfacePanel.surface();
    stageSurface.setLayout(new GridBagLayout());

    // A click on the stage background — either the bare host margin or the surface around the
    // live component — clears the focus owner. With a single live component on the stage, focus
    // sticks to the component on first click and has nowhere natural to go until the user clicks
    // another focusable element or Tabs away; clearing it on a background click matches the user
    // expectation that the workbench surface is a "deselect" target. Swing's events don't bubble,
    // so this listener never fires on clicks that hit the live component or any of its children.
    final MouseAdapter clearFocusOnBackgroundClick =
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent event) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
          }
        };
    stageHost.addMouseListener(clearFocusOnBackgroundClick);
    stageSurface.addMouseListener(clearFocusOnBackgroundClick);
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

    // The stage rides in a scroll pane so a cramped window scrolls rather than shrinking the
    // surface. stageHost is Scrollable and fills the viewport when there is room (the surface
    // centers with its full breathing room), but holds its preferred size when the viewport is
    // narrower than the surface + margin — at which point a scrollbar appears instead of GridBag
    // collapsing the surface toward the live component (#179).
    final JScrollPane stageScroll =
        new JScrollPane(
            stageHost,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    stageScroll.setBorder(null);
    stageScroll.getViewport().setOpaque(false);
    stageScroll.setOpaque(false);
    stageScroll.getVerticalScrollBar().setUnitIncrement(16);
    stageScroll.getHorizontalScrollBar().setUnitIncrement(16);
    stageScroll.putClientProperty(FAB_SCROLL_IGNORE, Boolean.TRUE);

    final JSplitPane split =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stageScroll, controlsRegion);
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

  // The Component | Surface switcher — an ElwhaButtonGroup connected REQUIRED group, dogfooding the
  // library's own M3 segmented-control component. REQUIRED auto-seeds the first ("Component")
  // segment, matching the controls column's default card.
  private JComponent buildSwitcher() {
    final int iconPx = ButtonSize.XS.iconSizePx();
    final ElwhaButtonGroup switcher =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.OUTLINED)
            .add(
                new ElwhaButton("Component")
                    .setIcons(MaterialIcons.widgets(iconPx), MaterialIcons.widgetsFilled(iconPx)))
            .add(
                new ElwhaButton("Surface")
                    .setIcons(MaterialIcons.layers(iconPx), MaterialIcons.layersFilled(iconPx)));
    switcher.addSelectionListener(group -> showSegment(group.getSelectedIndex() == 1));

    final JPanel bar = new JPanel(new BorderLayout());
    bar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    bar.add(switcher, BorderLayout.WEST);
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

  // The stage's scroll view. Fills the viewport (so the GridBag-centered surface keeps its full
  // breathing room) whenever the viewport is at least as large as this host's preferred size, but
  // holds its preferred size — letting the scroll pane show a scrollbar — once the viewport is
  // smaller. This is what stops a cramped window from shrinking the surface (#179): GridBag would
  // otherwise collapse the surface toward (and below) the live component's marginless minimum.
  private static final class StageHost extends JPanel implements Scrollable {

    StageHost() {
      super(new GridBagLayout());
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(
        final Rectangle visibleRect, final int orientation, final int direction) {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(
        final Rectangle visibleRect, final int orientation, final int direction) {
      return orientation == javax.swing.SwingConstants.VERTICAL
          ? visibleRect.height
          : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return getParent() instanceof JViewport vp && vp.getWidth() >= getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return getParent() instanceof JViewport vp && vp.getHeight() >= getPreferredSize().height;
    }
  }
}
