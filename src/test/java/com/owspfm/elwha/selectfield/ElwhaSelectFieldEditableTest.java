package com.owspfm.elwha.selectfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.menu.SelectionMode;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the editable combo (Phase 2) — filter-as-you-type over the cached menu, the
 * no-match placeholder, and the commit semantics: text matching an option canonicalizes to that
 * option, and text that matches nothing either reverts or commits according to the free-text
 * policy.
 *
 * <p>Commits are driven through {@code resolveFieldText} / {@code revertToCommitted}, the seams the
 * Enter and focus-loss paths funnel into. <b>Which events reach those seams</b> is a separate
 * contract: the focus-loss guard (a temporary loss or a hop within the control is not a commit
 * point) needs real focus transfer and lives in {@link ElwhaSelectFieldGuiTest}, with the
 * window-deactivation half noted there as unreachable in either tier without a second window.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSelectFieldEditableTest {

  private static final List<String> PLANETS = List.of("Mercury", "Venus", "Earth", "Mars");

  private static ElwhaSelectField<String> combo() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    select.setEditable(true);
    select.optionsMenu();
    return select;
  }

  /** The embedded editor — typing into it is what a real user's keystrokes reach. */
  private static JTextComponent editor(final ElwhaSelectField<String> select) {
    return ((ElwhaTextField) select.getComponent(0)).getEditor();
  }

  /** The labels of the option rows currently left visible by the filter. */
  private static List<String> visibleOptions(final ElwhaSelectField<String> select) {
    final List<String> labels = new ArrayList<>();
    for (final ElwhaMenuItem item : select.optionsMenu().getItems()) {
      if (item.isVisible()) {
        labels.add(item.getLabel());
      }
    }
    return labels;
  }

  // -------------------------------------------------------------- filter

  @Test
  void anEmptyFilterShowsEveryOption() {
    final ElwhaSelectField<String> select = combo();

    assertThat(visibleOptions(select))
        .as("nothing typed, nothing hidden")
        .containsExactlyElementsOf(PLANETS);
  }

  @Test
  void typingNarrowsTheVisibleOptions() {
    final ElwhaSelectField<String> select = combo();

    editor(select).setText("ma");

    assertThat(visibleOptions(select))
        .as("the filter matches against the display strings")
        .containsExactly("Mars");
  }

  @Test
  void filterIsCaseInsensitive() {
    final ElwhaSelectField<String> select = combo();

    editor(select).setText("MARS");

    assertThat(visibleOptions(select))
        .as("a user typing in any case still finds the option")
        .containsExactly("Mars");
  }

  @Test
  void filterMatchesSubstringsNotJustPrefixes() {
    final ElwhaSelectField<String> select = combo();

    editor(select).setText("art");

    assertThat(visibleOptions(select))
        .as("a mid-word match qualifies — 'art' finds Earth")
        .containsExactly("Earth");
  }

  @Test
  void aFilterMatchingNothingShowsTheNoMatchesPlaceholderInstead() {
    final ElwhaSelectField<String> select = combo();

    editor(select).setText("zzz");

    assertThat(visibleOptions(select))
        .as("an empty list would read as a broken menu — the placeholder says why")
        .containsExactly("No matches");
  }

  @Test
  void noMatchesRowIsNotSelectable() {
    final ElwhaSelectField<String> select = combo();
    editor(select).setText("zzz");

    final ElwhaMenuItem placeholder =
        select.optionsMenu().getItems().stream()
            .filter(item -> "No matches".equals(item.getLabel()))
            .findFirst()
            .orElseThrow();

    assertThat(placeholder.isEnabled()).as("the placeholder is a message, not an option").isFalse();
  }

  @Test
  void clearingTheFilterRestoresTheFullList() {
    final ElwhaSelectField<String> select = combo();
    editor(select).setText("ma");

    editor(select).setText("");

    assertThat(visibleOptions(select))
        .as("backspacing back to empty shows everything again")
        .containsExactlyElementsOf(PLANETS);
  }

  @Test
  void aPureSelectNeverFilters() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    select.optionsMenu();

    editor(select).setText("ma");

    assertThat(visibleOptions(select))
        .as("filtering is an editable-combo behavior; a pure select's text is its value")
        .containsExactlyElementsOf(PLANETS);
  }

  @Test
  void aProgrammaticWriteBackDoesNotBecomeAFilter() {
    final ElwhaSelectField<String> select = combo();

    select.setSelectedValue("Mars");

    assertThat(visibleOptions(select))
        .as("picking an option must not leave the list filtered down to that option")
        .containsExactlyElementsOf(PLANETS);
  }

  @Test
  void noMatchesRowExistsOnlyInEditableMode() {
    final ElwhaSelectField<String> pure = ElwhaSelectField.filled("Planet");
    pure.setOptions(PLANETS);

    assertThat(pure.optionsMenu().getItems())
        .as("a pure select has no filter, so it has no empty state to explain")
        .hasSize(PLANETS.size());
  }

  // -------------------------------------------------------------- commit

  @Test
  void textMatchingAnOptionCommitsThatOption() {
    final ElwhaSelectField<String> select = combo();
    editor(select).setText("Mars");

    select.resolveFieldText();

    assertThat(select.getSelectedValue())
        .as("the typed text resolves to the option")
        .isEqualTo("Mars");
  }

  @Test
  void aCaseInsensitiveMatchCanonicalizesTheDisplayText() {
    final ElwhaSelectField<String> select = combo();
    editor(select).setText("mArS");

    select.resolveFieldText();

    assertThat(select.getSelectedValue()).as("case does not stop a match").isEqualTo("Mars");
    assertThat(select.getText())
        .as("and the field is rewritten in the option's own spelling")
        .isEqualTo("Mars");
  }

  @Test
  void aConstrainedComboRevertsTextThatMatchesNoOption() {
    final ElwhaSelectField<String> select = combo();
    select.setSelectedValue("Venus");
    editor(select).setText("Pluto");

    select.resolveFieldText();

    assertThat(select.getText())
        .as(
            "the default combo is constrained — unrecognized text falls back to the committed"
                + " value")
        .isEqualTo("Venus");
    assertThat(select.getSelectedValue()).as("and the value is untouched").isEqualTo("Venus");
  }

  @Test
  void aConstrainedComboWithNoPriorValueRevertsToEmpty() {
    final ElwhaSelectField<String> select = combo();
    editor(select).setText("Pluto");

    select.resolveFieldText();

    assertThat(select.getText()).as("there is nothing to fall back to but empty").isEmpty();
    assertThat(select.getSelectedValue()).as("and still no selection").isNull();
  }

  @Test
  void afreeTextComboKeepsWhatWasTyped() {
    final ElwhaSelectField<String> select = combo();
    select.setFreeTextAllowed(true);
    editor(select).setText("Pluto");

    select.resolveFieldText();

    assertThat(select.getText()).as("free text is kept as the field's value").isEqualTo("Pluto");
    assertThat(select.getSelectedValue())
        .as("but it maps to no option, so there is no selected value")
        .isNull();
  }

  @Test
  void committingFreeTextClearsAPreviousSelectionAndAnnouncesIt() {
    final ElwhaSelectField<String> select = combo();
    select.setFreeTextAllowed(true);
    select.setSelectedValue("Venus");
    final List<String> heard = new ArrayList<>();
    select.addSelectionChangeListener(heard::add);
    editor(select).setText("Pluto");

    select.resolveFieldText();

    assertThat(select.getSelectedValue()).as("free text supersedes the option").isNull();
    assertThat(heard).as("and that is a change worth announcing").containsExactly((String) null);
    assertThat(select.optionsMenu().getSelectedItems())
        .as("with every menu mark cleared")
        .isEmpty();
  }

  @Test
  void committingFreeTextTwiceAnnouncesOnce() {
    final ElwhaSelectField<String> select = combo();
    select.setFreeTextAllowed(true);
    editor(select).setText("Pluto");
    select.resolveFieldText();
    final List<String> heard = new ArrayList<>();
    select.addSelectionChangeListener(heard::add);

    editor(select).setText("Charon");
    select.resolveFieldText();

    assertThat(heard)
        .as("the selection was already null — a second free-text commit changes nothing")
        .isEmpty();
    assertThat(select.getText()).as("though the text still updates").isEqualTo("Charon");
  }

  @Test
  void revertingRestoresTheLastCommittedValue() {
    final ElwhaSelectField<String> select = combo();
    select.setSelectedValue("Venus");
    editor(select).setText("Mar");

    select.revertToCommitted();

    assertThat(select.getText()).as("Escape abandons the in-flight text").isEqualTo("Venus");
  }

  @Test
  void revertingAlsoClearsTheFilter() {
    final ElwhaSelectField<String> select = combo();
    select.setSelectedValue("Venus");
    editor(select).setText("Mar");
    assertThat(visibleOptions(select))
        .as("the in-flight text filtered the list")
        .containsExactly("Mars");

    select.revertToCommitted();

    assertThat(visibleOptions(select))
        .as("abandoning the text abandons the filter with it")
        .containsExactlyElementsOf(PLANETS);
  }

  @Test
  void freeTextThatLaterMatchesAnOptionResolvesToTheOption() {
    final ElwhaSelectField<String> select = combo();
    select.setFreeTextAllowed(true);
    editor(select).setText("Pluto");
    select.resolveFieldText();

    editor(select).setText("Mars");
    select.resolveFieldText();

    assertThat(select.getSelectedValue())
        .as("an exact match always wins over the free-text policy")
        .isEqualTo("Mars");
  }

  @Test
  void bothCommitSeamsAreNoOpsOnAPureSelect() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    select.setSelectedValue("Venus");
    editor(select).setText("Mars");

    select.resolveFieldText();
    select.revertToCommitted();

    assertThat(select.getSelectedValue())
        .as("a pure select has no typed text to commit or abandon")
        .isEqualTo("Venus");
  }

  // ------------------------------------------------------ menu lifecycle

  @Test
  void anEditableComboBuildsASingleSelectionMenu() {
    assertThat(combo().optionsMenu().getSelectionMode())
        .as("a combo picks one option at a time")
        .isEqualTo(SelectionMode.SINGLE);
  }

  @Test
  void aMultiSelectBuildsAMultiSelectionMenu() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    select.setMultiSelect(true);

    assertThat(select.optionsMenu().getSelectionMode())
        .as("a multi-select's menu toggles rather than closing on each pick")
        .isEqualTo(SelectionMode.MULTI);
  }

  @Test
  void changingTheOptionsRebuildsTheMenuOnTheNextOpen() {
    final ElwhaSelectField<String> select = combo();
    final Object before = select.optionsMenu();

    select.setOptions(List.of("Io", "Europa"));

    assertThat(select.optionsMenu())
        .as("the rebuild-on-options-change lifecycle: the cached menu is dropped, not patched")
        .isNotSameAs(before);
    assertThat(visibleOptions(select))
        .as("and rebuilt from the new options")
        .containsExactly("Io", "Europa");
  }

  @Test
  void changingTheDisplayFunctionRebuildsTheMenuToo() {
    final ElwhaSelectField<String> select = combo();
    final Object before = select.optionsMenu();

    select.setDisplayFunction(String::toUpperCase);

    assertThat(select.optionsMenu())
        .as("stale labels would be worse than a rebuild")
        .isNotSameAs(before);
    assertThat(visibleOptions(select))
        .as("with the labels re-rendered")
        .containsExactly("MERCURY", "VENUS", "EARTH", "MARS");
  }

  @Test
  void aRebuiltMenuCarriesTheCurrentSelectionMarks() {
    final ElwhaSelectField<String> select = combo();
    select.setSelectedValue("Mars");

    select.setDisplayFunction(String::toUpperCase);

    assertThat(select.optionsMenu().getSelectedItems())
        .as("a rebuild must not silently unmark the selected option")
        .hasSize(1);
    assertThat(select.optionsMenu().getSelectedItems().get(0).getLabel())
        .as("the same one, re-rendered")
        .isEqualTo("MARS");
  }

  @Test
  void switchingModesDropsTheCachedMenuSoItsSelectionModeIsRebuilt() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    assertThat(select.optionsMenu().getSelectionMode())
        .as("single to start")
        .isEqualTo(SelectionMode.SINGLE);

    select.setMultiSelect(true);

    assertThat(select.optionsMenu().getSelectionMode())
        .as("a mode flip cannot leave a SINGLE menu behind a multi-select")
        .isEqualTo(SelectionMode.MULTI);
  }
}
