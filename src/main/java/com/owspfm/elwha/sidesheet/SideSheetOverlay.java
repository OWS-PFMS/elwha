package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaLayers;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * The modal presentation host for {@link ElwhaSideSheet} — the package-private {@link
 * AbstractElwhaOverlay} subclass behind {@link ElwhaSideSheet#showModal(Component)}. Pins the modal
 * side-sheet posture: {@code ElwhaLayers.OVERLAY_LAYER} (190, below dialogs and menus per #221), a
 * 32% {@code SCRIM} backdrop (the dialog's scrim treatment), the focus-trap dismiss policy, an
 * edge-docked full-height placement flush against the resolved window edge, and the slide-from-edge
 * entrance/exit synchronized with the scrim fade. One host instance serves one {@code
 * showModal(...)}; the sheet constructs a fresh one per show.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
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
  // build-time snapshot — the sheet is a long-lived component with mutable config).
  @Override
  protected void installKeyBindings() {
    final InputMap im = surface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap am = surface.getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-sidesheet-esc");
    am.put(
        "elwha-sidesheet-esc",
        action(
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

  // Docks the surface full-height, flush against the resolved window edge.
  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    final int bodyW = Math.min(sheet.getSheetWidth(), paneWidth);
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

  @Override
  protected void onClosed() {
    sheet.modalClosed(exitCause);
  }

  // The wrapper that slides the sheet in from its resolved edge. During the motion the fully-laid
  // surface is rasterized once at device resolution and only that bitmap is translated per frame
  // (the DialogSurface snapshot pattern — one stable render instead of a full shadow + child paint
  // every tick); at full progress it's a plain live paint with no buffer cost.
  private final class SlideSurface extends JPanel {

    private BufferedImage motionSnapshot;
    private int snapshotW;
    private int snapshotH;

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
        motionSnapshot = null;
        super.paint(g);
        return;
      }
      final int w = getWidth();
      final int h = getHeight();
      if (w <= 0 || h <= 0) {
        return;
      }
      final AffineTransform tx = ((Graphics2D) g).getTransform();
      final double sx = tx.getScaleX() > 0 ? tx.getScaleX() : 1.0;
      final double sy = tx.getScaleY() > 0 ? tx.getScaleY() : 1.0;
      final int deviceW = Math.max(1, (int) Math.ceil(w * sx));
      final int deviceH = Math.max(1, (int) Math.ceil(h * sy));
      if (motionSnapshot == null || snapshotW != deviceW || snapshotH != deviceH) {
        final BufferedImage snap = new BufferedImage(deviceW, deviceH, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D bg = snap.createGraphics();
        try {
          bg.scale(sx, sy);
          super.paint(bg);
        } finally {
          bg.dispose();
        }
        motionSnapshot = snap;
        snapshotW = deviceW;
        snapshotH = deviceH;
      }

      final int dx = Math.round((1f - p) * w) * (sheet.isDockedRight() ? 1 : -1);
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.translate(dx, 0);
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(motionSnapshot, 0, 0, w, h, null);
      } finally {
        g2.dispose();
      }
    }

    // This surface translates its render during the slide; a descendant's partial repaint (a
    // ripple tick, a caret blink) would otherwise paint untranslated mid-motion. Declaring a
    // painting origin forces descendant repaints through paint() (the #305/#176 Swing contract for
    // transformed children).
    @Override
    public boolean isPaintingOrigin() {
      return true;
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
