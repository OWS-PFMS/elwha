package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaLayers;
import com.owspfm.elwha.theme.MotionSnapshot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * The modal presentation host for {@link ElwhaSideSheet} — the package-private {@link
 * AbstractElwhaOverlay} subclass behind {@link ElwhaSideSheet#showModal(Component)}. Pins the modal
 * side-sheet posture: {@code ElwhaLayers.OVERLAY_LAYER} (190, below dialogs and menus), a 32%
 * {@code SCRIM} backdrop (the dialog's scrim treatment), the focus-trap dismiss policy, an
 * edge-docked full-height placement flush against the resolved window edge, and the slide-from-edge
 * entrance/exit synchronized with the scrim fade. One host instance serves one {@code
 * showModal(...)}; the sheet constructs a fresh one per show.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
final class SideSheetOverlay extends AbstractElwhaOverlay {

  private final ElwhaSideSheet sheet;
  private SheetDismissCause exitCause;

  SideSheetOverlay(final ElwhaSideSheet sheet) {
    this.sheet = sheet;
  }

  // ----------------------------------------------------------- anatomy hooks

  @Override
  protected JComponent createSurface() {
    final SlideSurface slide = new SlideSurface();
    slide.add(sheet, BorderLayout.CENTER);
    return slide;
  }

  @Override
  protected JComponent createBackdrop() {
    return new SheetScrim();
  }

  @Override
  protected Integer overlayLayer() {
    return ElwhaLayers.OVERLAY_LAYER;
  }

  @Override
  protected String accessibleName() {
    return sheet.getHeadline();
  }

  // Esc → dismiss, honored live so a toggle while shown takes effect (unlike the dialog's
  // build-time snapshot — the sheet is a long-lived component with mutable config). Registered
  // through topmostAction so a dialog stacked above the sheet keeps Escape (#599).
  @Override
  protected void installKeyBindings() {
    final InputMap im = surface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap am = surface.getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-sidesheet-esc");
    am.put(
        "elwha-sidesheet-esc",
        topmostAction(
            () -> {
              if (sheet.isDismissibleByEsc()) {
                dismissSheet(SheetDismissCause.ESC);
              }
            }));
  }

  @Override
  protected Component initialFocusTarget() {
    final Component inContent = firstFocusable(sheet.contentHolderPanel());
    if (inContent != null) {
      return inContent;
    }
    return sheet.isCloseAffordanceVisible() ? sheet.closeAffordanceButton() : null;
  }

  // Docks the surface full-height, flush against the resolved window edge. The band is the sheet's
  // FOOTPRINT (sheet width plus its own detached margin), so a detached sheet's border floats the
  // painted body off all four window edges and the slide carries the whole floating card; a docked
  // sheet has a zero margin, so the footprint is just the sheet width (V1 behavior verbatim).
  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    final int bodyW = Math.min(sheet.modalFootprintWidth(), paneWidth);
    final int x = sheet.isDockedRight() ? paneWidth - bodyW : 0;
    surface.setBounds(x, 0, bodyW, paneHeight);
    surface.validate();
  }

  // ----------------------------------------------------------- dismiss plumbing

  void dismissSheet(final SheetDismissCause cause) {
    if (isClosing() || !isShowing()) {
      return;
    }
    exitCause = cause;
    beginClose();
  }

  boolean showingNow() {
    return isShowing();
  }

  // Re-docks the surface after a config change that moves its geometry (a live setSheetWidth) —
  // the base only re-lays on host resize.
  void relayoutHost() {
    relayout();
  }

  // Drag-to-dismiss: the sheet's header drag maps a [0,1] dismiss fraction onto the slide motion
  // (fraction 0 = docked, 1 = fully off-edge), the scrim dimming with it. Release routes to a
  // threshold dismiss (DRAG cause, the exit continues from the dragged position) or a settle back.
  void dragTo(final float dismissFraction) {
    scrubMotion(1f - dismissFraction);
  }

  void dragSettle() {
    settleMotion();
  }

  void dragDismiss() {
    dismissSheet(SheetDismissCause.DRAG);
  }

  @Override
  protected void onClosed() {
    sheet.modalClosed(exitCause);
  }

  // The wrapper that slides the sheet in from its resolved edge. During the motion the fully-laid
  // surface is rasterized once at device resolution and only that bitmap is translated per frame
  // (the DialogSurface snapshot pattern — one stable render instead of a full shadow + child paint
  // every tick); at full progress it's a plain live paint with no buffer cost.
  private final class SlideSurface extends JPanel {

    private final MotionSnapshot motionSnapshot = new MotionSnapshot();

    SlideSurface() {
      super(new BorderLayout());
      setOpaque(false);
      // The trap's last-resort focus holder when the sheet has no focusable content and the close
      // affordance is hidden (the DialogSurface precedent).
      setFocusable(true);
    }

    @Override
    public void paint(final Graphics g) {
      final float p = Math.max(0f, Math.min(1f, motionProgress));
      if (p >= 1f) {
        motionSnapshot.release();
        super.paint(g);
        return;
      }
      final int w = getWidth();
      final int h = getHeight();
      if (w <= 0 || h <= 0) {
        return;
      }
      final BufferedImage raster = motionSnapshot.rasterFor((Graphics2D) g, w, h, super::paint);

      final int dx = Math.round((1f - p) * w) * (sheet.isDockedRight() ? 1 : -1);
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.translate(dx, 0);
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(raster, 0, 0, w, h, null);
      } finally {
        g2.dispose();
      }
    }

    // This surface translates its render during the slide; a descendant's partial repaint (a
    // ripple tick, a caret blink) would otherwise paint untranslated mid-motion. Declaring a
    // painting origin forces descendant repaints through paint() (the #305/#176 Swing contract for
    // transformed children).
    //
    // Gated on the tween, not unconditional: past full progress paint() short-circuits to
    // super.paint() and translates nothing, so a bare `true` would keep forcing a whole-surface
    // re-composite for every caret blink and hover tick for as long as the sheet is open.
    //
    // Routed through the snapshot because being ASKED this question is Swing's signal that a
    // descendant is redirecting a repaint up here, which is exactly when the cached raster has gone
    // stale (#713). See MotionSnapshot for the measurement behind that.
    @Override
    public boolean isPaintingOrigin() {
      return motionSnapshot.paintingOrigin(motionProgress < 1f);
    }
  }

  // The full-bounds scrim: SCRIM @ 32% × motionProgress beneath the surface, consuming every mouse
  // event that reaches it — the input-blocking half of the sheet's modality. A press dismisses
  // when the sheet allows it; it blocks either way.
  private final class SheetScrim extends JComponent {
    private static final float SCRIM_ALPHA = 0.32f;

    SheetScrim() {
      setOpaque(false);
      final MouseAdapter consumer =
          new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
              if (sheet.isDismissibleByScrim()) {
                dismissSheet(SheetDismissCause.SCRIM);
              }
            }
          };
      addMouseListener(consumer);
      addMouseMotionListener(consumer);
      addMouseWheelListener(e -> {});
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        final float p = Math.max(0f, Math.min(1f, motionProgress));
        final Color scrim = ColorRole.SCRIM.resolve();
        g2.setColor(
            new Color(
                scrim.getRed(),
                scrim.getGreen(),
                scrim.getBlue(),
                Math.round(255 * SCRIM_ALPHA * p)));
        g2.fillRect(0, 0, getWidth(), getHeight());
      } finally {
        g2.dispose();
      }
    }
  }
}
