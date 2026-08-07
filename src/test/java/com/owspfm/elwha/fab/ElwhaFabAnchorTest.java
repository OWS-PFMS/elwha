package com.owspfm.elwha.fab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of {@link ElwhaFabAnchor} — the placement math the FAB design doc §15 recipe
 * describes, exercised headless in a plain component hierarchy.
 *
 * <p>The contract the anchor exists to fix is that the inset is measured to the FAB's <em>visible
 * body</em>, not to its halo-inclusive bounds, so every placement assertion here works on the body
 * rect recovered by backing {@code getShadowInsets()} out of the FAB's bounds.
 *
 * <p>Scroll behavior is driven by writing directly to the source scrollbar's {@code
 * BoundedRangeModel} — the same signal a real scroll produces, with no wall clock involved; reduced
 * motion (pinned by the fixture) settles the slide and the morph on the spot.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaFabAnchorTest {

  private static final int WIDTH = 800;
  private static final int HEIGHT = 600;

  private static ElwhaFabAnchor anchor(final ElwhaFab fab) {
    final ElwhaFabAnchor anchor = new ElwhaFabAnchor(new JPanel(), fab);
    anchor.setSize(WIDTH, HEIGHT);
    anchor.doLayout();
    return anchor;
  }

  private static ElwhaFabAnchor anchor() {
    return anchor(ElwhaFab.standard(MaterialIcons.add()));
  }

  private static ElwhaFabAnchor relaidOut(final ElwhaFabAnchor anchor) {
    anchor.doLayout();
    return anchor;
  }

  /** The FAB's visible round-rect, recovered by backing the halo reserve out of its bounds. */
  private static Rectangle bodyRect(final ElwhaFabAnchor anchor) {
    final Rectangle bounds = anchor.getFab().getBounds();
    final Insets halo = anchor.getFab().getShadowInsets();
    return new Rectangle(
        bounds.x + halo.left,
        bounds.y + halo.top,
        bounds.width - halo.left - halo.right,
        bounds.height - halo.top - halo.bottom);
  }

  // ------------------------------------------------------------ composition

  @Test
  void bothConstructorArgumentsAreRequired() {
    assertThatThrownBy(() -> new ElwhaFabAnchor(null, ElwhaFab.standard(MaterialIcons.add())))
        .as("a null content component is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ElwhaFabAnchor(new JPanel(), null))
        .as("and so is a null FAB")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void fabFloatsAboveTheContentOnItsOwnLayer() {
    final ElwhaFabAnchor anchor = anchor();

    assertThat(anchor.getLayer(anchor.getFab()))
        .as("the FAB sits on the palette layer of the anchor's private pane")
        .isEqualTo(JLayeredPane.PALETTE_LAYER.intValue());
    assertThat(anchor.getLayer((java.awt.Component) anchor.getContent()))
        .as("while the content stays on the default layer beneath it")
        .isEqualTo(JLayeredPane.DEFAULT_LAYER.intValue());
  }

  @Test
  void contentFillsTheWholeAnchor() {
    final ElwhaFabAnchor anchor = anchor();

    assertThat(anchor.getContent().getBounds())
        .as("the anchor is a drop-in wrapper — the content occupies all of it")
        .isEqualTo(new Rectangle(0, 0, WIDTH, HEIGHT));
  }

  @Test
  void anchorReportsItsContentsPreferredSize() {
    final JPanel content = new JPanel();
    content.setPreferredSize(new Dimension(321, 123));
    final ElwhaFabAnchor anchor =
        new ElwhaFabAnchor(content, ElwhaFab.standard(MaterialIcons.add()));

    assertThat(anchor.getPreferredSize())
        .as("the floating FAB never inflates the layout the anchor asks its parent for")
        .isEqualTo(new Dimension(321, 123));
    assertThat(anchor.getMinimumSize())
        .as("and the minimum follows it")
        .isEqualTo(anchor.getPreferredSize());
  }

  @Test
  void anchorRemembersWhatItWasBuiltWith() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());
    final JPanel content = new JPanel();

    final ElwhaFabAnchor anchor = new ElwhaFabAnchor(content, fab);

    assertThat(anchor.getFab()).as("the FAB is queryable").isSameAs(fab);
    assertThat(anchor.getContent()).as("and so is the content").isSameAs(content);
  }

  // -------------------------------------------------------------- defaults

  @Test
  void anchorDefaults() {
    final ElwhaFabAnchor anchor = anchor();

    assertThat(anchor.getCorner())
        .as("bottom-trailing is the M3 default corner")
        .isEqualTo(ElwhaFabAnchor.Corner.BOTTOM_TRAILING);
    assertThat(anchor.getInsetDp())
        .as("with the M3 placement-diagram inset")
        .isEqualTo(ElwhaFabAnchor.DEFAULT_INSET_DP);
    assertThat(anchor.getScrollResponse())
        .as("and pure static placement until a consumer opts in")
        .isEqualTo(ElwhaFabAnchor.ScrollResponse.NONE);
  }

  // ------------------------------------------------------------- placement

  @ParameterizedTest
  @CsvSource({
    "BOTTOM_TRAILING,true,right,bottom",
    "BOTTOM_TRAILING,false,left,bottom",
    "BOTTOM_LEADING,true,left,bottom",
    "BOTTOM_LEADING,false,right,bottom",
    "TOP_TRAILING,true,right,top",
    "TOP_TRAILING,false,left,top",
    "TOP_LEADING,true,left,top",
    "TOP_LEADING,false,right,top"
  })
  void everyCornerPinsTheVisibleBodyToTheSpecMargin(
      final ElwhaFabAnchor.Corner corner,
      final boolean leftToRight,
      final String horizontalEdge,
      final String verticalEdge) {
    final ElwhaFabAnchor anchor = anchor();
    anchor.applyComponentOrientation(
        leftToRight ? ComponentOrientation.LEFT_TO_RIGHT : ComponentOrientation.RIGHT_TO_LEFT);
    anchor.setCorner(corner);
    relaidOut(anchor);

    final Rectangle body = bodyRect(anchor);
    final int inset = anchor.getInsetDp();
    final int horizontalMargin =
        "left".equals(horizontalEdge) ? body.x : WIDTH - (body.x + body.width);
    final int verticalMargin =
        "top".equals(verticalEdge) ? body.y : HEIGHT - (body.y + body.height);

    assertThat(horizontalMargin)
        .as(
            "%s (ltr=%s) puts the body %d dp from the %s edge",
            corner, leftToRight, inset, horizontalEdge)
        .isEqualTo(inset);
    assertThat(verticalMargin)
        .as(
            "%s (ltr=%s) puts the body %d dp from the %s edge",
            corner, leftToRight, inset, verticalEdge)
        .isEqualTo(inset);
  }

  @Test
  void haloIsBackedOutRatherThanCountedAsMargin() {
    final ElwhaFabAnchor anchor = anchor();
    final Insets halo = anchor.getFab().getShadowInsets();

    final Rectangle bounds = anchor.getFab().getBounds();

    assertThat(halo.right).as("the fixture depends on a non-empty reserve").isPositive();
    assertThat(WIDTH - (bounds.x + bounds.width))
        .as(
            "the naive recipe would land the halo-inclusive bounds on the margin — this one does"
                + " not, so the bounds sit closer to the edge than the inset by exactly the halo")
        .isEqualTo(anchor.getInsetDp() - halo.right);
    assertThat(WIDTH - (bodyRect(anchor).x + bodyRect(anchor).width))
        .as("while the visible body lands exactly on it")
        .isEqualTo(anchor.getInsetDp());
  }

  @Test
  void aCustomInsetMovesTheBodyNotTheBounds() {
    final ElwhaFabAnchor anchor = anchor();

    anchor.setInsetDp(40);
    relaidOut(anchor);

    final Rectangle body = bodyRect(anchor);
    assertThat(WIDTH - (body.x + body.width))
        .as("the horizontal margin follows the setter")
        .isEqualTo(40);
    assertThat(HEIGHT - (body.y + body.height)).as("and so does the vertical one").isEqualTo(40);
  }

  @Test
  void aZeroInsetFlushesTheBodyAgainstTheCorner() {
    final ElwhaFabAnchor anchor = anchor();

    anchor.setInsetDp(0);
    relaidOut(anchor);

    final Rectangle body = bodyRect(anchor);
    assertThat(body.x + body.width).as("a zero inset is legal and lands flush").isEqualTo(WIDTH);
    assertThat(body.y + body.height).as("on both axes").isEqualTo(HEIGHT);
  }

  @Test
  void fabIsSizedFromItsOwnPreferredSizeAndNeverStretched() {
    final ElwhaFabAnchor anchor = anchor(ElwhaFab.extended(MaterialIcons.add(), "Compose"));

    assertThat(anchor.getFab().getSize())
        .as("the anchor positions, never sizes — size-per-window-class stays the consumer's call")
        .isEqualTo(anchor.getFab().getPreferredSize());
  }

  @Test
  void resizingTheAnchorRepinsTheFabWithNoConsumerListener() {
    final ElwhaFabAnchor anchor = anchor();

    anchor.setSize(WIDTH * 2, HEIGHT / 2);
    anchor.doLayout();

    final Rectangle body = bodyRect(anchor);
    assertThat(WIDTH * 2 - (body.x + body.width))
        .as("the opinionated doLayout re-pins on every resize")
        .isEqualTo(anchor.getInsetDp());
    assertThat(HEIGHT / 2 - (body.y + body.height))
        .as("on both axes")
        .isEqualTo(anchor.getInsetDp());
  }

  @Test
  void nullPlacementArgumentsAreRejected() {
    final ElwhaFabAnchor anchor = anchor();

    assertThatThrownBy(() -> anchor.setCorner(null))
        .as("there is no null corner")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> anchor.setScrollResponse(null))
        .as("nor a null scroll response")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> anchor.setInsetDp(-1))
        .as("and a negative inset is meaningless")
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ----------------------------------------------------------- scroll HIDE

  /**
   * A scroll pane laid out over a tall view, so its vertical model carries a real range. Writing a
   * range straight onto the scrollbar is not enough: the scroll pane's own plumbing re-syncs the
   * model from the viewport on the next change and clamps the write away.
   */
  private static JScrollPane scrollable() {
    final JPanel view = new JPanel();
    view.setPreferredSize(new Dimension(400, 5000));
    final JScrollPane pane = new JScrollPane(view);
    pane.setSize(400, 300);
    pane.doLayout();
    pane.getViewport().doLayout();
    return pane;
  }

  private static ElwhaFabAnchor scrollAnchor(
      final JScrollPane pane, final ElwhaFab fab, final ElwhaFabAnchor.ScrollResponse response) {
    final ElwhaFabAnchor anchor = new ElwhaFabAnchor(pane, fab);
    anchor.setSize(WIDTH, HEIGHT);
    anchor.setScrollResponse(response);
    anchor.doLayout();
    return anchor;
  }

  @Test
  void aScrollPaneContentBecomesTheScrollSourceAutomatically() {
    final JScrollPane pane = scrollable();

    final ElwhaFabAnchor anchor = new ElwhaFabAnchor(pane, ElwhaFab.standard(MaterialIcons.add()));

    assertThat(anchor.getScrollSource())
        .as("the common case needs no wiring at all")
        .isSameAs(pane);
  }

  @Test
  void aNonScrollingContentLeavesTheSourceUnset() {
    assertThat(anchor().getScrollSource())
        .as("a plain content component supplies no scroll signal")
        .isNull();
  }

  @Test
  void scrollingDownSlidesTheFabOffItsAnchoredEdge() {
    final JScrollPane pane = scrollable();
    final ElwhaFabAnchor anchor =
        scrollAnchor(
            pane, ElwhaFab.standard(MaterialIcons.add()), ElwhaFabAnchor.ScrollResponse.HIDE);
    final int restingY = anchor.getFab().getY();

    pane.getVerticalScrollBar().setValue(200);
    relaidOut(anchor);

    assertThat(anchor.getFab().getY())
        .as("G14b — scrolling down slides a bottom-anchored FAB down and out of view")
        .isGreaterThan(restingY);
    assertThat(anchor.getFab().getY())
        .as("far enough that its whole body clears the bottom edge")
        .isGreaterThanOrEqualTo(HEIGHT);
  }

  @Test
  void scrollingBackUpSlidesTheFabHome() {
    final JScrollPane pane = scrollable();
    final ElwhaFabAnchor anchor =
        scrollAnchor(
            pane, ElwhaFab.standard(MaterialIcons.add()), ElwhaFabAnchor.ScrollResponse.HIDE);
    final int restingY = anchor.getFab().getY();
    pane.getVerticalScrollBar().setValue(200);

    pane.getVerticalScrollBar().setValue(0);
    relaidOut(anchor);

    assertThat(anchor.getFab().getY())
        .as("scroll-up brings it back to its anchored margin")
        .isEqualTo(restingY);
  }

  @Test
  void aTopAnchoredFabSlidesOffTheOtherWay() {
    final JScrollPane pane = scrollable();
    final ElwhaFabAnchor anchor =
        scrollAnchor(
            pane, ElwhaFab.standard(MaterialIcons.add()), ElwhaFabAnchor.ScrollResponse.HIDE);
    anchor.setCorner(ElwhaFabAnchor.Corner.TOP_TRAILING);
    relaidOut(anchor);
    final int restingY = anchor.getFab().getY();

    pane.getVerticalScrollBar().setValue(200);
    relaidOut(anchor);

    assertThat(anchor.getFab().getY())
        .as("the slide always leaves by the edge the FAB is anchored to")
        .isLessThan(restingY);
  }

  @Test
  void aSubThresholdScrollIsTreatedAsJitter() {
    final JScrollPane pane = scrollable();
    final ElwhaFabAnchor anchor =
        scrollAnchor(
            pane, ElwhaFab.standard(MaterialIcons.add()), ElwhaFabAnchor.ScrollResponse.HIDE);
    final int restingY = anchor.getFab().getY();

    pane.getVerticalScrollBar().setValue(1);
    relaidOut(anchor);

    assertThat(anchor.getFab().getY())
        .as("a one-pixel wobble must not flip the FAB in and out")
        .isEqualTo(restingY);
  }

  @Test
  void defaultResponseIgnoresScrollingEntirely() {
    final JScrollPane pane = scrollable();
    final ElwhaFabAnchor anchor = new ElwhaFabAnchor(pane, ElwhaFab.standard(MaterialIcons.add()));
    anchor.setSize(WIDTH, HEIGHT);
    anchor.doLayout();
    final int restingY = anchor.getFab().getY();

    pane.getVerticalScrollBar().setValue(400);
    relaidOut(anchor);

    assertThat(anchor.getFab().getY()).as("NONE is pure static placement").isEqualTo(restingY);
  }

  // --------------------------------------------------------- scroll SHRINK

  @Test
  void scrollingDownShrinksAnExtendedFabToStandard() {
    final JScrollPane pane = scrollable();
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    scrollAnchor(pane, fab, ElwhaFabAnchor.ScrollResponse.SHRINK);

    pane.getVerticalScrollBar().setValue(200);

    assertThat(fab.getForm())
        .as("G33 — scroll-down collapses the Extended FAB into the Standard one")
        .isEqualTo(ElwhaFab.Form.STANDARD);
  }

  @Test
  void scrollingBackUpRestoresTheExtendedForm() {
    final JScrollPane pane = scrollable();
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    scrollAnchor(pane, fab, ElwhaFabAnchor.ScrollResponse.SHRINK);
    pane.getVerticalScrollBar().setValue(200);

    pane.getVerticalScrollBar().setValue(0);

    assertThat(fab.getForm())
        .as("G34 — scroll-up expands it again")
        .isEqualTo(ElwhaFab.Form.EXTENDED);
  }

  @Test
  void aShrunkFabIsRepinnedAtItsNewNarrowerWidth() {
    final JScrollPane pane = scrollable();
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final ElwhaFabAnchor anchor = scrollAnchor(pane, fab, ElwhaFabAnchor.ScrollResponse.SHRINK);

    pane.getVerticalScrollBar().setValue(200);
    relaidOut(anchor);

    assertThat(WIDTH - (bodyRect(anchor).x + bodyRect(anchor).width))
        .as("the margin survives the morph — a narrower body still hugs the same edge")
        .isEqualTo(anchor.getInsetDp());
  }

  @Test
  void shrinkNeedsAFabThatCarriesBothMorphEndpoints() {
    final JScrollPane pane = scrollable();
    scrollAnchor(pane, ElwhaFab.extended("Compose"), ElwhaFabAnchor.ScrollResponse.SHRINK);

    assertThatThrownBy(() -> pane.getVerticalScrollBar().setValue(200))
        .as(
            "a label-only FAB has no Standard endpoint to shrink into, and the first scroll-down"
                + " says so rather than silently doing nothing")
        .isInstanceOf(IllegalStateException.class);
  }

  // ------------------------------------------------------- response switch

  @ParameterizedTest
  @EnumSource(ElwhaFabAnchor.ScrollResponse.class)
  void scrollResponseRoundTrips(final ElwhaFabAnchor.ScrollResponse response) {
    final ElwhaFabAnchor anchor =
        scrollAnchor(scrollable(), ElwhaFab.extended(MaterialIcons.add(), "Compose"), response);

    assertThat(anchor.getScrollResponse()).as("%s round-trips", response).isEqualTo(response);
  }

  @Test
  void switchingResponseRestoresTheShownBaselineFirst() {
    final JScrollPane pane = scrollable();
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final ElwhaFabAnchor anchor = scrollAnchor(pane, fab, ElwhaFabAnchor.ScrollResponse.HIDE);
    final int restingY = anchor.getFab().getY();
    pane.getVerticalScrollBar().setValue(200);

    anchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.NONE);
    relaidOut(anchor);

    assertThat(anchor.getFab().getY())
        .as("a hidden FAB is brought home before the response changes, never left off-screen")
        .isEqualTo(restingY);
  }

  @Test
  void switchingAwayFromShrinkRestoresTheExtendedForm() {
    final JScrollPane pane = scrollable();
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final ElwhaFabAnchor anchor = scrollAnchor(pane, fab, ElwhaFabAnchor.ScrollResponse.SHRINK);
    pane.getVerticalScrollBar().setValue(200);

    anchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.NONE);

    assertThat(fab.getForm())
        .as("a shrunk FAB is expanded back before the response changes")
        .isEqualTo(ElwhaFab.Form.EXTENDED);
  }

  @Test
  void clearingTheScrollSourceMakesTheResponseInert() {
    final JScrollPane pane = scrollable();
    final ElwhaFabAnchor anchor =
        scrollAnchor(
            pane, ElwhaFab.standard(MaterialIcons.add()), ElwhaFabAnchor.ScrollResponse.HIDE);
    final int restingY = anchor.getFab().getY();

    anchor.setScrollSource(null);
    pane.getVerticalScrollBar().setValue(400);
    relaidOut(anchor);

    assertThat(anchor.getScrollSource()).as("the source is cleared").isNull();
    assertThat(anchor.getFab().getY())
        .as("and the old source no longer drives the FAB")
        .isEqualTo(restingY);
  }
}
