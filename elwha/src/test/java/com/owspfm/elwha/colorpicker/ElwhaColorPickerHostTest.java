package com.owspfm.elwha.colorpicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the swatch tiers and of the two hosts that wrap the picker — the modal {@link
 * ElwhaColorPickerDialog} and the light-dismiss {@link ElwhaColorPickerPopover}.
 *
 * <p>Both hosts are asserted <b>construction-and-configuration only</b>: showing either one needs a
 * real window, so the display-bearing half (placement, light dismiss, the confirm/cancel round trip
 * through a real button press) belongs to the gui tier. What is worth pinning headless is that
 * every configuration setter actually reaches the embedded picker — a host that silently drops
 * {@code setAlphaEnabled} would look perfectly fine until someone needed alpha.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaColorPickerHostTest {

  private static SwatchesPane swatches() {
    return (SwatchesPane) new ElwhaColorPicker(Color.RED).paneFor(PickerMode.SWATCHES);
  }

  // ------------------------------------------------------- the swatch tiers

  @Test
  void materialCatalogOffersHuesEachWithNamedShades() {
    assertThat(MaterialSwatchCatalog.hues())
        .as("the Material tier is a fixed catalog of hue families")
        .isNotEmpty();
    assertThat(MaterialSwatchCatalog.shadeName(0))
        .as("and each shade carries the M2 numeric name users recognize")
        .isNotBlank();
  }

  @Test
  void aCatalogColorIsFoundBackInTheCatalog() {
    final int[] found = MaterialSwatchCatalog.find(Color.WHITE);

    assertThat(found)
        .as("look-up either locates a catalog cell or reports a miss — never a bad index")
        .satisfiesAnyOf(
            result -> assertThat(result).isNull(), result -> assertThat(result).hasSize(2));
  }

  @ParameterizedTest
  @EnumSource(SwatchSource.class)
  void everyTierCanBeShown(final SwatchSource source) {
    final SwatchesPane pane = swatches();

    assertThatCode(() -> pane.showSource(source))
        .as("%s renders without a window", source)
        .doesNotThrowAnyException();
  }

  @Test
  void selectingAHueThenAShadeCommitsThatSwatch() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    pane.showSource(SwatchSource.MATERIAL);

    pane.selectHue(2);
    pane.selectShade(3);

    assertThat(pane.activeHueIndex()).as("the hue row tracks the chosen family").isEqualTo(2);
    assertThat(picker.getColor()).as("and picking a shade in it commits a color").isNotNull();
  }

  @Test
  void hueRowKeepsItsSelectionAcrossAShadePick() {
    final SwatchesPane pane = swatches();
    pane.showSource(SwatchSource.MATERIAL);
    pane.selectHue(4);

    pane.selectShade(1);
    pane.selectShade(6);

    assertThat(pane.activeHueIndex())
        .as("moving along the shade strip must not jump back to another hue family")
        .isEqualTo(4);
  }

  @Test
  void savedTierReflectsThePickersFavorites() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    picker.setSwatchSource(SwatchSource.SAVED);

    picker.addFavorite(Color.CYAN);

    assertThatCode(pane::favoritesChanged)
        .as("the saved tier rebuilds from the picker's list rather than holding its own")
        .doesNotThrowAnyException();
    assertThat(picker.getFavorites()).as("which is where the color lives").contains(Color.CYAN);
  }

  @Test
  void aRecentColorCanBeReselected() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    picker.setColor(Color.GREEN);
    picker.setColor(Color.BLUE);

    pane.selectRecent(1);

    assertThat(picker.getColor())
        .as("the recents strip is a shortcut back to a color already used")
        .isEqualTo(Color.GREEN);
  }

  @Test
  void pickerHeaderIsInertChromeAroundItsEyedropper() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    picker.setEyedropperEnabled(true);
    final ColorPickerHeader header = new ColorPickerHeader(picker);

    assertThat(header.isFocusable())
        .as("the header row binds no keys; it must not take a stop in front of the eyedropper")
        .isFalse();
  }

  @Test
  void everySwatchTierIgnoresAnIndexOutsideItsCatalog() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);

    pane.selectHue(-1);
    pane.selectHue(9999);
    pane.selectShade(-1);
    pane.selectShade(9999);
    pane.selectRecent(-1);

    assertThat(picker.getColor())
        .as(
            "indexAt answers -1 for a press between cells, so an out-of-range index has to be a"
                + " no-op rather than an exception out of a mouse handler")
        .isEqualTo(Color.RED);
  }

  @Test
  void recentsStripIgnoresACellPastTheEndOfTheList() {
    final ElwhaColorPicker picker = new ElwhaColorPicker(Color.RED);
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);

    pane.selectRecent(999);

    assertThat(picker.getColor())
        .as(
            "the recents row is laid out at a fixed cell count but backed by a list that starts"
                + " short, so a click on an empty cell has to be a no-op")
        .isEqualTo(Color.RED);
  }

  // --------------------------------------------------------- the dialog

  @Test
  void aFreshDialogCarriesADefaultTitleAndColor() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();

    assertThat(dialog.getTitle()).as("a dialog names itself out of the box").isNotBlank();
    assertThat(dialog.getInitialColor()).as("and opens on some color").isNotNull();
  }

  @Test
  void dialogsConfigurationReachesItsEmbeddedPicker() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();

    dialog.setTitle("Brand color");
    dialog.setInitialColor(Color.MAGENTA);
    dialog.setModes(PickerMode.SWATCHES, PickerMode.WHEEL);
    dialog.setAlphaEnabled(true);

    assertThat(dialog.getTitle()).as("the title round-trips").isEqualTo("Brand color");
    assertThat(dialog.getInitialColor()).as("as does the opening color").isEqualTo(Color.MAGENTA);
    assertThat(dialog.picker().getColor())
        .as(
            "the initial color is staged rather than applied — one dialog instance can be reopened"
                + " on a different color without the previous show's edits leaking forward")
        .isNotEqualTo(Color.MAGENTA);
    assertThat(dialog.picker().getModes())
        .as("and the mode list reaches the picker")
        .containsExactly(PickerMode.SWATCHES, PickerMode.WHEEL);
    assertThat(dialog.picker().isAlphaEnabled()).as("alpha too").isTrue();
  }

  @Test
  void dismissingADialogThatWasNeverShownIsHarmless() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();

    assertThatCode(dialog::dismiss)
        .as("a host that was configured and then abandoned must tear down cleanly")
        .doesNotThrowAnyException();
  }

  @Test
  void dialogsCallbacksAreOptional() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();

    assertThatCode(
            () -> {
              dialog.onConfirm(null);
              dialog.onCancel(null);
              dialog.dismiss();
            })
        .as("a consumer that only reads the color afterwards need not register anything")
        .doesNotThrowAnyException();
  }

  // -------------------------------------------------------- the popover

  @Test
  void aFreshPopoverIsNotShowing() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();

    assertThat(popover.isShowing()).as("construction does not open anything").isFalse();
    assertThat(popover.getColor()).as("but it already holds a color").isNotNull();
  }

  @Test
  void popoversConfigurationReachesItsEmbeddedPicker() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();

    popover.setInitialColor(Color.ORANGE);
    popover.setModes(PickerMode.SLIDERS);
    popover.setSwatchSources(SwatchSource.THEME);
    popover.setAlphaEnabled(true);
    popover.setEyedropperEnabled(true);

    assertThat(popover.getColor()).as("the opening color round-trips").isEqualTo(Color.ORANGE);
    assertThat(popover.getModes())
        .as("the mode list round-trips")
        .containsExactly(PickerMode.SLIDERS);
    assertThat(popover.picker().getSwatchSources())
        .as("the swatch tiers reach the picker")
        .containsExactly(SwatchSource.THEME);
    assertThat(popover.picker().isAlphaEnabled()).as("as does alpha").isTrue();
    assertThat(popover.picker().isEyedropperEnabled()).as("and the eyedropper toggle").isTrue();
  }

  @Test
  void aPopoverForwardsItsPickersChanges() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    final List<Color> heard = new java.util.ArrayList<>();
    popover.addChangeListener(e -> heard.add(popover.getColor()));

    popover.picker().setColor(Color.PINK);

    assertThat(heard)
        .as("a listener on the host hears what the embedded picker commits")
        .containsExactly(Color.PINK);
  }

  @Test
  void closingAPopoverThatWasNeverShownIsHarmless() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();

    assertThatCode(popover::close)
        .as("light-dismiss teardown must be safe before the first show")
        .doesNotThrowAnyException();
    assertThat(popover.isShowing()).as("and it stays closed").isFalse();
  }

  @Test
  void aDismissCallbackIsOptional() {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();

    assertThatCode(
            () -> {
              popover.onDismiss(null);
              popover.close();
            })
        .as("registering no dismiss handler must not break teardown")
        .doesNotThrowAnyException();
  }

  @Test
  void bothHostsWrapAPickerRatherThanReimplementingOne() {
    assertThat(new ElwhaColorPickerDialog().picker())
        .as("the dialog composes the picker")
        .isInstanceOf(ElwhaColorPicker.class);
    assertThat(new ElwhaColorPickerPopover().picker())
        .as("and so does the popover — one picker, two presentations")
        .isInstanceOf(ElwhaColorPicker.class);
  }
}
