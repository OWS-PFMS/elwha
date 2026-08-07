package com.owspfm.elwha.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleValue;
import javax.swing.DefaultBoundedRangeModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A coverage of the progress family's accessibility surface — design doc §8: the {@link
 * AccessibleRole#PROGRESS_BAR} role, the model-backed {@link AccessibleValue}, and the M3 / {@code
 * JProgressBar} rule that an indeterminate indicator reports {@link AccessibleState#BUSY} and
 * withholds a current value, because it has no progress to report.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaProgressAccessibilityTest {

  static Stream<Arguments> indicators() {
    return Stream.of(
        Arguments.of(
            "linear", (Supplier<AbstractElwhaProgressIndicator>) ElwhaLinearProgressIndicator::new),
        Arguments.of(
            "circular",
            (Supplier<AbstractElwhaProgressIndicator>) ElwhaCircularProgressIndicator::new));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void roleIsProgressBar(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    assertThat(factory.get().getAccessibleContext().getAccessibleRole())
        .as("§8 — AT recognises these as progress bars regardless of the painted shape")
        .isEqualTo(AccessibleRole.PROGRESS_BAR);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void contextIsItsOwnValueModel(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AccessibleContext context = factory.get().getAccessibleContext();

    assertThat(context.getAccessibleValue())
        .as("§8 — the value surface is exposed on the context itself, as JProgressBar does")
        .isSameAs(context);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void accessibleValueMirrorsTheModel(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    indicator.setValue(37);

    final AccessibleValue value = indicator.getAccessibleContext().getAccessibleValue();

    assertThat(value.getCurrentAccessibleValue()).isEqualTo(37);
    assertThat(value.getMinimumAccessibleValue()).isEqualTo(0);
    assertThat(value.getMaximumAccessibleValue()).isEqualTo(100);
  }

  @Test
  void accessibleMaximumIsTheLargestReachableValue() {
    final ElwhaLinearProgressIndicator bar =
        new ElwhaLinearProgressIndicator(new DefaultBoundedRangeModel(0, 20, 0, 100));

    assertThat(bar.getAccessibleContext().getAccessibleValue().getMaximumAccessibleValue())
        .as("§8 — with an extent the value tops out below the model maximum, and AT is told so")
        .isEqualTo(80);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void anIndeterminateIndicatorWithholdsItsValue(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    indicator.setValue(60);

    indicator.setIndeterminate(true);

    assertThat(indicator.getAccessibleContext().getAccessibleValue().getCurrentAccessibleValue())
        .as("§8 — there is no meaningful progress while indeterminate, so none is announced")
        .isNull();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void valueReturnsWhenTheModeDoes(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    indicator.setValue(60);
    indicator.setIndeterminate(true);

    indicator.setIndeterminate(false);

    assertThat(indicator.getAccessibleContext().getAccessibleValue().getCurrentAccessibleValue())
        .as("the value was withheld, not discarded")
        .isEqualTo(60);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void anIndeterminateIndicatorReportsBusy(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.setIndeterminate(true);

    assertThat(
            indicator.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.BUSY))
        .as("§8 — BUSY is how 'working, duration unknown' is conveyed")
        .isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void aDeterminateIndicatorIsNotBusy(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    assertThat(
            factory
                .get()
                .getAccessibleContext()
                .getAccessibleStateSet()
                .contains(AccessibleState.BUSY))
        .as("a determinate readout reports a value instead of a busy state")
        .isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void settingTheAccessibleValueDrivesTheModel(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    final boolean applied =
        indicator.getAccessibleContext().getAccessibleValue().setCurrentAccessibleValue(45);

    assertThat(applied).isTrue();
    assertThat(indicator.getValue()).isEqualTo(45);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void aNullAccessibleValueIsRejectedRatherThanThrowing(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    assertThat(
            factory
                .get()
                .getAccessibleContext()
                .getAccessibleValue()
                .setCurrentAccessibleValue(null))
        .as("a null write reports failure; it does not blow up on the AT's thread")
        .isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void aValueChangeIsAnnouncedToListeners(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    final List<PropertyChangeEvent> events = new ArrayList<>();
    indicator.getAccessibleContext().addPropertyChangeListener(events::add);

    indicator.setValue(55);

    assertThat(events)
        .as("§8 — AT is pushed the new value rather than having to poll for it")
        .anyMatch(
            e ->
                AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(e.getPropertyName())
                    && Integer.valueOf(55).equals(e.getNewValue()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void anIndeterminateIndicatorAnnouncesNoValueChanges(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();
    indicator.setIndeterminate(true);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    indicator.getAccessibleContext().addPropertyChangeListener(events::add);

    indicator.setValue(55);

    assertThat(events)
        .as("announcing a value that getCurrentAccessibleValue withholds would contradict itself")
        .noneMatch(e -> AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(e.getPropertyName()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void contextIsStableAcrossCalls(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    assertThat(indicator.getAccessibleContext())
        .as("AT holds onto the context, so it must not be rebuilt per call")
        .isSameAs(indicator.getAccessibleContext());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicators")
  void consumersCanNameTheActivity(
      final String name, final Supplier<AbstractElwhaProgressIndicator> factory) {
    final AbstractElwhaProgressIndicator indicator = factory.get();

    indicator.getAccessibleContext().setAccessibleName("Download progress");

    assertThat(indicator.getAccessibleContext().getAccessibleName())
        .as("§8 — the component cannot know what it is measuring; the consumer names it")
        .isEqualTo("Download progress");
  }
}
