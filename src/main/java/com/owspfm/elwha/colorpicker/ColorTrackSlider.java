package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * The picker's gradient-track slider — a <em>dedicated</em> primitive (design doc {@code
 * elwha-color-picker-design.md} §2 lock, said loudly: NOT an {@code ElwhaSlider} extension, so
 * gradient-track paint stays off the finished shared primitive). M3 slider geometry — a 16px
 * full-corner track under a narrow full-corner bar handle — but the track renders a caller-supplied
 * color sweep (hue rainbow, channel sweep, alpha ramp) and the handle stays theme-neutral (SURFACE
 * fill, OUTLINE border) so it reads over any gradient.
 *
 * <p>Drags report {@code adjusting == true} per change with a settling call on release; keyboard
 * nudges (arrows ±1, Page ±10, Home/End) report settled values directly. {@link #setValue} is the
 * programmatic path and never notifies.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ColorTrackSlider extends JComponent {

  /** Receives user-gesture value changes (never {@link #setValue} echoes). */
  interface ValueListener {

    /**
     * Called per user change.
     *
     * @param value the new value
     * @param adjusting {@code true} while a drag is in flight
     */
    void valueChanged(int value, boolean adjusting);
  }

  static final int TRACK_HEIGHT = 16;
  static final int HANDLE_WIDTH = 6;
  static final int HANDLE_HEIGHT = 24;
  static final int COMPONENT_HEIGHT = 28;

  private final int min;
  private final int max;
  private int value;
  private Color[] trackStops = {Color.BLACK, Color.WHITE};
  private boolean checkerboardBacking;
  private ValueListener listener;
  private boolean dragging;

  ColorTrackSlider(final int min, final int max, final int value) {
    this.min = min;
    this.max = max;
    this.value = clamp(value);
    setOpaque(false);
    setFocusable(true);
    setAlignmentX(LEFT_ALIGNMENT);
    final MouseAdapter mouse =
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent e) {
            if (!isInteractive()) {
              return;
            }
            requestFocusInWindow();
            dragging = true;
            userSet(valueAt(e.getX()), true);
          }

          @Override
          public void mouseDragged(final MouseEvent e) {
            if (dragging && isInteractive()) {
              userSet(valueAt(e.getX()), true);
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (dragging) {
              dragging = false;
              userSet(value, false);
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
    bindKey(KeyEvent.VK_LEFT, () -> userSet(value - 1, false));
    bindKey(KeyEvent.VK_RIGHT, () -> userSet(value + 1, false));
    bindKey(KeyEvent.VK_PAGE_DOWN, () -> userSet(value - 10, false));
    bindKey(KeyEvent.VK_PAGE_UP, () -> userSet(value + 10, false));
    bindKey(KeyEvent.VK_HOME, () -> userSet(min, false));
    bindKey(KeyEvent.VK_END, () -> userSet(max, false));
  }

  int value() {
    return value;
  }

  void setValue(final int next) {
    final int clamped = clamp(next);
    if (clamped != value) {
      value = clamped;
      repaint();
    }
  }

  void setListener(final ValueListener listener) {
    this.listener = listener;
  }

  void setTrackStops(final Color... stops) {
    this.trackStops = stops.clone();
    repaint();
  }

  void setCheckerboardBacking(final boolean checkerboardBacking) {
    this.checkerboardBacking = checkerboardBacking;
    repaint();
  }

  void userSet(final int next, final boolean adjusting) {
    final int clamped = clamp(next);
    final boolean changed = clamped != value;
    value = clamped;
    if (changed || !adjusting) {
      repaint();
      if (listener != null) {
        listener.valueChanged(value, adjusting);
      }
    }
  }

  private boolean isInteractive() {
    return isEnabled();
  }

  private int clamp(final int candidate) {
    return Math.max(min, Math.min(max, candidate));
  }

  private int inset() {
    return HANDLE_WIDTH / 2 + 2;
  }

  private int valueAt(final int x) {
    final int span = Math.max(1, getWidth() - 2 * inset());
    final float fraction = (x - inset()) / (float) span;
    return min + Math.round(Math.max(0f, Math.min(1f, fraction)) * (max - min));
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

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(120, COMPONENT_HEIGHT);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(60, COMPONENT_HEIGHT);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT);
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (!isEnabled()) {
        g2.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      }
      final int width = getWidth();
      final int trackY = (getHeight() - TRACK_HEIGHT) / 2;
      final RoundRectangle2D.Double track =
          new RoundRectangle2D.Double(0, trackY, width, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);
      if (checkerboardBacking) {
        Checkerboard.fill(g2, track);
      }
      if (trackStops.length > 1) {
        final float[] fractions = new float[trackStops.length];
        for (int i = 0; i < trackStops.length; i++) {
          fractions[i] = i / (float) (trackStops.length - 1);
        }
        g2.setPaint(
            new LinearGradientPaint(inset(), 0, width - inset(), 0, fractions, trackStops));
      } else {
        g2.setPaint(trackStops[0]);
      }
      g2.fill(track);
      g2.setPaint(ColorRole.OUTLINE_VARIANT.resolve());
      g2.setStroke(new BasicStroke(1f));
      g2.draw(track);
      paintHandle(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintHandle(final Graphics2D g2) {
    final int span = Math.max(1, getWidth() - 2 * inset());
    final float fraction = max == min ? 0f : (value - min) / (float) (max - min);
    final int handleX = Math.round(inset() + fraction * span) - HANDLE_WIDTH / 2;
    final int handleY = (getHeight() - HANDLE_HEIGHT) / 2;
    if (isFocusOwner()) {
      g2.setColor(ColorRole.PRIMARY.resolve());
      g2.setStroke(new BasicStroke(2f));
      g2.drawRoundRect(handleX - 3, handleY - 3, HANDLE_WIDTH + 6, HANDLE_HEIGHT + 6, 10, 10);
    }
    g2.setColor(ColorRole.SURFACE.resolve());
    g2.fillRoundRect(handleX, handleY, HANDLE_WIDTH, HANDLE_HEIGHT, HANDLE_WIDTH, HANDLE_WIDTH);
    g2.setColor(ColorRole.OUTLINE.resolve());
    g2.setStroke(new BasicStroke(1f));
    g2.drawRoundRect(handleX, handleY, HANDLE_WIDTH, HANDLE_HEIGHT, HANDLE_WIDTH, HANDLE_WIDTH);
  }
}
