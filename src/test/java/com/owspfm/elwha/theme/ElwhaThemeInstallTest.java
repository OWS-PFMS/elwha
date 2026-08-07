package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of {@link ElwhaTheme#install} — that the ordered eight-step sequence populates
 * every token namespace, that re-installing is the whole runtime-switch mechanism, that {@link
 * Mode#SYSTEM} resolves before anything is written, and that values land as {@code UIResource}s so
 * a later {@code updateUI()} re-installs them. Oracle: {@code
 * docs/research/elwha-theme-install-api.md} §3 and §6.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaThemeInstallTest {

  // --------------------------------------------------- the token namespaces

  @Test
  void installWritesEveryColorRole() {
    for (final ColorRole role : ColorRole.values()) {
      assertThat(UIManager.getColor(role.uiKey()))
          .as("%s has a key after install", role)
          .isNotNull();
    }
  }

  @Test
  void installWritesEveryGeometryAndStateToken() {
    for (final ShapeScale shape : ShapeScale.values()) {
      assertThat(UIManager.get(shape.uiKey())).as("%s has a key after install", shape).isNotNull();
    }
    for (final SpaceScale space : SpaceScale.values()) {
      assertThat(UIManager.get(space.uiKey())).as("%s has a key after install", space).isNotNull();
    }
    for (final StateLayer layer : StateLayer.values()) {
      assertThat(UIManager.get(layer.uiKey())).as("%s has a key after install", layer).isNotNull();
    }
    assertThat(UIManager.get(StateLayer.DISABLED_CONTENT_KEY))
        .as("the disabled-content treatment is written alongside the layers")
        .isNotNull();
    assertThat(UIManager.get(StateLayer.DISABLED_CONTAINER_KEY))
        .as("so is the disabled-container treatment")
        .isNotNull();
  }

  @Test
  void installWritesEveryTypeRoleAndTheGlobalDefaultFont() {
    for (final TypeRole role : TypeRole.values()) {
      assertThat(UIManager.getFont(role.uiKey()))
          .as("%s has a font after install", role)
          .isNotNull();
    }
    assertThat(UIManager.getFont("defaultFont"))
        .as("raw Swing gets a default font from the same typography")
        .isNotNull();
  }

  @Test
  void colorsAreStoredAsUiResourcesSoUpdateUiReinstallsThem() {
    assertThat(UIManager.get(ColorRole.PRIMARY.uiKey()))
        .as("a plain Color would be treated as developer-owned and never replaced on a re-skin")
        .isInstanceOf(ColorUIResource.class);
  }

  @Test
  void fontsAreStoredAsUiResourcesForTheSameReason() {
    assertThat(UIManager.get(TypeRole.BODY_MEDIUM.uiKey()))
        .as("fonts follow the same UIResource rule as colors")
        .isInstanceOf(FontUIResource.class);
    assertThat(UIManager.get("defaultFont"))
        .as("the global default font is a UIResource too")
        .isInstanceOf(FontUIResource.class);
  }

  // -------------------------------------------------------- runtime switching

  @Test
  void reinstallingWithAnotherModeIsTheWholeSwitchMechanism() {
    ThemeExtension.install(Mode.LIGHT);
    final Color lightSurface = ColorRole.SURFACE.resolve();

    ElwhaTheme.install(ElwhaTheme.current().withMode(Mode.DARK));

    assertThat(ColorRole.SURFACE.resolve())
        .as("there is no separate switch API — re-install rewrites the keys in place")
        .isEqualTo(MaterialPalettes.baseline().dark().get(ColorRole.SURFACE))
        .isNotEqualTo(lightSurface);
  }

  @Test
  void reinstallingWithAnotherThemeSwapsThePalette() {
    final Theme blue =
        MaterialPalettes.primary().stream()
            .filter(theme -> !"Material Baseline".equals(theme.name()))
            .findFirst()
            .orElseThrow();

    ElwhaTheme.install(ElwhaTheme.current().withTheme(blue));

    assertThat(ColorRole.PRIMARY.resolve().getRGB())
        .as("a theme switch and a mode switch are the same operation on a different field")
        .isEqualTo(blue.light().get(ColorRole.PRIMARY).getRGB());
  }

  @Test
  void installIsIdempotent() {
    final Color before = ColorRole.SURFACE.resolve();

    ElwhaTheme.install(ElwhaTheme.current());
    ElwhaTheme.install(ElwhaTheme.current());

    assertThat(ColorRole.SURFACE.resolve())
        .as("re-installing the same config leaves the resolved tokens where they were")
        .isEqualTo(before);
  }

  @Test
  void currentReportsTheConfigMostRecentlyInstalled() {
    final Config config =
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build();

    ElwhaTheme.install(config);

    assertThat(ElwhaTheme.current())
        .as("callers derive their next switch from current() rather than threading it themselves")
        .isSameAs(config);
  }

  @Test
  void installRejectsANullConfig() {
    assertThatThrownBy(() -> ElwhaTheme.install(null))
        .as("there is no implicit default config")
        .isInstanceOf(NullPointerException.class);
  }

  // ------------------------------------------------------------- mode resolution

  @Test
  void systemModeResolvesToAConcreteModeBeforeAnythingIsWritten() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());

    final Palette installed = MaterialPalettes.baseline().paletteFor(Mode.SYSTEM);
    assertThat(ColorRole.SURFACE.resolve().getRGB())
        .as("SYSTEM installs whichever concrete scheme the OS probe resolved to")
        .isEqualTo(installed.get(ColorRole.SURFACE).getRGB());
    assertThat(ElwhaTheme.current().mode())
        .as("the config keeps SYSTEM — resolution happens at install, not in the config")
        .isEqualTo(Mode.SYSTEM);
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void everyModeInstallsWithoutThrowing(final Mode mode) {
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());

    assertThat(ColorRole.SURFACE.resolve()).as("%s produces a live theme", mode).isNotNull();
  }

  @Test
  void theBaseLookAndFeelTracksTheResolvedMode() {
    ThemeExtension.install(Mode.DARK);

    assertThat(UIManager.getLookAndFeel().getName())
        .as("dark mode installs FlatLaf's dark base so unthemed Swing keys follow too")
        .containsIgnoringCase("dark");

    ThemeExtension.install(Mode.LIGHT);

    assertThat(UIManager.getLookAndFeel().getName())
        .as("light mode installs the light base")
        .containsIgnoringCase("light");
  }

  // ------------------------------------------------------- reduced motion

  @Test
  void anExplicitReducedMotionOverrideIsApplied() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).reducedMotion(false).build());

    assertThat(MorphAnimator.isReducedMotion())
        .as("a non-null override forces the global toggle regardless of the OS signal")
        .isFalse();
  }

  @Test
  void anAbsentReducedMotionOverrideDefersToWhateverIsAlreadySet() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).reducedMotion(false).build());

    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).reducedMotion(null).build());

    assertThat(MorphAnimator.isReducedMotion())
        .as("null means defer to the OS signal, so install must not clobber the toggle")
        .isFalse();
  }
}
