package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.fab.ElwhaFabAnchor;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #258 smoketest — {@link ElwhaFabAnchor} Phase 1 static floating placement. A long
 * scrollable content panel is wrapped in an anchor with a floating Extended FAB; the control bar
 * drives the three Phase 1 knobs so the reviewer can confirm each:
 *
 * <ul>
 *   <li><strong>Corner</strong> — the FAB jumps between all four corners, body pinned to the spec
 *       margin at each.
 *   <li><strong>RTL</strong> — flipping orientation mirrors {@code TRAILING}/{@code LEADING} to the
 *       opposite edge (and mirrors the FAB's own icon/label) with no other change.
 *   <li><strong>Inset</strong> — the body-to-edge gap tracks the spinner (default 16 dp).
 * </ul>
 *
 * <p>Scroll the content and resize the window: the FAB stays glued to its corner above the
 * scrolling rows with no flicker, demonstrating the {@code doLayout} resize-pinning that replaces
 * the §15 recipe's {@code ComponentListener}.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabAnchorPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabAnchorPlayground {

  private final JFrame frame = new JFrame("ElwhaFabAnchor — Phase 1 static placement (#258)");
  private final ElwhaFabAnchor anchor =
      new ElwhaFabAnchor(
          buildScrollableContent(),
          ElwhaFab.extended(MaterialIcons.editFilled(ElwhaFab.Size.SMALL.iconPx()), "Compose"));

  private ElwhaFabAnchorPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaFabAnchorPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildControlBar(), BorderLayout.NORTH);
    frame.add(anchor, BorderLayout.CENTER);
    frame.setSize(960, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JScrollPane buildScrollableContent() {
    final JPanel rows = new JPanel();
    rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
    for (int i = 1; i <= 40; i++) {
      final JLabel row = new JLabel("Content row " + i + " — scroll me; the FAB floats above.");
      row.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
      rows.add(row);
    }
    return new JScrollPane(rows);
  }

  private JPanel buildControlBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));

    final JComboBox<ElwhaFabAnchor.Corner> cornerBox =
        new JComboBox<>(ElwhaFabAnchor.Corner.values());
    cornerBox.setSelectedItem(anchor.getCorner());
    cornerBox.addActionListener(
        e -> anchor.setCorner((ElwhaFabAnchor.Corner) cornerBox.getSelectedItem()));

    final JToggleButton rtl = new JToggleButton("RTL");
    rtl.addActionListener(
        e -> {
          anchor.applyComponentOrientation(
              rtl.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          anchor.revalidate();
          anchor.repaint();
        });

    final JSpinner inset = new JSpinner(new SpinnerNumberModel(anchor.getInsetDp(), 0, 96, 4));
    inset.addChangeListener(e -> anchor.setInsetDp((Integer) inset.getValue()));

    bar.add(new JLabel("Corner:"));
    bar.add(cornerBox);
    bar.add(Box.createHorizontalStrut(8));
    bar.add(rtl);
    bar.add(Box.createHorizontalStrut(8));
    bar.add(new JLabel("Inset (dp):"));
    bar.add(inset);
    return bar;
  }
}
