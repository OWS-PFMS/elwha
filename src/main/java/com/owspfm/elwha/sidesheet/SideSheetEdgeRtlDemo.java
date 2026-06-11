package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S4 visual smoke for {@link ElwhaSideSheet} edge anchoring + RTL (#465): the full edge ×
 * orientation matrix. Top: four embedded standard sheets — TRAILING/LEADING × LTR/RTL — each
 * showing its divider on the content-facing side and the header mirroring (back leading, close
 * trailing, paddings flipped) under RTL. Bottom: trigger buttons opening the <em>modal</em>
 * presentation in each combo — verify the docking side, the slide-in direction (from the resolved
 * edge), and the rounded content-facing corners all flip together.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetEdgeRtlDemo {

  private SideSheetEdgeRtlDemo() {}

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
    SwingUtilities.invokeLater(SideSheetEdgeRtlDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet — S4 edge × orientation matrix (#465)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final JPanel grid = new JPanel(new GridLayout(1, 4, 24, 0));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    grid.add(embedded(SheetEdge.TRAILING, ComponentOrientation.LEFT_TO_RIGHT, "TRAILING · LTR"));
    grid.add(embedded(SheetEdge.LEADING, ComponentOrientation.LEFT_TO_RIGHT, "LEADING · LTR"));
    grid.add(embedded(SheetEdge.TRAILING, ComponentOrientation.RIGHT_TO_LEFT, "TRAILING · RTL"));
    grid.add(embedded(SheetEdge.LEADING, ComponentOrientation.RIGHT_TO_LEFT, "LEADING · RTL"));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(modalTrigger(frame, SheetEdge.TRAILING, false, "Modal: TRAILING LTR"));
    controls.add(modalTrigger(frame, SheetEdge.LEADING, false, "Modal: LEADING LTR"));
    controls.add(modalTrigger(frame, SheetEdge.TRAILING, true, "Modal: TRAILING RTL"));
    controls.add(modalTrigger(frame, SheetEdge.LEADING, true, "Modal: LEADING RTL"));

    frame.setLayout(new BorderLayout());
    frame.add(grid, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(1480, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel embedded(
      final SheetEdge edge, final ComponentOrientation orientation, final String label) {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet(label);
    sheet.setSheetEdge(edge);
    sheet.setBackAffordanceVisible(true);
    sheet.setContent(new JLabel("divider on the content-facing side"));
    sheet.setActions(ElwhaButton.filledButton("Apply"));
    sheet.applyComponentOrientation(orientation);

    final JPanel host = new JPanel(new BorderLayout(0, 8));
    host.setOpaque(false);
    host.add(new JLabel(label), BorderLayout.NORTH);
    host.add(sheet, BorderLayout.CENTER);
    host.setPreferredSize(new Dimension(280, 460));
    return host;
  }

  private static ElwhaButton modalTrigger(
      final JFrame frame, final SheetEdge edge, final boolean rtl, final String label) {
    final ElwhaButton trigger = ElwhaButton.filledTonalButton(label);
    trigger.addActionListener(
        e -> {
          final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet(label);
          sheet.setSheetEdge(edge);
          sheet.setBackAffordanceVisible(true);
          sheet.setContent(new JLabel("slides in from its resolved edge"));
          sheet.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.textButton("Reset"));
          frame
              .getContentPane()
              .applyComponentOrientation(
                  rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
          sheet.showModal(frame.getContentPane());
          sheet.setOnClose(cause -> System.out.println(label + " closed: " + cause));
        });
    return trigger;
  }
}
