package com.owspfm.elwha.selectfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A accessibility coverage for {@link ElwhaSelectField} — the combobox approximation Swing has
 * no role for. The expanded/collapsed state is announced twice over: by the trailing arrow (which
 * is a real {@code ElwhaIconButton}, so it brings Button semantics) and, in the editable combo, by
 * the typeable editor itself, which is where AT actually sits.
 *
 * <p>Also pins the cached-item lifecycle that keeps a reopened menu clean: items are reused across
 * opens and are removed without a {@code mouseExited}, so the teardown has to reset the transient
 * interaction chrome itself.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSelectFieldAccessibilityTest {

  private static final List<String> PLANETS = List.of("Mercury", "Venus", "Earth", "Mars");

  private static ElwhaSelectField<String> planets() {
    final ElwhaSelectField<String> select = ElwhaSelectField.filled("Planet");
    select.setOptions(PLANETS);
    return select;
  }

  private static ElwhaTextField embedded(final ElwhaSelectField<String> select) {
    return (ElwhaTextField) select.getComponent(0);
  }

  private static ElwhaIconButton arrow(final ElwhaSelectField<String> select) {
    return embedded(select).getTrailingIconButton();
  }

  /** Records the accessible state-change announcements fired on a context. */
  private static List<Object> watchStates(final AccessibleContext context) {
    final List<Object> announced = new ArrayList<>();
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())) {
            announced.add(event.getNewValue());
          }
        });
    return announced;
  }

  // ---------------------------------------------------------- the arrow

  @Test
  void theArrowIsARealButtonSoItBringsItsOwnRole() {
    final ElwhaSelectField<String> select = planets();

    assertThat(arrow(select).getAccessibleContext().getAccessibleRole())
        .as("the open/close affordance announces as a push button")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void expandingAnnouncesTheExpandedStateOnTheArrow() {
    final ElwhaSelectField<String> select = planets();
    final List<Object> announced = watchStates(arrow(select).getAccessibleContext());

    select.applyExpandedState(true);

    assertThat(announced)
        .as("opening the options is a state change AT must hear")
        .contains(AccessibleState.EXPANDED);
  }

  @Test
  void collapsingAnnouncesTheCollapsedState() {
    final ElwhaSelectField<String> select = planets();
    select.applyExpandedState(true);
    final List<Object> announced = watchStates(arrow(select).getAccessibleContext());

    select.applyExpandedState(false);

    assertThat(announced).as("and so is closing them").contains(AccessibleState.COLLAPSED);
  }

  // --------------------------------------------------------- the editor

  @Test
  void theEditableComboDescribesItsOwnKeyboardContract() {
    final ElwhaSelectField<String> select = planets();

    select.setEditable(true);

    assertThat(embedded(select).getEditor().getAccessibleContext().getAccessibleDescription())
        .as("a typeable field that also owns a popup has to say so")
        .isEqualTo("Editable combo box. Typing filters the options; Down opens them.");
  }

  @Test
  void leavingEditableModeDropsThatDescription() {
    final ElwhaSelectField<String> select = planets();
    select.setEditable(true);

    select.setEditable(false);

    assertThat(embedded(select).getEditor().getAccessibleContext().getAccessibleDescription())
        .as("a pure select is not a combo box and must not claim to be one")
        .isNull();
  }

  @Test
  void anEditableCombosEditorAnnouncesTheExpandedStateItself() {
    final ElwhaSelectField<String> select = planets();
    select.setEditable(true);
    final List<Object> announced = watchStates(embedded(select).getEditor().getAccessibleContext());

    select.applyExpandedState(true);

    assertThat(announced)
        .as("focus stays in the editor while the menu is open, so the editor is what AT is on")
        .contains(AccessibleState.EXPANDED);
  }

  @Test
  void aPureSelectsEditorStaysQuietAboutExpansion() {
    final ElwhaSelectField<String> select = planets();
    final List<Object> announced = watchStates(embedded(select).getEditor().getAccessibleContext());

    select.applyExpandedState(true);

    assertThat(announced)
        .as("in a pure select focus moves to the menu, so the editor has nothing to announce")
        .isEmpty();
  }

  // ---------------------------------------------------------- the items

  @Test
  void theOptionsAnnounceAsMenuItems() {
    final ElwhaSelectField<String> select = planets();

    assertThat(select.optionsMenu().getItems().get(0).getAccessibleContext().getAccessibleRole())
        .as("options carry the menu-item role")
        .isEqualTo(AccessibleRole.MENU_ITEM);
  }

  @Test
  void theSelectedOptionCarriesTheSelectedState() {
    final ElwhaSelectField<String> select = planets();

    select.setSelectedValue("Mars");

    final ElwhaMenuItem mars =
        select.optionsMenu().getItems().stream()
            .filter(item -> "Mars".equals(item.getLabel()))
            .findFirst()
            .orElseThrow();
    assertThat(
            mars.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED))
        .as("what is marked on screen is marked to AT")
        .isTrue();
  }

  @Test
  void everyToggledOptionInAMultiSelectCarriesTheState() {
    final ElwhaSelectField<String> select = planets();
    select.setMultiSelect(true);

    select.setSelectedValues(List.of("Venus", "Mars"));

    assertThat(select.optionsMenu().getSelectedItems())
        .as("a multi-select marks every chosen option, not just the first")
        .hasSize(2);
  }

  // ------------------------------------------------- cached-item chrome

  @Test
  void tearingDownACachedItemResetsItsHoverChromeSoAReopenIsClean() {
    final ElwhaSelectField<String> select = planets();
    final ElwhaMenuItem item = select.optionsMenu().getItems().get(0);
    final Dimension size = item.getPreferredSize();
    final Color resting = probe(item, size);

    Input.enter(item, 4, size.height / 2);
    assertThat(probe(item, size))
        .as("hovering an option tints it — otherwise this test proves nothing")
        .isNotEqualTo(resting);

    item.removeNotify();

    assertThat(probe(item, size))
        .as(
            "items are cached across opens and torn down without a mouseExited, so teardown has to"
                + " clear the hover itself or the next open paints a stale highlight")
        .isEqualTo(resting);
  }

  /** The item's container fill, sampled clear of the label's glyph box. */
  private static Color probe(final ElwhaMenuItem item, final Dimension size) {
    return new Color(Pixels.render(item, size.width, size.height).getRGB(2, size.height / 2), true);
  }
}
