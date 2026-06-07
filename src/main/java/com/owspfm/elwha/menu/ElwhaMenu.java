package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 * The M3 Expressive <strong>vertical menu</strong> — a temporary, light-dismissed surface anchored
 * to a trigger that lists {@link ElwhaMenuItem} primitives. Built on the shared overlay host (epic
 * #298 S1): mounts at {@link javax.swing.JLayeredPane#POPUP_LAYER} above dialogs, dismisses on
 * outside-press / focus-loss / Escape, and restores focus to the trigger on an intentional close.
 *
 * <p><strong>Usage.</strong>
 *
 * <pre>{@code
 * ElwhaMenu menu = ElwhaMenu.builder()
 *     .addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Edit"))
 *     .addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"))
 *     .onClose(cause -> ...)
 *     .build();
 * trigger.addActionListener(e -> menu.open(trigger));
 * }</pre>
 *
 * <p><strong>Configuration:</strong> {@link ColorStyle#STANDARD} (surface) or {@link
 * ColorStyle#VIBRANT} (tertiary-tinted) color; {@link Layout#STANDARD} (flat) or {@link
 * Layout#GROUPED} with {@link Separator#GAP} (expressive rounded cards) or {@link
 * Separator#DIVIDER} (subtle line). A menu taller than the window scrolls with a persistent
 * scrollbar and is forced to {@code DIVIDER} (M3 forbids gaps in a scrollable menu). The container
 * tints {@code SURFACE_CONTAINER_LOW} (Standard) or {@code TERTIARY_CONTAINER} (Vibrant) at
 * Level&nbsp;3 elevation, {@link ShapeScale#MD} corners.
 *
 * <p><strong>Selection.</strong> {@link SelectionMode#NONE} (the default) is an action menu — items
 * fire and the menu closes. {@link SelectionMode#SINGLE} selects one item at a time (auto-deselects
 * the prior) and closes on select; {@link SelectionMode#MULTI} toggles items and stays open until
 * dismissed. The selected item paints the {@code TERTIARY_CONTAINER} / Vibrant {@code TERTIARY}
 * fill plus a ✓ checkmark; read selection back with {@link #getSelectedItems()} or observe it via
 * {@link Builder#onSelectionChange(Consumer)}.
 *
 * <p><strong>Trigger.</strong> The menu never mutates its trigger — it opens and closes without
 * touching the trigger's state, so the trigger is "unchanged after select" by construction. M3's
 * "trigger shows a pressed state while the menu is open" affordance is intentionally <em>not</em>
 * faked via the trigger's selected state (that corrupts a {@code SELECTABLE} toggle button, which
 * already flips its own selection on click); a faithful transient held-visual needs a dedicated
 * button API the lib doesn't have yet — a future enhancement, tracked as a known gap.
 *
 * <p><strong>One at a time.</strong> Menus are singular popovers: opening a menu by clicking
 * another trigger is a press outside the current menu, so the current menu light-dismisses on that
 * same click while the new one opens — no explicit bookkeeping, no focus hand-off churn.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenu extends AbstractElwhaMenuOverlay {

  /**
   * M3 menu container elevation. Research §I sanctions Level 2–3 ("swatch (Level 2–3)"); Level 2 is
   * used so the drop shadow reads as a light lift rather than a heavy band under the surface.
   */
  static final int ELEVATION = 2;

  /** Container corner radius in dp (research §I shape). */
  static final int CONTAINER_ARC_PX = ShapeScale.MD.px();

  /** Padding inside the container around the item column (single-slab modes). */
  static final int CONTENT_PAD_PX = 4;

  /** Transparent gap between groups in {@link Separator#GAP} mode (research §I, 2 dp token). */
  static final int GROUP_GAP_PX = 3;

  /** Min / max container content width in dp. */
  static final int MIN_WIDTH_PX = 160;

  static final int MAX_WIDTH_PX = 320;

  /** Margin reserved from the window edges when deciding whether the menu must scroll. */
  static final int VIEWPORT_MARGIN_PX = 16;

  private static final int DIVIDER_THICKNESS_PX = 1;
  private static final int DIVIDER_INSET_X_PX = 0;
  private static final int SCROLLBAR_WIDTH_PX = 14;

  private final List<List<ElwhaMenuItem>> groups;
  private final Layout layout;
  // Mutable so a submenu can inherit its parent's color style at open time when the consumer didn't
  // pin one explicitly (colorStyleExplicit). The container tint and the items both read it.
  private ColorStyle colorStyle;
  private final boolean colorStyleExplicit;
  private final SelectionMode selectionMode;
  private final Consumer<ElwhaMenuItem> onSelectionChange;
  private Separator separator;

  // Live state — non-null while shown.
  private List<JComponent> groupPanels;
  private List<ElwhaMenuItem> itemOrder;
  private MenuSurface menuSurface;
  // The open child submenu and the item that opened it (epic #322): a parent menu holds at most one
  // open submenu; opening a different one closes the prior, and the chain host tears this down when
  // the parent itself closes.
  private ElwhaMenu openChildMenu;
  private ElwhaSubMenuItem openerSubItem;
  private int focusedIndex = -1;
  // The roving focus ring is keyboard-visible (M3 focus-visible): the index tracks always, but the
  // ring paints only after keyboard navigation, not on a pointer-opened menu.
  private boolean keyboardFocusVisible;
  private boolean scrollable;

  private ElwhaMenu(final Builder b) {
    super(b.onClose);
    this.groups = b.groups;
    this.layout = b.layout;
    this.separator = b.separator;
    this.colorStyle = b.colorStyle;
    this.colorStyleExplicit = b.colorStyleExplicit;
    this.selectionMode = b.selectionMode;
    this.onSelectionChange = b.onSelectionChange;
    final boolean selectable = selectionMode != SelectionMode.NONE;
    for (final List<ElwhaMenuItem> group : groups) {
      for (final ElwhaMenuItem item : group) {
        item.setColorStyle(colorStyle);
        // Reserve the check-column up front in a selection mode so toggling never shifts the label;
        // MULTI items are checkbox-like (report CHECKED), SINGLE items radio-like (SELECTED only).
        item.setReserveLeadingColumn(selectable);
        item.setCheckable(selectionMode == SelectionMode.MULTI);
        item.addActionListener(e -> onItemActivated(item));
        if (item instanceof ElwhaSubMenuItem sub) {
          sub.attachOwner(this);
        }
      }
    }
  }

  // A submenu opens to the side of its opener item, not below a trigger (chain host §6).
  @Override
  protected boolean sideAnchored() {
    return chainParentOverlay() != null;
  }

  // Adopt the parent menu's color style when this submenu's was left at the STANDARD default
  // (consumer didn't pin one), so a Vibrant chain stays Vibrant unless a nested menu overrides it.
  void inheritColorStyle(final ColorStyle parentStyle) {
    if (colorStyleExplicit || colorStyle == parentStyle) {
      return;
    }
    this.colorStyle = parentStyle;
    for (final ElwhaMenuItem item : flatten()) {
      item.setColorStyle(parentStyle);
    }
  }

  // Hover-intent / keyboard entry points from an ElwhaSubMenuItem (epic #322 S2). Open is
  // idempotent
  // (already-open is a no-op); close collapses just this opener's submenu level.
  void requestOpenSubMenu(final ElwhaSubMenuItem opener) {
    if (isShowing()) {
      openSubMenuFor(opener);
    }
  }

  void requestCloseSubMenu(final ElwhaSubMenuItem opener) {
    if (openerSubItem == opener && openChildMenu != null) {
      openChildMenu.close(MenuDismissCause.PROGRAMMATIC);
    }
  }

  boolean isPointerInSubChain(final java.awt.Point screenPoint) {
    return screenPoint != null && chainContainsScreenPoint(screenPoint);
  }

  // The mounted surface bounds in layered-pane coordinates, or null when not shown — for placement
  // guards to assert side-anchoring without reaching into the host.
  Rectangle surfaceBounds() {
    return menuSurface != null ? menuSurface.getBounds() : null;
  }

  // Maps an item activation (mouse click or the container's Enter/Space routing) to the configured
  // SelectionMode: NONE fires-and-closes (action menu); SINGLE selects one then closes; MULTI
  // toggles and stays open. A submenu trigger never selects/closes — it opens its nested menu
  // (chain host, epic #322). A selection closes the whole chain, not just the leaf, so picking an
  // action item from inside a submenu dismisses every open level. The item's own listeners (the
  // consumer's) have already fired by now.
  private void onItemActivated(final ElwhaMenuItem item) {
    if (item instanceof ElwhaSubMenuItem sub) {
      openSubMenuFor(sub);
      return;
    }
    switch (selectionMode) {
      case NONE -> closeChain(MenuDismissCause.SELECTION);
      case SINGLE -> {
        for (final ElwhaMenuItem other : flatten()) {
          other.setSelected(other == item);
        }
        fireSelectionChange(item);
        closeChain(MenuDismissCause.SELECTION);
      }
      case MULTI -> {
        item.setSelected(!item.isSelected());
        fireSelectionChange(item);
      }
      default -> throw new AssertionError(selectionMode);
    }
  }

  // Opens (or re-targets) the submenu owned by a trigger item, as a child overlay of this menu. Any
  // other submenu already open under this level is closed first — one open submenu per level. The
  // opener is marked expanded; the chain host restores focus to it when the submenu closes
  // (onChainChildClosed).
  private void openSubMenuFor(final ElwhaSubMenuItem opener) {
    final ElwhaMenu sub = opener.getSubMenu();
    if (openChildMenu == sub && sub.isShowing()) {
      return;
    }
    if (openChildMenu != null && openChildMenu != sub) {
      openChildMenu.close(MenuDismissCause.PROGRAMMATIC);
    }
    if (openerSubItem != null && openerSubItem != opener) {
      openerSubItem.setExpanded(false);
    }
    this.openChildMenu = sub;
    this.openerSubItem = opener;
    opener.setExpanded(true);
    sub.inheritColorStyle(colorStyle);
    sub.showInChain(opener, this);
  }

  @Override
  protected void onChainChildClosed() {
    if (openerSubItem != null) {
      openerSubItem.setExpanded(false);
      final int idx = itemOrder != null ? itemOrder.indexOf(openerSubItem) : -1;
      if (idx >= 0) {
        setFocusedIndex(idx);
      }
    }
    openChildMenu = null;
    openerSubItem = null;
  }

  private void fireSelectionChange(final ElwhaMenuItem item) {
    if (onSelectionChange != null) {
      onSelectionChange.accept(item);
    }
  }

  /**
   * Starts a new fluent builder.
   *
   * @return a fresh {@link Builder}
   * @version v0.4.0
   * @since v0.4.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Opens the menu anchored to {@code anchor} (typically the trigger that fired it). Equivalent to
   * the host {@code show(anchor)}; the M3-named entry point. Returns immediately.
   *
   * @param anchor the trigger component to anchor and restore focus to
   * @version v0.4.0
   * @since v0.4.0
   */
  public void open(final Component anchor) {
    show(anchor);
  }

  /**
   * Renders the menu's surface — container + items — as a standalone, non-modal component for a
   * <em>static preview</em> (a Showcase gallery tile, documentation). There is no overlay mount,
   * light-dismiss, focus management, or entrance motion, and no item carries the roving focus ring.
   * Not a substitute for {@link #open(Component)}, which presents the menu for real. Each call
   * returns a fresh component.
   *
   * @return a non-modal render of the menu surface
   * @version v0.4.0
   * @since v0.4.0
   */
  public JComponent renderPreview() {
    final JComponent preview = createSurface();
    this.focusedIndex = -1;
    pushFocusedState();
    return preview;
  }

  /**
   * The menu's items in display order, flattened across groups.
   *
   * @return an unmodifiable view of every item
   * @version v0.4.0
   * @since v0.4.0
   */
  public List<ElwhaMenuItem> getItems() {
    final List<ElwhaMenuItem> flat = new ArrayList<>();
    for (final List<ElwhaMenuItem> group : groups) {
      flat.addAll(group);
    }
    return List.copyOf(flat);
  }

  /**
   * The selection mode this menu was built with (default {@link SelectionMode#NONE}).
   *
   * @return the selection mode
   * @version v0.4.0
   * @since v0.4.0
   */
  public SelectionMode getSelectionMode() {
    return selectionMode;
  }

  /**
   * The currently selected items, in display order. Empty in {@link SelectionMode#NONE} (an action
   * menu holds no persistent selection); at most one element in {@link SelectionMode#SINGLE}; any
   * number in {@link SelectionMode#MULTI}.
   *
   * @return an unmodifiable snapshot of the selected items
   * @version v0.4.0
   * @since v0.4.0
   */
  public List<ElwhaMenuItem> getSelectedItems() {
    final List<ElwhaMenuItem> selected = new ArrayList<>();
    for (final ElwhaMenuItem item : flatten()) {
      if (item.isSelected()) {
        selected.add(item);
      }
    }
    return List.copyOf(selected);
  }

  // ---------------------------------------------------------- surface

  @Override
  protected JComponent createSurface() {
    final int contentWidth = resolveContentWidth();
    final int columnHeight = totalColumnHeight(contentWidth);

    final int available =
        (layeredPane != null ? layeredPane.getHeight() : Integer.MAX_VALUE)
            - 2 * VIEWPORT_MARGIN_PX;
    final Insets shadow = ShadowPainter.shadowInsets(ELEVATION);
    final int chromeV = 2 * (shadow.top + CONTENT_PAD_PX);
    this.scrollable = columnHeight + chromeV > available;
    if (scrollable && separator == Separator.GAP) {
      // M3: gaps are unsupported in a scrollable menu — force the subtle divider.
      this.separator = Separator.DIVIDER;
    }

    this.groupPanels = new ArrayList<>();
    final JComponent column = buildColumn(contentWidth);

    final JComponent content;
    if (scrollable) {
      final JScrollPane scroll =
          new JScrollPane(
              column,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setOpaque(false);
      scroll.getViewport().setOpaque(false);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      final int bodyH = Math.max(ElwhaMenuItem.MIN_TARGET_PX, available - chromeV);
      scroll.setPreferredSize(new Dimension(contentWidth + SCROLLBAR_WIDTH_PX, bodyH));
      content = scroll;
    } else {
      content = column;
    }

    final boolean gapCards = layout == Layout.GROUPED && separator == Separator.GAP && !scrollable;
    this.menuSurface = new MenuSurface(content, gapCards);

    this.itemOrder = flatten();
    this.focusedIndex = itemOrder.isEmpty() ? -1 : 0;
    this.keyboardFocusVisible = false;
    pushFocusedState();
    return menuSurface;
  }

  @Override
  protected void clearTransientState() {
    groupPanels = null;
    itemOrder = null;
    menuSurface = null;
    focusedIndex = -1;
  }

  @Override
  protected Component initialFocusTarget() {
    return menuSurface;
  }

  private int resolveContentWidth() {
    int max = 0;
    for (final List<ElwhaMenuItem> group : groups) {
      for (final ElwhaMenuItem item : group) {
        max = Math.max(max, item.getPreferredSize().width);
      }
    }
    return Math.max(MIN_WIDTH_PX, Math.min(MAX_WIDTH_PX, max));
  }

  private int totalColumnHeight(final int contentWidth) {
    int h = 0;
    boolean first = true;
    for (final List<ElwhaMenuItem> group : groups) {
      if (!first) {
        h += layout == Layout.GROUPED ? separatorGapHeight() : 0;
      }
      first = false;
      for (final ElwhaMenuItem item : group) {
        h += item.getPreferredSize().height;
      }
    }
    return h;
  }

  private int separatorGapHeight() {
    return separator == Separator.GAP ? GROUP_GAP_PX : DIVIDER_THICKNESS_PX + 2 * CONTENT_PAD_PX;
  }

  // Builds the vertical item column from group panels, inserting gap/divider separators between
  // groups (GROUPED). STANDARD collapses to a single flat group.
  private JComponent buildColumn(final int contentWidth) {
    final JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

    final List<List<ElwhaMenuItem>> effective =
        layout == Layout.STANDARD ? List.of(flatten()) : groups;

    boolean first = true;
    for (final List<ElwhaMenuItem> group : effective) {
      if (!first && layout == Layout.GROUPED) {
        column.add(separatorComponent(contentWidth));
      }
      first = false;
      final JPanel groupPanel = new JPanel();
      groupPanel.setOpaque(false);
      groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
      groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      for (final ElwhaMenuItem item : group) {
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        groupPanel.add(item);
      }
      groupPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
      column.add(groupPanel);
      groupPanels.add(groupPanel);
    }
    column.setPreferredSize(new Dimension(contentWidth, totalColumnHeight(contentWidth)));
    column.setMaximumSize(new Dimension(contentWidth, Integer.MAX_VALUE));
    return column;
  }

  private List<ElwhaMenuItem> flatten() {
    final List<ElwhaMenuItem> flat = new ArrayList<>();
    for (final List<ElwhaMenuItem> group : groups) {
      flat.addAll(group);
    }
    return flat;
  }

  private Component separatorComponent(final int contentWidth) {
    if (separator == Separator.GAP) {
      return Box.createVerticalStrut(GROUP_GAP_PX);
    }
    final JPanel wrap = new JPanel();
    wrap.setOpaque(false);
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
    wrap.add(Box.createVerticalStrut(CONTENT_PAD_PX));
    final JPanel line =
        new JPanel() {
          @Override
          protected void paintComponent(final Graphics g) {
            g.setColor(ColorRole.OUTLINE_VARIANT.resolve());
            g.fillRect(DIVIDER_INSET_X_PX, 0, getWidth() - 2 * DIVIDER_INSET_X_PX, getHeight());
          }
        };
    line.setOpaque(false);
    line.setPreferredSize(new Dimension(contentWidth, DIVIDER_THICKNESS_PX));
    line.setMaximumSize(new Dimension(Integer.MAX_VALUE, DIVIDER_THICKNESS_PX));
    line.setAlignmentX(Component.LEFT_ALIGNMENT);
    wrap.add(line);
    wrap.add(Box.createVerticalStrut(CONTENT_PAD_PX));
    wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
    wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, separatorGapHeight()));
    return wrap;
  }

  @Override
  protected String accessibleName() {
    return "Menu";
  }

  // ------------------------------------------------ keyboard + roving focus

  // Esc is bound by the host base; this adds the in-menu navigation. The menu surface is the single
  // focus owner (items are non-focusable, §X) and carries a roving "focused" index whose ring is
  // painted on the active item.
  @Override
  protected void installKeyBindings() {
    super.installKeyBindings();
    final InputMap im = menuSurface.getInputMap(JComponent.WHEN_FOCUSED);
    bind(im, KeyEvent.VK_DOWN, "menu.next", () -> moveFocus(1));
    bind(im, KeyEvent.VK_UP, "menu.prev", () -> moveFocus(-1));
    bind(im, KeyEvent.VK_HOME, "menu.first", () -> setFocusedIndex(0));
    bind(im, KeyEvent.VK_END, "menu.last", () -> setFocusedIndex(itemOrder.size() - 1));
    bind(im, KeyEvent.VK_ENTER, "menu.activate", this::activateFocused);
    bind(im, KeyEvent.VK_SPACE, "menu.activateSpace", this::activateFocused);
    // Submenu chain (epic #322): Right opens the focused item's submenu (if it has one) and moves
    // focus into it; Left closes this level back to its opener when this menu is itself a submenu.
    bind(im, KeyEvent.VK_RIGHT, "menu.openSub", this::openFocusedSubMenu);
    bind(im, KeyEvent.VK_LEFT, "menu.closeSub", this::closeIfSubMenu);
    menuSurface.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyTyped(final KeyEvent e) {
            final char c = e.getKeyChar();
            if (Character.isLetterOrDigit(c)) {
              typeAhead(c);
            }
          }
        });
  }

  private void bind(final InputMap im, final int keyCode, final String name, final Runnable body) {
    im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
    menuSurface.getActionMap().put(name, action(body));
  }

  int focusedIndex() {
    return focusedIndex;
  }

  JComponent focusComponent() {
    return menuSurface;
  }

  void moveFocus(final int delta) {
    if (itemOrder == null || itemOrder.isEmpty()) {
      return;
    }
    final int n = itemOrder.size();
    final int start = focusedIndex < 0 ? 0 : focusedIndex;
    setFocusedIndex(((start + delta) % n + n) % n);
  }

  void setFocusedIndex(final int index) {
    if (itemOrder == null || itemOrder.isEmpty()) {
      return;
    }
    // Reached only from keyboard navigation (Up/Down/Home/End/type-ahead) — arm the focus ring.
    this.keyboardFocusVisible = true;
    this.focusedIndex = Math.max(0, Math.min(index, itemOrder.size() - 1));
    pushFocusedState();
  }

  private void pushFocusedState() {
    if (itemOrder == null) {
      return;
    }
    for (int i = 0; i < itemOrder.size(); i++) {
      itemOrder.get(i).setFocused(keyboardFocusVisible && i == focusedIndex);
    }
    if (focusedIndex >= 0 && focusedIndex < itemOrder.size()) {
      final ElwhaMenuItem item = itemOrder.get(focusedIndex);
      if (item.getWidth() > 0 && item.getHeight() > 0) {
        item.scrollRectToVisible(new Rectangle(0, 0, item.getWidth(), item.getHeight()));
      }
    }
  }

  void activateFocused() {
    if (itemOrder == null || focusedIndex < 0 || focusedIndex >= itemOrder.size()) {
      return;
    }
    itemOrder.get(focusedIndex).activate(0);
  }

  // Right-arrow: open the focused item's submenu (a no-op on a non-submenu item). Routes through
  // the
  // same activation path as click so opening is identical regardless of input source.
  private void openFocusedSubMenu() {
    if (itemOrder == null || focusedIndex < 0 || focusedIndex >= itemOrder.size()) {
      return;
    }
    if (itemOrder.get(focusedIndex) instanceof ElwhaSubMenuItem sub) {
      openSubMenuFor(sub);
    }
  }

  // Left-arrow: when this menu is itself an open submenu, close this level back to its opener; the
  // chain host restores focus to the opener item. A no-op on a root menu (V1 left Left unbound).
  private void closeIfSubMenu() {
    if (chainParentOverlay() != null) {
      close(MenuDismissCause.ESCAPE);
    }
  }

  void typeAhead(final char c) {
    if (itemOrder == null || itemOrder.isEmpty()) {
      return;
    }
    final String target = String.valueOf(Character.toLowerCase(c));
    final int n = itemOrder.size();
    final int start = focusedIndex < 0 ? 0 : focusedIndex;
    for (int k = 1; k <= n; k++) {
      final int i = (start + k) % n;
      final String label = itemOrder.get(i).getLabel();
      if (label != null && label.toLowerCase(Locale.ROOT).startsWith(target)) {
        setFocusedIndex(i);
        return;
      }
    }
  }

  // Container tint by color style: Standard = SURFACE_CONTAINER_LOW, Vibrant = TERTIARY_CONTAINER
  // (research §K row 4). Items resolve their own content/selected roles off the same style.
  private Color containerColor() {
    return (colorStyle == ColorStyle.VIBRANT
            ? ColorRole.TERTIARY_CONTAINER
            : ColorRole.SURFACE_CONTAINER_LOW)
        .resolve();
  }

  // The painted menu surface: drop shadow + tinted container (one slab, or per-group rounded cards
  // in GAP mode), with the item column inset by the shadow reserve + content padding.
  private final class MenuSurface extends JPanel {

    private final boolean gapCards;

    MenuSurface(final JComponent content, final boolean gapCards) {
      super(new java.awt.BorderLayout());
      this.gapCards = gapCards;
      setOpaque(false);
      setFocusable(true);
      setFocusTraversalKeysEnabled(false);
      final Insets shadow = ShadowPainter.shadowInsets(ELEVATION);
      final int pad = gapCards ? 0 : CONTENT_PAD_PX;
      setBorder(
          BorderFactory.createEmptyBorder(
              shadow.top + pad, shadow.left + pad, shadow.bottom + pad, shadow.right + pad));
      add(content, java.awt.BorderLayout.CENTER);
      getAccessibleContext().setAccessibleName("Menu");
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext =
            new AccessibleJPanel() {
              @Override
              public javax.accessibility.AccessibleRole getAccessibleRole() {
                return javax.accessibility.AccessibleRole.POPUP_MENU;
              }
            };
      }
      return accessibleContext;
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Insets shadow = ShadowPainter.shadowInsets(ELEVATION);
        final int bx = shadow.left;
        final int by = shadow.top;
        final int bw = getWidth() - shadow.left - shadow.right;
        final int bh = getHeight() - shadow.top - shadow.bottom;

        if (gapCards) {
          // Each group is its own floating card: shadow + fill per card, so the transparent gap
          // between cards shows only soft shadow falloff — never the dark interior a single
          // full-bounds union shadow would leave bleeding through the gap.
          paintGroupCards(g2, bx, bw);
        } else {
          final Graphics2D sg = (Graphics2D) g2.create();
          sg.translate(bx, by);
          ShadowPainter.paint(sg, bw, bh, CONTAINER_ARC_PX * 2, ELEVATION);
          sg.dispose();
          g2.setColor(containerColor());
          g2.fill(
              new RoundRectangle2D.Float(
                  bx, by, bw, bh, CONTAINER_ARC_PX * 2f, CONTAINER_ARC_PX * 2f));
        }
      } finally {
        g2.dispose();
      }
    }

    private void paintGroupCards(final Graphics2D g2, final int bx, final int bw) {
      if (groupPanels == null) {
        return;
      }
      // Pass 1: every card's shadow, before any fill, so a card's shadow can't darken an adjacent
      // card's body — only the inter-card gap and the outer edges keep the shadow.
      for (final JComponent gp : groupPanels) {
        if (gp.getParent() == null) {
          continue;
        }
        final Rectangle r = SwingUtilities.convertRectangle(gp.getParent(), gp.getBounds(), this);
        final Graphics2D sg = (Graphics2D) g2.create();
        sg.translate(bx, r.y);
        ShadowPainter.paint(sg, bw, r.height, CONTAINER_ARC_PX * 2, ELEVATION);
        sg.dispose();
      }
      // Pass 2: every card's fill.
      g2.setColor(containerColor());
      for (final JComponent gp : groupPanels) {
        if (gp.getParent() == null) {
          continue;
        }
        final Rectangle r = SwingUtilities.convertRectangle(gp.getParent(), gp.getBounds(), this);
        g2.fill(
            new RoundRectangle2D.Float(
                bx, r.y, bw, r.height, CONTAINER_ARC_PX * 2f, CONTAINER_ARC_PX * 2f));
      }
    }
  }

  // ------------------------------------------------------------ builder

  /**
   * Fluent builder for {@link ElwhaMenu}. Add items with {@link #addItem(ElwhaMenuItem)}; start a
   * new group ({@link Layout#GROUPED}) with {@link #addGroup()}.
   *
   * @author Charles Bryan (cfb3@uw.edu)
   * @version v0.4.0
   * @since v0.4.0
   */
  public static final class Builder {

    private final List<List<ElwhaMenuItem>> groups = new ArrayList<>();
    private Layout layout = Layout.STANDARD;
    private Separator separator = Separator.GAP;
    private ColorStyle colorStyle = ColorStyle.STANDARD;
    private boolean colorStyleExplicit;
    private SelectionMode selectionMode = SelectionMode.NONE;
    private Consumer<ElwhaMenuItem> onSelectionChange;
    private Consumer<MenuDismissCause> onClose;

    private Builder() {
      groups.add(new ArrayList<>());
    }

    /**
     * Appends an item to the current group.
     *
     * @param item the item; required
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder addItem(final ElwhaMenuItem item) {
      groups.get(groups.size() - 1).add(Objects.requireNonNull(item, "item"));
      return this;
    }

    /**
     * Starts a new group. Only meaningful under {@link Layout#GROUPED}; setting it also switches
     * the layout to {@code GROUPED}.
     *
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder addGroup() {
      this.layout = Layout.GROUPED;
      groups.add(new ArrayList<>());
      return this;
    }

    /**
     * Sets the layout (default {@link Layout#STANDARD}).
     *
     * @param layout the layout
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder layout(final Layout layout) {
      this.layout = Objects.requireNonNull(layout, "layout");
      return this;
    }

    /**
     * Sets the group separator style (default {@link Separator#GAP}); only applies under {@link
     * Layout#GROUPED}.
     *
     * @param separator the separator style
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder separator(final Separator separator) {
      this.separator = Objects.requireNonNull(separator, "separator");
      return this;
    }

    /**
     * Sets the color style — {@link ColorStyle#STANDARD} (default, surface) or {@link
     * ColorStyle#VIBRANT} (tertiary-tinted, higher emphasis).
     *
     * @param colorStyle the color style
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder colorStyle(final ColorStyle colorStyle) {
      this.colorStyle = Objects.requireNonNull(colorStyle, "colorStyle");
      this.colorStyleExplicit = true;
      return this;
    }

    /**
     * Sets the selection mode (default {@link SelectionMode#NONE} — an action menu). {@link
     * SelectionMode#SINGLE} selects one item and closes on select; {@link SelectionMode#MULTI}
     * toggles items and stays open. Set an initial selection by calling {@link
     * ElwhaMenuItem#setSelected(boolean)} on items before {@link #build()}.
     *
     * @param selectionMode the selection mode
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder selectionMode(final SelectionMode selectionMode) {
      this.selectionMode = Objects.requireNonNull(selectionMode, "selectionMode");
      return this;
    }

    /**
     * Sets a selection-change callback, invoked with the item whose selection just toggled (read
     * the new state via {@link ElwhaMenuItem#isSelected()} or the menu's {@link
     * #getSelectedItems()}). Fires only in {@link SelectionMode#SINGLE} / {@link
     * SelectionMode#MULTI}; never in {@code NONE}.
     *
     * @param onSelectionChange the selection-change callback, or {@code null}
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder onSelectionChange(final Consumer<ElwhaMenuItem> onSelectionChange) {
      this.onSelectionChange = onSelectionChange;
      return this;
    }

    /**
     * Sets the close hook, fired after teardown with the {@link MenuDismissCause}.
     *
     * @param onClose the close callback, or {@code null}
     * @return this builder
     * @version v0.4.0
     * @since v0.4.0
     */
    public Builder onClose(final Consumer<MenuDismissCause> onClose) {
      this.onClose = onClose;
      return this;
    }

    /**
     * Builds the menu.
     *
     * @return a new {@link ElwhaMenu}
     * @throws IllegalStateException if no items were added
     * @version v0.4.0
     * @since v0.4.0
     */
    public ElwhaMenu build() {
      groups.removeIf(List::isEmpty);
      if (groups.isEmpty()) {
        throw new IllegalStateException("menu has no items");
      }
      return new ElwhaMenu(this);
    }
  }
}
