package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.list.CardSelectionMode;
import com.owspfm.elwha.card.list.DefaultCardListModel;
import com.owspfm.elwha.card.list.ElwhaCardList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/**
 * Showcase tab for the V3 {@link ElwhaCardList}. Left: a live list of cards. Right: controls for
 * orientation + selection mode + a status log of recent selection / reorder events.
 *
 * <p>Use Cmd+↑ / Cmd+↓ to reorder, Delete / Cmd+Backspace to remove, right-click for the context
 * menu, click + drag to reorder with the mouse.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardListShowcase extends JPanel {

  private final DefaultCardListModel<String> model;
  private final ElwhaCardList<String> list;
  private final JTextArea status;

  /** Builds the showcase tab. */
  public ElwhaCardListShowcase() {
    super(new BorderLayout());

    model =
        new DefaultCardListModel<>(
            new ArrayList<>(
                List.of(
                    "Trip plan — Olympic Hot Springs",
                    "Cycle: switchbacks at mile 4",
                    "Cycle: river crossing at mile 7",
                    "Cycle: alpine meadow at mile 9",
                    "Trip notes — pack for rain")));

    list = new ElwhaCardList<>(model);
    list.setOrientation(ElwhaListOrientation.VERTICAL);
    list.getSelectionModel().setSelectionMode(CardSelectionMode.MULTIPLE);
    list.setCellRenderer(this::renderCell);

    status = new JTextArea(8, 32);
    status.setEditable(false);
    status.putClientProperty("FlatLaf.styleClass", "monospaced");

    list.getSelectionModel().addChangeListener(sm -> log("selection: " + sm.getSelectedItems()));
    model.addChangeListener(m -> log("model: " + m.getItems().size() + " items"));

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

    final JComboBox<ElwhaListOrientation> orientationBox =
        new JComboBox<>(ElwhaListOrientation.values());
    orientationBox.setSelectedItem(ElwhaListOrientation.VERTICAL);
    orientationBox.addActionListener(
        e -> {
          list.setOrientation((ElwhaListOrientation) orientationBox.getSelectedItem());
          list.revalidate();
          list.repaint();
        });
    addLabeled(p, gbc, "Orientation", orientationBox);

    final JComboBox<CardSelectionMode> modeBox = new JComboBox<>(CardSelectionMode.values());
    modeBox.setSelectedItem(CardSelectionMode.MULTIPLE);
    modeBox.addActionListener(
        e ->
            list.getSelectionModel()
                .setSelectionMode((CardSelectionMode) modeBox.getSelectedItem()));
    addLabeled(p, gbc, "Selection mode", modeBox);

    addRow(p, gbc, newCheck("Enabled", true, list::setEnabled));

    addRow(p, gbc, button("Add item", () -> model.add("New item " + model.getItems().size())));
    addRow(
        p,
        gbc,
        button(
            "Remove selected",
            () -> {
              final java.util.Set<String> sel = list.getSelectionModel().getSelectedItems();
              final List<String> items = new ArrayList<>(model.getItems());
              items.removeAll(sel);
              model.setItems(items);
            }));
    addRow(p, gbc, button("Clear log", () -> status.setText("")));

    gbc.weighty = 1;
    p.add(Box.createVerticalGlue(), gbc);
    p.setPreferredSize(new Dimension(260, 220));
    return p;
  }

  private ElwhaCard renderCell(final String item) {
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

  private static JCheckBox newCheck(
      final String label,
      final boolean initial,
      final java.util.function.Consumer<Boolean> onChange) {
    final JCheckBox c = new JCheckBox(label, initial);
    c.addActionListener(e -> onChange.accept(c.isSelected()));
    return c;
  }

  private static JButton button(final String label, final Runnable action) {
    final JButton b = new JButton(label);
    b.addActionListener(e -> action.run());
    return b;
  }

  private static void addLabeled(
      final JPanel p, final GridBagConstraints gbc, final String label, final JComponent c) {
    addRow(p, gbc, new JLabel(label));
    addRow(p, gbc, c);
  }

  private static void addRow(final JPanel p, final GridBagConstraints gbc, final JComponent c) {
    p.add(c, gbc);
    gbc.gridy++;
  }
}
