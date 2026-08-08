package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Component;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JSpinner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The shared surface control set — one panel driving one {@link ElwhaSurface}, in the two modes it
 * serves: stage mode (the ground under a workbench's live component, with Size and visibility) and
 * component mode (the Surface leaf's own subject).
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class SurfaceControlPanelTest {

  private static SurfaceControlPanel stagePanel(final WorkbenchControls into) {
    return new SurfaceControlPanel(into, true);
  }

  @SuppressWarnings("unchecked")
  private static ElwhaSelectField<Object> select(
      final WorkbenchControls column, final String label) {
    return (ElwhaSelectField<Object>) ShowcaseFixture.controlLabelled(column, label);
  }

  // ----------------------------------------------------------- the controls

  @Test
  void stageModeOffersTheSurfaceAxesPlusTheStageOnes() {
    final WorkbenchControls column = new WorkbenchControls();

    stagePanel(column);

    assertThat(ShowcaseFixture.controlLabelled(column, "Surface role")).isNotNull();
    assertThat(ShowcaseFixture.controlLabelled(column, "Shape")).isNotNull();
    assertThat(ShowcaseFixture.controlLabelled(column, "Elevation")).isNotNull();
    assertThat(ShowcaseFixture.controlLabelled(column, "Border role")).isNotNull();
    assertThat(ShowcaseFixture.controlLabelled(column, "Border width")).isNotNull();
    assertThat(ShowcaseFixture.controlLabelled(column, "Size"))
        .as("the stage's own size floor is a stage-mode control")
        .isNotNull();
  }

  @Test
  void componentModeDropsTheStageOnlyControls() {
    final WorkbenchControls column = new WorkbenchControls();

    new SurfaceControlPanel(column, false);

    assertThat(ShowcaseFixture.controlLabelled(column, "Surface role"))
        .as("the surface's own axes are the same in both modes")
        .isNotNull();
    assertThat(ShowcaseFixture.controlLabelled(column, "Size"))
        .as("a demonstrated surface sizes itself; there is no stage to floor")
        .isNull();
  }

  @Test
  void componentModeIsAlwaysVisible() {
    assertThat(new SurfaceControlPanel(new WorkbenchControls(), false).isSurfaceVisible())
        .as("only a stage surface can be hidden — hiding the subject makes no sense")
        .isTrue();
  }

  // -------------------------------------------------------------- the drive

  @Test
  void surfaceInstanceIsStableAcrossChanges() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);
    final ElwhaSurface surface = panel.surface();

    select(column, "Surface role").setSelectedValue(ColorRole.TERTIARY_CONTAINER);

    assertThat(panel.surface())
        .as("the workbench mounts the surface once; the panel reconfigures it in place")
        .isSameAs(surface);
  }

  @Test
  void choosingARoleAppliesItToTheSurface() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    select(column, "Surface role").setSelectedValue(ColorRole.TERTIARY_CONTAINER);

    assertThat(panel.surface().getSurfaceRole()).isEqualTo(ColorRole.TERTIARY_CONTAINER);
  }

  @Test
  void choosingAShapeAppliesItToTheSurface() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    select(column, "Shape").setSelectedValue(ShapeScale.XL);

    assertThat(panel.surface().getShape()).isEqualTo(ShapeScale.XL);
  }

  @Test
  void raisingElevationAppliesItToTheSurface() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    ((JSpinner) ShowcaseFixture.controlLabelled(column, "Elevation")).setValue(3);

    assertThat(panel.surface().getElevation()).isEqualTo(3);
  }

  @Test
  void everyChangeNotifiesTheHostingWorkbench() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);
    final AtomicInteger fired = new AtomicInteger();
    panel.addChangeListener(fired::incrementAndGet);

    select(column, "Surface role").setSelectedValue(ColorRole.TERTIARY_CONTAINER);
    ((JSpinner) ShowcaseFixture.controlLabelled(column, "Elevation")).setValue(2);

    assertThat(fired.get())
        .as("the workbench re-mounts and re-renders its code off this callback")
        .isEqualTo(2);
  }

  @Test
  void aChangeIsAppliedBeforeListenersHear() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);
    final AtomicInteger seen = new AtomicInteger(-1);
    panel.addChangeListener(() -> seen.set(panel.surface().getElevation()));

    ((JSpinner) ShowcaseFixture.controlLabelled(column, "Elevation")).setValue(4);

    assertThat(seen.get())
        .as("a listener that re-reads the surface must not see the pre-change value")
        .isEqualTo(4);
  }

  // --------------------------------------------------------------- the size

  @Test
  void stageOpensAtTheMediumSize() {
    assertThat(stagePanel(new WorkbenchControls()).chosenStageSize())
        .isEqualTo(new java.awt.Dimension(340, 220));
  }

  @Test
  void hidingTheStageSurfaceIsReportedToTheWorkbench() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    showToggle(column).doClick();

    assertThat(panel.isSurfaceVisible()).isFalse();
  }

  // --------------------------------------------------------------- the code

  @Test
  void codeNamesTheChosenRoleAndShape() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    select(column, "Surface role").setSelectedValue(ColorRole.TERTIARY_CONTAINER);
    select(column, "Shape").setSelectedValue(ShapeScale.XL);

    assertThat(panel.code()).contains("ColorRole.TERTIARY_CONTAINER").contains("ShapeScale.XL");
  }

  @Test
  void stageCodeShowsTheComponentBeingAdded() {
    assertThat(stagePanel(new WorkbenchControls()).code())
        .as("stage mode renders the surface-plus-component recipe, not a bare surface")
        .contains("stage.add(component)");
  }

  @Test
  void componentCodeIsABareSurface() {
    assertThat(new SurfaceControlPanel(new WorkbenchControls(), false).code())
        .as("component mode demonstrates the surface itself")
        .contains("new ElwhaSurface()")
        .doesNotContain("stage.add(component)");
  }

  @Test
  void aHiddenStageSurfaceSaysSoInsteadOfRenderingCode() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    showToggle(column).doClick();

    assertThat(panel.code()).contains("Surface hidden").doesNotContain("new ElwhaSurface");
  }

  @Test
  void codeOmitsABorderNobodyAskedFor() {
    assertThat(stagePanel(new WorkbenchControls()).code())
        .as("NONE maps to a null border role — rendering a setBorderRole(null) line would be noise")
        .doesNotContain("setBorderRole");
  }

  @Test
  void choosingABorderRoleRendersBothBorderCalls() {
    final WorkbenchControls column = new WorkbenchControls();
    final SurfaceControlPanel panel = stagePanel(column);

    // BorderOption is private to the panel, so the second option is named positionally: the list
    // opens with NONE, and anything after it is a real role.
    final ElwhaSelectField<Object> borderRole = select(column, "Border role");
    borderRole.setSelectedValue(borderRole.getOptions().get(1));

    assertThat(panel.code()).contains("setBorderRole").contains("setBorderWidth");
  }

  private static ElwhaCheckbox showToggle(final WorkbenchControls column) {
    for (final Component child : column.getComponents()) {
      if (child instanceof ElwhaCheckbox box && "Show surface".equals(box.getLabel())) {
        return box;
      }
    }
    throw new AssertionError("stage mode supplies a Show-surface toggle");
  }
}
