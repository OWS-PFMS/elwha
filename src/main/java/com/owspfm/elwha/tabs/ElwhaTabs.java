package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ColorRole;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
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
public class ElwhaTabs extends JComponent {

  static final int BAR_HEIGHT_INLINE_PX = 48;
  static final int DIVIDER_THICKNESS_PX = 1;
  static final int PRIMARY_INDICATOR_HEIGHT_PX = 3;
  static final int PRIMARY_INDICATOR_CORNER_RADIUS_PX = 3;
  static final int SECONDARY_INDICATOR_HEIGHT_PX = 2;

  private final TabsVariant variant;
  private final List<ElwhaTab> tabs = new ArrayList<>();
  private final List<ChangeListener> changeListeners = new ArrayList<>();

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
    tabs.add(tab);
    add(tab);
    if (activeTabIndex < 0) {
      activate(0, true);
    }
    revalidate();
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
    repaint();
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

  private void activate(final int index, final boolean silent) {
    if (index == activeTabIndex) {
      return;
    }
    activeTabIndex = index;
    for (int i = 0; i < tabs.size(); i++) {
      tabs.get(i).setActive(i == index);
    }
    if (!silent) {
      fireChange();
    }
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
    final int width = getWidth();
    final int height = getHeight();
    final int base = width / count;
    final int remainder = width % count;
    int x = 0;
    for (int i = 0; i < count; i++) {
      final ElwhaTab tab = tabs.get(i);
      final int w = base + (i < remainder ? 1 : 0);
      final int tabHeight = Math.min(tab.getPreferredSize().height, height);
      tab.setBounds(x, height - tabHeight, w, tabHeight);
      x += w;
    }
  }

  @Override
  public Dimension getPreferredSize() {
    int width = 0;
    for (ElwhaTab tab : tabs) {
      width += tab.getPreferredSize().width;
    }
    return new Dimension(width, BAR_HEIGHT_INLINE_PX);
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
    final ElwhaTab active = getActiveTab();
    if (active == null) {
      return;
    }
    final Rectangle rect = indicatorRestRect(active);
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
}
