package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of what a menu row announces: its role, where its name comes from, the roving
 * focus flag, and the submenu trigger's expand/collapse states — the M3 {@code aria-expanded}
 * analog.
 *
 * <p><b>Not covered here:</b> the open submenu exposed as the trigger's accessible <em>child</em>.
 * That needs the nested menu actually mounted, and opening a submenu chain reaches {@code
 * MouseInfo.getPointerInfo()} (the hover-driven corner morph), which throws under {@code
 * java.awt.headless=true}. The {@code gui} tier carries it.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaMenuItemAccessibilityTest {

  private static ElwhaMenu nestedMenu() {
    return ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Link")).build();
  }

  @Test
  void aRowAnnouncesItselfAsAMenuItem() {
    assertThat(ElwhaMenuItem.of("Copy").getAccessibleContext().getAccessibleRole())
        .isEqualTo(AccessibleRole.MENU_ITEM);
  }

  @Test
  void aRowIsNamedByItsLabel() {
    assertThat(ElwhaMenuItem.of("Copy").getAccessibleContext().getAccessibleName())
        .as("the visible label is the accessible name — a leading icon is decorative")
        .isEqualTo("Copy");
  }

  @Test
  void aLeadingIconAddsNothingToTheName() {
    assertThat(
            ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename")
                .getAccessibleContext()
                .getAccessibleName())
        .isEqualTo("Rename");
  }

  @Test
  void anExplicitNameOverridesTheLabel() {
    final ElwhaMenuItem item = ElwhaMenuItem.of("Copy");
    item.getAccessibleContext().setAccessibleName("Copy to clipboard");

    assertThat(item.getAccessibleContext().getAccessibleName())
        .as("a caller-set name wins, for rows whose label is not self-describing")
        .isEqualTo("Copy to clipboard");
  }

  @Test
  void anEmptyExplicitNameFallsBackToTheLabel() {
    final ElwhaMenuItem item = ElwhaMenuItem.of("Copy");
    item.getAccessibleContext().setAccessibleName("");

    assertThat(item.getAccessibleContext().getAccessibleName())
        .as("a blank override is not a name — the label stands")
        .isEqualTo("Copy");
  }

  @Test
  void rovingHighlightIsAnnouncedAsFocus() {
    final ElwhaMenuItem item = ElwhaMenuItem.of("Copy");

    item.setFocused(true);

    assertThat(item.getAccessibleContext().getAccessibleStateSet().toArray())
        .as(
            "the row the menu's roving focus is on reports itself focused, though it never owns"
                + " focus")
        .contains(AccessibleState.FOCUSED);
  }

  @Test
  void aPlainRowIsNeitherExpandableNorExpanded() {
    assertThat(ElwhaMenuItem.of("Copy").getAccessibleContext().getAccessibleStateSet().toArray())
        .as("only a submenu trigger has anything to expand")
        .doesNotContain(
            AccessibleState.EXPANDABLE, AccessibleState.EXPANDED, AccessibleState.COLLAPSED);
  }

  @Test
  void aSubMenuTriggerAnnouncesItselfExpandableAndCollapsed() {
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", nestedMenu());

    assertThat(item.getAccessibleContext().getAccessibleStateSet().toArray())
        .as("a resting submenu trigger advertises that something can open from it")
        .contains(AccessibleState.EXPANDABLE, AccessibleState.COLLAPSED)
        .doesNotContain(AccessibleState.EXPANDED);
  }

  @Test
  void anOpenSubMenuFlipsTheTriggerToExpanded() {
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", nestedMenu());

    item.setExpanded(true);

    assertThat(item.isExpanded()).as("the trigger tracks its submenu's open state").isTrue();
    assertThat(item.getAccessibleContext().getAccessibleStateSet().toArray())
        .as("and announces it as expanded rather than collapsed")
        .contains(AccessibleState.EXPANDABLE, AccessibleState.EXPANDED)
        .doesNotContain(AccessibleState.COLLAPSED);
  }

  @Test
  void collapsingRestoresTheCollapsedState() {
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", nestedMenu());
    item.setExpanded(true);

    item.setExpanded(false);

    assertThat(item.getAccessibleContext().getAccessibleStateSet().toArray())
        .contains(AccessibleState.COLLAPSED)
        .doesNotContain(AccessibleState.EXPANDED);
  }

  @Test
  void aCollapsedTriggerExposesNoSubMenuChild() {
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", nestedMenu());

    assertThat(item.getAccessibleContext().getAccessibleChildrenCount())
        .as("a closed submenu is not part of the accessibility tree")
        .isZero();
  }
}
