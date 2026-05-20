package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.chip.ChipInteractionMode;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.chip.list.ChipSelectionMode;
import com.owspfm.elwha.chip.list.DefaultChipListModel;
import com.owspfm.elwha.chip.list.ElwhaChipList;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.list.ElwhaListOrientation;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

/**
 * The Elwha Showcase's Chip List container demo, mounted on the shared {@link ContainerWorkbench}
 * scaffold: a live {@link ElwhaChipList} of sample items, a controls column for every list option,
 * and an event log that makes selection / reorder / pin / anchor callbacks visible.
 *
 * <p>This is the {@code ContainerWorkbench}-pattern migration of the standalone playground's live
 * list — a multi-instance surface a single live component cannot express.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
final class ChipListContainer {

  private static final String[] SAMPLE = {
    "Demand", "Supply", "Capacity", "Quality", "Latency", "Cost",
    "Revenue", "Risk", "Adoption", "Churn", "Inventory", "Throughput"
  };
  private static final int CHIP_ICON_SIZE = 14;

  private final ContainerWorkbench workbench = new ContainerWorkbench();
  private final DefaultChipListModel<String> model = new DefaultChipListModel<>();
  private final Set<String> pinned = new HashSet<>();
  private final ElwhaChipList<String> list;
  private String anchor;
  private int addedCount;

  /**
   * Builds the Chip List container — model, list, controls, and event-log wiring.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  ChipListContainer() {
    for (final String item : SAMPLE) {
      model.add(item);
    }

    list = new ElwhaChipList<>(model, (item, index) -> buildChip(item));
    list.setItemGap(6).setListPadding(new Insets(8, 8, 8, 8));
    list.setSelectionMode(ChipSelectionMode.MULTIPLE);
    list.setMovementMode(ElwhaChipList.MovementMode.PINNED);
    list.setPinAffordance(ElwhaChipList.IconAffordance.BUTTON);
    list.setAnchorAffordance(ElwhaChipList.IconAffordance.BUTTON);
    armBindings(ElwhaChipList.MovementMode.PINNED);

    list.getSelectionModel()
        .addSelectionListener(event -> workbench.logEvent("selection: " + event.getSelected()));
    list.addReorderListener(
        event ->
            workbench.logEvent(
                "reordered: "
                    + event.getItem()
                    + " ("
                    + event.getFromIndex()
                    + " → "
                    + event.getToIndex()
                    + ")"));

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

    final JComboBox<ChipSelectionMode> selection = new JComboBox<>(ChipSelectionMode.values());
    selection.setSelectedItem(list.getSelectionMode());
    selection.addActionListener(
        event -> list.setSelectionMode((ChipSelectionMode) selection.getSelectedItem()));

    final JComboBox<ElwhaChipList.MovementMode> movement =
        new JComboBox<>(ElwhaChipList.MovementMode.values());
    movement.setSelectedItem(list.getMovementMode());
    movement.addActionListener(
        event -> {
          final ElwhaChipList.MovementMode mode =
              (ElwhaChipList.MovementMode) movement.getSelectedItem();
          list.setMovementMode(mode);
          armBindings(mode);
          workbench.logEvent("movement mode: " + mode);
        });

    controls.addSection("List");
    controls.addControl("Orientation", orientation);
    controls.addControl("Selection mode", selection);
    controls.addControl("Movement mode", movement);

    final JSpinner columns = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
    columns.addChangeListener(event -> list.setColumns((Integer) columns.getValue()));
    final JSpinner gap = new JSpinner(new SpinnerNumberModel(6, 0, 30, 1));
    gap.addChangeListener(event -> list.setItemGap((Integer) gap.getValue()));

    controls.addSection("Layout");
    controls.addControl("Columns (grid)", columns);
    controls.addControl("Item gap", gap);

    final JComboBox<ElwhaChipList.IconAffordance> pin =
        new JComboBox<>(ElwhaChipList.IconAffordance.values());
    pin.setSelectedItem(list.getPinAffordance());
    pin.addActionListener(
        event -> list.setPinAffordance((ElwhaChipList.IconAffordance) pin.getSelectedItem()));
    final JComboBox<ElwhaChipList.IconAffordance> anchorAffordance =
        new JComboBox<>(ElwhaChipList.IconAffordance.values());
    anchorAffordance.setSelectedItem(list.getAnchorAffordance());
    anchorAffordance.addActionListener(
        event ->
            list.setAnchorAffordance(
                (ElwhaChipList.IconAffordance) anchorAffordance.getSelectedItem()));

    controls.addSection("Affordances");
    controls.addControl("Pin", pin);
    controls.addControl("Anchor", anchorAffordance);

    controls.addSection("Data");
    controls.addControl("", buildDataRow());
  }

  private JComponent buildDataRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    final ElwhaButton add = ElwhaButton.outlinedButton("Add");
    add.addActionListener(
        event -> {
          final String item = "New " + (++addedCount);
          model.add(item);
          workbench.logEvent("added: " + item);
        });

    final ElwhaButton clear = ElwhaButton.outlinedButton("Clear");
    clear.addActionListener(
        event -> {
          model.clear();
          workbench.logEvent("cleared all");
        });

    final ElwhaButton reset = ElwhaButton.outlinedButton("Reset");
    reset.addActionListener(
        event -> {
          model.clear();
          for (final String item : SAMPLE) {
            model.add(item);
          }
          workbench.logEvent("reset to sample data");
        });

    row.add(add);
    row.add(clear);
    row.add(reset);
    return row;
  }

  private void armBindings(final ElwhaChipList.MovementMode mode) {
    if (mode == ElwhaChipList.MovementMode.PINNED) {
      list.setPinPredicate(pinned::contains);
      list.setPinAction(
          (item, pinNow) -> {
            if (pinNow) {
              pinned.add(item);
            } else {
              pinned.remove(item);
            }
            list.pinStateChanged();
            workbench.logEvent((pinNow ? "pinned: " : "unpinned: ") + item);
          });
    } else if (mode == ElwhaChipList.MovementMode.ANCHORED) {
      list.setAnchorPredicate(item -> item != null && item.equals(anchor));
      list.setAnchorAction(
          item -> {
            anchor = item;
            list.anchorStateChanged();
            workbench.logEvent("anchored: " + item);
          });
    }
  }

  private ElwhaChip buildChip(final String item) {
    final ElwhaChip chip =
        new ElwhaChip(item)
            .setVariant(ChipVariant.FILLED)
            .setInteractionMode(ChipInteractionMode.CLICKABLE);
    chip.setTrailingIcon(
        MaterialIcons.delete(CHIP_ICON_SIZE),
        "Remove " + item,
        () -> {
          model.remove(item);
          workbench.logEvent("removed: " + item);
        });
    chip.attachContextMenu(
        () -> {
          final JPopupMenu menu = new JPopupMenu();
          final ElwhaChipList.MovementMode mode = list.getMovementMode();
          if (mode == ElwhaChipList.MovementMode.PINNED) {
            menu.add(list.createPinMenuItem(item));
            menu.addSeparator();
          } else if (mode == ElwhaChipList.MovementMode.ANCHORED) {
            menu.add(list.createAnchorMenuItem(item));
            menu.addSeparator();
          }
          final JMenuItem details = new JMenuItem("Show details");
          details.addActionListener(
              event ->
                  JOptionPane.showMessageDialog(
                      list, "Details for: " + item, "Chip", JOptionPane.INFORMATION_MESSAGE));
          menu.add(details);
          final JMenuItem rename = new JMenuItem("Rename…");
          rename.addActionListener(
              event -> {
                final String renamed = JOptionPane.showInputDialog(list, "Rename chip:", item);
                if (renamed != null && !renamed.isEmpty()) {
                  final int index = model.indexOf(item);
                  if (index >= 0) {
                    model.set(index, renamed);
                    workbench.logEvent("renamed: " + item + " → " + renamed);
                  }
                }
              });
          menu.add(rename);
          menu.addSeparator();
          final JMenuItem remove = new JMenuItem("Remove");
          remove.addActionListener(
              event -> {
                model.remove(item);
                workbench.logEvent("removed: " + item);
              });
          menu.add(remove);
          return menu;
        });
    return chip;
  }
}
