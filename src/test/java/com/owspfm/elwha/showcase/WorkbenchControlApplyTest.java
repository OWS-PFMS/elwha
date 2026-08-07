package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The workbench control apply paths — driving a control has to reach the staged component, on every
 * leaf, for every value the control offers.
 *
 * <p>The sweep is deliberately exhaustive rather than representative. A workbench's {@code apply}
 * rebuilds its live component from the full control set on every change, so an illegal pairing, a
 * null-hostile combo entry, or a control wired to nothing only shows up when something walks the
 * whole matrix. Controls are driven through their models and actions, never through focus, so all
 * of it stays in the headless tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class WorkbenchControlApplyTest {

  static List<String> workbenchLeaves() {
    final List<String> labels = new ArrayList<>();
    for (final String label : ShowcaseFixture.leafLabels()) {
      final ComponentWorkbench workbench =
          ShowcaseFixture.findFirst(ShowcaseFixture.surfaceOf(label), ComponentWorkbench.class);
      if (workbench != null) {
        labels.add(label);
      }
    }
    return labels;
  }

  private static ComponentWorkbench workbenchOf(final String label) {
    return ShowcaseFixture.findFirst(ShowcaseFixture.surfaceOf(label), ComponentWorkbench.class);
  }

  private static <T> List<T> controlsOfType(
      final ComponentWorkbench workbench, final Class<T> type) {
    return ShowcaseFixture.findAll(workbench.controls(), type);
  }

  // ------------------------------------------------------------- the sweeps

  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everyWorkbenchStagesALiveComponent(final String label) {
    assertThat(workbenchOf(label).stage())
        .as("%s — a workbench with an empty stage demonstrates nothing", label)
        .isNotNull();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everyChoiceOnEveryComboApplies(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final JComboBox<?> combo : controlsOfType(workbench, JComboBox.class)) {
      final int original = combo.getSelectedIndex();
      for (int choice = 0; choice < combo.getItemCount(); choice++) {
        combo.setSelectedIndex(choice);

        assertThat(workbench.stage())
            .as(
                "%s — choosing %s on the %s control left the stage empty",
                label, combo.getItemAt(choice), name(combo))
            .isNotNull();
        assertThat(ShowcaseFixture.descendants(workbench))
            .as(
                "%s — choosing %s on the %s control staged a component that never got mounted",
                label, combo.getItemAt(choice), name(combo))
            .contains(workbench.stage());
      }
      restore(combo, original);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everyLiveToggleApplies(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final ElwhaCheckbox toggle : controlsOfType(workbench, ElwhaCheckbox.class)) {
      if (!toggle.isEnabled()) {
        continue;
      }
      final boolean original = toggle.isChecked();

      toggle.doClick();
      assertToggled(workbench, label, toggle, !original);

      toggle.doClick();
      assertToggled(workbench, label, toggle, original);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void aGatedToggleRefusesTheClick(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final ElwhaCheckbox toggle : controlsOfType(workbench, ElwhaCheckbox.class)) {
      if (toggle.isEnabled()) {
        continue;
      }
      final boolean original = toggle.isChecked();

      toggle.doClick();

      assertThat(toggle.isChecked())
          .as(
              "%s — a workbench disables %s while it does not apply to the current configuration, "
                  + "and a disabled control must ignore the gesture rather than stage something "
                  + "the code view cannot describe",
              label, toggle.getLabel())
          .isEqualTo(original);
      assertThat(workbench.stage()).as("%s — and the stage survives it", label).isNotNull();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everySpinnerApplies(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final JSpinner spinner : controlsOfType(workbench, JSpinner.class)) {
      final Object original = spinner.getValue();
      final Object next = spinner.getModel().getNextValue();
      if (next == null) {
        continue;
      }

      spinner.setValue(next);

      assertThat(spinner.getValue())
          .as("%s — the %s spinner refused a value its own model offered", label, name(spinner))
          .isEqualTo(next);
      assertThat(workbench.stage())
          .as("%s — stepping the %s spinner left the stage empty", label, name(spinner))
          .isNotNull();
      spinner.setValue(original);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everyWorkbenchRendersEquivalentJava(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    assertThat(workbench.codeFor("Component"))
        .as("%s — the code view is half the point of a workbench", label)
        .isNotBlank();
  }

  // --------------------------------------------------- the staged component

  @Test
  void buttonVariantControlRestagesTheButton() {
    final ComponentWorkbench workbench = workbenchOf("Button");
    final JComboBox<?> variants = combo(workbench, "Variant");

    for (final ButtonVariant variant : ButtonVariant.values()) {
      variants.setSelectedItem(variant);

      assertThat(((ElwhaButton) workbench.stage()).getVariant())
          .as("the control does not merely record a choice; it rebuilds the live button")
          .isEqualTo(variant);
    }
  }

  @Test
  void buttonSizeControlRestagesTheButton() {
    final ComponentWorkbench workbench = workbenchOf("Button");
    final JComboBox<?> sizes = combo(workbench, "Size");

    for (final ButtonSize size : ButtonSize.values()) {
      sizes.setSelectedItem(size);

      assertThat(((ElwhaButton) workbench.stage()).getButtonSize()).isEqualTo(size);
    }
  }

  @Test
  void buttonEnabledToggleReachesTheStage() {
    final ComponentWorkbench workbench = workbenchOf("Button");
    final ElwhaCheckbox enabled = toggle(workbench, "Enabled");

    setChecked(enabled, false);
    assertThat(workbench.stage().isEnabled())
        .as("the disabled state is a demonstrable state, so it has to reach the live component")
        .isFalse();

    setChecked(enabled, true);
    assertThat(workbench.stage().isEnabled()).isTrue();
  }

  @Test
  void buttonCodeViewTracksTheChosenVariant() {
    final ComponentWorkbench workbench = workbenchOf("Button");

    combo(workbench, "Variant").setSelectedItem(ButtonVariant.OUTLINED);

    assertThat(workbench.codeFor("Component"))
        .as("the equivalent Java is regenerated with the component, not left stale")
        .contains("ButtonVariant.OUTLINED");
  }

  @Test
  void illegalSelectableTextPairingIsCoercedRatherThanThrown() {
    final ComponentWorkbench workbench = workbenchOf("Button");
    final JComboBox<?> variants = combo(workbench, "Variant");
    final JComboBox<?> modes = combo(workbench, "Interaction mode");

    variants.setSelectedItem(ButtonVariant.TEXT);
    modes.setSelectedItem(com.owspfm.elwha.button.ButtonInteractionMode.SELECTABLE);

    assertThat(modes.getSelectedItem())
        .as(
            "#183 — SELECTABLE + TEXT is illegal, so the workbench walks the control back to a "
                + "legal pairing instead of letting the demo throw")
        .isEqualTo(com.owspfm.elwha.button.ButtonInteractionMode.CLICKABLE);
    assertThat(workbench.stage()).isNotNull();
  }

  @Test
  void chipVariantControlRestagesTheChip() {
    final ComponentWorkbench workbench = workbenchOf("Chip");
    final JComboBox<?> variants = combo(workbench, "Variant");

    for (final ChipVariant variant : ChipVariant.values()) {
      variants.setSelectedItem(variant);

      assertThat(((ElwhaChip) workbench.stage()).getVariant()).isEqualTo(variant);
    }
  }

  @Test
  void iconButtonVariantControlRestagesTheButton() {
    final ComponentWorkbench workbench = workbenchOf("Icon Button");
    final JComboBox<?> variants = combo(workbench, "Variant");

    for (final IconButtonVariant variant : IconButtonVariant.values()) {
      variants.setSelectedItem(variant);

      assertThat(((ElwhaIconButton) workbench.stage()).getVariant()).isEqualTo(variant);
    }
  }

  // ----------------------------------------------------------------- detail

  private static void assertToggled(
      final ComponentWorkbench workbench,
      final String label,
      final ElwhaCheckbox toggle,
      final boolean expected) {
    assertThat(toggle.isChecked())
        .as("%s — clicking the %s toggle did not change it", label, toggle.getLabel())
        .isEqualTo(expected);
    assertThat(workbench.stage())
        .as("%s — toggling %s left the stage empty", label, toggle.getLabel())
        .isNotNull();
  }

  private static void restore(final JComboBox<?> combo, final int original) {
    if (original >= 0 && original < combo.getItemCount()) {
      combo.setSelectedIndex(original);
    }
  }

  private static void setChecked(final ElwhaCheckbox toggle, final boolean checked) {
    if (toggle.isChecked() != checked) {
      toggle.doClick();
    }
  }

  private static JComboBox<?> combo(final ComponentWorkbench workbench, final String label) {
    final Component control = ShowcaseFixture.controlLabelled(workbench.controls(), label);
    assertThat(control).as("the %s control", label).isInstanceOf(JComboBox.class);
    return (JComboBox<?>) control;
  }

  private static ElwhaCheckbox toggle(final ComponentWorkbench workbench, final String text) {
    for (final ElwhaCheckbox box : controlsOfType(workbench, ElwhaCheckbox.class)) {
      if (text.equals(box.getLabel())) {
        return box;
      }
    }
    throw new AssertionError("no " + text + " toggle on this workbench");
  }

  private static String name(final JComponent control) {
    final String label = ShowcaseFixture.labelOf(control);
    return label.isEmpty() ? control.getClass().getSimpleName() : label;
  }
}
