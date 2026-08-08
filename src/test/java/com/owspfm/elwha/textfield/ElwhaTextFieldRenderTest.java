package com.owspfm.elwha.textfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A chrome coverage for {@link ElwhaTextField} — the per-variant container and active
 * indicator, the state color table of design §4 (hover deepens, error beats hover, disabled dims),
 * and the Expressive 1dp&#8594;3dp indicator bump, each against resolved roles in both modes.
 *
 * <p>Fields under test carry no label and no text, so the whole body is chrome and no probe lands
 * in a glyph box. The indicator's <i>width</i> and the outlined stroke's <i>role</i> are read from
 * the recorded paint calls rather than from pixels: a 1px anti-aliased stroke has no honest pixel
 * to sample, and the recorder carries the exact color the component asked for.
 *
 * <p><b>Focus is absent by design.</b> The focused chrome (primary indicator, 3dp stroke) is driven
 * by a real {@code FocusListener} on the embedded editor, and dispatched {@code FocusEvent}s are
 * banned as a testing idiom — that half of the state table lives in {@link ElwhaTextFieldGuiTest}.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTextFieldRenderTest {

  private static Stream<Arguments> variantsInBothModes() {
    return Stream.of(Mode.LIGHT, Mode.DARK)
        .flatMap(mode -> Stream.of(Variant.values()).map(v -> Arguments.of(v, mode)));
  }

  /** A label-free, text-free field — every painted pixel is chrome. */
  private static ElwhaTextField chromeOnly(final Variant variant) {
    return new ElwhaTextField(variant, "");
  }

  private static Dimension size(final ElwhaTextField field) {
    return field.getPreferredSize();
  }

  /** The recorded active-indicator rectangle — the filled variant's bottom bar. */
  private static Optional<PaintLog.Painted> indicator(final PaintLog log) {
    return log.shapes().stream()
        .filter(s -> !s.stroked())
        .filter(s -> s.bounds().getHeight() <= ElwhaTextField.FOCUS_STROKE)
        .filter(s -> s.bounds().getWidth() > ElwhaTextField.DEFAULT_WIDTH / 2)
        .reduce((first, second) -> second);
  }

  /** The recorded outlined container stroke. */
  private static Optional<PaintLog.Painted> outline(final PaintLog log) {
    return log.shapes().stream().filter(PaintLog.Painted::stroked).findFirst();
  }

  // ------------------------------------------------------- filled container

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void filledPaintsTheHighestSurfaceContainer(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaTextField field = chromeOnly(Variant.FILLED);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "the filled container fills surfaceContainerHighest in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void hoverCompositesTheEightPercentLayerOverTheFilledContainer(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    field.setSize(size(field));
    Input.enter(field, 20, 20);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        StateLayer.HOVER.over(ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(), ColorRole.ON_SURFACE),
        "hover composites the on-surface hover layer over the filled container in " + mode);
  }

  @Test
  void outlinedHasNoContainerFillToHover() {
    final ElwhaTextField field = chromeOnly(Variant.OUTLINED);
    field.setSize(size(field));
    Input.enter(field, 20, 20);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        ColorRole.SURFACE.resolve(),
        "the outlined container stays transparent even while hovered — §4 has no fill to tint");
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void disabledDropsTheFilledContainerToTheFourPercentOverlay(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    field.setEnabled(false);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContainerOpacity()),
        "a disabled filled container is on-surface at the disabled container opacity in " + mode);
  }

  @Test
  void disabledBeatsHoverOnTheContainer() {
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    field.setSize(size(field));
    Input.enter(field, 20, 20);
    field.setEnabled(false);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContainerOpacity()),
        "a disabled field ignores a live hover state");
  }

  // ------------------------------------------------------- active indicator

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void restingIndicatorRoleIsPerVariant(final Variant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaTextField field = chromeOnly(variant);

    final PaintLog log = PaintLog.capture(field);

    final Color want =
        (variant == Variant.FILLED ? ColorRole.ON_SURFACE_VARIANT : ColorRole.OUTLINE).resolve();
    final Optional<PaintLog.Painted> stroke =
        variant == Variant.FILLED ? indicator(log) : outline(log);
    assertThat(stroke).as("%s paints a container stroke", variant).isPresent();
    assertThat(stroke.orElseThrow().color())
        .as(
            "a resting %s stroke is %s in %s",
            variant, variant == Variant.FILLED ? "onSurfaceVariant" : "outline", mode)
        .isEqualTo(want);
  }

  @ParameterizedTest
  @EnumSource(Variant.class)
  void hoverDeepensTheIndicatorToOnSurface(final Variant variant) {
    final ElwhaTextField field = chromeOnly(variant);
    field.setSize(size(field));
    Input.enter(field, 20, 20);

    final PaintLog log = PaintLog.capture(field);

    final Optional<PaintLog.Painted> stroke =
        variant == Variant.FILLED ? indicator(log) : outline(log);
    assertThat(stroke.orElseThrow().color())
        .as("%s deepens its stroke to on-surface while hovered", variant)
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
  }

  @ParameterizedTest
  @MethodSource("variantsInBothModes")
  void errorPaintsTheIndicatorInTheErrorRole(final Variant variant, final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaTextField field = chromeOnly(variant);
    field.setError(true);

    final PaintLog log = PaintLog.capture(field);

    final Optional<PaintLog.Painted> stroke =
        variant == Variant.FILLED ? indicator(log) : outline(log);
    assertThat(stroke.orElseThrow().color())
        .as("%s strokes the error role while errored in %s", variant, mode)
        .isEqualTo(ColorRole.ERROR.resolve());
  }

  @ParameterizedTest
  @EnumSource(Variant.class)
  void errorPlusHoverDeepensTheErrorRatherThanReplacingIt(final Variant variant) {
    final ElwhaTextField field = chromeOnly(variant);
    field.setError(true);
    field.setSize(size(field));
    Input.enter(field, 20, 20);

    final PaintLog log = PaintLog.capture(field);

    final Optional<PaintLog.Painted> stroke =
        variant == Variant.FILLED ? indicator(log) : outline(log);
    assertThat(stroke.orElseThrow().color())
        .as("%s composites the hover layer over error, not on-surface — research §T5", variant)
        .isEqualTo(StateLayer.HOVER.over(ColorRole.ERROR.resolve(), ColorRole.ON_SURFACE));
  }

  @ParameterizedTest
  @EnumSource(Variant.class)
  void aDisabledIndicatorIsOnSurfaceAtTheContentOpacity(final Variant variant) {
    final ElwhaTextField field = chromeOnly(variant);
    field.setError(true);
    field.setEnabled(false);

    final PaintLog log = PaintLog.capture(field);

    final Optional<PaintLog.Painted> stroke =
        variant == Variant.FILLED ? indicator(log) : outline(log);
    final Color want = ColorRole.ON_SURFACE.resolve();
    assertThat(stroke.orElseThrow().color())
        .as("%s drops to a translucent on-surface stroke once disabled, error or not", variant)
        .isEqualTo(
            new Color(
                want.getRed(),
                want.getGreen(),
                want.getBlue(),
                Math.round(StateLayer.disabledContentOpacity() * 255f)));
  }

  @Test
  void restingIndicatorIsOneDeviceIndependentPixelTall() {
    final PaintLog log = PaintLog.capture(chromeOnly(Variant.FILLED));

    final Rectangle2D bar = indicator(log).orElseThrow().bounds();
    assertThat(bar.getHeight())
        .as("the resting active indicator is 1dp — the Expressive bump belongs to focus")
        .isEqualTo(ElwhaTextField.RESTING_STROKE);
    assertThat(bar.getMaxY())
        .as("and it sits flush with the bottom of the container box")
        .isEqualTo(ElwhaTextField.CONTAINER_HEIGHT);
  }

  @Test
  void filledContainerIsTopRoundedAndSpansTheFullWidth() {
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    final PaintLog log = PaintLog.capture(field);

    final Rectangle2D fill =
        log.shapes().stream()
            .filter(s -> !s.stroked())
            .filter(s -> s.color().equals(ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()))
            .findFirst()
            .orElseThrow()
            .bounds();
    assertThat(fill.getX()).as("the filled container starts at the left edge").isZero();
    assertThat(fill.getWidth())
        .as("and spans the whole field width")
        .isEqualTo(field.getPreferredSize().width);
    assertThat(fill.getHeight())
        .as("at the 56dp container height")
        .isEqualTo(ElwhaTextField.CONTAINER_HEIGHT);
  }

  @Test
  void outlinedStrokeIsSampleableOnItsStraightLeftEdge() {
    final ElwhaTextField field = chromeOnly(Variant.OUTLINED);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        0,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        ColorRole.OUTLINE.resolve(),
        "the outlined stroke rasterizes its outline role on the left edge");
  }

  // -------------------------------------------------------------- read-only

  @Test
  void aReadOnlyFieldKeepsTheUndimmedChrome() {
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    field.setReadOnly(true);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "read-only is not disabled — §6 keeps the normal container");
    assertThat(field.isReadOnly()).as("and the field reports itself read-only").isTrue();
    assertThat(field.isEnabled()).as("while staying enabled").isTrue();
  }

  @Test
  void hoverClearsOnExit() {
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    field.setSize(size(field));
    Input.enter(field, 20, 20);
    Input.exit(field, -5, -5);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "leaving the field drops the hover layer");
  }

  @Test
  void hoveringTheEmbeddedEditorAlsoHoversTheField() {
    final ElwhaTextField field = chromeOnly(Variant.FILLED);
    field.setSize(size(field));
    field.doLayout();
    Input.enter(field.getEditor(), 5, 5);

    final Dimension pref = size(field);
    Pixels.assertPixelNear(
        Pixels.render(field, pref.width, pref.height),
        pref.width / 2,
        ElwhaTextField.CONTAINER_HEIGHT / 2,
        StateLayer.HOVER.over(ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(), ColorRole.ON_SURFACE),
        "the editor covers most of the field, so its own hover must drive the chrome too");
  }

  // -------------------------------------------------------------- defaults

  @Test
  void freshFieldDefaults() {
    final ElwhaTextField field = new ElwhaTextField();

    assertThat(field.getVariant()).as("the default variant is FILLED").isEqualTo(Variant.FILLED);
    assertThat(field.getInputMode())
        .as("the default input mode is SINGLE_LINE")
        .isEqualTo(ElwhaTextField.InputMode.SINGLE_LINE);
    assertThat(field.isMultiline()).as("so the field is not multiline").isFalse();
    assertThat(field.getLabel()).as("no label means the empty string, never null").isEmpty();
    assertThat(field.getText()).as("and no text").isEmpty();
    assertThat(field.getPlaceholder()).as("no placeholder").isEmpty();
    assertThat(field.getPrefixText()).as("no prefix").isEmpty();
    assertThat(field.getSuffixText()).as("no suffix").isEmpty();
    assertThat(field.getSupportingText()).as("no supporting text").isEmpty();
    assertThat(field.getErrorText()).as("no error text").isEmpty();
    assertThat(field.getMaxLength()).as("no counter").isEqualTo(-1);
    assertThat(field.getSupportingTextVisibility())
        .as("supporting content is always visible by default")
        .isEqualTo(ElwhaTextField.SupportingTextVisibility.ALWAYS);
    assertThat(field.isError()).as("not errored").isFalse();
    assertThat(field.isRequired()).as("not required").isFalse();
    assertThat(field.isNoAsterisk()).as("and the asterisk is not suppressed").isFalse();
    assertThat(field.isReadOnly()).as("editable by default").isFalse();
    assertThat(field.getRows()).as("the text-area row default is 3").isEqualTo(3);
    assertThat(field.getLeadingIcon()).as("no leading icon").isNull();
    assertThat(field.getTrailingIcon()).as("no trailing icon").isNull();
    assertThat(field.getTrailingIconButton()).as("no trailing button").isNull();
  }

  @Test
  void aNullVariantOrLabelFallsBackRatherThanThrowing() {
    final ElwhaTextField field = new ElwhaTextField(null, null);

    assertThat(field.getVariant())
        .as("a null variant is treated as FILLED")
        .isEqualTo(Variant.FILLED);
    assertThat(field.getLabel()).as("a null label is treated as empty").isEmpty();

    field.setVariant(null);
    assertThat(field.getVariant()).as("and the setter agrees").isEqualTo(Variant.FILLED);
    field.setLabel(null);
    assertThat(field.getLabel()).as("as does the label setter").isEmpty();
  }

  // ------------------------------------------------------------- error glyph

  @Test
  void theHeldErrorGlyphStillRetintsAcrossAModeSwitch() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setError(true);
    final Dimension size = field.getPreferredSize();
    final int glyphX = size.width - ElwhaTextField.PAD_LR_ICON - ElwhaTextField.ICON_GLYPH / 2;
    final int glyphY = ElwhaTextField.CONTAINER_HEIGHT / 2;

    Pixels.assertAnyPixelNear(
        Pixels.render(field, size.width, size.height),
        glyphX,
        glyphY,
        ElwhaTextField.ICON_GLYPH / 2,
        ColorRole.ERROR.resolve(),
        "the auto error glyph carries the error role");

    ThemeExtension.install(Mode.DARK);

    Pixels.assertAnyPixelNear(
        Pixels.render(field, size.width, size.height),
        glyphX,
        glyphY,
        ElwhaTextField.ICON_GLYPH / 2,
        ColorRole.ERROR.resolve(),
        "#638 — the glyph is held across paints, so its tint must follow a runtime mode switch");
  }

  @Test
  void everyStaticFactoryProducesTheVariantItNames() {
    assertThat(ElwhaTextField.filled("Email").getVariant())
        .as("filled()")
        .isEqualTo(Variant.FILLED);
    assertThat(ElwhaTextField.outlined("Email").getVariant())
        .as("outlined()")
        .isEqualTo(Variant.OUTLINED);
    assertThat(ElwhaTextField.filled("Email").getLabel())
        .as("a factory carries the label through")
        .isEqualTo("Email");
  }
}
