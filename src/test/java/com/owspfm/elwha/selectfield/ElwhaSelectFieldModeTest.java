package com.owspfm.elwha.selectfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaSelectField}'s mode axes and its delegation to the embedded field
 * — the <b>editable / multi-select mutual exclusion</b> (which <em>forces</em> the other mode off
 * rather than throwing, so a consumer never has to order its setters defensively), the read-only /
 * disabled propagation, the expanded state with its arrow rotation and announcement, and the
 * deliberate absence of a trailing-slot passthrough.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSelectFieldModeTest {

  private static ElwhaSelectField<String> planets() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(List.of("Mercury", "Venus", "Earth", "Mars"));
    return select;
  }

  /** The embedded field the select composes — the only child it adds. */
  private static ElwhaTextField embedded(final ElwhaSelectField<String> select) {
    return (ElwhaTextField) select.getComponent(0);
  }

  /** The select-owned dropdown arrow, which occupies the embedded field's trailing slot. */
  private static ElwhaIconButton arrow(final ElwhaSelectField<String> select) {
    return embedded(select).getTrailingIconButton();
  }

  // -------------------------------------------------- mutual exclusion

  @Test
  void turningOnEditableForcesMultiSelectOff() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.setEditable(true);

    assertThat(select.isEditable()).as("editable takes effect").isTrue();
    assertThat(select.isMultiSelect())
        .as("and multi-select yields — V1 defers the filterable multi-select")
        .isFalse();
  }

  @Test
  void turningOnMultiSelectForcesEditableOff() {
    final ElwhaSelectField<String> select = planets();
    select.setEditable(true);

    select.setMultiSelect(true);

    assertThat(select.isMultiSelect()).as("multi-select takes effect").isTrue();
    assertThat(select.isEditable()).as("and editable yields, symmetrically").isFalse();
  }

  @Test
  void theExclusionForcesRatherThanThrowsSoSetterOrderNeverMatters() {
    final ElwhaSelectField<String> select = planets();

    assertThatCode(
            () -> {
              select.setEditable(true);
              select.setMultiSelect(true);
              select.setEditable(true);
              select.setMultiSelect(true);
            })
        .as("a consumer must be able to set either mode without guarding the other first")
        .doesNotThrowAnyException();
    assertThat(select.isMultiSelect()).as("the last setter wins").isTrue();
    assertThat(select.isEditable()).as("and the other is off").isFalse();
  }

  @Test
  void forcingMultiSelectOffPreservesItsFirstValueAsTheSingleSelection() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);
    select.setSelectedValues(List.of("Venus", "Mars"));

    select.setEditable(true);

    assertThat(select.getSelectedValue())
        .as("the forced collapse follows the ordinary multi-off rule, not a silent wipe")
        .isEqualTo("Venus");
  }

  // ------------------------------------------------------- editability

  @Test
  void thePureSelectKeepsItsEmbeddedFieldReadOnly() {
    final ElwhaSelectField<String> select = planets();

    assertThat(embedded(select).isReadOnly())
        .as("the default select has no typing surface — picking is the only way to set a value")
        .isTrue();
  }

  @Test
  void editableModeOpensTheEmbeddedFieldForTyping() {
    final ElwhaSelectField<String> select = planets();

    select.setEditable(true);

    assertThat(embedded(select).isReadOnly()).as("the combo's editor is typeable").isFalse();
  }

  @Test
  void readOnlyBeatsEditable() {
    final ElwhaSelectField<String> select = planets();
    select.setEditable(true);

    select.setReadOnly(true);

    assertThat(select.isReadOnly()).as("the select reports read-only").isTrue();
    assertThat(embedded(select).isReadOnly())
        .as("and a read-only combo cannot be typed into — read-only shows, never changes")
        .isTrue();
  }

  @Test
  void clearingReadOnlyRestoresTypingOnlyForAnEditableSelect() {
    final ElwhaSelectField<String> select = planets();
    select.setReadOnly(true);

    select.setReadOnly(false);

    assertThat(embedded(select).isReadOnly())
        .as("a pure select stays untypeable when read-only is lifted")
        .isTrue();

    select.setEditable(true);
    assertThat(embedded(select).isReadOnly()).as("only editable mode opens the editor").isFalse();
  }

  // ---------------------------------------------------------- disabled

  @Test
  void disablingTheSelectDisablesTheFieldAndTheArrowTogether() {
    final ElwhaSelectField<String> select = planets();

    select.setEnabled(false);

    assertThat(select.isEnabled()).as("the control is disabled").isFalse();
    assertThat(embedded(select).isEnabled()).as("the embedded field with it").isFalse();
    assertThat(arrow(select).isEnabled())
        .as("and the arrow too — a live arrow on a dead control is the #432 class of bug")
        .isFalse();
  }

  @Test
  void aDisabledSelectRefusesToExpand() {
    final ElwhaSelectField<String> select = planets();
    select.setEnabled(false);

    assertThat(select.isExpanded()).as("nothing has opened it").isFalse();
  }

  @Test
  void enablingPropagatesBack() {
    final ElwhaSelectField<String> select = planets();
    select.setEnabled(false);

    select.setEnabled(true);

    assertThat(embedded(select).isEnabled()).as("the field re-enables").isTrue();
    assertThat(arrow(select).isEnabled()).as("and so does the arrow").isTrue();
  }

  // ---------------------------------------------------------- expanded

  @Test
  void expandingRotatesTheArrowToPointUp() {
    final ElwhaSelectField<String> select = planets();

    select.applyExpandedState(true);

    assertThat(select.isExpanded()).as("the control reports itself expanded").isTrue();
    assertThat(select.arrowAngle())
        .as("and the glyph is a half turn over — reduced motion lands there instantly")
        .isEqualTo(180f);
  }

  @Test
  void collapsingRotatesTheArrowBack() {
    final ElwhaSelectField<String> select = planets();
    select.applyExpandedState(true);

    select.applyExpandedState(false);

    assertThat(select.isExpanded()).as("collapsed").isFalse();
    assertThat(select.arrowAngle()).as("and the glyph points down again").isZero();
  }

  @Test
  void theArrowRenamesItselfWithTheExpandedState() {
    final ElwhaSelectField<String> select = planets();

    assertThat(arrow(select).getAccessibleContext().getAccessibleName())
        .as("a collapsed control offers to open")
        .isEqualTo("Open options");

    select.applyExpandedState(true);

    assertThat(arrow(select).getAccessibleContext().getAccessibleName())
        .as("and an expanded one offers to close")
        .isEqualTo("Close options");
  }

  // -------------------------------------------------------- delegation

  @Test
  void thePreferredSizeIsTheEmbeddedFieldsOwn() {
    final ElwhaSelectField<String> select = planets();

    assertThat(select.getPreferredSize())
        .as("the select paints nothing of its own — it is the field, plus a menu")
        .isEqualTo(embedded(select).getPreferredSize());
  }

  @Test
  void theLabelSupportingTextAndPlaceholderDelegateToTheField() {
    final ElwhaSelectField<String> select = planets();

    select.setLabel("Destination");
    select.setSupportingText("Pick one");
    select.setPlaceholder("Choose...");

    assertThat(select.getLabel()).as("the label round-trips").isEqualTo("Destination");
    assertThat(embedded(select).getLabel())
        .as("through the embedded field")
        .isEqualTo("Destination");
    assertThat(select.getSupportingText()).as("as does the supporting text").isEqualTo("Pick one");
    assertThat(embedded(select).getSupportingText()).as("on the field").isEqualTo("Pick one");
    assertThat(select.getPlaceholder()).as("and the placeholder").isEqualTo("Choose...");
  }

  @Test
  void theErrorStateDelegatesToTheFieldsChrome() {
    final ElwhaSelectField<String> select = planets();

    select.setErrorText("Choose a destination");
    select.setError(true);

    assertThat(select.isError()).as("the select reports the error").isTrue();
    assertThat(embedded(select).isError())
        .as("and the field paints it — one error chrome, not two")
        .isTrue();
    assertThat(select.getErrorText())
        .as("with the message carried through")
        .isEqualTo("Choose a destination");
  }

  @Test
  void theLeadingIconSlotIsAvailableToConsumers() {
    final ElwhaSelectField<String> select = planets();

    select.setLeadingIcon(MaterialIcons.info(24));

    assertThat(embedded(select).getLeadingIcon())
        .as("the leading slot is the consumer's")
        .isNotNull();
  }

  @Test
  void theTrailingSlotStaysOwnedByTheArrow() {
    final ElwhaSelectField<String> select = planets();

    assertThat(arrow(select))
        .as("the arrow occupies the trailing slot from construction")
        .isNotNull();
    assertThat(embedded(select).getTrailingIcon())
        .as("as the interactive button, not a static glyph")
        .isNull();
  }

  @Test
  void theVariantFactoriesReachTheEmbeddedFieldsChrome() {
    assertThat(embedded(ElwhaSelectField.filled("Planet")).getVariant())
        .as("filled()")
        .isEqualTo(ElwhaTextField.Variant.FILLED);
    assertThat(embedded(ElwhaSelectField.outlined("Planet")).getVariant())
        .as("outlined()")
        .isEqualTo(ElwhaTextField.Variant.OUTLINED);
  }

  @Test
  void aNullVariantOrLabelFallsBackRatherThanThrowing() {
    final ElwhaSelectField<String> select = new ElwhaSelectField<>(null, null);

    assertThat(embedded(select).getVariant())
        .as("a null variant is treated as FILLED")
        .isEqualTo(ElwhaTextField.Variant.FILLED);
    assertThat(select.getLabel()).as("and a null label as empty").isEmpty();
  }
}
