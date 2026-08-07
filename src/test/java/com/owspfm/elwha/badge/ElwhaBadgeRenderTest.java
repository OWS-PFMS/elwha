package com.owspfm.elwha.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of badge chrome — design doc §6 (the {@code Error} / {@code On error} default
 * mapping and the override surface) and §7 (the anatomy: Small is container-only, Large is
 * container plus a {@link TypeRole#LABEL_SMALL} label).
 *
 * <p>The label is asserted through {@link PaintLog} rather than a pixel probe: the determinism
 * rules forbid probing inside a glyph box, and a recorded {@code drawString} carries the exact
 * string, font and resolved color — which is the whole contract anyway.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaBadgeRenderTest {

  private static PaintLog paint(final ElwhaBadge badge) {
    return PaintLog.capture(badge);
  }

  private static Rectangle2D containerBounds(final PaintLog log) {
    return log.shapes().stream()
        .filter(p -> !p.stroked())
        .findFirst()
        .orElseThrow(() -> new AssertionError("the badge painted no container fill at all"))
        .bounds();
  }

  // ------------------------------------------------------------- container

  @Test
  void aSmallBadgePaintsASolidDotInTheErrorRole() {
    final ElwhaBadge badge = ElwhaBadge.small();
    final Dimension size = badge.getPreferredSize();

    Pixels.assertPixelNear(
        Pixels.render(badge, size.width, size.height),
        size.width / 2,
        size.height / 2,
        ColorRole.ERROR.resolve(),
        "§6 — the Small dot is filled in the default Error container role");
  }

  @Test
  void aLargeBadgePaintsItsPillInTheErrorRole() {
    final ElwhaBadge badge = ElwhaBadge.large("9");
    final Dimension size = badge.getPreferredSize();

    Pixels.assertPixelNear(
        Pixels.render(badge, size.width, size.height),
        2,
        size.height / 2,
        ColorRole.ERROR.resolve(),
        "§6 — the Large container carries the same default container role as Small");
  }

  @Test
  void theContainerFillSpansTheWholeBadgeBox() {
    final ElwhaBadge badge = ElwhaBadge.large("42");
    final Dimension size = badge.getPreferredSize();

    final Rectangle2D container = containerBounds(paint(badge));

    assertThat(container.getWidth())
        .as("the container is the badge — there is no inset chrome around it")
        .isEqualTo(size.width);
    assertThat(container.getHeight()).isEqualTo(size.height);
  }

  @Test
  void theSmallDotIsFullyRoundRatherThanASquare() {
    final ElwhaBadge badge = ElwhaBadge.small();
    final Dimension size = badge.getPreferredSize();

    // Probe a corner of the 6 dp box: on a full-round dot the corner is outside the disc, so it
    // still shows the ground the badge was painted over.
    Pixels.assertPixelNear(
        Pixels.render(badge, size.width, size.height),
        0,
        0,
        ColorRole.SURFACE.resolve(),
        "§7.1 — a 6 dp arc on a 6 dp box is a circle, so the box corner stays empty");
  }

  @Test
  void theLargePillHasRoundEndCaps() {
    final ElwhaBadge badge = ElwhaBadge.large("999+");
    final Dimension size = badge.getPreferredSize();

    Pixels.assertPixelNear(
        Pixels.render(badge, size.width, size.height),
        0,
        0,
        ColorRole.SURFACE.resolve(),
        "§7.2 — a 16 dp arc on a 16 dp-tall pill rounds the caps fully, clearing the box corner");
  }

  // ----------------------------------------------------------- color axis

  @Test
  void theDefaultColorMappingIsErrorOnError() {
    assertThat(ElwhaBadge.small().getContainerColor())
        .as("§6 — Error is the M3 default badge container")
        .isEqualTo(ColorRole.ERROR);
    assertThat(ElwhaBadge.large("1").getLabelColor())
        .as("§6 — and On error is its paired content role")
        .isEqualTo(ColorRole.ON_ERROR);
  }

  @Test
  void aSmallBadgeHasNoLabelColorToReport() {
    assertThat(ElwhaBadge.small().getLabelColor())
        .as("§2 — Small has no label sub-part, so it reports no label role")
        .isNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = ColorRole.class,
      names = {"PRIMARY", "TERTIARY", "SECONDARY_CONTAINER"})
  void anOverriddenContainerRoleIsWhatGetsPainted(final ColorRole role) {
    final ElwhaBadge badge = ElwhaBadge.small().withContainerColor(role);
    final Dimension size = badge.getPreferredSize();

    Pixels.assertPixelNear(
        Pixels.render(badge, size.width, size.height),
        size.width / 2,
        size.height / 2,
        role.resolve(),
        "§6 — the override surface reaches the paint, not just the getter");
  }

  @Test
  void anOverriddenLabelRoleIsWhatTheGlyphIsDrawnIn() {
    final ElwhaBadge badge =
        ElwhaBadge.large("7")
            .withContainerColor(ColorRole.PRIMARY)
            .withLabelColor(ColorRole.ON_PRIMARY);

    final Optional<PaintLog.Text> label = paint(badge).text("7");

    assertThat(label).as("the label is painted at all").isPresent();
    assertThat(label.orElseThrow().color())
        .as("§6 — the consumer's paired content role is resolved at paint time")
        .isEqualTo(ColorRole.ON_PRIMARY.resolve());
  }

  @Test
  void colorOverridesRejectNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ElwhaBadge.small().withContainerColor(null));
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ElwhaBadge.large("1").withLabelColor(null));
  }

  @Test
  void aSmallBadgeRejectsALabelColor() {
    assertThatExceptionOfType(IllegalStateException.class)
        .as("there is no label sub-part on Small to color")
        .isThrownBy(() -> ElwhaBadge.small().withLabelColor(ColorRole.ON_PRIMARY));
  }

  @Test
  void colorOverridesAreFluent() {
    final ElwhaBadge badge = ElwhaBadge.large("1");

    assertThat(badge.withContainerColor(ColorRole.PRIMARY)).isSameAs(badge);
    assertThat(badge.withLabelColor(ColorRole.ON_PRIMARY)).isSameAs(badge);
  }

  @Test
  void rolesAreResolvedAtPaintTimeSoAModeSwitchFollows() {
    final ElwhaBadge badge = ElwhaBadge.small();
    final Dimension size = badge.getPreferredSize();

    ThemeExtension.install(Mode.DARK);

    Pixels.assertPixelNear(
        Pixels.render(badge, size.width, size.height, ColorRole.SURFACE.resolve()),
        size.width / 2,
        size.height / 2,
        ColorRole.ERROR.resolve(),
        "the badge holds a role, not a color — dark mode re-resolves it with no re-construction");
  }

  // ---------------------------------------------------------------- label

  @Test
  void aLargeBadgePaintsExactlyItsStoredContent() {
    final PaintLog log = paint(ElwhaBadge.large(1500));

    assertThat(log.painted("999+"))
        .as("§3 — the coerced value is what reaches the screen")
        .isTrue();
    assertThat(log.painted("1500")).as("and the raw value never does").isFalse();
  }

  @Test
  void theLabelIsDrawnAtLabelSmall() {
    final PaintLog.Text label = paint(ElwhaBadge.large("42")).text("42").orElseThrow();

    assertThat(label.font())
        .as("§7.2 — the Large label is the LABEL_SMALL type role")
        .isEqualTo(TypeRole.LABEL_SMALL.resolve());
  }

  @Test
  void theLabelIsDrawnInTheOnErrorRoleByDefault() {
    final PaintLog.Text label = paint(ElwhaBadge.large("42")).text("42").orElseThrow();

    assertThat(label.color())
        .as("§6 — On error is the default paired content role")
        .isEqualTo(ColorRole.ON_ERROR.resolve());
  }

  @Test
  void theLabelIsHorizontallyCenteredInThePill() {
    final ElwhaBadge badge = ElwhaBadge.large("42");
    final int width = badge.getPreferredSize().width;
    final int textWidth = badge.getFontMetrics(TypeRole.LABEL_SMALL.resolve()).stringWidth("42");

    final PaintLog.Text label = paint(badge).text("42").orElseThrow();

    assertThat(label.x())
        .as("§4.2 — equal padding columns means the glyph run starts at the centering offset")
        .isEqualTo((width - textWidth) / 2.0);
  }

  @Test
  void theLabelBaselineCentersTheGlyphBandNotTheLineBox() {
    final ElwhaBadge badge = ElwhaBadge.large("42");
    final int height = badge.getPreferredSize().height;
    final java.awt.FontMetrics fm = badge.getFontMetrics(TypeRole.LABEL_SMALL.resolve());

    final PaintLog.Text label = paint(badge).text("42").orElseThrow();

    assertThat(label.y())
        .as("the ascent-minus-descent band is centered, which is what kills the downward drift")
        .isEqualTo((height + fm.getAscent() - fm.getDescent()) / 2);
  }

  @Test
  void theLabelStaysInsideTheContainer() {
    final ElwhaBadge badge = ElwhaBadge.large("999+");
    final PaintLog log = paint(badge);
    final int textWidth = badge.getFontMetrics(TypeRole.LABEL_SMALL.resolve()).stringWidth("999+");

    final PaintLog.Text label = log.text("999+").orElseThrow();
    final Rectangle2D container = containerBounds(log);

    assertThat(label.x())
        .as("§4.2 — the label never breaches the leading padding column")
        .isGreaterThanOrEqualTo(container.getMinX());
    assertThat(label.x() + textWidth)
        .as("nor the trailing one, which is why the pill widens with its content")
        .isLessThanOrEqualTo(container.getMaxX());
  }

  @Test
  void aSmallBadgePaintsNoTextAtAll() {
    assertThat(paint(ElwhaBadge.small()).texts())
        .as("§7.1 — the Small anatomy is a container and nothing else")
        .isEmpty();
  }

  @Test
  void aRelabeledBadgePaintsTheNewContent() {
    final ElwhaBadge badge = ElwhaBadge.large("1");

    badge.setContent("42");

    final PaintLog log = paint(badge);
    assertThat(log.painted("42")).as("the new label is what paints").isTrue();
    assertThat(log.painted("1")).as("and the old one is gone").isFalse();
  }
}
