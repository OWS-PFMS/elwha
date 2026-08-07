package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the M3 state-layer model — the locked five-layer opacity table, the sRGB
 * compositing {@code over} performs, and the two disabled-opacity constants that deliberately sit
 * outside the enum. Oracle: {@code docs/research/elwha-token-taxonomy.md} §5.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class StateLayerTest {

  /** The locked opacity table from taxonomy §5. */
  private static final Map<StateLayer, Float> LOCKED_OPACITY = new EnumMap<>(StateLayer.class);

  static {
    LOCKED_OPACITY.put(StateLayer.HOVER, 0.08f);
    LOCKED_OPACITY.put(StateLayer.FOCUS, 0.10f);
    LOCKED_OPACITY.put(StateLayer.PRESSED, 0.10f);
    LOCKED_OPACITY.put(StateLayer.DRAGGED, 0.16f);
    LOCKED_OPACITY.put(StateLayer.SELECTED, 0.12f);
  }

  @Test
  void theModelHasTheFiveLockedLayers() {
    assertThat(StateLayer.values())
        .as("hover, focus, pressed, dragged, and the Elwha-invented selected layer")
        .containsExactly(
            StateLayer.HOVER,
            StateLayer.FOCUS,
            StateLayer.PRESSED,
            StateLayer.DRAGGED,
            StateLayer.SELECTED);
  }

  @ParameterizedTest
  @EnumSource(StateLayer.class)
  void everyLayerResolvesToItsLockedOpacity(final StateLayer layer) {
    assertThat(layer.opacity())
        .as("%s is locked at %s in the token table", layer, LOCKED_OPACITY.get(layer))
        .isEqualTo(LOCKED_OPACITY.get(layer), within(0.0001f));
  }

  @ParameterizedTest
  @EnumSource(StateLayer.class)
  void uiKeyIsTheKeyUnderTheStateNamespace(final StateLayer layer) {
    assertThat(layer.uiKey())
        .as("every state layer lives under Elwha.state.*")
        .isEqualTo("Elwha.state." + layer.key());
  }

  @Test
  void opacitiesResolveThroughUiManagerSoStatesStayThemeable() {
    UIManager.put(StateLayer.HOVER.uiKey(), 0.5f);

    assertThat(StateLayer.HOVER.opacity())
        .as("a re-written token key changes the resolved opacity")
        .isEqualTo(0.5f, within(0.0001f));
  }

  @Test
  void anAbsentKeyFallsBackToTheCompiledDefault() {
    UIManager.put(StateLayer.DRAGGED.uiKey(), null);

    assertThat(StateLayer.DRAGGED.opacity())
        .as("an unthemed layer degrades to its locked default rather than going fully transparent")
        .isEqualTo(0.16f, within(0.0001f));
  }

  // ------------------------------------------------------------- compositing

  @Test
  void overBlendsTheTintIntoTheBaseAtTheLayerOpacity() {
    final Color base = new Color(0x00, 0x00, 0x00);
    final Color tint = new Color(0xFF, 0xFF, 0xFF);

    assertThat(StateLayer.SELECTED.over(base, tint))
        .as("a 12 percent white layer over black lands at the straight sRGB interpolation")
        .isEqualTo(Pixels.mix(base, tint, 0.12f));
  }

  @Test
  void overResolvesTheTintRoleAtCallTime() {
    final Color base = ColorRole.SURFACE.resolve();

    assertThat(StateLayer.HOVER.over(base, ColorRole.ON_SURFACE))
        .as("the role overload resolves the tint through the installed palette")
        .isEqualTo(StateLayer.HOVER.over(base, ColorRole.ON_SURFACE.resolve()));
  }

  @Test
  void overFollowsAModeSwitch() {
    final Color base = new Color(0x80, 0x80, 0x80);
    ThemeExtension.install(Mode.LIGHT);
    final Color light = StateLayer.HOVER.over(base, ColorRole.ON_SURFACE);

    ThemeExtension.install(Mode.DARK);

    assertThat(StateLayer.HOVER.over(base, ColorRole.ON_SURFACE))
        .as("the blend re-resolves its tint on a mode switch rather than caching it")
        .isNotEqualTo(light);
  }

  @Test
  void theBlendResultIsFullyOpaque() {
    final Color blended =
        StateLayer.PRESSED.over(new Color(0x12, 0x34, 0x56), new Color(0xAB, 0xCD, 0xEF));

    assertThat(blended.getAlpha())
        .as("the overlay opacity governs the mix, not the result's alpha")
        .isEqualTo(255);
  }

  @Test
  void aZeroOpacityLayerLeavesTheBaseUntouched() {
    UIManager.put(StateLayer.FOCUS.uiKey(), 0f);
    final Color base = new Color(0x33, 0x66, 0x99);

    assertThat(StateLayer.FOCUS.over(base, Color.WHITE))
        .as("a fully transparent layer is a no-op over its base")
        .isEqualTo(base);
  }

  // --------------------------------------------------------- disabled pair

  @Test
  void disabledOpacitiesAreTheLockedTreatmentConstants() {
    assertThat(StateLayer.disabledContentOpacity())
        .as("M3 drops disabled content to 38 percent")
        .isEqualTo(0.38f, within(0.0001f));
    assertThat(StateLayer.disabledContainerOpacity())
        .as("M3 drops a disabled container fill to 12 percent")
        .isEqualTo(0.12f, within(0.0001f));
  }

  @Test
  void disabledConstantsLiveOutsideTheLayerEnum() {
    assertThat(StateLayer.DISABLED_CONTENT_KEY)
        .as("disabled is an opacity treatment, keyed alongside the layers but not one of them")
        .isEqualTo("Elwha.state.disabledContent");
    assertThat(StateLayer.DISABLED_CONTAINER_KEY)
        .as("the container treatment keys beside it")
        .isEqualTo("Elwha.state.disabledContainer");
  }

  @Test
  void anAbsentDisabledKeyFallsBackToTheCompiledDefault() {
    UIManager.put(StateLayer.DISABLED_CONTENT_KEY, null);

    assertThat(StateLayer.disabledContentOpacity())
        .as("an unthemed disabled treatment degrades to its locked default")
        .isEqualTo(0.38f, within(0.0001f));
  }
}
