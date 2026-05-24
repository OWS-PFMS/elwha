package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual smoke-test for the Phase 2 morph wiring on {@link ElwhaButton}. A grid of buttons in each
 * variant × size × interaction mode, all wired with the press shape + press width morph (held while
 * pressed, release reverses) and the select round↔square morph (toggles on click for {@link
 * ButtonInteractionMode#SELECTABLE}). A {@link JCheckBox} flips {@link
 * MorphAnimator#setReducedMotion(boolean)} globally so the visual collapses to the v1 static
 * behavior.
 *
 * <p>Independent of The Elwha Showcase per {@code docs/research/elwha-button-anim-design.md} §14
 * (Showcase wiring is Phase 5) and per [[fresh-demo-per-story]] (each story gets its own artifact).
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.button.playground.ButtonMorphPlayground"}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ButtonMorphPlayground {

  private ButtonMorphPlayground() {}

  /**
   * Entry point — installs a baseline LIGHT theme and shows the smoketest frame.
   *
   * @param args ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(ButtonMorphPlayground::buildAndShow);
  }

  private static void buildAndShow() {
    final JFrame frame = new JFrame("ElwhaButton morph (#176 Phase 2) — smoke-test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    final JCheckBox reducedBox = new JCheckBox("Reduced motion (global)");
    reducedBox.setSelected(MorphAnimator.isReducedMotion());
    reducedBox.addActionListener(e -> MorphAnimator.setReducedMotion(reducedBox.isSelected()));
    toolbar.add(reducedBox);
    toolbar.add(
        new JLabel(
            "  Click a button: press shape + press width morph holds while pressed; "
                + "selectables animate round ↔ square on toggle."));

    final JPanel grid = new JPanel(new GridLayout(0, 4, 16, 16));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    // Row: CLICKABLE — press morph only, no select flip
    grid.add(label("CLICKABLE · ROUND · S"));
    grid.add(label("CLICKABLE · SQUARE · S"));
    grid.add(label("CLICKABLE · ROUND · L"));
    grid.add(label("CLICKABLE · SQUARE · L"));
    grid.add(button("Filled", ButtonShape.ROUND, ButtonSize.S, false));
    grid.add(button("Filled", ButtonShape.SQUARE, ButtonSize.S, false));
    grid.add(button("Filled", ButtonShape.ROUND, ButtonSize.L, false));
    grid.add(button("Filled", ButtonShape.SQUARE, ButtonSize.L, false));

    // Row: SELECTABLE — press morph AND select flip animation
    grid.add(label("SELECTABLE · ROUND · S"));
    grid.add(label("SELECTABLE · SQUARE · S"));
    grid.add(label("SELECTABLE · ROUND · L"));
    grid.add(label("SELECTABLE · SQUARE · L"));
    grid.add(button("Toggle", ButtonShape.ROUND, ButtonSize.S, true));
    grid.add(button("Toggle", ButtonShape.SQUARE, ButtonSize.S, true));
    grid.add(button("Toggle", ButtonShape.ROUND, ButtonSize.L, true));
    grid.add(button("Toggle", ButtonShape.SQUARE, ButtonSize.L, true));

    // Row: pre-seeded SELECTED state — addNotify should snap the morph, no initial-paint
    // animation. If this row animates from unselected → selected on first display, the
    // addNotify snap is missing or broken.
    grid.add(label("SELECTED · ROUND · S"));
    grid.add(label("SELECTED · SQUARE · S"));
    grid.add(label("SELECTED · ROUND · L"));
    grid.add(label("SELECTED · SQUARE · L"));
    grid.add(selectedButton("On", ButtonShape.ROUND, ButtonSize.S));
    grid.add(selectedButton("On", ButtonShape.SQUARE, ButtonSize.S));
    grid.add(selectedButton("On", ButtonShape.ROUND, ButtonSize.L));
    grid.add(selectedButton("On", ButtonShape.SQUARE, ButtonSize.L));

    frame.add(toolbar, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JLabel label(final String text) {
    final JLabel l = new JLabel(text, JLabel.CENTER);
    l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
    return l;
  }

  private static ElwhaButton button(
      final String text, final ButtonShape shape, final ButtonSize size, final boolean selectable) {
    final ElwhaButton b = new ElwhaButton(text).setShape(shape).setButtonSize(size);
    if (selectable) {
      b.setInteractionMode(ButtonInteractionMode.SELECTABLE);
    }
    return b;
  }

  private static ElwhaButton selectedButton(
      final String text, final ButtonShape shape, final ButtonSize size) {
    return new ElwhaButton(text)
        .setShape(shape)
        .setButtonSize(size)
        .setInteractionMode(ButtonInteractionMode.SELECTABLE)
        .setSelected(true);
  }
}
