package com.owspfm.elwha.icons;

import static org.assertj.core.api.Assertions.assertThat;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the bundled Material Symbols lookups — that every shipped factory resolves a
 * real SVG at both its default and its explicit size, that instances are never shared across call
 * sites, and that the {@link MaterialIcons.Symbol} fill axis falls back gracefully for glyphs the
 * bundle ships no fill variant for.
 *
 * <p>The factory sweep is reflective on purpose: adding a lookup whose resource name is misspelled
 * is exactly the kind of mistake a hand-maintained list would let through.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class MaterialIconsTest {

  /** A glyph the bundle ships both fill axes for. */
  private static final String SYMBOL_WITH_FILL = "push_pin";

  /** A glyph with no semantic fill axis, so no {@code _fill} asset is bundled. */
  private static final String SYMBOL_WITHOUT_FILL = "check";

  private static List<Method> factories(final Class<?>... parameterTypes) {
    final List<Method> found = new ArrayList<>();
    for (final Method method : MaterialIcons.class.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && Modifier.isStatic(method.getModifiers())
          && method.getReturnType() == FlatSVGIcon.class
          && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
        found.add(method);
      }
    }
    found.sort(Comparator.comparing(Method::getName));
    return found;
  }

  private static FlatSVGIcon invoke(final Method factory, final Object... args) {
    try {
      return (FlatSVGIcon) factory.invoke(null, args);
    } catch (final ReflectiveOperationException failure) {
      throw new AssertionError("could not call " + factory.getName(), failure);
    }
  }

  // -------------------------------------------------------- the default size

  @Test
  void defaultSizeIsTheM3IconButtonSize() {
    assertThat(MaterialIcons.DEFAULT_SIZE)
        .as("Material Symbols are drawn on the 20-dp optical axis and render at 24 px")
        .isEqualTo(24);
  }

  // ------------------------------------------------------------ the sweep

  @Test
  void bundleShipsAGenerousSetOfLookups() {
    assertThat(factories())
        .as("a reflective sweep that found almost nothing would silently assert nothing")
        .hasSizeGreaterThan(30);
  }

  @Test
  void everyNoArgFactoryResolvesARealSvgAtTheDefaultSize() {
    for (final Method factory : factories()) {
      final FlatSVGIcon icon = invoke(factory);

      assertThat(icon).as("%s returns an icon", factory.getName()).isNotNull();
      assertThat(icon.hasFound())
          .as("%s names an SVG that is actually bundled", factory.getName())
          .isTrue();
      assertThat(icon.getIconWidth())
          .as("%s renders at the default size", factory.getName())
          .isEqualTo(MaterialIcons.DEFAULT_SIZE);
      assertThat(icon.getIconHeight())
          .as("%s renders square", factory.getName())
          .isEqualTo(MaterialIcons.DEFAULT_SIZE);
    }
  }

  @Test
  void everySizedFactoryHonorsTheRequestedSize() {
    for (final Method factory : factories(int.class)) {
      final FlatSVGIcon icon = invoke(factory, 16);

      assertThat(icon.hasFound())
          .as("%s(int) names an SVG that is actually bundled", factory.getName())
          .isTrue();
      assertThat(icon.getIconWidth())
          .as("%s(int) renders at the size it was asked for", factory.getName())
          .isEqualTo(16);
      assertThat(icon.getIconHeight()).as("%s(int) stays square", factory.getName()).isEqualTo(16);
    }
  }

  @Test
  void everyNoArgFactoryHasASizedCounterpart() {
    final List<String> sized = factories(int.class).stream().map(Method::getName).toList();

    for (final Method factory : factories()) {
      assertThat(sized)
          .as(
              "%s must pair with a sized overload so callers never chain derive()",
              factory.getName())
          .contains(factory.getName());
    }
  }

  @Test
  void everyLookupHandsBackAFreshInstance() {
    assertThat(MaterialIcons.pushPin())
        .as("a shared instance would leak one call site's color filter or resize into every other")
        .isNotSameAs(MaterialIcons.pushPin());
  }

  @Test
  void everyLookupArrivesAlreadyThemed() {
    for (final Method factory : factories()) {
      assertThat(invoke(factory).getColorFilter())
          .as(
              "%s carries the shared Label.foreground filter without the caller asking",
              factory.getName())
          .isSameAs(MaterialIcons.LABEL_FOREGROUND_FILTER);
    }
  }

  // ---------------------------------------------------------------- themed

  @Test
  void themedAttachesTheSharedFilterAndReturnsTheSameIcon() {
    final FlatSVGIcon consumerIcon = new FlatSVGIcon("com/owspfm/icons/material/check.svg", 20, 20);

    final FlatSVGIcon returned = MaterialIcons.themed(consumerIcon);

    assertThat(returned)
        .as("themed follows FlatLaf's fluent-setter convention and mutates in place")
        .isSameAs(consumerIcon);
    assertThat(returned.getColorFilter())
        .as("a consumer-owned SVG gets exactly the filter the bundled ones carry")
        .isSameAs(MaterialIcons.LABEL_FOREGROUND_FILTER);
  }

  // ------------------------------------------------------------------ pair

  @Test
  void aPairResolvesBothFillAxes() {
    final MaterialIcons.IconPair pair = MaterialIcons.pair(SYMBOL_WITH_FILL);

    assertThat(pair.resting().hasFound()).as("the outline half is bundled").isTrue();
    assertThat(pair.filled().hasFound()).as("the fill half is bundled").isTrue();
    assertThat(pair.filled().getName())
        .as("the fill variant is the base name plus the _fill suffix")
        .contains(SYMBOL_WITH_FILL + "_fill");
    assertThat(pair.resting().getIconWidth())
        .as("both halves render at the default size")
        .isEqualTo(MaterialIcons.DEFAULT_SIZE);
  }

  @Test
  void aSizedPairSizesBothHalves() {
    final MaterialIcons.IconPair pair = MaterialIcons.pair(SYMBOL_WITH_FILL, 18);

    assertThat(pair.resting().getIconWidth()).as("the outline half is sized").isEqualTo(18);
    assertThat(pair.filled().getIconWidth()).as("the fill half is sized to match").isEqualTo(18);
  }

  // ---------------------------------------------------------------- symbol

  @Test
  void aSymbolRemembersItsBareName() {
    assertThat(MaterialIcons.symbol(SYMBOL_WITH_FILL).name())
        .as("the handle carries the name with no path, extension, or suffix")
        .isEqualTo(SYMBOL_WITH_FILL);
  }

  @Test
  void aSymbolWithAFillAssetSwapsGlyphsWhenSelected() {
    final MaterialIcons.Symbol symbol = MaterialIcons.symbol(SYMBOL_WITH_FILL);

    assertThat(symbol.hasSelectedVariant())
        .as("%s ships a fill-1 asset", SYMBOL_WITH_FILL)
        .isTrue();
    assertThat(symbol.selected().getName())
        .as("selecting swaps to the fill-1 glyph")
        .isNotEqualTo(symbol.unselected().getName());
    assertThat(symbol.selected().hasFound()).as("the swapped-in glyph resolves").isTrue();
  }

  @Test
  void theFillVariantAnswerIsHeldPerSymbolRatherThanReAskedTheClasspath() {
    final MaterialIcons.Symbol withFill = MaterialIcons.symbol(SYMBOL_WITH_FILL);
    final MaterialIcons.Symbol withoutFill = MaterialIcons.symbol(SYMBOL_WITHOUT_FILL);

    for (int call = 0; call < 3; call++) {
      assertThat(withFill.hasSelectedVariant())
          .as("call %d — memoising must not drift from the bundle's answer", call)
          .isTrue();
      assertThat(withoutFill.hasSelectedVariant())
          .as("call %d — and the held answer is per symbol, not one shared flag", call)
          .isFalse();
    }
  }

  @Test
  void aSymbolWithNoFillAssetFallsBackToTheOutlineRatherThanBreaking() {
    final MaterialIcons.Symbol symbol = MaterialIcons.symbol(SYMBOL_WITHOUT_FILL);

    assertThat(symbol.hasSelectedVariant())
        .as(
            "%s has no semantic fill axis, so the bundle ships no fill-1 asset",
            SYMBOL_WITHOUT_FILL)
        .isFalse();
    assertThat(symbol.selected().getName())
        .as("no fill variant means no visual swap — the correct result, not an error")
        .isEqualTo(symbol.unselected().getName());
    assertThat(symbol.selected().hasFound())
        .as("the fallback resolves a real glyph rather than a missing _fill resource")
        .isTrue();
  }

  @Test
  void bothSymbolAxesHonorAnExplicitSize() {
    final MaterialIcons.Symbol symbol = MaterialIcons.symbol(SYMBOL_WITH_FILL);

    assertThat(symbol.unselected(14).getIconWidth()).as("the outline axis sizes").isEqualTo(14);
    assertThat(symbol.selected(14).getIconWidth()).as("the fill axis sizes to match").isEqualTo(14);
  }

  @Test
  void bothSymbolAxesDefaultToTheDefaultSize() {
    final MaterialIcons.Symbol symbol = MaterialIcons.symbol(SYMBOL_WITH_FILL);

    assertThat(symbol.unselected().getIconWidth())
        .as("the no-arg outline form takes the default size")
        .isEqualTo(MaterialIcons.DEFAULT_SIZE);
    assertThat(symbol.selected().getIconWidth())
        .as("the no-arg fill form takes the default size")
        .isEqualTo(MaterialIcons.DEFAULT_SIZE);
  }

  @Test
  void symbolGlyphsAreThemedLikeTheNamedLookups() {
    final MaterialIcons.Symbol symbol = MaterialIcons.symbol(SYMBOL_WITH_FILL);

    assertThat(symbol.unselected().getColorFilter())
        .as("a symbol handle routes through the same load path as a named factory")
        .isSameAs(MaterialIcons.LABEL_FOREGROUND_FILTER);
    assertThat(symbol.selected().getColorFilter())
        .as("the fill axis is themed too")
        .isSameAs(MaterialIcons.LABEL_FOREGROUND_FILTER);
  }
}
