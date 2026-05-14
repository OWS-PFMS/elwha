package com.owspfm.ui.components.chip;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.ui.components.chip.list.ChipSelectionMode;
import com.owspfm.ui.components.chip.list.DefaultChipListModel;
import com.owspfm.ui.components.chip.list.FlatChipList;
import com.owspfm.ui.components.flatlist.FlatListOrientation;
import com.owspfm.ui.components.icons.MaterialIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Interactive playground for {@link FlatChip} and {@link FlatChipList}.
 *
 * <p>Three panes, top-to-bottom:
 *
 * <ol>
 *   <li><strong>Variant gallery</strong>: 4-row × 6-column matrix showing every {@link ChipVariant}
 *       in every visual state (default, hover, pressed, focused, disabled, selected).
 *   <li><strong>Live list</strong>: a {@link FlatChipList} the user can switch between all four
 *       orientations, with drag-to-reorder, right-click context menus, trailing-button actions, and
 *       selection enabled. Adjust orientation, columns, gap, and reorder via the controls above the
 *       list.
 *   <li><strong>LAF tweak panel</strong>: sliders, spinners, and color pickers for every public
 *       {@code FlatChip.*} {@link UIManager} key. Changes apply immediately to the live list above.
 *       A "Reset" button restores theme defaults; a Light/Dark toggle switches the FlatLaf
 *       baseline.
 * </ol>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.ui.components.chip.FlatChipPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class FlatChipPlayground {

  private static final String[] SAMPLE_ITEMS = {
    "Demand",
    "Supply",
    "Capacity",
    "Quality",
    "Latency",
    "Cost",
    "Revenue",
    "Risk",
    "Adoption",
    "Churn",
    "Inventory",
    "Throughput"
  };

  private final JFrame frame = new JFrame("FlatChip playground");
  private final DefaultChipListModel<String> model = new DefaultChipListModel<>();
  private final java.util.Set<String> pinned = new java.util.HashSet<>();
  private String anchor;
  private FlatChipList<String> list;
  private JScrollPane listScroll;

  private FlatChipPlayground() {
    for (String s : SAMPLE_ITEMS) {
      model.add(s);
    }
  }

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          FlatLightLaf.setup();
          new FlatChipPlayground().launch();
        });
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Variant gallery", buildVariantGallery());
    tabs.addTab("Live list", buildLiveListPane());

    final JSplitPane split =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, buildLafPanelWrapper());
    split.setResizeWeight(0.65);
    split.setBorder(BorderFactory.createEmptyBorder());
    frame.add(split, BorderLayout.CENTER);
    frame.add(buildThemeToggleBar(), BorderLayout.NORTH);

    frame.setSize(1100, 820);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // ----------------------------------------------------------- variant gallery

  private JPanel buildVariantGallery() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.anchor = GridBagConstraints.WEST;

    final String[] cols = {"default", "hover", "pressed", "focused", "disabled", "selected"};
    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Variant"), gbc);
    for (int c = 0; c < cols.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(cols[c]), gbc);
    }

    final ChipVariant[] variants = ChipVariant.values();
    for (int r = 0; r < variants.length; r++) {
      final ChipVariant v = variants[r];
      gbc.gridy = r + 1;
      gbc.gridx = 0;
      matrix.add(headerLabel(v.name()), gbc);
      for (int c = 0; c < cols.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildSampleChip(v, cols[c]), gbc);
      }
    }

    // Final row: one chip per icon, each demonstrating a different MaterialIcons glyph in the
    // trailing-button slot. Lets the user eyeball icon weight, hinting, hover circle, and
    // alignment against the surrounding capsule for whichever icon they want to use as a
    // remove / pin / edit affordance.
    gbc.gridy = variants.length + 1;
    gbc.gridx = 0;
    matrix.add(headerLabel("trailing icons"), gbc);
    final String[] iconNames = {"delete", "edit", "info", "favorite", "star", "add"};
    final javax.swing.Icon[] icons = {
      MaterialIcons.delete(),
      MaterialIcons.edit(),
      MaterialIcons.info(),
      MaterialIcons.favorite(),
      MaterialIcons.star(),
      MaterialIcons.add(),
    };
    for (int c = 0; c < icons.length; c++) {
      gbc.gridx = c + 1;
      // Pick a different variant per cell so the user sees how each icon reads on each surface.
      final ChipVariant v = variants[c % variants.length];
      final String iconName = iconNames[c];
      final FlatChip chip = new FlatChip(iconName).setVariant(v);
      chip.setInteractionMode(ChipInteractionMode.HOVERABLE);
      chip.setTrailingIcon(icons[c], iconName, () -> System.out.println("clicked: " + iconName));
      matrix.add(chip, gbc);
    }

    final JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(matrix, BorderLayout.NORTH);
    return wrap;
  }

  private JLabel headerLabel(final String text) {
    final JLabel l = new JLabel(text);
    l.putClientProperty("FlatLaf.styleClass", "small");
    l.setForeground(UIManager.getColor("Label.disabledForeground"));
    return l;
  }

  private Component buildSampleChip(final ChipVariant variant, final String state) {
    final FlatChip chip = new FlatChip(variant.name().toLowerCase().replace('_', ' '));
    chip.setVariant(variant);

    final boolean disabled = "disabled".equals(state);
    final boolean selected = "selected".equals(state);
    chip.setInteractionMode(disabled ? ChipInteractionMode.STATIC : ChipInteractionMode.SELECTABLE);
    chip.setEnabled(!disabled);
    chip.setSelected(selected);

    switch (state) {
      case "hover" -> chip.setSurfaceColor(approxStateColor(variant, 0.18f));
      case "pressed" -> chip.setSurfaceColor(approxStateColor(variant, 0.28f));
      case "focused" -> chip.setBorderWidth(2);
      default -> {
        // default / disabled / selected — leave as-is
      }
    }
    return chip;
  }

  private Color approxStateColor(final ChipVariant variant, final float amount) {
    Color base = UIManager.getColor("Panel.background");
    if (base == null) {
      base = new Color(245, 245, 245);
    }
    Color tint = UIManager.getColor("Label.foreground");
    if (tint == null) {
      tint = Color.DARK_GRAY;
    }
    if (variant == ChipVariant.WARM_ACCENT) {
      base = new Color(248, 226, 165);
    }
    final int r = (int) (base.getRed() * (1 - amount) + tint.getRed() * amount);
    final int g = (int) (base.getGreen() * (1 - amount) + tint.getGreen() * amount);
    final int b = (int) (base.getBlue() * (1 - amount) + tint.getBlue() * amount);
    return new Color(r, g, b);
  }

  // ---------------------------------------------------------------- live list

  private JPanel buildLiveListPane() {
    list =
        new FlatChipList<>(model, (item, idx) -> buildLiveChip(item))
            .setSelectionMode(ChipSelectionMode.MULTIPLE)
            .setItemGap(6)
            .setListPadding(new Insets(8, 8, 8, 8));
    // Default to PINNED mode so the pin demo is the visible starting state — flip via the
    // Mode selector to Static, Movable, or Anchored. Default both affordances to BUTTON so
    // the clickable-icon flow is what visitors see first; flip to INDICATOR or NONE via the
    // Pin/Anchor combos in the control bar.
    list.setMovementMode(FlatChipList.MovementMode.PINNED);
    list.setPinAffordance(FlatChipList.IconAffordance.BUTTON);
    list.setAnchorAffordance(FlatChipList.IconAffordance.BUTTON);
    armBindingsForMode(FlatChipList.MovementMode.PINNED);

    listScroll = new JScrollPane(list);
    listScroll.setBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));

    final JPanel controls = buildLiveListControls();

    final JPanel wrap = new JPanel(new BorderLayout(0, 8));
    wrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    wrap.add(controls, BorderLayout.NORTH);
    wrap.add(listScroll, BorderLayout.CENTER);
    wrap.add(buildLiveListFooter(), BorderLayout.SOUTH);
    return wrap;
  }

  /**
   * Re-installs whichever set of caller-side bindings matches the new movement mode. Required
   * because {@link FlatChipList#setMovementMode} clears the inactive side's predicate/action under
   * the PINNED↔ANCHORED mutex — so switching mode in the combo wipes the prior side's bindings, and
   * the next flip back needs them re-armed.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private void armBindingsForMode(final FlatChipList.MovementMode mode) {
    if (mode == FlatChipList.MovementMode.PINNED) {
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
    } else if (mode == FlatChipList.MovementMode.ANCHORED) {
      list.setAnchorPredicate(item -> item != null && item.equals(anchor));
      list.setAnchorAction(
          item -> {
            anchor = item;
            list.anchorStateChanged();
          });
    }
  }

  private FlatChip buildLiveChip(final String item) {
    final FlatChip chip =
        new FlatChip(item)
            .setVariant(ChipVariant.FILLED)
            .setInteractionMode(ChipInteractionMode.CLICKABLE);
    // Trailing trashcan that removes the chip from the model. Uses Material Symbols' "delete"
    // glyph via FlatSVGIcon — auto-themed against the active LAF. App-level override via
    // UIManager.put("FlatChip.removeIcon", icon) is still honored.
    final javax.swing.Icon trashIcon =
        UIManager.get("FlatChip.removeIcon") instanceof javax.swing.Icon override
            ? override
            : MaterialIcons.delete();
    chip.setTrailingIcon(trashIcon, "Remove " + item, () -> model.remove(item));
    // Right-click → mode-appropriate toggle (pin/unpin or set/remove anchor) / details / rename
    // / remove. The mode-specific item is composed at popup time so flipping modes via the
    // selector above reflects in the next right-click without re-attaching listeners.
    chip.attachContextMenu(
        () -> {
          final JPopupMenu p = new JPopupMenu();
          final FlatChipList.MovementMode m = list.getMovementMode();
          if (m == FlatChipList.MovementMode.PINNED) {
            p.add(list.createPinMenuItem(item));
            p.addSeparator();
          } else if (m == FlatChipList.MovementMode.ANCHORED) {
            p.add(list.createAnchorMenuItem(item));
            p.addSeparator();
          }
          final JMenuItem details = new JMenuItem("Show details");
          details.addActionListener(
              e ->
                  JOptionPane.showMessageDialog(
                      frame, "Details for: " + item, "Chip", JOptionPane.INFORMATION_MESSAGE));
          p.add(details);
          final JMenuItem rename = new JMenuItem("Rename…");
          rename.addActionListener(
              e -> {
                final String newName = JOptionPane.showInputDialog(frame, "Rename chip:", item);
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

  private JPanel buildLiveListControls() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    final JLabel orientLbl = new JLabel("Orientation:");
    p.add(orientLbl);
    final JComboBox<FlatListOrientation> orient = new JComboBox<>(FlatListOrientation.values());
    orient.setSelectedItem(FlatListOrientation.VERTICAL);
    orient.addActionListener(
        e -> list.setOrientation((FlatListOrientation) orient.getSelectedItem()));
    p.add(orient);

    p.add(new JLabel("Mode:"));
    final JComboBox<FlatChipList.MovementMode> mode =
        new JComboBox<>(FlatChipList.MovementMode.values());
    mode.setSelectedItem(list.getMovementMode());
    mode.addActionListener(
        e -> {
          final FlatChipList.MovementMode next = (FlatChipList.MovementMode) mode.getSelectedItem();
          list.setMovementMode(next);
          // PINNED↔ANCHORED mutex wiped the inactive side's predicate/action — re-arm whichever
          // side the new mode needs.
          armBindingsForMode(next);
        });
    p.add(mode);

    p.add(new JLabel("Columns (grid):"));
    final JSpinner cols = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
    cols.addChangeListener(e -> list.setColumns((Integer) cols.getValue()));
    p.add(cols);

    p.add(new JLabel("Gap:"));
    final JSlider gap = new JSlider(0, 30, 6);
    gap.setPreferredSize(new Dimension(120, gap.getPreferredSize().height));
    gap.addChangeListener(e -> list.setItemGap(gap.getValue()));
    p.add(gap);

    p.add(new JLabel("Pin:"));
    final JComboBox<FlatChipList.IconAffordance> pinAff =
        new JComboBox<>(FlatChipList.IconAffordance.values());
    pinAff.setSelectedItem(list.getPinAffordance());
    pinAff.addActionListener(
        e -> list.setPinAffordance((FlatChipList.IconAffordance) pinAff.getSelectedItem()));
    p.add(pinAff);

    p.add(new JLabel("Anchor:"));
    final JComboBox<FlatChipList.IconAffordance> anchorAff =
        new JComboBox<>(FlatChipList.IconAffordance.values());
    anchorAff.setSelectedItem(list.getAnchorAffordance());
    anchorAff.addActionListener(
        e -> list.setAnchorAffordance((FlatChipList.IconAffordance) anchorAff.getSelectedItem()));
    p.add(anchorAff);

    p.add(new JLabel("Select:"));
    final JComboBox<ChipSelectionMode> sel = new JComboBox<>(ChipSelectionMode.values());
    sel.setSelectedItem(list.getSelectionMode());
    sel.addActionListener(e -> list.setSelectionMode((ChipSelectionMode) sel.getSelectedItem()));
    p.add(sel);

    return p;
  }

  private JPanel buildLiveListFooter() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    final JButton addBtn =
        new JButton(
            new AbstractAction("Add chip") {
              private int counter;

              @Override
              public void actionPerformed(final ActionEvent e) {
                model.add("New " + (++counter));
              }
            });
    p.add(addBtn);

    final JButton clearBtn =
        new JButton(
            new AbstractAction("Clear all") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                model.clear();
              }
            });
    p.add(clearBtn);

    final JButton repopulate =
        new JButton(
            new AbstractAction("Reset items") {
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

  // ----------------------------------------------------------------- LAF panel

  private JPanel buildLafPanelWrapper() {
    final JPanel wrap = new JPanel(new BorderLayout());
    wrap.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));

    final JLabel title = new JLabel("LAF tweak panel (live)");
    title.putClientProperty("FlatLaf.styleClass", "h4");
    wrap.add(title, BorderLayout.NORTH);

    final JPanel grid = new JPanel(new GridLayout(0, 4, 12, 8));
    addLafRow(grid, "Arc (corner radius)", buildArcSlider());
    addLafRow(grid, "Padding (horizontal)", buildPaddingSlider(true));
    addLafRow(grid, "Padding (vertical)", buildPaddingSlider(false));
    addLafRow(grid, "Background", buildColorPicker(FlatChip.K_BACKGROUND));
    addLafRow(grid, "Foreground", buildColorPicker(FlatChip.K_FOREGROUND));
    addLafRow(grid, "Border color", buildColorPicker(FlatChip.K_BORDER_COLOR));
    addLafRow(grid, "Hover background", buildColorPicker(FlatChip.K_HOVER_BACKGROUND));
    addLafRow(grid, "Pressed background", buildColorPicker(FlatChip.K_PRESSED_BACKGROUND));
    addLafRow(grid, "Selected background", buildColorPicker(FlatChip.K_SELECTED_BACKGROUND));
    addLafRow(grid, "Selected border", buildColorPicker(FlatChip.K_SELECTED_BORDER_COLOR));
    addLafRow(grid, "Focus color", buildColorPicker(FlatChip.K_FOCUS_COLOR));
    addLafRow(grid, "Disabled background", buildColorPicker(FlatChip.K_DISABLED_BACKGROUND));
    addLafRow(grid, "Warm accent", buildColorPicker(FlatChip.K_WARM_ACCENT));

    final JButton reset =
        new JButton(
            new AbstractAction("Reset all FlatChip.* keys") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                resetAllKeys();
              }
            });
    final JPanel bottom = new JPanel(new BorderLayout(8, 0));
    bottom.add(reset, BorderLayout.WEST);
    final JLabel hint =
        new JLabel(
            "<html><i>Changes apply immediately. Reset to clear all overrides and fall back to "
                + "theme defaults.</i></html>");
    hint.setForeground(UIManager.getColor("Label.disabledForeground"));
    bottom.add(hint, BorderLayout.CENTER);

    wrap.add(grid, BorderLayout.CENTER);
    wrap.add(bottom, BorderLayout.SOUTH);
    return wrap;
  }

  private void addLafRow(final JPanel grid, final String label, final JComponent control) {
    final JLabel l = new JLabel(label);
    l.putClientProperty("FlatLaf.styleClass", "small");
    grid.add(l);
    grid.add(control);
  }

  private JSlider buildArcSlider() {
    // Range 0-60 covers everything from sharp-cornered (0) past typical capsule depth (≈30) up to
    // the visible cap (clampArc() in FlatChip bounds it by min(width, height) at paint time).
    final JSlider s = new JSlider(0, 60, 20);
    s.setMajorTickSpacing(20);
    s.setPaintTicks(true);
    s.addChangeListener(
        e -> {
          UIManager.put(FlatChip.K_ARC, s.getValue());
          repaintLive();
        });
    return s;
  }

  private JSlider buildPaddingSlider(final boolean horizontal) {
    final JSlider s = new JSlider(0, 30, horizontal ? 10 : 4);
    s.addChangeListener(
        e -> {
          final int v = s.getValue();
          final Insets cur =
              UIManager.get(FlatChip.K_PADDING) instanceof Insets in
                  ? in
                  : new Insets(4, 10, 4, 10);
          final Insets next =
              horizontal
                  ? new Insets(cur.top, v, cur.bottom, v)
                  : new Insets(v, cur.left, v, cur.right);
          UIManager.put(FlatChip.K_PADDING, next);
          // K_PADDING is baked into each chip's Swing Border at construction time, so a
          // plain repaint won't pick up runtime changes — push the new value through the
          // updateUI cascade, which causes FlatChip.updateUI() to call rebuildBorder() and
          // read the fresh UIManager value. Standard Swing convention for live UIManager
          // tweaks; the same dance is what FlatLaf does for global theme switches.
          SwingUtilities.updateComponentTreeUI(list);
        });
    return s;
  }

  private JButton buildColorPicker(final String key) {
    final JButton btn = new JButton();
    btn.setPreferredSize(new Dimension(80, 22));
    refreshSwatch(btn, key);
    btn.addActionListener(
        e -> {
          final Color current = UIManager.getColor(key);
          final Color chosen =
              JColorChooser.showDialog(frame, key, current == null ? Color.WHITE : current);
          if (chosen != null) {
            UIManager.put(key, chosen);
            refreshSwatch(btn, key);
            repaintLive();
          }
        });
    return btn;
  }

  private void refreshSwatch(final JButton button, final String key) {
    final Color c = UIManager.getColor(key);
    if (c != null) {
      button.setBackground(c);
      button.setText(toHex(c));
      button.setForeground(isLight(c) ? Color.BLACK : Color.WHITE);
    } else {
      button.setBackground(null);
      button.setText("(theme default)");
      button.setForeground(UIManager.getColor("Label.disabledForeground"));
    }
  }

  private static String toHex(final Color c) {
    return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
  }

  private static boolean isLight(final Color c) {
    return (c.getRed() + c.getGreen() + c.getBlue()) / 3 > 128;
  }

  private void resetAllKeys() {
    final String[] keys = {
      FlatChip.K_BACKGROUND,
      FlatChip.K_FOREGROUND,
      FlatChip.K_BORDER_COLOR,
      FlatChip.K_ARC,
      FlatChip.K_PADDING,
      FlatChip.K_HOVER_BACKGROUND,
      FlatChip.K_PRESSED_BACKGROUND,
      FlatChip.K_SELECTED_BACKGROUND,
      FlatChip.K_SELECTED_BORDER_COLOR,
      FlatChip.K_FOCUS_COLOR,
      FlatChip.K_DISABLED_BACKGROUND,
      FlatChip.K_WARM_ACCENT
    };
    for (String k : keys) {
      UIManager.put(k, null);
    }
    repaintLive();
    SwingUtilities.updateComponentTreeUI(frame);
  }

  private void repaintLive() {
    if (list != null) {
      list.repaint();
    }
  }

  // ------------------------------------------------------------ theme toggle

  private JPanel buildThemeToggleBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    final JLabel l = new JLabel("Theme:");
    bar.add(l);
    final JRadioButton light = new JRadioButton("Light", true);
    final JRadioButton dark = new JRadioButton("Dark");
    final ButtonGroup grp = new ButtonGroup();
    grp.add(light);
    grp.add(dark);
    light.addActionListener(e -> applyTheme(true));
    dark.addActionListener(e -> applyTheme(false));
    bar.add(light);
    bar.add(dark);
    return bar;
  }

  private void applyTheme(final boolean light) {
    try {
      if (light) {
        FlatLightLaf.setup();
      } else {
        FlatDarkLaf.setup();
      }
      SwingUtilities.updateComponentTreeUI(frame);
    } catch (Exception ignored) {
      // theme switch failures don't merit a popup in a demo
    }
  }
}
