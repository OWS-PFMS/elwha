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
 * Visual smoke-test for the Phase 4 connected-group animated pill-pop — design doc §7. Today's
 * connected groups snap the selection visual (pill jumps to the newly-selected segment). With Phase
 * 4 wiring, both the previously-selected segment and the newly-selected segment animate their
 * per-corner radii concurrently at 300 ms EASE_IN_OUT — the pill <em>slides</em> from one tab to
 * the next through a brief intermediate where both segments hold mid-flight radii.
 *
 * <p>Rows:
 *
 * <ol>
 *   <li>3 connected tabs, S size — click around, watch the pill slide between adjacent tabs.
 *   <li>5 connected tabs, S size — same with more travel distance between non-adjacent tabs.
 *   <li>5 connected tabs, L size — same at L, where the pill / end-cap contrast is more visible.
 *   <li>3 connected tabs, S size — rapid clicks (mid-animation state changes); the morph should
 *       start the next animation from the in-flight displayed value, not snap back.
 * </ol>
 *
 * <p>A {@link JCheckBox} flips {@link MorphAnimator#setReducedMotion(boolean)} globally so the
 * pill-pop collapses to a snap. Independent of The Elwha Showcase per {@code
 * docs/research/elwha-button-anim-design.md} §14 and per [[fresh-demo-per-story]].
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.buttongroup.playground.ConnectedPillPopPlayground"}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ConnectedPillPopPlayground {

  private ConnectedPillPopPlayground() {}

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
    SwingUtilities.invokeLater(ConnectedPillPopPlayground::buildAndShow);
  }

  private static void buildAndShow() {
    final JFrame frame = new JFrame("Connected pill-pop (#176 Phase 4) — smoke-test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    final JCheckBox reducedBox = new JCheckBox("Reduced motion (global)");
    reducedBox.setSelected(MorphAnimator.isReducedMotion());
    reducedBox.addActionListener(e -> MorphAnimator.setReducedMotion(reducedBox.isSelected()));
    toolbar.add(reducedBox);
    toolbar.add(
        new JLabel("  Click between tabs: the pill should slide, not snap (300 ms EASE_IN_OUT)."));

    final JPanel grid = new JPanel(new GridLayout(0, 1, 0, 20));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    grid.add(row("3 tabs · S — click adjacent tabs", connectedGroup(3, ButtonSize.S)));
    grid.add(row("5 tabs · S — click across larger distances", connectedGroup(5, ButtonSize.S)));
    grid.add(
        row(
            "5 tabs · L — same at L size (pill / end-cap contrast more visible)",
            connectedGroup(5, ButtonSize.L)));
    grid.add(
        row(
            "3 tabs · S — rapid clicks (mid-animation state changes; should not snap back)",
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
