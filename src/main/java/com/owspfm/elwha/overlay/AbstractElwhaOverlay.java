package com.owspfm.elwha.overlay;

import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Library-internal base for Elwha's in-window overlay primitives — the surfaces that mount on the
 * host frame's {@link JLayeredPane} rather than spawning a separate {@link Window}. Owns the
 * overlay-host lifecycle every consumer shares: host resolution, layered-pane attach at a
 * subclass-chosen z-band, the {@link MorphAnimator} entrance/exit, relayout-on-resize, the {@link
 * KeyboardFocusManager} focus management, and the dismiss/teardown sequence. Subclasses supply the
 * anatomy (surface, optional backdrop, placement, key bindings) and choose between two strategy
 * axes: <em>dismiss policy</em> (modal-trap vs light-dismiss) and <em>placement</em> (the {@link
 * #layoutSurface} hook — centered, anchored, frame-filling).
 *
 * <p><strong>Extraction provenance (epic #298 S1 spike).</strong> Lifted out of {@code
 * com.owspfm.elwha.dialog.AbstractElwhaDialog} (#254/#271) so the M3 Menu (#298) and the pending
 * side-sheet (#308) reuse the mount/focus/motion plumbing instead of copying it. {@code
 * AbstractElwhaDialog} is now the modal-scrim + centered subclass ({@link #lightDismiss()} {@code
 * false}, {@link #overlayLayer()} {@link JLayeredPane#MODAL_LAYER}); the menu host is the
 * light-dismiss + anchored subclass ({@link #lightDismiss()} {@code true}, {@link #overlayLayer()}
 * {@link JLayeredPane#POPUP_LAYER}). The split is recorded in {@code
 * docs/research/elwha-menu-design.md} §2.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .overlay} package boundary into the component packages (dialog, menu, side-sheet) that subclass
 * it. Library consumers must not depend on this type — its signature and semantics can change
 * without a deprecation cycle, mirroring {@code ShadowPainter} / {@code RipplePainter}.
 *
 * <p><strong>Dismiss policy.</strong> When {@link #lightDismiss()} is {@code false} (modal), a
 * focus escape to the inert background is <em>pulled back</em> into the surface (the trap) and
 * there is no outside-click teardown — the subclass-supplied backdrop blocks input. When {@link
 * #lightDismiss()} is {@code true} (menu/popover), a focus escape or a mouse press outside the
 * surface <em>dismisses</em> the overlay, and no backdrop is required.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public abstract class AbstractElwhaOverlay {

  /** Entrance / exit duration — M3 medium2 (300 ms). */
  protected static final int MOTION_MS = MorphAnimator.MEDIUM2_MS;

  // Every currently-shown overlay, in show order. Only the topmost (highest overlayLayer(), ties
  // broken by most-recently-shown) reacts to a focus escape — so a modal dialog's focus trap
  // suspends while a menu sits above it at POPUP_LAYER, instead of the two fighting over focus.
  private static final Deque<AbstractElwhaOverlay> OPEN = new ArrayDeque<>();

  // Live overlay state — non-null only while shown.

  /** The host layered pane the overlay is attached to; {@code null} when not shown. */
  protected JLayeredPane layeredPane;

  /**
   * The full-bounds backdrop painted beneath the surface — a scrim for a modal dialog, {@code null}
   * for a light-dismiss menu or a frame-filling surface. Repainted every motion tick so a fading
   * backdrop tracks {@link #motionProgress}.
   */
  protected JComponent backdrop;

  /** The overlay surface (the visible body); {@code null} when not shown. */
  protected JComponent surface;

  /** The host window — used by the focus listener's same-window check. */
  protected Window hostWindow;

  /**
   * The component {@link #show(Component)} was invoked with — used to resolve the host root pane,
   * restore focus on close, and (for an anchored placement) as the geometric anchor. {@code null}
   * when not shown.
   */
  protected Component anchor;

  /** Orientation captured from the {@code show(parent)} component for RTL mirroring. */
  protected ComponentOrientation orientation = ComponentOrientation.LEFT_TO_RIGHT;

  /**
   * Eased entrance/exit progress in {@code [0, 1]} ({@code 1} = fully shown). Read by subclass
   * surface / backdrop paint to drive the entrance and exit animations.
   */
  protected float motionProgress = 1f;

  /** The entrance/exit animator hosted on the surface; {@code null} when not shown. */
  protected MorphAnimator entrance;

  private Component focusOwnerBeforeShow;
  private ComponentListener relayoutListener;
  private PropertyChangeListener focusListener;
  private AWTEventListener outsidePressListener;
  private boolean closing;

  // -------------------------------------------------- abstract anatomy hooks

  /**
   * Creates and fully populates the overlay surface. Called once per {@link #show(Component)},
   * after the host is resolved (so {@link #layeredPane} is available for width-dependent layout).
   *
   * @return the populated surface to attach to the layered pane
   */
  protected abstract JComponent createSurface();

  /**
   * Sizes and positions {@link #surface} within the layered pane. Called on show and on every host
   * resize, after the backdrop (when present) has been stretched over the full pane.
   *
   * @param paneWidth the layered pane's current width
   * @param paneHeight the layered pane's current height
   */
  protected abstract void layoutSurface(int paneWidth, int paneHeight);

  /**
   * The accessible name to set on the surface, or {@code null}.
   *
   * @return the accessible name, or {@code null}
   */
  protected abstract String accessibleName();

  // ------------------------------------------------ overridable strategy

  /**
   * Creates the full-bounds backdrop painted beneath the surface, or {@code null} for none (a
   * light-dismiss menu needs no scrim).
   *
   * @return the backdrop component, or {@code null} (default)
   */
  protected JComponent createBackdrop() {
    return null;
  }

  /**
   * The {@link JLayeredPane} z-band the overlay mounts on. Defaults to {@link
   * JLayeredPane#MODAL_LAYER} (200) — the dialog band; a menu/popover overrides to {@link
   * JLayeredPane#POPUP_LAYER} (300) so it tops dialogs.
   *
   * @return the layer constraint
   */
  protected Integer overlayLayer() {
    return JLayeredPane.MODAL_LAYER;
  }

  /**
   * Whether this overlay light-dismisses. {@code false} (default) is modal: focus escapes are
   * trapped back into the surface and there is no outside-click teardown. {@code true} is a
   * menu/popover: a focus escape or a mouse press outside the surface dismisses it.
   *
   * @return {@code true} for light-dismiss, {@code false} (default) for modal
   */
  protected boolean lightDismiss() {
    return false;
  }

  /**
   * Whether focus should be restored to the {@code show(parent)} owner when the overlay closes.
   * Default {@code true}. A light-dismiss overlay closed <em>because</em> focus moved elsewhere (a
   * Tab-away or a click into other content) overrides this to {@code false} so the teardown does
   * not yank focus back from where the user just put it.
   *
   * @return {@code true} (default) to restore focus on close
   */
  protected boolean restoreFocusOnClose() {
    return true;
  }

  /** Installs key bindings on {@link #surface}'s input/action maps. Default no-op. */
  protected void installKeyBindings() {}

  /**
   * The eased curve applied to raw animator progress before it reaches {@link #motionProgress}.
   *
   * @return the easing curve (default {@link Easing#EMPHASIZED_DECELERATE})
   */
  protected Easing easing() {
    return Easing.EMPHASIZED_DECELERATE;
  }

  /**
   * The entrance/exit motion duration in milliseconds.
   *
   * @return the duration (default {@link #MOTION_MS})
   */
  protected int motionDurationMs() {
    return MOTION_MS;
  }

  /**
   * The preferred initial-focus target; {@code null} falls through to the first focusable
   * descendant of the surface, then to the surface itself.
   *
   * @return the preferred focus target, or {@code null}
   */
  protected Component initialFocusTarget() {
    return null;
  }

  /**
   * Reaction to focus escaping the surface to the inert background (still inside the host window).
   * Default — the modal trap — pulls focus back to the surface. A light-dismiss overlay overrides
   * to begin closing.
   */
  protected void onFocusEscaped() {
    focusInitial();
  }

  /**
   * Reaction to a mouse press landing outside the surface while a {@linkplain #lightDismiss()
   * light-dismiss} overlay is shown. Default begins closing; a subclass tracking a dismiss cause
   * overrides to record it before closing. Only invoked when {@link #lightDismiss()} is {@code
   * true}.
   */
  protected void onOutsidePress() {
    beginClose();
  }

  /** Hook for subclasses to null out their own live state during teardown. Default no-op. */
  protected void clearTransientState() {}

  /**
   * Fired after the overlay is fully detached and focus restored. Subclasses report their own
   * dismiss outcome here (the dialog fires its {@code onClose} consumer with the {@code
   * DismissCause}).
   */
  protected void onClosed() {}

  // --------------------------------------------------------- lifecycle

  /**
   * Shows the overlay on the host frame resolved from {@code parent}. Returns immediately; the
   * outcome is reported through {@link #onClosed()} after teardown.
   *
   * @param parent any component in the target window's tree; used to resolve the host root pane, to
   *     restore focus on close, and (for an anchored placement) as the geometric anchor
   * @throws NullPointerException if {@code parent} is {@code null}
   * @throws IllegalStateException if {@code parent} is not yet in a realized window
   * @version v0.4.0
   * @since v0.4.0
   */
  public final void show(final Component parent) {
    Objects.requireNonNull(parent, "parent");
    final JRootPane root = SwingUtilities.getRootPane(parent);
    if (root == null) {
      throw new IllegalStateException("parent is not in a realized window with a root pane");
    }
    this.layeredPane = root.getLayeredPane();
    this.hostWindow = SwingUtilities.getWindowAncestor(root);
    this.anchor = parent;
    this.orientation = parent.getComponentOrientation();
    this.focusOwnerBeforeShow =
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

    this.motionProgress = 0f;
    this.closing = false;
    this.surface = createSurface();
    this.backdrop = createBackdrop();
    installKeyBindings();
    surface.applyComponentOrientation(orientation);
    final String name = accessibleName();
    if (name != null) {
      surface.getAccessibleContext().setAccessibleName(name);
    }

    final Integer layer = overlayLayer();
    if (backdrop != null) {
      layeredPane.add(backdrop, layer);
    }
    layeredPane.add(surface, layer);
    layeredPane.moveToFront(surface);

    relayoutListener =
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            relayout();
          }
        };
    layeredPane.addComponentListener(relayoutListener);
    OPEN.remove(this);
    OPEN.addLast(this);
    installFocusListener();
    if (lightDismiss()) {
      installOutsidePressListener();
    }

    relayout();

    entrance = new MorphAnimator(surface, motionDurationMs());
    entrance.addProgressListener(this::onMotionTick);
    entrance.snapTo(0f);
    entrance.start();

    layeredPane.revalidate();
    layeredPane.repaint();

    SwingUtilities.invokeLater(this::focusInitial);
  }

  /**
   * Whether the overlay is currently mid-teardown — the re-entry guard so a dismiss triggered from
   * within an exit listener (or a second outside click) can't double-fire.
   *
   * @return {@code true} once {@link #beginClose()} has started the exit
   */
  protected final boolean isClosing() {
    return closing;
  }

  /**
   * Whether the overlay is currently shown (mounted on a layered pane).
   *
   * @return {@code true} between {@link #show(Component)} and full teardown
   */
  protected final boolean isShowing() {
    return layeredPane != null;
  }

  /**
   * Begins closing the overlay: runs the exit motion, then tears the overlay down when it reaches 0
   * (reduced motion snaps and fires the listener synchronously, so this path still completes). A
   * no-op if not shown or already closing.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final void beginClose() {
    if (closing || layeredPane == null) {
      return;
    }
    closing = true;
    // Animate the exit only when there is entrance progress to collapse. Closing while the entrance
    // is still at 0 (a dismiss within the first ~16 ms, before the animator's first tick) would
    // make
    // reverse() a no-op (progress already == target 0), so no tick ever fires and teardown never
    // runs — the overlay would wedge open. Tear down directly in that case.
    if (entrance != null && entrance.progress() > 0f) {
      entrance.reverse();
    } else {
      performTeardown();
    }
  }

  // Per-tick motion update: ease the linear progress, repaint a present backdrop (the surface is
  // the
  // animator's own repaint host), and finish teardown once the exit has fully collapsed.
  private void onMotionTick() {
    if (entrance == null) {
      return;
    }
    motionProgress = easing().ease(entrance.progress());
    if (backdrop != null) {
      backdrop.repaint();
    }
    if (closing && entrance.target() == 0f && entrance.progress() == 0f) {
      performTeardown();
    }
  }

  // Detaches the overlay, restores focus (when policy allows), and fires onClosed(). Idempotent —
  // the exit-motion completion tick can land on progress 0 more than once.
  private void performTeardown() {
    if (layeredPane == null) {
      return;
    }
    if (entrance != null) {
      entrance.stop();
    }
    OPEN.remove(this);
    if (focusListener != null) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .removePropertyChangeListener("focusOwner", focusListener);
    }
    if (outsidePressListener != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(outsidePressListener);
    }
    layeredPane.removeComponentListener(relayoutListener);
    if (backdrop != null) {
      layeredPane.remove(backdrop);
    }
    layeredPane.remove(surface);
    layeredPane.revalidate();
    layeredPane.repaint();

    final boolean restore = restoreFocusOnClose();
    final Component toRestore = focusOwnerBeforeShow;
    final JLayeredPane closed = layeredPane;
    layeredPane = null;
    backdrop = null;
    surface = null;
    anchor = null;
    relayoutListener = null;
    focusListener = null;
    outsidePressListener = null;
    entrance = null;
    hostWindow = null;
    focusOwnerBeforeShow = null;
    closing = false;
    clearTransientState();

    if (restore && toRestore != null) {
      SwingUtilities.invokeLater(toRestore::requestFocusInWindow);
    } else {
      closed.repaint();
    }

    onClosed();
  }

  // Stretches the backdrop over the full layered pane, then delegates surface sizing/placement to
  // the subclass. Re-run on host resize.
  protected final void relayout() {
    if (layeredPane == null) {
      return;
    }
    final int w = layeredPane.getWidth();
    final int h = layeredPane.getHeight();
    if (backdrop != null) {
      backdrop.setBounds(0, 0, w, h);
    }
    layoutSurface(w, h);
  }

  // Focus listener: while shown, if focus leaves the surface to the inert background (still inside
  // the host window), defer to onFocusEscaped() — the modal trap pulls it back; light-dismiss
  // closes. Focus moving to a different window is left alone (the host window simply lost
  // activation).
  private void installFocusListener() {
    focusListener =
        evt -> {
          final Object next = evt.getNewValue();
          if (!(next instanceof Component) || surface == null) {
            return;
          }
          final Component owner = (Component) next;
          if (SwingUtilities.isDescendingFrom(owner, surface)) {
            return;
          }
          if (SwingUtilities.getWindowAncestor(owner) != hostWindow) {
            return;
          }
          // Only the topmost overlay reacts: a modal dialog's trap stays quiet while a menu sits
          // above it (POPUP_LAYER), so the two don't fight over focus and thrash (#298 F1).
          if (!isTopmost()) {
            return;
          }
          onFocusEscaped();
        };
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener("focusOwner", focusListener);
  }

  // The topmost open overlay is the one with the greatest overlayLayer(); ties (e.g. stacked
  // dialogs on MODAL_LAYER) are broken by show order — the most recently shown wins.
  private boolean isTopmost() {
    AbstractElwhaOverlay top = null;
    for (final AbstractElwhaOverlay o : OPEN) {
      if (top == null || o.overlayLayer() >= top.overlayLayer()) {
        top = o;
      }
    }
    return top == this;
  }

  // Light-dismiss outside-click: a passive AWT listener (cannot consume the event) that closes the
  // overlay when a mouse press lands on a component that is not a descendant of the surface. The
  // press that opened the overlay was dispatched before this listener was added, so it never
  // self-dismisses on open.
  private void installOutsidePressListener() {
    outsidePressListener =
        (final AWTEvent event) -> {
          if (!(event instanceof MouseEvent me) || me.getID() != MouseEvent.MOUSE_PRESSED) {
            return;
          }
          if (surface == null || closing) {
            return;
          }
          final Object src = me.getSource();
          if (src instanceof Component c && SwingUtilities.isDescendingFrom(c, surface)) {
            return;
          }
          onOutsidePress();
        };
    Toolkit.getDefaultToolkit()
        .addAWTEventListener(outsidePressListener, AWTEvent.MOUSE_EVENT_MASK);
  }

  // Initial / recovered focus: the subclass's preferred target, else the first focusable
  // descendant,
  // else the surface itself — never the inert background.
  private void focusInitial() {
    if (surface == null) {
      return;
    }
    final Component preferred = initialFocusTarget();
    if (preferred != null && preferred.requestFocusInWindow()) {
      return;
    }
    final Component first = firstFocusable(surface);
    if (first != null) {
      first.requestFocusInWindow();
    } else {
      surface.requestFocusInWindow();
    }
  }

  /**
   * Depth-first search for the first focus-accepting descendant, skipping container panels.
   *
   * @param root the container to search
   * @return the first focusable descendant, or {@code null}
   */
  protected static Component firstFocusable(final Container root) {
    for (final Component child : root.getComponents()) {
      if (child.isFocusable()
          && child.isEnabled()
          && child.isVisible()
          && child.isDisplayable()
          && !(child instanceof JPanel)) {
        return child;
      }
      if (child instanceof Container) {
        final Component nested = firstFocusable((Container) child);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  /**
   * Wraps a {@link Runnable} as an {@link AbstractAction} for key-binding action maps.
   *
   * @param body the action body
   * @return an action that runs {@code body}
   */
  protected static AbstractAction action(final Runnable body) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        body.run();
      }
    };
  }
}
