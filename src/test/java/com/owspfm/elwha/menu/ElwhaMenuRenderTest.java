package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of what a menu actually paints: the container tint per {@link ColorStyle}, the
 * selected-row fill and its "on" content color, the hover state layer, the roving focus ring, and
 * the disabled dim. Fills are probed as pixels against resolved roles; text colors and the focus
 * ring come off a {@link PaintLog}, never from probing glyph or stroke pixels.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaMenuRenderTest {

  private static final String LABEL = "Gallery";

  /** A point inside the row's visual band but clear of the label glyphs and any trailing slot. */
  private static int probeX(final ElwhaMenuItem item) {
    return item.getWidth() - ElwhaMenuItem.INSET_X_PX / 2;
  }

  /**
   * Builds a row, applies {@code configure}, then sizes it — in that order, because state that adds
   * a leading column (selection, a reserved check column) changes the row's measured width and
   * would otherwise slide the label under the trailing probe point.
   */
  private static ElwhaMenuItem sizedItem(final Consumer<ElwhaMenuItem> configure) {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    configure.accept(item);
    item.setSize(item.getPreferredSize());
    return item;
  }

  private static ElwhaMenuItem sizedItem() {
    return sizedItem(item -> {});
  }

  // ------------------------------------------------------------- row surface

  @Test
  void anUnselectedRowPaintsNoFillOfItsOwn() {
    final ElwhaMenuItem item = sizedItem();

    final BufferedImage shot = Pixels.render(item, item.getWidth(), item.getHeight());

    Pixels.assertPixelNear(
        shot,
        probeX(item),
        item.getHeight() / 2,
        ColorRole.SURFACE.resolve(),
        "a resting row is transparent, so the menu container shows through");
  }

  @Test
  void aSelectedRowPaintsTheTertiaryContainerFill() {
    final ElwhaMenuItem item = sizedItem(row -> row.setSelected(true));

    final BufferedImage shot = Pixels.render(item, item.getWidth(), item.getHeight());

    Pixels.assertPixelNear(
        shot,
        probeX(item),
        item.getHeight() / 2,
        ColorRole.TERTIARY_CONTAINER.resolve(),
        "a standard selected row wears the tertiary container fill");
  }

  @Test
  void aVibrantSelectedRowPaintsTheBoldTertiaryFill() {
    final ElwhaMenuItem item =
        sizedItem(
            row -> {
              row.setColorStyle(ColorStyle.VIBRANT);
              row.setSelected(true);
            });

    final BufferedImage shot = Pixels.render(item, item.getWidth(), item.getHeight());

    Pixels.assertPixelNear(
        shot,
        probeX(item),
        item.getHeight() / 2,
        ColorRole.TERTIARY.resolve(),
        "a vibrant selected row swaps up to the bold tertiary fill");
  }

  @Test
  void hoveringPaintsTheOnSurfaceStateLayer() {
    final ElwhaMenuItem item = sizedItem();
    Input.enter(item, 10, item.getHeight() / 2);

    final BufferedImage shot = Pixels.render(item, item.getWidth(), item.getHeight());

    Pixels.assertPixelNear(
        shot,
        probeX(item),
        item.getHeight() / 2,
        Pixels.mix(
            ColorRole.SURFACE.resolve(),
            ColorRole.ON_SURFACE.resolve(),
            StateLayer.HOVER.opacity()),
        "a hovered row composites the M3 hover layer over whatever is behind it");
  }

  @Test
  void aDisabledRowPaintsNoStateLayerEvenAfterAPointerEnter() {
    final ElwhaMenuItem item = sizedItem(row -> row.setEnabled(false));
    Input.enter(item, 10, item.getHeight() / 2);

    final BufferedImage shot = Pixels.render(item, item.getWidth(), item.getHeight());

    Pixels.assertPixelNear(
        shot,
        probeX(item),
        item.getHeight() / 2,
        ColorRole.SURFACE.resolve(),
        "a disabled row stays inert under the pointer");
  }

  // ------------------------------------------------------------- row content

  @Test
  void aRestingLabelPaintsInTheOnSurfaceRole() {
    final ElwhaMenuItem item = sizedItem();

    final PaintLog log = PaintLog.capture(item, item.getWidth(), item.getHeight());

    assertThat(log.text(LABEL))
        .as("the label is painted at all")
        .isPresent()
        .get()
        .extracting(PaintLog.Text::color)
        .as("in the standard on-surface role")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
  }

  @Test
  void aSelectedLabelSwitchesToTheOnFillRole() {
    final ElwhaMenuItem item = sizedItem(row -> row.setSelected(true));

    final PaintLog log = PaintLog.capture(item, item.getWidth(), item.getHeight());

    assertThat(log.text(LABEL).orElseThrow().color())
        .as("content on the selected fill uses the fill's own on-role, for contrast")
        .isEqualTo(ColorRole.ON_TERTIARY_CONTAINER.resolve());
  }

  @Test
  void aVibrantSelectedLabelUsesTheOnTertiaryRole() {
    final ElwhaMenuItem item =
        sizedItem(
            row -> {
              row.setColorStyle(ColorStyle.VIBRANT);
              row.setSelected(true);
            });

    final PaintLog log = PaintLog.capture(item, item.getWidth(), item.getHeight());

    assertThat(log.text(LABEL).orElseThrow().color())
        .as("the bold vibrant fill takes the on-tertiary content role")
        .isEqualTo(ColorRole.ON_TERTIARY.resolve());
  }

  @Test
  void supportingAndTrailingTextPaintInTheVariantRole() {
    final ElwhaMenuItem item =
        sizedItem(
            row -> {
              row.setSupportingText("Second line");
              row.setTrailingText("⌘G");
            });

    final PaintLog log = PaintLog.capture(item, item.getWidth(), item.getHeight());

    assertThat(log.text("Second line").orElseThrow().color())
        .as("supporting text is de-emphasized against the label")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
    assertThat(log.text("⌘G").orElseThrow().color())
        .as("and a keyboard shortcut carries the same de-emphasis")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
  }

  @Test
  void rovingFocusRingIsAStrokedSecondaryOutline() {
    final ElwhaMenuItem item = sizedItem(row -> row.setFocused(true));

    final PaintLog log = PaintLog.capture(item, item.getWidth(), item.getHeight());

    assertThat(log.shapes())
        .as("the focused row is ringed with a stroked secondary outline")
        .anyMatch(s -> s.stroked() && ColorRole.SECONDARY.resolve().equals(s.color()));
  }

  @Test
  void anUnfocusedRowCarriesNoRing() {
    final ElwhaMenuItem item = sizedItem();

    final PaintLog log = PaintLog.capture(item, item.getWidth(), item.getHeight());

    assertThat(log.shapes())
        .as("the ring appears only on the row the roving focus is on")
        .noneMatch(s -> s.stroked() && ColorRole.SECONDARY.resolve().equals(s.color()));
  }

  @Test
  void aSelectedRowDropsTheCheckmarkIntoTheReservedColumn() {
    final ElwhaMenuItem item = sizedItem(row -> row.setReserveLeadingColumn(true));
    final int glyphX = ElwhaMenuItem.INSET_X_PX + ElwhaMenuItem.ICON_SIZE_PX / 2;
    final int glyphY = item.getHeight() / 2;
    final Color tick = ColorRole.ON_TERTIARY_CONTAINER.resolve();

    Pixels.assertNoPixelNear(
        Pixels.render(item, item.getWidth(), item.getHeight()),
        glyphX,
        glyphY,
        ElwhaMenuItem.ICON_SIZE_PX / 2,
        tick,
        "the reserved column is empty while the row is unselected");

    item.setSelected(true);

    Pixels.assertAnyPixelNear(
        Pixels.render(item, item.getWidth(), item.getHeight()),
        glyphX,
        glyphY,
        ElwhaMenuItem.ICON_SIZE_PX / 2,
        tick,
        "selection drops the tick into the column that was already reserved for it");
  }

  // --------------------------------------------------------- menu container

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void standardContainerTintsSurfaceContainerLowInBothModes(final Mode mode) {
    ThemeExtension.install(mode);
    final ElwhaMenu menu = ElwhaMenu.builder().addItem(ElwhaMenuItem.of(LABEL)).build();

    assertContainerTint(menu.renderPreview(), ColorRole.SURFACE_CONTAINER_LOW.resolve());
  }

  @Test
  void aVibrantContainerTintsTertiaryContainer() {
    final ElwhaMenu menu =
        ElwhaMenu.builder().colorStyle(ColorStyle.VIBRANT).addItem(ElwhaMenuItem.of(LABEL)).build();

    assertContainerTint(menu.renderPreview(), ColorRole.TERTIARY_CONTAINER.resolve());
  }

  private static void assertContainerTint(final JComponent surface, final Color want) {
    final Dimension pref = surface.getPreferredSize();
    final BufferedImage shot = Pixels.render(surface, pref.width, pref.height);
    final Insets shadow = ShadowPainter.shadowInsets(ElwhaMenu.ELEVATION);
    Pixels.assertPixelNear(
        shot,
        shadow.left + 2,
        pref.height / 2,
        want,
        "the menu container is filled with its color style's tint");
  }
}
