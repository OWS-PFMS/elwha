package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.list.DefaultElwhaListModel;
import com.owspfm.elwha.list.ElwhaItemList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.list.MovementMode;
import com.owspfm.elwha.list.SelectionMode;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/**
 * Showcase tab for a card-rendering {@link ElwhaItemList}. Left: a live list of cards. Right:
 * controls for orientation + selection mode + a status log of recent selection / reorder events.
 *
 * <p>Use Cmd+↑ / Cmd+↓ to reorder, Delete / Cmd+Backspace to remove, right-click for the context
 * menu, click + drag to reorder with the mouse.
 *
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.2.0
 */
public final class ElwhaCardListShowcase extends JPanel {

  private final DefaultElwhaListModel<String> model;
  private final ElwhaItemList<String> list;
  private final JTextArea status;

  /** Builds the showcase tab. */
  public ElwhaCardListShowcase() {
    super(new BorderLayout());

    model =
        new DefaultElwhaListModel<>(
            new ArrayList<>(
                List.of(
                    "Trip plan — Olympic Hot Springs",
                    "Cycle: switchbacks at mile 4",
                    "Cycle: river crossing at mile 7",
                    "Cycle: alpine meadow at mile 9",
                    "Trip notes — pack for rain")));

    list = new ElwhaItemList<>(model, this::renderCell);
    list.setOrientation(ElwhaListOrientation.VERTICAL);
    list.setSelectionMode(SelectionMode.MULTIPLE);
    list.setMovementMode(MovementMode.MOVABLE);

    status = new JTextArea(8, 32);
    status.setEditable(false);
    status.putClientProperty("FlatLaf.styleClass", "monospaced");

    list.addSelectionListener(event -> log("selection: " + event.getSelected()));
    model.addListDataListener(event -> log("model: " + model.getSize() + " items"));

    final JScrollPane listScroll = new JScrollPane(list);
    listScroll.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 8));
    listScroll.getVerticalScrollBar().setUnitIncrement(16);

    final JPanel right = new JPanel(new BorderLayout());
    right.add(buildControls(), BorderLayout.NORTH);
    right.add(new JScrollPane(status), BorderLayout.CENTER);
    right.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 16));

    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, right);
    split.setResizeWeight(0.6);
    split.setDividerLocation(620);
    add(split, BorderLayout.CENTER);
  }

  private JComponent buildControls() {
    final JPanel p = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.insets = new Insets(4, 4, 4, 4);

    final ElwhaSelectField<ElwhaListOrientation> orientationBox =
        ElwhaSelectField.outlined("Orientation");
    orientationBox.setOptions(List.of(ElwhaListOrientation.values()));
    orientationBox.setSelectedValue(ElwhaListOrientation.VERTICAL);
    orientationBox.addSelectionChangeListener(
        v -> {
          list.setOrientation(v);
          list.revalidate();
          list.repaint();
        });
    addRow(p, gbc, orientationBox);

    final ElwhaSelectField<SelectionMode> modeBox = ElwhaSelectField.outlined("Selection mode");
    modeBox.setOptions(List.of(SelectionMode.values()));
    modeBox.setSelectedValue(SelectionMode.MULTIPLE);
    modeBox.addSelectionChangeListener(list::setSelectionMode);
    addRow(p, gbc, modeBox);

    addRow(p, gbc, newCheck("Enabled", true, list::setEnabled));

    addRow(p, gbc, button("Add item", () -> model.add("New item " + model.getItems().size())));
    addRow(
        p,
        gbc,
        button(
            "Remove selected",
            () -> {
              final List<String> selected = list.getSelectionModel().getSelected();
              final List<String> items = new ArrayList<>(model.getItems());
              items.removeAll(selected);
              model.setItems(items);
            }));
    addRow(p, gbc, button("Clear log", () -> status.setText("")));

    gbc.weighty = 1;
    p.add(Box.createVerticalGlue(), gbc);
    p.setPreferredSize(new Dimension(260, p.getPreferredSize().height));
    return p;
  }

  private ElwhaCard renderCell(final String item, final int index) {
    final ElwhaCard card = ElwhaCard.outlinedCard().setActionable(true).setSelectable(true);
    card.add(new ElwhaCardHeader().setTitle(item));
    card.add(
        new ElwhaCardSupportingText(
            "Cmd+↑/↓ reorder · click+drag · right-click menu · Delete to remove"));
    return card;
  }

  private void log(final String line) {
    status.append(line + "\n");
    status.setCaretPosition(status.getDocument().getLength());
  }

  private static ElwhaCheckbox newCheck(
      final String label,
      final boolean initial,
      final java.util.function.Consumer<Boolean> onChange) {
    final ElwhaCheckbox c = new ElwhaCheckbox(label);
    c.setChecked(initial);
    c.addActionListener(e -> onChange.accept(c.isChecked()));
    return c;
  }

  private static ElwhaButton button(final String label, final Runnable action) {
    final ElwhaButton b = ElwhaButton.outlinedButton(label);
    b.addActionListener(e -> action.run());
    return b;
  }

  private static void addRow(final JPanel p, final GridBagConstraints gbc, final JComponent c) {
    p.add(c, gbc);
    gbc.gridy++;
  }
}
