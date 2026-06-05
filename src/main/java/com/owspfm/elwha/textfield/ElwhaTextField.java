package com.owspfm.elwha.textfield;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.StateLayer;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
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
  static final int SUPPORTING_TOP_PAD = SpaceScale.XS.px(); // 4
  static final int RESTING_STROKE = 1;
  static final int FOCUS_STROKE = 3; // Expressive bump (design §4; resting 1dp)
  static final int DEFAULT_WIDTH = 245; // M3 default layout width
  static final int MAX_WIDTH = 488; // M3 maximum width

  private Variant variant;
  private final JTextField editor = new JTextField();

  private String label = "";

  private boolean hovered;
  private boolean focused;
  private boolean error;

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
    editor
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                repaint();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                repaint();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                repaint();
              }
            });
  }

  private void initInteraction() {
    editor.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            focused = true;
            repaint();
          }

          @Override
          public void focusLost(final FocusEvent e) {
            focused = false;
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
    if (error) {
      fireAccessibleAlert();
    }
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
    editor.getAccessibleContext().setAccessibleName(label);
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
    final String old = ctx.getAccessibleDescription();
    final String announcement = error ? "Error" : null;
    ctx.setAccessibleDescription(announcement);
    ctx.firePropertyChange(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY, old, announcement);
  }

  // ---- Layout ---------------------------------------------------------------

  @Override
  public Dimension getPreferredSize() {
    final int supportingRow = SUPPORTING_TOP_PAD + lineHeight(TypeRole.BODY_SMALL);
    return new Dimension(DEFAULT_WIDTH, CONTAINER_HEIGHT + supportingRow);
  }

  @Override
  public void doLayout() {
    final int pad = PAD_LR_NO_ICON;
    final int editorH =
        Math.min(lineHeight(TypeRole.BODY_LARGE), CONTAINER_HEIGHT - 2 * PAD_TOP_BOTTOM);
    final boolean labelled = !label.isEmpty();
    final int editorY =
        labelled ? CONTAINER_HEIGHT - PAD_TOP_BOTTOM - editorH : (CONTAINER_HEIGHT - editorH) / 2;
    editor.setBounds(pad, editorY, Math.max(0, getWidth() - 2 * pad), editorH);
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
      paintLabel(g2);
    } finally {
      g2.dispose();
    }
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
    final float inset = stroke / 2f;
    final RoundRectangle2D outline =
        new RoundRectangle2D.Float(
            inset, inset, w - stroke, CONTAINER_HEIGHT - stroke, arc * 2f, arc * 2f);
    g2.setColor(indicatorColor());
    g2.setStroke(new java.awt.BasicStroke(stroke));
    g2.draw(outline);
  }

  /** The label-float mechanism (S1: snap to floated when focused or populated; S2 animates). */
  private void paintLabel(final Graphics2D g2) {
    if (label.isEmpty()) {
      return;
    }
    final boolean floated = focused || !editor.getText().isEmpty();
    final TypeRole role = floated ? TypeRole.BODY_SMALL : TypeRole.BODY_LARGE;
    g2.setFont(role.resolve());
    g2.setColor(labelColor());
    final int x = PAD_LR_NO_ICON;
    final int y =
        floated
            ? PAD_TOP_BOTTOM + g2.getFontMetrics().getAscent()
            : (CONTAINER_HEIGHT
                    + g2.getFontMetrics().getAscent()
                    - g2.getFontMetrics().getDescent())
                / 2;
    g2.drawString(label, x, y);
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
      return ColorRole.ERROR.resolve();
    }
    if (focused) {
      return ColorRole.PRIMARY.resolve();
    }
    final ColorRole resting =
        variant == Variant.FILLED ? ColorRole.ON_SURFACE_VARIANT : ColorRole.OUTLINE;
    if (hovered) {
      return ColorRole.ON_SURFACE.resolve();
    }
    return resting.resolve();
  }

  private Color labelColor() {
    if (!isEnabled()) {
      return alpha(ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());
    }
    if (error) {
      return ColorRole.ERROR.resolve();
    }
    if (focused) {
      return ColorRole.PRIMARY.resolve();
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
