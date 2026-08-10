package com.owspfm.elwha.buttongroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the §6 standard-group width ripple — the group's job of pushing a decaying
 * borrow factor out from a pressed segment to its neighbors (1.0 at the source, 0.3 at ±1, 0.1 at
 * ±2, nothing beyond).
 *
 * <p>Reduced motion is pinned by the fixture, so each segment's morph settles the instant the group
 * starts it and {@code currentWidthBorrow()} reads back exactly the factor that was dispatched —
 * the ripple's shape is asserted without sampling a single animation frame.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonGroupRippleTest {

  private static final float EXACT = 1e-6f;

  private static ElwhaButtonGroup laidOut(final ElwhaButtonGroup group) {
    final Dimension pref = group.getPreferredSize();
    group.setSize(pref.width, pref.height);
    group.doLayout();
    return group;
  }

  private static ElwhaButtonGroup fiveSegments(final ButtonGroupVariant variant) {
    return laidOut(new ElwhaButtonGroup(variant).add("One", "Two", "Three", "Four", "Five"));
  }

  private static void press(final ElwhaButtonGroup group, final int index) {
    final Component segment = group.getButtonAt(index);
    Input.press(segment, segment.getWidth() / 2, segment.getHeight() / 2);
  }

  private static void release(final ElwhaButtonGroup group, final int index) {
    final Component segment = group.getButtonAt(index);
    Input.release(segment, segment.getWidth() / 2, segment.getHeight() / 2);
  }

  private static float borrow(final ElwhaButtonGroup group, final int index) {
    return ((ElwhaButton) group.getButtonAt(index)).currentWidthBorrow();
  }

  private static void assertBorrows(final ElwhaButtonGroup group, final float... want) {
    for (int i = 0; i < want.length; i++) {
      assertThat(borrow(group, i))
          .as("segment %d borrows %.1f of the press-width delta", i, want[i])
          .isEqualTo(want[i], within(EXACT));
    }
  }

  // ------------------------------------------------------------ dispatch

  @Test
  void aRestingGroupBorrowsNothingAnywhere() {
    assertBorrows(fiveSegments(ButtonGroupVariant.STANDARD), 0f, 0f, 0f, 0f, 0f);
  }

  @Test
  void aPressInTheMiddleRipplesOutSymmetrically() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);

    press(group, 2);

    assertBorrows(group, 0.1f, 0.3f, 1.0f, 0.3f, 0.1f);
  }

  @Test
  void aPressAtTheEndRipplesOnlyInward() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);

    press(group, 0);

    assertBorrows(group, 1.0f, 0.3f, 0.1f, 0f, 0f);
  }

  @Test
  void segmentsBeyondTheDecayTableAreLeftAlone() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);

    press(group, 4);

    assertThat(borrow(group, 1))
        .as("three segments out is past the end of the decay table")
        .isEqualTo(0f);
    assertThat(borrow(group, 0)).as("and so is four").isEqualTo(0f);
  }

  @Test
  void pressedSegmentTakesTheFullPinchItself() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);

    press(group, 1);

    assertThat(borrow(group, 1))
        .as(
            "a standard segment is selectable, so its own press morph is suppressed and the"
                + " group owns the whole width signal")
        .isEqualTo(1.0f, within(EXACT));
  }

  @Test
  void releasingUnwindsTheWholeRipple() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);
    press(group, 2);

    release(group, 2);

    assertBorrows(group, 0f, 0f, 0f, 0f, 0f);
  }

  @Test
  void aSecondPressRetargetsTheRippleRatherThanStacking() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);
    press(group, 0);
    release(group, 0);

    press(group, 4);

    assertBorrows(group, 0f, 0f, 0.1f, 0.3f, 1.0f);
  }

  // ------------------------------------------------------- variant scope

  @Test
  void aConnectedGroupIsExcludedFromTheWidthRipple() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.CONNECTED);

    press(group, 2);

    assertBorrows(group, 0f, 0f, 0f, 0f, 0f);
  }

  @Test
  void switchingToConnectedStopsFutureRipples() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);

    group.setVariant(ButtonGroupVariant.CONNECTED);
    laidOut(group);
    press(group, 2);

    assertBorrows(group, 0f, 0f, 0f, 0f, 0f);
  }

  // -------------------------------------------------------- mixed groups

  @Test
  void anIconSegmentNeitherSourcesNorReceivesTheRipple() {
    final ElwhaButtonGroup group =
        laidOut(
            ElwhaButtonGroup.standard()
                .add(ElwhaButton.filledTonalButton("One"))
                .add(new ElwhaIconButton(MaterialIcons.gridView()))
                .add(ElwhaButton.filledTonalButton("Three")));

    press(group, 1);

    assertThat(borrow(group, 0))
        .as("an icon segment is not morph-wired, so pressing it starts no ripple at all")
        .isEqualTo(0f);
    assertThat(borrow(group, 2)).as("on either side").isEqualTo(0f);
  }

  @Test
  void aLabelSegmentStillRipplesPastAnIconNeighbor() {
    final ElwhaButtonGroup group =
        laidOut(
            ElwhaButtonGroup.standard()
                .add(ElwhaButton.filledTonalButton("One"))
                .add(new ElwhaIconButton(MaterialIcons.gridView()))
                .add(ElwhaButton.filledTonalButton("Three")));

    press(group, 0);

    assertThat(borrow(group, 0))
        .as("the label press source pinches fully")
        .isEqualTo(1.0f, within(EXACT));
    assertThat(borrow(group, 2))
        .as("and the decay counts positions, so a non-morphing neighbor does not absorb it")
        .isEqualTo(0.1f, within(EXACT));
  }

  @Test
  void aDisabledGroupStartsNoRipple() {
    final ElwhaButtonGroup group = fiveSegments(ButtonGroupVariant.STANDARD);
    group.setEnabled(false);

    press(group, 2);

    assertBorrows(group, 0f, 0f, 0f, 0f, 0f);
  }
}
