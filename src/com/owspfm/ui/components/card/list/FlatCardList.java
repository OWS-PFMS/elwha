package com.owspfm.ui.components.card.list;

import com.owspfm.ui.components.card.CardInteractionMode;
import com.owspfm.ui.components.card.FlatCard;
import com.owspfm.ui.components.flatlist.FlatList;
import com.owspfm.ui.components.flatlist.FlatListOrientation;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

/**
 * A reusable, observable, model-driven list of {@link FlatCard}s.
 *
 * <p>Drop-in replacement for the hand-rolled {@code JPanel + BoxLayout + listener} scaffolding that
 * tends to accumulate around card-heavy surfaces. Caller provides a {@link CardListModel} and a
 * {@link CardAdapter}; the list handles selection, drag-to-reorder, filter, sort, empty/loading
 * states, animations, keyboard navigation, and theme-aware repaints.
 *
 * <p><strong>Quick start</strong>:
 *
 * <pre>{@code
 * DefaultCardListModel<Cycle> model = new DefaultCardListModel<>(cycles);
 * CardAdapter<Cycle> adapter = (cycle, idx) ->
 *     new FlatCard()
 *         .setHeader("Cycle #" + (idx + 1), cycle.summary())
 *         .setBody(buildCycleBody(cycle));
 *
 * FlatCardList<Cycle> list = new FlatCardList<>(model, adapter)
 *     .setSelectionMode(CardSelectionMode.SINGLE)
 *     .setReorderable(true);
 * list.addReorderListener(evt ->
 *     System.out.println(evt.getItem() + ": " + evt.getFromIndex() + " -> " + evt.getToIndex()));
 * }</pre>
 *
 * <p>This component is theme-aware: its colors and corner radius derive from FlatLaf {@link
 * UIManager} keys, so it tracks light/dark theme switches without caller intervention.
 *
 * <p>This package has no dependencies on application code; it can be lifted into its own library by
 * moving the {@code com.owspfm.ui.components.card.list} (and {@code .card}) directories.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class FlatCardList<T> extends JPanel implements Accessible, FlatList<T> {

  /**
   * Aliased reference to the shared {@link FlatListOrientation} enum (extracted in story #237).
   *
   * <p>This {@code FlatCardList.Orientation} symbol existed prior to the shared-abstraction
   * extraction and only ever shipped {@code VERTICAL} and {@code GRID}. It is preserved as an alias
   * so existing call sites such as {@code FlatCardList.Orientation.GRID} continue to compile;
   * however, all new code should reference {@link FlatListOrientation} directly. Story #242 added
   * support for {@link FlatListOrientation#HORIZONTAL} and {@link FlatListOrientation#WRAP}, which
   * are also reachable through this class now.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final class Orientation {
    /** Single-column vertical stack (default). */
    public static final FlatListOrientation VERTICAL = FlatListOrientation.VERTICAL;

    /** N-column grid (set with {@link FlatCardList#setColumns(int)}). */
    public static final FlatListOrientation GRID = FlatListOrientation.GRID;

    private Orientation() {
      // alias holder — instances are not constructed
    }

    /**
     * Returns the orientation values historically supported by {@code FlatCardList} — {@code
     * [VERTICAL, GRID]} — for callers that need to iterate them. New code should iterate {@link
     * FlatListOrientation#values()} directly.
     *
     * @return the legacy two-value array
     * @version v0.1.0
     * @since v0.1.0
     */
    public static FlatListOrientation[] values() {
      return new FlatListOrientation[] {VERTICAL, GRID};
    }
  }

  private static final Logger LOG = Logger.getLogger(FlatCardList.class.getName());

  private static final int DEFAULT_GAP = 8;
  private static final int DEFAULT_COLUMNS = 2;
  private static final int DEFAULT_ANIMATION_MS = 180;
  private static final int ANIMATION_TICK_MS = 16;

  /** Pixels the cursor must travel from press before a drag is considered activated. */
  private static final int DRAG_THRESHOLD = 5;

  // Backing model + adapter ------------------------------------------------
  private final CardListModel<T> model;
  private final CardAdapter<T> adapter;
  private final CardListDataListener modelListener = this::onModelChanged;
  private final CardSelectionModel<T> selectionModel = new DefaultCardSelectionModel<>();

  // Configuration ----------------------------------------------------------
  private FlatListOrientation orientation = FlatListOrientation.VERTICAL;
  private int columns = DEFAULT_COLUMNS;
  private int itemGap = DEFAULT_GAP;
  private Insets listPadding = new Insets(DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP);
  private CardSelectionMode selectionMode = CardSelectionMode.NONE;
  private boolean reorderable;
  private ReorderHandle reorderHandle = ReorderHandle.WHOLE_CARD;
  private Predicate<T> filter;
  private Comparator<T> comparator;
  private JComponent emptyState;
  private JComponent loadingComponent;
  private boolean loading;
  private boolean animateChanges;
  private int animationDurationMs = DEFAULT_ANIMATION_MS;
  private boolean reorderWarningLogged;

  // Listeners --------------------------------------------------------------
  private final List<CardReorderListener<T>> reorderListeners = new ArrayList<>();

  // Render cache: visible items in render order ----------------------------
  private final List<T> visibleItems = new ArrayList<>();
  private final Map<T, FlatCard> cardByItem = new LinkedHashMap<>();
  private int focusedVisibleIndex = -1;
  private T anchorItem;

  // Layout panels ----------------------------------------------------------
  private final JPanel content;
  private final JPanel emptyHolder;
  private final JPanel loadingHolder;

  // Drag state -------------------------------------------------------------
  private DragState drag;
  private final Map<FlatCard, Integer> displacedY = new HashMap<>();
  private final Map<FlatCard, Integer> targetY = new HashMap<>();
  private final Map<FlatCard, Integer> displacedX = new HashMap<>();
  private final Map<FlatCard, Integer> targetX = new HashMap<>();
  private Timer dragAnimTimer;

  // Animation state --------------------------------------------------------
  private Timer fadeTimer;
  private float fadeAlpha = 1f;

  // ------------------------------------------------------------------ ctor

  /**
   * Builds a list bound to the given model and adapter.
   *
   * @param model the backing model (required)
   * @param adapter the adapter that maps items to cards (required)
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCardList(final CardListModel<T> model, final CardAdapter<T> adapter) {
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

    model.addCardListDataListener(modelListener);
    rebuildVisibleItems();
    rebuildContent(false);

    installKeyboard();
  }

  // ------------------------------------------------------------- public API

  /**
   * Returns the backing model.
   *
   * @return the model
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardListModel<T> getModel() {
    return model;
  }

  /**
   * Returns the selection model. Always non-null even when selection is disabled.
   *
   * @return the selection model
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardSelectionModel<T> getSelectionModel() {
    return selectionModel;
  }

  /**
   * Sets the layout orientation.
   *
   * @param orientation one of {@link FlatListOrientation}; null is ignored. All four orientations
   *     are supported as of story #242.
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatCardList<T> setOrientation(final FlatListOrientation orientation) {
    if (orientation == null || orientation == this.orientation) {
      return this;
    }
    this.orientation = orientation;
    applyOrientationLayout();
    rebuildContent(false);
    return this;
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
   * Sets the column count for {@link FlatListOrientation#GRID}.
   *
   * @param columns column count, clamped to {@code >= 1}
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatCardList<T> setColumns(final int columns) {
    final int v = Math.max(1, columns);
    if (v == this.columns) {
      return this;
    }
    this.columns = v;
    if (orientation == FlatListOrientation.GRID) {
      applyOrientationLayout();
      rebuildContent(false);
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
   * Sets the gap between rendered cards (vertical stack: between rows; grid: between cells).
   *
   * @param gap pixels, clamped to {@code >= 0}
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatCardList<T> setItemGap(final int gap) {
    final int v = Math.max(0, gap);
    if (v == itemGap) {
      return this;
    }
    itemGap = v;
    applyOrientationLayout();
    rebuildContent(false);
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
  public FlatCardList<T> setListPadding(final Insets insets) {
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
  public FlatCardList<T> setSelectionMode(final CardSelectionMode mode) {
    if (mode == null || mode == selectionMode) {
      return this;
    }
    selectionMode = mode;
    if (mode == CardSelectionMode.NONE) {
      selectionModel.clearSelection();
    }
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the active selection mode.
   *
   * @return the selection mode (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardSelectionMode getSelectionMode() {
    return selectionMode;
  }

  /**
   * Enables or disables drag-to-reorder.
   *
   * @param reorderable whether reorder is enabled
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCardList<T> setReorderable(final boolean reorderable) {
    if (reorderable == this.reorderable) {
      return this;
    }
    this.reorderable = reorderable;
    rebuildContent(false);
    return this;
  }

  /**
   * Returns whether reorder is currently enabled.
   *
   * @return reorder flag
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isReorderable() {
    return reorderable;
  }

  /**
   * Sets the location on each card that initiates a reorder drag.
   *
   * @param handle one of {@link ReorderHandle}; null is ignored
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCardList<T> setReorderHandle(final ReorderHandle handle) {
    if (handle == null || handle == reorderHandle) {
      return this;
    }
    reorderHandle = handle;
    rebuildContent(false);
    return this;
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
  public FlatCardList<T> setFilter(final Predicate<T> filter) {
    this.filter = filter;
    rebuildVisibleItems();
    rebuildContent(animateChanges);
    return this;
  }

  /**
   * Sets a sort comparator that orders rendered items. Pass null to clear.
   *
   * <p>Combines with the filter (filter first, then sort). When non-null, drag-to-reorder is
   * disabled and a one-shot warning is logged.
   *
   * @param comparator the comparator; null clears sorting
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatCardList<T> setSortOrder(final Comparator<T> comparator) {
    this.comparator = comparator;
    if (comparator != null && reorderable && !reorderWarningLogged) {
      LOG.warning("FlatCardList: reorder disabled while a sort order is active");
      reorderWarningLogged = true;
    }
    rebuildVisibleItems();
    rebuildContent(animateChanges);
    return this;
  }

  /**
   * Replaces the placeholder shown when zero items are visible. Pass null to fall back to the
   * built-in default.
   *
   * @param component the placeholder; null restores the default
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public FlatCardList<T> setEmptyState(final JComponent component) {
    emptyState = component;
    if (emptyHolder.isShowing() || visibleItems.isEmpty()) {
      rebuildContent(false);
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
  public FlatCardList<T> setLoading(final boolean loading) {
    if (loading == this.loading) {
      return this;
    }
    this.loading = loading;
    rebuildContent(false);
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
  public FlatCardList<T> setLoadingComponent(final JComponent component) {
    loadingComponent = component;
    if (loading) {
      rebuildContent(false);
    }
    return this;
  }

  /**
   * Toggles fade animations on add/remove and reorder.
   *
   * @param animate whether to animate
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCardList<T> setAnimateChanges(final boolean animate) {
    animateChanges = animate;
    return this;
  }

  /**
   * Sets the animation duration in milliseconds.
   *
   * @param ms the duration; clamped to {@code >= 50}
   * @return this list
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCardList<T> setAnimationDuration(final int ms) {
    animationDurationMs = Math.max(50, ms);
    return this;
  }

  /**
   * Registers a reorder listener.
   *
   * @param listener the listener; null is ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addReorderListener(final CardReorderListener<T> listener) {
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
  public void removeReorderListener(final CardReorderListener<T> listener) {
    reorderListeners.remove(listener);
  }

  /**
   * Returns the rendered card for the given item, or {@code null} if the item is not currently
   * visible.
   *
   * @param item the item
   * @return the card, or null
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard getCardFor(final T item) {
    return cardByItem.get(item);
  }

  // ------------------------------------------------------------- internals

  private void applyOrientationLayout() {
    content.removeAll();
    switch (orientation) {
      case GRID -> content.setLayout(new GridStackingLayout());
      case HORIZONTAL -> content.setLayout(new HorizontalStackingLayout());
      case WRAP -> content.setLayout(new WrapStackingLayout());
      case VERTICAL -> content.setLayout(new StackingLayout());
      default -> content.setLayout(new StackingLayout());
    }
  }

  private void rebuildPadding() {
    setBorder(
        BorderFactory.createEmptyBorder(
            listPadding.top, listPadding.left, listPadding.bottom, listPadding.right));
  }

  private void onModelChanged(final CardListDataEvent event) {
    rebuildVisibleItems();
    rebuildContent(animateChanges);
  }

  private void rebuildVisibleItems() {
    visibleItems.clear();
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
  }

  private void rebuildContent(final boolean animate) {
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

    cardByItem.clear();
    content.removeAll();
    applyOrientationLayout();

    final boolean canReorder = reorderable && comparator == null;
    for (int i = 0; i < visibleItems.size(); i++) {
      final T item = visibleItems.get(i);
      final FlatCard card = adapter.cardFor(item, i);
      if (card == null) {
        continue;
      }
      cardByItem.put(item, card);
      configureCardForList(card, item, i, canReorder);
      content.add(card);
    }
    add(content, BorderLayout.CENTER);

    if (focusedVisibleIndex >= visibleItems.size()) {
      focusedVisibleIndex = visibleItems.size() - 1;
    }

    revalidate();
    repaint();
    if (animate) {
      startFadeIn();
    } else {
      fadeAlpha = 1f;
    }
  }

  private void configureCardForList(
      final FlatCard card, final T item, final int index, final boolean canReorder) {
    if (selectionMode != CardSelectionMode.NONE) {
      // CLICKABLE (not SELECTABLE) so FlatCard fires action events but does NOT auto-toggle
      // its own selected state on release — the list owns selection through its model and
      // calls setSelected() explicitly, which would otherwise desync against FlatCard's toggle.
      card.setInteractionMode(CardInteractionMode.CLICKABLE);
      card.setSelected(selectionModel.isSelected(item));
    }

    final MouseInputAdapter handler =
        new MouseInputAdapter() {
          @Override
          public void mousePressed(final MouseEvent event) {
            if (!shouldHandleHere(event)) {
              return;
            }
            requestFocusInWindow();
            focusedVisibleIndex = index;
            handleSelectionPress(item, index, event.getModifiersEx());
            if (canReorder && shouldStartDragFrom(event)) {
              initiateDrag(item, card, event);
            }
            event.consume();
          }

          @Override
          public void mouseDragged(final MouseEvent event) {
            if (drag == null || drag.item != item) {
              return;
            }
            if (!drag.active && hasMovedPastThreshold(event, card)) {
              activateDrag();
            }
            if (drag.active) {
              continueDrag(event);
            }
            event.consume();
          }

          @Override
          public void mouseReleased(final MouseEvent event) {
            if (drag == null || drag.item != item) {
              return;
            }
            if (drag.active) {
              endDrag(event);
            } else {
              // Press-with-no-drag — let FlatCard's own mouseReleased run its course (which
              // will fire the deferred header toggle if applicable). Just clear the pending
              // drag state so the next press starts clean.
              drag = null;
            }
            event.consume();
          }

          private boolean shouldHandleHere(final MouseEvent event) {
            return event.getButton() == MouseEvent.BUTTON1
                || event.getID() != MouseEvent.MOUSE_PRESSED;
          }

          private boolean shouldStartDragFrom(final MouseEvent event) {
            return switch (reorderHandle) {
              case WHOLE_CARD -> true;
              case LEADING_ICON -> isInLeadingIcon(card, event);
              case TRAILING_HANDLE -> false;
            };
          }
        };
    card.addMouseListener(handler);
    card.addMouseMotionListener(handler);

    if (canReorder && reorderHandle == ReorderHandle.TRAILING_HANDLE) {
      installTrailingHandle(card, item, index);
    }

    // Cursor strategy: distinguish "draggable surface" (MOVE) from "click affordance" (HAND).
    // - WHOLE_CARD: the entire card body is draggable, so the card cursor is MOVE.
    // - TRAILING_HANDLE / LEADING_ICON: only the dedicated affordance is draggable; the rest
    //   of the card stays at HAND for selection (set by setInteractionMode(CLICKABLE) above).
    // The chevron always overrides to HAND (set in FlatCard) so it visibly contrasts with a
    // MOVE'd card body.
    if (canReorder && reorderHandle == ReorderHandle.WHOLE_CARD) {
      card.setCursor(Cursors.grab());
    }
  }

  private boolean isInLeadingIcon(final FlatCard card, final MouseEvent event) {
    return event.getX() <= 24;
  }

  private boolean hasMovedPastThreshold(final MouseEvent event, final FlatCard card) {
    if (drag == null) {
      return false;
    }
    final Point now = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    final int dx = now.x - drag.startPoint.x;
    final int dy = now.y - drag.startPoint.y;
    return dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD;
  }

  private void installTrailingHandle(final FlatCard card, final T item, final int index) {
    final JLabel grip = new JLabel("⋮⋮");
    grip.setHorizontalAlignment(SwingConstants.CENTER);
    grip.setForeground(UIManager.getColor("Label.disabledForeground"));
    grip.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
    grip.setCursor(Cursors.grab());
    grip.setToolTipText("Drag to reorder");
    grip.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent event) {
            initiateDrag(item, card, event);
          }

          @Override
          public void mouseReleased(final MouseEvent event) {
            if (drag == null) {
              return;
            }
            if (drag.active) {
              endDrag(event);
            } else {
              drag = null;
            }
          }
        });
    grip.addMouseMotionListener(
        new MouseInputAdapter() {
          @Override
          public void mouseDragged(final MouseEvent event) {
            if (drag == null) {
              return;
            }
            if (!drag.active && hasMovedPastThreshold(event, card)) {
              activateDrag();
            }
            if (drag.active) {
              continueDrag(event);
            }
          }
        });
    card.setTrailingActions(grip);
  }

  // -------------------------------------------------------------- selection

  private void handleSelectionPress(final T item, final int index, final int modifiers) {
    if (selectionMode == CardSelectionMode.NONE) {
      return;
    }
    final boolean shift = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
    final int meta = InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK;
    final boolean toggle = (modifiers & meta) != 0;

    if (selectionMode == CardSelectionMode.SINGLE) {
      selectionModel.setSelected(List.of(item));
      anchorItem = item;
    } else if (shift && anchorItem != null) {
      final int anchorIdx = visibleItems.indexOf(anchorItem);
      if (anchorIdx >= 0) {
        final int from = Math.min(anchorIdx, index);
        final int to = Math.max(anchorIdx, index);
        final List<T> range = new ArrayList<>(visibleItems.subList(from, to + 1));
        selectionModel.setSelected(range);
      } else {
        selectionModel.setSelected(List.of(item));
        anchorItem = item;
      }
    } else if (toggle) {
      selectionModel.toggle(item);
      anchorItem = item;
    } else {
      selectionModel.setSelected(List.of(item));
      anchorItem = item;
    }
    syncSelectionVisuals();
  }

  private void syncSelectionVisuals() {
    for (Map.Entry<T, FlatCard> entry : cardByItem.entrySet()) {
      entry.getValue().setSelected(selectionModel.isSelected(entry.getKey()));
    }
  }

  // ----------------------------------------------------------------- drag

  /**
   * Records drag intent without committing to it yet — the user might just be clicking. Visuals
   * (cursor swap, Z-order, animation, pending-click cancellation) are deferred to {@link
   * #activateDrag()} once the cursor crosses {@link #DRAG_THRESHOLD}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void initiateDrag(final T item, final FlatCard card, final MouseEvent event) {
    drag = new DragState();
    drag.item = item;
    drag.card = card;
    drag.fromIndex = visibleItems.indexOf(item);
    drag.toIndex = drag.fromIndex;
    drag.startPoint = SwingUtilities.convertPoint(card, event.getPoint(), content);
    drag.currentPoint = drag.startPoint;
    drag.grabOffset = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), card);
    drag.active = false;
  }

  /**
   * Promotes a pending drag (set up by {@link #initiateDrag}) to the active state: cancels any
   * deferred header-click toggle, swaps the cursor to grabbing, brings the card to the top of the
   * Z-order, snapshots displaced positions, and starts the animation timer.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void activateDrag() {
    if (drag == null || drag.active) {
      return;
    }
    drag.active = true;

    // Telling the card to drop any pending press state suppresses the header-collapse toggle
    // that would otherwise fire on the upcoming mouseReleased — the user meant "drag", not
    // "click to expand".
    drag.card.cancelPendingClick();

    displacedY.clear();
    displacedX.clear();
    for (FlatCard c : cardByItem.values()) {
      displacedY.put(c, c.getY());
      displacedX.put(c, c.getX());
    }
    recomputeDragTargets();

    content.setComponentZOrder(drag.card, 0);

    setCursor(Cursors.grabbing());
    drag.card.setCursor(Cursors.grabbing());
    startDragAnimation();
    content.revalidate();
    content.repaint();
  }

  private void continueDrag(final MouseEvent event) {
    if (drag == null) {
      return;
    }
    final Point p = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), content);
    drag.currentPoint = p;
    final int newDrop = computeDropIndex(p);
    if (newDrop != drag.toIndex) {
      drag.toIndex = newDrop;
      recomputeDragTargets();
    }
    content.revalidate();
    content.repaint();
  }

  /**
   * Computes the slot the dragged card would land in. Uses a stable "natural" position formula
   * (independent of in-flight animation), so the answer doesn't oscillate as cards slide.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private int computeDropIndex(final Point point) {
    if (orientation == FlatListOrientation.GRID) {
      return computeGridDropIndex(point);
    }
    int slot = 0;
    final Insets in = content.getInsets();
    int y = in.top;
    for (T item : visibleItems) {
      if (item == drag.item) {
        continue;
      }
      final FlatCard c = cardByItem.get(item);
      if (c == null) {
        continue;
      }
      final int h = c.getPreferredSize().height;
      final int mid = y + h / 2;
      if (point.y > mid) {
        slot++;
      }
      y += h + itemGap;
    }
    return slot;
  }

  private int computeGridDropIndex(final Point point) {
    final Insets in = content.getInsets();
    final int availW = Math.max(1, content.getWidth() - in.left - in.right);
    final int cellW = Math.max(1, (availW - (columns - 1) * itemGap) / columns);
    final int cellH = Math.max(1, computeGridCellHeight());
    final int col = Math.max(0, Math.min(columns - 1, (point.x - in.left) / (cellW + itemGap)));
    final int row = Math.max(0, (point.y - in.top) / (cellH + itemGap));
    final int idx = row * columns + col;
    return Math.max(0, Math.min(visibleItems.size() - 1, idx));
  }

  private int computeGridCellHeight() {
    int max = 0;
    for (FlatCard c : cardByItem.values()) {
      max = Math.max(max, c.getPreferredSize().height);
    }
    return max > 0 ? max : 32;
  }

  /**
   * Recomputes target X / Y for every non-dragged card, based on the visual order produced by
   * removing the dragged card and reinserting it at {@code drag.toIndex}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void recomputeDragTargets() {
    targetY.clear();
    targetX.clear();
    final List<FlatCard> ordered = new ArrayList<>();
    for (T item : visibleItems) {
      final FlatCard c = cardByItem.get(item);
      if (c != null) {
        ordered.add(c);
      }
    }
    ordered.remove(drag.card);
    final int dropClamp = Math.max(0, Math.min(ordered.size(), drag.toIndex));
    ordered.add(dropClamp, drag.card);

    final Insets in = content.getInsets();
    if (orientation == FlatListOrientation.GRID) {
      final int availW = Math.max(1, content.getWidth() - in.left - in.right);
      final int cellW = Math.max(1, (availW - (columns - 1) * itemGap) / columns);
      final int cellH = Math.max(1, computeGridCellHeight());
      for (int i = 0; i < ordered.size(); i++) {
        final FlatCard c = ordered.get(i);
        final int col = i % columns;
        final int row = i / columns;
        targetX.put(c, in.left + col * (cellW + itemGap));
        targetY.put(c, in.top + row * (cellH + itemGap));
      }
    } else {
      int y = in.top;
      for (FlatCard c : ordered) {
        targetY.put(c, y);
        targetX.put(c, in.left);
        y += c.getPreferredSize().height + itemGap;
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
              for (Map.Entry<FlatCard, Integer> entry : targetY.entrySet()) {
                final FlatCard c = entry.getKey();
                if (c == drag.card) {
                  continue;
                }
                if (animateAxis(c, entry.getValue(), displacedY, c.getY())) {
                  moved = true;
                }
                final Integer tx = targetX.get(c);
                if (tx != null && animateAxis(c, tx, displacedX, c.getX())) {
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

  /**
   * Lerps a single axis (x or y) for {@code card} toward {@code target}; returns true if it moved.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static boolean animateAxis(
      final FlatCard card, final int target, final Map<FlatCard, Integer> map, final int fallback) {
    final Integer curBoxed = map.get(card);
    final int cur = curBoxed != null ? curBoxed : fallback;
    if (cur == target) {
      return false;
    }
    final int diff = target - cur;
    int step = (int) Math.round(diff * 0.30);
    if (step == 0 || Math.abs(diff) <= 1) {
      step = diff;
    }
    map.put(card, cur + step);
    return true;
  }

  private void endDrag(final MouseEvent event) {
    setCursor(Cursor.getDefaultCursor());
    if (dragAnimTimer != null) {
      dragAnimTimer.stop();
    }
    displacedY.clear();
    targetY.clear();
    displacedX.clear();
    targetX.clear();
    if (drag == null) {
      return;
    }
    // Restore the dragged card's hover cursor. If the drop triggers a model.move below, the
    // card will be rebuilt and re-configured anyway — this only matters for no-op drops where
    // the same card stays on screen.
    if (drag.card != null && reorderable && reorderHandle == ReorderHandle.WHOLE_CARD) {
      drag.card.setCursor(Cursors.grab());
    }
    final int fromVis = drag.fromIndex;
    final int toVis = drag.toIndex;
    final T item = drag.item;
    drag = null;
    if (fromVis < 0 || toVis < 0 || fromVis == toVis) {
      restoreContentOrder();
      return;
    }
    boolean modelMoved = false;
    if (model instanceof DefaultCardListModel<T> mutable) {
      final int fromModel = indexOfInModel(item);
      final T atTarget = visibleItems.get(toVis);
      final int toModel = indexOfInModel(atTarget);
      if (fromModel >= 0 && toModel >= 0 && fromModel != toModel) {
        mutable.move(fromModel, toModel);
        fireReorder(item, fromModel, toModel);
        modelMoved = true;
      }
    } else {
      LOG.warning("FlatCardList: reorder requires a mutable DefaultCardListModel; drop ignored");
    }
    if (!modelMoved) {
      // Successful model.move fires a MOVED event → onModelChanged → rebuildContent, which
      // resets child order. When the model is NOT mutated (e.g., the card at toVis happened to
      // be the dragged item, or the model wasn't mutable), the child order scrambled by
      // activateDrag's setComponentZOrder(drag.card, 0) call would otherwise persist and make
      // the layout (which iterates children by index) visually relocate the dragged card to row
      // 0 / col 0 — independent of the model.
      restoreContentOrder();
    }
  }

  /**
   * Restores child component order of content to match {@code visibleItems}, then revalidates and
   * repaints. Required after a no-op drag-drop because {@link #activateDrag} calls {@code
   * content.setComponentZOrder(drag.card, 0)} during the drag to bring the dragged card to the
   * visual front — which also rearranges the children array. The idle layouts (Stacking and
   * GridStacking) place children by index, so a scrambled order after a no-op drop visually
   * relocates the dragged card to position 0 even though the model is untouched.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void restoreContentOrder() {
    int targetIndex = 0;
    for (T item : visibleItems) {
      final FlatCard card = cardByItem.get(item);
      if (card == null) {
        continue;
      }
      if (card.getParent() == content) {
        content.setComponentZOrder(card, targetIndex);
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

  private void fireReorder(final T item, final int from, final int to) {
    final CardReorderEvent<T> evt = new CardReorderEvent<>(this, item, from, to);
    for (CardReorderListener<T> l : new ArrayList<>(reorderListeners)) {
      l.cardReordered(evt);
    }
  }

  // ----------------------------------------------------------- placeholder

  private final class DragState {
    T item;
    FlatCard card;
    int fromIndex;
    int toIndex;
    Point startPoint;
    Point currentPoint;

    /** Press point in card-local coordinates; used to keep the card under the cursor. */
    Point grabOffset;

    /** False until the cursor has moved past {@link #DRAG_THRESHOLD} pixels from press. */
    boolean active;
  }

  /** Inner content panel — paints the drop placeholder on top of its laid-out children. */
  private final class ContentPanel extends JPanel {
    @Override
    protected void paintChildren(final Graphics g) {
      super.paintChildren(g);
      paintDropPlaceholder(g);
    }

    @Override
    public Dimension getMaximumSize() {
      if (orientation == FlatListOrientation.VERTICAL) {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
      }
      return super.getMaximumSize();
    }
  }

  /**
   * Layout manager for vertical mode. Stacks cards top-to-bottom with the configured gap when idle;
   * during a drag, places the dragged card under the cursor and reads the per-card displaced Y
   * (driven by the drag animation timer) for everything else.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class StackingLayout implements java.awt.LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // No-op — this layout doesn't use named constraints.
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      displacedY.remove(comp);
      targetY.remove(comp);
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
      final Insets in = parent.getInsets();
      final int width = Math.max(0, parent.getWidth() - in.left - in.right);
      if (drag == null) {
        int y = in.top;
        for (int i = 0; i < parent.getComponentCount(); i++) {
          final Component c = parent.getComponent(i);
          final Dimension pref = c.getPreferredSize();
          c.setBounds(in.left, y, width, pref.height);
          y += pref.height + itemGap;
        }
        return;
      }
      // Drag mode: dragged card follows the cursor (Y); others use displaced Y.
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component c = parent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        if (c == drag.card) {
          int y = drag.currentPoint.y - drag.grabOffset.y;
          // Clamp so the card doesn't drift past the visible content area.
          final int maxY = Math.max(in.top, parent.getHeight() - in.bottom - pref.height);
          y = Math.max(in.top, Math.min(maxY, y));
          c.setBounds(in.left, y, width, pref.height);
        } else {
          final Integer dispY = displacedY.get(c);
          final int y = dispY != null ? dispY : in.top;
          c.setBounds(in.left, y, width, pref.height);
        }
      }
    }
  }

  /**
   * 2D stacking layout for {@link Orientation#GRID}. Cells are uniform width and uniform height
   * (max-card preferred height across the visible set, mirroring {@code GridLayout}). During a
   * drag, the dragged card follows the cursor in both axes; others use the per-card displaced X / Y
   * driven by the animation timer.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class GridStackingLayout implements java.awt.LayoutManager {
    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // No-op — this layout doesn't use named constraints.
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      displacedY.remove(comp);
      targetY.remove(comp);
      displacedX.remove(comp);
      targetX.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Insets in = parent.getInsets();
      final int count = parent.getComponentCount();
      final int rows = (count + columns - 1) / Math.max(1, columns);
      final int cellH = computeGridCellHeight();
      int cellW = 0;
      for (int i = 0; i < count; i++) {
        cellW = Math.max(cellW, parent.getComponent(i).getPreferredSize().width);
      }
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
      final Insets in = parent.getInsets();
      final int availW = Math.max(0, parent.getWidth() - in.left - in.right);
      final int cellW = Math.max(1, (availW - (columns - 1) * itemGap) / columns);
      final int cellH = Math.max(1, computeGridCellHeight());

      if (drag == null) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
          final Component c = parent.getComponent(i);
          final int col = i % columns;
          final int row = i / columns;
          c.setBounds(
              in.left + col * (cellW + itemGap), in.top + row * (cellH + itemGap), cellW, cellH);
        }
        return;
      }

      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component c = parent.getComponent(i);
        if (c == drag.card) {
          int x = drag.currentPoint.x - drag.grabOffset.x;
          int y = drag.currentPoint.y - drag.grabOffset.y;
          final int maxX = Math.max(in.left, parent.getWidth() - in.right - cellW);
          final int maxY = Math.max(in.top, parent.getHeight() - in.bottom - cellH);
          x = Math.max(in.left, Math.min(maxX, x));
          y = Math.max(in.top, Math.min(maxY, y));
          c.setBounds(x, y, cellW, cellH);
        } else {
          final Integer dispX = displacedX.get(c);
          final Integer dispY = displacedY.get(c);
          final int x = dispX != null ? dispX : in.left;
          final int y = dispY != null ? dispY : in.top;
          c.setBounds(x, y, cellW, cellH);
        }
      }
    }
  }

  private void paintDropPlaceholder(final Graphics g) {
    // Both vertical and grid modes now use the StackingLayout / GridStackingLayout to physically
    // slide surrounding cards out of the way; no separate placeholder rectangle is needed.
  }

  /**
   * Single-row horizontal layout (story #242 back-port of FlatChipList's HorizontalLayout). Cards
   * are placed left-to-right at preferred width and a shared row height (max of preferred heights).
   * Overflow is clipped on the right — wrap the list in a {@link javax.swing.JScrollPane} for
   * horizontal scrolling. Drag-while-active is not specially supported for this orientation; the
   * layout falls back to the static positions during a drag, which is acceptable since horizontal
   * card rows are typically read-only.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class HorizontalStackingLayout implements java.awt.LayoutManager {
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
      final Insets in = parent.getInsets();
      int rowH = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        rowH = Math.max(rowH, parent.getComponent(i).getPreferredSize().height);
      }
      int x = in.left;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component c = parent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        c.setBounds(x, in.top, pref.width, rowH);
        x += pref.width + itemGap;
      }
    }
  }

  /**
   * Multi-row wrapping layout (story #242 back-port of FlatChipList's WrapLayout). Cards flow
   * left-to-right and wrap to a new row when the container's available width is exhausted. Respects
   * {@link #itemGap} for both row and column spacing. As with horizontal, drag is not specially
   * supported here.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class WrapStackingLayout implements java.awt.LayoutManager {
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
      return measureOrLayout(parent, false);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      measureOrLayout(parent, true);
    }

    private Dimension measureOrLayout(final Container parent, final boolean apply) {
      final Insets in = parent.getInsets();
      final int avail = Math.max(in.left, parent.getWidth() - in.right);
      int x = in.left;
      int y = in.top;
      int rowH = 0;
      int maxX = in.left;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        final Component c = parent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        if (x > in.left && x + pref.width > avail) {
          x = in.left;
          y += rowH + itemGap;
          rowH = 0;
        }
        if (apply) {
          c.setBounds(x, y, pref.width, pref.height);
        }
        x += pref.width + itemGap;
        rowH = Math.max(rowH, pref.height);
        maxX = Math.max(maxX, x - itemGap);
      }
      return new Dimension(maxX + in.right, y + rowH + in.bottom);
    }
  }

  // ------------------------------------------------------------- animation

  private void startFadeIn() {
    if (fadeTimer != null && fadeTimer.isRunning()) {
      fadeTimer.stop();
    }
    fadeAlpha = 0f;
    final long start = System.currentTimeMillis();
    fadeTimer =
        new Timer(
            ANIMATION_TICK_MS,
            e -> {
              final long elapsed = System.currentTimeMillis() - start;
              fadeAlpha = Math.min(1f, elapsed / (float) animationDurationMs);
              content.repaint();
              if (fadeAlpha >= 1f) {
                ((Timer) e.getSource()).stop();
              }
            });
    fadeTimer.start();
  }

  @Override
  protected void paintChildren(final Graphics g) {
    if (fadeAlpha >= 1f || !animateChanges) {
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

  // -------------------------------------------------------------- defaults

  private JComponent defaultEmptyState() {
    final JLabel lbl = new JLabel("No items to show", SwingConstants.CENTER);
    lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
    lbl.setBorder(BorderFactory.createEmptyBorder(32, 16, 32, 16));
    return lbl;
  }

  private JComponent defaultLoading() {
    final JLabel lbl = new JLabel("Loading…", SwingConstants.CENTER);
    lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
    lbl.setBorder(BorderFactory.createEmptyBorder(32, 16, 32, 16));
    return lbl;
  }

  // -------------------------------------------------------------- keyboard

  private void installKeyboard() {
    final InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap am = getActionMap();

    final Action up = new MoveFocus(-1, false);
    final Action down = new MoveFocus(1, false);
    final Action upPage = new MoveFocus(-5, false);
    final Action downPage = new MoveFocus(5, false);
    final Action home = new JumpFocus(0);
    final Action end = new JumpFocus(Integer.MAX_VALUE);
    final Action activate = new ActivateFocused();
    final Action selectAll = new SelectAllAction();

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "flatlist.up");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "flatlist.down");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "flatlist.pageUp");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "flatlist.pageDown");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "flatlist.home");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "flatlist.end");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "flatlist.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "flatlist.activate");
    im.put(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK),
        "flatlist.selectAll");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "flatlist.selectAll");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), "flatlist.selectAll");

    am.put("flatlist.up", up);
    am.put("flatlist.down", down);
    am.put("flatlist.pageUp", upPage);
    am.put("flatlist.pageDown", downPage);
    am.put("flatlist.home", home);
    am.put("flatlist.end", end);
    am.put("flatlist.activate", activate);
    am.put("flatlist.selectAll", selectAll);
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
    final FlatCard card = cardByItem.get(visibleItems.get(next));
    if (card != null) {
      card.requestFocusInWindow();
      scrollRectToVisible(card.getBounds());
    }
  }

  private final class MoveFocus extends AbstractAction {
    private final int delta;

    MoveFocus(final int delta, final boolean ignored) {
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
      if (selectionMode != CardSelectionMode.MULTIPLE) {
        return;
      }
      selectionModel.setSelected(new ArrayList<>(visibleItems));
      syncSelectionVisuals();
    }
  }

  // ----------------------------------------------------------- LAF / a11y

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    repaint();
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleFlatCardList();
    }
    return accessibleContext;
  }

  /** Accessible role of the list itself; rendered cards expose their own LIST_ITEM context. */
  protected class AccessibleFlatCardList extends AccessibleJPanel {
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
      final FlatCard card = cardByItem.get(visibleItems.get(index));
      if (card == null) {
        return null;
      }
      final Component c = card;
      return c instanceof Accessible a ? a : null;
    }
  }
}
