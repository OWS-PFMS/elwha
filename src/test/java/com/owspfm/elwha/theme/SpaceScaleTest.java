package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the spacing token ladder — the locked six-step 4 px-grid table, the {@link
 * Insets} conveniences, and the resolve-through-{@code UIManager} contract. Oracle: {@code
 * docs/research/elwha-token-taxonomy.md} §4.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class SpaceScaleTest {

  /** The locked spacing table from taxonomy §4. */
  private static final Map<SpaceScale, Integer> LOCKED_PX = new EnumMap<>(SpaceScale.class);

  static {
    LOCKED_PX.put(SpaceScale.XS, 4);
    LOCKED_PX.put(SpaceScale.SM, 8);
    LOCKED_PX.put(SpaceScale.MD, 12);
    LOCKED_PX.put(SpaceScale.LG, 16);
    LOCKED_PX.put(SpaceScale.XL, 24);
    LOCKED_PX.put(SpaceScale.XXL, 32);
  }

  @Test
  void theLadderHasTheSixLockedStepsAndStopsAtDoubleExtraLarge() {
    assertThat(SpaceScale.values())
        .as("the scale stops at XXL — Q2 locked no XXXL")
        .containsExactly(
            SpaceScale.XS,
            SpaceScale.SM,
            SpaceScale.MD,
            SpaceScale.LG,
            SpaceScale.XL,
            SpaceScale.XXL);
  }

  @ParameterizedTest
  @EnumSource(SpaceScale.class)
  void everyStepResolvesToItsLockedLength(final SpaceScale step) {
    assertThat(step.px())
        .as("%s is locked at %d px in the token table", step, LOCKED_PX.get(step))
        .isEqualTo(LOCKED_PX.get(step));
  }

  @ParameterizedTest
  @EnumSource(SpaceScale.class)
  void everyStepSitsOnTheFourPixelGrid(final SpaceScale step) {
    assertThat(step.px() % 4).as("%s is a multiple of the 4 px base unit", step).isZero();
  }

  @ParameterizedTest
  @EnumSource(SpaceScale.class)
  void uiKeyIsTheKeyUnderTheSpaceNamespace(final SpaceScale step) {
    assertThat(step.uiKey())
        .as("every spacing step lives under Elwha.space.*")
        .isEqualTo("Elwha.space." + step.key());
  }

  @Test
  void insetsAreUniformAcrossAllFourEdges() {
    assertThat(SpaceScale.LG.insets())
        .as("a single step pads every edge by its resolved length")
        .isEqualTo(new Insets(16, 16, 16, 16));
  }

  @Test
  void symmetricInsetsTakeVerticalThenHorizontal() {
    assertThat(SpaceScale.insets(SpaceScale.SM, SpaceScale.XL))
        .as("the vertical step pads top and bottom, the horizontal step left and right")
        .isEqualTo(new Insets(8, 24, 8, 24));
  }

  @Test
  void insetsAreFreshPerCallSoCallersCannotShareState() {
    final Insets first = SpaceScale.MD.insets();
    first.top = 99;

    assertThat(SpaceScale.MD.insets().top)
        .as("mutating one caller's insets does not reach the next caller")
        .isEqualTo(12);
  }

  @Test
  void lengthsResolveThroughUiManagerSoSpacingStaysThemeable() {
    UIManager.put(SpaceScale.MD.uiKey(), 40);

    assertThat(SpaceScale.MD.px())
        .as("a re-written token key changes the resolved length — spacing is not compiled in")
        .isEqualTo(40);
    assertThat(SpaceScale.MD.insets())
        .as("the insets convenience reads through the same resolution")
        .isEqualTo(new Insets(40, 40, 40, 40));
  }

  @Test
  void anAbsentKeyFallsBackToTheCompiledDefault() {
    UIManager.put(SpaceScale.XXL.uiKey(), null);

    assertThat(SpaceScale.XXL.px())
        .as("an unthemed step degrades to its locked default rather than collapsing to zero")
        .isEqualTo(32);
  }
}
