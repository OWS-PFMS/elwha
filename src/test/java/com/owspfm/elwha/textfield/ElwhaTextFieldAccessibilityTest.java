package com.owspfm.elwha.textfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.InputMode;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A accessibility coverage for {@link ElwhaTextField} (design §8) — the composed accessible
 * name, the layered description (supporting text, then error, then counter), and the
 * error&#8594;"alert" announcement, which is the one gap Swing does not fill: there is no {@code
 * ALERT} role, so the field approximates it by firing a description property change on the
 * focusable editor.
 *
 * <p>The a11y surface lives on the <em>editor</em>, not the decorator — that is the node AT lands
 * on, and hanging the name off the wrong node is a silent failure no screen reader would forgive.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTextFieldAccessibilityTest {

  private static AccessibleContext context(final ElwhaTextField field) {
    return field.getEditor().getAccessibleContext();
  }

  /** Records the description property changes the alert mechanism fires. */
  private static List<Object> watchDescriptions(final ElwhaTextField field) {
    final List<Object> announced = new ArrayList<>();
    context(field)
        .addPropertyChangeListener(
            event -> {
              if (AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY.equals(
                  event.getPropertyName())) {
                announced.add(event.getNewValue());
              }
            });
    return announced;
  }

  // ------------------------------------------------------------------ role

  @Test
  void theEditorCarriesTheTextboxRole() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    assertThat(context(field).getAccessibleRole())
        .as("the embedded editor brings the Textbox role for free")
        .isEqualTo(AccessibleRole.TEXT);
  }

  @Test
  void aMultiLineFieldKeepsAnAccessibleTextSurface() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);

    assertThat(context(field).getAccessibleRole())
        .as("swapping to a text area must not lose the role")
        .isEqualTo(AccessibleRole.TEXT);
    assertThat(context(field).getAccessibleText())
        .as("nor the AccessibleText surface AT reads the content through")
        .isNotNull();
  }

  // ------------------------------------------------------------------ name

  @Test
  void theAccessibleNameIsTheLabel() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    assertThat(context(field).getAccessibleName())
        .as("the floating label names the field")
        .isEqualTo("Email");
  }

  @Test
  void aRequiredFieldAnnouncesTheAsterisk() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setRequired(true);

    assertThat(context(field).getAccessibleName())
        .as("required is announced, not merely drawn")
        .isEqualTo("Email *");
  }

  @Test
  void suppressingTheAsteriskGlyphAlsoSuppressesItInTheName() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setRequired(true);
    field.setNoAsterisk(true);

    assertThat(context(field).getAccessibleName())
        .as("no-asterisk forms indicate requiredness once, elsewhere — the name follows the glyph")
        .isEqualTo("Email");
  }

  @Test
  void supportingTextJoinsTheName() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setSupportingText("We never share it");

    assertThat(context(field).getAccessibleName())
        .as("the supporting line is part of what the field is called")
        .isEqualTo("Email, We never share it");
  }

  @Test
  void errorTextReplacesSupportingTextInTheNameJustAsItDoesOnScreen() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setSupportingText("We never share it");
    field.setErrorText("Enter a valid address");
    field.setError(true);

    assertThat(context(field).getAccessibleName())
        .as("what is announced matches what is displayed")
        .isEqualTo("Email, Enter a valid address");
  }

  @Test
  void aLabelLessFieldStillNamesItselfFromItsSupportingText() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "");
    field.setSupportingText("Search the catalog");

    assertThat(context(field).getAccessibleName())
        .as("with no label the supporting text carries the name alone, with no stray separator")
        .isEqualTo("Search the catalog");
  }

  @Test
  void theNameSurvivesAnEditorSwap() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setSupportingText("Up to 500 words");

    field.setInputMode(InputMode.TEXT_AREA);

    assertThat(context(field).getAccessibleName())
        .as("the rebuilt editor is re-named — an unnamed field is an unusable one")
        .isEqualTo("Notes, Up to 500 words");
  }

  // ----------------------------------------------------------- description

  @Test
  void theDescriptionIsTheSupportingText() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setSupportingText("We never share it");

    assertThat(context(field).getAccessibleDescription())
        .as("supporting text is the field's description")
        .isEqualTo("We never share it");
  }

  @Test
  void anErroredDescriptionLeadsWithSupportingTextThenTheError() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setSupportingText("We never share it");
    field.setErrorText("Enter a valid address");
    field.setError(true);

    assertThat(context(field).getAccessibleDescription())
        .as("the description keeps both messages, in the research §X order")
        .isEqualTo("We never share it, Enter a valid address");
  }

  @Test
  void anErrorWithNoSupportingTextDescribesItselfAlone() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setErrorText("Required");
    field.setError(true);

    assertThat(context(field).getAccessibleDescription())
        .as("no supporting text means no leading comma")
        .isEqualTo("Required");
  }

  @Test
  void aBareFieldHasNoDescriptionAtAllRatherThanAnEmptyOne() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    assertThat(context(field).getAccessibleDescription())
        .as("an empty string would make AT announce a pause for nothing")
        .isNull();
  }

  @Test
  void theCounterIsAnnouncedInWords() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Bio");
    field.setMaxLength(20);
    field.setText("hello");

    assertThat(context(field).getAccessibleDescription())
        .as("a bare 5/20 is meaningless read aloud — research §X spells it out")
        .isEqualTo("character count, 5 of 20 characters entered");
  }

  @Test
  void theAnnouncedCountTracksEveryKeystroke() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Bio");
    field.setMaxLength(20);
    field.setText("hello");

    field.setText("hi");

    assertThat(context(field).getAccessibleDescription())
        .as("the live count is re-synced on document changes")
        .contains("2 of 20");
  }

  @Test
  void theCounterTrailsTheSupportingTextInTheDescription() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Bio");
    field.setSupportingText("Tell us about yourself");
    field.setMaxLength(20);
    field.setText("hi");

    assertThat(context(field).getAccessibleDescription())
        .as("supporting text first, then the count — the useful part leads")
        .isEqualTo("Tell us about yourself, character count, 2 of 20 characters entered");
  }

  // ----------------------------------------------------------- error alert

  @Test
  void enteringTheErrorStateFiresTheAlertAnnouncement() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setErrorText("Enter a valid address");
    final List<Object> announced = watchDescriptions(field);

    field.setError(true);

    assertThat(announced)
        .as("Swing has no ALERT role, so the error fires a description change AT surfaces live")
        .contains("Enter a valid address");
  }

  @Test
  void theAlertIsFiredFromAClearedValueSoRepeatsAreStillAnnounced() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setErrorText("Required");
    field.setError(true);
    final List<Object> announced = watchDescriptions(field);

    field.setErrorText("Required");

    assertThat(announced)
        .as("re-setting the same message must still announce — a null old value forces it")
        .contains("Required");
  }

  @Test
  void anErrorWithoutTextStillAnnouncesSomething() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    final List<Object> announced = watchDescriptions(field);

    field.setError(true);

    assertThat(announced).as("a bare error state is still worth saying out loud").contains("Error");
  }

  @Test
  void theAlertComposesSupportingTextAheadOfTheError() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setSupportingText("We never share it");
    field.setErrorText("Enter a valid address");
    final List<Object> announced = watchDescriptions(field);

    field.setError(true);

    assertThat(announced)
        .as("the announcement carries the same composed message as the description")
        .contains("We never share it, Enter a valid address");
  }

  @Test
  void leavingTheErrorStateAnnouncesNothing() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setErrorText("Required");
    field.setError(true);
    final List<Object> announced = watchDescriptions(field);

    field.setError(false);

    assertThat(announced)
        .as("recovery is not an alert — only entering the error state interrupts the user")
        .doesNotContain("Required");
  }

  @Test
  void aRedundantSetErrorDoesNotReAnnounce() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.setErrorText("Required");
    field.setError(true);
    final List<Object> announced = watchDescriptions(field);

    field.setError(true);

    assertThat(announced)
        .as("setting an already-set error state is a no-op, not a second interruption")
        .isEmpty();
  }

  // ------------------------------------------------------- trailing button

  @Test
  void anInteractiveTrailingIconIsARealChildSoItBringsItsOwnButtonRole() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Password");
    final ElwhaIconButton reveal =
        new ElwhaIconButton(MaterialIcons.visibility(ElwhaTextField.ICON_GLYPH));
    reveal.getAccessibleContext().setAccessibleName("Show password");

    field.setTrailingIconButton(reveal);

    assertThat(field.getComponents())
        .as("the button is hosted, not painted — that is what buys the Button semantics")
        .contains(reveal);
    assertThat(reveal.getAccessibleContext().getAccessibleRole())
        .as("and it announces as a push button in its own right")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }
}
