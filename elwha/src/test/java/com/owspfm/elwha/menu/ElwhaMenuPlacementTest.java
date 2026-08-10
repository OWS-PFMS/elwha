package com.owspfm.elwha.menu;

import static com.owspfm.elwha.menu.AbstractElwhaMenuOverlay.ANCHOR_GAP_PX;
import static com.owspfm.elwha.menu.AbstractElwhaMenuOverlay.SUBMENU_GAP_PX;
import static com.owspfm.elwha.menu.AbstractElwhaMenuOverlay.placeAnchored;
import static com.owspfm.elwha.menu.AbstractElwhaMenuOverlay.placeBeside;
import static com.owspfm.elwha.menu.AbstractElwhaMenuOverlay.restoresFocus;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the menu host's placement engines against a pinned viewport. Both are pure
 * functions of anchor bounds, preferred size, viewport, and direction — deliberately so, because
 * the interesting cases (a trigger near the bottom edge, a submenu near the trailing edge, RTL) are
 * geometry, not window management. The {@code gui} tier proves the same rules land on a real
 * screen; the exhaustive edge matrix lives here.
 *
 * <p>Also covers the dismiss-cause → restore-focus policy, which is a pure predicate for the same
 * reason: whether a close hands focus back to the trigger is a rule, not a race.
 */
class ElwhaMenuPlacementTest {

  private static final int PANE_W = 800;
  private static final int PANE_H = 600;
  private static final Dimension MENU = new Dimension(200, 150);

  // ------------------------------------------------------- anchored (root menu)

  @Test
  void aRootMenuOpensBelowItsTriggerLeadingAligned() {
    final Rectangle trigger = new Rectangle(100, 80, 120, 40);

    final Rectangle bounds = placeAnchored(trigger, MENU, PANE_W, PANE_H, false);

    assertThat(bounds.x).as("the menu's leading edge lines up with the trigger's").isEqualTo(100);
    assertThat(bounds.y)
        .as("and it sits one anchor gap below the trigger's bottom edge")
        .isEqualTo(80 + 40 + ANCHOR_GAP_PX);
    assertThat(bounds.getSize()).as("at the surface's preferred size").isEqualTo(MENU);
  }

  @Test
  void aTriggerNearTheBottomFlipsTheMenuAboveIt() {
    final Rectangle trigger = new Rectangle(100, PANE_H - 60, 120, 40);

    final Rectangle bounds = placeAnchored(trigger, MENU, PANE_W, PANE_H, false);

    assertThat(bounds.y)
        .as("no room below, so the menu opens upward off the trigger's top edge")
        .isEqualTo(trigger.y - ANCHOR_GAP_PX - MENU.height);
    assertThat(bounds.y + bounds.height)
        .as("and clears the trigger entirely")
        .isLessThanOrEqualTo(trigger.y);
  }

  @Test
  void aMenuWithRoomNeitherWayIsClampedInsteadOfFlipped() {
    final Rectangle trigger = new Rectangle(0, 100, 80, 40);
    final int shortPane = 200;

    final Rectangle bounds = placeAnchored(trigger, MENU, PANE_W, shortPane, false);

    assertThat(bounds.y + bounds.height)
        .as(
            "a menu that fits neither way is lifted only until its bottom edge meets the"
                + " viewport's")
        .isEqualTo(shortPane);
    assertThat(bounds.y)
        .as("rather than flipped above a trigger with no room there")
        .isNotNegative();
  }

  @Test
  void aTriggerNearTheTrailingEdgeShiftsTheMenuBackInside() {
    final Rectangle trigger = new Rectangle(PANE_W - 40, 80, 40, 40);

    final Rectangle bounds = placeAnchored(trigger, MENU, PANE_W, PANE_H, false);

    assertThat(bounds.x + bounds.width)
        .as("the menu is shifted so its trailing edge stops at the viewport edge")
        .isEqualTo(PANE_W);
    assertThat(bounds.x).as("without going negative").isNotNegative();
  }

  @Test
  void rightToLeftAnchorsTheMenuToTheTriggersTrailingEdge() {
    final Rectangle trigger = new Rectangle(300, 80, 120, 40);

    final Rectangle bounds = placeAnchored(trigger, MENU, PANE_W, PANE_H, true);

    assertThat(bounds.x + bounds.width)
        .as("under RTL the menu's trailing edge lines up with the trigger's right edge")
        .isEqualTo(trigger.x + trigger.width);
  }

  @Test
  void aSurfaceLargerThanTheViewportIsCappedToIt() {
    final Rectangle trigger = new Rectangle(10, 10, 40, 20);
    final Dimension oversized = new Dimension(PANE_W + 300, PANE_H + 300);

    final Rectangle bounds = placeAnchored(trigger, oversized, PANE_W, PANE_H, false);

    assertThat(bounds.width)
        .as("an oversized menu is capped at the viewport width")
        .isEqualTo(PANE_W);
    assertThat(bounds.height).as("and at the viewport height").isEqualTo(PANE_H);
  }

  // ------------------------------------- which engine a menu picks (#238 §16.4)

