package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A chrome coverage for {@link ElwhaButton} — the per-variant container role, the state-layer
 * composites, the disabled treatment, the outline, and the §7 hybrid selection swaps, each probed
 * in both theme modes against resolved roles.
 *
 * <p>Every button under test carries an empty label so the whole body is chrome: the determinism
 * rules forbid probing pixels inside a glyph box, and content colors are asserted through {@link
 * ElwhaButton#resolveForegroundColor()} (a package-visible seam) rather than through paint.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonRenderTest {

  private static Stream<Arguments> variantsInBothModes() {
    return Stream.of(Mode.LIGHT, Mode.DARK)
        .flatMap(mode -> Stream.of(ButtonVariant.values()).map(v -> Arguments.of(v, mode)));
  }

  /** A label-free button at the M tier — 48x56 body, no touch-target inflation, no glyphs. */
  private static ElwhaButton chromeOnly(final ButtonVariant variant) {
    return new ElwhaButton().setVariant(variant).setButtonSize(ButtonSize.M);
  }

  private static BufferedImage render(final ElwhaButton button) {
    final Dimension pref = button.getPreferredSize();
    return Pixels.render(button, pref.width, pref.height);
  }

  /** The center of the visible body in component coordinates. */
  private static Point bodyCenter(final ElwhaButton button) {
    final Insets shadow = button.getShadowInsets();
    return new Point(
        shadow.left + (button.getPreferredSize().width - shadow.left - shadow.right) / 2,
        shadow.top + button.getButtonSize().containerHeightPx() / 2);
  }

  /**
   * The color the variant's resting body should resolve to — the ground shows through a null fill.
   */
  private static Color restingFill(final ButtonVariant variant) {
    return variant.surfaceRole() != null
        ? variant.surfaceRole().resolve()
        : ColorRole.SURFACE.resolve();
  }

  /**
   * The role a state layer tints against — the variant's own surface, or SURFACE for a null fill.
   */
  private static ColorRole overlayBasis(final ButtonVariant variant) {
    return variant.surfaceRole() != null ? variant.surfaceRole() : ColorRole.SURFACE;
  }

  // ------------------------------------------------------------- resting

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void everyVariantPaintsItsOwnContainerRole(final ButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button = chromeOnly(variant);

    final BufferedImage image = render(button);

    assertThat(button.getSurfaceRole())
        .as("%s reports the container role it paints", variant)
        .isEqualTo(variant.surfaceRole());
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        restingFill(variant),
        variant + " paints its resting container role in " + mode);
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void hoverCompositesTheEightPercentLayerOverTheContainer(
      final ButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button = chromeOnly(variant).setHovered(true);

    final BufferedImage image = render(button);

    final ColorRole basis = overlayBasis(variant);
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        Pixels.mix(
            basis.resolve(),
            basis.on().orElse(ColorRole.ON_SURFACE).resolve(),
            StateLayer.HOVER.opacity()),
        variant + " composites the hover layer over its container in " + mode);
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void pressCompositesTheTenPercentLayerOverTheContainer(
      final ButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button = chromeOnly(variant).setPressed(true);

    final BufferedImage image = render(button);

    final ColorRole basis = overlayBasis(variant);
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        Pixels.mix(
            basis.resolve(),
            basis.on().orElse(ColorRole.ON_SURFACE).resolve(),
            StateLayer.PRESSED.opacity()),
        variant + " composites the pressed layer over its container in " + mode);
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void pressWinsOverHover(final ButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button = chromeOnly(variant).setHovered(true).setPressed(true);

    final BufferedImage image = render(button);

    final ColorRole basis = overlayBasis(variant);
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        Pixels.mix(
            basis.resolve(),
            basis.on().orElse(ColorRole.ON_SURFACE).resolve(),
            StateLayer.PRESSED.opacity()),
        variant + " paints only the pressed layer while both states are live");
  }

  @Test
  void keyboardActivationFlashesThePressedLayerToo() {
    final ElwhaButton button = chromeOnly(ButtonVariant.FILLED);

    Input.pressBoundKey(button, "pressed SPACE", "elwhabutton.activate");

    final ColorRole basis = overlayBasis(ButtonVariant.FILLED);
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        Pixels.mix(
            basis.resolve(),
            basis.on().orElse(ColorRole.ON_SURFACE).resolve(),
            StateLayer.PRESSED.opacity()),
        "#562 — a keyboard user gets the same press confirmation a pointer user does; the button"
            + " used to seed only a ripple, which is suppressible, and paint no pressed layer");
  }

  // ------------------------------------------------------------ disabled

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void disabledSwapsTheContainerToOnSurfaceAtTwelvePercent(
      final ButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button = chromeOnly(variant);
    button.setEnabled(false);

    final BufferedImage image = render(button);

    final Color want =
        variant.surfaceRole() == null
            ? ColorRole.SURFACE.resolve()
            : Pixels.mix(
                ColorRole.SURFACE.resolve(),
                ColorRole.ON_SURFACE.resolve(),
                StateLayer.disabledContainerOpacity());
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        want,
        variant
            + " swaps its disabled container to ON_SURFACE in "
            + mode
            + " — #767, the role is replaced, not faded");
  }

  @ParameterizedTest
  @EnumSource(ButtonVariant.class)
  void aDisabledButtonPaintsNoStateLayerAtAll(final ButtonVariant variant) {
    final ElwhaButton button = chromeOnly(variant).setHovered(true).setPressed(true);
    button.setEnabled(false);

    final BufferedImage image = render(button);

    final Color want =
        variant.surfaceRole() == null
            ? ColorRole.SURFACE.resolve()
            : Pixels.mix(
                ColorRole.SURFACE.resolve(),
                ColorRole.ON_SURFACE.resolve(),
                StateLayer.disabledContainerOpacity());
    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        want,
        variant + " ignores hover and press once disabled — the frozen visual of §15.7");
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void disabledForegroundSwapsToOnSurface(final ButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button = chromeOnly(variant);
    button.setEnabled(false);

    assertThat(button.resolveForegroundColor())
        .as(
            "#767 — disabled label/icon color is ON_SURFACE in %s; fading %s's own foreground"
                + " role left FILLED's dark-mode label invisible",
            mode, variant)
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void disabledOutlinedStrokesOnSurfaceNotItsOutlineRole(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button =
        chromeOnly(ButtonVariant.OUTLINED).setShape(ButtonShape.SQUARE).setBorderWidth(8);
    button.setEnabled(false);

    final BufferedImage image = render(button);

    Pixels.assertPixelNear(
        image,
        4,
        button.getButtonSize().containerHeightPx() / 2,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContainerOpacity()),
        "#767 — the disabled outline swaps to ON_SURFACE at 12% in " + mode);
  }

  // ------------------------------------------------------------- outline

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void outlinedVariantStrokesItsOutlineRole(final Mode mode) {
    ThemeExtension.install(mode);
    // A square body puts a straight edge at mid-height, and a wide stroke gives the probe room
    // clear of the anti-aliased boundary on either side.
    final ElwhaButton button =
        chromeOnly(ButtonVariant.OUTLINED).setShape(ButtonShape.SQUARE).setBorderWidth(8);

    final BufferedImage image = render(button);

    Pixels.assertPixelNear(
        image,
        4,
        button.getButtonSize().containerHeightPx() / 2,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "OUTLINED strokes its outline role in " + mode);
  }

  @Test
  void variantsWithoutABorderRoleStrokeNothing() {
    final ElwhaButton button =
        chromeOnly(ButtonVariant.FILLED).setShape(ButtonShape.SQUARE).setBorderWidth(8);

    final BufferedImage image = render(button);

    Pixels.assertPixelNear(
        image,
        4,
        button.getButtonSize().containerHeightPx() / 2,
        ColorRole.PRIMARY.resolve(),
        "a border width on a border-less variant leaves the container fill untouched");
  }

  // --------------------------------------------------- §7 selection swaps

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void filledSwapsItsContainerAcrossTheToggle(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button =
        chromeOnly(ButtonVariant.FILLED).setInteractionMode(ButtonInteractionMode.SELECTABLE);

    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.SURFACE_CONTAINER.resolve(),
        "an unselected FILLED toggle drops to surfaceContainer in " + mode);

    button.setSelected(true);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.PRIMARY.resolve(),
        "a selected FILLED toggle swaps up to primary in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void outlinedSwapsToTheInverseSurfaceWhenSelected(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaButton button =
        chromeOnly(ButtonVariant.OUTLINED).setInteractionMode(ButtonInteractionMode.SELECTABLE);

    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.SURFACE.resolve(),
        "an unselected OUTLINED toggle stays transparent in " + mode);

    button.setSelected(true);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.INVERSE_SURFACE.resolve(),
        "a selected OUTLINED toggle fills with the inverse surface in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = ButtonVariant.class,
      names = {"ELEVATED", "FILLED_TONAL"})
  void overlaySelectionVariantsCompositeTwelvePercentInstead(final ButtonVariant variant) {
    final ElwhaButton button =
        chromeOnly(variant).setInteractionMode(ButtonInteractionMode.SELECTABLE).setSelected(true);

    final BufferedImage image = render(button);

    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        image,
        center.x,
        center.y,
        Pixels.mix(
            variant.surfaceRole().resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.SELECTED.opacity()),
        variant + " keeps its resting container and composites the selected layer over it");
  }

  @ParameterizedTest
  @EnumSource(
      value = ButtonVariant.class,
      names = {"ELEVATED", "FILLED_TONAL"})
  void overlaySelectionVariantsStrokePrimaryWhenSelected(final ButtonVariant variant) {
    // The selected stroke is pinned at 2 px regardless of the resting border width, so the probe
    // sits inside that band on a square body's straight left edge.
    final ElwhaButton button =
        chromeOnly(variant)
            .setShape(ButtonShape.SQUARE)
            .setInteractionMode(ButtonInteractionMode.SELECTABLE)
            .setSelected(true);

    final BufferedImage image = render(button);

    final Insets shadow = button.getShadowInsets();
    Pixels.assertPixelNear(
        image,
        shadow.left + 1,
        shadow.top + button.getButtonSize().containerHeightPx() / 2,
        ColorRole.PRIMARY.resolve(),
        variant + " swaps its border to primary while selected");
  }

  @Test
  void aSurfaceRoleOverrideBeatsEveryVariantDefault() {
    final ElwhaButton button =
        chromeOnly(ButtonVariant.OUTLINED).setSurfaceRole(ColorRole.TERTIARY);

    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.TERTIARY.resolve(),
        "the override fills a variant that has no resting container of its own");

    button.setSurfaceRole(null);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.SURFACE.resolve(),
        "clearing the override restores the variant default");
  }

  @Test
  void anOverrideDoesNotReachTheTextVariant() {
    final ElwhaButton button = chromeOnly(ButtonVariant.TEXT).setSurfaceRole(ColorRole.TERTIARY);

    final Point center = bodyCenter(button);
    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        ColorRole.SURFACE.resolve(),
        "TEXT has no container to override — it stays transparent");
    assertThat(button.getSurfaceRole()).as("and reports no effective container role").isNull();
  }

  // ------------------------------------------------------------ foreground

  @ParameterizedTest
  @EnumSource(ButtonVariant.class)
  void restingForegroundIsTheVariantsOwnContentRole(final ButtonVariant variant) {
    final ElwhaButton button = chromeOnly(variant);

    assertThat(button.resolveForegroundColor())
        .as("%s tints its label and icon with its declared content role", variant)
        .isEqualTo(variant.foregroundRole().resolve());
  }

  @Test
  void selectableFilledAndOutlinedRepairTheirForegroundWithTheSwappedContainer() {
    final ElwhaButton filled =
        chromeOnly(ButtonVariant.FILLED).setInteractionMode(ButtonInteractionMode.SELECTABLE);
    assertThat(filled.resolveForegroundColor())
        .as("an unselected FILLED toggle pairs against surfaceContainer")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
    filled.setSelected(true);
    assertThat(filled.resolveForegroundColor())
        .as("a selected FILLED toggle pairs against primary")
        .isEqualTo(ColorRole.ON_PRIMARY.resolve());

    final ElwhaButton outlined =
        chromeOnly(ButtonVariant.OUTLINED)
            .setInteractionMode(ButtonInteractionMode.SELECTABLE)
            .setSelected(true);
    assertThat(outlined.resolveForegroundColor())
        .as("a selected OUTLINED toggle pairs against the inverse surface")
        .isEqualTo(ColorRole.INVERSE_ON_SURFACE.resolve());
  }

  @Test
  void anOverriddenContainerRepairsTheForegroundButNeverOnText() {
    assertThat(
            chromeOnly(ButtonVariant.FILLED)
                .setSurfaceRole(ColorRole.TERTIARY)
                .resolveForegroundColor())
        .as("an overridden container re-pairs the foreground with its on-role")
        .isEqualTo(ColorRole.ON_TERTIARY.resolve());
    assertThat(
            chromeOnly(ButtonVariant.TEXT)
                .setSurfaceRole(ColorRole.TERTIARY)
                .resolveForegroundColor())
        .as("TEXT keeps its rigid primary foreground through any override")
        .isEqualTo(ColorRole.PRIMARY.resolve());
  }

  // -------------------------------------------------------------- factories

  @Test
  void everyStaticFactoryProducesTheVariantItNames() {
    assertThat(ElwhaButton.elevatedButton("x").getVariant())
        .as("elevatedButton")
        .isEqualTo(ButtonVariant.ELEVATED);
    assertThat(ElwhaButton.filledButton("x").getVariant())
        .as("filledButton")
        .isEqualTo(ButtonVariant.FILLED);
    assertThat(ElwhaButton.filledTonalButton("x").getVariant())
        .as("filledTonalButton")
        .isEqualTo(ButtonVariant.FILLED_TONAL);
    assertThat(ElwhaButton.outlinedButton("x").getVariant())
        .as("outlinedButton")
        .isEqualTo(ButtonVariant.OUTLINED);
    assertThat(ElwhaButton.textButton("x").getVariant())
        .as("textButton")
        .isEqualTo(ButtonVariant.TEXT);
    assertThat(ElwhaButton.filledButton("Save").getText())
        .as("a factory carries the label through")
        .isEqualTo("Save");
  }

  @Test
  void aReEnabledButtonPaintsNoStateLayerFromBeforeItWasDisabled() {
    final ElwhaButton button = chromeOnly(ButtonVariant.FILLED);
    final Point center = bodyCenter(button);
    button.setHovered(true);
    button.setPressed(true);

    button.setEnabled(false);
    button.setEnabled(true);

    Pixels.assertPixelNear(
        render(button),
        center.x,
        center.y,
        restingFill(ButtonVariant.FILLED),
        "disabling drops the hover and press state, so re-enabling resumes at rest instead of"
            + " replaying an overlay the pointer left behind");
  }

  @Test
  void freshButtonDefaults() {
    final ElwhaButton button = new ElwhaButton();

    assertThat(button.getVariant())
        .as("the default variant is FILLED")
        .isEqualTo(ButtonVariant.FILLED);
    assertThat(button.getInteractionMode())
        .as("the default interaction mode is CLICKABLE")
        .isEqualTo(ButtonInteractionMode.CLICKABLE);
    assertThat(button.getShape()).as("the default shape is ROUND").isEqualTo(ButtonShape.ROUND);
    assertThat(button.getButtonSize()).as("the default size tier is S").isEqualTo(ButtonSize.S);
    assertThat(button.getBorderWidth()).as("the default border width is 1").isEqualTo(1);
    assertThat(button.getText()).as("no label means the empty string, never null").isEmpty();
    assertThat(button.getIcon()).as("no icon by default").isNull();
    assertThat(button.isRippleEnabled()).as("the press ripple is on by default").isTrue();
    assertThat(button.isSelected()).as("nothing is selected by default").isFalse();
    assertThat(button.isFocusable()).as("a button is a tab stop").isTrue();
  }
}
