package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.list.CardAdapter;
import com.owspfm.elwha.card.list.CardSelectionMode;
import com.owspfm.elwha.card.list.DefaultCardListModel;
import com.owspfm.elwha.card.list.ElwhaCardList;
import com.owspfm.elwha.card.list.ReorderHandle;
import com.owspfm.elwha.list.ElwhaListOrientation;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Tabbed panel that exercises every feature of {@link ElwhaCardList} side-by-side with live
 * controls, a status log, and a snippet pane.
 *
 * <p>Layout:
 *
 * <ul>
 *   <li>Left: a focus {@link ElwhaCardList} backed by a {@link DefaultCardListModel} of {@link
 *       DemoItem}, plus a status panel that tails the most recent selection / reorder events
 *   <li>Right: live controls for orientation, columns, gap, padding, selection, reorder, filter,
 *       sort, empty/loading states, animations, and add/remove/shuffle buttons that mutate the
 *       underlying model
 *   <li>Bottom of right pane: code-snippet pane that updates with the Java needed to reproduce the
 *       current configuration
 * </ul>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class ElwhaCardListShowcase extends JPanel {

  private static final int CONTROL_GAP = 6;
  private static final String[] PRIORITY_LABELS = {"Low", "Medium", "High", "Urgent"};

  private final DefaultCardListModel<DemoItem> model = new DefaultCardListModel<>();
  private final ElwhaCardList<DemoItem> list;
  private final JTextArea status;
  private final JTextArea snippet;
  private final Random random = new Random(42);

  private JComboBox<ElwhaListOrientation> orientationBox;
  private JSlider columnsSlider;
  private JLabel columnsLabel;
  private JSlider gapSlider;
  private JLabel gapLabel;
  private JSlider paddingSlider;
  private JLabel paddingLabel;
  private JComboBox<CardSelectionMode> selectionBox;
  private JCheckBox reorderableBox;
  private JComboBox<ReorderHandle> reorderHandleBox;
  private JTextField filterField;
  private JComboBox<String> sortBox;
  private JCheckBox emptyBox;
  private JCheckBox loadingBox;
  private JCheckBox animateBox;
  private JSlider animationSlider;
  private JLabel animationLabel;

  /** Builds the showcase panel and seeds the model with sample items. */
  public ElwhaCardListShowcase() {
    super(new BorderLayout());

    seedModel();

    final CardAdapter<DemoItem> adapter =
        (item, index) ->
            new ElwhaCard()
                .setVariant(CardVariant.OUTLINED)
                .setHeader(item.title(), item.subtitle())
                .setBody(buildBody(item))
                .setCollapsible(true)
                .setCollapsed(true)
                .setAnimateCollapse(true);

    list = new ElwhaCardList<>(model, adapter);
    list.getSelectionModel().addSelectionListener(this::onSelectionChanged);
    list.addReorderListener(
        evt ->
            log(
                "Reorder: "
                    + evt.getItem().title()
                    + " ["
                    + evt.getFromIndex()
                    + " → "
                    + evt.getToIndex()
                    + "]"));

    status = new JTextArea(5, 30);
    status.setEditable(false);
    status.putClientProperty("FlatLaf.styleClass", "monospaced");

    snippet = new JTextArea();
    snippet.setEditable(false);
    snippet.putClientProperty("FlatLaf.styleClass", "monospaced");

    final JPanel left = buildLeft();
    final JPanel right = buildRight();

    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
    split.setResizeWeight(0.55);
    split.setDividerLocation(640);
    add(split, BorderLayout.CENTER);

    syncFromControls();
  }

  // ---------------------------------------------------------------- model

  private void seedModel() {
    model.add(
        new DemoItem("Cycle alpha", "Reinforcing loop, length 4", LocalDate.of(2026, 4, 12), 4));
    model.add(new DemoItem("Cycle beta", "Balancing loop, length 3", LocalDate.of(2026, 3, 28), 2));
    model.add(
        new DemoItem("Cycle gamma", "Reinforcing loop, length 5", LocalDate.of(2026, 5, 1), 3));
    model.add(
        new DemoItem("Cycle delta", "Balancing loop, length 4", LocalDate.of(2026, 1, 15), 1));
    model.add(
        new DemoItem("Cycle epsilon", "Reinforcing loop, length 6", LocalDate.of(2026, 4, 30), 4));
    model.add(new DemoItem("Cycle zeta", "Balancing loop, length 3", LocalDate.of(2026, 2, 9), 2));
    model.add(
        new DemoItem("Cycle eta", "Reinforcing loop, length 4", LocalDate.of(2026, 3, 14), 3));
    model.add(
        new DemoItem("Cycle theta", "Balancing loop, length 5", LocalDate.of(2026, 4, 22), 1));
    model.add(
        new DemoItem("Cycle iota", "Reinforcing loop, length 7", LocalDate.of(2026, 1, 30), 4));
    model.add(new DemoItem("Cycle kappa", "Balancing loop, length 4", LocalDate.of(2026, 5, 2), 2));
  }

  private static JPanel buildBody(final DemoItem item) {
    final JPanel body = new JPanel(new BorderLayout());
    body.setOpaque(false);
    final JLabel meta =
        new JLabel(
            "Date: " + item.date() + "    Priority: " + PRIORITY_LABELS[item.priority() - 1]);
    meta.putClientProperty("FlatLaf.styleClass", "small");
    body.add(meta, BorderLayout.CENTER);
    return body;
  }

  // ------------------------------------------------------------- left pane

  private JPanel buildLeft() {
    final JPanel left = new JPanel(new BorderLayout());
    left.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    final JLabel heading = new JLabel("ElwhaCardList");
    heading.putClientProperty("FlatLaf.styleClass", "h3");
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

    final JScrollPane scroll = new JScrollPane(list);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(20);

    final JPanel statusWrap = new JPanel(new BorderLayout());
    final JLabel statusLbl = new JLabel("Event log");
    statusLbl.putClientProperty("FlatLaf.styleClass", "h4");
    statusLbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
    statusWrap.add(statusLbl, BorderLayout.NORTH);
    statusWrap.add(new JScrollPane(status), BorderLayout.CENTER);
    statusWrap.setPreferredSize(new Dimension(400, 140));

    left.add(heading, BorderLayout.NORTH);
    left.add(scroll, BorderLayout.CENTER);
    left.add(statusWrap, BorderLayout.SOUTH);
    return left;
  }

  // ------------------------------------------------------------ right pane

  private JPanel buildRight() {
    final JPanel right = new JPanel(new BorderLayout());
    right.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    final JLabel heading = new JLabel("Live controls");
    heading.putClientProperty("FlatLaf.styleClass", "h3");
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    right.add(heading, BorderLayout.NORTH);

    final JPanel controls = new JPanel(new GridBagLayout());
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(CONTROL_GAP, CONTROL_GAP, CONTROL_GAP, CONTROL_GAP);

    addRow(controls, gc, "Orientation", buildOrientationBox());
    addRow(controls, gc, "Grid columns", buildColumnsControl());
    addRow(controls, gc, "Item gap", buildGapControl());
    addRow(controls, gc, "List padding", buildPaddingControl());
    addRow(controls, gc, "Selection", buildSelectionBox());
    addRow(controls, gc, "Reorder", buildReorderControls());
    addRow(controls, gc, "Filter", buildFilterField());
    addRow(controls, gc, "Sort", buildSortBox());
    addRow(controls, gc, "Empty state", buildEmptyBox());
    addRow(controls, gc, "Loading state", buildLoadingBox());
    addRow(controls, gc, "Animations", buildAnimateBox());
    addRow(controls, gc, "Animation duration", buildAnimationSlider());
    addRow(controls, gc, "Mutate model", buildMutateButtons());

    final JPanel snippetWrap = new JPanel(new BorderLayout());
    final JLabel snippetLbl = new JLabel("Java snippet");
    snippetLbl.putClientProperty("FlatLaf.styleClass", "h4");
    final JButton copy = new JButton("Copy");
    copy.addActionListener(
        e ->
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(snippet.getText()), null));
    final JPanel snippetHeader = new JPanel(new BorderLayout());
    snippetHeader.add(snippetLbl, BorderLayout.WEST);
    snippetHeader.add(copy, BorderLayout.EAST);
    snippetHeader.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
    snippetWrap.add(snippetHeader, BorderLayout.NORTH);
    snippetWrap.add(new JScrollPane(snippet), BorderLayout.CENTER);
    snippetWrap.setPreferredSize(new Dimension(400, 220));

    final JPanel center = new JPanel(new BorderLayout());
    center.add(new JScrollPane(controls), BorderLayout.CENTER);
    center.add(snippetWrap, BorderLayout.SOUTH);
    right.add(center, BorderLayout.CENTER);
    return right;
  }

  private static void addRow(
      final JPanel parent,
      final GridBagConstraints gc,
      final String label,
      final java.awt.Component control) {
    final JLabel lbl = new JLabel(label);
    gc.gridx = 0;
    gc.weightx = 0;
    parent.add(lbl, gc);
    gc.gridx = 1;
    gc.weightx = 1;
    parent.add(control, gc);
    gc.gridy++;
  }

  // ------------------------------------------------------------- controls

  private JComboBox<ElwhaListOrientation> buildOrientationBox() {
    orientationBox = new JComboBox<>(ElwhaListOrientation.values());
    orientationBox.addActionListener(e -> syncFromControls());
    return orientationBox;
  }

  private JPanel buildColumnsControl() {
    columnsSlider = new JSlider(1, 5, 2);
    columnsLabel = new JLabel("2");
    columnsLabel.setPreferredSize(new Dimension(28, 16));
    columnsSlider.addChangeListener(
        e -> {
          columnsLabel.setText(String.valueOf(columnsSlider.getValue()));
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(columnsSlider, BorderLayout.CENTER);
    wrap.add(columnsLabel, BorderLayout.EAST);
    return wrap;
  }

  private JPanel buildGapControl() {
    gapSlider = new JSlider(0, 32, 8);
    gapLabel = new JLabel("8");
    gapLabel.setPreferredSize(new Dimension(28, 16));
    gapSlider.addChangeListener(
        e -> {
          gapLabel.setText(String.valueOf(gapSlider.getValue()));
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(gapSlider, BorderLayout.CENTER);
    wrap.add(gapLabel, BorderLayout.EAST);
    return wrap;
  }

  private JPanel buildPaddingControl() {
    paddingSlider = new JSlider(0, 32, 8);
    paddingLabel = new JLabel("8");
    paddingLabel.setPreferredSize(new Dimension(28, 16));
    paddingSlider.addChangeListener(
        e -> {
          paddingLabel.setText(String.valueOf(paddingSlider.getValue()));
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(paddingSlider, BorderLayout.CENTER);
    wrap.add(paddingLabel, BorderLayout.EAST);
    return wrap;
  }

  private JComboBox<CardSelectionMode> buildSelectionBox() {
    selectionBox = new JComboBox<>(CardSelectionMode.values());
    selectionBox.setSelectedItem(CardSelectionMode.SINGLE);
    selectionBox.addActionListener(e -> syncFromControls());
    return selectionBox;
  }

  private JPanel buildReorderControls() {
    reorderableBox = new JCheckBox("Enabled");
    reorderableBox.setSelected(true);
    reorderableBox.addActionListener(e -> syncFromControls());

    reorderHandleBox = new JComboBox<>(ReorderHandle.values());
    reorderHandleBox.addActionListener(e -> syncFromControls());

    final JPanel wrap = new JPanel();
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));
    wrap.add(reorderableBox);
    wrap.add(Box.createHorizontalStrut(8));
    wrap.add(reorderHandleBox);
    return wrap;
  }

  private JTextField buildFilterField() {
    filterField = new JTextField();
    filterField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent event) {
                syncFromControls();
              }

              @Override
              public void removeUpdate(final DocumentEvent event) {
                syncFromControls();
              }

              @Override
              public void changedUpdate(final DocumentEvent event) {
                syncFromControls();
              }
            });
    return filterField;
  }

  private JComboBox<String> buildSortBox() {
    sortBox = new JComboBox<>(new String[] {"None", "Title", "Date", "Priority"});
    sortBox.addActionListener(e -> syncFromControls());
    return sortBox;
  }

  private JCheckBox buildEmptyBox() {
    emptyBox = new JCheckBox("Force empty placeholder");
    emptyBox.addActionListener(e -> syncFromControls());
    return emptyBox;
  }

  private JCheckBox buildLoadingBox() {
    loadingBox = new JCheckBox("Show loading state");
    loadingBox.addActionListener(e -> syncFromControls());
    return loadingBox;
  }

  private JCheckBox buildAnimateBox() {
    animateBox = new JCheckBox("Fade on add/remove");
    animateBox.setSelected(true);
    animateBox.addActionListener(e -> syncFromControls());
    return animateBox;
  }

  private JPanel buildAnimationSlider() {
    animationSlider = new JSlider(50, 800, 180);
    animationLabel = new JLabel("180 ms");
    animationLabel.setPreferredSize(new Dimension(60, 16));
    animationSlider.addChangeListener(
        e -> {
          animationLabel.setText(animationSlider.getValue() + " ms");
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(animationSlider, BorderLayout.CENTER);
    wrap.add(animationLabel, BorderLayout.EAST);
    return wrap;
  }

  private JPanel buildMutateButtons() {
    final JButton add = new JButton("Add");
    add.addActionListener(e -> addRandomItem());
    final JButton remove = new JButton("Remove last");
    remove.addActionListener(e -> removeLast());
    final JButton shuffle = new JButton("Shuffle");
    shuffle.addActionListener(e -> shuffleModel());

    final JPanel wrap = new JPanel();
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));
    wrap.add(add);
    wrap.add(Box.createHorizontalStrut(6));
    wrap.add(remove);
    wrap.add(Box.createHorizontalStrut(6));
    wrap.add(shuffle);
    return wrap;
  }

  // -------------------------------------------------------------- glue

  private void syncFromControls() {
    final ElwhaListOrientation orientation =
        (ElwhaListOrientation) orientationBox.getSelectedItem();
    final boolean grid = orientation == ElwhaListOrientation.GRID;
    columnsSlider.setEnabled(grid);
    columnsLabel.setEnabled(grid);

    list.setOrientation(orientation)
        .setColumns(columnsSlider.getValue())
        .setItemGap(gapSlider.getValue())
        .setListPadding(
            new Insets(
                paddingSlider.getValue(),
                paddingSlider.getValue(),
                paddingSlider.getValue(),
                paddingSlider.getValue()))
        .setSelectionMode((CardSelectionMode) selectionBox.getSelectedItem())
        .setReorderable(reorderableBox.isSelected())
        .setReorderHandle((ReorderHandle) reorderHandleBox.getSelectedItem())
        .setFilter(buildFilter())
        .setSortOrder(buildComparator())
        .setLoading(loadingBox.isSelected())
        .setAnimateChanges(animateBox.isSelected())
        .setAnimationDuration(animationSlider.getValue());

    if (emptyBox.isSelected()) {
      list.setFilter(item -> false);
    }

    updateSnippet();
  }

  private Predicate<DemoItem> buildFilter() {
    final String text = filterField.getText();
    if (text == null || text.isEmpty()) {
      return null;
    }
    final String lower = text.toLowerCase();
    return item ->
        item.title().toLowerCase().contains(lower) || item.subtitle().toLowerCase().contains(lower);
  }

  private Comparator<DemoItem> buildComparator() {
    final String which = (String) sortBox.getSelectedItem();
    if (which == null || "None".equals(which)) {
      return null;
    }
    return switch (which) {
      case "Title" -> Comparator.comparing(DemoItem::title);
      case "Date" -> Comparator.comparing(DemoItem::date);
      case "Priority" -> Comparator.comparingInt(DemoItem::priority).reversed();
      default -> null;
    };
  }

  private void onSelectionChanged(
      final com.owspfm.elwha.card.list.CardSelectionEvent<DemoItem> event) {
    final List<DemoItem> selected = event.getSelected();
    if (selected.isEmpty()) {
      log("Selection cleared");
      return;
    }
    final StringBuilder sb = new StringBuilder("Selected: ");
    for (int i = 0; i < selected.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(selected.get(i).title());
    }
    log(sb.toString());
  }

  private void log(final String message) {
    status.append(message + "\n");
    status.setCaretPosition(status.getDocument().getLength());
  }

  // ----------------------------------------------------------- mutations

  private void addRandomItem() {
    final int n = model.getSize() + 1;
    model.add(
        new DemoItem(
            "Cycle #" + n,
            "Auto-added at " + LocalDate.now(),
            LocalDate.now(),
            1 + random.nextInt(4)));
  }

  private void removeLast() {
    if (model.getSize() == 0) {
      return;
    }
    model.remove(model.getSize() - 1);
  }

  private void shuffleModel() {
    final List<DemoItem> snapshot = new ArrayList<>();
    for (DemoItem t : model) {
      snapshot.add(t);
    }
    Collections.shuffle(snapshot, random);
    while (model.getSize() > 0) {
      model.remove(model.getSize() - 1);
    }
    model.addAll(snapshot);
  }

  // ------------------------------------------------------------ snippet

  private void updateSnippet() {
    final StringBuilder sb = new StringBuilder(512);
    sb.append("DefaultCardListModel<DemoItem> model = new DefaultCardListModel<>(items);\n");
    sb.append("CardAdapter<DemoItem> adapter = (item, idx) ->\n");
    sb.append("    new ElwhaCard()\n");
    sb.append("        .setVariant(CardVariant.OUTLINED)\n");
    sb.append("        .setHeader(item.title(), item.subtitle())\n");
    sb.append("        .setBody(buildBody(item));\n\n");
    sb.append("ElwhaCardList<DemoItem> list = new ElwhaCardList<>(model, adapter)\n");
    sb.append("    .setOrientation(Orientation.")
        .append(((ElwhaListOrientation) orientationBox.getSelectedItem()).name())
        .append(")\n");
    if (orientationBox.getSelectedItem() == ElwhaListOrientation.GRID) {
      sb.append("    .setColumns(").append(columnsSlider.getValue()).append(")\n");
    }
    sb.append("    .setItemGap(").append(gapSlider.getValue()).append(")\n");
    sb.append("    .setListPadding(new Insets(")
        .append(paddingSlider.getValue())
        .append(", ")
        .append(paddingSlider.getValue())
        .append(", ")
        .append(paddingSlider.getValue())
        .append(", ")
        .append(paddingSlider.getValue())
        .append("))\n");
    sb.append("    .setSelectionMode(CardSelectionMode.")
        .append(((CardSelectionMode) selectionBox.getSelectedItem()).name())
        .append(")\n");
    if (reorderableBox.isSelected()) {
      sb.append("    .setReorderable(true)\n");
      sb.append("    .setReorderHandle(ReorderHandle.")
          .append(((ReorderHandle) reorderHandleBox.getSelectedItem()).name())
          .append(")\n");
    }
    final String filterText = filterField.getText();
    if (filterText != null && !filterText.isEmpty()) {
      sb.append("    .setFilter(item -> item.title().toLowerCase().contains(\"")
          .append(filterText.toLowerCase())
          .append("\"))\n");
    }
    final Object sortKey = sortBox.getSelectedItem();
    if (sortKey != null && !"None".equals(sortKey)) {
      sb.append("    .setSortOrder(Comparator.comparing(DemoItem::")
          .append(((String) sortKey).toLowerCase())
          .append("))\n");
    }
    if (loadingBox.isSelected()) {
      sb.append("    .setLoading(true)\n");
    }
    if (animateBox.isSelected()) {
      sb.append("    .setAnimateChanges(true)\n");
      sb.append("    .setAnimationDuration(").append(animationSlider.getValue()).append(")\n");
    }
    sb.append(";\n");
    sb.append("list.getSelectionModel().addSelectionListener(evt -> ...);\n");
    sb.append("list.addReorderListener(evt -> ...);\n");
    snippet.setText(sb.toString());
    snippet.setCaretPosition(0);
  }
}
