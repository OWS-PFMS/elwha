package com.owspfm.elwha.card;

import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
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
 * Surface), padding {@link SpaceScale#LG} (16 dp) on both axes, orientation {@link
 * CardOrientation#VERTICAL}, expansion overflow {@link ExpansionOverflow#GROW}. The four M3
 * measurement defaults — 12dp shape, 16dp padding, 8dp inter-card (consumer-controlled on the
 * list), start-aligned text — are baked in.
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

  /** Property name fired when orientation changes. */
  public static final String PROPERTY_ORIENTATION = "orientation";

  /**
   * Maximum supported elevation level (0..5), corresponding to M3 ElevationTokens Level0..Level5.
   */
  public static final int MAX_ELEVATION = 5;

  private CardVariant variant = CardVariant.ELEVATED;
  private CardOrientation orientation = CardOrientation.VERTICAL;
  private ExpansionOverflow expansionOverflow = ExpansionOverflow.GROW;
  private int elevation = defaultElevationFor(CardVariant.ELEVATED);
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
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    applyVariant(variant);
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
    this.elevation = defaultElevationFor(v);
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
   * Overrides the variant-derived resting elevation. Clamped to {@code 0..MAX_ELEVATION}.
   *
   * @param elevationLevel the new resting elevation level (0..{@link #MAX_ELEVATION})
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setElevation(final int elevationLevel) {
    this.elevation = Math.max(0, Math.min(MAX_ELEVATION, elevationLevel));
    repaint();
    return this;
  }

  /**
   * @return the resting elevation level (0..{@link #MAX_ELEVATION})
   * @version v0.2.0
   * @since v0.2.0
   */
  public int getElevation() {
    return elevation;
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
    int sum = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      final Component child = getComponent(i);
      if (child.isVisible()) {
        sum += child.getPreferredSize().height;
      }
    }
    final Insets ins = getInsets();
    return sum + ins.top + ins.bottom;
  }

  /** Sets each child's visibility according to the card's collapsed state and the child's rule. */
  private void applyCollapseVisibility() {
    for (int i = 0; i < getComponentCount(); i++) {
      final Component child = getComponent(i);
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
    this.expansionOverflow = Objects.requireNonNull(strategy, "strategy");
    revalidate();
    return this;
  }

  /**
   * @return the active expansion-overflow strategy
   * @version v0.2.0
   * @since v0.2.0
   */
  public ExpansionOverflow getExpansionOverflow() {
    return expansionOverflow;
  }

  // ----------------------------------------------------------- orientation

  /**
   * Sets the orientation. VERTICAL uses {@code BoxLayout(Y_AXIS)} where {@code add()} order is the
   * stack order. HORIZONTAL uses a custom 2-column layout filled via {@link #setLeadingColumn} /
   * {@link #setTrailingColumn}; the 2-column wiring lands with the HORIZONTAL orientation story.
   *
   * @param newOrientation the orientation (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setOrientation(final CardOrientation newOrientation) {
    Objects.requireNonNull(newOrientation, "orientation");
    if (this.orientation == newOrientation) {
      return this;
    }
    final CardOrientation old = this.orientation;
    this.orientation = newOrientation;
    if (newOrientation == CardOrientation.VERTICAL) {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    } else {
      // Placeholder until the HORIZONTAL 2-column LayoutManager lands. Cards constructed with
      // VERTICAL and never switched experience no behavior change.
      setLayout(new JPanel().getLayout());
    }
    firePropertyChange(PROPERTY_ORIENTATION, old, newOrientation);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the current orientation
   * @version v0.2.0
   * @since v0.2.0
   */
  public CardOrientation getOrientation() {
    return orientation;
  }

  /**
   * Sets the leading column for a HORIZONTAL card. {@code add()} is not used in HORIZONTAL mode —
   * leading + trailing columns are the only way to populate a horizontal card.
   *
   * @param component the leading column content
   * @return {@code this} for fluent chaining
   * @throws UnsupportedOperationException until the HORIZONTAL layout wiring story lands
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setLeadingColumn(final JComponent component) {
    throw new UnsupportedOperationException(
        "HORIZONTAL 2-column layout wires in the HORIZONTAL orientation story");
  }

  /**
   * Sets the trailing column for a HORIZONTAL card. See {@link #setLeadingColumn(JComponent)}.
   *
   * @param component the trailing column content
   * @return {@code this} for fluent chaining
   * @throws UnsupportedOperationException until the HORIZONTAL layout wiring story lands
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCard setTrailingColumn(final JComponent component) {
    throw new UnsupportedOperationException(
        "HORIZONTAL 2-column layout wires in the HORIZONTAL orientation story");
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
    // No-op until the actionability story wires the press-tracking state machine.
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
  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    // TODO(#90): shadow + state-layer overlay + focus ring + ripple paint stack.
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
}
