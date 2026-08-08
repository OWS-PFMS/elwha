package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Font;
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

  /**
   * The type half of the FlatLaf bridge (#696). Before it, every raw Swing component read {@code
   * defaultFont} and so painted Body Medium — a {@code JButton}, a {@code JTable} row and a tab
   * label were typographically identical, and the "raw Swing inherits the design language" claim
   * held for colour and shape but not type. M3 assigns a role per component archetype, and Swing
   * keys fonts by component class, so those assignments transfer exactly.
   */
  @Test
  void rawSwingFontKeysCarryTheRoleM3AssignsEachArchetype() {
    final Typography installed = Typography.defaults();

    assertRole("Button.font", TypeRole.LABEL_LARGE, installed);
    assertRole("CheckBox.font", TypeRole.LABEL_LARGE, installed);
    assertRole("MenuItem.font", TypeRole.LABEL_LARGE, installed);
    assertRole("TextField.font", TypeRole.BODY_LARGE, installed);
    assertRole("ComboBox.font", TypeRole.BODY_LARGE, installed);
    assertRole("List.font", TypeRole.BODY_LARGE, installed);
    assertRole("Label.font", TypeRole.BODY_MEDIUM, installed);
    assertRole("ToolTip.font", TypeRole.BODY_SMALL, installed);
    assertRole("TabbedPane.font", TypeRole.TITLE_SMALL, installed);
    assertRole("TableHeader.font", TypeRole.TITLE_SMALL, installed);
    assertRole("ProgressBar.font", TypeRole.LABEL_MEDIUM, installed);

    assertThat(UIManager.getFont("Button.font"))
        .as("the point of the bridge: a button and a body label no longer read the same font")
        .isNotEqualTo(UIManager.getFont("Label.font"));
  }

  /**
   * FlatLaf's style-class ladder is how raw Swing names a role per <em>usage</em> — the case
   * Swing's one {@code Label.font} cannot express, and the one #696 opened with. Left un-bridged,
   * {@code h1}-{@code h3} resolve through {@code $semibold.font}, which falls back to a system face
   * when the installed family has no semibold: measured at Helvetica Neue while everything around
   * it was Inter. So this pins both halves — the rungs land on M3 steps, and none of them leaves
   * the installed family.
   */
  @Test
  void styleClassLadderLandsOnM3StepsInsideTheInstalledFamily() {
    final Typography installed = Typography.defaults();

    assertRole("h00.font", TypeRole.DISPLAY_SMALL, installed);
    assertRole("h0.font", TypeRole.HEADLINE_LARGE, installed);
    assertRole("h1.font", TypeRole.HEADLINE_MEDIUM, installed);
    assertRole("h2.font", TypeRole.HEADLINE_SMALL, installed);
    assertRole("h3.font", TypeRole.TITLE_LARGE, installed);
    assertRole("h4.font", TypeRole.TITLE_MEDIUM, installed);
    assertRole("large.font", TypeRole.BODY_LARGE, installed);
    assertRole("small.font", TypeRole.BODY_SMALL, installed);
    assertRole("semibold.font", TypeRole.LABEL_LARGE, installed);

    assertThat(UIManager.getFont("h1.font").getFamily())
        .as("the regression: a heading must not escape to a system face for want of a semibold")
        .startsWith(installed.familyName());
  }

  /**
   * #658 asked whether writing {@code defaultFont} <em>after</em> the base look-and-feel installs
   * means raw Swing never picks it up — FlatLaf reads that key while building its defaults table,
   * so a later write should arrive too late. It does not: FlatLaf resolves lazily against {@code
   * defaultFont}. Two ladder rungs are deliberately left to that derivation ({@code medium.font} at
   * default−1 and {@code mini.font} at default−3 land on sizes the M3 scale has no step for), which
   * makes them the live witness that the ordering still holds now that #696 writes explicit values
   * over most of the table.
   */
  @Test
  void unbridgedRungsStillDeriveFromDefaultFont() {
    final Typography installed = Typography.defaults();
    final int defaultSize = installed.get(TypeRole.BODY_MEDIUM).getSize();

    assertThat(UIManager.getFont("medium.font"))
        .as("medium.font is derived, not written")
        .isNotNull()
        .extracting(Font::getFamily, Font::getSize)
        .containsExactly(installed.familyName(), defaultSize - 1);
    assertThat(UIManager.getFont("mini.font"))
        .as("and so is mini.font")
        .isNotNull()
        .extracting(Font::getFamily, Font::getSize)
        .containsExactly(installed.familyName(), defaultSize - 3);
  }

  @Test
  void bridgedFontKeysAreStoredAsUiResourcesSoUpdateUiReinstallsThem() {
    assertThat(UIManager.get("Button.font"))
        .as("a plain Font would be treated as developer-owned and survive a re-skin")
        .isInstanceOf(FontUIResource.class);
  }

  private static void assertRole(final String key, final TypeRole role, final Typography source) {
    assertThat(UIManager.getFont(key))
        .as("%s carries %s", key, role)
        .isNotNull()
        .isEqualTo(source.get(role));
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
  void baseLookAndFeelTracksTheResolvedMode() {
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
