package com.owspfm.elwha.iconbutton;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A accessibility-shape coverage for {@link ElwhaIconButton}. An icon-only control has no
 * visible label to fall back on, so the name-resolution chain is the contract that keeps it
 * announceable at all.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaIconButtonAccessibilityTest {

  @Test
  void aPushIconButtonExposesThePushButtonRole() {
    assertThat(new ElwhaIconButton(MaterialIcons.add()).getAccessibleContext().getAccessibleRole())
        .as("a CLICKABLE icon button is a push button")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void aToggleExposesTheToggleButtonRole() {
    final ElwhaIconButton button =
        new ElwhaIconButton(MaterialIcons.pushPin())
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE);

    assertThat(button.getAccessibleContext().getAccessibleRole())
        .as("a SELECTABLE icon button is a toggle button")
        .isEqualTo(AccessibleRole.TOGGLE_BUTTON);
  }

  @Test
  void nameResolutionFallsThroughDeclaredNameThenTooltipThenComponentName() {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.delete());
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("an unlabeled icon button still announces something")
        .isEqualTo("Icon button");

    button.setName("deleteButton");
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("the component name is picked up when nothing better exists")
        .isEqualTo("deleteButton");

    button.setToolTipText("Delete");
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("a tooltip outranks the component name")
        .isEqualTo("Delete");

    button.getAccessibleContext().setAccessibleName("Delete the selected row");
    assertThat(button.getAccessibleContext().getAccessibleName())
        .as("and an explicitly declared name outranks everything")
        .isEqualTo("Delete the selected row");
  }

  @Test
  void selectionIsExposedAsStateNotOnlyAsTint() {
    final ElwhaIconButton button =
        new ElwhaIconButton(MaterialIcons.pushPin())
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    final AccessibleContext context = button.getAccessibleContext();

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.SELECTED))
        .as("an unselected toggle carries no SELECTED state")
        .isFalse();

    button.setSelected(true);

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.SELECTED))
        .as("a selected toggle exposes SELECTED, so the primary tint is never the only signal")
        .isTrue();
  }

  @Test
  void aPushIconButtonNeverClaimsToBeSelected() {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add());
    button.setSelected(true);

    assertThat(
            button
                .getAccessibleContext()
                .getAccessibleStateSet()
                .contains(AccessibleState.SELECTED))
        .as("a push icon button has no selected state to expose")
        .isFalse();
  }

  @Test
  void enabledStateTracksTheComponent() {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add());
    assertThat(
            button.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.ENABLED))
        .as("an enabled icon button reports ENABLED")
        .isTrue();

    button.setEnabled(false);

    assertThat(
            button.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.ENABLED))
        .as("a disabled icon button drops ENABLED so assistive tech can skip it")
        .isFalse();
  }

  @Test
  void everySizeStaysKeyboardReachableDespiteSuppressedClickFocus() {
    for (final IconButtonSize size : IconButtonSize.values()) {
      final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add()).setButtonSize(size);
      assertThat(button.isFocusable()).as("%s is a tab stop", size).isTrue();
      assertThat(button.isRequestFocusEnabled())
          .as("%s still refuses click-focus by default — Tab navigation is gated separately", size)
          .isFalse();
    }
  }
}
