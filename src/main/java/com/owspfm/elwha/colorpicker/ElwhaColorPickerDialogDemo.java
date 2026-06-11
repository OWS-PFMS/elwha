package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
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
 * S5 visual smoke for {@link ElwhaColorPickerDialog} (#487): pending-until-OK — drag spectrum
 * colors and watch the tile <em>not</em> change until OK; Cancel/Esc/scrim discard; the confirmed
 * color re-stages on reopen; the static {@code show} convenience; and a second instance restricted
 * to SWATCHES + SLIDERS proving {@code setModes} pass-through.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerDialogDemo {

  private ElwhaColorPickerDialogDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerDialogDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPickerDialog — S5 (#487)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ColorTile tile = new ColorTile(new Color(0x42A5F5));
    final JLabel readout = new JLabel("Confirmed: #42A5F5");

    final ElwhaColorPickerDialog instanceDialog = new ElwhaColorPickerDialog();
    instanceDialog.setTitle("Accent color");
    instanceDialog.setInitialColor(tile.color);
    instanceDialog.onConfirm(
        chosen -> {
          tile.setColor(chosen);
          readout.setText("Confirmed: " + String.format("#%06X", chosen.getRGB() & 0xFFFFFF));
        });
    instanceDialog.onCancel(() -> readout.setText(readout.getText() + " (cancelled)"));

    final ElwhaButton open = ElwhaButton.filledButton("Pick color…");
    open.addActionListener(e -> instanceDialog.show(frame));

    final ElwhaButton openStatic = ElwhaButton.filledTonalButton("Static show(…)");
    openStatic.addActionListener(
        e ->
            ElwhaColorPickerDialog.show(
                frame, "One-shot pick", tile.color, chosen -> tile.setColor(chosen)));

    final ElwhaColorPickerDialog restricted = new ElwhaColorPickerDialog();
    restricted.setTitle("Swatches + sliders only");
    restricted.setModes(PickerMode.SWATCHES, PickerMode.SLIDERS);
    restricted.onConfirm(tile::setColor);
    final ElwhaButton openRestricted = ElwhaButton.outlinedButton("Restricted modes…");
    openRestricted.addActionListener(e -> restricted.show(frame));

    final JPanel content = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 16));
    content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    content.add(tile);
    content.add(open);
    content.add(openStatic);
    content.add(openRestricted);
    content.add(readout);

    frame.setContentPane(content);
    frame.setSize(760, 620);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  /** The confirmed-color tile — only OK may change it. */
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
