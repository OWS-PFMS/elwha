package com.owspfm.elwha.selectfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of what {@link ElwhaSelectField} forwards to the chassis it embeds, and of the
 * one thing it deliberately does not.
 *
 * <p>#727 asked whether the select field should expose its embedded {@link ElwhaTextField} / {@code
 * ElwhaIconButton} / {@code ElwhaMenu} so the Showcase's workbench facets could bind to them. Ruled
 * no (conventions §14) — a host exposes children the consumer supplied, not the parts it is built
 * from. Refusing is only honest if the wrapper's own API can say what the caller wanted, so the
 * settings that were genuinely unreachable were forwarded instead; this pins them.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/727">#727</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSelectFieldDelegationTest {

  private static ElwhaSelectField<String> select() {
    final ElwhaSelectField<String> select = ElwhaSelectField.outlined("Planet");
    select.setOptions(List.of("Mercury", "Venus", "Earth"));
    return select;
  }

  @Test
  void theVariantIsReachableAfterConstruction() {
    // Constructor-only before #727 — and with no getter at all, so a caller could neither read it
    // back nor change it on a field the factory had already made.
    final ElwhaSelectField<String> select = select();
    assertThat(select.getVariant()).isEqualTo(ElwhaTextField.Variant.OUTLINED);

    select.setVariant(ElwhaTextField.Variant.FILLED);
    assertThat(select.getVariant()).isEqualTo(ElwhaTextField.Variant.FILLED);
  }

  @Test
  void theLeadingIconGetterCompletesItsSetter() {
    // Conventions §5 — an asymmetric setter-without-getter is drift to fix in the next pass.
    final ElwhaSelectField<String> select = select();
    assertThat(select.getLeadingIcon()).isNull();

    final Icon icon = MaterialIcons.edit();
    select.setLeadingIcon(icon);
    assertThat(select.getLeadingIcon()).isSameAs(icon);
  }

  @Test
  void theFormAxesRoundTrip() {
    final ElwhaSelectField<String> select = select();

    select.setRequired(true);
    assertThat(select.isRequired()).isTrue();
    select.setNoAsterisk(true);
    assertThat(select.isNoAsterisk()).as("marking optional fields instead is a real form").isTrue();

    select.setPrefixText("$");
    select.setSuffixText("/mo");
    assertThat(select.getPrefixText()).isEqualTo("$");
    assertThat(select.getSuffixText()).isEqualTo("/mo");

    select.setSupportingTextVisibility(ElwhaTextField.SupportingTextVisibility.ON_FOCUS);
    assertThat(select.getSupportingTextVisibility())
        .as("supporting text was forwarded without its visibility policy")
        .isEqualTo(ElwhaTextField.SupportingTextVisibility.ON_FOCUS);

    select.setMaxLength(12);
    assertThat(select.getMaxLength()).isEqualTo(12);
  }

  @Test
  void everyForwardedSettingReachesTheEmbeddedChassis() {
    // The point of forwarding rather than exposing: the caller's write has to land on the field
    // that paints, not on a copy the wrapper keeps.
    final ElwhaSelectField<String> select = select();
    select.setVariant(ElwhaTextField.Variant.FILLED);
    select.setRequired(true);
    select.setPrefixText("$");

    final ElwhaTextField chassis = embeddedField(select);
    assertThat(chassis.getVariant()).isEqualTo(ElwhaTextField.Variant.FILLED);
    assertThat(chassis.isRequired()).isTrue();
    assertThat(chassis.getPrefixText()).isEqualTo("$");
  }

  @Test
  void theTrailingSlotStaysTheSelectFieldsOwn() {
    // §14's worked hazard: a reachable chassis would let a caller displace the arrow the select
    // field is documented to own. No passthrough exists, so the arrow survives everything the
    // public API can do to the field.
    final ElwhaSelectField<String> select = select();
    final ElwhaTextField chassis = embeddedField(select);
    final Object arrowBefore = chassis.getTrailingIconButton();
    assertThat(arrowBefore).as("the select field installs its own trailing button").isNotNull();

    select.setVariant(ElwhaTextField.Variant.FILLED);
    select.setLeadingIcon(MaterialIcons.edit());
    select.setEditable(true);

    assertThat(chassis.getTrailingIconButton())
        .as("and nothing on the select field's own surface can replace it")
        .isSameAs(arrowBefore);
    assertThat(Arrays.stream(ElwhaSelectField.class.getMethods()).map(m -> m.getName()))
        .as("no accessor hands the embedded components out either (§14)")
        .doesNotContain("getField", "getEditor", "getArrow", "getMenu", "getTabs");
  }

  /** The only reach-through in the test, and it is what the ruling declines to make public. */
  private static ElwhaTextField embeddedField(final ElwhaSelectField<String> select) {
    return (ElwhaTextField) select.getComponent(0);
  }
}
