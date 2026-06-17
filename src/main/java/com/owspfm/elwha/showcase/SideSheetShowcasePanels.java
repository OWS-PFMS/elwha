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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaSideSheet} (story #466). The Workbench switcher
 * carries two presentation facets so it is unambiguous which controls drive which presentation. The
 * {@code Component} segment stages a live <em>standard</em> sheet docked beside reflowing content,
 * with an open/close toggle plus edge / width / affordance / footer / divider / RTL controls
 * applied live. The {@code Modal} facet is a self-contained editor for the <em>modal</em>
 * presentation — its own edge / width / affordance / footer configuration and the modal-only
 * dismissal toggles — whose trigger presents the sheet on the real Showcase frame and echoes each
 * {@code SheetDismissCause} to the status line; the facet's width control re-docks an open modal in
 * place (200 forces the footer-action wrap). The Gallery embeds static configurations directly —
 * the sheet is an ordinary component, so no preview shim is needed; modal-chrome instances render
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

  /**
   * Builds the interactive Workbench: the {@code Component} segment configures the live docked
   * <em>standard</em> sheet; a {@code Modal} facet (added by {@link #buildModalFacet}) is a
   * self-contained editor for the scrim-presented sheet.
   */
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

    // A checkbox advertises persistent state, but the header close affordance is a second close
    // path it never hears about, so it desyncs (#506). A button carries no contradictory state:
    // toggling off the live isOpen() always does the right thing, and the close animation's resize
    // lets us re-label after an in-sheet X close.
    final ElwhaButton openToggle =
        ElwhaButton.filledTonalButton(sheet.isOpen() ? "Close sheet" : "Open sheet");
    openToggle.addActionListener(e -> sheet.setOpen(!sheet.isOpen()));
    sheet.addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            openToggle.setText(sheet.isOpen() ? "Close sheet" : "Open sheet");
          }
        });

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

    final ElwhaSelectField<Integer> widthBox = ElwhaSelectField.outlined("Width");
    widthBox.setOptions(List.of(200, 256, 320, 400));
    widthBox.setSelectedValue(256);
    widthBox.addSelectionChangeListener(w -> sheet.setSheetWidth(w != null ? w : 256));

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

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Standard side sheet");
    controls.addControl("", openToggle);
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

    workbench.setStage(stage);
    workbench.setCode(
        """
        ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Filters");
        sheet.setContent(filterForm);
        sheet.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.outlinedButton("Cancel"));
        frame.add(sheet, BorderLayout.LINE_END);   // embed beside content

        sheet.open();    // animate width 0 -> sheetWidth; siblings reflow
        sheet.close();   // reverse; the sheet stays in the hierarchy
        """);

    buildModalFacet(workbench, stage);
    return workbench;
  }

  // The Modal facet is a self-contained editor for the scrim-presented sheet, kept separate
  // from the standard controls so it is clear which presentation each control drives (a smoke
  // finding). It owns its edge / width / affordance / footer config plus the modal-only
  // dismissal toggles; the trigger presents the sheet over the shared stage and echoes the
  // SheetDismissCause. One live modal at a time, held so the width control can re-dock it while
  // shown; cleared on close.
  private static void buildModalFacet(final ComponentWorkbench workbench, final JPanel stage) {
    final WorkbenchControls controls = new WorkbenchControls();
    final ElwhaSideSheet[] liveModal = new ElwhaSideSheet[1];

    final ElwhaSelectField<SheetEdge> edgeBox = ElwhaSelectField.outlined("Edge");
    edgeBox.setOptions(List.of(SheetEdge.values()));
    edgeBox.setSelectedValue(SheetEdge.TRAILING);

    // 200 forces the footer-action wrap; widening un-wraps. Live on an open modal.
    final ElwhaSelectField<Integer> widthBox = ElwhaSelectField.outlined("Width");
    widthBox.setOptions(List.of(200, 256, 320, 400));
    widthBox.setSelectedValue(256);
    widthBox.addSelectionChangeListener(
        w -> {
          if (liveModal[0] != null) {
            liveModal[0].setSheetWidth(w != null ? w : 256);
          }
        });

    final ElwhaCheckbox closeBox = new ElwhaCheckbox("Close affordance");
    closeBox.setChecked(true);
    final ElwhaCheckbox backBox = new ElwhaCheckbox("Back affordance");

    final ElwhaSelectField<Integer> actionsBox = ElwhaSelectField.outlined("Actions");
    actionsBox.setOptions(List.of(0, 1, 2));
    actionsBox.setSelectedValue(2);

    final ElwhaCheckbox footerDividerBox = new ElwhaCheckbox("Footer divider");
    footerDividerBox.setChecked(true);

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

    controls.addSection("Modal side sheet");
    controls.addControl("", edgeBox);
    controls.addControl("", widthBox);
    controls.addSection("Header");
    controls.addControl("", closeBox);
    controls.addControl("", backBox);
    controls.addSection("Footer & dividers");
    controls.addControl("", actionsBox);
    controls.addControl("", footerDividerBox);
    controls.addSection("Dismissal");
    controls.addControl("", escBox);
    controls.addControl("", scrimBox);
    controls.addSection("Present");
    controls.addControl("", openModal);
    controls.addControl("", status);

    final ComponentWorkbench.Facet facet = workbench.addFacet("Modal", controls);
    facet.setCode(
        """
        ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Filters");
        modal.setContent(filterForm);
        modal.setActions(ElwhaButton.filledButton("Apply"), ElwhaButton.outlinedButton("Cancel"));
        modal.setDismissibleByEsc(true);
        modal.setDismissibleByScrim(true);
        modal.setOnClose(cause -> handle(cause)); // SheetDismissCause
        modal.showModal(frame);                   // scrim + slide-in over the host frame
        """);
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
