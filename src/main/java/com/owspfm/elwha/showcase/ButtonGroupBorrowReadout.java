package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.buttongroup.ButtonGroupVariant;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Workbench stage that mounts a {@link ButtonGroupVariant#STANDARD} {@link ElwhaButtonGroup} above
 * a row of live per-segment width-borrow readouts ([#184]). Each readout sits centered under its
 * segment and shows that segment's current {@link ElwhaButton#currentWidthBorrow()} value (the §6
 * decay-vector contribution {@code [1.0, 0.3, 0.1, 0]}), formatted to two decimals. At rest every
 * readout reads {@code 0.00}; holding a middle segment animates the neighbors {@code 0 → target →
 * 0} in sync with the visible width pinch, making the decay vector auditable as numbers rather than
 * only as the eye-judged pinch.
 *
 * <p>Only STANDARD groups have a width-ripple ({@code CONNECTED} is excluded per §6), so this stage
 * is used only for STANDARD; the workbench mounts the bare group / flex wrapper otherwise. Segments
 * that are not {@link ElwhaButton} (icon-only segments) have no width-borrow and read {@code —}.
 *
 * <p>A single 16&nbsp;ms {@link Timer} walks the segments and repaints the readouts; it is tied to
 * display lifecycle via {@code addNotify} / {@code removeNotify}, so when the workbench swaps the
 * stage the old timer stops automatically.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
final class ButtonGroupBorrowReadout extends JPanel {

  private static final int READOUT_TICK_MS = 16;
  private static final int GROUP_TO_READOUT_GAP_PX = 10;

  private final ElwhaButtonGroup group;
  private final JLabel[] readouts;
  private final boolean[] supportsBorrow;
  private final Timer timer;

  ButtonGroupBorrowReadout(final ElwhaButtonGroup group) {
    this.group = group;
    setOpaque(false);
    setLayout(null);
    add(group);

    final int count = group.getButtonCount();
    readouts = new JLabel[count];
    supportsBorrow = new boolean[count];
    for (int i = 0; i < count; i++) {
      final boolean borrows = group.getButtonAt(i) instanceof ElwhaButton;
      supportsBorrow[i] = borrows;
      final JLabel label = new JLabel(borrows ? "0.00" : "—", SwingConstants.CENTER);
      label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
      label.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      readouts[i] = label;
      add(label);
    }

    timer = new Timer(READOUT_TICK_MS, event -> updateReadouts());
    timer.setRepeats(true);
  }

  private void updateReadouts() {
    for (int i = 0; i < readouts.length; i++) {
      if (!supportsBorrow[i]) {
        continue;
      }
      final ElwhaButton segment = (ElwhaButton) group.getButtonAt(i);
      final String text = String.format("%.2f", segment.currentWidthBorrow());
      if (!text.equals(readouts[i].getText())) {
        readouts[i].setText(text);
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension gp = group.getPreferredSize();
    int labelH = 0;
    for (final JLabel label : readouts) {
      labelH = Math.max(labelH, label.getPreferredSize().height);
    }
    return new Dimension(gp.width, gp.height + GROUP_TO_READOUT_GAP_PX + labelH);
  }

  @Override
  public void doLayout() {
    final Dimension gp = group.getPreferredSize();
    final int gx = Math.max(0, (getWidth() - gp.width) / 2);
    group.setBounds(gx, 0, gp.width, gp.height);
    group.doLayout();

    final int labelY = gp.height + GROUP_TO_READOUT_GAP_PX;
    for (int i = 0; i < readouts.length; i++) {
      final JComponent segment = group.getButtonAt(i);
      final Rectangle sb = segment.getBounds();
      final Dimension lp = readouts[i].getPreferredSize();
      final int centerX = gx + sb.x + sb.width / 2;
      readouts[i].setBounds(centerX - lp.width / 2, labelY, lp.width, lp.height);
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    timer.start();
  }

  @Override
  public void removeNotify() {
    timer.stop();
    super.removeNotify();
  }
}
