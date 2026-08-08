package com.owspfm.elwha.testkit;

import java.lang.reflect.Method;
import javax.swing.JComponent;

/**
 * Reads a component's {@code isPaintingOrigin()} answer — the Swing flag that decides whether a
 * descendant's own repaint is redirected up to an ancestor that transforms or composites it.
 *
 * <p>Reflective because the method is {@code protected} on {@link JComponent} and the components
 * that matter here are private inner surfaces (a dialog's scale-and-fade surface, a side sheet's
 * slide surface), so a test can neither name their type nor reach the inherited declaration. The
 * flag is a real contract rather than an implementation detail — an unconditional {@code true}
 * forces every caret blink to re-composite the whole overlay for as long as it is open — so it is
 * worth pinning directly instead of inferring it from a paint trace.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class PaintOrigin {

  private PaintOrigin() {}

  /**
   * Returns whether {@code component} currently reports itself as a painting origin.
   *
   * @param component the component to ask
   * @return the component's own {@code isPaintingOrigin()} answer
   * @throws IllegalStateException if the method cannot be reached reflectively
   * @version v0.5.0
   * @since v0.5.0
   */
  public static boolean of(final JComponent component) {
    try {
      final Method method = findMethod(component.getClass());
      method.setAccessible(true);
      return (boolean) method.invoke(component);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("isPaintingOrigin() is unreachable on " + component, e);
    }
  }

  private static Method findMethod(final Class<?> type) throws NoSuchMethodException {
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      try {
        return c.getDeclaredMethod("isPaintingOrigin");
      } catch (final NoSuchMethodException ignored) {
        // keep walking up to JComponent's own declaration
      }
    }
    throw new NoSuchMethodException("isPaintingOrigin() not declared on " + type);
  }
}
