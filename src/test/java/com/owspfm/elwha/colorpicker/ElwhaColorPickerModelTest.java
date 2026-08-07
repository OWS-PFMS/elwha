package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of {@link ElwhaColorPicker}'s own model — the committed color with its alpha
 * normalization, the adjusting flag, the configurable mode and swatch-source lists, the favorites
 * store, and the recent-colors ring. Every pane writes through this one seam, so its contracts hold
 * whichever mode the user happens to be in.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaColorPickerModelTest {

  // -------------------------------------------------------------- the color

  @Test
  void aFreshPickerHasEveryModeAndSourceAvailable() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    assertThat(picker.getModes())
        .as("all four picker modes ship enabled")
        .containsExactly(PickerMode.values());
    assertThat(picker.getMode())
        .as("opening on the first of them")
        .isEqualTo(picker.getModes().get(0));
    assertThat(picker.getSwatchSources())
        .as("and all three swatch tiers")
        .containsExactly(SwatchSource.values());
    assertThat(picker.isAdjusting()).as("nothing is being dragged").isFalse();
    assertThat(picker.isAlphaEnabled()).as("alpha is opt-in").isFalse();
    assertThat(picker.getFavorites()).as("with no saved colors yet").isEmpty();
  }

  @Test
  void initialColorIsHeld() {
    assertThat(new ElwhaColorPicker(Color.MAGENTA).getColor())
        .as("the constructor's color is the starting value")
        .isEqualTo(Color.MAGENTA);
  }

  @Test
  void anOpaqueColorRoundTrips() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setColor(new Color(12, 34, 56));

    assertThat(picker.getColor()).as("what goes in comes out").isEqualTo(new Color(12, 34, 56));
  }

  @Test
  void alphaIsStrippedWhileTheAlphaChannelIsOff() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setColor(new Color(12, 34, 56, 128));

    assertThat(picker.getColor().getAlpha())
        .as("a picker with no alpha channel must not hand back a translucent color")
        .isEqualTo(255);
    assertThat(picker.getColor().getRGB() & 0xFFFFFF)
        .as("while the RGB it was given survives")
        .isEqualTo(new Color(12, 34, 56).getRGB() & 0xFFFFFF);
  }

  @Test
  void alphaIsKeptOnceTheChannelIsEnabled() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setAlphaEnabled(true);

    picker.setColor(new Color(12, 34, 56, 128));

    assertThat(picker.getColor().getAlpha()).as("translucency is now meaningful").isEqualTo(128);
  }

  // ---------------------------------------------------------- the listeners

  @Test
  void aChangeListenerHearsEveryCommit() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final int[] changes = {0};
    picker.addChangeListener(e -> changes[0]++);

    picker.setColor(Color.GREEN);
    picker.setColor(Color.BLUE);

    assertThat(changes[0]).as("one notification per commit").isEqualTo(2);
  }

  @Test
  void reSettingTheSameColorFiresNothing() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final int[] changes = {0};
    picker.addChangeListener(e -> changes[0]++);

    picker.setColor(Color.RED);

    assertThat(changes[0]).as("a no-op commit is not a change").isZero();
  }

  @Test
  void aRemovedListenerStopsHearing() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final int[] changes = {0};
    final ChangeListener listener = e -> changes[0]++;
    picker.addChangeListener(listener);

    picker.removeChangeListener(listener);
    picker.setColor(Color.GREEN);

    assertThat(changes[0]).as("removal takes effect").isZero();
  }

  @Test
  void aCommitFromOnePaneReachesEveryOtherPane() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final WheelPane wheel = (WheelPane) picker.paneFor(PickerMode.WHEEL);
    final SpectrumPane spectrum = (SpectrumPane) picker.paneFor(PickerMode.SPECTRUM);

    wheel.hueSatTo(240f, 1f, false);

    assertThat(spectrum.hueDegrees())
        .as("the panes are views on one model, so a wheel edit moves the spectrum too")
        .isCloseTo(240f, org.assertj.core.api.Assertions.within(2f));
  }

  @Test
  void aPaneDoesNotReceiveItsOwnCommitBack() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final WheelPane wheel = (WheelPane) picker.paneFor(PickerMode.WHEEL);

    wheel.hueSatTo(200f, 0f, true);

    assertThat(wheel.hueDegrees())
        .as("echoing a commit back to its source would snap the hue the source is mid-edit on")
        .isEqualTo(200f);
  }

  // ------------------------------------------------------------- the modes

  @Test
  void modeListCanBeNarrowed() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setModes(PickerMode.SWATCHES, PickerMode.SLIDERS);

    assertThat(picker.getModes())
        .as("a consumer can ship only the modes its users need")
        .containsExactly(PickerMode.SWATCHES, PickerMode.SLIDERS);
    assertThat(picker.getMode()).as("opening on the first survivor").isEqualTo(PickerMode.SWATCHES);
  }

  @ParameterizedTest
  @EnumSource(PickerMode.class)
  void everyModeCanBeSelected(final PickerMode mode) {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setMode(mode);

    assertThat(picker.getMode()).as("%s round-trips", mode).isEqualTo(mode);
    assertThat(picker.paneFor(mode)).as("and has a pane behind it").isNotNull();
  }

  @Test
  void selectingAModeThatIsNotOfferedIsRejected() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setModes(PickerMode.SWATCHES);

    assertThatThrownBy(() -> picker.setMode(PickerMode.WHEEL))
        .as(
            "asking for a mode the consumer removed is a programming error, not a preference —"
                + " unlike the select field's mode axes, which force rather than throw")
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(picker.getMode())
        .as("and the picker is left as it was")
        .isEqualTo(PickerMode.SWATCHES);
  }

  @Test
  void switchingModesPreservesTheColor() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(10, 120, 200));

    picker.setMode(PickerMode.WHEEL);
    picker.setMode(PickerMode.SLIDERS);

    assertThat(picker.getColor())
        .as("the mode is a view onto one value, so changing views changes nothing")
        .isEqualTo(new Color(10, 120, 200));
  }

  @Test
  void anEmptyModeListIsRefusedRatherThanLeavingAnEmptyPicker() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    assertThatThrownBy(picker::setModes)
        .as("a picker with no modes would have nothing to show — better to fail at the call site")
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(picker.getModes()).as("leaving the picker usable").isNotEmpty();
  }

  // ------------------------------------------------------ the swatch tiers

  @Test
  void swatchSourceListCanBeNarrowed() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setSwatchSources(SwatchSource.THEME);

    assertThat(picker.getSwatchSources())
        .as("a consumer can offer only its theme's colors")
        .containsExactly(SwatchSource.THEME);
    assertThat(picker.getSwatchSource())
        .as("and the sub-toggle opens on it")
        .isEqualTo(SwatchSource.THEME);
  }

  @ParameterizedTest
  @EnumSource(SwatchSource.class)
  void everySwatchSourceCanBeSelected(final SwatchSource source) {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setSwatchSource(source);

    assertThat(picker.getSwatchSource()).as("%s round-trips", source).isEqualTo(source);
  }

  @Test
  void selectingASwatchSourceThatIsNotOfferedIsRejected() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setSwatchSources(SwatchSource.MATERIAL);

    assertThatThrownBy(() -> picker.setSwatchSource(SwatchSource.SAVED))
        .as("a removed tier stays unreachable, and says so loudly")
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(picker.getSwatchSource())
        .as("with the picker unchanged")
        .isEqualTo(SwatchSource.MATERIAL);
  }

  // -------------------------------------------------------- the favorites

  @Test
  void aFavoriteIsStoredAndReadBack() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.addFavorite(Color.CYAN);

    assertThat(picker.getFavorites())
        .as("the saved tier holds what was added")
        .contains(Color.CYAN);
  }

  @Test
  void aFavoriteIsNotStoredTwice() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.addFavorite(Color.CYAN);
    picker.addFavorite(Color.CYAN);

    assertThat(picker.getFavorites())
        .as("saving a color already saved is a no-op, not a duplicate row")
        .hasSize(1);
  }

  @Test
  void aFavoriteCanBeRemoved() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.addFavorite(Color.CYAN);

    picker.removeFavorite(Color.CYAN);

    assertThat(picker.getFavorites()).as("and unsaved again").doesNotContain(Color.CYAN);
  }

  @Test
  void favoritesListCanBeReplacedWholesale() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.addFavorite(Color.CYAN);

    picker.setFavorites(List.of(Color.RED, Color.GREEN));

    assertThat(picker.getFavorites())
        .as("a consumer restoring a persisted list replaces rather than merges")
        .containsExactly(Color.RED, Color.GREEN);
  }

  @Test
  void favoritesSnapshotIsNotAliveToTheCaller() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    final List<Color> seed = new ArrayList<>(List.of(Color.RED));
    picker.setFavorites(seed);

    seed.clear();

    assertThat(picker.getFavorites())
        .as("the picker copies the caller's list rather than aliasing it")
        .containsExactly(Color.RED);
  }

  @Test
  void aFavoritesListenerHearsEveryChange() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    final int[] changes = {0};
    picker.addFavoritesListener(e -> changes[0]++);

    picker.addFavorite(Color.CYAN);
    picker.removeFavorite(Color.CYAN);

    assertThat(changes[0])
        .as("a consumer persisting the saved tier needs to hear both directions")
        .isEqualTo(2);
  }

  @Test
  void aFavoritesListenerCanBeRemoved() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    final int[] changes = {0};
    final ChangeListener listener = e -> changes[0]++;
    picker.addFavoritesListener(listener);

    picker.removeFavoritesListener(listener);
    picker.addFavorite(Color.CYAN);

    assertThat(changes[0]).as("removal takes effect").isZero();
  }

  // -------------------------------------------------------------- recents

  @Test
  void aCommittedColorJoinsTheRecentList() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);

    picker.setColor(Color.GREEN);

    assertThat(picker.recentColors())
        .as("a committed color is offered back for reuse, most recent first")
        .startsWith(Color.GREEN);
  }

  @Test
  void reCommittingAColorMovesItToTheFrontRatherThanDuplicating() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    picker.setColor(Color.GREEN);
    picker.setColor(Color.BLUE);

    picker.setColor(Color.GREEN);

    assertThat(picker.recentColors()).as("most recent first").startsWith(Color.GREEN);
    assertThat(picker.recentColors().stream().filter(Color.GREEN::equals).count())
        .as("and only once")
        .isEqualTo(1);
  }

  @Test
  void recentListIsBounded() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    for (int i = 0; i < 40; i++) {
      picker.setColor(new Color(i * 5, 20, 30));
    }

    assertThat(picker.recentColors())
        .as("an unbounded recents strip would grow past the picker's width")
        .hasSizeLessThan(20);
  }

  // ---------------------------------------------------------- the disabled

  @Test
  void disablingThePickerDisablesItsPanes() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setEnabled(false);

    assertThat(picker.isEnabled()).as("the picker is disabled").isFalse();
    assertThat(picker.paneFor(PickerMode.WHEEL).isEnabled())
        .as("and so is every pane inside it")
        .isFalse();
  }

  @Test
  void eyedropperRefusesToOpenWhileDisabled() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setEyedropperEnabled(true);
    picker.setEnabled(false);

    assertThatCode(picker::openEyedropper)
        .as("a disabled picker cannot start a screen sample")
        .doesNotThrowAnyException();
    assertThat(picker.isSamplerOpen()).as("so no sampler window is created").isFalse();
  }

  @Test
  void eyedropperRefusesToOpenWhileTurnedOff() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    picker.setEyedropperEnabled(false);

    picker.openEyedropper();

    assertThat(picker.isSamplerOpen())
        .as("the feature toggle gates the gesture, not just the button's visibility")
        .isFalse();
    assertThat(picker.isEyedropperEnabled()).as("and reads back off").isFalse();
  }

  @Test
  void supportingTextRoundTrips() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();

    picker.setSupportingText("Pick a brand color");

    assertThat(picker.getSupportingText())
        .as("the helper line is the consumer's")
        .isEqualTo("Pick a brand color");
  }

  @Test
  void hexReadoutFormatsTheCurrentColor() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0x1A, 0x2B, 0x3C));

    assertThat(picker.formatCurrentHex().toUpperCase(java.util.Locale.ROOT))
        .as("the readout is the canonical six-digit hex of the committed color")
        .contains("1A2B3C");
  }
}
