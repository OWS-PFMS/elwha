package com.owspfm.elwha.sidesheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of {@link ElwhaSideSheet}'s configuration axes: the two types, the two postures,
 * the logical anchor edge and how it resolves against component orientation, the width band the
 * resize gesture clamps to, the anatomy slots, and the affordance/divider toggles. Also pins the
 * two gestures' <em>opt-in</em> defaults, which the V2 design made deliberate.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSideSheetConfigTest {

  private static ElwhaSideSheet sheet() {
    return new ElwhaSideSheet("Filters");
  }

  private static List<Component> descendants(final Container root) {
    final List<Component> found = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      found.add(child);
      if (child instanceof Container container) {
        found.addAll(descendants(container));
      }
    }
    return found;
  }

  // -------------------------------------------------------------- factories

  @Test
  void aConvenienceConstructorMakesAStandardSheet() {
    assertThat(sheet().getSheetType())
        .as("an embeddable panel is the default flavor")
        .isEqualTo(SheetType.STANDARD);
  }

  @Test
  void typedFactoriesPickTheirType() {
    assertThat(ElwhaSideSheet.standardSheet("Filters").getSheetType())
        .isEqualTo(SheetType.STANDARD);
    assertThat(ElwhaSideSheet.modalSheet("Filters").getSheetType()).isEqualTo(SheetType.MODAL);
  }

  @Test
  void aFreshSheetStartsDockedTrailingAtTheM3Width() {
    final ElwhaSideSheet sheet = sheet();

    assertThat(sheet.getSheetEdge()).isEqualTo(SheetEdge.TRAILING);
    assertThat(sheet.getSheetPosture())
        .as("a docked sheet is exactly the V1 sheet, so it stays the default")
        .isEqualTo(SheetPosture.DOCKED);
    assertThat(sheet.getSheetWidth()).isEqualTo(ElwhaSideSheet.SHEET_WIDTH_PX);
    assertThat(sheet.isOpen()).as("an embedded sheet starts open").isTrue();
  }

  @Test
  void everyChromeAxisRejectsNull() {
    final ElwhaSideSheet sheet = sheet();
    assertThatNullPointerException().isThrownBy(() -> sheet.setSheetType(null));
    assertThatNullPointerException().isThrownBy(() -> sheet.setSheetEdge(null));
    assertThatNullPointerException().isThrownBy(() -> sheet.setSheetPosture(null));
  }

  @Test
  void everyChromeAxisRoundTrips() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setSheetType(SheetType.MODAL);
    sheet.setSheetEdge(SheetEdge.LEADING);
    sheet.setSheetPosture(SheetPosture.DETACHED);

    assertThat(sheet.getSheetType()).isEqualTo(SheetType.MODAL);
    assertThat(sheet.getSheetEdge()).isEqualTo(SheetEdge.LEADING);
    assertThat(sheet.getSheetPosture()).isEqualTo(SheetPosture.DETACHED);
  }

  // ------------------------------------------------------------ edge resolve

  @ParameterizedTest
  @CsvSource({
    "TRAILING, true, true",
    "TRAILING, false, false",
    "LEADING, true, false",
    "LEADING, false, true"
  })
  void aLogicalEdgeResolvesAgainstComponentOrientation(
      final SheetEdge edge, final boolean leftToRight, final boolean expectDockedRight) {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSheetEdge(edge);
    sheet.setComponentOrientation(
        leftToRight ? ComponentOrientation.LEFT_TO_RIGHT : ComponentOrientation.RIGHT_TO_LEFT);

    assertThat(sheet.isDockedRight())
        .as("%s under %s reading order", edge, leftToRight ? "LTR" : "RTL")
        .isEqualTo(expectDockedRight);
  }

  // ------------------------------------------------------------------ width

  @Test
  void aNegativeWidthIsClampedRatherThanRejected() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setSheetWidth(-40);

    assertThat(sheet.getSheetWidth()).isZero();
  }

  @Test
  void resizeBandStartsAtItsDocumentedDefaults() {
    final ElwhaSideSheet sheet = sheet();

    assertThat(sheet.getMinSheetWidth()).isEqualTo(200);
    assertThat(sheet.getMaxSheetWidth()).isEqualTo(600);
  }

  @Test
  void raisingTheMinimumAboveTheCurrentWidthGrowsTheSheet() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setMinSheetWidth(320);

    assertThat(sheet.getSheetWidth())
        .as("a sheet narrower than its own floor is widened to it")
        .isEqualTo(320);
  }

  @Test
  void raisingTheMinimumPastTheMaximumPushesTheMaximumUpToo() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setMinSheetWidth(800);

    assertThat(sheet.getMaxSheetWidth())
        .as("the band can never invert")
        .isGreaterThanOrEqualTo(sheet.getMinSheetWidth());
  }

  @Test
  void loweringTheMaximumBelowTheCurrentWidthShrinksTheSheet() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSheetWidth(500);

    sheet.setMaxSheetWidth(400);

    assertThat(sheet.getSheetWidth()).isEqualTo(400);
  }

  @Test
  void maximumNeverDropsBelowTheMinimum() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setMaxSheetWidth(10);

    assertThat(sheet.getMaxSheetWidth()).isEqualTo(sheet.getMinSheetWidth());
  }

  @Test
  void resizeGestureClampsToTheBand() {
    final ElwhaSideSheet sheet = sheet();

    sheet.resizeWidthTo(10_000);
    assertThat(sheet.getSheetWidth()).isEqualTo(sheet.getMaxSheetWidth());

    sheet.resizeWidthTo(-10);
    assertThat(sheet.getSheetWidth()).isEqualTo(sheet.getMinSheetWidth());
  }

  // ----------------------------------------------------------- footprint

  @Test
  void aDockedSheetsFootprintIsJustItsWidth() {
    final ElwhaSideSheet sheet = sheet();

    assertThat(sheet.modalFootprintWidth())
        .as("a docked sheet sits flush, so there is no margin to carry")
        .isEqualTo(sheet.getSheetWidth());
  }

  @Test
  void aDetachedSheetsFootprintCarriesItsMarginOnBothSides() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setSheetPosture(SheetPosture.DETACHED);

    assertThat(sheet.modalFootprintWidth())
        .as("the floating card's own border is the margin, so the host never re-adds it")
        .isEqualTo(sheet.getSheetWidth() + 2 * ElwhaSideSheet.DETACHED_MARGIN_PX);
  }

  // ----------------------------------------------------------- open / close

  @Test
  void closingCollapsesThePreferredWidthToNothing() {
    final ElwhaSideSheet sheet = sheet();

    sheet.close();

    assertThat(sheet.isOpen()).isFalse();
    assertThat(sheet.getPreferredSize().width)
        .as("a collapsed sheet leaves no sliver behind in the host layout")
        .isZero();
  }

  @Test
  void reopeningRestoresTheFullWidth() {
    final ElwhaSideSheet sheet = sheet();
    sheet.close();

    sheet.open();

    assertThat(sheet.isOpen()).isTrue();
    assertThat(sheet.getPreferredSize().width).isEqualTo(sheet.getSheetWidth());
  }

  @Test
  void aClosedDetachedSheetLeavesNoMarginSliver() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSheetPosture(SheetPosture.DETACHED);

    sheet.close();

    assertThat(sheet.getPreferredSize().width)
        .as("width 0 means 0, not twice the detached margin")
        .isZero();
  }

  @Test
  void openingAnAlreadyOpenSheetIsInert() {
    final ElwhaSideSheet sheet = sheet();

    assertThatCode(sheet::open).doesNotThrowAnyException();
    assertThat(sheet.isOpen()).isTrue();
  }

  // ------------------------------------------------------------ anatomy

  @Test
  void headlineIsAlsoTheAccessibleName() {
    final ElwhaSideSheet sheet = sheet();

    assertThat(sheet.getHeadline()).isEqualTo("Filters");
    assertThat(sheet.getAccessibleContext().getAccessibleName()).isEqualTo("Filters");

    sheet.setHeadline("Details");
    assertThat(sheet.getAccessibleContext().getAccessibleName())
        .as("renaming the sheet renames what assistive tech announces")
        .isEqualTo("Details");
  }

  @Test
  void aSheetAnnouncesItselfAsAPanel() {
    assertThat(sheet().getAccessibleContext().getAccessibleRole())
        .as("Swing has no side-sheet role, so it is a named panel")
        .isEqualTo(AccessibleRole.PANEL);
  }

  @Test
  void contentSlotIsHostedAndReplaceable() {
    final ElwhaSideSheet sheet = sheet();
    final JLabel first = new JLabel("one");
    final JLabel second = new JLabel("two");

    sheet.setContent(first);
    assertThat(sheet.getContent()).isSameAs(first);
    assertThat(descendants(sheet)).contains(first);

    sheet.setContent(second);
    assertThat(descendants(sheet))
        .as("replacing the content detaches the previous slot rather than stacking")
        .contains(second)
        .doesNotContain(first);

    sheet.setContent(null);
    assertThat(sheet.getContent()).isNull();
  }

  @Test
  void footerActionsAreStoredLeadingFirstAndReplaceWholesale() {
    final ElwhaSideSheet sheet = sheet();
    final ElwhaButton apply = ElwhaButton.filledButton("Apply");
    final ElwhaButton reset = ElwhaButton.textButton("Reset");

    sheet.setActions(apply, reset);
    assertThat(sheet.getActions())
        .as("the confirming action leads, per the M3 sheet renders")
        .containsExactly(apply, reset);

    sheet.setActions(reset);
    assertThat(sheet.getActions())
        .as("a later call replaces rather than appends")
        .containsExactly(reset);

    sheet.setActions();
    assertThat(sheet.getActions()).as("and no actions removes the footer entirely").isEmpty();
  }

  @Test
  void aNullActionInTheVarargsIsSkipped() {
    final ElwhaSideSheet sheet = sheet();
    final ElwhaButton apply = ElwhaButton.filledButton("Apply");

    sheet.setActions(apply, null);

    assertThat(sheet.getActions()).containsExactly(apply);
  }

  @Test
  void actionListHandedBackIsASnapshot() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setActions(ElwhaButton.textButton("Reset"));

    assertThat(sheet.getActions()).isUnmodifiable();
  }

  // ---------------------------------------------------------- affordances

  @Test
  void closeShowsByDefaultAndBackDoesNot() {
    final ElwhaSideSheet sheet = sheet();

    assertThat(sheet.isCloseAffordanceVisible()).as("every sheet needs a way out").isTrue();
    assertThat(sheet.isBackAffordanceVisible())
        .as("back is for multi-step flows, so it is opt-in")
        .isFalse();
  }

  @Test
  void bothAffordancesToggle() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setCloseAffordanceVisible(false);
    sheet.setBackAffordanceVisible(true);

    assertThat(sheet.isCloseAffordanceVisible()).isFalse();
    assertThat(sheet.isBackAffordanceVisible()).isTrue();
  }

  @Test
  void bothDividersShowByDefaultAndToggle() {
    final ElwhaSideSheet sheet = sheet();
    assertThat(sheet.isEdgeDividerVisible()).isTrue();
    assertThat(sheet.isFooterDividerVisible()).isTrue();

    sheet.setEdgeDividerVisible(false);
    sheet.setFooterDividerVisible(false);

    assertThat(sheet.isEdgeDividerVisible()).isFalse();
    assertThat(sheet.isFooterDividerVisible()).isFalse();
  }

  @Test
  void closeAffordanceClosesAnEmbeddedSheet() {
    final ElwhaSideSheet sheet = sheet();

    sheet.onCloseActivated();

    assertThat(sheet.isOpen())
        .as("with no modal presentation to dismiss, close means collapse")
        .isFalse();
  }

  // ------------------------------------------------- open-state notification

  /**
   * #506 — a standard sheet has two close paths, the consumer's own and the header X, and only the
   * modal presentation reported its outcome. A consumer embedding a standard sheet with a visible X
   * had no way to learn the user had self-closed it, so any control mirroring the open state
   * silently desynced. The Showcase worked around it by listening for componentResized and
   * re-reading isOpen(), which is not something a real consumer should have to invent.
   */
  @Test
  void aCloseAffordancePressReportsItselfToTheConsumer() {
    final ElwhaSideSheet sheet = sheet();
    final List<Boolean> seen = new ArrayList<>();
    sheet.addPropertyChangeListener(
        ElwhaSideSheet.PROPERTY_OPEN, e -> seen.add((Boolean) e.getNewValue()));

    sheet.onCloseActivated();

    assertThat(seen)
        .as("the path a consumer cannot otherwise observe is the point")
        .containsExactly(false);
  }

  @Test
  void programmaticOpenAndCloseFireTheSameProperty() {
    final ElwhaSideSheet sheet = sheet();
    final List<Boolean> seen = new ArrayList<>();
    sheet.addPropertyChangeListener(
        ElwhaSideSheet.PROPERTY_OPEN, e -> seen.add((Boolean) e.getNewValue()));

    sheet.close();
    sheet.open();

    assertThat(seen)
        .as("one event per target flip, whichever path flipped it")
        .containsExactly(false, true);
  }

  @Test
  void aRedundantSetOpenFiresNothing() {
    final ElwhaSideSheet sheet = sheet();
    final List<Boolean> seen = new ArrayList<>();
    sheet.addPropertyChangeListener(
        ElwhaSideSheet.PROPERTY_OPEN, e -> seen.add((Boolean) e.getNewValue()));

    sheet.setOpen(true);
    sheet.close();
    sheet.close();

    assertThat(seen).as("the event marks a change, not a call").containsExactly(false);
  }

  @Test
  void openStateIsReadableFromInsideTheListener() {
    final ElwhaSideSheet sheet = sheet();
    final List<Boolean> readBack = new ArrayList<>();
    sheet.addPropertyChangeListener(
        ElwhaSideSheet.PROPERTY_OPEN, e -> readBack.add(sheet.isOpen()));

    sheet.close();

    assertThat(readBack)
        .as("the target flips before the event, so a listener never reads a stale value")
        .containsExactly(false);
  }

  @Test
  void backAffordanceFallsBackToClosingWhenNoHandlerIsSet() {
    final ElwhaSideSheet sheet = sheet();

    sheet.onBackActivated();

    assertThat(sheet.isOpen()).isFalse();
  }

  @Test
  void aBackHandlerTakesOverFromTheCloseFallback() {
    final ElwhaSideSheet sheet = sheet();
    final List<String> steps = new ArrayList<>();
    sheet.setOnBack(() -> steps.add("back"));

    sheet.onBackActivated();

    assertThat(steps).containsExactly("back");
    assertThat(sheet.isOpen()).as("a multi-step flow stays open and navigates instead").isTrue();
    assertThat(sheet.getOnBack()).isNotNull();
  }

  // -------------------------------------------------------------- gestures

  @Test
  void bothDragGesturesAreOffUntilAskedFor() {
    final ElwhaSideSheet sheet = sheet();

    assertThat(sheet.isDragToDismissEnabled())
        .as("a header that dismisses on drag is non-obvious on the desktop, so it is opt-in")
        .isFalse();
    assertThat(sheet.isResizable()).as("and so is edge resize").isFalse();
  }

  @Test
  void bothDragGesturesToggleIndependently() {
    final ElwhaSideSheet sheet = sheet();

    sheet.setDragToDismissEnabled(true);
    assertThat(sheet.isDragToDismissEnabled()).isTrue();
    assertThat(sheet.isResizable())
        .as("they use different zones and do not imply each other")
        .isFalse();

    sheet.setResizable(true);
    assertThat(sheet.isResizable()).isTrue();
    assertThat(sheet.isDragToDismissEnabled()).isTrue();
  }

  // --------------------------------------------------------- modal defaults

  @Test
  void aSheetThatWasNeverPresentedIsNotModallyShowing() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");

    assertThat(sheet.isModalShowing()).isFalse();
    assertThatCode(sheet::dismiss)
        .as("dismissing an unpresented sheet is inert, not an error")
        .doesNotThrowAnyException();
  }

  @Test
  void bothModalDismissRoutesAreOpenByDefaultAndToggle() {
    final ElwhaSideSheet sheet = sheet();
    assertThat(sheet.isDismissibleByEsc()).isTrue();
    assertThat(sheet.isDismissibleByScrim()).isTrue();

    sheet.setDismissibleByEsc(false);
    sheet.setDismissibleByScrim(false);

    assertThat(sheet.isDismissibleByEsc()).isFalse();
    assertThat(sheet.isDismissibleByScrim()).isFalse();
  }

  @Test
  void closeHookRoundTrips() {
    final ElwhaSideSheet sheet = sheet();
    final List<SheetDismissCause> causes = new ArrayList<>();

    sheet.setOnClose(causes::add);

    assertThat(sheet.getOnClose()).isNotNull();
    assertThat(causes).as("the hook fires per presentation, not on assignment").isEmpty();
  }

  @Test
  void aSheetWithNoContentStillLaysOutItsHeaderAndBody() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSize(sheet.getPreferredSize());

    assertThatCode(sheet::doLayout).doesNotThrowAnyException();
    assertThat(sheet.bodyComponent().getWidth())
        .as("the anatomy body spans the sheet's content box")
        .isEqualTo(sheet.getSheetWidth());
  }

  @Test
  void aDetachedSheetInsetsItsBodyByTheMarginOnEverySide() {
    final ElwhaSideSheet sheet = sheet();
    sheet.setSheetPosture(SheetPosture.DETACHED);
    sheet.setSize(sheet.getPreferredSize());
    sheet.doLayout();

    assertThat(sheet.bodyComponent().getX())
        .as("the floating card is pushed off the leading edge by its margin")
        .isEqualTo(ElwhaSideSheet.DETACHED_MARGIN_PX);
    assertThat(sheet.bodyComponent().getY()).isEqualTo(ElwhaSideSheet.DETACHED_MARGIN_PX);
  }

  @Test
  void resizeStripIsMountedOnlyWhileResizeIsEnabled() {
    final ElwhaSideSheet sheet = sheet();
    final JPanel body = (JPanel) sheet.bodyComponent();

    assertThat(descendants(sheet))
        .as("the hot zone exists but stays hidden until asked for")
        .anyMatch(c -> c != body && !c.isVisible());

    sheet.setResizable(true);
    sheet.setSize(sheet.getPreferredSize());
    sheet.doLayout();

    assertThat(descendants(sheet))
        .anyMatch(c -> c.isVisible() && c.getWidth() == 8 && c.getHeight() > 0);
  }
}
