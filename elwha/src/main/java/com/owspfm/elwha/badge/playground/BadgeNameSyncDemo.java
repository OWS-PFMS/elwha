package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Headless verification harness for the badge-anchor accessible-name sync fix ([#220], option c).
 * Not an interactive playground — it drives the push-model a11y splice and exits non-zero on any
 * failure, so it doubles as a smoke gate.
 *
 * <p>The anchor splices {@code "{hostBaseName} {badge.accessibilityText}"} onto the host's
 * accessible name. The bug: the base name was captured once at attach and never re-read, so a
 * consumer that relabeled the host after attach (locale switch, dynamic name) had its new name
 * clobbered by the next splice, and detach restored the stale pre-attach name. Option c installs a
 * listener on the host's {@link AccessibleContext} that adopts a consumer's post-attach name as the
 * new base.
 *
 * <p>Two scenarios:
 *
 * <ol>
 *   <li><strong>Base case (unchanged):</strong> with no consumer relabel, the base stays the
 *       pre-attach name across badge-content changes, and detach restores it.
 *   <li><strong>Consumer relabel ([#220] fix):</strong> after the consumer mutates the host's
 *       accessible name post-attach, the next splice uses the new base (not the stale one), and
 *       detach restores the consumer's latest base.
 * </ol>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class BadgeNameSyncDemo {

  private BadgeNameSyncDemo() {}

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    baseCaseUnchanged();
    consumerRelabelTracked();

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: all badge name-sync checks passed.");
  }

  // No consumer relabel: the captured pre-attach base must survive content changes and be restored
  // on detach — the behavior that must not regress.
  private static void baseCaseUnchanged() {
    final JComponent host = host("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large("3");
    final ElwhaBadgeAnchor.Attachment att = ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    check("attach splices base + badge text", name(host).equals("Inbox 3 new notifications"));

    badge.setContent("7");
    check(
        "content change re-splices onto the captured base",
        name(host).equals("Inbox 7 new notifications"));

    ElwhaBadgeAnchor.detach(att);
    check("detach restores the pre-attach base", name(host).equals("Inbox"));
  }

  // Consumer relabels the host post-attach: the new name must become the base, and detach must
  // restore the consumer's latest name — not the stale pre-attach one.
  private static void consumerRelabelTracked() {
    final JComponent host = host("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large("3");
    final ElwhaBadgeAnchor.Attachment att = ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    host.getAccessibleContext().setAccessibleName("Boîte de réception");
    check(
        "consumer relabel re-splices onto the new base",
        name(host).equals("Boîte de réception 3 new notifications"));

    badge.setContent("5");
    check(
        "later content change uses the new base, not the stale one",
        name(host).equals("Boîte de réception 5 new notifications"));

    ElwhaBadgeAnchor.detach(att);
    check(
        "detach restores the consumer's latest base, not the stale pre-attach name",
        name(host).equals("Boîte de réception"));
  }

  private static JComponent host(final String accessibleName) {
    final JComponent host = new JPanel();
    host.getAccessibleContext().setAccessibleName(accessibleName);
    return host;
  }

  private static String name(final JComponent host) {
    final AccessibleContext ctx = host.getAccessibleContext();
    return ctx == null ? null : ctx.getAccessibleName();
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
