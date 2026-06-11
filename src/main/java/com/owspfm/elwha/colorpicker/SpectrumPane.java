package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * The SPECTRUM pane (design doc {@code elwha-color-picker-design.md} §6): the saturation/value
 * square (x = saturation, y = value, top bright) over a hue {@link ColorTrackSlider} with the
 * six-stop rainbow track.
 *
 * <p><strong>Hue-preservation invariant.</strong> The pane owns float h/s/v as the source of truth
 * while editing: dragging through greys (s&nbsp;=&nbsp;0) or black (v&nbsp;=&nbsp;0) never snaps
 * the hue back to red, the classic RGB-roundtrip picker bug. External commits resync s and v but
 * adopt the incoming hue only when the color actually carries one.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class SpectrumPane extends ColorPickerPane {

  private static final int SV_BOX_HEIGHT = 168;

  private final SvBox svBox;
  private final ColorTrackSlider hueSlider;
  private final ColorTrackSlider alphaSlider;

  private float hueDegrees;
  private float saturation;
  private float value;
  private int alpha;

  SpectrumPane(final ElwhaColorPicker picker) {
    super(picker);
    adoptHsv(picker.getColor());
    this.alpha = picker.getColor().getAlpha();
    this.svBox = new SvBox();
    this.hueSlider = new ColorTrackSlider(0, 360, Math.round(hueDegrees));
    hueSlider.setTrackStops(rainbow());
    hueSlider.setAccessibleChannelName("Hue");
    hueSlider.setListener((degrees, adjusting) -> hueTo(degrees, adjusting));
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(svBox);
    add(Box.createVerticalStrut(SpaceScale.MD.px()));
    add(hueSlider);
    if (picker.isAlphaEnabled()) {
      this.alphaSlider = new ColorTrackSlider(0, 255, alpha);
      alphaSlider.setCheckerboardBacking(true);
      alphaSlider.setAccessibleChannelName("Alpha");
      alphaSlider.setListener(this::alphaTo);
      add(Box.createVerticalStrut(SpaceScale.SM.px()));
      add(alphaSlider);
      updateAlphaTrack();
    } else {
      this.alphaSlider = null;
    }
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

  void hueTo(final int degrees, final boolean adjusting) {
    hueDegrees = Math.max(0, Math.min(360, degrees));
    hueSlider.setValue(degrees);
    svBox.repaint();
    commitHsv(adjusting);
  }

  void pointTo(final float s, final float v, final boolean adjusting) {
    saturation = Math.max(0f, Math.min(1f, s));
    value = Math.max(0f, Math.min(1f, v));
    svBox.repaint();
    commitHsv(adjusting);
  }

  void alphaTo(final int next, final boolean adjusting) {
    alpha = Math.max(0, Math.min(255, next));
    commitHsv(adjusting);
  }

  @Override
  void syncFromPicker(final Color color) {
    if ((currentRgb() & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF)
        && alpha == color.getAlpha()) {
      return;
    }
    adoptHsv(color);
    alpha = color.getAlpha();
    hueSlider.setValue(Math.round(hueDegrees));
    if (alphaSlider != null) {
      alphaSlider.setValue(alpha);
      updateAlphaTrack();
    }
    svBox.repaint();
  }

  private void adoptHsv(final Color color) {
    final float[] hsb =
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
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
    if (alphaSlider != null) {
      updateAlphaTrack();
      commit(new Color((currentRgb() & 0xFFFFFF) | (alpha << 24), true), adjusting);
    } else {
      commit(new Color(currentRgb()), adjusting);
    }
  }

  private void updateAlphaTrack() {
    final int rgb = currentRgb() & 0xFFFFFF;
    alphaSlider.setTrackStops(new Color(rgb, true), new Color(rgb));
  }

  private static Color[] rainbow() {
    final Color[] stops = new Color[7];
    for (int i = 0; i < 7; i++) {
      stops[i] = Color.getHSBColor(i / 6f, 1f, 1f);
    }
    return stops;
  }

  /** The saturation/value square with its ring thumb and arrow-key nudging. */
  private final class SvBox extends JComponent {

    private BufferedImage cache;
    private int cachedHue = -1;

    SvBox() {
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
                pointTo(saturation, value, false);
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
      bindKey(KeyEvent.VK_LEFT, () -> pointTo(saturation - 0.01f, value, false));
      bindKey(KeyEvent.VK_RIGHT, () -> pointTo(saturation + 0.01f, value, false));
      bindKey(KeyEvent.VK_UP, () -> pointTo(saturation, value + 0.01f, false));
      bindKey(KeyEvent.VK_DOWN, () -> pointTo(saturation, value - 0.01f, false));
      bindKey(KeyEvent.VK_PAGE_UP, () -> pointTo(saturation, value + 0.1f, false));
      bindKey(KeyEvent.VK_PAGE_DOWN, () -> pointTo(saturation, value - 0.1f, false));
    }

    private boolean isInteractive() {
      return isEnabled() && SpectrumPane.this.isEnabled() && picker().isEnabled();
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
      final float s = e.getX() / (float) Math.max(1, getWidth() - 1);
      final float v = 1f - e.getY() / (float) Math.max(1, getHeight() - 1);
      pointTo(s, v, adjusting);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(0, SV_BOX_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, SV_BOX_HEIGHT);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!isInteractive()) {
          g2.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
        }
        final int width = getWidth();
        final int height = getHeight();
        if (width < 2 || height < 2) {
          return;
        }
        final int arc = ShapeScale.SM.px();
        final RoundRectangle2D.Double clip =
            new RoundRectangle2D.Double(0, 0, width, height, arc * 2.0, arc * 2.0);
        final Graphics2D clipped = (Graphics2D) g2.create();
        clipped.clip(clip);
        clipped.drawImage(image(width, height), 0, 0, null);
        clipped.dispose();
        g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
        g2.setStroke(new BasicStroke(1f));
        g2.draw(clip);
        paintThumb(g2, width, height);
      } finally {
        g2.dispose();
      }
    }

    private void paintThumb(final Graphics2D g2, final int width, final int height) {
      final int x = Math.round(saturation * (width - 1));
      final int y = Math.round((1f - value) * (height - 1));
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
        accessibleContext = new AccessibleSvBox();
      }
      return accessibleContext;
    }

    /** Names the square and reads back the current saturation/value pair. */
    private final class AccessibleSvBox extends AccessibleJComponent {

      @Override
      public javax.accessibility.AccessibleRole getAccessibleRole() {
        return javax.accessibility.AccessibleRole.PANEL;
      }

      @Override
      public String getAccessibleName() {
        return "Saturation and value";
      }

      @Override
      public String getAccessibleDescription() {
        return Math.round(saturation * 100f)
            + "% saturation, "
            + Math.round(value * 100f)
            + "% value";
      }
    }

    private BufferedImage image(final int width, final int height) {
      final int hue = Math.round(hueDegrees);
      if (cache == null
          || cache.getWidth() != width
          || cache.getHeight() != height
          || cachedHue != hue) {
        cache = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] row = new int[width];
        for (int y = 0; y < height; y++) {
          final float v = 1f - y / (float) (height - 1);
          for (int x = 0; x < width; x++) {
            row[x] = Color.HSBtoRGB(hue / 360f, x / (float) (width - 1), v);
          }
          cache.setRGB(0, y, width, 1, row, 0, width);
        }
        cachedHue = hue;
      }
      return cache;
    }
  }
}
