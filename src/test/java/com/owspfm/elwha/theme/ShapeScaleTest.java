package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the corner-radius token ladder — the locked seven-step table, the key
 * mechanics, and the resolve-through-{@code UIManager} contract that makes the geometry themeable.
 * Oracle: {@code docs/research/elwha-token-taxonomy.md} §3.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShapeScaleTest {

  /** The locked radius table from taxonomy §3. */
  private static final Map<ShapeScale, Integer> LOCKED_PX = new EnumMap<>(ShapeScale.class);

  static {
    LOCKED_PX.put(ShapeScale.NONE, 0);
    LOCKED_PX.put(ShapeScale.XS, 4);
    LOCKED_PX.put(ShapeScale.SM, 8);
    LOCKED_PX.put(ShapeScale.MD, 12);
    LOCKED_PX.put(ShapeScale.LG, 16);
    LOCKED_PX.put(ShapeScale.XL, 28);
    LOCKED_PX.put(ShapeScale.FULL, 9999);
  }

  @Test
  void ladderHasTheSevenLockedSteps() {
    assertThat(ShapeScale.values())
        .as("the shape scale is seven steps, none to full")
        .containsExactly(
            ShapeScale.NONE,
            ShapeScale.XS,
            ShapeScale.SM,
            ShapeScale.MD,
            ShapeScale.LG,
            ShapeScale.XL,
            ShapeScale.FULL);
  }

  @ParameterizedTest
  @EnumSource(ShapeScale.class)
  void everyStepResolvesToItsLockedRadius(final ShapeScale step) {
    assertThat(step.px())
        .as("%s is locked at %d px in the token table", step, LOCKED_PX.get(step))
        .isEqualTo(LOCKED_PX.get(step));
  }

  @Test
  void ladderIsMonotonic() {
    int previous = -1;
    for (final ShapeScale step : ShapeScale.values()) {
      assertThat(step.px())
          .as("%s widens the corner rather than narrowing it", step)
          .isGreaterThan(previous);
      previous = step.px();
    }
  }

  @ParameterizedTest
  @EnumSource(ShapeScale.class)
  void uiKeyIsTheKeyUnderTheShapeNamespace(final ShapeScale step) {
    assertThat(step.uiKey())
        .as("every shape step lives under Elwha.shape.*")
        .isEqualTo("Elwha.shape." + step.key());
  }

  @Test
  void keysAreTheLowercaseStepNames() {
    assertThat(ShapeScale.NONE.key()).as("NONE keys as none").isEqualTo("none");
    assertThat(ShapeScale.MD.key()).as("MD keys as md").isEqualTo("md");
    assertThat(ShapeScale.FULL.key()).as("FULL keys as full").isEqualTo("full");
  }

  @Test
  void radiiResolveThroughUiManagerSoGeometryStaysThemeable() {
    UIManager.put(ShapeScale.MD.uiKey(), 99);

    assertThat(ShapeScale.MD.px())
        .as("a re-written token key changes the resolved radius — geometry is not compiled in")
        .isEqualTo(99);
  }

  @Test
  void anAbsentKeyFallsBackToTheCompiledDefault() {
    UIManager.put(ShapeScale.LG.uiKey(), null);

    assertThat(ShapeScale.LG.px())
        .as("an unthemed step degrades to its locked default rather than collapsing to zero")
        .isEqualTo(16);
  }

  @Test
  void aNonIntegerKeyIsIgnored() {
    UIManager.put(ShapeScale.SM.uiKey(), "not a number");

    assertThat(ShapeScale.SM.px())
        .as("a malformed token value degrades to the locked default rather than throwing")
        .isEqualTo(8);
  }
}
