package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link DefaultElwhaListModel}'s mutation and event contract — the typed event
 * layer the unified family gained over the two it replaces, including the {@code STRUCTURE} type
 * that neither predecessor had.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class DefaultElwhaListModelTest {

  private final List<ElwhaListDataEvent<String>> events = new ArrayList<>();

  private DefaultElwhaListModel<String> observed(final String... items) {
    final DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>(List.of(items));
    model.addListDataListener(events::add);
    return model;
  }

  private ElwhaListDataEvent<String> only() {
    assertThat(events).as("exactly one event was fired").hasSize(1);
    return events.get(0);
  }

  @Test
  void addAppendsAndReportsTheInsertedIndex() {
    final DefaultElwhaListModel<String> model = observed("a", "b");

    model.add("c");

    assertThat(model.getItems()).as("append lands at the end").containsExactly("a", "b", "c");
    assertThat(only().getType()).as("append fires ADDED").isEqualTo(ElwhaListDataEvent.Type.ADDED);
    assertThat(only().getIndex0()).as("ADDED index0 is the insertion point").isEqualTo(2);
    assertThat(only().getIndex1()).as("a single append spans one index").isEqualTo(2);
  }

  @Test
  void addAllFiresOneEventSpanningTheInsertedRange() {
    final DefaultElwhaListModel<String> model = observed("a");

    model.addAll(List.of("b", "c", "d"));

    assertThat(only().getType())
        .as("batch append fires ADDED")
        .isEqualTo(ElwhaListDataEvent.Type.ADDED);
    assertThat(only().getIndex0()).as("the range starts at the first inserted index").isEqualTo(1);
    assertThat(only().getIndex1()).as("the range ends at the last inserted index").isEqualTo(3);
  }

  @Test
  void emptyAddAllIsSilent() {
    final DefaultElwhaListModel<String> model = observed("a");

    model.addAll(List.of());

    assertThat(events).as("an empty batch fires nothing").isEmpty();
    assertThat(model.getSize()).as("an empty batch changes nothing").isEqualTo(1);
  }

  @Test
  void removeByValueReportsThePriorIndex() {
    final DefaultElwhaListModel<String> model = observed("a", "b", "c");

    assertThat(model.remove("b")).as("removing a present item reports success").isTrue();
    assertThat(model.getItems()).as("the item is gone").containsExactly("a", "c");
    assertThat(only().getType())
        .as("removal fires REMOVED")
        .isEqualTo(ElwhaListDataEvent.Type.REMOVED);
    assertThat(only().getIndex0()).as("REMOVED carries the index held before removal").isEqualTo(1);
  }

  @Test
  void removingAnAbsentItemIsSilent() {
    final DefaultElwhaListModel<String> model = observed("a");

    assertThat(model.remove("zzz")).as("removing an absent item reports failure").isFalse();
    assertThat(events).as("a failed removal fires nothing").isEmpty();
  }

  @Test
  void setReplacesInPlaceAndFiresChanged() {
    final DefaultElwhaListModel<String> model = observed("a", "b");

    assertThat(model.set(1, "B")).as("set returns the previous occupant").isEqualTo("b");
    assertThat(model.getItems())
        .as("the replacement holds the same slot")
        .containsExactly("a", "B");
    assertThat(only().getType())
        .as("in-place replacement fires CHANGED")
        .isEqualTo(ElwhaListDataEvent.Type.CHANGED);
  }

  @Test
  void moveCarriesSourceAndDestinationIndices() {
    final DefaultElwhaListModel<String> model = observed("a", "b", "c", "d");

    model.move(0, 2);

    assertThat(model.getItems())
        .as("move is remove-then-insert-at")
        .containsExactly("b", "c", "a", "d");
    assertThat(only().getType()).as("move fires MOVED").isEqualTo(ElwhaListDataEvent.Type.MOVED);
    assertThat(only().getIndex0()).as("MOVED index0 is the source").isEqualTo(0);
    assertThat(only().getIndex1()).as("MOVED index1 is the destination").isEqualTo(2);
  }

  @Test
  void moveToTheSameIndexIsSilent() {
    final DefaultElwhaListModel<String> model = observed("a", "b");

    model.move(1, 1);

    assertThat(events).as("a no-op move fires nothing").isEmpty();
  }

  @Test
  void moveRejectsOutOfRangeIndices() {
    final DefaultElwhaListModel<String> model = observed("a", "b");

    assertThatThrownBy(() -> model.move(0, 5))
        .as("an out-of-range destination is rejected")
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void setItemsAndClearBothFireStructure() {
    final DefaultElwhaListModel<String> model = observed("a", "b");

    model.setItems(List.of("x", "y", "z"));
    assertThat(only().getType())
        .as("a wholesale replacement fires STRUCTURE")
        .isEqualTo(ElwhaListDataEvent.Type.STRUCTURE);
    assertThat(only().getIndex0()).as("STRUCTURE carries no meaningful index").isEqualTo(-1);
    assertThat(only().getIndex1()).as("STRUCTURE carries no meaningful index").isEqualTo(-1);

    events.clear();
    model.clear();
    assertThat(only().getType())
        .as("clear fires STRUCTURE")
        .isEqualTo(ElwhaListDataEvent.Type.STRUCTURE);

    events.clear();
    model.clear();
    assertThat(events).as("clearing an empty model fires nothing").isEmpty();
  }

  @Test
  void nullItemsIsTreatedAsEmpty() {
    final DefaultElwhaListModel<String> model = observed("a");

    model.setItems(null);

    assertThat(model.getSize()).as("null contents empty the model").isZero();
  }

  @Test
  void getItemsIsAnUntrackingSnapshot() {
    final DefaultElwhaListModel<String> model = observed("a", "b");
    final List<String> snapshot = model.getItems();

    model.add("c");

    assertThat(snapshot)
        .as("the snapshot does not follow later mutations")
        .containsExactly("a", "b");
    assertThatThrownBy(() -> snapshot.add("d"))
        .as("the snapshot is unmodifiable")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void listenersAreDeduplicatedAndRemovable() {
    final DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>(List.of("a"));
    final ElwhaListDataListener<String> listener = events::add;

    model.addListDataListener(listener);
    model.addListDataListener(listener);
    model.addListDataListener(null);
    model.add("b");
    assertThat(events).as("a listener registered twice is notified once").hasSize(1);

    events.clear();
    model.removeListDataListener(listener);
    model.add("c");
    assertThat(events).as("a removed listener stops hearing changes").isEmpty();
  }

  @Test
  void containsAndIndexOfReadTheCurrentContents() {
    final DefaultElwhaListModel<String> model = observed("a", "b");

    assertThat(model.contains("b")).as("a present item is reported present").isTrue();
    assertThat(model.contains("q")).as("an absent item is reported absent").isFalse();
    assertThat(model.indexOf("b")).as("indexOf finds the first occurrence").isEqualTo(1);
    assertThat(model.indexOf("q")).as("indexOf reports -1 for an absent item").isEqualTo(-1);
    assertThat(model.getElementAt(0)).as("getElementAt reads by index").isEqualTo("a");
  }

  @Test
  void iterationWalksModelOrder() {
    final DefaultElwhaListModel<String> model = observed("a", "b", "c");
    final List<String> seen = new ArrayList<>();

    for (final String item : model) {
      seen.add(item);
    }

    assertThat(seen).as("iteration follows model order").containsExactly("a", "b", "c");
  }
}
