package com.owspfm.elwha.iconbutton;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.badge.IconBearing;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A geometry coverage for {@link ElwhaIconButton} — the M3 size table, the low-level container
 * / icon escape hatches, and the {@link IconBearing} contract that {@code ElwhaBadgeAnchor} places
 * against.
 *
 * <p>The {@code IconBearing} cases cross-check the advertised bounds against where the glyph is
 * actually asked to paint, using a recording {@link Icon} — the badge anchor's correctness depends
 * on those two agreeing, and no glyph pixel needs to be probed to prove it.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaIconButtonGeometryTest {

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

  // --------------------------------------------------------- size presets

  @ParameterizedTest
  @EnumSource(IconButtonSize.class)
  void everySizePresetDrivesBothDimensionsAtOnce(final IconButtonSize size) {
    final ElwhaIconButton button = new ElwhaIconButton().setButtonSize(size);

    assertThat(button.getContainerSize())
        .as("%s pins the container side", size)
        .isEqualTo(size.containerPx());
    assertThat(button.getIconSize()).as("%s pins the icon size too", size).isEqualTo(size.iconPx());
    assertThat(button.getPreferredSize())
        .as("%s reports a square footprint", size)
        .isEqualTo(new Dimension(size.containerPx(), size.containerPx()));
  }

  @ParameterizedTest
  @EnumSource(IconButtonSize.class)
  void sizeAndShapeStayIndependent(final IconButtonSize size) {
    final ElwhaIconButton button = new ElwhaIconButton().setButtonSize(size);

    assertThat(button.getShape())
        .as("picking %s leaves the corner treatment alone", size)
        .isEqualTo(ShapeScale.FULL);
  }

  @Test
  void footprintIsPinnedOnEveryAxisBecauseThereIsNoHaloToReserve() {
    final ElwhaIconButton button = new ElwhaIconButton().setButtonSize(IconButtonSize.L);

    assertThat(button.getMinimumSize())
        .as("an icon button never lays out smaller than its container")
        .isEqualTo(button.getPreferredSize());
    // Pinning the maximum is only safe because this primitive paints no elevation shadow and so
    // bakes no halo into its preferred size (the trap that bit the FAB in #199).
    assertThat(button.getMaximumSize())
        .as("and never larger — a shadow-free square is allowed to pin both bounds")
        .isEqualTo(button.getPreferredSize());
  }

  @Test
  void lowLevelSettersOverrideThePresetWithoutRewritingTheBookkeeping() {
    final ElwhaIconButton button = new ElwhaIconButton().setButtonSize(IconButtonSize.S);

    button.setContainerSize(77).setIconSize(33);

    assertThat(button.getContainerSize())
        .as("the raw container side wins, last write")
        .isEqualTo(77);
    assertThat(button.getIconSize()).as("as does the raw icon size").isEqualTo(33);
    assertThat(button.getButtonSize())
        .as("but the preset enum is bookkeeping and still reports the last preset selected")
        .isEqualTo(IconButtonSize.S);
  }

  @Test
  void aLaterPresetOverwritesTheRawSetters() {
    final ElwhaIconButton button = new ElwhaIconButton().setContainerSize(77).setIconSize(33);

    button.setButtonSize(IconButtonSize.XL);

    assertThat(button.getContainerSize())
        .as("last write wins in the other direction too")
        .isEqualTo(IconButtonSize.XL.containerPx());
    assertThat(button.getIconSize())
        .as("for both dimensions")
        .isEqualTo(IconButtonSize.XL.iconPx());
  }

  @Test
  void dimensionsClampAtOnePixel() {
    final ElwhaIconButton button = new ElwhaIconButton().setContainerSize(-5).setIconSize(0);

    assertThat(button.getContainerSize()).as("a container never collapses to zero").isEqualTo(1);
    assertThat(button.getIconSize()).as("nor does the icon").isEqualTo(1);
  }

  @Test
  void borderWidthClampsAtZero() {
    assertThat(new ElwhaIconButton().setBorderWidth(-3).getBorderWidth())
        .as("a negative stroke width clamps to zero")
        .isZero();
  }

  @Test
  void nullConfigurationIsIgnoredRatherThanApplied() {
    final ElwhaIconButton button =
        new ElwhaIconButton()
            .setVariant(IconButtonVariant.OUTLINED)
            .setShape(ShapeScale.SM)
            .setButtonSize(IconButtonSize.L)
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE);

    button.setVariant(null).setShape(null).setButtonSize(null).setInteractionMode(null);

    assertThat(button.getVariant())
        .as("a null variant is ignored")
        .isEqualTo(IconButtonVariant.OUTLINED);
    assertThat(button.getShape()).as("a null shape is ignored").isEqualTo(ShapeScale.SM);
    assertThat(button.getButtonSize()).as("a null size is ignored").isEqualTo(IconButtonSize.L);
    assertThat(button.getInteractionMode())
        .as("a null mode is ignored")
        .isEqualTo(IconButtonInteractionMode.SELECTABLE);
  }

  // ---------------------------------------------------------- icon scaling

  @ParameterizedTest
  @EnumSource(IconButtonSize.class)
  void aScalableGlyphIsRederivedToTheTiersIconSize(final IconButtonSize size) {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add()).setButtonSize(size);

    assertThat(button.getIcon().getIconWidth())
        .as("%s paints its glyph at the size the layout reserves", size)
        .isEqualTo(size.iconPx());
  }

  @Test
  void anUnscalableGlyphKeepsItsIntrinsicSize() {
    final ElwhaIconButton button =
        new ElwhaIconButton(new RecordingIcon(9)).setButtonSize(IconButtonSize.XL);

    assertThat(button.getIcon().getIconWidth())
        .as("a non-vector icon cannot be rescaled losslessly, so it is left alone")
        .isEqualTo(9);
  }

  @Test
  void installingASingleIconClearsAnyPreviousSelectedIcon() {
    final ElwhaIconButton button =
        new ElwhaIconButton().setIcons(MaterialIcons.pushPin(), MaterialIcons.pushPinFilled());
    assertThat(button.getSelectedIcon()).as("the pair installs both glyphs").isNotNull();

    button.setIcon(MaterialIcons.pushPin());

    assertThat(button.getSelectedIcon())
        .as("setIcon is the single-glyph form and drops the selected half of the pair")
        .isNull();
  }

  // ------------------------------------------------------------ IconBearing

  @Test
  void anIconButtonConformsToThePlacementContract() {
    assertThat(new ElwhaIconButton())
        .as("badges anchor against this contract")
        .isInstanceOf(IconBearing.class);
  }

  @Test
  void anEmptyIconButtonAdvertisesAnEmptyIconBox() {
    assertThat(new ElwhaIconButton().getIconBounds().isEmpty())
        .as("with no glyph installed there is nothing to anchor to")
        .isTrue();
  }

  @ParameterizedTest
  @EnumSource(IconButtonSize.class)
  void advertisedIconBoxIsCenteredAndTierSized(final IconButtonSize size) {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add()).setButtonSize(size);
    button.setSize(size.containerPx(), size.containerPx());

    final Rectangle bounds = button.getIconBounds();

    assertThat(bounds.width)
        .as("%s advertises the tier's icon size", size)
        .isEqualTo(size.iconPx());
    assertThat(bounds.x)
        .as("%s centers the glyph horizontally", size)
        .isEqualTo((size.containerPx() - size.iconPx()) / 2);
    assertThat(bounds.y)
        .as("%s centers the glyph vertically", size)
        .isEqualTo((size.containerPx() - size.iconPx()) / 2);
  }

  @Test
  void advertisedIconBoxIsWhereTheGlyphActuallyPaints() {
    final RecordingIcon icon = new RecordingIcon(18);
    final ElwhaIconButton button = new ElwhaIconButton(icon).setButtonSize(IconButtonSize.XL);

    Pixels.render(button, IconButtonSize.XL.containerPx(), IconButtonSize.XL.containerPx());

    assertThat(icon.painted)
        .as("the badge anchor's placement target matches the glyph's real paint rect")
        .isEqualTo(button.getIconBounds());
  }

  @Test
  void advertisedIconBoxFollowsTheToggleSwap() {
    final RecordingIcon resting = new RecordingIcon(18);
    final RecordingIcon selected = new RecordingIcon(30);
    final ElwhaIconButton button =
        new ElwhaIconButton()
            .setIcons(resting, selected)
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE)
            .setButtonSize(IconButtonSize.XL);
    button.setSize(IconButtonSize.XL.containerPx(), IconButtonSize.XL.containerPx());

    assertThat(button.getIconBounds().width)
        .as("the resting glyph defines the box while unselected")
        .isEqualTo(18);

    button.setSelected(true);

    assertThat(button.getIconBounds().width)
        .as("and the selected glyph takes over once toggled — a badge must follow it")
        .isEqualTo(30);
  }

  // ------------------------------------------------------- corner override

  @Test
  void cornerRadiiOverrideRoundTripsAndClears() {
    final ElwhaIconButton button = new ElwhaIconButton();
    assertThat(button.getCornerRadii()).as("no override on a fresh button").isNull();

    final CornerRadii radii = CornerRadii.of(20, 4, 4, 20);
    button.setCornerRadii(radii);
    assertThat(button.getCornerRadii()).as("the override round-trips").isEqualTo(radii);

    button.setCornerRadii(null);
    assertThat(button.getCornerRadii())
        .as("clearing returns the button to shape-derived corners")
        .isNull();
  }

  @Test
  void everyMutatorReturnsTheButtonForChaining() {
    final ElwhaIconButton button = new ElwhaIconButton();

    final ElwhaIconButton chained =
        button
            .setVariant(IconButtonVariant.OUTLINED)
            .setShape(ShapeScale.SM)
            .setButtonSize(IconButtonSize.L)
            .setIcons(MaterialIcons.pushPin(), MaterialIcons.pushPinFilled())
            .setContainerSize(50)
            .setIconSize(20)
            .setBorderWidth(2)
            .setHovered(true)
            .setPressed(true)
            .setSelected(true)
            .setRippleEnabled(false)
            .setCornerRadii(CornerRadii.uniform(3));

    assertThat(chained).as("the fluent surface always hands back the same button").isSameAs(button);
  }
}
