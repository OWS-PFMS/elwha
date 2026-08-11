package com.owspfm.elwha.navrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.navrail.ElwhaNavigationRail.OverflowMode;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the trailing-actions overflow — design doc §16. The three modes and the
 * height-driven predicate that drives {@code WHEN_NEEDED}, the entry point that replaces the stack,
 * the menu built from the collapsed actions, and the routing that sends a row's activation back to
 * the icon button the consumer passed rather than to a fork of its handlers.
 *
 * <p>Heights are expressed relative to the rail's own {@linkplain
 * ElwhaNavigationRail#getPreferredSize() preferred height} rather than as literals: the preferred
 * height <em>is</em> the height at which the full stack exactly fits, so it is the collapse
 * threshold, and pinning the tests to it keeps them honest if the paddings ever move.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaNavigationRailOverflowTest {

  private static final String ACTIVATE = "elwhaiconbutton.activate";

  /**
   * Enough of a squeeze that the full stack no longer clears the bottom pad, but not so much that
   * the destinations themselves stop fitting — past that point the rail is short of room for its
   * content, not short of an affordance, and nothing about overflow can be read from the geometry.
   */
  private static final int TIGHT = -60;

  private HeadlessHost host;

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(700, 900);
  }

  // ------------------------------------------------------------------ setup

  private static ElwhaIconButton action(final String glyph, final String name) {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.symbol(glyph).unselected());
    if (name != null) {
      button.getAccessibleContext().setAccessibleName(name);
    }
    return button;
  }

  /** {@code count} named trailing actions — "Action 0", "Action 1", … in slot order. */
  private static List<ElwhaIconButton> actions(final int count) {
    final List<ElwhaIconButton> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(action("help", "Action " + i));
    }
    return list;
  }

  /** A collapsed-variant rail with four destinations and {@code actionCount} trailing actions. */
  private static ElwhaNavigationRail rail(final int actionCount) {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.setPrimary(RailFixture.destinations(4));
    rail.setTrailingActions(actions(actionCount));
    return rail;
  }

  /**
   * Lays the rail out at {@code preferred height + delta}. A delta of {@code 0} is the height the
   * rail asked for — exactly enough for the whole stack; anything negative squeezes it.
   */
  private static ElwhaNavigationRail layoutAt(final ElwhaNavigationRail rail, final int delta) {
    rail.setSize(rail.getPreferredSize().width, rail.getPreferredSize().height + delta);
    rail.doLayout();
    return rail;
  }

  /** The same, with the rail mounted under a root pane so its menu has somewhere to open. */
  private ElwhaNavigationRail mountedAt(final ElwhaNavigationRail rail, final int delta) {
    host.rootPane().getContentPane().add(rail);
    return layoutAt(rail, delta);
  }

  private static List<String> rowLabels(final ElwhaMenu menu) {
    final List<String> labels = new ArrayList<>();
    for (final ElwhaMenuItem row : menu.getItems()) {
      labels.add(row.getLabel());
    }
    return labels;
  }

  private static void click(final ElwhaMenuItem row) {
    row.setSize(row.getPreferredSize());
    Input.click(row, 20, row.getHeight() / 2);
  }

  // ------------------------------------------------------------- the modes

  @Test
  void whenNeededIsTheDefault() {
    assertThat(ElwhaNavigationRail.collapsed().getOverflowMode())
        .as("§16.1 — adaptive by default: a rail with the height it asked for looks unchanged")
        .isEqualTo(OverflowMode.WHEN_NEEDED);
  }

  @Test
  void aNullModeIsRefused() {
    assertThatNullPointerException()
        .isThrownBy(() -> ElwhaNavigationRail.collapsed().setOverflowMode(null));
  }

  @Test
  void whenNeededLeavesTheStackAloneWhileItFits() {
    final ElwhaNavigationRail rail = layoutAt(rail(4), 0);

    assertThat(rail.isTrailingActionsCollapsed())
        .as("§16.1 — at the height the rail asked for, the whole stack fits and nothing collapses")
        .isFalse();
    assertThat(rail.getOverflowButton()).as("so there is no entry point to show").isNull();
    assertThat(rail.getTrailingActions()).allMatch(Component::isVisible);
  }

  @Test
  void whenNeededCollapsesOncePastTheHeightItAskedFor() {
    final ElwhaNavigationRail rail = layoutAt(rail(4), -1);

    assertThat(rail.isTrailingActionsCollapsed())
        .as(
            "§16.1 — the preferred height is exactly the threshold: one pixel short of it, the "
                + "stack no longer clears the destinations")
        .isTrue();
    assertThat(rail.getOverflowButton()).as("and the entry point stands in for it").isNotNull();
    assertThat(rail.getTrailingActions())
        .as("§16.1 — a collapsed action must not paint or take a click behind the entry point")
        .noneMatch(Component::isVisible);
  }

  @Test
  void whenNeededNeverCollapsesALoneAction() {
    final ElwhaNavigationRail rail = layoutAt(rail(1), TIGHT);

    assertThat(rail.isTrailingActionsCollapsed())
        .as(
            "§16.1 — an entry point is the same height as the row it hides, so collapsing one "
                + "action costs a click and saves nothing")
        .isFalse();
  }

  @Test
  void alwaysCollapsesEvenWithRoomToSpare() {
    final ElwhaNavigationRail rail = rail(4);
    rail.setOverflowMode(OverflowMode.ALWAYS);
    layoutAt(rail, 400);

    assertThat(rail.isTrailingActionsCollapsed())
        .as("§16.1 — ALWAYS buys one consistent affordance at every window height")
        .isTrue();
  }

  @Test
  void alwaysCollapsesEvenALoneAction() {
    final ElwhaNavigationRail rail = rail(1);
    rail.setOverflowMode(OverflowMode.ALWAYS);
    layoutAt(rail, 400);

    assertThat(rail.isTrailingActionsCollapsed())
        .as("§16.1 — consistency is the whole point of asking for ALWAYS")
        .isTrue();
  }

  @Test
  void neverKeepsTheStackEvenWhenItCannotFit() {
    final ElwhaNavigationRail rail = rail(4);
    rail.setOverflowMode(OverflowMode.NEVER);
    layoutAt(rail, -300);

    assertThat(rail.isTrailingActionsCollapsed())
        .as("§16.1 — NEVER preserves the pre-#238 degradation: the stack slides down and clips")
        .isFalse();
    assertThat(rail.getTrailingActions()).allMatch(Component::isVisible);
  }

  @Test
  void anEmptySlotCollapsesInNoMode() {
    for (final OverflowMode mode : OverflowMode.values()) {
      final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
      rail.setPrimary(RailFixture.destinations(4));
      rail.setOverflowMode(mode);
      layoutAt(rail, -300);

      assertThat(rail.isTrailingActionsCollapsed())
          .as("%s — there is nothing to collapse, so there is no entry point either", mode)
          .isFalse();
      assertThat(rail.getOverflowButton()).isNull();
    }
  }

  @Test
  void growingTheRailBackHandsTheActionsBack() {
    final ElwhaNavigationRail rail = rail(4);
    layoutAt(rail, TIGHT);
    assertThat(rail.isTrailingActionsCollapsed()).isTrue();

    layoutAt(rail, 0);

    assertThat(rail.isTrailingActionsCollapsed())
        .as("§16.1 — the collapse is recomputed every pass, so a resize is reversible")
        .isFalse();
    assertThat(rail.getOverflowButton()).isNull();
    assertThat(rail.getTrailingActions()).allMatch(Component::isVisible);
  }

  @Test
  void aCollapseIsReportedAsAPropertyChange() {
    final ElwhaNavigationRail rail = rail(4);
    final List<Object> seen = new ArrayList<>();
    rail.addPropertyChangeListener(
        ElwhaNavigationRail.PROPERTY_TRAILING_COLLAPSED, e -> seen.add(e.getNewValue()));

    layoutAt(rail, TIGHT);
    layoutAt(rail, 0);

    assertThat(seen)
        .as(
            "§16.1 — a height-driven collapse is invisible to a consumer that is not told about "
                + "it, and it fires on the change only, never per pass")
        .containsExactly(true, false);
  }

  @Test
  void handingBackACollapsedActionRestoresItsVisibility() {
    final ElwhaNavigationRail rail = rail(4);
    final List<ElwhaIconButton> original = rail.getTrailingActions();
    layoutAt(rail, TIGHT);

    rail.setTrailingActions(null);

    assertThat(original)
        .as(
            "a button the rail hid on the consumer's behalf must not leave the slot still "
                + "invisible — the consumer never made it so")
        .allMatch(Component::isVisible);
    assertThat(rail.isTrailingActionsCollapsed())
        .as("and an empty slot is not a collapsed one")
        .isFalse();
  }

  // --------------------------------------------------------- reserved height

  @Test
  void whenNeededReservesTheWholeStack() {
    final ElwhaNavigationRail one = rail(1);
    final ElwhaNavigationRail four = rail(4);

    assertThat(four.getPreferredSize().height)
        .as("§16.1 — the rail wants room for its actions; collapsing is what it settles for")
        .isGreaterThan(one.getPreferredSize().height);
  }

  @Test
  void alwaysReservesOneRowOnly() {
    final ElwhaNavigationRail four = rail(4);
    final int stacked = four.getPreferredSize().height;
    four.setOverflowMode(OverflowMode.ALWAYS);

    assertThat(four.getPreferredSize().height)
        .as("§16.1 — a stack ALWAYS will never show is not space the rail can ask the host for")
        .isLessThan(stacked)
        .isEqualTo(rail(1).getPreferredSize().height);
  }

  // ------------------------------------------------------- the entry point

  @Test
  void anEntryPointIsANamedPushButton() {
    final ElwhaIconButton entry = layoutAt(rail(4), TIGHT).getOverflowButton();

    assertThat(entry.getAccessibleContext().getAccessibleName())
        .as("§16.6 — an icon-only entry point is silent to a screen reader without one")
        .isEqualTo("More actions");
    assertThat(entry.getAccessibleContext().getAccessibleRole())
        .as(
            "§16.6 — CLICKABLE, not SELECTABLE: a toggle would stay painted 'on' after the menu "
                + "light-dismissed, because a menu never mutates its trigger")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
    assertThat(entry.getInteractionMode()).isEqualTo(IconButtonInteractionMode.CLICKABLE);
  }

  @Test
  void anEntryPointTakesTheStacksPlaceAtTheFootOfTheRail() {
    final ElwhaNavigationRail rail = layoutAt(rail(4), TIGHT);
    final ElwhaIconButton entry = rail.getOverflowButton();

    assertThat(entry.getY() + entry.getHeight())
        .as(
            "§3 — the entry point is bottom-anchored inside the rail's pad, like the stack it "
                + "replaces")
        .isEqualTo(rail.getHeight() - ElwhaNavigationRail.CHROME_PAD_PX);
    assertThat(entry.getX())
        .as("and centred in the Collapsed column, like every other chrome button")
        .isEqualTo((rail.getWidth() - entry.getWidth()) / 2);
  }

  @Test
  void tabReachesTheEntryPointInsteadOfTheHiddenActions() {
    final ElwhaNavigationRail rail = rail(4);
    layoutAt(rail, TIGHT);

    final List<Component> walked = tabWalk(rail);

    assertThat(walked)
        .as("§16.5 — Tab must not walk into buttons the collapse took off screen")
        .doesNotContainAnyElementsOf(rail.getTrailingActions())
        .endsWith(rail.getOverflowButton());
  }

  @Test
  void tabWalksEveryActionWhileTheStackStands() {
    final ElwhaNavigationRail rail = rail(4);
    layoutAt(rail, 0);

    assertThat(tabWalk(rail))
        .as("§10.2 — uncollapsed, the trailing actions are the last stops, in slot order")
        .endsWith(rail.getTrailingActions().toArray(new Component[0]));
  }

  private static List<Component> tabWalk(final ElwhaNavigationRail rail) {
    final List<Component> walked = new ArrayList<>();
    Component stop = rail.getFocusTraversalPolicy().getFirstComponent(rail);
    while (stop != null && walked.size() < 20) {
      walked.add(stop);
      stop = rail.getFocusTraversalPolicy().getComponentAfter(rail, stop);
    }
    return walked;
  }

  // -------------------------------------------------------------- the menu

  @Test
  void aMenuListsEveryCollapsedActionInSlotOrder() {
    final ElwhaNavigationRail rail = layoutAt(rail(4), TIGHT);

    assertThat(rowLabels(rail.buildOverflowMenu()))
        .as("§16.2 — one row per collapsed action, in the order the consumer slotted them")
        .containsExactly("Action 0", "Action 1", "Action 2", "Action 3");
  }

  @Test
  void aRowIsDisabledWhenItsActionIs() {
    final ElwhaNavigationRail rail = rail(3);
    rail.getTrailingActions().get(1).setEnabled(false);
    layoutAt(rail, TIGHT);

    final List<ElwhaMenuItem> rows = rail.buildOverflowMenu().getItems();

    assertThat(rows.get(1).isEnabled())
        .as(
            "§16.2 — a disabled action produces a disabled row, not a missing one: the action "
                + "still exists and hiding it would misreport the rail's contents")
        .isFalse();
    assertThat(rows.get(0).isEnabled()).isTrue();
    assertThat(rows.get(2).isEnabled()).isTrue();
  }

  @Test
  void rowsAreRebuiltPerOpen() {
    final ElwhaNavigationRail rail = layoutAt(rail(2), TIGHT);
    rail.buildOverflowMenu();

    rail.getTrailingActions().get(0).getAccessibleContext().setAccessibleName("Renamed");

    assertThat(rowLabels(rail.buildOverflowMenu()))
        .as("§16.2 — an action re-labelled between opens is current the next time it appears")
        .containsExactly("Renamed", "Action 1");
  }

  @Test
  void labelsFallThroughNameThenTooltipThenTheGenericLiteral() {
    final ElwhaIconButton named = action("help", "Named");
    final ElwhaIconButton tipped = action("info", null);
    tipped.setToolTipText("Tipped");
    final ElwhaIconButton bare = action("settings", null);
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.setPrimary(RailFixture.destinations(4));
    rail.setTrailingActions(List.of(named, tipped, bare));
    layoutAt(rail, TIGHT);

    assertThat(rowLabels(rail.buildOverflowMenu()))
        .as(
            "§16.3 — the label is the action's accessible name, and an icon button already "
                + "resolves that through name → tooltip → component name → the generic literal")
        .containsExactly("Named", "Tipped", "Icon button");
  }

  @Test
  void aRowsIconIsACopyRatherThanTheButtonsLiveInstance() {
    final ElwhaNavigationRail rail = layoutAt(rail(2), TIGHT);
    final ElwhaIconButton first = rail.getTrailingActions().get(0);

    final ElwhaMenuItem row = rail.buildOverflowMenu().getItems().get(0);

    assertThat(row.getLeadingIcon())
        .as(
            "§16.3 — a row stamps its own colour filter onto any FlatSVGIcon it is handed, so "
                + "the button's live icon must never reach one (#197)")
        .isNotSameAs(first.getIcon());
    assertThat(row.getLeadingIcon().getIconHeight())
        .as("§16.3 — and the copy is taken at the row's 20 dp icon size, not the button's 24")
        .isEqualTo(ElwhaNavigationRail.OVERFLOW_MENU_ICON_PX);
  }

  @Test
  void anEmptySlotBuildsNoMenu() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.setPrimary(RailFixture.destinations(4));

    assertThat(rail.buildOverflowMenu()).as("there is nothing to list").isNull();
  }

  // ---------------------------------------------------------- the routing

  @Test
  void activatingARowClicksTheActionItStandsFor() {
    final ElwhaNavigationRail rail = layoutAt(rail(3), TIGHT);
    final ElwhaIconButton second = rail.getTrailingActions().get(1);
    final List<Object> sources = new ArrayList<>();
    second.addActionListener(e -> sources.add(e.getSource()));

    click(rail.buildOverflowMenu().getItems().get(1));

    assertThat(sources)
        .as(
            "§16.2 — the row stands in for the action; the consumer's own listener fires with "
                + "the original button as the event source, not with a fork of the handler")
        .containsExactly(second);
  }

  @Test
  void activatingARowTogglesASelectableAction() {
    final ElwhaIconButton toggle = action("dark_mode", "Theme");
    toggle.setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.setPrimary(RailFixture.destinations(4));
    rail.setTrailingActions(List.of(toggle, action("help", "Help")));
    layoutAt(rail, TIGHT);

    click(rail.buildOverflowMenu().getItems().get(0));

    assertThat(toggle.isSelected())
        .as("§16.2 — a SELECTABLE action toggles exactly as it would have in the rail")
        .isTrue();
  }

  @Test
  void aDisabledActionsRowRoutesNothing() {
    final ElwhaNavigationRail rail = rail(2);
    final ElwhaIconButton disabled = rail.getTrailingActions().get(0);
    final List<Object> fired = new ArrayList<>();
    disabled.addActionListener(e -> fired.add(e));
    disabled.setEnabled(false);
    layoutAt(rail, TIGHT);

    click(rail.buildOverflowMenu().getItems().get(0));

    assertThat(fired).as("§16.2 — inert through the menu exactly as it is in the rail").isEmpty();
  }

  // ------------------------------------------------------------- the popup

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"pressed SPACE", "pressed ENTER"})
  void anEntryPointOpensTheMenuFromTheKeyboard(final String keystroke) {
    final ElwhaNavigationRail rail = mountedAt(rail(4), TIGHT);

    Input.pressBoundKey(rail.getOverflowButton(), keystroke, ACTIVATE);

    assertThat(host.mounted())
        .as("§16.5 — the entry point is an ordinary icon button, so its own bindings open it")
        .isNotEmpty();
  }

  @Test
  void anOpenMenuCarriesItsOwnNavigationAndDismissBindings() {
    final ElwhaNavigationRail rail = mountedAt(rail(4), TIGHT);

    Input.pressBoundKey(rail.getOverflowButton(), "pressed SPACE", ACTIVATE);

    assertThat(host.mounted())
        .as(
            "§16.5 — ↑ / ↓ within the popup and Escape to close are the menu's own bindings; "
                + "overflow inherits them rather than re-implementing them")
        .anySatisfy(
            mounted ->
                assertThat(boundKeys((JComponent) mounted))
                    .contains(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ESCAPE));
  }

  private static List<Integer> boundKeys(final JComponent surface) {
    final List<Integer> bound = new ArrayList<>();
    for (final int code : List.of(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ESCAPE)) {
      if (surface.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(code, 0))
          != null) {
        bound.add(code);
      }
    }
    return bound;
  }

  @Test
  void growingTheRailBackClosesTheMenuItAnchored() {
    final ElwhaNavigationRail rail = mountedAt(rail(4), TIGHT);
    Input.pressBoundKey(rail.getOverflowButton(), "pressed SPACE", ACTIVATE);
    assertThat(host.mounted()).isNotEmpty();

    layoutAt(rail, 0);

    assertThat(host.mounted())
        .as("an open menu must not outlive the entry point it was anchored to")
        .isEmpty();
  }
}
