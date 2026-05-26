package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #191 (S5) smoketest — Extended FAB RTL mirroring + dynamic width + 80 dp minimum + no wrap
 * / no truncation. The {@link ComponentOrientation} toggle flips the icon and label between the LTR
 * (icon leading / label trailing) and RTL (icon trailing / label leading) arrangements design doc
 * §11 calls for. The width column drives label length from a single character through a long
 * marketing phrase so the reviewer can confirm the body grows to fit, never wraps, never truncates
 * — and the "Hi" row shows the 80 dp floor binding for the smallest size when content alone would
 * not reach it. The Standard form (icon-only, symmetric) is rendered in the same matrix as a
 * control — its layout is RTL-invariant so the two columns should look identical.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.fab.playground.ElwhaFabRtlPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFabRtlPlayground {

  private static final String[] LABELS = {
    "Hi", "Compose", "Compose message", "Send a new encrypted message to my team"
  };

  private final JFrame frame = new JFrame("ElwhaFab — S5 RTL + dynamic width (#191)");
  private JPanel matrixHost;
  private ComponentOrientation orientation = ComponentOrientation.LEFT_TO_RIGHT;

  private ElwhaFabRtlPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaFabRtlPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    matrixHost = new JPanel(new BorderLayout());
    matrixHost.add(new JScrollPane(buildMatrix()), BorderLayout.CENTER);
    frame.add(matrixHost, BorderLayout.CENTER);
    frame.add(buildControlBar(), BorderLayout.NORTH);
    frame.setSize(1240, 820);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildMatrix() {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    grid.applyComponentOrientation(orientation);
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(10, 16, 10, 16);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaFab.Size[] sizes = ElwhaFab.Size.values();

    gc.gridy = 0;
    gc.gridx = 0;
    grid.add(boldLabel("Label \\ Size"), gc);
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      grid.add(boldLabel(sizes[col].name() + " · " + sizes[col].containerPx() + " dp"), gc);
    }

    int row = 1;
    for (String label : LABELS) {
      gc.gridy = row;
      gc.gridx = 0;
      gc.anchor = GridBagConstraints.WEST;
      grid.add(new JLabel("\"" + label + "\""), gc);
      gc.anchor = GridBagConstraints.CENTER;
      for (int col = 0; col < sizes.length; col++) {
        gc.gridx = col + 1;
        final ElwhaFab fab =
            ElwhaFab.extended(MaterialIcons.add(sizes[col].iconPx()), label).setFabSize(sizes[col]);
        fab.setComponentOrientation(orientation);
        fab.setToolTipText(
            label + " — " + sizes[col].name() + " · width " + fab.getPreferredSize().width + " px");
        grid.add(fab, gc);
      }
      row++;
    }

    // text-only Extended row — confirms label-only path also mirrors + clamps.
    gc.gridy = row++;
    gc.gridx = 0;
    gc.anchor = GridBagConstraints.WEST;
    grid.add(new JLabel("\"Send\" (text-only)"), gc);
    gc.anchor = GridBagConstraints.CENTER;
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      final ElwhaFab fab = ElwhaFab.extended("Send").setFabSize(sizes[col]);
      fab.setComponentOrientation(orientation);
      grid.add(fab, gc);
    }

    // Standard control row — RTL-invariant; both orientations should render identically.
    gc.gridy = row;
    gc.gridx = 0;
    gc.anchor = GridBagConstraints.WEST;
    grid.add(new JLabel("Standard (control)"), gc);
    gc.anchor = GridBagConstraints.CENTER;
    for (int col = 0; col < sizes.length; col++) {
      gc.gridx = col + 1;
      final ElwhaFab fab =
          ElwhaFab.standard(MaterialIcons.add(sizes[col].iconPx())).setFabSize(sizes[col]);
      fab.setComponentOrientation(orientation);
      fab.setToolTipText("Standard — " + sizes[col].name());
      grid.add(fab, gc);
    }

    return grid;
  }

  private JLabel boldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  private JPanel buildControlBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    bar.add(new JLabel("Orientation:"));
    final ButtonGroup orientGroup = new ButtonGroup();
    final JToggleButton ltrButton = new JToggleButton("LTR");
    ltrButton.setSelected(true);
    ltrButton.addActionListener(e -> applyOrientation(ComponentOrientation.LEFT_TO_RIGHT));
    orientGroup.add(ltrButton);
    bar.add(ltrButton);
    final JToggleButton rtlButton = new JToggleButton("RTL");
    rtlButton.addActionListener(e -> applyOrientation(ComponentOrientation.RIGHT_TO_LEFT));
    orientGroup.add(rtlButton);
    bar.add(rtlButton);

    bar.add(new JLabel("    Mode:"));
    final ButtonGroup modeGroup = new ButtonGroup();
    for (Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK, Mode.SYSTEM}) {
      final JToggleButton button = new JToggleButton(mode.name());
      button.addActionListener(e -> applyMode(mode));
      if (ElwhaTheme.current().mode() == mode) {
        button.setSelected(true);
      }
      modeGroup.add(button);
      bar.add(button);
    }
    return bar;
  }

  private void applyOrientation(final ComponentOrientation next) {
    this.orientation = next;
    matrixHost.removeAll();
    matrixHost.add(new JScrollPane(buildMatrix()), BorderLayout.CENTER);
    matrixHost.revalidate();
    matrixHost.repaint();
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
