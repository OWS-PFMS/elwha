package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.list.ElwhaListItemView;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the {@link ElwhaListItemView} capability as {@code ElwhaChip} implements it.
 *
 * <p>The load-bearing rule is the one the interface's own Javadoc calls out: a view must not
 * self-toggle while list-interactive. Inverted wiring there is what produced the card family's
 * stale-visual and selection-lost-on-filter defects, and {@code setListInteractive} exists to
 * switch it off. So the tests below check both halves — that handing over forces the non-owning
 * mode, and that a click in that mode fires an action for the list to act on without moving the
 * chip's own selected state.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipListItemViewTest {

  private static final int WIDTH = 120;
  private static final int HEIGHT = 40;
  private static final int CENTER_X = 60;
  private static final int CENTER_Y = 20;

  private static ElwhaChip sized(final ChipInteractionMode mode) {
    final ElwhaChip chip = new ElwhaChip("Demand").setInteractionMode(mode);
    chip.setSize(WIDTH, HEIGHT);
    return chip;
  }

  // ------------------------------------------------------------- capability

  @Test
  void aChipIsAListItemView() {
    assertThat(new ElwhaChip("Demand"))
        .as("the chip satisfies the capability a hosting list needs to drive it")
        .isInstanceOf(ElwhaListItemView.class);
  }

  @Test
  void mutatorsReturnTheChipByCovariantOverride() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);

    assertThat(chip.setSelected(true))
        .as("setSelected keeps the chip's own fluent type")
        .isSameAs(chip);
    assertThat(chip.cancelPendingClick()).as("and so does cancelPendingClick").isSameAs(chip);
    assertThat(chip.setListInteractive(true)).as("and setListInteractive").isSameAs(chip);
    assertThat(chip.setDragged(true))
        .as("the default drag hook is a no-op that still chains — a chip has no elevation to lift")
        .isSameAs(chip);
  }

  @Test
  void selectionIsReadableAndWritableFromTheList() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    chip.setSelected(true);
    assertThat(chip.isSelected()).as("the list writes selection onto the view").isTrue();

    chip.setSelected(false);
    assertThat(chip.isSelected()).as("and clears it the same way").isFalse();
  }

  // ------------------------------------------------------ handing over control

  @ParameterizedTest
  @EnumSource(ChipInteractionMode.class)
  void handingOverAlwaysForcesTheClickableMode(final ChipInteractionMode mode) {
    final ElwhaChip chip = sized(mode);

    chip.setListInteractive(true);

    assertThat(chip.getInteractionMode())
        .as("whatever the chip was, a list-driven one fires actions without owning selection")
        .isEqualTo(ChipInteractionMode.CLICKABLE);
  }

  @Test
  void takingControlBackRestoresThePreviousMode() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);

    chip.setListInteractive(true);
    chip.setListInteractive(false);

    assertThat(chip.getInteractionMode())
        .as("the chip returns to the configuration it carried before the list claimed it")
        .isEqualTo(ChipInteractionMode.SELECTABLE);
  }

  @Test
  void aRepeatedHandOverKeepsTheOriginalMode() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);

    chip.setListInteractive(true);
    chip.setListInteractive(true);
    chip.setListInteractive(false);

    assertThat(chip.getInteractionMode())
        .as(
            "the remembered mode is captured once, so a second hand-over cannot overwrite it with"
                + " the CLICKABLE the first one installed")
        .isEqualTo(ChipInteractionMode.SELECTABLE);
  }

  @Test
  void releasingAChipThatWasNeverClaimedChangesNothing() {
    final ElwhaChip chip = sized(ChipInteractionMode.STATIC);

    chip.setListInteractive(false);

    assertThat(chip.getInteractionMode())
        .as("with nothing remembered there is nothing to restore")
        .isEqualTo(ChipInteractionMode.STATIC);
  }

  @Test
  void aFullRoundTripCanBeRepeated() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);

    chip.setListInteractive(true);
    chip.setListInteractive(false);
    chip.setListInteractive(true);

    assertThat(chip.getInteractionMode())
        .as("claiming again after a release works, so the memory really was cleared")
        .isEqualTo(ChipInteractionMode.CLICKABLE);

    chip.setListInteractive(false);
    assertThat(chip.getInteractionMode())
        .as("and the second release restores the original mode once more")
        .isEqualTo(ChipInteractionMode.SELECTABLE);
  }

  // ----------------------------------------------------- the no-self-toggle rule

  @Test
  void aListInteractiveChipDoesNotSelfToggleOnClick() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setListInteractive(true);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0])
        .as("the click still reaches the list, which is what decides what happens next")
        .isEqualTo(1);
    assertThat(chip.isSelected())
        .as("but the view does not move its own selection — the list writes that back")
        .isFalse();
  }

  @Test
  void aListWriteBackIsWhatMovesTheChrome() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setListInteractive(true);
    chip.addActionListener(e -> chip.setSelected(true));

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(chip.isSelected())
        .as("selection arrives through setSelected, exactly as a hosting list would drive it")
        .isTrue();
  }

  @Test
  void takingControlBackRestoresSelfToggling() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setListInteractive(true);
    chip.setListInteractive(false);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(chip.isSelected())
        .as("an unclaimed selectable chip owns its selection again")
        .isTrue();
  }

  @Test
  void aHandOverDoesNotDisturbTheCurrentSelection() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setSelected(true);

    chip.setListInteractive(true);

    assertThat(chip.isSelected())
        .as("claiming a chip changes who drives selection, not what the selection currently is")
        .isTrue();
  }

  // -------------------------------------------------------- pending-click hook

  @Test
  void cancelPendingClickStopsTheReleaseFromReachingTheList() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    chip.setListInteractive(true);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.press(chip, CENTER_X, CENTER_Y);
    chip.cancelPendingClick();
    Input.release(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0])
        .as("without this hook the release that ends a drag would also fire the view's action")
        .isZero();
  }

  @Test
  void cancelPendingClickOnAnIdleChipIsHarmless() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    chip.cancelPendingClick();
    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0]).as("cancelling nothing leaves the next real click working").isEqualTo(1);
  }
}
