package com.owspfm.elwha.navrail;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ContentMorphPainter;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShadowPainter;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * The M3 Expressive Navigation Rail container — a vertical {@link JComponent} that docks to a
 * leading edge of the host frame and slots together a header chrome (optional menu button + FAB), a
 * stack of {@link ElwhaNavRailDestination primary destinations}, and an optional list of trailing
 * utility actions.
 *
 * <p><strong>Phase 3 — both variants and the Collapsed↔Expanded morph.</strong> The rail now
 * supports {@link Variant#COLLAPSED} (96 dp fixed) and {@link Variant#EXPANDED} (220–360 dp
 * configurable). The variant is changed via {@link #setVariant(Variant)} (snap) or {@link
 * #morphTo(Variant)} (animated, 350 ms — drives every {@link ElwhaNavRailDestination} in lock-step
 * and orchestrates the slotted {@link ElwhaFab}'s Standard↔Extended form per design doc §9). The
 * Expanded variant adds optional sections (section header + secondary destinations) via {@link
 * #addSection(String, List)} / {@link #clearSections()}; sections render only in Expanded.
 *
 * <p><strong>Geometry (M3 token-locked, see {@code elwha-navigation-rail-design.md} §4).</strong>
 * Collapsed width is fixed at {@value #COLLAPSED_WIDTH_PX} dp. Header chrome is top-anchored with
 * {@value #CHROME_PAD_PX} dp leading / top padding and a {@value #CHROME_GAP_PX} dp gap between the
 * menu button and the FAB. Trailing actions stack at the bottom of the rail with {@value
 * #CHROME_PAD_PX} dp bottom pad and {@value #TRAILING_ACTION_GAP_PX} dp inter-action gap. The
 * destination region is the slack between the chrome and the trailing actions and grows with the
 * rail's height.
 *
 * <p><strong>Vertical-space contract.</strong> {@link #getMinimumSize()} honestly reports the
 * height required to host every populated slot (chrome + every destination + every trailing action
 * + paddings); consumers should give the rail at least that much height (typically the full
 * content-area height of the host window). When the host gives the rail less than its minimum, the
 * trailing actions slide down to the bottom of the destination stack rather than overlapping it —
 * content then clips at the rail's bottom edge as graceful degradation. Collapsing the
 * trailing-actions stack into an overflow menu (parallel to a future {@code ElwhaFabMenu}) is filed
 * as a follow-up.
 *
 * <p><strong>Paint contract.</strong>
 *
 * <ul>
 *   <li>Surface fill (off by default; opt in via {@link #setSurfaceFilled(boolean)}) renders the
 *       rail body in {@link ColorRole#SURFACE_CONTAINER} — M3's token for rail backgrounds.
 *   <li>Optional trailing-edge divider (1 px {@link ColorRole#OUTLINE_VARIANT}) when {@link
 *       #setDivider(boolean) divider} is enabled. Mirrors under right-to-left orientation.
 *   <li>Elevation 0 (default) paints no shadow; elevation 1 paints an M3-aligned drop shadow via
 *       {@link com.owspfm.elwha.theme.ShadowPainter} — same painter / same tokens as {@link
 *       com.owspfm.elwha.card.ElwhaCard} and {@link com.owspfm.elwha.fab.ElwhaFab}. The shadow halo
 *       extends outward from the body silhouette; the host must reserve {@link
 *       com.owspfm.elwha.theme.ShadowPainter#shadowInsets(int)
 *       ShadowPainter.shadowInsets(elevation).right} (LTR) pixels of trailing-edge bounds clearance
 *       so the halo renders without clipping. A layered-pane host (e.g. The Elwha Showcase frame)
 *       sets the rail's {@link #setBounds bounds} to {@code preferredSize.width +
 *       shadowInsets(elevation).right} on the trailing side and the halo falls cleanly onto the
 *       content area behind the rail. A layout-managed host (e.g. {@code BorderLayout.WEST}) won't
 *       widen bounds — the halo silently clips on the body's trailing edge, which is the documented
 *       trade-off for hosts that can't reserve inset.
 * </ul>
 *
 * <p><strong>Accessibility.</strong> {@link AccessibleRole#PAGE_TAB_LIST} (pairs with each
 * destination's {@link AccessibleRole#PAGE_TAB} from Phase 1 — matches ARIA {@code tablist} /
 * {@code tab}). Consumers must supply an accessible name via {@code
 * getAccessibleContext().setAccessibleName(...)}; the rail logs a {@link Logger#warning(String)} at
 * first paint if no name is set.
 *
 * <p><strong>Keyboard navigation</strong> (design doc §10.2): Tab enters the rail at the menu
 * button (if present) → FAB (if present) → exactly one destination (the focused one, falling back
 * to the selected one, then the first) → trailing actions (in order) → next focusable after the
 * rail. Shift+Tab reverses. Within the destination band, ↑ / ↓ move focus cyclically to the
 * previous / next destination; Home / End jump to the first / last; Space / Enter activate the
 * focused destination (selection only changes on activation, never on focus traversal). Escape is
 * intentionally not consumed.
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
    /** Wide rail with inline labels + section headers (220–360 dp). */
    EXPANDED
  }

  /**
   * One section in the Expanded rail — a header label plus the secondary destinations that fall
   * under it. Sections paint only when {@link Variant#EXPANDED}; in Collapsed they're silently
   * hidden (their destinations are still tracked for selection-model membership but neither painted
   * nor laid out).
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public static final class Section {
    private final String header;
    private final List<ElwhaNavRailDestination> destinations;

    Section(final String header, final List<ElwhaNavRailDestination> destinations) {
      this.header = Objects.requireNonNull(header, "header");
      this.destinations = List.copyOf(destinations);
    }

    /** Returns the section header label. */
    public String getHeader() {
      return header;
    }

    /** Returns the section's secondary destinations as an unmodifiable list. */
    public List<ElwhaNavRailDestination> getDestinations() {
      return destinations;
    }
  }

  /**
   * Listener fired when the rail's selected destination changes. Parallels the chip-list selection
   * listener pattern.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  @FunctionalInterface
  public interface NavRailSelectionListener {
    /**
     * Called when the rail's selected destination changes (no-op same-instance calls suppressed).
     *
     * @param previous the previously-selected destination, or {@code null} if none
     * @param current the now-selected destination, or {@code null} if the rail is empty
     */
    void selectionChanged(ElwhaNavRailDestination previous, ElwhaNavRailDestination current);
  }

  /** Property name fired when the selected destination changes. */
  public static final String PROPERTY_SELECTED = "selected";

  /**
   * Property name fired when the rail's variant changes — once per {@link #setVariant(Variant)} or
   * per {@link #morphTo(Variant)} call (fires at the start of the morph, reporting the new target
   * value; not per tick).
   */
  public static final String PROPERTY_VARIANT = "variant";

  static final int COLLAPSED_WIDTH_PX = 96;
  static final int EXPANDED_WIDTH_MIN_PX = 220;
  static final int EXPANDED_WIDTH_MAX_PX = 360;
  static final int EXPANDED_WIDTH_DEFAULT_PX = 256;
  static final int CHROME_PAD_PX = 16;
  static final int CHROME_GAP_PX = 16;
  static final int DESTINATION_GAP_PX = 4;
  static final int TRAILING_ACTION_GAP_PX = 4;
  static final int DIVIDER_WIDTH_PX = 1;
  static final int M3_PRIMARY_MIN = 3;
  static final int M3_PRIMARY_MAX = 7;
  static final int SECTION_HEADER_TOP_PAD_PX = 16;
  static final int SECTION_HEADER_BOTTOM_PAD_PX = 8;
  // Headers align with the destination icon column (rail CHROME_PAD + destination
  // LEADING_PAD_EXPANDED)
  // so "Tools" / "Other" sit directly above the icon glyphs below them, matching M3's expanded
  // rail.
  static final int SECTION_HEADER_LEADING_PAD_PX =
      CHROME_PAD_PX + ElwhaNavRailDestination.LEADING_PAD_EXPANDED;

  // Mirrors {@code ElwhaFab.HOVER_ELEVATION} (which is private). Used to compute the FAB's
  // shadow-blur reserve so the rail can compensate when placing the FAB in Expanded — the FAB's
  // visible pill sits inset within its bounds by this elevation's shadow insets.
  private static final int FAB_HOVER_ELEVATION = 4;

  private Variant variant;
  private int expandedWidthPx = EXPANDED_WIDTH_DEFAULT_PX;
  private boolean surfaceFilled;
  private boolean divider;
  private int elevation;

  private ElwhaIconButton menuButton;
  private ElwhaFab fab;
  private final List<ElwhaIconButton> trailingActions = new ArrayList<>();

  private final List<ElwhaNavRailDestination> primary = new ArrayList<>();
  private final List<Section> sections = new ArrayList<>();
  private ElwhaNavRailDestination selected;
  private final List<NavRailSelectionListener> selectionListeners = new ArrayList<>();
  private final java.awt.event.ActionListener destinationClickListener =
      e -> {
        if (e.getSource() instanceof ElwhaNavRailDestination d) {
          setSelected(d);
        }
      };

  private boolean missingAccessibleNameWarned;

  // Morph state ------------------------------------------------------------
  private final MorphAnimator variantMorph = new MorphAnimator(this, MorphAnimator.MEDIUM3_MS);
  private Variant morphFrom = Variant.COLLAPSED;
  private Variant morphTo = Variant.COLLAPSED;

  private ElwhaNavigationRail(final Variant variant) {
    this.variant = Objects.requireNonNull(variant, "variant");
    setLayout(null);
    setOpaque(false);
    setFocusTraversalPolicyProvider(true);
    setFocusTraversalPolicy(new RailFocusTraversalPolicy());
    variantMorph.snapTo(variant == Variant.EXPANDED ? 1f : 0f);
    variantMorph.addProgressListener(this::broadcastMorphProgress);
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
   * Creates an Expanded rail — 220–360 dp wide (default {@value #EXPANDED_WIDTH_DEFAULT_PX} dp),
   * icon-beside-label destinations, optional sections, Extended-form FAB if present.
   *
   * @return a new rail in {@link Variant#EXPANDED}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaNavigationRail expanded() {
    return new ElwhaNavigationRail(Variant.EXPANDED);
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
   * Snaps the rail to the given variant without animation. The animated counterpart is {@link
   * #morphTo(Variant)}; consumers driving programmatic state (e.g. initial app config or a settings
   * restore) typically prefer this snap path while interactive consumers (clicking the menu button)
   * prefer {@code morphTo}.
   *
   * <p>The variant is broadcast to every primary + secondary destination via package-private {@code
   * setHostVariant}, and the slotted FAB's form is snapped to Standard / Extended accordingly so
   * chrome stays in sync with the rail.
   *
   * @param v the new variant; must not be {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setVariant(final Variant v) {
    Objects.requireNonNull(v, "variant");
    if (this.variant == v) {
      return;
    }
    final Variant prior = this.variant;
    this.variant = v;
    variantMorph.snapTo(v == Variant.EXPANDED ? 1f : 0f);
    pushHostVariantToDestinations();
    snapFabFormToVariant();
    syncMenuButtonGlyph();
    firePropertyChange(PROPERTY_VARIANT, prior, v);
    revalidate();
    repaint();
  }

  /**
   * Animates the rail to the target variant — 350 ms ({@link MorphAnimator#MEDIUM3_MS}) drives the
   * container width interpolation, every destination's per-frame morph (active-indicator dimensions
   * + label cross-fade per design doc §9.2), and orchestrates the slotted {@link ElwhaFab}'s
   * Standard↔Extended form so all chrome runs in phase.
   *
   * <p>Same-target calls are no-ops. {@link MorphAnimator#setReducedMotion(boolean) Reduced motion}
   * collapses the morph into a single-frame snap (the animator handles this internally — the rail
   * needs no special path).
   *
   * @param target the target variant; must not be {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void morphTo(final Variant target) {
    Objects.requireNonNull(target, "target");
    if (this.variant == target) {
      return;
    }
    final Variant prior = this.variant;
    this.morphFrom = this.variant;
    this.morphTo = target;
    this.variant = target;
    if (fab != null) {
      fab.morphTo(target == Variant.EXPANDED ? ElwhaFab.Form.EXTENDED : ElwhaFab.Form.STANDARD);
    }
    firePropertyChange(PROPERTY_VARIANT, prior, target);
    if (target == Variant.EXPANDED) {
      variantMorph.start();
    } else {
      variantMorph.reverse();
    }
    syncMenuButtonGlyph();
    revalidate();
    repaint();
  }

  /**
   * Reports whether the rail's variant-morph animator is currently scheduled — {@code true} from
   * the {@link #morphTo(Variant)} call that starts a transition until the animator settles. Useful
   * for consumers that want to defer follow-on layout work until the morph completes.
   *
   * @return {@code true} if the morph animator is running
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isMorphing() {
    return variantMorph.isRunning();
  }

  /**
   * Returns the rail's configured Expanded width.
   *
   * @return the Expanded width in pixels
   * @version v0.3.0
   * @since v0.3.0
   */
  public int getExpandedWidth() {
    return expandedWidthPx;
  }

  /**
   * Sets the rail's Expanded width — the container width used in {@link Variant#EXPANDED} and as
   * the target endpoint of {@link #morphTo(Variant)} transitions. Must lie in {@code [220, 360]} —
   * the M3-tokened range for Expanded rails.
   *
   * @param px the Expanded width in pixels
   * @throws IllegalArgumentException if {@code px} is outside {@code [220, 360]}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setExpandedWidth(final int px) {
    if (px < EXPANDED_WIDTH_MIN_PX || px > EXPANDED_WIDTH_MAX_PX) {
      throw new IllegalArgumentException(
          "expandedWidth must be in ["
              + EXPANDED_WIDTH_MIN_PX
              + ", "
              + EXPANDED_WIDTH_MAX_PX
              + "]: "
              + px);
    }
    if (this.expandedWidthPx == px) {
      return;
    }
    this.expandedWidthPx = px;
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
   * {@code null} to clear. The rail auto-wires the button to toggle its variant on click — clicking
   * morphs Collapsed↔Expanded — and auto-swaps the icon between {@link
   * com.owspfm.elwha.icons.MaterialIcons#menu() menu} (in Collapsed) and {@link
   * com.owspfm.elwha.icons.MaterialIcons#menuOpen() menuOpen} (in Expanded) to match M3 §4.3.
   * Consumers needing different click semantics should pass {@code null} here and host their own
   * icon button outside the rail.
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
      this.menuButton.removeActionListener(menuToggleListener);
      remove(this.menuButton);
    }
    this.menuButton = menu;
    if (menu != null) {
      add(menu);
      menu.addActionListener(menuToggleListener);
      syncMenuButtonGlyph();
    }
    revalidate();
    repaint();
  }

  // Auto-wires the menu button to toggle the rail's variant per M3 §4.3 — hamburger glyph in
  // Collapsed (expand affordance), menuOpen glyph in Expanded (collapse affordance). Consumers
  // that need a different click semantic should slot a different ElwhaIconButton and listen on
  // the rail's PROPERTY_SELECTED / variant change events instead.
  private final java.awt.event.ActionListener menuToggleListener =
      e -> morphTo(variant == Variant.EXPANDED ? Variant.COLLAPSED : Variant.EXPANDED);

  private void syncMenuButtonGlyph() {
    if (menuButton == null) {
      return;
    }
    menuButton.setIcon(
        variant == Variant.EXPANDED
            ? com.owspfm.elwha.icons.MaterialIcons.menuOpen()
            : com.owspfm.elwha.icons.MaterialIcons.menu());
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
      snapFabFormToVariant();
    }
    revalidate();
    repaint();
  }

  private void snapFabFormToVariant() {
    if (fab == null) {
      return;
    }
    final ElwhaFab.Form target =
        variant == Variant.EXPANDED ? ElwhaFab.Form.EXTENDED : ElwhaFab.Form.STANDARD;
    if (fab.getForm() != target) {
      try {
        fab.morphTo(target);
        // Force the FAB to its end-state immediately for snap semantics — morphTo starts the
        // animator, but a setVariant call should not animate. Reaching into MorphAnimator from
        // outside the FAB isn't exposed; the FAB animator runs the 300ms transition; for now we
        // accept that a non-animated setVariant call snaps the rail but the FAB transitions over
        // 300ms. This is consistent with consumer expectation that the FAB carries its own motion
        // semantics independently of the rail's snap vs morph distinction.
      } catch (final IllegalStateException ex) {
        // The FAB was constructed in a form that can't morph to the target (standard-only or
        // extended-only). Leave it as-is — consumer chose those construction-time semantics.
      }
    }
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
   * Sets the rail's primary destinations — the vertical stack of {@link ElwhaNavRailDestination}s
   * that live between the header chrome and the trailing actions. {@code null} is treated as an
   * empty list; otherwise the rail stores a defensive copy.
   *
   * <p>M3 recommends 3–7 primary destinations; the rail logs a {@link Logger#warning(String)} if
   * the list size falls outside that range but does not throw (design doc §3 phrasing).
   *
   * <p>Selection invariant (single-mandatory):
   *
   * <ul>
   *   <li>If the new list is empty, {@link #getSelected()} becomes {@code null}.
   *   <li>If the previously-selected destination is still present in the new list, it remains
   *       selected.
   *   <li>Otherwise the new list's first entry becomes the selected destination by default.
   * </ul>
   *
   * <p>Each destination's {@link
   * ElwhaNavRailDestination#addActionListener(java.awt.event.ActionListener) action listener} chain
   * is augmented with an internal handler that routes clicks to {@link #setSelected}; consumers can
   * still subscribe their own action listeners on a destination for additional side effects, but
   * the rail's container is the single source of truth for which destination is selected.
   *
   * @param destinations the new list of primary destinations, or {@code null} to clear
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setPrimary(final List<ElwhaNavRailDestination> destinations) {
    for (final ElwhaNavRailDestination old : primary) {
      remove(old);
      old.removeActionListener(destinationClickListener);
      uninstallKeyboardNavigation(old);
    }
    primary.clear();
    if (destinations != null) {
      for (final ElwhaNavRailDestination d : destinations) {
        if (d == null) {
          continue;
        }
        primary.add(d);
        add(d);
        d.setHostVariant(variant);
        d.addActionListener(destinationClickListener);
        installKeyboardNavigation(d);
      }
    }
    if (!primary.isEmpty()
        && (primary.size() < M3_PRIMARY_MIN || primary.size() > M3_PRIMARY_MAX)) {
      LOG.warning(
          "ElwhaNavigationRail.setPrimary received "
              + primary.size()
              + " destinations; M3 recommends "
              + M3_PRIMARY_MIN
              + "–"
              + M3_PRIMARY_MAX
              + ". This is advisory only — paint and layout still work.");
    }

    final ElwhaNavRailDestination prior = selected;
    final ElwhaNavRailDestination next;
    if (allDestinations().isEmpty()) {
      next = null;
    } else if (prior != null && allDestinations().contains(prior)) {
      next = prior;
    } else if (!primary.isEmpty()) {
      next = primary.get(0);
    } else {
      next = allDestinations().get(0);
    }
    applySelection(prior, next);

    revalidate();
    repaint();
  }

  /**
   * Adds a section to the Expanded rail — a header label plus its secondary destinations. Sections
   * paint and lay out only when {@link #getVariant()} is {@link Variant#EXPANDED}; in Collapsed
   * they are silently hidden but their destinations remain valid selection-model members.
   *
   * <p>Selection-model union: section destinations are added to the rail's all-destinations set, so
   * {@link #setSelected(ElwhaNavRailDestination)} accepts them. Each secondary destination receives
   * the same click-routing + keyboard-navigation wiring as primary destinations.
   *
   * @param header the section header label; must not be {@code null}
   * @param secondary the section's destinations; copied defensively
   * @throws NullPointerException if {@code header} is null
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addSection(final String header, final List<ElwhaNavRailDestination> secondary) {
    Objects.requireNonNull(header, "header");
    final List<ElwhaNavRailDestination> dests =
        secondary == null ? Collections.emptyList() : new ArrayList<>(secondary);
    dests.removeIf(Objects::isNull);
    final Section section = new Section(header, dests);
    sections.add(section);
    for (final ElwhaNavRailDestination d : dests) {
      add(d);
      d.setHostVariant(variant);
      d.addActionListener(destinationClickListener);
      installKeyboardNavigation(d);
    }
    revalidate();
    repaint();
  }

  /**
   * Removes every section previously added via {@link #addSection(String, List)}, detaching every
   * secondary destination from the rail. If the currently-selected destination was a member of a
   * removed section, selection falls back to the first primary destination (or {@code null} when
   * primary is also empty).
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void clearSections() {
    if (sections.isEmpty()) {
      return;
    }
    final ElwhaNavRailDestination prior = selected;
    for (final Section s : sections) {
      for (final ElwhaNavRailDestination d : s.getDestinations()) {
        remove(d);
        d.removeActionListener(destinationClickListener);
        uninstallKeyboardNavigation(d);
      }
    }
    sections.clear();
    if (prior != null && !primary.contains(prior)) {
      applySelection(prior, primary.isEmpty() ? null : primary.get(0));
    }
    revalidate();
    repaint();
  }

  /**
   * Returns a defensive copy of the current sections. Section instances themselves are immutable.
   *
   * @return the sections list
   * @version v0.3.0
   * @since v0.3.0
   */
  public List<Section> getSections() {
    return new ArrayList<>(sections);
  }

  /** All destinations across primary + every section, in document order. */
  private List<ElwhaNavRailDestination> allDestinations() {
    final List<ElwhaNavRailDestination> out = new ArrayList<>(primary);
    for (final Section s : sections) {
      out.addAll(s.getDestinations());
    }
    return out;
  }

  private void pushHostVariantToDestinations() {
    for (final ElwhaNavRailDestination d : allDestinations()) {
      d.setHostVariant(variant);
    }
  }

  private void broadcastMorphProgress() {
    // The animator's progress runs 0→1 going forward (toward EXPANDED) and 1→0 going reverse
    // (toward COLLAPSED). Destinations interpret progress as "from→to fraction" — always 0 at the
    // start of THIS morph and 1 at the end. Invert when the target is COLLAPSED so reverse morphs
    // and forward morphs share the same 0→1 contract from the destination's perspective.
    final float raw = variantMorph.progress();
    final float p = (morphTo == Variant.EXPANDED) ? raw : (1f - raw);
    for (final ElwhaNavRailDestination d : allDestinations()) {
      d.setMorphProgress(p, morphFrom, morphTo);
    }
    revalidate();
  }

  /**
   * Returns a defensive copy of the current primary destinations list.
   *
   * @return a defensive copy of the primary destinations
   * @version v0.3.0
   * @since v0.3.0
   */
  public List<ElwhaNavRailDestination> getPrimary() {
    return new ArrayList<>(primary);
  }

  /**
   * Returns the currently-selected destination, or {@code null} if the primary list is empty.
   *
   * @return the selected destination, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaNavRailDestination getSelected() {
    return selected;
  }

  /**
   * Selects the given destination. Pass {@code null} only when the primary list is empty
   * (clearing); otherwise the destination must be a member of the current primary list.
   *
   * <p>Same-instance calls are no-ops (clicks on the already-selected destination cause no
   * selection events). On a state change, the previous destination's selected flag is pushed to
   * {@code false}, the new one's to {@code true}, and both a {@link #PROPERTY_SELECTED "selected"}
   * property-change event and a {@link NavRailSelectionListener#selectionChanged} notification are
   * fired in that order.
   *
   * @param destination the destination to select, or {@code null} only when {@link #getPrimary()
   *     primary} is empty
   * @throws IllegalArgumentException if {@code destination} is non-null but not in the current
   *     primary list
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setSelected(final ElwhaNavRailDestination destination) {
    if (destination == null) {
      if (!allDestinations().isEmpty()) {
        throw new IllegalArgumentException(
            "setSelected(null) is only legal when primary and sections are empty");
      }
      applySelection(selected, null);
      return;
    }
    if (!allDestinations().contains(destination)) {
      throw new IllegalArgumentException(
          "destination is not a member of the rail's primary or secondary list");
    }
    if (selected == destination) {
      return;
    }
    applySelection(selected, destination);
  }

  private void applySelection(
      final ElwhaNavRailDestination prior, final ElwhaNavRailDestination next) {
    if (prior == next) {
      return;
    }
    if (prior != null) {
      prior.setSelected(false);
    }
    this.selected = next;
    if (next != null) {
      next.setSelected(true);
    }
    firePropertyChange(PROPERTY_SELECTED, prior, next);
    for (final NavRailSelectionListener l : new ArrayList<>(selectionListeners)) {
      l.selectionChanged(prior, next);
    }
  }

  /**
   * Adds a selection listener. Fires on {@link #setSelected} state changes (not on no-op same-
   * instance calls). The listener is invoked on the same thread that triggered the change —
   * typically the EDT.
   *
   * @param listener the listener to add; {@code null} is ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addSelectionListener(final NavRailSelectionListener listener) {
    if (listener != null) {
      selectionListeners.add(listener);
    }
  }

  /**
   * Removes a previously-added selection listener.
   *
   * @param listener the listener to remove
   * @version v0.3.0
   * @since v0.3.0
   */
  public void removeSelectionListener(final NavRailSelectionListener listener) {
    selectionListeners.remove(listener);
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
    return new Dimension(currentWidthPx(), preferredContentHeight());
  }

  @Override
  public Dimension getMinimumSize() {
    if (isMinimumSizeSet()) {
      return super.getMinimumSize();
    }
    return new Dimension(currentWidthPx(), preferredContentHeight());
  }

  @Override
  public Dimension getMaximumSize() {
    if (isMaximumSizeSet()) {
      return super.getMaximumSize();
    }
    return new Dimension(currentWidthPx(), Integer.MAX_VALUE);
  }

  /**
   * Current container width — driven by variant and (mid-morph) by the morph animator's container-
   * width interpolation. Steady-state in COLLAPSED reports {@link #COLLAPSED_WIDTH_PX}; steady-
   * state in EXPANDED reports {@link #expandedWidthPx}; during a morph reports the lerped value so
   * the parent {@link BorderLayout}-style host relayouts in step.
   */
  int currentWidthPx() {
    if (!variantMorph.isRunning()) {
      return variant == Variant.EXPANDED ? expandedWidthPx : COLLAPSED_WIDTH_PX;
    }
    return ContentMorphPainter.containerWidth(
        COLLAPSED_WIDTH_PX, expandedWidthPx, variantMorph.progress());
  }

  private int preferredContentHeight() {
    int h = CHROME_PAD_PX;
    if (menuButton != null) {
      h += menuButton.getPreferredSize().height + CHROME_GAP_PX;
    }
    if (fab != null) {
      h += fab.getPreferredSize().height + CHROME_GAP_PX;
    }
    h += primaryStackHeight();
    if (variant == Variant.EXPANDED) {
      h += sectionsStackHeight();
    }
    if (!trailingActions.isEmpty()) {
      h += CHROME_GAP_PX;
    }
    h += trailingHeight();
    h += CHROME_PAD_PX;
    return Math.max(h, COLLAPSED_WIDTH_PX);
  }

  private int primaryStackHeight() {
    if (primary.isEmpty()) {
      return 0;
    }
    int h = 0;
    for (final ElwhaNavRailDestination d : primary) {
      h += ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX;
    }
    if (variant == Variant.COLLAPSED) {
      h += DESTINATION_GAP_PX * (primary.size() - 1);
    }
    return h;
  }

  private int sectionsStackHeight() {
    if (sections.isEmpty()) {
      return 0;
    }
    final int headerHeight = sectionHeaderHeight();
    int h = 0;
    for (final Section s : sections) {
      h += SECTION_HEADER_TOP_PAD_PX;
      h += headerHeight;
      h += SECTION_HEADER_BOTTOM_PAD_PX;
      h += s.getDestinations().size() * ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX;
    }
    return h;
  }

  private int sectionHeaderHeight() {
    final Font f = getFont();
    if (f == null) {
      return 14;
    }
    final FontMetrics fm = getFontMetrics(f);
    return fm.getHeight();
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
    // bodyWidth() is the rail's body width — getWidth() minus any trailing-edge shadow reserve
    // a layered-pane host added to its bounds. All centering / row-width math operates on the
    // body, never on the bounds.
    final int w = bodyWidth();
    final int h = getHeight();
    // During a morph in either direction, lay out destinations with the wider Expanded bounds —
    // the destination's own paint lerps the indicator inside those bounds, and Collapsed paint
    // self-clips to the centered 56-wide pill region. Using the wider bounds avoids a bounds-jump
    // mid-morph that would otherwise crop the indicator.
    final boolean expandedish = variant == Variant.EXPANDED || variantMorph.isRunning();

    int topY = CHROME_PAD_PX;
    if (menuButton != null) {
      final Dimension d = menuButton.getPreferredSize();
      final int x = expandedish ? iconColumnAlignedX(d.width) : (w - d.width) / 2;
      menuButton.setBounds(x, topY, d.width, d.height);
      topY += d.height + CHROME_GAP_PX;
    }
    if (fab != null) {
      final Dimension d = fab.getPreferredSize();
      // FAB reserves shadow-blur padding around its painted pill (the pill is inset by the
      // hover-elevation shadow insets within the FAB's bounds). To put the FAB's painted pill
      // leading edge at CHROME_PAD in rail coords (so its internal icon lands on the icon column
      // at CHROME_PAD + LEADING_PAD_EXPANDED, matching destination icons), pull the FAB bounds
      // left by the shadow inset. In Collapsed, the FAB centers horizontally as before — the
      // shadow inset effectively shifts the visible pill a few px left of geometric center, which
      // M3 spec accepts as the price of a halo-reserving FAB.
      final int fabShadowLeft = ShadowPainter.shadowInsets(FAB_HOVER_ELEVATION).left;
      final int x = expandedish ? CHROME_PAD_PX - fabShadowLeft : (w - d.width) / 2;
      fab.setBounds(x, topY, d.width, d.height);
      topY += d.height + CHROME_GAP_PX;
    }

    int destinationsBottom = topY;
    final int rowContentWidth = w - 2 * CHROME_PAD_PX;
    for (final ElwhaNavRailDestination dest : primary) {
      topY = layoutOneDestination(dest, topY, w, rowContentWidth, expandedish);
      destinationsBottom = topY;
      if (variant == Variant.COLLAPSED && !variantMorph.isRunning()) {
        topY += DESTINATION_GAP_PX;
      }
    }

    if (variant == Variant.EXPANDED && !sections.isEmpty()) {
      final int headerH = sectionHeaderHeight();
      for (final Section s : sections) {
        topY += SECTION_HEADER_TOP_PAD_PX;
        topY += headerH;
        topY += SECTION_HEADER_BOTTOM_PAD_PX;
        for (final ElwhaNavRailDestination dest : s.getDestinations()) {
          topY = layoutOneDestination(dest, topY, w, rowContentWidth, true);
          destinationsBottom = topY;
        }
      }
    } else if (variant == Variant.COLLAPSED) {
      // Hide secondary destinations in Collapsed by parking them off-screen — they remain in the
      // selection-model union but neither paint nor accept input. (Setting them invisible would
      // also work; off-screen parking matches the Phase 2 pattern of the rail layout-managing its
      // own children via setBounds.)
      for (final Section s : sections) {
        for (final ElwhaNavRailDestination dest : s.getDestinations()) {
          dest.setBounds(0, -10_000, 0, 0);
        }
      }
    }

    if (trailingActions.isEmpty()) {
      return;
    }

    final int trailingBlock = trailingHeight();
    final int bottomAnchorTop = h - CHROME_PAD_PX - trailingBlock;
    final int safeTop = destinationsBottom + CHROME_GAP_PX;
    int by = Math.max(bottomAnchorTop, safeTop);
    for (final ElwhaIconButton a : trailingActions) {
      final Dimension d = a.getPreferredSize();
      final int ax = expandedish ? iconColumnAlignedX(d.width) : (w - d.width) / 2;
      a.setBounds(ax, by, d.width, d.height);
      by += d.height + TRAILING_ACTION_GAP_PX;
    }
  }

  /**
   * Returns the x where an icon-button-sized component should be placed in Expanded so its internal
   * (centered) glyph lands on the rail's icon column (at {@code CHROME_PAD + LEADING_PAD_EXPANDED}
   * from the rail's leading edge — the same column the destination icons and the FAB's
   * icon-inside-pill sit on).
   */
  private static int iconColumnAlignedX(final int buttonWidth) {
    final int columnX = CHROME_PAD_PX + ElwhaNavRailDestination.LEADING_PAD_EXPANDED;
    return columnX - (buttonWidth - ElwhaNavRailDestination.ICON_SIZE_PX) / 2;
  }

  private int layoutOneDestination(
      final ElwhaNavRailDestination dest,
      final int topY,
      final int railWidth,
      final int rowContentWidth,
      final boolean expandedish) {
    final int rowH = ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX;
    if (expandedish) {
      dest.setRowContentWidth(rowContentWidth);
      dest.setBounds(CHROME_PAD_PX, topY, rowContentWidth, rowH);
    } else {
      dest.setRowContentWidth(0);
      final int cw = ElwhaNavRailDestination.COLLAPSED_WIDTH_PX;
      dest.setBounds((railWidth - cw) / 2, topY, cw, rowH);
    }
    return topY + rowH;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    warnIfMissingAccessibleName();
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int bodyW = bodyWidth();
      final int h = getHeight();

      if (elevation > 0) {
        ShadowPainter.paint(g2, bodyW, h, 0, elevation);
      }

      if (surfaceFilled) {
        g2.setColor(ColorRole.SURFACE_CONTAINER.resolve());
        g2.fillRect(0, 0, bodyW, h);
      }

      if (divider) {
        g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
        final boolean ltr = getComponentOrientation().isLeftToRight();
        final int x = ltr ? bodyW - DIVIDER_WIDTH_PX : 0;
        g2.fillRect(x, 0, DIVIDER_WIDTH_PX, h);
      }

      if (variant == Variant.EXPANDED && !sections.isEmpty()) {
        paintSectionHeaders(g2);
      }
    } finally {
      g2.dispose();
    }
  }

  private void paintSectionHeaders(final Graphics2D g2) {
    final Font font = getFont();
    if (font == null) {
      return;
    }
    g2.setFont(font);
    g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
    final FontMetrics fm = g2.getFontMetrics();
    final int headerH = fm.getHeight();

    int topY = CHROME_PAD_PX;
    if (menuButton != null) {
      topY += menuButton.getPreferredSize().height + CHROME_GAP_PX;
    }
    if (fab != null) {
      topY += fab.getPreferredSize().height + CHROME_GAP_PX;
    }
    topY += primary.size() * ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX;

    for (final Section s : sections) {
      topY += SECTION_HEADER_TOP_PAD_PX;
      final int baseline = topY + fm.getAscent();
      g2.drawString(s.getHeader(), SECTION_HEADER_LEADING_PAD_PX, baseline);
      topY += headerH;
      topY += SECTION_HEADER_BOTTOM_PAD_PX;
      topY += s.getDestinations().size() * ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX;
    }
  }

  // The rail's body width — distinct from getWidth() when an elevation-aware host (e.g. The
  // Elwha Showcase on a JLayeredPane) widens the rail's bounds by the trailing-edge shadow
  // reserve so the ShadowPainter halo can render outside the body silhouette without clipping
  // against the component bounds. Layout-managed hosts (BorderLayout.WEST etc.) won't widen
  // bounds, so bodyWidth() == getWidth() and the trailing halo silently clips on the body's
  // right edge — the documented trade-off in the class Javadoc.
  private int bodyWidth() {
    return getWidth() - trailingShadowReserve();
  }

  /**
   * Returns the trailing-edge bounds reserve a layered-pane host should add to the rail's {@link
   * #setBounds(int, int, int, int) bounds} so the {@link ShadowPainter} halo can render outside the
   * body silhouette without clipping. Equal to {@link ShadowPainter#shadowInsets(int)
   * ShadowPainter.shadowInsets(getElevation()).right} when {@link #getElevation() elevation} {@code
   * > 0}, otherwise zero. A host that mounts the rail at a leading edge should size its bounds to
   * {@code getPreferredSize().width + trailingShadowReserve()} on the trailing side; layout-managed
   * hosts that can't reserve the inset will see the halo silently clip on the body edge.
   *
   * @return the trailing-edge halo reserve in pixels; {@code 0} when {@link #getElevation()
   *     elevation} is {@code 0}
   * @version v0.3.0
   * @since v0.3.0
   */
  public int trailingShadowReserve() {
    return elevation > 0 ? ShadowPainter.shadowInsets(elevation).right : 0;
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

  // ---------------------------------------------------------- keyboard navigation

  private static final String ACTION_FOCUS_NEXT = "elwhaNavRail.focusNext";
  private static final String ACTION_FOCUS_PREV = "elwhaNavRail.focusPrev";
  private static final String ACTION_FOCUS_FIRST = "elwhaNavRail.focusFirst";
  private static final String ACTION_FOCUS_LAST = "elwhaNavRail.focusLast";

  private void installKeyboardNavigation(final ElwhaNavRailDestination d) {
    final InputMap im = d.getInputMap(JComponent.WHEN_FOCUSED);
    final ActionMap am = d.getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ACTION_FOCUS_NEXT);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ACTION_FOCUS_PREV);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), ACTION_FOCUS_FIRST);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), ACTION_FOCUS_LAST);
    am.put(ACTION_FOCUS_NEXT, focusNeighborAction(+1));
    am.put(ACTION_FOCUS_PREV, focusNeighborAction(-1));
    am.put(ACTION_FOCUS_FIRST, focusEdgeAction(true));
    am.put(ACTION_FOCUS_LAST, focusEdgeAction(false));
  }

  private void uninstallKeyboardNavigation(final ElwhaNavRailDestination d) {
    final InputMap im = d.getInputMap(JComponent.WHEN_FOCUSED);
    final ActionMap am = d.getActionMap();
    im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
    im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
    im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
    im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
    am.remove(ACTION_FOCUS_NEXT);
    am.remove(ACTION_FOCUS_PREV);
    am.remove(ACTION_FOCUS_FIRST);
    am.remove(ACTION_FOCUS_LAST);
  }

  private Action focusNeighborAction(final int delta) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (!(e.getSource() instanceof ElwhaNavRailDestination from)) {
          return;
        }
        final List<ElwhaNavRailDestination> traversable = traversableDestinations();
        if (traversable.isEmpty()) {
          return;
        }
        final int fromIndex = traversable.indexOf(from);
        if (fromIndex < 0) {
          return;
        }
        final int size = traversable.size();
        final int target = ((fromIndex + delta) % size + size) % size;
        traversable.get(target).requestFocusInWindow();
      }
    };
  }

  private Action focusEdgeAction(final boolean first) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final List<ElwhaNavRailDestination> traversable = traversableDestinations();
        if (traversable.isEmpty()) {
          return;
        }
        traversable.get(first ? 0 : traversable.size() - 1).requestFocusInWindow();
      }
    };
  }

  /**
   * The flat list of destinations the keyboard navigates over — primary in document order, then
   * each section's secondary destinations in section order. In Collapsed variant the rail hides
   * sections; secondary destinations are excluded from keyboard traversal in that variant since
   * they're not visible.
   */
  private List<ElwhaNavRailDestination> traversableDestinations() {
    if (variant == Variant.COLLAPSED) {
      return new ArrayList<>(primary);
    }
    return allDestinations();
  }

  private ElwhaNavRailDestination currentTabStopDestination() {
    final List<ElwhaNavRailDestination> traversable = traversableDestinations();
    for (final ElwhaNavRailDestination d : traversable) {
      if (d.isFocusOwner()) {
        return d;
      }
    }
    if (selected != null && traversable.contains(selected)) {
      return selected;
    }
    return traversable.isEmpty() ? null : traversable.get(0);
  }

  /**
   * Custom focus traversal policy: Tab visits menu button (if any) → FAB (if any) → exactly one
   * destination (the focused one, falling back to the selected one, then the first) → trailing
   * actions (in order). Other destinations are excluded from Tab order; arrow keys (↑/↓/Home/End)
   * move focus within the destination band.
   */
  private final class RailFocusTraversalPolicy extends FocusTraversalPolicy {

    @Override
    public Component getComponentAfter(final Container root, final Component current) {
      final List<Component> order = tabOrder();
      final int i = order.indexOf(current);
      if (i < 0 || order.isEmpty()) {
        return order.isEmpty() ? null : order.get(0);
      }
      if (i + 1 >= order.size()) {
        return null;
      }
      return order.get(i + 1);
    }

    @Override
    public Component getComponentBefore(final Container root, final Component current) {
      final List<Component> order = tabOrder();
      final int i = order.indexOf(current);
      if (i <= 0) {
        return null;
      }
      return order.get(i - 1);
    }

    @Override
    public Component getFirstComponent(final Container root) {
      final List<Component> order = tabOrder();
      return order.isEmpty() ? null : order.get(0);
    }

    @Override
    public Component getLastComponent(final Container root) {
      final List<Component> order = tabOrder();
      return order.isEmpty() ? null : order.get(order.size() - 1);
    }

    @Override
    public Component getDefaultComponent(final Container root) {
      return getFirstComponent(root);
    }

    private List<Component> tabOrder() {
      final List<Component> out = new ArrayList<>();
      if (menuButton != null) {
        out.add(menuButton);
      }
      if (fab != null) {
        out.add(fab);
      }
      final ElwhaNavRailDestination tabStop = currentTabStopDestination();
      if (tabStop != null) {
        out.add(tabStop);
      }
      out.addAll(trailingActions);
      return out;
    }
  }
}
