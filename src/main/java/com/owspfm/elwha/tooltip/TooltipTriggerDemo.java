package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * S2 visual smoke for the {@link ElwhaTooltip} trigger machinery (#448): a toolbar of icon-button
 * anchors with attached plain tooltips — hover one to see the 500&nbsp;ms dwell, slide between them
 * to see one-at-a-time eviction, hover onto a shown tooltip to see it stay (WCAG 1.4.13), press an
 * anchor to see press-to-dismiss, Tab through the toolbar to see the immediate keyboard-focus
 * trigger (click focus stays quiet), and Esc / wheel to dismiss. Spinners retune both delays live;
 * a detach toggle proves full listener teardown.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipTriggerDemo {

  private TooltipTriggerDemo() {}

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
    SwingUtilities.invokeLater(TooltipTriggerDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTooltip — S2 trigger machinery (#448)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ElwhaIconButton deleteButton = ElwhaIconButton.standardIconButton(MaterialIcons.delete());
    final ElwhaIconButton editButton = ElwhaIconButton.standardIconButton(MaterialIcons.edit());
    final ElwhaIconButton infoButton = ElwhaIconButton.standardIconButton(MaterialIcons.info());
    final ElwhaIconButton helpButton = ElwhaIconButton.standardIconButton(MaterialIcons.help());

    final ElwhaTooltip deleteTip = ElwhaTooltip.plain("Delete").attach(deleteButton);
    final ElwhaTooltip editTip = ElwhaTooltip.plain("Edit").attach(editButton);
    final ElwhaTooltip infoTip =
        ElwhaTooltip.plain(
                "Hover onto this tooltip — it stays while the pointer is over it, then lingers"
                    + " 600 ms once you leave.")
            .attach(infoButton);
    final ElwhaTooltip helpTip = ElwhaTooltip.plain("Help").attach(helpButton);
    final List<ElwhaTooltip> tips = List.of(deleteTip, editTip, infoTip, helpTip);

    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 16));
    toolbar.setBorder(BorderFactory.createEmptyBorder(48, 24, 48, 24));
    toolbar.add(deleteButton);
    toolbar.add(editButton);
    toolbar.add(infoButton);
    toolbar.add(helpButton);

    final ElwhaTextField field = ElwhaTextField.outlined("Focus parking lot");
    final ElwhaButton sendButton = ElwhaButton.filledButton("Send");
    final ElwhaTooltip sendTip = ElwhaTooltip.plain("Tab here shows me immediately");
    sendTip.attach(sendButton);

    final JSpinner showDelay = new JSpinner(new SpinnerNumberModel(500, 0, 5000, 100));
    showDelay.addChangeListener(
        e -> tips.forEach(t -> t.setShowDelayMs((Integer) showDelay.getValue())));
    final JSpinner hideDelay = new JSpinner(new SpinnerNumberModel(600, 0, 5000, 100));
    hideDelay.addChangeListener(
        e -> tips.forEach(t -> t.setHideDelayMs((Integer) hideDelay.getValue())));

    final ElwhaButton detachToggle = ElwhaButton.outlinedButton("Detach Delete");
    detachToggle.addActionListener(
        e -> {
          if (deleteTip.getAttachedAnchor() != null) {
            deleteTip.detach();
            detachToggle.setText("Attach Delete");
          } else {
            deleteTip.attach(deleteButton);
            detachToggle.setText("Detach Delete");
          }
        });

    final ElwhaButton programmatic = ElwhaButton.textButton("Show Help's tooltip now");
    programmatic.addActionListener(e -> helpTip.show(helpButton));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(field);
    controls.add(sendButton);
    controls.add(new JLabel("Show delay (ms):"));
    controls.add(showDelay);
    controls.add(new JLabel("Hide linger (ms):"));
    controls.add(hideDelay);
    controls.add(detachToggle);
    controls.add(programmatic);

    frame.setLayout(new BorderLayout());
    frame.add(toolbar, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(860, 420);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
