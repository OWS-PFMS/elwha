package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A accessibility-shape coverage for {@link ElwhaButton} — the role each interaction mode
 * exposes, the name-resolution fallback chain, and the state set assistive tech reads instead of
 * the shape and color signals.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonAccessibilityTest {

  @Test
  void aPushButtonExposesThePushButtonRole() {
    assertThat(new ElwhaButton("Save").getAccessibleContext().getAccessibleRole())
        .as("a CLICKABLE button is a push button")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void aToggleExposesTheToggleButtonRole() {
    final ElwhaButton button =
        new ElwhaButton("Pin").setInteractionMode(ButtonInteractionMode.SELECTABLE);

    assertThat(button.getAccessibleContext().getAccessibleRole())
        .as("a SELECTABLE button is a toggle button")
        .isEqualTo(AccessibleRole.TOGGLE_BUTTON);
  }

  @Test
  void nameResolutionFallsThroughLabelThenTooltipThenComponentName() {
    final ElwhaButton button = new ElwhaButton("Save");
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("the visible label is the accessible name")
        .isEqualTo("Save");

    button.setText("");
    button.setToolTipText("Save the document");
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("without a label the tooltip carries the name")
        .isEqualTo("Save the document");

    button.setToolTipText(null);
    button.setName("saveButton");
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("without either, the component name carries it")
        .isEqualTo("saveButton");

    button.setName(null);
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("and a fully unlabeled button still announces something")
        .isEqualTo("Button");
  }

  @Test
  void selectionIsExposedAsStateNotOnlyAsShapeAndColor() {
    final ElwhaButton button =
        new ElwhaButton("Pin").setInteractionMode(ButtonInteractionMode.SELECTABLE);
    final AccessibleContext context = button.getAccessibleContext();

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.SELECTED))
        .as("an unselected toggle carries no SELECTED state")
        .isFalse();

    button.setSelected(true);

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.SELECTED))
        .as("a selected toggle exposes SELECTED to assistive tech")
        .isTrue();
  }

  @Test
  void aPushButtonNeverClaimsToBeSelected() {
    final ElwhaButton button = new ElwhaButton("Save");
    button.setSelected(true);

    assertThat(
            button
                .getAccessibleContext()
                .getAccessibleStateSet()
                .contains(AccessibleState.SELECTED))
        .as("a push button has no selected state to expose")
        .isFalse();
  }

  @Test
  void enabledStateTracksTheComponent() {
    final ElwhaButton button = new ElwhaButton("Save");
    assertThat(
            button.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.ENABLED))
        .as("an enabled button reports ENABLED")
        .isTrue();

    button.setEnabled(false);

    assertThat(
            button.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.ENABLED))
        .as("a disabled button drops ENABLED so assistive tech can skip it")
        .isFalse();
  }

  @Test
  void aButtonIsATabStopAtEverySize() {
    for (final ButtonSize size : ButtonSize.values()) {
      assertThat(new ElwhaButton("Save").setButtonSize(size).isFocusable())
          .as("%s is keyboard reachable", size)
          .isTrue();
    }
  }
}
