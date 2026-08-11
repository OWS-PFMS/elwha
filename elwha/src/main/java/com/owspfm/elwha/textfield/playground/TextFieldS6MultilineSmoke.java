package com.owspfm.elwha.textfield.playground;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.textfield.ElwhaTextField.InputMode;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * S6 headless guard (#353): the multi-line / text-area editor swap. Asserts that switching {@link
 * InputMode} swaps the embedded editor between {@link JTextField} and {@link JTextArea} (preserving
 * text + state), that auto-grow grows the container while a fixed text area stays put and scrolls,
 * that the input top-anchors in multi-line, and that error / disabled / read-only carry across a
 * mode swap — across both variants.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.textfield.playground.TextFieldS6MultilineSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldS6MultilineSmoke {

  private TextFieldS6MultilineSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    // --- editor swap: single -> multi -> text-area -> back ---------------------
    final ElwhaTextField f = ElwhaTextField.outlined("Notes");
    check("single-line editor is a JTextField", f.getEditor() instanceof JTextField);
    check("default mode is SINGLE_LINE", f.getInputMode() == InputMode.SINGLE_LINE);
    check("single-line is not multiline", !f.isMultiline());

    f.setText("kept across swaps");
    f.setInputMode(InputMode.MULTI_LINE);
    check("multi-line editor is a JTextArea", f.getEditor() instanceof JTextArea);
    check("text preserved across the editor swap", "kept across swaps".equals(f.getText()));
    check("multi-line reports multiline", f.isMultiline());
    check("auto-grow area wraps lines", ((JTextArea) f.getEditor()).getLineWrap());

    f.setInputMode(InputMode.TEXT_AREA);
    check("text-area editor is a JTextArea", f.getEditor() instanceof JTextArea);
    check("text-area is hosted in a JScrollPane", hasScrollPaneChild(f));
    check("text preserved into the text-area swap", "kept across swaps".equals(f.getText()));

    f.setInputMode(InputMode.SINGLE_LINE);
    check("switching back restores a JTextField", f.getEditor() instanceof JTextField);
    check("no scroll-pane child remains in single-line", !hasScrollPaneChild(f));

    // --- auto-grow grows; text-area is fixed and scrolls -----------------------
    for (final Variant v : Variant.values()) {
      final ElwhaTextField grow = new ElwhaTextField(v, "Message");
      grow.setInputMode(InputMode.MULTI_LINE);
      layoutAt(grow, 240);
      final int oneLine = grow.getPreferredSize().height;
      grow.setText("line one\nline two\nline three\nline four\nline five");
      layoutAt(grow, 240);
      final int manyLines = grow.getPreferredSize().height;
      check(v + " auto-grow container grows with content", manyLines > oneLine);
      check(
          v + " auto-grow input top-anchors (not centred)",
          grow.getEditor().getY() < grow.getPreferredSize().height / 2);

      final ElwhaTextField area = new ElwhaTextField(v, "Bio");
      area.setInputMode(InputMode.TEXT_AREA);
      area.setRows(3);
      layoutAt(area, 240);
      final int fixed = area.getPreferredSize().height;
      area.setText("a\nb\nc\nd\ne\nf\ng\nh"); // exceeds 3 rows -> scrolls, no growth
      layoutAt(area, 240);
      check(
          v + " fixed text-area height does not grow with content",
          area.getPreferredSize().height == fixed);

      area.setRows(6);
      layoutAt(area, 240);
      check(v + " more rows opens a taller text area", area.getPreferredSize().height > fixed);
    }

    // --- state carries across a mode swap --------------------------------------
    final ElwhaTextField st = ElwhaTextField.filled("Field");
    st.setError(true);
    st.setErrorText("required");
    st.setReadOnly(true);
    st.setInputMode(InputMode.MULTI_LINE);
    check("error state carries across the swap", st.isError());
    check("read-only carries across the swap", st.isReadOnly());
    check(
        "errored multi-line keeps the error in the accessible name",
        st.getEditor().getAccessibleContext().getAccessibleName().contains("required"));

    final ElwhaTextField dis = ElwhaTextField.outlined("Field");
    dis.setEnabled(false);
    dis.setInputMode(InputMode.TEXT_AREA);
    check("disabled state carries across the swap", !dis.getEditor().isEnabled());

    // --- a11y surface survives -------------------------------------------------
    final ElwhaTextField a11y = ElwhaTextField.filled("Comment");
    a11y.setInputMode(InputMode.MULTI_LINE);
    check("JTextArea keeps its AccessibleContext", a11y.getEditor().getAccessibleContext() != null);

    System.out.println(
        failures == 0 ? "PASS — all S6 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static boolean hasScrollPaneChild(final ElwhaTextField field) {
    for (final java.awt.Component c : field.getComponents()) {
      if (c instanceof JScrollPane) {
        return true;
      }
    }
    return false;
  }

  private static void layoutAt(final ElwhaTextField field, final int width) {
    field.setSize(width, field.getPreferredSize().height);
    field.doLayout();
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
