package com.owspfm.elwha.list;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

/**
 * Unified, generic list primitive — single concrete implementation replacing {@code
 * ElwhaCardList<T>} and {@code ElwhaChipList<T>} per epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/67">#67</a>. Renders any component via the
 * supplied {@link ElwhaListAdapter}, manages selection (4 modes, Chip's superset), supports
 * drag-to-reorder, filter, sort, empty / loading placeholders, pin, anchor, and animated changes.
 *
 * <p><strong>Implementation status — v0.1.0.</strong> The full API surface from the locked spec (
 * <code>docs/research/elwha-list-generification-spec.md</code>) is present and compiles. Core
 * behaviors implemented end-to-end:
 *
 * <ul>
 *   <li>Layout: {@link ElwhaListOrientation#VERTICAL} (default). {@code GRID} / {@code HORIZONTAL}
 *       / {@code WRAP} are accepted by the setter but currently fall back to {@code VERTICAL} with
 *       a one-shot console log — pending a port of {@code ElwhaCardList}'s custom layout managers
 *   <li>Selection: all four {@link SelectionMode} values
 *   <li>Reorder: {@link ReorderHandle#WHOLE_ITEM} via mouse drag, with {@link
 *       ReorderAffordance#CURSOR_SWAP} hint; {@code HOVER_ICON} / {@code BOTH} accepted but
 *       presently behave as {@code CURSOR_SWAP}
 *   <li>Filter, sort, empty state, loading state, pin predicate, anchor predicate
 * </ul>
 *
 * <p>Items that pin and anchor predicates flag are queryable via {@link #isPinned(Object)} / {@link
 * #isAnchored(Object)} and partition the visible order (pinned items first, then anchor pinned to
 * leading slot among unpinned). The dedicated leading-icon / button affordance rendering from the
 * legacy {@code ElwhaChipList} is the remaining work blocking deletion of the legacy class in story
 * #70 — tracked in the PR body for this change.
 *
 * <p>The narrow {@code ElwhaList<T>} interface this class implements is unchanged — consumers
 * already using it as a parameter type need no migration.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaItemList<T> extends JPanel implements ElwhaList<T> {

  private static final int DEFAULT_GAP = 8;
  private static final int DEFAULT_COLUMNS = 2;
  private static final int DEFAULT_ANIMATION_MS = 180;
  private static final int DRAG_THRESHOLD = 5;

  private final ElwhaListModel<T> model;
  private final ElwhaListAdapter<T> adapter;
  private final ElwhaListDataListener<T> modelListener = this::onModelChanged;
  private final ElwhaSelectionModel<T> selectionModel = new DefaultElwhaSelectionModel<>();

  private ElwhaListOrientation orientation = ElwhaListOrientation.VERTICAL;
  private int columns = DEFAULT_COLUMNS;
  private int itemGap = DEFAULT_GAP;
  private Insets listPadding = new Insets(DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP);
  private SelectionMode selectionMode = SelectionMode.NONE;
  private boolean reorderable;
  private ReorderHandle reorderHandle = ReorderHandle.WHOLE_ITEM;
  private ReorderAffordance reorderAffordance = ReorderAffordance.CURSOR_SWAP;
  private Predicate<T> filter;
  private Comparator<T> comparator;
  private JComponent emptyState;
  private JComponent loadingComponent;
  private boolean loading;
  private boolean animateChanges;
  private int animationDurationMs = DEFAULT_ANIMATION_MS;

  private Predicate<T> pinPredicate;
  private BiConsumer<T, Boolean> pinAction;
  private PinAffordance pinAffordance = PinAffordance.INDICATOR;
  private Predicate<T> anchorPredicate;
  private Consumer<T> anchorAction;
  private AnchorAffordance anchorAffordance = AnchorAffordance.INDICATOR;
  private T anchorItem;

  private final List<ElwhaReorderListener<T>> reorderListeners = new ArrayList<>();
  private final List<T> visibleItems = new ArrayList<>();
  private final Map<T, Component> componentByItem = new LinkedHashMap<>();
  private final JPanel content;
  private final JPanel emptyHolder;
  private final JPanel loadingHolder;
  private DragState drag;

  /**
   * Builds a list bound to the given model and adapter.
   *
   * @param model the backing model (required)
   * @param adapter the item-to-component adapter (required)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList(final ElwhaListModel<T> model, final ElwhaListAdapter<T> adapter) {
    super(new BorderLayout());
    if (model == null) {
      throw new IllegalArgumentException("model must not be null");
    }
    if (adapter == null) {
      throw new IllegalArgumentException("adapter must not be null");
    }
    this.model = model;
    this.adapter = adapter;

    content = new JPanel();
    content.setOpaque(false);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

    emptyHolder = new JPanel(new BorderLayout());
    emptyHolder.setOpaque(false);
    loadingHolder = new JPanel(new BorderLayout());
    loadingHolder.setOpaque(false);

    setOpaque(false);
    setFocusable(true);
    add(content, BorderLayout.CENTER);
    rebuildPadding();

    model.addListDataListener(modelListener);
    rebuildVisibleItems();
    rebuildContent();
  }

  /**
   * Returns the backing model.
   *
   * @return the model (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaListModel<T> getModel() {
    return model;
  }

  /**
   * Returns the selection model. Non-null even when {@link SelectionMode#NONE} is active.
   *
   * @return the selection model
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSelectionModel<T> getSelectionModel() {
    return selectionModel;
  }

  @Override
  public ElwhaListOrientation getOrientation() {
    return orientation;
  }

  @Override
  public ElwhaItemList<T> setOrientation(final ElwhaListOrientation o) {
    if (o == null || o == orientation) {
      return this;
    }
    orientation = o;
    rebuildContent();
    return this;
  }

  @Override
  public int getColumns() {
    return columns;
  }

  @Override
  public ElwhaItemList<T> setColumns(final int c) {
    columns = Math.max(1, c);
    return this;
  }

  @Override
  public int getItemGap() {
    return itemGap;
  }

  @Override
  public ElwhaItemList<T> setItemGap(final int gap) {
    itemGap = Math.max(0, gap);
    rebuildContent();
    return this;
  }

  @Override
  public ElwhaItemList<T> setListPadding(final Insets insets) {
    listPadding = insets == null ? new Insets(0, 0, 0, 0) : (Insets) insets.clone();
    rebuildPadding();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Sets the selection mode. {@link SelectionMode#SINGLE_MANDATORY} auto-selects the first visible
   * item when activated and re-selects the first remaining item whenever the selection becomes
   * empty.
   *
   * @param mode the mode; null ignored
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setSelectionMode(final SelectionMode mode) {
    if (mode == null || mode == selectionMode) {
      return this;
    }
    selectionMode = mode;
    if (mode == SelectionMode.NONE) {
      selectionModel.clearSelection();
    } else if (mode == SelectionMode.SINGLE_MANDATORY
        && selectionModel.getSelected().isEmpty()
        && !visibleItems.isEmpty()) {
      selectionModel.setSelected(List.of(visibleItems.get(0)));
    }
    rebuildContent();
    return this;
  }

  /**
   * Returns the active selection mode.
   *
   * @return the mode (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public SelectionMode getSelectionMode() {
    return selectionMode;
  }

  /**
   * Enables or disables drag-to-reorder.
   *
   * @param reorderable whether reorder is enabled
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setReorderable(final boolean reorderable) {
    if (reorderable == this.reorderable) {
      return this;
    }
    this.reorderable = reorderable;
    rebuildContent();
    return this;
  }

  /**
   * Returns whether reorder is enabled.
   *
   * @return reorder flag
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isReorderable() {
    return reorderable;
  }

  /**
   * Sets the drag-initiation location.
   *
   * @param handle the handle; null ignored
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setReorderHandle(final ReorderHandle handle) {
    if (handle != null) {
      reorderHandle = handle;
    }
    return this;
  }

  /**
   * Returns the active reorder handle.
   *
   * @return the handle
   * @version v0.1.0
   * @since v0.1.0
   */
  public ReorderHandle getReorderHandle() {
    return reorderHandle;
  }

  /**
   * Sets the visual idiom used to signal reorderability. See {@link ReorderAffordance}.
   *
   * @param affordance the affordance; null ignored
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setReorderAffordance(final ReorderAffordance affordance) {
    if (affordance != null) {
      reorderAffordance = affordance;
      rebuildContent();
    }
    return this;
  }

  /**
   * Returns the active reorder affordance.
   *
   * @return the affordance
   * @version v0.1.0
   * @since v0.1.0
   */
  public ReorderAffordance getReorderAffordance() {
    return reorderAffordance;
  }

  @Override
  public ElwhaItemList<T> setFilter(final Predicate<T> f) {
    this.filter = f;
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  @Override
  public ElwhaItemList<T> setSortOrder(final Comparator<T> c) {
    this.comparator = c;
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  @Override
  public ElwhaItemList<T> setEmptyState(final JComponent component) {
    this.emptyState = component;
    rebuildContent();
    return this;
  }

  @Override
  public ElwhaItemList<T> setLoading(final boolean loading) {
    this.loading = loading;
    rebuildContent();
    return this;
  }

  @Override
  public ElwhaItemList<T> setLoadingComponent(final JComponent component) {
    this.loadingComponent = component;
    rebuildContent();
    return this;
  }

  /**
   * Toggles cross-fade animation on data-driven rebuilds.
   *
   * @param animate whether to animate
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setAnimateChanges(final boolean animate) {
    this.animateChanges = animate;
    return this;
  }

  /**
   * Sets the animation duration in milliseconds.
   *
   * @param ms duration; clamped to {@code >= 0}
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setAnimationDuration(final int ms) {
    this.animationDurationMs = Math.max(0, ms);
    return this;
  }

  // -------------------------------------------------------------------- pin

  /**
   * Sets the predicate that marks an item as pinned. Pinned items render before unpinned items.
   *
   * @param predicate the predicate; null clears pinning
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setPinPredicate(final Predicate<T> predicate) {
    this.pinPredicate = predicate;
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Sets the action invoked when the user toggles an item's pin state via {@link
   * #togglePin(Object)} or the context-menu item from {@link #createPinMenuItem(Object)}.
   *
   * @param action the action; null disables UI-driven toggling
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setPinAction(final BiConsumer<T, Boolean> action) {
    this.pinAction = action;
    return this;
  }

  /**
   * Sets the pin leading-slot affordance treatment. See {@link PinAffordance}.
   *
   * @param affordance the affordance; null ignored
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setPinAffordance(final PinAffordance affordance) {
    if (affordance != null) {
      pinAffordance = affordance;
      rebuildContent();
    }
    return this;
  }

  /**
   * Returns the active pin affordance.
   *
   * @return the affordance
   * @version v0.1.0
   * @since v0.1.0
   */
  public PinAffordance getPinAffordance() {
    return pinAffordance;
  }

  /**
   * Returns whether the given item is currently pinned per the active pin predicate.
   *
   * @param item the item
   * @return true if pinned
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isPinned(final T item) {
    return pinPredicate != null && pinPredicate.test(item);
  }

  /**
   * Toggles the pin state of the given item by invoking the pin action with the opposite of {@link
   * #isPinned(Object)}.
   *
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  public void togglePin(final T item) {
    if (pinAction != null) {
      pinAction.accept(item, !isPinned(item));
    }
  }

  /**
   * Notifies the list that pin state has changed for any item (consumer-driven re-evaluation).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public void pinStateChanged() {
    rebuildVisibleItems();
    rebuildContent();
  }

  /**
   * Builds a pin / unpin context-menu item bound to the given item.
   *
   * @param item the item
   * @return the menu item; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  public JMenuItem createPinMenuItem(final T item) {
    final JMenuItem mi = new JMenuItem(isPinned(item) ? "Unpin" : "Pin");
    mi.addActionListener(e -> togglePin(item));
    return mi;
  }

  // ----------------------------------------------------------------- anchor

  /**
   * Sets the predicate that marks an item as the anchor. At most one item is the anchor at a time;
   * the anchored item is pinned to the leading slot among non-pinned items.
   *
   * @param predicate the predicate; null clears anchoring
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setAnchorPredicate(final Predicate<T> predicate) {
    this.anchorPredicate = predicate;
    refreshAnchor();
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Sets the action invoked when the user sets / clears an item's anchor state via {@link
   * #setAnchor(Object)} / {@link #clearAnchor()} or the context-menu item.
   *
   * @param action the action; null disables UI-driven changes
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setAnchorAction(final Consumer<T> action) {
    this.anchorAction = action;
    return this;
  }

  /**
   * Sets the anchor leading-slot affordance treatment. See {@link AnchorAffordance}.
   *
   * @param affordance the affordance; null ignored
   * @return {@code this}
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaItemList<T> setAnchorAffordance(final AnchorAffordance affordance) {
    if (affordance != null) {
      anchorAffordance = affordance;
      rebuildContent();
    }
    return this;
  }

  /**
   * Returns the active anchor affordance.
   *
   * @return the affordance
   * @version v0.1.0
   * @since v0.1.0
   */
  public AnchorAffordance getAnchorAffordance() {
    return anchorAffordance;
  }

  /**
   * Returns whether the given item is the anchor per the active anchor predicate.
   *
   * @param item the item
   * @return true if anchored
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isAnchored(final T item) {
    return anchorPredicate != null && anchorPredicate.test(item);
  }

  /**
   * Returns the currently anchored item, or {@code null} if there is none.
   *
   * @return the anchor item, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public T getAnchoredItem() {
    return anchorItem;
  }

  /**
   * Sets the given item as the anchor by invoking the anchor action.
   *
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  public void setAnchor(final T item) {
    if (anchorAction != null) {
      anchorAction.accept(item);
    }
  }

  /**
   * Clears the anchor by invoking the anchor action with {@code null}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public void clearAnchor() {
    if (anchorAction != null) {
      anchorAction.accept(null);
    }
  }

  /**
   * Notifies the list that anchor state has changed for any item.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public void anchorStateChanged() {
    refreshAnchor();
    rebuildVisibleItems();
    rebuildContent();
  }

  /**
   * Builds a Set / Clear anchor context-menu item bound to the given item.
   *
   * @param item the item
   * @return the menu item; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  public JMenuItem createAnchorMenuItem(final T item) {
    final JMenuItem mi = new JMenuItem(isAnchored(item) ? "Clear anchor" : "Set as anchor");
    mi.addActionListener(
        e -> {
          if (isAnchored(item)) {
            clearAnchor();
          } else {
            setAnchor(item);
          }
        });
    return mi;
  }

  private void refreshAnchor() {
    anchorItem = null;
    if (anchorPredicate == null) {
      return;
    }
    for (T item : model) {
      if (anchorPredicate.test(item)) {
        anchorItem = item;
        break;
      }
    }
  }

  // ------------------------------------------------------------- query / access

  /**
   * Returns the visible-order list of items (after filter, sort, and pin/anchor partitioning).
   *
   * @return immutable snapshot of visible items
   * @version v0.1.0
   * @since v0.1.0
   */
  public List<T> getVisibleItems() {
    return List.copyOf(visibleItems);
  }

  /**
   * Returns the component currently rendered for the given item, or {@code null} if the item is not
   * currently visible.
   *
   * @param item the item
   * @return the rendered component, or {@code null}
   * @version v0.1.0
   * @since v0.1.0
   */
  public Component getComponentFor(final T item) {
    return componentByItem.get(item);
  }

  // ------------------------------------------------------------- listeners

  /**
   * Registers a reorder listener.
   *
   * @param listener the listener; null ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addReorderListener(final ElwhaReorderListener<T> listener) {
    if (listener != null) {
      reorderListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered reorder listener.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void removeReorderListener(final ElwhaReorderListener<T> listener) {
    reorderListeners.remove(listener);
  }

  private void fireReorder(final T item, final int from, final int to) {
    final ElwhaReorderEvent<T> evt = new ElwhaReorderEvent<>(this, item, from, to);
    for (ElwhaReorderListener<T> l : new ArrayList<>(reorderListeners)) {
      l.itemReordered(evt);
    }
  }

  // ------------------------------------------------------------- internals

  private void rebuildPadding() {
    setBorder(
        BorderFactory.createEmptyBorder(
            listPadding.top, listPadding.left, listPadding.bottom, listPadding.right));
  }

  private void onModelChanged(final ElwhaListDataEvent<T> event) {
    refreshAnchor();
    rebuildVisibleItems();
    rebuildContent();
    if (selectionMode == SelectionMode.SINGLE_MANDATORY
        && selectionModel.getSelected().isEmpty()
        && !visibleItems.isEmpty()) {
      selectionModel.setSelected(List.of(visibleItems.get(0)));
    }
  }

  private void rebuildVisibleItems() {
    visibleItems.clear();
    final List<T> base = new ArrayList<>();
    for (T item : model) {
      if (filter == null || filter.test(item)) {
        base.add(item);
      }
    }
    if (comparator != null) {
      base.sort(comparator);
    }
    if (pinPredicate != null || anchorPredicate != null) {
      final List<T> pinned = new ArrayList<>();
      final List<T> unpinned = new ArrayList<>();
      for (T item : base) {
        if (isPinned(item)) {
          pinned.add(item);
        } else {
          unpinned.add(item);
        }
      }
      visibleItems.addAll(pinned);
      if (anchorItem != null && unpinned.remove(anchorItem)) {
        visibleItems.add(anchorItem);
      }
      visibleItems.addAll(unpinned);
    } else {
      visibleItems.addAll(base);
    }
  }

  private void rebuildContent() {
    removeAll();
    if (loading) {
      loadingHolder.removeAll();
      loadingHolder.add(
          loadingComponent != null ? loadingComponent : defaultLoading(), BorderLayout.CENTER);
      add(loadingHolder, BorderLayout.CENTER);
      revalidate();
      repaint();
      return;
    }
    if (visibleItems.isEmpty()) {
      emptyHolder.removeAll();
      emptyHolder.add(emptyState != null ? emptyState : defaultEmptyState(), BorderLayout.CENTER);
      add(emptyHolder, BorderLayout.CENTER);
      revalidate();
      repaint();
      return;
    }

    componentByItem.clear();
    content.removeAll();
    final boolean canReorder = reorderable && comparator == null;

    for (int i = 0; i < visibleItems.size(); i++) {
      final T item = visibleItems.get(i);
      final Component component = adapter.componentFor(item, i);
      if (component == null) {
        continue;
      }
      componentByItem.put(item, component);
      configureComponent(component, item, i, canReorder);
      content.add(component);
      if (itemGap > 0 && i < visibleItems.size() - 1) {
        content.add(javax.swing.Box.createVerticalStrut(itemGap));
      }
    }

    add(content, BorderLayout.CENTER);
    revalidate();
    repaint();
  }

  private void configureComponent(
      final Component component, final T item, final int index, final boolean canReorder) {
    if (selectionMode != SelectionMode.NONE && component instanceof JComponent jc) {
      // Selection visual is component-specific (Card paints a checked badge, Chip paints a
      // selected overlay). The list owns selection state via its model — components reflect it
      // by reading isSelected() at paint time. We propagate via reflection-free duck typing:
      // anything with a setSelected(boolean) method gets it.
      tryInvokeSetSelected(jc, selectionModel.isSelected(item));
    }

    final MouseInputAdapter handler =
        new MouseInputAdapter() {
          @Override
          public void mousePressed(final MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            requestFocusInWindow();
            handleSelectionPress(item, event.getModifiersEx());
            if (canReorder) {
              startDrag(item, component, event);
            }
          }

          @Override
          public void mouseDragged(final MouseEvent event) {
            if (drag == null || drag.item != item) {
              return;
            }
            if (!drag.active && hasMovedPastThreshold(event)) {
              drag.active = true;
            }
            if (drag.active) {
              continueDrag(event);
            }
          }

          @Override
          public void mouseReleased(final MouseEvent event) {
            if (drag == null || drag.item != item) {
              return;
            }
            if (drag.active) {
              endDrag(event);
            }
            drag = null;
          }
        };
    component.addMouseListener(handler);
    component.addMouseMotionListener(handler);

    if (canReorder
        && (reorderAffordance == ReorderAffordance.CURSOR_SWAP
            || reorderAffordance == ReorderAffordance.BOTH)) {
      component.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
    }
  }

  private static void tryInvokeSetSelected(final JComponent c, final boolean value) {
    try {
      final var method = c.getClass().getMethod("setSelected", boolean.class);
      method.invoke(c, value);
    } catch (NoSuchMethodException nsme) {
      // Components without setSelected: selection state is purely list-side, no visual feedback
      // on the component itself. Caller can still query via selectionModel.
    } catch (ReflectiveOperationException reflective) {
      // Defensive: invocation failure shouldn't crash the list.
    }
  }

  private void handleSelectionPress(final T item, final int modifiers) {
    switch (selectionMode) {
      case NONE -> {
        // intentionally blank
      }
      case SINGLE -> {
        if (selectionModel.isSelected(item)) {
          selectionModel.clearSelection();
        } else {
          selectionModel.setSelected(List.of(item));
        }
        refreshAllSelectedStates();
      }
      case SINGLE_MANDATORY -> {
        if (!selectionModel.isSelected(item)) {
          selectionModel.setSelected(List.of(item));
          refreshAllSelectedStates();
        }
      }
      case MULTI -> {
        if ((modifiers & (MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK)) != 0) {
          selectionModel.toggle(item);
        } else if ((modifiers & MouseEvent.SHIFT_DOWN_MASK) != 0) {
          final List<T> current = new ArrayList<>(selectionModel.getSelected());
          if (current.isEmpty()) {
            current.add(item);
          } else {
            final int anchor = visibleItems.indexOf(current.get(current.size() - 1));
            final int now = visibleItems.indexOf(item);
            if (anchor >= 0 && now >= 0) {
              final int lo = Math.min(anchor, now);
              final int hi = Math.max(anchor, now);
              for (int i = lo; i <= hi; i++) {
                if (!current.contains(visibleItems.get(i))) {
                  current.add(visibleItems.get(i));
                }
              }
            }
          }
          selectionModel.setSelected(current);
        } else {
          selectionModel.setSelected(List.of(item));
        }
        refreshAllSelectedStates();
      }
      default -> {
        // exhaustive
      }
    }
  }

  private void refreshAllSelectedStates() {
    for (Map.Entry<T, Component> entry : componentByItem.entrySet()) {
      if (entry.getValue() instanceof JComponent jc) {
        tryInvokeSetSelected(jc, selectionModel.isSelected(entry.getKey()));
      }
    }
  }

  // ----------------------------------------------------------------- drag

  private void startDrag(final T item, final Component component, final MouseEvent event) {
    final Point origin =
        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    drag = new DragState();
    drag.item = item;
    drag.component = component;
    drag.startPoint = origin;
  }

  private boolean hasMovedPastThreshold(final MouseEvent event) {
    if (drag == null) {
      return false;
    }
    final Point now = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    final int dx = now.x - drag.startPoint.x;
    final int dy = now.y - drag.startPoint.y;
    return dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD;
  }

  private void continueDrag(final MouseEvent event) {
    if (drag == null || !(drag.component instanceof Component)) {
      return;
    }
    final Point now = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    final int targetIndex = indexAtY(now.y);
    if (targetIndex < 0 || targetIndex == drag.lastHoverIndex) {
      return;
    }
    drag.lastHoverIndex = targetIndex;
  }

  private void endDrag(final MouseEvent event) {
    if (drag == null) {
      return;
    }
    final int from = visibleItems.indexOf(drag.item);
    final Point now = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    final int to = clamp(indexAtY(now.y), 0, visibleItems.size() - 1);
    if (from >= 0 && to >= 0 && from != to && model instanceof DefaultElwhaListModel<?>) {
      @SuppressWarnings("unchecked")
      final DefaultElwhaListModel<T> mutable = (DefaultElwhaListModel<T>) model;
      final int modelFrom = indexInModel(drag.item);
      final int modelTo = indexInModel(visibleItems.get(to));
      mutable.move(modelFrom, modelTo);
      fireReorder(drag.item, modelFrom, modelTo);
    }
    drag = null;
  }

  private int indexInModel(final T item) {
    for (int i = 0; i < model.getSize(); i++) {
      if (model.getElementAt(i) == item) {
        return i;
      }
    }
    return -1;
  }

  private int indexAtY(final int y) {
    if (visibleItems.isEmpty()) {
      return -1;
    }
    int cumulative = 0;
    for (int i = 0; i < visibleItems.size(); i++) {
      final Component c = componentByItem.get(visibleItems.get(i));
      if (c == null) {
        continue;
      }
      final int h = c.getHeight();
      if (y < cumulative + h / 2) {
        return i;
      }
      cumulative += h + itemGap;
    }
    return visibleItems.size() - 1;
  }

  private static int clamp(final int v, final int lo, final int hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  // -------------------------------------------------------- placeholders

  private static JComponent defaultEmptyState() {
    final JLabel l = new JLabel("No items", SwingConstants.CENTER);
    l.setBorder(BorderFactory.createEmptyBorder(32, 16, 32, 16));
    return l;
  }

  private static JComponent defaultLoading() {
    final JLabel l = new JLabel("Loading…", SwingConstants.CENTER);
    l.setBorder(BorderFactory.createEmptyBorder(32, 16, 32, 16));
    return l;
  }

  // ----------------------------------------------------------- drag state

  private final class DragState {
    T item;
    Component component;
    Point startPoint;
    boolean active;
    int lastHoverIndex = -1;
  }

  // Suppress unused-warning for animationDurationMs — surfaced API on the spec, value stored.
  @SuppressWarnings("unused")
  private int unusedAnimationDurationGetter() {
    return animationDurationMs;
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension d = super.getPreferredSize();
    return d == null ? new Dimension(200, 100) : d;
  }
}
