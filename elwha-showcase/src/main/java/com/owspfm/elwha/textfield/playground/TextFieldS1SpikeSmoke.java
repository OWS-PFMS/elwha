package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.accessibility.AccessibleContext;

/**
 * S1 headless guard (#333): the spike's risk is the {@link ElwhaTextField} decorator boundary and
 * the error&#8594;"alert" accessibility mechanism. This exercises both without a display — every
 * variant&#215;state combination paints to an offscreen buffer without throwing, and an {@link
 * AccessibleContext} listener confirms {@code setError(true)} fires the alert property change on
 * the embedded editor.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS1SpikeSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS1SpikeSmoke {

  private TextFieldS1SpikeSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    for (final ElwhaTextField.Variant variant : ElwhaTextField.Variant.values()) {
      final ElwhaTextField field = new ElwhaTextField(variant, "Label");
      paints("enabled/" + variant, field);

      field.setText("value");
      paints("populated/" + variant, field);

      field.setEnabled(false);
      paints("disabled/" + variant, field);
      field.setEnabled(true);

      field.setReadOnly(true);
      paints("read-only/" + variant, field);
      field.setReadOnly(false);

      field.setError(true);
      paints("error/" + variant, field);
    }

    check("decorator embeds a JTextField", new ElwhaTextField().getEditor() != null);

    final ElwhaTextField alertField = new ElwhaTextField("Alert");
    final AtomicBoolean fired = new AtomicBoolean(false);
    final AccessibleContext ctx = alertField.getEditor().getAccessibleContext();
    ctx.addPropertyChangeListener(
        new PropertyChangeListener() {
          @Override
          public void propertyChange(final PropertyChangeEvent evt) {
            if (AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY.equals(evt.getPropertyName())) {
              fired.set(true);
            }
          }
        });
    alertField.setError(true);
    check("setError(true) fires the accessibility alert property change", fired.get());

    System.out.println(
        failures == 0 ? "PASS — all S1 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void paints(final String label, final ElwhaTextField field) {
    try {
      field.setSize(field.getPreferredSize());
      field.doLayout();
      final BufferedImage img =
          new BufferedImage(
              Math.max(1, field.getWidth()),
              Math.max(1, field.getHeight()),
              BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g = img.createGraphics();
      field.paint(g);
      g.dispose();
      check("paints " + label, true);
    } catch (final RuntimeException ex) {
      check("paints " + label + " (threw " + ex + ")", false);
    }
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
