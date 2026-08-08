package com.owspfm.elwha.fab;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintPass;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ContentMorphPainter;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShadowPainter;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A geometry coverage for {@link ElwhaFab} — the §4 size table, the §4.3 dynamic Extended
 * width and its 80 dp floor, the {@code ShadowBearing} halo reserve, the §11 RTL mirroring, and the
 * §9.1 Standard ↔ Extended morph.
 *
 * <p>Morph endpoints are read from the component under pinned reduced motion (every {@code
 * MorphAnimator} snaps, so no frame is sampled from the wall clock); the in-between frames are
 * asserted by feeding injected progress values into the stateless {@link ContentMorphPainter} the
 * FAB composes, seeded with the FAB's own measured endpoint widths.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaFabGeometryTest {

  /** M3's "Dynamic, min 80" annotation on the Extended placement diagram (design doc §4.3). */
  private static final int EXTENDED_MIN_WIDTH = 80;

  /** An icon that records the rectangle it was asked to paint into. */
  private static final class RecordingIcon implements Icon {
    private final int side;
    private Rectangle painted;

    RecordingIcon(final int side) {
      this.side = side;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      painted = new Rectangle(x, y, side, side);
    }

    @Override
    public int getIconWidth() {
      return side;
    }

    @Override
    public int getIconHeight() {
      return side;
    }
  }

  private static int bodyWidth(final ElwhaFab fab) {
    final Insets reserve = fab.getShadowInsets();
    return fab.getPreferredSize().width - reserve.left - reserve.right;
  }

  private static int bodyHeight(final ElwhaFab fab) {
    final Insets reserve = fab.getShadowInsets();
    return fab.getPreferredSize().height - reserve.top - reserve.bottom;
  }

  private static void render(final ElwhaFab fab) {
    final Dimension pref = fab.getPreferredSize();
    Pixels.render(fab, pref.width, pref.height);
  }

  // ---------------------------------------------------------- size table

  @ParameterizedTest
  @EnumSource(ElwhaFab.Size.class)
  void aStandardFabIsASquareOfItsTiersContainerSide(final ElwhaFab.Size size) {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add()).setFabSize(size);

    assertThat(bodyWidth(fab)).as("%s is square on the width", size).isEqualTo(size.containerPx());
    assertThat(bodyHeight(fab))
        .as("%s is square on the height", size)
        .isEqualTo(size.containerPx());
  }

  @ParameterizedTest
  @EnumSource(ElwhaFab.Size.class)
  void anExtendedFabReusesTheStandardContainerHeight(final ElwhaFab.Size size) {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose").setFabSize(size);

    assertThat(bodyHeight(fab))
        .as("§4 — the May-2025 alignment rule gives both forms one height per tier (%s)", size)
        .isEqualTo(size.containerPx());
  }

  @ParameterizedTest
  @EnumSource(ElwhaFab.Size.class)
  void anExtendedFabSpendsItsTiersLeadingGapAndTrailingInsets(final ElwhaFab.Size size) {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose").setFabSize(size);
    final FontMetrics metrics = fab.getFontMetrics(size.labelTypeRole().resolve());

    assertThat(bodyWidth(fab))
        .as("§4.2/§4.3 — %s hugs leading + icon + gap + label + trailing", size)
        .isEqualTo(
            Math.max(
                EXTENDED_MIN_WIDTH,
                size.extendedLeadingPx()
                    + size.iconPx()
                    + size.extendedIconGapPx()
                    + metrics.stringWidth("Compose")
                    + size.extendedTrailingPx()));
  }

  @Test
  void aLabelOnlyExtendedFabSpendsNoIconSlot() {
    final ElwhaFab fab = ElwhaFab.extended("Compose");
    final ElwhaFab.Size size = fab.getFabSize();
    final FontMetrics metrics = fab.getFontMetrics(size.labelTypeRole().resolve());

    assertThat(bodyWidth(fab))
        .as("with no icon the gap disappears along with the glyph")
        .isEqualTo(
            Math.max(
                EXTENDED_MIN_WIDTH,
                size.extendedLeadingPx()
                    + metrics.stringWidth("Compose")
                    + size.extendedTrailingPx()));
  }

  @Test
  void aVeryShortExtendedLabelStillClearsTheEightyPixelFloor() {
    final ElwhaFab fab = ElwhaFab.extended("");

    assertThat(bodyWidth(fab))
        .as("§4.3 — the M3 dynamic-width floor binds for the smallest size and shortest label")
        .isEqualTo(EXTENDED_MIN_WIDTH);
  }

  @ParameterizedTest
  @EnumSource(ElwhaFab.Size.class)
  void aScalableGlyphIsRederivedToTheTiersIconSize(final ElwhaFab.Size size) {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add()).setFabSize(size);

    assertThat(fab.paintedIcon().getIconWidth())
        .as("%s paints its glyph at the size the layout reserves", size)
        .isEqualTo(size.iconPx());
  }

  @Test
  void aNullSizeOrColorIsIgnoredRatherThanApplied() {
    final ElwhaFab fab =
        ElwhaFab.standard(MaterialIcons.add())
            .setFabSize(ElwhaFab.Size.LARGE)
            .setColor(ElwhaFab.Color.TERTIARY);

    fab.setFabSize(null).setColor(null);

    assertThat(fab.getFabSize()).as("a null size is ignored").isEqualTo(ElwhaFab.Size.LARGE);
    assertThat(fab.getColor()).as("a null color is ignored").isEqualTo(ElwhaFab.Color.TERTIARY);
  }

  // ------------------------------------------------------- shadow reserve

  @ParameterizedTest
  @EnumSource(ElwhaFab.Size.class)
  void haloReserveIsSizedForTheHoverElevationAtEveryTier(final ElwhaFab.Size size) {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add()).setFabSize(size);

    assertThat(fab.getShadowInsets())
        .as("%s reserves for the worst case it can paint — the level-4 hover bump", size)
        .isEqualTo(ShadowPainter.shadowInsets(4));
  }

  @Test
  void reserveDoesNotShrinkWhenTheFabIsDisabledOrAtRest() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());
    final Insets resting = fab.getShadowInsets();

    fab.setEnabled(false);

    assertThat(fab.getShadowInsets())
        .as("the reserve is constant, so a placement helper never has to re-measure it")
        .isEqualTo(resting);
  }

  @Test
  void haloReserveIsHandedOutAsACopy() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());

    fab.getShadowInsets().top = 999;

    assertThat(fab.getShadowInsets().top)
        .as("a caller mutating the returned insets cannot reach the FAB's reserve")
        .isEqualTo(ShadowPainter.shadowInsets(4).top);
  }

  @Test
  void reserveIsBakedIntoPreferredSizeOnEveryEdge() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add()).setFabSize(ElwhaFab.Size.LARGE);
    final Insets halo = fab.getShadowInsets();

    assertThat(fab.getPreferredSize())
        .as("the body is padded by the halo so the shadow never clips against the bounds")
        .isEqualTo(
            new Dimension(
                ElwhaFab.Size.LARGE.containerPx() + halo.left + halo.right,
                ElwhaFab.Size.LARGE.containerPx() + halo.top + halo.bottom));
    assertThat(fab.getMinimumSize())
        .as("and a FAB never lays out below that")
        .isEqualTo(fab.getPreferredSize());
  }

  @Test
  void maximumSizeIsDeliberatelyNotPinnedToThePreferredSize() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add());

    assertThat(fab.getMaximumSize().height)
        .as(
            "#199 — pinning the maximum to a halo-inclusive preferred size is what broke the"
                + " shadow before; the FAB must leave its maximum unbounded")
        .isGreaterThan(fab.getPreferredSize().height);
  }

  // ---------------------------------------------------------- RTL mirroring

  @Test
  void aStandardFabCentersItsGlyphOnBothAxes() {
    final RecordingIcon icon = new RecordingIcon(20);
    final ElwhaFab fab = ElwhaFab.standard(icon);

    render(fab);

    assertThat(icon.painted.x)
        .as("§4.1 — the Standard form centers the icon horizontally")
        .isEqualTo((bodyWidth(fab) - 20) / 2);
    assertThat(icon.painted.y)
        .as("and puts it on the vertical midline")
        .isEqualTo((bodyHeight(fab) - 20) / 2);
  }

  @Test
  void anExtendedFabLeadsWithItsGlyphAndMirrorsUnderRightToLeft() {
    final RecordingIcon leftToRight = new RecordingIcon(20);
    final ElwhaFab ltr = ElwhaFab.extended(leftToRight, "Compose");
    render(ltr);

    final RecordingIcon rightToLeft = new RecordingIcon(20);
    final ElwhaFab rtl = ElwhaFab.extended(rightToLeft, "Compose");
    rtl.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    render(rtl);

    final int body = bodyWidth(ltr);
    assertThat(leftToRight.painted.x)
        .as("§4.2 — the icon leads in the reading direction")
        .isLessThan(body / 2);
    assertThat(rightToLeft.painted.x)
        .as("§11 — and mirrors to the trailing side under RTL")
        .isGreaterThan(body / 2);
    assertThat(rightToLeft.painted.x + 20)
        .as("the two positions are exact mirrors about the body's center line")
        .isEqualTo(body - leftToRight.painted.x);
  }

  @Test
  void mirroringLeavesTheVerticalPlacementAlone() {
    final RecordingIcon icon = new RecordingIcon(20);
    final ElwhaFab fab = ElwhaFab.extended(icon, "Compose");
    fab.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    render(fab);

    assertThat(icon.painted.y)
        .as("§11 — RTL flips the X anchor only; the icon stays on the vertical midline")
        .isEqualTo((bodyHeight(fab) - 20) / 2);
  }

  // ---------------------------------------------------------------- morph

  @Test
  void aFabBuiltForBothEndpointsMorphsAllTheWayInEitherDirection() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final int extended = bodyWidth(fab);
    assertThat(fab.getForm())
        .as("it starts in the form its factory named")
        .isEqualTo(ElwhaFab.Form.EXTENDED);

    fab.morphTo(ElwhaFab.Form.STANDARD);

    assertThat(fab.getForm())
        .as("the live form follows the destination")
        .isEqualTo(ElwhaFab.Form.STANDARD);
    assertThat(bodyWidth(fab))
        .as("and the body collapses to the square container")
        .isEqualTo(fab.getFabSize().containerPx());

    fab.morphTo(ElwhaFab.Form.EXTENDED);

    assertThat(bodyWidth(fab))
        .as("the return trip restores the content-hugging width")
        .isEqualTo(extended);
  }

  @Test
  void underReducedMotionAMorphIsAlreadySettledWhenItReturns() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    assertThat(MorphAnimator.isReducedMotion()).as("the fixture pins reduced motion").isTrue();

    fab.morphTo(ElwhaFab.Form.STANDARD);

    assertThat(fab.isMorphing())
        .as("so no consumer polling isMorphing can ever race a Timer in this suite")
        .isFalse();
  }

  @Test
  void aMorphingFabSchedulesNoRelayoutFromInsideItsPaintPass() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    // Parented deliberately: JComponent.revalidate() returns early on an orphan, so an unparented
    // FAB would report zero whether or not the paint asked for one.
    new JPanel(null).add(fab);
    MorphAnimator.setReducedMotion(false);
    fab.morphTo(ElwhaFab.Form.STANDARD);
    // Re-pinned before painting: the morph is live (a Swing timer cannot tick while this test
    // holds the dispatch thread) and the restore keeps the rest of the test snapping as usual.
    MorphAnimator.setReducedMotion(true);
    assertThat(fab.isMorphing()).as("the morph really is running").isTrue();

    final PaintPass pass = PaintPass.capture(fab);

    assertThat(pass.revalidations())
        .as("#656 — revalidating from paint re-enters the invalidation queue from the paint pass")
        .isZero();
  }

  @Test
  void aMorphTickIsWhatDrivesTheRelayout() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final JPanel parent = new JPanel(null);
    parent.add(fab);
    final PaintPass idle = PaintPass.capture(fab);

    final PaintPass duringTick = tickCounted(fab);

    assertThat(idle.revalidations()).as("a settled FAB asks for nothing").isZero();
    assertThat(duringTick.revalidations())
        .as("the animator's own progress tick is where the parent learns the body width changed")
        .isPositive();
  }

  /** Counts what a morph tick schedules, with the FAB parented so revalidate() is not short-cut. */
  private static PaintPass tickCounted(final ElwhaFab fab) {
    return PaintPass.captureDuring(fab, () -> fab.morphTo(ElwhaFab.Form.STANDARD));
  }

  @Test
  void morphingToTheCurrentFormIsANoOp() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final int before = bodyWidth(fab);

    assertThat(fab.morphTo(ElwhaFab.Form.EXTENDED)).as("morphTo chains").isSameAs(fab);
    assertThat(bodyWidth(fab))
        .as("and re-targeting the current form changes nothing")
        .isEqualTo(before);
  }

  @Test
  void widthCurveRunsMonotonicallyBetweenTheTwoMeasuredEndpoints() {
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final int extended = bodyWidth(fab);
    fab.morphTo(ElwhaFab.Form.STANDARD);
    final int standard = bodyWidth(fab);

    // §9.1 — the FAB drives its container width through this stateless helper on an eased
    // progress. Feeding injected progress values reproduces every intermediate frame without
    // sampling one from a running animation.
    int previous = Integer.MIN_VALUE;
    for (final float progress : new float[] {0f, 0.25f, 0.5f, 0.75f, 1f}) {
      final int width =
          ContentMorphPainter.containerWidth(standard, extended, Easing.EMPHASIZED.ease(progress));
      assertThat(width)
          .as("the container width never moves backwards through the morph (progress %s)", progress)
          .isGreaterThanOrEqualTo(previous);
      previous = width;
    }
    assertThat(ContentMorphPainter.containerWidth(standard, extended, 0f))
        .as("progress 0 paints exactly the Standard width the component reports")
        .isEqualTo(standard);
    assertThat(ContentMorphPainter.containerWidth(standard, extended, 1f))
        .as("and progress 1 paints exactly the Extended width")
        .isEqualTo(extended);
  }

  @Test
  void labelHoldsItsFadeUntilTheBodyHasRoomForIt() {
    // §9.1 — the container-emphasized cross-fade: the body expands through the first half with no
    // visible label, then the label fades in through the second half.
    assertThat(ContentMorphPainter.labelAlpha(0f)).as("no label at the Standard end").isZero();
    assertThat(ContentMorphPainter.labelAlpha(0.5f))
        .as("still nothing at the cross-fade inflection")
        .isZero();
    assertThat(ContentMorphPainter.labelAlpha(0.75f))
        .as("half faded in a quarter past it")
        .isEqualTo(0.5f);
    assertThat(ContentMorphPainter.labelAlpha(1f))
        .as("fully opaque at the Extended end")
        .isEqualTo(1f);
  }
}
