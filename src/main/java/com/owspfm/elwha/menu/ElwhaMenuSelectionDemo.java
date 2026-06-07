package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.GridLayout;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Visual demo for epic #298 S6 — the {@link SelectionMode} axis. Three columns, one per mode, each
 * with a live {@link ElwhaButton} trigger that opens a real overlay menu and a status line that
 * reports what happened:
 *
 * <ul>
 *   <li><strong>NONE</strong> — an action menu: picking an item fires its action and closes;
 *       nothing stays selected (the status shows the last action).
 *   <li><strong>SINGLE</strong> — picks one view; selecting auto-deselects the prior and closes the
 *       menu (the status shows the current selection). "List" starts pre-selected.
 *   <li><strong>MULTI</strong> — toggles columns; the menu stays open so several can be checked off
 *       in one session (the status shows the running set).
 * </ul>
 *
 * <p>Mouse and keyboard drive selection identically — open a menu and use Up/Down + Enter/Space.
 * Dogfoods {@link ElwhaButton} triggers per the operator's house rule. Pass {@code dark} as the
 * first arg for dark mode.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaMenuSelectionDemo {

  private ElwhaMenuSelectionDemo() {}

  public static void main(final String[] args) {
    final boolean dark = args.length > 0 && "dark".equalsIgnoreCase(args[0]);
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(dark ? Mode.DARK : Mode.LIGHT)
            .build());
    SwingUtilities.invokeLater(ElwhaMenuSelectionDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu S6 — SelectionMode");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(840, 360);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new GridLayout(1, 3, 24, 0));
    content.setBackground(ColorRole.SURFACE.resolve());
    content.setBorder(BorderFactory.createEmptyBorder(28, 24, 28, 24));
    content.add(noneColumn());
    content.add(singleColumn());
    content.add(multiColumn());

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  // --- NONE: action menu, last action reported -----------------------------

  private static JComponent noneColumn() {
    final JLabel status = status("Last action: —");
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.NONE)
            .addItem(action("Rename", MaterialIcons.edit(20), status))
            .addItem(action("Duplicate", MaterialIcons.add(20), status))
            .addItem(action("Delete", MaterialIcons.delete(20), status))
            .build();
    final ElwhaButton trigger = ElwhaButton.outlinedButton("Actions ⋯");
    trigger.addActionListener(e -> menu.open(trigger));
    return column("NONE — action menu", "fires + closes; no selection", trigger, status);
  }

  private static ElwhaMenuItem action(
      final String label, final javax.swing.Icon icon, final JLabel status) {
    final ElwhaMenuItem item = ElwhaMenuItem.of(icon, label);
    item.addActionListener(e -> status.setText("Last action: " + label));
    return item;
  }

  // --- SINGLE: one view, auto-deselect + close -----------------------------

  private static JComponent singleColumn() {
    final JLabel status = status("View: List");
    final ElwhaMenuItem list = ElwhaMenuItem.of(MaterialIcons.menu(20), "List");
    final ElwhaMenuItem grid = ElwhaMenuItem.of(MaterialIcons.gridView(20), "Grid");
    final ElwhaMenuItem gallery = ElwhaMenuItem.of(MaterialIcons.widgets(20), "Gallery");
    list.setSelected(true);
    // Build the menu once and reuse it so the pick persists: reopen and the chosen view is still
    // checked. (A fresh menu per open would reset to "List" every time.)
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.SINGLE)
            .onSelectionChange(item -> status.setText("View: " + item.getLabel()))
            .addItem(list)
            .addItem(grid)
            .addItem(gallery)
            .build();
    final ElwhaButton trigger = ElwhaButton.outlinedButton("View ▾");
    trigger.addActionListener(e -> menu.open(trigger));
    return column("SINGLE — one of N", "selects one, closes; pick persists", trigger, status);
  }

  // --- MULTI: toggle columns, stays open -----------------------------------

  private static JComponent multiColumn() {
    final JLabel status = status("Columns: —");
    // One instance, reused — the toggled set accumulates and persists across reopens.
    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.MULTI)
            .onSelectionChange(item -> status.setText("Columns: " + summary(item)))
            .addItem(ElwhaMenuItem.of(MaterialIcons.star(20), "Name"))
            .addItem(ElwhaMenuItem.of(MaterialIcons.autorenew(20), "Modified"))
            .addItem(ElwhaMenuItem.of(MaterialIcons.layers(20), "Size"))
            .build();
    final ElwhaButton trigger = ElwhaButton.outlinedButton("Columns ▾");
    trigger.addActionListener(e -> menu.open(trigger));
    return column("MULTI — toggle", "stays open; toggles persist", trigger, status);
  }

  private static String summary(final ElwhaMenuItem changed) {
    final java.awt.Container parent = changed.getParent();
    if (parent == null) {
      return changed.isSelected() ? changed.getLabel() : "—";
    }
    final String set =
        java.util.Arrays.stream(parent.getComponents())
            .filter(ElwhaMenuItem.class::isInstance)
            .map(ElwhaMenuItem.class::cast)
            .filter(ElwhaMenuItem::isSelected)
            .map(ElwhaMenuItem::getLabel)
            .collect(Collectors.joining(", "));
    return set.isEmpty() ? "—" : set;
  }

  // --- scaffolding ---------------------------------------------------------

  private static JComponent column(
      final String title, final String subtitle, final ElwhaButton trigger, final JLabel status) {
    final JPanel col = new JPanel();
    col.setOpaque(false);
    col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

    final JLabel heading = new JLabel(title, SwingConstants.CENTER);
    heading.setForeground(ColorRole.ON_SURFACE.resolve());
    heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD));
    heading.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    col.add(heading);

    final JLabel sub = new JLabel(subtitle, SwingConstants.CENTER);
    sub.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    sub.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    col.add(sub);
    col.add(Box.createVerticalStrut(16));

    trigger.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    col.add(trigger);
    col.add(Box.createVerticalStrut(16));

    status.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    col.add(status);
    col.add(Box.createVerticalGlue());
    return col;
  }

  private static JLabel status(final String text) {
    final JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setForeground(ColorRole.ON_SURFACE.resolve());
    return label;
  }
}
