package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A throw-contract coverage for {@link ElwhaButton}'s argument validation (conventions §15):
 * the variant factories fail fast on a {@code null} label with the parameter named, while the
 * constructors keep their documented null-tolerant deferred-content semantics.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonValidationTest {

  static Stream<Arguments> factories() {
    return Stream.of(
        Arguments.of("elevatedButton", (Function<String, ElwhaButton>) ElwhaButton::elevatedButton),
        Arguments.of("filledButton", (Function<String, ElwhaButton>) ElwhaButton::filledButton),
        Arguments.of(
            "filledTonalButton", (Function<String, ElwhaButton>) ElwhaButton::filledTonalButton),
        Arguments.of("outlinedButton", (Function<String, ElwhaButton>) ElwhaButton::outlinedButton),
        Arguments.of("textButton", (Function<String, ElwhaButton>) ElwhaButton::textButton));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void aVariantFactoryRejectsANullLabelNamingTheParameter(
      final String name, final Function<String, ElwhaButton> factory) {
    assertThatNullPointerException()
        .as(name + "(null) fails fast at the call site instead of building a blank button")
        .isThrownBy(() -> factory.apply(null))
        .withMessage("label");
  }

  @Test
  void constructorsKeepTheirDocumentedNullTolerance() {
    assertThatCode(() -> new ElwhaButton((String) null)).doesNotThrowAnyException();
    assertThatCode(() -> new ElwhaButton(null, null)).doesNotThrowAnyException();
    assertThat(new ElwhaButton((String) null).getText())
        .as("a null label coerces to empty — the documented deferred-content path")
        .isEmpty();
  }
}
