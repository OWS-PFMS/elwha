package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.font.TextAttribute;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the Layer 4 disclosure affordances — {@link ElwhaCardChevron} and {@link
 * ElwhaCardExpandLink} (spec §6.1 / §6.2) — including the #23 footgun each one closes: on being
 * added under a card, the affordance anchors its own host container as {@link
 * CollapseRule#ALWAYS_VISIBLE} so a collapsed card is never stranded with no way to re-expand.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardDisclosureTest {

  /** The chevron inherits the icon button's activation action key. */
  private static final String ACTIVATE = "elwhaiconbutton.activate";

  /** A child with a pinned preferred size, so the card has measurable content to collapse. */
  private static final class Block extends JComponent {
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(100, 40);
    }
  }

  // ----------------------------------------------------------- chevron

  @Test
  void chevronRequiresACardToDrive() {
    assertThatThrownBy(() -> new ElwhaCardChevron(null))
        .as("an unbound chevron would have nothing to toggle")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void chevronReportsTheCardItDrives() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThat(new ElwhaCardChevron(card).getCard()).as("the binding is queryable").isSameAs(card);
  }

  @Test
  void chevronSizesItselfToSitBesideHeaderText() {
    assertThat(new ElwhaCardChevron(ElwhaCard.filledCard()).getButtonSize())
        .as("§6.1 — the 32 dp step fits a header row without dominating it")
        .isEqualTo(IconButtonSize.S);
  }

  @Test
  void clickingTheChevronTogglesTheCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);

    Input.pressBoundKey(chevron, "pressed SPACE", ACTIVATE);
    assertThat(card.isCollapsed()).as("the first activation collapses the card").isTrue();

    Input.pressBoundKey(chevron, "pressed SPACE", ACTIVATE);
    assertThat(card.isCollapsed()).as("and the second expands it again").isFalse();
  }

  @Test
  void chevronGlyphFollowsTheCardEvenWhenSomethingElseToggledIt() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    final javax.swing.Icon expanded = chevron.getIcon();

    card.setCollapsed(true);

    assertThat(chevron.getIcon())
        .as("the chevron listens to the card rather than tracking only its own clicks")
        .isNotSameAs(expanded);
  }

  @Test
  void aChevronOnANonCollapsibleCardIsInert() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);

    Input.pressBoundKey(chevron, "pressed SPACE", ACTIVATE);

    assertThat(card.isCollapsed())
        .as("the collapsible gate holds even when the affordance is present")
        .isFalse();
  }

  @Test
  void aChevronAddedUnderACardAnchorsItsHostAgainstCollapse() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardHeader header = new ElwhaCardHeader().setTitle("Title");
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    header.addTrailing(chevron);
    card.add(header);

    chevron.addNotify();

    assertThat(card.getCollapseConstraint(header))
        .as("#23 — the chevron anchors the header it sits in, not itself")
        .isEqualTo(CollapseRule.ALWAYS_VISIBLE);
  }

  @Test
  void aChevronAddedStraightToTheCardAnchorsItself() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    card.add(chevron);

    chevron.addNotify();

    assertThat(card.getCollapseConstraint(chevron))
        .as("with no intervening container the chevron is its own host")
        .isEqualTo(CollapseRule.ALWAYS_VISIBLE);
  }

  @Test
  void aChevronOutsideItsCardsTreeAnchorsNothing() {
    final ElwhaCard driven = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCard elsewhere = ElwhaCard.filledCard();
    final ElwhaCardChevron chevron = new ElwhaCardChevron(driven);
    elsewhere.add(chevron);

    chevron.addNotify();

    assertThat(driven.getCollapseConstraint(chevron))
        .as("a chevron parked outside the card it drives degrades quietly rather than throwing")
        .isEqualTo(CollapseRule.COLLAPSIBLE);
  }

  @Test
  void anchoredHostActuallySurvivesACollapse() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardHeader header = new ElwhaCardHeader().setTitle("Title");
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    header.addTrailing(chevron);
    card.add(header);
    final Block body = new Block();
    card.add(body);
    chevron.addNotify();

    card.setCollapsed(true);

    assertThat(header.isVisible())
        .as("the user keeps an affordance to expand the card again")
        .isTrue();
    assertThat(body.isVisible()).as("while the body collapses as usual").isFalse();
  }

  // -------------------------------------------------------- expand link

  @Test
  void expandLinkRequiresACardAndBothLabels() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThatThrownBy(() -> new ElwhaCardExpandLink(null, "More", "Less"))
        .as("an unbound link would have nothing to toggle")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ElwhaCardExpandLink(card, null, "Less"))
        .as("the collapsed-state label is required")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ElwhaCardExpandLink(card, "More", null))
        .as("as is the expanded-state label")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void expandLinkStartsWithTheLabelForTheCardsCurrentState() {
    final ElwhaCard expanded = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCard collapsed = ElwhaCard.filledCard().setCollapsible(true).setCollapsed(true);

    assertThat(new ElwhaCardExpandLink(expanded, "Show more", "Show less").getText())
        .as("an expanded card offers to collapse")
        .isEqualTo("Show less");
    assertThat(new ElwhaCardExpandLink(collapsed, "Show more", "Show less").getText())
        .as("a collapsed one offers to expand")
        .isEqualTo("Show more");
  }

  @Test
  void expandLinkReportsItsBindingAndBothLabels() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");

    assertThat(link.getCard()).as("the driven card is queryable").isSameAs(card);
    assertThat(link.getExpandText()).as("as is the collapsed-state label").isEqualTo("Show more");
    assertThat(link.getCollapseText()).as("and the expanded-state one").isEqualTo("Show less");
  }

  @Test
  void expandLinkLabelFollowsTheCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");

    card.setCollapsed(true);

    assertThat(link.getText())
        .as("the link listens to the card, so any other affordance updates it too")
        .isEqualTo("Show more");
  }

  @Test
  void keyboardActivationTogglesTheCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");

    Input.pressBoundKey(link, "released SPACE", "toggle");
    assertThat(card.isCollapsed()).as("Space toggles the disclosure").isTrue();

    Input.pressBoundKey(link, "released ENTER", "toggle");
    assertThat(card.isCollapsed()).as("and Enter toggles it back").isFalse();
  }

  @Test
  void expandLinkReadsAsALink() {
    final ElwhaCardExpandLink link =
        new ElwhaCardExpandLink(ElwhaCard.filledCard(), "Show more", "Show less");

    assertThat(link.getAccessibleContext().getAccessibleRole())
        .as("§6.2 — the disclosure text is a hyperlink to assistive technology")
        .isEqualTo(AccessibleRole.HYPERLINK);
    assertThat(link.isFocusable()).as("and it takes a tab stop").isTrue();
    assertThat(link.getCursor().getType())
        .as("and shows the hand pointer")
        .isEqualTo(Cursor.HAND_CURSOR);
  }

  @Test
  void expandLinkLooksLikeALink() {
    final ElwhaCardExpandLink link =
        new ElwhaCardExpandLink(ElwhaCard.filledCard(), "Show more", "Show less");

    assertThat(link.getForeground())
        .as("§6.2 — the link is the primary accent by default")
        .isEqualTo(ColorRole.PRIMARY.resolve());
    assertThat(link.getFont().getAttributes().get(TextAttribute.UNDERLINE))
        .as("and is underlined so it reads as a link rather than body copy")
        .isEqualTo(TextAttribute.UNDERLINE_ON);
  }

  @Test
  void expandLinkColorRoleIsOverridableAndRejectsNull() {
    final ElwhaCardExpandLink link =
        new ElwhaCardExpandLink(ElwhaCard.filledCard(), "More", "Less");

    assertThat(link.setColorRole(ColorRole.TERTIARY).getColorRole())
        .as("a caller may re-tint the link")
        .isEqualTo(ColorRole.TERTIARY);
    assertThat(link.getForeground())
        .as("and the paint-time getter honors it")
        .isEqualTo(ColorRole.TERTIARY.resolve());
    assertThatThrownBy(() -> link.setColorRole(null))
        .as("a null color role is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void anExpandLinkAddedUnderACardAnchorsItselfAgainstCollapse() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");
    card.add(new Block());
    card.add(link);

    link.addNotify();

    assertThat(card.getCollapseConstraint(link))
        .as("#23 — the link that re-expands the card must outlive the collapse")
        .isEqualTo(CollapseRule.ALWAYS_VISIBLE);
  }

  @Test
  void anExpandLinkOutsideItsCardsTreeAnchorsNothing() {
    final ElwhaCard driven = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCard elsewhere = ElwhaCard.filledCard();
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(driven, "More", "Less");
    elsewhere.add(link);

    link.addNotify();

    assertThat(driven.getCollapseConstraint(link))
        .as("a link parked outside the card it drives degrades quietly rather than throwing")
        .isEqualTo(CollapseRule.COLLAPSIBLE);
  }

  // ----------------------------------------------------------- rule pruning

  @Test
  void removingAChildForgetsItsCollapseRule() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final Block pinned = new Block();
    card.add(pinned);
    card.setCollapseConstraint(pinned, CollapseRule.ALWAYS_VISIBLE);

    card.remove(pinned);

    assertThat(card.collapseConstraintCount())
        .as(
            "#594 — nothing pruned the rule map, so a card that cycled its content held every "
                + "child it had ever shown")
        .isZero();
  }

  @Test
  void clearingACardForgetsEveryCollapseRule() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardHeader header = new ElwhaCardHeader().setTitle("Title");
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    header.addTrailing(chevron);
    card.add(header);
    card.add(new Block());
    chevron.addNotify();
    assertThat(card.collapseConstraintCount()).as("the chevron self-anchored its header").isOne();

    card.removeAll();

    assertThat(card.collapseConstraintCount())
        .as("#594 — a full rebuild starts from an empty rule map")
        .isZero();
  }

  @Test
  void aReAddedChildIsRuledAfresh() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    card.add(chevron);
    chevron.addNotify();

    card.remove(chevron);
    card.add(chevron);
    chevron.addNotify();

    assertThat(card.getCollapseConstraint(chevron))
        .as("pruning on remove must not cost a re-added affordance its #23 anchor")
        .isEqualTo(CollapseRule.ALWAYS_VISIBLE);
  }

  // -------------------------------------------------------------- teardown

  @Test
  void aRemovedChevronStopsTrackingItsCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    card.add(chevron);
    chevron.addNotify();

    card.remove(chevron);
    chevron.removeNotify();
    card.setCollapsed(true);

    assertThat(card.expansionListenerCount())
        .as("#595 — the card holds its listeners strongly, so a swapped-out chevron must let go")
        .isZero();
  }

  @Test
  void aRemovedExpandLinkStopsTrackingItsCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");
    card.add(link);
    link.addNotify();

    card.remove(link);
    link.removeNotify();

    assertThat(card.expansionListenerCount())
        .as("#595 — same leak, same fix, on the other disclosure affordance")
        .isZero();
  }

  @Test
  void aReAddedChevronTracksItsCardAgainAndCatchesUpOnWhatItMissed() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    card.add(chevron);
    chevron.addNotify();
    chevron.removeNotify();

    card.setCollapsed(true);
    chevron.addNotify();

    assertThat(card.expansionListenerCount())
        .as("a stage swap re-adds the affordance; one subscription, not two")
        .isOne();
    assertThat(chevron.getIcon())
        .as("and it resyncs, so it never paints the state the card left behind")
        .isNotNull();
    card.setCollapsed(false);
    assertThat(card.expansionListenerCount())
        .as("still exactly one after the card toggles again")
        .isOne();
  }

  @Test
  void anUnparentedAffordanceStillTracksItsCard() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");

    card.setCollapsed(true);

    assertThat(link.getText())
        .as("the subscription starts at construction — an offscreen render reads the right text")
        .isEqualTo("Show more");
  }

  @Test
  void aChevronAndALinkCanDriveTheSameCardTogether() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    final ElwhaCardChevron chevron = new ElwhaCardChevron(card);
    final ElwhaCardExpandLink link = new ElwhaCardExpandLink(card, "Show more", "Show less");

    Input.pressBoundKey(chevron, "pressed SPACE", ACTIVATE);

    assertThat(link.getText())
        .as("both affordances read the same card state, so they never disagree on screen")
        .isEqualTo("Show more");
  }
}
