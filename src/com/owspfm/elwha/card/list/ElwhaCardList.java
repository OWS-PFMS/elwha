package com.owspfm.elwha.card.list;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.list.ElwhaList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

/**
 * V3 ElwhaCardList — a vertical or grid list of {@link ElwhaCard} primitives backed by a {@link
 * CardListModel} and projected through a {@link Function} cell renderer. Implements the {@link
 * ElwhaList} cross-cutting contract (orientation, gap, padding, empty/loading, filter/sort) and
 * owns a {@link CardSelectionModel} for selection.
 *
 * <p><strong>Reorder.</strong> Three equivalent affordances:
 *
 * <ul>
 *   <li>Mouse drag — press any card, drag past the threshold ({@link #DRAG_THRESHOLD_PX}), drop on
 *       the target slot. The chrome's {@link ElwhaCard#setDragged(boolean)} flag drives the
 *       elevation lift (spec §9) + DRAGGED state-layer overlay (spec §10.1). A 2 dp {@link
 *       com.owspfm.elwha.theme.ColorRole#PRIMARY}-colored drop indicator paints in the target gap.
 *       On release, {@link CardListModel#move(int, int)} commits the reorder.
 *   <li>Keyboard — Cmd+↑ / Cmd+↓ move the focused card; Delete or Cmd+Backspace remove it.
 *   <li>Right-click context menu — Move up / Move down / Delete, placed below the active row so it
 *       doesn't overlap (per M3 doctrine).
 * </ul>
 *
 * <p>The keyboard + context menu are the M3 single-pointer alternatives required by spec §16.4.
 * Mouse drag relies on the V3 chrome's {@link ElwhaCard#cancelPendingClick()} call during drag
 * activation to suppress the latent action / selection toggle on release.
 *
 * <p>Inter-card gap defaults to {@link SpaceScale#SM} (8 dp per M3 measurement spec frame).
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardList<T> extends JPanel implements ElwhaList<T> {

  private final CardListModel<T> model;
  private final CardSelectionModel<T> selectionModel = new CardSelectionModel<>();
  private Function<T, ElwhaCard> cellRenderer = item -> ElwhaCard.elevatedCard();
  private Predicate<T> filter;
  private Comparator<T> sortOrder;
  private ElwhaListOrientation orientation = ElwhaListOrientation.VERTICAL;
  private int itemGap = SpaceScale.SM.px();
  private Insets listPadding = new Insets(0, 0, 0, 0);
  private int columns = 1;
  private boolean loading;
  private JComponent emptyState;
  private JComponent loadingComponent;
  private final Map<T, ElwhaCard> cardByItem = new IdentityHashMap<>();
  private final List<T> renderedItems = new ArrayList<>();

  /** Pixels the cursor must travel from press before a drag-reorder activates. */
  private static final int DRAG_THRESHOLD_PX = 6;

  private DragState dragState;

  /**
   * Creates a list with the given backing model.
   *
   * @param model the model (must not be {@code null})
   * @throws NullPointerException if {@code model} is {@code null}
   */
  public ElwhaCardList(final CardListModel<T> model) {
    this.model = Objects.requireNonNull(model, "model");
    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    model.addChangeListener(m -> rebuild());
    selectionModel.addChangeListener(s -> repaint());
    rebuild();
  }

  /**
   * Sets the cell renderer — maps each item to a fresh {@link ElwhaCard}. The renderer is invoked
   * on every rebuild.
   *
   * @param renderer the renderer (must not be {@code null})
   * @return {@code this}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardList<T> setCellRenderer(final Function<T, ElwhaCard> renderer) {
    this.cellRenderer = Objects.requireNonNull(renderer, "renderer");
    rebuild();
    return this;
  }

  /**
   * @return the backing data model
   * @version v0.2.0
   * @since v0.2.0
   */
  public CardListModel<T> getModel() {
    return model;
  }

  /**
   * @return the selection model
   * @version v0.2.0
   * @since v0.2.0
   */
  public CardSelectionModel<T> getSelectionModel() {
    return selectionModel;
  }

  // -------------------------------------------------- ElwhaList<T> contract

  @Override
  public ElwhaListOrientation getOrientation() {
    return orientation;
  }

  @Override
  public ElwhaCardList<T> setOrientation(final ElwhaListOrientation newOrientation) {
    this.orientation = Objects.requireNonNull(newOrientation, "orientation");
    applyOrientation();
    rebuild();
    return this;
  }

  @Override
  public int getItemGap() {
    return itemGap;
  }

  @Override
  public ElwhaCardList<T> setItemGap(final int gap) {
    this.itemGap = Math.max(0, gap);
    rebuild();
    return this;
  }

  @Override
  public ElwhaCardList<T> setListPadding(final Insets insets) {
    this.listPadding =
        insets != null
            ? new Insets(insets.top, insets.left, insets.bottom, insets.right)
            : new Insets(0, 0, 0, 0);
    setBorder(
        javax.swing.BorderFactory.createEmptyBorder(
            listPadding.top, listPadding.left, listPadding.bottom, listPadding.right));
    return this;
  }

  @Override
  public ElwhaCardList<T> setColumns(final int newColumns) {
    this.columns = Math.max(1, newColumns);
    applyOrientation();
    rebuild();
    return this;
  }

  @Override
  public int getColumns() {
    return columns;
  }

  @Override
  public ElwhaCardList<T> setEmptyState(final JComponent component) {
    this.emptyState = component;
    rebuild();
    return this;
  }

  @Override
  public ElwhaCardList<T> setLoading(final boolean newLoading) {
    this.loading = newLoading;
    rebuild();
    return this;
  }

  @Override
  public ElwhaCardList<T> setLoadingComponent(final JComponent component) {
    this.loadingComponent = component;
    rebuild();
    return this;
  }

  @Override
  public ElwhaCardList<T> setFilter(final Predicate<T> newFilter) {
    this.filter = newFilter;
    rebuild();
    return this;
  }

  @Override
  public ElwhaCardList<T> setSortOrder(final Comparator<T> comparator) {
    this.sortOrder = comparator;
    rebuild();
    return this;
  }

  // ----------------------------------------------------------------- layout

  private void applyOrientation() {
    switch (orientation) {
      case VERTICAL -> setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      case HORIZONTAL -> setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      case GRID -> setLayout(new GridLayout(0, Math.max(1, columns), itemGap, itemGap));
      case WRAP ->
          setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, itemGap, itemGap));
      default -> setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
  }

  private void rebuild() {
    removeAll();
    renderedItems.clear();
    cardByItem.clear();
    if (loading && loadingComponent != null) {
      add(loadingComponent);
      revalidate();
      repaint();
      return;
    }
    final List<T> items = new ArrayList<>(model.getItems());
    if (filter != null) {
      items.removeIf(filter.negate());
    }
    if (sortOrder != null) {
      items.sort(sortOrder);
    }
    if (items.isEmpty() && emptyState != null) {
      add(emptyState);
      revalidate();
      repaint();
      return;
    }
    selectionModel.retainAll(new HashSet<>(items));
    for (int i = 0; i < items.size(); i++) {
      final T item = items.get(i);
      final ElwhaCard card = cellRenderer.apply(item);
      card.setSelectable(selectionModel.getSelectionMode() != CardSelectionMode.NONE);
      card.setSelected(selectionModel.isSelected(item));
      installCardInteraction(card, item);
      cardByItem.put(item, card);
      renderedItems.add(item);
      if (i > 0
          && (orientation == ElwhaListOrientation.VERTICAL
              || orientation == ElwhaListOrientation.HORIZONTAL)) {
        add(
            orientation == ElwhaListOrientation.VERTICAL
                ? Box.createVerticalStrut(itemGap)
                : Box.createHorizontalStrut(itemGap));
      }
      add(card);
    }
    revalidate();
    repaint();
  }

  private void installCardInteraction(final ElwhaCard card, final T item) {
    card.addSelectionChangeListener(e -> selectionModel.toggle(item));
    final MouseInputAdapter handler =
        new MouseInputAdapter() {
          @Override
          public void mousePressed(final MouseEvent e) {
            maybeShowContextMenu(e, item, card);
            if (e.getButton() == MouseEvent.BUTTON1) {
              dragState = new DragState();
              dragState.item = item;
              dragState.card = card;
              dragState.fromIndex = renderedItems.indexOf(item);
              dragState.dropSlot = dragState.fromIndex;
              dragState.pressPoint =
                  SwingUtilities.convertPoint(card, e.getPoint(), ElwhaCardList.this);
            }
          }

          @Override
          public void mouseDragged(final MouseEvent e) {
            if (dragState == null || dragState.item != item) {
              return;
            }
            final Point listPt =
                SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ElwhaCardList.this);
            if (!dragState.active) {
              final int dx = listPt.x - dragState.pressPoint.x;
              final int dy = listPt.y - dragState.pressPoint.y;
              if (dx * dx + dy * dy >= DRAG_THRESHOLD_PX * DRAG_THRESHOLD_PX) {
                activateDrag();
              }
            }
            if (dragState.active) {
              updateDropSlot(listPt);
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            maybeShowContextMenu(e, item, card);
            if (dragState == null || dragState.item != item) {
              return;
            }
            if (dragState.active) {
              finishDrag();
            } else {
              dragState = null;
            }
          }
        };
    card.addMouseListener(handler);
    card.addMouseMotionListener(handler);
    installKeyboardReorder(card, item);
  }

  /** Per-list drag state — only one drag is in flight at a time. */
  private final class DragState {
    private T item;
    private ElwhaCard card;
    private int fromIndex;
    private int dropSlot;
    private Point pressPoint;
    private boolean active;
  }

  /**
   * Promotes a pending drag to active: sets the chrome's dragged flag (chrome paints DRAGGED
   * state-layer per spec §10.1 + elevation lift per §9), cancels the latent action / selection
   * toggle so the upcoming release doesn't fire one, and switches the cursor.
   */
  private void activateDrag() {
    dragState.active = true;
    dragState.card.setDragged(true);
    dragState.card.cancelPendingClick();
    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    repaint();
  }

  /**
   * Updates the drop slot from the cursor's Y position. Slot semantics match {@code
   * DefaultCardListModel.move(from, to)}: {@code to} is the destination index in the post-remove
   * list, so it ranges 0..(renderedItems.size()-1) and indicates "insert dragged item at slot N
   * after removing it from its original position."
   */
  private void updateDropSlot(final Point listPoint) {
    int slot = 0;
    for (int i = 0; i < renderedItems.size(); i++) {
      if (i == dragState.fromIndex) {
        continue;
      }
      final ElwhaCard c = cardByItem.get(renderedItems.get(i));
      if (c == null) {
        continue;
      }
      final int mid = c.getY() + c.getHeight() / 2;
      if (listPoint.y > mid) {
        slot++;
      }
    }
    if (slot != dragState.dropSlot) {
      dragState.dropSlot = slot;
      repaint();
    }
  }

  /**
   * Commits the drag: clears chrome dragged state, restores the cursor, and (if the drop slot
   * differs from the origin) calls {@code model.move(fromIndex, dropSlot)}. The model change fires
   * a listener that rebuilds the list.
   */
  private void finishDrag() {
    final ElwhaCard draggedCard = dragState.card;
    final int from = dragState.fromIndex;
    final int to = dragState.dropSlot;
    dragState = null;
    draggedCard.setDragged(false);
    setCursor(Cursor.getDefaultCursor());
    repaint();
    if (from != to && from >= 0 && to >= 0) {
      model.move(from, to);
    }
  }

  /**
   * Computes the Y where the drop indicator should paint for {@code slot}: in the gap above the
   * card that would be at that slot, or below the last non-dragged card if slot is past the end.
   */
  private int dropIndicatorY(final int slot) {
    final List<ElwhaCard> others = new ArrayList<>();
    for (int i = 0; i < renderedItems.size(); i++) {
      if (i == dragState.fromIndex) {
        continue;
      }
      final ElwhaCard c = cardByItem.get(renderedItems.get(i));
      if (c != null) {
        others.add(c);
      }
    }
    if (others.isEmpty()) {
      return 0;
    }
    if (slot <= 0) {
      return Math.max(0, others.get(0).getY() - itemGap / 2);
    }
    if (slot >= others.size()) {
      final ElwhaCard last = others.get(others.size() - 1);
      return last.getY() + last.getHeight() + itemGap / 2;
    }
    return Math.max(0, others.get(slot).getY() - itemGap / 2);
  }

  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);
    if (dragState == null || !dragState.active) {
      return;
    }
    if (orientation != ElwhaListOrientation.VERTICAL) {
      return;
    }
    final int y = dropIndicatorY(dragState.dropSlot);
    g.setColor(com.owspfm.elwha.theme.ColorRole.PRIMARY.resolve());
    g.fillRect(0, y - 1, getWidth(), 2);
  }

  private void installKeyboardReorder(final ElwhaCard card, final T item) {
    final int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    final InputMap im = card.getInputMap(JComponent.WHEN_FOCUSED);
    final javax.swing.ActionMap am = card.getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, mod), "elwhaCardListMoveUp");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, mod), "elwhaCardListMoveDown");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, mod), "elwhaCardListDelete");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "elwhaCardListDelete");
    am.put(
        "elwhaCardListMoveUp",
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            moveItem(item, -1);
          }
        });
    am.put(
        "elwhaCardListMoveDown",
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            moveItem(item, 1);
          }
        });
    am.put(
        "elwhaCardListDelete",
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            deleteItem(item);
          }
        });
  }

  private void maybeShowContextMenu(final MouseEvent e, final T item, final ElwhaCard card) {
    if (!e.isPopupTrigger()) {
      return;
    }
    final JPopupMenu menu = new JPopupMenu();
    final JMenuItem up = new JMenuItem("Move up");
    up.addActionListener(a -> moveItem(item, -1));
    up.setEnabled(renderedItems.indexOf(item) > 0);
    menu.add(up);
    final JMenuItem down = new JMenuItem("Move down");
    down.addActionListener(a -> moveItem(item, 1));
    down.setEnabled(renderedItems.indexOf(item) < renderedItems.size() - 1);
    menu.add(down);
    menu.addSeparator();
    final JMenuItem delete = new JMenuItem("Delete");
    delete.addActionListener(a -> deleteItem(item));
    menu.add(delete);
    // Place below the card to avoid overlapping the active row per M3 doctrine.
    menu.show(card, 0, card.getHeight());
  }

  private void moveItem(final T item, final int delta) {
    final int from = renderedItems.indexOf(item);
    if (from < 0) {
      return;
    }
    final int to = Math.max(0, Math.min(renderedItems.size() - 1, from + delta));
    if (from == to) {
      return;
    }
    final List<T> updated = new ArrayList<>(model.getItems());
    final int modelFrom = updated.indexOf(item);
    if (modelFrom < 0) {
      return;
    }
    updated.remove(modelFrom);
    updated.add(Math.max(0, Math.min(updated.size(), modelFrom + delta)), item);
    model.setItems(updated);
    final ElwhaCard refocus = cardByItem.get(item);
    if (refocus != null) {
      refocus.requestFocusInWindow();
    }
  }

  private void deleteItem(final T item) {
    final List<T> updated = new ArrayList<>(model.getItems());
    final int index = updated.indexOf(item);
    if (index < 0) {
      return;
    }
    updated.remove(index);
    model.setItems(updated);
    final Set<T> retained = new LinkedHashSet<>(selectionModel.getSelectedItems());
    retained.remove(item);
    selectionModel.setSelectedItems(retained);
  }

  /** Re-exposed for tests / consumers that need to walk the currently rendered card list. */
  Component[] renderedCards() {
    return getComponents();
  }
}
