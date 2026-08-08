package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The three-region workbench scaffold every Components leaf mounts on — stage, controls column,
 * code view — plus the {@code Component | facets | Surface} switcher that swaps between them.
 *
 * <p>Every test here builds its own workbench: the scaffold's contract is about start state and
 * transitions, and the shared catalog's workbenches have been driven by the time this class runs.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ComponentWorkbenchTest {

  private static List<String> segmentsOf(final ComponentWorkbench workbench) {
    return ShowcaseFixture.segmentsOf(workbench);
  }

  private static ElwhaButtonGroup switcher(final ComponentWorkbench workbench) {
    return ShowcaseFixture.findFirst(workbench, ElwhaButtonGroup.class);
  }

  private static void selectSegment(final ComponentWorkbench workbench, final String name) {
    switcher(workbench).setSelectedIndex(segmentsOf(workbench).indexOf(name));
  }

  // ------------------------------------------------------------------ stage

  @Test
  void aFreshWorkbenchStagesNothing() {
    assertThat(new ComponentWorkbench().stage())
        .as("a builder mounts its component; the scaffold does not invent one")
        .isNull();
  }

  @Test
  void setStageMountsTheLiveComponent() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton live = new ElwhaButton("Common button");

    workbench.setStage(live);

    assertThat(workbench.stage()).isSameAs(live);
    assertThat(ShowcaseFixture.descendants(workbench))
        .as("the live component is really in the tree, not just recorded")
        .contains(live);
  }

  @Test
  void liveComponentSitsOnTheStageSurface() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton live = new ElwhaButton("Common button");

    workbench.setStage(live);

    final ElwhaSurface surface = ShowcaseFixture.findFirst(workbench, ElwhaSurface.class);
    assertThat(surface)
        .as("the component never floats on the bare panel — role and elevation read honestly")
        .isNotNull();
    assertThat(surface.getComponents()).containsExactly(live);
  }

  @Test
  void setStageReplacesThePreviousComponent() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton first = new ElwhaButton("First");
    final ElwhaButton second = new ElwhaButton("Second");
    workbench.setStage(first);

    workbench.setStage(second);

    assertThat(workbench.stage()).isSameAs(second);
    assertThat(ShowcaseFixture.descendants(workbench))
        .as("a workbench stages one component; the old one is unmounted, not stacked")
        .contains(second)
        .doesNotContain(first);
  }

  @Test
  void stageSurfaceHoldsTheChosenSizeAsAFloor() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    workbench.setStage(new ElwhaButton("Common button"));

    final Dimension surface =
        ShowcaseFixture.findFirst(workbench, ElwhaSurface.class).getPreferredSize();
    assertThat(surface.width).as("the MEDIUM stage width").isGreaterThanOrEqualTo(340);
    assertThat(surface.height).as("the MEDIUM stage height").isGreaterThanOrEqualTo(220);
  }

  @Test
  void stageSurfaceGrowsPastTheFloorForALargeComponent() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final JPanel large = new JPanel();
    large.setPreferredSize(new Dimension(800, 600));

    workbench.setStage(large);

    final Dimension surface =
        ShowcaseFixture.findFirst(workbench, ElwhaSurface.class).getPreferredSize();
    assertThat(surface.width)
        .as("a big component is never clipped by the surface's rounded body")
        .isGreaterThanOrEqualTo(800);
    assertThat(surface.height).isGreaterThanOrEqualTo(600);
  }

  // --------------------------------------------------------------- switcher

  @Test
  void switcherOpensOnComponentAndSurface() {
    assertThat(segmentsOf(new ComponentWorkbench()))
        .as("§13 — the bookends: Component is the thing, Surface is the stage it sits on")
        .containsExactly("Component", "Surface");
  }

  @Test
  void aFacetLandsBetweenTheBookends() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    workbench.addFacet("Badge", new WorkbenchControls());

    assertThat(segmentsOf(workbench))
        .as("§13 — a rail-with-badge reads Component | Badge | Surface")
        .containsExactly("Component", "Badge", "Surface");
  }

  @Test
  void facetsKeepInsertionOrder() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    workbench.addFacet("Badge", new WorkbenchControls());
    workbench.addFacet("Destination", new WorkbenchControls());

    assertThat(segmentsOf(workbench))
        .containsExactly("Component", "Badge", "Destination", "Surface");
  }

  @Test
  void componentSegmentIsSelectedOnOpen() {
    assertThat(new ComponentWorkbench().activeSegment())
        .as("§13 — REQUIRED selection auto-seeds the first segment, matching the controls card")
        .isEqualTo("Component");
  }

  @Test
  void addingAFacetKeepsTheActiveSegment() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    selectSegment(workbench, "Surface");

    workbench.addFacet("Badge", new WorkbenchControls());

    assertThat(workbench.activeSegment())
        .as("the switcher is rebuilt when a facet arrives; the selection must survive it")
        .isEqualTo("Surface");
  }

  // -------------------------------------------------------------- code view

  @Test
  void setCodeShowsImmediatelyOnTheComponentSegment() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    workbench.setCode("new ElwhaButton(\"Hi\");");

    assertThat(workbench.codeView().code()).isEqualTo("new ElwhaButton(\"Hi\");");
  }

  @Test
  void surfaceSegmentShowsTheSurfaceCode() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.setCode("new ElwhaButton(\"Hi\");");

    selectSegment(workbench, "Surface");

    assertThat(workbench.codeView().code())
        .as("§13 — the code view tracks the active segment")
        .contains("ElwhaSurface")
        .doesNotContain("ElwhaButton");
  }

  @Test
  void switchingBackRestoresTheComponentCode() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.setCode("new ElwhaButton(\"Hi\");");
    selectSegment(workbench, "Surface");

    selectSegment(workbench, "Component");

    assertThat(workbench.codeView().code()).isEqualTo("new ElwhaButton(\"Hi\");");
  }

  @Test
  void codeSetWhileAnotherSegmentIsActiveIsHeldNotShown() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    selectSegment(workbench, "Surface");

    workbench.setCode("new ElwhaButton(\"Hi\");");

    assertThat(workbench.codeView().code())
        .as("a control change under the Surface tab must not hijack the visible code")
        .doesNotContain("ElwhaButton");
    assertThat(workbench.codeFor("Component"))
        .as("but it is held, ready for the switch back")
        .isEqualTo("new ElwhaButton(\"Hi\");");
  }

  @Test
  void aFacetOwnsItsOwnCode() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ComponentWorkbench.Facet facet = workbench.addFacet("Badge", new WorkbenchControls());
    workbench.setCode("component code");

    facet.setCode("badge code");
    selectSegment(workbench, "Badge");

    assertThat(workbench.codeView().code()).isEqualTo("badge code");
    assertThat(workbench.codeFor("Component")).isEqualTo("component code");
  }

  @Test
  void anUnknownSegmentHasNoCode() {
    assertThat(new ComponentWorkbench().codeFor("Nonexistent")).isEmpty();
  }

  // ----------------------------------------------------------- pinned band

  @Test
  void aFreshWorkbenchPinsNothing() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    assertThat(workbench.pinnedActionFor("Component")).isNull();
    assertThat(workbench.pinnedBand().getPreferredSize().height)
        .as("#507 — a leaf that pins nothing must see no layout change at all")
        .isZero();
  }

  @Test
  void aPinnedActionIsMountedOutsideTheScrollingColumn() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton trigger = new ElwhaButton("Open modal");

    workbench.setPinnedAction(trigger);

    assertThat(ShowcaseFixture.descendants(workbench)).contains(trigger);
    assertThat(ShowcaseFixture.descendants(workbench.controls()))
        .as("#507 — the whole point is that it does not scroll with the column")
        .doesNotContain(trigger);
    assertThat(ShowcaseFixture.descendants(workbench.pinnedBand())).contains(trigger);
  }

  @Test
  void aPinnedActionGivesTheBandHeight() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    workbench.setPinnedAction(new ElwhaButton("Open modal"));

    assertThat(workbench.pinnedBand().getPreferredSize().height).isPositive();
  }

  @Test
  void pinningNullClearsTheBand() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.setPinnedAction(new ElwhaButton("Open modal"));

    workbench.setPinnedAction(null);

    assertThat(workbench.pinnedActionFor("Component")).isNull();
    assertThat(workbench.pinnedBand().getPreferredSize().height).isZero();
  }

  @Test
  void pinnedBandFollowsTheActiveSegment() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton trigger = new ElwhaButton("Open modal");
    workbench.setPinnedAction(trigger);

    selectSegment(workbench, "Surface");

    assertThat(ShowcaseFixture.descendants(workbench.pinnedBand()))
        .as("#507 — the band is per-segment, like the controls card it sits above")
        .doesNotContain(trigger);

    selectSegment(workbench, "Component");

    assertThat(ShowcaseFixture.descendants(workbench.pinnedBand())).contains(trigger);
  }

  @Test
  void aFacetPinsItsOwnAction() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ComponentWorkbench.Facet facet = workbench.addFacet("Modal", new WorkbenchControls());
    final ElwhaButton trigger = new ElwhaButton("Open modal");

    facet.setPinnedAction(trigger);
    selectSegment(workbench, "Modal");

    assertThat(ShowcaseFixture.descendants(workbench.pinnedBand())).contains(trigger);
  }

  @Test
  void aSegmentThatPinsNothingKeepsAZeroHeightBand() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.addFacet("Modal", new WorkbenchControls()).setPinnedAction(new ElwhaButton("Open"));

    assertThat(workbench.activeSegment()).isEqualTo("Component");
    assertThat(workbench.pinnedBand().getPreferredSize().height)
        .as("#507 — one facet's pinned action must not reserve a strip on every other segment")
        .isZero();
  }

  @Test
  void anActionPinnedWhileAnotherSegmentIsActiveIsHeldNotShown() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton trigger = new ElwhaButton("Open modal");
    selectSegment(workbench, "Surface");

    workbench.setPinnedAction(trigger);

    assertThat(ShowcaseFixture.descendants(workbench.pinnedBand())).doesNotContain(trigger);
    assertThat(workbench.pinnedActionFor("Component"))
        .as("but it is held, ready for the switch back")
        .isSameAs(trigger);
  }

  // ------------------------------------------------------- surface controls

  @Test
  void hidingTheSurfaceDropsTheComponentOntoTheBareStage() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final ElwhaButton live = new ElwhaButton("Common button");
    workbench.setStage(live);

    showSurfaceToggle(workbench).doClick();

    assertThat(ShowcaseFixture.findAll(workbench, ElwhaSurface.class))
        .as("a hidden surface is unmounted, not merely transparent")
        .isEmpty();
    assertThat(ShowcaseFixture.descendants(workbench))
        .as("and the component falls back to the bare stage background")
        .contains(live);
  }

  @Test
  void hidingTheSurfaceSaysSoInTheCodeView() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.setStage(new ElwhaButton("Common button"));

    showSurfaceToggle(workbench).doClick();
    selectSegment(workbench, "Surface");

    assertThat(workbench.codeView().code()).contains("Surface hidden");
  }

  private static ElwhaCheckbox showSurfaceToggle(final ComponentWorkbench workbench) {
    for (final ElwhaCheckbox box : ShowcaseFixture.findAll(workbench, ElwhaCheckbox.class)) {
      if ("Show surface".equals(box.getLabel())) {
        return box;
      }
    }
    throw new AssertionError("the scaffold supplies a Show-surface toggle");
  }

  // ---------------------------------------------------------------- layout

  @Test
  void scaffoldCarriesAllThreeRegions() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.setStage(new ElwhaButton("Common button"));

    assertThat(ShowcaseFixture.findFirst(workbench, ElwhaSurface.class))
        .as("the stage")
        .isNotNull();
    assertThat(ShowcaseFixture.findFirst(workbench, WorkbenchControls.class))
        .as("the controls column")
        .isNotNull();
    assertThat(ShowcaseFixture.findFirst(workbench, CodeView.class))
        .as("the code view")
        .isNotNull();
  }

  @Test
  void controlsColumnIsTheOneBuildersPopulate() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    workbench.controls().addControl("Variant", new JLabel("stand-in"));

    assertThat(ShowcaseFixture.descendants(workbench))
        .as("controls() hands back a column already mounted in the scaffold")
        .contains(workbench.controls());
    assertThat(ShowcaseFixture.findAll(workbench.controls(), JLabel.class))
        .extracting(JLabel::getText)
        .contains("Variant");
  }
}
