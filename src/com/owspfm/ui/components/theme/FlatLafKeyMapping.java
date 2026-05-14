package com.owspfm.ui.components.theme;

import java.awt.Color;
import javax.swing.UIManager;

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
 * progress.
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
    UIManager.put("Component.focusColor", primary);
    UIManager.put("Component.focusedBorderColor", primary);
    UIManager.put("Component.borderColor", outline);
    UIManager.put("Component.disabledBorderColor", outlineVariant);
    UIManager.put("Component.arc", ShapeScale.SM.px());
    UIManager.put("Component.error.borderColor", error);
    UIManager.put("Component.error.focusedBorderColor", error);

    // --- Panels and root surfaces ---
    UIManager.put("Panel.background", surface);
    UIManager.put("Panel.foreground", onSurface);
    UIManager.put("Viewport.background", surface);
    UIManager.put("RootPane.background", surface);
    UIManager.put("Separator.foreground", outlineVariant);
    UIManager.put("Label.foreground", onSurface);
    UIManager.put("Label.disabledForeground", onSurfaceVariant);

    // --- Buttons ---
    UIManager.put("Button.background", surfaceContainerLow);
    UIManager.put("Button.foreground", onSurface);
    UIManager.put("Button.borderColor", outline);
    UIManager.put("Button.disabledBorderColor", outlineVariant);
    UIManager.put("Button.default.background", primary);
    UIManager.put("Button.default.foreground", onPrimary);
    UIManager.put("Button.default.borderColor", primary);
    UIManager.put("ToggleButton.background", surfaceContainerLow);
    UIManager.put("ToggleButton.foreground", onSurface);
    UIManager.put("ToggleButton.selectedBackground", primaryContainer);
    UIManager.put("ToggleButton.selectedForeground", onPrimaryContainer);

    // --- Text components ---
    UIManager.put("TextField.background", surface);
    UIManager.put("TextField.foreground", onSurface);
    UIManager.put("TextField.placeholderForeground", onSurfaceVariant);
    UIManager.put("TextField.border", null);
    UIManager.put("FormattedTextField.background", surface);
    UIManager.put("FormattedTextField.foreground", onSurface);
    UIManager.put("PasswordField.background", surface);
    UIManager.put("PasswordField.foreground", onSurface);
    UIManager.put("TextArea.background", surface);
    UIManager.put("TextArea.foreground", onSurface);
    UIManager.put("EditorPane.background", surface);
    UIManager.put("EditorPane.foreground", onSurface);
    UIManager.put("TextComponent.selectionBackground", primaryContainer);
    UIManager.put("TextComponent.selectionForeground", onPrimaryContainer);

    // --- Combo / spinner ---
    UIManager.put("ComboBox.background", surface);
    UIManager.put("ComboBox.foreground", onSurface);
    UIManager.put("ComboBox.buttonBackground", surfaceContainerLow);
    UIManager.put("Spinner.background", surface);
    UIManager.put("Spinner.foreground", onSurface);

    // --- Selection controls ---
    UIManager.put("CheckBox.background", surface);
    UIManager.put("CheckBox.foreground", onSurface);
    UIManager.put("CheckBox.icon.checkmarkColor", onPrimary);
    UIManager.put("CheckBox.icon.selectedBackground", primary);
    UIManager.put("RadioButton.background", surface);
    UIManager.put("RadioButton.foreground", onSurface);
    UIManager.put("RadioButton.icon.centerColor", onPrimary);
    UIManager.put("RadioButton.icon.selectedBackground", primary);

    // --- Lists, tables, trees ---
    UIManager.put("List.background", surface);
    UIManager.put("List.foreground", onSurface);
    UIManager.put("List.selectionBackground", primaryContainer);
    UIManager.put("List.selectionForeground", onPrimaryContainer);
    UIManager.put("Table.background", surface);
    UIManager.put("Table.foreground", onSurface);
    UIManager.put("Table.gridColor", outlineVariant);
    UIManager.put("Table.selectionBackground", primaryContainer);
    UIManager.put("Table.selectionForeground", onPrimaryContainer);
    UIManager.put("TableHeader.background", surfaceContainerHigh);
    UIManager.put("TableHeader.foreground", onSurfaceVariant);
    UIManager.put("Tree.background", surface);
    UIManager.put("Tree.foreground", onSurface);
    UIManager.put("Tree.selectionBackground", primaryContainer);
    UIManager.put("Tree.selectionForeground", onPrimaryContainer);

    // --- Scrollbars ---
    UIManager.put("ScrollPane.background", surface);
    UIManager.put("ScrollBar.track", surface);
    UIManager.put("ScrollBar.thumb", outlineVariant);
    UIManager.put("ScrollBar.hoverThumbColor", outline);
    UIManager.put("ScrollBar.pressedThumbColor", onSurfaceVariant);

    // --- Tabs ---
    UIManager.put("TabbedPane.background", surface);
    UIManager.put("TabbedPane.foreground", onSurfaceVariant);
    UIManager.put("TabbedPane.underlineColor", primary);
    UIManager.put("TabbedPane.selectedForeground", onSurface);
    UIManager.put("TabbedPane.hoverColor", surfaceVariant);

    // --- Progress, slider ---
    UIManager.put("ProgressBar.background", surfaceVariant);
    UIManager.put("ProgressBar.foreground", primary);
    UIManager.put("Slider.background", surface);
    UIManager.put("Slider.trackColor", surfaceVariant);
    UIManager.put("Slider.thumbColor", primary);

    // --- Menus ---
    UIManager.put("MenuBar.background", surface);
    UIManager.put("MenuBar.foreground", onSurface);
    UIManager.put("PopupMenu.background", surfaceContainerHigh);
    UIManager.put("MenuItem.foreground", onSurface);
    UIManager.put("Menu.foreground", onSurface);

    // --- Tooltips — M3 uses the inverse surface ---
    UIManager.put("ToolTip.background", inverseSurface);
    UIManager.put("ToolTip.foreground", inverseOnSurface);
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
    Color buttonFocused = StateLayer.FOCUS.over(surfaceContainerLow, onSurface);

    UIManager.put("Button.hoverBackground", buttonHover);
    UIManager.put("Button.pressedBackground", buttonPressed);
    UIManager.put("Button.focusedBackground", buttonFocused);
    UIManager.put("Button.default.hoverBackground", StateLayer.HOVER.over(primary, onPrimary));
    UIManager.put("Button.default.pressedBackground", StateLayer.PRESSED.over(primary, onPrimary));
    UIManager.put("Button.default.focusedBackground", StateLayer.FOCUS.over(primary, onPrimary));

    UIManager.put("ToggleButton.hoverBackground", buttonHover);
    UIManager.put("ToggleButton.pressedBackground", buttonPressed);

    UIManager.put("List.hoverBackground", StateLayer.HOVER.over(surface, onSurface));
    UIManager.put("List.selectionInactiveBackground", StateLayer.SELECTED.over(surface, onSurface));
    UIManager.put("Table.hoverBackground", StateLayer.HOVER.over(surface, onSurface));
    UIManager.put("Tree.hoverBackground", StateLayer.HOVER.over(surface, onSurface));

    UIManager.put("MenuItem.hoverBackground", StateLayer.HOVER.over(surface, onSurface));
    UIManager.put("MenuItem.selectionBackground", primaryContainer);

    UIManager.put("TabbedPane.hoverColor", StateLayer.HOVER.over(surface, onSurface));
    UIManager.put("TabbedPane.focusColor", StateLayer.FOCUS.over(surface, primary));

    UIManager.put("ComboBox.buttonHoverBackground", buttonHover);
    UIManager.put("ComboBox.buttonPressedBackground", buttonPressed);
  }
}
