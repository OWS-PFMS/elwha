package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * The popover's overlay host (V2 design doc {@code elwha-color-picker-v2-design.md} §5): a
 * light-dismiss {@code AbstractElwhaOverlay} on {@code POPUP_LAYER} whose surface is the dialog's
 * container treatment (SURFACE_CONTAINER_HIGH, XL corners, level-3 shadow via {@code ElwhaSurface})
 * wrapping the popover's embedded picker — the {@code menu/} package-boundary pattern keeping
 * {@code AbstractElwhaOverlay} out of the public API.
 *
 * <p>Placement is the pure {@link #place} function: below the anchor with leading edges aligned
 * (trailing under RTL), flipping above when below would clip, shifted to stay inside the pane —
 * computed against the surface's <em>body</em> rectangle, not its shadow-inclusive bounds (the
 * shadow-reserve doctrine).
 *
 * <p>While the embedded picker's screen sampler is open, {@link #ownsFocus} answers {@code true}
 * for everything — the sampler's always-on-top capture windows must not light-dismiss the popover
 * out from under the pick (design §4).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class PopoverHost extends com.owspfm.elwha.overlay.AbstractElwhaOverlay {

  private static final int ELEVATION = 3;

  private final ElwhaColorPickerPopover owner;

  PopoverHost(final ElwhaColorPickerPopover owner) {
    this.owner = owner;
  }

  static Rectangle place(
      final Dimension pane,
      final Rectangle anchorBounds,
      final Dimension surfaceSize,
      final Insets shadow,
      final int gap,
      final boolean ltr) {
    final int bodyWidth = surfaceSize.width - shadow.left - shadow.right;
    final int bodyHeight = surfaceSize.height - shadow.top - shadow.bottom;
    int bodyX = ltr ? anchorBounds.x : anchorBounds.x + anchorBounds.width - bodyWidth;
    int bodyY = anchorBounds.y + anchorBounds.height + gap;
    if (bodyY + bodyHeight > pane.height && anchorBounds.y - gap - bodyHeight >= 0) {
      bodyY = anchorBounds.y - gap - bodyHeight;
    }
    bodyX = Math.max(0, Math.min(pane.width - bodyWidth, bodyX));
    bodyY = Math.max(0, Math.min(Math.max(0, pane.height - bodyHeight), bodyY));
    return new Rectangle(
        bodyX - shadow.left, bodyY - shadow.top, surfaceSize.width, surfaceSize.height);
  }

  @Override
  protected JComponent createSurface() {
    final ElwhaSurface popoverSurface = new ElwhaSurface();
    popoverSurface.setSurfaceRole(ColorRole.SURFACE_CONTAINER_HIGH);
    popoverSurface.setShape(ShapeScale.XL);
    popoverSurface.setBorderRole(null);
    popoverSurface.setElevation(ELEVATION);
    popoverSurface.setLayout(new BorderLayout());
    popoverSurface.setFocusable(true);
    final JPanel content = new JPanel(new BorderLayout());
    content.setOpaque(false);
    content.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, SpaceScale.SM.px(), 0));
    content.add(owner.picker(), BorderLayout.CENTER);
    popoverSurface.add(content, BorderLayout.CENTER);
    return new FadingWrapper(popoverSurface);
  }

  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    final Dimension pref = surface.getPreferredSize();
    final Point anchorAt = SwingUtilities.convertPoint(anchor, 0, 0, layeredPane);
    final Rectangle anchorBounds =
        new Rectangle(anchorAt.x, anchorAt.y, anchor.getWidth(), anchor.getHeight());
    final Insets shadow = shadowInsets();
    surface.setBounds(
        place(
            new Dimension(paneWidth, paneHeight),
            anchorBounds,
            pref,
            shadow,
            SpaceScale.XS.px(),
            orientation.isLeftToRight()));
    surface.validate();
  }

  private Insets shadowInsets() {
    if (surface instanceof FadingWrapper wrapper
        && wrapper.getComponent(0) instanceof ElwhaSurface body) {
      return body.getShadowInsets();
    }
    return new Insets(0, 0, 0, 0);
  }

  @Override
  protected String accessibleName() {
    return owner.picker().getAccessibleContext().getAccessibleName();
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
  protected boolean ownsFocus(final Component c) {
    // The latch (design §4): the sampler's always-on-top windows are toplevel siblings, so their
    // presses would otherwise read as "outside" and dismiss the popover mid-pick.
    return owner.picker().isSamplerOpen() || super.ownsFocus(c);
  }

  @Override
  protected void installKeyBindings() {
    final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    surface
        .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(escape, "popover-escape");
    surface
        .getActionMap()
        .put(
            "popover-escape",
            new AbstractAction() {
              @Override
              public void actionPerformed(final java.awt.event.ActionEvent e) {
                beginClose();
              }
            });
  }

  @Override
  protected void onClosed() {
    owner.handleClosed();
  }

  void requestClose() {
    if (isShowing() && !isClosing()) {
      beginClose();
    }
  }

  boolean showing() {
    return isShowing();
  }

  /** Composes the body surface and fades the whole popover with the host's motion progress. */
  private final class FadingWrapper extends JPanel {

    FadingWrapper(final ElwhaSurface body) {
      super(new BorderLayout());
      setOpaque(false);
      add(body, BorderLayout.CENTER);
    }

    @Override
    public void paint(final Graphics g) {
      final float p = Math.max(0f, Math.min(1f, motionProgress));
      if (p >= 1f) {
        super.paint(g);
        return;
      }
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setComposite(AlphaComposite.SrcOver.derive(p));
        super.paint(g2);
      } finally {
        g2.dispose();
      }
    }

    @Override
    public boolean isPaintingOrigin() {
      // Children animate (ripples, caret) while this container composites them in paint().
      return true;
    }
  }
}
