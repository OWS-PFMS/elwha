package com.owspfm.elwha.icons;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins the glyph inventory published in the {@code com.owspfm.elwha.icons} package doc to the
 * resource directory that is the actual truth. The doc promises consumers a complete list of the
 * names {@link MaterialIcons#get(String)} accepts; a hand-kept list rots the day a glyph lands
 * without a doc edit, so this sweep derives both inventories (both-cuts and outline-only) from
 * {@code src/main/resources/com/owspfm/icons/material} and fails when the doc's groups, counts, or
 * file total disagree.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class MaterialIconsInventoryDocTest {

  private static final Path RESOURCE_DIR =
      Path.of("src", "main", "resources", "com", "owspfm", "icons", "material");

  private static final Path PACKAGE_INFO =
      Path.of("src", "main", "java", "com", "owspfm", "elwha", "icons", "package-info.java");

  @Test
  void everyBundledGlyphAppearsInThePackageDocExactlyOnce() throws IOException {
    final List<String> stems = resourceStems();
    final Set<String> bases =
        stems.stream()
            .filter(s -> !s.endsWith("_fill"))
            .collect(Collectors.toCollection(TreeSet::new));
    final Set<String> fills =
        stems.stream()
            .filter(s -> s.endsWith("_fill"))
            .map(s -> s.substring(0, s.length() - "_fill".length()))
            .collect(Collectors.toCollection(TreeSet::new));
    assertThat(bases).as("every fill variant shadows a bundled outline glyph").containsAll(fills);

    final String doc = normalizedPackageDoc();
    for (final String base : bases) {
      assertThat(countCodeMentions(doc, base))
          .as("glyph {@code %s} is listed exactly once in the icons package doc", base)
          .isEqualTo(1);
    }

    final Set<String> outlineOnly = new TreeSet<>(bases);
    outlineOnly.removeAll(fills);
    assertThat(doc)
        .as("the doc's group counts match the directory")
        .contains("The " + fills.size() + " shipped in both cuts")
        .contains("The " + outlineOnly.size() + " shipped outline-only")
        .contains("the " + stems.size() + " bundled SVGs");

    final String bothCutsSection =
        doc.substring(doc.indexOf("shipped in both cuts"), doc.indexOf("shipped outline-only"));
    for (final String fill : fills) {
      assertThat(countCodeMentions(bothCutsSection, fill))
          .as("glyph {@code %s} sits in the both-cuts group", fill)
          .isEqualTo(1);
    }
  }

  private static List<String> resourceStems() throws IOException {
    try (Stream<Path> files = Files.list(RESOURCE_DIR)) {
      return files
          .map(p -> p.getFileName().toString())
          .filter(n -> n.endsWith(".svg"))
          .map(n -> n.substring(0, n.length() - ".svg".length()))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  private static String normalizedPackageDoc() throws IOException {
    return Files.readAllLines(PACKAGE_INFO).stream()
        .map(l -> l.replaceFirst("^\\s*\\*\\s?", ""))
        .collect(Collectors.joining(" "))
        .replaceAll("\\s+", " ");
  }

  private static int countCodeMentions(final String doc, final String glyph) {
    final String needle = "{@code " + glyph + "}";
    int count = 0;
    int at = doc.indexOf(needle);
    while (at >= 0) {
      count++;
      at = doc.indexOf(needle, at + needle.length());
    }
    return count;
  }
}
