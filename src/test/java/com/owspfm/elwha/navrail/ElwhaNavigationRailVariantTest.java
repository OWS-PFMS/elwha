package com.owspfm.elwha.navrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the rail's two variants and the transition between them — design doc §3 (the
 * container width tables), §9 (the Collapsed↔Expanded morph), §4.3 (the menu button the rail owns
 * and drives), and the Expanded-only section rendering.
 *
 * <p>The morph is asserted at <em>injected</em> progress through the destination's {@code
 * setMorphProgress} seam rather than by racing the rail's animator: reduced motion is pinned by the
 * harness, so a live {@code morphTo} settles instantly and would only ever show end states.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaNavigationRailVariantTest {

  // -------------------------------------------------------------- container

  @Test
  void aCollapsedRailIsTheFixedM3Width() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);

    assertThat(rail.getVariant()).isEqualTo(ElwhaNavigationRail.Variant.COLLAPSED);
    assertThat(rail.getPreferredSize().width)
        .as("§3 — Collapsed is a fixed 96 dp column")
        .isEqualTo(ElwhaNavigationRail.COLLAPSED_WIDTH_PX);
  }

  @Test
  void anExpandedRailTakesItsConfiguredWidth() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.expanded(), 4);

    assertThat(rail.getVariant()).isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
    assertThat(rail.getPreferredSize().width)
        .as("§3 — Expanded defaults to 256 dp")
        .isEqualTo(ElwhaNavigationRail.EXPANDED_WIDTH_DEFAULT_PX);
  }

  @ParameterizedTest
  @ValueSource(ints = {220, 300, 360})
  void anExpandedWidthInsideTheM3RangeIsAccepted(final int width) {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();

    rail.setExpandedWidth(width);

    assertThat(rail.getExpandedWidth()).isEqualTo(width);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 100, 219, 361, 9000})
  void anExpandedWidthOutsideTheM3RangeIsRejected(final int width) {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .as(
            "§3 — outside 220–360 dp it stops reading as a rail, so the setter refuses rather "
                + "than quietly clamping to something the caller did not ask for")
        .isThrownBy(() -> rail.setExpandedWidth(width));
  }

  @Test
  void expandedWidthOnlyAffectsTheExpandedVariant() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);

    rail.setExpandedWidth(340);

    assertThat(rail.getPreferredSize().width)
        .as("§3 — a Collapsed rail is 96 dp regardless of what Expanded is configured to")
        .isEqualTo(ElwhaNavigationRail.COLLAPSED_WIDTH_PX);
  }

  @Test
  void switchingVariantSnapsTheContainerWidth() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(rail.getPreferredSize().width)
        .as("§9 — setVariant is the un-animated path")
        .isEqualTo(ElwhaNavigationRail.EXPANDED_WIDTH_DEFAULT_PX);
    assertThat(rail.isMorphing()).as("and it starts no animation").isFalse();
  }

  @Test
  void aVariantChangeIsAnnouncedAsAProperty() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    rail.addPropertyChangeListener(ElwhaNavigationRail.PROPERTY_VARIANT, events::add);

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(events).as("consumer UI syncs its own state off this").hasSize(1);
    assertThat(events.get(0).getNewValue()).isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
  }

  @Test
  void reSettingTheSameVariantAnnouncesNothing() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    rail.addPropertyChangeListener(ElwhaNavigationRail.PROPERTY_VARIANT, events::add);

    rail.setVariant(ElwhaNavigationRail.Variant.COLLAPSED);

    assertThat(events).isEmpty();
  }

  @Test
  void everyDestinationIsToldTheRailsVariant() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);

    for (final ElwhaNavRailDestination destination : rail.getPrimary()) {
      assertThat(destination.getHostVariant())
          .as("§9 — the rail is the single source of truth for layout form")
          .isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
    }
  }

  @Test
  void aDestinationAddedLaterAdoptsTheRailsVariant() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();

    rail.setPrimary(RailFixture.destinations(3));

    assertThat(rail.getPrimary().get(0).getHostVariant())
        .as("§9 — arrivals are pushed the current variant, not a default")
        .isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
  }

  // ------------------------------------------------------ destination form

  @Test
  void aCollapsedDestinationIsAStackedFixedWidthCell() {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");
    destination.setHostVariant(ElwhaNavigationRail.Variant.COLLAPSED);

    assertThat(destination.getPreferredSize().width)
        .as("§3 — Collapsed destinations are the rail's own 96 dp column")
        .isEqualTo(ElwhaNavRailDestination.COLLAPSED_WIDTH_PX);
    assertThat(destination.getPreferredSize().height)
        .isEqualTo(ElwhaNavRailDestination.COLLAPSED_CONTENT_HEIGHT_PX);
  }

  @Test
  void anExpandedDestinationHugsItsLabel() {
    final ElwhaNavRailDestination shortLabel = RailFixture.destination("Hi");
    final ElwhaNavRailDestination longLabel = RailFixture.destination("A much longer label");
    shortLabel.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);
    longLabel.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(longLabel.getPreferredSize().width)
        .as("§3 — Expanded rows are hug-width, so a longer label asks for more")
        .isGreaterThan(shortLabel.getPreferredSize().width);
    assertThat(longLabel.getPreferredSize().height)
        .as("§3 — but the row height is fixed either way")
        .isEqualTo(ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX);
  }

  @Test
  void anExpandedDestinationStretchesToTheRowWhileCollapsedDoesNot() {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");

    destination.setHostVariant(ElwhaNavigationRail.Variant.COLLAPSED);
    assertThat(destination.getMaximumSize().width)
        .as("§3 — a Collapsed cell is exactly its column, never wider")
        .isEqualTo(destination.getPreferredSize().width);

    destination.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);
    assertThat(destination.getMaximumSize().width)
        .as("§3 — an Expanded row fills the rail's width so its indicator can span it")
        .isEqualTo(Integer.MAX_VALUE);
  }

  // ------------------------------------------------ the unparented-font fallback

  @Test
  void anUnparentedExpandedDestinationStillMeasuresItsLabel() {
    final ElwhaNavRailDestination detached =
        ElwhaNavRailDestination.of(com.owspfm.elwha.icons.MaterialIcons.symbol("home"), "Home");
    detached.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(detached.getParent()).as("nothing to inherit a font from").isNull();
    assertThat(detached.getPreferredSize().width)
        .as(
            "a component never added to a container has a null font, which is exactly the state "
                + "an offscreen render measures in; the label role supplies the family instead")
        .isGreaterThan(
            ElwhaNavRailDestination.LEADING_PAD_EXPANDED
                + ElwhaNavRailDestination.ICON_SIZE_PX
                + ElwhaNavRailDestination.TRAILING_PAD_EXPANDED);
  }

  @Test
  void anUnparentedDestinationStillPaintsItsLabel() {
    final ElwhaNavRailDestination detached =
        ElwhaNavRailDestination.of(com.owspfm.elwha.icons.MaterialIcons.symbol("home"), "Home");
    detached.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(PaintLog.capture(detached).painted("Home"))
        .as("and it renders rather than silently dropping the text")
        .isTrue();
  }

  @Test
  void anUnparentedRailStillPaintsItsSectionHeaders() {
    final ElwhaNavigationRail detached = ElwhaNavigationRail.expanded();
    detached.setPrimary(RailFixture.destinations(2));
    detached.addSection("Tools", RailFixture.destinations(2));
    detached.setSize(detached.getPreferredSize());
    detached.doLayout();

    assertThat(detached.getParent()).as("nothing to inherit a font from").isNull();
    assertThat(
            PaintLog.capture(detached, detached.getWidth(), detached.getHeight()).painted("Tools"))
        .as("a null inherited font used to skip the header paint outright")
        .isTrue();
  }

  @Test
  void anUnparentedRailReservesTheHeightItActuallyPaints() {
    final ElwhaNavigationRail withSection = ElwhaNavigationRail.expanded();
    withSection.setPrimary(RailFixture.destinations(2));
    withSection.addSection("Tools", RailFixture.destinations(1));
    final ElwhaNavigationRail withoutSection = ElwhaNavigationRail.expanded();
    withoutSection.setPrimary(RailFixture.destinations(3));

    assertThat(withSection.getPreferredSize().height)
        .as(
            "the reserved header height and the painted header now read one font, so the space "
                + "set aside cannot disagree with the glyphs drawn into it")
        .isGreaterThan(withoutSection.getPreferredSize().height);
  }

  // ------------------------------------------------- the morph, at injected progress

  @Test
  void reducedMotionIsPinnedSoNoMorphAssertionRacesTheAnimator() {
    assertThat(MorphAnimator.isReducedMotion()).isTrue();
  }

  @Test
  void aMorphAtZeroProgressIsStillTheStartingVariant() {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");

    destination.setMorphProgress(
        0f, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(destination.getHostVariant())
        .as("§9 — the endpoints of a morph are the variants themselves")
        .isEqualTo(ElwhaNavigationRail.Variant.COLLAPSED);
  }

  @Test
  void aMorphAtFullProgressCommitsTheTargetVariant() {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");

    destination.setMorphProgress(
        1f, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(destination.getHostVariant())
        .as("§9 — a settled morph leaves the destination in the static end state")
        .isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
  }

  @ParameterizedTest
  @ValueSource(floats = {0.1f, 0.3f, 0.49f})
  void labelAnchorStaysStackedThroughTheFirstHalfOfTheMorph(final float progress) {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");

    destination.setMorphProgress(
        progress, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(destination.getPreferredSize().width)
        .as(
            "§9.2 — the label anchor switches discretely at the halfway point, so before it the "
                + "row is still laid out as a Collapsed cell")
        .isEqualTo(ElwhaNavRailDestination.COLLAPSED_WIDTH_PX);
  }

  @ParameterizedTest
  @ValueSource(floats = {0.5f, 0.7f, 0.9f})
  void labelAnchorSwitchesToInlineInTheSecondHalf(final float progress) {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");

    destination.setMorphProgress(
        progress, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(destination.getPreferredSize().width)
        .as("§9.2 — past the switch the row measures as an Expanded hug-width row")
        .isNotEqualTo(ElwhaNavRailDestination.COLLAPSED_WIDTH_PX);
  }

  @Test
  void aMorphProgressOutOfRangeIsClamped() {
    final ElwhaNavRailDestination destination = RailFixture.destination("Home");

    destination.setMorphProgress(
        5f, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);
    assertThat(destination.getHostVariant()).isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);

    destination.setMorphProgress(
        -5f, ElwhaNavigationRail.Variant.EXPANDED, ElwhaNavigationRail.Variant.COLLAPSED);
    assertThat(destination.getHostVariant())
        .as("§9 — a phase outside [0,1] resolves to the nearer endpoint, never to a third state")
        .isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
  }

  @Test
  void sameInjectedProgressAlwaysProducesTheSameGeometry() {
    final ElwhaNavRailDestination first = RailFixture.destination("Home");
    final ElwhaNavRailDestination second = RailFixture.destination("Home");

    first.setMorphProgress(
        0.75f, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);
    second.setMorphProgress(
        0.75f, ElwhaNavigationRail.Variant.COLLAPSED, ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(first.getPreferredSize())
        .as("the morph is a pure function of phase — no clock, no per-instance drift")
        .isEqualTo(second.getPreferredSize());
  }

  @Test
  void aMorphRunsSymmetricallyInBothDirections() {
    final ElwhaNavRailDestination collapsing = RailFixture.destination("Home");

    collapsing.setMorphProgress(
        1f, ElwhaNavigationRail.Variant.EXPANDED, ElwhaNavigationRail.Variant.COLLAPSED);

    assertThat(collapsing.getHostVariant())
        .as("§9 — progress runs 0→1 regardless of direction; from/to say which way")
        .isEqualTo(ElwhaNavigationRail.Variant.COLLAPSED);
  }

  @Test
  void aSettledMorphLeavesNothingRunning() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);

    rail.morphTo(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(rail.isMorphing())
        .as("§9 — under reduced motion the transition resolves in one step")
        .isFalse();
    assertThat(rail.getVariant()).isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
  }

  // ------------------------------------------------- the menu-button ownership rule

  @Test
  void railOwnsItsMenuButtonAndDrivesTheVariantFromIt() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);
    RailFixture.mount(rail, rail.getPreferredSize().width, 400);

    Input.click(menu, menu.getWidth() / 2, menu.getHeight() / 2);

    assertThat(rail.getVariant())
        .as("§4.3 — the rail wires the menu button itself; the consumer supplies only the button")
        .isEqualTo(ElwhaNavigationRail.Variant.EXPANDED);
  }

  @Test
  void menuGlyphSwapsToTheCollapseAffordanceWhenExpanded() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);
    final Object collapsedGlyph = menu.getIcon();

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(menu.getIcon())
        .as("§4.3 — ☰ means 'expand' in Collapsed; the Expanded rail shows a collapse affordance")
        .isNotSameAs(collapsedGlyph);
  }

  @Test
  void menuGlyphSwapsBackOnCollapse() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.expanded(), 4);
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);
    final Object expandedGlyph = menu.getIcon();

    rail.setVariant(ElwhaNavigationRail.Variant.COLLAPSED);

    assertThat(menu.getIcon()).isNotSameAs(expandedGlyph);
  }

  @Test
  void aRailWithNoMenuButtonIsFixedState() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);

    assertThat(rail.getMenuButton())
        .as("§3 — the menu slot is optional; without it the consumer drives the variant by API")
        .isNull();
    assertThat(rail.getVariant()).isEqualTo(ElwhaNavigationRail.Variant.COLLAPSED);
  }

  @Test
  void menuButtonCanBeCleared() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 4);
    final ElwhaIconButton menu = new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
    rail.setMenuButton(menu);

    rail.setMenuButton(null);

    assertThat(rail.getMenuButton()).isNull();
    assertThat(menu.getParent()).isNull();
  }

  // ------------------------------------------------------------------ sections

  @Test
  void sectionsRenderOnlyInExpanded() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.setPrimary(RailFixture.destinations(3));
    rail.addSection("Tools", RailFixture.destinations(2));
    rail.setSize(rail.getPreferredSize());
    rail.doLayout();

    assertThat(PaintLog.capture(rail, rail.getWidth(), rail.getHeight()).painted("Tools"))
        .as("§3 — a section header has nowhere to sit in a 96 dp column, so Collapsed hides it")
        .isFalse();

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);
    rail.setSize(rail.getPreferredSize());
    rail.doLayout();

    assertThat(PaintLog.capture(rail, rail.getWidth(), rail.getHeight()).painted("Tools"))
        .as("§3 — and Expanded shows it")
        .isTrue();
  }

  @Test
  void sectionDestinationsStayMembersWhileHidden() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();
    rail.setPrimary(RailFixture.destinations(3));
    final List<ElwhaNavRailDestination> secondary = RailFixture.destinations(2);
    rail.addSection("Tools", secondary);

    rail.setSelected(secondary.get(1));

    assertThat(rail.getSelected())
        .as("§3 — hiding a section is a rendering decision; its destinations remain selectable")
        .isSameAs(secondary.get(1));
  }

  @Test
  void sectionsAreReportedInAddOrder() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();
    rail.setPrimary(RailFixture.destinations(2));
    rail.addSection("Tools", RailFixture.destinations(2));
    rail.addSection("Archive", RailFixture.destinations(1));

    assertThat(rail.getSections()).hasSize(2);
    assertThat(rail.getSections().get(0).getHeader()).isEqualTo("Tools");
    assertThat(rail.getSections().get(1).getHeader()).isEqualTo("Archive");
  }

  @Test
  void clearingSectionsLeavesThePrimaryListAlone() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();
    rail.setPrimary(RailFixture.destinations(3));
    rail.addSection("Tools", RailFixture.destinations(2));

    rail.clearSections();

    assertThat(rail.getSections()).isEmpty();
    assertThat(rail.getPrimary()).hasSize(3);
  }

  @Test
  void addingASectionToARailWithNoPrimaryStillLeavesItOneSelection() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();
    final List<ElwhaNavRailDestination> secondary = RailFixture.destinations(2);

    rail.addSection("Tools", secondary);

    assertThat(rail.getSelected())
        .as(
            "#633 — the single-mandatory invariant held for setPrimary but not for addSection, and"
                + " setSelected(null) throws, so the consumer had no way to repair it")
        .isSameAs(secondary.get(0));
  }

  @Test
  void addingASectionDoesNotStealAnExistingSelection() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.expanded();
    final List<ElwhaNavRailDestination> primary = RailFixture.destinations(3);
    rail.setPrimary(primary);
    rail.setSelected(primary.get(2));

    rail.addSection("Tools", RailFixture.destinations(2));

    assertThat(rail.getSelected()).as("the fallback only fills a vacancy").isSameAs(primary.get(2));
  }

  // -------------------------------------------------------------------- fonts

  @Test
  void aDestinationLabelUsesItsTokenRoleRatherThanTheContainersFont() {
    final ElwhaNavRailDestination inherited = RailFixture.destination("Library");
    final JPanel page = new JPanel();
    page.setFont(new Font(Font.SERIF, Font.PLAIN, 40));
    page.add(inherited);
    inherited.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);
    final ElwhaNavRailDestination offscreen = RailFixture.destination("Library");
    offscreen.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(inherited.getPreferredSize().width)
        .as(
            "#628 — getFont() answers the inherited container font in any real hierarchy, so"
                + " preferring it applied LABEL_MEDIUM only to offscreen renders")
        .isEqualTo(offscreen.getPreferredSize().width);
  }

  @Test
  void anExplicitFontOnADestinationStillWins() {
    final ElwhaNavRailDestination roled = RailFixture.destination("Library");
    roled.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);
    final ElwhaNavRailDestination overridden = RailFixture.destination("Library");
    overridden.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);

    overridden.setFont(new Font(Font.SERIF, Font.PLAIN, 40));

    assertThat(overridden.getPreferredSize().width)
        .as(
            "isFontSet() is the distinction: a font set on the component outranks the role, an"
                + " inherited one does not")
        .isGreaterThan(roled.getPreferredSize().width);
  }

  @Test
  void aSectionHeaderUsesItsTokenRoleRatherThanTheContainersFont() {
    final ElwhaNavigationRail inherited = ElwhaNavigationRail.expanded();
    inherited.setPrimary(RailFixture.destinations(2));
    inherited.addSection("Tools", RailFixture.destinations(1));
    final JPanel page = new JPanel();
    page.setFont(new Font(Font.SERIF, Font.PLAIN, 40));
    page.add(inherited);

    final ElwhaNavigationRail offscreen = ElwhaNavigationRail.expanded();
    offscreen.setPrimary(RailFixture.destinations(2));
    offscreen.addSection("Tools", RailFixture.destinations(1));

    assertThat(inherited.getPreferredSize().height)
        .as(
            "#628 — the reserved header height followed the inherited font, so a themed panel"
                + " silently resized the rail's chrome")
        .isEqualTo(offscreen.getPreferredSize().height);
  }

  // ----------------------------------------------------------- surface knobs

  @Test
  void surfaceAndDividerAreOptIn() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();

    assertThat(rail.isSurfaceFilled())
        .as("§3 — transparent by default, so the rail composes with the host's background")
        .isFalse();
    assertThat(rail.hasDivider()).isFalse();
    assertThat(rail.getElevation()).isZero();
  }

  @Test
  void surfaceKnobsAreReadBack() {
    final ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();

    rail.setSurfaceFilled(true);
    rail.setDivider(true);
    rail.setElevation(1);

    assertThat(rail.isSurfaceFilled()).isTrue();
    assertThat(rail.hasDivider()).isTrue();
    assertThat(rail.getElevation()).isEqualTo(1);
  }

  // ---------------------------------------------------------- teardown state

  /**
   * The destination's state layer fills a pill, not its whole box, so a corner probe would miss it
   * and a center probe would land on the glyph. Comparing whole rasters sidesteps both.
   */
  private static boolean rastersMatch(final BufferedImage a, final BufferedImage b) {
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          return false;
        }
      }
    }
    return true;
  }

  private static BufferedImage shot(final ElwhaNavRailDestination destination) {
    return Pixels.render(destination, 90, 60, Color.MAGENTA);
  }

  @Test
  void aDestinationRemovedWhileHoveredComesBackUnhovered() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 3);
    final ElwhaNavRailDestination hovered = rail.getPrimary().get(1);
    final BufferedImage atRest = shot(RailFixture.destination("Item 1"));

    Input.enter(hovered, 4, 4);
    assertThat(rastersMatch(shot(hovered), atRest))
        .as("the hover layer is really on — otherwise the teardown assertion proves nothing")
        .isFalse();

    hovered.removeNotify();

    assertThat(rastersMatch(shot(hovered), atRest))
        .as(
            "#625 — the hover poll is the only self-correction, so teardown has to clear the "
                + "flag or a re-added destination paints its hover layer for good")
        .isTrue();
  }

  @Test
  void aDestinationDisabledWhileHoveredComesBackUnhovered() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 3);
    final ElwhaNavRailDestination hovered = rail.getPrimary().get(1);
    final BufferedImage atRest = shot(RailFixture.destination("Item 1"));

    Input.enter(hovered, 4, 4);
    assertThat(rastersMatch(shot(hovered), atRest))
        .as("the hover layer is really on — otherwise the disable assertion proves nothing")
        .isFalse();

    hovered.setEnabled(false);
    hovered.setEnabled(true);

    assertThat(rastersMatch(shot(hovered), atRest))
        .as(
            "#683 — a disabled destination receives no mouse events and the hover poll is the "
                + "only self-correction, so re-enabling must not replay the hover layer")
        .isTrue();
  }

  @Test
  void aNonMemberDestinationCannotBeSelected() {
    final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 3);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .as("§6 — the rail's selection is over its own members, so a stranger is a bug")
        .isThrownBy(() -> rail.setSelected(RailFixture.destination("Stranger")));
  }
}
