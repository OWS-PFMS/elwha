package com.owspfm.elwha.navrail;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of a disabled destination's content treatment. A disabled row already refuses
 * clicks and keystrokes, so the only thing left to get wrong is telling the user — and a row that
 * paints identically to its live neighbours reads as live. {@code ElwhaTab} sets the house rule
 * (content at {@link com.owspfm.elwha.theme.StateLayer#disabledContentOpacity()}); this pins the
 * rail to it.
 *
 * <p>The probe is the glyph, not the label: an icon has large solid interiors that reach the
 * resolved role exactly, while a small anti-aliased label's coverage depends on the font the host
 * happens to supply. The probe disc is inset well inside {@link
 * ElwhaNavRailDestination#getIconBounds()} so the assertion never depends on a single pixel.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaNavRailDestinationDisabledTest {

  /** Half the glyph box, so the disc stays inside the icon rather than reaching its edges. */
  private static final int PROBE_RADIUS = ElwhaNavRailDestination.ICON_SIZE_PX / 4;

  private static BufferedImage render(final ElwhaNavRailDestination destination) {
    final Dimension pref = destination.getPreferredSize();
    return Pixels.render(destination, pref.width, pref.height);
  }

  private static ElwhaNavRailDestination laidOut() {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");
    destination.setSize(destination.getPreferredSize());
    destination.doLayout();
    return destination;
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void anEnabledDestinationPaintsItsGlyphAtFullStrength(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaNavRailDestination destination = laidOut();
    final Rectangle icon = destination.getIconBounds();

    Pixels.assertAnyPixelNear(
        render(destination),
        icon.x + icon.width / 2,
        icon.y + icon.height / 2,
        PROBE_RADIUS,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "an unselected destination's glyph resolves to ON_SURFACE_VARIANT in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aDisabledDestinationDimsItsGlyphSoItReadsAsUnavailable(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaNavRailDestination destination = laidOut();
    destination.setEnabled(false);
    final Rectangle icon = destination.getIconBounds();

    Pixels.assertNoPixelNear(
        render(destination),
        icon.x + icon.width / 2,
        icon.y + icon.height / 2,
        PROBE_RADIUS,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "nothing in a disabled destination reaches the full-strength role in " + mode);
  }

  @Test
  void reEnablingRestoresTheFullStrengthGlyph() {
    final ElwhaNavRailDestination destination = laidOut();
    destination.setEnabled(false);
    destination.setEnabled(true);
    final Rectangle icon = destination.getIconBounds();

    Pixels.assertAnyPixelNear(
        render(destination),
        icon.x + icon.width / 2,
        icon.y + icon.height / 2,
        PROBE_RADIUS,
        ColorRole.ON_SURFACE_VARIANT.resolve(),
        "the dimming is a paint-time branch, not a latched color");
  }
}
