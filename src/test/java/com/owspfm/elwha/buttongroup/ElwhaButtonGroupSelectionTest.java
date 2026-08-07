package com.owspfm.elwha.buttongroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the {@link ElwhaButtonGroup} selection model — the three M3 modes (design doc
 * §5), the segment coercion that makes them enforceable, and the single unified event the group
 * emits however the selection changed.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonGroupSelectionTest {

  private static ElwhaButtonGroup groupOf(final String... labels) {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add(labels);
    final Dimension pref = group.getPreferredSize();
    group.setSize(pref.width, pref.height);
    group.doLayout();
    return group;
  }

  private static void clickSegment(final ElwhaButtonGroup group, final int index) {
    final java.awt.Component segment = group.getButtonAt(index);
    Input.click(segment, segment.getWidth() / 2, segment.getHeight() / 2);
  }

  // ----------------------------------------------------------- construction

  @Test
  void aFreshGroupIsAnEmptyStandardSingleSelectContainer() {
    final ElwhaButtonGroup group = new ElwhaButtonGroup();

    assertThat(group.getVariant())
        .as("the default variant is the gapped toolbar cluster")
        .isEqualTo(ButtonGroupVariant.STANDARD);
    assertThat(group.getSelectionMode())
        .as("the default selection mode is single-select")
        .isEqualTo(SelectionMode.SINGLE);
    assertThat(group.getButtonCount()).as("and it starts empty").isZero();
    assertThat(group.getSelectedIndex()).as("with no selection").isEqualTo(-1);
    assertThat(group.isFocusable())
        .as("§13 — the container is not a tab stop; each segment is its own")
        .isFalse();
  }

  @Test
  void bothFactoryPresetsProduceTheVariantTheyName() {
    assertThat(ElwhaButtonGroup.standard().getVariant())
        .as("standard()")
        .isEqualTo(ButtonGroupVariant.STANDARD);
    assertThat(ElwhaButtonGroup.connected().getVariant())
        .as("connected()")
        .isEqualTo(ButtonGroupVariant.CONNECTED);
    assertThat(new ElwhaButtonGroup(null).getVariant())
        .as("a null variant falls back to STANDARD rather than failing")
        .isEqualTo(ButtonGroupVariant.STANDARD);
  }

  // -------------------------------------------------------- segment intake

  @Test
  void everySegmentIsCoercedIntoItsPrimitivesSelectableMode() {
    final ElwhaButton label = ElwhaButton.filledTonalButton("List");
    final ElwhaIconButton icon = new ElwhaIconButton(MaterialIcons.gridView());
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add(label).add(icon);

    assertThat(label.getInteractionMode())
        .as("a label segment becomes selectable so the group can enforce its mode")
        .isEqualTo(ButtonInteractionMode.SELECTABLE);
    assertThat(icon.getInteractionMode())
        .as("and so does an icon segment")
        .isEqualTo(IconButtonInteractionMode.SELECTABLE);
    assertThat(group.getButtonCount()).as("a group may freely mix the two").isEqualTo(2);
  }

  @Test
  void anIconSegmentTakesFocusOnClickUnlikeAStandaloneIconButton() {
    final ElwhaIconButton icon = new ElwhaIconButton(MaterialIcons.gridView());
    assertThat(icon.isRequestFocusEnabled())
        .as("a standalone icon button suppresses click-focus for toolbar use")
        .isFalse();

    ElwhaButtonGroup.standard().add(icon);

    assertThat(icon.isRequestFocusEnabled())
        .as("but a segmented control expects a clicked segment to take focus")
        .isTrue();
  }

  @Test
  void aTextVariantSegmentIsRejected() {
    assertThatThrownBy(() -> ElwhaButtonGroup.standard().add(ElwhaButton.textButton("List")))
        .as("M3 excludes text buttons from button groups — they have no container treatment")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TEXT-variant");
  }

  @Test
  void nullSegmentsAndLabelsAreRejected() {
    assertThatThrownBy(() -> ElwhaButtonGroup.standard().add((ElwhaButton) null))
        .as("a null label segment")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaButtonGroup.standard().add((ElwhaIconButton) null))
        .as("a null icon segment")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaButtonGroup.standard().add((String[]) null))
        .as("a null label array")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaButtonGroup.standard().add("List", null, "Grid"))
        .as("a null label inside the array")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void labelConvenienceBuildsTonalSegments() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");

    assertThat(((ElwhaButton) group.getButtonAt(0)).getVariant())
        .as("a label-only segment defaults to the tonal colour style")
        .isEqualTo(ButtonVariant.FILLED_TONAL);
    assertThat(((ElwhaButton) group.getButtonAt(0)).getText())
        .as("and carries the label it was named with")
        .isEqualTo("List");
  }

  @Test
  void labelPlusIconConvenienceBuildsOneTonalSegment() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard().add("Compose", MaterialIcons.edit());

    final ElwhaButton segment = (ElwhaButton) group.getButtonAt(0);
    assertThat(segment.getText()).as("the label lands on the segment").isEqualTo("Compose");
    assertThat(segment.getIcon()).as("and so does the leading icon").isNotNull();
  }

  @Test
  void clearingRemovesEverySegment() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");

    group.clear();

    assertThat(group.getButtonCount()).as("clear empties the group").isZero();
    assertThat(group.getSelectedIndex()).as("and leaves no selection behind").isEqualTo(-1);
  }

  @Test
  void segmentIndicesAreBoundsChecked() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");

    assertThatThrownBy(() -> group.getButtonAt(2))
        .as("reading past the end")
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> group.setSelected(9, true))
        .as("and selecting past the end")
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  // ------------------------------------------------------------- SINGLE

  @Test
  void singleSelectAllowsAtMostOneSegment() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");

    group.setSelectedIndex(0);
    group.setSelectedIndex(2);

    assertThat(group.getSelectedIndices())
        .as("selecting one segment releases the previous one")
        .containsExactly(2);
  }

  @Test
  void singleSelectStillAllowsAnEmptySelection() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setSelectedIndex(1);

    group.setSelected(1, false);

    assertThat(group.getSelectedIndices())
        .as("SINGLE permits clearing the last selection — that is what REQUIRED forbids")
        .isEmpty();
  }

  @Test
  void clickingASegmentDrivesTheSameModelAsTheProgrammaticPath() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");

    clickSegment(group, 1);

    assertThat(group.getSelectedIndex()).as("a user click selects through the group").isEqualTo(1);

    clickSegment(group, 2);

    assertThat(group.getSelectedIndices())
        .as("and the mutex applies to pointer input exactly as to setSelected")
        .containsExactly(2);
  }

  // -------------------------------------------------------------- MULTI

  @Test
  void multiSelectHoldsAnyNumberOfSegments() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");
    group.setSelectionMode(SelectionMode.MULTI);

    group.setSelectedIndex(0);
    group.setSelectedIndex(2);

    assertThat(group.getSelectedIndices())
        .as("MULTI keeps every selected segment, in ascending order")
        .containsExactly(0, 2);
  }

  @Test
  void leavingMultiCollapsesToTheFirstSelectedSegment() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");
    group.setSelectionMode(SelectionMode.MULTI);
    group.setSelectedIndex(1);
    group.setSelectedIndex(2);

    group.setSelectionMode(SelectionMode.SINGLE);

    assertThat(group.getSelectedIndices())
        .as("switching back to single-select keeps the first selection and drops the rest")
        .containsExactly(1);
  }

  // ------------------------------------------------------------ REQUIRED

  @Test
  void aRequiredGroupSeedsItsFirstSegment() {
    final ElwhaButtonGroup group = ElwhaButtonGroup.standard();
    group.setSelectionMode(SelectionMode.REQUIRED);

    group.add("List", "Grid");

    assertThat(group.getSelectedIndex())
        .as("the exactly-one invariant holds before the user has clicked anything")
        .isZero();
  }

  @Test
  void switchingToRequiredSeedsAnUnselectedGroup() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");

    group.setSelectionMode(SelectionMode.REQUIRED);

    assertThat(group.getSelectedIndex()).as("the mode change establishes the invariant").isZero();
  }

  @Test
  void aRequiredGroupRefusesToEmptyItself() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setSelectionMode(SelectionMode.REQUIRED);

    group.setSelected(0, false);

    assertThat(group.getSelectedIndices())
        .as("deselecting the only selection is refused")
        .containsExactly(0);
  }

  @Test
  void aRequiredGroupStillMovesItsSelection() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");
    group.setSelectionMode(SelectionMode.REQUIRED);

    clickSegment(group, 2);

    assertThat(group.getSelectedIndices())
        .as("moving the selection is always allowed — only emptying it is not")
        .containsExactly(2);
  }

  @Test
  void aRequiredGroupRefusesAWholesaleClear() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setSelectionMode(SelectionMode.REQUIRED);

    assertThatThrownBy(group::clearSelection)
        .as("clearSelection cannot be reconciled with the exactly-one invariant")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("REQUIRED");
  }

  @ParameterizedTest
  @EnumSource(
      value = SelectionMode.class,
      names = {"SINGLE", "MULTI"})
  void everyOtherModeAllowsAWholesaleClear(final SelectionMode mode) {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setSelectionMode(mode);
    group.setSelectedIndex(0);
    group.setSelected(1, true);

    group.clearSelection();

    assertThat(group.getSelectedIndices()).as("%s allows an empty selection", mode).isEmpty();
  }

  // ------------------------------------------------------------- listener

  @Test
  void oneUnifiedEventIsEmittedHoweverTheSelectionChanged() {
    final ElwhaButtonGroup group = groupOf("List", "Grid", "Compact");
    final List<List<Integer>> heard = new ArrayList<>();
    group.addSelectionListener(source -> heard.add(source.getSelectedIndices()));

    group.setSelectedIndex(0);
    clickSegment(group, 1);
    group.clearSelection();

    assertThat(heard)
        .as("programmatic, pointer, and wholesale changes all report through one listener")
        .containsExactly(List.of(0), List.of(1), List.of());
  }

  @Test
  void anIdempotentChangeIsNotReported() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setSelectedIndex(0);
    final int[] fired = {0};
    group.addSelectionListener(source -> fired[0]++);

    group.setSelectedIndex(0);
    group.setSelectionMode(SelectionMode.MULTI);

    assertThat(fired[0]).as("the group only notifies when the selection actually moved").isZero();
  }

  @Test
  void aRemovedListenerHearsNothingAndANullOneIsIgnored() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    final int[] fired = {0};
    final ElwhaButtonGroup.SelectionListener listener = source -> fired[0]++;
    group.addSelectionListener(listener);
    group.addSelectionListener(null);

    group.removeSelectionListener(listener);
    group.setSelectedIndex(1);

    assertThat(fired[0]).as("removal detaches the listener").isZero();
  }

  @Test
  void selectedIndicesIsAnUnmodifiableView() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setSelectedIndex(0);

    assertThatThrownBy(() -> group.getSelectedIndices().add(1))
        .as("callers cannot mutate the group through its reported selection")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // ------------------------------------------------------------- enablement

  @Test
  void disablingTheGroupCascadesToEverySegment() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");

    group.setEnabled(false);

    assertThat(group.getButtonAt(0).isEnabled())
        .as("the first segment follows the group")
        .isFalse();
    assertThat(group.getButtonAt(1).isEnabled()).as("and so does the last").isFalse();

    group.setEnabled(true);

    assertThat(group.getButtonAt(0).isEnabled()).as("re-enabling cascades too").isTrue();
  }

  @Test
  void aDisabledGroupSelectsNothingFromPointerInput() {
    final ElwhaButtonGroup group = groupOf("List", "Grid");
    group.setEnabled(false);

    clickSegment(group, 1);

    assertThat(group.getSelectedIndex()).as("a disabled control is inert end to end").isEqualTo(-1);
  }
}
