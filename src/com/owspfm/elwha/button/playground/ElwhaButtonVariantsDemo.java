package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Story 3 (#116) scratch demo: visual smoketest for {@link ElwhaButton}'s five variants ×
 * interaction modes × selection state, plus the {@code TEXT + SELECTABLE} runtime guard.
 *
 * <p>Three sections stacked vertically:
 *
 * <ol>
 *   <li><strong>CLICKABLE matrix</strong> — 5 variants × 4 pre-rendered states (Enabled / Disabled
 *       / Hovered / Pressed). The Focused state is not pre-rendered statically (component-level
 *       focus owner is dynamic); tab into the LIVE row to see the focus border swap.
 *   <li><strong>SELECTABLE matrix</strong> — 4 variants (TEXT excluded — see throw row below) × 3
 *       pre-rendered selection × hover states, demonstrating the §7 hybrid color model: FILLED and
 *       OUTLINED swap surface roles; ELEVATED and FILLED_TONAL composite the uniform 12% SELECTED
 *       overlay + PRIMARY border swap.
 *   <li><strong>Live interaction rows</strong> — one button per variant for CLICKABLE and per
 *       toggleable variant for SELECTABLE, wired to a status label. Plus a "with icon" row to
 *       verify leading-icon layout, ripple animation, focus traversal, and Space/Enter activation.
 * </ol>
 *
 * <p>Top toolbar toggles light / dark mode and square / round shape. The {@code TEXT + SELECTABLE}
 * throw is verified at startup; both directions of the symmetric guard print to the status label.
 *
 * <p>Run: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.button.playground.ElwhaButtonVariantsDemo"}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaButtonVariantsDemo {

  private static final ButtonVariant[] ALL_VARIANTS = {
    ButtonVariant.ELEVATED,
    ButtonVariant.FILLED,
    ButtonVariant.FILLED_TONAL,
    ButtonVariant.OUTLINED,
    ButtonVariant.TEXT
  };

  private static final ButtonVariant[] SELECTABLE_VARIANTS = {
    ButtonVariant.ELEVATED, ButtonVariant.FILLED, ButtonVariant.FILLED_TONAL, ButtonVariant.OUTLINED
  };

  private ButtonShape currentShape = ButtonShape.ROUND;

  private final JLabel statusLabel = new JLabel(" ");
  private final JPanel matrixPanel = new JPanel();

  private ElwhaButtonVariantsDemo() {}

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
          new ElwhaButtonVariantsDemo().show();
        });
  }

  private void show() {
    final JFrame frame = new JFrame("ElwhaButton — Story 3 Variants Demo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    frame.add(buildToolbar(), BorderLayout.NORTH);

    matrixPanel.setLayout(new BoxLayout(matrixPanel, BoxLayout.Y_AXIS));
    rebuildMatrix();
    final JScrollPane scroll = new JScrollPane(matrixPanel);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    frame.add(scroll, BorderLayout.CENTER);

    statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    statusLabel.setText(verifyTextToggleGuard());
    frame.add(statusLabel, BorderLayout.SOUTH);

    frame.setSize(1100, 900);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // -------------------------------------------------------------- toolbar

  private JPanel buildToolbar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    bar.setBackground(ColorRole.SURFACE_CONTAINER_LOW.resolve());

    final JComboBox<Mode> modeBox = new JComboBox<>(new Mode[] {Mode.LIGHT, Mode.DARK});
    modeBox.setSelectedItem(Mode.LIGHT);
    modeBox.addActionListener(
        e -> {
          ElwhaTheme.install(ElwhaTheme.current().withMode((Mode) modeBox.getSelectedItem()));
          rebuildMatrix();
          SwingUtilities.invokeLater(() -> SwingUtilities.windowForComponent(bar).repaint());
        });

    final JCheckBox squareToggle = new JCheckBox("Square shape");
    squareToggle.setOpaque(false);
    squareToggle.addActionListener(
        e -> {
          currentShape = squareToggle.isSelected() ? ButtonShape.SQUARE : ButtonShape.ROUND;
          rebuildMatrix();
        });

    bar.add(new JLabel("Mode:"));
    bar.add(modeBox);
    bar.add(Box.createHorizontalStrut(20));
    bar.add(squareToggle);
    return bar;
  }

  // ------------------------------------------------------------- matrix

  private void rebuildMatrix() {
    matrixPanel.removeAll();
    matrixPanel.setBackground(ColorRole.SURFACE.resolve());
    matrixPanel.add(buildClickableMatrix());
    matrixPanel.add(buildSelectableMatrix());
    matrixPanel.add(buildLivePanel());
    matrixPanel.revalidate();
    matrixPanel.repaint();
  }

  private JPanel buildClickableMatrix() {
    final JPanel section = section("CLICKABLE — 5 variants × 4 states (focus visible in LIVE row)");
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setOpaque(false);
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 8, 8, 8);
    gbc.anchor = GridBagConstraints.CENTER;

    final String[] stateLabels = {"Enabled", "Disabled", "Hovered", "Pressed"};

    gbc.gridy = 0;
    gbc.gridx = 0;
    grid.add(header(""), gbc);
    for (int i = 0; i < stateLabels.length; i++) {
      gbc.gridx = i + 1;
      grid.add(header(stateLabels[i]), gbc);
    }

    int row = 1;
    for (ButtonVariant v : ALL_VARIANTS) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      grid.add(header(v.name()), gbc);

      gbc.gridx = 1;
      grid.add(makeBtn(v, false, false, false), gbc);
      gbc.gridx = 2;
      grid.add(makeBtn(v, true, false, false), gbc);
      gbc.gridx = 3;
      grid.add(makeBtn(v, false, true, false), gbc);
      gbc.gridx = 4;
      grid.add(makeBtn(v, false, false, true), gbc);
    }

    section.add(grid);
    return section;
  }

  private JPanel buildSelectableMatrix() {
    final JPanel section =
        section("SELECTABLE — 4 variants × 3 selection states (TEXT throws — see status bar)");
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setOpaque(false);
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 8, 8, 8);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridy = 0;
    gbc.gridx = 0;
    grid.add(header(""), gbc);
    gbc.gridx = 1;
    grid.add(header("Unselected"), gbc);
    gbc.gridx = 2;
    grid.add(header("Selected"), gbc);
    gbc.gridx = 3;
    grid.add(header("Selected + Hover"), gbc);

    int row = 1;
    for (ButtonVariant v : SELECTABLE_VARIANTS) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      grid.add(header(v.name()), gbc);

      gbc.gridx = 1;
      grid.add(makeToggle(v, false, false), gbc);
      gbc.gridx = 2;
      grid.add(makeToggle(v, true, false), gbc);
      gbc.gridx = 3;
      grid.add(makeToggle(v, true, true), gbc);
    }

    section.add(grid);
    return section;
  }

  private JPanel buildLivePanel() {
    final JPanel section = section("LIVE — click / Space / Enter; ripple animates; tab for focus");
    final JPanel pushRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    pushRow.setOpaque(false);
    for (ButtonVariant v : ALL_VARIANTS) {
      final ElwhaButton b = new ElwhaButton(v.name()).setVariant(v).setShape(currentShape);
      b.addActionListener(e -> statusLabel.setText("Clicked: " + v.name()));
      pushRow.add(b);
    }
    final ElwhaButton iconBtn =
        new ElwhaButton("With icon", MaterialIcons.delete(20)).setShape(currentShape);
    iconBtn.addActionListener(e -> statusLabel.setText("Clicked: With icon (FILLED)"));
    pushRow.add(iconBtn);

    final JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    toggleRow.setOpaque(false);
    for (ButtonVariant v : SELECTABLE_VARIANTS) {
      final ElwhaButton b =
          new ElwhaButton(v.name())
              .setVariant(v)
              .setInteractionMode(ButtonInteractionMode.SELECTABLE)
              .setShape(currentShape);
      b.addSelectionChangeListener(
          evt -> statusLabel.setText("Toggle " + v.name() + " → selected=" + b.isSelected()));
      toggleRow.add(b);
    }

    final JPanel stack = new JPanel();
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
    stack.setOpaque(false);
    stack.add(pushRow);
    stack.add(toggleRow);
    section.add(stack);
    return section;
  }

  // ----------------------------------------------------------- factories

  private ElwhaButton makeBtn(
      final ButtonVariant variant,
      final boolean disabled,
      final boolean hovered,
      final boolean pressed) {
    final ElwhaButton b =
        new ElwhaButton(variant.name()).setVariant(variant).setShape(currentShape);
    b.setEnabled(!disabled);
    b.setHovered(hovered);
    b.setPressed(pressed);
    return b;
  }

  private ElwhaButton makeToggle(
      final ButtonVariant variant, final boolean selected, final boolean hovered) {
    final ElwhaButton b =
        new ElwhaButton(variant.name())
            .setVariant(variant)
            .setInteractionMode(ButtonInteractionMode.SELECTABLE)
            .setShape(currentShape)
            .setSelected(selected);
    b.setHovered(hovered);
    return b;
  }

  // ------------------------------------------------------------- ui glue

  private JPanel section(final String title) {
    final JPanel s = new JPanel();
    s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
    s.setOpaque(false);
    s.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    final JLabel l = new JLabel(title);
    l.setFont(TypeRole.TITLE_MEDIUM.resolve());
    l.setForeground(ColorRole.ON_SURFACE.resolve());
    l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    s.add(l);
    return s;
  }

  private JLabel header(final String text) {
    final JLabel l = new JLabel(text);
    l.setFont(TypeRole.LABEL_SMALL.resolve());
    l.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    return l;
  }

  // ----------------------------------------------------------- a11y guard

  private static String verifyTextToggleGuard() {
    boolean part1 = false;
    try {
      new ElwhaButton("test")
          .setVariant(ButtonVariant.TEXT)
          .setInteractionMode(ButtonInteractionMode.SELECTABLE);
    } catch (IllegalStateException expected) {
      part1 = true;
    }
    boolean part2 = false;
    try {
      new ElwhaButton("test")
          .setInteractionMode(ButtonInteractionMode.SELECTABLE)
          .setVariant(ButtonVariant.TEXT);
    } catch (IllegalStateException expected) {
      part2 = true;
    }
    if (part1 && part2) {
      return "OK — TEXT + SELECTABLE guard verified (both directions throw IllegalStateException)";
    }
    return "FAILED: TEXT + SELECTABLE guard part1=" + part1 + " part2=" + part2;
  }
}
