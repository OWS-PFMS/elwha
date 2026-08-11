package com.owspfm.elwha.switches;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Phase-1 / S4 playground (story #405) — the {@link ElwhaSwitch} icons configurations: both-icons
 * mode (check/close defaults, 24px handle in both states), selected-icon-only mode (watch the check
 * rotate &minus;45&deg; into view while the unselected handle drops back to 16px), custom glyph
 * slots (favorite/star pair via {@code MaterialIcons}), and the disabled icon treatments. Toggle
 * each row and watch the icons crossfade along the slide.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchIconsDemo {

  private final JFrame frame = new JFrame("ElwhaSwitch — Phase 1 / S4 icons");

  private ElwhaSwitchIconsDemo() {}

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(() -> new ElwhaSwitchIconsDemo().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final ElwhaSwitch both = new ElwhaSwitch(true);
    both.setIconsVisible(true);

    final ElwhaSwitch selectedOnly = new ElwhaSwitch();
    selectedOnly.setShowOnlySelectedIcon(true);

    final ElwhaSwitch custom = new ElwhaSwitch(true);
    custom.setIconsVisible(true);
    custom.setSelectedIcon(MaterialIcons.favoriteFilled(ElwhaSwitch.ICON_SIZE_PX));
    custom.setUnselectedIcon(MaterialIcons.favorite(ElwhaSwitch.ICON_SIZE_PX));

    final ElwhaSwitch disabledOn = new ElwhaSwitch(true);
    disabledOn.setIconsVisible(true);
    disabledOn.setEnabled(false);
    final ElwhaSwitch disabledOff = new ElwhaSwitch();
    disabledOff.setIconsVisible(true);
    disabledOff.setEnabled(false);

    final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 12));
    rows.setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
    rows.add(row("icons — check / close defaults, 24px handle both states", both));
    rows.add(row("show-only-selected-icon — check rotates in; 16px handle off", selectedOnly));
    rows.add(row("custom icons — favorite filled / outline", custom));
    rows.add(row("disabled selected with icon (0.38 ON_SURFACE glyph)", disabledOn));
    rows.add(row("disabled unselected with icon", disabledOff));

    final ElwhaButton flip = ElwhaButton.filledTonalButton("Flip the enabled rows");
    flip.addActionListener(
        e -> {
          both.setSelected(!both.isSelected());
          selectedOnly.setSelected(!selectedOnly.isSelected());
          custom.setSelected(!custom.isSelected());
        });
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 8));
    top.add(flip);

    frame.add(top, BorderLayout.NORTH);
    frame.add(rows, BorderLayout.CENTER);
    frame.setMinimumSize(new Dimension(680, 360));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel row(final String text, final ElwhaSwitch elwhaSwitch) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 0));
    row.add(elwhaSwitch);
    row.add(new JLabel(text));
    return row;
  }
}
