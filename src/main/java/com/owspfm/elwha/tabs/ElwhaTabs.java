package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The M3 tab bar — a dedicated Swing container of {@link ElwhaTab} children that organizes groups
 * of related content at the same level of hierarchy. Ships both M3 variants ({@link
 * TabsVariant#PRIMARY} / {@link TabsVariant#SECONDARY}); the variant belongs to the bar and is
 * stamped onto every tab as it is added.
 *
 * <p><strong>The bar owns the chrome that spans tabs</strong> (design §2): the {@link
 * ColorRole#SURFACE} container fill, the full-width 1&nbsp;px {@link ColorRole#OUTLINE_VARIANT}
 * divider along the bottom edge, and the single active indicator painted over the divider —
 * 3&nbsp;px tall with rounded top corners hugging the content width for primary tabs, 2&nbsp;px
 * square spanning the full tab width for secondary tabs. Tabs paint only their own content.
 *
 * <p><strong>Selection is exactly-one-mandatory</strong> (the {@code SINGLE_MANDATORY} tab-strip
 * semantics): the first tab added auto-activates silently; removing the active tab re-activates
 * the first remaining tab; programmatic activation flows through {@link #setActiveTabIndex(int)} /
 * {@link #setActiveTab(ElwhaTab)}. {@link ChangeListener}s fire on <em>any</em> active-tab change
 * (programmatic included) — except the initial silent auto-activation, matching material-web.
 *
 * <p><strong>Content panels are consumer composition</strong> — M3 ships the bar; pair it with a
 * {@link java.awt.CardLayout} panel:
 *
 * <pre>{@code
 * ElwhaTabs tabs = ElwhaTabs.primary();
 * tabs.addTab("Video");
 * tabs.addTab("Photos");
 * tabs.addTab("Audio");
 *
 * JPanel pages = new JPanel(new CardLayout());
 * pages.add(videoPanel, "0");
 * pages.add(photosPanel, "1");
 * pages.add(audioPanel, "2");
 * tabs.addChangeListener(e ->
 *     ((CardLayout) pages.getLayout()).show(pages, String.valueOf(tabs.getActiveTabIndex())));
 * }</pre>
 *
 * <p>Design: {@code docs/research/elwha-tabs-design.md}; research: {@code elwha-tabs-research.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaTabs extends JComponent implements Accessible {

  static final int BAR_HEIGHT_INLINE_PX = 48;
  static final int DIVIDER_THICKNESS_PX = 1;
  static final int PRIMARY_INDICATOR_HEIGHT_PX = 3;
  static final int PRIMARY_INDICATOR_CORNER_RADIUS_PX = 3;
  static final int SECONDARY_INDICATOR_HEIGHT_PX = 2;
  static final int INDICATOR_SLIDE_MS = 250;
  static final int SCROLLABLE_MIN_TAB_WIDTH_PX = 72;
  static final int SCROLLABLE_MAX_TAB_WIDTH_PX = 264;
  static final int SCROLL_MARGIN_PX = 48;

  private final TabsVariant variant;
  private final List<ElwhaTab> tabs = new ArrayList<>();
  private final List<ChangeListener> changeListeners = new ArrayList<>();

  private final MorphAnimator slideAnimator = new MorphAnimator(this, INDICATOR_SLIDE_MS);
  private Rectangle slideFromRect;

  private final MorphAnimator scrollAnimator = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);
  private TabMode tabMode = TabMode.FIXED;
  private int scrollOffset;
  private int scrollFrom;
  private int scrollTo;

  private boolean autoActivate;
  private int activeTabIndex = -1;

  /**
   * Constructs a {@link TabsVariant#PRIMARY} tab bar.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTabs() {
    this(TabsVariant.PRIMARY);
  }

  /**
   * Constructs a tab bar of the given variant.
   *
   * @param variant the M3 variant; required
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTabs(final TabsVariant variant) {
    this.variant = Objects.requireNonNull(variant, "variant");
    setOpaque(true);
    setFocusable(false);
    // The roving tab stop (research §I): the bar's traversal cycle exposes exactly one tab — the
    // focused one, else the active one — so Tab enters the bar on the right tab and leaves it on
    // the next press; arrows move within (the nav-rail mechanism).
    setFocusTraversalPolicyProvider(true);
    setFocusTraversalPolicy(new TabsFocusTraversalPolicy());
    scrollAnimator.addProgressListener(
        () -> {
          final float t = Easing.STANDARD.ease(scrollAnimator.progress());
          scrollOffset = Math.round(scrollFrom + (scrollTo - scrollFrom) * t);
          doLayout();
        });
    addMouseWheelListener(
        e -> {
          if (tabMode != TabMode.SCROLLABLE || !isEnabled()) {
            return;
          }
          setScrollOffset(scrollOffset + (int) Math.round(e.getPreciseWheelRotation() * 30));
        });
  }

  /**
   * Constructs a primary tab bar — main content destinations under a top app bar.
   *
   * @return a new {@link TabsVariant#PRIMARY} bar
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTabs primary() {
    return new ElwhaTabs(TabsVariant.PRIMARY);
  }

  /**
   * Constructs a secondary tab bar — hierarchy within a content area.
   *
   * @return a new {@link TabsVariant#SECONDARY} bar
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTabs secondary() {
    return new ElwhaTabs(TabsVariant.SECONDARY);
  }

  /**
   * The bar's M3 variant.
   *
   * @return the variant, never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public TabsVariant getVariant() {
    return variant;
  }

  // ------------------------------------------------------------------- tabs

  /**
   * Adds a tab to the trailing end of the bar, stamping the bar's variant onto it. The first tab
   * added auto-activates silently (no {@link ChangeListener} event) — the bar always has exactly
   * one active tab while it has children.
   *
   * @param tab the tab to add; required
   * @return {@code tab}, for chaining
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTab addTab(final ElwhaTab tab) {
    Objects.requireNonNull(tab, "tab");
    tab.setVariant(variant);
    tab.setEnabled(isEnabled());
    tabs.add(tab);
    add(tab);
    installKeyboardNavigation(tab);
    if (activeTabIndex < 0) {
      activate(0, true);
    }
    revalidate();
    if (getWidth() > 0) {
      doLayout();
      keepActiveVisible();
    }
    repaint();
    return tab;
  }

  /**
   * Convenience: creates a label-only tab via {@link ElwhaTab#of(String)} and adds it.
   *
   * @param label the tab label; required
   * @return the created tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTab addTab(final String label) {
    return addTab(ElwhaTab.of(label));
  }

  /**
   * Removes a tab from the bar. Removing the active tab re-activates the first remaining tab
   * (firing {@link ChangeListener}s — the selection did change); removing a tab before the active
   * one keeps the same tab active.
   *
   * @param tab the tab to remove; unknown tabs are ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeTab(final ElwhaTab tab) {
    final int index = tabs.indexOf(tab);
    if (index < 0) {
      return;
    }
    tabs.remove(index);
    remove(tab);
    uninstallKeyboardNavigation(tab);
    if (index == activeTabIndex) {
      activeTabIndex = -1;
      tab.setActive(false);
      if (!tabs.isEmpty()) {
        activate(0, false);
      }
    } else if (index < activeTabIndex) {
      activeTabIndex--;
    }
    revalidate();
    if (getWidth() > 0) {
      doLayout();
      keepActiveVisible();
    }
    repaint();
  }

  /**
   * Whether moving the keyboard focus between tabs also activates them (the material-web {@code
   * auto-activate} attribute). Off by default — arrows move focus only; Enter/Space activate.
   *
   * @param autoActivate {@code true} to select on focus move
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setAutoActivate(final boolean autoActivate) {
    this.autoActivate = autoActivate;
  }

  /**
   * Whether keyboard focus moves auto-activate tabs.
   *
   * @return the auto-activate flag
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isAutoActivate() {
    return autoActivate;
  }

  /**
   * Switches the bar between {@link TabMode#FIXED} equal-width layout and the {@link
   * TabMode#SCROLLABLE} content-width scrolling strip. Switching resets the scroll offset and then
   * keeps the active tab visible.
   *
   * @param tabMode the layout mode; required
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTabMode(final TabMode tabMode) {
    Objects.requireNonNull(tabMode, "tabMode");
    if (this.tabMode == tabMode) {
      return;
    }
    this.tabMode = tabMode;
    scrollAnimator.stop();
    scrollOffset = 0;
    revalidate();
    doLayout();
    keepActiveVisible();
    repaint();
  }

  /**
   * The bar's layout mode.
   *
   * @return the mode, never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public TabMode getTabMode() {
    return tabMode;
  }

  /**
   * Scrolls the strip, if overflowing, so the given tab is visible with the M3 48&nbsp;px margin —
   * tweened (300&nbsp;ms standard) while displayable, snapped otherwise. No-op in {@link
   * TabMode#FIXED} or for tabs that are not children of this bar; wheel input cancels an in-flight
   * tween.
   *
   * @param tab the tab to scroll to
   * @version v0.4.0
   * @since v0.4.0
   */
  public void scrollToTab(final ElwhaTab tab) {
    if (tabMode != TabMode.SCROLLABLE || !tabs.contains(tab) || getWidth() <= 0) {
      return;
    }
    final int target = clampScroll(scrollTargetFor(tab));
    if (target == scrollOffset) {
      return;
    }
    if (!isDisplayable()) {
      setScrollOffset(target);
      return;
    }
    scrollFrom = scrollOffset;
    scrollTo = target;
    scrollAnimator.snapTo(0f);
    scrollAnimator.start();
    if (scrollAnimator.progress() >= 1f) {
      setScrollOffset(target);
    }
  }

  // The material-web scroll-to formula (research §I): keep the tab visible with SCROLL_MARGIN_PX
  // of its neighbors showing on either side. Works in logical content coordinates so the same
  // formula serves both component orientations.
  private int scrollTargetFor(final ElwhaTab tab) {
    final int tabOffset = contentXOf(tab);
    final int min = tabOffset - SCROLL_MARGIN_PX;
    final int max = tabOffset + tab.getWidth() - getWidth() + SCROLL_MARGIN_PX;
    return Math.min(min, Math.max(max, scrollOffset));
  }

  // A tab's position in logical content coordinates (0 = the leading edge of the first tab),
  // derived from its on-screen bounds and the current offset under either orientation.
  private int contentXOf(final ElwhaTab tab) {
    if (getComponentOrientation().isLeftToRight()) {
      return tab.getX() + scrollOffset;
    }
    return getWidth() - tab.getX() - tab.getWidth() + scrollOffset;
  }

  private int clampScroll(final int offset) {
    return Math.max(0, Math.min(offset, maxScroll()));
  }

  private int maxScroll() {
    if (tabMode != TabMode.SCROLLABLE) {
      return 0;
    }
    return Math.max(0, scrollableContentWidth() - getWidth());
  }

  private int scrollableContentWidth() {
    int width = 0;
    for (ElwhaTab tab : tabs) {
      width += clampedScrollableWidth(tab);
    }
    return width;
  }

  private static int clampedScrollableWidth(final ElwhaTab tab) {
    return Math.max(
        SCROLLABLE_MIN_TAB_WIDTH_PX,
        Math.min(tab.getPreferredSize().width, SCROLLABLE_MAX_TAB_WIDTH_PX));
  }

  // Direct (non-tweened) scroll: wheel input and snap paths. Cancels any in-flight tween.
  void setScrollOffset(final int offset) {
    scrollAnimator.stop();
    final int clamped = clampScroll(offset);
    if (clamped == scrollOffset) {
      return;
    }
    scrollOffset = clamped;
    doLayout();
    repaint();
  }

  int getScrollOffset() {
    return scrollOffset;
  }

  private void keepActiveVisible() {
    final ElwhaTab active = getActiveTab();
    if (active != null && tabMode == TabMode.SCROLLABLE && getWidth() > 0) {
      setScrollOffset(clampScroll(scrollTargetFor(active)));
    }
  }

  /**
   * The number of tabs in the bar.
   *
   * @return the tab count
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getTabCount() {
    return tabs.size();
  }

  /**
   * The tab at the given index.
   *
   * @param index the tab index
   * @return the tab
   * @throws IndexOutOfBoundsException if the index is out of range
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTab getTabAt(final int index) {
    return tabs.get(index);
  }

  // -------------------------------------------------------------- selection

  /**
   * The index of the active tab, or {@code -1} only while the bar has no tabs.
   *
   * @return the active tab index
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getActiveTabIndex() {
    return activeTabIndex;
  }

  /**
   * Activates the tab at the given index. Fires {@link ChangeListener}s on an actual change;
   * activating the already-active index is a no-op. Out-of-range indices are ignored
   * (material-web parity).
   *
   * @param index the tab index to activate
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setActiveTabIndex(final int index) {
    if (index < 0 || index >= tabs.size()) {
      return;
    }
    activate(index, false);
  }

  /**
   * The active tab, or {@code null} only while the bar has no tabs.
   *
   * @return the active tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaTab getActiveTab() {
    return activeTabIndex >= 0 ? tabs.get(activeTabIndex) : null;
  }

  /**
   * Activates the given tab. Tabs that are not children of this bar are ignored.
   *
   * @param tab the tab to activate
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setActiveTab(final ElwhaTab tab) {
    setActiveTabIndex(tabs.indexOf(tab));
  }

  /**
   * Adds a listener notified on any active-tab change — programmatic or user-driven — except the
   * initial silent auto-activation of the first tab added.
   *
   * @param listener the listener to add; null is ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addChangeListener(final ChangeListener listener) {
    if (listener != null) {
      changeListeners.add(listener);
    }
  }

  /**
   * Removes a previously added change listener.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    changeListeners.remove(listener);
  }

  /**
   * Enables or disables the whole bar, cascading to every tab: content paints at the disabled
   * opacity and all interaction is off. There is no per-tab disabled — M3 defines none for tabs
   * (design §10).
   *
   * @param enabled the new enabled state
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    for (ElwhaTab tab : tabs) {
      tab.setEnabled(enabled);
    }
    repaint();
  }

  // A user gesture (click; keyboard with S6) on a tab — activates it and fires that tab's
  // ActionListeners. A gesture on the already-active tab is a no-op (material-web parity).
  void userActivate(final ElwhaTab tab, final int modifiers) {
    if (!isEnabled()) {
      return;
    }
    final int index = tabs.indexOf(tab);
    if (index < 0 || index == activeTabIndex) {
      return;
    }
    activate(index, false);
    tab.fireAction(modifiers);
  }

  private void activate(final int index, final boolean silent) {
    if (index == activeTabIndex) {
      return;
    }
    // FLIP-style slide (research §I): freeze the indicator's current rect as the slide origin,
    // then animate toward the (live) rest rect of the new active tab — recomputing the
    // destination each paint self-corrects across mid-slide relayouts. Snap when there is no
    // previous tab (initial auto-activation), the bar is not displayable, or it has no geometry
    // yet; reduced motion snaps inside MorphAnimator.start().
    final Rectangle from =
        activeTabIndex >= 0 && isDisplayable() && getWidth() > 0 ? currentIndicatorRect() : null;
    activeTabIndex = index;
    for (int i = 0; i < tabs.size(); i++) {
      tabs.get(i).setActive(i == index);
    }
    slideFromRect = from;
    if (from != null) {
      slideAnimator.snapTo(0f);
      slideAnimator.start();
    } else {
      slideAnimator.snapTo(1f);
    }
    if (!silent) {
      fireChange();
    }
    scrollToTab(getActiveTab());
    repaint();
  }

  private void fireChange() {
    final ChangeEvent event = new ChangeEvent(this);
    for (ChangeListener l : new ArrayList<>(changeListeners)) {
      l.stateChanged(event);
    }
  }

  // ------------------------------------------------------------------ layout

  @Override
  public void doLayout() {
    final int count = tabs.size();
    if (count == 0) {
      return;
    }
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final int height = getHeight();
    final int width = getWidth();
    if (tabMode == TabMode.SCROLLABLE) {
      scrollOffset = clampScroll(scrollOffset);
      int x = ltr ? -scrollOffset : width + scrollOffset;
      for (ElwhaTab tab : tabs) {
        final int w = clampedScrollableWidth(tab);
        final int tabHeight = Math.min(tab.getPreferredSize().height, height);
        if (ltr) {
          tab.setBounds(x, height - tabHeight, w, tabHeight);
          x += w;
        } else {
          x -= w;
          tab.setBounds(x, height - tabHeight, w, tabHeight);
        }
      }
      return;
    }
    final int base = width / count;
    final int remainder = width % count;
    int x = ltr ? 0 : width;
    for (int i = 0; i < count; i++) {
      final ElwhaTab tab = tabs.get(i);
      final int w = base + (i < remainder ? 1 : 0);
      final int tabHeight = Math.min(tab.getPreferredSize().height, height);
      if (ltr) {
        tab.setBounds(x, height - tabHeight, w, tabHeight);
        x += w;
      } else {
        x -= w;
        tab.setBounds(x, height - tabHeight, w, tabHeight);
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    int width = 0;
    int height = BAR_HEIGHT_INLINE_PX;
    for (ElwhaTab tab : tabs) {
      final Dimension pref = tab.getPreferredSize();
      width += tabMode == TabMode.SCROLLABLE ? clampedScrollableWidth(tab) : pref.width;
      height = Math.max(height, pref.height);
    }
    return new Dimension(width, height);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    g.setColor(ColorRole.SURFACE.resolve());
    g.fillRect(0, 0, getWidth(), getHeight());
  }

  // The divider and the single active indicator paint AFTER the children so they always sit on
  // top — and because the tabs are non-opaque, any child-initiated repaint dirties this (opaque)
  // bar, re-running this hook. This is the §2 architecture lock: one indicator, container-owned.
  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintDivider(g2);
      paintIndicator(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintDivider(final Graphics2D g2) {
    g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
    g2.fillRect(0, getHeight() - DIVIDER_THICKNESS_PX, getWidth(), DIVIDER_THICKNESS_PX);
  }

  private void paintIndicator(final Graphics2D g2) {
    final Rectangle rect = currentIndicatorRect();
    if (rect == null) {
      return;
    }
    g2.setColor(ColorRole.PRIMARY.resolve());
    if (variant == TabsVariant.PRIMARY) {
      // Top corners rounded, bottom square: fill a taller round-rect clipped to the indicator
      // band so the bottom corners fall outside the clip.
      final Graphics2D c = (Graphics2D) g2.create();
      try {
        c.clip(rect);
        final int arc = PRIMARY_INDICATOR_CORNER_RADIUS_PX * 2;
        c.fill(new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height + arc, arc, arc));
      } finally {
        c.dispose();
      }
    } else {
      g2.fill(rect);
    }
  }

  // The indicator rect as painted right now, in bar coordinates: the active tab's rest rect at
  // rest, or the x+width interpolation (eased EMPHASIZED, design §6) from slideFromRect toward
  // the live rest rect mid-slide.
  Rectangle currentIndicatorRect() {
    final ElwhaTab active = getActiveTab();
    if (active == null) {
      return null;
    }
    final Rectangle rest = indicatorRestRect(active);
    if (slideFromRect == null || slideAnimator.progress() >= 1f) {
      return rest;
    }
    final float t = Easing.EMPHASIZED.ease(slideAnimator.progress());
    final int x = Math.round(slideFromRect.x + (rest.x - slideFromRect.x) * t);
    final int w = Math.round(slideFromRect.width + (rest.width - slideFromRect.width) * t);
    return new Rectangle(x, rest.y, w, rest.height);
  }

  @Override
  public void removeNotify() {
    slideAnimator.stop();
    slideFromRect = null;
    scrollAnimator.stop();
    super.removeNotify();
  }

  // The indicator's at-rest rect for a tab, in bar coordinates: content-hugging for PRIMARY,
  // full tab width for SECONDARY, bottom-aligned over the divider (research §T / §I).
  Rectangle indicatorRestRect(final ElwhaTab tab) {
    final int height =
        variant == TabsVariant.PRIMARY
            ? PRIMARY_INDICATOR_HEIGHT_PX
            : SECONDARY_INDICATOR_HEIGHT_PX;
    final int y = getHeight() - height;
    if (variant == TabsVariant.PRIMARY) {
      final Rectangle span = tab.contentSpan();
      return new Rectangle(tab.getX() + span.x, y, span.width, height);
    }
    return new Rectangle(tab.getX(), y, tab.getWidth(), height);
  }

  // ---------------------------------------------------------- keyboard navigation

  private static final String ACTION_FOCUS_RIGHT = "elwhaTabs.focusRight";
  private static final String ACTION_FOCUS_LEFT = "elwhaTabs.focusLeft";
  private static final String ACTION_FOCUS_FIRST = "elwhaTabs.focusFirst";
  private static final String ACTION_FOCUS_LAST = "elwhaTabs.focusLast";

  private void installKeyboardNavigation(final ElwhaTab tab) {
    tab.getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ACTION_FOCUS_RIGHT);
    tab.getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ACTION_FOCUS_LEFT);
    tab.getInputMap(WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), ACTION_FOCUS_FIRST);
    tab.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), ACTION_FOCUS_LAST);
    tab.getActionMap().put(ACTION_FOCUS_RIGHT, arrowAction(true));
    tab.getActionMap().put(ACTION_FOCUS_LEFT, arrowAction(false));
    tab.getActionMap().put(ACTION_FOCUS_FIRST, edgeAction(true));
    tab.getActionMap().put(ACTION_FOCUS_LAST, edgeAction(false));
  }

  private void uninstallKeyboardNavigation(final ElwhaTab tab) {
    tab.getInputMap(WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
    tab.getInputMap(WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
    tab.getInputMap(WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
    tab.getInputMap(WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
    tab.getActionMap().remove(ACTION_FOCUS_RIGHT);
    tab.getActionMap().remove(ACTION_FOCUS_LEFT);
    tab.getActionMap().remove(ACTION_FOCUS_FIRST);
    tab.getActionMap().remove(ACTION_FOCUS_LAST);
  }

  private Action arrowAction(final boolean rightKey) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        // The bar's enabled state, explicitly — a bare isEnabled() here is the Action's own and
        // would silently pass on a disabled bar (the #432 slider bug).
        if (!ElwhaTabs.this.isEnabled() || !(e.getSource() instanceof ElwhaTab from)) {
          return;
        }
        // RTL flips which arrow is "forwards" (research §I).
        final boolean forward =
            getComponentOrientation().isLeftToRight() ? rightKey : !rightKey;
        moveFocus(from, forward);
      }
    };
  }

  private Action edgeAction(final boolean first) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (!ElwhaTabs.this.isEnabled() || tabs.isEmpty()) {
          return;
        }
        focusTab(tabs.get(first ? 0 : tabs.size() - 1));
      }
    };
  }

  // Moves the roving focus one tab forward/backward with wrap-around (research §I).
  void moveFocus(final ElwhaTab from, final boolean forward) {
    final int count = tabs.size();
    final int i = tabs.indexOf(from);
    if (i < 0 || count < 2) {
      return;
    }
    focusTab(tabs.get((i + (forward ? 1 : -1) + count) % count));
  }

  private void focusTab(final ElwhaTab target) {
    target.requestFocusInWindow();
    scrollToTab(target);
    if (autoActivate) {
      userActivate(target, 0);
    }
  }

  // The roving tab stop: the focused tab, else the active tab, else the first.
  private ElwhaTab currentTabStop() {
    for (ElwhaTab tab : tabs) {
      if (tab.isFocusOwner()) {
        return tab;
      }
    }
    if (activeTabIndex >= 0) {
      return tabs.get(activeTabIndex);
    }
    return tabs.isEmpty() ? null : tabs.get(0);
  }

  /**
   * The bar's traversal cycle exposes exactly one tab — the roving stop — so Tab enters on the
   * focused-else-active tab and the next Tab press leaves the bar; arrow keys move within.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  private final class TabsFocusTraversalPolicy extends FocusTraversalPolicy {

    @Override
    public Component getComponentAfter(final Container root, final Component current) {
      return null;
    }

    @Override
    public Component getComponentBefore(final Container root, final Component current) {
      return null;
    }

    @Override
    public Component getFirstComponent(final Container root) {
      return currentTabStop();
    }

    @Override
    public Component getLastComponent(final Container root) {
      return currentTabStop();
    }

    @Override
    public Component getDefaultComponent(final Container root) {
      return currentTabStop();
    }
  }

  // ----------------------------------------------------------- accessibility

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaTabs();
    }
    return accessibleContext;
  }

  /**
   * The bar's accessible context — {@link AccessibleRole#PAGE_TAB_LIST} (the ARIA {@code tablist}
   * mapping) with an {@link AccessibleSelection} of exactly one selected child (the {@code
   * JTabbedPane} shape): {@code addAccessibleSelection} activates a tab; removal/clearing are
   * no-ops because the selection is mandatory.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  protected class AccessibleElwhaTabs extends AccessibleJComponent implements AccessibleSelection {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.PAGE_TAB_LIST;
    }

    @Override
    public AccessibleSelection getAccessibleSelection() {
      return this;
    }

    @Override
    public int getAccessibleSelectionCount() {
      return activeTabIndex >= 0 ? 1 : 0;
    }

    @Override
    public Accessible getAccessibleSelection(final int i) {
      return i == 0 ? getActiveTab() : null;
    }

    @Override
    public boolean isAccessibleChildSelected(final int i) {
      return i == activeTabIndex;
    }

    @Override
    public void addAccessibleSelection(final int i) {
      setActiveTabIndex(i);
    }

    @Override
    public void removeAccessibleSelection(final int i) {
      // Mandatory selection — a tab cannot be deselected, only replaced.
    }

    @Override
    public void clearAccessibleSelection() {
      // Mandatory selection.
    }

    @Override
    public void selectAllAccessibleSelection() {
      // Single selection.
    }
  }
}
