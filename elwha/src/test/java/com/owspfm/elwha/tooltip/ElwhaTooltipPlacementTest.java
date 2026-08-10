package com.owspfm.elwha.tooltip;

import static com.owspfm.elwha.tooltip.ElwhaTooltip.ANCHOR_GAP_PX;
import static com.owspfm.elwha.tooltip.ElwhaTooltip.EDGE_MARGIN_PX;
import static com.owspfm.elwha.tooltip.ElwhaTooltip.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

/**
 * Tier A coverage of the tooltip placement engine against a pinned viewport: which side of the
 * anchor the body lands on and when it flips, how the three alignments resolve (including RTL), the
 * edge-margin clamp, and the halo correction that makes all of the above measure the <em>body</em>
 * rather than the surface's shadow reserve.
 *
 * <p>Every assertion is written against the body rectangle — surface bounds plus the halo offset —
 * because that is the rectangle a user sees and the one the spec's numbers refer to. The surface's
 * own width is bounded upstream by the variant max-width tokens (see {@code
 * ElwhaTooltipSurfaceTest}); the engine positions what it is handed, it does not resize it.
 */
class ElwhaTooltipPlacementTest {

  private static final int PANE_W = 800;
  private static final int PANE_H = 600;
  private static final Insets FLAT = new Insets(0, 0, 0, 0);
  private static final Insets HALO = new Insets(6, 8, 10, 8);
  private static final Dimension BUBBLE = new Dimension(120, 30);

  /** The visible body inside a surface rect — the surface minus its shadow reserve. */
  private static Rectangle body(final Rectangle surface, final Insets halo, final Dimension pref) {
    return new Rectangle(
        surface.x + halo.left,
        surface.y + halo.top,
        pref.width - halo.left - halo.right,
        pref.height - halo.top - halo.bottom);
  }

  // ------------------------------------------------------------------- side

  @Test
  void aTooltipPrefersTheSpaceAboveItsAnchor() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.y + bounds.height)
        .as("the body's bottom edge sits one gap above the anchor's top")
        .isEqualTo(anchor.y - ANCHOR_GAP_PX);
  }

  @Test
  void anAnchorNearTheTopFlipsTheTooltipBelowIt() {
    final Rectangle anchor = new Rectangle(300, 10, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.y)
        .as("no room above, so the tooltip flips to the other side of the anchor")
        .isEqualTo(anchor.y + anchor.height + ANCHOR_GAP_PX);
  }

  @Test
  void askingForBelowKeepsItBelow() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.BELOW,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.y).isEqualTo(anchor.y + anchor.height + ANCHOR_GAP_PX);
  }

  @Test
  void anAnchorNearTheBottomFlipsAPreferredBelowUpwards() {
    final Rectangle anchor = new Rectangle(300, PANE_H - 50, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.BELOW,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.y + bounds.height)
        .as("the preferred side is only a preference — it yields when it would clip")
        .isEqualTo(anchor.y - ANCHOR_GAP_PX);
  }

  @Test
  void aTooltipNeverCrossesTheVerticalEdgeMargin() {
    final Rectangle anchor = new Rectangle(300, 0, 80, PANE_H);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.y)
        .as("an anchor filling the viewport leaves no side free, so the body clamps to the margin")
        .isGreaterThanOrEqualTo(EDGE_MARGIN_PX);
    assertThat(bounds.y + bounds.height).isLessThanOrEqualTo(PANE_H - EDGE_MARGIN_PX);
  }

  // -------------------------------------------------------------- alignment

  @Test
  void centerAlignmentSplitsTheAnchorEvenly() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.x + bounds.width / 2)
        .as("the body's center lines up with the anchor's")
        .isEqualTo(anchor.x + anchor.width / 2);
  }

  @Test
  void startAlignmentIsFlushWithTheAnchorsLeadingEdge() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.START,
            false);

    assertThat(bounds.x).isEqualTo(anchor.x);
  }

  @Test
  void endAlignmentIsFlushWithTheAnchorsTrailingEdge() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.END,
            false);

    assertThat(bounds.x + bounds.width).isEqualTo(anchor.x + anchor.width);
  }

  @Test
  void rightToLeftSwapsWhichEdgeStartAndEndMean() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    final Rectangle start =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.START,
            true);
    final Rectangle end =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.END,
            true);

    assertThat(start.x + start.width)
        .as("under RTL the start edge is the anchor's right edge")
        .isEqualTo(anchor.x + anchor.width);
    assertThat(end.x).as("and the end edge is its left").isEqualTo(anchor.x);
  }

  @Test
  void centerAlignmentIsDirectionAgnostic() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);

    assertThat(
            place(
                anchor,
                BUBBLE,
                FLAT,
                PANE_W,
                PANE_H,
                TooltipPlacement.ABOVE,
                TooltipAlignment.CENTER,
                true))
        .as("there is no leading or trailing edge to mirror when centering")
        .isEqualTo(
            place(
                anchor,
                BUBBLE,
                FLAT,
                PANE_W,
                PANE_H,
                TooltipPlacement.ABOVE,
                TooltipAlignment.CENTER,
                false));
  }

  @Test
  void anAnchorAtTheTrailingEdgeIsPulledBackInsideTheMargin() {
    final Rectangle anchor = new Rectangle(PANE_W - 20, 200, 20, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.x + bounds.width)
        .as("the body stops at the edge margin rather than overhanging")
        .isEqualTo(PANE_W - EDGE_MARGIN_PX);
  }

  @Test
  void anAnchorAtTheLeadingEdgeIsPushedInsideTheMargin() {
    final Rectangle anchor = new Rectangle(0, 200, 20, 40);

    final Rectangle bounds =
        place(
            anchor,
            BUBBLE,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.x).isEqualTo(EDGE_MARGIN_PX);
  }

  @Test
  void aBodyWiderThanTheMarginBandIsPinnedToTheLeadingMargin() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);
    final Dimension huge = new Dimension(PANE_W + 200, 30);

    final Rectangle bounds =
        place(
            anchor,
            huge,
            FLAT,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(bounds.x)
        .as(
            "a body with nowhere to fit is pinned to the leading margin rather than centered"
                + " off-screen")
        .isEqualTo(EDGE_MARGIN_PX);
  }

  // ------------------------------------------------------------------- halo

  @Test
  void aShadowedTooltipStillMeasuresItsBodyAgainstTheAnchor() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);
    final Dimension pref = new Dimension(120 + HALO.left + HALO.right, 30 + HALO.top + HALO.bottom);

    final Rectangle surface =
        place(
            anchor,
            pref,
            HALO,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);
    final Rectangle body = body(surface, HALO, pref);

    assertThat(body.y + body.height)
        .as("the gap is measured from the painted body, not from the shadow reserve around it")
        .isEqualTo(anchor.y - ANCHOR_GAP_PX);
    assertThat(body.x + body.width / 2)
        .as("and so is the horizontal alignment")
        .isEqualTo(anchor.x + anchor.width / 2);
  }

  @Test
  void surfaceRectStillCarriesTheWholePreferredSize() {
    final Rectangle anchor = new Rectangle(300, 200, 80, 40);
    final Dimension pref = new Dimension(120 + HALO.left + HALO.right, 30 + HALO.top + HALO.bottom);

    final Rectangle surface =
        place(
            anchor,
            pref,
            HALO,
            PANE_W,
            PANE_H,
            TooltipPlacement.ABOVE,
            TooltipAlignment.CENTER,
            false);

    assertThat(surface.getSize())
        .as("the mounted surface keeps room for its shadow — only the offsets back it out")
        .isEqualTo(pref);
  }
}
