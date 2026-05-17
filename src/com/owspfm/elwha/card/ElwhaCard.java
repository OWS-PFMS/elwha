package com.owspfm.elwha.card;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.SurfacePainter;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Token-native, M3-aligned card primitive. Composes a header strip (leading icon + leading actions
 * + headline / subhead + trailing actions + chevron), an optional summary band, a media slot, a
 * supporting-text slot, and a bottom actions row inside an {@link ElwhaSurface} chassis whose fill,
 * shape, and border are token-bound through {@link ColorRole} / {@link ShapeScale}.
 *
 * <p><strong>Composition.</strong> {@code ElwhaCard} extends {@link ElwhaSurface} — the round-rect
 * fill, border stroke, and shape are inherited; the four token-typed setters ({@link
 * #setSurfaceRole(ColorRole)}, {@link #setShape(ShapeScale)}, {@link #setBorderRole(ColorRole)},
 * {@link #setBorderWidth(int)}) come from Surface. Card overlays state-layer paint (hover /
 * pressed), a drop-shadow keyed off the elevation axis, a focus ring, and an M3-style top-trailing
 * checked-icon overlay when selected.
 *
 * <p><strong>Slot vocabulary.</strong> The M3 formal slots are {@link #setHeadline(String)}, {@link
 * #setSubhead(String)}, {@link #setSupportingText(String)}, {@link #setMedia(JComponent)}, and
 * {@link #setActions(Component...)}. The OWS-specific header extensions are {@link
 * #setLeadingIcon(Icon)}, {@link #setLeadingActions(Component...)}, and {@link
 * #setTrailingActions(Component...)}; the disclosure axis is {@link #setCollapsible(boolean)} +
 * {@link #setCollapsed(boolean)} + {@link #setSummary(JComponent)} + {@link
 * #setSummaryVisibility(SummaryVisibility)} + {@link #setAnimateCollapse(boolean)}. Each extension
 * is justified in {@code docs/research/elwha-card-v2-spec.md} §4.
 *
 * <p><strong>Construction.</strong>
 *
 * <pre>{@code
 * ElwhaCard card = ElwhaCard.elevatedCard("Recent activity")
 *     .setSubhead("Last 30 days")
 *     .setSupportingText("12 cycles found across 4 factors.")
 *     .setActions(new JButton("Open"), new JButton("Dismiss"))
 *     .setInteractionMode(CardInteractionMode.HOVERABLE);
 * }</pre>
 *
 * <p><strong>Collapsible:</strong>
 *
 * <pre>{@code
 * ElwhaCard card = new ElwhaCard("Advanced options")
 *     .setSupportingText("...")
 *     .setCollapsible(true)
 *     .setCollapsed(true)
 *     .setSummary(new JLabel("3 options hidden"));
 * card.addExpansionChangeListener(evt -> ...);
 * }</pre>
 *
 * <p><strong>Doctrine.</strong> Matches every rule in {@code
 * docs/development/component-api-conventions.md}: {@code getX()}-only getters; per-variant static
 * factories ({@link #elevatedCard(String)}, {@link #filledCard(String)}, {@link
 * #outlinedCard(String)}); single-arg convenience constructor; the inherited {@code
 * setBorderRole(ColorRole)} is not part of Card's advertised API (Card is variant-bearing — border
 * role is variant-derived); symmetric border-width getter/setter (inherited).
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaCard extends ElwhaSurface {

  /**
   * Property name fired when the collapsed state changes — wire via {@link
   * #addExpansionChangeListener(PropertyChangeListener)}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final String PROPERTY_COLLAPSED = "collapsed";

  /**
   * Property name fired when the selected state changes — wire via {@link
   * #addSelectionChangeListener(PropertyChangeListener)}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final String PROPERTY_SELECTED = "selected";

  /**
   * Maximum supported elevation level (0 disables the shadow entirely). Carried forward from V1 for
   * clamp semantics.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public static final int MAX_ELEVATION = 8;

  private static final int ANIMATION_MS = 160;
  private static final int ANIMATION_STEPS = 10;
  private static final int CHECKED_BADGE_DIAMETER = 24;
  private static final int CHECKED_BADGE_ICON_PX = 16;
  private static final SpaceScale DEFAULT_PADDING_H = SpaceScale.LG;
  private static final SpaceScale DEFAULT_PADDING_V = SpaceScale.LG;
  private static final int INNER_GAP_PX = SpaceScale.SM.px();

  private CardVariant variant = CardVariant.ELEVATED;
  private CardInteractionMode interactionMode = CardInteractionMode.STATIC;
  private SummaryVisibility summaryVisibility = SummaryVisibility.COLLAPSED_ONLY;
  private int elevation = CardVariant.ELEVATED.restingElevation();
  private boolean elevationUserSet;
  private boolean dragged;
  private boolean animateCollapse;
  private SpaceScale paddingHorizontal = DEFAULT_PADDING_H;
  private SpaceScale paddingVertical = DEFAULT_PADDING_V;

  private boolean hovered;
  private boolean pressed;
  private boolean selected;
  private boolean collapsible;
  private boolean collapsed;
  private boolean pendingHeaderToggle;

  private JPanel headerRow;
  private JLabel leadingIconLabel;
  private JPanel leadingActionsPanel;
  private JPanel textStack;
  private JLabel headlineLabel;
  private JLabel subheadLabel;
  private JPanel trailingActionsPanel;
  private ElwhaIconButton chevronButton;

  private JPanel contentArea;
  private CollapsibleContainer collapsibleBody;
  private JPanel mediaHolder;
  private JLabel supportingTextLabel;
  private JPanel actionsHolder;
  private JPanel summaryHolder;

  private JComponent media;
  private String supportingText;
  private JComponent summary;
  private Icon leadingIcon;

  private final List<ActionListener> actionListeners = new ArrayList<>();

  private Timer animationTimer;
  private float animationFraction = 1f;

  /**
   * Creates a card with the default {@link CardVariant#ELEVATED} variant and {@link
   * CardInteractionMode#STATIC} interaction; no headline.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard() {
    this(null);
  }

  /**
   * Convenience constructor — sets the headline in one call. Equivalent to {@code new
   * ElwhaCard().setHeadline(headline)}.
   *
   * @param headline the headline text (may be {@code null} or empty for no headline)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard(final String headline) {
    super();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    applyVariantDefaults(variant);
    initSlots();
    initInteraction();
    rebuildBorder();
    if (headline != null && !headline.isEmpty()) {
      setHeadline(headline);
    }
  }

  /**
   * Creates an {@link CardVariant#ELEVATED} card with the headline already set.
   *
   * @param headline the headline text
   * @return a configured elevated card
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaCard elevatedCard(final String headline) {
    return new ElwhaCard(headline).setVariant(CardVariant.ELEVATED);
  }

  /**
   * Creates a {@link CardVariant#FILLED} card with the headline already set.
   *
   * @param headline the headline text
   * @return a configured filled card
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaCard filledCard(final String headline) {
    return new ElwhaCard(headline).setVariant(CardVariant.FILLED);
  }

  /**
   * Creates an {@link CardVariant#OUTLINED} card with the headline already set.
   *
   * @param headline the headline text
   * @return a configured outlined card
   * @version v0.1.0
   * @since v0.1.0
   */
  public static ElwhaCard outlinedCard(final String headline) {
    return new ElwhaCard(headline).setVariant(CardVariant.OUTLINED);
  }

  // ---------------------------------------------------------------- slots

  private void initSlots() {
    headerRow = newStretchingRow(new BorderLayout(INNER_GAP_PX, 0));
    headerRow.setVisible(false);

    leadingIconLabel = new JLabel();
    leadingIconLabel.setVisible(false);
    leadingActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_GAP_PX / 2, 0));
    leadingActionsPanel.setOpaque(false);
    leadingActionsPanel.setVisible(false);

    final JPanel westGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_GAP_PX / 2, 0));
    westGroup.setOpaque(false);
    westGroup.add(leadingIconLabel);
    westGroup.add(leadingActionsPanel);
    headerRow.add(westGroup, BorderLayout.WEST);

    textStack = new JPanel();
    textStack.setOpaque(false);
    textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
    headlineLabel = new JLabel();
    headlineLabel.putClientProperty("FlatLaf.styleClass", "h4");
    headlineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    subheadLabel = new JLabel();
    subheadLabel.putClientProperty("FlatLaf.styleClass", "small");
    subheadLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    subheadLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    subheadLabel.setVisible(false);
    textStack.add(headlineLabel);
    textStack.add(subheadLabel);
    headerRow.add(textStack, BorderLayout.CENTER);

    trailingActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, INNER_GAP_PX / 2, 0));
    trailingActionsPanel.setOpaque(false);
    trailingActionsPanel.setVisible(false);
    chevronButton = ElwhaIconButton.standardIconButton(chevronIcon(collapsed));
    chevronButton.setVisible(false);
    chevronButton.addActionListener(e -> setCollapsed(!collapsed));

    final JPanel eastGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, INNER_GAP_PX / 2, 0));
    eastGroup.setOpaque(false);
    eastGroup.add(trailingActionsPanel);
    eastGroup.add(chevronButton);
    headerRow.add(eastGroup, BorderLayout.EAST);

    summaryHolder = newSlotHolder();
    mediaHolder = newMediaHolder();
    actionsHolder = newSlotHolder();

    supportingTextLabel = new JLabel();
    supportingTextLabel.setAlignmentX(0f);
    supportingTextLabel.setVisible(false);
    // M3 supporting-text spacing: vertical gap between header text and supporting text.
    supportingTextLabel.setBorder(BorderFactory.createEmptyBorder(INNER_GAP_PX, 0, 0, 0));

    collapsibleBody = new CollapsibleContainer();
    collapsibleBody.setOpaque(false);
    collapsibleBody.setLayout(new BoxLayout(collapsibleBody, BoxLayout.Y_AXIS));
    collapsibleBody.setAlignmentX(0f);
    // M3 Card anatomy order: image (media) at top, then headline / subhead, then supporting
    // text, then actions. The expanding-card pattern keeps image + headline always visible and
    // reveals supporting text + actions on expand (see M3 "Glass Souls" expanding example).
    collapsibleBody.add(supportingTextLabel);
    collapsibleBody.add(actionsHolder);

    // M3 Card padding model: media is full-bleed to the card edges; text content (header,
    // summary, supporting text, actions) lives inside a padded content wrapper. When media is
    // absent the content wrapper's top padding still gives the headline breathing room.
    contentArea = new JPanel();
    contentArea.setOpaque(false);
    contentArea.setLayout(new BoxLayout(contentArea, BoxLayout.Y_AXIS));
    contentArea.setAlignmentX(0f);
    contentArea.add(headerRow);
    contentArea.add(summaryHolder);
    contentArea.add(collapsibleBody);

    add(mediaHolder);
    add(contentArea);
  }

  private static JPanel newStretchingRow(final BorderLayout layout) {
    final JPanel p =
        new JPanel(layout) {
          @Override
          public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
          }
        };
    p.setOpaque(false);
    p.setAlignmentX(0f);
    return p;
  }

  private static JPanel newSlotHolder() {
    final JPanel p =
        new JPanel(new BorderLayout()) {
          @Override
          public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
          }
        };
    p.setOpaque(false);
    p.setBorder(BorderFactory.createEmptyBorder(INNER_GAP_PX, 0, 0, 0));
    p.setAlignmentX(0f);
    p.setVisible(false);
    return p;
  }

  /**
   * Media slot holder — no padding (media full-bleeds to card edges per M3) and a rounded-top clip
   * that matches the card's shape so the image's top corners hug the card's rounded corner instead
   * of poking past it.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private JPanel newMediaHolder() {
    final JPanel p =
        new JPanel(new BorderLayout()) {
          @Override
          public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
          }

          @Override
          protected void paintChildren(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
              g2.setRenderingHint(
                  RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              final int arc = getShape().px();
              final int w = getWidth();
              final int h = getHeight();
              // Clip to a shape with rounded top corners (matching the card) and square bottom
              // corners (text content continues below the media in M3's anatomy). Uses cubic
              // Bezier with the standard k = 0.55228 * r control distance so the corner curve is
              // visually indistinguishable from SurfacePainter's RoundRectangle2D corner —
              // eliminates
              // the sub-pixel mismatch a quadTo approximation would produce.
              final float r = arc;
              final float k = r * 0.5522847498f;
              final java.awt.geom.Path2D.Float clip = new java.awt.geom.Path2D.Float();
              clip.moveTo(0, r);
              clip.curveTo(0, r - k, r - k, 0, r, 0);
              clip.lineTo(w - r, 0);
              clip.curveTo(w - r + k, 0, w, r - k, w, r);
              clip.lineTo(w, h);
              clip.lineTo(0, h);
              clip.closePath();
              g2.clip(clip);
              super.paintChildren(g2);
            } finally {
              g2.dispose();
            }
          }
        };
    p.setOpaque(false);
    p.setAlignmentX(0f);
    p.setVisible(false);
    return p;
  }

  // ------------------------------------------------------------ interaction

  private void initInteraction() {
    final MouseAdapter ma =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            if (interactionMode != CardInteractionMode.STATIC && isEnabled()) {
              hovered = true;
              repaint();
            }
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            hovered = false;
            pressed = false;
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (!isEnabled()) {
              return;
            }
            if (collapsible && isInHeader(e)) {
              pendingHeaderToggle = true;
            }
            if (interactionMode == CardInteractionMode.CLICKABLE
                || interactionMode == CardInteractionMode.SELECTABLE
                || pendingHeaderToggle) {
              pressed = true;
              requestFocusInWindow();
              repaint();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!pressed || !isEnabled()) {
              pressed = false;
              pendingHeaderToggle = false;
              repaint();
              return;
            }
            pressed = false;
            if (contains(e.getPoint())) {
              if (pendingHeaderToggle) {
                pendingHeaderToggle = false;
                setCollapsed(!collapsed);
              } else {
                activate(e.getModifiersEx());
              }
            }
            pendingHeaderToggle = false;
            repaint();
          }
        };
    addMouseListener(ma);
    // chevronButton handles its own click via addActionListener -> setCollapsed; its mouse events
    // are consumed by the icon button so the parent header handler doesn't fire twice.

    addFocusListener(
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
        });

    final InputMap im = getInputMap(WHEN_FOCUSED);
    final ActionMap am = getActionMap();
    final Action activate =
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            if (!isEnabled()) {
              return;
            }
            if (collapsible) {
              setCollapsed(!collapsed);
            }
            activate(0);
          }
        };
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "elwhacard.activate");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwhacard.activate");
    am.put("elwhacard.activate", activate);
  }

  private boolean isInHeader(final MouseEvent e) {
    final Component src = e.getComponent();
    if (src == headerRow) {
      return true;
    }
    if (src == this) {
      return e.getY() < headerRow.getY() + headerRow.getHeight() && e.getY() >= headerRow.getY();
    }
    return false;
  }

  private void activate(final int modifiers) {
    if (interactionMode == CardInteractionMode.SELECTABLE) {
      setSelected(!selected);
    }
    if (interactionMode == CardInteractionMode.CLICKABLE
        || interactionMode == CardInteractionMode.SELECTABLE) {
      final ActionEvent evt =
          new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
      for (ActionListener l : new ArrayList<>(actionListeners)) {
        l.actionPerformed(evt);
      }
    }
  }

  // ---------------------------------------------------------------- variant

  /**
   * Sets the surface variant and applies the variant's default surface role, border role, and
   * elevation. Any prior {@link #setElevation(int)} override is preserved; explicit {@link
   * #setSurfaceRole(ColorRole)} / inherited border setters are reset to the new variant's defaults.
   *
   * @param variant the new variant; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setVariant(final CardVariant variant) {
    if (variant == null || variant == this.variant) {
      return this;
    }
    this.variant = variant;
    applyVariantDefaults(variant);
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns the active surface variant.
   *
   * @return the active variant (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardVariant getVariant() {
    return variant;
  }

  private void applyVariantDefaults(final CardVariant v) {
    setSurfaceRole(v.surfaceRole());
    setBorderRole(v.borderRole());
    setBorderWidth(v.borderRole() == null ? 0 : 1);
    if (!elevationUserSet) {
      elevation = v.restingElevation();
    }
  }

  // ---------------------------------------------------------- interaction

  /**
   * Sets the interaction mode. Drives cursor, focus, hover feedback, and which inputs fire {@link
   * ActionListener}s.
   *
   * @param mode the new mode; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setInteractionMode(final CardInteractionMode mode) {
    if (mode == null || mode == interactionMode) {
      return this;
    }
    interactionMode = mode;
    setFocusable(mode == CardInteractionMode.CLICKABLE || mode == CardInteractionMode.SELECTABLE);
    setCursor(
        mode == CardInteractionMode.STATIC
            ? Cursor.getDefaultCursor()
            : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    repaint();
    return this;
  }

  /**
   * Returns the active interaction mode.
   *
   * @return the active interaction mode (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardInteractionMode getInteractionMode() {
    return interactionMode;
  }

  // ------------------------------------------------------------ elevation

  /**
   * Overrides the variant-derived resting elevation (0..{@link #MAX_ELEVATION}). A subsequent
   * {@link #setVariant(CardVariant)} call will not reset this override.
   *
   * @param elevation desired resting elevation (0 disables the shadow)
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setElevation(final int elevation) {
    final int clamped = Math.max(0, Math.min(MAX_ELEVATION, elevation));
    elevationUserSet = true;
    if (clamped == this.elevation) {
      return this;
    }
    this.elevation = clamped;
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns the resting elevation in dp.
   *
   * @return the resting elevation (0..{@link #MAX_ELEVATION})
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getElevation() {
    return elevation;
  }

  /**
   * Toggles the dragged-state flag — paint switches to the variant's dragged elevation (Elevated→2,
   * Filled→8, Outlined→8 dp). Called by {@code ElwhaCardList} drag plumbing; not typically called
   * by application code.
   *
   * @param dragged whether the card is participating in a drag
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setDragged(final boolean dragged) {
    if (dragged == this.dragged) {
      return this;
    }
    this.dragged = dragged;
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns whether the card is currently in the dragged state.
   *
   * @return {@code true} if dragged
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isDragged() {
    return dragged;
  }

  private int effectiveElevation() {
    return dragged ? variant.draggedElevation() : elevation;
  }

  // -------------------------------------------------------------- padding

  /**
   * Sets the content padding from the spacing scale — {@code horizontal} on left/right, {@code
   * vertical} on top/bottom. Token-typed; the V1 raw-{@code Insets} / int setters are gone.
   *
   * @param horizontal left/right padding step ({@code null} ignored)
   * @param vertical top/bottom padding step ({@code null} ignored)
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setPadding(final SpaceScale horizontal, final SpaceScale vertical) {
    if (horizontal != null) {
      paddingHorizontal = horizontal;
    }
    if (vertical != null) {
      paddingVertical = vertical;
    }
    rebuildBorder();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active content padding as resolved {@link Insets} (defensive copy).
   *
   * @return the content padding
   * @version v0.1.0
   * @since v0.1.0
   */
  public Insets getPadding() {
    return SpaceScale.insets(paddingVertical, paddingHorizontal);
  }

  // ------------------------------------------------------ M3 formal slots

  /**
   * Sets the headline text. Pass {@code null} or empty to hide the headline row.
   *
   * @param headline the headline text
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setHeadline(final String headline) {
    headlineLabel.setText(headline == null ? "" : headline);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns the current headline text (never {@code null}; empty if unset).
   *
   * @return the headline
   * @version v0.1.0
   * @since v0.1.0
   */
  public String getHeadline() {
    return headlineLabel.getText() == null ? "" : headlineLabel.getText();
  }

  /**
   * Sets the subhead text. Pass {@code null} or empty to hide the subhead.
   *
   * @param subhead the subhead text
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setSubhead(final String subhead) {
    final boolean has = subhead != null && !subhead.isEmpty();
    subheadLabel.setText(has ? subhead : "");
    subheadLabel.setVisible(has);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns the current subhead text (never {@code null}; empty if unset).
   *
   * @return the subhead
   * @version v0.1.0
   * @since v0.1.0
   */
  public String getSubhead() {
    return subheadLabel.getText() == null ? "" : subheadLabel.getText();
  }

  /**
   * Sets the supporting-text block (multi-line text body, M3 formal slot). Pass {@code null} or
   * empty to hide.
   *
   * @param text the supporting text
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setSupportingText(final String text) {
    supportingText = text;
    final boolean has = text != null && !text.isEmpty();
    supportingTextLabel.setText(has ? wrapHtml(text) : "");
    supportingTextLabel.setVisible(has);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the current supporting text, or {@code null} if unset.
   *
   * @return the supporting text
   * @version v0.1.0
   * @since v0.1.0
   */
  public String getSupportingText() {
    return supportingText;
  }

  private static String wrapHtml(final String text) {
    return "<html><body style='width: 100%'>" + text + "</body></html>";
  }

  /**
   * Sets the media slot (rendered above the supporting text, full-bleed by parent layout). Pass
   * {@code null} to clear.
   *
   * @param media the media component
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setMedia(final JComponent media) {
    mediaHolder.removeAll();
    this.media = media;
    if (media != null) {
      mediaHolder.add(media, BorderLayout.CENTER);
    }
    mediaHolder.setVisible(media != null);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the current media component, or {@code null}.
   *
   * @return the media component
   * @version v0.1.0
   * @since v0.1.0
   */
  public JComponent getMedia() {
    return media;
  }

  /**
   * Sets the bottom actions row (M3 formal "actions" slot — right-aligned by default). Pass an
   * empty array (or {@code null}) to clear.
   *
   * @param actions zero or more action components rendered left to right
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setActions(final Component... actions) {
    actionsHolder.removeAll();
    if (actions != null && actions.length > 0) {
      final JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, INNER_GAP_PX, 0));
      row.setOpaque(false);
      for (Component c : actions) {
        if (c != null) {
          row.add(c);
        }
      }
      actionsHolder.add(row, BorderLayout.CENTER);
    }
    actionsHolder.setVisible(actions != null && actions.length > 0);
    revalidate();
    repaint();
    return this;
  }

  // ------------------------------------------------ OWS header extensions

  /**
   * Replaces the leading icon shown at the left edge of the header row. Pass {@code null} to clear.
   *
   * @param icon the icon, or {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setLeadingIcon(final Icon icon) {
    leadingIcon = icon;
    leadingIconLabel.setIcon(icon);
    leadingIconLabel.setVisible(icon != null);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns the leading icon, or {@code null}.
   *
   * @return the leading icon
   * @version v0.1.0
   * @since v0.1.0
   */
  public Icon getLeadingIcon() {
    return leadingIcon;
  }

  /**
   * Replaces the leading actions row — sits between the leading icon and the headline. Typed to
   * {@link ElwhaIconButton} per the M3 Top App Bar convention for header trailing/leading
   * affordances. Pass an empty array (or {@code null}) to clear.
   *
   * @param actions zero or more {@link ElwhaIconButton}s rendered left to right
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setLeadingActions(final ElwhaIconButton... actions) {
    leadingActionsPanel.removeAll();
    if (actions != null) {
      for (ElwhaIconButton b : actions) {
        if (b != null) {
          leadingActionsPanel.add(b);
        }
      }
    }
    leadingActionsPanel.setVisible(leadingActionsPanel.getComponentCount() > 0);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Replaces the trailing actions row — sits to the right of the headline, before the chevron.
   * Typed to {@link ElwhaIconButton} per the M3 Top App Bar convention. For a single overflow
   * affordance the M3-conventional choice is {@code
   * ElwhaIconButton.standardIconButton(MaterialIcons.moreVert())}. Pass an empty array (or {@code
   * null}) to clear.
   *
   * @param actions zero or more {@link ElwhaIconButton}s rendered left to right
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setTrailingActions(final ElwhaIconButton... actions) {
    trailingActionsPanel.removeAll();
    if (actions != null) {
      for (ElwhaIconButton b : actions) {
        if (b != null) {
          trailingActionsPanel.add(b);
        }
      }
    }
    trailingActionsPanel.setVisible(trailingActionsPanel.getComponentCount() > 0);
    refreshHeaderVisibility();
    return this;
  }

  // ----------------------------------------------------- disclosure axis

  /**
   * Enables or disables the chevron + click-to-collapse behavior.
   *
   * @param collapsible whether the card can collapse
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setCollapsible(final boolean collapsible) {
    if (collapsible == this.collapsible) {
      return this;
    }
    this.collapsible = collapsible;
    chevronButton.setVisible(collapsible);
    if (!collapsible && collapsed) {
      setCollapsed(false);
    }
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns whether the card supports collapse / expand.
   *
   * @return {@code true} if collapsible
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isCollapsible() {
    return collapsible;
  }

  /**
   * Programmatically sets the collapsed state. Fires a {@link #PROPERTY_COLLAPSED} change event.
   *
   * @param collapsed the new collapsed state
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setCollapsed(final boolean collapsed) {
    if (collapsed == this.collapsed) {
      return this;
    }
    final boolean old = this.collapsed;
    this.collapsed = collapsed;
    chevronButton.setIcon(chevronIcon(collapsed));
    summaryHolder.setVisible(shouldShowSummary());
    if (animateCollapse) {
      animateTo(collapsed ? 0f : 1f);
    } else {
      animationFraction = collapsed ? 0f : 1f;
      collapsibleBody.setVisible(!collapsed);
    }
    revalidate();
    repaint();
    firePropertyChange(PROPERTY_COLLAPSED, old, collapsed);
    return this;
  }

  /**
   * Returns whether the card is currently collapsed.
   *
   * @return {@code true} if collapsed
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Sets the summary band — visibility governed by {@link
   * #setSummaryVisibility(SummaryVisibility)}. Replaces V1's {@code
   * setCollapsedSummary(JComponent)}.
   *
   * @param summary the summary component (typically a chip row, metric, or one-line label), or
   *     {@code null} to clear
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setSummary(final JComponent summary) {
    summaryHolder.removeAll();
    this.summary = summary;
    if (summary != null) {
      summaryHolder.add(summary, BorderLayout.CENTER);
    }
    summaryHolder.setVisible(shouldShowSummary());
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the current summary component, or {@code null}.
   *
   * @return the summary component
   * @version v0.1.0
   * @since v0.1.0
   */
  public JComponent getSummary() {
    return summary;
  }

  /**
   * Sets the summary visibility policy — see {@link SummaryVisibility}. Replaces V1's {@code
   * setKeepSummaryWhenExpanded(boolean)} escape hatch with a first-class enum.
   *
   * @param visibility the new visibility policy; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setSummaryVisibility(final SummaryVisibility visibility) {
    if (visibility == null || visibility == summaryVisibility) {
      return this;
    }
    summaryVisibility = visibility;
    summaryHolder.setVisible(shouldShowSummary());
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active summary visibility policy.
   *
   * @return the policy (never {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public SummaryVisibility getSummaryVisibility() {
    return summaryVisibility;
  }

  private boolean shouldShowSummary() {
    if (summary == null) {
      return false;
    }
    return summaryVisibility == SummaryVisibility.ALWAYS || collapsed;
  }

  /**
   * Toggles smooth height interpolation when collapsing / expanding. Off by default.
   *
   * @param animate whether to animate
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setAnimateCollapse(final boolean animate) {
    animateCollapse = animate;
    return this;
  }

  /**
   * Returns whether the collapse / expand transition is animated.
   *
   * @return {@code true} if animated
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isAnimateCollapse() {
    return animateCollapse;
  }

  // ------------------------------------------------------------- selection

  /**
   * Sets the selected state. Fires a {@link #PROPERTY_SELECTED} change event; paint composites the
   * M3 top-trailing checked-icon overlay when {@code true}.
   *
   * @param selected the new selected state
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard setSelected(final boolean selected) {
    if (selected == this.selected) {
      return this;
    }
    final boolean old = this.selected;
    this.selected = selected;
    repaint();
    firePropertyChange(PROPERTY_SELECTED, old, selected);
    return this;
  }

  /**
   * Returns the current selected state.
   *
   * @return {@code true} if selected
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isSelected() {
    return selected;
  }

  // ------------------------------------------------------------- listeners

  /**
   * Cancels any in-flight click that {@link #initInteraction()} saw on {@code mousePressed} but
   * hasn't completed via {@code mouseReleased}. Called by {@code ElwhaCardList} drag plumbing once
   * it commits to a drag, suppressing the otherwise-spurious header toggle or activation.
   *
   * @return {@code this} for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaCard cancelPendingClick() {
    pressed = false;
    pendingHeaderToggle = false;
    repaint();
    return this;
  }

  /**
   * Registers an action listener — fires on click ({@link CardInteractionMode#CLICKABLE}) or on
   * toggle ({@link CardInteractionMode#SELECTABLE}).
   *
   * @param listener the listener (ignored if {@code null})
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addActionListener(final ActionListener listener) {
    if (listener != null) {
      actionListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered action listener.
   *
   * @param listener the listener to remove
   * @version v0.1.0
   * @since v0.1.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  /**
   * Registers a {@link PropertyChangeListener} scoped to {@link #PROPERTY_SELECTED}. Replaces V1's
   * generic {@code onChange(String, PCL)} subscription.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addSelectionChangeListener(final PropertyChangeListener listener) {
    addPropertyChangeListener(PROPERTY_SELECTED, listener);
  }

  /**
   * Removes a previously registered selection-change listener.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void removeSelectionChangeListener(final PropertyChangeListener listener) {
    removePropertyChangeListener(PROPERTY_SELECTED, listener);
  }

  /**
   * Registers a {@link PropertyChangeListener} scoped to {@link #PROPERTY_COLLAPSED}.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addExpansionChangeListener(final PropertyChangeListener listener) {
    addPropertyChangeListener(PROPERTY_COLLAPSED, listener);
  }

  /**
   * Removes a previously registered expansion-change listener.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  public void removeExpansionChangeListener(final PropertyChangeListener listener) {
    removePropertyChangeListener(PROPERTY_COLLAPSED, listener);
  }

  // -------------------------------------------------------------- internals

  private void refreshHeaderVisibility() {
    final boolean hasHeadline =
        headlineLabel.getText() != null && !headlineLabel.getText().isEmpty();
    final boolean any =
        hasHeadline
            || subheadLabel.isVisible()
            || leadingIconLabel.isVisible()
            || leadingActionsPanel.isVisible()
            || trailingActionsPanel.isVisible()
            || collapsible;
    headerRow.setVisible(any);
    headerRow.revalidate();
  }

  private void rebuildBorder() {
    final Insets shadow = shadowInsets();
    // Card outer border carries only the drop-shadow inset. Content padding lives on the inner
    // contentArea wrapper so media (mediaHolder) can full-bleed to the card's edges per M3.
    setBorder(
        BorderFactory.createEmptyBorder(shadow.top, shadow.left, shadow.bottom, shadow.right));
    if (contentArea != null) {
      contentArea.setBorder(
          BorderFactory.createEmptyBorder(
              paddingVertical.px(),
              paddingHorizontal.px(),
              paddingVertical.px(),
              paddingHorizontal.px()));
    }
  }

  private Insets shadowInsets() {
    final int e = effectiveElevation();
    if (e <= 0) {
      return new Insets(0, 0, 0, 0);
    }
    // Reserve just enough gutter for the rendered shadow to fit inside the component:
    // - 1px top: a thin ambient halo sliver above the card
    // - lateral (e/2 + 1): ambient halo on the sides
    // - bottom (e + 2): key + ambient drop offset
    final int lateral = Math.max(1, e / 2 + 1);
    return new Insets(1, lateral, e + 2, lateral);
  }

  private static final int CHEVRON_ICON_PX = 20;

  private static Icon chevronIcon(final boolean collapsed) {
    return collapsed
        ? MaterialIcons.expandMore(CHEVRON_ICON_PX)
        : MaterialIcons.expandLess(CHEVRON_ICON_PX);
  }

  private void animateTo(final float target) {
    if (animationTimer != null && animationTimer.isRunning()) {
      animationTimer.stop();
    }
    final float start = animationFraction;
    final long startTime = System.currentTimeMillis();
    if (target > 0) {
      collapsibleBody.setVisible(true);
    }
    animationTimer =
        new Timer(
            ANIMATION_MS / ANIMATION_STEPS,
            e -> {
              final float t =
                  Math.min(1f, (System.currentTimeMillis() - startTime) / (float) ANIMATION_MS);
              final float ease = (float) (1 - Math.pow(1 - t, 3));
              animationFraction = start + (target - start) * ease;
              collapsibleBody.revalidate();
              collapsibleBody.repaint();
              revalidate();
              repaint();
              if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                animationFraction = target;
                if (target == 0f) {
                  collapsibleBody.setVisible(false);
                }
              }
            });
    animationTimer.start();
  }

  // -------------------------------------------------------------- painting

  /**
   * Paints the card — drop shadow (elevation-driven), Surface-painter pass for the rounded fill and
   * border (with hover / pressed state-layer overlay), focus ring, and the M3 top-trailing
   * checked-icon overlay when selected.
   *
   * @param g the graphics context
   * @version v0.1.0
   * @since v0.1.0
   */
  /**
   * Paints under the children: drop shadow (M3 key + ambient layers) and the rounded surface fill +
   * border with hover / pressed state-layer overlay (delegated to {@link SurfacePainter}).
   *
   * @param g the graphics context
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final Insets shadow = shadowInsets();
      final int x = shadow.left;
      final int y = shadow.top;
      final int w = getWidth() - shadow.left - shadow.right;
      final int h = getHeight() - shadow.top - shadow.bottom;
      final int arc = getShape().px();

      paintShadow(g2, x, y, w, h, arc);

      final Graphics2D inner = (Graphics2D) g2.create(x, y, w, h);
      try {
        inner.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        SurfacePainter.paint(
            inner, w, h, arc, getSurfaceRole(), activeOverlay(), getBorderRole(), getBorderWidth());
      } finally {
        inner.dispose();
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Paints over the children: disabled scrim, focus ring, and the M3 top-trailing checked-icon
   * selection badge. Painting these on top of children ensures the selection badge sits above the
   * media slot (which would otherwise hide it).
   *
   * @param g the graphics context
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);
    if (!isEnabled() && interactionMode == CardInteractionMode.STATIC && !selected) {
      // Nothing decorative needs to paint over disabled static cards.
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final Insets shadow = shadowInsets();
      final int x = shadow.left;
      final int y = shadow.top;
      final int w = getWidth() - shadow.left - shadow.right;
      final int h = getHeight() - shadow.top - shadow.bottom;
      final int arc = getShape().px();

      if (!isEnabled()) {
        paintDisabledScrim(g2, x, y, w, h, arc);
      }
      if (isFocusOwner() && interactionMode != CardInteractionMode.STATIC && isEnabled()) {
        paintFocusRing(g2, x, y, w, h, arc);
      }
      if (selected) {
        paintCheckedBadge(g2, x, y, w);
      }
    } finally {
      g2.dispose();
    }
  }

  private StateLayer activeOverlay() {
    if (!isEnabled() || interactionMode == CardInteractionMode.STATIC) {
      return null;
    }
    if (pressed) {
      return StateLayer.PRESSED;
    }
    if (hovered) {
      return StateLayer.HOVER;
    }
    return null;
  }

  /**
   * Paints an M3-style elevation drop shadow as a two-layer composite: an ambient halo that extends
   * slightly past the card on three sides (left, right, bottom) below the top edge, plus a key
   * shadow offset straight down just below the card. Neither layer paints above the card's top edge
   * — the top edge stays crisp.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void paintShadow(
      final Graphics2D g2, final int x, final int y, final int w, final int h, final int arc) {
    final int e = effectiveElevation();
    if (e <= 0) {
      return;
    }
    final int clamped = Math.min(e, MAX_ELEVATION);

    // Stack of progressively-larger soft-shadow layers, each at low alpha. Stacking many low-
    // alpha layers approximates a Gaussian-blur drop shadow without the cost of a ConvolveOp
    // pass. Top edge of every layer sits 1px below the card top so the card top silhouette
    // stays crisp.
    final int layers = 6;
    final int maxOffsetY = clamped + 1;
    final int maxSpread = Math.max(1, clamped / 2);
    final int basePerLayerAlpha = Math.max(10, 24 - (16 - 2 * clamped));
    // basePerLayerAlpha grows from ~10 (e=1) to ~24 (e=8) so high-elevation cards have a
    // noticeably stronger total shadow.
    for (int i = 1; i <= layers; i++) {
      final float t = (float) i / layers;
      final int offsetY = Math.round(maxOffsetY * t);
      final int spread = Math.round(maxSpread * t);
      g2.setColor(new Color(0, 0, 0, basePerLayerAlpha));
      g2.fill(
          new java.awt.geom.RoundRectangle2D.Float(
              x - spread,
              y + 1 + Math.max(0, offsetY - spread),
              w + 2f * spread,
              h + offsetY,
              arc + spread,
              arc + spread));
    }

    // Key: tight crisp drop directly below the card, no lateral spread. Drives the "lifted"
    // perception and reads as a clear bottom edge separator at all elevations.
    final int keyOffset = Math.max(1, clamped / 2);
    final int keyAlpha = 30 + (int) Math.round(clamped * 3.0);
    g2.setColor(new Color(0, 0, 0, Math.min(70, keyAlpha)));
    g2.fill(new java.awt.geom.RoundRectangle2D.Float(x, y + keyOffset + 1, w, h, arc, arc));
  }

  private void paintDisabledScrim(
      final Graphics2D g2, final int x, final int y, final int w, final int h, final int arc) {
    final Graphics2D dim = (Graphics2D) g2.create();
    try {
      dim.setComposite(AlphaComposite.SrcOver.derive(StateLayer.disabledContainerOpacity()));
      final Color base = getSurfaceRole() != null ? getSurfaceRole().resolve() : Color.WHITE;
      dim.setColor(base);
      dim.fill(new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, arc, arc));
    } finally {
      dim.dispose();
    }
  }

  private void paintFocusRing(
      final Graphics2D g2, final int x, final int y, final int w, final int h, final int arc) {
    final Color ring = ColorRole.PRIMARY.resolve();
    g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 220));
    g2.setStroke(new java.awt.BasicStroke(2f));
    g2.draw(new java.awt.geom.RoundRectangle2D.Float(x + 1f, y + 1f, w - 2f, h - 2f, arc, arc));
  }

  private void paintCheckedBadge(final Graphics2D g2, final int x, final int y, final int w) {
    final int pad = SpaceScale.SM.px();
    final int diameter = CHECKED_BADGE_DIAMETER;
    final int bx = x + w - diameter - pad;
    final int by = y + pad;
    final Color fill = ColorRole.PRIMARY.resolve();
    final Color glyph = ColorRole.PRIMARY.on().orElse(ColorRole.ON_PRIMARY).resolve();
    g2.setColor(fill);
    g2.fillOval(bx, by, diameter, diameter);
    final FlatSVGIcon check = MaterialIcons.check(CHECKED_BADGE_ICON_PX);
    check.setColorFilter(new FlatSVGIcon.ColorFilter(orig -> glyph));
    final int iconOffset = (diameter - CHECKED_BADGE_ICON_PX) / 2;
    check.paintIcon(this, g2, bx + iconOffset, by + iconOffset);
  }

  // --------------------------------------------------------------- LAF hooks

  /**
   * Re-applies the subhead's disabled-foreground colour after a LAF change.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    if (subheadLabel != null) {
      subheadLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    }
    // updateUI() fires from the JPanel super-ctor before our field initializers run;
    // skip the border rebuild on that first call and let the explicit rebuildBorder()
    // in our own constructor pick it up.
    if (paddingHorizontal != null && paddingVertical != null) {
      rebuildBorder();
    }
    repaint();
  }

  /**
   * Re-applies the cursor for the current interaction mode after an enabled-state change.
   *
   * @param enabled the new enabled state
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(
        enabled && interactionMode != CardInteractionMode.STATIC
            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
    repaint();
  }

  // --------------------------------------------------------------- container

  private final class CollapsibleContainer extends JPanel {
    @Override
    public Dimension getPreferredSize() {
      final Dimension d = super.getPreferredSize();
      if (animationFraction >= 1f) {
        return d;
      }
      return new Dimension(d.width, (int) Math.max(0, d.height * animationFraction));
    }

    @Override
    public Dimension getMinimumSize() {
      final Dimension p = getPreferredSize();
      return new Dimension(0, p.height);
    }

    @Override
    public Dimension getMaximumSize() {
      final Dimension d = super.getPreferredSize();
      final int h =
          animationFraction >= 1f ? d.height : (int) Math.max(0, d.height * animationFraction);
      return new Dimension(Integer.MAX_VALUE, h);
    }
  }
}
