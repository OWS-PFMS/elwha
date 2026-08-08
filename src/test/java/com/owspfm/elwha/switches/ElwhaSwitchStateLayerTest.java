package com.owspfm.elwha.switches;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the switch's one-layer-at-a-time state policy (#630). The halo used to
 * composite hover, focus and press as three sequential fills, which lands on a colour no single
 * state produces and which no other selection control paints — {@link
 * com.owspfm.elwha.checkbox.ElwhaCheckbox} and {@link com.owspfm.elwha.radio.ElwhaRadioButton} pick
 * exactly one. A stacked implementation still looks plausible in isolation, so the priority order
 * is pinned here against the exact single-state blend.
 *
 * <p>The focus half of #630 — that the treatment arms on keyboard traversal and not on a click —
 * needs a real {@code FocusEvent} cause and lives in the {@code gui} tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchStateLayerTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final int CENTER_Y = 20;

  /** Inside the halo circle riding the unselected handle, clear of the handle itself. */
  private static final int HALO_X = 20;

  private static final int HALO_Y = CENTER_Y - 18;

  /** A ground no bundled palette carries, so every translucent blend is unambiguous. */
  private static final Color GROUND = Color.MAGENTA;

  private static BufferedImage render(final ElwhaSwitch toggle) {
    return Pixels.render(toggle, WIDTH, HEIGHT, GROUND);
  }

  /** The halo tint an unselected, enabled switch uses. */
  private static Color haloOver(final StateLayer layer) {
    return Pixels.mix(GROUND, ColorRole.ON_SURFACE.resolve(), layer.opacity());
  }

  @Test
  void hoverAlonePaintsExactlyTheHoverLayer() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setHovered(true);

    Pixels.assertPixelNear(
        render(toggle), HALO_X, HALO_Y, haloOver(StateLayer.HOVER), "hover paints its own layer");
  }

  @Test
  void pressWinsOverHover() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setHovered(true);
    toggle.setPressed(true);

    Pixels.assertPixelNear(
        render(toggle),
        HALO_X,
        HALO_Y,
        haloOver(StateLayer.PRESSED),
        "#630 — exactly one layer paints, the highest-priority one; the two used to composite as"
            + " sequential fills, landing on a colour neither state produces");
  }

  @Test
  void aRestingSwitchPaintsNoHaloAtAll() {
    Pixels.assertPixelNear(
        render(new ElwhaSwitch()),
        HALO_X,
        HALO_Y,
        GROUND,
        "with no state live the halo is absent, not a zero-opacity fill");
  }
}
