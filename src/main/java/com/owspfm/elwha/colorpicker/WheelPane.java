package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * The WHEEL pane (design doc {@code elwha-color-picker-v2-design.md} §2): a hue/saturation disc —
 * hue is the angle (0° at three o'clock, counter-clockwise positive, the {@code atan2}/HSB
 * convention), saturation the radius — over a value {@link ColorTrackSlider}, the macOS Color Wheel
 * / Flutter wheel lineage. Structurally the SPECTRUM pane's sibling: one 2D surface, one value
 * track, the alpha track when enabled.
 *
 * <p>The wheel itself never mirrors under RTL — a hue angle is a fixed convention — while its
 * tracks mirror like every {@code ColorTrackSlider}.
 *
 * <p><strong>Hue-preservation invariant.</strong> Identical to SPECTRUM: the pane owns float h/s
 * while editing, so dragging through the desaturated center or sliding value to black never snaps
 * the hue. External commits adopt the incoming hue only when the color actually carries one.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class WheelPane extends ColorPickerPane {

  // The SV-box height budget (design §2): keeps the wheel card equal to the spectrum card so the
  // CardLayout host never pads a shorter pane before the dialog's action row.
  static final int DISC_DIAMETER = 146;

  private final WheelDisc disc;
  private final ColorTrackSlider valueSlider;
  private final ColorTrackSlider alphaSlider;

  private float hueDegrees;
  private float saturation;
  private float value;
  private int alpha;

  WheelPane(final ElwhaColorPicker picker) {
    super(picker);
    adoptHsv(picker.getColor());
    this.alpha = picker.getColor().getAlpha();
    this.disc = new WheelDisc();
    this.valueSlider = new ColorTrackSlider(0, 100, Math.round(value * 100f));
    valueSlider.setAccessibleChannelName("Value");
    valueSlider.setListener(this::valueTo);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(disc);
    add(Box.createVerticalStrut(SpaceScale.SM.px()));
    add(valueSlider);
    if (picker.isAlphaEnabled()) {
      this.alphaSlider = new ColorTrackSlider(0, 255, alpha);
      alphaSlider.setCheckerboardBacking(true);
      alphaSlider.setAccessibleChannelName("Alpha");
      alphaSlider.setListener(this::alphaTo);
      add(alphaSlider);
      updateAlphaTrack();
    } else {
      this.alphaSlider = null;
    }
    updateValueTrack();
  }

  float hueDegrees() {
    return hueDegrees;
  }

  float saturation() {
    return saturation;
  }

  float value() {
    return value;
  }

  void hueSatTo(final float degrees, final float sat, final boolean adjusting) {
    hueDegrees = ((degrees % 360f) + 360f) % 360f;
    saturation = Math.max(0f, Math.min(1f, sat));
    disc.repaint();
    commitHsv(adjusting);
  }

  void valueTo(final int percent, final boolean adjusting) {
    value = Math.max(0, Math.min(100, percent)) / 100f;
    disc.repaint();
    commitHsv(adjusting);
  }

  void alphaTo(final int next, final boolean adjusting) {
    alpha = Math.max(0, Math.min(255, next));
    commitHsv(adjusting);
  }

  @Override
  void syncFromPicker(final Color color) {
    if ((currentRgb() & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF) && alpha == color.getAlpha()) {
      return;
    }
    adoptHsv(color);
    alpha = color.getAlpha();
    valueSlider.setValue(Math.round(value * 100f));
    if (alphaSlider != null) {
      alphaSlider.setValue(alpha);
      updateAlphaTrack();
    }
    updateValueTrack();
    disc.repaint();
  }

  private void adoptHsv(final Color color) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    if (hsb[1] > 0f && hsb[2] > 0f) {
      hueDegrees = hsb[0] * 360f;
    }
    if (hsb[2] > 0f) {
      saturation = hsb[1];
    }
    value = hsb[2];
  }

  private int currentRgb() {
    return Color.HSBtoRGB(hueDegrees / 360f, saturation, value);
  }

  private void commitHsv(final boolean adjusting) {
    updateValueTrack();
    if (alphaSlider != null) {
      updateAlphaTrack();
      commit(new Color((currentRgb() & 0xFFFFFF) | (alpha << 24), true), adjusting);
    } else {
      commit(new Color(currentRgb()), adjusting);
    }
  }

  private void updateValueTrack() {
    valueSlider.setTrackStops(
        Color.BLACK, new Color(Color.HSBtoRGB(hueDegrees / 360f, saturation, 1f)));
  }

  private void updateAlphaTrack() {
    final int rgb = currentRgb() & 0xFFFFFF;
    alphaSlider.setTrackStops(new Color(rgb, true), new Color(rgb));
  }

  /** The hue/saturation disc with its ring thumb and polar arrow-key nudging. */
  private final class WheelDisc extends JComponent {

    private BufferedImage cache;

    WheelDisc() {
      setOpaque(false);
      setFocusable(true);
      setAlignmentX(LEFT_ALIGNMENT);
      final MouseAdapter mouse =
          new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
              if (isInteractive()) {
                requestFocusInWindow();
                pointAt(e, true);
              }
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
              if (isInteractive()) {
                pointAt(e, true);
              }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
              if (isInteractive()) {
                hueSatTo(hueDegrees, saturation, false);
              }
            }
          };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
      addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
              repaint();
            }

            @Override
            public void focusLost(final FocusEvent e) {
              repaint();
            }
          });
      bindKey(KeyEvent.VK_LEFT, () -> hueSatTo(hueDegrees - 1f, saturation, false));
      bindKey(KeyEvent.VK_RIGHT, () -> hueSatTo(hueDegrees + 1f, saturation, false));
      bindKey(KeyEvent.VK_UP, () -> hueSatTo(hueDegrees, saturation + 0.01f, false));
      bindKey(KeyEvent.VK_DOWN, () -> hueSatTo(hueDegrees, saturation - 0.01f, false));
      bindKey(KeyEvent.VK_PAGE_UP, () -> hueSatTo(hueDegrees + 10f, saturation, false));
      bindKey(KeyEvent.VK_PAGE_DOWN, () -> hueSatTo(hueDegrees - 10f, saturation, false));
      bindKey(KeyEvent.VK_HOME, () -> hueSatTo(hueDegrees, 0f, false));
      bindKey(KeyEvent.VK_END, () -> hueSatTo(hueDegrees, 1f, false));
    }

    private boolean isInteractive() {
      return isEnabled() && WheelPane.this.isEnabled() && picker().isEnabled();
    }

    private void bindKey(final int keyCode, final Runnable action) {
      final KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, 0);
      getInputMap(WHEN_FOCUSED).put(stroke, stroke.toString());
      getActionMap()
          .put(
              stroke.toString(),
              new AbstractAction() {
                @Override
                public void actionPerformed(final java.awt.event.ActionEvent e) {
                  if (isInteractive()) {
                    action.run();
                  }
                }
              });
    }

    private void pointAt(final MouseEvent e, final boolean adjusting) {
      final float radius = DISC_DIAMETER / 2f;
      final float dx = e.getX() - getWidth() / 2f;
      final float dy = e.getY() - radius;
      final double dist = Math.hypot(dx, dy);
      final float degrees =
          dist == 0 ? hueDegrees : (float) ((Math.toDegrees(Math.atan2(-dy, dx)) + 360.0) % 360.0);
      hueSatTo(degrees, (float) Math.min(1.0, dist / radius), adjusting);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(0, DISC_DIAMETER);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, DISC_DIAMETER);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!isInteractive()) {
          g2.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
        }
        final int x0 = getWidth() / 2 - DISC_DIAMETER / 2;
        g2.drawImage(discImage(), x0, 0, null);
        final int overlayAlpha = Math.round((1f - value) * 255f);
        if (overlayAlpha > 0) {
          // Compositing translucent black is exact for HSV — RGB(h,s,v) = v · RGB(h,s,1) — so the
          // full-saturation disc renders once and value never re-renders it (design §2).
          g2.setColor(new Color(0, 0, 0, overlayAlpha));
          g2.fill(new Ellipse2D.Double(x0, 0, DISC_DIAMETER, DISC_DIAMETER));
        }
        g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Ellipse2D.Double(x0 + 0.5, 0.5, DISC_DIAMETER - 1, DISC_DIAMETER - 1));
        paintThumb(g2, x0);
      } finally {
        g2.dispose();
      }
    }

    private void paintThumb(final Graphics2D g2, final int x0) {
      final float radius = DISC_DIAMETER / 2f;
      final double angle = Math.toRadians(hueDegrees);
      final int x = Math.round(x0 + radius + (float) (Math.cos(angle) * saturation * radius));
      final int y = Math.round(radius - (float) (Math.sin(angle) * saturation * radius));
      g2.setColor(ColorRole.SURFACE.resolve());
      g2.setStroke(new BasicStroke(2f));
      g2.drawOval(x - 8, y - 8, 16, 16);
      g2.setColor(ColorRole.OUTLINE.resolve());
      g2.setStroke(new BasicStroke(1f));
      g2.drawOval(x - 9, y - 9, 18, 18);
      if (isFocusOwner()) {
        g2.setColor(ColorRole.PRIMARY.resolve());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - 12, y - 12, 24, 24);
      }
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleWheelDisc();
      }
      return accessibleContext;
    }

    /** Names the disc and reads back the current hue/saturation pair. */
    private final class AccessibleWheelDisc extends AccessibleJComponent {

      @Override
      public javax.accessibility.AccessibleRole getAccessibleRole() {
        return javax.accessibility.AccessibleRole.PANEL;
      }

      @Override
      public String getAccessibleName() {
        return "Hue and saturation wheel";
      }

      @Override
      public String getAccessibleDescription() {
        return Math.round(hueDegrees) + "° hue, " + Math.round(saturation * 100f) + "% saturation";
      }
    }

    private BufferedImage discImage() {
      if (cache == null) {
        // The edge is antialiased inside the image (per-pixel coverage), never via clip() —
        // Java2D clipping is never antialiased (the V1 shade-strip smoke-iterate finding).
        cache = new BufferedImage(DISC_DIAMETER, DISC_DIAMETER, BufferedImage.TYPE_INT_ARGB);
        final float radius = DISC_DIAMETER / 2f;
        final int[] row = new int[DISC_DIAMETER];
        for (int y = 0; y < DISC_DIAMETER; y++) {
          final float dy = y - radius + 0.5f;
          for (int x = 0; x < DISC_DIAMETER; x++) {
            final float dx = x - radius + 0.5f;
            final float dist = (float) Math.hypot(dx, dy);
            final float coverage = Math.max(0f, Math.min(1f, radius - dist + 0.5f));
            if (coverage == 0f) {
              row[x] = 0;
              continue;
            }
            final float hue =
                (float) ((Math.toDegrees(Math.atan2(-dy, dx)) + 360.0) % 360.0) / 360f;
            final int rgb = Color.HSBtoRGB(hue, Math.min(1f, dist / radius), 1f);
            row[x] = (Math.round(coverage * 255f) << 24) | (rgb & 0xFFFFFF);
          }
          cache.setRGB(0, y, DISC_DIAMETER, 1, row, 0, DISC_DIAMETER);
        }
      }
      return cache;
    }
  }
}
