package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.tooltip.ElwhaTooltip;
import com.owspfm.elwha.tooltip.TooltipAlignment;
import com.owspfm.elwha.tooltip.TooltipPlacement;
import com.owspfm.elwha.tooltip.TooltipVariant;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaTooltip} (story #452): a {@link
 * ComponentWorkbench} stage hosting a live anchor with the configured tooltip attached — variant
 * (plain / rich / persistent rich), texts, action count, placement, alignment, and both trigger
 * delays — plus a programmatic "Show now", with the generated-code view tracking every control; and
 * a static gallery of {@code renderPreview()} tiles covering the plain short / wrapped forms and
 * the rich subhead / no-subhead / one- and two-action forms. Boolean controls and text inputs
 * dogfood {@code ElwhaCheckbox} / {@code ElwhaTextField}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class TooltipShowcasePanels {

  private TooltipShowcasePanels() {}

  /** Builds the full leaf surface — Workbench + Gallery tabs. */
  static JComponent buildComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildWorkbench());
    tabs.addTab("Gallery", buildGallery());
    return tabs;
  }

  /** Builds the interactive Workbench (live anchored tooltip + control rail + generated code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<TooltipVariant> variantBox = new JComboBox<>(TooltipVariant.values());
    final ElwhaCheckbox persistentCtl = new ElwhaCheckbox("Persistent (rich only)");
    final ElwhaTextField textCtl = ElwhaTextField.outlined("");
    textCtl.setText("Save to favorites");
    final ElwhaTextField subheadCtl = ElwhaTextField.outlined("");
    subheadCtl.setText("Rich tooltip");
    final ElwhaTextField supportingCtl = ElwhaTextField.outlined("");
    supportingCtl.setText("Supporting text wraps at 320 px and sits on the M3 baseline rhythm.");
    final JComboBox<Integer> actionsBox = new JComboBox<>(new Integer[] {0, 1, 2});
    final JComboBox<TooltipPlacement> placementBox = new JComboBox<>(TooltipPlacement.values());
    final JComboBox<TooltipAlignment> alignmentBox = new JComboBox<>(TooltipAlignment.values());
    alignmentBox.setSelectedItem(TooltipAlignment.CENTER);
    final JSpinner showDelay = new JSpinner(new SpinnerNumberModel(500, 0, 5000, 100));
    final JSpinner hideDelay = new JSpinner(new SpinnerNumberModel(600, 0, 5000, 100));
    final ElwhaButton showNow = ElwhaButton.textButton("Show now");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Tooltip");
    controls.addControl("Variant", variantBox);
    controls.addControl("", persistentCtl);
    controls.addControl("Text (plain)", textCtl);
    controls.addControl("Subhead (rich)", subheadCtl);
    controls.addControl("Supporting (rich)", supportingCtl);
    controls.addControl("Actions (rich)", actionsBox);
    controls.addSection("Placement");
    controls.addControl("Prefer", placementBox);
    controls.addControl("Alignment", alignmentBox);
    controls.addSection("Triggers");
    controls.addControl("Show delay ms", showDelay);
    controls.addControl("Hide delay ms", hideDelay);
    controls.addControl("", showNow);

    final AtomicReference<ElwhaTooltip> subject = new AtomicReference<>();
    final AtomicReference<ElwhaButton> anchorRef = new AtomicReference<>();

    final Runnable apply =
        () -> {
          final ElwhaTooltip previous = subject.get();
          if (previous != null) {
            previous.detach();
          }
          final TooltipVariant variant = (TooltipVariant) variantBox.getSelectedItem();
          final boolean persistent = persistentCtl.isChecked() && variant == TooltipVariant.RICH;
          final ElwhaTooltip tip;
          if (variant == TooltipVariant.PLAIN) {
            tip = ElwhaTooltip.plain(orFallback(textCtl.getText(), "Save to favorites"));
          } else {
            final ElwhaTooltip.RichBuilder builder =
                ElwhaTooltip.rich()
                    .supportingText(orFallback(supportingCtl.getText(), "Supporting text"))
                    .persistent(persistent);
            if (!subheadCtl.getText().isBlank()) {
              builder.subhead(subheadCtl.getText());
            }
            final int actionCount = (Integer) actionsBox.getSelectedItem();
            if (actionCount >= 1) {
              builder.action("Learn more", e -> System.out.println("tooltip action: learn more"));
            }
            if (actionCount >= 2) {
              builder.action("Dismiss", e -> System.out.println("tooltip action: dismiss"));
            }
            tip = builder.build();
          }
          tip.setPreferredPlacement((TooltipPlacement) placementBox.getSelectedItem());
          tip.setAlignment((TooltipAlignment) alignmentBox.getSelectedItem());
          tip.setShowDelayMs((Integer) showDelay.getValue());
          tip.setHideDelayMs((Integer) hideDelay.getValue());

          final ElwhaButton anchor =
              ElwhaButton.filledTonalButton(persistent ? "Click me" : "Hover me");
          tip.attach(anchor);
          subject.set(tip);
          anchorRef.set(anchor);
          workbench.setStage(stage(anchor));
          workbench.setCode(
              renderCode(
                  variant,
                  persistent,
                  textCtl.getText(),
                  subheadCtl.getText(),
                  supportingCtl.getText(),
                  (Integer) actionsBox.getSelectedItem(),
                  (TooltipPlacement) placementBox.getSelectedItem(),
                  (TooltipAlignment) alignmentBox.getSelectedItem()));
        };

    showNow.addActionListener(
        e -> {
          final ElwhaTooltip tip = subject.get();
          final ElwhaButton anchor = anchorRef.get();
          if (tip != null && anchor != null && anchor.isShowing()) {
            tip.show(anchor);
          }
        });

    variantBox.addActionListener(e -> apply.run());
    persistentCtl.addActionListener(e -> apply.run());
    onChange(textCtl, apply);
    onChange(subheadCtl, apply);
    onChange(supportingCtl, apply);
    actionsBox.addActionListener(e -> apply.run());
    placementBox.addActionListener(e -> apply.run());
    alignmentBox.addActionListener(e -> apply.run());
    showDelay.addChangeListener(e -> apply.run());
    hideDelay.addChangeListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  /** Builds the static gallery — {@code renderPreview()} tiles per configuration. */
  static JComponent buildGallery() {
    final JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));

    addTile(column, "Plain", ElwhaTooltip.plain("Save to favorites").renderPreview());
    addTile(
        column,
        "Plain — wrapped at 200 px",
        ElwhaTooltip.plain(
                "Plain tooltips wrap their label by hand once it would exceed the M3 maximum"
                    + " width.")
            .renderPreview());
    addTile(
        column,
        "Rich — subhead + supporting text",
        ElwhaTooltip.rich()
            .subhead("Rich tooltip")
            .supportingText(
                "Rich tooltips bring attention to a particular element or feature that warrants"
                    + " the user's attention.")
            .build()
            .renderPreview());
    addTile(
        column,
        "Rich — no subhead",
        ElwhaTooltip.rich()
            .supportingText("Without a subhead the supporting text starts on its own baseline.")
            .build()
            .renderPreview());
    addTile(
        column,
        "Rich — one action",
        ElwhaTooltip.rich()
            .subhead("Grant value")
            .supportingText("The closing price is from the day before the grant date.")
            .action("Learn more", e -> System.out.println("learn more"))
            .build()
            .renderPreview());
    addTile(
        column,
        "Rich — two actions",
        ElwhaTooltip.rich()
            .subhead("Delete forever?")
            .supportingText("Two start-aligned text buttons on the 36 px action row.")
            .action("Learn more", e -> System.out.println("learn more"))
            .action("Dismiss", e -> System.out.println("dismiss"))
            .build()
            .renderPreview());

    final JScrollPane scroll = new JScrollPane(column);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    return scroll;
  }

  private static void addTile(final JPanel column, final String caption, final JComponent tile) {
    final JLabel label = new JLabel(caption);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));
    column.add(label);
    final JPanel holder = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));
    holder.setOpaque(false);
    holder.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    holder.add(tile);
    column.add(holder);
  }

  private static JComponent stage(final ElwhaButton anchor) {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalGlue());
    final JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
    row.setOpaque(false);
    row.add(anchor);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, anchor.getPreferredSize().height + 8));
    row.setAlignmentX(0.5f);
    panel.add(row);
    panel.add(Box.createVerticalGlue());
    return panel;
  }

  private static String orFallback(final String value, final String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String renderCode(
      final TooltipVariant variant,
      final boolean persistent,
      final String text,
      final String subhead,
      final String supporting,
      final int actionCount,
      final TooltipPlacement placement,
      final TooltipAlignment alignment) {
    final StringBuilder code = new StringBuilder(280);
    if (variant == TooltipVariant.PLAIN) {
      code.append("ElwhaTooltip tip = ElwhaTooltip.plain(\"").append(text).append("\");\n");
    } else {
      code.append("ElwhaTooltip tip = ElwhaTooltip.rich()\n");
      if (subhead != null && !subhead.isBlank()) {
        code.append("    .subhead(\"").append(subhead).append("\")\n");
      }
      code.append("    .supportingText(\"").append(supporting).append("\")\n");
      if (actionCount >= 1) {
        code.append("    .action(\"Learn more\", e -> openDocs())\n");
      }
      if (actionCount >= 2) {
        code.append("    .action(\"Dismiss\", e -> {})\n");
      }
      if (persistent) {
        code.append("    .persistent(true)\n");
      }
      code.append("    .build();\n");
    }
    if (placement != TooltipPlacement.ABOVE) {
      code.append("tip.setPreferredPlacement(TooltipPlacement.").append(placement).append(");\n");
    }
    code.append("tip.setAlignment(TooltipAlignment.").append(alignment).append(");\n");
    code.append("tip.attach(anchor);");
    return code.toString();
  }

  private static void onChange(final ElwhaTextField control, final Runnable callback) {
    final DocumentListener listener =
        new DocumentListener() {
          @Override
          public void insertUpdate(final DocumentEvent e) {
            SwingUtilities.invokeLater(callback);
          }

          @Override
          public void removeUpdate(final DocumentEvent e) {
            SwingUtilities.invokeLater(callback);
          }

          @Override
          public void changedUpdate(final DocumentEvent e) {
            SwingUtilities.invokeLater(callback);
          }
        };
    control.getEditor().getDocument().addDocumentListener(listener);
  }
}
