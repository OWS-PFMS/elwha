package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.list.ElwhaListItemView;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShadowBearing;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of {@link ElwhaCard}'s chrome — the §8 variant table (container role, outline,
 * resting elevation), the always-on shadow reserve that keeps every variant's painted body the same
 * size, and the §9 transient elevation lifts.
 *
 * <p>Every fill probe renders onto a deliberately contrasting ground. Three of the four roles in
 * play here — {@code SURFACE}, {@code SURFACE_CONTAINER_LOW}, {@code SURFACE_CONTAINER_LOWEST} —
 * sit within the ±10/channel probe tolerance of each other in the baseline palette, so a probe on
 * the default surface ground would pass even against a card that painted nothing at all.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardRenderTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 140;

  /** A ground no card fill resolves to in either mode, so "the card painted" is provable. */
  private static final Color CONTRAST_GROUND = Color.MAGENTA;

  private static BufferedImage render(final ElwhaCard card) {
    return Pixels.render(card, WIDTH, HEIGHT, CONTRAST_GROUND);
  }

  // ----------------------------------------------------------- construction

  @Test
  void freshCardCarriesTheDocumentedDefaults() {
    final ElwhaCard card = new ElwhaCard();

    assertThat(card.getVariant()).as("a bare card is ELEVATED").isEqualTo(CardVariant.ELEVATED);
    assertThat(card.getShape())
        .as("cards default to the 12 dp M3 card corner")
        .isEqualTo(ShapeScale.MD);
    assertThat(card.getPadding())
        .as("both axes default to the 16 dp M3 card padding")
        .isEqualTo(
            new Insets(
                SpaceScale.LG.px(), SpaceScale.LG.px(), SpaceScale.LG.px(), SpaceScale.LG.px()));
    assertThat(card.getExpansionOverflow())
        .as("an expanded card grows by default rather than scrolling internally")
        .isEqualTo(ExpansionOverflow.GROW);
    assertThat(card.isActionable()).as("a card is not a button until asked").isFalse();
    assertThat(card.isSelectable()).as("a card is not selectable until asked").isFalse();
    assertThat(card.isCollapsible()).as("a card is not collapsible until asked").isFalse();
    assertThat(card.isDragged()).as("a fresh card carries no drag chrome").isFalse();
  }

  @Test
  void cardIsASurfaceAndAListItemView() {
    final ElwhaCard card = new ElwhaCard();

    assertThat(card)
        .as("the card builds on the shared surface chassis rather than reimplementing it")
        .isInstanceOf(ElwhaSurface.class);
    assertThat(card)
        .as("a card can be hosted by the item list with full selection and drag polish")
        .isInstanceOf(ElwhaListItemView.class);
    assertThat(card)
        .as("placement helpers read the card's halo through the shared interface")
        .isInstanceOf(ShadowBearing.class);
  }

  @Test
  void everyFactoryNamesItsVariant() {
    assertThat(ElwhaCard.elevatedCard().getVariant())
        .as("the elevated factory yields an ELEVATED card")
        .isEqualTo(CardVariant.ELEVATED);
    assertThat(ElwhaCard.filledCard().getVariant())
        .as("the filled factory yields a FILLED card")
        .isEqualTo(CardVariant.FILLED);
    assertThat(ElwhaCard.outlinedCard().getVariant())
        .as("the outlined factory yields an OUTLINED card")
        .isEqualTo(CardVariant.OUTLINED);
  }

  @Test
  void variantSetterRejectsNull() {
    assertThatThrownBy(() -> new ElwhaCard().setVariant(null))
        .as("a null variant is rejected rather than silently keeping the old one")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void paddingSetterRejectsNullOnEitherAxis() {
    assertThatThrownBy(() -> new ElwhaCard().setPadding(null, SpaceScale.SM))
        .as("a null horizontal step is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ElwhaCard().setPadding(SpaceScale.SM, null))
        .as("a null vertical step is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void paddingResolvesTheTokenStepsPerAxis() {
    final ElwhaCard card = new ElwhaCard().setPadding(SpaceScale.XS, SpaceScale.XL);

    assertThat(card.getPadding())
        .as("horizontal drives left and right, vertical drives top and bottom")
        .isEqualTo(
            new Insets(
                SpaceScale.XL.px(), SpaceScale.XS.px(), SpaceScale.XL.px(), SpaceScale.XS.px()));
  }

  // --------------------------------------------------------- variant table

  @ParameterizedTest
  @EnumSource(CardVariant.class)
  void eachVariantFillsWithItsOwnContainerRole(final CardVariant variant) {
    final ElwhaCard card = new ElwhaCard().setVariant(variant);

    Pixels.assertPixelNear(
        render(card),
        WIDTH / 2,
        HEIGHT / 2,
        variant.containerRole().resolve(),
        variant + " fills its body with the container role from the variant table");
  }

  @ParameterizedTest
  @EnumSource(CardVariant.class)
  void eachVariantFillsWithItsOwnContainerRoleInDarkMode(final CardVariant variant) {
    ThemeExtension.install(Mode.DARK);
    final ElwhaCard card = new ElwhaCard().setVariant(variant);

    Pixels.assertPixelNear(
        render(card),
        WIDTH / 2,
        HEIGHT / 2,
        variant.containerRole().resolve(),
        variant + " resolves its container role against the dark scheme too");
  }

  @ParameterizedTest
  @EnumSource(CardVariant.class)
  void variantDrivesTheChassisRolesAndBorderWidth(final CardVariant variant) {
    final ElwhaCard card = new ElwhaCard().setVariant(variant);

    assertThat(card.getSurfaceRole())
        .as("%s carries its table container role", variant)
        .isEqualTo(variant.containerRole());
    assertThat(card.getBorderRole())
        .as("%s carries its table outline role", variant)
        .isEqualTo(variant.borderRole());
    assertThat(card.getBorderWidth())
        .as("%s strokes at 1 dp only when the table gives it an outline", variant)
        .isEqualTo(variant.borderRole() != null ? 1 : 0);
  }

  @ParameterizedTest
  @EnumSource(CardVariant.class)
  void variantDrivesTheRestingElevation(final CardVariant variant) {
    assertThat(new ElwhaCard().setVariant(variant).getElevation())
        .as("%s rests at the §9 elevation for its row", variant)
        .isEqualTo(ElwhaCard.defaultElevationFor(variant));
  }

  @Test
  void onlyTheElevatedVariantRestsLifted() {
    assertThat(ElwhaCard.defaultElevationFor(CardVariant.ELEVATED))
        .as("Elevated rests at M3 Level 1")
        .isEqualTo(1);
    assertThat(ElwhaCard.defaultElevationFor(CardVariant.FILLED)).as("Filled rests flat").isZero();
    assertThat(ElwhaCard.defaultElevationFor(CardVariant.OUTLINED))
        .as("Outlined rests flat — the outline is its separation cue")
        .isZero();
  }

  @Test
  void switchingVariantReappliesTheWholeRow() {
    final ElwhaCard card = ElwhaCard.outlinedCard();

    card.setVariant(CardVariant.FILLED);

    assertThat(card.getSurfaceRole())
        .as("the new variant's container role takes over")
        .isEqualTo(CardVariant.FILLED.containerRole());
    assertThat(card.getBorderWidth())
        .as("moving to a borderless variant drops the stroke rather than leaving it behind")
        .isZero();
    assertThat(card.getElevation())
        .as("the new variant's resting elevation takes over too")
        .isEqualTo(ElwhaCard.defaultElevationFor(CardVariant.FILLED));
  }

  @Test
  void outlinedCardStrokesItsOutlineAtTheBodyEdge() {
    final ElwhaCard card = ElwhaCard.outlinedCard();
    final Insets reserve = card.getInsets();

    final BufferedImage image = render(card);

    Pixels.assertAnyPixelNear(
        image,
        WIDTH / 2,
        reserve.top,
        3,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "the outline is stroked on the top body edge, just inside the shadow reserve");
  }

  @Test
  void aBorderlessVariantLeavesTheSameEdgeBare() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final Insets reserve = card.getInsets();

    final BufferedImage image = render(card);

    Pixels.assertNoPixelNear(
        image,
        WIDTH / 2,
        reserve.top,
        3,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "Filled carries no outline, so the body edge shows only the container fill");
  }

  // -------------------------------------------------------- shadow reserve

  @ParameterizedTest
  @EnumSource(CardVariant.class)
  void everyVariantReservesTheTopOfTheShadowLadder(final CardVariant variant) {
    assertThat(new ElwhaCard().setVariant(variant).getInsets())
        .as(
            "%s reserves the level-%d halo regardless of where it rests, so a mixed grid of"
                + " cards has one body width",
            variant, ElwhaCard.MAX_ELEVATION)
        .isEqualTo(ShadowPainter.shadowInsets(ElwhaCard.MAX_ELEVATION));
  }

  @Test
  void reserveDoesNotMoveWhenTheRestingElevationDoes() {
    final ElwhaCard card = new ElwhaCard();
    final Insets atRest = card.getInsets();

    card.setElevation(0);
    final Insets flattened = card.getInsets();
    card.setElevation(ElwhaCard.MAX_ELEVATION);

    assertThat(flattened)
        .as("dropping a card flat must not shrink its body — sibling widths would jump")
        .isEqualTo(atRest);
    assertThat(card.getInsets())
        .as("lifting it back must not grow the body either")
        .isEqualTo(atRest);
  }

  @Test
  void cardCeilingIsTheSharedPainterCeiling() {
    assertThat(ElwhaCard.MAX_ELEVATION)
        .as("the card and the shared shadow painter must agree on where the ladder stops")
        .isEqualTo(ShadowPainter.MAX_ELEVATION);
  }

  @Test
  void paintedBodySitsInsideTheReserveRatherThanFillingTheBounds() {
    final ElwhaCard card = ElwhaCard.filledCard();
    final Insets reserve = card.getInsets();

    final BufferedImage image = render(card);

    assertThat(reserve.left)
        .as("the reserve is real, so a bounds-anchored consumer is not looking at the body edge")
        .isPositive();
    Pixels.assertPixelNear(
        image,
        0,
        HEIGHT / 2,
        CONTRAST_GROUND,
        "the chassis leaves its left reserve column unpainted — the body starts further in");
    Pixels.assertPixelNear(
        image,
        reserve.left + 2,
        HEIGHT / 2,
        CardVariant.FILLED.containerRole().resolve(),
        "the painted body begins immediately inside the reserved left edge");
    Pixels.assertPixelNear(
        image,
        WIDTH - reserve.right - 3,
        HEIGHT / 2,
        CardVariant.FILLED.containerRole().resolve(),
        "and ends immediately inside the reserved right edge");
  }

  @Test
  void bodyIsCenteredHorizontallyWithinTheStretchedBounds() {
    final Insets reserve = new ElwhaCard().getInsets();

    assertThat(reserve.left)
        .as("the horizontal reserve is symmetric, so the body is centered on the x axis")
        .isEqualTo(reserve.right);
    assertThat(reserve.bottom)
        .as("the vertical reserve is not — M3 shadows fall downward")
        .isGreaterThan(reserve.top);
  }

  // ------------------------------------------------------ transient lifts

  @Test
  void aRestingCardPaintsItsRestingElevation() {
    final ElwhaCard card = ElwhaCard.elevatedCard();

    assertThat(card.currentElevationForPaint())
        .as("with no interaction the painted elevation is the resting one")
        .isEqualTo(ElwhaCard.defaultElevationFor(CardVariant.ELEVATED));
  }

  @ParameterizedTest
  @EnumSource(CardVariant.class)
  void draggingLiftsEveryVariantThreeLevels(final CardVariant variant) {
    final ElwhaCard card = new ElwhaCard().setVariant(variant).setDragged(true);

    assertThat(card.currentElevationForPaint())
        .as("%s lifts three levels above its rest while dragged", variant)
        .isEqualTo(ElwhaCard.defaultElevationFor(variant) + 3);
  }

  @Test
  void liftNeverClimbsPastTheTopOfTheLadder() {
    final ElwhaCard card = ElwhaCard.elevatedCard().setElevation(ElwhaCard.MAX_ELEVATION);

    card.setDragged(true);

    assertThat(card.currentElevationForPaint())
        .as("a card already at the ceiling stays there rather than painting an unsupported level")
        .isEqualTo(ElwhaCard.MAX_ELEVATION);
  }

  @Test
  void draggedStateRoundTripsAndClears() {
    final ElwhaCard card = ElwhaCard.filledCard();

    assertThat(card.setDragged(true).isDragged()).as("the drag flag round-trips").isTrue();
    assertThat(card.setDragged(false).isDragged()).as("and clears again").isFalse();
    assertThat(card.currentElevationForPaint())
        .as("clearing the flag drops the card back to its resting elevation")
        .isEqualTo(ElwhaCard.defaultElevationFor(CardVariant.FILLED));
  }

  // -------------------------------------------------------------- disabled

  @Test
  void disablingAnElevatedCardSwapsItsContainerRole() {
    final ElwhaCard card = ElwhaCard.elevatedCard();
    card.setEnabled(false);

    final BufferedImage image = render(card);

    assertThat(ColorRole.SURFACE.resolve())
        .as("the swap is only observable if the two roles differ — guard the oracle")
        .isNotEqualTo(CardVariant.ELEVATED.containerRole().resolve());
    Pixels.assertAnyPixelNear(
        image,
        WIDTH / 2,
        HEIGHT / 2,
        4,
        ColorRole.SURFACE.resolve(),
        "a disabled Elevated card fills with SURFACE rather than its resting container role");
  }

  @Test
  void disablingAnOutlinedCardKeepsItsContainerRole() {
    final ElwhaCard card = ElwhaCard.outlinedCard();
    card.setEnabled(false);

    Pixels.assertPixelNear(
        render(card),
        WIDTH / 2,
        HEIGHT / 2,
        CardVariant.OUTLINED.containerRole().resolve(),
        "Outlined signals disabled through its faded outline, not a fill change");
  }

  @Test
  void disabledRoleSwapIsScopedToPaint() {
    final ElwhaCard card = ElwhaCard.elevatedCard();

    card.setEnabled(false);

    assertThat(card.getSurfaceRole())
        .as("outside a paint pass the card still reports the role a consumer configured")
        .isEqualTo(CardVariant.ELEVATED.containerRole());
  }

  // ------------------------------------------------------------- sizing

  @Test
  void chassisNeverResistsItsParent() {
    assertThat(new ElwhaCard().getMaximumSize())
        .as("§3.4 rule 1 — any parent layout may stretch or compress the chassis freely")
        .isEqualTo(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
  }

  @Test
  void anEmptyCardStillReservesItsPaddingAndHalo() {
    final ElwhaCard card = new ElwhaCard();
    final Insets reserve = card.getInsets();

    final Dimension preferred = card.getPreferredSize();

    assertThat(preferred.height)
        .as("an empty card is its two padding rows plus the shadow reserve, not zero")
        .isEqualTo(2 * SpaceScale.LG.px() + reserve.top + reserve.bottom);
    assertThat(preferred.width)
        .as("and its two padding columns plus the same reserve")
        .isEqualTo(2 * SpaceScale.LG.px() + reserve.left + reserve.right);
  }
}
