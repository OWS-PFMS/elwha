package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S9 visual smoke for the full-screen presentation (#494): one {@link ElwhaColorPickerDialog}
 * instance presented both ways — the modal basic dialog and {@code showFullScreen} (top app bar
 * with leading ✕ and trailing <em>Save</em>, content column pinned to the picker width). Verify the
 * shared pending-until-OK semantics (Save confirms + re-stages; ✕/Esc discard), the presentations
 * guarding each other, and the alpha pass-through.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerFullScreenDemo {

  private ElwhaColorPickerFullScreenDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerFullScreenDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPickerDialog — S9 full-screen (#494)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ColorTile tile = new ColorTile(new Color(0x00897B));
    final JLabel readout = new JLabel("Confirmed: #00897B");

    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    dialog.setTitle("Select color");
    dialog.setInitialColor(tile.color);
    dialog.onConfirm(
        chosen -> {
          tile.setColor(chosen);
          readout.setText("Confirmed: " + String.format("#%06X", chosen.getRGB() & 0xFFFFFF));
        });
    dialog.onCancel(() -> readout.setText(readout.getText() + " (discarded)"));

    final ElwhaButton fullScreen = ElwhaButton.filledButton("Open full-screen picker");
    fullScreen.addActionListener(e -> dialog.showFullScreen(frame));

    final ElwhaButton modal = ElwhaButton.filledTonalButton("Open modal picker (same instance)");
    modal.addActionListener(e -> dialog.show(frame));

    final ElwhaSwitch alpha = new ElwhaSwitch();
    alpha.setLabel("Alpha");
    alpha.addActionListener(e -> dialog.setAlphaEnabled(alpha.isSelected()));

    final JPanel content = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 16));
    content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    content.add(tile);
    content.add(fullScreen);
    content.add(modal);
    content.add(alpha);
    content.add(readout);

    frame.setContentPane(content);
    frame.setSize(960, 720);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  /** The confirmed-color tile — only Save/OK may change it. */
  private static final class ColorTile extends JComponent {

    private Color color;

    ColorTile(final Color color) {
      this.color = color;
    }

    void setColor(final Color color) {
      this.color = color;
      repaint();
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(72, 72);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final int arc = ShapeScale.MD.px();
        g2.setColor(color);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
      } finally {
        g2.dispose();
      }
    }
  }
}
