package com.owspfm.ui.components.card.playground;

import com.owspfm.ui.components.card.CardVariant;
import com.owspfm.ui.components.card.FlatCard;
import com.owspfm.ui.components.card.list.CardAdapter;
import com.owspfm.ui.components.card.list.CardSelectionMode;
import com.owspfm.ui.components.card.list.DefaultCardListModel;
import com.owspfm.ui.components.card.list.FlatCardList;
import com.owspfm.ui.components.card.list.ReorderHandle;
import com.owspfm.ui.components.flatlist.FlatListOrientation;
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
 * Tabbed panel that exercises every feature of {@link FlatCardList} side-by-side with live
 * controls, a status log, and a snippet pane.
 *
 * <p>Layout:
 *
 * <ul>
 *   <li>Left: a focus {@link FlatCardList} backed by a {@link DefaultCardListModel} of {@link
 *       DemoItem}, plus a status panel that tails the most recent selection / reorder events
 *   <li>Right: live controls for orientation, columns, gap, padding, selection, reorder, filter,
 *       sort, empty/loading states, animations, and add/remove/shuffle buttons that mutate the
 *       underlying model
 *   <li>Bottom of right pane: code-snippet pane that updates with the Java needed to reproduce the
 *       current configuration
 * </ul>
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.2
 */
public final class FlatCardListShowcase extends JPanel {

  private static final int CONTROL_GAP = 6;
  private static final String[] PRIORITY_LABELS = {"Low", "Medium", "High", "Urgent"};

  private final DefaultCardListModel<DemoItem> myModel = new DefaultCardListModel<>();
  private final FlatCardList<DemoItem> myList;
  private final JTextArea myStatus;
  private final JTextArea mySnippet;
  private final Random myRandom = new Random(42);

  private JComboBox<FlatListOrientation> myOrientationBox;
  private JSlider myColumnsSlider;
  private JLabel myColumnsLabel;
  private JSlider myGapSlider;
  private JLabel myGapLabel;
  private JSlider myPaddingSlider;
  private JLabel myPaddingLabel;
  private JComboBox<CardSelectionMode> mySelectionBox;
  private JCheckBox myReorderableBox;
  private JComboBox<ReorderHandle> myReorderHandleBox;
  private JTextField myFilterField;
  private JComboBox<String> mySortBox;
  private JCheckBox myEmptyBox;
  private JCheckBox myLoadingBox;
  private JCheckBox myAnimateBox;
  private JSlider myAnimationSlider;
  private JLabel myAnimationLabel;

  /** Builds the showcase panel and seeds the model with sample items. */
  public FlatCardListShowcase() {
    super(new BorderLayout());

    seedModel();

    final CardAdapter<DemoItem> adapter =
        (item, index) ->
            new FlatCard()
                .setVariant(CardVariant.OUTLINED)
                .setHeader(item.title(), item.subtitle())
                .setBody(buildBody(item))
                .setCollapsible(true)
                .setCollapsed(true)
                .setAnimateCollapse(true);

    myList = new FlatCardList<>(myModel, adapter);
    myList.getSelectionModel().addSelectionListener(this::onSelectionChanged);
    myList.addReorderListener(
        evt ->
            log(
                "Reorder: "
                    + evt.getItem().title()
                    + " ["
                    + evt.getFromIndex()
                    + " → "
                    + evt.getToIndex()
                    + "]"));

    myStatus = new JTextArea(5, 30);
    myStatus.setEditable(false);
    myStatus.putClientProperty("FlatLaf.styleClass", "monospaced");

    mySnippet = new JTextArea();
    mySnippet.setEditable(false);
    mySnippet.putClientProperty("FlatLaf.styleClass", "monospaced");

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
    myModel.add(
        new DemoItem("Cycle alpha", "Reinforcing loop, length 4", LocalDate.of(2026, 4, 12), 4));
    myModel.add(
        new DemoItem("Cycle beta", "Balancing loop, length 3", LocalDate.of(2026, 3, 28), 2));
    myModel.add(
        new DemoItem("Cycle gamma", "Reinforcing loop, length 5", LocalDate.of(2026, 5, 1), 3));
    myModel.add(
        new DemoItem("Cycle delta", "Balancing loop, length 4", LocalDate.of(2026, 1, 15), 1));
    myModel.add(
        new DemoItem("Cycle epsilon", "Reinforcing loop, length 6", LocalDate.of(2026, 4, 30), 4));
    myModel.add(
        new DemoItem("Cycle zeta", "Balancing loop, length 3", LocalDate.of(2026, 2, 9), 2));
    myModel.add(
        new DemoItem("Cycle eta", "Reinforcing loop, length 4", LocalDate.of(2026, 3, 14), 3));
    myModel.add(
        new DemoItem("Cycle theta", "Balancing loop, length 5", LocalDate.of(2026, 4, 22), 1));
    myModel.add(
        new DemoItem("Cycle iota", "Reinforcing loop, length 7", LocalDate.of(2026, 1, 30), 4));
    myModel.add(
        new DemoItem("Cycle kappa", "Balancing loop, length 4", LocalDate.of(2026, 5, 2), 2));
  }

