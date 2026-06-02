package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Headless smoke for #310: the floating FAB must take its scroll-shrink source only from a genuine
 * page-level scroll, never from a Workbench's inner panes (stage / controls / code view).
 *
 * <p>Drives the real {@link ElwhaShowcase#findScrollPane(Component)} selector (the method that
 * picks the FAB's scroll source) reflectively. Asserts: (1) a fully-built {@link
 * ComponentWorkbench} yields {@code null} — no eligible pane — even though its tree contains
 * several {@link JScrollPane} instances (so the {@code null} is non-vacuous: the panes exist, they
 * are all tagged ignore); and (2) a plain gallery panel holding one untagged scroll pane still
 * resolves to that pane, so a real page scroll keeps driving the FAB. Exits non-zero on any failed
 * assertion.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class FabScrollSourceSmoke {

  private FabScrollSourceSmoke() {}

  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final Method findScrollPane =
        ElwhaShowcase.class.getDeclaredMethod("findScrollPane", Component.class);
    findScrollPane.setAccessible(true);

    int checks = 0;
    int failures = 0;

    // --- A Workbench card: stage + controls + code-view scroll panes, all tagged ignore. ---
    final ComponentWorkbench workbench = new ComponentWorkbench();
    workbench.setStage(new ElwhaButton("Stage"));
    workbench.controls().addControl("Knob", new JLabel("value"));
    workbench.setCode("ElwhaButton b = new ElwhaButton(\"Stage\");");

    final int workbenchScrolls = countScrollPanes(workbench);
    failures +=
        check(
            workbenchScrolls >= 3,
            "Workbench tree contains >= 3 scroll panes (non-vacuous guard), found "
                + workbenchScrolls);
    checks++;

    final Object workbenchSource = findScrollPane.invoke(null, workbench);
    failures +=
        check(
            workbenchSource == null,
            "findScrollPane(workbench) == null (no inner pane drives the FAB), got "
                + workbenchSource);
    checks++;

    // --- A gallery / landing card: one genuine page-level scroll pane, untagged. ---
    final JPanel gallery = new JPanel();
    final JScrollPane pageScroll = new JScrollPane(new JLabel("page content"));
    gallery.add(pageScroll);

    final Object gallerySource = findScrollPane.invoke(null, gallery);
    failures +=
        check(
            gallerySource == pageScroll,
            "findScrollPane(gallery) returns the untagged page scroll (FAB still shrinks on real"
                + " page scroll)");
    checks++;

    // --- Tagging the gallery pane the same way must exclude it too (rule is general). ---
    pageScroll.putClientProperty(ComponentWorkbench.FAB_SCROLL_IGNORE, Boolean.TRUE);
    final Object taggedGallerySource = findScrollPane.invoke(null, gallery);
    failures +=
        check(
            taggedGallerySource == null,
            "findScrollPane ignores a pane once tagged FAB_SCROLL_IGNORE");
    checks++;

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }

  private static int countScrollPanes(final Component component) {
    int count = component instanceof JScrollPane ? 1 : 0;
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += countScrollPanes(child);
      }
    }
    return count;
  }
}
