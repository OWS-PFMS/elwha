package com.owspfm.elwha.chip.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.chip.ChipInteractionMode;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.list.DefaultElwhaListModel;
import com.owspfm.elwha.list.ElwhaItemList;
import com.owspfm.elwha.list.ElwhaListOrientation;
import com.owspfm.elwha.list.IconAffordance;
import com.owspfm.elwha.list.MovementMode;
import com.owspfm.elwha.list.SelectionMode;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

/**
 * Reusable panel builders for the {@link ElwhaChip} playground surfaces. Lets the standalone {@code
 * ElwhaChipPlayground} and the {@code ThemePlayground}'s {@code Chip} tab share one canonical
 * implementation of the variant gallery and the live list, so the validation matrix and the live
 * interaction surface stay in lockstep across both entry points.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public final class ChipPlaygroundPanels {

  private ChipPlaygroundPanels() {}

  /**
   * Builds the variant-gallery panel: every {@link ChipVariant} × every {@link ChipInteractionMode}
   * × {idle, hover, pressed, selected, focused, disabled}, plus a row of the four M3 factory
   * presets, plus a trailing-icon sampler row. The "hover" / "pressed" / "focused" columns are
   * static visual approximations — the live interaction surface is the {@link #buildLiveListPanel}
   * pane.
   *
   * @return the gallery panel
   * @version v0.3.0
   * @since v0.1.0
   */
  public static JPanel buildVariantGallery() {
    final JPanel controls = buildTokenControlBar();
    final JPanel matrix = buildVariantGalleryMatrix();
    controls.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    matrix.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    final JPanel wrap = new JPanel();
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
    wrap.add(controls);
    wrap.add(matrix);
    return wrap;
  }

  /**
   * Builds just the variant-gallery matrix — the {@link ChipVariant} × {@link ChipInteractionMode}
   * × state grid, the factory-preset row, and the trailing-icon sampler — without the live token
   * control bar. The Elwha Showcase uses this directly: its Chip Workbench is the live-config
   * surface, so the gallery tab carries the reference matrix alone.
   *
   * @return the gallery matrix panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JPanel buildVariantGalleryMatrix() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.anchor = GridBagConstraints.WEST;

    final String[] cols = {"idle", "hover*", "pressed*", "focused*", "selected", "disabled"};
    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Variant × Mode"), gbc);
    for (int c = 0; c < cols.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(cols[c]), gbc);
    }

    int row = 1;
    final ChipVariant[] variants = ChipVariant.values();
    final ChipInteractionMode[] modes = ChipInteractionMode.values();
    for (ChipVariant v : variants) {
      for (ChipInteractionMode mode : modes) {
        gbc.gridy = row++;
        gbc.gridx = 0;
        matrix.add(headerLabel(v.name() + " / " + mode.name()), gbc);
        for (int c = 0; c < cols.length; c++) {
          gbc.gridx = c + 1;
          matrix.add(buildSampleChip(v, mode, cols[c]), gbc);
        }
      }
    }

    // Factory presets — one row per preset, default rendering only.
    gbc.gridy = row++;
    gbc.gridx = 0;
    matrix.add(headerLabel("Factories"), gbc);
    gbc.gridx = 1;
    matrix.add(ElwhaChip.assistChip("Assist"), gbc);
    gbc.gridx = 2;
    matrix.add(ElwhaChip.filterChip("Filter"), gbc);
    gbc.gridx = 3;
    matrix.add(ElwhaChip.inputChip("Input", () -> {}), gbc);
    gbc.gridx = 4;
    matrix.add(ElwhaChip.suggestionChip("Suggest"), gbc);

    // Trailing-icon sampler — show how different M3 glyphs land in the trailing slot.
    gbc.gridy = row;
    gbc.gridx = 0;
    matrix.add(headerLabel("Trailing icons"), gbc);
    final String[] iconNames = {"delete", "edit", "info", "favorite", "star", "add"};
    final int chipIconSize = 14;
    final javax.swing.Icon[] icons = {
      MaterialIcons.delete(chipIconSize),
      MaterialIcons.edit(chipIconSize),
      MaterialIcons.info(chipIconSize),
      MaterialIcons.favorite(chipIconSize),
      MaterialIcons.star(chipIconSize),
      MaterialIcons.add(chipIconSize),
    };
    for (int c = 0; c < icons.length; c++) {
      gbc.gridx = c + 1;
      final ChipVariant v = variants[c % variants.length];
      final ElwhaChip chip = new ElwhaChip(iconNames[c]).setVariant(v);
      chip.setInteractionMode(ChipInteractionMode.HOVERABLE);
      final String name = iconNames[c];
      chip.setTrailingIcon(icons[c], name, () -> {});
      matrix.add(chip, gbc);
    }
    return matrix;
  }

  /**
   * Builds the live-list pane: a {@link ElwhaItemList} with sample data, plus controls for
   * orientation, movement mode, columns, gap, pin / anchor affordances, and selection mode.
   *
   * @return the live-list panel
   * @version v0.5.0
   * @since v0.1.0
   */
  public static JPanel buildLiveListPanel() {
    return new LiveListPanel().build();
  }

  // ----- token control bar shown beneath the variant gallery -----

  private static JPanel buildTokenControlBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    // A single live chip the user can drive through the token setters.
    final ElwhaChip target =
        new ElwhaChip("Live chip").setInteractionMode(ChipInteractionMode.SELECTABLE);

    // Forward-declared so the variant listener can flip the selected-checkbox enable state when
    // the variant moves to or from GHOST.
    final ElwhaCheckbox selectedBox = new ElwhaCheckbox("Selected");

    final ElwhaSelectField<ChipVariant> variantBox = ElwhaSelectField.outlined("Variant");
    variantBox.setOptions(List.of(ChipVariant.values()));
    variantBox.setSelectedValue(target.getVariant());
    variantBox.addSelectionChangeListener(
        v -> {
          target.setVariant(v);
          final boolean ghost = v == ChipVariant.GHOST;
          selectedBox.setEnabled(!ghost);
          selectedBox.setToolTipText(
              ghost
                  ? "GHOST does not render selection (issue #50, M3 alignment). "
                      + "Selection state is preserved but invisible until the variant changes."
                  : null);
        });
    bar.add(variantBox);

    final ElwhaSelectField<SurfaceRoleChoice> roleBox = ElwhaSelectField.outlined("Surface role");
    roleBox.setOptions(List.of(SurfaceRoleChoice.values()));
    roleBox.setSelectedValue(SurfaceRoleChoice.VARIANT_DEFAULT);
    roleBox.addSelectionChangeListener(choice -> target.setSurfaceRole(choice.role));
    bar.add(roleBox);

    final ElwhaSelectField<ShapeScale> shapeBox = ElwhaSelectField.outlined("Shape");
    shapeBox.setOptions(List.of(ShapeScale.values()));
    shapeBox.setSelectedValue(target.getShape());
    shapeBox.addSelectionChangeListener(target::setShape);
    bar.add(shapeBox);

    final ElwhaSelectField<SpaceScale> horizontalBox = ElwhaSelectField.outlined("Padding h");
    horizontalBox.setOptions(List.of(SpaceScale.values()));
    horizontalBox.setSelectedValue(SpaceScale.MD);
    final ElwhaSelectField<SpaceScale> verticalBox = ElwhaSelectField.outlined("Padding v");
    verticalBox.setOptions(List.of(SpaceScale.values()));
    verticalBox.setSelectedValue(SpaceScale.XS);
    final Consumer<SpaceScale> pad =
        v -> target.setPadding(horizontalBox.getSelectedValue(), verticalBox.getSelectedValue());
    horizontalBox.addSelectionChangeListener(pad);
    verticalBox.addSelectionChangeListener(pad);
    bar.add(horizontalBox);
    bar.add(verticalBox);

    // Two-way binding: clicking the chip in SELECTABLE mode flips PROPERTY_SELECTED → checkbox
    // mirrors the chip state; clicking the checkbox calls setSelected → chip mirrors the checkbox.
    selectedBox.setChecked(target.isSelected());
    selectedBox.addActionListener(e -> target.setSelected(selectedBox.isChecked()));
    target.addPropertyChangeListener(
        ElwhaChip.PROPERTY_SELECTED,
        evt -> selectedBox.setChecked(Boolean.TRUE.equals(evt.getNewValue())));
    bar.add(selectedBox);

    bar.add(Box.createHorizontalStrut(16));
    bar.add(target);

    return bar;
  }

  /** Wraps a {@link ColorRole} as a select option, with {@code VARIANT_DEFAULT} → null. */
  private enum SurfaceRoleChoice {
    VARIANT_DEFAULT(null),
    PRIMARY(ColorRole.PRIMARY),
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),
    SECONDARY(ColorRole.SECONDARY),
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),
    TERTIARY(ColorRole.TERTIARY),
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),
    SURFACE(ColorRole.SURFACE),
    SURFACE_VARIANT(ColorRole.SURFACE_VARIANT),
    ERROR_CONTAINER(ColorRole.ERROR_CONTAINER);

    final ColorRole role;

    SurfaceRoleChoice(ColorRole role) {
      this.role = role;
    }
  }

  // ----- variant gallery cell builders -----

  private static JComponent buildSampleChip(
      final ChipVariant variant, final ChipInteractionMode mode, final String column) {
    final ElwhaChip chip = new ElwhaChip(variant.name().toLowerCase().replace('_', ' '));
    chip.setVariant(variant);

    final boolean disabled = "disabled".equals(column);
    final boolean selected = "selected".equals(column);
    chip.setInteractionMode(disabled ? ChipInteractionMode.STATIC : mode);
    chip.setEnabled(!disabled);
    chip.setSelected(selected);

    // The starred columns are static visual approximations — the live state-layer overlays only
    // paint under real interaction, so we widen the border on focused to simulate the focus
    // outline at gallery rest.
    switch (column) {
      case "focused*" -> chip.setBorderWidth(2);
      default -> {
        // idle / hover* / pressed* / selected / disabled — leave as-is
      }
    }
    return chip;
  }

  private static JLabel headerLabel(final String text) {
    final JLabel l = new JLabel(text);
    l.putClientProperty("FlatLaf.styleClass", "small");
    l.setForeground(UIManager.getColor("Label.disabledForeground"));
    return l;
  }

  // ----- live list -----

  private static final class LiveListPanel {

    private static final String[] SAMPLE_ITEMS = {
      "Demand", "Supply", "Capacity", "Quality", "Latency", "Cost",
      "Revenue", "Risk", "Adoption", "Churn", "Inventory", "Throughput"
    };

    private final DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>();
    private final java.util.Set<String> pinned = new java.util.HashSet<>();
    private String anchor;
    private ElwhaItemList<String> list;

    JPanel build() {
      for (String s : SAMPLE_ITEMS) {
        model.add(s);
      }

      list =
          new ElwhaItemList<>(model, (item, idx) -> buildLiveChip(item))
              .setSelectionMode(SelectionMode.MULTIPLE)
              .setItemGap(6)
              .setListPadding(new Insets(8, 8, 8, 8));
      list.setMovementMode(MovementMode.PINNED);
      list.setPinAffordance(IconAffordance.BUTTON);
      list.setAnchorAffordance(IconAffordance.BUTTON);
      armBindingsForMode(MovementMode.PINNED);

      final JScrollPane scroll = new JScrollPane(list);
      scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));

      final JPanel wrap = new JPanel(new BorderLayout(0, 8));
      wrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
      wrap.add(buildControls(), BorderLayout.NORTH);
      wrap.add(scroll, BorderLayout.CENTER);
      wrap.add(buildFooter(), BorderLayout.SOUTH);
      return wrap;
    }

    private void armBindingsForMode(final MovementMode mode) {
      if (mode == MovementMode.PINNED) {
        list.setPinPredicate(pinned::contains);
        list.setPinAction(
            (item, pinNow) -> {
              if (pinNow) {
                pinned.add(item);
              } else {
                pinned.remove(item);
              }
              list.pinStateChanged();
            });
      } else if (mode == MovementMode.ANCHORED) {
        list.setAnchorPredicate(item -> item != null && item.equals(anchor));
        list.setAnchorAction(
            item -> {
              anchor = item;
              list.anchorStateChanged();
            });
      }
    }

    private ElwhaChip buildLiveChip(final String item) {
      final ElwhaChip chip =
          new ElwhaChip(item)
              .setVariant(ChipVariant.FILLED)
              .setInteractionMode(ChipInteractionMode.CLICKABLE);
      chip.setTrailingIcon(MaterialIcons.delete(14), "Remove " + item, () -> model.remove(item));
      chip.attachContextMenu(
          () -> {
            final JPopupMenu p = new JPopupMenu();
            final MovementMode m = list.getMovementMode();
            if (m == MovementMode.PINNED) {
              p.add(list.createPinMenuItem(item));
              p.addSeparator();
            } else if (m == MovementMode.ANCHORED) {
              p.add(list.createAnchorMenuItem(item));
              p.addSeparator();
            }
            final JMenuItem details = new JMenuItem("Show details");
            details.addActionListener(
                e ->
                    JOptionPane.showMessageDialog(
                        list, "Details for: " + item, "Chip", JOptionPane.INFORMATION_MESSAGE));
            p.add(details);
            final JMenuItem rename = new JMenuItem("Rename…");
            rename.addActionListener(
                e -> {
                  final String newName = JOptionPane.showInputDialog(list, "Rename chip:", item);
                  if (newName != null && !newName.isEmpty()) {
                    final int idx = model.indexOf(item);
                    if (idx >= 0) {
                      model.set(idx, newName);
                    }
                  }
                });
            p.add(rename);
            p.addSeparator();
            final JMenuItem remove = new JMenuItem("Remove");
            remove.addActionListener(e -> model.remove(item));
            p.add(remove);
            return p;
          });
      return chip;
    }

    private JPanel buildControls() {
      final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

      final ElwhaSelectField<ElwhaListOrientation> orient =
          ElwhaSelectField.outlined("Orientation");
      orient.setOptions(List.of(ElwhaListOrientation.values()));
      orient.setSelectedValue(ElwhaListOrientation.VERTICAL);
      orient.addSelectionChangeListener(list::setOrientation);
      p.add(orient);

      final ElwhaSelectField<MovementMode> mode = ElwhaSelectField.outlined("Mode");
      mode.setOptions(List.of(MovementMode.values()));
      mode.setSelectedValue(list.getMovementMode());
      mode.addSelectionChangeListener(
          next -> {
            list.setMovementMode(next);
            armBindingsForMode(next);
          });
      p.add(mode);

      p.add(new JLabel("Columns:"));
      final JSpinner cols = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
      cols.addChangeListener(e -> list.setColumns((Integer) cols.getValue()));
      p.add(cols);

      p.add(new JLabel("Gap:"));
      final JSpinner gap = new JSpinner(new SpinnerNumberModel(6, 0, 30, 1));
      gap.addChangeListener(e -> list.setItemGap((Integer) gap.getValue()));
      p.add(gap);

      final ElwhaSelectField<IconAffordance> pinAff = ElwhaSelectField.outlined("Pin");
      pinAff.setOptions(List.of(IconAffordance.values()));
      pinAff.setSelectedValue(list.getPinAffordance());
      pinAff.addSelectionChangeListener(list::setPinAffordance);
      p.add(pinAff);

      final ElwhaSelectField<IconAffordance> anchorAff = ElwhaSelectField.outlined("Anchor");
      anchorAff.setOptions(List.of(IconAffordance.values()));
      anchorAff.setSelectedValue(list.getAnchorAffordance());
      anchorAff.addSelectionChangeListener(list::setAnchorAffordance);
      p.add(anchorAff);

      final ElwhaSelectField<SelectionMode> sel = ElwhaSelectField.outlined("Select");
      sel.setOptions(List.of(SelectionMode.values()));
      sel.setSelectedValue(list.getSelectionMode());
      sel.addSelectionChangeListener(list::setSelectionMode);
      p.add(sel);

      return p;
    }

    private JPanel buildFooter() {
      final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
      final ElwhaButton addBtn = ElwhaButton.filledTonalButton("Add chip");
      addBtn.addActionListener(
          new AbstractAction() {
            private int counter;

            @Override
            public void actionPerformed(final ActionEvent e) {
              model.add("New " + (++counter));
            }
          });
      p.add(addBtn);

      final ElwhaButton clearBtn = ElwhaButton.filledTonalButton("Clear all");
      clearBtn.addActionListener(
          new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
              model.clear();
            }
          });
      p.add(clearBtn);

      final ElwhaButton repopulate = ElwhaButton.filledTonalButton("Reset items");
      repopulate.addActionListener(
          new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
              model.clear();
              for (String s : SAMPLE_ITEMS) {
                model.add(s);
              }
            }
          });
      p.add(repopulate);
      return p;
    }
  }
}
