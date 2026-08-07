package com.owspfm.elwha.selectfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaSelectField}'s typed value model — the single-select seam, the
 * option-order invariant the multi-select maintains, the summary rendering, and the change-listener
 * contract (fires on real changes, never on no-ops, never in the wrong mode).
 *
 * <p>Selection is driven through the package-private {@code selectIndex} / {@code toggleIndex}
 * seams, which are the same paths a real menu pick funnels through — opening the popup itself needs
 * a window and belongs to the display-bearing tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSelectFieldModelTest {

  private static final List<String> PLANETS = List.of("Mercury", "Venus", "Earth", "Mars");

  private static ElwhaSelectField<String> planets() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    return select;
  }

  // ------------------------------------------------------------- options

  @Test
  void aFreshSelectHasNoOptionsAndNoSelection() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");

    assertThat(select.getSelectedValue()).as("nothing is selected").isNull();
    assertThat(select.getSelectedValues()).as("and the list form agrees, never null").isEmpty();
    assertThat(select.getText()).as("so the field is empty").isEmpty();
    assertThat(select.isExpanded()).as("and closed").isFalse();
    assertThat(select.isEditable()).as("the default is the pure select").isFalse();
    assertThat(select.isMultiSelect()).as("and single-valued").isFalse();
    assertThat(select.isFreeTextAllowed()).as("with free text off").isFalse();
    assertThat(select.getSummaryLimit()).as("and the summary threshold at 3").isEqualTo(3);
  }

  @Test
  void nullOptionsAreTreatedAsNone() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Earth");

    select.setOptions(null);
    select.setSelectedValue("Earth");

    assertThat(select.getSelectedValue())
        .as("with the options gone, nothing is selectable — a null list is not a crash")
        .isEqualTo("Earth");
  }

  @Test
  void clearingTheValueOfANeverPopulatedSelectDoesNotThrow() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");

    select.setSelectedValue(null);

    assertThat(select.getSelectedValue())
        .as(
            "a form that resets its fields before the options load must not blow up — ElwhaMenu"
                + " rejects an empty item list, so the selection seam cannot build one")
        .isNull();
    assertThat(select.getText()).as("and the field is simply empty").isEmpty();
  }

  @Test
  void clearingTheValueAfterTheOptionsAreEmptiedDoesNotThrow() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Earth");

    select.setOptions(List.of());
    select.setSelectedValue(null);

    assertThat(select.getSelectedValue()).as("the same guard covers an emptied select").isNull();
    assertThat(select.getText()).as("clearing the value clears the field text").isEmpty();
  }

  @Test
  void optionListIsCopiedDefensively() {
    final List<String> mutable = new ArrayList<>(PLANETS);
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(mutable);

    mutable.clear();

    assertThat(select.optionsMenu().getItems())
        .as("mutating the caller's list must not reach into the select")
        .hasSize(4);
  }

  @Test
  void displayFunctionRendersTheFieldTextAndTheMenuLabels() {
    final ElwhaSelectField<String> select = planets();
    select.setDisplayFunction(String::toUpperCase);

    select.setSelectedValue("Earth");

    assertThat(select.getText())
        .as("the field shows the rendered display string")
        .isEqualTo("EARTH");
    assertThat(select.optionsMenu().getItems().get(0).getLabel())
        .as("and the menu labels use the same renderer")
        .isEqualTo("MERCURY");
  }

  @Test
  void aNullDisplayFunctionRestoresTheDefaultRenderer() {
    final ElwhaSelectField<String> select = planets();
    select.setDisplayFunction(String::toUpperCase);

    select.setDisplayFunction(null);
    select.setSelectedValue("Earth");

    assertThat(select.getText()).as("null resets to String::valueOf").isEqualTo("Earth");
  }

  // ------------------------------------------------------- single select

  @Test
  void selectingAValueWritesItsDisplayTextIntoTheField() {
    final ElwhaSelectField<String> select = planets();

    select.setSelectedValue("Mars");

    assertThat(select.getSelectedValue()).as("the value is held").isEqualTo("Mars");
    assertThat(select.getText()).as("and rendered into the field").isEqualTo("Mars");
    assertThat(select.getSelectedValues())
        .as("the list form mirrors it as a one-element list")
        .containsExactly("Mars");
  }

  @Test
  void selectingClearsTheOtherMenuMarks() {
    final ElwhaSelectField<String> select = planets();

    select.setSelectedValue("Venus");
    select.setSelectedValue("Mars");

    assertThat(select.optionsMenu().getSelectedItems())
        .as("a single select marks exactly one option")
        .hasSize(1);
    assertThat(select.optionsMenu().getSelectedItems().get(0).getLabel())
        .as("the most recent one")
        .isEqualTo("Mars");
  }

  @Test
  void aValueOutsideTheOptionsIsIgnored() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Earth");

    select.setSelectedValue("Pluto");

    assertThat(select.getSelectedValue())
        .as("a select is constrained to its options — an unknown value changes nothing")
        .isEqualTo("Earth");
  }

  @Test
  void aNullValueClearsTheSelection() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Earth");

    select.setSelectedValue(null);

    assertThat(select.getSelectedValue()).as("null clears").isNull();
    assertThat(select.getText()).as("emptying the field so the label rests again").isEmpty();
    assertThat(select.optionsMenu().getSelectedItems()).as("and unmarking every option").isEmpty();
  }

  @Test
  void aMenuPickRoutesThroughTheSameSelectionSeam() {
    final ElwhaSelectField<String> select = planets();

    select.selectIndex(2);

    assertThat(select.getSelectedValue())
        .as("picking the third option selects Earth")
        .isEqualTo("Earth");
    assertThat(select.getText()).as("and writes it back").isEqualTo("Earth");
  }

  @Test
  void anOutOfRangePickIsIgnored() {
    final ElwhaSelectField<String> select = planets();

    select.selectIndex(9);
    select.selectIndex(-1);

    assertThat(select.getSelectedValue()).as("no option, no selection").isNull();
  }

  // ---------------------------------------------------------- listeners

  @Test
  void selectionListenerFiresOnceWithTheNewValue() {
    final ElwhaSelectField<String> select = planets();
    final List<String> heard = new ArrayList<>();
    select.addSelectionChangeListener(heard::add);

    select.setSelectedValue("Mars");

    assertThat(heard)
        .as("one change, one notification, carrying the new value")
        .containsExactly("Mars");
  }

  @Test
  void reselectingTheSameValueFiresNothing() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Mars");
    final List<String> heard = new ArrayList<>();
    select.addSelectionChangeListener(heard::add);

    select.setSelectedValue("Mars");

    assertThat(heard).as("a no-op set is not a change").isEmpty();
  }

  @Test
  void clearingFiresWithNull() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Mars");
    final List<String> heard = new ArrayList<>();
    select.addSelectionChangeListener(heard::add);

    select.setSelectedValue(null);

    assertThat(heard).as("clearing is a change, announced as null").containsExactly((String) null);
  }

  @Test
  void aRemovedListenerStopsHearing() {
    final ElwhaSelectField<String> select = planets();
    final List<String> heard = new ArrayList<>();
    final java.util.function.Consumer<String> listener = heard::add;
    select.addSelectionChangeListener(listener);

    select.removeSelectionChangeListener(listener);
    select.setSelectedValue("Mars");

    assertThat(heard).as("removal takes effect").isEmpty();
  }

  @Test
  void aNullListenerIsIgnoredRatherThanStored() {
    final ElwhaSelectField<String> select = planets();

    select.addSelectionChangeListener(null);
    select.addMultiSelectionChangeListener(null);
    select.setSelectedValue("Mars");

    assertThat(select.getSelectedValue())
        .as("registering null must not blow up the notification loop")
        .isEqualTo("Mars");
  }

  // -------------------------------------------------------- multi select

  @Test
  void multiSelectKeepsTheSelectionInOptionOrderNotToggleOrder() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.setSelectedValues(List.of("Mars", "Venus"));

    assertThat(select.getSelectedValues())
        .as("the model is canonicalized to option order")
        .containsExactly("Venus", "Mars");
    assertThat(select.getSelectedValue())
        .as("and the single-value getter tracks the first element")
        .isEqualTo("Venus");
  }

  @Test
  void multiSelectIgnoresUnknownValuesAndCollapsesDuplicates() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.setSelectedValues(List.of("Mars", "Pluto", "Mars"));

    assertThat(select.getSelectedValues())
        .as("unknown values drop out and duplicates collapse")
        .containsExactly("Mars");
  }

  @Test
  void togglingAnOptionAddsAndRemovesItWithoutDisturbingTheRest() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.toggleIndex(1);
    select.toggleIndex(3);
    assertThat(select.getSelectedValues()).as("both toggles land").containsExactly("Venus", "Mars");

    select.toggleIndex(1);
    assertThat(select.getSelectedValues())
        .as("and toggling again removes only that one")
        .containsExactly("Mars");
  }

  @Test
  void everyToggleNotifiesTheMultiListenerBecauseTheMenuStaysOpen() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    final List<List<String>> heard = new ArrayList<>();
    select.addMultiSelectionChangeListener(heard::add);

    select.toggleIndex(1);
    select.toggleIndex(3);

    assertThat(heard).as("a toggling menu fires per toggle, not once at close").hasSize(2);
    assertThat(heard.get(1))
        .as("each carrying the whole selection in option order")
        .containsExactly("Venus", "Mars");
  }

  @Test
  void multiSnapshotIsUnmodifiable() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Mars"));

    assertThat(select.getSelectedValues())
        .as("a caller must not be able to edit the selection behind the component's back")
        .isUnmodifiable();
  }

  @Test
  void singleValueListenerIsSilentInMultiMode() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    final List<String> single = new ArrayList<>();
    select.addSelectionChangeListener(single::add);

    select.toggleIndex(1);

    assertThat(single)
        .as("multi mode has its own listener contract — the single one would lie about the state")
        .isEmpty();
  }

  @Test
  void aNoOpMultiSetFiresNothing() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Venus", "Mars"));
    final List<List<String>> heard = new ArrayList<>();
    select.addMultiSelectionChangeListener(heard::add);

    select.setSelectedValues(List.of("Mars", "Venus"));

    assertThat(heard)
        .as("the same selection in a different order is still the same selection")
        .isEmpty();
  }

  @Test
  void togglingIsANoOpOutsideMultiMode() {
    final ElwhaSelectField<String> select = planets();

    select.toggleIndex(1);

    assertThat(select.getSelectedValue())
        .as("a single select has no toggle semantics to reach")
        .isNull();
  }

  @Test
  void changingOptionsPrunesTheMultiSelectionToWhatSurvives() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Venus", "Mars"));

    select.setOptions(List.of("Venus", "Jupiter"));

    assertThat(select.getSelectedValues())
        .as("values that are no longer options cannot stay selected")
        .containsExactly("Venus");
  }

  // ------------------------------------------------------------ summary

  @Test
  void aSmallSelectionRendersAsJoinedDisplayStrings() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.setSelectedValues(List.of("Mars", "Venus"));

    assertThat(select.getText())
        .as("within the limit the field spells the selection out, in option order")
        .isEqualTo("Venus, Mars");
  }

  @Test
  void aSelectionPastTheLimitCollapsesToTheCountForm() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.setSelectedValues(PLANETS);

    assertThat(select.getText())
        .as("four options past a limit of three would blow out the field")
        .isEqualTo("4 selected");
  }

  @Test
  void anEmptyMultiSelectionEmptiesTheFieldSoTheLabelRests() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Mars"));

    select.setSelectedValues(List.of());

    assertThat(select.getText()).as("no selection, no summary").isEmpty();
  }

  @Test
  void changingTheSummaryLimitReRendersImmediately() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Venus", "Mars"));

    select.setSummaryLimit(1);

    assertThat(select.getText())
        .as("the summary follows the threshold without waiting for the next selection")
        .isEqualTo("2 selected");
  }

  @Test
  void aNegativeSummaryLimitClampsToZeroSoEveryNonEmptySelectionCounts() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Mars"));

    select.setSummaryLimit(-5);

    assertThat(select.getSummaryLimit()).as("the limit clamps at zero").isZero();
    assertThat(select.getText())
        .as("so even one value shows the count form")
        .isEqualTo("1 selected");
  }

  // --------------------------------------------------------- mode flips

  @Test
  void turningMultiSelectOnSeedsTheSelectionFromTheSingleValue() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Mars");

    select.setMultiSelect(true);

    assertThat(select.getSelectedValues())
        .as("the flip preserves what it can rather than silently discarding the value")
        .containsExactly("Mars");
    assertThat(select.getText()).as("and the summary shows it").isEqualTo("Mars");
  }

  @Test
  void turningMultiSelectOffCollapsesToTheFirstValueInOptionOrder() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Mars", "Venus"));

    select.setMultiSelect(false);

    assertThat(select.getSelectedValue())
        .as("the first in option order survives")
        .isEqualTo("Venus");
    assertThat(select.getSelectedValues()).as("and the rest clear").containsExactly("Venus");
    assertThat(select.getText())
        .as("the field drops back to a plain display string")
        .isEqualTo("Venus");
  }

  @Test
  void neitherModeFlipFiresAChangeListener() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Mars");
    final List<String> single = new ArrayList<>();
    final List<List<String>> multi = new ArrayList<>();
    select.addSelectionChangeListener(single::add);
    select.addMultiSelectionChangeListener(multi::add);

    select.setMultiSelect(true);
    select.setMultiSelect(false);

    assertThat(single)
        .as("the single value is unchanged by either flip, so nothing is announced")
        .isEmpty();
    assertThat(multi).as("and the multi listener stays quiet too").isEmpty();
  }

  @Test
  void aRedundantModeFlipIsANoOp() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Mars"));

    select.setMultiSelect(true);

    assertThat(select.getSelectedValues())
        .as("setting the mode it is already in must not reset the selection")
        .containsExactly("Mars");
  }

  @Test
  void setSelectedValuesLenientlyAppliesTheFirstKnownValueInSingleMode() {
    final ElwhaSelectField<String> select = planets();

    select.setSelectedValues(List.of("Pluto", "Mars", "Venus"));

    assertThat(select.getSelectedValue())
        .as("one write path per mode: the first recognized value becomes the single selection")
        .isEqualTo("Mars");
  }

  @Test
  void setSelectedValuesWithNothingUsableClearsInSingleMode() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Mars");

    select.setSelectedValues(List.of("Pluto"));

    assertThat(select.getSelectedValue()).as("no recognized value means no selection").isNull();
  }

  @Test
  void nullValuesClearInEitherMode() {
    final ElwhaSelectField<String> select = planets();
    select.setSelectedValue("Mars");
    select.setSelectedValues(null);
    assertThat(select.getSelectedValue()).as("null clears the single select").isNull();

    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Mars"));
    select.setSelectedValues(null);
    assertThat(select.getSelectedValues()).as("and the multi select").isEmpty();
  }
}
