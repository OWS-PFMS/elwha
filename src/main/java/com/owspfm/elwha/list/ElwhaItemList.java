package com.owspfm.elwha.list;

import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

/**
 * The Elwha model-driven item list — one generic container that renders any domain item type
 * through an adapter, and the single successor to the parallel card-list and chip-list families
 * (spec {@code docs/research/elwha-list-generification-spec.md}).
 *
 * <p><strong>Architecture.</strong> Items are domain objects, not components: {@code T} is the item
 * type and an {@link ElwhaItemAdapter} maps each visible item to the {@link JComponent} that draws
 * it. That indirection is what lets one class serve chips, cards, and anything else. The list is
 * <em>not</em> built on {@link javax.swing.JList} — it hosts real interactive child components, so
 * a renderer-stamp model would defeat the point. Layout runs through four hand-written managers
 * (one per {@link ElwhaListOrientation}) rather than stock Swing layouts, because drag-reorder
 * needs to place children at animated positions and because filler struts as real children would
 * pollute both the accessibility tree and the item indices.
 *
 * <p><strong>Selection.</strong> The list owns it. A press consults the active {@link
 * SelectionMode}, mutates the {@link ElwhaSelectionModel}, and pushes the result onto every
 * rendered view through {@link ElwhaListItemView#setSelected(boolean)}. Selection is keyed on item
 * identity, so it survives filter and sort changes. See {@link SelectionMode} for the four modes'
 * semantics, including {@link SelectionMode#SINGLE_MANDATORY}'s auto-seeding.
 *
 * <p><strong>Reorder.</strong> Enabled by {@link MovementMode}. Dragging an item slides its
 * siblings into the opening gap; the drop slot is computed against the items' natural
 * (preferred-size) positions rather than their mid-animation ones, which is what keeps the target
 * from oscillating under the pointer. {@link MovementMode#PINNED} and {@link MovementMode#ANCHORED}
 * add a partition that both the render order and the drag clamps honor. Keyboard equivalents
 * (Cmd/Ctrl+Arrow to move, Delete to remove) and a context menu cover the single-pointer
 * requirement. Reorder is unavailable while a sort comparator is active, and while the backing
 * {@link ElwhaListModel} refuses {@link ElwhaListModel#move(int, int)}.
 *
 * <p><strong>Interaction &amp; motion.</strong> Selection commits on release-without-drag, not on
 * press — a click selects, a drag moves. {@link ReorderAffordance} chooses how draggability is
 * advertised. Sibling displacement and the optional add/remove fade both snap instantly under
 * {@link MorphAnimator#isReducedMotion()}.
 *
 * <p><strong>Right-to-left.</strong> All four orientations mirror under an RTL {@link
 * java.awt.ComponentOrientation}: items flow from the right edge, grid column zero sits on the
 * right, and drag slot computation mirrors with them.
 *
 * <p><strong>Accessibility.</strong> The list reports {@link AccessibleRole#LIST}, exposes exactly
 * the visible items as accessible children in render order, and implements {@link
 * AccessibleSelection} so assistive technology can read and drive the selection.
 *
 * <p><strong>Quick start</strong>:
 *
 * <pre>{@code
 * DefaultElwhaListModel<Factor> model = new DefaultElwhaListModel<>(factors);
 * ElwhaItemList<Factor> list =
 *     new ElwhaItemList<>(model, (factor, index) -> ElwhaChip.filterChip(factor.name()))
 *         .setSelectionMode(SelectionMode.MULTIPLE)
 *         .setMovementMode(MovementMode.MOVABLE);
 * list.addSelectionListener(event -> System.out.println(event.getSelected()));
 * }</pre>
 *
 * @param <T> the item type
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaItemList<T> extends JPanel implements Accessible, ElwhaList<T> {

  /** Pixels the pointer must travel from the press before a drag activates. */
  static final int DRAG_THRESHOLD = 5;

  private static final Logger LOG = Logger.getLogger(ElwhaItemList.class.getName());
  private static final int DEFAULT_GAP = 6;
  private static final int DEFAULT_COLUMNS = 4;
  private static final int ANIMATION_TICK_MS = 16;
  private static final int DEFAULT_ANIMATION_MS = 180;
  private static final int MIN_ANIMATION_MS = 50;
  private static final int HANDLE_FADE_MS = 120;
  private static final int DRAG_HANDLE_SIZE = 16;
  private static final int DRAG_HANDLE_INSET = 4;

  // Pin / anchor glyph size on chips — held at the pre-bump MaterialIcons default so chip visuals
  // do not regress against the 24 px M3 standard the shared default moved to.
  private static final int CHIP_ICON_SIZE = 14;

  // Model, adapter, selection ----------------------------------------------
  private ElwhaListModel<T> model;
  private ElwhaItemAdapter<T> adapter;
  private final ElwhaListDataListener<T> modelListener = this::onModelChanged;
  private ElwhaSelectionModel<T> selectionModel = new DefaultElwhaSelectionModel<>();
  private final ElwhaSelectionListener<T> selectionBridge = this::onSelectionChanged;
  private boolean modelReorderable;

  // Configuration ----------------------------------------------------------
  private ElwhaListOrientation orientation = ElwhaListOrientation.VERTICAL;
  private int columns = DEFAULT_COLUMNS;
  private int itemGap = DEFAULT_GAP;
  private Insets listPadding = new Insets(DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP);
  private SelectionMode selectionMode = SelectionMode.NONE;
  private MovementMode movementMode = MovementMode.STATIC;
  private ReorderAffordance reorderAffordance = ReorderAffordance.CURSOR_SWAP;
  private boolean sortBlocksReorderLogged;
  private Predicate<T> filter;
  private Comparator<T> comparator;
  private Predicate<T> pinPredicate;
  private BiConsumer<T, Boolean> pinAction;
  private Predicate<T> anchorPredicate;
  private Consumer<T> anchorAction;
  private IconAffordance pinAffordance = IconAffordance.INDICATOR;
  private IconAffordance anchorAffordance = IconAffordance.INDICATOR;
  private JComponent emptyState;
  private JComponent loadingComponent;
  private boolean loading;
  private boolean animateChanges;
  private int animationDurationMs = DEFAULT_ANIMATION_MS;

  // Listeners --------------------------------------------------------------
  private final List<ElwhaReorderListener<T>> reorderListeners = new ArrayList<>();
  private final List<ElwhaSelectionListener<T>> selectionListeners = new ArrayList<>();

  // Render cache: visible items in render order ----------------------------
  private final List<T> visibleItems = new ArrayList<>();
  private final Map<T, JComponent> viewByItem = new LinkedHashMap<>();
  private int focusedVisibleIndex = -1;
  private T selectionAnchor;

  // Layout panels ----------------------------------------------------------
  private final ContentPanel content;
  private final JPanel emptyHolder;
  private final JPanel loadingHolder;

  // Drag state -------------------------------------------------------------
  private DragState drag;
  private final Map<JComponent, Integer> displacedX = new HashMap<>();
  private final Map<JComponent, Integer> displacedY = new HashMap<>();
  private final Map<JComponent, Integer> targetX = new HashMap<>();
  private final Map<JComponent, Integer> targetY = new HashMap<>();
  private Timer dragAnimTimer;

  // Fade + drag-handle animation -------------------------------------------
  private float fadeAlpha = 1f;
  private Timer fadeTimer;
  private T handleHoverItem;
  private float handleAlpha;
  private Timer handleTimer;

  // -------------------------------------------------------------------- ctor

  /**
   * Builds a list bound to the given model and adapter.
   *
   * @param model the backing model
   * @param adapter the adapter mapping items to components
   * @throws NullPointerException if either argument is null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList(final ElwhaListModel<T> model, final ElwhaItemAdapter<T> adapter) {
    super(new BorderLayout());
    this.model = Objects.requireNonNull(model, "model");
    this.adapter = Objects.requireNonNull(adapter, "adapter");

    content = new ContentPanel();
    applyOrientationLayout();

    emptyHolder = new JPanel(new BorderLayout());
    emptyHolder.setOpaque(false);
    loadingHolder = new JPanel(new BorderLayout());
    loadingHolder.setOpaque(false);

    setOpaque(false);
    setFocusable(true);
    add(content, BorderLayout.CENTER);
    rebuildPadding();

    model.addListDataListener(modelListener);
    selectionModel.addSelectionListener(selectionBridge);
    modelReorderable = probeReorderable(model);
    rebuildVisibleItems();
    rebuildContent(false);

    installKeyboard();
  }

  // -------------------------------------------------------------- model API

  /**
   * Returns the backing model.
   *
   * @return the model; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaListModel<T> getModel() {
    return model;
  }

  /**
   * Replaces the backing model. The list unsubscribes from the outgoing model, re-probes the
   * incoming one for reorder support, and rebuilds. Selection is left alone — items still present
   * stay selected by identity.
   *
   * @param model the new model
   * @return this list, for fluent chaining
   * @throws NullPointerException if {@code model} is null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setModel(final ElwhaListModel<T> model) {
    Objects.requireNonNull(model, "model");
    if (model == this.model) {
      return this;
    }
    this.model.removeListDataListener(modelListener);
    this.model = model;
    model.addListDataListener(modelListener);
    modelReorderable = probeReorderable(model);
    rebuildVisibleItems();
    ensureMandatorySelection();
    rebuildContent(animateChanges);
    return this;
  }

  /**
   * Returns the adapter that maps items to components.
   *
   * @return the adapter; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemAdapter<T> getAdapter() {
    return adapter;
  }

  /**
   * Replaces the adapter and rebuilds every rendered component.
   *
   * @param adapter the new adapter
   * @return this list, for fluent chaining
   * @throws NullPointerException if {@code adapter} is null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setAdapter(final ElwhaItemAdapter<T> adapter) {
    this.adapter = Objects.requireNonNull(adapter, "adapter");
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the component currently rendering the given item.
   *
   * @param item the item to look up
   * @return the component, or null if the item is filtered out, not in the model, or the list is
   *     showing its empty or loading state
   * @version v0.5.0
   * @since v0.5.0
   */
  public JComponent getComponentFor(final T item) {
    return viewByItem.get(item);
  }

  /**
   * Returns an unmodifiable snapshot of the items currently rendered, in render order: filter
   * applied, then the pin or anchor partition, then the comparator within each partition. The
   * snapshot does not track later changes.
   *
   * @return the visible items in render order; never null, possibly empty
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<T> getVisibleItems() {
    return List.copyOf(visibleItems);
  }

  // ------------------------------------------------------ ElwhaList contract

  @Override
  public ElwhaListOrientation getOrientation() {
    return orientation;
  }

  /**
   * Sets the layout orientation. All four {@link ElwhaListOrientation} values are supported.
   *
   * @param orientation the new orientation; null is ignored
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setOrientation(final ElwhaListOrientation orientation) {
    if (orientation == null || orientation == this.orientation) {
      return this;
    }
    this.orientation = orientation;
    applyOrientationLayout();
    rebuildContent(false);
    return this;
  }

  @Override
  public int getColumns() {
    return columns;
  }

  /**
   * Sets the column count used by {@link ElwhaListOrientation#GRID}. No effect on the other
   * orientations, though the value is retained for when GRID is selected.
   *
   * @param columns the column count, clamped to at least 1
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setColumns(final int columns) {
    final int clamped = Math.max(1, columns);
    if (clamped == this.columns) {
      return this;
    }
    this.columns = clamped;
    if (orientation == ElwhaListOrientation.GRID) {
      applyOrientationLayout();
      rebuildContent(false);
    }
    return this;
  }

  @Override
  public int getItemGap() {
    return itemGap;
  }

  /**
   * Sets the gap between rendered items, revalidating the layout.
   *
   * @param gap the gap in pixels, clamped to at least 0
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setItemGap(final int gap) {
    final int clamped = Math.max(0, gap);
    if (clamped == itemGap) {
      return this;
    }
    itemGap = clamped;
    applyOrientationLayout();
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the padding around the rendered list.
   *
   * @return a copy of the padding insets; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public Insets getListPadding() {
    return (Insets) listPadding.clone();
  }

  /**
   * Sets the padding around the rendered list, revalidating and repainting.
   *
   * @param insets the padding; null is treated as zero on every edge
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setListPadding(final Insets insets) {
    listPadding = insets == null ? new Insets(0, 0, 0, 0) : (Insets) insets.clone();
    rebuildPadding();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Replaces the empty-state placeholder shown when no item is visible.
   *
   * @param component the placeholder; null restores the built-in "No items to show" label
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setEmptyState(final JComponent component) {
    emptyState = component;
    if (visibleItems.isEmpty()) {
      rebuildContent(false);
    }
    return this;
  }

  /**
   * Returns whether the list is showing its loading state.
   *
   * @return true while loading
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isLoading() {
    return loading;
  }

  /**
   * Sets the loading flag. While true the list renders the loading component in place of its items.
   *
   * @param loading whether to show the loading state
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setLoading(final boolean loading) {
    if (loading == this.loading) {
      return this;
    }
    this.loading = loading;
    rebuildContent(false);
    return this;
  }

  /**
   * Replaces the component shown while loading.
   *
   * @param component the loading component; null restores the built-in "Loading…" label
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setLoadingComponent(final JComponent component) {
    loadingComponent = component;
    if (loading) {
      rebuildContent(false);
    }
    return this;
  }

  /**
   * Returns the active filter predicate.
   *
   * @return the filter, or null when unfiltered
   * @version v0.5.0
   * @since v0.5.0
   */
  public Predicate<T> getFilter() {
    return filter;
  }

  /**
   * Sets a filter predicate; items it rejects are not rendered. Selection is unaffected — a
   * selected item that filters out stays selected and returns selected.
   *
   * @param filter the predicate; null clears filtering
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setFilter(final Predicate<T> filter) {
    this.filter = filter;
    rebuildVisibleItems();
    ensureMandatorySelection();
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the active sort comparator.
   *
   * @return the comparator, or null when unsorted
   * @version v0.5.0
   * @since v0.5.0
   */
  public Comparator<T> getSortOrder() {
    return comparator;
  }

  /**
   * Sets a comparator that orders rendered items — within each partition when a pin or anchor
   * partition is active. Drag-reorder is suppressed while a comparator is installed, since a
   * dropped item would immediately sort back.
   *
   * @param comparator the comparator; null clears sorting and restores reorder
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public ElwhaItemList<T> setSortOrder(final Comparator<T> comparator) {
    this.comparator = comparator;
    rebuildVisibleItems();
    ensureMandatorySelection();
    rebuildContent(false);
    return this;
  }

  // -------------------------------------------------------------- selection

  /**
   * Returns the active selection mode.
   *
   * @return the selection mode; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public SelectionMode getSelectionMode() {
    return selectionMode;
  }

  /**
   * Sets the selection mode. Switching to {@link SelectionMode#NONE} clears the selection;
   * switching to {@link SelectionMode#SINGLE_MANDATORY} seeds the first visible item when the
   * selection is empty.
   *
   * @param mode the selection mode; null is ignored
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setSelectionMode(final SelectionMode mode) {
    if (mode == null || mode == selectionMode) {
      return this;
    }
    selectionMode = mode;
    if (mode == SelectionMode.NONE) {
      selectionModel.clearSelection();
    } else if (mode == SelectionMode.SINGLE_MANDATORY) {
      ensureMandatorySelection();
    }
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the selection model. Never null, even in {@link SelectionMode#NONE}.
   *
   * @return the selection model
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaSelectionModel<T> getSelectionModel() {
    return selectionModel;
  }

  /**
   * Replaces the selection model. Listeners registered through {@link
   * #addSelectionListener(ElwhaSelectionListener)} follow the list, not the model, so they survive
   * the swap; listeners registered directly on the outgoing model do not.
   *
   * @param model the new selection model
   * @return this list, for fluent chaining
   * @throws NullPointerException if {@code model} is null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setSelectionModel(final ElwhaSelectionModel<T> model) {
    Objects.requireNonNull(model, "model");
    if (model == selectionModel) {
      return this;
    }
    selectionModel.removeSelectionListener(selectionBridge);
    selectionModel = model;
    model.addSelectionListener(selectionBridge);
    ensureMandatorySelection();
    syncSelectionVisuals();
    repaint();
    return this;
  }

  /**
   * Registers a listener notified after every selection change, from user gestures and programmatic
   * writes alike. Registering here rather than on the selection model keeps the listener attached
   * across a {@link #setSelectionModel(ElwhaSelectionModel)} swap.
   *
   * @param listener the listener; null and duplicates are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  public void addSelectionListener(final ElwhaSelectionListener<T> listener) {
    if (listener != null && !selectionListeners.contains(listener)) {
      selectionListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered selection listener.
   *
   * @param listener the listener to remove; unknown listeners are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  public void removeSelectionListener(final ElwhaSelectionListener<T> listener) {
    selectionListeners.remove(listener);
  }

  // ------------------------------------------------------- movement + pin

  /**
   * Returns the active movement mode.
   *
   * @return the movement mode; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public MovementMode getMovementMode() {
    return movementMode;
  }

  /**
   * Sets the movement mode, rebuilding so the drag affordance, partition, and injected menu entries
   * update immediately. Because {@link MovementMode#PINNED} and {@link MovementMode#ANCHORED} are
   * mutually exclusive, entering one clears the other's predicate and action, logged at {@code
   * FINE}.
   *
   * @param mode the movement mode; null is ignored
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setMovementMode(final MovementMode mode) {
    if (mode == null || mode == movementMode) {
      return this;
    }
    movementMode = mode;
    if (mode == MovementMode.ANCHORED && (pinPredicate != null || pinAction != null)) {
      LOG.fine("ElwhaItemList: switching to ANCHORED clears the pin predicate and action");
      pinPredicate = null;
      pinAction = null;
    } else if (mode == MovementMode.PINNED && (anchorPredicate != null || anchorAction != null)) {
      LOG.fine("ElwhaItemList: switching to PINNED clears the anchor predicate and action");
      anchorPredicate = null;
      anchorAction = null;
    }
    rebuildVisibleItems();
    rebuildContent(false);
    return this;
  }

  /**
   * Back-compatible shorthand over {@link #setMovementMode(MovementMode)}: {@code true} enters
   * {@link MovementMode#MOVABLE} unless a partitioned mode is already active (flipping out of it
   * would silently drop the partition), {@code false} enters {@link MovementMode#STATIC}.
   *
   * @param reorderable whether drag-reorder is enabled
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setReorderable(final boolean reorderable) {
    if (!reorderable) {
      return setMovementMode(MovementMode.STATIC);
    }
    if (movementMode == MovementMode.STATIC) {
      return setMovementMode(MovementMode.MOVABLE);
    }
    return this;
  }

  /**
   * Returns whether reorder is enabled — equivalent to {@code getMovementMode() !=
   * MovementMode.STATIC}. Note this reports the configured mode, not whether a drag would currently
   * succeed: an active sort comparator or a model that refuses {@code move} suppresses reorder
   * without changing the mode.
   *
   * @return true when the movement mode permits reorder
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isReorderable() {
    return movementMode != MovementMode.STATIC;
  }

  /**
   * Returns how draggability is advertised.
   *
   * @return the reorder affordance; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public ReorderAffordance getReorderAffordance() {
    return reorderAffordance;
  }

  /**
   * Sets how draggability is advertised — cursor swap, hover-revealed drag handle, both, or
   * nothing. Only effective when the movement mode permits reorder.
   *
   * @param affordance the affordance; null is ignored
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setReorderAffordance(final ReorderAffordance affordance) {
    if (affordance == null || affordance == reorderAffordance) {
      return this;
    }
    reorderAffordance = affordance;
    handleHoverItem = null;
    handleAlpha = 0f;
    rebuildContent(false);
    return this;
  }

  /**
   * Installs the predicate that reports which items are pinned. Pinned items render before unpinned
   * ones regardless of model order or comparator, and drags clamp to their own partition.
   *
   * <p>The list does not own pin state — the caller does, and this predicate reads it. The
   * predicate is consulted during rebuilds, not polled, so call {@link #pinStateChanged()} after
   * changing the state behind it.
   *
   * <p>Installing a non-null predicate implicitly enters {@link MovementMode#PINNED}, since the
   * partition means nothing otherwise; the flip is logged at {@code FINE}.
   *
   * @param isPinned the predicate; null clears partitioning and restores flat render order
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setPinPredicate(final Predicate<T> isPinned) {
    pinPredicate = isPinned;
    if (isPinned != null && movementMode != MovementMode.PINNED) {
      LOG.fine("ElwhaItemList: pin predicate installed; entering PINNED");
      return setMovementMode(MovementMode.PINNED);
    }
    rebuildVisibleItems();
    rebuildContent(false);
    return this;
  }

  /**
   * Installs the action {@link #togglePin(Object)} invokes, called with the item and its
   * <em>new</em> pin state. The action must persist the change and then call {@link
   * #pinStateChanged()} so the list rebuilds.
   *
   * @param action the action; null disables {@code togglePin}
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setPinAction(final BiConsumer<T, Boolean> action) {
    pinAction = action;
    return this;
  }

  /**
   * Returns whether the item is pinned — true only when the list is in {@link MovementMode#PINNED}
   * and the installed predicate says so. The predicate is inert in every other mode.
   *
   * @param item the item to test
   * @return whether the item is currently pinned
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isPinned(final T item) {
    return movementMode == MovementMode.PINNED && pinPredicate != null && pinPredicate.test(item);
  }

  /**
   * Flips an item's pin state through the configured pin action. A no-op outside {@link
   * MovementMode#PINNED}, with no action installed, or for a null item.
   *
   * @param item the item to toggle
   * @version v0.5.0
   * @since v0.5.0
   */
  public void togglePin(final T item) {
    if (movementMode != MovementMode.PINNED || pinAction == null || item == null) {
      return;
    }
    pinAction.accept(item, !isPinned(item));
  }

  /**
   * Signals that the pin predicate's answers changed without the model changing, triggering a
   * rebuild so the partition catches up. Cheaper than firing a synthetic model event.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void pinStateChanged() {
    rebuildVisibleItems();
    rebuildContent(false);
  }

  /**
   * Builds a Pin / Unpin menu item bound to the given item, labelled for its state at the moment of
   * the call — rebuild the menu on each show to keep the label fresh. For callers composing their
   * own context menu; auto-injection only fires when the component carries no caller menu.
   *
   * @param item the item the entry toggles
   * @return a fresh menu item; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public JMenuItem createPinMenuItem(final T item) {
    final JMenuItem menuItem = new JMenuItem(isPinned(item) ? "Unpin" : "Pin");
    menuItem.addActionListener(event -> togglePin(item));
    return menuItem;
  }

  // ----------------------------------------------------------------- anchor

  /**
   * Installs the predicate identifying the anchored item, which renders in the leading slot and
   * cannot be dragged. At most one item should report true; if several do, the first in model order
   * wins.
   *
   * <p>Installing a non-null predicate implicitly enters {@link MovementMode#ANCHORED}, which — the
   * two modes being mutually exclusive — also clears any pin predicate and action.
   *
   * @param isAnchored the predicate; null clears the anchor partition
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setAnchorPredicate(final Predicate<T> isAnchored) {
    anchorPredicate = isAnchored;
    if (isAnchored != null && movementMode != MovementMode.ANCHORED) {
      LOG.fine("ElwhaItemList: anchor predicate installed; entering ANCHORED");
      return setMovementMode(MovementMode.ANCHORED);
    }
    rebuildVisibleItems();
    rebuildContent(false);
    return this;
  }

  /**
   * Installs the action {@link #setAnchor(Object)} and {@link #clearAnchor()} invoke, called with
   * the item to anchor or null to clear. The action must persist the change and then call {@link
   * #anchorStateChanged()}.
   *
   * @param action the action; null disables anchor mutation
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setAnchorAction(final Consumer<T> action) {
    anchorAction = action;
    return this;
  }

  /**
   * Returns whether the item is anchored — true only when the list is in {@link
   * MovementMode#ANCHORED} and the installed predicate says so.
   *
   * @param item the item to test
   * @return whether the item is currently anchored
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isAnchored(final T item) {
    return movementMode == MovementMode.ANCHORED
        && anchorPredicate != null
        && anchorPredicate.test(item);
  }

  /**
   * Returns the first model item the anchor predicate reports as anchored.
   *
   * @return the anchored item, or null when nothing is anchored or the list is not in {@link
   *     MovementMode#ANCHORED}
   * @version v0.5.0
   * @since v0.5.0
   */
  public T getAnchoredItem() {
    if (movementMode != MovementMode.ANCHORED || anchorPredicate == null) {
      return null;
    }
    for (final T item : model) {
      if (anchorPredicate.test(item)) {
        return item;
      }
    }
    return null;
  }

  /**
   * Makes the given item the sole anchor through the configured anchor action. A no-op outside
   * {@link MovementMode#ANCHORED} or with no action installed.
   *
   * @param item the item to anchor, or null to clear the anchor
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setAnchor(final T item) {
    if (movementMode != MovementMode.ANCHORED || anchorAction == null) {
      return;
    }
    anchorAction.accept(item);
  }

  /**
   * Clears the anchor — equivalent to {@code setAnchor(null)}.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void clearAnchor() {
    setAnchor(null);
  }

  /**
   * Signals that the anchor predicate's answers changed without a model event, triggering a
   * rebuild.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void anchorStateChanged() {
    rebuildVisibleItems();
    rebuildContent(false);
  }

  /**
   * Builds a Set as anchor / Remove anchor menu item bound to the given item, labelled for its
   * state at the moment of the call.
   *
   * @param item the item the entry anchors or releases
   * @return a fresh menu item; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public JMenuItem createAnchorMenuItem(final T item) {
    final JMenuItem menuItem = new JMenuItem(isAnchored(item) ? "Remove anchor" : "Set as anchor");
    menuItem.addActionListener(
        event -> {
          if (isAnchored(item)) {
            clearAnchor();
          } else {
            setAnchor(item);
          }
        });
    return menuItem;
  }

  /**
   * Builds the Move up / Move down / Delete entries for the given item, each disabled when it would
   * not apply. For callers composing their own context menu — the list injects the same entries
   * itself only when the item's component carries no caller-attached menu.
   *
   * @param item the item the entries act on
   * @return fresh menu items in display order; empty when reorder is unavailable
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<JMenuItem> createReorderMenuItems(final T item) {
    if (!canReorderAnything()) {
      return List.of();
    }
    final int index = visibleItems.indexOf(item);
    final JMenuItem up = new JMenuItem("Move up");
    up.setEnabled(index > 0);
    up.addActionListener(event -> moveFocusedItem(item, -1));
    final JMenuItem down = new JMenuItem("Move down");
    down.setEnabled(index >= 0 && index < visibleItems.size() - 1);
    down.addActionListener(event -> moveFocusedItem(item, 1));
    final JMenuItem delete = new JMenuItem("Delete");
    delete.addActionListener(event -> deleteItem(item));
    return List.of(up, down, delete);
  }

  // ------------------------------------------------------------ affordances

  /**
   * Sets the pin affordance treatment. Only effective in {@link MovementMode#PINNED}.
   *
   * @param affordance the treatment; null is ignored
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setPinAffordance(final IconAffordance affordance) {
    if (affordance == null || affordance == pinAffordance) {
      return this;
    }
    pinAffordance = affordance;
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the pin affordance treatment.
   *
   * @return the pin affordance; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public IconAffordance getPinAffordance() {
    return pinAffordance;
  }

  /**
   * Sets the anchor affordance treatment. Only effective in {@link MovementMode#ANCHORED}.
   *
   * @param affordance the treatment; null is ignored
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setAnchorAffordance(final IconAffordance affordance) {
    if (affordance == null || affordance == anchorAffordance) {
      return this;
    }
    anchorAffordance = affordance;
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the anchor affordance treatment.
   *
   * @return the anchor affordance; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public IconAffordance getAnchorAffordance() {
    return anchorAffordance;
  }

  // -------------------------------------------------------------- animation

  /**
   * Returns whether add and remove changes fade in.
   *
   * @return true when change animation is enabled
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isAnimateChanges() {
    return animateChanges;
  }

  /**
   * Toggles the fade-in applied when a model change rebuilds the rendered items. Off by default,
   * and suppressed entirely under {@link MorphAnimator#isReducedMotion()}.
   *
   * @param animate whether model changes fade in
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setAnimateChanges(final boolean animate) {
    animateChanges = animate;
    if (!animate) {
      stopFade();
    }
    return this;
  }

  /**
   * Returns the change-fade duration.
   *
   * @return the duration in milliseconds
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getAnimationDuration() {
    return animationDurationMs;
  }

  /**
   * Sets the change-fade duration.
   *
   * @param ms the duration in milliseconds, clamped to at least 50
   * @return this list, for fluent chaining
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaItemList<T> setAnimationDuration(final int ms) {
    animationDurationMs = Math.max(MIN_ANIMATION_MS, ms);
    return this;
  }

  // ------------------------------------------------------------ reorder API

  /**
   * Registers a listener notified after a user-driven reorder commits to the model. Programmatic
   * {@code model.move(…)} calls do not fire it.
   *
   * @param listener the listener; null and duplicates are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  public void addReorderListener(final ElwhaReorderListener<T> listener) {
    if (listener != null && !reorderListeners.contains(listener)) {
      reorderListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered reorder listener.
   *
   * @param listener the listener to remove; unknown listeners are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  public void removeReorderListener(final ElwhaReorderListener<T> listener) {
    reorderListeners.remove(listener);
  }

  /**
   * Fires a reorder event to every registered listener.
   *
   * @param item the moved item
   * @param from the source index in the model
   * @param to the destination index in the model
   * @version v0.5.0
   * @since v0.5.0
   */
  protected void fireReorder(final T item, final int from, final int to) {
    final ElwhaReorderEvent<T> event = new ElwhaReorderEvent<>(this, item, from, to);
    for (final ElwhaReorderListener<T> listener : new ArrayList<>(reorderListeners)) {
      listener.itemReordered(event);
    }
  }

  // --------------------------------------------------------------- internals

  private static <T> boolean probeReorderable(final ElwhaListModel<T> model) {
    try {
      // A no-op move: every mutable implementation returns early, and a read-only one throws
      // before it can matter. Probing once here keeps the check off the drag path, where a
      // refusal mid-gesture would leave the drag half-committed.
      model.move(0, 0);
      return true;
    } catch (final UnsupportedOperationException refused) {
      LOG.fine("ElwhaItemList: model refuses move(); reorder disabled for this model");
      return false;
    } catch (final IndexOutOfBoundsException empty) {
      // A model that range-checks before short-circuiting the no-op rejects the indices, not the
      // operation — that is not a refusal to reorder.
      return true;
    }
  }

  private boolean isLeftToRight() {
    return getComponentOrientation().isLeftToRight();
  }

  /**
   * Converts an x coordinate between physical and leading-edge-relative space. The mapping is its
   * own inverse, so one method serves both directions: layout code computes positions as if the
   * list were left-to-right and flips them on the way out, and drag code flips pointer positions on
   * the way in.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private int flipX(final Container parent, final int x, final int width) {
    if (isLeftToRight()) {
      return x;
    }
    final Insets in = parent.getInsets();
    return in.left + (parent.getWidth() - in.right) - x - width;
  }

  private void applyOrientationLayout() {
    content.removeAll();
    switch (orientation) {
      case HORIZONTAL -> content.setLayout(new HorizontalLayout());
      case WRAP -> content.setLayout(new WrapLayout());
      case GRID -> content.setLayout(new GridLayoutImpl());
      case VERTICAL -> content.setLayout(new VerticalLayout());
      default -> content.setLayout(new VerticalLayout());
    }
  }

  private void rebuildPadding() {
    setBorder(
        BorderFactory.createEmptyBorder(
            listPadding.top, listPadding.left, listPadding.bottom, listPadding.right));
  }

  private void onModelChanged(final ElwhaListDataEvent<T> event) {
    rebuildVisibleItems();
    if (event.getType() == ElwhaListDataEvent.Type.REMOVED
        || event.getType() == ElwhaListDataEvent.Type.STRUCTURE) {
      pruneSelectionToModel();
    }
    // Mandatory mode has to re-assert "never empty" after a removal emptied the selection. Runs
    // after the visible rebuild so the replacement lands on an item that is actually showing.
    ensureMandatorySelection();
    rebuildContent(animateChanges);
  }

  /**
   * Drops selection entries whose items have left the model.
   *
   * <p>Only removals and wholesale replacements prune. Filtering and sorting deliberately do not: a
   * filtered-out item is hidden, not gone, and the family contract is that it comes back still
   * selected. An item deleted from the model has no such future.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private void pruneSelectionToModel() {
    final List<T> selected = selectionModel.getSelected();
    final List<T> retained = new ArrayList<>();
    for (final T item : selected) {
      if (model.contains(item)) {
        retained.add(item);
      }
    }
    if (retained.size() != selected.size()) {
      selectionModel.setSelected(retained);
    }
  }

  private void onSelectionChanged(final ElwhaSelectionEvent<T> event) {
    syncSelectionVisuals();
    for (final ElwhaSelectionListener<T> listener : new ArrayList<>(selectionListeners)) {
      listener.selectionChanged(event);
    }
  }

  private void ensureMandatorySelection() {
    if (selectionMode != SelectionMode.SINGLE_MANDATORY) {
      return;
    }
    if (selectionModel.getSelected().isEmpty() && !visibleItems.isEmpty()) {
      final T first = visibleItems.get(0);
      selectionModel.setSelected(List.of(first));
      selectionAnchor = first;
    }
  }

  private void rebuildVisibleItems() {
    visibleItems.clear();
    final boolean pinPartition = movementMode == MovementMode.PINNED && pinPredicate != null;
    final boolean anchorPartition =
        movementMode == MovementMode.ANCHORED && anchorPredicate != null;

    if (!pinPartition && !anchorPartition) {
      for (final T item : model) {
        if (filter == null || filter.test(item)) {
          visibleItems.add(item);
        }
      }
      if (comparator != null) {
        visibleItems.sort(comparator);
      }
      return;
    }

    if (anchorPartition) {
      T anchor = null;
      final List<T> rest = new ArrayList<>();
      for (final T item : model) {
        if (filter != null && !filter.test(item)) {
          continue;
        }
        if (anchor == null && anchorPredicate.test(item)) {
          anchor = item;
        } else {
          rest.add(item);
        }
      }
      if (comparator != null) {
        rest.sort(comparator);
      }
      if (anchor != null) {
        visibleItems.add(anchor);
      }
      visibleItems.addAll(rest);
      return;
    }

    // Pinned partition. A comparator applies within each partition independently so pinned items
    // keep their group identity no matter what the sort key says.
    final List<T> pinned = new ArrayList<>();
    final List<T> unpinned = new ArrayList<>();
    for (final T item : model) {
      if (filter != null && !filter.test(item)) {
        continue;
      }
      if (pinPredicate.test(item)) {
        pinned.add(item);
      } else {
        unpinned.add(item);
      }
    }
    if (comparator != null) {
      pinned.sort(comparator);
      unpinned.sort(comparator);
    }
    visibleItems.addAll(pinned);
    visibleItems.addAll(unpinned);
  }

  private void rebuildContent(final boolean animate) {
    removeAll();
    handleHoverItem = null;
    handleAlpha = 0f;

    if (loading) {
      loadingHolder.removeAll();
      loadingHolder.add(
          loadingComponent != null ? loadingComponent : defaultLoading(), BorderLayout.CENTER);
      add(loadingHolder, BorderLayout.CENTER);
      viewByItem.clear();
      revalidate();
      repaint();
      return;
    }
    if (visibleItems.isEmpty()) {
      emptyHolder.removeAll();
      emptyHolder.add(emptyState != null ? emptyState : defaultEmptyState(), BorderLayout.CENTER);
      add(emptyHolder, BorderLayout.CENTER);
      viewByItem.clear();
      revalidate();
      repaint();
      return;
    }

    viewByItem.clear();
    content.removeAll();
    applyOrientationLayout();

    for (int i = 0; i < visibleItems.size(); i++) {
      final T item = visibleItems.get(i);
      final JComponent view = adapter.componentFor(item, i);
      if (view == null) {
        continue;
      }
      viewByItem.put(item, view);
      configureView(view, item, i);
      content.add(view);
    }
    add(content, BorderLayout.CENTER);

    if (focusedVisibleIndex >= visibleItems.size()) {
      focusedVisibleIndex = visibleItems.size() - 1;
    }

    revalidate();
    repaint();
    if (animate && !MorphAnimator.isReducedMotion()) {
      startFadeIn();
    } else {
      stopFade();
    }
  }

  private void configureView(final JComponent view, final T item, final int index) {
    final ElwhaListItemView itemView = view instanceof ElwhaListItemView v ? v : null;
    if (itemView != null && selectionMode != SelectionMode.NONE) {
      // The view responds to clicks but must not toggle its own selected state — this list owns
      // selection and writes it back through setSelected.
      itemView.setListInteractive(true);
      itemView.setSelected(selectionModel.isSelected(item));
    }

    if (movementMode == MovementMode.PINNED) {
      applyPinAffordance(view, item);
    } else if (movementMode == MovementMode.ANCHORED) {
      applyAnchorAffordance(view, item);
    }

    final boolean canDragThis = canReorderAnything() && !isAnchored(item);
    if (canDragThis && usesCursorSwap()) {
      // The open-hand cursor on hover advertises the drag, overriding whatever pointer the view's
      // own interactivity installed.
      view.setCursor(ReorderCursors.grab());
    }

    installViewListeners(view, item, index, canDragThis);
  }

  private void installViewListeners(
      final JComponent view, final T item, final int index, final boolean canDragThis) {
    final MouseInputAdapter handler =
        new MouseInputAdapter() {
          // Selection commits on release-without-drag rather than on press, matching Finder and
          // Explorer: a click selects, a drag moves. Committing on press made every drag select
          // the item it was only meant to rearrange.
          private boolean pendingClick;
          private int pendingModifiers;

          @Override
          public void mousePressed(final MouseEvent event) {
            if (maybeShowContextMenu(event, view, item)) {
              return;
            }
            if (event.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            requestFocusInWindow();
            focusedVisibleIndex = index;
            pendingClick = true;
            pendingModifiers = event.getModifiersEx();
            if (canDragThis) {
              initiateDrag(item, view, event);
            }
          }

          @Override
          public void mouseDragged(final MouseEvent event) {
            if (drag == null || drag.item != item) {
              return;
            }
            if (!drag.active && hasMovedPastThreshold(event)) {
              activateDrag();
              pendingClick = false;
            }
            if (drag.active) {
              continueDrag(event);
            }
          }

          @Override
          public void mouseReleased(final MouseEvent event) {
            if (drag != null && drag.item == item) {
              if (drag.active) {
                // The release event carries the only authoritative final pointer position: Swing
                // and the OS coalesce motion events, so on a throw the last mouseDragged lags
                // behind where the user actually let go. Recompute the slot from it before
                // committing.
                refreshDropFromReleaseEvent(event);
                endDrag();
              } else {
                drag = null;
              }
            }
            if (maybeShowContextMenu(event, view, item)) {
              pendingClick = false;
              return;
            }
            if (pendingClick) {
              pendingClick = false;
              handleSelectionPress(item, index, pendingModifiers);
            }
          }

          @Override
          public void mouseEntered(final MouseEvent event) {
            if (canDragThis && usesHoverIcon()) {
              showDragHandle(item);
            }
          }

          @Override
          public void mouseExited(final MouseEvent event) {
            if (usesHoverIcon() && handleHoverItem == item && (drag == null || !drag.active)) {
              hideDragHandle();
            }
          }
        };

    if (view instanceof ElwhaChip chip) {
      // The chip's inner content row runs its own listener that would otherwise swallow these
      // events before they reach the body.
      chip.addChipMouseListener(handler);
      chip.addChipMouseMotionListener(handler);
    } else {
      view.addMouseListener(handler);
      view.addMouseMotionListener(handler);
    }
  }

  private boolean canReorderAnything() {
    if (movementMode == MovementMode.STATIC || !modelReorderable) {
      return false;
    }
    if (comparator != null) {
      if (!sortBlocksReorderLogged) {
        LOG.warning("ElwhaItemList: reorder is suppressed while a sort order is active");
        sortBlocksReorderLogged = true;
      }
      return false;
    }
    return true;
  }

  private boolean usesCursorSwap() {
    return reorderAffordance == ReorderAffordance.CURSOR_SWAP
        || reorderAffordance == ReorderAffordance.BOTH;
  }

  private boolean usesHoverIcon() {
    return reorderAffordance == ReorderAffordance.HOVER_ICON
        || reorderAffordance == ReorderAffordance.BOTH;
  }

  private void applyPinAffordance(final JComponent view, final T item) {
    if (!(view instanceof ElwhaChip chip)) {
      return;
    }
    switch (pinAffordance) {
      case NONE -> {
        // The adapter's own leading icon stands.
      }
      case INDICATOR -> {
        if (isPinned(item)) {
          // Filled means active in every affordance mode; an outline-when-indicator convention
          // would make the same glyph mean different things depending on an unrelated setting.
          chip.setLeadingIcon(MaterialIcons.pushPinFilled(CHIP_ICON_SIZE));
        }
      }
      case BUTTON ->
          chip.setLeadingAffordance(
              MaterialIcons.pushPin(CHIP_ICON_SIZE),
              MaterialIcons.pushPinFilled(CHIP_ICON_SIZE),
              isPinned(item),
              false,
              isPinned(item) ? "Unpin" : "Pin",
              () -> togglePin(item));
      default -> {
        // exhaustive
      }
    }
  }

  private void applyAnchorAffordance(final JComponent view, final T item) {
    if (!(view instanceof ElwhaChip chip)) {
      return;
    }
    switch (anchorAffordance) {
      case NONE -> {
        // The adapter's own leading icon stands.
      }
      case INDICATOR -> {
        if (isAnchored(item)) {
          chip.setLeadingIcon(MaterialIcons.anchorFilled(CHIP_ICON_SIZE));
        }
      }
      case BUTTON -> {
        final boolean anchored = isAnchored(item);
        chip.setLeadingAffordance(
            MaterialIcons.anchor(CHIP_ICON_SIZE),
            MaterialIcons.anchorFilled(CHIP_ICON_SIZE),
            anchored,
            !anchored,
            anchored ? "Remove anchor" : "Set as anchor",
            () -> {
              if (isAnchored(item)) {
                clearAnchor();
              } else {
                setAnchor(item);
              }
            });
      }
      default -> {
        // exhaustive
      }
    }
  }

  // ------------------------------------------------------------ context menu

  private boolean maybeShowContextMenu(
      final MouseEvent event, final JComponent view, final T item) {
    if (!event.isPopupTrigger() || hasCallerMenu(view)) {
      return false;
    }
    final JPopupMenu menu = new JPopupMenu();
    for (final JMenuItem entry : createReorderMenuItems(item)) {
      menu.add(entry);
    }
    final boolean pinEntry =
        movementMode == MovementMode.PINNED && pinPredicate != null && pinAction != null;
    final boolean anchorEntry =
        movementMode == MovementMode.ANCHORED && anchorPredicate != null && anchorAction != null;
    if (pinEntry || anchorEntry) {
      if (menu.getComponentCount() > 0) {
        menu.addSeparator();
      }
      menu.add(pinEntry ? createPinMenuItem(item) : createAnchorMenuItem(item));
    }
    if (menu.getComponentCount() == 0) {
      return false;
    }
    // Below the item rather than over it, so the row the menu acts on stays readable.
    menu.show(view, 0, view.getHeight());
    return true;
  }

  private boolean hasCallerMenu(final JComponent view) {
    if (view.getComponentPopupMenu() != null) {
      return true;
    }
    return view instanceof ElwhaChip chip && chip.hasContextMenu();
  }

  // -------------------------------------------------------------------- drag

  private void initiateDrag(final T item, final JComponent view, final MouseEvent event) {
    drag = new DragState();
    drag.item = item;
    drag.view = view;
    drag.fromIndex = visibleItems.indexOf(item);
    drag.toIndex = drag.fromIndex;
    // The handler is installed on the view and, for chips, on its inner content row, so
    // event.getComponent() may be either. convertPoint reads the point in the source's own
    // coordinate space — passing the view when the event came from a child gives wrong absolutes.
    drag.startPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    drag.currentPoint = drag.startPoint;
    drag.grabOffset = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), view);
    drag.active = false;
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

  private void activateDrag() {
    if (drag == null || drag.active) {
      return;
    }
    drag.active = true;
    if (drag.view instanceof ElwhaListItemView itemView) {
      itemView.cancelPendingClick();
      itemView.setDragged(true);
    }

    displacedX.clear();
    displacedY.clear();
    for (final JComponent view : viewByItem.values()) {
      displacedX.put(view, view.getX());
      displacedY.put(view, view.getY());
    }
    recomputeDragTargets();
    content.setComponentZOrder(drag.view, 0);
    if (usesCursorSwap()) {
      drag.view.setCursor(ReorderCursors.grabbing());
    }
    if (usesHoverIcon()) {
      showDragHandle(drag.item);
    }
    startDragAnimation();
    content.revalidate();
    content.repaint();
  }

  private void refreshDropFromReleaseEvent(final MouseEvent event) {
    if (drag == null) {
      return;
    }
    final Point releasePoint =
        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    // On a careful drop the release lands within the threshold of the last sampled drag position,
    // and the slot already showing under the pointer is what the user chose — recomputing there
    // could snap across a cell boundary and surprise them. On a throw the pointer has moved
    // further, and the recompute recovers the position event coalescing dropped.
    final int dx = releasePoint.x - drag.currentPoint.x;
    final int dy = releasePoint.y - drag.currentPoint.y;
    if (dx * dx + dy * dy <= DRAG_THRESHOLD * DRAG_THRESHOLD) {
      return;
    }
    drag.currentPoint = releasePoint;
    drag.toIndex = clampSlotToPartition(computeDropIndex(computeDropAnchor()), drag.item);
  }

  private void continueDrag(final MouseEvent event) {
    if (drag == null) {
      return;
    }
    drag.currentPoint =
        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    // Clamping during the drag, not only at the drop, keeps the feedback honest: without it a
    // pinned item drags over unpinned neighbours, shuffling them, only to snap back on release.
    final int newDrop = clampSlotToPartition(computeDropIndex(computeDropAnchor()), drag.item);
    if (newDrop != drag.toIndex) {
      drag.toIndex = newDrop;
      recomputeDragTargets();
    }
    content.revalidate();
    content.repaint();
  }

  /**
   * Computes the point the drop slot is measured from, in leading-edge-relative coordinates.
   *
   * <ul>
   *   <li>VERTICAL and HORIZONTAL use the dragged item's leading corner, so dragging a wide item
   *       over narrow neighbours swaps as soon as that edge crosses a midline rather than waiting
   *       for the pointer somewhere inside it.
   *   <li>GRID uses the dragged item's center, which is what a uniform two-dimensional cell grid
   *       wants.
   *   <li>WRAP uses the bare pointer. Leading-edge anchoring breaks here because the anchor's y
   *       depends on the grab offset, which lands above row zero whenever the user grabs near an
   *       item's bottom; the pointer is always in the user's own row and column.
   * </ul>
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private Point computeDropAnchor() {
    final int width = drag.view.getWidth();
    final int physicalLeftX = drag.currentPoint.x - drag.grabOffset.x;
    final int topY = drag.currentPoint.y - drag.grabOffset.y;
    return switch (orientation) {
      case GRID ->
          new Point(flipX(content, physicalLeftX + width / 2, 0), topY + drag.view.getHeight() / 2);
      case WRAP -> new Point(flipX(content, drag.currentPoint.x, 0), drag.currentPoint.y);
      case HORIZONTAL, VERTICAL -> new Point(flipX(content, physicalLeftX, width), topY);
    };
  }

  /**
   * Maps a leading-edge-relative anchor to a drop slot in {@code [0, visibleCount]}, where the
   * upper bound means end-of-list.
   *
   * <p>Every strategy walks the items' <strong>natural</strong> preferred-size positions instead of
   * reading their current bounds. Current bounds are mid-slide during the gap animation, and
   * feeding those back into the slot calculation oscillates: a small item covered by a larger
   * dragged one is partway through its slide, the calculation sees the in-progress position,
   * retargets, the slide accelerates, and the answer flips back and forth. Natural positions depend
   * on nothing but the pointer.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private int computeDropIndex(final Point point) {
    return switch (orientation) {
      case VERTICAL -> dropIndexVertical(point);
      case HORIZONTAL -> dropIndexHorizontal(point);
      case WRAP -> dropIndexWrap(point);
      case GRID -> dropIndexGrid(point);
    };
  }

  private int dropIndexVertical(final Point point) {
    final Insets in = content.getInsets();
    int slot = 0;
    int y = in.top;
    for (final T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final JComponent view = viewByItem.get(item);
      if (view == null) {
        continue;
      }
      final int height = view.getPreferredSize().height;
      if (point.y > y + height / 2) {
        slot++;
      }
      y += height + itemGap;
    }
    return slot;
  }

  private int dropIndexHorizontal(final Point point) {
    final Insets in = content.getInsets();
    int slot = 0;
    int x = in.left;
    for (final T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final JComponent view = viewByItem.get(item);
      if (view == null) {
        continue;
      }
      final int width = view.getPreferredSize().width;
      if (point.x > x + width / 2) {
        slot++;
      }
      x += width + itemGap;
    }
    return slot;
  }

  /**
   * Recreates the natural row-break layout for the non-dragged sequence and advances the slot for
   * every center the pointer has passed in reading order.
   *
   * <p>The anchor's y is clamped to the top inset so a pointer that drifts above the content does
   * not fail both per-item tests — without the clamp neither the within-row nor the below-row
   * branch fires for any item and the slot sticks at zero regardless of x. Drift below the content
   * already works, since the below-row branch then fires for every row.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private int dropIndexWrap(final Point point) {
    final Insets in = content.getInsets();
    final int maxRight = Math.max(in.left, content.getWidth() - in.right);
    final int anchorY = Math.max(in.top, point.y);
    int slot = 0;
    int x = in.left;
    int y = in.top;
    int rowHeight = 0;
    for (final T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final JComponent view = viewByItem.get(item);
      if (view == null) {
        continue;
      }
      final Dimension pref = view.getPreferredSize();
      if (x > in.left && x + pref.width > maxRight) {
        x = in.left;
        y += rowHeight + itemGap;
        rowHeight = 0;
      }
      final int rowBottom = y + Math.max(pref.height, rowHeight);
      if (anchorY > rowBottom) {
        slot++;
      } else if (anchorY >= y && point.x > x + pref.width / 2) {
        slot++;
      }
      x += pref.width + itemGap;
      rowHeight = Math.max(rowHeight, pref.height);
    }
    return slot;
  }

  /**
   * Maps the anchor to the grid cell it sits in.
   *
   * <p>Cell dimensions come from the rendered bounds of the non-dragged items, which the layout
   * stretches uniformly, rather than from the container width divided by the column count. The
   * container-derived formula occasionally produced an inflated cell width — a stale layout, or the
   * dragged item's own width — and pushed every anchor into column zero, which surfaced as random
   * "jumped to the front" drops.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private int dropIndexGrid(final Point point) {
    final Insets in = content.getInsets();
    int cellWidth = 0;
    int cellHeight = 0;
    int nonDraggedCount = 0;
    for (final T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final JComponent view = viewByItem.get(item);
      if (view == null) {
        continue;
      }
      cellWidth = Math.max(cellWidth, Math.max(view.getWidth(), view.getPreferredSize().width));
      cellHeight = Math.max(cellHeight, Math.max(view.getHeight(), view.getPreferredSize().height));
      nonDraggedCount++;
    }
    if (cellWidth <= 0) {
      cellWidth = 100;
    }
    if (cellHeight <= 0) {
      cellHeight = 24;
    }
    final int cols = Math.max(1, columns);
    final int maxRow = Math.max(0, (nonDraggedCount + cols - 1) / cols - 1);
    final int col = Math.max(0, Math.min(cols - 1, (point.x - in.left) / (cellWidth + itemGap)));
    final int row = Math.max(0, Math.min(maxRow, (point.y - in.top) / (cellHeight + itemGap)));
    return Math.max(0, Math.min(nonDraggedCount, row * cols + col));
  }

  /**
   * Recomputes every non-dragged item's target position for the order that results from lifting the
   * dragged item out and reinserting it at the current slot. The animation timer interpolates
   * toward these.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private void recomputeDragTargets() {
    targetX.clear();
    targetY.clear();
    final List<JComponent> ordered = new ArrayList<>();
    for (final T item : visibleItems) {
      final JComponent view = viewByItem.get(item);
      if (view != null) {
        ordered.add(view);
      }
    }
    ordered.remove(drag.view);
    ordered.add(Math.max(0, Math.min(ordered.size(), drag.toIndex)), drag.view);

    final Insets in = content.getInsets();
    switch (orientation) {
      case VERTICAL -> {
        int y = in.top;
        for (final JComponent view : ordered) {
          final Dimension pref = view.getPreferredSize();
          targetX.put(view, flipX(content, in.left, pref.width));
          targetY.put(view, y);
          y += pref.height + itemGap;
        }
      }
      case HORIZONTAL -> {
        int x = in.left;
        for (final JComponent view : ordered) {
          final Dimension pref = view.getPreferredSize();
          targetX.put(view, flipX(content, x, pref.width));
          targetY.put(view, in.top);
          x += pref.width + itemGap;
        }
      }
      case WRAP -> {
        final int maxRight = Math.max(in.left, content.getWidth() - in.right);
        int x = in.left;
        int y = in.top;
        int rowHeight = 0;
        for (final JComponent view : ordered) {
          final Dimension pref = view.getPreferredSize();
          if (x > in.left && x + pref.width > maxRight) {
            x = in.left;
            y += rowHeight + itemGap;
            rowHeight = 0;
          }
          targetX.put(view, flipX(content, x, pref.width));
          targetY.put(view, y);
          x += pref.width + itemGap;
          rowHeight = Math.max(rowHeight, pref.height);
        }
      }
      case GRID -> {
        final int available = Math.max(1, content.getWidth() - in.left - in.right);
        final int cellWidth = Math.max(1, (available - (columns - 1) * itemGap) / columns);
        int cellHeight = 0;
        for (final JComponent view : ordered) {
          cellHeight = Math.max(cellHeight, view.getPreferredSize().height);
        }
        if (cellHeight == 0) {
          cellHeight = 24;
        }
        for (int i = 0; i < ordered.size(); i++) {
          final JComponent view = ordered.get(i);
          targetX.put(
              view, flipX(content, in.left + (i % columns) * (cellWidth + itemGap), cellWidth));
          targetY.put(view, in.top + (i / columns) * (cellHeight + itemGap));
        }
      }
      default -> {
        // exhaustive
      }
    }
  }

  private void startDragAnimation() {
    if (dragAnimTimer != null && dragAnimTimer.isRunning()) {
      dragAnimTimer.stop();
    }
    if (MorphAnimator.isReducedMotion()) {
      snapDragTargets();
      return;
    }
    dragAnimTimer =
        new Timer(
            ANIMATION_TICK_MS,
            event -> {
              if (drag == null) {
                ((Timer) event.getSource()).stop();
                return;
              }
              boolean moved = false;
              for (final Map.Entry<JComponent, Integer> entry : targetY.entrySet()) {
                final JComponent view = entry.getKey();
                if (view == drag.view) {
                  continue;
                }
                if (animateAxis(view, entry.getValue(), displacedY, view.getY())) {
                  moved = true;
                }
                final Integer tx = targetX.get(view);
                if (tx != null && animateAxis(view, tx, displacedX, view.getX())) {
                  moved = true;
                }
              }
              if (moved) {
                content.revalidate();
                content.repaint();
              }
            });
    dragAnimTimer.start();
  }

  private void snapDragTargets() {
    for (final Map.Entry<JComponent, Integer> entry : targetY.entrySet()) {
      final JComponent view = entry.getKey();
      if (view == drag.view) {
        continue;
      }
      displacedY.put(view, entry.getValue());
      final Integer tx = targetX.get(view);
      if (tx != null) {
        displacedX.put(view, tx);
      }
    }
    content.revalidate();
    content.repaint();
  }

  private static boolean animateAxis(
      final JComponent view,
      final int target,
      final Map<JComponent, Integer> positions,
      final int fallback) {
    final Integer boxed = positions.get(view);
    final int current = boxed != null ? boxed : fallback;
    if (current == target) {
      return false;
    }
    final int diff = target - current;
    int step = (int) Math.round(diff * 0.30);
    if (step == 0 || Math.abs(diff) <= 1) {
      step = diff;
    }
    positions.put(view, current + step);
    return true;
  }

  private void endDrag() {
    if (dragAnimTimer != null) {
      dragAnimTimer.stop();
    }
    displacedX.clear();
    displacedY.clear();
    targetX.clear();
    targetY.clear();
    if (drag == null) {
      return;
    }
    if (drag.view instanceof ElwhaListItemView itemView) {
      itemView.setDragged(false);
    }
    // Restore to the hover-state cursor rather than the platform default: the pointer is still
    // over a draggable item, so the grab hand is what belongs there. A successful drop rebuilds
    // and sets it again, but a no-op drop reuses this very instance.
    if (usesCursorSwap()) {
      drag.view.setCursor(ReorderCursors.grab());
    }
    if (usesHoverIcon()) {
      hideDragHandle();
    }

    final int fromVisible = drag.fromIndex;
    final int toVisible = drag.toIndex;
    final T item = drag.item;
    drag = null;

    if (fromVisible < 0 || toVisible < 0) {
      restoreContentOrder();
      return;
    }
    final int fromModel = model.indexOf(item);
    // The slot is in post-removal visible-slot space, which diverges from model index space
    // whenever a filter, a comparator, or a partition is active. Translating through the
    // neighbour that will follow the dragged item is correct in every case, including the plain
    // one where the two spaces happen to coincide.
    final int nonDraggedCount = visibleItems.size() - 1;
    final int clamped =
        clampSlotToPartition(Math.max(0, Math.min(nonDraggedCount, toVisible)), item);
    final int toModel = translateVisibleSlotToModelIndex(clamped, item);

    if (fromModel < 0 || toModel < 0 || fromModel == toModel || toModel >= model.getSize()) {
      restoreContentOrder();
      return;
    }
    if (!commitMove(fromModel, toModel)) {
      restoreContentOrder();
      return;
    }
    fireReorder(item, fromModel, toModel);
  }

  /**
   * Applies a model move, absorbing a late refusal from a model whose no-op probe passed but whose
   * real mutation does not. Reorder never hard-fails.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private boolean commitMove(final int fromModel, final int toModel) {
    try {
      // The MOVED event rebuilds the content, which restores the child order the drag scrambled.
      model.move(fromModel, toModel);
      return true;
    } catch (final UnsupportedOperationException refused) {
      LOG.fine("ElwhaItemList: model refused move(); the drop is discarded");
      modelReorderable = false;
      return false;
    }
  }

  /**
   * Clamps a post-removal slot into the dragged item's partition: a pinned item can only land
   * inside the pinned run, an unpinned one only after it, and with an anchor present nothing may
   * take slot zero. A pass-through when no partition is active.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private int clampSlotToPartition(final int slot, final T draggedItem) {
    if (movementMode == MovementMode.ANCHORED && anchorPredicate != null) {
      // Anchored items never reach this path — they are drag-blocked upstream — so this only
      // clamps the others out of the leading slot the anchor occupies.
      if (getAnchoredItem() == null) {
        return slot;
      }
      return Math.max(1, Math.min(slot, visibleItems.size() - 1));
    }
    if (movementMode != MovementMode.PINNED || pinPredicate == null) {
      return slot;
    }
    final int pinnedAfter = countPinnedVisible() - (isPinned(draggedItem) ? 1 : 0);
    if (isPinned(draggedItem)) {
      return Math.max(0, Math.min(slot, pinnedAfter));
    }
    return Math.max(pinnedAfter, Math.min(slot, visibleItems.size() - 1));
  }

  /**
   * Translates a post-removal visible slot into the model index that will render the item there.
   * Identifies the neighbour that should follow the dragged item in the new visible order and
   * returns that neighbour's post-removal model index; past the last item, the end of the model.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private int translateVisibleSlotToModelIndex(final int slot, final T draggedItem) {
    final List<T> remaining = new ArrayList<>(visibleItems);
    remaining.remove(draggedItem);
    if (slot >= remaining.size()) {
      return Math.max(0, model.getSize() - 1);
    }
    final T neighbor = remaining.get(slot);
    int index = 0;
    for (final T item : model) {
      if (item == draggedItem) {
        continue;
      }
      if (item == neighbor) {
        return index;
      }
      index++;
    }
    return Math.max(0, model.getSize() - 1);
  }

  private int countPinnedVisible() {
    if (movementMode != MovementMode.PINNED || pinPredicate == null) {
      return 0;
    }
    int count = 0;
    for (final T item : visibleItems) {
      if (pinPredicate.test(item)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Puts the content panel's children back in visible order. Needed after a no-op drop because
   * activating a drag raises the dragged view to Z-order zero, which also reorders the child array
   * that the idle layouts place by index. A committed drop does not need this — the model event
   * rebuilds the children from scratch.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private void restoreContentOrder() {
    int index = 0;
    for (final T item : visibleItems) {
      final JComponent view = viewByItem.get(item);
      if (view != null && view.getParent() == content) {
        content.setComponentZOrder(view, index);
        index++;
      }
    }
    content.revalidate();
    content.repaint();
  }

  /** Tracks the one drag that can be in flight at a time. */
  private final class DragState {
    private T item;
    private JComponent view;
    private int fromIndex;
    private int toIndex;
    private Point startPoint;
    private Point currentPoint;
    private Point grabOffset;
    private boolean active;
  }

  // --------------------------------------------------------------- selection

  /**
   * Selects a single item programmatically and syncs the rendered chrome.
   *
   * @param item the item to select
   * @version v0.5.0
   * @since v0.5.0
   */
  protected void selectItem(final T item) {
    selectionModel.setSelected(List.of(item));
  }

  private void handleSelectionPress(final T item, final int index, final int modifiers) {
    if (selectionMode == SelectionMode.NONE) {
      return;
    }
    final boolean shift = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
    final int metaOrCtrl = InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK;
    final boolean toggle = (modifiers & metaOrCtrl) != 0;

    if (selectionMode == SelectionMode.SINGLE_MANDATORY) {
      if (!selectionModel.isSelected(item)) {
        selectionModel.setSelected(List.of(item));
        selectionAnchor = item;
      }
    } else if (selectionMode == SelectionMode.SINGLE) {
      if (selectionModel.isSelected(item)) {
        selectionModel.clearSelection();
      } else {
        selectionModel.setSelected(List.of(item));
        selectionAnchor = item;
      }
    } else if (shift && selectionAnchor != null) {
      final int anchorIndex = visibleItems.indexOf(selectionAnchor);
      if (anchorIndex >= 0) {
        final int from = Math.min(anchorIndex, index);
        final int to = Math.max(anchorIndex, index);
        selectionModel.setSelected(new ArrayList<>(visibleItems.subList(from, to + 1)));
      } else {
        selectionModel.setSelected(List.of(item));
        selectionAnchor = item;
      }
    } else if (toggle) {
      selectionModel.toggle(item);
      selectionAnchor = item;
    } else {
      selectionModel.setSelected(List.of(item));
      selectionAnchor = item;
    }
    syncSelectionVisuals();
  }

  private void syncSelectionVisuals() {
    for (final Map.Entry<T, JComponent> entry : viewByItem.entrySet()) {
      if (entry.getValue() instanceof ElwhaListItemView itemView) {
        itemView.setSelected(selectionModel.isSelected(entry.getKey()));
      }
    }
  }

  // ------------------------------------------------------- reorder commands

  private void moveFocusedItem(final T item, final int delta) {
    if (!canReorderAnything() || item == null || isAnchored(item)) {
      return;
    }
    final int fromVisible = visibleItems.indexOf(item);
    if (fromVisible < 0) {
      return;
    }
    final int toVisible = Math.max(0, Math.min(visibleItems.size() - 1, fromVisible + delta));
    if (toVisible == fromVisible) {
      return;
    }
    final int clamped = clampSlotToPartition(toVisible, item);
    if (clamped == fromVisible) {
      return;
    }
    final int fromModel = model.indexOf(item);
    final int toModel = translateVisibleSlotToModelIndex(clamped, item);
    if (fromModel < 0 || toModel < 0 || fromModel == toModel || toModel >= model.getSize()) {
      return;
    }
    if (!commitMove(fromModel, toModel)) {
      return;
    }
    fireReorder(item, fromModel, toModel);
    focusedVisibleIndex = visibleItems.indexOf(item);
    final JComponent view = viewByItem.get(item);
    if (view != null) {
      view.requestFocusInWindow();
    }
  }

  private void deleteItem(final T item) {
    if (item == null) {
      return;
    }
    try {
      if (model.remove(item)) {
        selectionModel.remove(item);
      }
    } catch (final UnsupportedOperationException refused) {
      LOG.fine("ElwhaItemList: model refused remove(); delete ignored");
    }
  }

  // ------------------------------------------------------------ placeholders

  private JComponent defaultEmptyState() {
    return placeholderLabel("No items to show");
  }

  private JComponent defaultLoading() {
    return placeholderLabel("Loading…");
  }

  private static JComponent placeholderLabel(final String text) {
    final JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setForeground(UIManager.getColor("Label.disabledForeground"));
    label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    return label;
  }

  // ---------------------------------------------------------------- keyboard

  private void installKeyboard() {
    final Action previous = new MoveFocus(-1, false);
    final Action next = new MoveFocus(1, false);
    final Action previousInline = new MoveFocus(-1, true);
    final Action nextInline = new MoveFocus(1, true);
    final Action home = new JumpFocus(true);
    final Action end = new JumpFocus(false);
    final Action activate = new ActivateFocused();
    final Action selectAll = new SelectAllAction();
    final Action moveUp = new MoveItem(-1);
    final Action moveDown = new MoveItem(1);
    final Action delete = new DeleteFocused();

    // Bound in both maps so the shortcuts work whether the list itself or one of its items holds
    // focus. WHEN_FOCUSED alone dies as soon as an item takes focus; the ancestor map alone never
    // fires while the list is the focus owner.
    for (final int condition : new int[] {WHEN_FOCUSED, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT}) {
      final InputMap map = getInputMap(condition);
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "elwhaList.previous");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "elwhaList.next");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "elwhaList.previousInline");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "elwhaList.nextInline");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "elwhaList.home");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "elwhaList.end");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhaList.activate");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhaList.activate");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "elwhaList.selectAll");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), "elwhaList.selectAll");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "elwhaList.moveUp");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.META_DOWN_MASK), "elwhaList.moveUp");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK),
          "elwhaList.moveDown");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.META_DOWN_MASK),
          "elwhaList.moveDown");
      map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "elwhaList.delete");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_DOWN_MASK),
          "elwhaList.delete");
      map.put(
          KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.META_DOWN_MASK),
          "elwhaList.delete");
    }

    final ActionMap actions = getActionMap();
    actions.put("elwhaList.previous", previous);
    actions.put("elwhaList.next", next);
    actions.put("elwhaList.previousInline", previousInline);
    actions.put("elwhaList.nextInline", nextInline);
    actions.put("elwhaList.home", home);
    actions.put("elwhaList.end", end);
    actions.put("elwhaList.activate", activate);
    actions.put("elwhaList.selectAll", selectAll);
    actions.put("elwhaList.moveUp", moveUp);
    actions.put("elwhaList.moveDown", moveDown);
    actions.put("elwhaList.delete", delete);
  }

  private void moveFocus(final int delta) {
    if (visibleItems.isEmpty()) {
      return;
    }
    final int next = Math.max(0, Math.min(visibleItems.size() - 1, focusedVisibleIndex + delta));
    focusedVisibleIndex = next;
    final JComponent view = viewByItem.get(visibleItems.get(next));
    if (view != null) {
      view.requestFocusInWindow();
      scrollRectToVisible(view.getBounds());
    }
  }

  private T focusedItem() {
    if (focusedVisibleIndex < 0 || focusedVisibleIndex >= visibleItems.size()) {
      return null;
    }
    return visibleItems.get(focusedVisibleIndex);
  }

  /** Moves focus one step, optionally honoring RTL for the inline (left / right) arrows. */
  private final class MoveFocus extends AbstractAction {
    private final int delta;
    private final boolean inline;

    MoveFocus(final int delta, final boolean inline) {
      super();
      this.delta = delta;
      this.inline = inline;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      final boolean mirrored =
          inline && !isLeftToRight() && orientation != ElwhaListOrientation.VERTICAL;
      moveFocus(mirrored ? -delta : delta);
    }
  }

  private final class JumpFocus extends AbstractAction {
    private final boolean toStart;

    JumpFocus(final boolean toStart) {
      super();
      this.toStart = toStart;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      focusedVisibleIndex = -1;
      moveFocus(toStart ? 0 : visibleItems.size());
    }
  }

  private final class ActivateFocused extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent event) {
      if (focusedVisibleIndex < 0 || focusedVisibleIndex >= visibleItems.size()) {
        return;
      }
      handleSelectionPress(visibleItems.get(focusedVisibleIndex), focusedVisibleIndex, 0);
    }
  }

  private final class SelectAllAction extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent event) {
      if (selectionMode != SelectionMode.MULTIPLE) {
        return;
      }
      selectionModel.setSelected(new ArrayList<>(visibleItems));
      syncSelectionVisuals();
    }
  }

  private final class MoveItem extends AbstractAction {
    private final int delta;

    MoveItem(final int delta) {
      super();
      this.delta = delta;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      moveFocusedItem(focusedItem(), delta);
    }
  }

  private final class DeleteFocused extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent event) {
      deleteItem(focusedItem());
    }
  }

  // --------------------------------------------------------------- animation

  private void startFadeIn() {
    stopFade();
    fadeAlpha = 0f;
    final long start = System.currentTimeMillis();
    fadeTimer =
        new Timer(
            ANIMATION_TICK_MS,
            event -> {
              final long elapsed = System.currentTimeMillis() - start;
              fadeAlpha = Math.min(1f, elapsed / (float) animationDurationMs);
              repaint();
              if (fadeAlpha >= 1f) {
                ((Timer) event.getSource()).stop();
              }
            });
    fadeTimer.start();
  }

  private void stopFade() {
    if (fadeTimer != null && fadeTimer.isRunning()) {
      fadeTimer.stop();
    }
    fadeAlpha = 1f;
  }

  private void showDragHandle(final T item) {
    if (handleHoverItem == item && handleAlpha >= 1f) {
      return;
    }
    handleHoverItem = item;
    if (MorphAnimator.isReducedMotion()) {
      handleAlpha = 1f;
      content.repaint();
      return;
    }
    startHandleFade(1f);
  }

  private void hideDragHandle() {
    if (MorphAnimator.isReducedMotion()) {
      handleHoverItem = null;
      handleAlpha = 0f;
      content.repaint();
      return;
    }
    startHandleFade(0f);
  }

  private void startHandleFade(final float target) {
    if (handleTimer != null && handleTimer.isRunning()) {
      handleTimer.stop();
    }
    final float step = ANIMATION_TICK_MS / (float) HANDLE_FADE_MS;
    handleTimer =
        new Timer(
            ANIMATION_TICK_MS,
            event -> {
              handleAlpha =
                  target > handleAlpha
                      ? Math.min(target, handleAlpha + step)
                      : Math.max(target, handleAlpha - step);
              content.repaint();
              if (handleAlpha == target) {
                ((Timer) event.getSource()).stop();
                if (target == 0f) {
                  handleHoverItem = null;
                }
              }
            });
    handleTimer.start();
  }

  /**
   * Paints the hover-revealed drag handle over the leading edge of the item under the pointer.
   *
   * <p>Drawn by the content panel rather than added as a child component so it cannot disturb the
   * item indices, the layout, or the accessibility tree.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private void paintDragHandle(final Graphics g) {
    if (!usesHoverIcon() || handleHoverItem == null || handleAlpha <= 0f) {
      return;
    }
    final JComponent view = viewByItem.get(handleHoverItem);
    if (view == null || view.getWidth() <= 0) {
      return;
    }
    final Icon icon = MaterialIcons.dragIndicator(DRAG_HANDLE_SIZE);
    final Rectangle bounds = view.getBounds();
    final int x =
        isLeftToRight()
            ? bounds.x + DRAG_HANDLE_INSET
            : bounds.x + bounds.width - DRAG_HANDLE_INSET - icon.getIconWidth();
    final int y = bounds.y + (bounds.height - icon.getIconHeight()) / 2;
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setComposite(
          AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, handleAlpha)));
      icon.paintIcon(content, g2, x, y);
    } finally {
      g2.dispose();
    }
  }

  @Override
  protected void paintChildren(final Graphics g) {
    if (fadeAlpha >= 1f) {
      super.paintChildren(g);
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
      super.paintChildren(g2);
    } finally {
      g2.dispose();
    }
  }

  /**
   * Reports this list as a paint origin while a fade is running.
   *
   * <p>The fade composites live children through {@code paintChildren}. Without this, a child
   * repainting itself — a ripple, a caret blink — would paint directly and bypass the composite,
   * which both breaks the fade and freezes the child's own animation.
   *
   * @return true while a change fade is in progress
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public boolean isPaintingOrigin() {
    return fadeAlpha < 1f;
  }

  // ------------------------------------------------------------------ layout

  /** The panel that hosts the item components and runs the four layout managers. */
  private final class ContentPanel extends JPanel {
    ContentPanel() {
      super();
      setOpaque(false);
    }

    @Override
    protected void paintChildren(final Graphics g) {
      super.paintChildren(g);
      paintDragHandle(g);
    }
  }

  /** Single-column stack; items keep their preferred size and align to the leading edge. */
  private final class VerticalLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // positions come from the container walk
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // positions come from the container walk
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Insets in = parent.getInsets();
      int height = in.top + in.bottom;
      int width = 0;
      final int count = parent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Dimension pref = parent.getComponent(i).getPreferredSize();
        height += pref.height;
        if (i + 1 < count) {
          height += itemGap;
        }
        width = Math.max(width, pref.width);
      }
      return new Dimension(width + in.left + in.right, height);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      if (drag != null && drag.active) {
        layoutDuringDrag(parent);
        return;
      }
      final Insets in = parent.getInsets();
      int y = in.top;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component child = parent.getComponent(i);
        final Dimension pref = child.getPreferredSize();
        child.setBounds(flipX(parent, in.left, pref.width), y, pref.width, pref.height);
        y += pref.height + itemGap;
      }
    }
  }

  /**
   * Single row, items at their preferred width and a shared row height. Content wider than the
   * container is clipped — wrap the list in a {@link javax.swing.JScrollPane} for the scrolling
   * overflow strategy.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private final class HorizontalLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // positions come from the container walk
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // positions come from the container walk
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Insets in = parent.getInsets();
      int width = in.left + in.right;
      int rowHeight = 0;
      final int count = parent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Dimension pref = parent.getComponent(i).getPreferredSize();
        width += pref.width;
        if (i + 1 < count) {
          width += itemGap;
        }
        rowHeight = Math.max(rowHeight, pref.height);
      }
      return new Dimension(width, rowHeight + in.top + in.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      if (drag != null && drag.active) {
        layoutDuringDrag(parent);
        return;
      }
      final Insets in = parent.getInsets();
      int rowHeight = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        rowHeight = Math.max(rowHeight, parent.getComponent(i).getPreferredSize().height);
      }
      int x = in.left;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component child = parent.getComponent(i);
        final Dimension pref = child.getPreferredSize();
        child.setBounds(flipX(parent, x, pref.width), in.top, pref.width, rowHeight);
        x += pref.width + itemGap;
      }
    }
  }

  /**
   * Multi-row wrapping flow that breaks when the container width is exhausted, honoring the
   * configured gap for both row and column spacing.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private final class WrapLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // positions come from the container walk
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // positions come from the container walk
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      return layoutCore(parent, parent.getWidth(), false);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      if (drag != null && drag.active) {
        layoutDuringDrag(parent);
        return;
      }
      layoutCore(parent, parent.getWidth(), true);
    }

    /**
     * One pass that measures and, when asked, also places — so the manager honors the standard
     * contract without walking the children twice.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    private Dimension layoutCore(
        final Container parent, final int availWidth, final boolean apply) {
      final Insets in = parent.getInsets();
      final int maxRight = Math.max(in.left, availWidth - in.right);
      int x = in.left;
      int y = in.top;
      int rowHeight = 0;
      int contentMaxX = in.left;
      final int count = parent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Component child = parent.getComponent(i);
        final Dimension pref = child.getPreferredSize();
        if (x > in.left && x + pref.width > maxRight) {
          x = in.left;
          y += rowHeight + itemGap;
          rowHeight = 0;
        }
        if (apply) {
          child.setBounds(flipX(parent, x, pref.width), y, pref.width, pref.height);
        }
        x += pref.width + itemGap;
        rowHeight = Math.max(rowHeight, pref.height);
        contentMaxX = Math.max(contentMaxX, x - itemGap);
      }
      return new Dimension(contentMaxX + in.right, y + rowHeight + in.bottom);
    }
  }

  /**
   * Uniform N-column grid. Column zero sits on the leading edge, so it moves to the right under an
   * RTL orientation.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private final class GridLayoutImpl implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // positions come from the container walk
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // positions come from the container walk
    }

    private int cellHeight(final Container parent) {
      int max = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        max = Math.max(max, parent.getComponent(i).getPreferredSize().height);
      }
      return max > 0 ? max : 24;
    }

    private int cellWidth(final Container parent) {
      int max = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        max = Math.max(max, parent.getComponent(i).getPreferredSize().width);
      }
      return max > 0 ? max : 60;
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Insets in = parent.getInsets();
      final int rows = (parent.getComponentCount() + columns - 1) / Math.max(1, columns);
      return new Dimension(
          cellWidth(parent) * columns + itemGap * Math.max(0, columns - 1) + in.left + in.right,
          cellHeight(parent) * rows + itemGap * Math.max(0, rows - 1) + in.top + in.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      if (drag != null && drag.active) {
        layoutDuringDrag(parent);
        return;
      }
      final Insets in = parent.getInsets();
      final int available = Math.max(0, parent.getWidth() - in.left - in.right);
      final int cellW = Math.max(1, (available - (columns - 1) * itemGap) / columns);
      final int cellH = cellHeight(parent);
      for (int i = 0; i < parent.getComponentCount(); i++) {
        parent
            .getComponent(i)
            .setBounds(
                flipX(parent, in.left + (i % columns) * (cellW + itemGap), cellW),
                in.top + (i / columns) * (cellH + itemGap),
                cellW,
                cellH);
      }
    }
  }

  /**
   * Positions children while a drag is active: the dragged item follows the pointer, compensated
   * for the grab offset and clamped inside the content bounds so its shadow halo cannot escape and
   * leave repaint residue on neighbouring sections; every other item reads its current animated
   * position from the displacement maps.
   *
   * <p>Sizes mirror the orientation's idle layout so nothing snaps when the drag begins — preferred
   * sizes for VERTICAL, HORIZONTAL, and WRAP, and the uniform stretched cell for GRID, including
   * for the dragged item itself.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private void layoutDuringDrag(final Container parent) {
    final Insets in = parent.getInsets();
    final boolean grid = orientation == ElwhaListOrientation.GRID;
    int gridCellW = 0;
    int gridCellH = 0;
    if (grid) {
      final int available = Math.max(0, parent.getWidth() - in.left - in.right);
      gridCellW = Math.max(1, (available - (columns - 1) * itemGap) / columns);
      for (int i = 0; i < parent.getComponentCount(); i++) {
        gridCellH = Math.max(gridCellH, parent.getComponent(i).getPreferredSize().height);
      }
      if (gridCellH <= 0) {
        gridCellH = 24;
      }
    }
    for (int i = 0; i < parent.getComponentCount(); i++) {
      final Component child = parent.getComponent(i);
      final Dimension pref = child.getPreferredSize();
      final int width = grid ? gridCellW : pref.width;
      final int height = grid ? gridCellH : pref.height;
      if (child == drag.view) {
        final int maxX = Math.max(in.left, parent.getWidth() - in.right - width);
        final int maxY = Math.max(in.top, parent.getHeight() - in.bottom - height);
        final int x = Math.max(in.left, Math.min(maxX, drag.currentPoint.x - drag.grabOffset.x));
        final int y = Math.max(in.top, Math.min(maxY, drag.currentPoint.y - drag.grabOffset.y));
        child.setBounds(x, y, width, height);
      } else {
        final Integer dispX = displacedX.get(child);
        final Integer dispY = displacedY.get(child);
        child.setBounds(
            dispX != null ? dispX : in.left, dispY != null ? dispY : in.top, width, height);
      }
    }
  }

  // ------------------------------------------------------------- LAF / a11y

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    repaint();
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaItemList();
    }
    return accessibleContext;
  }

  /**
   * Exposes the list as an {@link AccessibleRole#LIST} whose children are exactly the visible items
   * in render order, and whose selection assistive technology can both read and drive.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  protected class AccessibleElwhaItemList extends AccessibleJPanel implements AccessibleSelection {

    /**
     * Creates the accessible context for the host list.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    protected AccessibleElwhaItemList() {}

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.LIST;
    }

    @Override
    public int getAccessibleChildrenCount() {
      return visibleItems.size();
    }

    @Override
    public Accessible getAccessibleChild(final int index) {
      if (index < 0 || index >= visibleItems.size()) {
        return null;
      }
      final JComponent view = viewByItem.get(visibleItems.get(index));
      return view instanceof Accessible accessible ? accessible : null;
    }

    @Override
    public AccessibleSelection getAccessibleSelection() {
      return this;
    }

    @Override
    public int getAccessibleSelectionCount() {
      int count = 0;
      for (final T item : visibleItems) {
        if (selectionModel.isSelected(item)) {
          count++;
        }
      }
      return count;
    }

    @Override
    public Accessible getAccessibleSelection(final int index) {
      int seen = 0;
      for (final T item : visibleItems) {
        if (!selectionModel.isSelected(item)) {
          continue;
        }
        if (seen == index) {
          final JComponent view = viewByItem.get(item);
          return view instanceof Accessible accessible ? accessible : null;
        }
        seen++;
      }
      return null;
    }

    @Override
    public boolean isAccessibleChildSelected(final int index) {
      if (index < 0 || index >= visibleItems.size()) {
        return false;
      }
      return selectionModel.isSelected(visibleItems.get(index));
    }

    @Override
    public void addAccessibleSelection(final int index) {
      if (index < 0 || index >= visibleItems.size() || selectionMode == SelectionMode.NONE) {
        return;
      }
      final T item = visibleItems.get(index);
      if (selectionMode == SelectionMode.MULTIPLE) {
        selectionModel.add(item);
      } else {
        selectionModel.setSelected(List.of(item));
      }
      syncSelectionVisuals();
    }

    @Override
    public void removeAccessibleSelection(final int index) {
      if (index < 0 || index >= visibleItems.size()) {
        return;
      }
      // Mandatory mode's whole contract is that the selection is never empty; honoring a
      // deselect here would break it from the assistive side only.
      if (selectionMode == SelectionMode.SINGLE_MANDATORY) {
        return;
      }
      selectionModel.remove(visibleItems.get(index));
      syncSelectionVisuals();
    }

    @Override
    public void clearAccessibleSelection() {
      if (selectionMode == SelectionMode.SINGLE_MANDATORY) {
        return;
      }
      selectionModel.clearSelection();
      syncSelectionVisuals();
    }

    @Override
    public void selectAllAccessibleSelection() {
      if (selectionMode != SelectionMode.MULTIPLE) {
        return;
      }
      selectionModel.setSelected(new ArrayList<>(visibleItems));
      syncSelectionVisuals();
    }
  }
}
