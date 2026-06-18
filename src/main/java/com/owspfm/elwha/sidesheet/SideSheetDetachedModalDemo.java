package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * S2 visual smoke for the detached <em>modal</em> {@link ElwhaSideSheet} presentation (#509): a
 * trigger frame that opens docked and detached modal sheets on either edge over the same scrim.
 * Verify the detached modal floats {@value ElwhaSideSheet#DETACHED_MARGIN_PX}px off all four window
 * edges with all corners rounded (the scrim shows through every margin), the docked modal still
 * sits flush, and the slide-in carries the whole floating card on/off screen from the resolved
 * edge. The posture toggle on an open sheet re-docks it in place.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetDetachedModalDemo {

  private SideSheetDetachedModalDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(SideSheetDetachedModalDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet V2 — S2 detached modal (#509)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final JPanel main = new JPanel(new GridLayout(0, 3, 16, 16));
    main.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    for (int i = 1; i <= 9; i++) {
      final JLabel cell = new JLabel("Background " + i, SwingConstants.CENTER);
      cell.setFont(TypeRole.BODY_LARGE.resolve());
      cell.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      cell.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
      main.add(cell);
    }

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(trigger(frame, "Docked · trailing", SheetPosture.DOCKED, SheetEdge.TRAILING));
    controls.add(trigger(frame, "Detached · trailing", SheetPosture.DETACHED, SheetEdge.TRAILING));
    controls.add(trigger(frame, "Docked · leading", SheetPosture.DOCKED, SheetEdge.LEADING));
    controls.add(trigger(frame, "Detached · leading", SheetPosture.DETACHED, SheetEdge.LEADING));

    frame.setLayout(new BorderLayout());
    frame.add(main, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(960, 600);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static ElwhaButton trigger(
      final JFrame frame, final String label, final SheetPosture posture, final SheetEdge edge) {
    final ElwhaButton button = ElwhaButton.filledTonalButton(label);
    button.addActionListener(
        e -> {
          final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Filters");
          sheet.setSheetPosture(posture);
          sheet.setSheetEdge(edge);
          sheet.setContent(filters());
          sheet.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.outlinedButton("Cancel"));
          sheet.showModal(frame.getContentPane());
        });
    return button;
  }

  private static JPanel filters() {
    final JPanel filters = new JPanel();
    filters.setOpaque(false);
    filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
    for (final String f :
        new String[] {"Watershed", "Reach", "Species", "Year range", "Survey type", "Gear"}) {
      final JLabel row = new JLabel(f);
      row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
      filters.add(row);
    }
    filters.add(Box.createVerticalGlue());
    return filters;
  }
}