  @Test
  void aRootMenuPicksTheAnchoredEngineByDefault() {
    assertThat(ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Cut")).build().sideAnchored())
        .as("a menu opens below its trigger unless it is told otherwise")
        .isFalse();
  }

  @Test
  void aRootMenuCanOptIntoTheBesideEngine() {
    assertThat(
            ElwhaMenu.builder()
                .sideAnchored(true)
                .addItem(ElwhaMenuItem.of("Cut"))
                .build()
                .sideAnchored())
        .as(
            "#238 §16.4 — a trigger on a vertical window edge (a nav rail's overflow button) has "
                + "nowhere below to open into, so it borrows the submenu's placement")
        .isTrue();
  }

  // ------------------------------------------------------- beside (submenu)

  @Test
  void aSubMenuOpensOffTheTrailingSideOfItsOpenerTopAligned() {
    final Rectangle opener = new Rectangle(100, 200, 160, 48);

    final Rectangle bounds = placeBeside(opener, MENU, PANE_W, PANE_H, false);

    assertThat(bounds.x)
        .as("the submenu starts at the opener's trailing edge")
        .isEqualTo(opener.x + opener.width + SUBMENU_GAP_PX);
    assertThat(bounds.y).as("top-aligned with the opener row").isEqualTo(opener.y);
  }

  @Test
  void aSubMenuNearTheTrailingEdgeFlipsToTheLeadingSide() {
    final Rectangle opener = new Rectangle(PANE_W - 180, 200, 160, 48);

    final Rectangle bounds = placeBeside(opener, MENU, PANE_W, PANE_H, false);

    assertThat(bounds.x)
        .as("no room on the trailing side, so the submenu opens to the leading side")
        .isEqualTo(opener.x - SUBMENU_GAP_PX - MENU.width);
    assertThat(bounds.x + bounds.width)
        .as("and stops short of the opener rather than covering it")
        .isLessThanOrEqualTo(opener.x);
  }

  @Test
  void aSubMenuWithRoomOnNeitherSideStaysTrailingAndClamps() {
    final int narrow = MENU.width + 40;
    final Rectangle opener = new Rectangle(20, 200, narrow - 40, 48);

    final Rectangle bounds = placeBeside(opener, MENU, narrow, PANE_H, false);

    assertThat(bounds.x).as("a submenu that fits neither side is clamped inside").isNotNegative();
    assertThat(bounds.x + bounds.width)
        .as("and never overhangs the trailing edge")
        .isLessThanOrEqualTo(narrow);
  }

  @Test
  void rightToLeftMirrorsWhichSideIsTrailing() {
    final Rectangle opener = new Rectangle(300, 200, 160, 48);

    final Rectangle bounds = placeBeside(opener, MENU, PANE_W, PANE_H, true);

    assertThat(bounds.x + bounds.width)
        .as("under RTL the trailing side is the opener's left edge")
        .isEqualTo(opener.x - SUBMENU_GAP_PX);
  }

  @Test
  void rightToLeftFlipsToTheLeadingSideAtTheLeftEdge() {
    final Rectangle opener = new Rectangle(10, 200, 160, 48);

    final Rectangle bounds = placeBeside(opener, MENU, PANE_W, PANE_H, true);

    assertThat(bounds.x)
        .as("no room to the left under RTL, so the submenu opens to the right")
        .isEqualTo(opener.x + opener.width + SUBMENU_GAP_PX);
  }

  @Test
  void aSubMenuOpenedLowIsLiftedToStayInsideTheViewport() {
    final Rectangle opener = new Rectangle(100, PANE_H - 60, 160, 48);

    final Rectangle bounds = placeBeside(opener, MENU, PANE_W, PANE_H, false);

    assertThat(bounds.y + bounds.height)
        .as("a low submenu is lifted until its bottom edge meets the viewport's")
        .isEqualTo(PANE_H);
    assertThat(bounds.y).as("without going above the viewport top").isNotNegative();
  }

  @Test
  void aSubMenuAbutsItsOpenerSoTheShadowReserveSuppliesTheSeparation() {
    assertThat(SUBMENU_GAP_PX)
        .as("the submenu surface abuts the opener row — the surfaces' own halo is the gap")
        .isZero();
    assertThat(ANCHOR_GAP_PX)
        .as("a root menu keeps the M3 anchored offset from its trigger")
        .isEqualTo(4);
  }

  // ------------------------------------------------------- focus-restore policy

  @ParameterizedTest
  @EnumSource(
      value = MenuDismissCause.class,
      names = {"ESCAPE", "SELECTION", "PROGRAMMATIC"})
  void anIntentionalCloseHandsFocusBackToTheTrigger(final MenuDismissCause cause) {
    assertThat(restoresFocus(cause))
        .as("%s is a deliberate dismiss, so focus returns where the user left it", cause)
        .isTrue();
  }

  @ParameterizedTest
  @EnumSource(
      value = MenuDismissCause.class,
      names = {"OUTSIDE_PRESS", "FOCUS_LOST"})
  void aCloseCausedByFocusMovingAwayLeavesFocusWhereItWent(final MenuDismissCause cause) {
    assertThat(restoresFocus(cause))
        .as("%s means focus already moved on — yanking it back would fight the user", cause)
        .isFalse();
  }
}
