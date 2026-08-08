package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the chassis's {@code VerticalCardLayout} — spec §3.3 (add order is stack
 * order), §3.4 (the chassis honors parent width and children fit inside it), §5.2 (media bleeds to
 * the chassis edge at the first or last slot), §5.4 (a FULL divider bleeds anywhere), and the M3
 * rule that a trailing action row sits on the bottom edge rather than stranded mid-air.
 *
 * <p>Content is measured with fixed-size stubs rather than text atoms: geometry assertions against
 * a wrapped {@code JLabel} would be assertions about font metrics.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardLayoutTest {

  private static final int WIDTH = 300;
  private static final int HEIGHT = 400;
  private static final int PAD = SpaceScale.LG.px();
  private static final int GAP = SpaceScale.SM.px();

  /** A child with a pinned preferred size, so every position below is exact arithmetic. */
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

  private static ElwhaCard laidOut(final ElwhaCard card, final int width, final int height) {
    card.setSize(width, height);
    card.doLayout();
    return card;
  }

  private static ElwhaCardMedia media() {
    return ElwhaCardMedia.painter((g, w, h) -> {});
  }

  // ------------------------------------------------------------------- RTL

  @Test
  void headerRowMirrorsUnderRightToLeft() {
    final ElwhaCardHeader header = new ElwhaCardHeader().setTitle("Title");
    final Block leading = new Block(24, 24);
    final Block trailing = new Block(24, 24);
    header.setLeading(leading);
    header.addTrailing(trailing);
    header.setSize(WIDTH, header.getPreferredSize().height);

    header.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    header.doLayout();
    assertThat(leading.getX()).as("left-to-right puts the leading slot at the left").isZero();
    final int ltrTrailingX = trailing.getParent().getX();

    header.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    header.doLayout();

    assertThat(leading.getParent().getX() + leading.getParent().getWidth())
        .as("under RTL the leading slot mirrors to the right edge (#607)")
        .isEqualTo(WIDTH);
    assertThat(trailing.getParent().getX())
        .as("and the trailing affordances swap to the left")
        .isLessThan(ltrTrailingX);
  }

  @Test
  void actionRowMirrorsUnderRightToLeft() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    final Block lead = new Block(60, 30);
    final Block trail = new Block(60, 30);
    actions.addLeading(lead);
    actions.addTrailing(trail);
    actions.setSize(WIDTH, actions.getPreferredSize().height);

    actions.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    actions.doLayout();
    assertThat(lead.getX()).as("left-to-right anchors the leading action at the left").isZero();

    actions.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    actions.doLayout();

    assertThat(lead.getX() + lead.getWidth())
        .as("under RTL the leading action anchors to the right edge (#607)")
        .isEqualTo(WIDTH);
    assertThat(trail.getX()).as("and the trailing action to the left").isZero();
  }

  // ------------------------------------------------------------ stack order

  @Test
  void childrenStackTopToBottomInAddOrder() {
    final ElwhaCard card = new ElwhaCard();
    final Block first = new Block(100, 20);
    final Block second = new Block(100, 30);
    card.add(first);
    card.add(second);

    laidOut(card, WIDTH, HEIGHT);

    final Insets reserve = card.getInsets();
    assertThat(first.getY())
        .as("the first child starts one padding row below the body's top edge")
        .isEqualTo(reserve.top + PAD);
    assertThat(second.getY())
        .as("the next child follows a single inter-element gap later")
        .isEqualTo(first.getY() + first.getHeight() + GAP);
  }

  @Test
  void everyOrdinaryChildIsInsetByTheHorizontalPadding() {
    final ElwhaCard card = new ElwhaCard();
    final Block block = new Block(100, 20);
    card.add(block);

    laidOut(card, WIDTH, HEIGHT);

    final Insets reserve = card.getInsets();
    assertThat(block.getX())
        .as("content starts one padding column inside the body")
        .isEqualTo(reserve.left + PAD);
    assertThat(block.getWidth())
        .as("and stretches to the body width less both padding columns")
        .isEqualTo(WIDTH - reserve.left - reserve.right - 2 * PAD);
  }

  @Test
  void horizontalPaddingTokenDrivesThatInset() {
    final ElwhaCard card = new ElwhaCard().setPadding(SpaceScale.XS, SpaceScale.LG);
    final Block block = new Block(100, 20);
    card.add(block);

    laidOut(card, WIDTH, HEIGHT);

    assertThat(block.getX())
        .as("a narrower padding token moves content closer to the body edge")
        .isEqualTo(card.getInsets().left + SpaceScale.XS.px());
  }

  @Test
  void hiddenChildrenAreSkippedRatherThanReservedFor() {
    final ElwhaCard card = new ElwhaCard();
    final Block hidden = new Block(100, 40);
    final Block shown = new Block(100, 20);
    card.add(hidden);
    card.add(shown);
    hidden.setVisible(false);

    laidOut(card, WIDTH, HEIGHT);

    assertThat(shown.getY())
        .as("a hidden sibling leaves no hole — the visible child takes the first slot")
        .isEqualTo(card.getInsets().top + PAD);
  }

  @Test
  void aChildNeverExtendsPastTheBodyBounds() {
    final ElwhaCard card = new ElwhaCard();
    final Block wide = new Block(9000, 20);
    card.add(wide);

    laidOut(card, WIDTH, HEIGHT);

    final Insets reserve = card.getInsets();
    assertThat(wide.getX() + wide.getWidth())
        .as("§3.4 — a child wider than the card is compressed, never allowed to overhang")
        .isLessThanOrEqualTo(WIDTH - reserve.right);
  }

  @Test
  void aNarrowChassisCompressesChildrenRatherThanClippingThem() {
    final ElwhaCard card = new ElwhaCard();
    final Block block = new Block(200, 20);
    card.add(block);

    laidOut(card, 120, HEIGHT);
    final int atNarrow = block.getWidth();
    laidOut(card, 300, HEIGHT);

    assertThat(atNarrow)
        .as("the chassis passes its own narrowed width down instead of holding content wide")
        .isLessThan(block.getWidth());
  }

  // ------------------------------------------------------------ edge bleed

  @Test
  void mediaAtTheTopBleedsToTheBodyEdgeAndDropsTheTopPadding() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardMedia cover = media();
    card.add(cover);
    card.add(new Block(100, 20));

    laidOut(card, WIDTH, HEIGHT);

    final Insets reserve = card.getInsets();
    assertThat(cover.getX())
        .as("a leading media cover starts at the body edge, not inside the padding")
        .isEqualTo(reserve.left);
    assertThat(cover.getWidth())
        .as("and spans the full body width")
        .isEqualTo(WIDTH - reserve.left - reserve.right);
    assertThat(cover.getY())
        .as("the top padding row is dropped so the cover meets the chassis corner")
        .isEqualTo(reserve.top);
  }

  @Test
  void mediaInTheMiddleIsPaddedLikeAnyOtherChild() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardMedia cover = media();
    card.add(new Block(100, 20));
    card.add(cover);
    card.add(new Block(100, 20));

    laidOut(card, WIDTH, HEIGHT);

    assertThat(cover.getX())
        .as("only the first or last slot bleeds — media in between respects the padding")
        .isEqualTo(card.getInsets().left + PAD);
  }

  @Test
  void mediaAtTheBottomDropsTheBottomPadding() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardMedia cover = media();
    final Block block = new Block(100, 20);
    card.add(block);
    card.add(cover);

    // Two passes: the cover's height is a function of the width it is given, so the card's own
    // preferred height is only meaningful once it has been laid out at the width under test.
    laidOut(card, WIDTH, HEIGHT);
    final int naturalHeight = card.getPreferredSize().height;
    laidOut(card, WIDTH, naturalHeight);

    assertThat(cover.getY() + cover.getHeight())
        .as("a trailing cover runs to the body's bottom edge with no padding row after it")
        .isEqualTo(naturalHeight - card.getInsets().bottom);
  }

  @Test
  void mediaHeightFollowsTheSlotWidthThroughTheAspectRatio() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardMedia cover = media().setAspectRatio(2.0);
    card.add(cover);

    laidOut(card, WIDTH, HEIGHT);

    assertThat(cover.getHeight())
        .as("cover-fit sizing derives the media height from the width it was actually given")
        .isEqualTo(Math.round(cover.getWidth() / 2.0f));
  }

  @Test
  void anExplicitMediaHeightOverridesTheAspectRatio() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardMedia cover = media().setAspectRatio(2.0).setPreferredHeight(77);
    card.add(cover);

    laidOut(card, WIDTH, HEIGHT);

    assertThat(cover.getHeight())
        .as("a pinned height wins over the derived one, at any chassis width")
        .isEqualTo(77);
  }

  // -------------------------------------------------------------- dividers

  @Test
  void aFullDividerBleedsEvenInTheMiddleOfTheStack() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardDivider rule = new ElwhaCardDivider(DividerStyle.FULL);
    card.add(new Block(100, 20));
    card.add(rule);
    card.add(new Block(100, 20));

    laidOut(card, WIDTH, HEIGHT);

    final Insets reserve = card.getInsets();
    assertThat(rule.getX())
        .as("FULL means edge to edge regardless of where the divider sits in the stack")
        .isEqualTo(reserve.left);
    assertThat(rule.getWidth())
        .as("so the 1 dp line meets the chassis on both sides")
        .isEqualTo(WIDTH - reserve.left - reserve.right);
  }

  @Test
  void anInsetDividerStaysInsideTheContentPadding() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardDivider rule = new ElwhaCardDivider();
    card.add(new Block(100, 20));
    card.add(rule);
    card.add(new Block(100, 20));

    laidOut(card, WIDTH, HEIGHT);

    assertThat(rule.getStyle())
        .as("a bare divider is INSET — the more common M3 in-body separator")
        .isEqualTo(DividerStyle.INSET);
    assertThat(rule.getX())
        .as("INSET lines up with the text it separates")
        .isEqualTo(card.getInsets().left + PAD);
  }

  // --------------------------------------------------------------- actions

  @Test
  void aTrailingActionRowIsPushedToTheBottomWhenTheCardHasSlack() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardActions actions = new ElwhaCardActions();
    actions.addTrailing(new Block(60, 30));
    card.add(new Block(100, 20));
    card.add(actions);

    final int natural = card.getPreferredSize().height;
    laidOut(card, WIDTH, natural + 120);

    assertThat(actions.getY() + actions.getHeight())
        .as("M3 puts actions on the card's bottom edge, not floating above the slack")
        .isEqualTo(natural + 120 - card.getInsets().bottom - PAD);
  }

  @Test
  void anActionRowStaysPutWhenTheCardIsExactlyItsNaturalHeight() {
    final ElwhaCard card = new ElwhaCard();
    final ElwhaCardActions actions = new ElwhaCardActions();
    actions.addTrailing(new Block(60, 30));
    card.add(new Block(100, 20));
    card.add(actions);

    final int natural = card.getPreferredSize().height;
    laidOut(card, WIDTH, natural);

    assertThat(actions.getY())
        .as("with no slack there is nothing to push against — the row keeps its stacked slot")
        .isEqualTo(card.getInsets().top + PAD + 20 + GAP);
  }

  @Test
  void aTrailingBlockThatIsNotAnActionRowIsNotPushedDown() {
    final ElwhaCard card = new ElwhaCard();
    final Block tail = new Block(100, 30);
    card.add(new Block(100, 20));
    card.add(tail);

    laidOut(card, WIDTH, card.getPreferredSize().height + 120);

    assertThat(tail.getY())
        .as("only an action row claims the bottom edge; ordinary content stays stacked")
        .isEqualTo(card.getInsets().top + PAD + 20 + GAP);
  }

  @Test
  void actionRowReservesHeightForTheRowsItWillWrapInto() {
    final ElwhaCardActions actions = new ElwhaCardActions();
    actions.addLeading(new Block(80, 30));
    actions.addLeading(new Block(80, 30));

    final int single = actions.heightForSlotWidth(400);
    final int wrapped = actions.heightForSlotWidth(100);

    assertThat(single).as("both buttons fit on one row at a generous width").isEqualTo(30);
    assertThat(wrapped)
        .as("at a width that fits only one, the chassis is told to reserve two rows plus the gap")
        .isEqualTo(2 * 30 + GAP);
  }

  @Test
  void anEmptyActionRowAsksForNoHeightAtAll() {
    assertThat(new ElwhaCardActions().heightForSlotWidth(300))
        .as("a row with nothing in it must not open a gap in the stack")
        .isZero();
  }

  // ------------------------------------------------------- preferred size

  @Test
  void preferredHeightIsTheStackPlusPaddingPlusTheHalo() {
    final ElwhaCard card = new ElwhaCard();
    card.add(new Block(100, 20));
    card.add(new Block(100, 30));

    final Insets reserve = card.getInsets();

    assertThat(card.getPreferredSize().height)
        .as("two children, one gap, two padding rows, and the shadow reserve")
        .isEqualTo(20 + GAP + 30 + 2 * PAD + reserve.top + reserve.bottom);
  }

  @Test
  void widestChildDrivesThePreferredWidth() {
    final ElwhaCard card = new ElwhaCard();
    card.add(new Block(80, 20));
    card.add(new Block(160, 20));

    final Insets reserve = card.getInsets();

    assertThat(card.getPreferredSize().width)
        .as("the card asks for enough room for its widest child plus padding and halo")
        .isEqualTo(160 + 2 * PAD + reserve.left + reserve.right);
  }

  @Test
  void reuseBufferOverloadCarriesTheCardsOwnWorstCaseReserve() {
    final ElwhaCard card = new ElwhaCard();
    final Insets buffer = new Insets(0, 0, 0, 0);

    assertThat(card.getInsets(buffer))
        .as("the card reserves for MAX_ELEVATION; both overloads must say so")
        .isEqualTo(card.getInsets());
  }

  @Test
  void aLeadingMediaCoverDropsOneOfThePaddingRowsFromThePreferredHeight() {
    final ElwhaCard withMedia = new ElwhaCard();
    withMedia.add(media().setPreferredHeight(50));
    final ElwhaCard withBlock = new ElwhaCard();
    withBlock.add(new Block(100, 50));

    assertThat(withMedia.getPreferredSize().height)
        .as("a bleeding cover is both the first and last child, so both padding rows go")
        .isEqualTo(withBlock.getPreferredSize().height - 2 * PAD);
  }
}
