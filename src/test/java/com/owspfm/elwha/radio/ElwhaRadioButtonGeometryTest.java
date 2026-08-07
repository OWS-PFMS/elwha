package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the block model the radio shares with {@code ElwhaCheckbox} — a leading 48px
 * touch-target block holding the icon, widened by an optional label, with the label's baseline
 * exported. The mirroring probes run label-less so no glyph pixels land where they look.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioButtonGeometryTest {

  private static final int TOUCH_TARGET = 48;
  private static final int LABEL_TRAILING_PAD = 4;
  private static final int WIDE = 160;
  private static final String LABEL = "Weekly digest";
  private static final Color GROUND = Color.MAGENTA;

  private static FontMetrics labelMetrics(final ElwhaRadioButton radio) {
    return radio.getFontMetrics(TypeRole.BODY_MEDIUM.resolve());
  }

  // ------------------------------------------------------------ preferred size

  @Test
  void aLabellessRadioIsTheTouchTargetSquare() {
    assertThat(new ElwhaRadioButton().getPreferredSize())
        .as("the 48px block is the whole component when there is no label")
        .isEqualTo(new Dimension(TOUCH_TARGET, TOUCH_TARGET));
  }

  @Test
  void aLabelWidensThePreferredSizeByItsTextPlusTheTrailingPad() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(LABEL);
    final FontMetrics metrics = labelMetrics(radio);

    assertThat(radio.getPreferredSize())
        .as("the label starts at the touch-target edge and adds a 4px trailing pad")
        .isEqualTo(
            new Dimension(
                TOUCH_TARGET + metrics.stringWidth(LABEL) + LABEL_TRAILING_PAD,
                Math.max(TOUCH_TARGET, metrics.getHeight())));
  }

  @Test
  void clearingTheLabelReturnsThePreferredSizeToTheSquare() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(LABEL);
    radio.setLabel(null);

    assertThat(radio.getPreferredSize())
        .as("the preferred size follows the label rather than latching its widest value")
        .isEqualTo(new Dimension(TOUCH_TARGET, TOUCH_TARGET));
  }

  @Test
  void anExplicitPreferredSizeWins() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(LABEL);
    radio.setPreferredSize(new Dimension(200, 60));

    assertThat(radio.getPreferredSize())
        .as("a caller-set preferred size is honored over the computed one")
        .isEqualTo(new Dimension(200, 60));
  }

  @Test
  void minimumSizeTracksPreferredSize() {
    final ElwhaRadioButton labelless = new ElwhaRadioButton();
    assertThat(labelless.getMinimumSize())
        .as("a radio does not shrink below its preferred size")
        .isEqualTo(labelless.getPreferredSize());

    final ElwhaRadioButton labeled = new ElwhaRadioButton(LABEL);
    assertThat(labeled.getMinimumSize())
        .as("including once a label has widened it")
        .isEqualTo(labeled.getPreferredSize());
  }

  @Test
  void anExplicitMinimumSizeWins() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(LABEL);
    radio.setMinimumSize(new Dimension(10, 10));

    assertThat(radio.getMinimumSize())
        .as("a caller-set minimum size is honored over the computed one")
        .isEqualTo(new Dimension(10, 10));
  }

  // ---------------------------------------------------------------- baseline

  @Test
  void aLabelledRadioExportsItsTextBaseline() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(LABEL);
    final FontMetrics metrics = labelMetrics(radio);
    final int height = radio.getPreferredSize().height;
    final int centre = height / 2;

    assertThat(radio.getBaseline(radio.getPreferredSize().width, height))
        .as("the baseline centres the text line box on the icon centre")
        .isEqualTo(centre - (metrics.getAscent() + metrics.getDescent()) / 2 + metrics.getAscent());
  }

  @Test
  void aLabellessRadioHasNoBaseline() {
    assertThat(new ElwhaRadioButton().getBaseline(TOUCH_TARGET, TOUCH_TARGET))
        .as("with no text there is nothing to align a form row against")
        .isEqualTo(-1);
  }

  @Test
  void baselineResizeBehaviorFollowsTheLabel() {
    assertThat(new ElwhaRadioButton(LABEL).getBaselineResizeBehavior())
        .as("the label rides the vertical centre, so the baseline moves with it")
        .isEqualTo(Component.BaselineResizeBehavior.CENTER_OFFSET);
    assertThat(new ElwhaRadioButton().getBaselineResizeBehavior())
        .as("and a label-less radio reports no baseline behavior at all")
        .isEqualTo(Component.BaselineResizeBehavior.OTHER);
  }

  // ---------------------------------------------------------- icon placement

  @Test
  void iconCentreSitsInTheLeadingBlock() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setSize(WIDE, TOUCH_TARGET);

    assertThat(radio.iconCenterX())
        .as("the icon centres in the leading 48px block regardless of how wide the component is")
        .isEqualTo(TOUCH_TARGET / 2);
    assertThat(radio.iconCenterY())
        .as("and rides the component's vertical centre")
        .isEqualTo(TOUCH_TARGET / 2);
  }

  @Test
  void rightToLeftMovesTheIconCentreToTheTrailingBlock() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setSize(WIDE, TOUCH_TARGET);
    radio.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    assertThat(radio.iconCenterX())
        .as("a right-to-left orientation mirrors the block to the far edge")
        .isEqualTo(WIDE - TOUCH_TARGET / 2);
  }

  @Test
  void rightToLeftPaintsTheIconAtTheTrailingEdge() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    radio.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    final BufferedImage image = Pixels.render(radio, WIDE, TOUCH_TARGET, GROUND);

    Pixels.assertPixelNear(
        image,
        WIDE - TOUCH_TARGET / 2,
        TOUCH_TARGET / 2,
        ColorRole.PRIMARY.resolve(),
        "the mirrored layout is what actually paints, not just what the accessor reports");
    Pixels.assertPixelNear(
        image,
        TOUCH_TARGET / 2,
        TOUCH_TARGET / 2,
        GROUND,
        "and the leading block it vacated is bare");
  }

  @Test
  void leftToRightPaintsTheIconAtTheLeadingEdge() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);

    final BufferedImage image = Pixels.render(radio, WIDE, TOUCH_TARGET, GROUND);

    Pixels.assertPixelNear(
        image,
        TOUCH_TARGET / 2,
        TOUCH_TARGET / 2,
        ColorRole.PRIMARY.resolve(),
        "the default orientation leads with the icon");
    Pixels.assertPixelNear(
        image,
        WIDE - TOUCH_TARGET / 2,
        TOUCH_TARGET / 2,
        GROUND,
        "leaving the trailing edge empty");
  }
}
