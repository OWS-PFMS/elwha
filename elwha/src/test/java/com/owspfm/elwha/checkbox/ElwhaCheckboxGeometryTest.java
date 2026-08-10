package com.owspfm.elwha.checkbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the block model the design doc §5 fixes as component constants: a 48px
 * touch-target square, widened by the label and its trailing pad, with the label's baseline
 * exported so a form row can align it. The right-to-left case is checked by where the container
 * actually paints, not by reading back a field.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCheckboxGeometryTest {

  private static final int TOUCH_TARGET = 48;
  private static final int LABEL_TRAILING_PAD = 4;
  private static final String LABEL = "Remember me";

  private static FontMetrics labelMetrics(final ElwhaCheckbox box) {
    return box.getFontMetrics(TypeRole.BODY_MEDIUM.resolve());
  }

  // ------------------------------------------------------------ preferred size

  @Test
  void aLabellessCheckboxIsTheTouchTargetSquare() {
    assertThat(new ElwhaCheckbox().getPreferredSize())
        .as("48x48 is the M3 touch target and the whole component when there is no label")
        .isEqualTo(new Dimension(TOUCH_TARGET, TOUCH_TARGET));
  }

  @Test
  void aLabelWidensThePreferredSizeByItsTextPlusTheTrailingPad() {
    final ElwhaCheckbox box = new ElwhaCheckbox(LABEL);
    final FontMetrics metrics = labelMetrics(box);

    assertThat(box.getPreferredSize())
        .as("the label starts at the touch-target edge and adds a 4px trailing pad")
        .isEqualTo(
            new Dimension(
                TOUCH_TARGET + metrics.stringWidth(LABEL) + LABEL_TRAILING_PAD,
                Math.max(TOUCH_TARGET, metrics.getHeight())));
  }

  @Test
  void aTallerLabelWinsOverTheTouchTargetHeight() {
    final ElwhaCheckbox box = new ElwhaCheckbox(LABEL);
    final FontMetrics metrics = labelMetrics(box);

    assertThat(box.getPreferredSize().height)
        .as("the height is the larger of the touch target and the text line box")
        .isEqualTo(Math.max(TOUCH_TARGET, metrics.getHeight()));
  }

  @Test
  void clearingTheLabelReturnsThePreferredSizeToTheSquare() {
    final ElwhaCheckbox box = new ElwhaCheckbox(LABEL);
    box.setLabel(null);

    assertThat(box.getPreferredSize())
        .as("the preferred size follows the label rather than latching its widest value")
        .isEqualTo(new Dimension(TOUCH_TARGET, TOUCH_TARGET));
  }

  @Test
  void anExplicitPreferredSizeWins() {
    final ElwhaCheckbox box = new ElwhaCheckbox(LABEL);
    box.setPreferredSize(new Dimension(200, 60));

    assertThat(box.getPreferredSize())
        .as("a caller-set preferred size is honored over the computed one")
        .isEqualTo(new Dimension(200, 60));
  }

  // ------------------------------------------------------------ minimum size

  @Test
  void minimumSizeTracksPreferredSize() {
    final ElwhaCheckbox labelless = new ElwhaCheckbox();
    assertThat(labelless.getMinimumSize())
        .as("a checkbox does not shrink below its preferred size")
        .isEqualTo(labelless.getPreferredSize());

    final ElwhaCheckbox labeled = new ElwhaCheckbox(LABEL);
    assertThat(labeled.getMinimumSize())
        .as("including once a label has widened it")
        .isEqualTo(labeled.getPreferredSize());
  }

  @Test
  void anExplicitMinimumSizeWins() {
    final ElwhaCheckbox box = new ElwhaCheckbox(LABEL);
    box.setMinimumSize(new Dimension(10, 10));

    assertThat(box.getMinimumSize())
        .as("a caller-set minimum size is honored over the computed one")
        .isEqualTo(new Dimension(10, 10));
  }

  // ---------------------------------------------------------------- baseline

  @Test
  void aLabelledCheckboxExportsItsTextBaseline() {
    final ElwhaCheckbox box = new ElwhaCheckbox(LABEL);
    final FontMetrics metrics = labelMetrics(box);
    final int height = box.getPreferredSize().height;
    final int centre = height / 2;

    assertThat(box.getBaseline(box.getPreferredSize().width, height))
        .as("the baseline centres the text line box on the container centre")
        .isEqualTo(centre - (metrics.getAscent() + metrics.getDescent()) / 2 + metrics.getAscent());
  }

  @Test
  void aLabellessCheckboxHasNoBaseline() {
    assertThat(new ElwhaCheckbox().getBaseline(TOUCH_TARGET, TOUCH_TARGET))
        .as("with no text there is nothing to align a form row against")
        .isEqualTo(-1);
  }

  @Test
  void baselineResizeBehaviorFollowsTheLabel() {
    assertThat(new ElwhaCheckbox(LABEL).getBaselineResizeBehavior())
        .as("the label rides the vertical centre, so the baseline moves with it")
        .isEqualTo(Component.BaselineResizeBehavior.CENTER_OFFSET);
    assertThat(new ElwhaCheckbox().getBaselineResizeBehavior())
        .as("and a label-less box reports no baseline behavior at all")
        .isEqualTo(Component.BaselineResizeBehavior.OTHER);
  }

  // --------------------------------------------------------------------- RTL

  private static final int WIDE = 160;

  /** Offsets from a container's centre into its upper-left quadrant, clear of the checkmark. */
  private static final int QUADRANT_DX = -6;

  private static final int QUADRANT_DY = -7;

  /**
   * The mirroring probes run label-less on purpose: the container block is the thing that moves,
   * and a label would put glyph pixels in the trailing half where the probes look.
   */
  private static BufferedImage renderWide(final ComponentOrientation orientation) {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(CheckState.CHECKED);
    box.setComponentOrientation(orientation);
    return Pixels.render(box, WIDE, TOUCH_TARGET);
  }

  @Test
  void rightToLeftMirrorsTheContainerToTheTrailingEdge() {
    final BufferedImage image = renderWide(ComponentOrientation.RIGHT_TO_LEFT);
    final int trailing = WIDE - TOUCH_TARGET / 2;

    Pixels.assertPixelNear(
        image,
        trailing + QUADRANT_DX,
        TOUCH_TARGET / 2 + QUADRANT_DY,
        ColorRole.PRIMARY.resolve(),
        "under a right-to-left orientation the container block sits at the trailing edge");
    Pixels.assertPixelNear(
        image,
        TOUCH_TARGET / 2 + QUADRANT_DX,
        TOUCH_TARGET / 2 + QUADRANT_DY,
        ColorRole.SURFACE.resolve(),
        "and the leading edge it vacated is bare");
  }

  @Test
  void leftToRightKeepsTheContainerAtTheLeadingEdge() {
    final BufferedImage image = renderWide(ComponentOrientation.LEFT_TO_RIGHT);
    final int trailing = WIDE - TOUCH_TARGET / 2;

    Pixels.assertPixelNear(
        image,
        TOUCH_TARGET / 2 + QUADRANT_DX,
        TOUCH_TARGET / 2 + QUADRANT_DY,
        ColorRole.PRIMARY.resolve(),
        "the default orientation leads with the container");
    Pixels.assertPixelNear(
        image,
        trailing + QUADRANT_DX,
        TOUCH_TARGET / 2 + QUADRANT_DY,
        ColorRole.SURFACE.resolve(),
        "leaving the trailing edge empty");
  }
}
