package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Insets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the chip's two orthogonal axes and the token setters over them: the {@link
 * ChipVariant} declares treatment and carries the roles, while the surface role stays independently
 * overridable per instance. The four M3 chip patterns ship as factory presets over those axes
 * rather than as a rigid type enum, so each preset is checked to be exactly a configuration a
 * caller could have written by hand — and to stay overridable afterwards.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipVariantTest {

  // ---------------------------------------------------------------- defaults

  @Test
  void aFreshChipCarriesTheLockedDefaults() {
    final ElwhaChip chip = new ElwhaChip();

    assertThat(chip.getVariant())
        .as("FILLED is the default treatment")
        .isEqualTo(ChipVariant.FILLED);
    assertThat(chip.getInteractionMode())
        .as("and a bare chip is inert until a mode is chosen")
        .isEqualTo(ChipInteractionMode.STATIC);
    assertThat(chip.getShape()).as("SM is the M3 chip corner value").isEqualTo(ShapeScale.SM);
    assertThat(chip.getBorderWidth()).as("with a hairline resting border").isEqualTo(1);
    assertThat(chip.isSelected()).as("and no selection").isFalse();
    assertThat(chip.getText()).as("the no-arg constructor gives empty text, never null").isEmpty();
    assertThat(chip.isOpaque())
        .as("an opaque panel would have Swing pre-fill square corners under the round-rect")
        .isFalse();
  }

  @Test
  void nullTextIsTreatedAsEmpty() {
    assertThat(new ElwhaChip(null).getText()).as("a null label is the empty string").isEmpty();
    assertThat(new ElwhaChip("Demand").setText(null).getText())
        .as("and setting null clears rather than storing null")
        .isEmpty();
  }

  // ----------------------------------------------------------------- variant

  @ParameterizedTest
  @EnumSource(ChipVariant.class)
  void everyVariantRoundTrips(final ChipVariant variant) {
    assertThat(new ElwhaChip("Demand").setVariant(variant).getVariant())
        .as(variant + " round-trips")
        .isEqualTo(variant);
  }

  @Test
  void aNullVariantIsIgnored() {
    final ElwhaChip chip = new ElwhaChip("Demand").setVariant(ChipVariant.OUTLINED);

    chip.setVariant(null);

    assertThat(chip.getVariant())
        .as("null is a no-op rather than a reset — there is no 'no treatment' state")
        .isEqualTo(ChipVariant.OUTLINED);
  }

  @Test
  void variantsCarryTheirOwnRoles() {
    assertThat(ChipVariant.FILLED.surfaceRole())
        .as("FILLED reads as a distinct group against the page")
        .isEqualTo(ColorRole.PRIMARY_CONTAINER);
    assertThat(ChipVariant.FILLED.borderRole())
        .as("with the softer of the two outline roles")
        .isEqualTo(ColorRole.OUTLINE_VARIANT);
    assertThat(ChipVariant.OUTLINED.surfaceRole())
        .as("OUTLINED sits on the plain surface")
        .isEqualTo(ColorRole.SURFACE);
    assertThat(ChipVariant.OUTLINED.borderRole())
        .as("and carries the full-strength outline")
        .isEqualTo(ColorRole.OUTLINE);
    assertThat(ChipVariant.GHOST.surfaceRole()).as("GHOST has no resting fill at all").isNull();
    assertThat(ChipVariant.GHOST.borderRole()).as("and no resting border").isNull();
  }

  // --------------------------------------------------------- surface override

  @Test
  void surfaceRoleFallsBackToTheVariant() {
    assertThat(new ElwhaChip("Demand").getSurfaceRole())
        .as("with no override the variant supplies the role")
        .isEqualTo(ColorRole.PRIMARY_CONTAINER);
    assertThat(new ElwhaChip("Demand").setVariant(ChipVariant.GHOST).getSurfaceRole())
        .as("including GHOST's deliberate null")
        .isNull();
  }

  @Test
  void anOverrideBeatsTheVariantAndNullRestoresIt() {
    final ElwhaChip chip = new ElwhaChip("Demand");

    chip.setSurfaceRole(ColorRole.SECONDARY_CONTAINER);
    assertThat(chip.getSurfaceRole())
        .as("colour and treatment are orthogonal, so a FILLED chip can take any surface role")
        .isEqualTo(ColorRole.SECONDARY_CONTAINER);

    chip.setSurfaceRole(null);
    assertThat(chip.getSurfaceRole())
        .as("and null clears the override rather than blanking the surface")
        .isEqualTo(ColorRole.PRIMARY_CONTAINER);
  }

  @Test
  void anOverrideSurvivesAVariantChange() {
    final ElwhaChip chip = new ElwhaChip("Demand").setSurfaceRole(ColorRole.TERTIARY_CONTAINER);

    chip.setVariant(ChipVariant.OUTLINED);

    assertThat(chip.getSurfaceRole())
        .as("the per-instance override is the outer of the two, so swapping treatment keeps it")
        .isEqualTo(ColorRole.TERTIARY_CONTAINER);
  }

  @Test
  void anOverrideGivesEvenAGhostChipASurface() {
    assertThat(
            new ElwhaChip("Demand")
                .setVariant(ChipVariant.GHOST)
                .setSurfaceRole(ColorRole.SECONDARY_CONTAINER)
                .getSurfaceRole())
        .as("GHOST's null is a default, not a prohibition")
        .isEqualTo(ColorRole.SECONDARY_CONTAINER);
  }

  // ------------------------------------------------------------ shape/padding

  @ParameterizedTest
  @EnumSource(ShapeScale.class)
  void everyShapeStepRoundTrips(final ShapeScale shape) {
    assertThat(new ElwhaChip("Demand").setShape(shape).getShape())
        .as(shape + " round-trips")
        .isEqualTo(shape);
  }

  @Test
  void aNullShapeRestoresTheDefault() {
    assertThat(new ElwhaChip("Demand").setShape(ShapeScale.FULL).setShape(null).getShape())
        .as("null falls back to the SM chip default rather than being ignored")
        .isEqualTo(ShapeScale.SM);
  }

  @Test
  void paddingResolvesToInsets() {
    final ElwhaChip chip = new ElwhaChip("Demand").setPadding(SpaceScale.LG, SpaceScale.SM);

    assertThat(chip.getPadding())
        .as("the horizontal step lands on left and right, the vertical on top and bottom")
        .isEqualTo(SpaceScale.insets(SpaceScale.SM, SpaceScale.LG));
  }

  @Test
  void aNullPaddingStepIsIgnoredPerAxis() {
    final ElwhaChip chip = new ElwhaChip("Demand").setPadding(SpaceScale.LG, SpaceScale.SM);

    chip.setPadding(null, SpaceScale.XL);

    assertThat(chip.getPadding())
        .as("passing null for one axis leaves that axis alone rather than resetting it")
        .isEqualTo(SpaceScale.insets(SpaceScale.XL, SpaceScale.LG));
  }

  @Test
  void paddingAccessorHandsBackACopy() {
    final ElwhaChip chip = new ElwhaChip("Demand");
    final Insets first = chip.getPadding();

    first.left = 999;

    assertThat(chip.getPadding().left)
        .as("a caller mutating the returned insets cannot reach the chip's own geometry")
        .isNotEqualTo(999);
  }

  @Test
  void aNegativeBorderWidthClampsToZero() {
    assertThat(new ElwhaChip("Demand").setBorderWidth(-4).getBorderWidth())
        .as("a border cannot be thinner than nothing")
        .isZero();
    assertThat(new ElwhaChip("Demand").setBorderWidth(3).getBorderWidth())
        .as("and an ordinary width is stored as given")
        .isEqualTo(3);
  }

  // ------------------------------------------------------------- fluent shape

  @Test
  void tokenSettersAreFluentAndReturnTheChip() {
    final ElwhaChip chip = new ElwhaChip("Demand");

    final ElwhaChip chained =
        chip.setVariant(ChipVariant.OUTLINED)
            .setInteractionMode(ChipInteractionMode.CLICKABLE)
            .setSurfaceRole(ColorRole.SECONDARY_CONTAINER)
            .setShape(ShapeScale.FULL)
            .setPadding(SpaceScale.MD, SpaceScale.XS)
            .setBorderWidth(2)
            .setText("Supply")
            .setSelected(true);

    assertThat(chained)
        .as("every setter returns the chip itself for inline configuration")
        .isSameAs(chip);
  }

  // ----------------------------------------------------------------- presets

  @Test
  void assistPresetIsAClickableOutlinedChip() {
    final ElwhaChip chip = ElwhaChip.assistChip("Directions");

    assertThat(chip.getVariant()).as("assist chips are outlined").isEqualTo(ChipVariant.OUTLINED);
    assertThat(chip.getInteractionMode())
        .as("and behave as push buttons, holding no selection of their own")
        .isEqualTo(ChipInteractionMode.CLICKABLE);
    assertThat(chip.getText()).as("carrying the given label").isEqualTo("Directions");
  }

  @Test
  void filterPresetIsASelectableOutlinedChip() {
    final ElwhaChip chip = ElwhaChip.filterChip("Demand");

    assertThat(chip.getVariant()).as("filter chips are outlined").isEqualTo(ChipVariant.OUTLINED);
    assertThat(chip.getInteractionMode())
        .as("and are the one preset that toggles a persistent selected state")
        .isEqualTo(ChipInteractionMode.SELECTABLE);
  }

  @Test
  void suggestionPresetIsAClickableOutlinedChip() {
    final ElwhaChip chip = ElwhaChip.suggestionChip("Try this");

    assertThat(chip.getVariant())
        .as("suggestion chips are outlined")
        .isEqualTo(ChipVariant.OUTLINED);
    assertThat(chip.getInteractionMode())
        .as("and fire and forget rather than holding state")
        .isEqualTo(ChipInteractionMode.CLICKABLE);
  }

  @Test
  void inputPresetAddsATrailingRemoveAffordance() {
    final ElwhaChip chip = ElwhaChip.inputChip("charles@example.com", () -> {});

    assertThat(chip.getVariant()).as("input chips are outlined").isEqualTo(ChipVariant.OUTLINED);
    assertThat(chip.getInteractionMode())
        .as("and clickable")
        .isEqualTo(ChipInteractionMode.CLICKABLE);
    assertThat(chip.getTrailingButton().isVisible())
        .as("with a remove affordance already installed in the trailing slot")
        .isTrue();
  }

  @Test
  void inputPresetToleratesANullRemoveHandler() {
    final ElwhaChip chip = ElwhaChip.inputChip("charles@example.com", null);

    assertThat(chip.getTrailingButton().isVisible())
        .as("a null handler still renders the affordance — it only suppresses the click")
        .isTrue();
  }

  @Test
  void aPresetStaysFullyOverridable() {
    final ElwhaChip chip =
        ElwhaChip.filterChip("Demand")
            .setVariant(ChipVariant.FILLED)
            .setInteractionMode(ChipInteractionMode.STATIC);

    assertThat(chip.getVariant())
        .as("presets are sugar over the axes, not a locked type — every choice is overridable")
        .isEqualTo(ChipVariant.FILLED);
    assertThat(chip.getInteractionMode())
        .as("including the interaction mode")
        .isEqualTo(ChipInteractionMode.STATIC);
  }

  @Test
  void presetsAreIndependentInstances() {
    final ElwhaChip first = ElwhaChip.filterChip("Demand");
    final ElwhaChip second = ElwhaChip.filterChip("Supply");

    first.setSelected(true);

    assertThat(second.isSelected())
        .as("a factory hands back a fresh chip each call, sharing no state")
        .isFalse();
    assertThat(second.getText()).as("with its own label").isEqualTo("Supply");
  }
}
