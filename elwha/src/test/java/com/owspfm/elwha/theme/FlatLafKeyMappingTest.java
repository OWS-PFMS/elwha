package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the raw-Swing bridge — that the curated FlatLaf {@code UIManager} keys carry
 * theme-derived values after an install, that the state-layer keys are the baked M3 overlay rather
 * than a flat role color, and that a mode switch moves them. Spot coverage of the load-bearing keys
 * rather than the whole table; the exhaustive list is Appendix A of {@code
 * docs/research/elwha-theme-install-api.md}.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class FlatLafKeyMappingTest {

  /** The direct role assignments a plain Swing component most visibly depends on. */
  private static Map<String, ColorRole> loadBearingStaticKeys() {
    final Map<String, ColorRole> keys = new LinkedHashMap<>();
    keys.put("Panel.background", ColorRole.SURFACE);
    keys.put("Label.foreground", ColorRole.ON_SURFACE);
    keys.put("RootPane.background", ColorRole.SURFACE);
    keys.put("Component.focusColor", ColorRole.PRIMARY);
    keys.put("Component.borderColor", ColorRole.OUTLINE);
    keys.put("TextField.background", ColorRole.SURFACE);
    keys.put("TextField.foreground", ColorRole.ON_SURFACE);
    keys.put("TextField.placeholderForeground", ColorRole.ON_SURFACE_VARIANT);
    keys.put("Button.background", ColorRole.SURFACE_CONTAINER_LOW);
    keys.put("Button.default.background", ColorRole.PRIMARY);
    keys.put("Button.default.foreground", ColorRole.ON_PRIMARY);
    keys.put("List.selectionBackground", ColorRole.PRIMARY_CONTAINER);
    keys.put("ScrollBar.thumb", ColorRole.OUTLINE_VARIANT);
    keys.put("Separator.foreground", ColorRole.OUTLINE_VARIANT);
    keys.put("ToolTip.background", ColorRole.INVERSE_SURFACE);
    keys.put("ToolTip.foreground", ColorRole.INVERSE_ON_SURFACE);
    keys.put("Component.error.borderColor", ColorRole.ERROR);
    return keys;
  }

  @Test
  void curatedKeysCarryTheirMappedRole() {
    loadBearingStaticKeys()
        .forEach(
            (key, role) ->
                assertThat(UIManager.getColor(key))
                    .as("%s is bridged to %s so raw Swing inherits the design language", key, role)
                    .isEqualTo(role.resolve()));
  }

  @Test
  void bridgedColorsAreUiResourcesSoAReskinCanReplaceThem() {
    for (final String key : loadBearingStaticKeys().keySet()) {
      assertThat(UIManager.get(key))
          .as("%s must be a UIResource or Swing freezes it on the first-installed theme", key)
          .isInstanceOf(ColorUIResource.class);
    }
  }

  @Test
  void globalCornerArcComesFromTheShapeLadder() {
    assertThat(UIManager.get("Component.arc"))
        .as("raw Swing's default corner radius is the small shape step, not a magic number")
        .isEqualTo(ShapeScale.SM.px());
  }

  @Test
  void focusRingIsTheFocusCueSoFocusedFillsMatchRestingOnes() {
    assertThat(UIManager.getInt("Component.focusWidth"))
        .as("a visible focus ring is the whole focus model, so the width is raised off zero")
        .isPositive();
    assertThat(UIManager.getColor("Button.focusedBackground"))
        .as("focus must not swap the fill — the ring alone signals focus")
        .isEqualTo(UIManager.getColor("Button.background"));
    assertThat(UIManager.getColor("Button.focusedBorderColor"))
        .as("focus must not swap the border either")
        .isEqualTo(UIManager.getColor("Button.borderColor"));
  }

  @Test
  void radioIconTakesItsOwnFlatLafStyleSoTheSharedIconPaletteCanSplit() {
    assertThat(UIManager.getString("RadioButton.icon.style"))
        .as("the radio needs the outlined style to get the M3 ring while the checkbox stays filled")
        .isEqualTo("outlined");
    assertThat(UIManager.getColor("CheckBox.icon.focusedSelectedBackground"))
        .as("an unset focusedSelected fill renders a focused, checked icon as empty")
        .isEqualTo(ColorRole.PRIMARY.resolve());
  }

  // ------------------------------------------------------- baked state layers

  @Test
  void hoverKeysCarryTheBakedOverlayRatherThanAFlatRole() {
    final Color base = ColorRole.SURFACE_CONTAINER_LOW.resolve();

    assertThat(UIManager.getColor("Button.hoverBackground"))
        .as("FlatLaf wants a discrete color, so the M3 overlay is blended once at install")
        .isEqualTo(StateLayer.HOVER.over(base, ColorRole.ON_SURFACE))
        .isNotEqualTo(base);
  }

  @Test
  void aKeyTheStateLayerHalfOwnsIsNotAlsoWrittenByTheStaticHalf() {
    final Color surface = ColorRole.SURFACE.resolve();

    assertThat(UIManager.getColor("TabbedPane.hoverColor"))
        .as("applyStaticKeys always runs first, so a static write here would be dead code")
        .isEqualTo(StateLayer.HOVER.over(surface, ColorRole.ON_SURFACE.resolve()))
        .isNotEqualTo(ColorRole.SURFACE_VARIANT.resolve());
  }

  @Test
  void pressedKeysCarryTheStrongerOverlay() {
    final Color base = ColorRole.SURFACE_CONTAINER_LOW.resolve();

    assertThat(UIManager.getColor("Button.pressedBackground"))
        .as("pressed bakes the pressed layer, not the hover one")
        .isEqualTo(StateLayer.PRESSED.over(base, ColorRole.ON_SURFACE));
  }

  @Test
  void emphasisButtonBakesAgainstItsOwnRolePair() {
    assertThat(UIManager.getColor("Button.default.hoverBackground"))
        .as("a primary-filled button tints with its own foreground, not the surface foreground")
        .isEqualTo(StateLayer.HOVER.over(ColorRole.PRIMARY.resolve(), ColorRole.ON_PRIMARY));
  }

  @Test
  void noFocusedBackgroundIsBaked() {
    assertThat(UIManager.getColor("Button.focusedBackground"))
        .as("a baked focused fill persists after a click and reads as a stuck pressed state")
        .isNotEqualTo(UIManager.getColor("Button.pressedBackground"));
  }

  @Test
  void bakedKeysAreAlsoUiResources() {
    assertThat(UIManager.get("Button.hoverBackground"))
        .as("baked colors follow the same re-skin rule as the direct assignments")
        .isInstanceOf(ColorUIResource.class);
  }

  // ------------------------------------------------------------ mode switch

  @Test
  void bridgeFollowsAModeSwitch() {
    ThemeExtension.install(Mode.LIGHT);
    final Color lightPanel = UIManager.getColor("Panel.background");
    final Color lightHover = UIManager.getColor("Button.hoverBackground");

    ThemeExtension.install(Mode.DARK);

    assertThat(UIManager.getColor("Panel.background"))
        .as("re-installing rewrites the bridged keys, so raw Swing re-skins with everything else")
        .isEqualTo(ColorRole.SURFACE.resolve())
        .isNotEqualTo(lightPanel);
    assertThat(UIManager.getColor("Button.hoverBackground"))
        .as("the state layers are re-baked against the new palette, not left stale")
        .isNotEqualTo(lightHover);
  }

  @Test
  void bridgeFollowsAThemeSwitch() {
    final Theme other = MaterialPalettes.secondary().get(0);

    ElwhaTheme.install(ElwhaTheme.config().theme(other).mode(Mode.LIGHT).build());

    assertThat(UIManager.getColor("Component.focusColor"))
        .as("the focus ring picks up the newly installed theme's primary")
        .isEqualTo(other.light().get(ColorRole.PRIMARY));
  }
}
