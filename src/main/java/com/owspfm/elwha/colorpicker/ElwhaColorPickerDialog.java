package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.DismissCause;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;

/**
 * The modal color picker — an {@link ElwhaColorPicker} staged inside an {@link ElwhaDialog} with
 * the M3 picker dialog's <strong>pending-until-OK</strong> semantics (design doc {@code
 * elwha-color-picker-design.md} §8): edits stay in the embedded picker, <em>OK</em> delivers the
 * final color to {@link #onConfirm}, and Cancel / Esc / scrim discard it. Composition over the
 * package-private dialog host by design — the dialog supplies the M3 container
 * (surface-container-high, XL corners, elevation 3), the headline naming the task, and the trailing
 * text-button action row.
 *
 * <p>Non-blocking, like every Elwha overlay: {@link #show} returns immediately and the outcome
 * arrives through the callbacks — the in-window translation of {@code JColorChooser.showDialog}. A
 * confirmed color becomes the next show's staged color; a cancelled one is forgotten.
 *
 * <p><strong>Two presentations, one semantics.</strong> {@link #show} presents the M3 basic modal;
 * {@link #showFullScreen} presents the same staged pick in the M3 full-screen dialog (#494) — the
 * top app bar's <em>Save</em> confirms, the leading ✕ and Esc discard. The presentations share the
 * staging, the callbacks, and the close routing, and guard each other: while one is showing the
 * other will not open.
 *
 * <pre>{@code
 * ElwhaColorPickerDialog.show(button, "Accent color", current, chosen -> apply(chosen));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaColorPickerDialog {

  private final ElwhaColorPicker picker;

  private String title = "Select color";
  private Color initialColor = Color.WHITE;
  private Consumer<Color> onConfirm;
  private Runnable onCancel;
  private ElwhaDialog dialog;
  private ElwhaFullScreenDialog fullScreenDialog;

  /**
   * Creates a dialog offering all three picker modes, staged on white.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaColorPickerDialog() {
    this.picker = new ElwhaColorPicker();
    picker.setSupportingText(null);
  }

  /**
   * Sets the dialog headline naming the task (M3: "use a descriptive title").
   *
   * @param title the headline text
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setTitle(final String title) {
    this.title = title;
  }

  /**
   * Returns the dialog headline.
   *
   * @return the headline text
   * @version v0.5.0
   * @since v0.5.0
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the color staged when the dialog next shows. A confirmed pick replaces it; a cancelled one
   * does not.
   *
   * @param initialColor the color to stage
   * @throws IllegalArgumentException if {@code initialColor} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setInitialColor(final Color initialColor) {
    if (initialColor == null) {
      throw new IllegalArgumentException("initialColor must not be null");
    }
    this.initialColor = initialColor;
  }

  /**
   * Returns the color staged for the next show.
   *
   * @return the staged color
   * @version v0.5.0
   * @since v0.5.0
   */
  public Color getInitialColor() {
    return initialColor;
  }

  /**
   * Restricts (or reorders) the embedded picker's modes — same closed-set contract as {@link
   * ElwhaColorPicker#setModes}.
   *
   * @param modes the modes to offer, in tab order
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setModes(final PickerMode... modes) {
    picker.setModes(modes);
  }

  /**
   * Opts the embedded picker into the alpha channel — same contract as {@link
   * ElwhaColorPicker#setAlphaEnabled}.
   *
   * @param alphaEnabled whether colors carry alpha
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setAlphaEnabled(final boolean alphaEnabled) {
    picker.setAlphaEnabled(alphaEnabled);
  }

  /**
   * Registers the OK callback — receives the pending color when the confirming action fires.
   *
   * @param onConfirm the confirm consumer
   * @version v0.5.0
   * @since v0.5.0
   */
  public void onConfirm(final Consumer<Color> onConfirm) {
    this.onConfirm = onConfirm;
  }

  /**
   * Registers the cancel callback — runs on Cancel, Esc, or scrim dismissal. The pending color is
   * discarded.
   *
   * @param onCancel the cancel callback
   * @version v0.5.0
   * @since v0.5.0
   */
  public void onCancel(final Runnable onCancel) {
    this.onCancel = onCancel;
  }

  /**
   * Stages the initial color and shows the dialog over the parent's window. Non-blocking; ignored
   * if the dialog is already showing.
   *
   * @param parent any component inside the host frame
   * @version v0.5.0
   * @since v0.5.0
   */
  public void show(final Component parent) {
    if (dialog != null || fullScreenDialog != null) {
      return;
    }
    stage();
    dialog =
        ElwhaDialog.builder()
            .headline(title)
            .content(picker)
            .cancelAction(ElwhaButton.textButton("Cancel"))
            .confirmAction(ElwhaButton.textButton("OK"))
            .dismissibleByEsc(true)
            .onClose(this::handleClose)
            .build();
    dialog.show(parent);
  }

  /**
   * Stages the initial color and presents the pick as an M3 <strong>full-screen dialog</strong> —
   * the top app bar's <em>Save</em> confirms, the leading ✕ and Esc discard; staging, callbacks,
   * and reopen memory match {@link #show}. Non-blocking; ignored while either presentation is
   * showing.
   *
   * @param parent any component inside the host frame
   * @version v0.5.0
   * @since v0.5.0
   */
  public void showFullScreen(final Component parent) {
    if (dialog != null || fullScreenDialog != null) {
      return;
    }
    stage();
    fullScreenDialog = buildFullScreen();
    fullScreenDialog.show(parent);
  }

  /**
   * Closes whichever presentation is showing, discarding the pending color (the cancel path).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void dismiss() {
    if (dialog != null) {
      dialog.dismiss();
    }
    if (fullScreenDialog != null) {
      fullScreenDialog.dismiss();
    }
  }

  /**
   * Convenience mirror of {@code JColorChooser.showDialog} — builds, stages, wires, and shows in
   * one call. Non-blocking: the pick arrives via {@code onConfirm}; dismissal is silent.
   *
   * @param parent any component inside the host frame
   * @param title the dialog headline, or {@code null} for "Select color"
   * @param initialColor the color to stage, or {@code null} for white
   * @param onConfirm receives the confirmed color
   * @return the dialog instance (reusable for later shows)
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaColorPickerDialog show(
      final Component parent,
      final String title,
      final Color initialColor,
      final Consumer<Color> onConfirm) {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    if (title != null) {
      dialog.setTitle(title);
    }
    if (initialColor != null) {
      dialog.setInitialColor(initialColor);
    }
    dialog.onConfirm(onConfirm);
    dialog.show(parent);
    return dialog;
  }

  ElwhaColorPicker picker() {
    return picker;
  }

  ElwhaFullScreenDialog buildFullScreen() {
    return ElwhaFullScreenDialog.builder()
        .headline(title)
        .content(picker)
        .confirmAction(ElwhaButton.textButton("Save"))
        .contentMaxWidth(picker.getPreferredSize().width)
        .dismissibleByEsc(true)
        .onClose(this::handleClose)
        .build();
  }

  void stage() {
    picker.setColor(initialColor);
  }

  void handleClose(final DismissCause cause) {
    dialog = null;
    fullScreenDialog = null;
    if (cause == DismissCause.CONFIRM) {
      initialColor = picker.getColor();
      if (onConfirm != null) {
        onConfirm.accept(picker.getColor());
      }
    } else if (onCancel != null) {
      onCancel.run();
    }
  }
}
