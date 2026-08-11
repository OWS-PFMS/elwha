package com.owspfm.elwha.badge;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.badge.AnchorFixture.IconHost;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRelation;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the badge accessibility surface — design doc §10: the push model (§10.1) that
 * splices the badge's announcement onto the host's accessible name, the default announcement
 * strings (§10.3), the host-name-first read order (§10.4), the non-focusable invariant (§10.5), the
 * {@link AccessibleRole#LABEL} role (§10.6), and the {@link AccessibleRelation#LABEL_FOR} back-link
 * the anchor installs.
 *
 * <p>Also covers the #220 lifecycle fix: a consumer that relabels the host after attach must have
 * that new value adopted as the splice base, not clobbered by the next splice and not lost at
 * detach.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaBadgeAccessibilityTest {

  private static IconHost mountedHost(final String accessibleName) {
    final IconHost host = new IconHost(new Rectangle(12, 8, 24, 24));
    host.getAccessibleContext().setAccessibleName(accessibleName);
    AnchorFixture.with(host, 20, 16, 48, 48);
    return host;
  }

  private static String hostName(final JComponent host) {
    return host.getAccessibleContext().getAccessibleName();
  }

  // ------------------------------------------------------ announcement text

  @Test
  void aSmallBadgeAnnouncesPresence() {
    assertThat(ElwhaBadge.small().getAccessibilityText())
        .as("§10.3 — Small carries no count, so it announces the fact of a notification")
        .isEqualTo("New notification");
  }

  @Test
  void aLargeBadgeAnnouncesItsCount() {
    assertThat(ElwhaBadge.large(3).getAccessibilityText())
        .as("§10.3 — Large announces the number it displays")
        .isEqualTo("3 new notifications");
  }

  @Test
  void announcementUsesTheCoercedContent() {
    assertThat(ElwhaBadge.large(4000).getAccessibilityText())
        .as("AT hears what is displayed, so the overflow sentinel carries through")
        .isEqualTo("999+ new notifications");
  }

  @Test
  void anAccessibilityOverrideReplacesTheDefault() {
    assertThat(
            ElwhaBadge.large(3)
                .setAccessibilityText("three unread messages")
                .getAccessibilityText())
        .as("§10.3 — the default is a fallback, not a ceiling")
        .isEqualTo("three unread messages");
  }

  @Test
  void aNullOverrideRevertsToTheDefault() {
    final ElwhaBadge badge = ElwhaBadge.large(3).setAccessibilityText("custom");

    badge.setAccessibilityText(null);

    assertThat(badge.getAccessibilityText())
        .as("clearing the override restores the derived string")
        .isEqualTo("3 new notifications");
  }

  @Test
  void accessibilityOverrideIsFluent() {
    final ElwhaBadge badge = ElwhaBadge.small();

    assertThat(badge.setAccessibilityText("x")).isSameAs(badge);
  }

  @Test
  void defaultAnnouncementTracksAContentChange() {
    final ElwhaBadge badge = ElwhaBadge.large(1);

    badge.setContent(9);

    assertThat(badge.getAccessibilityText()).isEqualTo("9 new notifications");
  }

  @Test
  void anOverrideSurvivesAContentChange() {
    final ElwhaBadge badge = ElwhaBadge.large(1).setAccessibilityText("pinned");

    badge.setContent(9);

    assertThat(badge.getAccessibilityText())
        .as("a consumer's explicit string is not silently recomputed out from under them")
        .isEqualTo("pinned");
  }

  // ------------------------------------------------- announcement events

  @Test
  void anOverrideFiresTheAccessibilityTextProperty() {
    final ElwhaBadge badge = ElwhaBadge.large(1);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_ACCESSIBILITY_TEXT, events::add);

    badge.setAccessibilityText("custom");

    assertThat(events).hasSize(1);
    assertThat(events.get(0).getOldValue()).isEqualTo("1 new notifications");
    assertThat(events.get(0).getNewValue()).isEqualTo("custom");
  }

  @Test
  void aContentChangeFiresTheAccessibilityTextProperty() {
    final ElwhaBadge badge = ElwhaBadge.large(1);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_ACCESSIBILITY_TEXT, events::add);

    badge.setContent(9);

    assertThat(events)
        .as("the derived announcement changed, so the anchor must re-splice")
        .hasSize(1);
    assertThat(events.get(0).getNewValue()).isEqualTo("9 new notifications");
  }

  @Test
  void aContentChangeUnderAnOverrideFiresNoAnnouncementEvent() {
    final ElwhaBadge badge = ElwhaBadge.large(1).setAccessibilityText("pinned");
    final List<PropertyChangeEvent> events = new ArrayList<>();
    badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_ACCESSIBILITY_TEXT, events::add);

    badge.setContent(9);

    assertThat(events)
        .as("the effective announcement did not change, so nothing needs re-splicing")
        .isEmpty();
  }

  // ------------------------------------------------------- accessible shape

  @Test
  void badgeReportsTheLabelRole() {
    assertThat(ElwhaBadge.small().getAccessibleContext().getAccessibleRole())
        .as("§10.6 — a badge labels its host; it is not a control")
        .isEqualTo(AccessibleRole.LABEL);
  }

  @Test
  void accessibleNameFallsBackToTheAnnouncement() {
    assertThat(ElwhaBadge.large(2).getAccessibleContext().getAccessibleName())
        .as("an unattached badge still reads sensibly on its own")
        .isEqualTo("2 new notifications");
  }

  @Test
  void anExplicitAccessibleNameWinsOverTheAnnouncement() {
    final ElwhaBadge badge = ElwhaBadge.large(2);

    badge.getAccessibleContext().setAccessibleName("override");

    assertThat(badge.getAccessibleContext().getAccessibleName()).isEqualTo("override");
  }

  @Test
  void anUnattachedBadgeLabelsNothing() {
    assertThat(
            ElwhaBadge.small()
                .getAccessibleContext()
                .getAccessibleRelationSet()
                .contains(AccessibleRelation.LABEL_FOR))
        .as("the LABEL_FOR back-link is the anchor's doing, not the badge's")
        .isFalse();
  }

  @Test
  void attachingInstallsTheLabelForRelationBackToTheHost() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(3);

    ElwhaBadgeAnchor.attach(host, badge);

    final AccessibleRelation relation =
        badge.getAccessibleContext().getAccessibleRelationSet().get(AccessibleRelation.LABEL_FOR);
    assertThat(relation).as("§10 — AT can walk from the badge to what it annotates").isNotNull();
    assertThat(relation.getTarget()).contains(host);
  }

  @Test
  void detachingClearsTheLabelForRelation() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(3);
    final ElwhaBadgeAnchor.Attachment attachment = ElwhaBadgeAnchor.attach(host, badge);

    ElwhaBadgeAnchor.detach(attachment);

    assertThat(
            badge
                .getAccessibleContext()
                .getAccessibleRelationSet()
                .contains(AccessibleRelation.LABEL_FOR))
        .as("a detached badge no longer claims to label anything")
        .isFalse();
  }

  // ------------------------------------------------------------ push model

  @Test
  void attachingSplicesTheAnnouncementOntoTheHostName() {
    final IconHost host = mountedHost("Inbox");

    ElwhaBadgeAnchor.attach(host, ElwhaBadge.large(3));

    assertThat(hostName(host))
        .as("§10.4 — host name first, badge announcement after")
        .isEqualTo("Inbox 3 new notifications");
  }

  @Test
  void aHostWithNoNameGetsTheAnnouncementAlone() {
    final IconHost host = new IconHost(new Rectangle(12, 8, 24, 24));
    AnchorFixture.with(host, 20, 16, 48, 48);

    ElwhaBadgeAnchor.attach(host, ElwhaBadge.small());

    assertThat(hostName(host))
        .as("no base name means no leading space in the announcement")
        .isEqualTo("New notification");
  }

  @Test
  void aContentChangeUpdatesTheSplicedHostNameInLockStep() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(3);
    ElwhaBadgeAnchor.attach(host, badge);

    badge.setContent(9);

    assertThat(hostName(host))
        .as("§10.1 — the push model keeps the host name current without consumer involvement")
        .isEqualTo("Inbox 9 new notifications");
  }

  @Test
  void anAnnouncementOverrideUpdatesTheSplicedHostName() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(3);
    ElwhaBadgeAnchor.attach(host, badge);

    badge.setAccessibilityText("three unread");

    assertThat(hostName(host)).isEqualTo("Inbox three unread");
  }

  @Test
  void detachingRestoresThePreAttachHostName() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadgeAnchor.Attachment attachment =
        ElwhaBadgeAnchor.attach(host, ElwhaBadge.large(3));

    ElwhaBadgeAnchor.detach(attachment);

    assertThat(hostName(host))
        .as("§9.2 — detach leaves the host exactly as it was found")
        .isEqualTo("Inbox");
  }

  @Test
  void reAttachingDoesNotCompoundTheSplice() {
    final IconHost host = mountedHost("Inbox");
    ElwhaBadgeAnchor.attach(host, ElwhaBadge.large(3));

    ElwhaBadgeAnchor.attach(host, ElwhaBadge.large(7));

    assertThat(hostName(host))
        .as("§9.3 — the prior attachment is unwound first, so names never stack up")
        .isEqualTo("Inbox 7 new notifications");
  }

  // --------------------------------------------- #220 base-name lifecycle

  @Test
  void aConsumerRelabelAfterAttachIsAdoptedAsTheNewBase() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(3);
    ElwhaBadgeAnchor.attach(host, badge);

    host.getAccessibleContext().setAccessibleName("Bandeja de entrada");

    assertThat(hostName(host))
        .as("#220 — a locale switch after attach re-splices onto the new base")
        .isEqualTo("Bandeja de entrada 3 new notifications");
  }

  @Test
  void aLaterContentChangeSplicesOntoTheAdoptedBase() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(3);
    ElwhaBadgeAnchor.attach(host, badge);
    host.getAccessibleContext().setAccessibleName("Bandeja de entrada");

    badge.setContent(9);

    assertThat(hostName(host))
        .as("#220 — the stale pre-attach base must not come back on the next splice")
        .isEqualTo("Bandeja de entrada 9 new notifications");
  }

  @Test
  void detachRestoresTheConsumersLatestNameNotTheStaleOne() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadgeAnchor.Attachment attachment =
        ElwhaBadgeAnchor.attach(host, ElwhaBadge.large(3));
    host.getAccessibleContext().setAccessibleName("Bandeja de entrada");

    ElwhaBadgeAnchor.detach(attachment);

    assertThat(hostName(host))
        .as("#220 — the consumer's mutation is preserved through the whole attachment")
        .isEqualTo("Bandeja de entrada");
  }

  @Test
  void anchorsOwnSpliceIsNotMistakenForAConsumerRelabel() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadge badge = ElwhaBadge.large(1);
    ElwhaBadgeAnchor.attach(host, badge);

    badge.setContent(2);
    badge.setContent(3);

    assertThat(hostName(host))
        .as("#220 — without the re-entrancy guard each splice would become the next base")
        .isEqualTo("Inbox 3 new notifications");
  }

  @Test
  void aHostThatExposesNoAccessibleContextStillAnchorsCleanly() {
    final JComponent bare = new JComponent() {};
    AnchorFixture.with(bare, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(bare, new Rectangle(12, 8, 24, 24), badge);

    assertThat(bare.getAccessibleContext())
        .as("a plain JComponent exposes no context, which the splice has to tolerate")
        .isNull();
    assertThat(badge.getParent())
        .as("and the badge still mounts and places — a11y is spliced, not required")
        .isNotNull();
  }

  @Test
  void hostNameListenerIsRemovedOnDetach() {
    final IconHost host = mountedHost("Inbox");
    final ElwhaBadgeAnchor.Attachment attachment =
        ElwhaBadgeAnchor.attach(host, ElwhaBadge.large(3));
    ElwhaBadgeAnchor.detach(attachment);

    host.getAccessibleContext().setAccessibleName("Renamed later");

    assertThat(hostName(host))
        .as("a detached anchor must not keep rewriting the host's name")
        .isEqualTo("Renamed later");
  }

  @Test
  void accessibleContextIsStableAcrossCalls() {
    final ElwhaBadge badge = ElwhaBadge.small();
    final AccessibleContext first = badge.getAccessibleContext();

    assertThat(badge.getAccessibleContext())
        .as("AT holds onto the context, so it must not be rebuilt per call")
        .isSameAs(first);
  }
}
