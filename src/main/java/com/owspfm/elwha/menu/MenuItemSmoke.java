package com.owspfm.elwha.menu;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.JLabel;

/**
 * Headless smoke for epic #298 S2 — the {@link ElwhaMenuItem} primitive across a slot × state
 * matrix. Asserts:
 *
 * <ul>
 *   <li>every slot combination meets the {@value ElwhaMenuItem#MIN_TARGET_PX} dp minimum interactive
 *       target;
 *   <li>the item is not independently focusable (the parent menu owns roving focus, §X);
 *   <li>a disabled item is inert — {@code activate()} fires no action listeners;
 *   <li>a11y reports {@link AccessibleRole#MENU_ITEM}, the label as the accessible name, and {@link
 *       AccessibleState#SELECTED} when selected;
 *   <li>every combo paints without throwing (icons, label/supporting, trailing, badge, slot,
 *       selected fill + checkmark, focus ring, disabled dim).
 * </ul>
 *
 * Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuItemSmoke {

  private MenuItemSmoke() {}

  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    int checks = 0;
    int failures = 0;

    final List<ElwhaMenuItem> matrix = new ArrayList<>();
    matrix.add(ElwhaMenuItem.of("Plain label"));
    matrix.add(ElwhaMenuItem.of(MaterialIcons.edit(ElwhaMenuItem.ICON_SIZE_PX), "Icon + label"));

    final ElwhaMenuItem supporting = ElwhaMenuItem.of(MaterialIcons.star(20), "Two-line");
    supporting.setSupportingText("supporting text");
    matrix.add(supporting);

    final ElwhaMenuItem shortcut = ElwhaMenuItem.of("Copy");
    shortcut.setTrailingText("⌘C");
    matrix.add(shortcut);

    final ElwhaMenuItem withTrailingIcon = ElwhaMenuItem.of("Submenu");
    withTrailingIcon.setTrailingIcon(MaterialIcons.expandMore(20));
    matrix.add(withTrailingIcon);

    final ElwhaMenuItem withBadge = ElwhaMenuItem.of(MaterialIcons.favorite(20), "Apparel");
    withBadge.setBadge(ElwhaBadge.large("New"));
    matrix.add(withBadge);

    final ElwhaMenuItem withSlot = ElwhaMenuItem.of("Slot row");
    withSlot.setSlot(new JLabel("custom slot content"));
    matrix.add(withSlot);

    final ElwhaMenuItem selected = ElwhaMenuItem.of(MaterialIcons.home(20), "Selected");
    selected.setSelected(true);
    matrix.add(selected);

    final ElwhaMenuItem disabled = ElwhaMenuItem.of("Disabled");
    disabled.setEnabled(false);
    matrix.add(disabled);

    // --- target height + not-focusable + paints ---
    for (final ElwhaMenuItem item : matrix) {
      final Dimension pref = item.getPreferredSize();
      failures +=
          check(
              pref.height >= ElwhaMenuItem.MIN_TARGET_PX,
              "'" + item.getLabel() + "' meets 48dp target, got " + pref.height);
      checks++;
      failures += check(!item.isFocusable(), "'" + item.getLabel() + "' is not independently focusable");
      checks++;
      failures += check(paints(item, pref), "'" + item.getLabel() + "' paints without throwing");
      checks++;
    }

    // --- focus ring state paints ---
    final ElwhaMenuItem focusable = ElwhaMenuItem.of("Focused");
    focusable.setFocused(true);
    failures += check(focusable.isFocused() && paints(focusable, focusable.getPreferredSize()),
        "focused item paints its ring");
    checks++;

    // --- a11y ---
    failures +=
        check(
            selected.getAccessibleContext().getAccessibleRole() == AccessibleRole.MENU_ITEM,
            "role is MENU_ITEM");
    checks++;
    failures +=
        check(
            "Selected".equals(selected.getAccessibleContext().getAccessibleName()),
            "accessible name is the label");
    checks++;
    failures +=
        check(
            selected
                .getAccessibleContext()
                .getAccessibleStateSet()
                .contains(AccessibleState.SELECTED),
            "selected item reports AccessibleState.SELECTED");
    checks++;

    // --- disabled is inert ---
    final int[] fired = {0};
    final ElwhaMenuItem inert = ElwhaMenuItem.of("Inert");
    inert.addActionListener(e -> fired[0]++);
    inert.setEnabled(false);
    inert.activate(0);
    failures += check(fired[0] == 0, "disabled item does not fire on activate()");
    checks++;
    inert.setEnabled(true);
    inert.activate(0);
    failures += check(fired[0] == 1, "enabled item fires on activate()");
    checks++;

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static boolean paints(final ElwhaMenuItem item, final Dimension size) {
    try {
      item.setSize(size);
      item.doLayout();
      final BufferedImage img =
          new BufferedImage(
              Math.max(1, size.width), Math.max(1, size.height), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g = img.createGraphics();
      try {
        item.paint(g);
      } finally {
        g.dispose();
      }
      return true;
    } catch (final RuntimeException ex) {
      System.out.println("    paint threw: " + ex);
      return false;
    }
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
