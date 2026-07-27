package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
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
 * S1 visual smoke for the detached <em>standard</em> {@link ElwhaSideSheet} posture (#508): a
 * BorderLayout stage with a standard sheet docked at the trailing edge whose {@link SheetPosture}
 * toggles live. Verify the detached chrome against the docked baseline — a {@value
 * ElwhaSideSheet#DETACHED_MARGIN_PX}px margin appears on all four sides, all four corners round
 * (16px), and the edge divider disappears (a rounded float reads as a panel without it) — and that
 * the open/close squash and the edge / RTL resolution still hold in the detached posture (the float
 * mirrors to the leading edge under RTL).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetDetachedDemo {

  private SideSheetDetachedDemo() {}

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
    SwingUtilities.invokeLater(SideSheetDetachedDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet V2 — S1 detached standard posture (#508)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setSheetPosture(SheetPosture.DETACHED);
    sheet.setContent(filters());
    sheet.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.textButton("Reset"));

    final JPanel stage = new JPanel(new BorderLayout());
    stage.add(main(), BorderLayout.CENTER);
    stage.add(sheet, BorderLayout.LINE_END);

    final ElwhaButton posture = ElwhaButton.filledTonalButton("Posture: DETACHED");
    posture.addActionListener(
        e -> {
          final boolean detached = sheet.getSheetPosture() == SheetPosture.DETACHED;
          sheet.setSheetPosture(detached ? SheetPosture.DOCKED : SheetPosture.DETACHED);
          posture.setText("Posture: " + sheet.getSheetPosture());
          stage.revalidate();
          stage.repaint();
        });

    final ElwhaButton toggle = ElwhaButton.outlinedButton("Open / close");
    toggle.addActionListener(e -> sheet.setOpen(!sheet.isOpen()));

    final ElwhaButton edge = ElwhaButton.outlinedButton("Edge: TRAILING");
    edge.addActionListener(
        e -> {
          final SheetEdge next =
              sheet.getSheetEdge() == SheetEdge.TRAILING ? SheetEdge.LEADING : SheetEdge.TRAILING;
          sheet.setSheetEdge(next);
          stage.remove(sheet);
          stage.add(
              sheet, next == SheetEdge.TRAILING ? BorderLayout.LINE_END : BorderLayout.LINE_START);
          edge.setText("Edge: " + next);
          stage.revalidate();
          stage.repaint();
        });

    final ElwhaButton rtl = ElwhaButton.textButton("RTL");
    rtl.addActionListener(
        e -> {
          final boolean toRtl = stage.getComponentOrientation().isLeftToRight();
          stage.applyComponentOrientation(
              toRtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
          rtl.setText(toRtl ? "LTR" : "RTL");
          stage.revalidate();
          stage.repaint();
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(posture);
    controls.add(toggle);
    controls.add(edge);
    controls.add(rtl);

    frame.setLayout(new BorderLayout());
    frame.add(stage, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(960, 560);
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
      final JLabel label = new JLabel("Reflowing content " + i, SwingConstants.CENTER);
      label.setFont(TypeRole.BODY_LARGE.resolve());
      label.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      label.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
      main.add(label);
    }
    return main;
  }
}
