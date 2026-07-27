package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * V2 S5 visual smoke for the popover (#501): trigger buttons in every interesting position — top
 * (opens below), bottom (flips above), trailing edge (shifts inside), plus an RTL frame toggle.
 * Verify live commits drive the trigger swatch + readout while the popover is open, Esc / outside
 * press / a second trigger click light-dismiss without reverting, the dismiss callback counts once
 * per show, and the eyedropper inside the popover does <em>not</em> dismiss it (the sampler latch).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerPopoverDemo {

  private ElwhaColorPickerPopoverDemo() {}

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
    SwingUtilities.invokeLater(ElwhaColorPickerPopoverDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaColorPicker — V2 S5 popover (#501)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    popover.setInitialColor(new Color(0x00897B));
    popover.setEyedropperEnabled(true);

    final JLabel readout = new JLabel("Live: " + hex(popover.getColor()) + " · dismissals: 0");
    final int[] dismissals = {0};
    popover.addChangeListener(
        e ->
            readout.setText(
                "Live: " + hex(popover.getColor()) + " · dismissals: " + dismissals[0]));
    popover.onDismiss(
        () -> {
          dismissals[0]++;
          readout.setText("Live: " + hex(popover.getColor()) + " · dismissals: " + dismissals[0]);
        });

    final ElwhaButton top = ElwhaButton.filledTonalButton("Pick (opens below)");
    top.addActionListener(e -> popover.show(top));
    final ElwhaButton bottom = ElwhaButton.filledTonalButton("Pick near bottom (flips above)");
    bottom.addActionListener(e -> popover.show(bottom));
    final ElwhaButton trailing = ElwhaButton.filledTonalButton("Trailing (shifts inside)");
    trailing.addActionListener(e -> popover.show(trailing));
    final ElwhaButton rtl = ElwhaButton.outlinedButton("Toggle RTL");
    rtl.addActionListener(
        e ->
            frame.applyComponentOrientation(
                frame.getComponentOrientation().isLeftToRight()
                    ? ComponentOrientation.RIGHT_TO_LEFT
                    : ComponentOrientation.LEFT_TO_RIGHT));

    final JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    north.add(top);
    north.add(rtl);
    north.add(readout);

    final JPanel south = new JPanel(new BorderLayout());
    south.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
    south.add(bottom, BorderLayout.LINE_START);
    south.add(trailing, BorderLayout.LINE_END);

    frame.setLayout(new BorderLayout());
    frame.add(north, BorderLayout.NORTH);
    frame.add(south, BorderLayout.SOUTH);
    frame.setSize(880, 700);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static String hex(final Color color) {
    return ColorHex.format(color, false);
  }
}
