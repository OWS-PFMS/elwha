package com.owspfm.elwha.menu;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the menu on a real display — the half the headless tier structurally cannot
 * reach. Anchored placement needs a realized window (a headless anchor is never {@code
 * isShowing()}, so the engine degrades to a zero rect); the submenu chain needs a real pointer,
 * because opening one resolves the hover-driven corner morph through {@code MouseInfo}; and every
 * focus assertion here is about real ownership arbitrated by the {@code KeyboardFocusManager},
 * which is exactly the class of bug epic&nbsp;#322 shipped a fix for.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaMenuGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaButton trigger;
  private ElwhaButton sink;
  private Robot robot;
  private final List<ElwhaMenu> opened = new ArrayList<>();

  @BeforeEach
  void showATriggerAndASink() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    // Each test's frame gets its own screen slot: under bare Xvfb the previous test's window can
    // still be tearing down at the shared position, and events aimed there go to the dying window.
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          trigger = ElwhaButton.filledButton("Open");
          sink = ElwhaButton.textButton("Elsewhere");
          frame = new JFrame("ElwhaMenuGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout());
          frame.add(trigger);
          frame.add(sink);
          frame.setSize(900, 420);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    // The native toolkit hands a fresh frame's focus to its first focusable child while Cacio hands
    // it to nothing; parking it on the sink makes the resting state the same on both.
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the sink", () -> sink.isFocusOwner());
    // A submenu row opens on a 400ms hover dwell, so the pointer must not be left over the frame.
    robot.mouseMove(0, 0);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    for (final ElwhaMenu menu : opened) {
      SwingUtilities.invokeAndWait(menu::close);
    }
    opened.clear();
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private void open(final ElwhaMenu menu) throws Exception {
    opened.add(menu);
    SwingUtilities.invokeAndWait(() -> menu.open(trigger));
    waitFor("the menu surface is on screen", () -> !mounted().isEmpty());
  }

  /** Overlay surfaces mounted on the frame's layered pane, front-most first. Call on the EDT. */
  private List<Component> mounted() {
    final List<Component> out = new ArrayList<>();
    for (final Component child : frame.getLayeredPane().getComponents()) {
      if (child != frame.getContentPane()) {
        out.add(child);
      }
    }
    return out;
  }

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  private Rectangle boundsInPane(final Component component) {
    final Point origin =
        SwingUtilities.convertPoint(
            component.getParent(), component.getLocation(), frame.getLayeredPane());
    return new Rectangle(origin, component.getSize());
  }

  private static Point centerOnScreen(final Component component) {
    final Point origin = component.getLocationOnScreen();
    return new Point(origin.x + component.getWidth() / 2, origin.y + component.getHeight() / 2);
  }

  private static ElwhaMenu.Builder threeItems() {
    return ElwhaMenu.builder()
        .addItem(ElwhaMenuItem.of("Grid"))
        .addItem(ElwhaMenuItem.of("List"))
        .addItem(ElwhaMenuItem.of("Gallery"));
  }

  /**
   * #396 — a scrollable menu used to size its viewport to the whole layered pane, and placement
   * then clamped the too-tall surface back inside it, over the trigger. Cosmetic for a plain menu;
   * for the editable {@code ElwhaSelectField} combo it buried the field the user was typing into,
   * so they could not see what was filtering the list. Needs a real window: a headless anchor is
   * never {@code isShowing()}, so the bound degrades to the viewport.
   */
  @Test
  void aLongMenuScrollsRatherThanCoveringItsTrigger() throws Exception {
    final ElwhaMenu.Builder builder = ElwhaMenu.builder();
    for (int i = 0; i < 40; i++) {
      builder.addItem(ElwhaMenuItem.of("Item " + i));
    }

    open(builder.build());

    final Rectangle surface = read(() -> boundsInPane(mounted().get(0)));
    final Rectangle anchorRect = read(() -> boundsInPane(trigger));
    assertThat(surface.intersects(anchorRect))
        .as(
            "a menu long enough to fill the window must scroll inside the room beside its "
                + "trigger, not grow past it and get clamped back on top of it")
        .isFalse();
  }

  @Test
  void aLongMenuStaysInsideTheWindow() throws Exception {
    final ElwhaMenu.Builder builder = ElwhaMenu.builder();
    for (int i = 0; i < 40; i++) {
      builder.addItem(ElwhaMenuItem.of("Item " + i));
    }

    open(builder.build());

    final Rectangle surface = read(() -> boundsInPane(mounted().get(0)));
    final int paneHeight = read(() -> frame.getLayeredPane().getHeight());
    assertThat(surface.y).as("the capped menu still starts inside the viewport").isGreaterThan(-1);
    assertThat(surface.y + surface.height)
        .as("and still ends inside it — the cap bounds the menu, it does not push it off-screen")
        .isLessThanOrEqualTo(paneHeight);
  }

  /** Captures the whole virtual screen into surefire-reports so the CI artifact carries it. */
  private void dumpScreen(final String label) {
    try {
      final Rectangle screen = new Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
      final java.io.File dir = new java.io.File("target/surefire-reports");
      dir.mkdirs();
      javax.imageio.ImageIO.write(
          robot.createScreenCapture(screen), "png", new java.io.File(dir, label + ".png"));
    } catch (final Exception e) {
      System.err.println("screen dump failed: " + e);
    }
  }

  // ---------------------------------------------------------------- placement

  @Test
  void anOpenedMenuLandsBelowItsTriggerLeadingAligned() throws Exception {
    final ElwhaMenu menu = threeItems().build();

    open(menu);

    final Rectangle surface = read(() -> boundsInPane(mounted().get(0)));
    final Rectangle anchor = read(() -> boundsInPane(trigger));
    assertThat(surface.y)
        .as("a real anchor puts the menu one gap below the trigger, which no headless run can show")
        .isEqualTo(anchor.y + anchor.height + AbstractElwhaMenuOverlay.ANCHOR_GAP_PX);
    assertThat(surface.x).as("leading-aligned with the trigger").isEqualTo(anchor.x);
  }

  @Test
  void anOpenedMenuSitsOnThePopupBandAndOwnsFocus() throws Exception {
    final ElwhaMenu menu = threeItems().build();

    open(menu);

    assertThat(read(() -> frame.getLayeredPane().getLayer(mounted().get(0))))
        .isEqualTo(JLayeredPane.POPUP_LAYER.intValue());
    waitFor(
        "keyboard focus really moves into the menu surface, not just nominally",
        () -> menu.focusComponent().isFocusOwner());
    assertThat(onEdt(() -> sink.isFocusOwner())).as("and leaves the parked sink").isFalse();
  }

  @Test
  void arrowKeysWalkTheRovingHighlightThroughTheRealKeyPipeline() throws Exception {
    final ElwhaMenu menu = threeItems().build();
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());

    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_DOWN,
        "Down through the real pipeline moves the highlight off the first row",
        () -> "List".equals(menu.getHighlightedItem().getLabel()));

    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_END,
        "End jumps the highlight to the last row",
        () -> "Gallery".equals(menu.getHighlightedItem().getLabel()));
  }

  @Test
  void escapeDismissesTheMenuAndHandsFocusBackToTheTrigger() throws Exception {
    final List<MenuDismissCause> causes = new ArrayList<>();
    final ElwhaMenu menu = threeItems().onClose(causes::add).build();
    SwingUtilities.invokeAndWait(() -> trigger.requestFocusInWindow());
    waitFor("the trigger owns focus before opening", () -> trigger.isFocusOwner());
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());

    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_ESCAPE,
        "Escape through the real pipeline dismisses the menu",
        () -> mounted().isEmpty());

    assertThat(causes).containsExactly(MenuDismissCause.ESCAPE);
    waitFor(
        "an intentional dismiss really returns focus to the trigger", () -> trigger.isFocusOwner());
  }

  @Test
  void aPressOutsideTheMenuDismissesItAndLeavesFocusWhereItLanded() throws Exception {
    final List<MenuDismissCause> causes = new ArrayList<>();
    final ElwhaMenu menu = threeItems().onClose(causes::add).build();
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOnScreenOffEdt(sink),
        "a press outside the surface light-dismisses the menu",
        () -> mounted().isEmpty());

    assertThat(causes).containsExactly(MenuDismissCause.OUTSIDE_PRESS);
    assertThat(onEdt(() -> trigger.isFocusOwner()))
        .as("focus is not yanked back to the trigger — the user just put it elsewhere")
        .isFalse();
  }

  private Point centerOnScreenOffEdt(final Component component) {
    try {
      return read(() -> centerOnScreen(component));
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  // ------------------------------------------------------------------ submenu

  @Test
  void rightArrowOpensASubMenuBesideItsOpenerWithoutClosingTheParent() throws Exception {
    final ElwhaMenu nested =
        ElwhaMenu.builder()
            .addItem(ElwhaMenuItem.of("Email"))
            .addItem(ElwhaMenuItem.of("Link"))
            .build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", nested);
    final ElwhaMenu menu =
        ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Grid")).addItem(share).build();
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());

    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_DOWN,
        "the highlight reaches the submenu row",
        () -> menu.getHighlightedItem() == share);
    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_RIGHT,
        "Right opens the nested menu",
        () -> share.isExpanded());

    assertThat(read(this::mounted))
        .as("the parent stays up — a chain is two open overlays, not a replacement")
        .hasSize(2);
    final Rectangle sub = read(() -> boundsInPane(mounted().get(0)));
    final Rectangle opener = read(() -> boundsInPane(share));
    assertThat(sub.x)
        .as("the nested menu opens off the opener row's trailing edge")
        .isEqualTo(opener.x + opener.width + AbstractElwhaMenuOverlay.SUBMENU_GAP_PX);
    assertThat(sub.y).as("top-aligned with the row that opened it").isEqualTo(opener.y);
  }

  @Test
  void escapeCollapsesOneChainLevelAtATime() throws Exception {
    final ElwhaMenu nested = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", nested);
    final ElwhaMenu menu =
        ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Grid")).addItem(share).build();
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());
    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_DOWN,
        "the highlight reaches the submenu row",
        () -> menu.getHighlightedItem() == share);
    GuiSteps.keyUntil(
        robot, java.awt.event.KeyEvent.VK_RIGHT, "the nested menu opens", () -> share.isExpanded());

    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_ESCAPE,
        "Escape closes the focused level only",
        () -> !share.isExpanded());

    assertThat(read(this::mounted))
        .as("the parent survives its child's dismissal — the #322 focus-bounce regression")
        .hasSize(1);
    waitFor(
        "and focus lands back on the parent surface rather than escaping to the frame",
        () -> menu.focusComponent().isFocusOwner());
  }

  @Test
  void aPressOutsideEveryLevelClosesTheWholeChain() throws Exception {
    final ElwhaMenu nested = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", nested);
    final ElwhaMenu menu =
        ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Grid")).addItem(share).build();
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());
    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_DOWN,
        "the highlight reaches the submenu row",
        () -> menu.getHighlightedItem() == share);
    GuiSteps.keyUntil(
        robot, java.awt.event.KeyEvent.VK_RIGHT, "the nested menu opens", () -> share.isExpanded());

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOnScreenOffEdt(sink),
        "a press outside every level unwinds the chain from its root",
        () -> mounted().isEmpty());

    assertThat(onEdt(() -> share.isExpanded()))
        .as("the opener's expanded state is cleared with it")
        .isFalse();
  }

  @Test
  void swappingAnOpenSubMenuClosesTheOutgoingOne() throws Exception {
    final ElwhaMenu outgoing = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).build();
    final ElwhaMenu incoming = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Link")).build();
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", outgoing);
    final ElwhaMenu menu =
        ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Grid")).addItem(share).build();
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());
    GuiSteps.keyUntil(
        robot,
        java.awt.event.KeyEvent.VK_DOWN,
        "the highlight reaches the submenu row",
        () -> menu.getHighlightedItem() == share);
    GuiSteps.keyUntil(
        robot, java.awt.event.KeyEvent.VK_RIGHT, "the nested menu opens", () -> share.isExpanded());

    SwingUtilities.invokeAndWait(() -> share.setSubMenu(incoming));

    assertThat(read(this::mounted))
        .as("#604 — the outgoing menu stayed mounted with no opener able to reach it")
        .hasSize(1);
    assertThat(onEdt(() -> share.isExpanded()))
        .as("and the row kept reporting EXPANDED with nothing open, disarming hover-to-open")
        .isFalse();
    assertThat(read(share::getSubMenu)).isSameAs(incoming);
  }

  // ---------------------------------------------------------------- selection

  @Test
  void aSingleSelectionMadeByClickSurvivesARealReopen() throws Exception {
    final ElwhaMenu menu = threeItems().selectionMode(SelectionMode.SINGLE).build();
    final ElwhaMenuItem list = menu.getItems().get(1);
    open(menu);
    waitFor("the menu surface owns focus", () -> menu.focusComponent().isFocusOwner());

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOnScreenOffEdt(list),
        "clicking a row through the real pipeline selects it",
        () -> list.isSelected());
    waitFor("and a single-select menu closes on the pick", () -> mounted().isEmpty());

    open(menu);

    assertThat(onEdt(() -> list.isSelected()))
        .as("selection lives on the items, so the rebuilt surface still shows the pick")
        .isTrue();
    assertThat(read(() -> menu.getSelectedItems().size()))
        .as("and it is still the only one")
        .isEqualTo(1);
  }

  @Test
  void clickingASecondTriggerSwapsMenusOnThatOneClick() throws Exception {
    final ElwhaMenu first = threeItems().build();
    final ElwhaMenu second = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Only")).build();
    opened.add(second);
    SwingUtilities.invokeAndWait(() -> sink.addActionListener(e -> second.open(sink)));
    open(first);
    waitFor("the first menu is up", () -> mounted().size() == 1);

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOnScreenOffEdt(sink),
        "the second trigger's menu opens",
        () -> second.focusComponent() != null);

    waitFor(
        "menus are singular popovers — the press that opens one is an outside press for the other",
        () -> mounted().size() == 1 && first.focusComponent() == null);
  }

  @Test
  void aScreenCaptureShowsTheMenuWhereThePlacementEnginePutIt() throws Exception {
    final ElwhaMenu menu = threeItems().build();
    open(menu);
    robot.mouseMove(0, 0);
    robot.waitForIdle();

    final Rectangle onScreen = read(() -> boundsOnScreen(mounted().get(0)));
    if (onScreen.width <= 0 || onScreen.height <= 0) {
      dumpScreen("menu-placement-failure");
    }
    assertThat(onScreen.width).as("the menu really occupies screen space").isPositive();
    final Rectangle triggerOnScreen = read(() -> boundsOnScreen(trigger));
    assertThat(onScreen.y)
        .as("below the trigger on the physical screen, not merely in pane coordinates")
        .isGreaterThan(triggerOnScreen.y + triggerOnScreen.height - 1);
  }

  private static Rectangle boundsOnScreen(final Component component) {
    return new Rectangle(component.getLocationOnScreen(), component.getSize());
  }
}
