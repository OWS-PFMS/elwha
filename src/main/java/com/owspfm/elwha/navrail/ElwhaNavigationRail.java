package com.owspfm.elwha.navrail;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * The M3 Expressive Navigation Rail container — a vertical {@link JComponent} that docks to a
 * leading edge of the host frame and slots together a header chrome (optional menu button + FAB), a
 * stack of {@link ElwhaNavRailDestination primary destinations}, and an optional list of trailing
 * utility actions.
 *
 * <p><strong>Phase 2 / Story #234 skeleton.</strong> This first revision lays out the rail's
 * Collapsed chrome — header slots, the destination region (empty until story #235 wires the list),
 * the trailing actions slot, surface paint, divider, and elevation. The Expanded variant and the
 * Collapsed↔Expanded morph land in Phase 3; only {@link Variant#COLLAPSED} is functional today.
 *
 * <p><strong>Geometry (M3 token-locked, see {@code elwha-navigation-rail-design.md} §4).</strong>
 * Collapsed width is fixed at {@value #COLLAPSED_WIDTH_PX} dp. Header chrome is top-anchored with
 * {@value #CHROME_PAD_PX} dp leading / top padding and a {@value #CHROME_GAP_PX} dp gap between the
 * menu button and the FAB. Trailing actions stack at the bottom of the rail with {@value
 * #CHROME_PAD_PX} dp bottom pad and {@value #TRAILING_ACTION_GAP_PX} dp inter-action gap. The
 * destination region (story #235 fills it) is the slack between the chrome and the trailing actions
 * and grows with the rail's height.
 *
 * <p><strong>Paint contract.</strong>
 *
 * <ul>
 *   <li>Surface fill (off by default; opt in via {@link #setSurfaceFilled(boolean)}) renders the
 *       rail body in {@link ColorRole#SURFACE_CONTAINER} — M3's token for rail backgrounds.
 *   <li>Optional trailing-edge divider (1 px {@link ColorRole#OUTLINE_VARIANT}) when {@link
 *       #setDivider(boolean) divider} is enabled. Mirrors under right-to-left orientation.
 *   <li>Elevation 0 (default) paints no shadow; elevation 1 paints a soft inset trailing-edge
 *       gradient as a self-contained visual elevation cue. Full {@link
 *       com.owspfm.elwha.theme.ShadowPainter}-driven drop shadow integration arrives in Phase 4
 *       when the rail is hosted on the Showcase frame's layered pane (where insets can be
 *       reserved).
 * </ul>
 *
 * <p><strong>Accessibility.</strong> {@link AccessibleRole#PAGE_TAB_LIST} (pairs with each
 * destination's {@link AccessibleRole#PAGE_TAB} from Phase 1 — matches ARIA {@code tablist} /
 * {@code tab}). Consumers must supply an accessible name via {@link #setAccessibleName(String)};
 * the rail logs a {@link Logger#warning(String)} at first paint if no name is set.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaNavigationRail extends JComponent {

  private static final Logger LOG = Logger.getLogger(ElwhaNavigationRail.class.getName());

  /** The rail's two variants per M3 Expressive — Collapsed (96 dp) and Expanded (220–360 dp). */
  public enum Variant {
    /** Compact icon-and-label rail, fixed 96 dp wide. */
    COLLAPSED,
    /** Wide rail with inline labels + section headers; lands in Phase 3 of epic #159. */
    EXPANDED
  }

  static final int COLLAPSED_WIDTH_PX = 96;
  static final int CHROME_PAD_PX = 16;
  static final int CHROME_GAP_PX = 16;
  static final int TRAILING_ACTION_GAP_PX = 4;
  static final int DIVIDER_WIDTH_PX = 1;
  static final int ELEVATION_GRADIENT_PX = 12;

  private Variant variant;
  private boolean surfaceFilled;
  private boolean divider;
  private int elevation;

  private ElwhaIconButton menuButton;
  private ElwhaFab fab;
  private final List<ElwhaIconButton> trailingActions = new ArrayList<>();

  private boolean missingAccessibleNameWarned;

  private ElwhaNavigationRail(final Variant variant) {
    this.variant = Objects.requireNonNull(variant, "variant");
    setLayout(null);
    setOpaque(false);
  }

  /**
   * Creates a Collapsed rail — 96 dp wide, icon-over-label destinations (once wired in story #235),
   * Standard-form FAB if present.
   *
   * @return a new rail in {@link Variant#COLLAPSED}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaNavigationRail collapsed() {
    return new ElwhaNavigationRail(Variant.COLLAPSED);
  }

  /**
   * Returns the rail's current variant.
   *
   * @return the active variant
   * @version v0.3.0
   * @since v0.3.0
   */
  public Variant getVariant() {
    return variant;
  }

  /**
   * Snaps the rail to the given variant without animation. Animated {@code morphTo(Variant)}
   * arrives in Phase 3.
   *
   * <p>Calling with {@link Variant#EXPANDED} throws {@link UnsupportedOperationException} until the
   * Expanded layout lands in Phase 3 (epic #159). The enum exposes both values now so the API shape
   * is locked, but only {@link Variant#COLLAPSED} is functional in Phase 2.
   *
   * @param v the new variant; must not be {@code null}
   * @throws UnsupportedOperationException if {@code v} is {@link Variant#EXPANDED}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setVariant(final Variant v) {
    Objects.requireNonNull(v, "variant");
    if (v == Variant.EXPANDED) {
      throw new UnsupportedOperationException(
          "EXPANDED variant lands in Phase 3 of epic #159 (Navigation Rail)");
    }
    if (this.variant == v) {
      return;
    }
    this.variant = v;
    revalidate();
    repaint();
  }

  /**
   * Reports whether the rail paints its surface fill ({@link ColorRole#SURFACE_CONTAINER}).
   *
   * @return {@code true} if filled, {@code false} if transparent (the default)
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isSurfaceFilled() {
    return surfaceFilled;
  }

  /**
   * Sets whether the rail paints a {@link ColorRole#SURFACE_CONTAINER} fill. Off by default — the
   * rail is transparent so it composes with whatever background the host frame uses.
   *
   * @param filled {@code true} to paint the surface, {@code false} for transparent
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setSurfaceFilled(final boolean filled) {
    if (this.surfaceFilled == filled) {
      return;
    }
    this.surfaceFilled = filled;
    repaint();
  }

  /**
   * Reports whether the rail paints a trailing-edge divider.
   *
   * @return {@code true} if the divider is enabled
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean hasDivider() {
    return divider;
  }

  /**
   * Sets whether the rail paints a 1 px {@link ColorRole#OUTLINE_VARIANT} divider on its trailing
   * edge (right edge under LTR, left edge under RTL).
   *
   * @param divider {@code true} to paint the divider
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setDivider(final boolean divider) {
    if (this.divider == divider) {
      return;
    }
    this.divider = divider;
    repaint();
  }

  /**
   * Returns the rail's elevation level.
   *
   * @return the elevation level (0 or 1)
   * @version v0.3.0
   * @since v0.3.0
   */
  public int getElevation() {
    return elevation;
  }

  /**
   * Sets the rail's elevation level. Only {@code 0} (flat — the M3 default) and {@code 1} (single
   * standard surface elevation) are accepted in Phase 2. Higher levels are not part of M3's rail
   * spec.
   *
   * @param level {@code 0} or {@code 1}
   * @throws IllegalArgumentException if {@code level} is not in {@code {0, 1}}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setElevation(final int level) {
    if (level != 0 && level != 1) {
      throw new IllegalArgumentException("elevation level must be 0 or 1: " + level);
    }
    if (this.elevation == level) {
      return;
    }
    this.elevation = level;
    repaint();
  }

  /**
   * Slots an icon button into the rail's menu position (top of the rail, above the FAB). Pass
   * {@code null} to clear. The lib does not bind any default click semantics to this button in
   * Phase 2; consumers are free to wire it to whatever action they want (typically
   * variant-toggling, which the rail will pick up automatically in Phase 3).
   *
   * @param menu the icon button to slot, or {@code null} to remove
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setMenuButton(final ElwhaIconButton menu) {
    if (this.menuButton == menu) {
      return;
    }
    if (this.menuButton != null) {
      remove(this.menuButton);
    }
    this.menuButton = menu;
    if (menu != null) {
      add(menu);
    }
    revalidate();
    repaint();
  }

  /**
   * Returns the currently-slotted menu button, or {@code null} if none.
   *
   * @return the menu button, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaIconButton getMenuButton() {
    return menuButton;
  }

  /**
   * Slots a {@link ElwhaFab} into the rail's anchored-action position (below the menu button). Pass
   * {@code null} to clear. The rail does not orchestrate the FAB's Standard↔Extended form in Phase
   * 2 — that wiring lands in Phase 3 alongside the Collapsed↔Expanded morph; the FAB simply sits in
   * its current form.
   *
   * @param fab the FAB to slot, or {@code null} to remove
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setFab(final ElwhaFab fab) {
    if (this.fab == fab) {
      return;
    }
    if (this.fab != null) {
      remove(this.fab);
    }
    this.fab = fab;
    if (fab != null) {
      add(fab);
    }
    revalidate();
    repaint();
  }

  /**
   * Returns the currently-slotted FAB, or {@code null} if none.
   *
   * @return the FAB, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab getFab() {
    return fab;
  }

  /**
   * Sets the rail's trailing actions — utility / system icon buttons (theme toggle, settings, help,
   * playground launcher, etc.) anchored to the bottom of the rail surface, below the destination
   * stack. {@code null} and an empty list both clear the slot.
   *
   * <p>Trailing actions are not destinations and are not part of the rail's selection model;
   * clicking one fires that button's own {@link java.awt.event.ActionListener} chain, nothing more.
   * The slot is an Elwha extension beyond the formal M3 token tables — m3.material.io itself
   * renders a rail with bottom-anchored utility buttons, so the pattern is demonstrated by the
   * spec's own home site even though not formally documented (design doc §3, PR #231).
   *
   * @param actions the actions to slot at the bottom of the rail, or {@code null} / empty to clear
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setTrailingActions(final List<ElwhaIconButton> actions) {
    for (final ElwhaIconButton old : trailingActions) {
      remove(old);
    }
    trailingActions.clear();
    if (actions != null) {
      for (final ElwhaIconButton a : actions) {
        if (a == null) {
          continue;
        }
        trailingActions.add(a);
        add(a);
      }
    }
    revalidate();
    repaint();
  }

  /**
   * Returns a defensive copy of the current trailing actions list. Modifying the returned list does
   * not change the rail.
   *
   * @return a defensive copy of the trailing actions
   * @version v0.3.0
   * @since v0.3.0
   */
  public List<ElwhaIconButton> getTrailingActions() {
    return new ArrayList<>(trailingActions);
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final int prefHeight = preferredContentHeight();
    return new Dimension(COLLAPSED_WIDTH_PX, prefHeight);
  }

  @Override
  public Dimension getMinimumSize() {
    if (isMinimumSizeSet()) {
      return super.getMinimumSize();
    }
    return new Dimension(COLLAPSED_WIDTH_PX, preferredContentHeight());
  }

  @Override
  public Dimension getMaximumSize() {
    if (isMaximumSizeSet()) {
      return super.getMaximumSize();
    }
    return new Dimension(COLLAPSED_WIDTH_PX, Integer.MAX_VALUE);
  }

  private int preferredContentHeight() {
    int h = CHROME_PAD_PX;
    if (menuButton != null) {
      h += menuButton.getPreferredSize().height + CHROME_GAP_PX;
    }
    if (fab != null) {
      h += fab.getPreferredSize().height + CHROME_GAP_PX;
    }
    h += CHROME_PAD_PX;
    h += trailingHeight();
    return Math.max(h, COLLAPSED_WIDTH_PX);
  }

  private int trailingHeight() {
    if (trailingActions.isEmpty()) {
      return 0;
    }
    int h = 0;
    for (final ElwhaIconButton a : trailingActions) {
      h += a.getPreferredSize().height;
    }
    h += TRAILING_ACTION_GAP_PX * (trailingActions.size() - 1);
    return h;
  }

  @Override
  public void doLayout() {
    final int w = getWidth();
    final int h = getHeight();

    int topY = CHROME_PAD_PX;
    if (menuButton != null) {
      final Dimension d = menuButton.getPreferredSize();
      menuButton.setBounds((w - d.width) / 2, topY, d.width, d.height);
      topY += d.height + CHROME_GAP_PX;
    }
    if (fab != null) {
      final Dimension d = fab.getPreferredSize();
      fab.setBounds((w - d.width) / 2, topY, d.width, d.height);
    }

    if (trailingActions.isEmpty()) {
      return;
    }
    int by = h - CHROME_PAD_PX;
    for (int i = trailingActions.size() - 1; i >= 0; i--) {
      final ElwhaIconButton a = trailingActions.get(i);
      final Dimension d = a.getPreferredSize();
      by -= d.height;
      a.setBounds((w - d.width) / 2, by, d.width, d.height);
      by -= TRAILING_ACTION_GAP_PX;
    }
  }

  @Override
  protected void paintComponent(final Graphics g) {
    warnIfMissingAccessibleName();
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int w = getWidth();
      final int h = getHeight();

      if (surfaceFilled) {
        g2.setColor(ColorRole.SURFACE_CONTAINER.resolve());
        g2.fillRect(0, 0, w, h);
      }

      if (elevation > 0) {
        paintElevationGradient(g2, w, h);
      }

      if (divider) {
        g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
        final boolean ltr = getComponentOrientation().isLeftToRight();
        final int x = ltr ? w - DIVIDER_WIDTH_PX : 0;
        g2.fillRect(x, 0, DIVIDER_WIDTH_PX, h);
      }
    } finally {
      g2.dispose();
    }
  }

  private void paintElevationGradient(final Graphics2D g, final int w, final int h) {
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final Color edgeColor = new Color(0, 0, 0, 30);
    final Color transparent = new Color(0, 0, 0, 0);
    final int gradientWidth = Math.min(ELEVATION_GRADIENT_PX, Math.max(1, w / 4));
    if (ltr) {
      final int edgeX = w - gradientWidth;
      final GradientPaint gp = new GradientPaint(edgeX, 0, transparent, (float) w, 0, edgeColor);
      g.setPaint(gp);
      g.fillRect(edgeX, 0, gradientWidth, h);
    } else {
      final GradientPaint gp = new GradientPaint(0, 0, edgeColor, gradientWidth, 0, transparent);
      g.setPaint(gp);
      g.fillRect(0, 0, gradientWidth, h);
    }
  }

  private void warnIfMissingAccessibleName() {
    if (missingAccessibleNameWarned) {
      return;
    }
    final AccessibleContext ctx = getAccessibleContext();
    final String name = ctx == null ? null : ctx.getAccessibleName();
    if (name == null || name.isEmpty()) {
      LOG.warning(
          "ElwhaNavigationRail has no accessible name. Call setAccessibleName(...) — e.g."
              + " \"Primary navigation\" — so screen readers can identify the rail.");
    }
    missingAccessibleNameWarned = true;
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaNavigationRail();
    }
    return accessibleContext;
  }

  /**
   * The rail's accessible context. Reports {@link AccessibleRole#PAGE_TAB_LIST} — the documented
   * pair to each destination's {@link AccessibleRole#PAGE_TAB}, matching ARIA's {@code tablist} /
   * {@code tab} pattern (design doc §10.1).
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  protected class AccessibleElwhaNavigationRail extends AccessibleJComponent {

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.PAGE_TAB_LIST;
    }
  }
}
