package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the content-side morph primitives — icon translation, the label cross-fade
 * with its inflection point, and container-width interpolation. Unlike {@link ShapeMorphPainter}
 * these deliberately do <em>not</em> apply an {@link Easing}: the caller feeds an already-eased
 * value so one animator can drive several parallel transitions. Oracle: {@code
 * docs/research/elwha-fab-design.md} §9.1.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ContentMorphPainterTest {

  // ------------------------------------------------------------- icon travel

  @Test
  void iconTravelsLinearlyBetweenItsAnchors() {
    assertThat(ContentMorphPainter.iconX(10, 50, 0f))
        .as("at rest the icon sits at its start anchor")
        .isEqualTo(10);
    assertThat(ContentMorphPainter.iconX(10, 50, 1f))
        .as("landed, it sits at its destination anchor")
        .isEqualTo(50);
    assertThat(ContentMorphPainter.iconX(10, 50, 0.5f))
        .as("half way through, it is half way between them")
        .isEqualTo(30);
  }

  @Test
  void iconPositionRoundsToWholePixels() {
    assertThat(ContentMorphPainter.iconX(0, 5, 0.5f))
        .as("a sub-pixel anchor is rounded rather than truncated")
        .isEqualTo(3);
  }

  @Test
  void iconIsNotClampedBecauseSpringsOvershoot() {
    assertThat(ContentMorphPainter.iconX(0, 100, 1.1f))
        .as("M3 spring motion overshoots on purpose — clamping here would flatten it")
        .isEqualTo(110);
    assertThat(ContentMorphPainter.iconX(0, 100, -0.1f))
        .as("undershoot is equally valid spring behavior")
        .isEqualTo(-10);
  }

  // ---------------------------------------------------------- label crossfade

  @Test
  void defaultInflectionIsTheContainerEmphasizedMidpoint() {
    assertThat(ContentMorphPainter.DEFAULT_LABEL_CROSSFADE_INFLECTION)
        .as("M3's container-emphasized pattern crosses over at the half-way mark")
        .isEqualTo(0.5f);
  }

  @Test
  void labelStaysInvisibleUntilTheInflection() {
    assertThat(ContentMorphPainter.labelAlpha(0f))
        .as("no label while the body is still growing")
        .isZero();
    assertThat(ContentMorphPainter.labelAlpha(0.5f))
        .as("the label first becomes visible exactly at the inflection")
        .isZero();
    assertThat(ContentMorphPainter.labelAlpha(0.75f))
        .as("past the inflection it ramps in")
        .isEqualTo(0.5f, within(0.0001f));
    assertThat(ContentMorphPainter.labelAlpha(1f))
        .as("it is fully opaque when the morph lands")
        .isEqualTo(1f);
  }

  @Test
  void defaultOverloadMatchesTheExplicitOne() {
    assertThat(ContentMorphPainter.labelAlpha(0.8f))
        .as("the no-inflection form is the same curve at the documented default")
        .isEqualTo(
            ContentMorphPainter.labelAlpha(
                0.8f, ContentMorphPainter.DEFAULT_LABEL_CROSSFADE_INFLECTION));
  }

  @Test
  void aCustomInflectionMovesTheCrossover() {
    assertThat(ContentMorphPainter.labelAlpha(0.5f, 0.25f))
        .as("an earlier crossover has the label already partly in at the half-way mark")
        .isPositive();
    assertThat(ContentMorphPainter.labelAlpha(0.5f, 0.75f))
        .as("a later crossover has it still fully hidden there")
        .isZero();
  }

  @Test
  void alphaIsClampedToTheUnitInterval() {
    assertThat(ContentMorphPainter.labelAlpha(2f))
        .as("an overshooting phase does not over-paint")
        .isEqualTo(1f);
    assertThat(ContentMorphPainter.labelAlpha(-1f))
        .as("an undershooting phase does not go negative")
        .isZero();
  }

  @Test
  void anInflectionOutsideTheOpenUnitIntervalIsRejected() {
    assertThatThrownBy(() -> ContentMorphPainter.labelAlpha(0.5f, 0f))
        .as("an inflection at zero would divide the ramp by its whole range")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ContentMorphPainter.labelAlpha(0.5f, 1f))
        .as("an inflection at one leaves no room for the label to fade in")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ContentMorphPainter.labelAlpha(0.5f, -0.2f))
        .as("a negative inflection is not a curve")
        .isInstanceOf(IllegalArgumentException.class);
  }

  // -------------------------------------------------------- container width

  @Test
  void containerWidthShortCircuitsAtTheEndpoints() {
    assertThat(ContentMorphPainter.containerWidth(56, 140, 0f))
        .as("a steady-state component reports exactly its resting width, not a rounded estimate")
        .isEqualTo(56);
    assertThat(ContentMorphPainter.containerWidth(56, 140, 1f))
        .as("and exactly its extended width once landed")
        .isEqualTo(140);
  }

  @Test
  void containerWidthInterpolatesInBetween() {
    assertThat(ContentMorphPainter.containerWidth(56, 140, 0.5f))
        .as("half way through the morph the container is half way between its two widths")
        .isEqualTo(98);
  }

  @Test
  void containerWidthTreatsOvershootAsAnEndpoint() {
    assertThat(ContentMorphPainter.containerWidth(56, 140, 1.2f))
        .as("layout width is not allowed to overshoot the way a painted position may")
        .isEqualTo(140);
    assertThat(ContentMorphPainter.containerWidth(56, 140, -0.2f))
        .as("nor undershoot below the resting width")
        .isEqualTo(56);
  }

  @Test
  void aCollapsingMorphInterpolatesJustAsWell() {
    assertThat(ContentMorphPainter.containerWidth(140, 56, 0.5f))
        .as("the reverse direction is the same interpolation with swapped endpoints")
        .isEqualTo(98);
  }
}
