package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.FocusVisible;
import java.awt.AWTEvent;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The per-anchor trigger machinery behind {@link ElwhaTooltip#attach(JComponent)} — the MDC Web
 * desktop interaction model ({@code elwha-tooltip-research.md} §I): a hover <em>dwell</em> timer
 * before showing, a hide <em>linger</em> timer that only expires while the pointer is outside the
 * anchor ∪ surface union (hovering the tooltip itself keeps it open — WCAG 1.4.13), an immediate
 * show on keyboard-caused focus ({@link FocusVisible} gating, so mouse-click focus stays quiet), a
 * press-to-dismiss on the anchor, and teardown when the anchor leaves the hierarchy. Timers are the
 * {@code ElwhaSubMenuItem} idiom — single-shot {@link Timer}s re-armed by a global hover watch
 * while the tooltip is shown.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
final class TooltipTrigger {

  private final ElwhaTooltip tooltip;
  private final JComponent anchor;
  private final Timer showTimer;
  private final Timer hideTimer;
  private final MouseListener mouseListener;
  private final FocusListener focusListener;
  private final HierarchyListener hierarchyListener;
  private final KeyListener keyListener;
  private AWTEventListener hoverWatch;
  private boolean keyboardFocusActive;
  private boolean shownByTrigger;

  TooltipTrigger(final ElwhaTooltip tooltip, final JComponent anchor) {
    this.tooltip = tooltip;
    this.anchor = anchor;
    this.showTimer = new Timer(tooltip.getShowDelayMs(), e -> showNow());
    showTimer.setRepeats(false);
    this.hideTimer = new Timer(tooltip.getHideDelayMs(), e -> lingerExpired());
    hideTimer.setRepeats(false);
    this.mouseListener =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            pointerEntered();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            pointerExited();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            anchorPressed();
          }
        };
    this.focusListener =
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {
            if (!tooltip.isPersistent() && FocusVisible.isKeyboardCause(e.getCause())) {
              keyboardFocusActive = true;
              showTimer.stop();
              hideTimer.stop();
              showNow();
            }
          }

          @Override
          public void focusLost(final FocusEvent e) {
            keyboardFocusActive = false;
            if (!tooltip.isPersistent()
                && shownByTrigger
                && tooltip.isTooltipShowing()
                && !pointerInUnion()) {
              tooltip.dismiss();
            }
          }
        };
    this.keyListener =
        new KeyAdapter() {
          @Override
          public void keyPressed(final KeyEvent e) {
            if (tooltip.isPersistent()
                && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
              toggle();
            }
          }
        };
    this.hierarchyListener =
        e -> {
          final long flags = HierarchyEvent.SHOWING_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED;
          if ((e.getChangeFlags() & flags) != 0 && !anchor.isShowing()) {
            showTimer.stop();
            hideTimer.stop();
            if (tooltip.isTooltipShowing()) {
              tooltip.dismiss();
            }
          }
        };
  }

  void install() {
    anchor.addMouseListener(mouseListener);
    anchor.addFocusListener(focusListener);
    anchor.addHierarchyListener(hierarchyListener);
    anchor.addKeyListener(keyListener);
  }

  void uninstall() {
    showTimer.stop();
    hideTimer.stop();
    anchor.removeMouseListener(mouseListener);
    anchor.removeFocusListener(focusListener);
    anchor.removeHierarchyListener(hierarchyListener);
    anchor.removeKeyListener(keyListener);
    removeHoverWatch();
  }

  /** Teardown callback from the tooltip — the show this trigger initiated is gone. */
  void onTooltipClosed() {
    shownByTrigger = false;
    hideTimer.stop();
    removeHoverWatch();
  }

  private void pointerEntered() {
    if (tooltip.isPersistent()) {
      return;
    }
    hideTimer.stop();
    if (!tooltip.isTooltipShowing()) {
      showTimer.setInitialDelay(tooltip.getShowDelayMs());
      showTimer.restart();
    }
  }

  private void pointerExited() {
    if (tooltip.isPersistent()) {
      return;
    }
    showTimer.stop();
    if (shownByTrigger && tooltip.isTooltipShowing()) {
      armLinger();
    }
  }

  // Persistent: the anchor press IS the toggle (mousePressed, not clicked — macOS drops
  // MOUSE_CLICKED under rapid clicks, #299). Non-persistent: the user is acting on the control —
  // the tooltip is noise now (desktop adaptation, design doc §7).
  private void anchorPressed() {
    if (tooltip.isPersistent()) {
      toggle();
      return;
    }
    showTimer.stop();
    if (tooltip.isTooltipShowing()) {
      tooltip.dismiss();
    }
  }

  private void toggle() {
    if (tooltip.isTooltipShowing()) {
      tooltip.dismiss();
    } else {
      showNow();
    }
  }

  private void showNow() {
    showTimer.stop();
    if (tooltip.isTooltipShowing() || !anchor.isShowing()) {
      hideTimer.stop();
      return;
    }
    shownByTrigger = true;
    tooltip.show(anchor);
    if (!tooltip.isTooltipShowing()) {
      shownByTrigger = false;
    } else if (!tooltip.isPersistent()) {
      // Persistent tooltips ignore hover entirely — no linger machinery to arm.
      installHoverWatch();
    }
  }

  private void lingerExpired() {
    if (keyboardFocusActive && anchor.isFocusOwner()) {
      return;
    }
    if (shownByTrigger && tooltip.isTooltipShowing() && !pointerInUnion()) {
      tooltip.dismiss();
    }
  }

  private void armLinger() {
    hideTimer.setInitialDelay(tooltip.getHideDelayMs());
    hideTimer.restart();
  }

  // While a trigger-initiated show is up, every pointer move arms or disarms the linger against
  // the anchor ∪ surface union — the ElwhaSubMenuItem hover-watch idiom. Passive: never consumes.
  private void installHoverWatch() {
    removeHoverWatch();
    hoverWatch =
        (final AWTEvent event) -> {
          if (!(event instanceof MouseEvent me)) {
            return;
          }
          final int id = me.getID();
          if (id != MouseEvent.MOUSE_MOVED
              && id != MouseEvent.MOUSE_DRAGGED
              && id != MouseEvent.MOUSE_ENTERED
              && id != MouseEvent.MOUSE_EXITED) {
            return;
          }
          if (!shownByTrigger || !tooltip.isTooltipShowing()) {
            return;
          }
          if (inUnion(me.getLocationOnScreen())) {
            hideTimer.stop();
          } else if (!hideTimer.isRunning()) {
            armLinger();
          }
        };
    Toolkit.getDefaultToolkit()
        .addAWTEventListener(
            hoverWatch, AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
  }

  private void removeHoverWatch() {
    if (hoverWatch != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(hoverWatch);
      hoverWatch = null;
    }
  }

  private boolean pointerInUnion() {
    final PointerInfo info = MouseInfo.getPointerInfo();
    return info != null && inUnion(info.getLocation());
  }

  private boolean inUnion(final Point screenPoint) {
    if (anchor.isShowing()) {
      final Rectangle anchorOnScreen =
          new Rectangle(anchor.getLocationOnScreen(), anchor.getSize());
      if (anchorOnScreen.contains(screenPoint)) {
        return true;
      }
    }
    final Rectangle surfaceOnScreen = tooltip.surfaceScreenBounds();
    return surfaceOnScreen != null && surfaceOnScreen.contains(screenPoint);
  }
}
