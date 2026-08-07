package com.owspfm.elwha.list;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Interactive harness for {@link ElwhaItemList} — the operator smoke surface for story #69.
 *
 * <p>The stage on the right hosts one list bound to a mutable model of fruit names. The controls on
 * the left drive every axis the story delivers: the four selection modes, the four movement modes
 * with both reorder affordances, all four orientations under either component orientation, the
 * empty and loading states, and the change-fade animation. A live readout under the stage reports
 * selection, visible order, and the most recent reorder event, so a gesture's effect is visible
 * without reading the console.
 *
 * <p>The segmented pickers in the control column are themselves {@code ElwhaItemList}s in {@link
 * SelectionMode#SINGLE_MANDATORY} — the component demonstrating itself, and a second exercise of
 * the tab-strip semantics on every run.
 *
 * <pre>{@code
 * mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.list.ElwhaItemListDemo"
 * }</pre>
 *
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaItemListDemo {

  private static final List<String> FRUIT =
      List.of(
          "Apricot",
          "Blackberry",
          "Cantaloupe",
          "Damson",
          "Elderberry",
          "Fig",
          "Guava",
          "Honeydew");

  private final DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>(FRUIT);
  private final Set<String> pinned = new LinkedHashSet<>();
  private final ElwhaItemList<String> stage;
  private final JLabel selectionReadout = readout();
  private final JLabel orderReadout = readout();
  private final JLabel reorderReadout = readout();

  private String anchored;

  private ElwhaItemListDemo() {
    stage =
        new ElwhaItemList<>(model, (item, index) -> chipFor(item))
            .setOrientation(ElwhaListOrientation.VERTICAL)
            .setSelectionMode(SelectionMode.MULTIPLE)
            .setMovementMode(MovementMode.MOVABLE)
            .setItemGap(SpaceScale.SM.px())
            .setListPadding(new Insets(12, 12, 12, 12));
    stage.addSelectionListener(event -> refreshReadouts());
    stage.addReorderListener(
        event ->
            reorderReadout.setText(
                "last reorder: "
                    + event.getItem()
                    + "  "
                    + event.getFromIndex()
                    + " → "
                    + event.getToIndex()));
    model.addListDataListener(event -> refreshReadouts());
    refreshReadouts();
  }

  private ElwhaChip chipFor(final String item) {
    return new ElwhaChip(item).setVariant(ChipVariant.OUTLINED);
  }

  // ------------------------------------------------------------------ stage

  private JPanel buildStage() {
    final JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final JScrollPane scroller = new JScrollPane(stage);
    scroller.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
    scroller.getVerticalScrollBar().setUnitIncrement(16);
    panel.add(scroller, BorderLayout.CENTER);

    final JPanel readouts = new JPanel();
    readouts.setLayout(new BoxLayout(readouts, BoxLayout.Y_AXIS));
    readouts.add(selectionReadout);
    readouts.add(orderReadout);
    readouts.add(reorderReadout);
    panel.add(readouts, BorderLayout.SOUTH);
    return panel;
  }

  private void refreshReadouts() {
    selectionReadout.setText("selected: " + stage.getSelectionModel().getSelected());
    orderReadout.setText("visible: " + stage.getVisibleItems());
  }

  // --------------------------------------------------------------- controls

  private JPanel buildControls() {
    final JPanel controls = new JPanel();
    controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
    controls.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 8));

    controls.add(heading("Selection mode"));
    controls.add(
        picker(
            List.of(
                SelectionMode.NONE,
                SelectionMode.SINGLE,
                SelectionMode.SINGLE_MANDATORY,
                SelectionMode.MULTIPLE),
            SelectionMode.MULTIPLE,
            mode -> {
              stage.setSelectionMode(mode);
              refreshReadouts();
            },
            mode -> mode == SelectionMode.SINGLE_MANDATORY ? "MANDATORY" : mode.name()));

    controls.add(heading("Movement mode"));
    controls.add(
        picker(
            List.of(
                MovementMode.STATIC,
                MovementMode.MOVABLE,
                MovementMode.PINNED,
                MovementMode.ANCHORED),
            MovementMode.MOVABLE,
            this::applyMovementMode,
            MovementMode::name));

    controls.add(heading("Reorder affordance"));
    controls.add(
        picker(
            List.of(
                ReorderAffordance.CURSOR_SWAP,
                ReorderAffordance.HOVER_ICON,
                ReorderAffordance.BOTH,
                ReorderAffordance.NONE),
            ReorderAffordance.CURSOR_SWAP,
            stage::setReorderAffordance,
            affordance -> affordance.name().replace('_', ' ')));

    controls.add(heading("Pin / anchor glyph"));
    controls.add(
        picker(
            List.of(IconAffordance.NONE, IconAffordance.INDICATOR, IconAffordance.BUTTON),
            IconAffordance.INDICATOR,
            affordance -> {
              stage.setPinAffordance(affordance);
              stage.setAnchorAffordance(affordance);
            },
            IconAffordance::name));

    controls.add(heading("Orientation"));
    controls.add(
        picker(
            List.of(
                ElwhaListOrientation.VERTICAL,
                ElwhaListOrientation.HORIZONTAL,
                ElwhaListOrientation.WRAP,
                ElwhaListOrientation.GRID),
            ElwhaListOrientation.VERTICAL,
            stage::setOrientation,
            ElwhaListOrientation::name));

    controls.add(heading("Toggles"));
    controls.add(toggles());

    controls.add(heading("Model"));
    controls.add(modelButtons());

    controls.add(Box.createVerticalGlue());
    return controls;
  }

  private void applyMovementMode(final MovementMode mode) {
    // The predicates have to be installed and cleared alongside the mode: a live predicate is what
    // makes a partition real, and leaving a stale one behind would partition a MOVABLE list.
    switch (mode) {
      case PINNED -> {
        stage.setAnchorPredicate(null);
        stage.setPinPredicate(pinned::contains);
        stage.setPinAction(
            (item, shouldPin) -> {
              if (shouldPin) {
                pinned.add(item);
              } else {
                pinned.remove(item);
              }
              stage.pinStateChanged();
              refreshReadouts();
            });
      }
      case ANCHORED -> {
        stage.setPinPredicate(null);
        stage.setAnchorPredicate(item -> item.equals(anchored));
        stage.setAnchorAction(
            item -> {
              anchored = item;
              stage.anchorStateChanged();
              refreshReadouts();
            });
      }
      default -> {
        stage.setPinPredicate(null);
        stage.setAnchorPredicate(null);
        stage.setMovementMode(mode);
      }
    }
    refreshReadouts();
  }

  private JPanel toggles() {
    final JPanel panel = new JPanel(new GridLayout(0, 1, 0, 2));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    final ElwhaCheckbox rightToLeft = new ElwhaCheckbox("Right-to-left");
    rightToLeft.addActionListener(
        event -> {
          stage.applyComponentOrientation(
              rightToLeft.isChecked()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          stage.revalidate();
          stage.repaint();
        });
    panel.add(rightToLeft);

    final ElwhaCheckbox sorted = new ElwhaCheckbox("Sort A–Z (suppresses reorder)");
    sorted.addActionListener(
        event -> {
          stage.setSortOrder(sorted.isChecked() ? Comparator.naturalOrder() : null);
          refreshReadouts();
        });
    panel.add(sorted);

    final ElwhaCheckbox filtered = new ElwhaCheckbox("Filter to names past E");
    filtered.addActionListener(
        event -> {
          stage.setFilter(filtered.isChecked() ? item -> item.compareTo("E") > 0 : null);
          refreshReadouts();
        });
    panel.add(filtered);

    final ElwhaCheckbox loading = new ElwhaCheckbox("Loading state");
    loading.addActionListener(event -> stage.setLoading(loading.isChecked()));
    panel.add(loading);

    final ElwhaCheckbox animate = new ElwhaCheckbox("Animate model changes");
    animate.addActionListener(event -> stage.setAnimateChanges(animate.isChecked()));
    panel.add(animate);

    final ElwhaCheckbox reducedMotion = new ElwhaCheckbox("Reduced motion");
    reducedMotion.setChecked(MorphAnimator.isReducedMotion());
    reducedMotion.addActionListener(
        event -> MorphAnimator.setReducedMotion(reducedMotion.isChecked()));
    panel.add(reducedMotion);

    final ElwhaCheckbox dark = new ElwhaCheckbox("Dark mode");
    dark.addActionListener(
        event ->
            ElwhaTheme.install(
                ElwhaTheme.config()
                    .theme(MaterialPalettes.baseline())
                    .mode(dark.isChecked() ? Mode.DARK : Mode.LIGHT)
                    .build()));
    panel.add(dark);
    return panel;
  }

  private JPanel modelButtons() {
    final JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    final ElwhaButton add = ElwhaButton.filledTonalButton("Add");
    add.addActionListener(event -> model.add("Item " + (model.getSize() + 1)));
    panel.add(add);

    final ElwhaButton removeFirst = ElwhaButton.outlinedButton("Remove first");
    removeFirst.addActionListener(
        event -> {
          if (model.getSize() > 0) {
            model.remove(0);
          }
        });
    panel.add(removeFirst);

    final ElwhaButton shuffle = ElwhaButton.outlinedButton("Move last to front");
    shuffle.addActionListener(
        event -> {
          if (model.getSize() > 1) {
            model.move(model.getSize() - 1, 0);
          }
        });
    panel.add(shuffle);

    final ElwhaButton reset = ElwhaButton.textButton("Reset");
    reset.addActionListener(
        event -> {
          pinned.clear();
          anchored = null;
          model.setItems(new ArrayList<>(FRUIT));
        });
    panel.add(reset);
    return panel;
  }

  /**
   * Builds a segmented picker as a horizontal mandatory-selection {@code ElwhaItemList} — the
   * component under test standing in for a control the library does not otherwise have.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  private <E> ElwhaItemList<E> picker(
      final List<E> options,
      final E initial,
      final Consumer<E> onPick,
      final java.util.function.Function<E, String> label) {
    final ElwhaItemList<E> list =
        new ElwhaItemList<>(
                new DefaultElwhaListModel<>(options),
                (option, index) ->
                    new ElwhaChip(label.apply(option)).setVariant(ChipVariant.OUTLINED))
            .setOrientation(ElwhaListOrientation.WRAP)
            .setItemGap(4)
            .setListPadding(new Insets(0, 0, 8, 0))
            .setSelectionMode(SelectionMode.SINGLE_MANDATORY);
    list.getSelectionModel().setSelected(List.of(initial));
    list.addSelectionListener(
        event -> {
          if (!event.getSelected().isEmpty()) {
            onPick.accept(event.getSelected().get(0));
          }
        });
    list.setAlignmentX(Component.LEFT_ALIGNMENT);
    list.setMaximumSize(new Dimension(Integer.MAX_VALUE, list.getPreferredSize().height + 40));
    return list;
  }

  private static JLabel heading(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(TypeRole.TITLE_SMALL.resolve());
    label.setForeground(ColorRole.PRIMARY.resolve());
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
    return label;
  }

  private static JLabel readout() {
    final JLabel label = new JLabel(" ");
    label.setFont(TypeRole.BODY_SMALL.resolve());
    label.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    return label;
  }

  private void show() {
    final JFrame frame = new JFrame("ElwhaItemList — story #69");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    final JScrollPane controls = new JScrollPane(buildControls());
    controls.setBorder(null);
    controls.getVerticalScrollBar().setUnitIncrement(16);
    controls.setPreferredSize(new Dimension(340, 900));

    final JPanel root = new JPanel(new BorderLayout());
    root.add(controls, BorderLayout.WEST);
    root.add(buildStage(), BorderLayout.CENTER);

    frame.setContentPane(root);
    frame.setSize(1100, 900);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /**
   * Launches the demo.
   *
   * @param args ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
          new ElwhaItemListDemo().show();
        });
  }
}
