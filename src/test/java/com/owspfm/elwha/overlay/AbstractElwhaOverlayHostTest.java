package com.owspfm.elwha.overlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the shared overlay host — the mount/teardown lifecycle, the strategy defaults
 * every subclass either inherits or overrides ({@code overlayLayer} / {@code lightDismiss} / {@code
 * takesFocus} / {@code restoreFocusOnClose}), the interactive motion scrub, and the epic&nbsp;#322
 * overlay <em>chain</em> bookkeeping: linking, root/leaf navigation, the ancestor teardown cascade,
 * and the self-close notification that leaves a parent standing.
 *
 * <p>The host mounts on a {@link HeadlessHost} root pane, so everything asserted here is reachable
 * with no realized window. What is deliberately <em>not</em> here: anchored placement (the anchor
 * is never {@code isShowing()} without a window — the placement engines' pure functions carry that)
 * and real focus arbitration, which belongs to the {@code gui} tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class AbstractElwhaOverlayHostTest {

  private static final List<Probe> OPENED = new ArrayList<>();

  private HeadlessHost host;
  private Probe overlay;

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(600, 400);
    overlay = newProbe("root");
  }

  @AfterEach
  void closeEveryOverlay() {
    for (final Probe open : OPENED) {
      if (open.isShowing()) {
        open.beginClose();
      }
    }
    OPENED.clear();
  }

  private static Probe newProbe(final String name) {
    final Probe probe = new Probe(name);
    OPENED.add(probe);
    return probe;
  }

  // -------------------------------------------------------- strategy defaults

  @Test
  void defaultsAreDialogFlavored() {
    assertThat(overlay.overlayLayer())
        .as("an overlay defaults to the dialog z-band")
        .isEqualTo(JLayeredPane.MODAL_LAYER);
    assertThat(overlay.lightDismiss()).as("and to modal dismissal, not light dismiss").isFalse();
    assertThat(overlay.takesFocus()).as("and takes keyboard focus while shown").isTrue();
    assertThat(overlay.restoreFocusOnClose())
        .as("and hands focus back to the opener on close")
        .isTrue();
  }

  // ------------------------------------------------------------ show contract

  @Test
  void showRejectsANullParent() {
    assertThatNullPointerException().isThrownBy(() -> overlay.show(null));
  }

  @Test
  void showRejectsAParentWithNoRootPane() {
    assertThatIllegalStateException()
        .isThrownBy(() -> overlay.show(new JPanel()))
        .withMessageContaining("realized window");
  }

  @Test
  void showMountsTheSurfaceOnTheChosenZBand() {
    overlay.layer = JLayeredPane.POPUP_LAYER;

    overlay.show(host.anchor());

    assertThat(host.mounted()).as("the surface is attached to the layered pane").hasSize(1);
    assertThat(host.layerOf(overlay.surface))
        .as("on the z-band the subclass chose")
        .isEqualTo(JLayeredPane.POPUP_LAYER);
    assertThat(overlay.isShowing()).as("and the host reports itself shown").isTrue();
  }

  @Test
  void aBackdropIsMountedBeneathTheSurfaceAndStretchedOverTheWholePane() {
    overlay.wantsBackdrop = true;

    overlay.show(host.anchor());

    assertThat(overlay.backdrop).as("the subclass backdrop is created").isNotNull();
    assertThat(host.mounted()).as("backdrop and surface are both mounted").hasSize(2);
    assertThat(host.mounted().indexOf(overlay.surface))
        .as("the surface is moved to the front, so it paints over the backdrop")
        .isLessThan(host.mounted().indexOf(overlay.backdrop));
    assertThat(overlay.backdrop.getBounds())
        .as("the backdrop covers the full layered pane")
        .isEqualTo(new Rectangle(0, 0, 600, 400));
  }

  @Test
  void showAppliesTheAccessibleNameAndOrientationToTheSurface() {
    host.anchor().applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    overlay.show(host.anchor());

    assertThat(overlay.surface.getAccessibleContext().getAccessibleName())
        .as("the subclass accessible name lands on the surface")
        .isEqualTo("root");
    assertThat(overlay.surface.getComponentOrientation().isLeftToRight())
        .as("and the anchor's orientation is applied for RTL mirroring")
        .isFalse();
  }

  @Test
  void aSecondShowWhileMountedIsANoOpRatherThanAStrandedSurface() {
    overlay.show(host.anchor());
    final JComponent first = overlay.surface;

    overlay.show(host.anchor());

    assertThat(overlay.surface).as("the live surface is not replaced").isSameAs(first);
    assertThat(host.mounted()).as("and no second surface is stranded on the pane").hasSize(1);
    assertThat(overlay.surfacesCreated).as("createSurface ran exactly once").isEqualTo(1);
  }

  // ------------------------------------------------------------ relayout

  @Test
  void relayoutRestretchesTheBackdropAndReplacesTheSurface() {
    overlay.wantsBackdrop = true;
    overlay.show(host.anchor());

    host.resize(800, 500);
    overlay.relayout();

    assertThat(overlay.backdrop.getBounds())
        .as("the backdrop is re-stretched over the resized pane")
        .isEqualTo(new Rectangle(0, 0, 800, 500));
    assertThat(overlay.lastPaneSize)
        .as("and the subclass places its surface against the new viewport")
        .isEqualTo(new Dimension(800, 500));
  }

  @Test
  void hostResizesAreWatchedOnlyWhileShown() {
    final int quiet = host.layeredPane().getComponentListeners().length;

    overlay.show(host.anchor());
    assertThat(host.layeredPane().getComponentListeners().length)
        .as("a shown overlay listens for host resizes so it can re-place itself")
        .isGreaterThan(quiet);

    overlay.beginClose();
    assertThat(host.layeredPane().getComponentListeners().length)
        .as("and stops listening once it is torn down")
        .isEqualTo(quiet);
  }

  // --------------------------------------------------------- close + teardown

  @Test
  void closingDetachesEverythingAndReportsTheOutcome() {
    overlay.wantsBackdrop = true;
    overlay.show(host.anchor());

    overlay.beginClose();

    assertThat(host.mounted()).as("both surface and backdrop are detached").isEmpty();
    assertThat(overlay.isShowing()).as("and the host no longer reports itself shown").isFalse();
    assertThat(overlay.closedCount).as("onClosed fires once after teardown").isEqualTo(1);
    assertThat(overlay.transientCleared)
        .as("and the subclass clears its own live state")
        .isEqualTo(1);
  }

  @Test
  void closingAnOverlayThatWasNeverShownDoesNothing() {
    assertThatCode(() -> overlay.beginClose()).doesNotThrowAnyException();
    assertThat(overlay.closedCount)
        .as("no close is reported for an overlay that never opened")
        .isZero();
  }

  @Test
  void closingTwiceReportsTheOutcomeOnlyOnce() {
    overlay.show(host.anchor());

    overlay.beginClose();
    overlay.beginClose();

    assertThat(overlay.closedCount)
        .as("the re-entry guard collapses a double dismiss")
        .isEqualTo(1);
  }

  @Test
  void aDismissBeforeTheFirstMotionTickStillTearsDown() {
    overlay.show(host.anchor());
    // Reproduces the wedge the direct-teardown branch exists for: closing while the entrance is
    // still at 0 makes reverse() a no-op, so no tick would ever fire and the overlay would hang.
    overlay.entrance.snapTo(0f);

    overlay.beginClose();

    assertThat(overlay.isShowing())
        .as("an overlay dismissed at zero progress still tears down")
        .isFalse();
    assertThat(overlay.closedCount).as("and reports its outcome").isEqualTo(1);
  }

  // ------------------------------------------------------------ motion scrub

  @Test
  void scrubbingSetsProgressLinearlyAndClampsToTheUnitRange() {
    overlay.show(host.anchor());

    overlay.scrubMotion(0.4f);
    assertThat(overlay.motionProgress)
        .as("a scrub drives progress 1:1 with the gesture, unlike the eased ticks")
        .isEqualTo(0.4f);

    overlay.scrubMotion(-2f);
    assertThat(overlay.motionProgress).as("under-range scrubs clamp to fully hidden").isZero();

    overlay.scrubMotion(9f);
    assertThat(overlay.motionProgress).as("over-range scrubs clamp to fully shown").isEqualTo(1f);
  }

  @Test
  void scrubbingSyncsTheAnimatorSoASettleContinuesFromTheScrubbedPosition() {
    overlay.show(host.anchor());

    overlay.scrubMotion(0.3f);

    assertThat(overlay.entrance.progress())
        .as("the animator tracks the scrub, so a release resumes rather than snapping")
        .isEqualTo(0.3f);

    overlay.settleMotion();

    assertThat(overlay.motionProgress)
        .as("settling returns the overlay to fully shown")
        .isCloseTo(1f, within(0.001f));
  }

  @Test
  void scrubbingAndSettlingAreInertWhenNotShown() {
    assertThatCode(
            () -> {
              overlay.scrubMotion(0.5f);
              overlay.settleMotion();
            })
        .doesNotThrowAnyException();
  }

  // ----------------------------------------------------------- overlay chain

  @Test
  void aChainlessOverlayIsItsOwnRootAndHasNoLinks() {
    overlay.show(host.anchor());

    assertThat(overlay.chainParentOverlay()).as("a root overlay has no chain parent").isNull();
    assertThat(overlay.chainChildOverlay()).as("and no child until one opens above it").isNull();
    assertThat(overlay.chainRootOverlay()).as("it is its own chain root").isSameAs(overlay);
    assertThat(overlay.chainOverlays()).as("and its chain is just itself").containsExactly(overlay);
  }

  @Test
  void showingInChainLinksBothDirectionsWithoutClosingTheParent() {
    final Probe child = newProbe("child");
    overlay.show(host.anchor());

    child.showInChain(host.anchor(), overlay);

    assertThat(overlay.isShowing()).as("opening a child leaves the parent up").isTrue();
    assertThat(child.chainParentOverlay()).as("the child knows its parent").isSameAs(overlay);
    assertThat(overlay.chainChildOverlay()).as("and the parent knows its child").isSameAs(child);
    assertThat(child.chainRootOverlay())
        .as("the chain root walks up from the leaf")
        .isSameAs(overlay);
    assertThat(child.chainOverlays())
        .as("and the chain lists root first, leaf last")
        .containsExactly(overlay, child);
  }

  @Test
  void showingInChainRejectsANullParent() {
    final Probe child = newProbe("child");
    assertThatNullPointerException().isThrownBy(() -> child.showInChain(host.anchor(), null));
  }

  @Test
  void tearingDownAnAncestorCascadesToEveryLevelAboveIt() {
    final Probe child = newProbe("child");
    final Probe grandchild = newProbe("grandchild");
    overlay.show(host.anchor());
    child.showInChain(host.anchor(), overlay);
    grandchild.showInChain(host.anchor(), child);

    overlay.beginClose();

    assertThat(child.isShowing()).as("closing an ancestor closes its child").isFalse();
    assertThat(grandchild.isShowing()).as("and cascades through the whole chain").isFalse();
    assertThat(host.mounted()).as("nothing is left mounted on the pane").isEmpty();
    assertThat(overlay.childClosedCount)
        .as("a cascade is not a self-close, so the parent is never notified")
        .isZero();
  }

  @Test
  void aChildClosingOnItsOwnLeavesTheParentUpAndNotifiesIt() {
    final Probe child = newProbe("child");
    overlay.show(host.anchor());
    child.showInChain(host.anchor(), overlay);

    child.beginClose();

    assertThat(child.isShowing()).as("the child level collapses").isFalse();
    assertThat(overlay.isShowing()).as("while its parent stays open").isTrue();
    assertThat(overlay.chainChildOverlay()).as("and the parent's child link is cleared").isNull();
    assertThat(overlay.childClosedCount)
        .as("the parent is told, so it can re-arm focus on the opener item")
        .isEqualTo(1);
  }

  @Test
  void aChainPointerTestIsSafeOnAnOverlayThatIsNotShown() {
    assertThat(overlay.chainContainsScreenPoint(new Point(10, 10)))
        .as("the hover-intent test degrades to false when nothing is mounted")
        .isFalse();
  }

  // ------------------------------------------------------- focus-target search

  @Test
  void firstFocusableReportsNothingWhenThereIsNoCandidate() {
    assertThat(AbstractElwhaOverlay.firstFocusable(new JPanel()))
        .as("an empty container offers no focus target")
        .isNull();
  }

  @Test
  void firstFocusableRefusesAnUnrealizedTree() {
    final JPanel outer = new JPanel();
    final JPanel nested = new JPanel();
    nested.add(new JButton("go"));
    outer.add(nested);

    assertThat(AbstractElwhaOverlay.firstFocusable(outer))
        .as("a focus target must be displayable, so an unmounted tree offers none")
        .isNull();
  }

  // ---------------------------------------------------------------- ownership

  @Test
  void ownershipDefaultsToDescendantsOfTheSurface() {
    overlay.show(host.anchor());
    final JButton inside = new JButton("in");
    overlay.surface.add(inside);

    assertThat(overlay.ownsFocus(inside))
        .as("a surface descendant belongs to the overlay")
        .isTrue();
    assertThat(overlay.ownsFocus(host.anchor()))
        .as("while the anchor outside it does not, by default")
        .isFalse();
  }

  /** A configurable {@link AbstractElwhaOverlay} standing in for the real subclasses. */
  private static final class Probe extends AbstractElwhaOverlay {

    private final String name;

    private Integer layer = JLayeredPane.MODAL_LAYER;
    private boolean wantsBackdrop;

    private int surfacesCreated;
    private int closedCount;
    private int childClosedCount;
    private int transientCleared;
    private Dimension lastPaneSize = new Dimension();

    Probe(final String name) {
      this.name = name;
    }

    @Override
    protected JComponent createSurface() {
      surfacesCreated++;
      final JPanel panel = new JPanel();
      panel.setFocusable(true);
      panel.setPreferredSize(new Dimension(160, 96));
      return panel;
    }

    @Override
    protected JComponent createBackdrop() {
      return wantsBackdrop ? new JPanel() : null;
    }

    @Override
    protected Integer overlayLayer() {
      return layer;
    }

    @Override
    protected void layoutSurface(final int paneWidth, final int paneHeight) {
      lastPaneSize = new Dimension(paneWidth, paneHeight);
      surface.setBounds(0, 0, surface.getPreferredSize().width, surface.getPreferredSize().height);
    }

    @Override
    protected String accessibleName() {
      return name;
    }

    @Override
    protected void clearTransientState() {
      transientCleared++;
    }

    @Override
    protected void onClosed() {
      closedCount++;
    }

    @Override
    protected void onChainChildClosed() {
      childClosedCount++;
    }
  }
}
