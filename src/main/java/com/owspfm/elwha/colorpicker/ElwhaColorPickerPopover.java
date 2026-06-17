package com.owspfm.elwha.colorpicker;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.event.ChangeListener;

/**
 * The Elwha color picker's <strong>docked popover</strong> presentation (V2 design doc {@code
 * elwha-color-picker-v2-design.md} §5) — the analog of the M3 docked date picker: an anchored,
 * light-dismiss surface wrapping an inline {@link ElwhaColorPicker}, opening below its anchor
 * (flipping above on clip, RTL-mirrored) on the popup layer.
 *
 * <p><strong>Commits are live.</strong> Every edit fires this popover's {@link ChangeListener}s
 * immediately — the Chrome {@code input[type=color]} convention. Dismissal (Esc, outside press,
 * focus escape, {@link #close()}) just closes: nothing further is committed and <em>nothing is
 * reverted</em>. Pending-until-OK semantics belong to {@code ElwhaColorPickerDialog}.
 *
 * <pre>{@code
 * ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
 * popover.addChangeListener(e -> swatchButton.setSwatchColor(popover.getColor()));
 * swatchButton.addActionListener(e -> popover.show(swatchButton));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerPopover {

  private final ElwhaColorPicker picker = new ElwhaColorPicker();
  private final PopoverHost host = new PopoverHost(this);

  private Runnable onDismiss;
  private boolean dismissPending;

  /**
   * Creates a popover whose embedded picker offers every mode. The picker's supporting text is
   * suppressed — the anchor names the task, the way a dialog headline does.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaColorPickerPopover() {
    picker.setSupportingText(null);
  }

  /**
   * Stages the color the popover opens with (and shows immediately when already open).
   *
   * @param color the color to stage
   * @throws IllegalArgumentException if {@code color} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setInitialColor(final Color color) {
    picker.setColor(color);
  }

  /**
   * Returns the embedded picker's current color — live while the popover is open.
   *
   * @return the current color
   * @version v0.5.0
   * @since v0.5.0
   */
  public Color getColor() {
    return picker.getColor();
  }

  /**
   * Restricts (or reorders) the embedded picker's modes — see {@link ElwhaColorPicker#setModes}.
   *
   * @param modes the modes to offer, in tab order
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setModes(final PickerMode... modes) {
    picker.setModes(modes);
  }

  /**
   * Returns the embedded picker's offered modes.
   *
   * @return an immutable mode list
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<PickerMode> getModes() {
    return picker.getModes();
  }

  /**
   * Restricts (or reorders) the SWATCHES mode's swatch sources — see {@link
   * ElwhaColorPicker#setSwatchSources}.
   *
   * @param sources the sources to offer, in toggle order
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setSwatchSources(final SwatchSource... sources) {
    picker.setSwatchSources(sources);
  }

  /**
   * Opts the embedded picker into the alpha channel — see {@link ElwhaColorPicker#setAlphaEnabled}.
   *
   * @param alphaEnabled whether colors carry alpha
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setAlphaEnabled(final boolean alphaEnabled) {
    picker.setAlphaEnabled(alphaEnabled);
  }

  /**
   * Opts the embedded picker into the eyedropper — see {@link
   * ElwhaColorPicker#setEyedropperEnabled}. While the screen sampler is open, the popover suspends
   * light dismissal so the sampler's capture windows never close it mid-pick.
   *
   * @param eyedropperEnabled whether the header offers the eyedropper
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setEyedropperEnabled(final boolean eyedropperEnabled) {
    picker.setEyedropperEnabled(eyedropperEnabled);
  }

  /**
   * Registers a listener on the embedded picker — fired live on every accepted commit while the
   * popover is open.
   *
   * @param listener the listener to add
   * @version v0.5.0
   * @since v0.5.0
   */
  public void addChangeListener(final ChangeListener listener) {
    picker.addChangeListener(listener);
  }

  /**
   * Removes a previously registered change listener.
   *
   * @param listener the listener to remove
   * @version v0.5.0
   * @since v0.5.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    picker.removeChangeListener(listener);
  }

  /**
   * Registers the dismissal callback — run once per {@link #show}, whatever the cause (Esc, outside
   * press, focus escape, {@link #close()}). Read {@link #getColor()} for the final value.
   *
   * @param onDismiss the callback, or {@code null} to clear
   * @version v0.5.0
   * @since v0.5.0
   */
  public void onDismiss(final Runnable onDismiss) {
    this.onDismiss = onDismiss;
  }

  /**
   * Shows the popover anchored to a component — below it, leading edges aligned (flipping above on
   * viewport clip; RTL aligns trailing edges). A popover already showing is a no-op.
   *
   * @param anchor the component to anchor to; must be in a realized window
   * @version v0.5.0
   * @since v0.5.0
   */
  public void show(final Component anchor) {
    if (host.showing()) {
      return;
    }
    dismissPending = true;
    host.show(anchor);
  }

  /**
   * Closes the popover programmatically; not showing is a no-op. The dismiss callback still runs.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void close() {
    host.requestClose();
  }

  /**
   * Answers whether the popover is currently shown.
   *
   * @return {@code true} while the popover is up
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isShowing() {
    return host.showing();
  }

  ElwhaColorPicker picker() {
    return picker;
  }

  void handleClosed() {
    if (!dismissPending) {
      return;
    }
    dismissPending = false;
    if (onDismiss != null) {
      onDismiss.run();
    }
  }
}
