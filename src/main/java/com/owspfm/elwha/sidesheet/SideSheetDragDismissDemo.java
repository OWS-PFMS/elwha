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
 * S3 visual smoke for drag-to-dismiss (#510): an embedded standard sheet and a modal trigger, both
 * with {@code setDragToDismissEnabled(true)}. Grab the <em>headline</em> (move cursor) and drag it
 * toward the anchored edge — the standard sheet narrows 1:1 with the pointer (the coplanar squash)
 * and the modal slides while its scrim dims. Release past the halfway mark to dismiss (modal echoes
 * {@code SheetDismissCause.DRAG}); release short of it to settle back. Detached variants drag the
 * same way. The "Reopen" button restores the standard sheet after a drag-close.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetDragDismissDemo {

  private SideSheetDragDismissDemo() {}

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
    SwingUtilities.invokeLater(SideSheetDragDismissDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet V2 — S3 drag-to-dismiss (#510)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaSideSheet standard = ElwhaSideSheet.standardSheet("Drag me to dismiss");
    standard.setDragToDismissEnabled(true);
    standard.setContent(filters());
    standard.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.textButton("Reset"));

    final JPanel stage = new JPanel(new BorderLayout());
    stage.add(main(), BorderLayout.CENTER);
    stage.add(standard, BorderLayout.LINE_END);

    final ElwhaButton reopen = ElwhaButton.filledTonalButton("Reopen standard");
    reopen.addActionListener(e -> standard.open());

    final ElwhaButton detachedStd = ElwhaButton.textButton("Toggle standard posture");
    detachedStd.addActionListener(
        e -> {
          standard.setSheetPosture(
              standard.getSheetPosture() == SheetPosture.DETACHED
                  ? SheetPosture.DOCKED
                  : SheetPosture.DETACHED);
          stage.revalidate();
          stage.repaint();
        });

    final ElwhaButton openModal = ElwhaButton.filledButton("Open modal (drag-to-dismiss)");
    openModal.addActionListener(
        e -> {
          final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Drag me to dismiss");
          modal.setDragToDismissEnabled(true);
          modal.setContent(filters());
          modal.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.outlinedButton("Cancel"));
          modal.showModal(frame.getContentPane());
        });

    final ElwhaButton openDetachedModal = ElwhaButton.outlinedButton("Open detached modal");
    openDetachedModal.addActionListener(
        e -> {
          final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Drag me to dismiss");
          modal.setSheetPosture(SheetPosture.DETACHED);
          modal.setDragToDismissEnabled(true);
          modal.setContent(filters());
          modal.showModal(frame.getContentPane());
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(reopen);
    controls.add(detachedStd);
    controls.add(openModal);
    controls.add(openDetachedModal);

    frame.setLayout(new BorderLayout());
    frame.add(stage, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(980, 580);
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
      final JLabel cell = new JLabel("Content " + i, SwingConstants.CENTER);
      cell.setFont(TypeRole.BODY_LARGE.resolve());
      cell.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      cell.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
      main.add(cell);
    }
    return main;
  }
}
