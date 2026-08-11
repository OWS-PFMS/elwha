package com.owspfm.elwha.tooltip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaTooltip}'s construction and configuration surface: the two
 * variants and the setters each one refuses, the delay contract, the anchor attach/detach
 * bookkeeping (including the {@code aria-describedby} analog it writes and later withdraws), and
 * the passive-focus posture that makes a tooltip the one overlay that never takes focus.
 *
 * <p>Trigger <em>timing</em> — dwell, linger, and the self-re-arming hide — is a real-clock,
 * real-pointer contract and lives in the {@code gui} tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTooltipConfigTest {

  private HeadlessHost host;
  private final List<ElwhaTooltip> shown = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(600, 400);
  }

  @AfterEach
  void dismissEveryTooltip() {
    for (final ElwhaTooltip tooltip : shown) {
      tooltip.dismiss();
    }
    shown.clear();
  }

  private ElwhaTooltip show(final ElwhaTooltip tooltip) {
    shown.add(tooltip);
    tooltip.show(host.anchor());
    return tooltip;
  }

  private static ElwhaTooltip richTooltip() {
    return ElwhaTooltip.rich()
        .subhead("Layers")
        .supportingText("Stack order of the canvas")
        .build();
  }

  // ---------------------------------------------------------------- variants

  @Test
  void aPlainTooltipCarriesItsLabelAndCentersOnTheAnchor() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");

    assertThat(tooltip.getVariant()).isEqualTo(TooltipVariant.PLAIN);
    assertThat(tooltip.getText()).isEqualTo("Rename");
    assertThat(tooltip.getAlignment())
        .as("a label bubble centers under its anchor by default")
        .isEqualTo(TooltipAlignment.CENTER);
    assertThat(tooltip.getPreferredPlacement()).isEqualTo(TooltipPlacement.ABOVE);
  }

  @Test
  void aPlainTooltipRequiresText() {
    assertThatNullPointerException().isThrownBy(() -> ElwhaTooltip.plain(null));
    assertThatNullPointerException().isThrownBy(() -> ElwhaTooltip.plain("Rename").setText(null));
  }

  @Test
  void aRichTooltipCarriesItsPartsAndAlignsToTheAnchorsEnd() {
    final ElwhaTooltip tooltip = richTooltip();

    assertThat(tooltip.getVariant()).isEqualTo(TooltipVariant.RICH);
    assertThat(tooltip.getSubhead()).isEqualTo("Layers");
    assertThat(tooltip.getSupportingText()).isEqualTo("Stack order of the canvas");
    assertThat(tooltip.getAlignment())
        .as("a rich card aligns to the anchor's trailing edge by default")
        .isEqualTo(TooltipAlignment.END);
    assertThat(tooltip.isPersistent())
        .as("and is the transient flavor unless asked otherwise")
        .isFalse();
  }

  @Test
  void aRichTooltipRequiresSupportingText() {
    assertThatIllegalStateException()
        .isThrownBy(() -> ElwhaTooltip.rich().subhead("Layers").build())
        .withMessageContaining("supportingText");
    assertThatNullPointerException().isThrownBy(() -> ElwhaTooltip.rich().supportingText(null));
  }

  @Test
  void aRichActionNeedsBothALabelAndAListener() {
    assertThatNullPointerException().isThrownBy(() -> ElwhaTooltip.rich().action(null, e -> {}));
    assertThatNullPointerException()
        .isThrownBy(() -> ElwhaTooltip.rich().action("Learn more", null));
  }

  @Test
  void plainOnlySettersRefuseARichTooltip() {
    final ElwhaTooltip tooltip = richTooltip();

    assertThatIllegalStateException()
        .isThrownBy(() -> tooltip.setText("nope"))
        .withMessageContaining("plain-only");
    assertThat(tooltip.getText()).as("and a rich tooltip has no plain label to read").isNull();
  }

  @Test
  void richOnlySettersRefuseAPlainTooltip() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");

    assertThatIllegalStateException().isThrownBy(() -> tooltip.setSubhead("nope"));
    assertThatIllegalStateException().isThrownBy(() -> tooltip.setSupportingText("nope"));
    assertThatIllegalStateException()
        .as("M3 defines persistence for the rich variant only")
        .isThrownBy(() -> tooltip.setPersistent(true));
  }

  @Test
  void placementAndAlignmentRejectNull() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");
    assertThatNullPointerException().isThrownBy(() -> tooltip.setPreferredPlacement(null));
    assertThatNullPointerException().isThrownBy(() -> tooltip.setAlignment(null));
  }

  // ------------------------------------------------------------------ delays

  @Test
  void delaysStartAtTheDesktopDefaults() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");

    assertThat(tooltip.getShowDelayMs()).isEqualTo(ElwhaTooltip.DEFAULT_SHOW_DELAY_MS);
    assertThat(tooltip.getHideDelayMs()).isEqualTo(ElwhaTooltip.DEFAULT_HIDE_DELAY_MS);
    assertThat(tooltip.getHideDelayMs())
        .as("the linger outlasts the dwell, so crossing the gap onto the tooltip keeps it open")
        .isGreaterThan(tooltip.getShowDelayMs());
  }

  @Test
  void delaysRoundTripAndRefuseNegatives() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");

    tooltip.setShowDelayMs(0);
    tooltip.setHideDelayMs(50);
    assertThat(tooltip.getShowDelayMs())
        .as("a zero dwell shows immediately, which is legal")
        .isZero();
    assertThat(tooltip.getHideDelayMs()).isEqualTo(50);

    assertThatIllegalArgumentException().isThrownBy(() -> tooltip.setShowDelayMs(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> tooltip.setHideDelayMs(-1));
  }

  // ------------------------------------------------------------------ attach

  @Test
  void attachingWiresTheAnchorAndDescribesIt() {
    final JPanel anchor = new JPanel();
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");

    assertThat(tooltip.attach(anchor)).as("attach chains").isSameAs(tooltip);
    assertThat(tooltip.getAttachedAnchor()).isSameAs(anchor);
    assertThat(anchor.getAccessibleContext().getAccessibleDescription())
        .as("the anchor gains the described-by text Swing's own tooltips would set")
        .isEqualTo("Rename");
  }

  @Test
  void anAnchorsDescriptionTracksTheTooltipText() {
    final JPanel anchor = new JPanel();
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(anchor);

    tooltip.setText("Rename this layer");

    assertThat(anchor.getAccessibleContext().getAccessibleDescription())
        .as("changing the visible text changes what assistive tech reads")
        .isEqualTo("Rename this layer");
  }

  @Test
  void aRichAnchorIsDescribedBySubheadAndSupportingTextTogether() {
    final JPanel anchor = new JPanel();
    richTooltip().attach(anchor);

    assertThat(anchor.getAccessibleContext().getAccessibleDescription())
        .isEqualTo("Layers. Stack order of the canvas");
  }

  @Test
  void oneTooltipAttachesToOneAnchor() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(new JPanel());

    assertThatIllegalStateException()
        .isThrownBy(() -> tooltip.attach(new JPanel()))
        .withMessageContaining("detach");
    assertThatNullPointerException().isThrownBy(() -> tooltip.attach(null));
  }

  @Test
  void detachingWithdrawsTheDescriptionItWrote() {
    final JPanel anchor = new JPanel();
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(anchor);

    tooltip.detach();

    assertThat(tooltip.getAttachedAnchor()).isNull();
    assertThat(anchor.getAccessibleContext().getAccessibleDescription())
        .as("a detached tooltip leaves no description behind")
        .isNull();
  }

  @Test
  void detachingLeavesAConsumerOwnedDescriptionAlone() {
    final JPanel anchor = new JPanel();
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(anchor);
    anchor.getAccessibleContext().setAccessibleDescription("set by the app");

    tooltip.detach();

    assertThat(anchor.getAccessibleContext().getAccessibleDescription())
        .as("only a description this tooltip still owns is cleared")
        .isEqualTo("set by the app");
  }

  @Test
  void detachingWhenNeverAttachedIsInert() {
    assertThatCode(() -> ElwhaTooltip.plain("Rename").detach()).doesNotThrowAnyException();
  }

  @Test
  void reattachingAfterDetachIsAllowed() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(new JPanel());
    tooltip.detach();

    final JPanel second = new JPanel();
    assertThatCode(() -> tooltip.attach(second)).doesNotThrowAnyException();
    assertThat(tooltip.getAttachedAnchor()).isSameAs(second);
  }

  // ----------------------------------------------------------- host posture

  @Test
  void aTooltipIsThePassiveFocusOverlay() {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename");

    assertThat(tooltip.takesFocus())
        .as("the anchor keeps focus the whole time — a hovering pointer must not steal typing")
        .isFalse();
    assertThat(tooltip.restoreFocusOnClose())
        .as("and there is nothing to restore, since nothing was taken")
        .isFalse();
    assertThat(tooltip.lightDismiss()).as("an outside press dismisses it").isTrue();
    assertThat(tooltip.overlayLayer())
        .as("it mounts on the popup band with the menu")
        .isEqualTo(JLayeredPane.POPUP_LAYER);
  }

  @Test
  void showingMountsASingleSurfaceWithNoScrim() {
    final ElwhaTooltip tooltip = show(ElwhaTooltip.plain("Rename"));

    assertThat(tooltip.isTooltipShowing()).isTrue();
    assertThat(host.mounted()).as("a tooltip has no backdrop to block input").hasSize(1);
    assertThat(host.mounted().get(0).getAccessibleContext().getAccessibleName())
        .as("the surface announces the tooltip text")
        .isEqualTo("Rename");
  }

  @Test
  void showingASecondTooltipEvictsTheFirst() {
    final ElwhaTooltip first = show(ElwhaTooltip.plain("Rename"));

    final ElwhaTooltip second = show(ElwhaTooltip.plain("Delete"));

    assertThat(first.isTooltipShowing())
        .as("at most one tooltip is up at a time, application-wide")
        .isFalse();
    assertThat(second.isTooltipShowing()).isTrue();
    assertThat(host.mounted()).hasSize(1);
  }

  @Test
  void dismissingDetachesTheSurface() {
    final ElwhaTooltip tooltip = show(ElwhaTooltip.plain("Rename"));

    tooltip.dismiss();

    assertThat(tooltip.isTooltipShowing()).isFalse();
    assertThat(host.mounted()).isEmpty();
  }

  @Test
  void dismissingATooltipThatIsNotShowingIsInert() {
    assertThatCode(() -> ElwhaTooltip.plain("Rename").dismiss()).doesNotThrowAnyException();
  }

  @Test
  void anAttachedAnchorCountsAsInsideSoItsOwnPressHandlerOwnsTheDismiss() {
    final JPanel anchor = new JPanel();
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(anchor);
    show(tooltip);

    assertThat(tooltip.ownsFocus(anchor))
        .as("the trigger's press is the trigger's business — the host must not race it")
        .isTrue();
    assertThat(tooltip.ownsFocus(host.anchor()))
        .as("while a press anywhere else is a genuine outside press")
        .isFalse();
  }

  @Test
  void detachingDismissesAShowingTooltip() {
    final JPanel anchor = new JPanel();
    final ElwhaTooltip tooltip = ElwhaTooltip.plain("Rename").attach(anchor);
    show(tooltip);

    tooltip.detach();

    assertThat(tooltip.isTooltipShowing())
        .as("a tooltip whose anchor is gone has nothing left to describe")
        .isFalse();
  }
}
