package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Reusable editors for the card's composition primitives — the sub-component editors a host mounts
 * as a Workbench facet (#318), mirroring {@code BadgePlaygroundPanels.buildBadgeEditor}.
 *
 * <p>A card is chrome plus the primitives a consumer composes into it, and a primitive carries
 * configuration of its own that the card does not own: {@link ElwhaCardMedia}'s frame and its
 * accessibility posture. A Workbench that exposes only which slots <em>exist</em> renders every one
 * of them in exactly one configuration, so this editor covers the axes the host does not.
 *
 * <p>The editor holds configuration rather than a reference to a live primitive, because the Card
 * Workbench rebuilds its card from scratch on every control change: a host re-applies with {@link
 * MediaEditor#applyTo(ElwhaCardMedia)} against the fresh instance rather than re-binding the
 * editor.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class CardPlaygroundPanels {

  private CardPlaygroundPanels() {}

  /**
   * Builds the reusable editor for a card's {@link ElwhaCardMedia} slot.
   *
   * @param onChange invoked after each control change, once the editor's gating has settled
   * @return the editor
   * @version v0.5.0
   * @since v0.5.0
   */
  public static MediaEditor buildMediaEditor(final Runnable onChange) {
    return new MediaEditor(onChange);
  }

  /**
   * The aspect ratios the media editor offers, as width ÷ height. Presets rather than a free
   * numeric field: the point is to show the slot reshaping the card, and four named ratios read as
   * the M3 imagery guidance where an arbitrary decimal reads as a number nobody chose.
   *
   * @author Charles Bryan
   * @version v0.5.0
   * @since v0.5.0
   */
  public enum MediaAspect {
    /** 16:9 — the widescreen default {@link ElwhaCardMedia} ships with. */
    WIDESCREEN("16:9", 16.0 / 9.0),
    /** 3:2 — the classic photographic frame. */
    PHOTO("3:2", 3.0 / 2.0),
    /** 4:3 — the squarer standard frame. */
    STANDARD("4:3", 4.0 / 3.0),
    /** 1:1 — square, for avatar-like and product imagery. */
    SQUARE("1:1", 1.0);

    private final String label;
    private final double ratio;

    MediaAspect(final String label, final double ratio) {
      this.label = label;
      this.ratio = ratio;
    }

    /**
     * The ratio as width ÷ height, for {@link ElwhaCardMedia#setAspectRatio(double)}.
     *
     * @return the ratio as width ÷ height
     * @version v0.5.0
     * @since v0.5.0
     */
    public double ratio() {
      return ratio;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  /**
   * A live editor for a card's media slot, built by {@link #buildMediaEditor(Runnable)}.
   *
   * @author Charles Bryan
   * @version v0.5.0
   * @since v0.5.0
   */
  public static final class MediaEditor {

    private final JPanel panel = new JPanel();
    private final ElwhaSelectField<MediaAspect> aspectBox =
        ElwhaSelectField.outlined("Aspect ratio");
    private final ElwhaCheckbox explicitHeightBox = new ElwhaCheckbox("Explicit height");
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(160, 40, 400, 20));
    private final ElwhaCheckbox decorativeBox = new ElwhaCheckbox("Decorative");
    private final ElwhaTextField altTextField = ElwhaTextField.outlined("Alt text");

    private MediaEditor(final Runnable onChange) {
      aspectBox.setOptions(List.of(MediaAspect.values()));
      aspectBox.setSelectedValue(MediaAspect.WIDESCREEN);
      decorativeBox.setChecked(true);
      altTextField.setText("A gradient placeholder standing in for card imagery");

      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setOpaque(false);
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(editorSection("Sizing"));
      panel.add(editorRow(aspectBox));
      panel.add(editorRow(explicitHeightBox));
      panel.add(editorRow("Height (dp)", heightSpinner));
      panel.add(editorSection("Accessibility"));
      panel.add(editorRow(decorativeBox));
      panel.add(editorRow(altTextField));

      final Runnable changed =
          () -> {
            syncGates();
            onChange.run();
          };
      aspectBox.addSelectionChangeListener(value -> changed.run());
      explicitHeightBox.addActionListener(event -> changed.run());
      heightSpinner.addChangeListener(event -> changed.run());
      decorativeBox.addActionListener(event -> changed.run());
      altTextField.getEditor().getDocument().addDocumentListener(onTextChange(onChange));
      syncGates();
    }

    // An explicit height overrides the ratio rather than combining with it, and decorative media is
    // skipped by assistive technology entirely — so in both cases the control it supersedes is
    // disabled rather than left looking live.
    private void syncGates() {
      final boolean explicit = explicitHeightBox.isChecked();
      aspectBox.setEnabled(!explicit);
      heightSpinner.setEnabled(explicit);
      altTextField.setEnabled(!decorativeBox.isChecked());
    }

    /**
     * The editor's control panel, for mounting in a {@code WorkbenchControls} column.
     *
     * @return the control panel
     * @version v0.5.0
     * @since v0.5.0
     */
    public JComponent panel() {
      return panel;
    }

    /**
     * Applies the current configuration to {@code media}.
     *
     * @param media the card's media slot; ignored when {@code null} (the card has no media)
     * @version v0.5.0
     * @since v0.5.0
     */
    public void applyTo(final ElwhaCardMedia media) {
      if (media == null) {
        return;
      }
      media.setAspectRatio(aspectBox.getSelectedValue().ratio());
      media.setPreferredHeight(
          explicitHeightBox.isChecked() ? (Integer) heightSpinner.getValue() : -1);
      media.setDecorative(decorativeBox.isChecked());
      media.setAltText(decorativeBox.isChecked() ? null : altTextField.getText());
    }

    /**
     * The equivalent Java for the current configuration, as calls against {@code receiver}.
     *
     * @param receiver the variable name to render the calls against
     * @return the equivalent-Java snippet, one call per line
     * @version v0.5.0
     * @since v0.5.0
     */
    public String code(final String receiver) {
      final StringBuilder code = new StringBuilder(240);
      if (explicitHeightBox.isChecked()) {
        code.append(receiver)
            .append(".setPreferredHeight(")
            .append(heightSpinner.getValue())
            .append(");");
      } else {
        code.append(receiver)
            .append(".setAspectRatio(")
            .append(aspectBox.getSelectedValue())
            .append(");   // ")
            .append(String.format("%.3f", aspectBox.getSelectedValue().ratio()));
      }
      if (decorativeBox.isChecked()) {
        code.append("\n").append(receiver).append(".setDecorative(true);   // skipped by AT");
      } else {
        code.append("\n").append(receiver).append(".setDecorative(false);");
        code.append("\n")
            .append(receiver)
            .append(".setAltText(\"")
            .append(altTextField.getText())
            .append("\");");
      }
      return code.toString();
    }
  }

  private static JComponent editorSection(final String title) {
    final JLabel label = new JLabel(title);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
    return label;
  }

  private static JComponent editorRow(final JComponent field) {
    return editorRow("", field);
  }

  private static JComponent editorRow(final String label, final JComponent field) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    row.setOpaque(false);
    row.setAlignmentX(Component.LEFT_ALIGNMENT);
    if (!label.isEmpty()) {
      final JLabel caption = new JLabel(label);
      caption.putClientProperty("FlatLaf.styleClass", "small");
      row.add(caption);
    }
    row.add(field);
    return row;
  }

  private static DocumentListener onTextChange(final Runnable action) {
    return new DocumentListener() {
      @Override
      public void insertUpdate(final DocumentEvent event) {
        action.run();
      }

      @Override
      public void removeUpdate(final DocumentEvent event) {
        action.run();
      }

      @Override
      public void changedUpdate(final DocumentEvent event) {
        action.run();
      }
    };
  }
}
