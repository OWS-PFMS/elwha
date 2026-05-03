package com.owspfm.ui.components.card.list;

import com.owspfm.ui.components.card.CardInteractionMode;
import com.owspfm.ui.components.card.FlatCard;
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
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public class FlatCardList<T> extends JPanel implements Accessible {

  /** Layout orientation for the rendered list. */
  public enum Orientation {
    /** Single-column vertical stack (default). */
    VERTICAL,
    /** N-column grid (set with {@link #setColumns(int)}). */
    GRID
  }

  private static final Logger LOG = Logger.getLogger(FlatCardList.class.getName());

  private static final int DEFAULT_GAP = 8;
  private static final int DEFAULT_COLUMNS = 2;
  private static final int DEFAULT_ANIMATION_MS = 180;
  private static final int ANIMATION_TICK_MS = 16;

  /** Pixels the cursor must travel from press before a drag is considered activated. */
  private static final int DRAG_THRESHOLD = 5;

  // Backing model + adapter ------------------------------------------------
  private final CardListModel<T> myModel;
  private final CardAdapter<T> myAdapter;
  private final CardListDataListener myModelListener = this::onModelChanged;
  private final CardSelectionModel<T> mySelectionModel = new DefaultCardSelectionModel<>();

  // Configuration ----------------------------------------------------------
  private Orientation myOrientation = Orientation.VERTICAL;
  private int myColumns = DEFAULT_COLUMNS;
  private int myItemGap = DEFAULT_GAP;
  private Insets myListPadding = new Insets(DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_GAP);
  private CardSelectionMode mySelectionMode = CardSelectionMode.NONE;
  private boolean myReorderable;
  private ReorderHandle myReorderHandle = ReorderHandle.WHOLE_CARD;
  private Predicate<T> myFilter;
  private Comparator<T> myComparator;
  private JComponent myEmptyState;
  private JComponent myLoadingComponent;
  private boolean myLoading;
  private boolean myAnimateChanges;
  private int myAnimationDurationMs = DEFAULT_ANIMATION_MS;
  private boolean myReorderWarningLogged;

  // Listeners --------------------------------------------------------------
  private final List<CardReorderListener<T>> myReorderListeners = new ArrayList<>();

  // Render cache: visible items in render order ----------------------------
  private final List<T> myVisibleItems = new ArrayList<>();
  private final Map<T, FlatCard> myCardByItem = new LinkedHashMap<>();
  private int myFocusedVisibleIndex = -1;
  private T myAnchorItem;

  // Layout panels ----------------------------------------------------------
  private final JPanel myContent;
  private final JPanel myEmptyHolder;
  private final JPanel myLoadingHolder;

  // Drag state -------------------------------------------------------------
  private DragState myDrag;
  private final Map<FlatCard, Integer> myDisplacedY = new HashMap<>();
  private final Map<FlatCard, Integer> myTargetY = new HashMap<>();
  private final Map<FlatCard, Integer> myDisplacedX = new HashMap<>();
  private final Map<FlatCard, Integer> myTargetX = new HashMap<>();
  private Timer myDragAnimTimer;

  // Animation state --------------------------------------------------------
  private Timer myFadeTimer;
  private float myFadeAlpha = 1f;

  // ------------------------------------------------------------------ ctor

  /**
   * Builds a list bound to the given model and adapter.
   *
   * @param theModel the backing model (required)
   * @param theAdapter the adapter that maps items to cards (required)
   */
  public FlatCardList(final CardListModel<T> theModel, final CardAdapter<T> theAdapter) {
    super(new BorderLayout());
    if (theModel == null) {
      throw new IllegalArgumentException("model must not be null");
    }
    if (theAdapter == null) {
      throw new IllegalArgumentException("adapter must not be null");
    }
    myModel = theModel;
    myAdapter = theAdapter;

    myContent = new ContentPanel();
    myContent.setOpaque(false);
    applyOrientationLayout();

    myEmptyHolder = new JPanel(new BorderLayout());
    myEmptyHolder.setOpaque(false);

    myLoadingHolder = new JPanel(new BorderLayout());
    myLoadingHolder.setOpaque(false);

    setOpaque(false);
    setFocusable(true);
    add(myContent, BorderLayout.CENTER);
    rebuildPadding();

    myModel.addCardListDataListener(myModelListener);
    rebuildVisibleItems();
    rebuildContent(false);

    installKeyboard();
  }

  // ------------------------------------------------------------- public API

  /**
   * Returns the backing model.
   *
   * @return the model
   */
  public CardListModel<T> getModel() {
    return myModel;
  }

  /**
   * Returns the selection model. Always non-null even when selection is disabled.
   *
   * @return the selection model
   */
  public CardSelectionModel<T> getSelectionModel() {
    return mySelectionModel;
  }

  /**
   * Sets the layout orientation.
   *
   * @param theOrientation one of {@link Orientation}; null is ignored
   * @return this list
   */
  public FlatCardList<T> setOrientation(final Orientation theOrientation) {
    if (theOrientation == null || theOrientation == myOrientation) {
      return this;
    }
    myOrientation = theOrientation;
    applyOrientationLayout();
    rebuildContent(false);
    return this;
  }

  /**
   * Sets the column count for {@link Orientation#GRID}.
   *
   * @param theColumns column count, clamped to {@code >= 1}
   * @return this list
   */
  public FlatCardList<T> setColumns(final int theColumns) {
    final int v = Math.max(1, theColumns);
    if (v == myColumns) {
      return this;
    }
    myColumns = v;
    if (myOrientation == Orientation.GRID) {
      applyOrientationLayout();
      rebuildContent(false);
    }
    return this;
  }

  /**
   * Sets the gap between rendered cards (vertical stack: between rows; grid: between cells).
   *
   * @param theGap pixels, clamped to {@code >= 0}
   * @return this list
   */
  public FlatCardList<T> setItemGap(final int theGap) {
    final int v = Math.max(0, theGap);
    if (v == myItemGap) {
      return this;
    }
    myItemGap = v;
    applyOrientationLayout();
    rebuildContent(false);
    return this;
  }

  /**
   * Sets the padding around the rendered list.
   *
   * @param theInsets the insets; null treated as zero
   * @return this list
   */
  public FlatCardList<T> setListPadding(final Insets theInsets) {
    myListPadding = theInsets == null ? new Insets(0, 0, 0, 0) : (Insets) theInsets.clone();
    rebuildPadding();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Sets the selection mode.
   *
   * @param theMode the selection mode; null is ignored
   * @return this list
   */
  public FlatCardList<T> setSelectionMode(final CardSelectionMode theMode) {
    if (theMode == null || theMode == mySelectionMode) {
      return this;
    }
    mySelectionMode = theMode;
    if (theMode == CardSelectionMode.NONE) {
      mySelectionModel.clearSelection();
    }
    rebuildContent(false);
    return this;
  }

  /**
   * Returns the active selection mode.
   *
   * @return the selection mode (never null)
   */
  public CardSelectionMode getSelectionMode() {
    return mySelectionMode;
  }

  /**
   * Enables or disables drag-to-reorder.
   *
   * @param theReorderable whether reorder is enabled
   * @return this list
   */
  public FlatCardList<T> setReorderable(final boolean theReorderable) {
    if (theReorderable == myReorderable) {
      return this;
    }
    myReorderable = theReorderable;
    rebuildContent(false);
    return this;
  }

  /**
   * Returns whether reorder is currently enabled.
   *
   * @return reorder flag
   */
  public boolean isReorderable() {
    return myReorderable;
  }

  /**
   * Sets the location on each card that initiates a reorder drag.
   *
   * @param theHandle one of {@link ReorderHandle}; null is ignored
   * @return this list
   */
  public FlatCardList<T> setReorderHandle(final ReorderHandle theHandle) {
    if (theHandle == null || theHandle == myReorderHandle) {
      return this;
    }
    myReorderHandle = theHandle;
    rebuildContent(false);
    return this;
  }

  /**
   * Sets a filter predicate that hides items rejected by it. Pass null to clear.
   *
   * @param theFilter the predicate; null clears filtering
   * @return this list
   */
  public FlatCardList<T> setFilter(final Predicate<T> theFilter) {
    myFilter = theFilter;
    rebuildVisibleItems();
    rebuildContent(myAnimateChanges);
    return this;
  }

  /**
   * Sets a sort comparator that orders rendered items. Pass null to clear.
   *
   * <p>Combines with the filter (filter first, then sort). When non-null, drag-to-reorder is
   * disabled and a one-shot warning is logged.
   *
   * @param theComparator the comparator; null clears sorting
   * @return this list
   */
  public FlatCardList<T> setSortOrder(final Comparator<T> theComparator) {
    myComparator = theComparator;
    if (theComparator != null && myReorderable && !myReorderWarningLogged) {
      LOG.warning("FlatCardList: reorder disabled while a sort order is active");
      myReorderWarningLogged = true;
    }
    rebuildVisibleItems();
    rebuildContent(myAnimateChanges);
    return this;
  }

  /**
   * Replaces the placeholder shown when zero items are visible. Pass null to fall back to the
   * built-in default.
   *
   * @param theComponent the placeholder; null restores the default
   * @return this list
   */
  public FlatCardList<T> setEmptyState(final JComponent theComponent) {
    myEmptyState = theComponent;
    if (myEmptyHolder.isShowing() || myVisibleItems.isEmpty()) {
      rebuildContent(false);
    }
    return this;
  }

  /**
   * Sets the loading flag. While true, the list renders the loading component instead of items.
   *
   * @param theLoading whether to show the loading state
   * @return this list
   */
  public FlatCardList<T> setLoading(final boolean theLoading) {
    if (theLoading == myLoading) {
      return this;
    }
    myLoading = theLoading;
    rebuildContent(false);
    return this;
  }

  /**
   * Replaces the loading-state component. Pass null to fall back to the built-in default.
   *
   * @param theComponent the loading component
   * @return this list
   */
  public FlatCardList<T> setLoadingComponent(final JComponent theComponent) {
    myLoadingComponent = theComponent;
    if (myLoading) {
      rebuildContent(false);
    }
    return this;
  }

  /**
   * Toggles fade animations on add/remove and reorder.
   *
   * @param theAnimate whether to animate
   * @return this list
   */
  public FlatCardList<T> setAnimateChanges(final boolean theAnimate) {
    myAnimateChanges = theAnimate;
    return this;
  }

  /**
   * Sets the animation duration in milliseconds.
   *
   * @param theMs the duration; clamped to {@code >= 50}
   * @return this list
   */
  public FlatCardList<T> setAnimationDuration(final int theMs) {
    myAnimationDurationMs = Math.max(50, theMs);
    return this;
  }

  /**
   * Registers a reorder listener.
   *
   * @param theListener the listener; null is ignored
   */
  public void addReorderListener(final CardReorderListener<T> theListener) {
    if (theListener != null && !myReorderListeners.contains(theListener)) {
      myReorderListeners.add(theListener);
    }
  }

  /**
   * Removes a previously registered reorder listener.
   *
   * @param theListener the listener
   */
  public void removeReorderListener(final CardReorderListener<T> theListener) {
    myReorderListeners.remove(theListener);
  }

  /**
   * Returns the rendered card for the given item, or {@code null} if the item is not currently
   * visible.
   *
   * @param theItem the item
   * @return the card, or null
   */
  public FlatCard getCardFor(final T theItem) {
    return myCardByItem.get(theItem);
  }

  // ------------------------------------------------------------- internals

  private void applyOrientationLayout() {
    myContent.removeAll();
    if (myOrientation == Orientation.GRID) {
      myContent.setLayout(new GridStackingLayout());
    } else {
      myContent.setLayout(new StackingLayout());
    }
  }

  private void rebuildPadding() {
    setBorder(
        BorderFactory.createEmptyBorder(
            myListPadding.top, myListPadding.left, myListPadding.bottom, myListPadding.right));
  }

  private void onModelChanged(final CardListDataEvent theEvent) {
    rebuildVisibleItems();
    rebuildContent(myAnimateChanges);
  }

  private void rebuildVisibleItems() {
    myVisibleItems.clear();
    final Iterator<T> it = myModel.iterator();
    while (it.hasNext()) {
      final T item = it.next();
      if (myFilter == null || myFilter.test(item)) {
        myVisibleItems.add(item);
      }
    }
    if (myComparator != null) {
      myVisibleItems.sort(myComparator);
    }
  }

  private void rebuildContent(final boolean animate) {
    removeAll();
    if (myLoading) {
      myLoadingHolder.removeAll();
      myLoadingHolder.add(
          myLoadingComponent != null ? myLoadingComponent : defaultLoading(), BorderLayout.CENTER);
      add(myLoadingHolder, BorderLayout.CENTER);
      revalidate();
      repaint();
      return;
    }
    if (myVisibleItems.isEmpty()) {
      myEmptyHolder.removeAll();
      myEmptyHolder.add(
          myEmptyState != null ? myEmptyState : defaultEmptyState(), BorderLayout.CENTER);
      add(myEmptyHolder, BorderLayout.CENTER);
      revalidate();
      repaint();
      return;
    }

    myCardByItem.clear();
    myContent.removeAll();
    applyOrientationLayout();

    final boolean canReorder = myReorderable && myComparator == null;
    for (int i = 0; i < myVisibleItems.size(); i++) {
      final T item = myVisibleItems.get(i);
      final FlatCard card = myAdapter.cardFor(item, i);
      if (card == null) {
        continue;
      }
      myCardByItem.put(item, card);
      configureCardForList(card, item, i, canReorder);
      myContent.add(card);
    }
    add(myContent, BorderLayout.CENTER);

    if (myFocusedVisibleIndex >= myVisibleItems.size()) {
      myFocusedVisibleIndex = myVisibleItems.size() - 1;
    }

    revalidate();
    repaint();
    if (animate) {
      startFadeIn();
    } else {
      myFadeAlpha = 1f;
    }
  }

  private void configureCardForList(
      final FlatCard theCard, final T theItem, final int theIndex, final boolean theCanReorder) {
    if (mySelectionMode != CardSelectionMode.NONE) {
      // CLICKABLE (not SELECTABLE) so FlatCard fires action events but does NOT auto-toggle
      // its own mySelected state on release — the list owns selection through its model and
      // calls setSelected() explicitly, which would otherwise desync against FlatCard's toggle.
      theCard.setInteractionMode(CardInteractionMode.CLICKABLE);
      theCard.setSelected(mySelectionModel.isSelected(theItem));
    }

    final MouseInputAdapter handler =
        new MouseInputAdapter() {
          @Override
          public void mousePressed(final MouseEvent theEvent) {
            if (!shouldHandleHere(theEvent)) {
              return;
            }
            requestFocusInWindow();
            myFocusedVisibleIndex = theIndex;
            handleSelectionPress(theItem, theIndex, theEvent.getModifiersEx());
            if (theCanReorder && shouldStartDragFrom(theEvent)) {
              initiateDrag(theItem, theCard, theEvent);
            }
            theEvent.consume();
          }

          @Override
          public void mouseDragged(final MouseEvent theEvent) {
            if (myDrag == null || myDrag.item != theItem) {
              return;
            }
            if (!myDrag.active && hasMovedPastThreshold(theEvent, theCard)) {
              activateDrag();
            }
            if (myDrag.active) {
              continueDrag(theEvent);
            }
            theEvent.consume();
          }

          @Override
          public void mouseReleased(final MouseEvent theEvent) {
            if (myDrag == null || myDrag.item != theItem) {
              return;
            }
            if (myDrag.active) {
              endDrag(theEvent);
            } else {
              // Press-with-no-drag — let FlatCard's own mouseReleased run its course (which
              // will fire the deferred header toggle if applicable). Just clear the pending
              // drag state so the next press starts clean.
              myDrag = null;
            }
            theEvent.consume();
          }

          private boolean shouldHandleHere(final MouseEvent theEvent) {
            return theEvent.getButton() == MouseEvent.BUTTON1
                || theEvent.getID() != MouseEvent.MOUSE_PRESSED;
          }

          private boolean shouldStartDragFrom(final MouseEvent theEvent) {
            return switch (myReorderHandle) {
              case WHOLE_CARD -> true;
              case LEADING_ICON -> isInLeadingIcon(theCard, theEvent);
              case TRAILING_HANDLE -> false;
            };
          }
        };
    theCard.addMouseListener(handler);
    theCard.addMouseMotionListener(handler);

    if (theCanReorder && myReorderHandle == ReorderHandle.TRAILING_HANDLE) {
      installTrailingHandle(theCard, theItem, theIndex);
    }

    // Cursor strategy: distinguish "draggable surface" (MOVE) from "click affordance" (HAND).
    // - WHOLE_CARD: the entire card body is draggable, so the card cursor is MOVE.
    // - TRAILING_HANDLE / LEADING_ICON: only the dedicated affordance is draggable; the rest
    //   of the card stays at HAND for selection (set by setInteractionMode(CLICKABLE) above).
    // The chevron always overrides to HAND (set in FlatCard) so it visibly contrasts with a
    // MOVE'd card body.
    if (theCanReorder && myReorderHandle == ReorderHandle.WHOLE_CARD) {
      theCard.setCursor(Cursors.grab());
    }
  }

  private boolean isInLeadingIcon(final FlatCard theCard, final MouseEvent theEvent) {
    return theEvent.getX() <= 24;
  }

  private boolean hasMovedPastThreshold(final MouseEvent theEvent, final FlatCard theCard) {
    if (myDrag == null) {
      return false;
    }
    final Point now =
        SwingUtilities.convertPoint(theEvent.getComponent(), theEvent.getPoint(), myContent);
    final int dx = now.x - myDrag.startPoint.x;
    final int dy = now.y - myDrag.startPoint.y;
    return dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD;
  }

  private void installTrailingHandle(final FlatCard theCard, final T theItem, final int theIndex) {
    final JLabel grip = new JLabel("⋮⋮");
    grip.setHorizontalAlignment(SwingConstants.CENTER);
    grip.setForeground(UIManager.getColor("Label.disabledForeground"));
    grip.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
    grip.setCursor(Cursors.grab());
    grip.setToolTipText("Drag to reorder");
    grip.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent theEvent) {
            initiateDrag(theItem, theCard, theEvent);
          }

          @Override
          public void mouseReleased(final MouseEvent theEvent) {
            if (myDrag == null) {
              return;
            }
            if (myDrag.active) {
              endDrag(theEvent);
            } else {
              myDrag = null;
            }
          }
        });
    grip.addMouseMotionListener(
        new MouseInputAdapter() {
          @Override
          public void mouseDragged(final MouseEvent theEvent) {
            if (myDrag == null) {
              return;
            }
            if (!myDrag.active && hasMovedPastThreshold(theEvent, theCard)) {
              activateDrag();
            }
            if (myDrag.active) {
              continueDrag(theEvent);
            }
          }
        });
    theCard.setTrailingActions(grip);
  }

  // -------------------------------------------------------------- selection

  private void handleSelectionPress(final T theItem, final int theIndex, final int theModifiers) {
    if (mySelectionMode == CardSelectionMode.NONE) {
      return;
    }
    final boolean shift = (theModifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
    final int meta = InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK;
    final boolean toggle = (theModifiers & meta) != 0;

    if (mySelectionMode == CardSelectionMode.SINGLE) {
      mySelectionModel.setSelected(List.of(theItem));
      myAnchorItem = theItem;
    } else if (shift && myAnchorItem != null) {
      final int anchorIdx = myVisibleItems.indexOf(myAnchorItem);
      if (anchorIdx >= 0) {
        final int from = Math.min(anchorIdx, theIndex);
        final int to = Math.max(anchorIdx, theIndex);
        final List<T> range = new ArrayList<>(myVisibleItems.subList(from, to + 1));
        mySelectionModel.setSelected(range);
      } else {
        mySelectionModel.setSelected(List.of(theItem));
        myAnchorItem = theItem;
      }
    } else if (toggle) {
      mySelectionModel.toggle(theItem);
      myAnchorItem = theItem;
    } else {
      mySelectionModel.setSelected(List.of(theItem));
      myAnchorItem = theItem;
    }
    syncSelectionVisuals();
  }

  private void syncSelectionVisuals() {
    for (Map.Entry<T, FlatCard> entry : myCardByItem.entrySet()) {
      entry.getValue().setSelected(mySelectionModel.isSelected(entry.getKey()));
    }
  }

  // ----------------------------------------------------------------- drag

  /**
   * Records drag intent without committing to it yet — the user might just be clicking. Visuals
   * (cursor swap, Z-order, animation, pending-click cancellation) are deferred to {@link
   * #activateDrag()} once the cursor crosses {@link #DRAG_THRESHOLD}.
   */
  private void initiateDrag(final T theItem, final FlatCard theCard, final MouseEvent theEvent) {
    myDrag = new DragState();
    myDrag.item = theItem;
    myDrag.card = theCard;
    myDrag.fromIndex = myVisibleItems.indexOf(theItem);
    myDrag.toIndex = myDrag.fromIndex;
    myDrag.startPoint = SwingUtilities.convertPoint(theCard, theEvent.getPoint(), myContent);
    myDrag.currentPoint = myDrag.startPoint;
    myDrag.grabOffset =
        SwingUtilities.convertPoint(theEvent.getComponent(), theEvent.getPoint(), theCard);
    myDrag.active = false;
  }

  /**
   * Promotes a pending drag (set up by {@link #initiateDrag}) to the active state: cancels any
   * deferred header-click toggle, swaps the cursor to grabbing, brings the card to the top of the
   * Z-order, snapshots displaced positions, and starts the animation timer.
   */
  private void activateDrag() {
    if (myDrag == null || myDrag.active) {
      return;
    }
    myDrag.active = true;

    // Telling the card to drop any pending press state suppresses the header-collapse toggle
    // that would otherwise fire on the upcoming mouseReleased — the user meant "drag", not
    // "click to expand".
    myDrag.card.cancelPendingClick();

    myDisplacedY.clear();
    myDisplacedX.clear();
    for (FlatCard c : myCardByItem.values()) {
      myDisplacedY.put(c, c.getY());
      myDisplacedX.put(c, c.getX());
    }
    recomputeDragTargets();

    myContent.setComponentZOrder(myDrag.card, 0);

    setCursor(Cursors.grabbing());
    myDrag.card.setCursor(Cursors.grabbing());
    startDragAnimation();
    myContent.revalidate();
    myContent.repaint();
  }

  private void continueDrag(final MouseEvent theEvent) {
    if (myDrag == null) {
      return;
    }
    final Point p =
        SwingUtilities.convertPoint(theEvent.getComponent(), theEvent.getPoint(), myContent);
    myDrag.currentPoint = p;
    final int newDrop = computeDropIndex(p);
    if (newDrop != myDrag.toIndex) {
      myDrag.toIndex = newDrop;
      recomputeDragTargets();
    }
    myContent.revalidate();
    myContent.repaint();
  }

  /**
   * Computes the slot the dragged card would land in. Uses a stable "natural" position formula
   * (independent of in-flight animation), so the answer doesn't oscillate as cards slide.
   */
  private int computeDropIndex(final Point thePoint) {
    if (myOrientation == Orientation.GRID) {
      return computeGridDropIndex(thePoint);
    }
    int slot = 0;
    final Insets in = myContent.getInsets();
    int y = in.top;
    for (T item : myVisibleItems) {
      if (item == myDrag.item) {
        continue;
      }
      final FlatCard c = myCardByItem.get(item);
      if (c == null) {
        continue;
      }
      final int h = c.getPreferredSize().height;
      final int mid = y + h / 2;
      if (thePoint.y > mid) {
        slot++;
      }
      y += h + myItemGap;
    }
    return slot;
  }

  private int computeGridDropIndex(final Point thePoint) {
    final Insets in = myContent.getInsets();
    final int availW = Math.max(1, myContent.getWidth() - in.left - in.right);
    final int cellW = Math.max(1, (availW - (myColumns - 1) * myItemGap) / myColumns);
    final int cellH = Math.max(1, computeGridCellHeight());
    final int col =
        Math.max(0, Math.min(myColumns - 1, (thePoint.x - in.left) / (cellW + myItemGap)));
    final int row = Math.max(0, (thePoint.y - in.top) / (cellH + myItemGap));
    final int idx = row * myColumns + col;
    return Math.max(0, Math.min(myVisibleItems.size() - 1, idx));
  }

  private int computeGridCellHeight() {
    int max = 0;
    for (FlatCard c : myCardByItem.values()) {
      max = Math.max(max, c.getPreferredSize().height);
    }
    return max > 0 ? max : 32;
  }

  /**
   * Recomputes target X / Y for every non-dragged card, based on the visual order produced by
   * removing the dragged card and reinserting it at {@code myDrag.toIndex}.
   */
  private void recomputeDragTargets() {
    myTargetY.clear();
    myTargetX.clear();
    final List<FlatCard> ordered = new ArrayList<>();
    for (T item : myVisibleItems) {
      final FlatCard c = myCardByItem.get(item);
      if (c != null) {
        ordered.add(c);
      }
    }
    ordered.remove(myDrag.card);
    final int dropClamp = Math.max(0, Math.min(ordered.size(), myDrag.toIndex));
    ordered.add(dropClamp, myDrag.card);

    final Insets in = myContent.getInsets();
    if (myOrientation == Orientation.GRID) {
      final int availW = Math.max(1, myContent.getWidth() - in.left - in.right);
      final int cellW = Math.max(1, (availW - (myColumns - 1) * myItemGap) / myColumns);
      final int cellH = Math.max(1, computeGridCellHeight());
      for (int i = 0; i < ordered.size(); i++) {
        final FlatCard c = ordered.get(i);
        final int col = i % myColumns;
        final int row = i / myColumns;
        myTargetX.put(c, in.left + col * (cellW + myItemGap));
        myTargetY.put(c, in.top + row * (cellH + myItemGap));
      }
    } else {
      int y = in.top;
      for (FlatCard c : ordered) {
        myTargetY.put(c, y);
        myTargetX.put(c, in.left);
        y += c.getPreferredSize().height + myItemGap;
      }
    }
  }

  private void startDragAnimation() {
    if (myDragAnimTimer != null && myDragAnimTimer.isRunning()) {
      myDragAnimTimer.stop();
    }
    myDragAnimTimer =
        new Timer(
            ANIMATION_TICK_MS,
            e -> {
              if (myDrag == null) {
                ((Timer) e.getSource()).stop();
                return;
              }
              boolean moved = false;
              for (Map.Entry<FlatCard, Integer> entry : myTargetY.entrySet()) {
                final FlatCard c = entry.getKey();
                if (c == myDrag.card) {
                  continue;
                }
                if (animateAxis(c, entry.getValue(), myDisplacedY, c.getY())) {
                  moved = true;
                }
                final Integer tx = myTargetX.get(c);
                if (tx != null && animateAxis(c, tx, myDisplacedX, c.getX())) {
                  moved = true;
                }
              }
              if (moved) {
                myContent.revalidate();
                myContent.repaint();
              }
            });
    myDragAnimTimer.start();
  }

  /**
   * Lerps a single axis (x or y) for {@code theCard} toward {@code theTarget}; returns true if it
   * moved.
   */
  private static boolean animateAxis(
      final FlatCard theCard,
      final int theTarget,
      final Map<FlatCard, Integer> theMap,
      final int theFallback) {
    final Integer curBoxed = theMap.get(theCard);
    final int cur = curBoxed != null ? curBoxed : theFallback;
    if (cur == theTarget) {
      return false;
    }
    final int diff = theTarget - cur;
    int step = (int) Math.round(diff * 0.30);
    if (step == 0 || Math.abs(diff) <= 1) {
      step = diff;
    }
    theMap.put(theCard, cur + step);
    return true;
  }

  private void endDrag(final MouseEvent theEvent) {
    setCursor(Cursor.getDefaultCursor());
    if (myDragAnimTimer != null) {
      myDragAnimTimer.stop();
    }
    myDisplacedY.clear();
    myTargetY.clear();
    myDisplacedX.clear();
    myTargetX.clear();
    if (myDrag == null) {
      return;
    }
    // Restore the dragged card's hover cursor. If the drop triggers a model.move below, the
    // card will be rebuilt and re-configured anyway — this only matters for no-op drops where
    // the same card stays on screen.
    if (myDrag.card != null && myReorderable && myReorderHandle == ReorderHandle.WHOLE_CARD) {
      myDrag.card.setCursor(Cursors.grab());
    }
    final int fromVis = myDrag.fromIndex;
    final int toVis = myDrag.toIndex;
    final T item = myDrag.item;
    myDrag = null;
    if (fromVis < 0 || toVis < 0 || fromVis == toVis) {
      myContent.revalidate();
      myContent.repaint();
      return;
    }
    if (myModel instanceof DefaultCardListModel<T> mutable) {
      final int fromModel = indexOfInModel(item);
      final T atTarget = myVisibleItems.get(toVis);
      final int toModel = indexOfInModel(atTarget);
      if (fromModel >= 0 && toModel >= 0) {
        mutable.move(fromModel, toModel);
        fireReorder(item, fromModel, toModel);
      }
    } else {
      LOG.warning("FlatCardList: reorder requires a mutable DefaultCardListModel; drop ignored");
    }
    myContent.revalidate();
    myContent.repaint();
  }

  private int indexOfInModel(final T theItem) {
    int i = 0;
    for (T t : myModel) {
      if (t == theItem || (t != null && t.equals(theItem))) {
        return i;
      }
      i++;
    }
    return -1;
  }

  private void fireReorder(final T theItem, final int theFrom, final int theTo) {
    final CardReorderEvent<T> evt = new CardReorderEvent<>(this, theItem, theFrom, theTo);
    for (CardReorderListener<T> l : new ArrayList<>(myReorderListeners)) {
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
      if (myOrientation == Orientation.VERTICAL) {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
      }
      return super.getMaximumSize();
    }
  }

  /**
   * Layout manager for vertical mode. Stacks cards top-to-bottom with the configured gap when idle;
   * during a drag, places the dragged card under the cursor and reads the per-card displaced Y
   * (driven by the drag animation timer) for everything else.
   */
  private final class StackingLayout implements java.awt.LayoutManager {
    @Override
    public void addLayoutComponent(final String theName, final Component theComp) {
      // No-op — this layout doesn't use named constraints.
    }

    @Override
    public void removeLayoutComponent(final Component theComp) {
      myDisplacedY.remove(theComp);
      myTargetY.remove(theComp);
    }

    @Override
    public Dimension preferredLayoutSize(final Container theParent) {
      final Insets in = theParent.getInsets();
      int total = in.top + in.bottom;
      int width = 0;
      final int count = theParent.getComponentCount();
      for (int i = 0; i < count; i++) {
        final Dimension pref = theParent.getComponent(i).getPreferredSize();
        total += pref.height;
        if (i + 1 < count) {
          total += myItemGap;
        }
        width = Math.max(width, pref.width);
      }
      return new Dimension(width + in.left + in.right, total);
    }

    @Override
    public Dimension minimumLayoutSize(final Container theParent) {
      return preferredLayoutSize(theParent);
    }

    @Override
    public void layoutContainer(final Container theParent) {
      final Insets in = theParent.getInsets();
      final int width = Math.max(0, theParent.getWidth() - in.left - in.right);
      if (myDrag == null) {
        int y = in.top;
        for (int i = 0; i < theParent.getComponentCount(); i++) {
          final Component c = theParent.getComponent(i);
          final Dimension pref = c.getPreferredSize();
          c.setBounds(in.left, y, width, pref.height);
          y += pref.height + myItemGap;
        }
        return;
      }
      // Drag mode: dragged card follows the cursor (Y); others use displaced Y.
      for (int i = 0; i < theParent.getComponentCount(); i++) {
        final Component c = theParent.getComponent(i);
        final Dimension pref = c.getPreferredSize();
        if (c == myDrag.card) {
          int y = myDrag.currentPoint.y - myDrag.grabOffset.y;
          // Clamp so the card doesn't drift past the visible content area.
          final int maxY = Math.max(in.top, theParent.getHeight() - in.bottom - pref.height);
          y = Math.max(in.top, Math.min(maxY, y));
          c.setBounds(in.left, y, width, pref.height);
        } else {
          final Integer dispY = myDisplacedY.get(c);
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
   */
  private final class GridStackingLayout implements java.awt.LayoutManager {
    @Override
    public void addLayoutComponent(final String theName, final Component theComp) {
      // No-op — this layout doesn't use named constraints.
    }

    @Override
    public void removeLayoutComponent(final Component theComp) {
      myDisplacedY.remove(theComp);
      myTargetY.remove(theComp);
      myDisplacedX.remove(theComp);
      myTargetX.remove(theComp);
    }

    @Override
    public Dimension preferredLayoutSize(final Container theParent) {
      final Insets in = theParent.getInsets();
      final int count = theParent.getComponentCount();
      final int rows = (count + myColumns - 1) / Math.max(1, myColumns);
      final int cellH = computeGridCellHeight();
      int cellW = 0;
      for (int i = 0; i < count; i++) {
        cellW = Math.max(cellW, theParent.getComponent(i).getPreferredSize().width);
      }
      return new Dimension(
          cellW * myColumns + myItemGap * Math.max(0, myColumns - 1) + in.left + in.right,
          cellH * rows + myItemGap * Math.max(0, rows - 1) + in.top + in.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(final Container theParent) {
      return preferredLayoutSize(theParent);
    }

    @Override
    public void layoutContainer(final Container theParent) {
      final Insets in = theParent.getInsets();
      final int availW = Math.max(0, theParent.getWidth() - in.left - in.right);
      final int cellW = Math.max(1, (availW - (myColumns - 1) * myItemGap) / myColumns);
      final int cellH = Math.max(1, computeGridCellHeight());

      if (myDrag == null) {
        for (int i = 0; i < theParent.getComponentCount(); i++) {
          final Component c = theParent.getComponent(i);
          final int col = i % myColumns;
          final int row = i / myColumns;
          c.setBounds(
              in.left + col * (cellW + myItemGap),
              in.top + row * (cellH + myItemGap),
              cellW,
              cellH);
        }
        return;
      }

      for (int i = 0; i < theParent.getComponentCount(); i++) {
        final Component c = theParent.getComponent(i);
        if (c == myDrag.card) {
          int x = myDrag.currentPoint.x - myDrag.grabOffset.x;
          int y = myDrag.currentPoint.y - myDrag.grabOffset.y;
          final int maxX = Math.max(in.left, theParent.getWidth() - in.right - cellW);
          final int maxY = Math.max(in.top, theParent.getHeight() - in.bottom - cellH);
          x = Math.max(in.left, Math.min(maxX, x));
          y = Math.max(in.top, Math.min(maxY, y));
          c.setBounds(x, y, cellW, cellH);
        } else {
          final Integer dispX = myDisplacedX.get(c);
          final Integer dispY = myDisplacedY.get(c);
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

  // ------------------------------------------------------------- animation

  private void startFadeIn() {
    if (myFadeTimer != null && myFadeTimer.isRunning()) {
      myFadeTimer.stop();
    }
    myFadeAlpha = 0f;
    final long start = System.currentTimeMillis();
    myFadeTimer =
        new Timer(
            ANIMATION_TICK_MS,
            e -> {
              final long elapsed = System.currentTimeMillis() - start;
              myFadeAlpha = Math.min(1f, elapsed / (float) myAnimationDurationMs);
              myContent.repaint();
              if (myFadeAlpha >= 1f) {
                ((Timer) e.getSource()).stop();
              }
            });
    myFadeTimer.start();
  }

  @Override
  protected void paintChildren(final Graphics g) {
    if (myFadeAlpha >= 1f || !myAnimateChanges) {
      super.paintChildren(g);
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myFadeAlpha));
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

  private void moveFocus(final int theDelta) {
    if (myVisibleItems.isEmpty()) {
      return;
    }
    int next = myFocusedVisibleIndex + theDelta;
    if (next < 0) {
      next = 0;
    }
    if (next >= myVisibleItems.size()) {
      next = myVisibleItems.size() - 1;
    }
    myFocusedVisibleIndex = next;
    final FlatCard card = myCardByItem.get(myVisibleItems.get(next));
    if (card != null) {
      card.requestFocusInWindow();
      scrollRectToVisible(card.getBounds());
    }
  }

  private final class MoveFocus extends AbstractAction {
    private final int myDelta;

    MoveFocus(final int theDelta, final boolean theIgnored) {
      super();
      myDelta = theDelta;
    }

    @Override
    public void actionPerformed(final ActionEvent theEvent) {
      moveFocus(myDelta);
    }
  }

  private final class JumpFocus extends AbstractAction {
    private final int myTarget;

    JumpFocus(final int theTarget) {
      super();
      myTarget = theTarget;
    }

    @Override
    public void actionPerformed(final ActionEvent theEvent) {
      myFocusedVisibleIndex = -1;
      moveFocus(myTarget == 0 ? 0 : myVisibleItems.size());
    }
  }

  private final class ActivateFocused extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent theEvent) {
      if (myFocusedVisibleIndex < 0 || myFocusedVisibleIndex >= myVisibleItems.size()) {
        return;
      }
      final T item = myVisibleItems.get(myFocusedVisibleIndex);
      handleSelectionPress(item, myFocusedVisibleIndex, 0);
    }
  }

  private final class SelectAllAction extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent theEvent) {
      if (mySelectionMode != CardSelectionMode.MULTIPLE) {
        return;
      }
      mySelectionModel.setSelected(new ArrayList<>(myVisibleItems));
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
      return myVisibleItems.size();
    }

    @Override
    public javax.accessibility.Accessible getAccessibleChild(final int theIndex) {
      if (theIndex < 0 || theIndex >= myVisibleItems.size()) {
        return null;
      }
      final FlatCard card = myCardByItem.get(myVisibleItems.get(theIndex));
      if (card == null) {
        return null;
      }
      final Component c = card;
      return c instanceof Accessible a ? a : null;
    }
  }
}
