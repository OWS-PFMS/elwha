package com.owspfm.elwha.textfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.SupportingTextVisibility;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the reserved supporting row (design §7 / the S7 build outcome) — the
 * supporting text, the live character counter, the error-replaces-supporting rule, and the {@link
 * SupportingTextVisibility} gate with its two documented overrides (error text and an over-limit
 * counter always show).
 *
 * <p>Row content is read from recorded {@code drawString} calls rather than by counting ink in a
 * band: "the counter painted the string 5/20 in the error role" is the contract, and stating it
 * that way removes the host font stack from the assertion entirely.
 *
 * <p>{@code ON_FOCUS}'s <b>revealed</b> half needs real focus and lives in {@link
 * ElwhaTextFieldGuiTest}; everything asserted here is the blurred half, which is where the override
 * rules actually bite.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTextFieldSupportingTextTest {

  private static ElwhaTextField supported(final String text) {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setSupportingText(text);
    return field;
  }

  private static Color disabledContent() {
    final Color base = ColorRole.ON_SURFACE.resolve();
    return new Color(
        base.getRed(),
        base.getGreen(),
        base.getBlue(),
        Math.round(StateLayer.disabledContentOpacity() * 255f));
  }

  // ------------------------------------------------------- supporting text

  @ParameterizedTest
  @EnumSource(Variant.class)
  void supportingTextPaintsOnTheReservedRowsBaseline(final Variant variant) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Label");
    field.setSupportingText("Helper text");

    final PaintLog.Text painted = PaintLog.capture(field).text("Helper text").orElseThrow();

    final int containerTop =
        variant == Variant.OUTLINED
            ? field.getFontMetrics(TypeRole.BODY_SMALL.resolve()).getHeight() / 2
            : 0;
    assertThat(painted.y())
        .as("%s drops the supporting row one 4dp pad below the container box", variant)
        .isEqualTo(
            containerTop
                + ElwhaTextField.CONTAINER_HEIGHT
                + ElwhaTextField.SUPPORTING_TOP_PAD
                + field.getFontMetrics(TypeRole.BODY_SMALL.resolve()).getAscent());
    assertThat(painted.x())
        .as("and starts at the 16dp text edge regardless of any icon slot")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);
    assertThat(painted.font().getSize2D())
        .as("in the BODY_SMALL scale")
        .isEqualTo((float) TypeRole.BODY_SMALL.pt());
  }

  @Test
  void supportingTextIsOnSurfaceVariantAndDimsWhenDisabled() {
    final ElwhaTextField field = supported("Helper text");
    assertThat(PaintLog.capture(field).text("Helper text").orElseThrow().color())
        .as("supporting text is onSurfaceVariant")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());

    field.setEnabled(false);
    assertThat(PaintLog.capture(field).text("Helper text").orElseThrow().color())
        .as("and dims to on-surface at 38% when disabled")
        .isEqualTo(disabledContent());
  }

  @Test
  void errorTextReplacesTheSupportingLineRatherThanStackingBelowIt() {
    final ElwhaTextField field = supported("Helper text");
    field.setErrorText("Enter a valid address");
    field.setError(true);

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.painted("Enter a valid address")).as("the error text takes the row").isTrue();
    assertThat(log.painted("Helper text"))
        .as("and the supporting text steps aside — one row, never two")
        .isFalse();
  }

  @Test
  void erroringWithNoErrorTextLeavesTheSupportingTextInPlaceButRecolored() {
    final ElwhaTextField field = supported("Helper text");
    field.setError(true);

    assertThat(PaintLog.capture(field).text("Helper text").orElseThrow().color())
        .as("with nothing to swap in, the supporting text carries the error role itself")
        .isEqualTo(ColorRole.ERROR.resolve());
  }

  @Test
  void clearingTheErrorRestoresTheSupportingText() {
    final ElwhaTextField field = supported("Helper text");
    field.setErrorText("Required");
    field.setError(true);
    field.setError(false);

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.painted("Helper text")).as("the supporting text comes back").isTrue();
    assertThat(log.painted("Required")).as("and the error text stands down").isFalse();
  }

  @Test
  void aNullSupportingOrErrorTextIsNormalizedToEmpty() {
    final ElwhaTextField field = supported("Helper text");

    field.setSupportingText(null);
    field.setErrorText(null);

    assertThat(field.getSupportingText()).as("a null supporting text reads back empty").isEmpty();
    assertThat(field.getErrorText()).as("as does a null error text").isEmpty();
  }

  // --------------------------------------------------------- visibility

  @Test
  void alwaysShowsTheSupportingTextOnABlurredField() {
    assertThat(PaintLog.capture(supported("Helper text")).painted("Helper text"))
        .as("ALWAYS is the default and paints the row unconditionally")
        .isTrue();
  }

  @Test
  void onFocusLeavesTheReservedRowBlankWhileBlurred() {
    final ElwhaTextField field = supported("Helper text");
    field.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);

    assertThat(PaintLog.capture(field).painted("Helper text"))
        .as("ON_FOCUS hides advisory content until the field is focused")
        .isFalse();
  }

  @Test
  void onFocusStillReservesTheRowHeightSoNothingShifts() {
    final ElwhaTextField always = supported("Helper text");
    final ElwhaTextField onFocus = supported("Helper text");
    onFocus.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);

    assertThat(onFocus.getPreferredSize())
        .as("the visibility mode changes the row's content, never its height")
        .isEqualTo(always.getPreferredSize());
  }

  @Test
  void errorTextOverridesOnFocusBecauseItIsTheHigherPriorityMessage() {
    final ElwhaTextField field = supported("Helper text");
    field.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    field.setErrorText("Required");
    field.setError(true);

    assertThat(PaintLog.capture(field).painted("Required"))
        .as("an error is never hidden by a visibility preference")
        .isTrue();
  }

  @Test
  void aNullVisibilityModeFallsBackToAlways() {
    final ElwhaTextField field = supported("Helper text");
    field.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);

    field.setSupportingTextVisibility(null);

    assertThat(field.getSupportingTextVisibility())
        .as("a null mode is treated as ALWAYS")
        .isEqualTo(SupportingTextVisibility.ALWAYS);
  }

  // ------------------------------------------------------------- counter

  @Test
  void noCounterIsPaintedWithoutAMaxLength() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setText("abc");

    assertThat(PaintLog.capture(field).texts())
        .as("-1 is the no-counter sentinel")
        .noneMatch(t -> t.string().contains("/"));
  }

  @Test
  void theCounterReportsTheLiveDocumentLengthOverTheLimit() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setMaxLength(20);
    field.setText("abc");

    assertThat(PaintLog.capture(field).painted("3/20"))
        .as("the counter reads used/total straight off the document")
        .isTrue();

    field.setText("abcdef");
    assertThat(PaintLog.capture(field).painted("6/20"))
        .as("and follows every text change")
        .isTrue();
  }

  @Test
  void theCounterIsRightAlignedInTheSupportingRow() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setMaxLength(20);
    field.setText("abc");

    final PaintLog.Text counter = PaintLog.capture(field).text("3/20").orElseThrow();

    final int width = field.getFontMetrics(TypeRole.BODY_SMALL.resolve()).stringWidth("3/20");
    assertThat(counter.x())
        .as("the counter hangs off the trailing text edge")
        .isEqualTo(field.getWidth() - ElwhaTextField.PAD_LR_NO_ICON - width);
  }

  @Test
  void theCounterSharesTheSupportingTextsBaseline() {
    final ElwhaTextField field = supported("Helper text");
    field.setMaxLength(20);
    field.setText("abc");

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.text("3/20").orElseThrow().y())
        .as("counter and supporting text share one reserved row")
        .isEqualTo(log.text("Helper text").orElseThrow().y());
  }

  @Test
  void anUnderLimitCounterUsesTheSupportingRole() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setMaxLength(20);
    field.setText("abc");

    assertThat(PaintLog.capture(field).text("3/20").orElseThrow().color())
        .as("an in-limit counter is just supporting content")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
  }

  @Test
  void anOverLimitCounterTurnsErrorWithoutTruncatingTheInput() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setMaxLength(3);
    field.setText("abcdef");

    assertThat(PaintLog.capture(field).text("6/3").orElseThrow().color())
        .as("over the limit the counter is the error cue")
        .isEqualTo(ColorRole.ERROR.resolve());
    assertThat(field.getText())
        .as("but the counter is display-only — a hard cap is the consumer's Document")
        .isEqualTo("abcdef");
  }

  @Test
  void anOverLimitCounterOverridesOnFocus() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    field.setMaxLength(3);
    field.setText("abcdef");

    assertThat(PaintLog.capture(field).painted("6/3"))
        .as("an over-limit counter is error-priority, so ON_FOCUS cannot hide it")
        .isTrue();
  }

  @Test
  void anInLimitCounterStaysHiddenUnderOnFocus() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setSupportingTextVisibility(SupportingTextVisibility.ON_FOCUS);
    field.setMaxLength(20);
    field.setText("abc");

    assertThat(PaintLog.capture(field).painted("3/20"))
        .as("an in-limit counter is advisory, so ON_FOCUS gates it like the supporting text")
        .isFalse();
  }

  @Test
  void aDisabledFieldDropsTheOverLimitErrorCue() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setMaxLength(3);
    field.setText("abcdef");
    field.setEnabled(false);

    assertThat(PaintLog.capture(field).text("6/3").orElseThrow().color())
        .as("a disabled field cannot be corrected, so it states the count without alarming")
        .isEqualTo(disabledContent());
  }

  @Test
  void aZeroMaxLengthStillCounts() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setMaxLength(0);

    assertThat(PaintLog.capture(field).painted("0/0"))
        .as("zero is a limit, not the no-counter sentinel")
        .isTrue();
  }

  @Test
  void theCounterAndSupportingTextCoexistOnTheSameRow() {
    final ElwhaTextField field = supported("Helper text");
    field.setMaxLength(20);
    field.setText("abc");

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.painted("Helper text")).as("the supporting text leads the row").isTrue();
    assertThat(log.painted("3/20")).as("and the counter trails it").isTrue();
    assertThat(log.text("3/20").orElseThrow().x())
        .as("with the counter to the right of the supporting text")
        .isGreaterThan(log.text("Helper text").orElseThrow().x());
  }
}
