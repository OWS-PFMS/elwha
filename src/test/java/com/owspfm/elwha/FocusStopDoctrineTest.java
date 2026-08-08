package com.owspfm.elwha;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.appbar.ElwhaAppBar;
import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardActions;
import com.owspfm.elwha.card.ElwhaCardDivider;
import com.owspfm.elwha.card.ElwhaCardExpandLink;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardLeadingIcon;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.card.ElwhaCardSubtitle;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.ElwhaCardThumbnail;
import com.owspfm.elwha.card.ElwhaCardTitle;
import com.owspfm.elwha.chip.ChipInteractionMode;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.colorpicker.ElwhaColorPicker;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.fab.ElwhaFabAnchor;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.loading.ElwhaLoadingIndicator;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.progress.ElwhaCircularProgressIndicator;
import com.owspfm.elwha.progress.ElwhaLinearProgressIndicator;
import com.owspfm.elwha.radio.ElwhaRadioButton;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.sidesheet.ElwhaSideSheet;
import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.tabs.ElwhaTab;
import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A coverage of the focus-stop doctrine (conventions §12, ruled in #688): a component's {@code
 * focusable} flag states whether <em>it</em> operates the keyboard, every component states it
 * explicitly, and the flag agrees with whether the component installs {@code WHEN_FOCUSED}
 * bindings.
 *
 * <p>The failure this pins is a tab stop that does nothing. {@code
 * LayoutFocusTraversalPolicy.accept} admits a component that has {@code WHEN_FOCUSED} bindings
 * <em>or</em>, failing that, one on which anybody ever called {@code setFocusable} — so {@code
 * setFocusable(true)} on binding-less chrome bypasses the binding test and manufactures an inert
 * stop between the controls a consumer is actually tabbing toward. Declaring the flag is what turns
 * a correct-by-default into a contract; {@link #everyTabStopOperatesTheKeyboard} is what keeps the
 * two halves from drifting apart.
 *
 * <p>Parameterized rather than written per component because the doctrine is under test, not any
 * one component's traversal: a new primitive belongs in one of the two lists below.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/688">#688</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class FocusStopDoctrineTest {

  /** Chrome, wrappers, and decorators — painted structure that operates no keyboard of its own. */
  private static Stream<Arguments> inertChrome() {
    return Stream.of(
        // The five #688 named: a shared base plus four decorators over an embedded focusable.
        Arguments.of("ElwhaSurface", (Supplier<JComponent>) ElwhaSurface::new),
        Arguments.of("ElwhaTextField", (Supplier<JComponent>) () -> new ElwhaTextField("Label")),
        Arguments.of(
            "ElwhaSelectField", (Supplier<JComponent>) () -> new ElwhaSelectField<String>("Label")),
        Arguments.of("ElwhaColorPicker", (Supplier<JComponent>) ElwhaColorPicker::new),
        Arguments.of("ElwhaSideSheet", (Supplier<JComponent>) () -> new ElwhaSideSheet("Title")),
        // The two the #688 sweep turned up that the issue had not listed.
        Arguments.of("ElwhaNavigationRail", (Supplier<JComponent>) ElwhaNavigationRail::collapsed),
        Arguments.of(
            "ElwhaFabAnchor",
            (Supplier<JComponent>)
                () -> new ElwhaFabAnchor(new JPanel(), ElwhaFab.standard(MaterialIcons.add()))),
        // Chrome that already declared it — here so the doctrine, not the sweep, is the contract.
        Arguments.of("ElwhaAppBar", (Supplier<JComponent>) ElwhaAppBar::small),
        Arguments.of("ElwhaTabs", (Supplier<JComponent>) ElwhaTabs::new),
        Arguments.of(
            "ElwhaButtonGroup",
            (Supplier<JComponent>) () -> ElwhaButtonGroup.standard().add("One", "Two")),
        Arguments.of("ElwhaBadge", (Supplier<JComponent>) () -> ElwhaBadge.large(8)),
        Arguments.of("ElwhaLoadingIndicator", (Supplier<JComponent>) ElwhaLoadingIndicator::new),
        Arguments.of(
            "ElwhaLinearProgressIndicator",
            (Supplier<JComponent>) ElwhaLinearProgressIndicator::new),
        Arguments.of(
            "ElwhaCircularProgressIndicator",
            (Supplier<JComponent>) ElwhaCircularProgressIndicator::new),
        Arguments.of("ElwhaMenuItem", (Supplier<JComponent>) () -> ElwhaMenuItem.of("Item")),
        Arguments.of("ElwhaCard (resting)", (Supplier<JComponent>) ElwhaCard::new),
        Arguments.of("ElwhaChip (static)", (Supplier<JComponent>) () -> new ElwhaChip("Tag")),
        Arguments.of("ElwhaCardTitle", (Supplier<JComponent>) () -> new ElwhaCardTitle("Title")),
        Arguments.of(
            "ElwhaCardSubtitle", (Supplier<JComponent>) () -> new ElwhaCardSubtitle("Subtitle")),
        Arguments.of(
            "ElwhaCardSupportingText",
            (Supplier<JComponent>) () -> new ElwhaCardSupportingText("Body")),
        Arguments.of(
            "ElwhaCardLeadingIcon",
            (Supplier<JComponent>) () -> new ElwhaCardLeadingIcon(MaterialIcons.add())),
        Arguments.of(
            "ElwhaCardThumbnail", (Supplier<JComponent>) () -> new ElwhaCardThumbnail(swatch())),
        Arguments.of("ElwhaCardHeader", (Supplier<JComponent>) ElwhaCardHeader::new),
        Arguments.of("ElwhaCardMedia", (Supplier<JComponent>) () -> ElwhaCardMedia.image(swatch())),
        Arguments.of("ElwhaCardActions", (Supplier<JComponent>) ElwhaCardActions::new),
        Arguments.of("ElwhaCardDivider", (Supplier<JComponent>) ElwhaCardDivider::new));
  }

  /** Components that operate the keyboard, and so legitimately own a stop. */
  private static Stream<Arguments> keyboardOperated() {
    return Stream.of(
        Arguments.of("ElwhaButton", (Supplier<JComponent>) () -> new ElwhaButton("Save")),
        Arguments.of("ElwhaIconButton", (Supplier<JComponent>) ElwhaIconButton::new),
        Arguments.of(
            "ElwhaFab", (Supplier<JComponent>) () -> ElwhaFab.standard(MaterialIcons.add())),
        Arguments.of(
            "ElwhaCheckbox", (Supplier<JComponent>) com.owspfm.elwha.checkbox.ElwhaCheckbox::new),
        Arguments.of("ElwhaRadioButton", (Supplier<JComponent>) ElwhaRadioButton::new),
        Arguments.of("ElwhaSwitch", (Supplier<JComponent>) ElwhaSwitch::new),
        Arguments.of("ElwhaSlider", (Supplier<JComponent>) () -> new ElwhaSlider(0, 100, 50)),
        Arguments.of("ElwhaTab", (Supplier<JComponent>) () -> ElwhaTab.of("Tab")),
        Arguments.of(
            "ElwhaNavRailDestination",
            (Supplier<JComponent>)
                () -> ElwhaNavRailDestination.of(MaterialIcons.symbol("home"), "Home")),
        Arguments.of(
            "ElwhaCardExpandLink",
            (Supplier<JComponent>)
                () -> new ElwhaCardExpandLink(new ElwhaCard(), "Show more", "Show less")));
  }

  @ParameterizedTest(name = "{0} declines the tab stop")
  @MethodSource("inertChrome")
  void chromeDeclinesTheTabStop(final String name, final Supplier<JComponent> factory) {
    assertThat(factory.get().isFocusable())
        .as("%s operates no keyboard of its own, so it must not take a tab stop (§12)", name)
        .isFalse();
  }

  @ParameterizedTest(name = "{0} owns a tab stop")
  @MethodSource("keyboardOperated")
  void keyboardOperatedComponentsOwnATabStop(
      final String name, final Supplier<JComponent> factory) {
    assertThat(factory.get().isFocusable())
        .as("%s responds to keystrokes, so it must be reachable by Tab (§12)", name)
        .isTrue();
  }

  /**
   * The half that catches the real defect: a component may only claim a stop if it can do something
   * with it. Mirrors {@code LayoutFocusTraversalPolicy}'s own test, walking the map's parent chain
   * because a component's own map is usually an empty child of the UI-installed one.
   */
  @ParameterizedTest(name = "{0} backs its tab stop with bindings")
  @MethodSource("keyboardOperated")
  void everyTabStopOperatesTheKeyboard(final String name, final Supplier<JComponent> factory) {
    assertThat(bindsKeys(factory.get()))
        .as(
            "%s is focusable, so it must install WHEN_FOCUSED bindings — an inert stop is the "
                + "defect §12 exists to prevent",
            name)
        .isTrue();
  }

  @Test
  void theCardTracksItsActionableBindings() {
    final ElwhaCard card = new ElwhaCard();
    assertThat(card.isFocusable()).as("a resting card is chrome").isFalse();

    card.setActionable(true);
    assertThat(card.isFocusable()).as("an actionable card binds Space/Enter").isTrue();
    assertThat(bindsKeys(card)).as("and the bindings are really there").isTrue();

    card.setActionable(false);
    assertThat(card.isFocusable()).as("and gives the stop back with the bindings").isFalse();
  }

  @Test
  void theChipTracksItsInteractionMode() {
    final ElwhaChip chip = new ElwhaChip("Tag");
    assertThat(chip.isFocusable()).as("a static chip is chrome").isFalse();

    chip.setInteractionMode(ChipInteractionMode.CLICKABLE);
    assertThat(chip.isFocusable()).as("a clickable chip is operable").isTrue();

    chip.setInteractionMode(ChipInteractionMode.SELECTABLE);
    assertThat(chip.isFocusable()).as("so is a selectable one").isTrue();

    chip.setInteractionMode(ChipInteractionMode.STATIC);
    assertThat(chip.isFocusable()).as("and back to chrome").isFalse();
  }

  @Test
  void theFieldDecoratorsForwardTheFocusRequestTheyDecline() {
    // The wrapper declines the stop, so requestFocusInWindow() would be a silent no-op (§9) unless
    // it forwards. Headless cannot arbitrate real focus, so this pins the delegation target rather
    // than the outcome: the call must reach the editor, not this.
    final ElwhaTextField field = new ElwhaTextField("Label");
    assertThat(field.getEditor().isFocusable())
        .as("the editor is the real target the decorator forwards to")
        .isTrue();
    assertThat(bindsKeys(field.getEditor()))
        .as("and it is the thing that owns the keystrokes")
        .isTrue();
    assertThat(bindsKeys(field)).as("while the chassis owns none").isFalse();
  }

  /** A tiny opaque image, so the image-bearing card slots have something real to hold. */
  private static Image swatch() {
    return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
  }

  private static boolean bindsKeys(final JComponent component) {
    InputMap map = component.getInputMap(JComponent.WHEN_FOCUSED);
    while (map != null && map.size() == 0) {
      map = map.getParent();
    }
    return map != null;
  }
}
