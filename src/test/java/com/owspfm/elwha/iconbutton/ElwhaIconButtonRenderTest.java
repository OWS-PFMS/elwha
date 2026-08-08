package com.owspfm.elwha.iconbutton;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A chrome coverage for {@link ElwhaIconButton} — the per-variant container and outline roles,
 * the state-layer composites, the disabled treatment, and the uniform selection model that
 * deliberately diverges from M3's per-variant surface swap (icon-button-design §10), each probed in
 * both theme modes against resolved roles.
 *
 * <p>No glyph is installed on the buttons under test, so every probe lands on chrome; content
 * colors are asserted through {@link ElwhaIconButton#resolveForegroundColor()} instead.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaIconButtonRenderTest {

  private static final int SIDE = IconButtonSize.M.containerPx();

  private static Stream<Arguments> variantsInBothModes() {
    return Stream.of(Mode.LIGHT, Mode.DARK)
        .flatMap(mode -> Stream.of(IconButtonVariant.values()).map(v -> Arguments.of(v, mode)));
  }

  private static ElwhaIconButton button(final IconButtonVariant variant) {
    return new ElwhaIconButton().setVariant(variant);
  }

  private static BufferedImage render(final ElwhaIconButton button) {
    return Pixels.render(button, SIDE, SIDE);
  }

  private static Color restingFill(final IconButtonVariant variant) {
    return variant.surfaceRole() != null
        ? variant.surfaceRole().resolve()
        : ColorRole.SURFACE.resolve();
  }

  private static ColorRole overlayBasis(final IconButtonVariant variant) {
    return variant.surfaceRole() != null ? variant.surfaceRole() : ColorRole.SURFACE;
  }

  private static Color composite(final IconButtonVariant variant, final StateLayer layer) {
    final ColorRole basis = overlayBasis(variant);
    return Pixels.mix(
        basis.resolve(), basis.on().orElse(ColorRole.ON_SURFACE).resolve(), layer.opacity());
  }

  // ------------------------------------------------------------- resting

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void everyVariantPaintsItsOwnContainerRole(final IconButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaIconButton button = button(variant);

    assertThat(button.getSurfaceRole())
        .as("%s reports the container role it paints", variant)
        .isEqualTo(variant.surfaceRole());
    Pixels.assertPixelNear(
        render(button),
        SIDE / 2,
        SIDE / 2,
        restingFill(variant),
        variant + " paints its resting container role in " + mode);
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void hoverCompositesTheEightPercentLayer(final IconButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(button(variant).setHovered(true)),
        SIDE / 2,
        SIDE / 2,
        composite(variant, StateLayer.HOVER),
        variant + " composites the hover layer over its container in " + mode);
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void pressCompositesTheTenPercentLayer(final IconButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(button(variant).setPressed(true)),
        SIDE / 2,
        SIDE / 2,
        composite(variant, StateLayer.PRESSED),
        variant + " composites the pressed layer over its container in " + mode);
  }

  @ParameterizedTest
  @EnumSource(IconButtonVariant.class)
  void pressWinsOverHover(final IconButtonVariant variant) {
    Pixels.assertPixelNear(
        render(button(variant).setHovered(true).setPressed(true)),
        SIDE / 2,
        SIDE / 2,
        composite(variant, StateLayer.PRESSED),
        variant + " paints only the pressed layer while both states are live");
  }

  // ---------------------------------------------------- uniform selection

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void selectionCompositesTheSameTwelvePercentLayerOnEveryVariant(
      final IconButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaIconButton button =
        button(variant).setInteractionMode(IconButtonInteractionMode.SELECTABLE).setSelected(true);

    Pixels.assertPixelNear(
        render(button),
        SIDE / 2,
        SIDE / 2,
        composite(variant, StateLayer.SELECTED),
        variant + " signals selection with the uniform selected layer in " + mode);
  }

  @Test
  void selectionIsInertWhileTheButtonIsMerelyClickable() {
    final ElwhaIconButton button = button(IconButtonVariant.FILLED).setSelected(true);

    assertThat(button.isSelected())
        .as("the setter is refused outright in CLICKABLE mode, so the flag never latches")
        .isFalse();
    Pixels.assertPixelNear(
        render(button),
        SIDE / 2,
        SIDE / 2,
        ColorRole.PRIMARY.resolve(),
        "and a CLICKABLE icon button paints no selected layer");
  }

  @ParameterizedTest
  @EnumSource(
      value = IconButtonVariant.class,
      names = {"FILLED", "FILLED_TONAL", "OUTLINED"})
  void nonStandardVariantsStrokePrimaryWhenSelected(final IconButtonVariant variant) {
    // A square body puts a straight edge under the probe; the wide stroke clears anti-aliasing.
    final ElwhaIconButton button =
        button(variant)
            .setShape(ShapeScale.NONE)
            .setBorderWidth(6)
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE)
            .setSelected(true);

    Pixels.assertPixelNear(
        render(button),
        3,
        SIDE / 2,
        ColorRole.PRIMARY.resolve(),
        variant + " swaps its border to primary while selected");
  }

  @Test
  void standardVariantStaysBorderlessInEveryState() {
    final ElwhaIconButton button =
        button(IconButtonVariant.STANDARD)
            .setShape(ShapeScale.NONE)
            .setBorderWidth(6)
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE)
            .setSelected(true);

    Pixels.assertPixelNear(
        render(button),
        3,
        SIDE / 2,
        composite(IconButtonVariant.STANDARD, StateLayer.SELECTED),
        "STANDARD is the text-equivalent emphasis level and never draws a border");
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void outlinedVariantStrokesItsOutlineRoleAtRest(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaIconButton button =
        button(IconButtonVariant.OUTLINED).setShape(ShapeScale.NONE).setBorderWidth(6);

    Pixels.assertPixelNear(
        render(button),
        3,
        SIDE / 2,
        ColorRole.OUTLINE.resolve(),
        "OUTLINED strokes its outline role in " + mode);
  }

  // ------------------------------------------------------------ disabled

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void disabledDropsTheContainerToTwelvePercent(final IconButtonVariant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaIconButton button = button(variant).setHovered(true).setPressed(true);
    button.setEnabled(false);

    final Color want =
        variant.surfaceRole() == null
            ? ColorRole.SURFACE.resolve()
            : Pixels.mix(
                ColorRole.SURFACE.resolve(),
                variant.surfaceRole().resolve(),
                StateLayer.disabledContainerOpacity());
    Pixels.assertPixelNear(
        render(button),
        SIDE / 2,
        SIDE / 2,
        want,
        variant + " freezes into the disabled treatment and drops every state layer in " + mode);
  }

  // ------------------------------------------------------------ foreground

  @ParameterizedTest
  @EnumSource(IconButtonVariant.class)
  void foregroundIsAlwaysTheOnPairOfTheEffectiveContainer(final IconButtonVariant variant) {
    final ElwhaIconButton button = button(variant);

    final ColorRole surface = variant.surfaceRole();
    assertThat(button.resolveForegroundColor())
        .as("%s derives its glyph tint rather than storing one", variant)
        .isEqualTo(
            surface == null
                ? ColorRole.ON_SURFACE_VARIANT.resolve()
                : surface.on().orElse(ColorRole.ON_SURFACE_VARIANT).resolve());
  }

  @Test
  void aSelectedStandardButtonTintsItsGlyphPrimary() {
    final ElwhaIconButton button =
        button(IconButtonVariant.STANDARD).setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    assertThat(button.resolveForegroundColor())
        .as("an unselected STANDARD button uses the neutral content role")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());

    button.setSelected(true);

    assertThat(button.resolveForegroundColor())
        .as("with no fill and no border, the primary tint is STANDARD's only selection signal")
        .isEqualTo(ColorRole.PRIMARY.resolve());
  }

  @Test
  void aSurfaceRoleOverrideDrivesBothTheFillAndTheGlyphTint() {
    final ElwhaIconButton button =
        button(IconButtonVariant.STANDARD).setSurfaceRole(ColorRole.TERTIARY_CONTAINER);

    Pixels.assertPixelNear(
        render(button),
        SIDE / 2,
        SIDE / 2,
        ColorRole.TERTIARY_CONTAINER.resolve(),
        "the override fills a variant that has no container of its own");
    assertThat(button.resolveForegroundColor())
        .as("and the glyph re-pairs against it")
        .isEqualTo(ColorRole.ON_TERTIARY_CONTAINER.resolve());

    button.setSurfaceRole(null);
    assertThat(button.getSurfaceRole())
        .as("clearing the override restores the variant default")
        .isEqualTo(IconButtonVariant.STANDARD.surfaceRole());
  }

  // -------------------------------------------------------------- factories

  @Test
  void everyStaticFactoryProducesTheVariantItNames() {
    assertThat(ElwhaIconButton.filledIconButton(null).getVariant())
        .as("filledIconButton")
        .isEqualTo(IconButtonVariant.FILLED);
    assertThat(ElwhaIconButton.filledTonalIconButton(null).getVariant())
        .as("filledTonalIconButton")
        .isEqualTo(IconButtonVariant.FILLED_TONAL);
    assertThat(ElwhaIconButton.outlinedIconButton(null).getVariant())
        .as("outlinedIconButton")
        .isEqualTo(IconButtonVariant.OUTLINED);
    assertThat(ElwhaIconButton.standardIconButton(null).getVariant())
        .as("standardIconButton")
        .isEqualTo(IconButtonVariant.STANDARD);
  }

  @Test
  void freshIconButtonDefaults() {
    final ElwhaIconButton button = new ElwhaIconButton();

    assertThat(button.getVariant())
        .as("the default variant is STANDARD")
        .isEqualTo(IconButtonVariant.STANDARD);
    assertThat(button.getInteractionMode())
        .as("the default interaction mode is CLICKABLE")
        .isEqualTo(IconButtonInteractionMode.CLICKABLE);
    assertThat(button.getShape()).as("the default shape is the capsule").isEqualTo(ShapeScale.FULL);
    assertThat(button.getButtonSize()).as("the default size tier is M").isEqualTo(IconButtonSize.M);
    assertThat(button.getBorderWidth()).as("the default border width is 1").isEqualTo(1);
    assertThat(button.isRippleEnabled()).as("the press ripple is on by default").isTrue();
    assertThat(button.isFocusable()).as("an icon button is a tab stop").isTrue();
    assertThat(button.isRequestFocusEnabled())
        .as("but a click does not pull focus — the toolbar default from the constructor")
        .isFalse();
  }
}
