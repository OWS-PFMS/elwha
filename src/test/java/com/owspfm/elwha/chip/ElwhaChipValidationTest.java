package com.owspfm.elwha.chip;

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
 * Tier A throw-contract coverage for {@link ElwhaChip}'s argument validation (conventions §15): the
 * variant factories fail fast on a {@code null} text with the parameter named, while the
 * constructor and {@code setText} keep their documented null-as-empty semantics.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipValidationTest {

  static Stream<Arguments> factories() {
    return Stream.of(
        Arguments.of("assistChip", (Function<String, ElwhaChip>) ElwhaChip::assistChip),
        Arguments.of("filterChip", (Function<String, ElwhaChip>) ElwhaChip::filterChip),
        Arguments.of(
            "inputChip", (Function<String, ElwhaChip>) text -> ElwhaChip.inputChip(text, null)),
        Arguments.of("suggestionChip", (Function<String, ElwhaChip>) ElwhaChip::suggestionChip));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void aVariantFactoryRejectsANullTextNamingTheParameter(
      final String name, final Function<String, ElwhaChip> factory) {
    assertThatNullPointerException()
        .as(name + "(null) fails fast at the call site instead of building a blank chip")
        .isThrownBy(() -> factory.apply(null))
        .withMessage("text");
  }

  @Test
  void constructorAndSetTextKeepTheirDocumentedNullTolerance() {
    assertThatCode(() -> new ElwhaChip((String) null)).doesNotThrowAnyException();
    assertThat(new ElwhaChip((String) null).getText())
        .as("a null constructor text coerces to empty — the documented deferred-content path")
        .isEmpty();
    assertThat(new ElwhaChip("Full").setText(null).getText())
        .as("setText documents null as the empty string")
        .isEmpty();
  }
}
