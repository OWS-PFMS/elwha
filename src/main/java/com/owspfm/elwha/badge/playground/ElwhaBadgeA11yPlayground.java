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
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Story #214 (S5) smoketest — push-model accessibility wiring. An {@link ElwhaIconButton} host with
 * {@code accessibleName = "Inbox"} gets an {@link ElwhaBadge} attached via {@link
 * ElwhaBadgeAnchor}; a side-panel inspector listens to the host's {@link AccessibleContext} and
 * displays the spliced {@code accessibleName} live. Interactive controls exercise every property
 * the splice depends on:
 *
 * <ul>
 *   <li>Variant toggle — Small announces {@code "Inbox New notification"}, Large announces {@code
 *       "Inbox {content} new notifications"}.
 *   <li>Content text field — every {@code setContent} call re-derives the default a11y text and the
 *       splice updates immediately.
 *   <li>Override text field + Clear — verifies {@link ElwhaBadge#setAccessibilityText(String)} wins
 *       over the default and that passing {@code null} reverts.
 *   <li>Detach button — verifies {@code host.accessibleName} restores to {@code "Inbox"}.
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.badge.playground.ElwhaBadgeA11yPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class ElwhaBadgeA11yPlayground {

  private static final String HOST_BASE_NAME = "Inbox";

  private final JFrame frame = new JFrame("ElwhaBadge — S5 accessibility (#214)");

  private final ElwhaIconButton host = new ElwhaIconButton(MaterialIcons.favoriteFilled());

  private final JLabel inspectorValue = new JLabel("(detached)");

  private ElwhaBadge badge = ElwhaBadge.large("3");

  private ElwhaBadgeAnchor.Attachment attachment;

  private ElwhaBadgeA11yPlayground() {}

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
    SwingUtilities.invokeLater(() -> new ElwhaBadgeA11yPlayground().launch());
  }

  private void launch() {
    host.getAccessibleContext().setAccessibleName(HOST_BASE_NAME);
    host.getAccessibleContext()
        .addPropertyChangeListener(
            e -> {
              if (AccessibleContext.ACCESSIBLE_NAME_PROPERTY.equals(e.getPropertyName())) {
                inspectorValue.setText(stringify(host.getAccessibleContext().getAccessibleName()));
              }
            });

    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(buildHostStage(), BorderLayout.CENTER);
    frame.add(buildInspector(), BorderLayout.EAST);
    frame.add(buildControls(), BorderLayout.SOUTH);
    frame.setSize(820, 460);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    attachment = ElwhaBadgeAnchor.attach(host, badge);
    inspectorValue.setText(stringify(host.getAccessibleContext().getAccessibleName()));
  }

  private JPanel buildHostStage() {
    final JPanel stage = new JPanel(new GridBagLayout());
    stage.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.CENTER;
    stage.add(host, gc);
    return stage;
  }

  private JPanel buildInspector() {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder("A11y inspector — host.accessibleName"));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 12, 8, 12);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("host base:"), gc);
    gc.gridx = 1;
    panel.add(new JLabel("\"" + HOST_BASE_NAME + "\""), gc);
    gc.gridx = 0;
    gc.gridy = 1;
    panel.add(new JLabel("spliced:"), gc);
    gc.gridx = 1;
    panel.add(inspectorValue, gc);
    return panel;
  }

  private JPanel buildControls() {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4, 8, 4, 8);
    gc.anchor = GridBagConstraints.WEST;

    final ElwhaTextField contentField = ElwhaTextField.outlined("Content");
    contentField.setText("3");
    final ElwhaTextField overrideField = ElwhaTextField.outlined("A11y override");
    final ElwhaButton clearOverride = ElwhaButton.filledTonalButton("Clear");

    contentField
        .getEditor()
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
                  return;
                }
                badge.setContent(text);
              }
            });

    overrideField
        .getEditor()
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
                final String text = overrideField.getText();
                badge.setAccessibilityText(text.isEmpty() ? null : text);
              }
            });

    clearOverride.addActionListener(
        e -> {
          overrideField.setText("");
          badge.setAccessibilityText(null);
        });

    final ElwhaButtonGroup variantGroup =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL)
            .add(new ElwhaButton("Small"))
            .add(new ElwhaButton("Large"));
    variantGroup.setSelectedIndex(1);
    variantGroup.addSelectionListener(
        g -> {
          if (g.getSelectedIndex() == 0) {
            swapBadge(ElwhaBadge.small());
            contentField.setEnabled(false);
          } else {
            final String seed = contentField.getText().isEmpty() ? "3" : contentField.getText();
            swapBadge(ElwhaBadge.large(seed));
            contentField.setEnabled(true);
          }
        });

    final ElwhaButton detachButton = ElwhaButton.filledTonalButton("Detach");
    detachButton.addActionListener(
        e -> {
          if (attachment != null) {
            ElwhaBadgeAnchor.detach(attachment);
            attachment = null;
          }
        });
    final ElwhaButton attachButton = ElwhaButton.filledTonalButton("Attach");
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
    gc.gridwidth = 2;
    panel.add(variantGroup, gc);
    gc.gridwidth = 1;

    gc.gridx = 1;
    gc.gridy = 1;
    gc.gridwidth = 2;
    panel.add(contentField, gc);
    gc.gridwidth = 1;

    gc.gridx = 1;
    gc.gridy = 2;
    panel.add(overrideField, gc);
    gc.gridx = 2;
    panel.add(clearOverride, gc);

    gc.gridx = 0;
    gc.gridy = 3;
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

  private static String stringify(final String value) {
    return value == null ? "(null)" : "\"" + value + "\"";
  }
}
