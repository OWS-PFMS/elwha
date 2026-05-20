package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.list.CardSelectionMode;
import com.owspfm.elwha.card.list.DefaultCardListModel;
import com.owspfm.elwha.card.list.ElwhaCardList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

/**
 * The Elwha Showcase's Card List container demo, mounted on the shared {@link ContainerWorkbench}
 * scaffold: a live {@link ElwhaCardList} of sample items rendered as cards, a controls column for
 * the list options, and an event log that surfaces selection and model-change callbacks.
 *
 * <p>This is the {@code ContainerWorkbench}-pattern migration of the standalone Card List showcase
 * — a multi-instance surface a single live component cannot express.
 *
 * @author Charles Bryan
 * @version v0.3.0
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
  private final DefaultCardListModel<String> model = new DefaultCardListModel<>(List.of(SAMPLE));
  private final ElwhaCardList<String> list = new ElwhaCardList<>(model);
  private int addedCount;

  /**
   * Builds the Card List container — model, list, controls, and event-log wiring.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  CardListContainer() {
    list.setCellRenderer(CardListContainer::renderCell);
    list.getSelectionModel().setSelectionMode(CardSelectionMode.MULTIPLE);
    list.setItemGap(8);
    list.setListPadding(new Insets(8, 8, 8, 8));

    list.getSelectionModel()
        .addChangeListener(
            selection -> workbench.logEvent("selection: " + selection.getSelectedItems()));
    model.addChangeListener(
        changed -> workbench.logEvent("model: " + changed.getItems().size() + " items"));

    final JScrollPane scroll = new JScrollPane(list);
    scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
    workbench.setContainer(scroll);

    buildControls();
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

    final JComboBox<ElwhaListOrientation> orientation =
        new JComboBox<>(ElwhaListOrientation.values());
    orientation.setSelectedItem(ElwhaListOrientation.VERTICAL);
    orientation.addActionListener(
        event -> list.setOrientation((ElwhaListOrientation) orientation.getSelectedItem()));

    final JComboBox<CardSelectionMode> selection = new JComboBox<>(CardSelectionMode.values());
    selection.setSelectedItem(list.getSelectionModel().getSelectionMode());
    selection.addActionListener(
        event ->
            list.getSelectionModel()
                .setSelectionMode((CardSelectionMode) selection.getSelectedItem()));

    controls.addSection("List");
    controls.addControl("Orientation", orientation);
    controls.addControl("Selection mode", selection);

    final JSpinner columns = new JSpinner(new SpinnerNumberModel(1, 1, 6, 1));
    columns.addChangeListener(event -> list.setColumns((Integer) columns.getValue()));
    final JSpinner gap = new JSpinner(new SpinnerNumberModel(8, 0, 30, 1));
    gap.addChangeListener(event -> list.setItemGap((Integer) gap.getValue()));

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

  private static ElwhaCard renderCell(final String item) {
    final ElwhaCard card = ElwhaCard.outlinedCard().setActionable(true).setSelectable(true);
    card.add(new ElwhaCardHeader().setTitle(item).setSubtitle("List item"));
    card.add(new ElwhaCardSupportingText("A card rendered for one model item."));
    return card;
  }
}
