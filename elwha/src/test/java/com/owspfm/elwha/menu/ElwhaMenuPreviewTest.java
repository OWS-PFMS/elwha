package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the {@link ElwhaMenu#renderPreview()} isolation contract (#589): a preview
 * renders what the live surface renders at rest, while sharing no component with it. Before the
 * fix, a preview was built through the live code path — it moved the consumer's item instances into
 * the preview column (a Swing component has one parent), so previewing an open menu emptied it and
 * previewing twice emptied the first preview.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaMenuPreviewTest {

  private HeadlessHost host;
  private final List<ElwhaMenu> opened = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(700, 500);
  }

  @AfterEach
  void closeEveryMenu() {
    for (final ElwhaMenu menu : opened) {
      menu.close();
    }
    opened.clear();
  }

  private ElwhaMenu open(final ElwhaMenu menu) {
    opened.add(menu);
    menu.open(host.anchor());
    return menu;
  }

  private static ElwhaMenu.Builder threeItems() {
    return ElwhaMenu.builder()
        .addItem(ElwhaMenuItem.of(MaterialIcons.edit(20), "Rename"))
        .addItem(ElwhaMenuItem.of("Duplicate"))
        .addItem(ElwhaMenuItem.of(MaterialIcons.delete(20), "Delete"));
  }

  /** Every {@link ElwhaMenuItem} in a rendered surface, in containment order. */
  private static List<ElwhaMenuItem> rowsIn(final Container root) {
    final List<ElwhaMenuItem> found = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      if (child instanceof ElwhaMenuItem item) {
        found.add(item);
      } else if (child instanceof Container container) {
        found.addAll(rowsIn(container));
      }
    }
    return found;
  }

  /**
   * Counts the pixels on which two surfaces disagree, rendered at the same raster size.
   *
   * <p>{@link Pixels#render} lays out only the component's immediate children, which for a menu
   * stops above the rows — the items would keep 0×0 bounds and paint nothing, so every comparison
   * would trivially agree. These shots lay the tree out to the leaves first.
   */
  private static int pixelDiff(final JComponent one, final JComponent other, final Dimension size) {
    final BufferedImage a = shot(one, size);
    final BufferedImage b = shot(other, size);
    int diff = 0;
    for (int y = 0; y < size.height; y++) {
      for (int x = 0; x < size.width; x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          diff++;
        }
      }
    }
    return diff;
  }

  private static BufferedImage shot(final JComponent root, final Dimension size) {
    root.setSize(size);
    layoutDeep(root);
    final BufferedImage image =
        new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(ColorRole.SURFACE.resolve());
      g.fillRect(0, 0, size.width, size.height);
      root.paint(g);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static void layoutDeep(final Component component) {
    if (component instanceof Container container) {
      container.doLayout();
      for (final Component child : container.getComponents()) {
        layoutDeep(child);
      }
    }
  }

  // ------------------------------------------------------------- no mutation

  @Test
  void previewingAnOpenMenuLeavesItsMountedRowsAlone() {
    final ElwhaMenu menu = open(threeItems().build());
    final JComponent mounted = (JComponent) host.mounted().get(0);
    final List<ElwhaMenuItem> before = rowsIn(mounted);

    menu.renderPreview();

    assertThat(rowsIn(mounted))
        .as("#589 — a preview must not move the consumer's rows out of the menu that is showing")
        .containsExactlyElementsOf(before)
        .hasSize(3);
  }

  @Test
  void aPreviewSharesNoRowWithTheLiveSurfaceOrWithAnotherPreview() {
    final ElwhaMenu menu = open(threeItems().build());
    final List<ElwhaMenuItem> live = rowsIn((JComponent) host.mounted().get(0));

    final List<ElwhaMenuItem> first = rowsIn(menu.renderPreview());
    final List<ElwhaMenuItem> second = rowsIn(menu.renderPreview());

    assertThat(first)
        .as("the preview builds its own rows")
        .hasSize(3)
        .doesNotContainAnyElementsOf(live);
    assertThat(second)
        .as("so a second preview cannot empty the first")
        .hasSize(3)
        .doesNotContainAnyElementsOf(first);
  }

  @Test
  void previewingDoesNotDisturbTheOpenMenusRovingHighlight() {
    final ElwhaMenu menu = open(threeItems().build());
    menu.moveHighlight(1);
    final ElwhaMenuItem highlighted = menu.getHighlightedItem();

    menu.renderPreview();

    assertThat(menu.getHighlightedItem())
        .as("#589 — the preview wrote the live roving-focus fields on its way out")
        .isSameAs(highlighted);
  }

  @Test
  void previewingAnOpenScrollableMenuLeavesItsScrollDowngradeIntact() {
    final ElwhaMenu.Builder builder = ElwhaMenu.builder().layout(Layout.GROUPED);
    for (int i = 0; i < 12; i++) {
      builder.addItem(ElwhaMenuItem.of("Item " + i));
    }
    final ElwhaMenu menu = builder.separator(Separator.GAP).build();
    host.resize(700, 200);
    open(menu);
    assertThat(menu.effectiveSeparator()).isEqualTo(Separator.DIVIDER);

    menu.renderPreview();

    assertThat(menu.effectiveSeparator())
        .as(
            "the preview measures against no layered pane, so it must not re-decide the open's"
                + " mode")
        .isEqualTo(Separator.DIVIDER);
  }

  @Test
  void aSlottedItemPreviewsAsItsLabelAndKeepsTheSlotWhereItWas() {
    final JLabel slot = new JLabel("swatch");
    final ElwhaMenuItem item = ElwhaMenuItem.of("Color");
    item.setSlot(slot);
    final ElwhaMenu menu = ElwhaMenu.builder().addItem(item).build();

    final JComponent preview = menu.renderPreview();

    assertThat(slot.getParent())
        .as("a consumer component belongs to one hierarchy — the preview cannot borrow it")
        .isSameAs(item);
    assertThat(rowsIn(preview))
        .singleElement()
        .satisfies(row -> assertThat(row.getSlot()).isNull());
  }

  // ---------------------------------------------------------- pixel fidelity

  @Test
  void aPreviewRendersWhatTheOpenMenuRenders() {
    final ElwhaMenu menu = open(threeItems().build());
    final JComponent mounted = (JComponent) host.mounted().get(0);

    assertThat(pixelDiff(mounted, menu.renderPreview(), mounted.getSize()))
        .as("the gallery tile is the resting live surface, pixel for pixel")
        .isZero();
  }

  @Test
  void aGroupedGapMenuPreviewsItsFloatingCards() {
    final ElwhaMenu menu =
        open(
            ElwhaMenu.builder()
                .layout(Layout.GROUPED)
                .separator(Separator.GAP)
                .addItem(ElwhaMenuItem.of("Cut"))
                .addGroup()
                .addItem(ElwhaMenuItem.of("Paste"))
                .build());
    final JComponent mounted = (JComponent) host.mounted().get(0);

    assertThat(pixelDiff(mounted, menu.renderPreview(), mounted.getSize()))
        .as(
            "the per-group card shadows and fills paint from the preview's own panels, not the live"
                + " menu's")
        .isZero();
  }

  @Test
  void aSelectedItemPreviewsWithItsFillAndCheckmark() {
    final ElwhaMenuItem copy = ElwhaMenuItem.of("Copy");
    final ElwhaMenu menu =
        open(
            ElwhaMenu.builder()
                .selectionMode(SelectionMode.SINGLE)
                .addItem(ElwhaMenuItem.of("Cut"))
                .addItem(copy)
                .build());
    copy.setSelected(true);
    final JComponent mounted = (JComponent) host.mounted().get(0);

    assertThat(pixelDiff(mounted, menu.renderPreview(), mounted.getSize()))
        .as("selection is pushed state, so a stand-in row must carry it into the preview")
        .isZero();
  }
}
