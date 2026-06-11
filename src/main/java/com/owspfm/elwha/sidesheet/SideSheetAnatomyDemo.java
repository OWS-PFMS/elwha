package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * S1 visual smoke for {@link ElwhaSideSheet} static chrome (#459): a configuration matrix of
 * embedded sheets — standard (square, SURFACE, edge divider) vs modal chrome
 * (SURFACE_CONTAINER_LOW, level-1 shadow, 16px content-facing corners), close/back affordances,
 * footer actions with divider — plus a light/dark toggle to eyeball token-correctness.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetAnatomyDemo {

  private SideSheetAnatomyDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(SideSheetAnatomyDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet — S1 static anatomy (#459)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaSideSheet standard = ElwhaSideSheet.standardSheet("Standard");
    standard.setContent(filler("SURFACE · elev 0 · square · edge divider"));

    final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Modal chrome");
    modal.setContent(filler("SURFACE_CONTAINER_LOW · elev 1 · 16px inner corners"));
    modal.setActions(ElwhaButton.filledButton("Save"), ElwhaButton.outlinedButton("Cancel"));

    final ElwhaSideSheet backFlow = ElwhaSideSheet.modalSheet("Step 2 of 3");
    backFlow.setBackAffordanceVisible(true);
    backFlow.setOnBack(() -> System.out.println("onBack ran"));
    backFlow.setContent(filler("back affordance · onBack logs"));

    final ElwhaSideSheet bare = ElwhaSideSheet.standardSheet("No affordances, no divider");
    bare.setCloseAffordanceVisible(false);
    bare.setEdgeDividerVisible(false);
    bare.setContent(filler("headline only"));

    final ElwhaSideSheet leading = ElwhaSideSheet.modalSheet("Leading edge");
    leading.setSheetEdge(SheetEdge.LEADING);
    leading.setContent(filler("corners round on the trailing side"));
    leading.setActions(ElwhaButton.filledButton("Apply"));

    final ElwhaSideSheet noFooterDivider = ElwhaSideSheet.modalSheet("Footer, no divider");
    noFooterDivider.setFooterDividerVisible(false);
    noFooterDivider.setContent(filler("actions without the divider"));
    noFooterDivider.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.textButton("Reset"));

    final JPanel grid = new JPanel(new GridLayout(1, 0, 24, 0));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    grid.add(sized(standard));
    grid.add(sized(modal));
    grid.add(sized(backFlow));
    grid.add(sized(bare));
    grid.add(sized(leading));
    grid.add(sized(noFooterDivider));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode nextMode =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(nextMode).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });
    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(mode);

    frame.setLayout(new BorderLayout());
    frame.add(grid, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(1720, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel sized(final ElwhaSideSheet sheet) {
    final JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    host.add(sheet, BorderLayout.CENTER);
    host.setPreferredSize(new Dimension(256, 460));
    return host;
  }

  private static JLabel filler(final String text) {
    final JLabel label = new JLabel("<html><div style='width:170px'>" + text + "</div></html>");
    label.setVerticalAlignment(SwingConstants.TOP);
    return label;
  }
}
