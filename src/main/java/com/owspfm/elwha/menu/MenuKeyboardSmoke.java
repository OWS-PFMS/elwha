package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Smoke for epic #298 S4 — keyboard navigation, roving focus, activation, and the trigger
 * pressed-while-open affordance. The menu must be shown to wire the roving focus, so the assertions
 * run windowed (skipped headless); reduced motion makes teardown synchronous and deterministic.
 *
 * <p>Covers: initial focus = first item; Down/Up move (and wrap); Home/End; type-ahead; a synthetic
 * Down key event routed through the surface's bindings; Enter-activation firing the item action and
 * closing with {@link MenuDismissCause#SELECTION}; the trigger showing selected while open and
 * restored to its prior state after. Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuKeyboardSmoke {

  private MenuKeyboardSmoke() {}

  public static void main(final String[] args) throws Exception {
    final boolean headless = GraphicsEnvironment.isHeadless();
    if (headless) {
      System.setProperty("java.awt.headless", "true");
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(true);

    if (headless) {
      System.out.println("  skip (headless) keyboard nav requires a focusable window");
      System.out.println();
      System.out.println("PASS — 0 checks (headless skip)");
      return;
    }

    final int[] fail = {0};
    final int[] checks = {0};
    SwingUtilities.invokeAndWait(() -> run(fail, checks));

    System.out.println();
    System.out.println(
        fail[0] == 0
            ? "PASS — " + checks[0] + " checks"
            : "FAIL — " + fail[0] + "/" + checks[0] + " checks failed");
    System.exit(fail[0] == 0 ? 0 : 1);
  }

  private static void run(final int[] fail, final int[] checks) {
    final JFrame frame = new JFrame("s4");
    // A SELECTABLE trigger to verify the pressed-while-open affordance (the M3 icon-button / split-
    // button overflow case). A plain CLICKABLE push-button has no held-visual API in the lib today,
    // so the menu leaves such triggers visually unchanged — see ElwhaMenu Javadoc.
    final ElwhaButton trigger =
        ElwhaButton.outlinedButton("Open").setInteractionMode(ButtonInteractionMode.SELECTABLE);
    final JPanel content = new JPanel();
    content.add(trigger);
    frame.setContentPane(content);
    frame.setSize(480, 360);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    final int[] activated = {0};
    final MenuDismissCause[] cause = {null};
    final ElwhaMenuItem apple = ElwhaMenuItem.of("Apple");
    apple.addActionListener(e -> activated[0]++);
    final ElwhaMenuItem date = ElwhaMenuItem.of("Date");
    date.setEnabled(false);
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .addItem(apple)
            .addItem(ElwhaMenuItem.of("Banana"))
            .addItem(ElwhaMenuItem.of("Cherry"))
            .addItem(date)
            .addItem(ElwhaMenuItem.of("Elderberry"))
            .onClose(c -> cause[0] = c)
            .build();
    final List<ElwhaMenuItem> items = menu.getItems();

    menu.open(trigger);

    fail[0] += check(checks, menu.focusedIndex() == 0, "initial roving index on first item");
    fail[0] +=
        check(checks, !items.get(0).isFocused(), "no focus ring on pointer/programmatic open (F2)");
    fail[0] += check(checks, trigger.isSelected(), "trigger shows selected while open");

    menu.moveFocus(1);
    fail[0] +=
        check(
            checks,
            menu.focusedIndex() == 1 && items.get(1).isFocused(),
            "Down → item 1 (ring armed by keyboard)");
    menu.moveFocus(-1);
    fail[0] += check(checks, menu.focusedIndex() == 0, "Up → item 0");
    menu.moveFocus(-1);
    fail[0] += check(checks, menu.focusedIndex() == items.size() - 1, "Up wraps to last");
    menu.moveFocus(1);
    fail[0] += check(checks, menu.focusedIndex() == 0, "Down wraps to first");
    menu.setFocusedIndex(items.size() - 1);
    fail[0] += check(checks, menu.focusedIndex() == items.size() - 1, "End → last");

    menu.typeAhead('c');
    fail[0] +=
        check(
            checks,
            "Cherry".equals(items.get(menu.focusedIndex()).getLabel()),
            "type-ahead 'c' → Cherry");
    menu.typeAhead('b');
    fail[0] +=
        check(
            checks,
            "Banana".equals(items.get(menu.focusedIndex()).getLabel()),
            "type-ahead 'b' → Banana");

    // Synthetic key event routed through the surface bindings (best-effort; informational).
    final JComponent surface = menu.focusComponent();
    surface.requestFocusInWindow();
    final int before = menu.focusedIndex();
    final KeyEvent down =
        new KeyEvent(
            surface, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
    surface.dispatchEvent(down);
    System.out.println(
        "  info  synthetic VK_DOWN: focusedIndex " + before + " -> " + menu.focusedIndex());

    // Activate the focused item → fires its action and closes with SELECTION; trigger restored.
    menu.setFocusedIndex(0);
    menu.activateFocused();
    fail[0] += check(checks, activated[0] == 1, "Enter activates focused item's action");
    fail[0] +=
        check(checks, cause[0] == MenuDismissCause.SELECTION, "activation closes with SELECTION");
    fail[0] += check(checks, !trigger.isSelected(), "trigger restored to unselected after close");

    // Disabled item is inert on activation.
    final int[] disabledFired = {0};
    date.addActionListener(e -> disabledFired[0]++);
    menu.open(trigger);
    menu.setFocusedIndex(items.indexOf(date));
    menu.activateFocused();
    fail[0] += check(checks, disabledFired[0] == 0, "disabled item inert on activate");
    menu.close(MenuDismissCause.ESCAPE);

    frame.dispose();
  }

  private static int check(final int[] checks, final boolean ok, final String label) {
    checks[0]++;
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
