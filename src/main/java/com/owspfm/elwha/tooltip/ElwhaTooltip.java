package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * The M3 tooltip (epic #445) — an overlay handle (not a {@link JComponent}, the {@link
 * com.owspfm.elwha.menu.ElwhaMenu} shape) carrying either the {@linkplain TooltipVariant#PLAIN
 * plain} label bubble or the {@linkplain TooltipVariant#RICH rich} contextual card. Mounts on the
 * host frame's layered pane at {@code POPUP_LAYER} as the overlay host's first <em>passive-focus
 * consumer</em>: a tooltip never takes keyboard focus and never restores it — the anchor keeps
 * focus the entire time it is shown.
 *
 * <p>Construct via {@link #plain(String)}. {@link #attach(JComponent)} installs the desktop trigger
 * machinery — hover dwell ({@link #setShowDelayMs(int)}, 500&nbsp;ms default), hide linger against
 * the anchor&nbsp;∪&nbsp;surface hover union ({@link #setHideDelayMs(int)}, 600&nbsp;ms default),
 * an immediate show on keyboard-caused focus, press-to-dismiss, and teardown when the anchor leaves
 * the hierarchy. {@link #show(Component)} / {@link #dismiss()} stay available for programmatic
 * control (timers are skipped). At most one tooltip is shown at a time across the application; Esc
 * and mouse-wheel input dismiss a showing tooltip (WCAG 1.4.13).
 *
 * <p>{@code attach} neither reads nor clears an existing Swing {@code setToolTipText} — do not
 * double-book an anchor with both mechanisms.
 *
 * <p>Placement prefers {@linkplain TooltipPlacement#ABOVE above} the anchor with a 4&nbsp;px gap,
 * flips below when the top would clip, aligns {@linkplain TooltipAlignment#CENTER flush
 * start/center/end} against the anchor (direction-aware), and clamps to the pane with an 8&nbsp;px
 * edge margin. Design: {@code docs/research/elwha-tooltip-design.md} §5.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTooltip extends AbstractElwhaOverlay {

  /**
   * Gap between the anchor edge and the tooltip body (M3 {@code SpacingBetweenTooltipAndAnchor}).
   */
  static final int ANCHOR_GAP_PX = 4;

  /** Clamp margin between the tooltip body and every pane edge. */
  static final int EDGE_MARGIN_PX = 8;

  /** Hover dwell before a trigger-initiated show (the MDC Web foundation default). */
  static final int DEFAULT_SHOW_DELAY_MS = 500;

  /** Hide linger after the pointer leaves the anchor ∪ surface union (MDC Web default). */
  static final int DEFAULT_HIDE_DELAY_MS = 600;

  // The one-at-a-time slot (Compose MutatorMutex / Swing ToolTipManager parity): showing any
  // tooltip evicts the incumbent.
  private static ElwhaTooltip shownTooltip;

  private final TooltipVariant variant;
  private String text;
  private TooltipPlacement preferredPlacement = TooltipPlacement.ABOVE;
  private TooltipAlignment alignment;
  private int showDelayMs = DEFAULT_SHOW_DELAY_MS;
  private int hideDelayMs = DEFAULT_HIDE_DELAY_MS;
  private TooltipSurface tooltipSurface;
  private JComponent attachedAnchor;
  private TooltipTrigger trigger;
  private AWTEventListener wheelWatch;

  private ElwhaTooltip(final TooltipVariant variant, final String text) {
    this.variant = variant;
    this.text = text;
    this.alignment = TooltipAlignment.CENTER;
  }

  /**
   * Creates a plain tooltip — the label-only inverse-surface bubble that briefly describes a UI
   * element.
   *
   * @param text the label text
   * @return the plain tooltip
   * @throws NullPointerException if {@code text} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTooltip plain(final String text) {
    return new ElwhaTooltip(TooltipVariant.PLAIN, Objects.requireNonNull(text, "text"));
  }

  /**
   * The tooltip's variant.
   *
   * @return the variant
   * @version v0.4.0
   * @since v0.4.0
   */
  public TooltipVariant getVariant() {
    return variant;
  }

  /**
   * The plain label text.
   *
   * @return the label text
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getText() {
    return text;
  }

  /**
   * Replaces the plain label text; a showing tooltip re-wraps and re-places immediately.
   *
   * @param text the new label text
   * @throws NullPointerException if {@code text} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setText(final String text) {
    this.text = Objects.requireNonNull(text, "text");
    if (tooltipSurface != null) {
      tooltipSurface.setText(text);
      relayout();
    }
  }

  /**
   * The preferred vertical side of the anchor (default {@link TooltipPlacement#ABOVE}).
   *
   * @return the preferred placement
   * @version v0.4.0
   * @since v0.4.0
   */
  public TooltipPlacement getPreferredPlacement() {
    return preferredPlacement;
  }

  /**
   * Sets the preferred vertical side; the engine still flips to the opposite side when the
   * preferred one would clip the pane. A showing tooltip re-places immediately.
   *
   * @param placement the preferred placement
   * @throws NullPointerException if {@code placement} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setPreferredPlacement(final TooltipPlacement placement) {
    this.preferredPlacement = Objects.requireNonNull(placement, "placement");
    relayout();
  }

  /**
   * The horizontal alignment against the anchor (plain default {@link TooltipAlignment#CENTER}).
   *
   * @return the alignment
   * @version v0.4.0
   * @since v0.4.0
   */
  public TooltipAlignment getAlignment() {
    return alignment;
  }

  /**
   * Sets the horizontal alignment against the anchor; direction-aware (RTL mirrors {@code
   * START}/{@code END}). A showing tooltip re-places immediately.
   *
   * @param alignment the alignment
   * @throws NullPointerException if {@code alignment} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setAlignment(final TooltipAlignment alignment) {
    this.alignment = Objects.requireNonNull(alignment, "alignment");
    relayout();
  }

  /**
   * Installs the trigger machinery on {@code anchor}: hover dwell → show, hide linger against the
   * anchor&nbsp;∪&nbsp;surface hover union, immediate show on keyboard-caused focus (mouse-click
   * focus stays quiet), press-to-dismiss, and teardown when the anchor leaves the hierarchy. One
   * anchor per tooltip — {@link #detach()} first to move it. Does not touch an existing Swing
   * {@code setToolTipText} on the anchor; do not double-book.
   *
   * @param anchor the component this tooltip describes
   * @return this tooltip, for chaining
   * @throws NullPointerException if {@code anchor} is {@code null}
   * @throws IllegalStateException if already attached
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTooltip attach(final JComponent anchor) {
    Objects.requireNonNull(anchor, "anchor");
    if (attachedAnchor != null) {
      throw new IllegalStateException("already attached to " + attachedAnchor + "; detach() first");
    }
    this.attachedAnchor = anchor;
    this.trigger = new TooltipTrigger(this, anchor);
    trigger.install();
    return this;
  }

  /**
   * Removes the trigger machinery from the attached anchor and dismisses a showing tooltip; a no-op
   * when not attached.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void detach() {
    if (trigger != null) {
      trigger.uninstall();
      trigger = null;
    }
    attachedAnchor = null;
    if (isTooltipShowing()) {
      dismiss();
    }
  }

  /**
   * The anchor this tooltip is attached to, or {@code null} when detached.
   *
   * @return the attached anchor, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public JComponent getAttachedAnchor() {
    return attachedAnchor;
  }

  /**
   * The hover dwell in milliseconds before a trigger-initiated show (default {@value
   * #DEFAULT_SHOW_DELAY_MS}).
   *
   * @return the show delay
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getShowDelayMs() {
    return showDelayMs;
  }

  /**
   * Sets the hover dwell before a trigger-initiated show.
   *
   * @param showDelayMs the dwell in milliseconds
   * @throws IllegalArgumentException if negative
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setShowDelayMs(final int showDelayMs) {
    if (showDelayMs < 0) {
      throw new IllegalArgumentException("showDelayMs must be >= 0: " + showDelayMs);
    }
    this.showDelayMs = showDelayMs;
  }

  /**
   * The hide linger in milliseconds after the pointer leaves the anchor&nbsp;∪&nbsp;surface union
   * (default {@value #DEFAULT_HIDE_DELAY_MS}) — long enough to cross the 4&nbsp;px gap onto the
   * tooltip, which keeps it open.
   *
   * @return the hide delay
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getHideDelayMs() {
    return hideDelayMs;
  }

  /**
   * Sets the hide linger after the pointer leaves the anchor&nbsp;∪&nbsp;surface union.
   *
   * @param hideDelayMs the linger in milliseconds
   * @throws IllegalArgumentException if negative
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setHideDelayMs(final int hideDelayMs) {
    if (hideDelayMs < 0) {
      throw new IllegalArgumentException("hideDelayMs must be >= 0: " + hideDelayMs);
    }
    this.hideDelayMs = hideDelayMs;
  }

  /**
   * Whether the tooltip is currently shown.
   *
   * @return {@code true} between {@link #show(Component)} and full teardown
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isTooltipShowing() {
    return isShowing();
  }

  /**
   * Dismisses a showing tooltip; a no-op otherwise.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void dismiss() {
    beginClose();
  }

  // ------------------------------------------------------- overlay anatomy

  @Override
  protected JComponent createSurface() {
    claimExclusive();
    installWheelWatch();
    this.tooltipSurface = new TooltipSurface(text);
    return tooltipSurface;
  }

  // Esc dismisses from anywhere in the focused window (the tooltip itself never has focus, so a
  // surface-focused binding could never fire). WCAG 1.4.13 "dismissible".
  @Override
  protected void installKeyBindings() {
    final InputMap im = surface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-tooltip-dismiss");
    surface.getActionMap().put("elwha-tooltip-dismiss", action(this::dismiss));
  }

  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    final Rectangle bounds =
        place(
            anchorBoundsInPane(),
            surface.getPreferredSize(),
            new Insets(0, 0, 0, 0),
            paneWidth,
            paneHeight,
            preferredPlacement,
            alignment,
            !orientation.isLeftToRight());
    surface.setBounds(bounds);
    surface.revalidate();
  }

  @Override
  protected String accessibleName() {
    return text;
  }

  @Override
  protected Integer overlayLayer() {
    return JLayeredPane.POPUP_LAYER;
  }

  @Override
  protected boolean lightDismiss() {
    return true;
  }

  @Override
  protected boolean takesFocus() {
    return false;
  }

  @Override
  protected boolean restoreFocusOnClose() {
    return false;
  }

  @Override
  protected void clearTransientState() {
    tooltipSurface = null;
    removeWheelWatch();
    if (shownTooltip == this) {
      shownTooltip = null;
    }
    if (trigger != null) {
      trigger.onTooltipClosed();
    }
  }

  // ------------------------------------------------------- shown-state watchers

  private void claimExclusive() {
    if (shownTooltip != null && shownTooltip != this) {
      shownTooltip.dismiss();
    }
    shownTooltip = this;
  }

  // Wheel input anywhere dismisses: the anchor may scroll out from under the surface, and a stale
  // tooltip floating over moved content is worse than no tooltip. Passive listener, never consumes.
  private void installWheelWatch() {
    removeWheelWatch();
    wheelWatch =
        (final AWTEvent event) -> {
          if (event instanceof MouseEvent me
              && me.getID() == MouseEvent.MOUSE_WHEEL
              && !isClosing()) {
            dismiss();
          }
        };
    Toolkit.getDefaultToolkit().addAWTEventListener(wheelWatch, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
  }

  private void removeWheelWatch() {
    if (wheelWatch != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(wheelWatch);
      wheelWatch = null;
    }
  }

  // The surface bounds on screen, or null when not shown — the trigger's hover-union half.
  Rectangle surfaceScreenBounds() {
    if (surface == null || !surface.isShowing()) {
      return null;
    }
    return new Rectangle(surface.getLocationOnScreen(), surface.getSize());
  }

  // ------------------------------------------------------- placement engine

  /**
   * Computes the tooltip surface bounds against the anchor: the <em>body</em> (the surface minus
   * its {@code halo} shadow reserve) sits {@value #ANCHOR_GAP_PX}&nbsp;px off the preferred
   * vertical side, flipping to the other side when the preferred one would cross the {@value
   * #EDGE_MARGIN_PX}&nbsp;px pane margin; horizontally the body aligns flush start/center/end with
   * the same anchor edge (start/end resolving through {@code rtl}), then clamps to the margins. A
   * pure function so the geometry is testable without a realized window.
   *
   * @param anchor the anchor bounds in pane coordinates
   * @param pref the surface's preferred size, halo included
   * @param halo the surface's shadow reserve (zero for the flat plain variant)
   * @param paneWidth the viewport width
   * @param paneHeight the viewport height
   * @param preferred the preferred vertical side
   * @param alignment the horizontal alignment
   * @param rtl {@code true} when the orientation is right-to-left
   * @return the surface bounds within the viewport
   */
  static Rectangle place(
      final Rectangle anchor,
      final Dimension pref,
      final Insets halo,
      final int paneWidth,
      final int paneHeight,
      final TooltipPlacement preferred,
      final TooltipAlignment alignment,
      final boolean rtl) {
    final int bodyW = Math.min(pref.width - halo.left - halo.right, paneWidth - 2 * EDGE_MARGIN_PX);
    final int bodyH = pref.height - halo.top - halo.bottom;

    int x;
    switch (alignment) {
      case START:
        x = rtl ? anchor.x + anchor.width - bodyW : anchor.x;
        break;
      case END:
        x = rtl ? anchor.x : anchor.x + anchor.width - bodyW;
        break;
      case CENTER:
      default:
        x = anchor.x + (anchor.width - bodyW) / 2;
        break;
    }
    x = Math.max(EDGE_MARGIN_PX, Math.min(x, paneWidth - bodyW - EDGE_MARGIN_PX));

    final int above = anchor.y - ANCHOR_GAP_PX - bodyH;
    final int below = anchor.y + anchor.height + ANCHOR_GAP_PX;
    final boolean aboveFits = above >= EDGE_MARGIN_PX;
    final boolean belowFits = below + bodyH <= paneHeight - EDGE_MARGIN_PX;
    int y;
    if (preferred == TooltipPlacement.ABOVE) {
      y = aboveFits || !belowFits ? above : below;
    } else {
      y = belowFits || !aboveFits ? below : above;
    }
    y = Math.max(EDGE_MARGIN_PX, Math.min(y, paneHeight - bodyH - EDGE_MARGIN_PX));

    return new Rectangle(x - halo.left, y - halo.top, pref.width, pref.height);
  }

  // The anchor's bounds in the layered pane's coordinate space; degrades to the pane's top-left
  // when the anchor is detached, mirroring the menu host.
  private Rectangle anchorBoundsInPane() {
    if (anchor == null || layeredPane == null || !anchor.isShowing()) {
      return new Rectangle(0, 0, 0, 0);
    }
    final Point origin = SwingUtilities.convertPoint(anchor, 0, 0, layeredPane);
    return new Rectangle(origin.x, origin.y, anchor.getWidth(), anchor.getHeight());
  }
}
