package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.fab.ElwhaFabAnchor;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #269 smoketest — {@link ElwhaFabAnchor} Phase 2 scroll-aware behavior. A tall scrollable
 * panel is wrapped in an anchor whose floating Extended FAB reacts to scroll; the control bar
 * switches the {@link ElwhaFabAnchor.ScrollResponse} so the reviewer can confirm each:
 *
 * <ul>
 *   <li><strong>NONE</strong> — static placement (Phase 1 behavior), FAB ignores scroll.
 *   <li><strong>HIDE</strong> — scroll down slides the FAB off the bottom edge; scroll up slides it
 *       back (M3 G14b).
 *   <li><strong>SHRINK</strong> — scroll down morphs Extended → Standard (icon-only); scroll up
 *       morphs back, staying corner-pinned through the animation (M3 G33 / G34, bidirectional).
 * </ul>
 *
 * <p>Resize mid-scroll to confirm pinning still holds. Toggle the OS reduced-motion setting (or the
 * Showcase Animation control) to confirm both responses snap with no tween. The FAB is built via
 * {@link ElwhaFab#extended(javax.swing.Icon, String)} so it carries both morph endpoints SHRINK
 * needs.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabAnchorScrollPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabAnchorScrollPlayground {

  private final JFrame frame = new JFrame("ElwhaFabAnchor — Phase 2 scroll-aware (#269)");
  private final ElwhaFabAnchor anchor =
      new ElwhaFabAnchor(
          buildScrollableContent(),
          ElwhaFab.extended(MaterialIcons.editFilled(ElwhaFab.Size.SMALL.iconPx()), "Compose"));

  private ElwhaFabAnchorScrollPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaFabAnchorScrollPlayground().launch());
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
    rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
    for (int i = 1; i <= 80; i++) {
      final JLabel row = new JLabel("Content row " + i + " — scroll to drive the floating FAB.");
      row.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
      rows.add(row);
    }
    return new JScrollPane(rows);
  }

  private JPanel buildControlBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    final JComboBox<ElwhaFabAnchor.ScrollResponse> responseBox =
        new JComboBox<>(ElwhaFabAnchor.ScrollResponse.values());
    responseBox.setSelectedItem(anchor.getScrollResponse());
    responseBox.addActionListener(
        e ->
            anchor.setScrollResponse(
                (ElwhaFabAnchor.ScrollResponse) responseBox.getSelectedItem()));
    bar.add(new JLabel("Scroll response:"));
    bar.add(responseBox);
    bar.add(new JLabel("   (scroll down → hide / shrink; scroll up → restore)"));
    return bar;
  }
}
