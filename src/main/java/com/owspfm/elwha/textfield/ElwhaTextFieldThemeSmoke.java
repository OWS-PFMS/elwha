package com.owspfm.elwha.textfield;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import javax.swing.SwingUtilities;

/**
 * Headless guard for theme-switch re-theming: a field constructed under one mode must re-resolve
 * its editor text and caret colors when {@code ElwhaTheme.install} switches modes (the
 * updateComponentTreeUI pass). Editor colors are developer-set plain {@code Color}s, which Swing's
 * UIResource contract deliberately preserves across {@code updateUI()} — so the re-resolve must be
 * explicit, or a light-built field paints near-black text on the dark surface (the color-picker
 * hex-field smoke finding; ElwhaSelectField inherits the fix through its embedded field).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaTextFieldThemeSmoke {

  private ElwhaTextFieldThemeSmoke() {}

  /**
   * Runs the guard.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    final ElwhaTextField field = ElwhaTextField.outlined("Hex");
    field.setText("#0D47A1");
    check(
        "light-built editor uses light ON_SURFACE",
        field.getEditor().getForeground().equals(ColorRole.ON_SURFACE.resolve()));

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    SwingUtilities.updateComponentTreeUI(field);
    check(
        "editor text re-resolves to dark ON_SURFACE after the switch",
        field.getEditor().getForeground().equals(ColorRole.ON_SURFACE.resolve()));
    check(
        "caret re-resolves to dark PRIMARY after the switch",
        field.getEditor().getCaretColor().equals(ColorRole.PRIMARY.resolve()));

    field.setError(true);
    SwingUtilities.updateComponentTreeUI(field);
    check(
        "error caret stays ERROR through the re-theme",
        field.getEditor().getCaretColor().equals(ColorRole.ERROR.resolve()));

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.updateComponentTreeUI(field);
    check(
        "switching back restores light colors",
        field.getEditor().getForeground().equals(ColorRole.ON_SURFACE.resolve()));

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    final ElwhaTextField detached = ElwhaTextField.outlined("Hex");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    // No tree walk reaches a detached field — mounting must heal it (the dialog repro).
    detached.addNotify();
    check(
        "detached-built field re-resolves on mount",
        detached.getEditor().getForeground().equals(ColorRole.ON_SURFACE.resolve()));
    check(
        "detached-built caret re-resolves on mount",
        detached.getEditor().getCaretColor().equals(ColorRole.PRIMARY.resolve()));

    System.out.println(
        "ElwhaTextFieldThemeSmoke: OK (in-hierarchy + detached-mount editor re-theme)");
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
