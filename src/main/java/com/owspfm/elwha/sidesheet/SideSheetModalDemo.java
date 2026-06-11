package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * S3 visual smoke for the modal {@link ElwhaSideSheet} presentation (#464): every dismiss path —
 * Esc, scrim click, close affordance, back affordance, programmatic, footer action calling {@code
 * dismiss()} — with the recorded {@link SheetDismissCause} echoed to the status line and stdout;
 * Esc/scrim dismissibility toggles applied live; initial focus landing in the content field; and
 * the z-band proof: a dialog opened <em>from inside the sheet</em> stacks above it (dialogs 200
 * &gt; sheets 190).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetModalDemo {

  private SideSheetModalDemo() {}

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
    SwingUtilities.invokeLater(SideSheetModalDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaSideSheet — S3 modal dismiss paths (#464)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final JLabel status = new JLabel("No sheet shown yet.");

    final ElwhaSideSheet sheet = ElwhaSideSheet.modalSheet("Edit reach");
    final JPanel form = new JPanel();
    form.setOpaque(false);
    form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
    final JTextField field = new JTextField("initial focus lands here");
    field.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 32));
    form.add(field);
    form.add(Box.createVerticalStrut(16));
    final ElwhaButton dialogFromSheet = ElwhaButton.filledTonalButton("Open dialog above sheet");
    dialogFromSheet.addActionListener(
        e ->
            ElwhaDialog.builder()
                .headline("Above the sheet")
                .supportingText("Dialogs (MODAL_LAYER 200) stack over sheets (OVERLAY_LAYER 190).")
                .confirmAction(ElwhaButton.textButton("OK"))
                .build()
                .show(frame));
    form.add(dialogFromSheet);
    form.add(Box.createVerticalGlue());
    sheet.setContent(form);

    final ElwhaButton apply = ElwhaButton.filledButton("Apply & dismiss");
    apply.setRippleEnabled(false);
    apply.addActionListener(e -> sheet.dismiss());
    final ElwhaButton stay = ElwhaButton.textButton("Stay open");
    stay.addActionListener(e -> status.setText("Footer action ran — sheet stayed open."));
    sheet.setActions(apply, stay);
    sheet.setOnClose(
        cause -> {
          status.setText("Closed: " + cause);
          System.out.println("Closed: " + cause);
        });

    final ElwhaButton openTrailing = ElwhaButton.filledButton("Open trailing sheet");
    openTrailing.addActionListener(
        e -> {
          sheet.setSheetEdge(SheetEdge.TRAILING);
          sheet.showModal(frame);
        });
    final ElwhaButton openLeading = ElwhaButton.outlinedButton("Open leading sheet");
    openLeading.addActionListener(
        e -> {
          sheet.setSheetEdge(SheetEdge.LEADING);
          sheet.showModal(frame);
        });

    final ElwhaCheckbox esc = new ElwhaCheckbox("Esc dismisses");
    esc.setChecked(true);
    esc.addActionListener(e -> sheet.setDismissibleByEsc(esc.isChecked()));
    final ElwhaCheckbox scrim = new ElwhaCheckbox("Scrim dismisses");
    scrim.setChecked(true);
    scrim.addActionListener(e -> sheet.setDismissibleByScrim(scrim.isChecked()));
    final ElwhaCheckbox back = new ElwhaCheckbox("Back affordance");
    back.addActionListener(e -> sheet.setBackAffordanceVisible(back.isChecked()));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(openTrailing);
    controls.add(openLeading);
    controls.add(esc);
    controls.add(scrim);
    controls.add(back);
    controls.add(status);

    final JLabel main = new JLabel("Main content — inert behind the scrim while the sheet is up.");
    main.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

    frame.setLayout(new BorderLayout());
    frame.add(main, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(960, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
