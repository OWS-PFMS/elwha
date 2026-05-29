package com.owspfm.elwha.fab;

import com.owspfm.elwha.theme.ShadowPainter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.JLayeredPane;

/**
 * Placement primitive that floats an {@link ElwhaFab} above a content component at a screen-edge
 * corner, per the M3 floating-action-button placement pattern. Absorbs the four-piece glue the FAB
 * design doc §15 documents as a consumer recipe — z-order, corner positioning, resize-pinning, and
 * RTL mirroring — into a single reusable container, the structural answer tracked on <a
 * href="https://github.com/OWS-PFMS/elwha/issues/205">#205</a>.
 *
 * <p><strong>Composition.</strong> The anchor <em>is</em> a {@link JLayeredPane}. The content sits
 * on {@link JLayeredPane#DEFAULT_LAYER} filling the whole pane; the FAB sits on {@link
 * JLayeredPane#PALETTE_LAYER}, so it paints above whatever the content does — scrolling, repaint,
 * tab swap. Drop the anchor in wherever a single component is expected (commonly {@code
 * frame.setContentPane(anchor)}).
 *
 * <p><strong>Positioning.</strong> An opinionated {@link #doLayout()} re-pins the FAB on every
 * resize, so no consumer {@code ComponentListener} is needed. The FAB is placed at the configured
 * {@link Corner} ({@link Corner#BOTTOM_TRAILING} by default) with a configurable inset (M3 default
 * 16 dp). {@link Corner#BOTTOM_TRAILING} / {@link Corner#TOP_TRAILING} resolve against {@link
 * #getComponentOrientation()} — they pin to the right edge in LTR and mirror to the left edge in
 * RTL automatically; a {@code componentOrientation} change triggers a re-layout.
 *
 * <p><strong>Spec-correct margin.</strong> The inset is measured to the FAB's <em>visible body</em>
 * edge, not its bounds. {@link ElwhaFab#getPreferredSize()} reserves a shadow-halo border around
 * the body; the anchor compensates for that halo (via {@link ShadowPainter#shadowInsets(int)}) so
 * the body sits exactly {@code inset} dp from the edge. The naive §15 recipe pins the
 * halo-inclusive bounds at the inset and so leaves the body at {@code inset + halo} — this
 * primitive corrects it.
 *
 * <p><strong>Positions, never sizes.</strong> The anchor reads {@link ElwhaFab#getPreferredSize()}
 * and never sets the FAB's size or form — size-per-window-class stays the consumer's call (FAB
 * design doc G1 / G4 / G29 / G30), and {@link ElwhaFab} itself is untouched (placement is DOCS /
 * out-of-scope for the component per G2 / G10 / G26 / §4.4).
 *
 * <p><strong>One placement owner per FAB.</strong> A FAB has exactly one placement owner. Container
 * components that own a FAB slot themselves (the {@code ElwhaNavigationRail} header FAB, which the
 * rail lays out and whose form it drives) keep that FAB <em>out</em> of this anchor — don't anchor
 * a slotted FAB, or the morph driver would be ambiguous. M3's one-FAB-per-screen guidance (G19)
 * makes this a non-issue in practice.
 *
 * <p><strong>Scope.</strong> Static placement only. Scroll-aware hide / shrink behavior (G14b / G33
 * / G34) is Phase 2. A full M3 Scaffold with named slots, adjacent-UI gaps (20 / 28 dp, §4.4), and
 * multi-FAB placement (FAB Menu, #185) are out of scope.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabAnchor extends JLayeredPane {

  /** The corner of the content region a floating FAB anchors to. */
  public enum Corner {
    /** Bottom-right in LTR, bottom-left in RTL — the M3 default. */
    BOTTOM_TRAILING,
    /** Bottom-left in LTR, bottom-right in RTL. */
    BOTTOM_LEADING,
    /** Top-right in LTR, top-left in RTL. */
    TOP_TRAILING,
    /** Top-left in LTR, top-right in RTL. */
    TOP_LEADING
  }

  /** M3 screen-edge inset for a floating FAB (placement diagram, design doc §4.4), in dp. */
  public static final int DEFAULT_INSET_DP = 16;

  // Mirrors ElwhaFab.HOVER_ELEVATION (private). ElwhaFab.getPreferredSize() reserves the
  // hover-elevation halo unconditionally, so the same elevation yields the halo to back out of the
  // bounds when pinning the visible body to the spec margin. The ElwhaNavigationRail header FAB
  // duplicates this same constant for the same reason; a shared read-only accessor on ElwhaFab
  // would
  // retire both copies (follow-up).
  private static final int FAB_HOVER_ELEVATION = 4;

  private final Component content;
  private final ElwhaFab fab;
  private Corner corner = Corner.BOTTOM_TRAILING;
  private int insetDp = DEFAULT_INSET_DP;

  private final PropertyChangeListener orientationListener = e -> revalidate();

  /**
   * Creates an anchor floating {@code fab} above {@code content} at {@link Corner#BOTTOM_TRAILING}
   * with the M3 default 16 dp inset.
   *
   * @param content the component the FAB floats above; fills the anchor
   * @param fab the floating action button
   * @throws NullPointerException if either argument is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFabAnchor(final Component content, final ElwhaFab fab) {
    this.content = Objects.requireNonNull(content, "content");
    this.fab = Objects.requireNonNull(fab, "fab");
    add(content, JLayeredPane.DEFAULT_LAYER);
    add(fab, JLayeredPane.PALETTE_LAYER);
    addPropertyChangeListener("componentOrientation", orientationListener);
  }

  /**
   * Returns the content component this anchor floats the FAB above.
   *
   * @return the content component
   * @version v0.3.0
   * @since v0.3.0
   */
  public Component getContent() {
    return content;
  }

  /**
   * Returns the floating action button.
   *
   * @return the FAB
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaFab getFab() {
    return fab;
  }

  /**
   * Returns the corner the FAB anchors to.
   *
   * @return the anchor corner
   * @version v0.3.0
   * @since v0.3.0
   */
  public Corner getCorner() {
    return corner;
  }

  /**
   * Sets the corner the FAB anchors to and re-pins it.
   *
   * @param corner the anchor corner
   * @throws NullPointerException if {@code corner} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setCorner(final Corner corner) {
    this.corner = Objects.requireNonNull(corner, "corner");
    revalidate();
    repaint();
  }

  /**
   * Returns the screen-edge inset (FAB visible body to edge) in dp.
   *
   * @return the inset in dp
   * @version v0.3.0
   * @since v0.3.0
   */
  public int getInsetDp() {
    return insetDp;
  }

  /**
   * Sets the screen-edge inset (FAB visible body to edge) in dp and re-pins the FAB.
   *
   * @param insetDp the inset in dp; must be {@code >= 0}
   * @throws IllegalArgumentException if {@code insetDp} is negative
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setInsetDp(final int insetDp) {
    if (insetDp < 0) {
      throw new IllegalArgumentException("insetDp must be >= 0: " + insetDp);
    }
    this.insetDp = insetDp;
    revalidate();
    repaint();
  }

  @Override
  public void doLayout() {
    final int w = getWidth();
    final int h = getHeight();
    content.setBounds(0, 0, w, h);

    final Dimension pref = fab.getPreferredSize();
    final Insets halo = ShadowPainter.shadowInsets(FAB_HOVER_ELEVATION);
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final boolean trailing = corner == Corner.BOTTOM_TRAILING || corner == Corner.TOP_TRAILING;
    final boolean fabOnRight = trailing == ltr;
    final boolean fabOnBottom = corner == Corner.BOTTOM_TRAILING || corner == Corner.BOTTOM_LEADING;

    final int x = fabOnRight ? w - pref.width - insetDp + halo.right : insetDp - halo.left;
    final int y = fabOnBottom ? h - pref.height - insetDp + halo.bottom : insetDp - halo.top;
    fab.setBounds(x, y, pref.width, pref.height);
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final Dimension d = content.getPreferredSize();
    final Insets in = getInsets();
    return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
  }

  @Override
  public Dimension getMinimumSize() {
    if (isMinimumSizeSet()) {
      return super.getMinimumSize();
    }
    return getPreferredSize();
  }
}
