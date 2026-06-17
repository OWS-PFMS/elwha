package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JWindow;

/**
 * The eyedropper's <strong>frozen-capture screen sampler</strong> (V2 design doc {@code
 * elwha-color-picker-v2-design.md} §4): on {@link #open()}, every {@link GraphicsDevice} is
 * captured once via {@link Robot#createScreenCapture} and shown in an undecorated always-on-top
 * full-screen window; a magnifier loupe (an 11×11 logical-pixel grid at 8×, center cell
 * highlighted, hex chip beneath) follows the pointer. A press or Enter samples the frozen image and
 * delivers the color; Esc cancels; arrow keys nudge the sample point one pixel. Sampling reads the
 * capture, never live pixels — one capture, no global hooks, every on-screen pixel reachable.
 *
 * <p><strong>macOS requires the Screen Recording permission</strong> (System Settings → Privacy
 * &amp; Security) for {@code Robot} captures of other applications' windows. A denial is not
 * detectable in code — macOS hands back a frame containing only the wallpaper and this JVM's own
 * windows — so the requirement is a documentation rule, not a runtime check. Headless environments
 * and {@link Robot} construction failures <em>are</em> detectable: {@link #isSupported()} answers
 * {@code false} and the picker's eyedropper affordance disables.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ScreenSampler {

  /** Loupe grid extent in logical pixels per side — odd, so a center cell exists. */
  static final int LOUPE_GRID = 11;

  /** Loupe magnification — each captured pixel renders as an 8×8 cell. */
  static final int LOUPE_SCALE = 8;

  private static final int LOUPE_MARGIN = 24;
  private static final int CHIP_HEIGHT = 22;

  private final Consumer<Color> onPick;
  private final Runnable onClosed;
  private final List<SamplerWindow> windows = new ArrayList<>();
  private KeyEventDispatcher keyDispatcher;
  private SamplerWindow activeWindow;
  private boolean done;

  ScreenSampler(final Consumer<Color> onPick, final Runnable onClosed) {
    this.onPick = onPick;
    this.onClosed = onClosed;
  }

  /**
   * Answers whether screen sampling can work here — a graphics environment exists and {@link Robot}
   * constructs. (A macOS permission denial is not detectable; see the class note.)
   */
  static boolean isSupported() {
    if (GraphicsEnvironment.isHeadless()) {
      return false;
    }
    try {
      new Robot();
      return true;
    } catch (final AWTException | SecurityException e) {
      return false;
    }
  }

  /** Samples the capture at a point, clamping to the image bounds. */
  static Color colorAt(final BufferedImage capture, final int x, final int y) {
    final int cx = Math.max(0, Math.min(capture.getWidth() - 1, x));
    final int cy = Math.max(0, Math.min(capture.getHeight() - 1, y));
    return new Color(capture.getRGB(cx, cy));
  }

  /**
   * Places the loupe box beside the pointer, flipping quadrants near the screen's trailing and
   * bottom edges so the loupe never clips.
   */
  static Rectangle loupePlacement(final Dimension screen, final Point pointer) {
    final int side = LOUPE_GRID * LOUPE_SCALE;
    final int total = side + CHIP_HEIGHT;
    int x = pointer.x + LOUPE_MARGIN;
    int y = pointer.y + LOUPE_MARGIN;
    if (x + side > screen.width) {
      x = pointer.x - LOUPE_MARGIN - side;
    }
    if (y + total > screen.height) {
      y = pointer.y - LOUPE_MARGIN - total;
    }
    return new Rectangle(Math.max(0, x), Math.max(0, y), side, total);
  }

  void open() {
    if (!isSupported() || !windows.isEmpty()) {
      if (windows.isEmpty()) {
        finish(null);
      }
      return;
    }
    final Robot robot;
    try {
      robot = new Robot();
    } catch (final AWTException e) {
      finish(null);
      return;
    }
    for (final GraphicsDevice device :
        GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
      final Rectangle bounds = device.getDefaultConfiguration().getBounds();
      final BufferedImage capture = robot.createScreenCapture(bounds);
      final SamplerWindow window = new SamplerWindow(capture, bounds);
      windows.add(window);
      window.setVisible(true);
    }
    if (!windows.isEmpty()) {
      // Key handling routes through a global dispatcher, not the surface's input map: an
      // undecorated full-screen JWindow does not reliably acquire keyboard focus (notably on
      // macOS), so WHEN_IN_FOCUSED_WINDOW bindings — Esc, Enter, the arrow nudges — never fired.
      // The dispatcher catches keys regardless of which window (if any) holds focus and targets
      // the window the pointer last moved over.
      activeWindow = windows.get(0);
      keyDispatcher = this::dispatchKey;
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
      windows.get(0).requestFocus();
    }
  }

  private boolean dispatchKey(final KeyEvent e) {
    if (e.getID() != KeyEvent.KEY_PRESSED || activeWindow == null) {
      return false;
    }
    switch (e.getKeyCode()) {
      case KeyEvent.VK_ESCAPE -> finish(null);
      case KeyEvent.VK_ENTER -> finish(activeWindow.sampleAtPointer());
      case KeyEvent.VK_LEFT -> activeWindow.nudge(-1, 0);
      case KeyEvent.VK_RIGHT -> activeWindow.nudge(1, 0);
      case KeyEvent.VK_UP -> activeWindow.nudge(0, -1);
      case KeyEvent.VK_DOWN -> activeWindow.nudge(0, 1);
      default -> {
        return false;
      }
    }
    e.consume();
    return true;
  }

  boolean isOpen() {
    return !windows.isEmpty() && !done;
  }

  void cancel() {
    finish(null);
  }

  private void finish(final Color picked) {
    if (done) {
      return;
    }
    done = true;
    if (keyDispatcher != null) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
      keyDispatcher = null;
    }
    activeWindow = null;
    for (final SamplerWindow window : windows) {
      window.dispose();
    }
    windows.clear();
    if (picked != null) {
      onPick.accept(picked);
    }
    onClosed.run();
  }

  /** One frozen-capture window per screen device. */
  private final class SamplerWindow extends JWindow {

    private final BufferedImage capture;
    private final Point pointer;

    SamplerWindow(final BufferedImage capture, final Rectangle deviceBounds) {
      this.capture = capture;
      this.pointer = new Point(deviceBounds.width / 2, deviceBounds.height / 2);
      setAlwaysOnTop(true);
      setBounds(deviceBounds);
      final SamplerSurface surface = new SamplerSurface();
      setContentPane(surface);
      surface.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      final MouseAdapter mouse =
          new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
              activeWindow = SamplerWindow.this;
              pointer.setLocation(e.getPoint());
              repaint();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
              pointer.setLocation(e.getPoint());
              finish(sampleAtPointer());
            }
          };
      surface.addMouseListener(mouse);
      surface.addMouseMotionListener(mouse);
    }

    private Color sampleAtPointer() {
      return colorAt(capture, pointer.x, pointer.y);
    }

    private void nudge(final int dx, final int dy) {
      pointer.translate(dx, dy);
      pointer.x = Math.max(0, Math.min(getWidth() - 1, pointer.x));
      pointer.y = Math.max(0, Math.min(getHeight() - 1, pointer.y));
      repaint();
    }

    /** Paints the frozen capture plus the loupe. */
    private final class SamplerSurface extends JComponent {

      @Override
      protected void paintComponent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.drawImage(capture, 0, 0, getWidth(), getHeight(), null);
          paintLoupe(g2);
        } finally {
          g2.dispose();
        }
      }

      private void paintLoupe(final Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Rectangle box = loupePlacement(getSize(), pointer);
        final int side = LOUPE_GRID * LOUPE_SCALE;
        final int half = LOUPE_GRID / 2;
        for (int row = 0; row < LOUPE_GRID; row++) {
          for (int col = 0; col < LOUPE_GRID; col++) {
            g2.setColor(colorAt(capture, pointer.x - half + col, pointer.y - half + row));
            g2.fillRect(
                box.x + col * LOUPE_SCALE, box.y + row * LOUPE_SCALE, LOUPE_SCALE, LOUPE_SCALE);
          }
        }
        g2.setColor(ColorRole.OUTLINE.resolve());
        g2.setStroke(new BasicStroke(1f));
        for (int line = 1; line < LOUPE_GRID; line++) {
          g2.drawLine(
              box.x + line * LOUPE_SCALE, box.y, box.x + line * LOUPE_SCALE, box.y + side - 1);
          g2.drawLine(
              box.x, box.y + line * LOUPE_SCALE, box.x + side - 1, box.y + line * LOUPE_SCALE);
        }
        final Color sampled = sampleAtPointer();
        g2.setColor(contrast(sampled));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(
            box.x + half * LOUPE_SCALE, box.y + half * LOUPE_SCALE, LOUPE_SCALE, LOUPE_SCALE);
        g2.setColor(ColorRole.SURFACE.resolve());
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(box.x - 1, box.y - 1, side + 1, side + 1);

        final String hex = ColorHex.format(sampled, false);
        final Font chipFont = TypeRole.LABEL_MEDIUM.resolve();
        final FontMetrics fm = g2.getFontMetrics(chipFont);
        g2.setColor(ColorRole.SURFACE.resolve());
        g2.fillRect(box.x, box.y + side + 2, side, CHIP_HEIGHT - 2);
        g2.setFont(chipFont);
        g2.setColor(ColorRole.ON_SURFACE.resolve());
        g2.drawString(
            hex,
            box.x + (side - fm.stringWidth(hex)) / 2,
            box.y + side + 2 + (CHIP_HEIGHT - 2 - fm.getHeight()) / 2 + fm.getAscent());
      }

      private Color contrast(final Color over) {
        final int luminance =
            (299 * over.getRed() + 587 * over.getGreen() + 114 * over.getBlue()) / 1000;
        return luminance > 150 ? Color.BLACK : Color.WHITE;
      }

      @Override
      public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
          accessibleContext =
              new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                  return javax.accessibility.AccessibleRole.PANEL;
                }

                @Override
                public String getAccessibleName() {
                  return "Screen color sampler";
                }

                @Override
                public String getAccessibleDescription() {
                  return "Click or press Enter to pick "
                      + ColorHex.format(sampleAtPointer(), false)
                      + "; Escape cancels";
                }
              };
        }
        return accessibleContext;
      }
    }
  }
}
