package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the sizing-hook escape doctrine (#567) on {@link ColorTrackSlider} — the one
 * swept site that is package-private, so it cannot join the library-wide sweep in {@code
 * com.owspfm.elwha.SizingHookEscapeTest}.
 *
 * <p>All three hooks are overridden here, which makes it the only site in the sweep where the
 * maximum-size escape is load-bearing on a track whose height is otherwise pinned.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/567">#567</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ColorTrackSliderSizingTest {

  private static final Dimension EXPLICIT = new Dimension(321, 177);

  private static ColorTrackSlider slider() {
    return new ColorTrackSlider(0, 255, 128);
  }

  @Test
  void anExplicitPreferredSizeWins() {
    final ColorTrackSlider track = slider();
    final Dimension natural = track.getPreferredSize();

    track.setPreferredSize(EXPLICIT);

    assertThat(track.getPreferredSize())
        .as("the track discards a caller-set preferred size (natural was %s)", natural)
        .isEqualTo(EXPLICIT);
  }

  @Test
  void anExplicitMinimumSizeWins() {
    final ColorTrackSlider track = slider();

    track.setMinimumSize(EXPLICIT);

    assertThat(track.getMinimumSize()).isEqualTo(EXPLICIT);
  }

  @Test
  void anExplicitMaximumSizeWins() {
    final ColorTrackSlider track = slider();

    track.setMaximumSize(EXPLICIT);

    assertThat(track.getMaximumSize()).isEqualTo(EXPLICIT);
  }

  @Test
  void anUnsetTrackKeepsItsOwnGeometry() {
    final ColorTrackSlider track = slider();

    assertThat(track.getPreferredSize().height)
        .as("the natural height stays the component-height token")
        .isEqualTo(ColorTrackSlider.COMPONENT_HEIGHT);
    assertThat(track.getMaximumSize().width)
        .as("and the track still stretches horizontally when nothing is set")
        .isEqualTo(Integer.MAX_VALUE);
  }
}
