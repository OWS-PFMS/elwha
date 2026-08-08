package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the disabled treatment, whose selected side is the switch's one deliberate
 * asymmetry (research §T ⚠): the disabled <em>unselected</em> handle is {@code ON_SURFACE} at the
 * 0.38 content opacity, but the disabled <em>selected</em> handle is <strong>opaque</strong> {@code
 * SURFACE} — a solid hole punched through the 12% track rather than another translucent layer. An
 * implementation that "tidied" the two sides into one rule would still look plausible, so both are
 * pinned here.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchDisabledPaintTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final int CENTER_Y = 20;

  /** A ground no bundled palette carries, so every translucent blend is unambiguous. */
  private static final Color GROUND = Color.MAGENTA;

  /** Track interior, clear of the unselected 16px handle and of the selected one. */
  private static final int TRACK_X = 9;

  /** On the 2px outline ring at the track's left edge. */
  private static final int RING_X = 5;

  private static final int OFF_HANDLE_X = 20;
  private static final int ON_HANDLE_X = 40;

  private static BufferedImage render(final ElwhaSwitch toggle) {
    return Pixels.render(toggle, WIDTH, HEIGHT, GROUND);
  }

  private static ElwhaSwitch disabled(final boolean selected) {
    final ElwhaSwitch toggle = new ElwhaSwitch(selected);
    toggle.setEnabled(false);
    return toggle;
  }

  private static Color over(final Color base, final ColorRole role, final float opacity) {
    return Pixels.mix(base, role.resolve(), opacity);
  }

  // --------------------------------------------------------- enabled baseline

  @Test
  void anEnabledSwitchPaintsItsRolesOpaque() {
    final BufferedImage off = render(new ElwhaSwitch());
    Pixels.assertPixelNear(
        off,
        TRACK_X,
        CENTER_Y,
        ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(),
        "the enabled unselected track is a plain SURFACE_CONTAINER_HIGHEST fill");
    Pixels.assertPixelNear(
        off, RING_X, CENTER_Y, ColorRole.OUTLINE.resolve(), "inside an opaque OUTLINE ring");
    Pixels.assertPixelNear(
        off,
        OFF_HANDLE_X,
        CENTER_Y,
        ColorRole.OUTLINE.resolve(),
        "and the resting handle is OUTLINE too");

    final BufferedImage on = render(new ElwhaSwitch(true));
    Pixels.assertPixelNear(
        on,
        TRACK_X,
        CENTER_Y,
        ColorRole.PRIMARY.resolve(),
        "the enabled selected track is PRIMARY");
    Pixels.assertPixelNear(
        on,
        ON_HANDLE_X,
        CENTER_Y,
        ColorRole.ON_PRIMARY.resolve(),
        "with an ON_PRIMARY handle riding it");
  }

  // ------------------------------------------------------- disabled selected

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aDisabledSelectedTrackDimsToTheContainerOpacity(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(disabled(true)),
        TRACK_X,
        CENTER_Y,
        over(GROUND, ColorRole.ON_SURFACE, StateLayer.disabledContainerOpacity()),
        "the disabled selected track is ON_SURFACE at the 0.12 container opacity in " + mode);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aDisabledSelectedHandleStaysOpaqueSurface(final Mode mode) {
    ThemeExtension.install(mode);

    Pixels.assertPixelNear(
        render(disabled(true)),
        ON_HANDLE_X,
        CENTER_Y,
        ColorRole.SURFACE.resolve(),
        "the disabled selected handle is opaque SURFACE — the spec's deliberate asymmetry, not"
            + " another translucent layer, in "
            + mode);
  }

  @Test
  void aDisabledSelectedSwitchLosesItsOutlineRing() {
    Pixels.assertPixelNear(
        render(disabled(true)),
        RING_X,
        CENTER_Y,
        over(GROUND, ColorRole.ON_SURFACE, StateLayer.disabledContainerOpacity()),
        "the selected track has no outline to dim — the ring belongs to the unselected side only");
  }

  // ----------------------------------------------------- disabled unselected

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void aDisabledUnselectedTrackDimsBothItsFillAndItsRing(final Mode mode) {
    ThemeExtension.install(mode);
    final BufferedImage image = render(disabled(false));

    Pixels.assertPixelNear(
        image,
        TRACK_X,
        CENTER_Y,
        over(GROUND, ColorRole.SURFACE_CONTAINER_HIGHEST, StateLayer.disabledContainerOpacity()),
        "the disabled unselected fill is SURFACE_CONTAINER_HIGHEST at 0.12 in " + mode);
    Pixels.assertPixelNear(
        image,
        RING_X,
        CENTER_Y,
        over(GROUND, ColorRole.ON_SURFACE, StateLayer.disabledContainerOpacity()),
        "and its ring is ON_SURFACE at 0.12 — painted as a filled band so the two never"
            + " double-blend, in "
            + mode);
  }

  @Test
  void aDisabledUnselectedHandleDimsToTheContentOpacity() {
    final Color trackUnderneath =
        over(GROUND, ColorRole.SURFACE_CONTAINER_HIGHEST, StateLayer.disabledContainerOpacity());

    Pixels.assertPixelNear(
        render(disabled(false)),
        OFF_HANDLE_X,
        CENTER_Y,
        over(trackUnderneath, ColorRole.ON_SURFACE, StateLayer.disabledContentOpacity()),
        "the disabled unselected handle is ON_SURFACE at the 0.38 content opacity, composited over"
            + " the dimmed track it rides");
  }

  // ------------------------------------------------------------ suppression

  @Test
  void aDisabledSwitchPaintsNoStateLayerEvenWhenForced() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setEnabled(false);
    toggle.setHovered(true);
    toggle.setPressed(true);

    Pixels.assertPixelNear(
        render(toggle),
        OFF_HANDLE_X,
        CENTER_Y - 18,
        GROUND,
        "the halo above the handle stays bare — a disabled switch paints no interaction layer"
            + " whatever the flags say");
  }

  @Test
  void reEnablingRestoresTheOpaqueRoles() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    toggle.setEnabled(false);
    toggle.setEnabled(true);

    Pixels.assertPixelNear(
        render(toggle),
        TRACK_X,
        CENTER_Y,
        ColorRole.PRIMARY.resolve(),
        "the disabled treatment is a paint-time branch, not a latched state");
  }

  // ------------------------------------------------------- transient teardown

  @Test
  void disablingDropsThePointerCursorTheHoverInstalled() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setSize(WIDTH, HEIGHT);
    Input.enter(toggle, WIDTH / 2, CENTER_Y);

    assertThat(toggle.getCursor().getType())
        .as("hovering an enabled switch advertises it as clickable")
        .isEqualTo(Cursor.HAND_CURSOR);

    toggle.setEnabled(false);

    assertThat(toggle.getCursor().getType())
        .as(
            "nothing on the exit path checks isEnabled, so the disable has to take the cursor back"
                + " itself")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @Test
  void aSwitchDisabledWhileHoveredComesBackAtRest() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setSize(WIDTH, HEIGHT);
    Input.enter(toggle, WIDTH / 2, CENTER_Y);
    Input.press(toggle, WIDTH / 2, CENTER_Y);

    toggle.setEnabled(false);
    toggle.setEnabled(true);

    Pixels.assertPixelNear(
        render(toggle),
        OFF_HANDLE_X,
        CENTER_Y - 18,
        GROUND,
        "re-enabling without moving the pointer must not replay the hover halo the disable"
            + " should have dropped");
  }

  @Test
  void aSwitchDisabledMidDragDoesNotFreezeItsHandleWhereThePointerLeftIt() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    toggle.setSize(WIDTH, HEIGHT);
    Input.press(toggle, OFF_HANDLE_X, CENTER_Y);
    Input.drag(toggle, ON_HANDLE_X, CENTER_Y);

    toggle.setEnabled(false);
    toggle.setEnabled(true);

    Pixels.assertPixelNear(
        render(toggle),
        OFF_HANDLE_X,
        CENTER_Y,
        ColorRole.OUTLINE.resolve(),
        "handleFraction follows dragFraction for as long as `dragging` holds, so an abandoned drag"
            + " would otherwise strand the handle mid-track for good");
  }
}
