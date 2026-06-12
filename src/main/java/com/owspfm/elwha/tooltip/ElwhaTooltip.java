package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import com.owspfm.elwha.theme.ShadowBearing;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
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

  /** Entrance fade duration (material-web/MDC transition). */
  static final int ENTER_MS = 150;

  /** Exit fade duration (material-web/MDC transition). */
  static final int EXIT_MS = 75;

  // The one-at-a-time slot (Compose MutatorMutex / Swing ToolTipManager parity): showing any
  // tooltip evicts the incumbent.
  private static ElwhaTooltip shownTooltip;

  private final TooltipVariant variant;
  private final List<TooltipAction> actions;
  private String text;
  private String subhead;
  private String supportingText;
  private boolean persistent;
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
    this.actions = List.of();
    this.alignment = TooltipAlignment.CENTER;
  }

  private ElwhaTooltip(final RichBuilder builder) {
    this.variant = TooltipVariant.RICH;
    this.subhead = builder.subhead;
    this.supportingText = builder.supportingText;
    this.actions = List.copyOf(builder.actions);
    this.persistent = builder.persistent;
    this.alignment = TooltipAlignment.END;
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
   * Starts a rich tooltip — the surface-container card with an optional subhead, supporting text,
   * and optional text-button actions.
   *
   * @return the rich builder
   * @version v0.4.0
   * @since v0.4.0
   */
  public static RichBuilder rich() {
    return new RichBuilder();
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
   * The plain label text; {@code null} on a rich tooltip.
   *
   * @return the label text, or {@code null}
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
   * @throws IllegalStateException on a rich tooltip — rich content flows through {@link
   *     #setSubhead(String)} / {@link #setSupportingText(String)}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setText(final String text) {
    if (variant != TooltipVariant.PLAIN) {
      throw new IllegalStateException(
          "setText is plain-only; use setSubhead/setSupportingText on a rich tooltip");
    }
    this.text = Objects.requireNonNull(text, "text");
    syncAnchorDescription();
    if (tooltipSurface != null) {
      tooltipSurface.setText(text);
      relayout();
    }
  }

  /**
   * The rich subhead; {@code null} on a plain tooltip or when no subhead was set.
   *
   * @return the subhead, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getSubhead() {
    return subhead;
  }

  /**
   * Replaces the rich subhead ({@code null} removes it); a showing tooltip re-flows immediately.
   *
   * @param subhead the new subhead, or {@code null} for none
   * @throws IllegalStateException on a plain tooltip
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSubhead(final String subhead) {
    requireRich("setSubhead");
    this.subhead = subhead;
    syncAnchorDescription();
    if (tooltipSurface != null) {
      tooltipSurface.setSubhead(subhead);
      relayout();
    }
  }

  /**
   * The rich supporting text; {@code null} on a plain tooltip.
   *
   * @return the supporting text, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getSupportingText() {
    return supportingText;
  }

  /**
   * Replaces the rich supporting text; a showing tooltip re-flows immediately.
   *
   * @param supportingText the new supporting text
   * @throws NullPointerException if {@code supportingText} is {@code null}
   * @throws IllegalStateException on a plain tooltip
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSupportingText(final String supportingText) {
    requireRich("setSupportingText");
    this.supportingText = Objects.requireNonNull(supportingText, "supportingText");
    syncAnchorDescription();
    if (tooltipSurface != null) {
      tooltipSurface.setSupportingText(supportingText);
      relayout();
    }
  }

  private void requireRich(final String method) {
    if (variant != TooltipVariant.RICH) {
      throw new IllegalStateException(method + " is rich-only; this tooltip is " + variant);
    }
  }

  /**
   * Whether this rich tooltip is the persistent flavor; always {@code false} on plain.
   *
   * @return {@code true} for persistent rich
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isPersistent() {
    return persistent;
  }

  /**
   * Switches the rich tooltip between the default and persistent flavors. Persistent tooltips are
   * <em>toggled</em> by an anchor click (or Enter/Space on a focused anchor) instead of shown by
   * hover/focus, stay through arbitrary hovering, and dismiss only on a press outside the contents,
   * Esc, wheel input, an action click, or a re-toggle.
   *
   * <p>MDC's pairing guidance, verbatim: persistent rich tooltips are <em>"recommended against
   * pairing with anchor elements that have click actions"</em> — the toggle and the anchor's own
   * action would both fire.
   *
   * @param persistent {@code true} for the persistent flavor
   * @throws IllegalStateException on a plain tooltip — M3 defines persistence for rich only
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setPersistent(final boolean persistent) {
    requireRich("setPersistent");
    this.persistent = persistent;
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
    syncAnchorDescription();
    return this;
  }

  /**
   * Removes the trigger machinery from the attached anchor, clears the accessible description it
   * wrote (only if still ours), and dismisses a showing tooltip; a no-op when not attached.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void detach() {
    if (attachedAnchor != null
        && Objects.equals(
            attachedAnchor.getAccessibleContext().getAccessibleDescription(), accessibleName())) {
      attachedAnchor.getAccessibleContext().setAccessibleDescription(null);
    }
    if (trigger != null) {
      trigger.uninstall();
      trigger = null;
    }
    attachedAnchor = null;
    if (isTooltipShowing()) {
      dismiss();
    }
  }

  // The aria-describedby analogue — the same wiring Swing's own setToolTipText performs. Re-run
  // on every content change so the description tracks the visible text.
  private void syncAnchorDescription() {
    if (attachedAnchor != null) {
      attachedAnchor.getAccessibleContext().setAccessibleDescription(accessibleName());
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
   * Dismisses a showing tooltip (75&nbsp;ms exit fade; reduced motion snaps); a no-op otherwise.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void dismiss() {
    if (entrance != null) {
      entrance.setDurationMs(EXIT_MS);
    }
    beginClose();
  }

  // ------------------------------------------------------- overlay anatomy

  // M3 tooltip motion is fade-dominant and fast — 150ms in / 75ms out on the standard curve
  // (research §I); the surface composites itself and its children by motionProgress.

  @Override
  protected int motionDurationMs() {
    return ENTER_MS;
  }

  @Override
  protected com.owspfm.elwha.theme.Easing easing() {
    return com.owspfm.elwha.theme.Easing.STANDARD;
  }

  // Every dismiss path funnels through dismiss() so the 75ms exit retune is never skipped.
  @Override
  protected void onOutsidePress() {
    dismiss();
  }

  @Override
  protected JComponent createSurface() {
    claimExclusive();
    installWheelWatch();
    this.tooltipSurface = buildSurface(true);
    return tooltipSurface;
  }

  /**
   * Renders the tooltip's surface as a standalone component for a <em>static preview</em> (a
   * Showcase gallery tile, documentation) — the {@link com.owspfm.elwha.menu.ElwhaMenu} {@code
   * renderPreview} contract. There is no overlay mount, no trigger machinery, no dismissal, and no
   * fade; action buttons fire their consumer listeners directly. Not a substitute for {@link
   * #show(Component)}. Each call returns a fresh component.
   *
   * @return a non-modal render of the tooltip surface
   * @version v0.4.0
   * @since v0.4.0
   */
  public JComponent renderPreview() {
    return buildSurface(false);
  }

  // The shared surface build. A live surface fades with motionProgress, wraps action listeners
  // with dismiss-then-fire (the consumer's handler may open a dialog — design §7), and dismisses
  // non-persistent tooltips on a press inside the contents; a preview carries none of that.
  private TooltipSurface buildSurface(final boolean live) {
    final TooltipSurface built = new TooltipSurface(variant, text, subhead, supportingText);
    if (live) {
      built.setAlphaSupplier(() -> motionProgress);
    }
    for (final TooltipAction action : actions) {
      final ElwhaButton button = ElwhaButton.textButton(action.label());
      button.addActionListener(
          live
              ? e -> {
                dismiss();
                action.listener().actionPerformed(e);
              }
              : action.listener());
      built.addActionButton(button);
    }
    if (live) {
      // The action buttons consume their own presses and never reach this.
      built.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
              if (!persistent) {
                dismiss();
              }
            }
          });
    }
    return built;
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
            tooltipSurface != null ? tooltipSurface.halo() : new Insets(0, 0, 0, 0),
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
    if (variant == TooltipVariant.PLAIN) {
      return text;
    }
    return subhead == null || subhead.isEmpty() ? supportingText : subhead + ". " + supportingText;
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

  // The attached anchor counts as "inside" for the host's outside-press routing (the
  // editable-combobox pattern): anchor presses are the trigger's sole authority — the persistent
  // toggle — and must not race the host's own dismiss, which would re-show under reduced motion
  // (teardown completes synchronously before the anchor's mouse listener runs).
  @Override
  protected boolean ownsFocus(final Component c) {
    return super.ownsFocus(c)
        || (attachedAnchor != null && SwingUtilities.isDescendingFrom(c, attachedAnchor));
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
  // when the anchor is detached, mirroring the menu host. A ShadowBearing anchor's halo reserve is
  // backed out — the shadow is not visual anchor, and leaving it in reads as a too-wide gap
  // (exactly the correction the engine already applies to the tooltip's own halo). Note the
  // residual: placement measures from the component bounds — a component stretched by a fill
  // layout beyond its preferred size anchors at its stretched edge, not its centered body.
  private Rectangle anchorBoundsInPane() {
    if (anchor == null || layeredPane == null || !anchor.isShowing()) {
      return new Rectangle(0, 0, 0, 0);
    }
    final Point origin = SwingUtilities.convertPoint(anchor, 0, 0, layeredPane);
    final Rectangle bounds =
        new Rectangle(origin.x, origin.y, anchor.getWidth(), anchor.getHeight());
    if (anchor instanceof ShadowBearing bearing) {
      final Insets halo = bearing.getShadowInsets();
      bounds.x += halo.left;
      bounds.y += halo.top;
      bounds.width = Math.max(0, bounds.width - halo.left - halo.right);
      bounds.height = Math.max(0, bounds.height - halo.top - halo.bottom);
    }
    return bounds;
  }

  // A rich action: the text-button label and the consumer's listener.
  private record TooltipAction(String label, ActionListener listener) {}

  /**
   * Fluent builder for a {@linkplain TooltipVariant#RICH rich} {@link ElwhaTooltip}. Supporting
   * text is required; subhead, actions, and persistence are optional.
   *
   * @author Charles Bryan (cfb3@uw.edu)
   * @version v0.4.0
   * @since v0.4.0
   */
  public static final class RichBuilder {

    private String subhead;
    private String supportingText;
    private final List<TooltipAction> actions = new ArrayList<>();
    private boolean persistent;

    private RichBuilder() {}

    /**
     * Sets the optional subhead — the {@code TITLE_SMALL} first line.
     *
     * @param subhead the subhead text
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public RichBuilder subhead(final String subhead) {
      this.subhead = subhead;
      return this;
    }

    /**
     * Sets the supporting text — the {@code BODY_MEDIUM} paragraph. Required.
     *
     * @param supportingText the supporting text
     * @return this builder
     * @throws NullPointerException if {@code supportingText} is {@code null}
     * @version v0.4.0
     * @since v0.4.0
     */
    public RichBuilder supportingText(final String supportingText) {
      this.supportingText = Objects.requireNonNull(supportingText, "supportingText");
      return this;
    }

    /**
     * Adds an action — a {@code PRIMARY} text button on the bottom-start action row. Clicking an
     * action dismisses the tooltip and then fires {@code onAction}.
     *
     * @param label the button label
     * @param onAction the consumer's listener
     * @return this builder
     * @throws NullPointerException if either argument is {@code null}
     * @version v0.4.0
     * @since v0.4.0
     */
    public RichBuilder action(final String label, final ActionListener onAction) {
      actions.add(
          new TooltipAction(
              Objects.requireNonNull(label, "label"),
              Objects.requireNonNull(onAction, "onAction")));
      return this;
    }

    /**
     * Marks the tooltip persistent — toggled by anchor click / Enter / Space instead of hover, and
     * dismissed only by outside-press, Esc, wheel, an action click, or a re-toggle.
     *
     * @param persistent {@code true} for the persistent flavor
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public RichBuilder persistent(final boolean persistent) {
      this.persistent = persistent;
      return this;
    }

    /**
     * Builds the rich tooltip.
     *
     * @return the rich tooltip
     * @throws IllegalStateException if no supporting text was set
     * @version v0.4.0
     * @since v0.4.0
     */
    public ElwhaTooltip build() {
      if (supportingText == null) {
        throw new IllegalStateException("supportingText is required on a rich tooltip");
      }
      return new ElwhaTooltip(this);
    }
  }
}
