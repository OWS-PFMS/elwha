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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * S4 visual smoke for drag-to-resize (#511): an embedded standard sheet with {@code
 * setResizable(true)}, bounds 200–480. Hover the sheet's content-facing (inner) edge — the cursor
 * becomes a horizontal-resize cursor — and drag to grow/shrink the sheet; the main content reflows
 * live and the width readout updates, clamped at both bounds. The posture toggle confirms the
 * resize edge tracks the floating body when detached.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetResizeDemo {

  private SideSheetResizeDemo() {}

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
    SwingUtilities.invokeLater(SideSheetResizeDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet V2 — S4 drag-to-resize (#511)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Resizable");
    sheet.setResizable(true);
    sheet.setMinSheetWidth(200);
    sheet.setMaxSheetWidth(480);
    sheet.setContent(filters());
    sheet.setActions(ElwhaButton.filledButton("Apply"));

    final JLabel readout =
        new JLabel("width " + sheet.getSheetWidth() + " px (drag the inner edge)");
    sheet.addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            readout.setText("width " + sheet.getSheetWidth() + " px  (bounds 200–480)");
          }
        });

    final JPanel stage = new JPanel(new BorderLayout());
    stage.add(main(), BorderLayout.CENTER);
    stage.add(sheet, BorderLayout.LINE_END);

    final ElwhaButton posture = ElwhaButton.filledTonalButton("Toggle posture");
    posture.addActionListener(
        e -> {
          sheet.setSheetPosture(
              sheet.getSheetPosture() == SheetPosture.DETACHED
                  ? SheetPosture.DOCKED
                  : SheetPosture.DETACHED);
          stage.revalidate();
          stage.repaint();
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(posture);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(stage, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(980, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
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

  private static JPanel main() {
    final JPanel main = new JPanel(new GridLayout(0, 2, 16, 16));
    main.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    for (int i = 1; i <= 8; i++) {
      final JLabel cell = new JLabel("Reflows on resize " + i, SwingConstants.CENTER);
      cell.setFont(TypeRole.BODY_LARGE.resolve());
      cell.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      cell.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
      main.add(cell);
    }
    return main;
  }
}
