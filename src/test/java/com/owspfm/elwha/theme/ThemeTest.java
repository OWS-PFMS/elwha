package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the light/dark palette pair — its required parts and the mode-to-palette
 * selection {@link ElwhaTheme#install} performs as step 3. Oracle: {@code
 * docs/research/elwha-theme-install-api.md} §1.2.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ThemeTest {

  private static Theme baseline() {
    return MaterialPalettes.baseline();
  }

  @Test
  void aThemeIsANameAndTwoPalettes() {
    assertThatThrownBy(() -> new Theme(null, baseline().light(), baseline().dark()))
        .as("a theme without a name cannot be offered in a picker")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Theme("x", null, baseline().dark()))
        .as("a theme without a light scheme is half a theme")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Theme("x", baseline().light(), null))
        .as("a theme without a dark scheme is half a theme")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void concreteModesSelectTheirOwnPalette() {
    assertThat(baseline().paletteFor(Mode.LIGHT))
        .as("LIGHT takes the light scheme")
        .isSameAs(baseline().light());
    assertThat(baseline().paletteFor(Mode.DARK))
        .as("DARK takes the dark scheme")
        .isSameAs(baseline().dark());
  }

  @Test
  void systemResolvesBeforeSelectingAPalette() {
    assertThat(baseline().paletteFor(Mode.SYSTEM))
        .as("SYSTEM never leaks through as a third scheme — it resolves to one of the two")
        .isIn(baseline().light(), baseline().dark());
  }

  @Test
  void twoSchemesAreDistinctObjects() {
    assertThat(baseline().light())
        .as("light and dark are separate palettes, not one shared map")
        .isNotSameAs(baseline().dark());
  }
}
