package com.owspfm.ui.components.pill.list;

import com.owspfm.ui.components.card.list.Cursors;
import com.owspfm.ui.components.flatlist.FlatList;
import com.owspfm.ui.components.flatlist.FlatListOrientation;
import com.owspfm.ui.components.icons.MaterialIcons;
import com.owspfm.ui.components.pill.FlatPill;
import com.owspfm.ui.components.pill.PillInteractionMode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
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
 * A reusable, observable, model-driven list of {@link FlatPill}s.
 *
 * <p>Selection (single / multiple / none), filter, sort, empty / loading state, keyboard
 * navigation, and accessibility ship from sub-story #236; all four {@link FlatListOrientation}
 * values (VERTICAL, HORIZONTAL, WRAP, GRID) are wired by sub-story #238; drag-to-reorder works on
 * every orientation as of sub-story #239. {@link FlatListOrientation#HORIZONTAL} uses a clip+scroll
 * overflow strategy — wrap the list in a {@link javax.swing.JScrollPane} for horizontally-scrolling
 * pill rows.
 *
 * <p><strong>Quick start</strong>:
 *
 * <pre>{@code
 * DefaultPillListModel<Factor> model = new DefaultPillListModel<>(factors);
 * PillAdapter<Factor> adapter =
 *     (factor, idx) -> new FlatPill(factor.name()).setVariant(PillVariant.OUTLINED);
 *
 * FlatPillList<Factor> list = new FlatPillList<>(model, adapter)
 *     .setSelectionMode(PillSelectionMode.SINGLE);
 * list.getSelectionModel().addSelectionListener(evt ->
 *     System.out.println("selection: " + evt.getSelected()));
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class FlatPillList<T> extends JPanel implements Accessible, FlatList<T> {

  /**
   * Movement semantics for the list. Mutually exclusive — the list is in exactly one mode at a
   * time. Switching modes via {@link #setMovementMode(MovementMode)} drives drag-enable, cursor
   * affordance, the auto-injected Pin/Unpin context menu, and (in the future) anchor support.
   *
   * <p>For back-compat, {@link #setReorderable(boolean)} maps to {@link #STATIC} (false) or {@link
   * #MOVABLE} (true), and {@link #setPinPredicate(Predicate)} implicitly flips the mode to {@link
   * #PINNED}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public enum MovementMode {
    /**
     * Pills are display-only. No drag, no Move-Up/Down menu items, no grab cursor; the pin API is
     * inert. Default for a fresh list (matches the historical {@code setReorderable(false)}
     * behavior).
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    STATIC,
    /** Pills can be freely reordered via drag. No pinned partition. */
    MOVABLE,
    /**
     * As {@link #MOVABLE}, plus a pinned partition: items reporting {@code true} from the pin
     * predicate render before unpinned items, regardless of comparator. Pin/Unpin is auto-injected
     * into the context menu when the predicate, action, and no caller-installed menu are all
     * present.
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    PINNED,
    /**
     * As {@link #MOVABLE}, plus a single-pill anchor: the item reporting {@code true} from the
     * anchor predicate is locked at the leading slot and cannot be dragged. Non-anchor pills can
     * reorder freely but cannot drop onto slot 0. Set as anchor / Remove anchor is auto-injected
     * into the context menu when the predicate, action, and no caller-installed menu are all
     * present. Mutually exclusive with {@link #PINNED}.
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    ANCHORED
  }

  /**
   * Visual treatment for the pin / anchor leading-slot affordance. Both pin and anchor get their
   * own independent setting — many callers will want a clickable pin but a menu-only anchor (or
   * vice versa).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public enum IconAffordance {
    /** No leading-icon affordance shown. The pin/anchor state remains queryable via API. */
    NONE,
    /**
     * Static glyph shown only when the item is pinned/anchored — not clickable. Default; matches
     * the historical "leading icon as state indicator" behavior.
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    INDICATOR,
    /**
     * Clickable button. For pin: outline glyph on every pill, filled when pinned; click toggles.
     * For anchor: persistent filled glyph on the anchored pill; hover-revealed outline glyph on
     * non-anchored pills; click sets/clears the anchor.
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    BUTTON
  }

  /** Pixels the cursor must travel from press before a drag is considered activated. */
  static final int DRAG_THRESHOLD = 5;

  private static final Logger LOG = Logger.getLogger(FlatPillList.class.getName());
  private static final int DEFAULT_GAP = 6;
  private static final int DEFAULT_COLUMNS = 4;
  private static final int ANIMATION_TICK_MS = 16;

  // Backing model + adapter -----------------------------------------------
  private final PillListModel<T> model;
  private final PillAdapter<T> adapter;
  private final PillListDataListener modelListener = this::onModelChanged;
  private final PillSelectionModel<T> selectionModel = new DefaultPillSelectionModel<>();

  // Configuration ---------------------------------------------------------
  private FlatListOrientation orientation = FlatListOrientation.VERTICAL;
  private int columns = DEFAULT_COLUMNS;
  private int itemGap = DEFAULT_GAP;
  private Insets listPadding = new Insets(DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP);
  private PillSelectionMode selectionMode = PillSelectionMode.NONE;
  private MovementMode movementMode = MovementMode.STATIC;
  private boolean reorderWarningLogged;
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

  // Listeners -------------------------------------------------------------
  private final List<PillReorderListener<T>> reorderListeners = new ArrayList<>();

  // Render cache: visible items in render order ---------------------------
  private final List<T> visibleItems = new ArrayList<>();
  private final Map<T, FlatPill> pillByItem = new LinkedHashMap<>();
  private int focusedVisibleIndex = -1;
  private T selectionAnchor;

  // Layout panels ---------------------------------------------------------
  private final JPanel content;
  private final JPanel emptyHolder;
  private final JPanel loadingHolder;

  // Drag state ------------------------------------------------------------
  private DragState drag;
  private final Map<FlatPill, Integer> displacedX = new HashMap<>();
  private final Map<FlatPill, Integer> displacedY = new HashMap<>();
  private final Map<FlatPill, Integer> targetX = new HashMap<>();
  private final Map<FlatPill, Integer> targetY = new HashMap<>();
  private Timer dragAnimTimer;

  // ------------------------------------------------------------------ ctor

  /**
   * Builds a list bound to the given model and adapter.
   *
   * @param model the backing model (required)
   * @param adapter the adapter that maps items to pills (required)
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList(final PillListModel<T> model, final PillAdapter<T> adapter) {
    super(new BorderLayout());
    if (model == null) {
      throw new IllegalArgumentException("model must not be null");
    }
    if (adapter == null) {
      throw new IllegalArgumentException("adapter must not be null");
    }
    this.model = model;
    this.adapter = adapter;

    content = new ContentPanel();
    content.setOpaque(false);
    applyOrientationLayout();

    emptyHolder = new JPanel(new BorderLayout());
    emptyHolder.setOpaque(false);

    loadingHolder = new JPanel(new BorderLayout());
    loadingHolder.setOpaque(false);

    setOpaque(false);
    setFocusable(true);
    add(content, BorderLayout.CENTER);
    rebuildPadding();

    model.addPillListDataListener(modelListener);
    rebuildVisibleItems();
    rebuildContent();

    installKeyboard();
  }

  // ------------------------------------------------------------ public API

  /**
   * Returns the backing model.
   *
   * @return the model
   * @version v0.1.0
   * @since v0.1.0
   */
  public PillListModel<T> getModel() {
    return model;
  }

  /**
   * Returns the selection model. Always non-null even when selection is disabled.
   *
   * @return the selection model
   * @version v0.1.0
   * @since v0.1.0
   */
  public PillSelectionModel<T> getSelectionModel() {
    return selectionModel;
  }

  /**
   * Returns the current orientation.
   *
   * @return the orientation (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatListOrientation getOrientation() {
    return orientation;
  }

  /**
   * Sets the layout orientation. All four {@link FlatListOrientation} values are supported.
   *
   * @param orientation one of {@link FlatListOrientation}; null is ignored
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setOrientation(final FlatListOrientation orientation) {
    if (orientation == null || orientation == this.orientation) {
      return this;
    }
    this.orientation = orientation;
    applyOrientationLayout();
    rebuildContent();
    return this;
  }

  /**
   * Sets the column count for {@link FlatListOrientation#GRID}. No effect on other orientations.
   *
   * @param columns column count, clamped to {@code >= 1}
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setColumns(final int columns) {
    final int v = Math.max(1, columns);
    if (v == this.columns) {
      return this;
    }
    this.columns = v;
    if (orientation == FlatListOrientation.GRID) {
      applyOrientationLayout();
      rebuildContent();
    }
    return this;
  }

  /**
   * Returns the column count for grid mode.
   *
   * @return the column count
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public int getColumns() {
    return columns;
  }

  /**
   * Sets the gap between rendered pills.
   *
   * @param gap pixels, clamped to {@code >= 0}
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setItemGap(final int gap) {
    final int v = Math.max(0, gap);
    if (v == itemGap) {
      return this;
    }
    itemGap = v;
    applyOrientationLayout();
    rebuildContent();
    return this;
  }

  /**
   * Returns the active item gap.
   *
   * @return the gap in pixels
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public int getItemGap() {
    return itemGap;
  }

  /**
   * Sets the padding around the rendered list.
   *
   * @param insets the insets; null treated as zero
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setListPadding(final Insets insets) {
    listPadding = insets == null ? new Insets(0, 0, 0, 0) : (Insets) insets.clone();
    rebuildPadding();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Sets the selection mode.
   *
   * @param mode the selection mode; null is ignored
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setSelectionMode(final PillSelectionMode mode) {
    if (mode == null || mode == selectionMode) {
      return this;
    }
    selectionMode = mode;
    if (mode == PillSelectionMode.NONE) {
      selectionModel.clearSelection();
    } else if (mode == PillSelectionMode.SINGLE_MANDATORY) {
      ensureMandatorySelection();
    }
    rebuildContent();
    return this;
  }

  /**
   * Guarantees that {@link PillSelectionMode#SINGLE_MANDATORY}'s "always exactly one" contract
   * holds: if no item is currently selected and the visible list is non-empty, auto-select the
   * first visible item. No-op otherwise (including when mode isn't mandatory).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void ensureMandatorySelection() {
    if (selectionMode != PillSelectionMode.SINGLE_MANDATORY) {
      return;
    }
    if (selectionModel.getSelected().isEmpty() && !visibleItems.isEmpty()) {
      final T first = visibleItems.get(0);
      selectionModel.setSelected(List.of(first));
      selectionAnchor = first;
    }
  }

  /**
   * Returns the active selection mode.
   *
   * @return the selection mode (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public PillSelectionMode getSelectionMode() {
    return selectionMode;
  }

  /**
   * Sets the movement mode. See {@link MovementMode} for semantics. Mode changes trigger a content
   * rebuild so visual affordances (cursor, pin glyph, partition divider, anchor icon) update
   * immediately.
   *
   * @param mode the movement mode; null is ignored
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setMovementMode(final MovementMode mode) {
    if (mode == null || mode == movementMode) {
      return this;
    }
    movementMode = mode;
    // Mutual exclusion between PINNED and ANCHORED — caller can re-install on a future flip.
    if (mode == MovementMode.ANCHORED && (pinPredicate != null || pinAction != null)) {
      LOG.fine("FlatPillList: switching to ANCHORED clears pin predicate/action");
      pinPredicate = null;
      pinAction = null;
    } else if (mode == MovementMode.PINNED && (anchorPredicate != null || anchorAction != null)) {
      LOG.fine("FlatPillList: switching to PINNED clears anchor predicate/action");
      anchorPredicate = null;
      anchorAction = null;
    }
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Returns the active movement mode.
   *
   * @return the active mode (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public MovementMode getMovementMode() {
    return movementMode;
  }

  /**
   * Back-compat shim: {@code true} flips to {@link MovementMode#MOVABLE} (unless the list is
   * already {@link MovementMode#PINNED}, in which case the pin partition is preserved); {@code
   * false} flips to {@link MovementMode#STATIC}. Prefer {@link #setMovementMode(MovementMode)} in
   * new code.
   *
   * @param reorderable whether reorder is enabled
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setReorderable(final boolean reorderable) {
    if (reorderable) {
      // Stay in PINNED if already there — flipping a pin-partitioned list to MOVABLE would
      // silently drop the partition and surprise the caller.
      if (movementMode == MovementMode.STATIC) {
        return setMovementMode(MovementMode.MOVABLE);
      }
      return this;
    }
    return setMovementMode(MovementMode.STATIC);
  }

  /**
   * Returns whether drag-to-reorder is enabled — equivalent to {@code getMovementMode() !=
   * MovementMode.STATIC}.
   *
   * @return reorder flag
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isReorderable() {
    return movementMode != MovementMode.STATIC;
  }

  /**
   * Sets a filter predicate that hides items rejected by it. Pass null to clear.
   *
   * @param filter the predicate; null clears filtering
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setFilter(final Predicate<T> filter) {
    this.filter = filter;
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Sets a sort comparator that orders rendered items. Pass null to clear.
   *
   * @param comparator the comparator; null clears sorting
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setSortOrder(final Comparator<T> comparator) {
    this.comparator = comparator;
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Installs a predicate that reports which items are pinned. Pinned items render before unpinned
   * items, regardless of model order or comparator. The list does NOT own pin state — the caller
   * decides; this predicate is queried during {@code rebuildVisibleItems} and on demand. Pass null
   * to clear partitioning (restores the original "flat" render order).
   *
   * <p>When pin state changes externally (caller toggles a flag, mutates a set, etc.), call {@code
   * pinStateChanged()} so the list rebuilds. The list does not poll the predicate.
   *
   * <p>Installing a non-null predicate implicitly flips the list to {@link MovementMode#PINNED} —
   * the partition is only meaningful in that mode. The flip is logged at {@code FINE} so callers
   * who explicitly set the mode first don't see a surprise.
   *
   * @param isPinned the predicate, or null to disable partitioning
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setPinPredicate(final Predicate<T> isPinned) {
    pinPredicate = isPinned;
    if (isPinned != null && movementMode != MovementMode.PINNED) {
      LOG.fine("FlatPillList: pin predicate installed; flipping movement mode to PINNED");
      // setMovementMode rebuilds, so don't call rebuild* below.
      return setMovementMode(MovementMode.PINNED);
    }
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Installs the action invoked by {@link #togglePin(Object)}. The list calls the action with the
   * item and the NEW pin state (true = pinning now, false = unpinning). The caller is responsible
   * for updating whatever backing store the {@code pinPredicate} reads from, then calling {@link
   * #pinStateChanged()} so the list rebuilds. Pass null to disable {@code togglePin}.
   *
   * @param action the action, or null
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setPinAction(final BiConsumer<T, Boolean> action) {
    pinAction = action;
    return this;
  }

  /**
   * Returns true if the installed pin predicate reports the item as pinned <em>and</em> the list is
   * in {@link MovementMode#PINNED} mode. False otherwise — in {@code STATIC}/{@code MOVABLE} the
   * predicate is inert even if installed.
   *
   * @param item the item
   * @return whether the item is currently pinned
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isPinned(final T item) {
    return movementMode == MovementMode.PINNED && pinPredicate != null && pinPredicate.test(item);
  }

  /**
   * Toggles the pin state of an item by invoking the configured {@link #setPinAction(BiConsumer)}.
   * The caller's action must persist the change AND call {@link #pinStateChanged()} (or otherwise
   * trigger a model event) so the list rebuilds. No-op outside {@link MovementMode#PINNED} mode or
   * when no action is installed.
   *
   * @param item the item to toggle
   * @version v0.1.0
   * @since v0.1.0
   */
  public void togglePin(final T item) {
    if (movementMode != MovementMode.PINNED || pinAction == null || item == null) {
      return;
    }
    pinAction.accept(item, !isPinned(item));
  }

  /**
   * Signals that the pin predicate's answers have changed for at least one item, without the model
   * itself changing. Triggers a rebuild so the partition reflects current predicate state. Cheaper
   * than firing a synthetic model event.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public void pinStateChanged() {
    rebuildVisibleItems();
    rebuildContent();
  }

  /**
   * Returns an unmodifiable snapshot of the items currently rendered, in render order (filter
   * applied, pin partition applied if a predicate is installed, comparator applied within partition
   * or globally as configured). Intended for tests, accessibility, and external navigation helpers.
   * The returned list does not track subsequent updates.
   *
   * @return rendered items in render order (never null, may be empty)
   * @version v0.1.0
   * @since v0.1.0
   */
  public List<T> getVisibleItems() {
    return List.copyOf(visibleItems);
  }

  /**
   * Builds a "Pin" / "Unpin" {@link JMenuItem} bound to the given item. Label reflects the current
   * pin state at the moment this is called — refresh by rebuilding the menu on each popup show.
   * Intended for callers who install their own context menu and want to compose the pin toggle into
   * it. Auto-injection only fires when the pill has no caller-installed menu, so this is the path
   * to use when you want both.
   *
   * @param item the item to toggle
   * @return a fresh menu item; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  public JMenuItem createPinMenuItem(final T item) {
    final JMenuItem menuItem = new JMenuItem(isPinned(item) ? "Unpin" : "Pin");
    menuItem.addActionListener(e -> togglePin(item));
    return menuItem;
  }

  // ----------------------------------------------------------------- anchor

  /**
   * Installs the predicate that identifies the anchored item. At most one item should report {@code
   * true}; if multiple do, the first hit in model order wins. Pass null to clear.
   *
   * <p>Installing a non-null predicate implicitly flips the list to {@link MovementMode#ANCHORED} —
   * the anchor partition is only meaningful in that mode. The flip is logged at {@code FINE}.
   * Because PINNED and ANCHORED are mutually exclusive, this also clears any installed pin
   * predicate/action.
   *
   * @param isAnchored the predicate, or null
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setAnchorPredicate(final Predicate<T> isAnchored) {
    anchorPredicate = isAnchored;
    if (isAnchored != null && movementMode != MovementMode.ANCHORED) {
      LOG.fine("FlatPillList: anchor predicate installed; flipping movement mode to ANCHORED");
      // setMovementMode handles the mutex with PINNED and rebuilds.
      return setMovementMode(MovementMode.ANCHORED);
    }
    rebuildVisibleItems();
    rebuildContent();
    return this;
  }

  /**
   * Installs the action invoked by {@link #setAnchor(Object)} / {@link #clearAnchor()}. The list
   * calls the action with the item to make anchored (or {@code null} to clear). The caller's action
   * must persist the change AND call {@link #anchorStateChanged()} (or trigger a model event) so
   * the list rebuilds. Pass null to disable.
   *
   * @param action the action, or null
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setAnchorAction(final Consumer<T> action) {
    anchorAction = action;
    return this;
  }

  /**
   * Returns true if the installed anchor predicate reports the item as anchored <em>and</em> the
   * list is in {@link MovementMode#ANCHORED} mode.
   *
   * @param item the item
   * @return whether the item is currently anchored
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isAnchored(final T item) {
    return movementMode == MovementMode.ANCHORED
        && anchorPredicate != null
        && anchorPredicate.test(item);
  }

  /**
   * Returns the first model item the anchor predicate reports as anchored, or {@code null} if no
   * item is anchored or the list is not in {@link MovementMode#ANCHORED} mode.
   *
   * @return the anchored item, or null
   * @version v0.1.0
   * @since v0.1.0
   */
  public T getAnchoredItem() {
    if (movementMode != MovementMode.ANCHORED || anchorPredicate == null) {
      return null;
    }
    for (T item : model) {
      if (anchorPredicate.test(item)) {
        return item;
      }
    }
    return null;
  }

  /**
   * Makes {@code item} the sole anchor by invoking the configured action. Pass null to clear the
   * anchor. No-op outside {@link MovementMode#ANCHORED} mode or when no action is installed.
   *
   * @param item the item to anchor, or null to clear
   * @version v0.1.0
   * @since v0.1.0
   */
  public void setAnchor(final T item) {
    if (movementMode != MovementMode.ANCHORED || anchorAction == null) {
      return;
    }
    anchorAction.accept(item);
  }

  /** Clears the anchor. Equivalent to {@code setAnchor(null)}. */
  public void clearAnchor() {
    setAnchor(null);
  }

  /**
   * Signals that the anchor predicate's answers have changed without a model event firing. Triggers
   * a rebuild so the partition reflects current predicate state.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public void anchorStateChanged() {
    rebuildVisibleItems();
    rebuildContent();
  }

  /**
   * Builds a "Set as anchor" / "Remove anchor" {@link JMenuItem} bound to the given item. Label
   * reflects the current anchor state at the moment this is called — refresh by rebuilding the menu
   * on each popup show. Intended for callers who install their own context menu and want to compose
   * the anchor toggle into it.
   *
   * @param item the item to toggle
   * @return a fresh menu item; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  public JMenuItem createAnchorMenuItem(final T item) {
    final JMenuItem menuItem = new JMenuItem(isAnchored(item) ? "Remove anchor" : "Set as anchor");
    menuItem.addActionListener(
        e -> {
          if (isAnchored(item)) {
            clearAnchor();
          } else {
            setAnchor(item);
          }
        });
    return menuItem;
  }

  // ----------------------------------------------------------- affordances

  /**
   * Sets the visual treatment for the pin affordance. Default {@link IconAffordance#INDICATOR}
   * preserves the historical behavior (filled pin glyph shown only on pinned pills, not clickable).
   * {@link IconAffordance#BUTTON} renders an outline pin on every pill, filled when pinned, with
   * click-to-toggle.
   *
   * <p>Only effective in {@link MovementMode#PINNED}; ignored in other modes.
   *
   * @param affordance the affordance treatment; null is ignored
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setPinAffordance(final IconAffordance affordance) {
    if (affordance == null || affordance == pinAffordance) {
      return this;
    }
    pinAffordance = affordance;
    rebuildContent();
    return this;
  }

  /**
   * Returns the current pin affordance treatment.
   *
   * @return the pin affordance (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconAffordance getPinAffordance() {
    return pinAffordance;
  }

  /**
   * Sets the visual treatment for the anchor affordance. Default {@link IconAffordance#INDICATOR}
   * shows the anchor glyph only on the anchored pill, not clickable. {@link IconAffordance#BUTTON}
   * adds a persistent filled glyph on the anchored pill plus a hover-revealed outline glyph on
   * non-anchored pills, both clickable.
   *
   * <p>Only effective in {@link MovementMode#ANCHORED}; ignored in other modes.
   *
   * @param affordance the affordance treatment; null is ignored
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPillList<T> setAnchorAffordance(final IconAffordance affordance) {
    if (affordance == null || affordance == anchorAffordance) {
      return this;
    }
    anchorAffordance = affordance;
    rebuildContent();
    return this;
  }

  /**
   * Returns the current anchor affordance treatment.
   *
   * @return the anchor affordance (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public IconAffordance getAnchorAffordance() {
    return anchorAffordance;
  }

  /**
   * Replaces the empty-state placeholder. Pass null to fall back to the built-in default.
   *
   * @param component the placeholder; null restores the default
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setEmptyState(final JComponent component) {
    emptyState = component;
    if (visibleItems.isEmpty()) {
      rebuildContent();
    }
    return this;
  }

  /**
   * Sets the loading flag. While true, the list renders the loading component instead of items.
   *
   * @param loading whether to show the loading state
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setLoading(final boolean loading) {
    if (loading == this.loading) {
      return this;
    }
    this.loading = loading;
    rebuildContent();
    return this;
  }

  /**
   * Replaces the loading-state component. Pass null to fall back to the built-in default.
   *
   * @param component the loading component
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatPillList<T> setLoadingComponent(final JComponent component) {
    loadingComponent = component;
    if (loading) {
      rebuildContent();
    }
    return this;
  }

  /**
   * Registers a reorder listener. (Reorder events are not fired until #239 wires drag.)
   *
   * @param listener the listener; null is ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addReorderListener(final PillReorderListener<T> listener) {
    if (listener != null && !reorderListeners.contains(listener)) {
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
  public void removeReorderListener(final PillReorderListener<T> listener) {
    reorderListeners.remove(listener);
  }

  /**
   * Returns the rendered pill for the given item, or {@code null} if the item is not currently
   * visible.
   *
   * @param item the item
   * @return the pill, or null
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatPill getPillFor(final T item) {
    return pillByItem.get(item);
  }

  // -------------------------------------------------------------- internals

  private void applyOrientationLayout() {
    content.removeAll();
    switch (orientation) {
      case VERTICAL -> content.setLayout(new VerticalLayout());
      case HORIZONTAL -> content.setLayout(new HorizontalLayout());
      case WRAP -> content.setLayout(new WrapLayout());
      case GRID -> content.setLayout(new GridLayoutImpl());
      default -> content.setLayout(new VerticalLayout());
    }
  }

  private void rebuildPadding() {
    setBorder(
        BorderFactory.createEmptyBorder(
            listPadding.top, listPadding.left, listPadding.bottom, listPadding.right));
  }

  private void onModelChanged(final PillListDataEvent event) {
    rebuildVisibleItems();
    // Mandatory mode must re-assert "exactly one selected" if a removal just emptied the
    // selection. Runs after rebuildVisibleItems so the auto-pick lands on a still-visible item.
    ensureMandatorySelection();
    rebuildContent();
  }

  private void rebuildVisibleItems() {
    visibleItems.clear();
    final boolean pinPartition = movementMode == MovementMode.PINNED && pinPredicate != null;
    final boolean anchorPartition =
        movementMode == MovementMode.ANCHORED && anchorPredicate != null;
    if (!pinPartition && !anchorPartition) {
      // Fast path — no partition.
      final Iterator<T> it = model.iterator();
      while (it.hasNext()) {
        final T item = it.next();
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
      // Anchor partition: the (at most one) anchored item renders first; the rest follow in
      // comparator order if any, otherwise model order.
      T anchor = null;
      final List<T> rest = new ArrayList<>();
      final Iterator<T> it = model.iterator();
      while (it.hasNext()) {
        final T item = it.next();
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
    // Pinned partition: pinned items render first, then unpinned. Comparator (if any) is applied
    // within each partition independently so pins maintain their group identity regardless of the
    // sort key.
    final List<T> pinned = new ArrayList<>();
    final List<T> unpinned = new ArrayList<>();
    final Iterator<T> it = model.iterator();
    while (it.hasNext()) {
      final T item = it.next();
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

    pillByItem.clear();
    content.removeAll();
    applyOrientationLayout();

    for (int i = 0; i < visibleItems.size(); i++) {
      final T item = visibleItems.get(i);
      final FlatPill pill = adapter.pillFor(item, i);
      if (pill == null) {
        continue;
      }
      pillByItem.put(item, pill);
      configurePillForList(pill, item, i);
      content.add(pill);
    }
    add(content, BorderLayout.CENTER);

    if (focusedVisibleIndex >= visibleItems.size()) {
      focusedVisibleIndex = visibleItems.size() - 1;
    }

    revalidate();
    repaint();
  }

  private void configurePillForList(final FlatPill pill, final T item, final int index) {
    if (selectionMode != PillSelectionMode.NONE) {
      // CLICKABLE (not SELECTABLE) so the pill fires action events but does NOT auto-toggle its
      // own selected state — the list owns selection through its model.
      pill.setInteractionMode(PillInteractionMode.CLICKABLE);
      pill.setSelected(selectionModel.isSelected(item));
    }

    if (movementMode == MovementMode.PINNED) {
      applyPinAffordance(pill, item);
    } else if (movementMode == MovementMode.ANCHORED) {
      applyAnchorAffordance(pill, item);
    }

    if (movementMode == MovementMode.PINNED
        && pinPredicate != null
        && pinAction != null
        && !pill.hasContextMenu()) {
      // Auto-inject Pin/Unpin only when the caller's adapter hasn't installed its own context
      // menu. Callers who DO have their own menu should compose the pin entry themselves via
      // createPinMenuItem(T) — that's the documented way to combine the two.
      pill.attachContextMenu(
          () -> {
            final JPopupMenu menu = new JPopupMenu();
            menu.add(createPinMenuItem(item));
            return menu;
          });
    } else if (movementMode == MovementMode.ANCHORED
        && anchorPredicate != null
        && anchorAction != null
        && !pill.hasContextMenu()) {
      // Mirror of the pin auto-injection: Set as anchor / Remove anchor when no caller menu.
      pill.attachContextMenu(
          () -> {
            final JPopupMenu menu = new JPopupMenu();
            menu.add(createAnchorMenuItem(item));
            return menu;
          });
    }

    // Anchored item is locked; even though the mode permits drag, this specific pill refuses it.
    final boolean draggableItem = !isAnchored(item);
    final boolean canReorder =
        movementMode != MovementMode.STATIC && comparator == null && draggableItem;
    if (canReorder && comparator != null && !reorderWarningLogged) {
      LOG.warning("FlatPillList: reorder disabled while a sort order is active");
      reorderWarningLogged = true;
    }
    if (canReorder) {
      // Open-hand "grab" while hovering signals draggable. Override FlatPill's default HAND_CURSOR
      // (which it set during setInteractionMode above) so the gesture affordance is correct: open
      // hand on hover → closed fist while dragging.
      pill.setCursor(Cursors.grab());
    }

    final MouseInputAdapter handler =
        new MouseInputAdapter() {
          // Selection commits on release-without-drag, not on press. This matches Finder /
          // Explorer behavior: a click selects, a drag moves. Without this deferral, every
          // drag-press updated selection as a side effect of the press itself — and the
          // dragged pill always ended up selected even when the user just wanted to
          // rearrange. Drag activation discards the pending click; release commits it.
          private boolean pendingClick;
          private int pendingModifiers;

          @Override
          public void mousePressed(final MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            requestFocusInWindow();
            focusedVisibleIndex = index;
            pendingClick = true;
            pendingModifiers = event.getModifiersEx();
            if (canReorder) {
              initiateDrag(item, pill, event);
            }
          }

          @Override
          public void mouseDragged(final MouseEvent event) {
            if (drag == null || drag.item != item) {
              return;
            }
            if (!drag.active && hasMovedPastThreshold(event)) {
              activateDrag();
              // Drag wins over click — discard the deferred selection update.
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
                // CRITICAL: recompute the drop slot from THIS event's cursor position before
                // committing. On a "throw" (release while moving), the cursor's last sampled
                // mouseDragged position lags behind the actual release position because Swing
                // / the OS coalesce motion events and don't guarantee a mouseDragged at the
                // exact pixel of release. The release event is the only event with the
                // authoritative final cursor position, so the slot is recomputed here from it.
                refreshDropFromReleaseEvent(event);
                endDrag();
              } else {
                drag = null;
              }
            }
            if (pendingClick) {
              pendingClick = false;
              handleSelectionPress(item, index, pendingModifiers);
            }
          }
        };
    // Install on both the pill body and its inner content row so presses on the text or leading-
    // icon area participate in drag detection. The content row has its own internal listener that
    // would otherwise trap these events.
    pill.addPillMouseListener(handler);
    pill.addPillMouseMotionListener(handler);
  }

  /**
   * Renders the pin affordance on a pill in PINNED mode. INDICATOR shows the filled pin glyph only
   * on pinned items (historical behavior). BUTTON renders a clickable affordance on every pill
   * (outline glyph on unpinned, filled on pinned). NONE skips both.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void applyPinAffordance(final FlatPill pill, final T item) {
    switch (pinAffordance) {
      case NONE -> {
        // Explicit no-op; the adapter's leading icon remains whatever it set.
      }
      case INDICATOR -> {
        if (isPinned(item)) {
          // Filled glyph for "actively pinned" — same as BUTTON mode's active state. Outline =
          // inactive, filled = active, every affordance mode. Anything else makes the icon mean
          // different things depending on the affordance setting, which is exactly the
          // inconsistency we want to avoid.
          pill.setLeadingIcon(MaterialIcons.pushPinFilled());
        }
      }
      case BUTTON ->
          pill.setLeadingAffordance(
              MaterialIcons.pushPin(),
              MaterialIcons.pushPinFilled(),
              isPinned(item),
              false,
              isPinned(item) ? "Unpin" : "Pin",
              () -> togglePin(item));
      default -> {
        // exhaustive — unreachable
      }
    }
  }

  /**
   * Renders the anchor affordance on a pill in ANCHORED mode. INDICATOR shows the anchor glyph only
   * on the anchored pill (historical behavior). BUTTON renders a clickable affordance on every pill
   * — persistent filled glyph on the anchored pill, hover-revealed outline glyph on non-anchored
   * pills. NONE skips both.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void applyAnchorAffordance(final FlatPill pill, final T item) {
    switch (anchorAffordance) {
      case NONE -> {
        // Explicit no-op.
      }
      case INDICATOR -> {
        if (isAnchored(item)) {
          // Filled glyph for "actively anchored" — same convention as the pin affordance.
          pill.setLeadingIcon(MaterialIcons.anchorFilled());
        }
      }
      case BUTTON -> {
        final boolean anchored = isAnchored(item);
        pill.setLeadingAffordance(
            MaterialIcons.anchor(),
            MaterialIcons.anchorFilled(),
            anchored,
            !anchored,
            anchored ? "Remove anchor" : "Set as anchor",
            () -> {
              // Re-check live state at click time in case it changed between render and click.
              if (isAnchored(item)) {
                clearAnchor();
              } else {
                setAnchor(item);
              }
            });
      }
      default -> {
        // exhaustive — unreachable
      }
    }
  }

  // ----------------------------------------------------------------- drag

  private void initiateDrag(final T item, final FlatPill pill, final MouseEvent event) {
    drag = new DragState();
    drag.item = item;
    drag.pill = pill;
    drag.fromIndex = visibleItems.indexOf(item);
    drag.toIndex = drag.fromIndex;
    // The mouse-listener handler is attached to both pill and pill.contentRow (see
    // addPillMouseListener / addPillMouseMotionListener), so event.getComponent() may be
    // either of them. convertPoint(source, p, dest) interprets `p` in `source`'s coordinate
    // system — using pill as the source when the event originated on contentRow gives
    // wrong absolute coordinates, so always pass the actual source component.
    drag.startPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    drag.currentPoint = drag.startPoint;
    drag.grabOffset = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), pill);
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
    drag.pill.cancelPendingClick();

    displacedX.clear();
    displacedY.clear();
    for (FlatPill p : pillByItem.values()) {
      displacedX.put(p, p.getX());
      displacedY.put(p, p.getY());
    }
    recomputeDragTargets();
    content.setComponentZOrder(drag.pill, 0);
    drag.pill.setCursor(Cursors.grabbing());
    startDragAnimation();
    content.revalidate();
    content.repaint();
  }

  /**
   * Recomputes {@code drag.toIndex} using the release event's cursor position. Called from {@code
   * mouseReleased} so the committed slot reflects where the user actually let go of the pill, not
   * the slightly-stale last-sampled mouseDragged position.
   *
   * <p>Throws (release while moving fast) need this because Swing/OS event coalescing means
   * mouseDragged may have last fired several pixels before release. The release event is the only
   * one carrying the authoritative final cursor position.
   *
   * @param event the release event
   * @version v0.1.0
   * @since v0.1.0
   */
  private void refreshDropFromReleaseEvent(final MouseEvent event) {
    if (drag == null) {
      return;
    }
    final Point releasePoint =
        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    // Only recompute the slot if the cursor moved meaningfully between the last mouseDragged and
    // mouseReleased. For a careful drop (user hovers, then lets go), the release cursor will be
    // within DRAG_THRESHOLD pixels of the last sampled drag cursor — the existing toIndex is
    // already what the animation is showing the user, so committing it directly is more
    // intuitive than recomputing from a release cursor that may snap-differ by one pixel
    // (e.g., crossing a cell boundary) and producing a surprise slot. For a throw (release
    // while still moving), the cursor will have moved past the threshold and the recompute
    // gives us the authoritative final position the event-coalescing pipeline lost.
    final int dx = releasePoint.x - drag.currentPoint.x;
    final int dy = releasePoint.y - drag.currentPoint.y;
    final boolean cursorMovedSignificantly = dx * dx + dy * dy > DRAG_THRESHOLD * DRAG_THRESHOLD;
    if (!cursorMovedSignificantly) {
      return;
    }
    drag.currentPoint = releasePoint;
    final Point anchor = computeDropAnchor();
    final int finalSlot = computeDropIndex(anchor);
    drag.toIndex = clampSlotToPartition(finalSlot, drag.item);
  }

  private void continueDrag(final MouseEvent event) {
    if (drag == null) {
      return;
    }
    drag.currentPoint =
        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    final Point dropAnchor = computeDropAnchor();
    final int rawDrop = computeDropIndex(dropAnchor);
    // Clamp to dragged item's partition so visual feedback during the drag matches what the drop
    // will commit. Without this the user sees unpinned items shuffle as a pinned pill drags over
    // them, only to snap back on release.
    final int newDrop = clampSlotToPartition(rawDrop, drag.item);
    if (newDrop != drag.toIndex) {
      drag.toIndex = newDrop;
      recomputeDragTargets();
    }
    content.revalidate();
    content.repaint();
  }

  /**
   * Computes the drop-slot anchor from {@code drag.currentPoint} according to the active
   * orientation's needs.
   *
   * <ul>
   *   <li><strong>HORIZONTAL / VERTICAL</strong>: leading-edge anchor — the dragged pill's top-left
   *       corner. When dragging a wide pill over narrow neighbors, the swap fires as soon as the
   *       leading edge crosses the target's midline, rather than waiting for the cursor (which is
   *       somewhere inside the wider pill) to do so.
   *   <li><strong>GRID</strong>: dragged-pill center — natural for 2D uniform-cell layouts.
   *   <li><strong>WRAP</strong>: bare cursor. Leading-edge anchoring breaks WRAP because the
   *       anchor.y depends on grabOffset.y, which routinely lands the y component <em>above</em>
   *       row 0 when the user grabs near the pill's bottom (and similarly anchor.x lands far left
   *       when the user grabs the pill's right edge). The cursor itself doesn't have that problem:
   *       it's always in the user's row at the user's column.
   * </ul>
   *
   * @return the anchor point used by {@code computeDropIndex}
   * @version v0.1.0
   * @since v0.1.0
   */
  private Point computeDropAnchor() {
    final int draggedLeftX = drag.currentPoint.x - drag.grabOffset.x;
    final int draggedTopY = drag.currentPoint.y - drag.grabOffset.y;
    return switch (orientation) {
      case GRID ->
          new Point(
              draggedLeftX + drag.pill.getWidth() / 2, draggedTopY + drag.pill.getHeight() / 2);
      case WRAP -> new Point(drag.currentPoint);
      case HORIZONTAL, VERTICAL -> new Point(draggedLeftX, draggedTopY);
    };
  }

  /**
   * Computes the drop slot for the cursor's current position, choosing the strategy that matches
   * the active orientation. The result is a visible-item index in the range {@code [0, size]};
   * end-of-list is represented by {@code size}.
   *
   * <p>Every strategy walks the <strong>natural</strong> (preferred-size-based) positions of the
   * non-dragged pills rather than reading {@code getX()/getY()}. The latter return mid-animation
   * displaced positions and create a feedback loop: a small pill covered by a larger dragged pill
   * is partway through its slide, the drop calculation observes the in-progress position,
   * recomputes targets, the slide accelerates, and the answer oscillates. Natural positions are
   * stable — the slot the cursor maps to depends only on the cursor.
   *
   * @version v0.1.0
   * @since v0.1.0
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
    for (T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final FlatPill p = pillByItem.get(item);
      if (p == null) {
        continue;
      }
      final int h = p.getPreferredSize().height;
      final int mid = y + h / 2;
      if (point.y > mid) {
        slot++;
      }
      y += h + itemGap;
    }
    return slot;
  }

  private int dropIndexHorizontal(final Point point) {
    final Insets in = content.getInsets();
    int slot = 0;
    int x = in.left;
    for (T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final FlatPill p = pillByItem.get(item);
      if (p == null) {
        continue;
      }
      final int w = p.getPreferredSize().width;
      final int mid = x + w / 2;
      if (point.x > mid) {
        slot++;
      }
      x += w + itemGap;
    }
    return slot;
  }

  /**
   * For WRAP, recreates the natural row-break layout for the non-dragged sequence. The drop slot
   * advances on every center the cursor has passed in reading order (rows top-to-bottom, within a
   * row left-to-right). Cursor below the bottom of a row counts as past every pill on that row.
   *
   * <p>{@code anchor.y} is clamped to {@code in.top} so cursor drifts <em>above</em> the component
   * (negative y) don't silently fail both per-pill checks — without the clamp, {@code anchor.y >=
   * y} (within-row) and {@code anchor.y > rowBottom} (below-row) would both be false for every
   * pill, leaving slot stuck at 0 regardless of {@code anchor.x}. Drifts below the component are
   * already handled correctly by the existing below-row branch firing for every row.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private int dropIndexWrap(final Point point) {
    final Insets in = content.getInsets();
    final int maxRight = Math.max(in.left, content.getWidth() - in.right);
    final int anchorY = Math.max(in.top, point.y);
    int slot = 0;
    int x = in.left;
    int y = in.top;
    int rowH = 0;
    for (T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final FlatPill p = pillByItem.get(item);
      if (p == null) {
        continue;
      }
      final Dimension pref = p.getPreferredSize();
      if (x > in.left && x + pref.width > maxRight) {
        x = in.left;
        y += rowH + itemGap;
        rowH = 0;
      }
      final int midX = x + pref.width / 2;
      final int rowBottom = y + Math.max(pref.height, rowH);
      if (anchorY > rowBottom) {
        slot++;
      } else if (anchorY >= y && point.x > midX) {
        slot++;
      }
      x += pref.width + itemGap;
      rowH = Math.max(rowH, pref.height);
    }
    return slot;
  }

  /**
   * For GRID, drop slot is the cell coordinate the anchor sits in.
   *
   * <p>Cell dimensions are read from the rendered bounds of the non-dragged pills (which the layout
   * uniformly stretches to cellW × cellH), not from {@code content.getWidth()} divided by column
   * count. The container-derived formula was occasionally producing huge cellW values — apparently
   * when {@code content.getWidth()} was inflated by the dragged pill's wide drag mode or by a stale
   * layout — and pushing every anchor into col=0, manifesting as random "move to front" drops.
   * Reading the actual rendered widths sidesteps that entirely.
   *
   * <p>{@code row} is clamped to the last occupied row so a far-past-the-last-row anchor doesn't
   * compute idx > nonDraggedCount and get pinned to the end.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private int dropIndexGrid(final Point point) {
    final Insets in = content.getInsets();
    int actualCellW = 0;
    int actualCellH = 0;
    int nonDraggedCount = 0;
    for (T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final FlatPill p = pillByItem.get(item);
      if (p == null) {
        continue;
      }
      actualCellW = Math.max(actualCellW, p.getWidth());
      actualCellH = Math.max(actualCellH, p.getHeight());
      nonDraggedCount++;
    }
    // Fallback to preferred sizes if bounds haven't been set (e.g., drag started before first
    // paint — unlikely in practice but defensive).
    if (actualCellW <= 0 || actualCellH <= 0) {
      for (T item : visibleItems) {
        if (item == drag.item) {
          continue;
        }
        final FlatPill p = pillByItem.get(item);
        if (p == null) {
          continue;
        }
        actualCellW = Math.max(actualCellW, p.getPreferredSize().width);
        actualCellH = Math.max(actualCellH, p.getPreferredSize().height);
      }
    }
    if (actualCellW <= 0) {
      actualCellW = 100;
    }
    if (actualCellH <= 0) {
      actualCellH = 24;
    }
    final int cols = Math.max(1, columns);
    final int maxRow = Math.max(0, (nonDraggedCount + cols - 1) / cols - 1);
    final int col = Math.max(0, Math.min(cols - 1, (point.x - in.left) / (actualCellW + itemGap)));
    final int row = Math.max(0, Math.min(maxRow, (point.y - in.top) / (actualCellH + itemGap)));
    final int idx = row * cols + col;
    final int slot = Math.max(0, Math.min(nonDraggedCount, idx));
    return slot;
  }

  /**
   * Recomputes target X/Y for every non-dragged pill, based on the order produced by removing the
   * dragged pill and reinserting it at {@code drag.toIndex}. The animation timer then interpolates
   * each pill toward its target.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void recomputeDragTargets() {
    targetX.clear();
    targetY.clear();
    final List<FlatPill> ordered = new ArrayList<>();
    for (T item : visibleItems) {
      final FlatPill p = pillByItem.get(item);
      if (p != null) {
        ordered.add(p);
      }
    }
    ordered.remove(drag.pill);
    final int clamp = Math.max(0, Math.min(ordered.size(), drag.toIndex));
    ordered.add(clamp, drag.pill);

    final Insets in = content.getInsets();
    switch (orientation) {
      case VERTICAL -> {
        int y = in.top;
        for (FlatPill p : ordered) {
          targetX.put(p, in.left);
          targetY.put(p, y);
          y += p.getPreferredSize().height + itemGap;
        }
      }
      case HORIZONTAL -> {
        int x = in.left;
        int rowH = 0;
        for (FlatPill p : ordered) {
          rowH = Math.max(rowH, p.getPreferredSize().height);
        }
        for (FlatPill p : ordered) {
          targetX.put(p, x);
          targetY.put(p, in.top);
          x += p.getPreferredSize().width + itemGap;
        }
      }
      case WRAP -> {
        final int maxRight = Math.max(in.left, content.getWidth() - in.right);
        int x = in.left;
        int y = in.top;
        int rowH = 0;
        for (FlatPill p : ordered) {
          final Dimension pref = p.getPreferredSize();
          if (x > in.left && x + pref.width > maxRight) {
            x = in.left;
            y += rowH + itemGap;
            rowH = 0;
          }
          targetX.put(p, x);
          targetY.put(p, y);
          x += pref.width + itemGap;
          rowH = Math.max(rowH, pref.height);
        }
      }
      case GRID -> {
        final int availW = Math.max(1, content.getWidth() - in.left - in.right);
        final int cellW = Math.max(1, (availW - (columns - 1) * itemGap) / columns);
        int cellH = 0;
        for (FlatPill p : ordered) {
          cellH = Math.max(cellH, p.getPreferredSize().height);
        }
        if (cellH == 0) {
          cellH = 24;
        }
        for (int i = 0; i < ordered.size(); i++) {
          final FlatPill p = ordered.get(i);
          final int col = i % columns;
          final int row = i / columns;
          targetX.put(p, in.left + col * (cellW + itemGap));
          targetY.put(p, in.top + row * (cellH + itemGap));
        }
      }
      default -> {
        // exhaustive — unreachable
      }
    }
  }

  private void startDragAnimation() {
    if (dragAnimTimer != null && dragAnimTimer.isRunning()) {
      dragAnimTimer.stop();
    }
    dragAnimTimer =
        new Timer(
            ANIMATION_TICK_MS,
            e -> {
              if (drag == null) {
                ((Timer) e.getSource()).stop();
                return;
              }
              boolean moved = false;
              for (Map.Entry<FlatPill, Integer> entry : targetY.entrySet()) {
                final FlatPill p = entry.getKey();
                if (p == drag.pill) {
                  continue;
                }
                if (animateAxis(p, entry.getValue(), displacedY, p.getY())) {
                  moved = true;
                }
                final Integer tx = targetX.get(p);
                if (tx != null && animateAxis(p, tx, displacedX, p.getX())) {
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

  private static boolean animateAxis(
      final FlatPill pill, final int target, final Map<FlatPill, Integer> map, final int fallback) {
    final Integer curBoxed = map.get(pill);
    final int cur = curBoxed != null ? curBoxed : fallback;
    if (cur == target) {
      return false;
    }
    final int diff = target - cur;
    int step = (int) Math.round(diff * 0.30);
    if (step == 0 || Math.abs(diff) <= 1) {
      step = diff;
    }
    map.put(pill, cur + step);
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
    // Flip the dragged pill back to the open-hand "grab" cursor. On a successful move, the
    // subsequent rebuildContent runs configurePillForList over fresh pills and sets grab() there
    // too — but on noop paths the original pill instance is reused, so restoring it here is what
    // makes the cursor switch back as soon as the user releases.
    drag.pill.setCursor(Cursors.grab());
    final int fromVis = drag.fromIndex;
    final int toVis = drag.toIndex;
    final T item = drag.item;
    drag = null;
    if (fromVis < 0 || toVis < 0) {
      restoreContentOrder();
      return;
    }
    if (model instanceof DefaultPillListModel<T> mutable) {
      final int fromModel = indexOfInModel(item);
      // toVis is in *slot space* (0..non_dragged_count), not in visible-list index space.
      // Without partition, visible-order == model-order so the slot is the model index directly
      // (ArrayList.move semantics: post-removal). With a pin partition active, the visible order
      // diverges from model order, so we clamp the slot to the dragged item's partition first,
      // then translate to a model index via the neighbor-at-slot.
      final int nonDraggedCount = visibleItems.size() - 1;
      final int slotClamped = Math.max(0, Math.min(nonDraggedCount, toVis));
      final int partitionClamped = clampSlotToPartition(slotClamped, item);
      // Partition-aware visible→model translation runs whenever visible order can diverge from
      // model order: PINNED with a predicate, or ANCHORED with a predicate. Otherwise the slot
      // is already a model index (post-removal).
      final boolean partitionActive =
          (movementMode == MovementMode.PINNED && pinPredicate != null)
              || (movementMode == MovementMode.ANCHORED && anchorPredicate != null);
      final int toModel =
          partitionActive
              ? translateVisibleSlotToModelIndex(partitionClamped, item)
              : partitionClamped;
      if (fromModel < 0 || toModel < 0 || fromModel == toModel) {
        restoreContentOrder();
        return;
      }
      if (toModel >= model.getSize()) {
        restoreContentOrder();
        return;
      }
      // mutable.move fires a MOVED event which triggers onModelChanged → rebuildContent, so the
      // component order is reset by that path. No explicit restoreContentOrder() needed here.
      mutable.move(fromModel, toModel);
      fireReorder(item, fromModel, toModel);
    } else {
      LOG.warning("FlatPillList: reorder requires a mutable DefaultPillListModel; drop ignored");
      restoreContentOrder();
    }
  }

  /**
   * Clamps a post-removal visible slot into the dragged item's pin-partition. Pinned items can only
   * land in [0..pinnedCountAfterRemoval]; unpinned items can only land in
   * [pinnedCountAfterRemoval..size]. With no pin predicate this is a no-op pass-through.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private int clampSlotToPartition(final int slotVis, final T draggedItem) {
    if (movementMode == MovementMode.ANCHORED && anchorPredicate != null) {
      // Anchored item is drag-blocked upstream, so this branch only runs for non-anchor items.
      // If an anchor is present in the visible list, the non-anchor partition starts at slot 1
      // (post-removal indexing) — clamp accordingly. If no anchor is currently shown, no clamp
      // is needed.
      if (getAnchoredItem() == null) {
        return slotVis;
      }
      final int postRemovalSize = visibleItems.size() - 1;
      return Math.max(1, Math.min(slotVis, postRemovalSize));
    }
    if (movementMode != MovementMode.PINNED || pinPredicate == null) {
      return slotVis;
    }
    final int draggedPinnedAdj = isPinned(draggedItem) ? 1 : 0;
    final int pinnedCountAfter = countPinnedVisible() - draggedPinnedAdj;
    final int postRemovalSize = visibleItems.size() - 1;
    if (isPinned(draggedItem)) {
      return Math.max(0, Math.min(slotVis, pinnedCountAfter));
    }
    return Math.max(pinnedCountAfter, Math.min(slotVis, postRemovalSize));
  }

  /**
   * Translates a post-removal visible slot to a post-removal model index when a pin predicate is
   * active. Returns the model index where the item should land such that, on the next rebuild, it
   * sits at the requested visible slot. Strategy: identify the neighbor that should follow the
   * dragged item in the new visible order, and return that neighbor's post-removal model index.
   * When the slot is past the last item, returns the post-removal end-of-model index.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private int translateVisibleSlotToModelIndex(final int slotVis, final T draggedItem) {
    final List<T> postVis = new ArrayList<>(visibleItems);
    postVis.remove(draggedItem);
    if (slotVis >= postVis.size()) {
      return Math.max(0, model.getSize() - 1);
    }
    final T neighbor = postVis.get(slotVis);
    int idx = 0;
    final Iterator<T> it = model.iterator();
    while (it.hasNext()) {
      final T item = it.next();
      if (item == draggedItem) {
        continue;
      }
      if (item == neighbor) {
        return idx;
      }
      idx++;
    }
    return Math.max(0, model.getSize() - 1);
  }

  private int countPinnedVisible() {
    if (movementMode != MovementMode.PINNED || pinPredicate == null) {
      return 0;
    }
    int n = 0;
    for (T item : visibleItems) {
      if (pinPredicate.test(item)) {
        n++;
      }
    }
    return n;
  }

  /**
   * Restores the child component order of content to match {@code visibleItems}, then revalidates
   * and repaints. Required after a no-op drag-drop because {@code activateDrag} calls {@code
   * content.setComponentZOrder(drag.pill, 0)} to bring the dragged pill to the visual front during
   * drag — which also reorders the children array. The idle layout (e.g., GridLayoutImpl) places
   * children by their index, so a scrambled child order after a noop drag visually places the
   * dragged pill at row 0 / col 0 even though the model is untouched. Successful drops sidestep
   * this because model.move → rebuildContent rebuilds the child list from scratch in visible order.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void restoreContentOrder() {
    int targetIndex = 0;
    for (T item : visibleItems) {
      final FlatPill pill = pillByItem.get(item);
      if (pill == null) {
        continue;
      }
      // setComponentZOrder is a no-op if the component is already at the desired index, so this
      // is safe to call even when nothing actually changed.
      if (pill.getParent() == content) {
        content.setComponentZOrder(pill, targetIndex);
        targetIndex++;
      }
    }
    content.revalidate();
    content.repaint();
  }

  private int indexOfInModel(final T item) {
    int i = 0;
    for (T t : model) {
      if (t == item || (t != null && t.equals(item))) {
        return i;
      }
      i++;
    }
    return -1;
  }

  /** Tracks an in-progress drag. */
  private final class DragState {
    T item;
    FlatPill pill;
    int fromIndex;
    int toIndex;
    Point startPoint;
    Point currentPoint;
    Point grabOffset;
    boolean active;
  }

  // ---------------------------------------------------------------- model

  /**
   * Convenience for selecting an item programmatically (no event mutation).
   *
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  protected void selectItem(final T item) {
    selectionModel.setSelected(List.of(item));
    syncSelectionVisuals();
  }

  // ------------------------------------------------------------- selection

  private void handleSelectionPress(final T item, final int index, final int modifiers) {
    if (selectionMode == PillSelectionMode.NONE) {
      return;
    }
    final boolean shift = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
    final int meta = InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK;
    final boolean toggle = (modifiers & meta) != 0;

    if (selectionMode == PillSelectionMode.SINGLE_MANDATORY) {
      // Click selects; click on the already-selected pill is a no-op (mandatory: never zero).
      if (!selectionModel.isSelected(item)) {
        selectionModel.setSelected(List.of(item));
        selectionAnchor = item;
      }
    } else if (selectionMode == PillSelectionMode.SINGLE) {
      // Toggleable single: click selects, click again deselects (filter-chip semantics).
      if (selectionModel.isSelected(item)) {
        selectionModel.clearSelection();
      } else {
        selectionModel.setSelected(List.of(item));
        selectionAnchor = item;
      }
    } else if (shift && selectionAnchor != null) {
      final int anchorIdx = visibleItems.indexOf(selectionAnchor);
      if (anchorIdx >= 0) {
        final int from = Math.min(anchorIdx, index);
        final int to = Math.max(anchorIdx, index);
        final List<T> range = new ArrayList<>(visibleItems.subList(from, to + 1));
        selectionModel.setSelected(range);
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
    for (Map.Entry<T, FlatPill> entry : pillByItem.entrySet()) {
      entry.getValue().setSelected(selectionModel.isSelected(entry.getKey()));
    }
  }

  /**
   * Fires a reorder event. Called by drag machinery added in #239.
   *
   * @param item the moved item
   * @param from source index in the model
   * @param to destination index in the model
   * @version v0.1.0
   * @since v0.1.0
   */
  protected void fireReorder(final T item, final int from, final int to) {
    final PillReorderEvent<T> evt = new PillReorderEvent<>(this, item, from, to);
    for (PillReorderListener<T> l : new ArrayList<>(reorderListeners)) {
      l.pillReordered(evt);
    }
  }

  // ---------------------------------------------------------- placeholders

  /** Inner content panel used by the layout managers. */
  private static final class ContentPanel extends JPanel {
    ContentPanel() {
      super();
      setOpaque(false);
    }
  }

  /** Vertical stack layout — pills sized to preferred height, stretched to fill width. */
  private final class VerticalLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // no-op
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // no-op
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Insets in = parent.getInsets();
      int total = in.top + in.bottom;
      int width = 0;
      final int count = parent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Dimension pref = parent.getComponent(i).getPreferredSize();
        total += pref.height;
        if (i + 1 < count) {
          total += itemGap;
        }
        width = Math.max(width, pref.width);
      }
      return new Dimension(width + in.left + in.right, total);
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
        final Component c = parent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        c.setBounds(in.left, y, pref.width, pref.height);
        y += pref.height + itemGap;
      }
    }
  }

  /**
   * Single-row horizontal layout. Pills are placed left-to-right at their preferred width and a
   * shared row height (the max of all pill preferred heights). If the total preferred width exceeds
   * the container, pills are simply clipped on the right — the host is expected to wrap the list in
   * a {@link javax.swing.JScrollPane} for the clip+scroll overflow strategy. An ellipsis-menu
   * overflow strategy is deferred to a future story.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class HorizontalLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // no-op
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // no-op
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Insets in = parent.getInsets();
      int totalW = in.left + in.right;
      int rowH = 0;
      final int count = parent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Dimension pref = parent.getComponent(i).getPreferredSize();
        totalW += pref.width;
        if (i + 1 < count) {
          totalW += itemGap;
        }
        rowH = Math.max(rowH, pref.height);
      }
      return new Dimension(totalW, rowH + in.top + in.bottom);
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
      int x = in.left;
      int rowH = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        rowH = Math.max(rowH, parent.getComponent(i).getPreferredSize().height);
      }
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component c = parent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        c.setBounds(x, in.top, pref.width, rowH);
        x += pref.width + itemGap;
      }
    }
  }

  /**
   * Multi-row wrapping layout — a {@link java.awt.FlowLayout}-derivative that breaks to a new row
   * when the container width is exhausted, and respects the configured {@code itemGap} for both row
   * and column spacing. Drag-reorder across row breaks lands in #239.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class WrapLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // no-op
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // no-op
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
     * Single pass that both measures and (optionally) lays out. The {@code apply} flag separates
     * the two modes so the layout manager honors the standard contract without computing twice.
     *
     * @version v0.1.0
     * @since v0.1.0
     */
    private Dimension layoutCore(
        final Container parent, final int availWidth, final boolean apply) {
      final Insets in = parent.getInsets();
      final int maxRight = Math.max(in.left, availWidth - in.right);
      int x = in.left;
      int y = in.top;
      int rowH = 0;
      int contentMaxX = in.left;
      final int count = parent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Component c = parent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        if (x > in.left && x + pref.width > maxRight) {
          // wrap to a new row
          x = in.left;
          y += rowH + itemGap;
          rowH = 0;
        }
        if (apply) {
          c.setBounds(x, y, pref.width, pref.height);
        }
        x += pref.width + itemGap;
        rowH = Math.max(rowH, pref.height);
        contentMaxX = Math.max(contentMaxX, x - itemGap);
      }
      return new Dimension(contentMaxX + in.right, y + rowH + in.bottom);
    }
  }

  /**
   * N-column grid layout with uniform cell width and uniform cell height. Mirrors the geometry of
   * {@link com.owspfm.ui.components.card.list.FlatCardList}'s grid mode, but for pill-sized
   * children.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class GridLayoutImpl implements LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // no-op
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // no-op
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
      final int count = parent.getComponentCount();
      final int rows = (count + columns - 1) / Math.max(1, columns);
      final int cellH = cellHeight(parent);
      final int cellW = cellWidth(parent);
      return new Dimension(
          cellW * columns + itemGap * Math.max(0, columns - 1) + in.left + in.right,
          cellH * rows + itemGap * Math.max(0, rows - 1) + in.top + in.bottom);
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
      final int availW = Math.max(0, parent.getWidth() - in.left - in.right);
      final int cellW = Math.max(1, (availW - (columns - 1) * itemGap) / columns);
      final int cellH = cellHeight(parent);
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component c = parent.getComponent(i);
        final int col = i % columns;
        final int row = i / columns;
        c.setBounds(
            in.left + col * (cellW + itemGap), in.top + row * (cellH + itemGap), cellW, cellH);
      }
    }
  }

  /**
   * Shared drag-mode positioning: dragged pill follows the cursor (with grab offset compensation
   * and clamping to the content bounds); every other pill reads its current animated position from
   * the per-axis displaced maps. All four layouts delegate here while a drag is active.
   *
   * <p>Pill <em>dimensions</em> during drag mirror the orientation's static layout so the visual
   * doesn't snap on drag start. For VERTICAL / HORIZONTAL / WRAP the static layout uses each pill's
   * preferred size, so drag mode reuses that. For GRID the static layout stretches pills to a
   * uniform cellW × cellH; drag mode preserves the stretch — including for the dragged pill — so
   * the user sees no size jump between idle and dragging.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void layoutDuringDrag(final Container parent) {
    final Insets in = parent.getInsets();
    final boolean isGrid = orientation == FlatListOrientation.GRID;
    final int gridCellW;
    final int gridCellH;
    if (isGrid) {
      final int availW = Math.max(0, parent.getWidth() - in.left - in.right);
      gridCellW = Math.max(1, (availW - (columns - 1) * itemGap) / columns);
      int maxH = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        maxH = Math.max(maxH, parent.getComponent(i).getPreferredSize().height);
      }
      gridCellH = maxH > 0 ? maxH : 24;
    } else {
      gridCellW = 0;
      gridCellH = 0;
    }
    for (int i = 0; i < parent.getComponentCount(); i++) {
      final Component c = parent.getComponent(i);
      final Dimension pref = c.getPreferredSize();
      final int w = isGrid ? gridCellW : pref.width;
      final int h = isGrid ? gridCellH : pref.height;
      if (c == drag.pill) {
        int x = drag.currentPoint.x - drag.grabOffset.x;
        int y = drag.currentPoint.y - drag.grabOffset.y;
        final int maxX = Math.max(in.left, parent.getWidth() - in.right - w);
        final int maxY = Math.max(in.top, parent.getHeight() - in.bottom - h);
        x = Math.max(in.left, Math.min(maxX, x));
        y = Math.max(in.top, Math.min(maxY, y));
        c.setBounds(x, y, w, h);
      } else {
        final Integer dispX = displacedX.get(c);
        final Integer dispY = displacedY.get(c);
        final int x = dispX != null ? dispX : in.left;
        final int y = dispY != null ? dispY : in.top;
        c.setBounds(x, y, w, h);
      }
    }
  }

  private JComponent defaultEmptyState() {
    final JLabel lbl = new JLabel("No items to show", SwingConstants.CENTER);
    lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
    lbl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    return lbl;
  }

  private JComponent defaultLoading() {
    final JLabel lbl = new JLabel("Loading…", SwingConstants.CENTER);
    lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
    lbl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    return lbl;
  }

  // -------------------------------------------------------------- keyboard

  private void installKeyboard() {
    final InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap am = getActionMap();

    final Action up = new MoveFocus(-1);
    final Action down = new MoveFocus(1);
    final Action home = new JumpFocus(0);
    final Action end = new JumpFocus(Integer.MAX_VALUE);
    final Action activate = new ActivateFocused();
    final Action selectAll = new SelectAllAction();

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "flatpill.up");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "flatpill.down");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "flatpill.up");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "flatpill.down");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "flatpill.home");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "flatpill.end");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "flatpill.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "flatpill.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "flatpill.selectAll");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), "flatpill.selectAll");

    am.put("flatpill.up", up);
    am.put("flatpill.down", down);
    am.put("flatpill.home", home);
    am.put("flatpill.end", end);
    am.put("flatpill.activate", activate);
    am.put("flatpill.selectAll", selectAll);
  }

  private void moveFocus(final int delta) {
    if (visibleItems.isEmpty()) {
      return;
    }
    int next = focusedVisibleIndex + delta;
    if (next < 0) {
      next = 0;
    }
    if (next >= visibleItems.size()) {
      next = visibleItems.size() - 1;
    }
    focusedVisibleIndex = next;
    final FlatPill pill = pillByItem.get(visibleItems.get(next));
    if (pill != null) {
      pill.requestFocusInWindow();
      scrollRectToVisible(pill.getBounds());
    }
  }

  private final class MoveFocus extends AbstractAction {
    private final int delta;

    MoveFocus(final int delta) {
      super();
      this.delta = delta;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      moveFocus(delta);
    }
  }

  private final class JumpFocus extends AbstractAction {
    private final int target;

    JumpFocus(final int target) {
      super();
      this.target = target;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      focusedVisibleIndex = -1;
      moveFocus(target == 0 ? 0 : visibleItems.size());
    }
  }

  private final class ActivateFocused extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent event) {
      if (focusedVisibleIndex < 0 || focusedVisibleIndex >= visibleItems.size()) {
        return;
      }
      final T item = visibleItems.get(focusedVisibleIndex);
      handleSelectionPress(item, focusedVisibleIndex, 0);
    }
  }

  private final class SelectAllAction extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent event) {
      if (selectionMode != PillSelectionMode.MULTIPLE) {
        return;
      }
      selectionModel.setSelected(new ArrayList<>(visibleItems));
      syncSelectionVisuals();
    }
  }

  // ------------------------------------------------------------ LAF / a11y

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    repaint();
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleFlatPillList();
    }
    return accessibleContext;
  }

  /** Accessible role of the list itself; rendered pills expose their own roles. */
  protected class AccessibleFlatPillList extends AccessibleJPanel {
    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.LIST;
    }

    @Override
    public int getAccessibleChildrenCount() {
      return visibleItems.size();
    }

    @Override
    public javax.accessibility.Accessible getAccessibleChild(final int index) {
      if (index < 0 || index >= visibleItems.size()) {
        return null;
      }
      final FlatPill pill = pillByItem.get(visibleItems.get(index));
      return pill;
    }
  }
}
