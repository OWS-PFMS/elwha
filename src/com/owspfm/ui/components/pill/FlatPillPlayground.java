package com.owspfm.ui.components.pill;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.ui.components.flatlist.FlatListOrientation;
import com.owspfm.ui.components.icons.MaterialIcons;
import com.owspfm.ui.components.pill.list.DefaultPillListModel;
import com.owspfm.ui.components.pill.list.FlatPillList;
import com.owspfm.ui.components.pill.list.PillSelectionMode;
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
 * Interactive playground for {@link FlatPill} and {@link FlatPillList}.
 *
 * <p>Three panes, top-to-bottom:
 *
 * <ol>
 *   <li><strong>Variant gallery</strong>: 4-row × 6-column matrix showing every {@link PillVariant}
 *       in every visual state (default, hover, pressed, focused, disabled, selected).
 *   <li><strong>Live list</strong>: a {@link FlatPillList} the user can switch between all four
 *       orientations, with drag-to-reorder, right-click context menus, trailing-button actions, and
 *       selection enabled. Adjust orientation, columns, gap, and reorder via the controls above the
 *       list.
 *   <li><strong>LAF tweak panel</strong>: sliders, spinners, and color pickers for every public
 *       {@code FlatPill.*} {@link UIManager} key. Changes apply immediately to the live list above.
 *       A "Reset" button restores theme defaults; a Light/Dark toggle switches the FlatLaf
 *       baseline.
 * </ol>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.ui.components.pill.FlatPillPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class FlatPillPlayground {

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

  private final JFrame myFrame = new JFrame("FlatPill playground");
  private final DefaultPillListModel<String> myModel = new DefaultPillListModel<>();
  private final java.util.Set<String> myPinned = new java.util.HashSet<>();
  private String myAnchor;
  private FlatPillList<String> myList;
  private JScrollPane myListScroll;

  private FlatPillPlayground() {
    for (String s : SAMPLE_ITEMS) {
      myModel.add(s);
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
          new FlatPillPlayground().launch();
        });
  }

  private void launch() {
    myFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    myFrame.setLayout(new BorderLayout());

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Variant gallery", buildVariantGallery());
    tabs.addTab("Live list", buildLiveListPane());

    final JSplitPane split =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, buildLafPanelWrapper());
    split.setResizeWeight(0.65);
    split.setBorder(BorderFactory.createEmptyBorder());
    myFrame.add(split, BorderLayout.CENTER);
    myFrame.add(buildThemeToggleBar(), BorderLayout.NORTH);

    myFrame.setSize(1100, 820);
    myFrame.setLocationRelativeTo(null);
    myFrame.setVisible(true);
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

    final PillVariant[] variants = PillVariant.values();
    for (int r = 0; r < variants.length; r++) {
      final PillVariant v = variants[r];
      gbc.gridy = r + 1;
      gbc.gridx = 0;
      matrix.add(headerLabel(v.name()), gbc);
      for (int c = 0; c < cols.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildSamplePill(v, cols[c]), gbc);
      }
    }

    // Final row: one pill per icon, each demonstrating a different MaterialIcons glyph in the
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
      final PillVariant v = variants[c % variants.length];
      final String iconName = iconNames[c];
      final FlatPill pill = new FlatPill(iconName).setVariant(v);
      pill.setInteractionMode(PillInteractionMode.HOVERABLE);
      pill.setTrailingIcon(icons[c], iconName, () -> System.out.println("clicked: " + iconName));
      matrix.add(pill, gbc);
    }

    final JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(matrix, BorderLayout.NORTH);
    return wrap;
  }

  private JLabel headerLabel(final String theText) {
    final JLabel l = new JLabel(theText);
    l.putClientProperty("FlatLaf.styleClass", "small");
    l.setForeground(UIManager.getColor("Label.disabledForeground"));
    return l;
  }

  private Component buildSamplePill(final PillVariant theVariant, final String theState) {
    final FlatPill pill = new FlatPill(theVariant.name().toLowerCase().replace('_', ' '));
    pill.setVariant(theVariant);

    final boolean disabled = "disabled".equals(theState);
    final boolean selected = "selected".equals(theState);
    pill.setInteractionMode(disabled ? PillInteractionMode.STATIC : PillInteractionMode.SELECTABLE);
    pill.setEnabled(!disabled);
    pill.setSelected(selected);

    switch (theState) {
      case "hover" -> pill.setSurfaceColor(approxStateColor(theVariant, 0.18f));
      case "pressed" -> pill.setSurfaceColor(approxStateColor(theVariant, 0.28f));
      case "focused" -> pill.setBorderWidth(2);
      default -> {
        // default / disabled / selected — leave as-is
      }
    }
    return pill;
  }

  private Color approxStateColor(final PillVariant theVariant, final float theAmount) {
    Color base = UIManager.getColor("Panel.background");
    if (base == null) {
      base = new Color(245, 245, 245);
    }
    Color tint = UIManager.getColor("Label.foreground");
    if (tint == null) {
      tint = Color.DARK_GRAY;
    }
    if (theVariant == PillVariant.WARM_ACCENT) {
      base = new Color(248, 226, 165);
    }
    final int r = (int) (base.getRed() * (1 - theAmount) + tint.getRed() * theAmount);
    final int g = (int) (base.getGreen() * (1 - theAmount) + tint.getGreen() * theAmount);
    final int b = (int) (base.getBlue() * (1 - theAmount) + tint.getBlue() * theAmount);
    return new Color(r, g, b);
  }

  // ---------------------------------------------------------------- live list

  private JPanel buildLiveListPane() {
    myList =
        new FlatPillList<>(myModel, (item, idx) -> buildLivePill(item))
            .setSelectionMode(PillSelectionMode.MULTIPLE)
            .setItemGap(6)
            .setListPadding(new Insets(8, 8, 8, 8));
    // Default to PINNED mode so the pin demo is the visible starting state — flip via the
    // Mode selector to Static, Movable, or Anchored. Default both affordances to BUTTON so
    // the clickable-icon flow is what visitors see first; flip to INDICATOR or NONE via the
    // Pin/Anchor combos in the control bar.
    myList.setMovementMode(FlatPillList.MovementMode.PINNED);
    myList.setPinAffordance(FlatPillList.IconAffordance.BUTTON);
    myList.setAnchorAffordance(FlatPillList.IconAffordance.BUTTON);
    armBindingsForMode(FlatPillList.MovementMode.PINNED);

    myListScroll = new JScrollPane(myList);
    myListScroll.setBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));

    final JPanel controls = buildLiveListControls();

    final JPanel wrap = new JPanel(new BorderLayout(0, 8));
    wrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    wrap.add(controls, BorderLayout.NORTH);
    wrap.add(myListScroll, BorderLayout.CENTER);
    wrap.add(buildLiveListFooter(), BorderLayout.SOUTH);
    return wrap;
  }

  /**
   * Re-installs whichever set of caller-side bindings matches the new movement mode. Required
   * because {@link FlatPillList#setMovementMode} clears the inactive side's predicate/action under
   * the PINNED↔ANCHORED mutex — so switching mode in the combo wipes the prior side's bindings, and
   * the next flip back needs them re-armed.
    * @version v0.1.0
    * @since v0.1.0
   */
  private void armBindingsForMode(final FlatPillList.MovementMode theMode) {
    if (theMode == FlatPillList.MovementMode.PINNED) {
      myList.setPinPredicate(myPinned::contains);
      myList.setPinAction(
          (item, pinNow) -> {
            if (pinNow) {
              myPinned.add(item);
            } else {
              myPinned.remove(item);
            }
            myList.pinStateChanged();
          });
    } else if (theMode == FlatPillList.MovementMode.ANCHORED) {
      myList.setAnchorPredicate(item -> item != null && item.equals(myAnchor));
      myList.setAnchorAction(
          item -> {
            myAnchor = item;
            myList.anchorStateChanged();
          });
    }
  }

  private FlatPill buildLivePill(final String theItem) {
    final FlatPill pill =
        new FlatPill(theItem)
            .setVariant(PillVariant.FILLED)
            .setInteractionMode(PillInteractionMode.CLICKABLE);
    // Trailing trashcan that removes the pill from the model. Uses Material Symbols' "delete"
    // glyph via FlatSVGIcon — auto-themed against the active LAF. App-level override via
    // UIManager.put("FlatPill.removeIcon", myIcon) is still honored.
    final javax.swing.Icon trashIcon =
        UIManager.get("FlatPill.removeIcon") instanceof javax.swing.Icon override
            ? override
            : MaterialIcons.delete();
    pill.setTrailingIcon(trashIcon, "Remove " + theItem, () -> myModel.remove(theItem));
    // Right-click → mode-appropriate toggle (pin/unpin or set/remove anchor) / details / rename
    // / remove. The mode-specific item is composed at popup time so flipping modes via the
    // selector above reflects in the next right-click without re-attaching listeners.
    pill.attachContextMenu(
        () -> {
          final JPopupMenu p = new JPopupMenu();
          final FlatPillList.MovementMode m = myList.getMovementMode();
          if (m == FlatPillList.MovementMode.PINNED) {
            p.add(myList.createPinMenuItem(theItem));
            p.addSeparator();
          } else if (m == FlatPillList.MovementMode.ANCHORED) {
            p.add(myList.createAnchorMenuItem(theItem));
            p.addSeparator();
          }
          final JMenuItem details = new JMenuItem("Show details");
          details.addActionListener(
              e ->
                  JOptionPane.showMessageDialog(
                      myFrame, "Details for: " + theItem, "Pill", JOptionPane.INFORMATION_MESSAGE));
          p.add(details);
          final JMenuItem rename = new JMenuItem("Rename…");
          rename.addActionListener(
              e -> {
                final String newName =
                    JOptionPane.showInputDialog(myFrame, "Rename pill:", theItem);
                if (newName != null && !newName.isEmpty()) {
                  final int idx = myModel.indexOf(theItem);
                  if (idx >= 0) {
                    myModel.set(idx, newName);
                  }
                }
              });
          p.add(rename);
          p.addSeparator();
          final JMenuItem remove = new JMenuItem("Remove");
          remove.addActionListener(e -> myModel.remove(theItem));
          p.add(remove);
          return p;
        });
    return pill;
  }

  private JPanel buildLiveListControls() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    final JLabel orientLbl = new JLabel("Orientation:");
    p.add(orientLbl);
    final JComboBox<FlatListOrientation> orient = new JComboBox<>(FlatListOrientation.values());
    orient.setSelectedItem(FlatListOrientation.VERTICAL);
    orient.addActionListener(
        e -> myList.setOrientation((FlatListOrientation) orient.getSelectedItem()));
    p.add(orient);

    p.add(new JLabel("Mode:"));
    final JComboBox<FlatPillList.MovementMode> mode =
        new JComboBox<>(FlatPillList.MovementMode.values());
    mode.setSelectedItem(myList.getMovementMode());
    mode.addActionListener(
        e -> {
          final FlatPillList.MovementMode next = (FlatPillList.MovementMode) mode.getSelectedItem();
          myList.setMovementMode(next);
          // PINNED↔ANCHORED mutex wiped the inactive side's predicate/action — re-arm whichever
          // side the new mode needs.
          armBindingsForMode(next);
        });
    p.add(mode);

    p.add(new JLabel("Columns (grid):"));
    final JSpinner cols = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
    cols.addChangeListener(e -> myList.setColumns((Integer) cols.getValue()));
    p.add(cols);

    p.add(new JLabel("Gap:"));
    final JSlider gap = new JSlider(0, 30, 6);
    gap.setPreferredSize(new Dimension(120, gap.getPreferredSize().height));
    gap.addChangeListener(e -> myList.setItemGap(gap.getValue()));
    p.add(gap);

    p.add(new JLabel("Pin:"));
    final JComboBox<FlatPillList.IconAffordance> pinAff =
        new JComboBox<>(FlatPillList.IconAffordance.values());
    pinAff.setSelectedItem(myList.getPinAffordance());
    pinAff.addActionListener(
        e -> myList.setPinAffordance((FlatPillList.IconAffordance) pinAff.getSelectedItem()));
    p.add(pinAff);

    p.add(new JLabel("Anchor:"));
    final JComboBox<FlatPillList.IconAffordance> anchorAff =
        new JComboBox<>(FlatPillList.IconAffordance.values());
    anchorAff.setSelectedItem(myList.getAnchorAffordance());
    anchorAff.addActionListener(
        e -> myList.setAnchorAffordance((FlatPillList.IconAffordance) anchorAff.getSelectedItem()));
    p.add(anchorAff);

    p.add(new JLabel("Select:"));
    final JComboBox<PillSelectionMode> sel = new JComboBox<>(PillSelectionMode.values());
    sel.setSelectedItem(myList.getSelectionMode());
    sel.addActionListener(e -> myList.setSelectionMode((PillSelectionMode) sel.getSelectedItem()));
    p.add(sel);

    return p;
  }

  private JPanel buildLiveListFooter() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    final JButton addBtn =
        new JButton(
            new AbstractAction("Add pill") {
              private int myCounter;

              @Override
              public void actionPerformed(final ActionEvent e) {
                myModel.add("New " + (++myCounter));
              }
            });
    p.add(addBtn);

    final JButton clearBtn =
        new JButton(
            new AbstractAction("Clear all") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                myModel.clear();
              }
            });
    p.add(clearBtn);

    final JButton repopulate =
        new JButton(
            new AbstractAction("Reset items") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                myModel.clear();
                for (String s : SAMPLE_ITEMS) {
                  myModel.add(s);
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
    addLafRow(grid, "Background", buildColorPicker(FlatPill.K_BACKGROUND));
    addLafRow(grid, "Foreground", buildColorPicker(FlatPill.K_FOREGROUND));
    addLafRow(grid, "Border color", buildColorPicker(FlatPill.K_BORDER_COLOR));
    addLafRow(grid, "Hover background", buildColorPicker(FlatPill.K_HOVER_BACKGROUND));
    addLafRow(grid, "Pressed background", buildColorPicker(FlatPill.K_PRESSED_BACKGROUND));
    addLafRow(grid, "Selected background", buildColorPicker(FlatPill.K_SELECTED_BACKGROUND));
    addLafRow(grid, "Selected border", buildColorPicker(FlatPill.K_SELECTED_BORDER_COLOR));
    addLafRow(grid, "Focus color", buildColorPicker(FlatPill.K_FOCUS_COLOR));
    addLafRow(grid, "Disabled background", buildColorPicker(FlatPill.K_DISABLED_BACKGROUND));
    addLafRow(grid, "Warm accent", buildColorPicker(FlatPill.K_WARM_ACCENT));

    final JButton reset =
        new JButton(
            new AbstractAction("Reset all FlatPill.* keys") {
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

  private void addLafRow(final JPanel theGrid, final String theLabel, final JComponent theControl) {
    final JLabel l = new JLabel(theLabel);
    l.putClientProperty("FlatLaf.styleClass", "small");
    theGrid.add(l);
    theGrid.add(theControl);
  }

  private JSlider buildArcSlider() {
    // Range 0-60 covers everything from sharp-cornered (0) past typical capsule depth (≈30) up to
    // the visible cap (clampArc() in FlatPill bounds it by min(width, height) at paint time).
    final JSlider s = new JSlider(0, 60, 20);
    s.setMajorTickSpacing(20);
    s.setPaintTicks(true);
    s.addChangeListener(
        e -> {
          UIManager.put(FlatPill.K_ARC, s.getValue());
          repaintLive();
        });
    return s;
  }

  private JSlider buildPaddingSlider(final boolean theHorizontal) {
    final JSlider s = new JSlider(0, 30, theHorizontal ? 10 : 4);
    s.addChangeListener(
        e -> {
          final int v = s.getValue();
          final Insets cur =
              UIManager.get(FlatPill.K_PADDING) instanceof Insets in
                  ? in
                  : new Insets(4, 10, 4, 10);
          final Insets next =
              theHorizontal
                  ? new Insets(cur.top, v, cur.bottom, v)
                  : new Insets(v, cur.left, v, cur.right);
          UIManager.put(FlatPill.K_PADDING, next);
          // K_PADDING is baked into each pill's Swing Border at construction time, so a
          // plain repaint won't pick up runtime changes — push the new value through the
          // updateUI cascade, which causes FlatPill.updateUI() to call rebuildBorder() and
          // read the fresh UIManager value. Standard Swing convention for live UIManager
          // tweaks; the same dance is what FlatLaf does for global theme switches.
          SwingUtilities.updateComponentTreeUI(myList);
        });
    return s;
  }

  private JButton buildColorPicker(final String theKey) {
    final JButton btn = new JButton();
    btn.setPreferredSize(new Dimension(80, 22));
    refreshSwatch(btn, theKey);
    btn.addActionListener(
        e -> {
          final Color current = UIManager.getColor(theKey);
          final Color chosen =
              JColorChooser.showDialog(myFrame, theKey, current == null ? Color.WHITE : current);
          if (chosen != null) {
            UIManager.put(theKey, chosen);
            refreshSwatch(btn, theKey);
            repaintLive();
          }
        });
    return btn;
  }

  private void refreshSwatch(final JButton theButton, final String theKey) {
    final Color c = UIManager.getColor(theKey);
    if (c != null) {
      theButton.setBackground(c);
      theButton.setText(toHex(c));
      theButton.setForeground(isLight(c) ? Color.BLACK : Color.WHITE);
    } else {
      theButton.setBackground(null);
      theButton.setText("(theme default)");
      theButton.setForeground(UIManager.getColor("Label.disabledForeground"));
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
      FlatPill.K_BACKGROUND,
      FlatPill.K_FOREGROUND,
      FlatPill.K_BORDER_COLOR,
      FlatPill.K_ARC,
      FlatPill.K_PADDING,
      FlatPill.K_HOVER_BACKGROUND,
      FlatPill.K_PRESSED_BACKGROUND,
      FlatPill.K_SELECTED_BACKGROUND,
      FlatPill.K_SELECTED_BORDER_COLOR,
      FlatPill.K_FOCUS_COLOR,
      FlatPill.K_DISABLED_BACKGROUND,
      FlatPill.K_WARM_ACCENT
    };
    for (String k : keys) {
      UIManager.put(k, null);
    }
    repaintLive();
    SwingUtilities.updateComponentTreeUI(myFrame);
  }

  private void repaintLive() {
    if (myList != null) {
      myList.repaint();
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

  private void applyTheme(final boolean theLight) {
    try {
      if (theLight) {
        FlatLightLaf.setup();
      } else {
        FlatDarkLaf.setup();
      }
      SwingUtilities.updateComponentTreeUI(myFrame);
    } catch (Exception ignored) {
      // theme switch failures don't merit a popup in a demo
    }
  }
}
