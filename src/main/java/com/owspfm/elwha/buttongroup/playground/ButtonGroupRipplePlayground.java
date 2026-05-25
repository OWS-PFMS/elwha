package com.owspfm.elwha.buttongroup.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
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
 * Visual smoke-test for the Phase 3 standard-{@link ElwhaButtonGroup} width-ripple — design doc §6.
 * When a segment of a STANDARD group is pressed, the pressed segment narrows (full press delta) and
 * its ±1 / ±2 neighbors borrow a diminishing fraction (30 % / 10 %) of the same delta. Segments
 * beyond ±2 don't ripple. CONNECTED groups don't ripple at all — M3 excludes them from the
 * choreography per §6.
 *
 * <p>Rows:
 *
 * <ol>
 *   <li>3 standard segments, S size — press the middle, watch the outer two pinch ~30 %.
 *   <li>5 standard segments, S size — press the center, watch the gradient: full → 30 % → 10 % → no
 *       ripple.
 *   <li>5 standard segments, L size — same as above at the larger tier, where the ripple is more
 *       visible.
 *   <li>Edge-segment press — press the first / last; only one side ripples (the §6 "row's edge
 *       borrows less" behavior falls out of the abs-distance formula).
 *   <li>3 CONNECTED segments — press any; the pill-pop is the existing static selection visual, the
 *       width-ripple does NOT fire (the group is connected).
 * </ol>
 *
 * <p>A {@link JCheckBox} flips {@link MorphAnimator#setReducedMotion(boolean)} globally so the
 * ripple collapses to a snap. Independent of The Elwha Showcase per {@code
 * docs/research/elwha-button-anim-design.md} §14 and per [[fresh-demo-per-story]].
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.buttongroup.playground.ButtonGroupRipplePlayground"}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ButtonGroupRipplePlayground {

  private ButtonGroupRipplePlayground() {}

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
    SwingUtilities.invokeLater(ButtonGroupRipplePlayground::buildAndShow);
  }

  private static void buildAndShow() {
    final JFrame frame = new JFrame("ElwhaButtonGroup width-ripple (#176 Phase 3) — smoke-test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    final JCheckBox reducedBox = new JCheckBox("Reduced motion (global)");
    reducedBox.setSelected(MorphAnimator.isReducedMotion());
    reducedBox.addActionListener(e -> MorphAnimator.setReducedMotion(reducedBox.isSelected()));
    toolbar.add(reducedBox);
    toolbar.add(
        new JLabel(
            "  Hold-press a segment: pressed = full pinch, ±1 = 30 %, ±2 = 10 %, ±3+ = none."));

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    grid.add(
        row(
            "3 segments · S · STANDARD — press middle, ±1 borrows",
            standardGroup(3, ButtonSize.S)));
    grid.add(
        row(
            "5 segments · S · STANDARD — press center, watch the gradient",
            standardGroup(5, ButtonSize.S)));
    grid.add(
        row(
            "5 segments · L · STANDARD — same at L size (ripple more visible)",
            standardGroup(5, ButtonSize.L)));
    grid.add(
        row(
            "5 segments · S · STANDARD — press the END segments (one-sided ripple)",
            standardGroup(5, ButtonSize.S)));
    grid.add(
        row(
            "3 segments · S · CONNECTED — selection pill-pop only, NO width-ripple",
            connectedGroup(3, ButtonSize.S)));

    frame.add(toolbar, BorderLayout.NORTH);
    frame.add(grid, BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel row(final String description, final ElwhaButtonGroup group) {
    final JPanel panel = new JPanel(new BorderLayout(0, 6));
    final JLabel label = new JLabel(description);
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
    panel.add(label, BorderLayout.NORTH);
    final JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    wrap.add(group);
    panel.add(wrap, BorderLayout.CENTER);
    return panel;
  }

  private static ElwhaButtonGroup standardGroup(final int count, final ButtonSize size) {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.standard()
            .setSelectionMode(SelectionMode.SINGLE)
            .setButtonSize(size)
            .setResizeMode(ResizeMode.FLEXIBLE);
    for (int i = 1; i <= count; i++) {
      group.add(new ElwhaButton("Seg " + i));
    }
    return group;
  }

  private static ElwhaButtonGroup connectedGroup(final int count, final ButtonSize size) {
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(size)
            .setResizeMode(ResizeMode.FLEXIBLE);
    for (int i = 1; i <= count; i++) {
      group.add(new ElwhaButton("Tab " + i));
    }
    return group;
  }
}
