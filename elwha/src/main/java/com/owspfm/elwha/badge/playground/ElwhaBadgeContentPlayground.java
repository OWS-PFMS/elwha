package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Story #211 (S2) smoketest — {@link ElwhaBadge} Large content rendering, dynamic width, and 4-
 * character silent truncation. Top row shows pre-built large badges at the spec'd content values (1
 * / 12 / 999 / 999+ / OVERFLOW); bottom row drives a single live badge via {@link
 * ElwhaBadge#setContent(String)} so the reviewer can see width adapt and overflow truncate as they
 * type.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.badge.playground.ElwhaBadgeContentPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ElwhaBadgeContentPlayground {

  private final JFrame frame = new JFrame("ElwhaBadge — S2 content + dynamic width (#211)");

  private ElwhaBadgeContentPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaBadgeContentPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildContentMatrix(), BorderLayout.CENTER);
    frame.setSize(720, 360);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildContentMatrix() {
    final JPanel root = new JPanel(new BorderLayout(16, 16));
    root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    final JPanel staticRow = new JPanel(new GridBagLayout());
    staticRow.setBorder(
        BorderFactory.createTitledBorder("Static — pre-built badges at spec'd content values"));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 24, 8, 24);
    gc.anchor = GridBagConstraints.CENTER;

    final String[] inputs = {"1", "12", "999", "1234", "999999", "BETA"};
    for (int col = 0; col < inputs.length; col++) {
      final ElwhaBadge badge = ElwhaBadge.large(inputs[col]);
      gc.gridx = col;
      gc.gridy = 0;
      staticRow.add(new JLabel("\"" + inputs[col] + "\""), gc);
      gc.gridy = 1;
      staticRow.add(badge, gc);
      gc.gridy = 2;
      staticRow.add(new JLabel("→ stored: \"" + badge.getContent() + "\""), gc);
    }

    root.add(staticRow, BorderLayout.NORTH);
    root.add(buildLivePanel(), BorderLayout.CENTER);
    return root;
  }

  private JPanel buildLivePanel() {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Live — setContent(...) on a single badge"));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 12, 8, 12);
    gc.anchor = GridBagConstraints.CENTER;

    final ElwhaBadge badge = ElwhaBadge.large("1");
    final ElwhaTextField field = ElwhaTextField.outlined("Content");
    field.setText("1");
    final JLabel storedLabel = new JLabel("stored: \"1\"");

    field
        .getEditor()
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                applyContent();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                applyContent();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                applyContent();
              }

              private void applyContent() {
                final String text = field.getText();
                if (text.isEmpty()) {
                  storedLabel.setText("stored: <rejected — empty>");
                  return;
                }
                badge.setContent(text);
                storedLabel.setText("stored: \"" + badge.getContent() + "\"");
              }
            });

    gc.gridx = 0;
    gc.gridy = 0;
    gc.gridwidth = 2;
    panel.add(field, gc);

    gc.gridy = 1;
    panel.add(badge, gc);

    gc.gridy = 2;
    panel.add(storedLabel, gc);

    return panel;
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
