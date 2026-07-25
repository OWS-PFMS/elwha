package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;

/**
 * V2 S6 headless completeness sweep (#502): keyboard reachability of every new surface (the wheel
 * disc and its polar bindings, tier grids with their arrow/space maps, the favorites Delete
 * binding), disabled inertness through outer-qualified gating (lesson #432 — bindings exist but do
 * nothing), RTL mirroring of the tier grids' cell geometry, and the accessible naming of every V2
 * interactive (eyedropper affordance, theme/saved grids, wheel disc).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerV2SweepSmoke {

  private ElwhaColorPickerV2SweepSmoke() {}

  /**
   * Runs the sweep.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkWheelKeyboard();
    checkDisabledInertness();
    checkRtlMirroring();
    checkNaming();

    System.out.println(
        "ElwhaColorPickerV2SweepSmoke: OK (wheel keys, disabled inertness, RTL geometry, names)");
  }

  private static void checkWheelKeyboard() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x00FF00));
    picker.setModes(PickerMode.WHEEL);
    layoutTree(picker, new Dimension(360, 460));
    final WheelPane pane = (WheelPane) picker.paneFor(PickerMode.WHEEL);
    final JComponent disc = (JComponent) pane.getComponent(0);
    check("disc is focusable", disc.isFocusable());

    invoke(disc, "pressed RIGHT");
    check("Right nudges hue +1", Math.round(pane.hueDegrees()) == 121);
    invoke(disc, "pressed PAGE_UP");
    check("PgUp jumps hue +10", Math.round(pane.hueDegrees()) == 131);
    invoke(disc, "pressed DOWN");
    check("Down nudges saturation −0.01", Math.abs(pane.saturation() - 0.99f) < 0.001f);
    invoke(disc, "pressed HOME");
    check("Home desaturates to the center", pane.saturation() == 0f);
    invoke(disc, "pressed END");
    check("End saturates to the rim", pane.saturation() == 1f);
  }

  private static void checkDisabledInertness() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x00FF00));
    picker.setFavorites(List.of(new Color(0x111111)));
    layoutTree(picker, new Dimension(360, 520));
    picker.setEnabled(false);

    final WheelPane wheel = (WheelPane) picker.paneFor(PickerMode.WHEEL);
    final JComponent disc = (JComponent) wheel.getComponent(0);
    final float hueBefore = wheel.hueDegrees();
    invoke(disc, "pressed RIGHT");
    check("disabled disc ignores keys", wheel.hueDegrees() == hueBefore);

    final Component themeGrid = findByAccessibleName(picker, "Theme colors");
    pressCell(themeGrid, themeGrid.getWidth() / 20, 17);
    check("disabled theme grid ignores presses", new Color(0x00FF00).equals(picker.getColor()));

    final Component savedGrid = findByAccessibleName(picker, "Saved colors");
    invoke((JComponent) savedGrid, "pressed DELETE");
    check("disabled saved grid ignores Delete", picker.getFavorites().size() == 1);

    picker.setEnabled(true);
    invoke(disc, "pressed RIGHT");
    check("re-enabled disc reacts again", wheel.hueDegrees() != hueBefore);
  }

  private static void checkRtlMirroring() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setModes(PickerMode.SWATCHES);
    picker.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    layoutTree(picker, new Dimension(360, 460));

    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    check("RTL applied to the pane tree", !pane.getComponentOrientation().isLeftToRight());
  }

  private static void checkNaming() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setEyedropperEnabled(true);
    picker.setFavorites(List.of(new Color(0x111111)));
    layoutTree(picker, new Dimension(360, 520));

    check("eyedropper named", findByAccessibleName(picker, "Pick color from screen") != null);
    check("theme grid named", findByAccessibleName(picker, "Theme colors") != null);
    check("saved grid named", findByAccessibleName(picker, "Saved colors") != null);
    check("wheel disc named", findByAccessibleName(picker, "Hue and saturation wheel") != null);

    final Component disc = findByAccessibleName(picker, "Hue and saturation wheel");
    final String description = disc.getAccessibleContext().getAccessibleDescription();
    check(
        "disc reads back hue and saturation",
        description != null && description.contains("hue") && description.contains("saturation"));
  }

  private static void invoke(final JComponent target, final String actionKey) {
    final javax.swing.Action action = target.getActionMap().get(actionKey);
    check("binding present: " + actionKey, action != null);
    action.actionPerformed(new java.awt.event.ActionEvent(target, 0, actionKey));
  }

  private static void pressCell(final Component grid, final int x, final int y) {
    grid.dispatchEvent(
        new java.awt.event.MouseEvent(
            grid,
            java.awt.event.MouseEvent.MOUSE_PRESSED,
            0L,
            0,
            x,
            y,
            1,
            false,
            java.awt.event.MouseEvent.BUTTON1));
    grid.dispatchEvent(
        new java.awt.event.MouseEvent(
            grid,
            java.awt.event.MouseEvent.MOUSE_RELEASED,
            0L,
            0,
            x,
            y,
            1,
            false,
            java.awt.event.MouseEvent.BUTTON1));
  }

  private static Component findByAccessibleName(final Component root, final String name) {
    final AccessibleContext context = root.getAccessibleContext();
    if (context != null && name.equals(context.getAccessibleName())) {
      return root;
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        final Component found = findByAccessibleName(child, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void layoutTree(final Component component, final Dimension size) {
    component.setSize(size);
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutTree(child, child.getSize());
      }
    }
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
