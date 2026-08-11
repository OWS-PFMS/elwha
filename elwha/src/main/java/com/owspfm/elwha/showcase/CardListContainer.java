package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.list.DefaultElwhaListModel;
import com.owspfm.elwha.list.ElwhaItemList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.list.MovementMode;
import com.owspfm.elwha.list.SelectionMode;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

/**
 * The Elwha Showcase's Card List container demo, mounted on the shared {@link ContainerWorkbench}
 * scaffold: a live {@link ElwhaItemList} of sample items rendered as cards, a controls column for
 * the list options, and an event log that surfaces selection and model-change callbacks.
 *
 * <p>This is the {@code ContainerWorkbench}-pattern migration of the standalone Card List showcase
 * — a multi-instance surface a single live component cannot express.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
final class CardListContainer {

  private static final String[] SAMPLE = {
    "Trailhead survey",
    "Permit filing",
    "Gear checklist",
    "Weather window",
    "Camp assignment",
    "Route brief"
  };

  private final ContainerWorkbench workbench = new ContainerWorkbench();
  private final DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>(List.of(SAMPLE));
  private final ElwhaItemList<String> list =
      new ElwhaItemList<>(model, CardListContainer::renderCell);
  private int addedCount;

  /**
   * Builds the Card List container — model, list, controls, and event-log wiring.
   *
   * @version v0.5.0
   * @since v0.3.0
   */
  CardListContainer() {
    list.setSelectionMode(SelectionMode.MULTIPLE);
    list.setMovementMode(MovementMode.MOVABLE);
    list.setItemGap(8);
    list.setListPadding(new Insets(8, 8, 8, 8));

    list.addSelectionListener(event -> workbench.logEvent("selection: " + event.getSelected()));
    model.addListDataListener(event -> workbench.logEvent("model: " + model.getSize() + " items"));

    final JScrollPane scroll = new JScrollPane(list);
    scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
    workbench.setContainer(scroll);

    buildControls();
    refreshCode();
  }

  // The equivalent Java for the live list, read off the list itself rather than off the controls,
  // so the snippet cannot drift from what is on the stage. Model and renderer lead: they are the
  // half of a list a reader cannot infer from looking at one (#582).
  private void refreshCode() {
    workbench.setCode(
        """
        DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>(items);
        ElwhaItemList<String> list = new ElwhaItemList<>(model, (item, index) -> {
          ElwhaCard card = ElwhaCard.outlinedCard().setActionable(true).setSelectable(true);
          card.add(new ElwhaCardHeader().setTitle(item).setSubtitle("List item"));
          return card;
        });
        list.setOrientation(ElwhaListOrientation.%s);
        list.setSelectionMode(SelectionMode.%s);
        list.setMovementMode(MovementMode.%s);
        list.setColumns(%d);
        list.setItemGap(%d);
        list.setListPadding(new Insets(8, 8, 8, 8));

        list.addSelectionListener(event -> handle(event.getSelected()));
        """
            .formatted(
                list.getOrientation(),
                list.getSelectionMode(),
                list.getMovementMode(),
                list.getColumns(),
                list.getItemGap()));
  }

  /**
   * Returns the live container surface for mounting in the Showcase nav.
   *
   * @return the container workbench
   * @version v0.3.0
   * @since v0.3.0
   */
  JComponent component() {
    return workbench;
  }

  private void buildControls() {
    final WorkbenchControls controls = workbench.controls();

    final ElwhaSelectField<ElwhaListOrientation> orientation =
        ElwhaSelectField.outlined("Orientation");
    orientation.setOptions(List.of(ElwhaListOrientation.values()));
    orientation.setSelectedValue(ElwhaListOrientation.VERTICAL);
    orientation.addSelectionChangeListener(
        value -> {
          list.setOrientation(value);
          refreshCode();
        });

    final ElwhaSelectField<SelectionMode> selection = ElwhaSelectField.outlined("Selection mode");
    selection.setOptions(List.of(SelectionMode.values()));
    selection.setSelectedValue(list.getSelectionMode());
    selection.addSelectionChangeListener(
        value -> {
          list.setSelectionMode(value);
          refreshCode();
        });

    controls.addSection("List");
    controls.addControl("", orientation);
    controls.addControl("", selection);

    // Seeded from the live list, not from a literal: the spinner used to open on 1 against a list
    // sitting at its own default, so the control disagreed with the stage until it was touched.
    // The code view (#582) is what made the mismatch visible.
    final JSpinner columns = new JSpinner(new SpinnerNumberModel(list.getColumns(), 1, 6, 1));
    columns.addChangeListener(
        event -> {
          list.setColumns((Integer) columns.getValue());
          refreshCode();
        });
    final JSpinner gap = new JSpinner(new SpinnerNumberModel(8, 0, 30, 1));
    gap.addChangeListener(
        event -> {
          list.setItemGap((Integer) gap.getValue());
          refreshCode();
        });

    controls.addSection("Layout");
    controls.addControl("Columns (grid)", columns);
    controls.addControl("Item gap", gap);

    controls.addSection("Data");
    controls.addControl("", buildDataRow());
  }

  private JComponent buildDataRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    final ElwhaButton add = ElwhaButton.outlinedButton("Add");
    add.addActionListener(event -> model.add("New item " + (++addedCount)));

    final ElwhaButton remove = ElwhaButton.outlinedButton("Remove");
    remove.addActionListener(
        event -> {
          final int last = model.getItems().size() - 1;
          if (last >= 0) {
            model.remove(last);
          }
        });

    final ElwhaButton reset = ElwhaButton.outlinedButton("Reset");
    reset.addActionListener(event -> model.setItems(List.of(SAMPLE)));

    row.add(add);
    row.add(remove);
    row.add(reset);
    return row;
  }

  private static ElwhaCard renderCell(final String item, final int index) {
    final ElwhaCard card = ElwhaCard.outlinedCard().setActionable(true).setSelectable(true);
    card.add(new ElwhaCardHeader().setTitle(item).setSubtitle("List item"));
    card.add(new ElwhaCardSupportingText("A card rendered for one model item."));
    return card;
  }
}
