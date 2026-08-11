package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the four {@link ElwhaListOrientation} layouts and the cross-cutting {@link
 * ElwhaList} geometry knobs — gap, padding, and column count. The #69 spike classes pinned that
 * every orientation mirrors under RTL; this pins where each one actually puts its items.
 *
 * <p>Items are fixed-size stubs so every position below is exact arithmetic rather than an
 * assertion about the theme's font metrics.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaItemListLayoutTest {

  private static final int WIDTH = 300;
  private static final int HEIGHT = 400;
  private static final int ITEM_WIDTH = 80;
  private static final int ITEM_HEIGHT = 30;

  /** An item view with a pinned preferred size. */
  private static final class Slab extends JComponent {
    private final Dimension preferred;

    Slab(final int width, final int height) {
      this.preferred = new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(preferred);
    }
  }

  private DefaultElwhaListModel<String> model;
  private ElwhaItemList<String> list;

  private ElwhaItemList<String> listOf(final String... items) {
    return listOf(ITEM_WIDTH, ITEM_HEIGHT, items);
  }

  private ElwhaItemList<String> listOf(
      final int itemWidth, final int itemHeight, final String... items) {
    model = new DefaultElwhaListModel<>(List.of(items));
    list = new ElwhaItemList<>(model, (item, index) -> new Slab(itemWidth, itemHeight));
    return layout();
  }

  private ElwhaItemList<String> layout() {
    return layoutAt(WIDTH, HEIGHT);
  }

  private ElwhaItemList<String> layoutAt(final int width, final int height) {
    list.setSize(width, height);
    list.doLayout();
    // The list's own border carries the list padding, so the children are laid out at the bounds
    // BorderLayout assigned them — resizing them to the full list would erase that inset.
    for (final Component child : list.getComponents()) {
      ((JComponent) child).doLayout();
    }
    return list;
  }

  /** The panel the layout managers actually run on, one level below the list's border. */
  private Component contentPanel() {
    for (final Component child : list.getComponents()) {
      if (child.getClass().getSimpleName().equals("ContentPanel")) {
        return child;
      }
    }
    throw new AssertionError("the list is not rendering items");
  }

  private int itemX(final String item) {
    return list.getComponentFor(item).getX();
  }

  private int itemY(final String item) {
    return list.getComponentFor(item).getY();
  }

  /** An item's position in the list's own coordinates, across the padding border. */
  private java.awt.Point itemInListSpace(final String item) {
    final JComponent view = list.getComponentFor(item);
    return javax.swing.SwingUtilities.convertPoint(view.getParent(), view.getLocation(), list);
  }

  /** True when the component is still mounted somewhere under the list. */
  private boolean mountedOnList(final Component component) {
    for (Component cursor = component; cursor != null; cursor = cursor.getParent()) {
      if (cursor == list) {
        return true;
      }
    }
    return false;
  }

  // ------------------------------------------------------------- vertical

  @Test
  void verticalStacksItemsWithTheConfiguredGap() {
    listOf("a", "b", "c").setItemGap(12);
    layout();

    assertThat(itemY("b"))
        .as("each item follows the one above it by its height plus the gap")
        .isEqualTo(itemY("a") + ITEM_HEIGHT + 12);
    assertThat(itemY("c")).as("and the pattern continues").isEqualTo(itemY("b") + ITEM_HEIGHT + 12);
  }

  @Test
  void verticalKeepsEveryItemOnTheLeadingEdge() {
    listOf("a", "b");
    layout();

    assertThat(itemX("a"))
        .as("a single column aligns every item to the same leading edge")
        .isEqualTo(itemX("b"));
  }

  @Test
  void verticalItemsKeepTheirPreferredWidthRatherThanFillingTheList() {
    listOf("a");
    layout();

    assertThat(list.getComponentFor("a").getWidth())
        .as("the stack does not stretch items to the list width")
        .isEqualTo(ITEM_WIDTH);
  }

  // ----------------------------------------------------------- horizontal

  @Test
  void horizontalRunsItemsAcrossWithTheConfiguredGap() {
    listOf("a", "b", "c").setOrientation(ElwhaListOrientation.HORIZONTAL).setItemGap(6);
    layout();

    assertThat(itemX("b"))
        .as("each item follows the previous one by its width plus the gap")
        .isEqualTo(itemX("a") + ITEM_WIDTH + 6);
    assertThat(itemY("a")).as("and the row shares one baseline").isEqualTo(itemY("b"));
  }

  @Test
  void horizontalGivesEveryItemTheTallestItemsHeight() {
    model = new DefaultElwhaListModel<>(List.of("short", "tall"));
    list =
        new ElwhaItemList<>(
            model, (item, index) -> new Slab(ITEM_WIDTH, "tall".equals(item) ? 60 : ITEM_HEIGHT));
    list.setOrientation(ElwhaListOrientation.HORIZONTAL);
    layout();

    assertThat(list.getComponentFor("short").getHeight())
        .as("a row is one band, so a short item is stretched to the row height")
        .isEqualTo(60);
  }

  // ----------------------------------------------------------------- wrap

  @Test
  void wrapBreaksToANewRowWhenTheWidthIsExhausted() {
    listOf("a", "b", "c", "d").setOrientation(ElwhaListOrientation.WRAP).setItemGap(8);
    layout();

    assertThat(itemY("d"))
        .as("four 80 px items do not fit across 300 px, so the last one starts a second row")
        .isGreaterThan(itemY("a"));
    assertThat(itemX("d"))
        .as("and a wrapped item restarts at the leading edge")
        .isEqualTo(itemX("a"));
  }

  @Test
  void wrapRowsAreSeparatedByTheSameGap() {
    listOf("a", "b", "c", "d").setOrientation(ElwhaListOrientation.WRAP).setItemGap(8);
    layout();

    assertThat(itemY("d"))
        .as("the gap applies between rows as well as between columns")
        .isEqualTo(itemY("a") + ITEM_HEIGHT + 8);
  }

  @Test
  void aNarrowerListWrapsSooner() {
    listOf("a", "b", "c").setOrientation(ElwhaListOrientation.WRAP);
    layout();
    final int rowsAtFullWidth = distinctRowCount();

    layoutAt(120, HEIGHT);

    assertThat(distinctRowCount())
        .as("the wrap point tracks the width the list was actually given")
        .isGreaterThan(rowsAtFullWidth);
  }

  private int distinctRowCount() {
    return (int) list.getVisibleItems().stream().map(this::itemY).distinct().count();
  }

  // ----------------------------------------------------------------- grid

  @Test
  void gridPlacesItemsInTheConfiguredColumnCount() {
    listOf("a", "b", "c", "d").setOrientation(ElwhaListOrientation.GRID).setColumns(2);
    layout();

    assertThat(itemY("a")).as("the first two items share row zero").isEqualTo(itemY("b"));
    assertThat(itemY("c")).as("the next two start row one").isGreaterThan(itemY("a"));
    assertThat(itemX("a")).as("and column zero is shared down the grid").isEqualTo(itemX("c"));
    assertThat(itemX("b")).as("as is column one").isEqualTo(itemX("d"));
  }

  @Test
  void gridStretchesItemsToFillTheirCell() {
    listOf("a", "b").setOrientation(ElwhaListOrientation.GRID).setColumns(2);
    layout();

    assertThat(list.getComponentFor("a").getWidth())
        .as("a uniform grid sizes its cells from the container, not from item preferences")
        .isNotEqualTo(ITEM_WIDTH);
    assertThat(list.getComponentFor("a").getWidth())
        .as("and both columns get the same width")
        .isEqualTo(list.getComponentFor("b").getWidth());
  }

  @Test
  void aSingleColumnGridIsOneItemPerRow() {
    listOf("a", "b").setOrientation(ElwhaListOrientation.GRID).setColumns(1);
    layout();

    assertThat(itemY("b"))
        .as("the column clamp means a one-column grid still stacks rather than collapsing")
        .isGreaterThan(itemY("a"));
  }

  // -------------------------------------------------------------- padding

  @Test
  void listPaddingInsetsTheRenderedItems() {
    listOf("a", "b").setListPadding(new Insets(10, 20, 30, 40));
    layout();

    assertThat(list.getInsets())
        .as("the padding is carried on the list's own border")
        .isEqualTo(new Insets(10, 20, 30, 40));
    assertThat(itemInListSpace("a"))
        .as("so the first item sits one padding column in and one padding row down")
        .isEqualTo(new java.awt.Point(20, 10));
    assertThat(contentPanel().getX() + contentPanel().getWidth())
        .as("and the content stops short of the trailing padding column")
        .isEqualTo(WIDTH - 40);
  }

  @Test
  void changingThePaddingMovesTheItems() {
    listOf("a").setListPadding(new Insets(4, 4, 4, 4));
    layout();
    final int atSmallPadding = itemInListSpace("a").y;

    list.setListPadding(new Insets(24, 24, 24, 24));
    layout();

    assertThat(itemInListSpace("a").y)
        .as("padding is live rather than fixed at construction")
        .isGreaterThan(atSmallPadding);
  }

  @Test
  void aZeroGapPutsItemsFlushAgainstEachOther() {
    listOf("a", "b").setItemGap(0);
    layout();

    assertThat(itemY("b"))
        .as("a zero gap is honored rather than falling back to a default")
        .isEqualTo(itemY("a") + ITEM_HEIGHT);
  }

  // ------------------------------------------------------- placeholders

  @Test
  void emptyPlaceholderIsReplaceableAndRestorable() {
    listOf();
    final JComponent custom = new Slab(50, 50);

    list.setEmptyState(custom);
    layout();
    assertThat(mountedOnList(custom)).as("a supplied placeholder is what gets mounted").isTrue();

    list.setEmptyState(null);
    layout();
    assertThat(mountedOnList(custom))
        .as("passing null restores the built-in rather than rendering nothing")
        .isFalse();
  }

  @Test
  void loadingPlaceholderReplacesTheItemsWhileItShows() {
    listOf("a", "b");
    final JComponent custom = new Slab(50, 50);
    list.setLoadingComponent(custom);

    list.setLoading(true);
    layout();

    assertThat(mountedOnList(custom)).as("the loading placeholder is mounted").isTrue();
    assertThat(list.getComponentFor("a")).as("and the items are not").isNull();

    list.setLoading(false);
    layout();
    assertThat(mountedOnList(custom)).as("clearing the flag unmounts it").isFalse();
    assertThat(list.getComponentFor("a")).as("and brings the items back").isNotNull();
  }

  // -------------------------------------------------------------- mirroring

  @Test
  void gridColumnZeroMovesToTheRightUnderRightToLeft() {
    listOf("a", "b", "c", "d").setOrientation(ElwhaListOrientation.GRID).setColumns(2);
    layout();

    list.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    layout();

    assertThat(itemX("a"))
        .as("column zero is the leading column, which reads right under RTL")
        .isGreaterThan(itemX("b"));
    assertThat(itemX("c"))
        .as("and the mirroring is consistent down the grid")
        .isEqualTo(itemX("a"));
  }

  @Test
  void wrapRowsStillReadTopToBottomUnderRightToLeft() {
    listOf("a", "b", "c", "d").setOrientation(ElwhaListOrientation.WRAP);
    layout();

    list.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    layout();

    assertThat(itemY("d"))
        .as("mirroring flips the inline axis only — rows still stack downward")
        .isGreaterThan(itemY("a"));
  }
}
