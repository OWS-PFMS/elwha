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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Story #212 (S3) smoketest — {@link ElwhaBadgeAnchor} attaches an {@link ElwhaBadge} to an {@link
 * ElwhaIconButton} (the first {@link com.owspfm.elwha.badge.IconBearing} host). Interactive
 * controls: variant toggle (Small / Large), content text field, detach / re-attach buttons.
 *
 * <p>The host is wrapped in a flexible-size panel so the reviewer can resize the window and watch
 * the anchor track — verifying that the {@code HierarchyBoundsListener} + {@code ComponentListener}
 * wiring keeps the badge glued to the upper-trailing corner of the icon as everything moves.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.badge.playground.ElwhaBadgeAnchorPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaBadgeAnchorPlayground {

  private final JFrame frame = new JFrame("ElwhaBadge — S3 anchor (#212)");

  private final ElwhaIconButton host = new ElwhaIconButton(MaterialIcons.favoriteFilled());

  private ElwhaBadge badge = ElwhaBadge.large("3");

  private ElwhaBadgeAnchor.Attachment attachment;

  private ElwhaBadgeAnchorPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaBadgeAnchorPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildHostStage(), BorderLayout.CENTER);
    frame.add(buildControls(), BorderLayout.SOUTH);
    frame.setSize(640, 420);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    attachment = ElwhaBadgeAnchor.attach(host, badge);
  }

  private JPanel buildHostStage() {
    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.CENTER;
    stage.add(host, gc);
    return stage;
  }

  private JPanel buildControls() {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4, 8, 4, 8);
    gc.anchor = GridBagConstraints.WEST;

    final JTextField contentField = new JTextField("3", 8);
    final JLabel storedLabel = new JLabel("stored: \"3\"");

    contentField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                apply();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                apply();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                apply();
              }

              private void apply() {
                if (badge.getVariant() == ElwhaBadge.Variant.SMALL) {
                  return;
                }
                final String text = contentField.getText();
                if (text.isEmpty()) {
                  storedLabel.setText("stored: <rejected — empty>");
                  return;
                }
                badge.setContent(text);
                storedLabel.setText("stored: \"" + badge.getContent() + "\"");
              }
            });

    final JToggleButton smallToggle = new JToggleButton("Small");
    final JToggleButton largeToggle = new JToggleButton("Large", true);
    final ButtonGroup variantGroup = new ButtonGroup();
    variantGroup.add(smallToggle);
    variantGroup.add(largeToggle);
    smallToggle.addActionListener(
        e -> {
          swapBadge(ElwhaBadge.small());
          contentField.setEnabled(false);
          storedLabel.setText("stored: (none — small badge)");
        });
    largeToggle.addActionListener(
        e -> {
          final String seed = contentField.getText().isEmpty() ? "3" : contentField.getText();
          swapBadge(ElwhaBadge.large(seed));
          contentField.setEnabled(true);
          storedLabel.setText("stored: \"" + badge.getContent() + "\"");
        });

    final JButton detachButton = new JButton("Detach");
    detachButton.addActionListener(
        e -> {
          if (attachment != null) {
            ElwhaBadgeAnchor.detach(attachment);
            attachment = null;
          }
        });
    final JButton attachButton = new JButton("Attach");
    attachButton.addActionListener(
        e -> {
          if (attachment == null) {
            attachment = ElwhaBadgeAnchor.attach(host, badge);
          }
        });

    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("Variant:"), gc);
    gc.gridx = 1;
    panel.add(smallToggle, gc);
    gc.gridx = 2;
    panel.add(largeToggle, gc);

    gc.gridx = 0;
    gc.gridy = 1;
    panel.add(new JLabel("Content:"), gc);
    gc.gridx = 1;
    gc.gridwidth = 2;
    panel.add(contentField, gc);
    gc.gridwidth = 1;

    gc.gridx = 3;
    gc.gridy = 1;
    panel.add(storedLabel, gc);

    gc.gridx = 0;
    gc.gridy = 2;
    panel.add(new JLabel("Anchor:"), gc);
    gc.gridx = 1;
    panel.add(detachButton, gc);
    gc.gridx = 2;
    panel.add(attachButton, gc);

    return panel;
  }

  private void swapBadge(final ElwhaBadge replacement) {
    if (attachment != null) {
      ElwhaBadgeAnchor.detach(attachment);
    }
    badge = replacement;
    attachment = ElwhaBadgeAnchor.attach(host, badge);
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
    if (attachment != null) {
      attachment.refresh();
    }
  }
}
