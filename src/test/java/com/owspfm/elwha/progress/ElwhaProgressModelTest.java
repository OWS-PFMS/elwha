package com.owspfm.elwha.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A coverage of the contract both indicators inherit from {@link
 * AbstractElwhaProgressIndicator} — design doc §2 (the {@code BoundedRangeModel} value model and
 * the mode switch), §5 (the geometry knobs and their clamps), §6 (the wave axes and the
 * animation-demand gate).
 *
 * <p>Everything here is variant-agnostic, so the cases run against both concrete indicators; the
 * per-variant painting lives in the four geometry and choreography classes.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaProgressModelTest {

  static Stream<org.junit.jupiter.params.provider.Arguments> indicators() {
    return Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(
            "linear", (Supplier<AbstractElwhaProgressIndicator>) ElwhaLinearProgressIndicator::new),
        org.junit.jupiter.params.provider.Arguments.of(
            "circular",
            (Supplier<AbstractElwhaProgressIndicator>) ElwhaCircularProgressIndicator::new));
  }

  // ------------------------------------------------------------ value model

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void defaultModelIsZeroToOneHundredAtZero(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    assertThat(indicator.getMinimum()).as("§2 — the JProgressBar-shaped default range").isZero();
    assertThat(indicator.getMaximum()).isEqualTo(100);
    assertThat(indicator.getValue()).isZero();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void valueAccessorsDelegateToTheModel(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setValue(42);

    assertThat(indicator.getValue()).isEqualTo(42);
    assertThat(indicator.getModel().getValue())
        .as("§2 — there is one source of truth, and it is the model")
        .isEqualTo(42);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void modelClampsOutOfRangeValues(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setValue(500);
    assertThat(indicator.getValue()).as("BoundedRangeModel semantics, unmodified").isEqualTo(100);

    indicator.setValue(-500);
    assertThat(indicator.getValue()).isZero();
  }

  @Test
  void aSuppliedModelIsSharedRatherThanCopied() {
    final BoundedRangeModel shared = new DefaultBoundedRangeModel(10, 0, 0, 100);
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(shared);
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator(shared);

    shared.setValue(75);

    assertThat(bar.getValue())
        .as("§2 — one model can drive several readouts; that is the point of taking one")
        .isEqualTo(75);
    assertThat(ring.getValue()).isEqualTo(75);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void rangeSettersMoveTheModelBounds(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setMinimum(20);
    indicator.setMaximum(60);

    assertThat(indicator.getMinimum()).isEqualTo(20);
    assertThat(indicator.getMaximum()).isEqualTo(60);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void changeListenersAreRegisteredOnTheModel(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    final AtomicInteger fired = new AtomicInteger();
    final ChangeListener listener = e -> fired.incrementAndGet();

    indicator.addChangeListener(listener);
    indicator.setValue(30);
    assertThat(fired.get()).as("a consumer can observe progress without holding the model").isOne();

    indicator.removeChangeListener(listener);
    indicator.setValue(60);
    assertThat(fired.get()).as("and can stop observing").isOne();
  }

  // -------------------------------------------------------- progress fraction

  @ParameterizedTest
  @CsvSource({"0, 0, 100, 0.0", "50, 0, 100, 0.5", "100, 0, 100, 1.0", "30, 20, 60, 0.25"})
  void fractionMapsTheValueOntoItsRange(
      final int value, final int min, final int max, final float fraction) {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(min, max, value);

    assertThat(bar.getProgressFraction())
        .as("§2 — the fraction is what the geometry is driven from")
        .isCloseTo(fraction, within(0.001f));
  }

  @Test
  void fractionHonorsTheModelExtent() {
    final BoundedRangeModel withExtent = new DefaultBoundedRangeModel(40, 20, 0, 100);
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(withExtent);

    assertThat(bar.getProgressFraction())
        .as("value can only reach max − extent, so the run is shortened to match")
        .isCloseTo(0.5f, within(0.001f));
  }

  @Test
  void aDegenerateRangeReadsAsZeroRatherThanDividingByZero() {
    final ElwhaLinearProgressIndicator bar =
        new ElwhaLinearProgressIndicator(new DefaultBoundedRangeModel(5, 0, 5, 5));

    assertThat(bar.getProgressFraction())
        .as("a zero-width range has no fraction to report; it must not blow up")
        .isZero();
  }

  // -------------------------------------------------------------- mode axis

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void indicatorsStartDeterminate(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    assertThat(factory.get().isIndeterminate())
        .as("§2 — determinate is the default; indeterminate is opted into")
        .isFalse();
  }

  @Test
  void indeterminateFactoriesPreSetTheMode() {
    assertThat(ElwhaLinearProgressIndicator.indeterminate().isIndeterminate()).isTrue();
    assertThat(ElwhaCircularProgressIndicator.indeterminate().isIndeterminate()).isTrue();
  }

  @Test
  void wavyFactoriesPreSetTheShape() {
    assertThat(ElwhaLinearProgressIndicator.wavy().isWavy()).isTrue();
    assertThat(ElwhaCircularProgressIndicator.wavy().isWavy()).isTrue();
  }

  @Test
  void wavyIndeterminateFactoriesSetBoth() {
    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.wavyIndeterminate();
    final ElwhaCircularProgressIndicator ring = ElwhaCircularProgressIndicator.wavyIndeterminate();

    assertThat(bar.isWavy()).isTrue();
    assertThat(bar.isIndeterminate()).isTrue();
    assertThat(ring.isWavy()).isTrue();
    assertThat(ring.isIndeterminate()).isTrue();
  }

  // ---------------------------------------------------------- geometry knobs

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void defaultThicknessAndGapAreTheM3Tokens(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    assertThat(indicator.getTrackThickness())
        .isEqualTo(AbstractElwhaProgressIndicator.TRACK_THICKNESS_DEFAULT_PX);
    assertThat(indicator.getIndicatorTrackGapSize())
        .as("§5 — the M3 TrackActiveSpace")
        .isEqualTo(AbstractElwhaProgressIndicator.TRACK_ACTIVE_SPACE_DEFAULT_PX);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void thicknessIsFlooredAtOnePixel(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setTrackThickness(0);
    assertThat(indicator.getTrackThickness())
        .as("a zero-thickness track has nothing to paint, so the setter floors it")
        .isOne();

    indicator.setTrackThickness(-9);
    assertThat(indicator.getTrackThickness()).isOne();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void gapIsFlooredAtZero(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setIndicatorTrackGapSize(-4);

    assertThat(indicator.getIndicatorTrackGapSize())
        .as("a negative gap would overlap the arcs; zero is the floor")
        .isZero();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void defaultColorRolesAreTheM3Pair(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    assertThat(indicator.getIndicatorColorRole()).isEqualTo(ColorRole.PRIMARY);
    assertThat(indicator.getTrackColorRole()).isEqualTo(ColorRole.SECONDARY_CONTAINER);
  }

  // -------------------------------------------------------------- wave axes

  @Test
  void eachVariantCarriesItsOwnSpecWaveValues() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator();

    assertThat(bar.getWaveAmplitude())
        .as("§6 — the linear bar's M3 amplitude")
        .isEqualTo(ElwhaLinearProgressIndicator.WAVE_AMPLITUDE_PX);
    assertThat(bar.getWavelengthDeterminate())
        .isEqualTo(ElwhaLinearProgressIndicator.WAVELENGTH_DETERMINATE_PX);
    assertThat(bar.getWavelengthIndeterminate())
        .as("§6 — the indeterminate wave is tighter than the determinate one")
        .isEqualTo(ElwhaLinearProgressIndicator.WAVELENGTH_INDETERMINATE_PX);
    assertThat(ring.getWaveAmplitude())
        .as("§6 — the ring's scallop is shallower")
        .isEqualTo(ElwhaCircularProgressIndicator.WAVE_AMPLITUDE_PX);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void waveGeometryIsClampedToPaintableValues(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setWaveAmplitude(-3f);
    assertThat(indicator.getWaveAmplitude()).as("a negative amplitude is just flat").isZero();

    indicator.setWavelengthDeterminate(0);
    indicator.setWavelengthIndeterminate(-5);
    assertThat(indicator.getWavelengthDeterminate())
        .as("a zero wavelength has no period to travel; one pixel is the floor")
        .isOne();
    assertThat(indicator.getWavelengthIndeterminate()).isOne();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void convenienceSetterWritesBothWavelengths(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setWavelength(33);

    assertThat(indicator.getWavelengthDeterminate()).isEqualTo(33);
    assertThat(indicator.getWavelengthIndeterminate()).isEqualTo(33);
  }

  @Test
  void defaultWaveSpeedIsOneWavelengthPerSecondForTheActiveMode() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();

    assertThat(bar.getWaveSpeed())
        .as("§6 — the auto default is mode-aware, tracking the determinate wavelength")
        .isEqualTo(ElwhaLinearProgressIndicator.WAVELENGTH_DETERMINATE_PX);

    bar.setIndeterminate(true);

    assertThat(bar.getWaveSpeed())
        .as("§6 — and follows the mode switch to the indeterminate wavelength")
        .isEqualTo(ElwhaLinearProgressIndicator.WAVELENGTH_INDETERMINATE_PX);
  }

  @Test
  void aZeroWaveSpeedFreezesTheWave() {
    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.wavy();

    bar.setWaveSpeed(0f);

    assertThat(bar.getWaveSpeed())
        .as("zero is an explicit value, not a request for the default")
        .isZero();
  }

  @Test
  void aNegativeWaveSpeedRestoresTheAutoDefault() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();
    bar.setWaveSpeed(5f);

    bar.setWaveSpeed(-1f);

    assertThat(bar.getWaveSpeed())
        .as("§6 — negative is the documented way back to one wavelength per second")
        .isEqualTo(ElwhaLinearProgressIndicator.WAVELENGTH_DETERMINATE_PX);
  }

  // -------------------------------------------------------- animation demand

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void aDeterminateFlatIndicatorNeverRunsTheClock(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setValue(50);

    assertThat(indicator.isAnimating())
        .as("§6 — nothing moves in a flat determinate readout, so no timer is spent on it")
        .isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void anIndicatorOutsideAHierarchyNeverRunsTheClock(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setIndeterminate(true);
    indicator.setWavy(true);

    assertThat(indicator.isAnimating())
        .as("§6 — the demand recompute gates on isShowing, so an unmounted indicator stays idle")
        .isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void leavingTheHierarchyStopsTheClock(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    indicator.setIndeterminate(true);

    indicator.removeNotify();

    assertThat(indicator.isAnimating())
        .as("§6 — no leaked timers when an animating indicator is torn down")
        .isFalse();
  }

  // ------------------------------------------------------------- posture

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void indicatorsAreNonInteractive(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    assertThat(indicator.isFocusable())
        .as("a progress readout is a status display, not a control")
        .isFalse();
    assertThat(indicator.isOpaque())
        .as("the chrome is round, so the component must not claim its whole box")
        .isFalse();
  }
}
