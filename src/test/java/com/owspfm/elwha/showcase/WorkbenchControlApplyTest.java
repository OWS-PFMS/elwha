package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The workbench control apply paths — driving a control has to reach the staged component, on every
 * leaf, for every value the control offers.
 *
 * <p>The sweep is deliberately exhaustive rather than representative. A workbench's {@code apply}
 * rebuilds its live component from the full control set on every change, so an illegal pairing, a
 * null-hostile option, or a control wired to nothing only shows up when something walks the whole
 * matrix. Controls are driven through their models and actions, never through focus, so all of it
 * stays in the headless tier.
 *
 * <p>The rails are Elwha's own controls since the dogfood sweep (#424): an {@code ElwhaSelectField}
 * is driven through {@code getOptions()} + {@code setSelectedValue}, an {@code ElwhaCheckbox}
 * through {@code doClick()}. {@code JSpinner} is the one raw survivor — the library has no stepper
 * — and keeps its row label where the Elwha controls carry their captions themselves.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class WorkbenchControlApplyTest {

  // The switcher bar's own horizontal padding, 8 px a side.
  private static final int SWITCHER_BAR_PADDING = 16;

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

  /**
   * Every builder-populated control of {@code type} on a workbench — the {@code Component}
   * segment's column and each facet's, since the facet rollout (#318) moved real control surface
   * out of the main column and a sweep that still read only {@code controls()} would stop seeing
   * it. The {@code Surface} column is excluded: it is the scaffold's, not a builder's, and {@link
   * SurfaceControlPanelTest} owns it.
   */
  private static <T> List<T> controlsOfType(
      final ComponentWorkbench workbench, final Class<T> type) {
    final List<T> found = new ArrayList<>();
    for (final JComponent column : workbench.controlColumns()) {
      found.addAll(ShowcaseFixture.findAll(column, type));
    }
    return found;
  }

  // -------------------------------------------------- the sweep sweeps something

  /**
   * The sweeps below iterate whatever controls they find, so a rail that stopped exposing any would
   * turn every one of them into a green no-op. That is exactly what the dogfood sweep (#424) risked
   * — it retyped every rail control, and a sweep still looking for the old type would have passed
   * loudly and tested nothing. The floors are the counts the swept Showcase actually carries; they
   * are a tripwire, not a target, so raising a floor is fine and dropping below one is the bug.
   */
  @Test
  void sweepsFindTheControlsTheyWalk() {
    int selects = 0;
    int toggles = 0;
    int spinners = 0;
    int options = 0;
    for (final String label : workbenchLeaves()) {
      final ComponentWorkbench workbench = workbenchOf(label);
      for (final ElwhaSelectField<?> select : controlsOfType(workbench, ElwhaSelectField.class)) {
        selects++;
        options += select.getOptions().size();
      }
      toggles += controlsOfType(workbench, ElwhaCheckbox.class).size();
      spinners += controlsOfType(workbench, JSpinner.class).size();
    }

    assertThat(selects).as("select fields the choice sweep walks").isGreaterThanOrEqualTo(80);
    assertThat(options).as("choices the choice sweep applies").isGreaterThanOrEqualTo(720);
    assertThat(toggles).as("checkboxes the toggle sweeps click").isGreaterThanOrEqualTo(90);
    assertThat(spinners).as("spinners the spinner sweep steps").isGreaterThanOrEqualTo(22);
  }

  /**
   * Every rail control is reachable by the caption it shows. The sweep moved most captions off the
   * row label and into the control's own floating label, and a caption that resolves to nothing is
   * a control no by-name test can ever drive again.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everyRailControlIsReachableByItsCaption(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final ElwhaSelectField<?> select : controlsOfType(workbench, ElwhaSelectField.class)) {
      final String caption = ShowcaseFixture.labelOf(select);
      assertThat(caption).as("%s — a rail select with no caption at all", label).isNotEmpty();
      assertThat(ShowcaseFixture.controlLabelled(select.getParent(), caption))
          .as(
              "%s — the %s select does not resolve back to itself by its own caption",
              label, caption)
          .isSameAs(select);
    }
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
  void everyChoiceOnEverySelectApplies(final String label) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final ElwhaSelectField<?> select : controlsOfType(workbench, ElwhaSelectField.class)) {
      final Object original = select.getSelectedValue();
      for (final Object choice : select.getOptions()) {
        choose(select, choice);

        assertThat(workbench.stage())
            .as(
                "%s — choosing %s on the %s control left the stage empty",
                label, choice, name(select))
            .isNotNull();
        assertThat(ShowcaseFixture.descendants(workbench))
            .as(
                "%s — choosing %s on the %s control staged a component that never got mounted",
                label, choice, name(select))
            .contains(workbench.stage());
      }
      restore(select, original);
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
    final ElwhaSelectField<?> variants = select(workbench, "Variant");

    for (final ButtonVariant variant : ButtonVariant.values()) {
      choose(variants, variant);

      assertThat(((ElwhaButton) workbench.stage()).getVariant())
          .as("the control does not merely record a choice; it rebuilds the live button")
          .isEqualTo(variant);
    }
  }

  @Test
  void buttonSizeControlRestagesTheButton() {
    final ComponentWorkbench workbench = workbenchOf("Button");
    final ElwhaSelectField<?> sizes = select(workbench, "Size");

    for (final ButtonSize size : ButtonSize.values()) {
      choose(sizes, size);

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

    choose(select(workbench, "Variant"), ButtonVariant.OUTLINED);

    assertThat(workbench.codeFor("Component"))
        .as("the equivalent Java is regenerated with the component, not left stale")
        .contains("ButtonVariant.OUTLINED");
  }

  @Test
  void illegalSelectableTextPairingIsCoercedRatherThanThrown() {
    final ComponentWorkbench workbench = workbenchOf("Button");
    final ElwhaSelectField<?> variants = select(workbench, "Variant");
    final ElwhaSelectField<?> modes = select(workbench, "Interaction mode");

    choose(variants, ButtonVariant.TEXT);
    choose(modes, com.owspfm.elwha.button.ButtonInteractionMode.SELECTABLE);

    assertThat(modes.getSelectedValue())
        .as(
            "#183 — SELECTABLE + TEXT is illegal, so the workbench walks the control back to a "
                + "legal pairing instead of letting the demo throw")
        .isEqualTo(com.owspfm.elwha.button.ButtonInteractionMode.CLICKABLE);
    assertThat(workbench.stage()).isNotNull();
  }

  @Test
  void chipVariantControlRestagesTheChip() {
    final ComponentWorkbench workbench = workbenchOf("Chip");
    final ElwhaSelectField<?> variants = select(workbench, "Variant");

    for (final ChipVariant variant : ChipVariant.values()) {
      choose(variants, variant);

      assertThat(((ElwhaChip) workbench.stage()).getVariant()).isEqualTo(variant);
    }
  }

  @Test
  void iconButtonVariantControlRestagesTheButton() {
    final ComponentWorkbench workbench = workbenchOf("Icon Button");
    final ElwhaSelectField<?> variants = select(workbench, "Variant");

    for (final IconButtonVariant variant : IconButtonVariant.values()) {
      choose(variants, variant);

      assertThat(((ElwhaIconButton) workbench.stage()).getVariant()).isEqualTo(variant);
    }
  }

  // ------------------------------------------------- the facet rollout (#318)

  /**
   * The hosts that expose an embedded sub-component as a facet, and the segment each one adds. The
   * rollout is a judgment per host rather than blanket coverage, so the roster is asserted by name:
   * a facet quietly dropped from one of these leaves is a regression, and a host that grew one
   * without a recorded call belongs in the audit before it belongs in the switcher.
   */
  static List<Arguments> facetHosts() {
    return List.of(
        Arguments.of("Card", List.of("Media")),
        Arguments.of("App Bar", List.of("Icon buttons")),
        Arguments.of("Nav Rail Destination", List.of("Badge")),
        Arguments.of("Navigation Rail", List.of("Badge")),
        Arguments.of("Tabs", List.of("Badge")),
        Arguments.of("Side Sheet", List.of("Modal")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("facetHosts")
  void aFacetHostCarriesItsSegmentsBetweenTheBookends(
      final String label, final List<String> facets) {
    final List<String> expected = new ArrayList<>();
    expected.add("Component");
    expected.addAll(facets);
    expected.add("Surface");

    assertThat(ShowcaseFixture.segmentsOf(workbenchOf(label)))
        .as("%s — #318: the sub-component facets this host was judged to warrant", label)
        .isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("facetHosts")
  void everyFacetCarriesRealControlSurface(final String label, final List<String> facets) {
    final ComponentWorkbench workbench = workbenchOf(label);
    final List<JComponent> columns = workbench.controlColumns();

    for (int i = 0; i < facets.size(); i++) {
      final JComponent column = columns.get(i + 1);
      assertThat(ShowcaseFixture.descendants(column))
          .as(
              "%s — the %s facet column is empty, so its segment shows a blank card",
              label, facets.get(i))
          .hasSizeGreaterThan(1);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("facetHosts")
  void everyFacetRendersEquivalentJava(final String label, final List<String> facets) {
    final ComponentWorkbench workbench = workbenchOf(label);

    for (final String facet : facets) {
      assertThat(workbench.codeFor(facet))
          .as("%s — the %s facet tracks its own snippet, or the code view goes blank", label, facet)
          .isNotBlank();
    }
  }

  @Test
  void cardMediaFacetReachesTheStagedCardsMediaSlot() {
    final ComponentWorkbench workbench = workbenchOf("Card");
    choose(select(workbench, "Media"), namedChoice(select(workbench, "Media"), "IMAGE"));
    final ElwhaSelectField<?> aspect = facetSelect(workbench, 0, "Aspect ratio");

    choose(aspect, namedChoice(aspect, "1:1"));

    final ElwhaCardMedia media = ShowcaseFixture.findFirst(workbench.stage(), ElwhaCardMedia.class);
    assertThat(media).as("#318 — the Media facet needs a media slot to drive").isNotNull();
    assertThat(media.getAspectRatio())
        .as("the facet does not merely record a choice; it reaches the rebuilt card's own media")
        .isEqualTo(1.0);
  }

  /**
   * A facet host's switcher has to fit the column it lives in. A connected group never shrinks a
   * segment below its own content width — {@code connectedSegmentWidthPx} floors the per-segment
   * share at the widest segment's preferred width in <em>both</em> resize modes, deliberately, so a
   * segment label is never truncated. Measured at {@code XS}, the widest segment is the scaffold's
   * own {@code Component} bookend (its icon plus a nine-character word, 124 px), which puts three
   * segments at 376 px and a fourth at 502 px against a 480 px column. {@code BorderLayout.WEST}
   * then hands the group its full preferred width and the region clips what runs past the edge:
   * nothing throws, nothing logs, the last segment simply loses its end.
   *
   * <p><strong>Ruling (#441).</strong> The column stays at 480. The 502 px is not a control that
   * cannot fit — it is four times a label width — and widening the column would take 40 px of stage
   * off every component leaf in the catalog to buy headroom no leaf uses. Three segments (one
   * facet) is the ceiling, and {@link #everyWorkbenchsSwitcherFitsTheControlsColumn} asserts it
   * across the whole catalog so a future host cannot rediscover the clip by hand.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("facetHosts")
  void aFacetHostsSwitcherFitsTheControlsColumn(final String label, final List<String> facets) {
    final ElwhaButtonGroup switcher =
        ShowcaseFixture.findFirst(workbenchOf(label), ElwhaButtonGroup.class);

    assertThat(switcher.getPreferredSize().width)
        .as(
            "%s — %d segments overflow the %d px controls column and the last one clips",
            label, facets.size() + 2, ComponentWorkbench.CONTROLS_WIDTH)
        .isLessThanOrEqualTo(ComponentWorkbench.CONTROLS_WIDTH - SWITCHER_BAR_PADDING);
  }

  /**
   * The same ceiling, swept over every component leaf rather than the hand-kept facet-host list —
   * so a leaf that gains a facet is covered the day it gains one, without anyone remembering to add
   * it here.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("workbenchLeaves")
  void everyWorkbenchsSwitcherFitsTheControlsColumn(final String label) {
    final ElwhaButtonGroup switcher =
        ShowcaseFixture.findFirst(workbenchOf(label), ElwhaButtonGroup.class);

    assertThat(switcher.getPreferredSize().width)
        .as(
            "%s — #441: the controls column is %d px and a connected group will not shrink a"
                + " segment below its label, so the switcher must be kept inside it",
            label, ComponentWorkbench.CONTROLS_WIDTH)
        .isLessThanOrEqualTo(ComponentWorkbench.CONTROLS_WIDTH - SWITCHER_BAR_PADDING);
  }

  static List<String> containerLeaves() {
    final List<String> labels = new ArrayList<>();
    for (final String label : ShowcaseFixture.leafLabels()) {
      if (ShowcaseFixture.findFirst(ShowcaseFixture.surfaceOf(label), ContainerWorkbench.class)
          != null) {
        labels.add(label);
      }
    }
    return labels;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("containerLeaves")
  void everyContainerLeafShowsItsEquivalentJava(final String label) {
    final ContainerWorkbench workbench =
        ShowcaseFixture.findFirst(ShowcaseFixture.surfaceOf(label), ContainerWorkbench.class);

    assertThat(workbench.codeView().code())
        .as(
            "%s — #582: a container leaf owes the same equivalent-Java answer a component leaf"
                + " does, and owes it from the moment it opens",
            label)
        .isNotBlank();
  }

  @Test
  void everyContainerLeafIsWiredToItsWorkbench() {
    assertThat(containerLeaves())
        .as("#582 — the four Containers leaves are the scaffold's whole population")
        .hasSize(4);
  }

  @Test
  void appBarIconButtonFacetReachesEveryButtonOnTheBar() {
    final ComponentWorkbench workbench = workbenchOf("App Bar");

    choose(facetSelect(workbench, 0, "Variant"), IconButtonVariant.FILLED_TONAL);

    final List<ElwhaIconButton> buttons =
        ShowcaseFixture.findAll(workbench.stage(), ElwhaIconButton.class);
    assertThat(buttons).as("the bar stages a nav button and its actions").isNotEmpty();
    assertThat(buttons)
        .as("#318 — one treatment across the whole bar, navigation button included")
        .allMatch(button -> button.getVariant() == IconButtonVariant.FILLED_TONAL);
  }

  // A select in the facet column at {@code facetIndex}, found by the caption it paints. Facet
  // columns nest their editor a few levels deep, and a caption can repeat across columns ("Variant"
  // means the app bar's on the Component segment and the icon button's on the facet), so the column
  // is named rather than searched for.
  private static ElwhaSelectField<?> facetSelect(
      final ComponentWorkbench workbench, final int facetIndex, final String caption) {
    final JComponent column = workbench.controlColumns().get(facetIndex + 1);
    for (final ElwhaSelectField<?> candidate :
        ShowcaseFixture.findAll(column, ElwhaSelectField.class)) {
      if (caption.equals(ShowcaseFixture.labelOf(candidate))) {
        return candidate;
      }
    }
    throw new AssertionError("no " + caption + " control in facet column " + facetIndex);
  }

  private static Object namedChoice(final ElwhaSelectField<?> select, final String name) {
    for (final Object choice : select.getOptions()) {
      if (name.equals(String.valueOf(choice))) {
        return choice;
      }
    }
    throw new AssertionError("no " + name + " choice on this control");
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

  private static void restore(final ElwhaSelectField<?> select, final Object original) {
    if (original != null) {
      choose(select, original);
    }
  }

  // A select is generic in its option type, and the sweeps walk it as ElwhaSelectField<?> — every
  // value handed back comes from this same field's own getOptions(), so the cast holds by
  // construction and setSelectedValue's not-among-the-options throw is unreachable from here.
  @SuppressWarnings("unchecked")
  private static void choose(final ElwhaSelectField<?> select, final Object value) {
    ((ElwhaSelectField<Object>) select).setSelectedValue(value);
  }

  private static void setChecked(final ElwhaCheckbox toggle, final boolean checked) {
    if (toggle.isChecked() != checked) {
      toggle.doClick();
    }
  }

  private static ElwhaSelectField<?> select(
      final ComponentWorkbench workbench, final String label) {
    final Component control = ShowcaseFixture.controlLabelled(workbench.controls(), label);
    assertThat(control).as("the %s control", label).isInstanceOf(ElwhaSelectField.class);
    return (ElwhaSelectField<?>) control;
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
