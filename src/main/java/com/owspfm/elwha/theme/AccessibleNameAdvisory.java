package com.owspfm.elwha.theme;

import java.awt.Component;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 * The library-wide missing-accessible-name advisory — a shared {@link Logger#warning(String)}
 * emitted once per component instance when a component that documents an accessible-name obligation
 * reaches its first paint without a meaningful name.
 *
 * <p><strong>Advisory only.</strong> The check never throws and never alters paint or layout; it
 * exists because a missing accessible name is invisible in a visual smoke, so a runtime log line is
 * the only guardrail that fires during development. Components call {@link #checkOnce checkOnce}
 * from {@code paintComponent}, mirroring the lifecycle point {@code ElwhaNavigationRail}
 * established: first paint is late enough that construction-time wiring (tooltips, labels, declared
 * names) has settled.
 *
 * <p><strong>Throttling.</strong> The first call per component instance performs the check —
 * warning if the resolved name is missing — and every later call for that instance is a no-op,
 * whatever the first outcome was. Instances are tracked weakly, so the advisory holds no component
 * alive.
 *
 * <p><strong>Log routing.</strong> The record is published through {@code
 * Logger.getLogger(owner.getName())} — the owning Elwha component class, not the runtime class of a
 * consumer subclass — so every advisory can be filtered under the {@code com.owspfm.elwha} logger
 * namespace.
 *
 * @author Charles Bryan
 * @version v1.1.0
 * @since v1.1.0
 */
public final class AccessibleNameAdvisory {

  private static final Set<Component> CHECKED =
      Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

  private AccessibleNameAdvisory() {}

  /**
   * Performs the once-per-instance missing-accessible-name check. On the first call for {@code
   * component}, logs a {@link java.util.logging.Level#WARNING WARNING} of the form {@code "<Owner>
   * has no accessible name. <remedy>"} when {@code resolvedName} is {@code null} or empty; later
   * calls for the same instance are no-ops regardless of the first outcome. Never throws.
   *
   * @param component the component instance being checked; {@code null} is a no-op
   * @param owner the Elwha component class that owns the advisory — supplies the message subject
   *     ({@link Class#getSimpleName()}) and the logger name ({@link Class#getName()})
   * @param resolvedName the component's meaningful accessible name, or {@code null} / empty when
   *     resolution would fall through to a generic literal or to nothing
   * @param remedy the component-specific guidance appended to the uniform message prefix, e.g.
   *     {@code "Call setAccessibleName(...) — e.g. \"Primary navigation\" — so screen readers can
   *     identify the rail."}
   * @version v1.1.0
   * @since v1.1.0
   */
  public static void checkOnce(
      final JComponent component,
      final Class<?> owner,
      final String resolvedName,
      final String remedy) {
    if (component == null || !CHECKED.add(component)) {
      return;
    }
    if (resolvedName == null || resolvedName.isEmpty()) {
      Logger.getLogger(owner.getName())
          .warning(owner.getSimpleName() + " has no accessible name. " + remedy);
    }
  }
}