  private static JPanel buildBody(final DemoItem theItem) {
    final JPanel body = new JPanel(new BorderLayout());
    body.setOpaque(false);
    final JLabel meta =
        new JLabel(
            "Date: " + theItem.date() + "    Priority: " + PRIORITY_LABELS[theItem.priority() - 1]);
    meta.putClientProperty("FlatLaf.styleClass", "small");
    body.add(meta, BorderLayout.CENTER);
    return body;
  }

  // ------------------------------------------------------------- left pane

  private JPanel buildLeft() {
    final JPanel left = new JPanel(new BorderLayout());
    left.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    final JLabel heading = new JLabel("FlatCardList");
    heading.putClientProperty("FlatLaf.styleClass", "h3");
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

    final JScrollPane scroll = new JScrollPane(myList);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(20);

    final JPanel statusWrap = new JPanel(new BorderLayout());
    final JLabel statusLbl = new JLabel("Event log");
    statusLbl.putClientProperty("FlatLaf.styleClass", "h4");
    statusLbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
    statusWrap.add(statusLbl, BorderLayout.NORTH);
    statusWrap.add(new JScrollPane(myStatus), BorderLayout.CENTER);
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
                .setContents(new StringSelection(mySnippet.getText()), null));
    final JPanel snippetHeader = new JPanel(new BorderLayout());
    snippetHeader.add(snippetLbl, BorderLayout.WEST);
    snippetHeader.add(copy, BorderLayout.EAST);
    snippetHeader.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
    snippetWrap.add(snippetHeader, BorderLayout.NORTH);
    snippetWrap.add(new JScrollPane(mySnippet), BorderLayout.CENTER);
    snippetWrap.setPreferredSize(new Dimension(400, 220));

