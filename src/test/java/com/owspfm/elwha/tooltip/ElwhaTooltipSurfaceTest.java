package com.owspfm.elwha.tooltip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.Corners;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the painted tooltip surface: the two variants' container roles and content
 * roles, the shadow reserve each one declares (the number the placement engine backs out), the M3
 * size bounds, and the hand-rolled word wrap that keeps a long label inside the max width without
 * an HTML view.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTooltipSurfaceTest {

  private static TooltipSurface plain(final String text) {
    return new TooltipSurface(TooltipVariant.PLAIN, text, null, null);
  }

  private static TooltipSurface rich(final String subhead, final String supporting) {
    return new TooltipSurface(TooltipVariant.RICH, null, subhead, supporting);
  }

  private static FontMetrics metricsOf(final JComponent host, final TypeRole role) {
    return host.getFontMetrics(role.resolve());
  }

  // ------------------------------------------------------------------- plain

  @Test
  void aPlainBubbleFillsTheInverseSurfaceRole() {
    final TooltipSurface surface = plain("Rename");
    final Dimension pref = surface.getPreferredSize();

    Pixels.assertPixelNear(
        Pixels.render(surface, pref.width, pref.height),
        pref.width / 2,
        2,
        ColorRole.INVERSE_SURFACE.resolve(),
        "the plain bubble is the inverse surface, so it reads against the page behind it");
  }

  @Test
  void aPlainLabelPaintsInTheInverseOnSurfaceRole() {
    final TooltipSurface surface = plain("Rename");
    final Dimension pref = surface.getPreferredSize();

    final PaintLog log = PaintLog.capture(surface, pref.width, pref.height);

    assertThat(log.text("Rename").orElseThrow().color())
        .as("label content takes the inverse surface's own on-role")
        .isEqualTo(ColorRole.INVERSE_ON_SURFACE.resolve());
  }

  @Test
  void aPlainBubbleReservesNoShadow() {
    assertThat(plain("Rename").halo())
        .as("the plain variant is flat, so the placement engine has nothing to back out")
        .isEqualTo(new Insets(0, 0, 0, 0));
  }

  @Test
  void aShortLabelIsHeldAtTheMinimumBubbleSize() {
    final Dimension pref = plain("Hi").getPreferredSize();

    assertThat(pref.width).isGreaterThanOrEqualTo(TooltipSurface.MIN_WIDTH_PX);
    assertThat(pref.height).isGreaterThanOrEqualTo(TooltipSurface.MIN_HEIGHT_PX);
  }

  @Test
  void aLongLabelWrapsRatherThanGrowingPastTheMaxWidth() {
    final TooltipSurface surface =
        plain("Rename this layer and every layer nested beneath it in the current composition");

    final Dimension pref = surface.getPreferredSize();

    assertThat(pref.width)
        .as("the bubble stops at the M3 plain max width")
        .isLessThanOrEqualTo(TooltipSurface.PLAIN_MAX_WIDTH_PX);
    assertThat(pref.height)
        .as("and takes the height back in extra lines")
        .isGreaterThan(TooltipSurface.MIN_HEIGHT_PX);
  }

  // -------------------------------------------------------------------- rich

  @Test
  void aRichCardFillsTheSurfaceContainerRole() {
    final TooltipSurface surface = rich("Layers", "Stack order of the canvas");
    final Dimension pref = surface.getPreferredSize();
    final Insets halo = surface.halo();

    Pixels.assertPixelNear(
        Pixels.render(surface, pref.width, pref.height),
        pref.width / 2,
        halo.top + 2,
        ColorRole.SURFACE_CONTAINER.resolve(),
        "the rich card is an ordinary elevated surface, not the inverse bubble");
  }

  @Test
  void richContentPaintsInTheVariantRole() {
    final TooltipSurface surface = rich("Layers", "Stack order of the canvas");
    final Dimension pref = surface.getPreferredSize();

    final PaintLog log = PaintLog.capture(surface, pref.width, pref.height);

    assertThat(log.text("Layers").orElseThrow().color())
        .as("the subhead is on-surface-variant")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
    assertThat(log.text("Stack order of the canvas").orElseThrow().color())
        .as("and so is the supporting paragraph")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
  }

  @Test
  void aRichCardReservesItsElevationHalo() {
    assertThat(rich("Layers", "Stack order").halo())
        .as("the rich card floats, so its shadow reserve is what placement backs out")
        .isEqualTo(ShadowPainter.shadowInsets(TooltipSurface.RICH_ELEVATION));
  }

  @Test
  void aRichCardKeepsItsBodyInsideTheMaxWidth() {
    final TooltipSurface surface =
        rich(
            "Layers",
            "The stacking order of every element on the canvas, from the backmost layer to the "
                + "frontmost one, including groups and their nested children");
    final Insets halo = surface.halo();

    final Dimension pref = surface.getPreferredSize();

    assertThat(pref.width - halo.left - halo.right)
        .as("the card's body stops at the M3 rich max width")
        .isLessThanOrEqualTo(TooltipSurface.RICH_MAX_WIDTH_PX);
  }

  @Test
  void aRichCardWithoutASubheadStillLaysOutItsSupportingText() {
    final TooltipSurface surface = rich(null, "Stack order of the canvas");
    final Dimension pref = surface.getPreferredSize();

    final PaintLog log = PaintLog.capture(surface, pref.width, pref.height);

    assertThat(log.painted("Stack order of the canvas"))
        .as("the subhead is optional; the paragraph moves up to take its baseline")
        .isTrue();
  }

  @Test
  void anActionButtonBecomesAChildAndIsPlacedInTheActionRow() {
    final TooltipSurface surface = rich("Layers", "Stack order of the canvas");
    final ElwhaButton action = ElwhaButton.textButton("Learn more");
    surface.addActionButton(action);
    final Dimension pref = surface.getPreferredSize();

    surface.setSize(pref);
    surface.doLayout();

    assertThat(surface.actionButtons()).containsExactly(action);
    assertThat(action.getParent()).as("actions are real children of the card").isSameAs(surface);
    assertThat(action.getWidth()).as("and are laid out at their own preferred width").isPositive();
    assertThat(action.getY() + action.getHeight())
        .as("inside the card, above its bottom padding")
        .isLessThanOrEqualTo(pref.height);
  }

  // ------------------------------------------------------------ paint origin

  @Test
  void aFadingSurfaceIsAPaintingOriginSoItsActionButtonsCannotPopInAtFullAlpha() {
    final TooltipSurface surface = rich("Layers", "Stack order of the canvas");
    surface.addActionButton(ElwhaButton.textButton("Learn more"));
    surface.setAlphaSupplier(() -> 0.4f);

    assertThat(surface.isPaintingOrigin())
        .as("mid-entrance the composite owns every child's render, so child repaints route here")
        .isTrue();
  }

  @Test
  void aSettledSurfaceIsNotAPaintingOrigin() {
    final TooltipSurface surface = rich("Layers", "Stack order of the canvas");
    surface.addActionButton(ElwhaButton.textButton("Learn more"));
    surface.setAlphaSupplier(() -> 1f);

    assertThat(surface.isPaintingOrigin())
        .as("at rest the composite is a no-op, so children keep their own repaint optimization")
        .isFalse();
  }

  // -------------------------------------------------------------------- a11y

  @Test
  void aSurfaceAnnouncesItselfAsATooltip() {
    assertThat(plain("Rename").getAccessibleContext().getAccessibleRole())
        .isEqualTo(AccessibleRole.TOOL_TIP);
  }

  // -------------------------------------------------------------------- wrap

  @Test
  void wrappingBreaksOnWhitespaceGreedily() {
    final TooltipSurface host = plain("x");
    final FontMetrics metrics = metricsOf(host, TypeRole.BODY_SMALL);
    final int widestWord =
        Math.max(
            metrics.stringWidth("alpha"),
            Math.max(metrics.stringWidth("beta"), metrics.stringWidth("gamma")));

    final List<String> lines = TooltipSurface.wrap("alpha beta gamma", metrics, widestWord);

    assertThat(lines)
        .as("each line takes as many whole words as fit")
        .containsExactly("alpha", "beta", "gamma");
  }

  @Test
  void wrappingHonorsAnExplicitLineBreak() {
    final TooltipSurface host = plain("x");
    final FontMetrics metrics = metricsOf(host, TypeRole.BODY_SMALL);

    final List<String> lines = TooltipSurface.wrap("first\nsecond", metrics, 10_000);

    assertThat(lines)
        .as("an explicit newline forces a break even when the text would fit on one line")
        .containsExactly("first", "second");
  }

  @Test
  void wrappingHardBreaksAWordWiderThanTheWrapWidth() {
    final TooltipSurface host = plain("x");
    final FontMetrics metrics = metricsOf(host, TypeRole.BODY_SMALL);
    final String unbroken = "supercalifragilistic";

    final List<String> lines = TooltipSurface.wrap(unbroken, metrics, metrics.stringWidth("super"));

    assertThat(lines)
        .as("a single long token is split rather than allowed to overflow")
        .hasSizeGreaterThan(1);
    assertThat(String.join("", lines)).as("without losing any characters").isEqualTo(unbroken);
  }

  @Test
  void wrappingNeverReturnsNoLines() {
    final TooltipSurface host = plain("x");

    assertThat(TooltipSurface.wrap("", metricsOf(host, TypeRole.BODY_SMALL), 100))
        .as("empty text still yields one (empty) line, so the caller never divides by zero")
        .hasSize(1);
  }

  // ------------------------------------------------------------ corner radius (#663)

  @Test
  void aPlainBubbleKeepsItsExtraSmallFourPixelCorner() {
    final TooltipSurface surface = plain("Rename");
    final Dimension pref = surface.getPreferredSize();
    final BufferedImage shot = Pixels.render(surface, pref.width, pref.height, Color.MAGENTA);

    Corners.assertTopLeftRadius(
        shot,
        0,
        0,
        ShapeScale.XS.px(),
        ColorRole.INVERSE_SURFACE.resolve(),
        Color.MAGENTA,
        ShapeScale.XS.px(),
        "the tooltip was already one of the sites that doubled at the painter, so the #663"
            + " normalization must not move it");
  }

  @Test
  void aRichCardKeepsItsMediumTwelvePixelCorner() {
    final TooltipSurface surface = rich("Subhead", "Supporting text");
    final Dimension pref = surface.getPreferredSize();
    final BufferedImage shot = Pixels.render(surface, pref.width, pref.height, Color.MAGENTA);
    final Insets halo = surface.halo();

    Corners.assertTopLeftRadius(
        shot,
        halo.left,
        halo.top,
        ShapeScale.MD.px(),
        ColorRole.SURFACE_CONTAINER.resolve(),
        new Color(shot.getRGB(halo.left - 3, halo.top - 3), true),
        ShapeScale.MD.px(),
        "the rich card's 12 dp corner is unchanged, body and halo still on one silhouette");
  }
}
