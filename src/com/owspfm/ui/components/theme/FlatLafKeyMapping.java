package com.owspfm.ui.components.theme;

import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/**
 * The bridge that makes raw Swing inherit the FlatComp design language — the curated mapping of
 * FlatLaf's component {@link UIManager} keys onto FlatComp token roles.
 *
 * <p>FlatLaf exposes hundreds of {@code UIManager} keys; this class maps a deliberately small,
 * hand-picked subset of them onto {@link ColorRole}s, {@link ShapeScale}s, and baked {@link
 * StateLayer}s so that a plain {@code JButton} or {@code JTextField} looks like it belongs next to
 * a {@code FlatChip}. The full table is the locked appendix of {@code
 * flatcomp-theme-install-api.md}.
 *
 * <p>Two halves, applied by {@link FlatCompTheme#install} as steps 5 and 6:
 *
 * <ul>
 *   <li>{@link #applyStaticKeys(Palette)} — direct role-to-key assignments.
 *   <li>{@link #applyStateLayerKeys(Palette)} — the M3 state-layer overlays, computed and baked
 *       into FlatLaf's discrete {@code *hoverBackground} / {@code *pressedBackground} keys.
 * </ul>
 *
 * <p>Values are pulled from the {@link Palette} being installed rather than re-resolved through
 * {@link ColorRole#resolve()} — the palette is the single source of truth for the install in
 * progress. Every color is written as a {@link ColorUIResource}: a plain {@code Color} would be
 * installed onto a component once and then never replaced on a subsequent {@code updateUI()} (Swing
 * treats non-{@code UIResource} values as developer-owned), which would freeze raw Swing on the
 * first-installed theme and break runtime mode switching.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
final class FlatLafKeyMapping {

  private FlatLafKeyMapping() {}

  /**
   * Writes the direct role-to-key assignments — backgrounds, foregrounds, borders, focus color, and
   * the global corner-arc default — onto FlatLaf's component keys.
   *
   * @param palette the palette being installed
   * @version v0.1.0
   * @since v0.1.0
   */
  static void applyStaticKeys(Palette palette) {
    Color surface = palette.get(ColorRole.SURFACE);
    Color onSurface = palette.get(ColorRole.ON_SURFACE);
    Color surfaceVariant = palette.get(ColorRole.SURFACE_VARIANT);
    Color onSurfaceVariant = palette.get(ColorRole.ON_SURFACE_VARIANT);
    Color surfaceContainerLow = palette.get(ColorRole.SURFACE_CONTAINER_LOW);
    Color surfaceContainerHigh = palette.get(ColorRole.SURFACE_CONTAINER_HIGH);
    Color primary = palette.get(ColorRole.PRIMARY);
    Color onPrimary = palette.get(ColorRole.ON_PRIMARY);
    Color primaryContainer = palette.get(ColorRole.PRIMARY_CONTAINER);
    Color onPrimaryContainer = palette.get(ColorRole.ON_PRIMARY_CONTAINER);
    Color outline = palette.get(ColorRole.OUTLINE);
    Color outlineVariant = palette.get(ColorRole.OUTLINE_VARIANT);
    Color error = palette.get(ColorRole.ERROR);
    Color inverseSurface = palette.get(ColorRole.INVERSE_SURFACE);
    Color inverseOnSurface = palette.get(ColorRole.INVERSE_ON_SURFACE);

    // --- Global component defaults ---
    putColor("Component.focusColor", primary);
    putColor("Component.focusedBorderColor", primary);
    putColor("Component.borderColor", outline);
    putColor("Component.disabledBorderColor", outlineVariant);
    UIManager.put("Component.arc", ShapeScale.SM.px());
    putColor("Component.error.borderColor", error);
    putColor("Component.error.focusedBorderColor", error);

    // --- Panels and root surfaces ---
    putColor("Panel.background", surface);
    putColor("Panel.foreground", onSurface);
    putColor("Viewport.background", surface);
    putColor("RootPane.background", surface);
    putColor("Separator.foreground", outlineVariant);
    putColor("Label.foreground", onSurface);
    putColor("Label.disabledForeground", onSurfaceVariant);

    // --- Buttons ---
    putColor("Button.background", surfaceContainerLow);
    putColor("Button.foreground", onSurface);
    putColor("Button.borderColor", outline);
    putColor("Button.disabledBorderColor", outlineVariant);
    putColor("Button.default.background", primary);
    putColor("Button.default.foreground", onPrimary);
    putColor("Button.default.borderColor", primary);
    // Focus & hover borders: FlatLaf shows focus/hover as border-color and fill changes, not an
    // outer ring (Component.focusWidth defaults to 0). EVERY such key must be mapped — an
    // unmapped one falls through to FlatLaf's blue accent default. focusedBackground is kept
    // equal to the normal background so focus never swaps the fill (which reads as a stuck
    // pressed / toggled state); a focused control is distinguished by its border color alone.
    putColor("Button.focusedBackground", surfaceContainerLow);
    putColor("Button.focusedBorderColor", primary);
    putColor("Button.hoverBorderColor", outline);
    putColor("Button.default.focusedBackground", primary);
    putColor("Button.default.focusedBorderColor", onPrimary);
    putColor("Button.default.focusColor", onPrimary);
    putColor("Button.default.hoverBorderColor", primary);
    putColor("ToggleButton.background", surfaceContainerLow);
    putColor("ToggleButton.foreground", onSurface);
    putColor("ToggleButton.selectedBackground", primaryContainer);
    putColor("ToggleButton.selectedForeground", onPrimaryContainer);

    // --- Text components ---
    putColor("TextField.background", surface);
    putColor("TextField.foreground", onSurface);
    putColor("TextField.placeholderForeground", onSurfaceVariant);
    putColor("FormattedTextField.background", surface);
    putColor("FormattedTextField.foreground", onSurface);
    putColor("PasswordField.background", surface);
    putColor("PasswordField.foreground", onSurface);
    putColor("TextArea.background", surface);
    putColor("TextArea.foreground", onSurface);
    putColor("EditorPane.background", surface);
    putColor("EditorPane.foreground", onSurface);
    putColor("TextComponent.selectionBackground", primaryContainer);
    putColor("TextComponent.selectionForeground", onPrimaryContainer);

    // --- Combo / spinner ---
    putColor("ComboBox.background", surface);
    putColor("ComboBox.foreground", onSurface);
    putColor("ComboBox.buttonBackground", surfaceContainerLow);
    putColor("Spinner.background", surface);
    putColor("Spinner.foreground", onSurface);

    // --- Selection controls ---
    putColor("CheckBox.background", surface);
    putColor("CheckBox.foreground", onSurface);
    putColor("CheckBox.icon.checkmarkColor", onPrimary);
    putColor("CheckBox.icon.selectedBackground", primary);
    putColor("CheckBox.icon.focusedBackground", surface);
    putColor("CheckBox.icon.focusedBorderColor", primary);
    putColor("CheckBox.icon[filled].focusedSelectedBackground", primary);
    putColor("CheckBox.icon[filled].focusedSelectedBorderColor", primary);
    putColor("CheckBox.icon[filled].focusedCheckmarkColor", onPrimary);
    putColor("RadioButton.background", surface);
    putColor("RadioButton.foreground", onSurface);
    putColor("RadioButton.icon.centerColor", onPrimary);
    putColor("RadioButton.icon.selectedBackground", primary);
    putColor("RadioButton.icon.focusedBackground", surface);
    putColor("RadioButton.icon.focusedBorderColor", primary);
    putColor("RadioButton.icon[filled].focusedSelectedBackground", primary);
    putColor("RadioButton.icon[filled].focusedSelectedBorderColor", primary);
    putColor("RadioButton.icon[filled].focusedCenterColor", onPrimary);

    // --- Lists, tables, trees ---
    putColor("List.background", surface);
    putColor("List.foreground", onSurface);
    putColor("List.selectionBackground", primaryContainer);
    putColor("List.selectionForeground", onPrimaryContainer);
    putColor("List.cellFocusColor", primary);
    putColor("Table.background", surface);
    putColor("Table.foreground", onSurface);
    putColor("Table.gridColor", outlineVariant);
    putColor("Table.cellFocusColor", primary);
    putColor("Table.selectionBackground", primaryContainer);
    putColor("Table.selectionForeground", onPrimaryContainer);
    putColor("TableHeader.background", surfaceContainerHigh);
    putColor("TableHeader.foreground", onSurfaceVariant);
    putColor("Tree.background", surface);
    putColor("Tree.foreground", onSurface);
    putColor("Tree.selectionBackground", primaryContainer);
    putColor("Tree.selectionForeground", onPrimaryContainer);

    // --- Scrollbars ---
    putColor("ScrollPane.background", surface);
    putColor("ScrollBar.track", surface);
    putColor("ScrollBar.thumb", outlineVariant);
    putColor("ScrollBar.hoverThumbColor", outline);
    putColor("ScrollBar.pressedThumbColor", onSurfaceVariant);

    // --- Tabs ---
    putColor("TabbedPane.background", surface);
    putColor("TabbedPane.foreground", onSurfaceVariant);
    putColor("TabbedPane.underlineColor", primary);
    putColor("TabbedPane.selectedForeground", onSurface);
    putColor("TabbedPane.hoverColor", surfaceVariant);

    // --- Progress, slider ---
    putColor("ProgressBar.background", surfaceVariant);
    putColor("ProgressBar.foreground", primary);
    putColor("Slider.background", surface);
    putColor("Slider.trackColor", surfaceVariant);
    putColor("Slider.trackValueColor", primary);
    putColor("Slider.thumbColor", primary);
    putColor("Slider.focusedColor", primary);

    // --- Menus ---
    putColor("MenuBar.background", surface);
    putColor("MenuBar.foreground", onSurface);
    putColor("PopupMenu.background", surfaceContainerHigh);
    putColor("MenuItem.foreground", onSurface);
    putColor("Menu.foreground", onSurface);

    // --- Tooltips — M3 uses the inverse surface ---
    putColor("ToolTip.background", inverseSurface);
    putColor("ToolTip.foreground", inverseOnSurface);
  }

  /**
   * Computes the M3 state-layer overlays and bakes the results into FlatLaf's discrete
   * interaction-state keys.
   *
   * <p>FlatLaf models hover / pressed / focused as separate flat colors; M3 models them as opacity
   * overlays. For each key that needs it, the overlay is alpha-blended over the base role color
   * once, here, and the resulting opaque color is written. FlatComp's own components do not use
   * these baked keys — they apply the overlay live at paint time.
   *
   * @param palette the palette being installed
   * @version v0.1.0
   * @since v0.1.0
   */
  static void applyStateLayerKeys(Palette palette) {
    Color surface = palette.get(ColorRole.SURFACE);
    Color onSurface = palette.get(ColorRole.ON_SURFACE);
    Color surfaceContainerLow = palette.get(ColorRole.SURFACE_CONTAINER_LOW);
    Color primary = palette.get(ColorRole.PRIMARY);
    Color onPrimary = palette.get(ColorRole.ON_PRIMARY);
    Color primaryContainer = palette.get(ColorRole.PRIMARY_CONTAINER);

    Color buttonHover = StateLayer.HOVER.over(surfaceContainerLow, onSurface);
    Color buttonPressed = StateLayer.PRESSED.over(surfaceContainerLow, onSurface);

    // Hover and pressed only — deliberately no *.focusedBackground bake. Focus is shown by
    // FlatLaf's focus ring (Component.focusColor); a focused-background fill persists after a
    // click and reads as a stuck "pressed" / toggled state, which it is not.
    putColor("Button.hoverBackground", buttonHover);
    putColor("Button.pressedBackground", buttonPressed);
    putColor("Button.default.hoverBackground", StateLayer.HOVER.over(primary, onPrimary));
    putColor("Button.default.pressedBackground", StateLayer.PRESSED.over(primary, onPrimary));

    putColor("ToggleButton.hoverBackground", buttonHover);
    putColor("ToggleButton.pressedBackground", buttonPressed);

    putColor("List.hoverBackground", StateLayer.HOVER.over(surface, onSurface));
    putColor("List.selectionInactiveBackground", StateLayer.SELECTED.over(surface, onSurface));
    putColor("Table.hoverBackground", StateLayer.HOVER.over(surface, onSurface));
    putColor("Tree.hoverBackground", StateLayer.HOVER.over(surface, onSurface));

    putColor("MenuItem.hoverBackground", StateLayer.HOVER.over(surface, onSurface));
    putColor("MenuItem.selectionBackground", primaryContainer);

    putColor("TabbedPane.hoverColor", StateLayer.HOVER.over(surface, onSurface));
    putColor("TabbedPane.focusColor", StateLayer.FOCUS.over(surface, primary));

    putColor("ComboBox.buttonHoverBackground", buttonHover);
    putColor("ComboBox.buttonPressedBackground", buttonPressed);
  }

  // Always store colors as ColorUIResource so updateUI() will re-install them on a theme switch.
  private static void putColor(String key, Color value) {
    UIManager.put(key, new ColorUIResource(value));
  }
}
