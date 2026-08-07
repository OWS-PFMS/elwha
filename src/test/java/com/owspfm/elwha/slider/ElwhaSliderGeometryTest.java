package com.owspfm.elwha.slider;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.slider.ElwhaSlider.Orientation;
import com.owspfm.elwha.slider.ElwhaSlider.Size;
import com.owspfm.elwha.slider.ElwhaSlider.Variant;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A measurement coverage for {@link ElwhaSlider} — the M3 §M size table (track thickness,
 * handle height, and the constants that deliberately do <em>not</em> scale), the reserved value-
 * bubble band, and the vertical transposition: the same paint code runs inside a &minus;90&deg;
 * rotated frame, so the whole geometry should come back transposed rather than re-derived.
 *
 * <p>Track thickness is read from the recorded paint calls, because it is not exposed any other
 * way; the recorder applies the current transform, so a vertical slider's track is reported where
 * it actually lands on the device — which is exactly what makes the transposition assertable.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSliderGeometryTest {

  /** The §M track thickness per size preset — the doc's measurement table, transcribed. */
  private static final Map<Size, Integer> TRACK_HEIGHT =
      Map.of(Size.XS, 16, Size.S, 24, Size.M, 40, Size.L, 56, Size.XL, 96);

  /** The §M resting handle height per size preset. */
  private static final Map<Size, Integer> HANDLE_HEIGHT =
      Map.of(Size.XS, 44, Size.S, 44, Size.M, 52, Size.L, 68, Size.XL, 108);

  private static ElwhaSlider sized(final ElwhaSlider slider) {
    slider.setSize(slider.getPreferredSize());
    return slider;
  }

  /** The painted track — the widest filled shape the slider draws along its long axis. */
  private static Rectangle2D track(final ElwhaSlider slider) {
    final Dimension pref = slider.getPreferredSize();
    return PaintLog.capture(slider, pref.width, pref.height).shapes().stream()
        .filter(s -> !s.stroked())
        .map(PaintLog.Painted::bounds)
        .max(java.util.Comparator.comparingDouble(b -> b.getWidth() * b.getHeight()))
        .orElseThrow();
  }

  // -------------------------------------------------------- the size table

  @ParameterizedTest
  @EnumSource(Size.class)
  void eachSizePresetPaintsItsDocumentedTrackThickness(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSizeVariant(size);

    assertThat(track(slider).getHeight())
        .as("%s tracks are %ddp thick per the M3 measurement table", size, TRACK_HEIGHT.get(size))
        .isEqualTo(TRACK_HEIGHT.get(size).doubleValue());
  }

  @ParameterizedTest
  @EnumSource(Size.class)
  void eachSizePresetReservesItsDocumentedHandleHeight(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSizeVariant(size);

    assertThat(slider.getMinimumSize().width)
        .as("the minimum long extent is one handle wide, so it reports the handle height")
        .isEqualTo(HANDLE_HEIGHT.get(size));
  }

  @ParameterizedTest
  @EnumSource(Size.class)
  void aSliderWithNoValueIndicatorIsExactlyItsHandleBandTall(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSizeVariant(size);

    assertThat(slider.isValueIndicatorEnabled())
        .as("the value bubble is opt-in, so it costs no height by default")
        .isFalse();
    assertThat(slider.getPreferredSize().height)
        .as("%s is one handle band tall", size)
        .isEqualTo(HANDLE_HEIGHT.get(size));
  }

  @ParameterizedTest
  @EnumSource(Size.class)
  void enablingTheValueIndicatorReservesItsBandAboveTheHandle(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSizeVariant(size);

    slider.setValueIndicatorEnabled(true);

    assertThat(slider.getPreferredSize().height)
        .as("%s stacks the bubble's reserved band above the handle band", size)
        .isEqualTo(
            HANDLE_HEIGHT.get(size)
                + ElwhaSlider.VALUE_BUBBLE_HEIGHT_PX
                + ElwhaSlider.VALUE_BUBBLE_GAP_PX);
  }

  @Test
  void reservedBandIsHeldWhetherOrNotTheBubbleIsShowing() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setValueIndicatorEnabled(true);
    final int resting = slider.getPreferredSize().height;

    slider.setPressed(true);

    assertThat(slider.getPreferredSize().height)
        .as("the bubble appears into space already reserved, so pressing shifts no layout")
        .isEqualTo(resting);
  }

  @Test
  void handleWidthAndTrackGapDoNotScaleWithTheSizePreset() {
    final ElwhaSlider extraSmall = new ElwhaSlider(0, 100, 0);
    final ElwhaSlider extraLarge = new ElwhaSlider(0, 100, 0);
    extraLarge.setSizeVariant(Size.XL);
    extraSmall.setSize(240, extraSmall.getPreferredSize().height);
    extraLarge.setSize(240, extraLarge.getPreferredSize().height);

    assertThat(extraLarge.handleCenterX())
        .as("travel is inset by the handle half-width, which M3 holds constant across sizes")
        .isEqualTo(extraSmall.handleCenterX());
  }

  @Test
  void preferredLongExtentIsTheDefaultTrackLength() {
    assertThat(new ElwhaSlider(0, 100, 50).getPreferredSize().width)
        .as("a slider asks for the default track length when the layout offers it a choice")
        .isEqualTo(ElwhaSlider.DEFAULT_TRACK_LENGTH_PX);
  }

  @Test
  void maximumSizeStretchesAlongTheTrackButNeverAcrossIt() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);

    assertThat(slider.getMaximumSize().width)
        .as("a slider is happy to be as long as the layout likes")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(slider.getMaximumSize().height)
        .as("but its cross axis is fixed by the size preset")
        .isEqualTo(slider.getPreferredSize().height);
  }

  // ------------------------------------------------------- transposition

  @ParameterizedTest
  @EnumSource(Size.class)
  void goingVerticalTransposesThePreferredSize(final Size size) {
    final ElwhaSlider horizontal = new ElwhaSlider(0, 100, 50);
    horizontal.setSizeVariant(size);
    final ElwhaSlider vertical = new ElwhaSlider(0, 100, 50);
    vertical.setSizeVariant(size);

    vertical.setOrientation(Orientation.VERTICAL);

    assertThat(vertical.getPreferredSize())
        .as("%s: the long axis becomes the height, the cross axis the width", size)
        .isEqualTo(
            new Dimension(
                horizontal.getPreferredSize().height, horizontal.getPreferredSize().width));
  }

  @Test
  void minimumAndMaximumSizesTransposeToo() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    final Dimension horizontalMin = slider.getMinimumSize();

    slider.setOrientation(Orientation.VERTICAL);

    assertThat(slider.getMinimumSize())
        .as("the minimum swaps with the orientation, not just the preferred size")
        .isEqualTo(new Dimension(horizontalMin.height, horizontalMin.width));
    assertThat(slider.getMaximumSize().height)
        .as("and a vertical slider stretches down the page instead of across it")
        .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void aVerticalTrackPaintsTallAndNarrowOnTheDevice() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setOrientation(Orientation.VERTICAL);

    final Rectangle2D bar = track(slider);

    assertThat(bar.getWidth())
        .as("the rotated paint frame turns the track's thickness into a device width")
        .isEqualTo((double) ElwhaSlider.TRACK_HEIGHT_PX);
    assertThat(bar.getHeight())
        .as("and its length into a device height — the same paint code, transposed")
        .isGreaterThan(bar.getWidth());
  }

  @ParameterizedTest
  @EnumSource(Size.class)
  void aVerticalTracksThicknessStillFollowsTheSizeTable(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSizeVariant(size);
    slider.setOrientation(Orientation.VERTICAL);

    assertThat(track(slider).getWidth())
        .as("%s: the size table is orientation-agnostic", size)
        .isEqualTo(TRACK_HEIGHT.get(size).doubleValue());
  }

  // ----------------------------------------------------------- inset icon

  @ParameterizedTest
  @EnumSource(
      value = Size.class,
      names = {"M", "L", "XL"})
  void biggerSizesAcceptAnInsetIcon(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 80);
    slider.setSizeVariant(size);

    slider.setInsetIcon(MaterialIcons.star(24));

    assertThat(slider.getInsetIcon())
        .as("%s has a track thick enough for a legible glyph", size)
        .isNotNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = Size.class,
      names = {"XS", "S"})
  void thinSizesKeepTheIconButPaintNothing(final Size size) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 80);
    slider.setSizeVariant(size);
    slider.setInsetIcon(MaterialIcons.star(24));

    assertThat(slider.getInsetIcon())
        .as("the property round-trips — the no-op is in the paint, not the setter")
        .isNotNull();
    assertThat(sized(slider).getPreferredSize())
        .as("and a documented no-op must not change the layout either")
        .isEqualTo(new ElwhaSlider(0, 100, 80).getPreferredSize());
  }

  @Test
  void clearingTheInsetIconIsAccepted() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 80);
    slider.setSizeVariant(Size.L);
    slider.setInsetIcon(MaterialIcons.star(24));

    slider.setInsetIcon(null);

    assertThat(slider.getInsetIcon()).as("null clears the slot").isNull();
  }

  // ---------------------------------------------------------- the origin

  @Test
  void defaultOriginIsZeroWhenZeroLiesInTheRange() {
    assertThat(new ElwhaSlider(-50, 50, 0).getOrigin())
        .as("a signed range grows its centered fill out of zero")
        .isZero();
  }

  @Test
  void defaultOriginIsTheMidpointWhenZeroIsOutOfRange() {
    assertThat(new ElwhaSlider(20, 80, 40).getOrigin())
        .as("with no zero to anchor on, the midpoint is the sensible fill origin")
        .isEqualTo(50);
  }

  @Test
  void anExplicitOriginIsClampedIntoTheRange() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);

    slider.setOrigin(500);

    assertThat(slider.getOrigin())
        .as("an out-of-range origin has nowhere to paint from")
        .isEqualTo(100);
  }

  @Test
  void variantAndSizeRoundTrip() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);

    assertThat(slider.getVariant()).as("STANDARD is the default").isEqualTo(Variant.STANDARD);
    assertThat(slider.getSizeVariant()).as("and XS the default size").isEqualTo(Size.XS);

    slider.setVariant(Variant.CENTERED);
    slider.setSizeVariant(Size.L);

    assertThat(slider.getVariant()).as("the variant round-trips").isEqualTo(Variant.CENTERED);
    assertThat(slider.getSizeVariant()).as("as does the size").isEqualTo(Size.L);
  }
}
