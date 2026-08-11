package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleContext;

/**
 * S5 headless guard (#337): the error contract and its accessibility wiring. Asserts that error
 * text replaces the supporting line without changing the reserved height, the auto error icon
 * reserves the trailing slot (but stays suppressed when the consumer owns the trailing slot), and
 * the error alert fires "supporting text, then error" on the embedded editor.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS5ErrorSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS5ErrorSmoke {

  private TextFieldS5ErrorSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    final ElwhaTextField f = ElwhaTextField.outlined("Email");
    f.setSupportingText("info line");
    final int heightBefore = f.getPreferredSize().height;
    final int editorWBefore = editorWidth(f);

    f.setError(true);
    f.setErrorText("Bad email");
    check("height unchanged on error (reserved row)", f.getPreferredSize().height == heightBefore);
    check(
        "error text replaces supporting in the accessible name",
        f.getEditor().getAccessibleContext().getAccessibleName().contains("Bad email"));
    check(
        "supporting text is no longer in the accessible name",
        !f.getEditor().getAccessibleContext().getAccessibleName().contains("info line"));
    check(
        "accessible description carries supporting then error",
        "info line, Bad email"
            .equals(f.getEditor().getAccessibleContext().getAccessibleDescription()));
    check("auto error icon reserves the trailing slot", editorWidth(f) < editorWBefore);

    // Alert fires with the composed message.
    final ElwhaTextField alertField = ElwhaTextField.filled("Field");
    alertField.setSupportingText("hint");
    final AtomicReference<Object> last = new AtomicReference<>();
    alertField
        .getEditor()
        .getAccessibleContext()
        .addPropertyChangeListener(
            new PropertyChangeListener() {
              @Override
              public void propertyChange(final PropertyChangeEvent evt) {
                if (AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY.equals(evt.getPropertyName())
                    && evt.getNewValue() != null) {
                  last.set(evt.getNewValue());
                }
              }
            });
    alertField.setError(true);
    alertField.setErrorText("Required");
    check("alert announces 'supporting, then error'", "hint, Required".equals(last.get()));

    // Consumer-owned trailing slot suppresses the auto error icon.
    final ElwhaTextField owned = ElwhaTextField.filled("Pwd");
    owned.setTrailingIconButton(new ElwhaIconButton(MaterialIcons.visibility()));
    final int ownedEditorW = editorWidth(owned);
    owned.setError(true);
    owned.setErrorText("Too short");
    check(
        "consumer trailing slot suppresses the auto error icon (no extra reservation)",
        editorWidth(owned) == ownedEditorW);

    check("MaterialIcons.error() resolves the bundled glyph", MaterialIcons.error() != null);

    System.out.println(
        failures == 0 ? "PASS — all S5 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static int editorWidth(final ElwhaTextField field) {
    field.setSize(field.getPreferredSize());
    field.doLayout();
    return field.getEditor().getWidth();
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
