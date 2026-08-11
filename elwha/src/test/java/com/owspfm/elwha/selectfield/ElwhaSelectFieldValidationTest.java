package com.owspfm.elwha.selectfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A throw-contract coverage for {@link ElwhaSelectField}'s argument validation (conventions
 * §15): the documented null-as-clear semantics stay legal, and a null hiding <em>inside</em> the
 * options list is rejected eagerly with the parameter named rather than surfacing as an unnamed
 * copy-time NPE.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSelectFieldValidationTest {

  @Test
  void setOptionsRejectsANullElementNamingTheParameter() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");

    assertThatNullPointerException()
        .isThrownBy(() -> select.setOptions(Arrays.asList("Mercury", null, "Earth")))
        .withMessage("options must not contain null elements");
  }

  @Test
  void setOptionsNullStaysTheDocumentedClear() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(List.of("Mercury", "Venus"));

    assertThatCode(() -> select.setOptions(null)).doesNotThrowAnyException();
    assertThat(select.getOptions()).as("null means no options — the documented reset").isEmpty();
  }

  @Test
  void setDisplayFunctionNullStaysTheDocumentedReset() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(List.of("Mercury"));
    select.setDisplayFunction(value -> "planet " + value);

    assertThatCode(() -> select.setDisplayFunction(null)).doesNotThrowAnyException();

    select.setSelectedValue("Mercury");
    assertThat(select.getSelectedValue())
        .as("the field still operates after the documented reset to String::valueOf")
        .isEqualTo("Mercury");
  }
}
