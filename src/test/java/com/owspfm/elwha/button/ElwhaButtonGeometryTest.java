package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.ShadowPainter;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A geometry coverage for {@link ElwhaButton} — the Appendix A measurement tables, the WCAG
 * touch-target inflation, the {@code ShadowBearing} halo reserve, icon slot sizing, and the
 * connected-segment rendering switch.
 *
 * <p>Widths are asserted against the size tier's own padding tokens on label-free buttons (exact
 * integers, no font in the path) and against {@link FontMetrics} where a label is involved — text
 * geometry rather than text pixels, per the determinism rules.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonGeometryTest {

  /** A fixed-size icon that is not a {@code FlatSVGIcon}, so it cannot be losslessly rescaled. */
  private static final class FixedIcon implements Icon {
    private final int side;
    private int paintedX = -1;

    FixedIcon(final int side) {
      this.side = side;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      // geometry-only fixture; nothing to draw beyond recording where it was asked to draw
      this.paintedX = x;
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

  // -------------------------------------------------------- size tier table

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void aLabelFreeButtonIsExactlyTwiceItsTierPadding(final ButtonSize size) {
    final ElwhaButton button = new ElwhaButton().setButtonSize(size);

    assertThat(button.getPreferredSize().width)
        .as("%s hugs its content with symmetric no-icon padding", size)
        .isEqualTo(size.paddingNoIconPx() * 2);
  }

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void anIconOnlyButtonSpendsItsTiersLeadingGapAndTrailingPadding(final ButtonSize size) {
    final ElwhaButton button =
        new ElwhaButton().setButtonSize(size).setIcon(new FixedIcon(size.iconSizePx()));

    assertThat(button.getPreferredSize().width)
        .as("%s reserves leading + icon + gap + trailing once an icon is installed", size)
        .isEqualTo(
            size.paddingWithIconLeadingPx()
                + size.iconSizePx()
                + size.paddingWithIconGapPx()
                + size.paddingWithIconTrailingPx());
  }

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void heightIsTheContainerOrTheTouchTargetWhicheverIsLarger(final ButtonSize size) {
    final ElwhaButton button = new ElwhaButton().setButtonSize(size);

    assertThat(button.getPreferredSize().height)
        .as(
            "%s inflates the cross axis to the WCAG target only when the container is below it",
            size)
        .isEqualTo(Math.max(size.containerHeightPx(), size.minimumTargetPx()));
  }

  @Test
  void onlyTheTwoSmallTiersAreInflatedAtAll() {
    assertThat(new ElwhaButton().setButtonSize(ButtonSize.XS).getPreferredSize().height)
        .as("XS grows from its 32 dp container to the 48 dp touch target")
        .isEqualTo(48);
    assertThat(new ElwhaButton().setButtonSize(ButtonSize.S).getPreferredSize().height)
        .as("S grows from its 40 dp container to the 48 dp touch target")
        .isEqualTo(48);
    assertThat(new ElwhaButton().setButtonSize(ButtonSize.M).getPreferredSize().height)
        .as("M is already above the target and is reported at its container height")
        .isEqualTo(ButtonSize.M.containerHeightPx());
  }

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void aLabelAddsExactlyItsMeasuredWidth(final ButtonSize size) {
    final ElwhaButton button = new ElwhaButton("Save").setButtonSize(size);
    final FontMetrics metrics = button.getFontMetrics(size.typeRole().resolve());

    assertThat(button.getPreferredSize().width)
        .as("%s adds the label measured in its own type role, nothing more", size)
        .isEqualTo(size.paddingNoIconPx() * 2 + metrics.stringWidth("Save"));
  }

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void minimumSizeTracksPreferredSize(final ButtonSize size) {
    final ElwhaButton button = new ElwhaButton("Save").setButtonSize(size);

    assertThat(button.getMinimumSize())
        .as("%s refuses to be laid out below its content-hugging size", size)
        .isEqualTo(button.getPreferredSize());
  }

  // ------------------------------------------------------- shadow reserve

  @ParameterizedTest
  @EnumSource(ButtonVariant.class)
  void onlyTheElevatedVariantReservesAHalo(final ButtonVariant variant) {
    final ElwhaButton button = new ElwhaButton().setVariant(variant);

    final Insets want =
        variant == ButtonVariant.ELEVATED ? ShadowPainter.shadowInsets(1) : new Insets(0, 0, 0, 0);
    assertThat(button.getShadowInsets())
        .as("%s reserves the halo its own elevation actually paints", variant)
        .isEqualTo(want);
  }

  @Test
  void aDisabledElevatedButtonPaintsNoShadowAndSoReservesNoHalo() {
    final ElwhaButton button = ElwhaButton.elevatedButton("Save");
    assertThat(button.getShadowInsets().top)
        .as("an enabled elevated button reserves a halo")
        .isPositive();

    button.setEnabled(false);

    assertThat(button.getShadowInsets())
        .as("disabling drops the shadow, so the reserve goes with it")
        .isEqualTo(new Insets(0, 0, 0, 0));
  }

  @Test
  void haloReserveIsBakedIntoPreferredSizeOnEveryEdge() {
    final ElwhaButton flat = new ElwhaButton().setVariant(ButtonVariant.FILLED);
    final ElwhaButton elevated = new ElwhaButton().setVariant(ButtonVariant.ELEVATED);
    final Insets halo = elevated.getShadowInsets();

    assertThat(elevated.getPreferredSize().width - flat.getPreferredSize().width)
        .as("the elevated button pads its body by the halo horizontally")
        .isEqualTo(halo.left + halo.right);
    assertThat(elevated.getPreferredSize().height)
        .as("and vertically, still floored at the touch target")
        .isEqualTo(
            Math.max(
                ButtonSize.S.containerHeightPx() + halo.top + halo.bottom,
                ButtonSize.S.minimumTargetPx()));
  }

  @Test
  void haloReserveIsHandedOutAsACopy() {
    final ElwhaButton button = ElwhaButton.elevatedButton("Save");
    final Insets first = button.getShadowInsets();

    first.top = 999;

    assertThat(button.getShadowInsets().top)
        .as("a caller mutating the returned insets cannot reach the button's reserve")
        .isEqualTo(ShadowPainter.shadowInsets(1).top);
  }

  // ------------------------------------------------------------ icon slot

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void aScalableIconIsRederivedToTheTiersIconSlot(final ButtonSize size) {
    final ElwhaButton button = new ElwhaButton("Save", MaterialIcons.add()).setButtonSize(size);

    assertThat(button.paintedIcon().getIconWidth())
        .as("%s paints its glyph at the slot the layout reserves", size)
        .isEqualTo(size.iconSizePx());
  }

  @Test
  void theIconLeadsTheLabelOnBothSidesOfTheOrientation() {
    final FixedIcon icon = new FixedIcon(18);
    final ElwhaButton button = new ElwhaButton("Save", icon);
    final Dimension pref = button.getPreferredSize();

    Pixels.render(button, pref.width, pref.height);
    final int ltrIconX = icon.paintedX;

    button.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    Pixels.render(button, pref.width, pref.height);

    assertThat(icon.paintedX)
        .as("the icon leads, so RTL moves it past the label to the trailing side (#565)")
        .isGreaterThan(ltrIconX);
    assertThat(icon.paintedX + 18)
        .as("and the pair still sits inside the same centered content block")
        .isLessThanOrEqualTo(pref.width);
  }

  @Test
  void theGetterHandsBackTheInstalledIconNotTheFilteredCopy() {
    final Icon installed = MaterialIcons.add();
    final ElwhaButton button = new ElwhaButton("Save", installed).setButtonSize(ButtonSize.XL);

    assertThat(button.getIcon())
        .as("the getter returns what the caller installed, reusable in another component (#664)")
        .isSameAs(installed);
    assertThat(button.paintedIcon())
        .as("while the painted glyph stays a private, filter-bound derivation")
        .isNotSameAs(installed);
  }

  @Test
  void switchingTierRescalesAnAlreadyInstalledIcon() {
    final ElwhaButton button = new ElwhaButton("Save", MaterialIcons.add());

    button.setButtonSize(ButtonSize.XL);

    assertThat(button.paintedIcon().getIconWidth())
        .as("the glyph follows the tier rather than staying at its install size")
        .isEqualTo(ButtonSize.XL.iconSizePx());
  }

  @Test
  void anUnscalableIconKeepsItsIntrinsicSize() {
    final ElwhaButton button =
        new ElwhaButton("Save", new FixedIcon(9)).setButtonSize(ButtonSize.L);

    assertThat(button.paintedIcon().getIconWidth())
        .as("a non-vector icon cannot be rescaled losslessly, so it is left alone")
        .isEqualTo(9);
  }

  @Test
  void installingASingleIconClearsAnyPreviousSelectedIcon() {
    final ElwhaButton button =
        new ElwhaButton().setIcons(MaterialIcons.star(), MaterialIcons.starFilled());
    assertThat(button.getSelectedIcon()).as("the pair installs both glyphs").isNotNull();

    button.setIcon(MaterialIcons.star());

    assertThat(button.getSelectedIcon())
        .as("setIcon is the single-glyph form and drops the selected half of the pair")
        .isNull();
  }

  // -------------------------------------------------- connected-segment mode

  @Test
  void connectedModePaintsTheBodyAcrossTheWholeComponentWidth() {
    final ElwhaButton button = new ElwhaButton().setButtonSize(ButtonSize.M);
    final int height = button.getPreferredSize().height;

    Pixels.assertPixelNear(
        Pixels.render(button, 200, height),
        190,
        height / 2,
        ColorRole.SURFACE.resolve(),
        "an ordinary button hugs its content and leaves the extra width empty");

    button.setCornerRadii(CornerRadii.uniform(4));

    Pixels.assertPixelNear(
        Pixels.render(button, 200, height),
        190,
        height / 2,
        ColorRole.PRIMARY.resolve(),
        "a connected segment fills the width its group gave it");
  }

  @Test
  void connectedModeStillReportsTheContentHuggingPreferredWidth() {
    final ElwhaButton button = new ElwhaButton().setButtonSize(ButtonSize.M);
    final Dimension hugging = button.getPreferredSize();

    button.setCornerRadii(CornerRadii.uniform(4));

    assertThat(button.getPreferredSize())
        .as("the group is free to size a segment larger, so preferred size stays content-hugging")
        .isEqualTo(hugging);
  }

  @Test
  void cornerRadiiOverrideRoundTripsAndClears() {
    final ElwhaButton button = new ElwhaButton();
    assertThat(button.getCornerRadii()).as("no override on a fresh button").isNull();

    final CornerRadii radii = CornerRadii.of(20, 4, 4, 20);
    button.setCornerRadii(radii);
    assertThat(button.getCornerRadii()).as("the override round-trips").isEqualTo(radii);

    button.setCornerRadii(null);
    assertThat(button.getCornerRadii())
        .as("clearing returns the button to shape-derived corners")
        .isNull();
  }

  // ----------------------------------------------------------- setter guards

  @Test
  void nullConfigurationIsIgnoredRatherThanApplied() {
    final ElwhaButton button =
        new ElwhaButton()
            .setVariant(ButtonVariant.OUTLINED)
            .setShape(ButtonShape.SQUARE)
            .setButtonSize(ButtonSize.L)
            .setInteractionMode(ButtonInteractionMode.SELECTABLE);

    button.setVariant(null).setShape(null).setButtonSize(null).setInteractionMode(null);

    assertThat(button.getVariant())
        .as("a null variant leaves the current one")
        .isEqualTo(ButtonVariant.OUTLINED);
    assertThat(button.getShape())
        .as("a null shape leaves the current one")
        .isEqualTo(ButtonShape.SQUARE);
    assertThat(button.getButtonSize())
        .as("a null size leaves the current one")
        .isEqualTo(ButtonSize.L);
    assertThat(button.getInteractionMode())
        .as("a null mode leaves the current one")
        .isEqualTo(ButtonInteractionMode.SELECTABLE);
  }

  @Test
  void borderWidthClampsAtZeroAndTextNeverBecomesNull() {
    final ElwhaButton button = new ElwhaButton("Save");

    button.setBorderWidth(-7);
    assertThat(button.getBorderWidth()).as("a negative stroke width clamps to zero").isZero();

    button.setText(null);
    assertThat(button.getText()).as("a null label reads back as the empty string").isEmpty();
  }

  @Test
  void everyMutatorReturnsTheButtonForChaining() {
    final ElwhaButton button = new ElwhaButton();

    final ElwhaButton chained =
        button
            .setVariant(ButtonVariant.OUTLINED)
            .setShape(ButtonShape.SQUARE)
            .setButtonSize(ButtonSize.M)
            .setText("Save")
            .setIcon(MaterialIcons.add())
            .setBorderWidth(2)
            .setSurfaceRole(ColorRole.TERTIARY)
            .setHovered(true)
            .setPressed(true)
            .setRippleEnabled(false)
            .setCornerRadii(CornerRadii.uniform(3));

    assertThat(chained).as("the fluent surface always hands back the same button").isSameAs(button);
  }
}
