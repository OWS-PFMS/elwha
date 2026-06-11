package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Base class for the picker's mode panes (design doc {@code elwha-color-picker-design.md} §2).
 * Every pane edit funnels through {@link #commit} into the picker's single commit path; the picker
 * fans accepted colors back out through {@link #syncFromPicker} to every pane that was not the
 * commit source, so panes never observe their own echo.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
abstract class ColorPickerPane extends JPanel {

  private final ElwhaColorPicker picker;

  ColorPickerPane(final ElwhaColorPicker picker) {
    this.picker = picker;
    setOpaque(false);
    // Bottom inset is SM, not LG: the dialog host supplies its own content-to-actions gap, and
    // the hex field already reserves a supporting-text row (smoke-iterate trim).
    setBorder(
        BorderFactory.createEmptyBorder(
            SpaceScale.MD.px(), SpaceScale.LG.px(), SpaceScale.SM.px(), SpaceScale.LG.px()));
  }

  final ElwhaColorPicker picker() {
    return picker;
  }

  final void commit(final Color color, final boolean adjusting) {
    picker.commitFromPane(this, color, adjusting);
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    cascadeEnabled(this, enabled);
    repaint();
  }

  private static void cascadeEnabled(final java.awt.Container container, final boolean enabled) {
    for (final java.awt.Component child : container.getComponents()) {
      child.setEnabled(enabled);
      if (child instanceof java.awt.Container nested) {
        cascadeEnabled(nested, enabled);
      }
    }
  }

  /**
   * Adopts a color committed elsewhere (another pane, or {@link ElwhaColorPicker#setColor}). Never
   * called for this pane's own commits.
   *
   * @param color the picker's new current color
   */
  abstract void syncFromPicker(Color color);
}
