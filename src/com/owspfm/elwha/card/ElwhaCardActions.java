package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * The card-actions layout primitive — composes action affordances into M3's two-segment row:
 * leading (left-anchored) + flex gap + trailing (right-anchored). Intra-segment spacing {@link
 * SpaceScale#SM} (8 dp); inter-segment is flex.
 *
 * <p>Either segment may be empty. M3 doctrine: lone promo actions go leading; paired actions or
 * overflow-bearing rows go trailing.
 *
 * <p><strong>Wrap behavior (#17).</strong> When the available width fits the single-row layout
 * (leading + flex + trailing), actions render on one row. When it doesn't, the leading segment
 * wraps onto its own row(s) at the top (left-aligned per row), and the trailing segment wraps onto
 * its own row(s) below (right-aligned per row). Within a segment, items pack greedily — as many fit
 * per row as possible; new row starts when the next item would overflow. At widths below a single
 * button width, the row still places one item per row but the button visually clips at the cell
 * edge (per spec §3.4 no-min-width contract).
 *
 * <p>The chassis's {@code VerticalCardLayout} calls {@link #heightForSlotWidth(int)} so it can
 * reserve the right amount of vertical space for the wrapped rows — without that hook,
 * preferred-size queries (which carry no width context) would always report single-row height and
 * the wrapped rows would overflow into siblings.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §5.3.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardActions extends JComponent {

  private final List<JComponent> leading = new ArrayList<>();
  private final List<JComponent> trailing = new ArrayList<>();

  /** Creates an empty action row. */
  public ElwhaCardActions() {
    setLayout(new WrappingActionRowLayout());
    setOpaque(false);
  }

  /**
   * Adds an action to the leading (left-anchored) segment.
   *
   * @param action the action component (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code action} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions addLeading(final JComponent action) {
    Objects.requireNonNull(action, "action");
    leading.add(action);
    add(action);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Removes every leading action.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions clearLeading() {
    for (final JComponent c : leading) {
      remove(c);
    }
    leading.clear();
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return an unmodifiable snapshot of the leading actions in insertion order
   * @version v0.2.0
   * @since v0.2.0
   */
  public List<JComponent> getLeadingActions() {
    return Collections.unmodifiableList(new ArrayList<>(leading));
  }

  /**
   * Adds an action to the trailing (right-anchored) segment.
   *
   * @param action the action component (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code action} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions addTrailing(final JComponent action) {
    Objects.requireNonNull(action, "action");
    trailing.add(action);
    add(action);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Removes every trailing action.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions clearTrailing() {
    for (final JComponent c : trailing) {
      remove(c);
    }
    trailing.clear();
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return an unmodifiable snapshot of the trailing actions in insertion order
   * @version v0.2.0
   * @since v0.2.0
   */
  public List<JComponent> getTrailingActions() {
    return Collections.unmodifiableList(new ArrayList<>(trailing));
  }

  /**
   * Computes the height this action row needs at a given slot width — including wrapped rows when
   * the single-row layout exceeds the width. Called by {@link ElwhaCard}'s {@code
   * VerticalCardLayout} so the chassis reserves enough vertical space for wraps (preferred-size
   * queries carry no width context, so this height-for-width hook is the workaround).
   *
   * @param slotWidth the cell width the chassis will assign in pixels; {@code <= 0} returns the
   *     single-row height as a fallback
   * @return the height in pixels the chassis should reserve
   * @version v0.2.0
   * @since v0.2.0
   */
  int heightForSlotWidth(final int slotWidth) {
    final int gap = SpaceScale.SM.px();
    final int rowH = rowHeight();
    if (rowH == 0) {
      return 0;
    }
    if (slotWidth <= 0) {
      return rowH;
    }
    final int leadingW = sumWithGaps(leading);
    final int trailingW = sumWithGaps(trailing);
    final int interGap = (leadingW > 0 && trailingW > 0) ? gap : 0;
    final int singleRowW = leadingW + interGap + trailingW;
    if (singleRowW <= slotWidth) {
      return rowH;
    }
    final int leadingRows = leading.isEmpty() ? 0 : wrappedRowCount(leading, slotWidth, gap);
    final int trailingRows = trailing.isEmpty() ? 0 : wrappedRowCount(trailing, slotWidth, gap);
    final int totalRows = leadingRows + trailingRows;
    if (totalRows <= 0) {
      return rowH;
    }
    return totalRows * rowH + (totalRows - 1) * gap;
  }

  /** Sum of preferred widths + intra-segment gaps for a non-empty list, 0 for an empty list. */
  private int sumWithGaps(final List<JComponent> items) {
    if (items.isEmpty()) {
      return 0;
    }
    final int gap = SpaceScale.SM.px();
    int w = 0;
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        w += gap;
      }
      w += items.get(i).getPreferredSize().width;
    }
    return w;
  }

  /** Max preferred height across all leading + trailing children — single-row height. */
  private int rowHeight() {
    int h = 0;
    for (final JComponent c : leading) {
      h = Math.max(h, c.getPreferredSize().height);
    }
    for (final JComponent c : trailing) {
      h = Math.max(h, c.getPreferredSize().height);
    }
    return h;
  }

  /**
   * Greedy wrap-row counter: places items left-to-right, starting a new row when the next item
   * would overflow {@code available}. At least one item per row (so an item wider than {@code
   * available} gets a row alone and visually clips).
   */
  private static int wrappedRowCount(
      final List<JComponent> items, final int available, final int gap) {
    int rows = 1;
    int rowW = 0;
    for (final JComponent c : items) {
      final int w = c.getPreferredSize().width;
      final int wantW = rowW + (rowW > 0 ? gap : 0) + w;
      if (wantW > available && rowW > 0) {
        rows++;
        rowW = w;
      } else {
        rowW = wantW;
      }
    }
    return rows;
  }

  /** Internal alignment marker for the wrap layout. */
  private enum Align {
    LEADING,
    TRAILING
  }

  /**
   * Custom {@link LayoutManager} that places leading items left-anchored and trailing items
   * right-anchored on a single row when they fit, and wraps to additional rows otherwise — leading
   * rows on top, trailing rows below, each row keeping its segment alignment.
   *
   * <p>Replaces the prior {@code BorderLayout(WEST + glue + EAST)} + {@code FlowLayout} combo,
   * which couldn't wrap and silently clipped at narrow widths.
   */
  private final class WrappingActionRowLayout implements LayoutManager {

    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // no-op — we track positions via the leading / trailing lists.
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // no-op — list maintenance is the action-add/clear methods' responsibility.
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final int gap = SpaceScale.SM.px();
      final int leadingW = sumWithGaps(leading);
      final int trailingW = sumWithGaps(trailing);
      final int interGap = (leadingW > 0 && trailingW > 0) ? gap : 0;
      return new Dimension(leadingW + interGap + trailingW, rowHeight());
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      // Min width = the widest single button (the chassis can compress us down to that without
      // any item visually clipping); height stays at row height since wrap doesn't change the
      // per-row height, just the row count.
      int maxButtonW = 0;
      for (final JComponent c : leading) {
        maxButtonW = Math.max(maxButtonW, c.getPreferredSize().width);
      }
      for (final JComponent c : trailing) {
        maxButtonW = Math.max(maxButtonW, c.getPreferredSize().width);
      }
      return new Dimension(maxButtonW, rowHeight());
    }

    @Override
    public void layoutContainer(final Container parent) {
      final int gap = SpaceScale.SM.px();
      final int available = parent.getWidth();
      final int rowH = rowHeight();
      if (rowH == 0) {
        return;
      }
      final int leadingW = sumWithGaps(leading);
      final int trailingW = sumWithGaps(trailing);
      final int interGap = (leadingW > 0 && trailingW > 0) ? gap : 0;
      final int singleRowW = leadingW + interGap + trailingW;

      if (singleRowW <= available) {
        // Single-row case: leading at left, trailing at right, flex gap between.
        placeRow(leading, 0, rowH, Align.LEADING, available);
        placeRow(trailing, 0, rowH, Align.TRAILING, available);
      } else {
        // Wrap: leading row(s) on top, trailing row(s) below.
        int y = 0;
        if (!leading.isEmpty()) {
          y = placeWrappedSegment(leading, y, rowH, Align.LEADING, available, gap);
        }
        if (!trailing.isEmpty()) {
          placeWrappedSegment(trailing, y, rowH, Align.TRAILING, available, gap);
        }
      }
    }

    /** Places one row's items at {@code y}, aligned per {@code align}. */
    private void placeRow(
        final List<JComponent> items,
        final int y,
        final int rowH,
        final Align align,
        final int available) {
      if (items.isEmpty()) {
        return;
      }
      final int gap = SpaceScale.SM.px();
      final int totalW = sumWithGaps(items);
      final int startX = (align == Align.LEADING) ? 0 : Math.max(0, available - totalW);
      int curX = startX;
      for (final JComponent c : items) {
        final Dimension p = c.getPreferredSize();
        final int by = y + Math.max(0, (rowH - p.height) / 2);
        c.setBounds(curX, by, p.width, p.height);
        curX += p.width + gap;
      }
    }

    /** Greedy wrap of a segment across multiple rows; returns the next-available Y. */
    private int placeWrappedSegment(
        final List<JComponent> items,
        final int startY,
        final int rowH,
        final Align align,
        final int available,
        final int gap) {
      int y = startY;
      int idx = 0;
      boolean firstRow = true;
      while (idx < items.size()) {
        int rowW = 0;
        final int firstOnRow = idx;
        while (idx < items.size()) {
          final int w = items.get(idx).getPreferredSize().width;
          final int wantW = rowW + (rowW > 0 ? gap : 0) + w;
          if (wantW > available && idx > firstOnRow) {
            break;
          }
          rowW = wantW;
          idx++;
        }
        if (!firstRow) {
          y += gap;
        }
        placeRow(items.subList(firstOnRow, idx), y, rowH, align, available);
        y += rowH;
        firstRow = false;
      }
      return y;
    }
  }
}
