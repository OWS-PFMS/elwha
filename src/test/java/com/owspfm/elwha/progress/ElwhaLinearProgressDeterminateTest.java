package com.owspfm.elwha.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of the linear determinate bar — design doc §5: the value→geometry mapping, the
 * four-part anatomy (active indicator, track-active gap, track, stop indicator), the size table,
 * and the RTL mirror.
 *
 * <p>Probes read the bar's mid row, where the chrome is a single 4 dp band, so a pixel is
 * unambiguously one anatomy part. The empty gap is probed against {@link ColorRole#SURFACE} — the
 * ground {@code Pixels.render} paints under the component — which is the contrasting-ground trick
 * that makes an absence probe meaningful.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaLinearProgressDeterminateTest {

  private static final int WIDTH = ElwhaLinearProgressIndicator.PREFERRED_WIDTH_PX;
  private static final int MID_Y = 2;

  // Resolved per call, never cached in a static field: a static initializer runs at class load,
  // before ThemeExtension installs the palette, so it would capture the enum's fallback constant
  // instead of the installed theme's value — a hardcoded hex wearing a role's clothes.
  private static Color active() {
    return ColorRole.PRIMARY.resolve();
  }

  private static Color track() {
    return ColorRole.SECONDARY_CONTAINER.resolve();
  }

  private static Color ground() {
    return ColorRole.SURFACE.resolve();
  }

  private static ElwhaLinearProgressIndicator barAt(final int percent) {
    return new ElwhaLinearProgressIndicator(0, 100, percent);
  }

  private static BufferedImage render(final ElwhaLinearProgressIndicator bar) {
    return Pixels.render(bar, WIDTH, bar.getPreferredSize().height);
  }

  private static void assertPart(
      final BufferedImage image, final int x, final Color want, final String what) {
    Pixels.assertPixelNear(image, x, MID_Y, want, what);
  }

  // -------------------------------------------------------------- size table

  @ParameterizedTest
  @CsvSource({
    // thickness, wavy, expected chrome height — the design §5 table (4 / 10 / 8 / 14)
    "4, false, 4",
    "4, true, 10",
    "8, false, 8",
    "8, true, 14"
  })
  void chromeHeightFollowsThicknessAndTheReservedWaveBand(
      final int thickness, final boolean wavy, final int height) {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();
    bar.setTrackThickness(thickness);
    bar.setWavy(wavy);

    assertThat(bar.getPreferredSize())
        .as("§5 — the wavy shape reserves a band of one amplitude above and below the track")
        .isEqualTo(new Dimension(WIDTH, height));
  }

  @Test
  void barStretchesHorizontallyButNotVertically() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();

    assertThat(bar.getMaximumSize().width)
        .as("§5 — the bar fills whatever width its layout gives it")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(bar.getMaximumSize().height)
        .as("§5 — but never grows past the chrome height")
        .isEqualTo(bar.getPreferredSize().height);
  }

  @Test
  void minimumWidthIsAShortRunAtFullChromeHeight() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();

    assertThat(bar.getMinimumSize())
        .as("§5 — a squeezed bar shortens rather than thinning")
        .isEqualTo(new Dimension(48, bar.getPreferredSize().height));
  }

  @Test
  void insetsAreAddedOnTopOfTheChrome() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();
    bar.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));

    assertThat(bar.getPreferredSize())
        .as("a border reserves its own room; it does not eat the chrome")
        .isEqualTo(new Dimension(WIDTH + 14, 4 + 10));
  }

  // ------------------------------------------------- value → geometry mapping

  @ParameterizedTest
  @CsvSource({"10, 24", "25, 60", "50, 120", "75, 180"})
  void activeIndicatorEndsAtTheValueFractionOfTheRun(final int percent, final int head) {
    final BufferedImage image = render(barAt(percent));

    assertPart(
        image, head - 3, active(), "§5 — the active fill runs right up to its fraction of the run");
    assertPart(
        image,
        head + 2,
        ground(),
        "§5 — and stops there, with the track-active gap opening immediately after");
  }

  @Test
  void aHalfFilledBarShowsAllFourAnatomyParts() {
    final BufferedImage image = render(barAt(50));

    assertPart(image, 60, active(), "§5 — the active indicator fills the leading half");
    assertPart(image, 122, ground(), "§5 — the 4 dp track-active space is empty, not tinted");
    assertPart(image, 180, track(), "§5 — the track carries the remainder");
    assertPart(image, WIDTH - 2, active(), "§5 — the stop indicator caps the trailing end");
  }

  @Test
  void anEmptyBarIsAllTrackWithAStopDot() {
    final BufferedImage image = render(barAt(0));

    assertPart(image, 4, track(), "§5 — at 0% there is no active indicator to gap away from");
    assertPart(image, 120, track(), "so the track owns the whole run");
    assertPart(image, WIDTH - 2, active(), "§5 — the stop dot is present from the start");
  }

  @Test
  void aFullBarIsAllActiveWithNoTrackOrStopDot() {
    final BufferedImage image = render(barAt(100));

    assertPart(image, 4, active(), "§5 — at 100% the active indicator owns the run");
    assertPart(image, 120, active(), "end to end");
    assertPart(
        image,
        WIDTH - 2,
        active(),
        "§5 — and the stop dot is subsumed rather than painted over it");
  }

  @Test
  void stopDotHidesOnceTheActiveHeadReachesIt() {
    // 97%: the head has passed the dot's slot but the track has not yet closed up entirely.
    final BufferedImage image = render(barAt(97));

    assertPart(
        image,
        WIDTH - 2,
        track(),
        "§5 — the dot hides when the head arrives, rather than being overprinted");
  }

  @Test
  void aZeroStopSizeRemovesTheDotEntirely() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.setTrackStopIndicatorSize(0);

    assertPart(
        render(bar),
        WIDTH - 2,
        track(),
        "§5 — zero is the documented way to opt out of the stop indicator");
  }

  @Test
  void gapWidensWithTheGapToken() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.setIndicatorTrackGapSize(16);

    final BufferedImage image = render(bar);

    assertPart(image, 128, ground(), "a wider TrackActiveSpace pushes the track further out");
    assertPart(image, 140, track(), "and the track resumes past it");
  }

  @Test
  void aZeroGapButtsTheTrackAgainstTheActiveHead() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.setIndicatorTrackGapSize(0);

    assertPart(
        render(bar), 122, track(), "the gap is a token, so collapsing it closes the chrome up");
  }

  // -------------------------------------------------------------- color axis

  @Test
  void anatomyRolesAreIndividuallyOverridable() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.setIndicatorColorRole(ColorRole.TERTIARY);
    bar.setTrackColorRole(ColorRole.ERROR_CONTAINER);
    bar.setStopIndicatorColorRole(ColorRole.ERROR);

    final BufferedImage image = render(bar);

    assertPart(image, 60, ColorRole.TERTIARY.resolve(), "the active role reaches the paint");
    assertPart(image, 180, ColorRole.ERROR_CONTAINER.resolve(), "so does the track role");
    assertPart(image, WIDTH - 2, ColorRole.ERROR.resolve(), "and the stop dot has its own");
  }

  @Test
  void defaultRolesAreTheM3Pair() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator();

    assertThat(bar.getIndicatorColorRole()).isEqualTo(ColorRole.PRIMARY);
    assertThat(bar.getTrackColorRole()).isEqualTo(ColorRole.SECONDARY_CONTAINER);
    assertThat(bar.getStopIndicatorColorRole())
        .as("§5 — the stop dot is primary, matching the active indicator")
        .isEqualTo(ColorRole.PRIMARY);
  }

  // --------------------------------------------------------------------- RTL

  @Test
  void rightToLeftFillsFromTheRightEdge() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    final BufferedImage image = render(bar);

    assertPart(image, 180, active(), "§5 — the run's leading end is the right edge under RTL");
    assertPart(image, 60, track(), "and the track takes the left remainder");
  }

  @Test
  void rightToLeftMovesTheStopDotToTheLeftEdge() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    assertPart(
        render(bar),
        2,
        active(),
        "§5 — the stop indicator sits at the visual trailing end, mirrored");
  }

  @Test
  void twoOrientationsAreMirrorImages() {
    final BufferedImage ltr = render(barAt(30));
    final ElwhaLinearProgressIndicator rtlBar = barAt(30);
    rtlBar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    final BufferedImage rtl = render(rtlBar);

    for (int x = 0; x < WIDTH; x++) {
      final int mirrored = rtl.getRGB(WIDTH - 1 - x, MID_Y);
      final Color a = new Color(ltr.getRGB(x, MID_Y), true);
      final Color b = new Color(mirrored, true);
      assertThat(Math.abs(a.getRed() - b.getRed()))
          .as("§5 — RTL is a pure horizontal mirror of the LTR chrome (column %d)", x)
          .isLessThanOrEqualTo(12);
    }
  }

  // ------------------------------------------------------------ degenerate

  @Test
  void aZeroWidthRunPaintsNothingRatherThanThrowing() {
    final ElwhaLinearProgressIndicator bar = barAt(50);
    bar.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

    final BufferedImage image = Pixels.render(bar, 60, bar.getPreferredSize().height);

    assertPart(image, 30, ground(), "insets wider than the component leave no run to paint");
  }
}
