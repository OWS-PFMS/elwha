package com.owspfm.elwha;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.appbar.ElwhaAppBar;
import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardDivider;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.card.ElwhaCardSubtitle;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.ElwhaCardThumbnail;
import com.owspfm.elwha.card.ElwhaCardTitle;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.colorpicker.ElwhaColorPicker;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.slider.ElwhaSlider;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.tabs.ElwhaTab;
import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.swing.JComponent;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tier A coverage of the sizing-hook escape doctrine (#567): a component that overrides {@code
 * getPreferredSize} / {@code getMinimumSize} / {@code getMaximumSize} to publish its own M3
 * geometry must still stand down when a consumer sets one explicitly.
 *
 * <p>The failure this pins is silent. {@code setPreferredSize} records the value on {@code
 * JComponent} and flips {@code isPreferredSizeSet()}, but an unconditional override never reads
 * either — so the call appears to succeed, the layout manager asks the component, and the component
 * answers with its own number. No exception, no warning, just a size the consumer did not ask for.
 *
 * <p>Every component below was found by the #440 Phase 1 scan to be missing the escape on at least
 * one hook. The sweep is parameterized rather than written per component because the doctrine is
 * what is under test, not any one component's geometry: a new primitive that overrides a sizing
 * hook belongs in this list.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/567">#567</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class SizingHookEscapeTest {

  /** Deliberately unlike any component's natural geometry, so a stale answer cannot coincide. */
  private static final Dimension EXPLICIT = new Dimension(321, 177);

  private static Stream<Arguments> components() {
    return Stream.of(
        Arguments.of("ElwhaSwitch", (Supplier<JComponent>) ElwhaSwitch::new),
        Arguments.of("ElwhaTextField", (Supplier<JComponent>) () -> new ElwhaTextField("Label")),
        Arguments.of(
            "ElwhaSelectField", (Supplier<JComponent>) () -> new ElwhaSelectField<String>("Label")),
        Arguments.of("ElwhaTabs", (Supplier<JComponent>) ElwhaTabs::new),
        Arguments.of("ElwhaTab", (Supplier<JComponent>) () -> ElwhaTab.of("Tab")),
        Arguments.of("ElwhaBadge", (Supplier<JComponent>) () -> ElwhaBadge.large(8)),
        Arguments.of(
            "ElwhaNavRailDestination",
            (Supplier<JComponent>)
                () -> ElwhaNavRailDestination.of(MaterialIcons.symbol("home"), "Home")),
        Arguments.of("ElwhaAppBar", (Supplier<JComponent>) ElwhaAppBar::small),
        // #712's ruling pass — every remaining consumer-reachable site.
        Arguments.of("ElwhaButton", (Supplier<JComponent>) () -> new ElwhaButton("Save")),
        Arguments.of("ElwhaIconButton", (Supplier<JComponent>) ElwhaIconButton::new),
        Arguments.of(
            "ElwhaButtonGroup",
            (Supplier<JComponent>) () -> ElwhaButtonGroup.standard().add("One", "Two")),
        Arguments.of(
            "ElwhaFab", (Supplier<JComponent>) () -> ElwhaFab.standard(MaterialIcons.add())),
        Arguments.of("ElwhaChip", (Supplier<JComponent>) () -> new ElwhaChip("Tag")),
        Arguments.of("ElwhaSlider", (Supplier<JComponent>) () -> new ElwhaSlider(0, 100, 50)),
        Arguments.of("ElwhaMenuItem", (Supplier<JComponent>) () -> ElwhaMenuItem.of("Item")),
        Arguments.of("ElwhaColorPicker", (Supplier<JComponent>) ElwhaColorPicker::new),
        Arguments.of("ElwhaCard", (Supplier<JComponent>) ElwhaCard::filledCard),
        Arguments.of("ElwhaCardTitle", (Supplier<JComponent>) () -> new ElwhaCardTitle("Title")),
        Arguments.of(
            "ElwhaCardSubtitle", (Supplier<JComponent>) () -> new ElwhaCardSubtitle("Subtitle")),
        Arguments.of(
            "ElwhaCardSupportingText",
            (Supplier<JComponent>) () -> new ElwhaCardSupportingText("Body")),
        Arguments.of("ElwhaCardDivider", (Supplier<JComponent>) ElwhaCardDivider::new),
        Arguments.of(
            "ElwhaCardThumbnail", (Supplier<JComponent>) () -> new ElwhaCardThumbnail(swatch())),
        Arguments.of(
            "ElwhaCardMedia", (Supplier<JComponent>) () -> ElwhaCardMedia.image(swatch())));
  }

  /** A tiny opaque image, so the image-bearing card slots measure something real. */
  private static Image swatch() {
    return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
  }

  @ParameterizedTest(name = "{0} honours an explicit preferred size")
  @MethodSource("components")
  void anExplicitPreferredSizeWins(final String name, final Supplier<JComponent> factory) {
    final JComponent component = factory.get();
    final Dimension natural = component.getPreferredSize();

    component.setPreferredSize(EXPLICIT);

    assertThat(component.getPreferredSize())
        .as("%s discards a caller-set preferred size (natural was %s)", name, natural)
        .isEqualTo(EXPLICIT);
  }

  @ParameterizedTest(name = "{0} honours an explicit minimum size")
  @MethodSource("components")
  void anExplicitMinimumSizeWins(final String name, final Supplier<JComponent> factory) {
    final JComponent component = factory.get();
    final Dimension natural = component.getMinimumSize();

    component.setMinimumSize(EXPLICIT);

    assertThat(component.getMinimumSize())
        .as("%s discards a caller-set minimum size (natural was %s)", name, natural)
        .isEqualTo(EXPLICIT);
  }

  @ParameterizedTest(name = "{0} honours an explicit maximum size")
  @MethodSource("components")
  void anExplicitMaximumSizeWins(final String name, final Supplier<JComponent> factory) {
    final JComponent component = factory.get();
    final Dimension natural = component.getMaximumSize();

    component.setMaximumSize(EXPLICIT);

    assertThat(component.getMaximumSize())
        .as("%s discards a caller-set maximum size (natural was %s)", name, natural)
        .isEqualTo(EXPLICIT);
  }

  /**
   * The escape must not fire on its own — a component with no explicit size still publishes its M3
   * geometry. Without this the sweep above would pass against an override deleted outright.
   */
  @ParameterizedTest(name = "{0} still publishes its own geometry when nothing is set")
  @MethodSource("components")
  void anUnsetComponentKeepsItsOwnGeometry(final String name, final Supplier<JComponent> factory) {
    final JComponent component = factory.get();

    assertThat(component.isPreferredSizeSet())
        .as("%s reports no explicit preferred size before one is set", name)
        .isFalse();
    assertThat(component.getPreferredSize())
        .as("%s publishes a non-empty natural preferred size", name)
        .isNotEqualTo(EXPLICIT)
        .isNotEqualTo(new Dimension(0, 0));
  }
}
