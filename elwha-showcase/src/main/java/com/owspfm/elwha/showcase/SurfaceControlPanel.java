package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * The reusable surface control set — the {@code role / shape / border} pickers (plus, in stage
 * mode, {@code size} and {@code visibility}) that drive one {@link ElwhaSurface}.
 *
 * <p>Used in two places, so the control set never diverges:
 *
 * <ul>
 *   <li><strong>Stage mode</strong> ({@code stage = true}) — every {@link ComponentWorkbench}
 *       builds one to configure the surface its live component sits on. Adds a Size picker and a
 *       Show-surface toggle on top of the {@code ElwhaSurface} axes.
 *   <li><strong>Component mode</strong> ({@code stage = false}) — the Surface Workbench's own
 *       <em>Component</em> segment uses one to configure the {@code ElwhaSurface} being
 *       demonstrated, which then sits on a stage-mode surface (surface-on-surface).
 * </ul>
 *
 * <p>The panel populates a caller-supplied {@link WorkbenchControls} column, owns the {@link
 * ElwhaSurface} it drives, renders the equivalent Java, and notifies registered listeners on every
 * change.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class SurfaceControlPanel {

  private final ElwhaSurface surface = new ElwhaSurface();
  private final boolean stage;
  private final List<Runnable> changeListeners = new ArrayList<>();

  private final ElwhaSelectField<ColorRole> roleBox = ElwhaSelectField.outlined("Surface role");
  private final ElwhaSelectField<ShapeScale> shapeBox = ElwhaSelectField.outlined("Shape");
  private final ElwhaSelectField<BorderOption> borderBox = ElwhaSelectField.outlined("Border role");
  private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 2, 1));
  private final JSpinner elevationSpinner =
      new JSpinner(new SpinnerNumberModel(0, 0, ElwhaSurface.MAX_ELEVATION, 1));
  private final ElwhaSelectField<StageSize> sizeBox = ElwhaSelectField.outlined("Size");
  private final ElwhaCheckbox visibleBox = new ElwhaCheckbox("Show surface");

  /**
   * Builds the control set into the given column and applies the initial configuration to a fresh
   * {@link ElwhaSurface}.
   *
   * @param into the controls column to populate
   * @param stage {@code true} to add the stage-only Size + visibility controls and default the fill
   *     to a container role; {@code false} for a bare surface configurator
   * @version v0.5.0
   * @since v0.3.0
   */
  public SurfaceControlPanel(final WorkbenchControls into, final boolean stage) {
    this.stage = stage;
    visibleBox.setChecked(true);
    roleBox.setOptions(List.of(ColorRole.values()));
    roleBox.setSelectedValue(stage ? ColorRole.SURFACE_CONTAINER_HIGH : ColorRole.SURFACE);
    shapeBox.setOptions(List.of(ShapeScale.values()));
    shapeBox.setSelectedValue(surface.getShape());
    borderBox.setOptions(List.of(BorderOption.values()));
    borderBox.setSelectedValue(BorderOption.NONE);
    sizeBox.setOptions(List.of(StageSize.values()));
    sizeBox.setSelectedValue(StageSize.MEDIUM);

    into.addSection("Surface");
    into.addControl("", roleBox);
    into.addControl("", shapeBox);
    into.addControl("Elevation", elevationSpinner);
    into.addSection("Border");
    into.addControl("", borderBox);
    into.addControl("Border width", widthSpinner);
    if (stage) {
      into.addSection("Stage");
      into.addControl("", sizeBox);
      into.addControl("", visibleBox);
    }

    roleBox.addSelectionChangeListener(value -> fireChanged());
    shapeBox.addSelectionChangeListener(value -> fireChanged());
    borderBox.addSelectionChangeListener(value -> fireChanged());
    widthSpinner.addChangeListener(event -> fireChanged());
    elevationSpinner.addChangeListener(event -> fireChanged());
    sizeBox.addSelectionChangeListener(value -> fireChanged());
    visibleBox.addActionListener(event -> fireChanged());

    applyToSurface();
  }

  /**
   * Returns the surface this panel drives — the same instance across the panel's lifetime.
   *
   * @return the driven surface
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaSurface surface() {
    return surface;
  }

  /**
   * Returns whether the surface should be shown. Always {@code true} in component mode; in stage
   * mode it tracks the Show-surface toggle.
   *
   * @return {@code true} if the surface is visible
   * @version v0.4.0
   * @since v0.3.0
   */
  public boolean isSurfaceVisible() {
    return !stage || visibleBox.isChecked();
  }

  /**
   * Returns the size chosen on the stage Size control — the floor a {@link ComponentWorkbench}
   * sizes its stage surface to before growing it to fit a larger component.
   *
   * @return the chosen stage size
   * @version v0.5.0
   * @since v0.3.0
   */
  public Dimension chosenStageSize() {
    return sizeBox.getSelectedValue().size();
  }

  /**
   * Registers a listener invoked after every control change, once the change has been applied to
   * the surface.
   *
   * @param listener the change listener
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addChangeListener(final Runnable listener) {
    changeListeners.add(listener);
  }

  /**
   * Renders the equivalent Java for the current configuration.
   *
   * @return the equivalent-Java snippet
   * @version v0.5.0
   * @since v0.3.0
   */
  public String code() {
    if (stage && !visibleBox.isChecked()) {
      return "// Surface hidden — the component sits on the bare stage background.";
    }
    final StringBuilder code = new StringBuilder(224);
    code.append(stage ? "ElwhaSurface stage = new ElwhaSurface()\n" : "new ElwhaSurface()\n");
    code.append("    .setSurfaceRole(ColorRole.").append(roleBox.getSelectedValue()).append(")\n");
    code.append("    .setShape(ShapeScale.").append(shapeBox.getSelectedValue()).append(")");
    final int elevation = (Integer) elevationSpinner.getValue();
    if (elevation > 0) {
      code.append("\n    .setElevation(").append(elevation).append(")");
    }
    final BorderOption border = borderBox.getSelectedValue();
    if (border != null && border.role != null) {
      code.append("\n    .setBorderRole(ColorRole.").append(border.role).append(")");
      code.append("\n    .setBorderWidth(").append(widthSpinner.getValue()).append(")");
    }
    code.append(";");
    if (stage) {
      final Dimension size = sizeBox.getSelectedValue().size();
      code.append("\nstage.setPreferredSize(new Dimension(")
          .append(size.width)
          .append(", ")
          .append(size.height)
          .append("));");
      code.append("\nstage.add(component);   // the component sits centered on the surface");
    }
    return code.toString();
  }

  private void fireChanged() {
    applyToSurface();
    changeListeners.forEach(Runnable::run);
  }

  private void applyToSurface() {
    surface.setSurfaceRole(roleBox.getSelectedValue());
    surface.setShape(shapeBox.getSelectedValue());
    surface.setElevation((Integer) elevationSpinner.getValue());
    final BorderOption border = borderBox.getSelectedValue();
    surface.setBorderRole(border == null ? null : border.role);
    surface.setBorderWidth((Integer) widthSpinner.getValue());
    // In stage mode the hosting ComponentWorkbench owns the surface's preferred size (it grows the
    // chosen size to fit a larger component); only the standalone demoed surface sizes itself here.
    if (!stage) {
      surface.setPreferredSize(StageSize.SMALL.size());
    }
  }

  /** Wraps a nullable border {@link ColorRole} as a select option — {@code NONE} maps to null. */
  private enum BorderOption {
    NONE(null),
    OUTLINE(ColorRole.OUTLINE),
    OUTLINE_VARIANT(ColorRole.OUTLINE_VARIANT),
    PRIMARY(ColorRole.PRIMARY),
    SECONDARY(ColorRole.SECONDARY),
    TERTIARY(ColorRole.TERTIARY),
    ERROR(ColorRole.ERROR);

    private final ColorRole role;

    BorderOption(final ColorRole role) {
      this.role = role;
    }
  }

  /** A named stage-surface size — the preferred size the component is centered within. */
  private enum StageSize {
    SMALL(260, 170),
    MEDIUM(340, 220),
    LARGE(460, 300);

    private final int width;
    private final int height;

    StageSize(final int width, final int height) {
      this.width = width;
      this.height = height;
    }

    Dimension size() {
      return new Dimension(width, height);
    }
  }
}
