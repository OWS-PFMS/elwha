package com.owspfm.elwha.switches;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Headless S2 guard (story #403) — synthesises press/drag/release gestures on an offscreen {@link
 * ElwhaSwitch} and asserts the §7 interaction contract: click toggles and fires {@code
 * ActionListener} + {@code ChangeListener}; programmatic {@code setSelected} fires only the change
 * listener; a release outside the bounds cancels; a drag commits to the nearest half (including a
 * scrub that lands back where it started — no events); Space press/release toggles via the bound
 * actions; a disabled switch ignores everything. Pixel checks cover the hover layer, the
 * hover/press handle-role shifts, and the pressed 28px handle growth.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchInteractionSmoke {

  private static final int W = 60;
  private static final int H = 40;
  private static final int CY = 20;

  private ElwhaSwitchInteractionSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkClickSemantics();
    checkDragSemantics();
    checkKeyboardSemantics();
    checkDisabledGuards();
    checkStatePixels();

    System.out.println(
        "ElwhaSwitchInteractionSmoke: OK (click/drag/Space toggle semantics + event contract +"
            + " state-layer and pressed-handle pixels)");
  }

  private static void checkClickSemantics() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final int[] actions = {0};
    final int[] changes = {0};
    final String[] lastCommand = {null};
    s.addActionListener(
        e -> {
          actions[0]++;
          lastCommand[0] = e.getActionCommand();
        });
    s.addChangeListener(e -> changes[0]++);

    press(s, 30, CY);
    release(s, 30, CY);
    check("click toggles on", s.isSelected());
    check("click fires ActionListener once", actions[0] == 1);
    check("click fires ChangeListener once", changes[0] == 1);
    check("action command reflects the committed state", "selected".equals(lastCommand[0]));

    press(s, 30, CY);
    release(s, 200, CY);
    check("release outside the bounds cancels the click", s.isSelected());
    check("cancelled click fires no events", actions[0] == 1 && changes[0] == 1);

    s.setSelected(false);
    check("programmatic setSelected fires ChangeListener", changes[0] == 2);
    check("programmatic setSelected never fires ActionListener", actions[0] == 1);
  }

  private static void checkDragSemantics() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final int[] actions = {0};
    final int[] changes = {0};
    s.addActionListener(e -> actions[0]++);
    s.addChangeListener(e -> changes[0]++);

    press(s, 20, CY);
    drag(s, 40, CY);
    release(s, 40, CY);
    check("drag to the far end commits selected", s.isSelected());
    check("drag commit fires both listeners once", actions[0] == 1 && changes[0] == 1);

    press(s, 40, CY);
    drag(s, 24, CY);
    release(s, 24, CY);
    check("drag past the midpoint back commits unselected", !s.isSelected());
    check("each committed drag fires once", actions[0] == 2 && changes[0] == 2);

    press(s, 20, CY);
    drag(s, 26, CY);
    release(s, 26, CY);
    check("a scrub landing on the starting half commits nothing", !s.isSelected());
    check("an uncommitted drag fires no events", actions[0] == 2 && changes[0] == 2);

    press(s, 20, CY);
    drag(s, 22, CY);
    release(s, 22, CY);
    check("sub-threshold movement still counts as a click", s.isSelected());
    check("the sub-threshold click fired", actions[0] == 3 && changes[0] == 3);
  }

  private static void checkKeyboardSemantics() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    final int[] actions = {0};
    s.addActionListener(e -> actions[0]++);

    spacePress(s);
    check("Space press alone does not toggle", !s.isSelected());
    spaceRelease(s);
    check("Space release commits the toggle", s.isSelected());
    check("Space toggle fires ActionListener", actions[0] == 1);

    spaceRelease(s);
    check("a stray Space release without a press is ignored", s.isSelected() && actions[0] == 1);
  }

  private static void checkDisabledGuards() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    s.setEnabled(false);
    final int[] events = {0};
    s.addActionListener(e -> events[0]++);
    s.addChangeListener(e -> events[0]++);

    press(s, 30, CY);
    release(s, 30, CY);
    spacePress(s);
    spaceRelease(s);
    check("a disabled switch ignores click and Space", !s.isSelected() && events[0] == 0);
  }

  private static void checkStatePixels() {
    final Color ground = ColorRole.SURFACE.resolve();

    final ElwhaSwitch hover = sized(new ElwhaSwitch());
    hover.setHovered(true);
    final BufferedImage hImg = render(hover);
    check(
        "hover paints the ON_SURFACE 8% layer on the halo",
        near(hImg, 7, 7, mix(ground, ColorRole.ON_SURFACE.resolve(), StateLayer.HOVER.opacity())));
    check(
        "hover shifts the unselected handle to ON_SURFACE_VARIANT",
        near(hImg, 20, CY, ColorRole.ON_SURFACE_VARIANT.resolve()));

    final ElwhaSwitch pressedOn = sized(new ElwhaSwitch(true));
    pressedOn.setPressed(true);
    final BufferedImage pImg = render(pressedOn);
    check(
        "pressed shifts the selected handle to PRIMARY_CONTAINER",
        near(pImg, 40, CY, ColorRole.PRIMARY_CONTAINER.resolve()));

    final ElwhaSwitch pressedOff = sized(new ElwhaSwitch());
    final BufferedImage idleImg = render(pressedOff);
    check(
        "idle unselected: x=9 is still track interior",
        near(idleImg, 9, CY, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));
    pressedOff.setPressed(true);
    final BufferedImage grownImg = render(pressedOff);
    check(
        "pressed unselected: the 28px handle reaches x=9",
        near(grownImg, 9, CY, ColorRole.ON_SURFACE_VARIANT.resolve()));
  }

  private static ElwhaSwitch sized(final ElwhaSwitch s) {
    s.setSize(W, H);
    return s;
  }

  private static void press(final ElwhaSwitch s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_PRESSED, x, y);
  }

  private static void drag(final ElwhaSwitch s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_DRAGGED, x, y);
  }

  private static void release(final ElwhaSwitch s, final int x, final int y) {
    dispatch(s, MouseEvent.MOUSE_RELEASED, x, y);
  }

  private static void spacePress(final ElwhaSwitch s) {
    s.getActionMap()
        .get("elwhaSwitch.press")
        .actionPerformed(new ActionEvent(s, ActionEvent.ACTION_PERFORMED, "space"));
  }

  private static void spaceRelease(final ElwhaSwitch s) {
    s.getActionMap()
        .get("elwhaSwitch.release")
        .actionPerformed(new ActionEvent(s, ActionEvent.ACTION_PERFORMED, "space"));
  }

  private static void dispatch(final ElwhaSwitch s, final int id, final int x, final int y) {
    final int button = MouseEvent.BUTTON1;
    final int modifiers = (id == MouseEvent.MOUSE_RELEASED) ? 0 : MouseEvent.BUTTON1_DOWN_MASK;
    // Explicit-abs-coords ctor: the (x,y)-only ctor calls getLocationOnScreen, which NPEs on an
    // unrealized (peerless) component in headless mode.
    final MouseEvent e =
        new MouseEvent(s, id, System.nanoTime(), modifiers, x, y, x, y, 1, false, button);
    for (final var l : s.getMouseListeners()) {
      switch (id) {
        case MouseEvent.MOUSE_PRESSED -> l.mousePressed(e);
        case MouseEvent.MOUSE_RELEASED -> l.mouseReleased(e);
        default -> {
          // handled by motion listeners below
        }
      }
    }
    if (id == MouseEvent.MOUSE_DRAGGED) {
      for (final var l : s.getMouseMotionListeners()) {
        l.mouseDragged(e);
      }
    }
  }

  private static BufferedImage render(final ElwhaSwitch s) {
    final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(ColorRole.SURFACE.resolve());
      g.fillRect(0, 0, W, H);
      s.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean near(final BufferedImage img, final int x, final int y, final Color want) {
    final Color got = new Color(img.getRGB(x, y), true);
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
  }

  /** Source-over blend of {@code over} at {@code alpha} onto an opaque {@code base}. */
  private static Color mix(final Color base, final Color over, final float alpha) {
    return new Color(
        Math.round(base.getRed() + (over.getRed() - base.getRed()) * alpha),
        Math.round(base.getGreen() + (over.getGreen() - base.getGreen()) * alpha),
        Math.round(base.getBlue() + (over.getBlue() - base.getBlue()) * alpha));
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
