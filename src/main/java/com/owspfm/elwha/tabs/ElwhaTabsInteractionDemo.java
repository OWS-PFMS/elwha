package com.owspfm.elwha.tabs;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * S2 visual smoke for {@link ElwhaTabs} interaction (#427): live click activation with an event
 * log separating bar {@code ChangeListener} events from per-tab {@code ActionListener} events
 * (programmatic activation logs change-only — verify in the log!), hover/press layers and ripple
 * under real pointer input, the primary inactive-pressed→PRIMARY layer quirk on a forced-state
 * row, and a whole-bar enabled switch.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsInteractionDemo {

  private ElwhaTabsInteractionDemo() {}

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
    SwingUtilities.invokeLater(ElwhaTabsInteractionDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTabs — S2 interaction (#427)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final JTextArea log = new JTextArea(8, 60);
    log.setEditable(false);
    log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    final ElwhaTabs primary = ElwhaTabs.primary();
    final ElwhaTabs secondary = ElwhaTabs.secondary();
    for (String label : new String[] {"Video", "Photos", "Audio", "Notes"}) {
      wireLogging(primary.addTab(label), log, "primary");
      wireLogging(secondary.addTab(label), log, "secondary");
    }
    primary.addChangeListener(
        e -> append(log, "primary   change  -> active=" + primary.getActiveTabIndex()));
    secondary.addChangeListener(
        e -> append(log, "secondary change  -> active=" + secondary.getActiveTabIndex()));

    final ElwhaTabs forced = ElwhaTabs.primary();
    forced.addTab("Active");
    forced.addTab("Hovered").setHovered(true);
    forced.addTab("Pressed (PRIMARY quirk)").setPressed(true);
    forced.addTab("Rest");

    final JPanel bars = new JPanel(new GridLayout(0, 1, 0, 18));
    bars.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    bars.add(titled("Primary — click around; click the active tab (no-op)", primary));
    bars.add(titled("Secondary — layers tint ON_SURFACE in every state", secondary));
    bars.add(titled("Forced states (gallery hooks) — inactive pressed flashes PRIMARY", forced));

    final ElwhaButton programmatic = ElwhaButton.filledTonalButton("Programmatic next (top bar)");
    programmatic.addActionListener(
        e -> primary.setActiveTabIndex((primary.getActiveTabIndex() + 1) % primary.getTabCount()));

    final ElwhaSwitch enabled = new ElwhaSwitch();
    enabled.setSelected(true);
    enabled.setLabel("Bars enabled");
    enabled.addActionListener(
        e -> {
          primary.setEnabled(enabled.isSelected());
          secondary.setEnabled(enabled.isSelected());
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(programmatic);
    controls.add(new JLabel("Enabled:"));
    controls.add(enabled);

    final JPanel south = new JPanel(new BorderLayout());
    south.add(controls, BorderLayout.NORTH);
    south.add(new JScrollPane(log), BorderLayout.CENTER);

    frame.setLayout(new BorderLayout());
    frame.add(bars, BorderLayout.CENTER);
    frame.add(south, BorderLayout.SOUTH);
    frame.setSize(760, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static void wireLogging(final ElwhaTab tab, final JTextArea log, final String barName) {
    tab.addActionListener(
        e -> append(log, barName + "   action  -> user activated \"" + tab.getLabel() + "\""));
  }

  private static void append(final JTextArea log, final String line) {
    log.append(line + "\n");
    log.setCaretPosition(log.getDocument().getLength());
  }

  private static JPanel titled(final String title, final ElwhaTabs bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 6));
    panel.setOpaque(false);
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}
