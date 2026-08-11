package com.owspfm.elwha.buttongroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of the connected variant's positional corner treatment (design doc §11) and of
 * the standard variant's resting shape contract.
 *
 * <p>The radii the group installs on each segment are asserted exactly against the §11 table, and
 * one pixel-probe pair proves those radii actually reach paint: a first segment's outward corner is
 * empty (pill) while its butted inner corner is filled (nearly square) in the same frame.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonGroupShapeTest {

  /** Diagonal inset from a corner: inside a 20 px arc, well outside a 68 px one. */
  private static final int CORNER_PROBE = 9;

  private static final int WIDE = 900;

  private static ElwhaButtonGroup connected(final ButtonSize size, final String... labels) {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add(labels);
    group.setButtonSize(size);
    return group;
  }

  private static CornerRadii radiiOf(final ElwhaButtonGroup group, final int index) {
    return ((ElwhaButton) group.getButtonAt(index)).getCornerRadii();
  }

  private static int pillOf(final ElwhaButtonGroup group) {
    return group.getPreferredSize().height / 2;
  }

  // ----------------------------------------------------- §11 corner table

  @ParameterizedTest
  @CsvSource({"XS,4", "S,8", "M,8", "L,16", "XL,20"})
  void aButtedInnerCornerTakesItsTiersRadius(final ButtonSize size, final int inner) {
    final ElwhaButtonGroup group = connected(size, "List", "Grid", "Compact");
    final int pill = pillOf(group);

    assertThat(radiiOf(group, 1))
        .as("%s — a middle segment is butted on both sides and stays nearly square", size)
        .isEqualTo(CornerRadii.uniform(inner));
    assertThat(radiiOf(group, 0))
        .as("%s — the leading segment keeps the bar's rounded outer end", size)
        .isEqualTo(CornerRadii.of(pill, inner, inner, pill));
    assertThat(radiiOf(group, 2))
        .as("%s — and the trailing segment keeps the other one", size)
        .isEqualTo(CornerRadii.of(inner, pill, pill, inner));
  }

  @Test
  void aLoneConnectedSegmentIsAPillOnBothEnds() {
    final ElwhaButtonGroup group = connected(ButtonSize.XL, "List");

    assertThat(radiiOf(group, 0))
        .as("with nothing butted against it, a single segment is the whole bar")
        .isEqualTo(CornerRadii.uniform(pillOf(group)));
  }

  @Test
  void aSelectedSegmentPopsToAUniformPillWhateverItsPosition() {
    final ElwhaButtonGroup group = connected(ButtonSize.XL, "List", "Grid", "Compact");
    final int pill = pillOf(group);

    group.setSelectedIndex(1);

    assertThat(radiiOf(group, 1))
        .as("the selected middle segment pops out of the bar as a rounded pill")
        .isEqualTo(CornerRadii.uniform(pill));
    assertThat(radiiOf(group, 0))
        .as("while its neighbors keep the segmented-bar treatment")
        .isEqualTo(CornerRadii.of(pill, 20, 20, pill));
  }

  @Test
  void deselectingReturnsASegmentToItsPositionalTreatment() {
    final ElwhaButtonGroup group = connected(ButtonSize.XL, "List", "Grid", "Compact");
    final int pill = pillOf(group);
    group.setSelectedIndex(0);

    group.setSelectedIndex(2);

    assertThat(radiiOf(group, 0))
        .as("the released segment falls back into the bar")
        .isEqualTo(CornerRadii.of(pill, 20, 20, pill));
    assertThat(radiiOf(group, 2))
        .as("and the newly selected one pops")
        .isEqualTo(CornerRadii.uniform(pill));
  }

  @Test
  void anIconSegmentTakesTheSameConnectedTreatment() {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .add(ElwhaButton.filledTonalButton("List"))
            .add(new ElwhaIconButton(MaterialIcons.gridView()));
    group.setButtonSize(ButtonSize.XL);
    final int pill = pillOf(group);

    assertThat(((ElwhaIconButton) group.getButtonAt(1)).getCornerRadii())
        .as("a mixed group butts label and icon segments into one continuous bar")
        .isEqualTo(CornerRadii.of(20, pill, pill, 20));
  }

  // ----------------------------------------------- the treatment in paint

  @Test
  void anOutwardCornerIsRoundedWhileTheButtedOneIsNot() {
    final ElwhaButtonGroup group = connected(ButtonSize.XL, "List", "Grid", "Compact");
    final BufferedImage image = Pixels.render(group, WIDE, group.getPreferredSize().height);
    final int segment = group.getButtonAt(0).getWidth();

    Pixels.assertPixelNear(
        image,
        CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.SURFACE.resolve(),
        "the bar's leading end-cap is fully rounded, so its outer corner paints nothing");
    Pixels.assertPixelNear(
        image,
        segment - 1 - CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.SECONDARY_CONTAINER.resolve(),
        "while the butted inner corner is nearly square and reaches into the corner");
  }

  @Test
  void aSelectedSegmentsInnerCornerRoundsOutInPaintToo() {
    final ElwhaButtonGroup group = connected(ButtonSize.XL, "List", "Grid", "Compact");
    group.setSelectedIndex(0);
    final BufferedImage image = Pixels.render(group, WIDE, group.getPreferredSize().height);
    final int segment = group.getButtonAt(0).getWidth();

    Pixels.assertPixelNear(
        image,
        segment - 1 - CORNER_PROBE,
        CORNER_PROBE,
        ColorRole.SURFACE.resolve(),
        "selection pops the segment to a pill, so its formerly square inner corner empties out");
  }

  // -------------------------------------------------- standard treatment

  @Test
  void aStandardGroupInstallsNoPerCornerOverrideAtAll() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("List", "Grid");
    group.setSelectedIndex(0);

    assertThat(radiiOf(group, 0))
        .as("§4 — a standard group's segments keep their own shape, so no override is stamped")
        .isNull();
    assertThat(radiiOf(group, 1)).as("on any segment").isNull();
  }

  @Test
  void aStandardLabelSegmentRestsRoundAndLetsItsOwnFlipSignalSelection() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("List", "Grid");

    group.setSelectedIndex(0);

    assertThat(((ElwhaButton) group.getButtonAt(0)).getShape())
        .as("the group only pins the resting shape — the button's own select-flip does the rest")
        .isEqualTo(ButtonShape.ROUND);
    assertThat(((ElwhaButton) group.getButtonAt(0)).isSelected())
        .as("and the flip is driven from the segment's own selected state")
        .isTrue();
  }

  @Test
  void aStandardIconSegmentStillTakesTheStaticShapeInversion() {
    final ElwhaIconButton icon = new ElwhaIconButton(MaterialIcons.gridView());
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.standard().add(ElwhaButton.filledTonalButton("List")).add(icon);

    assertThat(icon.getShape())
        .as("an unselected icon segment rests round")
        .isEqualTo(ShapeScale.FULL);

    group.setSelectedIndex(1);

    assertThat(icon.getShape())
        .as("§10 — selection takes the opposite shape; the icon side applies it statically")
        .isEqualTo(ShapeScale.SM);
  }

  @Test
  void switchingToStandardClearsTheConnectedOverride() {
    final ElwhaButtonGroup group = connected(ButtonSize.XL, "List", "Grid");
    assertThat(radiiOf(group, 0)).as("a connected group stamps an override").isNotNull();

    group.setVariant(ButtonGroupVariant.STANDARD);

    assertThat(radiiOf(group, 0))
        .as("switching variant releases the segments back to their own shape")
        .isNull();
  }
}
