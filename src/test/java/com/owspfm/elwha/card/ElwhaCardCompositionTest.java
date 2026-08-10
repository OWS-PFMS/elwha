package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the Layer 3 composition primitives — {@link ElwhaCardHeader}'s single-leading
 * / text-stack / N-trailing anatomy (spec §5.1) and {@link ElwhaCardActions}' two-segment row with
 * wrap (spec §5.3, #17) — plus the rule that every primitive in the family mounts through the plain
 * {@code card.add(...)} the V3 architecture is built on.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardCompositionTest {

  private static final int WIDTH = 320;
  private static final int HEIGHT = 400;

  /** A child with a pinned preferred size, so segment arithmetic below is exact. */
  private static final class Block extends JComponent {
    private final Dimension preferred;

    Block(final int width, final int height) {
      this.preferred = new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(preferred);
    }
  }

  private static void layOutDeeply(final Component component, final int width, final int height) {
    component.setSize(width, height);
    if (component instanceof Container container) {
      container.doLayout();
      for (final Component child : container.getComponents()) {
        layOutDeeply(child, child.getWidth(), child.getHeight());
      }
    }
  }

  // ---------------------------------------------------------- header slots

  @Test
  void aFreshHeaderCarriesNoSlots() {
    final ElwhaCardHeader header = new ElwhaCardHeader();

    assertThat(header.getLeading()).as("no leading item until one is set").isNull();
    assertThat(header.getTitle()).as("no title until one is set").isNull();
    assertThat(header.getSubtitle()).as("no subtitle until one is set").isNull();
    assertThat(header.getTrailingItems()).as("and an empty trailing row").isEmpty();
  }

  @Test
  void leadingSlotHoldsExactlyOneItem() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    final ElwhaCardLeadingIcon first = new ElwhaCardLeadingIcon(MaterialIcons.pushPin());
    final ElwhaCardThumbnail second = new ElwhaCardThumbnail(blankImage());

    header.setLeading(first);
    header.setLeading(second);

    assertThat(header.getLeading())
        .as("M3 allows one leading item, so a second set replaces rather than stacks")
        .isSameAs(second);
  }

  @Test
  void clearingTheLeadingSlotEmptiesIt() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    header.setLeading(new ElwhaCardLeadingIcon(MaterialIcons.pushPin()));

    header.clearLeading();

    assertThat(header.getLeading()).as("the slot reports empty again").isNull();
  }

  @Test
  void leadingSlotRejectsNull() {
    assertThatThrownBy(() -> new ElwhaCardHeader().setLeading(null))
        .as("clearing goes through clearLeading, not a null set")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void titleAndSubtitleHaveTextShorthands() {
    final ElwhaCardHeader header =
        new ElwhaCardHeader().setTitle("Recent activity").setSubtitle("Last 30 days");

    assertThat(header.getTitle())
        .as("the shorthand builds the typed atom for the caller")
        .isInstanceOf(ElwhaCardTitle.class);
    assertThat(header.getTitle().getText())
        .as("carrying the supplied text")
        .contains("Recent activity");
    assertThat(header.getSubtitle().getText())
        .as("and the same for the subtitle")
        .contains("Last 30 days");
  }

  @Test
  void aCallerBuiltAtomCanBeHandedInDirectly() {
    final ElwhaCardTitle custom = new ElwhaCardTitle("Custom");
    final ElwhaCardSubtitle sub = new ElwhaCardSubtitle("Sub");

    final ElwhaCardHeader header = new ElwhaCardHeader().setTitle(custom).setSubtitle(sub);

    assertThat(header.getTitle())
        .as("typography customization goes through a pre-built atom")
        .isSameAs(custom);
    assertThat(header.getSubtitle()).as("the subtitle takes one too").isSameAs(sub);
  }

  @Test
  void replacingTheTitleDropsThePreviousOne() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    final ElwhaCardTitle first = new ElwhaCardTitle("first");
    header.setTitle(first);

    header.setTitle(new ElwhaCardTitle("second"));

    assertThat(descendants(header))
        .as("the old title is removed from the stack rather than left painting underneath")
        .doesNotContain(first);
  }

  @Test
  void titleLeadsTheSubtitleWhicheverOrderTheyWereSet() {
    final ElwhaCardHeader subtitleFirst = new ElwhaCardHeader();
    subtitleFirst.setSubtitle("second line");
    subtitleFirst.setTitle("first line");
    layOutDeeply(subtitleFirst, 240, 60);

    assertThat(subtitleFirst.getTitle().getY())
        .as("the title always sits above the subtitle, even when set afterwards")
        .isLessThanOrEqualTo(subtitleFirst.getSubtitle().getY());
  }

  @Test
  void headerAtomSettersRejectNull() {
    final ElwhaCardHeader header = new ElwhaCardHeader();

    assertThatThrownBy(() -> header.setTitle((ElwhaCardTitle) null))
        .as("a null title atom is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> header.setSubtitle((ElwhaCardSubtitle) null))
        .as("a null subtitle atom is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> header.addTrailing(null))
        .as("a null trailing affordance is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void trailingRowIsOpenEndedAndPolymorphic() {
    final ElwhaCardHeader header = new ElwhaCardHeader().setTitle("t");
    final ElwhaIconButton overflow = new ElwhaIconButton(MaterialIcons.moreVert());
    final Block chipStandIn = new Block(40, 24);

    header.addTrailing(overflow).addTrailing(chipStandIn);

    assertThat(header.getTrailingItems())
        .as("trailing accepts any affordance type and keeps insertion order")
        .containsExactly(overflow, chipStandIn);
  }

  @Test
  void trailingSnapshotDoesNotTrackOrMutate() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    header.addTrailing(new Block(10, 10));
    final List<JComponent> snapshot = header.getTrailingItems();

    header.addTrailing(new Block(10, 10));

    assertThat(snapshot).as("the snapshot does not follow later additions").hasSize(1);
    assertThatThrownBy(() -> snapshot.add(new Block(1, 1)))
        .as("and cannot be used to reach into the header")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void clearingTrailingRemovesEveryAffordance() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    final Block affordance = new Block(20, 20);
    header.addTrailing(affordance);

    header.clearTrailing();

    assertThat(header.getTrailingItems()).as("the list is emptied").isEmpty();
    assertThat(descendants(header))
        .as("and the component leaves the tree rather than lingering invisibly")
        .doesNotContain(affordance);
  }

  @Test
  void headerPlacesLeadingFirstAndTrailingLast() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    final ElwhaCardThumbnail leading = new ElwhaCardThumbnail(blankImage());
    final Block trailing = new Block(24, 24);
    header.setLeading(leading);
    header.setTitle("Title");
    header.addTrailing(trailing);

    layOutDeeply(header, 300, 60);

    assertThat(leading.getX() + leading.getWidth())
        .as("§5.1 — the leading column is anchored to the start of the row")
        .isLessThanOrEqualTo(header.getTitle().getParent().getX());
    assertThat(trailing.getParent().getX() + trailing.getParent().getWidth())
        .as("and the trailing row to the end of it")
        .isEqualTo(300);
  }

  @Test
  void anEmptySegmentTakesNoWidth() {
    final ElwhaCardHeader withOnlyText = new ElwhaCardHeader().setTitle("Title");
    final ElwhaCardHeader withLeading = new ElwhaCardHeader().setTitle("Title");
    withLeading.setLeading(new ElwhaCardThumbnail(blankImage()));

    assertThat(withOnlyText.getPreferredSize().width)
        .as("a header with no leading item does not reserve the column or its gap")
        .isLessThan(withLeading.getPreferredSize().width);
  }

  // ------------------------------------------------- the add() guard (#768)

  @Test
  void externalAddFailsFastAndNamesTheSlotApi() {
    final ElwhaCardHeader header = new ElwhaCardHeader();

    assertThatThrownBy(() -> header.add(new ElwhaCardTitle("orphan")))
        .as("#768 — an external add() throws instead of silently rendering nothing")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("setTitle")
        .hasMessageContaining("setSubtitle")
        .hasMessageContaining("setLeading")
        .hasMessageContaining("addTrailing");
  }

  @Test
  void everyAddOverloadIsGuarded() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    final Block child = new Block(10, 10);

    assertThatThrownBy(() -> header.add(child, 0))
        .as("the index overload funnels through the same guard")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> header.add(child, "constraint"))
        .as("and so does the constraints overload")
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(descendants(header))
        .as("a rejected child never joins the tree")
        .doesNotContain(child);
  }

  @Test
  void allFourSanctionedMutatorsStillPassTheGuard() {
    final ElwhaCardHeader header =
        new ElwhaCardHeader()
            .setLeading(new ElwhaCardThumbnail(blankImage()))
            .setTitle("Recent activity")
            .setSubtitle("Last 30 days")
            .addTrailing(new Block(24, 24));

    assertThat(header.getLeading()).as("setLeading still mounts").isNotNull();
    assertThat(header.getTitle()).as("setTitle still mounts").isNotNull();
    assertThat(header.getSubtitle()).as("setSubtitle still mounts").isNotNull();
    assertThat(header.getTrailingItems()).as("addTrailing still mounts").hasSize(1);
  }

  // --------------------------------------------------------- actions row

  @Test
  void actionsStartEmptyOnBothSegments() {
    final ElwhaCardActions actions = new ElwhaCardActions();

    assertThat(actions.getLeadingActions()).as("no leading actions yet").isEmpty();
    assertThat(actions.getTrailingActions()).as("and no trailing actions").isEmpty();
  }

  @Test
  void actionsKeepInsertionOrderPerSegment() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    final Block leadA = new Block(50, 30);
    final Block leadB = new Block(50, 30);
    final Block trail = new Block(50, 30);

    actions.addLeading(leadA).addLeading(leadB).addTrailing(trail);

    assertThat(actions.getLeadingActions())
        .as("the leading segment keeps the order it was built in")
        .containsExactly(leadA, leadB);
    assertThat(actions.getTrailingActions())
        .as("and the trailing segment is tracked separately")
        .containsExactly(trail);
  }

  @Test
  void actionSettersRejectNull() {
    final ElwhaCardActions actions = new ElwhaCardActions();

    assertThatThrownBy(() -> actions.addLeading(null))
        .as("a null leading action is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> actions.addTrailing(null))
        .as("a null trailing action is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void clearingOneSegmentLeavesTheOtherAlone() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    final Block lead = new Block(50, 30);
    final Block trail = new Block(50, 30);
    actions.addLeading(lead).addTrailing(trail);

    actions.clearLeading();

    assertThat(actions.getLeadingActions()).as("the cleared segment empties").isEmpty();
    assertThat(actions.getTrailingActions())
        .as("the other segment survives")
        .containsExactly(trail);
    assertThat(descendants(actions))
        .as("the cleared action leaves the component tree")
        .doesNotContain(lead)
        .contains(trail);
  }

  @Test
  void actionSnapshotsAreUnmodifiable() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    actions.addTrailing(new Block(10, 10));

    assertThatThrownBy(() -> actions.getTrailingActions().add(new Block(1, 1)))
        .as("callers read the segment, they do not edit it in place")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void aWideRowAnchorsLeadingLeftAndTrailingRight() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    final Block lead = new Block(60, 30);
    final Block trail = new Block(60, 30);
    actions.addLeading(lead).addTrailing(trail);

    layOutDeeply(actions, 400, 30);

    assertThat(lead.getX()).as("§5.3 — the promo action sits at the start of the row").isZero();
    assertThat(trail.getX() + trail.getWidth())
        .as("and the confirming action at the end, with the slack between them")
        .isEqualTo(400);
  }

  @Test
  void aNarrowRowWrapsLeadingAboveTrailing() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    final Block lead = new Block(80, 30);
    final Block trail = new Block(80, 30);
    actions.addLeading(lead).addTrailing(trail);

    layOutDeeply(actions, 100, actions.heightForSlotWidth(100));

    assertThat(lead.getY())
        .as("#17 — a row too narrow for one line wraps rather than clipping")
        .isLessThan(trail.getY());
    assertThat(lead.getX()).as("the leading segment keeps its own alignment on its row").isZero();
    assertThat(trail.getX() + trail.getWidth())
        .as("and the trailing segment keeps its right anchor on its row")
        .isEqualTo(100);
  }

  @Test
  void aSegmentPacksGreedilyBeforeStartingANewRow() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    for (int i = 0; i < 3; i++) {
      actions.addLeading(new Block(60, 30));
    }

    assertThat(actions.heightForSlotWidth(150))
        .as("two 60 px items plus a gap fit on the first row, so the third opens a second one")
        .isEqualTo(2 * 30 + 8);
  }

  @Test
  void anActionWiderThanTheRowGetsItsOwnRowRatherThanBlockingLayout() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    actions.addLeading(new Block(400, 30));

    assertThat(actions.heightForSlotWidth(50))
        .as("an over-wide action still occupies exactly one row — no infinite wrap")
        .isEqualTo(30);
  }

  // ---------------------------------------------------------- mounting

  @Test
  void everyPrimitiveMountsThroughPlainCardAdd() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final List<JComponent> primitives =
        List.of(
            new ElwhaCardHeader().setTitle("Title").setSubtitle("Sub"),
            new ElwhaCardSupportingText("Body copy"),
            new ElwhaCardDivider(),
            ElwhaCardMedia.painter((g, w, h) -> {}),
            new ElwhaCardActions().addTrailing(new Block(60, 30)));
    for (final JComponent primitive : primitives) {
      card.add(primitive);
    }

    layOutDeeply(card, WIDTH, HEIGHT);

    assertThat(descendants(card))
        .as("the V3 architecture composes through add(...), with no typed slot setters")
        .containsAll(primitives);
    for (final JComponent primitive : primitives) {
      assertThat(primitive.getWidth())
          .as("%s was given real bounds by the chassis", primitive.getClass().getSimpleName())
          .isPositive();
    }
  }

  private static List<Component> descendants(final Container root) {
    final List<Component> found = new ArrayList<>();
    collect(root, found);
    return found;
  }

  private static void collect(final Container parent, final List<Component> into) {
    for (final Component child : parent.getComponents()) {
      into.add(child);
      if (child instanceof Container container) {
        collect(container, into);
      }
    }
  }

  private static BufferedImage blankImage() {
    return new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
  }
}