    final JPanel center = new JPanel(new BorderLayout());
    center.add(new JScrollPane(controls), BorderLayout.CENTER);
    center.add(snippetWrap, BorderLayout.SOUTH);
    right.add(center, BorderLayout.CENTER);
    return right;
  }

  private static void addRow(
      final JPanel theParent,
      final GridBagConstraints theGc,
      final String theLabel,
      final java.awt.Component theControl) {
    final JLabel lbl = new JLabel(theLabel);
    theGc.gridx = 0;
    theGc.weightx = 0;
    theParent.add(lbl, theGc);
    theGc.gridx = 1;
    theGc.weightx = 1;
    theParent.add(theControl, theGc);
    theGc.gridy++;
  }

  // ------------------------------------------------------------- controls

  private JComboBox<FlatListOrientation> buildOrientationBox() {
    myOrientationBox = new JComboBox<>(FlatListOrientation.values());
    myOrientationBox.addActionListener(e -> syncFromControls());
    return myOrientationBox;
  }

  private JPanel buildColumnsControl() {
    myColumnsSlider = new JSlider(1, 5, 2);
    myColumnsLabel = new JLabel("2");
    myColumnsLabel.setPreferredSize(new Dimension(28, 16));
    myColumnsSlider.addChangeListener(
        e -> {
          myColumnsLabel.setText(String.valueOf(myColumnsSlider.getValue()));
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(myColumnsSlider, BorderLayout.CENTER);
    wrap.add(myColumnsLabel, BorderLayout.EAST);
    return wrap;
  }

  private JPanel buildGapControl() {
    myGapSlider = new JSlider(0, 32, 8);
    myGapLabel = new JLabel("8");
    myGapLabel.setPreferredSize(new Dimension(28, 16));
    myGapSlider.addChangeListener(
        e -> {
          myGapLabel.setText(String.valueOf(myGapSlider.getValue()));
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(myGapSlider, BorderLayout.CENTER);
    wrap.add(myGapLabel, BorderLayout.EAST);
    return wrap;
  }

  private JPanel buildPaddingControl() {
    myPaddingSlider = new JSlider(0, 32, 8);
    myPaddingLabel = new JLabel("8");
    myPaddingLabel.setPreferredSize(new Dimension(28, 16));
    myPaddingSlider.addChangeListener(
        e -> {
          myPaddingLabel.setText(String.valueOf(myPaddingSlider.getValue()));
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(myPaddingSlider, BorderLayout.CENTER);
    wrap.add(myPaddingLabel, BorderLayout.EAST);
    return wrap;
  }

  private JComboBox<CardSelectionMode> buildSelectionBox() {
    mySelectionBox = new JComboBox<>(CardSelectionMode.values());
    mySelectionBox.setSelectedItem(CardSelectionMode.SINGLE);
    mySelectionBox.addActionListener(e -> syncFromControls());
    return mySelectionBox;
  }

  private JPanel buildReorderControls() {
    myReorderableBox = new JCheckBox("Enabled");
    myReorderableBox.setSelected(true);
    myReorderableBox.addActionListener(e -> syncFromControls());

    myReorderHandleBox = new JComboBox<>(ReorderHandle.values());
    myReorderHandleBox.addActionListener(e -> syncFromControls());

    final JPanel wrap = new JPanel();
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));
    wrap.add(myReorderableBox);
    wrap.add(Box.createHorizontalStrut(8));
    wrap.add(myReorderHandleBox);
    return wrap;
  }

  private JTextField buildFilterField() {
    myFilterField = new JTextField();
    myFilterField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent theEvent) {
                syncFromControls();
              }

              @Override
              public void removeUpdate(final DocumentEvent theEvent) {
                syncFromControls();
              }

              @Override
              public void changedUpdate(final DocumentEvent theEvent) {
                syncFromControls();
              }
            });
    return myFilterField;
  }

  private JComboBox<String> buildSortBox() {
    mySortBox = new JComboBox<>(new String[] {"None", "Title", "Date", "Priority"});
    mySortBox.addActionListener(e -> syncFromControls());
    return mySortBox;
  }

  private JCheckBox buildEmptyBox() {
    myEmptyBox = new JCheckBox("Force empty placeholder");
    myEmptyBox.addActionListener(e -> syncFromControls());
    return myEmptyBox;
  }

  private JCheckBox buildLoadingBox() {
    myLoadingBox = new JCheckBox("Show loading state");
    myLoadingBox.addActionListener(e -> syncFromControls());
    return myLoadingBox;
  }

  private JCheckBox buildAnimateBox() {
    myAnimateBox = new JCheckBox("Fade on add/remove");
    myAnimateBox.setSelected(true);
    myAnimateBox.addActionListener(e -> syncFromControls());
    return myAnimateBox;
  }

  private JPanel buildAnimationSlider() {
    myAnimationSlider = new JSlider(50, 800, 180);
    myAnimationLabel = new JLabel("180 ms");
    myAnimationLabel.setPreferredSize(new Dimension(60, 16));
    myAnimationSlider.addChangeListener(
        e -> {
          myAnimationLabel.setText(myAnimationSlider.getValue() + " ms");
          syncFromControls();
        });
    final JPanel wrap = new JPanel(new BorderLayout(8, 0));
    wrap.add(myAnimationSlider, BorderLayout.CENTER);
    wrap.add(myAnimationLabel, BorderLayout.EAST);
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
    final FlatListOrientation orientation =
        (FlatListOrientation) myOrientationBox.getSelectedItem();
    final boolean grid = orientation == FlatListOrientation.GRID;
    myColumnsSlider.setEnabled(grid);
    myColumnsLabel.setEnabled(grid);

    myList
        .setOrientation(orientation)
        .setColumns(myColumnsSlider.getValue())
        .setItemGap(myGapSlider.getValue())
        .setListPadding(
            new Insets(
                myPaddingSlider.getValue(),
                myPaddingSlider.getValue(),
                myPaddingSlider.getValue(),
                myPaddingSlider.getValue()))
        .setSelectionMode((CardSelectionMode) mySelectionBox.getSelectedItem())
        .setReorderable(myReorderableBox.isSelected())
        .setReorderHandle((ReorderHandle) myReorderHandleBox.getSelectedItem())
        .setFilter(buildFilter())
        .setSortOrder(buildComparator())
        .setLoading(myLoadingBox.isSelected())
        .setAnimateChanges(myAnimateBox.isSelected())
        .setAnimationDuration(myAnimationSlider.getValue());

    if (myEmptyBox.isSelected()) {
      myList.setFilter(item -> false);
    }

    updateSnippet();
  }

  private Predicate<DemoItem> buildFilter() {
    final String text = myFilterField.getText();
    if (text == null || text.isEmpty()) {
      return null;
    }
    final String lower = text.toLowerCase();
    return item ->
        item.title().toLowerCase().contains(lower) || item.subtitle().toLowerCase().contains(lower);
  }

  private Comparator<DemoItem> buildComparator() {
    final String which = (String) mySortBox.getSelectedItem();
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
      final com.owspfm.ui.components.card.list.CardSelectionEvent<DemoItem> theEvent) {
    final List<DemoItem> selected = theEvent.getSelected();
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

  private void log(final String theMessage) {
    myStatus.append(theMessage + "\n");
    myStatus.setCaretPosition(myStatus.getDocument().getLength());
  }

  // ----------------------------------------------------------- mutations

  private void addRandomItem() {
    final int n = myModel.getSize() + 1;
    myModel.add(
        new DemoItem(
            "Cycle #" + n,
            "Auto-added at " + LocalDate.now(),
            LocalDate.now(),
            1 + myRandom.nextInt(4)));
  }

  private void removeLast() {
    if (myModel.getSize() == 0) {
      return;
    }
    myModel.remove(myModel.getSize() - 1);
  }

  private void shuffleModel() {
    final List<DemoItem> snapshot = new ArrayList<>();
    for (DemoItem t : myModel) {
      snapshot.add(t);
    }
    Collections.shuffle(snapshot, myRandom);
    while (myModel.getSize() > 0) {
      myModel.remove(myModel.getSize() - 1);
    }
    myModel.addAll(snapshot);
  }

  // ------------------------------------------------------------ snippet

  private void updateSnippet() {
    final StringBuilder sb = new StringBuilder(512);
    sb.append("DefaultCardListModel<DemoItem> model = new DefaultCardListModel<>(items);\n");
    sb.append("CardAdapter<DemoItem> adapter = (item, idx) ->\n");
    sb.append("    new FlatCard()\n");
    sb.append("        .setVariant(CardVariant.OUTLINED)\n");
    sb.append("        .setHeader(item.title(), item.subtitle())\n");
    sb.append("        .setBody(buildBody(item));\n\n");
    sb.append("FlatCardList<DemoItem> list = new FlatCardList<>(model, adapter)\n");
    sb.append("    .setOrientation(Orientation.")
        .append(((FlatListOrientation) myOrientationBox.getSelectedItem()).name())
        .append(")\n");
    if (myOrientationBox.getSelectedItem() == FlatListOrientation.GRID) {
      sb.append("    .setColumns(").append(myColumnsSlider.getValue()).append(")\n");
    }
    sb.append("    .setItemGap(").append(myGapSlider.getValue()).append(")\n");
    sb.append("    .setListPadding(new Insets(")
        .append(myPaddingSlider.getValue())
        .append(", ")
        .append(myPaddingSlider.getValue())
        .append(", ")
        .append(myPaddingSlider.getValue())
        .append(", ")
        .append(myPaddingSlider.getValue())
        .append("))\n");
    sb.append("    .setSelectionMode(CardSelectionMode.")
        .append(((CardSelectionMode) mySelectionBox.getSelectedItem()).name())
        .append(")\n");
    if (myReorderableBox.isSelected()) {
      sb.append("    .setReorderable(true)\n");
      sb.append("    .setReorderHandle(ReorderHandle.")
          .append(((ReorderHandle) myReorderHandleBox.getSelectedItem()).name())
          .append(")\n");
    }
    final String filterText = myFilterField.getText();
    if (filterText != null && !filterText.isEmpty()) {
      sb.append("    .setFilter(item -> item.title().toLowerCase().contains(\"")
          .append(filterText.toLowerCase())
          .append("\"))\n");
    }
    final Object sortKey = mySortBox.getSelectedItem();
    if (sortKey != null && !"None".equals(sortKey)) {
      sb.append("    .setSortOrder(Comparator.comparing(DemoItem::")
          .append(((String) sortKey).toLowerCase())
          .append("))\n");
    }
    if (myLoadingBox.isSelected()) {
      sb.append("    .setLoading(true)\n");
    }
    if (myAnimateBox.isSelected()) {
      sb.append("    .setAnimateChanges(true)\n");
      sb.append("    .setAnimationDuration(").append(myAnimationSlider.getValue()).append(")\n");
    }
    sb.append(";\n");
    sb.append("list.getSelectionModel().addSelectionListener(evt -> ...);\n");
    sb.append("list.addReorderListener(evt -> ...);\n");
    mySnippet.setText(sb.toString());
    mySnippet.setCaretPosition(0);
  }
}
