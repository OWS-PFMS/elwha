package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The SLIDERS pane (design doc {@code elwha-color-picker-design.md} §7): the RGB/HSV sub-toggle (a
 * connected {@code ElwhaButtonGroup}, single-required), three context-gradient {@link
 * ColorTrackSlider} channel rows per model, and the validated hex {@code ElwhaTextField} — the
 * picker's precise-definition mode and the analog of the M3 pickers' text-input mode.
 *
 * <p>The HSV rows share the spectrum pane's hue-preservation invariant: pane-owned h/s/v floats
 * survive grey roundtrips. Hex commits on Enter and focus-loss; invalid text raises the field's
 * error state and reverts on focus-loss.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class SlidersPane extends ColorPickerPane {

  /** The pane's channel models — which three rows are showing. */
  enum ChannelModel {
    /** Red / green / blue, 0–255. */
    RGB,
    /** Hue 0–360, saturation and value 0–100. */
    HSV
  }

  private final ElwhaButtonGroup modelToggle;
  private final CardLayout rowCards;
  private final JPanel rowsHost;
  private final ChannelRow[] rgbRows = new ChannelRow[3];
  private final ChannelRow[] hsvRows = new ChannelRow[3];
  private final ElwhaTextField hexField;

  private final ChannelRow alphaRow;

  private ChannelModel channelModel = ChannelModel.RGB;
  private float hueDegrees;
  private float saturation;
  private float value;
  private int alpha;

  SlidersPane(final ElwhaColorPicker picker) {
    super(picker);
    adoptHsv(picker.getColor());
    this.alpha = picker.getColor().getAlpha();

    this.modelToggle =
        ElwhaButtonGroup.connected()
            .add("RGB", "HSV")
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(com.owspfm.elwha.button.ButtonSize.XS);
    modelToggle.setSelectedIndex(0);
    modelToggle.setAlignmentX(LEFT_ALIGNMENT);
    modelToggle.addSelectionListener(
        group -> setChannelModel(group.getSelectedIndex() == 1 ? ChannelModel.HSV : ChannelModel.RGB));

    rgbRows[0] = new ChannelRow("R", 0, 255, (v, adj) -> rgbFromUser(0, v, adj));
    rgbRows[1] = new ChannelRow("G", 0, 255, (v, adj) -> rgbFromUser(1, v, adj));
    rgbRows[2] = new ChannelRow("B", 0, 255, (v, adj) -> rgbFromUser(2, v, adj));
    hsvRows[0] = new ChannelRow("H", 0, 360, (v, adj) -> hsvFromUser(0, v, adj));
    hsvRows[1] = new ChannelRow("S", 0, 100, (v, adj) -> hsvFromUser(1, v, adj));
    hsvRows[2] = new ChannelRow("V", 0, 100, (v, adj) -> hsvFromUser(2, v, adj));
    rgbRows[0].slider.setAccessibleChannelName("Red");
    rgbRows[1].slider.setAccessibleChannelName("Green");
    rgbRows[2].slider.setAccessibleChannelName("Blue");
    hsvRows[0].slider.setAccessibleChannelName("Hue");
    hsvRows[1].slider.setAccessibleChannelName("Saturation");
    hsvRows[2].slider.setAccessibleChannelName("Value");

    this.rowCards = new CardLayout();
    this.rowsHost = new JPanel(rowCards);
    rowsHost.setOpaque(false);
    rowsHost.setAlignmentX(LEFT_ALIGNMENT);
    rowsHost.add(stack(rgbRows), ChannelModel.RGB.name());
    rowsHost.add(stack(hsvRows), ChannelModel.HSV.name());

    this.hexField = ElwhaTextField.outlined("Hex");
    hexField.setAlignmentX(LEFT_ALIGNMENT);
    if (hexField.getEditor() instanceof JTextField textField) {
      textField.addActionListener(e -> commitHexText(hexField.getText()));
    }
    hexField
        .getEditor()
        .addFocusListener(
            new FocusAdapter() {
              @Override
              public void focusLost(final FocusEvent e) {
                if (ColorHex.parse(hexField.getText(), picker.isAlphaEnabled()) == null) {
                  revertHex();
                } else {
                  commitHexText(hexField.getText());
                }
              }
            });

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(modelToggle);
    add(Box.createVerticalStrut(SpaceScale.MD.px()));
    add(rowsHost);
    if (picker.isAlphaEnabled()) {
      this.alphaRow = new ChannelRow("A", 0, 255, this::alphaFromUser);
      alphaRow.slider.setAccessibleChannelName("Alpha");
      alphaRow.slider.setCheckerboardBacking(true);
      alphaRow.setAlignmentX(LEFT_ALIGNMENT);
      add(Box.createVerticalStrut(SpaceScale.SM.px()));
      add(alphaRow);
    } else {
      this.alphaRow = null;
    }
    add(Box.createVerticalStrut(SpaceScale.MD.px()));
    add(hexField);
    refresh(picker.getColor());
  }

  ChannelModel getChannelModel() {
    return channelModel;
  }

  void setChannelModel(final ChannelModel model) {
    this.channelModel = model;
    modelToggle.setSelectedIndex(model == ChannelModel.HSV ? 1 : 0);
    rowCards.show(rowsHost, model.name());
    revalidate();
    repaint();
  }

  void rgbFromUser(final int channel, final int channelValue, final boolean adjusting) {
    final Color current = picker().getColor();
    final int[] rgb = {current.getRed(), current.getGreen(), current.getBlue()};
    rgb[channel] = Math.max(0, Math.min(255, channelValue));
    final Color next = withAlpha(rgb[0], rgb[1], rgb[2]);
    adoptHsv(next);
    commit(next, adjusting);
    refresh(next);
  }

  void hsvFromUser(final int channel, final int channelValue, final boolean adjusting) {
    switch (channel) {
      case 0 -> hueDegrees = Math.max(0, Math.min(360, channelValue));
      case 1 -> saturation = Math.max(0f, Math.min(1f, channelValue / 100f));
      default -> value = Math.max(0f, Math.min(1f, channelValue / 100f));
    }
    final int rgb = Color.HSBtoRGB(hueDegrees / 360f, saturation, value);
    final Color next = withAlpha((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    commit(next, adjusting);
    refresh(next);
  }

  void alphaFromUser(final int channelValue, final boolean adjusting) {
    alpha = Math.max(0, Math.min(255, channelValue));
    final Color current = picker().getColor();
    final Color next = withAlpha(current.getRed(), current.getGreen(), current.getBlue());
    commit(next, adjusting);
    refresh(next);
  }

  void commitHexText(final String text) {
    final boolean allowAlpha = picker().isAlphaEnabled();
    final Color parsed = ColorHex.parse(text, allowAlpha);
    if (parsed == null) {
      hexField.setError(true);
      hexField.setErrorText(allowAlpha ? "Use #RRGGBB or #RRGGBBAA" : "Use #RRGGBB");
      return;
    }
    hexField.setError(false);
    adoptHsv(parsed);
    if (allowAlpha) {
      alpha = parsed.getAlpha();
    }
    commit(parsed, false);
    refresh(picker().getColor());
  }

  private Color withAlpha(final int red, final int green, final int blue) {
    return picker().isAlphaEnabled()
        ? new Color(red, green, blue, alpha)
        : new Color(red, green, blue);
  }

  boolean isHexError() {
    return hexField.isError();
  }

  String hexText() {
    return hexField.getText();
  }

  void revertHex() {
    hexField.setError(false);
    hexField.setText(ColorHex.format(picker().getColor(), picker().isAlphaEnabled()));
  }

  int alphaValue() {
    return alphaRow != null ? alphaRow.slider.value() : 255;
  }

  int rgbValue(final int channel) {
    return rgbRows[channel].slider.value();
  }

  int hsvValue(final int channel) {
    return hsvRows[channel].slider.value();
  }

  @Override
  void syncFromPicker(final Color color) {
    adoptHsv(color);
    refresh(color);
  }

  private void adoptHsv(final Color color) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    if (hsb[1] > 0f && hsb[2] > 0f) {
      hueDegrees = hsb[0] * 360f;
    }
    if (hsb[2] > 0f) {
      saturation = hsb[1];
    }
    value = hsb[2];
  }

  private void refresh(final Color color) {
    rgbRows[0].slider.setValue(color.getRed());
    rgbRows[1].slider.setValue(color.getGreen());
    rgbRows[2].slider.setValue(color.getBlue());
    hsvRows[0].slider.setValue(Math.round(hueDegrees));
    hsvRows[1].slider.setValue(Math.round(saturation * 100f));
    hsvRows[2].slider.setValue(Math.round(value * 100f));
    if (alphaRow != null) {
      alpha = color.getAlpha();
      alphaRow.slider.setValue(alpha);
      final int rgb = color.getRGB() & 0xFFFFFF;
      alphaRow.slider.setTrackStops(new Color(rgb, true), new Color(rgb));
    }
    updateTracks(color);
    final String formatted = ColorHex.format(color, picker().isAlphaEnabled());
    if (!hexField.isError() && !formatted.equals(hexField.getText())) {
      hexField.setText(formatted);
    }
    repaint();
  }

  private void updateTracks(final Color color) {
    final int r = color.getRed();
    final int g = color.getGreen();
    final int b = color.getBlue();
    rgbRows[0].slider.setTrackStops(new Color(0, g, b), new Color(255, g, b));
    rgbRows[1].slider.setTrackStops(new Color(r, 0, b), new Color(r, 255, b));
    rgbRows[2].slider.setTrackStops(new Color(r, g, 0), new Color(r, g, 255));
    final Color[] rainbow = new Color[7];
    for (int i = 0; i < 7; i++) {
      rainbow[i] = Color.getHSBColor(i / 6f, 1f, 1f);
    }
    hsvRows[0].slider.setTrackStops(rainbow);
    hsvRows[1].slider.setTrackStops(
        new Color(Color.HSBtoRGB(hueDegrees / 360f, 0f, value)),
        new Color(Color.HSBtoRGB(hueDegrees / 360f, 1f, value)));
    hsvRows[2].slider.setTrackStops(
        new Color(Color.HSBtoRGB(hueDegrees / 360f, saturation, 0f)),
        new Color(Color.HSBtoRGB(hueDegrees / 360f, saturation, 1f)));
  }

  private static JComponent stack(final ChannelRow[] rows) {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    for (int i = 0; i < rows.length; i++) {
      if (i > 0) {
        panel.add(Box.createVerticalStrut(SpaceScale.SM.px()));
      }
      rows[i].setAlignmentX(LEFT_ALIGNMENT);
      panel.add(rows[i]);
    }
    return panel;
  }

  /** One channel row — painted label and value flanking a hosted ColorTrackSlider. */
  private static final class ChannelRow extends JComponent {

    private static final int LABEL_WIDTH = 20;
    private static final int VALUE_WIDTH = 40;

    private final String label;
    final ColorTrackSlider slider;

    ChannelRow(
        final String label,
        final int min,
        final int max,
        final ColorTrackSlider.ValueListener listener) {
      this.label = label;
      this.slider = new ColorTrackSlider(min, max, min);
      slider.setListener(listener);
      setOpaque(false);
      add(slider);
    }

    @Override
    public void doLayout() {
      final int gap = SpaceScale.SM.px();
      slider.setBounds(
          LABEL_WIDTH + gap,
          0,
          Math.max(0, getWidth() - LABEL_WIDTH - VALUE_WIDTH - 2 * gap),
          getHeight());
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(160, ColorTrackSlider.COMPONENT_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, ColorTrackSlider.COMPONENT_HEIGHT);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final Font labelFont = TypeRole.LABEL_LARGE.resolve();
        FontMetrics fm = g2.getFontMetrics(labelFont);
        g2.setFont(labelFont);
        g2.setColor(ColorRole.ON_SURFACE_VARIANT.resolve());
        g2.drawString(label, 0, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        final Font valueFont = TypeRole.BODY_MEDIUM.resolve();
        fm = g2.getFontMetrics(valueFont);
        g2.setFont(valueFont);
        g2.setColor(ColorRole.ON_SURFACE.resolve());
        final String valueText = Integer.toString(slider.value());
        g2.drawString(
            valueText,
            getWidth() - fm.stringWidth(valueText),
            (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
      } finally {
        g2.dispose();
      }
    }

    @Override
    public void setEnabled(final boolean enabled) {
      super.setEnabled(enabled);
      for (final Component child : getComponents()) {
        child.setEnabled(enabled);
      }
    }
  }
}
