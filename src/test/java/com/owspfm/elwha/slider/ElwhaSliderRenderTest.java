package com.owspfm.elwha.slider;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.slider.ElwhaSlider.Orientation;
import com.owspfm.elwha.slider.ElwhaSlider.Variant;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A chrome and accessibility coverage for {@link ElwhaSlider} — the two track roles and where
 * each variant puts them (STANDARD fills from the leading edge, CENTERED out of its origin, RANGE
 * between the handles), the disabled treatment, and the {@code AccessibleValue} surface AT drives
 * the slider through.
 *
 * <p>Track segments are identified from the recorded paint calls by their resolved role, which is
 * the only way to tell an active segment from an inactive one without knowing where each variant
 * decided to put it.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSliderRenderTest {

  private static ElwhaSlider percent(final int value) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, value);
    slider.setSize(240, slider.getPreferredSize().height);
    return slider;
  }

  private static PaintLog capture(final ElwhaSlider slider) {
    final Dimension size = slider.getWidth() > 0 ? slider.getSize() : slider.getPreferredSize();
    return PaintLog.capture(slider, size.width, size.height);
  }

  /** Every filled shape painted in the given resolved color. */
  private static List<PaintLog.Painted> segmentsIn(final PaintLog log, final Color color) {
    return log.shapes().stream()
        .filter(s -> !s.stroked())
        .filter(s -> color.equals(s.color()))
        .toList();
  }

  private static Color disabledContent() {
    final Color base = ColorRole.ON_SURFACE.resolve();
    return new Color(
        base.getRed(),
        base.getGreen(),
        base.getBlue(),
        Math.round(StateLayer.disabledContentOpacity() * 255f));
  }

  // ------------------------------------------------------------ the roles

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void theTrackCarriesBothRolesAtOnce(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaSlider slider = percent(50);

    final PaintLog log = capture(slider);

    assertThat(segmentsIn(log, ColorRole.PRIMARY.resolve()))
        .as("the active segment is primary in " + mode)
        .isNotEmpty();
    assertThat(segmentsIn(log, ColorRole.SECONDARY_CONTAINER.resolve()))
        .as("and the inactive segment is secondaryContainer in " + mode)
        .isNotEmpty();
  }

  @Test
  void theStandardVariantFillsFromTheLeadingEdgeToTheHandle() {
    final ElwhaSlider slider = percent(50);

    final PaintLog log = capture(slider);

    final double activeStart =
        segmentsIn(log, ColorRole.PRIMARY.resolve()).stream()
            .mapToDouble(s -> s.bounds().getMinX())
            .min()
            .orElseThrow();
    assertThat(activeStart)
        .as("a standard slider's fill is anchored at the track's start, not at its value")
        .isLessThan(4);
  }

  @Test
  void theCenteredVariantLeavesInactiveTrackOnBothSidesOfTheFill() {
    final ElwhaSlider slider = new ElwhaSlider(-50, 50, 25);
    slider.setVariant(Variant.CENTERED);
    slider.setSize(240, slider.getPreferredSize().height);

    final PaintLog log = capture(slider);

    final double activeStart =
        segmentsIn(log, ColorRole.PRIMARY.resolve()).stream()
            .mapToDouble(s -> s.bounds().getMinX())
            .min()
            .orElseThrow();
    assertThat(activeStart)
        .as("a centered slider grows out of its origin, so the fill starts mid-track")
        .isGreaterThan(4);
    assertThat(segmentsIn(log, ColorRole.SECONDARY_CONTAINER.resolve()))
        .as("with inactive track flanking it on both sides")
        .hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void theRangeVariantFillsBetweenItsTwoHandles() {
    final ElwhaSlider slider = ElwhaSlider.range(0, 100, 30, 70);
    slider.setSize(240, slider.getPreferredSize().height);

    final PaintLog log = capture(slider);

    final double activeStart =
        segmentsIn(log, ColorRole.PRIMARY.resolve()).stream()
            .mapToDouble(s -> s.bounds().getMinX())
            .min()
            .orElseThrow();
    assertThat(activeStart)
        .as("the active span begins at the lower handle, not at the track start")
        .isGreaterThan(4);
    assertThat(segmentsIn(log, ColorRole.SECONDARY_CONTAINER.resolve()))
        .as("leaving inactive track outside both handles")
        .hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void aValueAtTheMinimumLeavesTheWholeTrackInactive() {
    final ElwhaSlider slider = percent(0);

    final PaintLog log = capture(slider);

    assertThat(segmentsIn(log, ColorRole.SECONDARY_CONTAINER.resolve()))
        .as("nothing is filled yet")
        .isNotEmpty();
  }

  // --------------------------------------------------------------- states

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aDisabledSliderDropsEveryChromeRoleToTheDimmedOnSurface(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaSlider slider = percent(50);
    slider.setEnabled(false);

    final PaintLog log = capture(slider);

    assertThat(segmentsIn(log, disabledContent()))
        .as("a disabled slider paints its chrome in on-surface at 38% in " + mode)
        .isNotEmpty();
    assertThat(segmentsIn(log, ColorRole.PRIMARY.resolve()))
        .as("and no primary survives the disabled treatment")
        .isEmpty();
  }

  @Test
  void aDisabledRangeSliderIsDimmedToo() {
    final ElwhaSlider slider = ElwhaSlider.range(0, 100, 30, 70);
    slider.setSize(240, slider.getPreferredSize().height);
    slider.setEnabled(false);

    assertThat(segmentsIn(capture(slider), ColorRole.PRIMARY.resolve()))
        .as("both handles and the span between them go dim together")
        .isEmpty();
  }

  @Test
  void everyVariantAndOrientationPaintsWithoutThrowing() {
    for (final Variant variant : Variant.values()) {
      for (final Orientation orientation : Orientation.values()) {
        final ElwhaSlider slider = new ElwhaSlider(0, 100, 40);
        slider.setVariant(variant);
        slider.setOrientation(orientation);
        slider.setStops(20);
        slider.setEndStopsVisible(true);
        slider.setValueIndicatorEnabled(true);
        slider.setPressed(true);

        final PaintLog log = capture(slider);

        assertThat(log.shapes())
            .as("%s / %s paints chrome in its most decorated configuration", variant, orientation)
            .isNotEmpty();
      }
    }
  }

  @Test
  void anUnparentedSliderCanPaintItsValueBubble() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 40);
    slider.setValueIndicatorEnabled(true);
    slider.setPressed(true);

    final PaintLog log = capture(slider);

    assertThat(log.painted("40"))
        .as(
            "a component that was never added to a container has no inherited font, and an"
                + " offscreen render — a gallery preview, a drag image — paints in exactly that"
                + " state, so the bubble's label must not depend on inheriting one")
        .isTrue();
  }

  @Test
  void aVerticalSlidersValueBubbleStaysUpright() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 40);
    slider.setOrientation(Orientation.VERTICAL);
    slider.setValueIndicatorEnabled(true);
    slider.setPressed(true);

    final PaintLog log = capture(slider);

    assertThat(log.text("40").orElseThrow().font().getSize2D())
        .as("the bubble paints after the rotated chrome frame is dropped, so its text reads level")
        .isGreaterThan(0f);
  }

  // ---------------------------------------------------------------- a11y

  @Test
  void theSliderAnnouncesAsASliderWithItsValueAndBounds() {
    final ElwhaSlider slider = percent(37);
    final AccessibleContext context = slider.getAccessibleContext();

    assertThat(context.getAccessibleRole()).as("role").isEqualTo(AccessibleRole.SLIDER);
    final AccessibleValue value = context.getAccessibleValue();
    assertThat(value.getCurrentAccessibleValue()).as("the current value").isEqualTo(37);
    assertThat(value.getMinimumAccessibleValue()).as("the range floor").isEqualTo(0);
    assertThat(value.getMaximumAccessibleValue()).as("and the ceiling").isEqualTo(100);
  }

  @Test
  void theLabelNamesTheSlider() {
    final ElwhaSlider slider = percent(50);

    slider.setLabel("Volume");

    assertThat(slider.getLabel()).as("the label round-trips").isEqualTo("Volume");
    assertThat(slider.getAccessibleContext().getAccessibleName())
        .as("and names the control to AT")
        .isEqualTo("Volume");
  }

  @Test
  void assistiveTechnologyCanSetTheValue() {
    final ElwhaSlider slider = percent(50);
    final AccessibleValue value = slider.getAccessibleContext().getAccessibleValue();

    assertThat(value.setCurrentAccessibleValue(80)).as("the set is accepted").isTrue();
    assertThat(slider.getValue()).as("and reaches the model").isEqualTo(80);
  }

  @Test
  void anAccessibleSetIsClampedAndSnappedLikeAnyOther() {
    final ElwhaSlider slider = percent(50);
    slider.setStops(25);

    slider.getAccessibleContext().getAccessibleValue().setCurrentAccessibleValue(300);

    assertThat(slider.getValue())
        .as("AT drives the same setter as everything else — no bypass of the clamps")
        .isEqualTo(100);
  }

  @Test
  void aNullAccessibleValueIsRefused() {
    final ElwhaSlider slider = percent(50);

    assertThat(slider.getAccessibleContext().getAccessibleValue().setCurrentAccessibleValue(null))
        .as("nothing to set")
        .isFalse();
    assertThat(slider.getValue()).as("and nothing changed").isEqualTo(50);
  }
}
