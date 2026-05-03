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
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
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
  private CardVariant myVariant = CardVariant.ELEVATED;
  private CardInteractionMode myInteractionMode = CardInteractionMode.STATIC;
  private int myElevation = 1;
  private Integer myCornerRadius;
  private Insets myPadding =
      new Insets(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);
  private Color myBorderColor;
  private Color mySurfaceColorOverride;
  private int myBorderWidth = 1;
  private boolean myAnimateCollapse;

  // State ------------------------------------------------------------------
  private boolean myHovered;
  private boolean myPressed;
  private boolean mySelected;
  private boolean myCollapsible;
  private boolean myCollapsed;
  private boolean myPendingHeaderToggle;

  // Header sub-components --------------------------------------------------
  private JPanel myHeaderRow;
  private JLabel myLeadingIconLabel;
  private JPanel myTextStack;
  private JLabel myTitleLabel;
  private JLabel mySubtitleLabel;
  private JPanel myLeadingActionsPanel;
  private JPanel myTrailingActionsPanel;
  private JLabel myChevronLabel;

  // Slot containers --------------------------------------------------------
  private CollapsibleContainer myCollapsibleBody;
  private JPanel myMediaHolder;
  private JPanel myBodyHolder;
  private JPanel myFooterHolder;
  private JPanel myCollapsedSummaryHolder;

  // User-supplied content --------------------------------------------------
  private JComponent myMedia;
  private JComponent myBody;
  private JComponent myFooter;
  private JComponent myCollapsedSummary;

  // Listeners --------------------------------------------------------------
  private final List<ActionListener> myActionListeners = new ArrayList<>();

  // Animation --------------------------------------------------------------
  private Timer myAnimationTimer;
  private float myAnimationFraction = 1f;

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
    myHeaderRow = newStretchingRow(new BorderLayout(DEFAULT_INNER_GAP, 0), 0);
    myHeaderRow.setVisible(false);

    myLeadingIconLabel = new JLabel();
    myLeadingIconLabel.setVisible(false);
    final JPanel westGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, DEFAULT_INNER_GAP / 2, 0));
    westGroup.setOpaque(false);
    westGroup.add(myLeadingIconLabel);
    myLeadingActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, DEFAULT_INNER_GAP / 2, 0));
    myLeadingActionsPanel.setOpaque(false);
    myLeadingActionsPanel.setVisible(false);
    westGroup.add(myLeadingActionsPanel);
    myHeaderRow.add(westGroup, BorderLayout.WEST);

    myTextStack = new JPanel();
    myTextStack.setOpaque(false);
    myTextStack.setLayout(new BoxLayout(myTextStack, BoxLayout.Y_AXIS));
    myTitleLabel = new JLabel();
    myTitleLabel.putClientProperty("FlatLaf.styleClass", "h4");
    myTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mySubtitleLabel = new JLabel();
    mySubtitleLabel.putClientProperty("FlatLaf.styleClass", "small");
    mySubtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mySubtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    mySubtitleLabel.setVisible(false);
    myTextStack.add(myTitleLabel);
    myTextStack.add(mySubtitleLabel);
    myHeaderRow.add(myTextStack, BorderLayout.CENTER);

    JPanel eastGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, DEFAULT_INNER_GAP / 2, 0));
    eastGroup.setOpaque(false);
    myTrailingActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, DEFAULT_INNER_GAP / 2, 0));
    myTrailingActionsPanel.setOpaque(false);
    myTrailingActionsPanel.setVisible(false);
    eastGroup.add(myTrailingActionsPanel);
    myChevronLabel = new JLabel(chevronGlyph(myCollapsed));
    myChevronLabel.setHorizontalAlignment(SwingConstants.CENTER);
    myChevronLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    myChevronLabel.setVisible(false);
    // Chevron is a button-like affordance and should always show the click cursor, even when
    // the host card has a different cursor (e.g., MOVE_CURSOR set by FlatCardList for drag).
    myChevronLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    eastGroup.add(myChevronLabel);
    myHeaderRow.add(eastGroup, BorderLayout.EAST);

    myMediaHolder = newSlotHolder();
    myBodyHolder = newSlotHolder();
    myFooterHolder = newSlotHolder();
    myCollapsedSummaryHolder = newSlotHolder();

    myCollapsibleBody = new CollapsibleContainer();
    myCollapsibleBody.setOpaque(false);
    myCollapsibleBody.setLayout(new BoxLayout(myCollapsibleBody, BoxLayout.Y_AXIS));
    myCollapsibleBody.setAlignmentX(0f);
    myCollapsibleBody.add(myMediaHolder);
    myCollapsibleBody.add(myBodyHolder);
    myCollapsibleBody.add(myFooterHolder);

    add(myHeaderRow);
    add(myCollapsedSummaryHolder);
    add(myCollapsibleBody);
    myCollapsedSummaryHolder.setVisible(false);
  }

  /**
   * Creates a row panel that stretches horizontally to fill its parent BoxLayout but locks
   * vertically to its preferred height.
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
            if (myInteractionMode != CardInteractionMode.STATIC && isEnabled()) {
              myHovered = true;
              repaint();
            }
          }

          @Override
          public void mouseExited(MouseEvent e) {
            myHovered = false;
            myPressed = false;
            repaint();
          }

          @Override
          public void mousePressed(MouseEvent e) {
            if (!isEnabled()) {
              return;
            }
            if (myCollapsible && isInHeader(e)) {
              // Defer the actual collapse toggle to mouseReleased — that way a hosting list can
              // call cancelPendingClick() if the press turns into a drag, suppressing what
              // would otherwise be an unwanted toggle on every drag start.
              myPendingHeaderToggle = true;
            }
            if (myInteractionMode == CardInteractionMode.CLICKABLE
                || myInteractionMode == CardInteractionMode.SELECTABLE
                || myPendingHeaderToggle) {
              myPressed = true;
              requestFocusInWindow();
              repaint();
            }
          }

          @Override
          public void mouseReleased(MouseEvent e) {
            if (!myPressed || !isEnabled()) {
              myPressed = false;
              myPendingHeaderToggle = false;
              repaint();
              return;
            }
            myPressed = false;
            if (contains(e.getPoint())) {
              if (myPendingHeaderToggle) {
                myPendingHeaderToggle = false;
                setCollapsed(!myCollapsed);
              } else {
                activate(e.getModifiersEx());
              }
            }
            myPendingHeaderToggle = false;
            repaint();
          }
        };
    addMouseListener(ma);
    // Intentionally NOT installing this listener on myHeaderRow: when a child has its own
    // mouse listener, AWT delivers the event there and stops, which prevents outer listeners
    // (e.g., a host FlatCardList that wants to handle selection on header clicks) from ever
    // seeing the event. The card-level listener detects header clicks via Y-bounds in
    // isInHeader(), so the toggle still fires correctly.
    myChevronLabel.addMouseListener(ma);

    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            repaint();
          }

          @Override
          public void focusLost(FocusEvent e) {
            myPressed = false;
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
            if (myCollapsible) {
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
    if (src == myHeaderRow || src == myChevronLabel) {
      return true;
    }
    if (src == this) {
      return e.getY() < myHeaderRow.getY() + myHeaderRow.getHeight()
          && e.getY() >= myHeaderRow.getY();
    }
    return false;
  }

  private void activate(int modifiers) {
    if (myInteractionMode == CardInteractionMode.SELECTABLE) {
      setSelected(!mySelected);
    }
    if (myInteractionMode == CardInteractionMode.CLICKABLE
        || myInteractionMode == CardInteractionMode.SELECTABLE) {
      ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
      for (ActionListener l : new ArrayList<>(myActionListeners)) {
        l.actionPerformed(evt);
      }
    }
  }

  private void toggleCollapsed(MouseEvent ignored) {
    setCollapsed(!myCollapsed);
  }

  // ----------------------------------------------------------- public API

  /**
   * Sets the surface variant.
   *
   * @param theVariant one of {@link CardVariant}; ignored if null
   * @return this card for fluent chaining
   */
  public FlatCard setVariant(final CardVariant theVariant) {
    if (theVariant == null || theVariant == myVariant) {
      return this;
    }
    myVariant = theVariant;
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns the active surface variant.
   *
   * @return the active variant (never null)
   */
  public CardVariant getVariant() {
    return myVariant;
  }

  /**
   * Sets the interaction mode.
   *
   * @param theMode one of {@link CardInteractionMode}; ignored if null
   * @return this card
   */
  public FlatCard setInteractionMode(final CardInteractionMode theMode) {
    if (theMode == null || theMode == myInteractionMode) {
      return this;
    }
    myInteractionMode = theMode;
    setFocusable(
        theMode == CardInteractionMode.CLICKABLE || theMode == CardInteractionMode.SELECTABLE);
    setCursor(
        theMode == CardInteractionMode.STATIC
            ? Cursor.getDefaultCursor()
            : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    repaint();
    return this;
  }

  /**
   * Returns the active interaction mode.
   *
   * @return the active interaction mode (never null)
   */
  public CardInteractionMode getInteractionMode() {
    return myInteractionMode;
  }

  /**
   * Sets the elevation level (clamped to {@code 0..MAX_ELEVATION}). Only effective on the {@link
   * CardVariant#ELEVATED} variant.
   *
   * @param theElevation desired elevation (0 disables shadow)
   * @return this card
   */
  public FlatCard setElevation(final int theElevation) {
    int v = Math.max(0, Math.min(MAX_ELEVATION, theElevation));
    if (v == myElevation) {
      return this;
    }
    myElevation = v;
    rebuildBorder();
    repaint();
    return this;
  }

  /**
   * Returns the current elevation level.
   *
   * @return current elevation level (0..MAX_ELEVATION)
   */
  public int getElevation() {
    return myElevation;
  }

  /**
   * Overrides the corner radius. Pass {@code null} to fall back to the FlatLaf {@code
   * Component.arc} key.
   *
   * @param theRadius the new arc, or null for theme default
   * @return this card
   */
  public FlatCard setCornerRadius(final Integer theRadius) {
    myCornerRadius = theRadius;
    repaint();
    return this;
  }

  /**
   * Returns the corner radius actually used by paint, resolving the override or theme default.
   *
   * @return effective corner radius, resolved against UIManager when not overridden
   */
  public int getEffectiveCornerRadius() {
    if (myCornerRadius != null) {
      return myCornerRadius;
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
   * @param theInsets new padding; null is treated as zero on all sides
   * @return this card
   */
  public FlatCard setPadding(final Insets theInsets) {
    myPadding = theInsets == null ? new Insets(0, 0, 0, 0) : (Insets) theInsets.clone();
    rebuildBorder();
    revalidate();
    repaint();
    return this;
  }

  /**
   * Convenience for {@link #setPadding(Insets)} with uniform spacing.
   *
   * @param thePadding spacing applied to all four sides
   * @return this card
   */
  public FlatCard setPadding(final int thePadding) {
    return setPadding(new Insets(thePadding, thePadding, thePadding, thePadding));
  }

  /**
   * Returns the configured content padding.
   *
   * @return current content padding (defensive copy)
   */
  public Insets getPadding() {
    return (Insets) myPadding.clone();
  }

  /**
   * Overrides the border color. Pass {@code null} to derive from the theme.
   *
   * @param theColor explicit border color, or null
   * @return this card
   */
  public FlatCard setBorderColor(final Color theColor) {
    myBorderColor = theColor;
    repaint();
    return this;
  }

  /**
   * Sets border width in pixels. Only visible on the {@link CardVariant#OUTLINED} variant.
   *
   * @param theWidth border thickness, clamped to {@code >= 0}
   * @return this card
   */
  public FlatCard setBorderWidth(final int theWidth) {
    myBorderWidth = Math.max(0, theWidth);
    repaint();
    return this;
  }

  // --- header slot -----

  /**
   * Sets the header title. Pass null or empty to hide the title row entirely.
   *
   * @param theTitle the header title text
   * @return this card
   */
  public FlatCard setHeader(final String theTitle) {
    return setHeader(theTitle, null, null);
  }

  /**
   * Sets the header title and subtitle.
   *
   * @param theTitle the title text (null/empty hides the header row)
   * @param theSubtitle the subtitle text (null/empty hides the subtitle line)
   * @return this card
   */
  public FlatCard setHeader(final String theTitle, final String theSubtitle) {
    return setHeader(theTitle, theSubtitle, null);
  }

  /**
   * Sets the header title, subtitle, and leading icon.
   *
   * @param theTitle the title text
   * @param theSubtitle the subtitle text (may be null)
   * @param theLeadingIcon the icon shown before the title (may be null)
   * @return this card
   */
  public FlatCard setHeader(
      final String theTitle, final String theSubtitle, final Icon theLeadingIcon) {
    boolean hasTitle = theTitle != null && !theTitle.isEmpty();
    myTitleLabel.setText(hasTitle ? theTitle : "");
    boolean hasSub = theSubtitle != null && !theSubtitle.isEmpty();
    mySubtitleLabel.setText(hasSub ? theSubtitle : "");
    mySubtitleLabel.setVisible(hasSub);
    setLeadingIcon(theLeadingIcon);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Replaces the leading icon shown to the left of the header text.
   *
   * @param theIcon the icon, or null to clear
   * @return this card
   */
  public FlatCard setLeadingIcon(final Icon theIcon) {
    myLeadingIconLabel.setIcon(theIcon);
    myLeadingIconLabel.setVisible(theIcon != null);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Replaces the leading actions row of the header. Leading actions render in the WEST slot, after
   * the leading icon (if any) and before the title — useful for an info or context button you want
   * sitting next to the title rather than lost on the far right of a wide card.
   *
   * @param theActions zero or more action components rendered in order from left to right
   * @return this card
   */
  public FlatCard setLeadingActions(final Component... theActions) {
    myLeadingActionsPanel.removeAll();
    if (theActions != null) {
      for (Component c : theActions) {
        if (c != null) {
          myLeadingActionsPanel.add(c);
        }
      }
    }
    boolean has = myLeadingActionsPanel.getComponentCount() > 0;
    myLeadingActionsPanel.setVisible(has);
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns the title label so callers can customize fonts, foreground, alignment, or FlatLaf style
   * class (e.g., {@code label.putClientProperty("FlatLaf.styleClass", "h2")} for a bigger title).
   *
   * @return the live title label; never null
   */
  public JLabel getTitleLabel() {
    return myTitleLabel;
  }

  /**
   * Returns the subtitle label so callers can customize fonts, foreground, or FlatLaf style class.
   *
   * @return the live subtitle label; never null
   */
  public JLabel getSubtitleLabel() {
    return mySubtitleLabel;
  }

  /**
   * Replaces the trailing actions row of the header.
   *
   * @param theActions zero or more action components rendered in order from left to right
   * @return this card
   */
  public FlatCard setTrailingActions(final Component... theActions) {
    myTrailingActionsPanel.removeAll();
    if (theActions != null) {
      for (Component c : theActions) {
        if (c != null) {
          myTrailingActionsPanel.add(c);
        }
      }
    }
    boolean has = myTrailingActionsPanel.getComponentCount() > 0;
    myTrailingActionsPanel.setVisible(has);
    refreshHeaderVisibility();
    return this;
  }

  // --- media slot ------

  /**
   * Sets the optional media slot, rendered between the header and the body.
   *
   * @param theMedia the component to display (typically a thumbnail or hero image); null clears
   * @return this card
   */
  public FlatCard setMedia(final JComponent theMedia) {
    myMediaHolder.removeAll();
    myMedia = theMedia;
    if (theMedia != null) {
      myMediaHolder.add(theMedia, BorderLayout.CENTER);
    }
    myMediaHolder.setVisible(theMedia != null);
    revalidate();
    repaint();
    return this;
  }

  // --- body slot -------

  /**
   * Sets the body content. The body fills any remaining vertical space.
   *
   * @param theBody the body component; null clears it
   * @return this card
   */
  public FlatCard setBody(final JComponent theBody) {
    myBodyHolder.removeAll();
    myBody = theBody;
    if (theBody != null) {
      myBodyHolder.add(theBody, BorderLayout.CENTER);
    }
    myBodyHolder.setVisible(theBody != null);
    revalidate();
    repaint();
    return this;
  }

  // --- footer slot -----

  /**
   * Sets the footer content (single component variant).
   *
   * @param theFooter the footer component; null clears it
   * @return this card
   */
  public FlatCard setFooter(final JComponent theFooter) {
    myFooterHolder.removeAll();
    myFooter = theFooter;
    if (theFooter != null) {
      myFooterHolder.add(theFooter, BorderLayout.CENTER);
    }
    myFooterHolder.setVisible(theFooter != null);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Convenience: sets the footer to a right-aligned actions row.
   *
   * @param theActions zero or more components rendered as a flow at the bottom of the card
   * @return this card
   */
  public FlatCard setFooter(final Component... theActions) {
    if (theActions == null || theActions.length == 0) {
      return setFooter((JComponent) null);
    }
    JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, DEFAULT_INNER_GAP, 0));
    row.setOpaque(false);
    for (Component c : theActions) {
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
   * @param theCollapsible whether the card can be collapsed
   * @return this card
   */
  public FlatCard setCollapsible(final boolean theCollapsible) {
    if (theCollapsible == myCollapsible) {
      return this;
    }
    myCollapsible = theCollapsible;
    myChevronLabel.setVisible(theCollapsible);
    if (!theCollapsible && myCollapsed) {
      setCollapsed(false);
    }
    refreshHeaderVisibility();
    return this;
  }

  /**
   * Returns whether the card supports collapse/expand.
   *
   * @return true if the chevron and toggle behavior are enabled
   */
  public boolean isCollapsible() {
    return myCollapsible;
  }

  /**
   * Programmatically sets the collapsed state. Fires a {@code "collapsed"} property change.
   *
   * @param theCollapsed the new state
   * @return this card
   */
  public FlatCard setCollapsed(final boolean theCollapsed) {
    if (theCollapsed == myCollapsed) {
      return this;
    }
    final boolean old = myCollapsed;
    myCollapsed = theCollapsed;
    myChevronLabel.setText(chevronGlyph(myCollapsed));
    myCollapsedSummaryHolder.setVisible(myCollapsed && myCollapsedSummary != null);
    if (myAnimateCollapse) {
      animateTo(myCollapsed ? 0f : 1f);
    } else {
      myAnimationFraction = myCollapsed ? 0f : 1f;
      myCollapsibleBody.setVisible(!myCollapsed);
    }
    revalidate();
    repaint();
    firePropertyChange(PROPERTY_COLLAPSED, old, myCollapsed);
    return this;
  }

  /**
   * Returns the current collapsed state.
   *
   * @return current collapsed state
   */
  public boolean isCollapsed() {
    return myCollapsed;
  }

  /**
   * Optional summary slot shown only when the card is collapsed.
   *
   * @param theSummary the compact summary component, or null
   * @return this card
   */
  public FlatCard setCollapsedSummary(final JComponent theSummary) {
    myCollapsedSummaryHolder.removeAll();
    myCollapsedSummary = theSummary;
    if (theSummary != null) {
      myCollapsedSummaryHolder.add(theSummary, BorderLayout.CENTER);
    }
    myCollapsedSummaryHolder.setVisible(myCollapsed && theSummary != null);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Toggles smooth height interpolation when collapsing or expanding.
   *
   * @param theAnimate whether to animate
   * @return this card
   */
  public FlatCard setAnimateCollapse(final boolean theAnimate) {
    myAnimateCollapse = theAnimate;
    return this;
  }

  // --- selection -------

  /**
   * Sets the selected state (only meaningful for {@link CardInteractionMode#SELECTABLE} cards).
   * Fires a {@code "selected"} property change.
   *
   * @param theSelected the new selection state
   * @return this card
   */
  public FlatCard setSelected(final boolean theSelected) {
    if (theSelected == mySelected) {
      return this;
    }
    boolean old = mySelected;
    mySelected = theSelected;
    repaint();
    firePropertyChange(PROPERTY_SELECTED, old, mySelected);
    return this;
  }

  /**
   * Returns the current selection state.
   *
   * @return current selection state
   */
  public boolean isSelected() {
    return mySelected;
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
   */
  public FlatCard cancelPendingClick() {
    myPressed = false;
    myPendingHeaderToggle = false;
    repaint();
    return this;
  }

  /**
   * Registers an action listener that fires on click (clickable) or toggle (selectable).
   *
   * @param theListener the listener to add; null is ignored
   */
  public void addActionListener(final ActionListener theListener) {
    if (theListener != null) {
      myActionListeners.add(theListener);
    }
  }

  /**
   * Removes a previously registered action listener.
   *
   * @param theListener the listener to remove
   */
  public void removeActionListener(final ActionListener theListener) {
    myActionListeners.remove(theListener);
  }

  /**
   * Convenience overload that scopes a {@link PropertyChangeListener} to either {@link
   * #PROPERTY_COLLAPSED} or {@link #PROPERTY_SELECTED}. Equivalent to {@link
   * #addPropertyChangeListener(String, PropertyChangeListener)}.
   *
   * @param thePropertyName one of {@link #PROPERTY_COLLAPSED} or {@link #PROPERTY_SELECTED}
   * @param theListener the listener to add
   */
  public void onChange(final String thePropertyName, final PropertyChangeListener theListener) {
    addPropertyChangeListener(thePropertyName, theListener);
  }

  // ------------------------------------------------------------- internals

  private void refreshHeaderVisibility() {
    boolean hasTitle = myTitleLabel.getText() != null && !myTitleLabel.getText().isEmpty();
    boolean hasSub = mySubtitleLabel.isVisible();
    boolean hasIcon = myLeadingIconLabel.isVisible();
    boolean hasLeadingActions = myLeadingActionsPanel.isVisible();
    boolean hasActions = myTrailingActionsPanel.isVisible();
    boolean hasChevron = myCollapsible;
    myHeaderRow.setVisible(
        hasTitle || hasSub || hasIcon || hasLeadingActions || hasActions || hasChevron);
    myHeaderRow.revalidate();
  }

  private void rebuildBorder() {
    Insets shadow = shadowInsets();
    setBorder(
        BorderFactory.createEmptyBorder(
            myPadding.top + shadow.top,
            myPadding.left + shadow.left,
            myPadding.bottom + shadow.bottom,
            myPadding.right + shadow.right));
  }

  private Insets shadowInsets() {
    if (myVariant != CardVariant.ELEVATED || myElevation <= 0) {
      return new Insets(0, 0, 0, 0);
    }
    int e = myElevation;
    return new Insets(e, e, e * 2, e);
  }

  private static String chevronGlyph(final boolean collapsed) {
    return collapsed ? "▸" : "▾";
  }

  private void animateTo(final float target) {
    if (myAnimationTimer != null && myAnimationTimer.isRunning()) {
      myAnimationTimer.stop();
    }
    final float start = myAnimationFraction;
    final long startTime = System.currentTimeMillis();
    if (target > 0) {
      myCollapsibleBody.setVisible(true);
    }
    myAnimationTimer =
        new Timer(
            ANIMATION_MS / ANIMATION_STEPS,
            e -> {
              float t =
                  Math.min(1f, (System.currentTimeMillis() - startTime) / (float) ANIMATION_MS);
              float ease = (float) (1 - Math.pow(1 - t, 3));
              myAnimationFraction = start + (target - start) * ease;
              myCollapsibleBody.revalidate();
              myCollapsibleBody.repaint();
              revalidate();
              repaint();
              if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                myAnimationFraction = target;
                if (target == 0f) {
                  myCollapsibleBody.setVisible(false);
                }
              }
            });
    myAnimationTimer.start();
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
    if (myVariant != CardVariant.ELEVATED || myElevation <= 0) {
      return;
    }
    int e = myElevation + (myHovered && isEnabled() ? 1 : 0);
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
    if (myPressed) {
      bg = blend(bg, UIManager.getColor("Component.focusedBorderColor"), 0.10f);
    } else if (myHovered && isEnabled() && myInteractionMode != CardInteractionMode.STATIC) {
      bg = blend(bg, foregroundForBlend(), 0.06f);
    }
    if (mySelected) {
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
    int bw = myBorderWidth;
    if (myVariant != CardVariant.OUTLINED && !mySelected) {
      return;
    }
    if (mySelected) {
      bw = Math.max(bw, 2);
      border = accentColor();
    }
    g2.setColor(border);
    for (int i = 0; i < bw; i++) {
      g2.drawRoundRect(x + i, y + i, w - 1 - 2 * i, h - 1 - 2 * i, arc, arc);
    }
  }

  private void paintFocusRing(final Graphics2D g2, int x, int y, int w, int h, int arc) {
    if (!isFocusOwner() || myInteractionMode == CardInteractionMode.STATIC || !isEnabled()) {
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
   * @param theColor the surface fill color, or null to clear
   * @return this card
   */
  public FlatCard setSurfaceColor(final Color theColor) {
    mySurfaceColorOverride = theColor;
    repaint();
    return this;
  }

  private Color surfaceColor() {
    if (mySurfaceColorOverride != null) {
      return mySurfaceColorOverride;
    }
    Color panel = UIManager.getColor("Panel.background");
    if (panel == null) {
      panel = Color.WHITE;
    }
    final boolean light = isLight(panel);
    Color c;
    switch (myVariant) {
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
    if (myBorderColor != null) {
      return myBorderColor;
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
    if (mySubtitleLabel != null) {
      mySubtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    }
    if (myPadding != null) {
      rebuildBorder();
    }
    repaint();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    setCursor(
        enabled && myInteractionMode != CardInteractionMode.STATIC
            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
    repaint();
  }

  // ------------------------------------------------------------- container

  /**
   * Wrapper panel whose preferred height is multiplied by {@link #myAnimationFraction}, used to
   * animate the collapse/expand transition without breaking layout managers further out. Width
   * stretches to fill the outer BoxLayout.
   */
  private final class CollapsibleContainer extends JPanel {
    @Override
    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      if (myAnimationFraction >= 1f) {
        return d;
      }
      return new Dimension(d.width, (int) Math.max(0, d.height * myAnimationFraction));
    }

    @Override
    public Dimension getMinimumSize() {
      Dimension p = getPreferredSize();
      return new Dimension(0, p.height);
    }

    @Override
    public Dimension getMaximumSize() {
      Dimension d = super.getPreferredSize();
      int h =
          myAnimationFraction >= 1f ? d.height : (int) Math.max(0, d.height * myAnimationFraction);
      return new Dimension(Integer.MAX_VALUE, h);
    }
  }
}
