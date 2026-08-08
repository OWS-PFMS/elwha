package com.owspfm.elwha.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.BorderFactory;
import javax.swing.DefaultBoundedRangeModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of the loading indicator as a component — design doc §5 (the size axis and the
 * contained variant's container), §6 (the reduced-motion freeze and the clock gate), and §8 (the
 * shared progress-bar accessibility shape).
 *
 * <p>{@code ThemeExtension} pins reduced motion on for the whole suite, which is not merely a
 * convenience here: it is also the contract under test. With reduced motion active the indicator is
 * required to hold a single static shape, so the painted frames below are stable by design rather
 * than by luck.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaLoadingIndicatorTest {

  private static BufferedImage render(final ElwhaLoadingIndicator indicator) {
    final Dimension size = indicator.getPreferredSize();
    return Pixels.render(indicator, size.width, size.height);
  }

  private static boolean sameRaster(final BufferedImage a, final BufferedImage b) {
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          return false;
        }
      }
    }
    return true;
  }

  private static int countNear(final BufferedImage image, final Color want) {
    int hits = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        final Color got = new Color(image.getRGB(x, y), true);
        if (Math.abs(got.getRed() - want.getRed()) <= 10
            && Math.abs(got.getGreen() - want.getGreen()) <= 10
            && Math.abs(got.getBlue() - want.getBlue()) <= 10) {
          hits++;
        }
      }
    }
    return hits;
  }

  // ------------------------------------------------------------- size axis

  @Test
  void standardIndicatorIsTheM3ActiveSize() {
    assertThat(new ElwhaLoadingIndicator().getPreferredSize())
        .as("§5 — the M3 ActiveSize box, square")
        .isEqualTo(
            new Dimension(
                ElwhaLoadingIndicator.INDICATOR_SIZE_DEFAULT_PX,
                ElwhaLoadingIndicator.INDICATOR_SIZE_DEFAULT_PX));
  }

  @Test
  void containedIndicatorIsTheM3ContainerSize() {
    assertThat(ElwhaLoadingIndicator.contained().getPreferredSize())
        .as("§5 — the contained variant reserves the larger ContainerSize box")
        .isEqualTo(
            new Dimension(
                ElwhaLoadingIndicator.CONTAINER_SIZE_DEFAULT_PX,
                ElwhaLoadingIndicator.CONTAINER_SIZE_DEFAULT_PX));
  }

  @Test
  void indicatorIsAFixedSizeWidget() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    final Dimension preferred = indicator.getPreferredSize();

    assertThat(indicator.getMinimumSize()).isEqualTo(preferred);
    assertThat(indicator.getMaximumSize())
        .as("§5 — a spinner has one legible size; it does not stretch with its layout")
        .isEqualTo(preferred);
  }

  @ParameterizedTest
  @CsvSource({"8, 8", "24, 24", "64, 64", "1, 8", "-5, 8"})
  void indicatorSizeIsClampedToSomethingPaintable(final int asked, final int stored) {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    indicator.setIndicatorSize(asked);

    assertThat(indicator.getIndicatorSize())
        .as("a sub-8 px shape has no room for its lobes, so the setter floors it")
        .isEqualTo(stored);
  }

  @Test
  void containerSizeIsClampedTheSameWay() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();

    indicator.setContainerSize(2);

    assertThat(indicator.getContainerSize()).isEqualTo(8);
  }

  @Test
  void switchingToContainedGrowsThePreferredBox() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    final int standalone = indicator.getPreferredSize().width;

    indicator.setContained(true);

    assertThat(indicator.getPreferredSize().width)
        .as("§5 — the container box is what gets reserved once one is painted")
        .isGreaterThan(standalone);
  }

  @Test
  void insetsAreAddedAroundTheBox() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    indicator.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    assertThat(indicator.getPreferredSize())
        .isEqualTo(
            new Dimension(
                ElwhaLoadingIndicator.INDICATOR_SIZE_DEFAULT_PX + 8,
                ElwhaLoadingIndicator.INDICATOR_SIZE_DEFAULT_PX + 8));
  }

  // ------------------------------------------------------- containment

  @Test
  void aStandaloneIndicatorPaintsNoContainer() {
    final BufferedImage image = render(new ElwhaLoadingIndicator());

    Pixels.assertPixelNear(
        image,
        0,
        0,
        ColorRole.SURFACE.resolve(),
        "§5 — without the contained variant the corner shows whatever is behind the shape");
  }

  @Test
  void aContainedIndicatorPaintsItsContainerCircle() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();
    final int mid = indicator.getPreferredSize().width / 2;

    final BufferedImage image = render(indicator);

    Pixels.assertPixelNear(
        image,
        2,
        mid,
        ColorRole.PRIMARY_CONTAINER.resolve(),
        "§5 — the container circle spans the reserved box, so its edge sits at the box edge");
  }

  @Test
  void containedFactoryUsesTheM3ColorPairing() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();

    assertThat(indicator.getContainerColorRole()).isEqualTo(ColorRole.PRIMARY_CONTAINER);
    assertThat(indicator.getIndicatorColorRole())
        .as("§5 — the active shape takes the container's paired content role")
        .isEqualTo(ColorRole.ON_PRIMARY_CONTAINER);
  }

  @Test
  void activeShapeStaysInsideItsContainer() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();
    final BufferedImage image = render(indicator);

    final int[] active = horizontalExtent(image, ColorRole.ON_PRIMARY_CONTAINER.resolve());
    final int[] container = horizontalExtent(image, ColorRole.PRIMARY_CONTAINER.resolve());

    assertThat(active[0]).as("the shape is painted at all").isNotNegative();
    assertThat(active[0])
        .as("§5 — the M3 38/48 ratio leaves a ring of container on the leading side")
        .isGreaterThan(container[0]);
    assertThat(active[1])
        .as("§5 — and on the trailing side; the shape never reaches the container's edge")
        .isLessThan(container[1]);
  }

  @Test
  void containedRatioSurvivesBeingPaintedIntoASmallerBox() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();
    final int preferred = indicator.getPreferredSize().width;

    final double atPreferred = activeToContainerRatio(indicator, preferred);
    final double squeezed = activeToContainerRatio(indicator, preferred / 2);

    assertThat(atPreferred)
        .as("§5 — the shape spans roughly the M3 38-of-48 fraction of its container")
        .isBetween(0.65, 0.85);
    assertThat(squeezed)
        .as(
            "§5 — a layout that hands the indicator less room than it asked for shrinks the "
                + "container and the shape together, rather than letting the shape burst out")
        .isCloseTo(atPreferred, within(0.08));
  }

  @Test
  void bothSizeKnobsTogetherSetTheContainedProportions() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();
    indicator.setIndicatorSize(19);
    indicator.setContainerSize(24);

    assertThat(activeToContainerRatio(indicator, 24))
        .as("§5 — halving both knobs is the same composition at half scale")
        .isBetween(0.65, 0.85);
  }

  /** The painted shape's width as a fraction of the painted container's, at a given box size. */
  private static double activeToContainerRatio(
      final ElwhaLoadingIndicator indicator, final int box) {
    final BufferedImage image = Pixels.render(indicator, box, box);
    final int[] active = horizontalExtent(image, ColorRole.ON_PRIMARY_CONTAINER.resolve());
    final int[] container = horizontalExtent(image, ColorRole.PRIMARY_CONTAINER.resolve());
    return (active[1] - active[0]) / (double) (container[1] - container[0]);
  }

  /** The leftmost and rightmost columns carrying {@code want}, or {@code {-1, -1}} if absent. */
  private static int[] horizontalExtent(final BufferedImage image, final Color want) {
    int min = -1;
    int max = -1;
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final Color got = new Color(image.getRGB(x, y), true);
        if (Math.abs(got.getRed() - want.getRed()) <= 10
            && Math.abs(got.getGreen() - want.getGreen()) <= 10
            && Math.abs(got.getBlue() - want.getBlue()) <= 10) {
          if (min < 0) {
            min = x;
          }
          max = x;
          break;
        }
      }
    }
    return new int[] {min, max};
  }

  @Test
  void settingContainedDoesNotRepaintTheColorRoles() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    indicator.setContained(true);

    assertThat(indicator.getIndicatorColorRole())
        .as("the setter is a geometry switch; the documented color pairing is the factory's job")
        .isEqualTo(ColorRole.PRIMARY);
  }

  // ------------------------------------------------------------ color axis

  @Test
  void shapeIsPaintedInTheIndicatorRole() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    indicator.setIndicatorColorRole(ColorRole.TERTIARY);

    assertThat(countNear(render(indicator), ColorRole.TERTIARY.resolve()))
        .as("the role reaches the fill, not just the getter")
        .isPositive();
  }

  @Test
  void containerIsPaintedInTheContainerRole() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.contained();
    indicator.setContainerColorRole(ColorRole.ERROR_CONTAINER);

    assertThat(countNear(render(indicator), ColorRole.ERROR_CONTAINER.resolve()))
        .as("so does the container's")
        .isPositive();
  }

  // ------------------------------------------------------------ mode axis

  @Test
  void anIndicatorIsIndeterminateByDefault() {
    assertThat(new ElwhaLoadingIndicator().isIndeterminate())
        .as("the component exists for short indeterminate waits; that is its default posture")
        .isTrue();
  }

  @Test
  void determinateFactoriesFlipTheMode() {
    assertThat(ElwhaLoadingIndicator.determinate().isIndeterminate()).isFalse();
    assertThat(ElwhaLoadingIndicator.containedDeterminate().isIndeterminate()).isFalse();
    assertThat(ElwhaLoadingIndicator.containedDeterminate().isContained()).isTrue();
  }

  @Test
  void aSuppliedModelIsSharedRatherThanCopied() {
    final DefaultBoundedRangeModel shared = new DefaultBoundedRangeModel(0, 0, 0, 100);
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.determinate(shared);

    shared.setValue(40);

    assertThat(indicator.getValue()).isEqualTo(40);
    assertThat(indicator.getProgressFraction()).isCloseTo(0.4f, within(0.001f));
  }

  @Test
  void aNullModelIsRejected() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new ElwhaLoadingIndicator(null));
  }

  @Test
  void aDegenerateRangeReadsAsZeroProgress() {
    final ElwhaLoadingIndicator indicator =
        ElwhaLoadingIndicator.determinate(new DefaultBoundedRangeModel(3, 0, 3, 3));

    assertThat(indicator.getProgressFraction())
        .as("a zero-width range must not divide by zero")
        .isZero();
  }

  @Test
  void determinateShapeChangesWithTheValue() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.determinate();
    final BufferedImage empty = render(indicator);

    indicator.setValue(100);

    assertThat(sameRaster(empty, render(indicator)))
        .as("§3 — the determinate morph is driven by progress, so the shape must visibly change")
        .isFalse();
  }

  // ------------------------------------------------------- reduced motion

  @Test
  void reducedMotionIsPinnedOnForTheSuite() {
    assertThat(MorphAnimator.isReducedMotion())
        .as("ThemeExtension pins it, so no assertion in this package can race an animation")
        .isTrue();
  }

  @Test
  void reducedMotionFreezesTheIndeterminateSpinnerOnOneShape() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    final BufferedImage first = render(indicator);
    final BufferedImage second = render(indicator);

    assertThat(sameRaster(first, second))
        .as("§6 — reduced motion snaps the spinner to a static shape rather than animating it")
        .isTrue();
  }

  @Test
  void frozenShapeIsTheFirstFormOfTheLoop() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    final ElwhaLoadingIndicator reference = ElwhaLoadingIndicator.determinate();
    reference.setValue(0);

    // The determinate start shape is the circle; the frozen indeterminate shape is the loop's
    // first form, so the two must differ — proving the freeze picks a real catalog shape rather
    // than falling back to a default.
    assertThat(sameRaster(render(indicator), render(reference)))
        .as("§6 — the freeze lands on the loop's opening form, not on a generic disc")
        .isFalse();
  }

  @Test
  void clockNeverRunsUnderReducedMotion() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    assertThat(indicator.isAnimating())
        .as("§6 — no timer is spent repainting a frozen shape")
        .isFalse();
  }

  @Test
  void clockNeverRunsWhileTheIndicatorIsNotShowing() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    indicator.removeNotify();

    assertThat(indicator.isAnimating())
        .as("§6 — hierarchy-gated, and stopped on teardown")
        .isFalse();
  }

  // ------------------------------------------------------------- posture

  @Test
  void indicatorIsNonInteractive() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    assertThat(indicator.isFocusable())
        .as("a spinner is a status display, not a control")
        .isFalse();
    assertThat(indicator.isOpaque())
        .as("the shape is round, so the component must not claim its whole box")
        .isFalse();
  }

  // ------------------------------------------------------- accessibility

  @Test
  void roleIsProgressBar() {
    assertThat(new ElwhaLoadingIndicator().getAccessibleContext().getAccessibleRole())
        .as("§8 — the same role the progress family reports")
        .isEqualTo(AccessibleRole.PROGRESS_BAR);
  }

  @Test
  void anIndeterminateIndicatorIsBusyAndWithholdsItsValue() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();
    indicator.setValue(50);

    final AccessibleContext context = indicator.getAccessibleContext();

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.BUSY))
        .as("§8 — 'working, duration unknown'")
        .isTrue();
    assertThat(context.getAccessibleValue().getCurrentAccessibleValue())
        .as("§8 — an indeterminate spinner has no progress to announce")
        .isNull();
  }

  @Test
  void aDeterminateIndicatorReportsItsValue() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.determinate();
    indicator.setValue(35);

    final AccessibleContext context = indicator.getAccessibleContext();

    assertThat(context.getAccessibleValue().getCurrentAccessibleValue()).isEqualTo(35);
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.BUSY)).isFalse();
  }

  @Test
  void accessibleValueCanDriveTheModel() {
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.determinate();

    assertThat(indicator.getAccessibleContext().getAccessibleValue().setCurrentAccessibleValue(20))
        .isTrue();
    assertThat(indicator.getValue()).isEqualTo(20);
  }

  @Test
  void leavingTheHierarchyReleasesTheValueModel() {
    final DefaultBoundedRangeModel shared = new DefaultBoundedRangeModel(40, 0, 0, 100);
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.determinate(shared);
    assertThat(shared.getChangeListeners())
        .as("the indicator subscribes at construction")
        .hasSize(1);

    indicator.addNotify();
    indicator.removeNotify();

    assertThat(shared.getChangeListeners())
        .as("#621 — a shared model would otherwise pin every readout it has ever driven")
        .isEmpty();
  }

  @Test
  void aReAddedIndicatorIsSubscribedOnceAndReadsTheModelAsItStandsNow() {
    final DefaultBoundedRangeModel shared = new DefaultBoundedRangeModel(40, 0, 0, 100);
    final ElwhaLoadingIndicator indicator = ElwhaLoadingIndicator.determinate(shared);
    indicator.addNotify();
    indicator.removeNotify();

    shared.setValue(90);
    indicator.addNotify();

    assertThat(shared.getChangeListeners())
        .as("a stage swap re-arms the subscription exactly once")
        .hasSize(1);
    assertThat(indicator.getProgressFraction())
        .as("and the readout reflects the model as it stands now, not as it left it")
        .isCloseTo(0.9f, within(0.001f));
  }

  @Test
  void consumersCanNameTheActivity() {
    final ElwhaLoadingIndicator indicator = new ElwhaLoadingIndicator();

    indicator.getAccessibleContext().setAccessibleName("Loading…");

    assertThat(indicator.getAccessibleContext().getAccessibleName()).isEqualTo("Loading…");
  }
}
