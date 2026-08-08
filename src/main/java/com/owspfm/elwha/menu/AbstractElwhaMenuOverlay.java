package com.owspfm.elwha.menu;

import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import com.owspfm.elwha.theme.BodyBearing;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Package-private menu/popover host — the light-dismiss + anchored specialization of {@link
 * AbstractElwhaOverlay} that {@link ElwhaMenu} extends. Pins the menu strategy axes (design doc
 * §2):
 *
 * <ul>
 *   <li><strong>z-band</strong> — {@link JLayeredPane#POPUP_LAYER} (300), so a menu opened from
 *       inside a dialog ({@code MODAL_LAYER}, 200) or above an Elwha overlay ({@code
 *       ElwhaLayers.OVERLAY_LAYER}, 190) tops them.
 *   <li><strong>dismiss policy</strong> — {@linkplain #lightDismiss() light dismiss}: a mouse press
 *       outside the surface, a focus escape, or Escape closes the menu; there is no scrim.
 *   <li><strong>placement</strong> — anchored below the trigger, leading-aligned, with a vertical
 *       flip (open upward) and horizontal shift when the window edge would clip the surface.
 * </ul>
 *
 * <p>Focus follows the menu contract: initial focus to the first item, trap-while-open inherited
 * from the base, and focus restored to the trigger only on an intentional close (Escape, selection,
 * programmatic) — never on a focus-loss/outside-press close, where yanking focus back would fight
 * the user.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.4.0
 */
abstract class AbstractElwhaMenuOverlay extends AbstractElwhaOverlay {

  /** Gap between the trigger's edge and the menu surface (M3 anchored offset). */
  static final int ANCHOR_GAP_PX = 4;

  /**
   * Side gap between a submenu's opener item and the submenu surface. {@code 0} so the surface
   * bounds abut the item — the surfaces' own shadow reserve supplies the visual separation,
   * matching the M3 "opens next to the parent item without overlapping" rule.
   */
  static final int SUBMENU_GAP_PX = 0;

  private final Consumer<MenuDismissCause> onClose;
  private MenuDismissCause lastCause;

  /**
   * @param onClose the close hook fired after teardown with the dismiss cause, or {@code null}
   */
  protected AbstractElwhaMenuOverlay(final Consumer<MenuDismissCause> onClose) {
    this.onClose = onClose;
  }

  // -------------------------------------------------- strategy axes (locked by S1)

  @Override
  protected final Integer overlayLayer() {
    return JLayeredPane.POPUP_LAYER;
  }

  @Override
  protected final boolean lightDismiss() {
    return true;
  }

  @Override
  protected void onFocusEscaped() {
    close(MenuDismissCause.FOCUS_LOST);
  }

  @Override
  protected void onOutsidePress() {
    close(MenuDismissCause.OUTSIDE_PRESS);
  }

  @Override
  protected final boolean restoreFocusOnClose() {
    return restoresFocus(lastCause);
  }

  /**
   * Whether a close with the given cause restores focus to the trigger. Intentional closes (Escape,
   * selection, programmatic) restore; a focus-loss or outside-press close does not — focus already
   * moved where the user wanted it. Pure function so the policy is testable.
   *
   * @param cause the dismiss cause
   * @return {@code true} to restore focus to the trigger
   */
  static boolean restoresFocus(final MenuDismissCause cause) {
    return cause == MenuDismissCause.ESCAPE
        || cause == MenuDismissCause.SELECTION
        || cause == MenuDismissCause.PROGRAMMATIC;
  }

  /**
   * The component that keeps keyboard focus while this menu is open instead of the menu surface —
   * the editable-combobox pattern. {@code null} (the default) means the surface itself takes focus.
   *
   * @return the external focus home, or {@code null}
   */
  protected Component focusHomeComponent() {
    return null;
  }

  // Esc is a host-level dismiss (the menu layers item navigation on top). Bound WHEN_FOCUSED on the
  // surface (the menu's single focus owner) rather than WHEN_IN_FOCUSED_WINDOW so that, in a
  // submenu
  // chain, Esc is owned by the focused (leaf) level only — closing one level at a time — instead of
  // an ambiguous window-wide binding that could collapse the wrong level.
  @Override
  protected void installKeyBindings() {
    final InputMap im = surface.getInputMap(JComponent.WHEN_FOCUSED);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-menu-dismiss");
    surface.getActionMap().put("elwha-menu-dismiss", action(() -> close(MenuDismissCause.ESCAPE)));
    if (focusHomeComponent() == null) {
      return;
    }
    // A focus-home menu never gives its surface focus, so the binding above is unreachable on one —
    // and ElwhaSelectField documents Escape-closes in three places, one of them naming this very
    // map (#579). Add the window-wide twin for that case only, so the chain-leaf precision above is
    // untouched everywhere else. Topmost-gated (#599): a window-wide binding resolves by
    // registration recency, not z-order, and a submenu opened from this level must still take
    // Escape one level at a time.
    surface
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-menu-dismiss-window");
    surface
        .getActionMap()
        .put("elwha-menu-dismiss-window", topmostAction(() -> close(MenuDismissCause.ESCAPE)));
  }

  /**
   * Closes the entire overlay chain this menu belongs to — from the root, with {@code cause} — so a
   * selection or a press outside every level dismisses all open levels at once. For a chainless
   * menu this is exactly {@link #close(MenuDismissCause)}.
   *
   * @param cause why the chain is closing
   */
  protected final void closeChain(final MenuDismissCause cause) {
    if (chainRootOverlay() instanceof AbstractElwhaMenuOverlay root) {
      root.close(cause);
    } else {
      close(cause);
    }
  }

  // ------------------------------------------------------- close / cause

  /**
   * Records the dismiss cause and begins closing. Idempotent via the base's re-entry guard.
   *
   * @param cause why the menu is closing
   */
  protected final void close(final MenuDismissCause cause) {
    if (isClosing() || !isShowing()) {
      return;
    }
    this.lastCause = cause;
    beginClose();
  }

  @Override
  protected final void onClosed() {
    if (onClose != null) {
      onClose.accept(lastCause);
    }
  }

  // ------------------------------------------------------- anchored placement

  /**
   * Places the menu surface anchored to the trigger ({@link #anchor}), leading-aligned below it,
   * flipping above and shifting horizontally to stay within the layered-pane viewport.
   *
   * @param paneWidth the layered pane width
   * @param paneHeight the layered pane height
   */
  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    final Rectangle anchorBounds = anchorBoundsInPane();
    final boolean rtl = !orientation.isLeftToRight();
    final Rectangle bounds =
        sideAnchored()
            ? placeBeside(anchorBounds, surface.getPreferredSize(), paneWidth, paneHeight, rtl)
            : placeAnchored(anchorBounds, surface.getPreferredSize(), paneWidth, paneHeight, rtl);
    surface.setBounds(bounds);
    surface.revalidate();
  }

  /**
   * Whether this overlay anchors <em>beside</em> its anchor (a submenu, opening to the side) rather
   * than below it (a root menu). Default {@code false}; {@link ElwhaMenu} returns {@code true} when
   * it is itself a submenu in an overlay chain.
   *
   * @return {@code true} to place beside the anchor (M3 {@code START_END}), {@code false} to place
   *     below it
   */
  protected boolean sideAnchored() {
    return false;
  }

  /**
   * Computes a submenu surface's bounds anchored <em>beside</em> its opener item (M3 {@code
   * START_END}): top-aligned to the opener, opening to the trailing side, flipping to the leading
   * side when the trailing side would clip the viewport, and shifted vertically to stay inside the
   * {@code paneWidth × paneHeight} viewport. RTL mirrors leading/trailing. A pure function so the
   * side-placement geometry is testable without a realized window.
   *
   * @param anchor the opener item bounds in pane coordinates
   * @param pref the surface's preferred size
   * @param paneWidth the viewport width
   * @param paneHeight the viewport height
   * @param rtl {@code true} when the orientation is right-to-left
   * @return the surface bounds within the viewport
   */
  static Rectangle placeBeside(
      final Rectangle anchor,
      final Dimension pref,
      final int paneWidth,
      final int paneHeight,
      final boolean rtl) {
    final int w = Math.min(pref.width, paneWidth);
    final int h = Math.min(pref.height, paneHeight);

    final int trailingX =
        rtl ? anchor.x - SUBMENU_GAP_PX - w : anchor.x + anchor.width + SUBMENU_GAP_PX;
    final int leadingX =
        rtl ? anchor.x + anchor.width + SUBMENU_GAP_PX : anchor.x - SUBMENU_GAP_PX - w;

    final boolean trailingClips = rtl ? trailingX < 0 : trailingX + w > paneWidth;
    final boolean leadingFits = rtl ? leadingX + w <= paneWidth : leadingX >= 0;
    int x = trailingClips && leadingFits ? leadingX : trailingX;
    x = Math.max(0, Math.min(x, paneWidth - w));

    int y = anchor.y;
    if (y + h > paneHeight) {
      y = paneHeight - h;
    }
    y = Math.max(0, y);
    return new Rectangle(x, y, w, h);
  }

  /**
   * The vertical room an anchored menu may take without covering the anchor it belongs to.
   *
   * <p>A scrollable menu used to size its viewport to the whole layered pane less the margin, and
   * {@link #placeAnchored} then clamped the too-tall surface back inside the viewport — which put
   * it over the trigger. For a plain menu that is cosmetic; for the editable {@code
   * ElwhaSelectField} combo it is a usability failure, because the open list buries the field the
   * user is typing into and they cannot see what is filtering the list.
   *
   * <p>The bound is therefore the larger of the space <em>below</em> the anchor and the space
   * <em>above</em> it — whichever way {@code placeAnchored} ends up flipping, the surface fits on
   * that side and the anchor row stays visible. A {@linkplain #sideAnchored() side-anchored}
   * submenu is exempt: it opens beside its opener, so covering it vertically is not the failure
   * mode, and the viewport is the only real bound.
   *
   * @param margin the viewport margin the menu keeps clear of the pane edges, in pixels
   * @return the height bound in pixels, or {@link Integer#MAX_VALUE} while there is no pane to
   *     measure against
   * @version v0.5.0
   * @since v0.5.0
   */
  protected final int anchoredAvailableHeight(final int margin) {
    if (layeredPane == null) {
      return Integer.MAX_VALUE;
    }
    final int viewport = layeredPane.getHeight() - 2 * margin;
    if (sideAnchored()) {
      return viewport;
    }
    final Rectangle bounds = anchorBoundsInPane();
    if (bounds.height <= 0) {
      return viewport;
    }
    final int below = layeredPane.getHeight() - margin - (bounds.y + bounds.height + ANCHOR_GAP_PX);
    final int above = bounds.y - ANCHOR_GAP_PX - margin;
    return Math.max(0, Math.min(viewport, Math.max(below, above)));
  }

  /**
   * Computes the menu surface bounds anchored to a trigger: leading-aligned below it, flipping
   * above when the surface would clip the bottom edge (and there is room above), and shifted
   * horizontally to stay inside the {@code paneWidth × paneHeight} viewport. A pure function of its
   * inputs so the placement geometry is testable without a realized window.
   *
   * @param anchor the trigger bounds in pane coordinates
   * @param pref the surface's preferred size
   * @param paneWidth the viewport width
   * @param paneHeight the viewport height
   * @param rtl {@code true} when the orientation is right-to-left (trailing-aligned)
   * @return the surface bounds within the viewport
   */
  static Rectangle placeAnchored(
      final Rectangle anchor,
      final Dimension pref,
      final int paneWidth,
      final int paneHeight,
      final boolean rtl) {
    final int w = Math.min(pref.width, paneWidth);
    final int h = Math.min(pref.height, paneHeight);

    int x = rtl ? anchor.x + anchor.width - w : anchor.x;
    if (x + w > paneWidth) {
      x = paneWidth - w;
    }
    x = Math.max(0, x);

    int y = anchor.y + anchor.height + ANCHOR_GAP_PX;
    final boolean clipsBelow = y + h > paneHeight;
    final boolean roomAbove = anchor.y - ANCHOR_GAP_PX - h >= 0;
    if (clipsBelow && roomAbove) {
      y = anchor.y - ANCHOR_GAP_PX - h;
    } else {
      y = Math.max(0, Math.min(y, paneHeight - h));
    }
    return new Rectangle(x, y, w, h);
  }

  // The trigger's VISIBLE BODY in the layered pane's coordinate space. Falls back to the pane's
  // top-left when the anchor is somehow detached, so placement degrades gracefully rather than
  // throwing. Measuring from the body rather than the bounds is what stops a menu opening off the
  // bottom of an invisible rect when a fill layout stretched its trigger, and it also backs out a
  // shadow halo this host never handled at all (#493).
  private Rectangle anchorBoundsInPane() {
    if (anchor == null || layeredPane == null || !anchor.isShowing()) {
      return new Rectangle(0, 0, 0, 0);
    }
    final Rectangle body = BodyBearing.bodyBoundsOf(anchor);
    final Point origin = SwingUtilities.convertPoint(anchor, body.x, body.y, layeredPane);
    return new Rectangle(origin.x, origin.y, body.width, body.height);
  }
}
