package com.owspfm.elwha.theme;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * The bridge that makes raw Swing inherit the Elwha design language — the curated mapping of
 * FlatLaf's component {@link UIManager} keys onto Elwha token roles.
 *
 * <p>FlatLaf exposes hundreds of {@code UIManager} keys; this class maps a deliberately small,
 * hand-picked subset of them onto {@link ColorRole}s, {@link ShapeScale}s, baked {@link
 * StateLayer}s, and {@link TypeRole}s so that a plain {@code JButton} or {@code JTextField} looks
 * like it belongs next to a {@code ElwhaChip}. The full table is the locked appendix of {@code
 * elwha-theme-install-api.md}.
 *
 * <p>Three halves, applied by {@link ElwhaTheme#install} as steps 5, 6 and 7:
 *
 * <ul>
 *   <li>{@link #applyStaticKeys(Palette)} — direct role-to-key assignments.
 *   <li>{@link #applyStateLayerKeys(Palette)} — the M3 state-layer overlays, computed and baked
 *       into FlatLaf's discrete {@code *hoverBackground} / {@code *pressedBackground} keys.
 *   <li>{@link #applyFontKeys(Typography)} — the per-component font keys and FlatLaf's semantic
 *       style-class ladder, mapped onto the type roles M3 assigns each archetype. Type is the one
 *       axis the bridge cannot carry all the way: M3 names a role per <em>usage</em> and Swing keys
 *       fonts per component class, so the ladder is what a consumer marks a heading with. See that
 *       method for the boundary.
 * </ul>
 *
 * <p>Values are pulled from the {@link Palette} being installed rather than re-resolved through
 * {@link ColorRole#resolve()} — the palette is the single source of truth for the install in
 * progress. Every color is written as a {@link ColorUIResource}: a plain {@code Color} would be
 * installed onto a component once and then never replaced on a subsequent {@code updateUI()} (Swing
 * treats non-{@code UIResource} values as developer-owned), which would freeze raw Swing on the
 * first-installed theme and break runtime mode switching. Fonts are written as {@link
 * FontUIResource} for exactly the same reason.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
final class FlatLafKeyMapping {

  private FlatLafKeyMapping() {}

  /**
   * Writes the direct role-to-key assignments — backgrounds, foregrounds, borders, focus color, and
   * the global corner-arc default — onto FlatLaf's component keys.
   *
   * <p>Keys whose final value is a baked state layer are written only by {@link
   * #applyStateLayerKeys(Palette)}, which always runs after this method — writing them here too
   * would be dead.
   *
   * @param palette the palette being installed
   * @version v0.5.0
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
    // Focus model: a focus ring is the indicator. Component.focusWidth (FlatLaf-default 0) is
    // raised so a primary-colored ring is drawn around any focused component; the resting
    // border and fill are left unchanged on focus (the *.focused* keys below all resolve to
    // their resting equivalents) so the ring is the single, consistent focus cue everywhere.
    putColor("Component.focusColor", primary);
    putColor("Component.focusedBorderColor", outline);
    UIManager.put("Component.focusWidth", 2);
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
    // Focus & hover borders: the focus ring (Component.focusWidth, above) is the focus cue, so
    // every *.focused* key resolves to its resting equivalent — focus swaps neither the fill
    // nor the border. These keys must still be mapped explicitly: an unmapped one falls through
    // to FlatLaf's blue accent default rather than being inert.
    putColor("Button.focusedBackground", surfaceContainerLow);
    putColor("Button.focusedBorderColor", outline);
    putColor("Button.hoverBorderColor", outline);
    putColor("Button.default.focusedBackground", primary);
    putColor("Button.default.focusedBorderColor", primary);
    putColor("Button.default.focusColor", primary);
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
    // FlatRadioButtonIcon extends FlatCheckBoxIcon and reads its color fields from literal
    // "CheckBox.icon.*" keys (only RadioButton.icon.style and .centerDiameter are radio-specific),
    // so this one CheckBox.icon palette colors both the checkbox and the radio. JCheckBox /
    // JRadioButton label colors stay on their own keys.
    putColor("CheckBox.background", surface);
    putColor("CheckBox.foreground", onSurface);
    putColor("RadioButton.background", surface);
    putColor("RadioButton.foreground", onSurface);
    putColor("CheckBox.icon.background", surface);
    putColor("CheckBox.icon.borderColor", outline);
    putColor("CheckBox.icon.selectedBackground", primary);
    putColor("CheckBox.icon.selectedBorderColor", primary);
    putColor("CheckBox.icon.checkmarkColor", onPrimary);
    // Per the focus-ring model, state doesn't change the icon's fill or border — the icon's own
    // primary focus ring is the cue. focusedSelectedBackground / focusedSelectedBorderColor MUST
    // be set explicitly: their fallback is focusedBackground / focusedBorderColor (NOT the
    // selected variants), so an unset focusedSelectedBackground makes a focused, checked icon
    // render as empty.
    putColor("CheckBox.icon.focusedBackground", surface);
    putColor("CheckBox.icon.focusedBorderColor", outline);
    putColor("CheckBox.icon.focusedSelectedBackground", primary);
    putColor("CheckBox.icon.focusedSelectedBorderColor", primary);
    putColor("CheckBox.icon.hoverBorderColor", outline);
    putColor("CheckBox.icon.pressedBorderColor", outline);

    // Radio button: give it a separate FlatLaf icon style ("outlined") so the shared icon
    // palette can split — checkbox keeps its filled look (primary fill, white check) and the
    // radio gets the canonical M3 ring (primary ring with primary dot, transparent inside,
    // thicker ring when checked). The radio reads CheckBox.icon[outlined].* first and falls
    // back to the base CheckBox.icon.* palette for anything not overridden.
    UIManager.put("RadioButton.icon.style", "outlined");
    putColor("CheckBox.icon[outlined].selectedBackground", surface);
    putColor("CheckBox.icon[outlined].focusedSelectedBackground", surface);
    putColor("CheckBox.icon[outlined].focusedSelectedBorderColor", primary);
    putColor("CheckBox.icon[outlined].checkmarkColor", primary);
    UIManager.put("CheckBox.icon[outlined].selectedBorderWidth", 2f);

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
   * once, here, and the resulting opaque color is written. Elwha's own components do not use these
   * baked keys — they apply the overlay live at paint time.
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

  /**
   * Writes the type half of the bridge — FlatLaf's per-component font keys and its semantic
   * font-style ladder, mapped onto the {@link TypeRole} M3 names each archetype.
   *
   * <p>Runs after {@code defaultFont} is written, because the rungs deliberately left out below are
   * derived from it.
   *
   * <p><strong>What this can and cannot express.</strong> M3 assigns a type role to a component
   * <em>archetype</em> — a button label is Label Large, a menu item is Label Large, a tab is Title
   * Small — and Swing keys fonts by component class, so those assignments transfer exactly. What
   * does not transfer is role-by-usage: Swing has one {@code Label.font}, so a {@code JLabel}
   * standing in for a headline and one carrying body text read the same key and cannot be told
   * apart from here. The ladder below is the answer to that, and it is FlatLaf's own: a consumer
   * marks the heading with {@code putClientProperty(FlatClientProperties.STYLE_CLASS, "h2")} and it
   * resolves to Headline Small. Bridging the ladder is what makes the M3 scale reachable from raw
   * Swing at all; without it the roles exist only for components that ask {@code
   * TypeRole.resolve()} directly.
   *
   * <p>Three of FlatLaf's rungs are left to its own derivation on purpose. {@code medium.font}
   * (default −1) and {@code mini.font} (default −3) land on sizes the M3 scale has no step for, so
   * snapping them would either collapse two rungs onto one font or invent a role; derived, they
   * keep the installed family and stay monotonic between the rungs on either side. {@code
   * light.font} has no Inter face to point at.
   *
   * @param typography the typography being installed
   * @version v0.5.0
   * @since v0.5.0
   */
  static void applyFontKeys(Typography typography) {
    Font displaySmall = typography.get(TypeRole.DISPLAY_SMALL);
    Font headlineLarge = typography.get(TypeRole.HEADLINE_LARGE);
    Font headlineMedium = typography.get(TypeRole.HEADLINE_MEDIUM);
    Font headlineSmall = typography.get(TypeRole.HEADLINE_SMALL);
    Font titleLarge = typography.get(TypeRole.TITLE_LARGE);
    Font titleMedium = typography.get(TypeRole.TITLE_MEDIUM);
    Font titleSmall = typography.get(TypeRole.TITLE_SMALL);
    Font bodyLarge = typography.get(TypeRole.BODY_LARGE);
    Font bodyMedium = typography.get(TypeRole.BODY_MEDIUM);
    Font bodySmall = typography.get(TypeRole.BODY_SMALL);
    Font labelLarge = typography.get(TypeRole.LABEL_LARGE);
    Font labelMedium = typography.get(TypeRole.LABEL_MEDIUM);

    // --- Actions: M3 gives a button label Label Large ---
    putFont("Button.font", labelLarge);
    putFont("ToggleButton.font", labelLarge);
    putFont("ToolBar.font", labelLarge);
    putFont("OptionPane.buttonFont", labelLarge);

    // --- Selection controls: the attached label is Label Large ---
    putFont("CheckBox.font", labelLarge);
    putFont("RadioButton.font", labelLarge);

    // --- Text input: M3 field input text is Body Large ---
    putFont("TextField.font", bodyLarge);
    putFont("FormattedTextField.font", bodyLarge);
    putFont("PasswordField.font", bodyLarge);
    putFont("TextArea.font", bodyLarge);
    putFont("TextPane.font", bodyLarge);
    putFont("EditorPane.font", bodyLarge);
    putFont("ComboBox.font", bodyLarge);
    putFont("Spinner.font", bodyLarge);

    // --- Collections: an M3 list item headline is Body Large; a column header is a small title ---
    putFont("List.font", bodyLarge);
    putFont("Table.font", bodyLarge);
    putFont("Tree.font", bodyLarge);
    putFont("TableHeader.font", titleSmall);

    // --- Menus: an M3 menu item label is Label Large ---
    putFont("Menu.font", labelLarge);
    putFont("MenuBar.font", labelLarge);
    putFont("MenuItem.font", labelLarge);
    putFont("CheckBoxMenuItem.font", labelLarge);
    putFont("RadioButtonMenuItem.font", labelLarge);
    putFont("PopupMenu.font", labelLarge);
    putFont("MenuItem.acceleratorFont", labelMedium);

    // --- Text and chrome ---
    putFont("Label.font", bodyMedium);
    putFont("OptionPane.messageFont", bodyMedium);
    putFont("ToolTip.font", bodySmall);
    putFont("TitledBorder.font", titleSmall);
    putFont("TabbedPane.font", titleSmall);
    putFont("InternalFrame.titleFont", titleMedium);

    // --- Status: readouts sit alongside the control, not in the reading flow ---
    putFont("ProgressBar.font", labelMedium);
    putFont("Slider.font", labelMedium);

    // --- FlatLaf's semantic style-class ladder: how raw Swing names a role per usage ---
    // Left un-bridged, h1-h3 resolve through $semibold.font, which falls back to a system face
    // when the installed family has no semibold — so the headings escaped the theme's own font
    // entirely (measured: Helvetica Neue against Inter everywhere else).
    putFont("h00.font", displaySmall);
    putFont("h0.font", headlineLarge);
    putFont("h1.font", headlineMedium);
    putFont("h2.font", headlineSmall);
    putFont("h3.font", titleLarge);
    putFont("h4.font", titleMedium);
    putFont("large.font", bodyLarge);
    putFont("small.font", bodySmall);
    putFont("semibold.font", labelLarge);
  }

  // Always store colors as ColorUIResource so updateUI() will re-install them on a theme switch.
  private static void putColor(String key, Color value) {
    UIManager.put(key, new ColorUIResource(value));
  }

  // FontUIResource for the same reason as ColorUIResource — see the class javadoc.
  private static void putFont(String key, Font value) {
    UIManager.put(key, new FontUIResource(value));
  }
}
