package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaMenu} as a builder and as an overlay host: what the builder
 * accepts and refuses, the z-band and light-dismiss posture it pins, the close-cause reporting, the
 * {@linkplain ElwhaMenu.Builder#focusHome(java.awt.Component) focus-home} widening that keeps an
 * editable combobox typeable while its menu is open, the live filter, and the roving-highlight
 * surface an external focus home drives.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaMenuHostTest {

  private HeadlessHost host;
  private final List<ElwhaMenu> opened = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(700, 500);
  }

  @AfterEach
  void closeEveryMenu() {
    for (final ElwhaMenu menu : opened) {
      menu.close();
    }
    opened.clear();
  }

  private ElwhaMenu open(final ElwhaMenu menu) {
    opened.add(menu);
    menu.open(host.anchor());
    return menu;
  }

  private static ElwhaMenu.Builder threeItems() {
    return ElwhaMenu.builder()
        .addItem(ElwhaMenuItem.of("Cut"))
        .addItem(ElwhaMenuItem.of("Copy"))
        .addItem(ElwhaMenuItem.of("Paste"));
  }

  // ---------------------------------------------------------------- separator

  private static ElwhaMenu.Builder twoGappedGroups(final int perGroup) {
    final ElwhaMenu.Builder builder =
        ElwhaMenu.builder().layout(Layout.GROUPED).separator(Separator.GAP);
    for (int group = 0; group < 2; group++) {
      if (group > 0) {
        builder.addGroup();
      }
      for (int i = 0; i < perGroup; i++) {
        builder.addItem(ElwhaMenuItem.of("Group " + group + " item " + i));
      }
    }
    return builder;
  }

  @Test
  void oneScrollableOpenDoesNotCostAMenuItsGapsForever() {
    final ElwhaMenu menu = twoGappedGroups(4).build();
    opened.add(menu);

    host.resize(700, 160);
    menu.open(host.anchor());
    assertThat(menu.effectiveSeparator())
        .as("M3 forbids gaps in a scrollable menu, so a short window forces the divider")
        .isEqualTo(Separator.DIVIDER);
    menu.close();

    host.resize(700, 900);
    menu.open(host.anchor());

    assertThat(menu.effectiveSeparator())
        .as(
            "the downgrade belongs to that one presentation — reopened with room to breathe, the"
                + " menu renders the gaps it was built with")
        .isEqualTo(Separator.GAP);
  }

  @Test
  void aMenuThatNeverScrollsKeepsItsAuthoredSeparator() {
    final ElwhaMenu menu = twoGappedGroups(2).build();

    open(menu);

    assertThat(menu.effectiveSeparator())
        .as("nothing to downgrade when the column fits")
        .isEqualTo(Separator.GAP);
  }

  // ------------------------------------------------------------------ builder

  @Test
  void aMenuWithNoItemsIsRefused() {
    assertThatIllegalStateException()
        .isThrownBy(() -> ElwhaMenu.builder().build())
        .withMessageContaining("no items");
  }

  @Test
  void everyBuilderSlotRejectsNull() {
    final ElwhaMenu.Builder builder = ElwhaMenu.builder();
    assertThatNullPointerException().isThrownBy(() -> builder.addItem(null));
    assertThatNullPointerException().isThrownBy(() -> builder.layout(null));
    assertThatNullPointerException().isThrownBy(() -> builder.separator(null));
    assertThatNullPointerException().isThrownBy(() -> builder.colorStyle(null));
    assertThatNullPointerException().isThrownBy(() -> builder.selectionMode(null));
  }

  @Test
  void itemsReadBackFlattenedInDisplayOrderAcrossGroups() {
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .addItem(ElwhaMenuItem.of("Cut"))
            .addGroup()
            .addItem(ElwhaMenuItem.of("Copy"))
            .addItem(ElwhaMenuItem.of("Paste"))
            .build();

    assertThat(menu.getItems())
        .extracting(ElwhaMenuItem::getLabel)
        .as("groups flatten into one display order")
        .containsExactly("Cut", "Copy", "Paste");
  }

  @Test
  void startingAGroupSwitchesTheLayoutToGrouped() {
    final ElwhaMenu grouped =
        ElwhaMenu.builder()
            .addItem(ElwhaMenuItem.of("Cut"))
            .addGroup()
            .addItem(ElwhaMenuItem.of("Copy"))
            .build();
    // A GROUPED menu inserts a separator between its groups; a STANDARD one collapses to one slab.
    final JPanel groupedColumn = (JPanel) grouped.renderPreview().getComponent(0);

    assertThat(groupedColumn.getComponentCount())
        .as("asking for a group is asking for GROUPED layout — the separator proves it took")
        .isEqualTo(3);
  }

  @Test
  void anEmptyTrailingGroupIsDroppedRatherThanRenderedAsAStrayGap() {
    final ElwhaMenu menu = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Cut")).addGroup().build();

    assertThat(menu.getItems()).as("the empty group leaves the item set alone").hasSize(1);
    assertThat(((JPanel) menu.renderPreview().getComponent(0)).getComponentCount())
        .as("and contributes no separator chrome")
        .isEqualTo(1);
  }

  @Test
  void itemAccessorHandsBackAnUnmodifiableSnapshot() {
    final ElwhaMenu menu = threeItems().build();

    assertThatCode(() -> menu.getItems().add(ElwhaMenuItem.of("Nope")))
        .as("callers cannot smuggle items in through the accessor")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // -------------------------------------------------------------- host posture

  @Test
  void aMenuMountsOnThePopupBandSoItTopsDialogsAndOverlays() {
    final ElwhaMenu menu = open(threeItems().build());

    assertThat(host.mounted()).as("only the surface mounts — a menu carries no scrim").hasSize(1);
    assertThat(host.layerOf(host.mounted().get(0)))
        .as("a menu sits on the popup band, above dialogs (200) and Elwha overlays (190)")
        .isEqualTo(JLayeredPane.POPUP_LAYER.intValue());
  }

  @Test
  void aMountedSurfaceAnnouncesItselfAsAPopupMenu() {
    open(threeItems().build());

    assertThat(host.mounted().get(0).getAccessibleContext().getAccessibleRole())
        .as("assistive tech hears a popup menu")
        .isEqualTo(AccessibleRole.POPUP_MENU);
    assertThat(host.mounted().get(0).getAccessibleContext().getAccessibleName())
        .as("named for what it is")
        .isEqualTo("Menu");
  }

  @Test
  void closingProgrammaticallyReportsThatCause() {
    final List<MenuDismissCause> causes = new ArrayList<>();
    final ElwhaMenu menu = open(threeItems().onClose(causes::add).build());

    menu.close();

    assertThat(causes)
        .as("an owner-driven close reports itself as programmatic")
        .containsExactly(MenuDismissCause.PROGRAMMATIC);
    assertThat(host.mounted()).as("and the surface is detached").isEmpty();
  }

  @Test
  void closingAMenuThatIsNotOpenIsInert() {
    final List<MenuDismissCause> causes = new ArrayList<>();
    final ElwhaMenu menu = threeItems().onClose(causes::add).build();

    assertThatCode(menu::close).doesNotThrowAnyException();
    assertThat(causes).as("nothing is reported for a menu that was never opened").isEmpty();
  }

  @Test
  void aMenuCanBeReopenedAfterClosing() {
    final ElwhaMenu menu = open(threeItems().build());
    menu.close();

    menu.open(host.anchor());
    opened.add(menu);

    assertThat(host.mounted()).as("a menu instance is reusable across opens").hasSize(1);
  }

  // ------------------------------------------------------------- focus home

  @Test
  void withoutAFocusHomeOnlyTheSurfaceCountsAsInside() {
    final ElwhaMenu menu = open(threeItems().build());

    assertThat(menu.ownsFocus(host.anchor()))
        .as(
            "a plain menu treats a press on its trigger as outside — that is how it"
                + " light-dismisses")
        .isFalse();
  }

  @Test
  void aFocusHomeWidensOwnershipSoTypingInTheFieldNeverDismissesTheMenu() {
    final JPanel field = new JPanel();
    final JTextField editor = new JTextField();
    field.add(editor);
    final ElwhaMenu menu = open(threeItems().focusHome(field).build());

    assertThat(menu.ownsFocus(editor))
        .as("the focus home's hierarchy belongs to the menu, so keystrokes there are not an escape")
        .isTrue();
    assertThat(menu.ownsFocus(host.anchor()))
        .as("while anything outside it still counts as outside")
        .isFalse();
  }

  @Test
  void aFocusHomeTakesInitialFocusInsteadOfTheMenuSurface() {
    final JTextField editor = new JTextField();
    final ElwhaMenu menu = open(threeItems().focusHome(editor).build());

    assertThat(menu.initialFocusTarget())
        .as("the editable-combobox pattern keeps focus in the field, not on the listbox")
        .isSameAs(editor);
  }

  @Test
  void aFocusHomeMenuAlsoBindsEscapeWhereItCanBeReached() {
    final JTextField editor = new JTextField();
    final List<MenuDismissCause> causes = new ArrayList<>();
    open(threeItems().focusHome(editor).onClose(causes::add).build());
    final JComponent surface = (JComponent) host.mounted().get(0);

    final Object windowWide =
        surface
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    assertThat(windowWide)
        .as("the surface binding is unreachable on a menu whose surface never takes focus")
        .isEqualTo("elwha-menu-dismiss-window");

    surface
        .getActionMap()
        .get(windowWide)
        .actionPerformed(new ActionEvent(surface, ActionEvent.ACTION_PERFORMED, "escape"));

    assertThat(causes)
        .as("and it really closes the menu, reporting Escape")
        .containsExactly(MenuDismissCause.ESCAPE);
    assertThat(host.mounted()).as("the surface is detached").isEmpty();
  }

  @Test
  void aPlainMenuKeepsEscapeOnTheSurfaceAlone() {
    open(threeItems().build());
    final JComponent surface = (JComponent) host.mounted().get(0);

    assertThat(
            surface
                .getInputMap(JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)))
        .as("a menu that owns focus reaches its own binding")
        .isEqualTo("elwha-menu-dismiss");
    assertThat(
            surface
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)))
        .as("and gets no window-wide twin, so a submenu chain still collapses one level at a time")
        .isNull();
  }

  // ----------------------------------------------------------- roving highlight

  @Test
  void highlightStartsOnTheFirstItemAndMovesWithWrap() {
    final ElwhaMenu menu = open(threeItems().build());

    assertThat(menu.getHighlightedItem().getLabel())
        .as("an opened menu highlights its first item")
        .isEqualTo("Cut");

    menu.moveHighlight(1);
    assertThat(menu.getHighlightedItem().getLabel())
        .as("moving down walks the list")
        .isEqualTo("Copy");

    menu.moveHighlight(-2);
    assertThat(menu.getHighlightedItem().getLabel())
        .as("and moving off the top wraps to the end")
        .isEqualTo("Paste");
  }

  @Test
  void highlightCanBePointedAtANamedItem() {
    final ElwhaMenu menu = open(threeItems().build());

    menu.highlight(menu.getItems().get(2));

    assertThat(menu.getHighlightedItem().getLabel())
        .as("the combobox's prefix-priority surface moves the highlight directly")
        .isEqualTo("Paste");
  }

  @Test
  void highlightingAnItemThatIsNotVisibleIsIgnored() {
    final ElwhaMenu menu = open(threeItems().build());
    final ElwhaMenuItem hidden = menu.getItems().get(2);
    menu.setVisibleItems(List.of(menu.getItems().get(0), menu.getItems().get(1)));

    menu.highlight(hidden);

    assertThat(menu.getHighlightedItem())
        .as("a filtered-out item cannot take the highlight")
        .isNotSameAs(hidden);
  }

  @Test
  void thereIsNoHighlightWhileTheMenuIsClosed() {
    final ElwhaMenu menu = threeItems().build();

    assertThat(menu.getHighlightedItem()).as("a closed menu highlights nothing").isNull();
    assertThatCode(
            () -> {
              menu.moveHighlight(1);
              menu.activateHighlighted();
            })
        .as("and the keyboard surface is inert rather than throwing")
        .doesNotThrowAnyException();
  }

  @Test
  void activatingTheHighlightFiresThatItemsListeners() {
    final List<String> fired = new ArrayList<>();
    final ElwhaMenuItem copy = ElwhaMenuItem.of("Copy");
    copy.addActionListener(e -> fired.add("Copy"));
    final ElwhaMenu menu =
        open(ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Cut")).addItem(copy).build());

    menu.moveHighlight(1);
    menu.activateHighlighted();

    assertThat(fired)
        .as("Enter on the highlighted row is exactly a click on it")
        .containsExactly("Copy");
  }

  @Test
  void aDisabledItemCanHoldTheHighlightButRefusesToFire() {
    final List<String> fired = new ArrayList<>();
    final ElwhaMenuItem paste = ElwhaMenuItem.of("Paste");
    paste.setEnabled(false);
    paste.addActionListener(e -> fired.add("Paste"));
    final ElwhaMenu menu =
        open(ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Cut")).addItem(paste).build());

    menu.moveHighlight(1);
    menu.activateHighlighted();

    assertThat(menu.getHighlightedItem())
        .as("a disabled row is still reachable by keyboard")
        .isSameAs(paste);
    assertThat(fired).as("but activating it does nothing").isEmpty();
  }

  // ------------------------------------------------------------- live filter

  @Test
  void filteringHidesTheItemsLeftOutAndRestoresThemOnClear() {
    final ElwhaMenu menu = threeItems().build();
    final ElwhaMenuItem copy = menu.getItems().get(1);

    menu.setVisibleItems(List.of(copy));
    assertThat(menu.getItems())
        .filteredOn(item -> item.isVisible())
        .as("only the matching item stays visible")
        .containsExactly(copy);

    menu.setVisibleItems(null);
    assertThat(menu.getItems())
        .as("and a null filter restores every item")
        .allMatch(item -> item.isVisible(), "visible");
  }

  @Test
  void filteringPreservesSelectionMarksOnHiddenItems() {
    final ElwhaMenu menu = threeItems().selectionMode(SelectionMode.MULTI).build();
    final ElwhaMenuItem paste = menu.getItems().get(2);
    paste.setSelected(true);

    menu.setVisibleItems(List.of(menu.getItems().get(0)));

    assertThat(paste.isSelected())
        .as("filtering an item out does not clear its selection")
        .isTrue();
    assertThat(menu.getSelectedItems())
        .as("and the selection snapshot still reports it")
        .containsExactly(paste);
  }

  @Test
  void filteringAnOpenMenuRebuildsTheColumnAndResetsTheHighlight() {
    final ElwhaMenu menu = open(threeItems().build());
    menu.moveHighlight(2);

    menu.setVisibleItems(List.of(menu.getItems().get(1), menu.getItems().get(2)));

    assertThat(menu.getHighlightedItem().getLabel())
        .as("a narrowed list re-highlights from the top")
        .isEqualTo("Copy");
  }

  @Test
  void aFilterThatMatchesNothingLeavesNoHighlight() {
    final ElwhaMenu menu = open(threeItems().build());

    menu.setVisibleItems(List.of());

    assertThat(menu.getHighlightedItem()).as("an empty result highlights nothing").isNull();
  }
}
