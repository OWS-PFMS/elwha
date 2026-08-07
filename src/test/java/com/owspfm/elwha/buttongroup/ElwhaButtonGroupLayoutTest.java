package com.owspfm.elwha.buttongroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A layout coverage for {@link ElwhaButtonGroup} — the §11 measurement tables (the per-size
 * standard gap, the 2 dp connected inner padding, the XS/S 48 dp segment floor), the §12
 * hug-vs-fill width contract, and the group-wide size and colour-style stamping.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonGroupLayoutTest {

  /** The §11 connected inner padding — 2 dp at every size, both round and square forms. */
  private static final int CONNECTED_GAP = 2;

  private static ElwhaButtonGroup laidOut(final ElwhaButtonGroup group) {
    final Dimension pref = group.getPreferredSize();
    group.setSize(pref.width, pref.height);
    group.doLayout();
    return group;
  }

  private static ElwhaButtonGroup laidOutAt(final ElwhaButtonGroup group, final int width) {
    group.setSize(width, group.getPreferredSize().height);
    group.doLayout();
    return group;
  }

  private static int segmentWidth(final ElwhaButtonGroup group, final int index) {
    return group.getButtonAt(index).getWidth();
  }

  // -------------------------------------------------------------- empty

  @Test
  void anEmptyGroupOccupiesNothingAndLaysOutNothing() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected();

    group.setSize(200, 40);
    group.doLayout();

    assertThat(group.getPreferredSize())
        .as("an empty group has no footprint")
        .isEqualTo(new Dimension(0, 0));
  }

  // ------------------------------------------------------- standard gaps

  @ParameterizedTest
  @CsvSource({"XS,18", "S,12", "M,8", "L,8", "XL,8"})
  void standardSegmentsAreSeparatedByTheirTiersGap(final ButtonSize size, final int gap) {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("List", "Grid");
    group.setButtonSize(size);
    laidOut(group);

    assertThat(group.getButtonAt(1).getX() - segmentWidth(group, 0))
        .as("%s separates its segments by the §11 gap", size)
        .isEqualTo(gap);
  }

  @Test
  void aStandardGroupHugsTheSumOfItsSegmentsPlusGaps() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("List", "Grid", "Compact");
    final int content =
        group.getButtonAt(0).getPreferredSize().width
            + group.getButtonAt(1).getPreferredSize().width
            + group.getButtonAt(2).getPreferredSize().width;

    assertThat(group.getPreferredSize().width)
        .as("§12 — a standard group is content-sized, gaps included")
        .isEqualTo(content + 12 * 2);
  }

  @Test
  void eachStandardSegmentKeepsItsOwnContentWidth() {
    final ElwhaButtonGroup group = laidOut(ElwhaButtonGroup.standard().add("A", "Much longer"));

    assertThat(segmentWidth(group, 0))
        .as("a standard segment is never stretched to match its neighbor")
        .isEqualTo(group.getButtonAt(0).getPreferredSize().width);
    assertThat(segmentWidth(group, 1))
        .as("nor is the wider one shrunk")
        .isEqualTo(group.getButtonAt(1).getPreferredSize().width);
  }

  @Test
  void aStandardGroupNeverGrowsPastItsContent() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("List", "Grid");

    assertThat(group.getMaximumSize())
        .as("§12 — a standard group hugs at every window size")
        .isEqualTo(group.getPreferredSize());
  }

  // ----------------------------------------------------- connected layout

  @Test
  void connectedSegmentsAreButtedWithTheTwoPixelInnerPadding() {
    final ElwhaButtonGroup group = laidOut(ElwhaButtonGroup.connected().add("List", "Grid"));

    assertThat(group.getButtonAt(1).getX() - segmentWidth(group, 0))
        .as("§11 — connected segments carry a 2 dp inner padding at every size")
        .isEqualTo(CONNECTED_GAP);
  }

  @Test
  void connectedSegmentsShareOneWidthTakenFromTheWidestContent() {
    final ElwhaButtonGroup group = laidOut(ElwhaButtonGroup.connected().add("A", "Much longer"));

    assertThat(segmentWidth(group, 0))
        .as("a connected bar reads as one control, so every segment takes the same width")
        .isEqualTo(segmentWidth(group, 1));
    assertThat(segmentWidth(group, 0))
        .as("and that width is the widest segment's content")
        .isGreaterThanOrEqualTo(group.getButtonAt(1).getPreferredSize().width);
  }

  @Test
  void aFlexibleConnectedGroupFillsTheWidthItIsGiven() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List", "Grid", "Compact");
    assertThat(group.getResizeMode())
        .as("flexible is the default resize mode")
        .isEqualTo(ResizeMode.FLEXIBLE);

    laidOutAt(group, 500);

    assertThat(segmentWidth(group, 0))
        .as("§12 — the segments grow to share the container width")
        .isEqualTo((500 - CONNECTED_GAP * 2) / 3);
  }

  @Test
  void aFixedConnectedGroupHugsEvenInAWideContainer() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List", "Grid");
    final int hugging = laidOut(group).getButtonAt(0).getWidth();

    group.setResizeMode(ResizeMode.FIXED);
    laidOutAt(group, 500);

    assertThat(segmentWidth(group, 0))
        .as("FIXED means the caller owns the width — the group does not grow into it")
        .isEqualTo(hugging);
  }

  @Test
  void aFlexibleConnectedGroupNeverShrinksBelowItsContent() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("Much longer label", "Grid");
    final int hugging = laidOut(group).getButtonAt(0).getWidth();

    laidOutAt(group, 40);

    assertThat(segmentWidth(group, 0))
        .as("a container narrower than the content floors at the content width")
        .isEqualTo(hugging);
  }

  @Test
  void maxWidthClampBoundsAFlexibleConnectedGroup() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List", "Grid");
    group.setMaxWidth(300);

    laidOutAt(group, 900);

    assertThat(segmentWidth(group, 0))
        .as("§12 — the optional clamp keeps a wide window from stretching the bar indefinitely")
        .isEqualTo((300 - CONNECTED_GAP) / 2);
    assertThat(group.getMaximumSize().width)
        .as("and the clamp is what the group reports as its maximum")
        .isEqualTo(300);
  }

  @Test
  void anUnclampedFlexibleConnectedGroupReportsAnUnboundedMaximum() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List", "Grid");

    assertThat(group.getMaxWidth()).as("zero is the unbounded default").isZero();
    assertThat(group.getMaximumSize().width)
        .as("so the group advertises no width ceiling at all")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(group.getMaximumSize().height)
        .as("while its height stays pinned to the row")
        .isEqualTo(group.getPreferredSize().height);
  }

  @Test
  void aFixedConnectedGroupReportsItsHuggingSizeAsTheMaximum() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List", "Grid");
    group.setResizeMode(ResizeMode.FIXED);

    assertThat(group.getMaximumSize())
        .as("a fixed group has no growth to advertise")
        .isEqualTo(group.getPreferredSize());
  }

  @ParameterizedTest
  @EnumSource(
      value = ButtonSize.class,
      names = {"XS", "S"})
  void smallTiersCarryAnAccessibilityWidthFloor(final ButtonSize size) {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("A", "B");
    group.setButtonSize(size);

    laidOut(group);

    assertThat(segmentWidth(group, 0))
        .as("§13 — %s connected segments carry a 48 dp minimum width as an a11y floor", size)
        .isGreaterThanOrEqualTo(48);
  }

  @Test
  void aConnectedGroupsPreferredWidthIsSegmentsPlusInnerPadding() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List", "Grid", "Compact");
    laidOut(group);

    assertThat(group.getPreferredSize().width)
        .as("the hugging footprint is three equal segments and two inner paddings")
        .isEqualTo(segmentWidth(group, 0) * 3 + CONNECTED_GAP * 2);
  }

  @Test
  void minimumSizeTracksPreferredSizeForBothVariants() {
    assertThat(ElwhaButtonGroup.standard().add("List", "Grid").getMinimumSize())
        .as("a standard group never lays out below its content")
        .isEqualTo(ElwhaButtonGroup.standard().add("List", "Grid").getPreferredSize());
    assertThat(ElwhaButtonGroup.connected().add("List", "Grid").getMinimumSize())
        .as("nor does a connected one")
        .isEqualTo(ElwhaButtonGroup.connected().add("List", "Grid").getPreferredSize());
  }

  @Test
  void everySegmentSharesTheRowHeight() {
    final ElwhaButtonGroup group =
        laidOut(
            ElwhaButtonGroup.connected()
                .add(ElwhaButton.filledTonalButton("List"))
                .add(new ElwhaIconButton(MaterialIcons.gridView())));

    assertThat(group.getButtonAt(0).getHeight())
        .as("§9 — the group adapts to the tallest segment and lays the row out flush")
        .isEqualTo(group.getButtonAt(1).getHeight());
    assertThat(group.getButtonAt(0).getY()).as("with every segment on the same baseline").isZero();
    assertThat(group.getButtonAt(1).getY()).as("including the icon segment").isZero();
  }

  // --------------------------------------------------- group-wide stamping

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void groupSizeIsStampedOntoEverySegment(final ButtonSize size) {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.standard()
            .add(ElwhaButton.filledTonalButton("List"))
            .add(new ElwhaIconButton(MaterialIcons.gridView()));

    group.setButtonSize(size);

    assertThat(((ElwhaButton) group.getButtonAt(0)).getButtonSize())
        .as("%s reaches the label segment", size)
        .isEqualTo(size);
    assertThat(((ElwhaIconButton) group.getButtonAt(1)).getButtonSize())
        .as("%s maps onto the icon segment's own size axis", size)
        .isEqualTo(IconButtonSize.values()[size.ordinal()]);
    assertThat(group.getButtonSize()).as("and the group reports it").isEqualTo(size);
  }

  @Test
  void aSegmentAddedLaterIsStampedOnIntake() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("List");
    group.setButtonSize(ButtonSize.XL);

    group.add(ElwhaButton.filledTonalButton("Grid"));

    assertThat(((ElwhaButton) group.getButtonAt(1)).getButtonSize())
        .as("a late arrival is brought up to the group's size immediately")
        .isEqualTo(ButtonSize.XL);
  }

  @ParameterizedTest
  @EnumSource(ButtonGroupColorStyle.class)
  void aConnectedGroupStampsOneColourStyleAcrossEverySegment(final ButtonGroupColorStyle style) {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .add(ElwhaButton.elevatedButton("List"))
            .add(ElwhaButton.outlinedButton("Grid"));

    group.setColorStyle(style);

    final ButtonVariant want =
        switch (style) {
          case FILLED -> ButtonVariant.FILLED;
          case TONAL -> ButtonVariant.FILLED_TONAL;
          case OUTLINED -> ButtonVariant.OUTLINED;
        };
    assertThat(((ElwhaButton) group.getButtonAt(0)).getVariant())
        .as("§8 — mixing colour styles inside a connected group is not expressible (%s)", style)
        .isEqualTo(want);
    assertThat(((ElwhaButton) group.getButtonAt(1)).getVariant())
        .as("every segment takes the group's style (%s)", style)
        .isEqualTo(want);
  }

  @Test
  void aStandardGroupLeavesEachSegmentsColourStyleAlone() {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.standard()
            .add(ElwhaButton.elevatedButton("List"))
            .add(ElwhaButton.outlinedButton("Grid"));

    group.setColorStyle(ButtonGroupColorStyle.FILLED);

    assertThat(((ElwhaButton) group.getButtonAt(0)).getVariant())
        .as("§8 — mixing styles freely is exactly what a standard group is for")
        .isEqualTo(ButtonVariant.ELEVATED);
    assertThat(((ElwhaButton) group.getButtonAt(1)).getVariant())
        .as("so the group's colour style stays inert here")
        .isEqualTo(ButtonVariant.OUTLINED);
  }

  // ---------------------------------------------------------- setter guards

  @Test
  void nullConfigurationIsIgnoredRatherThanApplied() {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected().add("List").setButtonSize(ButtonSize.L);
    group.setResizeMode(ResizeMode.FIXED);
    group.setColorStyle(ButtonGroupColorStyle.OUTLINED);
    group.setSelectionMode(SelectionMode.MULTI);

    group.setVariant(null);
    group.setButtonSize(null);
    group.setResizeMode(null);
    group.setColorStyle(null);
    group.setSelectionMode(null);

    assertThat(group.getVariant())
        .as("a null variant is ignored")
        .isEqualTo(ButtonGroupVariant.CONNECTED);
    assertThat(group.getButtonSize()).as("a null size is ignored").isEqualTo(ButtonSize.L);
    assertThat(group.getResizeMode())
        .as("a null resize mode is ignored")
        .isEqualTo(ResizeMode.FIXED);
    assertThat(group.getColorStyle())
        .as("a null colour style is ignored")
        .isEqualTo(ButtonGroupColorStyle.OUTLINED);
    assertThat(group.getSelectionMode())
        .as("a null selection mode is ignored")
        .isEqualTo(SelectionMode.MULTI);
  }

  @Test
  void maxWidthClampIsFlooredAtZero() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.connected().add("List");

    group.setMaxWidth(-40);

    assertThat(group.getMaxWidth()).as("a negative clamp reads back as unbounded").isZero();
  }
}
