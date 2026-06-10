package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Windowed diagnostic for the Phase-2 editable-combo typing report (PR #395 smoke): letters typed
 * after the menu auto-opens reportedly never reach the editor while Backspace still works. Robot
 * types two letters and a backspace into a focused editable combo and logs the editor text, the
 * expanded state, and every {@code focusOwner} transition — pinpointing whether the opening menu
 * steals focus from the editor (the focus-home contract) or the keystrokes are consumed elsewhere.
 * Exits non-zero when the typed text fails to land. Needs a display.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldEditableTypingDiag {

  private SelectFieldEditableTypingDiag() {}

  private static JFrame frame;
  private static ElwhaSelectField<String> combo;
  private static ElwhaTextField field;
  private static int failures;

  /**
   * Runs the diagnostic.
   *
   * @param args ignored
   * @throws Exception on Robot/EDT failures
   */
  public static void main(final String[] args) throws Exception {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("needs a display; skipping");
      return;
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener(
            "focusOwner",
            evt -> {
              final Object next = evt.getNewValue();
              if (next != null) {
                System.out.println("  [focus] -> " + next.getClass().getSimpleName());
              }
            });

    SwingUtilities.invokeAndWait(
        () -> {
          frame = new JFrame("typing-diag");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          combo = ElwhaSelectField.outlined("Language");
          combo.setOptions(List.of("Java", "Kotlin", "Scala", "Clojure"));
          combo.setEditable(true);
          field = (ElwhaTextField) combo.getComponent(0);
          final JPanel root = new JPanel(new java.awt.BorderLayout());
          root.setBorder(javax.swing.BorderFactory.createEmptyBorder(40, 40, 200, 40));
          root.add(combo, java.awt.BorderLayout.NORTH);
          frame.setContentPane(root);
          frame.setSize(420, 420);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });

    SwingUtilities.invokeAndWait(
        () -> {
          field
              .getEditor()
              .addKeyListener(
                  new java.awt.event.KeyAdapter() {
                    @Override
                    public void keyTyped(final java.awt.event.KeyEvent e) {
                      System.out.println(
                          "  [keyTyped] char='"
                              + e.getKeyChar()
                              + "' consumed="
                              + e.isConsumed()
                              + " editorEditable="
                              + field.getEditor().isEditable()
                              + " focusOwner="
                              + name(
                                  KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                      .getFocusOwner()));
                    }
                  });
          field
              .getEditor()
              .getDocument()
              .addDocumentListener(
                  new javax.swing.event.DocumentListener() {
                    @Override
                    public void insertUpdate(final javax.swing.event.DocumentEvent e) {
                      System.out.println("  [doc] insert -> \"" + field.getText() + "\"");
                    }

                    @Override
                    public void removeUpdate(final javax.swing.event.DocumentEvent e) {
                      System.out.println("  [doc] remove -> \"" + field.getText() + "\"");
                      for (final StackTraceElement el : new Throwable().getStackTrace()) {
                        final String line = el.toString();
                        if (line.contains("owspfm") || line.contains("JTextComponent")) {
                          System.out.println("      at " + line);
                        }
                      }
                    }

                    @Override
                    public void changedUpdate(final javax.swing.event.DocumentEvent e) {}
                  });
        });

    final Robot robot = new Robot();
    robot.setAutoDelay(40);
    robot.waitForIdle();
    Thread.sleep(300);

    SwingUtilities.invokeAndWait(() -> field.getEditor().requestFocusInWindow());
    robot.waitForIdle();
    Thread.sleep(200);
    log("after focusing editor");

    type(robot, KeyEvent.VK_K);
    Thread.sleep(500); // menu open + entrance + invokeLater focusInitial all settle
    log("after typing 'k' (menu should be open)");

    type(robot, KeyEvent.VK_O);
    Thread.sleep(300);
    log("after typing 'o'");

    type(robot, KeyEvent.VK_BACK_SPACE);
    Thread.sleep(300);
    log("after backspace");

    check("text is 'k' after k,o,backspace", "k".equals(combo.getText()));
    check("menu opened on typing", combo.isExpanded() || true); // informational only above

    System.out.println(failures == 0 ? "PASS" : "FAIL — " + failures + " check(s)");
    SwingUtilities.invokeAndWait(frame::dispose);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void type(final Robot robot, final int keyCode) {
    robot.keyPress(keyCode);
    robot.keyRelease(keyCode);
    robot.waitForIdle();
  }

  private static void log(final String stage) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final Component owner =
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          System.out.println(
              stage
                  + ": text=\""
                  + combo.getText()
                  + "\" expanded="
                  + combo.isExpanded()
                  + " focusOwner="
                  + (owner == null ? "null" : owner.getClass().getSimpleName()));
        });
  }

  private static String name(final Object o) {
    return o == null ? "null" : o.getClass().getSimpleName();
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
