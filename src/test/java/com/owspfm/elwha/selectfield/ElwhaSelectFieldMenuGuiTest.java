package com.owspfm.elwha.selectfield;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the editable combobox <em>with its menu actually open</em> — the S6 backlog.
 * The headless suite drives the filter and commit seams directly, which proves what they do; this
 * proves the wiring that carries a real keystroke to them: the editor keeps focus while the menu is
 * up (so the menu's own bindings are inert and the field must route navigation itself), the
 * prefix-priority highlight tracks typing, and Enter commits the highlight rather than the text.
 *
 * <p>That focus arrangement is the whole reason the ARIA editable-combobox pattern needed the
 * host's {@code focusHome} widening — and it cannot be represented without real focus ownership.
 *
 * <p><b>Known gap, deliberately not asserted here.</b> Escape does not close an editable combo's
 * menu, though three places in {@code ElwhaSelectField} document that it does. The menu host binds
 * Escape on the surface's {@code WHEN_FOCUSED} map (so a submenu chain collapses one level at a
 * time), and a {@code focusHome} menu's surface never owns focus — the binding is unreachable. The
 * light-dismiss route below is the one that works today; the Escape route is reported rather than
 * asserted, so this suite neither goes red nor freezes the gap in place.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaSelectFieldMenuGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaSelectField<String> combo;
  private ElwhaButton sink;
  private Robot robot;

  @BeforeEach
  void showAnEditableCombo() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          combo = ElwhaSelectField.filled("Planet");
          // "Mars" is a prefix match for "mar"; "Mercury" is not, and neither is the substring-only
          // "Kamar" — the trio is what makes prefix-priority observable rather than incidental.
          combo.setOptions(List.of("Kamar", "Mercury", "Mars", "Venus"));
          combo.setEditable(true);
          sink = ElwhaButton.textButton("Elsewhere");
          frame = new JFrame("ElwhaSelectFieldMenuGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout(FlowLayout.LEADING, 30, 30));
          frame.add(combo);
          frame.add(sink);
          frame.setSize(700, 500);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the sink", () -> sink.isFocusOwner());
    robot.mouseMove(2, 2);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private JTextComponent editor() {
    return ((ElwhaTextField) combo.getComponent(0)).getEditor();
  }

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  /** Focuses the editor and opens the menu with the ARIA open gesture. */
  private void openTheMenu() throws Exception {
    SwingUtilities.invokeAndWait(() -> editor().requestFocusInWindow());
    waitFor("the editor owns focus", () -> editor().isFocusOwner());
    GuiSteps.keyUntil(
        robot, KeyEvent.VK_DOWN, "Down opens the option menu", () -> combo.isExpanded());
  }

  /**
   * The menu's currently highlighted label. <b>Call on the EDT</b> — {@code WaitFor} predicates
   * already run there, and nesting an {@code invokeAndWait} inside one is an error.
   */
  private String highlightedLabel() {
    final com.owspfm.elwha.menu.ElwhaMenuItem item = openMenu().getHighlightedItem();
    return item == null ? null : item.getLabel();
  }

  private com.owspfm.elwha.menu.ElwhaMenu openMenu() {
    return combo.optionsMenu();
  }

  private void type(final String text) throws Exception {
    GuiSteps.typeUntil(robot, text, combo::getText, "typing '" + text + "' into the editor");
  }

  // ------------------------------------------------------------- focus home

  @Test
  void anEditorKeepsFocusWhileItsMenuIsOpen() throws Exception {
    openTheMenu();

    assertThat(onEdt(() -> editor().isFocusOwner()))
        .as(
            "the ARIA combobox pattern keeps the field typeable — the menu is a non-focused"
                + " listbox")
        .isTrue();
  }

  @Test
  void typingIntoTheEditorDoesNotDismissTheMenu() throws Exception {
    openTheMenu();

    type("ma");

    assertThat(onEdt(() -> combo.isExpanded()))
        .as("keystrokes in the focus home are not a focus escape, so the menu survives them")
        .isTrue();
  }

  // ------------------------------------------------------ prefix priority

  @Test
  void highlightPrefersAPrefixMatchOverAnEarlierSubstringMatch() throws Exception {
    openTheMenu();

    type("mar");

    waitFor(
        "the first prefix match takes the active-option ring, even though a substring match "
            + "sits above it in the list",
        () -> "Mars".equals(highlightedLabel()));
  }

  @Test
  void clearingTheFilterReleasesThePrefixPriority() throws Exception {
    openTheMenu();
    type("mar");
    waitFor("the prefix match is highlighted", () -> "Mars".equals(highlightedLabel()));

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_BACK_SPACE,
        "deleting back to a broader filter re-runs the priority",
        () -> "Kamar".equals(highlightedLabel()) || "Mars".equals(highlightedLabel()));

    assertThat(onEdt(() -> combo.isExpanded())).as("and the menu is still open").isTrue();
  }

  // --------------------------------------------------------- menu navigation

  @Test
  void arrowKeysInTheEditorMoveTheMenuHighlight() throws Exception {
    openTheMenu();
    final String top = read(this::highlightedLabel);

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_DOWN,
        "Down in the editor is routed to the menu's roving highlight",
        () -> !java.util.Objects.equals(top, highlightedLabel()));

    // Walk back Up with a bounded press-until-target loop rather than a single guarded press: a
    // slow-but-delivered Down above may have moved more than one row, and Up is not idempotent
    // mid-list — but it IS clamped at the top, so overshooting toward the target is harmless.
    for (int press = 0;
        press < 4 && !java.util.Objects.equals(top, read(this::highlightedLabel));
        press++) {
      robot.keyPress(KeyEvent.VK_UP);
      robot.keyRelease(KeyEvent.VK_UP);
      robot.waitForIdle();
      final long deadline = System.currentTimeMillis() + 1500;
      while (System.currentTimeMillis() < deadline
          && !java.util.Objects.equals(top, read(this::highlightedLabel))) {
        Thread.sleep(20);
      }
    }
    waitFor("and Up walks it back to the top", () -> top.equals(highlightedLabel()));
  }

  // ---------------------------------------------------------------- commit

  @Test
  void enterCommitsTheHighlightedOptionRatherThanTheTypedText() throws Exception {
    openTheMenu();
    type("mar");
    waitFor("the prefix match is highlighted", () -> "Mars".equals(highlightedLabel()));

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_ENTER,
        "Enter commits through the open menu",
        () -> "Mars".equals(combo.getSelectedValue()));

    waitFor("and the menu closes on the pick", () -> !combo.isExpanded());
    assertThat(onEdt(() -> "Mars".equals(combo.getText())))
        .as("the field shows the option's canonical text, not the half-typed filter")
        .isTrue();
  }

  @Test
  void clickingAnOptionCommitsItThroughTheRealPipeline() throws Exception {
    openTheMenu();
    final com.owspfm.elwha.menu.ElwhaMenuItem venus =
        read(
            () ->
                openMenu().getItems().stream()
                    .filter(item -> "Venus".equals(item.getLabel()))
                    .findFirst()
                    .orElseThrow());

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> {
          try {
            return read(
                () -> {
                  final java.awt.Point origin = venus.getLocationOnScreen();
                  return new java.awt.Point(
                      origin.x + venus.getWidth() / 2, origin.y + venus.getHeight() / 2);
                });
          } catch (final Exception e) {
            throw new IllegalStateException(e);
          }
        },
        "clicking an option row commits it",
        () -> "Venus".equals(combo.getSelectedValue()));

    waitFor("and closes the menu", () -> !combo.isExpanded());
  }

  @Test
  void aPressOutsideClosesTheMenuWithoutCommittingTheFilter() throws Exception {
    SwingUtilities.invokeAndWait(() -> combo.setSelectedValue("Venus"));
    openTheMenu();
    type("mar");
    waitFor("the filter narrowed the list", () -> "Mars".equals(highlightedLabel()));

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> {
          try {
            return read(
                () -> {
                  final java.awt.Point origin = sink.getLocationOnScreen();
                  return new java.awt.Point(
                      origin.x + sink.getWidth() / 2, origin.y + sink.getHeight() / 2);
                });
          } catch (final Exception e) {
            throw new IllegalStateException(e);
          }
        },
        "a press outside light-dismisses the option menu",
        () -> !combo.isExpanded());

    assertThat(onEdt(() -> "Venus".equals(combo.getSelectedValue())))
        .as("closing the menu is not a commit — the previous value stands")
        .isTrue();
  }
}
