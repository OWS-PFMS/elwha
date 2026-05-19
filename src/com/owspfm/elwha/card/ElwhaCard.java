package com.owspfm.elwha.card;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

/**
 * M3-aligned card chrome — Layer 1 of the V3 architecture. {@code ElwhaCard} extends {@link
 * ElwhaSurface} and adds the card-specific behavior axes (variant, elevation, actionability,
 * selection, collapse, orientation). It owns no typed content slots; consumers compose content by
 * adding companion primitives from this package (Layer 2-4) via {@code card.add(...)}.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} for the full implementation contract; this
 * class is the §3 chrome.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaCard card = ElwhaCard.elevatedCard();
 * card.add(new ElwhaCardHeader().setTitle("Recent activity").setSubtitle("Last 30 days"));
 * card.add(new ElwhaCardSupportingText("12 cycles found across 4 factors."));
 * }</pre>
 *
 * <p><strong>Variants.</strong> Per spec §8: {@link CardVariant#ELEVATED} (default) on {@code
 * SURFACE_CONTAINER_LOW} at elevation Level 1, {@link CardVariant#FILLED} on {@code
 * SURFACE_CONTAINER_HIGHEST} at Level 0, {@link CardVariant#OUTLINED} on {@code SURFACE} with a
 * {@code OUTLINE_VARIANT} border at 1dp.
 *
 * <p><strong>Defaults.</strong> {@link ShapeScale#MD} (12 dp corner radius — inherited from
 * Surface), padding {@link SpaceScale#LG} (16 dp) on both axes, expansion overflow {@link
 * ExpansionOverflow#GROW}. The four M3 measurement defaults — 12dp shape, 16dp padding, 8dp
 * inter-card (consumer-controlled on the list), start-aligned text — are baked in.
 *
 * <p><strong>Orientation.</strong> v0.2.0 ships VERTICAL only; HORIZONTAL is deferred to v0.3.0 per
 * spec §15.3 (#112). The v0.3 HORIZONTAL design will reuse {@code add(...)} with typed partitioning
 * ({@link ElwhaCardMedia} → leading column, everything else → trailing column under the same
 * VerticalCardLayout rules) — no separate setLeadingColumn / setTrailingColumn API.
 *
 * <p><strong>Actionability is atomic.</strong> {@link #setActionable(boolean)} flips the entire
 * quadrad — cursor + hover state-layer + ripple + tab stop + AccessibleRole — together; consumers
 * cannot configure those independently. See spec §12.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public class ElwhaCard extends ElwhaSurface {

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  /** Property name fired when the collapsed state changes. */
  public static final String PROPERTY_COLLAPSED = "collapsed";

  /** Property name fired when actionability toggles. */
  public static final String PROPERTY_ACTIONABLE = "actionable";

  /**
   * Maximum supported elevation level (0..5), corresponding to M3 ElevationTokens Level0..Level5.
   * Aliases {@link ElwhaSurface#MAX_ELEVATION} — the underlying field + paint pipeline live on
   * Surface (any elevatable primitive uses the same shadow stack).
   */
  public static final int MAX_ELEVATION = ElwhaSurface.MAX_ELEVATION;

  private CardVariant variant = CardVariant.ELEVATED;
  private ExpansionOverflow expansionOverflow = ExpansionOverflow.GROW;
  private SpaceScale paddingHorizontal = SpaceScale.LG;
  private SpaceScale paddingVertical = SpaceScale.LG;

  private boolean actionable;
  private boolean selectable;
  private boolean selected;
  private boolean collapsible;
  private boolean collapsed;
  private boolean animateCollapse = !GraphicsEnvironment.isHeadless();
  private boolean dragged;

  /** 250 ms M3 collapse-tween duration. */
  private static final int COLLAPSE_ANIMATION_MS = 250;

  /** Default cap for SCROLL expansion overflow (320 dp per spec §22). */
  private static final int SCROLL_MAX_EXPANDED_HEIGHT_PX = 320;

  /** Animation progress, 0..1; 1f means animation is idle / complete. */
  private float animationFraction = 1f;

  /** Pixel height at the start of the current animation. */
  private int animationStartHeight;

  /** Pixel height at the end of the current animation. */
  private int animationEndHeight;

  private Timer collapseTimer;

  /** Internal body panel + scroll wrapper used when {@link ExpansionOverflow#SCROLL} is active. */
  private JPanel scrollBody;

  private JScrollPane scrollPane;

  /** Interaction state (set by the actionability mouse/key listeners). */
  private boolean hovered;

  private boolean pressed;

  /**
   * Set by {@link #cancelPendingClick()} when a press converts into a drag — the next mouseReleased
   * on this card suppresses the action / selection toggle it would have fired.
   */
  private boolean clickCanceled;

  /** Ripple state (seeded at click point, animated 0..1 over RIPPLE_TOTAL_MS). */
  private Point rippleOrigin;

  private float rippleProgress = 1f;
  private Timer rippleTimer;
  private MouseAdapter mouseHandler;
  private FocusAdapter focusHandler;

  /** Ripple total duration in ms (250 ms expand + 150 ms fade tail per spec §10.3). */
  private static final int RIPPLE_TOTAL_MS = 400;

  /** Selection-badge geometry (px). */
  private static final int CHECKED_BADGE_DIAMETER = 24;

  private static final int CHECKED_BADGE_ICON_PX = 16;

  /**
   * Per-paint flag set during {@link #paintComponent} when the ElwhaCard is locally owning the
   * border treatment (disabled-outlined wash, focused-outlined replacement) so that {@link
   * ElwhaSurface}'s super-paint skips its resting border stroke. The flag is read via the
   * overridden {@link #getBorderRole()} — returning {@code null} from there suppresses the border
   * inside {@link com.owspfm.elwha.theme.SurfacePainter#paint}.
   */
  private boolean suppressRestingBorder;

  /**
   * Per-paint flag toggled inside {@link #paintComponent} so {@link #getSurfaceRole()} returns the
   * disabled-variant container role swap (Elevated → SURFACE, Filled → SURFACE_VARIANT) per spec
   * §11 + PL-9. Without the swap, painting the variant's resting role at full opacity gives no
   * visible disabled cue.
   */
  private boolean paintingDisabled;

  private final Map<Component, CollapseRule> collapseConstraints = new IdentityHashMap<>();
  private final java.util.List<ActionListener> actionListeners = new java.util.ArrayList<>();
  private final PropertyChangeSupport selectionChange = new PropertyChangeSupport(this);
  private final PropertyChangeSupport expansionChange = new PropertyChangeSupport(this);

  /**
   * Creates a card with the {@link CardVariant#ELEVATED} default. Use the static factories ({@link
   * #elevatedCard()}, {@link #filledCard()}, {@link #outlinedCard()}) for a variant-named entry
   * point.
   */
  public ElwhaCard() {
    super();
    setLayout(new VerticalCardLayout());
    applyVariant(variant);
    installInteraction();
    installKeyboardActivation();
  }

  // ------------------------------------------------------------- factories

  /**
   * Factory: a default {@link CardVariant#ELEVATED} card.
   *
   * @return a new ElwhaCard with {@code ELEVATED} variant
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaCard elevatedCard() {
    return new ElwhaCard().setVariant(CardVariant.ELEVATED);
  }

  /**
   * Factory: a default {@link CardVariant#FILLED} card.
   *
   * @return a new ElwhaCard with {@code FILLED} variant
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaCard filledCard() {
    return new ElwhaCard().setVariant(CardVariant.FILLED);
  }

  /**
   * Factory: a default {@link CardVariant#OUTLINED} card.
   *
   * @return a new ElwhaCard with {@code OUTLINED} variant
   * @version v0.2.0
   * @since v0.2.0
   */
  public static ElwhaCard outlinedCard() {
    return new ElwhaCard().setVariant(CardVariant.OUTLINED);
  }

  // --------------------------------------------------------------- variant

  /**
   * Sets the card variant, applying the variant's container role + border defaults from spec §8 and
   * the resting elevation default from spec §9. Per-instance overrides on {@link #setSurfaceRole},
   * {@link #setBorderWidth}, or {@link #setElevation} survive a subsequent {@code setVariant} only
   * if re-applied by the consumer afterwards.
   *
   * @param newVariant the variant to apply (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code newVariant} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setVariant(final CardVariant newVariant) {
    Objects.requireNonNull(newVariant, "variant");
    if (this.variant == newVariant) {
      return this;
    }
    this.variant = newVariant;
    applyVariant(newVariant);
    repaint();
    return this;
  }

  /**
   * @return the current card variant
   * @version v0.2.0
   * @since v0.2.0
   */
  public CardVariant getVariant() {
    return variant;
  }

  private void applyVariant(final CardVariant v) {
    setSurfaceRole(v.containerRole());
    setBorderRole(v.borderRole());
    setBorderWidth(v.borderRole() != null ? 1 : 0);
    setElevation(defaultElevationFor(v));
  }

  /**
   * The resting elevation for a variant per spec §9: ELEVATED→1, FILLED→0, OUTLINED→0.
   *
   * @param v the variant
   * @return the M3 ElevationToken level for the variant's rest state
   * @version v0.2.0
   * @since v0.2.0
   */
  public static int defaultElevationFor(final CardVariant v) {
    return v == CardVariant.ELEVATED ? 1 : 0;
  }

  // ------------------------------------------------------------- elevation

  /**
   * Overrides the variant-derived resting elevation. Clamped to {@code 0..MAX_ELEVATION}. Covariant
   * override of {@link ElwhaSurface#setElevation(int)} for the fluent return type; the elevation
   * field + paint pipeline live on Surface.
   *
   * @param elevationLevel the new resting elevation level (0..{@link #MAX_ELEVATION})
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public ElwhaCard setElevation(final int elevationLevel) {
    super.setElevation(elevationLevel);
    return this;
  }

  // --------------------------------------------------------------- padding

  /**
   * Sets the typed-token padding on both axes. Defaults to {@link SpaceScale#LG} on each axis (16
   * dp per the M3 measurement spec frame).
   *
   * @param horizontal horizontal padding step (must not be {@code null})
   * @param vertical vertical padding step (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if either argument is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setPadding(final SpaceScale horizontal, final SpaceScale vertical) {
    this.paddingHorizontal = Objects.requireNonNull(horizontal, "horizontal");
    this.paddingVertical = Objects.requireNonNull(vertical, "vertical");
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the current padding as {@link Insets} resolved from the token steps
   * @version v0.2.0
   * @since v0.2.0
   */
  public Insets getPadding() {
    final int h = paddingHorizontal.px();
    final int v = paddingVertical.px();
    return new Insets(v, h, v, h);
  }

  // --------------------------------------------------------- actionability

  /**
   * Toggles actionability as an atomic gate (spec §12). When {@code true}: hand cursor on hover,
   * state-layer hover/press overlays, click ripple, chassis tab stop, AccessibleRole=PUSH_BUTTON,
   * action listeners fire on click + Space + Enter. When {@code false}: none of those.
   *
   * <p>Behavior wiring (cursor, ripple, state-layer paint, focus traversal, accessibility) lands
   * with the actionability story; this setter records the property and fires the change event.
   *
   * @param newActionable whether the card behaves as a button
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setActionable(final boolean newActionable) {
    if (this.actionable == newActionable) {
      return this;
    }
    final boolean old = this.actionable;
    this.actionable = newActionable;
    setFocusable(newActionable);
    setCursor(
        newActionable && isEnabled()
            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
    firePropertyChange(PROPERTY_ACTIONABLE, old, newActionable);
    repaint();
    return this;
  }

  /**
   * @return whether the card is currently actionable
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isActionable() {
    return actionable;
  }

  /**
   * Adds an action listener fired on click / Space / Enter when the card is actionable.
   *
   * @param listener the listener (ignored if {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public void addActionListener(final ActionListener listener) {
    if (listener != null) {
      actionListeners.add(listener);
    }
  }

  /**
   * Removes a previously-added action listener.
   *
   * @param listener the listener to remove (no-op if not registered)
   * @version v0.2.0
   * @since v0.2.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  /**
   * Dispatches an {@link ActionEvent} to every registered listener. Called by the actionability
   * wiring when a click / keyboard activation lands on an actionable card.
   *
   * @param command the action command string (typically the card's accessible name)
   * @version v0.2.0
   * @since v0.2.0
   */
  protected void fireActionPerformed(final String command) {
    final ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
    for (final ActionListener l : new java.util.ArrayList<>(actionListeners)) {
      l.actionPerformed(event);
    }
  }

  // ------------------------------------------------------------- selection

  /**
   * Toggles whether the card can enter the selected state. Orthogonal to actionability — a card may
   * be selectable without being actionable (use case: gallery picker where the cell isn't a button
   * but does carry a selected/unselected signal).
   *
   * @param newSelectable whether the card can be selected
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setSelectable(final boolean newSelectable) {
    this.selectable = newSelectable;
    return this;
  }

  /**
   * @return whether the card can be selected
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isSelectable() {
    return selectable;
  }

  /**
   * Sets the selected state. No-op when {@link #isSelectable()} is {@code false}.
   *
   * @param newSelected the new selected state
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setSelected(final boolean newSelected) {
    if (!selectable || this.selected == newSelected) {
      return this;
    }
    final boolean old = this.selected;
    this.selected = newSelected;
    firePropertyChange(PROPERTY_SELECTED, old, newSelected);
    selectionChange.firePropertyChange(PROPERTY_SELECTED, old, newSelected);
    repaint();
    return this;
  }

  /**
   * @return whether the card is currently selected
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Adds a listener notified on every {@link #PROPERTY_SELECTED} change.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  public void addSelectionChangeListener(final PropertyChangeListener listener) {
    selectionChange.addPropertyChangeListener(listener);
  }

  /**
   * Removes a previously-registered selection-change listener.
   *
   * @param listener the listener to remove
   * @version v0.2.0
   * @since v0.2.0
   */
  public void removeSelectionChangeListener(final PropertyChangeListener listener) {
    selectionChange.removePropertyChangeListener(listener);
  }

  // -------------------------------------------------------------- collapse

  /**
   * Toggles whether the card can enter the collapsed state.
   *
   * @param newCollapsible whether the card supports collapse
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setCollapsible(final boolean newCollapsible) {
    this.collapsible = newCollapsible;
    return this;
  }

  /**
   * @return whether the card is collapsible
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isCollapsible() {
    return collapsible;
  }

  /**
   * Sets the collapsed state. No-op when {@link #isCollapsible()} is {@code false}.
   *
   * @param newCollapsed the new collapsed state
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setCollapsed(final boolean newCollapsed) {
    if (!collapsible || this.collapsed == newCollapsed) {
      return this;
    }
    final boolean old = this.collapsed;
    final int beforeHeight = computeContentHeight();
    this.collapsed = newCollapsed;
    applyCollapseVisibility();
    final int afterHeight = computeContentHeight();
    firePropertyChange(PROPERTY_COLLAPSED, old, newCollapsed);
    expansionChange.firePropertyChange(PROPERTY_COLLAPSED, old, newCollapsed);
    if (animateCollapse && beforeHeight != afterHeight) {
      startCollapseAnimation(beforeHeight, afterHeight);
    } else {
      animationFraction = 1f;
      revalidate();
      repaint();
    }
    return this;
  }

  /** Returns the height of all currently-visible children plus the chassis insets. */
  private int computeContentHeight() {
    final Container host = contentHost();
    int sum = 0;
    for (int i = 0; i < host.getComponentCount(); i++) {
      final Component child = host.getComponent(i);
      if (child.isVisible()) {
        sum += child.getPreferredSize().height;
      }
    }
    final Insets ins = getInsets();
    return sum + ins.top + ins.bottom;
  }

  /** Sets each child's visibility according to the card's collapsed state and the child's rule. */
  private void applyCollapseVisibility() {
    final Container host = contentHost();
    for (int i = 0; i < host.getComponentCount(); i++) {
      final Component child = host.getComponent(i);
      final CollapseRule rule = collapseConstraints.getOrDefault(child, CollapseRule.COLLAPSIBLE);
      final boolean visible = rule == CollapseRule.ALWAYS_VISIBLE || !collapsed;
      if (child.isVisible() != visible) {
        child.setVisible(visible);
      }
    }
  }

  private void startCollapseAnimation(final int from, final int to) {
    animationStartHeight = from;
    animationEndHeight = to;
    animationFraction = 0f;
    if (collapseTimer != null && collapseTimer.isRunning()) {
      collapseTimer.stop();
    }
    final int frameMs = 16;
    final long startNanos = System.nanoTime();
    collapseTimer =
        new Timer(
            frameMs,
            e -> {
              final long now = System.nanoTime();
              final float t =
                  Math.min(1f, (now - startNanos) / (COLLAPSE_ANIMATION_MS * 1_000_000f));
              // M3 standard-easing approximation: cubic ease-in-out.
              animationFraction =
                  t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
              revalidate();
              repaint();
              if (t >= 1f) {
                animationFraction = 1f;
                ((Timer) e.getSource()).stop();
                if (getParent() != null) {
                  getParent().revalidate();
                }
              }
            });
    collapseTimer.setRepeats(true);
    collapseTimer.setInitialDelay(0);
    collapseTimer.start();
  }

  /**
   * @return whether the card is currently collapsed
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Toggles the collapse-animation behavior. Behavior wiring (height tween at 250 ms with M3
   * easing) lands with the collapse story.
   *
   * @param animate whether to animate collapse transitions
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setAnimateCollapse(final boolean animate) {
    this.animateCollapse = animate;
    return this;
  }

  /**
   * @return whether collapse transitions animate
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isAnimateCollapse() {
    return animateCollapse;
  }

  /**
   * Assigns a per-child collapse rule. Children without an explicit rule default to {@link
   * CollapseRule#COLLAPSIBLE}. Rule storage uses identity equality on the child component.
   *
   * @param child the child component (must not be {@code null})
   * @param rule the rule to apply (must not be {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public void setCollapseConstraint(final Component child, final CollapseRule rule) {
    Objects.requireNonNull(child, "child");
    Objects.requireNonNull(rule, "rule");
    collapseConstraints.put(child, rule);
    applyCollapseVisibility();
    revalidate();
  }

  /**
   * @param child the child component
   * @return the rule for {@code child} — {@link CollapseRule#COLLAPSIBLE} if no rule was assigned
   * @version v0.2.0
   * @since v0.2.0
   */
  public CollapseRule getCollapseConstraint(final Component child) {
    return collapseConstraints.getOrDefault(child, CollapseRule.COLLAPSIBLE);
  }

  /**
   * Adds a listener notified on every {@link #PROPERTY_COLLAPSED} change.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  public void addExpansionChangeListener(final PropertyChangeListener listener) {
    expansionChange.addPropertyChangeListener(listener);
  }

  /**
   * Removes a previously-registered expansion-change listener.
   *
   * @param listener the listener to remove
   * @version v0.2.0
   * @since v0.2.0
   */
  public void removeExpansionChangeListener(final PropertyChangeListener listener) {
    expansionChange.removePropertyChangeListener(listener);
  }

  // ---------------------------------------------------- expansion overflow

  /**
   * Sets the overflow strategy when an expanded card exceeds its resting height. Defaults to {@link
   * ExpansionOverflow#GROW}. See spec §14.4.
   *
   * @param strategy the strategy (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setExpansionOverflow(final ExpansionOverflow strategy) {
    Objects.requireNonNull(strategy, "strategy");
    if (this.expansionOverflow == strategy) {
      return this;
    }
    this.expansionOverflow = strategy;
    if (strategy == ExpansionOverflow.SCROLL) {
      installScrollWrapper();
    } else {
      uninstallScrollWrapper();
    }
    revalidate();
    repaint();
    return this;
  }

  private void installScrollWrapper() {
    if (scrollPane != null) {
      return;
    }
    final java.util.List<Component> existing = new java.util.ArrayList<>();
    for (int i = 0; i < getComponentCount(); i++) {
      existing.add(getComponent(i));
    }
    super.removeAll();
    scrollBody = new JPanel();
    scrollBody.setOpaque(false);
    scrollBody.setLayout(new VerticalCardLayout());
    for (final Component c : existing) {
      scrollBody.add(c);
    }
    scrollPane =
        new JScrollPane(
            scrollBody,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setBorder(null);
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    super.setLayout(new java.awt.BorderLayout());
    super.addImpl(scrollPane, null, 0);
  }

  private void uninstallScrollWrapper() {
    if (scrollPane == null) {
      return;
    }
    final java.util.List<Component> inside = new java.util.ArrayList<>();
    for (int i = 0; i < scrollBody.getComponentCount(); i++) {
      inside.add(scrollBody.getComponent(i));
    }
    scrollBody.removeAll();
    super.removeAll();
    scrollPane = null;
    scrollBody = null;
    super.setLayout(new VerticalCardLayout());
    for (final Component c : inside) {
      super.addImpl(c, null, -1);
    }
  }

  /**
   * Returns the container that owns the card's content children. In {@link ExpansionOverflow#GROW}
   * mode this is the card itself; in {@link ExpansionOverflow#SCROLL} mode this is the inner scroll
   * body. Code that iterates children (visibility filtering, height computation, drag-reorder slot
   * math) routes through this rather than {@code this}.
   */
  Container contentHost() {
    return scrollBody != null ? scrollBody : this;
  }

  /**
   * Routes {@code add()} calls into the inner scroll body when {@link ExpansionOverflow#SCROLL} is
   * active, so consumer code can keep calling {@code card.add(...)} regardless of the overflow
   * strategy.
   */
  @Override
  protected void addImpl(final Component comp, final Object constraints, final int index) {
    if (scrollBody != null && comp != scrollPane) {
      scrollBody.add(comp, constraints, index);
      return;
    }
    super.addImpl(comp, constraints, index);
  }

  /**
   * @return the active expansion-overflow strategy
   * @version v0.2.0
   * @since v0.2.0
   */
  public ExpansionOverflow getExpansionOverflow() {
    return expansionOverflow;
  }

  // ------------------------------------------------------------------ drag

  /**
   * Marks the card as being dragged. ElwhaCardList sets this while a drag-reorder gesture is in
   * progress; the dragged state lifts the elevation and applies the dragged state-layer overlay
   * (spec §9, §10.1).
   *
   * @param newDragged the new dragged state
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setDragged(final boolean newDragged) {
    this.dragged = newDragged;
    repaint();
    return this;
  }

  /**
   * @return whether the card is currently flagged as dragged
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isDragged() {
    return dragged;
  }

  /**
   * Cancels a pending click on the card — invoked by ElwhaCardList when a press converts into a
   * drag, so the press doesn't also fire an action listener on release.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard cancelPendingClick() {
    clickCanceled = true;
    pressed = false;
    repaint();
    return this;
  }

  // -------------------------------------------------------------- painting

  /**
   * Paints the chrome — delegates to Surface for the rounded fill and border. Subsequent stories
   * layer on shadow / state-layer overlay / focus ring / ripple, and the M3 top-trailing selection
   * badge in {@link #paintChildren}.
   *
   * @param g the graphics context
   * @version v0.2.0
   * @since v0.2.0
   */
  // -------------------------------------------------------------- painting

  /**
   * Card transient elevation lift per spec §9 — hover bumps every variant +1 level; dragged bumps
   * every variant +3 levels. Resting elevation stays in the {@link ElwhaSurface#elevation} field;
   * this hook only affects the painted shadow + body, not the reserved shadow insets.
   *
   * @return the elevation to paint right now
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  protected int currentElevationForPaint() {
    int e = super.currentElevationForPaint();
    if (dragged) {
      e += 3;
    } else if (hovered && isEnabled()) {
      e += 1;
    }
    return Math.min(MAX_ELEVATION, e);
  }

  /**
   * Always reserves the {@link #MAX_ELEVATION} shadow inset regardless of the current resting
   * elevation. This keeps the visible body size identical across variants (Elevated, Filled,
   * Outlined) and across elevation transitions (hover lift, drag lift), so a mixed-variant grid of
   * cards has consistent body widths and a hovered card doesn't suddenly shrink as its reserve
   * grows. Matches Compose Material3 / MaterialCardView, where elevation is painted outside the
   * measured body — Swing doesn't allow that, so the reserve is always-on instead.
   *
   * @return the chassis insets — always {@code ShadowPainter.shadowInsets(MAX_ELEVATION)}
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public Insets getInsets() {
    return com.owspfm.elwha.theme.ShadowPainter.shadowInsets(MAX_ELEVATION);
  }

  /**
   * Paints under the children: Surface chassis (shadow + fill + border) via {@code super}, then the
   * card's state-layer overlay tinted to the variant's on-pair. Selection badge, focus ring,
   * disabled outlined border, and ripple paint above children in {@link #paintChildren}.
   *
   * <p>Sets two per-paint flags ({@link #paintingDisabled}, {@link #suppressRestingBorder}) so the
   * chassis super-paint sees the disabled container-role swap (PL-9) and skips the resting border
   * when ElwhaCard is locally painting it (focused-outlined PL-8, disabled-outlined PL-10). The
   * flags are reset before this method returns so a subsequent paint with different state-pair uses
   * the resting defaults.
   *
   * @param g the graphics context
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final boolean disabled = !isEnabled();
    final boolean focused = actionable && isFocusOwner() && isEnabled();
    paintingDisabled = disabled;
    suppressRestingBorder = variant == CardVariant.OUTLINED && (focused || disabled);
    try {
      super.paintComponent(g);
    } finally {
      paintingDisabled = false;
      suppressRestingBorder = false;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintStateLayerOverlay(g2);
    } finally {
      g2.dispose();
    }
  }

  /**
   * Overrides the chassis surface role during disabled paint to apply the M3 variant swap per spec
   * §11 + PL-9 — Elevated → SURFACE, Filled → SURFACE_VARIANT, Outlined unchanged. The swap is
   * scoped to {@link #paintComponent} via {@link #paintingDisabled}; at all other times the resting
   * role from {@link ElwhaSurface#getSurfaceRole()} is returned.
   *
   * @return the surface role to fill the chassis with
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public ColorRole getSurfaceRole() {
    if (paintingDisabled) {
      if (variant == CardVariant.ELEVATED) {
        return ColorRole.SURFACE;
      }
      if (variant == CardVariant.FILLED) {
        return ColorRole.SURFACE_VARIANT;
      }
    }
    return super.getSurfaceRole();
  }

  /**
   * Overrides the chassis border role so the super-paint can be told to skip its resting border
   * stroke. Returns {@code null} (no border) when {@link #suppressRestingBorder} is set — used by
   * {@link #paintComponent} so a focused-outlined or disabled-outlined card can paint its own
   * border treatment in {@link #paintChildren} without double-stacking the resting outline.
   *
   * @return the border role to stroke with, or {@code null} to suppress
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public ColorRole getBorderRole() {
    return suppressRestingBorder ? null : super.getBorderRole();
  }

  /**
   * Paints above the children: focus ring, disabled outlined border replacement, ripple, selection
   * badge. Painting these on top of children ensures the selection badge sits above any media child
   * that would otherwise hide it.
   *
   * <p>Per PL-8 (focused-outlined) and PL-10 (disabled-outlined), the border for Outlined cards in
   * those states is painted here at the chassis body edge — the resting OUTLINE_VARIANT stroke is
   * suppressed by {@link #suppressRestingBorder} during super.paintComponent, so there's no
   * double-stacking.
   *
   * @param g the graphics context
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (actionable && rippleProgress < 1f && rippleOrigin != null) {
        paintRipple(g2);
      }
      if (variant == CardVariant.OUTLINED && !isEnabled()) {
        paintDisabledOutlinedBorder(g2);
      }
      if (actionable && isFocusOwner() && isEnabled()) {
        paintFocusRing(g2);
      }
      if (selected) {
        paintSelectionBadge(g2);
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * @return the visible card body rect ({@code [x, y, w, h]}) — inset from chassis bounds by the
   *     shadow reserve in {@link #getInsets()}. Chrome overlays (state-layer, focus ring, ripple,
   *     selection badge, disabled scrim) paint relative to these bounds, not the chassis bounds.
   */
  private java.awt.Rectangle bodyBounds() {
    final Insets s = getInsets();
    return new java.awt.Rectangle(
        s.left,
        s.top,
        Math.max(0, getWidth() - s.left - s.right),
        Math.max(0, getHeight() - s.top - s.bottom));
  }

  /**
   * Hover / pressed / dragged state-layer overlay — on-surface tint at variant-agnostic opacity.
   */
  private void paintStateLayerOverlay(final Graphics2D g2) {
    if (!isEnabled() || !actionable) {
      return;
    }
    final StateLayer layer;
    if (dragged) {
      layer = StateLayer.DRAGGED;
    } else if (pressed) {
      layer = StateLayer.PRESSED;
    } else if (hovered) {
      layer = StateLayer.HOVER;
    } else {
      return;
    }
    final Color tint = ColorRole.ON_SURFACE.resolve();
    g2.setComposite(AlphaComposite.SrcOver.derive(layer.opacity()));
    g2.setColor(tint);
    final java.awt.Rectangle b = bodyBounds();
    final int arc = getShape().px();
    g2.fill(new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, arc, arc));
  }

  /**
   * Disabled-outlined border replacement per PL-10: paint the OUTLINE role (the stronger of OUTLINE
   * / OUTLINE_VARIANT per M3) at 12 % opacity at the chassis edge. The resting OUTLINE_VARIANT
   * border is suppressed by {@link #suppressRestingBorder} so there's no double-stacking.
   */
  private void paintDisabledOutlinedBorder(final Graphics2D g2) {
    final Color stroke = ColorRole.OUTLINE.resolve();
    g2.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
    g2.setColor(stroke);
    g2.setStroke(new BasicStroke(1f));
    final java.awt.Rectangle b = bodyBounds();
    final int arc = getShape().px();
    // Center the stroke on the body edge (inset by 0.5 px) so the outer edge tracks the chassis
    // corner exactly, matching the geometry SurfacePainter uses for the resting border.
    g2.draw(
        new RoundRectangle2D.Float(b.x + 0.5f, b.y + 0.5f, b.width - 1f, b.height - 1f, arc, arc));
  }

  /**
   * M3 focus ring. For Elevated / Filled: SECONDARY ring, 2 dp inset (per spec §10.2). For Outlined
   * per PL-8: ON_SURFACE 2 dp stroke painted at the chassis body edge to REPLACE the resting
   * OUTLINE_VARIANT border (which is suppressed during paintComponent), not double-paint inside it.
   * Without the replacement, a focused Outlined card would show two concentric strokes — the 1 dp
   * resting outline plus a 2 dp ring inside it.
   */
  private void paintFocusRing(final Graphics2D g2) {
    final java.awt.Rectangle b = bodyBounds();
    final int arc = getShape().px();
    if (variant == CardVariant.OUTLINED) {
      g2.setColor(ColorRole.ON_SURFACE.resolve());
      g2.setStroke(new BasicStroke(2f));
      // Inset by 1 px so a 2 dp stroke is fully inside the body bounds and visually covers where
      // the resting 1 px outline would have been.
      g2.draw(
          new RoundRectangle2D.Float(b.x + 1f, b.y + 1f, b.width - 2f, b.height - 2f, arc, arc));
    } else {
      final Color ring = ColorRole.SECONDARY.resolve();
      g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 220));
      g2.setStroke(new BasicStroke(2f));
      g2.draw(
          new RoundRectangle2D.Float(b.x + 1f, b.y + 1f, b.width - 2f, b.height - 2f, arc, arc));
    }
  }

  /** Expanding-circle ripple, clipped to the card's rounded body shape. */
  private void paintRipple(final Graphics2D g2) {
    final java.awt.Rectangle b = bodyBounds();
    final Graphics2D rg = (Graphics2D) g2.create();
    try {
      // RipplePainter works in body-local coordinates; translate to the body origin and convert
      // the component-space click point to match.
      rg.translate(b.x, b.y);
      RipplePainter.paint(
          rg,
          b.width,
          b.height,
          new Point(rippleOrigin.x - b.x, rippleOrigin.y - b.y),
          rippleProgress,
          getShape().px(),
          ColorRole.ON_SURFACE.resolve());
    } finally {
      rg.dispose();
    }
  }

  /** M3 top-trailing selected badge — PRIMARY circle + check glyph, no layout reservation. */
  private void paintSelectionBadge(final Graphics2D g2) {
    final java.awt.Rectangle b = bodyBounds();
    final int pad = SpaceScale.SM.px();
    final int d = CHECKED_BADGE_DIAMETER;
    final int x = b.x + b.width - d - pad;
    final int y = b.y + pad;
    g2.setColor(ColorRole.PRIMARY.resolve());
    g2.fillOval(x, y, d, d);
    final FlatSVGIcon check = MaterialIcons.check(CHECKED_BADGE_ICON_PX);
    final Color glyph = ColorRole.PRIMARY.on().orElse(ColorRole.ON_PRIMARY).resolve();
    check.setColorFilter(new FlatSVGIcon.ColorFilter(orig -> glyph));
    final int off = (d - CHECKED_BADGE_ICON_PX) / 2;
    check.paintIcon(this, g2, x + off, y + off);
  }

  // ------------------------------------------------------------ interaction

  private void installInteraction() {
    mouseHandler =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            hovered = true;
            repaint();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            hovered = false;
            pressed = false;
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (actionable && isEnabled()) {
              pressed = true;
              startRipple(e.getPoint());
              if (isFocusable()) {
                requestFocusInWindow();
              }
              repaint();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            final boolean wasPressed = pressed;
            final boolean canceled = clickCanceled;
            pressed = false;
            clickCanceled = false;
            repaint();
            if (wasPressed && !canceled && isEnabled() && contains(e.getPoint())) {
              handleActivation();
            }
          }
        };
    addMouseListener(mouseHandler);
    focusHandler =
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            pressed = false;
            repaint();
          }
        };
    addFocusListener(focusHandler);
  }

  private void installKeyboardActivation() {
    final InputMap im = getInputMap(WHEN_FOCUSED);
    final ActionMap am = getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhaCardActivate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhaCardActivate");
    am.put(
        "elwhaCardActivate",
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (actionable && isEnabled()) {
              startRipple(new Point(getWidth() / 2, getHeight() / 2));
              handleActivation();
            }
          }
        });
  }

  private void handleActivation() {
    if (selectable) {
      setSelected(!selected);
    }
    if (actionable) {
      fireActionPerformed(getAccessibleContext().getAccessibleName());
    }
  }

  private void startRipple(final Point origin) {
    rippleOrigin = origin;
    rippleProgress = 0f;
    if (rippleTimer != null && rippleTimer.isRunning()) {
      rippleTimer.stop();
    }
    final long startNanos = System.nanoTime();
    rippleTimer =
        new Timer(
            16,
            e -> {
              rippleProgress =
                  Math.min(1f, (System.nanoTime() - startNanos) / (RIPPLE_TOTAL_MS * 1_000_000f));
              repaint();
              if (rippleProgress >= 1f) {
                ((Timer) e.getSource()).stop();
              }
            });
    rippleTimer.setRepeats(true);
    rippleTimer.setInitialDelay(0);
    rippleTimer.start();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(
        actionable && enabled
            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
    repaint();
  }

  /**
   * @return an AccessibleContext whose role reflects the actionability atomic gate — {@link
   *     AccessibleRole#PUSH_BUTTON} when actionable, {@link AccessibleRole#PANEL} otherwise — and
   *     whose name gains a {@code " (selected)"} suffix when {@link #isSelected()}
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext =
          new AccessibleJComponent() {
            @Override
            public AccessibleRole getAccessibleRole() {
              return actionable ? AccessibleRole.PUSH_BUTTON : AccessibleRole.PANEL;
            }

            @Override
            public String getAccessibleName() {
              final String base = super.getAccessibleName();
              return selected && base != null ? base + " (selected)" : base;
            }
          };
    }
    return accessibleContext;
  }

  /**
   * Returns the card's preferred height, accounting for (a) the collapse animation tween
   * interpolating between pre-collapse and post-collapse heights, and (b) the SCROLL expansion
   * overflow cap.
   *
   * @return the card's preferred size
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public Dimension getPreferredSize() {
    final Dimension d = super.getPreferredSize();
    int height = d.height;
    if (animationFraction < 1f) {
      height =
          Math.round(
              animationStartHeight
                  + (animationEndHeight - animationStartHeight) * animationFraction);
    }
    if (expansionOverflow == ExpansionOverflow.SCROLL && !collapsed) {
      height = Math.min(height, SCROLL_MAX_EXPANDED_HEIGHT_PX);
    }
    return new Dimension(d.width, height);
  }

  /**
   * Spec §3.4 rule 1: the chassis cooperates with parent-assigned width, never resists shrinking.
   * Returns {@code Integer.MAX_VALUE} on both axes — explicitly documenting that {@code JPanel}'s
   * unbounded default is the intended contract, not a happenstance. {@code BoxLayout}, {@code
   * GridLayout}, and any other parent layout can stretch or compress the chassis freely. Consumers
   * needing a hard minimum should call {@link #setMinimumSize(Dimension)} or wrap the card in a
   * constrained container.
   *
   * @return a {@code Dimension} with unbounded width and height
   * @version v0.2.0
   * @since v0.2.0
   */
  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * VERTICAL LayoutManager. Stacks children top-to-bottom in {@code add()} order — same observable
   * behavior as {@code BoxLayout(Y_AXIS)} per spec §3.3 — plus two card-specific rules:
   *
   * <ul>
   *   <li><strong>Padding from token roles.</strong> Each non-edge-media child is inset by {@link
   *       #paddingHorizontal} horizontally and the card's vertical padding between siblings (top
   *       and bottom of the card itself).
   *   <li><strong>Edge-bleed for {@link ElwhaCardMedia}.</strong> When media is the first child, it
   *       gets full card width and no top inset; when it's the last child, no bottom inset. This is
   *       the spec §5.2 contract that lets the media's cubic-Bezier corner clip align with the
   *       chassis's rounded outer shape.
   * </ul>
   */
  private final class VerticalCardLayout implements LayoutManager {
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
      final Insets ins = parent.getInsets();
      final int padH = paddingHorizontal.px();
      final int padV = paddingVertical.px();
      final int interGap = interElementGap();
      final int count = parent.getComponentCount();
      // Slot-width estimate for the heightForChild calls. Once the chassis has been laid out
      // even once, parent.getWidth() is positive and reflects the actual width children will
      // get — we use it so width-sensitive children (ElwhaCardActions wrap rows, ElwhaCardMedia
      // cover-fit slot height) report the right preferred height for the chassis to reserve.
      // Without this, the preferred-height computation would use single-row / intrinsic heights
      // and the chassis would be sized too short to contain wrapped action rows. Settles after
      // one re-layout cycle.
      final int parentBodyW = Math.max(0, parent.getWidth() - ins.left - ins.right);
      int totalH = 0;
      int maxW = 0;
      boolean anyVisible = false;
      Component firstVisible = null;
      Component lastVisible = null;
      Component prevVisible = null;
      for (int i = 0; i < count; i++) {
        final Component c = parent.getComponent(i);
        if (!c.isVisible()) {
          continue;
        }
        if (firstVisible == null) {
          firstVisible = c;
        }
        lastVisible = c;
        anyVisible = true;
      }
      if (!anyVisible) {
        return new Dimension(ins.left + ins.right + 2 * padH, ins.top + ins.bottom + 2 * padV);
      }
      if (!(firstVisible instanceof ElwhaCardMedia)) {
        totalH += padV;
      }
      for (int i = 0; i < count; i++) {
        final Component c = parent.getComponent(i);
        if (!c.isVisible()) {
          continue;
        }
        if (prevVisible != null) {
          totalH += interGap;
        }
        final Dimension p = c.getPreferredSize();
        final boolean bleed = isEdgeBleed(c, firstVisible, lastVisible);
        final int cellW = bleed ? parentBodyW : Math.max(0, parentBodyW - 2 * padH);
        totalH += heightForChild(c, cellW);
        maxW = Math.max(maxW, bleed ? p.width : p.width + 2 * padH);
        prevVisible = c;
      }
      if (!(lastVisible instanceof ElwhaCardMedia)) {
        totalH += padV;
      }
      return new Dimension(maxW + ins.left + ins.right, totalH + ins.top + ins.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      final Insets ins = parent.getInsets();
      final int padH = paddingHorizontal.px();
      final int padV = paddingVertical.px();
      final int interGap = interElementGap();
      final int bodyX = ins.left;
      final int bodyY = ins.top;
      final int bodyW = Math.max(0, parent.getWidth() - ins.left - ins.right);
      final int bodyH = Math.max(0, parent.getHeight() - ins.top - ins.bottom);
      final int count = parent.getComponentCount();

      Component firstVisible = null;
      Component lastVisible = null;
      int visibleCount = 0;
      for (int i = 0; i < count; i++) {
        final Component c = parent.getComponent(i);
        if (!c.isVisible()) {
          continue;
        }
        if (firstVisible == null) {
          firstVisible = c;
        }
        lastVisible = c;
        visibleCount++;
      }
      if (firstVisible == null) {
        return;
      }

      // Push the last visible ElwhaCardActions to the bottom edge of the body when the body has
      // slack (cell forced taller than content) — per M3, actions belong at the bottom of the
      // card, not stranded mid-air.
      final boolean anchorActionsToBottom = lastVisible instanceof ElwhaCardActions;
      int naturalContentH = 0;
      if (!(firstVisible instanceof ElwhaCardMedia)) {
        naturalContentH += padV;
      }
      if (visibleCount > 1) {
        naturalContentH += interGap * (visibleCount - 1);
      }
      for (int i = 0; i < count; i++) {
        final Component c = parent.getComponent(i);
        if (!c.isVisible()) {
          continue;
        }
        final boolean bleed = isEdgeBleed(c, firstVisible, lastVisible);
        final int cellW = bleed ? bodyW : Math.max(0, bodyW - 2 * padH);
        naturalContentH += heightForChild(c, cellW);
      }
      if (!(lastVisible instanceof ElwhaCardMedia)) {
        naturalContentH += padV;
      }
      final int slack = Math.max(0, bodyH - naturalContentH);
      final int actionsLift = anchorActionsToBottom ? slack : 0;

      int y = bodyY + ((firstVisible instanceof ElwhaCardMedia) ? 0 : padV);
      boolean placedAny = false;
      for (int i = 0; i < count; i++) {
        final Component c = parent.getComponent(i);
        if (!c.isVisible()) {
          continue;
        }
        if (placedAny) {
          y += interGap;
        }
        final boolean bleed = isEdgeBleed(c, firstVisible, lastVisible);
        final int x = bleed ? bodyX : bodyX + padH;
        final int w = bleed ? bodyW : Math.max(0, bodyW - 2 * padH);
        final int h = heightForChild(c, w);
        final int childY = (c == lastVisible) ? y + actionsLift : y;
        c.setBounds(x, childY, w, h);
        y += h;
        placedAny = true;
      }
    }

    /**
     * Height a child should occupy given its assigned slot width.
     *
     * <ul>
     *   <li>{@link ElwhaCardMedia} honors spec §3.4 rule 3 (cover-fit slot sizing) — height tracks
     *       the actual cell width via {@link ElwhaCardMedia#heightForSlotWidth(int)} rather than
     *       the intrinsic preferred-size hint.
     *   <li>{@link ElwhaCardActions} reports its wrapped-row height via {@link
     *       ElwhaCardActions#heightForSlotWidth(int)} (#17) so the chassis reserves vertical space
     *       for whatever rows the wrap layout produces at this width — preferred-size queries carry
     *       no width context and would otherwise always report single-row height.
     *   <li>Everything else uses its preferred height.
     * </ul>
     */
    private int heightForChild(final Component c, final int slotWidth) {
      if (c instanceof ElwhaCardMedia media) {
        return media.heightForSlotWidth(slotWidth);
      }
      if (c instanceof ElwhaCardActions actions) {
        return actions.heightForSlotWidth(slotWidth);
      }
      return c.getPreferredSize().height;
    }

    /**
     * A child gets full card width (no horizontal padding) when:
     *
     * <ul>
     *   <li>It's an {@link ElwhaCardMedia} at the first or last visible position (spec §5.2 — gives
     *       the cubic-Bezier corner clip a chassis-edge anchor).
     *   <li>It's an {@link ElwhaCardDivider} with {@link DividerStyle#FULL} (spec §5.4 — "FULL
     *       spans card edge-to-edge ignoring parent content padding"). FULL dividers bleed at any
     *       position; INSET dividers respect padding like a regular child.
     * </ul>
     */
    private boolean isEdgeBleed(final Component c, final Component first, final Component last) {
      if (c instanceof ElwhaCardMedia && (c == first || c == last)) {
        return true;
      }
      return c instanceof ElwhaCardDivider d && d.getStyle() == DividerStyle.FULL;
    }
  }

  /**
   * Vertical gap inserted between every adjacent pair of visible siblings inside the card body.
   * Defaults to {@link SpaceScale#SM} (8 dp) per the M3 vertical-rhythm convention — gives header,
   * supporting text, divider, and actions room to breathe instead of stacking flush. Applies to
   * media bleeding to the chassis top/bottom too (so the next text sibling isn't crushed against
   * the media edge).
   */
  private int interElementGap() {
    return SpaceScale.SM.px();
  }
}
