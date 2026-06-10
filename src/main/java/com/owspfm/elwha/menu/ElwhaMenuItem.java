package com.owspfm.elwha.menu;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.RipplePainter;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * One row of an {@link ElwhaMenu} — the M3 Expressive <strong>menu item</strong> primitive. A
 * dedicated {@link JComponent} that owns its anatomy and state painting, mirroring {@link
 * com.owspfm.elwha.navrail.ElwhaNavRailDestination} rather than styling a {@code JMenuItem} or a
 * Button (design §3, research §Q′).
 *
 * <p><strong>Slot anatomy</strong> (research §J, terminology §P): {@code [leading icon?] · label (+
 * supporting text?) · [badge?] · [trailing text?] · [trailing icon?]}. The label region can be
 * swapped for arbitrary display content via {@link #setSlot(JComponent)} ("a container with a
 * swappable slot") — display-only; per the spec a slot must not host a second interactive control.
 *
 * <p><strong>Tokens</strong> (research §I, zero new): {@value #MIN_TARGET_PX} dp min interactive
 * target around a {@value #VISUAL_HEIGHT_PX} dp visual row, {@value #INSET_X_PX} dp
 * leading/trailing insets, {@value #INSET_Y_PX} dp top/bottom, {@value #BETWEEN_PX} dp between icon
 * and label, {@value #ICON_SIZE_PX} dp icons. Label {@link TypeRole#LABEL_LARGE}, supporting {@link
 * TypeRole#BODY_SMALL}, trailing {@link TypeRole#LABEL_LARGE}.
 *
 * <p><strong>States</strong> (research §L): Enabled / Disabled (38% dim, focusable-but-inert via
 * the container's roving focus) / Hovered ({@code ON_SURFACE} state layer) / Focused (3 dp inset
 * {@code SECONDARY} ring) / Pressed (ripple). Selected = {@code TERTIARY_CONTAINER} fill + a {@code
 * ON_TERTIARY_CONTAINER} ✓ checkmark (Standard color), a 3:1 + non-color cue per a11y (§X).
 *
 * <p><strong>Focus model.</strong> The item is <em>not</em> independently focusable — the parent
 * {@link ElwhaMenu} owns the single keyboard focus and pushes a roving "focused" flag down (so a
 * disabled item can still be focused but not activated, per §X). Selection, color style, and the
 * focused flag are all push-only from the container.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenuItem extends JComponent {

  /** Visual row height in dp (research §I). */
  static final int VISUAL_HEIGHT_PX = 44;

  /** Minimum interactive target height in dp (research §M/§Q). */
  static final int MIN_TARGET_PX = 48;

  /** Leading / trailing inset in dp. */
  static final int INSET_X_PX = 16;

  /** Top / bottom inset in dp. */
  static final int INSET_Y_PX = 8;

  /** Between-space (icon ↔ label, label ↔ trailing) in dp. */
  static final int BETWEEN_PX = 12;

  /** Leading / trailing icon size in dp. */
  static final int ICON_SIZE_PX = 20;

  /** Item / selected-fill corner radius in dp (research §I shape, SM end of the range). */
  static final int ARC_PX = 8;

  private static final int FOCUS_RING_STROKE_PX = 3;
  private static final int FOCUS_RING_INSET_PX = 3;
  private static final int RIPPLE_TOTAL_MS = 400;
  private static final int RIPPLE_TICK_MS = 16;

  private Icon leadingIcon;
  private String label;
  private String supportingText;
  private String trailingText;
  private Icon trailingIcon;
  private JComponent slot;
  private ElwhaBadge badge;

  private boolean selected;
  private boolean focused;
  private boolean reserveLeading;
  private boolean checkable;
  private ColorStyle colorStyle = ColorStyle.STANDARD;

  private boolean hovered;
  private boolean pressed;

  private Point rippleOrigin;
  private float rippleProgress = 1f;
  private Timer rippleTimer;

  private final Icon checkmark = MaterialIcons.check(ICON_SIZE_PX);
  private final List<ActionListener> actionListeners = new ArrayList<>();

  /**
   * Constructs an action item with a label and no leading icon.
   *
   * @param label the item label; required, non-null
   * @throws NullPointerException if {@code label} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaMenuItem(final String label) {
    this(null, label);
  }

  /**
   * Constructs an item with a leading icon and a label.
   *
   * @param leadingIcon the 20 dp leading icon, or {@code null} for none
   * @param label the item label; required, non-null
   * @throws NullPointerException if {@code label} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaMenuItem(final Icon leadingIcon, final String label) {
    this.label = Objects.requireNonNull(label, "label");
    this.leadingIcon = leadingIcon;
    applyIconColorFilter(leadingIcon);
    applyIconColorFilter(checkmark);
    setOpaque(false);
    setFocusable(false);
    initInteraction();
  }

  /**
   * Creates an action item with a label.
   *
   * @param label the item label; required
   * @return a new item
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaMenuItem of(final String label) {
    return new ElwhaMenuItem(label);
  }

  /**
   * Creates an item with a leading icon and a label.
   *
   * @param leadingIcon the 20 dp leading icon
   * @param label the item label; required
   * @return a new item
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaMenuItem of(final Icon leadingIcon, final String label) {
    return new ElwhaMenuItem(leadingIcon, label);
  }

  // ------------------------------------------------------------- slots

  /**
   * @return the leading icon, or {@code null}
   */
  public Icon getLeadingIcon() {
    return leadingIcon;
  }

  /**
   * Sets the leading icon (20 dp), or {@code null} to clear it.
   *
   * @param icon the leading icon, or {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setLeadingIcon(final Icon icon) {
    this.leadingIcon = icon;
    applyIconColorFilter(icon);
    revalidate();
    repaint();
  }

  /**
   * @return the item label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the item label.
   *
   * @param label the new label; required
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setLabel(final String label) {
    this.label = Objects.requireNonNull(label, "label");
    revalidate();
    repaint();
  }

  /**
   * @return the supporting (second-line) text, or {@code null}
   */
  public String getSupportingText() {
    return supportingText;
  }

  /**
   * Sets the optional supporting text shown on a second line under the label.
   *
   * @param text the supporting text, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSupportingText(final String text) {
    this.supportingText = text;
    revalidate();
    repaint();
  }

  /**
   * @return the trailing text (e.g. a keyboard shortcut), or {@code null}
   */
  public String getTrailingText() {
    return trailingText;
  }

  /**
   * Sets the optional trailing text — typically a keyboard shortcut such as {@code ⌘C} (M3
   * "trailing supporting text" / keyboard command).
   *
   * @param text the trailing text, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTrailingText(final String text) {
    this.trailingText = text;
    revalidate();
    repaint();
  }

  /**
   * @return the trailing icon, or {@code null}
   */
  public Icon getTrailingIcon() {
    return trailingIcon;
  }

  /**
   * Sets the optional trailing icon (20 dp).
   *
   * @param icon the trailing icon, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTrailingIcon(final Icon icon) {
    this.trailingIcon = icon;
    applyIconColorFilter(icon);
    revalidate();
    repaint();
  }

  /**
   * @return the swapped-in content slot, or {@code null}
   */
  public JComponent getSlot() {
    return slot;
  }

  /**
   * Swaps the label region for arbitrary display content (an image, a progress indicator, a color
   * swatch — research §Q). Display-only: per the M3 slot-accessibility rule a slot must not host a
   * second interactive control. Passing {@code null} restores the painted label.
   *
   * @param slot the slot content, or {@code null} to restore the label
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSlot(final JComponent slot) {
    if (this.slot != null) {
      remove(this.slot);
    }
    this.slot = slot;
    if (slot != null) {
      add(slot);
    }
    revalidate();
    repaint();
  }

  /**
   * @return the trailing badge, or {@code null}
   */
  public ElwhaBadge getBadge() {
    return badge;
  }

  /**
   * Sets an optional badge (e.g. a {@code New} pill) shown inline in the trailing cluster — the M3
   * menu-item badge sits in the row, not corner-anchored. Passing {@code null} clears it.
   *
   * @param badge the badge, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setBadge(final ElwhaBadge badge) {
    if (this.badge != null) {
      remove(this.badge);
    }
    this.badge = badge;
    if (badge != null) {
      add(badge);
    }
    revalidate();
    repaint();
  }

  // -------------------------------------------------------- pushed state

  /**
   * @return whether this item paints in its selected form
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Sets the selected state — a tinted fill + a ✓ checkmark ({@code TERTIARY_CONTAINER} / {@code
   * ON_TERTIARY_CONTAINER} under Standard, bold {@code TERTIARY} / {@code ON_TERTIARY} under
   * Vibrant). Push-only from the parent {@link ElwhaMenu} when a {@code SelectionMode} is active.
   *
   * @param selected the new selected state
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSelected(final boolean selected) {
    if (this.selected == selected) {
      return;
    }
    this.selected = selected;
    applyIconColorFilter(leadingIcon);
    applyIconColorFilter(trailingIcon);
    applyIconColorFilter(checkmark);
    revalidate();
    repaint();
  }

  // Pushed by the container when a SelectionMode is active: reserve the leading check-column on
  // every
  // item (even unselected ones) so toggling a MULTI item's selection never shifts the label.
  // Without
  // it, an item with no leading icon would widen by one icon + gap the first time it's selected.
  void setReserveLeadingColumn(final boolean reserve) {
    if (this.reserveLeading == reserve) {
      return;
    }
    this.reserveLeading = reserve;
    revalidate();
    repaint();
  }

  // Pushed by the container in MULTI mode so the item reports AccessibleState.CHECKED
  // (checkbox-like)
  // alongside SELECTED; SINGLE leaves this false (radio-like, SELECTED only).
  void setCheckable(final boolean checkable) {
    this.checkable = checkable;
  }

  // The container's roving focus drives this — a disabled item can still be the focused item.
  void setFocused(final boolean focused) {
    if (this.focused == focused) {
      return;
    }
    this.focused = focused;
    repaint();
  }

  boolean isFocused() {
    return focused;
  }

  // Pushed by the container; STANDARD (surface) or VIBRANT (tertiary-tinted), research §K.
  void setColorStyle(final ColorStyle style) {
    this.colorStyle = Objects.requireNonNull(style, "style");
    applyIconColorFilter(leadingIcon);
    applyIconColorFilter(trailingIcon);
    applyIconColorFilter(checkmark);
    repaint();
  }

  // --------------------------------------------------------- listeners

  /**
   * Adds an action listener fired on mouse click and on keyboard activation (Enter/Space, routed by
   * the parent menu).
   *
   * @param listener the listener; {@code null} is ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addActionListener(final ActionListener listener) {
    if (listener != null) {
      actionListeners.add(listener);
    }
  }

  /**
   * Removes a previously added action listener.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeActionListener(final ActionListener listener) {
    actionListeners.remove(listener);
  }

  // Fires the action (mouse click, or the container's Enter routing). Inert when disabled.
  void activate(final int modifiers) {
    if (!isEnabled()) {
      return;
    }
    final ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", modifiers);
    for (final ActionListener l : new ArrayList<>(actionListeners)) {
      l.actionPerformed(evt);
    }
  }

  // ------------------------------------------------------------ sizing

  private boolean hasLeadingColumn() {
    return leadingIcon != null || selected || reserveLeading;
  }

  @Override
  public Dimension getPreferredSize() {
    final FontMetrics labelFm = getFontMetrics(TypeRole.LABEL_LARGE.resolve());
    int contentWidth = 0;
    if (hasLeadingColumn()) {
      contentWidth += ICON_SIZE_PX + BETWEEN_PX;
    }
    contentWidth += labelOrSlotWidth(labelFm);
    contentWidth += trailingClusterWidth(labelFm);

    final int width = INSET_X_PX + contentWidth + INSET_X_PX;
    final int height = Math.max(MIN_TARGET_PX, INSET_Y_PX + contentHeight(labelFm) + INSET_Y_PX);
    return new Dimension(width, height);
  }

  private int labelOrSlotWidth(final FontMetrics labelFm) {
    if (slot != null) {
      return slot.getPreferredSize().width;
    }
    int w = labelFm.stringWidth(label);
    if (supportingText != null) {
      w = Math.max(w, getFontMetrics(TypeRole.BODY_SMALL.resolve()).stringWidth(supportingText));
    }
    return w;
  }

  private int trailingClusterWidth(final FontMetrics labelFm) {
    int w = 0;
    if (badge != null) {
      w += BETWEEN_PX + badge.getPreferredSize().width;
    }
    if (trailingText != null) {
      w += BETWEEN_PX + getFontMetrics(TypeRole.LABEL_LARGE.resolve()).stringWidth(trailingText);
    }
    if (trailingIcon != null) {
      w += BETWEEN_PX + ICON_SIZE_PX;
    }
    return w;
  }

  private int contentHeight(final FontMetrics labelFm) {
    if (slot != null) {
      return Math.max(labelFm.getHeight(), slot.getPreferredSize().height);
    }
    int h = labelFm.getHeight();
    if (supportingText != null) {
      h += getFontMetrics(TypeRole.BODY_SMALL.resolve()).getHeight();
    }
    return h;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }

  @Override
  public void doLayout() {
    final FontMetrics labelFm = getFontMetrics(TypeRole.LABEL_LARGE.resolve());
    final boolean rtl = !getComponentOrientation().isLeftToRight();
    int leadingEdge = INSET_X_PX + (hasLeadingColumn() ? ICON_SIZE_PX + BETWEEN_PX : 0);
    int trailingEdge = getWidth() - INSET_X_PX;

    if (trailingIcon != null) {
      trailingEdge -= ICON_SIZE_PX + BETWEEN_PX;
    }
    if (trailingText != null) {
      trailingEdge -=
          getFontMetrics(TypeRole.LABEL_LARGE.resolve()).stringWidth(trailingText) + BETWEEN_PX;
    }
    if (badge != null) {
      final Dimension bp = badge.getPreferredSize();
      final int bx = trailingEdge - bp.width;
      placeChild(badge, rtl ? getWidth() - bx - bp.width : bx, (getHeight() - bp.height) / 2, bp);
      trailingEdge -= bp.width + BETWEEN_PX;
    }
    if (slot != null) {
      final Dimension sp = slot.getPreferredSize();
      final int sw = Math.max(0, trailingEdge - leadingEdge);
      final int sx = rtl ? getWidth() - leadingEdge - sw : leadingEdge;
      placeChild(
          slot,
          sx,
          (getHeight() - Math.min(sp.height, getHeight())) / 2,
          sw,
          Math.min(sp.height, getHeight()));
    }
    // Painted slots (icons/text) are positioned in paintComponent; child components handled above.
  }

  private void placeChild(final JComponent c, final int x, final int y, final Dimension pref) {
    placeChild(c, x, y, pref.width, pref.height);
  }

  private void placeChild(final JComponent c, final int x, final int y, final int w, final int h) {
    c.setBounds(x, y, w, h);
  }

  // -------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final float contentAlpha = isEnabled() ? 1f : StateLayer.disabledContentOpacity();
      paintSelectedFill(g2);
      paintStateLayer(g2);
      paintRipple(g2);
      paintLeading(g2, contentAlpha);
      paintLabelAndSupporting(g2, contentAlpha);
      paintTrailing(g2, contentAlpha);
      paintFocusRing(g2);
    } finally {
      g2.dispose();
    }
  }

  private Rectangle visualBounds() {
    final int inset = Math.max(0, (getHeight() - VISUAL_HEIGHT_PX) / 2);
    return new Rectangle(0, inset, getWidth(), getHeight() - 2 * inset);
  }

  private void paintSelectedFill(final Graphics2D g2) {
    if (!selected) {
      return;
    }
    final Rectangle b = visualBounds();
    g2.setColor((vibrant() ? ColorRole.TERTIARY : ColorRole.TERTIARY_CONTAINER).resolve());
    g2.fill(new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, ARC_PX * 2f, ARC_PX * 2f));
  }

  private void paintStateLayer(final Graphics2D g2) {
    if (!isEnabled()) {
      return;
    }
    final StateLayer overlay = pressed ? StateLayer.PRESSED : hovered ? StateLayer.HOVER : null;
    if (overlay == null) {
      return;
    }
    final Rectangle b = visualBounds();
    final Graphics2D s = (Graphics2D) g2.create();
    try {
      s.setComposite(AlphaComposite.SrcOver.derive(overlay.opacity()));
      s.setColor(stateLayerColor());
      s.fill(new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, ARC_PX * 2f, ARC_PX * 2f));
    } finally {
      s.dispose();
    }
  }

  private void paintRipple(final Graphics2D g2) {
    if (rippleOrigin == null || rippleProgress >= 1f) {
      return;
    }
    final Rectangle b = visualBounds();
    final Graphics2D r = (Graphics2D) g2.create();
    try {
      r.translate(b.x, b.y);
      RipplePainter.paint(
          r,
          b.width,
          b.height,
          new Point(rippleOrigin.x - b.x, rippleOrigin.y - b.y),
          rippleProgress,
          ARC_PX,
          stateLayerColor());
    } finally {
      r.dispose();
    }
  }

  private void paintLeading(final Graphics2D g2, final float alpha) {
    if (!hasLeadingColumn()) {
      return;
    }
    final Icon icon = selected ? checkmark : leadingIcon;
    if (icon == null) {
      return;
    }
    final boolean rtl = !getComponentOrientation().isLeftToRight();
    final int x = rtl ? getWidth() - INSET_X_PX - ICON_SIZE_PX : INSET_X_PX;
    final int y = (getHeight() - ICON_SIZE_PX) / 2;
    paintWithAlpha(g2, alpha, ig -> icon.paintIcon(this, ig, x, y));
  }

  private void paintLabelAndSupporting(final Graphics2D g2, final float alpha) {
    if (slot != null) {
      return;
    }
    final boolean rtl = !getComponentOrientation().isLeftToRight();
    final FontMetrics labelFm = g2.getFontMetrics(TypeRole.LABEL_LARGE.resolve());
    final int textLeft = INSET_X_PX + (hasLeadingColumn() ? ICON_SIZE_PX + BETWEEN_PX : 0);
    final int labelW = labelFm.stringWidth(label);
    final int x = rtl ? getWidth() - textLeft - labelW : textLeft;

    final int blockH = contentHeight(labelFm);

    paintWithAlpha(
        g2,
        alpha,
        tg -> {
          tg.setFont(TypeRole.LABEL_LARGE.resolve());
          tg.setColor(labelColor());
          tg.drawString(label, x, (getHeight() - blockH) / 2 + labelFm.getAscent());
          if (supportingText != null) {
            final FontMetrics sf = tg.getFontMetrics(TypeRole.BODY_SMALL.resolve());
            final int sw = sf.stringWidth(supportingText);
            final int sx = rtl ? getWidth() - textLeft - sw : textLeft;
            tg.setFont(TypeRole.BODY_SMALL.resolve());
            tg.setColor(supportingColor());
            tg.drawString(
                supportingText,
                sx,
                (getHeight() - blockH) / 2 + labelFm.getHeight() + sf.getAscent());
          }
        });
  }

  private void paintTrailing(final Graphics2D g2, final float alpha) {
    final boolean rtl = !getComponentOrientation().isLeftToRight();
    int edge = getWidth() - INSET_X_PX;
    if (trailingIcon != null) {
      final int x = rtl ? INSET_X_PX : edge - ICON_SIZE_PX;
      final int y = (getHeight() - ICON_SIZE_PX) / 2;
      paintWithAlpha(g2, alpha, ig -> trailingIcon.paintIcon(this, ig, x, y));
      edge -= ICON_SIZE_PX + BETWEEN_PX;
    }
    if (trailingText != null) {
      final FontMetrics fm = g2.getFontMetrics(TypeRole.LABEL_LARGE.resolve());
      final int tw = fm.stringWidth(trailingText);
      final int x =
          rtl ? INSET_X_PX + (trailingIcon != null ? ICON_SIZE_PX + BETWEEN_PX : 0) : edge - tw;
      final int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
      paintWithAlpha(
          g2,
          alpha,
          tg -> {
            tg.setFont(TypeRole.LABEL_LARGE.resolve());
            tg.setColor(trailingTextColor());
            tg.drawString(trailingText, x, y);
          });
    }
  }

  private void paintFocusRing(final Graphics2D g2) {
    if (!focused) {
      return;
    }
    final Rectangle b = visualBounds();
    final Graphics2D f = (Graphics2D) g2.create();
    try {
      f.setStroke(new BasicStroke(FOCUS_RING_STROKE_PX));
      f.setColor(ColorRole.SECONDARY.resolve());
      final float in = FOCUS_RING_INSET_PX + FOCUS_RING_STROKE_PX / 2f;
      f.draw(
          new RoundRectangle2D.Float(
              b.x + in, b.y + in, b.width - 2 * in, b.height - 2 * in, ARC_PX * 2f, ARC_PX * 2f));
    } finally {
      f.dispose();
    }
  }

  private interface Painter {
    void paint(Graphics2D g);
  }

  private void paintWithAlpha(final Graphics2D g2, final float alpha, final Painter p) {
    final Graphics2D a = (Graphics2D) g2.create();
    try {
      if (alpha < 1f) {
        a.setComposite(AlphaComposite.SrcOver.derive(alpha));
      }
      p.paint(a);
    } finally {
      a.dispose();
    }
  }

  // ------------------------------------------------------------ colors

  private void applyIconColorFilter(final Icon icon) {
    if (icon instanceof FlatSVGIcon svg) {
      svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> iconColor()));
    }
  }

  // Content color "on" the selected fill: Standard selects TERTIARY_CONTAINER (→ ON_TERTIARY_-
  // CONTAINER), Vibrant selects the bold TERTIARY fill (→ ON_TERTIARY). Research §K rows 8 / 11.
  private boolean vibrant() {
    return colorStyle == ColorStyle.VIBRANT;
  }

  private Color onSelectedFill() {
    return (vibrant() ? ColorRole.ON_TERTIARY : ColorRole.ON_TERTIARY_CONTAINER).resolve();
  }

  private Color iconColor() {
    if (selected) {
      return onSelectedFill();
    }
    return (vibrant() ? ColorRole.ON_TERTIARY_CONTAINER : ColorRole.ON_SURFACE_VARIANT).resolve();
  }

  private Color labelColor() {
    if (selected) {
      return onSelectedFill();
    }
    return (vibrant() ? ColorRole.ON_TERTIARY_CONTAINER : ColorRole.ON_SURFACE).resolve();
  }

  private Color supportingColor() {
    if (selected) {
      return onSelectedFill();
    }
    return (vibrant() ? ColorRole.ON_TERTIARY_CONTAINER : ColorRole.ON_SURFACE_VARIANT).resolve();
  }

  private Color trailingTextColor() {
    return supportingColor();
  }

  private Color stateLayerColor() {
    if (selected) {
      return onSelectedFill();
    }
    return (vibrant() ? ColorRole.ON_TERTIARY_CONTAINER : ColorRole.ON_SURFACE).resolve();
  }

  // ------------------------------------------------------- interaction

  private void initInteraction() {
    final MouseAdapter ma =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            if (isEnabled()) {
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
            if (isEnabled() && e.getButton() == MouseEvent.BUTTON1) {
              pressed = true;
              startRipple(e.getPoint());
              repaint();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            final boolean wasPressed = pressed;
            pressed = false;
            repaint();
            if (wasPressed && isEnabled() && contains(e.getPoint())) {
              activate(e.getModifiersEx());
            }
          }
        };
    addMouseListener(ma);
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
            RIPPLE_TICK_MS,
            e -> {
              rippleProgress =
                  Math.min(1f, (System.nanoTime() - startNanos) / (RIPPLE_TOTAL_MS * 1_000_000f));
              repaint();
              if (rippleProgress >= 1f) {
                rippleTimer.stop();
              }
            });
    rippleTimer.setRepeats(true);
    rippleTimer.start();
  }

  @Override
  public void removeNotify() {
    if (rippleTimer != null) {
      rippleTimer.stop();
    }
    // Items are cached across menu opens, and teardown (or a setVisibleItems relayout) removes
    // them without a mouseExited — reset the transient interaction chrome here or a stale hover
    // fill / frozen mid-ripple frame repaints on the next open.
    hovered = false;
    pressed = false;
    rippleOrigin = null;
    rippleProgress = 1f;
    super.removeNotify();
  }

  // ----------------------------------------------------- accessibility

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaMenuItem();
    }
    return accessibleContext;
  }

  /**
   * Accessible context for a menu item: {@link AccessibleRole#MENU_ITEM}, accessible name = the
   * item label (the leading icon is decorative, §X), {@link AccessibleState#SELECTED} when
   * selected, and additionally {@link AccessibleState#CHECKED} when the parent menu is in {@link
   * SelectionMode#MULTI} (checkbox-like) — so the ✓ is never the only selection signal.
   *
   * @author Charles Bryan (cfb3@uw.edu)
   * @version v0.4.0
   * @since v0.4.0
   */
  protected final class AccessibleElwhaMenuItem extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.MENU_ITEM;
    }

    @Override
    public String getAccessibleName() {
      final String override = super.getAccessibleName();
      if (override != null && !override.isEmpty()) {
        return override;
      }
      return label;
    }

    @Override
    public AccessibleStateSet getAccessibleStateSet() {
      final AccessibleStateSet states = super.getAccessibleStateSet();
      if (selected) {
        states.add(AccessibleState.SELECTED);
        if (checkable) {
          states.add(AccessibleState.CHECKED);
        }
      }
      if (focused) {
        states.add(AccessibleState.FOCUSED);
      }
      return states;
    }
  }
}
