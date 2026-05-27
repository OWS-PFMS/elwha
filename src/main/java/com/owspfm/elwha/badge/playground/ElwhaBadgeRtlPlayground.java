package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #213 (S4) smoketest — {@link ElwhaBadgeAnchor} RTL mirroring via {@link
 * ComponentOrientation}. Two side-by-side host stages: one with default LTR orientation, one with
 * RTL forced on its root container so {@code ComponentOrientation} propagates down to the host
 * naturally. A live orientation toggle on a third host reads the {@code componentOrientation}
 * property-change event the anchor listens for, exercising the dynamic-flip path.
 *
 * <p>The Large badge uses {@code "999+"} content so the mirror is visually unambiguous — the pill
 * sits at the icon's upper-right in LTR and upper-left in RTL with the same offsets.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.badge.playground.ElwhaBadgeRtlPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaBadgeRtlPlayground {

  private final JFrame frame = new JFrame("ElwhaBadge — S4 RTL (#213)");

  private final ElwhaIconButton liveHost = new ElwhaIconButton(MaterialIcons.edit());

  private ElwhaBadgeRtlPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaBadgeRtlPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildStageRow(), BorderLayout.CENTER);
    frame.add(buildLiveToggle(), BorderLayout.SOUTH);
    frame.setSize(880, 460);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    ElwhaBadgeAnchor.attach(liveHost, ElwhaBadge.large("999+"));
  }

  private JPanel buildStageRow() {
    final JPanel row = new JPanel(new GridBagLayout());
    row.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 24, 8, 24);
    gc.anchor = GridBagConstraints.CENTER;

    final JPanel ltrStage = buildStage("LTR", ComponentOrientation.LEFT_TO_RIGHT);
    final JPanel rtlStage = buildStage("RTL", ComponentOrientation.RIGHT_TO_LEFT);
    final JPanel liveStage = buildLiveStage();

    gc.gridx = 0;
    row.add(ltrStage, gc);
    gc.gridx = 1;
    row.add(rtlStage, gc);
    gc.gridx = 2;
    row.add(liveStage, gc);
    return row;
  }

  private JPanel buildStage(final String label, final ComponentOrientation orientation) {
    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setBorder(BorderFactory.createTitledBorder(label + " — static orientation"));
    stage.setComponentOrientation(orientation);
    final ElwhaIconButton host = new ElwhaIconButton(MaterialIcons.edit());
    host.setComponentOrientation(orientation);
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(32, 32, 32, 32);
    stage.add(host, gc);
    ElwhaBadgeAnchor.attach(host, ElwhaBadge.large("999+"));
    return stage;
  }

  private JPanel buildLiveStage() {
    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setBorder(BorderFactory.createTitledBorder("Live — toggle below"));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(32, 32, 32, 32);
    stage.add(liveHost, gc);
    return stage;
  }

  private JPanel buildLiveToggle() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
    bar.add(new JLabel("Live host orientation:"));
    final ButtonGroup group = new ButtonGroup();
    final JToggleButton ltr = new JToggleButton("LTR", true);
    final JToggleButton rtl = new JToggleButton("RTL");
    ltr.addActionListener(
        e -> liveHost.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT));
    rtl.addActionListener(
        e -> liveHost.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT));
    group.add(ltr);
    group.add(rtl);
    bar.add(ltr);
    bar.add(rtl);
    return bar;
  }

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    bar.add(new JLabel("Mode:"));
    final ButtonGroup group = new ButtonGroup();
    for (Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK, Mode.SYSTEM}) {
      final JToggleButton button = new JToggleButton(mode.name());
      button.addActionListener(e -> applyMode(mode));
      if (ElwhaTheme.current().mode() == mode) {
        button.setSelected(true);
      }
      group.add(button);
      bar.add(button);
    }
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
