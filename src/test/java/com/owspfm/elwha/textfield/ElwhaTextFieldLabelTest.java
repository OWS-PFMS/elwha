package com.owspfm.elwha.textfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.InputMode;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the floating label (design §6 / §8) — the two end states, the type-scale drop
 * that accompanies the float, the horizontal anchoring through icon slots and RTL, the required
 * asterisk, the label color table, and the M3 label-notch the outlined stroke punches once the
 * label has risen onto it.
 *
 * <p>The label is asserted from <em>recorded paint calls</em>, never from glyph pixels: the
 * recorder carries the exact string, the exact baseline, the exact derived font, and the exact
 * resolved color, which is what the contract actually specifies. The notch is asserted as chrome —
 * a notched container path is literally a second sub-path, so counting sub-paths states the
 * contract with no rasterization at all.
 *
 * <p>Only the <b>populated</b> half of "focused or populated" is reachable here; the focused float
 * needs real focus ownership and lives in {@link ElwhaTextFieldGuiTest}.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTextFieldLabelTest {

  private static PaintLog.Text label(final ElwhaTextField field, final String text) {
    return PaintLog.capture(field)
        .text(text)
        .orElseThrow(() -> new AssertionError("the label '" + text + "' was never painted"));
  }

  private static int lineHeight(
      final ElwhaTextField field, final com.owspfm.elwha.theme.TypeRole role) {
    return field.getFontMetrics(role.resolve()).getHeight();
  }

  private static float restingPt() {
    return com.owspfm.elwha.theme.TypeRole.BODY_LARGE.pt();
  }

  private static float floatedPt() {
    return com.owspfm.elwha.theme.TypeRole.BODY_SMALL.pt();
  }

  // ------------------------------------------------------------ end states

  @ParameterizedTest
  @EnumSource(Variant.class)
  void anEmptyUnfocusedFieldRestsTheLabelOnTheInputBaseline(final Variant variant) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Email");

    final PaintLog.Text painted = label(field, "Email");

    assertThat(painted.font().getSize2D())
        .as("%s rests the label at the input's own type scale", variant)
        .isEqualTo(restingPt());
    field.setSize(field.getPreferredSize());
    field.doLayout();
    final FontMetrics fm =
        field.getFontMetrics(com.owspfm.elwha.theme.TypeRole.BODY_LARGE.resolve());
    assertThat(painted.y())
        .as("and on the input's baseline, so the float lifts cleanly off the text")
        .isEqualTo(field.getEditor().getY() + fm.getAscent());
  }

  @ParameterizedTest
  @EnumSource(Variant.class)
  void populatingTheFieldFloatsTheLabelAndDropsItToTheSmallScale(final Variant variant) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Email");
    final double restingY = label(field, "Email").y();

    field.setText("ada@x.io");
    final PaintLog.Text floated = label(field, "Email");

    assertThat(floated.font().getSize2D())
        .as("%s drops the floated label from BODY_LARGE to BODY_SMALL", variant)
        .isEqualTo(floatedPt());
    assertThat(floated.y()).as("and lifts it above its resting baseline").isLessThan(restingY);
  }

  @Test
  void filledFloatedLabelLandsInsideTheTopOfItsFill() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setText("ada@x.io");

    final PaintLog.Text floated = label(field, "Email");

    final FontMetrics fm = field.getFontMetrics(floated.font());
    assertThat(floated.y())
        .as("the filled label floats one padding step down from the top of the fill")
        .isEqualTo(ElwhaTextField.PAD_TOP_BOTTOM + fm.getAscent());
  }

  @Test
  void outlinedFloatedLabelStraddlesTheTopStroke() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Email");
    field.setText("ada@x.io");

    final PaintLog.Text floated = label(field, "Email");

    final FontMetrics fm = field.getFontMetrics(floated.font());
    final int containerTop = lineHeight(field, com.owspfm.elwha.theme.TypeRole.BODY_SMALL) / 2;
    assertThat(floated.y())
        .as("the outlined label centres on the stroke it notches, not below it")
        .isEqualTo(Math.round(containerTop + (fm.getAscent() - fm.getDescent()) / 2f));
  }

  @Test
  void clearingTheTextReturnsTheLabelToRest() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    final double restingY = label(field, "Email").y();

    field.setText("ada@x.io");
    field.setText("");

    assertThat(label(field, "Email").y())
        .as("emptying an unfocused field un-floats the label")
        .isEqualTo(restingY);
    assertThat(label(field, "Email").font().getSize2D())
        .as("restoring the resting type scale with it")
        .isEqualTo(restingPt());
  }

  @Test
  void aLabelLessFieldPaintsNoLabelAtAll() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setText("value");

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.texts())
        .as("the adjacent-label pattern paints nothing where the label would be")
        .noneMatch(t -> t.string().isEmpty());
  }

  @Test
  void aMultiLineFieldFloatsItsLabelFromTheFirstLine() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);
    field.setText("a note");

    assertThat(label(field, "Notes").font().getSize2D())
        .as("the label floats over a grown container exactly as it does over a single line")
        .isEqualTo(floatedPt());
  }

  // ----------------------------------------------------------- anchoring

  @Test
  void labelSharesTheInputsLeadingEdge() {
    final ElwhaTextField bare = new ElwhaTextField(Variant.FILLED, "Email");
    assertThat(label(bare, "Email").x())
        .as("a bare label starts at the 16dp text edge")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);

    final ElwhaTextField iconed = new ElwhaTextField(Variant.FILLED, "Email");
    iconed.setLeadingIcon(MaterialIcons.info(ElwhaTextField.ICON_GLYPH));
    assertThat(label(iconed, "Email").x())
        .as("and clears the leading icon slot when one is present")
        .isEqualTo(ElwhaTextField.ICON_SLOT);
  }

  @Test
  void labelMirrorsUnderRightToLeft() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    field.setText("ada@x.io");

    final PaintLog.Text floated = label(field, "Email");

    final int width = field.getFontMetrics(floated.font()).stringWidth("Email");
    assertThat(floated.x())
        .as("under RTL the label is right-anchored so its trailing edge meets the text edge")
        .isEqualTo(field.getWidth() - ElwhaTextField.PAD_LR_NO_ICON - width);
  }

  // ------------------------------------------------------------- required

  @Test
  void aRequiredFieldAppendsAnAsteriskToThePaintedLabel() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setRequired(true);

    assertThat(PaintLog.capture(field).painted("Email *"))
        .as("required marks the label with an asterisk")
        .isTrue();
  }

  @Test
  void noAsteriskKeepsTheFieldRequiredButHidesTheGlyph() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setRequired(true);
    field.setNoAsterisk(true);

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.painted("Email")).as("the bare label is painted").isTrue();
    assertThat(log.painted("Email *")).as("with no asterisk — M3 no-asterisk").isFalse();
    assertThat(field.isRequired()).as("while the field stays required").isTrue();
  }

  // ---------------------------------------------------------- label color

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void restingLabelIsOnSurfaceVariant(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    assertThat(label(field, "Email").color())
        .as("a resting label is onSurfaceVariant in " + mode)
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
  }

  @Test
  void anErroredLabelTurnsTheErrorRole() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setError(true);

    assertThat(label(field, "Email").color())
        .as("error recolors the label — and beats focus, per §4")
        .isEqualTo(ColorRole.ERROR.resolve());
  }

  @Test
  void anErroredHoveredLabelDeepensRatherThanSwitchingRole() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setError(true);
    field.setSize(field.getPreferredSize());
    Input.enter(field, 20, 20);

    assertThat(label(field, "Email").color())
        .as("hover composites over the error role rather than replacing it")
        .isEqualTo(StateLayer.HOVER.over(ColorRole.ERROR.resolve(), ColorRole.ON_SURFACE));
  }

  @Test
  void aDisabledLabelDimsToTheContentOpacity() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setEnabled(false);

    final Color base = ColorRole.ON_SURFACE.resolve();
    assertThat(label(field, "Email").color())
        .as("a disabled label is on-surface at 38%")
        .isEqualTo(
            new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                Math.round(StateLayer.disabledContentOpacity() * 255f)));
  }

  // -------------------------------------------------------------- notch

  @Test
  void outlinedStrokeIsUnbrokenWhileTheLabelRests() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Email");

    final PaintLog.Painted stroke = outlinedStroke(field);

    assertThat(stroke.subPaths())
        .as("a resting label sits inside the box, so the container stroke is one closed loop")
        .isEqualTo(1);
  }

  @Test
  void floatingTheLabelPunchesTheNotchInTheTopEdge() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Email");
    field.setText("ada@x.io");

    final PaintLog.Painted stroke = outlinedStroke(field);

    assertThat(stroke.subPaths())
        .as("the floated label breaks the top edge into two runs — the M3 label-notch")
        .isEqualTo(2);
  }

  @Test
  void notchBracketsTheFloatedLabelWithFourPixelsOfAir() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Email");
    field.setText("ada@x.io");

    final PaintLog log = PaintLog.capture(field);
    final PaintLog.Text floated = log.text("Email").orElseThrow();
    final double labelWidth = field.getFontMetrics(floated.font()).stringWidth("Email");
    final Rectangle2D gap = topEdgeGap(log);

    assertThat(gap.getMinX())
        .as("the notch opens one 4dp pad before the label")
        .isEqualTo(floated.x() - ElwhaTextField.LABEL_NOTCH_PAD);
    assertThat(gap.getMaxX())
        .as("and closes one 4dp pad after it")
        .isEqualTo(floated.x() + labelWidth + ElwhaTextField.LABEL_NOTCH_PAD);
  }

  @Test
  void aLabelLessOutlinedFieldNeverNotches() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "");
    field.setText("value");

    assertThat(outlinedStroke(field).subPaths())
        .as("with no label there is nothing to make room for")
        .isEqualTo(1);
  }

  @Test
  void notchTracksTheLeadingIconSlot() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Email");
    field.setLeadingIcon(MaterialIcons.info(ElwhaTextField.ICON_GLYPH));
    field.setText("ada@x.io");

    assertThat(topEdgeGap(PaintLog.capture(field)).getMinX())
        .as("the notch follows the label past the icon slot rather than staying at the text edge")
        .isEqualTo(ElwhaTextField.ICON_SLOT - ElwhaTextField.LABEL_NOTCH_PAD);
  }

  /** The outlined container stroke — the only stroked shape the field paints. */
  private static PaintLog.Painted outlinedStroke(final ElwhaTextField field) {
    return PaintLog.capture(field).shapes().stream()
        .filter(PaintLog.Painted::stroked)
        .findFirst()
        .orElseThrow();
  }

  /**
   * The horizontal span the container stroke skips along its top edge — derived from the two runs
   * the notch splits the path into, so it measures the chrome rather than the label's ink.
   */
  private static Rectangle2D topEdgeGap(final PaintLog log) {
    final java.awt.Shape path =
        log.shapes().stream().filter(PaintLog.Painted::stroked).findFirst().orElseThrow().shape();
    final java.awt.geom.PathIterator it = path.getPathIterator(null);
    final double[] coords = new double[6];
    double runEnd = Double.NaN;
    double gapStart = Double.NaN;
    double gapEnd = Double.NaN;
    boolean firstMove = true;
    while (!it.isDone()) {
      final int type = it.currentSegment(coords);
      if (type == java.awt.geom.PathIterator.SEG_MOVETO && !firstMove) {
        gapStart = runEnd;
        gapEnd = coords[0];
        break;
      }
      if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
        firstMove = false;
      }
      if (type == java.awt.geom.PathIterator.SEG_LINETO) {
        runEnd = coords[0];
      }
      it.next();
    }
    return new Rectangle2D.Double(gapStart, 0, gapEnd - gapStart, 1);
  }

  // --------------------------------------------------------- placeholder

  @Test
  void aLabelLessFieldShowsItsPlaceholderImmediately() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setPlaceholder("Search");

    assertThat(field.getEditor().getClientProperty("JTextField.placeholderText"))
        .as("with no label to collide with, the placeholder is the resting hint")
        .isEqualTo("Search");
  }

  @Test
  void aLabelledFieldHidesThePlaceholderBehindTheRestingLabel() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setPlaceholder("you@example.com");

    assertThat(field.getEditor().getClientProperty("JTextField.placeholderText"))
        .as("the resting label occupies the placeholder's row, so the hint waits for focus")
        .isNull();
  }

  @Test
  void aPopulatedFieldNeverShowsThePlaceholder() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setPlaceholder("Search");

    field.setText("term");

    assertThat(field.getEditor().getClientProperty("JTextField.placeholderText"))
        .as("text wins over the hint")
        .isNull();
  }

  @Test
  void placeholderSurvivesAnEditorSwap() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setPlaceholder("Search");

    field.setInputMode(InputMode.MULTI_LINE);

    assertThat(field.getEditor().getClientProperty("JTextField.placeholderText"))
        .as("the rebuilt editor is re-seeded with the placeholder")
        .isEqualTo("Search");
  }

  // ------------------------------------------------------------- affixes

  @Test
  void affixesWaitForTheLabelToFloatOutOfTheirWay() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Amount");
    field.setPrefixText("$");
    field.setSuffixText("kg");

    assertThat(PaintLog.capture(field).painted("$"))
        .as("a resting label sits where the prefix would, so the affixes stay hidden")
        .isFalse();

    field.setText("12");
    final PaintLog log = PaintLog.capture(field);
    assertThat(log.painted("$")).as("once the label floats the prefix appears").isTrue();
    assertThat(log.painted("kg")).as("and the suffix with it").isTrue();
  }

  @Test
  void aLabelLessFieldPaintsItsAffixesImmediately() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setPrefixText("$");

    assertThat(PaintLog.capture(field).painted("$"))
        .as("with no label there is nothing to wait for")
        .isTrue();
  }

  @Test
  void affixesAreOnSurfaceVariantAndDimWhenDisabled() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setPrefixText("$");
    assertThat(PaintLog.capture(field).text("$").orElseThrow().color())
        .as("affixes are onSurfaceVariant")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());

    field.setEnabled(false);
    final Color base = ColorRole.ON_SURFACE.resolve();
    assertThat(PaintLog.capture(field).text("$").orElseThrow().color())
        .as("and dim to on-surface at 38% when disabled")
        .isEqualTo(
            new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                Math.round(StateLayer.disabledContentOpacity() * 255f)));
  }

  @Test
  void affixesFlankTheInputOnTheCorrectSides() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setPrefixText("$");
    field.setSuffixText("kg");

    final PaintLog log = PaintLog.capture(field);

    assertThat(log.text("$").orElseThrow().x())
        .as("the prefix leads at the text edge")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);
    final double suffixWidth =
        field.getFontMetrics(log.text("kg").orElseThrow().font()).stringWidth("kg");
    assertThat(log.text("kg").orElseThrow().x())
        .as("and the suffix trails against the far text edge")
        .isEqualTo(field.getWidth() - ElwhaTextField.PAD_LR_NO_ICON - suffixWidth);
  }

  // ---------------------------------------------------------------- teardown

  @Test
  void aFieldReAddedWithItsLabelFloatedStillPaintsItFloated() {
    // setText before the mount: on a peered-but-unparented field the editor's accessible caret
    // hook walks to a window that is not there and NPEs — a Swing quirk, nothing to do with this.
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setText("ada@x.io");
    field.addNotify();
    final double floatedY = label(field, "Email").y();

    field.removeNotify();
    field.addNotify();

    assertThat(label(field, "Email").y())
        .as(
            "#641 — stopping labelMorph resets it to 0 while the float flag survives, and"
                + " updateLabelFloat early-returns on a matching flag, so without a resync the"
                + " label would come back painted over the field's own text")
        .isEqualTo(floatedY);
  }
}
