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
 * The shared source scan behind the dogfood sweep guards (#317, #424, #321): the storefront must
 * not reintroduce a raw Swing control where an Elwha primitive exists.
 *
 * <p>Source-level rather than runtime, because the swept sites span ~70 classes each with its own
 * {@code main}; a static scan covers them all without standing every playground up.
 *
 * <p><strong>The guarded surface</strong> is the storefront: the {@code showcase} package plus
 * every {@code playground} package under the library root, discovered by walking rather than
 * listed, so a new component's playground is guarded the day it lands. Story-time {@code *Demo} /
 * {@code *Smoke} / {@code *Diag} mains that sit directly in a component package are outside it — a
 * raw control is frequently the deliberate substrate of what such a proof measures (a tooltip over
 * a plain Swing anchor, a bare {@code JButton}'s listener count), and guarding those directories
 * would ossify their internals. The frozen {@code card/fixes/} harnesses are outside it for the
 * same reason #317 excluded them.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class RawSwingSweep {

  private static final Path SOURCE_ROOT = Path.of("src/main/java/com/owspfm/elwha");

  private RawSwingSweep() {}

  /**
   * The raw-construction sites of {@code widget} inside the guarded surface, minus the allowlist.
   *
   * @param widget the raw Swing simple name, e.g. {@code JComboBox}
   * @param allowlist file simple name to the reason that file may keep raw sites
   * @return one {@code path:line source} entry per violation, empty when clean
   * @throws IOException if the source tree cannot be read
   */
  static List<String> violations(final String widget, final Map<String, String> allowlist)
      throws IOException {
    final Pattern raw = Pattern.compile("new\\s+" + widget + "\\s*[(<]");
    final List<String> violations = new ArrayList<>();
    for (final Path file : guardedFiles()) {
      final String name = file.getFileName().toString();
      if (allowlist.containsKey(name) || name.endsWith("SweepGuard.java")) {
        continue;
      }
      final List<String> lines = Files.readAllLines(file);
      for (int i = 0; i < lines.size(); i++) {
        if (raw.matcher(lines.get(i)).find()) {
          violations.add(SOURCE_ROOT.relativize(file) + ":" + (i + 1) + "  " + lines.get(i).trim());
        }
      }
    }
    return violations;
  }

  /**
   * Runs one widget's scan and reports it. Exits the JVM non-zero on any violation, or if the
   * source tree cannot be located — a wrong working directory has to fail loudly rather than pass
   * vacuously on an empty scan.
   *
   * @param widget the raw Swing simple name being guarded
   * @param replacement the Elwha primitive that belongs at those sites, named in the report
   * @param allowlist file simple name to the reason that file may keep raw sites
   * @throws IOException if the source tree cannot be read
   */
  static void report(
      final String widget, final String replacement, final Map<String, String> allowlist)
      throws IOException {
    if (!Files.isDirectory(SOURCE_ROOT)) {
      System.err.println(
          "FAIL — source root not found at "
              + SOURCE_ROOT.toAbsolutePath()
              + " (run from the module root, e.g. via mvn exec:java)");
      System.exit(2);
    }

    final List<String> violations = violations(widget, allowlist);
    System.out.println(
        "Scanned " + guardedFiles().size() + " files across the guarded storefront surface.");
    System.out.println("Allowlisted keep(s):");
    if (allowlist.isEmpty()) {
      System.out.println("  (none)");
    }
    allowlist.forEach((file, reason) -> System.out.println("  " + file + " — " + reason));

    if (violations.isEmpty()) {
      System.out.println("\nPASS — no stray raw `new " + widget + "` in the guarded packages.");
      System.exit(0);
    }
    System.out.println(
        "\nFAIL — "
            + violations.size()
            + " stray raw `new "
            + widget
            + "` (use "
            + replacement
            + "):");
    violations.forEach(v -> System.out.println("  " + v));
    System.exit(1);
  }

  /**
   * Whether the guarded file named {@code fileSimpleName} still constructs {@code widget} raw — the
   * question an allowlist entry answers "yes, deliberately" to. A "no" means the entry has gone
   * stale and is now silently un-guarding a file that no longer needs the exemption.
   *
   * @param widget the raw Swing simple name
   * @param fileSimpleName the allowlisted file's simple name, e.g. {@code ThemePlayground.java}
   * @return {@code true} when that file still holds at least one raw construction
   * @throws IOException if the source tree cannot be read
   */
  static boolean stillNeedsExemption(final String widget, final String fileSimpleName)
      throws IOException {
    final Pattern raw = Pattern.compile("new\\s+" + widget + "\\s*[(<]");
    for (final Path file : guardedFiles()) {
      if (!file.getFileName().toString().equals(fileSimpleName)) {
        continue;
      }
      for (final String line : Files.readAllLines(file)) {
        if (raw.matcher(line).find()) {
          return true;
        }
      }
    }
    return false;
  }

  /** Every Java source file on the guarded surface. */
  static List<Path> guardedFiles() throws IOException {
    final List<Path> files = new ArrayList<>();
    try (Stream<Path> tree = Files.walk(SOURCE_ROOT)) {
      for (final Path file : (Iterable<Path>) tree.filter(RawSwingSweep::isGuarded)::iterator) {
        files.add(file);
      }
    }
    files.sort(Path::compareTo);
    return files;
  }

  private static boolean isGuarded(final Path path) {
    if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".java")) {
      return false;
    }
    final Path parent = SOURCE_ROOT.relativize(path).getParent();
    if (parent == null) {
      return false;
    }
    final String dir = parent.toString().replace('\\', '/');
    return dir.equals("showcase") || dir.endsWith("playground");
  }
}
