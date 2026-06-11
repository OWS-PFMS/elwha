package com.owspfm.elwha.slider;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.chip.ChipInteractionMode;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;

/**
 * Headless guard for #432 — the {@code AbstractAction.isEnabled()} shadowing bug. An unqualified
 * {@code isEnabled()} inside an anonymous keyboard {@code AbstractAction} binds to the Action's own
 * always-true flag, not the component's, so disabled components still responded to their bound keys
 * (reachable: focus survives {@code setEnabled(false)}, and a disabled {@code RANGE} slider's focus
 * proxies stayed Tab-able). Asserts: a disabled single slider ignores every bound action; a
 * disabled range slider's values hold and its focus proxies leave the traversal order; re-enabling
 * restores both; and the five sibling activation keymaps fixed in the same pass (Button /
 * IconButton / FAB / Chip / Card / NavRailDestination) fire zero action events while disabled but
 * fire again once re-enabled (proving the listener wiring, so the zero is not vacuous).
 * ElwhaSwitch's already-qualified guards are covered by {@code ElwhaSwitchInteractionSmoke}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SliderDisabledKeyGuard {

  private SliderDisabledKeyGuard() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkSingleSlider();
    checkRangeSlider();
    checkSiblings();

    System.out.println(
        "SliderDisabledKeyGuard: OK (disabled slider keyboard inert + range proxies leave"
            + " traversal + sibling activation keymaps inert while disabled)");
  }

  private static void checkSingleSlider() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSize(240, ElwhaSlider.HANDLE_HEIGHT_PX);
    final int[] changes = {0};
    slider.addChangeListener(e -> changes[0]++);

    slider.setEnabled(false);
    invokeOwnActions(slider);
    check(
        "disabled single slider holds its value through every bound action",
        slider.getValue() == 50);
    check("disabled single slider fires no change events", changes[0] == 0);

    slider.setEnabled(true);
    // The all-actions sweep above may leave the Space-held modifier set (spaceDown/spaceUp fire
    // in undefined map order; the flag is guard-free by design) — clear it deterministically.
    invoke(slider, "elwhaSlider.spaceUp");
    invoke(slider, "elwhaSlider.increase");
    check("re-enabled slider steps again (wiring proven)", slider.getValue() == 51);
  }

  private static void checkRangeSlider() {
    final ElwhaSlider range = ElwhaSlider.range(0, 100, 30, 70);
    range.setSize(240, ElwhaSlider.HANDLE_HEIGHT_PX);

    range.setEnabled(false);
    invokeOwnActions(range);
    check(
        "disabled range slider holds both handle values",
        range.getLowerValue() == 30 && range.getUpperValue() == 70);
    check(
        "disabled range slider's focus proxies leave the traversal order",
        enabledFocusableChildren(range) == 0);

    range.setEnabled(true);
    check("re-enabling restores the focus proxies", enabledFocusableChildren(range) == 2);
    invoke(range, "elwhaSlider.spaceUp");
    invoke(range, "elwhaSlider.increase");
    check("re-enabled range slider steps its focused handle again", range.getLowerValue() == 31);
  }

  private static void checkSiblings() {
    final ElwhaButton button = ElwhaButton.filledButton("Guard");
    sweep("ElwhaButton", button, "elwhabutton.activate", attach(button::addActionListener));

    final ElwhaIconButton iconButton = ElwhaIconButton.filledIconButton(MaterialIcons.check());
    sweep(
        "ElwhaIconButton",
        iconButton,
        "elwhaiconbutton.activate",
        attach(iconButton::addActionListener));

    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());
    sweep("ElwhaFab", fab, "elwhafab.activate", attach(fab::addActionListener));

    final ElwhaChip chip = new ElwhaChip("Guard");
    chip.setInteractionMode(ChipInteractionMode.CLICKABLE);
    sweep("ElwhaChip", chip, "elwhachip.activate", attach(chip::addActionListener));

    final ElwhaCard card = new ElwhaCard();
    card.setActionable(true);
    sweep("ElwhaCard", card, "elwhaCardActivate", attach(card::addActionListener));

    final ElwhaNavRailDestination destination =
        ElwhaNavRailDestination.of(MaterialIcons.home(), MaterialIcons.homeFilled(), "Guard");
    sweep(
        "ElwhaNavRailDestination",
        destination,
        "elwhaNavRailDestination.activate",
        attach(destination::addActionListener));
  }

  /** Registers a counting listener through the component's own {@code addActionListener}. */
  private static int[] attach(
      final java.util.function.Consumer<java.awt.event.ActionListener> registrar) {
    final int[] fired = {0};
    registrar.accept(e -> fired[0]++);
    return fired;
  }

  /** Disabled → every own action inert; re-enabled → the activate key fires. */
  private static void sweep(
      final String name, final JComponent c, final String activateKey, final int[] fired) {
    c.setSize(c.getPreferredSize());
    c.setEnabled(false);
    invokeOwnActions(c);
    check("disabled " + name + " fires no action events from its keymap", fired[0] == 0);
    c.setEnabled(true);
    invoke(c, activateKey);
    check("re-enabled " + name + " activates again (wiring proven)", fired[0] >= 1);
  }

  /** Invokes every action registered in the component's own ActionMap (not UI parents). */
  private static void invokeOwnActions(final JComponent c) {
    final Object[] keys = c.getActionMap().keys();
    if (keys == null) {
      return;
    }
    for (final Object key : keys) {
      invoke(c, String.valueOf(key));
    }
  }

  private static void invoke(final JComponent c, final String key) {
    c.getActionMap()
        .get(key)
        .actionPerformed(new ActionEvent(c, ActionEvent.ACTION_PERFORMED, key));
  }

  /** Counts children that focus traversal would visit (focusable and enabled). */
  private static int enabledFocusableChildren(final JComponent parent) {
    int count = 0;
    for (final Component child : parent.getComponents()) {
      if (child.isFocusable() && child.isEnabled()) {
        count++;
      }
    }
    return count;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
