package com.owspfm.elwha.card.list;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.list.ElwhaList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
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

/**
 * V3 ElwhaCardList — a vertical or grid list of {@link ElwhaCard} primitives backed by a {@link
 * CardListModel} and projected through a {@link Function} cell renderer. Implements the {@link
 * ElwhaList} cross-cutting contract (orientation, gap, padding, empty/loading, filter/sort) and
 * owns a {@link CardSelectionModel} for selection.
 *
 * <p><strong>M3-compliant drag-reorder a11y.</strong> Per the M3 doctrine cited in {@code
 * elwha-card-v3-spec.md} §16.4 ("any dragging or swiping interactions need a single-pointer
 * alternative, like selecting the same actions from a menu"), this list ships:
 *
 * <ul>
 *   <li>Keyboard reorder: Cmd+↑ / Cmd+↓ move the focused card up / down; Delete or Cmd+Backspace
 *       removes the focused card.
 *   <li>Right-click context menu: Move up / Move down / Delete actions over the active row. Menu
 *       placement avoids overlapping the row.
 * </ul>
 *
 * <p><strong>Mouse drag-reorder is deferred.</strong> The mechanical V1 port is a separate
 * iteration; the M3 blocker for v0.2 is the single-pointer alternative above, which is shipped
 * here. Drag-handle cursor resources stay bundled at {@code card/v1/list/cursors/} for chip-side
 * reuse; the V3 list will pick them up when mouse drag lands.
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
    card.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent e) {
            maybeShowContextMenu(e, item, card);
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            maybeShowContextMenu(e, item, card);
          }
        });
    installKeyboardReorder(card, item);
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
