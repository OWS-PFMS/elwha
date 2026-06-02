package com.owspfm.elwha.showcase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Headless regression guard for #317: the active Showcase + playground demo packages must not
 * reintroduce a raw {@code new JButton(...)} where an {@link com.owspfm.elwha.button.ElwhaButton}
 * belongs. Scans the swept source directories on disk and fails if any {@code new JButton} appears
 * outside a small, explicitly-justified allowlist.
 *
 * <p>Source-level rather than runtime because the swept sites span ~20 demo classes (each a
 * separate {@code main}); a static scan covers them all without standing every playground up. The
 * allowlist holds the one legitimate survivor — {@code ThemePlayground}'s default button, which is
 * handed to {@link javax.swing.JRootPane#setDefaultButton(javax.swing.JButton)} and so must be a
 * {@code JButton} ({@code ElwhaButton} extends {@code JComponent}, not {@code JButton}). The frozen
 * {@code card/v1/} legacy and {@code card/fixes/} demos are deliberately out of scope and not
 * scanned.
 *
 * <p>Run from the module root (where {@code mvn exec:java} puts the working directory). Exits
 * non-zero if a stray {@code new JButton} is found, or if the source tree cannot be located (so a
 * wrong working directory fails loudly rather than passing vacuously).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class JButtonSweepGuard {

  private static final Pattern RAW_JBUTTON = Pattern.compile("new\\s+JButton\\s*\\(");

  // Swept directories (relative to src/main/java/com/owspfm/elwha).
  private static final List<String> SCOPE =
      List.of(
          "showcase",
          "dialog/playground",
          "theme/playground",
          "badge/playground",
          "fab/playground",
          "chip/playground");

  // The one justified survivor: file simple-name -> reason. Lines matching here are exempt.
  private static final Map<String, String> ALLOWLIST =
      Map.of(
          "ThemePlayground.java", "default button passed to JRootPane.setDefaultButton(JButton)");

  private JButtonSweepGuard() {}

  public static void main(final String[] args) throws IOException {
    final Path base = Path.of("src/main/java/com/owspfm/elwha");
    if (!Files.isDirectory(base)) {
      System.err.println(
          "FAIL — source root not found at "
              + base.toAbsolutePath()
              + " (run from the module root, e.g. via mvn exec:java)");
      System.exit(2);
    }

    final List<String> violations = new ArrayList<>();
    int scanned = 0;
    for (final String dir : SCOPE) {
      final Path root = base.resolve(dir);
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> tree = Files.walk(root)) {
        for (final Path file : (Iterable<Path>) tree.filter(JButtonSweepGuard::isJava)::iterator) {
          final String name = file.getFileName().toString();
          if (name.equals("JButtonSweepGuard.java")) {
            continue; // this guard's own Javadoc names the pattern it forbids
          }
          scanned++;
          final List<String> lines = Files.readAllLines(file);
          for (int i = 0; i < lines.size(); i++) {
            if (RAW_JBUTTON.matcher(lines.get(i)).find() && !ALLOWLIST.containsKey(name)) {
              violations.add(base.relativize(file) + ":" + (i + 1) + "  " + lines.get(i).trim());
            }
          }
        }
      }
    }

    System.out.println("Scanned " + scanned + " files across " + SCOPE.size() + " swept dirs.");
    System.out.println(
        "Allowlisted survivor(s): "
            + ALLOWLIST.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .toList());
    if (violations.isEmpty()) {
      System.out.println("\nPASS — no stray raw `new JButton` in the swept packages.");
      System.exit(0);
    }
    System.out.println("\nFAIL — " + violations.size() + " stray raw `new JButton`:");
    violations.forEach(v -> System.out.println("  " + v));
    System.exit(1);
  }

  private static boolean isJava(final Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java");
  }
}
