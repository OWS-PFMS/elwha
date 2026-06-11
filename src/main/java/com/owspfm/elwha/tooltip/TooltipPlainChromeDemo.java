package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * S1 visual smoke for {@link ElwhaTooltip} plain chrome and placement (#447): a 3×3 grid of anchor
 * buttons — pressing one shows its tooltip, so every flip/clamp corner case is one click away —
 * plus a focused text field proving the passive-focus contract (the caret keeps blinking while a
 * tooltip shows above the field), a long wrapped label, alignment/placement cycling, and a
 * light/dark toggle for token-correctness in both modes.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipPlainChromeDemo {

  private TooltipPlainChromeDemo() {}

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
    SwingUtilities.invokeLater(TooltipPlainChromeDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaTooltip — S1 plain chrome & placement (#447)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final List<ElwhaTooltip> tooltips = new ArrayList<>();

    final JPanel grid = new JPanel(new GridLayout(3, 3, 48, 48));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    final String[] labels = {
      "Top left", "Top center", "Top right",
      "Mid left", "Center", "Mid right",
      "Bottom left", "Bottom center", "Bottom right"
    };
    for (final String label : labels) {
      final ElwhaButton anchorButton = ElwhaButton.filledTonalButton(label);
      final ElwhaTooltip tip =
          label.equals("Center")
              ? ElwhaTooltip.plain(
                  "Plain tooltips wrap their label by hand at the 200 px max width — "
                      + "no HTML views anywhere near this paint pipeline.")
              : ElwhaTooltip.plain("Save to favorites");
      tooltips.add(tip);
      anchorButton.addActionListener(e -> tip.show(anchorButton));
      // GridBagLayout centers the button at its preferred size — GridLayout would stretch it to
      // the whole cell and the tooltip would hug the invisible cell edge, not the pill.
      final JPanel cell = new JPanel(new java.awt.GridBagLayout());
      cell.setOpaque(false);
      cell.add(anchorButton);
      grid.add(cell);
    }

    final ElwhaTextField field = ElwhaTextField.outlined("Focus me and type");
    final ElwhaTooltip fieldTip = ElwhaTooltip.plain("The caret must keep blinking");
    tooltips.add(fieldTip);
    final ElwhaButton overField = ElwhaButton.textButton("Show over the field");
    overField.addActionListener(
        e -> {
          // The composite's wrapper is not focusable — the caret lives in the inner editor, and
          // only an explicit editor focus request brings it back after the button click took it.
          field.getEditor().requestFocusInWindow();
          fieldTip.show(field);
        });

    final ElwhaButton dismissAll = ElwhaButton.outlinedButton("Dismiss all");
    dismissAll.addActionListener(e -> tooltips.forEach(ElwhaTooltip::dismiss));

    final ElwhaButton alignment = ElwhaButton.outlinedButton("Alignment: CENTER");
    alignment.addActionListener(
        e -> {
          final TooltipAlignment[] all = TooltipAlignment.values();
          final TooltipAlignment next =
              all[(tooltips.get(0).getAlignment().ordinal() + 1) % all.length];
          tooltips.forEach(t -> t.setAlignment(next));
          alignment.setText("Alignment: " + next);
        });

    final ElwhaButton placement = ElwhaButton.outlinedButton("Prefer: ABOVE");
    placement.addActionListener(
        e -> {
          final TooltipPlacement next =
              tooltips.get(0).getPreferredPlacement() == TooltipPlacement.ABOVE
                  ? TooltipPlacement.BELOW
                  : TooltipPlacement.ABOVE;
          tooltips.forEach(t -> t.setPreferredPlacement(next));
          placement.setText("Prefer: " + next);
        });

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode nextMode =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(nextMode).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final JPanel fieldRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 0));
    fieldRow.add(field);
    fieldRow.add(overField);

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(dismissAll);
    controls.add(alignment);
    controls.add(placement);
    controls.add(mode);

    final JPanel south = new JPanel(new BorderLayout());
    south.add(fieldRow, BorderLayout.NORTH);
    south.add(controls, BorderLayout.SOUTH);

    frame.setLayout(new BorderLayout());
    frame.add(grid, BorderLayout.CENTER);
    frame.add(south, BorderLayout.SOUTH);
    frame.setSize(760, 560);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }
}
