package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.sidesheet.ElwhaSideSheet;
import com.owspfm.elwha.sidesheet.SheetEdge;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaSideSheet} (story #466). The Workbench stages a
 * live <em>standard</em> sheet docked beside reflowing placeholder content — the embeddable half of
 * the component demos itself on the stage, with Open / Edge / Width / affordance / footer / divider
 * / RTL controls applied live — plus an "Open modal side sheet" trigger that presents the
 * <em>modal</em> half on the real Showcase frame with the same configuration (the dialog-leaf
 * trigger pattern), echoing each {@code SheetDismissCause} to the status line. The Width control is
 * live on both halves: it reflows the staged standard sheet and re-docks an open modal in place
 * (200 forces the footer-action wrap). The Gallery embeds static configurations directly — the
 * sheet is an ordinary component, so no preview shim is needed; modal-chrome instances render
 * without scrim or motion.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class SideSheetShowcasePanels {

  private static final String[] FILTER_ROWS = {
    "Watershed", "Reach", "Species", "Year range", "Survey type", "Gear"
  };

  private SideSheetShowcasePanels() {}

  /** Builds the interactive Workbench (live docked sheet + reflowing stage + modal trigger). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
    sheet.setContent(sheetContent());
    sheet.setActions(footerActions(2));

    final JPanel stage = new JPanel(new BorderLayout());
    stage.setOpaque(false);
    stage.add(mainContent(), BorderLayout.CENTER);
    stage.add(sheet, BorderLayout.LINE_END);
    stage.setPreferredSize(new Dimension(820, 480));

    final ElwhaCheckbox openBox = new ElwhaCheckbox("Open");
    openBox.setChecked(true);
    openBox.addActionListener(e -> sheet.setOpen(openBox.isChecked()));

    final ElwhaSelectField<SheetEdge> edgeBox = ElwhaSelectField.outlined("Edge");
    edgeBox.setOptions(List.of(SheetEdge.values()));
    edgeBox.setSelectedValue(SheetEdge.TRAILING);
    edgeBox.addSelectionChangeListener(
        edge -> {
          final SheetEdge next = edge != null ? edge : SheetEdge.TRAILING;
          sheet.setSheetEdge(next);
          stage.remove(sheet);
          stage.add(
              sheet, next == SheetEdge.TRAILING ? BorderLayout.LINE_END : BorderLayout.LINE_START);
          stage.revalidate();
          stage.repaint();
        });

    // One live modal at a time; held so the width control can re-dock it while shown (the
    // ModalDemo behavior). Cleared on close.
    final ElwhaSideSheet[] liveModal = new ElwhaSideSheet[1];

    // Live width: applies to the staged standard sheet AND re-docks an open modal in place.
    // 200 forces the footer-action wrap; widening un-wraps.
    final ElwhaSelectField<Integer> widthBox = ElwhaSelectField.outlined("Width");
    widthBox.setOptions(List.of(200, 256, 320, 400));
    widthBox.setSelectedValue(256);
    widthBox.addSelectionChangeListener(
        w -> {
          final int width = w != null ? w : 256;
          sheet.setSheetWidth(width);
          if (liveModal[0] != null) {
            liveModal[0].setSheetWidth(width);
          }
        });

    final ElwhaCheckbox closeBox = new ElwhaCheckbox("Close affordance");
    closeBox.setChecked(true);
    closeBox.addActionListener(e -> sheet.setCloseAffordanceVisible(closeBox.isChecked()));
    final ElwhaCheckbox backBox = new ElwhaCheckbox("Back affordance");
    backBox.addActionListener(e -> sheet.setBackAffordanceVisible(backBox.isChecked()));

    final ElwhaSelectField<Integer> actionsBox = ElwhaSelectField.outlined("Actions");
    actionsBox.setOptions(List.of(0, 1, 2));
    actionsBox.setSelectedValue(2);
    actionsBox.addSelectionChangeListener(n -> sheet.setActions(footerActions(n != null ? n : 0)));

    final ElwhaCheckbox footerDividerBox = new ElwhaCheckbox("Footer divider");
    footerDividerBox.setChecked(true);
    footerDividerBox.addActionListener(
        e -> sheet.setFooterDividerVisible(footerDividerBox.isChecked()));
    final ElwhaCheckbox edgeDividerBox = new ElwhaCheckbox("Edge divider");
    edgeDividerBox.setChecked(true);
    edgeDividerBox.addActionListener(e -> sheet.setEdgeDividerVisible(edgeDividerBox.isChecked()));

    final ElwhaCheckbox rtlBox = new ElwhaCheckbox("Right-to-left");
    rtlBox.addActionListener(
        e -> {
          stage.applyComponentOrientation(
              rtlBox.isChecked()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          stage.revalidate();
          stage.repaint();
        });

    final ElwhaCheckbox escBox = new ElwhaCheckbox("Esc dismisses");
    escBox.setChecked(true);
    final ElwhaCheckbox scrimBox = new ElwhaCheckbox("Scrim dismisses");
    scrimBox.setChecked(true);
    final JLabel status = new JLabel("Modal not opened yet.");

    final ElwhaButton openModal = ElwhaButton.filledButton("Open modal side sheet");
    openModal.addActionListener(
        e -> {
          final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Filters");
          modal.setContent(sheetContent());
          modal.setActions(footerActions(orDefault(actionsBox.getSelectedValue(), 2)));
          modal.setSheetEdge(orDefault(edgeBox.getSelectedValue(), SheetEdge.TRAILING));
          modal.setSheetWidth(orDefault(widthBox.getSelectedValue(), 256));
          modal.setCloseAffordanceVisible(closeBox.isChecked());
          modal.setBackAffordanceVisible(backBox.isChecked());
          modal.setFooterDividerVisible(footerDividerBox.isChecked());
          modal.setDismissibleByEsc(escBox.isChecked());
          modal.setDismissibleByScrim(scrimBox.isChecked());
          modal.setOnClose(
              cause -> {
                liveModal[0] = null;
                status.setText("Modal closed: " + cause);
              });
          liveModal[0] = modal;
          modal.showModal(stage);
          status.setText("Modal open…");
        });

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Side sheet");
    controls.addControl("", openBox);
    controls.addControl("", edgeBox);
    controls.addControl("", widthBox);
    controls.addSection("Header");
    controls.addControl("", closeBox);
    controls.addControl("", backBox);
    controls.addSection("Footer & dividers");
    controls.addControl("", actionsBox);
    controls.addControl("", footerDividerBox);
    controls.addControl("", edgeDividerBox);
    controls.addSection("Behavior");
    controls.addControl("", rtlBox);
    controls.addSection("Modal");
    controls.addControl("", escBox);
    controls.addControl("", scrimBox);
    controls.addControl("", openModal);
    controls.addControl("", status);

    workbench.setStage(stage);
    workbench.setCode(
        """
        ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
        sheet.setContent(filterForm);
        sheet.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.outlinedButton("Cancel"));
        frame.add(sheet, BorderLayout.LINE_END);   // standard: embed + open()/close()

        ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Filters");
        modal.setContent(filterForm);
        modal.setOnClose(cause -> handle(cause)); // SheetDismissCause
        modal.showModal(frame);                   // modal: scrim + slide-in
        """);
    return workbench;
  }

  /** Builds the static configuration gallery — direct embeds, no live overlays. */
  static JComponent buildGallery() {
    final JPanel row = new JPanel(new GridLayout(1, 0, 24, 0));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final ElwhaSideSheet standard = ElwhaSideSheet.standardSheet("Standard");
    standard.setContent(galleryFiller("SURFACE · square · edge divider"));
    row.add(galleryCell("Standard (docked)", standard));

    final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Modal chrome");
    modal.setContent(galleryFiller("SURFACE_CONTAINER_LOW · 16px inner corners · no shadow"));
    modal.setActions(ElwhaButton.filledButton("Save"), ElwhaButton.outlinedButton("Cancel"));
    row.add(galleryCell("Modal chrome (static)", modal));

    final ElwhaSideSheet leading = ElwhaSideSheet.modalSheet("Leading edge");
    leading.setSheetEdge(SheetEdge.LEADING);
    leading.setContent(galleryFiller("corners round on the opposite side"));
    leading.setActions(ElwhaButton.filledButton("Apply"));
    row.add(galleryCell("Leading edge", leading));

    final ElwhaSideSheet backFlow = ElwhaSideSheet.modalSheet("Step 2 of 3");
    backFlow.setBackAffordanceVisible(true);
    backFlow.setContent(galleryFiller("back affordance for multi-step flows"));
    row.add(galleryCell("Back affordance", backFlow));

    final ElwhaSideSheet bare = ElwhaSideSheet.standardSheet("Bare");
    bare.setCloseAffordanceVisible(false);
    bare.setEdgeDividerVisible(false);
    bare.setContent(galleryFiller("no affordances, no dividers"));
    row.add(galleryCell("Headline only", bare));

    return row;
  }

  private static JComponent sheetContent() {
    final JPanel content = new JPanel();
    content.setOpaque(false);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    for (final String label : FILTER_ROWS) {
      final ElwhaCheckbox rowBox = new ElwhaCheckbox(label);
      rowBox.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      content.add(rowBox);
      content.add(Box.createVerticalStrut(8));
    }
    content.add(Box.createVerticalGlue());
    return content;
  }

  private static JComponent mainContent() {
    final JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
    grid.setOpaque(false);
    grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    for (int i = 1; i <= 6; i++) {
      final JLabel cell = new JLabel("Reflowing content " + i, SwingConstants.CENTER);
      cell.setFont(TypeRole.BODY_LARGE.resolve());
      cell.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      cell.setBorder(BorderFactory.createLineBorder(ColorRole.OUTLINE_VARIANT.resolve()));
      grid.add(cell);
    }
    return grid;
  }

  private static ElwhaButton[] footerActions(final int count) {
    // Filled confirm + outlined dismiss — the action pairing the spec renders show.
    return switch (count) {
      case 1 -> new ElwhaButton[] {ElwhaButton.filledButton("Apply")};
      case 2 ->
          new ElwhaButton[] {
            ElwhaButton.filledButton("Apply"), ElwhaButton.outlinedButton("Cancel")
          };
      default -> new ElwhaButton[0];
    };
  }

  private static JComponent galleryCell(final String caption, final ElwhaSideSheet sheet) {
    final JPanel cell = new JPanel(new BorderLayout(0, 8));
    cell.setOpaque(false);
    final JLabel title = new JLabel(caption);
    title.setFont(TypeRole.LABEL_LARGE.resolve());
    cell.add(title, BorderLayout.NORTH);
    final JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    host.add(sheet, BorderLayout.CENTER);
    host.setPreferredSize(new Dimension(270, 420));
    cell.add(host, BorderLayout.CENTER);
    return cell;
  }

  private static JLabel galleryFiller(final String text) {
    final JLabel label = new JLabel("<html><div style='width:170px'>" + text + "</div></html>");
    label.setVerticalAlignment(SwingConstants.TOP);
    return label;
  }

  private static <T> T orDefault(final T value, final T fallback) {
    return value != null ? value : fallback;
  }
}
