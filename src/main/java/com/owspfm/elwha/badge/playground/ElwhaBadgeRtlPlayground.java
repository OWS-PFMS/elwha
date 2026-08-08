package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ElwhaBadgeRtlPlayground {

  private final JFrame frame = new JFrame("ElwhaBadge — S4 RTL (#213)");

  private final ElwhaIconButton liveHost = new ElwhaIconButton(MaterialIcons.favoriteFilled());

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
    final ElwhaIconButton host = new ElwhaIconButton(MaterialIcons.favoriteFilled());
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
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL)
            .add(new ElwhaButton("LTR"))
            .add(new ElwhaButton("RTL"));
    group.setSelectedIndex(0);
    group.addSelectionListener(
        g ->
            liveHost.setComponentOrientation(
                g.getSelectedIndex() == 1
                    ? ComponentOrientation.RIGHT_TO_LEFT
                    : ComponentOrientation.LEFT_TO_RIGHT));
    bar.add(group);
    return bar;
  }

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    bar.add(new JLabel("Mode:"));
    final Mode[] modes = {Mode.LIGHT, Mode.DARK, Mode.SYSTEM};
    final ElwhaButtonGroup group =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL);
    for (final Mode mode : modes) {
      group.add(new ElwhaButton(mode.name()));
    }
    final Mode current = ElwhaTheme.current().mode();
    for (int i = 0; i < modes.length; i++) {
      if (modes[i] == current) {
        group.setSelectedIndex(i);
      }
    }
    // Seeded before the listener is attached so the initial selection does not re-install the
    // theme on startup.
    group.addSelectionListener(g -> applyMode(modes[g.getSelectedIndex()]));
    bar.add(group);
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}
