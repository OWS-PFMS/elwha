package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S6 visual smoke for the alpha channel (#488): the checkerboard-backed alpha tracks in the
 * SPECTRUM and SLIDERS panes, the 8-digit hex grammar (type {@code #F4433680}), the headline
 * stepping down to medium while alpha is on, the checkerboard preview behind a translucent color,
 * swatch picks preserving the current alpha, and the live tile compositing the picked color over a
 * checkerboard. Flip the switch to watch alpha strip back to opaque.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerAlphaDemo {

  private ElwhaColorPickerAlphaDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerAlphaDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — S6 alpha (#488)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xF4, 0x43, 0x36, 0x80));
    picker.setAlphaEnabled(true);
    picker.setSupportingText("Alpha enabled");

    final AlphaTile tile = new AlphaTile(picker.getColor());
    final JLabel readout = new JLabel("Listener: " + picker.formatCurrentHex());
    picker.addChangeListener(
        e -> {
          tile.setColor(picker.getColor());
          readout.setText(
              "Listener: "
                  + picker.formatCurrentHex()
                  + (picker.isAdjusting() ? " (adjusting)" : ""));
        });

    final ElwhaSwitch alphaToggle = new ElwhaSwitch();
    alphaToggle.setSelected(true);
    alphaToggle.setLabel("Alpha enabled");
    alphaToggle.addActionListener(e -> picker.setAlphaEnabled(alphaToggle.isSelected()));

    final JPanel center = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 16));
    center.add(picker);
    center.add(tile);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
    controls.add(alphaToggle);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(center, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(640, 620);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  /** A tile compositing the picked color over a checkerboard — translucency made visible. */
  private static final class AlphaTile extends JComponent {

    private Color color;

    AlphaTile(final Color color) {
      this.color = color;
    }

    void setColor(final Color color) {
      this.color = color;
      repaint();
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(96, 96);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        Checkerboard.fill(g2, new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        g2.setColor(color);
        g2.fillRect(0, 0, getWidth(), getHeight());
      } finally {
        g2.dispose();
      }
    }
  }
}
