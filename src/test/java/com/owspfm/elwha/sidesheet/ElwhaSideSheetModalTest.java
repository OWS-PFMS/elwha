package com.owspfm.elwha.sidesheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintOrigin;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ElwhaLayers;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the side sheet's <em>modal presentation</em> and its two drag gestures: the
 * z-band and docking geometry the host pins, each dismiss route and the cause it reports, the live
 * honoring of the Esc and scrim toggles (unlike the dialog's build-time snapshot), and the
 * gesture-release threshold that decides between dismissing and settling back.
 *
 * <p>Mid-drag <em>motion</em> is observable here only on a standard sheet, whose scrub moves the
 * preferred width the host layout reads. A modal drag scrubs a paint-time translation instead, so
 * its intermediate frames are a {@code gui}-tier concern; what is asserted here is the release
 * verdict, which is where the contract lives.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSideSheetModalTest {

  private static final int PANE_W = 900;
  private static final int PANE_H = 600;

  private HeadlessHost host;
  private final List<ElwhaSideSheet> presented = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(PANE_W, PANE_H);
  }

  @AfterEach
  void dismissEverySheet() {
    for (final ElwhaSideSheet sheet : presented) {
      sheet.dismiss();
    }
    presented.clear();
  }

  private ElwhaSideSheet present(final ElwhaSideSheet sheet) {
    presented.add(sheet);
    sheet.showModal(host.anchor());
    return sheet;
  }

  private static ElwhaSideSheet sheet() {
    return ElwhaSideSheet.modalSheet("Filters");
  }

  /** The mounted slide surface — the one the sheet itself lives inside. */
  private Component surface(final ElwhaSideSheet sheet) {
    return host.mounted().stream()
        .filter(
            c ->
                c instanceof java.awt.Container container
                    && SwingUtilities.isDescendingFrom(sheet, container))
        .findFirst()
        .orElseThrow();
  }

  /** The mounted scrim — the backdrop that is not the surface. */
  private Component scrim(final ElwhaSideSheet sheet) {
    final Component slide = surface(sheet);
    return host.mounted().stream().filter(c -> c != slide).findFirst().orElseThrow();
  }

  // ------------------------------------------------------------ paint origin

  @Test
  void aSurfaceIsAPaintingOriginOnlyWhileTheEntranceIsStillRunning() {
    MorphAnimator.setReducedMotion(false);
    final ElwhaSideSheet sheet = present(sheet());
    // Re-pinned before asserting: motionProgress is already latched at 0 (a Swing timer cannot tick
    // while this test holds the dispatch thread), and the restore keeps the teardown synchronous.
    MorphAnimator.setReducedMotion(true);

    assertThat(PaintOrigin.of((JComponent) surface(sheet)))
        .as("mid-slide the surface translates a snapshot, so children must repaint through it")
        .isTrue();
  }

  @Test
  void aSettledSurfaceIsNotAPaintingOrigin() {
    final ElwhaSideSheet sheet = present(sheet());

    assertThat(PaintOrigin.of((JComponent) surface(sheet)))
        .as("at rest paint() translates nothing, so a caret blink need not re-composite the sheet")
        .isFalse();
  }

  // ------------------------------------------------------------ presentation

  @Test
  void presentingMountsAScrimAndASurfaceOnTheOverlayBand() {
    final ElwhaSideSheet sheet = present(sheet());

    assertThat(sheet.isModalShowing()).isTrue();
    assertThat(host.mounted()).hasSize(2);
    for (final Component mounted : host.mounted()) {
      assertThat(host.layerOf(mounted))
          .as("a sheet sits below dialogs and menus, so either can open above it")
          .isEqualTo(ElwhaLayers.OVERLAY_LAYER.intValue());
    }
  }

  @Test
  void presentingAStandardSheetForcesTheModalChrome() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");

    present(sheet);

    assertThat(sheet.getSheetType())
        .as("the chrome contract is type-derived, so presenting modally switches the type")
        .isEqualTo(SheetType.MODAL);
  }

  @Test
  void presentingAClosedSheetSnapsItOpen() {
    final ElwhaSideSheet sheet = sheet();
    sheet.close();

    present(sheet);

    assertThat(sheet.isOpen())
        .as("a modal presentation is its own open — the embedded collapse does not carry over")
        .isTrue();
  }

  @Test
  void presentingTwiceIsANoOp() {
    final ElwhaSideSheet sheet = present(sheet());

    sheet.showModal(host.anchor());

    assertThat(host.mounted()).as("a second presentation cannot strand a surface").hasSize(2);
  }

  // ---------------------------------------------------------------- docking

  @Test
  void aTrailingSheetDocksFlushAgainstTheRightEdgeFullHeight() {
    final ElwhaSideSheet sheet = present(sheet());

    assertThat(surface(sheet).getBounds())
        .isEqualTo(
            new Rectangle(
                PANE_W - sheet.modalFootprintWidth(), 0, sheet.modalFootprintWidth(), PANE_H));
  }

  @Test
  void aLeadingSheetDocksAgainstTheOtherEdge() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSheetEdge(SheetEdge.LEADING);

    present(sheet);

    assertThat(surface(sheet).getX()).isZero();
  }

  @Test
  void rightToLeftFlipsWhichEdgeTheSheetDocksAgainst() {
    host.anchor().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    final ElwhaSideSheet sheet = sheet();

    present(sheet);

    assertThat(surface(sheet).getX()).as("a trailing sheet docks on the left under RTL").isZero();
  }

  @Test
  void aPresentationAdoptsTheHostsReadingOrderOverOneSetOnTheSheet() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    present(sheet);

    assertThat(sheet.getComponentOrientation().isLeftToRight())
        .as("the overlay host mirrors its whole surface tree to the anchor's reading order")
        .isTrue();
    assertThat(surface(sheet).getX())
        .as("so an LTR host docks the sheet on the right even after a local RTL call")
        .isEqualTo(PANE_W - sheet.modalFootprintWidth());
  }

  @Test
  void aDetachedSheetDocksAtItsWiderFootprint() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSheetPosture(SheetPosture.DETACHED);

    present(sheet);

    assertThat(surface(sheet).getWidth())
        .as("the slide band carries the floating card plus its margin")
        .isEqualTo(sheet.getSheetWidth() + 2 * ElwhaSideSheet.DETACHED_MARGIN_PX);
  }

  @Test
  void changingTheWidthWhileShownRedocksTheSurface() {
    final ElwhaSideSheet sheet = present(sheet());

    sheet.setSheetWidth(400);

    assertThat(surface(sheet).getWidth()).isEqualTo(400);
    assertThat(surface(sheet).getX())
        .as("and it stays flush against its edge at the new width")
        .isEqualTo(PANE_W - 400);
  }

  @Test
  void changingThePostureWhileShownRedocksTheSurface() {
    final ElwhaSideSheet sheet = present(sheet());
    final int docked = surface(sheet).getWidth();

    sheet.setSheetPosture(SheetPosture.DETACHED);

    assertThat(surface(sheet).getWidth())
        .as("detaching widens the band by the margin, live")
        .isEqualTo(docked + 2 * ElwhaSideSheet.DETACHED_MARGIN_PX);
  }

  @Test
  void aHostResizeRedocksTheSurface() {
    final ElwhaSideSheet sheet = present(sheet());

    host.resizeAndNotify(600, 400);

    assertThat(surface(sheet).getBounds())
        .isEqualTo(
            new Rectangle(600 - sheet.modalFootprintWidth(), 0, sheet.modalFootprintWidth(), 400));
  }

  // ----------------------------------------------------------- dismiss routes

  @Test
  void aProgrammaticDismissReportsItself() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);

    sheet.dismiss();

    assertThat(causes).containsExactly(SheetDismissCause.PROGRAMMATIC);
    assertThat(host.mounted()).isEmpty();
    assertThat(sheet.isModalShowing()).isFalse();
  }

  @Test
  void aCloseAffordanceReportsItsOwnCause() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);

    sheet.onCloseActivated();

    assertThat(causes).containsExactly(SheetDismissCause.CLOSE_AFFORDANCE);
  }

  @Test
  void aBackAffordanceReportsItsOwnCause() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);

    sheet.onBackActivated();

    assertThat(causes).containsExactly(SheetDismissCause.BACK_AFFORDANCE);
  }

  @Test
  void escapeIsBoundWindowWideAndReportsItsOwnCause() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);
    final JComponent slide = (JComponent) surface(sheet);

    assertThat(
            slide
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("ESCAPE")))
        .isEqualTo("elwha-sidesheet-esc");
    slide
        .getActionMap()
        .get("elwha-sidesheet-esc")
        .actionPerformed(new ActionEvent(slide, 0, "escape"));

    assertThat(causes).containsExactly(SheetDismissCause.ESC);
  }

  @Test
  void turningEscapeOffWhileShownTakesEffectImmediately() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);
    final JComponent slide = (JComponent) surface(sheet);

    sheet.setDismissibleByEsc(false);
    slide
        .getActionMap()
        .get("elwha-sidesheet-esc")
        .actionPerformed(new ActionEvent(slide, 0, "escape"));

    assertThat(causes)
        .as("a long-lived sheet honors its config live, unlike a dialog's build-time snapshot")
        .isEmpty();
    assertThat(sheet.isModalShowing()).isTrue();
  }

  @Test
  void aScrimPressReportsItsOwnCause() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);

    Input.press(scrim(sheet), 10, 10);

    assertThat(causes).containsExactly(SheetDismissCause.SCRIM);
  }

  @Test
  void aScrimPressOnANonDismissibleSheetIsSwallowedRatherThanPassedThrough() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    sheet.setDismissibleByScrim(false);
    present(sheet);
    final Component backdrop = scrim(sheet);

    Input.press(backdrop, 10, 10);

    assertThat(causes).as("the sheet stays up").isEmpty();
    assertThat(backdrop.getMouseListeners())
        .as("but the scrim still consumes the press — it always blocks the UI behind it")
        .isNotEmpty();
  }

  @Test
  void aDismissedSheetReportsItsCauseOnlyOnce() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    present(sheet);

    sheet.dismiss();
    sheet.dismiss();

    assertThat(causes).hasSize(1);
  }

  @Test
  void aSheetCanBePresentedAgainAfterDismissal() {
    final ElwhaSideSheet sheet = present(sheet());
    sheet.dismiss();

    present(sheet);

    assertThat(sheet.isModalShowing()).isTrue();
    assertThat(host.mounted()).hasSize(2);
  }

  // ------------------------------------------------- borrow and return (#597)

  @Test
  void anEmbeddedSheetGoesBackToItsLayoutSlotAfterAModalPresentation() {
    final JPanel page = new JPanel(new BorderLayout());
    final JLabel main = new JLabel("main content");
    page.add(main, BorderLayout.CENTER);
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    page.add(sheet, BorderLayout.LINE_END);

    present(sheet);
    assertThat(sheet.getParent())
        .as("the presentation lifts the sheet out of the page")
        .isNotSameAs(page);
    sheet.dismiss();

    assertThat(sheet.getParent())
        .as("#597 — the sheet never came back and the page lost its trailing pane for good")
        .isSameAs(page);
    assertThat(((BorderLayout) page.getLayout()).getConstraints(sheet))
        .as("and it came back to the edge it was placed on, not merely into the container")
        .isEqualTo(BorderLayout.LINE_END);
    assertThat(page.getComponents()).contains(main);
  }

  @Test
  void aStandardSheetGetsItsChromeBackAfterAModalPresentation() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");

    present(sheet);
    assertThat(sheet.getSheetType())
        .as("the presentation forces MODAL chrome, per design doc §5")
        .isEqualTo(SheetType.MODAL);
    sheet.dismiss();

    assertThat(sheet.getSheetType())
        .as("#597 — the forcing outlived the presentation and left standard chrome behind")
        .isEqualTo(SheetType.STANDARD);
  }

  @Test
  void aClosedEmbeddedSheetComesBackClosed() {
    final JPanel page = new JPanel(new BorderLayout());
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    page.add(sheet, BorderLayout.LINE_END);
    sheet.close();

    present(sheet);
    assertThat(sheet.isOpen()).as("a modal presentation is open by definition").isTrue();
    sheet.dismiss();

    assertThat(sheet.isOpen())
        .as("the collapsed pane must not reappear expanded in the layout it returns to")
        .isFalse();
  }

  @Test
  void aConsumerRetypeWhileShownOutranksTheRestore() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");

    present(sheet);
    sheet.setSheetType(SheetType.STANDARD);
    sheet.dismiss();

    assertThat(sheet.getSheetType())
        .as("the teardown undoes only the forcing showModal itself did")
        .isEqualTo(SheetType.STANDARD);
  }

  @Test
  void aModalTypedSheetThatWasNeverEmbeddedIsUnaffected() {
    final ElwhaSideSheet sheet = present(sheet());

    sheet.dismiss();

    assertThat(sheet.getSheetType()).isEqualTo(SheetType.MODAL);
    assertThat(sheet.getParent()).as("there was no layout slot to give back").isNull();
  }

  // ---------------------------------------------------------------- gestures

  @Test
  void aStandardDragScrubsTheOpenWidthOneToOne() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setDragToDismissEnabled(true);

    sheet.applyDragFraction(0.25f);

    assertThat(sheet.getPreferredSize().width)
        .as("the coplanar squash tracks the pointer instead of easing behind it")
        .isEqualTo(Math.round(sheet.getSheetWidth() * 0.75f));
  }

  @Test
  void aStandardDragPastTheThresholdCloses() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setDragToDismissEnabled(true);

    sheet.applyDragFraction(0.7f);
    sheet.releaseDrag(0.7f);

    assertThat(sheet.isOpen()).isFalse();
    assertThat(sheet.getPreferredSize().width).isZero();
  }

  @Test
  void aStandardDragReleasedShortOfTheThresholdSettlesBackOpen() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setDragToDismissEnabled(true);

    sheet.applyDragFraction(0.3f);
    sheet.releaseDrag(0.3f);

    assertThat(sheet.isOpen()).isTrue();
    assertThat(sheet.getPreferredSize().width)
        .as("a drag that did not commit leaves the sheet exactly as it was")
        .isEqualTo(sheet.getSheetWidth());
  }

  /** Drags the header a whole sheet-width toward the docked edge, starting with {@code button}. */
  private void headerDrag(final ElwhaSideSheet sheet, final int button) {
    final Component header = sheet.headerPanel();
    final int toward = sheet.isDockedRight() ? sheet.getSheetWidth() : -sheet.getSheetWidth();
    Input.press(header, 10, 5, button);
    Input.drag(header, 10 + toward, 5);
    Input.release(header, 10 + toward, 5);
  }

  @Test
  void aHeaderDragDismissesTheSheet() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setDragToDismissEnabled(true);

    headerDrag(sheet, MouseEvent.BUTTON1);

    assertThat(sheet.isOpen())
        .as("a left-button drag past the threshold is the gesture, and it still works")
        .isFalse();
  }

  @Test
  void onlyTheLeftButtonStartsAHeaderDrag() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setDragToDismissEnabled(true);

    headerDrag(sheet, MouseEvent.BUTTON3);

    assertThat(sheet.isOpen())
        .as("a right-button press-drag on the header must not scrub the sheet away")
        .isTrue();
    assertThat(sheet.getPreferredSize().width)
        .as("nor leave it scrubbed part-way")
        .isEqualTo(sheet.getSheetWidth());
  }

  @Test
  void halfwayIsAlreadyPastTheDismissThreshold() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setDragToDismissEnabled(true);

    sheet.releaseDrag(0.5f);

    assertThat(sheet.isOpen()).as("the threshold is inclusive at the halfway point").isFalse();
  }

  @Test
  void aModalDragPastTheThresholdDismissesWithTheDragCause() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    sheet.setDragToDismissEnabled(true);
    present(sheet);

    sheet.applyDragFraction(0.8f);
    sheet.releaseDrag(0.8f);

    assertThat(causes)
        .as("a dragged-away modal reports the gesture, not a generic programmatic close")
        .containsExactly(SheetDismissCause.DRAG);
    assertThat(host.mounted()).isEmpty();
  }

  @Test
  void aModalDragReleasedShortOfTheThresholdSettlesBackDocked() {
    final List<SheetDismissCause> causes = new ArrayList<>();
    final ElwhaSideSheet sheet = sheet();
    sheet.setOnClose(causes::add);
    sheet.setDragToDismissEnabled(true);
    present(sheet);

    sheet.applyDragFraction(0.4f);
    sheet.releaseDrag(0.4f);

    assertThat(causes).isEmpty();
    assertThat(sheet.isModalShowing()).as("the sheet is still up").isTrue();
    assertThat(surface(sheet).getBounds())
        .as("and back at its docked geometry")
        .isEqualTo(
            new Rectangle(
                PANE_W - sheet.modalFootprintWidth(), 0, sheet.modalFootprintWidth(), PANE_H));
  }

  @Test
  void aDragFractionOutsideTheUnitRangeIsClamped() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");

    assertThatCode(
            () -> {
              sheet.applyDragFraction(-3f);
              sheet.applyDragFraction(7f);
            })
        .doesNotThrowAnyException();
    assertThat(sheet.getPreferredSize().width)
        .as("a fully-dragged sheet is collapsed, not negative")
        .isZero();
  }
}
