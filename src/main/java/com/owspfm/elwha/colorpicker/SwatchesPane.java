package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * The SWATCHES pane (design doc {@code elwha-color-picker-design.md} §5): the Material hue grid
 * (twenty circular cells showing each hue's 500 shade), the active hue's shade strip (one connected
 * full-corner segmented run, 50–900), and the recent-colors row fed by the picker's non-adjusting
 * commits. The cell equal to the picker's current color carries the selection indicator — a 2px
 * primary ring plus a luminance-picked check, the M3 selected-day treatment translated to cells
 * that are themselves colored.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class SwatchesPane extends ColorPickerPane {

  private static final int GRID_COLUMNS = 10;
  private static final int CIRCLE_DIAMETER = 24;
  private static final int GRID_ROW_PITCH = 34;
  private static final int STRIP_HEIGHT = 28;
  private static final int STRIP_GAP = 2;
  private static final int RECENT_PITCH = 30;

  private final HueGrid hueGrid;
  private final ShadeStrip shadeStrip;
  private final RecentRow recentRow;

  private int activeHue;

  SwatchesPane(final ElwhaColorPicker picker) {
    super(picker);
    this.hueGrid = new HueGrid();
    this.shadeStrip = new ShadeStrip();
    this.recentRow = new RecentRow();
    final int[] found = MaterialSwatchCatalog.find(picker.getColor());
    this.activeHue = found != null ? found[0] : 0;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(hueGrid);
    add(Box.createVerticalStrut(SpaceScale.MD.px()));
    add(shadeStrip);
    add(Box.createVerticalStrut(SpaceScale.MD.px()));
    add(recentRow);
  }

  @Override
  void syncFromPicker(final Color color) {
    final int[] found = MaterialSwatchCatalog.find(color);
    if (found != null) {
      activeHue = found[0];
    }
    repaint();
  }

  int activeHueIndex() {
    return activeHue;
  }

  void selectHue(final int hueIndex) {
    activeHue = hueIndex;
    commit(
        preserveAlpha(
            MaterialSwatchCatalog.hues().get(hueIndex).shades()[
                MaterialSwatchCatalog.REPRESENTATIVE_SHADE]),
        false);
    repaint();
  }

  void selectShade(final int shadeIndex) {
    commit(preserveAlpha(MaterialSwatchCatalog.hues().get(activeHue).shades()[shadeIndex]), false);
    repaint();
  }

  private Color preserveAlpha(final Color catalogColor) {
    if (!picker().isAlphaEnabled()) {
      return catalogColor;
    }
    return new Color(
        catalogColor.getRed(),
        catalogColor.getGreen(),
        catalogColor.getBlue(),
        picker().getColor().getAlpha());
  }

  void selectRecent(final int recentIndex) {
    final List<Color> recent = picker().recentColors();
    if (recentIndex < recent.size()) {
      commit(recent.get(recentIndex), false);
      repaint();
    }
  }

  private boolean matchesCurrent(final Color cell) {
    return (cell.getRGB() & 0xFFFFFF) == (picker().getColor().getRGB() & 0xFFFFFF);
  }

  private static Color contrastTint(final Color over) {
    final int luminance =
        (299 * over.getRed() + 587 * over.getGreen() + 114 * over.getBlue()) / 1000;
    return luminance > 150 ? Color.BLACK : Color.WHITE;
  }

  private static void paintCheck(final Graphics2D g2, final Rectangle box, final Color tint) {
    final Path2D.Double check = new Path2D.Double();
    check.moveTo(box.x + box.width * 0.22, box.y + box.height * 0.55);
    check.lineTo(box.x + box.width * 0.43, box.y + box.height * 0.74);
    check.lineTo(box.x + box.width * 0.78, box.y + box.height * 0.32);
    g2.setColor(tint);
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(check);
  }

  /**
   * Shared chassis for the pane's three interactive strips: hover/pressed tracking (activation on
   * press — macOS drops MOUSE_CLICKED under rapid clicks), an arrow-key focus cursor with
   * Space/Enter activation, and state-layer painting. Subclasses supply geometry, cell colors, and
   * the activation.
   */
  private abstract class CellStrip extends JComponent {

    int cursor = -1;
    int hover = -1;
    int pressed = -1;

    CellStrip() {
      setOpaque(false);
      setFocusable(true);
      setAlignmentX(LEFT_ALIGNMENT);
      final MouseAdapter mouse =
          new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
              setHover(indexAt(e.getPoint()));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
              setHover(-1);
              pressed = -1;
            }

            @Override
            public void mousePressed(final MouseEvent e) {
              if (!isInteractive()) {
                return;
              }
              final int index = indexAt(e.getPoint());
              if (index >= 0) {
                pressed = index;
                cursor = index;
                requestFocusInWindow();
                activate(index);
              }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
              pressed = -1;
              repaint();
            }
          };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
      addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
              if (cursor < 0) {
                cursor = 0;
              }
              repaint();
            }

            @Override
            public void focusLost(final FocusEvent e) {
              repaint();
            }
          });
      bindKey(KeyEvent.VK_LEFT, () -> moveCursor(ltr() ? -1 : 1));
      bindKey(KeyEvent.VK_RIGHT, () -> moveCursor(ltr() ? 1 : -1));
      bindKey(KeyEvent.VK_UP, () -> moveCursor(-columns()));
      bindKey(KeyEvent.VK_DOWN, () -> moveCursor(columns()));
      bindKey(KeyEvent.VK_SPACE, this::activateCursor);
      bindKey(KeyEvent.VK_ENTER, this::activateCursor);
    }

    abstract int count();

    abstract int columns();

    abstract Rectangle cellRect(int index);

    abstract Color cellColor(int index);

    abstract void activate(int index);

    abstract String stripName();

    abstract String cellName(int index);

    boolean isInteractive() {
      return SwatchesPane.this.isEnabled() && picker().isEnabled();
    }

    final boolean ltr() {
      return getComponentOrientation().isLeftToRight();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleCellStrip();
      }
      return accessibleContext;
    }

    /** Names the strip and describes the focused cell for assistive tech. */
    private final class AccessibleCellStrip extends AccessibleJComponent {

      @Override
      public javax.accessibility.AccessibleRole getAccessibleRole() {
        return javax.accessibility.AccessibleRole.LIST;
      }

      @Override
      public String getAccessibleName() {
        return stripName();
      }

      @Override
      public String getAccessibleDescription() {
        return cursor >= 0 && cursor < count() ? cellName(cursor) : null;
      }
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

    private void moveCursor(final int delta) {
      final int next = Math.max(0, Math.min(count() - 1, (cursor < 0 ? 0 : cursor) + delta));
      if (next != cursor) {
        cursor = next;
        repaint();
      }
    }

    private void activateCursor() {
      if (cursor >= 0 && cursor < count()) {
        activate(cursor);
      }
    }

    private void setHover(final int index) {
      if (hover != index) {
        hover = index;
        repaint();
      }
    }

    private int indexAt(final Point point) {
      for (int i = 0; i < count(); i++) {
        if (cellRect(i).contains(point)) {
          return i;
        }
      }
      return -1;
    }

    final Graphics2D prepare(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (!isInteractive()) {
        g2.setComposite(
            java.awt.AlphaComposite.SrcOver.derive(StateLayer.disabledContentOpacity()));
      }
      return g2;
    }

    final Color stateLayered(final Color base, final int index) {
      Color result = base;
      final Color tint = contrastTint(base);
      if (isInteractive()) {
        if (index == pressed) {
          result = StateLayer.PRESSED.over(result, tint);
        } else if (index == hover) {
          result = StateLayer.HOVER.over(result, tint);
        }
        if (isFocusOwner() && index == cursor) {
          result = StateLayer.FOCUS.over(result, tint);
        }
      }
      return result;
    }
  }

  private final class HueGrid extends CellStrip {

    @Override
    int count() {
      return MaterialSwatchCatalog.hues().size();
    }

    @Override
    int columns() {
      return GRID_COLUMNS;
    }

    @Override
    Rectangle cellRect(final int index) {
      final int pitchX = Math.max(1, getWidth() / GRID_COLUMNS);
      int col = index % GRID_COLUMNS;
      if (!ltr()) {
        col = GRID_COLUMNS - 1 - col;
      }
      final int row = index / GRID_COLUMNS;
      return new Rectangle(col * pitchX, row * GRID_ROW_PITCH, pitchX, GRID_ROW_PITCH);
    }

    @Override
    Color cellColor(final int index) {
      return MaterialSwatchCatalog.hues()
          .get(index)
          .shades()[MaterialSwatchCatalog.REPRESENTATIVE_SHADE];
    }

    @Override
    void activate(final int index) {
      selectHue(index);
    }

    @Override
    String stripName() {
      return "Hue swatches";
    }

    @Override
    String cellName(final int index) {
      final MaterialSwatchCatalog.Hue hue = MaterialSwatchCatalog.hues().get(index);
      return hue.name()
          + " "
          + MaterialSwatchCatalog.shadeName(MaterialSwatchCatalog.REPRESENTATIVE_SHADE)
          + " · "
          + ColorHex.format(cellColor(index), false);
    }

    @Override
    public Dimension getPreferredSize() {
      final int rows = (count() + GRID_COLUMNS - 1) / GRID_COLUMNS;
      return new Dimension(0, rows * GRID_ROW_PITCH);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = prepare(g);
      try {
        for (int i = 0; i < count(); i++) {
          final Rectangle cell = cellRect(i);
          final Color base = cellColor(i);
          final int x = cell.x + (cell.width - CIRCLE_DIAMETER) / 2;
          final int y = cell.y + (cell.height - CIRCLE_DIAMETER) / 2;
          g2.setColor(stateLayered(base, i));
          g2.fillOval(x, y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
          g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
          g2.drawOval(x, y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
          if (matchesCurrent(base)) {
            g2.setColor(ColorRole.PRIMARY.resolve());
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x - 3, y - 3, CIRCLE_DIAMETER + 6, CIRCLE_DIAMETER + 6);
            paintCheck(
                g2,
                new Rectangle(x + 5, y + 5, CIRCLE_DIAMETER - 10, CIRCLE_DIAMETER - 10),
                contrastTint(base));
          }
        }
      } finally {
        g2.dispose();
      }
    }
  }

  private final class ShadeStrip extends CellStrip {

    @Override
    int count() {
      return MaterialSwatchCatalog.SHADE_COUNT;
    }

    @Override
    int columns() {
      return MaterialSwatchCatalog.SHADE_COUNT;
    }

    @Override
    Rectangle cellRect(final int index) {
      final int width = getWidth();
      final int position = ltr() ? index : count() - 1 - index;
      final int segment = (width - (count() - 1) * STRIP_GAP) / count();
      final int x = position * (segment + STRIP_GAP);
      final int w = position == count() - 1 ? width - x : segment;
      return new Rectangle(x, 0, w, STRIP_HEIGHT);
    }

    @Override
    Color cellColor(final int index) {
      return MaterialSwatchCatalog.hues().get(activeHue).shades()[index];
    }

    @Override
    void activate(final int index) {
      selectShade(index);
    }

    @Override
    String stripName() {
      return "Shades of " + MaterialSwatchCatalog.hues().get(activeHue).name();
    }

    @Override
    String cellName(final int index) {
      return MaterialSwatchCatalog.hues().get(activeHue).name()
          + " "
          + MaterialSwatchCatalog.shadeName(index)
          + " · "
          + ColorHex.format(cellColor(index), false);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(0, STRIP_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, STRIP_HEIGHT);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = prepare(g);
      try {
        g2.clip(
            new RoundRectangle2D.Double(
                0, 0, getWidth(), STRIP_HEIGHT, STRIP_HEIGHT, STRIP_HEIGHT));
        for (int i = 0; i < count(); i++) {
          final Rectangle cell = cellRect(i);
          final Color base = cellColor(i);
          g2.setColor(stateLayered(base, i));
          g2.fillRect(cell.x, cell.y, cell.width, cell.height);
          if (matchesCurrent(base)) {
            g2.setColor(ColorRole.PRIMARY.resolve());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(cell.x + 1, cell.y + 1, cell.width - 3, cell.height - 3);
            final int box = 14;
            paintCheck(
                g2,
                new Rectangle(
                    cell.x + (cell.width - box) / 2, cell.y + (cell.height - box) / 2, box, box),
                contrastTint(base));
          }
        }
      } finally {
        g2.dispose();
      }
    }
  }

  private final class RecentRow extends CellStrip {

    @Override
    int count() {
      return picker().recentColors().size();
    }

    @Override
    int columns() {
      return ElwhaColorPicker.RECENT_CAPACITY;
    }

    @Override
    Rectangle cellRect(final int index) {
      final int top = labelHeight() + SpaceScale.SM.px();
      final int x =
          ltr()
              ? index * RECENT_PITCH
              : getWidth() - (index + 1) * RECENT_PITCH + (RECENT_PITCH - CIRCLE_DIAMETER);
      return new Rectangle(x, top, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
    }

    @Override
    Color cellColor(final int index) {
      return picker().recentColors().get(index);
    }

    @Override
    void activate(final int index) {
      selectRecent(index);
    }

    @Override
    String stripName() {
      return "Recent colors";
    }

    @Override
    String cellName(final int index) {
      return ColorHex.format(cellColor(index), picker().isAlphaEnabled());
    }

    private int labelHeight() {
      return getFontMetrics(TypeRole.LABEL_MEDIUM.resolve()).getHeight();
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(0, labelHeight() + SpaceScale.SM.px() + CIRCLE_DIAMETER);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = prepare(g);
      try {
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final Font labelFont = TypeRole.LABEL_MEDIUM.resolve();
        final FontMetrics fm = g2.getFontMetrics(labelFont);
        g2.setFont(labelFont);
        g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
        g2.drawString(
            picker().recentColors().isEmpty() ? "Recent — pick a color to begin" : "Recent",
            0,
            fm.getAscent());
        for (int i = 0; i < count(); i++) {
          final Rectangle cell = cellRect(i);
          final Color base = cellColor(i);
          g2.setColor(stateLayered(base, i));
          g2.fillOval(cell.x, cell.y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
          g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
          g2.drawOval(cell.x, cell.y, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
          if (matchesCurrent(base)) {
            g2.setColor(ColorRole.PRIMARY.resolve());
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cell.x - 3, cell.y - 3, CIRCLE_DIAMETER + 6, CIRCLE_DIAMETER + 6);
          }
        }
      } finally {
        g2.dispose();
      }
    }
  }
}
