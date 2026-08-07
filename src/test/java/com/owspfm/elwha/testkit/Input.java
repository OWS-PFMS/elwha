package com.owspfm.elwha.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Synthetic-input helpers for the headless tier. Mouse events travel through the real {@link
 * Component#dispatchEvent} pipeline (exercising the component's own listener registration and event
 * masks — proven equivalent to and stricter than the smokes' direct listener iteration); keyboard
 * coverage asserts the {@code WHEN_FOCUSED} InputMap binding <em>and then</em> invokes the mapped
 * action, covering the wiring the smokes historically skipped.
 *
 * <p>Real focus ownership and real key dispatch belong to the {@code gui} tier — these helpers
 * deliberately do not fake either (dispatched {@code FocusEvent}s are banned; research doc §4).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class Input {

  private Input() {}

  /**
   * Dispatches a mouse press at {@code (x, y)}.
   *
   * @param target the component under test
   * @param x press x in component coordinates
   * @param y press y in component coordinates
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void press(final Component target, final int x, final int y) {
    mouse(target, MouseEvent.MOUSE_PRESSED, x, y);
  }

  /**
   * Dispatches a mouse drag to {@code (x, y)}.
   *
   * @param target the component under test
   * @param x drag x in component coordinates
   * @param y drag y in component coordinates
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void drag(final Component target, final int x, final int y) {
    mouse(target, MouseEvent.MOUSE_DRAGGED, x, y);
  }

  /**
   * Dispatches a mouse release at {@code (x, y)}.
   *
   * @param target the component under test
   * @param x release x in component coordinates
   * @param y release y in component coordinates
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void release(final Component target, final int x, final int y) {
    mouse(target, MouseEvent.MOUSE_RELEASED, x, y);
  }

  /**
   * Dispatches a mouse-entered event — the only way to reach a hover state that the component keeps
   * private and drives from its own {@code MouseListener} (rather than exposing a {@code
   * setHovered} seam). Unlike focus, hover carries no toolkit-level ownership: {@code
   * MOUSE_ENTERED} through the real dispatch pipeline is exactly what the platform delivers, so
   * nothing is faked.
   *
   * @param target the component under test
   * @param x pointer x in component coordinates
   * @param y pointer y in component coordinates
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void enter(final Component target, final int x, final int y) {
    mouse(target, MouseEvent.MOUSE_ENTERED, x, y);
  }

  /**
   * Dispatches a mouse-exited event, clearing a hover state entered with {@link #enter}.
   *
   * @param target the component under test
   * @param x pointer x in component coordinates
   * @param y pointer y in component coordinates
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void exit(final Component target, final int x, final int y) {
    mouse(target, MouseEvent.MOUSE_EXITED, x, y);
  }

  /**
   * Dispatches a press-then-release pair at {@code (x, y)} — the synthetic click.
   *
   * @param target the component under test
   * @param x click x in component coordinates
   * @param y click y in component coordinates
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void click(final Component target, final int x, final int y) {
    press(target, x, y);
    release(target, x, y);
  }

  /**
   * Asserts the keystroke is bound in the component's {@code WHEN_FOCUSED} InputMap to the given
   * action key, then invokes that action through the ActionMap — the headless-tier keyboard
   * contract (binding wiring + action behavior, no fake focus).
   *
   * @param target the component under test
   * @param keystroke the keystroke descriptor, e.g. {@code "pressed SPACE"}
   * @param expectedActionKey the action key the binding must map to
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void pressBoundKey(
      final JComponent target, final String keystroke, final Object expectedActionKey) {
    final Object bound =
        target.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(keystroke));
    assertThat(bound)
        .as("'%s' is bound in WHEN_FOCUSED to %s", keystroke, expectedActionKey)
        .isEqualTo(expectedActionKey);
    target
        .getActionMap()
        .get(bound)
        .actionPerformed(new ActionEvent(target, ActionEvent.ACTION_PERFORMED, keystroke));
  }

  private static void mouse(final Component target, final int id, final int x, final int y) {
    final boolean buttonDown = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED;
    final boolean pointerOnly = id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_EXITED;
    final int modifiers = buttonDown ? MouseEvent.BUTTON1_DOWN_MASK : 0;
    final int button = pointerOnly ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1;
    final int clicks = pointerOnly ? 0 : 1;
    // Explicit-abs-coords ctor: the (x,y)-only ctor calls getLocationOnScreen, which NPEs on an
    // unrealized (peerless) component in headless mode.
    target.dispatchEvent(
        new MouseEvent(
            target, id, System.nanoTime(), modifiers, x, y, x, y, clicks, false, button));
  }
}
