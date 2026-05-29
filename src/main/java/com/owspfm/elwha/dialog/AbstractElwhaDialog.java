package com.owspfm.elwha.dialog;

import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Package-private base for Elwha's in-window modal dialog primitives — {@link ElwhaDialog} (the M3
 * Basic Dialog) and {@link ElwhaFullScreenDialog} (the M3 Full-screen Dialog). Owns the
 * overlay-host lifecycle the two share: host resolution, {@link JLayeredPane#MODAL_LAYER} attach,
 * the dismiss/teardown sequence, the {@link KeyboardFocusManager} focus trap + restore,
 * relayout-on-resize, and the {@link MorphAnimator} entrance/exit plumbing. Subclasses supply the
 * anatomy — surface, optional backdrop, layout, key bindings, motion paint — through the abstract
 * hooks.
 *
 * <p>Extracted from {@code ElwhaDialog} in epic #271 S1 so the Full-screen Dialog reuses the
 * machinery rather than duplicating it; the split is recorded in {@code
 * docs/research/elwha-fullscreen-dialog-design.md} §2.1.
 *
 * <p><strong>Modality mechanism.</strong> The dialog is an overlay installed on {@code
 * SwingUtilities.getRootPane(parent).getLayeredPane()} at {@link JLayeredPane#MODAL_LAYER} rather
 * than a separate {@link Window}: a subclass-supplied backdrop beneath the surface (a scrim for the
 * Basic Dialog; nothing for the frame-filling Full-screen Dialog) plus the surface on top. Input is
 * blocked by the backdrop's event-consumer and/or the surface physically covering the content,
 * backed by the keyboard focus trap. Reuses the {@code ElwhaFabAnchor} (#205) layered-pane glue.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.3.0
 * @since v0.3.0
 */
abstract class AbstractElwhaDialog {

  /** Entrance / exit duration — M3 medium2 (300 ms). */
  static final int MOTION_MS = MorphAnimator.MEDIUM2_MS;

  private final boolean dismissibleByEsc;
  private final Consumer<DismissCause> onClose;

  // Live overlay state — non-null only while shown.

  /** The host layered pane the overlay is attached to; {@code null} when not shown. */
  protected JLayeredPane layeredPane;

  /**
   * The full-bounds backdrop painted beneath the surface — a scrim for the Basic Dialog, {@code
   * null} for the frame-filling Full-screen Dialog. Repainted every motion tick so a fading
   * backdrop tracks {@link #motionProgress}.
   */
  protected JComponent backdrop;

  /** The dialog surface (the visible body); {@code null} when not shown. */
  protected JComponent surface;

  /** The host window — used by the focus trap's same-window check. */
  protected Window hostWindow;

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
  private PropertyChangeListener focusTrap;
  private DismissCause exitCause;
  private boolean closing;

  /**
   * @param dismissibleByEsc whether Escape dismisses the dialog
   * @param onClose the close hook fired after teardown, or {@code null} for none
   */
  protected AbstractElwhaDialog(
      final boolean dismissibleByEsc, final Consumer<DismissCause> onClose) {
    this.dismissibleByEsc = dismissibleByEsc;
    this.onClose = onClose;
  }

  // -------------------------------------------------- abstract anatomy hooks

  /**
   * Creates and fully populates the dialog surface. Called once per {@link #show(Component)}, after
   * the host is resolved (so {@link #layeredPane} is available for width-dependent layout).
   *
   * @return the populated surface to attach to the layered pane
   */
  protected abstract JComponent createSurface();

  /**
   * Creates the full-bounds backdrop painted beneath the surface, or {@code null} for none (the
   * Full-screen Dialog fills the frame and needs no scrim).
   *
   * @return the backdrop component, or {@code null}
   */
  protected abstract JComponent createBackdrop();

  /** Installs Esc / Enter key bindings on {@link #surface}'s input/action maps. */
  protected abstract void installKeyBindings();

  /**
   * Sizes and positions {@link #surface} within the layered pane. Called on show and on every host
   * resize, after the backdrop (when present) has been stretched over the full pane.
   *
   * @param paneWidth the layered pane's current width
   * @param paneHeight the layered pane's current height
   */
  protected abstract void layoutSurface(int paneWidth, int paneHeight);

  /**
   * The accessible name to set on the surface (typically the headline), or {@code null}.
   *
   * @return the accessible name, or {@code null}
   */
  protected abstract String accessibleName();

  // ------------------------------------------------ overridable defaults

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

  /** Hook for subclasses to null out their own live state during teardown. Default no-op. */
  protected void clearTransientState() {}

  // --------------------------------------------------------- lifecycle

  /**
   * Shows the dialog as a modal overlay on the host frame resolved from {@code parent}. Returns
   * immediately; the outcome is reported through the {@code onClose} hook with a {@link
   * DismissCause}.
   *
   * @param parent any component in the target window's tree; used to resolve the host root pane and
   *     to restore focus on close
   * @throws NullPointerException if {@code parent} is {@code null}
   * @throws IllegalStateException if {@code parent} is not yet in a realized window
   * @version v0.3.0
   * @since v0.3.0
   */
  public final void show(final Component parent) {
    Objects.requireNonNull(parent, "parent");
    final JRootPane root = SwingUtilities.getRootPane(parent);
    if (root == null) {
      throw new IllegalStateException("parent is not in a realized window with a root pane");
    }
    this.layeredPane = root.getLayeredPane();
    this.hostWindow = SwingUtilities.getWindowAncestor(root);
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

    if (backdrop != null) {
      layeredPane.add(backdrop, JLayeredPane.MODAL_LAYER);
    }
    layeredPane.add(surface, JLayeredPane.MODAL_LAYER);
    layeredPane.moveToFront(surface);

    relayoutListener =
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            relayout();
          }
        };
    layeredPane.addComponentListener(relayoutListener);
    installFocusTrap();

    relayout();

    // Entrance: subclass paint reads the eased motionProgress (scale-in for Basic, slide-up for
    // Full-screen). One animator hosted on the surface drives it; reduced motion snaps to the end.
    entrance = new MorphAnimator(surface, motionDurationMs());
    entrance.addProgressListener(this::onMotionTick);
    entrance.snapTo(0f);
    entrance.start();

    layeredPane.revalidate();
    layeredPane.repaint();

    SwingUtilities.invokeLater(this::focusInitial);
  }

  /**
   * Closes the dialog programmatically (cancel-equivalent), reporting {@link
   * DismissCause#PROGRAMMATIC}. A no-op if not currently shown.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public final void dismiss() {
    dismiss(DismissCause.PROGRAMMATIC);
  }

  // Begins closing: runs the exit motion, then tears the overlay down when it reaches 0 (reduced
  // motion snaps + fires the listener synchronously, so this path still completes). Re-entry
  // guarded
  // so an action listener that also calls dismiss() — or a backdrop click mid-exit — can't
  // double-fire.
  final void dismiss(final DismissCause cause) {
    if (closing || layeredPane == null) {
      return;
    }
    closing = true;
    exitCause = cause;
    if (entrance != null) {
      entrance.reverse();
    } else {
      performTeardown(cause);
    }
  }

  // Per-tick motion update: ease the linear progress, repaint the backdrop (the surface is the
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
      performTeardown(exitCause);
    }
  }

  // Detaches the overlay, restores focus, and fires onClose. Idempotent — the exit-motion
  // completion
  // tick can land on progress 0 more than once.
  private void performTeardown(final DismissCause cause) {
    if (layeredPane == null) {
      return;
    }
    if (entrance != null) {
      entrance.stop();
    }
    if (focusTrap != null) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .removePropertyChangeListener("focusOwner", focusTrap);
    }
    layeredPane.removeComponentListener(relayoutListener);
    if (backdrop != null) {
      layeredPane.remove(backdrop);
    }
    layeredPane.remove(surface);
    layeredPane.revalidate();
    layeredPane.repaint();

    final Component toRestore = focusOwnerBeforeShow;
    final JLayeredPane closed = layeredPane;
    layeredPane = null;
    backdrop = null;
    surface = null;
    relayoutListener = null;
    focusTrap = null;
    entrance = null;
    hostWindow = null;
    focusOwnerBeforeShow = null;
    closing = false;
    clearTransientState();

    if (toRestore != null) {
      SwingUtilities.invokeLater(toRestore::requestFocusInWindow);
    } else {
      closed.repaint();
    }

    if (onClose != null) {
      onClose.accept(cause);
    }
  }

  // Stretches the backdrop over the full layered pane, then delegates surface sizing/centering to
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

  // Keyboard focus trap: while shown, if focus escapes the dialog surface to the now-inert
  // background (still inside the host window), pull it back. Focus moving to a different window is
  // left alone — the host window simply lost activation.
  private void installFocusTrap() {
    focusTrap =
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
          SwingUtilities.invokeLater(this::focusInitial);
        };
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener("focusOwner", focusTrap);
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

  // Depth-first search for the first focus-accepting descendant, skipping container panels.
  static Component firstFocusable(final Container root) {
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

  /**
   * @return whether the Escape key dismisses this dialog (set at build time)
   */
  protected final boolean isDismissibleByEsc() {
    return dismissibleByEsc;
  }
}
