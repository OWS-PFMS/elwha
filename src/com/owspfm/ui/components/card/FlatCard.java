package com.owspfm.ui.components.card;

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
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * A standalone, FlatLaf-aware card primitive that mirrors modern web/React card semantics.
 *
 * <p>{@code FlatCard} composes optional <em>header</em>, <em>media</em>, <em>body</em>, and
 * <em>footer</em> slots into a single panel with a unified rounded surface. The visual treatment is
 * selectable via {@link CardVariant}; the interaction semantics are selectable via {@link
 * CardInteractionMode}; both can be combined freely with collapsible behavior.
 *
 * <p>The component reads colors and the corner radius from FlatLaf {@link UIManager} keys, so it
 * tracks light/dark theme switches without caller intervention.
 *
 * <p><strong>Quick start</strong>:
 *
 * <pre>{@code
 * FlatCard card = new FlatCard()
 *     .setVariant(CardVariant.ELEVATED)
 *     .setHeader("Recent activity", "Last 30 days")
 *     .setBody(new JLabel("12 cycles found across 4 factors."))
 *     .setFooter(new JButton("Open"), new JButton("Dismiss"))
 *     .setInteractionMode(CardInteractionMode.HOVERABLE);
 * }</pre>
 *
 * <p><strong>Collapsible</strong>:
 *
 * <pre>{@code
 * FlatCard card = new FlatCard()
 *     .setHeader("Advanced options")
 *     .setBody(buildOptionsPanel())
 *     .setCollapsible(true)
 *     .setCollapsed(true);
 * card.addPropertyChangeListener("collapsed", evt -> ...);
 * }</pre>
 *
 * <p><strong>Click-as-button</strong>:
 *
 * <pre>{@code
 * FlatCard card = new FlatCard()
 *     .setHeader("Open project")
 *     .setBody(buildPreview())
 *     .setInteractionMode(CardInteractionMode.CLICKABLE);
 * card.addActionListener(evt -> openProject());
 * }</pre>
 *
 * <p>This class has no dependencies on application code; it can be lifted into its own library by
 * moving the {@code com.owspfm.ui.components.card} directory.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class FlatCard extends JPanel {

  /** Property name fired when the collapsed state changes. */
  public static final String PROPERTY_COLLAPSED = "collapsed";

  /** Property name fired when the selected state changes. */
  public static final String PROPERTY_SELECTED = "selected";

  /** Maximum supported elevation level (0 disables the shadow entirely). */
  public static final int MAX_ELEVATION = 5;

  private static final int DEFAULT_PADDING = 16;
  private static final int DEFAULT_INNER_GAP = 8;
  private static final int DEFAULT_ARC = 12;
  private static final int ANIMATION_MS = 160;
  private static final int ANIMATION_STEPS = 10;

  // Configuration ----------------------------------------------------------
  private CardVariant variant = CardVariant.ELEVATED;
  private CardInteractionMode interactionMode = CardInteractionMode.STATIC;
  private int elevation = 1;
  private Integer cornerRadius;
  private Insets padding =
      new Insets(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
  private Color borderColor;
  private Color surfaceColorOverride;
  private int borderWidth = 1;
  private boolean animateCollapse;

  // State ------------------------------------------------------------------
  private boolean hovered;
  private boolean pressed;
  private boolean selected;
  private boolean collapsible;
  private boolean collapsed;
  private boolean keepSummaryWhenExpanded;
  private boolean pendingHeaderToggle;

  // Header sub-components --------------------------------------------------
  private JPanel headerRow;
  private JLabel leadingIconLabel;
  private JPanel textStack;
  private JLabel titleLabel;
  private JLabel subtitleLabel;
  private JPanel leadingActionsPanel;
  private JPanel trailingActionsPanel;
  private JLabel chevronLabel;

  // Slot containers --------------------------------------------------------
  private CollapsibleContainer collapsibleBody;
  private JPanel mediaHolder;
  private JPanel bodyHolder;
  private JPanel footerHolder;
  private JPanel collapsedSummaryHolder;

  // User-supplied content --------------------------------------------------
  private JComponent media;
  private JComponent body;
  private JComponent footer;
  private JComponent collapsedSummary;

  // Listeners --------------------------------------------------------------
  private final List<ActionListener> actionListeners = new ArrayList<>();

  // Animation --------------------------------------------------------------
  private Timer animationTimer;
  private float animationFraction = 1f;

  // ------------------------------------------------------------------ ctor

  /** Creates a card with default ELEVATED variant and STATIC interaction. */
  public FlatCard() {
    super();
    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    initSlots();
    initInteraction();
    rebuildBorder();
  }

  // ----------------------------------------------------------------- slots

  private void initSlots() {
    headerRow = newStretchingRow(new BorderLayout(DEFAULT_INNER_GAP, 0), 0);
    headerRow.setVisible(false);

    leadingIconLabel = new JLabel();
    leadingIconLabel.setVisible(false);
    final JPanel westGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, DEFAULT_INNER_GAP / 2, 0));
    westGroup.setOpaque(false);
    westGroup.add(leadingIconLabel);
    leadingActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, DEFAULT_INNER_GAP / 2, 0));
    leadingActionsPanel.setOpaque(false);
    leadingActionsPanel.setVisible(false);
    westGroup.add(leadingActionsPanel);
    headerRow.add(westGroup, BorderLayout.WEST);

    textStack = new JPanel();
    textStack.setOpaque(false);
    textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
    titleLabel = new JLabel();
    titleLabel.putClientProperty("FlatLaf.styleClass", "h4");
    titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    subtitleLabel = new JLabel();
    subtitleLabel.putClientProperty("FlatLaf.styleClass", "small");
    subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    subtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    subtitleLabel.setVisible(false);
    textStack.add(titleLabel);
    textStack.add(subtitleLabel);
    headerRow.add(textStack, BorderLayout.CENTER);

    JPanel eastGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, DEFAULT_INNER_GAP / 2, 0));
    eastGroup.setOpaque(false);
    trailingActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, DEFAULT_INNER_GAP / 2, 0));
    trailingActionsPanel.setOpaque(false);
    trailingActionsPanel.setVisible(false);
    eastGroup.add(trailingActionsPanel);
    chevronLabel = new JLabel(chevronGlyph(collapsed));
    chevronLabel.setHorizontalAlignment(SwingConstants.CENTER);
    chevronLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    chevronLabel.setVisible(false);
    // Chevron is a button-like affordance and should always show the click cursor, even when
    // the host card has a different cursor (e.g., MOVE_CURSOR set by FlatCardList for drag).
    chevronLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    eastGroup.add(chevronLabel);
    headerRow.add(eastGroup, BorderLayout.EAST);

    mediaHolder = newSlotHolder();
    bodyHolder = newSlotHolder();
    footerHolder = newSlotHolder();
    collapsedSummaryHolder = newSlotHolder();

    collapsibleBody = new CollapsibleContainer();
    collapsibleBody.setOpaque(false);
    collapsibleBody.setLayout(new BoxLayout(collapsibleBody, BoxLayout.Y_AXIS));
    collapsibleBody.setAlignmentX(0f);
    collapsibleBody.add(mediaHolder);
    collapsibleBody.add(bodyHolder);
    collapsibleBody.add(footerHolder);

    add(headerRow);
    add(collapsedSummaryHolder);
    add(collapsibleBody);
    collapsedSummaryHolder.setVisible(false);
  }

  /**
   * Creates a row panel that stretches horizontally to fill its parent BoxLayout but locks
   * vertically to its preferred height.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static JPanel newStretchingRow(final BorderLayout layout, final int topGap) {
    JPanel p =
        new JPanel(layout) {
          @Override
          public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
          }
        };
    p.setOpaque(false);
    p.setAlignmentX(0f);
    if (topGap > 0) {
      p.setBorder(BorderFactory.createEmptyBorder(topGap, 0, 0, 0));
    }
    return p;
  }

  private static JPanel newSlotHolder() {
    JPanel p =
        new JPanel(new BorderLayout()) {
          @Override
          public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
          }
        };
    p.setOpaque(false);
    p.setBorder(BorderFactory.createEmptyBorder(DEFAULT_INNER_GAP, 0, 0, 0));
    p.setAlignmentX(0f);
    p.setVisible(false);
    return p;
  }

  // ------------------------------------------------------------- interaction

  private void initInteraction() {
    MouseAdapter ma =
        new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent e) {
            if (interactionMode != CardInteractionMode.STATIC && isEnabled()) {
              hovered = true;
              repaint();
            }
          }

          @Override
          public void mouseExited(MouseEvent e) {
            hovered = false;
            pressed = false;
            repaint();
          }

          @Override
          public void mousePressed(MouseEvent e) {
            if (!isEnabled()) {
              return;
            }
            if (collapsible && isInHeader(e)) {
              // Defer the actual collapse toggle to mouseReleased — that way a hosting list can
              // call cancelPendingClick() if the press turns into a drag, suppressing what
              // would otherwise be an unwanted toggle on every drag start.
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
          public void mouseReleased(MouseEvent e) {
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
    // Intentionally NOT installing this listener on headerRow: when a child has its own
    // mouse listener, AWT delivers the event there and stops, which prevents outer listeners
    // (e.g., a host FlatCardList that wants to handle selection on header clicks) from ever
    // seeing the event. The card-level listener detects header clicks via Y-bounds in
    // isInHeader(), so the toggle still fires correctly.
    chevronLabel.addMouseListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            repaint();
          }

          @Override
          public void focusLost(FocusEvent e) {
            pressed = false;
            repaint();
          }
        });

    InputMap im = getInputMap(WHEN_FOCUSED);
    ActionMap am = getActionMap();
    Action activate =
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) {
              return;
            }
            if (collapsible) {
              toggleCollapsed(null);
            }
            activate(0);
          }
        };
    KeyStroke space = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
    KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    im.put(space, "flatcard.activate");
    im.put(enter, "flatcard.activate");
    am.put("flatcard.activate", activate);
  }

  private boolean isInHeader(MouseEvent e) {
    Component src = e.getComponent();
    if (src == headerRow || src == chevronLabel) {
      return true;
    }
    if (src == this) {
      return e.getY() < headerRow.getY() + headerRow.getHeight() && e.getY() >= headerRow.getY();
    }
    return false;
  }

  private void activate(int modifiers) {
    if (interactionMode == CardInteractionMode.SELECTABLE) {
      setSelected(!selected);
    }
    if (interactionMode == CardInteractionMode.CLICKABLE
        || interactionMode == CardInteractionMode.SELECTABLE) {
      ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
      for (ActionListener l : new ArrayList<>(actionListeners)) {
        l.actionPerformed(evt);
      }
    }
  }

  private void toggleCollapsed(MouseEvent ignored) {
    setCollapsed(!collapsed);
  }

  // ----------------------------------------------------------- public API

  /**
   * Sets the surface variant.
   *
   * @param variant one of {@link CardVariant}; ignored if null
   * @return this card for fluent chaining
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setVariant(final CardVariant variant) {
    if (variant == null || variant == this.variant) {
      return this;
    }
    this.variant = variant;
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns the active surface variant.
   *
   * @return the active variant (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardVariant getVariant() {
    return variant;
  }

  /**
   * Sets the interaction mode.
   *
   * @param mode one of {@link CardInteractionMode}; ignored if null
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setInteractionMode(final CardInteractionMode mode) {
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
   * @return the active interaction mode (never null)
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardInteractionMode getInteractionMode() {
    return interactionMode;
  }

  /**
   * Sets the elevation level (clamped to {@code 0..MAX_ELEVATION}). Only effective on the {@link
   * CardVariant#ELEVATED} variant.
   *
   * @param elevation desired elevation (0 disables shadow)
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setElevation(final int elevation) {
    int v = Math.max(0, Math.min(MAX_ELEVATION, elevation));
    if (v == this.elevation) {
      return this;
    }
    this.elevation = v;
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns the current elevation level.
   *
   * @return current elevation level (0..MAX_ELEVATION)
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getElevation() {
    return elevation;
  }

  /**
   * Overrides the corner radius. Pass {@code null} to fall back to the FlatLaf {@code
   * Component.arc} key.
   *
   * @param radius the new arc, or null for theme default
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setCornerRadius(final Integer radius) {
    cornerRadius = radius;
    repaint();
    return this;
  }

  /**
   * Returns the corner radius actually used by paint, resolving the override or theme default.
   *
   * @return effective corner radius, resolved against UIManager when not overridden
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getEffectiveCornerRadius() {
    if (cornerRadius != null) {
      return cornerRadius;
    }
    Object arc = UIManager.get("Component.arc");
    if (arc instanceof Number n) {
      return Math.max(0, n.intValue());
    }
    return DEFAULT_ARC;
  }

  /**
   * Replaces the content padding (the gap between the rounded surface and the slots).
   *
   * @param insets new padding; null is treated as zero on all sides
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setPadding(final Insets insets) {
    padding = insets == null ? new Insets(0, 0, 0, 0) : (Insets) insets.clone();
    rebuildBorder();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Convenience for {@link #setPadding(Insets)} with uniform spacing.
   *
   * @param padding spacing applied to all four sides
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setPadding(final int padding) {
    return setPadding(new Insets(padding, padding, padding, padding));
  }

  /**
   * Returns the configured content padding.
   *
   * @return current content padding (defensive copy)
   * @version v0.1.0
   * @since v0.1.0
   */
  public Insets getPadding() {
    return (Insets) padding.clone();
  }

  /**
   * Overrides the border color. Pass {@code null} to derive from the theme.
   *
   * @param color explicit border color, or null
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setBorderColor(final Color color) {
    borderColor = color;
    repaint();
    return this;
  }

  /**
   * Sets border width in pixels. Only visible on the {@link CardVariant#OUTLINED} variant.
   *
   * @param width border thickness, clamped to {@code >= 0}
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setBorderWidth(final int width) {
    borderWidth = Math.max(0, width);
    repaint();
    return this;
  }

  // --- header slot -----

  /**
   * Sets the header title. Pass null or empty to hide the title row entirely.
   *
   * @param title the header title text
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setHeader(final String title) {
    return setHeader(title, null);
  }

  /**
   * Sets the header title and subtitle. Does not modify the leading icon — use {@link
   * #setLeadingIcon(Icon)} or the three-arg {@link #setHeader(String, String, Icon)} overload to
   * change it.
   *
   * @param title the title text (null/empty hides the title text)
   * @param subtitle the subtitle text (null/empty hides the subtitle line)
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setHeader(final String title, final String subtitle) {
    boolean hasTitle = title != null && !title.isEmpty();
    titleLabel.setText(hasTitle ? title : "");
    boolean hasSub = subtitle != null && !subtitle.isEmpty();
    subtitleLabel.setText(hasSub ? subtitle : "");
    subtitleLabel.setVisible(hasSub);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Sets the header title, subtitle, and leading icon. Pass {@code null} for {@code leadingIcon} to
   * explicitly clear the icon.
   *
   * @param title the title text
   * @param subtitle the subtitle text (may be null)
   * @param leadingIcon the icon shown before the title (may be null to clear)
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setHeader(final String title, final String subtitle, final Icon leadingIcon) {
    setHeader(title, subtitle);
    setLeadingIcon(leadingIcon);
    return this;
  }

  /**
   * Replaces the leading icon shown to the left of the header text.
   *
   * @param icon the icon, or null to clear
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setLeadingIcon(final Icon icon) {
    leadingIconLabel.setIcon(icon);
    leadingIconLabel.setVisible(icon != null);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Replaces the leading actions row of the header. Leading actions render in the WEST slot, after
   * the leading icon (if any) and before the title — useful for an info or context button you want
   * sitting next to the title rather than lost on the far right of a wide card.
   *
   * @param actions zero or more action components rendered in order from left to right
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setLeadingActions(final Component... actions) {
    leadingActionsPanel.removeAll();
    if (actions != null) {
      for (Component c : actions) {
        if (c != null) {
          leadingActionsPanel.add(c);
        }
      }
    }
    boolean has = leadingActionsPanel.getComponentCount() > 0;
    leadingActionsPanel.setVisible(has);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns the title label so callers can customize fonts, foreground, alignment, or FlatLaf style
   * class (e.g., {@code label.putClientProperty("FlatLaf.styleClass", "h2")} for a bigger title).
   *
   * @return the live title label; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  public JLabel getTitleLabel() {
    return titleLabel;
  }

  /**
   * Returns the subtitle label so callers can customize fonts, foreground, or FlatLaf style class.
   *
   * @return the live subtitle label; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  public JLabel getSubtitleLabel() {
    return subtitleLabel;
  }

  /**
   * Replaces the trailing actions row of the header.
   *
   * @param actions zero or more action components rendered in order from left to right
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setTrailingActions(final Component... actions) {
    trailingActionsPanel.removeAll();
    if (actions != null) {
      for (Component c : actions) {
        if (c != null) {
          trailingActionsPanel.add(c);
        }
      }
    }
    boolean has = trailingActionsPanel.getComponentCount() > 0;
    trailingActionsPanel.setVisible(has);
    refreshHeaderVisibility();
    return this;
  }

  // --- media slot ------

  /**
   * Sets the optional media slot, rendered between the header and the body.
   *
   * @param media the component to display (typically a thumbnail or hero image); null clears
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setMedia(final JComponent media) {
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

  // --- body slot -------

  /**
   * Sets the body content. The body fills any remaining vertical space.
   *
   * @param body the body component; null clears it
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setBody(final JComponent body) {
    bodyHolder.removeAll();
    this.body = body;
    if (body != null) {
      bodyHolder.add(body, BorderLayout.CENTER);
    }
    bodyHolder.setVisible(body != null);
    revalidate();
    repaint();
    return this;
  }

  // --- footer slot -----

  /**
   * Sets the footer content (single component variant).
   *
   * @param footer the footer component; null clears it
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setFooter(final JComponent footer) {
    footerHolder.removeAll();
    this.footer = footer;
    if (footer != null) {
      footerHolder.add(footer, BorderLayout.CENTER);
    }
    footerHolder.setVisible(footer != null);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Convenience: sets the footer to a right-aligned actions row.
   *
   * @param actions zero or more components rendered as a flow at the bottom of the card
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setFooter(final Component... actions) {
    if (actions == null || actions.length == 0) {
      return setFooter((JComponent) null);
    }
    JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, DEFAULT_INNER_GAP, 0));
    row.setOpaque(false);
    for (Component c : actions) {
      if (c != null) {
        row.add(c);
      }
    }
    return setFooter(row);
  }

  // --- collapsible -----

  /**
   * Enables or disables the header chevron and click-to-collapse behavior.
   *
   * @param collapsible whether the card can be collapsed
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setCollapsible(final boolean collapsible) {
    if (collapsible == this.collapsible) {
      return this;
    }
    this.collapsible = collapsible;
    chevronLabel.setVisible(collapsible);
    if (!collapsible && collapsed) {
      setCollapsed(false);
    }
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns whether the card supports collapse/expand.
   *
   * @return true if the chevron and toggle behavior are enabled
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isCollapsible() {
    return collapsible;
  }

  /**
   * Programmatically sets the collapsed state. Fires a {@code "collapsed"} property change.
   *
   * @param collapsed the new state
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setCollapsed(final boolean collapsed) {
    if (collapsed == this.collapsed) {
      return this;
    }
    final boolean old = collapsed;
    this.collapsed = collapsed;
    chevronLabel.setText(chevronGlyph(collapsed));
    collapsedSummaryHolder.setVisible(shouldShowCollapsedSummary());
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
   * Returns the current collapsed state.
   *
   * @return current collapsed state
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Optional summary slot shown only when the card is collapsed.
   *
   * @param summary the compact summary component, or null
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setCollapsedSummary(final JComponent summary) {
    collapsedSummaryHolder.removeAll();
    collapsedSummary = summary;
    if (summary != null) {
      collapsedSummaryHolder.add(summary, BorderLayout.CENTER);
    }
    collapsedSummaryHolder.setVisible(shouldShowCollapsedSummary());
    revalidate();
    repaint();
    return this;
  }

  /**
   * Opts the card into showing the collapsed-summary slot in <em>both</em> states (collapsed
   * <em>and</em> expanded), instead of only when collapsed.
   *
   * <p>Useful when the summary carries affordances (click targets, hover highlights, glyphs
   * encoding metadata) that should remain reachable while the user studies the expanded body — so
   * the user doesn't have to collapse the card to interact with the summary again.
   *
   * @param keep true to keep the collapsed-summary visible while expanded; false (default) to
   *     restore the standard "summary visible only when collapsed" behavior
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setKeepSummaryWhenExpanded(final boolean keep) {
    if (keep == keepSummaryWhenExpanded) {
      return this;
    }
    keepSummaryWhenExpanded = keep;
    collapsedSummaryHolder.setVisible(shouldShowCollapsedSummary());
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns whether the card keeps the collapsed-summary slot visible while expanded.
   *
   * @return true if the summary is shown in both collapsed and expanded states
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isKeepSummaryWhenExpanded() {
    return keepSummaryWhenExpanded;
  }

  private boolean shouldShowCollapsedSummary() {
    if (collapsedSummary == null) {
      return false;
    }
    return collapsed || keepSummaryWhenExpanded;
  }

  /**
   * Toggles smooth height interpolation when collapsing or expanding.
   *
   * @param animate whether to animate
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setAnimateCollapse(final boolean animate) {
    animateCollapse = animate;
    return this;
  }

  // --- selection -------

  /**
   * Sets the selected state (only meaningful for {@link CardInteractionMode#SELECTABLE} cards).
   * Fires a {@code "selected"} property change.
   *
   * @param selected the new selection state
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setSelected(final boolean selected) {
    if (selected == this.selected) {
      return this;
    }
    boolean old = selected;
    this.selected = selected;
    repaint();
    firePropertyChange(PROPERTY_SELECTED, old, selected);
    return this;
  }

  /**
   * Returns the current selection state.
   *
   * @return current selection state
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isSelected() {
    return selected;
  }

  // --- listeners -------

  /**
   * Cancels any in-flight click that's been seen by mousePressed but not yet completed by
   * mouseReleased.
   *
   * <p>Specifically clears the deferred header-collapse toggle and the {@code pressed} state, so
   * the in-progress press-release sequence won't fire either of them. Hosting components that
   * convert presses into drags (e.g., a list with reorder enabled) should call this once they
   * decide a drag has started, to suppress an otherwise-spurious header toggle on every drag.
   *
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard cancelPendingClick() {
    pressed = false;
    pendingHeaderToggle = false;
    repaint();
    return this;
  }

  /**
   * Registers an action listener that fires on click (clickable) or toggle (selectable).
   *
   * @param listener the listener to add; null is ignored
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
   * Convenience overload that scopes a {@link PropertyChangeListener} to either {@link
   * #PROPERTY_COLLAPSED} or {@link #PROPERTY_SELECTED}. Equivalent to {@link
   * #addPropertyChangeListener(String, PropertyChangeListener)}.
   *
   * @param propertyName one of {@link #PROPERTY_COLLAPSED} or {@link #PROPERTY_SELECTED}
   * @param listener the listener to add
   * @version v0.1.0
   * @since v0.1.0
   */
  public void onChange(final String propertyName, final PropertyChangeListener listener) {
    addPropertyChangeListener(propertyName, listener);
  }

  // ------------------------------------------------------------- internals

  private void refreshHeaderVisibility() {
    boolean hasTitle = titleLabel.getText() != null && !titleLabel.getText().isEmpty();
    boolean hasSub = subtitleLabel.isVisible();
    boolean hasIcon = leadingIconLabel.isVisible();
    boolean hasLeadingActions = leadingActionsPanel.isVisible();
    boolean hasActions = trailingActionsPanel.isVisible();
    boolean hasChevron = collapsible;
    headerRow.setVisible(
        hasTitle || hasSub || hasIcon || hasLeadingActions || hasActions || hasChevron);
    headerRow.revalidate();
  }

  private void rebuildBorder() {
    Insets shadow = shadowInsets();
    setBorder(
        BorderFactory.createEmptyBorder(
            padding.top + shadow.top,
            padding.left + shadow.left,
            padding.bottom + shadow.bottom,
            padding.right + shadow.right));
  }

  private Insets shadowInsets() {
    if (variant != CardVariant.ELEVATED || elevation <= 0) {
      return new Insets(0, 0, 0, 0);
    }
    int e = elevation;
    return new Insets(e, e, e * 2, e);
  }

  private static String chevronGlyph(final boolean collapsed) {
    return collapsed ? "▸" : "▾";
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
              float t =
                  Math.min(1f, (System.currentTimeMillis() - startTime) / (float) ANIMATION_MS);
              float ease = (float) (1 - Math.pow(1 - t, 3));
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

  // ------------------------------------------------------------- painting

  @Override
  protected void paintComponent(final Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Insets shadow = shadowInsets();
      int x = shadow.left;
      int y = shadow.top;
      int w = getWidth() - shadow.left - shadow.right;
      int h = getHeight() - shadow.top - shadow.bottom;
      int arc = getEffectiveCornerRadius();

      paintShadow(g2, x, y, w, h, arc);
      paintBackground(g2, x, y, w, h, arc);
      paintBorderShape(g2, x, y, w, h, arc);
      paintFocusRing(g2, x, y, w, h, arc);
    } finally {
      g2.dispose();
    }
  }

  private void paintShadow(final Graphics2D g2, int x, int y, int w, int h, int arc) {
    if (variant != CardVariant.ELEVATED || elevation <= 0) {
      return;
    }
    int e = elevation + (hovered && isEnabled() ? 1 : 0);
    int layers = Math.max(2, e + 1);
    for (int i = layers; i >= 1; i--) {
      int alpha = Math.max(8, 60 / i);
      g2.setColor(new Color(0, 0, 0, alpha));
      int spread = i;
      int offsetY = Math.max(1, e + i / 2);
      g2.fillRoundRect(
          x - spread, y + offsetY, w + 2 * spread, h + spread, arc + spread, arc + spread);
    }
  }

  private void paintBackground(final Graphics2D g2, int x, int y, int w, int h, int arc) {
    Color bg = surfaceColor();
    if (pressed) {
      bg = blend(bg, UIManager.getColor("Component.focusedBorderColor"), 0.10f);
    } else if (hovered && isEnabled() && interactionMode != CardInteractionMode.STATIC) {
      bg = blend(bg, foregroundForBlend(), 0.06f);
    }
    if (selected) {
      bg = blend(bg, accentColor(), 0.12f);
    }
    g2.setColor(bg);
    g2.fillRoundRect(x, y, w, h, arc, arc);
  }

  private void paintBorderShape(final Graphics2D g2, int x, int y, int w, int h, int arc) {
    Color border = effectiveBorderColor();
    if (border == null) {
      return;
    }
    int bw = borderWidth;
    if (variant != CardVariant.OUTLINED && !selected) {
      return;
    }
    if (selected) {
      bw = Math.max(bw, 2);
      border = accentColor();
    }
    g2.setColor(border);
    for (int i = 0; i < bw; i++) {
      g2.drawRoundRect(x + i, y + i, w - 1 - 2 * i, h - 1 - 2 * i, arc, arc);
    }
  }

  private void paintFocusRing(final Graphics2D g2, int x, int y, int w, int h, int arc) {
    if (!isFocusOwner() || interactionMode == CardInteractionMode.STATIC || !isEnabled()) {
      return;
    }
    Color ring = UIManager.getColor("Component.focusColor");
    if (ring == null) {
      ring = accentColor();
    }
    g2.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 160));
    g2.drawRoundRect(x - 1, y - 1, w + 1, h + 1, arc + 2, arc + 2);
  }

  /**
   * Overrides the variant-derived surface color with a caller-supplied tint. Pass {@code null} to
   * restore variant-default behavior. Hover, pressed, and selected blends are still applied on top
   * of the override.
   *
   * @param color the surface fill color, or null to clear
   * @return this card
   * @version v0.1.0
   * @since v0.1.0
   */
  public FlatCard setSurfaceColor(final Color color) {
    surfaceColorOverride = color;
    repaint();
    return this;
  }

  private Color surfaceColor() {
    if (surfaceColorOverride != null) {
      return surfaceColorOverride;
    }
    Color panel = UIManager.getColor("Panel.background");
    if (panel == null) {
      panel = Color.WHITE;
    }
    final boolean light = isLight(panel);
    Color c;
    switch (variant) {
      case ELEVATED ->
          c = light ? blend(panel, Color.WHITE, 1.0f) : blend(panel, Color.WHITE, 0.10f);
      case OUTLINED ->
          c = light ? blend(panel, Color.WHITE, 1.0f) : blend(panel, Color.WHITE, 0.06f);
      case FILLED -> c = blend(panel, foregroundForBlend(), 0.07f);
      default -> c = panel;
    }
    return c == null ? Color.WHITE : c;
  }

  private static boolean isLight(final Color c) {
    if (c == null) {
      return true;
    }
    return (c.getRed() + c.getGreen() + c.getBlue()) / 3 > 128;
  }

  private Color effectiveBorderColor() {
    if (borderColor != null) {
      return borderColor;
    }
    Color c = UIManager.getColor("Component.borderColor");
    if (c == null) {
      c = UIManager.getColor("Separator.foreground");
    }
    return c;
  }

  private Color foregroundForBlend() {
    Color c = UIManager.getColor("Label.foreground");
    return c == null ? Color.DARK_GRAY : c;
  }

  private Color accentColor() {
    Color c = UIManager.getColor("Component.accentColor");
    if (c == null) {
      c = UIManager.getColor("Component.focusColor");
    }
    return c == null ? new Color(72, 130, 180) : c;
  }

  private Color pickFirstNonNull(final String... keys) {
    for (String k : keys) {
      Color c = UIManager.getColor(k);
      if (c != null) {
        return c;
      }
    }
    return null;
  }

  private static Color blend(final Color a, final Color b, final float t) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    float clamp = Math.max(0f, Math.min(1f, t));
    int r = (int) (a.getRed() * (1 - clamp) + b.getRed() * clamp);
    int g = (int) (a.getGreen() * (1 - clamp) + b.getGreen() * clamp);
    int bl = (int) (a.getBlue() * (1 - clamp) + b.getBlue() * clamp);
    return new Color(r, g, bl);
  }

  // ------------------------------------------------------------- LAF hooks

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    if (subtitleLabel != null) {
      subtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    }
    if (padding != null) {
      rebuildBorder();
    }
    repaint();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(
        enabled && interactionMode != CardInteractionMode.STATIC
            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
    repaint();
  }

  // ------------------------------------------------------------- container

  /**
   * Wrapper panel whose preferred height is multiplied by {@link #animationFraction}, used to
   * animate the collapse/expand transition without breaking layout managers further out. Width
   * stretches to fill the outer BoxLayout.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private final class CollapsibleContainer extends JPanel {
    @Override
    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      if (animationFraction >= 1f) {
        return d;
      }
      return new Dimension(d.width, (int) Math.max(0, d.height * animationFraction));
    }

    @Override
    public Dimension getMinimumSize() {
      Dimension p = getPreferredSize();
      return new Dimension(0, p.height);
    }

    @Override
    public Dimension getMaximumSize() {
      Dimension d = super.getPreferredSize();
      int h = animationFraction >= 1f ? d.height : (int) Math.max(0, d.height * animationFraction);
      return new Dimension(Integer.MAX_VALUE, h);
    }
  }
}
