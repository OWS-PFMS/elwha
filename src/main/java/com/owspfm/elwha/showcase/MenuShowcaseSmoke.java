package com.owspfm.elwha.showcase;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * Headless guard for epic #298 S7 — the ElwhaMenu Showcase leaf must build without throwing, since
 * the Showcase eagerly constructs every component leaf (including the Gallery's {@code
 * renderPreview()} snapshots) at startup; a throw there breaks the whole app.
 *
 * <p>Reflectively invokes {@code ElwhaShowcase.buildMenuComponent()} (the same factory {@code
 * populateCatalog} registers) and asserts it returns a two-tab {@link JTabbedPane} (Workbench +
 * Gallery) whose Gallery contains rendered menu surfaces (role {@code POPUP_MENU}). Exits non-zero
 * on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuShowcaseSmoke {

  private MenuShowcaseSmoke() {}

  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    int checks = 0;
    int failures = 0;

    final Method build = ElwhaShowcase.class.getDeclaredMethod("buildMenuComponent");
    build.setAccessible(true);
    final Object component = build.invoke(null);

    failures += check(component instanceof JTabbedPane, "menu leaf is a JTabbedPane");
    checks++;

    if (component instanceof JTabbedPane tabs) {
      failures +=
          check(
              tabs.getTabCount() == 2,
              "leaf has Workbench + Gallery tabs, got " + tabs.getTabCount());
      checks++;
      final int popupMenus = countPopupMenus(tabs);
      failures +=
          check(popupMenus >= 3, "Gallery rendered >= 3 menu surfaces, found " + popupMenus);
      checks++;
    }

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static int countPopupMenus(final Component root) {
    int count = 0;
    if (root instanceof JComponent jc
        && jc.getAccessibleContext() != null
        && javax.accessibility.AccessibleRole.POPUP_MENU.equals(
            jc.getAccessibleContext().getAccessibleRole())) {
      count++;
    }
    if (root instanceof Container c) {
      for (final Component child : c.getComponents()) {
        count += countPopupMenus(child);
      }
    }
    return count;
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
