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
 * @version v0.1.0
 * @since v0.1.0
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
