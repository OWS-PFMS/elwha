package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A throw-contract coverage for {@link ElwhaCardHeader}'s argument validation (conventions
 * §15): the string shorthands fail fast on {@code null} with the parameter named — matching the
 * typed-atom overloads that already reject a {@code null} atom — while the atoms' own constructors
 * keep their documented null-as-empty tolerance for the deliberately empty case.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardHeaderValidationTest {

  @Test
  void setTitleShorthandRejectsANullTextNamingTheParameter() {
    final ElwhaCardHeader header = new ElwhaCardHeader();

    assertThatNullPointerException()
        .isThrownBy(() -> header.setTitle((String) null))
        .withMessage("text");
  }

  @Test
  void setSubtitleShorthandRejectsANullTextNamingTheParameter() {
    final ElwhaCardHeader header = new ElwhaCardHeader();

    assertThatNullPointerException()
        .isThrownBy(() -> header.setSubtitle((String) null))
        .withMessage("text");
  }

  @Test
  void typedOverloadsStillRejectANullAtom() {
    final ElwhaCardHeader header = new ElwhaCardHeader();

    assertThatNullPointerException()
        .isThrownBy(() -> header.setTitle((ElwhaCardTitle) null))
        .withMessage("title");
    assertThatNullPointerException()
        .isThrownBy(() -> header.setSubtitle((ElwhaCardSubtitle) null))
        .withMessage("subtitle");
  }

  @Test
  void atomConstructorsKeepTheirDocumentedNullTolerance() {
    assertThatCode(() -> new ElwhaCardTitle(null)).doesNotThrowAnyException();
    assertThatCode(() -> new ElwhaCardSubtitle(null)).doesNotThrowAnyException();

    final ElwhaCardHeader header = new ElwhaCardHeader();
    header.setTitle(new ElwhaCardTitle(null));
    assertThat(header.getTitle())
        .as("a deliberately empty title is constructed explicitly through the atom")
        .isNotNull();
  }
}
