package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of {@link ColorHex} — the picker's hex round trip. The parse contract is total:
 * any text that is not valid web hex answers {@code null}, because the SLIDERS pane calls it from a
 * {@code focusLost} handler where a thrown exception would escape onto the dispatch thread and
 * leave the field neither reverted nor showing its error text (#616).
 */
class ColorHexTest {

  // ------------------------------------------------------------------ format

  @Test
  void aColorFormatsAsUppercaseWebHex() {
    assertThat(ColorHex.format(new Color(18, 52, 86), false)).isEqualTo("#123456");
    assertThat(ColorHex.format(new Color(18, 52, 86, 128), true))
        .as("alpha is appended in web order, not AWT's leading-alpha int layout")
        .isEqualTo("#12345680");
  }

  // ------------------------------------------------------------------- parse

  @Test
  void validHexParsesWithAndWithoutItsHashAndShorthand() {
    assertThat(ColorHex.parse("#123456", false)).isEqualTo(new Color(18, 52, 86));
    assertThat(ColorHex.parse("123456", false)).isEqualTo(new Color(18, 52, 86));
    assertThat(ColorHex.parse("#abc", false)).isEqualTo(new Color(170, 187, 204));
    assertThat(ColorHex.parse("#12345680", true)).isEqualTo(new Color(18, 52, 86, 128));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-12345", "+12345", "-1234", "-12345678", "1234 6", "12345g", "#-abcde"})
  void aSignedOrOtherwiseNonHexStringOfTheRightLengthAnswersNull(final String text) {
    // #616 — Integer.parseInt accepts a leading sign, so "-12345" reached `new Color(-1, ...)` and
    // threw IllegalArgumentException out of a focusLost handler, while "+12345" quietly parsed to a
    // color the user never typed. Length was never the same question as "is this hex".
    assertThat(ColorHex.parse(text, true))
        .as("the documented contract is null for anything that is not valid hex")
        .isNull();
  }

  @Test
  void theWrongNumberOfDigitsAnswersNull() {
    assertThat(ColorHex.parse(null, false)).isNull();
    assertThat(ColorHex.parse("", false)).isNull();
    assertThat(ColorHex.parse("#12345", false)).isNull();
    assertThat(ColorHex.parse("#123456789", true)).isNull();
  }

  @Test
  void anAlphaFormIsRejectedWhenAlphaIsNotAllowed() {
    assertThat(ColorHex.parse("#12345680", false))
        .as("an opaque-only field must not silently drop the alpha byte")
        .isNull();
    assertThat(ColorHex.parse("#abcd", false)).isNull();
  }
}
