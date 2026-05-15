package com.owspfm.elwha.surface.playground;

import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

/**
 * Reusable panel builders for the {@link ElwhaSurface} playground surfaces. Lets the standalone
 * {@code ElwhaSurfacePlayground} and the {@code ThemePlayground}'s {@code Surface} tab share one
 * canonical implementation of the role × shape matrix and the live control surface, so the
 * validation matrix and the live interaction surface stay in lockstep across both entry points.
 *
 * <p>Mirrors {@code com.owspfm.elwha.chip.playground.ChipPlaygroundPanels} — same factored-builder
 * pattern, same reasons.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class SurfacePlaygroundPanels {

  private static final int CELL_WIDTH = 64;
  private static final int CELL_HEIGHT = 36;
  private static final int ROLE_LABEL_WIDTH = 200;

  private SurfacePlaygroundPanels() {}

  /**
   * Builds the role × shape matrix panel: every {@link ColorRole} as a row × every {@link
   * ShapeScale} as a column, each cell a real {@link ElwhaSurface} instance. The matrix re-skins
   * end-to-end on a theme/mode switch — the binding-rule contract for the whole component set.
   *
   * <p>Roles that are never meant to serve as a surface (foreground {@code ON_*} roles, outlines,
   * {@code SHADOW}, {@code SCRIM}, {@code SURFACE_TINT}) are still rendered. That is by design —
   * the matrix is a validation surface, and seeing which roles look like surfaces vs. which look
   * like flat-colored blocks is the validation.
   *
   * @return the matrix panel
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JPanel buildMatrixPanel() {
    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 4, 4, 4);
    gbc.anchor = GridBagConstraints.WEST;

    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("ColorRole \\ ShapeScale"), gbc);
    final ShapeScale[] shapes = ShapeScale.values();
    for (int c = 0; c < shapes.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(shapes[c].key() + " (" + shapes[c].px() + ")"), gbc);
    }

    int row = 1;
    for (ColorRole role : ColorRole.values()) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      final JLabel roleLabel = roleLabel(role);
      matrix.add(roleLabel, gbc);
      for (int c = 0; c < shapes.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildCell(role, shapes[c]), gbc);
      }
    }

    return matrix;
  }

  /**
   * Builds the live-control panel: a single {@link ElwhaSurface} driven by combo boxes for role,
   * shape, border role, and border width. The same {@link ElwhaSurface} instance is the live target
   * — token mutations are visible immediately.
   *
   * @return the live-control panel
   * @version v0.1.0
   * @since v0.1.0
   */
  public static JPanel buildLivePanel() {
    final ElwhaSurface target = new ElwhaSurface();
    target.setPreferredSize(new Dimension(280, 180));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    controls.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

    controls.add(new JLabel("Surface role:"));
    final JComboBox<ColorRole> roleBox = new JComboBox<>(ColorRole.values());
    roleBox.setSelectedItem(target.getSurfaceRole());
    roleBox.addActionListener(e -> target.setSurfaceRole((ColorRole) roleBox.getSelectedItem()));
    controls.add(roleBox);

    controls.add(new JLabel("Shape:"));
    final JComboBox<ShapeScale> shapeBox = new JComboBox<>(ShapeScale.values());
    shapeBox.setSelectedItem(target.getShape());
    shapeBox.addActionListener(e -> target.setShape((ShapeScale) shapeBox.getSelectedItem()));
    controls.add(shapeBox);

    controls.add(new JLabel("Border role:"));
    final JComboBox<BorderRoleChoice> borderBox = new JComboBox<>(BorderRoleChoice.values());
    borderBox.setSelectedItem(BorderRoleChoice.NONE);
    borderBox.addActionListener(
        e -> target.setBorderRole(((BorderRoleChoice) borderBox.getSelectedItem()).role));
    controls.add(borderBox);

    controls.add(new JLabel("Border width:"));
    final JSpinner widthSpinner =
        new JSpinner(new SpinnerNumberModel(target.getBorderWidth(), 0, 8, 1));
    widthSpinner.addChangeListener(e -> target.setBorderWidth((Integer) widthSpinner.getValue()));
    controls.add(widthSpinner);

    final JPanel stage = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    stage.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    stage.add(target);

    final JPanel wrap = new JPanel();
    wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
    controls.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    stage.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    wrap.add(controls);
    wrap.add(stage);
    wrap.add(Box.createVerticalGlue());
    return wrap;
  }

  // ----- private helpers -----

  /** Wraps a nullable {@link ColorRole} as a combo-box entry, with {@code NONE} → null. */
  private enum BorderRoleChoice {
    NONE(null),
    OUTLINE(ColorRole.OUTLINE),
    OUTLINE_VARIANT(ColorRole.OUTLINE_VARIANT),
    PRIMARY(ColorRole.PRIMARY),
    SECONDARY(ColorRole.SECONDARY),
    TERTIARY(ColorRole.TERTIARY),
    ERROR(ColorRole.ERROR);

    final ColorRole role;

    BorderRoleChoice(final ColorRole role) {
      this.role = role;
    }
  }

  private static JComponent buildCell(final ColorRole role, final ShapeScale shape) {
    final ElwhaSurface cell = new ElwhaSurface().setSurfaceRole(role).setShape(shape);
    cell.setLayout(new BorderLayout());
    cell.setPreferredSize(new Dimension(CELL_WIDTH, CELL_HEIGHT));
    cell.setToolTipText(role.name() + "  ·  " + shape.name() + " (" + shape.px() + " px)");
    cell.add(new SampleGlyph(role), BorderLayout.CENTER);
    return cell;
  }

  private static JLabel roleLabel(final ColorRole role) {
    final JLabel label = new JLabel(role.name());
    label.putClientProperty("FlatLaf.styleClass", "small");
    label.setPreferredSize(new Dimension(ROLE_LABEL_WIDTH, CELL_HEIGHT));
    return label;
  }

  private static JLabel headerLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.putClientProperty("FlatLaf.styleClass", "small");
    label.setForeground(UIManager.getColor("Label.disabledForeground"));
    return label;
  }

  /**
   * A tiny "Aa" sample painted in the role's {@code on}-pair (or {@code ON_SURFACE} for non-pairing
   * roles). Lives inside each surface cell so the cell visualizes whether the role can serve as a
   * surface — pairing roles get a legible "Aa"; foreground / utility roles read as flat blocks.
   */
  private static final class SampleGlyph extends JComponent {

    private final ColorRole role;

    SampleGlyph(final ColorRole role) {
      this.role = role;
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Color fg = role.on().map(ColorRole::resolve).orElseGet(ColorRole.ON_SURFACE::resolve);
      g.setColor(fg);
      g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
      g.drawString("Aa", 6, getHeight() / 2 + 4);
    }
  }
}
