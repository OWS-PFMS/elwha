package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the two M3 icon configurations and the handle sizing they drive — the part of
 * the §7 contract the {@code ElwhaSwitchInteractionTest} spike does not reach. Oracle: research §A
 * / §Mo as restated in the class Javadoc, and the shipped {@code ElwhaSwitchIconsSmoke} geometry.
 *
 * <p>Glyph presence is probed as a disc scan over the handle's centre rather than at a stroke
 * pixel: the glyph is a bundled Material Symbol whose exact paths are not this library's contract,
 * but <em>that a glyph in the right role is on the handle at all</em> is.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchIconsTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final int CENTER_Y = 20;

  /** Track interior when the unselected handle rides at 16px, handle when it rides at 24px. */
  private static final int SIZING_X = 9;

  /** The unselected handle's centre — travelStart. */
  private static final int OFF_HANDLE_X = 20;

  /** The selected handle's centre — travelEnd. */
  private static final int ON_HANDLE_X = 40;

  /** Radius of the glyph scan; stays inside even the 24px handle. */
  private static final int GLYPH_RADIUS = 6;

  private static BufferedImage render(final ElwhaSwitch toggle) {
    return Pixels.render(toggle, WIDTH, HEIGHT);
  }

  // -------------------------------------------------------------------- API

  @Test
  void iconsAreOffByDefaultButTheGlyphsExist() {
    final ElwhaSwitch toggle = new ElwhaSwitch();

    assertThat(toggle.isIconsVisible()).as("neither icon configuration is on by default").isFalse();
    assertThat(toggle.isShowOnlySelectedIcon()).as("nor the selected-only one").isFalse();
    assertThat(toggle.getSelectedIcon()).as("but the selected glyph slot is filled").isNotNull();
    assertThat(toggle.getUnselectedIcon()).as("and so is the unselected one").isNotNull();
  }

  @Test
  void bothIconSlotsAcceptAReplacementAndRestoreOnNull() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    final Icon custom = MaterialIcons.starFilled(ElwhaSwitch.ICON_SIZE_PX);

    toggle.setSelectedIcon(custom);
    assertThat(toggle.getSelectedIcon()).as("a custom selected glyph sticks").isSameAs(custom);
    toggle.setSelectedIcon(null);
    assertThat(toggle.getSelectedIcon())
        .as("and null restores the check default rather than clearing the slot")
        .isNotNull();
    assertThat(toggle.getSelectedIcon()).as("which is not the custom one").isNotSameAs(custom);

    toggle.setUnselectedIcon(custom);
    assertThat(toggle.getUnselectedIcon())
        .as("the unselected slot behaves the same")
        .isSameAs(custom);
    toggle.setUnselectedIcon(null);
    assertThat(toggle.getUnselectedIcon()).as("restoring the close default on null").isNotNull();
  }

  @Test
  void bothIconModesRoundTrip() {
    final ElwhaSwitch toggle = new ElwhaSwitch();

    toggle.setIconsVisible(true);
    assertThat(toggle.isIconsVisible()).as("the both-states mode round-trips").isTrue();
    toggle.setShowOnlySelectedIcon(true);
    assertThat(toggle.isShowOnlySelectedIcon()).as("and so does the selected-only mode").isTrue();
    assertThat(toggle.isIconsVisible())
        .as("the two flags are independent — setting one does not clear the other")
        .isTrue();
  }

  // --------------------------------------------------------- handle sizing

  @Test
  void bothIconsModeGrowsTheUnselectedHandle() {
    final ElwhaSwitch bare = new ElwhaSwitch();
    Pixels.assertPixelNear(
        render(bare),
        SIZING_X,
        CENTER_Y,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "with no icons the unselected handle rides at 16px and leaves this column to the track");

    final ElwhaSwitch withIcons = new ElwhaSwitch();
    withIcons.setIconsVisible(true);
    Pixels.assertPixelNear(
        render(withIcons),
        SIZING_X,
        CENTER_Y,
        ColorRole.OUTLINE.resolve(),
        "a handle whose icon shows rides at 24px, so the same column is now handle");
  }

  @Test
  void selectedOnlyModeWinsAndReturnsTheUnselectedHandleTo16px() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setIconsVisible(true);
    toggle.setShowOnlySelectedIcon(true);

    Pixels.assertPixelNear(
        render(toggle),
        SIZING_X,
        CENTER_Y,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "show-only-selected wins over icons-visible: the unselected handle drops back to 16px");
  }

  @Test
  void clearingSelectedOnlyHandsPrecedenceBackToBothIcons() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setIconsVisible(true);
    toggle.setShowOnlySelectedIcon(true);

    toggle.setShowOnlySelectedIcon(false);

    Pixels.assertPixelNear(
        render(toggle),
        SIZING_X,
        CENTER_Y,
        ColorRole.OUTLINE.resolve(),
        "the precedence is a live read of both flags, not a one-way latch");
  }

  // ---------------------------------------------------------------- glyphs

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void selectedHandleGlyphResolvesOnPrimaryContainer(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    toggle.setIconsVisible(true);

    Pixels.assertAnyPixelNear(
        render(toggle),
        ON_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        ColorRole.ON_PRIMARY_CONTAINER.resolve(),
        "the selected handle carries an ON_PRIMARY_CONTAINER glyph in " + mode);
  }

  @Test
  void unselectedHandleGlyphResolvesSurfaceContainerHighest() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setIconsVisible(true);

    Pixels.assertAnyPixelNear(
        render(toggle),
        OFF_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "the unselected handle carries a SURFACE_CONTAINER_HIGHEST glyph — the track's own colour,"
            + " punched through the OUTLINE handle");
  }

  @Test
  void selectedOnlyModeLeavesTheUnselectedHandleBare() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setShowOnlySelectedIcon(true);

    Pixels.assertNoPixelNear(
        render(toggle),
        OFF_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "in selected-only mode the unselected handle shows no glyph at all");
  }

  @Test
  void selectedOnlyModeStillPaintsTheSelectedGlyph() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    toggle.setShowOnlySelectedIcon(true);

    Pixels.assertAnyPixelNear(
        render(toggle),
        ON_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        ColorRole.ON_PRIMARY_CONTAINER.resolve(),
        "the selected side is exactly what selected-only mode keeps");
  }

  @Test
  void withNoIconModeOnNeitherHandleCarriesAGlyph() {
    Pixels.assertNoPixelNear(
        render(new ElwhaSwitch()),
        OFF_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "the default switch paints a bare unselected handle");
    Pixels.assertNoPixelNear(
        render(new ElwhaSwitch(true)),
        ON_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        ColorRole.ON_PRIMARY_CONTAINER.resolve(),
        "and a bare selected one");
  }

  @Test
  void aDisabledGlyphBlendsAtTheContentOpacity() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    toggle.setIconsVisible(true);
    toggle.setEnabled(false);

    Pixels.assertAnyPixelNear(
        render(toggle),
        ON_HANDLE_X,
        CENTER_Y,
        GLYPH_RADIUS,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.disabledContentOpacity()),
        "a disabled glyph is ON_SURFACE at 0.38 over the opaque SURFACE handle");
  }
}
