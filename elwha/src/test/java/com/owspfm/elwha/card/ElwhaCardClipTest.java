package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the card's corner-clip rule (#157 / #272): the chassis forces {@code
 * ElwhaSurface}'s rounded child clip on — and pays for its offscreen buffer — only when an
 * edge-bleed {@link ElwhaCardMedia} is the first or last visible child. Every other card inherits
 * the surface default and skips the buffer, so a ripple-animating text card allocates nothing per
 * frame.
 *
 * <p>Also pins the z-order half of #157: an {@link CardVariant#OUTLINED} card repaints its outline
 * above the children, so an opaque media cover reaching the chassis edge cannot hide it.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardClipTest {

  private static final int WIDTH = 220;
  private static final int HEIGHT = 160;

  /** A saturated media fill that no theme role resolves to, so leaked pixels are unmistakable. */
  private static final Color MEDIA_INK = new Color(0, 200, 0);

  private static ElwhaCardMedia media() {
    return ElwhaCardMedia.painter(
        (g, w, h) -> {
          g.setColor(MEDIA_INK);
          g.fillRect(0, 0, w, h);
        });
  }

  private static ElwhaCardSupportingText text() {
    return new ElwhaCardSupportingText("body");
  }

  // ------------------------------------------------------ inherited chrome

  /**
   * The chain below is the assertion: every inherited {@code ElwhaSurface} setter the V3 spec §3.2
   * advertises as card API returns {@code ElwhaCard}, so a fluent chain can pass through one and
   * still reach a card-only setter. Before #570 only {@code setElevation} was covariant and this
   * did not compile.
   */
  @Test
  void aFluentChainSurvivesEveryInheritedSurfaceSetter() {
    final ElwhaCard card =
        ElwhaCard.filledCard()
            .setShape(ShapeScale.XL)
            .setSurfaceRole(ColorRole.SURFACE_CONTAINER_HIGH)
            .setBorderWidth(2)
            .setClipChildrenToCorners(true)
            .setElevation(3)
            .setActionable(true);

    assertThat(card.getShape()).as("and each call still lands").isEqualTo(ShapeScale.XL);
    assertThat(card.getSurfaceRole()).isEqualTo(ColorRole.SURFACE_CONTAINER_HIGH);
    assertThat(card.getBorderWidth()).isEqualTo(2);
    assertThat(card.getElevation()).isEqualTo(3);
    assertThat(card.isActionable()).isTrue();
  }

  // --------------------------------------------------------- atom clip rule

  @Test
  void aCircularThumbnailIntersectsTheInheritedClipRatherThanReplacingIt() {
    final BufferedImage source = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D ink = source.createGraphics();
    ink.setColor(MEDIA_INK);
    ink.fillRect(0, 0, 40, 40);
    ink.dispose();
    final ElwhaCardThumbnail thumbnail = new ElwhaCardThumbnail(source);
    thumbnail.setSize(40, 40);

    final BufferedImage shot = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = shot.createGraphics();
    // Stands in for the rounded-body clip ElwhaSurface.paintChildren hands every card child.
    g.setClip(0, 0, 20, 40);
    thumbnail.paint(g);
    g.dispose();

    assertThat(new Color(shot.getRGB(30, 20), true).getAlpha())
        .as("a point inside the thumbnail's circle but outside the chassis clip stays unpainted")
        .isZero();
    assertThat(new Color(shot.getRGB(10, 20), true).getAlpha())
        .as("while the part inside both clips still paints")
        .isNotZero();
  }

  // ------------------------------------------------------------ the trigger

  @Test
  void aTextOnlyCardDoesNotPayForTheClip() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(text());

    assertThat(card.clipsChildrenToCorners())
        .as("inset content never reaches the corners, so the offscreen buffer is skipped")
        .isFalse();
  }

  @Test
  void mediaAtTheTopForcesTheClip() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(media());
    card.add(text());

    assertThat(card.clipsChildrenToCorners())
        .as("a leading media cover reaches the top corners and must be cut to them")
        .isTrue();
  }

  @Test
  void mediaAtTheBottomForcesTheClip() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(text());
    card.add(media());

    assertThat(card.clipsChildrenToCorners())
        .as("a trailing media cover reaches the bottom corners and must be cut to them")
        .isTrue();
  }

  @Test
  void mediaStillForcesTheClipWhenTheCardScrollsItsOverflow() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.setExpansionOverflow(ExpansionOverflow.SCROLL);
    card.add(media());
    card.add(text());

    assertThat(card.clipsChildrenToCorners())
        .as("SCROLL re-parents children into the scroll body; the rule must follow them there")
        .isTrue();
  }

  @Test
  void mediaSandwichedBetweenSiblingsDoesNotForceTheClip() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(text());
    card.add(media());
    card.add(text());

    assertThat(card.clipsChildrenToCorners())
        .as("media in the middle is inset like any other child and touches no corner")
        .isFalse();
  }

  @Test
  void hiddenMediaDoesNotCountAsTheEdgeChild() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final ElwhaCardMedia cover = media();
    card.add(cover);
    card.add(text());

    cover.setVisible(false);

    assertThat(card.clipsChildrenToCorners())
        .as("the rule reads the first VISIBLE child — a hidden cover paints nothing to clip")
        .isFalse();
  }

  @Test
  void collapsingAwayTheMediaReleasesTheClip() {
    final ElwhaCard card = ElwhaCard.filledCard().setCollapsible(true);
    card.add(media());
    card.add(text());

    card.setCollapsed(true);

    assertThat(card.clipsChildrenToCorners())
        .as("collapse hides the cover, so the card stops buying the buffer while collapsed")
        .isFalse();
  }

  @Test
  void aConsumerCanStillTurnTheClipOnWithoutAnyMedia() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(text());

    card.setClipChildrenToCorners(true);

    assertThat(card.clipsChildrenToCorners())
        .as("the media rule widens the surface default, it does not replace the consumer flag")
        .isTrue();
  }

  @Test
  void mediaRuleDoesNotRewriteTheConsumerFlag() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(media());

    assertThat(card.getClipChildrenToCorners())
        .as("the public flag keeps reporting what a consumer set, not what the card resolved")
        .isFalse();
  }

  // -------------------------------------------------------------- the paint

  @Test
  void aClippedMediaCoverIsCutBackFromTheChassisCorner() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.setShape(ShapeScale.XL);
    card.add(media());
    final Insets reserve = card.getInsets();

    final BufferedImage image = Pixels.render(card, WIDTH, HEIGHT, Color.MAGENTA);

    Pixels.assertPixelNear(
        image,
        reserve.left + 1,
        reserve.top + 1,
        Color.MAGENTA,
        "the corner square outside the curve is left to the ground, not filled by the cover");
    Pixels.assertPixelNear(
        image,
        WIDTH / 2,
        reserve.top + 4,
        MEDIA_INK,
        "the cover still bleeds to the top body edge between the corners");
  }

  @Test
  void anOutlinedCardKeepsItsOutlineAboveAnEdgeBleedCover() {
    final ElwhaCard card = ElwhaCard.outlinedCard();
    card.add(media());
    final Insets reserve = card.getInsets();

    final BufferedImage image = Pixels.render(card, WIDTH, HEIGHT, Color.MAGENTA);

    Pixels.assertAnyPixelNear(
        image,
        WIDTH / 2,
        reserve.top,
        3,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "the outline repaints above the children, so the opaque cover cannot hide the top edge");
  }

  @Test
  void coverStillFillsTheBodyUnderThatOutline() {
    final ElwhaCard card = ElwhaCard.outlinedCard();
    card.add(media());
    final Insets reserve = card.getInsets();

    final BufferedImage image = Pixels.render(card, WIDTH, HEIGHT, Color.MAGENTA);

    Pixels.assertPixelNear(
        image,
        WIDTH / 2,
        reserve.top + 6,
        MEDIA_INK,
        "painting the outline last must not scrub the media out from under it");
  }
}
