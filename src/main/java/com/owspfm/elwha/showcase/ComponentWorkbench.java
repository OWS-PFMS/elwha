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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
 * <p><strong>Extra facets.</strong> A composed component (e.g. a navigation rail carrying a badge)
 * can expose an embedded sub-component's own editor as an additional switcher segment via {@link
 * #addFacet(String, JComponent)}. Extra facets land <em>between</em> the {@code Component} and
 * {@code Surface} bookends — Component is always first (it is the thing), Surface is always last
 * (it is the stage it sits on) — so a rail-with-badge reads {@code Component | Badge | Surface}.
 * Each facet owns its own code text through the returned {@link Facet} handle; the code view tracks
 * the active segment. The {@code Surface} segment stays scaffold-special (it reconfigures the
 * stage) and is not a {@link Facet}.
 *
 * @author Charles Bryan
 * @version v0.4.0
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

  private static final String COMPONENT_SEGMENT = "Component";
  private static final String SURFACE_SEGMENT = "Surface";

  private final JPanel stageHost;
  private final ElwhaSurface stageSurface;
  private final SurfaceControlPanel surfacePanel;
  private final WorkbenchControls componentControls;
  private final JPanel controlsCards;
  private final JPanel switcherBar;
  private final CodeView codeView;
  private final List<Facet> facets = new ArrayList<>();

  private JComponent liveComponent;
  private String componentCode = "";
  private String surfaceCode = "";
  private String activeSegment = COMPONENT_SEGMENT;

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
    controlsCards.add(componentControls, COMPONENT_SEGMENT);
    controlsCards.add(surfaceControls, SURFACE_SEGMENT);

    final JScrollPane controlsScroll =
        new JScrollPane(
            controlsCards,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    controlsScroll.getVerticalScrollBar().setUnitIncrement(16);
    controlsScroll.setBorder(null);

    switcherBar = new JPanel(new BorderLayout());
    switcherBar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    rebuildSwitcher();

    final JPanel controlsRegion = new JPanel(new BorderLayout());
    controlsRegion.setPreferredSize(new Dimension(CONTROLS_WIDTH, 0));
    controlsRegion.setBorder(
        BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Component.borderColor")));
    controlsRegion.add(switcherBar, BorderLayout.NORTH);
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
    if (COMPONENT_SEGMENT.equals(activeSegment)) {
      codeView.setCode(code);
    }
  }

  /**
   * Adds an extra switcher facet between the {@code Component} and {@code Surface} bookends — a
   * named segment that swaps the controls column to {@code controls} when selected. Use for a
   * composed component's embedded sub-component editor (e.g. a navigation rail's badge). Facets
   * appear in insertion order between the bookends.
   *
   * @param name the segment label, also the facet's code-tracking key (must be unique among facets)
   * @param controls the controls panel shown when this facet is active
   * @return a handle for pushing this facet's equivalent-Java code via {@link
   *     Facet#setCode(String)}
   * @version v0.4.0
   * @since v0.4.0
   */
  public Facet addFacet(final String name, final JComponent controls) {
    final Facet facet = new Facet(name);
    facets.add(facet);
    controlsCards.add(controls, name);
    rebuildSwitcher();
    return facet;
  }

  /**
   * A handle to an extra switcher facet added via {@link ComponentWorkbench#addFacet(String,
   * JComponent)}. Carries the facet's equivalent-Java code, shown in the code view whenever the
   * facet is the active segment.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  public final class Facet {
    private final String name;
    private String code = "";

    private Facet(final String name) {
      this.name = name;
    }

    /**
     * Updates this facet's equivalent-Java code. Shown immediately if this facet is the active
     * switcher segment.
     *
     * @param code the code text to show
     * @version v0.4.0
     * @since v0.4.0
     */
    public void setCode(final String code) {
      this.code = code;
      if (name.equals(activeSegment)) {
        codeView.setCode(code);
      }
    }
  }

  // The Component | <facets…> | Surface switcher — an ElwhaButtonGroup connected REQUIRED group,
  // dogfooding the library's own M3 segmented-control component. REQUIRED auto-seeds the first
  // ("Component") segment, matching the controls column's default card. Rebuilt whenever a facet is
  // added; the bookends carry house-style icons, builder-supplied facets are text-only.
  private void rebuildSwitcher() {
    final int iconPx = ButtonSize.XS.iconSizePx();
    final ElwhaButtonGroup switcher =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.OUTLINED);

    final List<String> names = new ArrayList<>();
    switcher.add(
        new ElwhaButton(COMPONENT_SEGMENT)
            .setIcons(MaterialIcons.widgets(iconPx), MaterialIcons.widgetsFilled(iconPx)));
    names.add(COMPONENT_SEGMENT);
    for (final Facet facet : facets) {
      switcher.add(new ElwhaButton(facet.name));
      names.add(facet.name);
    }
    switcher.add(
        new ElwhaButton(SURFACE_SEGMENT)
            .setIcons(MaterialIcons.layers(iconPx), MaterialIcons.layersFilled(iconPx)));
    names.add(SURFACE_SEGMENT);

    final int activeIndex = Math.max(0, names.indexOf(activeSegment));
    switcher.setSelectedIndex(activeIndex);
    switcher.addSelectionListener(group -> showSegment(names.get(group.getSelectedIndex())));

    switcherBar.removeAll();
    switcherBar.add(switcher, BorderLayout.WEST);
    switcherBar.revalidate();
    switcherBar.repaint();
  }

  private void showSegment(final String name) {
    activeSegment = name;
    ((CardLayout) controlsCards.getLayout()).show(controlsCards, name);
    codeView.setCode(codeFor(name));
  }

  private String codeFor(final String name) {
    if (COMPONENT_SEGMENT.equals(name)) {
      return componentCode;
    }
    if (SURFACE_SEGMENT.equals(name)) {
      return surfaceCode;
    }
    for (final Facet facet : facets) {
      if (facet.name.equals(name)) {
        return facet.code;
      }
    }
    return "";
  }

  private void onSurfaceChanged() {
    surfaceCode = surfacePanel.code();
    mountStage();
    if (SURFACE_SEGMENT.equals(activeSegment)) {
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
