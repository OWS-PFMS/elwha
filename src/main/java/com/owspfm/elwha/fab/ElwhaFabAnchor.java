package com.owspfm.elwha.fab;

import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShadowPainter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.BoundedRangeModel;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

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
 * <p><strong>Scroll-aware behavior.</strong> Opt in via {@link #setScrollResponse(ScrollResponse)}
 * with a scroll source ({@link #setScrollSource(JScrollPane)}, auto-defaulted to {@code content}
 * when it is a {@link JScrollPane}). {@link ScrollResponse#HIDE} slides the FAB off the anchored
 * edge on scroll-down and back on scroll-up (G14b); {@link ScrollResponse#SHRINK} morphs Extended ↔
 * Standard on scroll-down / scroll-up (G33 / G34), reusing {@link ElwhaFab#morphTo(ElwhaFab.Form)}.
 * Both honor the global reduced-motion flag (the animation snaps). Default is {@link
 * ScrollResponse#NONE} — pure static placement.
 *
 * <p><strong>Scope.</strong> A full M3 Scaffold with named slots, adjacent-UI gaps (20 / 28 dp,
 * §4.4), and multi-FAB placement (FAB Menu, #185) are out of scope.
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

  /** How a floating FAB reacts to its scroll source scrolling (M3 G14b / G33 / G34). */
  public enum ScrollResponse {
    /** No reaction — pure static placement (default). */
    NONE,
    /** Slide off the anchored edge on scroll-down, slide back on scroll-up (G14b). */
    HIDE,
    /** Morph Extended → Standard on scroll-down, Standard → Extended on scroll-up (G33 / G34). */
    SHRINK
  }

  /** M3 screen-edge inset for a floating FAB (placement diagram, design doc §4.4), in dp. */
  public static final int DEFAULT_INSET_DP = 16;

  // Scroll deltas below this (px) are treated as jitter and ignored for direction detection.
  private static final int SCROLL_THRESHOLD_PX = 2;

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

  private ScrollResponse scrollResponse = ScrollResponse.NONE;
  private JScrollPane scrollSource;
  private final MorphAnimator hideAnim;
  private final ChangeListener scrollListener = e -> onScroll();
  private BoundedRangeModel attachedModel;
  private int prevScrollValue;
  private boolean hidden;
  private boolean shrunk;
  private Timer shrinkTracker;

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
    this.hideAnim = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);
    this.hideAnim.addProgressListener(
        () -> {
          revalidate();
          repaint();
        });
    if (content instanceof JScrollPane sp) {
      this.scrollSource = sp;
    }
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

  /**
   * Returns the scroll response.
   *
   * @return the scroll response
   * @version v0.3.0
   * @since v0.3.0
   */
  public ScrollResponse getScrollResponse() {
    return scrollResponse;
  }

  /**
   * Sets how the FAB reacts to its {@linkplain #setScrollSource(JScrollPane) scroll source}
   * scrolling. With no resolvable source the response is inert. Switching response restores the FAB
   * to its shown / Extended baseline first.
   *
   * <p>{@link ScrollResponse#SHRINK} requires a FAB built via {@link ElwhaFab#extended(javax.swing
   * .Icon, String)} — it must carry both morph endpoints; otherwise the first scroll-down's {@link
   * ElwhaFab#morphTo(ElwhaFab.Form)} throws {@link IllegalStateException}.
   *
   * @param response the scroll response
   * @throws NullPointerException if {@code response} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setScrollResponse(final ScrollResponse response) {
    Objects.requireNonNull(response, "response");
    if (response == scrollResponse) {
      return;
    }
    resetToBaseline();
    detachScrollListener();
    scrollResponse = response;
    if (response != ScrollResponse.NONE) {
      attachScrollListener();
    }
    revalidate();
    repaint();
  }

  /**
   * Returns the scroll source driving the {@linkplain #getScrollResponse() scroll response}, or
   * {@code null} if none.
   *
   * @return the scroll source, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public JScrollPane getScrollSource() {
    return scrollSource;
  }

  /**
   * Sets the scroll source whose vertical scrolling drives the {@linkplain #getScrollResponse()
   * scroll response}. Defaults to the content component when it is a {@link JScrollPane}; set
   * explicitly for a nested scroll pane. Pass {@code null} to clear.
   *
   * @param source the scroll source, or {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public void setScrollSource(final JScrollPane source) {
    if (source == scrollSource) {
      return;
    }
    detachScrollListener();
    scrollSource = source;
    if (scrollResponse != ScrollResponse.NONE) {
      attachScrollListener();
    }
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

    int hideOffset = 0;
    if (scrollResponse == ScrollResponse.HIDE && hideAnim.progress() > 0f) {
      final int slide = pref.height + insetDp + halo.bottom;
      final int magnitude = Math.round(hideAnim.progress() * slide);
      hideOffset = fabOnBottom ? magnitude : -magnitude;
    }
    fab.setBounds(x, y + hideOffset, pref.width, pref.height);
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

  private void attachScrollListener() {
    if (scrollSource == null || attachedModel != null) {
      return;
    }
    attachedModel = scrollSource.getVerticalScrollBar().getModel();
    prevScrollValue = attachedModel.getValue();
    attachedModel.addChangeListener(scrollListener);
  }

  private void detachScrollListener() {
    if (attachedModel != null) {
      attachedModel.removeChangeListener(scrollListener);
      attachedModel = null;
    }
  }

  private void onScroll() {
    if (attachedModel == null) {
      return;
    }
    final int value = attachedModel.getValue();
    final int delta = value - prevScrollValue;
    prevScrollValue = value;
    if (Math.abs(delta) < SCROLL_THRESHOLD_PX) {
      return;
    }
    final boolean down = delta > 0;
    if (scrollResponse == ScrollResponse.HIDE) {
      if (down && !hidden) {
        hidden = true;
        hideAnim.start();
      } else if (!down && hidden) {
        hidden = false;
        hideAnim.reverse();
      }
    } else if (scrollResponse == ScrollResponse.SHRINK) {
      if (down && !shrunk) {
        shrunk = true;
        fab.morphTo(ElwhaFab.Form.STANDARD);
        startShrinkTracker();
      } else if (!down && shrunk) {
        shrunk = false;
        fab.morphTo(ElwhaFab.Form.EXTENDED);
        startShrinkTracker();
      }
    }
  }

  // The FAB's preferred width interpolates each tick during the Standard↔Extended morph, but it is
  // absolutely positioned in this pane, so the parent never auto-relayouts — poll isMorphing() and
  // re-pin until the morph settles (mirrors ElwhaShowcase.mountRailOnLayeredPane). The leading
  // revalidate() covers the reduced-motion snap, where the morph completes before the first tick.
  private void startShrinkTracker() {
    if (shrinkTracker == null) {
      shrinkTracker =
          new Timer(
              16,
              e -> {
                revalidate();
                if (!fab.isMorphing()) {
                  ((Timer) e.getSource()).stop();
                }
              });
      shrinkTracker.setCoalesce(true);
    }
    revalidate();
    shrinkTracker.restart();
  }

  private void resetToBaseline() {
    if (hidden) {
      hidden = false;
      hideAnim.reverse();
    }
    if (shrunk) {
      shrunk = false;
      fab.morphTo(ElwhaFab.Form.EXTENDED);
      startShrinkTracker();
    }
  }
}
