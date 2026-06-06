package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual spike demo for epic #298 S1 — exercises the menu overlay host ({@link
 * AbstractElwhaMenuOverlay}) by hand. Four triggers prove the host's anatomy-agnostic mechanics
 * before the real {@code ElwhaMenu} container lands in S3:
 *
 * <ul>
 *   <li><strong>Center</strong> — opens below the trigger; click outside / Esc / Tab-away
 *       dismisses.
 *   <li><strong>Near bottom edge</strong> — the menu flips above to avoid clipping.
 *   <li><strong>Near right edge</strong> — the menu shifts left to stay in the window.
 *   <li><strong>From inside a dialog</strong> — opens above the dialog (POPUP_LAYER over
 *       MODAL_LAYER).
 * </ul>
 *
 * <p>The surface is a placeholder slab (two buttons) — S2/S3 supply the real {@code ElwhaMenuItem}
 * list. Throwaway per-story smoke artifact.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuHostSpikeDemo {

  private MenuHostSpikeDemo() {}

  /** A minimal shadowed, rounded menu host — placeholder for the S3 container. */
  private static final class SpikeMenu extends AbstractElwhaMenuOverlay {
    private static final int ELEVATION = 3;
    private static final int ARC = ShapeScale.MD.px();

    SpikeMenu() {
      super(cause -> System.out.println("menu closed: " + cause));
    }

    @Override
    protected JComponent createSurface() {
      final JPanel surfacePanel =
          new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(final Graphics g) {
              final Graphics2D g2 = (Graphics2D) g.create();
              try {
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final Insets pad = ShadowPainter.shadowInsets(ELEVATION);
                final int bw = getWidth() - pad.left - pad.right;
                final int bh = getHeight() - pad.top - pad.bottom;
                final Graphics2D sg = (Graphics2D) g2.create();
                sg.translate(pad.left, pad.top);
                ShadowPainter.paint(sg, bw, bh, ARC * 2, ELEVATION);
                sg.setColor(ColorRole.SURFACE_CONTAINER_LOW.resolve());
                sg.fill(new RoundRectangle2D.Float(0, 0, bw, bh, ARC * 2, ARC * 2));
                sg.dispose();
              } finally {
                g2.dispose();
              }
            }
          };
      surfacePanel.setOpaque(false);
      final Insets pad = ShadowPainter.shadowInsets(ELEVATION);
      final JPanel items = new JPanel();
      items.setOpaque(false);
      items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
      items.setBorder(
          BorderFactory.createEmptyBorder(
              pad.top + 8, pad.left + 8, pad.bottom + 8, pad.right + 8));
      // The placeholder buttons close the host on click, so the spike also demonstrates
      // activation-dismiss (the real ElwhaMenu wires this on its ElwhaMenuItems).
      for (final String label : new String[] {"First action", "Second action", "Third action"}) {
        final ElwhaButton b = ElwhaButton.textButton(label);
        b.addActionListener(e -> close(MenuDismissCause.SELECTION));
        items.add(b);
      }
      surfacePanel.add(items, BorderLayout.CENTER);
      surfacePanel.setPreferredSize(
          new Dimension(220 + pad.left + pad.right, 150 + pad.top + pad.bottom));
      return surfacePanel;
    }

    @Override
    protected String accessibleName() {
      return "Spike menu";
    }
  }

  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(MenuHostSpikeDemo::build);
  }

  private static void build() {
    final JFrame frame = new JFrame("ElwhaMenu S1 — host spike");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(720, 520);
    frame.setLocationRelativeTo(null);

    final JPanel content = new JPanel(new GridBagLayout());
    content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);

    final ElwhaButton center = ElwhaButton.filledButton("Open below (center)");
    center.addActionListener(e -> new SpikeMenu().show(center));
    gc.gridx = 1;
    gc.gridy = 1;
    content.add(center, gc);

    final ElwhaButton bottom = ElwhaButton.filledTonalButton("Flip above ↑");
    bottom.addActionListener(e -> new SpikeMenu().show(bottom));
    gc.gridx = 1;
    gc.gridy = 3;
    gc.anchor = GridBagConstraints.PAGE_END;
    gc.weighty = 1;
    content.add(bottom, gc);

    final ElwhaButton right = ElwhaButton.filledTonalButton("Shift left ←");
    right.addActionListener(e -> new SpikeMenu().show(right));
    gc.gridx = 2;
    gc.gridy = 1;
    gc.anchor = GridBagConstraints.LINE_END;
    gc.weightx = 1;
    gc.weighty = 0;
    content.add(right, gc);

    final ElwhaButton overDialog = ElwhaButton.outlinedButton("Open dialog → menu");
    overDialog.addActionListener(e -> openDialogWithMenu(overDialog));
    gc.gridx = 0;
    gc.gridy = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    gc.weightx = 1;
    content.add(overDialog, gc);

    final JLabel hint =
        new JLabel(
            "<html>S1 host spike: click a trigger to open the placeholder menu. "
                + "Dismiss via outside-click, Esc, or Tab-away.</html>");
    gc.gridx = 0;
    gc.gridy = 0;
    gc.gridwidth = 3;
    gc.weightx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    content.add(hint, gc);

    frame.setContentPane(content);
    frame.setVisible(true);
  }

  // Opens a real ElwhaDialog whose body holds a trigger; the menu opened from it must render above
  // the dialog (POPUP_LAYER 300 > MODAL_LAYER 200).
  private static void openDialogWithMenu(final JComponent parent) {
    final ElwhaButton inDialogTrigger = ElwhaButton.filledButton("Open menu (over this dialog)");
    final JPanel body = new JPanel(new BorderLayout());
    body.setOpaque(false);
    body.add(inDialogTrigger, BorderLayout.CENTER);
    inDialogTrigger.addActionListener(e -> new SpikeMenu().show(inDialogTrigger));
    ElwhaDialog.builder()
        .headline("Dialog with a menu")
        .supportingText("The menu opened from the button below tops this dialog.")
        .content(body)
        .confirmAction(ElwhaButton.textButton("Done"))
        .build()
        .show(parent);
  }
}
