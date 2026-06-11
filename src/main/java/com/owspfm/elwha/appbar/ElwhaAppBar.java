package com.owspfm.elwha.appbar;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * The M3 Expressive app bar — the token-themed header for the top of an app shell: a leading
 * navigation button, a bar-painted title (+ optional subtitle), and trailing action icon buttons.
 * Ships the three Expressive variants ({@link AppBarVariant#SMALL} / {@link
 * AppBarVariant#MEDIUM_FLEXIBLE} / {@link AppBarVariant#LARGE_FLEXIBLE}); center-aligned text is an
 * option ({@link #setTitleCentered(boolean)}), not a variant, per the Expressive respec.
 *
 * <p><strong>Hosting.</strong> The bar is an in-flow component — drop it in {@code
 * BorderLayout.NORTH} above the content it heads. It fills whatever width the host grants and
 * reports its variant's height as preferred size.
 *
 * <p><strong>Slots.</strong> The navigation button and trailing elements are real Swing children
 * laid out in 48&nbsp;px slots (4&nbsp;px edge spaces, zero gap between trailing slots — the
 * v14.0.0 token values); the title and subtitle are painted by the bar itself. Actions are {@link
 * ElwhaIconButton}s — the M3 anatomy — while {@link #addTrailingElement(JComponent)} carries the
 * Expressive imagery/avatar/filled-button allowance. The bar never restyles hosted components
 * beyond positioning them.
 *
 * <pre>{@code
 * ElwhaAppBar bar = ElwhaAppBar.small();
 * bar.setTitle("Inbox");
 * bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", e -> drawer.toggle());
 * bar.addAction(MaterialIcons.favorite(), "Favorite", e -> favorite());
 * bar.addAction(MaterialIcons.moreVert(), "More options", e -> menu.show(...));
 * frame.add(bar, BorderLayout.NORTH);
 * }</pre>
 *
 * <p>Design: {@code docs/research/elwha-appbar-design.md}; research: {@code
 * elwha-appbar-research.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaAppBar extends JComponent {

  static final int STRIP_HEIGHT_PX = 64;
  static final int EDGE_SPACE_PX = 4;
  static final int SLOT_SIZE_PX = 48;
  static final int TITLE_INSET_PX = 16;
  static final int EXPANDED_MARGIN_PX = 16;

  private final AppBarVariant variant;
  private final List<JComponent> trailingElements = new ArrayList<>();
  private final List<ElwhaIconButton> actions = new ArrayList<>();

  private ElwhaIconButton navigationIcon;
  private String title = "";
  private String subtitle;
  private boolean titleCentered;

  /**
   * Constructs a {@link AppBarVariant#SMALL} app bar.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaAppBar() {
    this(AppBarVariant.SMALL);
  }

  /**
   * Constructs an app bar of the given variant.
   *
   * @param variant the M3 variant; required
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaAppBar(final AppBarVariant variant) {
    this.variant = Objects.requireNonNull(variant, "variant");
    setOpaque(true);
  }

  /**
   * Constructs a small app bar — the single-row regular bar.
   *
   * @return a new {@link AppBarVariant#SMALL} bar
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaAppBar small() {
    return new ElwhaAppBar(AppBarVariant.SMALL);
  }

  /**
   * Constructs a medium flexible app bar — a larger headline that collapses on scroll.
   *
   * @return a new {@link AppBarVariant#MEDIUM_FLEXIBLE} bar
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaAppBar mediumFlexible() {
    return new ElwhaAppBar(AppBarVariant.MEDIUM_FLEXIBLE);
  }

  /**
   * Constructs a large flexible app bar — emphasizes the page headline; collapses on scroll.
   *
   * @return a new {@link AppBarVariant#LARGE_FLEXIBLE} bar
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaAppBar largeFlexible() {
    return new ElwhaAppBar(AppBarVariant.LARGE_FLEXIBLE);
  }

  /**
   * The bar's M3 variant.
   *
   * @return the variant, never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public AppBarVariant getVariant() {
    return variant;
  }

  // ------------------------------------------------------------------- slots

  /**
   * Installs the leading navigation button, replacing any previous one. Pass {@code null} to clear;
   * with no navigation button the title takes the 16&nbsp;px edge inset instead.
   *
   * <p>Icon-only buttons need an accessible name (the MDC content-description requirement) — set
   * one via {@code getAccessibleContext().setAccessibleName(...)} or use the {@link
   * #setNavigationIcon(Icon, String, ActionListener)} convenience, which does.
   *
   * @param button the navigation button, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setNavigationIcon(final ElwhaIconButton button) {
    if (navigationIcon != null) {
      remove(navigationIcon);
    }
    navigationIcon = button;
    if (button != null) {
      add(button);
    }
    revalidate();
    repaint();
  }

  /**
   * Convenience: creates a {@linkplain ElwhaIconButton#standardIconButton(Icon) standard} icon
   * button with the given glyph, accessible name, and listener, and installs it as the navigation
   * button.
   *
   * @param icon the glyph, e.g. {@code MaterialIcons.menu()}; required
   * @param accessibleName the accessible name (also the tooltip); required
   * @param listener the action listener; null is allowed
   * @return the created button, for further configuration
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaIconButton setNavigationIcon(
      final Icon icon, final String accessibleName, final ActionListener listener) {
    final ElwhaIconButton button = namedStandardButton(icon, accessibleName, listener);
    setNavigationIcon(button);
    return button;
  }

  /**
   * The leading navigation button, or {@code null} when none is installed.
   *
   * @return the navigation button
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaIconButton getNavigationIcon() {
    return navigationIcon;
  }

  /**
   * Adds an action icon button to the trailing end. Actions lay out in add order ending 4&nbsp;px
   * from the trailing edge, zero gap between slots.
   *
   * <p>Icon-only buttons need an accessible name — see {@link #addAction(Icon, String,
   * ActionListener)}.
   *
   * @param action the action button; required
   * @return {@code action}, for chaining
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaIconButton addAction(final ElwhaIconButton action) {
    Objects.requireNonNull(action, "action");
    actions.add(action);
    trailingElements.add(action);
    add(action);
    revalidate();
    repaint();
    return action;
  }

  /**
   * Convenience: creates a {@linkplain ElwhaIconButton#standardIconButton(Icon) standard} icon
   * button with the given glyph, accessible name, and listener, and adds it as an action.
   *
   * @param icon the glyph, e.g. {@code MaterialIcons.moreVert()}; required
   * @param accessibleName the accessible name (also the tooltip); required
   * @param listener the action listener; null is allowed
   * @return the created button
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaIconButton addAction(
      final Icon icon, final String accessibleName, final ActionListener listener) {
    return addAction(namedStandardButton(icon, accessibleName, listener));
  }

  /**
   * Removes a previously added action.
   *
   * @param action the action to remove; unknown actions are ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeAction(final ElwhaIconButton action) {
    if (actions.remove(action)) {
      trailingElements.remove(action);
      remove(action);
      revalidate();
      repaint();
    }
  }

  /**
   * The action buttons, in add order.
   *
   * @return a snapshot of the actions; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public List<ElwhaIconButton> getActions() {
    return new ArrayList<>(actions);
  }

  /**
   * Adds an arbitrary trailing element — the Expressive allowance for imagery, avatars, and filled
   * buttons in the trailing slot. The element joins the trailing run in add order, vertically
   * centered in the 64&nbsp;px strip; the bar does not restyle it.
   *
   * @param element the element to add; required
   * @return {@code element}, for chaining
   * @version v0.4.0
   * @since v0.4.0
   */
  public JComponent addTrailingElement(final JComponent element) {
    Objects.requireNonNull(element, "element");
    trailingElements.add(element);
    add(element);
    revalidate();
    repaint();
    return element;
  }

  /**
   * Removes a previously added trailing element (or action).
   *
   * @param element the element to remove; unknown elements are ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeTrailingElement(final JComponent element) {
    if (trailingElements.remove(element)) {
      actions.remove(element);
      remove(element);
      revalidate();
      repaint();
    }
  }

  private static ElwhaIconButton namedStandardButton(
      final Icon icon, final String accessibleName, final ActionListener listener) {
    Objects.requireNonNull(icon, "icon");
    Objects.requireNonNull(accessibleName, "accessibleName");
    final ElwhaIconButton button = ElwhaIconButton.standardIconButton(icon);
    button.getAccessibleContext().setAccessibleName(accessibleName);
    button.setToolTipText(accessibleName);
    if (listener != null) {
      button.addActionListener(listener);
    }
    return button;
  }

  // -------------------------------------------------------------------- text

  /**
   * Sets the title — the bar's headline, painted by the bar in the variant's title role. Single
   * line; ellipsized at the slot edges.
   *
   * @param title the title text; {@code null} is treated as empty
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTitle(final String title) {
    this.title = title == null ? "" : title;
    revalidate();
    repaint();
  }

  /**
   * The title text.
   *
   * @return the title; never {@code null}, possibly empty
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the subtitle, painted under the title in the variant's subtitle role. {@code null} or
   * empty clears it — flexible variants drop to the no-subtitle expanded height.
   *
   * @param subtitle the subtitle text, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSubtitle(final String subtitle) {
    this.subtitle = subtitle == null || subtitle.isEmpty() ? null : subtitle;
    revalidate();
    repaint();
  }

  /**
   * The subtitle text, or {@code null} when none is set.
   *
   * @return the subtitle
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getSubtitle() {
    return subtitle;
  }

  /**
   * Whether the title (and subtitle) center over the container instead of leading-aligning after
   * the navigation button. Centered text stays clear of the button slots, ellipsizing against them
   * when space runs out.
   *
   * @param titleCentered {@code true} to center
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setTitleCentered(final boolean titleCentered) {
    if (this.titleCentered != titleCentered) {
      this.titleCentered = titleCentered;
      repaint();
    }
  }

  /**
   * Whether the title is center-aligned.
   *
   * @return the centered flag
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isTitleCentered() {
    return titleCentered;
  }

  // ------------------------------------------------------------------ layout

  @Override
  public void doLayout() {
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final int width = getWidth();
    if (navigationIcon != null) {
      final int slot = slotWidth(navigationIcon);
      placeInSlot(navigationIcon, ltr ? EDGE_SPACE_PX : width - EDGE_SPACE_PX - slot, slot);
    }
    int edge = ltr ? width - EDGE_SPACE_PX : EDGE_SPACE_PX;
    for (int i = trailingElements.size() - 1; i >= 0; i--) {
      final JComponent element = trailingElements.get(i);
      final int slot = slotWidth(element);
      edge = ltr ? edge - slot : edge + slot;
      placeInSlot(element, ltr ? edge : edge - slot, slot);
    }
  }

  private static int slotWidth(final JComponent element) {
    return Math.max(SLOT_SIZE_PX, element.getPreferredSize().width);
  }

  private void placeInSlot(final JComponent element, final int slotX, final int slotWidth) {
    final Dimension pref = element.getPreferredSize();
    final int w = Math.min(pref.width, slotWidth);
    final int h = Math.min(pref.height, STRIP_HEIGHT_PX);
    element.setBounds(slotX + (slotWidth - w) / 2, (STRIP_HEIGHT_PX - h) / 2, w, h);
  }

  // The title region's logical bounds (LTR-equivalent coordinates handled by caller flips):
  // start after the nav slot (or the 16px inset), end before the trailing run (or the 16px
  // inset). Returns {start, end} in component coordinates honoring orientation.
  private int[] titleRegion() {
    final int width = getWidth();
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final int navExtent =
        navigationIcon != null ? EDGE_SPACE_PX + slotWidth(navigationIcon) : TITLE_INSET_PX;
    int trailingExtent = TITLE_INSET_PX;
    if (!trailingElements.isEmpty()) {
      trailingExtent = EDGE_SPACE_PX;
      for (JComponent element : trailingElements) {
        trailingExtent += slotWidth(element);
      }
    }
    return ltr
        ? new int[] {navExtent, width - trailingExtent}
        : new int[] {trailingExtent, width - navExtent};
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    int width = navigationIcon != null ? EDGE_SPACE_PX + slotWidth(navigationIcon) : TITLE_INSET_PX;
    width += getFontMetrics(stripTitleFont()).stringWidth(title);
    if (trailingElements.isEmpty()) {
      width += TITLE_INSET_PX;
    } else {
      width += EDGE_SPACE_PX;
      for (JComponent element : trailingElements) {
        width += slotWidth(element);
      }
    }
    return new Dimension(width, variant.expandedHeightPx(subtitle != null));
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(SLOT_SIZE_PX * 2, getPreferredSize().height);
  }

  // ------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setColor(ColorRole.SURFACE.resolve());
      g2.fillRect(0, 0, getWidth(), getHeight());
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      if (!variant.isFlexible()) {
        paintStripText(g2);
      }
    } finally {
      g2.dispose();
    }
  }

  // The small bar's title+subtitle stack, vertically centered as a unit in the 64px strip —
  // also the collapsed-title layer the flexible variants fade in (S4).
  private void paintStripText(final Graphics2D g2) {
    if (title.isEmpty() && subtitle == null) {
      return;
    }
    final Font titleFont = stripTitleFont();
    final Font subtitleFont = TypeRole.LABEL_MEDIUM.resolve();
    final FontMetrics titleFm = g2.getFontMetrics(titleFont);
    final FontMetrics subtitleFm = g2.getFontMetrics(subtitleFont);
    final int[] region = titleRegion();
    final int available = region[1] - region[0];
    if (available <= 0) {
      return;
    }
    final int blockHeight = titleFm.getHeight() + (subtitle != null ? subtitleFm.getHeight() : 0);
    final int top = (STRIP_HEIGHT_PX - blockHeight) / 2;

    g2.setColor(ColorRole.ON_SURFACE.resolve());
    g2.setFont(titleFont);
    drawClippedLine(g2, title, titleFm, region, top + titleFm.getAscent());

    if (subtitle != null) {
      g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
      g2.setFont(subtitleFont);
      drawClippedLine(
          g2, subtitle, subtitleFm, region, top + titleFm.getHeight() + subtitleFm.getAscent());
    }
  }

  // Lays one line into the title region: leading-aligned (orientation-aware) or centered over
  // the container, clamped into the region, ellipsized to fit.
  private void drawClippedLine(
      final Graphics2D g2,
      final String text,
      final FontMetrics fm,
      final int[] region,
      final int baseline) {
    final String clipped = clipText(text, fm, region[1] - region[0]);
    final int textWidth = fm.stringWidth(clipped);
    final int x;
    if (titleCentered) {
      x = Math.max(region[0], Math.min((getWidth() - textWidth) / 2, region[1] - textWidth));
    } else if (getComponentOrientation().isLeftToRight()) {
      x = region[0];
    } else {
      x = region[1] - textWidth;
    }
    g2.drawString(clipped, x, baseline);
  }

  static String clipText(final String text, final FontMetrics fm, final int available) {
    if (available <= 0 || fm.stringWidth(text) <= available) {
      return text;
    }
    final String ellipsis = "…";
    final int ellipsisWidth = fm.stringWidth(ellipsis);
    for (int end = text.length() - 1; end > 0; end--) {
      if (fm.stringWidth(text.substring(0, end)) + ellipsisWidth <= available) {
        return text.substring(0, end) + ellipsis;
      }
    }
    return ellipsisWidth <= available ? ellipsis : "";
  }

  private Font stripTitleFont() {
    return TypeRole.TITLE_LARGE.resolve();
  }
}
