package com.owspfm.elwha.appbar;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S5 visual smoke for {@link ElwhaAppBar} a11y, RTL and enabled propagation (#461): the full RTL
 * mirror (nav slot, action order, title alignment, expanded headline margins), bar-level setEnabled
 * cascading to hosted buttons with per-button state restore, the disabled text treatment, the live
 * accessible-name readout, and Tab traversal order (nav → actions).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarA11yRtlDemo {

  private ElwhaAppBarA11yRtlDemo() {}

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
    SwingUtilities.invokeLater(ElwhaAppBarA11yRtlDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — S5 a11y, RTL & enabled (#461)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaAppBar small = ElwhaAppBar.small();
    small.setTitle("Inbox");
    small.setSubtitle("Synced 5 minutes ago");
    small.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    small.addAction(MaterialIcons.favorite(), "Favorite", null);
    final ElwhaIconButton edit = small.addAction(MaterialIcons.edit(), "Edit", null);
    small.addAction(MaterialIcons.moreVert(), "More options", null);

    final ElwhaAppBar large = ElwhaAppBar.largeFlexible();
    large.setTitle("Headline");
    large.setSubtitle("Subtitle");
    large.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    large.addAction(MaterialIcons.moreVert(), "More options", null);

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 18));
    bars.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    bars.add(titled("Small — Tab through: nav → favorite → edit → more", small));
    bars.add(titled("Large flexible — RTL mirrors the expanded headline too", large));

    final JLabel readout = new JLabel();
    final Runnable updateReadout =
        () ->
            readout.setText(
                "accessible name: \"" + small.getAccessibleContext().getAccessibleName() + "\"");
    updateReadout.run();

    final ElwhaButton rtl = ElwhaButton.filledTonalButton("Toggle RTL");
    rtl.addActionListener(
        e -> {
          final ComponentOrientation next =
              frame.getComponentOrientation().isLeftToRight()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          frame.applyComponentOrientation(next);
          frame.revalidate();
          frame.repaint();
        });

    final ElwhaButton enabled = ElwhaButton.filledTonalButton("Toggle bar enabled");
    enabled.addActionListener(e -> small.setEnabled(!small.isEnabled()));

    final ElwhaButton disableEdit = ElwhaButton.outlinedButton("Disable 'Edit' (survives restore)");
    disableEdit.addActionListener(e -> edit.setEnabled(!edit.isEnabled()));

    final ElwhaButton rename = ElwhaButton.outlinedButton("Change title (name updates)");
    rename.addActionListener(
        e -> {
          small.setTitle(small.getTitle().equals("Inbox") ? "Archive" : "Inbox");
          updateReadout.run();
        });

    final ElwhaButton mode = ElwhaButton.outlinedButton("Light / dark");
    mode.addActionListener(
        e -> {
          final Mode next =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(next).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(rtl);
    controls.add(enabled);
    controls.add(disableEdit);
    controls.add(rename);
    controls.add(mode);
    controls.add(readout);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(860, 520);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static JPanel titled(final String label, final ElwhaAppBar bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setOpaque(false);
    panel.add(new JLabel(label), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}
