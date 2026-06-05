package com.owspfm.elwha.textfield;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A token-native M3 text-field primitive: a labeled, token-themed single-line input with two chrome
 * variants ({@link Variant#FILLED} / {@link Variant#OUTLINED}), a floating label, supporting/error
 * text, and icon / prefix-suffix slots.
 *
 * <p><b>Architecture (the load-bearing decision, design §2).</b> {@code ElwhaTextField} is a {@code
 * JComponent} <i>decorator</i> over an embedded {@link javax.swing.JTextField}: the wrapped editor
 * owns the caret, selection, IME, copy/paste, undo, editing keys, Tab traversal, and the {@code
 * AccessibleJTextComponent} surface — all of which Swing provides for free and which a from-scratch
 * editor would have to re-derive. Elwha owns the chrome paint (filled fill + active indicator, or
 * outlined stroke), the floating label, the typed slots, the token mapping, and the one Swing
 * accessibility gap — the error&#8594;"alert" announcement.
 *
 * <p>Decisions and the deliberate M3 mappings: {@code docs/research/elwha-textfield-design.md} and
 * its companion {@code elwha-textfield-research.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaTextField extends JComponent {

  /**
   * The chrome variant — a style-only axis with one identical API (M3: "both variants provide the
   * same functionality"). Mirrors the per-variant naming of {@code ElwhaCard} / {@code ElwhaChip}.
   *
   * @since v0.4.0
   */
  public enum Variant {
    /**
     * Higher-emphasis field: a {@code surface-container-highest} fill with top-only rounded corners
     * and a bottom active indicator.
     */
    FILLED,
    /**
     * Lower-emphasis field: a transparent container with a full {@code outline} stroke on all four
     * rounded corners (the floated label notches the top edge). Better when many fields stack in a
     * form.
     */
    OUTLINED
  }

  // M3 measurements (design §5 / research §M). dp == px in this code; FlatLaf scales the Graphics.
  static final int CONTAINER_HEIGHT = 56;
  static final int PAD_TOP_BOTTOM = SpaceScale.SM.px(); // 8
  static final int PAD_LR_NO_ICON = SpaceScale.LG.px(); // 16
  static final int PAD_LR_ICON =
      SpaceScale.MD.px(); // 12 (edge-to-icon when an icon slot is present)
  static final int ICON_TEXT_GAP = SpaceScale.LG.px(); // 16 (icon-to-text)
  static final int ICON_GLYPH = 24; // painted glyph size (touch target is the 56dp field)
  static final int SUPPORTING_TOP_PAD = SpaceScale.XS.px(); // 4
  static final int LABEL_NOTCH_PAD = SpaceScale.XS.px(); // 4 (outlined label-notch gap)
  static final int RESTING_STROKE = 1;
  static final int FOCUS_STROKE = 3; // Expressive bump (design §4; resting 1dp)
  static final int DEFAULT_WIDTH = 245; // M3 default layout width
  static final int MAX_WIDTH = 488; // M3 maximum width

  /** Distance from a field edge to the text region when that side carries an icon slot. */
  static final int ICON_SLOT = PAD_LR_ICON + ICON_GLYPH + ICON_TEXT_GAP; // 52

  private static final String FLATLAF_PLACEHOLDER_KEY = "JTextField.placeholderText";

  private Variant variant;
  private final JTextField editor = new JTextField();

  private String label = "";
  private String placeholder = "";
  private String prefix = "";
  private String suffix = "";
  private String supportingText = "";
  private String errorText = "";

  private Icon leadingIcon;
  private Icon trailingIcon;
  private ElwhaIconButton trailingButton;

  private boolean required;
  private boolean noAsterisk;

  private boolean hovered;
  private boolean focused;
  private boolean error;

  /** 0 = resting (centered, BODY_LARGE); 1 = floated (top, BODY_SMALL). */
  private final MorphAnimator labelMorph = new MorphAnimator(this, MorphAnimator.SHORT3_MS);

  private boolean labelFloated;

  /** Creates a {@link Variant#FILLED} field with no label. */
  public ElwhaTextField() {
    this(Variant.FILLED, "");
  }

  /**
   * Creates a {@link Variant#FILLED} field with the given floating label.
   *
   * @param label the floating label text (may be empty for a label-less field)
   */
  public ElwhaTextField(final String label) {
    this(Variant.FILLED, label);
  }

  /**
   * Creates a field with the given variant and floating label.
   *
   * @param variant the chrome variant
   * @param label the floating label text (may be empty for a label-less field)
   */
  public ElwhaTextField(final Variant variant, final String label) {
    this.variant = variant == null ? Variant.FILLED : variant;
    this.label = label == null ? "" : label;
    initEditor();
    initInteraction();
    setLayout(null);
    add(editor);
    setOpaque(false);
    syncAccessibleName();
  }

  /**
   * Creates a {@link Variant#FILLED} field with the given label.
   *
   * @param label the floating label text
   * @return a new filled field
   */
  public static ElwhaTextField filled(final String label) {
    return new ElwhaTextField(Variant.FILLED, label);
  }

  /**
   * Creates a {@link Variant#OUTLINED} field with the given label.
   *
   * @param label the floating label text
   * @return a new outlined field
   */
  public static ElwhaTextField outlined(final String label) {
    return new ElwhaTextField(Variant.OUTLINED, label);
  }

  private void initEditor() {
    editor.setBorder(BorderFactory.createEmptyBorder());
    editor.setOpaque(false);
    editor.setForeground(ColorRole.ON_SURFACE.resolve());
    editor.setFont(TypeRole.BODY_LARGE.resolve());
    updateCaretColor();
    editor
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                onTextChanged();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                onTextChanged();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                onTextChanged();
              }
            });
  }

  private void initInteraction() {
    editor.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            focused = true;
            updateLabelFloat();
            updatePlaceholder();
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            focused = false;
            updateLabelFloat();
            updatePlaceholder();
            repaint();
          }
        });

    final MouseAdapter ma =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            hovered = true;
            repaint();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            hovered = false;
            repaint();
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (editor.isEnabled()) {
              editor.requestFocusInWindow();
            }
          }
        };
    addMouseListener(ma);
    editor.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            hovered = true;
            repaint();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            hovered = false;
            repaint();
          }
        });
  }

  private void onTextChanged() {
    updateLabelFloat();
    updatePlaceholder();
    repaint();
  }

  /** Drives the float animator toward floated (focused or populated) or resting. */
  private void updateLabelFloat() {
    final boolean shouldFloat = !label.isEmpty() && (focused || !editor.getText().isEmpty());
    if (shouldFloat == labelFloated) {
      return;
    }
    labelFloated = shouldFloat;
    if (shouldFloat) {
      labelMorph.start();
    } else {
      labelMorph.reverse();
    }
  }

  /**
   * Toggles FlatLaf's native placeholder so it shows only when the editor is empty and the label is
   * out of the way (focused, populated-but-empty is impossible, or label-less).
   */
  private void updatePlaceholder() {
    final boolean show =
        !placeholder.isEmpty() && editor.getText().isEmpty() && (focused || label.isEmpty());
    editor.putClientProperty(FLATLAF_PLACEHOLDER_KEY, show ? placeholder : null);
  }

  // ---- Public API -----------------------------------------------------------

  /**
   * Returns the chrome variant.
   *
   * @return the variant
   */
  public Variant getVariant() {
    return variant;
  }

  /**
   * Sets the chrome variant.
   *
   * @param variant the variant (a {@code null} is treated as {@link Variant#FILLED})
   */
  public void setVariant(final Variant variant) {
    this.variant = variant == null ? Variant.FILLED : variant;
    repaint();
  }

  /**
   * Returns the floating label text.
   *
   * @return the label (empty if the field is label-less)
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the floating label text. An empty label renders a label-less field.
   *
   * @param label the label text
   */
  public void setLabel(final String label) {
    this.label = label == null ? "" : label;
    syncAccessibleName();
    updateLabelFloat();
    updatePlaceholder();
    revalidate();
    repaint();
  }

  /**
   * Returns the current input text.
   *
   * @return the editor text
   */
  public String getText() {
    return editor.getText();
  }

  /**
   * Sets the input text.
   *
   * @param text the text to display in the editor
   */
  public void setText(final String text) {
    editor.setText(text);
    onTextChanged();
  }

  /**
   * Returns the placeholder hint shown in the empty (and focused, when labelled) field.
   *
   * @return the placeholder text
   */
  public String getPlaceholder() {
    return placeholder;
  }

  /**
   * Sets the placeholder hint. The placeholder is shown in {@code on-surface-variant} while the
   * editor is empty and either focused or label-less (so it never collides with a resting label).
   *
   * @param placeholder the placeholder text
   */
  public void setPlaceholder(final String placeholder) {
    this.placeholder = placeholder == null ? "" : placeholder;
    updatePlaceholder();
    repaint();
  }

  /**
   * Returns the leading (in-field) icon.
   *
   * @return the leading icon, or {@code null} if none
   */
  public Icon getLeadingIcon() {
    return leadingIcon;
  }

  /**
   * Sets the leading in-field icon (a {@code MaterialIcons} 24dp glyph). The leading slot is for
   * in-field affordances; decorative form-row icons belong in the consumer's layout, not here.
   *
   * @param icon the leading icon, or {@code null} to clear
   */
  public void setLeadingIcon(final Icon icon) {
    this.leadingIcon = icon;
    revalidate();
    repaint();
  }

  /**
   * Returns the static trailing icon.
   *
   * @return the trailing icon, or {@code null} if none (or an interactive trailing button is set)
   */
  public Icon getTrailingIcon() {
    return trailingIcon;
  }

  /**
   * Sets a static (non-interactive) trailing icon — e.g. a decorative or error glyph. Clears any
   * interactive {@linkplain #setTrailingIconButton(ElwhaIconButton) trailing button}. For a
   * clickable affordance (clear / show-password), use {@link #setTrailingIconButton} instead so the
   * field inherits Button accessibility for free.
   *
   * @param icon the trailing icon, or {@code null} to clear
   */
  public void setTrailingIcon(final Icon icon) {
    if (trailingButton != null) {
      remove(trailingButton);
      trailingButton = null;
    }
    this.trailingIcon = icon;
    revalidate();
    repaint();
  }

  /**
   * Returns the interactive trailing icon button.
   *
   * @return the trailing button, or {@code null} if none
   */
  public ElwhaIconButton getTrailingIconButton() {
    return trailingButton;
  }

  /**
   * Sets an interactive trailing icon button (clear / show-password / picker launch). The button is
   * hosted as a real child so it brings free {@code AccessibleRole.PUSH_BUTTON} semantics; it
   * replaces any static {@linkplain #setTrailingIcon(Icon) trailing icon}.
   *
   * @param button the trailing icon button, or {@code null} to clear
   */
  public void setTrailingIconButton(final ElwhaIconButton button) {
    if (trailingButton != null) {
      remove(trailingButton);
    }
    this.trailingIcon = null;
    this.trailingButton = button;
    if (button != null) {
      add(button);
    }
    revalidate();
    repaint();
  }

  /**
   * Returns the inline prefix affix.
   *
   * @return the prefix text
   */
  public String getPrefixText() {
    return prefix;
  }

  /**
   * Sets the inline prefix affix shown immediately before the input ({@code on-surface-variant}),
   * e.g. a currency symbol.
   *
   * @param prefix the prefix text
   */
  public void setPrefixText(final String prefix) {
    this.prefix = prefix == null ? "" : prefix;
    revalidate();
    repaint();
  }

  /**
   * Returns the inline suffix affix.
   *
   * @return the suffix text
   */
  public String getSuffixText() {
    return suffix;
  }

  /**
   * Sets the inline suffix affix shown immediately after the input ({@code on-surface-variant}),
   * e.g. a unit.
   *
   * @param suffix the suffix text
   */
  public void setSuffixText(final String suffix) {
    this.suffix = suffix == null ? "" : suffix;
    revalidate();
    repaint();
  }

  /**
   * Returns the supporting text shown below the field.
   *
   * @return the supporting text
   */
  public String getSupportingText() {
    return supportingText;
  }

  /**
   * Sets the supporting text shown below the field ({@code BODY_SMALL}, {@code
   * on-surface-variant}). Its row height is always reserved so an error-text swap never shifts
   * layout.
   *
   * @param supportingText the supporting text
   */
  public void setSupportingText(final String supportingText) {
    this.supportingText = supportingText == null ? "" : supportingText;
    syncAccessibleName();
    repaint();
  }

  /**
   * Returns whether the field is marked required.
   *
   * @return {@code true} if required
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Marks the field required — appends an asterisk to the label (unless {@linkplain
   * #setNoAsterisk(boolean) suppressed}) and includes it in the accessible name. Elwha enforces no
   * validation; {@code required} is the visual + a11y cue only.
   *
   * @param required {@code true} to mark required
   */
  public void setRequired(final boolean required) {
    this.required = required;
    syncAccessibleName();
    repaint();
  }

  /**
   * Returns whether the required asterisk glyph is suppressed.
   *
   * @return {@code true} if the asterisk is hidden
   */
  public boolean isNoAsterisk() {
    return noAsterisk;
  }

  /**
   * Suppresses the required asterisk glyph while keeping the field {@linkplain #setRequired
   * required} (M3 {@code no-asterisk}) — for forms that indicate required fields with a single note
   * instead.
   *
   * @param noAsterisk {@code true} to hide the asterisk
   */
  public void setNoAsterisk(final boolean noAsterisk) {
    this.noAsterisk = noAsterisk;
    syncAccessibleName();
    repaint();
  }

  /**
   * Returns whether the field is in the error state.
   *
   * @return {@code true} if errored
   */
  public boolean isError() {
    return error;
  }

  /**
   * Sets the error state. In the error state the chrome (active indicator / outline, label,
   * supporting text, caret) renders in the {@code error} color, overriding focus.
   *
   * @param error {@code true} to render the error chrome
   */
  public void setError(final boolean error) {
    if (this.error == error) {
      return;
    }
    this.error = error;
    updateCaretColor();
    syncAccessibleName();
    if (error) {
      fireAccessibleAlert();
    }
    revalidate();
    repaint();
  }

  /**
   * Returns the error text that replaces the supporting line while errored.
   *
   * @return the error text
   */
  public String getErrorText() {
    return errorText;
  }

  /**
   * Sets the error text. While the field is {@linkplain #setError(boolean) errored}, this
   * <b>replaces</b> the supporting line (the row height is reserved, so there is no layout shift)
   * and — when the consumer has set no trailing icon — auto-shows the non-color error icon in the
   * trailing slot. It also feeds the accessibility alert (supporting text first, then error).
   *
   * @param errorText the error text
   */
  public void setErrorText(final String errorText) {
    this.errorText = errorText == null ? "" : errorText;
    syncAccessibleName();
    if (error) {
      fireAccessibleAlert();
    }
    revalidate();
    repaint();
  }

  /**
   * Returns the embedded editor. Exposed so consumers can attach document/input listeners and read
   * the {@code AccessibleJTextComponent} surface; the chrome remains Elwha-owned.
   *
   * @return the wrapped {@link JTextField}
   */
  public JTextField getEditor() {
    return editor;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    editor.setEnabled(enabled);
    repaint();
  }

  /**
   * Sets the read-only state. Unlike {@link #setEnabled(boolean) disabling}, a read-only field
   * keeps the normal (non-dimmed) chrome and its text stays selectable/copyable — it just cannot be
   * edited ({@code JTextComponent.setEditable(false)}).
   *
   * @param readOnly {@code true} for a read-only field
   */
  public void setReadOnly(final boolean readOnly) {
    editor.setEditable(!readOnly);
    repaint();
  }

  /**
   * Returns whether the field is read-only.
   *
   * @return {@code true} if not editable
   */
  public boolean isReadOnly() {
    return !editor.isEditable();
  }

  // ---- Accessibility (design §8; the error->alert is the one Swing gap) ------

  private void syncAccessibleName() {
    final StringBuilder name = new StringBuilder(label);
    if (required && !noAsterisk) {
      name.append(" *");
    }
    final String support = displayedSupporting();
    if (!support.isEmpty()) {
      name.append(name.length() > 0 ? ", " : "").append(support);
    }
    final AccessibleContext ctx = editor.getAccessibleContext();
    ctx.setAccessibleName(name.toString());
    ctx.setAccessibleDescription(accessibleDescription());
  }

  /** The supporting line as shown: error text replaces supporting text while errored. */
  private String displayedSupporting() {
    return error && !errorText.isEmpty() ? errorText : supportingText;
  }

  /** A11y description: supporting text first, then error text (research §X). */
  private String accessibleDescription() {
    if (error && !errorText.isEmpty()) {
      return supportingText.isEmpty() ? errorText : supportingText + ", " + errorText;
    }
    return supportingText.isEmpty() ? null : supportingText;
  }

  /** Whether the non-color error icon should auto-fill the trailing slot. */
  private boolean showAutoErrorIcon() {
    return error && isEnabled() && trailingIcon == null && trailingButton == null;
  }

  /** The label as painted, with the required asterisk appended when shown. */
  private String displayLabel() {
    return required && !noAsterisk ? label + " *" : label;
  }

  private boolean hasTrailing() {
    return trailingIcon != null || trailingButton != null || showAutoErrorIcon();
  }

  /** Distance from the left edge to the text region (icon slot if a leading slot sits left). */
  private int leftContentEdge() {
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final boolean iconLeft = ltr ? leadingIcon != null : hasTrailing();
    return iconLeft ? ICON_SLOT : PAD_LR_NO_ICON;
  }

  /** Distance from the right edge to the text region (icon slot if a trailing slot sits right). */
  private int rightContentEdge() {
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final boolean iconRight = ltr ? hasTrailing() : leadingIcon != null;
    return iconRight ? ICON_SLOT : PAD_LR_NO_ICON;
  }

  /** Caret follows the active stroke: error&#8594;{@code error}, otherwise {@code primary}. */
  private void updateCaretColor() {
    editor.setCaretColor((error ? ColorRole.ERROR : ColorRole.PRIMARY).resolve());
  }

  /**
   * Fires the accessibility "alert" announcement for the error state. Swing has no {@code ALERT}
   * role, so this approximates it by firing an {@link AccessibleContext} description property
   * change on the focusable editor — the Java Access Bridge surfaces this to AT as a live update.
   * The full message composition (supporting text, then error) lands in S5; S1 proves the
   * mechanism.
   */
  private void fireAccessibleAlert() {
    final AccessibleContext ctx = editor.getAccessibleContext();
    final String announcement =
        !errorText.isEmpty()
            ? (supportingText.isEmpty() ? errorText : supportingText + ", " + errorText)
            : "Error";
    // Fire from a cleared value so AT re-announces even when the text is unchanged.
    ctx.setAccessibleDescription(null);
    ctx.firePropertyChange(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY, null, announcement);
    ctx.setAccessibleDescription(accessibleDescription());
  }

  // ---- Layout ---------------------------------------------------------------

  @Override
  public Dimension getPreferredSize() {
    final int supportingRow = SUPPORTING_TOP_PAD + lineHeight(TypeRole.BODY_SMALL);
    return new Dimension(DEFAULT_WIDTH, CONTAINER_HEIGHT + supportingRow);
  }

  @Override
  public void doLayout() {
    final int w = getWidth();
    final boolean ltr = getComponentOrientation().isLeftToRight();

    if (trailingButton != null) {
      final int btnW = trailingButton.getPreferredSize().width;
      final int btnH = trailingButton.getPreferredSize().height;
      final int btnY = (CONTAINER_HEIGHT - btnH) / 2;
      // Centre the button's glyph where a 24dp icon would sit (PAD_LR_ICON + 12 from the edge).
      final int glyphCenterFromEdge = PAD_LR_ICON + ICON_GLYPH / 2;
      final int btnX = ltr ? w - glyphCenterFromEdge - btnW / 2 : glyphCenterFromEdge - btnW / 2;
      trailingButton.setBounds(btnX, btnY, btnW, btnH);
    }

    final FontMetrics fm = getFontMetrics(TypeRole.BODY_LARGE.resolve());
    final int prefixW = prefix.isEmpty() ? 0 : fm.stringWidth(prefix) + ICON_TEXT_GAP / 2;
    final int suffixW = suffix.isEmpty() ? 0 : fm.stringWidth(suffix) + ICON_TEXT_GAP / 2;

    final int textLeft = leftContentEdge() + (ltr ? prefixW : suffixW);
    final int textRight = w - rightContentEdge() - (ltr ? suffixW : prefixW);

    final int editorH = Math.min(fm.getHeight(), CONTAINER_HEIGHT - 2 * PAD_TOP_BOTTOM);
    final boolean labelled = !label.isEmpty();
    final int editorY =
        labelled ? CONTAINER_HEIGHT - PAD_TOP_BOTTOM - editorH : (CONTAINER_HEIGHT - editorH) / 2;
    editor.setBounds(textLeft, editorY, Math.max(0, textRight - textLeft), editorH);
  }

  private int lineHeight(final TypeRole role) {
    return getFontMetrics(role.resolve()).getHeight();
  }

  // ---- Paint ----------------------------------------------------------------

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int w = getWidth();
      final int arc = ShapeScale.XS.px();
      if (variant == Variant.FILLED) {
        paintFilledChrome(g2, w, arc);
      } else {
        paintOutlinedChrome(g2, w, arc);
      }
      paintIcons(g2, w);
      paintAffixes(g2);
      paintLabel(g2);
      paintSupportingText(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintIcons(final Graphics2D g2, final int w) {
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final Icon trailingSlot = showAutoErrorIcon() ? themedErrorIcon() : trailingIcon;
    final Icon leftSlot = ltr ? leadingIcon : trailingSlot;
    final Icon rightSlot = ltr ? trailingSlot : leadingIcon;
    final int iconY = (CONTAINER_HEIGHT - ICON_GLYPH) / 2;
    if (leftSlot != null) {
      paintIcon(g2, leftSlot, PAD_LR_ICON, iconY);
    }
    if (rightSlot != null) {
      paintIcon(g2, rightSlot, w - PAD_LR_ICON - ICON_GLYPH, iconY);
    }
  }

  /** The auto error glyph, tinted to the {@code error} role (the non-color cue). */
  private Icon themedErrorIcon() {
    final FlatSVGIcon icon = MaterialIcons.error(ICON_GLYPH);
    icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> ColorRole.ERROR.resolve()));
    return icon;
  }

  private void paintIcon(final Graphics2D g2, final Icon icon, final int x, final int y) {
    if (!isEnabled()) {
      final java.awt.Composite old = g2.getComposite();
      g2.setComposite(
          java.awt.AlphaComposite.getInstance(
              java.awt.AlphaComposite.SRC_OVER, StateLayer.disabledContentOpacity()));
      icon.paintIcon(this, g2, x, y);
      g2.setComposite(old);
    } else {
      icon.paintIcon(this, g2, x, y);
    }
  }

  private void paintAffixes(final Graphics2D g2) {
    if (prefix.isEmpty() && suffix.isEmpty()) {
      return;
    }
    final boolean affixVisible = label.isEmpty() || labelMorph.progress() > 0.5f;
    if (!affixVisible) {
      return;
    }
    g2.setFont(TypeRole.BODY_LARGE.resolve());
    g2.setColor(affixColor());
    final int baseline = editor.getY() + getFontMetrics(TypeRole.BODY_LARGE.resolve()).getAscent();
    final boolean ltr = getComponentOrientation().isLeftToRight();
    if (!prefix.isEmpty()) {
      final int x = ltr ? leftContentEdge() : getWidth() - rightContentEdge() - textWidth(prefix);
      g2.drawString(prefix, x, baseline);
    }
    if (!suffix.isEmpty()) {
      final int x = ltr ? getWidth() - rightContentEdge() - textWidth(suffix) : leftContentEdge();
      g2.drawString(suffix, x, baseline);
    }
  }

  private void paintSupportingText(final Graphics2D g2) {
    final String support = displayedSupporting();
    if (support.isEmpty()) {
      return;
    }
    g2.setFont(TypeRole.BODY_SMALL.resolve());
    g2.setColor(supportingColor());
    final int y =
        CONTAINER_HEIGHT
            + SUPPORTING_TOP_PAD
            + getFontMetrics(TypeRole.BODY_SMALL.resolve()).getAscent();
    g2.drawString(support, PAD_LR_NO_ICON, y);
  }

  private int textWidth(final String text) {
    return getFontMetrics(TypeRole.BODY_LARGE.resolve()).stringWidth(text);
  }

  private void paintFilledChrome(final Graphics2D g2, final int w, final int arc) {
    final Color fill = containerFill();
    if (fill != null) {
      g2.setColor(fill);
      g2.fill(topRoundedRect(w, CONTAINER_HEIGHT, arc));
    }
    final int stroke = focused ? FOCUS_STROKE : RESTING_STROKE;
    g2.setColor(indicatorColor());
    g2.fillRect(0, CONTAINER_HEIGHT - stroke, w, stroke);
  }

  private void paintOutlinedChrome(final Graphics2D g2, final int w, final int arc) {
    final int stroke = focused ? FOCUS_STROKE : RESTING_STROKE;
    g2.setColor(indicatorColor());
    g2.setStroke(new java.awt.BasicStroke(stroke));
    g2.draw(outlinedPath(w, arc, stroke));
  }

  /**
   * Builds the outlined container stroke as a rounded rectangle, with a gap punched in the top edge
   * for the floated label (the M3 label-notch). The notch opens during the second half of the float
   * so the gap only appears once the label has risen onto the stroke.
   */
  private Path2D outlinedPath(final int w, final int arc, final int stroke) {
    final float inset = stroke / 2f;
    final float x0 = inset;
    final float y0 = inset;
    final float x1 = w - inset;
    final float y1 = CONTAINER_HEIGHT - inset;
    final float r = arc;

    float gapStart = 0f;
    float gapEnd = 0f;
    final float floatProgress = Easing.EMPHASIZED.ease(labelMorph.progress());
    if (!label.isEmpty() && floatProgress > 0.5f) {
      final Font floated = TypeRole.BODY_SMALL.resolve();
      final int labelW = getFontMetrics(floated).stringWidth(label);
      gapStart = PAD_LR_NO_ICON - LABEL_NOTCH_PAD;
      gapEnd = PAD_LR_NO_ICON + labelW + LABEL_NOTCH_PAD;
    }

    final Path2D path = new Path2D.Float();
    path.moveTo(x0 + r, y0);
    if (gapEnd > gapStart) {
      path.lineTo(Math.max(x0 + r, gapStart), y0);
      path.moveTo(Math.min(x1 - r, gapEnd), y0);
    }
    path.lineTo(x1 - r, y0);
    path.quadTo(x1, y0, x1, y0 + r);
    path.lineTo(x1, y1 - r);
    path.quadTo(x1, y1, x1 - r, y1);
    path.lineTo(x0 + r, y1);
    path.quadTo(x0, y1, x0, y1 - r);
    path.lineTo(x0, y0 + r);
    path.quadTo(x0, y0, x0 + r, y0);
    return path;
  }

  /**
   * Paints the floating label, interpolating size (BODY_LARGE&#8594;BODY_SMALL) and baseline
   * (centered&#8594;top) by the eased float progress. Reduced motion makes the animator snap, so
   * the label jumps between the two end states with no interpolation.
   */
  private void paintLabel(final Graphics2D g2) {
    if (label.isEmpty()) {
      return;
    }
    final float t = Easing.EMPHASIZED.ease(labelMorph.progress());

    final float restPt = TypeRole.BODY_LARGE.pt();
    final float floatPt = TypeRole.BODY_SMALL.pt();
    final Font font = TypeRole.BODY_LARGE.resolve().deriveFont(restPt + (floatPt - restPt) * t);
    g2.setFont(font);

    final float restBaseline = restingLabelBaseline();
    final float floatBaseline = PAD_TOP_BOTTOM + g2.getFontMetrics().getAscent();
    final float y = restBaseline + (floatBaseline - restBaseline) * t;

    final String text = displayLabel();
    final boolean ltr = getComponentOrientation().isLeftToRight();
    final int x =
        ltr
            ? leftContentEdge()
            : getWidth() - rightContentEdge() - g2.getFontMetrics().stringWidth(text);
    g2.setColor(labelColor());
    g2.drawString(text, x, Math.round(y));
  }

  /** The baseline at which the resting (centered) label sits — aligned with the editor's text. */
  private float restingLabelBaseline() {
    final int ascent = getFontMetrics(TypeRole.BODY_LARGE.resolve()).getAscent();
    final int descent = getFontMetrics(TypeRole.BODY_LARGE.resolve()).getDescent();
    return (CONTAINER_HEIGHT + ascent - descent) / 2f;
  }

  // ---- State -> color resolution (full table in S3) -------------------------

  private Color containerFill() {
    if (variant != Variant.FILLED) {
      return null;
    }
    final Color base = ColorRole.SURFACE_CONTAINER_HIGHEST.resolve();
    if (!isEnabled()) {
      return alpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContainerOpacity());
    }
    if (hovered) {
      return StateLayer.HOVER.over(base, ColorRole.ON_SURFACE);
    }
    return base;
  }

  private Color indicatorColor() {
    if (!isEnabled()) {
      return alpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    if (error) {
      // Error beats focus; error+hover deepens via the hover layer over error (research §T5).
      return hovered
          ? StateLayer.HOVER.over(ColorRole.ERROR.resolve(), ColorRole.ON_SURFACE)
          : ColorRole.ERROR.resolve();
    }
    if (focused) {
      return ColorRole.PRIMARY.resolve();
    }
    if (hovered) {
      return ColorRole.ON_SURFACE.resolve();
    }
    return (variant == Variant.FILLED ? ColorRole.ON_SURFACE_VARIANT : ColorRole.OUTLINE).resolve();
  }

  private Color labelColor() {
    if (!isEnabled()) {
      return alpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    if (error) {
      return hovered
          ? StateLayer.HOVER.over(ColorRole.ERROR.resolve(), ColorRole.ON_SURFACE)
          : ColorRole.ERROR.resolve();
    }
    if (focused) {
      return ColorRole.PRIMARY.resolve();
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
  }

  private Color affixColor() {
    if (!isEnabled()) {
      return alpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
  }

  private Color supportingColor() {
    if (!isEnabled()) {
      return alpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    if (error) {
      return ColorRole.ERROR.resolve();
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
  }

  private static Color alpha(final Color base, final float opacity) {
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.round(opacity * 255f));
  }

  private static Path2D topRoundedRect(final int w, final int h, final int radius) {
    final float r = radius;
    final Path2D path = new Path2D.Float();
    path.moveTo(0, h);
    path.lineTo(0, r);
    path.quadTo(0, 0, r, 0);
    path.lineTo(w - r, 0);
    path.quadTo(w, 0, w, r);
    path.lineTo(w, h);
    path.closePath();
    return path;
  }
}
