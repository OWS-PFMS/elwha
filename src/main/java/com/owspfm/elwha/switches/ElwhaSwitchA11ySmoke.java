package com.owspfm.elwha.switches;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.JLabel;

/**
 * Headless S5 guard (story #406) — asserts the design §8 accessible contract (TOGGLE_BUTTON role,
 * CHECKED state with property-change events, the "click" action firing the user-gesture event
 * surface, the 0/1 value firing only the programmatic surface, setLabel/labelFor naming with
 * setLabel precedence) and the §7 RTL mirror (selected rests left, drag commit halves flip,
 * rendered geometry mirrors).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchA11ySmoke {

  private static final int W = 60;
  private static final int H = 40;
  private static final int CY = 20;

  private ElwhaSwitchA11ySmoke() {}

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

    checkRoleAndState();
    checkAction();
    checkValue();
    checkNaming();
    checkRtl();

    System.out.println(
        "ElwhaSwitchA11ySmoke: OK (role/state/action/value + naming precedence + RTL mirror)");
  }

  private static void checkRoleAndState() {
    final ElwhaSwitch s = new ElwhaSwitch();
    final AccessibleContext ax = s.getAccessibleContext();
    check("role is TOGGLE_BUTTON", ax.getAccessibleRole() == AccessibleRole.TOGGLE_BUTTON);
    check(
        "unselected has no CHECKED state",
        !ax.getAccessibleStateSet().contains(AccessibleState.CHECKED));

    final boolean[] stateEventFired = {false};
    ax.addPropertyChangeListener(
        e -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(e.getPropertyName())) {
            stateEventFired[0] = true;
          }
        });
    s.setSelected(true);
    check("selected adds CHECKED", ax.getAccessibleStateSet().contains(AccessibleState.CHECKED));
    check("the toggle fires an ACCESSIBLE_STATE_PROPERTY change", stateEventFired[0]);
  }

  private static void checkAction() {
    final ElwhaSwitch s = new ElwhaSwitch();
    final AccessibleContext ax = s.getAccessibleContext();
    check("one accessible action", ax.getAccessibleAction().getAccessibleActionCount() == 1);
    check(
        "the action is named click",
        "click".equals(ax.getAccessibleAction().getAccessibleActionDescription(0)));

    final int[] actions = {0};
    s.addActionListener(e -> actions[0]++);
    check("doAccessibleAction toggles", ax.getAccessibleAction().doAccessibleAction(0));
    check("the action toggled the switch", s.isSelected());
    check("the action fires the user-gesture ActionListener", actions[0] == 1);

    s.setEnabled(false);
    check("a disabled switch refuses the action", !ax.getAccessibleAction().doAccessibleAction(0));
    check("the refused action did not toggle", s.isSelected() && actions[0] == 1);
  }

  private static void checkValue() {
    final ElwhaSwitch s = new ElwhaSwitch();
    final AccessibleContext ax = s.getAccessibleContext();
    check(
        "value is 0 unselected",
        ax.getAccessibleValue().getCurrentAccessibleValue().intValue() == 0);
    check(
        "value min/max are 0/1",
        ax.getAccessibleValue().getMinimumAccessibleValue().intValue() == 0
            && ax.getAccessibleValue().getMaximumAccessibleValue().intValue() == 1);

    final int[] actions = {0};
    final int[] changes = {0};
    s.addActionListener(e -> actions[0]++);
    s.addChangeListener(e -> changes[0]++);
    check(
        "setCurrentAccessibleValue(1) accepts",
        ax.getAccessibleValue().setCurrentAccessibleValue(1));
    check("the value write selected the switch", s.isSelected());
    check(
        "the value write is programmatic — ChangeListener only",
        changes[0] == 1 && actions[0] == 0);
  }

  private static void checkNaming() {
    final ElwhaSwitch s = new ElwhaSwitch();
    final JLabel label = new JLabel("Wi-Fi");
    label.setLabelFor(s);
    check(
        "labelFor association names the switch",
        "Wi-Fi".equals(s.getAccessibleContext().getAccessibleName()));
    s.setLabel("Wireless");
    check(
        "an explicit setLabel takes precedence",
        "Wireless".equals(s.getAccessibleContext().getAccessibleName()));
    check("getLabel round-trips", "Wireless".equals(s.getLabel()));
  }

  private static void checkRtl() {
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    s.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    check("RTL: the unselected handle rests at the right end", s.handleCenterX() == 40);
    final BufferedImage off = render(s);
    check(
        "RTL: the rendered handle is at the right end",
        near(off, 40, CY, ColorRole.OUTLINE.resolve())
            && near(off, 20, CY, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));

    s.setSelected(true);
    check("RTL: the selected handle rests at the left end", s.handleCenterX() == 20);

    // RTL drag: from the selected (left) end toward the right is toward UNselected.
    press(s, 20, CY);
    drag(s, 36, CY);
    release(s, 36, CY);
    check("RTL: a drag to the right commits unselected (halves flip)", !s.isSelected());

    // RTL click still just toggles.
    press(s, 30, CY);
    release(s, 30, CY);
    check("RTL: click toggles", s.isSelected());
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

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
