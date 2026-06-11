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
 * S2 visual smoke for the standard {@link ElwhaSideSheet} presentation (#463): a BorderLayout stage
 * — wrapping main content in CENTER, the sheet at LINE_END — with an open/close toggle. Verify the
 * coplanar squash: the main content reflows as the sheet's width animates 0↔256 (300ms emphasized;
 * snaps under reduced motion), the sheet's children keep their open-width layout (clipped, no
 * re-wrap), and the edge divider rides the moving boundary. The width buttons exercise {@code
 * setSheetWidth} live; the close affordance in the header collapses the sheet too.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetStandardDemo {

  private SideSheetStandardDemo() {}

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
    SwingUtilities.invokeLater(SideSheetStandardDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet — S2 standard squash (#463)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
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
    sheet.setContent(filters);
    sheet.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.textButton("Reset"));

    final JPanel main = new JPanel(new GridLayout(0, 2, 16, 16));
    main.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    for (int i = 1; i <= 8; i++) {
      main.add(cell("Reflowing content " + i));
    }

    final ElwhaButton toggle = ElwhaButton.filledTonalButton("Toggle sheet");
    toggle.addActionListener(e -> sheet.setOpen(!sheet.isOpen()));
    final ElwhaButton narrow = ElwhaButton.outlinedButton("Width 256");
    narrow.addActionListener(e -> sheet.setSheetWidth(256));
    final ElwhaButton wide = ElwhaButton.outlinedButton("Width 320");
    wide.addActionListener(e -> sheet.setSheetWidth(320));
    final ElwhaButton divider = ElwhaButton.textButton("Toggle edge divider");
    divider.addActionListener(e -> sheet.setEdgeDividerVisible(!sheet.isEdgeDividerVisible()));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(toggle);
    controls.add(narrow);
    controls.add(wide);
    controls.add(divider);

    frame.setLayout(new BorderLayout());
    frame.add(main, BorderLayout.CENTER);
    frame.add(sheet, BorderLayout.LINE_END);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(960, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JLabel cell(final String text) {
    final JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setFont(TypeRole.BODY_LARGE.resolve());
    label.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    label.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
    return label;
  }
}
